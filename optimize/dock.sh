#!/usr/bin/env bash
# The home screen's favourites row, for agents and scripts: no remote, no long-press.
#   optimize/dock.sh show                  the current order, left to right
#   optimize/dock.sh apps                  every app that can go in it: package | name
#   optimize/dock.sh set PKG1 PKG2 ...     a new order, left to right (up to 10; fewer than 5 is topped up)
#   optimize/dock.sh reset                 back to the defaults in android/launcher/.../config.xml
#   optimize/dock.sh hidden                apps left off the home screen, and why: package | reason
#   optimize/dock.sh hide PKG ...          take these off the home screen (all its lists)
#   optimize/dock.sh show PKG ...          put these on it, even against the rules (the TV's own home doesn't list them)
#   optimize/dock.sh names                 every app's name: package | shown | its own | from (agent, own, table)
#   optimize/dock.sh name PKG 'الاسم' ...   give apps an Arabic name on this TV (pairs; '' = its usual name again)
#   optimize/dock.sh names-clear           every app back to its usual name
#   optimize/dock.sh input-names           the TV's inputs: id | shown | the TV's own name | ours | CEC device name | state (connected/standby/disconnected)
#   optimize/dock.sh input-name ID 'PS5'   name an input in the sources list on this TV (ID from input-names; '' = the TV's own name again)
#   optimize/dock.sh sleep 30              «مؤقت النوم» for the whole TV: 15 30 60 90, or any 1 to 600 minutes; then the TV goes to sleep
#   optimize/dock.sh sleep                 how the timer stands: minutes left, and what its end will do
#   optimize/dock.sh sleep cancel          stop it
#   optimize/dock.sh familydns on|off|status|grant   «حماية العائلة (DNS)»: Private DNS to AdGuard Family, checked and undone by optimize/familydns.sh
#   optimize/dock.sh sleep-setup           turn on the sleep service (an accessibility service, once) so the end turns the screen off
#   optimize/dock.sh type-setup            turn on the remote-and-typing service (a second accessibility service, once) so the phone remote can press keys and type in other apps
#   optimize/dock.sh pin set 1234         «الرقابة الأبوية»: the master PIN (4 digits), set or changed from here without the old one; never printed back
#   optimize/dock.sh pin lock PKG          lock one app (its package, e.g. com.netflix.ninja), a pill (@inputs @wifi @tvsettings @tasks @saver @listen @sleep @center) or @urtv (our settings); `pin unlock PKG` opens it again
#   optimize/dock.sh pin status            what is set: PIN or not, what is locked, kids mode, wrong tries (never the PIN)
#   optimize/dock.sh pin off               no PIN: nothing asks, kids mode ends; the apps chosen stay for the next PIN
#   optimize/dock.sh pin reset             the PIN was forgotten: everything parental back to nothing (the PIN, the locks, kids mode, the wrong tries)
# The home screen redraws at once; the sleep timer's pill too. Only ADB can do this (sa.urtv.home/.DockReceiver needs the shell's DUMP).
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
send() {   # $1 action, $2 extra arguments; prints the receiver's answer (the "data" of am broadcast); fails when there is none or it is an error
  local raw out
  raw=$("$PY" -I optimize/tv.py "am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.$1 ${2:-}" | tr -d '\r') || raw=""
  # no «data=» at all = the receiver never ran (app missing, another signature, just updated): that is not «done»
  if ! grep -q 'data=' <<< "$raw"; then
    echo "!! dock.sh: no answer from the TV's home app (it is not installed, was just updated, or the connection dropped), so nothing is known to have changed. Run optimize/install.sh launcher, then the same command again." >&2
    return 1
  fi
  out=$(sed -n '/data="/,$p' <<< "$raw" | sed '1s/.*data="//; $s/"$//')
  echo "$out"
  [[ "$out" != error:* ]]
}
case "${1:-}:$#" in
  familydns:*) shift; exec optimize/familydns.sh "$@" ;;
  show:1) send GET_DOCK ;;
  apps:*)  send LIST_APPS ;;
  reset:*) send RESET_DOCK ;;
  hidden:*) send HIDDEN ;;
  hide:*|show:*)
    what=$(tr '[:lower:]' '[:upper:]' <<< "$1")
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/dock.sh hide|show PKG ..."; exit 1; }
    for p in "$@"; do [[ "$p" =~ ^[A-Za-z0-9_.]+$ ]] || { echo "not a package name: $p"; exit 1; }; done
    send "$what" "--es apps $(IFS=,; echo "$*")" ;;
  set:*)
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/dock.sh set PKG1 PKG2 ..."; exit 1; }
    for p in "$@"; do [[ "$p" =~ ^[A-Za-z0-9_.]+$ ]] || { echo "not a package name: $p"; exit 1; }; done
    send SET_DOCK "--es apps $(IFS=,; echo "$*")" ;;
  sleep:1) send SLEEP_TV ;;
  sleep:2)
    case "$2" in
      cancel) minutes=0 ;;
      ""|*[!0-9]*) echo "usage: optimize/dock.sh sleep [MINUTES | cancel]"; exit 1 ;;
      *) minutes=$2; [ "$minutes" -le 600 ] || { echo "minutes: 1 to 600"; exit 1; } ;;
    esac
    send SLEEP_TV "--ei minutes $minutes" ;;
  sleep-setup:1|type-setup:1)
    # two accessibility services of ours, each turned on on its own (typing is a separate service: it can read the screen to find a text field, the sleep one cannot)
    if [ "$1" = type-setup ]; then svc="sa.urtv.home/sa.urtv.home.TypeService"; what="the typing service"; done_msg="the typing service is on (remote and typing): the phone remote can press the arrows, OK, Back and Home in the app in front and type into its text field"
    else svc="sa.urtv.home/sa.urtv.home.SleepService"; what="the sleep service"; done_msg="the sleep service is on: the end of «مؤقت النوم» turns the screen off"; fi
    cur=$("$PY" -I optimize/tv.py "settings get secure enabled_accessibility_services; settings get secure accessibility_enabled" | tr -d '\r')
    now=$(sed -n 1p <<< "$cur")
    # only the word «null» means no service is on; an empty answer is «the TV did not say», and the person's own list must not be overwritten
    if [ -z "$now" ] || [ -z "$(sed -n 2p <<< "$cur")" ]; then
      echo "!! enabled_accessibility_services: the TV did not say which accessibility services are on, so $what was NOT added and nothing was changed. Run the same command again."
      exit 1
    fi
    # what the TV had, once (optimize/reset.sh takes our entry out again and puts the switch back as it was)
    if [ ! -f "${URTV_RUN_DIR:-optimize/run}/accessibility-before.txt" ]; then
      mkdir -p "${URTV_RUN_DIR:-optimize/run}"
      printf 'accessibility_enabled=%s\n' "$(sed -n 2p <<< "$cur")" > "${URTV_RUN_DIR:-optimize/run}/accessibility-before.txt"
    fi
    # the list goes to the TV's shell in single quotes (a service like pkg/.Svc$Inner must not have its $Inner expanded)
    case "$now" in
      *"$svc"*) echo "$what was already on" ;;
      null|"") "$PY" -I optimize/tv.py "settings put secure enabled_accessibility_services '$svc'" >/dev/null ;;
      *) if [[ "$now" =~ ^[A-Za-z0-9_.\$/:]+$ ]]; then
           "$PY" -I optimize/tv.py "settings put secure enabled_accessibility_services '$now:$svc'" >/dev/null
         else echo "!! enabled_accessibility_services: the list on this TV has a character this script does not send, so $what was not added"; exit 1; fi ;;
    esac
    "$PY" -I optimize/tv.py "settings put secure accessibility_enabled 1" >/dev/null
    after=$("$PY" -I optimize/tv.py "settings get secure enabled_accessibility_services" | tail -n 1 | tr -d '\r')
    case "$after" in *"$svc"*) ;; *) echo "!! enabled_accessibility_services: this TV did not take $what (it says ${after:-nothing})" ;; esac
    echo "$done_msg" ;;
  pin:*)
    shift
    case "${1:-}:$#" in
      set:2) [[ "$2" =~ ^[0-9]{4}$ ]] || { echo "usage: optimize/dock.sh pin set <4 digits>"; exit 1; }
             send PIN_SET "--es pin $2" ;;
      lock:2|unlock:2) [[ "$2" =~ ^(@[a-z]+|[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+)$ ]] || { echo "usage: optimize/dock.sh pin lock|unlock <package, e.g. com.example.app, or a pill like @wifi, or @urtv>"; exit 1; }
             send "PIN_$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')" "--es target $2" ;;
      status:1) send PIN_STATUS ;;
      off:1) send PIN_OFF ;;
      reset:1) send PIN_RESET ;;
      *) echo "usage: optimize/dock.sh pin set <4 digits> | lock <target> | unlock <target> | status | off | reset"; exit 1 ;;
    esac ;;
  names:1) send NAMES ;;
  names-clear:1) send CLEAR_NAMES ;;
  name:*)
    shift
    [ $# -gt 0 ] && [ $(( $# % 2 )) -eq 0 ] || { echo "usage: optimize/dock.sh name PKG 'الاسم' [PKG 'الاسم' ...]"; exit 1; }
    pairs=""
    while [ $# -gt 0 ]; do
      [[ "$1" =~ ^[A-Za-z0-9_.]+$ ]] || { echo "not a package name: $1"; exit 1; }
      case "$2" in *"'"*|*";"*|*"|"*) echo "no ' ; or | in a name: $2"; exit 1 ;; esac
      pairs="$pairs${pairs:+;}$1|$2"
      shift 2
    done
    send SET_NAMES "--es names '$pairs'" ;;
  input-names:1) send INPUT_NAMES ;;
  input-name:3)
    # the id goes to the TV's shell in single quotes, so a $ in it (com.x/.Svc$Inner/HDMI1) is not expanded there
    id_ok='^[A-Za-z0-9._/%:\$-]+$'
    [[ "$2" =~ $id_ok ]] || { echo "not an input id (use one from: optimize/dock.sh input-names)"; exit 1; }
    bytes=$(printf '%s' "$3" | LC_ALL=C wc -c)
    plain=$(printf '%s' "$3" | LC_ALL=C tr -d '\000-\037\177' | LC_ALL=C wc -c)
    [ "$bytes" -eq "$plain" ] || { echo "a name is one line with no control characters"; exit 1; }   # (the names are the owner's: no error line repeats one)
    chars=$(printf '%s' "$3" | LC_ALL=C tr -d '\200-\277' | LC_ALL=C wc -c)   # letters, not bytes (an Arabic letter is two bytes)
    [ "$chars" -le 20 ] || { echo "a name is up to 20 characters"; exit 1; }
    esc=$(printf '%s' "$3" | sed "s/'/'\\\\''/g")   # an apostrophe inside the quotes, as optimize/type.sh does; the app decides whether the name is kept
    send SET_INPUT_NAME "--es id '$2' --es name '$esc'" ;;
  input-name:*) echo "usage: optimize/dock.sh input-name ID 'الاسم'   (ID from: optimize/dock.sh input-names; '' = the TV's own name)"; exit 1 ;;
  *) sed -n '2,25p' "$0"; exit 1 ;;
esac
