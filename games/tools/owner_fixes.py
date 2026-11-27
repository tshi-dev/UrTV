"""The owner's decisions of 2026-11-27 (content review, docs/research/content-review/SUMMARY.md on work/44-content-review),
applied to the built data so a rebuild cannot undo them. build_all.py calls it last; games/check.py fails if one is missing.
   python3 -I games/tools/owner_fixes.py [GAMES_DIR]      apply in place (idempotent)
Every changed fact carries its source here."""
import json, os, re, sys

# 4. No Palestine and no Israel in any game or quiz (the owner); games/check.py BANNED holds every spelling.
# 5. The quiz: items whose answer is not settled are removed ...
REMOVE_QUIZ = {
    "geo-cap-Q983", "geo-rev-Q983",   # Equatorial Guinea: the capital is moving from Malabo to Ciudad de la Paz
    "geo-cap-Q697", "geo-rev-Q697",   # Nauru has no official capital (en.wikipedia.org/wiki/Yaren_District)
    "geo-cap-Q252",                   # Indonesia: Jakarta and Nusantara both listed (wikidata Q252 P36)
    "geo-lang-Q115",                  # Ethiopia: five federal working languages since 2020 (en.wikipedia.org/wiki/Languages_of_Ethiopia)
    "geo-lang-Q1032",                 # Niger: Hausa national, French working language since 2025 (en.wikipedia.org/wiki/Languages_of_Niger)
    "an-class-Q152",                  # «حيوان سمكة» -> «أسماك»: the answer is in the question
}
# ... a wrong option that is also right is replaced ...
OPTION = {
    "hv-dhuhr": ("الجمعة", ("الوتر", "Witr")),   # Jumu'ah is also prayed between Fajr and Asr (en.wikipedia.org/wiki/Friday_prayer)
    "geo-cont-Q29": ("أفريقيا", ("أوقيانوسيا", "Oceania")),   # Spain: Ceuta, Melilla, Canaries (wikidata Q29 P30)
    "geo-cont-Q142": ("أفريقيا", ("أمريكا الجنوبية", "South America")),   # France: Réunion, Mayotte (wikidata Q142 P30; French Guiana is South America too)
    "geo-cont-Q38": ("أفريقيا", ("أوقيانوسيا", "Oceania")),   # Italy: Lampedusa (wikidata Q38 P30)
    "geo-cont-Q804": ("أمريكا الجنوبية", ("أفريقيا", "Africa")),   # Panama (wikidata Q804 P30)
    "qu-next-55": ("الرحمن", ("الملك", "Al-Mulk")),   # the surah named in the question is not an option
}
# ... wrong labels, outdated names and wording, everywhere they appear (answers, options, questions, samples)
LABEL = {
    "البلد": ("فاليتا", "Valletta"),   # Malta's capital (ar.wikipedia.org/wiki/فاليتا)
    "بوليفار السيادي": ("بوليفار فنزويلي", "Venezuelan bolívar"),   # redenominated 2021; the name stays true (en.wikipedia.org/wiki/Venezuelan_bolívar)
    "لبلانة": ("ليوبليانا", None), "إسكوبية": ("سكوبيه", None),
    "لو ملداوي": ("ليو مولدوفي", None), "تنك قزاقستاني": ("تينغ كازاخستاني", None), "كوردبا نيكاراغوا": ("كوردوبا نيكاراغوي", None),
    "جوردة هايتية": ("غورد هايتي", None), "بوتسوانا بولا": ("بولا بوتسواني", None), "بانجا تونجي": ("بانغا تونغي", None),
    "مثقال موزنبيقي": ("مثقال موزمبيقي", None), "مونت بلانك": ("مون بلان", None),
}
QUESTION = {
    "hi-giza": ("في أي دولة تقع الهرم الأكبر في الجيزة؟", "في أي دولة يقع الهرم الأكبر في الجيزة؟"),
    "sg-oryx": ("ذو القرنين", "ذي القرنين"),
    "sg-camel-hump": (" (الهجين)", ""),
    "geo-lang-Q819": None,   # answer «لاو» -> «اللاوية», below
}
ANSWER = {"geo-lang-Q819": ("اللاوية", None)}
KHWARIZMI = {"question": {"ar": "من العالِم الذي اشتُقّت من اسمه كلمة «خوارزمية»؟",
                          "en": "Which scholar's name gave us the word «algorithm»?"},
             "source": ["https://en.wikipedia.org/wiki/Algorithm"], "evidence": ["al-Khwarizmi"]}
EN = {"How many sides does a octagon have?": "How many sides does an octagon have?"}

# the family games
KHANAT = {("م", "country", "ألمانيا"): ("مالي", "Mali"), ("و", "object", "ألوان"): ("وعاء", "bowl"),
          ("ن", "saudi_city", "نيوم"): ("النماص", "Al-Namas")}
ODD_ONE_OUT = {"family": {"صديق", "جار", "ضيف", "معلم", "عريس", "عروس"}, "clothes": {"نظارة", "ساعة", "كم"},
               "farm": {"قطة", "كلب", "حمامة"}, "dishes": {"برياني"}}
ODD_ONE_ADD = {   # so each topic keeps its 20 words (games/check_words.py)
    "family": [("حفيدة", "granddaughter", 2), ("زوج", "husband", 2), ("زوجة", "wife", 2), ("ابن الخال", "cousin (maternal)", 3),
               ("بنت العم", "cousin (paternal)", 3)],
    "farm": [("جاموس", "buffalo", 3), ("تيس", "billy goat", 3)]}
GLOSS = {"بلبل": "bulbul", "نسر": "vulture", "نقانق": "sausages", "صرصور": "cockroach", "برج المملكة": "Kingdom Centre"}
WORD5_DROP = {"مشرحة", "مشيمة", "طاغية", "إهانة", "أمركة", "مسكون", "تخدير"}
WORD5_HARD = {"زغاية", "ساجور", "تناضح", "كناشة", "مشاية", "ملحون", "مرسين", "مقروض"}
SAYING = {"اسأل مجرب ولا تسأل": ("طبيبا", "حكيما")}


def elem(name):
    return name if name.startswith("ال") or " " in name else "ال" + name


def fix_quiz(items):
    out = []
    for it in items:
        i = it["id"]
        if i in REMOVE_QUIZ:
            continue
        for fld in ("answer",):
            v = LABEL.get(it[fld]["ar"])
            if v:
                it[fld]["ar"] = v[0]
                it[fld]["en"] = v[1] or it[fld]["en"]
        for w in it["wrong"]:
            v = LABEL.get(w["ar"])
            if v:
                w["ar"], w["en"] = v[0], v[1] or w["en"]
        for old, new in LABEL.items():
            if it["question"]["ar"].startswith(old + " "):
                it["question"]["ar"] = new[0] + it["question"]["ar"][len(old):]
                if new[1]:
                    it["question"]["en"] = it["question"]["en"].replace(it["question"]["en"].split(" is the capital")[0], new[1], 1)
        if i in OPTION:
            old, (ar, en) = OPTION[i]
            for w in it["wrong"]:
                if w["ar"] == old:
                    w["ar"], w["en"] = ar, en
        if QUESTION.get(i):
            a, b = QUESTION[i]
            it["question"]["ar"] = it["question"]["ar"].replace(a, b)
        if i in ANSWER:
            it["answer"]["ar"] = ANSWER[i][0]
        if i == "hv-khwarizmi":
            it.update(json.loads(json.dumps(KHWARIZMI)))
        if it["question"]["en"] in EN:
            it["question"]["en"] = EN[it["question"]["en"]]
        if i.startswith("el-sym-"):
            name = it["question"]["ar"].split("لعنصر ", 1)[1].rstrip("؟")
            it["question"]["ar"] = it["question"]["ar"].replace("لعنصر " + name, "لعنصر " + elem(name))
        if i.startswith(("el-bysym-", "el-name-")):
            it["answer"]["ar"] = elem(it["answer"]["ar"])
            for w in it["wrong"]:
                w["ar"] = elem(w["ar"])
        if i.startswith("an-class-"):
            m = re.match("إلى أي مجموعة ينتمي حيوان (.+)؟$", it["question"]["ar"])
            if m:
                it["question"]["ar"] = f"إلى أي مجموعة من الحيوانات ينتمي {elem(m.group(1))}؟"
        out.append(it)
    return out


def load(p):
    return json.load(open(p, encoding="utf-8"))


def save(p, d, indent=0):
    """indent 0 as the builders write; a final newline only where the file had one"""
    nl = open(p, encoding="utf-8").read().endswith("\n")
    with open(p, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=0)
        if nl:
            f.write("\n")


def apply(g):
    p = os.path.join(g, "data", "quiz.json")
    quiz = fix_quiz(load(p))
    save(p, quiz, 0)
    ids = {x["id"] for x in quiz}
    p = os.path.join(g, "letter-hive", "index.json")
    hive = load(p)
    for key in ("index", "en_index"):
        hive[key] = {L: [i for i in v if i in ids] for L, v in hive[key].items()}
    save(p, hive, 1)
    p = os.path.join(g, "khanat", "letters.json")
    kh = load(p)
    for (L, box, old), (ar, en) in KHANAT.items():
        for s in kh["samples"].get(L, {}).get(box, []):
            if s["ar"] == old:
                s["ar"], s["en"] = ar, en
    save(p, kh, 1)
    p = os.path.join(g, "odd-one", "topics.json")
    oo = load(p)
    for t in oo["topics"]:
        t["words"] = [w for w in t["words"] if w["ar"] not in ODD_ONE_OUT.get(t["id"], set())]
        have = {w["ar"] for w in t["words"]}
        t["words"] += [{"ar": ar, "en": en, "difficulty": d, "source": "curated"} for ar, en, d in ODD_ONE_ADD.get(t["id"], []) if ar not in have]
        for w in t["words"]:
            w["en"] = GLOSS.get(w["ar"], w["en"])
    save(p, oo, 1)
    p = os.path.join(g, "charades", "words.json")
    ch = load(p)
    for w in ch:
        w["en"] = GLOSS.get(w["ar"], w["en"])
    save(p, ch, 1)
    p = os.path.join(g, "word5", "answers.json")
    w5 = [w for w in load(p) if w["display"] not in WORD5_DROP]
    for w in w5:
        if w["display"] in WORD5_HARD:
            w["difficulty"] = 3
        w["en"] = GLOSS.get(w["display"], w["en"])
    save(p, w5, 1)
    p = os.path.join(g, "missing-word", "items.json")
    mw = load(p)
    for x in mw:
        for start, (old, new) in SAYING.items():
            if x["text"]["ar"].startswith(start) and x["answer"]["ar"] == old:
                x["answer"]["ar"] = new
        v = LABEL.get(x["answer"]["ar"])
        if v:
            x["answer"]["ar"] = v[0]
        m = re.match("(الرمز الكيميائي لعنصر )(.+?)( هو ___)$", x["text"]["ar"])
        if m:
            x["text"]["ar"] = m.group(1) + elem(m.group(2)) + m.group(3)
    mw = [x for x in mw if x["id"][3:] not in REMOVE_QUIZ]
    save(p, mw, 1)


def missing(g):
    """What check.py fails on: a decision that is not in the data."""
    quiz = {x["id"]: x for x in load(os.path.join(g, "data", "quiz.json"))}
    out = [i for i in REMOVE_QUIZ if i in quiz]
    out += [i for i, (old, _) in OPTION.items() if i in quiz and any(w["ar"] == old for w in quiz[i]["wrong"])]
    out += [f"label {k}" for x in quiz.values() for k in LABEL
            if k in (x["answer"]["ar"], *(w["ar"] for w in x["wrong"]))]
    kh = load(os.path.join(g, "khanat", "letters.json"))["samples"]
    out += [f"khanat {o}" for (L, b, o) in KHANAT if any(s["ar"] == o for s in kh.get(L, {}).get(b, []))]
    oo = {t["id"]: t for t in load(os.path.join(g, "odd-one", "topics.json"))["topics"]}
    out += [f"odd-one {t}" for t, ws in ODD_ONE_OUT.items() if t in oo and any(w["ar"] in ws for w in oo[t]["words"])]
    out += [f"word5 {w['display']}" for w in load(os.path.join(g, "word5", "answers.json")) if w["display"] in WORD5_DROP]
    return out


if __name__ == "__main__":
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..")
    apply(root)
    print("owner fixes applied; left:", missing(root))
