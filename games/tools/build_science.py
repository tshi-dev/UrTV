"""Chemical elements: Wikidata (symbol, atomic number, Arabic name) cross-checked against each element's English
Wikipedia intro ("symbol X and atomic number N"). Only elements where both sources agree are kept."""
import sys, os, re
sys.path.insert(0, os.path.dirname(__file__))
import net
from common import item, pick_wrong, rng

Q = '''SELECT ?e ?el ?ela ?z ?sym WHERE { ?e wdt:P31 wd:Q11344 ; wdt:P1086 ?z ; wdt:P246 ?sym .
 ?e rdfs:label ?el FILTER(lang(?el)="en") ?e rdfs:label ?ela FILTER(lang(?ela)="ar") }'''
COMMON = {"Hydrogen","Helium","Carbon","Nitrogen","Oxygen","Sodium","Iron","Gold","Silver","Copper","Calcium","Chlorine","Mercury","Lead","Neon"}

def build(stats):
    rows = {}
    for b in net.sparql(Q):
        z = int(b["z"]["value"])
        if z <= 118: rows[z] = (b["e"]["value"].rsplit("/", 1)[1], b["el"]["value"], b["ela"]["value"], b["sym"]["value"])
    titles = [v[1] for z, v in sorted(rows.items()) if z <= 86]
    intro = net.wiki_intro("en", titles)
    ok = []
    for z, (q, en, ar, sym) in sorted(rows.items()):
        if z > 86: continue
        t = intro.get(en, "")
        m = re.search(r"symbol\s+([A-Z][a-z]?)\s+and\s+atomic\s+number\s+(\d+)", t)
        if not m or m.group(1) != sym or int(m.group(2)) != z or " " in ar or len(ar) > 20:
            stats["dropped"] = stats.get("dropped", 0) + 1; continue
        ok.append((z, q, en, ar, sym))
    out = {"quiz": [], "truefalse": []}
    names = [(ar, en) for z, q, en, ar, sym in ok]
    syms = [(s, s) for *_, s in ok]
    for z, q, en, ar, sym in ok:
        src = [f"https://www.wikidata.org/wiki/{q}", f"https://en.wikipedia.org/wiki/{en.replace(' ', '_')}"]
        d = 1 if en in COMMON else (2 if z <= 54 else 3)
        a = (sym, sym)
        out["quiz"].append(item(f"el-sym-{z}", "quiz", "science", d, "all", f"ما الرمز الكيميائي لعنصر {ar}؟",
            f"What is the chemical symbol of {en}?", a, pick_wrong(f"s{z}", syms, a), src))
        out["quiz"].append(item(f"el-bysym-{z}", "quiz", "science", min(3, d + 1), "all", f"ما العنصر الذي رمزه الكيميائي {sym}؟",
            f"Which element has the chemical symbol {sym}?", (ar, en), pick_wrong(f"b{z}", names, (ar, en)), src))
        a2 = (ar, en)
        out["quiz"].append(item(f"el-name-{z}", "quiz", "science", min(3, d + 1), "all", f"أي عنصر رقمه الذري {z}؟",
            f"Which element has atomic number {z}?", a2, pick_wrong(f"n{z}", names, a2), src, numeric=True))
    return out
