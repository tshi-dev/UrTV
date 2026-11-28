#!/usr/bin/env python3
"""4.4 beta 4: the «يُتلى الآن» card stayed on the home while a surah or a station was paused, and nothing closed it. Now Back on the focused card closes
it (NowPlaying.close: what it holds stops, the focus goes to the pills), its hint while paused says so, and a card paused for 10 minutes hides by
itself (NowCardRule, looked at on every change and on the clock's minute; the playback stays paused for the player until the service's own stop).
NowCardRule and the hint (no Android) on cases, then the wiring.
Run: python3 tests/now_card_close_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
TIL = HOME / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
np = (TIL / "NowPlaying.java").read_text(encoding="utf-8")
upd = main[main.index("private void updateNowPlaying()"):]
upd = upd[:upd.index("\n    }\n")]
check("updateNowPlaying: shown by NowCardRule (paused 10 minutes: gone), the paused time kept between looks",
      "nowPausedSince = NowCardRule.pausedSince(s.show, s.playing, nowPausedSince, now);" in upd and "if (!NowCardRule.visible(s.show, s.playing, nowPausedSince, now)) {" in upd)
check("the clock's minute tick looks at the card again (so the 10 minutes are kept without a change from the player)", "updateNowPlaying();   // the minutes left of its sleep timer" in main)
key = main[main.index("// 4.4 beta 4: Back on the «يُتلى الآن» card closes it"):]
key = key[:key.index("return true;\n        }") + 12]
check("Back on the focused card: on its release (not a cancelled one) NowPlaying.close, the focus to the pills, the card gone; the press is taken too",
      "e.getKeyCode() == KeyEvent.KEYCODE_BACK && nowCard != null && getCurrentFocus() == nowCard" in key and "e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()" in key
      and "NowPlaying.close(this);" in key and "pills.get(0).requestFocus();" in key and key.rstrip().endswith("return true;"))
check("Back on the card is looked at before the home's own Back (backExitTakes)", main.index("// 4.4 beta 4: Back on the «يُتلى الآن» card closes it") < main.index("if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && backExitTakes(e)) return true;"))
check("NowPlaying.close stops the playback (MusicService.stopAction: the source goes, CHANGED is sent)", "public static void close(Context c) { MusicService.stopAction(c); }" in np)
check("the paused card's hint is hintPaused, the playing one's as before", "MusicService.isPlaying() || MusicService.isBuffering() ? NowPlayingText.hint(radio) : NowPlayingText.hintPaused()" in np)
check("English: the paused hint has its line", "اختيار: تشغيل، رجوع: إغلاق، مطولًا: فتح\tOK: play, Back: close, hold: open" in (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP now card close: no javac here")
    sys.exit(1 if failed else 0)
M = 60_000
T = 1_765_000_000_000
CASES = [
    ("playing: shown, no paused time", f"1,1,0,{T}", f"0 1"),
    ("first seen paused: shown, the time starts now", f"1,0,0,{T}", f"{T} 1"),
    ("paused 9 minutes: still shown", f"1,0,{T},{T + 9 * M}", f"{T} 1"),
    ("paused 10 minutes: hidden", f"1,0,{T},{T + 10 * M}", f"{T} 0"),
    ("played again after that: shown, the time forgotten", f"1,1,{T},{T + 11 * M}", "0 1"),
    ("nothing on: hidden", f"0,0,{T},{T + M}", "0 0"),
    ("a paused time in the future (the clock went back): starts again from now", f"1,0,{T + 5 * M},{T}", f"{T} 1"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "NowCardRule.java"),
                        str(TIL / "NowPlayingText.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/NowCardHint.java"), str(ROOT / "tests/NowCardRuleRun.java")], capture_output=True, text=True)
    check("NowCardRule compiles", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.NowCardRuleRun", *[c[1] for c in CASES], "hint"], capture_output=True).stdout.decode("utf-8").strip().split("\n")
        for (name, _, want), got in zip(CASES, out):
            check(name, got == want, f"{got!r}, wanted {want!r}")
        check("the paused hint: «اختيار: تشغيل، رجوع: إغلاق، مطولًا: فتح» (no «·»)", out[-1] == "اختيار: تشغيل، رجوع: إغلاق، مطولًا: فتح" and "·" not in out[-1], out[-1])
sys.exit(1 if failed else 0)
