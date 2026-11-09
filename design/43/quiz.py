#!/usr/bin/env python3
"""«المسابقة» (live quiz), mockups only: everyone answers on the phone, faster = more points, the leaderboard on the TV. Our own game and look: the answers are
told apart by a LETTER (أ ب ج د / A B C D) on identical tiles, never by a shape or colour code; the colours belong to the PLAYERS (their name chip), the one amber
accent is ours (graphite look), the focus is a solid white fill with its ink. Big sizes: question >= 88 px, answers >= 56 px, secondary >= 36 px, 5 % safe area.
   python3 design/43/quiz.py /tmp/qz && node design/43/render.js /tmp/qz design/43      (names quiz-*)    DejaVu Sans stands in for the private Thmanyah."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen import ic

AMB = "#F5B547"; INK = "#14161A"; OK = "#3DDC97"; BAD = "#FF6B6B"
PLAYERS = [("سلمان", "Salman", "#5AA9F0"), ("نورة", "Noura", "#F2705A"), ("خالد", "Khalid", "#7BD88F"), ("ريم", "Reem", "#C28BFF"),
           ("فهد", "Fahd", "#FFD15C"), ("هند", "Hind", "#4FD1C5"), ("ماجد", "Majed", "#FF8FB1"), ("لينا", "Lina", "#A3B1FF")]
LET_AR = "أبجد"; LET_EN = "ABCD"
S = {}

def page(body, w=1920, h=1080, bg="radial-gradient(ellipse at 30% 0%,#17303F,#0B1118 72%)", text="#fff", sub="rgba(255,255,255,.78)", en=False):
    return f'''<!doctype html><html lang="{"en" if en else "ar"}" dir="{"ltr" if en else "rtl"}"><head><meta charset="utf-8"><style>
*{{box-sizing:border-box;margin:0;padding:0}}html,body{{width:{w}px;height:{h}px;overflow:hidden}}
body{{font-family:'DejaVu Sans',sans-serif;direction:{"ltr" if en else "rtl"};position:relative;background:{bg};color:{text}}}
.abs{{position:absolute}}.row{{display:flex;align-items:center}}.col{{display:flex;flex-direction:column}}.btn{{display:flex;align-items:center;justify-content:center}}
.sub{{color:{sub}}}.foc{{background:#fff;color:{INK};box-shadow:0 0 0 6px rgba(255,255,255,.35)}}
</style></head><body>{body}</body></html>'''

def hint(*items):
    p = "".join(f'<div class="row" style="gap:12px"><div class="btn" style="min-width:64px;height:48px;padding:0 16px;border-radius:12px;background:rgba(255,255,255,.18);font-size:30px;font-weight:700">{k}</div><div style="font-size:32px;color:rgba(255,255,255,.85)">{w}</div></div>' for k, w in items)
    return f'<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:48px;height:56px">{p}</div>'
def head(title, sub="", en=False):
    side = "left" if en else "right"; al = "flex-start" if en else "flex-end"
    return f'<div class="abs col" style="{side}:96px;top:54px;align-items:{al};gap:8px"><div style="font-size:56px;font-weight:700">{title}</div>' + (f'<div class="sub" style="font-size:36px">{sub}</div>' if sub else '') + '</div>'
def chip(i, size=72, en=False, on=False):
    n = PLAYERS[i][1 if en else 0]
    return f'<div class="row" style="gap:16px;height:{size}px;padding:0 24px 0 24px;border-radius:20px;background:{PLAYERS[i][2]};color:{INK};font-size:{int(size*.5)}px;font-weight:700;white-space:nowrap">{n}</div>'
def cta(t, w=640, foc=True, icon="check"):
    return f'<div class="btn {"foc" if foc else ""}" style="height:104px;width:{w}px;border-radius:28px;font-size:44px;font-weight:700;gap:16px;{"" if foc else "background:rgba(255,255,255,.14)"}">{ic(icon, 48)}{t}</div>'
def timer(sec, total=20, w=720, col=AMB):
    return f'<div class="row" style="gap:24px;width:{w}px"><div style="font-size:96px;font-weight:700;line-height:1;min-width:140px;text-align:center">{sec}</div><div style="flex:1;height:24px;border-radius:12px;background:rgba(255,255,255,.16)"><div style="width:{int((w-164)*sec/total)}px;height:24px;border-radius:12px;background:{col}"></div></div></div>'
def qr(size=400):
    import random
    r = random.Random(7); n = 25; c = size // n
    cells = ""
    for y in range(n):
        for x in range(n):
            finder = (x < 7 and y < 7) or (x > n - 8 and y < 7) or (x < 7 and y > n - 8)
            if finder:
                a, b = (x % (n - 7) if x > n - 8 else x), (y % (n - 7) if y > n - 8 else y)
                on = a in (0, 6) or b in (0, 6) or (2 <= a <= 4 and 2 <= b <= 4)
            else: on = r.random() < .48
            if on: cells += f'<rect x="{x*c}" y="{y*c}" width="{c}" height="{c}"/>'
    return f'<div style="width:{size+64}px;height:{size+64}px;border-radius:32px;background:#fff;display:flex;align-items:center;justify-content:center"><svg width="{size}" height="{size}" fill="{INK}">{cells}</svg></div>'

# ---------------------------------------------------------------- TV
def lobby(en=False):
    T = (lambda a, e: e) if en else (lambda a, e: a)
    pl = "".join(f'<div style="{"animation:none;transform:scale(1.06)" if i == 5 else ""}">{chip(i, 80, en)}</div>' for i in range(6))
    return page(head(T("المسابقة", "Quiz"), T("الكل يجاوب من جواله", "Everyone answers on their phone"), en) +
      f'''<div class="abs col" style="{"left" if en else "right"}:96px;top:190px;width:760px;gap:24px;align-items:center">{qr(300)}
        <div class="sub" style="font-size:40px">{T("امسح الرمز بكاميرا الجوال", "Scan with the phone camera")}</div>
        <div class="row" style="gap:24px"><div class="sub" style="font-size:40px">{T("أو اكتب الرمز", "or type the code")}</div><div style="font-size:96px;font-weight:700;letter-spacing:8px;color:{AMB};line-height:1">{T("٧٤٢٩", "7429")}</div></div></div>
      <div class="abs col" style="{"right" if en else "left"}:96px;top:230px;width:900px;gap:32px"><div style="font-size:64px;font-weight:700">{T("٦ لاعبين دخلوا", "6 players are in")}</div><div class="row" style="flex-wrap:wrap;gap:24px;max-width:900px">{pl}</div></div>
      <div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center">{cta(T("ابدأ", "Start"), 640, True, "check")}</div>''' + hint(*([("OK", "Start"), ("Back", "Leave")] if en else [("اختيار", "ابدأ"), ("رجوع", "اترك الغرفة")])), en=en)
S["quiz-01-tv-lobby"] = lobby(); S["quiz-01b-tv-lobby-en"] = lobby(True)

def setup(en=False):
    T = (lambda a, e: e) if en else (lambda a, e: a)
    def seg(label, opts, sel, focus=False):
        o = "".join(f'<div class="btn {"foc" if (focus and i == sel) else ""}" style="min-width:{w}px;height:96px;padding:0 32px;border-radius:24px;font-size:44px;font-weight:700;{"" if (focus and i == sel) else ("background:"+AMB+";color:"+INK if i == sel else "background:rgba(255,255,255,.12)")}">{t}</div>' for i, (t, w) in enumerate(opts))
        return f'<div class="row" style="gap:32px"><div style="font-size:44px;width:260px;font-weight:600">{label}</div><div class="row" style="gap:16px">{o}</div></div>'
    cats = [(T("عام", "General"), 200), (T("رياضة", "Sport"), 200), (T("تاريخ", "History"), 200), (T("علوم", "Science"), 200), (T("أفلام", "Films"), 200)]
    lv = [(T("سهل", "Easy"), 200), (T("وسط", "Medium"), 200), (T("صعب", "Hard"), 200)]
    ct = [("٥", 140), ("١٠", 140), ("١٥", 140)] if not en else [("5", 140), ("10", 140), ("15", 140)]
    kd = [(T("عادي", "Normal"), 240), (T("للأطفال", "Kids"), 240)]
    return page(head(T("جهّز المسابقة", "Set up the quiz"), T("٦ لاعبين جاهزين", "6 players ready"), en) +
      f'<div class="abs col" style="left:96px;right:96px;top:230px;gap:48px">{seg(T("القسم", "Category"), cats, 1, True)}{seg(T("المستوى", "Level"), lv, 1)}{seg(T("الأسئلة", "Questions"), ct, 1)}{seg(T("النمط", "Mode"), kd, 0)}</div>'
      f'<div class="abs row" style="left:0;right:0;bottom:150px;justify-content:center">{cta(T("ابدأ المسابقة", "Start the quiz"), 720)}</div>' + hint(*([("▲ ▼", "Row"), ("◀ ▶", "Choice"), ("OK", "Start")] if en else [("▲ ▼", "السطر"), ("◀ ▶", "الاختيار"), ("اختيار", "ابدأ")])), en=en)
S["quiz-02-tv-setup"] = setup(); S["quiz-02b-tv-setup-en"] = setup(True)

QA = ("كم عدد أضلاع المسدّس؟", ["أربعة", "خمسة", "ستة", "ثمانية"], 2)
QE = ("How many sides does a hexagon have?", ["Four", "Five", "Six", "Eight"], 2)
def question(en=False, reveal=False):
    T = (lambda a, e: e) if en else (lambda a, e: a)
    q, ans, right = QE if en else QA
    L = LET_EN if en else LET_AR
    votes = [1, 1, 3, 1] if reveal else [0, 0, 0, 0]
    tiles = ""
    for i, a in enumerate(ans):
        st = ""
        if reveal:
            st = f"background:{OK};color:{INK}" if i == right else "background:rgba(255,255,255,.08);opacity:.55"
        else: st = "background:rgba(255,255,255,.12)"
        bar = ""
        if reveal:
            bar = f'<div style="position:absolute;left:{24 if en else "auto"}px;right:{"auto" if en else 24}px;bottom:16px;height:12px;width:{votes[i]*80}px;border-radius:6px;background:{"rgba(0,0,0,.35)" if i == right else "rgba(255,255,255,.5)"}"></div>'
        tiles += f'<div class="row" style="position:relative;height:200px;padding:0 40px;gap:32px;border-radius:32px;{st}"><div class="btn" style="width:104px;height:104px;border-radius:28px;background:{"rgba(0,0,0,.18)" if (reveal and i == right) else "rgba(255,255,255,.18)"};font-size:64px;font-weight:700">{L[i]}</div><div style="font-size:72px;font-weight:700;flex:1">{a}</div>{ic("check", 64) if (reveal and i == right) else ""}{(f"<div style=font-size:56px;font-weight:700>{votes[i] if en else '١٢٣٤'[votes[i]-1]}</div>" if reveal else "")}{bar}</div>'
    top = (f'<div class="abs row" style="{"left" if en else "right"}:96px;top:54px;gap:32px"><div class="sub" style="font-size:40px">{T("السؤال ٣ من ١٠","Question 3 of 10")}</div></div>'
           + (f'<div class="abs" style="{"right" if en else "left"}:96px;top:44px">{timer(14)}</div>' if not reveal else f'<div class="abs row" style="{"right" if en else "left"}:96px;top:54px;gap:16px;font-size:44px;font-weight:700">{T("أسرع إجابة صحيحة: نورة، ٢٫١ ثانية","Fastest right answer: Noura, 2.1 s")}</div>'))
    cnt = '' if reveal else f'<div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:16px;font-size:48px"><div class="btn" style="min-width:112px;height:72px;border-radius:20px;background:rgba(255,255,255,.14);font-weight:700">{T("٤ / ٦","4 / 6")}</div><div class="sub">{T("جاوبوا","answered")}</div></div>'
    return page(top + f'<div class="abs col" style="left:96px;right:96px;top:170px;align-items:{"flex-start" if en else "flex-end"}"><div style="font-size:96px;font-weight:700;line-height:1.2;width:100%">{q}</div></div>'
      f'<div class="abs" style="left:96px;right:96px;top:420px;display:grid;grid-template-columns:1fr 1fr;gap:24px">{tiles}</div>' + cnt +
      (hint(*([("OK", "Next")] if en else [("اختيار", "التالي")])) if reveal else ""), en=en)
S["quiz-03-tv-question"] = question(); S["quiz-03b-tv-question-en"] = question(True)
S["quiz-04-tv-reveal"] = question(reveal=True); S["quiz-04b-tv-reveal-en"] = question(en=True, reveal=True)

def board(en=False):
    T = (lambda a, e: e) if en else (lambda a, e: a)
    rows = [(1, 0, 2480, "+٨٢٠", "+820", 1), (0, 1, 2310, "+٧٤٠", "+740", 0), (2, 0, 2090, "+٦٥٠", "+650", 2), (3, 2, 1760, "+٥١٠", "+510", -1), (4, 0, 1500, "+٠", "+0", 0)]
    out = ""
    for rank, (pi, _, pts, d_ar, d_en, mv) in enumerate(rows, 1):
        mvh = {1: f'<div style="color:{OK};font-size:44px;font-weight:700">▲ ١</div>' if not en else f'<div style="color:{OK};font-size:44px;font-weight:700">▲ 1</div>', -1: f'<div style="color:{BAD};font-size:44px;font-weight:700">▼ {"1" if en else "١"}</div>', 0: '<div style="width:80px"></div>', 2: f'<div style="color:{OK};font-size:44px;font-weight:700">▲ {"1" if en else "١"}</div>'}[mv]
        top = rank == 1
        out += f'<div class="row" style="height:112px;padding:0 40px;gap:32px;border-radius:32px;background:{"rgba(245,181,71,.2)" if top else "rgba(255,255,255,.1)"};{"box-shadow:0 0 0 4px "+AMB if top else ""}"><div style="font-size:72px;font-weight:700;width:96px;text-align:center;color:{AMB if top else "#fff"}">{rank if en else "١٢٣٤٥"[rank-1]}</div>{chip(pi, 80, en)}<div style="flex:1"></div>{mvh}<div class="sub" style="font-size:44px;min-width:140px;text-align:center;direction:ltr">{d_en if en else d_ar}</div><div style="font-size:72px;font-weight:700;min-width:220px;text-align:center">{pts if en else "".join("٠١٢٣٤٥٦٧٨٩"[int(c)] for c in str(pts))}</div></div>'
    return page(head(T("الترتيب", "Leaderboard"), T("بعد السؤال ٣ من ١٠", "After question 3 of 10"), en) + f'<div class="abs col" style="left:240px;right:240px;top:190px;gap:16px">{out}</div>' +
      f'<div class="abs row" style="left:0;right:0;bottom:90px;justify-content:center">{cta(T("السؤال التالي", "Next question"), 640)}</div>', en=en)
S["quiz-05-tv-leaderboard"] = board(); S["quiz-05b-tv-leaderboard-en"] = board(True)

def podium(en=False):
    T = (lambda a, e: e) if en else (lambda a, e: a)
    def col(i, rank, h, pts):
        pts_s = str(pts) if en else "".join("٠١٢٣٤٥٦٧٨٩"[int(c)] for c in str(pts))
        return f'<div class="col" style="align-items:center;gap:20px;justify-content:flex-end"><div style="color:{AMB if rank==1 else "#fff"}">{ic("star", 96 if rank==1 else 64) if rank==1 else ""}</div>{chip(i, 88 if rank==1 else 72, en)}<div style="font-size:48px;font-weight:700">{pts_s}</div><div class="btn" style="width:320px;height:{h}px;border-radius:32px 32px 0 0;background:{"linear-gradient(180deg,#F5B547,#B9801F)" if rank==1 else "rgba(255,255,255,.16)"};color:{INK if rank==1 else "#fff"};font-size:120px;font-weight:700">{rank if en else "١٢٣"[rank-1]}</div></div>'
    return page(head(T("انتهت المسابقة", "Quiz over"), T("الفائزون", "The winners"), en) + f'<div class="abs row" style="left:0;right:0;top:200px;bottom:300px;justify-content:center;align-items:flex-end;gap:32px;direction:ltr">{col(0,2,300,7100)}{col(1,1,440,7860)}{col(2,3,220,6420)}</div>' +
      f'<div class="abs row" style="left:0;right:0;bottom:130px;justify-content:center;gap:32px">{cta(T("نلعب مرة ثانية", "Play again"), 640)}{cta(T("ألعاب العائلة", "Family games"), 560, False, "chev")}</div>' + hint(*([("OK", "Play again"), ("Back", "Games")] if en else [("اختيار", "نلعب مرة ثانية"), ("رجوع", "الألعاب")])), en=en)
S["quiz-06-tv-podium"] = podium(); S["quiz-06b-tv-podium-en"] = podium(True)

# ---------------------------------------------------------------- PHONE (360 px), four per sheet: ar dark, ar light, en dark, en light
def phone(content, dark, en, h=720):
    bg = "#12161C" if dark else "#F3F1EC"; tx = "#fff" if dark else "#1B1D21"; sb = "rgba(255,255,255,.7)" if dark else "rgba(27,29,33,.7)"
    return f'<div style="width:360px;height:{h}px;border-radius:32px;overflow:hidden;position:relative;background:{bg};color:{tx};direction:{"ltr" if en else "rtl"};box-shadow:0 0 0 2px rgba(255,255,255,.12),0 24px 60px rgba(0,0,0,.5);font-family:\'DejaVu Sans\',sans-serif" data-sub="{sb}">{content}</div>'
def sheet(fn, title):
    cells = "".join(f'<div class="col" style="gap:12px;align-items:center">{phone(fn(d, e), d, e)}<div style="font-size:24px;opacity:.7">{lab}</div></div>' for d, e, lab in ((1, 0, "عربي، داكن"), (0, 0, "عربي، فاتح"), (1, 1, "English, dark"), (0, 1, "English, light")))
    return page(f'<div class="abs" style="left:0;right:0;top:40px;text-align:center;font-size:36px;font-weight:700;opacity:.9">{title}</div><div class="abs row" style="left:0;right:0;top:112px;justify-content:center;gap:48px;align-items:flex-start;direction:ltr">{cells}</div>')
def sub(d): return "rgba(255,255,255,.7)" if d else "rgba(27,29,33,.7)"
def btnP(t, d, primary=True, h=56):
    st = f"background:{AMB};color:{INK}" if primary else f"background:{'rgba(255,255,255,.12)' if d else 'rgba(27,29,33,.08)'}"
    return f'<div class="btn" style="height:{h}px;border-radius:16px;font-size:20px;font-weight:700;{st}">{t}</div>'
def Tt(en): return (lambda a, e: e) if en else (lambda a, e: a)

def p_join(d, en):
    T = Tt(en)
    cols = "".join(f'<div style="width:44px;height:44px;border-radius:14px;background:{c};{"box-shadow:0 0 0 3px "+("#fff" if d else INK) if i == 3 else ""}"></div>' for i, (_, _, c) in enumerate(PLAYERS))
    f = "rgba(255,255,255,.1)" if d else "rgba(27,29,33,.07)"
    return f'''<div class="col" style="padding:32px 24px;gap:24px;height:100%"><div style="font-size:32px;font-weight:700">{T("المسابقة","Quiz")}</div><div style="font-size:20px;color:{sub(d)}">{T("الغرفة ٧٤٢٩","Room 7429")}</div>
      <div class="col" style="gap:8px"><div style="font-size:18px;color:{sub(d)}">{T("اسمك","Your name")}</div><div class="row" style="height:64px;padding:0 20px;border-radius:16px;background:{f};font-size:26px;font-weight:600">{T("ريم","Reem")}</div></div>
      <div class="col" style="gap:12px"><div style="font-size:18px;color:{sub(d)}">{T("لونك","Your colour")}</div><div style="display:grid;grid-template-columns:repeat(4,44px);gap:16px;justify-content:space-between">{cols}</div></div>
      <div style="flex:1"></div>{btnP(T("ادخل","Join"), d, True, 64)}</div>'''
def p_wait(d, en):
    T = Tt(en)
    return f'''<div class="col" style="padding:32px 24px;gap:24px;height:100%;align-items:center;justify-content:center;text-align:center"><div class="row" style="height:72px;padding:0 28px;border-radius:20px;background:{PLAYERS[3][2]};color:{INK};font-size:34px;font-weight:700">{T("ريم","Reem")}</div>
      <div style="font-size:30px;font-weight:700">{T("أنت داخل","You're in")}</div><div style="font-size:20px;color:{sub(d)};line-height:1.5">{T("٦ لاعبين. ننتظر أن تبدأ اللعبة على التلفزيون","6 players. Waiting for the game to start on the TV")}</div></div>'''
def p_answers(d, en, picked=None):
    T = Tt(en); q, ans, _ = QE if en else QA; L = LET_EN if en else LET_AR
    f = "rgba(255,255,255,.12)" if d else "rgba(27,29,33,.08)"
    tiles = ""
    for i, a in enumerate(ans):
        on = picked == i
        st = f"background:{AMB};color:{INK}" if on else (f"background:{f};opacity:.45" if picked is not None else f"background:{f}")
        tiles += f'<div class="row" style="height:116px;padding:0 20px;gap:16px;border-radius:20px;{st}"><div class="btn" style="width:56px;height:56px;border-radius:16px;background:rgba(0,0,0,.14);font-size:30px;font-weight:700">{L[i]}</div><div style="font-size:30px;font-weight:700">{a}</div></div>'
    foot = f'<div style="font-size:20px;color:{sub(d)};text-align:center">{T("وصلت إجابتك، ننتظر البقية","Sent, waiting for the others")}</div>' if picked is not None else f'<div style="font-size:20px;color:{sub(d)};text-align:center">{T("السؤال ٣ من ١٠","Question 3 of 10")}</div>'
    return f'<div class="col" style="padding:24px 16px;gap:12px;height:100%"><div style="height:12px;border-radius:6px;background:{"rgba(255,255,255,.16)" if d else "rgba(27,29,33,.12)"}"><div style="width:68%;height:12px;border-radius:6px;background:{AMB}"></div></div><div style="font-size:22px;font-weight:700;line-height:1.35;padding:8px 4px 4px">{q}</div><div class="col" style="gap:12px;flex:1">{tiles}</div>{foot}</div>'
def p_sent(d, en):
    return p_answers(d, en, picked=2)
def p_result(ok):
    def f(d, en):
        T = Tt(en)
        col = OK if ok else BAD
        return f'''<div class="col" style="padding:40px 24px;gap:20px;height:100%;align-items:center;justify-content:center;text-align:center"><div class="btn" style="width:144px;height:144px;border-radius:44px;background:{col};color:{INK}">{ic("check" if ok else "plus", 88, 2.4) if ok else '<div style="font-size:96px;font-weight:700;line-height:1;transform:rotate(45deg)">+</div>'}</div>
          <div style="font-size:36px;font-weight:700">{T("صح!" if ok else "غلط","Right!" if ok else "Wrong")}</div>
          <div style="font-size:56px;font-weight:700;color:{AMB if d else "#8A5B00"};line-height:1">{T("+٨٢٠","+820") if ok else T("+٠","+0")}</div>
          <div style="font-size:20px;color:{sub(d)}">{T("سرعتك ٢٫١ ثانية","Your time: 2.1 s") if ok else T("الصح: ستة","Right answer: Six")}</div>
          <div class="row" style="height:64px;padding:0 24px;border-radius:20px;background:{"rgba(255,255,255,.12)" if d else "rgba(27,29,33,.08)"};gap:12px;font-size:26px;font-weight:700">{T("الترتيب ٢ من ٦","Rank 2 of 6")}<span style="color:{OK};font-size:22px">▲ {T("١","1")}</span></div></div>'''
    return f
def p_final(d, en):
    T = Tt(en)
    return f'''<div class="col" style="padding:40px 24px;gap:20px;height:100%;align-items:center;justify-content:center;text-align:center"><div style="color:{AMB}">{ic("star", 88)}</div><div style="font-size:24px;color:{sub(d)}">{T("ترتيبك","Your rank")}</div><div style="font-size:120px;font-weight:700;line-height:1">{T("٢","2")}</div><div style="font-size:24px">{T("من ٦ لاعبين","of 6 players")}</div>
      <div style="font-size:44px;font-weight:700;color:{AMB if d else "#8A5B00"}">{T("٧١٠٠","7100")}</div><div style="flex:1"></div>{btnP(T("نلعب مرة ثانية","Play again"), d, True, 60)}</div>'''
for n, (fn, t) in {"quiz-07-phone-join": (p_join, "الجوال: الدخول"), "quiz-08-phone-waiting": (p_wait, "الجوال: الانتظار"), "quiz-09-phone-answers": (p_answers, "الجوال: أربع إجابات بنصّها"),
                   "quiz-10-phone-sent": (p_sent, "الجوال: وصلت إجابتك"), "quiz-11-phone-right": (p_result(True), "الجوال: صح مع النقاط والترتيب"),
                   "quiz-12-phone-wrong": (p_result(False), "الجوال: غلط"), "quiz-13-phone-final": (p_final, "الجوال: الترتيب النهائي")}.items():
    S[n] = sheet(fn, t)

# ---------------------------------------------------------------- motion strip: key frames of the four moves (join pop, timer, reveal, rank change)
def strip():
    def frame(t, inner, w=420, h=240):
        return f'<div class="col" style="gap:8px;align-items:center"><div style="width:{w}px;height:{h}px;border-radius:20px;background:rgba(255,255,255,.07);position:relative;overflow:hidden">{inner}</div><div style="font-size:22px;opacity:.75">{t}</div></div>'
    def row(title, note, frames):
        return f'<div class="col" style="gap:12px"><div class="row" style="gap:24px"><div style="font-size:34px;font-weight:700;min-width:240px">{title}</div><div style="font-size:24px;opacity:.7">{note}</div></div><div class="row" style="gap:24px;direction:ltr">{frames}</div></div>'
    pop = "".join(frame(t, f'<div class="abs" style="left:50%;top:50%;transform:translate(-50%,-50%) scale({s});opacity:{o}">{chip(1, 72)}</div>') for t, s, o in (("٠", .6, 0), ("١٢٠ ms", 1.12, 1), ("٢٤٠ ms", 1, 1), ("٣٦٠ ms", 1, 1)))
    tm = "".join(frame(t, f'<div class="abs col" style="left:24px;right:24px;top:70px;gap:16px"><div style="font-size:64px;font-weight:700;text-align:center;color:{"#FF6B6B" if s <= 5 else "#fff"}">{s}</div><div style="height:16px;border-radius:8px;background:rgba(255,255,255,.16)"><div style="width:{int(372*s/20)}px;height:16px;border-radius:8px;background:{"#FF6B6B" if s <= 5 else AMB}"></div></div></div>') for t, s in (("20", 20), ("10", 10), ("5: red + tick", 5), ("0: lock", 0)))
    rv = "".join(frame(t, "".join(f'<div class="abs" style="left:24px;right:24px;top:{20+i*52}px;height:44px;border-radius:12px;background:{(OK if i==2 else "rgba(255,255,255,.12)") if k else "rgba(255,255,255,.12)"};opacity:{(1 if i==2 else .45) if k==2 else 1}"></div>' for i in range(4))) for t, k in (("tiles", 0), ("correct fills 200 ms", 1), ("others fade 200 ms", 2), ("bars grow 400 ms", 2)))
    rk = "".join(frame(t, "".join(f'<div class="abs row" style="left:24px;right:24px;top:{y}px;height:44px;border-radius:12px;background:{"rgba(245,181,71,.3)" if i==0 else "rgba(255,255,255,.12)"}"><div style="margin:0 12px">{chip(pi, 32)}</div></div>' for i, (pi, y) in enumerate(ys))) for t, ys in (("before", [(1, 20), (0, 76), (2, 132)]), ("swap 300 ms", [(0, 48), (1, 48), (2, 132)]) if False else ("swap 300 ms", [(1, 40), (0, 62), (2, 132)]), ("settled", [(0, 20), (1, 76), (2, 132)]), ("+pts counts up", [(0, 20), (1, 76), (2, 132)])))
    return page('<div class="abs" style="left:96px;right:96px;top:40px;font-size:40px;font-weight:700">حركة المسابقة: أربع حركات، كلها أقصر من نصف ثانية إلا العدّاد</div>' + f'<div class="abs col" style="left:96px;top:130px;gap:36px">{row("دخول لاعب","ينبثق من ٠٫٦ إلى ١٫١٢ ثم يستقر، ٣٦٠ مللي ثانية",pop)}{row("العدّاد","شريط يتناقص كل ثانية، أحمر في آخر ٥ مع نقرة، ويقفل عند الصفر",tm)}{row("كشف الإجابة","الصحيحة تمتلئ بالأخضر، الباقي يخفت، ثم تنمو أعمدة من اختار ماذا",rv)}{row("تغيّر الترتيب","صفّان يتبادلان مكانيهما ٣٠٠ مللي ثانية، والنقاط تعدّ صعودًا",rk)}</div>')
S["quiz-14-motion-strip"] = strip().replace("height:1080px;overflow:hidden", "height:1500px;overflow:hidden")

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in sorted(S.items()): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(S), "pages")
