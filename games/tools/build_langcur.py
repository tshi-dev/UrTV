"""Official language and currency of countries (Wikidata), only for countries with exactly one of each. Same country list as build_geo."""
import sys, os, collections
sys.path.insert(0, os.path.dirname(__file__))
import net, geo_fetch, build_geo
from common import item, pick_wrong

def q(prop):
    return f'''SELECT ?c ?v ?vl ?vla WHERE {{ ?c wdt:P463 wd:Q1065 ; wdt:P31 wd:Q3624078 ; wdt:{prop} ?v . FILTER NOT EXISTS {{ ?c wdt:P576 ?g }}
 ?v rdfs:label ?vl FILTER(lang(?vl)="en") ?v rdfs:label ?vla FILTER(lang(?vla)="ar") }}'''

def build():
    rows = geo_fetch.countries()
    ok = {k: v for k, v in rows.items() if v["en"] not in build_geo.SKIP and len(v["caps"]) == 1}
    out = {"quiz": []}
    for prop, kind in (("P37", "lang"), ("P38", "cur")):
        got = collections.defaultdict(dict)
        for b in net.sparql(q(prop)):
            c = b["c"]["value"].rsplit("/", 1)[1]
            if c in ok: got[c][b["v"]["value"].rsplit("/", 1)[1]] = (b["vla"]["value"], b["vl"]["value"])
        single = {c: next(iter(v.items())) for c, v in got.items() if len(v) == 1}
        pool = sorted({v[1] for v in single.values()})
        cnt = collections.Counter()
        for c, (vq, (vla, vl)) in sorted(single.items()):
            ar_ans = vla[len("اللغة "):] if vla.startswith("اللغة ") else vla
            if len(ar_ans) > 22 or cnt[vq] >= 8: continue
            cnt[vq] += 1
            name = build_geo.NAMES.get(ok[c]["en"], ok[c]["en"])
            if kind == "lang":
                qa, qe, cat = f"ما اللغة الرسمية في {ok[c]['ar']}؟", f"What is the official language of {name}?", "geography"
                en_ans = vl.replace(" language", "")
            else:
                qa, qe, cat = f"ما عملة {ok[c]['ar']}؟", f"What is the currency of {name}?", "geography"
                en_ans = vl
            a = (ar_ans, en_ans)
            wrong_pool = [((p[0][len("اللغة "):] if p[0].startswith("اللغة ") else p[0]), p[1].replace(" language", "")) for p in pool_pairs(single)]
            out["quiz"].append(item(f"geo-{kind}-{c}", "quiz", cat, 2, "all", qa, qe, a, pick_wrong(f"{kind}{c}", wrong_pool, a),
                                    [f"https://www.wikidata.org/wiki/{c}", f"https://www.wikidata.org/wiki/{vq}"]))
    return out

def pool_pairs(single):
    return sorted({v[1] for v in single.values()})
