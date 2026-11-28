#!/usr/bin/env bash
# Does Home bring back the new home screen from inside an app? Opens an app, presses Home, and says what is in
# front (and fails if no app could be opened). Prints «OK: Home opens تلفزيونك» or «!! Home opened …».
#   optimize/check-home.sh [package]      (default: YouTube, else the first app of the favourites row)
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
tv() { "$PY" -I optimize/tv.py "$@"; }
front_now() { tv "dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'" | tr -d '\r'; }
# 4.4: a slow TV takes more than 8 s to bring an app (or the home) to the front: the front is read once a second for up to
# WAIT seconds (30), and an app that is not in front by then is started once more before it counts as «could not be opened»
WAIT="${CHECK_HOME_WAIT:-30}"
STEP="${CHECK_HOME_STEP:-1}"
in_front() {   # $1 what to look for in the front activity: 0 the moment it is there, 1 after WAIT seconds without it
  local i=0
  while :; do
    grep -qF "$1" <<< "$(front_now)" && return 0
    [ "$i" -lt "$WAIT" ] || return 1
    i=$((i + 1)); sleep "$STEP"
  done
}
opened() {   # $1 package: started and in front (monkey says "No activities found" for an app without a launcher entry)
  local out try
  for try in 1 2; do
    out=$(tv "monkey -p $1 1" 2>&1 || true)
    ! grep -q 'No activities found' <<< "$out" || return 1
    in_front "$1" && return 0
    [ "$try" = 2 ] || echo "   ($1 not in front after ${WAIT} s: started once more)"
  done
  return 1
}
if [ -n "${1:-}" ]; then candidates=("$1")
else
  candidates=(com.google.android.youtube.tv)
  # the first app of the favourites row (dock.sh show prints "dock: pkg1,pkg2,...")
  # shellcheck disable=SC2207
  candidates+=($(optimize/dock.sh show 2> /dev/null | sed -n 's/^dock: //p' | cut -d' ' -f1 | tr ',' '\n' \
                 | grep -v '^sa\.urtv\.' | sed -n 1p || true))
fi
app=
for c in "${candidates[@]}"; do
  if opened "$c"; then app=$c; break; fi
done
if [ -z "$app" ]; then echo "!! No app could be opened (tried ${candidates[*]}): Home was not tested"; exit 1; fi
home_ok=no
for try in 1 2; do   # Home pressed, and once more if the home is not in front after WAIT seconds
  tv "input keyevent KEYCODE_HOME" > /dev/null
  if in_front sa.urtv.home; then home_ok=yes; break; fi
done
front=$(front_now)
if [ "$home_ok" = yes ]; then
  echo "OK: Home opens تلفزيونك"
else
  echo "!! Home opened: ${front:-nothing}"
  exit 1
fi
