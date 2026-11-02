#!/usr/bin/env bash
# Control «تلاوة» (the Quran recitations and the radio, inside the home screen) from a computer over ADB.
#   optimize/music.sh status
#   optimize/music.sh reciters | stations
#   optimize/music.sh surah sudais 1       play a surah (1-114) by a reciter id
#   optimize/music.sh radio 0              a live station (0 = Saudi Quran Radio)
#   optimize/music.sh pause | resume | toggle | next | prev | stop
#   optimize/music.sh seek 10              forward 10 s (negative: back)
#   optimize/music.sh sleep 30             stop after 30 min (0 cancels)
#   optimize/music.sh adhan                the adhan: on/off, the voice, when the next one is
#   optimize/music.sh adhan now [voice|fajr]  play it right now (fajr: the voice chosen for Fajr)
#   optimize/music.sh adhan on | off
#   optimize/music.sh adhan voice 3        which recording plays at Dhuhr, Asr, Maghrib and Isha (1-21, but the four Fajr ones are for Fajr only)
#   optimize/music.sh adhan fajr-voice 10  «مؤذن الفجر»: the recording for Fajr (own = your own recording); default 10, «فجر الحرم المكي»
#   optimize/music.sh adhan short on|off   «أذان مختصر»: the adhan stops after its first minute with a 3-second fade (off by default)
#   optimize/music.sh adhan takbir on|off   «تكبير فقط» (off by default): only the opening takbir plays (about 14 seconds), with a 2-second fade; turns short off
#   optimize/music.sh adhan stopkey on|off   «إيقاف الأذان بأي زر» (ON by default): while the adhan plays, the first key press on the home screen or the
#                                          screensaver stops it and is consumed (it does not also open the focused app or move the focus); the
#                                          volume keys and Power are left alone, Home, Back, OK, the arrows, numbers and media keys all stop it.
#                                          Off: only OK on the adhan bar stops it. Inside other apps no key is caught. Only that adhan ends
#   optimize/music.sh adhan saver-only on|off   «الأذان على شاشة التوقف فقط» (off by default): the adhan plays only while the screensaver shows or our home is in front;
#                                          another app or an input in front gets none (the alarm is still judged once, and never made up)
#   optimize/music.sh adhan notice on|off  «تنبيه عند الأذان» (off by default): a 5-second notice «حان وقت صلاة العصر» at the prayer's time, shown even when the adhan
#                                          is off or skipped (saver-only); the status says what the last alarm did: adhan_alarm_last=
#   optimize/music.sh adhan stop           stop the adhan that is playing (nothing else)
#   optimize/music.sh adhan volume 60      the adhan's own volume, 10-100 (percent of the TV's maximum)
#   optimize/music.sh adhan add FILE       the person's own recording as the adhan («أذان من ملفي»): made small here (mono AAC, about
#                                          48 kbps, at most 5 minutes; needs ffmpeg on this computer), put on the TV, and made the voice.
#                                          One file only: a new one replaces it. It stays on the TV; nothing bundled, nothing sent anywhere
#   optimize/music.sh adhan voice own      use it again (voice 1-21 switches back); optimize/music.sh adhan own-clear removes it
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${PY:-}" ]; then
  if [ -x optimize/venv/bin/python ]; then
    PY=optimize/venv/bin/python
  elif [ -x optimize/venv/Scripts/python.exe ]; then
    PY=optimize/venv/Scripts/python.exe
  else
    PY=python3
  fi
fi
tv() { "$PY" -I optimize/tv.py "$@"; }
FFMPEG="${FFMPEG:-ffmpeg}"
FFPROBE="${FFPROBE:-ffprobe}"
OWN_STAGE=/data/local/tmp/urtv-own
OWN_MAX_SECONDS=300
OWN_MAX_BYTES=$((3 << 20))
OWN_WORK=
own_send() {   # $1 = the extras after -a ...ADHAN_OWN or VOICE; prints the receiver's answer; exit 1 on "error:"
  local out
  out=$(tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a $1" | sed -n '/data="/,$p' | sed '1s/.*data="//; $s/"$//')
  echo "$out"
  [[ "$out" != error:* ]]
}
own_add() {   # $1 = the person's file: made small, uploaded, kept by the app, made the voice
  local f=$1 work secs bytes sent note=
  [ -f "$f" ] || { echo "error: $f is not a file" >&2; exit 1; }
  if ! command -v "$FFMPEG" > /dev/null 2>&1; then
    echo "error: ffmpeg is needed on this computer to make the recording small enough for the TV (mono AAC, about 48 kbps, at most 5 minutes)." >&2
    echo "Install it (macOS: brew install ffmpeg; Windows: winget install ffmpeg; Linux: sudo apt install ffmpeg), then run the same command again." >&2
    exit 1
  fi
  work=$(mktemp -d)
  OWN_WORK=$work
  trap '[ -z "$OWN_WORK" ] || rm -rf "$OWN_WORK"' EXIT
  local af=()
  if command -v "$FFPROBE" > /dev/null 2>&1; then   # a recording longer than 5 minutes is cut there, with a short fade so it does not stop dead
    secs=$("$FFPROBE" -v error -show_entries format=duration -of csv=p=0 "$f" 2> /dev/null | head -n 1 | cut -d. -f1 || true)
    if [[ "$secs" =~ ^[0-9]+$ ]] && [ "$secs" -gt "$OWN_MAX_SECONDS" ]; then
      af=(-af "afade=t=out:st=$((OWN_MAX_SECONDS - 4)):d=4")
      note="note: the recording is $((secs / 60)):$(printf %02d $((secs % 60))) long; the adhan is at most 5 minutes, so only the first 5:00 is kept."
    fi
  fi
  if ! "$FFMPEG" -nostdin -v error -y -i "$f" -map 0:a:0 -vn -sn -dn -map_metadata -1 -t "$OWN_MAX_SECONDS" -ac 1 ${af[@]+"${af[@]}"} \
      -c:a aac -b:a 48k -movflags +faststart "$work/own.m4a" 2> "$work/err.txt"; then
    echo "error: ffmpeg could not make an adhan of $f: $(tail -n 1 "$work/err.txt" 2> /dev/null)" >&2
    echo "(it must be an audio file, or a video with sound)" >&2
    exit 1
  fi
  [ -s "$work/own.m4a" ] || { echo "error: ffmpeg made nothing from $f (is there sound in it?)" >&2; exit 1; }
  bytes=$(wc -c < "$work/own.m4a" | tr -d ' ')
  [ "$bytes" -le "$OWN_MAX_BYTES" ] || { echo "error: the small copy is still $((bytes / 1024)) KB; the TV keeps at most $((OWN_MAX_BYTES >> 20)) MB" >&2; exit 1; }
  [ -z "$note" ] || echo "$note"
  echo "prepared $f: $((bytes / 1024)) KB"
  tv "mkdir -p $OWN_STAGE" > /dev/null
  tv --push-b64 "$work/own.m4a" "$OWN_STAGE/in.m4a" | tail -1
  # the shell streams it into the app (its folder is private); where this TV lacks `content write`, the app reads it itself
  tv "content write --uri content://sa.urtv.home.adhan/own-in < $OWN_STAGE/in.m4a" > /dev/null 2>&1 || true
  sent=$(own_send "sa.urtv.home.ADHAN_OWN --es op add --ei size $bytes" || true)
  case "$sent" in error:*)
    sent=$(own_send "sa.urtv.home.ADHAN_OWN --es op add --ei size $bytes --es path $OWN_STAGE/in.m4a" || true) ;;
  esac
  tv "rm -f $OWN_STAGE/in.m4a" > /dev/null || true
  tv "rmdir $OWN_STAGE" > /dev/null 2>&1 || true
  case "$sent" in saved*) ;; *) echo "${sent:-error: no answer from the TV (is the home screen app installed and up to date?)}" >&2; exit 1 ;; esac
  echo "added $f: it is the adhan now (it stays on this TV only)"
  echo "$sent" | tail -n +2
}

usage() {
  cat <<'EOF'
Usage: optimize/music.sh <command> [arguments]

  status                  current player state (playing, source, position, error)
  reciters                reciter ids for `surah`
  stations                station indices for `radio`
  surah <id> <1-114>      play a surah, e.g. surah sudais 1
  radio <index>           play a live station, e.g. radio 0
  pause | resume | toggle | next | prev | stop
  seek <seconds>          jump forward/back (e.g. seek -10)
  sleep <minutes>         sleep timer; 0 cancels
  adhan [now [voice|fajr]]   the adhan: its state, play it now, on/off, or the voice
  adhan on | off
  adhan voice <1-21 | own>   (not the Fajr recordings 10, 13, 17, 20)
  adhan fajr-voice <1-21 | own>
  adhan short on | off
  adhan saver-only on | off   the adhan plays only over the screensaver or our home (default off)
  adhan notice on | off   a 5-second notice with the prayer's name at its time (default off)
  adhan takbir on | off   only the opening takbir plays, then a fade (default off; turns short off)
  adhan stopkey on | off  any key stops the adhan while it plays (default on; off: only OK on the adhan bar)
  adhan stop              stop the adhan that is playing
  adhan volume <10-100>
  adhan add <file>        your own recording as the adhan (needs ffmpeg here); adhan own-clear removes it
EOF
  exit 2
}

command="${1:-}"
[ -n "$command" ] || usage
shift || true

case "$command" in
  status)
    [ $# -eq 0 ] || usage
    out=$(tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.GET_PLAYER")
    data=$(printf '%s\n' "$out" | sed -n 's/.*data="\(.*\)"/\1/p' | tail -n 1)
    if [ -z "$data" ]; then
      echo "No status from sa.urtv.home. Is the app installed and the TV reachable?" >&2
      exit 1
    fi
    printf '%s\n' "$data"
    ;;

  reciters|stations)
    [ $# -eq 0 ] || usage
    "$PY" - "$command" <<'PY'
import json, pathlib, sys
sys.stdout.reconfigure(encoding="utf-8")
which = sys.argv[1]
path = pathlib.Path("android/launcher/src/main/assets/quran") / (which + ".json")
for i, item in enumerate(json.loads(path.read_text(encoding="utf-8"))[which]):
    if which == "reciters":
        print(f'{i}\t{item["id"]}\t{item["name"]}')
    else:
        print(f'{i}\t{item["name"]}')
PY
    ;;

  surah)
    [ $# -eq 2 ] || usage
    reciter=$1
    surah=$2
    case "$reciter" in ''|*[!a-z]*) echo "Reciter id must be lowercase letters; run: optimize/music.sh reciters" >&2; exit 2;; esac
    case "$surah" in ''|*[!0-9]*) usage;; esac
    { [ "$surah" -ge 1 ] && [ "$surah" -le 114 ]; } || { echo "Surah must be 1-114." >&2; exit 2; }
    tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_SURAH --es reciter $reciter --ei surah $surah" > /dev/null
    echo "Playing surah $surah by $reciter."
    ;;

  radio)
    [ $# -eq 1 ] || usage
    station=$1
    case "$station" in ''|*[!0-9]*) usage;; esac
    tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_RADIO --ei station $station" > /dev/null
    echo "Playing station $station."
    ;;

  pause|resume|toggle|next|prev|stop)
    [ $# -eq 0 ] || usage
    action=$(printf '%s' "$command" | tr '[:lower:]' '[:upper:]')
    tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.$action" > /dev/null
    echo "$action sent."
    ;;

  seek)
    [ $# -eq 1 ] || usage
    seconds=$1
    case "$seconds" in ''|*[!0-9-]*) usage;; esac
    digits=${seconds#-}
    case "$digits" in ''|*[!0-9]*) usage;; esac
    ms=$((seconds * 1000))
    tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.SEEK_BY --ei delta $ms" > /dev/null
    echo "Seek ${seconds}s."
    ;;

  sleep)
    [ $# -eq 1 ] || usage
    minutes=$1
    case "$minutes" in ''|*[!0-9]*) usage;; esac
    tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.SLEEP --ei minutes $minutes" > /dev/null
    if [ "$minutes" -eq 0 ]; then echo "Sleep timer cancelled."; else echo "Stopping in $minutes min."; fi
    ;;

  adhan)
    what="${1:-state}"
    case "$what" in
      state)
        [ $# -le 1 ] || usage
        out=$(tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.GET_PLAYER")
        data=$(printf '%s\n' "$out" | sed -n 's/.*data="\(.*\)"/\1/p' | tail -n 1)
        [ -n "$data" ] || { echo "No status from sa.urtv.home. Is the app installed and the TV reachable?" >&2; exit 1; }
        printf '%s\n' "$data" | tr '|' '\n' | grep -E '^(adhan=|voice=|fajr_voice=|adhan_short=|adhan_takbir=|adhan_stopkey=|adhan_saver_only=|adhan_notice=|adhan_alarm_last=|adhan_live=|adhan_volume=|adhan_exact=|adhan_next|adhan_last|source=|playing=)'
        own=$(printf '%s\n' "$data" | tr '|' '\n' | sed -n 's/^own_bytes=//p')
        if [ "${own:-0}" -gt 0 ] 2> /dev/null; then
          echo "own=$((own / 1024)) KB (your own recording; adhan voice own uses it, adhan own-clear removes it)"
        else
          echo "own=none (adhan add FILE puts your own recording on the TV)"
        fi
        ;;
      add)
        [ $# -eq 2 ] || usage
        own_add "$2"
        ;;
      own-clear)
        [ $# -eq 1 ] || usage
        own_send "sa.urtv.home.ADHAN_OWN --es op clear" || exit 1
        echo "Your own recording is removed."
        ;;
      now)
        [ $# -le 2 ] || usage
        if [ $# -eq 2 ] && [ "$2" = fajr ]; then
          tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN --ez fajr true" > /dev/null
        elif [ $# -eq 2 ]; then
          voice=$2
          case "$voice" in ''|*[!0-9]*) usage;; esac
          tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN --ei voice $voice" > /dev/null
        else
          tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN" > /dev/null
        fi
        echo "Adhan playing."
        ;;
      on|off)
        [ $# -eq 1 ] || usage
        action=$(printf '%s' "$what" | tr '[:lower:]' '[:upper:]')
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_$action" > /dev/null
        echo "Automatic adhan $what."
        ;;
      voice)
        [ $# -eq 2 ] || usage
        voice=$2
        [ "$voice" != own ] || voice=0
        case "$voice" in ''|*[!0-9]*) usage;; esac
        { [ "$voice" -le 21 ] && { [ "$voice" -ge 1 ] || [ "$2" = own ]; }; } || { echo "The voice is 1-21, or own." >&2; exit 2; }
        case "$voice" in 10|13|17|20) echo "Voice $voice is a Fajr recording: it plays at Fajr only. Use: optimize/music.sh adhan fajr-voice $voice" >&2; exit 2;; esac
        own_send "sa.urtv.home.VOICE --ei voice $voice" > /dev/null || { echo "There is no recording of your own on the TV yet (optimize/music.sh adhan add FILE)." >&2; exit 1; }
        if [ "$voice" -eq 0 ]; then echo "Adhan voice: your own recording."; else echo "Adhan voice $voice."; fi
        ;;
      fajr-voice)
        [ $# -eq 2 ] || usage
        voice=$2
        [ "$voice" != own ] || voice=0
        case "$voice" in ''|*[!0-9]*) usage;; esac
        { [ "$voice" -le 21 ] && { [ "$voice" -ge 1 ] || [ "$2" = own ]; }; } || { echo "The voice is 1-21, or own." >&2; exit 2; }
        own_send "sa.urtv.home.FAJR_VOICE --ei voice $voice" > /dev/null || { echo "There is no recording of your own on the TV yet (optimize/music.sh adhan add FILE)." >&2; exit 1; }
        if [ "$voice" -eq 0 ]; then echo "Fajr voice: your own recording."; else echo "Fajr voice $voice."; fi
        ;;
      short)
        [ $# -eq 2 ] || usage
        case "$2" in on) flag=true;; off) flag=false;; *) usage;; esac
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_SHORT --ez on $flag" > /dev/null
        if [ "$flag" = true ]; then echo "Short adhan on: it stops after its first minute."; else echo "Short adhan off: the whole adhan plays."; fi
        ;;
      takbir)
        [ $# -eq 2 ] || usage
        case "$2" in on) flag=true;; off) flag=false;; *) usage;; esac
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_TAKBIR --ez on $flag" > /dev/null
        if [ "$flag" = true ]; then echo "Takbir only on: just the opening takbir plays, then a fade (short adhan is off)."; else echo "Takbir only off."; fi
        ;;
      stopkey)
        [ $# -eq 2 ] || usage
        case "$2" in on) flag=true;; off) flag=false;; *) usage;; esac
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_STOPKEY --ez on $flag" > /dev/null
        if [ "$flag" = true ]; then echo "Any key stops the adhan while it plays, on the home screen and the screensaver (the volume keys and Power are left alone)."; else echo "Only OK on the adhan bar stops the adhan."; fi
        ;;
      saver-only)
        [ $# -eq 2 ] || usage
        case "$2" in on) flag=true;; off) flag=false;; *) usage;; esac
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_SAVERONLY --ez on $flag" > /dev/null
        if [ "$flag" = true ]; then echo "Saver-only on: the adhan plays only over the screensaver or our home screen."; else echo "Saver-only off: the adhan plays whatever is in front."; fi
        ;;
      notice)
        [ $# -eq 2 ] || usage
        case "$2" in on) flag=true;; off) flag=false;; *) usage;; esac
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_NOTICE --ez on $flag" > /dev/null
        if [ "$flag" = true ]; then echo "Notice on: a 5-second «حان وقت صلاة …» at each prayer's time, even when the adhan is off or skipped."; else echo "Notice off."; fi
        ;;
      stop)
        [ $# -eq 1 ] || usage
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.STOP_ADHAN" > /dev/null
        echo "Adhan stopped (if one was playing)."
        ;;
      volume)
        [ $# -eq 2 ] || usage
        percent=$2
        case "$percent" in ''|*[!0-9]*) usage;; esac
        { [ "$percent" -ge 10 ] && [ "$percent" -le 100 ]; } || { echo "The adhan volume is 10-100." >&2; exit 2; }
        tv "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_VOLUME --ei percent $percent" > /dev/null
        echo "Adhan volume $percent%."
        ;;
      *)
        usage
        ;;
    esac
    ;;

  help|-h|--help)
    usage
    ;;
  *)
    usage
    ;;
esac
