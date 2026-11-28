#!/usr/bin/env python3
"""Find a club's TheSportsDB id, by its Arabic or English name, or list a league's clubs to pick from.

    optimize/saver.sh find-team الهلال          a club: every match in our list, Saudi clubs first
    optimize/saver.sh find-team --list Saudi    every club of the leagues whose name has "Saudi"

Our list, assets/shared/teams.json, has every club of the app's leagues and the national teams, with their ids
(built in CI from their matches: TheSportsDB's free search misses some clubs, the Saudi Al-Hilal among them). It is
searched as the TV's settings screen searches it (optimize/team_search.py: «الاهلي» = «الأهلي», «Atletico» =
«Atlético», the Saudi club first when names clash). Only a club that isn't in it is searched online: Arabic names go through assets/shared/teams-ar.json to their
English spellings, and the usual spellings are tried too (Al-Hilal, Al Hilal, Hilal, … FC).
Prints `id:Name  |  Arabic  |  leagues`, ready for `optimize/saver.sh teams`.
"""
import importlib.util
import json
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path

for stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

SHARED = Path(__file__).resolve().parents[1] / "assets/shared"
ARABIC = json.loads((SHARED / "teams-ar.json").read_text(encoding="utf-8"))      # English name -> Arabic
try:
    CLUBS = json.loads((SHARED / "teams.json").read_text(encoding="utf-8"))     # [{id, en, ar, leagues}]
except FileNotFoundError:   # an old checkout: search online only
    print("   (assets/shared/teams.json is missing: searching TheSportsDB only)", file=sys.stderr)
    CLUBS = []
API = "https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t="
# by path: `python -I` (how saver.sh runs this) leaves this folder off the import path
_spec = importlib.util.spec_from_file_location("team_search", Path(__file__).resolve().parent / "team_search.py")
team_search = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(team_search)


# 4.4: the names people say that are not a club's name: Arabic nicknames, common spellings and club abbreviations -> the club's id in
# teams.json. Compared after team_search.normalize (so «الزعيم» = «زعيم», «آرسنال» = «ارسنال»); the club goes first, the other matches
# follow. tests/find_team_alias_test.py checks every id is in our list and no alias is another club's own name.
ALIASES = {
    "136013": ["الزعيم", "زعيم اسيا", "الموج الازرق"],                      # Al-Hilal
    "136022": ["العالمي", "فارس نجد"],                                        # Al-Nassr
    "136018": ["العميد", "النمور", "عميد الاندية"],                           # Al-Ittihad
    "137721": ["الراقي", "قلعة الكؤوس"],                                      # Al-Ahli
    "136020": ["الليث", "شيخ الاندية"],                                       # Al-Shabab
    "136017": ["فارس الدهناء", "النواخذة"],                                   # Al-Ettifaq
    "136012": ["السكري"],                                                     # Al-Taawoun
    "136011": ["النموذجي"],                                                   # Al-Fateh
    "136137": ["الاخضر", "المنتخب السعودي", "منتخبنا"],                       # Saudi Arabia
    "133738": ["الملكي", "الريال", "ريال", "real", "rma"],                    # Real Madrid
    "133739": ["البرسا", "برشا", "البلوغرانا", "barca", "barsa"],             # Barcelona
    "133612": ["مان يونايتد", "مانيونايتد", "مان يو", "الشياطين الحمر",
               "man utd", "man united", "manu", "mufc"],                       # Manchester United
    "133613": ["مان سيتي", "مانسيتي", "السيتي", "man city", "mcfc"],          # Manchester City
    "133664": ["البايرن", "بايرن", "bayern"],                                 # Bayern Munich
    "133714": ["بي اس جي", "باريس", "psg"],                                   # Paris SG
    "133676": ["اليوفي", "يوفي", "السيدة العجوز", "juve"],                    # Juventus
    "133681": ["الانتر", "انتر", "inter"],                                    # Inter Milan
    "133610": ["البلوز", "تشلسي"],                                           # Chelsea
    "133604": ["المدفعجية", "الارسنال", "ارسنال"],                            # Arsenal
    "133602": ["الريدز", "ليفر"],                                             # Liverpool
    "133616": ["السبيرز", "spurs"],                                           # Tottenham Hotspur
    "133729": ["الاتلتي", "اتلتيكو", "atleti"],                               # Atlético Madrid
    "133650": ["بوروسيا", "bvb"],                                             # Borussia Dortmund
}


def alias_key(text):
    return " ".join(team_search.normalize(text))


ALIAS_OF = {alias_key(a): cid for cid, names in ALIASES.items() for a in names}


def alias_club(q):   # the club an alias names, or None
    cid = ALIAS_OF.get(alias_key(q))
    return next((c for c in CLUBS if c.get("id") == cid), None) if cid else None


def plain(ar):   # Arabic compared as the search does: no harakat/tatweel, one hamza and taa/yaa form, no ال, and the
    words = team_search.normalize(ar)   # spelling variants (تشلسي = تشيلسي) folded to one skeleton
    return "".join(team_search.skeleton(w) or w for w in words)


def ours(q):   # the clubs in our list that match, best first
    return team_search.search(CLUBS, q)


def show(clubs, limit=15):   # in the order given: search() already ranks them
    for c in clubs[:limit]:
        print(f"{c['id']}:{c['en']}  |  {c['ar'] or '-'}  |  {', '.join(c['leagues'])}")


def spellings(name):
    base = re.sub(r"\s+(S?FC|F\.C\.|Club|Football Club)$", "", name.strip(), flags=re.I)
    if " " in base and not re.match(r"Al[- ]", base, flags=re.I):   # Saudi Arabia, Real Madrid: as they are
        return list(dict.fromkeys([name, base]))
    bare = re.sub(r"^Al[- ]", "", base, flags=re.I)
    return list(dict.fromkeys([name, base, f"Al-{bare}", f"Al {bare}", bare]))


def search(name):
    try:
        with urllib.request.urlopen(API + urllib.parse.quote(name), timeout=20) as r:
            return json.load(r).get("teams") or []
    except Exception as e:  # noqa: BLE001
        print(f"   (TheSportsDB didn't answer for {name}: {e})", file=sys.stderr)
        return []


def main():
    q = " ".join(sys.argv[1:]).strip()
    if not q:
        sys.exit("usage: optimize/saver.sh find-team NAME   (Arabic or English)")
    if sys.argv[1] == "--list":
        words = [w.lower() for w in sys.argv[2:]]
        found = [c for c in CLUBS if any(all(w in l.lower() for w in words) for l in c["leagues"])] if words else CLUBS
        saudi = lambda c: not any("Saudi" in l for l in c["leagues"])   # noqa: E731
        show(sorted(found, key=lambda c: (saudi(c), c["en"])), limit=None)
        return
    hit = ours(q)
    club = alias_club(q)
    if club:   # a nickname or an abbreviation: that club first, then whatever else the words match
        hit = [club] + [c for c in hit if c.get("id") != club["id"]]
    if hit:
        show(hit)
        return
    if re.search(r"[\u0600-\u06FF]", q):
        names = [en for en, ar in ARABIC.items() if plain(ar) == plain(q)]
        if not names:
            sys.exit(f"«{q}» isn't in our Arabic list (assets/shared/teams-ar.json): try its English name.")
    else:
        names = [q]
    tried, found = set(), {}
    for name in names:   # the Arabic list has many spellings of one club: stop at the first that finds it
        if found:
            break
        for s in spellings(name):
            if s.lower() in tried or len(tried) >= 8:
                continue
            tried.add(s.lower())
            for t in search(s):
                if t.get("strSport") == "Soccer":
                    found[t["idTeam"]] = t
    if not found:
        sys.exit(f"Nothing found for {q} (tried: {', '.join(sorted(tried))}). Try the city, or the English name.")
    for t in sorted(found.values(), key=lambda t: (t.get("strCountry") != "Saudi Arabia", t["strTeam"])):
        print(f"{t['idTeam']}:{t['strTeam']}  |  {t.get('strLeague') or ''}  |  {t.get('strCountry') or ''}")


if __name__ == "__main__":
    main()
