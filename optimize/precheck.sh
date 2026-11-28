#!/usr/bin/env bash
# Is this device an Android TV (leanback), and is its WebView 80 or newer? The first step of optimize/run.sh.
#   optimize/precheck.sh              the check: prints what to tell the person; exit 3 if it is not an Android TV, 6 if its Android is older than 8 (stop)
#   optimize/precheck.sh report       the «Device:» and «WebView:» lines for report.md
#   optimize/precheck.sh webview-store   opens «Android System WebView» in the TV's Play Store, for the person to update it
# Only reads from the TV (except webview-store, which opens a page).
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
if [ "${1:-}" = webview-store ]; then
  "$PY" -I optimize/tv.py "am start -a android.intent.action.VIEW -d market://details?id=com.google.android.webview" > /dev/null
  echo "Opened «Android System WebView» in the TV's Play Store: the person presses Update, then optimize/precheck.sh again."
  exit 0
fi
ask() {
  printf '%s\n' \
    "pm list features" \
    "getprop ro.product.manufacturer" \
    "dumpsys webviewupdate" \
    "getprop ro.build.version.sdk" \
    "getprop ro.build.version.release" \
    'for p in $(pm list packages webview); do echo "${p#package:} $(dumpsys package ${p#package:} | grep versionName | sed -n 1p)"; done' \
    | "$PY" -I optimize/tv.py --batch
}
answers=$(ask)
# 4.4 (reports: an empty answer read as «not an Android TV» or «did not say what it is»): no feature line, or no maker, is asked again on a new
# connection (each tv.py is one), twice at most, before precheck.py judges it
for _ in 1 2; do
  grep -q '^feature:' <<< "$answers" && [ -n "$(sed -n '/^\$ getprop ro.product.manufacturer$/{n;p;}' <<< "$answers" | tr -d '\r[:space:]')" ] && break
  echo "(the TV gave an empty answer about what it is: asking again)" >&2
  sleep "${PRECHECK_WAIT_S:-3}"
  answers=$(ask)
done
exec "$PY" -I optimize/precheck.py "${1:-check}" <<< "$answers"
