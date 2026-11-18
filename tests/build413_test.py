#!/usr/bin/env python3
"""4.4, the tester's build 413 (Home 100 % in normal use; T9 with the owner's real remote: the add-on's Home key catch 3/3, Google TV never started):
1. the Home key catch is ON by default on a Google TV (launcherx installed), only while the person's home is ours, the window watch kept as the
   fallback; a switch in the settings («زر الرئيسية يفتح تلفزيونك مباشرة») and `dock.sh home-key on|off|status`;
2. the Home step is judged only after Android bound the add-on (up to 10 s), and the result page reads the role and the add-on again for 10 s;
3. the fight guard pauses at the 6th burst in 4 s (HomeWatchRun runs it; here the constant);
4. the intro's first focus is the reading area itself, not its first button («English»).
Read from the source. Run: python3 tests/build413_test.py"""
import subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return (ROOT / p).read_text(encoding="utf-8")
svc = read("android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java")
hk = svc.split("static boolean homeKey(Context c) {")[1].split("\n    }\n")[0]
check("1. the catch: the person's switch wins once set, else on where Google TV's home is installed",
      "if (sp.contains(ExtrasWords.HOME_KEY)) return sp.getBoolean(ExtrasWords.HOME_KEY, false);" in hk and 'return installed(c, "com.google.android.apps.tv.launcherx");' in hk)
check("1. it acts only while the person's home is ours", "private boolean homeKeyActive() { return homeKey(this) && (wanted(this) || oursChosen()); }" in svc
      and "if (e.getKeyCode() == KeyEvent.KEYCODE_HOME && homeKeyActive()) return homeKeyCaught(e);" in svc and "|| homeKeyActive();" in svc)
check("1. the key filter follows when the person's home changes", "if (s != null) s.main.post(s::filterKeys);   // the Home key catch follows the person's home" in svc)
check("1. the window watch is still there as the fallback", "watch.decide(on, pkg, cls, stockHomes, chosen || wantedNow, now, fight)" in svc)
dock = read("optimize/dock.sh")
check("1. dock.sh home-key sends HOME_KEY on|off|status", 'on|off|status) send HOME_KEY "--es mode $2" ;;' in dock)
out = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), "home-key", "maybe"], capture_output=True, text=True, cwd=ROOT)
check("1. a wrong word is refused before the TV is asked", out.returncode == 1 and "usage: optimize/dock.sh home-key" in out.stdout, out.stdout + out.stderr)
act, bridge = read("android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java"), read("android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java")
prov = read("android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java")
check("1. the settings switch: shown when the add-on answers, flips through the home (4.4, QA-7: both asked on a thread, the answer kept)",
      'final Boolean homeKey = homeKeySeen();' in act and 'if (homeKey != null)' in act
      and 'row("زر الرئيسية يفتح تلفزيونك مباشرة", homeKey ? "مفعّل" : "متوقف"' in act and "setHomeKey(!homeKey);" in act
      and 'NowPlayingBridge.homeKey(app, null)' in act and 'NowPlayingBridge.homeKey(app, on ? "on" : "off")' in act)
check("1. the bridge and the provider (same key only)", '"home_key", set, null' in bridge and 'if ("home_key".equals(method)' in prov
      and "sa.urtv.home.Extras.homeKey(getContext()" in prov.split('if ("home_key".equals(method)')[1].split("return b;")[0]
      and 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING")' in prov.split('if ("home_key".equals(method)')[1].split("return b;")[0])
check("1. its English and marker", "\nزر الرئيسية يفتح تلفزيونك مباشرة\t" in read("i18n/en/saver.tsv") and 'S.n("زر الرئيسية يفتح تلفزيونك مباشرة")' in read("android/app/src/main/java/sa/urtv/screensaver/Words.java"))
check("1. the docs say on by default on a Google TV", "**On by default on a Google TV**" in read("docs/SETTINGS.md") and "ON by default on a Google TV from build 413" in read("docs/HOMEDOCTOR.md"))
eng = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java")
fw = eng.split("Result finishWatch(boolean watchOk) throws IOException {")[1].split("\n    }\n")[0]
check("2. the Home step is judged after the add-on is bound (up to 10 s), then the key", "if (watchOk && boundWithin(BOUND_WAIT_MS) && homeKeyOpensOurs())" in fw and "BOUND_WAIT_MS = 10_000;" in eng)
bw = eng.split("boolean boundWithin(int ms) throws IOException {")[1].split("\n    }\n")[0]
check("2. bound is Android's own answer, read once a second", "dumpsys accessibility | grep -m1 'Bound services:'" in bw and "sleeper.sleep(1000);" in bw and "if (waited >= ms) return false;" in bw)
sa = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java")
hl = sa.split("private void homeLines(")[1].split("\n    }\n")[0]
check("2. the result page reads the role and the add-on again once a second, up to 10 s, until both are fine", "homeLines(roleLine, watchLine, RESULT_RECHECK);" in sa and "RESULT_RECHECK = 10;" in sa
      and "if ((ours && bound) || left <= 0" in hl and "postDelayed(" in hl and "left - 1" in hl)
check("3. the guard window is 4 s", "static final long WINDOW_MS = 4_000," in read("android/launcher/src/main/java/sa/urtv/home/HomeWatch.java"))
box = read("android/launcher/src/main/java/sa/urtv/home/Box.java")
fs = box.split("void focusSelf() {")[1].split("\n        }\n")[0]
check("4. the reading area takes the focus itself, not its first button", "scroll.focusSelf();" in box and fs.index("FOCUS_BEFORE_DESCENDANTS") < fs.index("requestFocus()") < fs.index("FOCUS_AFTER_DESCENDANTS"))
sys.exit(1 if failed else 0)
