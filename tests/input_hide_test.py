#!/usr/bin/env python3
"""4.5 «المصادر»: hide an input (an HDMI port nothing is plugged into, AV) from the sources popup; it stays in the inputs screen, dimmed, with «إظهار»
(mockup docs/mockups/tools-45/01-inputs.png). The rule (InputHide) runs in tests/InputHideRun.java; the wiring is read from the source.
Run: python3 tests/input_hide_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
lab, main, act, rec, dock = read(H / "InputLabels.java"), read(H / "MainActivity.java"), read(H / "InputNamesActivity.java"), read(H / "DockReceiver.java"), read(ROOT / "optimize/dock.sh")
check("no Android in the rule; tvOS Launcher credited for the idea", "import android" not in read(H / "InputHide.java") and "tvOS-Launcher" in read(H / "InputHide.java"))
check("the sources popup lists InputLabels.shown (the hidden ones left out); the inputs screen still lists them all",
      "List<TvInputInfo> inputs = InputLabels.shown(this);" in main and "List<TvInputInfo> inputs = InputLabels.reported(this);" in act)
check("kept by the same key as the names (the port)", "InputLabels.setHidden(this, id, !off)" in act and "final String id = InputLabels.key(t);" in act)
check("a hidden input is dimmed and offers «إظهار»", 'off ? S.t("إظهار") : S.t("إخفاء")' in act and "if (off) c.setAlpha(0.5f);" in act)
hide = rec.split('case "sa.urtv.home.INPUT_HIDE": {')[1].split("return;\n            }")[0]
check("ADB: INPUT_HIDE takes a valid id and hide|show, refuses to hide an input the TV does not report", "InputHide.validId(id)" in hide and "does not report that input" in hide)
check("input-names says hidden/shown", '"hidden" : "shown"' in rec)
check("dock.sh input-hide / input-show", 'send INPUT_HIDE "--es id' in dock and "input-hide:2|input-show:2)" in dock)
out = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), "input-hide", "bad id"], capture_output=True, text=True, cwd=ROOT)
check("dock.sh refuses a bad id before asking the TV", out.returncode == 1 and "not an input id" in out.stdout, out.stdout + out.stderr)
en = read(ROOT / "i18n/en/home.tsv")
check("the new words have their English", all(("\n" + w + "\t") in en for w in ("إظهار", "إخفاء", "رجع المصدر لقائمة «المصادر»", "انشال المصدر من قائمة «المصادر»")))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "InputHide.java"), str(ROOT / "tests/InputHideRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.InputHideRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every InputHide case ran", "ALL PASS" in out, out[-300:])
sys.exit(1 if failed else 0)
