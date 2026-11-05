#!/usr/bin/env bash
# On the CI emulator: installs both apps and the stubs, then runs the rendered tray check (the emulator action runs each script line on its own, so one file).
#   tray_run.sh SAVER_APK HOME_APK STUBS_DIR OUT_DIR
set -e
timeout 120 adb wait-for-device
echo "tray_run: device is up"
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
timeout 180 adb install -r -g "$1" || timeout 180 adb install -r "$1"
if ! timeout 150 adb install -r -g "$2"; then
  echo "tray_run: the home app's install did not finish; what the package manager says:"
  adb logcat -d -t 80 -s PackageManager:V PackageInstaller:V ActivityManager:W 2>&1 | tail -40 || true
  adb shell dumpsys package sa.urtv.home 2>&1 | head -5 || true
  timeout 150 adb install -r "$2" || { echo "tray_run: the home app could not be installed"; exit 1; }
fi
for s in "$3"/*.apk; do timeout 120 adb install -r "$s"; done
echo "tray_run: apps installed, starting the check"
mkdir -p "$4"
timeout 2760 python3 -u .github/screens/tray_check.py "$4"
