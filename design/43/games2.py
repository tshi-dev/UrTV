#!/usr/bin/env python3
"""«ألعاب العائلة», round 2 (mockups only): each game owns the WHOLE screen with its own mood, everything much bigger (play area 80-90 % of the screen, the main word or letter
at least 96 px, secondary text at least 36 px, scores and timers large). Our type, radius (16-32 px, no pills), focus language (a solid fill with its ink) and 8-scale stay.
   python3 design/43/games2.py /tmp/hg2 && node design/43/render.js /tmp/hg2 design/43      (names games2-*)    DejaVu Sans stands in for the private Thmanyah."""
import sys, math
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen
from gen import ic
import games as G1

FOC = "#FFFFFF"; INK = "#14161A"
def pg(bg, body, text="#FFFFFF", sub="rgba(255,255,255,.78)", en=False):
    h = f'''<!doctype html><html lang="{"en" if en else "ar"}" dir="{"ltr" if en else "rtl"}"><head><meta charset="utf-8"><style>
*{{box-sizing:border-box;margin:0;padding:0}}html,body{{width:1920px;height:1080px;overflow:hidden}}
body{{font-family:'DejaVu Sans',sans-serif;direction:{"ltr" if en else "rtl"};position:relative;background:{bg};color:{text}}}
.abs{{position:absolute}}.row{{display:flex;align-items:center}}.col{{display:flex;flex-direction:column}}
.btn{{display:flex;align-items:center;justify-content:center}}.sub{{color:{sub}}}
.foc{{background:{FOC};color:{INK};box-shadow:0 0 0 6px rgba(255,255,255,.35)}}
</style></head><body>{body}</body></html>'''
    return h

def hint(*items, col="rgba(255,255,255,.85)"):
    p = "".join(f'<div class="row" style="gap:12px"><div class="btn" style="min-width:64px;height:48px;padding:0 16px;border-radius:12px;background:rgba(255,255,255,.18);font-size:30px;font-weight:700">{k}</div><div style="font-size:32px;color:{col}">{w}</div></div>' for k, w in items)
    return f'<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:48px;height:56px">{p}</div>'
def score(a, b, ta="الأول", tb="الثاني", turn=0, ca="#5AA9F0", cb="#F2705A"):
    def c(n, s, col, on):
        return f'<div class="row" style="height:96px;padding:0 32px;gap:24px;border-radius:24px;background:rgba(0,0,0,.35);{"box-shadow:0 0 0 6px "+col if on else ""}"><div style="width:20px;height:56px;border-radius:10px;background:{col}"></div><div style="font-size:40px;font-weight:600">{n}</div><div style="font-size:64px;font-weight:700">{s}</div></div>'
    return f'<div class="abs row" style="left:96px;top:54px;gap:24px">{c(ta, a, ca, turn==0)}{c(tb, b, cb, turn==1)}</div>'
def ttl(t, s=""):
    return f'<div class="abs col" style="right:96px;top:54px;align-items:flex-end;gap:8px"><div style="font-size:56px;font-weight:700">{t}</div>' + (f'<div class="sub" style="font-size:36px">{s}</div>' if s else '') + '</div>'
def cta(t, w=640, icon="ok", foc=True, extra=""):
    return f'<div class="btn {"foc" if foc else ""}" style="height:104px;width:{w}px;border-radius:28px;font-size:44px;font-weight:700;gap:16px;{extra if not foc else ""}">{ic(icon, 48)}{t}</div>'
def bigtimer(sec, total=60, w=1100, col="#fff"):
    return f'<div class="col" style="gap:12px;align-items:flex-end"><div style="font-size:120px;font-weight:700;line-height:1">٠:{sec}</div><div style="width:{w}px;height:20px;border-radius:12px;background:rgba(0,0,0,.3)"><div style="width:{int(w*sec/total)}px;height:20px;border-radius:12px;background:{col}"></div></div></div>'

S = {}
# ---------------------------------------------------------------- hub (titles fit; 7 games + the mode card)
def hub(en=False, light=False):
    G = [("name", "اسم حيوان نبات", "حرف واحد والكل يجاوب", "leaf", "Name, animal, plant", "One letter, everyone answers", "linear-gradient(135deg,#1F8A70,#0B4F4C)"),
         ("odd", "واحد مو معنا", "أحدكم ما يعرف الكلمة", "eyeoff", "One of us is out", "One player is out of the loop", "linear-gradient(135deg,#3B1F4A,#12101C)"),
         ("act", "بدون كلام", "صِف الكلمة بلا ما تقولها", "talk", "Without words", "Describe it, don't say it", "linear-gradient(135deg,#E8346A,#F59E0B)"),
         ("miss", "الكلمة المفقودة", "أكمل الجملة أو المثل", "blank", "The missing word", "Complete the saying", "linear-gradient(135deg,#1E3A8A,#0B1530)"),
         ("hex", "تحدي الحروف", "فريقان على لوحة سداسية", "hex", "Letters challenge", "Two teams, one hex board", "linear-gradient(135deg,#4C3FBF,#1B1550)"),
         ("dom", "دومينو", "اللوحة على الشاشة", "dom", "Dominoes", "The board is on the screen", "linear-gradient(135deg,#1F6B3A,#0E3B22)"),
         ("word", "خمّن الكلمة", "خمس حروف وست محاولات", "word", "Guess the word", "Five letters, six tries", "linear-gradient(135deg,#475569,#1E293B)")]
    tiles = []
    for i, g in enumerate(G):
        n, s = (g[4], g[5]) if en else (g[1], g[2])
        foc = i == 0
        tiles.append(f'<div class="col" style="width:408px;height:336px;padding:32px;border-radius:32px;background:{g[6]};justify-content:space-between;{"box-shadow:0 0 0 8px #fff,0 24px 60px rgba(0,0,0,.45);transform:scale(1.03)" if foc else ""}"><div class="btn" style="width:96px;height:96px;border-radius:28px;background:rgba(255,255,255,.2)">{ic(g[3], 56)}</div><div class="col" style="gap:8px"><div style="font-size:38px;font-weight:700;line-height:1.15;white-space:nowrap">{n}</div><div style="font-size:30px;opacity:.9">{s}</div></div></div>')
    mt, ms, mb = ("No phones", "One remote, pass it round", "With phones") if en else ("بدون جوالات", "ريموت واحد ونمرّره", "بالجوالات")
    tiles.append(f'<div class="col" style="width:408px;height:336px;padding:32px;border-radius:32px;border:4px solid rgba(255,255,255,.35);justify-content:space-between"><div class="col" style="gap:8px"><div style="font-size:40px;font-weight:700">{mt}</div><div class="sub" style="font-size:30px">{ms}</div></div><div class="btn" style="height:80px;border-radius:24px;background:rgba(255,255,255,.16);font-size:34px;gap:12px">{ic("phone", 40)}{mb}</div></div>')
    t = "Family games" if en else "ألعاب العائلة"
    bg = "linear-gradient(160deg,#F4F1EA,#DAD5C8)" if light else "linear-gradient(160deg,#17181B,#0B0C0E)"
    txt = "#14161A" if light else "#fff"
    return pg(bg, f'<div class="abs" style="right:96px;top:54px;font-size:64px;font-weight:700">{t}</div><div class="abs" style="left:96px;right:96px;top:190px;display:grid;grid-template-columns:repeat(4,408px);gap:32px;justify-content:center">{"".join(tiles)}</div>' + hint(*([("OK", "start"), ("Back", "home")] if en else [("اختيار", "ابدأ"), ("رجوع", "الرئيسية")]), col=("rgba(20,22,26,.8)" if light else "rgba(255,255,255,.85)")), text=txt, sub=("rgba(20,22,26,.7)" if light else "rgba(255,255,255,.78)"), en=en)

# ---------------------------------------------------------------- 1 name animal plant: fresh green, a giant letter
BG1 = "radial-gradient(ellipse at 20% 10%,#2BB38A,#0B4F4C 70%)"
S["games2-02-name-play"] = pg(BG1, score("٣٠", "٢٠") + ttl("اسم حيوان نبات", "الجولة ٣ من ٥") + f'''<div class="abs btn" style="left:96px;top:200px;width:640px;height:640px;border-radius:64px;background:#FFFFFF;color:#0B4F4C;font-size:520px;font-weight:700;line-height:1">م</div>
<div class="abs col" style="right:96px;top:230px;gap:48px;align-items:flex-end">{bigtimer(42, 60, 960)}<div style="font-size:64px;font-weight:700;text-align:right;line-height:1.3;width:960px">اكتبوا على الورق: اسم، حيوان، نبات، جماد، بلاد</div><div class="sub" style="font-size:40px;width:960px;text-align:right">اللي يخلّص أول يضغط اختيار</div></div>''' + hint(("اختيار", "وقف الوقت"), ("رجوع", "اترك الجولة")))
rows = "".join(f'<div class="row" style="height:128px;padding:0 40px;gap:32px;border-radius:28px;background:rgba(0,0,0,.28)"><div style="flex:1;font-size:48px;font-weight:600">{n}</div>' + "".join(f'<div class="btn {"foc" if (i==0 and v==10) else ""}" style="width:160px;height:88px;border-radius:24px;font-size:56px;font-weight:700;background:{"" if (i==0 and v==10) else "rgba(255,255,255,.16)"}">{v}</div>' for v in (0, 5, 10)) + '</div>' for i, n in enumerate(("حيوان: ماعز", "نبات: ملوخية", "بلاد: مصر", "جماد: مكتب")))
S["games2-03-name-score"] = pg(BG1, score("٣٠", "٢٠") + ttl("النقاط", "٠ أو ٥ أو ١٠ لكل جواب") + f'<div class="abs col" style="left:240px;right:240px;top:200px;gap:24px">{rows}</div>' + hint(("▲ ▼", "الجواب"), ("◀ ▶", "النقاط"), ("اختيار", "الجولة الجاية")))

# ---------------------------------------------------------------- 2 odd one out: dark, secretive
BG2 = "radial-gradient(ellipse at 50% 40%,#2A1240,#08060D 75%)"
S["games2-04-odd-handover"] = pg(BG2, f'<div class="abs col" style="inset:0;align-items:center;justify-content:center;gap:40px"><div style="color:#B58CFF">{ic("eyeoff", 280, 1.2)}</div><div style="font-size:144px;font-weight:700">غطّوا عيونكم</div><div style="font-size:52px;opacity:.85">سلّموا الريموت إلى لاعب ٣</div>{cta("أنا جاهز", 720, "ok")}<div class="sub" style="font-size:36px">اضغط اختيار مطوّلًا</div></div>')
S["games2-05-odd-secret"] = pg(BG2, ttl("واحد مو معنا", "لاعب ٣ من ٥: اطلع وحدك") + f'<div class="abs col" style="left:0;right:0;top:230px;align-items:center;gap:32px"><div style="font-size:44px;opacity:.8">الكلمة السرّية</div><div style="font-size:300px;font-weight:700;line-height:1.1;color:#E0C8FF">الجمل</div><div style="font-size:44px;opacity:.85">لا تقولها. لمّح بكلمة وحدة لما يجي دورك</div></div><div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center">{cta("خبّيتها، مرّر", 720, "eyeoff")}</div>')
ppl = "".join(f'<div class="col btn {"foc" if i==2 else ""}" style="width:320px;height:440px;border-radius:40px;gap:24px;{"" if i==2 else "background:rgba(255,255,255,.1)"}"><div class="btn" style="width:160px;height:160px;border-radius:50%;background:{"rgba(0,0,0,.1)" if i==2 else "rgba(255,255,255,.14)"};font-size:104px;font-weight:700">{n}</div><div style="font-size:48px;font-weight:700">لاعب {n}</div></div>' for i, n in enumerate("١٢٣٤٥"))
S["games2-06-odd-vote"] = pg(BG2, ttl("مين هو الغريب؟", "كل واحد لمّح، وبعدين صوّتوا") + f'<div class="abs" style="left:96px;top:54px">{bigtimer(48, 90, 520, "#B58CFF")}</div><div class="abs row" style="left:96px;right:96px;top:330px;gap:24px;justify-content:center">{ppl}</div>' + hint(("◀ ▶", "اللاعب"), ("اختيار", "هذا هو")))

# ---------------------------------------------------------------- 3 without words: bright and loud
BG3 = "linear-gradient(135deg,#FF2E7E,#FF8A00)"
S["games2-07-words-handover"] = pg(BG3, f'<div class="abs col" style="inset:0;align-items:center;justify-content:center;gap:40px"><div style="font-size:144px;font-weight:700">دور الفريق الأول</div><div style="font-size:64px">اللي يصف: لاعب ٢. غطّوا عيونكم</div>{cta("أنا جاهز", 720, "ok", True)}</div>')
S["games2-08-words-play"] = pg(BG3, score("٣٠", "٢٠", "الأول", "الثاني", 0, "#FFFFFF", "#14161A") + f'<div class="abs" style="right:96px;top:54px">{bigtimer(36, 60, 640, "#fff")}</div><div class="abs col" style="left:0;right:0;top:230px;align-items:center;gap:32px"><div style="font-size:44px;opacity:.9">صِف الكلمة بلا ما تقولها</div><div style="font-size:340px;font-weight:700;line-height:1.1">سيارة</div><div style="font-size:44px;opacity:.9">ممنوع: عجلة، سواقة، مركبة</div></div><div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center;gap:32px">{cta("تخطّي", 480, "back", False, "background:rgba(0,0,0,.28)")}{cta("عرفوها", 720, "check", True)}</div>')
S["games2-09-words-end"] = pg(BG3, ttl("انتهى الوقت") + f'<div class="abs col" style="left:240px;right:240px;top:200px;gap:16px">' + "".join(f'<div class="row" style="height:104px;padding:0 40px;border-radius:28px;background:rgba(0,0,0,.25);justify-content:space-between;font-size:56px"><span style="font-weight:700">{w}</span><span style="font-size:44px">{"عرفوها" if ok else "تخطّوها"}</span></div>' for w, ok in (("سيارة", 1), ("قهوة", 1), ("مطار", 0), ("بحر", 1))) + f'</div><div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center;gap:48px"><div style="font-size:120px;font-weight:700">+٣</div>{cta("دور الفريق الثاني", 720)}</div>')

# ---------------------------------------------------------------- 4 missing word: calm navy
BG4 = "radial-gradient(ellipse at 50% 0%,#274690,#0B1530 70%)"
opts = "".join(f'<div class="btn {"foc" if i==1 else ""}" style="flex:1;height:200px;border-radius:36px;font-size:80px;font-weight:700;{"" if i==1 else "background:rgba(255,255,255,.12)"}">{o}</div>' for i, o in enumerate(("الصيف", "اللبن", "الحليب", "العسل")))
S["games2-10-missing-question"] = pg(BG4, score("٤٠", "٣٠", "الأول", "الثاني", 1) + ttl("الكلمة المفقودة", "سؤال ٤ من ١٠") + f'<div class="abs col" style="left:96px;right:96px;top:230px;align-items:center;gap:96px"><div style="font-size:140px;font-weight:700;line-height:1.3;text-align:center;color:#F5E6B8">الصيف ضيّعتِ <span style="display:inline-block;min-width:420px;border-bottom:12px solid #F2C14E">&nbsp;</span></div><div class="row" style="gap:24px;width:100%">{opts}</div></div>' + hint(("◀ ▶", "اختر"), ("اختيار", "ثبّت")))
S["games2-11-missing-reveal"] = pg(BG4, score("٤٠", "٣٠", "الأول", "الثاني", 1) + ttl("الكلمة المفقودة", "سؤال ٤ من ١٠") + f'<div class="abs col" style="left:96px;right:96px;top:230px;align-items:center;gap:56px"><div style="font-size:140px;font-weight:700;line-height:1.3;text-align:center;color:#F5E6B8">الصيف ضيّعتِ <span style="color:#54C08A">اللبن</span></div><div class="row" style="gap:32px;padding:32px 48px;border-radius:32px;background:rgba(255,255,255,.12);font-size:44px;line-height:1.5;max-width:1500px"><div class="btn" style="width:88px;height:88px;border-radius:24px;background:#54C08A;color:#0B1530;flex:none">{ic("check", 56, 2.6)}</div><div>يُقال لمن ضيّع الفرصة في وقتها ثم طلبها بعد فوات الأوان.</div></div></div><div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center">{cta("السؤال الجاي", 720)}</div>')

# ---------------------------------------------------------------- 5 hex board: 28 real letters, 4 rows of 7, filling the height
LET = "أبتثجحخدذرزسشصضطظعغفقكلمنهوي"   # the 28 letters of the alphabet, each once
def hexboard(R=116, owners=None, sel=(1, 3)):
    owners = owners or {(0, 1): "A", (0, 2): "A", (1, 0): "B", (1, 4): "B", (2, 5): "A", (3, 2): "B", (2, 0): "B", (0, 6): "B"}
    W = math.sqrt(3) * R
    cells = []
    k = 0
    for r in range(4):
        for c in range(7):
            x = R * 1.05 + c * W + (W / 2 if r % 2 else 0)
            y = R + r * R * 1.5
            pts = " ".join(f"{x + R*0.94*math.cos(math.radians(60*i-30)):.1f},{y + R*0.94*math.sin(math.radians(60*i-30)):.1f}" for i in range(6))
            o = owners.get((r, c)); foc = (r, c) == sel
            fill = "#FFFFFF" if foc else {"A": "#5AA9F0", "B": "#F2705A"}.get(o, "rgba(255,255,255,.14)")
            ink = INK if (foc or o) else "#fff"
            cells.append(f'<polygon points="{pts}" fill="{fill}" stroke="{"#FFFFFF" if foc else "rgba(255,255,255,.28)"}" stroke-width="{8 if foc else 2}"/><text x="{x:.1f}" y="{y+30:.1f}" text-anchor="middle" font-size="88" font-weight="700" fill="{ink}" font-family="DejaVu Sans">{LET[k]}</text>')
            k += 1
    return f'<svg width="{R*1.05*2 + 6*W + W/2:.0f}" height="{R*2 + 3*R*1.5:.0f}" style="display:block">{"".join(cells)}</svg>'
BG5 = "radial-gradient(ellipse at 50% 50%,#3B2FA0,#120E3B 75%)"
S["games2-12-hex-board"] = pg(BG5, f'<div class="abs" style="left:96px;top:54px">{G1.team_bar if False else ""}</div>' + score("٣٠", "٢٠", "الأزرق", "الأحمر", 0) + ttl("تحدي الحروف", "الأزرق: يمين إلى يسار. الأحمر: فوق إلى تحت") + f'<div class="abs" style="left:60px;top:196px">{hexboard()}</div>' + hint(("▲ ▼ ◀ ▶", "تحرّك"), ("اختيار", "اختر الحرف")))
S["games2-13-hex-question"] = pg(BG5, score("٣٠", "٢٠", "الأزرق", "الأحمر", 0) + ttl("تحدي الحروف", "حرف «م»") + f'''<div class="abs row" style="left:96px;right:96px;top:230px;gap:64px;align-items:center"><div class="btn" style="width:420px;height:420px;border-radius:72px;background:#fff;color:#120E3B;font-size:320px;font-weight:700;flex:none">م</div><div style="font-size:84px;font-weight:700;line-height:1.35">مدينة تبدأ بحرف الميم فيها الكعبة المشرّفة</div></div>
<div class="abs row" style="left:96px;right:96px;top:720px;gap:24px">''' + "".join(f'<div class="btn {"foc" if i==0 else ""}" style="flex:1;height:176px;border-radius:32px;font-size:56px;font-weight:700;{"" if i==0 else "background:rgba(255,255,255,.12)"}">{o}</div>' for i, o in enumerate(("مكة المكرمة", "الرياض", "جدة", "الدمام"))) + '</div>' + hint(("◀ ▶", "اختر"), ("اختيار", "ثبّت")))
S["games2-14-hex-win"] = pg(BG5, ttl("الأزرق وصل!", "من اليمين إلى اليسار") + f'<div class="abs" style="left:60px;top:196px">{hexboard(116, {(0,6):"A",(0,5):"A",(1,5):"A",(1,4):"A",(1,3):"A",(2,3):"A",(2,2):"A",(2,1):"A",(3,1):"A",(3,0):"A",(1,1):"B",(2,5):"B",(3,4):"B"}, (9,9))}</div>' + hint(("اختيار", "لعبة ثانية")))

# ---------------------------------------------------------------- 6 dominoes: a green table
BG6 = "radial-gradient(ellipse at 50% 45%,#2C8A52,#0E3B22 78%)"
def dom(a, b, w=240, foc=False, vertical=False):
    H = w // 2
    def dots(n, ox):
        pos = {0: [], 1: [(1, 1)], 2: [(0, 0), (2, 2)], 3: [(0, 0), (1, 1), (2, 2)], 4: [(0, 0), (0, 2), (2, 0), (2, 2)], 5: [(0, 0), (0, 2), (1, 1), (2, 0), (2, 2)], 6: [(0, 0), (0, 2), (1, 0), (1, 2), (2, 0), (2, 2)]}[n]
        return "".join(f'<circle cx="{ox + 20 + x * 40}" cy="{20 + y * 40}" r="11" fill="#14161A"/>' for x, y in pos)
    return f'<svg width="{w}" height="{H}" viewBox="0 0 240 120" style="display:block"><rect x="3" y="3" width="234" height="114" rx="20" fill="#FBF7EC" stroke="{"#FFFFFF" if foc else "rgba(0,0,0,.35)"}" stroke-width="{8 if foc else 2}"/><path d="M120 14v92" stroke="#B9B2A0" stroke-width="4"/>{dots(a, 0)}{dots(b, 120)}</svg>'
chain = '<div class="row" style="gap:12px;justify-content:center">' + "".join(dom(a, b, 232) for a, b in ((3, 6), (6, 6), (6, 1), (1, 4), (4, 4), (4, 0), (0, 2))) + '</div>'
S["games2-15-domino-board"] = pg(BG6, score("٢ قطع", "٥ قطع", "سعد", "نورة", 0, "#F2C14E", "#F2705A") + ttl("دومينو", "دور سعد") + f'<div class="abs" style="left:0;right:0;top:240px;display:flex;justify-content:center"><div style="padding:48px;border-radius:48px;background:rgba(0,0,0,.18);box-shadow:inset 0 0 0 8px #5A3A1B">{chain}</div></div><div class="abs col" style="left:0;right:0;top:600px;align-items:center;gap:32px"><div style="font-size:56px">الطرفان المفتوحان: ٣ و٢</div>{cta("سعد: اكشف يدك", 760, "eye")}</div>' + hint(("اختيار", "اكشف اليد"), ("رجوع", "اترك اللعبة")))
hand = "".join(f'<div style="{"transform:translateY(-32px)" if i==2 else ""}">{dom(a, b, 232, i==2)}</div>' for i, (a, b) in enumerate(((3, 5), (2, 2), (3, 1), (5, 6), (0, 4))))
S["games2-16-domino-hand"] = pg(BG6, ttl("يد سعد", "بس سعد يشوف. غطّوا عيونكم") + f'<div class="abs" style="left:0;right:0;top:200px;display:flex;justify-content:center;transform:scale(.8);transform-origin:top center">{chain}</div><div class="abs col" style="left:0;right:0;top:560px;align-items:center;gap:48px"><div class="row" style="gap:16px">{hand}</div><div style="font-size:48px">الاختيار يضعها عند الطرف ٣</div></div>' + hint(("◀ ▶", "اختر قطعة"), ("اختيار", "ضعها"), ("▼", "اخفِ اليد ومرّر")))

# ---------------------------------------------------------------- 7 guess the word: calm slate, grid centred and large, keyboard beside it, both big
BG7 = "linear-gradient(160deg,#2B3441,#151A22)"
GREEN, AMBER = "#54C08A", "#F0A64A"
def wrows(rows, cell=112):
    out = []
    for r in rows:
        out.append('<div class="row" style="gap:12px">' + "".join(f'<div class="btn" style="width:{cell}px;height:{cell}px;border-radius:24px;font-size:76px;font-weight:700;background:{ {"g": GREEN, "a": AMBER, "x": "rgba(255,255,255,.2)", "e": "rgba(255,255,255,.08)"}[s] };color:{"#0B1530" if s in "ga" else "#fff"}">{ch}</div>' for ch, s in r) + '</div>')
    return '<div class="col" style="gap:12px">' + "".join(out) + '</div>'
def kb(foc="ر", used=None):
    used = used or {}
    out = []
    for row in ("ضصثقفغعهخحج", "شسيبلاتنمكط", "ئءؤرىةوزظد"):
        out.append('<div class="row" style="gap:10px;justify-content:center">' + "".join(f'<div class="btn {"foc" if k==foc else ""}" style="width:88px;height:96px;border-radius:20px;font-size:54px;font-weight:700;{"" if k==foc else "background:" + {"g": GREEN, "a": AMBER, "x": "rgba(255,255,255,.05)"}.get(used.get(k), "rgba(255,255,255,.16)")}">{k}</div>' for k in row) + '</div>')
    out.append('<div class="row" style="gap:10px;justify-content:center"><div class="btn" style="width:260px;height:96px;border-radius:20px;font-size:40px;background:rgba(255,255,255,.16)">حذف</div><div class="btn" style="width:440px;height:96px;border-radius:20px;font-size:44px;font-weight:700;background:rgba(255,255,255,.16)">تأكيد</div></div>')
    return '<div class="col" style="gap:10px">' + "".join(out) + '</div>'
rows3 = [[("ك", "x"), ("ت", "a"), ("ا", "g"), ("ب", "x"), ("ة", "x")], [("م", "x"), ("د", "g"), ("ر", "a"), ("س", "x"), ("ة", "x")], [("ش", "x"), ("ج", "g"), ("", "e"), ("", "e"), ("", "e")], [("", "e")] * 5, [("", "e")] * 5, [("", "e")] * 5]
S["games2-17-guess-play"] = pg(BG7, ttl("خمّن الكلمة", "المحاولة ٣ من ٦") + f'<div class="abs" style="right:96px;top:190px">{wrows(rows3)}</div><div class="abs" style="left:96px;top:190px;width:1120px;height:760px;display:flex;align-items:center">{kb("ر", {"ك": "x", "ت": "a", "ا": "g", "ب": "x", "ة": "x", "م": "x", "د": "g", "ر": "a", "س": "x", "ش": "x", "ج": "g"})}</div>' + hint(("▲ ▼ ◀ ▶", "الحروف"), ("اختيار", "اكتب"), ("رجوع", "امسح")))
S["games2-18-guess-end"] = pg(BG7, ttl("أحسنت", "في المحاولة ٣") + f'<div class="abs col" style="left:0;right:0;top:200px;align-items:center;gap:56px"><div class="row" style="gap:16px">' + "".join(f'<div class="btn" style="width:220px;height:220px;border-radius:40px;background:{GREEN};color:#0B1530;font-size:160px;font-weight:700">{c}</div>' for c in "شجرة") + f'</div><div style="font-size:44px;line-height:1.5;max-width:1400px;text-align:center">الشجرة: نبات خشبي معمّر له ساق وجذع وأغصان، ومنها السدر والأراك.</div></div><div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center">{cta("كلمة ثانية", 720)}</div>')

S["games2-01-hub"] = hub()
S["games2-19-hub-light"] = hub(light=True)
S["games2-20-hub-en"] = hub(en=True)

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in sorted(S.items()): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(S), "pages")
