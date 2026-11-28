#!/usr/bin/env python3
"""4.3.7: after an update, once, the home's top bar offers the diagnostic app «فحص تلفزيونك» (sa.urtv.doctor), gated by urtv-data's release/doctor.json (its main keeps it as content/doctor.json)
(DoctorOfferRule, DoctorOfferStore). The rules run in Java (tests/DoctorOfferRun.java): no flag, flag off, doctor installed, fresh install, kids mode and
«لاحقاً» for this build each mean no card. The wiring is pinned: the one bar, after the build, city and home questions; «لاحقاً» keeps the build; «ثبّته» says
Downloader's code; no permission is added (only a package visibility entry).

    python3 tests/doctor_offer_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "DoctorOfferRule.java"), str(ROOT / "tests/DoctorOfferRun.java")], capture_output=True, text=True)
        check("the rule compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.DoctorOfferRun"], capture_output=True, text=True, encoding="utf-8").stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
else:
    print("FAIL no javac or java: the rule cannot be run")
    failed += 1

def DOCTOR_PKG_OK(sug):
    return 'static final String PKG = "sa.urtv.doctor"' in (HOME / "DoctorOfferRule.java").read_text(encoding="utf-8") and 'new App("doctor", DOCTOR,' in sug


main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
store = (HOME / "DoctorOfferStore.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
upd = main[main.index("private void updateHomeNudge"):]
upd = upd[:upd.index("    /** «تراجع»")]
check("the bar (4.4): the doctor comes after the stock-home, update, add-on, overlay, resume and city questions, never two at once, never over playback",
      "boolean askDoctor = !askStock && !askUpdate && !askExtras && !askOverlay && !askResume && !askCity && !quiet" in upd and "|| askCity || askDoctor;" in upd)
check("the bar asks the rule with: the kept flag (ContentFile.Kept, off the UI thread), whether sa.urtv.doctor is installed, whether the app was updated, kids mode, the dismissed build and this build",
      "DoctorOfferRule.fromDoc(doctorFlag.value())" in upd and "isInstalled(DoctorOfferRule.PKG), DoctorOfferStore.updated(this), kids, DoctorOfferStore.dismissed(this), BuildConfig.VERSION_CODE" in upd
      and "new ContentFile.Kept(DoctorOfferStore.NAME)" in main and main.count("doctorFlag.load(app)") == 2)
check("«لاحقاً» keeps the build (never again for it); «ثبّته» says Downloader's code and installs nothing",
      "DoctorOfferStore.dismiss(this, BuildConfig.VERSION_CODE)" in main and "doctorCodeShown = true" in main and "PackageInstaller" not in store and "REQUEST_INSTALL" not in store)
check("the flag is urtv-data's release/doctor.json (its main keeps it as content/doctor.json) through ContentFile (kept copy, a background fetch at most every 6 hours, a bad answer keeps the last good one)",
      'PATH = "release/doctor.json"' in store and "ContentFile.refresh(" in store and "6 * 3600_000L" in store and "DoctorOfferStore.keep(app, DoctorOfferRule.fromDoc(doctorFlag.value()))" in main and "static boolean listed(Context c) { return !listedCode(c).isEmpty(); }" in store and "ContentFile.read" not in store)   # (the list reads a preference the home's tick keeps: no disk work on a UI thread, regressions/ui_thread_ipc_test)
check("the words: «ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني» (7 words), «ثبّته», «لاحقاً»; each has its English",
      all(w in main for w in ("ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني", "ثبّته", "افتح Downloader واكتب الرمز")) and len("ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني".split()) <= 8
      and all(w in (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8") for w in ("ثبّته\tInstall it", "ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني\t")))
sug = (HOME / "SuggestedApps.java").read_text(encoding="utf-8")
act = (HOME / "SuggestedAppsActivity.java").read_text(encoding="utf-8")
check("the suggested apps list: «فحص تلفزيونك» is a FLAG entry listed only while the flag is on, and the tile says Downloader's code (nothing installed by us); the package name is the rule's",
      'DOCTOR = "sa.urtv.doctor"' in sug and DOCTOR_PKG_OK(sug) and "a.kind == SuggestedApps.Kind.FLAG && !DoctorOfferStore.listed(this)" in act and "if (a.kind == SuggestedApps.Kind.FLAG) {" in act
      and "static boolean listed(Context c)" in store)
check("no permission is added for it: a package visibility entry for sa.urtv.doctor and no uses-permission that names it",
      '<package android:name="sa.urtv.doctor" />' in man and not re.search(r"<uses-permission[^>]*doctor", man))
sys.exit(1 if failed else 0)
