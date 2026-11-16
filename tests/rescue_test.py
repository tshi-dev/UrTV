"""4.4 «إنقاذ تلفزيونك» (UrTV Rescue, android/rescue) and the other ways back with no computer: the plan on a JVM (tests/RescuePlanRun.java), then the app, CI, the
setup's «قبل ما نبدأ: لو صار شيء» screen, docs/RESCUE.md (the phone route) and the home's own check after a bad boot, read from the source.

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
        src = [SETUP / "RescuePlan.java", SETUP / "WatchList.java", SETUP / "SetupLists.java", SETUP.parent / "ExtrasWords.java", ROOT / "tests/RescuePlanRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, src)], capture_output=True, text=True)
        check("RescuePlan compiles on a plain JVM", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.RescuePlanRun"], capture_output=True, text=True).stdout
            for l in out.splitlines():
                if l.startswith(("ok ", "BAD ")): check("RescuePlan: " + l.split(" ", 1)[1], l.startswith("ok "))
            check("every RescuePlan case ran", "ALL PASS" in out, out[-300:])

plan, eng = text(SETUP / "RescuePlan.java"), text(SETUP / "SetupEngine.java")
for k in ("LAUNCHERX", "LAUNCHERX_HOME", "TVLAUNCHER", "TVLAUNCHER_HOME", "WRAITH"):
    m1 = re.search(r'\b' + k + r' = ([^,;]+)', plan); m2 = re.search(r'\b' + k + r' = ([^,;]+)', eng)
    check(f"RescuePlan.{k} is SetupEngine's", m1 and m2 and m1.group(1) == m2.group(1), (m1 and m1.group(1), m2 and m2.group(1)))
gradle, man = text(ROOT / "android/rescue/build.gradle"), text(ROOT / "android/rescue/src/main/AndroidManifest.xml")
act = text(ROOT / "android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java")
check("its own app (sa.urtv.rescue), our key, in the Gradle build, the setup's adb client and plan copied in (one copy of each)",
      "applicationId 'sa.urtv.rescue'" in gradle and "keyAlias 'urtv'" in gradle and "':rescue'" in text(ROOT / "android/settings.gradle")
      and all(f"'{f}'" in gradle for f in ("AdbClient.java", "AdbKeys.java", "RescuePlan.java")))
check("in the TV's apps list (LAUNCHER and LEANBACK_LAUNCHER), no other screen, asks the home only through our signature permission",
      "android.intent.category.LEANBACK_LAUNCHER" in man and man.count("<activity") == 1 and 'android:protectionLevel="signature"' in man and '<package android:name="sa.urtv.home" />' in man)
check("one big button «رجّع التلفزيون كما كان», over the TV's own adb (127.0.0.1:5555), the plan's commands only, and it says what it did",
      'text("رجّع التلفزيون كما كان"' in act and 'AdbClient.connect("127.0.0.1", 5555' in act and "for (String c : p.commands) adb.run(c);" in act
      and "شغّلنا " in act and "زر الرئيسية يفتح الشاشة الأصلية الآن" in act and "أطفأنا «إضافات تلفزيونك»" in act)
check("the home answers its ledger (what our setup turned off) to our key only", 'case ExtrasWords.LEDGER:' in text(SETUP.parent / "ExtrasBridge.java") and "public static String ledger(Context c)" in text(SETUP / "SetupHook.java"))
wf = text(ROOT / ".github/workflows/android.yml")
check("CI builds it and publishes urtv-rescue.apk with the others", "cp android/rescue/build/outputs/apk/release/rescue-release.apk urtv-rescue.apk" in wf and wf.count("            urtv-rescue.apk") == 4)   # the artifact, apk-latest, the numbered release, apk-next
check("tests/android_compile.sh compiles it", "the rescue module compiles" in text(ROOT / "tests/android_compile.sh"))
sys.exit(1 if failed else 0)
