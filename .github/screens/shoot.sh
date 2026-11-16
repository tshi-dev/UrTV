#!/usr/bin/env bash
# Runs on the CI emulator (see .github/workflows/screens.yml): installs both apps and takes the pictures with screencap.
#   shoot.sh SAVER_APK HOME_APK OUT_DIR
# Every shot is best effort: the remote's keys go in one by one and what is on screen is saved under a fixed name.
set +e
SAVER_APK=$1; HOME_APK=$2; OUT=$3
mkdir -p "$OUT"
LOG="$OUT/log.txt"
exec > >(tee -a "$LOG") 2>&1

adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell wm size 1920x1080
adb shell wm density 320
adb install -r -g "$SAVER_APK" || adb install -r "$SAVER_APK"
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"

key() { for k in "$@"; do adb shell input keyevent "$k"; sleep 0.6; done; }
shot() { sleep "${2:-1.5}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1 $(stat -c %s "$OUT/$1.png" 2>/dev/null)"; }
start() { adb shell am start -W --activity-clear-task -n "$1" > /dev/null; }
saver() { adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET $*" > /dev/null; }   # values with no spaces
task() { adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.TASKS --es op add --es text '$1'" > /dev/null; }
music() { adb shell "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a $*" > /dev/null; }

# ---- the settings that make the screens show something: a card on the home screen, three places for cards, the matches of two clubs
saver --es teams 136013:Al-Hilal --es home_panel matches,prayerrow,hourly --es panel prayers --es cardstyle glass1
sleep 2

# ---- the home screen, then every pill with the focus (the row is left to right in the reading order: the keys go through it)
start sa.urtv.home/.MainActivity
sleep 12
shot home-00-idle 2
key DPAD_UP DPAD_UP DPAD_UP
shot home-01-top 1
for i in 1 2 3 4 5 6 7 8; do
  key DPAD_RIGHT
  shot "home-pill-$i" 0.8
done
key DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT
for i in 1 2 3 4; do
  key DPAD_LEFT
  shot "home-pill-left-$i" 0.8
done
# the tasks card (the first card of the home screen's cards is «المهام» only when it is chosen; a second try with it chosen)
saver --es home_panel tasks,matches,hourly
task "اتصل بالسباك"
task "شراء حليب وخبز"
start sa.urtv.home/.MainActivity
sleep 8
key DPAD_DOWN
shot home-tasks-card-focused 1.5
saver --es home_panel matches,prayerrow,hourly

# ---- «تلاوة وأذان»: readers, a surah grid, the player (with a surah playing: the stream needs the network), the adhan list and the volume row
start sa.urtv.home/.tilawah.MainActivity
shot tilawah-01-readers 3
key DPAD_DOWN DPAD_DOWN
shot tilawah-02-readers-down 1
key DPAD_CENTER
shot tilawah-03-surah-grid 3
key DPAD_RIGHT DPAD_RIGHT DPAD_DOWN
shot tilawah-04-surah-grid-focus 1
key DPAD_CENTER
shot tilawah-05-player 8
key BACK BACK
start sa.urtv.home/.tilawah.MainActivity
key DPAD_UP DPAD_RIGHT DPAD_RIGHT DPAD_DOWN
shot tilawah-06-stations-or-adhan-tab 2
key DPAD_UP DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_DOWN
shot tilawah-07-adhan-tab 2
key DPAD_DOWN DPAD_DOWN
shot tilawah-08-adhan-volume-row 1
start sa.urtv.home/.tilawah.AdhanActivity
shot tilawah-09-adhan-voices 3
key DPAD_DOWN DPAD_DOWN DPAD_DOWN
shot tilawah-10-adhan-voices-focus 1

# ---- a surah playing: the home screen's «يُتلى الآن» card and the screensaver's own
music sa.urtv.home.PLAY_SURAH --es reciter sudais --ei surah 18
sleep 10
start sa.urtv.home/.MainActivity
sleep 4
shot home-now-playing 2
key DPAD_UP DPAD_UP DPAD_UP DPAD_LEFT
shot home-now-playing-card-focused 1
saver --es panel matches --es cardstyle dark3
adb shell am start -W --activity-clear-task -n sa.urtv.screensaver/.MainActivity > /dev/null
sleep 25
shot saver-now-playing 2
music sa.urtv.home.STOP

# ---- the settings (4.3.2): the first screen, then a section's rows; the focus starts on the wide «المدينة»; ▼ goes to «الأذان» (the right one of the first pair),
# ◀ to «المظهر»; each Back returns to the button it came from
start sa.urtv.screensaver/.SettingsActivity
shot settings-01-top 3
key DPAD_DOWN DPAD_LEFT DPAD_CENTER
shot settings-02-look 3
for i in 1 2 3 4 5 6 7 8 9; do key DPAD_DOWN; done
shot settings-03-look-further 1
key BACK DPAD_DOWN DPAD_CENTER
shot settings-04-saver 3
key BACK DPAD_RIGHT DPAD_CENTER
shot settings-05-home 3
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do key DPAD_DOWN; done
shot settings-06-home-further 1
key BACK DPAD_DOWN DPAD_CENTER
shot settings-07-prayer 3
key BACK DPAD_LEFT DPAD_CENTER
shot settings-08-matches-and-markets 3
key BACK DPAD_DOWN DPAD_RIGHT DPAD_CENTER
shot settings-09-control 3
key BACK DPAD_DOWN DPAD_DOWN DPAD_LEFT DPAD_CENTER
shot settings-10-search 2
key BACK

adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
ls "$OUT"
exit 0
