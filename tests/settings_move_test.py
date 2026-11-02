#!/usr/bin/env python3
"""optimize/saver.sh export / import («نقل الإعدادات لتلفزيون ثاني»), against a fake TV that keeps its settings in a file and
answers like the apps do (GET, SET, GET_PLAYER and the adhan broadcasts). No TV needed.

    python3 tests/settings_move_test.py

The module runs with PY_TV (the test-only override of the Python that runs tv.py) pointed at the fake and SETTINGS_BEFORE at a temporary file,
so the real optimize/run is never touched.
"""
import copy
import importlib.util
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE = ROOT / "optimize/settings_move.py"
tmp = Path(tempfile.mkdtemp())
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


# ---- the fake TV: its state is a JSON file; every command it gets is logged
FAKE = r'''#!/usr/bin/env python3
import json, os, re, sys
cmd = sys.argv[3]
st_path, log = os.environ["FAKE_TV"], os.environ["FAKE_LOG"]
st = json.load(open(st_path))
open(log, "a").write(cmd + "\n")
def out(text=None):
    if text is None:
        print("Broadcast completed: result=0")
    else:
        print('Broadcast completed: result=0, data="' + text + '"')
    sys.exit(0)
for sub in list(st.get("fail_once", [])):  # the TV does not answer at all, once, for a command with this in it
    if sub in cmd:
        st["fail_once"].remove(sub)
        json.dump(st, open(st_path, "w"))
        sys.exit(1)
for bad in st.get("refuse", []):          # a value the TV refuses (answers error:)
    if bad in cmd:
        out("error: refused by the fake TV: " + bad)
ENUM = {"digits": ["hindi", "latin"], "clocksize": ["large", "medium", "small"], "mkstyle": ["arrows", "prices"],
        "cardstyle": ["current", "dark1", "glass1", "dark3", "glass3"], "prayermethod": ["auto", "gulf", "jafari"],
        "sunrise": ["on", "off"], "prayerbar": ["on", "off"], "wxmore": ["on", "off"], "months": ["default", "levant"],
        "greeting": ["on", "off"], "listenpill": ["on", "off"], "updatebell": ["on", "off"], "adhanbar": ["on", "off"], "clock24": ["on", "off"], "mkbar": ["on", "off"], "prayeradj": ["on", "off"]}
if "SaverReceiver" in cmd and cmd.endswith(".GET"):
    s = st["saver"]
    adj = " ".join(f"{k}={'+' if v > 0 else ''}{v}" for k, v in s["adj"].items())
    out("\n".join([
        "version: 9.9", f"city: {s['city'][0]} ({s['city'][1]}, {s['city'][2]})", f"timezone: {s['tz']}",
        "teams: " + ", ".join(s["teams"]), f"panel: {s['panel']}", f"home_panel: {s['home_panel']}",
        f"greeting: {s['greeting']}", f"listenpill: {s['listenpill']}", f"updatebell: {s['updatebell']}", f"adhanbar: {s['adhanbar']}", "yearstrip: off", f"prayermethod: {s['prayermethod']}",
        f"prayeradj: {s['prayeradj']} {adj}", f"hijrioffset: {s['hijri']}", "minrating: 0", "photos: 30", f"digits: {s['digits']}",
        f"clock24: {s['clock24']}", "national: auto", f"cardstyle: {s['cardstyle']}",
        "markets: " + ("" if s["mkbar"] == "on" else "off ") + ",".join(s["markets"]), f"clocksize: {s['clocksize']}",
        f"mkstyle: {s['mkstyle']}", "stocks: 2222", "slot: matches", "countdown: none", "idle: 10", "genres: ", "langs: ", "events: ",
        "leagues: ", "autoupdate: on", "pin: 4321", "wifi: HomeNet", "tasks: buy milk", "updatechannel: " + s.get("updatechannel", "stable")]
        + ([f"country: {s['country']}"] if "country" in s else [])
        + ([] if st.get("old_app") else [f"panel2: {s['panel2']}", f"sunrise: {s['sunrise']}", f"prayerbar: {s['prayerbar']}",
                                          f"wxmore: {s['wxmore']}", f"months: {s['months']}"])))
if "SaverReceiver" in cmd and re.search(r"screensaver\.SET( |$)", cmd):
    s, extras = st["saver"], dict(re.findall(r"--es (\w+) '([^']*)'", cmd))
    assert cmd.count("--es ") == len(extras), "an unquoted value was sent: " + cmd
    for k, v in extras.items():
        if k in ENUM and v not in ENUM[k]:
            out(f"error: {k} is one of " + " ".join(ENUM[k]))
        if k == "lat" and not re.fullmatch(r"-?\d{1,2}(\.\d+)?", v) or k == "lon" and not re.fullmatch(r"-?\d{1,3}(\.\d+)?", v):
            out("error: lat/lon must be numbers like 25.86 / 43.49")
    if "city" in extras and not all(k in extras for k in ("lat", "lon", "tz")):
        out("error: give lat lon tz")
    for k, v in extras.items():                       # all checked: now saved
        if k == "city": s["city"] = [v, extras["lat"], extras["lon"]]; s["tz"] = extras["tz"]
        elif k == "teams": s["teams"] = [f"{p.split(':')[0]}|{p.split(':', 1)[1] or '#' + p.split(':')[0]}" for p in v.split(",") if p]
        elif k == "home_panel": s["home_panel"] = v
        elif k == "panel": s["panel"] = v
        elif k == "panel2" and (v == s["panel"] or v == extras.get("panel")) and v != "none": out("error: panel2 is " + v + ", the first card already")
        elif k == "hijrioffset": s["hijri"] = ("+" if int(v) > 0 else "") + str(int(v))
        elif k.startswith("adj_"): s["adj"][k[4:]] = int(v)
        elif k == "markets": s["markets"] = v.split(",")
        elif k in ("lat", "lon", "tz"): pass
        else: s[k] = v
    lost = st.pop("lose_reply", False)             # the TV applied it, but the answer never arrives
    json.dump(st, open(st_path, "w"))
    if lost: sys.exit(1)
    out("saved\n" + "city: " + s["city"][0])
if "MusicReceiver" in cmd and "ADHAN_RESCHEDULE" in cmd:
    out()
if "MusicReceiver" in cmd:
    if st.get("no_home"):
        print("Error: could not find receiver"); sys.exit(1)
    a = st["adhan"]
    if cmd.endswith(".GET_PLAYER"):
        out("|".join(["playing=false", "adhan=" + a["on"], "voice=" + str(a["voice"]), "fajr_voice=" + str(a["fajr_voice"]),
                      "adhan_short=" + a["short"], "adhan_stopkey=" + a["stopkey"], "adhan_volume=" + str(a["volume"]),
                      "own_bytes=" + str(st.get("own_bytes", 0)), "error="]))
    m = re.search(r"\.(\w+)(?: --e[iz] (\w+) (\w+))?$", cmd)
    act, val = m.group(1), m.group(3)
    ans = None
    if act in ("ADHAN_ON", "ADHAN_OFF"): a["on"] = "on" if act == "ADHAN_ON" else "off"
    elif act == "ADHAN_SHORT": a["short"] = "on" if val == "true" else "off"; ans = "saved"
    elif act == "ADHAN_STOPKEY": a["stopkey"] = "on" if val == "true" else "off"; ans = "saved"
    elif act == "ADHAN_VOLUME":
        if 10 <= int(val) <= 100 and "volume" not in st.get("silent", []): a["volume"] = int(val)
    elif act in ("VOICE", "FAJR_VOICE"):
        v = int(val)
        if v == 0 and not st.get("own_bytes"): out("error: no recording of your own yet")
        if not 0 <= v <= 21: out("error: the voice is 1-21")
        a["voice" if act == "VOICE" else "fajr_voice"] = "own" if v == 0 else v; ans = "saved"
    json.dump(st, open(st_path, "w"))
    out(ans)
out("error: unknown " + cmd)
'''
fake = tmp / "fakepy"
fake.write_text(FAKE)
fake.chmod(0o755)

BASE = {
    "saver": {"city": ["الرياض", "24.6877", "46.7219"], "tz": "Asia/Riyadh", "teams": ["136013|الهلال", "136137|المنتخب السعودي"],
              "panel": "verse", "home_panel": "none", "greeting": "on", "listenpill": "on", "updatebell": "on", "adhanbar": "on", "prayermethod": "auto", "prayeradj": "off",
              "adj": {"fajr": 0, "dhuhr": 0, "asr": 0, "maghrib": 0, "isha": 0}, "hijri": "0", "digits": "hindi", "clock24": "off",
              "cardstyle": "current", "markets": ["tasi", "spx", "btc"], "mkbar": "on", "clocksize": "large", "mkstyle": "arrows",
              "panel2": "none", "sunrise": "off", "prayerbar": "off", "wxmore": "off", "months": "default", "updatechannel": "stable"},
    "adhan": {"on": "on", "voice": 1, "fajr_voice": 10, "short": "off", "stopkey": "on", "volume": 70},
}
CONFIGURED = copy.deepcopy(BASE)
CONFIGURED["saver"].update({"city": ["الدوحة", "25.2854", "51.531"], "tz": "Asia/Qatar", "teams": ["136022|النصر", "133738|ريال مدريد"],
                            "panel": "prayers", "home_panel": "verse,hourly", "greeting": "off", "listenpill": "off", "updatebell": "off", "adhanbar": "off",
                            "prayermethod": "jafari", "prayeradj": "on", "hijri": "-1", "digits": "latin", "clock24": "on",
                            "cardstyle": "glass1", "markets": ["gold", "btc"], "mkbar": "off", "clocksize": "medium", "mkstyle": "prices",
                            "panel2": "hourly", "sunrise": "on", "prayerbar": "on", "wxmore": "on", "months": "levant"})
CONFIGURED["saver"]["adj"] = {"fajr": 2, "dhuhr": 0, "asr": -3, "maghrib": 1, "isha": 0}
CONFIGURED["adhan"] = {"on": "off", "voice": 3, "fajr_voice": 13, "short": "on", "stopkey": "off", "volume": 40}


class Tv:
    def __init__(self, name, state):
        self.state, self.log = tmp / f"tv-{name}.json", tmp / f"tv-{name}.log"
        self.state.write_text(json.dumps(state))
        self.log.write_text("")
        self.before = tmp / f"tv-{name}-before.json"

    def data(self):
        return json.loads(self.state.read_text())

    def edit(self, **kw):
        d = self.data()
        d.update(kw)
        self.state.write_text(json.dumps(d))

    def sent(self):
        return [l for l in self.log.read_text().splitlines() if l]

    def writes(self):   # commands that can change something
        return [l for l in self.sent() if not l.endswith(".GET") and not l.endswith(".GET_PLAYER")]

    def run(self, *args):
        env = {**os.environ, "PY_TV": str(fake), "FAKE_TV": str(self.state), "FAKE_LOG": str(self.log), "SETTINGS_BEFORE": str(self.before)}
        r = subprocess.run([sys.executable, "-I", str(MODULE), *args], capture_output=True, text=True, env=env, encoding="utf-8", cwd=tmp)
        return r.returncode, r.stdout, r.stderr


def settings_of(tv):   # what the TV shows, through the same reads
    return tv.run("export")[1]


def write(name, obj):
    p = tmp / name
    p.write_text(obj if isinstance(obj, str) else json.dumps(obj, ensure_ascii=False), encoding="utf-8")
    return str(p)


# ---- a round trip: export one TV, import onto a second
a = Tv("a", CONFIGURED)
rc, out, err = a.run("export")
check("export: exits 0 and prints JSON only on stdout", rc == 0 and json.loads(out)["schema"] == 1, (rc, out[:100], err))
exp = json.loads(out)
b = Tv("b", BASE)
f = write("moved.json", out)
rc, out, err = b.run("import", f)
check("import without --yes: a preview, nothing sent", rc == 0 and "Preview:" in out and not b.writes() and "--yes" in out, (rc, out, err, b.sent()))
check("preview: old → new, one line per changed setting", "  digits: hindi → latin" in out and "  prayer.method: auto → jafari" in out
      and "  prayer.adjust.fajr: 0 → +2" in out and "  hijri_offset: 0 → -1" in out and "  adhan.volume: 70 → 40" in out
      and "  markets.list: tasi,spx,btc → gold,btc" in out and "  city: الرياض (24.6877, 46.7219) Asia/Riyadh → الدوحة (25.2854, 51.531) Asia/Qatar" in out, out)
check("preview: the «تلاوة وأذان» button's setting is one more line", "  listen_pill: on \u2192 off" in out, out)
check("preview: the «جرس التحديثات» setting is one more line", "  update_bell: on \u2192 off" in out, out)
check("preview: unchanged settings are absent", "prayer.adjust.dhuhr" not in out and "prayer.adjust.isha" not in out, out)
rc, out, err = b.run("import", f, "--yes")
check("import --yes: applied (exit 0)", rc == 0 and "Done" in out, (rc, out, err))
check("round trip: the second TV exports the same settings", settings_of(b) == json.dumps(exp, ensure_ascii=False, indent=2) + "\n", settings_of(b))
check("round trip: the apps' own values are what the fake keeps", b.data()["saver"] == CONFIGURED["saver"] and b.data()["adhan"] == CONFIGURED["adhan"], b.data())
check("the saver's values go in ONE SET broadcast", sum(".SET " in l for l in b.writes()) == 1, b.writes())
n = len(b.writes())
rc, out, err = b.run("import", f, "--yes")
check("importing the same file again: nothing to change, nothing sent", rc == 0 and "Nothing to change" in out and len(b.writes()) == n, out)
check("the before-import file is the old settings, same schema", json.loads(b.before.read_text())["schema"] == 1
      and json.loads(b.before.read_text())["digits"] == "hindi" and json.loads(b.before.read_text())["city"]["name"] == "الرياض", b.before.read_text()[:200])
rc, out, err = b.run("import", str(b.before), "--yes")
check("undo: importing the before-file restores the first settings", rc == 0 and b.data()["saver"] == BASE["saver"] and b.data()["adhan"] == BASE["adhan"], (out, b.data()))

# ---- what is in an export: an allow-list
mod_spec = importlib.util.spec_from_file_location("settings_move", MODULE)
sm = importlib.util.module_from_spec(mod_spec)
mod_spec.loader.exec_module(sm)
check("export has only allow-listed top-level keys", set(exp) <= set(sm.ALLOWED_TOP), set(exp) - set(sm.ALLOWED_TOP))
text = json.dumps(exp, ensure_ascii=False).lower()
check("the PIN, ADB keys, journals, Wi-Fi, tasks, auto-update never appear in an export (the fake TV shows them in GET)",
      not any(w in text for w in ("4321", "pin", "homenet", "wifi", "buy milk", "adbkey", "journal", "autoupdate", "task")), text)
src = MODULE.read_text(encoding="utf-8")
check("the allow-list comment names the parental PIN TODO next to the key list", "TODO(parental PIN" in src and src.index("TODO(parental PIN") < src.index("ALLOWED_TOP ="), "")
check("no deny-list in the code", not re.search(r"(deny|block|forbid|exclude)\w*\s*=", src, re.I), "")
rc, out, err = Tv("pin", BASE).run("import", write("pin.json", {"schema": 1, "pin": "1234", "adb_key": "x", "digits": "latin", "city": {"name": "x", "lat": 1, "lon": 1, "tz": "UTC", "pin": "1"}}))
check("a pin or key in a file is reported and ignored, never sent", rc == 0 and "ignored" in out and "pin" in out and "adb_key" in out and "city.pin" in out, (rc, out, err))
t = Tv("pin2", BASE)
t.run("import", write("pin2.json", {"schema": 1, "pin": "1234", "digits": "latin"}), "--yes")
check("...and nothing with the pin went to the TV", t.writes() and not any("1234" in l or "pin" in l for l in t.sent()), t.writes())
check("the sources of the exported settings do not include the own adhan or photos paths", "own_bytes" not in text and "photos" not in text, text)

# ---- own adhan voice: left out of an export, kept in the before-file
o = Tv("own", BASE)
o.edit(own_bytes=1000)
d = o.data(); d["adhan"]["voice"] = "own"; o.state.write_text(json.dumps(d))
rc, out, err = o.run("export")
check("an own-recording voice is not exported (note on stderr)", rc == 0 and "voice" not in json.loads(out)["adhan"] and "own recording" in err, (out, err))
o.run("import", write("v.json", {"schema": 1, "adhan": {"voice": 5}}), "--yes")
check("...and the before-file keeps it, so an undo brings it back", json.loads(o.before.read_text())["adhan"]["voice"] == "own", o.before.read_text())
o.run("import", str(o.before), "--yes")
check("...undo: the voice is the own recording again", o.data()["adhan"]["voice"] == "own", o.data())

# ---- every validation refusal: nothing is sent, not even a read
GOOD = copy.deepcopy(exp)
CITY = GOOD["city"]


def refused(name, mutate, want, **kw):
    d = copy.deepcopy(GOOD)
    mutate(d)
    tv = Tv("v", BASE)
    rc, out, err = tv.run("import", write("bad.json", d), "--yes")
    check(f"refused: {name}", rc != 0 and want in err and not tv.sent() and not tv.before.exists(), (rc, out, err, tv.sent()))


refused("city name is a number", lambda d: d["city"].update(name=5), "city.name must be text")
refused("city name too long", lambda d: d["city"].update(name="x" * 61), "1 to 60 characters")
refused("city name empty", lambda d: d["city"].update(name=" "), "1 to 60 characters")
refused("city name with an apostrophe", lambda d: d["city"].update(name="Ra's"), "no apostrophes")
refused("city name with a newline", lambda d: d["city"].update(name="a\nb"), "control character")
refused("city missing its zone", lambda d: d["city"].pop("tz"), "city.tz is missing")
refused("lat a string", lambda d: d["city"].update(lat="25"), "city.lat must be a number")
refused("lat a boolean", lambda d: d["city"].update(lat=True), "city.lat must be a number")
refused("lat out of range", lambda d: d["city"].update(lat=91), "city.lat must be from -90 to 90")
refused("lon out of range", lambda d: d["city"].update(lon=-180.5), "city.lon must be from -180 to 180")
refused("tz not a zone", lambda d: d["city"].update(tz="Mars/Base"), "city.tz must be a time zone")
refused("tz a path", lambda d: d["city"].update(tz="../etc/passwd"), "city.tz must be a time zone")
refused("tz with a quote", lambda d: d["city"].update(tz="Asia/Riyadh'; reboot; '"), "city.tz must be a time zone")
refused("tz a number", lambda d: d["city"].update(tz=3), "city.tz must be text")
refused("digits unknown", lambda d: d.update(digits="roman"), "digits must be one of hindi latin")
refused("digits a number", lambda d: d.update(digits=1), "digits must be one of")
refused("card style unknown", lambda d: d.update(card_style="neon"), "card_style must be one of")
refused("clock size unknown", lambda d: d.update(clock_size="huge"), "clock_size must be one of")
refused("clock24 not a boolean", lambda d: d.update(clock24="on"), "clock24 must be true or false")
refused("greeting not a boolean", lambda d: d.update(greeting=1), "greeting must be true or false")
refused("listen_pill not a boolean", lambda d: d.update(listen_pill="off"), "listen_pill must be true or false")
refused("prayer method unknown", lambda d: d["prayer"].update(method="mwl"), "prayer.method must be one of auto gulf jafari")
refused("prayer adjust 21", lambda d: d["prayer"]["adjust"].update(fajr=21), "prayer.adjust.fajr must be a whole number from -20 to 20")
refused("prayer adjust -21", lambda d: d["prayer"]["adjust"].update(isha=-21), "prayer.adjust.isha")
refused("prayer adjust a fraction", lambda d: d["prayer"]["adjust"].update(asr=1.5), "prayer.adjust.asr")
refused("prayer adjust a boolean", lambda d: d["prayer"]["adjust"].update(asr=True), "prayer.adjust.asr")
refused("hijri offset 3", lambda d: d.update(hijri_offset=3), "hijri_offset must be a whole number from -2 to 2")
refused("hijri offset -3", lambda d: d.update(hijri_offset=-3), "hijri_offset")
refused("hijri offset a string", lambda d: d.update(hijri_offset="1"), "hijri_offset")
refused("panel unknown", lambda d: d["cards"].update(panel="news"), "cards.panel must be one of")
refused("next prayer as the screensaver panel (home only)", lambda d: d["cards"].update(panel="nextprayer"), "cards.panel must be one of")
refused("four home cards", lambda d: d["cards"].update(home=["verse", "hourly", "year", "today"]), "cards.home must be a list of up to 3")
refused("home card twice", lambda d: d["cards"].update(home=["verse", "verse"]), "cards.home")
refused("home card unknown", lambda d: d["cards"].update(home=["news"]), "cards.home")
refused("markets unknown", lambda d: d["markets"].update(list=["tasi", "dogecoin"]), "markets.list must be 1 to 4")
refused("five markets", lambda d: d["markets"].update(list=["tasi", "aramco", "spx", "btc", "gold"]), "markets.list")
refused("no markets", lambda d: d["markets"].update(list=[]), "markets.list")
refused("mkstyle unknown", lambda d: d["markets"].update(style="bars"), "markets.style must be one of arrows prices")
refused("team id not digits", lambda d: d.update(teams=[{"id": "12a", "name": "x"}]), "teams[0].id")
refused("team id a number", lambda d: d.update(teams=[{"id": 12, "name": "x"}]), "teams[0].id")
refused("team name with an apostrophe", lambda d: d.update(teams=[{"id": "1", "name": "O'Hara"}]), "no apostrophes")
refused("team name with a comma", lambda d: d.update(teams=[{"id": "1", "name": "a,b"}]), "must not have any of")
refused("team name too long", lambda d: d.update(teams=[{"id": "1", "name": "x" * 61}]), "1 to 60 characters")
refused("team twice", lambda d: d.update(teams=[{"id": "1", "name": "a"}, {"id": "1", "name": "b"}]), "teams has a club twice")
refused("21 teams", lambda d: d.update(teams=[{"id": str(i), "name": "t"} for i in range(21)]), "up to 20 teams")
refused("teams not a list", lambda d: d.update(teams="136013"), "teams must be a list")
refused("adhan volume 9", lambda d: d["adhan"].update(volume=9), "adhan.volume must be a whole number from 10 to 100")
refused("adhan volume 101", lambda d: d["adhan"].update(volume=101), "adhan.volume")
refused("adhan volume a string", lambda d: d["adhan"].update(volume="50"), "adhan.volume")
refused("adhan voice 0", lambda d: d["adhan"].update(voice=0), "adhan.voice")
refused("adhan voice 22", lambda d: d["adhan"].update(voice=22), "adhan.voice")
refused("adhan voice a Fajr recording", lambda d: d["adhan"].update(voice=10), "adhan.voice must be 1 to 21")
refused("adhan fajr voice 22", lambda d: d["adhan"].update(fajr_voice=22), "adhan.fajr_voice")
refused("adhan short not a boolean", lambda d: d["adhan"].update(short="on"), "adhan.short must be true or false")
refused("a section that is not an object", lambda d: d.update(prayer=[1]), "prayer must be an object")
refused("all errors are listed together", lambda d: (d.update(digits="x"), d["adhan"].update(volume=1)), "digits must be")
d = copy.deepcopy(GOOD); d["digits"] = "x"; d["adhan"]["volume"] = 1
rc, out, err = Tv("v2", BASE).run("import", write("two.json", d))
check("both errors are in one message", "digits must" in err and "adhan.volume" in err, err)
for name, content in (("NaN", '{"schema":1,"city":{"name":"x","lat":NaN,"lon":1,"tz":"UTC"}}'),
                      ("Infinity", '{"schema":1,"city":{"name":"x","lat":1,"lon":Infinity,"tz":"UTC"}}'),
                      ("-Infinity", '{"schema":1,"city":{"name":"x","lat":-Infinity,"lon":1,"tz":"UTC"}}'),
                      ("1e999 (inf after parsing)", '{"schema":1,"city":{"name":"x","lat":1e999,"lon":1,"tz":"UTC"}}')):
    tv = Tv("c", BASE)
    rc, out, err = tv.run("import", write("c.json", content), "--yes")
    check(f"refused: coordinate {name}", rc != 0 and ("not allowed" in err or "finite" in err) and not tv.sent(), (rc, err))

# ---- the file itself
text = json.dumps(GOOD, ensure_ascii=False, indent=2)
for name, content, want in (("truncated JSON", text[:len(text) // 2], "not complete or not valid JSON"),
                            ("empty file", "", "not complete or not valid JSON"),
                            ("not JSON", "digits: latin", "not complete or not valid JSON"),
                            ("a list", "[1, 2]", "must be a JSON object"),
                            ("no schema", '{"digits": "latin"}', 'needs "schema": 1'),
                            ("schema 0", '{"schema": 0}', 'needs "schema": 1'),
                            ("schema a string", '{"schema": "1"}', 'needs "schema": 1'),
                            ("schema true", '{"schema": true}', 'needs "schema": 1'),
                            ("a key twice", '{"schema": 1, "digits": "latin", "digits": "hindi"}', "given twice"),
                            ("a future schema", '{"schema": 2, "digits": "latin"}', "newer version")):
    tv = Tv("f", BASE)
    rc, out, err = tv.run("import", write("f.json", content), "--yes")
    check(f"refused: {name}", rc != 0 and want in err and not tv.sent() and not tv.before.exists(), (rc, err, tv.sent()))
tv = Tv("f", BASE)
rc, out, err = tv.run("import", str(tmp / "missing.json"))
check("refused: no such file", rc != 0 and "not a file" in err and not tv.sent(), err)
tv = Tv("f", BASE)
(tmp / "utf16.json").write_bytes(json.dumps(GOOD).encode("utf-16"))
rc, out, err = tv.run("import", str(tmp / "utf16.json"))
check("refused: a UTF-16 file (PowerShell's >) says so", rc != 0 and "not UTF-8" in err and not tv.sent(), err)
rc, out, err = tv.run("import")
check("import with no file: usage", rc != 0 and "usage" in err and not tv.sent(), err)
(tmp / "bom.json").write_bytes(b"\xef\xbb\xbf" + json.dumps({"schema": 1, "digits": "latin"}).encode())
rc, out, err = tv.run("import", str(tmp / "bom.json"))
check("a UTF-8 BOM is fine", rc == 0 and "digits: hindi" in out, (out, err))

# ---- schema migrations (a fake: there is only schema 1 today)
check("MIGRATIONS has no step today (schema 1 is current)", sm.CURRENT_SCHEMA == 1 and sm.MIGRATIONS == {}, "")
old_cur, old_mig = sm.CURRENT_SCHEMA, sm.MIGRATIONS
sm.CURRENT_SCHEMA = 2
sm.MIGRATIONS = {1: lambda d: {**{k: v for k, v in d.items() if k != "numerals"}, "digits": d["numerals"]}}
try:
    got = sm.migrate({"schema": 1, "numerals": "latin"})
    check("an older schema goes through its migration step", got == {"schema": 2, "digits": "latin"}, got)
    clean, notes = sm.load(write("old.json", {"schema": 1, "numerals": "latin"}))
    check("...and then is validated as the current one", clean["digits"] == "latin" and clean["schema"] == 2, clean)
    try:
        sm.load(write("old2.json", {"schema": 1, "numerals": "roman"}))
        check("...a bad value in a migrated file is refused", False, "no refusal")
    except sm.Refused as e:
        check("...a bad value in a migrated file is refused", "digits must" in str(e), str(e))
    sm.MIGRATIONS = {}
    try:
        sm.migrate({"schema": 1})
        check("an older schema with no migration is refused", False, "no refusal")
    except sm.Refused as e:
        check("an older schema with no migration is refused", "no way to bring it" in str(e), str(e))
    try:
        sm.migrate({"schema": 3})
        check("a future schema is refused", False, "no refusal")
    except sm.Refused as e:
        check("a future schema is refused (module level)", "newer version" in str(e), str(e))
finally:
    sm.CURRENT_SCHEMA, sm.MIGRATIONS = old_cur, old_mig

# ---- all or nothing: the TV refuses one value
tv = Tv("r", BASE)
rc, out, err = tv.run("import", f, "--yes")
tv = Tv("r2", BASE)
tv.edit(refuse=["percent 40"])
rc, out, err = tv.run("import", f, "--yes")
check("a refused adhan value: exit 1 and a !! line saying the TV was put back", rc == 1 and re.search(r"^!! .*refused by the fake TV.*put back as it was", out, re.M), (rc, out, err))
check("...every setting is as before (saver and adhan)", tv.data()["saver"] == BASE["saver"] and tv.data()["adhan"] == BASE["adhan"], tv.data())
check("...the earlier adhan values were sent and put back", any("ADHAN_OFF" in l for l in tv.writes()) and any("ADHAN_ON" in l for l in tv.writes()), tv.writes())
check("...the before-file is there for a manual undo", tv.before.exists(), "")
tv = Tv("r3", BASE)
tv.edit(refuse=["--es teams '136022"])
rc, out, err = tv.run("import", f, "--yes")
check("the saver refusing a value: nothing was changed, !! line", rc == 1 and "!! " in out and tv.data()["saver"] == BASE["saver"] and tv.data()["adhan"] == BASE["adhan"] and not any("MusicReceiver" in l for l in tv.writes()), (rc, out, tv.writes()))
tv = Tv("r4", BASE)
tv.edit(silent=["volume"])
rc, out, err = tv.run("import", f, "--yes")
check("a value the TV silently ignores (no error: answer) is caught by reading back, and put back",
      rc == 1 and "!! " in out and "adhan.volume" in out and tv.data()["saver"] == BASE["saver"] and tv.data()["adhan"] == BASE["adhan"], (rc, out, tv.data()))
tv = Tv("r5", BASE)
tv.edit(refuse=["percent 40", "ADHAN_SHORT --ez on false"])
rc, out, err = tv.run("import", f, "--yes")
check("if even the put-back fails: a !! line with the saved file's path and the command to run",
      rc == 1 and re.search(r"^!! .*could NOT be put back.*settings-before-import|^!! .*could NOT be put back.*-before\.json", out, re.M) and str(tv.before) in out.replace("\n", " "), (rc, out))
tv = Tv("r6", BASE)
tv.edit(no_home=True)
rc, out, err = tv.run("import", f)
check("adhan settings in the file but the home app silent: refused before anything is sent", rc != 0 and "does not report" in err and not tv.writes(), (rc, err))
tv = Tv("r7", BASE)
tv.edit(no_home=True)
rc, out, err = tv.run("export")
check("export with the home app silent: the rest is exported, a note says why", rc == 0 and "adhan" in json.loads(out) and "volume" not in json.loads(out)["adhan"] and "note" in err, (out[-200:], err))
rc, out, err = tv.run("import", write("noadhan.json", {k: v for k, v in GOOD.items() if k != "adhan"}), "--yes")
check("...and a file without adhan settings imports fine onto that TV", rc == 0 and "Done" in out, (rc, out, err))

# ---- quoting: text is data
tv = Tv("q", BASE)
weird = {"schema": 1, "city": {"name": "$(reboot) `id` ; \"x\" #", "lat": 25.5, "lon": 51.5, "tz": "Asia/Qatar"},
         "teams": [{"id": "1", "name": "a $HOME b"}]}
rc, out, err = tv.run("import", write("w.json", weird), "--yes")
check("shell characters in a name are only data: inside single quotes, one value", rc == 0
      and "--es city '$(reboot) `id` ; \"x\" #'" in " ".join(tv.writes()) and "--es teams '1:a $HOME b'" in " ".join(tv.writes()), (out, tv.writes()))
check("the quoted values reach the TV intact", tv.data()["saver"]["city"][0] == '$(reboot) `id` ; "x" #', tv.data()["saver"]["city"])
t2 = Tv("q2", BASE)
t2.edit(saver={**BASE["saver"], "city": ["Ra's Tanura", "26.64", "50.16"]})
rc, out, err = t2.run("import", write("q.json", {"schema": 1, "digits": "latin"}), "--yes")
check("a TV whose current city has an apostrophe: other settings still import (the city is not touched)", rc == 0 and t2.data()["saver"]["digits"] == "latin", (rc, out, err))
t3 = Tv("q3", BASE)
t3.edit(saver={**BASE["saver"], "city": ["Ra's Tanura", "26.64", "50.16"]})
rc, out, err = t3.run("import", f)
check("...but a file that changes that city is stopped before sending if the old name can't be put back safely", rc != 0 and "no apostrophes" in err and not t3.writes(), (rc, out, err))

# ---- only the settings in the file move
tv = Tv("p", BASE)
rc, out, err = tv.run("import", write("part.json", {"schema": 1, "prayer": {"adjust": {"fajr": 2}}, "adhan": {"volume": 55}}), "--yes")
check("a partial file changes only what it names", rc == 0 and tv.data()["saver"]["adj"] == {**BASE["saver"]["adj"], "fajr": 2}
      and tv.data()["adhan"] == {**BASE["adhan"], "volume": 55} and tv.data()["saver"]["digits"] == "hindi", (out, tv.data()))
check("prayer adjustments trigger the adhan alarm reschedule, as saver.sh does", any("ADHAN_RESCHEDULE" in l for l in tv.writes()), tv.writes())
tv = Tv("p2", BASE)
tv.run("import", write("d.json", {"schema": 1, "digits": "latin"}), "--yes")
check("a digits-only import does not reschedule the adhan", not any("ADHAN_RESCHEDULE" in l for l in tv.writes()), tv.writes())
tv = Tv("p3", BASE)
rc, out, err = tv.run("import", write("e.json", {"schema": 1, "teams": [], "cards": {"home": []}}), "--yes")
check("no team and no home card are sent as the app wants them (teams '', home_panel none)", rc == 0 and "--es teams ''" in " ".join(tv.writes())
      and tv.data()["saver"]["teams"] == [] , (out, tv.writes()))
check("the before-file is only written with --yes", not Tv("p4", BASE).before.exists(), "")

# ---- saver.sh dispatch and docs
sh = tmp / "fakepy-sh"
sh.write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$*" >> "$SENT"\n')
sh.chmod(0o755)
for words in (["export"], ["import", "a.json", "--yes"]):
    sent = tmp / "sent.txt"
    sent.write_text("")
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *words], capture_output=True, text=True, env={**os.environ, "PY": str(sh), "SENT": str(sent)})
    check(f"saver.sh {words[0]}: runs optimize/settings_move.py with the words", r.returncode == 0 and sent.read_text().strip() == "-I optimize/settings_move.py " + " ".join(words), (r.stderr, sent.read_text()))
check("saver.sh with no words lists export and import", "saver.sh export" in subprocess.run(["bash", str(ROOT / "optimize/saver.sh")], capture_output=True, text=True).stdout, "")
check("the default before-file is optimize/run/settings-before-import.json (git-ignored)", sm.BEFORE == "optimize/run/settings-before-import.json"
      and "optimize/run/" in (ROOT / ".gitignore").read_text(), sm.BEFORE)

# ---- Windows and the Python that runs tv.py
check("PY defaults to this very Python (the venv's python can be a shell script on Windows), PY_TV is the test-only override",
      'os.environ.get("PY_TV") or sys.executable' in MODULE.read_text(encoding="utf-8") and "optimize/venv/bin/python" not in MODULE.read_text(encoding="utf-8").split("def tv(")[0].split("PY =")[1].split("\n")[0].split("#")[0])
env_off = {k: v for k, v in os.environ.items() if k not in ("PY", "PY_TV")}
code = "import importlib.util,sys;s=importlib.util.spec_from_file_location('m',sys.argv[1]);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);print(m.PY==sys.executable)"
check("with no PY_TV set the module runs tv.py with sys.executable, whatever PY says", subprocess.run([sys.executable, "-c", code, str(MODULE)], capture_output=True, text=True, env={**env_off, "PY": "/nonexistent/venv/bin/python"}).stdout.strip() == "True", "")

# ---- rollback: the reply to the saver's SET is lost after the TV applied it
lost = Tv("lost", BASE)
lost.edit(lose_reply=True)
rc, out, err = lost.run("import", write("lost.json", exp), "--yes")
check("a lost reply to the saver SET: the import fails with !! and the TV is put back (the SET had been applied)", rc != 0 and "!! The import failed" in out
      and "put back as it was" in out and lost.data()["saver"] == BASE["saver"], (rc, out, lost.data()["saver"]))

# ---- rollback: one put-back step failing does not stop the others
mid = Tv("mid", BASE)
mid.edit(refuse=["ADHAN_VOLUME"], fail_once=["ADHAN_SHORT --ez on false"])      # volume is refused (the import fails); the put-back of «short» gets no answer
rc, out, err = mid.run("import", write("mid.json", exp), "--yes")
d = mid.data()
check("a put-back step that gets no answer: !! says the TV could NOT be put back completely, with the file to import", rc != 0 and "could NOT be put back" in out and str(mid.before) in out, (rc, out))
check("and every other put-back still happened (the saver's settings and the other adhan ones are as before)",
      d["saver"] == BASE["saver"] and d["adhan"]["voice"] == BASE["adhan"]["voice"] and d["adhan"]["fajr_voice"] == BASE["adhan"]["fajr_voice"]
      and d["adhan"]["stopkey"] == BASE["adhan"]["stopkey"] and d["adhan"]["on"] == BASE["adhan"]["on"], d)

# ---- undo with the before-file keeps that file as it is
u = Tv("u", BASE)
uf = write("u-moved.json", exp)
u.run("import", uf, "--yes")
original = u.before.read_bytes()
rc, out, err = u.run("import", str(u.before), "--yes")
check("undo: the before-file is not overwritten by its own import (it stays the way back)", rc == 0 and u.before.read_bytes() == original and "kept as it is" in out, (rc, out))
check("and the undo itself worked", u.data()["saver"] == BASE["saver"] and u.data()["adhan"] == BASE["adhan"], u.data())
rc, out, err = u.run("import", str(u.before), "--yes")
check("importing the before-file again is still harmless (nothing to change)", rc == 0 and u.before.read_bytes() == original, (rc, out))

# ---- relative file names: the caller's directory, else the repository root
caller = tmp / "caller"
caller.mkdir(exist_ok=True)
(caller / "here.json").write_text(json.dumps(exp, ensure_ascii=False), encoding="utf-8")
r1 = Tv("r1", BASE)
env = {**os.environ, "PY_TV": str(fake), "FAKE_TV": str(r1.state), "FAKE_LOG": str(r1.log), "SETTINGS_BEFORE": str(r1.before), "SAVER_CALLER_DIR": str(caller)}
r = subprocess.run([sys.executable, "-I", str(MODULE), "import", "here.json"], capture_output=True, text=True, env=env, encoding="utf-8", cwd=ROOT)
check("a relative FILE is looked for in the directory saver.sh was run from", r.returncode == 0 and "Preview:" in r.stdout, (r.stdout, r.stderr))
r = subprocess.run([sys.executable, "-I", str(MODULE), "import", "nothing-here.json"], capture_output=True, text=True, env=env, encoding="utf-8", cwd=ROOT)
check("a file that is nowhere is refused, nothing sent", r.returncode != 0 and "is not a file" in r.stderr and not r1.writes(), (r.stdout, r.stderr))

# ---- names and ids
refused("team id in Arabic-Indic digits (the TV refuses them)", lambda d: d.update(teams=[{"id": "\u0661\u0662\u0663", "name": "x"}]), "teams[0].id")
joiner = copy.deepcopy(GOOD)
joiner["teams"] = [{"id": "1", "name": "\u06af\u200c\u0644"}]
joiner["city"]["name"] = "\u0645\u06cc\u200d\u0632"
jt = Tv("j", BASE)
rc, out, err = jt.run("import", write("joiner.json", joiner))
check("a zero-width (non-)joiner in a name is fine (Persian and Arabic names need it)", rc == 0 and "Preview:" in out, (rc, out, err))
for ch, label in (("\u202e", "right-to-left override"), ("\u2066", "isolate"), ("\u200f", "right-to-left mark"), ("\ufeff", "byte order mark"), ("\u0007", "bell")):
    refused(f"a {label} in a team name", lambda d, ch=ch: d.update(teams=[{"id": "1", "name": "a" + ch + "b"}]), "control character")
deep = write("deep.json", "[" * 20000 + "]" * 20000)
rd = Tv("deep", BASE)
rc, out, err = rd.run("import", deep)
check("a file nested too deeply is refused, not a traceback", rc != 0 and "nested too deeply" in err and "Traceback" not in err and not rd.sent(), (rc, err[-200:]))

# ---- today's settings: the second card, the sunrise, the prayer bar, the humidity and wind, the month names (and never the update channel)
check("export carries cards.panel2, sunrise, prayerbar, wxmore and months as the TV reports them",
      exp["cards"]["panel2"] == "hourly" and exp["sunrise"] is True and exp["prayerbar"] is True and exp["wxmore"] is True and exp["months"] == "levant", exp)
check("the hidden update-channel switch is never exported, never allow-listed, and never sent",
      "updatechannel" not in json.dumps(exp) and "updatechannel" not in sm.ALLOWED_TOP and "channel" not in json.dumps(sm.ops({"months": "levant"}, {})) , "")
old = Tv("old", BASE); old.edit(old_app=True)
rc, out, err = old.run("export")
oe = json.loads(out)
check("a TV whose app does not report them: absent from the export (drift-safe), not guessed", rc == 0 and "sunrise" not in oe and "months" not in oe and "panel2" not in oe.get("cards", {}), oe)
rc, out, err = old.run("import", write("new-on-old.json", {"schema": 1, "sunrise": True}), "--yes")
check("importing sunrise onto a TV that does not report it: refused, nothing sent", rc != 0 and "does not report sunrise" in err + out and not old.writes(), (rc, out, err))
refused("panel2 is the first card again", lambda d: d["cards"].update(panel="verse", panel2="verse"), "cards.panel2 must be another card")
refused("panel2 with no first card", lambda d: d["cards"].update(panel="none", panel2="verse"), "cards.panel2 must be another card")
refused("panel2 is a home-only card", lambda d: d["cards"].update(panel2="nextprayer"), "cards.panel2 must be one of")
refused("months unknown", lambda d: d.update(months="roman"), "months must be one of default levant")
refused("sunrise not a boolean", lambda d: d.update(sunrise="on"), "sunrise must be true or false")
b2 = Tv("b2", BASE)
b2.run("import", write("today.json", {"schema": 1, "sunrise": True, "months": "levant", "cards": {"panel": "prayers", "panel2": "hourly"}}), "--yes")
check("import sends them in the one SET with the TV's own names", sum(".SET " in l for l in b2.writes()) == 1
      and all(x in b2.writes()[0] for x in ("--es sunrise 'on'", "--es months 'levant'", "--es panel2 'hourly'", "--es panel 'prayers'")), b2.writes())

# ---- the choice lists are the apps' own (they cannot drift)
prefs = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")


def table(name):
    body = re.search(r"static final String\[\]\[\] " + name + r" = \{(.*?)\n?\s*\};", prefs, re.S).group(1)
    return tuple(re.findall(r'\{"([^"]+)",', body))


for name, mine in (("DIGITS", sm.DIGITS), ("CARD_STYLES", sm.CARD_STYLES), ("CLOCK_SIZES", sm.CLOCK_SIZES), ("MK_STYLES", sm.MK_STYLES),
                   ("MARKETS", sm.MARKETS), ("PANELS", sm.PANELS)):
    check(f"{name} is the app's own list", table(name) == mine, (table(name), mine))
check("MAX_MARKETS, HOME_CARDS, MAX_TEAMS are the app's", f"MAX_MARKETS = {sm.MAX_MARKETS};" in prefs and f"HOME_CARDS = {sm.HOME_CARDS};" in prefs
      and f"MAX_TEAMS = {sm.MAX_TEAMS};" in prefs, "")
adj = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/PrayerAdj.java").read_text(encoding="utf-8")
check("prayer names and range are the app's", f"MAX = {sm.PRAYER_ADJ_MAX};" in adj and ", ".join(f'"{p}"' for p in sm.PRAYERS) in adj, "")
hij = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/HijriOffset.java").read_text(encoding="utf-8")
check("the Hijri range is the app's", f"MAX = {sm.HIJRI_MAX};" in hij, "")
voices = (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/AdhanVoices.java").read_text(encoding="utf-8")
check("adhan voices: count and the Fajr ones are the launcher's", f"COUNT = {sm.ADHAN_VOICES};" in voices
      and "FAJR = {" + ", ".join(map(str, sm.ADHAN_FAJR_VOICES)) + "}" in voices, "")

# Country is optional for old files/apps, but mandatory to preserve a new explicit selection.
world_state = copy.deepcopy(BASE)
world_state['saver']['country'] = 'SA'
world_tv = Tv('world-country', world_state)
world_export = json.loads(settings_of(world_tv))
check('export retains the city country when the app reports it', world_export['city']['country'] == 'SA', world_export['city'])
world_export['city']['country'] = 'FR'
rc, out, err = world_tv.run('import', write('world-country.json', world_export), '--yes')
check('changing only the country is applied and verified', rc == 0 and world_tv.data()['saver']['country'] == 'FR' and any("--es country 'FR'" in command for command in world_tv.writes()), (rc, out, err))
rc, out, err = world_tv.run('import', str(world_tv.before), '--yes')
check('the before-file restores the original country', rc == 0 and world_tv.data()['saver']['country'] == 'SA', (rc, out, err))
old_world_tv = Tv('old-world-country', BASE)
rc, out, err = old_world_tv.run('import', write('world-on-old.json', world_export), '--yes')
check('explicit countries are refused by an older app that cannot report them', rc != 0 and 'city.country' in out + err and not old_world_tv.writes(), (rc, out, err))
before_world_writes = len(world_tv.writes())
world_export['city']['country'] = 'invalid'
rc, out, err = world_tv.run('import', write('world-bad-country.json', world_export), '--yes')
check('malformed country codes are rejected before any write', rc != 0 and 'city.country' in out + err and len(world_tv.writes()) == before_world_writes, (rc, out, err))

print("\nall settings move checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
