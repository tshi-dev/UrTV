#!/usr/bin/env bash
# TCL firmware (TclAppBoot) keeps apps from starting at boot, and from binding their services while they are not running,
# unless they hold its auto-start app-op. Ours need it: the adhan alarms after a restart, the sleep service and BootReceiver.
# The idea and the op names are from CleanYourTV (github.com/Gathin23/cleanyourtv, MIT; their pull request «keep TCL's
# AUTO_START setting so key helpers and Sleep Timer survive reboot», #1143, and the install scripts of their add-ons): the op
# is AUTO_START on G08 builds and APP_AUTO_START on G10, other TVs do not know it, and without `appops write-settings` the
# grant does not reach the disk before the next restart. On the owner's TCL (Android 14, 30 Oct) TclAppBoot also checks a second op,
# ASSOC_START («callee_does't_have_OP_ASSOC_START_permission»): it blocked our receivers after an update (the adhan, A11yHeal) and
# the binding of our accessibility services until a restart. Both allowed + write-settings: they ran and bound at once (tester report).
#   optimize/autostart.sh           allow it for sa.urtv.home and sa.urtv.screensaver on a TCL, then say what the TV answers
#   optimize/autostart.sh check     only say it (the lines report.md gets): «Auto-start: …», or a !! line when a TCL refuses
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
tv() { "$PY" -I optimize/tv.py "$@"; }
PKGS=(sa.urtv.home sa.urtv.screensaver)

is_tcl() {
  local who
  who=$(tv "getprop ro.product.manufacturer; getprop ro.product.brand; pm list packages com.tcl." | tr -d '\r' | tr 'A-Z' 'a-z')
  grep -qE '^tcl$|^package:com\.tcl\.' <<< "$who"
}
installed() { tv "pm list packages $1" | tr -d '\r' | grep -qx "package:$1"; }
state() {   # what the TV says for this package: allow, ignore, deny, default… none when it says it has no such op, unknown when it said nothing
  local out got op silent=0
  for op in AUTO_START APP_AUTO_START; do
    out=$(tv "appops get $1 $op 2>&1" | tr -d '\r' || true)   # «Error: Unknown operation string» is written to stderr: it must be read
    got=$(sed -n "s/^$op: \([a-z_]*\).*/\1/p" <<< "$out" | head -1)
    [ -z "$got" ] || { echo "$got"; return; }
    # only an explicit «unknown operation» (or «no operations») is «this build has no such op»; an empty answer is «the TV did not say»
    if grep -qiE 'unknown operation|no operations' <<< "$out"; then :; else silent=1; fi
  done
  if [ "$silent" = 1 ]; then echo unknown; else echo none; fi
}
assoc() {   # TCL's second op (ASSOC_START): allow, ignore…, or none when this build has no such op
  local out got
  out=$(tv "appops get $1 ASSOC_START 2>&1" | tr -d '\r' || true)
  got=$(sed -n "s/^ASSOC_START: \([a-z_]*\).*/\1/p" <<< "$out" | head -1)
  echo "${got:-none}"
}

mode="${1:-allow}"
if ! is_tcl; then echo "Auto-start: not a TCL, nothing to allow"; exit 0; fi
if [ "$mode" = allow ]; then
  for p in "${PKGS[@]}"; do
    installed "$p" || continue
    tv "appops set $p AUTO_START allow 2> /dev/null; appops set $p APP_AUTO_START allow 2> /dev/null; cmd appops set $p AUTO_START allow 2> /dev/null; cmd appops set $p APP_AUTO_START allow 2> /dev/null; appops set $p ASSOC_START allow 2> /dev/null; cmd appops set $p ASSOC_START allow 2> /dev/null; appops write-settings 2> /dev/null; true" > /dev/null
  done
fi
line=; bad=; silent=
for p in "${PKGS[@]}"; do
  installed "$p" || continue
  st=$(state "$p"); as=$(assoc "$p")
  case "$as" in allow|none) ;; *) [ "$st" = unknown ] || st="$st/assoc-$as" ;; esac   # either op left at ignore blocks the app
  line="$line${line:+, }$p $st"
  case "$st" in allow|none) ;; */assoc-*) bad="$bad $p($st)" ;; unknown) silent="$silent $p" ;; *) bad="$bad $p($st)" ;; esac   # none: this TCL build has no such op
done
if [ -z "$line" ]; then echo "Auto-start (TCL): our apps are not installed"
elif [ -n "$silent" ] && [ -z "$bad" ]; then
  echo "!! Auto-start (TCL): the TV did not answer, so it is not known whether our apps may start at boot. Run optimize/autostart.sh again"
elif [ -n "$bad" ]; then
  echo "!! Auto-start (TCL): not allowed for$bad: after a restart the adhan alarms and the sleep service may not start. Run optimize/autostart.sh again"
else echo "Auto-start (TCL): $line"; fi
