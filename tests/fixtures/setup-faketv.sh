#!/usr/bin/env bash
# A fake Android TV for the parity tests of the setup in the app (tests/setup_prep_test.py, setup_community_test.py …): BOTH the shell scripts
# (as `$PY -I optimize/tv.py <command>` and `--batch`) and the app's Java steps (tests/SetupParityRun.java, one command per call) talk to this one
# TV, so their effects can be compared. The TV's commands are shell functions over files in $T/tv; a command line is run with eval, so
# `a; b`, `2> /dev/null`, `| grep` and `echo` work as on the TV. Every change is written to $T/changes, one line each.
#   env: T (the folder), SDK (34), MAKER (TCL), RCMODE=1 (exit with the command's own code: the Java side reads it as AdbClient.run does)
shift 2   # -I optimize/tv.py
S="$T/tv"; mkdir -p "$S"
change() { echo "$*" >> "$T/changes"; }
refused() { case " ${REFUSE:-} " in *" $1 "*) return 0 ;; esac; return 1; }
getprop() {
  case "$1" in
    ro.build.version.sdk) echo "${SDK-34}" ;;
    ro.build.type) echo "${BUILD_TYPE:-user}" ;;
    ro.product.manufacturer|ro.product.brand) echo "${MAKER:-TCL}" ;;
    ro.product.model) echo "${MODEL:-55C655}" ;;
    ro.product.device) echo "${DEVICE:-G10}" ;;
    ro.build.version.release) echo "${RELEASE:-14}" ;;
    *) [ -f "$S/prop_$1" ] && cat "$S/prop_$1" ;;
  esac
}
settings() {
  local ns=$2 k=$3
  case "$1" in
    get) [ "${EMPTYREAD:-}" = "$k" ] && return 0; [ -f "$S/$ns.$k" ] && cat "$S/$ns.$k" || echo null ;;
    put) change "settings put $ns $k $4"; refused "$k" || echo "$4" > "$S/$ns.$k" ;;
    delete) change "settings delete $ns $k"; refused "$k" || rm -f "$S/$ns.$k" ;;
  esac
}
device_config() {
  [ "${SDK-34}" -ge 29 ] 2> /dev/null || { echo "/system/bin/sh: device_config: not found"; return 127; }
  case "$1" in
    get) [ -f "$S/dc.$3" ] && cat "$S/dc.$3" || echo null ;;
    put) change "device_config put $2 $3 $4"; refused "$3" || echo "$4" > "$S/dc.$3" ;;
    delete) change "device_config delete $2 $3"; refused "$3" || rm -f "$S/dc.$3" ;;
  esac
}
num() { case "$1" in active) echo 10 ;; working_set) echo 20 ;; frequent) echo 30 ;; rare) echo 40 ;; restricted) echo 45 ;; never) echo 50 ;; esac; }
am() {
  case "$1" in
    get-standby-bucket) [ "${UNREADABLE:-}" = yes ] && return 0; [ -f "$S/bucket" ] && cat "$S/bucket" || echo "${BUCKET:-10}" ;;
    set-standby-bucket) change "am set-standby-bucket $2 $3"
                        if [ "$3" = restricted ] && [ "${SDK-34}" -lt 30 ]; then echo "Bad bucket: restricted"; return 1; fi
                        refused standby_bucket || num "$3" > "$S/bucket" ;;
    force-stop) change "am force-stop $2" ;;
  esac
}
appops() {
  case "$1" in
    get) local op=$3
         case "$op" in AUTO_START|APP_AUTO_START)
           case " ${AUTOSTART_OPS:-} " in *" $op "*) ;; *) echo "Error: Unknown operation string: $op" >&2; return 255 ;; esac ;;
         esac
         [ "${UNREADABLE:-}" = yes ] && [ "$2" = com.android.vending ] && return 0
         if [ -f "$S/op.$2.$op" ]; then echo "$op: $(cat "$S/op.$2.$op"); time=-1d2h"; else echo "No operations."; fi ;;
    set) change "appops set $2 $3 $4"
         case "$3" in AUTO_START|APP_AUTO_START) case " ${AUTOSTART_OPS:-} " in *" $3 "*) ;; *) echo "Error: Unknown operation string: $3" >&2; return 255 ;; esac ;; esac
         refused "$3" && return 0
         if [ "$4" = default ]; then rm -f "$S/op.$2.$3"; else echo "$4" > "$S/op.$2.$3"; fi ;;
    write-settings) change "appops write-settings" ;;
  esac
}
pkgs() { cat "$S/packages" 2> /dev/null; }
off() { cat "$S/off" 2> /dev/null; }
pm() {
  case "$1" in
    list)
      case "$2 $3" in
        "packages -d") [ "${SILENT_OFF:-}" = yes ] && return 0; off | sed 's/^/package:/' ;;
        "packages ") [ "${NO_ANDROID:-}" = yes ] || echo package:android; pkgs | sed 's/^/package:/' ;;
        "features ") echo feature:android.software.leanback ;;
        *) { echo android; pkgs; } | grep -F -- "$3" | sed 's/^/package:/' ;;
      esac ;;
    enable) local p=${!#}; change "pm enable $*"; case " ${STUCK:-} " in *" $p "*) return 0 ;; esac
            grep -vxF "$p" "$S/off" > "$S/off.tmp" 2> /dev/null; mv "$S/off.tmp" "$S/off"; echo "Package $p new state: enabled" ;;
    disable-user) local p=${!#}; change "pm disable-user $*"; pkgs | grep -qxF "$p" && { off | grep -qxF "$p" || echo "$p" >> "$S/off"; }
                  echo "Package $p new state: disabled-user" ;;
    trim-caches) change "pm trim-caches $2" ;;
  esac
}
cmd() {
  case "$1" in
    appops) shift; appops "$@" ;;
    package) case "$2" in
               resolve-activity) echo "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false"; echo "${HOME_NOW:-com.google.android.apps.tv.launcherx/.home.HomeActivity}" ;;
               query-activities) [ "${NO_HOMES:-}" = yes ] && return 0; printf '%s\n' ${HOMES:-com.google.android.apps.tv.launcherx/.home.HomeActivity} | sed 's/^/    /' ;;
             esac ;;
  esac
}
dumpsys() {
  case "$1" in
    activity) if [ "${2:-}" = settings ]; then echo "  max_cached_processes=1024"; else echo "  topResumedActivity=ActivityRecord{1a2b u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t3}"; fi ;;
    dreams) echo "  mCurrentDream=null" ;;
    meminfo) if [ -n "${2:-}" ]; then echo "                   Pss  Private"; echo "        TOTAL    51200    40000"; return 0; fi
             printf 'Total PSS by process:
    180,000K: system (pid 900)
     90,000K: com.google.android.apps.tv.launcherx (pid 1500 / activities)

Total RAM: 2,030,000K (status normal)
 Free RAM:   830,000K
 Used RAM: 1,100,000K
 Lost RAM:    90,000K
' ;;
  esac
}
# what measure.sh and the app read: the TV's own /proc/meminfo, processes, CPU and boot (files of the fake TV, not this computer's)
grep() { if [ "${!#}" = /proc/meminfo ]; then command grep "${@:1:$#-1}" "$S/meminfo"; else command grep "$@"; fi; }
cat() { if [ "${1:-}" = /proc/sys/kernel/random/boot_id ]; then echo "${BOOT_ID:-boot-1}"; else command cat "$@"; fi; }
ps() { seq 1 "${PROCS:-300}"; }
top() { echo "Tasks: 300 total"; echo "400%cpu  10%user   0%nice   8%sys 350%idle   0%iow"; }
df() { echo "Filesystem 1K-blocks Used Available Use% Mounted on"; echo "/dev/block/dm-5 ${DF_TOTAL:-16777216} 4194304 ${DF_AVAIL:-12582912} 25% /data"; }
sync() { :; }
input() { :; }

if [ "${1:-}" = --batch ]; then
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    out=$(eval "$line" 2>&1)
    printf '$ %s\n%s\n' "$line" "$out"
  done
  exit 0
fi
# a TV that drops the answer about its disabled apps (nothing at all comes back, not even the end marker)
if [ "${SILENT_OFF:-}" = yes ] && [[ "$1" == *"pm list packages -d"* ]]; then echo; exit 0; fi
out=$(eval "$1" 2>&1); rc=$?
printf '%s\n' "$out"
[ "${RCMODE:-}" = 1 ] && exit "$rc"
exit 0
