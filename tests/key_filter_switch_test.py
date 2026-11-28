#!/usr/bin/env python3
"""R-408: a third-party key remapper stopped working: Android gives a key to every accessibility service that filters keys in turn, and the keys
our add-on keeps (Home on a Google TV, the Settings key, the remote's app buttons) never reached it. «فلتر أزرار الريموت» (on by default):
`optimize/dock.sh key-filter off` and the add-on never asks for the remote's keys. Documented in docs/SETTINGS.md and docs/TVS.md.
tests/KeyFilterSwitchRun.java runs the rule. Run: python3 tests/key_filter_switch_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def text(p): return p.read_text(encoding="utf-8") if p.exists() else ""

svc = text(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java")
prov = text(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasCallProvider.java")
fk = svc.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("filterKeys asks the switch last, before the filter is set", "want = KeyFilterSwitch.want(keyFilter(this), want);" in fk
      and fk.index("KeyFilterSwitch.want(") < fk.index("setServiceInfo(info)"))
check("a key that comes while it is off goes on untouched", "if (!keyFilter(this)) return false;" in svc.split("protected boolean onKeyEvent(KeyEvent e) {")[1].split("return keyEvent(e);")[0])
check("on by default", 'getBoolean(ExtrasWords.KEY_FILTER, true)' in svc)
check("a change judges the filter again at once", "s.main.post(s::filterKeys)" in svc.split("static void setKeyFilter(Context c, boolean on) {")[1].split("\n    }\n")[0])
check("the add-on's provider takes on/off and answers the state", "ExtrasA11yService.setKeyFilter(getContext(), ExtrasWords.KEY_FILTER_ON.equals(arg))" in prov
      and "out.putBoolean(ExtrasWords.KEY_FILTER, ExtrasA11yService.keyFilter(getContext()));" in prov)
check("the home's DockReceiver and its manifest", 'case "sa.urtv.home.KEY_FILTER":' in text(HOME / "DockReceiver.java")
      and 'android:name="sa.urtv.home.KEY_FILTER"' in text(ROOT / "android/launcher/src/main/AndroidManifest.xml") and "Extras.keyFilter(c," in text(HOME / "DockReceiver.java"))
dock = text(ROOT / "optimize/dock.sh")
check("dock.sh key-filter on|off|status", 'on|off|status) send KEY_FILTER "--es mode $2" ;;' in dock and "optimize/dock.sh key-filter on|off|status" in dock)
check("the shared copy for the add-on", "'KeyFilterSwitch.java'" in text(ROOT / "android/extras/build.gradle").split("def shared = ")[1].split("\n")[0])
check("documented: the conflict and the switch", "`optimize/dock.sh key-filter off`" in text(ROOT / "docs/SETTINGS.md") and "key-filter off" in text(ROOT / "docs/TVS.md"))

if not shutil.which("javac"):
    print("SKIP KeyFilterSwitchRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(HOME / "KeyFilterSwitch.java"), str(ROOT / "tests/KeyFilterSwitchRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.KeyFilterSwitchRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of KeyFilterSwitchRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
