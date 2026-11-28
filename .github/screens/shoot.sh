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

# ---- 4.4 beta 4 (the owner, docs/research/SETTINGS-FINDABILITY.md): ONE tree of nine sections in every settings style. «أزرار» (the default): the
# first screen of buttons (the search wide on top), then «الصلاة والأذان», then «صوري والخلفيات» with its add and delete first
start sa.urtv.screensaver/.SettingsActivity
shot settings-a-buttons-hub 3
key DPAD_DOWN DPAD_CENTER
shot settings-a-buttons-prayer 2
key BACK DPAD_LEFT DPAD_CENTER
shot settings-a-buttons-photos 2
key BACK DPAD_DOWN DPAD_RIGHT DPAD_CENTER
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16; do key DPAD_DOWN; done
shot settings-a-buttons-look-cards 1
key BACK BACK
# «كنبة»: the side list on the right, the rows on the left; it opens on «الصلاة والأذان», the focus on its item; ▼ shows the next section's rows at once
saver --es settingsstyle couch
start sa.urtv.screensaver/.SettingsActivity
shot settings-b-couch-prayer 3
key DPAD_DOWN
shot settings-b-couch-photos 2
key DPAD_DOWN
shot settings-b-couch-look 2
key DPAD_CENTER
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16; do key DPAD_DOWN; done
shot settings-b-couch-look-cards 1
key BACK DPAD_DOWN
shot settings-b-couch-saver 2
key DPAD_DOWN
shot settings-b-couch-apps 2
key DPAD_DOWN
shot settings-b-couch-matches 2
key DPAD_DOWN
shot settings-b-couch-family 2
key DPAD_DOWN
shot settings-b-couch-lang 2
key DPAD_DOWN
shot settings-b-couch-about 2
for i in 1 2 3 4 5 6 7 8 9 10; do key DPAD_UP; done
key DPAD_CENTER
shot settings-b-couch-search 2
key BACK BACK
# tvOS: the same buttons and sections in its own look
saver --es settingsstyle tvos
start sa.urtv.screensaver/.SettingsActivity
shot settings-c-tvos-hub 3
key DPAD_DOWN DPAD_CENTER
shot settings-c-tvos-prayer 2
key BACK BACK
saver --es settingsstyle buttons

# ---- 4.4 beta 4 (the tester, build 654: the settings not found from the home in «كنبة»): «كنبة» with «بدون» (the date over the big clock), the
# apps row's «الإعدادات» at its end (the owner's «تكبير»), «مركز التحكم» with «الإعدادات» as its first row, and OK on it
saver --es homelayout couch --es couch on --es clock glass --es appsstyle zoom --es home_panel none
# the focus walked to a name the screen reads (uiautomator): the emulator has no city, so its header also has «اختر المدينة», which counted keys hit
focused() { adb shell "uiautomator dump /sdcard/u.xml > /dev/null 2>&1; cat /sdcard/u.xml" 2> /dev/null | grep -q "content-desc=\"$1\"[^>]*focused=\"true\""; }
focus_to() { local i; for i in $(seq 0 "$2"); do focused "$1" && return 0; [ "$i" -lt "$2" ] && key "$3"; done; return 1; }   # $1 name, $2 at most, $3 the key
start sa.urtv.home/.MainActivity
shot couch-none-date 12
key DPAD_DOWN DPAD_DOWN
focus_to "الإعدادات" 10 DPAD_LEFT
shot couch-row-gear 1.5
key DPAD_UP DPAD_UP DPAD_UP
focus_to "مركز التحكم" 8 DPAD_LEFT || focus_to "مركز التحكم" 12 DPAD_RIGHT
shot couch-header 1
key DPAD_CENTER
shot couch-panel 2
focus_to "الإعدادات" 4 DPAD_DOWN
shot couch-panel-settings 1
key DPAD_CENTER
shot couch-panel-settings-open 3
key BACK
saver --es homelayout classic --es home_panel matches,prayerrow,hourly

# 4.4 beta 4: «كنبة»'s side cards, A (two) and B (three), on the home in the «كنبة» layout; then off again (the default)
saver --es homelayout couch --es clock glass --es home_panel stocks   # a strip that leaves the column's cards free (it never repeats the strip)
for opt in a b; do
  saver --es sidecards $opt
  start sa.urtv.home/.MainActivity; sleep 8   # our home (the emulator's own launcher answers HOME)
  start sa.urtv.home/.MainActivity; sleep 6   # again: the big clock is up from the first start, so the strip sits under its digits
  shot home-side-cards-$opt 2
done
adb logcat -d -s UrTVSide:I > "$OUT/side-cards.txt"   # which cards each column loaded (run 22-23: B's shot looked like A's)
saver --es sidecards off --es home_panel matches,prayerrow,hourly
# 4.4 beta 4 (the findability review §5): the presses to each target in every settings style, counted by moving the focus as a person would
python3 .github/screens/presses.py "$OUT" || echo "presses: the walk stopped"
# ---- 4.4 beta 4 known bugs (the supervisor's a, h): (a) a bar of a few apps is as wide as its tiles; (h) «رملي»: the reciter letters in the look's
# own ink, and a reminder card under the clock's column, not over it
saver --es homelayout classic --es appsstyle zoom --es home_panel none
start sa.urtv.home/.MainActivity
shot bug-a-dock-few-apps 10
saver --es look sand
due=$(adb shell "TZ=Asia/Riyadh date '+%Y-%m-%d %H:%M'" | tr -d '\r')   # the city of these shots is Riyadh: a reminder due now
adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.TASKS --es op add --es text 'موعد الطبيب' --es at '$due' --es where home" > /dev/null
start sa.urtv.home/.MainActivity
shot bug-h-sand-reminder 10
start sa.urtv.home/.tilawah.MainActivity
key DPAD_RIGHT   # from the list to the letters (the start edge is the right in Arabic)
shot bug-h-sand-letters 3
key BACK
saver --es look glass --es home_panel matches,prayerrow,hourly

adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
ls "$OUT"
exit 0
