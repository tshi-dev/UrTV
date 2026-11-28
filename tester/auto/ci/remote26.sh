#!/usr/bin/env bash
# Runs on the CI emulator at Android 8 (API 26), after the main round (.github/workflows/auto-round.yml). Review round 3, report row 765 (a TCL OTT box on
# Android 8.0: after the install «the remote moves briefly, then nothing»): the home must keep answering the D-pad on Android 8, with and without the add-on's
# accessibility service switched on.
#   remote26.sh HOME_APK EXTRAS_APK OUT_DIR      (EXTRAS_APK «none» on a release that has no add-on: 4.3.x, where the service is the home's own TypeService)
# Two passes: A) the home alone, B) the add-on installed and its service enabled. Each pass presses the D-pad 30 times in the home. A key that the system
# does not answer in 8 seconds (adb's input waits for the key to be finished) is a freeze; the focus has to land on at least 3 different places (a frozen remote gives 1; run 1: 5 and 3). The verdict
# is in OUT_DIR/remote26/result.txt («PASS» or «FAIL …»). Never run this on a person's machine.
set +e
HOME_APK=$1; EXTRAS_APK=$2; OUT=$3/remote26
cd "$(dirname "$0")/../../.."
mkdir -p "$OUT"
exec > >(tee -a "$OUT/ci.log") 2>&1
adb wait-for-device
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
echo "api $SDK"
[ "$SDK" = 26 ] || { echo "FAIL: this emulator is API $SDK, not 26" > "$OUT/result.txt"; exit 1; }
[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ] || { echo "FAIL: this is not an emulator (ro.kernel.qemu is not 1): the script installs debug builds and switches an accessibility service on, so it runs only on the CI emulator" > "$OUT/result.txt"; exit 1; }
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"
adb shell cmd package set-home-activity sa.urtv.home/.MainActivity > /dev/null 2>&1

# one pass: 30 keys; the focused place after each (uiautomator), the longest answer
walk() {
  tag=$1; slow=0; places=""; keys="DPAD_DOWN DPAD_DOWN DPAD_RIGHT DPAD_RIGHT DPAD_DOWN DPAD_LEFT DPAD_UP DPAD_RIGHT DPAD_DOWN DPAD_DOWN"
  adb shell input keyevent KEYCODE_HOME; sleep 3
  adb shell am start -W -n sa.urtv.home/.MainActivity > /dev/null 2>&1; sleep 10
  n=0
  for round in 1 2 3; do
    for k in $keys; do
      n=$((n + 1))
      t0=$(date +%s)
      timeout 20 adb shell input keyevent KEYCODE_$k
      t1=$(date +%s)
      [ $((t1 - t0)) -ge 8 ] && { slow=$((slow + 1)); echo "$tag key $n ($k): the system took $((t1 - t0)) s to answer"; }
      sleep 1
      adb shell uiautomator dump /sdcard/r26.xml > /dev/null 2>&1
      adb pull /sdcard/r26.xml "$OUT/$tag-$n.xml" > /dev/null 2>&1
      f=$(grep -o '<node[^>]*focused="true"[^>]*>' "$OUT/$tag-$n.xml" 2>/dev/null | grep -o 'bounds="[^"]*"' | head -n 1)
      places="$places
$f"
    done
  done
  distinct=$(echo "$places" | sed '/^$/d' | sort -u | wc -l)
  adb exec-out screencap -p > "$OUT/$tag.png" 2> /dev/null
  echo "$tag: $n keys, $slow slow, $distinct different focused places"
  [ "$slow" = 0 ] && [ "$distinct" -ge 3 ]
}

fail=""
walk home || fail="$fail the home alone (see home-*.xml, home.png);"
if [ "$EXTRAS_APK" = none ]; then SERVICE=sa.urtv.home/sa.urtv.home.TypeService
else SERVICE=sa.urtv.extras/sa.urtv.home.ExtrasA11yService; adb install -r -g "$EXTRAS_APK" || adb install -r "$EXTRAS_APK"; fi
adb shell settings put secure enabled_accessibility_services "$SERVICE"
adb shell settings put secure accessibility_enabled 1
sleep 8
echo "services: $(adb shell settings get secure enabled_accessibility_services | tr -d '\r')"
walk addon || fail="$fail the home with the add-on's service on (see addon-*.xml, addon.png);"
adb logcat -d -b crash > "$OUT/crash.log" 2>&1
if [ -z "$fail" ]; then echo "PASS: on Android 8 the D-pad moves the focus in the home, alone and with the add-on's service on (30 keys each, none held)" > "$OUT/result.txt"
else echo "FAIL: on Android 8 the remote froze or the focus did not move:$fail" > "$OUT/result.txt"; fi
cat "$OUT/result.txt"
grep -q '^PASS' "$OUT/result.txt"
