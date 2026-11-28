"""4.3.3 «إنقاذ تلفزيونك» (UrTV Rescue, android/rescue): the plan on a JVM (tests/RescuePlanRun.java), then the app, the home's ledger provider and CI, read from the
source. It must undo what 4.3.3's setup does: the stock home, the setup wizard and its helpers (SetupEngine), and what the ledger and the community list turned off.

    python3 tests/rescue_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        src = [SETUP / "RescuePlan.java", SETUP / "SetupLists.java", ROOT / "tests/RescuePlanRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, src)], capture_output=True, text=True)
        check("RescuePlan compiles on a plain JVM", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.RescuePlanRun"], capture_output=True, text=True).stdout
            for l in out.splitlines():
                if l.startswith(("ok ", "BAD ")): check("RescuePlan: " + l.split(" ", 1)[1], l.startswith("ok "))
            check("every RescuePlan case ran", "ALL PASS" in out, out[-300:])

plan, eng, lists = text(SETUP / "RescuePlan.java"), text(SETUP / "SetupEngine.java"), text(SETUP / "SetupLists.java")
for k in ("LAUNCHERX", "LAUNCHERX_HOME", "TVLAUNCHER", "TVLAUNCHER_HOME", "WRAITH"):
    m1 = re.search(r'\b' + k + r' = ([^,;]+)', plan); m2 = re.search(r'\b' + k + r' = ([^,;]+)', eng)
    check(f"RescuePlan.{k} is SetupEngine's", m1 and m2 and m1.group(1) == m2.group(1), (m1 and m1.group(1), m2 and m2.group(1)))
check("it turns back on the stock home's helpers the setup turns off (SetupLists.HOME_HELPERS), and the setup turns off only those, the wizard and the ledger's names",
      "SetupLists.HOME_HELPERS" in plan and "for (String h : SetupLists.HOME_HELPERS) if (pk.contains(h)) sh.run(\"pm disable-user --user 0 \" + h)" in eng
      and 'sh.run("pm disable-user --user 0 " + WRAITH)' in eng and "pm disable-user --user 0 \" + stockPkg" in eng)
gradle, man = text(ROOT / "android/rescue/build.gradle"), text(ROOT / "android/rescue/src/main/AndroidManifest.xml")
act = text(ROOT / "android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java")
check("its own app (sa.urtv.rescue), our key, in the Gradle build, the setup's adb client and plan copied in (one copy of each)",
      "applicationId 'sa.urtv.rescue'" in gradle and "keyAlias 'urtv'" in gradle and "':rescue'" in text(ROOT / "android/settings.gradle")
      and all(f"'{f}'" in gradle for f in ("AdbClient.java", "AdbKeys.java", "RescuePlan.java")) and "sharedHome" not in gradle)
check("in the TV's apps list (LAUNCHER and LEANBACK_LAUNCHER), no other screen, asks the home only through our signature permission",
      "android.intent.category.LEANBACK_LAUNCHER" in man and man.count("<activity") == 1 and 'android:protectionLevel="signature"' in man and '<package android:name="sa.urtv.home" />' in man
      and "sa.urtv.permission.RESCUE" in man)
check("one big button «رجّع التلفزيون كما كان», over the TV's own adb (127.0.0.1:5555), the plan's commands only, and it says what it did; no extras, no accessibility",
      'text("رجّع التلفزيون كما كان"' in act and 'AdbClient.connect("127.0.0.1", 5555' in act and "for (String c : p.commands) adb.run(c);" in act and "شغّلنا " in act
      and "ExtrasWords" not in act and "accessibility" not in act.replace("DEV_HELP", ""))
home_man = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
prov = text(SETUP / "RescueLedger.java")
check("the home answers its ledger (disabled-by-us.txt and community-applied.txt) to our key only: a signature permission, read only, package names only",
      'android:name=".setup.RescueLedger"' in home_man and 'android:authorities="sa.urtv.home.rescue"' in home_man and 'android:permission="sa.urtv.permission.RESCUE"' in home_man
      and 'name="sa.urtv.permission.RESCUE" android:protectionLevel="signature"' in home_man and 'LEDGER_AUTHORITY = "sa.urtv.home.rescue"' in plan
      and "disabled-by-us.txt" in prov and "community-applied.txt" in prov and "ledgerNames" in prov and "return null; }" in prov)
wf = text(ROOT / ".github/workflows/android.yml")
check("CI builds it and publishes urtv-rescue.apk with the others in the artifact and the apk-latest and numbered releases (not apk-next), never urtv-extras.apk",
      "cp android/rescue/build/outputs/apk/release/rescue-release.apk urtv-rescue.apk" in wf and wf.count("            urtv-rescue.apk") == 4 and "urtv-extras.apk" not in wf
      and "urtv-rescue.apk" not in wf.split("tag_name: apk-next")[1].split("tag_name: apk-437rc")[0])   # (4.3.7: the release candidate carries it too)
check("tests/android_compile.sh compiles it", "the rescue module compiles" in text(ROOT / "tests/android_compile.sh"))
doc = text(ROOT / "docs/RESCUE.md")
check("docs/RESCUE.md names the fixed link apk-latest/urtv-rescue.apk", "releases/download/apk-latest/urtv-rescue.apk" in doc)
sys.exit(1 if failed else 0)
