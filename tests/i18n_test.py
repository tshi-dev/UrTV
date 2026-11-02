#!/usr/bin/env python3
"""«اللغة» (docs/ENGLISH.md): the English interface's plumbing and its completeness.

1. The table (i18n/en/*.tsv): every line «arabic<TAB>english», no two entries that disagree, the same placeholders on both sides.
2. Completeness: every S.t("…") / S.n("…") literal in both apps has an entry, and every entry is used. S.t takes one plain literal
   first, so nothing escapes this check. The page and the phone page (HTML): once their area's table has an entry, every Arabic literal
   and text node translate() finds has one («=» for one that stays Arabic, as a verse or a club's name); an entry a page uses is safe in
   JS and HTML.
3. Arabic is byte for byte what it was: Tr.t with ar gives each literal back (tests/TrRun.java), the hook never writes the Arabic file
   (it only returns the English copy), the fragment carries nothing for ar, S.wrap gives the screen its
   own context back, and S never reads the table for ar.
4. The roads the setting travels, read from the source: Prefs, the receiver (optimize/saver.sh lang), the ConfigProvider (the home and the
   phone), ConfigSnapshot and Digits on the home's side, the phone API, the table copied into both APKs.
The JVM part needs javac; without it, skipped with a note.   Run: python3 tests/i18n_test.py"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "i18n"))
import tr  # noqa: E402

APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


def escape(s):
    return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")


# ---- 1. the table
where = {}
try:
    table = tr.load(where=where)
    check(f"the table loads: {len(table)} entries in i18n/en/*.tsv, one tab per line, no two that disagree", True)
except tr.Bad as e:
    table = {}
    check("the table loads", False, e)
files = sorted(p.name for p in (ROOT / "i18n/en").glob("*.tsv"))
check("one table file per area, so the phases never edit the same file", files == ["home.tsv", "page.tsv", "phone.tsv", "saver.tsv", "setup.tsv"], files)
for f in (ROOT / "i18n/en").glob("*.tsv"):
    raw = f.read_bytes()
    check(f"{f.name}: UTF-8, LF line ends, no BOM, ends with a line break", not raw.startswith(b"\xef\xbb\xbf") and b"\r" not in raw and raw.endswith(b"\n"))
bad_ph = [ar for ar, en in table.items() if tr.placeholders(ar) != tr.placeholders(en)]
check("every entry has the same placeholders in both languages (%s, %1$s…)", not bad_ph, bad_ph[:5])


def kept_arabic(directory=ROOT / "i18n/en"):
    """The Arabic of the entries written «=» (docs/ENGLISH.md rule 6: this one stays Arabic, as a verse or a club's name)."""
    kept = set()
    for f in Path(directory).glob("*.tsv"):
        for line in f.read_text(encoding="utf-8").split("\n"):
            parts = line.rstrip("\r").split("\t")
            if not line.startswith("#") and len(parts) == 2 and parts[1] == "=":
                kept.add(tr.unescape(parts[0]))
    return kept


def untranslated_of(tab, kept):
    """Entries whose English has Arabic letters but for the «=» ones, which keep the Arabic on purpose."""
    return [ar for ar, en in tab.items() if re.search(r"[\u0600-\u06ff]", en) and ar not in kept]


untranslated = untranslated_of(table, kept_arabic())
check("no English side has Arabic letters (but an entry written «=», which keeps the Arabic on purpose)", not untranslated, untranslated[:5])

def _try(f):
    """True when f raises tr.Bad"""
    try:
        f()
        return False
    except tr.Bad:
        return True


two = tempfile.mkdtemp()
Path(two, "a.tsv").write_text("حفظ\tSave\n", encoding="utf-8")
Path(two, "b.tsv").write_text("حفظ\tKeep\n", encoding="utf-8")
check("the loader refuses a line without exactly one tab or with an empty side, and two files that disagree",
      _try(lambda: tr.load(two)) and _try(lambda: tr.parse("بلا تبويب\n")) and _try(lambda: tr.parse("أ\tب\tج\n")) and _try(lambda: tr.parse("\tSave\n"))
      and _try(lambda: tr.parse("حفظ\t\n")) and not _try(lambda: tr.parse("# تعليق\n\nحفظ\tSave\n")))
def _eq_dir():
    d = tempfile.mkdtemp()
    Path(d, "a.tsv").write_text("الهلال\t=\n", encoding="utf-8")
    return d


same = tempfile.mkdtemp()
Path(same, "a.tsv").write_text("حفظ\tSave\n", encoding="utf-8")
Path(same, "b.tsv").write_text("حفظ\tSave\n", encoding="utf-8")
check("…but the same entry in two files is fine (two phases may need the same word)", tr.load(same) == {"حفظ": "Save"})


# ---- 2. completeness
used = {}   # arabic -> first place
java_files = sorted(APP.rglob("*.java")) + sorted(HOME.rglob("*.java"))
literals = []
for f in java_files:
    try:
        for line, ar in tr.java_literals(text(f), f.name):
            literals.append(ar)
            used.setdefault(ar, f"{f.relative_to(ROOT)}:{line}")
    except tr.Bad as e:
        check("S.t( is always followed by one plain literal", False, e)
check(f"the S.t literals of both apps are read ({len(literals)} calls)", True)

# the HTML files with their own English copy (docs/ENGLISH.md): the Arabic file, its area's table, the English file the area's generator writes
HTML = [("page", ROOT / "src/template.html", "dist/tv-en.html"),
        ("phone", ROOT / "android/launcher/src/main/assets/phone/index.html", "android/launcher/src/main/assets/phone/index.en.html"),
        ("phone", ROOT / "android/launcher/src/main/assets/phone/portfolio.html", "android/launcher/src/main/assets/phone/portfolio.en.html")]
page_words = set()
for area, f, _ in HTML:
    found = [ar for _, _, ar in tr.arabic_literals(text(f))]
    page_words.update(found)
    for ar in found:
        used.setdefault(ar, str(f.relative_to(ROOT)))
    area_rows = tr.parse(text(ROOT / f"i18n/en/{area}.tsv"), f"{area}.tsv")
    _, left = tr.translate(text(f), table)
    if area_rows:
        check(f"{area}: started, so every Arabic literal and text node of {f.relative_to(ROOT)} has an entry («=» to keep it Arabic)", not left, left[:10])
    else:
        print(f"PASS {area}: not started (i18n/en/{area}.tsv is empty): {len(left)} Arabic literals and text nodes wait for it")
# the XML strings (res/values/strings.xml): never a values-en/ (a TV whose system language is English would then show English to every Arabic
# home); the screens show them through S.tv(getString(…)), so once the app's area has started each one needs an entry
for mod, area in (("app", "saver"), ("launcher", "home")):
    res = ROOT / f"android/{mod}/src/main/res"
    qualified = sorted(p.name for p in res.iterdir() if re.match(r"values-(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\+[a-zA-Z+]+)(?:-|$)", p.name))
    check(f"{mod}: no language folder in res/ (values-en would follow the TV's system language, not «اللغة»)", not qualified, qualified)
    xml = text(res / "values/strings.xml")
    strings = [re.sub(r"\\(['\"@?])", r"\1", v).replace("\\n", "\n") for v in re.findall(r"<string name=\"[^\"]+\"[^>]*>(.*?)</string>", xml, re.S)]
    strings = [v for v in strings if re.search(f"[{tr.AR}]", v)]
    for v in strings:
        used.setdefault(v, f"android/{mod}/src/main/res/values/strings.xml")
    if tr.parse(text(ROOT / f"i18n/en/{area}.tsv"), area):
        miss = [v for v in strings if v not in table]
        check(f"{mod}: started, so every Arabic string of res/values/strings.xml has an entry", not miss, miss[:5])
    else:
        print(f"PASS {mod}: not started: its {len(strings)} Arabic XML strings wait for i18n/en/{area}.tsv")
xml_words = set()
for mod in ("app", "launcher"):
    xml = text(ROOT / f"android/{mod}/src/main/res/values/strings.xml")
    xml_words.update(re.sub(r"\\(['\"@?])", r"\1", v).replace("\\n", "\n") for v in re.findall(r"<string name=\"[^\"]+\"[^>]*>(.*?)</string>", xml, re.S))
missing = [f"{where_} «{ar}»" for ar, where_ in used.items() if ar not in table and ar not in page_words and ar not in xml_words]
check("every S.t / S.n literal has an English entry (add it to the area's i18n/en/*.tsv)", not missing, missing[:10])
unused = [f"{where[ar]} «{ar}»" for ar in table if ar not in used]
check("every entry is used by a Java literal or an Arabic literal of a page (remove the ones whose text left the code)", not unused, unused[:10])
unsafe = [ar for ar in page_words if ar in table and set(table[ar]) & tr.UNSAFE]
check("an entry a page uses has no ' \" ` < > & backslash or line break (’ for an apostrophe)", not unsafe, unsafe[:5])

# the extractor itself
src = 'class A { void f() { /* S.t("في تعليق") */ // S.t("سطر تعليق")\n a(S.t("حفظ")); b(S.t( "بقي %s دقيقة" , n)); c(sa.urtv.home.S.t("سطر\\nثان")); d("S.t(\\"ليس نداء\\")"); e(S.n("هندية")); f(S.tv(label)); } }'
check("the extractor: S.t and S.n calls in code, comments skipped, escapes read, a string that only looks like a call skipped, S.tv not read",
      [ar for _, ar in tr.java_literals(src)] == ["حفظ", "بقي %s دقيقة", "سطر\nثان", "هندية"], tr.java_literals(src))
check("the extractor refuses S.t(variable) and S.t(\"a\" + b)", _try(lambda: tr.java_literals('x(S.t(name));')) and _try(lambda: tr.java_literals('x(S.t("أ" + b));')))

# ---- 3. Arabic stays byte for byte
for area, f, _ in HTML:
    t = text(f)
    before = f.read_bytes()
    english, _ = tr.translate(t, table)
    check(f"{f.relative_to(ROOT)}: the hook only returns the English copy, the Arabic file stays byte for byte", f.read_bytes() == before)
    english0, left0 = tr.translate(t, {})
    check(f"{f.relative_to(ROOT)}: with no entry the copy is the file but its <html lang dir> (nothing is dropped or doubled)",
          english0 == t.replace('<html lang="ar" dir="rtl">', '<html lang="en" dir="ltr">', 1))
    every = {ar: "W" for ar in left0}
    english1, left1 = tr.translate(t, every)
    check(f"{f.relative_to(ROOT)}: with an entry for each, nothing Arabic is left of what it found", not left1, left1[:5])
sample = '<html lang="ar" dir="rtl"><b>حفظ</b> <i> بقي %s دقيقة </i><script>x(\'حفظ\'); y("غير مترجم"); // the page’s \'نص\'\nz(`مرحبا ${n}`); w = a>b ? \'نعم\' : 0;</script>'
eng, left = tr.translate(sample, {"حفظ": "Save", "بقي %s دقيقة": "%s min left", "نعم": "Yes", "نص": "Text"})
check("the hook: whole literals and text nodes, <html> to en/ltr, a template with ${} left alone, the missing ones listed",
      eng == '<html lang="en" dir="ltr"><b>Save</b> <i> %s min left </i><script>x(\'Save\'); y("غير مترجم"); // the page’s \'Text\'\nz(`مرحبا ${n}`); w = a>b ? \'Yes\' : 0;</script>'
      and left == ["غير مترجم"], (eng, left))
eng, left = tr.translate('<input aria-label="بحث"><b data-x="نعم">نص</b><i title="حفظ" >x</i>', {"بحث": "Search", "نعم": "Yes", "نص": "Text", "حفظ": "Save"})
check("the hook: an attribute that ends its tag (aria-label=\"…\">) is a whole literal too, with or without a space before «>»",
      eng == '<input aria-label="Search"><b data-x="Yes">Text</b><i title="Save" >x</i>' and not left, (eng, left))
check("«=» keeps a word Arabic", tr.load(_eq_dir()) == {"الهلال": "الهلال"})
_mixed = tempfile.mkdtemp()
Path(_mixed, "a.tsv").write_text("الهلال\t=\nحفظ\tحفظ\nنعم\tYes نعم\nتم\tDone\n", encoding="utf-8")
check("«=» counts as kept Arabic for the «no Arabic on the English side» check; Arabic written out on the English side still fails it",
      kept_arabic(_mixed) == {"الهلال"} and sorted(untranslated_of(tr.load(_mixed), kept_arabic(_mixed))) == sorted(["حفظ", "نعم"]))

prefs = text(APP / "Prefs.java")
check("Prefs: «اللغة» is a pref, ar by default, normalised; setting it tells S",
      'String lang() { return Tr.norm(sp.getString("lang", Tr.AR)); }' in prefs and 'void setLang(String v) { sp.edit().putString("lang", Tr.norm(v)).apply(); S.set(v); }' in prefs)
check("the fragment gets Tr.fragment(lang()), which is empty for ar: the page, the previews and the home get the same link as before",
      prefs.count("Tr.fragment(lang())") == 1 and "+ Tr.fragment(lang())" in prefs[prefs.index("String fragment() {"):])
for app, s_src in (("screensaver", text(APP / "S.java")), ("home", text(HOME / "S.java"))):
    check(f"{app} S: t() passes no table for ar, and wrap() gives the screen its own context for ar",
          "Tr.t(lang, Tr.EN.equals(lang) ? table() : null, ar, args)" in s_src and "if (!en()) return base;" in s_src)
    check(f"{app} S: the table is the assets' i18n/en/*.tsv, read once, never a crash", 'list("i18n/en")' in s_src and 'open("i18n/en/" + f)' in s_src
          and "catch (java.io.IOException | RuntimeException ignored)" in s_src)


def same_but_package(a, b, publics):
    a = a.replace("package sa.urtv.screensaver;", "package sa.urtv.home;")
    for word in publics:
        a = a.replace(f"\nfinal class {word} {{", f"\npublic final class {word} {{")
    a = re.sub(r"\n    static ", "\n    public static ", a)
    return a == b


for name in ("Tr", "Dir"):
    check(f"{name}.java is the same in both apps but its package and public", same_but_package(text(APP / f"{name}.java"), text(HOME / f"{name}.java"), [name]))
d = text(APP / "Dir.java")
check("Dir: with Arabic the very constants the screens hard-code today (RIGHT, LEFT, RTL, D-pad left forward)",
      "rtl ? Gravity.RIGHT : Gravity.LEFT" in d and "rtl ? Gravity.LEFT : Gravity.RIGHT" in d and "rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR" in d
      and "rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR" in d and "rtl ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT" in d
      and "static boolean rtl(String lang) { return !Tr.EN.equals(lang); }" in d)

# ---- 4. the roads
rcv = text(APP / "SaverReceiver.java")
check("the receiver: lang is checked (ar or en, nothing half-saved), set and shown",
      'if (lang != null && !Tr.valid(lang))' in rcv and 'return "error: lang is one of ar en";' in rcv and "if (lang != null) p.setLang(lang);" in rcv
      and '+ "\\nlang: " + p.lang()' in rcv and "[--es lang ar|en]" in rcv
      and rcv.index('return "error: lang is one of ar en";') < rcv.index("if (lang != null) p.setLang(lang);"))
cp = text(APP / "ConfigProvider.java")
check("the ConfigProvider: the phone reads lang and sets it through the receiver's own checks",
      'out.putString("lang", p.lang());' in cp and 'new String[] {"city", "home_panel", "idle", "lang"}' in cp)
snap = text(HOME / "ConfigSnapshot.java")
dig = text(HOME / "Digits.java")
check("the home: ConfigSnapshot reads lang from the fragment, Digits passes every snapshot to S, which keeps it for the next start",
      "String lang() { return Tr.fromFragment(fragment); }" in snap and "S.remember(c, snap);" in dig
      and dig.index("S.remember(c, snap);") < dig.index("if (snap == null || snap.fragment == null) return;")
      and '"urtv_lang"' in text(HOME / "S.java"))
schema = text(HOME / "phone/Schema.java")
acts = text(HOME / "phone/PhoneActions.java")
check("the phone API: set takes lang ar or en, state says it",
      '"saver_idle", "lang", "pin");' in schema and 'case "lang": if (!"ar".equals(v) && !"en".equals(v)) return no("bad lang"); break;' in schema
      and 'extras.putString("lang", (String) v.get("lang"));' in acts and 'm.put("lang", sa.urtv.home.Tr.norm(b.getString("lang")));' in acts)
for mod in ("app", "launcher"):
    g = text(ROOT / f"android/{mod}/build.gradle")
    check(f"{mod}: the table goes into the APK as assets/i18n/en/", "def copyI18n = tasks.register('copyI18n', Copy) {" in g and "from('../../i18n') { include 'en/*.tsv' }" in g
          and 'into "$buildDir/generated/i18n/i18n"' in g and "preBuild.dependsOn copyI18n" in g and '"$buildDir/generated/i18n"] }' in g)
doc = text(ROOT / "docs/ENGLISH.md")
check("docs/ENGLISH.md names every table file and the commands", all(f"i18n/en/{f}" in doc for f in files) and "python3 tests/i18n_test.py" in doc)

# optimize/saver.sh lang: one quoted extra, anything else refused with nothing sent
tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
Path(fake).write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)


def saver(*a):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *a], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()


SET = "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET "
for v in ("ar", "en"):
    rc, out, sent = saver("lang", v)
    check(f"saver.sh lang {v}: one quoted extra", rc == 0 and sent == SET + f"--es lang '{v}'", f"rc={rc} sent={sent!r} {out}")
for words in (["lang"], ["lang", ""], ["lang", "EN"], ["lang", "fr"], ["lang", "en;reboot"], ["lang", "it's"]):
    rc, out, sent = saver(*words)
    check(f"saver.sh {words!r}: refused, nothing sent", rc != 0 and not sent and "usage: optimize/saver.sh lang ar|en" in out, f"rc={rc} sent={sent!r} {out}")

# ---- the JVM part: Tr.java's rules, every literal back byte for byte with ar, and the placeholders the same as the Python side's
if not shutil.which("javac"):
    print("SKIP i18n JVM part: no javac here")
    sys.exit(1 if failed else 0)
cases = ["حفظ", "بقي %s دقيقة", "من %s إلى %s", "%2$s and %1$s", "%s%% x %s", "٥٠%", "a %3$s", "%", "%%s", "%1$s %1$s", "%9$s", "%10$s", "x\\ny %s"]
cases += [escape(ar) for ar in table] + [escape(ar) for ar in table.values()]
with tempfile.TemporaryDirectory() as d:
    lit_file, case_file = Path(d, "lits.txt"), Path(d, "cases.txt")
    lit_file.write_text("".join(escape(s) + "\n" for s in literals), encoding="utf-8")
    case_file.write_text("".join(c + "\n" for c in cases), encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", d, str(APP / "Tr.java"), str(ROOT / "tests/TrRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", d, "sa.urtv.screensaver.TrRun", str(lit_file), str(case_file)], capture_output=True)
out = run.stdout.decode("utf-8")
print("".join(l + "\n" for l in out.splitlines() if not l.startswith("P\t")), end="")
diff = []
for l in out.splitlines():
    if l.startswith("P\t"):
        _, c, p = l.split("\t")
        want = ",".join(map(str, tr.placeholders(tr.unescape(c))))
        if p != want:
            diff.append(f"{c}: java {p} python {want}")
check("Java and Python read the same placeholders", not diff and out.count("\nP\t") + out.startswith("P\t") >= len(cases) - 1, diff[:5] or run.stderr.decode()[-500:])
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
