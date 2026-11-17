#!/usr/bin/env bash
# Runs on the CI emulator (.github/workflows/ui-measure.yml): records the screen while the home changes look, for docs/ui/STUDY.md's audit.
#   measure.sh SAVER_APK HOME_APK OUT_DIR
# For each case: a screenrecord, the device time of the change, the activity events (a relaunch or a recreate shows there), the WebView's
# loads, and the focused view before and after (uiautomator). .github/ui/frames.py reads the videos afterwards. Best effort: nothing fails the run.
set +e
SAVER_APK=$1; HOME_APK=$2; OUT=$3
mkdir -p "$OUT"
exec > >(tee -a "$OUT/log.txt") 2>&1

adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell wm size 1920x1080
adb shell wm density 320
adb install -r -g "$SAVER_APK" || adb install -r "$SAVER_APK"
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"
for s in stubs/*.apk; do adb install -r "$s" > /dev/null; done

saver() { adb shell "date +%s.%N > /sdcard/t_change; am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET $*" > /dev/null; adb shell cat /sdcard/t_change; }
start() { adb shell am start -W --activity-clear-task -n "$1" > /dev/null; }
focus() { adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; adb shell cat /sdcard/ui.xml | grep -o '<node[^>]*focused="true"[^>]*>' | sed -E 's/.*resource-id="([^"]*)".*class="([^"]*)".*bounds="([^"]*)".*/\1 \2 \3/' | head -1; }

# one case: NAME SECONDS SETTING...   (the home is in front; the change comes from ADB like the phone or dock.sh, so the home is not paused)
case_front() {
  name=$1; secs=$2; shift 2
  echo "== $name"
  echo "focus-before: $(focus)" > "$OUT/$name.txt"
  adb logcat -c; adb logcat -b events -c
  adb shell "date +%s.%N > /sdcard/t_rec; screenrecord --bit-rate 6000000 --time-limit $secs /sdcard/$name.mp4" &
  rec=$!
  sleep 2
  echo "t_change: $(saver "$@")" >> "$OUT/$name.txt"
  wait $rec
  echo "t_rec: $(adb shell cat /sdcard/t_rec)" >> "$OUT/$name.txt"
  echo "focus-after: $(focus)" >> "$OUT/$name.txt"
  adb pull "/sdcard/$name.mp4" "$OUT/$name.mp4" > /dev/null
  adb shell rm "/sdcard/$name.mp4"
  adb logcat -b events -d -v epoch | grep -E "wm_(on_create|on_resume|on_pause|relaunch|on_restart|on_destroy)|am_relaunch|am_create_activity|am_restart" > "$OUT/$name.events.txt"
  adb logcat -d -v epoch | grep -iE "chromium|cr_|WebView|loadUrl|Choreographer.*Skipped|recreate|sa\.urtv" | head -400 > "$OUT/$name.logcat.txt"
  adb exec-out screencap -p > "$OUT/$name-end.png"
}

# the person's own path: the settings in front, the change made there, then Back to the home
case_back() {
  name=$1; secs=$2; shift 2
  echo "== $name"
  start sa.urtv.screensaver/.SettingsActivity
  sleep 6
  saver "$@" > /dev/null
  sleep 1
  adb logcat -c; adb logcat -b events -c
  adb shell "date +%s.%N > /sdcard/t_rec; screenrecord --bit-rate 6000000 --time-limit $secs /sdcard/$name.mp4" &
  rec=$!
  sleep 2
  echo "t_change: $(adb shell 'date +%s.%N; input keyevent BACK' | head -1)" > "$OUT/$name.txt"
  wait $rec
  echo "t_rec: $(adb shell cat /sdcard/t_rec)" >> "$OUT/$name.txt"
  echo "focus-after: $(focus)" >> "$OUT/$name.txt"
  adb pull "/sdcard/$name.mp4" "$OUT/$name.mp4" > /dev/null
  adb shell rm "/sdcard/$name.mp4"
  adb logcat -b events -d -v epoch | grep -E "wm_(on_create|on_resume|on_pause|relaunch|on_restart|on_destroy)|am_relaunch|am_create_activity|am_restart" > "$OUT/$name.events.txt"
  adb logcat -d -v epoch | grep -iE "chromium|cr_|WebView|loadUrl|Choreographer.*Skipped|recreate|sa\.urtv" | head -400 > "$OUT/$name.logcat.txt"
  adb exec-out screencap -p > "$OUT/$name-end.png"
}

# inside the settings: the change made while they are in front (what the person sees there: the row, any preview)
case_settings() {
  name=$1; secs=$2; shift 2
  echo "== $name"
  start sa.urtv.screensaver/.SettingsActivity
  sleep 6
  echo "focus-before: $(focus)" > "$OUT/$name.txt"
  adb logcat -c; adb logcat -b events -c
  adb shell "date +%s.%N > /sdcard/t_rec; screenrecord --bit-rate 6000000 --time-limit $secs /sdcard/$name.mp4" &
  rec=$!
  sleep 2
  echo "t_change: $(saver "$@")" >> "$OUT/$name.txt"
  wait $rec
  echo "t_rec: $(adb shell cat /sdcard/t_rec)" >> "$OUT/$name.txt"
  echo "focus-after: $(focus)" >> "$OUT/$name.txt"
  adb pull "/sdcard/$name.mp4" "$OUT/$name.mp4" > /dev/null
  adb shell rm "/sdcard/$name.mp4"
  adb logcat -b events -d -v epoch | grep -E "wm_(on_create|on_resume|on_pause|relaunch|on_restart|on_destroy)|am_relaunch|am_create_activity|am_restart" > "$OUT/$name.events.txt"
  adb logcat -d -v epoch | grep -iE "chromium|cr_|WebView|loadUrl|Choreographer.*Skipped|recreate|sa\.urtv" | head -400 > "$OUT/$name.logcat.txt"
  adb exec-out screencap -p > "$OUT/$name-end.png"
}

# a known start: «شكلك المعتاد», photos, the plain clock, the coloured tiles; a card so the home is not empty
adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET --es preset tv --es home_panel prayerrow,hourly" > /dev/null
start sa.urtv.home/.MainActivity
sleep 15
adb exec-out screencap -p > "$OUT/start.png"
key() { for k in "$@"; do adb shell input keyevent "$k"; sleep 0.6; done; }
pk=$(adb shell pm list packages urtv.stub | sed "s/package://" | tr -d "\r" | head -7 | paste -sd,)
adb shell "am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SET_DOCK --es apps $pk" > /dev/null
sleep 3
key DPAD_DOWN   # the focus on the apps row, so a lost focus shows

# the home in front, the change from outside (70 s: the home may only read it at its next minute)
case_front 01-preset-tv-to-couch 70 --es preset couch
case_front 02-preset-couch-to-tv 70 --es preset tv
case_front 03-bg-photos-to-scenes 70 --es background scenes
case_front 04-bg-scenes-to-photos 70 --es background photos
case_front 05-apps-tiles-to-banners 70 --es appsstyle banners
case_front 06-apps-banners-to-icons 70 --es appsstyle icons
case_front 07-apps-icons-to-tiles 70 --es appsstyle tiles
case_front 08-clock-plain-to-glass 70 --es clock glass
case_front 09-clock-glass-to-plain 70 --es clock plain
case_front 10-layout-classic-to-football 70 --es homelayout football
case_front 11-layout-football-to-classic 70 --es homelayout classic
# the person's path: chosen in the settings, then Back
case_back 20-back-after-couch 12 --es preset couch
case_back 21-back-after-tv 12 --es preset tv
case_back 22-back-after-apps-banners 12 --es appsstyle banners
case_back 23-back-after-apps-tiles 12 --es appsstyle tiles
# inside the settings: does the screen itself change at once (its rows, its look, its preview)
case_settings 30-settings-preset-couch 12 --es preset couch
case_settings 31-settings-preset-tv 12 --es preset tv
echo done
