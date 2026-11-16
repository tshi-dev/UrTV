#!/usr/bin/env python3
"""4.4 «شاشة سوداء» (GitHub #62): listening with the picture off. BlackScreen (run for real): it stays while something of ours plays or loads,
closes after 4 s of nothing (a pause, the sleep timer, a stop), any key but volume and mute closes it. The source checks: FLAG_KEEP_SCREEN_ON only
while playing and cleared when it goes, brightness 0, the key goes back to the player and never reaches what is behind; it is opened from the
player's options, the home's «يُتلى الآن» card (menu key) and the control center (only while something plays), the last two behind the
«تلاوة وأذان» lock (PIN, kids mode).   python3 tests/black_screen_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

act, rule, player, home, center = read(T / "BlackScreenActivity.java"), read(T / "BlackScreen.java"), read(T / "PlayerActivity.java"), read(H / "MainActivity.java"), read(H / "ControlCenter.java")
man = read(ROOT / "android/launcher/src/main/AndroidManifest.xml")
check("BlackScreen is plain Java", "import android" not in rule)
check("the screen is kept on only while something plays, and the flag is cleared when it pauses and when it closes",
      "addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)" in act and "clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)" in act
      and "keepOn(on);" in act and "keepOn(false);" in act[act.index("protected void onPause()"):])
check("as dark as the TV allows", "lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;" in act)
check("it follows the player: on every change it says, and once a second", "new IntentFilter(NowPlaying.CHANGED)" in act and "handler.postDelayed(this, 1000)" in act
      and "if (BlackScreen.close(on, quietSince, now)) finish();" in act)
check("a key closes it and goes back to the player; the key never reaches what is behind", "return true;   // the key is the black screen's" in act
      and "PlayerActivity.class" in act and "if (!BlackScreen.closesOn(e.getKeyCode())) return super.dispatchKeyEvent(e);" in act)
check("its own, black, not exported, not in recents", 'android:name=".tilawah.BlackScreenActivity"' in man
      and 'android:exported="false"' in man[man.index(".tilawah.BlackScreenActivity"):man.index(".tilawah.BlackScreenActivity") + 300]
      and "Theme.Black.NoTitleBar.Fullscreen" in man)
check("the player's options: «شاشة سوداء» before the red stop (the player is behind the «تلاوة وأذان» lock)",
      'sa.urtv.home.S.t("شاشة سوداء"), Ui.str(this, R.string.stop_now)' in player and "i == labels.length - 1 ? R.color.red" in player
      and "case 4:   // 4.4 #62" in player and "case 5:\n                MusicService.stopAction(this);" in player)
check("the home's card: the menu key, behind the «تلاوة وأذان» lock", "KeyEvent.KEYCODE_MENU" in home
      and 'launchGuarded(ParentalPolicy.PILL_LISTEN, sa.urtv.home.tilawah.BlackScreenActivity.open(this), "تلاوة وأذان")' in home)
check("the control center: a tile only while something plays, behind the same lock", "if (sa.urtv.home.tilawah.NowPlaying.read(act).show)" in center
      and 'tile("black", S.t("شاشة سوداء")' in center and 'host.launchGuarded(ParentalPolicy.PILL_LISTEN, sa.urtv.home.tilawah.BlackScreenActivity.open(act)' in center
      and '"saver", "black"};' in center)
check("CHANGELOG credits the idea", "فكرة واقتراح: @nasaa1nasaa-crypto (#62)" in read(ROOT / "CHANGELOG.md"))
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
public class BlackRun { static void out(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  out("a surah playing or loading keeps it", BlackScreen.playing(Prefs.SOURCE_SURAH, true, false) && BlackScreen.playing(Prefs.SOURCE_RADIO, false, true) && BlackScreen.playing(Prefs.SOURCE_ADHAN, true, false));
  out("nothing chosen, or paused: not playing", !BlackScreen.playing(Prefs.SOURCE_NONE, false, false) && !BlackScreen.playing(Prefs.SOURCE_SURAH, false, false));
  out("a short gap (the next surah loading) does not close it", !BlackScreen.close(false, 1000, 1000 + BlackScreen.GRACE_MS - 1) && !BlackScreen.close(true, 0, 99_999));
  out("4 s of nothing closes it", BlackScreen.close(false, 1000, 1000 + BlackScreen.GRACE_MS) && BlackScreen.GRACE_MS == 4000);
  out("any key closes it: OK, Back, Home, the arrows, play/pause", BlackScreen.closesOn(23) && BlackScreen.closesOn(4) && BlackScreen.closesOn(3) && BlackScreen.closesOn(19) && BlackScreen.closesOn(85));
  out("volume and mute stay the TV's", !BlackScreen.closesOn(24) && !BlackScreen.closesOn(25) && !BlackScreen.closesOn(164) && !BlackScreen.closesOn(91));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "BlackRun.java").write_text(run)
        Path(tmp, "Prefs.java").write_text("package sa.urtv.home.tilawah;\nfinal class Prefs { static final int SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3; }\n")
        check("the stub's numbers are Prefs' own", "static final int SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3;" in read(T / "Prefs.java"))
        r = subprocess.run(["javac", "-d", tmp, str(T / "BlackScreen.java"), str(Path(tmp, "Prefs.java")), str(Path(tmp, "BlackRun.java"))], capture_output=True, text=True)
        check("BlackScreen compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.BlackRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 6, len(out.splitlines()))
sys.exit(1 if failed else 0)
