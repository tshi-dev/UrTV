#!/usr/bin/env bash
# Runs on the CI emulator at Android 10 (API 29), after the main round (.github/workflows/auto-round.yml). Review F97: «أضف من فلاشة أو التنزيلات» lists the photos
# of the TV's Downloads; on Android 10 an app that targets 29 or more sees none of them through File.listFiles unless it asks for the legacy storage view.
#   storage29.sh HOME_APK OUT_DIR
# It puts a photo in /sdcard/Download, gives the home the read permission, opens the screen (as root: the screen is not exported) and says in OUT_DIR/storage29/result.txt
# whether the list found it («PASS» or «FAIL …»). Never run this on a person's machine.
set +e
HOME_APK=$1; OUT=$2/storage29
cd "$(dirname "$0")/../../.."
mkdir -p "$OUT"
exec > >(tee -a "$OUT/ci.log") 2>&1
adb wait-for-device
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
echo "api $SDK"
[ "$SDK" = 29 ] || { echo "FAIL: this emulator is API $SDK, not 29" > "$OUT/result.txt"; exit 1; }
[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ] || { echo "FAIL: this is not an emulator (ro.kernel.qemu is not 1): the script installs a debug build, grants permissions and roots the device, so it runs only on the CI emulator (review F130)" > "$OUT/result.txt"; exit 1; }
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"
adb root > /dev/null 2>&1; adb wait-for-device
adb push assets/backgrounds/9hWJs7iblh8.jpg /sdcard/Download/urtv-ci-photo.jpg
adb shell pm grant sa.urtv.home android.permission.READ_EXTERNAL_STORAGE || echo "(the permission could not be granted)"
adb shell am start -n sa.urtv.home/.MainActivity > /dev/null 2>&1; sleep 8   # the home in front: Android 10 refuses a screen started by a background process
adb shell am start -n sa.urtv.home/.PhotoAddActivity 2>&1 | tee "$OUT/start.txt"
if grep -qi "permission denial\|not exported" "$OUT/start.txt"; then   # adb root was refused: the debug build lets the shell act as the app, which may start its own screens
  adb shell run-as sa.urtv.home am start --user 0 -n sa.urtv.home/.PhotoAddActivity 2>&1 | tee "$OUT/start.txt"
fi
sleep 12
adb shell uiautomator dump /sdcard/photos.xml > /dev/null 2>&1
adb pull /sdcard/photos.xml "$OUT/photos.xml" > /dev/null 2>&1
adb exec-out screencap -p > "$OUT/photos.png" 2> /dev/null
if [ ! -s "$OUT/photos.xml" ]; then echo "FAIL: the screen could not be read" > "$OUT/result.txt"
elif grep -qi "permission denial\|not exported" "$OUT/start.txt"; then echo "FAIL: the screen could not be opened from the shell (not exported, and adb root was refused): the check cannot tell" > "$OUT/result.txt"
elif grep -q "لا صور هنا" "$OUT/photos.xml"; then echo "FAIL: Android 10 lists no photo in /sdcard/Download («لا صور هنا»): the home needs requestLegacyExternalStorage (review F97)" > "$OUT/result.txt"
elif grep -q "اضغط" "$OUT/photos.xml"; then echo "PASS: the photo in Downloads is listed on Android 10" > "$OUT/result.txt"
else echo "FAIL: the screen shows neither the photos nor the empty note (see photos.xml)" > "$OUT/result.txt"; fi
cat "$OUT/result.txt"
grep -q '^PASS' "$OUT/result.txt"
