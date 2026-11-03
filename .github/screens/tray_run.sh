#!/usr/bin/env bash
# On the CI emulator: installs both apps and the stubs, then runs the rendered tray check (the emulator action runs each script line on its own, so one file).
#   tray_run.sh SAVER_APK HOME_APK STUBS_DIR OUT_DIR
set -e
timeout 120 adb wait-for-device
echo "tray_run: device is up"
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
timeout 180 adb install -r -g "$1" || timeout 180 adb install -r "$1"
timeout 180 adb install -r -g "$2" || timeout 180 adb install -r "$2"
for s in "$3"/*.apk; do timeout 120 adb install -r "$s"; done
echo "tray_run: apps installed, starting the check"
mkdir -p "$4"
timeout 1100 python3 -u .github/screens/tray_check.py "$4"
