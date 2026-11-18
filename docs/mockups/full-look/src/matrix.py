"""The proposal's card x look x background table (docs/mockups/full-look/08-matrix.png and the README's table). Built, its single source is
CardFit in the app (one table, tests/card_fit_test.py: every pair decided); this file only draws the proposal from the same rules."""
CARDS = [('nextprayer', 'الصلاة القادمة'), ('prayerrow', 'الصلوات في صف'), ('prayers', 'مواقيت الصلاة اليوم'), ('hourly', 'طقس الساعات القادمة'),
         ('forecast', 'طقس الأيام القادمة'), ('nextmatch', 'مباراة فريقك القادمة'), ('matchday', 'مباراة اليوم'), ('livenow', 'مباشر الآن'),
         ('matches', 'مباريات فرقك القادمة'), ('nextmatches', 'مباراة كل فريق القادمة'), ('minitable', 'جدول الترتيب'), ('yesterday', 'نتائج أمس'),
         ('weekend', 'نهاية الأسبوع'), ('cuppath', 'طريق الكأس'), ('formula', 'فورمولا ١'), ('national', 'المنتخب السعودي'),
         ('verse', 'آية أو حديث اليوم'), ('hadith', 'حديث اليوم'), ('wisdom', 'حكمة اليوم'), ('adhkar', 'أذكار الصباح والمساء'),
         ('shows', 'جديد المسلسلات'), ('stocks', 'الأسهم'), ('tasks', 'المهام'), ('year', 'السنة'), ('smart', 'البطاقة الذكية'),
         ('today', 'يومك'), ('album', 'ألبوم صوري')]
LOOKS = [('classic', 'تلفزيونك'), ('football', 'كرة القدم'), ('couch', 'كنبة'), ('tvos', 'tvOS')]
BGS = [('photos', 'صوري'), ('scenes', 'الآفاق')]
COMPACT = {"nextprayer", "prayerrow", "hourly", "verse", "hadith", "wisdom", "year", "smart"}   # CardFit.COMPACT today
TVOS = COMPACT | {"nextmatch", "matchday", "livenow", "formula", "national", "today", "album", "forecast"}   # a title and three lines on a glass card
NOTE = {("tvos", "verse"): "بعرض بطاقتين", ("tvos", "hadith"): "بعرض بطاقتين", ("tvos", "forecast"): "صف من ٤ أيام",
        ("couch", "smart"): "من البطاقات المناسبة", ("tvos", "smart"): "من البطاقات المناسبة"}

def fit(card, look, bg):
    """(yes, reason): the background never decides alone, since every card sits on its own glass (a flat fill on a weak TV), but it is a column of its own"""
    if look == "couch" and card not in COMPACT: return False, "قائمة طويلة: ما تنعرض كاملة في شريط كنبة"
    if look == "tvos" and card not in TVOS: return False, "قائمة طويلة: أطول من بطاقة tvOS"
    return True, NOTE.get((look, card), "")

if __name__ == "__main__":
    head = "".join(f"<th>{l}<small>{' و'.join(b for _, b in BGS)}</small></th>" for _, l in LOOKS)
    rows = ""
    for k, n in CARDS:
        cells = ""
        for lk, _ in LOOKS:
            r = [fit(k, lk, b) for b, _ in BGS]
            ok = all(x[0] for x in r)
            note = r[0][1] if ok else ""
            cells += f'<td class="{"y" if ok else "n"}">{"يناسب" if ok else "ما يناسب"}{"، " + note if note else ""}</td>'
        rows += f"<tr><td class=c>{n}</td>{cells}</tr>"
    html = f'''<!doctype html><html lang="ar"><head><meta charset="utf-8"><link rel="stylesheet" href="base.css"><style>
html,body{{height:auto;overflow:visible}}body{{background:#121014;padding:40px 60px}}h1{{font-size:36px;margin-bottom:4px}}p{{font-size:20px;color:var(--dim);margin-bottom:14px}}
table{{border-collapse:separate;border-spacing:0 3px;width:100%;font-size:19px}}th{{font-size:21px;text-align:center;padding:4px 8px;font-weight:600}}th small{{display:block;font-size:15px;color:var(--faint);font-weight:400}}
td{{padding:3px 12px;text-align:center;background:rgba(255,255,255,.05)}}td.c{{text-align:right;width:28%}}td.y{{color:#9fe3b6}}td.n{{color:#f0a48a;background:rgba(240,120,90,.10)}}
.note{{position:static;margin-top:12px;font-size:19px;color:var(--gold)}}</style></head><body>
<h1>أي بطاقة تناسب أي شكل وأي خلفية</h1><p>جدول واحد في الكود يقرر كل بطاقة مع كل شكل وكل خلفية، واختبار يتأكد ما في خانة بلا قرار.</p>
<table><tr><th></th>{head}</tr>{rows}</table>
<div class="note">الخلفية ما تغيّر النتيجة اليوم: كل بطاقة على زجاجها (ولون ثابت في التلفزيون الضعيف)، فتنقرأ على صوري وعلى الآفاق. والقرار بالشكل: الشريط القصير ما يشيل القوائم الطويلة.</div></body></html>'''
    open("08-matrix.html", "w", encoding="utf-8").write(html)
    md = "| البطاقة | " + " | ".join(l for _, l in LOOKS) + " |\n|---|" + "---|" * len(LOOKS) + "\n"
    for k, n in CARDS:
        md += f"| {n} | " + " | ".join(("يناسب" + ("، " + fit(k, lk, "photos")[1] if fit(k, lk, "photos")[1] else "")) if all(fit(k, lk, b)[0] for b, _ in BGS) else "ما يناسب" for lk, _ in LOOKS) + " |\n"
    open("matrix.md", "w", encoding="utf-8").write(md)
