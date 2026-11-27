import hashlib, random

def rng(key):
    return random.Random(int(hashlib.sha256(key.encode()).hexdigest()[:12], 16))

def pick_wrong(key, pool, answer, n=3):
    """n distinct entries of pool (list of (ar, en)) other than answer, deterministic."""
    r = rng(key)
    cand = [p for p in pool if p[0] != answer[0] and p[1] != answer[1]]
    r.shuffle(cand)
    out, seen = [], {answer[0], answer[1]}
    for ar, en in cand:
        if ar in seen or en in seen: continue
        seen.add(ar); seen.add(en); out.append({"ar": ar, "en": en})
        if len(out) == n: break
    return out

def item(id, type, category, difficulty, age, q_ar, q_en, a, wrong=None, source=None, numeric=False, evidence=None):
    d = {"id": id, "type": type, "category": category, "difficulty": difficulty, "age": age,
         "question": {"ar": q_ar, "en": q_en}}
    d["answer"] = a if type == "truefalse" else {"ar": a[0], "en": a[1]}
    if wrong: d["wrong"] = wrong
    d["source"] = source
    if numeric: d["numeric"] = True
    if evidence: d["evidence"] = evidence
    d["verified"] = True
    return d


import re as _re
def _norm(s):
    s = _re.sub(r"[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]", "", s)
    return _re.sub("[أإآٱ]", "ا", s).replace("ة", "ه").replace("ى", "ي")

def leaks(it):
    """the answer is in the question as whole words («ما عاصمة موناكو؟» answered «موناكو»): a quiz item that gives itself away"""
    a = _norm(it["answer"]["ar"]); a = _re.sub(r"^ال", "", a) if len(a) > 5 else a
    qt = [_re.sub(r"^ال", "", w) if len(w) > 5 else w for w in _re.findall(r"[\u0621-\u064A]+", _norm(it["question"]["ar"]))]
    at = _re.findall(r"[\u0621-\u064A]+", a)
    return len(a) > 3 and bool(at) and all(x in qt for x in at)
