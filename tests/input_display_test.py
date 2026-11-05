#!/usr/bin/env python3
"""The names in the sources list: the owner's name, else the HDMI-CEC device's own, else the TV's; and the standby mark. InputDisplay.java (no
Android in it) on tests/input_display_cases.tsv, then the wiring as source checks: only public SDK API and no new permission, the popup's
order stays the TV's own names', «أسماء المصادر» and `dock.sh input-names` show the CEC name, no device name is ever logged, and the docs.
Needs javac for the Java cases; without it they are skipped with a note.
Run: python3 tests/input_display_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def read(p):
    return (ROOT / p).read_text(encoding="utf-8")

labels = (HOME / "InputLabels.java").read_text(encoding="utf-8")
disp = (HOME / "InputDisplay.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
act = (HOME / "InputNamesActivity.java").read_text(encoding="utf-8")
recv = (HOME / "DockReceiver.java").read_text(encoding="utf-8")
manifest = read("android/launcher/src/main/AndroidManifest.xml")

check("InputDisplay is pure: no android import", "import android" not in disp)
check("InputDisplay credits talkies", "talkies" in disp and "THIRD_PARTY_NOTICES.md" in disp)
code = re.sub(r"/\*.*?\*/|//[^\n]*", "", labels, flags=re.S)
check("reading the CEC device: public API only (parent id, type, label: no HdmiDeviceInfo, which is @SystemApi)",
      "getParentId()" in code and "TvInputInfo.TYPE_HDMI" in code and "loadLabel(c)" in code and "HdmiDeviceInfo" not in code and "getHdmiDeviceInfo" not in code)
check("the state is read from TvInputManager.getInputState, standby is INPUT_STATE_CONNECTED_STANDBY", "tim.getInputState(id)" in labels and "INPUT_STATE_CONNECTED_STANDBY" in labels)
check("no new permission: the manifest has no TV_INPUT_HARDWARE / HDMI_CEC / ACCESS_TV_DESCRAMBLER", not re.search(r"TV_INPUT_HARDWARE|HDMI_CEC|TV_DESCRAMBLER|CAPTURE_TV", manifest))
check("the sorting is still by the TV's own names (a CEC name or a standby mark moves nothing)",
      "Collections.sort(inputs, (x, y) -> own(c, x).compareToIgnoreCase(own(c, y)))" in labels)
check("popup: the row is InputLabels.rowLabel (name + standby mark), opened by its id", "InputLabels.rowLabel(this, t, given)" in main and "openPassthroughInput(t)" in main)
check("rowLabel asks InputDisplay.label with the standby state; label() is the name only",
      "InputDisplay.label(given.get(key(t)), cecRaw(c, t), own(c, t)," in labels and "InputDisplay.name(given.get(key(t)), cecRaw(c, t), own(c, t))" in labels)
check("screen «أسماء المصادر»: the dim hint line and the editor's hint are the CEC name when there is one", act.count("InputLabels.fallback(this, t)") == 2)
check("receiver: «input-names» keeps its four columns and adds the CEC name and the state after them",
      'append(ours == null ? "" : ours)' in recv.replace("\n", " ").replace("  ", " ") or '.append(" | ").append(ours == null ? "" : ours)' in recv)
check("receiver: the added columns are the cleaned CEC name and the state word",
      "InputDisplay.cecName(InputLabels.cecRaw(c, t))" in recv and "InputLabels.stateWord(InputLabels.state(c, t))" in recv)
check("privacy: no log, print or file write in the naming code", all(not re.search(r"Log\.|System\.(out|err)|printStackTrace|FileWriter|openFileOutput", s) for s in (labels, disp)))
check("every String.format has Locale.ROOT (none here)", "String.format" not in labels + disp)
check("docs/SETTINGS.md says the CEC name, the standby mark and the new columns",
      "CEC" in read("docs/SETTINGS.md") and "وضع الاستعداد" in read("docs/SETTINGS.md") and "ours | CEC name | state" in read("docs/SETTINGS.md").replace("\\|", "|"))
check("docs/FEATURES.md tells about the device name and standby", "وضع الاستعداد" in read("docs/FEATURES.md"))
check("CHANGELOG has a line", "standby" in read("CHANGELOG.md").lower() and "CEC" in read("CHANGELOG.md"))
check("tests/run.sh runs this test", "tests/input_display_test.py" in read("tests/run.sh"))

if not shutil.which("javac"):
    print("SKIP input display: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in read("tests/input_display_cases.tsv").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "InputDisplay.java"), str(HOME / "InputNames.java"),
                    str(ROOT / "tests/InputNamesRun.java"), str(ROOT / "tests/InputDisplayRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.InputDisplayRun", str(ROOT / "tests/input_display_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    want = c[5]
    check(f"input display {c[0]}: {c[6]}", g == want, f"{g!r}, wanted {want!r}")
check("every case answered", len(got) > len(cases), (len(got), len(cases)))
sys.exit(1 if failed else 0)
