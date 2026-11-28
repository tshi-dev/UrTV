#!/usr/bin/env bash
# Does Home bring back the new home screen from inside an app? Opens an app, presses Home, and says what is in
# front (and fails if no app could be opened). Prints «OK: Home opens تلفزيونك» or «!! Home opened …».
#   optimize/check-home.sh [package]      (default: YouTube, else the first app of the favourites row)
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
tv() { "$PY" -I optimize/tv.py "$@"; }
front_now() { tv "dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'" | tr -d '\r'; }
opened() {   # $1 package: started and in front (monkey says "No activities found" for an app without a launcher entry)
  local out
  out=$(tv "monkey -p $1 1" 2>&1 || true)
  ! grep -q 'No activities found' <<< "$out" || return 1
  sleep 8
  grep -q "$1" <<< "$(front_now)"
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
tv "input keyevent KEYCODE_HOME" > /dev/null
sleep 4
front=$(front_now)
if grep -q sa.urtv.home <<< "$front"; then
  echo "OK: Home opens تلفزيونك"
else
  echo "!! Home opened: ${front:-nothing}"
  exit 1
fi
