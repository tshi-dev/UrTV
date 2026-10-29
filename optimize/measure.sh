#!/usr/bin/env bash
# Measure the TV's memory and load, fairly.
#   optimize/measure.sh before                 restart, 3 minutes on the home screen, then read (about 5 minutes)
#   optimize/measure.sh after --now            read right away, no restart
#   optimize/measure.sh power                  what UrTV holds right now: Doze, wake locks, alarms, jobs (read-only, see optimize/power.sh)
#   optimize/measure.sh saver --screensaver    start the screensaver, read after 60 s while it runs, then leave it
# The TV is kept awake while it settles (its screensaver timeout is stretched, then put back), so a screensaver
# kicking in can't spoil an idle reading. Writes optimize/run/ram-<label>.txt, adds a line to optimize/run/summary.md.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
[ "${1:-}" != power ] || exec optimize/power.sh   # what UrTV holds right now (Doze, wake locks, alarms, jobs); read-only
label="${1:?usage: optimize/measure.sh LABEL [--now|--screensaver]}"
mode="${2:-}"
RUN="${URTV_RUN_DIR:-optimize/run}"
mkdir -p "$RUN"
out="$RUN/ram-$label.txt"
tv() { "$PY" -I optimize/tv.py "$@"; }

# "before" is the baseline: a re-run (run.sh resumed after an interruption) keeps the first one
case "$label" in before*)
  if grep -q "^| $label |" $RUN/summary.md 2> /dev/null; then
    echo "$label was measured already: keeping the first reading."; exit 0
  fi ;;
esac

if [ "$mode" = --screensaver ]; then
  tv "input keyevent KEYCODE_HOME" > /dev/null
  if ! optimize/start-saver.sh > /dev/null; then   # some Android 14 sets don't let ADB start it: nothing to measure
    echo "Android didn't start the screensaver from ADB on this TV: $label not measured." | tee "$out"
    if [ -f $RUN/summary.md ]; then
      echo "| $label | not measured (Android didn't start the screensaver from ADB) | | | | |" >> $RUN/summary.md
    fi
    tv "input keyevent KEYCODE_HOME" > /dev/null
    exit 0
  fi
  echo "screensaver running; reading in 60 s…"
  sleep "${SAVER_S:-60}"
else
  # Wireless debugging (TLS, pairing) gets a NEW port when the TV restarts, so the connection is lost and the tools cannot find the TV again (37 reports ended with
  # «TV did not come back within 5 min», mostly Android 14): there, measure without the restart and say so. Anything but a plain «adb» answer counts as wireless.
  if [ "$mode" != --now ]; then
    transport=$(tv --transport 2> /dev/null | tail -n 1) || transport=wireless
    if [ "$transport" != adb ]; then
      echo "This TV is connected by Wireless debugging (its port changes when it restarts), so it is not restarted: $label is measured now, without the restart and the 3-minute wait. The reading can be a little higher than after a restart."
      mode=--now
    fi
  fi
  if [ "$mode" != --now ]; then
    optimize/settle.sh wait 2> /dev/null || true   # a restart before Android has written the package state can lose what was disabled
    tv --reboot
  fi
  # the TV's own value is kept in a file while it is stretched, and put back exactly (null: deleted, not guessed); if this run
  # dies with the TV restarting (the trap never runs), the next measure puts it back first
  keep="$RUN/.screen-off-timeout"
  put_timeout_back() {
    local t; t=$(cat "$keep" 2> /dev/null || true)
    [ -n "$t" ] || return 0
    if [ "$t" = null ]; then tv "settings delete system screen_off_timeout" > /dev/null
    elif [[ "$t" =~ ^[0-9]+$ ]]; then tv "settings put system screen_off_timeout $t" > /dev/null; fi
    rm -f "$keep"
  }
  put_timeout_back
  timeout=$(tv "settings get system screen_off_timeout" | tr -d '\r' | tail -n 1)
  case "$timeout" in
    null|[0-9]*)   # the TV said its value (null: not set): stretched, and put back exactly
      echo "$timeout" > "$keep"
      tv "settings put system screen_off_timeout 1800000" > /dev/null        # 30 min: stay on the home screen
      trap put_timeout_back EXIT ;;
    *) echo "!! screen_off_timeout: the TV did not say its value, so it was not stretched and a screensaver may spoil this reading" ;;
  esac
  tv "input keyevent KEYCODE_HOME" > /dev/null
  if [ "$mode" != --now ]; then
    echo "letting the TV settle for 3 minutes (kept awake)…"
    sleep "${SETTLE_S:-180}"
  fi
fi

printf '%s\n' \
  "echo '== memory'; grep -E 'MemTotal|MemAvailable|SwapTotal|SwapFree' /proc/meminfo" \
  "echo '== android'; dumpsys meminfo | grep -E 'Total RAM|Free RAM|Used RAM|Lost RAM'" \
  "echo '== processes'; ps -A | wc -l" \
  "echo '== cpu'; top -b -n 1 | head -14" \
  "echo '== disabled packages'; pm list packages -d | wc -l" \
  "echo '== on screen'; dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'; dumpsys dreams 2> /dev/null | grep -m1 -iE 'dreaming|mCurrentDream'" \
  "echo '== biggest'; dumpsys meminfo | sed -n '/Total [PR]SS by process/,/^$/p' | head -16" \
  "echo '== tweaks'; for k in window_animation_scale transition_animation_scale animator_duration_scale cached_apps_freezer; do echo \"\$k=\$(settings get global \$k)\"; done; echo \"max_cached_processes=\$(device_config get activity_manager max_cached_processes)\"" \
  "echo '== our apps'; for p in sa.urtv.screensaver sa.urtv.home; do echo \"\$p: \$(dumpsys meminfo \$p 2> /dev/null | grep -m1 -E '^ *TOTAL')\"; done" \
  | tv --batch > "$out"
[ "$mode" = --screensaver ] && tv "input keyevent KEYCODE_HOME" > /dev/null

kb() { local v; v=$(sed -n "s/^$1:[[:space:]]*\([0-9][0-9]*\).*/\1/p" "$out" | head -1); echo "${v:-0}"; }   # the first number on that exact line; 0 if the TV has none
avail=$(( $(kb MemAvailable) / 1024 )); total=$(( $(kb MemTotal) / 1024 ))
swap=$(( ($(kb SwapTotal) - $(kb SwapFree)) / 1024 ))
procs=$(sed -n '/== processes/{n;n;p;}' "$out" | tr -dc '0-9')
cpus=$(grep -m1 -oE '[0-9]+%cpu' "$out" | tr -dc '0-9' || true)        # e.g. 400%cpu = 4 cores
idle=$(grep -m1 -oE '[0-9]+%idle' "$out" | tr -dc '0-9' || true)       # summed over the cores
[ -n "$cpus" ] && [ -n "$idle" ] && [ "$cpus" -gt 0 ] && idle=$(( idle * 100 / cpus ))
disabled=$(sed -n '/== disabled packages/{n;n;p;}' "$out" | tr -dc '0-9')
if [ "$avail" -eq 0 ] || [ "$total" -eq 0 ] || [ "$avail" -gt "$total" ]; then   # a TV whose meminfo we misread (one said 292396 MB free of 1683)
  avail="not measured (memory read failed)"; total=
fi
[ -f $RUN/summary.md ] || printf '| state | free RAM (MB%s) | swap used (MB) | processes | CPU idle | disabled packages |\n|---|---|---|---|---|---|\n' "${total:+ of $total}" > $RUN/summary.md
if [ -f $RUN/summary.md ] && grep -q "^| $label |" $RUN/summary.md; then   # a repeated reading replaces its row
  grep -v "^| $label |" $RUN/summary.md > $RUN/summary.md.tmp || true
  mv $RUN/summary.md.tmp $RUN/summary.md
fi
printf '| %s | %s | %s | %s | %s | %s |\n' "$label" "$avail" "$swap" "$procs" "${idle:+$idle%}" "$disabled" | tee -a $RUN/summary.md
# our two apps' own memory (TOTAL PSS, MB), one line under the table; the table's columns stay as they are
ours() { local v; v=$(sed -nE "s/^$1:[[:space:]]*TOTAL( PSS:)?[[:space:]]*([0-9]+).*/\2/p" "$out" | head -1); if [ -n "$v" ]; then echo "$(( v / 1024 )) MB"; else echo "not running"; fi; }
echo "tweaks at this reading: $(sed -n '/^== tweaks/,/^== our apps/p' "$out" | grep = | tr -d '\r' | paste -sd' ' -)"
echo "our apps (TOTAL PSS): screensaver $(ours sa.urtv.screensaver), home $(ours sa.urtv.home)"
