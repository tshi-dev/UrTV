#!/usr/bin/env python3
"""The entry window of «تلفزيونك» (mockups only): four windows, each with its own mark (drawn from the app icon's language: a gold outlined screen on a dark rounded square, a different picture
inside), name, one live line and its own colour mood; two layouts (a row of four, a 2x2), Arabic dark and light and English, the icon set, and where each existing entry lands.
   python3 design/43/hub4.py /tmp/hh && node design/43/render.js /tmp/hh design/43      (names hub4-*)    DejaVu Sans stands in for the private Thmanyah."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))

GOLD = "#E0BA74"
INNER = {
 "markets": '<path d="M6 16l4-4 3 3 5-6" /><path d="M14 9h4v4"/>',
 "matches": '<circle cx="12" cy="11.5" r="4.6"/><path d="M12 6.9l2.2 1.6-.8 2.6h-2.8l-.8-2.6zM9.4 13.6l-2 1M14.6 13.6l2 1"/>',
 "games": '<path d="M12 6l4.6 2.7v5.3L12 16.7 7.4 14V8.7z"/><circle cx="12" cy="11.4" r="1" fill="currentColor"/>',
 "settings": '<path d="M7 9h6M16 9h1M7 14h1M11 14h6"/><circle cx="14.5" cy="9" r="1.5"/><circle cx="9.5" cy="14" r="1.5"/>',
}
def mark(kind, size=160, bg="#0F2A1F", stroke=GOLD, radius=0.22):
    r = int(size * radius)
    return f'<div style="width:{size}px;height:{size}px;border-radius:{r}px;background:{bg};display:flex;align-items:center;justify-content:center;flex:none"><svg width="{size*0.74:.0f}" height="{size*0.74:.0f}" viewBox="0 0 24 24" fill="none" stroke="{stroke}" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="13.5" rx="2.6"/><path d="M8.5 20.5h7M12 17.5v3"/>{INNER[kind].replace("currentColor", stroke)}</svg></div>'

MOOD = {"markets": ("linear-gradient(150deg,#14705A,#0A3A33)", "#0F2A1F"), "matches": ("linear-gradient(150deg,#1F5FBF,#0E2D66)", "#0B1B3B"),
        "games": ("linear-gradient(150deg,#7B3FC9,#32135F)", "#1E0F3A"), "settings": ("linear-gradient(150deg,#5B6677,#242B36)", "#161B23")}
TX = {
 "ar": dict(t="تلفزيونك", sub="اختر نافذة: اختيار تفتحها", hint=[("اختيار", "افتح"), ("رجوع", "الرئيسية")],
            w=[("markets", "الأسواق ومحفظتي", "تاسي ١١٬٢٤٠ صاعد ٠٫٦٪، السوق مفتوح"), ("matches", "المباريات", "التالية: الهلال والنصر ٧:٣٠ م"), ("games", "الألعاب", "آخر لعبة: دومينو، فاز سعد"), ("settings", "الإعدادات", "الشكل زجاجي، الواي فاي متصل")]),
 "en": dict(t="Your TV", sub="Pick a window: OK opens it", hint=[("OK", "open"), ("Back", "home")],
            w=[("markets", "Markets and portfolio", "TASI 11,240 up 0.6%, market open"), ("matches", "Matches", "Next: Al-Hilal v Al-Nassr 7:30 PM"), ("games", "Games", "Last game: dominoes, Saad won"), ("settings", "Settings", "Look: glass, Wi-Fi connected")]),
}
def shell(body, light, en):
    bg = "linear-gradient(160deg,#F4F1EA,#DAD5C8)" if light else "linear-gradient(160deg,#17181B,#0B0C0E)"
    fg = "#14161A" if light else "#fff"
    d = "ltr" if en else "rtl"
    return f'''<!doctype html><html lang="{"en" if en else "ar"}" dir="{d}"><head><meta charset="utf-8"><style>*{{box-sizing:border-box;margin:0;padding:0}}html,body{{width:1920px;height:1080px;overflow:hidden}}
body{{font-family:'DejaVu Sans',sans-serif;direction:{d};background:{bg};color:{fg};position:relative}}.abs{{position:absolute}}.row{{display:flex;align-items:center}}.col{{display:flex;flex-direction:column}}</style></head><body>{body}</body></html>'''
def head(t, light):
    return f'<div class="abs col" style="right:96px;top:54px;align-items:flex-end;gap:8px"><div style="font-size:64px;font-weight:700">{t["t"]}</div><div style="font-size:36px;opacity:.7">{t["sub"]}</div></div>'
def keys(t, light):
    c = "rgba(20,22,26,.8)" if light else "rgba(255,255,255,.85)"; b = "rgba(20,22,26,.12)" if light else "rgba(255,255,255,.18)"
    return '<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:48px;height:56px">' + "".join(f'<div class="row" style="gap:12px"><div style="min-width:64px;height:48px;padding:0 16px;border-radius:12px;background:{b};display:flex;align-items:center;justify-content:center;font-size:30px;font-weight:700">{k}</div><div style="font-size:32px;color:{c}">{w}</div></div>' for k, w in t["hint"]) + '</div>'
def card(kind, name, live, foc, w, h, tall, light):
    bg, markbg = MOOD[kind]
    ring = "box-shadow:0 0 0 8px " + ("#14161A" if light else "#fff") + ",0 28px 60px rgba(0,0,0,.4);transform:scale(1.02);" if foc else ""
    inner = f'<div class="col" style="height:100%;justify-content:space-between">{mark(kind, 176 if tall else 160, markbg)}<div class="col" style="gap:16px"><div style="font-size:52px;font-weight:700;line-height:1.15">{name}</div><div style="font-size:34px;opacity:.92;line-height:1.35">{live}</div></div></div>' if tall else \
            f'<div class="row" style="height:100%;gap:40px">{mark(kind, 176, markbg)}<div class="col" style="gap:12px;flex:1"><div style="font-size:52px;font-weight:700;line-height:1.15">{name}</div><div style="font-size:34px;opacity:.92;line-height:1.35">{live}</div></div></div>'
    return f'<div style="width:{w}px;height:{h}px;padding:40px;border-radius:36px;background:{bg};color:#fff;{ring}">{inner}</div>'
def layout(kind, lang, light=False):
    t = TX[lang]; en = lang == "en"
    ws = t["w"]
    if kind == "row":
        cards = "".join(card(k, n, l, i == 0, 408, 640, True, light) for i, (k, n, l) in enumerate(ws))
        body = head(t, light) + f'<div class="abs row" style="left:96px;right:96px;top:230px;gap:32px;justify-content:center">{cards}</div>' + keys(t, light)
    else:
        cards = "".join(card(k, n, l, i == 0, 832, 360, False, light) for i, (k, n, l) in enumerate(ws))
        body = head(t, light) + f'<div class="abs" style="left:96px;right:96px;top:200px;display:grid;grid-template-columns:repeat(2,832px);gap:32px;justify-content:center">{cards}</div>' + keys(t, light)
    return shell(body, light, en)

def marks_page():
    items = "".join(f'<div class="col" style="align-items:center;gap:24px;width:360px">{mark(k, 280, MOOD[k][1])}<div style="font-size:44px;font-weight:700">{n}</div></div>' for k, n, _ in TX["ar"]["w"])
    small = "".join(f'<div class="col" style="align-items:center;gap:12px">{mark(k, s, MOOD[k][1])}<div style="font-size:28px;opacity:.7">{s} px</div></div>' for k in ("markets",) for s in (96, 64, 48))
    body = f'<div class="abs" style="right:96px;top:54px;font-size:56px;font-weight:700">علامات النوافذ الأربع</div><div class="abs" style="right:96px;top:130px;font-size:34px;opacity:.7">شاشة بإطار ذهبي على مربع داكن، مثل أيقونة التطبيق، وفي داخلها رسم واحد</div><div class="abs row" style="left:96px;right:96px;top:260px;gap:48px;justify-content:center">{items}</div><div class="abs row" style="left:96px;right:96px;bottom:96px;gap:48px;justify-content:center">{small}</div>'
    return shell(body, False, False)

def placement_page():
    rows = [("مركز التحكم (الزر الدائري فوق)", "يبقى في الشاشة الرئيسية وفوق كل نافذة. مو نافذة: هو لوحة سريعة (الشبكة، الصوت، النوم، الإطفاء)"),
            ("الأزرار فوق: الأسواق، المباريات", "تُحذف من الشريط الصغير وتصير النافذتين ١ و٢. الشريط يبقى: الجوال ومركز التحكم"),
            ("زر الإعدادات في الريموت", "ضغطة قصيرة: إعدادات التلفزيون. مطوّلة: نافذة ٤ «الإعدادات» (نفس اليوم)"),
            ("بطاقة الألعاب في الرئيسية", "تفتح النافذة ٣ مباشرة. «ألعاب العائلة» ما لها مدخل ثاني"),
            ("«محفظتي» و«أسواق»", "نافذة ١ واحدة: صفحة الأسواق وفيها لسان محفظتي (قفلها بقفل المحفظة كما هو)"),
            ("فتح تلفزيونك (من الرئيسية أو من الجهاز)", "تفتح هالمنافذ الأربعة. «رجوع» من أي نافذة ترجع هنا، ومن هنا للرئيسية")]
    body = '<div class="abs" style="right:96px;top:54px;font-size:56px;font-weight:700">وين يروح كل مدخل</div><div class="abs col" style="left:96px;right:96px;top:170px;gap:16px">' + "".join(f'<div class="row" style="gap:32px;min-height:120px;padding:16px 32px;border-radius:24px;background:rgba(255,255,255,.07)"><div style="width:620px;font-size:36px;font-weight:700">{a}</div><div style="flex:1;font-size:34px;opacity:.9;line-height:1.4">{b}</div></div>' for a, b in rows) + '</div>'
    return shell(body, False, False)

def build():
    S = {}
    S["hub4-01-row-ar-dark"] = layout("row", "ar"); S["hub4-02-grid-ar-dark"] = layout("grid", "ar")
    S["hub4-03-row-ar-light"] = layout("row", "ar", True); S["hub4-04-grid-ar-light"] = layout("grid", "ar", True)
    S["hub4-05-row-en"] = layout("row", "en"); S["hub4-06-grid-en"] = layout("grid", "en")
    S["hub4-07-marks"] = marks_page(); S["hub4-08-placement"] = placement_page()
    return S
if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in build().items(): (d / f"{n}.html").write_text(h, encoding="utf-8")
