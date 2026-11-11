"""Inflected forms for the accepted-guesses list of word5: first the Arabic noun/verb/adjective lexemes with a short lemma,
then their forms in batches by lexeme id (an indexed lookup, unlike the scan that timed out). Cached; safe to re-run.
Usage: python3 -I games/tools/lex_forms2.py OUT.json"""
import sys, os, re, json, urllib.parse
sys.path.insert(0, os.path.dirname(__file__))
import net, lex_forms as lf

CATS = "wd:Q1084 wd:Q24905 wd:Q34698"
def ids():
    q = f'''SELECT ?l WHERE {{ ?l dct:language wd:Q13955 ; wikibase:lexicalCategory ?c . VALUES ?c {{ {CATS} }} }} LIMIT 120000'''
    return sorted({b["l"]["value"].rsplit("/", 1)[1] for b in net.sparql(q)})

def forms(batch):
    vals = " ".join("wd:" + x for x in batch)
    q = f'''SELECT ?w WHERE {{ VALUES ?l {{ {vals} }} ?l ontolex:lexicalForm ?f . ?f ontolex:representation ?w . FILTER(lang(?w)="ar") }}'''
    return [b["w"]["value"] for b in net.sparql(q)]

if __name__ == "__main__":
    out, fail = set(), []
    L = ids(); print("lexemes", len(L), flush=True)
    for i in range(0, len(L), 400):
        try: ws = forms(L[i:i + 400])
        except Exception as e: fail.append(i); continue
        for w in ws:
            n = lf.norm(w)
            if re.fullmatch(f"[{lf.LET28}]{{5}}", n): out.add(n)
        print(i, len(out), flush=True)
    json.dump(sorted(out), open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False)
    print("total", len(out), "failed batches", len(fail))
