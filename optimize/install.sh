#!/usr/bin/env bash
# Download the latest APKs from GitHub and install them on the TV.
#   optimize/install.sh saver      شاشة تلفزيونك: install + make it the screensaver
#   optimize/install.sh launcher   the home screen: install + make it the default Home
#                                  (it carries the «تلاوة» screens: the Quran recitations and the radio)
#   optimize/install.sh extras     two free apps from their own GitHub releases: TV Bro (a TV web browser,
#                                  github.com/truefedex/tv-bro) and SmartTube (YouTube without ads,
#                                  github.com/yuliskov/SmartTube), the build that fits the TV's processor
#   optimize/install.sh all        both (saver, launcher)
# The first install of the screensaver replaces an APK signed with another key: uninstall the old one first
# (pm uninstall sa.urtv.screensaver) — this script does that only if a plain install fails.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
REL=https://github.com/tshi-dev/UrTV/releases/download/${APK_TAG:-apk-latest}   # APK_TAG=v3.N: that numbered build
what="${1:?usage: optimize/install.sh saver|launcher|extras|all}"
case "$what" in saver|launcher|extras|all) ;; *) echo "usage: optimize/install.sh saver|launcher|extras|all" >&2; exit 2;; esac
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
  if [ -z "$verifier_old" ] || [ "$verifier_old" = null ]; then
    "$PY" -I optimize/tv.py "settings delete global $VERIFIER" > /dev/null 2>&1 || true
  else
    "$PY" -I optimize/tv.py "settings put global $VERIFIER $verifier_old" > /dev/null 2>&1 || true
  fi
}
trap restore_verifier EXIT

pm_install() {   # $1 file name in /data/local/tmp, $2 package: pm's answer. A big APK can install silently for over a
  # minute, which ADB takes for a dead TV: it runs in the background on the TV and is polled (tv.py --bg).
  # Some operator TVs have a system service that removes apps installed from ADB seconds later; installed with the
  # Play Store as the installer they stay. So: still there after 3 s? If not, install once more that way, and say so.
  local out
  out=$("$PY" -I optimize/tv.py --bg install "pm install -r /data/local/tmp/$1")
  if grep -q Success <<< "$out"; then
    sleep 3
    if ! "$PY" -I optimize/tv.py "pm list packages $2" | tr -d '\r' | grep -qx "package:$2"; then
      echo "$2 was removed right after the install (a service of this TV does that to apps installed over ADB): installing it again as if from the Play Store." >&2
      out=$("$PY" -I optimize/tv.py --bg install "pm install -i com.android.vending -r /data/local/tmp/$1")
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
  if ! curl_get -fsSL -o "optimize/$1" "$REL/$1"; then
    echo "!! Could not download $REL/$1 (tag ${APK_TAG:-apk-latest}): no such build, or no internet"; exit 1
  fi
  "$PY" -I optimize/tv.py --push-b64 "optimize/$1" "/data/local/tmp/$1"
  local out
  out=$(pm_install "$1" "$2")
  if grep -q INSTALL_FAILED_VERIFICATION_FAILURE <<< "$out"; then
    verifier_old=$("$PY" -I optimize/tv.py "settings get global $VERIFIER" | tr -d '\r\n' || true)
    echo "Play Protect refused the install over ADB: its ADB check is off for this install only, and put back right after (was: ${verifier_old:-not set})."
    echo "للشخص: لو طلعت على التلفزيون رسالة من Play Protect (التطبيق «غير آمن» أو «مبني لإصدار قديم»)، اضغط «تثبيت على أي حال» (Install anyway) بالريموت. هذا تطبيق تلفزيونك، والرسالة تطلع لأن الشاشة الرئيسية مبنية لإصدار أندرويد أقدم."
    verifier_set=1
    "$PY" -I optimize/tv.py "settings put global $VERIFIER 0" > /dev/null || true
    out=$(pm_install "$1" "$2")
  fi
  if ! grep -q Success <<< "$out"; then
    # only a signing-key clash justifies removing the installed app (and its settings); anything else stops here
    if grep -qE 'UPDATE_INCOMPATIBLE|signatures do not match' <<< "$out"; then
      echo "An older $2 signed with another key is installed: removing it, then installing again."
      # removing the home app while the stock one is off would leave the TV with no home at all
      [ "$2" = sa.urtv.home ] && "$PY" -I optimize/tv.py "pm enable ${STOCK%%/*}" > /dev/null || true
      "$PY" -I optimize/tv.py "pm uninstall $2" || true
      out=$(pm_install "$1" "$2")
      grep -q Success <<< "$out" || { echo "!! Install of $1 failed again: $out"; exit 1; }
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

from_github() {   # $1 owner/repo, $2 package, $3 asset pattern ({abi} = the TV's processor types, best first)
  local abis url name out code body size
  # an extra that an earlier try already installed is not downloaded or uploaded again: a flaky Wi-Fi that dropped on the second
  # one resumes there, not from the first (an upload cut off half way is resumed by tv.py itself)
  if "$PY" -I optimize/tv.py "pm list packages $2" | tr -d '\r' | grep -qx "package:$2"; then
    echo "$2 is already on the TV: left as it is (it updates itself)."; return 0
  fi
  abis=$("$PY" -I optimize/tv.py "getprop ro.product.cpu.abilist" | tr -d '\r')
  body=$(curl_get -sSL -w '\n%{http_code}' "https://api.github.com/repos/$1/releases/latest") || body=$'\n000'
  code=${body##*$'\n'}; body=${body%$'\n'*}
  if [ "$code" = 403 ] || [ "$code" = 429 ]; then
    echo "!! GitHub rate limit, try later: $1 skipped"; failed=1; return 0
  elif [ "$code" != 200 ]; then
    echo "!! GitHub did not list the releases of $1 (HTTP $code): skipped"; failed=1; return 0
  fi
  url=$("$PY" -I -c '
import json, re, sys
assets = {a["name"]: (a["browser_download_url"], a.get("size", 0)) for a in json.load(sys.stdin)["assets"]}
for abi in [a for a in sys.argv[2].split(",") if a] + ["universal", ""]:
    pat = re.compile(sys.argv[1].replace("{abi}", re.escape(abi)) + "$")
    hit = sorted(n for n in assets if pat.search(n))
    if hit:
        print(assets[hit[0]][0], assets[hit[0]][1]); break
' "$3" "$abis" <<< "$body") || true
  if [ -z "$url" ]; then echo "!! No build of $1 for this TV ($abis): skipped"; return 0; fi
  size=${url##* }; url=${url% *}
  name=$(basename "$url")
  if [ -f "optimize/$name" ] && [ "$size" != 0 ] && [ "$(wc -c < "optimize/$name" | tr -d ' ')" = "$size" ]; then
    echo "$name is already downloaded: not downloading it again."
  elif ! curl_get -fsSL -o "optimize/$name" "$url"; then echo "!! Could not download $url"; failed=1; return 0; fi
  "$PY" -I optimize/tv.py --push-b64 "optimize/$name" "/data/local/tmp/$name"
  out=$(pm_install "$name" "$2")
  "$PY" -I optimize/tv.py "rm -f /data/local/tmp/$name" > /dev/null
  rm -f "optimize/$name"
  if grep -q Success <<< "$out"; then echo "Installed $name"
  elif grep -qE 'UPDATE_INCOMPATIBLE|signatures do not match' <<< "$out"; then
    echo "$2 is already installed from somewhere else: left as it is (with its settings)."   # never removed
  else echo "!! Install of $name failed: $out"; failed=1; fi
}

if [ "$what" = extras ]; then
  from_github truefedex/tv-bro com.phlox.tvwebbrowser 'tvbro-[0-9.]+-generic-geckoExcluded\.apk'   # light: the TV's WebView
  from_github yuliskov/SmartTube org.smarttube.stable 'SmartTube_stable_[0-9.]+_{abi}\.apk'
  [ "$failed" = 0 ] || exit 1
fi

if [ "$what" = saver ] || [ "$what" = all ]; then
  push_install urtv-screensaver.apk sa.urtv.screensaver
  printf '%s\n' \
    "settings put secure screensaver_enabled 1" \
    "settings put secure screensaver_components sa.urtv.screensaver/.ScreenDream" \
    "settings put secure screensaver_default_component sa.urtv.screensaver/.ScreenDream" \
    "am start --activity-clear-task -n sa.urtv.screensaver/.SettingsActivity" \
    | "$PY" -I optimize/tv.py --batch
  echo "Screensaver installed and selected. Its settings screen is open on the TV."
fi

if [ "$what" = launcher ] || [ "$what" = all ]; then
  # Which stock home does this TV have? Google TV sets: launcherx. Older Android TV: tvlauncher.
  if "$PY" -I optimize/tv.py "pm list packages com.google.android.apps.tv.launcherx" | grep -q launcherx; then
    STOCK=com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity
  else
    STOCK=com.google.android.tvlauncher/.MainActivity
  fi
  echo "Stock home: $STOCK"

  # 1. The new home must be installed and working BEFORE anything is disabled.
  push_install urtv-home.apk sa.urtv.home
  "$PY" -I optimize/tv.py "pm grant sa.urtv.home android.permission.ACCESS_FINE_LOCATION" || true
  # exact alarms for the adhan (Android 12 and newer; older TVs need nothing)
  "$PY" -I optimize/tv.py "appops set sa.urtv.home SCHEDULE_EXACT_ALARM allow" || true
  # The launcher updates itself: without this it can download a build but Android never lets it install.
  "$PY" -I optimize/tv.py "appops set sa.urtv.home REQUEST_INSTALL_PACKAGES allow" || true

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
      other) "$PY" -I optimize/tv.py "pm disable-user --user 0 $WRAITH" > /dev/null || true; return 0 ;;
      owned) echo "The setup wizard of this TV owns its saved Wi-Fi network: left on, so the network is not lost." ;;
      *)     echo "Could not tell who owns the saved Wi-Fi network: the setup wizard is left on." ;;
    esac
    return 1
  }

  # Steps 2-4 as one function, so an ADB drop in the middle (TCL G08: Wi-Fi fell during the batch) can be retried.
  # "verify" as $1: only step 4 (the switch is armed already and the batch may have run).
  # Returns 0: the new home is on screen and the switch is disarmed; 1: tv.py failed (connection lost);
  # 2: Home did not open the new launcher; 3: the safety switch fired first.
  switch_home() {
    local top res stepout ours=no _ wraith=no cmds
    if [ "${1:-}" != verify ]; then
      # before anything is changed: does the wizard answer Home, and can it go without taking the Wi-Fi with it?
      if wraith_answers_home; then
        # owned: the switch goes on with the wizard left on (the HOME role and set-home-activity choose ours, and the
        # check below and the safety switch catch a TV where it still wins). Stopping here blocked 13 of 39 setups on
        # 51b9680, and forgetting the network did not help on half of them.
        case "$(wifi_owner)" in
          owned) echo "The setup wizard answers Home and owns the saved Wi-Fi network: it is left on (the network stays), and the switch goes on without turning it off." ;;
          other) wraith=listed ;;
          *) echo "The setup wizard answers Home, but the TV did not say who owns its saved Wi-Fi: the wizard is left on, so the network is not lost." ;;
        esac
      fi
      # 2. Arm the safety switch on the TV: it restores the stock home in 300 s unless we confirm (slow ADB needs time).
      "$PY" -I optimize/tv.py --push-b64 optimize/deadman.sh /data/local/tmp/deadman.sh || return 1
      printf '%s\n' \
        "rm -f /data/local/tmp/home_ok" \
        "nohup sh /data/local/tmp/deadman.sh $STOCK 300 > /dev/null 2>&1 &" \
        | "$PY" -I optimize/tv.py --batch || return 1
      echo "Safety switch armed: the stock home comes back by itself in 5 minutes unless the new home works."

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
      printf '%s\n' "$stepout"
      # Nokia Streaming Box 8000 answers «This command is illegal» to pm disable-user for its own launcher: no switch there.
      # Put the stock home back as it was and stop the switch (4); nothing else of the setup depends on it.
      if grep -A2 -F "\$ pm disable-user --user 0 ${STOCK%%/*}" <<< "$stepout" | grep -qi 'illegal'; then
        printf '%s\n' "cmd role add-role-holder --user 0 android.app.role.HOME ${STOCK%%/*}" \
          "cmd package set-home-activity --user 0 $STOCK" "pm enable com.google.android.tungsten.setupwraith" \
          "touch /data/local/tmp/home_ok" "input keyevent KEYCODE_HOME" | "$PY" -I optimize/tv.py --batch > /dev/null || true
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
    [ "$ours" = yes ] || return 2
    "$PY" -I optimize/tv.py "touch /data/local/tmp/home_ok" || return 1
    # the switch may have fired already if ADB was very slow: then the stock home is back
    res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN") || return 1
    grep -q sa.urtv.home <<< "$res" || return 3
  }

  rc=0; switch_home || rc=$?
  if [ "$rc" = 1 ]; then   # ADB dropped: wait for the TV, see what Home is now, and go on or start the switch again
    back=no
    for _ in $(seq 36); do   # every 10 s, about 6 minutes
      if "$PY" -I optimize/tv.py "getprop ro.serialno" > /dev/null 2>&1; then back=yes; break; fi
      sleep 10
    done
    if [ "$back" = no ]; then
      echo "!! The TV did not come back within 6 minutes. Check it is on and on Wi-Fi: the safety switch restores the stock home."
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
    5) echo "!! The setup wizard of this TV answers Home and owns its saved Wi-Fi network: turning it off would delete the network and take the TV off Wi-Fi. Nothing was changed."
       echo "   Tell the person, in Arabic: «على التلفزيون: الإعدادات ← الشبكة والإنترنت، انسَ شبكة الواي فاي (Forget) واتصل فيها من جديد، وقلّي «تم»» (the network then belongs to the system, not the wizard). Then the same command."
       exit 1 ;;
    4) home_skipped=1
       echo "Home switch skipped: this TV refuses to turn its own home screen off («This command is illegal», seen on a Nokia Streaming Box 8000). It keeps its own home; the stock home is back as it was. The screensaver and the rest of the setup work without it."
       mkdir -p "${URTV_RUN_DIR:-optimize/run}"
       echo "Home switch: skipped, this TV refuses to turn its own home screen off" > "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt" ;;
    2) echo "!! Home did not open the new launcher. Leave the TV alone: the stock home returns within 5 minutes."
       echo "   Some TCL firmware pins Home to the stock launcher; on those sets keep the stock home."
       echo "   If the line above said the setup wizard owns the Wi-Fi, keep the stock home too: never turn the wizard off by hand."
       exit 1 ;;
    *) echo "!! The safety switch put the stock home back (ADB was too slow). Nothing is broken: run the same command again."
       exit 1 ;;
  esac
  if [ "$home_skipped" = 0 ]; then
    # 5. Only now: the stock home's helpers are dead weight (restore.sh brings them back).
    grep -vE '^\s*(#|$)' optimize/lists/home-replaced.txt | awk '{print "pm disable-user --user 0 " $1}' \
      | "$PY" -I optimize/tv.py --batch > /dev/null || true
    rm -f "${URTV_RUN_DIR:-optimize/run}/home-skipped.txt"
    echo "Launcher is home. Press Home from inside an app to double-check."
  fi
  echo "Undo everything: optimize/reset.sh"
fi

# The report's «Versions:» line was written by run.sh before this install: say what is on the TV now, so run.sh's report
# and share.sh agree. (Only when the report exists; no report, nothing to correct.)
if [ "$what" != extras ] && [ -f "${URTV_RUN_DIR:-optimize/run}/report.md" ]; then
  rep="${URTV_RUN_DIR:-optimize/run}/report.md"
  vs=$("$PY" -I optimize/tv.py "dumpsys package sa.urtv.screensaver 2> /dev/null | grep -m1 versionName" 2> /dev/null | sed 's/.*=//' | tr -d '\r ') || vs=""
  vh=$("$PY" -I optimize/tv.py "dumpsys package sa.urtv.home 2> /dev/null | grep -m1 versionName" 2> /dev/null | sed 's/.*=//' | tr -d '\r ') || vh=""
  if [ -n "$vs$vh" ]; then
    line="Versions: screensaver ${vs:-none}, home ${vh:-none}"
    if grep -q '^Versions:' "$rep"; then
      awk -v l="$line" '/^Versions:/ { print l; next } { print }' "$rep" > "$rep.tmp" && mv "$rep.tmp" "$rep"
    else
      printf '%s\n' "$line" >> "$rep"
    fi
  fi
fi
