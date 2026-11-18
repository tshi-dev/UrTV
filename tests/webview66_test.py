#!/usr/bin/env python3
"""GitHub #19 (omarghxd-art): the screensaver page on WebView 66 (Android 9 TVs). Chrome 66 has no inset (87), no clamp()/min()/max() (79) and no flex
gap (84), so the photo may not show and the clock be small; and it may lack Umm al-Qura. Every such use has its fallback before it, and the Hijri
date is worked out where the engine cannot.   python3 tests/webview66_test.py"""
import json, re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


page = (ROOT / "src/template.html").read_text(encoding="utf-8")
css = re.search(r"<style[^>]*>(.*?)</style>", page, re.S).group(1)
NEW = re.compile(r"(?<![\w-])(clamp|min|max)\(")
decls = [(m.start(), m.group(1), m.group(2)) for m in re.finditer(r"(?<=[;{])\s*(-?-?[a-z][a-z-]*)\s*:([^;{}]*)", css)]
check("no inset: (top, right, bottom and left instead)", not re.search(r"(?<![\w-])inset\s*:", css))
bare = []
for i, (at, prop, val) in enumerate(decls):
    if prop.startswith("--") or not NEW.search(val):
        continue
    before = decls[i - 1] if i else None
    if not (before and before[1] == prop and not NEW.search(before[2])):
        bare.append(f"{prop}:{val[:50]}")
check("every clamp(), min() and max() has a plain value of the same property just before it", not bare, bare[:5])
sup = re.search(r"@supports not \(width:clamp\(1px,1px,1px\)\)\{(.*?)\}\}", css)
custom = {p for _, p, v in decls if p.startswith("--") and NEW.search(v)}
check("…a custom property with one has its plain value for such an engine (@supports not clamp)", sup is not None and all(f"{p}:" in sup.group(1) for p in custom), custom)
check("flex gap: on an engine without it (before Chrome 84) the gaps become margins (found by measuring, once)",
      "row-gap:1px" in page and "const ok = t.scrollHeight === 1;" in page and "c.style[side] = cg + 'px'" in page and "new MutationObserver(soon)" in page)
check("a size set from the script: clamp() only where the engine has it",
      "const CLAMP_OK = !!(window.CSS && CSS.supports && CSS.supports('width','clamp(1px,1px,1px)'));" in page and "(CLAMP_OK ? 'clamp(11px,.92vw,17px)' : '.92vw')" in page)
check("the Hijri date: the engine's Umm al-Qura where it has it, else the tabular calendar with our month names (never a Gregorian month as Hijri)",
      "const HIJRI_OK = " in page and "if(!HIJRI_OK && !CONFIG.dateNum) try{" in page and "const t = tabHijri(CONFIG.hijri ? hijriAt(now) : now)" in page and "if(c !== 'gregory' && !HIJRI_OK)" in page
      and "'جمادى الأولى'" in page and "'ذو الحجة'" in page)
if shutil.which("node"):
    fn = re.search(r"const UQ = .*?;", page).group(0) + "\n" + re.search(r"function tabHijri\(d\)\{.*?\n\}", page, re.S).group(0)
    js = fn + """
const f = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn', {timeZone:'UTC', day:'numeric', month:'numeric', year:'numeric'});
let worst = 0, n = 0;
for(let k = 0; k < 400; k++){
  const d = new Date(Date.UTC(2026, 0, 1) + k * 864e5 + 12 * 36e5);
  const p = f.formatToParts(d).reduce((a, x) => (a[x.type] = +x.value, a), {});
  let off = 9;   // the shift (a day either way at most) that gives the same day, month and year as Umm al-Qura
  for(const k of [0, -1, 1]){ const t = tabHijri(new Date(d.getTime() + d.getTimezoneOffset() * 6e4 + k * 864e5)); if(t.y === p.year && t.m === p.month && t.d === p.day){ off = Math.abs(k); break; } }
  worst = Math.max(worst, off); n++;
}
console.log(JSON.stringify({worst, n}));"""
    r = subprocess.run(["node", "-e", js], capture_output=True, text=True)
    got = json.loads(r.stdout) if r.returncode == 0 else {"err": r.stderr[-300:]}
    check("…the worked-out date is Umm al-Qura's own over 400 days (its months 1446-1460 are in the page)", got.get("worst", 9) == 0 and got.get("n") == 400, got)
else:
    print("NOTE node not found: the tabular calendar not compared here (CI does)")
check("credited in the CHANGELOG", "omarghxd-art" in (ROOT / "CHANGELOG.md").read_text(encoding="utf-8") and "#19" in (ROOT / "CHANGELOG.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
