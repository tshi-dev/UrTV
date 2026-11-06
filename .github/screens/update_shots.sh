#!/usr/bin/env bash
# On the CI emulator (.github/workflows/update-shots.yml): pictures of «تحديث تلفزيوني» before (master's layout) and after (this branch's), in the glass and sand looks,
# Arabic and English, in four states. Debug builds only: the receiver DebugUpdateShot draws a state without the network.
#   update_shots.sh SAVER_APK BEFORE_LAUNCHER_APK AFTER_LAUNCHER_APK OUT_DIR
set -e
timeout 120 adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell wm size 1920x1080
adb shell wm density 320
timeout 180 adb install -r -g "$1" || timeout 180 adb install -r "$1"
OUT="$4"; mkdir -p "$OUT"
saver() { adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET $*" > /dev/null; }
for which in before after; do
  apk="$2"; [ "$which" = after ] && apk="$3"
  timeout 180 adb install -r -g "$apk" || timeout 180 adb install -r "$apk"
  for look in glass sand; do
    for lang in ar en; do
      saver --es look "$look" --es lang "$lang"
      sleep 2
      for state in available downloading waiting_owner done; do
        adb shell am force-stop sa.urtv.home
        adb shell "am broadcast -n sa.urtv.home/.DebugUpdateShot --es preview $state" > /dev/null
        sleep 4
        adb exec-out screencap -p > "$OUT/$which-$look-$lang-$state.png"
        echo "shot $which-$look-$lang-$state $(stat -c %s "$OUT/$which-$look-$lang-$state.png")"
      done
    done
  done
done
saver --es look glass --es lang ar
