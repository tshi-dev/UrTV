#!/usr/bin/env python3
"""4.3.4 follow-up (a Sony RMF-TX520P report: «only the numbers, Home, volume and Settings work»): «تعديل المفضلة» and «تعديل التطبيقات» took
EVERY key but the volume and power ones, so a remote's app, colour, channel, media and maker keys did nothing while a mode was on. FavEdit.owns:
the modes keep only the keys they use (the arrows, OK, Back) and the ones that would move the focus behind them; every other key goes on.
The rule runs on a JVM; both modes are checked to use it.
Run: python3 tests/edit_keys_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
for fn in ("editKey", "appsEditKey"):
    body = main.split(f"private boolean {fn}(KeyEvent e) {{")[1].split("\n    }\n")[0]
    check(f"{fn}: a key the mode does not use goes on (its first test, before a release is eaten)",
          "if (AdhanKeys.reserved(k) || !FavEdit.owns(k)) return false;" in body
          and body.index("FavEdit.owns(k)") < body.index("ACTION_DOWN) return true"))

if not shutil.which("javac"):
    print("SKIP the rule on a JVM: no javac")
    sys.exit(1 if failed else 0)
OWN = {4: "BACK", 19: "UP", 20: "DOWN", 21: "LEFT", 22: "RIGHT", 23: "CENTER", 66: "ENTER", 96: "BUTTON_A", 160: "NUMPAD_ENTER",
       61: "TAB", 92: "PAGE_UP", 93: "PAGE_DOWN", 122: "MOVE_HOME", 123: "MOVE_END"}
PASS = {166: "CHANNEL_UP", 167: "CHANNEL_DOWN", 172: "GUIDE", 178: "TV_INPUT", 170: "TV", 183: "PROG_RED", 184: "PROG_GREEN",
        185: "PROG_YELLOW", 186: "PROG_BLUE", 85: "MEDIA_PLAY_PAUSE", 86: "MEDIA_STOP", 87: "MEDIA_NEXT", 88: "MEDIA_PREVIOUS",
        89: "MEDIA_REWIND", 90: "MEDIA_FAST_FORWARD", 126: "MEDIA_PLAY", 127: "MEDIA_PAUSE", 130: "MEDIA_RECORD", 165: "INFO",
        175: "CAPTIONS", 222: "MEDIA_AUDIO_TRACK", 229: "LAST_CHANNEL", 231: "VOICE_ASSIST", 284: "ALL_APPS", 82: "MENU",
        7: "0", 16: "9", 3: "HOME", 176: "SETTINGS", 4001: "a maker's own code", 0: "UNKNOWN"}
codes = list(OWN) + list(PASS)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"),
                    str(HOME / "FavEdit.java"), str(HOME / "ParentalPolicy.java"), str(HOME / "AdhanKeys.java"), str(ROOT / "tests/EditKeysRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.EditKeysRun"] + [str(c) for c in codes], check=True, capture_output=True, text=True).stdout.split()
got = dict(zip(codes, out))
check("the keys the modes use, and the ones that would move the focus, are theirs", all(got.get(c) == "own" for c in OWN),
      {OWN[c]: got.get(c) for c in OWN if got.get(c) != "own"})
check("app, colour, channel, media, number, guide, input, mic and the maker's keys go on (unknown keys are never swallowed)",
      all(got.get(c) == "pass" for c in PASS), {PASS[c]: got.get(c) for c in PASS if got.get(c) != "pass"})
check("an answer for every key", len(out) == len(codes), out)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
