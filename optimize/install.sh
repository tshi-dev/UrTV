#!/usr/bin/env bash
# Download the latest APKs from GitHub and install them on the TV.
#   optimize/install.sh saver      شاشة تلفزيونك: install + make it the screensaver
#   optimize/install.sh launcher   the home screen: install + make it the default Home
#                                  (it carries the «تلاوة» screens: the Quran recitations and the radio)
#   optimize/install.sh extras     two free apps from their own GitHub releases: TV Bro (a TV web browser,
#                                  github.com/truefedex/tv-bro) and SmartTube (YouTube without ads,
#                                  github.com/yuliskov/SmartTube), the build that fits the TV's processor
#   optimize/install.sh all        both (saver, launcher)
# An APK signed with another key cannot replace the installed one: this script stops with a !! line and never uninstalls
# by itself (that deletes the app's settings); the agent asks the person first.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
REL=https://github.com/tshi-dev/UrTV/releases/download/${APK_TAG:-apk-latest}   # APK_TAG=v4.0.N: that numbered build (v3.N for the old ones)
what="${1:?usage: optimize/install.sh saver|launcher|extras|all}"
case "$what" in saver|launcher|extras|all) ;; *) echo "usage: optimize/install.sh saver|launcher|extras|all" >&2; exit 2;; esac
. optimize/home-journal.sh   # the journal of the home switch (what the switch changed, to undo it after a restart)
failed=0   # an extra that did not install: the rest are still tried, then run.sh must not count the step as done

# curl with up to 3 tries. A Windows curl (Schannel) that can't reach the certificate revocation server fails every
# download; on that error, and only if this curl has the flag, the next try skips just that check.
curl_get() {
  local i rc=0 errf extra=()
  errf=$(mktemp)
  for i in 1 2 3; do
    rc=0; curl "${extra[@]+"${extra[@]}"}" "$@" 2> "$errf" || rc=$?
    [ "$rc" = 0 ] && { rm -f "$errf"; return 0; }
    [ "$rc" = 22 ] && break   # the server answered with an error (no such file): another try gives the same
    if [ "${#extra[@]}" = 0 ] && grep -qiE 'revocation|CRYPT_E_' "$errf" \
       && curl -V 2> /dev/null | grep -qi schannel && curl --help all 2> /dev/null | grep -q ssl-revoke-best-effort; then
      echo "Windows could not check the certificate revocation list: trying again without that check." >&2
      extra=(--ssl-revoke-best-effort)
    fi
    [ "$i" = 3 ] || sleep $((i * 2))
  done
  cat "$errf" >&2; rm -f "$errf"
  return "$rc"
}

# Play Protect can refuse an ADB install (INSTALL_FAILED_VERIFICATION_FAILURE, seen on a TCL Android 14): its ADB
# check is switched off for that install only, and put back as it was, whatever happens (see the EXIT trap).
VERIFIER=verifier_verify_adb_installs
verifier_old=; verifier_set=0
restore_verifier() {
  [ "$verifier_set" = 1 ] || return 0
  verifier_set=0
  if [ "$verifier_old" = null ]; then
    "$PY" -I optimize/tv.py "settings delete global $VERIFIER" > /dev/null 2>&1 \
      || echo "!! Play Protect's ADB check could not be put back (the connection dropped): it was not set before; run optimize/venv/bin/python -I optimize/tv.py \"settings delete global $VERIFIER\""
  else
    "$PY" -I optimize/tv.py "settings put global $VERIFIER $verifier_old" > /dev/null 2>&1 \
      || echo "!! Play Protect's ADB check could not be put back (the connection dropped): it was $verifier_old before; run optimize/venv/bin/python -I optimize/tv.py \"settings put global $VERIFIER $verifier_old\""
  fi
}
trap restore_verifier EXIT

pm_install() {   # $1 file name in /data/local/tmp, $2 package: pm's answer. A big APK can install silently for over a
  # minute, which ADB takes for a dead TV: it runs in the background on the TV and is polled (tv.py --bg).
  # Some operator TVs have a system service that removes apps installed from ADB seconds later; installed with the
  # Play Store as the installer they stay. So: still there after 3 s? If not, install once more that way, and say so.
  # tv.py --bg exits with pm's own exit code (1 on a failed install): that is not a failure of this function. A TV that
  # never answered gives no output at all: NO_ANSWER, which the callers turn into a clear line.
  local out
  out=$(TV_JOB_LIMIT_S=1200 "$PY" -I optimize/tv.py --bg install "pm install -r /data/local/tmp/$1") || true
  if [ -z "$out" ]; then printf '%s\n' NO_ANSWER; return 0; fi
  if grep -q Success <<< "$out"; then
    sleep 3
    if ! "$PY" -I optimize/tv.py "pm list packages $2" | tr -d '\r' | grep -qx "package:$2"; then
      echo "$2 was removed right after the install (a service of this TV does that to apps installed over ADB): installing it again as if from the Play Store." >&2
      out=$(TV_JOB_LIMIT_S=1200 "$PY" -I optimize/tv.py --bg install "pm install -i com.android.vending -r /data/local/tmp/$1") || true
      sleep 3
      if grep -q Success <<< "$out" && ! "$PY" -I optimize/tv.py "pm list packages $2" | tr -d '\r' | grep -qx "package:$2"; then
        out="Failure [$2 is removed by this TV right after every install]"
      fi
    fi
  fi
  printf '%s\n' "$out"
}

push_install() {   # $1 apk name, $2 package
  if [ "$2" = sa.urtv.home ]; then   # the home app targets an older SDK than Play Protect likes (seen on Android 12-14)
    echo "للشخص: بعد لحظات ممكن تطلع على التلفزيون رسالة من Play Protect؛ إذا طلعت اضغط «تثبيت على أي حال» (Install anyway)."
  fi
  # TCL: reinstalling the home app can switch the TV to an input; its TV app is stopped first (idea from Gathin23/cleanyourtv 11993bb0)
  if [ "$2" = sa.urtv.home ] && "$PY" -I optimize/tv.py "pm list packages com.tcl.tv" 2> /dev/null | tr -d '\r' | grep -qx "package:com.tcl.tv"; then
    "$PY" -I optimize/tv.py "am force-stop com.tcl.tv" > /dev/null 2>&1 || true
  fi
  if ! curl_get -fsSL -o "optimize/$1" "$REL/$1"; then
    echo "!! Could not download $REL/$1 (tag ${APK_TAG:-apk-latest}): no such build, or no internet"; exit 1
  fi
  "$PY" -I optimize/tv.py --push-b64 "optimize/$1" "/data/local/tmp/$1"
  local out
  out=$(pm_install "$1" "$2")
  if grep -q INSTALL_FAILED_VERIFICATION_FAILURE <<< "$out"; then
    verifier_old=$("$PY" -I optimize/tv.py "settings get global $VERIFIER" | tr -d '\r\n' || true)
    if [ -z "$verifier_old" ]; then   # only the word «null» means «not set»; an empty answer is «the TV did not say»
      echo "!! Play Protect refused the install over ADB, but the TV did not say what its ADB check is set to, so it was not changed. Run the same command again."
      exit 1
    fi
    echo "Play Protect refused the install over ADB: its ADB check is off for this install only, and put back right after (was: ${verifier_old:-not set})."
    echo "للشخص: لو طلعت على التلفزيون رسالة من Play Protect (التطبيق «غير آمن» أو «مبني لإصدار قديم»)، اضغط «تثبيت على أي حال» (Install anyway) بالريموت. هذا تطبيق تلفزيونك، والرسالة تطلع لأن الشاشة الرئيسية مبنية لإصدار أندرويد أقدم."
    verifier_set=1
    "$PY" -I optimize/tv.py "settings put global $VERIFIER 0" > /dev/null || true
    out=$(pm_install "$1" "$2")
  fi
  if [ "$out" = NO_ANSWER ]; then
    echo "!! The TV stopped answering while $1 was installing (some TVs take minutes and drop ADB meanwhile). Wait a minute, check the TV is on and on Wi-Fi, then run the same command again: an install that finished is found and not repeated."
    exit 1
  fi
  if ! grep -q Success <<< "$out"; then
    # an app with this name but another signature (a copy built by hand, or from another source) cannot be updated in place,
    # and removing it deletes its settings, tasks and own adhan: never done here, the person decides
    if grep -qE 'UPDATE_INCOMPATIBLE|signatures do not match' <<< "$out"; then
      echo "!! $2 is already installed, signed with another key (a copy built by hand or from another source), so ours cannot replace it. Nothing was removed."
      echo "   Removing it (pm uninstall $2) would delete its settings, tasks and own adhan, and Android cannot keep them across signatures. Ask the person first, in Arabic: «عندك نسخة من التطبيق غير الرسمية، لو أزلناها تنحذف إعداداتها ومهامها. أزيلها وأثبّت الرسمية (تتحدّث وحدها بعدين)؟». Only after a yes: pm uninstall $2, then the same command again."
      exit 1
    else
      echo "!! Install of $1 failed: $out"
      if grep -q INSTALL_FAILED_VERIFICATION_FAILURE <<< "$out"; then
        echo "   Play Protect still refuses it. Tell the person, in Arabic: «اضغط على التلفزيون «تثبيت على أي حال» لما تطلع رسالة Play Protect، وإذا ما طلعت: الإعدادات ← متجر Play ← Play Protect ← «فحص التطبيقات» وقف مؤقتاً، وبعدها نعيد المحاولة». Do not turn the verifier off yourself (settings put global package_verifier_enable 0): only this script changes verifier_verify_adb_installs, for the install, and puts it back."
      fi
      exit 1
    fi
  fi
  restore_verifier
  "$PY" -I optimize/tv.py "rm -f /data/local/tmp/$1"
}

pick_asset() {   # $1 asset pattern ({abi} = a processor type), $2 the TV's processor types (best first), $3 the release JSON: "url size" of the best build
  "$PY" -I -c '
import json, re, sys
assets = {a["name"]: (a["browser_download_url"], a.get("size", 0)) for a in json.load(sys.stdin)["assets"]}
for abi in [a for a in sys.argv[2].split(",") if a] + ["universal", ""]:
    pat = re.compile(sys.argv[1].replace("{abi}", re.escape(abi)) + "$")
    hit = sorted(n for n in assets if pat.search(n))
    if hit:
        print(assets[hit[0]][0], assets[hit[0]][1]); break
' "$1" "$2" <<< "$3"
}

from_github() {   # $1 owner/repo, $2 package, $3 asset pattern ({abi} = the TV's processor types, best first)
  local abis url name out code body size attempt prop
  # an extra that an earlier try already installed is not downloaded or uploaded again: a flaky Wi-Fi that dropped on the second
  # one resumes there, not from the first (an upload cut off half way is resumed by tv.py itself)
  if "$PY" -I optimize/tv.py "pm list packages $2" | tr -d '\r' | grep -qx "package:$2"; then
    echo "$2 is already on the TV: left as it is (it updates itself)."; return 0
  fi
  body=$(curl_get -sSL -w '\n%{http_code}' "https://api.github.com/repos/$1/releases/latest") || body=$'\n000'
  code=${body##*$'\n'}; body=${body%$'\n'*}
  if [ "$code" = 403 ] || [ "$code" = 429 ]; then
    echo "!! GitHub rate limit, try later: $1 skipped"; failed=1; return 0
  elif [ "$code" != 200 ]; then
    echo "!! GitHub did not list the releases of $1 (HTTP $code): skipped"; failed=1; return 0
  fi
  # the list of processor types is read up to 3 times: a flaky TV (seen on an AFMU0) answered with nothing, or with a short list,
  # and «no build» was said for an app that has one. An empty answer is asked again (abilist, then abilist32, then abi).
  for attempt in 1 2 3; do
    abis=""
    for prop in ro.product.cpu.abilist ro.product.cpu.abilist32 ro.product.cpu.abi; do
      abis=$("$PY" -I optimize/tv.py "getprop $prop" 2> /dev/null | tr -d '\r\n ' || true)
      [ -z "$abis" ] || break
    done
    url=$(pick_asset "$3" "$abis" "$body") || url=""
    [ -z "$url" ] || break
    [ "$attempt" = 3 ] || sleep 3
  done
  if [ -z "$url" ]; then echo "!! No build of $1 for this TV (${abis:-no answer about its processor}): skipped"; return 0; fi
  size=${url##* }; url=${url% *}
  name=$(basename "$url")
  if [ -f "optimize/$name" ] && [ "$size" != 0 ] && [ "$(wc -c < "optimize/$name" | tr -d ' ')" = "$size" ]; then
    echo "$name is already downloaded: not downloading it again."
  elif ! curl_get -fsSL -o "optimize/$name" "$url"; then echo "!! Could not download $url"; failed=1; return 0; fi
  # An optional app never holds the setup up: the upload and the install are tried twice (an upload resumes where it stopped),
  # and a TV that did not answer both times means this app is skipped with one line and the setup goes on.
  local try
  for try in 1 2; do
    if "$PY" -I optimize/tv.py --push-b64 "optimize/$name" "/data/local/tmp/$name"; then out=$(pm_install "$name" "$2"); else out=NO_ANSWER; fi
    [ "$out" = NO_ANSWER ] || break
    # the install may even have finished while the TV was silent
    ! "$PY" -I optimize/tv.py "pm list packages $2" 2> /dev/null | tr -d '\r' | grep -qx "package:$2" || break
    [ "$try" = 2 ] || sleep 5
  done
  if [ "$out" = NO_ANSWER ] && ! "$PY" -I optimize/tv.py "pm list packages $2" 2> /dev/null | tr -d '\r' | grep -qx "package:$2"; then
    echo "!! skipped after 2 timeouts: ${1##*/}; to add it later: optimize/install.sh extras"
    return 0   # the downloaded file stays, so the next try does not fetch it again
  fi
  "$PY" -I optimize/tv.py "rm -f /data/local/tmp/$name" > /dev/null 2>&1 || true
  rm -f "optimize/$name"
  if [ "$out" = NO_ANSWER ]; then echo "Installed $name (the TV was slow to answer)"
  elif grep -q Success <<< "$out"; then echo "Installed $name"
  elif grep -qE 'UPDATE_INCOMPATIBLE|signatures do not match' <<< "$out"; then
    echo "$2 is already installed from somewhere else: left as it is (with its settings)."   # never removed
  else echo "!! Install of $name failed: $out"; failed=1; fi
}

# A home switch that a restart cut off (journal says «switching», Home is not ours, no marker) is undone first.
[ "$what" = extras ] || jrecover || exit 1

if [ "$what" = extras ]; then
  from_github truefedex/tv-bro com.phlox.tvwebbrowser 'tvbro-[0-9.]+-generic-geckoExcluded\.apk'   # light: the TV's WebView
  from_github yuliskov/SmartTube org.smarttube.stable 'SmartTube_stable_[0-9.]+_{abi}\.apk'
  [ "$failed" = 0 ] || exit 1
fi

if [ "$what" = saver ] || [ "$what" = all ]; then
  push_install urtv-screensaver.apk sa.urtv.screensaver
  # a WebView older than 80 cannot run the page: the TV keeps its own screensaver (ours is installed, not selected) until
  # the person updates «Android System WebView»; precheck.sh held exits 4 then, and 5 (or a failure) when the TV did not say: unknown holds too
  held=0; held_rc=0; PY="$PY" optimize/precheck.sh held > /dev/null 2>&1 || held_rc=$?
  if [ "$held_rc" = 4 ]; then held=1; elif [ "$held_rc" != 0 ]; then held=2; fi
  if [ "$held" = 2 ]; then
    echo "!! The screensaver is installed but this run did NOT switch it on (what the TV had selected is untouched): the TV did not say which «Android System WebView» it has, and an old one (older than 80) would not run the page. Run optimize/install.sh saver again; if it says this again, check the WebView in the TV's Play Store (optimize/precheck.sh webview-store opens it)."
  elif [ "$held" = 1 ]; then
    echo "!! The screensaver is installed but NOT switched on: this TV's WebView is older than 80 and the page would not run, so the TV keeps its own screensaver. Update «Android System WebView» in the Play Store (optimize/precheck.sh webview-store opens it), then run optimize/install.sh saver again."
  else
    printf '%s\n' \
      "settings put secure screensaver_enabled 1" \
      "settings put secure screensaver_components sa.urtv.screensaver/.ScreenDream" \
      "settings put secure screensaver_default_component sa.urtv.screensaver/.ScreenDream" \
      "am start --activity-clear-task -n sa.urtv.screensaver/.SettingsActivity" \
      | "$PY" -I optimize/tv.py --batch
    # read back: some firmware keeps its own screensaver whatever is written (reset.sh puts back what backup.sh recorded)
    got=$("$PY" -I optimize/tv.py "settings get secure screensaver_components" 2> /dev/null | tr -d '\r' | tail -n 1) || got=""
    case "$got" in
      *sa.urtv.screensaver/*) ;;
      *) echo "!! screensaver_components: this TV did not take our screensaver (it says ${got:-nothing}); choose it in the TV's own settings (Settings, Device Preferences, Screen saver)" ;;
    esac
    echo "Screensaver installed and selected. Its settings screen is open on the TV."
  fi
fi

if [ "$what" = launcher ] || [ "$what" = all ]; then
  # Which stock home does this TV have? Google TV sets: launcherx. Older Android TV: tvlauncher.
  # Only a package that is really installed is ever named (a guessed one would be "restored" to nothing): another maker's own home is
  # taken from the app that answers Home now, if there is exactly one besides ours and the setup wizard; else the switch is not made.
  # One read of the package list; a list without the android package itself is «the TV did not answer», not «nothing is installed».
  pkglist=$("$PY" -I optimize/tv.py "pm list packages" 2> /dev/null | tr -d '\r') || pkglist=""
  have_pkg() { grep -qx "package:$1" <<< "$pkglist"; }
  STOCK=; stock_unreadable=no
  if ! have_pkg android; then
    stock_unreadable=yes
  elif have_pkg com.google.android.apps.tv.launcherx; then
    STOCK=com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity
  elif have_pkg com.google.android.tvlauncher; then
    STOCK=com.google.android.tvlauncher/.MainActivity
  else
    # query-activities prints each component indented under «Activity #n:»; the setup wizard, a bare fallback home, the settings apps and
    # Android's own core are never taken for the TV's home
    cand=$("$PY" -I optimize/tv.py "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null" 2> /dev/null \
           | tr -d '\r' | sed 's/^[[:space:]]*//' | grep -E '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' \
           | grep -vE '^(sa\.urtv\.|com\.google\.android\.tungsten\.setupwraith|com\.android\.settings|com\.android\.tv\.settings)' \
           | grep -vE '/([A-Za-z0-9_.]*\.)?FallbackHome$' | sort -u || true)
    if [ -f optimize/lists/core.txt ]; then
      cand=$(grep -vEf <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/core.txt | sed 's/.*/^(&)\//') <<< "$cand" || true)
    fi
    if [ "$(grep -c . <<< "$cand")" = 1 ] && have_pkg "${cand%%/*}"; then STOCK=$cand; fi
  fi
  echo "Stock home: ${STOCK:-not found}"

  # 1. The new home must be installed and working BEFORE anything is disabled.
  push_install urtv-home.apk sa.urtv.home
  "$PY" -I optimize/tv.py "pm grant sa.urtv.home android.permission.ACCESS_FINE_LOCATION" || true
  # exact alarms for the adhan (Android 12 and newer; older TVs need nothing)
  # (this operation exists from Android 12, SDK 31; older Android prints «Unknown operation string» and needs nothing)
  sdk=$("$PY" -I optimize/tv.py "getprop ro.build.version.sdk" 2> /dev/null | tr -dc '0-9' || true)
  if [ "${sdk:-0}" -ge 31 ]; then "$PY" -I optimize/tv.py "appops set sa.urtv.home SCHEDULE_EXACT_ALARM allow" || true; fi
  # «تحديث تلفزيوني» (the TV's own update) hands the new apps to Android's install screen, which needs «install unknown apps» for the home app:
  # given ahead of time so the owner isn't sent to a settings screen first. The mode it had is kept, once (a second run must not take our own
  # «allow» for the original), and optimize/reset.sh puts back exactly that. It is given only over Android's default: an explicit choice
  # stays. If the TV doesn't answer, nothing is changed: the TV's update screen asks for the permission itself when it is needed.
  appop_file="${URTV_RUN_DIR:-optimize/run}/install-appop.txt"   # APPOP-BEGIN
  if [ ! -s "$appop_file" ]; then
    appop_now=$("$PY" -I optimize/tv.py "appops get sa.urtv.home REQUEST_INSTALL_PACKAGES" 2>/dev/null | tr -d '\r' || true)
    appop_mode=$(sed -nE 's/^REQUEST_INSTALL_PACKAGES: *(allow|ignore|deny|default|foreground).*/\1/p' <<< "$appop_now" | head -n 1)
    if [ -z "$appop_mode" ] && grep -q "No operations" <<< "$appop_now"; then appop_mode=default; fi   # never set: Android's own default
    if [ -n "$appop_mode" ]; then
      mkdir -p "$(dirname "$appop_file")"
      printf '%s\n' "$appop_mode" > "$appop_file"
    fi
  fi
  # only Android's own default is replaced: an «ignore», «deny» or «foreground» the owner (or the firmware) chose stays as it is
  if [ -s "$appop_file" ] && [ "$(tr -d '\r\n ' < "$appop_file")" = default ]; then
    "$PY" -I optimize/tv.py "appops set sa.urtv.home REQUEST_INSTALL_PACKAGES allow" || true
  fi   # APPOP-END

  # The setup wizard (setupwraith) is turned off only when it really answers Home. Disabling it can delete the Wi-Fi network
  # that was saved during the first-run wizard (CleanYourTV's README, github.com/Gathin23/cleanyourtv, MIT: a network saved
  # then belongs to the wizard, and Android removes an app's saved networks when it is disabled; two TCL sets lost their
  # Wi-Fi password at the first restart, and a Nokia refused it). Their guard asks `dumpsys wifi` who owns the networks.
  WRAITH=com.google.android.tungsten.setupwraith
  wraith_answers_home() {   # the apps that answer HOME, all of them (resolve-activity only names the winner)
    "$PY" -I optimize/tv.py "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null; cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME" \
      | grep "$WRAITH" > /dev/null   # no grep -q: with pipefail its early exit would SIGPIPE the command before it
  }
  wifi_owner() {   # owned: a saved network belongs to the wizard; other: none does; unknown: the TV did not say
    local w owners
    w=$("$PY" -I optimize/tv.py "dumpsys wifi") || { echo unknown; return; }
    owners=$(grep -o 'cname=[^ ]*' <<< "$w" || true)
    if [ -z "$owners" ]; then echo unknown
    elif grep -q setupwraith <<< "$owners"; then echo owned
    else echo other; fi
  }
  wraith_off() {   # 0: turned off; 1: left on (owns the Wi-Fi, or it could not be told)
    case "$(wifi_owner)" in
      other) "$PY" -I optimize/tv.py "pm disable-user --user 0 $WRAITH" > /dev/null || true; optimize/settle.sh mark 2> /dev/null || true; return 0 ;;
      owned) echo "The setup wizard of this TV owns its saved Wi-Fi network: left on, so the network is not lost." ;;
      *)     echo "Could not tell who owns the saved Wi-Fi network: the setup wizard is left on." ;;
    esac
    return 1
  }

  WIZARD_KEPT=no   # yes: the switch went on with the setup wizard left on (it owns the Wi-Fi, or the TV did not say who does)
  DIAG=
  home_diag() {   # why Home does not open ours: who holds the role, what Home resolves to, what is on screen after Home
    local holders res top
    holders=$("$PY" -I optimize/tv.py "cmd role get-role-holders --user 0 android.app.role.HOME 2> /dev/null" 2> /dev/null | tr -d '\r' | sed -n 1p) || holders=""
    res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null | tr -d '\r' | tail -n 1) || res=""
    top=$("$PY" -I optimize/tv.py "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'" 2> /dev/null | tr -d '\r' | head -n 1 | sed 's/^ *//') || top=""
    DIAG="role holder: ${holders:-none}; Home resolves to: ${res:-unknown}; on screen after Home: ${top:-unknown}"
  }
  # an on-TV dialog (a Home chooser, a role request, a permission) is what is in front
  dialog_open() { grep -qE 'ResolverActivity|ChooserActivity|RequestRoleActivity|permissioncontroller|GrantPermissions' <<< "$1"; }

  # Steps 2-4 as one function, so an ADB drop in the middle (TCL G08: Wi-Fi fell during the batch) can be retried.
  # "verify" as $1: only step 4 (the switch is armed already and the batch may have run).
  # Returns 0: the new home is on screen and the switch is disarmed; 1: tv.py failed (connection lost);
  # 2: Home did not open the new launcher; 3: the safety switch fired first.
  switch_home() {
    local top res stepout ours=no _ wraith=no cmds alive disabled
    if [ "${1:-}" != verify ]; then
      # before anything is changed: does the wizard answer Home, and can it go without taking the Wi-Fi with it?
      if wraith_answers_home; then
        # owned: the switch goes on with the wizard left on (the HOME role and set-home-activity choose ours, and the
        # check below and the safety switch catch a TV where it still wins). Stopping here blocked 13 of 39 setups on
        # 51b9680, and forgetting the network did not help on half of them.
        case "$(wifi_owner)" in
          owned) WIZARD_KEPT=yes; echo "The setup wizard answers Home and owns the saved Wi-Fi network: it is left on (the network stays), and the switch goes on without turning it off." ;;
          other) wraith=listed ;;
          *) WIZARD_KEPT=yes; echo "The setup wizard answers Home, but the TV did not say who owns its saved Wi-Fi: the wizard is left on, so the network is not lost." ;;   # (if Home then does not open ours, this TV keeps its own home: one plain result, rc 6)
        esac
      fi
      # 2. Arm the safety switch on the TV: it restores the stock home in 300 s unless we confirm (slow ADB needs time).
      #    It carries a nonce (a marker of an older switch cannot disarm this one), and it must show it is alive before
      #    anything is changed. The journal records what the switch changes, to undo it if the TV restarts in the middle.
      local old_nonce role wiz stk
      old_nonce=$(jget nonce)
      NONCE="$(date +%s)-$$-$RANDOM"
      role=$("$PY" -I optimize/tv.py "cmd role get-role-holders --user 0 android.app.role.HOME 2> /dev/null" 2> /dev/null | tr -d '\r' | sed -n 1p) || role=""
      grep -qE '^[A-Za-z0-9_.]+$' <<< "$role" || role=""
      # a second run starts with ours as the holder: the earlier journal still knows who it was before
      if [ "$role" = sa.urtv.home ] || [ -z "$role" ]; then role=$(jget role_holder_before); fi
      if [ -z "$role" ]; then   # no role holder named (older Android, or the TV said nothing): the app that answers Home now is "who it was before"
        res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null | tr -d '\r' | tail -n 1) || res=""
        role=$(sed -n 's#^\([A-Za-z0-9_.]*\)/[A-Za-z0-9_.$]*$#\1#p' <<< "$res")
        [ -n "$res" ] || return 9   # nothing says who has Home: undoing the switch could not put the right app back
        # only the stock home counts as «before»: the chooser (android), the setup wizard or ours would send a restore to the wrong app
        [ "$role" = "${STOCK%%/*}" ] || role=""   # anything else: no one earlier is known, a restore goes to the stock home
      fi
      wiz=absent; stk=absent
      if disabled=$("$PY" -I optimize/tv.py "pm list packages -d" 2> /dev/null | tr -d '\r'); then
        grep -qx "package:$WRAITH" <<< "$disabled" && wiz=disabled || wiz=enabled
        grep -qx "package:${STOCK%%/*}" <<< "$disabled" && stk=disabled || stk=enabled
      fi
      [ "$wiz" != disabled ] || [ "$(jget wizard_before)" != enabled ] || wiz=enabled   # we turned it off in an earlier run
      [ "$stk" != disabled ] || [ "$(jget stock_before)" != enabled ] || stk=enabled
      jwrite "$NONCE" switching "$STOCK" "$role" "$wiz" "$stk"
      "$PY" -I optimize/tv.py --push-b64 optimize/deadman.sh /data/local/tmp/deadman.sh || return 1
      printf '%s\n' \
        "${old_nonce:+touch /data/local/tmp/home_ok.$old_nonce}" \
        "rm -f /data/local/tmp/home_ok /data/local/tmp/home_ok.$NONCE" \
        "for f in /data/local/tmp/home_ok.* /data/local/tmp/home_pid.* /data/local/tmp/home_hb.*; do case \$f in *.$NONCE|*.${old_nonce:-none}|*'*') ;; *) rm -f \$f ;; esac; done" \
        "if command -v setsid > /dev/null 2>&1; then setsid sh /data/local/tmp/deadman.sh $STOCK 300 $NONCE < /dev/null > /dev/null 2>&1 & else nohup sh /data/local/tmp/deadman.sh $STOCK 300 $NONCE < /dev/null > /dev/null 2>&1 & fi" \
        | grep . | "$PY" -I optimize/tv.py --batch || return 1
      # alive = its pid answers and its heartbeat is under 25 s old (it writes one every 10 s)
      alive=no
      for _ in 1 2 3 4 5; do
        sleep 2
        res=$("$PY" -I optimize/tv.py "p=\$(cat /data/local/tmp/home_pid.$NONCE 2> /dev/null); h=\$(cat /data/local/tmp/home_hb.$NONCE 2> /dev/null); if [ -n \"\$p\" ] && kill -0 \$p 2> /dev/null && [ -n \"\$h\" ] && [ \$((\$(date +%s) - h)) -le 25 ]; then echo WATCHDOG_ALIVE; else echo WATCHDOG_DEAD; fi") || return 1
        if grep -q WATCHDOG_ALIVE <<< "$res"; then alive=yes; break; fi
      done
      if [ "$alive" = yes ]; then
        echo "Safety switch armed and running: the stock home comes back by itself in 5 minutes unless the new home works."
      else
        # Seen on a TCL with Android 14: a background job started over ADB ends the moment the ADB session closes (setsid and nohup
        # did not help), so there is no safety switch on the TV itself. The switch is still checked and undone from this computer
        # (below), and the journal restores it on the next run if this computer stops in the middle. Not an error: it is the
        # protection every earlier install had.
        jsafety unavailable
        echo "This TV ends background jobs when the ADB session closes, so there is no safety switch on the TV itself. The switch is checked and undone from this computer, and the journal restores it on the next run."
      fi

      # 3. Switch: the HOME role (Android 10+; the classic set-home-activity for older sets and as a backup), then
      #    the setup app off, then the stock home (this order avoided the black screen).
      cmds=("cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home"
            "cmd package set-home-activity --user 0 sa.urtv.home/.MainActivity")
      [ "$wraith" != listed ] || cmds+=("pm disable-user --user 0 $WRAITH")
      cmds+=("pm disable-user --user 0 ${STOCK%%/*}"
             "cmd package set-home-activity --user 0 sa.urtv.home/.MainActivity"
             "am force-stop com.android.tv.settings"
             "input keyevent KEYCODE_HOME")
      stepout=$(printf '%s\n' "${cmds[@]}" | "$PY" -I optimize/tv.py --batch) || return 1
      optimize/settle.sh mark 2> /dev/null || true   # packages were disabled: Android writes the state ~10 s later
      printf '%s\n' "$stepout"
      # Nokia Streaming Box 8000 answers «This command is illegal» to pm disable-user for its own launcher: no switch there.
      # Put the stock home back as it was and stop the switch (4); nothing else of the setup depends on it.
      if grep -A2 -F "\$ pm disable-user --user 0 ${STOCK%%/*}" <<< "$stepout" | grep -qi 'illegal'; then
        printf '%s\n' "cmd role add-role-holder --user 0 android.app.role.HOME ${STOCK%%/*}" \
          "cmd package set-home-activity --user 0 $STOCK" "pm enable com.google.android.tungsten.setupwraith" \
          "touch /data/local/tmp/home_ok.$NONCE" "input keyevent KEYCODE_HOME" | "$PY" -I optimize/tv.py --batch > /dev/null \
          || return 1   # the put-back did not go through (ADB dropped): the retry path decides, the journal stays «switching»
        jstate restored
        return 4
      fi
    fi
    # 4. Verify Home really opens the new launcher (a few tries: a slow TV), then disarm the switch.
    for _ in 1 2 3; do
      sleep 6
      top=$("$PY" -I optimize/tv.py "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'") || return 1
      if grep -q sa.urtv.home <<< "$top"; then ours=yes; break; fi
      "$PY" -I optimize/tv.py "input keyevent KEYCODE_HOME" > /dev/null || return 1
      # it took Home back after the switch: now it has to go (only if that cannot cost the Wi-Fi)
      if [ "$wraith" = no ] && wraith_answers_home; then wraith=listed; wraith_off || true; fi
    done
    if [ "$ours" != yes ]; then
      # An on-TV dialog may be waiting for the person (a Home chooser, a role request): wait for the answer before anything is undone.
      if dialog_open "$top"; then
        echo "An on-TV dialog is open ($(sed 's/^ *//' <<< "$top" | head -c 120)). Ask the person, in Arabic, to choose «تلفزيونك» then «دائماً» or «السماح» with the remote. Waiting up to 40 seconds for the answer."
        for _ in 1 2 3 4 5 6 7 8; do
          sleep 5
          top=$("$PY" -I optimize/tv.py "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'") || return 1
          if grep -q sa.urtv.home <<< "$top"; then ours=yes; break; fi
          dialog_open "$top" || break
        done
      fi
    fi
    if [ "$ours" != yes ]; then
      # One more try before the stock home goes back: the role and the home activity again, then Home (some firmware only takes
      # it at the second ask).
      echo "Home did not open the new launcher: asking once more."
      printf '%s\n' "cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home" \
        "cmd package set-home-activity --user 0 sa.urtv.home/.MainActivity" "input keyevent KEYCODE_HOME" | "$PY" -I optimize/tv.py --batch > /dev/null || return 1
      for _ in 1 2; do
        sleep 6
        top=$("$PY" -I optimize/tv.py "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'") || return 1
        if grep -q sa.urtv.home <<< "$top"; then ours=yes; break; fi
        "$PY" -I optimize/tv.py "input keyevent KEYCODE_HOME" > /dev/null || return 1
      done
    fi
    if [ "$ours" != yes ]; then
      home_diag   # the reason goes into the message below
      # the stock home back now, not in 5 minutes: a restart in between would stop the safety switch and could leave the
      # wizard's recovery screen as Home (seen on a TCL G07 with the stock home off and the wizard on)
      printf '%s\n' "pm enable ${STOCK%%/*}" "pm enable $WRAITH" \
        "cmd role add-role-holder --user 0 android.app.role.HOME ${STOCK%%/*}" "cmd package set-home-activity --user 0 $STOCK" \
        "touch /data/local/tmp/home_ok.$NONCE" "input keyevent KEYCODE_HOME" | "$PY" -I optimize/tv.py --batch > /dev/null \
        || return 1   # the stock home is NOT confirmed back (ADB dropped): the retry path decides; never say «put back» then
      jstate restored
      # the setup wizard had to stay on (it owns the Wi-Fi password) and Home still goes to the stock home: this TV keeps its own home
      [ "$WIZARD_KEPT" = yes ] && return 6
      return 2
    fi
    "$PY" -I optimize/tv.py "touch /data/local/tmp/home_ok.$NONCE" || return 1
    # the switch may have fired already if ADB was very slow: then the stock home is back
    # an empty answer is «the TV did not say», not «Home is not ours»: asked again; only a real answer that is not ours means the switch fired
    res=""; for n in 1 2 3; do
      res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null | tr -d '\r' | grep -E '^[A-Za-z0-9_.]+/' | tail -n 1) || res=""
      [ -z "$res" ] || break
      sleep 3
    done
    if [ -z "$res" ]; then   # still nothing: the journal stays «switching» (not «restored»), so a later run or restore-home.sh looks again
      echo "!! The TV did not say what Home is now; the safety switch is already off. Press Home on the remote; if it is not the new launcher, run optimize/restore-home.sh."
      return 10   # its own code: ADB is up (it answered the touch), so this is not «ADB dropped» and nothing is retried
    fi
    grep -q sa.urtv.home <<< "$res" || { jstate restored; return 3; }   # the safety switch fired: the stock home is back
    jstate done
  }


  # Home is already ours and no switch was cut off: there is nothing to switch and nothing to protect (no safety switch, no journal)
  already=no
  res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null) || res=""
  if grep -q sa.urtv.home <<< "$res" && [ "$(jget state)" != switching ]; then
    already=yes
    echo "Home is already the new launcher: nothing to switch, so no safety switch is started."
  fi
  rc=0; if [ "$already" = yes ]; then :; elif [ "$stock_unreadable" = yes ]; then rc=8; elif [ -z "$STOCK" ]; then rc=7; else switch_home || rc=$?; fi
  if [ "$rc" = 1 ]; then   # ADB dropped: wait for the TV, see what Home is now, and go on or start the switch again
    back=no; last_hint=""
    for n in $(seq 60); do   # every 10 s, 10 minutes; the same address and port as before (tv.py keeps the remembered one)
      if "$PY" -I optimize/tv.py "getprop ro.serialno" > /dev/null 2>&1; then back=yes; break; fi
      # every minute: if another device on the Wi-Fi now has ADB open, say so. Only a hint: the TV is never switched by itself.
      if [ $((n % 6)) = 0 ]; then
        hint=$("$PY" -I optimize/tv.py --address-hint 2> /dev/null || true)
        if [ -n "$hint" ] && [ "$hint" != "$last_hint" ]; then echo "$hint"; last_hint=$hint; fi
      fi
      sleep 10
    done
    if [ "$back" = no ]; then
      echo "!! The TV did not come back within 10 minutes. Check it is on and on Wi-Fi: the safety switch restores the stock home."
      echo "   Then run the same command again."
      exit 1
    fi
    res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null) || res=""
    rc=0
    if grep -q sa.urtv.home <<< "$res"; then
      echo "ADB dropped during the switch; the TV is back and the new home is already set: checking it"
      switch_home verify || rc=$?
    else
      echo "ADB dropped during the switch; the TV is back, trying the switch again"
      switch_home || rc=$?   # arms the switch again first
    fi
  fi
  home_skipped=0
  case "$rc" in
    0) ;;
    4) home_skipped=1
       echo "Home switch skipped: this TV refuses to turn its own home screen off («This command is illegal», seen on a Nokia Streaming Box 8000). It keeps its own home; the stock home is back as it was. The screensaver and the rest of the setup work without it."
       mkdir -p "${URTV_RUN_DIR:-optimize/run}"
       echo "Home switch: skipped, this TV refuses to turn its own home screen off" > "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt" ;;
    10) exit 1 ;;   # the line was printed where it happened
    9) echo "!! Could not tell which app is the TV's home now, so the home was NOT switched (undoing it could not put the right one back). Run the same command again."
       exit 1 ;;
    8) echo "!! Could not read this TV's package list (it came back empty, or without the android package), so the home switch was not made and nothing was changed. Run the same command again."
       exit 1 ;;
    7) home_skipped=1
       echo "!! Could not tell which app is this TV's own home screen (the Google TV and Android TV launchers are not installed, and no single other app answers Home), so the home switch was not made and nothing was changed: the stock home could not be put back if the switch failed. UrTV works as an app and as the screensaver."
       mkdir -p "${URTV_RUN_DIR:-optimize/run}"
       echo "Home switch: skipped, could not tell which app is this TV's own home screen" > "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt" ;;
    2|6) home_skipped=1   # every way a TV keeps its own home ends here: ONE plain result that is not an error, so nobody runs the launcher again and again
       if [ "$WIZARD_KEPT" = yes ]; then why="Its setup wizard must stay on to keep the saved Wi-Fi password, and Home still goes to the stock home"
       else why="Home did not open the new launcher after the role and the home activity were asked twice (some firmware pins Home to its own launcher)"; fi
       echo "Home switch skipped: this TV keeps its own home screen. $why ($DIAG). UrTV works as an app and as the screensaver; the stock home is back as it was. This is not an error, and trying again will not change it."
       echo "للشخص: تلفزيونك ما صار الشاشة الرئيسية في هذا التلفزيون، لكنه يشتغل من أيقونة «تلفزيونك» في صف التطبيقات."
       mkdir -p "${URTV_RUN_DIR:-optimize/run}"
       echo "Home switch: skipped, this TV keeps its own home screen (open UrTV from its «تلفزيونك» tile in the apps row)" > "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt" ;;
    *) echo "!! The safety switch put the stock home back (ADB was too slow). Nothing is broken: run the same command again."
       exit 1 ;;
  esac
  if [ "$home_skipped" = 0 ]; then
    # 5. Only now: the stock home's helpers are dead weight (restore.sh brings them back).
    grep -vE '^[[:space:]]*(#|$)' optimize/lists/home-replaced.txt | awk '{print "pm disable-user --user 0 " $1}' \
      | "$PY" -I optimize/tv.py --batch > /dev/null || true
    optimize/settle.sh mark 2> /dev/null || true
    rm -f "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt"
    echo "Launcher is home. Press Home from inside an app to double-check."
  fi
  echo "Undo everything: optimize/reset.sh"
fi

# The report's «Versions:» line was written by run.sh before this install: say what is on the TV now, so run.sh's report
# and share.sh agree. (Only when the report exists; no report, nothing to correct.)
if [ "$what" != extras ] && [ -f "${URTV_RUN_DIR:-optimize/run}/report.md" ]; then
  rep="${URTV_RUN_DIR:-optimize/run}/report.md"
  # the name (4.0) and, from versionCode, the build: «4.0 (build 151)» (the report must say which build a TV runs)
  appver() {
    local o n b
    o=$("$PY" -I optimize/tv.py "dumpsys package $1 2> /dev/null | grep -e versionName -e versionCode= | sed -n 1,2p" 2> /dev/null | tr -d '\r') || o=""
    n=$(sed -n 's/.*versionName=//p' <<< "$o" | sed -n 1p | tr -d ' ')
    b=$(sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' <<< "$o" | sed -n 1p)
    [ -z "$n" ] || n="$n${b:+ (build $b)}"
    printf '%s' "$n"
  }
  vs=$(appver sa.urtv.screensaver) || vs=""
  vh=$(appver sa.urtv.home) || vh=""
  if [ -n "$vs$vh" ]; then
    line="Versions: screensaver ${vs:-none}, home ${vh:-none}"
    if grep -q '^Versions:' "$rep"; then
      awk -v l="$line" '/^Versions:/ { print l; next } { print }' "$rep" > "$rep.tmp" && mv "$rep.tmp" "$rep"
    else
      printf '%s\n' "$line" >> "$rep"
    fi
  fi
fi
