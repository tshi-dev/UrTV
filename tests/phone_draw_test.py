#!/usr/bin/env python3
"""4.3 #53 «ارسم»: a paired phone draws (assets/phone/draw.html, /d) and its strokes go to the TV live (POST /api/draw about every 50 ms, compact
batches of 0..1000 points) and draw over the screen in MessageActivity (the text's overlay, switches and Back); «امسح» clears both; the drawing fades
8 s after the last stroke. PhoneDraw runs for real (tests/PhoneDrawRun.java); the wiring and the page are read from the sources.
    python3 tests/phone_draw_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
P = H / "phone"
A = ROOT / "android/launcher/src/main/assets/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
api, schema, acts, act, draw = read(P / "PhoneApi.java"), read(P / "Schema.java"), read(P / "PhoneActions.java"), read(H / "MessageActivity.java"), read(P / "PhoneDraw.java")
page, main, en = read(A / "draw.html"), read(A / "index.html"), read(A / "index.en.html")

check("the route: /api/draw behind the session like every /api, with its own request budget (drawing never starves the page); /d the page",
      'case DRAW_PATH: endpoint = "draw"; break;' in api and "(DRAW_PATH.equals(r.path) ? drawing : general).allow(r.client, now)" in api
      and 'if (r.path.equals("/d")) return post ? json(405, err("method")) : drawPage();' in api)
check("the schema: strokes (PhoneDraw.valid) or clear, nothing else", 'Clean c = only(body, "s", "clear");' in schema and "PhoneDraw.valid(body.get(\"s\"))" in schema
      and 'if (clear == body.containsKey("s")) return no("strokes or clear");' in schema)
check("the TV decides with the text's two switches and our screen in front; the overlay is started once, the strokes follow it",
      "PhoneDraw.TV.decide(sa.urtv.home.MessageActivity.on(app), sa.urtv.home.MessageActivity.overApps(app), ours" in acts
      and "if (!sa.urtv.home.MessageActivity.drawing())" in acts and '.putExtra("draw", true)' in acts)
check("the overlay draws the strokes over the whole screen, fades them, closes when nothing is left; Back clears the drawing",
      "root.addView(layer, new FrameLayout.LayoutParams(-1, -1));" in act and "PhoneDraw.TV.alpha(" in act and "if (a == 0f && !textShown) { finish(); return; }" in act
      and "PhoneDraw.TV.clear(); finish(); return true;" in act and "handler.postDelayed(this, 50);" in act)
check("the page: about every 50 ms, the same caps as the TV (30 points an entry, 8 entries, 240 a batch), six colours, «امسح» clears both",
      "}, 50);" in page and "ENTRY = 30, ENTRIES = 8, BATCH = 240" in page and len(re.findall(r"'#[0-9a-f]{6}'", page.split("var COLS = [")[1].split("]")[0])) == 6
      and "post({ clear: true })" in page and "ENTRY_POINTS = 30;" in draw and "MAX_BATCH_STROKES = 8, MAX_BATCH_POINTS = 240" in draw)
check("the page: only with the main page's session, its header on every POST, nothing from elsewhere",
      "sessionStorage.getItem('urtv')" in page and "'X-UrTV-Session': sess" in page and not re.search(r"https?://", page.split("<script")[1]))
check("the page in English: every word of its own", all(k in page.split("var W = EN ? {")[1].split("} : {")[0] for k in ("t1:", "t2:", "t4:", "t5:", "clr:", "colours:", "draw_full:")))
check("the main page links it beside the message, in both languages, both pages still under 64 KB",
      '<a class="lnk" href="/d">ارسم</a>' in main and '<a class="lnk" href="/d">Draw</a>' in en
      and len(main.encode()) < 64 * 1024 and len(en.encode()) < 64 * 1024 and len(page.encode()) < 64 * 1024, (len(main.encode()), len(en.encode())))

if not shutil.which("javac"):
    print("SKIP the drawing's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(P / "PhoneDraw.java"), str(ROOT / "tests/PhoneDrawRun.java")], capture_output=True, text=True)
        check("PhoneDraw compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.PhoneDrawRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"PhoneDraw: every case ({len(lines)})", len(lines) == 16 and not bad, bad or out)
sys.exit(1 if failed else 0)
