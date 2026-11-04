"""Tester step 28b (build 183): source checks of the fixes that no pure-Java run reaches.
Run: python3 tests/step28b_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    if not ok:
        failed += 1


def text(p):
    return Path(p).read_text(encoding="utf-8")


main = text(HOME / "MainActivity.java")
check("1: ▲ from a home card goes to a pill that is on screen (the pill it came from may have left the row), by its own key handler; OK is left to the click",
      "keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;" in main and "pv.isShown() && pv.isFocusable()" in main and "lastPillId = v.getId()" in main)
tpl = text(ROOT / "src/template.html")
check("2: the matches list keeps its day when a team is followed or unfollowed (the new team's games must not move it), and Back on the open card draws nothing of it",
      "if(ok && b.k !== 'remind') MP.picked = true;" in tpl and "mpBack(); return e.preventDefault(); }" in tpl)
mf, md = text(HOME / "MarketFile.java"), text(HOME / "MarketData.java")
check("3: with no fx file from CI and no rates yet the app asks Yahoo itself (two batches of the 35 codes) and keeps the result as CI's file",
      "private static String fxFromYahoo()" in mf and "if (fxText == null || MarketData.parseFx(fxText).isEmpty()) {" in mf and "from += 18" in mf and "static String fxFile(" in md)
ms = text(HOME / "tilawah/MusicService.java")
check("4: a station that gives nothing in 20 s is retried once with its own address straight to the player, then says «تعذّر التشغيل»; the host (never the token) is logged",
      "radioDirectTried" in ms and "trying \" + hostOf(radioAddress) + \" as given" in ms and 'S.t("تعذّر التشغيل")' in ms)
check("5: a Tadawul row with no timestamp and no close is read from fulldayPrice, its time is now", 'meta.get("fulldayPrice")' in md and "System.currentTimeMillis() / 1000.0" in md)
rc = text(HOME / "tilawah/AdhanReceiver.java")
check("6 (the owner's choice B): a muted TV is unmuted for the adhan and muted again at its end; the mute is read FIRST (before the unmute) and logged as tv_muted_before; the re-mute result is logged; every end goes through adhanVolumeBack (over, stopped, left, the screen off, the service gone, a restart after a crash)",
      "final boolean mutedBefore = tvMuted();" in ms and '" tv_muted_before=" + mutedBefore' in ms and ms.index("tv_muted_before=") < ms.index("adhanVolumeUp();\n        boolean comeBack")
      and 'adhan end: re-mute " + (audio.isStreamMute(AudioManager.STREAM_MUSIC) ? "done" : "FAILED' in ms and "adhan volume: the TV was muted, unmuted for the adhan" in ms
      and ms.count("adhanVolumeBack();") >= 5 and "if (source == Prefs.SOURCE_ADHAN) finishAdhan();" in ms and "savedVolume = prefs.savedTvVolume();" in ms
      and "muted(context)" not in rc and "adhan skipped: the TV is muted" not in rc and "adhan skipped: the TV is muted" not in ms)
sc = text(HOME / "setup/SetupCommunity.java")
check("7: the community list's name is in the report also when our own lists know the TV", "choice.meta != null ? \" (\" + choice.meta.name + \")\" : \"\"" in sc)
sys.exit(1 if failed else 0)
