#!/system/bin/sh
# Safety switch, runs ON the TV. Started by optimize/install.sh before the stock home is disabled.
# If the marker home_ok.<nonce> does not appear within LIMIT seconds (the new home did not come up), it puts the stock
# home back on its own: re-enable it, re-enable the setup app, make it home, press Home.
# Without this, a TCL set left with no home shows a black screen and can reboot-loop with Wi-Fi down.
# The nonce belongs to one switch: the marker of an older run cannot disarm this one. While it waits it writes its pid
# (home_pid.<nonce>) and the time every 10 s (home_hb.<nonce>), so install.sh can tell it is alive before it switches.
# A restart of the TV kills it (stock Android cannot keep it): install.sh's journal (optimize/run/home-switch.json) covers that.
# Usage (on the TV): sh /data/local/tmp/deadman.sh <stock home component> <seconds> <nonce> &
# (On some TVs, e.g. a TCL on Android 14, a job started over ADB ends when the ADB session closes: install.sh then says so.)
# (HOME_DIR and the PATH are only changed by tests/install_retry_test.py)

STOCK="${1:-com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity}"
LIMIT="${2:-180}"
NONCE="${3:-}"
DIR="${HOME_DIR:-/data/local/tmp}"
# A nonce is required: an old marker without one (home_ok) must never be able to disarm a switch. install.sh always passes one.
[ -n "$NONCE" ] || { echo "deadman.sh needs a nonce (third argument)" >&2; exit 2; }
OK="$DIR/home_ok.$NONCE"
PIDF="$DIR/home_pid.$NONCE"; HB="$DIR/home_hb.$NONCE"
echo $$ > "$PIDF"
trap 'rm -f "$PIDF" "$HB"' EXIT
i=0
while [ $i -lt "$LIMIT" ]; do
  [ -f "$OK" ] && exit 0
  [ $((i % 10)) -eq 0 ] && date +%s > "$HB"
  sleep 1
  i=$((i + 1))
done
# the marker can land in the last second of the wait: looked at once more before the stock home is put back (4.3 S10)
[ -f "$OK" ] && exit 0

pm enable "${STOCK%%/*}"
pm enable com.google.android.tungsten.setupwraith
cmd role add-role-holder --user 0 android.app.role.HOME "${STOCK%%/*}"
cmd package set-home-activity --user 0 "$STOCK"
am start -n "$STOCK"
input keyevent KEYCODE_HOME
