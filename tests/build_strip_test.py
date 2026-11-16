#!/usr/bin/env python3
"""The build drops the page script's lines that are only a // comment (src/build.py js_line_comments), never a line of a string,
a `template` or a regex, and stops when it cannot follow the script.   Run: python3 tests/build_strip_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
import build   # noqa: E402

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def strip(js): return build.strip_js_comments("<script>" + js + "</script>")[8:-9]

js = "const a = 1;\n  // a note\nconst u = 'https://x.org/a';   // kept: after code\n"
check("a whole-line comment goes; a comment after code and a // in a string stay", strip(js) == "const a = 1;\nconst u = 'https://x.org/a';   // kept: after code\n", strip(js))
tpl = "const t = `one\n// not a comment\n${x ? `in\n// nor this\n` : ''}`;\n// gone\nf();\n"
check("a line of a template (nested ones too) is never taken", strip(tpl) == "const t = `one\n// not a comment\n${x ? `in\n// nor this\n` : ''}`;\nf();\n", strip(tpl))
rx = "const r = /\\/\\/ x[/]`/g, d = a / b / c;\n// gone\nreturn /`/.test(s);\n"
check("a regex (with / and ` inside) and a division are told apart", strip(rx) == "const r = /\\/\\/ x[/]`/g, d = a / b / c;\nreturn /`/.test(s);\n", strip(rx))
check("/* block */ comments and a // inside one are left to the browser", strip("/* a\n// b\n*/\nx();\n") == "/* a\n// b\n*/\nx();\n")
for bad in ("const s = `open\n", "f({\n", "const s = 'open\n"):
    try:
        strip(bad); ok = False
    except ValueError:
        ok = True
    check(f"a script it cannot follow stops the build ({bad.strip()!r})", ok)
page = (ROOT / "dist/tv.html").read_text(encoding="utf-8") if (ROOT / "dist/tv.html").is_file() else ""
check("the built page's script has no whole-line // comment left", page and not build.js_line_comments(page[page.index("<script>") + 8:page.rindex("</script>")]))
sys.exit(1 if failed else 0)
