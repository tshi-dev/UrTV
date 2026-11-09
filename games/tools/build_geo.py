"""Capitals, continents and Gulf items from Wikidata (UN member states). Run via build_all.py."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from common import item, pick_wrong, rng
import geo_fetch

# not shipped: not a clean "one country, one capital" (dispute, several capitals), or must not be listed
SKIP = {"Israel", "Taiwan", "Kingdom of Denmark", "Kingdom of the Netherlands", "Bolivia", "South Africa", "Sri Lanka",
        "Eswatini", "Ivory Coast", "Benin", "Tanzania", "Malaysia", "Switzerland", "Montenegro", "Georgia",
        "Palestine", "Western Sahara", "Kosovo"}
NAMES = {"People's Republic of China": "China", "The Bahamas": "Bahamas", "The Gambia": "Gambia",
         "Democratic Republic of the Congo": "DR Congo", "Republic of the Congo": "Republic of the Congo"}
FAMOUS = {"Saudi Arabia", "Egypt", "France", "Germany", "Italy", "Spain", "United Kingdom", "United States", "Japan", "China",
          "India", "Turkey", "Russia", "Brazil", "Canada", "Australia", "Jordan", "Iraq", "Lebanon", "Syria", "Morocco",
          "Algeria", "Tunisia", "Libya", "Sudan", "Yemen", "Oman", "Qatar", "Kuwait", "Bahrain", "United Arab Emirates",
          "Pakistan", "Indonesia", "Greece", "Argentina", "Mexico", "South Korea", "Iran"}
GCC = {"Saudi Arabia", "Oman", "Qatar", "Kuwait", "Bahrain", "United Arab Emirates"}
CONT = {"Asia": ("آسيا", "Asia"), "Africa": ("أفريقيا", "Africa"), "Europe": ("أوروبا", "Europe"),
        "North America": ("أمريكا الشمالية", "North America"), "South America": ("أمريكا الجنوبية", "South America"),
        "Oceania": ("أوقيانوسيا", "Oceania")}

def build():
    rows = geo_fetch.countries()
    cs = []
    for q, r in rows.items():
        en = NAMES.get(r["en"], r["en"])
        if r["en"] in SKIP or len(r["caps"]) != 1: continue
        (cq, (cen, car)), = r["caps"].items()
        cs.append({"q": q, "en": en, "ar": r["ar"], "cq": cq, "cen": cen, "car": car, "conts": r["conts"],
                   "d": 1 if r["en"] in FAMOUS else 3})
    cs.sort(key=lambda c: c["q"])
    caps = [(c["car"], c["cen"]) for c in cs]
    out = {"quiz": [], "truefalse": []}
    for c in cs:
        src = [f"https://www.wikidata.org/wiki/{c['q']}", f"https://www.wikidata.org/wiki/{c['cq']}"]
        cat = "saudi-gulf" if c["en"] in GCC else "geography"
        kid = "kids" if c["d"] == 1 else "all"
        a = (c["car"], c["cen"])
        out["quiz"].append(item(f"geo-cap-{c['q']}", "quiz", cat, c["d"], kid, f"ما عاصمة {c['ar']}؟",
                                f"What is the capital of {c['en']}?", a, pick_wrong("w" + c["q"], caps, a), src))
        r = rng("tf" + c["q"])
        if r.random() < .5:
            out["truefalse"].append(item(f"geo-tf-{c['q']}", "truefalse", cat, c["d"], kid,
                f"عاصمة {c['ar']} هي {c['car']}.", f"The capital of {c['en']} is {c['cen']}.", True, None, src))
        else:
            w = pick_wrong("tw" + c["q"], caps, a, 1)[0]
            out["truefalse"].append(item(f"geo-tf-{c['q']}", "truefalse", cat, c["d"], kid,
                f"عاصمة {c['ar']} هي {w['ar']}.", f"The capital of {c['en']} is {w['en']}.", False, None, src))
        if c["d"] == 1 and c["en"] not in GCC:
            pass
        if len(c["conts"]) == 1:
            k = next(iter(c["conts"]))
            if k in CONT:
                pool = [v for kk, v in CONT.items() if kk != k]
                out["quiz"].append(item(f"geo-cont-{c['q']}", "quiz", "geography", 1 if c["d"] == 1 else 2, kid,
                    f"في أي قارة تقع {c['ar']}؟", f"On which continent is {c['en']}?", CONT[k],
                    pick_wrong("c" + c["q"], pool, CONT[k]), src[:1]))
        if c["d"] == 3 or c["en"] in {"Egypt", "France", "Japan", "Brazil", "Canada", "Italy", "Germany"}:
            pool = [(x["ar"], x["en"]) for x in cs]
            ca = (c["ar"], c["en"])
            out["quiz"].append(item(f"geo-rev-{c['q']}", "quiz", "geography", 3, "all",
                f"{c['car']} هي عاصمة أي دولة؟", f"{c['cen']} is the capital of which country?", ca,
                pick_wrong("r" + c["q"], pool, ca), src))
    return out
