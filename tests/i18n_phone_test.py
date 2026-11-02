#!/usr/bin/env python3
"""«اللغة», phase 4 (docs/ENGLISH.md): the phone page in English, android/launcher/src/main/assets/phone/index.en.html. src/phone_en.py writes
it from the Arabic index.html (EN_CODE, then i18n/en/phone.tsv); the language link goes in both only through it, and only with LINK on
(off until phases 1 and 3): off, the Arabic file is byte for byte what it was. PhoneHost serves the one «اللغة» asks for. The page in a browser: i18n_phone_test.mjs.
Run: python3 tests/i18n_phone_test.py"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "i18n"))
import phone_en   # noqa: E402
import tr         # noqa: E402

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

AR = re.compile("[؀-ۿ]")
ar_b, en_b = phone_en.AR_FILE.read_bytes(), phone_en.EN_FILE.read_bytes()
ar, en = ar_b.decode("utf-8"), en_b.decode("utf-8")

# ---- generated and current
r = subprocess.run([sys.executable, "src/phone_en.py", "--check"], cwd=ROOT, capture_output=True, text=True)
check("index.en.html and the link in index.html are current (run python3 src/phone_en.py after editing index.html or i18n/en/phone.tsv)",
      r.returncode == 0, (r.stdout + r.stderr)[-300:])

# ---- the language link: off (LINK) until phases 1 and 3 are in, and then only what the generator puts in
link_ar, link_en = phone_en.pages(True)
bare_ar, bare_en = phone_en.pages(False)
check(f"src/phone_en.py: LINK is {phone_en.LINK}, and both files are the pages {'with' if phone_en.LINK else 'without'} the link",
      (ar, en) == ((link_ar, link_en) if phone_en.LINK else (bare_ar, bare_en)))
check("the link off: index.html is the source with no trace of it (no id=\"lang\", no lang: in a request)",
      all('id="lang"' not in x and "$('lang')" not in x and "lang: '" not in x for x in (bare_ar, bare_en)))
check("the generator writes the same page twice (the link is put in once, taken out whole)",
      phone_en.with_link(link_ar, True) == link_ar and phone_en.with_link(link_ar, False) == bare_ar and phone_en.with_link(bare_ar, True) == link_ar)
for anchor, add in phone_en.LINK_AR:
    check(f"the link on: its piece is there once, by its anchor: {add.strip()[:50]}", link_ar.count(phone_en.piece(anchor, add)) == 1 and link_ar.count(add) == 1)
check("the link on: «English» on the Arabic page, hidden until the TV says it knows «اللغة» (state.lang)",
      '<button id="lang" class="lnk hidden" lang="en">English</button>' in link_ar and "$('lang').classList.toggle('hidden', !s.lang);" in link_ar)
check("the link on: it sets the TV's language through /api/set (PIN and all), then loads the page again",
      "$('lang').onclick = function () { gated('/api/set', { lang: 'en' }).then(function (j) { if (j.ok) location.reload(); else say(j); }); };" in link_ar)
check("the link on: «العربية» on the English page, setting lang ar", 'lang="ar">العربية</button>' in link_en and "gated('/api/set', { lang: 'ar' })" in link_en and "lang: 'en'" not in link_en)
check("the link on: both pages still under 64 KB", len(link_ar.encode()) < 64 * 1024 and len(link_en.encode()) < 64 * 1024, len(link_ar.encode()))
check("index.html: still lang=\"ar\" dir=\"rtl\"", '<html lang="ar" dir="rtl">' in ar[:200])

# ---- sizes and the page's own rules, for each file
for name, b, s in (("index.html", ar_b, ar), ("index.en.html", en_b, en)):
    check(f"{name}: under 64 KB ({len(b)} bytes)", len(b) < 64 * 1024, len(b))
    outside = s.replace(s[s.index("<script"):s.index("</script>")], "")
    check(f"{name}: no inline handler attributes, one nonce'd script, no external address",
          not re.search(r"\son[a-z]+\s*=", outside) and s.count("<script") == 1 and '<script nonce="__NONCE__">' in s and not re.search(r"https?://(?!github)", s))

# ---- the English page
check("index.en.html: <html lang=\"en\" dir=\"ltr\">", '<html lang="en" dir="ltr">' in en[:200])
check("index.en.html: the same ids as the Arabic page (the same code works on both)", re.findall(r'\sid="([^"]+)"', en) == re.findall(r'\sid="([^"]+)"', ar))
check("index.en.html: the same script but its words (the same number of lines)", en.count("\n") == ar.count("\n") + 1, (en.count("\n"), ar.count("\n")))
code = re.sub(r"/\*.*?\*/", "", en, flags=re.S)
code = "\n".join(re.sub(r"(^|\s)//.*$", "", line) for line in code.split("\n"))
allowed = ['lang="ar">العربية</button>', "/[٠-٩]/g", "/[۰-۹]/g"]   # the link back says it in Arabic; the PIN field takes Arabic-Indic digits
rest = code
for a in allowed:
    rest = rest.replace(a, "")
check("index.en.html: no Arabic outside comments, but the digits the PIN field reads (and «العربية» on the link back, when on)",
      not AR.search(rest), [line.strip()[:80] for line in rest.split("\n") if AR.search(line)][:5])
check("index.en.html: English counts (1 hour, 2 hours) and the session's time «25 min left»",
      "function nn(n, w) { return n + ' ' + (n === 1 ? w[0] : w[2]); }" in en and "nn(Math.max(1, Math.round(secs / 60)), MN) + ' left'" in en)
check("index.en.html: left to right where the style says a side: the select's arrow, the playing card's bar, back and send point the other way",
      "padding-right:34px" in en and "padding-left:34px" not in en and "padding:12px 12px 12px 18px" in en and "[data-k=back] svg,#txtGo svg{transform:scaleX(-1)}" in en)
check("index.en.html: the time of the next adhan in English (no Arabic AM/PM)", "toLocaleTimeString('en-US'," in en and "'ar-u-nu-latn'" not in en)
check("index.en.html: the pills' names are the page's own (the TV sends them in Arabic)", "(x.name || t[1])" in ar and "(x.name || t[1])" not in en)
check("index.en.html: the TV's own words, untranslated: Quran and adhan names come as data", "s.surahs[p.surah - 1]" in en and "na.prayer" in en)

# ---- the table: every entry safe in a page, the English has no Arabic, each Arabic of the page has one (tests/i18n_test.py checks it too)
rows = tr.parse((ROOT / "i18n/en/phone.tsv").read_text(encoding="utf-8"), "phone.tsv")
found = {a for _, _, a in tr.arabic_literals(ar)}
check(f"phone.tsv: {len(rows)} entries, each for an Arabic literal of index.html", rows and all(a in found for _, a, _ in rows), [a for _, a, _ in rows if a not in found][:5])
check("phone.tsv: every English is safe in JS quotes and HTML, and has no Arabic",
      all(not (set(e) & tr.UNSAFE) and not AR.search(e) for _, _, e in rows), [e for _, _, e in rows if set(e) & tr.UNSAFE or AR.search(e)][:5])
check("phone.tsv: the labels of an element that ends its tag (aria-label=\"…\">) have their entries: the hook sees them",
      all(a in found for a in ("لوحة اللمس", "الأقسام", "مدة الخمول")) and all(a in {r[1] for r in rows} for a in ("لوحة اللمس", "الأقسام", "مدة الخمول")))

# ---- the TV serves the page of «اللغة»
host = (ROOT / "android/launcher/src/main/java/sa/urtv/home/phone/PhoneHost.java").read_text(encoding="utf-8")
acts = (ROOT / "android/launcher/src/main/java/sa/urtv/home/phone/PhoneActions.java").read_text(encoding="utf-8")
check("PhoneHost: the page by Tr.phonePageName, asked of the screensaver at each load (PhoneActions.lang)",
      "return page(app, actions.lang());" in host and "sa.urtv.home.Tr.phonePageName(lang)" in host)
check("PhoneHost: an English file missing from the APK gives the Arabic one; each file is read once",
      ": page(c, sa.urtv.home.Tr.AR);" in host and "if (!pages.containsKey(name))" in host and "pageText" not in host)
check("PhoneActions.lang: the screensaver's phone_state now (no cached snapshot), the home's kept value when it does not answer",
      re.search(r'String lang\(\) \{\s*Bundle b = saver\("phone_state", null, null\);\s*return b == null \? sa\.urtv\.home\.S\.lang\(\) : sa\.urtv\.home\.Tr\.norm\(b\.getString\("lang"\)\);', acts) is not None)

print(f"\n{'all passed' if not failed else f'{failed} FAILED'}")
sys.exit(1 if failed else 0)
