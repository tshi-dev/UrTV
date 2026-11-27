#!/usr/bin/env bash
# Runs on the CI emulator (.github/workflows/games-screens.yml): the one-remote modes of the family games, shot with screencap, the setting «اللعب بالريموت بس» turned on in each
# game's own setup page with the remote's keys (nothing is written behind the app's back).   games.sh SAVER_APK HOME_APK OUT_DIR
# Best effort: every shot is taken whatever is on the screen; the log says what was pressed.
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

# the party games are held back by the release file (the live one opens them only for the owner's pass); this emulator is given its own kept copy that opens them for everyone, so
# the screens can be taken: the debug build's private files through run-as (nothing of the app is changed; a fresh file is not fetched again for three hours)
cat > "$OUT/gr.json" <<'JSON'
{"v":1,"games":{"forehead":{"a":"now","b":"now","native":true},"author":{"a":"now","b":"now","native":true},"closest":{"a":"now","b":"now","native":true},"liar":{"a":"now","b":"now","native":true},"emoji":{"a":"now","b":"now","native":true},"link":{"a":"now","b":"now","native":true},"night":{"a":"now","b":"now","native":true},"draw":{"a":"now","b":"now","native":true},"quran-verse":{"a":"now","b":"now","native":true},"maths":{"a":"now","b":"now","native":true},"logic":{"a":"now","b":"now","native":true}},"codes":[]}
JSON
adb push "$OUT/gr.json" /data/local/tmp/gr.json > /dev/null
adb shell run-as sa.urtv.screensaver mkdir -p files/content
adb shell run-as sa.urtv.screensaver cp /data/local/tmp/gr.json files/content/games-release.json
adb shell run-as sa.urtv.screensaver ls -la files/content
rm -f "$OUT/gr.json"

key() { for k in "$@"; do adb shell input keyevent "$k"; sleep 0.7; done; }
shot() { sleep "${2:-1.5}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1 $(stat -c %s "$OUT/$1.png" 2>/dev/null)"; }

# the games list: 4 tiles a line, the first at the START edge (the right, in Arabic); the keys: LEFT is toward the end (the next tile). The order is the registry's:
# 0 quiz 1 khanat 2 missing-word 3 odd-one 4 charades 5 letter-hive 6 word5 7 domino 8 forehead 9 author 10 closest 11 liar 12 emoji ...
# the focus starts on the first game with no phones (khanat, 1); after a game it is on that game
CUR=1
list() { adb shell am start -W --activity-clear-task -n sa.urtv.screensaver/.YourTvHubActivity --es open games > /dev/null; sleep 6; CUR=1; }
tile() {   # go to tile $1 from $CUR, then open it
  local to=$1 dr dc
  dr=$(( to / 4 - CUR / 4 )); dc=$(( to % 4 - CUR % 4 ))
  while [ $dr -gt 0 ]; do key DPAD_DOWN; dr=$((dr-1)); done
  while [ $dr -lt 0 ]; do key DPAD_UP; dr=$((dr+1)); done
  while [ $dc -gt 0 ]; do key DPAD_LEFT; dc=$((dc-1)); done
  while [ $dc -lt 0 ]; do key DPAD_RIGHT; dc=$((dc+1)); done
  CUR=$to
  key DPAD_CENTER; sleep 2
}
remote_on() {   # the setup page: the focus starts on the start button; up twice is «اللعب بالريموت بس» (just above «الصوت»); left is the next chip («نعم»); OK picks it
  shot "$1-setup-before" 1
  key DPAD_UP DPAD_UP DPAD_LEFT DPAD_CENTER
  shot "$1-setup-remote-on" 1
  key DPAD_DOWN DPAD_DOWN
}
leave() { key BACK DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2; }   # the pause card: «الرئيسية»

list
shot list-00 2

# ---- «نادوا الجواب» with «اقرأ الإيموجي»
tile 12
remote_on callaloud
key DPAD_CENTER; sleep 3
shot callaloud-setup-prefilled 1
key DPAD_CENTER; sleep 3
shot callaloud-a-question 2
key DPAD_CENTER
shot callaloud-b-reveal 1
key DPAD_CENTER
shot callaloud-c-who-first 1
key DPAD_LEFT
shot callaloud-c2-who-first-focus 1
key DPAD_CENTER; sleep 1
shot callaloud-d-next-with-score 1.5
leave

# ---- «على جبيني» as a mode of «بدون كلام»
tile 8
remote_on forehead
key DPAD_CENTER; sleep 3
shot forehead-a-cover 2
key DPAD_CENTER
shot forehead-b-room 2
key DPAD_CENTER; sleep 1.5
shot forehead-c-next-word 1
leave

# ---- «واحد يكذب» by hands
tile 11
remote_on liar
key DPAD_CENTER; sleep 3
shot liar-a-pass 2
key DPAD_CENTER; sleep 3.5
shot liar-b-secret 1
key DPAD_CENTER; sleep 1
key DPAD_CENTER; sleep 3.5
key DPAD_CENTER; sleep 1
key DPAD_CENTER; sleep 3.5
key DPAD_CENTER; sleep 1.5
shot liar-d-talk 1.5
key DPAD_CENTER
shot liar-e-hands-vote 1.5
key DPAD_LEFT
shot liar-e2-hands-vote-focus 1
key DPAD_CENTER
shot liar-f-result 2
leave

# ---- «أقرب رقم» on paper
tile 10
remote_on closest
key DPAD_CENTER; sleep 3
shot closest-a-paper 2
key DPAD_CENTER
shot closest-b-reveal 1.5
key DPAD_CENTER
shot closest-c-first-place 1.5
key DPAD_CENTER
shot closest-c2-second-place 1.5
key DPAD_LEFT DPAD_CENTER; sleep 1
shot closest-d-next-with-score 2
leave

# ---- two old games with the new score cells
tile 2
key DPAD_CENTER; sleep 4
shot old-missing-word-score-cells 2
leave
tile 1
key DPAD_CENTER; sleep 9
shot old-khanat-score-cells 2
leave

list
shot list-01-tags 2
key DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT
shot list-02-tags-first-row 1

adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
ls "$OUT"
exit 0
