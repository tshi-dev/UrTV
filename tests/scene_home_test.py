#!/usr/bin/env python3
"""4.3.3 «الآفاق» (docs/mockups, docs/mockups/PERFORMANCE.md): a second home style, chosen in «المظهر ← الواجهة»; the home as it was stays the
default. A scene's photo, its big glass clock and the scenery cut out over the clock; «آفاق السعودية» in the app, «آفاق عالمية» downloaded once from
urtv-data with its sha256; on a weak TV (LOW) a still photo at 1280, no cut-out, no fade.

    python3 tests/scene_home_test.py      (ScenePlan runs on a JVM: needs javac; Pillow for the files' sizes)

ScenePlan (the rules) runs for real (tests/ScenePlanRun.java); the files of the bundled pack are measured; the wiring is read from the source."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
PACK = ROOT / "android/launcher/src/main/assets/scenes/saudi"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p): return p.read_text(encoding="utf-8")


plan, clock, scene, dl, main, snap, card = (read(HOME / f) for f in ("ScenePlan.java", "SceneClock.java", "SceneHome.java", "SceneDownload.java", "MainActivity.java", "ConfigSnapshot.java", "HomeCard.java"))
prefs, prov, recv, st, hub = (read(APP / f) for f in ("Prefs.java", "ConfigProvider.java", "SaverReceiver.java", "SettingsActivity.java", "SettingsHub.java"))
ram = read(HOME / "Ram.java")

# ---- the rules, on a JVM
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "ScenePlan.java"), str(ROOT / "tests/ScenePlanRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.ScenePlanRun", str(PACK / "scenes.tsv")],
                             check=True, capture_output=True).stdout.decode("utf-8").splitlines()
    rows = [l.split("\t") for l in out]
    scenes = [r for r in rows if r[0] == "scene"]
    check("4.4 the app carries one Saudi scene, Riyadh, with its cut-out (the four others are downloaded the first time: the owner keeps the APK light)",
          [s[1] for s in scenes] == ["riyadh"] and all(s[5] != "-" for s in scenes), scenes)
    check("a bad line is left out, never half read (a bad id, a clock too small to read, a bad box); a good one is kept",
          [r for r in rows if r[0] == "bad"][0][1] == "1")
    packs = {r[1]: r[2] for r in rows if r[0] == "packs"}
    check("the packs that are on: none named (or unknown) is the Saudi pack alone; the order is Saudi, then Couch TV",
          packs == {"": "saudi", "saudi": "saudi", "couch": "couch", "couch,saudi": "saudi,couch", "nope": "saudi"}, packs)
    tiers = {r[1]: r[2:] for r in rows if r[0] == "tier"}
    check("LOW («خفيف»): the photo at 1280, no cut-out, no fade, a scene every 5 minutes (PERFORMANCE.md)", tiers["0"] == ["1280", "false", "false", "300000", "300000"], tiers["0"])
    check("MID and NORMAL: the screen's width, the cut-out and the fade, 20 s a scene (10 s at least)",
          tiers["1"] == tiers["2"] == ["1920", "true", "true", "20000", "10000"], tiers)
    box = {(r[1], r[2]): [int(x) for x in r[3].split(",")] for r in rows if r[0] == "box"}
    top, bottom = int(scenes[0][3]), int(scenes[0][4])
    check("the clock: in the middle with no cards; with the cards (on the left of an Arabic home) on the right half, on the left in English",
          box[("false", "true")][0] + box[("false", "true")][2] == 1920 and box[("true", "true")][0] >= 960 and box[("true", "false")][2] <= 960, box)
    check("the clock stands on the scene's own bottom line (its scenery crosses the digits there), and fits the screen",
          all(b[3] == bottom and b[1] >= top and 0 <= b[0] < b[2] <= 1920 for b in box.values()), box)
    b720 = [int(x) for x in [r for r in rows if r[0] == "box720"][0][1].split(",")]
    check("a 720 screen: the same place, scaled", b720[3] == round(bottom * 720 / 1080) and b720[0] >= 640 and b720[2] <= 1280, b720)
    check("the scenes take turns in order, the first first; none: nothing", [r for r in rows if r[0] == "next"][0][1] == "0,0,-1")
    ch = {r[1]: (r[2], r[3] if len(r) > 3 else "") for r in rows if r[0] == "choose"}
    check("the scenes now (ScenePlan.choose): both rules off, all; «يتبع الوقت» at night the night scenes, by day the others; «يتبع الطقس» in fog the fog scenes (it wins); no fog: as the time says",
          ch["falsefalsefalsefalse"][0] == "aabbcc" and ch["truetruefalsefalse"][0] == "aa" and ch["truefalsefalsefalse"][0] == "bbcc"
          and ch["falsefalsetruetrue"][0] == "bb" and ch["truetruetruetrue"][0] == "bb" and ch["falsefalsetruefalse"][0] == "aabbcc", ch)
    check("the app's scene: Riyadh is a night scene; by day, with no day scene yet, and in fog, with no fog scene, the rules let go (never an empty home)",
          ch["truetruefalsefalse"][1] == "riyadh" and ch["truefalsefalsefalse"][1] == "riyadh" and ch["falsefalsetruetrue"][1] == "riyadh", ch)
    check("«بقاء كل أفق»: 10, 20, 60 or 300 s, anything else 20; fog is Open-Meteo's 45 and 48",
          [r for r in rows if r[0] == "stay"][0][1] == "10,300,20,20" and [r for r in rows if r[0] == "fog"][0][1] == "true,true,false")
    check("ScenePlan's tiers are Ram's numbers", [r for r in rows if r[0] == "tiers"][0][1] == "0,1,2"
          and "public static final int LOW = 0, MID = 1, NORMAL = 2;" in ram)
else:
    print("SKIP scene rules: no javac here")

# ---- the bundled pack's files: their sizes (PERFORMANCE.md), the cut-out's box, the credits
try:
    from PIL import Image
except ImportError:
    Image = None
lines = [l.split("\t") for l in read(PACK / "scenes.tsv").splitlines() if l and not l.startswith("#")]
ids = [l[0] for l in lines]
over, bad = [], []
for l in lines:
    i = l[0]
    for suffix, cap in (("", 320), ("-glass", 80)):
        f = PACK / f"{i}{suffix}.jpg"
        if not f.is_file() or f.stat().st_size > cap * 1024: over.append(f.name)
    fg = PACK / f"{i}-fg.webp"
    if l[5] != "-" and (not fg.is_file() or fg.stat().st_size > 200 * 1024): over.append(fg.name)
    if Image:
        if Image.open(PACK / f"{i}.jpg").size != (1920, 1080) or Image.open(PACK / f"{i}-glass.jpg").size != (960, 540): bad.append(i)
        if l[5] != "-":
            x, y, w, h = map(int, l[5].split(","))
            im = Image.open(fg)
            if im.size != (w, h) or im.mode != "RGBA": bad.append(i + " cut-out " + str(im.size))
check("each photo at most 320 KB, its glass copy 80 KB, its cut-out 200 KB (PERFORMANCE.md); every file the list names is there", not over, over)
check("photos 1920x1080, glass copies 960x540, each cut-out the size of its box with its transparency", not bad, bad)
names = {p.name for p in PACK.iterdir()}
want = {"scenes.tsv", "CREDITS.txt"} | {f"{i}{s}" for i in ids for s in (".jpg", "-glass.jpg", "-fg.webp")}
check("nothing else in the pack's folder", names == want, sorted(names ^ want))
credits = read(PACK / "CREDITS.txt")
check("every scene is credited (photographer, licence, source), the CC BY-SA one says its glass and cut-out carry its licence",
      all(re.search(rf"^{i}\s", credits, re.M) for i in ids) and "B.alotaby" in credits and "Richard Mortel" in credits and "Unsplash License" in credits
      and "The glass copy and the cut-out of this photo are under CC BY-SA 4.0 too." in credits)
check("docs/PHOTO-CREDITS.md lists the scenes too", all(f"scenes/saudi/{i}.jpg" in read(ROOT / "docs/PHOTO-CREDITS.md") for i in ids))

# ---- the Couch TV pack: downloaded once from urtv-data, checked before it is opened
check("the downloaded packs (couch, and the Saudi pack's four others): from urtv-data's scenes/ (its CDN copy when GitHub's raw host cannot be reached), nowhere else; one index per pack",
      'HOSTS = {"https://raw.githubusercontent.com/tshi-dev/urtv-data/data/scenes/", "https://cdn.jsdelivr.net/gh/tshi-dev/urtv-data@data/scenes/"}' in dl
      and 'get(pack + ".json", 16 * 1024)' in dl and 'get(pack + ".zip", MAX_BYTES)' in dl and "static boolean known(String pack) { return ScenePlan.SAUDI.equals(pack) || ScenePlan.COUCH.equals(pack); }" in dl
      and "SceneDownload.fetchIfNeeded(app, p, () ->" in scene)
check("…its size and sha256 from the index checked before the zip is opened; a name that is not a scene file, nothing kept; 12 MB at most; moved into place whole",
      "zip.length != Integer.parseInt(want[1])" in dl and 'hex(MessageDigest.getInstance("SHA-256").digest(zip)).equals(want[0])' in dl
      and dl.index("MessageDigest.getInstance") < dl.index("new ZipInputStream") and "if (e.isDirectory() || !allowed(e.getName())) { delete(work); return false; }" in dl
      and "MAX_BYTES = 12 * 1024 * 1024" in dl and "work.renameTo(done)" in dl and r'"[a-z][a-z0-9-]{1,23}(\\.jpg|-glass\\.jpg|-fg\\.webp)"' in dl)
check("…a failure waits a day; the Saudi pack shows meanwhile", "RETRY_MS = 24L * 60 * 60 * 1000" in dl
      and "if (all.isEmpty()) all.addAll(ScenePlan.parse(ScenePlan.SAUDI" in scene)

# ---- the clock and the layers
check("the glass clock: the scene's glass copy as a BitmapShader in the digits, brighter, a light edge and a soft dark shadow; a pale scene a dark fill",
      "new BitmapShader(glass" in clock and "float k = dark ? 0.42f : 1.45f;" in clock and "edge.setStyle(Paint.Style.STROKE);" in clock
      and "shadow.setShadowLayer(" in clock and clock.index("c.drawText(text, x, y, shadow);") < clock.index("c.drawText(text, x, y, fill);"))
check("the layers: the photo, the clock, then the cut-out over the clock; decoded off the UI thread; only the showing and the next scene in memory",
      "box.addView(photo" in scene and scene.index("box.addView(photo") < scene.index("box.addView(clock") < scene.index("box.addView(mask")
      and "io.execute(() -> {" in scene and "final Slot[] slots = new Slot[2];" in scene and "was.release();" in scene)
check("the tier's rules are ScenePlan's: decode width, cut-out, fade, stay", all(f"ScenePlan.{f}(tier" in scene for f in ("decodeWidth", "mask", "fade", "stayMs")))
check("no timer while another screen is in front; a running timer is not reset by the minute tick", "void stop() { running = false; ui.removeCallbacksAndMessages(null); }" in scene
      and "if (running) return;" in scene and "if (sceneHome != null) sceneHome.stop();" in main and "if (sceneHome != null) sceneHome.start();" in main)

# ---- the home: an option, the home as it was by default
tick = main[main.index("private void sceneTick(ConfigSnapshot cfg)"):].split("\n    }\n")[0]
check("the home: the scenes only when chosen; then the scene replaces the photo, and with the glass clock the header's clock (the rest stays)",
      'boolean on = "scenes".equals(cfg.homeBg);' in tick and "clock.setVisibility(bigClock ? View.INVISIBLE : View.VISIBLE);" in tick and "wallFront.setVisibility(show);" in tick
      and "sceneHome.setTime(clock.getText());" in tick and "sceneTick(cfg);" in main and "homeCard.shownCards() > 0" in tick)
check("the scene sits over the photos and under the dimming and the scrims (put just after the front photo when made)",
      "sceneBackdrop.addView(sceneHome, sceneBackdrop.indexOfChild(wallFront) + 1," in main and main.index("sceneBackdrop = backdrop;") < main.index("backdrop.addView(wallDimView"))
check("the home reads the layout and the packs from the screensaver (an older one says nothing: classic, saudi)",
      'String homeLayout = "classic", scenePacks = "saudi", sceneStay = "20";' in snap and 'cur.getColumnIndex("scene_packs")' in snap and "int shownCards() { return cards; }" in card)

# ---- 4.4 the owner: «العادية» is the default and costs nothing of the scenes; «الآفاق» costs no ordinary photo
import glob
body = tick.split("\n", 1)[1]
first = [l.strip() for l in body.splitlines() if l.strip() and not l.strip().startswith("//")]
check("off (the default): sceneTick's first step after reading the layout returns when the scenes are off and nothing of them exists",
      first[0].startswith('boolean on = "scenes".equals(cfg.homeBg);') and first[1] == 'boolean glass = "glass".equals(cfg.homeClock);'
      and first[2] == "photoGlassTick(cfg, !on && glass);" and first[3].startswith("if (!on && sceneHome == null && !scenesOn) return;"), first[:4])
check("off: the scene layer is not made at the home's start, only in sceneTick when chosen; turned off, it is shut down and removed whole",
      main.count("new SceneHome(") == 1 and "if (on && sceneHome == null) {" in tick and tick.index("if (on && sceneHome == null) {") < tick.index("new SceneHome(")
      and "sceneHome.shutdown();" in tick and "sceneBackdrop.removeView(sceneHome);" in tick and "sceneHome = null;" in tick
      and "new SceneHome(" not in main[main.index("private View buildUi()") if "private View buildUi()" in main else 0:main.index("private void sceneTick(")])
check("off: every other use of the layer waits for it to exist (start, stop, trim)", main.count("if (sceneHome != null) sceneHome.") == 3)
users = sorted(Path(f).name for f in glob.glob(str(HOME / "**/*.java"), recursive=True)
               if re.search(r"\b(SceneHome|SceneDownload|SceneClock)\b", read(Path(f))) and Path(f).name not in ("SceneHome.java", "SceneDownload.java", "SceneClock.java", "ScenePlan.java", "PhotoGlass.java"))
check("off: only the home's screen (and 4.4's glass clock over the photos, made only while chosen: tests/photo_glass_test.py) knows the scene classes (nothing else can start a read, a download or a timer)", users == ["MainActivity.java"], users)
seth = scene[scene.index("    void set(boolean wanted"):].split("\n    }\n")[0]
check("off: the scene files are read and the Couch TV pack is downloaded only after «on» is checked; shut down, the thread, the timer and the pictures go",
      seth.index("if (!on) {") < seth.index("io.execute(") < seth.index("SceneDownload.fetchIfNeeded(app, p,") and "assets" not in main.split("private void sceneTick(")[0][-400:]
      and "io.shutdownNow();" in scene and "for (Slot sl : slots) sl.release();" in scene)
check("off: the scenes' memory tier, font and pictures are asked for only when the layer is made (none at the home's start)",
      main.count("new SceneHome(this, Ram.tier(this), font(\"display\"))") == 1)

# ---- the settings
check("4.4 three choices: «التخطيط» (تلفزيونك, the default, كنبة or كرة القدم), «الخلفية» (photos, the default, or scenes), «الساعة» (plain, the default, or glass)",
      'HOME_LAYOUTS = {"classic", "couch", "football"};' in prefs and '{"تلفزيونك", "كنبة", "كرة القدم"}' in prefs and 'HOME_BGS = {"photos", "scenes"};' in prefs
      and 'HOME_CLOCKS = {"plain", "glass"};' in prefs and 'sp.getString("homeBg", oldScenes() ? "scenes" : "photos")' in prefs and 'sp.getString("homeClock", oldScenes() ? "glass" : "plain")' in prefs)
check("the presets set the three at once: «شكل تلفزيونك» (our layout, photos, plain clock; the default) and «شكل كنبة» (the couch layout, scenes, glass clock); any other mix reads «مخصّص»",
      '{{"urtv", "شكل تلفزيونك", "classic", "photos", "plain", "frame"}, {"couch", "شكل كنبة", "couch", "scenes", "glass", "zoom"}}' in prefs and 'return "مخصّص";' in prefs
      and 'row("الشكل الجاهز", Prefs.look3Label(prefs.look3())' in st and 'row("التخطيط"' in st and 'row("الخلفية"' in st and 'row("الساعة"' in st)
check("the home: «الخلفية ← الآفاق» shows the scenes on any layout; the glass clock and the cut-out only with «الساعة ← زجاجية», else the header's clock stays",
      'boolean on = "scenes".equals(cfg.homeBg);' in tick and 'boolean glass = "glass".equals(cfg.homeClock);' in tick and "boolean bigClock = glass;" in tick
      and "final Bitmap glass = withClock ? decode(" in scene and "final Bitmap mask = withClock && sc.mask != null" in scene
      and 'if ("scenes".equals(homeLayout)) { homeLayout = "classic"; homeBg = "scenes"; homeClock = "glass"; }' in snap)
check("«حزم الآفاق» (the owner's «الآفاق», was «المشاهد»): «آفاق السعودية» and «آفاق عالمية» (the Couch TV pack), none chosen is the Saudi one; in «المظهر» ← «الخلفية والآفاق»",
      '{"saudi", "آفاق السعودية"}, {"couch", "آفاق عالمية"}' in prefs and 'if (out.isEmpty()) out.add("saudi");' in prefs
      and 'row("حزم الآفاق"' in st
      and '{LOOK, H + "الشكل الجاهز والتخطيط", "الشكل الجاهز", "التخطيط", "وضع الألوان", "صفحات الأسهم والمباريات داكنة",' in hub
      and 'H + "الخلفية والآفاق", "الخلفية", "حزم الآفاق", "بقاء كل أفق", "الأفق يتبع الوقت", "الأفق يتبع الطقس",' in hub and '{"الساعة", LOOK}, {"شكل التطبيقات", LOOK}, {"حزم الآفاق", LOOK}' in hub)
check("«بقاء كل أفق», «الأفق يتبع الوقت», «الأفق يتبع الطقس»: rows in «المظهر», off by default (20 s), given to the home, which picks night by the prayer times and fog by the greeting's weather",
      'row("بقاء كل أفق"' in st and 'row("الأفق يتبع الوقت"' in st and 'row("الأفق يتبع الطقس"' in st and 'sp.getBoolean("sceneByTime", false)' in prefs
      and 'sp.getBoolean("sceneByWeather", false)' in prefs and 'sp.getString("sceneStay", "20")' in prefs and '"scene_stay", "scene_time", "scene_weather"' in prov
      and "night = t >= d.maghrib || t < d.fajr;" in main and 'ScenePlan.fog(g.getInt("code", -1))' in main and "sceneHome.mood(" in main and "ScenePlan.stay(cfg.sceneStay)" in main)
check("the ordinary photos are not decoded while the scene home shows (memory): no rotation, a photo on its way not shown, the two layers emptied; back to classic, a photo again",
      "if (scenesOn) return;" in main and main.index("if (scenesOn) return;") < main.index("final int gen = ++wallGen;")
      and "for (ImageView w : new ImageView[] {wallBack, wallFront}) w.setImageDrawable(null);" in tick and "} else nextWallpaper();" in tick)
check("the provider gives scene_packs, home_bg and home_clock; saver.sh scenepacks, look, background, clock (and 4.3.3's homelayout scenes = the couch preset); `show` says them",
      '"scene_packs"' in prov and '"home_bg", "home_clock"' in prov and 'Prefs.join(p.scenePacks(), ",")' in prov and 'getStringExtra("scenepacks")' in recv and '"\\nscenepacks: "' in recv
      and 'if ("scenes".equals(homeLayout)) { homeLayout = null; look3 = "couch"; }' in recv and '"\\nbackground: " + p.homeBg()' in recv
      and all(f"{c}) " in read(ROOT / "optimize/saver.sh") for c in ("scenepacks", "preset", "background", "clock")))
sh = read(ROOT / "optimize/saver.sh")
check("the preset has its own command (saver.sh preset urtv|couch, the extra «preset»); «look» stays the screens' look (glass, graphite…): one look) case, one reader of the extra each",
      sh.count("\n  look)") == 1 and "usage: optimize/saver.sh look glass|graphite|navy|clean|sand" in sh and 'set1 preset "${2:?usage: optimize/saver.sh preset urtv|couch}"' in sh
      and recv.count('i.getStringExtra("look")') == 1 and 'String look3 = i.getStringExtra("preset")' in recv and '"\\npreset: "' in recv)
main_tile = main[main.index("card.setOnFocusChangeListener((v, has) -> {"):main.index("card.setOnLongClickListener(")]
check("4.4 «شكل التطبيقات»: إطار (the default: the ring, the name above the tray) or تكبير وظل (larger with a soft shadow, no ring, the name under the tile); «شكل كنبة» turns it on",
      'HOME_APPS = {"frame", "zoom"};' in prefs and '{"إطار", "تكبير وظل"}' in prefs and 'sp.getString("appsStyle", "frame")' in prefs and 'row("شكل التطبيقات"' in st
      and '"apps_style"' in prov and 'getStringExtra("appsstyle")' in recv and "appsstyle) " in read(ROOT / "optimize/saver.sh")
      and "final boolean zoom = inDock && appsZoom;" in main_tile and "FavBar.ZOOM_SCALE" in main_tile and "alpha(has && !zoom ? 1f : 0f)" in main_tile
      and "placeZoomName(appsZoom && inBar ? f : null);" in main and 'if (as >= 0 && "zoom".equals(cur.getString(as))) appsStyle = "zoom";' in snap)
sys.exit(1 if failed else 0)
