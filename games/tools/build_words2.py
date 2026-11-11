"""Second-pass word data (curated vocabulary + lexemes). Writes odd-one, charades, khanat, word5, missing-word, letter-hive.
Run after build_all.py: SSL_CERT_FILE=<ca bundle> python3 -I games/tools/build_words2.py [forms5.json]"""
import sys, os, re, json, collections, urllib.parse
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import net, curated, sayings_curated, names_curated, build_words as bw, geo_fetch, build_geo
G = os.path.join(HERE, "..")
LET28, norm, strip, first_letter = bw.LET28, bw.norm, bw.strip, bw.first_letter
dump = lambda path, obj, **kw: json.dump(obj, open(os.path.join(G, path), "w", encoding="utf-8"), ensure_ascii=False, **kw)

def parse():
    T = {}
    for tid, (ar, en, text) in curated.T.items():
        rows, seen = [], set()
        for tok in text.split():
            a, e = tok.split(":")
            a, e = a.replace("_", " "), e.replace("_", " ")
            if norm(a) in seen: continue
            seen.add(norm(a)); rows.append((a, e))
        T[tid] = (ar, en, rows)
    return T

def level(i, n): return 1 if i < n / 3 else (2 if i < 2 * n / 3 else 3)

def main(forms_path):
    T = parse()
    # ---- odd-one
    odd = []
    for tid, (ar, en, rows) in T.items():
        if tid in ("actions", "feelings"): continue
        odd.append({"id": tid, "ar": ar, "en": en, "age": "all", "words": [{"ar": a, "en": e, "difficulty": level(i, len(rows)), "source": "curated"} for i, (a, e) in enumerate(rows)]})
    dump("odd-one/topics.json", {"topics": odd}, indent=0)
    # ---- charades
    words, seen = [], set()
    DESCRIBE_ONLY = {"saudi_cities", "arab_countries", "landmarks", "spaces"}
    for tid, (ar, en, rows) in T.items():
        for i, (a, e) in enumerate(rows):
            if norm(a) in seen: continue
            seen.add(norm(a)); lv = level(i, len(rows))
            words.append({"id": f"chc-{len(words)}", "ar": a, "en": e, "category": tid, "difficulty": lv, "age": "kids" if lv == 1 and tid not in ("jobs",) else "all",
                          "modes": ["describe"] if tid in DESCRIBE_ONLY else ["act", "describe"], "source": "curated", "verified": "curated"})
    dump("charades/words.json", words, indent=0)
    # ---- khanat
    BOX = {"animal": ["farm", "wild", "birds", "sea", "insects"], "plant": ["fruits", "vegetables"],
           "object": ["home", "kitchen", "school", "tools", "tech", "toys", "clothes", "vehicles"], "food": ["food", "sweets", "dishes", "drinks"],
           "job": ["jobs"], "saudi_city": ["saudi_cities"], "country": ["arab_countries"]}
    LETTERS = "ابتجحخدذرزسشصطعغفقكلمنهوي"
    samples = {l: {} for l in LETTERS}
    def add(box, a, e, src="curated"):
        l = first_letter(a)
        if l in samples and len(a) <= 24 and re.fullmatch(r"[\u0621-\u064A ]{3,24}", strip(a)) and not re.search(r"[A-Za-z]", a):
            lst = samples[l].setdefault(box, [])
            if len(lst) < 15 and all(norm(x["ar"]) != norm(a) for x in lst): lst.append({"ar": a, "en": e, "source": src})
    for box, tids in BOX.items():
        for t in tids:
            for a, e in T[t][2]: add(box, a, e)
    for n in names_curated.NAMES: add("name", n, "")
    for v in geo_fetch.countries().values():
        if v["en"] in build_geo.SKIP or len(v["caps"]) != 1 or v["en"] not in build_geo.FAMOUS: continue   # well-known countries and their capitals only
        add("country", v["ar"], build_geo.NAMES.get(v["en"], v["en"]), bw.src(v["q"]))
        (cq, (cen, car)), = v["caps"].items(); add("city", car, cen, bw.src(cq))   # world capitals: a separate, additive box "city"
    dump("khanat/letters.json", {"letters": list(LETTERS), "rare_letters": list("ثضظ"), "hard_letters": list("خذشصطغزوي"),
         "boxes": ["animal", "plant", "object", "country", "name"], "extra_boxes": ["food", "job", "saudi_city", "city"], "samples": samples}, indent=0)
    cov = {b: [sum(1 for l in LETTERS if len(samples[l].get(b, [])) >= 15), sum(1 for l in LETTERS if samples[l].get(b))] for b in ["animal", "plant", "object", "country", "name", "food", "job", "saudi_city", "city"]}
    print("khanat letters with 15 samples / with any sample (of 25):", cov)
    # ---- missing-word: facts (bank) + repo sayings + curated sayings
    mw = [x for x in json.load(open(os.path.join(G, "missing-word/items.json"), encoding="utf-8")) if x["deck"] in ("fact", "saying") and not x["id"].startswith("mw-cur-")]
    have = {norm(strip(x["text"]["ar"].replace("___", ""))) for x in mw if x["deck"] == "saying"}
    def cloze(text):
        toks = text.split()
        cand = [j for j, t in enumerate(toks) if len(strip(t)) >= 4]
        if len(toks) < 3 or not cand: return None
        j = cand[-1]; w = toks[j]
        return " ".join(toks[:j] + ["___"] + toks[j + 1:]), w
    nxt = 0
    for s in sayings_curated.SAYINGS:
        if any(norm(strip(s)) in h or h in norm(strip(s)) for h in have if len(h) > 10): continue
        c = cloze(s)
        if not c: continue
        mw.append({"id": f"mw-cur-{nxt}", "deck": "saying", "text": {"ar": c[0], "en": ""}, "answer": {"ar": c[1], "en": ""}, "accept": [], "difficulty": 2, "age": "all",
                   "source": ["curated"], "verified": "curated", "full": s}); nxt += 1
    dump("missing-word/items.json", mw, indent=0)
    print("missing-word", collections.Counter(x["deck"] for x in mw))
    # ---- letter-hive: 28 letters
    q = json.load(open(os.path.join(G, "data/quiz.json"), encoding="utf-8"))
    idx = collections.defaultdict(list)
    for it in q:
        if it["type"] != "quiz" or not re.match(r"[ء-ي]", it["answer"]["ar"]) or it["id"].startswith("el-sym-"): continue
        l = first_letter(it["answer"]["ar"])
        if l in LET28: idx[l].append(it["id"])
    # English side: the first Latin letter of the English answer (for English play), same rule, built from the same bank
    en_idx = collections.defaultdict(list)
    for it in q:
        if it["type"] != "quiz" or it["id"].startswith("el-sym-"): continue
        e = it["answer"]["en"].strip()
        e = e[4:] if e.lower().startswith("the ") else e
        if re.match(r"[A-Za-z]", e): en_idx[e[0].upper()].append(it["id"])
    short = [l for l in LET28 if len(idx[l]) < 12]
    dump("letter-hive/index.json", {"letters": list(LET28), "short_letters": short, "index": {l: idx[l] for l in LET28}, "coverage": {l: len(idx[l]) for l in LET28},
                                 "en_index": {l: en_idx[l] for l in sorted(en_idx)}, "en_coverage": {l: len(en_idx[l]) for l in sorted(en_idx)}}, indent=0)
    dump("letter-hive/letters.json", list(LET28))
    print("letter-hive short letters:", {l: len(idx[l]) for l in short}, "| English letters under 12:", {l: len(en_idx[l]) for l in sorted(en_idx) if len(en_idx[l]) < 12}, "missing:", [c for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" if c not in en_idx])
    # ---- word5
    lex = json.loads(net.get("https://query.wikidata.org/sparql?format=json&query=" + urllib.parse.quote(
        '''SELECT ?l ?lemma ?cat WHERE { ?l a ontolex:LexicalEntry ; dct:language wd:Q13955 ; wikibase:lemma ?lemma ; wikibase:lexicalCategory ?cat . FILTER(STRLEN(?lemma) >= 5 && STRLEN(?lemma) <= 9) } LIMIT 20000''')))["results"]["bindings"]
    ok28 = lambda w: re.fullmatch(f"[{LET28}]{{5}}", w) is not None
    lem, accepted = {}, set()
    for b in lex:
        cat = b["cat"]["value"].rsplit("/", 1)[1]
        n = norm(b["lemma"]["value"])
        if ok28(n) and cat in ("Q24905", "Q1084", "Q34698", "Q380057"):
            accepted.add(n)
            if cat in ("Q1084", "Q34698") and n not in lem: lem[n] = (strip(b["lemma"]["value"]), b["l"]["value"].rsplit("/", 1)[1])
    dump("word5/lemmas.json", sorted(accepted))   # lemma level only: the base of the bundled starter's guesses
    if forms_path and os.path.exists(forms_path): accepted |= set(json.load(open(forms_path, encoding="utf-8")))
    else: print("WARNING: no forms file, accepted guesses are lemmas only")
    # frequency proxy: the word is an ar.wikipedia article (not a redirect) of at least 3000 bytes
    titles = sorted({v[0] for v in lem.values()})
    exists = {}
    for i in range(0, len(titles), 50):
        chunk = titles[i:i + 50]
        p = {"action": "query", "prop": "info", "titles": "|".join(chunk), "format": "json"}
        d = json.loads(net.get("https://ar.wikipedia.org/w/api.php?" + urllib.parse.urlencode(p)))["query"]
        for pg in d["pages"].values():
            if "missing" in pg or "redirect" in pg or pg.get("length", 0) < 3000: continue
            exists[pg["title"]] = pg["length"]
    BLOCK5 = {"تكفير", "تشييع", "جنازه", "مذبحه", "مقبره", "عقوبه", "وسواس", "خازوق", "مسحور", "شعوذه", "جريمه", "تدخين", "عنوسه", "كنيسه", "تابين", "ناووس", "مناضل", "ترهيب", "مقاتل", "تشهير", "تجريف", "تغريب", "علمنه", "عقيده", "تاويل", "فلسفه", "شفاعه", "تعذيب", "تجريم", "سياسه", "ضريبه", "تخريف", "تضليل", "بوليس", "سينما", "مسكنه", "مجهول", "مدسوس", "تشييط", "سياسي", "حكومه", "وزاره", "دبابه", "ذخيره", "طاعون", "تاميم", "ثالوث", "تعزير", "تعزيه", "عداوه", "تهجير", "سفاره", "تاييد", "تجنيد", "تسليح", "مقلاع", "صاروخ", "ابليس", "شيطان", "افيون", "خنزير", "اعدام", "احليل", "غرمول", "زانيه", "فاحشه", "مومسه", "مخنوث", "منكوح", "جاسوس", "هرطقه", "زندقه", "عفريت", "كابوس", "مخمور", "مدمنه", "مجذوم", "اسهال", "بقشيش", "بخشيش", "شيطنه", "هلوسه", "سفسطه", "مجذور", "مجدور", "قرصنه", "ثفروق", "غرنوق", "مسخره", "ساخره", "داعره", "عاهره", "خمره", "سكران", "سكيره", "نحوسه", "ملحمه", "معركه", "مقتله", "مقتول", "مجزره", "مشنقه", "قذيفه", "مقصله", "صليبي", "يسوعي", "انجيل", "اقنوم", "كهنوت", "اصحاح", "تثليث", "تنصير", "تعميد", "اسلمه", "عنعنه"}
    ans, aseen = [], set()
    for t, (ar, en, rows) in T.items():
        for i, (a, e) in enumerate(rows):
            n = norm(a)
            if " " in a or not ok28(n) or n in aseen or n in BLOCK5 or n.startswith("ال"): continue
            aseen.add(n); lv = level(i, len(rows))
            ans.append({"word": n, "display": strip(a), "en": e, "category": t, "difficulty": lv, "age": "kids" if lv == 1 else "all", "source": "curated", "verified": "curated"})
    for w, (disp, lid) in sorted(lem.items()):
        if w in aseen or w in BLOCK5 or w.startswith("ال") or disp not in exists: continue
        aseen.add(w)
        ans.append({"word": w, "display": disp, "en": "", "category": "dictionary", "difficulty": 2 if exists[disp] >= 15000 else 3, "age": "all",
                    "source": "https://www.wikidata.org/wiki/Lexeme:" + lid, "verified": True})
    for w in list(accepted):
        if w in BLOCK5: accepted.discard(w)
    accepted |= {a["word"] for a in ans}
    dump("word5/answers.json", ans, indent=0)
    dump("word5/accepted.json", sorted(accepted))
    print("word5 answers", len(ans), "of which curated", sum(1 for a in ans if a["source"] == "curated"), "accepted", len(accepted))

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
