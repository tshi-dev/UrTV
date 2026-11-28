#!/usr/bin/env bash
# «حماية العائلة (DNS)»: Android's Private DNS pointed at AdGuard Family. Off by default; nothing here runs unless it is asked.
#   optimize/dock.sh familydns on       ask the home app to switch it on (it checks the service can be reached, records what the TV had,
#                                       sets it, checks the internet works through it, and puts the old values back if not)
#   optimize/dock.sh familydns off      the app puts back exactly what the TV had (a value that was not set is deleted)
#   optimize/dock.sh familydns status   on or off, who switched it
#   optimize/dock.sh familydns grant    give the home app the permission it needs (recorded; reset.sh takes it back)
# The home app (sa.urtv.home, a version with this feature) is required: the whole job runs on the TV, so a dropped connection or a closed
# window here cannot leave the TV half switched, and there is one record, the app's. (A background job started from here would not
# survive on every TV: some end it with the ADB session, so there is no script-side fallback.) Run optimize/install.sh launcher first.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
HOST=family.adguard-dns.com   # AdGuard DNS «Family protection» (DNS over TLS); the app probes, sets and tests this host
PORT=853
RUN="${URTV_RUN_DIR:-optimize/run}"
APP=sa.urtv.home
tv() { "$PY" -I optimize/tv.py "$@"; }
get() {   # setting -> its value; «null» only when the TV itself says so, «unknown» when it could not be read
  local v; v=$(tv "settings get global $1" 2> /dev/null | tr -d '\r' | tail -n 1) || v=""; echo "${v:-unknown}"
}
sdk() { tv "getprop ro.build.version.sdk" 2> /dev/null | tr -d '\r' | tail -n 1 | tr -dc '0-9'; }
pkg_state() {   # yes | no | unknown: «no» only when the package list was read to its end marker and holds android itself
  local raw; raw=$(tv "pm list packages; echo END_OF_LIST" 2> /dev/null | tr -d '\r') || { echo unknown; return; }
  if [ "$(tail -n 1 <<< "$raw")" != END_OF_LIST ] || ! grep -qx "package:android" <<< "$raw"; then echo unknown
  elif grep -qx "package:$APP" <<< "$raw"; then echo yes; else echo no; fi
}
app_here() { [ "$(pkg_state)" = yes ]; }
app() {   # mode -> the app's answer text, empty when it did not answer. An app without the feature answers «error: unknown action …».
  tv "am broadcast -n $APP/.DockReceiver -a $APP.FAMILY_DNS --es mode $1" 2> /dev/null | tr -d '\r' | sed -n 's/.*data="\(.*\)"$/\1/p' | sed -n 1p || true
}
feature_state() {   # yes | no | unknown: «no» only for the app's own «error: unknown action» (every launcher released before this feature)
  local a; a=$(app status)
  case "$a" in "family-dns: "*) echo yes ;; "error: unknown action"*) echo no ;; *) echo unknown ;; esac
}
app_idle() {   # wait (up to 90 s; the app's own check takes at most about 45 s: 5 s probe, 40 s check) for a running check to end; the last status line
  local i s=""
  for i in $(seq 45); do
    s=$(app status)
    case "$s" in *"; working"*) sleep 2 ;; *) break ;; esac
  done
  echo "$s"
}
could_not_ask() { echo "!! familydns: the TV did not answer whether the home app with «حماية العائلة» is on it, so nothing was changed. Run the same command again."; }
need_app() {   # on: the home app with this feature is required
  local p f; p=$(pkg_state)
  [ "$p" != unknown ] || { could_not_ask; exit 1; }
  f=no; [ "$p" = no ] || f=$(feature_state)
  [ "$f" != unknown ] || { could_not_ask; exit 1; }
  if [ "$p" = no ] || [ "$f" = no ]; then
    echo "!! familydns: حماية العائلة تحتاج تطبيق الواجهة: optimize/install.sh launcher (the home app with this feature is not on this TV, so nothing was changed)"
    exit 1
  fi
}
explain() {   # a result name of the app -> a !! line
  case "$1" in
    ENABLED) ;;
    NO_ROUTE) echo "!! familydns: this TV cannot reach $HOST on port $PORT (DNS over TLS), so nothing was changed. The network may block it" ;;
    REVERTED) echo "!! familydns: the internet did not work through $HOST (Private DNS did not validate, or a fresh name or the internet did not answer within 40 s: up to 12 s for Private DNS, 3 tries of 6 s for a fresh name, 10 s for the internet), so the old Private DNS setting was put back" ;;
    RESTORE_FAILED) echo "!! familydns: the old Private DNS setting could NOT be put back (the TV refused it). It is recorded and will be tried again at the next start of the home app; run optimize/dock.sh familydns off to try now, and check optimize/dock.sh familydns status" ;;
    NOT_TAKEN) echo "!! familydns: this TV did not take the Private DNS setting, so the old one was put back" ;;
    NEEDS_GRANT) echo "!! familydns: the home app has no permission to change it: run optimize/dock.sh familydns grant" ;;
    UNSUPPORTED) echo "!! familydns: this TV's Android is older than 9, which has no Private DNS; nothing was changed" ;;
    UNREADABLE_VALUE) echo "!! familydns: could not read this TV's Private DNS setting, so nothing was changed" ;;
    NOT_RECORDED) echo "!! familydns: the old setting could not be recorded, so nothing was changed" ;;
    ALREADY_ON_BY_OTHER) echo "!! familydns: the Private DNS is already pointed at $HOST by someone else; left as it is (switch it off where it was switched on)" ;;
    *) echo "!! familydns: $1" ;;
  esac
}
grant() {
  mkdir -p "$RUN"
  tv "pm grant $APP android.permission.WRITE_SECURE_SETTINGS" > /dev/null 2>&1 \
    || { echo "!! familydns: this TV did not allow the permission (some operator builds refuse it), so the on/off cannot work here"; return 1; }
  grep -qx 'android.permission.WRITE_SECURE_SETTINGS' "$RUN/grants-journal.txt" 2> /dev/null || echo 'android.permission.WRITE_SECURE_SETTINGS' >> "$RUN/grants-journal.txt"
}

case "${1:-status}" in
  grant)
    need_app
    grant && echo "The home app may now switch the Private DNS from the TV's settings (حماية العائلة). optimize/reset.sh takes the permission back." ;;
  status)
    mode=$(get private_dns_mode); spec=$(get private_dns_specifier)
    if [ "$mode" = hostname ] && [ "$spec" = "$HOST" ]; then echo "حماية العائلة (DNS): on ($HOST)"
    else echo "حماية العائلة (DNS): off (Private DNS: $mode$([ "$spec" = null ] || echo ", $spec"))"; fi
    if app_here; then s=$(app status); case "$s" in "family-dns: "*) echo "the home app says: $s" ;; esac; fi ;;
  on)
    sd=$(sdk) || sd=""
    if [ -n "$sd" ] && [ "$sd" -lt 28 ]; then explain UNSUPPORTED; exit 1; fi
    need_app
    s=$(app_idle)
    case "$s" in *"grant=no"*) grant || exit 1 ;; esac
    a=$(app on)
    case "$a" in
      *started*) ;;
      *) r=${a#family-dns: }; explain "${r:-NO_ANSWER}"; exit 1 ;;
    esac
    s=$(app_idle)
    r=$(sed -n 's/.*last=\([A-Z_]*\).*/\1/p' <<< "$s")
    if [ "$r" = ENABLED ] && grep -q "family-dns: on (us)" <<< "$s"; then
      echo "حماية العائلة (DNS) is on ($HOST); the internet works through it (switched by the home app, which holds the record)."
    else explain "${r:-NO_ANSWER}"; exit 1; fi ;;
  off)
    p=$(pkg_state)
    [ "$p" != unknown ] || { could_not_ask; exit 1; }
    f=no; [ "$p" = no ] || f=$(feature_state)
    [ "$f" != unknown ] || { could_not_ask; exit 1; }
    if [ "$f" = no ]; then
      # an app without the feature has no record, so there is nothing to put back (every launcher released before this feature)
      echo "Nothing to put back: no home app with «حماية العائلة» is on this TV."
      if [ "$(get private_dns_specifier)" = "$HOST" ]; then echo "(The TV's Private DNS points at $HOST, but not through this app: if the person did not set it, turn it off in the TV's network settings.)"; fi
      exit 0
    fi
    app_idle > /dev/null
    a=$(app off); r=${a#family-dns: }
    case "$r" in
      RESTORED) echo "حماية العائلة (DNS) is off: the TV's Private DNS is as it was (put back by the home app)." ;;
      NOTHING_TO_RESTORE) echo "حماية العائلة (DNS) is off: nothing was switched on by the home app." ;;
      CHANGED_BY_SOMEONE_ELSE) echo "The Private DNS was changed by someone else since it was switched on: left as it is." ;;
      *) echo "!! familydns: the home app did not put the old Private DNS back (${r:-no answer}): run optimize/dock.sh familydns off again"; exit 1 ;;
    esac ;;
  *) echo "usage: optimize/dock.sh familydns on|off|status|grant"; exit 1 ;;
esac
