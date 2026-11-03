#!/usr/bin/env bash
# Speed and memory tweaks that are safe and reversible. Nothing is uninstalled or disabled here.
#   optimize/tweaks.sh apply     - animations at half length
#                                - Play Store kept from running in the background (it stays installed and opens
#                                  normally; on one set its idle CPU fell 31% -> 6%)
#                                - app caches trimmed, only when the free storage is low (under 1 GB or 10%): the
#                                  caches (thumbnails, app data to load) are useful while there is room
#                                - our two apps compiled ahead of time from their profile (cmd package compile -m speed-profile,
#                                  Android 7 and newer): a sideloaded app runs on the JIT until Android's idle job compiles it
#   optimize/tweaks.sh restore   back to the values the TV had before (the journal optimize/run/tweaks-journal.txt, written
#                                by the first apply: a value that was not set is deleted, not written); a TV with no journal
#                                gets Android's defaults
#   optimize/tweaks.sh status    what each setting was and is now (the report shows it beside the measurements)
# Every value is read back after it is set: a TV that does not take it gets a «!!» line (the setup goes on).
# Not by default until it is measured again: CACHED_LIMIT=16 optimize/tweaks.sh apply also keeps at most 16 closed
# apps in memory (only lowered, never raised). It shows more free RAM, but Android 14 raised this limit on purpose
# (up to 30% fewer cold app starts, source.android.com/docs/core/perf/cached-apps-freezer), and frozen cached apps
# use no CPU anyway. Seen on a TCL Google TV (Android 14): 1024 at boot; the device_config value only takes effect
# after a restart and did not survive one there (null again), so `dumpsys activity settings` shows the boot value
# and `device_config get activity_manager max_cached_processes` the setting.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
CACHED="${CACHED_LIMIT:-}"
JOURNAL="${URTV_RUN_DIR:-optimize/run}/tweaks-journal.txt"
KEYS="window_animation_scale transition_animation_scale animator_duration_scale cached_apps_freezer max_cached_processes"
# cached_apps_freezer is no longer set (one report linked it to apps hanging after standby; Android 14 has it on anyway, on 11-13 it is experimental):
# apply leaves it at the TV's own value and does not record it. It stays in KEYS for restore and status, so a TV an older run set to «enabled»
# (its journal has the line) gets its original back, by restore and by the next apply.
# What this Android has: `device_config` is Android 10 (SDK 29) and newer, `am set-standby-bucket` is Android 9 (SDK 28) and newer, and the «restricted» bucket
# is Android 11 (SDK 30) and newer (Android 9 and 10 take «rare»). An SDK the TV did not say counts as «too old to be sure»: those two settings are left alone.
SDK=$("$PY" -I optimize/tv.py "getprop ro.build.version.sdk" 2> /dev/null | tr -dc '0-9') || SDK=""
SDK_SAY=${SDK:-unknown}; SDK=${SDK:-0}
SKIPPED=()   # "key: why", written to the journal so the record says what was not touched and why
if [ "$SDK" -lt 29 ]; then KEYS=${KEYS/ max_cached_processes/}; SKIPPED+=("max_cached_processes: device_config needs Android 10 (this TV is SDK $SDK_SAY)"); fi
OURS="sa.urtv.home sa.urtv.screensaver"   # compiled ahead of time by apply (journaled as compiled_<package>), back to «as installed» by restore
BUCKET=restricted; [ "$SDK" -ge 30 ] || BUCKET=rare
AKEYS=" $KEYS "; AKEYS=${AKEYS/ cached_apps_freezer / }; AKEYS=$(echo $AKEYS)   # what apply records and sets (after the SDK trimming above)
getcmd() { case "$1" in max_cached_processes) echo "device_config get activity_manager $1" ;; *) echo "settings get global $1" ;; esac; }
putcmd() {   # key value: a value that was not set (null) is deleted
  case "$1" in
    max_cached_processes) if [ "$2" = null ]; then echo "device_config delete activity_manager $1"; else echo "device_config put activity_manager $1 $2"; fi ;;
    *) if [ "$2" = null ]; then echo "settings delete global $1"; else echo "settings put global $1 $2"; fi ;;
  esac
}
# Only the word «null» that the TV itself prints means «not set». A read that failed or came back empty is «unknown»: it is never
# recorded as null (restore would delete the TV's real value) and a setting that cannot be read is not changed.
tvget() { local v; v=$("$PY" -I optimize/tv.py "$(getcmd "$1")" 2> /dev/null | tr -d '\r' | tail -n 1) || v=""; echo "${v:-unknown}"; }
JOURNAL_DONE="${JOURNAL%.txt}.done"   # a restored journal is kept under this name: a second restore must not write defaults over it
jget() { sed -n "s/^$1=//p" "$JOURNAL" 2> /dev/null | head -n 1; }
jset() {   # key value: the key's line in the journal (added if it is not there)
  if grep -q "^$1=" "$JOURNAL"; then sed "s/^$1=.*/$1=$2/" "$JOURNAL" > "$JOURNAL.tmp" && mv "$JOURNAL.tmp" "$JOURNAL"
  else echo "$1=$2" >> "$JOURNAL"; fi
}
original() {   # key tv|ps: what the journal has; an «unknown» one is read again (the TV's value was never touched, so it is still the original)
  local o; o=$(jget "$1")
  if [ -z "$o" ] || [ "$o" = unknown ]; then
    if [ "$2" = ps ]; then o=$(psget "$1"); else o=$(tvget "$1"); fi
    [ "$o" = unknown ] || jset "$1" "$o"
  fi
  echo "${o:-unknown}"
}
# The Play Store's standby bucket and background appops are recorded and restored the same way (names: the bucket by name, the
# appops by mode; «default» when the TV has none set). A firmware that does not say gets «unknown» and today's values on restore.
PS=com.android.vending
PSKEYS="standby_bucket appop_RUN_IN_BACKGROUND appop_RUN_ANY_IN_BACKGROUND"
if [ "$SDK" -lt 28 ]; then PSKEYS=${PSKEYS/standby_bucket /}; SKIPPED+=("standby_bucket: standby buckets need Android 9 (this TV is SDK $SDK_SAY)"); fi
# Android 9 and 10 (SDK < 30) are not asked for a bucket at all: 6 of 7 Android 9 reports still warned because the Play Store went back to «active» (the
# «restricted» bucket is Android 11's; «rare» did not hold). Only apply skips it; restore still puts back a bucket an older run recorded.
if [ "${1:-}" = apply ] && [ "$SDK" -lt 30 ] && [ "$SDK" -ge 28 ]; then PSKEYS=${PSKEYS/standby_bucket /}; SKIPPED+=("standby_bucket: the Play Store's standby bucket is only set on Android 11 and newer (this TV is SDK $SDK_SAY)"); fi
if [ "$SDK" -lt 24 ]; then SKIPPED+=("compile: cmd package compile needs Android 7 (this TV is SDK $SDK_SAY)"); fi
has_key() { case " $KEYS $PSKEYS " in *" $1 "*) return 0 ;; esac; return 1; }
bucket_name() { case "$1" in 5) echo exempted ;; 10) echo active ;; 20) echo working_set ;; 30) echo frequent ;; 40) echo rare ;; 45) echo restricted ;; 50) echo never ;; *) echo unknown ;; esac; }
psget() {
  local o
  case "$1" in
    standby_bucket) o=$("$PY" -I optimize/tv.py "am get-standby-bucket $PS" 2> /dev/null | tr -d '\r' | tail -n 1 | tr -dc '0-9') || o=""
                    bucket_name "$o" ;;
    appop_*) o=$("$PY" -I optimize/tv.py "appops get $PS ${1#appop_}" 2> /dev/null | tr -d '\r' | head -n 1) || o=""
             case "$o" in
               "No operations"*) echo default ;;
               "${1#appop_}: "*) o=${o#*: }; o=${o%%[;, ]*}; [[ "$o" =~ ^[a-z_]+$ ]] && echo "$o" || echo unknown ;;
               *) echo unknown ;;
             esac ;;
  esac
}
test_build() { "$PY" -I optimize/tv.py "getprop ro.build.type" 2> /dev/null | tr -d '\r' | grep -Eqx 'userdebug|eng'; }   # test-keys alone is common on shipped TVs
psput() {   # key value
  case "$1" in
    standby_bucket) echo "am set-standby-bucket $PS $2" ;;
    appop_*) echo "cmd appops set $PS ${1#appop_} $2" ;;
  esac
}
verify() {   # key wanted: read back; a «!!» line when the TV did not take it
  local got; got=$(tvget "$1")
  if [ "$got" != "$2" ]; then echo "!! $1: this TV did not take the value (asked for $2, it says $got)"; return 1; fi
}
case "${1:?usage: optimize/tweaks.sh apply|restore|status}" in
  apply)   v=0.5; bucket=restricted; bg=deny ;;
  restore) v=1.0; bucket=active;     bg=allow ;;   # (what a TV with no record gets; with one, its own values)
  status)
    if [ ! -f "$JOURNAL" ]; then
      if [ -f "$JOURNAL_DONE" ]; then echo "Tweaks: already put back (optimize/run/tweaks-journal.done)"
      else echo "Tweaks: none recorded (nothing was changed by optimize/tweaks.sh apply)"; fi
      exit 0
    fi
    for k in $KEYS; do [ "$k" != cached_apps_freezer ] || [ -n "$(jget "$k")" ] || continue; echo "- tweak $k: was $(jget "$k"), now $(tvget "$k")"; done
    for k in $PSKEYS; do echo "- tweak $k: was $(jget "$k"), now $(psget "$k")"; done
    for p in $OURS; do [ -z "$(jget "compiled_$p")" ] || echo "- tweak compiled_$p: $(jget "compiled_$p") (undo: cmd package compile --reset)"; done
    exit 0 ;;
  *) echo "usage: optimize/tweaks.sh apply|restore|status"; exit 1 ;;
esac
cmds=()
want=()   # key=value pairs to read back
pswant=() # the same for the Play Store's bucket and appops
if [ "$1" = apply ]; then
  # the first apply records what the TV had (a later one, as after run.sh again, keeps that record: the TV is not "as it came" then)
  if [ ! -f "$JOURNAL" ]; then
    mkdir -p "$(dirname "$JOURNAL")"; rm -f "$JOURNAL_DONE"
    { echo "# what the TV had before optimize/tweaks.sh apply (null: it was not set; unknown: it could not be read, so it was not changed); optimize/tweaks.sh restore puts exactly this back"
      for k in $AKEYS; do echo "$k=$(tvget "$k")"; done
      for k in $PSKEYS; do echo "$k=$(psget "$k")"; done
      for sk in "${SKIPPED[@]+"${SKIPPED[@]}"}"; do echo "skipped_${sk%%:*}=${sk#*: }"; done; } > "$JOURNAL.tmp" && mv "$JOURNAL.tmp" "$JOURNAL"
  fi
  change() {   # key value: only a setting whose original is known (recorded) is changed
    local o; o=$(original "$1" tv)
    if [ "$o" = unknown ]; then echo "!! $1: could not read what this TV has, so it is left as it is (nothing to put back)"; return 0; fi
    cmds+=("$(putcmd "$1" "$2")"); want+=("$1=$2")
  }
  # the Play Store's bucket and appops follow the same rule as the other settings: what the TV had must be known (nothing to put back
  # otherwise), so an unreadable one is left alone with a !! line
  if has_key standby_bucket; then
    o=$(original standby_bucket ps)
    case "$o" in
      exempted) echo "The Play Store is exempted from standby on this TV: its standby bucket is left as it is" ;;
      unknown) echo "!! standby_bucket: could not read what this TV has for the Play Store, so it is left as it is (nothing to put back)" ;;
      *) cmds+=("$(psput standby_bucket "$BUCKET")"); pswant+=("standby_bucket=$BUCKET") ;;
    esac
  else echo "Play Store standby bucket: left alone (it is set on Android 11 and newer; this TV is SDK $SDK_SAY)"; fi
  for op in RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND; do
    if [ "$(original "appop_$op" ps)" = unknown ]; then echo "!! appop_$op: could not read what this TV has for the Play Store, so it is left as it is (nothing to put back)"
    else cmds+=("$(psput "appop_$op" deny)"); pswant+=("appop_$op=deny"); fi
  done
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do change "$k" "$v"; done
  if ! has_key max_cached_processes; then
    echo "Closed apps kept in memory: left alone (device_config needs Android 10; this TV is SDK $SDK_SAY)"
  elif [ -n "$CACHED" ]; then   # opt-in only (see above); never raise the limit: low-RAM sets may sit below it
    now=$("$PY" -I optimize/tv.py "dumpsys activity settings 2> /dev/null | grep -m1 max_cached_processes" | tr -dc '0-9')
    if [ -n "$now" ] && [ "$now" -gt "$CACHED" ]; then
      change max_cached_processes "$CACHED"
      echo "Closed apps kept in memory: $now -> $CACHED"
    else
      echo "Closed apps kept in memory: ${now:-unknown} (left as is)"
    fi
  else
    change max_cached_processes null   # Android's own limit (undoes an older run; the journal has the original)
  fi
  # a TV an older run set (its journal has the freezer's original): put that back now; a TV that never had it set is not touched
  o=$(jget cached_apps_freezer)
  if [[ "$o" =~ ^(null|[A-Za-z0-9_.-]+)$ ]] && [ "$o" != unknown ]; then cmds+=("$(putcmd cached_apps_freezer "$o")"); want+=("cached_apps_freezer=$o"); fi
  # trimming throws away every app's cache (thumbnails, things apps would load again): only worth it when storage is short
  space=$("$PY" -I optimize/tv.py "df -k /data 2> /dev/null | tail -n 1" | tr -d '\r' | awk '{print $2, $4}') || space=""
  read -r total avail <<< "$space" || true
  if [[ "${total:-}" =~ ^[0-9]+$ && "${avail:-}" =~ ^[0-9]+$ && "$total" -gt 0 ]]; then
    if [ "$avail" -lt 1048576 ] || [ $((avail * 10)) -lt "$total" ]; then
      cmds+=("pm trim-caches 999G")
      echo "Free storage is low ($((avail / 1024)) MB of $((total / 1024)) MB): app caches trimmed."
    else
      echo "Free storage is fine ($((avail / 1024)) MB of $((total / 1024)) MB): app caches left alone."
    fi
  else
    echo "Free storage unknown: app caches left alone."
  fi
elif [ -f "$JOURNAL" ]; then
  for k in $KEYS; do
    o=$(jget "$k")
    if [ "$k" = cached_apps_freezer ] && [ -z "$o" ]; then :   # not set by this version (no line in the journal): nothing to put back
    elif [ "$o" = unknown ] || [ -z "$o" ]; then echo "!! $k: what the TV had was not recorded (it could not be read), so it is left as it is"
    elif [[ "$o" =~ ^(null|[A-Za-z0-9_.-]+)$ ]]; then cmds+=("$(putcmd "$k" "$o")"); want+=("$k=$o")
    else echo "!! $k: the recorded value is not one this script puts back; left as it is"; fi
  done
  for k in $PSKEYS; do
    o=$(jget "$k")
    if [ "$k" = standby_bucket ] && [ -z "$o" ] && [ -n "$(jget skipped_standby_bucket)" ]; then :   # apply never touched it (Android 9 and 10): nothing to put back
    elif [ "$o" = exempted ]; then :   # the system's own exemption: never changed by apply, nothing to put back
    elif [[ "$o" =~ ^[a-z_]+$ ]] && [ "$o" != unknown ]; then cmds+=("$(psput "$k" "$o")"); pswant+=("$k=$o")
    else   # the original could not be read: apply left it alone, so there is nothing to put back (an older apply may have changed it: said)
      echo "!! $k: the TV's own value was not recorded (this firmware did not say), so it is left as it is; if an older version of these tools changed it, set it in the TV's settings"
    fi
  done
  echo "Putting back the values the TV had before (optimize/run/tweaks-journal.txt)."
elif [ -f "$JOURNAL_DONE" ]; then
  echo "The tweaks were already put back (optimize/run/tweaks-journal.done): nothing to do."
  exit 0
else
  ! has_key standby_bucket || cmds+=("$(psput standby_bucket active)")
  cmds+=("$(psput appop_RUN_IN_BACKGROUND allow)" "$(psput appop_RUN_ANY_IN_BACKGROUND allow)")
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do cmds+=("$(putcmd "$k" "$v")"); done
  ! has_key max_cached_processes || cmds+=("$(putcmd max_cached_processes null)")
  cmds+=("$(putcmd cached_apps_freezer device_default)")
  echo "No journal of the tweaks: Android's defaults."
fi
printf '%s\n' "${cmds[@]}" | "$PY" -I optimize/tv.py --batch > /dev/null
bad=0
for kv in "${want[@]+"${want[@]}"}"; do verify "${kv%%=*}" "${kv#*=}" || bad=1; done
for kv in "${pswant[@]+"${pswant[@]}"}"; do   # the standby bucket is the system's to move, so a mismatch is said, not fatal
  got=$(psget "${kv%%=*}")
  # an appop put back to «allow» that the TV reads as «default» is the same thing (allow IS the default for these two): not a problem (step 27: two !! on every reset)
  if [ "$got" != "${kv#*=}" ] && ! { case "${kv%%=*}" in appop_*) [ "${kv#*=}" = allow ] && [ "$got" = default ] ;; *) false ;; esac; }; then
    if [ "${kv%%=*}" = standby_bucket ] && test_build; then   # a test build (userdebug, test-keys) keeps its own bucket: not a problem to report
      echo "info: standby_bucket stays $got on this test build (userdebug / test-keys): the system keeps its own standby bucket"
    else echo "!! ${kv%%=*}: this TV did not take the value (asked for ${kv#*=}, it says $got)"; bad=1; fi
  fi
done
# Our two apps, compiled ahead of time from their profile (Android 7 / SDK 24 and newer; the same commands as SetupTweaks.java). Only an app that
# is on the TV, and only a «Success» is journaled; anything else is said and left to Android's own idle job (nothing to put back).
if [ "$1" = apply ]; then
  if [ "$SDK" -lt 24 ]; then echo "Our apps: not compiled ahead of time (cmd package compile needs Android 7; this TV is SDK $SDK_SAY)"
  else
    for p in $OURS; do
      "$PY" -I optimize/tv.py "pm list packages $p" 2> /dev/null | tr -d '\r' | grep -qx "package:$p" || continue
      r=$("$PY" -I optimize/tv.py "cmd package compile -m speed-profile -f $p 2>&1" 2> /dev/null | tr -d '\r') || r=""
      if grep -qx Success <<< "$r"; then jset "compiled_$p" speed-profile; echo "Compiled ahead of time: $p"
      else echo "info: $p was not compiled ahead of time (the TV said: $(tail -n 1 <<< "${r:-nothing}")); Android compiles it later by itself"; fi
    done
  fi
elif [ -f "$JOURNAL" ]; then   # the undo: back to how it was installed
  for p in $OURS; do
    [ -n "$(jget "compiled_$p")" ] || continue
    "$PY" -I optimize/tv.py "cmd package compile --reset $p 2>&1" > /dev/null 2>&1 || true
    echo "Back to as installed: $p"
  done
fi
if [ "$1" = restore ] && [ -f "$JOURNAL" ] && [ "$bad" = 0 ]; then mv -f "$JOURNAL" "$JOURNAL_DONE"; fi   # kept when the TV refused something
if [ "$1" = apply ]; then echo "Animation scale $v, Play Store background: $bg"
else echo "Tweaks undone, Play Store background: $bg"; fi
