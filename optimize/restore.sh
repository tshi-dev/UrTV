#!/usr/bin/env bash
# Undo everything: re-enable every package in the given debloat lists, bring back the stock launcher, Google
# TV's setup app and the stock home's helpers (lists/home-replaced.txt), and make the stock launcher home again.
# Usage: optimize/restore.sh [debloat list ...]        (no list: only the home screen)
#   STOCK_LAUNCHER=com.google.android.tvlauncher/.MainActivity optimize/restore.sh list.txt   (older Android TV)
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
. optimize/home-journal.sh
# The stock home that is really on this TV: the one asked for, else the one the home switch journal recorded (found when the switch was made,
# from a good package list), else a known launcher found in the package list now. A package that is not installed is never named.
STOCK_LAUNCHER="${STOCK_LAUNCHER:-$(jget stock)}"
if [ -z "$STOCK_LAUNCHER" ]; then
  pkglist=$("$PY" -I optimize/tv.py "pm list packages" 2> /dev/null | tr -d '\r') || pkglist=""
  if ! grep -qx "package:android" <<< "$pkglist"; then   # the answer must hold the android package itself, else it is «the TV did not say»
    echo "!! restore: could not read the TV's package list, so its own home could not be found: the home was not touched and the safety switch (if any) was left armed. Run the same command again."
  elif grep -qx "package:com.google.android.apps.tv.launcherx" <<< "$pkglist"; then STOCK_LAUNCHER=com.google.android.apps.tv.launcherx/.home.HomeActivity
  elif grep -qx "package:com.google.android.tvlauncher" <<< "$pkglist"; then STOCK_LAUNCHER=com.google.android.tvlauncher/.MainActivity; fi
fi
nonce=$(jget nonce)
RUN="${URTV_RUN_DIR:-optimize/run}"
LEDGER="$RUN/disabled-by-us.txt"
problems=""
RESULT=""
# A list's packages, one per line, in $RESULT (no subshell: a file that cannot be read is added to $problems, said at the end). No match (an
# empty file, only comments) is a valid empty set; only a read error is a problem. The home recovery below never depends on any of this.
words_of() {   # file -> its first words, comments and blank lines dropped
  local txt; RESULT=""
  txt=$(cat "$1" 2> /dev/null) || { problems="$problems $1"; return 0; }
  RESULT=$({ grep -vE '^[[:space:]]*(#|$)' <<< "$txt" || true; } | awk '{print $1}')
}
ledger_pkgs() {   # the ledger's package names (anything else on a line is dropped)
  local txt; RESULT=""
  txt=$(cat "$1" 2> /dev/null) || { problems="$problems $1"; return 0; }
  RESULT=$({ grep -E '^[A-Za-z0-9_.]+$' <<< "$txt" || true; } | sort -u)
}
minus() {   # $1 without the lines of $2 (awk, not grep -f: an empty pattern file matches everything on some greps)
  awk 'NR==FNR { if ($0 != "") s[$0] = 1; next } !($0 in s)' <(printf '%s\n' "$2") <(printf '%s\n' "$1")
}
legacy=""
for LIST in "$@"; do words_of "$LIST"; legacy="$legacy"$'\n'"$RESULT"; done
words_of optimize/lists/home-replaced.txt; home_helpers=$RESULT
# What the TV had off before UrTV, from a real backup (a saved package list of 20 or more): those stay off.
backup="$RUN/backup.txt"; backup_ok=no; before_off=""
if [ -r "$backup" ] && [ "$(grep -c '^package:' "$backup" || true)" -ge 20 ]; then
  backup_ok=yes
  before_off=$(awk '/^== disabled packages/{s=1; next} /^== /{s=0} s' "$backup" | tr -d '\r' | sed -n 's/^package://p')
fi
# What to turn back on. The ledger lists what our disable runs turned off (installed, and not already off before them). An install from before
# the ledger turned its first packages off without it, and a later run then found them already off: so with a real backup the lists count too,
# minus what the TV had off before UrTV. With no ledger: the same lists, and with no backup either every package of them, and it says so.
# The stock home's helpers (lists/home-replaced.txt) are part of the home recovery and always come.
enable=""
if [ -e "$LEDGER" ]; then
  ledger_pkgs "$LEDGER"; enable=$RESULT
  if [ "$backup_ok" = yes ] && [ "$#" -gt 0 ]; then enable="$enable"$'\n'"$(minus "$legacy" "$before_off")"; fi
elif [ "$#" -gt 0 ]; then
  if [ "$backup_ok" = yes ]; then enable=$(minus "$legacy" "$before_off")
  else
    enable=$legacy
    echo "Note: there is no record of what our setup turned off and no backup of what the TV had off before it, so every package of the lists is turned on (including any the TV itself had off)."
  fi
fi
{
  # 1. the home recovery first: it never waits for, or depends on, the package part
  { printf '%s\n' "$home_helpers" | { grep -E '^[A-Za-z0-9_.]+$' || true; } | sort -u | awk '{print "pm enable " $1}'; } || true
  echo "pm enable com.google.android.tungsten.setupwraith"
  if [ -n "$STOCK_LAUNCHER" ]; then   # the safety switch is stopped only when the home really is put back by hand
    echo "touch /data/local/tmp/home_ok"
    [ -z "$nonce" ] || echo "touch /data/local/tmp/home_ok.$nonce"   # (the one of the last home switch)
    echo "pm enable ${STOCK_LAUNCHER%%/*}"
    echo "pm disable sa.urtv.rescue/sa.urtv.home.setup.RescueHome 2> /dev/null"   # 4.4 (R7): the stock home is back, the rescue's fallback home steps aside (RescueHome.DISARM)
    # Google TV's home can come back without its notification access (seen on other TVs); give it back
    [ "${STOCK_LAUNCHER%%/*}" != com.google.android.apps.tv.launcherx ] || echo "cmd notification allow_listener com.google.android.apps.tv.launcherx/.coreservices.notificationlistener.TvNotificationListenerService"
    echo "cmd role add-role-holder --user 0 android.app.role.HOME ${STOCK_LAUNCHER%%/*}"
    echo "cmd package set-home-activity --user 0 $STOCK_LAUNCHER"
  fi
  # 2. then the packages
  { printf '%s\n' "$enable" | { grep -E '^[A-Za-z0-9_.]+$' || true; } | sort -u | awk '{print "pm enable " $1}'; } || true
  echo "input keyevent KEYCODE_HOME"
} | "$PY" -I optimize/tv.py --batch
[ -z "$STOCK_LAUNCHER" ] || jstate restored
optimize/settle.sh mark 2> /dev/null || true
[ -n "$STOCK_LAUNCHER" ] || echo "!! restore: could not tell which app is this TV's own home, so no home was set: press Home on the remote; if nothing opens, tell the person to use the TV's own settings (Apps, Default apps, Home app)."
if [ -n "$problems" ]; then
  echo "!! restore: the home was put back, but these files could not be read, so their packages were NOT turned on:$problems. Fix the file (or its permissions) and run optimize/restore.sh again."
  exit 1
fi
echo "Restored. Press Home on the remote."
