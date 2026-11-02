#!/usr/bin/env python3
"""src/template.html -> three pages in dist/:

urtv.html           stand-alone: fonts and the bundled photos inlined, works offline from a double-click. Not committed:
                    it carries Thmanyah, whose licence allows the font only inside a built product.
tv.html             for the Android app: same page, but fonts and bundled photos are URLs the app serves from
                    its own assets (PhotoCache.java). 2.2 MB less to parse, so the screensaver starts faster.
tv-en.html          tv.html in English, for «اللغة» English (docs/ENGLISH.md): the template with EN_CODE below, laid out left to right
                    (en_css), through i18n/tr.py with i18n/en/page.tsv. tv.html is written exactly as before. The verses, hadith,
                    adhkar and sayings stay Arabic (right to left in their cards); team names are the English names the data has.
"""
import base64
import json
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "i18n"))
import tr   # noqa: E402  (i18n/tr.py: the table and the hook that writes the English copy)
FONTS = {
    "__F_LIGHT__": "Thmanyah-Light.woff2",
    "__F_REGULAR__": "Thmanyah-Regular.woff2",
    "__F_MEDIUM__": "Thmanyah-Medium.woff2",
    "__F_DISPLAY__": "ThmanyahDisplay-Bold.woff2",
    "__F_HAFS__": "UthmanicHafs-v30.woff2",   # the whole font, unmodified: its licence forbids changing it (see UthmanicHafs-NOTICE.txt)
}
# tv.html points here for fonts and bundled photos; the app answers from its assets, a browser from GitHub.
REPO = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/"


TAFSIR_MAX = 130   # characters kept of «التفسير الميسر»; the card shows at most three lines of it


def tafsir_for_screen(text):
    """The tafsir cut at a word to TAFSIR_MAX characters, «…» after it if cut ('' when there is none)."""
    t = " ".join((text or "").split())
    if len(t) <= TAFSIR_MAX:
        return t
    return t[:TAFSIR_MAX].rsplit(" ", 1)[0].rstrip(" ،,:؛.") + "…"


def verses_for_screen():
    """assets/shared/verses.json as fetched by .github/tools/fetch_verses.py. The text shown is the `uthmani` one, the
    KFGQPC text the Uthmanic Hafs font is made for, kept exactly as fetched (waqf signs included, the font draws them;
    its code points are not normalised or re-ordered) except the ۞ hizb sign the data puts in front of a word. A verse without it is left out: this font gets no other text.
    The basmala is not part of that text. Surah names are shown plain, without harakat, in Thmanyah."""
    path = ROOT / "assets/shared/verses.json"
    if not path.is_file():
        return []
    out = []
    for v in json.loads(path.read_text(encoding="utf-8")):
        if not v.get("uthmani"):
            continue
        # NFC first: the API writes آ as ا + madda, which stripping the harakat would turn into ا (ال عمران)
        name = re.sub("[\u064B-\u065F\u0670\u06D6-\u06ED]", "", unicodedata.normalize("NFC", v["surah"]))
        name = name.replace("\u0671", "ا")   # ٱ: no glyph in Thmanyah
        e = {"text": " ".join(v["uthmani"].replace("\u06DE", "").split()), "surah": name, "ayah": v["ayah"]}
        if v.get("tafsir"):
            e["tafsir"] = tafsir_for_screen(v["tafsir"])
        out.append(e)
    return out


HADITH_MAX_WORDS = 70   # a longer hadith does not fit the card beside the clock; it stays in the data
ISNAD_MAX_WORDS = 35    # nor is one shown whose chain of narrators runs longer (.github/tools/verify_hadith.py)
HARAKAT = "[\u064B-\u0652\u0670]*"
SAW = re.compile(r"\s+".join(HARAKAT.join(w) + HARAKAT for w in "صلى الله عليه وسلم".split()))


def narration_parts(text):
    """A full narration as the book has it -> [["n", narration], ["q", the Prophet's words], ...].

    The books put his words between straight quotes; everything else (the isnad, who asked what, the compiler's
    note) is narration. Only typography changes: direction marks and the tatweel used as a dash go, «صلى الله
    عليه وسلم» becomes ﷺ, {verse} becomes ﴿verse﴾, and no space before a comma or full stop."""
    t = re.sub("[\u200e\u200f]", "", text)
    t = re.sub(r"\s+ـ\s+", " ", t)
    t = SAW.sub("ﷺ", t).replace("{", "﴿").replace("}", "﴾")
    t = re.sub(r"\s+([،,.:؛])", r"\1", t)
    parts = []
    for i, piece in enumerate(t.split('"')):
        piece = " ".join(piece.split()).strip(" .")
        if piece and piece not in ".،:":
            parts.append(["q" if i % 2 else "n", piece])
    return parts


LINK = re.compile(r"(?:^|(?<=[\s،,:]))(?:عن|سمعت|سمع|سمعا|حدثني|حدثنا|أخبرني|أخبرنا|أن|أنه|إن|يحدث|(?<=قال )قال)(?=[\s،,:])")
TRAIL = re.compile(r"(?:[\s،,:.]|\b(?:قال|قالت|يقول|سمعت|سمع|عن|أن|أنه|إن|أخبره|حدثه|لي|لنا)\b)+$")
SECOND_CHAIN = re.compile(r"ﷺ\s*\.\s*و?(?:عن|حدثنا|حدثني)\s")
NOT_A_NAME = {"أبيه", "أبيها", "جده", "أذناى", "أذناي", "أذني", "رجل", "رجلا"}
NOTE = re.compile(r"قال أبو عيسى|وفي الباب|حدثنا|حدثني|أخبرنا|وفي رواية|وفي حديث|وبهذا الإسناد|وزاد|نحوه|قال أبو داود|قال أبو معاوية")
PROPHET = re.compile(r"رسول الله|النبي")


def bare_map(text):
    """text without harakat, and for each of its characters the index in text (to cut the original there)."""
    keep = [(c, i) for i, c in enumerate(text) if not re.match(HARAKAT[:-1], c)]
    return "".join(c for c, _ in keep), [i for _, i in keep] + [len(text)]


def simple_parts(parts):
    """The «Riyad as-Salihin» form: from the companion who narrated it, «عن أبي هريرة …», then the hadith as the book
    has it. The narrators before the companion, and the compiler's notes after the Prophet's last words, go."""
    if not parts or parts[0][0] != "n":
        return parts
    head = parts[0][1]
    bare, at = bare_map(head)
    second = list(SECOND_CHAIN.finditer(bare))   # «… عن النبي ﷺ. وحدثنا …»: the book gives a second chain, use it
    if second:
        head = head[at[second[-1].end()]:]
        bare, at = bare_map(head)
    m = PROPHET.search(bare)
    if not m:
        return None
    pre = TRAIL.sub("", bare[:m.start()].rstrip())
    links = list(LINK.finditer(pre))
    if not links:
        return None
    pick = -1   # «عن أبيه», «عن جده», «سمعت أذناي», «قال رجل»: start a narrator earlier until it says who
    while pre[links[pick].end():].strip(" ،,:").split(" ")[0].strip("،,") in NOT_A_NAME and len(links) > -pick:
        pick -= 1
    start = links[pick].end()
    while start < len(bare) and bare[start] in " ،,:":
        start += 1
    rest = head[at[start]:].strip()
    rest = re.sub("^" + HARAKAT.join("أبا") + "|^" + HARAKAT.join("أبو"), "أبي", rest)   # after «عن»: أبي هريرة
    rest = re.sub(r"\s" + HARAKAT.join("أخبره") + HARAKAT + "(?:" + HARAKAT.join("ما") + "|م)?" + HARAKAT + r"(?=\s)", "", rest)   # «… أخبره أن …»
    rest = re.sub("^([^\\s،,]{3,}?)" + "ا" + "[\u064B\u064E]*" + r"(?=[\s،,])", r"\1", rest)   # «سمعت أنسا» -> «عن أنس»
    out = [["n", "عن " + rest]]
    for k, t in parts[1:]:   # stop at the compiler's notes or a further chain between his words
        if k == "n" and NOTE.search(re.sub(HARAKAT, "", t)):
            break
        out.append([k, t])
    while out and out[-1][0] == "n":
        out.pop()
    return out


WEAK = re.compile(r"da.?if|weak|munkar|mawdu|fabricat|shadh|mudtarib", re.I)   # as .github/tools/verify_hadith.py
ALBANI_OK = {"sahih", "hasan", "hasan sahih"}   # as .github/tools/verify_hadith.py
GRADE_AR = {"sahih": "صحيح", "hasan": "حسن", "hasan sahih": "حسن صحيح", "sahih lighairihi": "صحيح لغيره",
            "hasan lighairihi": "حسن لغيره"}


def scholars(grades):
    """The four Sunan: Al-Albani's grade decides when the scholars differ; with no Al-Albani, all of them."""
    albani = [g for g in grades or [] if "albani" in g.get("name", "").lower()]
    return albani or grades or []


def weak_by_scholars(grades):
    return any(WEAK.search(g.get("grade", "")) for g in scholars(grades))


def albani_accepts(book, grades):
    """Bukhari and Muslim: yes. The four Sunan: only if Al-Albani grades the narration Sahih, Hasan or Hasan Sahih;
    one he grades weak in any form, or does not grade, is never shown."""
    if book in ("صحيح البخاري", "صحيح مسلم"):
        return True
    albani = [g.get("grade", "") for g in grades or [] if "albani" in g.get("name", "").lower()]
    return bool(albani) and all(" ".join(re.sub(r"[^a-z ]", " ", a.lower()).split()) in ALBANI_OK for a in albani)


def albani_grade(grades):
    for g in grades or []:
        if "albani" in g.get("name", "").lower():
            ar = GRADE_AR.get(re.sub(r"[^a-z ]", "", g.get("grade", "").lower()).strip())
            return f"{ar} عند الألباني" if ar else ""
    return ""


def tirmidhi_grade(text):
    bare = re.sub(HARAKAT, "", text)
    if "قال أبو عيسى" not in bare:
        return ""
    g = re.search("حسن صحيح|صحيح حسن|حسن غريب|صحيح غريب|حسن|صحيح", bare[bare.index("قال أبو عيسى"):])
    return g.group(0) if g else ""


def weak_in_tirmidhi(text):
    """al-Tirmidhi grades each hadith after it («قال أبو عيسى …»). Not shown: one he grades only غريب or ضعيف,
    or says is more correctly a companion's own words (موقوف, «ولم يرفعه»)."""
    bare = re.sub(HARAKAT, "", text)
    if "قال أبو عيسى" not in bare:
        return False
    note = bare[bare.index("قال أبو عيسى"):]
    return not re.search("حديث حسن|حديث صحيح|حسن صحيح", note) or "ولم يرفعه" in bare or "موقوفا" in bare


def hadith_for_screen():
    """assets/shared/hadith.json -> each hadith from its companion on (simple_parts) if it fits the card, with the
    book, its number and a grade: al-Tirmidhi's own, or for the other Sunan Al-Albani's. One a scholar grades weak,
    one from the four Sunan Al-Albani does not grade Sahih, Hasan or Hasan Sahih, or one al-Tirmidhi does not grade
    حسن or صحيح, is not shown."""
    digits = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")
    out = []
    for h in json.loads((ROOT / "assets/shared/hadith.json").read_text(encoding="utf-8")):
        full = h.get("full")
        if not (h.get("found") and full):
            continue
        whole = narration_parts(full["text"])
        if whole and whole[0][0] == "n" and len(whole[0][1].split()) > ISNAD_MAX_WORDS:
            continue
        parts = simple_parts(whole)
        if not parts:
            continue
        words = sum(len(p[1].split()) for p in parts)
        if weak_in_tirmidhi(full["text"]) or weak_by_scholars(full.get("grades")) \
                or not albani_accepts(full["book"], full.get("grades")):
            continue
        if any(k == "q" for k, _ in parts) and words <= HADITH_MAX_WORDS:
            grade = tirmidhi_grade(full["text"]) or albani_grade(full.get("grades"))
            source = f"{full['book']} · {str(full['number']).translate(digits)}" + (f" · {grade}" if grade else "")
            out.append({"parts": parts, "source": source})
    return out


def data_uri(path, mime):
    return f"data:{mime};base64," + base64.b64encode(path.read_bytes()).decode("ascii")


def js_json(value):
    """json.dumps for a <script>: a "</script>" inside a text would end it, "<\\/" is the same string in JSON."""
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")


ADHKAR_MAX_WORDS = 70   # a longer dhikr does not fit the card beside the clock


def adhkar_for_screen():
    """assets/shared/adhkar.json -> only the adhkar .github/tools/verify_adhkar.py found (and apply_adhkar_report.py marked
    `checked`) in the six books or the Quran, in file order. A Quranic one is shown in the Uthmanic Hafs font with its `uthmani` text, as in verses_for_screen."""
    path = ROOT / "assets/shared/adhkar.json"
    out = []
    for d in json.loads(path.read_text(encoding="utf-8")) if path.is_file() else []:
        if not d.get("checked") or d["when"] not in ("morning", "evening", "night", "both"):
            continue
        if d.get("quran"):
            if not d.get("uthmani"):
                continue
            text = " ".join(d["uthmani"].split())
        else:
            text = " ".join(re.sub("[\u06D6-\u06DE\u06E9]", "", d["text"]).replace("\u0671", "\u0627").split())
        if len(text.split()) > ADHKAR_MAX_WORDS:
            continue
        e = {"when": d["when"], "text": text, "count": d["count"], "source": d["source"]}
        if d.get("quran"):
            e["quran"] = True
        if d.get("virtue"):   # the hadith's own words, shown as the source line
            e["virtue"] = d["virtue"]
        out.append(e)
    return out


# NBA and NFL teams are «city nickname»; two games side by side may need the nickname alone («ليكرز × ووريورز»).
# The city takes as many Arabic words as English ones, but for the few written as one word.
US_SPORTS = ("Basketball", "American Football")
CITY_ONE_WORD = {"New York"}
NICK_TWO_WORDS = {"Trail Blazers"}


def us_short(clubs):
    out = {}
    for c in clubs:
        ar, en = c.get("ar", ""), c.get("en", "").split()
        if c.get("sport") not in US_SPORTS or not ar or "(" in ar or len(en) < 2:
            continue
        nick = 2 if " ".join(en[-2:]) in NICK_TWO_WORDS else 1
        city = " ".join(en[:-nick])
        drop = 1 if city in CITY_ONE_WORD else len(city.split())
        words = ar.split()
        if len(words) > drop:
            out[ar] = " ".join(words[drop:])
    return out


# ---- the English page (tv-en.html). The words are i18n/en/page.tsv (tr.translate); what a table cannot do is here: each pair is a
# piece of the template's code, exactly as it is there (the times it must be found), and what the English page has instead. A template
# change that moves one of them stops the build, so the English page never silently keeps an Arabic line.
HIJRI_EN = ["Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
            "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah"]
EN_CODE = [
    # Latin digits unless Arabic-Indic ones are asked for; the dates and numbers in English
    ("digits: _Q.get('digits') === 'latin' ? 'latin' : 'hindi',", "digits: _Q.get('digits') === 'hindi' ? 'hindi' : 'latin',", 1),
    # the Gregorian words from fixed English names, never from Intl's en-GB (its commas and order differ between ICU versions, so between
    # WebViews): Intl gives only the numbers of the day in the zone. «Monday, 2 November 2026», «Monday 2 November», «2 November», «Mon»
    ("const GREG = 'ar-SA-u-ca-gregory-nu-' + NU;", """const GREG = 'en-u-ca-gregory-nu-latn';
const EN_DAYS = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
const EN_MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
const EN_YMD = {};
function enDay(d, tz){   // {y, m, d, wd} of the day d is in, in that zone (the numbers only from Intl)
  tz = tz || 'UTC';
  const f = EN_YMD[tz] || (EN_YMD[tz] = new Intl.DateTimeFormat('en-US', {timeZone: tz, year:'numeric', month:'numeric', day:'numeric'}));
  const p = f.formatToParts(d).reduce((a, x) => (a[x.type] = x.value, a), {});
  const y = parseInt(p.year, 10), m = parseInt(p.month, 10), dd = parseInt(p.day, 10);
  return {y, m, d: dd, wd: new Date(Date.UTC(y, m - 1, dd)).getUTCDay()};
}
function enDate(d, o){
  const p = enDay(d, o.timeZone);
  const wd = o.weekday === 'short' ? EN_DAYS[p.wd].slice(0, 3) : o.weekday ? EN_DAYS[p.wd] : '';
  const dm = o.day ? ar(p.d) + (o.month ? ' ' + EN_MONTHS[p.m - 1] : '') : o.month ? EN_MONTHS[p.m - 1] : '';
  const date = [dm, o.year ? ar(p.y) : ''].filter(Boolean).join(' ');
  return wd && date ? wd + (o.year ? ', ' : ' ') + date : wd || date;
}""", 1),
    ("const gregFmt = opts => { const f = new Intl.DateTimeFormat(GREG, opts); return {format: d => gm(f.format(d))}; };",
     "const gregFmt = opts => ({format: d => enDate(d, opts)});", 1),
    ("const FMT_WEEKDAY = new Intl.DateTimeFormat(GREG,{timeZone:CONFIG.timezone,weekday:'long'});",
     "const FMT_WEEKDAY = gregFmt({timeZone:CONFIG.timezone,weekday:'long'});", 1),
    ("const dayName = d => new Intl.DateTimeFormat(GREG, {timeZone:'UTC', weekday:'long'}).format(d);",
     "const dayName = d => enDate(d, {timeZone:'UTC', weekday:'long'});", 1),
    ("new Intl.DateTimeFormat(GREG,{timeZone:CONFIG.timezone,weekday:'short'}).format(new Date(o.date+'T12:00:00Z'))",
     "enDate(new Date(o.date+'T12:00:00Z'), {timeZone:CONFIG.timezone, weekday:'short'})", 1),
    ("new Intl.DateTimeFormat(GREG, {timeZone:'UTC', weekday:'long'}).format(new Date(d.date + 'T12:00:00Z'))",
     "enDate(new Date(d.date + 'T12:00:00Z'), {timeZone:'UTC', weekday:'long'})", 1),
    ("new Intl.NumberFormat('ar-SA-u-nu-' + NU,", "new Intl.NumberFormat('en-US-u-nu-' + NU,", 1),
    ("const arDigits = s => String(s).replace(/[0-9]/g, d => AR[+d]);", "const arDigits = s => ar(s);", 1),
    # #digits=hindi still gives Arabic-Indic digits (the table makes ٠…٩ of the code 0…9: here they are not literals it sees)
    ("const AR = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];", "const AR = [" + ",".join(f"'\\u{0x660 + k:04x}'" for k in range(10)) + "];", 1),
    # «Monday, 22 Jumada al-Awwal 1448 AH»: the month transliterated, as the Gregorian line under it
    ("""    const hf = new Intl.DateTimeFormat('ar-SA-u-ca-islamic-umalqura-nu-' + NU,{
      timeZone: CONFIG.timezone, weekday:'long', day:'numeric', month:'long', year:'numeric'
    });
    let h;
    if(!CONFIG.hijri) h = hf.format(now);
    else{   // the day, month and year move; the weekday is today's
      const wd = hf.formatToParts(now).find(x => x.type === 'weekday');
      h = hf.formatToParts(hijriAt(now)).map(x => x.type === 'weekday' && wd ? wd.value : x.value).join('');
    }
    h = h.replace(/\\s*هـ\\s*$/,'').replace(/،\\s*$/,'');
    setText(document.getElementById('hijri'), h + ' هـ');""",
     """    const hp = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn',{timeZone: CONFIG.timezone, day:'numeric', month:'numeric', year:'numeric'})
      .formatToParts(hijriAt(now)).reduce((a, x) => (a[x.type] = x.value, a), {});
    const wd = EN_DAYS[enDay(now, CONFIG.timezone).wd];
    const hm = HIJRI_EN[parseInt(hp.month, 10) - 1];
    if(hm) setText(document.getElementById('hijri'), wd + ', ' + ar(parseInt(hp.day, 10)) + ' ' + hm + ' ' + ar(parseInt(hp.year, 10)) + ' AH');""", 1),
    ("if(nat) setText(nl, nat.name + ' ' + ar(nat.n) + nat.unit);", "if(nat) setText(nl, nat.name + ' · ' + ar(nat.n) + ' years');", 1),
    # one day, 2 days; «2 hours, 15 min» (the parts are joined with ' و ' → ', ', which the code also splits on)
    ("""const UNIT = {
  day: {one:'يوم', two:'يومين', few:'أيام', many:'يوم'},
  hour:{one:'ساعة', two:'ساعتين', few:'ساعات', many:'ساعة'},
  min: {one:'دقيقة', two:'دقيقتين', few:'دقائق', many:'دقيقة'}
};
function qty(n,u){
  if(n === 2) return u.two;
  if(n >= 3 && n <= 10) return `${ar(n)} ${u.few}`;
  return `${ar(n)} ${u.many}`;
}""", """const UNIT = {day: {one:'day', many:'days'}, hour: {one:'hour', many:'hours'}, min: {one:'min', many:'min'}};
const qty = (n, u) => `${ar(n)} ${n === 1 ? u.one : u.many}`;""", 1),
    # «Maghrib adhan in 8 min», «Maghrib iqama in 12 min», «Maghrib adhan · Any button stops the adhan»
    ("return (n ? 'أذان ' + n : 'الأذان') + '  ·  '", "return (n ? n + ' adhan' : 'Adhan') + '  ·  '", 1),
    ("return 'أذان ' + prayerName(x) + ' بعد ' + untilAdhan(toAdhan);", "return prayerName(x) + ' adhan in ' + untilAdhan(toAdhan);", 1),
    ("return 'إقامة ' + prayerName(x) + ' بعد ' + humanRemaining(", "return prayerName(x) + ' iqama in ' + humanRemaining(", 1),
    ("'الإقامة بعد <b>'", "'Iqama in <b>'", 1),
    ("'بعد <b>'", "'in <b>'", 3),
    ("'قبل <b>' + humanRemaining(Math.max(60, Math.floor(ago) * 60)) + '</b>'", "'<b>' + humanRemaining(Math.max(60, Math.floor(ago) * 60)) + '</b> ago'", 1),
    ("setHTML(elD, ar(oc.days) + ' <span class=\"unit\">' + OCC_TEXT.unit + '</span>');",
     "setHTML(elD, ar(oc.days) + ' <span class=\"unit\">' + (oc.days === 1 ? 'day' : OCC_TEXT.unit) + '</span>');", 1),
    ("""'<span class="num">' + ar(n) + '</span> <span class="unit">يوم</span>', name);""",
     """'<span class="num">' + ar(n) + '</span> <span class="unit">' + (n === 1 ? 'day' : 'days') + '</span>', name);""", 1),
    ("ramadanDay:n => 'اليوم ' + ar(n) + ' من رمضان',", "ramadanDay:n => 'Ramadan day ' + ar(n),", 1),
    # team and people's names as the data has them, in English (no Arabic table, no transliteration, no Wikidata lookup)
    ("const teamAr = (s, id) => arName(s, 'team', id);", "const teamAr = (s, id) => String(s || '').trim();", 1),
    ("function translit(en){\n", "function translit(en){ return String(en || ''); }\nfunction translitAr(en){\n", 1),
    ("'morocco':'المغرب'}", "'morocco':'Morocco'}", 1),
    # the games
    ("`<div class=\"sub\">النتيجة النهائية · ${day}</div></div>`", "`<div class=\"sub\">Final · ${day}</div></div>`", 1),
    ("`بعد ${shortRemaining(diff)}</div>", "`In ${shortRemaining(diff)}</div>", 1),
    ("`بدأت ${clk}` : `الدقيقة ${ar(m)}`", "`Started ${clk}` : `${ar(m)}'`", 1),
    ("· بدأ ${clk}`", "· started ${clk}`", 1),
    # «Saudi Arabian Grand Prix», short «Saudi Arabian GP»: the event's own name
    ("const gpName = gp ? 'جائزة ' + (GP_AR[place] || trl(gp[1].trim())) + ' الكبرى' : trl(name);",
     "const gpName = gp ? gp[1].replace(/\\bformula\\s*1\\b/i,'').replace(/\\b20\\d\\d\\b/,'').trim() + ' Grand Prix' : name;", 1),
    ("gpName.replace(/ الكبرى$/, '')", "gpName.replace(/ Grand Prix$/, ' GP')", 1),
    # the cards
    ("`الموسم ${ar(e.season)}` : `الموسم ${ar(e.season)} · ح ${ar(e.number)}`", "`Season ${ar(e.season)}` : `S${ar(e.season)} E${ar(e.number)}`", 1),
    ("`اليوم <b>${ar(day)}</b> من ${ar(total)} · ", "`Day <b>${ar(day)}</b> of ${ar(total)} · ", 1),
    ("${fmtTime(n.h, n.mi)}،</span><span class=\"r\">بعد ${left}</span>", "${fmtTime(n.h, n.mi)},</span><span class=\"r\">in ${left}</span>", 1),
    ("`${prayerName(n)} بعد ${shortHM(", "`${prayerName(n)} in ${shortHM(", 1),
    ("return m < 1 ? 'الآن' : m < 60 ? 'قبل ' + ar(m) + ' د' : 'قبل ' + ar(Math.floor(m / 60)) + ' س';",
     "return m < 1 ? 'now' : m < 60 ? ar(m) + ' min ago' : ar(Math.floor(m / 60)) + ' h ago';", 1),
]


def en_css(html):
    """The style block laid out left to right: every left is a right and back (text-align, left:, margin-left, …), the gold rules fade
    the other way, and the prayer bar fills from the left; the Arabic content stays right to left. What only centres (left:50% with translateX(-50%)) stays, and so does the gap
    between the clock's digits (the clock is left to right in both). The clock's AM/PM goes after the digits, as English writes it."""
    a, b = html.index("<style>"), html.index("</style>")
    keep = ["left:50%", "i + i{margin-left:"]
    css = html[a:b]
    for k, w in enumerate(keep):
        css = css.replace(w, f"\0{k}\0")
    css = re.sub(r"\b(left|right)\b", lambda m: "right" if m.group(1) == "left" else "left", css)
    css = css.replace("linear-gradient(90deg", "linear-gradient(270deg").replace("direction:rtl", "direction:ltr")
    for k, w in enumerate(keep):
        css = css.replace(f"\0{k}\0", w)
    css += "  .clock .ampm{order:1;margin-inline-end:0;margin-inline-start:.25em}\n"
    # the Arabic content keeps its own direction on the left-to-right page: the verse, hadith, dhikr and saying, their sources, the tafsir
    css += "  .week li.quote,.week li.src,.week li.tafsir,.week li.adf .s{direction:rtl}\n"
    return html[:a] + css + html[b:]


def english(template, table=None):
    """The template as the English page's source (still with the build tokens): EN_CODE, en_css, then every Arabic literal through the
    table. Raises when a piece of EN_CODE is not where it was, or a literal has no entry (tests/i18n_test.py says which)."""
    html = template
    for old, new, times in EN_CODE:
        if html.count(old) != times:
            raise SystemExit(f"src/build.py EN_CODE: found {html.count(old)} times, not {times}: {old[:80]!r}")
        html = html.replace(old, new)
    html = html.replace("const AR = [", "const HIJRI_EN = " + json.dumps(HIJRI_EN, separators=(",", ":")) + ";\nconst AR = [", 1)
    html = en_css(html)
    out, left = tr.translate(html, tr.load() if table is None else table)
    if left:
        raise SystemExit("i18n/en/page.tsv has no entry for: " + " | ".join(left[:10]))
    return out


def us_short_en(clubs):
    """us_short for the English page: «Los Angeles Lakers» → «Lakers»."""
    out = {}
    for c in clubs:
        en = c.get("en", "").split()
        if c.get("sport") not in US_SPORTS or len(en) < 2:
            continue
        nick = 2 if " ".join(en[-2:]) in NICK_TWO_WORDS else 1
        out[c["en"]] = " ".join(en[-nick:])
    return out


def build_stamp():
    """UTC YYYYMMDDHHMMSS of the newest commit that touched the page's sources (else of HEAD, else now): the same for every build of one
    commit, never going back as commits are added. URTV_BUILD overrides it. The app compares these, so a page older than its own is never used."""
    import os, subprocess, time
    if os.environ.get("URTV_BUILD", "").isdigit():
        return os.environ["URTV_BUILD"]
    for args in (["--", "src", "assets"], []):
        try:
            t = subprocess.run(["git", "log", "-1", "--format=%ct"] + args, cwd=ROOT, capture_output=True, text=True, timeout=30).stdout.strip()
        except Exception:
            t = ""
        if t.isdigit():
            return time.strftime("%Y%m%d%H%M%S", time.gmtime(int(t)))
    return time.strftime("%Y%m%d%H%M%S", time.gmtime())


def fill(html, clubs, english_page):
    """The build tokens of the data, the same for both languages but the team tables: the English page shows the English names."""
    # 2 KB, so inlined in both pages
    html = html.replace("__F_MARKS__", data_uri(ROOT / "assets/fonts/NotoNaskhArabic-Marks.woff2", "font/woff2"))
    if english_page:
        html = html.replace("__TEAMS_AR__", "{}").replace("__TEAMS_BY_ID__", "{}").replace("__TEAMS_SHORT__", js_json(us_short_en(clubs)))
    else:
        teams = json.loads((ROOT / "assets/shared/teams-ar.json").read_text(encoding="utf-8"))
        html = html.replace("__TEAMS_AR__", js_json(teams))
        # by TheSportsDB id: "Al-Faisaly" and "Al Faisaly" are different clubs under one name key
        by_id = {c["id"]: c["ar"] for c in clubs if c.get("id") and c.get("ar")}
        html = html.replace("__TEAMS_BY_ID__", js_json(by_id))
        html = html.replace("__TEAMS_SHORT__", js_json(us_short(clubs)))
    # leagues of Formula 1, UFC, boxing and tennis, found by CI (.github/tools/team_ids.py); {} until a run delivers them
    html = html.replace("__EVENTS__", js_json(json.loads((ROOT / "assets/shared/events.json").read_text(encoding="utf-8"))))
    # «مباريات اليوم»'s marks: the rows only (the page shows a row while it is high, whole, verified and in season; broadcast_check.py)
    html = html.replace("__BROADCAST__", js_json(json.loads((ROOT / "assets/shared/broadcast.json").read_text(encoding="utf-8"))["rows"]))
    html = html.replace("__VERSES__", js_json(verses_for_screen()))
    html = html.replace("__HADITH__", js_json(hadith_for_screen()))   # checked by CI
    html = html.replace("__ADHKAR__", js_json(adhkar_for_screen()))   # checked by CI
    html = html.replace("__WISDOM__", js_json(json.loads((ROOT / "assets/shared/wisdom.json").read_text(encoding="utf-8"))))
    return html


def main():
    template = (ROOT / "src/template.html").read_text(encoding="utf-8").replace("__BUILD__", build_stamp())
    clubs = json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))
    html = fill(template, clubs, False)
    html_en = fill(english(template), clubs, True)

    bg_dir = ROOT / "assets/backgrounds"
    bgs = json.loads((bg_dir / "backgrounds.json").read_text(encoding="utf-8"))
    for name, inline, src in (("urtv.html", True, html), ("tv.html", False, html), ("tv-en.html", False, html_en)):
        page = src
        for token, font in FONTS.items():
            path = ROOT / "assets/fonts" / font
            if inline and not path.is_file():   # Thmanyah comes from the private urtv-fonts repo (CI, or a copy you have):
                page = page.replace(token, "data:,")   # without it this page falls back to the system's Arabic font
            else:
                page = page.replace(token, data_uri(path, "font/woff2") if inline else REPO + "assets/fonts/" + font)
        entries = []
        for bg in bgs:
            u = data_uri(bg_dir / bg["file"], "image/jpeg") if inline else REPO + "assets/backgrounds/" + bg["file"]
            entries.append(f'  {{u:"{u}", c:{js_json(bg["credit"])}, d:{bg["brightness"]}}}')
        out = ROOT / "dist" / name
        out.parent.mkdir(exist_ok=True)
        out.write_text(page.replace("__BG_SET__", ",\n".join(entries)), encoding="utf-8")
        print(f"wrote {out.relative_to(ROOT)} ({out.stat().st_size / 1e6:.1f} MB)")

if __name__ == "__main__":
    main()
