"""optimize/install.sh launcher when ADB drops during the switch: a stub tv.py, no TV needed.

    python3 tests/install_retry_test.py

Each case copies install.sh into a scratch tree, puts a fake `python` (PY), `curl` and `sleep` first, and checks
what the script printed, how it ended and how often it armed the safety switch.
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

STUB_PY = r'''#!/usr/bin/env bash
# fake: python -I optimize/tv.py <args>; state files live in $T
shift 2
log() { echo "$*" >> "$T/log"; }
step() { sed -n 's/^  "step": "\([^"]*\)".*/\1/p' "$J" 2> /dev/null; }   # what the journal says is about to act, at this moment
[ "$1" = --bg ] && { shift 2; set -- "$*"; case "$1" in "pm install"*) log bg ;; esac; }   # --bg NAME 'cmd': runs on the TV in the background
case "$1" in
  --push-b64) exit 0 ;;
  --batch)
    in=$(cat)
    case "$in" in
      *nohup*) log arm; rm -f "$T/home_ok"
        case "$in" in *"rm -f /data/local/tmp/home_ok /data/local/tmp/home_ok."*"command -v setsid"*"setsid sh /data/local/tmp/deadman.sh"*"< /dev/null"*"else nohup sh"*"< /dev/null"*) log armline-ok ;; esac
        echo "$in" | sed -n 's/.*deadman.sh [^ ]* [0-9]* \([^ ]*\) .*/\1/p' > "$T/nonce"
        case "$in" in *"touch /data/local/tmp/home_ok."*) log supersede ;; esac ;;
      *"am force-stop com.android.tv.settings"*)
        log switch; log "switch@$(step)"; rm -f "$T/front"
        case "$in" in "pm disable-user --user 0 com.google.android.apps.tv.launcherx"*"add-role-holder --user 0 android.app.role.HOME sa.urtv.home"*) log stock-before-role ;; esac
        case "$in" in *"disable-user --user 0 com.google.android.tungsten.setupwraith"*) log wraith-off ;; esac
        n=$(( $(cat "$T/switches" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/switches"
        if [ "$ILLEGAL" = stock ]; then   # Nokia Streaming Box 8000: the stock home cannot be turned off
          log illegal; printf '%s\n' '$ cmd role add-role-holder x' 'ok' '$ pm disable-user --user 0 com.google.android.apps.tv.launcherx' 'This command is illegal' '$ input keyevent KEYCODE_HOME'
          exit 0
        fi
        if [ "$n" -le "$FAIL_SWITCHES" ]; then   # the connection drops in this batch
          [ "$FIRST_DROP" = ours ] && [ "$n" = 1 ] && echo ours > "$T/home"
          [ "$FIRST_DROP" = stock ] && [ "$n" = 1 ] && echo stock > "$T/home"
          log drop; exit 1
        fi
        [ "$NOHOME" = yes ] && { echo stock > "$T/home"; exit 0; }   # firmware or the wizard keeps Home
        [ "$NOHOME" = once ] && { echo stock > "$T/home"; exit 0; }   # Home only goes to ours at the second ask
        echo ours > "$T/home" ;;
      *"add-role-holder --user 0 android.app.role.HOME sa.urtv.home"*"input keyevent KEYCODE_HOME"*) log retry; [ "$NOHOME" = once ] && echo ours > "$T/home" ;;
      *"pm enable"*"touch /data/local/tmp/home_ok"*) [ "$RESTORE_FAIL" = yes ] && { log restore-fail; exit 1; }
        log restore-stock; echo stock > "$T/home"; rm -f "$T/front"
        case "$in" in *"pm enable com.google.android.tungsten.setupwraith"*) log wizard-on; rm -f "$T/wraith_off" ;; esac
        case "$in" in *"add-role-holder --user 0 android.app.role.HOME other.launcher"*) log holder-other ;; esac
        case "$in" in *"pm enable com.old.helper"*) log helpers ;; esac
        case "$in" in *"pm enable other.launcher"*) log enable-holder ;; esac ;;
      *) log other-batch ;;
    esac
    exit 0 ;;
esac
case "$1" in
  "pm list packages")   # PKGS=none: the TV answered nothing; STOCKLESS=yes: neither Google launcher is installed; PKGEXTRA: more packages
    [ "$PKGS" = none ] && exit 0
    echo "package:android"; echo "package:com.android.settings"
    [ "$STOCKLESS" = yes ] || echo "package:com.google.android.apps.tv.launcherx"
    for p in $PKGEXTRA; do echo "package:$p"; done ;;
  *launcherx*) echo "package:com.google.android.apps.tv.launcherx" ;;
  "settings get global verifier_verify_adb_installs") [ "$VERIFIER_EMPTY" = yes ] && exit 0; cat "$T/verifier" 2>/dev/null || echo null ;;
  "settings put global verifier_verify_adb_installs"*) log "put-${1##* }"; echo "${1##* }" > "$T/verifier" ;;
  "settings delete global verifier_verify_adb_installs") log delete; rm -f "$T/verifier" ;;
  "pm install -i com.android.vending"*) log vending; touch "$T/stays"; echo Success ;;
  "pm list packages sa.urtv."*)
    pkg=${1##* }
    if [ -n "$VANISH" ] && { [ "$VANISH" = always ] || [ ! -f "$T/stays" ]; }; then log gone; else echo "package:$pkg"; fi ;;
  "pm list packages com.tcl.tv") [ "$TCLTV" = yes ] && { log tcl-listed; echo "package:com.tcl.tv"; echo "package:com.tcl.tvinput"; } ;;
  "am force-stop com.tcl.tv") log force-stop ;;
  "pm uninstall"*) log uninstall ;;
  "pm install"*)
    [ "$INSTALL_MODE" = silent ] && { log silent; exit 1; }   # tv.py --bg gave up: no output at all
    [ "$INSTALL_MODE" = downgrade ] && { log downgrade; echo "Failure [INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected: Update version code 168 is older than current 174]"; exit 1; }
    [ "$INSTALL_MODE" = incompatible ] && { log incompatible; echo "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match]"; exit 1; }   # --bg now exits with pm's code
    if [ -n "$VERIFY" ] && { [ "$VERIFY" = always ] || [ "$(cat "$T/verifier" 2>/dev/null)" != 0 ]; }; then
      log refused; echo "Failure [INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed]"
    else log installed; echo Success; fi ;;
  "getprop ro.serialno")
    n=$(( $(cat "$T/polls" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/polls"
    [ "$n" -le "$DOWN_POLLS" ] && exit 1
    echo 0123456789 ;;
  "cmd package query-activities"*) echo "2 activities found:"; echo "  Activity #0:"; echo "    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true"   # the real format: components are indented
       [ "$WRAITH_HOME" = yes ] && echo "    com.google.android.tungsten.setupwraith/.RecoveryActivity"
       [ -n "$QUERY_EXTRA" ] && printf '    %s\n' $QUERY_EXTRA ;;
  "dumpsys wifi") log wifi-owner-read
       if [ "$WIFI" = moves ]; then   # the person forgets the network and joins it again: the wizard's for 3 reads, then the Settings'
         n=$(( $(cat "$T/wifireads" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/wifireads"
         [ "$n" -le 3 ] && echo "cname=com.google.android.tungsten.setupwraith" || echo "cname=android.uid.system"; exit 0
       fi
       case "$WIFI" in owned) echo "cname=com.google.android.tungsten.setupwraith" ;; other) echo "cname=android.uid.system" ;; esac ;;
  "pm disable-user --user 0 com.google.android.tungsten.setupwraith") log wraith-off; log "wraith-off@$(step)"; touch "$T/wraith_off"
       [ "$DROP_AT" = wizard ] && { log drop; exit 1; } ;;
  "cmd wifi status"*)   # read only. WIFI_READ=none: the TV says nothing about it; WIFI_AFTER=lost: the network is gone once the wizard is off
       log "wifi-read@$(step)"
       [ "$WIFI_READ" = none ] && { echo W_END; echo L_END; exit 0; }
       if [ "$WIFI_AFTER" = lost ] && [ -f "$T/wraith_off" ]; then echo "Wifi is enabled"; echo "Wifi is not connected"; echo W_END; echo "No networks"; echo L_END
       else echo "Wifi is enabled"; echo 'Wifi is connected to "x"'; echo W_END; echo "Network Id      SSID                         Security type"; echo "0            x                             wpa2-psk"; echo L_END; fi ;;
  "am start -W"*sa.urtv.home*) log "home-start@$(step)"
       [ "$DROP_AT" = start ] && { log drop; exit 1; }
       if [ "$START" = fail ]; then echo "Error: Activity not started, unable to resolve Intent"; else echo "Status: ok"; [ "$FRONT" = no ] || touch "$T/front"; fi ;;
  *resolve-activity*) [ "$RESOLVE" = empty ] && exit 0
       [ "$RESOLVE" = late ] && [ "$(cat "$T/switches" 2>/dev/null || echo 0)" -ge 2 ] && exit 0   # the TV says nothing once the switch gave up
       [ "$RESOLVE" = fired ] && ls "$T"/marker.* > /dev/null 2>&1 && { echo "com.google.android.apps.tv.launcherx/.home.HomeActivity"; exit 0; }   # the safety switch fired first
       [ "$RESOLVE" = after ] && ls "$T"/marker.* > /dev/null 2>&1 && exit 0
       [ "$RESOLVE" = chooser ] && { echo "android/com.android.internal.app.ResolverActivity"; exit 0; }
       [ "$RESOLVE" = stockpkg ] && ! ls "$T"/marker.* > /dev/null 2>&1 && { echo "com.google.android.apps.tv.launcherx/.home.HomeActivity"; exit 0; }
       [ "$(cat "$T/home" 2>/dev/null)" = ours ] && echo sa.urtv.home/.MainActivity || echo stock/.Home ;;
  "cmd role get-role-holders"*) echo "$ROLE" ;;
  "pm list packages -d; echo D_END") [ "$DISABLED" = none ] && exit 0; [ "$DISABLED" = stock ] && echo "package:com.google.android.apps.tv.launcherx"; echo D_END ;;
  "getprop ro.product.manufacturer; getprop ro.product.device; dumpsys package com.google.android.tungsten.setupwraith"*) log cast-read
       # CAST=yes: the wizard holds the cast provider; MAKER/DEVICE: who made it (a Chromecast by default; a TCL G07 has the provider too)
       echo "${MAKER:-Google}"; echo "${DEVICE:-sabrina}"
       [ "$CAST" = yes ] && echo "    com.google.android.tungsten.setupwraith/com.google.android.libraries.tv.setupwraith.libs.castinfo.CastInfoProvider" ;;
  "pm list packages -d") [ "$CAST_WIZ" = off ] && echo "package:com.google.android.tungsten.setupwraith" ;;
  *dumpsys*) n=$(( $(cat "$T/dumps" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/dumps"
    if [ "$DIALOG" = yes ] && [ "$n" -le 4 ]; then echo "mResumedActivity: ActivityRecord{x u0 android/com.android.internal.app.ResolverActivity t5}"
    elif [ "$DIALOG" = yes ]; then echo ours > "$T/home"; echo "mResumedActivity: sa.urtv.home"
    elif [ "$(cat "$T/home" 2>/dev/null)" = ours ] || [ -f "$T/front" ]; then echo "mResumedActivity: sa.urtv.home"
    else echo "mResumedActivity: ActivityRecord{x u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t3}"; fi ;;
  "touch /data/local/tmp/home_ok."*)
    n=${1##*.}
    if [ "$n" = "$(cat "$T/nonce" 2>/dev/null)" ]; then log disarm; else log disarm-wrong; fi
    touch "$T/marker.$n" ;;
  *WATCHDOG_ALIVE*) log hb-check; [ "$WATCHDOG" = dead ] && echo WATCHDOG_DEAD || echo WATCHDOG_ALIVE ;;
  "[ -f /data/local/tmp/home_ok."*) n=${1#*home_ok.}; n=${n%% *}; [ -f "$T/marker.$n" ] && echo MARKER ;;
  "cmd role get-role-holders"*) echo "$ROLE" ;;
esac
exit 0
'''

STUB_CURL = r'''#!/bin/sh
# fake curl; CURL_MODE=revoke: a Windows (Schannel) curl that fails until --ssl-revoke-best-effort is given
[ "$1" = -V ] && { echo "curl 8.4.0 (x86_64-pc-win32) Schannel"; exit 0; }
[ "$1" = --help ] && { echo "--ssl-revoke-best-effort"; exit 0; }
ok=0; for a in "$@"; do [ "$a" = --ssl-revoke-best-effort ] && ok=1; done
if [ "$CURL_MODE" = revoke ] && [ $ok = 0 ]; then
  echo "curl: (35) schannel: next InitializeSecurityContext failed: CRYPT_E_NO_REVOCATION_CHECK" >&2; exit 35
fi
echo x >> "$T/curls"
while [ $# -gt 0 ]; do [ "$1" = -o ] && : > "$2"; shift; done
'''


def run(name, fail_switches, first_drop, down_polls=0, verify="", verifier=None, curl_mode="", vanish="", illegal="", wraith_home="", wifi="", nohome="", install_mode="",
        watchdog="", restore_fail="", tcltv="", dialog="", verifier_empty="", pkgs="", stockless="", pkgextra="", query_extra="", seed=None, seed_home="", seed_marker=False, role="", resolve="", script="install.sh", args=("launcher",),
        seed_ask="", wifi_after="", wifi_read="", start="", front="", drop_at="", seed_step=False, disabled="", cast="", cast_wiz="", seed_cast="", wifi_wait=None, maker="", device=""):
    t = Path(tempfile.mkdtemp())
    tree, bin_ = t / "tree", t / "bin"
    (tree / "optimize" / "lists").mkdir(parents=True); bin_.mkdir()
    shutil.copy(ROOT / "optimize" / "install.sh", tree / "optimize")
    shutil.copy(ROOT / "optimize" / "deadman.sh", tree / "optimize")
    shutil.copy(ROOT / "optimize" / "home-journal.sh", tree / "optimize")
    (tree / "optimize" / "lists" / "home-replaced.txt").write_text("# none\n" if script == "install.sh" else "com.old.helper\n")
    shutil.copy(ROOT / "optimize" / "restore-home.sh", tree / "optimize")
    shutil.copy(ROOT / "optimize" / "restore.sh", tree / "optimize")
    if seed:   # a journal an earlier run left: (state, role holder before); seed_step: one of this version (with a step)
        (tree / "optimize" / "run").mkdir()
        (tree / "optimize" / "run" / "home-switch.json").write_text(
            '{\n  "nonce": "old1",\n  "state": "%s",\n' % seed[0] + ""
            + '  "stock": "com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity",\n'
            '  "role_holder_before": "%s",\n  "wizard_before": "enabled",\n  "stock_before": "enabled",\n  "written": "x"' % seed[1] + (',\n  "step": "stock_off"' if seed_step else "") + '\n}\n')
    if seed_ask:   # an earlier run asked the person to forget the Wi-Fi network and join it again
        (tree / "optimize" / "run").mkdir(exist_ok=True)
        (tree / "optimize" / "run" / "wifi-ask.txt").write_text(seed_ask + "\n")
    if seed_cast:   # the person's «البث من الجوال» choice (optimize/cast.sh)
        (tree / "optimize" / "run").mkdir(exist_ok=True)
        (tree / "optimize" / "run" / "cast.txt").write_text(seed_cast + "\n")
    if seed_home:
        (t / "home").write_text(seed_home + "\n")
    if seed_marker:
        (t / "marker.old1").write_text("")
    for f, body in (("py", STUB_PY), ("curl", STUB_CURL),
                    ("sleep", "#!/bin/sh\nexit 0\n")):
        (bin_ / f).write_text(body); (bin_ / f).chmod(0o755)
    if verifier is not None:
        (t / "verifier").write_text(verifier + "\n")
    env = dict(os.environ, QUERY_EXTRA=query_extra, PKGS=pkgs, STOCKLESS=stockless, PKGEXTRA=pkgextra, VERIFIER_EMPTY=verifier_empty, DIALOG=dialog, TCLTV=tcltv, RESTORE_FAIL=restore_fail, WATCHDOG=watchdog, ROLE=role, RESOLVE=resolve, INSTALL_MODE=install_mode, NOHOME=nohome, WRAITH_HOME=wraith_home, WIFI=wifi, ILLEGAL=illegal, VERIFY=verify, VANISH=vanish, CURL_MODE=curl_mode, PATH=f"{bin_}:{os.environ['PATH']}", PY=str(bin_ / "py"), T=str(t),
               FAIL_SWITCHES=str(fail_switches), FIRST_DROP=first_drop, DOWN_POLLS=str(down_polls),
               WIFI_AFTER=wifi_after, WIFI_READ=wifi_read, START=start, FRONT=front, DROP_AT=drop_at, DISABLED=disabled, CAST=cast, CAST_WIZ=cast_wiz, MAKER=maker, DEVICE=device, **({"URTV_WIFI_WAIT": wifi_wait} if wifi_wait is not None else {}), J=str(tree / "optimize" / "run" / "home-switch.json"))
    r = subprocess.run(["bash", str(tree / "optimize" / script), *args], env=env, capture_output=True, text=True)
    log = (t / "log").read_text().split() if (t / "log").exists() else []
    af = tree / "optimize" / "run" / "wifi-ask.txt"
    log.append("ask-" + ("changed" if af.exists() and "wifi-changed" in af.read_text() else "kept" if af.exists() else "none"))
    jf = tree / "optimize" / "run" / "home-switch.json"
    if jf.exists():
        log.append("rhb-" + [l for l in jf.read_text().splitlines() if '"role_holder_before"' in l][0].split('"')[3])
        log.append("safety-" + ([l for l in jf.read_text().splitlines() if '"safety_switch"' in l] or ['"x": "none"'])[0].split('"')[3])
        log.append("journal-" + [l for l in jf.read_text().splitlines() if '"state"' in l][0].split('"')[3])
    if (t / "verifier").exists():
        log.append("final-" + (t / "verifier").read_text().strip())
    shutil.rmtree(t)
    return r.returncode, r.stdout + r.stderr, log

AGAIN = "the TV is back, trying the switch again"
CHECK = "already set: checking it"
cases = [
    ("no drop", run("a", 0, ""), lambda c, o, l: c == 0 and "ADB dropped" not in o and l.count("arm") == 1 and l.count("bg") == 1 and l.count("installed") == 1),
    ("drop, stock home back: switch again", run("b", 1, "stock", down_polls=2),
     lambda c, o, l: c == 0 and AGAIN in o and l.count("arm") == 2 and l.count("drop") == 1 and "Launcher is home" in o),
    ("drop, new home already set: only verify", run("c", 1, "ours"),
     lambda c, o, l: c == 0 and CHECK in o and l.count("arm") == 1 and l.count("disarm") == 1),
    ("two drops, the stock home answers Home and is enabled, the journal still «switching»: «the stock home is back», NOT «the safety switch put it back»", run("d", 2, "stock", resolve="stockpkg"),
     lambda c, o, l: c == 1 and l.count("arm") == 2 and "The stock home is back: Home answers with com.google.android.apps.tv.launcherx and it is enabled" in o and "safety switch put" not in o),
    ("two drops, the stock home still DISABLED (the 4.1 report): never «put back»; it says the stock home is off and to run restore-home.sh", run("d2", 2, "stock", disabled="stock", resolve="stockpkg"),
     lambda c, o, l: c == 1 and "safety switch put" not in o and "stock home is back" not in o and "still turned OFF" in o and "optimize/restore-home.sh" in o),
    ("two drops and then the TV says nothing about its packages or Home: no claim, it says what is unknown", run("d4", 2, "stock", disabled="none", resolve="late"),
     lambda c, o, l: c == 1 and "safety switch put" not in o and "did not say enough" in o and "the stock home: unknown" in o and "Home: no answer" in o),
    ("the safety switch fired first (code 3): the stock home answers Home, is enabled, the journal says restored: then, and only then, «the safety switch put the stock home back»", run("d5", 0, "", resolve="fired"),
     lambda c, o, l: c == 1 and "!! The safety switch put the stock home back (ADB was too slow): Home answers with com.google.android.apps.tv.launcherx, it is enabled, and the journal says restored" in o),
    ("code 3 but the stock home is still disabled: not «put back»", run("d6", 0, "", resolve="fired", disabled="stock"),
     lambda c, o, l: c == 1 and "safety switch put" not in o and "still turned OFF" in o),
    ("Play Protect refuses: check off for the install, old value back", run("f", 0, "", verify="once", verifier="1"),
     lambda c, o, l: c == 0 and "Play Protect refused" in o and "(was: 1)" in o and l.count("refused") == 1
     and l.count("installed") == 1 and l[l.index("put-0"):].count("put-1") == 1 and l[-1] == "final-1"),
    ("Play Protect refuses, setting unset: deleted again", run("g", 0, "", verify="once"),
     lambda c, o, l: c == 0 and "(was: null)" in o and "delete" in l and "final-0" not in l and "final-1" not in l),
    ("Play Protect refuses twice: one retry, old value still restored", run("h", 0, "", verify="always", verifier="1"),
     lambda c, o, l: c == 1 and l.count("refused") == 2 and "Install of urtv-home.apk failed" in o and l[-1] == "final-1"),
    ("Windows revocation error: retried with --ssl-revoke-best-effort", run("j", 0, "", curl_mode="revoke"),
     lambda c, o, l: c == 0 and "without that check" in o and "Play Protect refused" not in o),
    ("the TV removes the app after a plain install: once more as the Play Store", run("k", 0, "", vanish="plain"),
     lambda c, o, l: c == 0 and "installing it again as if from the Play Store" in o and l.count("installed") == 1
     and l.count("vending") == 1 and l.count("gone") == 1 and "Launcher is home" in o),
    ("the TV removes it every time: one retry, then it says so", run("l", 0, "", vanish="always"),
     lambda c, o, l: c == 1 and l.count("vending") == 1 and "Install of urtv-home.apk failed" in o and "removed by this TV" in o),
    ("a normal install: no Play Store installer, no second install", run("m", 0, ""),
     lambda c, o, l: c == 0 and "vending" not in l and l.count("installed") == 1 and "Play Store" not in o),
    ("the TV refuses to turn its own home off («This command is illegal»): the switch is skipped and said", run("n", 0, "", illegal="stock"),
     lambda c, o, l: c == 0 and "Home switch skipped" in o and "Nokia" in o and "Launcher is home" not in o and l.count("illegal") == 1
     and "restore-stock" in l and "!!" not in o),
    ("Play Protect refuses: the Arabic words for the person, «تثبيت على أي حال»", run("o", 0, "", verify="once", verifier="1"),
     lambda c, o, l: c == 0 and "تثبيت على أي حال" in o),
    ("Play Protect refuses twice: tells the person what to press and not to switch the verifier off", run("p", 0, "", verify="always", verifier="1"),
     lambda c, o, l: c == 1 and "تثبيت على أي حال" in o and "package_verifier_enable" in o),
    ("the setup wizard does not answer Home: it is not turned off", run("q", 0, ""),
     lambda c, o, l: c == 0 and "wraith-off" not in l and "Launcher is home" in o),
    ("the wizard answers Home and the Wi-Fi is the system's: turned off", run("r", 0, "", wraith_home="yes", wifi="other"),
     lambda c, o, l: c == 0 and l.count("wraith-off") == 1 and "Launcher is home" in o),
    ("a Chromecast (the wizard holds the cast credentials) with the Wi-Fi the system's: the wizard stays on, our home is still made the Home, and it says why",
     run("cc1", 0, "", wraith_home="yes", wifi="other", cast="yes"),
     lambda c, o, l: c == 0 and "wraith-off" not in l and "Launcher is home" in o and "holds its cast credentials" in o and "cast-read" in l),
    ("a Chromecast whose wizard owns the Wi-Fi: no Wi-Fi pause (the wizard stays anyway), the wizard on, the switch goes on",
     run("cc2", 0, "", wraith_home="yes", wifi="owned", cast="yes"),
     lambda c, o, l: c == 0 and "Home switch paused" not in o and "ask-kept" not in l and "wraith-off" not in l and "Launcher is home" in o),
    ("a Chromecast where the wizard keeps Home: never turned off (not even by the verify step), the stock home back, ONE plain line naming the cast trade-off and dock.sh cast off",
     run("cc3", 0, "", wraith_home="yes", wifi="other", cast="yes", nohome="yes"),
     lambda c, o, l: c == 0 and "!!" not in o and "wraith-off" not in l and "keeps its own home screen" in o and "cast credentials" in o and "dock.sh cast off" in o and "restore-stock" in l),
    ("a Chromecast whose person chose our home first (cast.txt = off): the usual switch, the wizard off with its Wi-Fi guard",
     run("cc4", 0, "", wraith_home="yes", wifi="other", cast="yes", seed_cast="off"),
     lambda c, o, l: c == 0 and l.count("wraith-off") == 1 and "Launcher is home" in o and "holds its cast credentials" not in o),
    ("a Chromecast set up before (the wizard already off): it says so and how to bring casting back; nothing else changes",
     run("cc5", 0, "", wifi="other", cast="yes", cast_wiz="off"),
     lambda c, o, l: c == 0 and "it is off (an earlier setup turned it off)" in o and "dock.sh cast on" in o and "Launcher is home" in o),
    ("a TCL G07 whose wizard has the same cast provider (tester 4.2): NOT a Chromecast, the wizard off as before, nothing about casting",
     run("cc7", 0, "", wraith_home="yes", wifi="other", cast="yes", maker="TCL", device="g07"),
     lambda c, o, l: c == 0 and l.count("wraith-off") == 1 and "cast credentials" not in o and "Launcher is home" in o and "cast-read" in l),
    ("a Google TV Streamer told by its codename (kirkwood) when the maker reads otherwise: casting kept",
     run("cc8", 0, "", wraith_home="yes", wifi="other", cast="yes", maker="unknown", device="kirkwood"),
     lambda c, o, l: c == 0 and "wraith-off" not in l and "holds its cast credentials" in o),
    ("not a Chromecast: the cast check changes nothing (the wizard off as before)",
     run("cc6", 0, "", wraith_home="yes", wifi="other"),
     lambda c, o, l: c == 0 and l.count("wraith-off") == 1 and "cast credentials" not in o),
    ("the wizard owns the Wi-Fi, the person was asked and the network did not move: it stays on and the switch goes on without it, as before", run("s", 0, "", wraith_home="yes", wifi="owned", seed_ask="asked x"),
     lambda c, o, l: c == 0 and "wraith-off" not in l and "Launcher is home" in o and "stays on" in o and "was not forgotten and joined again" in o and "!!" not in o and "ask-none" in l),
    ("Home never opens ours and the wizard owns the Wi-Fi (it must stay on): the stock home is put back at once, and it says plainly that this TV keeps its own home (not an error)",
     run("u", 0, "", wraith_home="yes", wifi="owned", nohome="yes", seed_ask="asked x"),
     lambda c, o, l: c == 0 and "wraith-off" not in l and "restore-stock" in l and "keeps its own home screen" in o and "!!" not in o and "Launcher is home" not in o and "أيقونة «تلفزيونك»" in o),
    ("Home does not open ours the first time and does at the second ask: no rollback, the switch is confirmed",
     run("ua", 0, "", nohome="once"),
     lambda c, o, l: c == 0 and "asking once more" in o and "retry" in l and "restore-stock" not in l and "Launcher is home" in o and l[-1] == "journal-done"),
    ("Home never opens ours (no wizard in the way): ONE plain result that is not an error (exit 0, no !!), the reason (role holder, what Home resolves to, what is on screen) in it, two asks made, the stock home back, and the Arabic hint to the «تلفزيونك» tile",
     run("ub", 0, "", nohome="yes", role="sa.urtv.home"),
     lambda c, o, l: c == 0 and "!!" not in o and "Home switch skipped: this TV keeps its own home screen" in o and "role holder: sa.urtv.home" in o and "Home resolves to: stock/.Home" in o
     and "on screen after Home:" in o and "launcherx" in o and "retry" in l and "restore-stock" in l and "أيقونة «تلفزيونك»" in o and "Launcher is home" not in o and "not an error" in o),
    ("the wizard answers Home and the TV does not say who owns the Wi-Fi, and Home never opens ours: the same plain result (it used to be an error that said to run again)",
     run("ud", 0, "", wraith_home="yes", wifi="", nohome="yes"),
     lambda c, o, l: c == 0 and "!!" not in o and "wraith-off" not in l and "keeps its own home screen" in o and "أيقونة «تلفزيونك»" in o and "restore-stock" in l),
    ("an on-TV dialog is open: it says to choose with the remote, waits, and does not roll back when the person answers",
     run("uc", 0, "", dialog="yes"),
     lambda c, o, l: c == 0 and "An on-TV dialog is open" in o and "restore-stock" not in l and "Launcher is home" in o),
    ("the wizard answers Home but the TV does not say who owns the Wi-Fi: the wizard stays on", run("t", 0, "", wraith_home="yes", wifi=""),
     lambda c, o, l: c == 0 and "wraith-off" not in l),
    ("an app of the same name with another signature: never uninstalled, stops and says what is lost", run("u", 0, "", install_mode="incompatible"),
     lambda c, o, l: c == 1 and "uninstall" not in l and l.count("incompatible") == 1 and "Nothing was removed" in o
     and "settings, tasks and own adhan" in o and "Ask the person first" in o),
    ("the TV has a newer build than the tag's (174 over 168): an info line, no !!, nothing installed, the switch goes on (tester W3)",
     run("w3", 0, "", install_mode="downgrade"),
     lambda c, o, l: c == 0 and "!!" not in o and "already has a newer build" in o and l.count("downgrade") == 1 and "uninstall" not in l and "Launcher is home" in o),
    ("the TV never answers during the install: a clear line, exit 1", run("v", 0, "", install_mode="silent"),
     lambda c, o, l: c == 1 and "stopped answering while urtv-home.apk was installing" in o and "uninstall" not in l),
    ("no refusal: the setting is never touched", run("i", 0, "", verifier="1"),
     lambda c, o, l: c == 0 and "put-0" not in l and "Play Protect refused" not in o),
]
cases += [
    ("Play Protect refuses and the TV does not say what its ADB check is set to (empty answer): it is NOT switched off, a !! line says so, exit 1",
     run("pv", 0, "", verify="once", verifier_empty="yes"),
     lambda c, o, l: c == 1 and "!! Play Protect refused the install over ADB, but the TV did not say" in o and "put-0" not in l and "refused" in l),
    ("another maker's home (no Google launcher): the one other app that answers Home, in the REAL indented query-activities format, is the stock home",
     run("st1", 0, "", stockless="yes", pkgextra="com.acme.home", query_extra="com.acme.home/.Main"),
     lambda c, o, l: c == 0 and "Stock home: com.acme.home/.Main" in o and "arm" in l and "Launcher is home" in o),
    ("only a bare fallback home and the settings app answer Home: neither is taken for the stock home; the switch is skipped with a !! line, nothing changed",
     run("st2", 0, "", stockless="yes", pkgextra="com.android.tv.settings", query_extra="com.android.tv.settings/.FallbackHome com.android.settings/.Settings"),
     lambda c, o, l: c == 0 and "!! Could not tell which app is this TV's own home screen" in o and "arm" not in l and "switch" not in l and "Launcher is home" not in o),
    ("the TV answers nothing for its package list: not «no Google launcher» but «the TV did not say»: stops with a !! line, nothing switched",
     run("st3", 0, "", pkgs="none"),
     lambda c, o, l: c == 1 and "!! Could not read this TV's package list" in o and "arm" not in l and "switch" not in l),
    ("a TCL (com.tcl.tv is there): its TV app is stopped BEFORE the home app is installed over, once",
     run("tc", 0, "", tcltv="yes"),
     lambda c, o, l: c == 0 and l.count("force-stop") == 1 and l.index("force-stop") < l.index("installed")),
    ("not a TCL: nothing is force-stopped (com.tcl.tvinput alone does not count)", run("tn", 0, ""), lambda c, o, l: c == 0 and "force-stop" not in l),
    ("normal switch: the journal is written, the safety switch is checked alive, then the marker with ITS nonce disarms it",
     run("w", 0, ""),
     lambda c, o, l: c == 0 and l.count("hb-check") == 1 and l.index("hb-check") < l.index("switch") and l.count("disarm") == 1
     and "disarm-wrong" not in l and l[-1] == "journal-done" and "supersede" not in l),
    ("the safety switch cannot stay alive on the TV (no heartbeat, as on a TCL Android 14): a plain line, no !!, the switch goes on and is confirmed, the journal says so",
     run("x", 0, "", watchdog="dead"),
     lambda c, o, l: c == 0 and l.count("hb-check") == 5 and "no safety switch on the TV itself" in o and "!!" not in o and "switch" in l
     and "disarm" in l and "Launcher is home" in o and l[-1] == "journal-done" and "safety-unavailable" in l),
    ("a TV where the safety switch does stay alive: the journal says armed", run("xa", 0, ""), lambda c, o, l: c == 0 and "safety-armed" in l),
    ("setsid first (with nohup as the fallback), both with stdin closed, and the stale plain home_ok is removed when arming",
     run("xb", 0, ""), lambda c, o, l: c == 0 and "armline-ok" in l),
    ("a restart mid-switch (journal «switching», Home not ours, no marker): the stock home is put back from the journal first, then the switch goes on",
     run("y", 0, "", seed=("switching", "other.launcher"), seed_home="stock"),
     lambda c, o, l: c == 0 and "earlier home switch was cut off" in o and l.index("restore-stock") < l.index("arm") and "holder-other" in l
     and "supersede" in l and l.count("restore-stock") == 1 and "enable-holder" in l and l[-1] == "journal-done"),
    ("Home never opens ours and the put-back batch fails (ADB drops): the journal stays «switching», so the next run redoes it",
     run("af", 0, "", nohome="yes", restore_fail="yes"),
     lambda c, o, l: c == 1 and "restore-fail" in l and l[-1] == "journal-switching"),
    ("a switch that confirmed itself (its marker is on the TV): the journal is not acted on",
     run("z", 0, "", seed=("switching", ""), seed_home="stock", seed_marker=True),
     lambda c, o, l: c == 0 and "cut off" not in o and "restore-stock" not in l),
    ("Home is ours already (the interrupted switch got there): not undone, the install goes on and checks it",
     run("aa", 0, "", seed=("switching", ""), seed_home="ours"),
     lambda c, o, l: c == 0 and "cut off" not in o and "restore-stock" not in l),
    ("a finished switch (journal «done»), Home not ours: not undone by itself (a TV update is not a cut-off switch)",
     run("ab", 0, "", seed=("done", ""), seed_home="stock"),
     lambda c, o, l: c == 0 and "cut off" not in o and "restore-stock" not in l),
    ("the second switch keeps who held Home before the first one (ours is not the «before»)",
     run("ac", 0, "", seed=("done", "other.launcher"), seed_home="stock", role="sa.urtv.home", script="install.sh", args=("launcher",)),
     lambda c, o, l: c == 0 and "rhb-other.launcher" in l and "arm" in l),
    ("Home is already ours and nothing was cut off: no switch, no safety switch, no heartbeat check, and it says so",
     run("ag", 0, "", seed_home="ours"),
     lambda c, o, l: c == 0 and "nothing to switch" in o and "arm" not in l and "hb-check" not in l and "switch" not in l and "disarm" not in l and "Launcher is home" in o),
    ("Home is ours and the journal says done: the same, and the journal is left as it was",
     run("ah", 0, "", seed=("done", "other.launcher"), seed_home="ours"),
     lambda c, o, l: c == 0 and "nothing to switch" in o and "arm" not in l and l[-1] == "journal-done"),
    ("Home is ours but a switch was cut off (journal «switching»): it is not skipped, the switch is made and confirmed",
     run("ai", 0, "", seed=("switching", ""), seed_home="ours"),
     lambda c, o, l: c == 0 and "nothing to switch" not in o and "arm" in l and "disarm" in l and l[-1] == "journal-done"),
    ("no role holder named and Home cannot be read either: the switch is not made (undoing it could not put the right app back)",
     run("ra", 0, "", resolve="empty", role=""),
     lambda c, o, l: c == 1 and "!! Could not tell which app is the TV's home now" in o and "arm" not in l and "switch" not in l),
    ("no role holder named, but the stock home answers Home: it is the «before» in the journal",
     run("rb", 0, "", role="", resolve="stockpkg"),
     lambda c, o, l: c == 0 and "rhb-com.google.android.apps.tv.launcherx" in l and "arm" in l),
    ("no role holder named, and what answers Home is the chooser (android) or anything but the stock home: not recorded, a restore goes to the stock home",
     run("rd", 0, "", role="", resolve="chooser"),
     lambda c, o, l: "rhb-" in l and "rhb-android" not in l and "arm" in l),
    ("Home cannot be read after the switch (empty each time): not called «restored», the journal stays «switching», and it says so",
     run("rc", 0, "", resolve="after"),
     lambda c, o, l: c == 1 and "!! The TV did not say what Home is now" in o and "journal-restored" not in l and "trying the switch again" not in o and l.count("arm") == 1),
    ("restore-home.sh: from the journal, the role goes back to who held it, the helpers come back, the journal says restored, ADB is said",
     run("ad", 0, "", seed=("done", "other.launcher"), seed_home="ours", script="restore-home.sh", args=()),
     lambda c, o, l: c == 0 and "holder-other" in l and "helpers" in l and l[-1] == "journal-restored" and "needs ADB" in o.replace("this needs ADB", "needs ADB")),
    ("restore-home.sh without a journal: the way restore.sh does it, and says so",
     run("ae", 0, "", script="restore-home.sh", args=()),
     lambda c, o, l: c == 0 and "No journal" in o and "restore-stock" in l),
]

# The wizard owns the saved Wi-Fi: ask the person once (forget the network, join it again), then read the owner again.
ASK_AR = "انسَ شبكة الواي فاي من إعدادات التلفزيون ← الشبكة، وارجع اتصل فيها بنفس كلمة المرور"
SAFE = dict(wraith_home="yes", wifi="other", seed_ask="asked x")
STEPS_FIRST = ["wraith-off@wizard_off", "wifi-read@wifi_check", "home-start@home_start", "switch@stock_off"]
def in_order(l, items):
    try:
        idx = [l.index(i) for i in items]
    except ValueError:
        return False
    return idx == sorted(idx)
cases += [
    ("the wizard answers Home and owns the Wi-Fi, not asked yet: STOPS before anything is changed (nothing installed, no safety switch, no wizard off), exit 5, a plain line (no !!) and the Arabic words for the person, and it remembers it asked",
     run("w1", 0, "", wraith_home="yes", wifi="owned"),
     lambda c, o, l: c == 5 and "!!" not in o and "Home switch paused, nothing was changed" in o and ASK_AR in o and "«كمّل»" in o
     and "still the setup wizard's after 10 minutes" in o and l.count("wifi-owner-read") >= 40
     and "installed" not in l and "bg" not in l and "arm" not in l and "wraith-off" not in l and "switch" not in l and "ask-kept" in l and "rhb-" not in " ".join(l)),
    ("4.1 reports: the run WAITS for the person (no second run): the network moves while it waits, and it goes on by itself in the safe order",
     run("w1b", 0, "", wraith_home="yes", wifi="moves"),
     lambda c, o, l: c == 0 and "!!" not in o and "Home switch paused, nothing was changed" in o and ASK_AR in o and "Waiting up to 10 minutes" in o
     and "the home switch goes on now, in the safe order" in o and "After «كمّل»" not in o and in_order(l, ["arm", "hb-check"] + STEPS_FIRST + ["disarm"])
     and "Launcher is home" in o),
    ("URTV_WIFI_WAIT=0: no wait, the pause stops at once as before (exit 5)",
     run("w1c", 0, "", wraith_home="yes", wifi="owned", wifi_wait="0"),
     lambda c, o, l: c == 5 and "Waiting up to" not in o and "After «كمّل»" in o and l.count("wifi-owner-read") == 1),
    ("the pause is said only once: asked before, the network still the wizard's: no second stop (it goes on as before)",
     run("w2", 0, "", wraith_home="yes", wifi="owned", seed_ask="asked x"),
     lambda c, o, l: c == 0 and "Home switch paused" not in o and "arm" in l and "wraith-off" not in l),
    ("asked, the network did not move, and Home stays with the stock home: as today, ONE plain line and the «تلفزيونك» tile, the wizard never off",
     run("w3", 0, "", wraith_home="yes", wifi="owned", seed_ask="asked x", nohome="yes"),
     lambda c, o, l: c == 0 and "!!" not in o and "wraith-off" not in l and "keeps its own home screen" in o and "أيقونة «تلفزيونك»" in o and "restore-stock" in l and "wizard-on" in l),
    ("the wizard does not own the Wi-Fi from the start (never asked): no pause, the usual switch (the wizard in the batch)",
     run("w4", 0, "", wraith_home="yes", wifi="other"),
     lambda c, o, l: c == 0 and "Home switch paused" not in o and "home-start@home_start" not in l and l.count("wraith-off") == 1 and "Launcher is home" in o),
    ("the wizard does not answer Home: no pause even if it owns the Wi-Fi (it is never turned off then)",
     run("w5", 0, "", wifi="owned"),
     lambda c, o, l: c == 0 and "Home switch paused" not in o and "wraith-off" not in l and "Launcher is home" in o),
    ("Home is already ours: no pause, nothing to switch", run("w6", 0, "", wraith_home="yes", wifi="owned", seed_home="ours"),
     lambda c, o, l: c == 0 and "Home switch paused" not in o and "nothing to switch" in o),
    ("the network moved off the wizard after the ask: the SAFE ORDER, the wizard off first, then the Wi-Fi checked, then our home started and in front, and only then the stock home off and the HOME role ours; confirmed, the ask forgotten",
     run("m1", 0, "", **SAFE),
     lambda c, o, l: c == 0 and "!!" not in o and in_order(l, ["arm", "hb-check"] + STEPS_FIRST + ["disarm"]) and l.count("wraith-off") == 1
     and "stock-before-role" in l and "restore-stock" not in l and "Launcher is home" in o and "the Wi-Fi stayed connected" in o and l[-1] == "journal-done" and "ask-none" in l),
    ("the safe order: the journal is written (and the safety switch armed) BEFORE the wizard goes, and names each step BEFORE it acts",
     run("m2", 0, "", **SAFE),
     lambda c, o, l: c == 0 and l.index("hb-check") < l.index("wraith-off") and all(x in l for x in STEPS_FIRST) and "wifi-read@armed" not in l
     and [x for x in l if x.startswith("wraith-off@")] == ["wraith-off@wizard_off"] and [x for x in l if x.startswith("switch@")] == ["switch@stock_off"]),
    ("the safe order: the Wi-Fi is not the same after the wizard went off: stopped, the wizard back ON and the stock home as it was (never turned off), a !! line, and the next run does not try this order again",
     run("m3", 0, "", wifi_after="lost", **SAFE),
     lambda c, o, l: c == 1 and "!! The Wi-Fi was not the same after the setup wizard was turned off" in o and "before: connected yes, saved networks 1; after: connected no, saved networks 0" in o and "wizard-on" in l and "restore-stock" in l
     and "home-start@home_start" not in l and "switch" not in l and l[-1] == "journal-restored" and "ask-changed" in l and "never touch the Wi-Fi yourself" in o),
    ("after a Wi-Fi change was seen once: the next run keeps the wizard on (as before), never the safe order",
     run("m4", 0, "", wraith_home="yes", wifi="other", seed_ask="asked x\nwifi-changed y"),
     lambda c, o, l: c == 0 and "wraith-off@wizard_off" not in l and "home-start@home_start" not in l),
    ("the safe order: our home does not come up in front: put back (the wizard ON, the stock home never off), ONE plain line with the tile, not an error",
     run("m5", 0, "", front="no", **SAFE),
     lambda c, o, l: c == 0 and "!!" not in o and "did not come up in front" in o and "أيقونة «تلفزيونك»" in o and "wizard-on" in l and "switch" not in l and l[-1] == "journal-restored"),
    ("the safe order: am start says Error: the same put-back", run("m6", 0, "", start="fail", **SAFE),
     lambda c, o, l: c == 0 and "did not come up in front" in o and "Error" in o and "wizard-on" in l and "switch" not in l),
    ("the safe order: Home never opens ours after the stock home went off: everything back, the wizard included, one plain line",
     run("m7", 0, "", nohome="yes", **SAFE),
     lambda c, o, l: c == 0 and "!!" not in o and "wraith-off@wizard_off" in l and "switch@stock_off" in l and "restore-stock" in l and "wizard-on" in l
     and "keeps its own home screen" in o and l[-1] == "journal-restored"),
    ("the safe order: ADB drops right after the wizard went off: when the TV is back the journal puts the wizard and the stock home back (not the switch half way), a !! line, exit 1",
     run("m8", 0, "", drop_at="wizard", **SAFE),
     lambda c, o, l: c == 1 and "ADB dropped during the safe order" in o and "«wizard_off»" in o and "wizard-on" in l and "restore-stock" in l
     and "home-start@home_start" not in l and "switch" not in l and l[-1] == "journal-restored"),
    ("the safe order: ADB drops while our home is started: the same, the stock home was never turned off",
     run("m9", 0, "", drop_at="start", **SAFE),
     lambda c, o, l: c == 1 and "«home_start»" in o and "wizard-on" in l and "switch" not in l and l[-1] == "journal-restored"),
    ("the safe order: the TV says nothing about its Wi-Fi: no safe order, the usual switch the way it was for a network that is not the wizard's",
     run("m10", 0, "", wifi_read="none", **SAFE),
     lambda c, o, l: c == 0 and "did not say what its Wi-Fi is now" in o and "home-start@home_start" not in l and l.count("wraith-off") == 1 and "Launcher is home" in o),
    ("restore-home.sh after a safe-order switch: the wizard is turned back on with the stock home",
     run("m11", 0, "", seed=("done", ""), seed_step=True, seed_home="ours", script="restore-home.sh", args=()),
     lambda c, o, l: c == 0 and "wizard-on" in l and "restore-stock" in l and l[-1] == "journal-restored"),
    ("a safe-order switch cut off by a restart (journal «switching», step stock_off, Home not ours): the next run puts the wizard and the stock home back first",
     run("m12", 0, "", seed=("switching", ""), seed_step=True, seed_home="stock", wraith_home="yes", wifi="other", seed_ask="asked x"),
     lambda c, o, l: c == 0 and "earlier home switch was cut off" in o and "wizard-on" in l and l.index("wizard-on") < l.index("arm")),
]
# run.sh: the pause at the launcher step is its own line, not «stopped», and run.sh ends with the same 5 (the next run.sh resumes there)
def run_finish(rc, current):
    src = (ROOT / "optimize" / "run.sh").read_text(encoding="utf-8")
    fn = src[src.index("finish() {"):src.index("trap finish EXIT")]
    d = tempfile.mkdtemp()
    script = f'LOCK={d}/lock; stamp() {{ echo "$*" >> {d}/steps; }}\n' + fn + f'current={current}\ntrap finish EXIT\nexit {rc}\n'
    r = subprocess.run(["bash", "-c", script], capture_output=True, text=True)
    steps = (Path(d) / "steps").read_text() if (Path(d) / "steps").exists() else ""
    shutil.rmtree(d)
    return r.returncode, r.stdout + r.stderr, steps.split()
# home_stop_line on its own (Home answering with ours cannot be reached through a whole run of the stub)
def stop_line(resolve_ans, disabled_ans, state):
    src = (ROOT / "optimize" / "install.sh").read_text(encoding="utf-8")
    fn = src[src.index("  home_stop_line() {"):src.index("  rc=0; if [ \"$already\" = yes ]")]
    d = Path(tempfile.mkdtemp())
    (d / "py").write_text('#!/bin/sh\nshift 2\ncase "$1" in *resolve-activity*) printf "%b" "$RA" ;; "pm list packages -d"*) printf "%b" "$DA" ;; esac\n'); (d / "py").chmod(0o755)
    script = f'PY={d}/py; STOCK=com.google.android.apps.tv.launcherx/x.Home; jget() {{ echo "{state}"; }}\n' + fn + 'home_stop_line 1\n'
    r = subprocess.run(["bash", "-c", script], capture_output=True, text=True, env=dict(os.environ, RA=resolve_ans, DA=disabled_ans))
    shutil.rmtree(d)
    return r.returncode, r.stdout + r.stderr, []
cases += [
    ("the stop line: Home answers with ours: says so and to run again, no «put back»", stop_line("sa.urtv.home/.MainActivity\\n", "package:com.google.android.apps.tv.launcherx\\nD_END\\n", "switching"),
     lambda c, o, l: "Home answers with the new launcher now" in o and "the stock home is disabled" in o and "safety switch put" not in o and "is back" not in o),
    ("the stop line: stock home answers and is enabled but the journal is «switching»: «is back», not «the safety switch put it back»", stop_line("com.google.android.apps.tv.launcherx/x.Home\\n", "D_END\\n", "switching"),
     lambda c, o, l: "The stock home is back" in o and "safety switch put" not in o),
    ("the stop line: all three agree: «the safety switch put the stock home back»", stop_line("com.google.android.apps.tv.launcherx/x.Home\\n", "D_END\\n", "restored"),
     lambda c, o, l: "!! The safety switch put the stock home back" in o),
    ("the stop line: journal restored and Home the stock's, but it is disabled: not «put back»", stop_line("com.google.android.apps.tv.launcherx/x.Home\\n", "package:com.google.android.apps.tv.launcherx\\nD_END\\n", "restored"),
     lambda c, o, l: "safety switch put" not in o and "still turned OFF" in o),
]
cases += [
    ("run.sh: install.sh paused at launcher (exit 5): «paused at launcher», waiting for «كمّل», no «stopped», exit 5", run_finish(5, "launcher"),
     lambda c, o, l: c == 5 and "run.sh: paused at launcher" in o and "«كمّل»" in o and "stopped" not in o and "paused" in l),
    ("run.sh: an exit 5 of another step is a plain stop", run_finish(5, "extras"),
     lambda c, o, l: c == 5 and "run.sh: stopped at extras" in o),
]
bad = [n for n, (c, o, l), ok in cases if not ok(c, o, l)]
for n, (c, o, l), ok in cases:
    print(f"install.sh launcher: {n}: " + ("ok" if ok(c, o, l) else f"WRONG (exit {c}, log {l})\n{o}"))
sys.exit(1 if bad else 0)
