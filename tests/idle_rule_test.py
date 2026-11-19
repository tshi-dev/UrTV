#!/usr/bin/env python3
"""«تبدأ شاشة التوقف بعد 2147483 ثانية» (user report): the TV's idle delay was 2147483647 (Integer.MAX: the TV turned its screensaver off) and
the row showed the raw number, and «تغيير» opened the maker's settings, which have no screensaver timer. Now the row says «أبداً (التلفزيون أوقف
شاشة التوقف)», and choosing a time writes it: this app where it may (WRITE_SETTINGS), else the home over its kept adb link (NowPlayingProvider
call("set_idle") -> SetupHook.setIdle), else one line that says how. The rules run in tests/IdleRuleRun.java and tests/IdleSetRun.java; the
wiring is read from the source here.
Run: python3 tests/idle_rule_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return p.read_text(encoding="utf-8")
act, rule, bridge, words = read(SAVER / "SettingsActivity.java"), read(SAVER / "IdleRule.java"), read(SAVER / "NowPlayingBridge.java"), read(SAVER / "Words.java")
hook, setrule = read(SETUP / "SetupHook.java"), read(SETUP / "IdleSetRule.java")
prov = read(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java")
en = read(ROOT / "i18n/en/saver.tsv")

check("no Android in either rule", "import android" not in rule and "import android" not in setrule)
lab = act.split("private String idleLabel() {")[1].split("\n    }\n")[0]
check("the row says never for IdleRule.NEVER, before any number is made", "case IdleRule.NEVER: return IDLE_NEVER;" in lab and lab.index("NEVER") < lab.index("ثانية"))
check("the never words", 'IDLE_NEVER = "أبداً (التلفزيون أوقف شاشة التوقف)";' in act)
check("the hub lines never say «تبدأ بعد أبداً»", "تبدأ بعد \" + idleLabel()" not in act.replace(act.split("private String idleSummary() {")[1].split("\n    }\n")[0], "")
      and 'if (k == IdleRule.NEVER) return "لا تبدأ: التلفزيون أوقف شاشة التوقف";' in act)
pick = act.split("private void pickIdle() {")[1].split("\n    }\n")[0]
st = act.split("private void setIdle(final String minutes) {")[1].split("\n    static final String IDLE_NO_LINK")[0]
check("«تغيير» no longer opens the maker's settings (they have no screensaver timer)", "openDreamSettings" not in pick and "openDreamSettings" not in st)
check("the list always opens; the choice goes to setIdle", "setIdle(Prefs.IDLE_MINUTES[i]);" in pick and "canWriteSettings" not in pick)
check("our own write first where it may, then the home's adb link off the UI thread, then the one line",
      st.index("Prefs.canWriteSettings(this)") < st.index("new Thread(") < st.index("NowPlayingBridge.setIdle(") < st.index("IDLE_NO_LINK"))
check("the bridge asks the home's provider, no_link when it could not", '"set_idle", minutes, null' in bridge and 'return "no_link"; }' in bridge)
call = prov.split('if ("set_idle".equals(method)')[1].split("\n        }\n")[0]
check("the provider checks the screensaver's key and answers SetupHook.setIdle", 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING")' in call and "SetupHook.setIdle(getContext(), arg)" in call)
si = hook.split("public static String setIdle(Context c, String minutes) {")[1].split("\n    }\n")[0]
check("the home refuses other minutes before adb, waits for a running setup, needs the kept key",
      si.index('return "bad"') < si.index('return "busy"') < si.index("AdbKeys.exists(app)") < si.index("SetupSession.openAny("))
check("it runs only IdleSetRule's commands, after its probe", "s.run(IdleSetRule.PROBE)" in si and "IdleSetRule.commands(ms, probe)" in si)
check("the screensaver's minutes and the home's are the same list", 'IDLE_MINUTES = {"1", "2", "5", "10", "15", "30"}' in read(SAVER / "Prefs.java") and 'MINUTES = {"1", "2", "5", "10", "15", "30"}' in setrule)
for w in ("أبداً (التلفزيون أوقف شاشة التوقف)", "لا تبدأ: التلفزيون أوقف شاشة التوقف", "جارٍ الضبط…", "إعداد التلفزيون شغّال الحين، جرّب بعده"):
    check("English and the marker for «" + w + "»", ("\n" + w + "\t") in en and 'S.n("' + w + '")' in words)
nl = act.split('IDLE_NO_LINK = "')[1].split('";')[0]
check("the no-link line has its English and marker", ("\n" + nl + "\t") in en and 'S.n("' + nl + '")' in words)

if not shutil.which("javac"):
    print("SKIP idle rules: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SAVER / "IdleRule.java"), str(ROOT / "tests/IdleRuleRun.java"),
                    str(SETUP / "IdleSetRule.java"), str(ROOT / "tests/IdleSetRun.java")], check=True, capture_output=True)
    for cls in ("sa.urtv.screensaver.IdleRuleRun", "sa.urtv.home.setup.IdleSetRun"):
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, cls], capture_output=True, text=True).stdout
        print(out.strip())
        check("every case of " + cls.rsplit(".", 1)[1] + " ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
