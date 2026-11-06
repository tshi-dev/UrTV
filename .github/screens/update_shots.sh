#!/usr/bin/env bash
# On the CI emulator (.github/workflows/update-shots.yml): pictures of «تحديث تلفزيوني» before (master's layout) and after (this branch's), in the glass and sand looks,
# Arabic and English, in four states. Debug builds only: the activity DebugUpdateShot draws a state without the network. A shot is taken only while UpdateActivity is the resumed activity; otherwise the script fails.
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
        name="$which-$look-$lang-$state"
        adb shell am force-stop sa.urtv.home
        adb shell am start -n sa.urtv.home/.DebugUpdateShot --es preview "$state" > /dev/null
        # the screen must be OURS and resumed before a picture is taken (the first run photographed the emulator's own home, with third parties' thumbnails in it)
        ok=0
        for i in $(seq 1 30); do
          if adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -q "sa.urtv.home/.UpdateActivity"; then ok=1; break; fi
          sleep 1
        done
        if [ "$ok" != 1 ]; then
          echo "FAIL $name: the top activity is not UpdateActivity:"
          adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" || true
          exit 1
        fi
        sleep 2
        adb exec-out screencap -p > "$OUT/$name.png.tmp"
        if ! adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -q "sa.urtv.home/.UpdateActivity"; then
          echo "FAIL $name: the top activity changed during the shot"; rm -f "$OUT/$name.png.tmp"; exit 1
        fi
        mv "$OUT/$name.png.tmp" "$OUT/$name.png"
        echo "shot $name $(stat -c %s "$OUT/$name.png")"
      done
    done
  done
done
saver --es look glass --es lang ar
