#!/usr/bin/env bash
# «البث من الجوال» on a Chromecast with Google TV (and the Google TV Streamer): casting from a phone and the Google Home remote need the
# TV's setup wizard (setupwraith) on, because the cast receiver reads its device credentials from it (optimize/install.sh).
# With the wizard on, Android may still send Home to the Google TV home on these devices; then the person chooses:
#   optimize/dock.sh cast status   does this TV hold its cast credentials in the wizard, is the wizard on, who answers Home, the choice
#   optimize/dock.sh cast on       casting kept (the default): the wizard on, the cast receiver restarted, then the usual home switch, which
#                                  keeps the wizard on and makes our home the Home if Android lets it (else the TV keeps its own home and
#                                  UrTV opens from its «تلفزيونك» tile)
#   optimize/dock.sh cast off      our home first: the choice is recorded, then the usual home switch, which may turn the wizard off with
#                                  its Wi-Fi guard (never when it owns the saved Wi-Fi). Casting from a phone then says «untrusted device».
# Only on a TV whose wizard holds the cast credentials (told by the provider, never by the model name); anywhere else it says so and stops.
# The wizard is only ever turned ON here; turning it off is install.sh's job, with its checks.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
RUN="${URTV_RUN_DIR:-optimize/run}"
CASTF="$RUN/cast.txt"
WRAITH=com.google.android.tungsten.setupwraith
tv() { "$PY" -I optimize/tv.py "$@"; }
casts() { tv "dumpsys package $WRAITH 2> /dev/null | grep -m 1 castinfo.CastInfoProvider" 2> /dev/null | grep castinfo.CastInfoProvider > /dev/null; }
wizard() {   # on | off | unknown
  local d; d=$(tv "pm list packages -d; echo END_OF_LIST" 2> /dev/null | tr -d '\r') || { echo unknown; return; }
  [ "$(tail -n 1 <<< "$d")" = END_OF_LIST ] || { echo unknown; return; }
  if grep -x "package:$WRAITH" <<< "$d" > /dev/null; then echo off; else echo on; fi
}
choice() { local c; c=$(tr -d '\r\n ' < "$CASTF" 2> /dev/null || true); echo "${c:-on}"; }
mode="${1:-status}"
case "$mode" in status|on|off) ;; *) echo "usage: optimize/dock.sh cast on|off|status"; exit 1 ;; esac
if ! casts; then
  echo "This TV's setup wizard does not hold cast credentials (not a Chromecast with Google TV), or the TV did not say: nothing to do. Casting does not depend on the home switch here."
  [ "$mode" = status ] && exit 0
  exit 1
fi
case "$mode" in
  status)
    home=$(tv "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null | tr -d '\r' | tail -n 1) || home=""
    echo "Chromecast: the setup wizard holds the cast credentials. Wizard: $(wizard). Home: ${home:-unknown}. Choice: $(choice) (on = casting kept, off = our home first)."
    ;;
  on)
    mkdir -p "$RUN"; echo on > "$CASTF"
    # turning the wizard on never touches the Wi-Fi; the cast receiver is restarted so it reads its credentials again
    printf '%s\n' "pm enable $WRAITH" "am force-stop com.google.android.apps.mediashell" | tv --batch > /dev/null \
      || { echo "!! cast.sh: the TV did not answer, so the setup wizard may still be off. Run optimize/find-tv.sh, then the same command again."; exit 1; }
    [ "$(wizard)" = on ] || { echo "!! cast.sh: the setup wizard did not come back on. Run the same command again; if it says this again, report it with optimize/share.sh."; exit 1; }
    echo "Casting on: the setup wizard is on and the cast receiver was restarted. Now the usual home switch (it keeps the wizard on)."
    optimize/install.sh launcher
    echo "If casting still says «untrusted device», restart the TV once: optimize/venv/bin/python -I optimize/tv.py --reboot"
    ;;
  off)
    mkdir -p "$RUN"; echo off > "$CASTF"
    echo "Casting off (our home first): recorded. Now the usual home switch; it turns the setup wizard off only when that cannot cost the saved Wi-Fi."
    optimize/install.sh launcher
    echo "للشخص: صار «تلفزيونك» هو الشاشة الرئيسية، والبث من الجوال وريموت Google Home ما يشتغلون. لو احتجتهم: نرجّعهم بأمر واحد."
    ;;
esac
