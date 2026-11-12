#!/usr/bin/env python3
"""«التطبيقات المعطّلة» (4.3.1, design mockups only, no app code): the settings row beside «تخلّص من واجهة المصنع», and the page that lists the apps turned off on the TV, with the apps chosen and the one
confirmation before turning them on.   python3 design/431-disabled-apps/disabled.py /tmp/hd && node design/43/render.js /tmp/hd design/431-disabled-apps
Rules as design/43: no pills (12 to 24 px radius), no «·» or «•», 8-scale spacing with 16 px inside a box, safe area x 96-1824, y 54-1026, Latin digits. The real fonts are private: DejaVu Sans stands in;
the app icons are stand-in squares (the real list shows each app's own icon where Android gives one, else its first letter)."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "43"))
import gen
from gen import page, ic
from clean import row

def settings_list():
    rows = [row("tv", "اجعل UrTV الشاشة الرئيسية", "✓ UrTV هو الشاشة الرئيسية"),
            row("shield", "تخلّص من واجهة المصنع", "عطّل واجهة المصنع وخلّ UrTV هو الأساس، وبضغطة ترجع", "مفعّل"),
            row("list", "التطبيقات المعطّلة", "شغّل من جديد تطبيقًا عطّلته أنت أو UrTV", "12 تطبيقًا", foc=True),
            row("tv", "UrTV النظيف", "الأساسيات فقط: الساعة والتاريخ وشريط التطبيقات", "مطفأ"),
            row("palette", "المظهر", "الشكل واللون والإضاءة", "زجاجي")]
    return f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">الشاشة الرئيسية</div><div class="abs col" style="left:352px;right:352px;top:152px;gap:16px">{"".join(rows)}</div>'

APPS = [("ت", "Tubi", "com.tubitv", True, True, "#B3473C"), ("م", "Movies Anywhere", "com.movies.anywhere", True, True, "#3C6EB3"), ("ر", "Rakuten TV", "tv.wuaki", True, False, "#B38A3C"),
        ("ه", "Hotstar", "in.startv.hotstar", False, False, "#3C8AB3"), ("غ", "Google TV Promos", "com.google.android.tv.promos", True, True, "#5B7B54"),
        ("ب", "Backdrop", "com.google.android.backdrop", True, False, "#7B54A0"), ("ك", "Kids Space", "com.google.android.apps.kids.home", False, False, "#A05474")]

def tick(on, foc=False):
    col = "var(--foct)" if foc else ("var(--accent)" if on else "var(--sub)")
    return f'<div class="btn" style="width:40px;height:40px;border-radius:12px;border:3px solid {"var(--foct)" if foc else "var(--sub)"};background:{(("var(--foct)" if foc else "var(--accent)") if on else "transparent")};color:{"var(--foc)" if foc else "var(--ink,#0F1115)"}">{ic("check", 28) if on else ""}</div>'

def app_row(letter, name, pkg, by_urtv, on, foc, color):
    tag = (f'<div style="border:2px solid {"var(--foct)" if foc else "var(--accent)"};border-radius:8px;padding:0 12px;height:32px;display:flex;align-items:center;font-size:18px;font-weight:600;color:{"var(--foct)" if foc else "var(--accent)"}">عطّلها UrTV</div>' if by_urtv else "")
    return f'''<div class="tile row{" foc" if foc else ""}" style="height:80px;padding:0 16px;gap:16px;border-radius:16px;color:var(--text)">{tick(on, foc)}
<div class="btn" style="width:48px;height:48px;border-radius:12px;background:{color};color:#fff;font-size:26px;font-weight:700">{letter}</div>
<div class="col" style="flex:1;gap:4px"><div style="font-size:24px;font-weight:600">{name}</div><div style="font-size:18px;opacity:.75;direction:ltr;text-align:right">{pkg}</div></div>{tag}</div>'''

def list_page(rows, footer, dim=False):
    head = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">التطبيقات المعطّلة</div><div class="abs sub" style="right:96px;top:112px;font-size:22px;width:1000px;line-height:1.5">اختر اللي تبي تشغّله من جديد. من هنا ما نعطّل شيء، بس نشغّل.</div>'
    body = f'<div class="abs col" style="right:96px;left:672px;top:176px;gap:8px">{"".join(rows)}</div>'
    side = f'''<div class="abs col panel" style="left:96px;top:176px;width:480px;padding:24px;gap:16px;border-radius:24px"><div style="font-size:28px;font-weight:600">12 تطبيقًا معطّلًا</div>
<div class="sub" style="font-size:20px;line-height:1.5">5 عطّلها UrTV في الإعداد. الباقي عطّلته أنت أو التلفزيون.</div><div class="sub" style="font-size:20px;line-height:1.5">«رجّع واجهة المصنع» يبقى صحيحًا: نحدّث سجلّه مع كل تطبيق تشغّله.</div></div>'''
    bar = f'<div class="abs row" style="right:96px;bottom:54px;gap:16px">{footer}</div>'
    return head + body + side + bar

def buttons(items):
    return "".join(f'<div class="btn {"foc" if f else "tile"}" style="height:64px;padding:0 32px;font-size:24px;font-weight:600;color:var(--text)">{t}</div>' for t, f in items)

def page_selected():
    rows = [app_row(l, n, p, u, on, i == 1, c) for i, (l, n, p, u, on, c) in enumerate([(a[0], a[1], a[2], a[3], a[4], a[5]) for i, a in enumerate(APPS)])]
    return page(list_page(rows, buttons([("شغّل 3 تطبيقات", True), ("رجوع", False)])), "glass")

def page_confirm():
    rows = [app_row(l, n, p, u, on, False, c) for (l, n, p, u, on, c) in [(a[0], a[1], a[2], a[3], a[4], a[5]) for i, a in enumerate(APPS)]]
    base = list_page(rows, buttons([("شغّل 3 تطبيقات", False), ("رجوع", False)]))
    w = 880
    dlg = f'''<div class="abs" style="inset:0;background:rgba(0,0,0,.55)"></div><div class="panel col abs" style="left:{(1920 - w) // 2}px;width:{w}px;top:280px;padding:32px;gap:16px;border-radius:24px;background:#1B1D22">
<div style="font-size:34px;font-weight:600">شغّل 3 تطبيقات؟</div>
<div style="font-size:24px;line-height:1.5">Movies Anywhere وGoogle TV Promos وTubi: تظهر في قائمة التطبيقات وتشتغل من جديد.</div>
<div style="font-size:22px;line-height:1.5" class="sub">نشغّلها من التلفزيون نفسه، وبعدها نعرض لك نتيجة كل تطبيق. ما نحذف شيء، وتقدر تعطّلها من جديد بنفس الإعداد الأصلي.</div>
<div class="row" style="gap:16px;margin-top:16px">{buttons([("إلغاء", True), ("نعم، شغّلها", False)])}</div></div>'''
    return page(base + dlg, "glass")

def build():
    return {"disabled-apps-01-settings-row": page(settings_list(), "glass"), "disabled-apps-02-page-and-confirm": page_confirm()}

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in build().items(): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(build()), "pages")
