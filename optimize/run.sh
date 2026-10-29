#!/usr/bin/env bash
# The whole setup in one command, made for slow or flaky ADB: every finished step is remembered, so after
# any failure just run it again and it carries on where it stopped.
#   optimize/run.sh            do the next steps
#   optimize/run.sh status     what is done
#   optimize/run.sh again      redo every step on the same TV, as after a TV system update put things back
#                              (the apps' settings stay: installing over keeps them; the old summary.md
#                              becomes summary-<n>.md so the new run's before/after starts clean)
#   SKIP_LAUNCHER=1 optimize/run.sh   keep the TV's own home screen (firmware that won't let another app be Home)
#   SKIP_EXTRAS=1 optimize/run.sh     without TV Bro and SmartTube
#   (a different TV than the one the setup started on is refused; for a new TV: optimize/venv/bin/python -I optimize/tv.py --new-tv)
# Before: optimize/setup.sh and optimize/find-tv.sh. Optional packages the household gave up go in
# optimize/lists/mine.txt (one per line, picked from lists/tcl-optional.txt) before running.
# Results in optimize/run/: summary.md (before/after), ram-*.txt, backup.txt, report.md, steps.log (times), run.log.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
RUN=optimize/run
mkdir -p "$RUN"
tv() { "$PY" -I optimize/tv.py "$@"; }
# an app's version as the report says it: the name (4.0) and, from versionCode, the build: «4.0 (build 151)»
appver() {
  local o n b
  o=$(tv "dumpsys package $1 2> /dev/null | grep -e versionName -e versionCode= | sed -n 1,2p" | tr -d '\r')
  n=$(sed -n 's/.*versionName=//p' <<< "$o" | sed -n 1p | tr -d ' ')
  b=$(sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' <<< "$o" | sed -n 1p)
  [ -z "$n" ] || n="$n${b:+ (build $b)}"
  printf '%s' "$n"
}

# One run at a time: a second one would take the first for another TV and redo everything. The lock is a file with
# the PID of the run; a PID that is gone means it died, and the lock is taken over.
LOCK="$RUN/.lock"
if [ "${1:-}" != status ]; then
  for _ in 1 2; do
    if ( set -C; echo $$ > "$LOCK" ) 2> /dev/null; then break; fi
    other=$(cat "$LOCK" 2> /dev/null || true)
    if [ -n "$other" ] && kill -0 "$other" 2> /dev/null; then
      echo "run.sh: not started: another run is working on this TV (process $other). Wait for it to end, then run the same command again."
      echo "(Its progress: optimize/run.sh status. If it is surely gone, delete $LOCK.)"
      exit 1
    fi
    echo "(a lock left by a run that is gone: taken over)"
    rm -f "$LOCK"
  done
  [ "$(cat "$LOCK" 2> /dev/null || true)" = "$$" ] || { echo "run.sh: not started: could not take the lock $LOCK"; exit 1; }
# when each step started, ended or stopped: optimize/share.sh turns it into how long each took
stamp() { echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$RUN/steps.log"; }
current=start
finish() {
  local rc=$?
  trap - EXIT
  [ "$(cat "$LOCK" 2> /dev/null || true)" = "$$" ] && rm -f "$LOCK"
  if [ "$rc" -eq 0 ]; then echo "run.sh: done"
  elif [ "$rc" -eq 3 ] && [ "$current" = precheck ]; then stamp "stopped $current (not an Android TV)"; echo "run.sh: stopped at precheck: this device is not an Android TV, nothing was changed (see above)"
  else stamp "stopped $current (exit $rc)"; echo "run.sh: stopped at $current, run the same command again"; fi
  exit "$rc"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
fi

STEPS=(precheck probe backup keep before before-saver safe mine community tweaks screensaver launcher extras autostart after after-saver report)
title() {
  case "$1" in
    precheck)    echo "Check it is an Android TV with a WebView the screensaver can run on" ;;
    probe)       echo "Connect and identify the TV" ;;
    backup)      echo "Save the TV's current state" ;;
    keep)        echo "Turn protected features back on if anything turned them off" ;;
    before)      echo "Measure before (restart + 3 min)" ;;
    before-saver) echo "Measure the TV's own screensaver (1 min)" ;;
    safe)        echo "Disable telemetry, ads and demos (lists/tcl-safe.txt)" ;;
    mine)        echo "Disable the household's optional picks (lists/mine.txt)" ;;
    community)   echo "A community list, if this model has one and ours doesn't know it" ;;
    tweaks)      echo "Speed and memory tweaks" ;;
    screensaver) echo "Install شاشة تلفزيونك" ;;
    launcher)    echo "Install the home screen (safety switch on)" ;;
    extras)      echo "Install TV Bro (web browser) and SmartTube (YouTube without ads)" ;;
    autostart)   echo "Let TCL start our apps at boot (auto-start)" ;;
    after)       echo "Measure after (restart + 3 min)" ;;
    after-saver) echo "Measure our screensaver (1 min)" ;;
    report)      echo "Write the report" ;;
  esac
}

if [ "${1:-}" = again ]; then   # a TV system update can re-enable what we turned off and take Home back
  rm -f "$RUN"/.done-* "$RUN/steps.log"
  kept=
  if [ -f "$RUN/summary.md" ]; then   # measure.sh appends: a second run's before/after lines would double up
    n=1; while [ -e "$RUN/summary-$n.md" ]; do n=$((n + 1)); done
    mv "$RUN/summary.md" "$RUN/summary-$n.md"; kept=" The last run's summary.md is now summary-$n.md."
  fi
  echo "Marks cleared: every step runs again (the backup from the first run is kept).$kept"
  shift
fi
if [ "${1:-}" = status ]; then
  for s in "${STEPS[@]}"; do printf '%s %s\n' "$([ -f "$RUN/.done-$s" ] && echo '✓' || echo '·')" "$(title "$s")"; done
  if [ -f "$RUN/report.md" ]; then   # the report's !! lines, so nobody has to grep it by hand
    grep '!!' "$RUN/report.md" | sed 's/^/report: /' || echo "report: no !! lines"
  fi
  exit 0
fi

exec > >(tee -a "$RUN/run.log") 2>&1   # everything it says, for optimize/share.sh: its !! lines and dropped connections

# Is this the TV the setup started on? Two devices can answer on one Wi-Fi (a box and the TV). The first TV seen is
# remembered in $RUN/tv-id.json; another one stops here, before the backup (which would overwrite the first TV's).
tv --check

. optimize/home-journal.sh

check_list() {   # never disable what UAD-ng rates Unsafe; show everything that is not "Recommended"
  local out
  out=$("$PY" -I optimize/check-packages.py "$1")
  echo "$out" | grep -v '^Recommended' | sed 's/^/   /' || true
  if echo "$out" | grep -q '^Unsafe'; then echo "!! $1 has a package rated Unsafe. Take it out and run again."; exit 1; fi
}

report() {
  echo "# TV setup report — $(date '+%Y-%m-%d %H:%M')"
  echo
  grep -E 'model|version.release|display.id' "$RUN/tv.txt" || true
  echo
  optimize/precheck.sh report 2> /dev/null || true   # «Device:» and «WebView:» (a !! line if the WebView is too old)
  echo
  cat "$RUN/summary.md"
  "$PY" -I optimize/heavy.py "$RUN/summary.md" || true   # the TV's own screensaver against ours: a !! advice line if ours is heavy
  echo
  echo "Features that must stay on:"
  for p in com.tcl.airplay2 com.google.android.apps.mediashell com.tcl.miracast com.google.android.katniss \
           com.tcl.tvinput com.android.vending com.google.android.gms; do
    # one question with end markers: an empty answer without them is «the TV did not say», not «not installed»
    pk=$(tv "pm list packages -d $p; echo D_END; pm list packages $p; echo I_END" 2> /dev/null | tr -d '\r') || pk=""
    if awk '/^D_END$/{exit} {print}' <<< "$pk" | grep -qx "package:$p"; then echo "- !! $p is DISABLED"
    elif awk '/^D_END$/{s=1; next} s' <<< "$pk" | grep -qx "package:$p"; then echo "- ok: $p"
    elif ! grep -qx 'I_END' <<< "$pk" || ! grep -qx 'D_END' <<< "$pk"; then echo "- ?? $p: the TV did not answer"
    fi
  done
  echo
  optimize/tweaks.sh status 2> /dev/null || true   # «- tweak …: was …, now …» (the measurements above show whether it helped)
  [ "$(jget safety_switch)" != unavailable ] || echo "Home switch: no safety switch on the TV itself (this TV ends background jobs with the ADB session); it was checked and can be undone from this computer and the journal"
  echo "Home: $(tv "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" | tail -1)"
  optimize/autostart.sh check 2> /dev/null || true   # TCL: «Auto-start (TCL): …» or a !! line
  [ ! -f "$RUN/home-skipped.txt" ] || cat "$RUN/home-skipped.txt"   # a TV that won't let its home be switched (Nokia Streaming Box 8000)
  echo "Screensaver: $(tv "settings get secure screensaver_components")"
  echo "Versions: screensaver $(appver sa.urtv.screensaver), home $(appver sa.urtv.home)"
  echo "Extras: TV Bro $([ -n "$(tv "pm list packages com.phlox.tvwebbrowser")" ] && echo yes || echo no), SmartTube $([ -n "$(tv "pm list packages org.smarttube.stable")" ] && echo yes || echo no)"
  local picks
  picks=$(grep -cvE '^[[:space:]]*(#|$)' optimize/lists/mine.txt 2>/dev/null || true)
  echo "Optional picks (lists/mine.txt): ${picks:-0}"
  echo
  echo "Packages from our lists (off: disabled, on: still on, absent: not on this TV):"
  local disabled installed l pkg state
  optimize/settle.sh wait 2> /dev/null || true   # the disabled state is read back after Android has written it
  disabled=$(tv "pm list packages -d" | tr -d '\r')
  installed=$(tv "pm list packages" | tr -d '\r')
  for l in tcl-safe tcl-optional keep home-replaced; do
    for pkg in $(grep -vE '^[[:space:]]*(#|$)' "optimize/lists/$l.txt" | awk '{print $1}'); do
      if grep -qx "package:$pkg" <<< "$disabled"; then state=off
      elif grep -qx "package:$pkg" <<< "$installed"; then state=on
      else state=absent; fi
      echo "- pkg $state: $pkg ($l)"
    done
  done
  echo "Turned off by the setup (name | package | list). One the person wants back: optimize/keep.sh restore <package>"
  optimize/keep.sh off 2> /dev/null | sed 's/^/- off-name: /' || true
  if [ -s "$RUN/community-applied.txt" ]; then   # the community list (CleanYourTV, MIT) for this model, if one was used
    echo "Community list: $(cat "$RUN/community-name.txt" 2> /dev/null) (CleanYourTV, MIT)"
    for pkg in $(grep -vE '^[[:space:]]*(#|$)' "$RUN/community-applied.txt" | awk '{print $1}'); do
      if grep -qx "package:$pkg" <<< "$disabled"; then state=off
      elif grep -qx "package:$pkg" <<< "$installed"; then state=on
      else state=absent; fi
      echo "- pkg $state: $pkg (community)"
    done
  fi
  echo "Left off the home screen:"
  optimize/dock.sh hidden 2> /dev/null | sed 's/^/- home hides: /' || true
  echo "Undo everything: optimize/reset.sh"
}

do_step() {
  case "$1" in
    precheck)    rc=0; optimize/precheck.sh > "$RUN/precheck.txt" 2>&1 || rc=$?   # 3: not an Android TV, stop (nothing was changed)
                 cat "$RUN/precheck.txt"; [ "$rc" = 0 ] || exit "$rc" ;;
    probe)       tv --probe | tee "$RUN/tv.txt" ;;
    backup)      optimize/backup.sh
                 # the TV as it came is the first run's: a run.sh again after the tweaks would record the tweaked TV
                 if [ -f "$RUN/backup.txt" ]; then echo "(the backup from the first run stays: optimize/reset.sh puts back what the TV had then)"
                 else
                   newest=$(ls -t optimize/backup-*.txt | head -1)
                   # a backup saved while the TV answered nothing would be kept for ever and make reset remove the person's own apps
                   if [ "$(grep -c '^package:' "$newest" || true)" -lt 20 ] || ! grep -qx 'package:android' "$newest"; then
                     rm -f "$newest"
                     echo "!! The TV's package list came back empty (or nearly) while the backup was saved, so it would be useless. Nothing was changed. Run the same command again."
                     exit 1
                   fi
                   cp "$newest" "$RUN/backup.txt"
                 fi ;;
    keep)        optimize/keep.sh | tee "$RUN/keep.txt" ;;
    before)      optimize/measure.sh before ;;
    before-saver) optimize/measure.sh before-screensaver --screensaver ;;
    safe)        check_list optimize/lists/tcl-safe.txt
                 optimize/disable.sh optimize/lists/tcl-safe.txt > /dev/null ;;
    mine)        if [ -s optimize/lists/mine.txt ]; then
                   check_list optimize/lists/mine.txt
                   optimize/disable.sh optimize/lists/mine.txt > /dev/null
                 else
                   echo "   (no lists/mine.txt: nothing optional)"
                 fi ;;
    community)   optimize/community.sh ;;
    tweaks)      optimize/tweaks.sh apply ;;
    screensaver) optimize/install.sh saver ;;
    launcher)    if [ "${SKIP_LAUNCHER:-}" = 1 ]; then echo "   skipped (SKIP_LAUNCHER=1): the TV keeps its own home screen"
                 else optimize/install.sh launcher; fi ;;
    extras)      if [ "${SKIP_EXTRAS:-}" = 1 ]; then echo "   skipped (SKIP_EXTRAS=1)"; else optimize/install.sh extras; fi ;;
    autostart)   optimize/autostart.sh ;;
    after)       optimize/measure.sh after ;;
    after-saver) optimize/measure.sh after-screensaver --screensaver ;;
    report)      report > "$RUN/report.md"; cat "$RUN/report.md" ;;
  esac
}

# The .done-* marks belong to one TV: a different one (its serial, else its address) starts from the top.
is_placeholder() { "$PY" -I optimize/tv.py --is-placeholder "$1" 2> /dev/null; }   # one list, in tv.py, shared with its identity check
# The TV's unique id by one policy (tv.py --id): serial, boot serial, the Android id ("aid:…"), and only then the address
tv_id=$(tv --id 2> /dev/null | tr -d '\r\n ' || true)
if [ -z "$tv_id" ]; then tv_id="${TV_HOST:-$(cat optimize/.tv_host 2> /dev/null || true)}"; fi
saved_id=$(cat "$RUN/.tv" 2> /dev/null || true)
looks_like_address() { [[ "$1" =~ ^[0-9]+(\.[0-9]+){3}(:[0-9]+)?$ ]] || [[ "$1" =~ ^[A-Za-z0-9.-]+:[0-9]+$ ]]; }
# an id saved by an older run that was a placeholder says nothing about which TV it was: it is replaced without wiping (as before this check)
if [ -n "$saved_id" ] && is_placeholder "$saved_id" && [ -n "$tv_id" ]; then echo "$tv_id" > "$RUN/.tv"; saved_id=$tv_id; fi
if [ -n "$tv_id" ] && [ "$saved_id" != "$tv_id" ]; then
  rm -f "$RUN"/.done-* "$RUN/steps.log"
  if looks_like_address "$tv_id" || { [ -n "$saved_id" ] && looks_like_address "$saved_id"; }; then
    # the id is only an address (or was): a new address may be the same TV after a DHCP change, so its backup and journals are kept; only the step marks restart
    [ -z "$saved_id" ] || echo "Another address than the last run (no serial to tell if it is the same TV): the steps start again, the backup and journals are kept."
  else
    [ -z "$saved_id" ] || echo "A different TV than the last run: starting from the first step."
    # what the last TV had is not this TV's (its backup, tweaks and home switch journals)
    rm -f "$RUN/backup.txt" "$RUN/tweaks-journal.txt" "$RUN/family-dns-journal.txt" "$RUN/family-dns-journal.done" "$RUN/grants-journal.txt" "$RUN/disabled-by-us.txt" "$RUN/.package-change" "$RUN/community-applied.txt" "$RUN/community-ask.txt" "$RUN/tweaks-journal.done" "$RUN/home-switch.json" "$RUN/accessibility-before.txt" "$RUN/.screen-off-timeout"
  fi
  echo "$tv_id" > "$RUN/.tv"
fi

# A home switch that a restart cut off is undone from its journal first (see optimize/home-journal.sh). Never stops the run.
jrecover || true

for s in "${STEPS[@]}"; do
  [ -f "$RUN/.done-$s" ] && continue
  current=$s
  echo
  echo "▶ $(title "$s")"
  stamp "start $s"
  do_step "$s"
  touch "$RUN/.done-$s"
  stamp "done $s"
done
# the heavy-screensaver line is advice for the owner, not a failure: it is shown but does not stop the run
ADVICE='the screensaver may slow this TV'
if grep '!!' "$RUN/report.md" | grep -qv "$ADVICE"; then
  current=report-check   # the stop below is no step's fault
  echo
  echo "Done, with problems:"
  grep '!!' "$RUN/report.md"
  rm -f "$RUN/.done-report"   # the next run writes the report again, so a fixed problem stops showing
  exit 1
fi
if grep -q "$ADVICE" "$RUN/report.md"; then
  echo
  grep "$ADVICE" "$RUN/report.md"
fi
echo
echo "All steps done. Next: settings (optimize/saver.sh), favourites (optimize/dock.sh), photos (optimize/shot.sh)."
