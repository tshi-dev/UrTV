#!/usr/bin/env bash
# Runs on the CI emulator (.github/workflows/games-flow-screens.yml): the AFTER of the owner's «phone or no phone» review (docs/research/GAMES-FLOW.md): the built flow, shot with screencap and the remote's keys: the list, the first row «كيف تلعبون؟», the room, the way out to the remote.
# game's own setup page with the remote's keys (nothing is written behind the app's back).   games.sh SAVER_APK HOME_APK OUT_DIR
# Best effort: every shot is taken whatever is on the screen; the log says what was pressed. (re-shot after the liar rounds' bar fix) (after the UI review: address, count line, titles)
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
shot after-00-list-tags 2     # no header sentence, no phone button: the tiles say «ريموت», «جوالات» or «ريموت أو جوالات» in one outlined style

# ---- an old game both ways: «واحد مو معنا»: the first row of its setup is «كيف تلعبون؟»
tile 3
shot after-01-oddone-setup-remote 1       # ريموت واحد (the default): every row, the start button has the focus
key DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_UP DPAD_LEFT DPAD_CENTER   # up to «كيف تلعبون؟», one chip toward the end: «جوالات», OK
shot after-02-oddone-setup-phones 1       # the number of players is gone (the phones decide), no «الأسماء» beside «ابدأ»
key DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 3
shot after-03-oddone-room-short 2         # the room: QR and the address (Latin), «ناقص ٣», «ابدأ» grey but with the focus, «نلعب بالريموت» beside it
key DPAD_CENTER; sleep 1
shot after-04-oddone-room-ok-while-short 1  # OK on the grey «ابدأ»: nothing jumps, the line stays
key DPAD_LEFT
shot after-05-oddone-room-remote-focus 1  # the focus on «نلعب بالريموت»
key DPAD_CENTER; sleep 4
shot after-06-oddone-remote-started 2     # the same game on the one remote
key BACK BACK; sleep 1
key DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2
list
tile 3
shot after-07-oddone-setup-remembered 1   # the choice is remembered: «ريموت واحد»
leave

# ---- a party game: «واحد يكذب» (phones only): «ابدأ» alone in the room, no way out button
tile 11
shot after-08-liar-setup 1
key DPAD_CENTER; sleep 3
shot after-09-liar-room 2
key BACK; sleep 1
leave

# ---- «الأسرع»: phones only
tile 0
shot after-10-quiz-setup 1
key DPAD_CENTER; sleep 3
shot after-11-quiz-room 2

# ---- the word game: every letter in the middle of its tile (the owner's rule), measured on the real pixels
dump() { adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" > /dev/null 2>&1; }
leave
tile 6
key DPAD_CENTER; sleep 3                                  # «ابدأ»: the board and the keyboard
key DPAD_CENTER DPAD_LEFT DPAD_CENTER DPAD_LEFT DPAD_CENTER   # ا ب ت
shot after-12-word5-typing 1; dump after-12-word5-typing
key DPAD_LEFT DPAD_CENTER DPAD_LEFT DPAD_CENTER; sleep 1   # ث ج: a full row
key DPAD_CENTER DPAD_CENTER; sleep 2                      # «إدخال» (twice: a word that is not in the list is taken the second time)
shot after-13-word5-coloured 1; dump after-13-word5-coloured
pip install --quiet pillow > /dev/null 2>&1
for n in after-12-word5-typing after-13-word5-coloured; do
  python3 tests/glyph_centre_shots.py "$OUT/$n.png" "$OUT/$n.xml" | tee -a "$OUT/glyph-centre.txt"
done

adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
ls "$OUT"
exit 0
