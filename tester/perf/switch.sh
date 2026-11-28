#!/usr/bin/env bash
# App-switch numbers on a TV or the CI emulator (4.4 beta 4, reports of lag and hangs switching apps): the home goes out to Settings and back ROUNDS
# times; each round keeps `am start -W` of the leave (the home's onPause is in its WaitTime), of the return (the home's resume and first frame) and
# `dumpsys gfxinfo` of the 3 s after the return; then tester/perf/switch.py prints them and the medians (and judges them when given limits).
#   switch.sh OUT_DIR [ROUNDS] [-- limits for switch.py]       ADB: the adb command (default: adb), e.g. ADB="adb -s emulator-5554"
# The TV is left on the home. On a person's TV only with the owner's go-ahead (it opens Settings ROUNDS times).
set +e
OUT=$1; ROUNDS=${2:-5}; shift; [ $# -gt 0 ] && shift; [ "${1:-}" = "--" ] && shift
ADB=${ADB:-adb}
cd "$(dirname "$0")/../.."
mkdir -p "$OUT"
case "$ROUNDS" in ''|*[!0-9]*) echo "!! switch.sh: ROUNDS must be a number"; exit 2;; esac
$ADB shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME > /dev/null
sleep 5
for i in $(seq 1 "$ROUNDS"); do
  $ADB shell am start -W -a android.settings.SETTINGS > "$OUT/leave-$i.txt" 2>&1
  sleep 4
  $ADB shell dumpsys gfxinfo sa.urtv.home reset > /dev/null 2>&1
  $ADB shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME > "$OUT/back-$i.txt" 2>&1
  sleep 3
  $ADB shell dumpsys gfxinfo sa.urtv.home > "$OUT/gfx-$i.txt" 2>&1
done
$ADB logcat -d -s StrictMode UrTVHome 2>/dev/null | grep -E "StrictMode|resume to" | tail -n 200 > "$OUT/strictmode.txt"   # a debug build: the main thread's disk and slow calls
python3 -I tester/perf/switch.py "$OUT" "$@" | tee "$OUT/switch.txt"
exit "${PIPESTATUS[0]}"
