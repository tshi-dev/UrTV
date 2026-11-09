"""Animal class quiz: for animals from wordlists.py, the Wikidata taxonomy (P171 chain) must put the species under exactly one
of five classes. Source: the animal's Wikidata item."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import net, wordlists, build_words
from common import item, pick_wrong

CLS = {"Q7377": ("ثدييات", "mammals"), "Q5113": ("طيور", "birds"), "Q10811": ("زواحف", "reptiles"), "Q10908": ("برمائيات", "amphibians"), "Q152": ("أسماك", "fish")}

def build():
    pairs, topics = [], {}
    for t in ("wild_animals", "birds_hand", "sea_and_reptiles", "animals_farm"):
        for tok in wordlists.L[t][2].split():
            ar, en = tok.split(":"); pairs.append((ar.replace("_", " "), en.replace("_", " ")))
    found = build_words.verify_pairs(pairs)
    qs = {v[0] for v in found.values()}
    vals = " ".join(f"wd:{q}" for q in sorted(qs)); cls = " ".join(f"wd:{c}" for c in CLS)
    rows = net.sparql(f"SELECT ?x ?c WHERE {{ VALUES ?x {{ {vals} }} VALUES ?c {{ {cls} }} ?x wdt:P171* ?c }}")
    got = {}
    for b in rows: got.setdefault(b["x"]["value"].rsplit("/", 1)[1], set()).add(b["c"]["value"].rsplit("/", 1)[1])
    out = {"quiz": []}
    options = [CLS[c] for c in CLS]
    for (ar, en), (q, sl) in sorted(found.items(), key=lambda kv: -kv[1][1]):
        c = got.get(q, set())
        if len(c) != 1: continue   # fish are a paraphyletic group in Wikidata: a species is under Q152 only sometimes; skip unclear ones
        k = next(iter(c))
        a = CLS[k]
        out["quiz"].append(item(f"an-class-{q}", "quiz", "animals", 1 if sl > 80 else 2, "kids", f"إلى أي مجموعة ينتمي حيوان {ar}؟",
                                f"Which group does the {build_words.DISPLAY_EN.get(en, en)} belong to?", a, pick_wrong("ac" + q, options, a), [f"https://www.wikidata.org/wiki/{q}"]))
    return out
