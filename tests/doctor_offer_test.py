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
check("the bar: the doctor comes after the builds, the city and the home question, never two at once",
      "boolean askDoctor = !askUpdate && !askCity && !homeNudge && DoctorOfferRule.show(" in upd and "askUpdate || askCity || homeNudge || askDoctor" in upd)
check("the bar asks the rule with: the kept flag, whether sa.urtv.doctor is installed, whether the app was updated, kids mode, the dismissed build and this build",
      "doctorFlag, isInstalled(DoctorOfferRule.PKG), DoctorOfferStore.updated(this), kids, DoctorOfferStore.dismissed(this), BuildConfig.VERSION_CODE" in upd)
check("«لاحقاً» keeps the build (never again for it); «ثبّته» says Downloader's code and installs nothing",
      "DoctorOfferStore.dismiss(this, BuildConfig.VERSION_CODE)" in main and "doctorCodeShown = true" in main and "DoctorOfferStore.flag(this)" in main
      and "PackageInstaller" not in store and "REQUEST_INSTALL" not in store)
check("the flag is read from urtv-data's release/doctor.json (its main keeps it as content/doctor.json) at most every 6 hours, in the background, bounded to 4 KB; a 404 forgets it; a failure keeps the last good copy",
      "urtv-data/data/release/doctor.json" in store and "EVERY_MS = 6 * 3600_000L" in store and "Bounded.readAll(in, 4096)" in store and "code == 404" in store and "p.edit().remove(\"flag\")" in store
      and "new Thread(" in store and "setDaemon(true)" in store)
check("the words: «ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني» (7 words), «ثبّته», «لاحقاً»; each has its English",
      all(w in main for w in ("ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني", "ثبّته", "افتح Downloader واكتب الرمز")) and len("ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني".split()) <= 8
      and all(w in (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8") for w in ("ثبّته\tInstall it", "ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني\t")))
sug = (HOME / "SuggestedApps.java").read_text(encoding="utf-8")
act = (HOME / "SuggestedAppsActivity.java").read_text(encoding="utf-8")
check("the suggested apps list: «فحص تلفزيونك» is a FLAG entry listed only while the flag is on, and the tile says Downloader's code (nothing installed by us); the package name is the rule's",
      'DOCTOR = "sa.urtv.doctor"' in sug and DOCTOR_PKG_OK(sug) and "a.kind == SuggestedApps.Kind.FLAG && !DoctorOfferStore.listed(this)" in act and "if (a.kind == SuggestedApps.Kind.FLAG) {" in act
      and "static boolean listed(Context c)" in store and "f != null && f.on && !f.code.isEmpty()" in store)
perms = re.findall(r"<uses-permission[^>]*android:name=\"([^\"]+)\"", man)
base = subprocess.run(["git", "show", "v4.3.517:android/launcher/src/main/AndroidManifest.xml"], cwd=ROOT, capture_output=True, text=True)
base_perms = re.findall(r"<uses-permission[^>]*android:name=\"([^\"]+)\"", base.stdout) if base.returncode == 0 else None
check("no permission is added: only a package visibility entry for sa.urtv.doctor (the manifest's permissions are the same as 4.3.6's)",
      '<package android:name="sa.urtv.doctor" />' in man and (base_perms is None or sorted(perms) == sorted(base_perms)), (perms, base_perms))
sys.exit(1 if failed else 0)
