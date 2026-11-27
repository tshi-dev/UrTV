"""Famous things per topic, from Wikidata (most-linked first). Output: scratch json with ar/en labels and Wikidata ids."""
import sys, os, json, re
sys.path.insert(0, os.path.dirname(__file__))
import net

def inst(cls, deep=True):
    return f"?x wdt:P31/wdt:P279* wd:{cls} ." if deep else f"?x wdt:P31 wd:{cls} ."

def taxon(cls): return f"?x wdt:P31 wd:Q16521 ; wdt:P105 wd:Q7432 ; wdt:P171* wd:{cls} ."

TOPICS = {  # id: (ar title, en title, pattern, min sitelinks)
 "mammals": ("حيوانات برية", "Wild animals", taxon("Q7377"), 60),
 "birds": ("طيور", "Birds", taxon("Q5113"), 50),
 "reptiles": ("زواحف وبرمائيات", "Reptiles", taxon("Q10811"), 40),
 "insects": ("حشرات", "Insects", taxon("Q1390"), 40),
 "fish": ("أسماك وحيوانات بحرية", "Sea life", taxon("Q152"), 40),
 "fruits": ("فواكه", "Fruits", inst("Q3314483"), 25),
 "vegetables": ("خضروات", "Vegetables", inst("Q11004"), 25),
 "flowers": ("أزهار وأشجار", "Flowers and trees", inst("Q506"), 25),
 "sports": ("رياضات", "Sports", inst("Q349"), 60),
 "vehicles": ("مركبات", "Vehicles", inst("Q42889"), 40),
 "clothes": ("ملابس", "Clothes", inst("Q11460"), 25),
 "furniture": ("أثاث", "Furniture", inst("Q14745"), 20),
 "tools": ("أدوات", "Tools", inst("Q39546"), 30),
 "drinks": ("مشروبات", "Drinks", inst("Q40050"), 30),
 "dishes": ("أكلات", "Dishes", inst("Q746549"), 25),
 "jobs": ("مهن", "Jobs", inst("Q28640"), 50),
 "saudi_cities": ("مدن سعودية", "Saudi cities", "?x wdt:P31/wdt:P279* wd:Q515 ; wdt:P17 wd:Q851 .", 8),
}

def fetch(tid):
    ar, en, pat, mn = TOPICS[tid]
    q = f'''SELECT ?x ?ar ?en ?sl WHERE {{ {pat} ?x wikibase:sitelinks ?sl . FILTER(?sl > {mn})
 ?x rdfs:label ?ar FILTER(lang(?ar)="ar") ?x rdfs:label ?en FILTER(lang(?en)="en") }} ORDER BY DESC(?sl) LIMIT 150'''
    rows = []
    for b in net.sparql(q):
        rows.append({"q": b["x"]["value"].rsplit("/", 1)[1], "ar": b["ar"]["value"], "en": b["en"]["value"], "sl": int(b["sl"]["value"])})
    return rows

if __name__ == "__main__":
    out = {}
    for t in TOPICS:
        try:
            out[t] = fetch(t)
        except Exception as e:
            out[t] = []; print("FAIL", t, str(e)[:80], flush=True)
        print(t, len(out[t]), flush=True)
    json.dump(out, open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False)
