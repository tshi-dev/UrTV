#!/usr/bin/env python3
"""Arabic letters stay joined: Chromium (and Android's TextView) draw Arabic letters apart when letter-spacing is not 0. Every CSS rule
with a non-zero letter-spacing in the page (src/template.html, so dist/) and the phone page is one that only ever holds Latin text or
digits (the list below, each with why); the phone code's Arabic placeholder has none; the home app's layouts have no non-zero
android:letterSpacing, and setLetterSpacing is only the phone screen's address and code (Latin and digits).
Run: python3 tests/arabic_spacing_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

LATIN_ONLY = {   # selector -> why it never holds Arabic letters
    "#credit": "hidden photo credit, Latin",
    ".clock": "the time: digits only (browsers ignore spacing on Arabic digits anyway)",
    "#code": "the pairing code input: digits; its Arabic placeholder is set to 0 below",
}

def spaced_rules(css):
    css = re.sub(r"/\*.*?\*/", "", css, flags=re.S)
    out = []
    for m in re.finditer(r"([^{}]+)\{([^{}]*)\}", css):
        v = re.search(r"letter-spacing\s*:\s*([^;}]+)", m.group(2))
        if v and not re.fullmatch(r"\s*(0|0?\.0+|0(px|em|rem)|normal)\s*", v.group(1)):
            out.append((m.group(1).strip(), v.group(1).strip()))
    return out

def styles(path):
    t = (ROOT / path).read_text(encoding="utf-8")
    return "\n".join(re.findall(r"<style[^>]*>(.*?)</style>", t, re.S))

for path in ("src/template.html", "dist/tv.html", "dist/tv-en.html", "android/launcher/src/main/assets/phone/index.html",
             "android/launcher/src/main/assets/phone/index.en.html"):
    bad = [(s, v) for s, v in spaced_rules(styles(path)) if s not in LATIN_ONLY]
    check(f"{path}: letter-spacing only on Latin/digit-only selectors", not bad, bad)

for sel in (".adbar", ".p-mid .unit", ".sc", ".subline", ".panel .label", ".week .hd"):
    rules = [v for s, v in spaced_rules(styles("src/template.html")) if s == sel]
    check(f"{sel} (Arabic text) has no letter-spacing", not rules, rules)

phone = styles("android/launcher/src/main/assets/phone/index.html")
check("the phone code's Arabic placeholder «الرمز (٦ أرقام)» is not spaced", re.search(r"#code::placeholder\{letter-spacing:0\}", phone) is not None)

lay = [(p.name, m) for p in (ROOT / "android").glob("*/src/main/res/layout/*.xml")
       for m in re.findall(r'android:letterSpacing="([^"]+)"', p.read_text(encoding="utf-8")) if float(m) != 0]
check("no layout spaces letters («يُتلى الآن» in the player was 0.18)", not lay, lay)
calls = []
for p in (ROOT / "android").glob("*/src/main/java/**/*.java"):
    for line in p.read_text(encoding="utf-8").split("\n"):
        if "setLetterSpacing(" in line and not line.strip().startswith(("*", "//")):
            calls.append(f"{p.name}: {line.strip()}")
check("setLetterSpacing only in the phone screen's boxed() (its address and code: Latin and digits)",
      calls == ["PhoneActivity.java: t.setLetterSpacing(spacing);"], calls)
pa = (ROOT / "android/launcher/src/main/java/sa/urtv/home/PhoneActivity.java").read_text(encoding="utf-8")
check("boxed() is called only with the address and the code", [x for x in re.findall(r"boxed\((\S+)", pa) if x != "String"] == ["ltr(control.address()", "ltr(spaced(o.code)),"], re.findall(r"boxed\((\S+)", pa))
check("tests/run.sh runs this test", "tests/arabic_spacing_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

print("\n" + (f"{failed} failed" if failed else "arabic spacing: all passed"))
raise SystemExit(1 if failed else 0)
