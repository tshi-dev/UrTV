#!/usr/bin/env python3
"""4.5 «أزرار الريموت»: two and three presses per button (mockup docs/mockups/tools-45/02-taps.png), extending #59 (KeyApps, RemoteButtons), not a
second mapper. The counter (KeyApps.Taps) runs in tests/KeyTapsRun.java: a button with only 1× acts at once as before; with a 2× or 3× it counts within
TAP_WINDOW_MS. Here: the stored line (youtube.2=…, @switcher, @center), the add-on's and the home's wiring, the ADB command.
Run: python3 tests/key_taps_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
ka, dock, rec, main = read(H / "KeyApps.java"), read(H / "Dock.java"), read(H / "DockReceiver.java"), read(H / "MainActivity.java")
svc, sh = read(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java"), read(ROOT / "optimize/dock.sh")
check("tvOS Launcher credited for the idea and its 300 ms window", "techgenius1/tvOS-Launcher" in ka and "TAP_WINDOW_MS = 300" in ka)
check("the line keeps youtube.2 / youtube.3 and @switcher / @center (an older add-on skips them)", 'k.matches("[a-z]+(\\\\.[23])?") && validTarget(v)' in ka)
check("the home sends the 2× and 3× too", 'for (String n : new String[]{".2", ".3"}) { String v = keyApp(c, b.id + n); if (!v.isEmpty()) m.put(b.id + n, v); }' in dock)
check("ADB: KEY_APP takes BUTTON.2|.3 and @switcher|@center; an app must be installed", 'k.replaceFirst("\\\\.[23]$", "")' in rec and "KeyApps.validTarget(pkg)" in rec and "is not installed" in rec)
check("the add-on: a counted button goes to tapKey over other apps; others keep the 4.4 path", "if (!homeFront && KeyApps.counted(taps)) return tapKey(id, taps, e);" in svc
      and "main.postAtTime(tapTimeout, tapper.deadline)" in svc and "if (tapper.pending()) tapTimeout.run();" in svc)
check("the add-on: the switcher and the control centre open in the home (open=switcher / open=center, behind their locks)", '.putExtra("open", KeyApps.SWITCHER.equals(target) ? "switcher" : "center")' in svc)
check("the add-on: a removed app makes that press nothing", "if (KeyApps.chosen(live[n]) && launchIntent(this, live[n]) == null) live[n] = \"\";" in svc)
check("the home: the same counter on our own screen, before the 4.4 path", "if (keyId != null && homeTapKey(keyId, e)) return true;" in main
      and main.index("homeTapKey(keyId, e)") < main.index("homeButtons.decide(keyId") and "handler.postAtTime(homeTapTimeout, homeTaps.deadline)" in main)
check("the home: an app behind the family lock and kids mode as its tile", "guarded(ParentalPolicy.REMOTE_APP, target, labelOf(target), () -> openPackage(target))" in main and "ParentalPolicy.visible(Parental.state(this), target)" in main)
check("dock.sh key BUTTON.2|.3 PKG|@switcher|@center|default", '[[ "$2" =~ ^[a-z]+(\\.[23])?$ ]]' in sh and '"$3" == @switcher || "$3" == @center' in sh)
out = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), "key", "youtube.4", "@switcher"], capture_output=True, text=True, cwd=ROOT)
check("dock.sh refuses a fourth press before asking the TV", out.returncode == 1 and "usage" in out.stdout, out.stdout + out.stderr)
out = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), "key", "youtube.2", "@shell"], capture_output=True, text=True, cwd=ROOT)
check("dock.sh refuses an unknown @target", out.returncode == 1 and "usage" in out.stdout, out.stdout)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "KeyApps.java"), str(ROOT / "tests/KeyTapsRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.KeyTapsRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every KeyApps.Taps case ran", "ALL PASS" in out, out[-300:])
sys.exit(1 if failed else 0)
