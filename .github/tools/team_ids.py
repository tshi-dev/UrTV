#!/usr/bin/env python3
"""Every club the app's leagues have, and the national teams, with their TheSportsDB ids and Arabic names (run in
CI: TheSportsDB, Wikidata and Wikipedia are unreachable from the dev container). Monthly too, for promoted and
renamed clubs.

Clubs: the free name search misses some clubs altogether (it never returns the Saudi Al-Hilal), but every match
lists both sides' ids, so each league's last two seasons with matches (and its last and next rounds) name all its
clubs. Leagues are the ones the app offers (Prefs.LEAGUES, read from the Java source so the two can't drift).
National teams are searched by name, else found in their matches against other national teams.

Checked, not assumed: each league's season must have at least LEAGUES[...]["min"] clubs, the KNOWN ids must come out
as the clubs the app already uses, every national team must be found, and each id is looked up on its own
(lookupteam.php, else the team's own matches) to see it names the same team.

Arabic names, best first: our own (national teams below; Saudi clubs from .github/tools/saudi-clubs-ar.json, by
their Arabic Wikipedia article, or assets/shared/teams-ar.json), then Wikidata's Arabic label (the item holding the
club's TheSportsDB id, else its English Wikipedia article, else a search whose description says football club),
then an Arabic name TheSportsDB lists, and only then, for a club abroad, a transliteration (marked "arFrom":
"translit"). An Arab league's club or a national team is never transliterated: without a name it fails the run
("arFrom": "missing") until someone adds it to teams-ar.json, which wins over everything else on every run. Two clubs
never share a name: the Saudi club keeps it and the other gets its country ("العين الإماراتي"); a club named like
a national team becomes «نادي قطر».

Incremental: the matches decide the clubs every run (about 70 requests), but a team already in
.github/tools/teams-cache.json or assets/shared/teams.json under the same English name skips its id lookup and its
Wikidata lookups, and known league ids and national teams are reused. RUN_FULL=1 (the monthly run, or a manual run
with "full") checks everything again.

Other sports: Prefs.LEAGUES also holds team leagues of other sports (NBA, NFL, ...; LEAGUES[...]["sport"], football
by default), collected exactly like football leagues, one runner each; every team in teams.json has its "sport".
Their Arabic names are in teams-ar.json (city and name as Saudi screens write them), which wins as for clubs. A
team of another sport is told apart from a club of the same name by its sport: «الأهلي (كرة السلة)».

Events (Formula 1, UFC, boxing, tennis): not teams but races, cards and tournaments. The "Events" part finds their
leagues on TheSportsDB by name and writes teams/events.json ({"f1": [{id, name}], ...}, the next
assets/shared/events.json, which the page reads to ask each league for its next events); tennis only when the
free API gives it upcoming events. Its report holds what each league answered (names, dates, countries), for
checking what the screen can parse. It also looks for a Saudi basketball league (report["saudi basketball"]).

In CI: `plan` lists the parts, `collect --part NAME` runs on one runner per league and one for the national teams,
and `merge` puts them together; with no arguments it does all three in one process.

Writes teams/teams.json ([{id, en, ar, arFrom, sport, leagues, alt}], the next assets/shared/teams.json),
teams/teams-cache.json (the next .github/tools/teams-cache.json), teams/diff.json and teams/diff.md (added, removed
and renamed against assets/shared/teams.json) and teams/report.json (with the requests made). Exits 1 after writing
them if a check failed, so the run shows red with its data still there for review.
"""
import datetime
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
API = "https://www.thesportsdb.com/api/v1/json/123/"
WIKIDATA = "https://www.wikidata.org/w/api.php"
SPARQL = "https://query.wikidata.org/sparql"
ENWIKI = "https://en.wikipedia.org/w/api.php"
UA = "UrTV club list (github.com/tshi-dev/UrTV)"
PREFS = ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java"
OUT = Path(os.environ.get("TEAMS_OUT", "teams"))
CACHE = ROOT / ".github/tools/teams-cache.json"
FULL = os.environ.get("RUN_FULL") == "1"
# seconds between TheSportsDB requests: the free key says 30 a minute, but at 2 s a run drew 48 "429 Too Many
# Requests" in 176 requests (37 minutes); at 4 s earlier runs drew almost none
PACE = 4.0
NATIONAL_LEAGUE = "National teams"

# Every league in Prefs.LEAGUES: its country (Wikidata's ids for it, for checking a found item), the adjective that
# tells its clubs apart from a namesake ("الإماراتي"), and the fewest clubs one season can have. MLS names its
# seasons by one year ("2026").
LEAGUES = {
    "Saudi-Arabian Pro League": {"country": "Saudi Arabia", "wd": {"Q851"}, "adj": "السعودي", "min": 18},
    "Saudi First Division League": {"country": "Saudi Arabia", "wd": {"Q851"}, "adj": "السعودي", "min": 18},
    "English Premier League": {"country": "England", "wd": {"Q21", "Q145", "Q25"}, "adj": "الإنجليزي", "min": 20},
    "Spanish La Liga": {"country": "Spain", "wd": {"Q29"}, "adj": "الإسباني", "min": 20},
    "Italian Serie A": {"country": "Italy", "wd": {"Q38"}, "adj": "الإيطالي", "min": 20},
    "German Bundesliga": {"country": "Germany", "wd": {"Q183"}, "adj": "الألماني", "min": 18},
    "French Ligue 1": {"country": "France", "wd": {"Q142"}, "adj": "الفرنسي", "min": 18},
    "Portuguese Primeira Liga": {"country": "Portugal", "wd": {"Q45"}, "adj": "البرتغالي", "min": 18},
    "Dutch Eredivisie": {"country": "Netherlands", "wd": {"Q55", "Q29999"}, "adj": "الهولندي", "min": 18},
    "Turkish Super Lig": {"country": "Turkey", "wd": {"Q43"}, "adj": "التركي", "min": 18},
    "Egyptian Premier League": {"country": "Egypt", "wd": {"Q79"}, "adj": "المصري", "min": 18},
    "UAE Pro League": {"country": "United Arab Emirates", "wd": {"Q878"}, "adj": "الإماراتي", "min": 14},
    "Qatar Stars League": {"country": "Qatar", "wd": {"Q846"}, "adj": "القطري", "min": 12},
    "Iraqi Premier League": {"country": "Iraq", "wd": {"Q796"}, "adj": "العراقي", "min": 18},   # 4.3; TheSportsDB 5056, «Iraq Stars League»
    # 4.3: the continental cups. "cup": followable as a competition (its games, league_fixtures.py), but no club list of its own: their
    # clubs are the national leagues' (a qualifying round's Faroese or Maltese club is not one to follow), so no club is collected for them
    "AFC Champions League Elite": {"country": "Worldwide", "wd": set(), "adj": "", "min": 0, "cup": True},
    "UEFA Champions League": {"country": "Europe", "wd": set(), "adj": "", "min": 0, "cup": True},
    "American Major League Soccer": {"country": "United States", "wd": {"Q30", "Q16"}, "adj": "الأمريكي", "min": 30,
                                     "calendar": True},
    # other sports: "sport" is TheSportsDB's strSport. The NBA names its seasons "2025-2026", the NFL "2025"; both
    # have a Canadian or foreign side in preseason games, so the league's own team list decides who belongs
    "NBA": {"sport": "Basketball", "country": "United States", "wd": {"Q30", "Q16"}, "adj": "الأمريكي", "min": 30},
    "NFL": {"sport": "American Football", "country": "United States", "wd": {"Q30"}, "adj": "الأمريكي", "min": 32,
            "calendar": True},
}
for _l in LEAGUES.values():
    _l.setdefault("sport", "Soccer")
SPORT_AR = {"Soccer": "كرة القدم", "Basketball": "كرة السلة", "American Football": "كرة القدم الأمريكية"}
# what a Wikidata description of a team of each sport says, and Wikidata's class for such a team
SPORT_WORDS = {"Soccer": re.compile(r"football|soccer", re.I), "Basketball": re.compile(r"basketball", re.I),
               "American Football": re.compile(r"american football|gridiron|national football league", re.I)}
SPORT_CLASS = {"Soccer": "Q476028", "Basketball": "Q13393265", "American Football": "Q17156793"}
GUESSES = {"Portuguese Primeira Liga": "4344", "Turkish Super Lig": "4339", "NBA": "4387", "NFL": "4391",
           "Iraqi Premier League": "5056", "AFC Champions League Elite": "4719", "UEFA Champions League": "4480"}   # 4.3: read by .github/tools/league_probe.py   # used only if the name matches
ALIASES = {"United States": ["USA", "United States of America"], "Jordan": ["Jordan national football team"],
           "South Korea": ["Korea Republic"], "Iran": ["IR Iran"], "China": ["China PR"]}
NATIONAL = {   # national teams: TheSportsDB's English name -> our Arabic name
    "Saudi Arabia": "السعودية", "United Arab Emirates": "الإمارات", "Qatar": "قطر", "Kuwait": "الكويت",
    "Bahrain": "البحرين", "Oman": "عمان", "Iraq": "العراق", "Jordan": "الأردن", "Syria": "سوريا", "Lebanon": "لبنان",
    "Palestine": "فلسطين", "Yemen": "اليمن", "Egypt": "مصر", "Morocco": "المغرب", "Algeria": "الجزائر",
    "Tunisia": "تونس", "Libya": "ليبيا", "Sudan": "السودان", "Iran": "إيران", "Japan": "اليابان",
    "South Korea": "كوريا الجنوبية", "Australia": "أستراليا", "China": "الصين", "Uzbekistan": "أوزبكستان",
    "Brazil": "البرازيل", "Argentina": "الأرجنتين", "France": "فرنسا", "England": "إنجلترا", "Spain": "إسبانيا",
    "Germany": "ألمانيا", "Italy": "إيطاليا", "Portugal": "البرتغال", "Netherlands": "هولندا", "Belgium": "بلجيكا",
    "Croatia": "كرواتيا", "United States": "الولايات المتحدة", "Mexico": "المكسيك", "Uruguay": "الأوروغواي",
    "Colombia": "كولومبيا", "Senegal": "السنغال", "Nigeria": "نيجيريا", "Cameroon": "الكاميرون", "Turkey": "تركيا",
}
# a national team the search misses is looked for in its matches against these
PARTNERS = ["Saudi Arabia", "Iraq", "Qatar", "United Arab Emirates", "Kuwait", "Oman", "Bahrain", "Palestine",
            "Syria", "Lebanon", "Egypt", "Morocco", "South Korea", "Japan"]
KNOWN = {"136013": "Al-Hilal", "136022": "Al-Nassr", "133738": "Real Madrid", "133604": "Arsenal",
         "136137": "Saudi Arabia"}
PROBE = {"133738": "Real Madrid"}   # does lookupteam.php answer by id? (one request per part, not one per KNOWN id)
HILAL_ARTICLE = "نادي الهلال (السعودية)"   # Al-Hilal's Arabic Wikipedia article: finds Wikidata's TheSportsDB property

FOOTBALL = re.compile(r"football|soccer", re.I)
# a club's second side, not the club: "RCD Espanyol B", "Real Madrid Castilla", "Bayern Munich II", "Ajax U19"
SIDE = re.compile(r"\s(?:B|C|II|III|Castilla|Atlètic|U-?\d{2}|Reserves?|Youth|Women|Femen\w*)$", re.I)
NOT_A_CLUB = re.compile(r"women|female|ladies|futsal|basketball|handball|volleyball|youth|under-\d|reserve", re.I)
# letters Thmanyah (the screen's font) lacks, as Arabic writes them
FONT_FIX = str.maketrans({"گ": "ج", "ژ": "ج", "ڨ": "ق", "ی": "ي", "ک": "ك", "ۀ": "ه", "ہ": "ه", "ھ": "ه", "ە": "ه",
                          "ۆ": "و", "ٱ": "ا"})


def not_a_club(desc, sport="Soccer"):   # a basketball team is "basketball" in its description, not a mistake
    return bool(NOT_A_CLUB.search(re.sub("basketball", "", desc, flags=re.I) if sport == "Basketball" else desc))


ARABIC_NAME = re.compile(r"[\u0621-\u063A\u0641-\u064A\u0660-\u0669\u067E\u0686\u06A4 .'\-]+")


# ---------- the network (the free TheSportsDB key allows 30 requests a minute) ----------

STATS = {"thesportsdb": 0, "wikidata and wikipedia": 0, "rate limited (429)": 0, "retried": 0, "empty answers": 0}


def fetch(url, data=None, headers=None, pause=PACE, tries=6, count="thesportsdb"):
    """JSON from url; None for an empty or non-JSON answer (TheSportsDB's way of saying "nothing"), which is
    asked once more, not six times: retrying those used to stretch a run past an hour."""
    empty = 0
    for attempt in range(tries):
        time.sleep(pause)
        STATS[count] += 1
        try:
            req = urllib.request.Request(url, data=data, headers={"User-Agent": UA, **(headers or {})})
            with urllib.request.urlopen(req, timeout=60) as r:
                body = r.read()
            try:
                return json.loads(body)
            except ValueError:
                empty += 1
                if empty >= 2:
                    STATS["empty answers"] += 1
                    return None
        except urllib.error.HTTPError as e:
            if e.code not in (429, 500, 502, 503, 504):
                raise
            STATS["rate limited (429)" if e.code == 429 else "retried"] += 1
            wait = e.headers.get("Retry-After") if e.headers else None
            time.sleep(int(wait) if wait and wait.isdigit() and int(wait) < 600 else 20 * (attempt + 1))
        except (urllib.error.URLError, TimeoutError):
            STATS["retried"] += 1
            time.sleep(10 * (attempt + 1))
    raise RuntimeError("no answer: " + url[:200])


def get(path):
    return fetch(API + path) or {}


def wiki(api, params):
    url = api + "?" + urllib.parse.urlencode({**params, "format": "json", "maxlag": "5"})
    d = fetch(url, pause=0.5, count="wikidata and wikipedia") or {}
    if (d.get("error") or {}).get("code") == "maxlag":
        time.sleep(10)
        d = fetch(url, pause=0.5, count="wikidata and wikipedia") or {}
    return d


# ---------- names ----------

def fold(name):   # "Atlético", "Beşiktaş", "Kasımpaşa" -> "atletico", "besiktas", "kasimpasa"
    s = unicodedata.normalize("NFKD", str(name).replace("ı", "i").replace("ß", "ss"))
    return "".join(c for c in s if not unicodedata.combining(c)).lower()


def team_key(name):   # the screen's teamKey(): "Al-Hilal", "Al Hilal SFC", "AL HILAL" are one club; so are accents
    s = re.sub(r"\b(fc|sc|cf|afc|club|saudi club)\b", "", fold(name))
    return re.sub(r"^al ", "al", re.sub(r"[^a-z0-9]+", " ", s).strip())


def latin(name):   # "Atlético" -> "atletico", for comparing names
    return re.sub(r"[^a-z0-9]+", " ", fold(name)).strip()


GENERIC = {"fc", "sc", "cf", "afc", "club", "al", "el", "de", "the", "and", "city", "united", "real", "sporting",
           "football", "soccer", "sport", "sports", "saudi", "cd", "ud", "sd", "ac", "as", "ss", "us", "sv", "vfb",
           "vfl", "tsg", "rc", "ca", "fk", "sk"}


def same_team(a, b):   # one name holds a distinctive word of the other ("Paris SG", "Paris Saint-Germain F.C.")
    if team_key(a) == team_key(b):
        return True
    wa = {w for w in latin(a).split() if len(w) >= 3 and w not in GENERIC}
    wb = {w for w in latin(b).split() if len(w) >= 3 and w not in GENERIC}
    return bool(wa & wb) or (latin(a).replace(" ", "") in latin(b).replace(" ", "")) \
        or (latin(b).replace(" ", "") in latin(a).replace(" ", ""))


def plain_ar(s):   # Arabic compared without harakat or hamza forms
    s = re.sub(r"[ً-ٰٟـ]", "", str(s))
    return re.sub(r"\s+", " ", s.translate(str.maketrans("أإآٱةى", "ااااهي"))).strip()


def clean_ar(label):
    """Wikidata's «نادي العين (الإمارات)» -> «العين»; '' if it isn't plain Arabic the screen's font can draw."""
    s = re.sub(r"[ً-ٰٟـ]", "", str(label or "")).translate(FONT_FIX)
    s = re.sub(r"\s*\([^)]*\)\s*", " ", s).strip()
    s = re.sub(r"^(?:(?:ال)?ناد[يى]|فريق)(?:\s+|$)", "", s)
    s = re.sub(r"\s+[0-9٠-٩]+$", "", s)   # a founding year: «بولونيا 1909», «بادربورن 07» -> «بولونيا», «بادربورن»
    s = re.sub("[0-9]", lambda m: chr(0x0660 + int(m.group())), s)   # «فلم 2 تيلبورخ» -> «فلم ٢ تيلبورخ»
    s = re.sub(r"\s+(?:لكرة القدم|الرياضي|للألعاب الرياضية)$", "", s).strip()
    return s if ARABIC_NAME.fullmatch(s) and len(s.replace(" ", "")) >= 2 else ""


# the screen's translit() (src/template.html), for the names nothing else gives: tests/team_ids_test.py compares them
TL_WORDS = {"the": "ذا", "of": "أوف", "and": "آند", "a": "إيه", "in": "إن", "on": "أون", "to": "تو", "us": "أس",
            "fc": "", "cf": "", "sc": "", "afc": "", "united": "يونايتد", "city": "سيتي", "real": "ريال",
            "club": "كلوب", "sporting": "سبورتنغ", "saint": "سانت", "st": "سانت"}
TL_LETTERS = dict(zip("abcdefghijklmnopqrstuvwxyz", "إيه بي سي دي إي إف جي إتش آي جيه كيه إل إم إن أو بي كيو آر إس تي "
                      "يو في دبليو إكس واي زد".split()))
TL_MAP = {"sh": "ش", "kh": "خ", "th": "ث", "dh": "ذ", "gh": "غ", "ch": "تش", "ph": "ف", "ck": "ك", "qu": "كو",
          "ou": "و", "oo": "و", "ee": "ي", "a": "ا", "b": "ب", "c": "ك", "d": "د", "e": "ي", "f": "ف", "g": "ج",
          "h": "ه", "i": "ي", "j": "ج", "k": "ك", "l": "ل", "m": "م", "n": "ن", "o": "و", "p": "ب", "q": "ق", "r": "ر",
          "s": "س", "t": "ت", "u": "و", "v": "ف", "w": "و", "x": "كس", "y": "ي", "z": "ز"}
TL_START = {"a": "أ", "e": "إ", "i": "إ", "o": "أو", "u": "أ"}


def translit(en):
    def word(w):
        if re.search("[؀-ۿ]", w) or not re.search("[A-Za-z0-9]", w):
            return w
        lw = w.lower()
        if lw in TL_WORDS:
            return TL_WORDS[lw]
        if re.fullmatch("[A-Z]{2,4}", w):   # CP, PSG
            return " ".join(TL_LETTERS[c] for c in lw)
        if lw.startswith("al-"):
            return "ال" + word(w[3:])
        out, x, i = "", re.sub(r"(.)\1", r"\1", lw), 0
        while i < len(x):
            two = TL_MAP.get(x[i:i + 2]) if i + 1 < len(x) else None
            if i == 0 and x[0] in TL_START and not two:
                out, i = out + TL_START[x[0]], i + 1
                continue
            if two:
                out, i = out + two, i + 2
                continue
            ch = x[i]
            if ch == "e" and i > 0:   # short or silent e
                i += 1
                continue
            if ch == "c" and re.fullmatch("[eiy]", x[i + 1:i + 2] or "-"):
                out, i = out + "س", i + 1
                continue
            out += chr(0x0660 + int(ch)) if ch in "0123456789" else TL_MAP.get(ch, "")
            i += 1
        return out
    s = re.sub(r"\bAl (?=[A-Z])", "Al-", str(en).replace("&", " و "), flags=re.A)
    return re.sub(r"\s{2,}", " ", "".join(word(w) for w in re.split(r"(\s+)", s))).strip()


# ---------- what earlier runs found ----------

def load_cache():
    """The last run's cache, and the committed list by id; both empty for a full run."""
    if FULL:
        return {"leagues": {}, "national": {}, "teams": {}}, {}
    try:
        cache = json.loads(CACHE.read_text(encoding="utf-8"))
    except (FileNotFoundError, ValueError):
        cache = {}
    try:
        listed = {t["id"]: t for t in json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))}
    except (FileNotFoundError, ValueError, KeyError, TypeError):
        listed = {}
    return {k: cache.get(k) or {} for k in ("leagues", "national", "teams")}, listed


def known(i, en, cache, listed):
    """What an earlier run checked about team i, if it still has the same name: else None (look it up again)."""
    c = cache["teams"].get(i)
    if c and team_key(c.get("en", "")) == team_key(en):
        return c
    t = listed.get(i)
    if t and team_key(t.get("en", "")) == team_key(en):   # in the committed list: checked when it was added
        return {"en": t["en"], "checked": "", "lookup": {"name": t["en"], "alt": t.get("alt") or [], "country": "",
                                                         "gender": ""}}
    return None


# ---------- clubs from their leagues' matches ----------

def seasons_to_try(league, today):
    if league.get("calendar"):
        return [str(today.year - k) for k in range(3)]
    y = today.year if today.month >= 7 else today.year - 1   # a season starts in July or August
    return [f"{y - k}-{y - k + 1}" for k in range(3)]


def sides(events):
    for e in events or []:
        for side in ("Home", "Away"):
            i, en = e.get(f"id{side}Team"), e.get(f"str{side}Team")
            if i and en:
                yield str(i), en.strip()


def no_id(events):   # clubs a match names without an id: they play, but can't be followed
    return {e.get(f"str{side}Team").strip() for e in events or [] for side in ("Home", "Away")
            if e.get(f"str{side}Team") and not e.get(f"id{side}Team")}


def league_ids(names, cached=None):
    if cached and all(n in cached for n in names):
        return dict(cached)
    ids = {l["strLeague"]: l["idLeague"] for l in get("all_leagues.php").get("leagues") or []}
    for country in dict.fromkeys(LEAGUES[n]["country"] for n in names if n in LEAGUES):
        if all(n in ids for n in names):
            break
        sports = {LEAGUES[n]["sport"] for n in names if n in LEAGUES and LEAGUES[n]["country"] == country}
        found = {}
        for sport in sorted(sports):
            got = get("search_all_leagues.php?" + urllib.parse.urlencode({"c": country, "s": sport}))
            for k in ("countries", "countrys", "leagues"):
                found.setdefault(k, []).extend(got.get(k) or [])
        for l in found.get("countries") or found.get("countrys") or found.get("leagues") or []:
            ids.setdefault(l["strLeague"], l["idLeague"])
    for name, i in GUESSES.items():
        if name in names and name not in ids:
            if name in [l.get("strLeague") for l in get(f"lookupleague.php?id={i}").get("leagues") or []]:
                ids[name] = i
    return ids


def fill_by_id(lid, name, every, reach=6, limit=24):
    """The free API shows a league's first 100 games and its team lists hold 10, which leaves a league of 30 teams a
    few short early in a season. TheSportsDB numbers a league's teams one after another (the NBA's are 134860-134889),
    so the ids next to those found are looked up, and kept when they answer as teams of this league. Returns the
    ids asked."""
    known = [int(i) for i in every if i.isdigit()]
    asked = 0
    if len(every) >= LEAGUES[name]["min"] or not known:
        return asked
    for i in [x for x in range(min(known) - reach, max(known) + reach + 1) if str(x) not in every]:
        if len(every) >= LEAGUES[name]["min"] or asked >= limit:
            break
        asked += 1
        hit = (get(f"lookupteam.php?id={i}").get("teams") or [None])[0]
        if hit and str(hit.get("idLeague")) == str(lid) and hit.get("strSport") == LEAGUES[name]["sport"] \
                and hit.get("strTeam"):
            every[str(i)] = hit["strTeam"].strip()
    return asked


def collect_league(name, lid, today, teams, report, problems):
    """The clubs of the last two seasons that have matches, and of the last and next rounds."""
    seasons, nameless = {}, {}
    for s in seasons_to_try(LEAGUES[name], today):
        if len(seasons) == 2:
            break
        events = get(f"eventsseason.php?id={lid}&s={s}").get("events")
        found = dict(sides(events))
        if found:
            seasons[s], nameless[s] = found, no_id(events)
    rounds, round_nameless = {}, set()
    for path in (f"eventspastleague.php?id={lid}", f"eventsnextleague.php?id={lid}"):
        events = get(path).get("events")
        rounds.update(sides(events))
        round_nameless |= no_id(events)
    every = {}
    for found in [*seasons.values(), rounds]:
        every.update(found)
    if LEAGUES[name]["sport"] != "Soccer":   # preseason games name foreign sides: the league's own list decides
        own = {str(t["idTeam"]): t["strTeam"].strip() for t in get("search_all_teams.php?" + urllib.parse.urlencode(
            {"l": name})).get("teams") or [] if t.get("idTeam") and t.get("strTeam")}
        report.setdefault("league team lists", {})[name] = len(own)
        if len(own) >= LEAGUES[name]["min"]:
            every = {i: own[i] for i in own}
            rounds = {i: en for i, en in rounds.items() if i in own}
            for found in seasons.values():
                for i in [i for i in found if i not in own]:
                    del found[i]
        else:
            every.update(own)
            report.setdefault("ids looked up next to the found ones", {})[name] = fill_by_id(lid, name, every)
    for i, en in every.items():
        t = teams.setdefault(i, {"en": en, "leagues": [], "sport": LEAGUES[name]["sport"]})
        if name not in t["leagues"]:
            t["leagues"].append(name)
    # the newest season with matches is the current one: its last and next rounds belong to it
    clubs_in = {s: {**found, **(rounds if k == 0 else {})} for k, (s, found) in enumerate(seasons.items())}
    without = {s: sorted((nameless[s] | (round_nameless if k == 0 else set())) - set(clubs_in[s].values()))
               for k, s in enumerate(seasons)}
    if len(seasons) == 2:   # the current season short of a club: last season's missing ones, by their own matches
        now, before = list(seasons)
        if len(clubs_in[now]) + len(without[now]) < LEAGUES[name]["min"]:
            for i, en in [(i, en) for i, en in seasons[before].items() if i not in clubs_in[now]][:6]:
                for path in (f"eventslast.php?id={i}", f"eventsnext.php?id={i}"):
                    d = get(path)
                    if any(str(e.get("idLeague")) == str(lid) and e.get("strSeason") == now
                           for e in d.get("results") or d.get("events") or []):
                        clubs_in[now][i] = en
                        report.setdefault("found in their own matches", []).append(f"{name} {now}: {i} {en}")
                        break
    report["clubs per league"][name] = {"clubs": len(every), "seasons": {s: len(f) for s, f in clubs_in.items()},
                                        "last and next rounds": len(rounds)}
    for k in ("ids looked up next to the found ones", "league team lists"):
        if name in report.get(k, {}):
            report["clubs per league"][name][k] = report[k][name]
    if any(without.values()):
        report["clubs per league"][name]["named in matches without an id"] = {s: v for s, v in without.items() if v}
    print(f"{name}: {len(every)} clubs {report['clubs per league'][name]['seasons']}", flush=True)
    if len(seasons) < 2:
        problems.append(f"{name}: matches found for {len(seasons)} of the last two seasons")
    for s, found in clubs_in.items():   # a club without an id still counts: the season's data is all there
        if LEAGUES[name]["sport"] != "Soccer":   # the free API shows 100 games of a season: the union above is checked
            if len(every) < LEAGUES[name]["min"]:
                problems.append(f"{name}: {len(every)} teams, at least {LEAGUES[name]['min']} expected")
            break
        if len(found) + len(without[s]) < LEAGUES[name]["min"]:
            problems.append(f"{name} {s}: {len(found)} clubs"
                            + (f" and {len(without[s])} without an id" if without[s] else "")
                            + f", at least {LEAGUES[name]['min']} expected")


def find_national(en, teams, report, cache=None, listed=None):
    """The men's team named exactly as the country (or a known alias); else from its matches with other teams.
    One found before is reused."""
    names = [en] + ALIASES.get(en, [])
    wanted = {n.lower() for n in names}
    if cache and en in cache["national"]:
        return tuple(cache["national"][en])
    for i, t in (listed or {}).items():
        if NATIONAL_LEAGUE in t.get("leagues", []) and t.get("en", "").lower() in wanted:
            return i, t["en"]
    seen = []
    for q in names:
        for t in get("searchteams.php?" + urllib.parse.urlencode({"t": q})).get("teams") or []:
            if t.get("strSport") == "Soccer":
                seen.append(t.get("strTeam"))
                if (t.get("strTeam") or "").lower() in wanted and (t.get("strGender") or "Male") == "Male":
                    return str(t["idTeam"]), t["strTeam"]
    known = {t["en"]: i for i, t in teams.items() if NATIONAL_LEAGUE in t["leagues"]}
    for partner in [p for p in PARTNERS if p != en and p in known][:8]:
        for a, b in ((en, partner), (partner, en)):
            q = f"{a} vs {b}".replace(" ", "_")
            for i, name in sides(get("searchevents.php?" + urllib.parse.urlencode({"e": q})).get("event")):
                if name.lower() in wanted and i != known[partner]:
                    report["national teams found in their matches"].append(f"{en}: {i} ({q})")
                    return i, name
    report["national teams not found"].append(f"{en} (search gave: {', '.join(seen[:8]) or 'nothing'})")
    return None


def verify(teams, report, cache=None, listed=None, today=""):
    """Look each id up on its own: lookupteam.php if it answers the KNOWN ids right, else the team's own matches.
    A team checked before under the same name keeps that check."""
    def lookup(i):
        hit = (get(f"lookupteam.php?id={i}").get("teams") or [None])[0]
        return hit if hit and str(hit.get("idTeam")) == i else None
    checks = report["id checks"]
    need = []
    for i, t in teams.items():
        before = known(i, t["en"], cache, listed) if cache is not None else None
        if before and before.get("lookup") is not None:
            t["lookup"], t["checked"] = before["lookup"], before.get("checked", "")
            checks["checked before"] += 1
        else:
            need.append(i)
    if not need:
        report["ids checked with"] = "nothing new to check"
        return
    usable = all((lambda t: t and team_key(t.get("strTeam")) == team_key(n))(lookup(i)) for i, n in PROBE.items())
    report["ids checked with"] = "lookupteam.php" if usable else "the team's own matches"
    for i in need:
        t = teams[i]
        t["checked"] = today
        if usable:
            hit = lookup(i)
            if not hit:
                checks["no answer"].append(f"{i} {t['en']}")
                continue
            alt = [a.strip() for a in re.split(r"[,;/]", hit.get("strTeamAlternate") or "") if a.strip()]
            t["lookup"] = {"name": hit.get("strTeam") or "", "alt": alt, "country": hit.get("strCountry") or "",
                           "gender": hit.get("strGender") or ""}
            if not any(same_team(t["en"], n) for n in [hit.get("strTeam") or ""] + alt):
                checks["another name"].append(f"{i}: {t['en']} in the matches, {hit.get('strTeam')} on its own")
            elif t["lookup"]["gender"] and t["lookup"]["gender"] != "Male":
                checks["another name"].append(f"{i}: {t['en']} is a {t['lookup']['gender']} team")
            else:
                checks["same team"] += 1
        else:
            names = []
            for path in (f"eventslast.php?id={i}", f"eventsnext.php?id={i}"):
                d = get(path)
                names = [n for j, n in sides(d.get("results") or d.get("events")) if j == i]
                if names:
                    break
            t["lookup"] = {"name": names[0] if names else "", "alt": [], "country": "", "gender": ""}
            if not names:
                checks["no answer"].append(f"{i} {t['en']}")
            elif not any(same_team(t["en"], n) for n in names):
                checks["another name"].append(f"{i}: {t['en']} in the league, {names[0]} in its own matches")
            else:
                checks["same team"] += 1


# ---------- Arabic names ----------

def wikidata_arabic(teams, report, cache=None):
    """{TheSportsDB id: (Arabic label, Arabic Wikipedia title, English label, how)} from Wikidata, matched by id
    first. A team looked up before under the same name keeps that answer (none too: a full run asks again)."""
    found, notes = {}, report["wikidata"]
    need = {}
    _, curated_names, _ = curated()
    for i, t in teams.items():
        c = (cache or {}).get("teams", {}).get(i)
        if t.get("sport", "Soccer") != "Soccer" and curated_names.get(team_key(t["en"])):
            continue   # ours wins anyway: no request for a team of another sport whose name we wrote
        # a team without a name, or with only a transliteration, is asked about again: Wikidata may have it by now
        if c and "wd" in c and team_key(c.get("en", "")) == team_key(t["en"]) \
                and c.get("arFrom") not in ("translit", "missing"):
            if c["wd"]:
                found[i] = tuple(c["wd"])
        else:
            need[i] = t
    notes["looked up before"] = len(teams) - len(need)
    if not need:
        return found
    try:
        d = wiki(WIKIDATA, {"action": "wbgetentities", "sites": "arwiki", "titles": HILAL_ARTICLE, "props": "claims"})
        hilal = next(iter(d.get("entities", {}).values()), {})
        props = [p for p, claims in hilal.get("claims", {}).items()
                 if any(((c.get("mainsnak") or {}).get("datavalue") or {}).get("value") == "136013" for c in claims)]
        if len(props) != 1:   # Al-Hilal's item doesn't hold it: find the property by its name
            d = wiki(WIKIDATA, {"action": "wbsearchentities", "search": "TheSportsDB team ID", "language": "en",
                                "type": "property", "limit": 5})
            props = [h["id"] for h in d.get("search", []) if re.search(r"thesportsdb.*team", h.get("label", ""), re.I)][:1]
            notes["TheSportsDB property found by name"] = bool(props)
        notes["TheSportsDB property"] = props
        if len(props) == 1:
            ids = list(need)
            for k in range(0, len(ids), 150):
                values = " ".join(f'"{i}"' for i in ids[k:k + 150])
                q = f"""SELECT ?id ?item ?ar ?en ?arTitle ?desc WHERE {{
                  VALUES ?id {{ {values} }} ?item wdt:{props[0]} ?id .
                  OPTIONAL {{ ?item rdfs:label ?ar FILTER(LANG(?ar) = "ar") }}
                  OPTIONAL {{ ?item rdfs:label ?en FILTER(LANG(?en) = "en") }}
                  OPTIONAL {{ ?item schema:description ?desc FILTER(LANG(?desc) = "en") }}
                  OPTIONAL {{ ?a schema:about ?item; schema:isPartOf <https://ar.wikipedia.org/>; schema:name ?arTitle }}
                }}"""
                rows = (fetch(SPARQL, data=urllib.parse.urlencode({"query": q}).encode(), pause=1, count="wikidata and "
                              "wikipedia", headers={"Accept": "application/sparql-results+json"}) or {}) \
                    .get("results", {}).get("bindings", [])
                by_id = {}
                for r in rows:
                    by_id.setdefault(r["id"]["value"], {})[r["item"]["value"]] = {k: v["value"] for k, v in r.items()}
                for i, items in by_id.items():
                    good = [v for v in items.values() if not not_a_club(v.get("desc", ""), teams[i].get("sport", "Soccer"))
                            and same_team(teams[i]["en"], v.get("en", ""))]
                    if len(good) != 1:
                        notes["ids on several or unlike items"].append(f"{i} {teams[i]['en']}: "
                                                                       + ", ".join(v.get("en", "?") for v in items.values()))
                        continue
                    found[i] = (good[0].get("ar", ""), good[0].get("arTitle", ""), good[0].get("en", ""), "id")
        notes["matched by id"] = sum(1 for i in need if i in found)
    except Exception as e:  # noqa: BLE001  (Wikidata down: the other sources still give names)
        notes["error (by id)"] = repr(e)[:300]
    try:
        by_name(need, found, notes)
    except Exception as e:  # noqa: BLE001
        notes["error (by name)"] = repr(e)[:300]
    return found


def by_name(teams, found, notes):
    """Clubs Wikidata doesn't hold the id of: their English Wikipedia article, else a search, with checks."""
    left = [i for i, t in teams.items() if i not in found and NATIONAL_LEAGUE not in t["leagues"]]
    titles = {}   # club id -> titles to try, best first
    for i in left:
        t = teams[i]
        base = [t["en"]] + [a for a in (t.get("lookup") or {}).get("alt", []) if re.fullmatch(r"[ -~À-ž]+", a)]
        titles[i] = list(dict.fromkeys(x for b in base[:3] for x in (b, b + " FC", b + " F.C.", b + " SC")))
    qid = {}   # title -> Wikidata id
    flat = list(dict.fromkeys(x for v in titles.values() for x in v))
    for k in range(0, len(flat), 50):
        d = wiki(ENWIKI, {"action": "query", "titles": "|".join(flat[k:k + 50]), "redirects": 1, "prop": "pageprops",
                          "ppprop": "wikibase_item|disambiguation"}).get("query", {})
        to = {x["from"]: x["to"] for x in d.get("normalized", []) + d.get("redirects", [])}
        pages = {p.get("title"): p.get("pageprops", {}) for p in d.get("pages", {}).values()}
        for title in flat[k:k + 50]:
            final = title
            for _ in range(3):
                final = to.get(final, final)
            props = pages.get(final) or {}
            if props.get("wikibase_item") and "disambiguation" not in props:
                qid[title] = props["wikibase_item"]
    candidates = {i: list(dict.fromkeys(qid[x] for x in titles[i] if x in qid)) for i in left}
    entities = {}
    accept(teams, left, candidates, entities, found)
    # no club behind its article («Roma» is the city): Wikidata's football clubs (Q476028) of that name, then any
    # item whose description says football
    for search in ("clubs", "items"):
        rest = [i for i in left if i not in found]
        for i in rest:
            if search == "clubs":
                d = wiki(WIKIDATA, {"action": "query", "list": "search", "srnamespace": 0, "srlimit": 5,
                                    "srsearch": f'{teams[i]["en"]} haswbstatement:P31={SPORT_CLASS[teams[i].get("sport", "Soccer")]}'})
                candidates[i] = [h["title"] for h in d.get("query", {}).get("search", [])]
            else:
                d = wiki(WIKIDATA, {"action": "wbsearchentities", "search": teams[i]["en"], "language": "en",
                                    "type": "item", "limit": 7})
                candidates[i] = [h["id"] for h in d.get("search", []) if SPORT_WORDS[teams[i].get("sport", "Soccer")].search(h.get("description", ""))]
        accept(teams, rest, candidates, entities, found)
        notes[f"matched by a search of {search}"] = sum(1 for i in rest if i in found)
    notes["matched by name"] = sum(1 for i in left if i in found)


def accept(teams, ids, candidates, entities, found):
    """The first candidate item that is a men's football club of the club's country, with its name, and has an
    Arabic label or article."""
    every = [q for i in ids for q in candidates.get(i, []) if q not in entities]
    every = list(dict.fromkeys(every))
    for k in range(0, len(every), 50):
        d = wiki(WIKIDATA, {"action": "wbgetentities", "ids": "|".join(every[k:k + 50]),
                            "props": "labels|descriptions|claims|sitelinks", "languages": "ar|en", "sitefilter": "arwiki"})
        entities.update(d.get("entities", {}))
    for i in ids:
        country = set().union(*(LEAGUES[l]["wd"] for l in teams[i]["leagues"] if l in LEAGUES))
        for q in candidates.get(i, []):
            e = entities.get(q) or {}
            desc = ((e.get("descriptions") or {}).get("en") or {}).get("value", "")
            en = ((e.get("labels") or {}).get("en") or {}).get("value", "")
            where = {((c.get("mainsnak") or {}).get("datavalue") or {}).get("value", {}).get("id")
                     for c in (e.get("claims") or {}).get("P17", [])}
            sport = teams[i].get("sport", "Soccer")
            if not SPORT_WORDS[sport].search(desc) or not_a_club(desc, sport) or (where and country and not where & country) \
                    or not same_team(teams[i]["en"], en) or (SIDE.search(en) and not SIDE.search(teams[i]["en"])):
                continue
            ar = ((e.get("labels") or {}).get("ar") or {}).get("value", "")
            title = ((e.get("sitelinks") or {}).get("arwiki") or {}).get("title", "")
            if clean_ar(ar) or clean_ar(title):
                found[i] = (ar, title, en, "name")
                break


def curated():
    """teams-ar.json by name key: Saudi clubs come first in it, so a Saudi club takes the first name for its key
    ("Al-Faisaly" is Saudi, "Al Faisaly" Jordanian) and any other club the last."""
    names = list(json.loads((ROOT / "assets/shared/teams-ar.json").read_text(encoding="utf-8")).items())
    last = {team_key(en): ar for en, ar in names}
    first = {team_key(en): ar for en, ar in reversed(names)}
    saudi = {c["article"]: c["ar"] for c in json.loads((ROOT / ".github/tools/saudi-clubs-ar.json")
                                                       .read_text(encoding="utf-8"))}
    return first, last, saudi


def is_saudi(t):
    return any(l in LEAGUES and LEAGUES[l]["country"] == "Saudi Arabia" for l in t["leagues"])


ARAB = {"Saudi Arabia", "Egypt", "United Arab Emirates", "Qatar"}   # their clubs are never named by transliteration


def arabic_speaking(t):
    return NATIONAL_LEAGUE in t["leagues"] or any(LEAGUES.get(l, {}).get("country") in ARAB for l in t["leagues"])


def arabic_names(teams, wd, report):
    """teams-ar.json first, on every run (nothing in the cache or on Wikidata overrides it), then Wikidata, then
    TheSportsDB's Arabic name; a transliteration only for clubs abroad. An Arab club or national team with none of
    these is left without a name ("arFrom": "missing") and fails the run, for someone to add it to teams-ar.json."""
    first, last, saudi = curated()
    for i, t in teams.items():
        w = wd.get(i)
        alt_ar = [clean_ar(a) for a in (t.get("lookup") or {}).get("alt", []) if re.search("[؀-ۿ]", a)]
        options = []
        if NATIONAL_LEAGUE in t["leagues"]:
            options.append((t.get("ar", ""), "curated"))
        if is_saudi(t):
            options += [(first.get(team_key(t["en"]), ""), "curated"), (saudi.get(w[1], "") if w else "", "curated")]
        else:
            options.append((last.get(team_key(t["en"]), ""), "curated"))
        if w:
            options += [(clean_ar(w[0]), "wikidata"), (clean_ar(w[1]), "wikidata")]
        options += [(a, "thesportsdb") for a in alt_ar]
        if not arabic_speaking(t):
            options.append((translit(t["en"]), "translit"))
        t["ar"], t["arFrom"] = next(((a, src) for a, src in options if a), ("", "missing"))
    report["Arabic names from"] = {src: sum(t["arFrom"] == src for t in teams.values())
                                   for src in ("curated", "wikidata", "thesportsdb", "translit", "missing")}


def country_adj(t):
    return next((LEAGUES[l]["adj"] for l in t["leagues"] if l in LEAGUES), "")


def unique_names(teams, report, problems):
    """No two teams share an Arabic name: national teams and Saudi clubs keep theirs, others add their country."""
    def groups():
        g = {}
        for i, t in teams.items():
            if t["ar"]:
                g.setdefault(plain_ar(t["ar"]), []).append(i)
        return [v for v in g.values() if len(v) > 1]
    for ids in groups():   # the same name in two sports: the one that is not football says its sport
        if len({teams[i].get("sport", "Soccer") for i in ids}) > 1:
            for i in ids:
                t = teams[i]
                if t.get("sport", "Soccer") != "Soccer":
                    before, t["ar"] = t["ar"], f"{t['ar']} ({SPORT_AR.get(t['sport'], t['sport'])})"
                    report["names told apart"].append(f"{i} {t['en']}: {before} -> {t['ar']}")
    for ids in groups():
        national = [i for i in ids if NATIONAL_LEAGUE in teams[i]["leagues"]]
        saudi = [i for i in ids if i not in national and is_saudi(teams[i])]
        keep = (national or saudi)[:1]
        for i in ids:
            if i in keep:
                continue
            t, before = teams[i], teams[i]["ar"]
            if national:
                t["ar"] = "نادي " + t["ar"]
            elif country_adj(t) and not t["ar"].endswith(country_adj(t)):
                t["ar"] = f"{t['ar']} {country_adj(t)}"
            report["names told apart"].append(f"{i} {t['en']}: {before} -> {t['ar']}")
    for ids in groups():   # still the same: two clubs of one country, or a name that already had its country
        for i in ids[1:]:
            t, before = teams[i], teams[i]["ar"]
            t["ar"] = f"{t['ar']} ({translit(t['en'])})"
            report["names told apart"].append(f"{i} {t['en']}: {before} -> {t['ar']}")
    for ids in groups():
        problems.append("one Arabic name for several teams: " + ", ".join(f"{i} {teams[i]['en']}" for i in ids))


# ---------- review ----------

def diff(old, new):
    """Added, removed and renamed teams, and teams that changed leagues, for the monthly review."""
    o, n = {t["id"]: t for t in old}, {t["id"]: t for t in new}
    brief = lambda t: {k: t.get(k) for k in ("id", "en", "ar", "sport", "leagues")}   # noqa: E731
    return {
        "added": [brief(n[i]) for i in n if i not in o],
        "removed": [brief(o[i]) for i in o if i not in n],
        "renamed": [{"id": i, "en": [o[i]["en"], n[i]["en"]], "ar": [o[i].get("ar"), n[i]["ar"]]}
                    for i in n if i in o and (o[i]["en"] != n[i]["en"] or o[i].get("ar") != n[i]["ar"])],
        "leagues changed": [{"id": i, "en": n[i]["en"], "leagues": [o[i]["leagues"], n[i]["leagues"]]}
                            for i in n if i in o and o[i]["leagues"] != n[i]["leagues"]],
    }


def diff_md(d, old_count, new_count):
    lines = [f"# Clubs list: {old_count} -> {new_count} teams", ""]
    for kind in ("added", "removed"):
        lines += [f"## {kind.capitalize()} ({len(d[kind])})", ""]
        lines += [f"- {t['id']} {t['en']} «{t['ar']}» ({', '.join(t['leagues'])})" for t in d[kind]] or ["- none"]
        lines.append("")
    lines += [f"## Renamed ({len(d['renamed'])})", ""]
    lines += [f"- {t['id']} {' -> '.join(dict.fromkeys(t['en']))} «{t['ar'][0]}» -> «{t['ar'][1]}»"
              for t in d["renamed"]] or ["- none"]
    lines += ["", f"## Leagues changed ({len(d['leagues changed'])})", ""]
    lines += [f"- {t['id']} {t['en']}: {', '.join(t['leagues'][0])} -> {', '.join(t['leagues'][1])}"
              for t in d["leagues changed"]] or ["- none"]
    return "\n".join(lines) + "\n"


def alt_names(t):
    names = [t.get("lookup", {}).get("name", "")] + t.get("lookup", {}).get("alt", []) + [t.get("wd_en", "")]
    out = []
    for a in names:
        a = a.strip()
        if a and not re.search("[؀-ۿ]", a) and team_key(a) != team_key(t["en"]) \
                and team_key(a) not in {team_key(x) for x in out}:
            out.append(a)
    return out[:4]


# ---------- the run: one part per league and one for the national teams (CI runs them side by side), then merge

def prefs_leagues():
    return re.findall(r'\{"([^"]+)", "[^"]+"\}', PREFS.read_text(encoding="utf-8").split("LEAGUES = {")[1].split("};")[0])


def slug(name):
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def plan():
    """The parts, for the workflow's matrix: every league of Prefs.LEAGUES, the national teams, then the events."""
    return [{"name": n, "slug": slug(n)} for n in prefs_leagues() + [NATIONAL_LEAGUE, EVENTS_PART]]


# ---------- events: races, cards and tournaments, not teams

EVENTS_PART = "Events"
# the page's #events= key -> how TheSportsDB names its leagues (all_leagues.php); tennis also needs strSport "Tennis"
EVENT_LEAGUES = {
    "f1": re.compile(r"^formula (?:1|one)$", re.I),
    "ufc": re.compile(r"^(?:ufc|ultimate fighting championship)$", re.I),
    "boxing": re.compile(r"^(?:pro(?:fessional)? )?boxing$", re.I),
    "tennis": re.compile(r"\b(?:atp|wta)\b", re.I),
}
EVENT_LOOSE = re.compile(r"formula|ufc|mma|boxing|\batp\b|\bwta\b|tennis|grand slam", re.I)   # for the report
EVENT_FIELDS = ("strEvent", "strEventAlternate", "strLeague", "strHomeTeam", "strAwayTeam", "dateEvent", "strTime",
                "strTimestamp", "strVenue", "strCity", "strCountry", "strStatus", "strSeason", "intRound")
MAX_TENNIS = 3
# TheSportsDB league ids around the ones events live at (Formula 1 4370, UFC 4443, ATP/WTA 4464/4517...): which is
# which is read from what each answers, never assumed
EVENT_IDS = [4370, 4443, 4444, 4445, 4446, 4447, 4448, 4449, 4450, 4464, 4465, 4466, 4467, 4468, 4517, 4518, 4519, 4520]


def event_sample(events, n=6):   # what a league answers, for reading the report (the screen parses these fields)
    return [{k: e.get(k) for k in EVENT_FIELDS if e.get(k) not in (None, "")} for e in (events or [])[:n]]


def collect_events(report, problems):
    """{key: [{id, name}]} for #events=: each wanted league found by name, tennis only if it has upcoming events.
    Also looks for a Saudi basketball league. The report keeps what each league answered."""
    leagues = list(get("all_leagues.php").get("leagues") or [])
    notes = report.setdefault("events", {})
    notes["all_leagues.php gave"] = len(leagues)
    # the free key's list holds 10 leagues: look the likely ids up one by one, as the report shows
    notes["lookupleague.php"] = {}
    for i in EVENT_IDS:
        got = (get(f"lookupleague.php?id={i}").get("leagues") or [{}])[0]
        notes["lookupleague.php"][str(i)] = [got.get("strLeague"), got.get("strSport")]
        if got.get("strLeague") and got["idLeague"] not in {l.get("idLeague") for l in leagues}:
            leagues.append(got)
    notes["leagues that look like events"] = [{"id": l["idLeague"], "name": l["strLeague"], "sport": l.get("strSport")}
                                              for l in leagues if EVENT_LOOSE.search(l.get("strLeague", ""))][:60]
    found = {}
    for key, rx in EVENT_LEAGUES.items():
        hits = [l for l in leagues if rx.search(l.get("strLeague", "")) and (key != "tennis" or l.get("strSport") == "Tennis")]
        notes[key] = []
        for l in hits[:8]:
            nxt = get(f"eventsnextleague.php?id={l['idLeague']}").get("events") or []
            past = get(f"eventspastleague.php?id={l['idLeague']}").get("events") or []
            notes[key].append({"id": l["idLeague"], "name": l["strLeague"], "sport": l.get("strSport"),
                               "upcoming": len(nxt), "past": len(past), "next": event_sample(nxt),
                               "last": event_sample(past, 4)})
            if key != "tennis" or nxt:
                found.setdefault(key, []).append({"id": str(l["idLeague"]), "name": l["strLeague"]})
        if key == "tennis":
            found["tennis"] = found.get("tennis", [])[:MAX_TENNIS]
            if not found["tennis"]:
                notes["tennis"].append("skipped: no ATP/WTA league with upcoming events on the free API")
                found.pop("tennis")
        elif key not in found:
            problems.append(f"events: no TheSportsDB league named like {key} ({rx.pattern})")
        elif key != "boxing":
            found[key] = found[key][:1]   # one league is the sport; boxing may hold several promotions' leagues
    saudi = [{"id": l["idLeague"], "name": l["strLeague"]} for l in leagues
             if l.get("strSport") == "Basketball" and re.search("saudi", l.get("strLeague", ""), re.I)]
    if not saudi:
        for l in get("search_all_leagues.php?" + urllib.parse.urlencode({"c": "Saudi Arabia", "s": "Basketball"})) \
                .get("countries") or []:
            saudi.append({"id": l["idLeague"], "name": l["strLeague"]})
    notes["saudi basketball"] = saudi or "no Saudi basketball league on TheSportsDB"
    return found


def new_report():
    return {"problems": [], "clubs per league": {}, "national teams found in their matches": [],
            "national teams not found": [], "id checks": {"checked before": 0, "same team": 0, "another name": [],
                                                           "no answer": []}}


def collect_part(part, today, cache, listed):
    """One league's clubs (or the national teams), each id checked: {part, teams, report, ...}."""
    started, before = time.time(), dict(STATS)
    report, teams, out = new_report(), {}, {"part": part, "started": started}
    try:
        if part == NATIONAL_LEAGUE:
            national = {}
            for en, ar in NATIONAL.items():
                hit = find_national(en, teams, report, cache, listed)
                if hit:
                    national[en] = list(hit)
                    teams.setdefault(hit[0], {"en": hit[1], "leagues": [], "sport": "Soccer"})
                    teams[hit[0]].update(ar=ar)
                    teams[hit[0]]["leagues"].append(NATIONAL_LEAGUE)
            out["national"] = national
            report["problems"] += [f"national team not found: {x}" for x in report["national teams not found"]]
            print(len(national), "of", len(NATIONAL), "national teams", flush=True)
        elif part == EVENTS_PART:
            out["events"] = collect_events(report, report["problems"])
            print({k: [l["name"] for l in v] for k, v in out["events"].items()}, flush=True)
        elif part not in LEAGUES:
            report["problems"].append(f"{part} is in Prefs.LEAGUES but not in team_ids.py's LEAGUES (country, minimum)")
        else:
            ids = league_ids([part], cache["leagues"])
            if part in ids and LEAGUES[part].get("cup"):
                print(f"{part}: a cup, its id only ({ids[part]}): no clubs of its own", flush=True)   # 4.3: its clubs are the national leagues'
            elif part in ids:
                collect_league(part, ids[part], today, teams, report, report["problems"])
                if len(report["clubs per league"][part]["seasons"]) < 2 and part in cache["leagues"]:
                    ids = league_ids([part])   # a reused league id that no longer answers: find it again
                    report["problems"].clear()
                    teams.clear()
                    collect_league(part, ids[part], today, teams, report, report["problems"])
            if part in ids:
                out["league id"] = ids[part]
            else:
                report["problems"].append(f"league not found on TheSportsDB: {part}")
        verify(teams, report, cache, listed, today.isoformat())
    except Exception as e:  # noqa: BLE001  (written down, so the merge names the part and why)
        report["problems"].append(f"{part}: the collection stopped: {e!r}"[:400])
        out["error"] = repr(e)[:400]
    out.update(teams=teams, report=report, requests={k: STATS[k] - before[k] for k in STATS},
               minutes=round((time.time() - started) / 60, 1))
    print(f"{part}: {len(teams)} teams, {out['requests']['thesportsdb']} TheSportsDB requests in {out['minutes']} "
          f"minutes", flush=True)
    return out


def merge(parts, today, cache, started=None):
    """Every part into one list: Arabic names (Wikidata here, on one runner), every check, the files. Returns the
    problems found."""
    leagues = prefs_leagues()
    report = {"date": today.isoformat(), "full run": FULL, "problems": [], "requests": {"per part": {}},
              **{k: v for k, v in new_report().items() if k != "problems"},
              "wikidata": {"ids on several or unlike items": []}, "names told apart": []}
    problems = report["problems"]
    by_part = {p["part"]: p for p in parts}
    teams, national, league_id = {}, {}, {}
    events = {}
    for name in leagues + [NATIONAL_LEAGUE, EVENTS_PART]:
        p = by_part.get(name)
        if p is None:
            problems.append(f"{name}: no data from its collection (the job failed before writing it)")
            continue
        r = p["report"]
        problems += r["problems"]
        report["clubs per league"].update(r["clubs per league"])
        for k in ("national teams found in their matches", "national teams not found"):
            report[k] += r[k]
        for k, v in r["id checks"].items():
            report["id checks"][k] += v
        for k in ("ids checked with", "found in their own matches"):
            if k in r:
                report.setdefault(k, {})[name] = r[k]
        report["requests"]["per part"][name] = {"thesportsdb": p["requests"]["thesportsdb"],
                                                "rate limited (429)": p["requests"]["rate limited (429)"],
                                                "minutes": p["minutes"]}
        national.update(p.get("national", {}))
        events.update(p.get("events", {}))
        if "events" in r:
            report["events"] = r["events"]
        if "league id" in p:
            league_id[name] = p["league id"]
        for i, t in p["teams"].items():
            have = teams.setdefault(i, {**t, "leagues": []})
            have["leagues"] += [l for l in t["leagues"] if l not in have["leagues"]]
            if t.get("ar"):
                have["ar"] = t["ar"]
    wrong = {i: n for i, n in KNOWN.items() if team_key(teams.get(i, {}).get("en", "")) != team_key(n)}
    if wrong:
        problems.append(f"these ids didn't come out right: {wrong}")
    before = dict(STATS)
    wd = wikidata_arabic(teams, report, cache)
    for i, w in wd.items():
        teams[i]["wd_en"] = w[2]
    arabic_names(teams, wd, report)
    unique_names(teams, report, problems)
    problems += [f"no Arabic name for {t['en']} ({i}, {', '.join(t['leagues'])}): add it to assets/shared/teams-ar.json"
                 for i, t in teams.items() if t["arFrom"] == "missing"]
    order = leagues + [NATIONAL_LEAGUE]
    out = [{"id": i, "en": t["en"], "ar": t["ar"], "arFrom": t["arFrom"],
            "sport": t.get("sport", "Soccer"), "leagues": t["leagues"], "alt": alt_names(t)}
           for i, t in teams.items()]
    out.sort(key=lambda t: (order.index(t["leagues"][0]), t["en"]))
    new_cache = {"leagues": league_id, "national": national,
                 "teams": {i: {"en": t["en"], "checked": t.get("checked", ""), "lookup": t.get("lookup"),
                               "wd": list(wd[i]) if i in wd else None, "arFrom": t["arFrom"]}
                           for i, t in sorted(teams.items())}}
    report["teams"] = len(out)
    report["requests"]["thesportsdb"] = sum(p["requests"]["thesportsdb"] for p in parts)
    report["requests"]["wikidata and wikipedia"] = STATS["wikidata and wikipedia"] - before["wikidata and wikipedia"]
    report["requests"]["slowest part, minutes"] = max([p["minutes"] for p in parts] or [0])
    first = min([p["started"] for p in parts] or [time.time()]) if started is None else started
    report["requests"]["wall minutes"] = round((time.time() - first) / 60, 1)
    try:
        old = json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))
    except (FileNotFoundError, ValueError):
        old = []
    d = diff(old, out)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "teams.json").write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (OUT / "events.json").write_text(json.dumps(events, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (OUT / "teams-cache.json").write_text(json.dumps(new_cache, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    (OUT / "diff.json").write_text(json.dumps(d, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (OUT / "diff.md").write_text(diff_md(d, len(old), len(out)), encoding="utf-8")
    (OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k not in ("clubs per league", "names told apart")},
                     ensure_ascii=False, indent=1))
    print({k: len(v) for k, v in d.items()})
    print(f"{report['requests']['thesportsdb']} TheSportsDB requests; {report['requests']['wall minutes']} minutes")
    return problems


def write_part(p):
    (OUT / "parts").mkdir(parents=True, exist_ok=True)
    (OUT / "parts" / f"{slug(p['part'])}.json").write_text(json.dumps(p, ensure_ascii=False) + "\n", encoding="utf-8")


def main(argv=None):
    """team_ids.py: every part, then the merge, in one process (tests, local runs).
    team_ids.py plan | collect --part NAME | merge: the workflow's three steps (one collect per runner)."""
    argv = sys.argv[1:] if argv is None else argv
    started, today = time.time(), datetime.date.today()
    if argv[:1] == ["plan"]:
        print(json.dumps(plan()))
        return
    cache, listed = load_cache()
    if argv[:1] == ["collect"]:
        name = argv[argv.index("--part") + 1]
        p = collect_part(name, today, cache, listed)
        write_part(p)
        if p["report"]["problems"]:
            print("\nPROBLEMS:\n- " + "\n- ".join(p["report"]["problems"]))
        sys.exit(1 if "error" in p else 0)   # a short league is the merge's to report; a crash is this job's
    if argv[:1] == ["merge"]:
        parts = [json.loads(f.read_text(encoding="utf-8")) for f in sorted((OUT / "parts").glob("*.json"))]
        problems = merge(parts, today, cache)
    else:
        names = [x["name"] for x in plan()]
        guess = (0 if all(n in cache["leagues"] for n in names[:-2]) else 15) + 5 * (len(names) - 2) + 20 \
            + 3 * sum(en not in cache["national"] for en in NATIONAL) + (0 if listed or cache["teams"] else 420)
        print(f"{'full' if FULL else 'incremental'} run: about {guess} TheSportsDB requests, "
              f"{guess * PACE / 60:.0f} minutes or more in one process", flush=True)
        parts = []
        for name in names:
            parts.append(collect_part(name, today, cache, listed))
            write_part(parts[-1])
        problems = merge(parts, today, cache, started)
    if problems:
        print("\nPROBLEMS:\n- " + "\n- ".join(problems))
        sys.exit(1)


if __name__ == "__main__":
    main()
