#!/usr/bin/env python3
"""«ألعاب العائلة» in OUR style (design/43, mockups only): the home's glass panels, tray tiles and solid-fill focus, big calm type, no pills (16 to 24 px radius),
no «·», 8-scale, 16 px inside a box, safe area x 96-1824, y 54-1026. One remote: pass-and-play is the default, phones an optional second choice.
   python3 design/43/games.py /tmp/hg && node design/43/render.js /tmp/hg design/43      (names games-*)   Real fonts are private: DejaVu Sans stands in."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen
from gen import page, ic
from clean import mirror

gen.ICONS.update({
    "eyeoff": '<path d="M3 3l18 18"/><path d="M10.6 5.2A9.5 9.5 0 0112 5c5 0 8.5 4.5 9.5 7a12 12 0 01-3 4M6.5 7A12 12 0 002.5 12c1 2.5 4.5 7 9.5 7a9 9 0 004-1"/>',
    "eye": '<path d="M2.5 12C3.5 9.5 7 5 12 5s8.5 4.5 9.5 7c-1 2.5-4.5 7-9.5 7s-8.5-4.5-9.5-7z"/><circle cx="12" cy="12" r="3"/>',
    "hex": '<path d="M12 2.5l8 4.5v10l-8 4.5-8-4.5V7z"/>',
    "dom": '<rect x="4" y="2.5" width="16" height="19" rx="3"/><path d="M4 12h16"/><circle cx="9" cy="7" r="1" fill="currentColor"/><circle cx="15" cy="17" r="1" fill="currentColor"/><circle cx="9" cy="17" r="1" fill="currentColor"/>',
    "word": '<rect x="3" y="3.5" width="18" height="17" rx="3"/><path d="M7.5 9h.01M12 9h.01M16.5 9h.01M7.5 15h9"/>',
    "blank": '<path d="M3 17h5M10 17h4M16 17h5"/><path d="M5 9h14M5 5h8"/>',
    "talk": '<path d="M4 5.5A2.5 2.5 0 016.5 3h11A2.5 2.5 0 0120 5.5v8a2.5 2.5 0 01-2.5 2.5H11l-4.5 4v-4A2.5 2.5 0 014 13.5z"/><path d="M8.5 8.5h7M8.5 11.5h4"/>',
    "leaf": '<path d="M5 19c0-8 5-14 15-15 0 10-6 15-14 15z"/><path d="M5 19L14 10"/>',
    "back": '<path d="M9 5l-7 7 7 7M2 12h14a5 5 0 015 5v2"/>',
    "ok": '<circle cx="12" cy="12" r="8.5"/>',
    "arrows": '<path d="M12 4v16M4 12h16"/>',
})

GREEN, AMBER, GREY = "#54C08A", "#F0A64A", "rgba(255,255,255,.16)"
TEAM_A, TEAM_B = "#5AA9F0", "#F2705A"

def title(t, sub=""):
    s = f'<div class="abs sub" style="right:96px;top:112px;font-size:24px">{sub}</div>' if sub else ""
    return f'<div class="abs" style="right:96px;top:54px;font-size:40px;font-weight:600">{t}</div>{s}'

def keys(*items):
    """the remote's keys at the bottom, as small rounded squares (8 px radius) and a word: «اختيار» is OK"""
    parts = "".join(f'<div class="row" style="gap:8px"><div class="btn panel" style="min-width:48px;height:40px;padding:0 12px;border-radius:12px;font-size:20px;font-weight:600">{k}</div><div style="font-size:22px" class="sub">{w}</div></div>' for k, w in items)
    return f'<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:32px;height:48px">{parts}</div>'

def team_bar(a="الفريق الأول", sa="٣٠", b="الفريق الثاني", sb="٢٠", turn=0, rounds="الجولة ٣ من ٧"):
    def card(n, s, col, on):
        return f'<div class="panel row" style="width:360px;height:72px;padding:0 24px;gap:16px;justify-content:space-between;border-radius:20px;{"box-shadow:0 0 0 3px "+col if on else ""}"><div class="row" style="gap:16px"><div style="width:16px;height:40px;border-radius:8px;background:{col}"></div><div style="font-size:26px;font-weight:600">{n}</div></div><div style="font-size:34px;font-weight:700">{s}</div></div>'
    return f'<div class="abs row" style="left:96px;top:54px;gap:16px">{card(a, sa, TEAM_A, turn == 0)}{card(b, sb, TEAM_B, turn == 1)}</div><div class="abs sub" style="left:96px;top:142px;font-size:22px">{rounds}</div>'

def big_btn(t, foc=False, w=360, icon=None, col=None):
    st = "" if foc else ""
    return f'<div class="btn {"foc" if foc else "tile"}" style="height:72px;width:{w}px;font-size:28px;font-weight:600;gap:12px;color:var(--text)">{ic(icon, 32) if icon else ""}{t}</div>'

def timer(sec=42, total=60, w=640):
    return f'<div class="col" style="gap:8px;width:{w}px;align-items:flex-end"><div style="font-size:48px;font-weight:700">٠:{sec}</div><div style="width:{w}px;height:12px;border-radius:8px;background:var(--tile)"><div style="width:{int(w*sec/total)}px;height:12px;border-radius:8px;background:var(--accent)"></div></div></div>'

# ------------------------------------------------------------------ hub, quick pick
GAMES = [("name", "اسم حيوان نبات جماد بلاد", "حرف واحد والكل يجاوب", "leaf", "Name Animal Plant Thing Place", "One letter, everyone answers"),
         ("odd", "واحد مو معنا", "أحدكم ما يعرف الكلمة", "eyeoff", "One of us doesn't know", "One player is out of the loop"),
         ("act", "بدون كلام", "وصف الكلمة بلا ما تقولها", "talk", "Without words", "Describe it without saying it"),
         ("miss", "الكلمة المفقودة", "أكمل الجملة أو المثل", "blank", "The missing word", "Complete the saying"),
         ("hex", "تحدي الحروف", "فريقان على لوحة سداسية", "hex", "Letters challenge", "Two teams on a hex board"),
         ("dom", "دومينو", "اللوحة على الشاشة", "dom", "Dominoes", "The board is on the screen"),
         ("word", "خمّن الكلمة", "خمس حروف وست محاولات", "word", "Guess the word", "Five letters, six tries")]

def hub(look="glass", en=False):
    tiles = []
    for i, g in enumerate(GAMES):
        name, sub = (g[4], g[5]) if en else (g[1], g[2])
        f = " foc" if i == 0 else ""
        tiles.append(f'<div class="panel col{f}" style="width:400px;height:240px;padding:24px;gap:8px;justify-content:space-between;border-radius:24px;color:var(--text)"><div class="btn" style="width:72px;height:72px;border-radius:20px;background:{"rgba(0,0,0,.08)" if i==0 else "var(--tile)"};color:{"var(--foct)" if i==0 else "var(--accent)"}">{ic(g[3], 40)}</div><div class="col" style="gap:4px"><div style="font-size:30px;font-weight:700">{name}</div><div style="font-size:22px;opacity:.8">{sub}</div></div></div>')
    mode_t, mode_s, mode_b = ("No phones", "One remote, pass it round", "With phones") if en else ("بدون جوالات", "ريموت واحد ونمرّره بيننا", "بالجوالات")
    tiles.append(f'<div class="col" style="width:400px;height:240px;padding:24px;gap:8px;justify-content:space-between;border:2px solid var(--line);border-radius:24px"><div class="col" style="gap:8px"><div style="font-size:28px;font-weight:700">{mode_t}</div><div class="sub" style="font-size:22px">{mode_s}</div></div><div class="btn tile" style="height:56px;font-size:22px;color:var(--text);gap:8px">{ic("phone", 28)}{mode_b}</div></div>')
    t1, t2 = ("Family games", "Pick one: OK starts, then OK again for the round") if en else ("ألعاب العائلة", "اختر لعبة: اختيار ثم اختيار تبدأ الجولة")
    body = title(t1, t2) + f'<div class="abs" style="left:96px;right:96px;top:200px;display:grid;grid-template-columns:repeat(4,400px);gap:24px;justify-content:center">{"".join(tiles)}</div>' + keys(*([("OK", "start"), ("Back", "home")] if en else [("اختيار", "ابدأ"), ("رجوع", "الرئيسية")]))
    h = page(body, look)
    return mirror(h) if en else h

def quickpick():
    def stepper(label, val, foc=False):
        return f'<div class="row" style="gap:24px;justify-content:space-between;height:88px;padding:0 24px;border-radius:16px" ><div style="font-size:28px">{label}</div><div class="row {"" }" style="gap:16px"><div class="btn tile" style="width:64px;height:64px;font-size:36px">−</div><div class="btn {"foc" if foc else "tile"}" style="min-width:120px;height:64px;font-size:34px;font-weight:700;color:var(--text)">{val}</div><div class="btn tile" style="width:64px;height:64px;font-size:36px">+</div></div></div>'
    names = "".join(f'<div class="tile row" style="height:72px;padding:0 24px;gap:16px;font-size:26px;border-radius:16px"><div style="width:16px;height:40px;border-radius:8px;background:{c}"></div>{n}</div>' for n, c in (("الفريق الأول", TEAM_A), ("الفريق الثاني", TEAM_B)))
    body = title("اسم حيوان نبات جماد بلاد", "من يلعب؟") + f'''<div class="abs col panel" style="left:560px;right:560px;top:200px;padding:32px;gap:16px;border-radius:24px">
<div style="font-size:28px;font-weight:600">طريقة اللعب</div>
<div class="row" style="gap:16px"><div class="btn foc" style="flex:1;height:88px;font-size:28px;font-weight:700;gap:12px">{ic("tv", 36)}فريقان</div><div class="btn tile" style="flex:1;height:88px;font-size:28px;gap:12px;color:var(--text)">{ic("star", 36)}كل واحد لنفسه</div></div>
{stepper("عدد اللاعبين", "٤")}
<div class="col" style="gap:8px">{names}</div>
<div class="row" style="gap:16px;margin-top:8px">{big_btn("ابدأ", True, 560, "ok")}{big_btn("بالجوالات", False, 240, "phone")}</div></div>''' + keys(("اختيار", "ابدأ"), ("◀ ▶", "غيّر"), ("رجوع", "الألعاب"))
    return page(body, "glass")

def hand_over(player="لاعب ٣", why="ينظر وحده", icon="eyeoff"):
    return f'''<div class="abs" style="inset:0;background:rgba(6,8,14,.94)"></div><div class="abs col" style="left:0;right:0;top:230px;align-items:center;gap:24px">
<div class="btn" style="width:160px;height:160px;border-radius:40px;background:var(--tile);color:var(--accent)">{ic(icon, 96, 1.5)}</div><div style="font-size:72px;font-weight:700">غطّوا عيونكم</div>
<div class="sub" style="font-size:34px">سلّموا الريموت إلى {player}، {why}</div>
<div style="margin-top:32px">{big_btn("أنا جاهز", True, 480, "ok")}</div><div class="sub" style="font-size:22px;margin-top:8px">اضغط اختيار مطوّلًا</div></div>'''

def screen(body, look="glass"):
    return page(body, look)

SHOTS = {}

# ================= 1 name animal plant thing place
def g1_play():
    cats = "".join(f'<div class="panel col" style="flex:1;height:200px;padding:16px;gap:8px;align-items:center;justify-content:center;border-radius:24px"><div class="btn" style="width:64px;height:64px;color:var(--accent)">{ic(i, 40)}</div><div style="font-size:30px;font-weight:600">{n}</div></div>' for n, i in (("اسم", "star"), ("حيوان", "movie"), ("نبات", "leaf"), ("جماد", "hex"), ("بلاد", "tv")))
    return screen(team_bar(turn=-1, rounds="الجولة ٣ من ٥") + title("اسم حيوان نبات جماد بلاد") + f'''<div class="abs row" style="left:96px;right:96px;top:230px;gap:48px;justify-content:center;align-items:center">
<div class="panel btn" style="width:360px;height:360px;font-size:240px;font-weight:700;border-radius:48px;color:var(--accent)">م</div>
<div class="col" style="gap:32px;align-items:flex-end"><div style="font-size:36px">اكتبوا بسرعة على الورق، والكل يقرأ بصوت عالي</div>{timer(42, 60)}</div></div>
<div class="abs row" style="left:96px;right:96px;top:640px;gap:16px">{cats}</div>''' + keys(("اختيار", "وقف الوقت"), ("رجوع", "اترك الجولة")))
def g1_score():
    rows = "".join(f'<div class="tile row" style="height:88px;padding:0 24px;gap:24px;border-radius:16px;{"" }"><div style="flex:1;font-size:30px;font-weight:600">{n}</div>' + "".join(f'<div class="btn {"foc" if (i==0 and v==10) else "tile"}" style="width:112px;height:56px;font-size:28px;font-weight:700;color:var(--text)">{v}</div>' for v in (0, 5, 10)) + '</div>' for i, n in enumerate(("حيوان: «ماعز» (سعد)", "نبات: «ملوخية» (نورة)", "بلاد: «مصر» (سعد ونورة)", "جماد: «مكتب» (سعد)")))
    return screen(team_bar(turn=-1, rounds="الجولة ٣ من ٥") + title("النقاط", "اللي كتبه واحد بس: ١٠. اللي كتبه أكثر من واحد: ٥") + f'<div class="abs col" style="left:352px;right:352px;top:230px;gap:16px">{rows}</div><div class="abs row" style="left:352px;bottom:130px">{big_btn("الجولة الجاية", False, 400, "ok")}</div>' + keys(("◀ ▶", "٠ أو ٥ أو ١٠"), ("▲ ▼", "الصف"), ("اختيار", "تأكيد")))
def g1_end():
    return screen(title("الفائز: الفريق الأول", "٥ جولات") + f'''<div class="abs row" style="left:96px;right:96px;top:260px;gap:48px;justify-content:center">
<div class="panel col" style="width:560px;height:420px;padding:32px;gap:16px;align-items:center;justify-content:center;border-radius:32px;box-shadow:0 0 0 4px {TEAM_A}"><div style="font-size:34px">الفريق الأول</div><div style="font-size:144px;font-weight:700">١٢٠</div><div class="sub" style="font-size:24px">أكثر حرف حقق نقاط: «م»</div></div>
<div class="panel col" style="width:560px;height:420px;padding:32px;gap:16px;align-items:center;justify-content:center;border-radius:32px"><div style="font-size:34px">الفريق الثاني</div><div style="font-size:144px;font-weight:700">٩٠</div></div></div>
<div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("لعبة ثانية", True, 360, "ok")}{big_btn("نعيد", False, 360, "sync" if False else "ok")}</div>''' + keys(("اختيار", "لعبة ثانية"), ("رجوع", "الألعاب")))

# ================= 2 odd one out
def g2_secret():
    return screen(title("واحد مو معنا", "اللاعب ٣ من ٥") + f'''<div class="abs col panel" style="left:560px;right:560px;top:230px;height:480px;padding:32px;gap:24px;align-items:center;justify-content:center;border-radius:32px">
<div class="sub" style="font-size:28px">الكلمة السرّية</div><div style="font-size:120px;font-weight:700;color:var(--accent)">الجمل</div><div class="sub" style="font-size:26px;text-align:center">لا تقولها. لمّح بكلمة وحدة بس لما يجي دورك</div></div>
<div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("خبّيتها، مرّر", True, 480, "eyeoff")}</div>''' + keys(("اختيار", "أخفِ الكلمة ومرّر")))
def g2_vote():
    ppl = "".join(f'<div class="{"btn foc" if i==2 else "panel btn"}" style="flex:1;height:200px;flex-direction:column;gap:12px;font-size:30px;font-weight:600;border-radius:24px;color:var(--text)"><div class="btn" style="width:72px;height:72px;border-radius:50%;background:var(--tile);font-size:36px;color:var(--accent)">{n[-1]}</div>{n}</div>' for i, n in enumerate(("لاعب ١", "لاعب ٢", "لاعب ٣", "لاعب ٤", "لاعب ٥")))
    return screen(title("مين هو الغريب؟", "كل واحد لمّح بكلمة، وبعدين صوّتوا") + f'<div class="abs" style="left:96px;top:54px">{timer(48, 90, 480)}</div><div class="abs row" style="left:96px;right:96px;top:300px;gap:16px">{ppl}</div><div class="abs sub" style="left:0;right:0;top:560px;text-align:center;font-size:30px">الكل يشير بإصبعه على اللاعب، واختاروا مين الأكثر أصوات بالريموت</div>' + keys(("◀ ▶", "اللاعب"), ("اختيار", "هذا هو")))
def g2_reveal():
    return screen(title("اللاعب ٣ هو الغريب", "") + f'''<div class="abs row" style="left:96px;right:96px;top:240px;gap:48px;justify-content:center"><div class="panel col" style="width:560px;height:360px;padding:32px;align-items:center;justify-content:center;gap:16px;border-radius:32px"><div class="sub" style="font-size:28px">الكلمة كانت</div><div style="font-size:104px;font-weight:700;color:var(--accent)">الجمل</div></div>
<div class="panel col" style="width:560px;height:360px;padding:32px;align-items:center;justify-content:center;gap:16px;border-radius:32px"><div class="sub" style="font-size:28px">الغريب حاول يخمّن</div><div style="font-size:64px;font-weight:700">الحصان؟</div><div style="font-size:30px;color:{GREEN}">غلط: الغريب خسر</div></div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center">{big_btn("جولة ثانية", True, 400, "ok")}</div>''' + keys(("اختيار", "جولة ثانية"), ("رجوع", "الألعاب")))

# ================= 3 without words
def g3_play():
    return screen(team_bar(turn=0, rounds="الدور ٢ من ٦") + f'<div class="abs" style="right:96px;top:54px">{timer(36, 60, 480)}</div>' + f'''<div class="abs col panel" style="left:480px;right:480px;top:230px;height:520px;padding:32px;align-items:center;justify-content:center;gap:24px;border-radius:32px">
<div class="sub" style="font-size:28px">صِف الكلمة بلا ما تقولها</div><div style="font-size:136px;font-weight:700;color:var(--accent)">سيارة</div><div class="sub" style="font-size:26px">ممنوع: عجلة، سواقة، مركبة</div></div>
<div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("تخطّي", False, 360, "back")}{big_btn("عرفوها", True, 480, "check")}</div>''' + keys(("اختيار", "عرفوها"), ("◀", "تخطّي")))
def g3_turn():
    return screen(title("دور الفريق الأول", "اللي يصف: لاعب ٢ من الفريق الأول") + hand_over("لاعب ٢", "يشوف الكلمة وحده").replace('<div class="abs" style="inset:0;background:rgba(6,8,14,.94)"></div>', '<div class="abs" style="inset:0;background:rgba(6,8,14,.94)"></div>'))
def g3_end():
    chips = "".join(f'<div class="tile row" style="height:64px;padding:0 24px;gap:16px;font-size:26px;border-radius:16px;justify-content:space-between"><span>{w}</span><span style="color:{GREEN if ok else "var(--sub)"}">{"عرفوها" if ok else "تخطّوها"}</span></div>' for w, ok in (("سيارة", True), ("قهوة", True), ("مطار", False), ("بحر", True)))
    return screen(team_bar("الفريق الأول", "٣٠", "الفريق الثاني", "٢٠", 0, "الدور ٢ من ٦") + title("انتهى الوقت") + f'<div class="abs col" style="left:520px;right:520px;top:230px;gap:8px">{chips}</div><div class="abs row" style="left:520px;right:520px;top:560px;gap:16px"><div class="panel col" style="flex:1;height:160px;align-items:center;justify-content:center;border-radius:24px"><div style="font-size:72px;font-weight:700">+٣</div><div class="sub" style="font-size:24px">للفريق الأول</div></div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center">{big_btn("دور الفريق الثاني", True, 480, "ok")}</div>' + keys(("اختيار", "التالي")))

# ================= 4 missing word
def g4_q():
    opts = "".join(f'<div class="{"btn foc" if i==1 else "panel btn"}" style="height:120px;font-size:38px;font-weight:600;border-radius:24px;color:var(--text);flex:1">{o}</div>' for i, o in enumerate(("الصيف", "اللبن", "الحليب", "العسل")))
    return screen(team_bar(turn=1, rounds="سؤال ٤ من ١٠") + title("الكلمة المفقودة") + f'<div class="abs" style="right:96px;top:112px">{timer(18, 30, 360)}</div><div class="abs col" style="left:240px;right:240px;top:260px;gap:48px;align-items:center"><div style="font-size:72px;font-weight:700;line-height:1.5;text-align:center">الصيف ضيّعتِ <span style="display:inline-block;min-width:240px;border-bottom:6px solid var(--accent);color:var(--accent)">&nbsp;</span></div><div class="row" style="gap:16px;width:100%;margin-top:48px">{opts}</div></div>' + keys(("◀ ▶", "اختر"), ("اختيار", "ثبّت جوابك")))
def g4_reveal():
    return screen(team_bar(turn=1, rounds="سؤال ٤ من ١٠") + title("الكلمة المفقودة") + f'''<div class="abs col" style="left:240px;right:240px;top:260px;gap:32px;align-items:center"><div style="font-size:72px;font-weight:700;line-height:1.5;text-align:center">الصيف ضيّعتِ <span style="color:{GREEN}">اللبن</span></div>
<div class="panel row" style="gap:24px;padding:24px 32px;border-radius:24px;font-size:30px;line-height:1.5;max-width:1200px"><div class="btn" style="width:56px;height:56px;border-radius:16px;background:{GREEN};color:#0b1530;flex:none">{ic("check", 36, 2.6)}</div><div>يُقال لمن ضيّع الفرصة في وقتها ثم طلبها بعد فوات الأوان.</div></div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center">{big_btn("السؤال الجاي", True, 480, "ok")}</div>''' + keys(("اختيار", "التالي")))
def g4_end():
    return g1_end().replace("أكثر حرف حقق نقاط: «م»", "جاوب ٧ من ١٠").replace("٥ جولات", "١٠ أسئلة").replace("١٢٠", "٧٠").replace("٩٠", "٥٠")

# ================= 5 hex letters
def hexgrid(sel=(2, 3), taken=None):
    taken = taken or {(0, 0): "A", (0, 1): "A", (1, 1): "B", (2, 0): "B", (3, 2): "A", (1, 3): "B", (4, 4): "A"}
    letters = "أبتثجحخدذرزسشصضطظعغفقكلمنهوي"
    import math
    R = 58
    cells = []
    k = 0
    for r in range(5):
        for c in range(7):
            x = 64 + c * R * math.sqrt(3) + (R * math.sqrt(3) / 2 if r % 2 else 0)
            y = 64 + r * R * 1.5
            pts = " ".join(f"{x + R*0.93*math.cos(math.radians(60*i-30)):.1f},{y + R*0.93*math.sin(math.radians(60*i-30)):.1f}" for i in range(6))
            t = taken.get((r, c))
            fill = TEAM_A if t == "A" else TEAM_B if t == "B" else "rgba(255,255,255,.10)"
            foc = (r, c) == sel
            cells.append(f'<polygon points="{pts}" fill="{"#FFFFFF" if foc else fill}" stroke="{"var(--ring)" if foc else "rgba(255,255,255,.22)"}" stroke-width="{4 if foc else 2}"/><text x="{x:.1f}" y="{y+16:.1f}" text-anchor="middle" font-size="44" font-weight="700" fill="{"#14161a" if foc or t else "#fff"}" font-family="DejaVu Sans">{letters[k % len(letters)]}</text>')
            k += 1
    return f'<svg width="760" height="{64 + 4*R*1.5 + 64:.0f}" style="display:block">{"".join(cells)}</svg>'
def g5_board():
    return screen(team_bar(turn=0, rounds="الأزرق يوصل اليمين واليسار، الأحمر فوق وتحت") + title("تحدي الحروف", "اختر حرفًا والفريق يجاوب سؤاله") + f'<div class="abs panel" style="left:520px;top:230px;padding:24px;border-radius:32px">{hexgrid()}</div>' + keys(("▲ ▼ ◀ ▶", "تحرّك"), ("اختيار", "اختر الحرف")))
def g5_q():
    opts = "".join(f'<div class="{"btn foc" if i==0 else "panel btn"}" style="height:96px;font-size:34px;font-weight:600;border-radius:20px;color:var(--text);flex:1">{o}</div>' for i, o in enumerate(("مكة المكرمة", "الرياض", "جدة", "الدمام")))
    return screen(team_bar(turn=0, rounds="حرف «م»") + title("تحدي الحروف") + f'<div class="abs col" style="left:240px;right:240px;top:230px;gap:48px;align-items:center"><div class="row" style="gap:32px"><div class="panel btn" style="width:160px;height:160px;font-size:104px;font-weight:700;border-radius:40px;color:var(--accent)">م</div><div style="font-size:52px;font-weight:700;line-height:1.5">مدينة تبدأ بحرف الميم فيها الكعبة المشرّفة</div></div><div class="row" style="gap:16px;width:100%">{opts}</div></div>' + keys(("◀ ▶", "اختر"), ("اختيار", "ثبّت")))
def g5_win():
    return screen(team_bar("الفريق الأول", "", "الفريق الثاني", "", 0, "فاز الفريق الأول") + title("وصلوا الحافة") + f'<div class="abs panel" style="left:520px;top:230px;padding:24px;border-radius:32px">{hexgrid((9, 9), {(0,0):"A",(0,1):"A",(1,1):"A",(1,2):"A",(2,2):"A",(2,3):"A",(3,3):"A",(3,4):"A",(4,4):"A",(4,5):"A",(1,4):"B",(2,5):"B",(3,0):"B"})}</div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center">{big_btn("لعبة ثانية", True, 400, "ok")}</div>' + keys(("اختيار", "لعبة ثانية")))

# ================= 6 dominoes
def tile_dom(a, b, w=96, vertical=False, foc=False, hide=False):
    def dots(n):
        pos = {0: [], 1: [(1, 1)], 2: [(0, 0), (2, 2)], 3: [(0, 0), (1, 1), (2, 2)], 4: [(0, 0), (0, 2), (2, 0), (2, 2)], 5: [(0, 0), (0, 2), (1, 1), (2, 0), (2, 2)], 6: [(0, 0), (0, 2), (1, 0), (1, 2), (2, 0), (2, 2)]}[n]
        return "".join(f'<circle cx="{14 + x*18}" cy="{14 + y*18}" r="5" fill="#14161a"/>' for x, y in pos)
    s = f'<svg width="{w}" height="{w//2*2+4}" viewBox="0 0 96 52" style="display:block"><rect x="1" y="1" width="94" height="50" rx="10" fill="{"#FFFFFF" if not hide else "rgba(255,255,255,.14)"}" stroke="{"var(--ring)" if foc else "rgba(0,0,0,.25)"}" stroke-width="{4 if foc else 1}"/>'
    if not hide: s += f'<path d="M48 8v36" stroke="#999" stroke-width="2"/><g transform="translate(0,0) scale(.46,.9)">{dots(a)}</g><g transform="translate(48,0) scale(.46,.9)">{dots(b)}</g>'
    return s + '</svg>'
def board_chain():
    seq = [(3, 6), (6, 6), (6, 1), (1, 4), (4, 4), (4, 0), (0, 2)]
    return '<div class="row" style="gap:8px;justify-content:center">' + "".join(tile_dom(b, a) for a, b in seq) + '</div>'
def g6_board():
    return screen(team_bar("سعد", "٢ قطع", "نورة", "٥ قطع", 0, "دور سعد") + title("دومينو", "اللوحة على الشاشة، واليد سرّية") + f'<div class="abs panel col" style="left:96px;right:96px;top:260px;height:320px;padding:32px;gap:16px;justify-content:center;border-radius:32px">{board_chain()}<div class="sub" style="text-align:center;font-size:26px">الطرفان المفتوحان: ٣ و٢</div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("سعد: اكشف يدك", True, 480, "eye")}{big_btn("اسحب من الكومة", False, 400, "dom")}</div>' + keys(("اختيار", "اكشف اليد"), ("رجوع", "اترك اللعبة")))
def g6_hand():
    hand = "".join(f'<div style="{"transform:translateY(-16px)" if i==2 else ""}">{tile_dom(a, b, 112, foc=(i==2))}</div>' for i, (a, b) in enumerate(((3, 5), (2, 2), (3, 1), (5, 6), (0, 4))))
    return screen(title("يد سعد", "بس سعد يشوف. غطّوا عيونكم") + f'<div class="abs panel col" style="left:96px;right:96px;top:240px;height:260px;padding:32px;justify-content:center;border-radius:32px">{board_chain()}</div><div class="abs col" style="left:0;right:0;top:560px;align-items:center;gap:24px"><div class="row" style="gap:24px">{hand}</div><div class="sub" style="font-size:26px">الاختيار يضع القطعة عند الطرف ٣</div></div>' + keys(("◀ ▶", "اختر قطعة"), ("اختيار", "ضعها"), ("▼", "اخفِ اليد ومرّر")))
def g6_end():
    return screen(title("فازت نورة", "خلّصت قطعها") + f'<div class="abs row" style="left:96px;right:96px;top:260px;gap:48px;justify-content:center"><div class="panel col" style="width:560px;height:360px;padding:32px;align-items:center;justify-content:center;gap:16px;border-radius:32px;box-shadow:0 0 0 4px {TEAM_B}"><div style="font-size:34px">نورة</div><div style="font-size:120px;font-weight:700">+١٦</div><div class="sub" style="font-size:24px">نقاط قطع سعد المتبقية</div></div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("جولة ثانية", True, 400, "ok")}</div>' + keys(("اختيار", "جولة ثانية")))

# ================= 7 guess the word
AR_KEYS = ["ضصثقفغعهخحج", "شسيبلاتنمكط", "ئءؤرىةوزظد"]
def word_rows(rows):
    out = []
    for r in rows:
        cells = "".join(f'<div class="btn" style="width:96px;height:96px;border-radius:16px;font-size:56px;font-weight:700;background:{ {"g": GREEN, "a": AMBER, "x": "rgba(255,255,255,.22)", "e": "rgba(255,255,255,.08)", "t": "rgba(255,255,255,.08)"}[s] };color:{"#0b1530" if s in "ga" else "var(--text)"};{"box-shadow:inset 0 0 0 3px var(--sub)" if s=="t" else ""}">{ch}</div>' for ch, s in r)
        out.append(f'<div class="row" style="gap:8px;justify-content:center">{cells}</div>')
    return "".join(out)
def keyboard(foc="ر", used=None):
    used = used or {}
    rows = []
    for row in AR_KEYS:
        rows.append('<div class="row" style="gap:8px;justify-content:center">' + "".join(f'<div class="btn {"foc" if k==foc else ""}" style="width:80px;height:72px;border-radius:12px;font-size:36px;font-weight:600;color:var(--text);background:{ {"g": GREEN, "a": AMBER, "x": "rgba(255,255,255,.06)"}.get(used.get(k), "var(--tile)") if k!=foc else "var(--foc)"}">{k}</div>' for k in row) + '</div>')
    rows.append(f'<div class="row" style="gap:8px;justify-content:center"><div class="btn tile" style="width:200px;height:72px;font-size:26px;color:var(--text)">حذف</div><div class="btn tile" style="width:360px;height:72px;font-size:26px;font-weight:700;color:var(--text)">تأكيد</div></div>')
    return '<div class="col" style="gap:8px">' + "".join(rows) + '</div>'
def g7_play():
    rows = [[("ك", "x"), ("ت", "a"), ("ا", "g"), ("ب", "x"), ("ة", "x")], [("م", "x"), ("د", "g"), ("ر", "a"), ("س", "x"), ("ة", "x")], [("ن", "t"), ("ج", "t"), ("", "e"), ("", "e"), ("", "e")], [("", "e")] * 5, [("", "e")] * 5, [("", "e")] * 5]
    return screen(title("خمّن الكلمة", "المحاولة ٣ من ٦") + f'<div class="abs row" style="left:96px;right:96px;top:150px;gap:64px;justify-content:center;align-items:flex-start"><div class="col" style="gap:8px">{word_rows(rows)}</div><div style="margin-top:16px">{keyboard("ر", {"ك": "x", "ت": "a", "ا": "g", "ب": "x", "ة": "x", "م": "x", "د": "g", "ر": "a", "س": "x"})}</div></div>' + keys(("▲ ▼ ◀ ▶", "تنقّل بين الحروف"), ("اختيار", "اكتب الحرف"), ("رجوع", "امسح")))
def g7_hint():
    rows = [[("ك", "x"), ("ت", "a"), ("ا", "g"), ("ب", "x"), ("ة", "x")], [("م", "x"), ("د", "g"), ("ر", "a"), ("س", "x"), ("ة", "x")], [("ش", "x"), ("ج", "g"), ("ر", "g"), ("ة", "g"), ("", "e")]]
    return screen(title("خمّن الكلمة", "المحاولة ٣ من ٦: أخضر في مكانه، أصفر في الكلمة بمكان ثاني") + f'<div class="abs row" style="left:96px;right:96px;top:170px;gap:64px;justify-content:center;align-items:flex-start"><div class="col" style="gap:8px">{word_rows(rows + [[("", "t")] * 5] * 0)}</div></div><div class="abs row" style="left:0;right:0;top:520px;justify-content:center;gap:32px"><div class="panel row" style="padding:24px 32px;gap:24px;border-radius:24px;font-size:30px"><div style="width:48px;height:48px;border-radius:12px;background:{GREEN}"></div>مكانه صح<div style="width:48px;height:48px;border-radius:12px;background:{AMBER}"></div>بمكان ثاني<div style="width:48px;height:48px;border-radius:12px;background:rgba(255,255,255,.22)"></div>مو في الكلمة</div></div>' + keys(("اختيار", "كمّل")))
def g7_end():
    rows = [[("ك", "x"), ("ت", "a"), ("ا", "g"), ("ب", "x"), ("ة", "x")], [("م", "x"), ("د", "g"), ("ر", "a"), ("س", "x"), ("ة", "x")], [("ش", "g"), ("ج", "g"), ("ر", "g"), ("ة", "g"), ("", "e")]]
    rows = [[(c, "g") for c in "شجرة"]]
    return screen(title("أحسنت", "في المحاولة ٣") + f'<div class="abs col" style="left:0;right:0;top:230px;align-items:center;gap:32px"><div class="row" style="gap:8px">' + "".join(f'<div class="btn" style="width:128px;height:128px;border-radius:20px;background:{GREEN};color:#0b1530;font-size:80px;font-weight:700">{c}</div>' for c in "شجرة") + f'</div><div class="panel row" style="gap:24px;padding:24px 32px;border-radius:24px;font-size:30px;line-height:1.5;max-width:1100px"><div>الشجرة: نبات خشبي معمّر له ساق وجذع وأغصان، ومنها شجر السدر والأراك.</div></div></div><div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px">{big_btn("كلمة ثانية", True, 400, "ok")}</div>' + keys(("اختيار", "كلمة ثانية")))

# ================= the motion strip
def motion():
    ticks = [("تحريك المؤشر", 0, 170, "ease-out، ملء صلب من لون الشكل ١٧٠ مللي"), ("فتح لعبة", 0, 220, "تنزلق اللوحة وتبهت ٢٢٠ مللي"), ("قلب البطاقة (الإجابة)", 80, 280, "تدور ٢٨٠ مللي ثم تظهر الحقيقة"), ("غطّوا عيونكم", 0, 240, "تعتيم ٢٤٠ مللي، لا يُرى شيء بين اللاعبين"), ("عدّاد النقاط", 120, 400, "يعدّ من القديم للجديد ٤٠٠ مللي"), ("الرجوع", 0, 160, "١٦٠ مللي، بلا تأخير")]
    rows = "".join(f'''<div class="row" style="gap:24px;height:96px"><div style="width:360px;font-size:28px;font-weight:600">{t}</div><div style="position:relative;width:640px;height:56px;border-radius:12px;background:var(--tile)"><div style="position:absolute;left:{s*640//500}px;width:{d*640//500}px;height:56px;border-radius:12px;background:var(--accent);opacity:.9"></div></div><div style="width:120px;font-size:30px;font-weight:700">{s+d}</div><div class="sub" style="font-size:22px;width:560px">{n}</div></div>''' for t, s, d, n in ticks)
    scale = "".join(f'<div style="position:absolute;left:{x*640//500}px;top:0;height:32px;border-left:2px solid var(--line);font-size:18px;padding-left:6px" class="sub">{x}</div>' for x in (0, 100, 200, 300, 400, 500))
    body = title("حركة الألعاب", "كل شيء أقل من نصف ثانية، بلا ارتداد ولا وميض، والحركة تتوقف لو طُلب تقليل الحركة") + f'<div class="abs col" style="left:96px;right:96px;top:230px;gap:8px"><div class="row" style="gap:24px"><div style="width:360px"></div><div style="position:relative;width:640px;height:32px">{scale}</div></div>{rows}</div>'
    return screen(body)

def build():
    S = {}
    S["games-01-hub"] = hub("glass")
    S["games-02-quick-pick"] = quickpick()
    S["games-03-name-play"] = g1_play(); S["games-04-name-score"] = g1_score(); S["games-05-name-end"] = g1_end()
    S["games-06-odd-handover"] = screen(hand_over("لاعب ٣", "يشوف الكلمة وحده"))
    S["games-07-odd-secret"] = g2_secret(); S["games-08-odd-vote"] = g2_vote(); S["games-09-odd-reveal"] = g2_reveal()
    S["games-10-words-handover"] = g3_turn(); S["games-11-words-play"] = g3_play(); S["games-12-words-end"] = g3_end()
    S["games-13-missing-question"] = g4_q(); S["games-14-missing-reveal"] = g4_reveal(); S["games-15-missing-end"] = g4_end()
    S["games-16-hex-board"] = g5_board(); S["games-17-hex-question"] = g5_q(); S["games-18-hex-win"] = g5_win()
    S["games-19-domino-board"] = g6_board(); S["games-20-domino-hand"] = g6_hand(); S["games-21-domino-end"] = g6_end()
    S["games-22-guess-play"] = g7_play(); S["games-23-guess-feedback"] = g7_hint(); S["games-24-guess-end"] = g7_end()
    S["games-25-hub-light"] = hub("sand")
    S["games-26-hub-en"] = hub("glass", en=True)
    S["games-27-motion"] = motion()
    return S

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    S = build()
    for n, h in S.items(): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(S), "pages")
