"""The reference reader of games/meta.json: what the app's setup screens must do (the app's Java copy follows these rules; tests/games_meta_test.py pins them).
Rules: unknown fields are skipped at every level; an entry that lacks a usable name (ar and en) or id is skipped, never the whole file; a file whose "v" is not 1 is
ignored (the app keeps its built-in lists); a chip is offered only when its group holds min_items or more questions for the chosen level and filter."""
import json

def read(text):
    try: m = json.loads(text)
    except ValueError: return None
    return m if isinstance(m, dict) and m.get("v") == 1 and isinstance(m.get("games"), dict) else None

def _name(e):
    return isinstance(e, dict) and isinstance(e.get("id"), str) and e["id"] and isinstance(e.get("ar"), str) and e["ar"] and isinstance(e.get("en"), str) and e["en"]

def quiz_chips(m, level=0, kids=False):
    """the «الفئة» chips for the quiz games: [(group id, ar, en, count, offered)], then «مزيج» is always offered by the app itself. level 0 = every level."""
    q = (m.get("games") or {}).get("quiz") or {}
    cats = {c["id"]: c for c in q.get("cats", []) if _name(c) and isinstance(c.get("n"), list) and len(c["n"]) == 3 and all(isinstance(x, int) for x in c["n"])}
    need = m.get("min_items") if isinstance(m.get("min_items"), int) else 40
    out, grouped = [], set()
    for g in m.get("groups", []):
        if not _name(g) or not isinstance(g.get("cats"), list): continue
        n = 0
        for cid in g["cats"]:
            c = cats.get(cid)
            if c is None: continue
            grouped.add(cid)
            n += (c["n"][level - 1] if level else sum(c["n"])) if not kids else min(c.get("kids", 0), c["n"][level - 1] if level else sum(c["n"]))
        out.append((g["id"], g["ar"], g["en"], n, n >= need))
    # a category that is in no group (new in the data) shows up by itself under its own name
    for cid, c in sorted(cats.items()):
        if cid in grouped: continue
        n = (c["n"][level - 1] if level else sum(c["n"])) if not kids else min(c.get("kids", 0), c["n"][level - 1] if level else sum(c["n"]))
        out.append((cid, c["ar"], c["en"], n, n >= need))
    return out

def read_quiz_v2(text):
    """quiz-v2.json: {"v":2,"items":[...]}; an item that does not have the fields the games need is skipped, extra fields are ignored"""
    try: d = json.loads(text)
    except ValueError: return None
    if not isinstance(d, dict) or d.get("v") != 2 or not isinstance(d.get("items"), list): return None
    keep = []
    for x in d["items"]:
        try:
            if (isinstance(x["i"], str) and x["d"] in (1, 2, 3) and len(x["q"]) == 2 and len(x["a"]) == 2 and len(x["w"]) == 3 and all(len(w) == 2 for w in x["w"])
                    and len({x["a"][0]} | {w[0] for w in x["w"]}) == 4): keep.append(x)
        except (KeyError, TypeError): continue
    return keep
