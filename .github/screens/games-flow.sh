#!/usr/bin/env bash
# Runs on the CI emulator (.github/workflows/games-screens.yml): the BEFORE of the owner's «phone or no phone» review (docs/research/GAMES-FLOW.md): the flow as it is today, shot with screencap, the setting «اللعب بالريموت بس» turned on in each
# game's own setup page with the remote's keys (nothing is written behind the app's back).   games.sh SAVER_APK HOME_APK OUT_DIR
# Best effort: every shot is taken whatever is on the screen; the log says what was pressed. (re-shot after the liar rounds' bar fix)
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
leave() { key BACK DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2; }   # the pause card: «الرئيسية»

list
shot flow-01-list-header 2     # the header says «بدون جوالات: ريموت واحد يدور بينكم»; the tiles say «جوال» or «ريموت»

# ---- an old game both ways: «واحد مو معنا» (the switch «اللعب بالجوالات» is in its setup, off every time)
tile 3
shot flow-02-oddone-setup 1
key DPAD_CENTER; sleep 3
shot flow-03-oddone-remote-start 2        # no phones: what the first press shows
key DPAD_CENTER; sleep 3.5
shot flow-04-oddone-remote-handover-count 0.5
shot flow-05-oddone-remote-secret 3
key BACK BACK; sleep 1
key DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2
list
tile 3
key DPAD_UP DPAD_UP DPAD_LEFT DPAD_CENTER
shot flow-06-oddone-phones-on-setup 1
key DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 3
shot flow-07-oddone-phones-lobby 2       # the room: QR, code, address; the start waits for 3 phones
key BACK; sleep 1
leave

# ---- a party game: «واحد يكذب» (phones only, or «اللعب بالريموت بس»)
tile 11
shot flow-08-liar-setup 1                # two opposite switches in the family: «اللعب بالجوالات» here and «اللعب بالريموت بس» there
key DPAD_CENTER; sleep 3
shot flow-09-liar-lobby 2
key BACK; sleep 1
leave

# ---- «الأسرع»: phones only
tile 0
shot flow-10-quiz-setup 1
key DPAD_CENTER; sleep 3
shot flow-11-quiz-room 2

adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
ls "$OUT"
exit 0
