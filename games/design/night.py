#!/usr/bin/env python3
"""Night-mode mockups of the family games (a proposal, no app code): three palettes, four renders. Usage: python3 night.py OUT_DIR  (needs node with playwright and CHROMIUM)
Contrast is computed here (WCAG ratio) so the numbers in games/PLAN-NIGHT-PHONE.md are the real ones."""
import json, os, subprocess, sys

PAL = {
 "A": {"name": "فحم دافئ", "en": "Warm charcoal", "paper": "#1C1A17", "tile": "#2B2824", "ink": "#ECE4D4", "deep": "#11503F", "tick": "#0C3C30", "saff": "#E3A33F", "onSaff": "#1A160F", "onDeep": "#F4EFE4", "ok": "#2F9F7A", "pink": "#D9694C"},
 "B": {"name": "ليل أزرق", "en": "Blue-black", "paper": "#12161C", "tile": "#1E242C", "ink": "#E6E9EE", "deep": "#0F4D4A", "tick": "#0B3A38", "saff": "#E0A83E", "onSaff": "#14120E", "onDeep": "#F1F4F7", "ok": "#2F9F86", "pink": "#D9694C"},
 "C": {"name": "بيج خافت", "en": "Dim sepia", "paper": "#24201A", "tile": "#332D25", "ink": "#E8DCC4", "deep": "#1F5A43", "tick": "#174632", "saff": "#D99A3A", "onSaff": "#1A1409", "onDeep": "#F3EBDA", "ok": "#35A07A", "pink": "#D2694A"},
}
SIDES = ["#6FA3D4", "#D9705C", "#7FC08C", "#B195DE", "#E0C06A", "#5BBDB3", "#E0A56E", "#8DBFDE", "#D595B4", "#AEBB72"]   # dimmed tints of the day colours
DAY_SIDES = ["#5AA9F0", "#F2705A", "#7BD88F", "#C28BFF", "#FFD15C", "#4FD1C5", "#FFB86B", "#8FD6FF", "#E58FB8", "#B7C76A"]

def lum(h):
    r, g, b = [int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    f = lambda c: c / 12.92 if c <= .03928 else ((c + .055) / 1.055) ** 2.4
    return .2126 * f(r) + .7152 * f(g) + .0722 * f(b)
def ratio(a, b):
    x, y = sorted((lum(a), lum(b)), reverse=True)
    return (x + .05) / (y + .05)

def table():
    rows = []
    for k, p in PAL.items():
        pairs = {"ink on paper": (p["ink"], p["paper"]), "ink on tile": (p["ink"], p["tile"]), "words on the green block": (p["onDeep"], p["deep"]), "words on saffron": (p["onSaff"], p["saff"]),
                 "saffron on paper": (p["saff"], p["paper"]), "right (green) on paper": (p["ok"], p["paper"]), "wrong (coral) on paper": (p["pink"], p["paper"]),
                 "weakest team colour on paper": (min(SIDES, key=lambda s: ratio(s, p["paper"])), p["paper"])}
        rows.append((k, {n: round(ratio(*v), 1) for n, v in pairs.items()}))
    return rows

def css(p):
    return f"""*{{box-sizing:border-box;margin:0}}body{{width:1920px;height:1080px;background:{p['paper']};color:{p['ink']};font-family:'DejaVu Sans','Noto Sans Arabic',sans-serif;direction:rtl;overflow:hidden;position:relative}}
.abs{{position:absolute}}h1{{font-size:88px;font-weight:700}}.sub{{font-size:36px;opacity:.8}}
.tile{{position:absolute;background:{p['tile']};border:6px solid {p['ink']}}}
.green{{position:absolute;background:{p['deep']};color:{p['onDeep']}}}
.btn{{position:absolute;height:104px;display:flex;align-items:center;justify-content:center;font-size:44px;font-weight:700;border:6px solid {p['ink']};background:{p['tile']}}}
.btn.f{{background:{p['ink']};color:{p['paper']}}}.saff{{background:{p['saff']};color:{p['onSaff']}}}
.hint{{position:absolute;bottom:36px;right:96px;font-size:30px;opacity:.75}}"""

def list_screen(p):
    games = [("الأسرع", "بالجوالات", 0), ("خمس خانات", "فرق أو أفراد", 1), ("خلية الحروف", "فريقان", 2), ("الكلمة المفقودة", "حقائق وأقوال", 3), ("خمّن الكلمة", "خمسة حروف", 4), ("دومينو", "٢ إلى ٤", 5)]
    t = ""
    for i, (n, s, c) in enumerate(games):
        col, row = i % 3, i // 3; x = 96 + col * 592; y = 300 + row * 330
        foc = i == 1
        t += f'<div class="tile" style="left:{x}px;top:{y}px;width:560px;height:300px;{"background:"+p["ink"]+";color:"+p["paper"] if foc else ""}"><div class="abs" style="right:28px;top:28px;width:56px;height:56px;background:{SIDES[c]};border:4px solid {p["paper"] if foc else p["ink"]}"></div><div class="abs" style="right:28px;bottom:84px;font-size:64px;font-weight:700">{n}</div><div class="abs" style="right:28px;bottom:28px;font-size:34px;opacity:.85">{s}</div></div>'
    sw = f'<div class="btn" style="left:96px;top:104px;width:360px;height:88px;font-size:36px;{"background:"+p["saff"]+";color:"+p["onSaff"]}">وضع ليلي: تشغيل</div>'
    return f'<style>{css(p)}</style><h1 class="abs" style="right:96px;top:84px">ألعاب العائلة</h1><div class="sub abs" style="right:96px;top:196px">اختاروا لعبة</div>{sw}{t}<div class="hint">▲ ▼ ◀ ▶ اختيار · OK ابدأ · رجوع</div>'.replace(" · ", "،  ")

def hex_screen(p):
    r = 78; cells = ""
    letters = "ابتجحدرزسغفكلمنهويخشصطعقذثض"
    owner = {2: 0, 7: 0, 12: 0, 13: 1, 18: 1, 8: 1, 17: 0}
    for i in range(25):
        row, col = divmod(i, 5); x = 60 + col * 200 + (100 if row % 2 else 0); y = 110 + row * 160
        o = owner.get(i); fill = SIDES[o] if o is not None else (p["saff"] if i == 11 else p["tile"]); fg = p["onSaff"] if (o is not None or i == 11) else p["ink"]
        cells += f'<div class="abs" style="left:{x}px;top:{y}px;width:184px;height:212px;background:{p["ink"] if i == 11 else p["paper"]};clip-path:polygon(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%)"></div><div class="abs" style="left:{x+7}px;top:{y+7}px;width:170px;height:198px;background:{fill};clip-path:polygon(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%);display:flex;align-items:center;justify-content:center;font-size:84px;font-weight:700;color:{fg}">{letters[i]}</div>'
    panel = ""
    for t, (n, c) in enumerate([("الصقور", SIDES[0]), ("النجوم", SIDES[1])]):
        on = t == 0
        panel += f'<div class="tile" style="left:96px;top:{150+t*170}px;width:520px;height:150px;{"background:"+p["ink"]+";color:"+p["paper"] if on else ""}"><div class="abs" style="right:24px;top:24px;width:48px;height:48px;background:{c}"></div><div class="abs" style="right:92px;top:16px;font-size:44px;font-weight:700">{n}</div><div class="abs" style="left:24px;top:8px;font-size:64px;font-weight:700;color:{p["saff"] if on else p["ink"]}">{["٥","٣"][t]}</div></div>'
    panel += f'<div class="abs" style="left:96px;top:540px;width:520px;font-size:60px;font-weight:700">دور الصقور</div><div class="abs" style="left:96px;top:700px;width:520px;font-size:38px;opacity:.8">اختاروا حرفًا ثم اضغطوا «اختيار»</div>'
    return f'<style>{css(p)}</style><div class="abs" style="right:96px;left:660px;top:0;bottom:0">{cells}</div>{panel}'

def question_screen(p):
    ans = [("القاهرة", ""), ("الإسكندرية", ""), ("أسوان", ""), ("الأقصر", "")]
    btn = ""
    for i, (a, _) in enumerate(ans):
        x = 96 + (i % 2) * 872; y = 600 + (i // 2) * 160
        right = i == 0
        btn += f'<div class="tile" style="left:{x}px;top:{y}px;width:856px;height:144px;{"background:"+p["ok"]+";color:"+p["onDeep"] if right else ""}"><div class="abs" style="right:28px;top:36px;width:72px;height:72px;background:{p["paper"]};color:{p["ink"]};display:flex;align-items:center;justify-content:center;font-size:48px;font-weight:700">{"أبجد"[i]}</div><div class="abs" style="right:128px;top:30px;font-size:60px;font-weight:700">{a}</div></div>'
    ticks = "".join(f'<div class="abs" style="left:{96+i*38}px;top:440px;width:26px;height:12px;background:{p["pink"] if i < 5 else p["tick"] if i > 12 else p["saff"]}"></div>' for i in range(20))
    return (f'<style>{css(p)}</style><div class="green" style="left:0;top:0;width:1920px;height:520px"></div><div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:700;color:{p["saff"]}">السؤال ٤ من ١٠، دور الصقور</div>'
            f'<div class="abs" style="right:96px;top:140px;width:1330px;font-size:104px;font-weight:700;color:{p["onDeep"]}">ما عاصمة مصر؟</div><div class="abs saff" style="left:96px;top:54px;width:200px;height:110px;display:flex;align-items:center;justify-content:center;font-size:72px;font-weight:700">١٢</div>{ticks}{btn}')

def palette_screen(p_all):
    out = f'<style>{css(PAL["A"])}</style><h1 class="abs" style="right:96px;top:40px;font-size:64px">ألوان الوضع الليلي: ثلاث خيارات</h1>'
    for i, (k, p) in enumerate(PAL.items()):
        x = 96 + i * 600; sw = ""
        for j, (nm, key) in enumerate([("paper", "paper"), ("tile", "tile"), ("ink", "ink"), ("deep", "deep"), ("saff", "saff"), ("ok", "ok"), ("pink", "pink")]):
            sw += f'<div class="abs" style="left:{20+j*76}px;top:140px;width:68px;height:68px;background:{p[key]};border:3px solid #888"></div>'
        side = "".join(f'<div class="abs" style="left:{20+j*52}px;top:232px;width:44px;height:44px;background:{SIDES[j]}"></div>' for j in range(10))
        mini = (f'<div class="abs" style="left:20px;top:300px;width:520px;height:300px;background:{p["paper"]};border:3px solid #666"><div class="abs" style="left:0;top:0;width:520px;height:130px;background:{p["deep"]}"></div>'
                f'<div class="abs" style="right:20px;top:24px;font-size:40px;font-weight:700;color:{p["onDeep"]}">ما عاصمة مصر؟</div>'
                f'<div class="abs" style="left:20px;top:150px;width:236px;height:60px;background:{p["ok"]};color:{p["onDeep"]};font-size:26px;display:flex;align-items:center;justify-content:center">القاهرة</div>'
                f'<div class="abs" style="left:264px;top:150px;width:236px;height:60px;background:{p["tile"]};color:{p["ink"]};font-size:26px;display:flex;align-items:center;justify-content:center;border:2px solid {p["ink"]}">أسوان</div>'
                f'<div class="abs" style="left:20px;top:230px;width:150px;height:48px;background:{p["saff"]};color:{p["onSaff"]};font-size:24px;display:flex;align-items:center;justify-content:center">١٢ ثانية</div></div>')
        out += f'<div class="abs" style="left:{x}px;top:140px;width:560px;height:760px;background:#0000;border:0"><div class="abs" style="right:20px;top:0;font-size:48px;font-weight:700">{k}: {p["name"]}</div>{sw}{side}{mini}</div>'
    rows = table()
    out += f'<div class="abs" style="right:96px;top:860px;font-size:28px;line-height:1.5;opacity:.9">' + "<br>".join(f"{k}: " + ", ".join(f"{n} {v}" for n, v in d.items() if n in ("ink on paper", "words on the green block", "words on saffron", "weakest team colour on paper")) for k, d in rows) + "</div>"
    return out

if __name__ == "__main__":
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    screens = {"night-1-games-list": list_screen(PAL["A"]), "night-2-hive-board": hex_screen(PAL["A"]), "night-3-question": question_screen(PAL["A"]), "night-4-palettes": palette_screen(PAL)}
    for n, h in screens.items(): open(f"{out}/{n}.html", "w", encoding="utf-8").write(f'<!doctype html><meta charset="utf-8">{h}')
    js = "const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1920,height:1080}});" + "".join(
        f"await p.goto('file://{out}/{n}.html');await p.screenshot({{path:'{out}/{n}.png'}});" for n in screens) + "await b.close()})()"
    open(f"{out}/shot.js", "w").write(js)
    print(json.dumps(table(), ensure_ascii=False, indent=1))
