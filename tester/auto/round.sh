#!/usr/bin/env bash
# The automatic tester round (4.4 beta 4): one command on the owner's PC, with the TV on the network and adb working (optimize/find-tv.sh did it).
#   tester/auto/round.sh [--tag apk-next] [--build NAME] [--no-install] [--only a,b,c] [--prev DIR]
# It installs the build of the tag (apk-next by default) OVER the installed one, so the data stays; saves the settings and the sound; then runs, with nobody
# touching the remote, every check below and writes tester/auto/out/<build>/report.md with the screenshots (the city boxed) and a column of the previous
# build's numbers. At the end it puts the settings, the sound, the display and the font back. Steps (--only takes these words, in this order):
#   install   layouts   reach   settings   saver   radio   display   font   adhan   games   health
# Never touches the family PIN, the TV's setup wizard or the old launcher, never changes the Wi-Fi, never restarts the TV. NEVER PRESSES BLIND: a key is sent only after
# the screen it is for was read (the front app, or the screen's own text through uiautomator), and a screen that is not the one expected is a FAIL, not a retry with more keys.
# What it reads is in tester/auto/lib.py (tested by tests/tester_auto_test.py); what it opens is in tester/auto/screens.tsv; its limits are in thresholds.tsv.
# Stand-ins for a test: ROUND_TV (default «optimize/venv/bin/python -I optimize/tv.py»), ROUND_SAVER, ROUND_MUSIC, ROUND_INSTALL, ROUND_STARTSAVER, ROUND_FAST=1 (no waiting).
set -uo pipefail
export PYTHONUTF8=1   # Arabic on a Windows console (cp1252) must not break lib.py (tester b654)
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

TAG=apk-next BUILD="" DOINSTALL=1 ONLY="" PREV="" SKIP="${ROUND_SKIP:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --tag) TAG="${2:?--tag NAME}"; shift 2 ;;
    --build) BUILD="${2:?--build NAME}"; shift 2 ;;
    --no-install) DOINSTALL=0; shift ;;
    --only) ONLY="${2:?--only a,b,c}"; shift 2 ;;
    --prev) PREV="${2:?--prev DIR}"; shift 2 ;;
    --skip) SKIP="${2:?--skip a,b}"; shift 2 ;;   # steps left out (and said so in the report): the CI emulator leaves out what it cannot do
    *) sed -n '2,12p' "$0"; exit 2 ;;
  esac
done

tv() { $TV "$@"; }                        # the TV's shell (or a tv.py option such as --shot FILE)
nap() { [ "${ROUND_FAST:-0}" = 1 ] || sleep "$1"; }
want() {
  case ",$SKIP," in *",$1,"*) return 1 ;; esac   # left out on purpose
  [ -z "$ONLY" ] || case ",$ONLY," in *",$1,"*) true ;; *) false ;; esac
}

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

for s in ${SKIP//,/ }; do printf '%s\t%s\t%s\n' "$s" SKIP "left out of this round (--skip / ROUND_SKIP)" >> "$OUT/steps.tsv"; done
step() { local msg="${3//$'\n'/ }"; msg="${msg//$'\t'/ }"; printf '%s\t%s\t%s\n' "$1" "$2" "$msg" >> "$OUT/steps.tsv"; echo "  $2  $1  $msg"; }   # one line a step, whatever it was given
metric() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$OUT/metrics.tsv"; echo "  metric $1 = $2$3"; }
rows() { grep -v '^#' tester/auto/screens.tsv | awk -F'\t' -v k="$1" '$1 == k'; }   # the rows of one kind

# ---- reading the screen (never a key before it)
front() { tv "dumpsys activity activities" 2> /dev/null | tr -d '\r' | $LIB front-app; }   # the whole dump is read here, in Python: nothing is piped to head on the TV (its «Broken pipe» line, tester b654)
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
PRESSES=0 TRAIL=""
reach_go() {   # $1 a name on the screen, $2 at most how many keys: the arrow a person presses toward it, read from the screen each time (lib toward), every key
  local i now fk dir was=""   # counted in PRESSES and kept in TRAIL; a key that left the focus in the same place (its name and place) is not counted and stops the walk.
  for i in $(seq 0 "$2"); do   # (emulator run 9: walking left first from «الجوال» took 8 presses where 3 to the right were enough)
    dump "$OUT/raw/focus.xml"
    now=$($LIB focused "$OUT/raw/focus.xml"); fk=$($LIB focus-key "$OUT/raw/focus.xml")
    [ "$i" -gt 0 ] && TRAIL="$TRAIL $dir→${now:-?}"
    dir=$($LIB toward "$OUT/raw/focus.xml" "$1")
    if [ "$dir" = here ]; then return 0; fi
    if [ "$dir" = none ]; then TRAIL="$TRAIL (no «$1» on the screen)"; return 1; fi
    if [ "$i" -gt 0 ] && [ "$fk" = "$was" ]; then PRESSES=$((PRESSES - 1)); TRAIL="$TRAIL (stuck)"; return 1; fi
    was=$fk
    [ "$i" -lt "$2" ] && key "KEYCODE_DPAD_$dir" && PRESSES=$((PRESSES + 1)) && nap 1
  done
  return 1
}

open_tilawah() {   # the تلاوة screen as a person opens it: Home, the «تلاوة وأذان» pill, OK (the screen is not exported: no `am start` from adb can open it, which is why the walk pressed on the home itself)
  key KEYCODE_HOME; nap 3
  on_top "$HOME_PKG" || return 1
  focus_walk "تلاوة وأذان" KEYCODE_DPAD_UP 4 || focus_walk "تلاوة وأذان" KEYCODE_DPAD_RIGHT 10 || focus_walk "تلاوة وأذان" KEYCODE_DPAD_LEFT 20 || return 1
  key KEYCODE_DPAD_CENTER; nap 3
}

# ---- what is put back at the end
SAVED_IDLE="" WM_BACK="wm size reset; wm density reset" LAYOUT_BEFORE="" PANEL_BEFORE="" SAVED_FONT="" SAVED_VOL="" SAVED_MUTED="" DISPLAY_CHANGED=0 FONT_CHANGED=0 SETTINGS_SAVED=0
restore() {
  trap - EXIT
  echo "putting everything back…"
  if [ "$DISPLAY_CHANGED" = 1 ]; then tv "$WM_BACK" > /dev/null 2>&1; fi
  if [ "$FONT_CHANGED" = 1 ]; then
    if [ -z "$SAVED_FONT" ] || [ "$SAVED_FONT" = null ]; then tv "settings delete system font_scale" > /dev/null 2>&1; else tv "settings put system font_scale $SAVED_FONT" > /dev/null 2>&1; fi
  fi
  [ "$SETTINGS_SAVED" = 1 ] && $SAVER import "$OUT/settings-before.json" --yes > "$OUT/raw/restore-settings.txt" 2>&1
  # what the export does not hold (review F102): the home's layout and card, the adhan of each prayer
  if [ -n "$LAYOUT_BEFORE" ]; then $SAVER homelayout "$LAYOUT_BEFORE" > /dev/null 2>&1; fi
  if [ -n "$PANEL_BEFORE" ]; then $SAVER home-card "$PANEL_BEFORE" > /dev/null 2>&1; fi
  if [ -s "$OUT/raw/adhan-before.txt" ]; then   # each prayer's own sound (a recording of its own, silent, general) and its screen exactly as they were, never through the state words
    while read -r pk psnd pscr; do
      [ -n "$pk" ] || continue
      case "$psnd" in v[0-9]*) psnd=${psnd#v} ;; esac
      $MUSIC adhan prayer "$pk" "$psnd" > /dev/null 2>&1; $MUSIC adhan screen "$pk" "$pscr" > /dev/null 2>&1
    done < <($LIB adhan-exact "$OUT/raw/adhan-before.txt")
  fi
  if [ -n "$SAVED_IDLE" ]; then tv "settings put system screen_off_timeout $SAVED_IDLE" > /dev/null 2>&1; fi   # the owner's idle (45 s, say), not the round's 10 minutes
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
LAYOUT_BEFORE=$($LIB kv "$OUT/raw/show-before.txt" homelayout); PANEL_BEFORE=$($LIB kv "$OUT/raw/show-before.txt" home_panel)
$MUSIC adhan 2> /dev/null | tr -d '\r' > "$OUT/raw/adhan-before.txt"
tv "wm size; wm density" 2> /dev/null | tr -d '\r' > "$OUT/raw/wm-before.txt"
WM_BACK=$($LIB wm-back "$OUT/raw/wm-before.txt")
# a long idle for the round: with the owner's 45 s the saver started in the middle of it and every later step failed (tester b654); put back in restore()
SAVED_IDLE=$(tv "settings get system screen_off_timeout" 2> /dev/null | tr -d '\r' | tail -n 1)
case "$SAVED_IDLE" in ''|*[!0-9]*) SAVED_IDLE="" ;; *) tv "settings put system screen_off_timeout 600000" > /dev/null 2>&1 ;; esac
CITY=$($LIB city "$OUT/raw/show-before.txt")
STYLE_BEFORE=$($LIB style "$OUT/raw/show-before.txt")
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

# ---- from the home to our settings, in each layout and card, by the keys a person presses (the tester, build 654: in «كنبة» they were not found):
# up to «مركز التحكم», OK, its row «الإعدادات», OK; at most REACH_MAX presses, Home not counted
REACH_MAX=6
if want reach; then
  echo "reach…"
  while IFS=$'\t' read -r _ lkey lname _ _ <&3; do
    while IFS=$'\t' read -r _ ckey cname _ _ <&4; do
      name="reach-$lkey-$ckey"
      if ! $SAVER homelayout "$lkey" > /dev/null 2>&1 || ! $SAVER home-card "$ckey" > /dev/null 2>&1; then step "$name" FAIL "saver.sh refused homelayout $lkey / home-card $ckey"; continue; fi
      key KEYCODE_HOME; nap 8
      if ! on_top "$HOME_PKG"; then step "$name" FAIL "$lname، $cname: the home did not come to the front: $(front)"; continue; fi
      PRESSES=0; dump "$OUT/raw/focus.xml"; TRAIL="start→$($LIB focused "$OUT/raw/focus.xml")"
      if ! reach_go "مركز التحكم" 12; then
        cp "$OUT/raw/focus.xml" "$OUT/raw/reach-$lkey-$ckey.xml" 2> /dev/null   # the screen it stopped on, for whoever reads the FAIL
        step "$name" FAIL "$lname، $cname: «مركز التحكم» was not reached from the home ($PRESSES presses; $TRAIL)"; continue
      fi
      key KEYCODE_DPAD_CENTER; PRESSES=$((PRESSES + 1)); nap 2
      if ! reach_go "الإعدادات" 4; then cp "$OUT/raw/focus.xml" "$OUT/raw/reach-$lkey-$ckey.xml" 2> /dev/null; step "$name" FAIL "$lname، $cname: «مركز التحكم» has no «الإعدادات» row ($PRESSES presses; $TRAIL)"; key KEYCODE_BACK; continue; fi
      key KEYCODE_DPAD_CENTER; PRESSES=$((PRESSES + 1))
      if ! on_top "$SAVER_PKG" || ! see "بحث في الإعدادات"; then step "$name" FAIL "$lname، $cname: OK on «الإعدادات» did not open the settings: $(front)"
      elif [ "$PRESSES" -le "$REACH_MAX" ]; then step "$name" PASS "$lname، $cname: the settings in $PRESSES presses from the home ($TRAIL)"
      else step "$name" FAIL "$lname، $cname: the settings took $PRESSES presses from the home (at most $REACH_MAX; $TRAIL)"; fi
    done 4< <(rows card)
  done 3< <(rows layout)
  $SAVER homelayout classic > /dev/null 2>&1; $SAVER home-card none > /dev/null 2>&1
  key KEYCODE_HOME
fi

# ---- the settings, each style opened to each section
if want settings; then
  echo "settings…"
  key KEYCODE_HOME; nap 2   # every part starts from the home (tester b654)
  if [ -z "$STYLE_BEFORE" ]; then   # an older build does not say its settings style in `saver.sh show`: walking the three styles would leave the TV in the last one (review F102, second pass)
    step "settings" SKIP "this build's saver.sh show does not say the settings style, so the three styles are not walked (they could not be put back): update the TV to this build and run again"
  else
  while IFS=$'\t' read -r _ stkey stname stcmd _ <&3; do
    tv "am broadcast -n $SAVER_PKG/.SaverReceiver -a $SAVER_PKG.SET --es settingsstyle $stcmd" > "$OUT/raw/style-$stkey.txt" 2>&1 || { step "settings-$stkey" FAIL "the settings style $stcmd was not accepted"; continue; }
    nap 2
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
  fi
  if [ -n "$STYLE_BEFORE" ]; then tv "am broadcast -n $SAVER_PKG/.SaverReceiver -a $SAVER_PKG.SET --es settingsstyle $STYLE_BEFORE" > /dev/null 2>&1; fi   # back to the style the TV had
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
  key KEYCODE_HOME; nap 2   # every part starts from the home (tester b654)
  $MUSIC stations 2> /dev/null | tr -d '\r' > "$OUT/raw/stations.txt"
  while IFS= read -r line <&3; do
    idx=$(sed -n 's/^[[:space:]]*\([0-9][0-9]*\)[^0-9].*$/\1/p' <<< "$line"); sname=$(sed -n 's/^[[:space:]]*[0-9][0-9]*[^[:alnum:]]*\(.*\)$/\1/p' <<< "$line")
    [ -n "$idx" ] || continue
    name="radio-$idx"
    tries=0; other=""; opened=0; found=0
    while :; do   # the station from the screen: if another app took the front on the way (a TV's own media centre), say who and try once more
      tries=$((tries + 1)); opened=0; found=0
      open_tilawah || break
      opened=1
      # the tabs are «القرّاء», «الإذاعات», «الأذان» (the first at the start side, so Arabic puts «الإذاعات» to the LEFT of it, English to the right): walk to the tab by its name
      # the screen opens with the focus on the list or its letter rail (run 17: on «ع»), where LEFT and RIGHT do not change the tab: UP first, until a tab has it
      for up in 1 2 3 4; do
        dump "$OUT/raw/focus.xml"
        case "$($LIB focused "$OUT/raw/focus.xml")" in "القرّاء"|"الإذاعات"|"الأذان") break ;; esac
        key KEYCODE_DPAD_UP; nap 1
      done
      focus_walk "الإذاعات" KEYCODE_DPAD_LEFT 3 || focus_walk "الإذاعات" KEYCODE_DPAD_RIGHT 6 || break
      nap 1
      focus_walk "$sname" KEYCODE_DPAD_DOWN 30 || break
      found=1
      tv "dumpsys audio" 2> /dev/null | tr -d '\r' > "$OUT/raw/audio-$name-before.txt"
      key KEYCODE_DPAD_CENTER
      nap "$STATION_S"
      tv "logcat -d -s MusicService:V UrTVMute:V" 2> /dev/null | tr -d '\r' > "$OUT/raw/$name.log"
      $MUSIC status 2> /dev/null | tr -d '\r' > "$OUT/raw/$name-status.txt"
      tv "dumpsys audio" 2> /dev/null | tr -d '\r' > "$OUT/raw/audio-$name-after.txt"
      grep -q 'source=radio' "$OUT/raw/$name-status.txt" && break
      front | grep -q "$HOME_PKG" || other=$(front)
      [ "$tries" -ge 2 ] && break
      key KEYCODE_HOME; nap 2
    done
    if [ "$opened" = 0 ]; then
      shot "$name"; cp "$OUT/raw/focus.xml" "$OUT/raw/$name-notfound.xml" 2> /dev/null
      step "$name" FAIL "the «تلاوة وأذان» pill was not reached from the home with the arrow keys (the screen is in shots/$name.png and raw/$name-notfound.xml): $(front)"; key KEYCODE_HOME; nap 2; continue
    fi
    if [ "$found" = 0 ]; then
      shot "$name"; cp "$OUT/raw/focus.xml" "$OUT/raw/$name-notfound.xml" 2> /dev/null   # what the screen showed instead (the walk's own dump) (the next round's reader needs it)
      step "$name" FAIL "the row «$sname» was not found on the stations tab (no key pressed on it; the screen is in shots/$name.png and raw/$name-notfound.xml)"; key KEYCODE_HOME; nap 2; continue
    fi
    shot "$name"
    was=$($LIB muted "$OUT/raw/audio-$name-before.txt"); is=$($LIB muted "$OUT/raw/audio-$name-after.txt")
    if grep -q 'source=radio' "$OUT/raw/$name-status.txt" && [ "$was$is" != nono ] && [ "$was" = "$is" ]; then step "$name" PASS "$sname: playing, the mute unchanged ($is)"
    elif ! grep -q 'source=radio' "$OUT/raw/$name-status.txt"; then step "$name" FAIL "$sname: not playing after ${STATION_S} s${other:+ (another app was in front: $other)} (see raw/$name.log)"
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
  tv "$WM_BACK" > /dev/null 2>&1; DISPLAY_CHANGED=0; nap 4
  key KEYCODE_HOME
fi

# ---- the largest font
if want font; then
  echo "font…"
  key KEYCODE_HOME; nap 2   # every part starts from the home (tester b654)
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
  key KEYCODE_HOME; nap 2   # every part starts from the home (tester b654)
  tv "am start --activity-clear-task -n $SAVER_PKG/.YourTvHubActivity --es open games" > /dev/null 2>&1
  if on_top "$SAVER_PKG" && see "الأسرع"; then shot games-list; step "games-list" PASS "the games list is on the screen (the badges: look at shots/games-list.png)"
  else shot games-list; step "games-list" FAIL "the games list was not on the screen: $(front)"; fi
  key KEYCODE_HOME
fi

# ---- health: cold starts, memory, jank over a scripted walk, crashes, ANRs, frozen lines
if want health; then
  echo "health…"
  key KEYCODE_HOME; nap 2   # every part starts from the home (tester b654)
  for who in home saver; do
    if [ "$who" = home ]; then pkg=$HOME_PKG; act=$HOME_PKG/.MainActivity; else pkg=$SAVER_PKG; act=$SAVER_PKG/.SettingsActivity; fi
    tv "am force-stop $pkg" > /dev/null 2>&1; nap 3
    tv "am start -W -n $act" 2> /dev/null | tr -d '\r' > "$OUT/raw/start-$who.txt"
    t=$($LIB start-time "$OUT/raw/start-$who.txt")
    if [ -n "$t" ]; then metric "cold_start_${who}_ms" "$t" ms
    elif grep -q "TotalTime" "$OUT/raw/start-$who.txt"; then step "cold-start-$who" SKIP "the system had already started it again after the force-stop (TotalTime 0): no cold start was measured, nothing is read as 0 ms"
    else step "cold-start-$who" FAIL "am start -W said no TotalTime (raw/start-$who.txt)"; fi
  done
  key KEYCODE_HOME; nap 6
  if on_top "$HOME_PKG"; then
    for k in RIGHT RIGHT LEFT LEFT DOWN UP; do key "KEYCODE_DPAD_$k"; done; nap 2   # a warm-up walk, not counted: the first frames after a start are cold caches, not the home's jank
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
