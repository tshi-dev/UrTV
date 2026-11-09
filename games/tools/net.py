"""Tiny cached fetcher for the question-bank builders. Honors Retry-After. Cache dir: env GAMES_CACHE."""
import hashlib, json, os, ssl, time, urllib.error, urllib.parse, urllib.request

UA = "urtv-games-data/1.0 (family quiz fact verification; contact via repository)"
CACHE = os.environ.get("GAMES_CACHE", "/tmp/games-cache")
os.makedirs(CACHE, exist_ok=True)
_ctx = ssl.create_default_context(cafile=os.environ.get("SSL_CERT_FILE") or None)

def get(url, accept="application/json"):
    key = os.path.join(CACHE, hashlib.sha256(url.encode()).hexdigest())
    if os.path.exists(key):
        return open(key, encoding="utf-8").read()
    for attempt in range(12):
        req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": accept})
        try:
            with urllib.request.urlopen(req, timeout=60, context=_ctx) as r:
                body = r.read().decode("utf-8")
            open(key, "w", encoding="utf-8").write(body)
            time.sleep(1.5)
            return body
        except urllib.error.HTTPError as e:
            if e.code in (429, 503):
                wait = int(e.headers.get("Retry-After") or 30)
                time.sleep(min(wait, 120) + 2)
                continue
            raise
    raise RuntimeError("giving up on " + url)

def sparql(q):
    u = "https://query.wikidata.org/sparql?format=json&query=" + urllib.parse.quote(q)
    return json.loads(get(u))["results"]["bindings"]

def wiki_intro(lang, titles):
    """{title: intro plain text} for up to 20 titles in one request."""
    out = {}
    for i in range(0, len(titles), 20):
        chunk = titles[i:i + 20]
        p = {"action": "query", "prop": "extracts", "exintro": 1, "explaintext": 1, "redirects": 1,
             "exlimit": "max", "format": "json", "titles": "|".join(chunk)}
        d = json.loads(get(f"https://{lang}.wikipedia.org/w/api.php?" + urllib.parse.urlencode(p)))["query"]
        norm = {n["to"]: n["from"] for n in d.get("normalized", [])}
        red = {r["to"]: r["from"] for r in d.get("redirects", [])}
        for pg in d["pages"].values():
            t = pg.get("title")
            orig = norm.get(red.get(t, t), red.get(t, t))
            out[orig] = pg.get("extract", "")
            out[t] = pg.get("extract", "")
    return out

def wiki_full(lang, title):
    p = {"action": "query", "prop": "extracts", "explaintext": 1, "redirects": 1, "format": "json", "titles": title}
    d = json.loads(get(f"https://{lang}.wikipedia.org/w/api.php?" + urllib.parse.urlencode(p)))["query"]["pages"]
    return next(iter(d.values())).get("extract", "")
