#!/usr/bin/env python3
"""«رياضي» mockups (4.4 beta 2, for the owner): python3 make.py && node render.js. Plain text, plain dividers, flat fills; no swatches,
no badges, no outline (focus = scale + soft shadow). A crest shows only where TheSportsDB's badge is cached; these mockups draw names only."""
from pathlib import Path
HERE = Path(__file__).parent
CSS = """@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Sans+Arabic:wght@400;500;600;700&display=swap');
:root{--fg:#f4f2ee;--dim:rgba(244,242,238,.68);--faint:rgba(244,242,238,.16);--pane:rgba(18,18,22,.58);--win:#4caf6e;--draw:#9a9a92;--flo:#d9534f;--k:1;--u:1px}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:1920px;height:1080px;overflow:clip;position:relative;background:#000;color:var(--fg);font-family:'IBM Plex Sans Arabic',sans-serif;direction:rtl}
.bg{position:absolute;inset:0;background:url(../../../../android/launcher/src/main/assets/scenes/saudi/riyadh.jpg) center/cover;filter:brightness(.62)}
.top{position:absolute;top:calc(48*var(--u));right:calc(96*var(--u));left:calc(96*var(--u));display:flex;justify-content:space-between;align-items:flex-end}
.greet{font-size:calc(30px*var(--k));font-weight:600}.greet small{display:block;font-size:calc(21px*var(--k));color:var(--dim);font-weight:400}
.clock{font-size:calc(96px*var(--k));font-weight:600;line-height:1}
.grid{position:absolute;top:calc(200*var(--u));right:calc(96*var(--u));left:calc(96*var(--u));bottom:calc(250*var(--u));display:grid;gap:calc(28*var(--u));grid-template-columns:1.15fr 1fr 1fr;align-items:start}
.col{display:flex;flex-direction:column;gap:calc(28*var(--u));min-width:0}
.p{background:var(--pane);backdrop-filter:blur(24px);border-radius:calc(20*var(--u));padding:calc(20*var(--u)) calc(26*var(--u))}
.p.f{transform:scale(1.03);box-shadow:0 24px 50px rgba(0,0,0,.45)}
.t{font-size:calc(22px*var(--k));color:var(--dim);font-weight:500;padding-bottom:calc(10*var(--u));border-bottom:1px solid var(--faint);margin-bottom:calc(8*var(--u))}
.lg{font-size:calc(20px*var(--k));color:var(--dim);margin:calc(10*var(--u)) 0 calc(4*var(--u))}
.r{display:flex;align-items:center;gap:calc(14*var(--u));font-size:calc(26px*var(--k));line-height:1.5;padding:0 calc(6*var(--u))}
.r .n{width:1.3em;color:var(--dim)}.r .nm{flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.r .v{font-weight:600;min-width:2em;text-align:left}
.r.me{font-weight:700;background:rgba(255,255,255,.08)}
.form{display:flex;gap:6px;direction:rtl}.fdot{width:calc(12px*var(--k));height:calc(12px*var(--k));border-radius:50%}
.w{background:var(--win)}.d{background:var(--draw)}.l{background:var(--flo)}
.big{font-size:calc(40px*var(--k));font-weight:700;line-height:1.3;margin-top:calc(6*var(--u))}
.cd{font-size:calc(56px*var(--k));font-weight:700;line-height:1.2;margin-top:calc(8*var(--u))}
.s{font-size:calc(22px*var(--k));color:var(--dim);line-height:1.5}
.sc{display:flex;align-items:center;font-size:calc(28px*var(--k));line-height:1.6}.sc .h{flex:1;text-align:right}.sc .a{flex:1;text-align:left}.sc .x{font-weight:700;padding:0 calc(16*var(--u))}
.min{font-size:calc(20px*var(--k));color:var(--dim);text-align:center}
.apps{position:absolute;left:0;right:0;bottom:calc(64*var(--u));display:flex;justify-content:center;gap:calc(28*var(--u))}
.app{width:calc(240*var(--u));height:calc(135*var(--u));border-radius:calc(16*var(--u));background:rgba(40,40,46,.9);display:grid;place-items:center;font-size:calc(24px*var(--k));color:var(--dim)}
.note{position:absolute;bottom:10px;left:40px;right:40px;font-size:18px;color:#f0e2b6;text-align:center}
.light .bg{filter:brightness(.45)}.light .p{backdrop-filter:none;background:rgba(18,18,22,.88)}.light .p.f{transform:none;box-shadow:none;background:rgba(44,44,50,.95)}
"""
f = lambda a: '<div class="form">' + ''.join(f'<i class="fdot {c}"></i>' for c in a) + '</div>'
def row(n, nm, v, form="", me=False): return f'<div class="r{" me" if me else ""}"><span class="n">{n}</span><span class="nm">{nm}</span>{f(form) if form else ""}<span class="v">{v}</span></div>'
TOPS = {"دوري روشن السعودي": [("١", "الهلال", "٣٤", "wwdww", True), ("٢", "الاتحاد", "٣١", "wlwww"), ("٣", "النصر", "٢٩", "dwwlw")],
        "الدوري الإنجليزي": [("١", "أرسنال", "٣٠", "wwdwl"), ("٢", "مانشستر سيتي", "٢٨", "wwwld"), ("٣", "ليفربول", "٢٦", "lwwdw")]}
def tops(leagues): return '<div class="p"><div class="t">المتصدرون</div>' + ''.join(f'<div class="lg">{lg}</div>' + ''.join(row(*x) for x in TOPS[lg]) for lg in leagues) + '</div>'
NEXT = '<div class="p f"><div class="t">مباراة الهلال القادمة</div><div class="big">الهلال ضد الاتحاد</div><div class="cd">بعد ٢ ساعة و١٤ د</div><div class="s">دوري روشن، الجولة ١٢، الليلة ٨:٠٠ م</div></div>'
LIVE = '<div class="p"><div class="t">مباشر الآن</div><div class="sc"><span class="h">النصر</span><span class="x">٢ - ١</span><span class="a">الأهلي</span></div><div class="min">الدقيقة ٦٧</div><div class="sc"><span class="h">أرسنال</span><span class="x">٠ - ٠</span><span class="a">تشيلسي</span></div><div class="min">مباشر</div></div>'
LAST = '<div class="p"><div class="t">آخر النتائج</div>' + ''.join(f'<div class="sc"><span class="h">{h}</span><span class="x">{x}</span><span class="a">{a}</span></div>' for h, x, a in [("الهلال", "٣ - ١", "الشباب"), ("الفتح", "٠ - ٢", "الهلال"), ("الهلال", "١ - ١", "التعاون")]) + '</div>'
LIVE1 = '<div class="p"><div class="t">مباشر الآن</div><div class="sc"><span class="h">النصر</span><span class="x">٢ - ١</span><span class="a">الأهلي</span></div><div class="min">الدقيقة ٦٧</div></div>'
HON = '<div class="p"><div class="t">بطل الدوري</div><div class="r"><span class="nm">دوري روشن، ٢٠٢٤-٢٠٢٥: الاتحاد</span></div><div class="r"><span class="nm">الإنجليزي، ٢٠٢٤-٢٠٢٥: ليفربول</span></div></div>'
APPS = '<div class="apps">' + ''.join(f'<div class="app">{a}</div>' for a in ["شاهد", "يوتيوب", "نتفليكس", "SSC", "ثمانية"]) + '</div>'
TOP = '<div class="top"><div class="greet">مساء الخير<small>الاثنين ٢٧ جمادى الأولى، ١٩ نوفمبر</small></div><div class="clock">٥:٤٦</div></div>'
def page(name, body, cls="", style="", note=""):
    (HERE / f"{name}.html").write_text(f'<!doctype html><html lang="ar"><head><meta charset="utf-8"><link rel="stylesheet" href="base.css"><style>{style}</style></head><body class="{cls}"><div class="bg"></div>{TOP}{body}{APPS}<div class="note">{note}</div></body></html>', encoding="utf-8")
(HERE / "base.css").write_text(CSS, encoding="utf-8")
FULL = f'<div class="grid"><div class="col">{tops(TOPS)}</div><div class="col">{NEXT}{LIVE}</div><div class="col">{LAST}{HON}</div></div>'
page("01-sport-1080", FULL, note="صور تخطيط: صف التطبيقات بأسماء بديلة (الحقيقي بأيقونات التطبيقات)، والشعار يظهر بس لو محفوظ، وإلا الاسم وحده.")
page("02-sport-720-large", f'<div class="grid" style="grid-template-columns:1.2fr 1fr">'
     f'<div class="col">{tops(["دوري روشن السعودي"])}</div><div class="col">{NEXT}{LIVE1}</div></div>',
     style="html,body{width:1280px;height:720px}:root{--k:1.3;--u:.6667px}.grid{top:118px;bottom:auto;gap:16px}.col{gap:16px}.clock{font-size:64px}.greet{font-size:26px}.greet small{font-size:18px}.top{top:28px}.p{padding:10px 16px}.t{font-size:22px;padding-bottom:6px}.lg{font-size:20px;margin:4px 0 2px}.r{font-size:26px;line-height:1.45}.cd{font-size:46px}.big{font-size:32px}.s{font-size:20px}.sc{font-size:26px}.min{font-size:18px}.apps{bottom:34px;gap:16px}.app{width:176px;height:99px;font-size:20px}.note{font-size:16px;bottom:4px}",
     note="٧٢٠p بالخط الكبير: عمودان، والنتائج والأبطال والدوري الثاني بالتمرير تحت. ولا نص تحت ١٦ بكسل.")
page("03-sport-light", FULL, cls="light", note="التلفزيون الضعيف: لون ثابت بلا تغبيش ولا ظل ولا تكبير، والتركيز لون أفتح.")
page("04-sport-quiet", f'<div class="grid" style="grid-template-columns:1fr 1fr"><div class="col">{NEXT}</div><div class="col">{LAST}</div></div>',
     note="بلا مباراة جارية ولا دوري متابَع له جدول: الأقسام تختفي (لا صناديق فاضية)، والباقي يتوسّط.")
