#!/usr/bin/env python3
"""Mockups of the «!» button and the «طريقة اللعب» panel of the games (docs/mockups/howto/, STEP 1: a proposal, no app code). Arabic RTL, the «ورقة» look of the games kit
(square corners, no pills, no «·»). The panel's words are read from docs/mockups/howto/TEXTS.md (the Arabic of the game's block), so the mockup shows the real text.
Usage: python3 games/design/howto.py OUT_DIR   (needs node with playwright; the fonts here are not the real ones, the spacing and the colours are what counts)"""
import os, re, subprocess, sys
from pathlib import Path
P = {"paper": "#EFE8DA", "tile": "#FBF8F1", "ink": "#16140F", "deep": "#0E5A45", "saff": "#F2A93B"}
ROOT = Path(__file__).resolve().parent.parent.parent
CSS = f"""*{{box-sizing:border-box;margin:0}}html,body{{background:{P['paper']}}}body{{width:1920px;height:1080px;color:{P['ink']};font-family:'DejaVu Sans','Noto Sans Arabic',sans-serif;direction:rtl;position:relative;overflow:hidden;transform-origin:0 0}}
.a{{position:absolute}}.band{{position:absolute;left:0;right:0;top:0;height:200px;background:{P['deep']};color:#fff}}
.hint{{position:absolute;top:978px;right:96px;font-size:30px;display:flex;gap:12px;align-items:center;height:48px;color:{P['ink']}}}.k{{background:rgba(22,20,15,.14);padding:2px 16px;min-width:64px;text-align:center;height:48px;line-height:48px}}.w{{opacity:.85;margin-left:36px;line-height:48px}}
.btn{{position:absolute;display:flex;align-items:center;justify-content:center;font-weight:700;border:6px solid {P['ink']}}}"""
def hint(*kv): return '<div class="hint">' + "".join(f'<span class="k">{k}</span><span class="w">{v}</span>' for k, v in zip(kv[::2], kv[1::2])) + '</div>'
def btn(label, x, y, w, h, focus, size=44, side="right"):
    bg, fg = (P["ink"], P["paper"]) if focus else (P["tile"], P["ink"])
    return f'<div class="btn" style="{side}:{x}px;top:{y}px;width:{w}px;height:{h}px;background:{bg};color:{fg};font-size:{size}px">{label}</div>'
def bang(focus):
    """the «!» button: a square at the top, at the END edge (the left in Arabic): the title keeps the start edge, the small line moves inward"""
    ring = f'<div class="a" style="left:84px;top:40px;width:120px;height:120px;border:6px solid {P["saff"]}"></div>' if focus else ""
    return ring + btn("!", 96, 52, 96, 96, focus, 64, "left")
def chips(rows, y0, rowh=76, gap=14):
    out = ""; y = y0
    for label, opts, picked, focus in rows:
        out += f'<div class="a" style="right:96px;top:{y + 8}px;width:320px;font-size:34px;font-weight:700">{label}</div>'
        x = 96 + 344
        for i, o in enumerate(opts):
            w = max(112, 22 * len(o) + 56); f = focus == i
            bg = P["ink"] if f else P["saff"] if i == picked else P["tile"]; fg = P["saff"] if f and i == picked else P["paper"] if f else P["ink"]
            out += f'<div class="btn" style="right:{x}px;top:{y}px;width:{w}px;height:{rowh}px;background:{bg};color:{fg};font-size:36px">{o}</div>'
            x += w + 14
        y += rowh + gap
    return out
def start_screen(bang_focus):
    rows = [("المستوى", ["متنوع", "سهل", "متوسط"], 0, -1), ("الفئة", ["للجميع", "أطفال"], 0, -1), ("وقت السؤال", ["١٥ ثانية", "٢٠ ثانية", "٣٠ ثانية"], 0, -1),
            ("زر الخيارات", ["نعم", "لا"], 0, -1), ("فرصة للفريق الآخر", ["لا", "نعم"], 0, -1), ("من يختار الحرف", ["من يجاوب صح يختار الحرف", "بالتناوب"], 0, -1)]
    h = hint("اختيار", "طريقة اللعب", "▼", "رجوع للإعدادات", "رجوع", "الألعاب") if bang_focus else hint("▲ ▼", "السطر", "◀ ▶", "الاختيار", "اختيار", "ابدأ", "رجوع", "الألعاب")
    return (f'<div class="band"><div class="a" style="right:96px;top:54px;font-size:96px;font-weight:700">خلية الحروف</div>'
            f'<div class="a" style="left:216px;top:84px;width:700px;text-align:left;font-size:40px;font-weight:700;color:{P["saff"]}">جهّز اللعبة</div></div>' + bang(bang_focus) + chips(rows, 224) +
            f'<div class="a" style="left:96px;top:806px;width:64px;text-align:center;font-size:36px;opacity:.8">▼</div>' + btn("ابدأ", 96, 856, 640, 104, not bang_focus, 44) + h)
def parse(game_no):
    t = (ROOT / "docs/mockups/howto/TEXTS.md").read_text(encoding="utf-8")
    sec = re.split(r"\n## ", t)[game_no]; name = sec.split("\n")[0]
    ar = lambda s: s.rsplit(" / ", 1)[0].strip()
    blocks = {}; cur = None
    for line in sec.split("\n")[1:]:
        m = re.match(r"\*\*([^*]+)\*\*\s*(.*)", line)
        if m:
            cur = m.group(1); blocks[cur] = []
            if m.group(2): blocks[cur].append(ar(m.group(2)))
        elif cur and line.strip(): blocks[cur].append(re.sub(r"^\d+\.\s*", "", ar(line)))
    name = re.sub(r"^\d+\.\s*", "", name); name = name.split(" (")[0]
    return name, blocks
def panel_page(game_no):
    name, b = parse(game_no); steps = b["كيف تلعب"]
    goal = f'<div class="a" style="right:96px;top:224px;width:1040px"><div style="background:{P["ink"]};color:{P["paper"]};font-size:36px;font-weight:700;padding:6px 20px;display:inline-block">الهدف</div><div style="font-size:38px;font-weight:700;margin-top:8px;line-height:1.3">{b["الهدف"][0]}</div></div>'
    nums = "".join(f'<div style="display:flex;gap:18px;margin-top:12px"><div style="min-width:60px;height:60px;background:{P["ink"]};color:{P["paper"]};font-size:38px;font-weight:700;display:flex;align-items:center;justify-content:center">{["١","٢","٣","٤","٥"][i]}</div><div style="font-size:36px;line-height:1.3">{s}</div></div>' for i, s in enumerate(steps))
    how = f'<div class="a" style="right:96px;top:396px;width:1040px"><div style="background:{P["deep"]};color:#fff;font-size:36px;font-weight:700;padding:6px 20px;display:inline-block">كيف تلعب</div>{nums}</div>'
    col = ""
    for key in ("بالجوالات", "النقاط", "النهاية"):
        if key in b:
            body = "".join(f'<div>{l}</div>' for l in b[key])
            col += f'<div style="background:{P["tile"]};border:6px solid {P["ink"]};margin-bottom:16px"><div style="background:{P["ink"]};color:{P["paper"]};font-size:36px;font-weight:700;padding:6px 20px">{key}</div><div style="padding:6px 20px 12px;font-size:34px;line-height:1.3">{body}</div></div>'
    col = f'<div class="a" style="left:96px;top:224px;width:640px">{col}</div>'
    return (f'<div class="band"><div class="a" style="right:96px;top:54px;font-size:96px;font-weight:700">طريقة اللعب</div><div class="a" style="left:96px;top:84px;width:700px;text-align:left;font-size:40px;font-weight:700;color:{P["saff"]}">{name}</div></div>'
            + goal + how + col + hint("▲ ▼", "تمرير", "رجوع", "إغلاق"))
def page(body, scale=1.0): return f'<!doctype html><meta charset="utf-8"><style>{CSS}</style>{body}'
SHOTS = {"howto-start-unfocused": (start_screen(False), 1920, 1080, 1.0), "howto-start-focused": (start_screen(True), 1920, 1080, 1.0)}
for no, tag in ((6, "letter-hive"), (1, "quiz")):
    SHOTS[f"howto-panel-{tag}"] = (panel_page(no), 1920, 1080, 1.0)
    SHOTS[f"howto-panel-{tag}-720"] = (panel_page(no), 1920, 1080, 1280 / 1920)   # the same layout drawn at 1280 x 720 (a device scale of 2/3)
if __name__ == "__main__":
    out = os.path.abspath(sys.argv[1]); os.makedirs(out, exist_ok=True)
    for n, (h, w, hh, sc) in SHOTS.items(): open(f"{out}/{n}.html", "w", encoding="utf-8").write(page(h, sc))
    js = "const {chromium}=require('playwright');(async()=>{const b=await chromium.launch(process.env.CHROMIUM?{executablePath:process.env.CHROMIUM}:{});" + "".join(
        f"{{const p=await b.newPage({{viewport:{{width:{w},height:{hh}}},deviceScaleFactor:{sc}}});await p.goto('file://{out}/{n}.html');await p.screenshot({{path:'{out}/{n}.png'}});await p.close()}}" for n, (h, w, hh, sc) in SHOTS.items()) + "await b.close()})()"
    open(f"{out}/shot.js", "w").write(js)
    r = subprocess.run(["node", f"{out}/shot.js"], capture_output=True, text=True, env={**os.environ, "NODE_PATH": os.environ.get("NODE_PATH", "/opt/node22/lib/node_modules")}); print(r.stderr[-300:])
    for n in SHOTS: os.remove(f"{out}/{n}.html")
    os.remove(f"{out}/shot.js")
