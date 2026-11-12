#!/usr/bin/env python3
"""«تخلّص من واجهة المصنع» (its own row in the settings): the engine on SetupEngineRun's fake TV (the offer and its refusals, the step, the one-press restore: exactly the ledger and nothing
more) and the boot-failure counter (BootGuard), on a plain JVM; the screen's rules from the source (double confirmation, focus on «إلغاء», the PIN, the readable list, its own settings row).
Run: python3 tests/factory_test.py   (needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

act = (SETUP / "FactoryActivity.java").read_text(encoding="utf-8")
clean = (HOME / "CleanActivity.java").read_text(encoding="utf-8")
eng = (SETUP / "SetupEngine.java").read_text(encoding="utf-8")
recv = (SETUP / "SetupBootReceiver.java").read_text(encoding="utf-8")
sess = (SETUP / "SetupSession.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
mainsrc = (HOME / "MainActivity.java").read_text(encoding="utf-8")

check("the screen asks twice: a first screen and «متأكد؟», each with the focus on «إلغاء», and only «نعم، عطّل» runs the step (with the family PIN when one is set)",
      act.count('View cancel = button(S.t("إلغاء"), v -> finish());') == 2 and act.count("focus(cancel);") == 2 and 'button(S.t("نعم، عطّل"), v -> pin(this::run))' in act and 'S.t("متأكد؟")' in act
      and act.count("factoryOff()") == 1 and "PinGuard.confirm(this," in act)
check("one OK on the settings' row lands on a first page, not on the check: a plain description and two buttons, the focus on «رجوع»; nothing starts until «ابدأ»; Back always leaves",
      "else intro();" in act and "private void intro()" in act and 'View back = button(S.t("رجوع"), v -> finish());' in act.split("private void intro()")[1].split("// ----")[0] and 'button(S.t("ابدأ"), v -> check())' in act
      and "focus(back);" in act.split("private void intro()")[1].split("// ----")[0] and "check();" not in act.split("private void intro()")[0].split("onCreate")[1] and "public void onBackPressed() { finish(); }" in act)
check("it lists exactly what it will turn off, by readable name (the TV's label, else the package), says «تعطيل لا حذف», what stays protected and how to undo",
      "names(offer.packages)" in act and "getApplicationLabel" in act and "تعطيل لا حذف" in act and "يبقى محميًّا ولا نلمسه" in act and "للتراجع: «رجّع واجهة المصنع»" in act)
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
check("its own row in the settings next to «اجعل UrTV الشاشة الرئيسية», on or off clean mode: not on CleanActivity any more; the row turns into «رجّع واجهة المصنع» while it is on; the activity is exported only behind the signature permission",
      "FactoryActivity" not in clean and "setup.FactoryActivity" not in mainsrc and 'row("رجّع واجهة المصنع", "مفعّل' in saver
      and saver.index('"تخلّص من واجهة المصنع"') > saver.index('"اجعل UrTV الشاشة الرئيسية"') and saver.index('"تخلّص من واجهة المصنع"') < saver.index('"\\u2066UrTV\\u2069 النظيف"')
      and '"sa.urtv.home.setup.FactoryActivity"' in saver
      and re.search(r'android:name="\.setup\.FactoryActivity"\s+android:exported="true"\s+android:permission="sa\.urtv\.home\.OPEN_CLEAN"', man) is not None)
adb = (SETUP / "AdbClient.java").read_text(encoding="utf-8"); ses = (SETUP / "SetupSession.java").read_text(encoding="utf-8")
check("the check uses the very path the first-run setup uses (SetupHost.open: the screensaver held back while the TV asks, 3 minutes to answer, 127.0.0.1:5555, the one key file), not its own shorter one",
      "new SetupHost(this).open(180_000," in act.split("private void check()")[1].split("private String whyNot")[0] and 'AdbClient.connect("127.0.0.1", 5555,' in ses and "SetupSession.open(this, 60_000, () ->" not in act)
check("every step of the connection is logged under UrTVFactory (the key, the connect, the key offered, the answer, the time it took, the offer), and a failure says what to do next (disconnect any computer's adb, revoke authorizations, switch debugging off and on)",
      "static volatile Trace trace;" in adb and adb.count("note(") >= 8 and '"UrTVFactory"' in ses and "UrTVFactory" in act and "adb disconnect" in act and "إلغاء تصريحات تصحيح USB" in act and 'S.t("ما قدرنا نكلّم التلفزيون: تأكد من تفعيل تصحيح USB، وافصل' in act)
check("it refuses with the reason where debugging is off (the connect says so), and the engine refuses where the wizard owns the Wi-Fi or the safety switch cannot be set",
      "debugging is off" in act and "تصحيح USB مطفأ" in act and "wizardOwnsWifi()" in eng.split("FactoryOffer factoryOffer()")[1][:900] and "deadman.arm(30)" in eng.split("FactoryOffer factoryOffer()")[1][:1600])
check("the ADB key is kept while the step is on (its automatic restore needs it), the restore at boot is tried again at the next boot when it did not finish",
      'factory.startsWith("on")' in sess and "SetupBootReceiver" in recv and "restoreFactory()" in recv and "the next boot tries again" in recv)
check("the boot counter: a boot counts only when our home did not come up in it (MainActivity.onResume says so) and our process crashed in it (ApplicationExitInfo; older Android: no home is enough); two in a row restore",
      "SetupHook.homeUp(this)" in mainsrc and "getHistoricalProcessExitReasons" in recv and "REASON_CRASH" in recv and "BootGuard.onBoot(prev, crashesSince(app, prev.bootAt), now)" in recv)
if not shutil.which("javac"):
    print("SKIP factory logic: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [HOME / "phone/UpdateRules.java", HOME / "SuggestedApps.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/FactoryRun.java"), str(ROOT / "tests/BootGuardRun.java")], capture_output=True, text=True)
    check("the engine, the fake TV and the runners compile", r.returncode == 0, r.stderr[-800:])
    if r.returncode:
        sys.exit(1)
    fr = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.FactoryRun"], capture_output=True, text=True)
    out = {l.split(" ", 1)[0]: l.split(" ", 1)[1] for l in fr.stdout.splitlines() if " " in l}
    check("F1 a healthy TV: it may go on, the plan is the stock home first then the safe list (installed, not off already), nothing changed, the safety switch tried and let go",
          out.get("F1", "").startswith("true first=com.google.android.apps.tv.launcherx plan=true changed=0 armed=1 disarmed=1"), out.get("F1"))
    check("F2 the setup wizard owns the Wi-Fi: refused with the reason, nothing changed", out.get("F2") == "true changed=0", out.get("F2"))
    check("F3 the safety switch cannot be set: refused with the reason, nothing changed", out.get("F3") == "true changed=0", out.get("F3"))
    check("F4 the step: done, the stock home is off and in the ledger (written first), so is the safe list; no protected feature is touched; nothing is uninstalled; the mark says on",
          out.get("F4") == "DONE stockOff=true ledgerHasStock=true ledgerHasSafe=true keepTouched=false uninstall=0 mark=true isOff=true", out.get("F4"))
    check("F5 the one-press restore: the TV is as it was, Home is the stock one, the ledger is empty, the mark says off, and the only things enabled are the ledger, the stock home, the wizard and the home's helpers",
          out.get("F5") == "DONE same=true home=true ledgerEmpty=true enabledAllLedger=true extraEnabled=[] mark=true isOff=false", out.get("F5"))
    check("F6 the restore is not the full undo: nothing uninstalled, no setting or appop touched", out.get("F6") == "uninstall=0 settingsPut=0 appops=0", out.get("F6"))
    check("F7 a package the TV will not turn on stays in the ledger and the mark stays «on» for the next try", out.get("F7") == "FAILED ledger=true mark=true", out.get("F7"))
    check("the runner ended well", fr.returncode == 0, fr.stderr[-400:])
    br = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.BootGuardRun"], capture_output=True, text=True)
    b = {l.split(" ", 1)[0]: l.split(" ", 1)[1] for l in br.stdout.splitlines() if " " in l}
    check("boot counter: the first boot after switching on counts nothing; a boot with no home and a crash counts, the second one restores", b.get("b1") == "0 false false" and b.get("b2") == "1 false" and b.get("b3") == "2 true", (b.get("b1"), b.get("b2"), b.get("b3")))
    check("boot counter: a home that came up resets the count; a boot with no home and no crash (the TV started in an HDMI input) does not count; an Android that cannot say crashes counts a boot with no home",
          b.get("reset") == "0 false" and b.get("hdmi") == "0 false" and b.get("unknown") == "2 true", (b.get("reset"), b.get("hdmi"), b.get("unknown")))
    check("boot counter file: round trip; a damaged or missing file is clean (no false alarm)", b.get("file") == "1 false 123" and b.get("damaged") == "0 true" and b.get("none") == "true", (b.get("file"), b.get("damaged"), b.get("none")))
sys.exit(1 if failed else 0)
