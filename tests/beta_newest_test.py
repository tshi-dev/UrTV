#!/usr/bin/env python3
"""4.3.4 (an independent review): on «النسخ التجريبية» the TV read ONLY apk-beta/version.txt, and since 4.3.3 a 404 there means «nothing newer»,
so a person who turned the beta on never got another update, 4.3.4 included. Now every reader takes the newer of apk-beta and apk-latest:
the update screen and the phone page (PhoneUpdater.prepare, which also downloads from the release it chose), the home's update pill
(UpdateNotice.checkIfDue) and the screensaver's «الإصدار المتاح» line (Updater.checkLatest). The rules run on a JVM.
Run: python3 tests/beta_newest_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

pu = (HOME / "phone/PhoneUpdater.java").read_text(encoding="utf-8")
un = (HOME / "UpdateNotice.java").read_text(encoding="utf-8")
up = (APP / "Updater.java").read_text(encoding="utf-8")
prep = pu.split("private static void prepare(Context c, boolean download) throws Exception {")[1].split("\n    }\n")[0]
check("the update screen on beta reads apk-latest too and keeps the newer (the release it downloads from follows)",
      'Version latest = readVersion(UpdateRules.BASE + "version.txt");' in prep
      and "if (!UpdateRules.betaOffered(builtOf(ver), builtOf(latest), betaDeclined(c))) { ver = latest; base = UpdateRules.BASE; }" in prep
      and prep.index("betaOffered") < prep.index("releaseBase = base;"))
check("…a 404 is «nothing newer» only when the chosen release is the beta one", "UpdateRules.BETA_BASE.equals(base))" in prep)
check("the home's pill on beta: the newer of both", 'if ("beta".equals(channel)) { int latest = newestAt(UpdateRules.BASE); if (!UpdateRules.betaWins(build, latest)) build = latest; }' in un)
check("the screensaver's version line on beta: the newer of both", "v = Versions.newerOf(v, l);" in up and "String l = latestFromAsset(VERSION_URL);" in up)
check("fromBeta() says the found update is the beta release (for the TV's question before it downloads)",
      "public static boolean fromBeta() { return UpdateRules.BETA_BASE.equals(releaseBase); }" in pu)

if not shutil.which("javac"):
    print("SKIP the rules on a JVM: no javac")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    (Path(tmp) / "B.java").write_text("""package sa.urtv.home.phone;
public class B { public static void main(String[] a) {
  // beta build, latest build (-1: not read, a 404 or no network) -> does the beta release win?
  int[][] c = {{-1, 471, 0}, {480, 471, 1}, {470, 471, 0}, {471, 471, 0}, {480, -1, 1}, {-1, -1, 0}, {0, 471, 0}};
  for (int[] x : c) System.out.println(UpdateRules.betaWins(x[0], x[1]) == (x[2] == 1) ? "ok" : "BAD " + x[0] + " " + x[1]);
} }
""", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "phone/UpdateRules.java"), str(Path(tmp) / "B.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.B"], check=True, capture_output=True, text=True).stdout.split("\n")
    out = [o for o in out if o]
    check("betaWins: beta 404 & latest 471 -> latest; beta 480 & latest 471 -> beta; an older or equal beta -> latest; latest unread -> beta; neither -> latest",
          out == ["ok"] * 7, out)
    (Path(tmp) / "V.java").write_text("""package sa.urtv.screensaver;
public class V { public static void main(String[] a) {
  System.out.println(Versions.newerOf(null, "3.471") + " " + Versions.newerOf("3.480", "3.471") + " " + Versions.newerOf("3.470", "3.471") + " " + Versions.newerOf(null, null) + " " + Versions.newerOf("3.471", null));
} }
""", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "Versions.java"), str(Path(tmp) / "V.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.V"], check=True, capture_output=True, text=True).stdout.split()
    check("Versions.newerOf: the version line shows the newer of both (null when neither answered)", got == ["3.471", "3.480", "3.471", "null", "3.471"], got)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
