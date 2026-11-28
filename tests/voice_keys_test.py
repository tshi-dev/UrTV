#!/usr/bin/env python3
"""R-404: the remote's voice / search key stopped working after a change of the favourites. The key went through the home's own key path (the
remote buttons, the shortcut keys, the edit modes) and the add-on's key filter, and any of them could keep it. Now SEARCH, ASSIST and VOICE_ASSIST
go to Android untouched: in the home right after the adhan's any-key and a reminder's card (the person's own choices), before every other handler;
in the add-on before anything else. tests/VoiceKeysRun.java runs the rule. Run: python3 tests/voice_keys_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
svc = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
gradle = (ROOT / "android/extras/build.gradle").read_text(encoding="utf-8")
dk = main.split("private boolean dispatchKey(KeyEvent e) {")[1].split("\n    }\n")[0]
line = "if (VoiceKeys.is(e.getKeyCode())) return super.dispatchKeyEvent(e);"
check("the home hands the voice / search key to Android", line in dk)
if line in dk:
    at = dk.index(line)
    check("...after the adhan and a reminder's card only", dk.index("adhanTakes(e)") < at and dk.index("reminder.key(e)") < at)
    for later in ("KeyButton.of(e)", "homeButtons.decide(", "remoteKeys.decide(", "editKey(e)", "appsEditKey(e)", "KEYCODE_MENU", "backExitTakes(e)"):
        check("...before " + later, later in dk and at < dk.index(later))
ke = svc.split("private boolean keyEvent(KeyEvent e) {")[1].split("\n    }\n")[0]
check("the add-on gives it back first, before the Home catch, an app button or the Settings key",
      ke.strip().startswith("if (VoiceKeys.is(e.getKeyCode())) return false;"), ke[:200])
check("the add-on gets the same rule (one copy, shared)", "'VoiceKeys.java'" in gradle.split("def shared = ")[1].split("\n")[0])

if not shutil.which("javac"):
    print("SKIP VoiceKeysRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "VoiceKeys.java"), str(ROOT / "tests/VoiceKeysRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.VoiceKeysRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of VoiceKeysRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
