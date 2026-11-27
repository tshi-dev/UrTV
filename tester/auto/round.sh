#!/usr/bin/env bash
# The automatic tester round (4.4 beta 4): one command on the owner's PC, with the TV on the network and adb working (optimize/find-tv.sh did it).
#   tester/auto/round.sh [--tag apk-next] [--build NAME] [--no-install] [--only a,b,c] [--prev DIR]
# It installs the build of the tag (apk-next by default) OVER the installed one, so the data stays; saves the settings and the sound; then runs, with nobody
# touching the remote, every check below and writes tester/auto/out/<build>/report.md with the screenshots (the city boxed) and a column of the previous
# build's numbers. At the end it puts the settings, the sound, the display and the font back. Steps (--only takes these words, in this order):
#   install   layouts   settings   saver   radio   display   font   adhan   games   health
# Never touches the family PIN, the TV's setup wizard or the old launcher, never changes the Wi-Fi, never restarts the TV. NEVER PRESSES BLIND: a key is sent only after
# the screen it is for was read (the front app, or the screen's own text through uiautomator), and a screen that is not the one expected is a FAIL, not a retry with more keys.
# What it reads is in tester/auto/lib.py (tested by tests/tester_auto_test.py); what it opens is in tester/auto/screens.tsv; its limits are in thresholds.tsv.
# Stand-ins for a test: ROUND_TV (default «optimize/venv/bin/python -I optimize/tv.py»), ROUND_SAVER, ROUND_MUSIC, ROUND_INSTALL, ROUND_STARTSAVER, ROUND_FAST=1 (no waiting).
set -uo pipefail
cd "$(dirname "$0")/../.."
PY="${PY:-optimize/venv/bin/python}"
TV="${ROUND_TV:-$PY -I optimize/tv.py}"
SAVER="${ROUND_SAVER:-optimize/saver.sh}"
MUSIC="${ROUND_MUSIC:-optimize/music.sh}"
INSTALL="${ROUND_INSTALL:-optimize/install.sh}"
STARTSAVER="${ROUND_STARTSAVER:-optimize/start-saver.sh}"
LIB="python3 tester/auto/lib.py"
HOME_PKG=sa.urtv.home
SAVER_PKG=sa.urtv.screensaver
PKGS="$HOME_PKG,$SAVER_PKG,sa.urtv.extras,sa.urtv.rescue"
FROZEN_REGEX="${FROZEN_REGEX:-StallWatch.*(stall|frozen)|Input dispatching timed out|ANR in sa\\.urtv}"
WALK_S="${WALK_S:-20}"
[ "${ROUND_FAST:-0}" = 1 ] && WALK_S=0
STATION_S="${STATION_S:-30}"

TAG=apk-next BUILD="" DOINSTALL=1 ONLY="" PREV=""
while [ $# -gt 0 ]; do
  case "$1" in
    --tag) TAG="${2:?--tag NAME}"; shift 2 ;;
    --build) BUILD="${2:?--build NAME}"; shift 2 ;;
    --no-install) DOINSTALL=0; shift ;;
    --only) ONLY="${2:?--only a,b,c}"; shift 2 ;;
    --prev) PREV="${2:?--prev DIR}"; shift 2 ;;
    *) sed -n '2,12p' "$0"; exit 2 ;;
  esac
done

tv() { $TV "$@"; }                        # the TV's shell (or a tv.py option such as --shot FILE)
nap() { [ "${ROUND_FAST:-0}" = 1 ] || sleep "$1"; }
want() { [ -z "$ONLY" ] || case ",$ONLY," in *",$1,"*) true ;; *) false ;; esac; }

# ---- where it writes
stamp=$(date +%Y%m%d-%H%M)
BUILD="${BUILD:-$TAG-$stamp}"
OUT="${ROUND_OUT:-tester/auto/out}/$BUILD"
mkdir -p "$OUT/shots" "$OUT/raw"
: > "$OUT/steps.tsv"
: > "$OUT/metrics.tsv"
echo "$BUILD" > "$OUT/build.txt"
if [ -z "$PREV" ]; then   # the newest other folder of out/
  PREV=$(ls -1dt "$(dirname "$OUT")"/*/ 2> /dev/null | grep -v "/$BUILD/\$" | head -n 1 | sed 's#/$##') || PREV=""
fi

step() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$OUT/steps.tsv"; echo "  $2  $1  $3"; }
metric() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$OUT/metrics.tsv"; echo "  metric $1 = $2$3"; }
rows() { grep -v '^#' tester/auto/screens.tsv | awk -F'\t' -v k="$1" '$1 == k'; }   # the rows of one kind

# ---- reading the screen (never a key before it)
front() { tv "dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity'" 2> /dev/null | tr -d '\r'; }
on_top() {   # $1 package: is it the front app (asked up to 6 times, 2 s apart)
  local i
  for i in 1 2 3 4 5 6; do
    if front | grep -q "$1"; then return 0; fi
    nap 2
  done
  return 1
}
dump() {   # the screen's text tree into $1
  tv "uiautomator dump /sdcard/urtv-round.xml > /dev/null 2>&1; cat /sdcard/urtv-round.xml 2> /dev/null; rm -f /sdcard/urtv-round.xml" 2> /dev/null | tr -d '\r' > "$1"
}
see() {   # $1 text: is it on the screen now (asked up to 8 times, 2 s apart; the dump is kept in raw/last.xml)
  local i
  for i in 1 2 3 4 5 6 7 8; do
    dump "$OUT/raw/last.xml"
    if [ "$($LIB has-text "$OUT/raw/last.xml" "$1")" = yes ]; then return 0; fi
    nap 2
  done
  return 1
}
key() { tv "input keyevent $1" > /dev/null 2>&1; }
shot() {   # $1 name: a screenshot, and, when the city is known and on this screen, a copy with it boxed
  local f="$OUT/shots/$1.png"
  tv --shot "$f" > /dev/null 2>&1 || { echo "  (no screenshot: $1)"; return 1; }
  if [ -n "${CITY:-}" ] && [ -s "$f" ]; then
    dump "$OUT/raw/shot.xml"
    case "$($LIB box "$OUT/raw/shot.xml" "$CITY" "$f" "$OUT/shots/$1-city.png")" in
      boxed) ;; missing) echo "  (the city is not on the screen of $1)" ;; *) ;;
    esac
  fi
  return 0
}
focus_walk() {   # $1 text, $2 the key, $3 at most how many presses: press only while the focused row is not $1 and say where it is; true when it is
  local i
  for i in $(seq 0 "${3:-30}"); do
    dump "$OUT/raw/focus.xml"
    if [ "$($LIB focused "$OUT/raw/focus.xml")" = "$1" ]; then return 0; fi
    [ "$i" -lt "${3:-30}" ] && key "$2" && nap 1
  done
  return 1
}

# ---- what is put back at the end
SAVED_FONT="" SAVED_VOL="" SAVED_MUTED="" DISPLAY_CHANGED=0 FONT_CHANGED=0 SETTINGS_SAVED=0
restore() {
  trap - EXIT
  echo "putting everything back…"
  if [ "$DISPLAY_CHANGED" = 1 ]; then tv "wm size reset; wm density reset" > /dev/null 2>&1; fi
  if [ "$FONT_CHANGED" = 1 ]; then
    if [ -z "$SAVED_FONT" ] || [ "$SAVED_FONT" = null ]; then tv "settings delete system font_scale" > /dev/null 2>&1; else tv "settings put system font_scale $SAVED_FONT" > /dev/null 2>&1; fi
  fi
  [ "$SETTINGS_SAVED" = 1 ] && $SAVER import "$OUT/settings-before.json" --yes > "$OUT/raw/restore-settings.txt" 2>&1
  if [ -n "$SAVED_VOL" ]; then tv "cmd media_session volume --stream 3 --set $SAVED_VOL" > /dev/null 2>&1; fi
  tv "input keyevent KEYCODE_HOME" > /dev/null 2>&1
  local now
  now=$(tv "dumpsys audio" 2> /dev/null | tr -d '\r'); printf '%s\n' "$now" > "$OUT/raw/audio-after.txt"
  if [ -n "$SAVED_MUTED" ] && [ "$($LIB muted "$OUT/raw/audio-after.txt")" != "$SAVED_MUTED" ]; then
    step "restore-mute" FAIL "the music stream's mute is not as it was ($SAVED_MUTED before): put it back by hand; the round never presses the mute key"
  fi
  $LIB report "$OUT" "$PREV" > /dev/null   # again: the steps of the putting back are in it
}
trap restore EXIT

# ---- start: the clock of the TV (for the logs), the settings, the sound
START_AT=$(tv "date '+%Y-%m-%d %H:%M:%S'" 2> /dev/null | tr -d '\r' | tail -n 1)
tv "logcat -c" > /dev/null 2>&1
if ! $SAVER export > "$OUT/settings-before.json" 2> "$OUT/raw/export.err" || [ ! -s "$OUT/settings-before.json" ]; then
  step "start" FAIL "the settings could not be saved (saver.sh export): nothing is changed, the round stops here"
  $LIB report "$OUT" "$PREV" > /dev/null
  exit 1
fi
SETTINGS_SAVED=1
$SAVER show > "$OUT/raw/show-before.txt" 2>&1
CITY=$($LIB city "$OUT/raw/show-before.txt")
tv "cmd media_session volume --stream 3 --get" 2> /dev/null | tr -d '\r' > "$OUT/raw/volume-before.txt"
SAVED_VOL=$($LIB volume "$OUT/raw/volume-before.txt")
tv "dumpsys audio" 2> /dev/null | tr -d '\r' > "$OUT/raw/audio-before.txt"
SAVED_MUTED=$($LIB muted "$OUT/raw/audio-before.txt")
SAVED_FONT=$(tv "settings get system font_scale" 2> /dev/null | tr -d '\r' | tail -n 1)

# ---- install (over the installed build: the data stays; a !! line or a refusal stops the round before anything else changed)
if want install && [ "$DOINSTALL" = 1 ]; then
  echo "install $TAG…"
  if APK_TAG="$TAG" $INSTALL saver > "$OUT/raw/install-saver.txt" 2>&1 && APK_TAG="$TAG" $INSTALL launcher > "$OUT/raw/install-launcher.txt" 2>&1; then
    step "install" PASS "$TAG over the installed build (data kept)"
  else
    step "install" FAIL "$(grep -h -m1 '^!!' "$OUT"/raw/install-*.txt 2> /dev/null || echo 'the install stopped: see raw/install-*.txt')"
    $LIB report "$OUT" "$PREV" > /dev/null
    exit 1
  fi
  nap 10
fi

# ---- layouts × cards
if want layouts; then
  echo "layouts…"
  while IFS=$'\t' read -r _ lkey lname _ _ <&3; do
    while IFS=$'\t' read -r _ ckey cname _ _ <&4; do
      name="layout-$lkey-$ckey"
      if ! $SAVER homelayout "$lkey" > /dev/null 2>&1 || ! $SAVER home-card "$ckey" > /dev/null 2>&1; then step "$name" FAIL "saver.sh refused homelayout $lkey / home-card $ckey"; continue; fi
      key KEYCODE_HOME; nap 8
      if on_top "$HOME_PKG"; then
        shot "$name"
        step "$name" PASS "$lname، $cname: the home is in front (look at shots/$name.png)"
      else
        step "$name" FAIL "$lname، $cname: the home did not come to the front: $(front)"
      fi
    done 4< <(rows card)
  done 3< <(rows layout)
  $SAVER homelayout classic > /dev/null 2>&1; $SAVER home-card none > /dev/null 2>&1
fi

# ---- the settings, each style opened to each section
if want settings; then
  echo "settings…"
  while IFS=$'\t' read -r _ stkey stname stcmd _ <&3; do
    # shellcheck disable=SC2086
    $SAVER $stcmd > /dev/null 2>&1 || { step "settings-$stkey" FAIL "saver.sh $stcmd refused"; continue; }
    while IFS=$'\t' read -r _ seckey secname extras marker <&4; do
      name="settings-$stkey-$seckey"
      # shellcheck disable=SC2086
      tv "am start --activity-clear-task -n $SAVER_PKG/.SettingsActivity $extras" > /dev/null 2>&1
      if on_top "$SAVER_PKG" && see "$marker"; then
        shot "$name"; step "$name" PASS "$stname ← $secname: «$marker» is on the screen"
      else
        shot "$name"; step "$name" FAIL "$stname ← $secname: «$marker» was not on the screen (front: $(front))"
      fi
    done 4< <(rows section)
  done 3< <(rows style)
  $SAVER preset tv > /dev/null 2>&1
  key KEYCODE_HOME
fi

# ---- the saver starts (dumpsys dreams, asked every 2 s until it runs)
if want saver; then
  echo "saver…"
  if $STARTSAVER > "$OUT/raw/start-saver.txt" 2>&1; then
    tv "dumpsys dreams; dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'" 2> /dev/null | tr -d '\r' > "$OUT/raw/dreams.txt"
    if [ "$($LIB dreaming "$OUT/raw/dreams.txt")" = yes ]; then shot saver; step "saver-start" PASS "a dream runs"; else step "saver-start" FAIL "start-saver.sh said it started, dumpsys dreams says no dream runs"; fi
  else
    step "saver-start" SKIP "$(tail -n 1 "$OUT/raw/start-saver.txt")"
  fi
  key KEYCODE_HOME; nap 3
fi

# ---- every radio station, started the way a person starts it (the تلاوة screen, the stations tab, the station's row, OK), 30 s each with the logs
if want radio; then
  echo "radio…"
  $MUSIC stations 2> /dev/null | tr -d '\r' > "$OUT/raw/stations.txt"
  while IFS= read -r line <&3; do
    idx=$(sed -n 's/^[[:space:]]*\([0-9][0-9]*\)[^0-9].*$/\1/p' <<< "$line"); sname=$(sed -n 's/^[[:space:]]*[0-9][0-9]*[^[:alnum:]]*\(.*\)$/\1/p' <<< "$line")
    [ -n "$idx" ] || continue
    name="radio-$idx"
    tv "am start -n $HOME_PKG/.tilawah.MainActivity" > /dev/null 2>&1
    if ! on_top "$HOME_PKG"; then step "$name" FAIL "the تلاوة screen did not open: $(front)"; continue; fi
    key KEYCODE_DPAD_RIGHT; nap 1   # one tab to the right: the stations (looked at below: the row must be found by its name, or nothing is pressed)
    if ! focus_walk "$sname" KEYCODE_DPAD_DOWN 30; then step "$name" FAIL "the row «$sname» was not found on the stations tab (no key pressed on it)"; key KEYCODE_BACK; continue; fi
    tv "dumpsys audio" 2> /dev/null | tr -d '\r' > "$OUT/raw/audio-$name-before.txt"
    key KEYCODE_DPAD_CENTER
    nap "$STATION_S"
    tv "logcat -d -s MusicService:V UrTVMute:V" 2> /dev/null | tr -d '\r' > "$OUT/raw/$name.log"
    $MUSIC status 2> /dev/null | tr -d '\r' > "$OUT/raw/$name-status.txt"
    tv "dumpsys audio" 2> /dev/null | tr -d '\r' > "$OUT/raw/audio-$name-after.txt"
    shot "$name"
    was=$($LIB muted "$OUT/raw/audio-$name-before.txt"); is=$($LIB muted "$OUT/raw/audio-$name-after.txt")
    if grep -q 'source=radio' "$OUT/raw/$name-status.txt" && [ "$was$is" != nono ] && [ "$was" = "$is" ]; then step "$name" PASS "$sname: playing, the mute unchanged ($is)"
    elif ! grep -q 'source=radio' "$OUT/raw/$name-status.txt"; then step "$name" FAIL "$sname: not playing after ${STATION_S} s (see raw/$name.log)"
    elif [ "$was" != "$is" ]; then step "$name" FAIL "$sname: the mute changed from $was to $is (raw/$name.log says who)"
    else step "$name" PASS "$sname: playing"; fi
    $MUSIC stop > /dev/null 2>&1
    key KEYCODE_BACK; nap 1
  done 3< "$OUT/raw/stations.txt"
  key KEYCODE_HOME
fi

# ---- 720p at density 213 (put back by restore, whatever happens)
if want display; then
  echo "display 720p…"
  tv "wm size 1280x720; wm density 213" > /dev/null 2>&1; DISPLAY_CHANGED=1; nap 5
  key KEYCODE_HOME; nap 6
  if on_top "$HOME_PKG"; then shot display-720p-home; step "display-720p-home" PASS "the home at 1280x720, density 213"; else step "display-720p-home" FAIL "the home was not in front at 720p: $(front)"; fi
  # shellcheck disable=SC2086
  tv "am start --activity-clear-task -n $SAVER_PKG/.SettingsActivity $(rows section | awk -F'\t' '$2 == "pray" {print $4}')" > /dev/null 2>&1
  if on_top "$SAVER_PKG"; then shot display-720p-settings; step "display-720p-settings" PASS "the settings at 720p"; else step "display-720p-settings" FAIL "the settings were not in front at 720p"; fi
  tv "wm size reset; wm density reset" > /dev/null 2>&1; DISPLAY_CHANGED=0; nap 4
  key KEYCODE_HOME
fi

# ---- the largest font
if want font; then
  echo "font…"
  tv "settings put system font_scale 1.3" > /dev/null 2>&1; FONT_CHANGED=1; nap 4
  key KEYCODE_HOME; nap 6
  if on_top "$HOME_PKG"; then shot font-large-home; step "font-large-home" PASS "the home at font scale 1.3"; else step "font-large-home" FAIL "the home was not in front at the large font"; fi
  tv "am start --activity-clear-task -n $SAVER_PKG/.SettingsActivity" > /dev/null 2>&1
  if on_top "$SAVER_PKG"; then shot font-large-settings; step "font-large-settings" PASS "the settings at font scale 1.3"; else step "font-large-settings" FAIL "the settings were not in front at the large font"; fi
  if [ -z "$SAVED_FONT" ] || [ "$SAVED_FONT" = null ]; then tv "settings delete system font_scale" > /dev/null 2>&1; else tv "settings put system font_scale $SAVED_FONT" > /dev/null 2>&1; fi
  FONT_CHANGED=0; nap 3
  key KEYCODE_HOME
fi

# ---- the adhan per prayer: each state set by command and read back from the status
if want adhan; then
  echo "adhan…"
  before=$($MUSIC adhan 2> /dev/null | tr -d '\r')
  while IFS=$'\t' read -r _ pkey pname _ _ <&3; do
    for st in both sound screen none; do
      name="adhan-$pkey-$st"
      $MUSIC adhan state "$pkey" "$st" > "$OUT/raw/$name.txt" 2>&1
      $MUSIC adhan 2> /dev/null | tr -d '\r' > "$OUT/raw/adhan-status.txt"
      got=$($LIB adhan-state "$OUT/raw/adhan-status.txt" "$pkey")
      if [ "$got" = "$st" ]; then step "$name" PASS "$pname: $st"; else step "$name" FAIL "$pname: set $st, the status says «${got:-nothing}»"; fi
    done
    $MUSIC adhan state "$pkey" both > /dev/null 2>&1   # each prayer ends as the default (the saved settings go back at the end too)
  done 3< <(rows adhan)
  : "$before"
fi

# ---- the games list
if want games; then
  echo "games…"
  tv "am start --activity-clear-task -n $SAVER_PKG/.QuizGamesActivity" > /dev/null 2>&1
  if on_top "$SAVER_PKG" && see "الأسرع"; then shot games-list; step "games-list" PASS "the games list is on the screen (the badges: look at shots/games-list.png)"
  else shot games-list; step "games-list" FAIL "the games list was not on the screen: $(front)"; fi
  key KEYCODE_HOME
fi

# ---- health: cold starts, memory, jank over a scripted walk, crashes, ANRs, frozen lines
if want health; then
  echo "health…"
  for who in home saver; do
    if [ "$who" = home ]; then pkg=$HOME_PKG; act=$HOME_PKG/.MainActivity; else pkg=$SAVER_PKG; act=$SAVER_PKG/.SettingsActivity; fi
    tv "am force-stop $pkg" > /dev/null 2>&1; nap 3
    tv "am start -W -n $act" 2> /dev/null | tr -d '\r' > "$OUT/raw/start-$who.txt"
    t=$($LIB start-time "$OUT/raw/start-$who.txt")
    if [ -n "$t" ]; then metric "cold_start_${who}_ms" "$t" ms; else step "cold-start-$who" FAIL "am start -W said no TotalTime (raw/start-$who.txt)"; fi
  done
  key KEYCODE_HOME; nap 6
  if on_top "$HOME_PKG"; then
    tv "dumpsys gfxinfo $HOME_PKG reset" > /dev/null 2>&1
    walk_end=$((SECONDS + WALK_S)); n=0
    while [ "$SECONDS" -lt "$walk_end" ] || [ "$n" -lt 6 ]; do   # a scripted D-pad walk: right, right, left, left, down, up; 250 ms apart
      for k in RIGHT RIGHT LEFT LEFT DOWN UP; do key "KEYCODE_DPAD_$k"; n=$((n + 1)); [ "${ROUND_FAST:-0}" = 1 ] || sleep 0.25; done
    done
    tv "dumpsys gfxinfo $HOME_PKG framestats" 2> /dev/null | tr -d '\r' > "$OUT/raw/gfxinfo.txt"
    IFS=$'\t' read -r jf jp j90 j99 < <($LIB jank "$OUT/raw/gfxinfo.txt")
    [ -n "$jf" ] && metric jank_home_frames "$jf" ""
    [ -n "$jp" ] && metric jank_home_pct "$jp" %
    [ -n "$j90" ] && metric jank_home_p90_ms "$j90" ms
    [ -n "$j99" ] && metric jank_home_p99_ms "$j99" ms
    [ -z "$jf" ] && step "jank" FAIL "gfxinfo said nothing the round can read (raw/gfxinfo.txt)"
  else
    step "jank" FAIL "the home was not in front for the walk: no key was sent ($(front))"
  fi
  for who in home saver; do
    if [ "$who" = home ]; then pkg=$HOME_PKG; else pkg=$SAVER_PKG; fi
    tv "dumpsys meminfo $pkg" 2> /dev/null | tr -d '\r' > "$OUT/raw/meminfo-$who.txt"
    p=$($LIB pss "$OUT/raw/meminfo-$who.txt")
    if [ -n "$p" ]; then metric "pss_${who}_kb" "$p" KB; fi
  done
  tv "logcat -b crash -d" 2> /dev/null | tr -d '\r' > "$OUT/raw/crash.txt"
  metric crashes "$($LIB crashes "$OUT/raw/crash.txt" "$PKGS")" ""
  tv "dumpsys dropbox --print" 2> /dev/null | tr -d '\r' > "$OUT/raw/dropbox.txt"
  $LIB anrs "$OUT/raw/dropbox.txt" "$PKGS" "$START_AT" > "$OUT/raw/anrs.txt"
  metric anrs "$(wc -l < "$OUT/raw/anrs.txt" | tr -d ' ')" ""
  pids=""
  for pkg in ${PKGS//,/ }; do pids="$pids,$(tv "pidof $pkg" 2> /dev/null | tr -d '\r' | awk '{print $1}')"; done
  tv "logcat -v threadtime -d" 2> /dev/null | tr -d '\r' > "$OUT/raw/logcat.txt"
  $LIB frozen "$OUT/raw/logcat.txt" "${pids#,}" "$FROZEN_REGEX" 120 > "$OUT/raw/frozen.txt"
  metric frozen_lines "$(wc -l < "$OUT/raw/frozen.txt" | tr -d ' ')" ""
fi

# ---- the report
restore
verdict=$($LIB report "$OUT" "$PREV")
echo
echo "report: $OUT/report.md ($verdict)"
[ -z "$PREV" ] || echo "compared with: $PREV"
echo "to hand it to the others: git add tester/auto/out/$BUILD && push to work/tester-report"
[ "$verdict" = green ]
