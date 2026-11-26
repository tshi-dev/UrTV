#!/usr/bin/env bash
# Start the TV's screensaver now, the ways Android allows over ADB, and say whether it really runs.
#   optimize/start-saver.sh             prints «dream» (it runs) or «not started»; exit 1 if not started
#   optimize/start-saver.sh --preview   if Android won't start it (some Android 14 sets refuse ADB), show
#                                       شاشة تلفزيونك's full-screen preview instead: the same page; prints «preview»
# Leave it with Home or Back.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
tv() { "$PY" -I optimize/tv.py "$@"; }

dreaming() {   # Android 11+ runs a dream in DreamActivity; dumpsys dreams has mCurrentDream (14) or
  local s      # mCurrentDreamToken (older), null while none runs
  s=$(tv "dumpsys dreams | grep -E 'mCurrentDream(Token)?='; dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'")
  grep -qE 'DreamActivity|mCurrentDream(Token)?=[^n ]' <<< "$s"
}

# a TV can take a long while to start its dream (the tester's TCL: 8 s with nothing, up to about 41 s): ask every 2 s until it runs, for up to
# SAVER_WAIT_S seconds (60), never a fixed 6 s after which a dream that starts at 8 s was said «not started»
wait_dream() {   # $1: how many seconds at most; true as soon as a dream runs
  local waited=0
  while [ "$waited" -lt "$1" ]; do
    sleep "${SAVER_POLL_S:-2}"
    waited=$((waited + ${SAVER_POLL_S:-2}))
    if dreaming; then return 0; fi
  done
  return 1
}

tv "am start -n com.android.systemui/.Somnambulator" > /dev/null
if wait_dream "${SAVER_WAIT_S:-60}"; then echo dream; exit 0; fi
tv "cmd dreams start-dreaming" > /dev/null 2>&1 || true    # Android 14's own way; refused on some sets
if wait_dream "${SAVER_WAIT2_S:-20}"; then echo dream; exit 0; fi
if [ "${1:-}" = --preview ]; then
  # a fresh preview each time (one already open would come back as it was, with the settings it opened with)
  tv "am start --activity-clear-task -n sa.urtv.screensaver/.MainActivity" > /dev/null
  echo preview
  exit 0
fi
echo "not started"
exit 1
