#!/usr/bin/env python3
"""Known bug (tester step 71): the radio stopped silently when the TV updated System WebView (the home's process ends, no error, no restart). «Was playing» is written when a
recitation or a station starts and cleared by every pause and stop of the person's; a home that starts with it still written (and nothing of ours playing) offers «كمّل الإذاعة» on its
bar, and never starts it by itself."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
H = "android/launcher/src/main/java/sa/urtv/home/"
rd = lambda p: (ROOT / p).read_text(encoding="utf-8")
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
prefs, svc, main = rd(H + "tilawah/Prefs.java"), rd(H + "tilawah/MusicService.java"), rd(H + "MainActivity.java")
check("ResumeOffer's source numbers are Prefs'", "SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3" in prefs and "SURAH = 1, RADIO = 2" in rd(H + "tilawah/ResumeOffer.java"))
check("the service writes «was playing» when a recitation or a station starts, and clears it on a pause and on a stop",
      "if (source == Prefs.SOURCE_SURAH || source == Prefs.SOURCE_RADIO) prefs.setPlayingAt(System.currentTimeMillis());" in svc.split("private void playbackStarted() {")[1].split("\n    }\n")[0]
      and "prefs.setPlayingAt(0);" in svc.split("private void playbackPaused() {")[1].split("\n    }\n")[0]
      and "prefs.setPlayingAt(0);" in svc.split("void stopPlayback() {")[1].split("\n    }\n")[0])
bar = main.split("private void updateHomeNudge(boolean takeFocus) {")[1].split("\n    }\n")[0]
check("review F124: the stamp is refreshed every 30 s while it plays (a power-off hours ago is not «cut off»), and the offer is only for the last 10 minutes",
      "prefs.setPlayingAt(System.currentTimeMillis());   // «was playing» = the last time it was seen alive" in svc and "MAX_AGE_MS = 10L * 60_000L" in rd(H + "tilawah/ResumeOffer.java"))
check("the bar offers it (not in kids mode, not in «UrTV النظيف», after the more urgent offers, before the city question), with its own words",
      "boolean askResume = !askStock && !askUpdate && !askExtras && !askOverlay && !kids && !cleanShown && sa.urtv.home.tilawah.MusicService.cutOff(this);" in bar
      and "!askOverlay && !askResume && mismatch == null" in bar and 'S.t("توقفت الإذاعة فجأة")' in bar and 'S.t("كمّل الإذاعة")' in bar and 'S.t("كمّل التلاوة")' in bar)
click = main[main.index("nudgeCard.setOnClickListener(v -> {"):]
click = click[:click.index("nudgeLater = text(")]
check("it goes on only when the person presses the button (guarded like the تلاوة pill); «لاحقاً» and Back drop the offer, and nothing starts the sound by itself",
      "if (nudgeIsResume) { guarded(ParentalPolicy.PILL_LISTEN" in click and "resumeCutOff(this)" in click
      and "if (nudgeIsResume) { sa.urtv.home.tilawah.MusicService.forgetCutOff(this)" in main.split("private void snoozeNudge()")[1].split("\n    }\n")[0]
      and main.count("resumeCutOff(") == 1)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / H / "tilawah/ResumeOffer.java"), str(ROOT / "tests/ResumeOfferRun.java")], check=True)
        got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.ResumeOfferRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
    want = {"radio cut": "true", "surah cut": "true", "never started": "false", "playing now": "false", "adhan or none": "false false", "too old": "false true", "future": "false"}
    for k, v in want.items():
        check(f"resume offer: {k}", got.get(k) == v, got.get(k))
else:
    print("SKIP resume offer on a JVM: no javac"); failed += 1
sys.exit(1 if failed else 0)
