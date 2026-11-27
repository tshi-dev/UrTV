"""Arabic lexeme forms from Wikidata, one query per first letter. Output: set of normalized 5-letter words (28-letter alphabet)."""
import sys, os, re, json, urllib.parse
sys.path.insert(0, os.path.dirname(__file__))
import net
LET28 = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
def strip(s): return re.sub(r"[ً-ٰٟـۖ-ۭ]", "", s)
def norm(s):
    s = strip(s); s = re.sub("[أإآٱ]", "ا", s); return s.replace("ة", "ه").replace("ى", "ي")
FIRST = {"ا": ["ا", "أ", "إ", "آ", "ٱ"], "ه": ["ه", "ة"], "ي": ["ي", "ى"]}
def fetch_letter(l):
    cond = " || ".join(f'STRSTARTS(?w, "{c}")' for c in FIRST.get(l, [l]))
    q = f'''SELECT DISTINCT ?w WHERE {{ ?lx dct:language wd:Q13955 ; ontolex:lexicalForm ?f . ?f ontolex:representation ?w . FILTER(lang(?w)="ar" && ({cond}) && STRLEN(?w) >= 5 && STRLEN(?w) <= 10) }} LIMIT 60000'''
    return [b["w"]["value"] for b in net.sparql(q)]
def words():
    out = set()
    for l in LET28:
        try: ws = fetch_letter(l)
        except Exception as e: print("FAIL", l, str(e)[:60], flush=True); continue
        n = {norm(w) for w in ws}
        n = {w for w in n if re.fullmatch(f"[{LET28}]{{5}}", w)}
        print(l, len(ws), len(n), flush=True); out |= n
    return out
if __name__ == "__main__":
    w = words(); json.dump(sorted(w), open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False); print("total", len(w))
