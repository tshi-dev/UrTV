#!/usr/bin/env python3
"""English phase 1, the screensaver app (docs/ENGLISH.md): its settings in «اللغة» English, and Arabic exactly as it was.

1. Arabic is untouched: Words.of gives the text itself unless «اللغة» is English, and every place that draws words (conv, the dialogs,
   the city, the teams, the page's city and digits) keeps its Arabic road as it was; the screen's direction comes from Dir.
2. Every Arabic text these screens show has its English: each literal of the settings' sources, run through WordMatch (on a JVM, with
   the real table and Words' own patterns), comes out with no Arabic letter; the sentences built from parts read right in English.
3. Words and i18n/en/saver.tsv are in step (each entry marked once for the build test, each pattern a pattern).
4. Places: every country of the list has its English name, every built-in city and Saudi region too; a country is never named as a city.
5. «اللغة»: its button in «المزيد», the two choices in their own words, the screen made again in the new language on the same button.
6. English is wider: the buttons' names and the rows' names (one line each) fit their place on a 1080p TV (960 dp wide), measured with a
   Latin font of the machine (Thmanyah is CI-only).
Run: python3 tests/i18n_saver_test.py   (the JVM part needs javac; the widths need Pillow)
"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sys.path.insert(0, str(ROOT / "i18n"))
import tr  # noqa: E402

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def src(name):
    return (APP / name).read_text(encoding="utf-8")


def fn(text, sig):
    return text[text.index(sig):].split("\n    }\n")[0]


AR = re.compile("[؀-ۿ]")
st, hub, prefs, words, teams = src("SettingsActivity.java"), src("SettingsHub.java"), src("Prefs.java"), src("Words.java"), src("Teams.java")

# ---- 1. Arabic is untouched
check("Words.of: the text itself unless «اللغة» is English (the table is never read for Arabic)",
      "static String of(String s) {\n        if (!S.en() || s == null) return s;" in words
      and "static CharSequence[] of(CharSequence[] items) {\n        if (!S.en() || items == null) return items;" in words)
check("Words.city and Words.display: the saved name and Cities.display as before unless English",
      "if (!S.en()) return saved[0];" in words and "if (!S.en()) return Cities.display(row);" in words)
check("conv: «اللغة» first, then the digits as before",
      "private static String conv(String s) { s = Words.of(s); return DigitsText.convert(s, latinDigits); }" in st)
dlg = src("WordsBuilder.java")
check("the dialogs: title, message, buttons and lists through Words.of (Dlg), but a list of choices in their own words (own())",
      all(f"@Override public AlertDialog.Builder {m}(" in dlg for m in ("setTitle", "setMessage", "setPositiveButton", "setNegativeButton",
                                                                         "setNeutralButton", "setItems", "setSingleChoiceItems", "setMultiChoiceItems"))
      and "return own || t == null ? t : Words.of(t.toString());" in dlg and "return own ? t : Words.of(t);" in dlg
      and "private static final class Dlg extends WordsBuilder {" in st)
attach = fn(st, "protected void attachBaseContext(android.content.Context base)")
check("attachBaseContext: English gets S.wrap's context; Arabic the very Arabic locale and direction it always had",
      "if (S.en()) { super.attachBaseContext(en); return; }" in attach and 'c.setLocale(new java.util.Locale("ar"));' in attach
      and 'c.setLayoutDirection(new java.util.Locale("ar"));' in attach and "super.attachBaseContext(base.createConfigurationContext(c));" in attach)
check("the settings' direction comes from Dir: no hard-coded right-to-left left", "LAYOUT_DIRECTION_RTL" not in st and "TEXT_DIRECTION_RTL" not in st
      and st.count("setLayoutDirection(Dir.layout());") >= 10 and st.count("setTextDirection(Dir.text())") >= 4)
hubfn = fn(st, "    private void hub(String want, int wasIndex) {")
check("the buttons' D-pad mirrors with the direction: the two sides of SettingsHub.next swap for English, Arabic as before (2 left, 3 right)",
      "final int left = Dir.rtl() ? 2 : 3, right = Dir.rtl() ? 3 : 2;" in hubfn
      and "t.setNextFocusLeftId(tiles.get(SettingsHub.next(i, tiles.size(), left, wide)).getId());" in hubfn
      and "t.setNextFocusRightId(tiles.get(SettingsHub.next(i, tiles.size(), right, wide)).getId());" in hubfn)
check("a prayer's minutes: the key forward in reading order adds one (left in Arabic, right in English)",
      "int d = code == Dir.forwardKey() ? 1 : code == Dir.backKey() ? -1 : 0;" in fn(st, "private void adjRow(final int k)"))
check("Prefs: the digits default to latin only for English never chosen; hindi as always for Arabic",
      'String digits() { return sp.getString("digits", Tr.EN.equals(lang()) ? "latin" : "hindi"); }' in prefs)
city = fn(prefs, "private String pageCity(String[] city)")
check("Prefs.fragment: the city as saved for Arabic; with English the built-in city's English name (Places), a place of one's own as saved",
      '"city=" + Uri.encode(pageCity(city)) + "&lat="' in prefs and "if (!Tr.EN.equals(lang())) return city[0];" in city
      and "return en != null ? en : city[0];" in city)
check("teams: the Arabic name as before; with English the English name of our list (data, not the table)",
      "if (t.ar.isEmpty() || (S.en() && !t.en.isEmpty())) return t.en;" in teams
      and "if (!S.en() || p.length < 2) return p.length < 2 ? saved : p[1];" in fn(st, "private String teamName(String saved)")
      and "return S.en() ? en : Arabic.translit(en);" in st and "return S.en() ? key : Arabic.translit(key);" in st)
check("the city search asks Open-Meteo in the screen's language (Arabic as before)", '&language=" + (S.en() ? "en" : "ar") + "&name="' in st)

# ---- 3. Words and the table in step
saver = tr.parse((ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8"), "saver.tsv")
table = tr.load()
marked = [ar for _, ar in tr.java_literals(words, "Words.java")]
shown = words[words.index("static final String[] SHOWN"):words.index("static final String[] PATTERNS")]
pats_block = words[words.index("static final String[] PATTERNS"):words.index("private static volatile WordMatch match;")]
shown_l = [ar for _, ar in tr.java_literals(shown)]
pats_l = [ar for _, ar in tr.java_literals(pats_block)]
xml = re.findall(r"<string name=\"[^\"]+\"[^>]*>(.*?)</string>", (ROOT / "android/app/src/main/res/values/strings.xml").read_text(encoding="utf-8"))
rows = [ar for _, ar, _ in saver]
check(f"Words marks every entry of saver.tsv once ({len(marked)}), but the app's name (strings.xml)",
      sorted(marked) == sorted(r for r in rows if r not in xml) and len(set(marked)) == len(marked),
      (set(rows) - set(marked) - set(xml), set(marked) - set(rows)))
check(f"the patterns ({len(pats_l)}) are the entries with a value (%s), and only those", all("%s" in p for p in pats_l) and not any("%" in w for w in shown_l))

# ---- 2. every text in English, on a JVM
SOURCES = ["SettingsActivity.java", "SettingsHub.java", "Prefs.java", "OnOff.java", "PrayerMethod.java", "PhotoInterval.java", "ClockSetting.java",
           "QuoteEvery.java", "GlassStyle.java", "Location.java", "Updater.java", "CountdownText.java", "PrayerAdj.java"]
# what is not a word of a screen: a club or a city (data, which has its own English), the bilingual title of «اللغة», a pattern of the code
NOT_WORDS = {"الهلال", "المنتخب السعودي", "الرياض", "العربية · English", "اللغة · Language", "Language · اللغة", "[\\s,،]+", "العربية", "منطقة"}


def literals(name):
    out = []
    code = tr.strip_comments(src(name))
    for m in re.finditer(r'"((?:[^"\\\n]|\\.)*)"', code):
        s = tr.java_unescape(m.group(1))
        if AR.search(s) and s not in NOT_WORDS:
            out.append(s)
    return out


every, pieces = [], []
for f in SOURCES:
    for s in literals(f):
        # a piece of a sentence built in the code («الفرق المتابعة (» + n + «)»): its sentences are the cases below
        (pieces if s != s.strip() or s.endswith("(") or s.startswith(")") or s.startswith("، ") else every).append(s)
CASES = [  # what the screen builds, and its English (the digits are turned after, by conv)
    ("الفرق المتابعة (٣/٢٠)", "Followed teams (٣/٢٠)"),
    ("الأسواق (٣/٤)", "Markets (٣/٤)"),
    ("صوري (٢/١٠)", "My photos (٢/١٠)"),
    ("قائمة الأسهم (٤/٨)", "Stock list (٤/٨)"),
    ("تمت متابعة الهلال", "Following الهلال"),
    ("رجوع إلى المزيد", "Back to More"),
    ("زرها ظاهر", "Button: Shown"),
    ("الفرق ٣ · الأسواق مخفية", "Teams: ٣ · Markets: Hidden"),
    ("الفرق ٣ · الأسواق ٥", "Teams: ٣ · Markets: ٥"),
    ("تلقائي · أم القرى", "Auto · Umm al-Qura"),
    ("الدوريات كاملة (حتى ٥، بدون اختيار = بدون)", "Whole leagues (up to ٥, none picked = none)"),
    ("رياضات أخرى (بدون اختيار = بدون)", "Other sports (none picked = none)"),
    ("التصنيفات (بدون اختيار = الكل)", "Genres (none picked = all)"),
    ("الفجر +٢ د", "Fajr +٢ min"),
    ("العشاء −٢٠ د", "Isha −٢٠ min"),
    ("تعذر التحديث (404)", "Update failed (404)"),
    ("الإصدار الحالي: ٤٫٣", "Current version: ٤٫٣"),
    ("المدينة: الرياض. لتغييرها: الإعدادات ثم الموقع", "City: Riyadh. To change it: Settings, then Location"),
    ("منطقة الرياض  (32)", "Riyadh Region (32)"),
    ("5 دقائق", "5 minutes"),
    ("١٥ دقيقة", "١٥ minutes"),
    ("45 ثانية", "45 seconds"),
    ("حتى ٥ دوريات", "Up to ٥ leagues"),
    ("اختر من سوق إلى ٤ أسواق", "Pick 1 to ٤ markets"),
    ("رمز غير صالح: XYZ!", "Invalid code: XYZ!"),
    ("رحلة (٣١ ديسمبر ٢٠٢٦)", "رحلة (٣١ December ٢٠٢٦)"),
    ("تبدأ بعد ٥ دقائق · آية أو حديث اليوم", "Starts after ٥ minutes · Verse or hadith of the day"),
    ("جديد المسلسلات، مواقيت الصلاة اليوم", "New episodes, Today’s prayer times"),
    ("الشكل: زجاجي", "Style: Glass"),
    ("الإضاءة: تلقائي · فاتح من الشروق إلى المغرب، وداكن بعده", "Brightness: Auto · Light from sunrise to Maghrib, dark after"),
    ("المدينة · الرياض", "City · Riyadh"),
    ("زجاجي · داكن", "Glass · Dark"),
    ("Real Madrid  ·  الدوري الإسباني", "Real Madrid  ·  La Liga"),
    ("الخرطوم  ·  ولاية الخرطوم  ·  السودان", "Khartoum  ·  ولاية الخرطوم  ·  Sudan"),
    ("نادي أول", "نادي أول"),   # data with no entry stays as it is
    ("الدولة", "Country"),
    ("الإمارات", "UAE"),          # the table first (the prayer method's name), then the places
    ("السودان", "Sudan"),
    ("الجزائر", "Algeria"),       # a name alone is the country; the city row asks Places.city
    ("الإصدار الحالي: ٤٫٣\nالإصدار المتاح: لا يوجد أحدث", "Current version: ٤٫٣\nAvailable version: None newer"),   # note(): lines under each other
]
CITY_ROW = re.search(r'\{"الجزائر", "([\d.\-]+)", "([\d.\-]+)"', src("Cities.java"))
PLACES = [("country\tPS", "Palestine"), ("country\tSA", "Saudi Arabia"), ("country\tAE", "United Arab Emirates"),
          ("city\tالرياض\t24.6877\t46.7219", "Riyadh"), ("city\tالجزائر\t%s\t%s" % CITY_ROW.groups(), "Algiers"),
          ("city\tمكان من عندي\t1.0\t2.0", "null"), ("name\tمنطقة الرياض", "Riyadh Region"), ("name\tالمنطقة الشرقية", "Eastern Province"),
          ("name\tتونس", "Tunisia"), ("cities", "")]
places_src = src("Places.java")
check("Places names no country we never list", not re.search(r'"(IL|EH)",|Israel|Western Sahara', places_src))
codes = set(re.findall(r'new Region\("[^"]+", "([A-Z]{2})"', src("Cities.java"))) | set(re.findall(r'\{"([A-Z]{2})", "[^"]+", "[A-Za-z_/]+", "\d+"', src("Cities.java")))

if not shutil.which("javac"):
    print("SKIP i18n saver JVM part: no javac here")
else:
    with tempfile.TemporaryDirectory() as d:
        d = Path(d)
        inputs = every + [c for c, _ in CASES] + [p for p, _ in PLACES] + [f"country\t{c}" for c in sorted(codes)] + ["regions"]
        (d / "in.txt").write_text("".join(s.replace("\n", "\\n") + "\n" for s in inputs), encoding="utf-8")
        (d / "patterns.txt").write_text("".join(p + "\n" for p in pats_l), encoding="utf-8")
        javac = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(d)] + [str(APP / f) for f in ("Tr.java", "WordMatch.java", "Places.java", "Cities.java", "DigitsText.java")]
                               + [str(ROOT / "tests/WordMatchRun.java")], capture_output=True)
        check("WordMatch, Places and Cities compile as plain Java", javac.returncode == 0, javac.stderr.decode()[-800:])
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", str(d), "sa.urtv.screensaver.WordMatchRun", str(ROOT / "i18n/en"), str(d / "patterns.txt"),
                              str(d / "in.txt"), str(d / "out.txt")], capture_output=True)
        check("the JVM run", run.returncode == 0, run.stderr.decode()[-800:])
        got = [g.replace("\\n", "\n") for g in (d / "out.txt").read_text(encoding="utf-8").split("\n")] if run.returncode == 0 else [""] * (len(inputs) + 1)
    n = len(every)
    left = [f"{s} -> {g}" for s, g in zip(every, got[:n]) if AR.search(g)]
    check(f"every Arabic text of the settings' screens comes out in English ({n} texts of {len(SOURCES)} files)", not left, left[:10])
    bad = [f"{c}: {g!r} (want {w!r})" for (c, w), g in zip(CASES, got[n:n + len(CASES)]) if g != w]
    check(f"the sentences the screens build read right in English ({len(CASES)} cases)", not bad, bad)
    k = n + len(CASES)
    bad = [f"{p}: {g!r} (want {w!r})" for (p, w), g in zip(PLACES, got[k:k + len(PLACES)]) if g != w]
    check("Places: countries by code, built-in cities by their place (Algiers, not the country), a place of one's own has none", not bad, bad)
    k += len(PLACES)
    nocity = next(g for (p, _), g in zip(PLACES, got[k - len(PLACES):k]) if p == "cities")
    check("every built-in city has a non-empty English name (the world list's own, else Places)", nocity == "", nocity[:200])
    nocountry = [c for c, g in zip(sorted(codes), got[k:k + len(codes)]) if g == "null" or AR.search(g)]
    check(f"every country of the list has its English name ({len(codes)})", not nocountry, nocountry)
    regions = got[k + len(codes)].split("|")
    check(f"every region has its English name ({len(regions)})", all(not r.endswith("=null") for r in regions), [r for r in regions if r.endswith("=null")])
    arabic = [r for r in regions if AR.search(r.split("=", 1)[-1])]
    check("…and no English place name has an Arabic letter", not arabic, arabic[:5])

# ---- 5. «اللغة»
check("«اللغة»'s button is off until the home app is in English (phase 3): one constant, and tiles() leaves it out while it is off",
      "static final boolean LANG_TILE = false;" in hub and "&& (LANG_TILE || !LANG.equals(c[0]))) out.add(c[0]);" in hub)
check("«اللغة»: a button of «المزيد» that opens its choices at once (DIRECT), named in ROWS",
      '{LANG, "اللغة", "", MORE, "اللغة"},' in hub and '{"اللغة", LANG}' in hub and 'LANG = "lang"' in hub)
check("«اللغة»: its row, icon and what it says on its button (English beside العربية, to be found)",
      'row("اللغة", langLabel(), R.drawable.ic_language, new Runnable() { public void run() { pickLang(); } });' in st
      and "case SettingsHub.LANG: return R.drawable.ic_language;" in st and "case SettingsHub.LANG: return langLabel();" in st
      and 'return Tr.EN.equals(prefs.lang()) ? "English" : "العربية · English";' in st)
pick = fn(st, "private void pickLang()")
check("«اللغة»: العربية and English in their own words (no translation, no digit change), saved, and the screen started again on that button",
      "new Dlg(this).raw().own()" in pick and '{"العربية", "English"}' in pick and "prefs.setLang(values[i]);" in pick
      and 'startActivity(new Intent(this, SettingsActivity.class).putExtra("screen", screen).putExtra("hubAt", SettingsHub.LANG));' in pick
      and "finish();" in pick)
check("…and only that start comes back on its button; any other start is on the first screen, as it always was",
      'if (getIntent().hasExtra("hubAt")) { screen = getIntent().getStringExtra("screen"); hubAt = getIntent().getStringExtra("hubAt"); }' in st
      and st.count('putExtra("hubAt"') == 1)

# ---- «اللغة» moves with the other settings (optimize/settings_move.py), checked like them
import importlib.util  # noqa: E402
spec = importlib.util.spec_from_file_location("settings_move", ROOT / "optimize/settings_move.py")
sm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sm)
clean, errs, _ = sm.validate({"schema": 1, "lang": "en"})
bad, errs2, _ = sm.validate({"schema": 1, "lang": "fr"})
check("settings_move: lang is allow-listed, ar or en (Tr.java), sent as the receiver's --es lang",
      "lang" in sm.ALLOWED_TOP and sm.LANGS == ("ar", "en") and clean.get("lang") == "en" and not errs and "lang" not in bad
      and any("lang" in e for e in errs2) and "--es lang 'en'" in sm.ops({"lang": "en"}, {})[0] and sm.flat({"lang": "ar"}) == {"lang": "ar"}, (clean, errs, errs2))
check("settings_move: the export takes lang only as the TV reports it (an older screensaver: absent, never guessed)",
      'if g.get("lang") in LANGS:' in (ROOT / "optimize/settings_move.py").read_text(encoding="utf-8")
      and r'"\nlang: " + p.lang()' in src("SaverReceiver.java"))

# ---- 6. English fits
try:
    from PIL import ImageFont
except ImportError:
    ImageFont = None
FONTS = [p for p in ("/usr/share/fonts/opentype/inter/Inter-Medium.otf", "/usr/share/fonts/truetype/noto/NotoSans-Medium.ttf",
                     "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf") if Path(p).is_file()]
if ImageFont is None or not FONTS:
    print("SKIP English widths: no Pillow or no Latin font here")
else:
    def width(text, sp):   # dp: the widest of the machine's Latin fonts, at that size
        return max(ImageFont.truetype(f, 100).getlength(text) * sp / 100 for f in FONTS)
    # a 1080p TV is 960 dp wide (density 2): SettingsActivity's widths at that size
    page = min(960 - 144, 1100)
    side = round(page * 0.36) - 36
    tile_text = (page - 12) / 2 - (16 + 26) - 10 - (36 + 8) - 18
    row_text = page - side - 36 - (24 + 20) - 9 - (28 + 6) - 18 - 14
    cat = dict(re.findall(r'\{(?:ADHAN|PLACE|CLOCK|LISTEN|CARDS|LOOK|CONTROL|PHONE|UPDATE|REPORT|MORE|SAVER|HOME|CONTENT|APPS|SHOWS|FAMILY|LANG), "([^"]+)", "([^"]*)"', hub))
    over = [f"{table[n]} {width(table[n], 24):.0f}>{tile_text:.0f}" for n in cat if width(table[n], 24) > tile_text]
    check(f"every button's name fits on its one line in English (24 sp, {tile_text:.0f} dp)", not over, over)
    names = set(re.findall(r'\brow\("([^"]+)"', st)) | set(re.findall(r'\{"([^"]+)", (?:ADHAN|PLACE|CLOCK|LISTEN|CARDS|LOOK|CONTROL|PHONE|UPDATE|REPORT|MORE|SAVER|HOME|CONTENT|APPS|SHOWS|FAMILY|LANG)\}', hub))
    over = [f"{table[n]} {width(table[n] + ' (20/20)', 21):.0f}>{row_text:.0f}" for n in names if n in table and width(table[n] + " (20/20)", 21) > row_text]
    check(f"every row's name fits on its one line in English, with a count after it (21 sp, {row_text:.0f} dp)", not over, over)

sys.exit(1 if failed else 0)
