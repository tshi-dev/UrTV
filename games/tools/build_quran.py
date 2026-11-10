"""Quran structure: api.alquran.cloud (Tanzil text) for names and order; every number is also checked against the
surah's ar.wikipedia intro, and kept only when both agree."""
import sys, os, re, json
sys.path.insert(0, os.path.dirname(__file__))
import net
from common import item, pick_wrong

API = "https://api.alquran.cloud/v1/surah"

def strip(s): return re.sub(r"[ً-ٰٟۖ-ۭـ]", "", s).replace("ٱ", "ا")

def build(stats):
    data = json.loads(net.get("https://api.alquran.cloud/v1/surah"))["data"]
    assert len(data) == 114 and sum(s["numberOfAyahs"] for s in data) == 6236
    ar = {s["number"]: strip(s["name"]).replace("سورة ", "", 1).strip() for s in data}
    en = {s["number"]: s["englishName"] for s in data}
    ayahs = {s["number"]: s["numberOfAyahs"] for s in data}
    titles = {n: "سورة " + ar[n] for n in ar}
    intro = net.wiki_intro("ar", list(titles.values()))
    checked = set()
    for n in ar:
        nums = [int(x) for x in re.findall(r"(?:آيات(?:ها)?|ولها|آياتها)\s*[:]?\s*(\d+)", intro.get(titles[n], ""))]
        if ayahs[n] in nums: checked.add(n)
    stats["ayah_crosschecked"] = len(checked)
    show = {n: ("آل عمران" if ar[n] == "ال عمران" else ar[n]) for n in ar}
    wiki = lambda n: "https://ar.wikipedia.org/wiki/" + titles[n].replace(" ", "_")
    pool = [(show[n], en[n]) for n in ar]
    famous = {1, 2, 3, 4, 12, 18, 19, 36, 55, 67, 112, 113, 114, 108, 110, 109, 111}
    out = {"quiz": [], "truefalse": []}
    for n in range(1, 114):
        if True:
            a = (show[n + 1], en[n + 1])
            out["quiz"].append(item(f"qu-next-{n}", "quiz", "islamic", 2 if n in famous else 3, "all",
                f"أي سورة تأتي بعد سورة {show[n]}؟", f"Which surah comes right after Surah {en[n]}?", a,
                pick_wrong(f"qn{n}", pool, a), [API]))
    for n in sorted(checked):
        if n in famous or ayahs[n] >= 100 or n >= 100:
            wrong = sorted({max(3, ayahs[n] + d) for d in (3, 5, -3, 8, -4, 12, 6, 10)} - {ayahs[n]})
            from common import rng
            r = rng(f"qc{n}"); r.shuffle(wrong)
            if len(wrong) < 3: continue
            out["quiz"].append(item(f"qu-cnt-{n}", "quiz", "islamic", 3 if ayahs[n] > 10 else 2, "all",
                f"كم عدد آيات سورة {show[n]} برواية حفص؟", f"How many verses does Surah {en[n]} have (Hafs count)?",
                (str(ayahs[n]), str(ayahs[n])), [{"ar": str(w), "en": str(w)} for w in wrong[:3]],
                [f"{API}/{n}", wiki(n)], numeric=True))
    # whole-book facts, two sources: the API (114 surahs, 6236 verses) and ar.wikipedia
    out["quiz"].append(item("qu-114", "quiz", "islamic", 1, "kids", "كم عدد سور القرآن الكريم؟",
        "How many surahs are in the Quran?", ("114", "114"),
        [{"ar": x, "en": x} for x in ("99", "110", "120")], [API, "https://ar.wikipedia.org/wiki/قائمة_سور_القرآن"], numeric=True))
    return out
