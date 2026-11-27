"""Builds the word-type data: odd-one topics, charades words, khanat letters, word5 lists, missing-word items and the
letter-hive index. Sources: Wikidata (cached), the verified quiz bank, assets/shared/{wisdom,verses}.json.
Run: SSL_CERT_FILE=<ca bundle> python3 -I games/tools/build_words.py"""
import sys, os, re, json, collections, hashlib
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import net, topics_fetch, geo_fetch, wordlists, urllib.parse
from common import rng

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
G = os.path.join(ROOT, "games")
LET28 = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

def strip(s): return re.sub(r"[ً-ٰٟـۖ-ۭ]", "", s)
def norm(s):
    s = strip(s)
    s = re.sub("[أإآٱ]", "ا", s).replace("ة", "ه").replace("ى", "ي")
    return s
def first_letter(s):
    n = norm(s).strip()
    if n.startswith("ال") and len(n) > 3: n = n[2:]
    return n[:1]

# not for a family game: alcohol, pork, gambling, sexual or violent items, other religions' clergy, and so on
STEMS = ("prostit", "gambl", "politic", "terror", "marijuana", "cannabis", "cocaine", "whisk", "alcohol", "lingerie", "bikini", "swimsuit", "underwear", "cigar", "tobacco")
BLOCK = ("wine", "beer", "whisk", "vodka", "rum", "gin", "champagne", "cocktail", "liquor", "brandy", "sake", "alcohol", "pig", "pork",
         "bacon", "ham", "boar", "swine", "hog", "bikini", "lingerie", "bra", "thong", "underwear", "swimsuit", "nude", "prostit",
         "priest", "monk", "nun", "pope", "rabbi", "bishop", "shaman", "pastor", "bull", "cock", "casino", "poker", "gambl", "cigar",
         "tobacco", "weapon", "gun", "rifle", "bomb", "sword", "knife", "soldier", "spy", "politic", "president", "king", "queen",
         "terror", "drug", "cannabis", "marijuana", "opium", "cocaine", "beggar", "slave", "executioner", "undertaker", "stripper",
         "model", "actor", "actress", "singer", "dancer", "magician", "witch", "hunter", "boxing", "wrestling", "bacchus", "sausage",
         "lobster", "shrimp", "crab", "oyster", "squid", "octopus", "eel", "clam", "mussel", "snail", "shark", "snake", "scorpion",
         "spider", "rat", "mouse", "cockroach", "louse", "flea", "tick", "maggot", "worm", "mosquito", "tuxedo", "kilt", "toga")

def clean(rows, single=False):
    out, seen = [], set()
    for r in rows:
        ar, en = r["ar"].strip(), r["en"].strip()
        if not re.fullmatch(r"[ء-يً-ْ ]{2,18}", ar) or len(ar.split()) > 2: continue
        if re.search(r"(iformes|idae|inae|ia)$", en) or len(en.split()) > 3 or "خنزير" in ar: continue
        if single and (len(ar.split()) > 1 or re.fullmatch(r"[A-Z][a-z]+ [a-z]+", en) and en.split()[1].endswith(("us", "is", "um", "ii", "ae"))): continue
        if re.search(r"[0-9()]", en) or len(en) > 28: continue
        toks = re.findall(r"[a-z]+", en.lower())
        if any(t.rstrip("s") in BLOCK or t in BLOCK or t.startswith(STEMS) for t in toks): continue
        k = norm(ar)
        if k in seen: continue
        seen.add(k); out.append({"q": r["q"], "ar": ar, "en": en, "sl": r["sl"]})
    return out

DISPLAY_EN = {"physician": "doctor", "association football": "football", "aircraft": "airplane", "fisher": "fisherman",
              "keffiyeh": "shemagh", "rapid transit": "metro", "date fruit": "dried date", "police officer": "police officer",
              "shooting sport": "shooting", "javelin throw": "javelin", "ice hockey": "hockey", "maize": "corn", "pea": "peas",
              "bean": "beans", "berry": "berries", "frying pan": "frying pan", "trousers": "trousers", "equestrianism": "horse riding",
              "star": "star", "kufi": "prayer cap", "thawb": "thobe", "agal": "agal", "buttermilk": "laban", "rain": "rain",
              "samosa": "samosa", "eggplant": "eggplant"}

def verify_pairs(pairs):
    """pairs: list of (ar, en). Returns {(ar, en): (qid, sitelinks)} for pairs where ONE Wikidata item has exactly these labels."""
    found = {}
    for i in range(0, len(pairs), 50):
        chunk = pairs[i:i + 50]
        vals = " ".join(f'("{ar}"@ar "{en}"@en)' for ar, en in chunk)
        q = f"SELECT ?ar ?en ?x ?sl WHERE {{ VALUES (?ar ?en) {{ {vals} }} ?x rdfs:label|skos:altLabel ?ar . ?x rdfs:label ?en . ?x wikibase:sitelinks ?sl . FILTER(?sl > 3) }}"
        for b in net.sparql(q):
            k = (b["ar"]["value"], b["en"]["value"]); sl = int(b["sl"]["value"])
            if k not in found or sl > found[k][1]: found[k] = (b["x"]["value"].rsplit("/", 1)[1], sl)
    return found

def load_topics():
    T = {}
    for t in ("mammals", "birds"):
        try: T[t] = clean(topics_fetch.fetch(t), single=True)
        except Exception as e: print("no data for", t, str(e)[:60]); T[t] = []
    allpairs = []
    parsed = {}
    for t, (ar_t, en_t, text) in wordlists.L.items():
        lst = []
        for tok in text.split():
            ar, en = tok.split(":")
            lst.append((ar.replace("_", " "), en.replace("_", " ")))
        parsed[t] = lst; allpairs += lst
    found = verify_pairs(allpairs)
    for t, lst in parsed.items():
        rows, dropped = [], []
        for ar, en in lst:
            if (ar, en) in found:
                q, sl = found[(ar, en)]
                rows.append({"q": q, "ar": ar, "en": DISPLAY_EN.get(en, en), "sl": sl})
            else: dropped.append(ar)
        T[t] = clean(rows)
        print("list", t, "kept", len(T[t]), "of", len(lst), "dropped:", " ".join(dropped))
    return T

COMMON5 = {  # normalized word: (English, category, suits kids). Kept only if the word is a Wikidata Arabic lexeme.
 "ابريق": ("jug", "things", 1), "اسبوع": ("week", "time", 1), "اسطبل": ("stable", "places", 1), "اسفنج": ("sponge", "things", 1),
 "اقليم": ("region", "places", 0), "انسان": ("human", "people", 1), "بازار": ("bazaar", "places", 0), "بالون": ("balloon", "things", 1),
 "بحيره": ("lake", "nature", 1), "بوابه": ("gate", "places", 1), "بيانو": ("piano", "things", 0), "تاريخ": ("history", "school", 0),
 "تجربه": ("experiment", "school", 0), "حاسوب": ("computer", "things", 1), "حافله": ("bus", "vehicles", 1), "حديقه": ("garden", "places", 1),
 "حقيبه": ("bag", "things", 1), "حكايه": ("story", "school", 1), "حلزون": ("snail", "animals", 1), "جزيره": ("island", "nature", 1),
 "جامعه": ("university", "places", 0), "جاموس": ("buffalo", "animals", 0), "جرجير": ("arugula", "food", 0), "دقيقه": ("minute", "time", 1),
 "دلفين": ("dolphin", "animals", 1), "دينار": ("dinar", "things", 0), "ديوان": ("divan", "things", 0), "راديو": ("radio", "things", 1),
 "رساله": ("letter", "things", 1), "رمضان": ("Ramadan", "time", 1), "زرافه": ("giraffe", "animals", 1), "زلزله": ("earthquake", "nature", 0),
 "زيتون": ("olive", "food", 1), "سجاده": ("prayer rug", "things", 1), "ستاره": ("curtain", "things", 1), "سفينه": ("ship", "vehicles", 1),
 "سياره": ("car", "vehicles", 1), "شاحنه": ("truck", "vehicles", 1), "صابون": ("soap", "things", 1), "صاعقه": ("lightning bolt", "nature", 0),
 "صنبور": ("tap", "things", 0), "ضيافه": ("hospitality", "culture", 0), "طاوله": ("table", "things", 1), "طياره": ("airplane", "vehicles", 1),
 "طبيعه": ("nature", "nature", 0), "طحينه": ("tahini", "food", 0), "طريقه": ("way", "school", 0), "عصفور": ("sparrow", "animals", 1),
 "عنقود": ("bunch of grapes", "food", 0), "فيضان": ("flood", "nature", 0), "قاموس": ("dictionary", "school", 0), "قافله": ("caravan", "culture", 0),
 "قصيده": ("poem", "culture", 0), "قلاده": ("necklace", "things", 0), "قنطره": ("bridge", "places", 0), "كتابه": ("writing", "school", 0),
 "كراسه": ("exercise book", "school", 1), "كزبره": ("coriander", "food", 0), "مدرسه": ("school", "places", 1), "مدينه": ("city", "places", 1),
 "مصباح": ("lamp", "things", 1), "مفتاح": ("key", "things", 1), "مكتبه": ("library", "places", 1), "ملعقه": ("spoon", "things", 1),
 "منديل": ("tissue", "things", 1), "منظار": ("binoculars", "things", 0), "منتزه": ("park", "places", 1), "مهندس": ("engineer", "jobs", 1),
 "ميزان": ("scale", "things", 0), "ملفوف": ("cabbage", "food", 0), "نتيجه": ("result", "school", 0), "نموذج": ("model", "school", 0),
 "نهايه": ("end", "school", 0), "هندسه": ("geometry", "school", 0), "وسيله": ("means", "school", 0), "وظيفه": ("job", "jobs", 0),
 "يقطين": ("pumpkin", "food", 0), "ينبوع": ("spring", "nature", 0), "مطبعه": ("printing press", "places", 0), "منضده": ("desk", "things", 1),
 "مروحه": ("fan", "things", 1), "مسمار": ("nail", "things", 1), "مطرقه": ("hammer", "things", 1), "مرحله": ("stage", "school", 0),
 "محفظه": ("wallet", "things", 1), "مصروف": ("allowance", "things", 0), "مشروب": ("drink", "food", 0), "محصول": ("crop", "nature", 0),
 "مختبر": ("laboratory", "places", 0), "مخترع": ("inventor", "jobs", 0), "مجموع": ("total", "school", 0), "مكنسه": ("broom", "things", 1),
 "مقياس": ("measure", "school", 0), "مسلسل": ("series", "things", 0), "غساله": ("washing machine", "things", 1), "ثلاجه": ("refrigerator", "things", 1),
 "بقاله": ("grocery", "places", 1), "بندقه": ("hazelnut", "food", 0), "بصيره": ("insight", "culture", 0), "بلوره": ("crystal", "things", 0),
 "جريده": ("newspaper", "things", 1), "جراحه": ("surgery", "school", 0), "حمامه": ("dove", "animals", 1), "خريطه": ("map", "school", 1),
 "خزانه": ("cupboard", "things", 1), "دراجه": ("bicycle", "vehicles", 1), "سلسله": ("chain", "things", 0), "سبوره": ("blackboard", "school", 1),
 "فراشه": ("butterfly", "animals", 1)}

def difficulty(i, n): return 1 if i < n * .35 else (2 if i < n * .7 else 3)
src = lambda q: "https://www.wikidata.org/wiki/" + q

def main():
    T = load_topics()
    print({k: len(v) for k, v in T.items()})
    # ---- odd-one topics
    TITLES = {k: v[:2] for k, v in topics_fetch.TOPICS.items()}
    TITLES.update({k: v[:2] for k, v in wordlists.L.items()})
    odd = []
    for t, rows in T.items():
        if t in ("mammals", "birds") or len(rows) < 12: continue
        rows = rows[:40]
        odd.append({"id": t, "ar": TITLES[t][0], "en": TITLES[t][1], "age": "all", "words": [
            {"ar": r["ar"], "en": r["en"], "difficulty": difficulty(i, len(rows)), "source": src(r["q"])} for i, r in enumerate(rows)]})
    json.dump({"topics": odd}, open(f"{G}/odd-one/topics.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    # ---- charades words (things easy to act or describe)
    CH = {"wild_animals": "animals", "birds_hand": "animals", "sea_and_reptiles": "animals", "animals_farm": "animals", "fruits": "food", "vegetables": "food", "food": "food",
          "sports": "sports", "jobs": "jobs", "vehicles": "vehicles", "clothes": "clothes", "furniture": "things", "tools": "things",
          "school": "things", "places": "places", "nature": "things"}
    words, seen = [], set()
    for t, cat in CH.items():
        rows = T.get(t, [])[:40]
        for i, r in enumerate(rows):
            if norm(r["ar"]) in seen or r["q"] in seen: continue
            seen.add(norm(r["ar"])); seen.add(r["q"])
            d = difficulty(i, len(rows))
            words.append({"id": f"ch-{r['q']}", "ar": r["ar"], "en": r["en"], "category": cat, "difficulty": d,
                          "age": "kids" if d == 1 else "all", "modes": ["act", "describe"] if t not in ("jobs", "sports") else ["act", "describe"],
                          "source": src(r["q"]), "verified": True})
    json.dump(words, open(f"{G}/charades/words.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    # ---- khanat samples per letter and box
    countries = [(v["ar"], v["en"], v["q"]) for v in geo_fetch.countries().values() if v["en"] not in ("Israel", "Taiwan")]
    BOX = {"animal": ["mammals", "birds", "animals_farm", "wild_animals", "birds_hand", "sea_and_reptiles"], "plant": ["fruits", "vegetables"],
           "object": ["furniture", "tools", "vehicles", "clothes", "school"], "food": ["food", "drinks"], "job": ["jobs"],
           "saudi_city": ["saudi_cities"]}
    LETTERS = "ابتجحخدذرزسشصطعغفقكلمنهوي"
    samples = {l: {} for l in LETTERS}
    def add(box, ar, en, q):
        l = first_letter(ar)
        if l in samples:
            lst = samples[l].setdefault(box, [])
            if len(lst) < 6 and all(x["ar"] != ar for x in lst): lst.append({"ar": ar, "en": en, "source": src(q)})
    for box, ts in BOX.items():
        for t in ts:
            for r in T.get(t, []): add(box, r["ar"], r["en"], r["q"])
    for ar, en, q in countries: add("country", ar, en, q)
    json.dump({"letters": list(LETTERS), "rare_letters": list("ثضظ"), "boxes": ["animal", "plant", "object", "country", "name"],
               "extra_boxes": ["food", "job", "saudi_city"], "samples": samples},
              open(f"{G}/khanat/letters.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    cov = {b: sum(1 for l in LETTERS if samples[l].get(b)) for b in list(BOX) + ["country"]}
    print("khanat coverage (letters with a sample)", cov)
    # ---- word5
    lex = json.loads(net.get("https://query.wikidata.org/sparql?format=json&query=" + __import__("urllib.parse").parse.quote(
        '''SELECT ?l ?lemma ?cat WHERE { ?l a ontolex:LexicalEntry ; dct:language wd:Q13955 ; wikibase:lemma ?lemma ; wikibase:lexicalCategory ?cat . FILTER(STRLEN(?lemma) >= 5 && STRLEN(?lemma) <= 9) } LIMIT 20000''')))["results"]["bindings"]
    ok28 = lambda w: re.fullmatch(f"[{LET28}]{{5}}", w) is not None
    accepted = {norm(b["lemma"]["value"]) for b in lex if b["cat"]["value"].rsplit("/", 1)[1] in ("Q24905", "Q1084", "Q34698")}
    accepted = {w for w in accepted if ok28(w)}
    lexid = {}
    for b in lex:
        n = norm(b["lemma"]["value"])
        if b["cat"]["value"].rsplit("/", 1)[1] in ("Q24905", "Q1084", "Q34698") and n not in lexid:
            lexid[n] = (strip(b["lemma"]["value"]), b["l"]["value"].rsplit("/", 1)[1])
    ans, aseen = [], set()
    for t, rows in T.items():
        if t in ("saudi_cities", "jobs", "mammals", "birds"): continue
        for i, r in enumerate(rows):
            w = norm(r["ar"])
            accepted.add(w) if ok28(w) else None
            if " " in r["ar"] or not ok28(w) or w in aseen or r["sl"] < 60: continue
            aseen.add(w)
            ans.append({"word": w, "display": strip(r["ar"]), "en": r["en"], "category": t, "difficulty": 1 if r["sl"] > 90 else 2,
                        "age": "kids" if r["sl"] > 90 else "all", "source": src(r["q"]), "verified": True})
    for w, (en, cat, kids) in COMMON5.items():
        if w in lexid and w not in aseen and ok28(w):
            aseen.add(w)
            ans.append({"word": w, "display": lexid[w][0], "en": en, "category": cat, "difficulty": 1 if kids else 2,
                        "age": "kids" if kids else "all", "source": "https://www.wikidata.org/wiki/Lexeme:" + lexid[w][1], "verified": True})
    accepted |= {a["word"] for a in ans}
    json.dump(ans, open(f"{G}/word5/answers.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    json.dump(sorted(accepted), open(f"{G}/word5/accepted.json", "w", encoding="utf-8"), ensure_ascii=False)
    print("word5 answers", len(ans), "accepted", len(accepted))
    # ---- missing-word + letter-hive from the bank and the repo's own checked files
    q = json.load(open(f"{G}/data/quiz.json", encoding="utf-8"))
    mw = []
    for it in q:
        if it["id"].startswith("geo-cap-"):
            m = re.fullmatch(r"ما عاصمة (.+)؟", it["question"]["ar"])
            if m: mw.append({"id": "mw-" + it["id"], "deck": "fact", "text": {"ar": f"عاصمة {m.group(1)} هي ___", "en": f"The capital of {it['question']['en'][len('What is the capital of '):-1]} is ___"},
                             "answer": it["answer"], "accept": [], "difficulty": it["difficulty"], "age": it["age"], "source": it["source"], "verified": True})
        if it["id"].startswith("el-sym-"):
            m = re.fullmatch(r"ما الرمز الكيميائي لعنصر (.+)؟", it["question"]["ar"])
            if m: mw.append({"id": "mw-" + it["id"], "deck": "fact", "text": {"ar": f"الرمز الكيميائي لعنصر {m.group(1)} هو ___", "en": f"The chemical symbol of {it['question']['en'][len('What is the chemical symbol of '):-1]} is ___"},
                             "answer": it["answer"], "accept": [], "difficulty": it["difficulty"], "age": it["age"], "source": it["source"], "verified": True})
    wis = json.load(open(f"{ROOT}/assets/shared/wisdom.json", encoding="utf-8"))
    for i, w in enumerate(wis):
        toks = w["text"].split()
        cand = [j for j, t in enumerate(toks) if len(strip(t).strip("،.؛")) >= 4]
        if len(toks) < 3 or not cand: continue
        j = cand[-1]
        word = toks[j].strip("،.؛")
        text = " ".join(toks[:j] + ["___" + (toks[j][len(word):] if toks[j] != word else "")] + toks[j + 1:])
        mw.append({"id": f"mw-wis-{i}", "deck": "saying", "text": {"ar": text, "en": ""}, "answer": {"ar": word, "en": ""},
                   "accept": [], "difficulty": 2, "age": "all", "source": ["repo:assets/shared/wisdom.json"], "verified": "repo"})
    json.dump(mw, open(f"{G}/missing-word/items.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    print("missing-word", collections.Counter(x["deck"] for x in mw))
    # letter-hive index: normalized first letter of each quiz answer (never of a number or a Latin string)
    idx = collections.defaultdict(list)
    for it in q:
        if it["type"] != "quiz" or not re.match(r"[ء-ي]", it["answer"]["ar"]): continue
        if it["id"].startswith("el-sym-"): continue
        l = first_letter(it["answer"]["ar"])
        if l in LET28: idx[l].append(it["id"])
    letters = [l for l in LET28 if len(idx[l]) >= 8]
    json.dump({"letters": letters, "index": {l: idx[l] for l in letters}, "coverage": {l: len(idx[l]) for l in LET28}},
              open(f"{G}/letter-hive/index.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    json.dump(letters, open(f"{G}/letter-hive/letters.json", "w", encoding="utf-8"), ensure_ascii=False)
    print("letter-hive letters with >= 8 items:", len(letters), "of 28; counts", {l: len(idx[l]) for l in LET28})

if __name__ == "__main__":
    main()
