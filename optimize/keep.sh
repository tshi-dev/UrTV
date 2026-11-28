#!/usr/bin/env bash
# Turn back on anything from optimize/lists/keep.txt that is disabled (AirPlay, casting, mirroring, voice, remote,
# inputs, tuner, firmware updates, Play Store, Google services). Our scripts never disable these, but a TV
# optimized before, by other tools or older versions, may have them off. Safe to run any time.
#   optimize/keep.sh
# And the apps the setup turned off, for the person who wants one back:
#   optimize/keep.sh off                   «name | package | list» of every app our lists turned off, by its usual name
#   optimize/keep.sh restore <pkg> …       turn those back on and remember them: a later `run.sh again` leaves them on
#   optimize/keep.sh unkeep <pkg> …        turn them off again and stop remembering them
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
# A tv.py that failed (the connection, the wrong TV) is a failure to say so, not an empty list that reads as «all on».
disabled_list() { "$PY" -I optimize/tv.py "pm list packages -d" | tr -d '\r' | sed -n 's/^package://p'; }
# the home switch's own: a person's «I want this one» never reaches these (optimize/restore.sh is for the stock home)
HOME_SWITCH='com.google.android.apps.tv.launcherx|com.google.android.tvlauncher|com.google.android.tungsten.setupwraith|com.tcl.keycustomfunctionservice|com.tcl.tv.tclhome_passive|com.google.android.leanbacklauncher.recommendations'

case "${1:-}" in
  off)
    listed=$(disabled_list) || { echo "!! Could not ask the TV which packages are off." >&2; exit 1; }
    printf '%s\n' "$listed" | sed 's/^/package:/' | "$PY" -I optimize/offlist.py off
    exit 0 ;;
  restore|unkeep)
    mode=$1; shift
    [ $# -gt 0 ] || { echo "usage: optimize/keep.sh $mode <package> …   (the package ids of 'optimize/keep.sh off')"; exit 2; }
    done_pkgs=()
    for p in "$@"; do
      if ! [[ "$p" =~ ^[A-Za-z0-9_][A-Za-z0-9_.]*$ ]]; then echo "!! not a package id: $p"; exit 2; fi
      if [ "$mode" = unkeep ]; then   # the same two refusals as optimize/disable.sh: features people use, and Android's own core
        if grep -vE '^[[:space:]]*(#|$)' optimize/lists/keep.txt | awk '{print $1}' | grep -xF "$p" > /dev/null; then echo "!! $p is in optimize/lists/keep.txt (a feature people use): it is never turned off here"; continue; fi
        if grep -qEf <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/core.txt | sed 's/.*/^(&)$/') <<< "$p"; then echo "!! $p is Android's own core: it is never turned off here"; continue; fi
      fi
      if grep -qxE "$HOME_SWITCH" <<< "$p"; then echo "!! $p belongs to the home screen switch: optimize/restore.sh brings the stock home back, not this"; continue; fi
      if [ -z "$("$PY" -I optimize/tv.py "pm list packages $p" | tr -d '\r' | grep -x "package:$p" || true)" ]; then
        echo "   $p is not on this TV: skipped"; continue
      fi
      if [ "$mode" = restore ]; then
        "$PY" -I optimize/tv.py "pm enable $p" > /dev/null || true
        # a list that cannot be read is not «now on»: the app is not called on, and not remembered
        now=$(disabled_list) || { echo "!! Could not ask the TV which packages are off, so «$p on again» was not confirmed. Run the same command again."; continue; }
        if grep -qx "$p" <<< "$now"; then "$PY" -I optimize/tv.py "pm enable --user 0 $p" > /dev/null || true; fi
        now=$(disabled_list) || { echo "!! Could not ask the TV which packages are off, so «$p on again» was not confirmed. Run the same command again."; continue; }
        if grep -qx "$p" <<< "$now"; then echo "!! $p is still off: this TV would not turn it on"; continue; fi
        echo "   on again: $("$PY" -I optimize/offlist.py name "$p") ($p)"
      else
        "$PY" -I optimize/tv.py "pm disable-user --user 0 $p" > /dev/null || true
        echo "   off again: $("$PY" -I optimize/offlist.py name "$p") ($p)"
      fi
      done_pkgs+=("$p")
    done
    if [ "${#done_pkgs[@]}" -gt 0 ]; then
      if [ "$mode" = restore ]; then "$PY" -I optimize/offlist.py remember "${done_pkgs[@]}"
      else "$PY" -I optimize/offlist.py forget "${done_pkgs[@]}"; fi
    fi
    [ "${#done_pkgs[@]}" = 0 ] || optimize/settle.sh mark 2> /dev/null || true   # Android writes the package state ~10 s later: nothing restarts the TV before that
    [ "$mode" = unkeep ] || [ "${#done_pkgs[@]}" = 0 ] || echo "Remembered: a later optimize/run.sh again leaves ${#done_pkgs[@]} of them on."
    exit 0 ;;
  "") ;;
  *) echo "usage: optimize/keep.sh [off | restore <package> … | unkeep <package> …]"; exit 2 ;;
esac
listed=$(disabled_list) || { echo "!! Could not ask the TV which packages are off, so the protected features were NOT checked." >&2; exit 1; }
off=$(printf '%s\n' "$listed" | grep -Fxf <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/keep.txt | awk '{print $1}') || true)
# the stock home and setup app are in keep.txt but belong to install.sh launcher: leave them to it
off=$(printf '%s\n' "$off" | grep -vxE 'com.google.android.apps.tv.launcherx|com.google.android.tvlauncher|com.google.android.tungsten.setupwraith|com.tcl.keycustomfunctionservice' || true)
if [ -z "$off" ]; then echo "Protected features: all on."; exit 0; fi
echo "Turning back on (they must never be off):"
printf '%s\n' "$off" | sed 's/^/   /'
printf '%s\n' "$off" | awk '{print "pm enable " $1}' | "$PY" -I optimize/tv.py --batch > /dev/null
disabled() {   # of the ones turned on above, those still off
  local now
  now=$(disabled_list) || { echo "!! Could not ask the TV again which packages are off." >&2; return 1; }
  printf '%s\n' "$now" | grep -Fxf <(printf '%s\n' "$off") || true
}
still=$(disabled)
if [ -n "$still" ]; then   # a second try, the way Android 9 sets want it
  printf '%s\n' "$still" | awk '{print "pm enable --user 0 " $1}' | "$PY" -I optimize/tv.py --batch > /dev/null || true
  still=$(disabled)
fi
if [ -n "$still" ]; then
  # Some packages the TV itself keeps off and pm enable can't turn on (a TCL outside Alexa's regions ships
  # com.tcl.smartalexa disabled, issue #4). That is not our doing: say so and go on. Only a package that was on
  # before this run (optimize/run/backup.txt) and is off now is a problem to stop for.
  before=$(awk '/^== disabled packages/{s=1; next} /^== /{s=0} s' optimize/run/backup.txt 2> /dev/null \
           | sed -n 's/^package://p' | tr -d '\r' || true)
  ours=$(grep -Fxvf <(printf '%s\n' "$before") <<< "$still" || true)
  if [ ! -f optimize/run/backup.txt ]; then   # nothing shows whether the TV had them off before: they are not assumed to be «the TV's own»
    echo "!! still off, and there is no optimize/run/backup.txt to show whether the TV had them off before UrTV: tell the person: $(paste -sd' ' - <<< "$still")"
    exit 1
  fi
  kept=$(grep -Fxvf <(printf '%s\n' "${ours:-}") <<< "$still" || true)
  [ -z "$kept" ] || echo "   left off by the TV itself (off before UrTV, and pm enable can't turn it on): $(paste -sd' ' - <<< "$kept")"
  if [ -n "$ours" ]; then echo "!! still off, tell the person: $(paste -sd' ' - <<< "$ours")"; exit 1; fi
fi
echo "Protected features: all on."
