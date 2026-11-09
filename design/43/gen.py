#!/usr/bin/env python3
"""4.3 design round: mockups of the TV screen, 1920x1080, Arabic, RTL. Not app code.

    python3 design/43/gen.py /tmp/h && node design/43/render.js /tmp/h design/43   # needs playwright + chromium

Fonts: the real Thmanyah files are private, so these use DejaVu Sans (it has Arabic) as the stand-in: letter shapes differ, widths are close.
Rules shown: no capsules (radius 12-24 on rectangles, circles only for dots), no tiny label above a title, spacing on the 8 scale (16 minimum inside a box),
safe area x 96-1824, y 54-1026, the D-pad focus state on one item of each screen.
"""
import sys
from pathlib import Path

OUT = Path(__file__).resolve().parent
BG = Path(__file__).resolve().parents[2] / "android/launcher/src/main/res/drawable-nodpi/bg.jpg"
if not BG.is_file():
    BG = Path("/home/user/UrTV/android/launcher/src/main/res/drawable-nodpi/bg.jpg")

# ---- looks: the five we have and four proposed (new=True). rest = a card/tray panel; foc = the focused fill; ring = the focus line
LOOKS = {
    "glass":     dict(ar="زجاجي",  new=False, dark=True,  veil="rgba(12,14,18,.30)", panel="rgba(20,22,26,.46)", tile="rgba(255,255,255,.10)", line="rgba(255,255,255,.16)", text="#FFFFFF", sub="#DADCE0", accent="#FFC2B3", foc="#FFFFFF", foct="#14161A", ring="rgba(255,255,255,.9)", blur=24),
    "graphite":  dict(ar="فحمي",   new=False, dark=True,  veil="rgba(18,19,20,.82)", panel="#202124",              tile="#2A2B2F",              line="rgba(255,255,255,.08)", text="#F2F0EC", sub="#B9B6B0", accent="#F0A64A", foc="#F0A64A", foct="#1A1206", ring="#F0A64A", blur=0),
    "navy":      dict(ar="كحلي",   new=False, dark=True,  veil="rgba(11,22,48,.80)", panel="#142247",              tile="#1B2C58",              line="rgba(224,186,116,.22)", text="#F3EEE2", sub="#B7BFD0", accent="#E0BA74", foc="#E0BA74", foct="#0B1630", ring="#E0BA74", blur=0),
    "clean":     dict(ar="نظيف",   new=False, dark=False, veil="rgba(244,244,241,.88)", panel="#FFFFFF",           tile="#F2F2EE",              line="rgba(20,24,30,.08)", text="#1B1D21", sub="#50555E", accent="#0B5E51", foc="#0B5E51", foct="#FFFFFF", ring="#0B5E51", blur=0),
    "sand":      dict(ar="رملي",   new=False, dark=False, veil="rgba(246,244,239,.55)", panel="rgba(255,255,255,.90)", tile="#F6F4EF",          line="#E3DFD5", text="#1B1F1D", sub="#545A57", accent="#166A45", foc="#EEF6F1", foct="#1B1F1D", ring="#1B8354", blur=16),
    "cinema":    dict(ar="سينمائي", new=True, dark=True,  veil="rgba(6,6,8,.86)",     panel="rgba(11,11,13,.94)", tile="#17171A",              line="rgba(255,255,255,.06)", text="#F5F1EA", sub="#A9A39A", accent="#E5484D", foc="#17171A", foct="#FFFFFF", ring="#FFFFFF", blur=0),
    "horizon":   dict(ar="أفق",    new=True, dark=True,  veil="rgba(14,20,36,.66)",  panel="rgba(16,24,44,.62)", tile="rgba(255,255,255,.08)", line="rgba(242,166,90,.30)", text="#FFF6EA", sub="#D2C6B6", accent="#F2A65A", foc="rgba(242,166,90,.22)", foct="#FFF6EA", ring="#F2A65A", blur=20),
    "dawn":      dict(ar="شفق",    new=True, dark=True,  veil="linear-gradient(160deg,rgba(54,30,92,.78),rgba(10,64,72,.74))", panel="rgba(255,255,255,.12)", tile="rgba(255,255,255,.10)", line="rgba(255,255,255,.20)", text="#FFFFFF", sub="#E2DCF0", accent="#B7A6FF", foc="rgba(255,255,255,.92)", foct="#241A3E", ring="#FFFFFF", blur=22),
    "minimal":   dict(ar="بسيط",   new=True, dark=True,  veil="rgba(0,0,0,.40)",     panel="rgba(0,0,0,0)",       tile="rgba(255,255,255,.00)", line="rgba(255,255,255,0)", text="#FFFFFF", sub="#E6E6E6", accent="#FFFFFF", foc="rgba(255,255,255,0)", foct="#FFFFFF", ring="#FFFFFF", blur=0),
}
ACCENTS = [("كهرماني", "#F0A64A"), ("ذهبي", "#E0BA74"), ("أخضر", "#54C08A"), ("فيروزي", "#2FB5A3"), ("سماوي", "#5AA9F0"), ("بنفسجي", "#9B8CF5"), ("وردي", "#F07A9A"), ("مرجاني", "#F2705A")]

ICONS = {
    "book": '<path d="M3 5.5C5.5 4.5 8.5 4.5 12 6c3.5-1.5 6.5-1.5 9-.5V19c-2.5-1-5.5-1-9 .5C8.5 18 5.5 18 3 19z"/><path d="M12 6v13.5"/>',
    "chart": '<path d="M3 17l6-6 4 4 8-9"/><path d="M15 6h6v6"/>',
    "phone": '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
    "wifi": '<path d="M2.5 9a14 14 0 0119 0"/><path d="M5.5 12.5a9.5 9.5 0 0113 0"/><path d="M8.7 16a5 5 0 016.6 0"/><circle cx="12" cy="19.2" r="1" fill="currentColor"/>',
    "sliders": '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    "mic": '<rect x="9" y="2.5" width="6" height="12" rx="3"/><path d="M5.5 11.5a6.5 6.5 0 0013 0M12 18v3.5"/>',
    "input": '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><path d="M9 12h8M14 9l3 3-3 3"/>',
    "tv": '<rect x="3" y="4.5" width="18" height="12.5" rx="2.5"/><path d="M8 20.5h8"/>',
    "check": '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    "plus": '<path d="M12 5v14M5 12h14"/>',
    "down": '<path d="M12 4v11M7 11l5 5 5-5M5 20h14"/>',
    "shield": '<path d="M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6z"/>',
    "photo": '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><circle cx="9" cy="10" r="1.8"/><path d="M4 18l5.5-5 4 3.5 3-2.5 4 4"/>',
    "palette": '<path d="M12 3a9 9 0 100 18c1.3 0 2-.8 2-1.8 0-1.4-1.2-1.6-1.2-2.8 0-1 .8-1.6 1.9-1.6H17a4 4 0 004-4c0-4.2-4-7.8-9-7.8z"/>',
    "sun": '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8"/>',
    "card": '<rect x="3" y="5" width="18" height="14" rx="3"/><path d="M7 10h6M7 14h10"/>',
    "grid": '<rect x="3.5" y="3.5" width="7" height="7" rx="1.8"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.8"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.8"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.8"/>',
    "chev": '<path d="M15 5l-7 7 7 7"/>',
    "star": '<path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1.1 5.9L12 16.9l-5.3 2.8 1.1-5.9L3.5 9.7l5.9-.8z"/>',
    "movie": '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><path d="M10 9.5v5l4.5-2.5z" fill="currentColor"/>',
    "list": '<path d="M8 6.5h12M8 12h12M8 17.5h12"/><circle cx="4.2" cy="6.5" r="1" fill="currentColor"/><circle cx="4.2" cy="12" r="1" fill="currentColor"/><circle cx="4.2" cy="17.5" r="1" fill="currentColor"/>',
    "music": '<path d="M9 18V6l10-2v12"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="16.5" cy="16" r="2.5"/>',
    "game": '<rect x="2.5" y="7" width="19" height="11" rx="4"/><path d="M7 10.5v4M5 12.5h4M16 11.5h.01M18 13.5h.01"/>',
    "weather": '<path d="M7 18a4 4 0 01-.4-8 5.5 5.5 0 0110.6 1.2A3.4 3.4 0 0117 18z"/>',
    "cast": '<path d="M3 8V6.5A2.5 2.5 0 015.5 4h13A2.5 2.5 0 0121 6.5v11a2.5 2.5 0 01-2.5 2.5H14"/><path d="M3 12a9 9 0 019 9M3 16.5a4.5 4.5 0 014.5 4.5"/><circle cx="3.6" cy="20.4" r=".9" fill="currentColor"/>',
}


def ic(name, size=32, sw=1.8):
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</svg>'


def css(look="glass", accent=None):
    L = dict(LOOKS[look])
    if accent:
        L["accent"] = accent
        if look in ("graphite", "navy", "clean"):
            L["foc"] = accent
            L["ring"] = accent
    return L


BASE = """
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:1920px;height:1080px;overflow:hidden}
body{font-family:'DejaVu Sans',sans-serif;direction:rtl;position:relative;background:#000;color:#fff}
.bg{position:absolute;inset:0;background:url('BGURL') center/cover}
.veil{position:absolute;inset:0}
.abs{position:absolute}
.row{display:flex;align-items:center}
.col{display:flex;flex-direction:column}
.t8{font-size:28px;font-weight:600}
.btn{display:flex;align-items:center;justify-content:center;border-radius:16px}
"""


def page(body, look="glass", accent=None, extra="", bg=True, plain=None):
    L = css(look, accent)
    vars_ = f"--panel:{L['panel']};--tile:{L['tile']};--line:{L['line']};--text:{L['text']};--sub:{L['sub']};--accent:{L['accent']};--foc:{L['foc']};--foct:{L['foct']};--ring:{L['ring']};--blur:{L['blur']}px;"
    back = f'<div class="bg"></div><div class="veil" style="background:{L["veil"]}"></div>' if bg else f'<div class="abs" style="inset:0;background:{plain or L["veil"]}"></div>'
    return f"""<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><style>{BASE.replace('BGURL', BG.as_uri())}
:root{{{vars_}}}
body{{color:var(--text)}}
.panel{{background:var(--panel);border:1px solid var(--line);backdrop-filter:blur(var(--blur));border-radius:24px}}
.tile{{background:var(--tile);border-radius:16px}}
.foc{{background:var(--foc)!important;color:var(--foct)!important;box-shadow:0 0 0 3px var(--ring)}}
.sub{{color:var(--sub)}}
.acc{{color:var(--accent)}}
{extra}</style></head><body>{back}{body}</body></html>"""


# ---------------------------------------------------------------- shared home pieces
def quick_row(look, focus=None, labels=True):
    """the top-left quick buttons: one bar, six 72 px squares (radius 16) on an 8 px grid, the icons 32 px; the focused one is solid with its name under it"""
    items = [("book", "تلاوة وأذان"), ("chart", "الأسواق"), ("phone", "الجوال"), ("wifi", "الشبكة"), ("sliders", "الإعدادات"), ("mic", "الصوت")]
    out = []
    for i, (n, name) in enumerate(items):
        f = " foc" if focus == i else ""
        out.append(f'<div class="btn{f}" style="width:72px;height:72px;color:var(--text);position:relative">{ic(n, 32)}'
                   + (f'<div style="position:absolute;top:84px;right:50%;transform:translateX(50%);white-space:nowrap;font-size:20px;color:var(--text);text-shadow:0 1px 8px rgba(0,0,0,.55)">{name}</div>' if focus == i and labels else '') + '</div>')
    return f'<div class="panel row" style="position:absolute;left:96px;top:54px;height:88px;padding:8px;gap:8px;border-radius:24px">{"".join(out)}</div>'


def greeting(look):
    return """<div class="abs col" style="right:96px;top:54px;align-items:flex-end;gap:8px;text-shadow:0 2px 14px rgba(0,0,0,.35)">
<div class="acc" style="font-size:32px;font-weight:600">صباح الخير</div>
<div style="font-size:22px;color:var(--sub)">سماء صافية في الرياض، ٢٨°</div>
<div style="font-size:112px;font-weight:300;line-height:1.05;margin-top:8px">٩:٥٧</div>
<div style="font-size:22px;color:var(--sub)">الأربعاء، ٢٤ جمادى الأولى ١٤٤٨ هـ</div></div>"""


APPS = [("#E5484D", "movie", "يوتيوب"), ("#3B82F6", "tv", "التلفزيون"), ("#14B8A6", "music", "الموسيقى"), ("#F59E0B", "game", "ألعاب"), ("#8B5CF6", "photo", "الصور"), ("#10B981", "cast", "البث"), ("#EC4899", "star", "المفضلة"), ("#64748B", "grid", "كل التطبيقات")]


def tray(look, focus=3, n=7, wide=False):
    """the apps bar: one panel, tiles of one size on an 8 px grid; its panel, line, tile fill and focus are the SAME tokens the cards use"""
    tw, th = (240, 136) if wide else (216, 136)
    tiles = []
    for i in range(n):
        c, g, name = APPS[i]
        f = " foc" if i == focus else ""
        tiles.append(f'<div class="tile btn col{f}" style="flex:1;height:{th}px;gap:8px;justify-content:center;color:var(--text)"><div class="btn" style="width:56px;height:56px;background:{c};border-radius:14px;color:#fff">{ic(g, 28)}</div><div style="font-size:20px">{name}</div></div>')
    return f'<div class="panel row" style="position:absolute;left:96px;right:96px;bottom:54px;height:168px;padding:16px;gap:16px;justify-content:flex-start">{"".join(tiles)}</div>'


def prayer_card(look):
    rows = [("الفجر", "٤:٣٨"), ("الشروق", "٥:٥٩"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١"), ("العشاء", "٦:٤١")]
    nxt = 3
    r = "".join(f'<div class="row" style="justify-content:space-between;height:32px;padding:0 8px;border-radius:8px;font-size:20px;{"background:var(--tile);color:var(--accent);font-weight:600" if i == nxt else "color:var(--sub)"}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate(rows))
    return f'<div class="panel col" style="position:absolute;left:96px;top:176px;width:480px;height:288px;padding:16px;gap:0"><div class="row" style="justify-content:space-between;margin-bottom:8px;height:40px"><span style="font-size:24px;font-weight:600">مواقيت اليوم</span><span class="acc" style="font-size:20px">العصر بعد ساعتين</span></div>{r}</div>'


def home(look, accent=None, focus=3, qfocus=None, extra_body="", cards=True, wide=False):
    glow = '<div class="abs" style="left:0;right:0;bottom:0;height:420px;background:radial-gradient(ellipse at 50% 100%,rgba(242,166,90,.38),transparent 70%)"></div>' if look == 'horizon' else ''
    body = quick_row(look, qfocus) + greeting(look) + (prayer_card(look) if cards else "") + glow + tray(look, focus, 7, wide) + extra_body
    return page(body, look, accent)


# ---------------------------------------------------------------- mini home (a live-looking preview tile)
def mini(look, accent=None, w=360, h=200, focus=True):
    L = css(look, accent)
    s = w / 1920
    h = round(w * 9 / 16)   # always the TV's shape: nothing is cropped
    glow = '<div style="position:absolute;left:0;right:0;bottom:0;height:420px;background:radial-gradient(ellipse at 50% 100%,rgba(242,166,90,.38),transparent 70%)"></div>' if look == 'horizon' else ''
    inner = f"""<div style="position:absolute;left:0;top:0;width:1920px;height:1080px;transform:scale({s});transform-origin:top left;--panel:{L['panel']};--tile:{L['tile']};--line:{L['line']};--text:{L['text']};--sub:{L['sub']};--accent:{L['accent']};--foc:{L['foc']};--foct:{L['foct']};--ring:{L['ring']};--blur:{L['blur']}px;color:{L['text']}">
<div class="bg" style="position:absolute;inset:0;background:url('{BG.as_uri()}') center/cover"></div><div style="position:absolute;inset:0;background:{L['veil']}"></div>
{quick_row(look, 2 if focus else None, False)}<div class="abs col" style="right:96px;top:54px;align-items:flex-end"><div style="font-size:150px;font-weight:300">٩:٥٧</div></div>
{prayer_card(look)}{glow}{tray(look, 3 if focus else -1, 7)}</div>"""
    return f'<div style="position:relative;width:{w}px;height:{h}px;overflow:hidden;border-radius:16px;background:#000;box-shadow:0 0 0 1px rgba(128,128,128,.35)">{inner}</div>'


# ================================================================ screens
SHOTS = {}


# 01/02 the quick buttons at rest and focused
def quick_variants():
    SHOTS["01-quick-buttons-rest"] = home("glass", focus=-1)
    SHOTS["02-quick-buttons-focus"] = home("glass", focus=-1, qfocus=1)


# 03/04 the one «المظهر» page
def appearance_page(sub=None):
    cats = [("الشكل", "palette", "زجاجي", "look"), ("اللون", "sun", "كهرماني", "accent"), ("الإضاءة", "sun", "تلقائي · فاتح نهارًا", "light"),
            ("البطاقات", "card", "زجاج · الرئيسية وشاشة التوقف", "cards"), ("الخلفية والصور", "photo", "٣ مجموعات · تعتيم ٣٠٪", "wall"), ("الحجم والكثافة", "grid", "٧ تطبيقات في الصف · مريح", "density")]
    def preview(kind):
        if kind == "look": return '<div class="row" style="justify-content:center;height:176px">' + mini("glass", None, 312, 176, False) + '</div>'
        if kind == "accent": return '<div class="row" style="gap:8px;height:176px;align-items:center;justify-content:center">' + "".join(f'<div style="width:40px;height:40px;border-radius:12px;background:{c}"></div>' for _, c in ACCENTS[:8]) + '</div>'
        if kind == "light": return '<div class="row" style="gap:8px;height:176px">' + "".join(f'<div style="flex:1;height:176px;border-radius:12px;background:{c};display:flex;align-items:center;justify-content:center;color:{t};font-size:20px">{n}</div>' for n, c, t in (("داكن", "#17181B", "#fff"), ("تلقائي", "linear-gradient(90deg,#17181B 50%,#ECEDEF 50%)", "#888"), ("فاتح", "#ECEDEF", "#14161A"))) + '</div>'
        if kind == "cards": return '<div class="row" style="gap:16px;height:176px;align-items:center"><div class="panel" style="width:176px;height:112px;border-radius:16px"></div><div style="width:176px;height:112px;border-radius:4px;background:rgba(10,18,14,.75);border:1px solid rgba(255,255,255,.12)"></div></div>'
        if kind == "wall": return '<div class="row" style="gap:8px;height:176px">' + "".join(f'<div style="flex:1;height:176px;border-radius:12px;background:url(\'{BG.as_uri()}\') {x}% center/cover"></div>' for x in (20, 50, 80)) + '</div>'
        return '<div class="row" style="gap:8px;height:176px;align-items:flex-end;justify-content:center">' + "".join(f'<div class="tile" style="width:{w}px;height:{h}px;background:rgba(255,255,255,.18)"></div>' for w, h in ((40, 64), (56, 80), (72, 96))) + '</div>'
    cells = []
    for i, (n, g, val, k) in enumerate(cats):
        f = " foc" if (sub is None and i == 0) or (sub is not None and k == sub) else ""
        cells.append(f'<div class="panel col{f}" style="width:560px;height:344px;padding:16px;gap:8px;border-radius:24px"><div style="height:176px;border-radius:16px;overflow:hidden">{preview(k)}</div><div style="font-size:28px;font-weight:600;margin-top:8px">{n}</div><div style="font-size:20px;opacity:.8">{val}</div></div>')
    return cells


def appearance_main():
    cells = appearance_page(None)
    body = f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">المظهر</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">كل ما يخص شكل التلفزيون في مكان واحد، وكل خيار يظهر قبل أن تختاره.</div>' \
           f'<div class="abs" style="left:96px;right:96px;top:176px;display:grid;grid-template-columns:repeat(3,1fr);gap:24px;justify-items:center">{"".join(cells)}</div>' \
           '<div class="abs row" style="left:96px;bottom:54px;gap:16px"><div class="panel btn" style="height:56px;padding:0 24px;font-size:22px;border-radius:16px;gap:8px">' + ic("chev", 24) + 'رجوع</div></div>'
    return page(body, "glass")


def appearance_open():
    looks = ["glass", "graphite", "navy", "sand", "cinema", "horizon", "dawn", "minimal"]
    tiles = []
    for i, k in enumerate(looks[:8]):
        f = " foc" if k == "glass" else ""
        tiles.append(f'<div class="panel col{f}" style="width:408px;height:300px;padding:16px;gap:8px;border-radius:24px">{mini(k, None, 376, 211, False)}<div style="font-size:24px;font-weight:600;margin-top:8px">{LOOKS[k]["ar"]}{" · الافتراضي" if k == "glass" else ""}</div></div>')
    acc = "".join(f'<div class="btn" style="width:64px;height:64px;background:{c};border-radius:16px;color:#14161A;{"box-shadow:0 0 0 4px #fff;" if n=="كهرماني" else ""}">{ic("check", 28, 2.4) if n=="كهرماني" else ""}</div>' for n, c in ACCENTS)
    body = f'<div class="abs row" style="right:96px;top:54px;gap:16px;font-size:40px;font-weight:600">المظهر <span class="sub" style="font-size:28px;font-weight:400">‹ الشكل</span></div>' \
           f'<div class="abs" style="left:96px;right:96px;top:140px;display:grid;grid-template-columns:repeat(4,1fr);gap:16px;justify-items:center">{"".join(tiles)}</div>' \
           f'<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:32px;justify-content:space-between"><div class="row" style="gap:8px">{acc}</div>' \
           '<div class="row" style="gap:8px">' + "".join(f'<div class="panel btn" style="height:64px;width:120px;font-size:22px;border-radius:16px;{"background:var(--foc);color:var(--foct)" if n=="تلقائي" else ""}">{n}</div>' for n in ("داكن", "تلقائي", "فاتح")) + '</div></div>'
    return page(body, "glass")


# 05 more looks, preview grid (new ones first)
def looks_grid():
    order = ["glass", "graphite", "navy", "sand", "cinema", "horizon", "dawn", "minimal"]
    notes = {"cinema": "أسود عميق، بطاقات عريضة، تركيز بإطار أبيض", "horizon": "توهّج الأفق خلف الشريط، تركيز متوهّج", "dawn": "تدرّج بنفسجي فيروزي بلا حدود", "minimal": "بلا زجاج: كلام فوق الصورة، تركيز بخط تحته",
             "glass": "الافتراضي: زجاج مدخّن كما هو الآن", "graphite": "فحم وكهرمان", "navy": "كحلي وذهب", "sand": "أبيض على رمل"}
    tiles = []
    for k in order:
        f = " foc" if k == "glass" else ""
        tiles.append(f'<div class="panel col{f}" style="width:408px;height:344px;padding:16px;gap:8px;border-radius:24px">{mini(k, None, 376, 188, True)}<div style="font-size:24px;font-weight:600;margin-top:8px">{LOOKS[k]["ar"]}</div><div style="font-size:18px;opacity:.78;line-height:1.5">{notes[k]}</div></div>')
    body = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">الشكل</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">الزجاجي هو الشكل الافتراضي، والباقي خيارات.</div>' \
           f'<div class="abs" style="left:96px;right:96px;top:176px;display:grid;grid-template-columns:repeat(4,1fr);gap:24px;justify-items:center">{"".join(tiles)}</div>'
    return page(body, "glass")


# 06 accent palette and the flexible options
def accents_page():
    sw = []
    for n, c in ACCENTS:
        on = n == "فيروزي"
        sw.append(f'<div class="col" style="align-items:center;gap:8px;width:128px"><div class="btn" style="width:96px;height:96px;background:{c};border-radius:24px;color:#10161A;{"box-shadow:0 0 0 4px var(--ring);" if on else ""}">{ic("check", 40, 2.4) if on else ""}</div><div style="font-size:20px">{n}</div></div>')
    def slider(name, val, lo, hi, unit):
        pct = (val - lo) / (hi - lo) * 100
        return f'<div class="col" style="gap:8px"><div class="row" style="justify-content:space-between;font-size:22px"><span>{name}</span><span class="acc">{val}{unit}</span></div><div style="position:relative;height:8px;border-radius:4px;background:rgba(255,255,255,.22)"><div style="position:absolute;right:0;top:0;height:8px;width:{pct}%;border-radius:4px;background:var(--accent)"></div><div style="position:absolute;right:calc({pct}% - 12px);top:-8px;width:24px;height:24px;border-radius:12px;background:#fff"></div></div></div>'
    focus_styles = "".join(f'<div class="panel btn col{" foc" if i==0 else ""}" style="width:152px;height:96px;gap:8px;border-radius:16px;font-size:20px"><div style="width:64px;height:32px;border-radius:8px;{s}"></div>{n}</div>' for i, (n, s) in enumerate((("ممتلئ", "background:var(--accent)"), ("إطار", "border:3px solid var(--accent)"), ("توهّج", "background:rgba(255,255,255,.1);box-shadow:0 0 16px 2px var(--accent)"))))
    body = f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">اللون والتحكم</div>' \
           f'<div class="abs panel col" style="right:96px;top:140px;width:1128px;height:440px;padding:24px;gap:24px"><div style="font-size:28px;font-weight:600">لون التمييز</div><div class="row" style="gap:8px;flex-wrap:wrap;justify-content:space-between">{"".join(sw[:4])}</div><div class="row" style="gap:8px;flex-wrap:wrap;justify-content:space-between">{"".join(sw[4:])}</div></div>' \
           f'<div class="abs panel col" style="right:96px;top:612px;width:1128px;height:360px;padding:24px;gap:24px"><div style="font-size:28px;font-weight:600">التحكم</div>{slider("شفافية الشريط", 70, 30, 100, "٪")}{slider("تعتيم الخلفية", 30, 0, 90, "٪")}<div class="row" style="gap:16px;font-size:22px"><span style="width:240px">شكل التركيز</span>{focus_styles}</div></div>' \
           f'<div class="abs" style="left:96px;top:140px;width:520px">{mini("graphite", "#2FB5A3", 520, 292, True)}<div class="sub" style="font-size:20px;margin-top:16px;line-height:1.6">المعاينة حيّة: الفيروزي على الفحمي يغيّر التركيز والعناوين فقط، والباقي كما هو.</div></div>'
    return page(body, "glass", "#2FB5A3")


# 07-09 the same home in three looks: cards + apps bar from the same tokens
def home_three():
    SHOTS["07-home-graphite"] = home("graphite", focus=2)
    SHOTS["08-home-sand"] = home("sand", focus=4)
    SHOTS["09-home-horizon"] = home("horizon", focus=1)


# 10-12 setup
def setup_plan():
    apps = [("TV Bro", "متصفح للتلفزيون", "#3B82F6", "globe", True, "٢٢ م.ب"), ("SmartTube", "يوتيوب بلا إعلانات", "#E5484D", "movie", True, "٣٤ م.ب"), ("مشغّل IPTV", "قنواتك من رابطك", "#14B8A6", "list", True, "١٨ م.ب"), ("محوّل الصوت", "تشغيل الأذان من الجوال", "#8B5CF6", "music", False, "٦ م.ب")]
    ICONS["globe"] = '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>'
    rows = "".join(f'<div class="tile row{" foc" if i==0 else ""}" style="height:96px;padding:0 16px;gap:16px"><div class="btn" style="width:56px;height:56px;border-radius:12px;background:{c};color:#fff">{ic(g, 28)}</div><div class="col" style="flex:1;gap:0"><div style="font-size:24px;font-weight:600">{n}</div><div style="font-size:18px;opacity:.78">{d}</div></div><div style="font-size:18px;opacity:.7">{sz}</div><div class="btn" style="width:40px;height:40px;border-radius:12px;{("background:var(--foct);color:var(--accent)" if i == 0 else "background:var(--accent);color:#14161A") if on else "border:2px solid var(--sub)"}">{ic("check", 24, 2.6) if on else ""}</div></div>' for i, (n, d, c, g, on, sz) in enumerate(apps))
    own = '<div class="tile row" style="height:80px;padding:0 16px;gap:16px;opacity:.85"><div class="btn" style="width:48px;height:48px;border-radius:12px;background:var(--accent);color:#14161A">' + ic("tv", 24) + '</div><div style="flex:1;font-size:22px">UrTV والشاشة الرئيسية وشاشة التوقف</div><div class="sub" style="font-size:18px">دائمًا</div></div>'
    body = f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">إعداد تلفزيوني</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">اختر ما يُثبَّت. لا يُحذف شيء من تلفزيونك.</div>' \
           f'<div class="abs panel col" style="right:96px;top:176px;width:1128px;height:816px;padding:24px;gap:16px"><div style="font-size:28px;font-weight:600">التطبيقات</div>{rows}<div style="height:8px"></div>{own}</div>' \
           f'<div class="abs panel col" style="left:96px;top:176px;width:576px;height:816px;padding:24px;gap:16px"><div style="font-size:28px;font-weight:600">ملخص</div><div style="font-size:22px;line-height:1.9" class="sub">٣ تطبيقات مختارة<br>٧٤ م.ب للتنزيل<br>نحو دقيقتين<br>التلفزيون يُعاد تشغيله مرة واحدة</div><div style="flex:1"></div><div class="btn" style="height:72px;background:var(--accent);color:#14161A;font-size:26px;font-weight:600;border-radius:16px;box-shadow:0 0 0 3px #fff">ابدأ الإعداد</div><div class="btn" style="height:56px;font-size:22px;border:1px solid var(--line);border-radius:16px">لاحقًا</div></div>'
    return page(body, "graphite", plain="#17181B", bg=False)


def setup_progress():
    steps = [("فحص التلفزيون", "done"), ("تنزيل التطبيقات", "done"), ("تثبيت SmartTube", "active"), ("تثبيت TV Bro", "wait"), ("ضبط الشاشة الرئيسية", "wait"), ("إعادة التشغيل", "wait")]
    def st(n, s):
        mark = {"done": f'<div class="btn" style="width:40px;height:40px;border-radius:20px;background:var(--accent);color:#14161A">{ic("check", 22, 2.6)}</div>', "active": '<div class="btn" style="width:40px;height:40px;border-radius:20px;border:3px solid var(--accent)"><div style="width:12px;height:12px;border-radius:6px;background:var(--accent)"></div></div>', "wait": '<div style="width:40px;height:40px;border-radius:20px;border:2px solid var(--sub);opacity:.6"></div>'}[s]
        return f'<div class="row" style="gap:16px;height:72px;font-size:26px;{"font-weight:600" if s=="active" else "opacity:.7" if s=="wait" else ""}">{mark}<span>{n}</span></div>'
    body = f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">نجهّز تلفزيونك</div>' \
           f'<div class="abs panel col" style="right:96px;top:140px;width:760px;height:560px;padding:24px;gap:0">{"".join(st(n, s) for n, s in steps)}</div>' \
           '<div class="abs panel col" style="left:96px;top:140px;width:880px;height:560px;padding:32px;gap:24px;justify-content:center"><div style="font-size:32px;font-weight:600">نثبّت SmartTube…</div><div class="sub" style="font-size:22px">٣٤ م.ب · الخطوة ٣ من ٦</div><div style="height:16px;border-radius:8px;background:rgba(255,255,255,.14);position:relative"><div style="position:absolute;right:0;top:0;height:16px;width:58%;border-radius:8px;background:var(--accent)"></div></div><div style="font-size:48px;font-weight:300">٥٨٪</div></div>' \
           '<div class="abs panel row" style="left:96px;right:96px;bottom:54px;height:96px;padding:0 24px;gap:16px;font-size:22px"><div style="color:var(--accent)">' + ic("shield", 32) + '</div><span>لا تفصل التلفزيون ولا تضغط زر الرئيسية الآن. قد تتحول الشاشة إلى السواد لحظات، وهذا طبيعي.</span></div>'
    return page(body, "graphite", plain="#17181B", bg=False)


def setup_done():
    items = ["TV Bro", "SmartTube", "مشغّل IPTV"]
    body = f'<div class="abs col" style="right:96px;top:54px;width:1000px;gap:24px"><div class="btn" style="width:96px;height:96px;border-radius:48px;background:var(--accent);color:#14161A">{ic("check", 56, 2.6)}</div><div style="font-size:56px;font-weight:600">تلفزيونك جاهز</div><div class="sub" style="font-size:26px;line-height:1.7">زر الرئيسية الآن يفتح UrTV. التطبيقات الثلاثة في صفّ المفضلة.</div></div>' \
           f'<div class="abs panel col" style="left:96px;top:176px;width:640px;height:520px;padding:24px;gap:16px"><div style="font-size:28px;font-weight:600">ما تم</div>' + "".join(f'<div class="row" style="gap:16px;font-size:24px;height:56px"><span style="color:var(--accent)">{ic("check", 28, 2.4)}</span>{n}</div>' for n in items + ["الشاشة الرئيسية UrTV", "شاشة التوقف"]) + '</div>' \
           '<div class="abs row" style="right:96px;bottom:54px;gap:16px"><div class="btn" style="height:72px;padding:0 32px;background:var(--accent);color:#14161A;font-size:26px;font-weight:600;box-shadow:0 0 0 3px #fff">افتح UrTV</div><div class="btn" style="height:72px;padding:0 32px;font-size:24px;border:1px solid var(--line)">إغلاق</div></div>'
    return page(body, "graphite", plain="#17181B", bg=False)


# 13 «تطبيقات مقترحة»
def suggested():
    apps = [("SmartTube", "يوتيوب بلا إعلانات", "#E5484D", "movie", "ثبّت"), ("TV Bro", "متصفح بالريموت", "#3B82F6", "globe", "مثبّت"), ("مشغّل IPTV", "قنواتك من رابطك", "#14B8A6", "list", "ثبّت"), ("Kodi", "مكتبتك من الأفلام", "#0EA5E9", "play", "ثبّت"),
            ("VLC", "يشغّل أي ملف", "#F59E0B", "movie", "مثبّت"), ("Send Files", "أرسل من الجوال", "#8B5CF6", "down", "ثبّت"), ("Aptoide TV", "متجر بديل", "#10B981", "grid", "ثبّت"), ("تلفاز المباريات", "نتائج مباشرة", "#EC4899", "star", "ثبّت")]
    ICONS["globe"] = '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>'
    ICONS["play"] = '<path d="M8 5l11 7-11 7z"/>'
    cells = []
    for i, (n, d, c, g, s) in enumerate(apps):
        inst = s == "مثبّت"
        f = " foc" if i == 0 else ""
        cells.append(f'<div class="panel col{f}" style="width:408px;height:280px;padding:16px;gap:8px;border-radius:24px;justify-content:space-between"><div class="row" style="gap:16px"><div class="btn" style="width:72px;height:72px;border-radius:16px;background:{c};color:#fff">{ic(g, 36)}</div><div class="col" style="gap:4px"><div style="font-size:26px;font-weight:600">{n}</div><div style="font-size:18px;opacity:.8">{d}</div></div></div>'
                     f'<div class="btn" style="height:56px;font-size:22px;border-radius:16px;{"border:1px solid var(--line);opacity:.85" if inst else "background:var(--accent);color:#14161A;font-weight:600"}">{(ic("check", 24, 2.4) + " ") if inst else ""}{s}</div></div>')
    body = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">تطبيقات مقترحة</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">من مصادر موثوقة، وتحديثها يبقى بيدك.</div>' \
           f'<div class="abs" style="left:96px;right:96px;top:176px;display:grid;grid-template-columns:repeat(4,1fr);gap:24px;justify-items:center">{"".join(cells)}</div>'
    return page(body, "graphite", plain="#17181B", bg=False)


# 14 the banner
def banner():
    nudge = '<div class="abs col" style="left:50%;transform:translateX(-50%);top:54px;align-items:center;gap:8px"><div class="panel foc" style="padding:16px 24px;text-align:center;font-size:24px;line-height:1.6;border-radius:16px;max-width:560px"><b>خلّ تلفزيونك يفتح على UrTV</b><br><span style="font-size:20px">اضغط هنا وكمّل إعداد تلفزيوني (دقيقتين)</span></div><div class="panel btn" style="height:56px;padding:0 32px;font-size:22px;border-radius:16px">لاحقًا</div></div>'
    return page(quick_row("glass", None) .replace("left:96px;top:54px", "left:96px;top:54px") + greeting("glass") + tray("glass", -1) + nudge, "glass")


def build():
    quick_variants()
    SHOTS["03-appearance-main"] = appearance_main()
    SHOTS["04-appearance-looks-open"] = appearance_open()
    SHOTS["05-new-looks-grid"] = looks_grid()
    SHOTS["06-accents-and-controls"] = accents_page()
    home_three()
    SHOTS["10-setup-plan"] = setup_plan()
    SHOTS["11-setup-progress"] = setup_progress()
    SHOTS["12-setup-done"] = setup_done()
    SHOTS["13-suggested-apps"] = suggested()
    SHOTS["14-home-banner"] = banner()


if __name__ == "__main__":
    # writes the pages as HTML into the folder given (default _html), then render.js turns each into a PNG: node design/43/render.js _html
    build()
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html")
    d.mkdir(exist_ok=True)
    for name, html in SHOTS.items():
        (d / f"{name}.html").write_text(html, encoding="utf-8")
    print(len(SHOTS), "pages in", d)
