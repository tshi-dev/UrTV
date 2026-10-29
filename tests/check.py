#!/usr/bin/env python3
"""Static checks for the whole repo: build, data files, photos, fonts, scripts, safety lists, private data.

    python3 tests/check.py          (needs: pip install fonttools brotli pillow)

Prints one line per check and exits non-zero if any failed. No network.
"""
import json
import py_compile
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

from fontTools.ttLib import TTFont
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
failures = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        failures.append(name)


def tracked(*globs):
    out = subprocess.run(["git", "ls-files", *globs], cwd=ROOT, capture_output=True, text=True).stdout.split()
    return [ROOT / f for f in out]


# ---- build
# Thmanyah's licence allows its files only inside a built product: never in this public repository (CI puts them in place)
leaked = [str(f.relative_to(ROOT)) for f in tracked("*Thmanyah*") + tracked("dist/urtv.html")]
check("Thmanyah's files and the page that inlines them are not committed", not leaked, leaked)
missing_fonts = [f for f in ("Thmanyah-Regular.woff2", "ThmanyahDisplay-Bold.woff2") if not (ROOT / "assets/fonts" / f).is_file()]
check("Thmanyah is in place for the build (CI: the private urtv-fonts checkout; locally: your own copy)", not missing_fonts, missing_fonts)
before = {n: (ROOT / "dist" / n).read_bytes() for n in ("tv.html",) if (ROOT / "dist" / n).is_file()}
r = subprocess.run([sys.executable, "src/build.py"], cwd=ROOT, capture_output=True, text=True)
check("src/build.py runs", r.returncode == 0, r.stderr[-400:])
for page, limit in (("urtv.html", 4_000_000), ("tv.html", 680_000)):   # tv.html: about 1000 verses and hadith (650 KB until the Gulf prayer methods and the checked Aladhan answer added 4 KB; 680 KB from the one release of 26 Oct: next-prayer card, crests, city line, adhan bar, then Wave 2 countries, Hijri offset and the 24-hour clock add about 11 KB)
    html = (ROOT / "dist" / page).read_text(encoding="utf-8")
    left = sorted(set(re.findall(r"__[A-Z][A-Z_]+__", html)))
    check(f"{page}: every build token replaced", not left, left)
    check(f"{page}: size under {limit // 1000} KB", len(html.encode()) < limit, len(html.encode()))
# the build stamp is the time of the newest commit of the sources: a build before the commit carries the one before, so it is left out here
unstamp = lambda b: re.sub(rb'(<meta name="urtv-build" content=")\d{14}', rb"\1", b)
stale = [n for n, b in before.items() if unstamp((ROOT / "dist" / n).read_bytes()) != unstamp(b)]
check("dist/tv.html carries the build stamp the app compares pages by (<meta name=\"urtv-build\">, 14 digits)",
      re.search(rb'<meta name="urtv-build" content="\d{14}">', (ROOT / "dist/tv.html").read_bytes()[:4096]) is not None)
check("dist/ matched a fresh build (run src/build.py after editing src/)", len(before) == 1 and not stale, stale)


# ---- the page must run on an old WebView (Chrome 66): no `?.` and no `??` (Chrome 80), no .flat() / .flatMap() (Chrome 69).
# A script that cannot parse never starts, so one such line leaves the screensaver blank on those TVs.
def too_new_js(html):
    """(line, what) for each `?.` / `??` / `.flat(` / `.flatMap(` in the code of the page's <script> blocks; strings,
    comments and regex literals are skipped, the code inside a template literal's ${...} is read."""
    found = []
    line_of = lambda i: html.count("\n", 0, i) + 1
    for m in re.finditer(r"<script\b[^>]*>(.*?)</script>", html, re.S):
        s, base = m.group(1), m.start(1)
        i, n, stack, last = 0, len(s), [], ""   # stack: one entry per open `${` (the brace depth inside it); last: last code char
        depth = 0
        def skip_template(i):   # s[i] is just after a backtick or a closing `}` of ${}: read the text up to ${ or the closing backtick
            while i < n:
                if s[i] == "\\":
                    i += 2
                elif s[i] == "`":
                    return i + 1, False
                elif s.startswith("${", i):
                    return i + 2, True
                else:
                    i += 1
            return i, False
        while i < n:
            c = s[i]
            if s.startswith("//", i):
                i = s.find("\n", i) if s.find("\n", i) >= 0 else n
            elif s.startswith("/*", i):
                i = s.find("*/", i + 2) + 2 if s.find("*/", i + 2) >= 0 else n
            elif c in "'\"":
                i += 1
                while i < n and s[i] != c and s[i] != "\n":
                    i += 2 if s[i] == "\\" else 1
                i += 1
                last = "a"
            elif c == "`":
                i, inside = skip_template(i + 1)
                if inside:
                    stack.append(depth)
                    depth = 0
                last = "a"
            elif c == "/" and (last == "" or last in "(,=:[!&|?{};+-*%<>~^" or re.search(r"\b(return|typeof|case|in|of)$", s[max(0, i - 7):i])):
                i += 1   # a regex literal: up to the closing slash, not one inside [...]
                cls = False
                while i < n and s[i] != "\n" and (cls or s[i] != "/"):
                    if s[i] == "\\":
                        i += 1
                    elif s[i] == "[":
                        cls = True
                    elif s[i] == "]":
                        cls = False
                    i += 1
                i += 1
                last = "a"
            else:
                if c == "{":
                    depth += 1
                elif c == "}":
                    if depth == 0 and stack:   # the end of a ${...}: back into the template text
                        depth = stack.pop()
                        i, inside = skip_template(i + 1)
                        if inside:
                            stack.append(depth)
                            depth = 0
                        last = "a"
                        continue
                    depth -= 1
                elif c == "?" and s.startswith("??", i):
                    found.append((line_of(base + i), "??"))
                    i += 1
                elif c == "?" and s.startswith("?.", i) and not s[i + 2:i + 3].isdigit():
                    found.append((line_of(base + i), "?."))
                elif c == "." and re.match(r"\.(flat|flatMap)\(", s[i:i + 9]):
                    found.append((line_of(base + i), ".flat("))
                if not c.isspace():
                    last = c
                i += 1
    return found
too_new = too_new_js((ROOT / "src/template.html").read_text(encoding="utf-8"))
check("src/template.html: no ?. or ?? or .flat() in the page's code (they stop the page on a WebView older than 80)", not too_new, too_new)

# ---- data files
for f in tracked("*.json"):
    try:
        json.loads(f.read_text(encoding="utf-8"))
        ok, err = True, ""
    except ValueError as e:
        ok, err = False, e
    check(f"valid JSON: {f.relative_to(ROOT)}", ok, err)

# ---- photos: every listed photo exists, is 1920x1080, has a credit; nothing unlisted
index = json.loads((ROOT / "photos/index.json").read_text(encoding="utf-8"))
listed = {p["f"] for p in index}
on_disk = {p.name for p in (ROOT / "photos").glob("*.jpg")}
check("photos: every index entry has a file", listed <= on_disk, sorted(listed - on_disk))
check("photos: no file missing from the index", on_disk <= listed, sorted(on_disk - listed))
bad = [f for f in sorted(on_disk) if Image.open(ROOT / "photos" / f).size != (1920, 1080)]
check("photos: all 1920x1080", not bad, bad)
credits = (ROOT / "docs/PHOTO-CREDITS.md").read_text(encoding="utf-8")
check("photos: every photo credited in docs/PHOTO-CREDITS.md", all(f in credits for f in on_disk),
      [f for f in on_disk if f not in credits])
bundled = json.loads((ROOT / "assets/backgrounds/backgrounds.json").read_text(encoding="utf-8"))
check("bundled backgrounds exist", all((ROOT / "assets/backgrounds" / b["file"]).is_file() for b in bundled))

# ---- verse and hadith: only checked text, and every character has a glyph
page = (ROOT / "dist/tv.html").read_text(encoding="utf-8")
verses = json.loads(re.search(r"const VERSES = (\[.*?\]);", page, re.S).group(1))
hadith = json.loads(re.search(r"const HADITH = (\[.*?\]);", page, re.S).group(1))
check("verses present", len(verses) >= 200, len(verses))
check("hadith present", len(hadith) >= 200, len(hadith))
wisdom = json.loads(re.search(r"const WISDOM = (\[.*?\]);", page, re.S).group(1))
check("wisdom: at least 50 sayings, none twice, each short, each with a source",
      len(wisdom) >= 50 and len({w["text"] for w in wisdom}) == len(wisdom)
      and all(0 < len(w["text"]) <= 90 and w["source"] for w in wisdom), len(wisdom))
adhkar = json.loads(re.search(r"const ADHKAR = (\[.*?\]);", page, re.S).group(1))
adhkar_raw = json.loads((ROOT / "assets/shared/adhkar.json").read_text(encoding="utf-8"))
BOOKS_OK = ("صحيح البخاري", "صحيح مسلم", "سنن أبي داود", "جامع الترمذي", "سنن النسائي", "سنن ابن ماجه", "سورة")
check("adhkar data: when, text, count and a source in the six books or the Quran; no text twice",
      all(d["when"] in ("morning", "evening", "night", "both") and d["text"] and isinstance(d["count"], int) and d["count"] > 0
          and d["source"].startswith(BOOKS_OK) and (not d.get("quran") or d["source"].startswith("سورة")) for d in adhkar_raw)
      and len({(d["when"], d["text"]) for d in adhkar_raw}) == len(adhkar_raw), [d["text"][:30] for d in adhkar_raw])
check("adhkar on screen are only those the CI check found (marked checked)",
      len(adhkar) == len([d for d in adhkar_raw if d.get("checked")]) or any(len(d["text"].split()) > 70 for d in adhkar_raw if d.get("checked")), len(adhkar))
check("adhkar on screen: counts the card can say in words", all(d["count"] in (1, 2, 3, 4, 7, 10, 33, 100) for d in adhkar), sorted({d["count"] for d in adhkar}))
check("wisdom: only «مثل عربي» or a named classical book (nothing attributed to a person)",
      all(w["source"] in ("مثل عربي", "مجمع الأمثال للميداني") for w in wisdom), sorted({w["source"] for w in wisdom}))
check("at least 500 verses and hadith together (a new one every day for over a year)", len(verses) + len(hadith) >= 500, len(verses) + len(hadith))
basmala = [v["surah"] for v in verses if v["surah"] != "سورة الفاتحة"
           and re.sub("[\u064B-\u065F\u0670]", "", v["text"]).startswith("بسم الله الرحمن الرحيم")]
check("no basmala in front of a verse (it is not part of the verse)", not basmala, basmala)
check("surah names keep آ (آل عمران)", all("سورة ال " not in v["surah"] for v in verses), sorted({v["surah"] for v in verses if "سورة ال " in v["surah"]}))
shown = [json.dumps(h["parts"], ensure_ascii=False) for h in hadith]
check("no verse or hadith twice", len({v["text"] for v in verses}) == len(verses) and len(set(shown)) == len(shown))
raw = json.loads((ROOT / "assets/shared/hadith.json").read_text(encoding="utf-8"))
sys.path.insert(0, str(ROOT / "src"))
from build import narration_parts, simple_parts, weak_in_tirmidhi, weak_by_scholars, albani_accepts, HADITH_MAX_WORDS, ISNAD_MAX_WORDS, js_json   # noqa: E402
embedded = js_json({"c": "</script><b>"})
check("embedded JSON cannot end its <script>", "</" not in embedded and json.loads(embedded)["c"] == "</script><b>", embedded)
sys.path.insert(0, str(ROOT / ".github/tools"))
from verify_hadith import norm as plain   # noqa: E402  (the six-book check's own matching)
form = lambda h: json.dumps(simple_parts(narration_parts(h["full"]["text"])), ensure_ascii=False)
found = {form(h) for h in raw if h.get("found") and h.get("full")}
check("hadith on screen come from narrations found in the six books", set(shown) <= found, len(set(shown) - found))
full_text = {plain(h["full"]["text"]) for h in raw if h.get("full")}
spelled = lambda t: plain(t.replace("ﷺ", " صلى الله عليه وسلم "))   # ﷺ on screen is the book's «صلى الله عليه وسلم»
cut = [t[:40] for h in hadith for k, t in h["parts"] if k == "q" and not any(spelled(t) in f for f in full_text)]
check("his words on screen are the book's words, uncut", not cut, cut)
opener = lambda h: plain(h["parts"][0][1]).split()[1:2]
not_a_name = [plain(h["parts"][0][1])[:30] for h in hadith if h["parts"][0][0] != "n" or not h["parts"][0][1].startswith("عن ")
              or opener(h)[0] in ("رسول", "النبي", "لي", "لنا", "قال", "سمع", "يقول", "ابا", "ابو", "ابيه", "جده", "اذناي", "رجل", "انسا", "سعدا")]
check("each hadith starts from the companion («عن أبي هريرة …»)", not not_a_name, not_a_name)
check("each full narration holds the words it was found by",
      all(plain(h["text"]) in plain(h["full"]["text"]) for h in raw if h.get("full")),
      [h["text"] for h in raw if h.get("full") and plain(h["text"]) not in plain(h["full"]["text"])])
weak = {form(h) for h in raw if h.get("full") and weak_in_tirmidhi(h["full"]["text"])}
check("no hadith al-Tirmidhi grades only غريب/ضعيف, or as a companion's words, is shown", weak and not (weak & set(shown)), len(weak & set(shown)))
long_chain = {form(h) for h in raw if h.get("full") and (lambda w: w and w[0][0] == "n" and len(w[0][1].split()) > ISNAD_MAX_WORDS)(narration_parts(h["full"]["text"]))}
check(f"no hadith with an isnad over {ISNAD_MAX_WORDS} words is shown", not (long_chain & set(shown)), len(long_chain & set(shown)))
graded_weak = {form(h) for h in raw if h.get("full") and weak_by_scholars(h["full"].get("grades"))}
check("no hadith the scholars grade weak (Al-Albani first) is shown", not (graded_weak & set(shown)), len(graded_weak & set(shown)))
not_albani = [f"{h['full']['book']} {h['full']['number']}" for h in raw if h.get("found")
              and not (h.get("full") and albani_accepts(h["full"]["book"], h["full"].get("grades")))]
check("no hadith in the data unless Bukhari, Muslim, or Al-Albani grades it Sahih/Hasan/Hasan Sahih", not not_albani, not_albani)
sunan = [h for h in raw if h.get("full") and h["full"]["book"] not in ("صحيح البخاري", "صحيح مسلم")]
check("every narration from the four Sunan carries the scholars' grades", all(h["full"].get("grades") for h in sunan),
      [h["text"][:30] for h in sunan if not h["full"].get("grades")])
check("every hadith on screen has the Prophet's words and fits the card",
      all(any(k == "q" for k, _ in h["parts"]) and sum(len(t.split()) for _, t in h["parts"]) <= HADITH_MAX_WORDS for h in hadith))
font = set(TTFont(ROOT / "assets/fonts/Thmanyah-Regular.woff2").getBestCmap())
marks = set(TTFont(ROOT / "assets/fonts/NotoNaskhArabic-Marks.woff2").getBestCmap())
text = "".join(v["surah"] for v in verses) + "".join(t for h in hadith for _, t in h["parts"]) \
    + "".join(h["source"] for h in hadith) + "".join(w["text"] + w["source"] for w in wisdom) \
    + "".join(d["source"] + ("" if d.get("quran") else d["text"]) for d in adhkar)
missing = sorted({c for c in text if not c.isspace() and ord(c) not in font and c not in "ﷺ﴿﴾"})   # those: NotoMarks
check("verse/hadith text: every character in Thmanyah", not missing, missing)
# the Quran text (verses, Quranic adhkar) is the KFGQPC Uthmani text, drawn by its own font, whole and unmodified
hafs = TTFont(ROOT / "assets/fonts/UthmanicHafs-v30.woff2")
quran = "".join(v["text"] for v in verses) + "".join(d["text"] for d in adhkar if d.get("quran"))
missing = sorted({c for c in quran if not c.isspace() and ord(c) not in hafs.getBestCmap()})
check("Quran text: every character in the Uthmanic Hafs font", not missing, missing)
check("Quran font: the whole KFGQPC 3.0 font, licence in its name table (not subset, see UthmanicHafs-NOTICE.txt)",
      hafs["name"].getDebugName(5) == "Version 3.0" and "KFGQPC" in str(hafs["name"].getDebugName(13)) and len(hafs.getBestCmap()) > 250,
      (hafs["name"].getDebugName(5), len(hafs.getBestCmap())))
raw_verses = json.loads((ROOT / "assets/shared/verses.json").read_text(encoding="utf-8"))
check("Quran text: every verse has its KFGQPC Uthmani text and the page shows exactly that",
      all(v.get("uthmani") for v in raw_verses) and [v["text"] for v in verses] == [" ".join(v["uthmani"].replace("\u06DE", "").split()) for v in raw_verses],
      [v["ref"] for v in raw_verses if not v.get("uthmani")])
check("Quranic adhkar carry their Uthmani text", all(d.get("uthmani") for d in adhkar_raw if d.get("quran")))
check("NotoMarks has ﷺ ﴿ ﴾", {0xFDFA, 0xFD3E, 0xFD3F} <= marks)
team_ar = json.loads((ROOT / "assets/shared/teams-ar.json").read_text(encoding="utf-8"))
names = "".join(v for v in (team_ar.values() if isinstance(team_ar, dict) else []) if isinstance(v, str))
missing = sorted({c for c in names if not c.isspace() and ord(c) not in font})
check("Arabic team names: every character in Thmanyah", not missing, missing)

# ---- the clubs list (assets/shared/teams.json, built by .github/tools/team_ids.py in CI)
from team_ids import ARAB, KNOWN, LEAGUES, NATIONAL, NATIONAL_LEAGUE, plain_ar, team_key   # noqa: E402
clubs = json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))
ids = [c["id"] for c in clubs]
check("clubs: unique ids", len(ids) == len(set(ids)), sorted({i for i in ids if ids.count(i) > 1}))
by_id = {c["id"]: c for c in clubs}
check("clubs: the known ids are the clubs the app uses",
      all(team_key(by_id.get(i, {}).get("en", "")) == team_key(n) for i, n in KNOWN.items()),
      {i: by_id.get(i, {}).get("en") for i, n in KNOWN.items() if team_key(by_id.get(i, {}).get("en", "")) != team_key(n)})
prefs_leagues = re.findall(r'\{"([^"]+)", "[^"]+"\}', (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java")
                           .read_text(encoding="utf-8").split("LEAGUES = {")[1].split("};")[0])
count = {l: sum(l in c["leagues"] for c in clubs) for l in prefs_leagues}
PENDING = set()
check("clubs: every league of the app has at least its minimum", all(count[l] >= LEAGUES.get(l, {}).get("min", 1)
      for l in prefs_leagues if l not in PENDING or count[l]), {l: (n, LEAGUES.get(l, {}).get("min")) for l, n in count.items()
                                                         if n < LEAGUES.get(l, {}).get("min", 1) and (l not in PENDING or n)})
national = [c for c in clubs if NATIONAL_LEAGUE in c["leagues"]]
check(f"clubs: all {len(NATIONAL)} national teams", len(national) == len(NATIONAL), len(national))
ours = [c for c in clubs if NATIONAL_LEAGUE in c["leagues"] or any(LEAGUES.get(l, {}).get("country") == "Saudi Arabia"
                                                                     for l in c["leagues"])]
check("clubs: every Saudi club and national team has our own or Wikidata's Arabic name, not a transliteration",
      all(c.get("ar") and c.get("arFrom") != "translit" for c in ours),
      [c["en"] for c in ours if not c.get("ar") or c.get("arFrom") == "translit"])
arab = [c for c in clubs if any(LEAGUES.get(l, {}).get("country") in ARAB for l in c["leagues"])]
check("clubs: no club of an Arab league is named by transliteration (add it to teams-ar.json)",
      all(c.get("arFrom") != "translit" for c in arab), [c["en"] for c in arab if c.get("arFrom") == "translit"])
check("clubs: every team has an Arabic name", all(c.get("ar") for c in clubs), [c["en"] for c in clubs if not c.get("ar")][:10])
check("clubs: each Arabic name's source is given", all(c.get("arFrom") in ("curated", "wikidata", "thesportsdb", "translit")
                                                      for c in clubs), sorted({str(c.get("arFrom")) for c in clubs}))
names = {}
for c in clubs:
    if c.get("ar"):
        names.setdefault(plain_ar(c["ar"]), []).append(c["en"])
check("clubs: no two teams share an Arabic name", all(len(v) == 1 for v in names.values()),
      {k: v for k, v in names.items() if len(v) > 1})
missing = sorted({ch for c in clubs for ch in c.get("ar", "") if not ch.isspace() and ord(ch) not in font})
check("clubs: every character of the Arabic names in Thmanyah", not missing, missing)

# ---- java: a loop over a table (String[][]) must take a row (String[]); the APK build broke on this twice
java = {f: f.read_text(encoding="utf-8") for f in tracked("android/*.java")}
bad = [f"{f.name}: {m.group(0)}" for f, src in java.items()
       for m in re.finditer(r"for \(String (\w+) : (\w+)\)", src)
       if re.search(r"String\[\]\[\] " + m.group(2) + r"\b", src)]
check("java: no table looped over as single strings", not bad, bad)
# the TV's font size setting scales WebView text; our pages size their own, so every WebView pins it at 100
loose = [f.name for f, src in java.items() if "getSettings()" in src and "setTextZoom(100)" not in src]
check("java: every WebView ignores the TV's font size (setTextZoom(100))", not loose, loose)
# a Latin word at the start of an Arabic line lands at its far left (bidi): it goes in isolates (\u2066 ... \u2069)
bidi = [f"{f.name}: {m.group(0)[:40]}" for f, src in java.items()
        for m in re.finditer(r'"[A-Za-z]+ [\u0600-\u06FF][^"\n]*"', src)]
bidi += [f"{x.name}: {m.group(0)[:40]}" for x in (ROOT / "android").glob("*/src/main/res/values*/*.xml")
         for m in re.finditer(r'<string [^>]*>[A-Za-z]+ [\u0600-\u06FF][^<]*', x.read_text(encoding="utf-8"))]
check("java: no Arabic string starts with a Latin word without an isolate", not bidi, bidi)
# the select key is «اختيار» in the hints, never the Latin word OK (it falls out of Thmanyah and shows in the system font)
okword = [f"{f.name}: {m.group(0)[:40]}" for f, src in java.items()
          for m in re.finditer(r'"[^"\n]*[\u0600-\u06FF][^"\n]*"', src) if re.search(r"\bOK\b", m.group(0))]
okword += [f"{x.name}: {m.group(0)[:40]}" for x in (ROOT / "android").glob("*/src/main/res/values*/*.xml")
           for m in re.finditer(r"<string [^>]*>[^<]*", x.read_text(encoding="utf-8")) if re.search(r"\bOK\b", m.group(0))]
check("java: no «OK» in an Arabic hint (use «اختيار»)", not okword, okword)
# the digits setting (hindi or latin) reaches every Java screen through DigitsText, one helper per app: nothing turns a digit by hand
by_hand = [f"{f.name}: {m.group(0)}" for f, src in java.items() if f.name not in ("DigitsText.java",)
           for m in re.finditer(r"'[٠\u0660]'\s*\+|\+\s*'[٠\u0660]'|'[0-9]'\s*\+\s*\(?\w+\s*-\s*'٠'", src)]
check("java: no digit conversion by hand outside DigitsText (Digits.of / DigitsText.convert)", not by_hand, by_hand)
# the preview and the settings must open fresh: an open one comes back with the settings it opened with, or in front of
# the one asked for (a TV test read the preview as the settings screen, and as the settings not working)
starts = [f"{f.name}: {l.strip()}" for f in sorted((ROOT / "optimize").glob("*.sh"))
          for l in f.read_text(encoding="utf-8").splitlines()
          if re.search(r"sa\.urtv\.screensaver/\.(MainActivity|SettingsActivity)", l) and "am start" in l
          and "--activity-clear-task" not in l]
check("optimize: the screensaver preview and settings always open fresh (--activity-clear-task)", not starts, starts)

# ---- docs/SETTINGS.md lists every setting, so agents use it instead of building one
settings_md = (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8")
def commands(script):   # the words a script's case accepts: "  show)", "  hide:*|show:*)", "  pause|resume)"
    src = (ROOT / "optimize" / script).read_text(encoding="utf-8")
    return {w.split(":")[0] for m in re.finditer(r"^  ([a-z][a-z|:*-]*)\)", src, re.M) for w in m.group(1).split("|")}
missing = sorted(f"{sc} {c}" for sc in ("saver.sh", "dock.sh", "music.sh", "photos.sh") for c in commands(sc)
                 if c not in ("help", "-h", "--help")
                 and not re.search(rf"{re.escape(sc)} {re.escape(c)}\b|`{re.escape(c)}\b|, {re.escape(c)}\b", settings_md))
tpl_src = (ROOT / "src/template.html").read_text(encoding="utf-8")
CARD_RE = {"panel": r"const CARD_KINDS = \[([^\]]+)\]", "slot": r"  slot: \(\w+ => \[([^\]]+)\]"}   # the page's lists of cards and slot choices
for key in ("panel", "slot"):
    m = re.search(CARD_RE[key], tpl_src)
    missing += [f"{key}={v}" for v in re.findall(r"'([a-z]+)'", m.group(1) if m else "") if v not in settings_md]
check("docs/SETTINGS.md: every saver.sh / dock.sh / music.sh / photos.sh command and every card and slot choice", not missing, missing)
# docs/FEATURES.md explains each feature to people: every saver.sh / photos.sh command and every card and slot choice
features_md = (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8")
unexplained = sorted(f"{sc} {c}" for sc in ("saver.sh", "photos.sh") if (ROOT / "optimize" / sc).exists() for c in commands(sc)
                     if c not in ("help", "-h", "--help")
                     and not re.search(rf"{re.escape(sc)} {re.escape(c)}\b|`{re.escape(c)}\b|، {re.escape(c)}\b", features_md))
for key in ("panel", "slot"):
    mm = re.search(CARD_RE[key], tpl_src)
    unexplained += [f"{key}={v}" for v in re.findall(r"'([a-z]+)'", mm.group(1) if mm else "") if v not in features_md]
check("docs/FEATURES.md: every saver.sh / photos.sh command and every card and slot choice is explained", not unexplained, unexplained)

# the app's list of cards (Prefs.PANELS: what settings and saver.sh accept) and the page's must be the same words
page_panels = re.search(CARD_RE["panel"], tpl_src)
page_panels = re.findall(r"'([a-z]+)'", page_panels.group(1) if page_panels else "") + ["none"]   # «none»: no card, not a kind
app_panels = re.findall(r'\{"([a-z]+)", "[^"]+"\}', re.search(r"PANELS = \{(.*?)\n    \};", (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8"), re.S).group(1))
check("cards: Prefs.PANELS and the page's #panel= list name the same cards", sorted(page_panels) == sorted(app_panels),
      f"page only {sorted(set(page_panels) - set(app_panels))}, app only {sorted(set(app_panels) - set(page_panels))}")

# the settings' pickers say in one plain line what each card and each card style is, in the words of docs/FEATURES.md
prefs_src = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
def table(name):
    body = re.search(rf"{name} = \{{(.*?)\}};", prefs_src, re.S).group(1)
    return dict(re.findall(r'\{"([a-z0-9]+)", "([^"]+)"\}', body))
flat = " ".join(features_md.split())
for tbl, src in (("PANEL_ABOUT", "PANELS"), ("CARD_STYLE_ABOUT", "CARD_STYLES")):
    about = table(tbl)
    names = [k for k, _ in re.findall(r'\{"([a-z0-9]+)", "([^"]+)"', re.search(rf"{src} = \{{(.*?)\}};", prefs_src, re.S).group(1))]
    check(f"settings: every entry of Prefs.{src} has its one line in Prefs.{tbl}", sorted(about) == sorted(names), sorted(set(names) ^ set(about)))
    check(f"settings: each line of Prefs.{tbl} is in the words of docs/FEATURES.md", all(" ".join(v.split()) in flat for v in about.values()),
          [k for k, v in about.items() if " ".join(v.split()) not in flat])

# ---- launcher config
cfg = ET.parse(ROOT / "android/launcher/src/main/res/values/config.xml").getroot()
arr = {a.get("name"): [i.text for i in a] for a in cfg.findall("string-array")}
check("launcher: no dock app is also hidden", not set(arr["dock_apps"]) & set(arr["hidden_apps"]))
check("launcher: screensaver app not hidden", "sa.urtv.screensaver" not in arr["hidden_apps"])

# Arabic app names: package|name rows; the names are drawn with Thmanyah, so it needs a glyph for each character
app_rows = [(i.text or "").split("|", 1) for i in ET.parse(ROOT / "android/launcher/src/main/res/values/app_names.xml")
            .getroot().find("string-array")]
pkg_re = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$")
arabic_re = re.compile("[\u0600-\u06FF]")
check("app names: every row is package|name", bool(app_rows) and all(len(r) == 2 for r in app_rows))
check("app names: every key is a valid package name", all(pkg_re.match(r[0]) for r in app_rows), [r[0] for r in app_rows if not pkg_re.match(r[0])])
check("app names: every name has Arabic letters", all(len(r) == 2 and arabic_re.search(r[1]) for r in app_rows), [r[0] for r in app_rows if len(r) < 2 or not arabic_re.search(r[1])])
check("app names: no package twice", len({r[0] for r in app_rows}) == len(app_rows))
missing = sorted({c for r in app_rows for c in r[-1] if not c.isspace() and ord(c) not in font})
check("app names: every character in Thmanyah", not missing, missing)

# the launcher Java compiles as far as it can without the Android SDK: errors about missing Android classes are ignored
java_src = sorted((ROOT / "android/launcher/src/main/java").rglob("*.java"))
with tempfile.TemporaryDirectory() as out:
    r = subprocess.run(["javac", "-proc:none", "-Xmaxerrs", "10000", "-encoding", "UTF-8", "-d", out] + [str(f) for f in java_src],
                       capture_output=True, text=True)
real = [f"{loc}: {m}" for loc, m in re.findall(r"^(\S+\.java:\d+): error: (.*)$", r.stderr, re.M)
        if not re.search(r"cannot find symbol|does not override or implement|package .* does not exist|static import only from classes", m)]
check("launcher Java: no type errors beyond the missing Android classes", not real, real[:8])

# ---- optimize: never disable a kept package
def pkgs(path):
    return {l.split("#")[0].strip() for l in path.read_text().splitlines() if l.split("#")[0].strip()}
keep = pkgs(ROOT / "optimize/lists/keep.txt")
for lst in sorted((ROOT / "optimize/lists").glob("*.txt")):
    if lst.name != "keep.txt":
        check(f"optimize: {lst.name} disables nothing from keep.txt", not pkgs(lst) & keep, sorted(pkgs(lst) & keep))

# ---- scripts
for f in tracked("*.py"):
    try:
        py_compile.compile(str(f), doraise=True)
        ok, err = True, ""
    except py_compile.PyCompileError as e:
        ok, err = False, e
    check(f"python compiles: {f.relative_to(ROOT)}", ok, err)
for f in tracked("*.sh"):
    r = subprocess.run(["bash", "-n", str(f)], capture_output=True, text=True)
    check(f"bash syntax: {f.relative_to(ROOT)}", r.returncode == 0, r.stderr)

# ---- nothing private or secret in the repo
texts = [f for f in tracked() if f.suffix in {".md", ".py", ".sh", ".java", ".xml", ".json", ".yml", ".html", ".txt", ".gradle"}]
ip = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
allowed_ip = {"0.0.0.0", "127.0.0.1"}
# the network check and its simulation hold only address ranges and made-up addresses, never someone's network
network_code = {ROOT / "optimize/find-tv.py", ROOT / "tests/find_tv_test.py", ROOT / "optimize/wireless.py", ROOT / "tests/wireless_test.py",
                # the report's privacy filter and its test: version strings that look like addresses, and made-up addresses it must remove
                ROOT / "optimize/share.py", ROOT / "tests/share_test.py"}
found = []
for f in texts:
    if f.name.endswith(".html") or f.parent.name == "photos" or f == Path(__file__).resolve():   # not this scan
        continue
    if f in network_code:
        s = f.read_text(encoding="utf-8", errors="ignore")
        if re.search(r"(?i)tailscale|\.ts\.net", s):
            found.append(f"{f.relative_to(ROOT)}: tailscale")
        continue
    s = f.read_text(encoding="utf-8", errors="ignore")
    found += [f"{f.relative_to(ROOT)}: {m}" for m in set(ip.findall(s)) - allowed_ip
              if all(0 <= int(x) <= 255 for x in m.split(".")) and not m.startswith(("1.", "2.", "3.", "4."))
              and not m.startswith(("192.0.2.", "198.51.100.", "203.0.113."))]   # reserved for examples
    if re.search(r"(?i)(client-id|access_key|api_key)\s*[:=]\s*['\"][A-Za-z0-9_-]{20,}", s):
        found.append(f"{f.relative_to(ROOT)}: looks like a key")
    if re.search(r"(?i)tailscale|\.ts\.net", s):
        found.append(f"{f.relative_to(ROOT)}: tailscale")
check("no private IPs, keys or network names", not found, found[:10])

print()
print(f"{len(failures)} failed" if failures else "all checks passed")
sys.exit(1 if failures else 0)
