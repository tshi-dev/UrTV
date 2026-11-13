#!/usr/bin/env python3
"""«أزرار الريموت» (KeyApps.java): which app the remote's YouTube, Netflix and Prime Video keys open. A key with no choice keeps its own
app and, outside our home, passes untouched; a chosen key opens its app on our home (RemoteKeys) and over other apps (TypeService's
filter), consuming its repeats and release. Then the wiring, read from the source.   Run: python3 tests/key_apps_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

src = lambda n: (HOME / n).read_text(encoding="utf-8")
ka, rk, ts, main, dock, recv = src("KeyApps.java"), src("RemoteKeys.java"), src("TypeService.java"), src("MainActivity.java"), src("Dock.java"), src("DockReceiver.java")
check("no Android in KeyApps", "import android" not in ka)
check("RemoteKeys takes the owner's app for each app key, and still compiles alone (no KeyApps in it)", all(f'chosen(choices, "{k}", {k.upper()})' in rk for k in ("youtube", "netflix", "prime")) and "KeyApps." not in re.sub(r"//.*|/\*.*?\*/", "", rk, flags=re.S))
check("MainActivity hands RemoteKeys the choices, and counts a chosen app as installed only under kids mode's rules",
      "remoteKeys.choices(Dock.keyApps(this))" in main and "KeyApps.chosen(pkg) && isInstalled(pkg) && ParentalPolicy.visible(family, pkg)" in main)
check("TypeService looks only at the app keys, and opens through kids mode and the family PIN",
      "KeyApps.keyOf(e.getKeyCode())" in ts and "ParentalPolicy.visible(family, pkg)" in ts and "ParentalPolicy.targets(ParentalPolicy.REMOTE_APP, pkg)" in ts)
check("the choice is checked and must be installed before it is saved", "KeyApps.validChoice(pkg)" in recv and "Dock.installed(c, pkg)" in recv)
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sa, sh, pv, pr = (A / "SettingsActivity.java").read_text(encoding="utf-8"), (A / "SettingsHub.java").read_text(encoding="utf-8"), (A / "ConfigProvider.java").read_text(encoding="utf-8"), (A / "Prefs.java").read_text(encoding="utf-8")
check("on the TV: three rows in «الشاشة الرئيسية», each a list of the TV's apps with «الافتراضي» first", "pickKeyApp(k[0], k[1], k[2])" in sa and all(f'{{"{t}", HOME}}' in sh for t in ("زر يوتيوب في الريموت", "زر نتفليكس في الريموت", "زر Prime Video في الريموت")))
check("the screensaver hands the choices to the home (key_apps), and the home takes them only when they change", '"key_apps"' in pv and "p.keyApps()" in pv and "Dock.followKeyApps(this, cfg.keyApps);" in main and 'cur.getColumnIndex("key_apps")' in (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8"))
check("the screensaver's copy is checked before it is saved, and dock.sh keeps the TV row in step", "keyAppOk(pkg)" in pr and "--es keyapp $2=$3" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8"))
check("FEATURES.md, SETTINGS.md and the setup prompt name it", "**أزرار الريموت**" in (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8") and "dock.sh key" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8") and "optimize/dock.sh key youtube" in (ROOT / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8"))
check("dock.sh has the key command", "send KEY_APP" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP key apps: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "KeyApps.java"), str(HOME / "RemoteKeys.java"), str(ROOT / "tests/KeyAppsRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.KeyAppsRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
want = {
    "keyOf 190": "youtube", "keyOf 4065": "youtube", "keyOf 191": "netflix", "keyOf 4062": "netflix", "keyOf 4099": "prime", "keyOf 21": "null",
    "target default": "com.google.android.youtube.tv", "target chosen": "org.smarttube.stable", "target bad": "com.netflix.ninja",
    "valid empty": "true", "valid pkg": "true", "valid one word": "false", "valid shell": "false",
    "outside default passes": "0", "outside home passes": "0", "outside other key passes": "0", "outside chosen opens": "2:org.smarttube.stable",
    "outside repeat eaten": "1", "outside release eaten": "1", "outside next press opens": "2", "outside lost release, new press opens": "2",
}
for k, v in want.items():
    check(f"key apps: {k}", got.get(k) == v, f"{got.get(k)!r}, wanted {v!r}")
sys.exit(1 if failed else 0)
