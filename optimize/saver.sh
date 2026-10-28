#!/usr/bin/env bash
# شاشة تلفزيونك's settings, for agents: no remote, no menus.
#   optimize/saver.sh show                         current settings, and last which screensaver is active (ours or the TV's own)
#   optimize/saver.sh off                          the TV's own screensaver back (the one from before setup; else a stock one; else none);
#                                                  ours stays installed, with its settings
#   optimize/saver.sh on                           ours again
#   optimize/saver.sh cities                       built-in Saudi cities by region
#   optimize/saver.sh city الرس                    a built-in city (Arabic name as listed, or a short form of it: مكة; says if it fits several)
#   optimize/saver.sh city 'اسم' 25.86 43.49 Asia/Riyadh   any place: name, latitude, longitude and its time zone (refused without the zone
#                                                      unless the name is a built-in city; a built-in city is always Asia/Riyadh)
#   optimize/saver.sh city 'الكويت' 29.37 47.98 Asia/Kuwait
#   optimize/saver.sh find-team الهلال             a club's id (Arabic or English name): id:Name | league
#   optimize/saver.sh teams 136013:Al-Hilal 150643:Al-Zulfi    followed teams in order (up to 20; more are left out and listed);
#                                                  `teams none` (or `teams ""`) follows no team: the defaults (الهلال، المنتخب السعودي) go
#   optimize/saver.sh card prayers                 beside the clock: shows prayers forecast hourly prayerrow matches verse wisdom adhkar stocks tasks year smart today none
#   optimize/saver.sh card2 hourly                 a second card beside the clock, shown only while the clock is small (clocksize small):
#                                                  the same choices but nextprayer and the first card; card2 none = one card (default)
#   optimize/saver.sh home-card verse,hourly,year  on the home screen (launcher): up to 3 cards, same choices and nextprayer (home only), in this order
#                                                  (one value works too; none = off). Two sit side by side, a third under them
#   optimize/saver.sh greeting off                 home screen: the greeting (by prayer time) and the weather line under it: on (default) or off
#   optimize/saver.sh listenpill off               home screen: the «تلاوة وأذان» button: on (default) or off. Off removes only the button: the adhan, its alarm and bar, the
#                                                  any-key stop, the «يُتلى الآن» card and the Quran stay; the screen opens from the app menu (long OK on an app) or the card
#   optimize/saver.sh watchnext off                home screen: «أكمل المشاهدة» under the favourites: on (default) or off. It shows once the home app may
#                                                  read the TV's Watch Next list (the owner allows it once from the settings row)
#   optimize/saver.sh updatebell off               home screen: the «جرس التحديثات» bell: on (default) or off. It shows only while the Play Store has app updates waiting;
#                                                  off removes only the bell
#   optimize/saver.sh appearance auto              home screen: «المظهر ← الإضاءة», dark (default), light, or auto (light from sunrise until Maghrib in the city)
#   optimize/saver.sh look navy                    all our screens: «المظهر ← الشكل», glass (default), graphite, navy, or clean
#   optimize/saver.sh adhanbar off                 the slim bar before the adhan (last 10 minutes, then until the iqama) on the screensaver and the home screen: on (default) or off
#   optimize/saver.sh updatechannel next          TESTING ONLY (agents; no screen shows it): «تحديث تلفزيوني» reads the version and the APKs from the apk-next release instead
#                                                  of apk-latest: next or latest (default). Same signature, newer-version and permission checks. Put it back to latest after
#   optimize/saver.sh yearstrip on                 the slim year strip under the screensaver's clock (365 dots in 7 rows, today lit, no words): on or off (default)
#   optimize/saver.sh sunrise on                   the prayer panel counts to the sunrise after Fajr's iqama, and tells it for 30 minutes: on or off (default)
#   optimize/saver.sh prayerbar on                 a thin bar under the prayer panel, how far it is from the last prayer to the next: on or off (default)
#   optimize/saver.sh wxmore on                    the humidity and the wind under the city at the top left: on or off (default)
#   optimize/saver.sh months levant                the Gregorian month's names on both screens: default (يناير…) or levant (كانون الثاني…)
#   optimize/saver.sh genres Drama,Comedy          episodes card: Drama Comedy Action Crime Thriller Mystery
#                                                  Science-Fiction Fantasy Adventure Romance Family History Horror
#                                                  Anime Documentary Sports  ("" = all)
#   optimize/saver.sh langs Arabic,English         episodes card: English Arabic Korean Japanese Turkish Spanish
#                                                  French Hindi German  ("" = all)
#   optimize/saver.sh minrating 7                  episodes card: lowest rating shown: 0 6 6.5 7 7.5 8
#   optimize/saver.sh photos 15                    change the photo every N minutes: 5 10 15 30 60 (default 30)
#   optimize/saver.sh idle 10                      the TV starts the screensaver after N idle minutes: 1 2 5 10 15 30
#                                                  (the TV's own timer; left alone unless you run this)
#   optimize/saver.sh cardstyle glass1             look of the card beside the clock: current dark1 glass1 dark3 glass3
#   optimize/saver.sh national off                 the quiet green look on National Day (21-25 Sep) and Founding Day (20-24 Feb): auto (default) or off
#   optimize/saver.sh digits latin                 numbers: hindi = ١٢٣ (default), latin = 123
#   optimize/saver.sh hijrioffset -1               the Hijri date moved by whole days, -2..2 (default 0 = Umm al-Qura as it is): where the month
#                                                  starts by moon sighting (Egypt, Jordan, the Levant, Morocco); the page and the home screen both follow
#   optimize/saver.sh clock24 on                   every time of day as 24-hour (١٩:٣٠) instead of 12-hour (٧:٣٠ م): on | off (default off)
#   optimize/saver.sh markets tasi,gold,btc        top left, up to 4 in order: tasi aramco spx btc gold silver oil xrp (default tasi,spx,btc)
#   optimize/saver.sh markets off                  hide the markets line at the top (only the weather stays; the list is kept and nothing is fetched for it); `markets on` brings it back
#   optimize/saver.sh clocksize medium             the screensaver's clock: large (default) medium small
#   optimize/saver.sh mkstyle prices               markets: arrows (▲▼ only, default) or prices (price and day's change)
#   optimize/saver.sh stocks 2222 1120 AAPL        watchlist of the stocks card (card stocks): up to 8 Tadawul codes (4 digits) or US tickers
#   optimize/saver.sh tasks list                   «المهام» kept on the TV; N = the number list shows
#   optimize/saver.sh tasks add 'نص'               (up to 12, 60 characters each)  |  done N  |  del N  |  clear
#   optimize/saver.sh slot fasting                 the bottom-left slot: matches (default; the weather when no team is followed)
#                                                  weather fasting countdown markets none
#   optimize/saver.sh countdown 2026-12-01 'رحلة الصيف'    the date and name slot=countdown counts down to ("clear" removes it)
#   optimize/saver.sh leagues 'Saudi-Arabian Pro League'   whole leagues, up to 5, by English or Arabic name (دوري روشن السعودي);
#                                                  every game of every club in them joins the teams' games ("" = none)
#   optimize/saver.sh events f1,ufc               other sports beside the teams' games: f1 ufc boxing  ("" = none)
#   optimize/saver.sh prayermethod jafari|gulf|auto  «طريقة الحساب»: auto (the default) = the city's country's own, gulf = the Gulf method (Fajr 19.5°, Isha 90 min) for any place, jafari = Shia Ithna-Ashari (Leva, Qum)
#   optimize/saver.sh prayeradj on|off             «تعديل وقت الصلاة» (off by default): each prayer moved by its own whole minutes, -20..20
#   optimize/saver.sh prayeradj fajr +2 maghrib -1   the minutes of fajr dhuhr asr maghrib isha (the others stay); sunrise is never moved
#   optimize/saver.sh export > settings.json       the settings to move to a second TV (city, prayer method, cards, markets, teams, adhan…;
#                                                  never tasks, photos, the person's own adhan, Wi-Fi or keys): one JSON file, {"schema":1,…}
#   optimize/saver.sh import settings.json         what it would change on this TV (old → new), nothing is sent; add --yes to apply it: all or
#                                                  nothing, and the settings from before are saved in optimize/run/settings-before-import.json
#                                                  (`import optimize/run/settings-before-import.json --yes` undoes it)
#   optimize/saver.sh update                       download the latest screen now
# Only ADB can change these (sa.urtv.screensaver/.SaverReceiver needs the shell's DUMP).
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
send() {   # $1 action, $2 extras; prints the receiver's answer (the "data" of am broadcast); exit 1 on "error:" or when there is no answer
  local raw out
  raw=$("$PY" -I optimize/tv.py "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.$1 ${2:-}" | tr -d '\r') || raw=""
  # no «data=» at all = the receiver never ran (app missing, another signature, just updated): that is not «done»
  if ! grep -q 'data=' <<< "$raw"; then
    if [ "$1" = UPDATE ]; then   # the download can still be running: no answer yet is not «not installed»
      echo "!! saver.sh: no answer to the update (it may still be downloading): wait a minute, then optimize/saver.sh show says the version; if it did not change, run the same command again." >&2
      return 1
    fi
    echo "!! saver.sh: no answer from the TV's screensaver app (it is not installed, was just updated, or the connection dropped), so nothing is known to have changed. Run optimize/install.sh saver, then the same command again." >&2
    return 1
  fi
  out=$(sed -n '/data="/,$p' <<< "$raw" | sed '1s/.*data="//; $s/"$//')
  echo "$out"
  [[ "$out" != error:* ]]
}
OURS=sa.urtv.screensaver/.ScreenDream
BACKUP="${SAVER_BACKUP:-optimize/run/backup.txt}"   # optimize/backup.sh's copy of the TV as it came
active() {   # one line: which screensaver the TV uses now
  local out comp en
  out=$(printf '%s\n' "settings get secure screensaver_components" "settings get secure screensaver_enabled" \
    | "$PY" -I optimize/tv.py --batch | tr -d '\r' | grep -v '^\$ ' || true)
  comp=$(sed -n 1p <<< "$out"); en=$(sed -n 2p <<< "$out"); comp="${comp%%,*}"
  if [ "$en" = 0 ]; then echo "screensaver now: off (the TV's screensaver switch is off)"
  elif [ -z "$comp" ] || [ "$comp" = null ]; then echo "screensaver now: the TV's own default"
  elif [[ "$comp" == sa.urtv.screensaver/* ]]; then echo "screensaver now: ours (شاشة تلفزيونك)"
  else echo "screensaver now: the TV's own, $comp"; fi
}
stock_dream() {   # a screensaver this TV lists besides ours: component, or nothing; returns 2 when the TV did not list its screensavers at all
  local raw list
  raw=$("$PY" -I optimize/tv.py "(cmd package query-services --brief -a android.service.dreams.DreamService || pm query-services --brief -a android.service.dreams.DreamService) 2> /dev/null" \
    | tr -d '\r' | sed 's/^[[:space:]]*//' | grep -E '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' || true)   # the real output is indented under «Service #N:»
  # a TV that answered lists at least one screensaver (ours, if it is installed); an empty answer is «the TV did not say», not «it has none of its own»
  [ -n "$raw" ] || return 2
  list=$(grep -v '^sa\.urtv\.' <<< "$raw" || true)
  { grep -iE 'backdrop|dream|ambient|photo' <<< "$list" || true; echo "$list"; } | grep -m1 . || true
}
quote() {  # one shell-safe argument for the TV's shell
  case "$1" in *"'"*) echo "no apostrophes please: $1" >&2; exit 1 ;; esac
  printf "'%s'" "$1"
}
# One extra for a SET: the value quoted FIRST, so an apostrophe stops the command. (Inside "$(quote ...)" in the middle of a
# word the failure was lost and an empty value was sent.)
set1() {   # $1 the extra's name, $2 its value
  local v
  v=$(quote "$2") || exit 1
  send SET "--es $1 $v"
}
case "${1:-}" in
  show)   rc=0; send GET || rc=$?; active; exit "$rc" ;;   # the version line stays first
  on)
    "$PY" -I optimize/tv.py "pm list packages sa.urtv.screensaver" | tr -d '\r' | grep -qx 'package:sa.urtv.screensaver' \
      || { echo "!! Our screensaver is not installed on this TV: optimize/install.sh saver"; exit 1; }
    printf '%s\n' "settings put secure screensaver_enabled 1" "settings put secure screensaver_components $OURS" \
      "settings put secure screensaver_default_component $OURS" | "$PY" -I optimize/tv.py --batch > /dev/null
    active ;;
  off)
    prev=; enabled=
    if [ -f "$BACKUP" ]; then   # what the TV had before setup: components, enabled, screen_off_timeout (backup.sh)
      vals=$(tr -d '\r' < "$BACKUP" | awk '/^== /{s=($2=="screensaver")} s&&!/^==/{print}')
      prev=$(sed -n 1p <<< "$vals"); enabled=$(sed -n 2p <<< "$vals")
    fi
    prev="${prev%%,*}"
    case "$prev" in ""|null|sa.urtv.screensaver/*) prev= ;; esac   # none then, or a backup taken with ours already set
    if [ -n "$prev" ]; then why="the one from before setup"; else
      why="a stock one"; prev=$(stock_dream) || {
        echo "!! saver.sh off: the TV did not list its screensavers, so nothing was changed. Run the same command again."; exit 1; }
    fi
    if [ -n "$prev" ] && [[ "$prev" =~ ^[A-Za-z0-9_.]+/[A-Za-z0-9_.\$]+$ ]]; then
      [ -f "$BACKUP" ] || enabled=1   # no backup at all: the stock screensaver is switched on, as before
      case "$enabled" in 0|1) ;; "") ;; *) enabled=1 ;; esac   # a backup whose line is empty (it could not read it): the switch is left as it is
      {
        [ -z "$enabled" ] || echo "settings put secure screensaver_enabled $enabled"
        echo "settings put secure screensaver_components '$prev'"
        echo "settings delete secure screensaver_default_component"
      } | "$PY" -I optimize/tv.py --batch > /dev/null
      echo "Ours is off ($why is back). It stays installed: optimize/saver.sh on turns it on again."
    else
      printf '%s\n' "settings put secure screensaver_enabled 0" "settings delete secure screensaver_components" \
        "settings delete secure screensaver_default_component" | "$PY" -I optimize/tv.py --batch > /dev/null
      echo "Ours is off, and this TV lists no screensaver of its own, so screensavers are off. It stays installed: optimize/saver.sh on turns it on again."
    fi
    active ;;
  cities) send CITIES ;;
  update) send UPDATE ;;
  city)
    [ $# -eq 2 ] || [ $# -eq 4 ] || [ $# -eq 5 ] || { echo "usage: optimize/saver.sh city NAME [LAT LON [TIMEZONE]]"; exit 1; }
    v=$(quote "$2") || exit 1
    extra="--es city $v"
    if [ $# -ge 4 ]; then
      v=$(quote "$3") || exit 1; w=$(quote "$4") || exit 1
      extra="$extra --es lat $v --es lon $w"
    fi
    if [ $# -eq 5 ]; then v=$(quote "$5") || exit 1; extra="$extra --es tz $v"; fi
    send SET "$extra" ;;
  teams)
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/saver.sh teams ID:EnglishName ... | none"; exit 1; }
    if [ $# -eq 1 ] && { [ "$1" = none ] || [ -z "$1" ]; }; then   # no team at all: an empty list (the app keeps it empty)
      set1 teams ""; exit $?
    fi
    [ "$1" != none ] || { echo "usage: optimize/saver.sh teams none   (alone: it clears the followed teams)"; exit 1; }
    if [ $# -gt 20 ]; then   # the app keeps the first 20 and drops the rest without a word: say so here
      echo "Only 20 teams can be followed. Kept: ${*:1:20}"
      echo "Left out (${#} given): ${*:11}"
      set -- "${@:1:20}"
    fi
    set1 teams "$(IFS=,; echo "$*")" ;;
  card)     [ "${2:-}" = nextprayer ] && { echo "!! nextprayer is a home-screen card only (the screensaver's bottom-right panel already shows the next prayer): optimize/saver.sh home-card nextprayer"; exit 1; }
            set1 panel "${2:?usage: optimize/saver.sh card prayers}" ;;
  card2)    [ "${2:-}" = nextprayer ] && { echo "!! nextprayer is a home-screen card only (the screensaver's bottom-right panel already shows the next prayer): pick another card2, or card2 none"; exit 1; }
            set1 panel2 "${2:?usage: optimize/saver.sh card2 hourly | none   (shown beside the small clock: optimize/saver.sh clocksize small)}" ;;
  home-card)set1 home_panel "${2?usage: optimize/saver.sh home-card verse,hourly,year | none}" ;;
  greeting) set1 greeting "${2:?usage: optimize/saver.sh greeting on|off}" ;;
  listenpill) set1 listenpill "${2:?usage: optimize/saver.sh listenpill on|off}" ;;
  updatebell) set1 updatebell "${2:?usage: optimize/saver.sh updatebell on|off}" ;;
  watchnext) set1 watchnext "${2:?usage: optimize/saver.sh watchnext on|off}" ;;
  appearance) set1 appearance "${2:?usage: optimize/saver.sh appearance dark|light|auto}" ;;
  look) set1 look "${2:?usage: optimize/saver.sh look glass|graphite|navy|clean}" ;;
  adhanbar) set1 adhanbar "${2:?usage: optimize/saver.sh adhanbar on|off}" ;;
  updatechannel) set1 updatechannel "${2:?usage: optimize/saver.sh updatechannel next|latest   (testing only)}" ;;
  yearstrip)set1 yearstrip "${2:?usage: optimize/saver.sh yearstrip on|off}" ;;
  sunrise)  set1 sunrise "${2:?usage: optimize/saver.sh sunrise on|off}" ;;
  prayerbar)set1 prayerbar "${2:?usage: optimize/saver.sh prayerbar on|off}" ;;
  wxmore)   set1 wxmore "${2:?usage: optimize/saver.sh wxmore on|off}" ;;
  months)   set1 months "${2:?usage: optimize/saver.sh months default|levant}" ;;
  genres)   set1 genres "${2-}" ;;
  # no value at all is a question, not a change: it used to clear them (`""` still means none)
  leagues)  [ $# -ge 2 ] || { echo "usage: optimize/saver.sh leagues 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)"; exit 1; }
            set1 leagues "$2" ;;
  events)   [ $# -ge 2 ] || { echo "usage: optimize/saver.sh events f1,ufc,boxing ('' = none)"; exit 1; }
            set1 events "$2" ;;
  langs)    [ $# -ge 2 ] || { echo "usage: optimize/saver.sh langs Arabic,English"; exit 1; }
            set1 langs "$2" ;;
  minrating)set1 minrating "${2:?usage: optimize/saver.sh minrating 7}" ;;
  photos)   set1 photos "${2:?usage: optimize/saver.sh photos 15}" ;;
  cardstyle)set1 cardstyle "${2:?usage: optimize/saver.sh cardstyle glass1}" ;;
  markets)  case "${2:?usage: optimize/saver.sh markets tasi,gold,btc | off | on}" in
              off|on) set1 mkbar "$2" ;;
              *) set1 markets "$2" && set1 mkbar on > /dev/null ;;   # choosing markets shows the line
            esac ;;
  clocksize)set1 clocksize "${2:?usage: optimize/saver.sh clocksize large|medium|small}" ;;
  mkstyle)  set1 mkstyle "${2:?usage: optimize/saver.sh mkstyle prices}" ;;
  stocks)
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/saver.sh stocks 2222 1120 AAPL"; exit 1; }
    set1 stocks "$(IFS=,; echo "$*")" ;;
  slot)     set1 slot "${2:?usage: optimize/saver.sh slot fasting}" ;;
  countdown)
    [ $# -ge 2 ] && [ $# -le 3 ] || { echo "usage: optimize/saver.sh countdown 2026-12-01 'name' | clear"; exit 1; }
    v=$(quote "$2") || exit 1
    extra="--es countdown $v"
    if [ $# -eq 3 ]; then v=$(quote "$3") || exit 1; extra="$extra --es cdname $v"; fi
    send SET "$extra" ;;
  national) set1 national "${2:?usage: optimize/saver.sh national auto|off}" ;;
  digits)   set1 digits "${2:?usage: optimize/saver.sh digits latin}" ;;
  hijrioffset)
    [[ "${2:-}" =~ ^[+-]?[0-2]$ ]] || { echo "usage: optimize/saver.sh hijrioffset -2|-1|0|+1|+2   (days; 0 = Umm al-Qura as it is)"; exit 1; }
    v=$(quote "$2") || exit 1
    send SET "--es hijrioffset $v" ;;   # the offset moves what is shown, never a prayer time: no adhan alarm to set again
  clock24)  case "${2:-}" in on|off) set1 clock24 "$2" ;; *) echo "usage: optimize/saver.sh clock24 on|off"; exit 1 ;; esac ;;
  idle)
    # the app may change the TV's timer only if the owner allowed it; the shell always may
    v=$(quote "${2:?usage: optimize/saver.sh idle 10}") || exit 1
    out=$(send SET "--es idle $v") || true
    case "$out" in
      "error: idle not allowed"*)
        case "$2" in 1|2|5|10|15|30) ;; *) echo "error: idle is one of 1 2 5 10 15 30"; exit 1 ;; esac
        "$PY" -I optimize/tv.py "settings put system screen_off_timeout $(($2 * 60000))"
        echo "idle: $2 (set from the shell)" ;;
      error:*) echo "$out"; exit 1 ;;
      *) echo "$out" ;;
    esac ;;
  tasks)
    case "${2:-}" in
      list)  send TASKS "--es op list" ;;
      clear) send TASKS "--es op clear" ;;
      add)   [ $# -eq 3 ] || { echo "usage: optimize/saver.sh tasks add 'نص'"; exit 1; }
             text=$(quote "$3") || exit 1   # refuse before anything is sent
             send TASKS "--es op add --es text $text" ;;
      done|del)
             [[ "${3:-}" =~ ^[0-9]+$ ]] || { echo "usage: optimize/saver.sh tasks $2 N   (N from 'tasks list')"; exit 1; }
             send TASKS "--es op $2 --ei n $3" ;;
      *) echo "usage: optimize/saver.sh tasks list | add 'نص' | done N | del N | clear"; exit 1 ;;
    esac ;;
  prayermethod)
    shift
    case "${1:-}" in
      jafari|gulf|auto) [ $# -eq 1 ] || { echo "usage: optimize/saver.sh prayermethod jafari|gulf|auto"; exit 1; } ;;
      *) echo "usage: optimize/saver.sh prayermethod jafari|gulf|auto   (auto = the city's country's own method)"; exit 1 ;;
    esac
    rc=0; send SET "--es prayermethod $1" || rc=$?
    # the home app's adhan alarm was set for the old times: set it again now (no launcher, no harm)
    [ "$rc" != 0 ] || "$PY" -I optimize/tv.py "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_RESCHEDULE" > /dev/null 2>&1 || true
    exit "$rc" ;;
  prayeradj)
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/saver.sh prayeradj on|off | fajr +2 | maghrib -1 …   (fajr dhuhr asr maghrib isha; minutes -20..20)"; exit 1; }
    case "$1" in
      on|off) [ $# -eq 1 ] || { echo "usage: optimize/saver.sh prayeradj on|off"; exit 1; }
              extra="--es prayeradj $1" ;;
      *) extra=
         while [ $# -gt 0 ]; do
           [ $# -ge 2 ] || { echo "usage: optimize/saver.sh prayeradj fajr +2 maghrib -1 …   (each prayer, then its minutes)"; exit 1; }
           case "$1" in
             fajr|الفجر) key=fajr ;; dhuhr|الظهر) key=dhuhr ;; asr|العصر) key=asr ;; maghrib|المغرب) key=maghrib ;; isha|العشاء) key=isha ;;
             *) echo "error: $1 is not a prayer: fajr dhuhr asr maghrib isha"; exit 1 ;;
           esac
           v=$(quote "$2") || exit 1
           extra="$extra --es adj_$key $v"
           shift 2
         done ;;
    esac
    rc=0; send SET "${extra# }" || rc=$?
    # the home app's adhan alarm was set for the old minutes: set it again now (no launcher, no harm)
    [ "$rc" != 0 ] || "$PY" -I optimize/tv.py "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_RESCHEDULE" > /dev/null 2>&1 || true
    exit "$rc" ;;
  export|import) SAVER_CALLER_DIR="${OLDPWD:-}" "$PY" -I optimize/settings_move.py "$@" ;;   # the logic is in Python: validated before anything is sent
  find-team) shift; "$PY" -I optimize/find-team.py "$@" ;;   # Arabic or English name
  *) sed -n '2,/^# Only ADB/p' "$0"; exit 1 ;;
esac
