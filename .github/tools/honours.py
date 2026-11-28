#!/usr/bin/env python3
"""4.5 honours, weekly (docs/HONOURS-STUDY.md): each league's champions season by season, and its top scorers, for the league and club pages.

  champions/<leagueId>.json  every season we can name: our archive's champion (sports/<id>.json "champs", computed from the games), Wikidata's
                             winner (P1346, CC0), and for a season Wikidata has no winner of, its Wikipedia article's infobox (CC BY-SA 4.0).
                             Two sources that name a season must agree, or the season is left out and written to the report. "complete" is
                             true only when every season from the first is named (no gap, no disagreement): only then does "titles" (a club's
                             count) mean anything, and the page shows a count only then.
  scorers/<leagueId>.json    (4.4: a lone scorer's "team", the archive's club id, when the English article names his club and it is one club of
                             that season's table in the archive; else none)
  scorers/<leagueId>.json    the top scorer(s) of the last 8 seasons: Wikidata's P3279 (with its goals), the season article's infobox (ar
                             «هداف الدوري», en "league topscorer"). Published only when every source that has the season agrees on the number
                             of players and their goals, or one source alone has a plain field (names and a number). CC BY-SA 4.0, each
                             season with its articles' addresses.
  report.json                the seasons left out and why, for a person (never shown on a screen).
  LICENSE.md                 the terms: champions CC BY 4.0 (ours) with Wikidata's CC0, scorers CC BY-SA 4.0 (Wikipedia's).

    python3 .github/tools/honours.py --archive DIR --prev DIR --out DIR

The archive DIR holds sports/<id>.json and sports/<id>/<season>.json (the match archive's own files). A source that does not answer keeps
the previous file: a league's file is never emptied by a failed run. Wikidata through its API (the query service refuses a busy client).
"""
import argparse
import datetime as dt
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from honours_study import same_club, season_keys, words   # noqa: E402

V = 1
UA = "UrTV-honours/1.0 (https://github.com/tshi-dev/UrTV; weekly, a few hundred requests)"
SCORER_SEASONS = 8
PACE = 1.5   # seconds between two requests to Wikimedia (it answers a fast client 429)
# TheSportsDB's league id -> its Wikidata item (the item every season item names with P3450)
LEAGUES = {"4328": "Q9448", "4331": "Q82595", "4332": "Q15804", "4334": "Q13394", "4335": "Q324867", "4337": "Q167541", "4339": "Q485568",
           "4344": "Q182994", "4346": "Q18543", "4480": "Q18756", "4483": "Q483794", "4484": "Q212412", "4490": "Q15980635", "4520": "Q570246",
           "4663": "Q60681", "4668": "Q255633", "4678": "Q5334350", "4719": "Q193041", "4829": "Q680619", "5056": "Q248512", "5627": "Q2656109",
           "5649": "Q1187210"}
WDAPI = "https://www.wikidata.org/w/api.php"
CHAMP_FIELD = {"ar": r"البطل|الفائز|حامل اللقب", "en": r"champions?|winners?"}
SCORER_FIELD = {"ar": r"هداف الدوري|هداف البطولة|هداف المسابقة|أفضل هداف|الهداف|الهدافون",
                "en": r"league[ _]topscorers?|top[ _]goal[ _]?scorers?|top[ _]?scorers?|topscorer"}
GOAL_WORD = r"(?:goals?|هدف(?:ًا|اً|ا)?|أهداف)"
UNWRAP = r"nowrap|nobr|small|صغير|لا لف"


# ---- the network (one place; the tests give their own)
def http(url, tries=6):
    for i in range(tries):
        try:
            return urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"}), timeout=90).read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 503) and i < tries - 1:
                wait = min(int(e.headers.get("retry-after") or 0), 120) + 5 * (i + 1)   # a «come back in 1000 s» is not waited for: the run fails, the files stay
                print(f"  {e.code}, again in {wait} s: {url[:80]}", flush=True)
                time.sleep(wait)
                continue
            raise
        except OSError:
            if i == tries - 1:
                raise
            time.sleep(5 * (i + 1))
    raise OSError("no answer: " + url)


def query_seasons(fetch):
    """every season item of our leagues: [{q, lg, start, end, win:[Q], ts:[(Q, goals)], ar, en}], from the Wikidata API (its search by statement,
    then the items): the query service limits a client to about one query a minute and refuses more for many minutes"""
    by_q = {}
    for lg, league in LEAGUES.items():
        j = json.loads(fetch(WDAPI + "?" + urllib.parse.urlencode({"action": "query", "list": "search", "srsearch": f"haswbstatement:P3450={league}",
                                                                   "srnamespace": 0, "srlimit": 500, "format": "json"})))
        for r in (j.get("query") or {}).get("search", []):
            if re.fullmatch(r"Q\d+", r.get("title", "")):
                by_q[r["title"]] = lg
        time.sleep(PACE)
    ents = {}
    qs = sorted(by_q)
    for i in range(0, len(qs), 50):
        j = json.loads(fetch(WDAPI + "?" + urllib.parse.urlencode({"action": "wbgetentities", "ids": "|".join(qs[i:i + 50]), "props": "claims|sitelinks|labels",
                                                                   "languages": "en",
                                                                   "sitefilter": "arwiki|enwiki", "format": "json"})))
        ents.update(j.get("entities") or {})
        time.sleep(PACE)
    return seasons_from_entities(ents, by_q)


def _value(snak):
    return ((snak or {}).get("datavalue") or {}).get("value") if (snak or {}).get("snaktype") == "value" else None


def dates_of_title(title):
    """a season item's title as (start, end) dates: «2008–09 …» or «2008-2009 …» a season over two years (August to May), «1977 …» one year
    (March to November); ("", "") when the title names no season"""
    m = re.search(r"(?<!\d)(1[89]\d\d|20\d\d)(?:\s*[–\-/]\s*(\d{4}|\d{2}))?(?!\d)", title or "")
    if not m:
        return "", ""
    y1 = int(m.group(1))
    if m.group(2):
        y2 = int(m.group(2)) if len(m.group(2)) == 4 else y1 // 100 * 100 + int(m.group(2))
        if y2 != y1 + 1:
            return "", ""
        return f"{y1}-08-01", f"{y2}-05-31"
    return f"{y1}-03-01", f"{y1}-11-30"


def seasons_from_entities(ents, by_q):
    out = []
    for q, e in ents.items():
        cl = e.get("claims") or {}
        lg = by_q.get(q)
        if not lg or LEAGUES[lg] not in {(_value(c.get("mainsnak")) or {}).get("id") for c in cl.get("P3450", [])}:
            continue

        def first_time(p):
            for c in cl.get(p, []):
                v = _value(c.get("mainsnak"))
                if v and v.get("time"):
                    return v["time"].lstrip("+")[:10]
            return ""
        win = []
        for c in cl.get("P1346", []):
            v = _value(c.get("mainsnak"))   # «unknown value» is no one
            if v and v.get("id") and v["id"] not in win and c.get("rank") != "deprecated":
                win.append(v["id"])
        ts = []
        for c in cl.get("P3279", []):
            v = _value(c.get("mainsnak"))
            if not v or not v.get("id") or c.get("rank") == "deprecated":
                continue
            amount = next((x.get("amount") for x in (_value(g) for g in (c.get("qualifiers") or {}).get("P1351", [])) if x and x.get("amount")), None)
            ts.append((v["id"], int(float(amount)) if amount and re.fullmatch(r"[+-]?\d+(\.0+)?", amount) else None))
        sl = e.get("sitelinks") or {}
        start, end = first_time("P580"), first_time("P582")
        if not start:   # most older season items have no start date: their English title says the season («2008–09 Süper Lig», «1977 Copa…»)
            start, end = dates_of_title((sl.get("enwiki") or {}).get("title") or ((e.get("labels") or {}).get("en") or {}).get("value", ""))
        out.append({"q": q, "lg": lg, "start": start, "end": end, "win": win, "ts": ts,
                    "ar": (sl.get("arwiki") or {}).get("title", ""), "en": (sl.get("enwiki") or {}).get("title", "")})
    return sorted(out, key=lambda s: (s["lg"], s["start"]))


def labels(fetch, qids):
    """{Q: {"en", "ar"}} (a club's Arabic label without «نادي»)"""
    out = {}
    qids = sorted(set(qids))
    for i in range(0, len(qids), 50):
        j = json.loads(fetch(WDAPI + "?" + urllib.parse.urlencode({"action": "wbgetentities", "ids": "|".join(qids[i:i + 50]), "props": "labels",
                                                                    "languages": "en|ar", "format": "json"})))
        for q, e in (j.get("entities") or {}).items():
            lab = e.get("labels") or {}
            out[q] = {"en": (lab.get("en") or {}).get("value", ""), "ar": ar_club((lab.get("ar") or {}).get("value", ""))}
        time.sleep(PACE)
    return out


def ar_labels_of_titles(fetch, titles):
    """{en article title: its item's Arabic label} (a player's name in Arabic)"""
    out = {}
    titles = sorted(set(titles))
    for i in range(0, len(titles), 50):
        j = json.loads(fetch(WDAPI + "?" + urllib.parse.urlencode({"action": "wbgetentities", "sites": "enwiki", "titles": "|".join(titles[i:i + 50]),
                                                                    "props": "labels|sitelinks", "languages": "ar", "sitefilter": "enwiki",
                                                                    "format": "json", "normalize": 1})))
        for e in (j.get("entities") or {}).values():
            t = ((e.get("sitelinks") or {}).get("enwiki") or {}).get("title")
            a = ((e.get("labels") or {}).get("ar") or {}).get("value", "")
            if t and a:
                out[t] = a
    return out


def wikitexts(fetch, wiki, titles):
    """{title: the article's wikitext (its first 20000 characters)}, 10 titles a request, 3 s apart"""
    out = {}
    titles = sorted(set(t for t in titles if t))
    for i in range(0, len(titles), 10):
        part = titles[i:i + 10]
        j = json.loads(fetch(f"https://{wiki}.wikipedia.org/w/api.php?" + urllib.parse.urlencode(
            {"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main", "titles": "|".join(part), "format": "json",
             "formatversion": 2, "redirects": 1})))
        back = {r["to"]: r["from"] for r in (j.get("query") or {}).get("redirects", [])}
        for n in (j.get("query") or {}).get("normalized", []):
            back.setdefault(n["to"], n["from"])
        for p in (j.get("query") or {}).get("pages", []):
            try:
                out[back.get(p["title"], p["title"])] = p["revisions"][0]["slots"]["main"]["content"][:20000]
            except (KeyError, IndexError):
                pass
        if i + 10 < len(titles):
            time.sleep(2 * PACE)
    return out


# ---- reading an infobox (no network: the tests read these)
def ar_club(s):
    return re.sub(r"^(?:نادي|النادي)\s+", "", (s or "").strip())


def field(wt, names):
    """the first infobox parameter called one of names, with a value: (name, its raw value) or (None, "")"""
    for m in re.finditer(r"\n\s*\|\s*([^=\n|]{2,40}?)\s*=\s*([^\n]*)", "\n" + (wt or "")):
        v = re.split(r"\s\|\s*[^=|\[\]{}<>]{2,40}?\s*=", m.group(2))[0]   # «| champions = | relegated = …» on one line: the next field is not the value
        if re.fullmatch(f"(?:{names})", m.group(1).strip(), re.I) and v.strip() and not v.strip().startswith("|"):
            return m.group(1).strip(), v
    return None, ""


def _plain(v):
    v = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>|<!--.*?-->", "", v, flags=re.S)
    for _ in range(6):   # templates from the inside out: the wrappers keep their text, the rest (flags, notes) goes
        v2 = re.sub(r"\{\{\s*(?:" + UNWRAP + r")\s*\|([^{}]*)\}\}", r"\1", v, flags=re.I)
        v2 = re.sub(r"\{\{[^{}]*\}\}", "", v2)
        if v2 == v:
            break
        v = v2
    return re.sub(r"<br\s*/?>|<[^>]+>|'''|''", " ", v)


def links(v):
    """the [[target|text]] of a value, outside parentheses (a club in brackets after a player is not a player): [(target, text)]"""
    v = _plain(v)
    depth, keep = 0, []
    for ch in v:
        depth += ch == "("
        if depth == 0:
            keep.append(ch)
        depth -= ch == ")" and depth > 0
    return [(m.group(1).strip(), (m.group(2) or m.group(1)).strip()) for m in re.finditer(r"\[\[([^|\]]+)(?:\|([^\]]+))?\]\]", "".join(keep))]


def scorer_value(v):
    """an infobox's top-scorer value -> {"names": [(target, text)], "goals": n} or None when it is not plain (no player, or two goal counts)"""
    if not v:
        return None
    p = _plain(v)
    nums = {int(n) for n in re.findall(r"(\d{1,3})\s*" + GOAL_WORD, p)} or {int(n) for n in re.findall(r"\(\s*(\d{1,3})\s*\)", p)}
    names = [x for x in links(v) if not re.search(r"\d{4}|F\.C\.|\bFC\b|نادي", x[0])]
    if not names or len(nums) != 1:
        return None
    return {"names": names, "goals": nums.pop()}


def scorer_club(v):
    """4.4 the club after a top scorer in an infobox, «[[Player]] ([[Club FC|Club]])»: [(target, text)] of the links inside the brackets"""
    p, out, depth, cur = _plain(v or ""), [], 0, []
    for ch in p:
        if ch == "(":
            depth += 1
            continue
        if ch == ")" and depth:
            depth -= 1
            if not depth:
                out += [(m.group(1).strip(), (m.group(2) or m.group(1)).strip()) for m in re.finditer(r"\[\[([^|\]]+)(?:\|([^\]]+))?\]\]", "".join(cur))]
                cur = []
            continue
        if depth:
            cur.append(ch)
    return out


def champion_value(v):
    """an infobox's champion value -> its club's name (the first link's text, else the plain text) or ''"""
    ls = links(v)
    if ls:
        return ls[0][1]
    t = re.sub(r"\s+", " ", re.sub(r"\(.*?\)|\[|\]", "", _plain(v))).strip()
    return t if 0 < len(t) < 60 and not re.search(r"[=|{}]", t) else ""


AR_STOP = {"نادي", "النادي", "سبورت", "الرياضي", "السعودي", "لكرة", "القدم", "إف", "سي", "اف", "إس"}


def ar_same(a, b):
    w = lambda s: {x for x in re.sub(r"[^؀-ۿ ]", " ", s or "").split() if x not in AR_STOP and len(x) > 1}
    return bool(w(a) & w(b))


# ---- a season's key, as the archive writes it
def season_key(start, end, archive_seasons):
    """a Wikidata season (start, end dates) as the archive's key: one the archive has when it matches, else «y-y+1» (a season over two
    years) or «y» (a season in one calendar year)"""
    if not start:
        return ""
    y = int(start[:4])
    own = str(y) if end and end[:4] == start[:4] else season_keys(start)[0]
    for k in [own] + season_keys(start) + [f"{y}-{y + 1}", str(y)]:
        if k in archive_seasons:
            return k
    return own


def season_over(it, today=None):
    """the season has ended: its end date passed, or with no end date it began more than 330 days ago"""
    today = today or dt.date.today()
    if it.get("end"):
        return it["end"] < today.isoformat()
    return bool(it.get("start")) and it["start"] < (today - dt.timedelta(days=330)).isoformat()


def year_of(s):
    return int(s[:4]) if re.match(r"\d{4}", s or "") else 0


# ---- a league's champions
def champions(lg, items, archive, lab, wp, names):
    """(the league's champions file body, its report lines). items: its Wikidata seasons; archive: its sports/<id>.json (or None);
    lab: Wikidata labels; wp: {wiki: {title: wikitext}}; names: {club id: (en, ar)} from the archive's tables"""
    undated = False   # an item the list cannot place (no date, or two items for one season): never complete
    arch = {c["s"]: c for c in ((archive or {}).get("champs") or [])}
    arch_seasons = set((archive or {}).get("seasons") or [])
    seasons, report = {}, []
    for it in items:
        s = season_key(it["start"], it["end"], arch_seasons)
        if not s:   # an item no date and no title places: the list cannot be called complete
            report.append({"lg": lg, "why": "a season item with no date", "item": it["q"]})
            undated = True
            continue
        if s in seasons:   # two items read as one season (a league that changed its calendar): the first kept, said so
            report.append({"lg": lg, "s": s, "why": "two Wikidata items for one season", "items": [seasons[s].get("q"), it["q"]]})
            undated = True   # which one is right is not known: the list is not called complete
            continue
        src, en, arn, wd = [], [], [], None
        wins = [w for w in it["win"] if w in lab]
        if len(wins) == 1:
            wd = wins[0]
            src.append("wikidata"); en.append(lab[wd]["en"]); arn.append(lab[wd]["ar"])
        elif not it["win"] and season_over(it):   # a season under way has no champion, whatever its article's infobox says
            for wiki in ("en", "ar"):
                v = champion_value(field(wp.get(wiki, {}).get(it[wiki]), CHAMP_FIELD[wiki])[1]) if it[wiki] else ""
                if v:
                    src.append("wikipedia-" + wiki)
                    (en if wiki == "en" else arn).append(v if wiki == "en" else ar_club(v))
        seasons[s] = {"s": s, "start": it["start"], "src": src, "en": [x for x in en if x], "ar": [x for x in arn if x], "wd": wd, "item": True, "q": it["q"],
                      "url": [f"https://{w}.wikipedia.org/wiki/" + urllib.parse.quote(it[w].replace(" ", "_")) for w in ("ar", "en") if it[w]]}
    for s, c in arch.items():
        e = seasons.setdefault(s, {"s": s, "start": "", "src": [], "en": [], "ar": [], "wd": None, "item": False, "url": []})
        e["src"].insert(0, "archive"); e["en"].insert(0, c.get("n") or ""); e["arch"] = c
        if c.get("ar"):
            e["ar"].insert(0, c["ar"])
    out, complete = [], True
    for s, e in sorted(seasons.items(), key=lambda kv: (year_of(kv[0]), kv[0]), reverse=True):
        en = [x for x in e["en"] if x]
        bad = next(((a, b) for i, a in enumerate(en) for b in en[i + 1:] if not same_club(a, b)), None)
        arn = [x for x in e["ar"] if x]
        bad = bad or next(((a, b) for i, a in enumerate(arn) for b in arn[i + 1:] if not ar_same(a, b)), None)
        if bad:
            report.append({"lg": lg, "s": s, "why": "sources disagree", "names": list(bad), "src": e["src"]})
            complete = False
            continue
        if not e["src"]:
            if e["item"]:
                report.append({"lg": lg, "s": s, "why": "no source names a champion"})
                complete = complete and year_of(s) > dt.date.today().year - 1   # the season under way is not a gap
            continue
        c = e.get("arch")
        cid = c["id"] if c else unique_id(en, names)
        row = {"s": s, "en": en[0] if en else "", "ar": (c or {}).get("ar") or (names.get(cid, ("", ""))[1] if cid else "") or (ar_club(arn[0]) if arn else ""),
               "src": "+".join(sorted(set(x.split("-")[0] for x in e["src"])))}
        if cid:
            row["id"] = cid
        if e["wd"]:
            row["wd"] = e["wd"]
        if e["url"] and any(x.startswith("wikipedia") for x in e["src"]):
            row["url"] = e["url"]
        for k in ("pts", "gap", "vs", "var", "vn", "score", "legs"):
            if c and k in c:
                row[k] = c[k]
        out.append(row)
    years = sorted({year_of(r["s"]) for r in out})
    if any(b - a > 1 for a, b in zip(years, years[1:])):
        complete = False
        report.append({"lg": lg, "why": "a gap in the seasons", "years": [f"{a}-{b}" for a, b in zip(years, years[1:]) if b - a > 1]})
    # a history, not the archive's last seasons: the oldest season named is Wikidata's first item of the league, and older than the archive's
    first_item = min((it["start"] for it in items if it["start"]), default="")
    oldest_arch = min((year_of(s) for s in arch_seasons), default=9999)
    if undated or not out or not first_item or year_of(out[-1]["s"]) > year_of(first_item) or year_of(out[-1]["s"]) >= oldest_arch:
        complete = False
    body = {"v": V, "id": lg, "complete": complete, "seasons": out}
    held = sorted({r["s"] for r in report if r.get("why") == "sources disagree"}, reverse=True)
    if held:
        body["held"] = held   # the page does not put the archive's champion back for these
    if out:
        body["first"] = out[-1]["s"]
    if complete:
        titles = {}
        for r in out:
            if r.get("id"):
                titles[r["id"]] = titles.get(r["id"], 0) + 1
        # a season whose champion has no id could be one of these clubs under an older name: that club's count is not said
        for r in out:
            if not r.get("id"):
                for cid in [c for c in titles if names.get(c) and r.get("en") and same_club(names[c][0], r["en"])]:
                    titles.pop(cid, None)
        body["titles"] = titles
        if any(not r.get("id") for r in out):   # a champion the archive does not know (a club of long ago): its club's count is still right
            body["unnamed"] = sum(1 for r in out if not r.get("id"))
    return body, report


def _jaccard(a, b):
    wa, wb = words(a), words(b)
    return len(wa & wb) / len(wa | wb) if wa and wb else 0.0


def unique_id(en_names, names):
    """the archive's club id whose name matches these names best: the clubs that share a word are scored by how much of their names they
    share («Real Madrid» is Real Madrid, not Real Betis or Real Sociedad); one club alone with the best score, at least half, else none"""
    scores = {}
    for cid, (n, _) in names.items():
        sc = max((_jaccard(n, x) for x in en_names if n and x), default=0.0)
        if sc > 0:
            scores[cid] = sc
    if not scores:
        return None
    best = max(scores.values())
    top = [cid for cid, sc in scores.items() if sc == best]
    return top[0] if len(top) == 1 and best >= 0.5 else None


# ---- a league's top scorers
def scorers(lg, items, archive, lab, wp, ar_of_title, names=None, season_clubs=None):
    """(the league's scorers file body, report lines): the last SCORER_SEASONS seasons that have begun. 4.4: a lone top scorer's club
    («team», the archive's id) when the English season article names one club after him and it is exactly one club of that season's table in
    our archive (season_clubs: {season: {club ids}}; names: {club id: (en, ar)}); else no team"""
    arch_seasons = set((archive or {}).get("seasons") or [])
    recent = [it for it in items if it["start"] and season_over(it)][-SCORER_SEASONS:]   # finished seasons: a top scorer «so far» goes stale in a week
    seasons, report = {}, []
    for it in recent:
        s = season_key(it["start"], it["end"], arch_seasons)
        found = {}
        if it["ts"] and all(g is not None for _, g in it["ts"]) and len({g for _, g in it["ts"]}) == 1:
            found["wikidata"] = {"n": len(it["ts"]), "goals": it["ts"][0][1],
                                 "top": [{"en": lab.get(q, {}).get("en", ""), "ar": lab.get(q, {}).get("ar", "")} for q, _ in it["ts"]]}
        for wiki in ("en", "ar"):
            v = scorer_value(field(wp.get(wiki, {}).get(it[wiki]), SCORER_FIELD[wiki])[1]) if it[wiki] else None
            if v:
                found[wiki] = {"n": len(v["names"]), "goals": v["goals"],
                               "top": [{"en": t, "ar": ar_of_title.get(t, "")} if wiki == "en" else {"en": "", "ar": x} for t, x in v["names"]]}
        if not found:
            continue
        if len({(f["n"], f["goals"]) for f in found.values()}) > 1:
            report.append({"lg": lg, "s": s, "why": "scorer sources disagree", "found": {k: [f["n"], f["goals"]] for k, f in found.items()}})
            continue
        best = found.get("wikidata") or found.get("en") or found["ar"]
        top = [dict(x) for x in best["top"]]
        if "ar" in found:
            if len(top) == 1 and not top[0]["ar"]:
                top[0]["ar"] = found["ar"]["top"][0]["ar"]
            for x, y in zip(top, found["ar"]["top"]) if len(top) == 1 else ():
                x["ar"] = x["ar"] or y["ar"]
        if "en" in found and best is not found["en"]:
            for x, y in zip(top, found["en"]["top"]):
                x["en"] = x["en"] or y["en"]
        for x in top:
            x["goals"] = best["goals"]
        ev = scorer_value(field(wp.get("en", {}).get(it["en"]), SCORER_FIELD["en"])[1]) if it["en"] else None
        cl = scorer_club(field(wp.get("en", {}).get(it["en"]), SCORER_FIELD["en"])[1]) if it["en"] else []
        pool = {c: (names or {})[c] for c in (season_clubs or {}).get(s, ()) if c in (names or {})}
        if len(top) == 1 and ev and len(ev["names"]) == 1 and len(cl) == 1 and pool:
            team = unique_id([cl[0][0], cl[0][1]], pool)
            if team:
                top[0]["team"] = team
        seasons[s] = {"top": [x for x in top if x["en"] or x["ar"]],
                      "src": [f"https://{w}.wikipedia.org/wiki/" + urllib.parse.quote(it[w].replace(" ", "_")) for w in ("ar", "en") if w in found]
                      + ([f"https://www.wikidata.org/wiki/{it['q']}"] if "wikidata" in found else [])}
    return {"v": V, "id": lg, "lic": "CC BY-SA 4.0", "seasons": seasons}, report


LICENSE = """# Terms of these files

- `champions/`: UrTV's own selection (CC BY 4.0; credit "UrTV data, CC BY 4.0"), from our match archive's results (TheSportsDB's data,
  credit TheSportsDB) and Wikidata (CC0). A season named from a Wikipedia article (`"src"` has `wikipedia`, its address in `"url"`) is
  CC BY-SA 4.0, as the article is.
- `scorers/`: **CC BY-SA 4.0** (https://creativecommons.org/licenses/by-sa/4.0/), from Wikipedia's season articles (each season's
  addresses in `"src"`) and Wikidata (CC0). Changes: the names and goals were read from the articles' infoboxes; an Arabic name may come
  from Wikidata. Reuse them under the same licence, with the same credit.
- `report.json`: the seasons left out and why.
"""


def load_json(p):
    try:
        return json.loads(Path(p).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def archive_names(adir, lg):
    """{club id: (its English name, its Arabic name)}: the archive's index (every club it keeps, with its leagues), then the league's tables"""
    names = {}
    for cid, t in (((load_json(Path(adir) / "index.json") or {}).get("teams")) or {}).items():
        if isinstance(t, dict) and lg in (t.get("leagues") or []) and t.get("en"):
            names[str(cid)] = (t["en"], t.get("ar") or "")
    files = [Path(adir) / f"sports/{lg}.json"] + sorted((Path(adir) / f"sports/{lg}").glob("*.json"))
    for f in files:
        for r in ((load_json(f) or {}).get("table") or []):
            if r.get("id") and r["id"] not in names:
                names[r["id"]] = (r.get("n") or "", r.get("ar") or "")
    return names


def run(args, fetch=http):
    adir, prev, out = Path(args.archive), Path(args.prev), Path(args.out)
    stamp = dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    try:
        items = query_seasons(fetch)
    except (OSError, ValueError, KeyError) as e:
        print("Wikidata's query did not answer: nothing written, the published files stay:", e)
        return 1
    lab = labels(fetch, [w for it in items for w in it["win"]] + [q for it in items for q, _ in it["ts"]])
    today = dt.date.today().isoformat()
    want = {"en": set(), "ar": set()}
    by_lg = {}
    for it in items:
        by_lg.setdefault(it["lg"], []).append(it)
    for lg, its in by_lg.items():
        recent = [it for it in its if it["start"] and it["start"] <= today][-SCORER_SEASONS - 1:]
        for it in its:
            if it in recent or not it["win"]:
                for w in ("en", "ar"):
                    if it[w]:
                        want[w].add(it[w])
    wp = {w: wikitexts(fetch, w, sorted(t)) for w, t in want.items()}
    en_titles = [t for it in items for t, _ in (scorer_value(field(wp["en"].get(it["en"]), SCORER_FIELD["en"])[1]) or {}).get("names", [])]
    ar_of_title = ar_labels_of_titles(fetch, en_titles)
    report = []
    for lg in sorted(LEAGUES):
        archive = load_json(adir / f"sports/{lg}.json")
        names = archive_names(adir, lg)
        season_clubs = {}   # 4.4 each season's clubs in our archive (its table), for a top scorer's club
        for f in [adir / f"sports/{lg}.json"] + sorted((adir / f"sports/{lg}").glob("*.json")):
            j = load_json(f) or {}
            if j.get("season"):
                season_clubs[str(j["season"])] = {str(r["id"]) for r in (j.get("table") or []) if r.get("id")}
        for kind, (body, rep) in (("champions", champions(lg, by_lg.get(lg, []), archive, lab, wp, names)),
                                  ("scorers", scorers(lg, by_lg.get(lg, []), archive, lab, wp, ar_of_title, names, season_clubs))):
            report += rep
            old = load_json(prev / f"{kind}/{lg}.json")
            empty = not (body.get("seasons"))
            if empty and old:
                body = old   # nothing found this time: the last good file stays
            elif empty:
                continue
            body = dict(body, updated=stamp)
            if old and {k: v for k, v in old.items() if k != "updated"} == {k: v for k, v in body.items() if k != "updated"}:
                body["updated"] = old.get("updated", stamp)
            p = out / f"{kind}/{lg}.json"
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps(body, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    (out / "report.json").write_text(json.dumps({"updated": stamp, "left_out": report}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (out / "LICENSE.md").write_text(LICENSE, encoding="utf-8")
    print(f"honours: {len(items)} Wikidata seasons, {len(report)} left out (report.json)")
    return 0


def main():
    a = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    a.add_argument("--archive", required=True)
    a.add_argument("--prev", required=True)
    a.add_argument("--out", required=True)
    return run(a.parse_args())


if __name__ == "__main__":
    sys.exit(main())
