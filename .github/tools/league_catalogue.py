#!/usr/bin/env python3
"""4.3 «كل الدوريات»: the catalogue of competitions the leagues picker offers, beside the built-in ones (Prefs.LEAGUES).

TheSportsDB's free key (123) lists 10 leagues in all_leagues.php and at most 5 a country in search_all_leagues.php (the
catalogue probe, data-catalogue-probe), but lookupleague.php answers any id. So CI walks the ids, gently: one request every
GAP seconds, a 429 waits 60 s, then 120, 240..., and BUSY_STOP of them in a row end the run where it is (the next run goes on
from there). Weekly at most (league-catalogue.yml). Nothing here runs on a TV and no key but the free one is used.

What is kept: a competition of Soccer, Basketball or American Football (the sports the page knows how long a game lasts) with
a current season. Content rules: no league of Israel, none named after it, and Western Sahara is never a country (Morocco's).
Names: AR (hand-named Arabic, about 120 of the best known, matched by TheSportsDB's id or English name); the rest in English
until named. Countries: COUNTRY_AR, English for the rest.

Out (a branch data-catalogue holds catalogue/):
  catalogue/catalogue.json  {"updated", "count", "leagues": [{"id", "en", "ar", "country", "country_ar", "sport", "top"}]}
                            sorted: the hand-named first (in AR's order), then by country and name
  catalogue/state.json      {"cursor", "seen": {id: entry or null}, "high"}: where the walk is, for the next run
  catalogue/report.md       how many, what was asked, which hand names matched nothing

    CATALOGUE_PREV=<dir with the last catalogue/> python3 .github/tools/league_catalogue.py
"""
import datetime
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://www.thesportsdb.com/api/v1/json/123/"
GAP = 3.0                 # seconds between requests: the free key allows 30 a minute; 429s were seen at 2.5 s
BUSY_STOP = 5             # 429s in a row: the run ends here, the next one goes on
LOW = 4320                # TheSportsDB's league ids start at 4328 (English Premier League)
MISS_TAIL = 400           # ids past the highest league found before the walk turns back to LOW
BUDGET_S = int(os.environ.get("CATALOGUE_BUDGET_S", str(270 * 60)))   # the job's time (300 min), less the push
OUT = Path(os.environ.get("CATALOGUE_OUT", "catalogue"))
PREV = Path(os.environ["CATALOGUE_PREV"]) if os.environ.get("CATALOGUE_PREV") else None
SPORTS = {"Soccer", "Basketball", "American Football"}

# the owner's content rules (as cities_world: never «إسرائيل», never Western Sahara as a country)
BANNED_COUNTRY = re.compile(r"^\s*israel\s*$", re.I)
BANNED_NAME = re.compile(r"israel", re.I)
COUNTRY_FIX = {"Western Sahara": "Morocco"}

# Hand-named Arabic, the best-known competitions first. Key: TheSportsDB's id when known, else its English name (matched
# loosely: case, punctuation and spaces aside). The report lists any key that matched nothing, to fix by hand.
AR = [
    ("4668", "دوري روشن السعودي"), ("5627", "دوري يلو (الدرجة الأولى)"), ("Saudi King Cup", "كأس خادم الحرمين الشريفين"),
    ("Saudi Super Cup", "كأس السوبر السعودي"), ("Saudi Second Division", "دوري الدرجة الثانية السعودي"),
    ("4328", "الدوري الإنجليزي"), ("4329", "دوري البطولة الإنجليزية"), ("English League 1", "دوري الدرجة الأولى الإنجليزي"),
    ("English League 2", "دوري الدرجة الثانية الإنجليزي"), ("English FA Cup", "كأس الاتحاد الإنجليزي"), ("English League Cup", "كأس الرابطة الإنجليزية"),
    ("FA Community Shield", "درع المجتمع الإنجليزي"), ("English Womens Super League", "الدوري الإنجليزي للسيدات"),
    ("4335", "الدوري الإسباني"), ("Spanish La Liga 2", "الدوري الإسباني الدرجة الثانية"), ("Spanish Copa del Rey", "كأس ملك إسبانيا"),
    ("Spanish Super Cup", "كأس السوبر الإسباني"),
    ("4332", "الدوري الإيطالي"), ("Italian Serie B", "الدوري الإيطالي الدرجة الثانية"), ("Italian Coppa Italia", "كأس إيطاليا"),
    ("Italian Super Cup", "كأس السوبر الإيطالي"),
    ("4331", "الدوري الألماني"), ("German 2. Bundesliga", "الدوري الألماني الدرجة الثانية"), ("German DFB Pokal", "كأس ألمانيا"),
    ("German Super Cup", "كأس السوبر الألماني"),
    ("4334", "الدوري الفرنسي"), ("French Ligue 2", "الدوري الفرنسي الدرجة الثانية"), ("French Coupe de France", "كأس فرنسا"),
    ("French Trophée des Champions", "كأس الأبطال الفرنسي"),
    ("4344", "الدوري البرتغالي"), ("Portuguese Taca de Portugal", "كأس البرتغال"), ("4337", "الدوري الهولندي"), ("Dutch KNVB Cup", "كأس هولندا"),
    ("4339", "الدوري التركي"), ("Turkish Cup", "كأس تركيا"), ("4330", "الدوري الإسكتلندي"), ("Belgian Pro League", "الدوري البلجيكي"),
    ("Greek Superleague Greece", "الدوري اليوناني"), ("Russian Football Premier League", "الدوري الروسي"), ("Swiss Super League", "الدوري السويسري"),
    ("Austrian Football Bundesliga", "الدوري النمساوي"), ("Danish Superliga", "الدوري الدنماركي"), ("Norwegian Eliteserien", "الدوري النرويجي"),
    ("Swedish Allsvenskan", "الدوري السويدي"), ("Ukrainian Premier League", "الدوري الأوكراني"), ("Croatian First Football League", "الدوري الكرواتي"),
    ("Serbian SuperLiga", "الدوري الصربي"), ("Polish Ekstraklasa", "الدوري البولندي"), ("Czech First League", "الدوري التشيكي"),
    ("4480", "دوري أبطال أوروبا"), ("UEFA Europa League", "الدوري الأوروبي"), ("UEFA Conference League", "دوري المؤتمر الأوروبي"),
    ("UEFA Europa Conference League", "دوري المؤتمر الأوروبي"), ("UEFA Super Cup", "كأس السوبر الأوروبي"), ("UEFA Nations League", "دوري الأمم الأوروبية"),
    ("UEFA European Championships", "كأس الأمم الأوروبية"), ("UEFA European Championship Qualifying", "تصفيات كأس الأمم الأوروبية"),
    ("UEFA Womens Champions League", "دوري أبطال أوروبا للسيدات"),
    ("FIFA World Cup", "كأس العالم"), ("FIFA World Cup Qualifying UEFA", "تصفيات كأس العالم (أوروبا)"),
    ("FIFA World Cup Qualifying AFC", "تصفيات كأس العالم (آسيا)"), ("FIFA World Cup Qualifying CAF", "تصفيات كأس العالم (أفريقيا)"),
    ("FIFA World Cup Qualifying CONMEBOL", "تصفيات كأس العالم (أمريكا الجنوبية)"), ("FIFA World Cup Qualifying CONCACAF", "تصفيات كأس العالم (الكونكاكاف)"),
    ("FIFA Club World Cup", "كأس العالم للأندية"), ("FIFA Intercontinental Cup", "كأس القارات للأندية"), ("FIFA Womens World Cup", "كأس العالم للسيدات"),
    ("FIFA U-20 World Cup", "كأس العالم للشباب"), ("FIFA U-17 World Cup", "كأس العالم للناشئين"), ("FIFA Arab Cup", "كأس العرب"),
    ("Olympics Soccer", "كرة القدم في الألعاب الأولمبية"), ("International Friendlies", "مباريات ودية دولية"), ("Club Friendlies", "مباريات ودية للأندية"),
    ("4719", "دوري أبطال آسيا للنخبة"), ("AFC Champions League Two", "دوري أبطال آسيا 2"), ("AFC Asian Cup", "كأس آسيا"),
    ("AFC Asian Cup Qualifying", "تصفيات كأس آسيا"), ("AFC U23 Asian Cup", "كأس آسيا تحت 23 سنة"), ("AFC Challenge League", "دوري التحدي الآسيوي"),
    ("Arabian Gulf Cup", "كأس الخليج العربي"), ("Arab Club Champions Cup", "البطولة العربية للأندية"), ("WAFF Championship", "بطولة غرب آسيا"),
    ("Africa Cup of Nations", "كأس الأمم الأفريقية"), ("Africa Cup of Nations Qualifying", "تصفيات كأس الأمم الأفريقية"),
    ("CAF Champions League", "دوري أبطال أفريقيا"), ("CAF Confederation Cup", "كأس الكونفدرالية الأفريقية"), ("CAF Super Cup", "كأس السوبر الأفريقي"),
    ("African Nations Championship", "بطولة أمم أفريقيا للمحليين"),
    ("Copa America", "كوبا أمريكا"), ("Copa Libertadores", "كأس ليبرتادوريس"), ("Copa Sudamericana", "كأس سود أمريكانا"),
    ("CONCACAF Champions Cup", "دوري أبطال الكونكاكاف"), ("CONCACAF Gold Cup", "الكأس الذهبية"), ("CONCACAF Nations League", "دوري أمم الكونكاكاف"),
    ("4678", "الدوري الإماراتي"), ("UAE League Cup", "كأس رابطة المحترفين الإماراتية"), ("UAE First Division League", "دوري الدرجة الأولى الإماراتي"),
    ("UAE Presidents Cup", "كأس رئيس الدولة الإماراتي"), ("4663", "الدوري القطري"), ("Qatari Emir Cup", "كأس أمير قطر"), ("Qatar Cup", "كأس قطر"),
    ("Qatari Second Division", "دوري الدرجة الثانية القطري"), ("Kuwaiti Premier League", "الدوري الكويتي"), ("Kuwait Emir Cup", "كأس أمير الكويت"),
    ("Bahraini Premier League", "الدوري البحريني"), ("Omani Professional League", "الدوري العماني"), ("Oman Professional League", "الدوري العماني"),
    ("5056", "دوري نجوم العراق"), ("Jordanian Pro League", "الدوري الأردني"), ("Jordan Premier League", "الدوري الأردني"),
    ("Lebanese Premier League", "الدوري اللبناني"), ("Syrian Premier League", "الدوري السوري"), ("Palestinian West Bank Premier League", "الدوري الفلسطيني"),
    ("Yemeni League", "الدوري اليمني"),
    ("4829", "الدوري المصري"), ("Egypt Cup", "كأس مصر"), ("Egyptian Super Cup", "كأس السوبر المصري"), ("Moroccan Botola Pro", "الدوري المغربي"),
    ("Moroccan Championship", "الدوري المغربي"), ("Algerian Ligue 1", "الدوري الجزائري"), ("Algerian Ligue Professionnelle 1", "الدوري الجزائري"),
    ("Tunisian Ligue Professionnelle 1", "الدوري التونسي"), ("Libyan Premier League", "الدوري الليبي"), ("Sudan Premier League", "الدوري السوداني"),
    ("South African Premier Soccer League", "الدوري الجنوب أفريقي"), ("Nigerian Professional Football League", "الدوري النيجيري"),
    ("Iranian Persian Gulf Pro League", "الدوري الإيراني"), ("Japanese J1 League", "الدوري الياباني"), ("South Korean K League 1", "الدوري الكوري الجنوبي"),
    ("Chinese Super League", "الدوري الصيني"), ("Australian A-League", "الدوري الأسترالي"), ("Indian Super League", "الدوري الهندي"),
    ("Uzbekistan Super League", "الدوري الأوزبكي"), ("Thai League 1", "الدوري التايلاندي"),
    ("4346", "الدوري الأمريكي"), ("US Open Cup", "كأس الولايات المتحدة المفتوحة"), ("Mexican Primera League", "الدوري المكسيكي"),
    ("Brazilian Serie A", "الدوري البرازيلي"), ("Brazilian Serie B", "الدوري البرازيلي الدرجة الثانية"), ("Copa do Brasil", "كأس البرازيل"),
    ("Argentinian Primera Division", "الدوري الأرجنتيني"), ("Copa Argentina", "كأس الأرجنتين"), ("Chilean Primera Division", "الدوري التشيلي"),
    ("Colombian Primera A", "الدوري الكولومبي"), ("Uruguayan Primera Division", "الدوري الأوروغوياني"),
    ("4387", "دوري كرة السلة الأمريكي (NBA)"), ("4391", "دوري كرة القدم الأمريكية (NFL)"), ("Euroleague Basketball", "الدوري الأوروبي لكرة السلة"),
    ("FIBA Basketball World Cup", "كأس العالم لكرة السلة"), ("Saudi Basketball League", "الدوري السعودي لكرة السلة"), ("NCAA Division I Basketball Mens", "دوري الجامعات الأمريكية لكرة السلة"),
    ("WNBA", "دوري كرة السلة الأمريكي للسيدات"), ("NCAA Division 1", "دوري الجامعات الأمريكية لكرة القدم الأمريكية"),
]

COUNTRY_AR = {
    "Saudi Arabia": "السعودية", "United Arab Emirates": "الإمارات", "Qatar": "قطر", "Kuwait": "الكويت", "Bahrain": "البحرين", "Oman": "عُمان",
    "Iraq": "العراق", "Jordan": "الأردن", "Syria": "سوريا", "Lebanon": "لبنان", "Palestine": "فلسطين", "Yemen": "اليمن", "Egypt": "مصر",
    "Morocco": "المغرب", "Algeria": "الجزائر", "Tunisia": "تونس", "Libya": "ليبيا", "Sudan": "السودان", "Iran": "إيران", "Turkey": "تركيا",
    "England": "إنجلترا", "Scotland": "إسكتلندا", "Wales": "ويلز", "Northern Ireland": "أيرلندا الشمالية", "Ireland": "أيرلندا", "Spain": "إسبانيا",
    "Italy": "إيطاليا", "Germany": "ألمانيا", "France": "فرنسا", "Portugal": "البرتغال", "Netherlands": "هولندا", "Belgium": "بلجيكا",
    "Greece": "اليونان", "Russia": "روسيا", "Switzerland": "سويسرا", "Austria": "النمسا", "Denmark": "الدنمارك", "Norway": "النرويج",
    "Sweden": "السويد", "Finland": "فنلندا", "Ukraine": "أوكرانيا", "Croatia": "كرواتيا", "Serbia": "صربيا", "Poland": "بولندا",
    "Czech Republic": "التشيك", "Romania": "رومانيا", "Hungary": "المجر", "Bulgaria": "بلغاريا", "Cyprus": "قبرص", "Japan": "اليابان",
    "South Korea": "كوريا الجنوبية", "China": "الصين", "Australia": "أستراليا", "India": "الهند", "Uzbekistan": "أوزبكستان", "Thailand": "تايلاند",
    "Indonesia": "إندونيسيا", "Malaysia": "ماليزيا", "United States": "الولايات المتحدة", "USA": "الولايات المتحدة", "Canada": "كندا",
    "Mexico": "المكسيك", "Brazil": "البرازيل", "Argentina": "الأرجنتين", "Chile": "تشيلي", "Colombia": "كولومبيا", "Uruguay": "الأوروغواي",
    "Peru": "بيرو", "Ecuador": "الإكوادور", "Paraguay": "باراغواي", "South Africa": "جنوب أفريقيا", "Nigeria": "نيجيريا", "Senegal": "السنغال",
    "Ghana": "غانا", "Cameroon": "الكاميرون", "Ivory Coast": "ساحل العاج", "Worldwide": "عالمي", "Europe": "أوروبا", "Asia": "آسيا",
    "Africa": "أفريقيا", "South America": "أمريكا الجنوبية", "North America": "أمريكا الشمالية",
}

stats = {"requests": 0, "429": 0, "errors": 0}


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", str(s or "").lower())


def allowed(row):
    """The content rules and the sports kept: the row as TheSportsDB gives it."""
    if not row or not row.get("idLeague") or not row.get("strLeague"):
        return False
    if row.get("strSport") not in SPORTS:
        return False
    if BANNED_COUNTRY.match(str(row.get("strCountry") or "")):
        return False
    if BANNED_NAME.search(str(row.get("strLeague") or "")) or BANNED_NAME.search(str(row.get("strLeagueAlternate") or "")):
        return False
    return bool(str(row.get("strCurrentSeason") or "").strip())


def entry(row):
    country = str(row.get("strCountry") or "").strip()
    country = COUNTRY_FIX.get(country, country)
    return {"id": str(row["idLeague"]), "en": str(row["strLeague"]).strip()[:80], "country": country[:40], "sport": row.get("strSport")}


def name(e):
    """(ar, rank) for an entry: the hand name and its place in AR, else ("", None)."""
    for i, (k, ar) in enumerate(AR):
        if k == e["id"] or (not k.isdigit() and norm(k) == norm(e["en"])):
            return ar, i
    return "", None


def build(seen):
    """The catalogue from the walk's findings: content rules applied once more (an older state), names, order."""
    out = []
    for e in seen.values():
        if not e or e.get("sport") not in SPORTS or BANNED_COUNTRY.match(e.get("country") or "") or BANNED_NAME.search(e.get("en") or ""):
            continue
        e = dict(e, country=COUNTRY_FIX.get(e.get("country"), e.get("country")))
        ar, rank = name(e)
        out.append(dict(e, ar=ar, country_ar=COUNTRY_AR.get(e["country"], ""), top=rank is not None, _r=rank))
    out.sort(key=lambda e: (e["_r"] is None, e["_r"] if e["_r"] is not None else 0, e["country"], e["en"]))
    for e in out:
        del e["_r"]
    return out


def unmatched(cat):
    have = {e["id"] for e in cat} | {norm(e["en"]) for e in cat}
    return [k for k, _ in AR if (k if k.isdigit() else norm(k)) not in have]


def get(path, **q):
    """One answer, None when there is none; raises Busy after BUSY_STOP 429s in a row."""
    url = BASE + path + ("?" + urllib.parse.urlencode(q) if q else "")
    wait = 60
    for _ in range(BUSY_STOP):
        time.sleep(GAP)
        stats["requests"] += 1
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "UrTV-catalogue"}), timeout=30) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                stats["429"] += 1
                time.sleep(wait)
                wait = min(wait * 2, 600)
                continue
            stats["errors"] += 1
            return None
        except Exception:   # noqa: BLE001 - a network hiccup: this id is asked again next run
            stats["errors"] += 1
            return None
    raise Busy()


class Busy(Exception):
    pass


def load_prev():
    if PREV and (PREV / "state.json").is_file():
        try:
            return json.loads((PREV / "state.json").read_text(encoding="utf-8"))
        except ValueError:
            pass
    return {"cursor": LOW, "seen": {}, "high": LOW}


def walk(state, budget_s=BUDGET_S, ask=None):
    """Asks ids from the cursor on, until the time is up or the server is busy; wraps to LOW past high + MISS_TAIL."""
    ask = ask or (lambda i: get("lookupleague.php", id=i))
    seen, cur, high = state["seen"], int(state["cursor"]), int(state["high"])
    started, stop = time.time(), None
    asked = 0
    while time.time() - started < budget_s:
        if cur > high + MISS_TAIL:
            cur = LOW
            if asked:   # a whole round in one run: done for this week
                stop = "round"
                break
        try:
            got = ask(cur)
        except Busy:
            stop = "busy"
            break
        asked += 1
        if got is not None:
            row = ((got or {}).get("leagues") or [None])[0]
            if row and str(row.get("idLeague")) == str(cur):
                high = max(high, cur)
                seen[str(cur)] = entry(row) if allowed(row) else None
            else:
                seen.pop(str(cur), None)
        cur += 1
    return {"cursor": cur, "seen": seen, "high": high}, {"asked": asked, "stop": stop or "time"}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    state, run = walk(load_prev())
    cat = build(state["seen"])
    now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    (OUT / "catalogue.json").write_text(json.dumps({"updated": now, "count": len(cat), "leagues": cat}, ensure_ascii=False, separators=(",", ":")) + "\n",
                                         encoding="utf-8")
    (OUT / "state.json").write_text(json.dumps(state, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    miss = unmatched(cat)
    rep = [f"# League catalogue, {now}", "", f"- {len(cat)} competitions ({sum(e['top'] for e in cat)} hand-named in Arabic)",
           f"- this run: {run['asked']} ids asked, ended by {run['stop']}; next from id {state['cursor']}; highest league id {state['high']}",
           f"- requests {stats['requests']}, 429s {stats['429']}, errors {stats['errors']}", "",
           "Hand names that matched nothing (fix the key in AR):", ""] + [f"- {k}" for k in miss]
    (OUT / "report.md").write_text("\n".join(rep) + "\n", encoding="utf-8")
    print("\n".join(rep))
    return 0


if __name__ == "__main__":
    sys.exit(main())
