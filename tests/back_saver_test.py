#!/usr/bin/env python3
"""«الرجوع يفتح شاشة التوقف» (off by default): Back on our home, with nothing to go back to, starts the screensaver's preview (as Clean Your
TV does). The rule runs in tests/HomeBackRun.java (tests/home_tile_test.py); here the wiring, read from the source: it is off until
`dock.sh backsaver on`, it comes after the grid's Back and the tile's leave, it starts the screensaver the way its pill does (behind @saver),
and dock.sh sends exactly on, off or status (a stand-in for optimize/tv.py records what it would send).   Run: python3 tests/back_saver_test.py"""
import os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def src(n): return (HOME / n).read_text(encoding="utf-8")
main, dock, recv, hb = src("MainActivity.java"), src("Dock.java"), src("DockReceiver.java"), src("HomeBack.java")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
back = main[main.index("public void onBackPressed()"):main.index("private boolean isDefaultHome()")]

check("off until it is turned on (the saved flag defaults to false)", 'prefs(c).getBoolean("back_saver", false)' in dock)
check("HomeBack's rule: only on, as the default home, at the top, with the screensaver there",
      "return on && defaultHome && !gridScrolled && saverThere;" in hb and "import android" not in hb)
check("Back: the grid's Back to the dock first, then the tile's leave, and only then the screensaver",
      back.index("dockCards.get(0).requestFocus();") < back.index("HomeBack.leaves(isDefaultHome(), false)") < back.index("HomeBack.startsSaver(Dock.backSaver(this), true, false, isInstalled(SAVER_PKG))"))
check("it starts the screensaver as its pill does (startSaver: the full-screen preview), behind the same lock (@saver)",
      'guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver)' in back)
check("only on our home: nothing in another screen or app reads it", sum(f.read_text(encoding="utf-8").count("Dock.backSaver(") for f in HOME.rglob("*.java")) == 2
      and "Dock.backSaver(this)" in back)
check("the receiver takes on, off or status and answers «back-saver: on|off»; anything else is refused before saving",
      'case "sa.urtv.home.BACK_SAVER"' in recv and 'setResultData("back-saver: " + (Dock.backSaver(c) ? "on" : "off"));' in recv
      and recv.index('error: add --es mode on|off|status"); return; }\n                if (!m.equals("status")) Dock.setBackSaver') > 0)
check("the manifest lets the shell (only) send it", '<action android:name="sa.urtv.home.BACK_SAVER" />' in man)

# dock.sh with a stand-in for tv.py
tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="back-saver: on"\'\n')
os.chmod(fake, 0o755)
def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip().split("\n")
A = "am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.BACK_SAVER"
for v in ("on", "off"):
    rc, out, sent = run("backsaver", v)
    check(f"dock.sh backsaver {v}: one broadcast with the mode", rc == 0 and sent == [A + " --es mode " + v], (rc, sent))
rc, out, sent = run("backsaver")
check("dock.sh backsaver: no argument asks how it stands", rc == 0 and sent == [A + " --es mode status"] and "back-saver: on" in out, (rc, sent))
for bad in ("yes", "on; reboot", ""):
    rc, out, sent = run("backsaver", bad)
    check(f"dock.sh backsaver {bad!r}: refused, nothing sent", rc != 0 and sent == [""], (rc, out, sent))
docs = (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8") + (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8")
check("SETTINGS.md and FEATURES.md name it and its command", "dock.sh backsaver on" in docs and "الرجوع يفتح شاشة التوقف" in docs)
sys.exit(1 if failed else 0)
