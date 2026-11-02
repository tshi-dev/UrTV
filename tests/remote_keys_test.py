#!/usr/bin/env python3
"""The shortcut keys of the remote on our home screen (RemoteKeys.java, tests/remote_keys_cases.tsv): YouTube/Netflix (BUTTON_3/4, and TCL's vendor keys 4065/4062/4099 for YouTube/Netflix/Prime Video, 4077 for Settings), the
settings key (short: the TV's settings, long: ours), the star key (scan code 578), acting on a first press only and consuming its repeats and release; an app that is not installed
or a key that is not mapped is left alone. Then the wiring, read from the source: MainActivity asks the adhan rule first and RemoteKeys
second, and opens through the family lock.   Run: python3 tests/remote_keys_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
keys = (HOME / "RemoteKeys.java").read_text(encoding="utf-8")
body = main.split("public boolean dispatchKeyEvent(KeyEvent e) {")[1].split("\n    }\n")[0] if "public boolean dispatchKeyEvent(KeyEvent e) {" in main else ""
check("dispatchKeyEvent asks the adhan rule first, then RemoteKeys, then the Menu key",
      "adhanTakes(e)" in body and "remoteKeys.decide(" in body and "KEYCODE_MENU" in body
      and body.index("adhanTakes(e)") < body.index("remoteKeys.decide(") < body.index("KEYCODE_MENU"), body[:300])
check("the key numbers are written out (no Android in RemoteKeys)", "import android" not in keys and "KeyEvent." not in re.sub(r"//.*|/\*.*?\*/", "", keys, flags=re.S))
check("the apps open through the family lock (REMOTE_APP / REMOTE_SETTINGS), the star through the Menu key's path",
      "ParentalPolicy.REMOTE_APP" in main and "ParentalPolicy.REMOTE_SETTINGS" in main and "ParentalPolicy.MENU_KEY" in body)
check("the Settings key (176, 83 and TCL's 4077): a short press opens the TV's own settings behind @tvsettings (REMOTE_SETTINGS), a long one OUR settings behind «قفل الإعدادات» (OUR_SETTINGS), falling back to the TV's settings when our app is missing",
      "return keyCode == KEYCODE_SETTINGS || keyCode == KEYCODE_NOTIFICATION || keyCode == TCL_SETTINGS;" in keys and "static final long LONG_MS = 500;" in keys
      and "heldMs >= LONG_MS ? OPEN_OUR_SETTINGS : OPEN_SETTINGS" in keys and "ParentalPolicy.OUR_SETTINGS" in main
      and "case RemoteKeys.OPEN_SETTINGS:   // a short press of the Settings key: the TV's own settings, behind @tvsettings\n                launchGuarded(ParentalPolicy.REMOTE_SETTINGS, new Intent(Settings.ACTION_SETTINGS)" in main
      and "case RemoteKeys.OPEN_OUR_SETTINGS:   // a long press of the Settings key: OUR settings (the TV's own if our app is missing)\n                openOurSettings(true);" in main
      and "SETTINGS" not in keys.split("private static Decision map")[1].split("default:")[0])
check("MainActivity hands RemoteKeys how long the key has been down", "e.getRepeatCount(), e.getEventTime() - e.getDownTime()," in body)
check("TCL's vendor keys 4062 Netflix, 4065 YouTube, 4099 Prime Video and 4077 Settings are mapped, and opensApp knows the app ones",
      all(f"{n} = {v}" in keys or f"{n} = {v}," in keys for n, v in [("TCL_NETFLIX", 4062), ("TCL_YOUTUBE", 4065), ("TCL_PRIME", 4099), ("TCL_SETTINGS", 4077)])
      and "keyCode == TCL_NETFLIX || keyCode == TCL_YOUTUBE || keyCode == TCL_PRIME" in keys.replace("\n", " ").replace("  ", ""), keys[:0])
check("Prime Video is among the apps a key can open", "com.amazon.amazonvideo.livingroom" in keys and "PACKAGES = {YOUTUBE, NETFLIX, PRIME}" in keys)
check("an app hidden by kids mode counts as not installed (the key then does nothing)", "ParentalPolicy.visible(" in main.split("Set<String> remoteInstalled()")[1][:600] if "Set<String> remoteInstalled()" in main else False)
check("no GLOBAL_BUTTON receiver (the system sends that broadcast to a named component only)",
      "GLOBAL_BUTTON" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
main_src = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("MainActivity forgets a held shortcut key when it goes to the background (its release went to the opened app)",
      "remoteKeys.reset();" in main_src.split("protected void onPause() {")[1].split("\n    }\n")[0])
check("MainActivity logs every first press that reaches it, codes only, for the TV test (adb logcat -s UrTVKeys)",
      'android.util.Log.d("UrTVKeys", "key=" + e.getKeyCode() + " scan=" + e.getScanCode())' in main_src)
check("TVS.md says the key may be taken before our home sees it, and how to check", "before our home sees it" in (ROOT / "docs/TVS.md").read_text(encoding="utf-8") and "logcat -s UrTVKeys" in (ROOT / "docs/TVS.md").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP remote keys: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/remote_keys_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "RemoteKeys.java"), str(ROOT / "tests/RemoteKeysRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.RemoteKeysRun", str(ROOT / "tests/remote_keys_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    if c[0] == "reset": continue
    check(f"remote keys: {c[7]}", g == c[6], f"{g!r}, wanted {c[6]!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
