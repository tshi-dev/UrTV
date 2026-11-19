"""4.3.3 «إضافات تلفزيونك» (UrTV Extras, android/extras): the optional add-on that holds the one accessibility service the home no longer has (Play Protect
blocked the home for them: «بيانات حساسة»). The Home watch on a JVM (tests/HomeWatchRun.java), then the wiring read from the source: the home has no
accessibility service, the two apps talk only through providers guarded by a signature permission, the offer opens only when a feature that needs the
add-on is asked for, the install checks our package and key, and CI builds and publishes urtv-extras.apk.

    python3 tests/extras_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
EXT = ROOT / "android/extras/src/main"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeWatch.java"), str(HOME / "ExtrasWords.java"), str(ROOT / "tests/HomeWatchRun.java")], capture_output=True, text=True)
        check("HomeWatch compiles on a plain JVM (no Android in it)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.HomeWatchRun"], capture_output=True, text=True).stdout
            for l in out.splitlines():
                if l.startswith(("ok ", "BAD ")): check("HomeWatch: " + l.split(" ", 1)[1], l.startswith("ok "))
            check("every HomeWatch case ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP the Java cases: no javac")

man, xman = text(ROOT / "android/launcher/src/main/AndroidManifest.xml"), text(EXT / "AndroidManifest.xml")
svc, prov = text(EXT / "java/sa/urtv/home/ExtrasA11yService.java"), text(EXT / "java/sa/urtv/home/ExtrasCallProvider.java")
words, ext, bridge, act = text(HOME / "ExtrasWords.java"), text(HOME / "Extras.java"), text(HOME / "ExtrasBridge.java"), text(HOME / "ExtrasActivity.java")
gradle = text(ROOT / "android/extras/build.gradle")

check("the home ships WITHOUT any accessibility service (no service, no xml, no BIND_ACCESSIBILITY_SERVICE)",
      "BIND_ACCESSIBILITY_SERVICE" not in man and "accessibilityservice" not in man and not (ROOT / "android/launcher/src/main/res/xml/type_service.xml").exists()
      and not (HOME / "TypeService.java").exists() and not (HOME / "SleepService.java").exists())
check("the add-on is its own app (sa.urtv.extras), same key as the others, in the Gradle build",
      "applicationId 'sa.urtv.extras'" in gradle and "keyAlias 'urtv'" in gradle and "':extras'" in text(ROOT / "android/settings.gradle"))
check("it has exactly one accessibility service, behind BIND_ACCESSIBILITY_SERVICE, and no internet",
      xman.count("BIND_ACCESSIBILITY_SERVICE") == 1 and "android.permission.INTERNET" not in xman and "<activity" not in xman)
perm = 'android:protectionLevel="signature"'
check("both apps declare sa.urtv.permission.EXTRAS as a signature permission, and each provider asks for it",
      perm in man and perm in xman and 'android:authorities="sa.urtv.extras.bridge"' in xman and 'android:authorities="sa.urtv.home.extras"' in man
      and re.search(r'android:name="sa\.urtv\.home\.ExtrasCallProvider"\s+android:authorities="sa\.urtv\.extras\.bridge"\s+android:exported="true"\s+android:permission="sa\.urtv\.permission\.EXTRAS"', xman)
      and re.search(r'android:name="\.ExtrasBridge"\s+android:authorities="sa\.urtv\.home\.extras"\s+android:exported="true"\s+android:permission="sa\.urtv\.permission\.EXTRAS"', man))
check("the names both sides use are one file, compiled into both", 'EXTRAS_AUTHORITY = "sa.urtv.extras.bridge", HOME_AUTHORITY = "sa.urtv.home.extras"' in words
      and "'ExtrasWords.java'" in gradle.split("def shared = ")[1].split("\n")[0] and "srcDirs += ['../launcher" not in gradle and 'SERVICE = "sa.urtv.extras/sa.urtv.home.ExtrasA11yService"' in words)
check("Android 11+ package visibility: each app names the other in <queries>", '<package android:name="sa.urtv.extras" />' in man and '<package android:name="sa.urtv.home" />' in xman)
check("the add-on's provider answers only its fixed calls (status, type, press, sleep, go_home, watch), no rows",
      all(f"case ExtrasWords.{m}:" in prov for m in ("STATUS", "TYPE", "PRESS", "SLEEP", "GO_HOME", "WATCH")) and "query(Uri u, String[] p, String s, String[] a, String o) { return null; }" in prov)
check("the home's bridge answers only the family lock's questions and the Settings key (never another key)",
      all(f"case ExtrasWords.{m}" in bridge for m in ("TYPE_CHECK", "SETTINGS_PIN", "KEY")) and "if (SettingsKey.isSettingsKey(e.getKeyCode()))" in bridge)
check("the home's calls fail soft: an add-on that is missing or does not answer is «off», never an error", "catch (RuntimeException e) { return null; }" in ext
      and "return r == null ? RemoteNav.OFF : r;" in ext and "return r == null ? ExtrasWords.OFF : r;" in ext)
check("the screens never ask the add-on themselves: the kept answer, asked again on a thread when older than 2 s (4.4, QA-6, R1)",
      "static final long FRESH_MS = 2000;" in ext and "android.os.SystemClock.elapsedRealtime() - askedAt < FRESH_MS" in ext
      and '"extras-status").start();' in ext and "refresh();\n        return known;" in ext)
# the offer: only when a feature that needs it was asked for
center, main, phone = text(HOME / "ControlCenter.java"), text(HOME / "MainActivity.java"), text(HOME / "PhoneApps.java")
check("the offer opens from the four places that need the add-on: «أطفئ الآن» (OK and a long OK), the sleep timer's line, the phone's keys and the phone's typing",
      center.count('Extras.intent(act, "sleep")') == 2 and 'Extras.intent(this, "sleep")' in main and 'Extras.offerOnce("keys")' in phone and 'Extras.offerOnce("type")' in phone)
check("the phone's keys and typing offer it at most once in two minutes", "now - lastOffer < 120_000" in ext)
# the sixth is the once-only bar for whoever used «فوق تطبيق آخر» on 4.3.x (OverlayOfferRule; tests/overlay_offer_test.py pins its once-flag)
check("nothing else opens the offer (no start-up nag)", len(re.findall(r"Extras\.(?:offer\w*|intent)\(", "".join(text(p) for p in HOME.rglob("*.java") if p.name not in ("Extras.java",)))) == 6)
check("the offer screen: the approved name and words, the Play Protect line, «ثبّت الإضافة» and «ليس الآن»",
      all(w in act for w in ('S.t("إضافات تلفزيونك")', "يطلب بيانات حساسة", "تثبيت على أي حال", 'S.t("ثبّت الإضافة")', 'S.t("ليس الآن")', "تسهيل الاستخدام")) and "تلفزيونك بلس" not in act)
check("the install is our release file only: our package and our key are checked before Android's installer",
      "SaverInstallRule.verify(ExtrasWords.PACKAGE," in act and "MirrorInstall.commit(this, f, ExtrasWords.PACKAGE, RESULT)" in act
      and 'URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-extras.apk"' in words)
check("installed but off: the button opens the TV's accessibility settings (not when they are behind the PIN)",
      "Settings.ACTION_ACCESSIBILITY_SETTINGS" in act and "ParentalPolicy.SLEEP_ACCESSIBILITY" in act)
check("the activity is exported only behind the signature permission OPEN_EXTRAS (the screensaver's «فوق تطبيق آخر» opens the offer)",
      re.search(r'android:name="\.ExtrasActivity" android:exported="true" android:permission="sa\.urtv\.home\.OPEN_EXTRAS"', man)
      and re.search(r'<permission\s+android:name="sa\.urtv\.home\.OPEN_EXTRAS"\s+android:protectionLevel="signature"', man))
# the watch
ev = svc.split("public void onAccessibilityEvent(AccessibilityEvent e) {")[1].split("\n    }\n")[0]
check("the Home watch reads of a window only its package and class, and asks HomeWatch",
      "if (!stockPackages.contains(pkg)) return;" in ev and "watch.decide(on, pkg, cls, stockHomes, chosen || wantedNow, now, fight)" in ev and not re.search(r"getText|getSource|getContentDescription", ev))
check("the watch is on by default only on a Google TV home (launcherx), and the home can switch it", 'return installed(c, "com.google.android.apps.tv.launcherx");' in svc and "case ExtrasWords.WATCH:" in prov)
check("the stock homes the watch knows are never ours, the setup wizard or a settings app", "SettingsKey.stockHomeOn(java.util.Collections.singletonList(ri.activityInfo.packageName), HOME_PKG)" in svc)
# scripts and CI
wf, ins, dock = text(ROOT / ".github/workflows/android.yml"), text(ROOT / "optimize/install.sh"), text(ROOT / "optimize/dock.sh")
check("CI builds the add-on and publishes urtv-extras.apk with the others (apk-latest, the numbered release, apk-next)",
      "cp android/extras/build/outputs/apk/release/extras-release.apk urtv-extras.apk" in wf and wf.count("            urtv-extras.apk") == 4)
check("install.sh addon installs it (Play Protect line for the person), dock.sh extras-setup turns its service on",
      "push_install urtv-extras.apk sa.urtv.extras" in ins and "تثبيت على أي حال" in ins.split('if [ "$what" = addon ]; then')[1][:600]
      and 'svc="sa.urtv.extras/sa.urtv.home.ExtrasA11yService"' in dock)
check("tests/android_compile.sh compiles the add-on too", "the extras module compiles" in text(ROOT / "tests/android_compile.sh"))
# 4.4 (the tester's TCL): turned on in the TV's list but not run by Android (TclAppBoot refused the bind; a force-stop): detected and said, never assumed to work
heal = text(HOME / "A11yHeal.java")
check("4.4: «turned on but not running» = listed and not bound (Extras.enabled is the service's own answer)", "static boolean listed(Context c) { return A11yList.has(read(c, LIST), TYPE); }" in heal
      and "return A11yList.has(list, TYPE) && Boolean.FALSE.equals(Extras.enabledKnown());" in heal)   # 4.4 (QA-6): only a real «off»
check("4.4: the offer screen says the one fix (off and on again in «تسهيل الاستخدام»), after our own heal where we may write the list",
      "if (A11yHeal.listed(this))" in act and 'A11yHeal.check(getApplicationContext(), "fix-offer")' in act and "state.setText(S.tv(STUCK_FIX));" in act and "أطفئها وشغّلها من جديد" in act)
check("4.4: the home's bar says it too (after the TV has been up 90 s, never in kids mode, its own «لاحقاً»), OK heals it or opens the accessibility settings behind the sleep lock",
      "A11yHeal.stuck(this)" in main and "elapsedRealtime() > 90_000" in main and '"extras_later_until"' in main and 'A11yHeal.check(getApplicationContext(), "fix-nudge")' in main
      and "if (nudgeIsExtras) {" in main and "ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)" in main)
check("4.4: a heal from the offer or the bar is not taken for our own update (UpdateCarry is not run for «fix-…»)", '!"boot".equals(why) && !why.startsWith("fix")' in heal)
check("4.4: dock.sh extras-setup reads Android's Bound services after writing the list, turns the list off and on while ours is not bound (twice), says the person's fix if it still is not, and allows TCL's AUTO_START",
      "Bound services:" in dock and "the list was turned off and on again" in dock and "أطفئها وشغّلها من جديد" in dock and "appops set sa.urtv.extras AUTO_START allow" in dock)
check("4.4 review F9: the offer asks the add-on on a thread when it resumes and looks again on the UI thread when it answers (not only the kept answer)",
      "protected void onResume() {" in act and 'new Thread(() -> { Extras.askNow(); runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) refresh(); }); }, "extras-offer").start();' in act.split("protected void onResume() {")[1].split("}\n\n")[0] + "}")
sys.exit(1 if failed else 0)
