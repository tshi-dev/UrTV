#!/usr/bin/env python3
"""The glass look («د · زجاج», every UrTV screen): one small blurred backdrop of the home's photo made once per photo, flat fills over it, no
live blur, one bitmap per process; the two apps' copies of the shared files the same; the colours of the XML dialogs and of the phone page
the same as GlassStyle's; and, for real (tests/GlassRun.java), every text of both looks at 4.5:1 or more over every photo we ship, over
plain colours (an owner's photo), and on the solid page.   Run: python3 tests/glass_test.py   (the contrast part needs javac)"""
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
java = {p: read(p) for p in list((ROOT / "android").rglob("*.java"))}

# ---- one look, two apps
for name in ("GlassStyle.java", "GlassPaint.java"):
    a, b = read(HOME / name), read(APP / name)
    check(f"{name} is the same in both apps (only the package line differs)",
          a.replace("package sa.urtv.home;", "", 1) == b.replace("package sa.urtv.screensaver;", "", 1))
style, paint, source, sourceApp = read(HOME / "GlassStyle.java"), read(HOME / "GlassPaint.java"), read(HOME / "GlassSource.java"), read(APP / "GlassSource.java")
check("GlassStyle has no Android in it (it runs on a plain JVM here)", "import android" not in style)

# ---- no live blur, one small bitmap
live = [str(p.relative_to(ROOT)) for p, s in java.items() if re.search(r"\bRenderEffect\b|setRenderEffect|ScriptIntrinsicBlur|RenderScript|BlurMaskFilter", s)]
check("no screen uses a live blur (RenderEffect, RenderScript, BlurMaskFilter)", not live, live)
pages = [ROOT / "android/launcher/src/main/assets/phone/index.html"]
css = [str(p.relative_to(ROOT)) for p in pages if re.search(r"backdrop-filter\s*:|filter\s*:\s*blur", read(p))]
check("the phone page has no live blur either (backdrop-filter, filter: blur)", not css, css)
blurs = [str(p.relative_to(ROOT)) for p, s in java.items() if "GlassStyle.blur(" in s and p.name != "GlassSource.java"]
check("the backdrop is blurred in one place only: GlassSource.update, once per photo", not blurs and "GlassStyle.blur(" in source
      and 'if (key.equals(p.getString("glass_key", null)) && f.isFile()) return;' in source, blurs)
check("…from the photo scaled down by Android first, at 1/8 of the screen (240x135)", "Bitmap.createScaledBitmap(photo, GlassStyle.W * 2, GlassStyle.H * 2, true)" in source
      and "static final int W = 240, H = 135;" in style)
check("…on the photo's own thread, only for the photo still shown", "if (gen == wallGen) GlassSource.update(this, bm, key);" in read(HOME / "MainActivity.java"))
decodes = [str(p.relative_to(ROOT)) for p, s in java.items() if re.search(r"GlassSource\.file\(|backdrop\.png|GlassSource\.BACKDROP|\bBACKDROP\b", s)
           and p.name not in ("GlassSource.java", "GlassProvider.java")]
check("one bitmap per process: only GlassSource decodes the backdrop, and keeps it (one static field, read again when the file changes)",
      not decodes and "private static Bitmap cached;" in source and "private static Bitmap cached;" in sourceApp and "if (stamp != cachedStamp)" in source, decodes)
check("the screens draw that same bitmap, never a copy", "new BitmapDrawable(c.getResources(), backdrop);   // the same bitmap, not a copy" in paint
      and "createBitmap" not in paint and "copy(" not in paint)
users = sorted(p.name for p, s in java.items() if "GlassSource.page(" in s or "GlassSource.page(" in s.replace("sa.urtv.home.GlassSource.page(", "GlassSource.page("))
want = ["DockActivity.java", "FamilyDnsActivity.java", "InputNamesActivity.java", "ParentalActivity.java", "PhoneActivity.java", "SettingsActivity.java",
        "SetupActivity.java", "TasksActivity.java", "UpdateActivity.java", "WatchNextActivity.java"]
check("every screen of ours draws its page with GlassSource.page", all(w in users for w in want), sorted(set(want) - set(users)))
old = [str(p.relative_to(ROOT)) for p, s in java.items() if "0xFF0A2016, 0xFF04120C, 0xFF020806" in s]
check("no screen keeps the old dark green page", not old, old)
cc = read(HOME / "ControlCenter.java")
check("«مركز التحكم»: the glass look of now, opaque over the look's solid colour (it sits over the home's photo), glass tiles, the solid focus fill",
      "GlassStyle.Look l = GlassSource.look(act);" in cc and "PANEL = GlassStyle.over(l.dialog, l.base);" in cc and "fill.setColor(f ? FOCUSED : TILE);" in cc
      and "name.setTextColor(f ? ON : TEXT);" in cc and "0xF0242428" not in cc)
TIL = HOME / "tilawah"
for name in ("MainActivity.java", "SurahActivity.java", "PlayerActivity.java", "AdhanActivity.java"):
    t = read(TIL / name)
    check(f"«تلاوة وأذان» {name}: the glass look of now (its light or dark resources) and the glass page",
          "super.attachBaseContext(Ui.glassContext(base));" in t and "Ui.glassPage(this, R.id.bg);" in t)
ui = read(TIL / "Ui.java")
check("«تلاوة وأذان»: light or dark from «المظهر» (GlassSource.light), not the TV's own night mode; the old photo layer is let go",
      "sa.urtv.home.GlassSource.light(base)" in ui and "UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES" in ui and "setImageDrawable(null)" in ui)
lres = ROOT / "android/launcher/src/main/res"
greens = [p.name for p in (lres / "drawable").glob("*.xml") if re.search(r"#(2E3F7A5C|1A123A28|4D9C9A5E|33467A52|733F7A5C|661A4630|8C2F6B4C|8C0F2D1F|26DCB86F|40DCB86F|00040E09|E6040E09)", read(p), re.I)]
check("«تلاوة وأذان»: no drawable keeps the old green glass", not greens, greens)

# ---- the settings app reads it through a signature permission
lm, am = read(ROOT / "android/launcher/src/main/AndroidManifest.xml"), read(ROOT / "android/app/src/main/AndroidManifest.xml")
check("the home app declares READ_GLASS as a signature permission", re.search(r'android:name="sa\.urtv\.home\.READ_GLASS"\s*android:protectionLevel="signature"', lm) is not None)
check("GlassProvider is behind it", re.search(r'android:name="\.GlassProvider"\s*android:authorities="sa\.urtv\.home\.glass"\s*android:exported="true"\s*android:permission="sa\.urtv\.home\.READ_GLASS"', lm) is not None)
check("the settings app asks for it", '<uses-permission android:name="sa.urtv.home.READ_GLASS" />' in am)
decl = lambda m: re.findall(r'<permission\s+android:name="sa\.urtv\.home\.READ_GLASS"\s+android:protectionLevel="(\w+)"\s*/>', m)
check("…and declares it too, the same (it is installed first: a permission only an app installed later declares is never granted to it)",
      decl(am) == ["signature"] and decl(lm) == ["signature"], (decl(am), decl(lm)))
prov = read(HOME / "GlassProvider.java")
check("the provider only reads: opening for anything but \"r\" is refused, and insert, update and delete throw",
      '!"r".equals(mode)' in prov and "MODE_READ_ONLY" in prov and prov.count("throw new UnsupportedOperationException()") == 3)
check("a missing or refused provider (an older home app) is the solid look, not a crash", "catch (RuntimeException e) {   // no home app" in sourceApp)
check("the phone page gets the backdrop only with a session, and the veils in /api/state",
      'case "/api/backdrop": return backdrop();' in read(HOME / "phone/PhoneApi.java") and '"/api/backdrop"' in read(HOME / "phone/PhoneApi.java").split("static List<String> sessionPaths()")[1]
      and "GlassSource.phoneState(app)" in read(HOME / "phone/PhoneActions.java"))

# ---- the same colours everywhere
look = {}
for name in ("DARK", "LIGHT"):
    m = re.search(rf"static final Look {name} = new Look\((.*?)\);", style, re.S)
    v = [x.strip() for x in m.group(1).replace("\n", " ").split(",")]
    keys = ["light", "base", "scrim", "scrimMin", "text", "sub", "accent", "warn", "panel", "line", "dialog", "focus", "onFocus", "onFocusSub", "hint"]
    look[name] = dict(zip(keys, v))
def hexrgb(v): return "#" + v[-6:].upper()
def hexargb(v): return "#" + v[2:].upper()
res = ROOT / "android/app/src/main/res"
styles = read(res / "values/styles.xml")
dark_style = styles.split('name="UrDialog"')[1].split("</style>")[0]
light_style = styles.split('name="UrDialogLight"')[1].split("</style>")[0]
for nm, st, l in (("dark", dark_style, look["DARK"]), ("light", light_style, look["LIGHT"])):
    low = st.lower()
    check(f"the {nm} dialogs' words are GlassStyle's", f'textcolorprimary">{hexrgb(l["text"]).lower()}<' in low and f'textcolorsecondary">{hexrgb(l["sub"]).lower()}<' in low, st)
check("the dialogs' glass is GlassStyle's (dark and light)", f'#{look["DARK"]["dialog"][2:].upper()}' in read(res / "drawable/glass_dialog.xml").upper()
      and f'#{look["LIGHT"]["dialog"][2:].upper()}' in read(res / "drawable/glass_dialog_light.xml").upper())
check("the dialogs' focused row is GlassStyle's focus fill and ink (dark and light)", f'#{look["DARK"]["focus"][2:].upper()}' in read(res / "drawable/glass_focus.xml").upper()
      and f'#{look["LIGHT"]["focus"][2:].upper()}' in read(res / "drawable/glass_focus_light.xml").upper()
      and hexrgb(look["DARK"]["onFocus"]) in read(res / "color/glass_item.xml").upper() and hexrgb(look["LIGHT"]["onFocus"]) in read(res / "color/glass_item_light.xml").upper())
check("the light dialogs keep nothing grey behind them (the frosted glass is checked over the page as it is)", 'backgroundDimAmount">0<' in light_style)
def colours(path):
    return {m.group(1): m.group(2).upper() for m in re.finditer(r'<color name="(\w+)">(#[0-9A-Fa-f]+)</color>', read(path))}
for nm, path, l in (("light", lres / "values/colors.xml", look["LIGHT"]), ("dark", lres / "values-night/colors.xml", look["DARK"])):
    c = colours(path)
    want_c = {"bg": hexrgb(l["base"]), "glass_text": hexrgb(l["text"]), "glass_sub": hexrgb(l["sub"]), "glass_accent": hexrgb(l["accent"]), "glass_warn": hexrgb(l["warn"]),
              "glass_panel": hexargb(l["panel"]), "glass_line": hexargb(l["line"]), "glass_dialog": hexargb(l["dialog"]), "glass_focus": hexargb(l["focus"]),
              "on_focus": hexrgb(l["onFocus"]), "on_focus_sub": hexrgb(l["onFocusSub"])}
    check(f"«تلاوة وأذان» {nm}: its colours are GlassStyle's", all(c.get(k) == v for k, v in want_c.items()), {k: (c.get(k), v) for k, v in want_c.items() if c.get(k) != v})
check("«تلاوة وأذان»: every word is a state list, the focus ink on a focused or selected row (sand, dim, gold, red)",
      all('android:state_selected="true" android:color="@color/on_focus' in read(lres / f"color/{n}.xml") for n in ("sand", "dim", "gold", "red")))
phone = read(ROOT / "android/launcher/src/main/assets/phone/index.html")
dark_t = re.search(r":root \{([^}]*)\}", phone).group(1)
light_t = re.search(r':root\[data-theme="light"\] \{([^}]*)\}', phone).group(1)
def tok(block, name): return re.search(rf"--{name}:([^;]+);", block).group(1).strip().upper()
for nm, block, l in (("dark", dark_t, look["DARK"]), ("light", light_t, look["LIGHT"])):
    check(f"the phone page's {nm} words are GlassStyle's (text, sub, warn, ink)", (tok(block, "text"), tok(block, "dim"), tok(block, "red"), tok(block, "ink"), tok(block, "bg"))
          == (hexrgb(l["text"]), hexrgb(l["sub"]), hexrgb(l["warn"]), hexrgb(l["onFocus"]), hexrgb(l["base"])), block)
check("the phone page's veil follows the TV's per photo (--vd, --vl) and starts at the strongest", "--va:var(--vd,.95)" in dark_t and "--va:var(--vl,.95)" in light_t)

# ---- for real: contrast over every photo we ship, plain colours, the solid page
if not shutil.which("javac"):
    print("SKIP glass contrast: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "GlassStyle.java"), str(ROOT / "tests/GlassRun.java")], check=True)
    out = subprocess.run(["java", "-Djava.awt.headless=true", "-cp", tmp, "sa.urtv.home.GlassRun", str(ROOT / "assets/backgrounds"), str(ROOT / "photos")],
                         check=True, capture_output=True).stdout.decode("utf-8")
lines = [l for l in out.split("\n") if l]
photos = [l for l in lines if l.startswith("photo ")]
shipped = len(list((ROOT / "assets/backgrounds").glob("*.jpg"))) + len([p for p in (ROOT / "photos").iterdir() if p.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp")])
check("every photo we ship was read, in both looks", len(photos) == 2 * shipped and not any(l.startswith("skip") for l in lines), (len(photos), shipped))
def worst(l): return float(l.rsplit("worst=", 1)[1])
bad = [l for l in lines if "worst=" in l and worst(l) < 4.5]
check("every text of both looks reads at 4.5:1 or more over every pixel of every photo we ship (the veil it gets)", not [l for l in bad if l.startswith("photo")], bad[:8])
check("…and over plain white, black, grey, red, blue and yellow: an owner's own photo reads too", not [l for l in bad if l.startswith("plain")], bad[:8])
check("…and on the solid page, before there is a backdrop", not [l for l in bad if l.startswith("solid")] and len([l for l in lines if l.startswith("solid")]) == 2, bad)
alphas = [float(re.search(r"alpha=([\d.]+)", l).group(1)) for l in photos]
check("the veil is never past 0.95 (some of the photo always shows)", max(alphas) <= 0.95, max(alphas))
print("veil per photo: dark", min(a for l, a in zip(photos, alphas) if " dark " in l), "to", max(a for l, a in zip(photos, alphas) if " dark " in l),
      "· light", min(a for l, a in zip(photos, alphas) if " light " in l), "to", max(a for l, a in zip(photos, alphas) if " light " in l))
sys.exit(1 if failed else 0)
