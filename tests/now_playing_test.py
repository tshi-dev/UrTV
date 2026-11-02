#!/usr/bin/env python3
"""The home screen's «يُتلى الآن» card: its words (NowPlayingText.java, on tests/now_playing_cases.tsv) and how it is wired (source checks:
MusicService says when the state changes, the card listens, nothing polls; OK, ◀ ▶ and a long OK; its place in the pills row; it goes
with the playback). Needs javac for the words; without it they are skipped with a note.   Run: python3 tests/now_playing_test.py"""
import re
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
card = (HOME / "NowCard.java").read_text(encoding="utf-8")
service = (TIL / "MusicService.java").read_text(encoding="utf-8")
now = (TIL / "NowPlaying.java").read_text(encoding="utf-8")

check("MusicService: announces a change to this app only, once per change (a signature of what the card shows), never on the clock's tick",
      'sendBroadcast(new Intent(NowPlaying.CHANGED).setPackage(getPackageName()))' in service and "if (now.equals(announced)) return;" in service)
check("MusicService: announce() is called from updateSession (every change goes through it) and when playback stops",
      re.search(r"private void updateSession\(\) \{\s*announce\(\);", service) is not None
      and re.search(r"sleepUntil = 0;\s*announce\(\);\s*foregroundStarted = false;", service) is not None)
check("MainActivity: the card listens for that broadcast and re-reads on resume; nothing polls (no postDelayed for it, no new timer)",
      "InternalReceivers.register(this, nowPlayingChanged, new IntentFilter(NowPlaying.CHANGED))" in main and "unregisterReceiver(nowPlayingChanged)" in main
      and "updateNowPlaying();\n        homeCard.refresh();" in main and "postDelayed" not in card and "Handler" not in card)
check("MainActivity: the sleep timer's minutes follow the clock's existing minute tick, not a timer of their own",
      "updateNowPlaying();   // the minutes left of its sleep timer" in main)
check("NowPlaying: shown only for a surah or a station, never for the adhan or nothing",
      "source != Prefs.SOURCE_SURAH && !radio) return new State(false" in now)
check("MainActivity: OK pauses or plays, a long OK opens the player",
      "nowCard.setOnClickListener(v -> NowPlaying.toggle(this))" in main
      and ("nowCard.setOnLongClickListener(v -> { startActivity(NowPlaying.player(this)); return true; })" in main
           or "nowCard.setOnLongClickListener(v -> { launchGuarded(ParentalPolicy.PILL_LISTEN, NowPlaying.player(this)" in main))   # the second once the family lock is in
check("MainActivity: ◀ is the previous surah and ▶ the next while the card has the focus, for a surah only (a station: ◀ goes on to the pills)",
      "NowPlaying.prev(this); else NowPlaying.next(this)" in main and "if (s.show && !s.radio) {" in main and "KEYCODE_DPAD_LEFT) NowPlaying.prev" in main)
check("MainActivity: it is the first child of the pills row, hung evenly over its top and bottom so the row stays a pill's height,"
      " hidden until something plays: the pills do not move",
      "int hang = dp((NowCard.HEIGHT - Pill.HEIGHT) / 2f);" in main and "right.addView(nowCard, 0, sideLlp(-2, dp(NowCard.HEIGHT), 0, -hang, dp(10), -hang))" in main
      and "nowCard.setVisibility(View.GONE);" in main)
import re
num = lambda pat, src: float(re.search(pat, src).group(1))
H, P = num(r"static final int HEIGHT = (\d+), LINE_MAX", card), num(r"static final int HEIGHT = (\d+);", (HOME / "Pill.java").read_text(encoding="utf-8"))
LINE_MAX, HINT_MAX = num(r"LINE_MAX = (\d+),", card), num(r"HINT_MAX = (\d+);", card)
TSP, SSP = num(r"TITLE_SP = ([\d.]+)", card), num(r"SUB_SP = ([\d.]+);", card)
NAME_SP = num(r"name\.setTextSize\(TypedValue\.COMPLEX_UNIT_SP, ([\d.]+)\)", (HOME / "Pill.java").read_text(encoding="utf-8"))
# Thmanyah lines are 1.25 em tall with the font padding off; the two lines and the 2 dp between them fit inside the card
check(f"NowCard: two lines, the first at a pill's name size ({NAME_SP:g} sp), the second smaller ({SSP:g} sp), inside {H:g} dp; the card hangs over the row evenly",
      TSP == NAME_SP and SSP < TSP and 1.25 * (TSP + SSP) + 2 <= H - 6 and (H - P) % 2 == 0 and H > P, f"{TSP} {SSP} {H} {P}")
# the longest real texts, measured in Thmanyah (HarfBuzz; tests can't shape here): «سورة الشعراء» 73 dp, the longest station 169, the longest
# reciter with a timer 184, the focus hint 262 (all at the sizes above): a line holds them, and the card is no wider than before at its widest
pad = sum(num(p, card) for p in (r"setPadding\(Math\.round\((\d+) \* d\), 0,", r"setPadding\(Math\.round\(\d+ \* d\), 0, Math\.round\((\d+) \* d\)",
                                  r"new LayoutParams\(Math\.round\((\d+) \* d\)", r"wp\.setMargins\(0, 0, Math\.round\((\d+) \* d\)"))
check(f"NowCard: a line ({LINE_MAX:g} dp) holds the longest real station (169) and reciter with a timer (184), the card ({LINE_MAX + pad:g} dp) no wider than"
      " the old one at its widest real text (252); the focused hint (262) fits its own width; longer names end in «…»",
      184 <= LINE_MAX and LINE_MAX + pad <= 252 and 262 <= HINT_MAX and "t.setEllipsize(TextUtils.TruncateAt.END)" in card and "t.setSingleLine(true)" in card
      and "sub.setMaxWidth(Math.round((focused ? HINT_MAX : LINE_MAX)" in card, f"{LINE_MAX} {pad}")
check("MainActivity: it goes with the playback, and the focus goes back to the first pill if it had it",
      "nowCard.hasFocus() && !pills.isEmpty()) pills.get(0).requestFocus()" in main)
check("MainActivity: it takes the focus like a pill (D-pad order: ▶ from the first pill reaches it; down goes to the cards or the dock)",
      "nowCard.setNextFocusDownId(card != null ? card.getId()" in main and "nowCard.setNextFocusLeftId(pills.get(0).getId())" in main)
# the screensaver's own «يُتلى الآن» card (item 6): the launcher answers a signed-only provider, the app gives the page one read-only call
manifest_h = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
manifest_s = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
provider = (TIL / "NowPlayingProvider.java").read_text(encoding="utf-8")
bridge = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java").read_text(encoding="utf-8")
screenview = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ScreenView.java").read_text(encoding="utf-8")
check("screen card: the launcher's provider is read-only, one row only while a surah or a station PLAYS (not paused, not the adhan)",
      "if (s.show && s.playing) out.addRow" in provider and "return null;" in provider.split("getType")[1].split("}")[0]
      and "insert(Uri uri, ContentValues v) { return null; }" in provider)
check("screen card: the provider needs a signature permission only the screensaver (same key) holds",
      'android:name="sa.urtv.home.READ_NOW_PLAYING"' in manifest_h and 'android:protectionLevel="signature"' in manifest_h
      and 'android:authorities="sa.urtv.home.nowplaying"' in manifest_h and 'android:readPermission="sa.urtv.home.READ_NOW_PLAYING"' in manifest_h
      and 'uses-permission android:name="sa.urtv.home.READ_NOW_PLAYING"' in manifest_s)
check("screen card: the page gets two read-only calls, window.UrtvNative.nowPlaying() and adhan() (the adhan bar), on the screen only (not on the settings' previews); errors give an empty answer",
      '@JavascriptInterface' in bridge and bridge.count("@JavascriptInterface") == 2 and "public String adhan()" in bridge and "catch (Exception ignored)" in bridge
      and 'if (screen) w.addJavascriptInterface(new NowPlayingBridge(c), "UrtvNative")' in screenview)
check("screen card: the page asks once at the start and then once a minute, no faster",
      "nowPlayingTick();\n  setInterval(nowPlayingTick, 60*1000);" in (ROOT / "src/template.html").read_text(encoding="utf-8"))
check("NowCard: a pill's height (Pill.HEIGHT) is kept by the row, not the card; its focus shows the keys on its second line",
      "sub.setText(focused ? hintText : subText)" in card and "playing ? pause : play" in card)

if not shutil.which("javac"):
    print("SKIP now playing text: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/now_playing_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(TIL / "NowPlayingText.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/NowPlayingTextRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.NowPlayingTextRun", str(ROOT / "tests/now_playing_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    check(f"now playing {c[0]}: {c[5]}", g == c[4], f"{g!r}, wanted {c[4]!r}")
check("every case answered", len(got) > len(cases), (len(got), len(cases)))
sys.exit(1 if failed else 0)
