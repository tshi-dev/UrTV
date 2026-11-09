#!/usr/bin/env python3
"""«المسابقة» (live quiz), round 2, MOCKUPS ONLY. Two different directions, each carried from the TV to the phone so the two are one game.
   A «لوحة» (the board): dark ink, bone, one amber. The answers are RAILS (full-width bars with a letter slab); the timer is a line that drains; at the reveal the rails turn into the vote bars.
   B «ورقة» (the paper): warm paper, deep green, saffron. The question is a solid green block with a tick per second; the answers are STAMPS (2x2 ink-bordered tiles with a letter tab); the reveal shows WHO chose what as squares of the players' colours.
   python3 design/43/quiz2.py /tmp/qz2 && node design/43/render.js /tmp/qz2 design/43      (names quiz-*)
   Type: DejaVu Sans stands in for the private Thmanyah (not reachable here); the real face is a little narrower, so every line has slack."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen import ic

INK = "#0D0E10"; BONE = "#F1EDE4"; AMB = "#FFB020"; GREEN = "#2EE6A6"; CORAL = "#FF6B57"
PAPER = "#EFE8DA"; DEEP = "#0E5A45"; SAFF = "#F2A93B"; PINK = "#E4572E"; OKB = "#17A673"
PL = {"سلمان": ("Salman", "#5AA9F0"), "نورة": ("Noura", "#F2705A"), "خالد": ("Khalid", "#7BD88F"), "ريم": ("Reem", "#C28BFF"), "فهد": ("Fahd", "#FFD15C"), "هند": ("Hind", "#4FD1C5")}
S = {}
def N(n, en): return str(n) if en else "".join("٠١٢٣٤٥٦٧٨٩"[int(c)] if c.isdigit() else c for c in str(n))
def nm(a, en): return PL[a][0] if en else a
def col(a): return PL[a][1]
QA = ("كم عدد أضلاع المسدّس؟", ["أربعة", "خمسة", "ستة", "ثمانية"], 2); QE = ("How many sides does a hexagon have?", ["Four", "Five", "Six", "Eight"], 2)
CHOSE = [["فهد"], ["سلمان"], ["نورة", "ريم", "خالد"], ["هند"]]      # who chose A B C D
LA, LE = "أبجد", "ABCD"

def page(body, bg, text, en=False, w=1920, h=1080):
    return f'''<!doctype html><html lang="{"en" if en else "ar"}" dir="{"ltr" if en else "rtl"}"><head><meta charset="utf-8"><style>
*{{box-sizing:border-box;margin:0;padding:0}}html,body{{width:{w}px;height:{h}px;overflow:hidden}}
body{{font-family:'DejaVu Sans',sans-serif;direction:{"ltr" if en else "rtl"};position:relative;background:{bg};color:{text}}}
.abs{{position:absolute}}.row{{display:flex;align-items:center}}.col{{display:flex;flex-direction:column}}.btn{{display:flex;align-items:center;justify-content:center}}
.tn{{font-variant-numeric:tabular-nums;direction:ltr}}
</style></head><body>{body}</body></html>'''
def hint(items, c, kc):
    p = "".join(f'<div class="row" style="gap:12px"><div class="btn" style="min-width:64px;height:48px;padding:0 16px;border-radius:8px;background:{kc};font-size:30px;font-weight:700">{k}</div><div style="font-size:32px;opacity:.85">{w}</div></div>' for k, w in items)
    return f'<div class="abs row" style="left:96px;right:96px;bottom:54px;gap:48px;height:48px;color:{c}">{p}</div>'
def sides(en): return ("left", "right") if en else ("right", "left")        # (start, end)
def Q(en): return QE if en else QA
def T(en): return (lambda a, e: e) if en else (lambda a, e: a)
def dots(answered, size, gap, tone_off, en):
    order = ["سلمان", "نورة", "خالد", "ريم", "فهد", "هند"]
    return "".join(f'<div style="width:{size}px;height:{size}px;background:{col(p) if i < answered else "transparent"};{"" if i < answered else "box-shadow:inset 0 0 0 4px "+tone_off}"></div>' for i, p in enumerate(order))

# =============================================================== DIRECTION A «لوحة»
def A_tv(en, reveal):
    st, ed = sides(en); t = T(en); q, ans, right = Q(en); L = LE if en else LA
    rails = ""
    for i, a in enumerate(ans):
        v = len(CHOSE[i]); ok = i == right
        if not reveal: bg, tx, slab, sl = "#1B1D21", BONE, BONE, INK
        elif ok: bg, tx, slab, sl = GREEN, INK, INK, GREEN
        else: bg, tx, slab, sl = "#141518", "rgba(241,237,228,.5)", "#34363B", "rgba(241,237,228,.8)"
        extra = ""
        if reveal:
            extra = f'<div class="abs" style="{st}:112px;bottom:0;height:10px;width:calc((100% - 112px) * {v}/6);background:{INK if ok else "rgba(241,237,228,.55)"}"></div>' \
                    f'<div class="tn" style="font-size:72px;font-weight:700;margin-{st}:auto;padding-{ed}:0;color:{INK if ok else "rgba(241,237,228,.7)"}">{N(v, en)}</div>'
            extra = extra.replace(f"margin-{st}:auto", "margin-inline-start:auto")
        rails += f'<div class="row" style="position:relative;height:104px;margin-bottom:12px;background:{bg};color:{tx}"><div class="btn" style="width:104px;height:104px;background:{slab};color:{sl};font-size:72px;font-weight:700">{L[i]}</div><div style="font-size:64px;font-weight:700;padding:0 32px;white-space:nowrap">{a}</div>{extra}{ic("check", 56, 3) if (reveal and ok) else ""}</div>'
    top_end = (f'<div class="abs tn" style="{ed}:96px;top:24px;font-size:176px;font-weight:700;color:{AMB};line-height:1">{N(14, en)}</div>' if not reveal else
               f'<div class="abs col" style="{ed}:96px;top:54px;align-items:flex-{"end" if en else "start"};gap:4px"><div style="font-size:36px;opacity:.7">{t("الأسرع","Fastest")}</div><div style="font-size:56px;font-weight:700">{nm("نورة", en)} <span class="tn" style="color:{AMB}">{N("2.1" if en else "٢٫١", en)}</span></div></div>')
    drain = f'<div class="abs" style="{st}:0;top:0;width:{"100" if reveal else "62"}%;height:16px;background:{AMB if not reveal else "#2A2C31"}"></div><div class="abs" style="left:0;right:0;top:0;height:16px;background:#1B1D21;z-index:-1"></div>'
    return page(drain +
      f'<div class="abs" style="{st}:96px;top:54px;font-size:40px;opacity:.75">{t("السؤال ٣ من ١٠","Question 3 of 10")}</div>' + top_end +
      (f'<div class="abs row" style="{ed}:96px;top:200px;gap:8px">{dots(4, 32, 8, "#4A4C52", en)}</div>' if not reveal else "") +
      f'<div class="abs col" style="{st}:96px;top:110px;height:340px;width:{1560 if en else 1400}px;justify-content:flex-end;font-size:{116 if en else 136}px;font-weight:700;line-height:1.1">{q}</div>'
      f'<div class="abs" style="left:96px;right:96px;top:478px">{rails}</div>' +
      (hint([("OK", "Next")] if en else [("اختيار", "التالي")], BONE, "#2A2C31") if reveal else ""), INK, BONE, en)

def A_board(en):
    st, ed = sides(en); t = T(en)
    rows = [("نورة", 2480, 820, 1), ("ريم", 2310, 820, 1), ("سلمان", 2090, 650, -1), ("خالد", 1760, 510, 0), ("فهد", 1500, 0, 0)]
    out = ""
    for r, (p, pts, d, mv) in enumerate(rows, 1):
        num = f'<div class="tn" style="width:220px;font-size:168px;font-weight:700;line-height:1;{"color:"+AMB if r == 1 else "color:transparent;-webkit-text-stroke:3px "+BONE}">{N(r, en)}</div>'
        arrow = {1: f'<span style="color:{GREEN}">▲</span>', -1: f'<span style="color:{CORAL}">▼</span>', 0: ""}[mv]
        out += f'<div class="row" style="height:148px;border-bottom:3px solid #26282D;gap:32px">{num}<div style="width:16px;height:96px;background:{col(p)}"></div><div style="font-size:72px;font-weight:700;flex:1;white-space:nowrap">{nm(p, en)}</div><div style="font-size:44px;width:110px;text-align:center">{arrow}</div><div class="tn" style="font-size:44px;opacity:.65;width:150px;text-align:center">+{N(d, en)}</div><div class="tn" style="font-size:104px;font-weight:700;width:300px;text-align:{"right" if en else "left"}">{N(pts, en)}</div></div>'
    return page(f'<div class="abs" style="{st}:96px;top:54px;font-size:56px;font-weight:700">{t("الترتيب","Leaderboard")}</div><div class="abs" style="{ed}:96px;top:62px;font-size:40px;opacity:.7">{t("بعد السؤال ٣ من ١٠","After question 3 of 10")}</div>'
      f'<div class="abs" style="left:96px;right:96px;top:150px;border-top:3px solid #26282D">{out}</div>' + hint([("OK", "Next question")] if en else [("اختيار", "السؤال التالي")], BONE, "#2A2C31"), INK, BONE, en)

def A_podium(en):
    st, ed = sides(en); t = T(en)
    def blk(p, rank, h, pts, win):
        bg, c = (AMB, INK) if win else (BONE, INK)
        return f'<div class="col" style="width:360px;justify-content:flex-end"><div style="font-size:64px;font-weight:700;margin-bottom:8px;white-space:nowrap">{nm(p, en)}</div><div class="tn" style="font-size:48px;opacity:.75;margin-bottom:16px">{N(pts, en)}</div><div style="width:360px;height:{h}px;background:{bg};color:{c};position:relative"><div class="tn" style="position:absolute;{"left" if True else ""}:28px;top:-10px;font-size:{min(340, int(h*.8))}px;font-weight:700;line-height:1">{N(rank, en)}</div></div></div>'
    cols = f'<div class="abs row" style="{ed}:96px;bottom:0;align-items:flex-end;gap:0;direction:ltr;height:900px">{blk("ريم", 2, 400, 7100, False)}{blk("نورة", 1, 560, 7860, True)}{blk("سلمان", 3, 280, 6420, False)}</div>'
    return page(f'<div class="abs col" style="{st}:96px;top:54px;gap:16px;width:640px"><div style="font-size:48px;opacity:.7">{t("انتهت المسابقة","Quiz over")}</div><div style="font-size:48px">{t("الفائزة","Winner")}</div><div style="font-size:176px;font-weight:700;line-height:1.05;color:{AMB}">{nm("نورة", en)}</div><div class="tn" style="font-size:112px;font-weight:700;line-height:1">{N(7860, en)}</div></div>'
      f'<div class="abs btn" style="{st}:96px;bottom:150px;width:560px;height:104px;background:{BONE};color:{INK};font-size:44px;font-weight:700;box-shadow:0 0 0 6px rgba(241,237,228,.35)">{t("نلعب مرة ثانية","Play again")}</div>' + cols +
      hint([("OK", "Play again"), ("Back", "Games")] if en else [("اختيار", "نلعب مرة ثانية"), ("رجوع", "الألعاب")], BONE, "#2A2C31").replace("bottom:54px", "bottom:54px;display:none"), INK, BONE, en)

# =============================================================== DIRECTION B «ورقة»
def B_tv(en, reveal):
    st, ed = sides(en); t = T(en); q, ans, right = Q(en); L = LE if en else LA
    ticks = "".join(f'<div style="flex:1;height:28px;background:{(SAFF if i < (20 if reveal else 14) else "#0A4A39") if not reveal else "#0A4A39"}"></div>' for i in range(20))
    stamp = f'<div class="abs btn" style="{ed}:96px;top:54px;width:232px;height:200px;background:{SAFF};color:{INK};font-size:{"128" if not reveal else "112"}px;font-weight:700"><span class="tn">{N(14, en) if not reveal else ""}</span>{ic("check", 120, 3) if reveal else ""}</div>'
    tiles = ""
    for i, a in enumerate(ans):
        ok = i == right
        if not reveal: bg, bd, tx = "#FBF8F1", f"6px solid {INK}", INK
        elif ok: bg, bd, tx = OKB, f"6px solid {INK}", "#fff"
        else: bg, bd, tx = "transparent", "4px dashed rgba(22,20,15,.35)", "rgba(22,20,15,.45)"
        sq = ""
        if reveal:
            sq = '<div class="row" style="margin-inline-start:auto;gap:8px">' + "".join(f'<div style="width:44px;height:44px;background:{col(p)};box-shadow:0 0 0 3px {INK}"></div>' for p in CHOSE[i]) + '</div>'
        tiles += f'<div class="row" style="position:relative;height:216px;background:{bg};border:{bd};color:{tx};padding-inline-start:136px;padding-inline-end:28px"><div class="btn abs" style="{st}:-6px;top:-6px;width:96px;height:96px;background:{INK};color:{PAPER if not (reveal and not ok) else "rgba(239,232,218,.6)"};font-size:64px;font-weight:700">{L[i]}</div><div style="font-size:68px;font-weight:700;white-space:nowrap">{a}</div>{sq}</div>'
    return page(f'<div class="abs" style="left:0;right:0;top:0;height:528px;background:{DEEP}"></div>'
      f'<div class="abs row" style="{st}:96px;top:54px;gap:32px;color:{SAFF}"><div style="font-size:40px;font-weight:700">{t("السؤال ٣ من ١٠","Question 3 of 10")}</div>' + ("" if reveal else f'<div class="row" style="gap:8px">{dots(4, 28, 8, "rgba(239,232,218,.5)", en)}</div>') + '</div>' + stamp +
      f'<div class="abs" style="{st}:96px;top:128px;width:1330px;font-size:104px;font-weight:700;line-height:1.14;color:#fff">{q}</div>'
      f'<div class="abs row" style="left:96px;right:96px;top:452px;gap:12px;direction:ltr">{ticks}</div>' +
      (f'<div class="abs" style="{st}:96px;top:390px;font-size:40px;color:{SAFF};font-weight:700">{t("الأسرع: نورة ٢٫١ ثانية","Fastest: Noura 2.1 s")}</div>' if reveal else "") +
      f'<div class="abs" style="left:96px;right:96px;top:568px;display:grid;grid-template-columns:1fr 1fr;gap:24px 36px">{tiles}</div>', PAPER, "#16140F", en)

def B_board(en):
    st, ed = sides(en); t = T(en)
    rows = [("نورة", 2480, 820, 1), ("ريم", 2310, 820, 1), ("سلمان", 2090, 650, -1), ("خالد", 1760, 510, 0), ("فهد", 1500, 0, 0)]
    out = ""
    for r, (p, pts, d, mv) in enumerate(rows, 1):
        arrow = {1: f'<span style="color:{OKB}">▲</span>', -1: f'<span style="color:{PINK}">▼</span>', 0: ""}[mv]
        out += f'<div class="row" style="height:128px;border:6px solid {INK};background:#FBF8F1;gap:32px;padding-inline-end:32px"><div class="btn tn" style="width:128px;height:116px;margin:-6px;background:{SAFF if r == 1 else INK};color:{INK if r == 1 else PAPER};font-size:88px;font-weight:700">{N(r, en)}</div><div style="width:20px;height:64px;background:{col(p)};box-shadow:0 0 0 3px {INK};margin-inline-start:16px"></div><div style="font-size:64px;font-weight:700;flex:1;white-space:nowrap">{nm(p, en)}</div><div style="font-size:44px;width:70px;text-align:center">{arrow}</div><div class="tn" style="font-size:44px;opacity:.6;width:130px;text-align:center">+{N(d, en)}</div><div class="tn" style="font-size:88px;font-weight:700;width:280px;text-align:{"right" if en else "left"}">{N(pts, en)}</div></div>'
    return page(f'<div class="abs" style="left:0;right:0;top:0;height:200px;background:{DEEP}"></div><div class="abs" style="{st}:96px;top:54px;font-size:96px;font-weight:700;color:#fff;line-height:1.1">{t("الترتيب","Leaderboard")}</div><div class="abs" style="{ed}:96px;top:84px;font-size:40px;color:{SAFF};font-weight:700">{t("بعد السؤال ٣ من ١٠","After question 3 of 10")}</div>'
      f'<div class="abs col" style="left:96px;right:96px;top:236px;gap:14px">{out}</div>', PAPER, "#16140F", en)

def B_podium(en):
    st, ed = sides(en); t = T(en)
    def blk(p, rank, h, pts, c):
        return f'<div class="col" style="width:576px;justify-content:flex-end;align-items:stretch"><div class="row" style="height:96px;background:{INK};color:{PAPER};padding:0 32px;justify-content:space-between"><span style="font-size:56px;font-weight:700">{nm(p, en)}</span><span class="tn" style="font-size:48px;color:{SAFF}">{N(pts, en)}</span></div><div class="btn tn" style="height:{h}px;background:{c};border:6px solid {INK};border-top:0;font-size:{min(340, int(h*.75))}px;font-weight:700;color:{INK};line-height:1">{N(rank, en)}</div></div>'
    cols = f'<div class="abs row" style="left:96px;right:96px;bottom:0;align-items:flex-end;direction:ltr">{blk("ريم", 2, 340, 7100, OKB)}{blk("نورة", 1, 520, 7860, SAFF)}{blk("سلمان", 3, 240, 6420, "#F08A6E")}</div>'
    return page(f'<div class="abs" style="left:0;right:0;top:0;height:200px;background:{DEEP}"></div><div class="abs" style="{st}:96px;top:54px;font-size:96px;font-weight:700;color:#fff;line-height:1.1">{t("انتهت المسابقة","Quiz over")}</div><div class="abs" style="{ed}:96px;top:84px;font-size:40px;color:{SAFF};font-weight:700">{t("الفائزة: نورة","Winner: Noura")}</div>'
      f'<div class="abs btn" style="{st}:96px;top:240px;width:560px;height:104px;background:{INK};color:{PAPER};font-size:44px;font-weight:700;box-shadow:0 0 0 6px rgba(22,20,15,.25)">{t("نلعب مرة ثانية","Play again")}</div>' + cols, PAPER, "#16140F", en)

for en, sfx in ((False, ""), (True, "-en")):
    S[f"quiz-A1-tv-question{sfx}"] = A_tv(en, False); S[f"quiz-A2-tv-reveal{sfx}"] = A_tv(en, True)
    S[f"quiz-A3-tv-leaderboard{sfx}"] = A_board(en); S[f"quiz-A4-tv-podium{sfx}"] = A_podium(en)
    S[f"quiz-B1-tv-question{sfx}"] = B_tv(en, False); S[f"quiz-B2-tv-reveal{sfx}"] = B_tv(en, True)
    S[f"quiz-B3-tv-leaderboard{sfx}"] = B_board(en); S[f"quiz-B4-tv-podium{sfx}"] = B_podium(en)

# =============================================================== PHONE (360 px)
def ph(inner, bg, tx, en, h=720):
    return f'<div style="width:360px;height:{h}px;overflow:hidden;position:relative;background:{bg};color:{tx};direction:{"ltr" if en else "rtl"};box-shadow:0 0 0 2px rgba(128,128,128,.35),0 24px 60px rgba(0,0,0,.45);font-family:\'DejaVu Sans\',sans-serif;border-radius:28px">{inner}</div>'
def sheet(title, cells, note):
    cs = "".join(f'<div class="col" style="gap:12px;align-items:center">{c}<div style="font-size:24px;opacity:.8;color:#fff">{lab}</div></div>' for c, lab in cells)
    return page(f'<div class="abs" style="left:0;right:0;top:36px;text-align:center;font-size:36px;font-weight:700">{title}</div><div class="abs row" style="left:0;right:0;top:108px;justify-content:center;gap:40px;align-items:flex-start;direction:ltr">{cs}</div><div class="abs" style="left:0;right:0;bottom:24px;text-align:center;font-size:22px;opacity:.6">{note}</div>', "#24262B", "#fff")
NOTE = "DejaVu Sans stands in for the private Thmanyah (not reachable here); the phone page's real face is a little narrower"

def A_ph(state, dark, en):
    t = T(en); q, ans, right = Q(en); L = LE if en else LA
    bg, tx, trk, slab, sl = (INK, BONE, "#1B1D21", BONE, INK) if dark else (BONE, INK, "#E2DCCE", INK, BONE)
    amb = AMB; tim = AMB if dark else INK
    if state in ("answer", "locked"):
        picked = 2 if state == "locked" else None
        rails = ""
        for i, a in enumerate(ans):
            if picked is None: rails += f'<div class="row" style="height:108px;background:{trk};margin-bottom:8px"><div class="btn" style="width:108px;height:108px;background:{slab};color:{sl};font-size:56px;font-weight:700">{L[i]}</div><div style="font-size:30px;font-weight:700;padding:0 20px">{a}</div></div>'
            elif i == picked: rails += f'<div class="row" style="height:108px;background:{amb};color:{INK};margin-bottom:8px"><div class="btn" style="width:108px;height:108px;background:{INK};color:{amb};font-size:56px;font-weight:700">{L[i]}</div><div style="font-size:30px;font-weight:700;padding:0 20px">{a}</div></div>'
            else: rails += f'<div style="height:8px;background:{trk};margin-bottom:8px"></div>'
        status = '' if picked is None else f'<div class="col" style="gap:8px;padding:24px 20px"><div style="font-size:44px;font-weight:700">{t("وصلت","Sent")}</div><div style="font-size:22px;opacity:.7">{t("ننتظر ٢ من اللاعبين","Waiting for 2 players")}</div></div>'
        head = f'<div class="abs" style="{"left" if en else "right"}:0;top:0;height:10px;width:{"62" if picked is None else "62"}%;background:{amb}"></div><div class="abs" style="left:0;right:0;top:0;height:10px;background:{trk};z-index:-1"></div>'
        tmr = f'<div class="abs tn" style="{"right" if en else "left"}:20px;top:22px;font-size:64px;font-weight:700;color:{tim};line-height:1">{N(14, en)}</div>' if picked is None else f'<div class="abs tn" style="{"right" if en else "left"}:20px;top:22px;font-size:64px;font-weight:700;opacity:.35;line-height:1">{N(14, en)}</div>'
        inner = head + tmr + f'<div class="abs" style="{"left" if en else "right"}:20px;top:28px;font-size:18px;opacity:.7">{t("السؤال ٣ / ١٠","Question 3 / 10")}</div><div class="abs" style="left:20px;right:20px;top:100px;font-size:28px;font-weight:700;line-height:1.3">{q}</div><div class="abs" style="left:0;right:0;top:{232 if picked is None else 232}px">{rails}{status}</div>'
        return ph(inner, bg, tx, en)
    if state == "right":
        inner = f'<div class="col" style="height:100%;padding:36px 24px;gap:8px"><div style="font-size:30px;font-weight:700">{t("صح","Right")}</div><div class="tn" style="font-size:92px;font-weight:700;line-height:1">+{N(820, en)}</div><div style="font-size:20px;opacity:.75">{t("سرعتك ٢٫١ ثانية","Your time: 2.1 s")}</div><div style="flex:1"></div><div style="font-size:22px;opacity:.75">{t("ترتيبك","Your rank")}</div><div class="row" style="gap:16px"><div class="tn" style="font-size:176px;font-weight:700;line-height:.95">{N(2, en)}</div><div style="font-size:24px">{t("من ٦","of 6")}<br><span style="font-weight:700">▲ {N(1, en)}</span></div></div></div>'
        return ph(inner, GREEN, INK, en)
    inner = f'<div class="abs" style="left:0;right:0;top:0;height:10px;background:{CORAL}"></div><div class="col" style="height:100%;padding:36px 24px;gap:8px"><div style="font-size:30px;font-weight:700;color:{CORAL}">{t("غلط","Wrong")}</div><div class="tn" style="font-size:92px;font-weight:700;line-height:1;opacity:.4">+{N(0, en)}</div><div style="font-size:22px;opacity:.8;line-height:1.4">{t("الصح: ستة","Right answer: Six")}</div><div style="flex:1"></div><div style="font-size:22px;opacity:.75">{t("ترتيبك","Your rank")}</div><div class="row" style="gap:16px"><div class="tn" style="font-size:176px;font-weight:700;line-height:.95">{N(4, en)}</div><div style="font-size:24px">{t("من ٦","of 6")}<br><span style="font-weight:700;color:{CORAL}">▼ {N(1, en)}</span></div></div></div>'
    return ph(inner, bg, tx, en)

def B_ph(state, light, en):
    t = T(en); q, ans, right = Q(en); L = LE if en else LA
    bg, tx, tile, bd = (PAPER, "#16140F", "#FBF8F1", INK) if light else ("#14130F", PAPER, "#1E1C16", PAPER)
    if state in ("answer", "locked"):
        picked = 2 if state == "locked" else None
        tiles = ""
        for i, a in enumerate(ans):
            if picked is None: b, c, brd = tile, tx, f"4px solid {bd}"
            elif i == picked: b, c, brd = (INK if light else PAPER), (PAPER if light else INK), f"4px solid {bd}"
            else: b, c, brd = "transparent", ("rgba(22,20,15,.4)" if light else "rgba(239,232,218,.4)"), f"3px dashed {'rgba(22,20,15,.35)' if light else 'rgba(239,232,218,.35)'}"
            lt_bg, lt_c = (INK, PAPER) if (light or i != picked) else (INK, PAPER)
            tiles += f'<div class="col" style="position:relative;height:214px;background:{b};color:{c};border:{brd};justify-content:flex-end;padding:16px"><div class="btn abs" style="{"left" if en else "right"}:-4px;top:-4px;width:64px;height:64px;background:{INK if light else PAPER};color:{PAPER if light else INK};font-size:40px;font-weight:700">{L[i]}</div><div style="font-size:32px;font-weight:700;line-height:1.15">{a}</div></div>'
        ticks = "".join(f'<div style="flex:1;height:12px;background:{SAFF if i < 14 else "#0A4A39"}"></div>' for i in range(20))
        stamp = f'<div class="abs btn" style="{"right" if en else "left"}:20px;top:20px;width:84px;height:72px;background:{SAFF};color:{INK};font-size:48px;font-weight:700"><span class="tn">{N(14, en)}</span></div>'
        stat = f'<div class="abs btn" style="left:20px;right:20px;top:190px;height:0"></div>'
        inner = f'<div class="abs" style="left:0;right:0;top:0;height:228px;background:{DEEP}"></div><div class="abs" style="{"left" if en else "right"}:20px;top:24px;font-size:18px;color:{SAFF};font-weight:700">{t("السؤال ٣ / ١٠","Question 3 / 10")}</div>' + stamp + f'<div class="abs" style="left:20px;right:20px;top:104px;font-size:25px;font-weight:700;line-height:1.3;color:#fff">{q}</div><div class="abs row" style="left:20px;right:20px;top:196px;gap:4px;direction:ltr">{ticks if picked is None else ""}</div>' \
              f'<div class="abs" style="left:16px;right:16px;top:252px;display:grid;grid-template-columns:1fr 1fr;gap:16px">{tiles}</div>' + (f'<div class="abs" style="left:20px;right:20px;top:186px;font-size:20px;font-weight:700;color:{SAFF}">{t("وصلت إجابتك، ننتظر ٢","Sent, waiting for 2")}</div>' if picked is not None else "")
        return ph(inner, bg, tx, en)
    ok = state == "right"; c = SAFF if ok else "#F08A6E"
    inner = f'<div class="col" style="height:100%;padding:36px 24px;gap:8px;color:{INK}"><div class="btn" style="width:96px;height:96px;background:{INK};color:{c}">{ic("check", 64, 3) if ok else "<div style=font-size:72px;font-weight:700;transform:rotate(45deg)>+</div>"}</div><div style="font-size:34px;font-weight:700;margin-top:12px">{t("صح" if ok else "غلط", "Right" if ok else "Wrong")}</div><div class="tn" style="font-size:112px;font-weight:700;line-height:1">+{N(820 if ok else 0, en)}</div><div style="font-size:20px;opacity:.8">{t("سرعتك ٢٫١ ثانية","Your time: 2.1 s") if ok else t("الصح: ستة","Right answer: Six")}</div><div style="flex:1"></div><div class="row" style="height:120px;background:{INK};color:{PAPER};padding:0 20px;gap:16px"><div class="tn" style="font-size:96px;font-weight:700;line-height:1;color:{c}">{N(2 if ok else 4, en)}</div><div style="font-size:24px">{t("ترتيبك من ٦","Your rank of 6")}<br><span style="color:{c};font-weight:700">{"▲" if ok else "▼"} {N(1, en)}</span></div></div></div>'
    return ph(inner, c, INK, en)

lab = lambda s, d: s
S["quiz-A5-phone-ar"] = sheet("A «لوحة»: الجوال، عربي: الإجابة، الإرسال، صح، غلط", [(A_ph("answer", True, False), "الإجابة"), (A_ph("locked", True, False), "وصلت"), (A_ph("right", True, False), "صح"), (A_ph("wrong", True, False), "غلط")], NOTE)
S["quiz-A6-phone-modes"] = sheet("A «لوحة»: الجوال، الفاتح، والإنجليزي", [(A_ph("answer", False, False), "عربي فاتح"), (A_ph("answer", True, True), "English dark"), (A_ph("answer", False, True), "English light"), (A_ph("right", False, True), "English right")], NOTE)
S["quiz-B5-phone-ar"] = sheet("B «ورقة»: الجوال، عربي: الإجابة، الإرسال، صح، غلط", [(B_ph("answer", True, False), "الإجابة"), (B_ph("locked", True, False), "وصلت"), (B_ph("right", True, False), "صح"), (B_ph("wrong", True, False), "غلط")], NOTE)
S["quiz-B6-phone-modes"] = sheet("B «ورقة»: الجوال، الداكن، والإنجليزي", [(B_ph("answer", False, False), "عربي داكن"), (B_ph("answer", True, True), "English light"), (B_ph("answer", False, True), "English dark"), (B_ph("locked", False, True), "English dark, sent")], NOTE)

# =============================================================== intent sheet
def intent():
    def sw(c, n, light=False): return f'<div class="col" style="gap:6px"><div style="width:96px;height:64px;background:{c};{"box-shadow:inset 0 0 0 2px #888" if light else ""}"></div><div style="font-size:20px;opacity:.85">{n}</div></div>'
    def box(title, bg, tx, lines, swatches, scale, sub):
        ls = "".join(f'<div class="row" style="gap:20px;align-items:flex-start"><div style="font-size:26px;font-weight:700;width:150px;opacity:.7;flex:none">{k}</div><div style="font-size:30px;line-height:1.4">{v}</div></div>' for k, v in lines)
        return f'<div class="col" style="width:880px;padding:40px 44px;gap:24px;background:{bg};color:{tx};height:920px"><div style="font-size:64px;font-weight:700;line-height:1">{title}</div><div style="font-size:30px;opacity:.75;margin-top:-12px">{sub}</div>{ls}<div class="row" style="gap:16px;margin-top:8px">{swatches}</div><div style="font-size:24px;opacity:.85;line-height:1.5">{scale}</div></div>'
    a = box("A «لوحة»", INK, BONE, [("الإحساس", "غرفة بثّ بالليل: السؤال هو الضوء الوحيد."), ("البصمة", "الإجابات قضبان بعرض الشاشة، وعند الكشف تصير هي أعمدة التصويت."), ("اللوحة", "حبر وعظم وكهرماني واحد، والأخضر للصحيح فقط."), ("الخط", "أرقام ١٧٦ / سؤال ١١٢ / إجابة ٦٤ / نص ٤٠."), ("الحركة", "خط الوقت يتناقص ثم يقفل، والقضبان تتحول إلى تصويت، والصفوف تتبادل مكانها.")],
            sw(INK, "حبر", False) + sw(BONE, "عظم") + sw(AMB, "كهرماني") + sw(GREEN, "صحيح") + sw(CORAL, "غلط"), "الأرقام تصميم: رقم الترتيب ١٧٦ مفرّغ، والنتيجة ١٠٤ ثقيلة، والسؤال يملأ ثلث الشاشة.", "للتلفزيون والجوال: نفس القضبان")
    b = box("B «ورقة»", PAPER, "#16140F", [("الإحساس", "حصة اختبار على طاولة ورقية: واضح، سريع، عادل."), ("البصمة", "كتلة خضراء فيها السؤال وعدّاد من ٢٠ علامة، والإجابات أختام بحدّ حبر وألسنة حروف."), ("اللوحة", "ورق وأخضر عميق وزعفران، والحبر للحدود."), ("الخط", "رقم ١٢٨ / سؤال ١٠٤ / إجابة ٦٨ / نص ٤٠."), ("الحركة", "علامة تسقط كل ثانية، والخاطئة تنكمش وتتقطع حدودها، ومربعات ألوان اللاعبين تُظهر من اختار ماذا.")],
            sw(PAPER, "ورق", True) + sw(DEEP, "أخضر") + sw(SAFF, "زعفران") + sw(PINK, "مرجاني") + sw("#16140F", "حبر"), "الأرقام تصميم: خانة الترتيب مربع حبر بحجم الصف، والمنصّة أعمدة ملوّنة بعرض الشاشة.", "للتلفزيون والجوال: نفس الأختام")
    return page(f'<div class="abs" style="left:0;right:0;top:0;height:120px;display:flex;align-items:center;justify-content:center;font-size:44px;font-weight:700;color:#fff">المسابقة: اتجاهان للتصميم</div><div class="abs row" style="left:0;right:0;top:120px;gap:0;direction:rtl">{a}{b}</div><div class="abs" style="left:0;right:0;bottom:12px;text-align:center;font-size:22px;color:#fff;opacity:.6">{NOTE}</div>', "#24262B", "#fff")
S["quiz-00-design-intent"] = intent()

# =============================================================== motion sheet (key frames; A left, B right)
def motion():
    FW, FH = 200, 120
    def fr(t, inner, bg): return f'<div class="col" style="gap:6px;align-items:center"><div style="width:{FW}px;height:{FH}px;position:relative;overflow:hidden;background:{bg}">{inner}</div><div style="font-size:18px;color:#fff;opacity:.75">{t}</div></div>'
    def A_t(p): return f'<div class="abs" style="right:0;top:0;height:8px;width:{p}%;background:{AMB if p > 25 else CORAL}"></div><div class="abs tn" style="left:12px;top:20px;font-size:48px;font-weight:700;color:{AMB if p > 25 else CORAL}">{N(round(p*.2), False)}</div>'
    def B_t(n): return '<div class="abs row" style="left:6px;right:6px;top:50px;gap:2px;direction:ltr">' + "".join(f'<div style="flex:1;height:16px;background:{SAFF if i < n else "#0A4A39"}"></div>' for i in range(20)) + f'</div><div class="abs tn" style="left:12px;top:8px;font-size:30px;font-weight:700;color:{SAFF}">{N(n, False)}</div>'
    def A_r(k): return "".join(f'<div class="abs" style="right:0;top:{8+i*28}px;height:22px;width:{(100 if (k == 0 or (i == 2 and k >= 1)) else (100 if k == 1 else [40, 30, 0, 24][i] + 20))}%;background:{(GREEN if (i == 2 and k >= 1) else "#1B1D21") if k else "#1B1D21"};opacity:{1 if (i == 2 or k < 2) else .6}"></div>' for i in range(4))
    def B_r(k): return "".join(f'<div class="abs" style="left:{8 + (i % 2) * 96}px;top:{8 + (i // 2) * 52}px;width:90px;height:46px;background:{(OKB if i == 2 else "transparent") if k else "#FBF8F1"};border:{"3px solid "+INK if (k == 0 or i == 2) else "2px dashed rgba(22,20,15,.35)"}"></div>' + (f'<div class="abs" style="left:{14 + (i % 2) * 96}px;top:{36 + (i // 2) * 52}px;width:{[12,12,12,12][i]}px;height:12px;background:{col(CHOSE[i][0])}"></div>' if k == 2 else "") for i in range(4))
    def A_k(k): return "".join(f'<div class="abs" style="right:0;left:0;top:{y}px;height:30px;background:{"#2A2300" if i == 0 else "#1B1D21"};border-bottom:2px solid #26282D"></div><div class="abs" style="right:10px;top:{y+6}px;width:10px;height:18px;background:{col(p)}"></div>' for i, (p, y) in enumerate(k))
    def B_k(k): return "".join(f'<div class="abs" style="left:8px;right:8px;top:{y}px;height:32px;background:#FBF8F1;border:3px solid {INK}"></div><div class="abs" style="left:16px;top:{y+9}px;width:8px;height:14px;background:{col(p)}"></div>' for p, y in k)
    seqs = {"timer": ([A_t(p) for p in (100, 55, 25, 0)], [B_t(n) for n in (20, 11, 5, 0)], ("٢٠ث", "١١ث", "٥ث: أحمر", "٠ يقفل")),
            "lock": (None, None, ("لمس", "الإجابة تمتلئ", "الباقي ينكمش", "وصلت")),
            "reveal": ([A_r(0), A_r(1), A_r(2), A_r(2)], [B_r(0), B_r(1), B_r(2), B_r(2)], ("الإجابات", "الصحيح يمتلئ ٢٠٠م", "الباقي يخفت", "الأعمدة/المربعات ٤٠٠م")),
            "rank": ([A_k([("ريم", 6), ("نورة", 38), ("سلمان", 70)]), A_k([("ريم", 14), ("نورة", 30), ("سلمان", 70)]), A_k([("نورة", 6), ("ريم", 38), ("سلمان", 70)]), A_k([("نورة", 6), ("ريم", 38), ("سلمان", 70)])],
                     [B_k([("ريم", 6), ("نورة", 44), ("سلمان", 82)]), B_k([("ريم", 14), ("نورة", 36), ("سلمان", 82)]), B_k([("نورة", 6), ("ريم", 44), ("سلمان", 82)]), B_k([("نورة", 6), ("ريم", 44), ("سلمان", 82)])], ("قبل", "تبادل ٣٠٠م", "استقرّ", "النقاط تعدّ"))}
    # lock row: phone-side, built by hand
    def A_l(k):
        rows = ""
        for i in range(4):
            on = i == 2
            h = 26 if (k == 0 or on) else 4
            rows += f'<div class="abs" style="left:8px;right:8px;top:{10+i*28 if k < 2 else (10+i*28 if on else 10+i*10)}px;height:{h}px;background:{AMB if (on and k >= 1) else "#1B1D21"}"></div>'
        return rows
    def B_l(k): return "".join(f'<div class="abs" style="left:{8 + (i % 2) * 96}px;top:{8 + (i // 2) * 52}px;width:90px;height:46px;background:{INK if (i == 2 and k >= 1) else ("transparent" if (k >= 2 and i != 2) else "#FBF8F1")};border:{"3px solid "+INK if (i == 2 or k < 2) else "2px dashed rgba(22,20,15,.35)"}"></div>' for i in range(4))
    seqs["lock"] = ([A_l(0), A_l(1), A_l(2), A_l(2)], [B_l(0), B_l(1), B_l(2), B_l(2)], seqs["lock"][2])
    titles = {"timer": "العدّاد", "lock": "القفل عند اللمس", "reveal": "كشف الإجابة", "rank": "تغيّر الترتيب"}
    rows = ""
    for key in ("timer", "lock", "reveal", "rank"):
        a, b, labs = seqs[key]
        fa = "".join(fr(labs[i], a[i], INK) for i in range(4)); fb = "".join(fr(labs[i], b[i], PAPER) for i in range(4))
        rows += f'<div class="col" style="gap:10px"><div style="font-size:34px;font-weight:700">{titles[key]}</div><div class="row" style="gap:48px;direction:ltr;align-items:flex-start"><div class="row" style="gap:12px;align-items:flex-start;direction:ltr">{fa}</div><div class="row" style="gap:12px;align-items:flex-start;direction:ltr">{fb}</div></div></div>'
    return page(f'<div class="abs" style="left:48px;right:48px;top:30px;font-size:40px;font-weight:700">حركة المسابقة: A «لوحة» يسارًا، B «ورقة» يمينًا</div><div class="abs col" style="left:48px;top:130px;gap:34px">{rows}</div><div class="abs" style="left:48px;bottom:16px;font-size:22px;opacity:.6">{NOTE}</div>', "#24262B", "#fff", False, 1920, 1100)
S["quiz-C1-motion-keyframes"] = motion()

if __name__ == "__main__":
    d = Path(sys.argv[1] if len(sys.argv) > 1 else "_html"); d.mkdir(exist_ok=True, parents=True)
    for n, h in sorted(S.items()): (d / f"{n}.html").write_text(h, encoding="utf-8")
    print(len(S), "pages")
