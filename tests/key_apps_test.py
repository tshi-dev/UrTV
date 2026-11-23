#!/usr/bin/env python3
"""«أزرار الريموت» (KeyApps.java): which app the remote's YouTube, Netflix and Prime Video keys open. A key with no choice keeps its own
app and, outside our home, passes untouched; a chosen key opens its app on our home (MainActivity, before RemoteKeys) and over other apps
(«إضافات تلفزيونك»'s filter, 4.4: the home has no accessibility service any more), consuming its repeats and release. Then the wiring, read from the source.   Run: python3 tests/key_apps_test.py"""
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
ka, rk, main, dock, recv = src("KeyApps.java"), src("RemoteKeys.java"), src("MainActivity.java"), src("Dock.java"), src("DockReceiver.java")
EX = ROOT / "android/extras"
ts = (EX / "src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
kb, bridge = src("KeyButton.java"), src("ExtrasBridge.java")
rb = src("RemoteButtons.java")
check("no Android in KeyApps or RemoteButtons", "import android" not in ka and "import android" not in rb)
ss_rb = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/RemoteButtons.java").read_text(encoding="utf-8")
check("the screensaver's RemoteButtons is the home's, but for its package line and the line naming the other copy",
      ss_rb.split("\n", 1)[1].replace("is in the home app (sa.urtv.home.RemoteButtons), which acts on the keys;", "X")
      == rb.split("\n", 1)[1].replace("is in the screensaver app (sa.urtv.screensaver.RemoteButtons), which draws the rows;", "X"))
check("Free My TV's licence is named for the table", "Free My TV" in rb and "MIT" in rb and "RemoteButtons.java" in (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8"))
check("RemoteKeys is untouched by the choices (a button on its default does exactly what it did): no KeyApps, no choices in it", "KeyApps" not in rk and "choices" not in rk)
main_key = main[main.index("String keyId = KeyButton.of(e);"):main.index("RemoteKeys.Decision key = remoteKeys.decide(")]
check("on our home a chosen button that is installed is answered first (Xiaomi's burst, its repeats and release), kids mode's hidden app opens nothing, the PIN as its tile",
      "KeyApps.chosen(keyChoice) && isInstalled(keyChoice) || homeButtons.holding(e.getKeyCode())" in main_key and "KeyButton.burstMs()" in main_key
      and "if (ParentalPolicy.visible(Parental.state(this), pkg)) guarded(ParentalPolicy.REMOTE_APP, pkg, labelOf(pkg), () -> openPackage(pkg));" in main_key)
check("the add-on (over other apps) asks for keys only while a button was given an app and a filter is safe here; it looks only at this remote's app buttons",
      "|| KeyApps.filterWanted(front, KeyApps.anyChosen(keyChoices), KeyButton.filterSafe())" in ts and "if (appKey(e)) return true;" in ts
      and "if (!KeyButton.filterSafe()) return false;" in ts and "|| homeKeyActive());" in ts and "KeyButton.homeCatchSafe()" in ts.split("private boolean homeKeyActive()")[1].split("\n")[0] and "String id = KeyButton.of(e);" in ts and "if (id == null) return false;" in ts
      and "RemoteButtons.idOf(TV, e.getKeyCode(), e.getScanCode(), pad, vendor, product)" in kb)
check("over other apps: kids mode's hidden app stays closed, the PIN or a home that does not answer opens our home (which asks the PIN, then opens the app, only a chosen one)",
      'r.getBoolean(ExtrasWords.HIDDEN, true)) { log("key app=hidden"); return; }' in ts and "if (r == null || r.getBoolean(ExtrasWords.NEEDS_PIN, true))" in ts
      and "putExtra(ExtrasWords.EXTRA_KEY_APP, pkg)" in ts and "ParentalPolicy.targets(ParentalPolicy.REMOTE_APP, pkg)" in bridge
      and "!Dock.keyApps(this).containsValue(pkg) || !ParentalPolicy.visible(Parental.state(this), pkg)" in main)
check("the home tells the add-on when the choices change, and the add-on asks when it starts (kept for a restart)",
      "tellExtras(c);" in dock and "Extras.keyApps(app, line)" in dock and "case ExtrasWords.KEY_APPS:" in bridge
      and "askHome(this, ExtrasWords.KEY_APPS, null, null)" in ts and "case ExtrasWords.KEY_APPS:" in (EX / "src/main/java/sa/urtv/home/ExtrasCallProvider.java").read_text(encoding="utf-8")
      and all(f in (EX / "build.gradle").read_text(encoding="utf-8") for f in ("'KeyApps.java'", "'RemoteButtons.java'", "'KeyButton.java'")))
check("no new permission for the home (Play Protect): the add-on only sees launchable apps (queries), the screensaver only the add-on's package",
      "QUERY_ALL_PACKAGES" not in (EX / "src/main/AndroidManifest.xml").read_text(encoding="utf-8") and "android.intent.category.LEANBACK_LAUNCHER" in (EX / "src/main/AndroidManifest.xml").read_text(encoding="utf-8")
      and "QUERY_ALL_PACKAGES" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8") and "AccessibilityService" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
check("the choice is checked and must be installed before it is saved", "KeyApps.validChoice(pkg)" in recv and "Dock.installed(c, pkg)" in recv)
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sa, sh, pv, pr = (A / "SettingsActivity.java").read_text(encoding="utf-8"), (A / "SettingsHub.java").read_text(encoding="utf-8"), (A / "ConfigProvider.java").read_text(encoding="utf-8"), (A / "Prefs.java").read_text(encoding="utf-8")
titles = re.findall(r'b\("[a-z]+", "([^"]+)"', rb)
check("on the TV: a row in «الأزرار السريعة» only for each button this remote has, each a list of the TV's apps with «الافتراضي» first",
      "RemoteButtons.buttons(Prefs.remoteTv())" in sa and "pickKeyApp(k.id, k.title)" in sa and titles and all(t.endswith(" في الريموت") and f'{{"{t}", BUTTONS}}' in sh for t in titles), titles)
check("the screensaver hands the choices to the home (key_apps), and the home takes them only when they change", '"key_apps"' in pv and "p.keyApps()" in pv and "Dock.followKeyApps(this, cfg.keyApps);" in main and 'cur.getColumnIndex("key_apps")' in (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8"))
check("the row says when a button works on our home only: no «إضافات تلفزيونك» (or its service off), or TCL's Android 11; no «•» in it",
      "، على الشاشة الرئيسية فقط في هذا التلفزيون" in sa and "، على الشاشة الرئيسية فقط (وفوق التطبيقات مع «إضافات تلفزيونك»)" in sa and "•" not in sa[sa.index("private String keyAppLabel"):sa.index("private void pickKeyApp")]
      and '<package android:name="sa.urtv.extras" />' in (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
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
    "filter Xiaomi AFKR0": "false", "filter Xiaomi MiTV-M": "false", "filter Xiaomi PatchWall": "false", "filter Sony": "true",   # 4.4 beta 3 (XMRM-M3): no key filter outside our home on Xiaomi
    "valid pkg": "true", "valid shell": "false",
    "outside default passes": "0", "outside home passes": "0", "outside other key passes": "0", "outside chosen opens": "2:org.smarttube.stable",
    "outside repeat eaten": "1", "outside release eaten": "1", "outside next press opens": "2",
    "xiaomi first opens": "2", "xiaomi burst eaten": "1", "xiaomi later opens": "2",
    "line": "youtube=org.smarttube.stable,netflix=,prime=", "parse": "{youtube=org.smarttube.stable, netflix=}", "parse null": "{}",
    "any chosen": "true", "none chosen": "false",
    "filter outside chosen": "true", "filter home front": "false", "filter all default": "false", "filter wanted TCL 11": "false",
    "after home burst eaten": "1", "after home release eaten": "1", "after home later opens": "2",
    "no burst window opens": "2", "holding after open": "true", "not holding after release": "false",
    "global TCL youtube ours": "com.google.android.youtube.tv", "global TCL youtube not ours": "null", "global TCL netflix": "null", "global Sony youtube": "null",
    "global wanted TCL": "true", "global wanted TCL 11": "false", "global wanted not ours": "false",
    "global over our home opens": "2:com.google.android.youtube.tv",
}
svc = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
ak = svc.split("private boolean appKey(KeyEvent e) {")[1].split("\n    }\n")[0]
check("beta 3: on its default, a TCL's YouTube button opens its own app from the add-on while our home is the home, our screen in front or not; the owner's choice wins; a missing app is said",
      "KeyApps.globalOwn(KeyButton.TV, id, wanted(this) || oursChosen())" in ak and "if (!KeyApps.chosen(choice)) {" in ak and "global ? false : homeFront" in ak
      and 'log("key app=noyoutube")' in ak)
check("beta 3: the add-on asks for key events for it (a TCL, our home the person's, the filter safe)",
      "KeyApps.globalWanted(KeyButton.TV, wanted(this) || oursChosen(), KeyButton.filterSafe())" in svc.split("private void filterKeys() {")[1].split("\n    }\n")[0])
for k, v in want.items():
    check(f"key apps: {k}", got.get(k) == v, f"{got.get(k)!r}, wanted {v!r}")
sys.exit(1 if failed else 0)
