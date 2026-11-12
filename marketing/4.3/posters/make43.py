"""UrTV 4.3 (4 pages) and UrTV Player (3 pages) tall posters, same look as t42. Crops of ../../xp43 only, nothing drawn. No «» quotes, no «·»."""
import sys
from pathlib import Path
from PIL import Image
HERE = Path(__file__).parent
sys.path.insert(0, str(HERE.parent / "t42")); sys.path.insert(0, str(HERE.parent / "t40")); sys.path.insert(0, str(HERE.parent))
import make, make40 as m, make42 as b
XP = HERE.parent.parent / "xp43"
ar = m.ar
SETS = {"t43": HERE, "tpl": HERE.parent / "tpl"}
CROPS = {
    "t43": {"hub.png": ("x1-hub.png", (140, 220, 1780, 900)), "games.png": ("x2-games.png", (0, 0, 1920, 1080)),
            "match.png": ("x3-match.png", (300, 40, 1620, 848)), "clean.png": ("x4-clean.png", (0, 0, 1920, 1080))},
    "tpl": {"list.png": ("p1-list.png", (60, 30, 1880, 720)), "match.png": ("x3-match.png", (300, 40, 1620, 848))},
}
for k, d in CROPS.items():
    (SETS[k] / "img").mkdir(parents=True, exist_ok=True)
    for out, (src, box) in d.items():
        Image.open(XP / src).convert("RGB").crop(box).save(SETS[k] / "img" / out)
ICON = b.dl_icon()
FOOT_U = '<div class="foot"><div>شاشة تلفزيونك، الواجهة الرئيسية، تلاوة وأذان</div><div class="l">github.com/tshi-dev/UrTV</div></div>'
FOOT_P = '<div class="foot"><div>مشغّل خفيف وسريع لقوائمك</div><div class="l">github.com/tshi-dev/UrTV-Player</div></div>'
EXTRA = "<style>.d.beta{color:#E9C77F}.how .small+.small{margin-top:8px}.how b{color:#E9C77F;font-weight:inherit}</style>"

def build(key, n, foot, pages):
    img = (SETS[key] / "img").as_uri()
    top = lambda i: b.TOP(i).replace(f"{ar(i)} / {ar(b.N)}", f"{ar(i)} / {ar(n)}")
    for i, body in pages.items():
        html = b.BASE.replace("</head>", EXTRA + "</head>") + top(i) + body.replace("@IMG", img) + foot + "</body></html>"
        (SETS[key] / f"t{i}.html").write_text(html.replace(b.IMG + "/downloader-icon.png", (HERE.parent / "t42" / "img").as_uri() + "/downloader-icon.png"), encoding="utf-8")

shot = lambda f, h: f'<div class="shot"><img src="@IMG/{f}" style="height:{h-2}px"></div>'
items = b.items

HOW_U = f"""
<div class="glass how">
  <div class="label">كيف تحدّث؟</div>
  <div class="small">عندك التطبيق؟ الإعدادات ← تحديث تلفزيوني</div>
  <div class="dl">{ICON}<div>
      <div class="dlh">تركيب جديد من التلفزيون نفسه</div>
      <div class="dlcode"><bdi>9378345</bdi></div>
      <div class="dls">افتح تطبيق <bdi>Downloader</bdi> واكتب الكود</div></div></div>
  <div class="small" style="margin-top:16px"><b>الأضمن:</b> خلّ مساعدك الذكي يركّبه لك من الكمبيوتر.<br>الصق له الطلب الجاهز من <bdi>github.com/tshi-dev/UrTV</bdi></div>
</div>"""

U = {}
U[1] = """
<div class="kick">تلفزيونك 4.3</div>
<h1>وصل 4.3،<br>وهذا جديده</h1>
""" + items([
    ("تلفزيونك: بيت واحد لكل شيء", "الأسواق والمباريات والألعاب والإعدادات بضغطة"),
    "ثماني ألعاب تجمع العائلة",
    "شاهد المباراة: من الجدول إلى البث",
    "<bdi>UrTV</bdi> النظيف: ساعة وصلاة، ولا شيء غيرها",
    "مظهر موحّد، وصفحة جوال بحلّة جديدة",
    "زر أطفئ الآن في مركز التحكم",
    "إصلاحات من بلاغاتكم",
    ("خلّه شاشتك الرئيسية", '<span class="d beta">من التطبيق نفسه: قيد التجربة</span>'),
], "p1") + HOW_U
U[2] = f"""
<div class="kick">الجديد في 4.3، الألعاب</div>
<h1>لمّة العائلة<br>على الشاشة</h1>
{shot("games.png", 520)}
""" + items([
    "ثماني ألعاب، وريموت واحد يدور بينكم",
    "في الأسرع، كل واحد يجاوب من جواله",
    "خمس خانات، خلية الحروف، خمّن الكلمة، ودومينو",
    "بالعربي، وبالإنجليزي إذا تلفزيونك إنجليزي",
])
U[3] = f"""
<div class="kick">الجديد في 4.3، كرة القدم</div>
<h1>مباراتك بضغطة</h1>
{shot("hub.png", 384)}
{shot("match.png", 568)}
""" + items([
    "تلفزيونك: نافذة لكل شيء، والمباراة الجاية قدّامك",
    "اختر المباراة واضغط شاهد المباراة، والباقي علينا",
    "دوريات أكثر، ووقت المباراة مع وقت الصلاة",
])
U[4] = f"""
<div class="kick">الجديد في 4.3، <bdi>UrTV</bdi> النظيف</div>
<h1>هدوء يليق<br>بمجلسك</h1>
{shot("clean.png", 520)}
""" + items([
    "الساعة ومواقيت الصلاة، وبس",
    "بطاقة الصلاة وين ما تحب: بالوسط، أو تحت الساعة",
    "مظهر موحّد للأزرار والبطاقات والشريط",
    "إذاعة القرآن أثبت، وتنقّل أنعم",
], "p4")
build("t43", 4, FOOT_U, U)

HOW_P = f"""
<div class="glass how">
  <div class="label">كيف تركّبه؟</div>
  <div class="small">عندك <bdi>UrTV</bdi>؟ الإعدادات ← تطبيقات مقترحة</div>
  <div class="dl">{ICON}<div>
      <div class="dlh">أو من التلفزيون نفسه</div>
      <div class="dlcode"><bdi>8035253</bdi></div>
      <div class="dls">افتح تطبيق <bdi>Downloader</bdi> واكتب الكود</div></div></div>
  <div class="small" style="margin-top:16px"><b>للعلم:</b> المشغّل ما فيه أي قنوات. تضيف قائمتك أنت.</div>
</div>"""
Pl = {}
Pl[1] = """
<div class="kick"><bdi>UrTV Player</bdi></div>
<h1>مشغّلك،<br>بقائمتك</h1>
""" + items([
    ("خفيف وسريع", "القناة تفتح قبل ما تكمّل جملتك"),
    "عربي من أول تشغيل",
    "يتحدّث بنفسه",
    ("يشتغل لحاله، أو مع <bdi>UrTV</bdi>", "ومن صفحة المباريات يفتح لك المباراة"),
    "بلاغ أو اقتراح برمز <bdi>QR</bdi> من داخله",
], "p4") + HOW_P
Pl[2] = f"""
<div class="kick"><bdi>UrTV Player</bdi>، القنوات</div>
<h1>يفتح قبل<br>ما تنتظر</h1>
{shot("list.png", 352)}
""" + items([
    "تنقّل بين قنواتك بدون انتظار",
    "أقسام، ومفضّلة، وآخر ما شاهدت",
    "بحث بالعربي والإنجليزي",
    "معاينة للقناة قبل ما تفتحها، وتطفيها متى ما بغيت",
], "p4")
Pl[3] = f"""
<div class="kick"><bdi>UrTV Player</bdi>، مع <bdi>UrTV</bdi></div>
<h1>من جدول المباريات<br>إلى البث</h1>
{shot("match.png", 568)}
""" + items([
    "اضغط شاهد المباراة في <bdi>UrTV</bdi>",
    "المشغّل يدوّر المباراة في قائمتك ويفتحها",
    "عندك ملاحظة؟ امسح الرمز من الإعدادات وقلها لنا",
], "p4")
build("tpl", 3, FOOT_P, Pl)
print("ok")
