#!/usr/bin/env bash
# Disable every package in a debloat list (one pm call per shell command; chained pm calls can stop early).
# Packages are disabled for user 0, never uninstalled. Undo with optimize/restore.sh <same list>.
# Refuses anything in optimize/lists/keep.txt, Android's own core (optimize/lists/core.txt) and the TV's inputs and keyboards
# (optimize/lists/inputs.txt), with no override. And asks the TV itself, in one round trip, which of the packages drive its HDMI, AV or tuner
# inputs or are a keyboard (TV_INPUT_HARDWARE asked for, a TvInputService or an input method served: Clean Your TV's input-driver guard,
# github.com/Gathin23/cleanyourtv, MIT): those stay on, and so does one whose answer cannot be read (fail closed).
# Usage: optimize/disable.sh optimize/lists/tcl-safe.txt
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: optimize/disable.sh <debloat list>}"
PY="${PY:-optimize/venv/bin/python}"
KEEP=optimize/lists/keep.txt
pkgs=$(grep -vE '^[[:space:]]*(#|$)' "$LIST" | awk '{print $1}')
blocked=$(printf '%s\n' "$pkgs" | grep -Fxf <(grep -vE '^[[:space:]]*(#|$)' "$KEEP" | awk '{print $1}') || true)
if [ -n "$blocked" ]; then
  echo "Refusing: these are in $KEEP (features people use, or the TV needs them):"
  printf '%s\n' "$blocked" | sed 's/^/  /'
  exit 1
fi
core=$(printf '%s\n' "$pkgs" | grep -Ef <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/core.txt | sed 's/.*/^(&)$/') || true)
if [ -n "$core" ]; then
  echo "Refusing: these are Android's own core, and a TV may not start again without them (optimize/lists/core.txt):"
  printf '%s\n' "$core" | sed 's/^/  /'
  exit 1
fi
inputs=$(printf '%s\n' "$pkgs" | grep -Ef <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/inputs.txt | awk '{print $1}' | sed 's/.*/^(&)$/') || true)
if [ -n "$inputs" ]; then
  echo "Refusing: these are the TV's inputs (HDMI, AV, sources, the tuner) or a keyboard; disabled, a port can stay black for good (optimize/lists/inputs.txt):"
  printf '%s\n' "$inputs" | sed 's/^/  /'
  exit 1
fi
# what the person asked to keep on (optimize/keep.sh restore) stays on, also on a later `run.sh again`
keptpkgs=$("$PY" -I optimize/offlist.py kept) || exit 1   # an unreadable kept.txt is not «nothing kept»: it stops here, with its own !! line
if [ -n "$keptpkgs" ]; then
  skipped=$(printf '%s\n' "$pkgs" | grep -Fxf <(printf '%s\n' "$keptpkgs") || true)
  [ -z "$skipped" ] || echo "Left on, as the person asked: $(paste -sd' ' - <<< "$skipped")" >&2
  pkgs=$(printf '%s\n' "$pkgs" | grep -Fxvf <(printf '%s\n' "$keptpkgs") || true)
fi
[ -n "$pkgs" ] || exit 0
# Which packages are ours to put back: installed on the TV, not already off before this run (a maker's own «disabled» package must not be turned
# ON by a restore, and a package that is not on the TV, or was removed for the user, is not ours at all). Each list is read with an end marker:
# an empty answer is «the TV did not say», not «none».
tvlist() {   # command -> package names, one per line; fails when the end marker is missing
  local raw; raw=$("$PY" -I optimize/tv.py "$1; echo END_OF_LIST" 2> /dev/null | tr -d '\r') || return 1
  [ "$(tail -n 1 <<< "$raw")" = END_OF_LIST ] || return 1
  sed -n 's/^package://p' <<< "$raw"
}
RUN="${URTV_RUN_DIR:-optimize/run}"
mkdir -p "$RUN"
LEDGER="$RUN/disabled-by-us.txt"
known=yes
installed=$(tvlist "pm list packages") || known=no
was_off=$(tvlist "pm list packages -d") || known=no
intended=""
if [ "$known" = yes ] && grep -qx android <<< "$installed"; then
  intended=$(printf '%s\n' "$pkgs" | awk 'NR==FNR { s[$0] = 1; next } ($0 in s)' <(printf '%s\n' "$installed") - \
    | awk 'NR==FNR { if ($0 != "") s[$0] = 1; next } !($0 in s)' <(printf '%s\n' "$was_off") -)   # (awk, not grep -f: an empty pattern file matches everything on some greps)
else
  # without these two lists a restore could not be exact (a package the TV itself had off would be turned ON): nothing is turned off, and it can be run again
  echo "!! disable.sh: the TV did not say which packages it has or which were already off, so NOTHING was turned off (a restore could not be exact). Run the same command again." >&2
  exit 1
fi
# The TV's own answer: a package that asks for TV_INPUT_HARDWARE (its own «requested permissions» block, not a shared user's), or serves
# android.media.tv.TvInputService or an input method, stays on whatever its name. From Android 6 (SDK 23) a readable dump with no permission block
# asks for nothing, so it passes; an older one, or a dump that cannot be read, stays on (NOCHK). «hung system process» is Android's watchdog
# switching every dump off: nothing is turned off then, and a restart clears it. The same command is sent by the setup on the TV
# (InputGuard.java; tests/input_guard_test.py keeps them equal).
GUARD='s=$(getprop ro.build.version.sdk); for p in @PKGS@; do d=$(dumpsys package $p 2>/dev/null); case "$d" in *"hung system process"*) echo "HUNG $p";; *"requested permissions:"*) printf "%s\n" "$d" | sed -n -E "/requested permissions:/,/(install|runtime) permissions:|User [0-9]+:/p" | grep -q TV_INPUT_HARDWARE && echo "INPUT $p";; *"grantedPermissions:"*) printf "%s\n" "$d" | sed -n -E "/grantedPermissions:/,/(install|runtime) permissions:|User [0-9]+:/p" | grep -q TV_INPUT_HARDWARE && echo "INPUT $p";; *"Package [$p]"*) [ "${s:-0}" -ge 23 ] 2>/dev/null || echo "NOCHK $p";; *"Unable to find"*) ;; *) echo "NOCHK $p";; esac; printf "%s\n" "$d" | grep -qE "android\.media\.tv\.TvInputService|android\.view\.InputMethod" && echo "INPUT $p"; done; echo END_OF_LIST'
rm -f "$RUN/inputs-left.txt"   # what the guard left on in this run (community.sh does not count them as applied)
if [ -n "$intended" ]; then
  raw=$("$PY" -I optimize/tv.py "${GUARD/@PKGS@/$(paste -sd' ' - <<< "$intended")}" 2> /dev/null | tr -d '\r') || raw=""
  if ! grep -qx END_OF_LIST <<< "$raw"; then
    echo "!! disable.sh: the TV did not say which of these packages drive its HDMI inputs or keyboard, so NOTHING was turned off. Run the same command again." >&2
    exit 1
  fi
  if grep -q '^HUNG ' <<< "$raw"; then
    echo "!! disable.sh: the TV answers «Service dumps disabled due to hung system process», so it cannot say which packages drive its HDMI inputs; NOTHING was turned off. Restart the TV (optimize/venv/bin/python -I optimize/tv.py --reboot), then run the same command again." >&2
    exit 1
  fi
  held=$(sed -n -E 's/^INPUT ([A-Za-z0-9_.]+)$/\1/p' <<< "$raw" | sort -u)
  unread=$(sed -n -E 's/^NOCHK ([A-Za-z0-9_.]+)$/\1/p' <<< "$raw" | sort -u)
  [ -z "$held" ] || echo "Left on, the TV says they drive its HDMI/AV/tuner inputs or are a keyboard: $(paste -sd' ' - <<< "$held")" >&2
  [ -z "$unread" ] || echo "Left on, the TV's answer about them cannot be read (an input driver must not be guessed): $(paste -sd' ' - <<< "$unread")" >&2
  if [ -n "$held$unread" ]; then
    out=$(printf '%s\n%s\n' "$held" "$unread")
    grep -E '^[A-Za-z0-9_.]+$' <<< "$out" | sort -u > "$RUN/inputs-left.txt" || true
    pkgs=$(printf '%s\n' "$pkgs" | awk 'NR==FNR { if ($0 != "") s[$0] = 1; next } !($0 in s)' <(printf '%s\n' "$out") -)
    intended=$(printf '%s\n' "$intended" | awk 'NR==FNR { if ($0 != "") s[$0] = 1; next } !($0 in s)' <(printf '%s\n' "$out") -)
    [ -n "$pkgs" ] || exit 0
  fi
fi
prev=""; [ ! -f "$LEDGER" ] || prev=$(cat "$LEDGER")
remember() { { printf '%s\n' "$prev"; printf '%s\n' "$1"; } | { grep -E '^[A-Za-z0-9_.]+$' || true; } | sort -u > "$LEDGER.tmp" && mv "$LEDGER.tmp" "$LEDGER"; }
remember "$intended"   # written BEFORE the batch: a batch that dies half way is still undone by the restore
printf '%s\n' "$pkgs" | awk '{print "pm disable-user --user 0 " $1}' | "$PY" -I optimize/tv.py --batch
# confirmed by what the TV says is off now (read from the package manager, which is up to date at once)
if now_off=$(tvlist "pm list packages -d"); then
  remember "$(printf '%s\n' "$intended" | awk 'NR==FNR { s[$0] = 1; next } ($0 in s)' <(printf '%s\n' "$now_off") -)"
fi
optimize/settle.sh mark 2> /dev/null || true
