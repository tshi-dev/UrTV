"""4.3 round 2: genuinely different home looks (own card shape, bar style, clock style, focus style), the settings row, and the family games. Imported by gen.py."""
from pathlib import Path
import gen as G

ic, BG = G.ic, G.BG
WALL = f"url('{BG.as_uri()}') center/cover"
APPS = G.APPS


def stage(bg, inner, color="#fff"):
    return f'<div style="position:absolute;left:0;top:0;width:1920px;height:1080px;overflow:hidden;color:{color};direction:rtl;font-family:\'DejaVu Sans\',sans-serif;background:{bg}">{inner}</div>'


def pat(color, op=0.10, size=48):
    """a quiet eight-point star lattice (the geometric ground of the sand/wood look); the SVG is percent-encoded so it can sit inside a style attribute"""
    from urllib.parse import quote
    svg = (f"<svg xmlns='http://www.w3.org/2000/svg' width='{size}' height='{size}' viewBox='0 0 48 48' fill='none' stroke='{color}' stroke-opacity='{op}' stroke-width='1.5'>"
           "<path d='M24 4l5.5 13.5L44 24l-14.5 6.5L24 44l-5.5-13.5L4 24l14.5-6.5z'/><circle cx='24' cy='24' r='6'/></svg>")
    return "url('data:image/svg+xml;utf8," + quote(svg, safe="") + "')"


# ---------------------------------------------------------------- 1. هدوء: calm, type only
def calm(focus=2):
    quick = "".join(f'<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;opacity:{1 if i == 2 else .7}">{ic(n, 28, 1.4)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    apps = ""
    for i, (c, g, name) in enumerate(APPS[:7]):
        f = i == focus
        apps += f'<div style="display:flex;flex-direction:column;align-items:center;gap:16px;flex:1;opacity:{1 if f else .62}"><div style="color:#fff">{ic(g, 44 if f else 36, 1.3)}</div><div style="font-size:{26 if f else 22}px;font-weight:{500 if f else 300};padding-bottom:8px;border-bottom:3px solid {"#fff" if f else "transparent"}">{name}</div></div>'
    rows = "".join(f'<div style="display:flex;justify-content:space-between;font-size:24px;font-weight:300;height:40px;{"font-weight:600;border-bottom:1px solid #fff" if i == 3 else "opacity:.7"}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate([("الفجر", "٤:٣٨"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١")][:4]))
    inner = (f'<div style="position:absolute;left:96px;top:54px;display:flex;gap:8px">{quick}</div>'
             '<div style="position:absolute;right:96px;top:54px;font-size:24px;font-weight:300;text-align:right">صباح الخير · الرياض ٢٨°</div>'
             '<div style="position:absolute;right:96px;top:240px;text-align:right"><div style="font-size:240px;font-weight:100;line-height:1;letter-spacing:0">٩:٥٧</div><div style="font-size:28px;font-weight:300;opacity:.85;margin-top:8px">الأربعاء ٢٤ جمادى الأولى ١٤٤٨</div></div>'
             f'<div style="position:absolute;left:96px;top:320px;width:360px">{rows}</div>'
             f'<div style="position:absolute;left:96px;right:96px;bottom:72px;display:flex">{apps}</div>')
    return stage(f"linear-gradient(rgba(0,0,0,.62),rgba(0,0,0,.62)),{WALL}", inner)


# ---------------------------------------------------------------- 2. نقش: warm sand and wood, geometric ground
def naqsh(focus=3):
    cream, brown, gold, ink = "#F4EAD5", "#3B2A1E", "#C79A4B", "#2B1D13"
    quick = "".join(f'<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;border-radius:8px;{"background:" + gold + ";color:" + ink if i == 2 else "color:" + cream}">{ic(n, 28, 1.8)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    rows = "".join(f'<div style="display:flex;justify-content:space-between;height:32px;padding:0 8px;font-size:20px;{"background:rgba(199,154,75,.28);font-weight:700;color:" + ink if i == 3 else "color:#5A4634"}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate([("الفجر", "٤:٣٨"), ("الشروق", "٥:٥٩"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١"), ("العشاء", "٦:٤١")]))
    card = (f'<div style="position:absolute;left:96px;top:176px;width:480px;height:304px;background:{cream};border:2px solid {gold};border-radius:8px;overflow:hidden;color:{ink}">'
            f'<div style="height:40px;background:{brown} {pat(gold, .55, 40)};border-bottom:2px solid {gold}"></div>'
            f'<div style="padding:8px 16px"><div style="display:flex;justify-content:space-between;height:40px;align-items:center;font-size:24px;font-weight:700"><span>مواقيت اليوم</span><span style="font-size:20px;color:#8A5A1E">العصر بعد ساعتين</span></div>{rows}</div></div>')
    tiles = ""
    for i, (c, g, name) in enumerate(APPS[:7]):
        f = i == focus
        tiles += (f'<div style="flex:1;height:136px;background:{cream};border-radius:8px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:{ink};'
                  f'border:{4 if f else 2}px solid {gold};{"box-shadow:0 0 0 4px rgba(199,154,75,.45);transform:translateY(-8px);" if f else ""}"><div style="width:56px;height:56px;border-radius:8px;background:{brown};color:{gold};display:flex;align-items:center;justify-content:center">{ic(g, 28)}</div><div style="font-size:20px">{name}</div></div>')
    tray_ = f'<div style="position:absolute;left:96px;right:96px;bottom:54px;height:176px;background:{brown} {pat(gold, .22, 56)};border:2px solid {gold};border-radius:8px;padding:20px 16px 20px;display:flex;gap:16px;align-items:flex-end">{tiles}</div>'
    inner = (f'<div style="position:absolute;left:96px;top:54px;height:72px;background:{brown};border:2px solid {gold};border-radius:8px;padding:8px;display:flex;gap:8px">{quick}</div>'
             f'<div style="position:absolute;right:96px;top:54px;text-align:right;text-shadow:0 2px 12px rgba(40,24,10,.6)"><div style="font-size:30px;font-weight:700;color:{gold}">صباح الخير</div><div style="font-size:120px;font-weight:700;line-height:1.05;color:{cream}">٩:٥٧</div>'
             f'<div style="height:4px;width:360px;margin:0 0 8px auto;background:{gold} {pat(ink, .0, 8)};border-radius:2px"></div><div style="font-size:22px;color:{cream}">الأربعاء، ٢٤ جمادى الأولى ١٤٤٨ هـ</div></div>' + card + tray_)
    return stage(f"linear-gradient(rgba(70,42,20,.58),rgba(70,42,20,.58)),{WALL}", inner)


# ---------------------------------------------------------------- 3. سينما: big artwork
def cinema(focus=2):
    posters = [("#7C2D12", "#F59E0B", "السفر الطويل"), ("#1E3A8A", "#60A5FA", "أرض الجبال"), ("#064E3B", "#34D399", "ليالي الصحراء"), ("#581C87", "#F0ABFC", "حكاية قديمة"), ("#7F1D1D", "#FCA5A5", "الرحلة")]
    cards = ""
    for i, (a, b, t) in enumerate(posters):
        f = i == focus
        w, h = (264, 396) if f else (232, 348)
        cards += (f'<div style="width:{w}px;height:{h}px;border-radius:16px;background:linear-gradient(160deg,{b},{a} 70%);position:relative;overflow:hidden;flex:none;'
                  f'{"box-shadow:0 0 0 4px #fff,0 24px 48px rgba(0,0,0,.6);" if f else "opacity:.85;"}"><div style="position:absolute;bottom:0;left:0;right:0;padding:16px;background:linear-gradient(transparent,rgba(0,0,0,.75));font-size:{26 if f else 22}px;font-weight:700">{t}</div></div>')
    quick = "".join(f'<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;{"background:rgba(255,255,255,.22);border-radius:16px" if i == 2 else "opacity:.8"}">{ic(n, 28, 1.6)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    inner = (f'<div style="position:absolute;left:96px;top:54px;display:flex;gap:8px">{quick}</div>'
             '<div style="position:absolute;right:96px;top:54px;text-align:right"><div style="font-size:56px;font-weight:300">٩:٥٧</div><div style="font-size:20px;opacity:.8">الأربعاء ٢٤ جمادى الأولى</div></div>'
             '<div style="position:absolute;right:96px;top:220px;width:900px;text-align:right"><div style="font-size:24px;color:#F59E0B;font-weight:600">أكمل المشاهدة</div><div style="font-size:88px;font-weight:700;line-height:1.1;margin:8px 0">ليالي الصحراء</div><div style="font-size:26px;opacity:.85;line-height:1.6">الحلقة ٤ · بقي ٢٣ دقيقة</div></div>'
             f'<div style="position:absolute;left:96px;right:96px;bottom:54px;display:flex;gap:24px;align-items:flex-end;justify-content:flex-start">{cards}</div>')
    return stage(f"linear-gradient(90deg,rgba(0,0,0,.2),rgba(0,0,0,.78) 55%,#000),{WALL}", inner)


# ---------------------------------------------------------------- 4. باستيل: soft light family
def pastel(focus=2):
    ink = "#3A3350"
    pas = ["#FFD9C7", "#D9E4FF", "#D6F2E4", "#FFF0B8", "#EAD7FF", "#FFD6E7", "#CFF1F5"]
    quick = "".join(f'<div style="width:56px;height:56px;border-radius:28px;background:#fff;color:{ink};display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(80,60,140,.14);{"outline:4px solid #7C6CF0;" if i == 2 else ""}">{ic(n, 28, 2)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    rows = "".join(f'<div style="display:flex;justify-content:space-between;height:36px;padding:0 16px;font-size:22px;border-radius:12px;align-items:center;{"background:#EAD7FF;font-weight:700" if i == 3 else ""}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate([("الفجر", "٤:٣٨"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١")][:4]))
    card = f'<div style="position:absolute;left:96px;top:176px;width:480px;height:272px;background:#fff;border-radius:32px;padding:24px;box-shadow:0 12px 32px rgba(80,60,140,.16);color:{ink}"><div style="font-size:28px;font-weight:700;margin-bottom:16px">مواقيت اليوم</div>{rows}</div>'
    tiles = ""
    for i, (c, g, name) in enumerate(APPS[:7]):
        f = i == focus
        tiles += (f'<div style="flex:1;height:{176 if f else 160}px;border-radius:32px;background:{pas[i]};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;color:{ink};'
                  f'{"box-shadow:0 0 0 5px #7C6CF0,0 20px 40px rgba(80,60,140,.28);transform:translateY(-12px);" if f else "box-shadow:0 6px 16px rgba(80,60,140,.10);"}"><div style="width:64px;height:64px;border-radius:32px;background:#fff;color:{c};display:flex;align-items:center;justify-content:center">{ic(g, 32, 2)}</div><div style="font-size:22px;font-weight:600">{name}</div></div>')
    inner = (f'<div style="position:absolute;left:96px;top:54px;display:flex;gap:16px">{quick}</div>'
             f'<div style="position:absolute;right:96px;top:54px;text-align:right;color:{ink}"><div style="font-size:30px;font-weight:700;color:#7C6CF0">صباح الخير</div><div style="font-size:120px;font-weight:700;line-height:1.05">٩:٥٧</div><div style="font-size:22px;opacity:.8">الأربعاء، ٢٤ جمادى الأولى ١٤٤٨ هـ</div></div>' + card +
             f'<div style="position:absolute;left:96px;right:96px;bottom:54px;display:flex;gap:16px;align-items:flex-end;height:200px">{tiles}</div>')
    return stage("linear-gradient(150deg,#FFE9DC,#EBE6FF 50%,#D9F3EA)", inner, ink)


# ---------------------------------------------------------------- 5. حاد: bold high contrast
def bold(focus=3):
    y = "#FFE600"
    quick = "".join(f'<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;border:4px solid {"#000" if i == 2 else "#fff"};{"background:" + y + ";color:#000" if i == 2 else ""}">{ic(n, 28, 2.6)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    rows = "".join(f'<div style="display:flex;justify-content:space-between;height:32px;padding:0 8px;font-size:22px;font-weight:700;{"background:" + y + ";color:#000" if i == 3 else ""}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate([("الفجر", "٤:٣٨"), ("الشروق", "٥:٥٩"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١"), ("العشاء", "٦:٤١")]))
    card = f'<div style="position:absolute;left:96px;top:176px;width:480px;height:304px;border:4px solid #fff"><div style="height:48px;background:{y};color:#000;font-size:26px;font-weight:800;display:flex;align-items:center;padding:0 16px">مواقيت اليوم</div><div style="padding:8px 8px">{rows}</div></div>'
    tiles = ""
    for i, (c, g, name) in enumerate(APPS[:7]):
        f = i == focus
        tiles += f'<div style="flex:1;height:136px;border:4px solid {"#000" if f else "#fff"};{"background:" + y + ";color:#000;" if f else ""}display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;font-size:22px;font-weight:800">{ic(g, 40, 2.8)}{name}</div>'
    inner = (f'<div style="position:absolute;left:96px;top:54px;display:flex;gap:8px">{quick}</div>'
             f'<div style="position:absolute;right:96px;top:54px;text-align:right"><div style="font-size:176px;font-weight:900;line-height:1;color:{y}">٩:٥٧</div><div style="font-size:28px;font-weight:700">الأربعاء ٢٤ جمادى الأولى ١٤٤٨</div></div>' + card +
             f'<div style="position:absolute;left:96px;right:96px;bottom:54px;display:flex;gap:16px;height:168px;align-items:center;border-top:4px solid #fff;padding-top:16px">{tiles}</div>')
    return stage("#000", inner)


# ---------------------------------------------------------------- 6. ليل: the late-hours look, amber only, dim
def night(focus=3):
    am, dim = "#D98A5F", "rgba(217,138,95,.55)"
    quick = "".join(f'<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;color:{am};border-radius:12px;{"outline:2px solid " + am if i == 2 else "opacity:.7"}">{ic(n, 28, 1.6)}</div>' for i, n in enumerate(["book", "chart", "phone", "wifi", "sliders"]))
    rows = "".join(f'<div style="display:flex;justify-content:space-between;height:32px;padding:0 8px;font-size:20px;color:{am};{"background:rgba(217,138,95,.14)" if i == 3 else "opacity:.6"}"><span>{a}</span><span>{b}</span></div>' for i, (a, b) in enumerate([("الفجر", "٤:٣٨"), ("الظهر", "١١:٥٢"), ("العصر", "٢:٤٦"), ("المغرب", "٥:١١"), ("العشاء", "٦:٤١")]))
    card = f'<div style="position:absolute;left:96px;top:176px;width:480px;height:256px;background:rgba(40,16,10,.55);border:1px solid rgba(217,138,95,.22);border-radius:16px;padding:16px"><div style="font-size:22px;color:{am};margin-bottom:8px">الصلاة القادمة بعد ساعتين</div>{rows}</div>'
    tiles = ""
    for i, (c, g, name) in enumerate(APPS[:7]):
        f = i == focus
        tiles += f'<div style="flex:1;height:136px;border-radius:16px;background:rgba(40,16,10,.55);border:{2 if f else 1}px solid {am if f else "rgba(217,138,95,.2)"};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:{am};font-size:20px;opacity:{1 if f else .7}">{ic(g, 36, 1.6)}{name}</div>'
    inner = (f'<div style="position:absolute;left:96px;top:54px;display:flex;gap:8px">{quick}</div>'
             f'<div style="position:absolute;right:96px;top:54px;text-align:right;color:{am}"><div style="font-size:22px;opacity:.8">تصبح على خير</div><div style="font-size:96px;font-weight:300;line-height:1.1">١٠:٤٢</div><div style="font-size:20px;opacity:.65">الأربعاء ٢٤ جمادى الأولى</div></div>' + card +
             f'<div style="position:absolute;left:96px;right:96px;bottom:54px;display:flex;gap:16px;height:136px">{tiles}</div>')
    return stage(f"linear-gradient(rgba(14,5,3,.93),rgba(14,5,3,.93)),{WALL}", inner, am)


VARIANTS = {"calm": ("هدوء", calm, "نص فقط، بلا صناديق: الساعة نحيلة وكبيرة، والتركيز خط تحت الاسم"),
            "naqsh": ("نقش", naqsh, "رمل وخشب ونقش هندسي هادئ، بطاقات بزوايا صغيرة وإطار ذهبي"),
            "cinema": ("سينما", cinema, "ملصقات كبيرة بلا شريط، والمركَّز يكبر بإطار أبيض"),
            "pastel": ("باستيل", pastel, "فاتح ناعم للعائلة: بطاقات مستديرة ملوّنة، والتركيز يرتفع"),
            "bold": ("حاد", bold, "أسود وأصفر بإطارات سميكة، والتركيز لون مقلوب"),
            "night": ("ليل", night, "كهرماني خافت فقط، للساعات المتأخرة بلا ضوء أبيض")}


def mini2(html, w):
    s = w / 1920
    h = round(w * 9 / 16)
    return f'<div style="position:relative;width:{w}px;height:{h}px;overflow:hidden;border-radius:16px;box-shadow:0 0 0 1px rgba(128,128,128,.35);flex:none"><div style="position:absolute;left:0;top:0;width:1920px;height:1080px;transform:scale({s});transform-origin:top left">{html}</div></div>'


def page_html(body, extra=""):
    return f"""<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><style>{G.BASE.replace('BGURL', BG.as_uri())}{extra}</style></head><body>{body}</body></html>"""


def looks_grid2():
    keys = ["glass", "calm", "naqsh", "cinema", "pastel", "bold", "night", "horizon"]
    tiles = []
    for k in keys:
        if k in VARIANTS:
            name, fn, note = VARIANTS[k]
            prev = mini2(fn(), 376)
        else:
            name = G.LOOKS[k]["ar"] + ("" if k == "horizon" else " · الافتراضي")
            note = "زجاج مدخّن كما هو الآن" if k == "glass" else "توهّج الأفق خلف الشريط، تركيز متوهّج"
            L = G.css(k)
            prev = G.mini(k, None, 376, 211, True)
        f = " foc" if k == "glass" else ""
        tiles.append(f'<div class="panel col{f}" style="width:408px;height:344px;padding:16px;gap:8px;border-radius:24px">{prev}<div style="font-size:24px;font-weight:600;margin-top:8px">{name}</div><div style="font-size:18px;opacity:.8;line-height:1.5">{note}</div></div>')
    body = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">الشكل</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">الزجاجي هو الافتراضي. كل شكل له بطاقاته وشريطه وساعته وتركيزه.</div>' \
           f'<div class="abs" style="left:96px;right:96px;top:176px;display:grid;grid-template-columns:repeat(4,1fr);gap:24px;justify-items:center">{"".join(tiles)}</div>'
    return G.page(body, "glass")


def full(key):
    return page_html(VARIANTS[key][1]())


# ---------------------------------------------------------------- the settings row
def settings_row(is_home):
    rows = [("تلاوة وأذان", "يعمل · الأذان على الشاشة", False), ("المظهر", "زجاجي · تلقائي", False), ("اجعل UrTV الشاشة الرئيسية", "✓ UrTV هو الشاشة الرئيسية" if is_home else "اضغط وكمّل إعداد تلفزيوني (دقيقتان)", True), ("تطبيقات مقترحة", "٨ تطبيقات", False), ("التحكم من الجوال", "مفعّل", False)]
    out = []
    for n, v, key in rows:
        foc = key and not is_home
        mark = f'<div class="btn" style="width:40px;height:40px;border-radius:12px;background:var(--accent);color:#14161A">{ic("check", 24, 2.6)}</div>' if key and is_home else (f'<div style="width:40px;height:40px">{ic("chev", 28)}</div>' if key else '<div style="width:40px;height:40px;opacity:.5">' + ic("chev", 28) + '</div>')
        out.append(f'<div class="tile row{" foc" if foc else ""}" style="height:88px;padding:0 24px;gap:16px;justify-content:space-between"><div class="col" style="gap:4px"><div style="font-size:26px;font-weight:600">{n}</div><div style="font-size:20px;opacity:.85">{v}</div></div>{mark}</div>')
    body = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">الشاشة الرئيسية</div>' \
           f'<div class="abs panel col" style="right:96px;left:96px;top:140px;padding:16px;gap:16px;border-radius:24px">{"".join(out)}</div>'
    return G.page(body, "glass")


# ---------------------------------------------------------------- family games
def games_hub():
    def art_quiz():
        cols = ["#E5484D", "#34D399", "#F5B301", "#3B82F6"]
        return '<div style="height:248px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px"><div style="font-size:96px;font-weight:700;line-height:1">؟</div><div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;width:360px">' + "".join(f'<div style="height:40px;border-radius:12px;background:{c}"></div>' for c in cols) + '</div></div>'
    def art_mem():
        cs = ["#E5484D", "#3B82F6", "#34D399", "#F5B301"]
        cells = "".join(f'<div style="height:72px;border-radius:12px;{"background:" + cs[i % 4] + ";display:flex;align-items:center;justify-content:center;color:#fff" if i in (1, 4, 6, 9) else "background:rgba(255,255,255,.18)"}">{ic("star", 32) if i in (1, 4, 6, 9) else ""}</div>' for i in range(12))
        return f'<div style="height:248px;display:grid;grid-template-columns:repeat(4,1fr);gap:8px;align-content:center">{cells}</div>'
    def art_word():
        letters = "كلمة"
        st = [("#34D399", "#fff"), ("#F5B301", "#14161A"), ("rgba(255,255,255,.18)", "#fff"), ("#34D399", "#fff")]
        row = lambda L: '<div style="display:flex;gap:8px;justify-content:center;direction:rtl">' + "".join(f'<div style="width:72px;height:72px;border-radius:12px;background:{st[i][0]};color:{st[i][1]};font-size:40px;font-weight:700;display:flex;align-items:center;justify-content:center">{L[i] if L else ""}</div>' for i in range(4)) + '</div>'
        return f'<div style="height:248px;display:flex;flex-direction:column;gap:8px;justify-content:center">{row("كتاب")}{row("كلمة")}{row("")}</div>'
    games = [("سؤال وجواب", "أسئلة إسلامية وعامة · فرق وأرقام", "٢–٤ فرق", art_quiz()), ("ذاكرة الأزواج", "اقلب البطاقات وجد الزوج", "١–٤ لاعبين", art_mem()), ("خمّن الكلمة", "كلمة عربية من أربعة حروف", "١–٢ لاعب", art_word())]
    tiles = "".join(f'<div class="panel col{" foc" if i == 0 else ""}" style="width:552px;height:560px;padding:24px;gap:16px;border-radius:24px"><div style="border-radius:16px;background:rgba(0,0,0,.25);padding:16px;color:#fff">{art}</div><div style="font-size:34px;font-weight:700">{n}</div><div style="font-size:22px;opacity:.85">{d}</div><div style="flex:1"></div><div style="font-size:22px;opacity:.85">{p}</div></div>' for i, (n, d, p, art) in enumerate(games))
    body = '<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">ألعاب العائلة</div><div class="abs sub" style="right:96px;top:112px;font-size:22px">بدون إنترنت، بدون إعلانات، وبالريموت فقط.</div>' \
           f'<div class="abs" style="left:96px;right:96px;top:176px;display:flex;gap:24px;justify-content:center">{tiles}</div>' \
           '<div class="abs row" style="left:96px;bottom:54px;gap:16px;font-size:22px" ><div class="panel btn" style="height:56px;padding:0 24px;border-radius:16px;gap:8px">' + ic("chev", 24) + 'رجوع</div></div>'
    return G.page(body, "glass")


def game_quiz():
    teams = [("الصقور", "٣", "#E5484D", True), ("النجوم", "٢", "#3B82F6", False), ("الأسود", "٢", "#34D399", False), ("الواحة", "١", "#F5B301", False)]
    sc = "".join(f'<div class="panel row" style="width:408px;height:88px;padding:0 24px;justify-content:space-between;border-radius:16px;{"box-shadow:0 0 0 3px #fff;" if a else ""}"><div class="row" style="gap:16px"><div style="width:16px;height:48px;border-radius:8px;background:{c}"></div><div style="font-size:26px;font-weight:600">{n}</div></div><div style="font-size:44px;font-weight:300">{s}</div></div>' for n, s, c, a in teams)
    opts = [("٣", "#E5484D"), ("٤", "#34D399"), ("٥", "#F5B301"), ("٦", "#3B82F6")]
    ob = "".join(f'<div class="btn row{" foc" if i == 2 else ""}" style="width:552px;height:112px;border-radius:16px;justify-content:flex-start;padding:0 24px;gap:24px;font-size:40px;font-weight:700;background:{c if i != 2 else "var(--foc)"};color:{"#fff" if i != 2 else "var(--foct)"}">{t}</div>' for i, (t, c) in enumerate(opts))
    body = f'<div class="abs" style="left:96px;right:96px;top:54px;display:flex;gap:24px;justify-content:center">{sc}</div>' \
           '<div class="abs panel col" style="left:96px;right:96px;top:176px;height:200px;align-items:center;justify-content:center;gap:8px;border-radius:24px"><div style="font-size:22px;opacity:.85">دور فريق الصقور · السؤال ٤ من ١٠</div><div style="font-size:56px;font-weight:700">كم عدد أركان الإسلام؟</div></div>' \
           f'<div class="abs" style="left:96px;right:96px;top:408px;display:grid;grid-template-columns:1fr 1fr;gap:24px;justify-items:center">{ob}</div>' \
           '<div class="abs" style="left:96px;right:96px;bottom:54px"><div style="height:16px;border-radius:8px;background:rgba(255,255,255,.2)"><div style="width:62%;height:16px;border-radius:8px;background:var(--accent);margin-right:0"></div></div><div class="row" style="justify-content:space-between;margin-top:16px;font-size:22px"><span>١٢ ثانية</span><span class="sub">اختر بالأسهم أو بالأزرار الملوّنة ثم OK</span></div></div>'
    return G.page(body, "glass")


def home_games():
    base = G.home("glass", focus=-1)
    # the card in place of the prayers card, and a tile in the bar: built on the same tokens
    card = '<div class="panel foc col" style="position:absolute;left:96px;top:176px;width:480px;height:288px;padding:16px;gap:16px;border-radius:24px;color:var(--foct)"><div style="font-size:28px;font-weight:700">ألعاب العائلة</div><div style="font-size:20px">سؤال وجواب · ذاكرة الأزواج · خمّن الكلمة</div><div class="row" style="gap:16px;flex:1">' + "".join(f'<div class="btn" style="flex:1;height:96px;border-radius:16px;background:{c};color:#fff">{ic(g, 40)}</div>' for c, g in (("#E5484D", "list"), ("#3B82F6", "star"), ("#34D399", "grid"))) + '</div><div style="font-size:20px">اضغط OK للعب</div></div>'
    return base.replace(G.prayer_card("glass"), card).replace("المفضلة", "ألعاب العائلة").replace(">يوتيوب<", ">يوتيوب<")
