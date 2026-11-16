#!/usr/bin/env python3
"""4.3.2 «ثبّت الجزء الثاني»: a TV with only the home (a Fire TV, or installed with Downloader) must never meet a silent nothing when it asks for the
settings, the city or the teams: the install screen opens, downloads OUR release file, checks its package and key, and hands it to Android's installer;
Downloader's code 2927902 is the fallback text. Needs javac for the JVM part.   Run: python3 tests/install_saver_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


rule, act, main, matches = text(HOME / "SaverInstallRule.java"), text(HOME / "InstallSaverActivity.java"), text(HOME / "MainActivity.java"), text(HOME / "MatchesActivity.java")
manifest = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        stub = Path(tmp) / "sa/urtv/home"
        stub.mkdir(parents=True)
        (stub / "S.java").write_text("package sa.urtv.home; final class S { static String t(String s) { return s; } }", encoding="utf-8")
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SaverInstallRule.java"), str(stub / "S.java"), str(ROOT / "tests/SaverInstallRuleRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SaverInstallRuleRun"], check=True, capture_output=True, text=True).stdout.split("\n")
    r = {l.split(" ")[0]: l.split(" ", 1)[1] for l in out if l}
    check("a missing second app needs the install screen, an installed one does not", r["missing"] == "true here false", r["missing"])
    check("only our own file passes: the package is the screensaver's and the key is exactly this app's", r["ok"] == "null", r["ok"])
    check("another package, another key, no key, a malformed hash, no package: each refused",
          r["other_app"] == "other_app" and r["other_key"] == "other_key" and r["unsigned"] == "unsigned" and r["unsigned2"] == "unsigned" and r["badhash"] == "unsigned" and r["nullpkg"] == "other_app", r)
    check("the file is the latest release's screensaver, and Downloader's code is 2927902",
          r["url"].startswith("https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk") and r["url"].endswith("code 2927902"), r["url"])
else:
    print("SKIP install saver (JVM half): no javac here")
check("the screen: «ثبّت الجزء الثاني», a button that downloads and installs, Downloader's code in Latin digits (not through S.t), the 5 % safe area",
      'S.t("ثبّت الجزء الثاني")' in act and 'S.t("نزّل وثبّت")' in act and "SaverInstallRule.DOWNLOADER_CODE" in act and "widthPixels * 0.05f" in act)
check("it downloads, verifies the package and the key BEFORE the installer, and uses the same install path as the other installs",
      "MirrorInstall.download(SaverInstallRule.URL, f)" in act and "SaverInstallRule.verify(" in act and act.index("SaverInstallRule.verify(") < act.index("MirrorInstall.commit(")
      and "MirrorInstall.signers(getPackageManager(), getPackageName())" in act)
check("REQUEST_INSTALL_PACKAGES flow: Android's «allow this source» screen when needed, then it goes on by itself; the installer's own confirmation is opened",
      "canRequestPackageInstalls()" in act and "Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES" in act and "STATUS_PENDING_USER_ACTION" in act and "REQUEST_INSTALL_PACKAGES" in manifest)
check("the activity is in the manifest and is not exported", 'android:name=".InstallSaverActivity"\n            android:exported="false"' in manifest)
check("«تلفزيونك», the clean home's button and the pill (openOurSettings without the TV fallback) open it when the second app is missing, behind the same lock",
      "else {\n            openSaverInstall();" in main and "void openSaverInstall()" in main and "guarded(ParentalPolicy.OUR_SETTINGS, null, S.t(\"ثبّت الجزء الثاني\")" in main)
check("«المدينة» (the control center, the question bar) opens it too, never nothing", "else openSaverInstall();" in main[main.index("private void openCity()"):main.index("private void openCity()") + 700])
check("«فرقي» on the matches page opens it when the settings app is missing (not in kids mode), and the page is made again when it comes back",
      "new android.content.Intent(this, InstallSaverActivity.class)" in matches and "if (Parental.state(this).kids) return;" in matches and "teamsOpened = true;" in matches)
check("the remote's Settings key still falls back to the TV's own settings (a person with no second app still gets a screen)",
      "launchGuarded(ParentalPolicy.REMOTE_SETTINGS, new Intent(Settings.ACTION_SETTINGS)" in main)
sys.exit(1 if failed else 0)
