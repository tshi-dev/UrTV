"""Review pass over the quiz bank: duplicates, options that are the same word in another spelling, an answer inside its own question, long labels, kinds of item per category."""
import json, re, sys, os, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_words as bw
G = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
q = json.load(open(f"{G}/data/quiz.json", encoding="utf-8"))
norm = lambda s: re.sub(r"\s+", " ", bw.norm(s)).strip()
def key(s):
    n = norm(s)
    n = re.sub(r"^(ال|جمهوريه |مملكه |دوله |اتحاد )", "", n)
    return n
flags = collections.defaultdict(list)
seen_q = {}
for it in q:
    qa, a, w = it["question"]["ar"], it["answer"]["ar"], [x["ar"] for x in it["wrong"]]
    opts = [a] + w
    ks = [key(o) for o in opts]
    if len(set(ks)) != 4: flags["option twice (same word in another spelling)"].append(it["id"])
    for i in range(4):
        for j in range(i + 1, 4):
            if len(ks[i]) > 3 and len(ks[j]) > 3 and (ks[i] in ks[j] or ks[j] in ks[i]): flags["one option inside another"].append(it["id"])
    if key(a) and key(a) in norm(qa) and len(key(a)) > 3: flags["answer appears in its question"].append(it["id"])
    n = norm(qa)
    if n in seen_q: flags["same question twice"].append((it["id"], seen_q[n]))
    seen_q[n] = it["id"]
    if len(a) > 22: flags["long answer"].append(it["id"])
    if any(len(x) > 22 for x in w): flags["long wrong option"].append(it["id"])
kinds = collections.defaultdict(collections.Counter)
for it in q:
    kinds[it["category"]][re.sub(r"-[A-Za-z0-9]+$", "", it["id"]) if "-" in it["id"] else it["id"]] += 1
print({k: dict(v.most_common(6)) for k, v in kinds.items()})
for k, v in flags.items(): print(k, len(v), v[:6])
