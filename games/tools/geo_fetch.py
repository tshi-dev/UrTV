"""Fetches current UN member states with one capital from Wikidata (cached). Prints a summary."""
import sys, os, json
sys.path.insert(0, os.path.dirname(__file__))
import net

Q = '''SELECT ?c ?cl ?cla ?cap ?capl ?capla ?cont ?contl WHERE {
 ?c wdt:P463 wd:Q1065 . ?c wdt:P31 wd:Q3624078 .
 FILTER NOT EXISTS { ?c wdt:P576 ?gone }
 ?c wdt:P36 ?cap .
 OPTIONAL { ?c wdt:P30 ?cont . ?cont rdfs:label ?contl FILTER(lang(?contl)="en") }
 ?c rdfs:label ?cl FILTER(lang(?cl)="en") ?c rdfs:label ?cla FILTER(lang(?cla)="ar")
 ?cap rdfs:label ?capl FILTER(lang(?capl)="en") ?cap rdfs:label ?capla FILTER(lang(?capla)="ar")
}'''

def countries():
    rows = {}
    for b in net.sparql(Q):
        q = b["c"]["value"].rsplit("/", 1)[1]
        r = rows.setdefault(q, {"q": q, "en": b["cl"]["value"], "ar": b["cla"]["value"], "caps": {}, "conts": set()})
        r["caps"][b["cap"]["value"].rsplit("/", 1)[1]] = (b["capl"]["value"], b["capla"]["value"])
        if "contl" in b: r["conts"].add(b["contl"]["value"])
    return rows

if __name__ == "__main__":
    c = countries()
    print(len(c), "countries;", sum(len(v["caps"]) == 1 for v in c.values()), "with one capital")
    print(sorted(v["en"] for v in c.values())[:300])
