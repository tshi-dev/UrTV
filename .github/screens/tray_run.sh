#!/usr/bin/env bash
# On the CI emulator: installs both apps and the stubs, then runs the rendered tray check (the emulator action runs each script line on its own, so one file).
#   tray_run.sh SAVER_APK HOME_APK STUBS_DIR OUT_DIR
set -e
adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb install -r -g "$1" || adb install -r "$1"
adb install -r -g "$2" || adb install -r "$2"
for s in "$3"/*.apk; do adb install -r "$s"; done
mkdir -p "$4"
python3 .github/screens/tray_check.py "$4"
