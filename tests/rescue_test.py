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
        src = [SETUP / "RescuePlan.java", SETUP / "HomeRole.java", SETUP / "WatchList.java", SETUP / "SetupLists.java", SETUP.parent / "ExtrasWords.java", ROOT / "tests/RescuePlanRun.java"]
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
check("in the TV's apps list (LAUNCHER and LEANBACK_LAUNCHER), no other screen there (4.4 R7: the fallback home is not exported, reached only by its alias, tests/rescue_home_test.py), asks the home only through our signature permission",
      "android.intent.category.LEANBACK_LAUNCHER" in man and man.count("android.intent.category.LAUNCHER") == 1 and man.count("android.intent.category.LEANBACK_LAUNCHER") == 1
      and man.count("<activity\n") == 2 and man.count("<activity-alias") == 1 and 'android:name="sa.urtv.home.setup.FallbackHomeActivity"\n            android:exported="false"' in man and 'android:protectionLevel="signature"' in man and '<package android:name="sa.urtv.home" />' in man)
check("one big button «رجّع التلفزيون كما كان», over the TV's own adb (127.0.0.1:5555), the plan's commands only, and it says what it did",
      'text("رجّع التلفزيون كما كان"' in act and 'AdbClient.connect("127.0.0.1", 5555' in act and "for (String c : p.commands) adb.run(c);" in act
      and "شغّلنا " in act and "زر الرئيسية يفتح الشاشة الأصلية الآن" in act and "أطفأنا «إضافات تلفزيونك»" in act)
check("the home answers its ledger (what our setup turned off) to our key only", 'case ExtrasWords.LEDGER:' in text(SETUP.parent / "ExtrasBridge.java") and "public static String ledger(Context c)" in text(SETUP / "SetupHook.java"))
wf = text(ROOT / ".github/workflows/android.yml")
check("CI builds it and publishes urtv-rescue.apk with the others", "cp android/rescue/build/outputs/apk/release/rescue-release.apk urtv-rescue.apk" in wf and wf.count("            urtv-rescue.apk") == 4)   # the artifact, apk-latest, the numbered release, apk-next
check("tests/android_compile.sh compiles it", "the rescue module compiles" in text(ROOT / "tests/android_compile.sh"))
# ---- the other ways back (4.4)
sa = text(SETUP / "SetupActivity.java"); info = text(SETUP / "RescueInfo.java")
start = sa[sa.index("private void buildStart("):sa.index('start = button(S.t("جهّز كل شي"));')]
check("«قبل ما نبدأ: لو صار شيء» on the setup's start screen, before «جهّز كل شي»: three steps to «إنقاذ تلفزيونك», a QR to docs/RESCUE.md, a photo asked for",
      "rescueFirst();" in start and "qrBeside(words, RescueInfo.DOC_URL, sa.urtv.home.Compact.QR_DP);" in sa and "QrCode.encodeLarge(url)" in sa and 'DOC_URL = "https://github.com/tshi-dev/UrTV/blob/master/docs/RESCUE.md"' in info
      and all(w in info for w in ("زر الإعدادات في الريموت ← التطبيقات ← Downloader ← فتح", "إنقاذ تلفزيونك", "رجّع التلفزيون كما كان", "صوّر هذه الشاشة بجوالك")))
check("the Downloader code is shown in Latin digits once there is one (until then the guide's link)", "RescueInfo.hasCode() ? S.tv(RescueInfo.STEP2_CODE, RescueInfo.DOWNLOADER_CODE)" in sa and 'matches("[0-9]{4,10}")' in info)
doc = text(ROOT / "docs/RESCUE.md")
check("docs/RESCUE.md: Arabic first, the rescue app through Downloader, the phone route (Bugjaeger / Remote ADB Shell) with ONE line, the TV-settings way, English after",
      doc.index("إنقاذ تلفزيونك") < doc.index("## English") and "Bugjaeger" in doc and "Remote ADB Shell" in doc and "urtv-rescue.apk" in doc
      and re.search(r"```\npm enable com\.google\.android\.apps\.tv\.launcherx; [^\n]*pm enable com\.google\.android\.tungsten\.setupwraith; [^\n]*cmd role add-role-holder --user 0 android\.app\.role\.HOME com\.google\.android\.apps\.tv\.launcherx; [^\n]*input keyevent KEYCODE_HOME\n```", doc)
      and "disable" not in doc.split("```")[1] and "uninstall" not in doc.split("```")[1])
order = [doc.index(w) for w in ("## ١. من التلفزيون وحده", "## ٢. على التلفزيون: «إنقاذ تلفزيونك»", "## ٣. من جوال أندرويد", "## ٤. من آيفون: iSH", "## ٥. من أي كمبيوتر فيه Chrome")]
check("owner 4.4: a route for every person, easiest first (the TV alone, the rescue app, Android phone, iPhone with iSH, any computer with Chrome via Tango), each saying what it needs, the same one line, English with all five",
      order == sorted(order) and order[-1] < doc.index("## English") and doc.count("يحتاج") >= 3 and "إظهار تطبيقات النظام" in doc and "apk add android-tools" in doc
      and "adb connect عنوان_التلفزيون:5555" in doc and "tangoapp.dev" in doc and "MIT" in doc and doc.count("```") == 2
      and all(w in doc.split("## English", 1)[1] for w in ("Show system apps", "iSH", "Tango", "USB data cable")))
boot, hook, main = text(SETUP / "SetupBootReceiver.java"), text(SETUP / "SetupHook.java"), text(SETUP.parent / "MainActivity.java")
check("the home's own check: two boots in a row where our home (the TV's Home) never came up and crashed: the next start offers «رجّع الواجهة الأصلية» first (nothing done by itself)",
      "homeGuard(app);" in boot and "BootGuard.onBoot(prev, Math.max(0, crashesSince(app, prev.bootAt))" in boot and "store.put(SetupHook.STOCK_OFFER" in boot
      and "boolean askStock = sa.urtv.home.setup.SetupHook.stockBackOffered(this);" in main and 'askStock ? S.t("رجّع الواجهة الأصلية")' in main
      and "Settings.ACTION_HOME_SETTINGS" in main and "clearStockBackOffer" in main and "BootGuard.homeUp(g)" in hook)
eng = text(SETUP / "SetupEngine.java")
home = eng[eng.index("    private Result homeSwitch() throws IOException {"):eng.index("    // ---- 4.4: the Home key, checked the person's way")]
check("the 5-minute safety switch is armed on every path of the switch before the role is touched, and the switch does not start without it",
      home.index("deadman.arm(SAFETY_SECONDS)") < home.index('sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF)') and "if (!deadman.arm(SAFETY_SECONDS))" in home
      and "SAFETY_SECONDS = 300" in eng.replace(" ", " "))
st = hook[hook.index("public static void stockTaken"):][:400]
check("4.4 review A2-F1: the stock-home bar clears HOME_WANTED and tells the add-on at once; reapplyIfLost waits while the offer is up (else the rescue is undone at the next boot)",
      'HOME_WANTED, ""' in st and "tellWatchNow(c)" in st and "SetupHook.stockTaken(this)" in main and "stockBackOffered(app) || SetupRecovery.pending(app)" in hook)
sys.exit(1 if failed else 0)

