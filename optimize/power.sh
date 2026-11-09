#!/usr/bin/env bash
# What UrTV holds on the TV right now: Doze, wake locks, alarms and jobs. Read-only: it only asks, it changes nothing.
#   optimize/power.sh                 ask the TV (also: optimize/measure.sh power)
#   optimize/power.sh --from FILE     summarise a saved answer (the sections «== deviceidle», «== wakelocks», «== alarms», «== jobs»)
# For «the TV does not go to sleep» or «the remote wakes it slowly». It lists what is held at this moment, not power use: only a
# power meter measures watts. An empty section means the TV showed nothing for it (or no permission), not that nothing is held.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
RAW="${URTV_RUN_DIR:-optimize/run}/power-raw.txt"
PKG='sa\.urtv'
if [ "${1:-}" = --from ]; then
  RAW="${2:?usage: optimize/power.sh --from FILE}"
else
  mkdir -p "$(dirname "$RAW")"
  printf '%s\n' \
    "echo '== deviceidle'; dumpsys deviceidle 2> /dev/null | grep -E 'mDeepEnabled|mLightEnabled|mState=|mLightState=|sa\\.urtv'" \
    "echo '== wakelocks'; dumpsys power 2> /dev/null | grep -i wake" \
    "echo '== uids'; for p in sa.urtv.home sa.urtv.screensaver; do echo \"\$p \$(dumpsys package \$p 2> /dev/null | grep -m1 userId=)\"; done" \
    "echo '== wifilocks'; dumpsys wifi 2> /dev/null | grep -iE 'lock'" \
    "echo '== alarms'; dumpsys alarm 2> /dev/null | grep sa.urtv" \
    "echo '== jobs'; dumpsys jobscheduler 2> /dev/null | grep sa.urtv" \
    | "$PY" -I optimize/tv.py --batch | tr -d '\r' > "$RAW"
fi
section() { awk -v want="$1" '/^== /{s=($2==want); next} s' "$RAW"; }
count() { grep -c . <<< "$1" || true; }

idle=$(section deviceidle); locks=$(section wakelocks); alarms=$(section alarms); jobs=$(section jobs)
wifilocks=$(section wifilocks)
# the apps' user ids: a wake lock names a uid (and often a work source), not the package
uids=$(section uids | sed -n 's/.*userId=\([0-9][0-9]*\).*/\1/p' | sort -u)
mine() {   # lines that are UrTV's: the tag or the text names it, or its uid owns it
  local u pat="$PKG"
  for u in $uids; do pat="$pat|uid=$u([^0-9]|\$)|WorkSource\\{$u([^0-9]|\$)|ws=\\[?$u([^0-9]|\$)"; done
  grep -E "$pat" || true
}

echo "What UrTV holds right now (read-only; a moment's reading, not a power measurement):"
# Doze
deep=$(sed -n 's/.*mState=\([A-Z_]*\).*/\1/p' <<< "$idle" | head -n 1); light=$(sed -n 's/.*mLightState=\([A-Z_]*\).*/\1/p' <<< "$idle" | head -n 1)
if [ -n "$deep$light" ]; then echo "- Doze: deep ${deep:-?}, light ${light:-?}"; else echo "- Doze: the TV did not say"; fi
NS="not seen: the TV showed nothing for it (or no permission), which does not mean none"   # 4.3 S10: an empty section claims nothing
if [ -z "$idle" ]; then echo "- Battery optimisation: $NS"
elif grep -qE "$PKG" <<< "$idle"; then echo "- Battery optimisation: an UrTV app is on the Doze allowlist ($(grep -oE "$PKG[A-Za-z0-9_.]*" <<< "$idle" | sort -u | paste -sd' ' -))"
else echo "- Battery optimisation: no UrTV app is on the Doze allowlist"; fi
# wake locks: the total the TV reports, and the ones that name us
total=$(sed -n 's/.*Wake Locks: size=\([0-9]*\).*/\1/p' <<< "$locks" | head -n 1)
held=$(grep -E "WAKE_LOCK|WakeLock" <<< "$locks" | mine || true)
ours=$(grep -c . <<< "$held" || true)
echo "- Wake locks: ${total:-?} held by anything, $ours owned by UrTV (by name or by its user id)"
grep . <<< "$held" | sed 's/^ */    /' | head -n 5 || true
wl=$(mine <<< "$wifilocks")
if [ -z "$wifilocks" ]; then echo "- Wi-Fi locks: $NS"
elif [ -n "$wl" ]; then echo "- Wi-Fi locks: UrTV holds one ($(grep -c . <<< "$wl") line(s))"; grep . <<< "$wl" | sed 's/^ */    /' | head -n 3
else echo "- Wi-Fi locks: none owned by UrTV"; fi
# alarms
na=$(grep -cE "$PKG" <<< "$alarms" || true)
if [ "$na" -gt 0 ]; then
  wake=$(grep -cE "RTC_WAKEUP|ELAPSED_WAKEUP|type 0|type 2" <<< "$alarms" || true)
  echo "- Alarms: $na line(s) naming UrTV ($wake of them wake the TV up)"
  sed 's/^ */    /' <<< "$alarms" | head -n 6
else echo "- Alarms: none seen naming UrTV (the TV lists only those; not shown and none look the same)"; fi
# jobs
nj=$(grep -cE "$PKG" <<< "$jobs" || true)
if [ "$nj" -gt 0 ]; then
  echo "- Jobs: $nj line(s) naming UrTV ($(grep -oE "$PKG[A-Za-z0-9_.]*/[.A-Za-z0-9_]*" <<< "$jobs" | sort -u | paste -sd' ' -))"
else echo "- Jobs: none seen naming UrTV (the TV lists only those; not shown and none look the same)"; fi
echo "- The 6-hour update job and the adhan alarm are expected (docs/POWER.md says why each exists)."
