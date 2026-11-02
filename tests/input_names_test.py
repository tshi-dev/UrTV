#!/usr/bin/env python3
"""«أسماء المصادر»: the names the owner gives the TV's inputs. InputNames.java (no Android in it) on tests/input_names_cases.tsv, then
the wiring as source checks: the sources popup shows the saved name but keeps the TV's own order, the screen is behind the family lock
and exported like DockActivity, the settings row opens it by class name, the receiver's actions, privacy (no name in a log, none in the
settings export), and the docs. Needs javac for the Java cases; without it they are skipped with a note.
Run: python3 tests/input_names_test.py"""
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

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
labels = (HOME / "InputLabels.java").read_text(encoding="utf-8")
act = (HOME / "InputNamesActivity.java").read_text(encoding="utf-8")
recv = (HOME / "DockReceiver.java").read_text(encoding="utf-8")
manifest = read("android/launcher/src/main/AndroidManifest.xml")
saver = read("android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")

# the sources popup
popup = main.split("private void showInputsNow", 1)[1].split("/** «مؤقت النوم»", 1)[0]
check("popup: each input shows InputLabels.rowLabel (the saved name, else the CEC device's, else the TV's own; tests/input_display_test.py)", "InputLabels.rowLabel(this, t, given)" in popup)
check("popup: the inputs come from InputLabels.reported, sorted by the TV's own names (renaming moves nothing)",
      "InputLabels.reported(this)" in popup and "Collections.sort(inputs, (x, y) -> own(c, x).compareToIgnoreCase(own(c, y)))" in labels)
check("popup: the input is still opened by its id, not by its name", "openPassthroughInput(t)" in popup)
check("MainActivity: one place produces input labels (the old inputLabel is gone)", "inputLabel(" not in main)
check("the saved names are keyed by the HDMI port (getParentId) when the input has one, else getId(), in the launcher's preferences",
      "given.get(key(t))" in labels and "t.getParentId()" in labels and "return parent != null && !parent.isEmpty() ? parent : t.getId();" in labels and "Dock.prefs(c)" in labels)
check("the screen, the sources popup and ADB all use that key", "InputLabels.key(" in act and "InputLabels.key(t)" in recv and "t.getId()" not in act and "t.getId()" not in recv.split("INPUT_NAMES")[1].split("SLEEP_TV")[0])

# the screen
check("screen: asks for the PIN itself, with «قفل الإعدادات» (the same target as «ترتيب المفضلة»)",
      "ParentalPolicy.targets(ParentalPolicy.ARRANGE_SCREEN, null)" in act and "Parental.guard(this" in act and "screen.setVisibility(View.INVISIBLE)" in act)
check("screen: a dialog or the PIN keypad is dismissed when the screen pauses", "PinPrompt.dismiss(this)" in act and "editor.dismiss()" in act)
check("screen: lists only the inputs the TV reports now", "InputLabels.reported(this)" in act)
check("screen: the name is cleaned by InputNames.clean before it is kept; a name that can't be kept leaves the box open",
      "InputNames.clean(in.getText().toString())" in act and "setPositiveButton(S.t(\"حفظ\"), null)" in act)
check("screen: a Latin name keeps its order in the Arabic row (first-strong text direction)", "TEXT_DIRECTION_FIRST_STRONG" in act)
check("screen: Arabic copy says «اختيار», not «OK»", "OK" not in re.sub(r"[A-Za-z]+\.", "", " ".join(re.findall(r'"[^"]*[؀-ۿ][^"]*"', act))))
m = re.search(r'<activity\s+android:name="\.InputNamesActivity"(.*?)/>', manifest, re.S)
dock = re.search(r'<activity\s+android:name="\.DockActivity"(.*?)/>', manifest, re.S)
check("manifest: exported exactly as DockActivity is (no permission), landscape, no action bar",
      bool(m) and 'android:exported="true"' in m.group(1) and "android:permission" not in m.group(1) and "intent-filter" not in m.group(1)
      and bool(dock) and "android:permission" not in dock.group(1) and 'android:screenOrientation="landscape"' in m.group(1))
check("screensaver settings: a row «أسماء المصادر» in the home-screen section opens the launcher's screen by class name",
      'row("أسماء المصادر"' in saver and 'sa.urtv.home.InputNamesActivity' in saver
      and saver.index('header("الشاشة الرئيسية (تلفزيونك)")') < saver.index('row("أسماء المصادر"') < saver.index('header("جديد المسلسلات")'))

check("the sources popup row stays one line, ellipsized at the end, direction by its first letter (a long name cannot push the list out)",
      all(x in main.split("private TextView inputRow(String label, Runnable action) {")[1].split("return r;")[0] for x in ("setSingleLine(true)", "TruncateAt.END", "TEXT_DIRECTION_FIRST_STRONG")))
check("the name box asks the keyboard for no suggestions (the owner's names stay out of its dictionary)", "TYPE_TEXT_FLAG_NO_SUGGESTIONS" in act)
# the receiver
check("receiver: INPUT_NAMES lists id | shown | the TV's name | ours, only the reported inputs",
      '"sa.urtv.home.INPUT_NAMES"' in recv and "InputLabels.reported(c)" in recv)
check("receiver: SET_INPUT_NAME validates the id and the name, refuses an input the TV does not report, empty clears",
      "InputNames.validId(id)" in recv and "InputNames.clean(raw)" in recv and "does not report that input" in recv and "InputLabels.set(c, id, name)" in recv)
check("receiver: needs the shell's DUMP like the others (the one receiver entry in the manifest)", 'android.permission.DUMP' in manifest and 'DockReceiver' in manifest)

# privacy
sources = [HOME / n for n in ("InputNames.java", "InputLabels.java", "InputNamesActivity.java")]
check("privacy: no Log call, no print and no journal in the new files, and none writes a name outside the launcher's preferences",
      all(not re.search(r"Log\.|System\.(out|err)|printStackTrace|FileWriter|openFileOutput", s.read_text(encoding="utf-8")) for s in sources))
check("privacy: the receiver's input actions log nothing", not re.search(r"Log\.", recv))
export = read("optimize/settings_move.py")
check("privacy: the settings export (an allow-list) does not know the input names", "input_names" not in export and "InputNames" not in export and "input-name" not in export)
for f in ("optimize/share.py", "optimize/share.sh"):
    check(f"privacy: {f} does not pick the input names up", "input_names" not in read(f) and "input-name" not in read(f))

# docs
check("docs/SETTINGS.md (agents) has the row with both commands, and says the export leaves the names out",
      "dock.sh input-names" in read("docs/SETTINGS.md") and "dock.sh input-name " in read("docs/SETTINGS.md") and "not in the settings export" in read("docs/SETTINGS.md"))
check("docs/FEATURES.md (people) tells about «أسماء المصادر»", "أسماء المصادر" in read("docs/FEATURES.md") and "dock.sh input-names" in read("docs/FEATURES.md"))

# the helper's cases
if not shutil.which("javac"):
    print("SKIP input names: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in read("tests/input_names_cases.tsv").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "InputNames.java"), str(ROOT / "tests/InputNamesRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.InputNamesRun", str(ROOT / "tests/input_names_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    check(f"input names {c[0]}: {c[5]}", g == c[4], f"{g!r}, wanted {c[4]!r}")
check("every case answered", len(got) > len(cases), (len(got), len(cases)))
sys.exit(1 if failed else 0)
