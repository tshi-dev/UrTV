#!/usr/bin/env python3
"""The release's name («4.0», the VERSION file) without breaking updates: the versionName is "4.0.<build>" (one word), the versionCode is still the
run number, version.txt is still exactly "3.<run>" (an installed 3.142 reads it as before), nothing takes our own versionName apart, the
release is published under its name with the build in the body, and the screens show «4.0 (بناء N)».
(The comparison itself, an installed 3.142 fed the new version.txt, is in tests/versions_test.py.)  Run: python3 tests/version_name_test.py"""
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


ver = text("VERSION")
check("VERSION is one line, major.minor, ending in a newline (the one source of the name)", re.fullmatch(r"\d+\.\d+\n", ver) is not None, repr(ver))
name = ver.strip()

for mod in ("app", "launcher"):
    g = text(f"android/{mod}/build.gradle")
    check(f"{mod}: versionCode is still the run number",
          "versionCode((System.getenv('GITHUB_RUN_NUMBER') ?: '1') as int)" in g)
    check(f"{mod}: versionName is the VERSION file and the run, one word (4.0.<build>), and MARKETING is the VERSION file",
          "versionName \"${file('../../VERSION').text.trim()}.${System.getenv('GITHUB_RUN_NUMBER') ?: '0'}\"" in g
          and 'buildConfigField "String", "MARKETING", "\\"${file(\'../../VERSION\').text.trim()}\\""' in g and "buildConfig true" in g)
    check(f"{mod}: the VERSION path is the repository root's from the module", (ROOT / "android" / mod / "../../VERSION").resolve() == (ROOT / "VERSION").resolve())
sh = text("tests/android_compile.sh")
check("the compile check's BuildConfig has MARKETING for both modules", 'MARKETING="4.0"' in sh)

# nothing takes our own versionName apart
java = [f for f in (ROOT / "android").rglob("*.java")]
parse = [f.name for f in java if re.search(r"\.versionName|VERSION_NAME", f.read_text(encoding="utf-8"))]
check("no Java reads our own versionName or BuildConfig.VERSION_NAME (the build is VERSION_CODE, the name MARKETING)", not parse, parse)
check("the updater compares builds: the screensaver's settings lines, saver.sh show and the update screen all use VERSION_CODE and MARKETING",
      "int mine = BuildConfig.VERSION_CODE;" in text("android/app/src/main/java/sa/urtv/screensaver/Updater.java")
      and "Versions.displayEn(BuildConfig.MARKETING, BuildConfig.VERSION_CODE)" in text("android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java")
      and "UpdateText.versionOf(BuildConfig.MARKETING, BuildConfig.VERSION_CODE)" in text("android/launcher/src/main/java/sa/urtv/home/phone/PhoneUpdater.java")
      and "UpdateText.versionOf(BuildConfig.MARKETING, result.target)" in text("android/launcher/src/main/java/sa/urtv/home/UpdateActivity.java"))
check("the update screen's compare is by build (version.txt's build against the installed versionCode), as it was",
      "UpdateRules.buildOf(ver.text)" in text("android/launcher/src/main/java/sa/urtv/home/phone/PhoneUpdater.java") and "installedCode" in text("android/launcher/src/main/java/sa/urtv/home/phone/PhoneUpdater.java"))

# the scripts that print versionName take it as one word
share = text("optimize/share.py")
m = re.search(r'versionName=\(\\S\+\)', share)
check("share.py takes versionName as one word (4.0.151 is one)", m is not None and re.search(r"versionName=(\S+)", "    versionName=4.0.151").group(1) == "4.0.151")
inst = text("optimize/install.sh")
check("install.sh and run.sh print versionName as it is (sed 's/.*=//'), no digits or dots assumed",
      "sed 's/.*=//' | tr -d '\\r '" in inst and "grep versionName" in text("optimize/run.sh"))
r = subprocess.run([sys.executable, str(ROOT / "tests/install_versions_test.py")], capture_output=True, text=True)
check("the report's «Versions:» line is written with 4.0.151 like with 3.124 (tests/install_versions_test.py: its cases, and this answer)", r.returncode == 0 and "FAIL" not in r.stdout, r.stdout[-300:])
tail = text("tests/install_versions_test.py")
check("install_versions_test also feeds a 4.0.<build> name", '"4.0.151"' in tail)

# the workflow
wf = text(".github/workflows/android.yml")
check("version.txt is written exactly as before: \"3.<run>\" (an installed 3.142 reads the newest build from it)", 'echo "3.${{ github.run_number }}" > version.txt' in wf and wf.count("> version.txt") == 1)
check("version.txt goes to apk-latest and apk-next as before, and not to the numbered release",
      wf.count("            version.txt\n") == 2)
check("the Version step validates VERSION (major.minor) and the numbered release is v<name>.<run> «UrTV <name> (build <run>)»",
      "id: ver" in wf and "grep -Eq '^[0-9]+\\.[0-9]+$'" in wf and "tag_name: v${{ steps.ver.outputs.name }}.${{ github.run_number }}" in wf
      and "name: UrTV ${{ steps.ver.outputs.name }} (build ${{ github.run_number }})" in wf)
check("the bodies say the version and the build", wf.count('body: "Both apps at version ${{ steps.ver.outputs.name }} (build ${{ github.run_number }}), built from') == 3)
check("apk-latest is still published from master with make_latest, apk-next from next", "tag_name: apk-latest" in wf and "make_latest: true" in wf and "tag_name: apk-next" in wf)
check("a change of VERSION alone starts a build", "'VERSION'" in wf.split("paths:")[1].splitlines()[0])
check("the old v3.N tag form is gone from the workflow (new builds are v<name>.<run>)", "v3.${{" not in wf)
# docs
cl = text("CHANGELOG.md")
check("the CHANGELOG's section for this release is 4.0", re.search(r"^## 4\.0 ", cl, re.M) is not None and "version.txt` is still exactly `3.<build>`" in cl)
check("the README names the release v4.0.N and says version.txt stays 3.<build>", "v4.0.N" in text("README.md") and "`3.<build>`" in text("README.md"))
# the screens
page = text("android/launcher/src/main/assets/phone/index.html")
check("the phone page shows the installed version and the newest as the TV words them (u.version, s.version)", "s.version ? 'إصدار تلفزيونك: ' + s.version" in page and "(u.version || 'الإصدار ' + u.build)" in page and 'id="ver"' in page)
check("the state carries the version, and the update status carries it with the build", '"version", UpdateText.versionOf(BuildConfig.MARKETING, BuildConfig.VERSION_CODE)' in text("android/launcher/src/main/java/sa/urtv/home/phone/PhoneActions.java")
      and 'm.put("version", UpdateText.versionOf(BuildConfig.MARKETING, build))' in text("android/launcher/src/main/java/sa/urtv/home/phone/PhoneUpdater.java"))
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
