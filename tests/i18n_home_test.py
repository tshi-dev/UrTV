#!/usr/bin/env python3
"""«اللغة» phase 3, the home app (docs/ENGLISH.md): the home, «مركز التحكم», «المهام», «الرقابة الأبوية», «تلاوة وأذان» and the other
screens of android/launcher but its setup/ and phone/ ones.
1. No word left behind: every Arabic literal of these sources is said through S.t / S.n, or has an entry a screen says it by (S.tv on a
   name, a constant, an XML string), or is listed below as one that stays Arabic on purpose (data, the words a parser reads, Arabic-only
   code). A new Arabic literal fails here until it is one of the three.
2. Arabic is what it was: the plain-Java classes, run on a JVM in Arabic, give exactly tests/i18n_home_cases.tsv (written from the sources
   before «اللغة»); run in English (tests/i18n_home_stub/S.java with the real table) not one Arabic letter is left.
3. The screens: every Activity takes «اللغة» (attachBaseContext → S.wrap), the layout screens of «تلاوة وأذان» turn left to right and say
   their layout's words (Ui.localize), no side is written as a constant (Dir), the services and receivers that speak call S.init.
4. The Quran's names: every surah, reciter and station has an English name ("en", Latin letters) beside its Arabic one, which stays the key.
The JVM part needs javac.   Run: python3 tests/i18n_home_test.py"""
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "i18n"))
import tr  # noqa: E402

HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def phase3(p):
    rel = p.relative_to(HOME).as_posix()
    return not rel.startswith(("phone/", "setup/")) and rel not in ("HomeCardChoice.java", "S.java", "Tr.java", "Dir.java")


SOURCES = sorted(p for p in HOME.rglob("*.java") if phase3(p))
ARABIC = re.compile(f"[{tr.AR}]")
LIT = re.compile(r'"((?:[^"\\\n]|\\.)*)"')

# The Arabic literals that stay Arabic, by file, and why. Anything else Arabic must be said in «اللغة».
KEEP = {
    "MonthNames.java": "the Arabic month names the Arabic date line uses (and its Levant spellings); English writes its own",
    "OnOff.java": "the state words OnOff reads in a row's Arabic line (the screens keep the Arabic line for it, and show it with S.tv)",
    "TaskVoice.java": "the Arabic commands the voice adding understands (the recognizer listens in Arabic)",
    "GlassStyle.java": "the looks' names the screensaver's settings show (its own copy of GlassStyle: phase 1)",
    "tilawah/Adhan.java": "the muezzins' and places' names in Arabic: data, with NAMES_EN / PLACES_EN beside them",
    "ClockZone.java": "the prayer calculations' names: the page's words (i18n/en/page.tsv), said with S.tv in label()",
}
KEEP_LINES = {
    ("MainActivity.java", "EEEE، d MMMM y"): "the Arabic Hijri date's pattern: English has englishDate",
    ("MainActivity.java", r"\s*هـ\s*$"): "the Arabic Hijri date's era mark, taken off and put back: English has englishDate",
    ("TasksActivity.java", "أضف مهمة … / خلصت … / احذف …"): "the recognizer's prompt: the voice adding listens in Arabic",
    ("tilawah/PrayerTimes.java", " ساعات"): "the Arabic count of hours: English counts in its own branch",
    ("tilawah/PrayerTimes.java", " ساعة"): "the same",
    ("tilawah/PrayerTimes.java", " دقائق"): "the Arabic count of minutes: English counts in its own branch",
    ("tilawah/PrayerTimes.java", " دقيقة"): "the same",
    ("Dock.java", "hidden with dock.sh hide or the × of «تعديل التطبيقات»"): "a line for the computer's log",
    ("DockReceiver.java", "error: add --es names 'pkg|الاسم;pkg2|الاسم'"): "an answer for the computer (optimize/dock.sh)",
}

# ---- 1. no word left behind
try:
    table = tr.load()
except tr.Bad as e:
    table = {}
    check("the table loads", False, e)
left = []
for p in SOURCES:
    rel = p.relative_to(HOME).as_posix()
    code = tr.strip_comments(p.read_text(encoding="utf-8"))
    for m in LIT.finditer(code):
        s = tr.java_unescape(m.group(1))
        if not ARABIC.search(s) or rel in KEEP or (rel, s) in KEEP_LINES:
            continue
        if re.search(r"\bS\.[tn]\(\s*$", code[max(0, m.start() - 8):m.start()]) or s in table:
            continue
        left.append(f"{rel}:{code.count(chr(10), 0, m.start()) + 1} «{s}»")
check(f"every Arabic literal of the {len(SOURCES)} home sources is said in «اللغة» (S.t, S.n, an entry a screen says it by) or kept on purpose",
      not left, left[:10])
for rel, why in KEEP.items():
    check(f"kept Arabic, {rel}: {why}", (HOME / rel).exists())
gone = [k for k in KEEP_LINES if k[1] not in {tr.java_unescape(m.group(1)) for m in LIT.finditer(tr.strip_comments((HOME / k[0]).read_text(encoding="utf-8")))}]
check("every literal kept on purpose is still in its file (else take it off the list)", not gone, gone)
home_rows = tr.parse((ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8"), "home.tsv")
check(f"i18n/en/home.tsv: {len(home_rows)} entries, none of them written «=» (the home has no word that stays Arabic in English)",
      len(home_rows) > 300 and all(ar != en for _, ar, en in home_rows))

# ---- 3. the screens
acts = [p for p in SOURCES if re.search(r"class \w+ extends (android\.app\.)?Activity\b", p.read_text(encoding="utf-8"))]
no_wrap = [p.name for p in acts if not re.search(r"protected void attachBaseContext\(android\.content\.Context b\) \{ super\.attachBaseContext\((sa\.urtv\.home\.)?S\.wrap\(b\)\); \}",
                                                p.read_text(encoding="utf-8"))]
check(f"every Activity ({len(acts)}) takes «اللغة»: attachBaseContext(S.wrap(b)), which is the context itself in Arabic", not no_wrap and len(acts) >= 14, no_wrap)
for f in ("MainActivity", "SurahActivity", "AdhanActivity", "PlayerActivity"):
    s = (HOME / f"tilawah/{f}.java").read_text(encoding="utf-8")
    m = re.search(r"setContentView\(R\.layout\.\w+\);\n\s*Ui\.localize\(this\);", s)
    check(f"tilawah/{f}: the layout's words and direction follow «اللغة» right after setContentView (Ui.localize)", bool(m))
ui = (HOME / "tilawah/Ui.java").read_text(encoding="utf-8")
check("Ui.localize, say, str and line do nothing in Arabic (each returns at once, or is getString itself)",
      "if (!sa.urtv.home.S.en()) return;" in ui and "if (!sa.urtv.home.S.en() || v == null) return;" in ui
      and "if (!sa.urtv.home.S.en()) return args.length == 0 ? c.getString(id) : c.getString(id, args);" in ui
      and "if (s == null || !sa.urtv.home.S.en()) return s;" in ui)
sides = [f"{p.relative_to(HOME)}:{n}" for p in SOURCES for n, l in enumerate(p.read_text(encoding="utf-8").split("\n"), 1)
         if re.search(r"Gravity\.(RIGHT|LEFT)\b|View\.(LAYOUT|TEXT)_DIRECTION_RTL\)", l) and "getLayoutDirection()" not in l and "Dir.rtl()" not in l]
check("no side is written as a constant: Gravity.RIGHT/LEFT and a forced right-to-left go through Dir (the home's two reads of a row's own direction, and a side chosen by Dir.rtl(), stay)",
      not sides, sides[:10])
for f in ("tilawah/MusicService.java", "tilawah/AdhanNotice.java"):
    check(f"{f}: speaks outside an Activity, so it reads «اللغة» first (S.init)", "sa.urtv.home.S.init(" in (HOME / f).read_text(encoding="utf-8"))
parental = (HOME / "Parental.java").read_text(encoding="utf-8")
check("the PIN prompt says the locked thing's name in «اللغة» (the screens pass their Arabic words)", 'S.t("«%s» مقفل بالرمز الأبوي", S.tv(name))' in parental)
cat = (HOME / "tilawah/Catalog.java").read_text(encoding="utf-8")
check("a station is still found by its Arabic name (the computer's and the saved choice's key) whatever «اللغة» shows",
      "st.key = s.getString(\"name\");" in cat and "stations.get(i).key.equals(name)" in cat)

# ---- 4. the Quran's names
Q = ROOT / "android/launcher/src/main/assets/quran"
latin = re.compile(r"^[A-Za-z][A-Za-z' ,.-]*[A-Za-z0-9]$")
for name, key in (("surahs.json", "surahs"), ("reciters.json", "reciters"), ("stations.json", "stations")):
    rows = json.loads((Q / name).read_text(encoding="utf-8"))[key]
    bad = [r.get("name") for r in rows if not latin.match(r.get("en", ""))]
    check(f"{name}: all {len(rows)} have an English name in Latin letters beside the Arabic one", not bad, bad[:5])
sur = json.loads((Q / "surahs.json").read_text(encoding="utf-8"))["surahs"]
check("the surahs' English names are their usual transliterations (spot check)", sur[0]["en"] == "Al-Fatihah" and sur[1]["en"] == "Al-Baqarah"
      and sur[35]["en"] == "Ya-Sin" and sur[111]["en"] == "Al-Ikhlas" and sur[113]["en"] == "An-Nas" and len({s["en"] for s in sur}) == 114)

# ---- 2. Arabic as it was, English with no Arabic: the plain-Java classes on a JVM
if not shutil.which("javac"):
    print("SKIP the words on a JVM: no javac here")
    sys.exit(1 if failed else 0)
PURE = ["ControlText", "PillText", "DigitsText", "GreetingText", "ClockText", "AdhanBarText", "ClockZone", "TvRowsPick", "InputDisplay",
        "InputNames", "MonthNames", "FamilyDns", "Tr", "tilawah/NowPlayingText", "tilawah/AdhanNoticeText", "tilawah/PlayerFault",
        "tilawah/OwnAdhanText"]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java")] + [str(HOME / f"{f}.java") for f in PURE]
                   + [str(ROOT / "tests/I18nHomeRun.java"), str(ROOT / "tests/I18nHomeTilawahRun.java")], check=True, capture_output=True)

    def run(*props):
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", *props, "-cp", tmp, "sa.urtv.home.I18nHomeRun"],
                             check=True, capture_output=True).stdout.decode("utf-8")
        return dict(l.split("\t", 1) for l in out.split("\n") if l)

    ar = run()
    en = run("-Durtv.lang=en", f"-Durtv.i18n={ROOT / 'i18n/en'}")
cases = dict(l.split("\t", 1) for l in (ROOT / "tests/i18n_home_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#"))
differs = [f"{k}: {ar.get(k)!r} (was {v!r})" for k, v in cases.items() if ar.get(k) != v]
check(f"Arabic: the {len(cases)} words of the plain-Java classes are byte for byte what they were before «اللغة»", not differs, differs[:5])
check("…and nothing new is said in Arabic but the English-only line (hijri.en)", set(ar) - set(cases) == {"hijri.en"}, sorted(set(ar) - set(cases)))
LETTER = re.compile("[\u0600-\u065f\u066d-\u06ff]")   # Arabic letters; not ٠…٩ and ٫ (the «الأرقام» setting: these runs use both kinds)
arabic_left = [f"{k}: {v}" for k, v in en.items() if LETTER.search(v)]
check(f"English: not one Arabic letter in the {len(en)} words", not arabic_left, arabic_left[:5])
check("English: the words a person reads (a sample)", en.get("control.name") == "Control Center" and en.get("bar.before") == "Fajr adhan in 8 min"
      and en.get("clock", "").startswith("7:30 PM") and en.get("zone", "").startswith("Time in Dubai")
      and en.get("hijri.en") == "Monday, 22 Jumada al-Awwal 1448 AH" and en.get("now.title", "").startswith("Surah Al-Fatihah"), en)
sys.exit(1 if failed else 0)
