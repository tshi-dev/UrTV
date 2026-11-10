#!/usr/bin/env python3
"""«توحيد المظهر» / Match the design (design/43, mockups only): one switch in «المظهر» that makes the three home surfaces (the upper-left quick buttons, the cards, the apps bar) one
material: the same fill, tint, opacity («شفافية الشريط»), corner radius, line, shadow, focus style and accent, in every look, dark and light. Off: each is set on its own.
   python3 design/43/sync.py /tmp/hs && node design/43/render.js /tmp/hs design/43      (names sync-*)
Rules: no pills (16 to 24 px radius), no «·», 8-scale, 16 px inside a box, safe area x 96-1824, y 54-1026. Real fonts are private: DejaVu Sans stands in."""
import re, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen, clean
from gen import page, ic, LOOKS

# the frosted light glass (GlassStyle.LIGHT): white panel, dark ink
LOOKS["glass_light"] = dict(ar="زجاجي فاتح", new=False, dark=False, veil="rgba(245,246,248,.45)", panel="rgba(255,255,255,.62)", tile="rgba(255,255,255,.50)", line="rgba(20,22,26,.12)",
                            text="#14161A", sub="#3A3E46", accent="#0B5E51", foc="#14161A", foct="#FFFFFF", ring="#14161A", blur=24)

RADIUS, SHADOW = 24, "0 8px 24px rgba(0,0,0,.28)"

def surf(html, name, tokens):
    """the first panel of `html` becomes the surface `name` (q = quick buttons, c = cards, b = apps bar) with its own tokens"""
    html = html.replace('class="panel', f'class="surf s{name}', 1)
    html = re.sub(r"border-radius:\d+px;?", "", html, count=1)
    return html.replace('style="', f'style="{tokens}', 1)

def tok(fill, line, radius=RADIUS, shadow=SHADOW):
    return f"--sf:{fill};--sl:{line};--sr:{radius}px;--ss:{shadow};"

CSS = ".surf{background:var(--sf);border:1px solid var(--sl);border-radius:var(--sr);box-shadow:var(--ss);backdrop-filter:blur(var(--blur))}"

def home(look, accent=None, sync=True, pct=100):
    L = gen.css(look, accent)
    a = pct / 100
    def op(c):   # «شفافية الشريط»: one opacity for the surface's fill, as a share of the look's own
        m = re.match(r"rgba\((\d+),(\d+),(\d+),([\d.]+)\)", c)
        if m: return f"rgba({m[1]},{m[2]},{m[3]},{float(m[4]) * a:.3f})"
        h = c.lstrip("#"); return f"rgba({int(h[0:2], 16)},{int(h[2:4], 16)},{int(h[4:6], 16)},{a:.3f})"
    if sync:
        t = tok(op(L["panel"]), L["line"])
        tq = tc = tb = t
    else:   # off: the buttons and the cards stay on the look's glass; the bar is its own, a deliberately different one
        tq = tc = tok(op(L["panel"]), L["line"])
        tb = tok("#202124", "rgba(240,166,74,.55)", 16, "none")
    body = surf(gen.quick_row(look, None), "Q", tq) + gen.greeting(look) + surf(gen.prayer_card(look), "C", tc) + surf(gen.tray(look, 3, 7, False), "B", tb)
    # the focused tile of the bar and the focused button: one focus style (solid fill + the accent's ring) in both
    return page(body, look, accent, extra=CSS)

def appearance(sync_on):
    def r(icon, title, sub, val="", foc=False, on=None): return clean.row(icon, title, sub, val, foc, on)
    rows = [r("palette", "الشكل", "الزجاج والفحمي والكحلي والبقية", "زجاجي"), r("sun", "اللون", "لون التركيز والأزرار", "كهرماني"),
            r("sun", "الإضاءة", "تلقائي أو فاتح أو داكن", "تلقائي"),
            r("card", "توحيد المظهر", "الأزرار والبطاقات وشريط التطبيقات بمادة وحدة", on=sync_on, foc=True)]
    if not sync_on:
        subs = [("phone", "الأزرار العلوية", "شفافية ٧٠٪، تعبئة زجاج"), ("card", "البطاقات", "شفافية ٧٠٪، تعبئة زجاج"), ("grid", "شريط التطبيقات", "شفافية ١٠٠٪، تعبئة فحمي")]
        for ic_, t, v in subs:
            rows.append(f'<div style="padding-inline-start:64px">{clean.row(ic_, t, "كل واحد بشفافيته وتعبئته", v)}</div>')
    hint = ("مفعّل: غيّر شفافية الشريط أو اللون فيتغيّر الثلاثة معًا." if sync_on else "متوقف: لكل سطح شفافيته وتعبئته. الشكل واللون والتركيز يبقون للكل.")
    body = (f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">المظهر</div>'
            f'<div class="abs sub" style="right:96px;top:112px;font-size:22px">{hint}</div>'
            f'<div class="abs col" style="left:352px;right:352px;top:176px;gap:16px">{"".join(rows)}</div>'
            f'<div class="abs row" style="left:96px;bottom:54px"><div class="panel btn" style="height:56px;padding:0 24px;font-size:22px;border-radius:16px;gap:8px">{ic("chev", 24)}رجوع</div></div>')
    return page(body, "glass").replace("مطفأ", "متوقف")

SHOTS = {
    "sync-01-appearance-row-on": appearance(True),
    "sync-02-appearance-row-off": appearance(False),
    "sync-03-home-sync-glass": home("glass"),
    "sync-04-home-sync-graphite": home("graphite"),
    "sync-05-home-sync-light": home("glass_light"),
    "sync-06-home-sync-off-different-bar": home("glass", sync=False, pct=70),
    "sync-07-home-sync-glass-70": home("glass", pct=70),
}

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True)
    for n, h in SHOTS.items(): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(SHOTS), "pages in", d)
