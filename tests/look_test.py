#!/usr/bin/env python3
"""«المظهر» of the home screen (the idea from talkies, MIT): dark (the default, the home as it was), light, or auto, light from sunrise
until Maghrib. Look (run for real, tests/LookRun.java) decides and holds the two palettes; the setting goes from the screensaver's
settings, saver.sh and its receiver through the ConfigProvider to the launcher's minute tick, read from the source. The text of each
look is measured against its scrim over a grey, a dark and a bright photo (WCAG contrast). Needs javac.   Run: python3 tests/look_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
main, look, snap = read(HOME / "MainActivity.java"), read(HOME / "Look.java"), read(HOME / "ConfigSnapshot.java")
prefs, provider, recv, settings = read(APP / "Prefs.java"), read(APP / "ConfigProvider.java"), read(APP / "SaverReceiver.java"), read(APP / "SettingsActivity.java")
saver = read(ROOT / "optimize/saver.sh")

# ---- the setting, end to end
check("Prefs: dark, light or auto; dark by default and for anything unknown", 'static final String[] APPEARANCES = {"dark", "light", "auto"};' in prefs
      and 'sp.getString("appearance", "dark")' in prefs and ': "dark"; }' in prefs)
check("the provider gives it to the launcher as a column of its own", '"update_channel", "appearance"' in provider and "p.updateChannel(), p.appearance()" in provider)
check("the receiver takes --es appearance dark|light|auto, refuses anything else, and show says it",
      'i.getStringExtra("appearance")' in recv and 'return "error: appearance is one of "' in recv and "p.setAppearance(appearance);" in recv
      and '"\\nappearance: " + p.appearance()' in recv)
check("saver.sh appearance, with its help line", 'appearance) set1 appearance "${2:?usage: optimize/saver.sh appearance dark|light|auto}" ;;' in saver
      and "#   optimize/saver.sh appearance auto" in saver)
home_at = settings.index('header("الشاشة الرئيسية (تلفزيونك)");')
check("«المظهر» is the first row of «الشاشة الرئيسية»: «الشكل» and «الإضاءة» (dark, light, auto), each chosen in its own list",
      'row("المظهر", Prefs.lookLabel(prefs.look()) + " · " + Prefs.appearanceLabel(prefs.appearance())' in settings
      and home_at < settings.index('row("المظهر"') < settings.index("row(HOME_CARD_NAMES[k]") and "prefs.setAppearance(values[pos])" in settings
      and "prefs.setLook(values[pos])" in settings)
check("the launcher reads the column; an older screensaver says nothing: dark", 'cur.getColumnIndex("appearance")' in snap and "this.appearance = Look.mode(appearance);" in snap)
tick = main[main.index("private void updateClock()"):].split("\n    }\n")[0]
check("the home asks for the look at each minute tick (and on coming back), with the sun when it is automatic",
      "applyLook(Look.light(cfg.appearance(), dayBySun(cfg), h * 60 + now.get(Calendar.MINUTE)));" in tick)
sun = main[main.index("private Boolean dayBySun(ConfigSnapshot cfg)"):].split("\n    }\n")[0]
check("the sun is up while «الغروب» is next (the prayer times' DayParts, as the greeting's line); unknown: null, the clock's 6:00–18:30",
      '"الغروب".equals(sa.urtv.home.tilawah.DayParts.now(this, cfg.fragment).sunLabel)' in sun and "return null;" in sun
      and "if (!Look.AUTO.equals(cfg.appearance())) return null;" in sun)
apply = main[main.index("private void applyLook(boolean light)"):].split("\n    }\n")[0]
check("the look is applied only when it changes; the home is built in the dark look and starts there (dark is exactly as it was)",
      "if (light == lightNow) return;" in apply and "private boolean lightNow;" in main)
for part in ("clock.setTextColor(p.text)", "title.setTextColor(p.ink)", "t.setTextColor(p.soft)", "t.setTextColor(p.gold)", "t.setShadowLayer(dp(3), 0, dp(1), p.shadow)",
             "for (TextView t : gridLabels) t.setTextColor(p.ink);", "scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, p.scrim));",
             "gridShade.setBackgroundColor(p.shade);", "dockBand.setColor(p.band);"):
    check(f"the light and dark looks recolour: {part}", part in apply)
check("a grid name made while the light look is on is made in it, and the list is emptied when the grid is built again",
      "if (lightNow) label.setTextColor(Look.LIGHT_LOOK.ink);" in main and "gridCards.clear();\n        gridLabels.clear();" in main)
ints = lambda s: [int(x, 16) for x in re.findall(r"0x([0-9A-F]{8})", s)]
scrim_src = re.search(r"scrim\.setBackground\(new GradientDrawable\(GradientDrawable\.Orientation\.TOP_BOTTOM,\s*new int\[\]\{([^}]*)\}\)\);", main).group(1)
dark = re.search(r"DARK_LOOK = new Palette\(([^;]*)\);", look, re.S).group(1)
check("the dark look's colours are the home's own (the scrim, the clock, the date, the gold, the shadow, the shade, the dock band)",
      ints(scrim_src) == ints(dark)[6:11] and ints(dark)[0] == 0xFFFFFDF6 and ints(dark)[2] == 0xFFC6BBA4 and "static final int GOLD = 0xFFD6AF6B;" in main
      and ints(dark)[3] == 0xFFD6AF6B and "0x80000000" in main and "gridShade.setBackgroundColor(0xB3000000);" in main
      and "band.setColor(0x1FFFFFFF);" in main and "band.setStroke(dp(1), 0x14FFFFFF);" in main, (ints(scrim_src), ints(dark)))
notices = read(ROOT / "THIRD_PARTY_NOTICES.md")
check("talkies is credited (THIRD_PARTY_NOTICES.md and Look)", "Copyright (c) 2026 TechPP" in notices and "talkies" in look and "THIRD_PARTY_NOTICES.md" in look)
check("documented: SETTINGS.md and FEATURES.md", "optimize/saver.sh appearance auto" in read(ROOT / "docs/SETTINGS.md") and "**المظهر:**" in read(ROOT / "docs/FEATURES.md"))

# ---- Look, for real
if not shutil.which("javac"):
    print("SKIP look: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "Look.java"), str(ROOT / "tests/LookRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.LookRun"], check=True, capture_output=True).stdout.decode().split("\n")
modes = {l.split(" = ")[0][5:]: l.split(" = ")[1] for l in out if l.startswith("mode ")}
check("a setting is auto, light or dark; anything else (an older screensaver, a typo) is dark",
      modes == {"auto": "auto", "light": "light", "dark": "dark", "": "dark", "night": "dark", "null": "dark"}, modes)
lit = {tuple(l[6:].split(" = ")[0].split(" ")): l.split(" = ")[1] == "1" for l in out if l.startswith("light ")}
check("light is always light, dark always dark, whatever the sun and the hour",
      all(v for (m, _, _), v in lit.items() if m == "light") and not any(v for (m, _, _), v in lit.items() if m == "dark"))
check("auto follows the sun when it is known (sunrise to Maghrib), whatever the clock says",
      all(v == (s == "true") for (m, s, _), v in lit.items() if m == "auto" and s != "null"))
fb = {int(t): v for (m, s, t), v in lit.items() if m == "auto" and s == "null"}
check("auto with the sun not known yet: light from 6:00 until 18:30 on the TV's clock", fb == {0: False, 359: False, 360: True, 720: True, 1109: True, 1110: False, 1439: False}, fb)
con = {tuple(l[9:].split(" = ")[0].split(" ")): float(l.split(" = ")[1]) for l in out if l.startswith("contrast ")}
for photo in ("grey", "dark", "bright"):
    for part, need in (("clock", 4.5), ("date", 4.5), ("title", 4.5), ("gridname", 4.5), ("greeting", 3.0)):
        c = con[("light", part, photo)]
        check(f"light look over a {photo} photo: the {part} reads (contrast {c:.1f}, at least {need})", c >= need, c)
for photo in ("grey", "bright"):   # the rows' names and the soft lines on the thinnest middle of the light veil (the review's case)
    for part, need in (("rowname", 3.0), ("softthin", 4.5)):
        c = con[("light", part, photo)]
        check(f"light look, the thin middle of the veil over a {photo} photo: the {part} reads (contrast {c:.1f}, at least {need})", c >= need, c)
for photo in ("grey", "dark", "bright"):
    c = con[("light", "rownamegrid", photo)]
    check(f"light look, a row's name over the grid's shade (a row has the focus) on a {photo} photo: contrast {c:.1f}, at least 4.5", c >= 4.5, c)
for photo in ("grey", "dark"):   # the dark look as it always was, over the photos it was made for
    for part, need in (("clock", 4.5), ("title", 4.5), ("gridname", 4.5), ("greeting", 3.0)):
        c = con[("dark", part, photo)]
        check(f"dark look over a {photo} photo: the {part} reads as before (contrast {c:.1f})", c >= need, c)
# ---- «المظهر ← الشكل»: glass (the default), graphite, navy, clean, for all our screens
main_src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
gs = (ROOT / "android/launcher/src/main/java/sa/urtv/home/GlassStyle.java").read_text(encoding="utf-8")
gsrc = (ROOT / "android/launcher/src/main/java/sa/urtv/home/GlassSource.java").read_text(encoding="utf-8")
gsrc_app = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/GlassSource.java").read_text(encoding="utf-8")
check("the four looks, glass first: an unknown or missing value is glass", 'LOOKS = {"glass", "graphite", "navy", "clean"};' in gs
      and "for (String k : LOOKS) if (k.equals(raw)) return k;\n        return LOOKS[0];" in gs)
check("«نظيف» has no dark of its own: its dark is graphite's (the mockup's note)", 'case "clean": return light ? CLEAN_LIGHT : GRAPHITE_DARK;' in gs)
check("the screensaver keeps it (glass by default), and the provider gives it as a column of its own after the others",
      'String look() { return GlassStyle.look(sp.getString("look", GlassStyle.LOOKS[0])); }' in prefs and '"watch_next", "look"});' in provider and "p.look()});" in provider)
check("the receiver takes --es look glass|graphite|navy|clean, refuses anything else, and show says it",
      'i.getStringExtra("look")' in recv and 'return "error: look is one of "' in recv and "p.setLook(look);" in recv and '"\\nlook: " + p.look()' in recv)
check("saver.sh look, with its help line", 'look) set1 look "${2:?usage: optimize/saver.sh look glass|graphite|navy|clean}" ;;' in saver and "#   optimize/saver.sh look navy" in saver)
check("the launcher reads the column; an older screensaver says nothing: glass", 'cur.getColumnIndex("look")' in snap and "this.look = GlassStyle.look(look);" in snap)
check("every screen of ours takes the look of now: the launcher's and the settings app's GlassSource, the home's own pieces",
      "return GlassStyle.of(lookName(c), light(c));" in gsrc and "return GlassStyle.of(new Prefs(c).look(), light(c));" in gsrc_app
      and "return GlassStyle.of(lookNow, lightNow);" in main_src)
check("any look's veil comes from the backdrop's two bounds (not stored once per look); unknown bounds: white and black, the strongest",
      "GlassStyle.scrimAlpha(b[0], b[1], l)" in gsrc and 'prefs(c).getInt("glass_hi", 0xFFFFFFFF), prefs(c).getInt("glass_lo", 0xFF000000)' in gsrc)
sys.exit(1 if failed else 0)
