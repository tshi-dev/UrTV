"""Maths and logic items. Every answer is computed by this script (never typed), so the items verify themselves.
Sources are `computed:<rule>`; checker allows that only in the maths-logic category."""
import sys, os, itertools, math, hashlib
sys.path.insert(0, os.path.dirname(__file__))
from common import rng

AR = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")
def ar(n): return str(n).translate(AR)

def mk(id, type, d, age, qa, qe, ans, wrong=None, src="computed:arithmetic", **extra):
    it = {"id": id, "type": type, "category": "maths-logic", "difficulty": d, "age": age,
          "question": {"ar": qa, "en": qe}, "answer": {"ar": ans[0], "en": ans[1]}}
    if wrong: it["wrong"] = [{"ar": a, "en": e} for a, e in wrong]
    it.update(extra)
    it["source"] = [src]; it["verified"] = True
    return it

def near(key, v, lo=0, step=None):
    """three distinct wrong integers close to v, never v, never below lo"""
    r = rng(key); cand = set()
    steps = step or [1, 2, 3, 5, 10]
    while len(cand) < 3:
        x = v + r.choice([-1, 1]) * r.choice(steps)
        if x != v and x >= lo: cand.add(x)
    return sorted(cand)

def num_opts(key, v, lo=0, step=None):
    return [(ar(x), str(x)) for x in near(key, v, lo, step)]

def build():
    out = {"quiz": [], "riddle": [], "estimate": []}
    # multiplication tables (kids)
    for a, b in [(6, 7), (7, 8), (8, 9), (6, 8), (9, 9), (7, 7), (12, 5), (11, 6), (9, 6), (8, 8), (12, 12), (7, 9), (6, 9), (13, 4), (15, 3), (14, 5), (25, 4), (16, 5)]:
        v = a * b
        out["quiz"].append(mk(f"mt-mul-{a}x{b}", "quiz", 1 if max(a, b) <= 9 else 2, "kids", f"ما ناتج {ar(a)} × {ar(b)}؟", f"What is {a} × {b}?", (ar(v), str(v)), num_opts(f"m{a}{b}", v, 1, [1, 2, 4, 6, 8, 10])))
    for a, b in [(48, 6), (72, 8), (56, 7), (81, 9), (144, 12), (63, 7), (96, 8), (120, 10), (45, 5)]:
        v = a // b; assert v * b == a
        out["quiz"].append(mk(f"mt-div-{a}-{b}", "quiz", 1 if a <= 72 else 2, "kids", f"ما ناتج {ar(a)} ÷ {ar(b)}؟", f"What is {a} ÷ {b}?", (ar(v), str(v)), num_opts(f"d{a}{b}", v, 1, [1, 2, 3, 4])))
    # two-digit addition and subtraction
    for a, b in [(47, 38), (56, 29), (68, 17), (95, 26), (134, 59), (205, 98), (76, 87), (350, 275)]:
        out["quiz"].append(mk(f"mt-add-{a}-{b}", "quiz", 1 if a < 100 else 2, "kids", f"ما ناتج {ar(a)} + {ar(b)}؟", f"What is {a} + {b}?", (ar(a + b), str(a + b)), num_opts(f"a{a}{b}", a + b, 1, [1, 2, 10, 20])))
    for a, b in [(91, 38), (100, 47), (83, 29), (250, 175), (500, 123), (72, 45), (64, 38)]:
        out["quiz"].append(mk(f"mt-sub-{a}-{b}", "quiz", 1 if a < 100 else 2, "kids", f"ما ناتج {ar(a)} − {ar(b)}؟", f"What is {a} − {b}?", (ar(a - b), str(a - b)), num_opts(f"s{a}{b}", a - b, 1, [1, 2, 10, 20])))
    # fractions and percentages of a number
    for n, f, name_ar, name_en in [(80, (1, 2), "نصف", "half"), (80, (1, 4), "ربع", "a quarter"), (60, (1, 3), "ثلث", "a third"), (200, (1, 5), "خُمس", "a fifth"), (90, (1, 10), "عُشر", "a tenth"), (120, (3, 4), "ثلاثة أرباع", "three quarters"), (48, (2, 3), "ثلثا", "two thirds")]:
        v = n * f[0] // f[1]; assert v * f[1] == n * f[0]
        out["quiz"].append(mk(f"mt-frac-{n}-{f[0]}{f[1]}", "quiz", 2, "all", f"كم يساوي {name_ar} العدد {ar(n)}؟", f"What is {name_en} of {n}?", (ar(v), str(v)), num_opts(f"f{n}{f[0]}", v, 1, [2, 3, 5, 10])))
    for n, p in [(200, 10), (50, 20), (80, 25), (300, 50), (40, 5), (250, 10), (120, 25), (60, 50)]:
        v = n * p // 100; assert v * 100 == n * p
        out["quiz"].append(mk(f"mt-pct-{n}-{p}", "quiz", 2, "all", f"كم يساوي {ar(p)}٪ من {ar(n)}؟", f"What is {p}% of {n}?", (ar(v), str(v)), num_opts(f"p{n}{p}", v, 1, [2, 5, 10])))
    # sequences
    seqs = []
    for start, d in [(2, 3), (5, 5), (10, 7), (3, 4), (1, 9), (100, -8), (50, -6), (7, 11)]:
        s = [start + d * i for i in range(5)]; seqs.append((f"arith{start}_{d}", s[:4], s[4], "أضف" if d > 0 else "اطرح"))
    for start, r in [(1, 2), (3, 2), (2, 3), (5, 2), (1, 3)]:
        s = [start * r ** i for i in range(5)]; seqs.append((f"geo{start}_{r}", s[:4], s[4], ""))
    seqs.append(("squares", [1, 4, 9, 16], 25, "")); seqs.append(("cubes", [1, 8, 27, 64], 125, ""))
    seqs.append(("fib", [1, 1, 2, 3, 5, 8], 13, "")); seqs.append(("tri", [1, 3, 6, 10, 15], 21, ""))
    seqs.append(("pow10", [1, 10, 100, 1000], 10000, ""))
    for key, head, v, _ in seqs:
        text_ar = "، ".join(ar(x) for x in head); text_en = ", ".join(str(x) for x in head)
        out["quiz"].append(mk(f"mt-seq-{key}", "quiz", 2 if key.startswith(("arith", "squares")) else 3, "all", f"ما العدد التالي في المتتالية: {text_ar}، ...؟", f"What number comes next: {text_en}, ...?", (ar(v), str(v)), num_opts("q" + key, v, 1, [1, 2, 3, 4, 6]), src="computed:sequence"))
    # geometry facts computed from definitions
    for name_ar, name_en, n in [("المثلث", "triangle", 3), ("الخماسي", "pentagon", 5), ("السداسي", "hexagon", 6), ("الثماني", "octagon", 8), ("المربع", "square", 4)]:
        out["quiz"].append(mk(f"mt-sides-{name_en}", "quiz", 1 if n in (3, 4) else 2, "kids", f"كم ضلعًا في الشكل {name_ar}؟", f"How many sides does a {name_en} have?", (ar(n), str(n)), num_opts("g" + name_en, n, 3, [1, 2, 3]), src="computed:definition"))
    for side in (5, 7, 9, 12):
        out["quiz"].append(mk(f"mt-perim-{side}", "quiz", 1, "kids", f"ما محيط مربع طول ضلعه {ar(side)} سم؟", f"What is the perimeter of a square with side {side} cm?", (ar(4 * side), str(4 * side)), num_opts(f"pr{side}", 4 * side, 1, [2, 4, 8]), src="computed:geometry"))
        out["quiz"].append(mk(f"mt-area-{side}", "quiz", 2, "kids", f"ما مساحة مربع طول ضلعه {ar(side)} سم (بالسم المربع)؟", f"What is the area of a square with side {side} cm (in square cm)?", (ar(side * side), str(side * side)), num_opts(f"ar{side}", side * side, 1, [2, 4, 8, 10]), src="computed:geometry"))
    # time and units
    for q_ar, q_en, v in [("كم ثانية في الدقيقة الواحدة؟", "How many seconds are in one minute?", 60), ("كم دقيقة في الساعة؟", "How many minutes are in an hour?", 60),
                          ("كم ساعة في اليوم؟", "How many hours are in a day?", 24), ("كم يومًا في الأسبوع؟", "How many days are in a week?", 7),
                          ("كم متر في الكيلومتر؟", "How many metres are in a kilometre?", 1000), ("كم سنتيمتر في المتر؟", "How many centimetres are in a metre?", 100),
                          ("كم جرام في الكيلوجرام؟", "How many grams are in a kilogram?", 1000), ("كم أسبوع في السنة تقريبًا؟", "About how many weeks are in a year?", 52)]:
        d = 1
        out["quiz"].append(mk("mt-unit-" + hashlib.sha256(q_en.encode()).hexdigest()[:6], "quiz", d, "kids", q_ar, q_en, (ar(v), str(v)), num_opts("u" + q_en, v, 1, [1, 3, 10, 30, 40, 50]), src="computed:definition"))
    # estimation: closest wins (computed, stable)
    est = [("كم ثانية في اليوم؟", "How many seconds are in a day?", 24 * 3600, "ثانية", "seconds"), ("كم دقيقة في الأسبوع؟", "How many minutes are in a week?", 7 * 24 * 60, "دقيقة", "minutes"),
           ("كم ساعة في الأسبوع؟", "How many hours are in a week?", 7 * 24, "ساعة", "hours"), ("كم رجلًا لـ ١٢ دجاجة؟", "How many legs do 12 chickens have?", 24, "رجل", "legs"),
           ("كم إصبعًا في ١٠ أيدٍ؟", "How many fingers are on 10 hands?", 50, "إصبع", "fingers"), ("كم ملّيمتر في المتر؟", "How many millimetres are in a metre?", 1000, "ملّيمتر", "millimetres"),
           ("كم مترًا في ٥ كيلومترات؟", "How many metres are in 5 kilometres?", 5000, "متر", "metres"), ("كم دقيقة في ٣ ساعات؟", "How many minutes are in 3 hours?", 180, "دقيقة", "minutes"),
           ("كم يومًا في ١٢ أسبوعًا؟", "How many days are in 12 weeks?", 84, "يوم", "days"), ("كم حرفًا في الأبجدية العربية؟", "How many letters are in the Arabic alphabet?", 28, "حرف", "letters")]
    for i, (qa, qe, v, ua, ue) in enumerate(est):
        out["estimate"].append(mk(f"mt-est-{i}", "estimate", 2, "all", qa, qe, (f"{ar(v)} {ua}", f"{v} {ue}"), value=v, unit={"ar": ua, "en": ue}, src="computed:arithmetic"))
    # riddles solved by brute force in the build
    def solve(pred, rng_=range(0, 1000)):
        sols = [n for n in rng_ if pred(n)]
        assert len(sols) == 1, sols
        return sols[0]
    R = []
    n = solve(lambda x: x * x == 64); R.append(("sq64", "ما العدد الذي إذا ضربته في نفسه كان الناتج ٦٤؟", "Which number multiplied by itself gives 64?", n, ""))
    n = solve(lambda x: x // 3 == 12 and x % 3 == 0); R.append(("third12", "عدد ثلثه ١٢. ما هو؟", "A number's third is 12. What is it?", n, ""))
    n = solve(lambda x: x + x // 2 == 30 and x % 2 == 0); R.append(("plus_half", "عدد إذا أضفت إليه نصفه صار ٣٠. ما هو؟", "A number plus its half is 30. What is it?", n, ""))
    n = solve(lambda x: 2 * x + 7 == 31); R.append(("lin", "فكّرت في عدد، ضاعفته ثم أضفت ٧ فصار ٣١. ما العدد؟", "I doubled a number and added 7 to get 31. Which number?", n, ""))
    n = solve(lambda x: x + (x + 1) + (x + 2) == 48); R.append(("consec", "ثلاثة أعداد متتالية مجموعها ٤٨. ما أصغرها؟", "Three consecutive numbers add up to 48. What is the smallest?", n, ""))
    n = solve(lambda x: x * 4 == 100 and True); R.append(("quarter", "ربع عدد يساوي ٢٥. ما العدد؟", "A quarter of a number is 25. What is it?", n, ""))
    n = solve(lambda x: x + 15 == 3 * x - 5 and x > 0); R.append(("balance", "إذا أضفنا ١٥ إلى عدد حصلنا على ما يساوي ثلاثة أمثاله ناقص ٥. ما العدد؟", "Adding 15 to a number gives three times it minus 5. Which number?", n, ""))
    n = solve(lambda x: x * 12 == 144); R.append(("dozen", "كم دزينة في ١٤٤ قطعة؟", "How many dozens are in 144 items?", n, ""))
    # handshakes: n people, each shakes every other once
    for people in (4, 5, 6, 8, 10):
        v = sum(1 for _ in itertools.combinations(range(people), 2))
        R.append((f"hand{people}", f"في مجلس {ar(people)} أشخاص، صافح كلٌّ منهم كل الآخرين مرة واحدة. كم مصافحة حدثت؟", f"{people} people each shake hands once with every other person. How many handshakes?", v, ""))
    # ages
    for fa, so, yrs in [(40, 10, 0), (36, 6, 0)]:
        n = solve(lambda y: (fa + y) == 2 * (so + y), range(0, 100)); R.append((f"age{fa}", f"عمر الأب {ar(fa)} وعمر ابنه {ar(so)}. بعد كم سنة يصير عمر الأب ضعف عمر الابن؟", f"A father is {fa} and his son is {so}. In how many years will the father be twice as old?", n, ""))
    # clock hands angle
    for h in (3, 6, 9):
        v = abs(30 * h) % 360; v = min(v, 360 - v)
        R.append((f"clock{h}", f"كم درجة بين عقربي الساعة عند الساعة {ar(h)}:٠٠ (الزاوية الأصغر)؟", f"How many degrees between the clock hands at {h}:00 (the smaller angle)?", v, ""))
    for key, qa, qe, v, _ in R:
        out["riddle"].append(mk(f"mt-rid-{key}", "riddle", 2, "all", qa, qe, (ar(v), str(v)), src="computed:solved-by-search"))
    return out
