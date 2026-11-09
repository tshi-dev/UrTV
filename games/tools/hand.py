"""Hand-written items whose facts are checked in code: every source is (page title, regex) and the regex must match the
English Wikipedia intro of that page when the build runs; an item whose evidence is missing is dropped (and counted)."""
import re
from common import item
import net

DROPPED = []

def W(title): return "https://en.wikipedia.org/wiki/" + title.replace(" ", "_")

def make(rows, titles_cache):
    """rows: dict(id, type, cat, d, age, q=(ar,en), a=(ar,en) or bool, wrong=[(ar,en)x3], ev=[(title, regex)], numeric)"""
    out = {}
    pages = sorted({t for r in rows for t, _ in r["ev"]})
    intro = net.wiki_intro("en", pages)
    for r in rows:
        ok = True
        for t, rx in r["ev"]:
            if not re.search(rx, intro.get(t, ""), re.I | re.S): ok = False; DROPPED.append((r["id"], t, rx))
        if not ok: continue
        need = 2 if r.get("numeric") else 1
        if len({t for t, _ in r["ev"]}) < need: DROPPED.append((r["id"], "needs two sources", "")); continue
        wrong = [{"ar": a, "en": e} for a, e in (r.get("wrong") or [])] or None
        it = item(r["id"], r["type"], r["cat"], r["d"], r["age"], r["q"][0], r["q"][1], r["a"], wrong,
                  [W(t) for t, _ in r["ev"]], r.get("numeric", False), [rx for _, rx in r["ev"]])
        out.setdefault(r["type"], []).append(it)
    return out
