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
rb = src("RemoteButtons.java")
check("no Android in KeyApps or RemoteButtons", "import android" not in ka and "import android" not in rb)
ss_rb = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/RemoteButtons.java").read_text(encoding="utf-8")
check("the screensaver's RemoteButtons is the home's, but for its package line and the line naming the other copy",
      ss_rb.split("\n", 1)[1].replace("is in the home app (sa.urtv.home.RemoteButtons), which acts on the keys;", "X")
      == rb.split("\n", 1)[1].replace("is in the screensaver app (sa.urtv.screensaver.RemoteButtons), which draws the rows;", "X"))
check("Free My TV's licence is named for the table", "Free My TV" in rb and "MIT" in rb and "RemoteButtons.java" in (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8"))
check("RemoteKeys takes the owner's app for each app key, and still compiles alone (no KeyApps in it)", all(f'chosen(choices, "{k}", {k.upper()})' in rk for k in ("youtube", "netflix", "prime")) and "KeyApps." not in re.sub(r"//.*|/\*.*?\*/", "", rk, flags=re.S))
check("MainActivity hands RemoteKeys the choices, and counts a chosen app as installed only under kids mode's rules",
      "remoteKeys.choices(Dock.keyApps(this))" in main and "KeyApps.chosen(pkg) && isInstalled(pkg) && ParentalPolicy.visible(family, pkg)" in main)
check("TypeService looks only at this remote's app buttons (RemoteButtons), and opens through kids mode and the family PIN",
      "RemoteButtons.idOf(REMOTE_TV" in ts and "ParentalPolicy.visible(family, pkg)" in ts and "ParentalPolicy.targets(ParentalPolicy.REMOTE_APP, pkg)" in ts)
check("the choice is checked and must be installed before it is saved", "KeyApps.validChoice(pkg)" in recv and "Dock.installed(c, pkg)" in recv)
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sa, sh, pv, pr = (A / "SettingsActivity.java").read_text(encoding="utf-8"), (A / "SettingsHub.java").read_text(encoding="utf-8"), (A / "ConfigProvider.java").read_text(encoding="utf-8"), (A / "Prefs.java").read_text(encoding="utf-8")
titles = re.findall(r'b\("[a-z]+", "([^"]+)"', rb)
check("on the TV: a row in «الشاشة الرئيسية» only for each button this remote has, each a list of the TV's apps with «الافتراضي» first",
      "RemoteButtons.buttons(Prefs.remoteTv())" in sa and "pickKeyApp(k.id, title)" in sa and titles and all(f'{{"{t} في الريموت", HOME}}' in sh for t in titles), titles)
check("the screensaver hands the choices to the home (key_apps), and the home takes them only when they change", '"key_apps"' in pv and "p.keyApps()" in pv and "Dock.followKeyApps(this, cfg.keyApps);" in main and 'cur.getColumnIndex("key_apps")' in (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8"))
check("the screensaver's copy is checked before it is saved, and dock.sh keeps the TV row in step", "keyAppOk(pkg)" in pr and "--es keyapp $2=$3" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8"))
check("FEATURES.md, SETTINGS.md and the setup prompt name it", "**أزرار الريموت**" in (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8") and "dock.sh key" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8") and "optimize/dock.sh key <button> <pkg>" in (ROOT / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8"))
check("dock.sh has the key command", "send KEY_APP" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP key apps: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "KeyApps.java"), str(HOME / "RemoteButtons.java"), str(ROOT / "tests/KeyAppsRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.KeyAppsRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
want = {
    "tv TCL": "12", "tv unknown": "0", "tv Sony": "6", "tv Philips old": "0", "tv Philips GTV": "4", "tv Xiaomi MiTV-M": "11", "tv Mi Box": "0",
    "tv Formovie": "7", "tv Chromecast": "1",
    "rows TCL": "netflix,youtube,prime,tclchannel", "rows unknown": "", "rows JVC": "", "rows Sony": "core,netflix,disney,prime,youtube,crunchyroll,play,sonyliv",
    "TCL 190": "youtube", "TCL 4065": "youtube", "TCL 4062": "netflix", "TCL 4020": "tclchannel", "TCL gamepad 190": "null", "TCL arrow": "null",
    "Sony 196 scan 496": "core", "Sony 191 scan 758": "netflix", "Sony VIDEO_APP scan 485": "youtube", "Sony 3004": "sonyliv", "Sony 196 unknown scan": "null",
    "onn other remote": "null", "onn its remote": "freetv", "Hisense 295": "prime", "filter TCL 11": "false", "filter TCL 12": "true",
    "valid pkg": "true", "valid shell": "false",
    "outside default passes": "0", "outside home passes": "0", "outside other key passes": "0", "outside chosen opens": "2:org.smarttube.stable",
    "outside repeat eaten": "1", "outside release eaten": "1", "outside next press opens": "2",
    "xiaomi first opens": "2", "xiaomi burst eaten": "1", "xiaomi later opens": "2",
}
for k, v in want.items():
    check(f"key apps: {k}", got.get(k) == v, f"{got.get(k)!r}, wanted {v!r}")
sys.exit(1 if failed else 0)
