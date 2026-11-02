#!/usr/bin/env python3
"""«اللغة», phase 4 (docs/ENGLISH.md): the phone page in English, android/launcher/src/main/assets/phone/index.en.html.

The Arabic android/launcher/src/main/assets/phone/index.html stays the source, byte for byte, but for the language link this generator
puts in it (LINK_AR): the link is never written by hand (the Arabic file has a few hundred bytes left of its 64 KB). The English copy is
that file with EN_CODE below (what a table cannot do: the English plurals, word order, the left-to-right style, the link the other way),
then every Arabic literal through i18n/tr.py with i18n/en/phone.tsv. PhoneHost serves the one «اللغة» asks for (Tr.phonePageName).

  python3 src/phone_en.py            writes both files (index.html only when its link is missing or old)
  python3 src/phone_en.py --check    writes nothing; exit 1 when either file is not what it would write (tests/i18n_phone_test.py)
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "i18n"))
import tr   # noqa: E402

PHONE = ROOT / "android/launcher/src/main/assets/phone"
AR_FILE, EN_FILE = PHONE / "index.html", PHONE / "index.en.html"

# ---- the language link, in the Arabic page too: (where it goes, what goes after it). Shown only by a TV that knows «اللغة» (state.lang);
# a press sets the TV's language (the same /api/set, PIN and all), and the TV then serves the page in it.
LINK_AR = [   # (anchor, what goes right after it) or, when the anchor starts a line, right before it
    ('<button id="theme" class="lnk">الوضع الفاتح</button>', '<button id="lang" class="lnk hidden" lang="en">English</button>'),
    ("    cards(s);\n", "    $('lang').classList.toggle('hidden', !s.lang);\n"),
    ("  // ---- the glass look", "  $('lang').onclick = function () { gated('/api/set', { lang: 'en' }).then(function (j) { if (j.ok) location.reload(); else say(j); }); };\n"),
]
piece = lambda anchor, add: add + anchor if anchor.startswith("  ") else anchor + add
LINK_EN = [
    ('<button id="lang" class="lnk hidden" lang="en">English</button>', '<button id="lang" class="lnk hidden" lang="ar">العربية</button>', 1),
    ("gated('/api/set', { lang: 'en' })", "gated('/api/set', { lang: 'ar' })", 1),
]


def with_link(html):
    """The Arabic page with the link: the source as it is in git, the link put in after its anchor (taken out first when it is there)."""
    for anchor, add in LINK_AR:
        html = html.replace(piece(anchor, add), anchor)
    for anchor, add in LINK_AR:
        if html.count(anchor) != 1:
            raise SystemExit(f"src/phone_en.py LINK_AR: found {html.count(anchor)} times, not once: {anchor[:60]!r}")
        html = html.replace(anchor, piece(anchor, add))
    return html


# ---- what the table cannot do: each pair is a piece of index.html exactly as it is there (the times it must be found), and what the English
# page has instead. A change to index.html that moves one stops the generator, so the English page never silently keeps an Arabic line.
EN_CODE = [
    # left to right: the select's arrow and the playing card's gold bar go to the other side; «back» and «send» point the other way
    ("padding-left:34px;background:var(--tile) linear-gradient(135deg,transparent 50%,var(--dim) 50%) 14px 50%/7px 7px no-repeat",
     "padding-right:34px;background:var(--tile) linear-gradient(135deg,transparent 50%,var(--dim) 50%) calc(100% - 14px) 50%/7px 7px no-repeat", 1),
    ("#now{display:flex;align-items:center;gap:12px;padding:12px 18px 12px 12px;", "#now{display:flex;align-items:center;gap:12px;padding:12px 12px 12px 18px;", 1),
    ("</style>", "[data-k=back] svg,#txtGo svg{transform:scaleX(-1)}\n</style>", 1),
    # a text with parentheses is no text node the hook sees
    ('<p>المؤذن (لغير الفجر)</p>', '<p>Muezzin (other prayers)</p>', 1),
    # a literal with a quote in it is no literal the hook sees (it would only see the service's name inside)
    ("""service_off: 'فعّل خدمة "تلفزيونك: التحكم والكتابة من الجوال" في إعدادات التلفزيون لتكتب في التطبيقات الأخرى'""",
     "service_off: 'To type in other apps, turn on the UrTV phone control service in the TV’s settings'", 1),
    # the English count: «1 hour», «2 hours», «11 min» (the Arabic one has a dual and two plurals; the table gives [one, two, few, many])
    ("function nn(n, w) { var r = n % 100; return n === 1 ? w[0] : n === 2 ? w[1] : n + ' ' + (r > 2 && r < 11 ? w[2] : w[3] || w[0]); }",
     "function nn(n, w) { return n + ' ' + (n === 1 ? w[0] : w[2]); }", 1),
    ("$('left').textContent = 'بقي ' + nn(Math.max(1, Math.round(secs / 60)), MN);",
     "$('left').textContent = nn(Math.max(1, Math.round(secs / 60)), MN) + ' left';", 1),
    ("toLocaleTimeString('ar-u-nu-latn',", "toLocaleTimeString('en-US',", 1),
    # «يعمل» is «Playing» on the player and «On» for kids mode
    ("'وضع الأطفال: ' + (fam.kids ? 'يعمل' : 'متوقف')", "'Kids mode: ' + (fam.kids ? 'On' : 'Off')", 1),
    # «Locked: 2 apps, 1 TV button»
    ("nn(a, ['تطبيق', 'تطبيقين', 'تطبيقات', 'تطبيقًا']) + ' و ' + pills + ' من أزرار التلفزيون'",
     "nn(a, ['app', '2 apps', 'apps']) + ', ' + nn(pills, ['TV button', '2 TV buttons', 'TV buttons'])", 1),
    # the TV sends the pills' Arabic names: the English page has its own
    ("(x.name || t[1])", "t[1]", 1),
] + LINK_EN


# An attribute just before «>» (aria-label="…">) is no literal the hook sees: attrs() gives it the table's English, or, for the words only
# such an attribute has (an entry there would be one the table test sees no use of), ATTR_EN's.
ATTR = re.compile(r'(\s[a-z-]+=")([^"<>\n]*[\u0600-\u06ff][^"<>\n]*)("\s*>)')
ATTR_EN = {
    "لوحة اللمس": "Touchpad", "الرمز": "Code", "نص للتلفزيون": "Text for the TV", "طريقة الكتابة": "Typing mode", "مدينة": "City",
    "مدة الخمول": "Idle time", "الأقسام": "Sections", "القارئ": "Reciter", "السورة": "Surah", "المؤذن": "Muezzin",
    "مستوى صوت الأذان": "Adhan volume",
}


def attrs(html, table):
    unused, missing = set(ATTR_EN), []

    def one(m):
        ar = m.group(2)
        unused.discard(ar)
        en = table.get(ar, ATTR_EN.get(ar))
        if en is None:
            missing.append(ar)
            return m.group(0)
        return m.group(1) + en + m.group(3)
    html = ATTR.sub(one, html)
    if missing or unused:
        raise SystemExit(f"src/phone_en.py ATTR_EN: no English for {missing[:5]}; not in the page: {sorted(unused)[:5]}")
    return html


def english(ar_html, table=None):
    """The English page from the Arabic one (with its link). Raises when a piece of EN_CODE is not where it was, or a literal has no entry."""
    html = ar_html
    for old, new, times in EN_CODE:
        if html.count(old) != times:
            raise SystemExit(f"src/phone_en.py EN_CODE: found {html.count(old)} times, not {times}: {old[:80]!r}")
        html = html.replace(old, new)
    table = tr.load() if table is None else table
    out, left = tr.translate(attrs(html, table), table)
    left = [a for a in left if a != "العربية"]   # the link back to Arabic says it in Arabic
    if left:
        raise SystemExit("i18n/en/phone.tsv has no entry for: " + " | ".join(left[:10]))
    return out


def main(check=False):
    source = AR_FILE.read_text(encoding="utf-8")
    ar = with_link(source)
    en = english(ar)
    old_en = EN_FILE.read_text(encoding="utf-8") if EN_FILE.exists() else None
    if check:
        bad = [p.name for p, new, old in ((AR_FILE, ar, source), (EN_FILE, en, old_en)) if new != old]
        if bad:
            print("not current (run python3 src/phone_en.py): " + ", ".join(bad))
            return 1
        print("index.html and index.en.html are current")
        return 0
    if ar != source:
        AR_FILE.write_bytes(ar.encode("utf-8"))
    EN_FILE.write_bytes(en.encode("utf-8"))
    print(f"index.html {len(ar.encode('utf-8'))} bytes, index.en.html {len(en.encode('utf-8'))} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv[1:]))
