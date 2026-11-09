#!/usr/bin/env python3
"""Phone page (design/43): «على الشاشة» as one group, bare text links become real rows/buttons. Before = the real page's own CSS and markup of those parts; after = the proposal.
python3 gen.py OUTDIR  (HTML), then: NODE_PATH=$(npm root -g) node render.js OUTDIR ..  The CSS is read from the app's own phone page, so before/after use the page's real tokens."""
import re, sys
from pathlib import Path
SRC = Path(__file__).resolve().parents[4] / "android/launcher/src/main/assets/phone/index.html"
if not SRC.exists(): SRC = Path("/home/user/UrTV/android/launcher/src/main/assets/phone/index.html")
css = re.search(r"<style>(.*?)</style>", SRC.read_text(encoding="utf-8"), re.S).group(1)
P = {
 "pencil": '<path d="M4 20l1-4L16.5 4.5a2 2 0 013 0l.5.5a2 2 0 010 3L8.5 19z"/><path d="M14.5 6.5l3 3"/>',
 "image": '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><circle cx="9" cy="10" r="1.8"/><path d="M4 18l5.5-5 4 3.5 3-2.5 4 4"/>',
 "chat": '<path d="M4 5.5A2.5 2.5 0 016.5 3h11A2.5 2.5 0 0120 5.5v8a2.5 2.5 0 01-2.5 2.5H11l-4.5 4v-4h0A2.5 2.5 0 014 13.5z"/>',
 "wallet": '<rect x="3" y="6" width="18" height="13" rx="2.5"/><path d="M3 10h18M16 14.5h2"/>',
 "sun": '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8"/>',
 "moon": '<path d="M20 14.5A8.5 8.5 0 019.5 4 8.5 8.5 0 1020 14.5z"/>',
 "globe": '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
 "chev": '<path d="M15 5l-7 7 7 7"/>',
 "pin": '<path d="M12 21s-6.5-6-6.5-11a6.5 6.5 0 0113 0c0 5-6.5 11-6.5 11z"/><circle cx="12" cy="10" r="2.3"/>',
}
def ic(n, cls=""): return f'<svg class="{cls}" viewBox="0 0 24 24">{P[n]}</svg>'
T = {
 "ar": dict(dir="rtl", lang="ar", scr="على الشاشة", msg="رسالة على الشاشة", ph="اكتب رسالة للتلفزيون", send="اعرضها على التلفزيون", draw="ارسم", drawh="اكتب بإصبعك على شاشة التلفزيون", photos="صوري", photosh="خلفيات وصور التلفزيون", quick="إعدادات سريعة", wallet="محفظتي", light="الوضع الفاتح", dark="الوضع الداكن", other="English", city="المدينة", citysub="الرياض", kb="رسالة على الشاشة"),
 "en": dict(dir="ltr", lang="en", scr="On the screen", msg="Message on the screen", ph="Write a message for the TV", send="Show it on the TV", draw="Draw", drawh="Write with your finger on the TV screen", photos="My photos", photosh="The TV's wallpapers and photos", quick="Quick settings", wallet="My wallet", light="Light mode", dark="Dark mode", other="العربية", city="City", citysub="Riyadh", kb="Message on the screen"),
}
def before(t, theme):
    mode = t["light"] if theme == "dark" else t["dark"]
    return f'''<div class="cap">{"قبل" if t["lang"]=="ar" else "Before"}</div>
<div class="card" id="msgCard"><div class="hd"><h2>{t["msg"]}</h2><a class="lnk" href="#">{t["draw"]}</a></div>
<input value="" placeholder="" aria-label="m"><button class="gold wide">{t["send"]}</button></div>
<div class="hd"><h2 class="h">{t["quick"]}</h2><a class="lnk" href="#">{t["wallet"]}</a><a class="lnk" href="#">{t["photos"]}</a><button class="lnk">{mode}</button><button class="lnk" lang="{'en' if t['lang']=='ar' else 'ar'}">{t["other"]}</button></div>
<div class="card"><div class="ch"><span class="ic">{ic("pin")}</span><div><h2>{t["city"]}</h2><p>{t["citysub"]}</p></div></div></div>'''
def after(t, theme):
    mode = t["light"] if theme == "dark" else t["dark"]
    sun = "sun" if theme == "dark" else "moon"
    row = lambda i, a, b: f'<button class="rowbtn">{ic(i, "ri")}<span class="rt"><b>{a}</b><small>{b}</small></span>{ic("chev", "rc")}</button>'
    return f'''<div class="cap">{"بعد" if t["lang"]=="ar" else "After"}</div>
<div class="card grp" id="msgCard"><div class="ch"><span class="ic">{ic("chat")}</span><h2>{t["scr"]}</h2></div>
<input value="" placeholder="{t["ph"]}" aria-label="m"><button class="gold wide">{t["send"]}</button>
<div class="rows">{row("pencil", t["draw"], t["drawh"])}{row("image", t["photos"], t["photosh"])}</div></div>
<div class="hd"><h2 class="h">{t["quick"]}</h2></div>
<div class="row two"><button class="chipbtn">{ic(sun)}<span>{mode}</span></button><button class="chipbtn" lang="{'en' if t['lang']=='ar' else 'ar'}">{ic("globe")}<span>{t["other"]}</span></button></div>
<div class="rows solo">{row("wallet", t["wallet"], "")}</div>
<div class="card"><div class="ch"><span class="ic">{ic("pin")}</span><div><h2>{t["city"]}</h2><p>{t["citysub"]}</p></div></div></div>'''
EXTRA = """
body{max-width:none;padding:24px;display:flex;gap:24px;justify-content:center;align-items:flex-start}
.ph{width:360px;padding:0 16px 16px;position:relative}
.cap{font-size:14px;color:var(--dim);margin:0 0 8px;font-weight:700;border-bottom:1px solid var(--line);padding-bottom:6px}
.rows{display:flex;flex-direction:column;gap:8px;margin-top:8px}.rows.solo{margin:8px 0}
.rowbtn{width:100%;min-height:56px;justify-content:flex-start;gap:12px;padding:8px 12px;border-radius:16px;text-align:start}
.rowbtn .ri{color:var(--gold)}.rowbtn .rt{flex:1;min-width:0;display:flex;flex-direction:column;align-items:flex-start;line-height:1.3}
.rowbtn .rt b{font-weight:500;font-size:16px}.rowbtn .rt small{font-size:13px;color:var(--dim)}
.rowbtn .rc{width:20px;height:20px;color:var(--dim)}[dir=rtl] .rowbtn .rc{transform:none}[dir=ltr] .rowbtn .rc{transform:scaleX(-1)}
.row.two{margin:8px 0}.chipbtn{min-height:52px;border-radius:16px;gap:8px;white-space:nowrap}
.grp input{margin-top:4px}
.grp .ch{margin-bottom:10px}
"""
def page(lang, theme):
    t = T[lang]
    return f'''<!doctype html><html lang="{t["lang"]}" dir="{t["dir"]}" data-theme="{theme}"><head><meta charset="utf-8"><meta name="viewport" content="width=360"><style>{css}{EXTRA}
html{{background:var(--bg)}}</style></head><body><div class="ph">{before(t, theme)}</div><div class="ph">{after(t, theme)}</div></body></html>'''
if __name__ == "__main__":
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    for lang in ("ar", "en"):
        for theme in ("dark", "light"):
            (out / f"phone-{lang}-{theme}.html").write_text(page(lang, theme), encoding="utf-8")
