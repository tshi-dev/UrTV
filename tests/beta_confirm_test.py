#!/usr/bin/env python3
"""4.3.4 (the owner): an update that is a BETA build (it came from apk-beta, newer than apk-latest) asks first, on the TV: «نسخة تجريبية»,
«هذا التحديث نسخة تجريبية: فيها مزايا جديدة وقد تكون فيها أخطاء. تبي تحمّلها؟», «حمّل» / «لاحقاً». Nothing of it is downloaded before «حمّل»;
«لاحقاً» remembers that build and it is not offered again (a newer beta is; an official build always is, the official flow is unchanged).
The phone page's «ثبّت» on a beta build brings the TV's update screen with the same question. The rules run on a JVM.
Run: python3 tests/beta_confirm_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

pu = (HOME / "phone/PhoneUpdater.java").read_text(encoding="utf-8")
ua = (HOME / "UpdateActivity.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
prep = pu.split("private static void prepare(Context c, boolean download) throws Exception {")[1].split("\n    }\n")[0]
inst = pu.split("public static Map<String, Object> install(final Context c, boolean fromPhone) {")[1].split("\n    }\n")[0]
ask = ua.split("private void askBeta() {")[1].split("\n    }\n")[0]

check("the TV's check stops at «found» for a beta build not answered yet: nothing downloaded",
      'if (UpdateRules.betaAsks(fromBeta(), newest, betaAgreed)) { set("found", "", ""); return; }' in prep
      and prep.index("betaAsks") < prep.index("downloadAndVerify(c, false);"))
check("a declined beta build is not chosen again (the latest release is read instead)", "betaDeclined(c)" in prep and "UpdateRules.betaOffered(" in prep)
check("install() of an unanswered beta build downloads nothing; from the phone it opens the TV's screen with the question",
      "if (found && UpdateRules.betaAsks(fromBeta(), build, betaAgreed)) {" in inst and 'putExtra("ask_beta", true)' in inst
      and inst.index("betaAsks") < inst.index("downloadAndVerify"))
check("the button asks first when it is a beta build; an official build installs as before",
      "if (PhoneUpdater.betaQuestion()) { askBeta(); return; }" in ua and "installGuarded();" in ua)
check("the phone's «ثبّت» question shows on the TV once (the extra is removed), and singleTop's new intent is read",
      'getIntent().removeExtra("ask_beta");' in ua and "setIntent(i);" in ua)
check("the words are the owner's", 'S.n("نسخة تجريبية")' in ua and 'S.n("هذا التحديث نسخة تجريبية: فيها مزايا جديدة وقد تكون فيها أخطاء. تبي تحمّلها؟")' in ua
      and 'S.n("حمّل")' in ua and 'S.n("لاحقاً")' in ua)
check("…and in English", all(f"{w}\t" in tsv for w in ("نسخة تجريبية", "هذا التحديث نسخة تجريبية: فيها مزايا جديدة وقد تكون فيها أخطاء. تبي تحمّلها؟", "حمّل", "لاحقاً")))
check("«حمّل»: agreed for this build, then the guarded install (the family PIN as before)",
      "PhoneUpdater.betaAnswer(this, true); installGuarded();" in ask)
check("«لاحقاً»: this build declined, then a check again (an official update, or «لا يوجد أحدث»)",
      "PhoneUpdater.betaAnswer(this, false);" in ask and "PhoneUpdater.check(this);" in ask)
check("the declined build is kept on disk; the agreed one only for this run", '.putInt("declined", b)' in pu and "betaAgreed = b;" in pu)

if not shutil.which("javac"):
    print("SKIP the rules on a JVM: no javac")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    (Path(tmp) / "C.java").write_text("""package sa.urtv.home.phone;
public class C { public static void main(String[] a) {
  System.out.println(String.join(" ",
    // betaOffered(beta, latest, declined)
    "" + UpdateRules.betaOffered(480, 471, -1), "" + UpdateRules.betaOffered(480, 471, 480), "" + UpdateRules.betaOffered(490, 471, 480),
    "" + UpdateRules.betaOffered(-1, 471, -1), "" + UpdateRules.betaOffered(470, 471, -1),
    // betaAsks(fromBeta, newest, agreed)
    "" + UpdateRules.betaAsks(true, 480, -1), "" + UpdateRules.betaAsks(true, 480, 480), "" + UpdateRules.betaAsks(true, 490, 480), "" + UpdateRules.betaAsks(false, 471, -1)));
} }
""", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "phone/UpdateRules.java"), str(Path(tmp) / "C.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.C"], check=True, capture_output=True, text=True).stdout.split()
want = ["true", "false", "true", "false", "false", "true", "false", "true", "false"]
check("offered: a newer beta yes; the declined build no; a newer beta than the declined yes; none or older no", got[:5] == want[:5], got)
check("asks: an unanswered beta yes; the agreed build no; a newer beta again yes; an official build never", got[5:] == want[5:], got)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
