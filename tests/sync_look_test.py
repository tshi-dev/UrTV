#!/usr/bin/env python3
"""4.3 «توحيد المظهر» (design/43 sync-*): the home's upper buttons (PillBar), cards (HomeCard, the page's #hpanel) and apps bar (the tray's band) take ONE material from
GlassStyle.material: fill, line, corners 24/16, shadow, ink, accent and focus. On (the default) the three are the same in every look, dark and light; off, each has its own strength
and fill, and with nothing chosen each is what it was before. The numbers are run for real (tests/SyncLookRun.java, every look); the plumbing is pinned here; the cards in a
browser are tests/sync_cards.test.mjs.

    python3 tests/sync_look_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "GlassStyle.java"), str(ROOT / "tests/SyncLookRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SyncLookRun"], capture_output=True, text=True).stdout
            bad = [l for l in out.splitlines() if l.startswith("FAIL")]
            n = re.search(r"ALL PASS (\d+)", out)
            check(f"every look, dark and light: on, the same token values reach all three surfaces; off keeps them separate ({n.group(1) if n else 0} cases)", not bad and n and int(n.group(1)) > 600, bad[:3])
gs, fav, bar, hc, main, pill = (read(HOME / f) for f in ("GlassStyle.java", "FavBar.java", "PillBar.java", "HomeCard.java", "MainActivity.java", "Pill.java"))
prefs, rcv, app, words = (read(APP / f) for f in ("Prefs.java", "SaverReceiver.java", "AppearanceActivity.java", "Words.java"))
check("one token set in one place: both apps' GlassStyle are the same file", gs.split("\n", 1)[1] == read(APP / "GlassStyle.java").split("\n", 1)[1])
check("the tokens: corners 24 outside and 16 inside, no shadow (none on all three), three surfaces, three fills",
      "RADIUS_OUTER = 24, RADIUS_INNER = 16;" in gs and "public static final int SHADOW = 0;" in gs and 'SURFACES = {"quick", "cards", "tray"}' in gs and 'FILLS = {"look", "glass", "graphite"}' in gs
      and 'FILL_BEFORE = {"look", "glass", "glass"}' in gs)
check("the white glass of before is one set of numbers (FavBar's own, which tests/FavBarRun.java runs alone)",
      "GLASS_FILL_DARK = 0x24FFFFFF, GLASS_LINE_DARK = 0x47FFFFFF, GLASS_FILL_LIGHT = 0xB8FFFFFF, GLASS_LINE_LIGHT = 0xD9FFFFFF;" in gs
      and "FILL_DARK = 0x24FFFFFF, LINE_DARK = 0x47FFFFFF, FILL_LIGHT = 0xB8FFFFFF, LINE_LIGHT = 0xD9FFFFFF;" in fav and "RADIUS = 24," in fav and "RING_LIGHT" not in fav)
# ---- the three surfaces
check("the upper buttons: PillBar draws the material's fill, line and outer corner; a button in it has the inner corner (8 dp = 16 px) and the look's focus in the focus style",
      "void look(GlassStyle.Material m) {" in bar and "fill.setColor(none ? 0 : m.fill);" in bar and "line.setColor(none ? 0 : m.line);" in bar and "radius = m.radius / 2f * dp;" in bar
      and "GlassStyle.barPercent()" not in bar and "pillBar.look(GlassStyle.material(glassNow, lookNow, GlassStyle.QUICK));" in main and "bg.setCornerRadius(dp(8));" in pill and "GlassStyle.focusStyle()" in pill)
apply = main[main.index("private void applyLook(boolean light)"):].split("\n    }\n")[0]
check("the apps bar: the band is the material's fill and line, its corner FavBar.RADIUS (24), its tiles' the inner 16; the focused tile's ring is the material's colour and width",
      "GlassStyle.Material m = GlassStyle.material(lookNow, light, GlassStyle.BAR);" in apply and "dockBand.setColor(noGlass ? 0 : m.fill);" in apply and "trayRingColor = m.ring; trayRingPx = m.ringWidth;" in apply
      and "r.setStroke(trayRingW(), trayRing());" in apply and "band.setCornerRadius(bar(FavBar.RADIUS));" in main and "TILE_RADIUS = 16" in fav and "FavBar.FILL_" not in main and "FavBar.LINE_" not in main)
check("the cards: HomeCard sends the material's fill and line, and with the switch on its ink (text, second text, accent); nothing of FavBar's and no strength of its own",
      "return u + glassQuery(GlassStyle.material(lookNow, lightNow, GlassStyle.CARDS));" in hc and '"&hpanel=" + GlassStyle.rgba(m.fill) + "&hline=" + GlassStyle.rgba(m.line)' in hc
      and 'm.ownInk ? q + "&hink=" + GlassStyle.rgba(m.text) + "&hsub=" + GlassStyle.rgba(m.sub) + "&hacc=" + GlassStyle.rgba(m.accent) : q' in hc and "FavBar." not in hc.split("glassQuery")[1].split("create(")[0]
      and "GlassStyle.barPercent()" not in hc and "homeCard.look(lookNow); homeCard.light(light);" in apply)
check("a change of the switch or of a surface repaints the home once (the key applyLook compares), like a touch of «اللون والتحكم»",
      '+ "|" + GlassStyle.materialKey();' in apply and "if (light == lightNow && key.equals(paletteLook)) return;" in apply)
# ---- nothing on the tray's key and scroll path (the home is chasing one late frame per key press there)
step = main[main.index("private void liftCard(View card, boolean lifted, float focusScale)"):].split("\n    }\n")[0]
check("no work on a focus step or a scroll: the material is made where the look is applied, where the tray is built and on the minute's tick, nowhere else; a focus step reads two fields",
      main.count("GlassStyle.material(") == 3 and "GlassStyle.material(" not in step and "trayRing()" in step and "private int trayRing() { return trayRingColor; }" in main)
check("PillBar and FavBar draw with no layer, blur or shadow of their own; the material adds none where the look is applied",
      all(w not in f for f in (bar, fav, apply.split("GlassStyle.Material m =")[1].split("if (wallDimView")[0]) for w in ("setLayerType(", "setElevation(", "BlurMaskFilter", "RenderEffect", "saveLayer", "setShadowLayer(")))
check("the control center is not part of the switch", "GlassStyle.material(" not in read(HOME / "ControlCenter.java"))
# ---- the setting
check("Prefs: on by default; off, a surface never set keeps «شفافية الشريط» and what it wore before; the string both apps read carries the surfaces only while it is off",
      'boolean lookSync() { return sp.getBoolean("lookSync", true); }' in prefs and 'sp.getInt("surfacePercent" + surface, barPercent())' in prefs and 'sp.getString("surfaceFill" + surface, GlassStyle.FILL_BEFORE[surface])' in prefs
      and 's.append(";sync=off");' in prefs and prefs.count("GlassStyle.setTweaks(tweaks());") >= 8)
check("an older screensaver says nothing: the switch is on; an older launcher skips what it does not know", 'else if ("sync".equals(k)) sync = !"off".equals(v);' in gs and "boolean sync = true, clear = false;" in gs
      and "if (pct[i] < 0) pct[i] = bar;" in gs)
check("an agent can set them, and `show` says them", all(w in rcv for w in ('getStringExtra("looksync")', 'getStringExtra("surface")', '"\\nlooksync: "', '"\\nsurface quick: "', '"\\nsurface cards: "', '"\\nsurface tray: "',
                                                                  "p.setLookSync(\"on\".equals(syncX));", '"error: looksync is one of on off"')))
saver = read(ROOT / "optimize/saver.sh")
check("saver.sh: looksync and surface are commands, each in the usage; the settings file carries the switch",
      '  looksync) set1 looksync "' in saver and "  surface) set1 surface " in saver and all(f"optimize/saver.sh {k} " in saver.split("set -euo")[0] for k in ("looksync", "surface"))
      and '"look_sync"' in read(ROOT / "optimize/settings_move.py") and 'es("looksync", flag(v))' in read(ROOT / "optimize/settings_move.py"))
check("«المظهر»: the tile «توحيد المظهر» says «مفعّل» or «متوقف» (the on/off rule), its page shows both before one is chosen, and off, three surfaces each with a strength and a fill",
      '{"sync", "توحيد المظهر"}' in app and "value = w(prefs.lookSync() ? OnOff.ON_WORD : OnOff.OFF_WORD);" in app and 'tile("sync:on", new Mock(Mock.SYNC, "on", home), w(OnOff.ON_WORD), on,' in app
      and 'tile("sync:off", new Mock(Mock.SYNC, "off", home), w(OnOff.OFF_WORD), !on,' in app and "if (!on) for (int i = 0; i < GlassStyle.SURFACES.length; i++) {" in app
      and 'stepper("surface" + s,' in app and 'tile("fill" + s + ":" + fill,' in app and 'ON_WORD = "مفعّل", OFF_WORD = "متوقف"' in read(APP / "OnOff.java"))
check("the looks page's previews draw the three surfaces in the material (one when it is on)",
      "GlassStyle.Material m = GlassStyle.material(l, prefs.look(), i < 3 ? GlassStyle.QUICK : i == 3 ? GlassStyle.CARDS : GlassStyle.BAR, sync);" in app and "paint.setColor(GlassStyle.over(m.fill, l.base));" in app)
en = read(ROOT / "i18n/en/saver.tsv")
names = re.search(r'SURFACE_NAMES = \{([^}]*)\}', gs).group(1) + re.search(r'FILL_NAMES = \{([^}]*)\}', gs).group(1)
check("every word of it is in the English table and marked in Words; no «·» or «•» in them", all(f"\n{w}\t" in en and f'S.n("{w}")' in words for w in re.findall(r'"([^"]+)"', names) + ["توحيد المظهر", "الشفافية"])
      and not re.search(r"[·•]", app[app.index('case "sync": {'):app.index('case "cards": {')]))
settings, feats = read(ROOT / "docs/SETTINGS.md"), read(ROOT / "docs/FEATURES.md")
check("docs: SETTINGS.md and FEATURES.md have the row with its commands", "| توحيد المظهر | `optimize/saver.sh looksync on`" in settings and "`optimize/saver.sh surface tray 100 graphite`" in settings
      and "**توحيد المظهر:**" in feats and "`optimize/saver.sh looksync off`" in feats)
shots = read(ROOT / ".github/screens/update_shots.sh")
check("the update shots: the home in the glass, the graphite and the light look with the switch on (and once off)",
      'for spec in "glass dark on" "graphite dark on" "glass light on" "glass dark off"; do' in shots and 'shoot "after-home-sync-$1-$2-$3" sa.urtv.home/.DebugSnapShot sa.urtv.home/.MainActivity live' in shots
      and 'saver --es look "$1" --es lang ar --es appearance "$2" --es looksync "$3"' in shots)
page = read(ROOT / "src/template.html")
check("the page: html.hsync maps the cards' colours to the look's ink, html.hlt is a light look's; validated like the fill, all three or none",
      "html.hsync .week{--sand:var(--hink);--dim:var(--hsub);--gold:var(--hacc);" in page and "if (ok(ink) && ok(sub) && ok(acc)) {" in page and "r.classList.add('hsync');" in page and "html.hlt .week li{color:var(--hink)}" in page)

print(f"\n{failed} FAILED" if failed else "\nall sync look checks passed")
sys.exit(1 if failed else 0)
