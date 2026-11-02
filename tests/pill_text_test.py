#!/usr/bin/env python3
"""The home screen's pills (PillText.java): what a pill shows with and without the focus, the sleep pill's name with the minutes
left, the network pill's name, on tests/pill_text_cases.tsv. Also the shape of Pill.java and its use in MainActivity: every pill
keeps its place in the row (same order, same D-pad ends), a fixed height, a ~150 ms width animation, its name as the
content description. Needs javac; without it the Java cases are skipped with a note.   Run: python3 tests/pill_text_test.py"""
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

pill = (HOME / "Pill.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("Pill: the width animation takes about 150 ms", re.search(r"MS = 150", pill) is not None)
check("Pill: the name is the content description (a screen reader hears it though only the icon shows)", "setContentDescription(text)" in pill)
check("Pill: a focused pill is its own focus target (focusable, children are not)", "setFocusable(true)" in pill and "setFocusable" not in pill.split("setFocusable(true)", 1)[1])
check("Pill: only the padding and the name's window change; the pill's own size params (its height) are never touched",
      pill.count("getLayoutParams()") == 2 and "clip.getLayoutParams()" in pill and "name.getLayoutParams()" in pill)
order = re.findall(r"Arrays\.asList\(([^)]*)\)", main.split("List<Pill> row", 1)[1].split("\n", 1)[0])
check("MainActivity: the pills left in the row are sources, then tasks (with the screensaver app, unless «زر المهام» hides it), the control center and tilawah (the sleep, screensaver, network and settings pills are tiles of the control center)",
      order == ["inputs"] and "if (isInstalled(SAVER_PKG) && ConfigSnapshot.read(this).tasksPillOn()) row.add(tasks);" in main and "if (centerWanted()) row.add(centerPill);" in main and "row.add(tilawah)" in main
      and main.index("row.add(tasks)") < main.index("row.add(centerPill)") < main.index("row.add(tilawah)"), order)
check("MainActivity: every pill is a Pill with a fixed 30 dp row (the row's height is reserved)",
      "right.addView(p, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(10), 0))" in main and "TextView tasks" not in main and "TextView saver" not in main)
check("MainActivity: D-pad ends and order are unchanged (wireFocus and moveFocusSideways still walk the pills list)",
      "p.setNextFocusLeftId(i == 0 ? p.getId() : pills.get(i - 1).getId())" in main and "int next = right == Dir.rtl() ? index - 1 : index + 1;" in main)
check("MainActivity: the control center's pill is gold while the sleep timer runs (its tile says how long is left)", "centerPill.setAccent(SleepTimer.minutesLeft(this) > 0)" in main
      and "wifiPill" not in main and "sleepPill" not in main)
check("MainActivity: OK on the screensaver tile starts the screensaver (the preview activity, a fresh task), a long OK opens its settings, both through the family lock (the pill's old targets)",
      'guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", MainActivity.this::startSaver)' in main
      and 'guarded(ParentalPolicy.PILL_SAVER_SETTINGS, SAVER_PKG, "شاشة التوقف", () -> openPackage(SAVER_PKG))' in main
      and 'SAVER_PKG + ".MainActivity"' in main and "FLAG_ACTIVITY_CLEAR_TASK" in main.split("private void startSaver", 1)[1].split("}", 1)[0])
center = (HOME / "ControlCenter.java").read_text(encoding="utf-8")
check("ControlCenter: OK on the screensaver tile starts it and its line says so; a long OK is only a shortcut to the settings (also in «إعدادات تلفزيونك»)", "host.openSaver()" in center and "host.openSaverSettings()" in center and '"ابدأها الآن"' in center and "مطوّلًا" not in center)

if not shutil.which("javac"):
    print("SKIP pill text: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/pill_text_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "PillText.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/PillTextRun.java")], check=True,
                   capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.PillTextRun", str(ROOT / "tests/pill_text_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    check(f"pill text {c[0]}: {c[5]}", g == c[4], f"{g!r}, wanted {c[4]!r}")
check("every case answered", len(got) > len(cases), (len(got), len(cases)))
sys.exit(1 if failed else 0)
