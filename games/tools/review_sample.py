"""Writes the random samples (seeded, 30 per list) for games/CONTENT-REVIEW.md. Usage: python3 -I games/tools/review_sample.py > sample.md"""
import json, os, random, collections
G = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
L = lambda p: json.load(open(os.path.join(G, p), encoding="utf-8"))
def pick(items, n=30, seed=7): r = random.Random(seed); return r.sample(items, min(n, len(items)))
def line(title, count, items): print(f"### {title} ({count})\n" + " | ".join(items) + "\n")
odd = L("odd-one/topics.json")["topics"]
line("واحد مو معنا: كلمات", sum(len(t["words"]) for t in odd), [f"{w['ar']}" for w in pick([w for t in odd for w in t["words"]])])
ch = L("charades/words.json"); line("بدون كلام / وصّف", len(ch), [x["ar"] for x in pick(ch)])
kh = L("khanat/letters.json")["samples"]; flat = [(b, x["ar"]) for l in kh.values() for b, v in l.items() for x in v]; line("خانات: أمثلة (الصندوق: الكلمة)", len(flat), [f"{b}: {a}" for b, a in pick(flat)])
w5 = L("word5/answers.json"); line("خمّن الكلمة: الإجابات", len(w5), [x["display"] for x in pick(w5)])
ac = L("word5/accepted.json"); line("خمّن الكلمة: كلمات مقبولة (عينة)", len(ac), pick(ac))
mw = L("missing-word/items.json"); line("الكلمة المفقودة: الأقوال", sum(1 for x in mw if x["deck"] == "saying"), [x["text"]["ar"] for x in pick([x for x in mw if x["deck"] == "saying"])])
q = L("data/quiz.json"); line("أسئلة وجواب (عينة)", len(q), [x["question"]["ar"] for x in pick(q)])
