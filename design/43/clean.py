#!/usr/bin/env python3
"""«UrTV النظيف» / Clean UrTV (design/43, mockups only): the settings row and its confirm, the clean home (Arabic dark and light, English dark), the customise sheet, and the
stock-interface step with its warning.   python3 design/43/clean.py /tmp/hc && node design/43/render.js /tmp/hc design/43   (names clean-*)
Rules: no pills (16 to 24 px radius), no «·», 8-scale, 16 px inside a box, safe area x 96-1824, y 54-1026. Real fonts are private: DejaVu Sans stands in."""
import re, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen
from gen import page, ic, APPS

def mirror(html):
    """English: left to right, the sides swapped (the screen's start edge is the left one)"""
    html = html.replace('lang="ar" dir="rtl"', 'lang="en" dir="ltr"').replace("direction:rtl", "direction:ltr")
    return re.sub(r"\b(left|right)\b(?=:)", lambda m: "right" if m.group(1) == "left" else "left", html)

def clock_block(en=False):
    t = ("Wednesday, 24 Jumada al-Ula 1448" if en else "الأربعاء، ٢٤ جمادى الأولى ١٤٤٨ هـ")
    g = ("Good morning" if en else "صباح الخير")
    n = ("Next prayer: Asr 2:46 PM, in 2 h 20 min" if en else "الصلاة القادمة: العصر ٢:٤٦ م، بعد ساعتين و٢٠ دقيقة")
    tm = "9:57" if en else "٩:٥٧"
    return f"""<div class="abs col" style="right:96px;top:54px;align-items:flex-end;gap:8px;text-shadow:0 2px 14px rgba(0,0,0,.35)">
<div class="row" style="gap:12px"><span class="sub" style="display:flex">{ic("wifi", 24)}</span><div class="acc" style="font-size:32px;font-weight:600">{g}</div></div>
<div style="font-size:176px;font-weight:300;line-height:1.05;margin-top:8px">{tm}</div>
<div class="sub" style="font-size:28px">{t}</div>
<div style="font-size:28px;margin-top:16px;font-weight:600">{n}</div></div>"""

def settings_btn(foc=False):
    return f'<div class="panel row" style="position:absolute;left:96px;top:54px;height:72px;padding:8px;border-radius:20px"><div class="btn{" foc" if foc else ""}" style="width:56px;height:56px;color:var(--text)">{ic("sliders", 28)}</div></div>'

def apps_bar(n=6, focus=2):
    return gen.tray("glass", focus, n)

def clean_home(look, en=False, accent=None):
    body = settings_btn() + clock_block(en) + apps_bar(6, 2)
    html = page(body, look, accent)
    return mirror(html) if en else html

def row(icon, title, sub, val="", foc=False, on=None):
    tog = ""
    if on is not None:
        tog = f'<div class="btn" style="min-width:112px;height:48px;padding:0 16px;border-radius:16px;background:{"var(--accent)" if on else "var(--tile)"};color:{"var(--foct)" if on else "var(--text)"};font-size:22px;font-weight:600">{"مفعّل" if on else "مطفأ"}</div>'
    return f'''<div class="tile row{" foc" if foc else ""}" style="height:88px;padding:0 24px;gap:16px;border-radius:16px;color:var(--text)">
<div class="btn" style="width:48px;height:48px">{ic(icon, 32)}</div><div class="col" style="flex:1;gap:4px"><div style="font-size:26px;font-weight:600">{title}</div><div style="font-size:20px;opacity:.8">{sub}</div></div>
<div style="font-size:22px;opacity:.85">{val}</div>{tog}</div>'''

def settings_list(foc_clean=True):
    rows = [row("tv", "UrTV النظيف", "الأساسيات فقط: الساعة والصلاة والتطبيقات. ضغطة وحدة ترجعك للشاشة الكاملة", on=False, foc=foc_clean),
            row("palette", "المظهر", "الشكل واللون والإضاءة", "زجاجي"), row("card", "البطاقات", "ما يظهر في الشاشة الرئيسية وشاشة التوقف", "٣ بطاقات"),
            row("phone", "تحكم من الجوال", "ريموت ونص وإعدادات من جوالك", "مطفأ"), row("shield", "الرقابة الأبوية", "رقم سري وأقفال", "مطفأ")]
    return f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">الإعدادات</div><div class="abs col" style="left:352px;right:352px;top:152px;gap:16px">{"".join(rows)}</div>'

def dialog(title, lines, buttons, warn=None, w=880, top=None):
    ls = "".join(f'<div style="font-size:24px;line-height:1.5;display:flex;gap:12px;align-items:flex-start"><span class="acc" style="flex:none;margin-top:6px">{ic("check", 24)}</span><span>{l}</span></div>' for l in lines)
    wr = f'<div class="row" style="gap:16px;align-items:flex-start;padding:16px;border-radius:16px;border:2px solid #FFB4A0;color:#FFD2C6;font-size:22px;line-height:1.5;margin-top:16px"><span style="flex:none;display:flex">{ic("shield", 32)}</span><span>{warn}</span></div>' if warn else ""
    bs = "".join(f'<div class="btn {"foc" if f else "tile"}" style="height:64px;padding:0 32px;font-size:24px;font-weight:600;color:var(--text)">{t}</div>' for t, f in buttons)
    return f'''<div class="abs" style="inset:0;background:rgba(0,0,0,.55)"></div>
<div class="panel col abs" style="left:{(1920 - w) // 2}px;width:{w}px;top:{top if top is not None else 200}px;padding:32px;gap:16px;border-radius:24px;background:#1B1D22"><div style="font-size:34px;font-weight:600">{title}</div>{ls}{wr}<div class="row" style="gap:16px;margin-top:16px">{bs}</div></div>'''

def confirm_screen():
    return page(settings_list(True) + dialog("تفعيل «UrTV النظيف»؟", ["تبقى الساعة والتاريخ والصلاة القادمة وشريط التطبيقات، وزر الإعدادات.", "تختفي الأزرار الإضافية ومركز التحكم والتنبيهات والصفوف الزائدة.", "ما يُحذف شيء: كل ما أخفيناه يرجع بضغطة، من «الإعدادات ← UrTV النظيف»."],
                [("فعّل", True), ("تخصيص", False), ("إلغاء", False)], top=300), "glass")

def settings_screen():
    return page(settings_list(True), "glass")

def tick(on, foc=False):
    return f'<div class="btn" style="width:40px;height:40px;border-radius:12px;border:3px solid {"var(--foct)" if foc else "var(--sub)"};background:{("var(--accent)" if not foc else "var(--foct)") if on else "transparent"};color:{"var(--foc)" if foc else "var(--foct)"}">{ic("check", 28, 2.6) if on else ""}</div>'

def custom_row(icon, name, note, on, foc=False, locked=False):
    return f'''<div class="tile row{" foc" if foc else ""}" style="height:72px;padding:0 16px;gap:16px;border-radius:16px;color:var(--text)">{tick(on, foc)}<div class="btn" style="width:40px;height:40px">{ic(icon, 28)}</div>
<div class="col" style="flex:1"><div style="font-size:24px;font-weight:600">{name}</div><div style="font-size:18px;opacity:.8">{note}</div></div></div>'''

def customise_screen():
    rows = [custom_row("grid", "شريط التطبيقات", "دائمًا ظاهر", True, locked=True), custom_row("card", "بطاقة واحدة", "مواقيت اليوم (تُختار من المظهر ← البطاقات)", True, foc=True),
            custom_row("wifi", "علامة الواي فاي", "بجنب التحية", True), custom_row("sun", "التحية", "صباح الخير", False), custom_row("sliders", "زر الإعدادات", "الزر الوحيد فوق", True)]
    pv = gen.mini("glass", None, 480, 270, False)
    body = f'''<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">تخصيص «UrTV النظيف»</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">اختر القليل اللي يبقى. الساعة والتاريخ والصلاة القادمة دائمًا.</div>
<div class="abs col" style="right:96px;width:880px;top:176px;gap:8px">{"".join(rows)}</div>
<div class="abs col" style="left:96px;top:176px;gap:16px;align-items:center"><div style="border-radius:16px;overflow:hidden">{clean_preview()}</div></div>
<div class="abs row" style="right:96px;bottom:54px;gap:16px"><div class="btn foc" style="height:64px;padding:0 32px;font-size:24px;font-weight:600">تم</div><div class="btn panel" style="height:64px;padding:0 32px;font-size:24px">رجّع الشاشة الكاملة</div></div>'''
    return page(body, "glass")

def clean_preview():
    L = gen.css("glass")
    inner = f'''<div style="position:absolute;left:0;top:0;width:1920px;height:1080px;transform:scale({480/1920});transform-origin:top left;--panel:{L['panel']};--tile:{L['tile']};--line:{L['line']};--text:{L['text']};--sub:{L['sub']};--accent:{L['accent']};--foc:{L['foc']};--foct:{L['foct']};--ring:{L['ring']};--blur:{L['blur']}px;color:{L['text']};font-family:DejaVu Sans;direction:rtl">
<div style="position:absolute;inset:0;background:url('{gen.BG.as_uri()}') center/cover"></div><div style="position:absolute;inset:0;background:{L['veil']}"></div>{settings_btn()}{clock_block()}{apps_bar(6, -1)}</div>'''
    return f'<div style="position:relative;width:480px;height:270px;overflow:hidden;border-radius:16px;box-shadow:0 0 0 1px rgba(255,255,255,.2)">{inner}</div>'

def stock_screen():
    body = f'''<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">UrTV النظيف</div>''' + dialog("تخلّص من واجهة المصنع؟",
        ["نعطّل (لا نحذف) واجهة المصنع وتطبيقاتها الزائدة من قائمتنا الآمنة، فيفتح التلفزيون على UrTV مباشرة.", "الواي فاي والبلوتوث والصوت والإعدادات وكل ما في قائمة الخصائص المحمية يبقى كما هو.", "نحفظ قائمة بكل ما عطّلناه، و«رجّع واجهة المصنع» يرجّعه كله بضغطة."],
        [("عطّل", False), ("إلغاء", True)],
        warn="تحتاج هذه الخطوة تفعيل «تصحيح USB» على هذا التلفزيون من إعدادات المطوّرين. لو توقفت UrTV مرتين عند التشغيل نرجّع واجهة المصنع تلقائيًا.", w=1000, top=130)
    return page(body, "glass")

def build():
    S = {}
    S["clean-01-settings-row"] = settings_screen()
    S["clean-02-confirm"] = confirm_screen()
    S["clean-03-home-ar-dark"] = clean_home("glass")
    S["clean-04-home-ar-light"] = clean_home("sand")
    S["clean-05-home-en-dark"] = clean_home("glass", en=True)
    S["clean-06-customise"] = customise_screen()
    S["clean-07-stock-removal"] = stock_screen()
    return S

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in build().items(): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(build()), "pages")
