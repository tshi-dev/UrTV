#!/usr/bin/env python3
"""The player's decisions that need no Android, run for real (tests/PlayerLogicRun.java): a Pause while the media loads (F21), the Fajr
voice and its migration, the short adhan's fade, the TV's volume after an adhan (F22), and what a failure says. Also: the Fajr
recordings are the ones whose files say Fajr, and the service is wired to these rules (read from the source).
Needs javac; without it the runs are skipped.  Run: python3 tests/player_logic_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

text = lambda n: (T / n).read_text(encoding="utf-8")
adhan, service, prefs, receiver = text("Adhan.java"), text("MusicService.java"), text("Prefs.java"), text("AdhanReceiver.java")

# the four Fajr recordings are the ones whose files are Fajr adhans
urls = re.findall(r'"(https://archive\.org/download/[^"]+\.(?:mp3|MP3))"', adhan)
check("21 recordings listed", len(urls) == 21, len(urls))
by_url = [i + 1 for i, u in enumerate(urls) if "fajr" in u.lower()]
check("the Fajr recordings by their files: 10, 13, 17, 20", by_url == [10, 13, 17, 20], by_url)

# F21: the service follows the wish and the generation
check("a Pause while loading is remembered (the service pauses through PlayIntent)", "if (player != null && !playing && wish.wants())" in service and "wish.pause();" in service)
check("prepared starts only if playing is still wanted, and a stale player does nothing", "if (!wish.current(gen)) return;   // an older" in service and "if (!wish.wants())" in service)
check("an explicit Resume while loading is remembered and starts once", "if (!prepared) {   // still loading: it plays when it arrives" in service)
check("Toggle while loading pauses", "if (playing || (buffering && wish.wants())) pause(); else resume();" in service)
check("Stop makes every callback stale", "wish.stop();" in service)
check("a retry after a stop or another pick does nothing", "wish.retry(gen, playing)" in service)
check("completion and error callbacks of a replaced player do nothing", service.count("wish.current(gen)") >= 4)

# F22: one way back for the volume; a refused focus and a failed adhan give it back
check("the volume goes back through one function that leaves a changed volume alone", "AdhanVolume.restore(" in service and service.count("private void adhanVolumeBack()") == 1)
check("a refused audio focus for the adhan ends it (volume back, what was paused goes on)", "} else if (source == Prefs.SOURCE_ADHAN) {\n                finishAdhan();" in service)
check("the level the adhan set is kept on disk with the saved volume", "setAdhanLevel(adhanLevel)" in service and "adhanLevel()" in prefs)

# Fajr voice
check("the alarm carries which prayer it is for, and the receiver plays Fajr's voice at Fajr",
      "EXTRA_PRAYER" in text("AdhanScheduler.java") and "prefs.playVoiceFor(context, prayer == PrayerTimes.FAJR)" in receiver)
check("choosing a voice for one never moves the other (the Fajr choice is written down first)", prefs.count("freezeFajr();") >= 3)
check("the usual voice is never a Fajr recording", "AdhanVoices.normalVoice(" in prefs)

# the short adhan: not for previews
check("the short adhan is set only for a real adhan and cut by its own timer", "previewSeconds == 0 ? AdhanVoices.length(prefs.adhanTakbir(), prefs.adhanShort()) : AdhanVoices.FULL;" in service and "shortTick" in service)

# the sleep timer's rows show where the focus is (a report: no highlight on them)
R = ROOT / "android/launcher/src/main/res"
check("the sleep timer's rows draw the focus (a selector with the gold frame for the focused state)",
      "@drawable/row_focusable" in (R / "layout/item_sleep.xml").read_text(encoding="utf-8")
      and 'state_focused="true" android:drawable="@drawable/row_selected"' in (R / "drawable/row_focusable.xml").read_text(encoding="utf-8"))

# counters
check("rebuffers, errors and the last error are counted", "rebuffers++;" in service and "errors++;" in service and "lastError =" in service)
check("the status line carries them", all(k in text("MusicReceiver.java") for k in ("|rebuffers=", "|errors=", "|last_error=", "|last_error_at=", "|rebuffer_ms=")))
player = text("PlayerActivity.java")
check("a held left/right seeks through SeekPace (one seek per 250 ms), both ways",
      player.count("if (seekPace.due(event.getRepeatCount(), event.getEventTime())) MusicService.seekBy(this, ") == 2)
strings = (ROOT / "android/launcher/src/main/res/values/strings.xml").read_text(encoding="utf-8")
check("the Player's hint says what left/right do there, in the card's words («يمين/يسار: ١٠ ثوانٍ»)", "يمين/يسار: ١٠ ثوانٍ" in strings)

if not shutil.which("javac"):
    print("SKIP player logic: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java")] + [str(T / n) for n in ("PlayIntent.java", "AdhanVoices.java", "AdhanVolume.java", "PlayerFault.java", "SeekPace.java")]
                   + [str(ROOT / "tests/PlayerLogicRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PlayerLogicRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t") for l in out.split("\n") if l]
fajr = rows[0][1].strip(",")
check("the code's Fajr list is the files' Fajr list", fajr == "10,13,17,20", fajr)
for name, res in rows[1:]:
    check(name, res == "ok")
check("every check ran", len(rows) >= 28, len(rows))
sys.exit(1 if failed else 0)
