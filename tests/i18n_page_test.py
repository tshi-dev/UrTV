#!/usr/bin/env python3
"""«اللغة», phase 2 (docs/ENGLISH.md): the English page dist/tv-en.html. src/build.py writes it beside tv.html, which stays byte for byte what
it was; its words come from i18n/en/page.tsv, what a table cannot say from build.EN_CODE. The app picks the page by «اللغة» (Updater,
Prefs.pageBase, both build.gradle) and the home card by the fragment's lang=en (HomeCardChoice). The page in a browser: i18n_page_test.mjs.
Run: python3 tests/i18n_page_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "i18n"))
import build   # noqa: E402
import tr      # noqa: E402

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

text = lambda p: (ROOT / p).read_text(encoding="utf-8")
unstamp = lambda b: re.sub(rb'(<meta name="urtv-build" content=")\d{14}', rb"\1", b)
AR = re.compile("[؀-ۿ]")

# ---- the build: tv.html as it was, tv-en.html beside it and current
before = {n: (ROOT / "dist" / n).read_bytes() for n in ("tv.html", "tv-en.html")}
r = subprocess.run([sys.executable, "src/build.py"], cwd=ROOT, capture_output=True, text=True)
check("src/build.py runs and writes dist/tv-en.html", r.returncode == 0 and "tv-en.html" in r.stdout, r.stderr[-400:])
after = {n: (ROOT / "dist" / n).read_bytes() for n in before}
check("dist/tv.html is byte for byte what the build wrote before the English page (but the build stamp)", unstamp(after["tv.html"]) == unstamp(before["tv.html"]))
check("dist/tv-en.html is current (run src/build.py after editing src/ or i18n/en/page.tsv)", unstamp(after["tv-en.html"]) == unstamp(before["tv-en.html"]))
en, ar = after["tv-en.html"].decode("utf-8"), after["tv.html"].decode("utf-8")
check("the two pages carry the same build stamp", re.search(r'urtv-build" content="(\d{14})', en).group(1) == re.search(r'urtv-build" content="(\d{14})', ar).group(1))
check("tv-en.html: <html lang=\"en\" dir=\"ltr\">; tv.html still lang=\"ar\" dir=\"rtl\"", '<html lang="en" dir="ltr">' in en[:300] and '<html lang="ar" dir="rtl">' in ar[:300])
check("tv-en.html: under its own 740 KB (tv.html's budget, tests/check.py)", len(after["tv-en.html"]) < 744_000, len(after["tv-en.html"]))
check("tv-en.html: every build token replaced", not re.findall(r"__[A-Z][A-Z_]+__", en), sorted(set(re.findall(r"__[A-Z][A-Z_]+__", en))))
check("tv-en.html: the English title", "<title>Your TV Screen</title>" in en and "document.title = 'Your TV Screen'" in en)

# ---- no Arabic where a person reads the page's own words: the markup, and every literal translate looks at outside the data and the
# code EN_CODE leaves unused (the content, verses, hadith, adhkar and sayings, is data: it stays Arabic)
markup = re.sub(r"<script>.*?</script>|<style>.*?</style>|<!--.*?-->", "", en, flags=re.S)
check("tv-en.html: no Arabic in the markup", not AR.search(markup), AR.findall(markup)[:5])
script = en[en.index("<script>"):]
code = re.sub(r"^const (ADHKAR|VERSES|HADITH|WISDOM|EVENTS|TEAM_AR|TEAM_BY_ID|BGS|BROADCAST)\b.*$", "", script, flags=re.M)
dead = ("const TL_WORDS", "const TL_LETTERS", "function translitAr")
left = [a for _, _, a in tr.arabic_literals(code)]
check("tv-en.html: no Arabic literal left in the page's code (the data and the unused transliteration aside)",
      all(any(code.rfind(d, 0, code.index(a)) > code.rfind("\n\n", 0, code.index(a)) for d in dead) for a in left), left[:8])
check("tv-en.html: the transliteration and the Arabic team tables are not used (names come as the data has them)",
      "const teamAr = (s, id) => String(s || '').trim();" in en and "const TEAM_AR = {};" in en and "const TEAM_BY_ID = {};" in en)
check("tv-en.html: Latin digits unless #digits=hindi", "digits: _Q.get('digits') === 'hindi' ? 'hindi' : 'latin'," in en
      and "digits: _Q.get('digits') === 'latin' ? 'latin' : 'hindi'," in ar and "const AR = ['\\u0660'," in en)
check("tv-en.html: the Hijri months transliterated, the Gregorian ones in English", all(m in en for m in build.HIJRI_EN) and "const EN_MONTHS = ['January'," in en)
# Intl's words differ between ICU versions («Monday, 2 November» or «Monday 2 November»), so between WebViews: the English page takes only
# numbers from Intl (and the shared code's numeric en-GB formatters, FMT_ALL and FMT_HM), and writes the names itself
words_intl = [m.group(0) for m in re.finditer(r"new Intl\.DateTimeFormat\((?:GREG|'en-GB'|'en-US')[^;]*?(?:weekday|month:'long'|month:'short')[^;]*", en)
              if "weekday:'short'}).format(new Date()) === 'Fri'" not in m.group(0) and "America/New_York" not in m.group(0)]
check("tv-en.html: no day or month name from Intl (fixed English names, the same on every WebView)",
      not words_intl and "new Intl.DateTimeFormat(GREG" not in en and "'en-GB',{timeZone: CONFIG.timezone, weekday" not in en, words_intl[:3])

# ---- the generator's own rules
check("every piece of EN_CODE is in the template the times it says (else the build stops)", all(text("src/template.html").count(a) == n for a, _, n in build.EN_CODE))
check("no piece of EN_CODE writes Arabic (the English page's words are the table's)", not [b for _, b, _ in build.EN_CODE if AR.search(b)])
check("EN_CODE leaves no Arabic literal without an entry", not tr.translate(build.english(text("src/template.html")), tr.load())[1])
css = en[en.index("<style>"):en.index("</style>")]
check("the style is laid out left to right: the date block on the left, the prayer bar fills from the left, centring kept",
      ".dates{text-align:left;" in css and ".pbar{" in css and "direction:ltr}" in css[css.index(".pbar{"):css.index(".pbar{") + 200]
      and css.count("left:50%") == text("src/template.html").count("left:50%") and "i + i{margin-left:" in css)
check("the Arabic content keeps right to left in its cards", ".week li.quote,.week li.src,.week li.tafsir,.week li.adf .s{direction:rtl}" in css)
rows = tr.parse(text("i18n/en/page.tsv"), "page.tsv")
check(f"page.tsv: an entry for each of the template's {len(rows)} Arabic literals, none kept Arabic", len(rows) == len({a for _, a, _ in rows})
      and not [a for _, a, e in rows if AR.search(e)])

# ---- the English words in the page's own font (Thmanyah: private, CI only; a SKIP here without it). The team names are data: not here.
thmanyah = ROOT / "assets/fonts/Thmanyah-Regular.woff2"
try:
    from fontTools.ttLib import TTFont
except ImportError:
    TTFont = None
if thmanyah.is_file() and TTFont:
    cmap = set(TTFont(thmanyah).getBestCmap())
    tpl = text("src/template.html")
    teams = {e for _, a, e in rows if a in tpl[tpl.index("const TEAM_SHORT"):tpl.index("__TEAMS_SHORT__")]}   # club names: data
    words = "".join(e for _, a, e in rows if e not in teams) + "".join(build.HIJRI_EN) + "'"
    missing = sorted({c for c in words if not c.isspace() and ord(c) not in cmap})
    check("the English page's words: every character in Thmanyah", not missing, missing)
else:
    print("SKIP the English words in Thmanyah: the font (CI only) or fontTools is not here")

# ---- the app: the page of «اللغة», Arabic as it was
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
upd, prefs = text(APP / "Updater.java"), text(APP / "Prefs.java")
base = prefs[prefs.index("private String pageBase(Context c) {"):]
base = base[:base.index("\n    }\n")]
check("Prefs.pageBase asks Updater.pageBase for English first, then its own Arabic lines, unchanged",
      base.startswith("private String pageBase(Context c) {\n        String own = Updater.pageBase(c, lang(), autoUpdate());")
      and "if (own != null) return own;" in base and 'return useRemote ? Uri.fromFile(remote).toString() : "file:///android_asset/tv.html";' in base)
check("Updater.pageBase: null for Arabic; English: the newer of web/tv-en.html and the asset, else the asset, else (no English page) Arabic",
      "return null;   // Arabic" in upd and 'return "file:///android_asset/" + Tr.pageName(lang);' in upd and "if (hasAsset(c, lang))" in upd)
check("Updater downloads the page of «اللغة» into web/ under the same name, with its own ETag; Arabic keeps URL_PAGE, web/tv.html and \"etag\"",
      "new URL(pageUrl(lang))" in upd and "pageFile(c, lang)" in upd and 'Tr.AR.equals(Tr.norm(lang)) ? "etag" : "etag:" + Tr.pageName(lang)' in upd
      and 'PhotoCache.REPO + "dist/tv.html"' in upd and 'new File(c.getFilesDir(), "web/tv.html")' in upd)
check("Updater: each page on its own clock (\"lastCheck\" for Arabic as always, \"lastCheck:tv-en.html\" for English): a switch to English asks at once",
      upd.count('"lastCheck" + checkSuffix(') == 3 and 'Tr.AR.equals(Tr.norm(lang)) ? "" : ":" + Tr.pageName(lang)' in upd
      and 'getLong("lastCheck", 0)' not in upd and 'putLong("lastCheck", now)' not in upd)
check("Updater: a 404 for the English page is quiet (the asset is used), an Arabic 404 the error line it always was",
      'if (code == HttpURLConnection.HTTP_NOT_FOUND && !Tr.AR.equals(lang)) return "الصفحة محدثة";' in upd
      and upd.index("HTTP_NOT_FOUND") < upd.index('return "تعذر التحديث (" + code + ")";'))
for g in ("android/app/build.gradle", "android/launcher/build.gradle"):
    check(f"{g}: the APK carries tv-en.html beside tv.html", "include 'tv.html', 'tv-en.html'" in text(g))

# ---- HomeCardChoice against Tr (both plain Java): the English page for lang=en, the Arabic link exactly as before
if not shutil.which("javac"):
    print("SKIP HomeCardChoice: no javac here")
    sys.exit(1 if failed else 0)
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
RUN = r'''package sa.urtv.home;
public class I18nPageRun {
    public static void main(String[] a) {
        String[] frags = {"city=Riyadh", "city=Riyadh&lang=en", "lang=en", "city=x&lang=en&digits=latin", "city=x&lang=english", "xlang=en", "", null};
        StringBuilder out = new StringBuilder();
        out.append(HomeCardChoice.BASE_EN.equals("file:///android_asset/web/" + Tr.pageName(Tr.EN) + "#")).append('\n');
        out.append(HomeCardChoice.BASE.equals("file:///android_asset/web/" + Tr.pageName(Tr.AR) + "#")).append('\n');
        for (String f : frags) out.append(HomeCardChoice.base(f).equals(Tr.EN.equals(Tr.fromFragment(f)) ? HomeCardChoice.BASE_EN : HomeCardChoice.BASE)).append('\n');
        out.append(HomeCardChoice.url(new String[] {"city=Riyadh&lang=en", "verse"})).append('\n');
        out.append(HomeCardChoice.url(new String[] {"city=Riyadh", "verse"})).append('\n');
        out.append(HomeCardChoice.url(null)).append('\n');
        System.out.print(out);
    }
}
'''
with tempfile.TemporaryDirectory() as tmp:
    (Path(tmp) / "I18nPageRun.java").write_text(RUN, encoding="utf-8")
    c = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeCardChoice.java"), str(HOME / "Tr.java"), str(Path(tmp) / "I18nPageRun.java")],
                       capture_output=True, text=True)
    check("HomeCardChoice and Tr compile together", c.returncode == 0, c.stderr[-400:])
    if c.returncode == 0:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.I18nPageRun"], capture_output=True, text=True).stdout.split("\n")
        check("HomeCardChoice.BASE_EN is the asset of Tr.pageName(\"en\"), BASE that of Arabic", got[0:2] == ["true", "true"], got[0:2])
        check("HomeCardChoice.base reads the fragment's language as Tr.fromFragment does", got[2:10] == ["true"] * 8, got[2:10])
        check("the home card: lang=en opens tv-en.html; Arabic and nothing known open tv.html as before",
              got[10:13] == ["file:///android_asset/web/tv-en.html#city=Riyadh&lang=en&panel=verse&home_panel=verse&widget=1",
                             "file:///android_asset/web/tv.html#city=Riyadh&panel=verse&home_panel=verse&widget=1",
                             "file:///android_asset/web/tv.html#panel=prayers&widget=1"], got[10:13])
sys.exit(1 if failed else 0)
