#!/usr/bin/env python3
"""4.4 the owner: no «·» and no «•» on a screen, in either language. Two parts of one line are joined by «، » with Arabic and ", " with English
(S.sep() in both apps; WordMatch turns «، » into ", " for the screensaver's English). A string that is only dots (a hidden number, «••••») is a mask,
not a separator; the parsers (OnOff, WordMatch, CouchText) still read the old marks in data that has them.   python3 tests/separators_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)[:600]))
    failed += not ok


LIT = re.compile(r'"(?:[^"\\\n]|\\.)*"')
MARK = re.compile("[·•]")
PARSERS = {"OnOff.java", "WordMatch.java", "CouchText.java"}


def code(src):
    """the source without its comments (string literals kept whole)"""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            m = LIT.match(src, i)
            if m:
                out.append(m.group(0)); i = m.end(); continue
        if src.startswith("//", i):
            j = src.find("\n", i); i = n if j < 0 else j; continue
        if src.startswith("/*", i):
            j = src.find("*/", i + 2); i = n if j < 0 else j + 2; continue
        if c == "'" and src.startswith("'\\''", i):
            out.append(src[i:i + 4]); i += 4; continue
        out.append(c); i += 1
    return "".join(out)


def mask(s):
    return re.fullmatch(r"[•\s]+", s) is not None


bad = []
for p in sorted(ROOT.glob("android/*/src/main/java/**/*.java")):
    if p.name in PARSERS:
        continue
    for m in LIT.finditer(code(p.read_text(encoding="utf-8"))):
        body = m.group(0)[1:-1]
        if MARK.search(body) and not mask(body):
            bad.append(f"{p.name}: {body[:70]}")
check("no Java string of the apps has «·» or «•» (a mask of dots aside)", not bad, bad)

bad = []
for p in sorted((ROOT / "android").glob("*/src/main/res/values*/strings.xml")):
    for m in re.finditer(r"<string[^>]*>(.*?)</string>", p.read_text(encoding="utf-8"), re.S):
        if MARK.search(m.group(1)):
            bad.append(f"{p.parent.name}/{p.name}: {m.group(1)[:70]}")
check("no resource string has «·» or «•»", not bad, bad)

bad = []
for p in sorted((ROOT / "i18n").glob("*/*.tsv")):
    for i, line in enumerate(p.read_text(encoding="utf-8").split("\n"), 1):
        if line.startswith("#") or "\t" not in line:
            continue
        if MARK.search(line):
            bad.append(f"{p.parent.name}/{p.name}:{i}: {line[:80]}")
check("no word of the English tables has «·» or «•», in the Arabic or the English", not bad, bad)

bad = []
for p in sorted((ROOT / "android/launcher/src/main/assets/phone").glob("*.html")):
    for i, line in enumerate(p.read_text(encoding="utf-8").split("\n"), 1):
        if re.search(r"'\s*[·•]\s*'", line):
            bad.append(f"{p.name}:{i}")
check("the phone pages join their parts without «·»", not bad, bad)

for app, path in (("launcher", "android/launcher/src/main/java/sa/urtv/home/S.java"), ("screensaver", "android/app/src/main/java/sa/urtv/screensaver/S.java")):
    s = (ROOT / path).read_text(encoding="utf-8")
    check(f"{app}: S.sep() is «، » with Arabic and \", \" with English", 'String sep() { return en() ? ", " : "، "; }' in s)
for p in ("android/app/src/main/java/sa/urtv/screensaver/OnOff.java", "android/launcher/src/main/java/sa/urtv/home/OnOff.java"):
    check(f"{Path(p).parent.name}/OnOff: the state word ends at a comma too («يعمل، …»)", '"،,".indexOf' in (ROOT / p).read_text(encoding="utf-8").replace(" ·• ", ""))
check("WordMatch: a line joined with \", \" (S.sep() in English) is read part by part",
      '{"، ", ", "}, {", ", ", "}}' in (ROOT / "android/app/src/main/java/sa/urtv/screensaver/WordMatch.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
