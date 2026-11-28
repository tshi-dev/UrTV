#!/usr/bin/env bash
# Put a TV that was set up with these tools back the way it came, so it can be set up again "like new":
# every package from our lists re-enabled, the stock home and screensaver back, our apps (old and new names)
# removed, and TV Bro and SmartTube if the setup installed them, tweaks undone. Your own apps and data are not touched.
#   optimize/reset.sh
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
. optimize/home-journal.sh
stock_re='launcherx|tvlauncher'; stockpkg=$(jget stock | cut -d/ -f1); [[ "$stockpkg" =~ ^[A-Za-z0-9_.]+$ ]] && stock_re="$stock_re|$stockpkg"   # a TV with its own home app
tv() { "$PY" -I optimize/tv.py "$@"; }

echo "1/4 Stock home first (never leave the TV without one)…"
lists=(optimize/lists/tcl-safe.txt optimize/lists/tcl-optional.txt)
for extra in optimize/lists/mine*.txt; do   # local lists, e.g. mine-bloat.txt; absent on a fresh clone
  [ -f "$extra" ] && lists+=("$extra")
done
[ -s "${URTV_RUN_DIR:-optimize/run}/community-applied.txt" ] && lists+=("${URTV_RUN_DIR:-optimize/run}/community-applied.txt")   # a community list the setup used
# restore's own problems (a file it could not read, a note about a legacy install) must be seen, and must not stop the reset before it has put the home back
restore_rc=0
restore_out=$(optimize/restore.sh "${lists[@]}" 2>&1) || restore_rc=$?
{ grep -E '^(!!|Note:)' <<< "$restore_out" || true; } >&2
# R-402 (a G-TEN on Android 15): a TV whose own home is in no list and not in the journal (set up on the TV, or before the journal) never
# matched, so the reset always stopped here. Then the TV's own HOME apps are read, after restore turned them back on: system apps only, never
# ours, the setup wizard or a Settings stand-in.
if [ -z "$stockpkg" ]; then
  sys_pkgs=$(tv "pm list packages -s" 2> /dev/null | tr -d '\r' | sed -n 's/^package://p' || true)
  for p in $(tv "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME" 2> /dev/null | tr -d '\r' \
             | grep -oE '[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+' | cut -d/ -f1 | sort -u || true); do
    case "$p" in sa.urtv.*|sa.yourtv.*|com.abdullah.*|com.google.android.tungsten.setupwraith|com.android.settings|com.android.tv.settings) continue ;; esac
    grep -qxF "$p" <<< "$sys_pkgs" || continue
    stock_re="$stock_re|$p"
  done
fi
stock=no
for _ in 1 2 3 4; do   # a cold stock home can take a while to come up
  tv "input keyevent KEYCODE_HOME" > /dev/null
  sleep 6
  home=$(tv "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" | grep -E "$stock_re" | tail -n 1 || true)
  here=$(tv "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'" | sed -n 1p || true)
  if [ -n "$home" ] && grep -qE "$stock_re" <<< "$here"; then
    stock=yes; break
  fi
done
if [ "$stock" = no ]; then
  echo "!! The stock home is not on screen. Stopping before removing anything: press Home on the remote,"
  echo "   then run optimize/reset.sh again."
  exit 1
fi

optimize/keep.sh || true   # a TV optimized before may have voice, mirroring or updates off
echo "2/4 The TV's own screensaver…"
printf '%s\n' "settings delete secure screensaver_components" "settings delete secure screensaver_default_component" \
  | tv --batch > /dev/null
# install.sh switched screensaver_enabled on and set ours; backup.sh saved the values from before (null: it was never set)
if [ -f "${URTV_RUN_DIR:-optimize/run}/backup.txt" ]; then
  vals=$(tr -d '\r' < "${URTV_RUN_DIR:-optimize/run}/backup.txt" | awk '/^== /{s=($2=="screensaver")} s&&!/^==/{print}')
  { read -r comps; read -r enabled; read -r timeout; } <<< "$vals"
  { # the screensaver the TV had (a component list; none then, or ours from an earlier setup: left deleted, the TV picks its own)
    if [[ "${comps:-}" =~ ^[A-Za-z0-9_.]+/[A-Za-z0-9_.\$]+(,[A-Za-z0-9_.]+/[A-Za-z0-9_.\$]+)*$ ]] && [[ "$comps" != *sa.urtv.screensaver/* ]]; then
      echo "settings put secure screensaver_components '$comps'"
    fi
    for kv in "secure screensaver_enabled ${enabled:-}" "system screen_off_timeout ${timeout:-}"; do
      read -r ns key val <<< "$kv"
      [ -n "$val" ] || continue
      if [ "$val" = null ]; then echo "settings delete $ns $key"; else echo "settings put $ns $key $val"; fi
    done
  } | tv --batch > /dev/null
else
  echo "   (no optimize/run/backup.txt: screensaver_enabled and screen_off_timeout left as they are)"
fi

# the sleep service (an accessibility service dock.sh sleep-setup turned on): our entry leaves the list, nothing else does; the
# switch is put back as dock.sh recorded it (null: deleted) when no other service is left. Before our app is removed: Android
# drops an uninstalled app's service from the list by itself, and the switch would be left on
RUNDIR="${URTV_RUN_DIR:-optimize/run}"
if [ -f "$RUNDIR/accessibility-before.txt" ]; then
  svcs=$(tv "settings get secure enabled_accessibility_services" 2> /dev/null | tail -n 1 | tr -d '\r' || true)
  if [ -z "$svcs" ]; then   # only the word «null» means no service is on; an empty answer is «the TV did not say»
    echo "!! enabled_accessibility_services: the TV did not say which accessibility services are on, so our sleep service and the accessibility switch were left as they are: run optimize/reset.sh again if the sleep service must go"
  else
  [ "$svcs" != null ] || svcs=""
  rest=$(tr ':' '\n' <<< "$svcs" | grep -vE '^sa\.urtv\.(home|extras)/' | grep . | paste -sd: - || true)
  if [ -n "$rest" ]; then
    if [ "$rest" = "$svcs" ]; then :   # ours was not in the list: nothing to take out
    elif [[ "$rest" =~ ^[A-Za-z0-9_.\$/:]+$ ]]; then echo "settings put secure enabled_accessibility_services '$rest'" | tv --batch > /dev/null
    else echo "!! enabled_accessibility_services: the list has a character this script does not send; our entry was left in it"; fi
  else
    before=$(sed -n 's/^accessibility_enabled=//p' "$RUNDIR/accessibility-before.txt" | sed -n 1p)
    { [ -z "$svcs" ] || echo "settings delete secure enabled_accessibility_services"
      if [ "$before" = null ]; then echo "settings delete secure accessibility_enabled"
      elif [[ "$before" =~ ^[0-9]+$ ]]; then echo "settings put secure accessibility_enabled $before"; fi
    } | tv --batch > /dev/null
  fi
  fi
fi
# «حماية العائلة (DNS)»: the Private DNS the TV had comes back (the script's record and the home app's own, which only the app can put
# back), and only when that is confirmed the permission is taken back and the app removed: without it the original would be lost
# a failed question is not «the app is not installed»: removing the app would lose its record of the original Private DNS
home_list=$(tv "pm list packages; echo END_OF_LIST" 2> /dev/null | tr -d '\r') || home_list=""
# the answer counts only when it is read to its end marker and holds android itself (an empty reply is «the TV did not say», not «not installed»)
if [ "$(tail -n 1 <<< "$home_list")" != END_OF_LIST ] || ! grep -qx "package:android" <<< "$home_list"; then
  echo "!! familydns: could not ask the TV whether the home app is installed, so nothing was removed. Run optimize/reset.sh again."
  exit 1
fi
if [ -f "${URTV_RUN_DIR:-optimize/run}/family-dns-journal.txt" ] || grep -x "package:sa.urtv.home" > /dev/null <<< "$home_list"; then
  if ! optimize/familydns.sh off; then
    echo "!! familydns: the TV's Private DNS could not be put back, so nothing more was removed (the original setting is still recorded): run optimize/dock.sh familydns off, then optimize/reset.sh again"
    exit 1
  fi
fi
if [ -f "${URTV_RUN_DIR:-optimize/run}/grants-journal.txt" ]; then
  while read -r perm; do [[ "$perm" =~ ^android\.permission\.[A-Z_]+$ ]] && tv "pm revoke sa.urtv.home $perm" > /dev/null 2>&1 || true; done < "${URTV_RUN_DIR:-optimize/run}/grants-journal.txt"
fi
echo "3/4 Removing our apps (old and new names; missing ones are skipped)…"
# the appop the launcher was given for the adhan (it vanishes with the app anyway)
sdk=$(tv "getprop ro.build.version.sdk" 2> /dev/null | tr -dc '0-9' || true)
if [ "${sdk:-0}" -ge 31 ]; then tv "appops set sa.urtv.home SCHEDULE_EXACT_ALARM default" > /dev/null 2>&1 || true; fi
tv "appops set sa.urtv.extras SYSTEM_ALERT_WINDOW default" > /dev/null 2>&1 || true   # 4.4 «فوق تطبيق آخر» (dock.sh overlay on): the add-on's, not the home's
tv "appops set sa.urtv.home SYSTEM_ALERT_WINDOW default" > /dev/null 2>&1 || true   # a 4.4 build before the overlay moved to the add-on
# and «install unknown apps» (the TV's own update): put back exactly the mode install.sh found, then forget it
appop_file="${URTV_RUN_DIR:-optimize/run}/install-appop.txt"      # APPOP-BEGIN (the same place install.sh keeps it)
if [ -s "$appop_file" ]; then
  appop_mode=$(tr -d '\r\n ' < "$appop_file")
  if [[ "$appop_mode" =~ ^(allow|ignore|deny|default|foreground)$ ]]; then
    tv "appops set sa.urtv.home REQUEST_INSTALL_PACKAGES $appop_mode" > /dev/null 2>&1 && rm -f "$appop_file" || true
  fi
fi   # APPOP-END
for p in sa.urtv.home sa.urtv.screensaver sa.urtv.extras sa.urtv.listening sa.yourtv.home sa.yourtv.screensaver com.abdullah.tvhome com.abdullah.tvsaver; do
  tv "pm uninstall $p" | grep Success > /dev/null && echo "   removed $p" || true
done
# TV Bro and SmartTube: only if the setup put them there (not in the TV's package list saved before it)
for p in com.phlox.tvwebbrowser org.smarttube.stable; do
  # only against a real package list: a backup saved while the TV answered nothing would make every app look «installed by the setup»
  if [ -f "${URTV_RUN_DIR:-optimize/run}/backup.txt" ] && [ "$(grep -c '^package:' "${URTV_RUN_DIR:-optimize/run}/backup.txt" || true)" -ge 20 ] \
     && ! grep -qx "package:$p" "${URTV_RUN_DIR:-optimize/run}/backup.txt"; then
    tv "pm uninstall $p" | grep Success > /dev/null && echo "   removed $p" || true
  fi
done

echo "4/4 Tweaks back to what the TV had…"
from_journal=0; [ -f "${URTV_RUN_DIR:-optimize/run}/tweaks-journal.txt" ] || [ -f "${URTV_RUN_DIR:-optimize/run}/tweaks-journal.done" ] && from_journal=1   # tweaks.sh keeps the TV's own values there
{ optimize/tweaks.sh restore || echo "!! tweaks.sh restore failed: run optimize/tweaks.sh restore again"; } | grep '!!' || true
# tweaks.sh restore puts Android's defaults when it has no journal; where backup.sh recorded what this TV had (animation scales,
# the cached-apps freezer, the cached-processes limit), that is put back instead (null: it was never set, so it is deleted)
if [ "${from_journal:-0}" != 1 ] && [ -f "${URTV_RUN_DIR:-optimize/run}/backup.txt" ]; then
  section() { tr -d '\r' < "${URTV_RUN_DIR:-optimize/run}/backup.txt" | awk -v want="$1" '/^== /{s=($2==want)} s&&!/^==/{print}'; }
  { read -r w t a <<< "$(section animation | sed -n 1,3p | paste -sd' ' -)" || true
    for kv in "window_animation_scale ${w:-}" "transition_animation_scale ${t:-}" "animator_duration_scale ${a:-}"; do
      read -r key val <<< "$kv"
      if [ "${val:-}" = null ]; then echo "settings delete global $key"
      elif [[ "${val:-}" =~ ^[0-9]+(\.[0-9]+)?$ ]]; then echo "settings put global $key $val"; fi
    done
    cached=$(section cached)
    if [ "$(wc -l <<< "$cached")" -ge 2 ]; then
      freezer=$(tail -n 1 <<< "$cached"); limit=$(head -n 1 <<< "$cached")
      if [ "$freezer" = null ]; then echo "settings delete global cached_apps_freezer"
      elif [[ "$freezer" =~ ^(enabled|disabled|device_default)$ ]]; then echo "settings put global cached_apps_freezer $freezer"; fi
      [[ "$limit" =~ ^[0-9]+$ ]] && echo "device_config put activity_manager max_cached_processes $limit" || true
    fi
  } | tv --batch > /dev/null
fi
[ -d "${URTV_RUN_DIR:-optimize/run}" ] && mv "${URTV_RUN_DIR:-optimize/run}" "${URTV_RUN_DIR:-optimize/run}-$(date +%Y%m%d-%H%M%S)"   # keep the old results apart

"$PY" -I optimize/offlist.py clear 2> /dev/null || true   # the apps kept on by request: a reset starts like new
echo "Done. Still disabled on this TV (made so by the manufacturer, or by something else):"
optimize/settle.sh wait 2> /dev/null || true
tv "pm list packages -d" | sed 's/^package:/   /'
if [ "$restore_rc" != 0 ]; then
  echo "!! reset: everything else was done, but optimize/restore.sh reported a problem (the !! line above): fix it, then run optimize/restore.sh again."
  exit "$restore_rc"
fi
