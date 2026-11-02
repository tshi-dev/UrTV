#!/usr/bin/env python3
"""Move a person's settings to a second TV («نقل الإعدادات لتلفزيون ثاني»): export them to a file, import them from one.

    optimize/saver.sh export > settings.json       the settings of the TV, read over the usual paths (GET and GET_PLAYER)
    optimize/saver.sh import settings.json         only a PREVIEW: what would change (old -> new), nothing is sent
    optimize/saver.sh import settings.json --yes   apply it; the TV's settings from before are saved first, so
    optimize/saver.sh import optimize/run/settings-before-import.json --yes   puts them back

The file is {"schema": 1, ...}, see SCHEMA below. Every key is optional on import (a missing one is left alone).
What is NOT here, on purpose: the ADB keys, journals and diagnostics, Wi-Fi, the tasks (the person's private notes),
the person's own photos and own adhan recording (separate files; a voice «own» is not exported), the parental PIN.

Import checks EVERYTHING in the file before it talks to the TV, then applies all or nothing: the saver's values go in one
SET (the app checks all of it first), the adhan's one by one; if the TV refuses one, or does not show what was sent when
read back, the earlier ones are put back and a `!!` line says so. Text from the file (a city, a team) is data: it goes into
the TV's shell only in single quotes, and an apostrophe is refused, as saver.sh does.
"""
import json
import math
import os
import re
import subprocess
import sys
import unicodedata
from pathlib import Path

for stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

ROOT = Path(__file__).resolve().parents[1]
PY = os.environ.get("PY_TV") or sys.executable   # runs optimize/tv.py: this very Python (on Windows the venv's "python" is a shell script that subprocess cannot start); PY_TV is for the tests
BEFORE = os.environ.get("SETTINGS_BEFORE") or "optimize/run/settings-before-import.json"
SAVER = "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver."
HOME = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home."

CURRENT_SCHEMA = 1
# schema N -> a function that makes the schema N+1 of it. Schema 1 is the current one, so nothing is needed yet; a file
# of an older schema is migrated step by step, a newer one is refused.
MIGRATIONS = {}

# The ONLY things that can be exported or imported, as an allow-list: the keys of the file, with the TV's own names in
# the table in `paths()` below. Never a deny-list: anything not named here (the TV also answers with other lines) is never read
# into the file.
# TODO(parental PIN, branch work/parental): the PIN must NEVER be added here, and must never be exported by any other way.
ALLOWED_TOP = ("schema", "city", "digits", "prayer", "hijri_offset", "clock24", "clock_size", "card_style", "cards",
               "greeting", "listen_pill", "update_bell", "markets", "teams", "adhan", "sunrise", "prayerbar", "wxmore", "months")
# NEVER here: the hidden update-channel switch (updatechannel) and the parental PIN. tests/settings_move_test.py checks both.

# The same lists the apps validate (android/app/src/main/java/sa/urtv/screensaver/Prefs.java; tests/settings_move_test.py
# reads that file and fails if these drift from it).
DIGITS = ("hindi", "latin")
PRAYER_METHODS = ("auto", "gulf", "jafari")
CARD_STYLES = ("current", "dark1", "glass1", "dark3", "glass3")
CLOCK_SIZES = ("large", "medium", "small")
MK_STYLES = ("arrows", "prices")
MONTHS = ("default", "levant")
SWITCHES = ("greeting", "sunrise", "prayerbar", "wxmore")   # on/off switches the TV reports as «on» / «off»
MARKETS = ("tasi", "aramco", "spx", "btc", "gold", "silver", "oil", "xrp")
MAX_MARKETS = 4
PANELS = ("shows", "prayers", "forecast", "hourly", "prayerrow", "nextprayer", "matches", "verse", "wisdom", "adhkar",
          "stocks", "tasks", "year", "smart", "today", "none")
HOME_ONLY = ("nextprayer",)   # Prefs.homeOnly: offered on the home cards, never as the screensaver's panel
HOME_CARDS = 3
MAX_TEAMS = 20
PRAYERS = ("fajr", "dhuhr", "asr", "maghrib", "isha")
PRAYER_ADJ_MAX = 20
HIJRI_MAX = 2
ADHAN_VOICES = 21                      # tilawah/AdhanVoices.COUNT
ADHAN_FAJR_VOICES = (10, 13, 17, 20)   # for Fajr only
ADHAN_VOLUME = (10, 100)
MAX_TEXT = 60                          # a city or team name
OWN = "own"                            # the person's own recording: only in the file saved before an import (it stays on that TV)

COORD = {"lat": (-90.0, 90.0, r"-?[0-9]{1,2}(\.[0-9]+)?"), "lon": (-180.0, 180.0, r"-?[0-9]{1,3}(\.[0-9]+)?")}   # the receiver's patterns
ZONE_SHAPE = re.compile(r"[A-Za-z][A-Za-z0-9_+\-]*(/[A-Za-z0-9_+\-]+){0,2}")
MAX_FILE = 100_000


class Refused(Exception):
    """A message for the person; nothing was sent."""


class TvError(Exception):
    pass


# --- talking to the TV (the same paths as saver.sh and music.sh) ---------------------------------------------------

def tv(cmd):
    """The data of the receiver's answer ('' when it gave none); TvError when the TV did not answer at all."""
    try:
        r = subprocess.run([PY, "-I", "optimize/tv.py", cmd], capture_output=True, text=True, cwd=ROOT, timeout=120,
                           encoding="utf-8", errors="replace")
    except (OSError, subprocess.SubprocessError) as e:
        raise TvError(f"the TV did not answer: {e}")
    out = r.stdout
    i = out.find('data="')
    if i < 0:
        if r.returncode != 0:
            raise TvError("the TV did not answer: " + (out + r.stderr).strip()[-300:])
        return ""
    data = out[i + 6:].rstrip()
    return data[:-1] if data.endswith('"') else data


def quote(v):
    """One shell-safe argument, as saver.sh quote(): an apostrophe stops everything (raised before anything is sent)."""
    if "'" in v:
        raise Refused(f"no apostrophes please: {v}")
    return "'" + v + "'"


def flag(v):
    return "on" if v else "off"


def signed(n):
    return f"+{n}" if n > 0 else str(n)


def coord(x):
    s = f"{x:.6f}".rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def parse_get(text):
    """The saver's GET answer as {name: value}; the value of a line is everything after 'name: '."""
    out = {}
    for line in text.splitlines():
        k, sep, v = line.partition(": ")
        if sep:
            out.setdefault(k.strip(), v.strip())
        elif line.endswith(":"):
            out.setdefault(line[:-1].strip(), "")
    return out


def read_tv(keep_own=False):
    """The TV's settings as the file's dict (only what the allow-list names). A part the TV does not report is left out.
    keep_own: a voice that is the person's own recording is kept as «own» (for the undo file); else it is left out."""
    got = tv(SAVER + "GET")
    if got.startswith("error:") or "city:" not in got:
        raise TvError("the screensaver app did not answer its settings (is it installed and up to date?): " + got[:120])
    g = parse_get(got)
    s = {"schema": CURRENT_SCHEMA}
    m = re.fullmatch(r"(.*) \((-?[\d.]+), (-?[\d.]+)\)", g.get("city", ""))
    if m and g.get("timezone"):
        s["city"] = {"name": m.group(1), "lat": float(m.group(2)), "lon": float(m.group(3)), "tz": g["timezone"]}
        if "country" in g and re.fullmatch(r"(?:[A-Z]{2})?", g["country"]):
            s["city"]["country"] = g["country"]
    for key, name in (("digits", "digits"), ("clock_size", "clocksize"), ("card_style", "cardstyle")):
        if name in g:
            s[key] = g[name]
    if "clock24" in g:
        s["clock24"] = g["clock24"] == "on"
    for k in SWITCHES:   # only what the TV reports (an older app has no sunrise, prayerbar or wxmore: absent, never guessed)
        if g.get(k) in ("on", "off"):
            s[k] = g[k] == "on"
    if g.get("months") in MONTHS:
        s["months"] = g["months"]
    if g.get("listenpill") in ("on", "off"):   # an older screensaver does not say; the key is then left out of the file
        s["listen_pill"] = g["listenpill"] == "on"
    if g.get("updatebell") in ("on", "off"):   # likewise
        s["update_bell"] = g["updatebell"] == "on"
    prayer = {}
    if "prayermethod" in g:
        prayer["method"] = g["prayermethod"]
    if g.get("prayeradj"):
        words = g["prayeradj"].split()
        prayer["adjust_on"] = words[0] == "on"
        adj = {}
        for w in words[1:]:
            k, _, v = w.partition("=")
            if k in PRAYERS and re.fullmatch(r"[+-]?\d+", v):
                adj[k] = int(v)
        if adj:
            prayer["adjust"] = adj
    if prayer:
        s["prayer"] = prayer
    if re.fullmatch(r"[+-]?\d+", g.get("hijrioffset", "x")):
        s["hijri_offset"] = int(g["hijrioffset"])
    cards = {}
    if "panel" in g:
        cards["panel"] = g["panel"]
    if "panel2" in g:
        cards["panel2"] = g["panel2"]
    if "home_panel" in g:
        cards["home"] = [] if g["home_panel"] in ("none", "") else g["home_panel"].split(",")
    if cards:
        s["cards"] = cards
    if "markets" in g or "mkstyle" in g:
        mk = {}
        if "markets" in g:
            words = g["markets"].split()
            mk["on"] = not (words and words[0] == "off")
            mk["list"] = [w for w in (words[1:] if words and words[0] == "off" else words)]
            mk["list"] = [x for w in mk["list"] for x in w.split(",") if x]
        if "mkstyle" in g:
            mk["style"] = g["mkstyle"]
        s["markets"] = mk
    if "teams" in g:
        teams = []
        for part in g["teams"].split(", "):
            tid, _, name = part.partition("|")
            if tid.strip():
                teams.append({"id": tid.strip(), "name": "" if name.strip() == "#" + tid.strip() else name.strip()})
        s["teams"] = teams
    adhan = read_adhan(keep_own)
    if g.get("adhanbar"):
        adhan["bar"] = g["adhanbar"] == "on"
    if adhan:
        s["adhan"] = adhan
    return s


def read_adhan(keep_own=False):
    """The adhan's settings from the home app (music.sh adhan's status line); {} when that app does not answer."""
    try:
        data = tv(HOME + "GET_PLAYER")
    except TvError:
        return {}
    f = dict(p.partition("=")[::2] for p in data.split("|") if "=" in p)
    out = {}
    if f.get("adhan") in ("on", "off"):
        out["on"] = f["adhan"] == "on"
    for key, name in (("voice", "voice"), ("fajr_voice", "fajr_voice")):
        v = f.get(name, "")
        if v.isdigit():
            if not (key == "voice" and int(v) in ADHAN_FAJR_VOICES):   # a Fajr recording kept as the voice by an old TV: not settable
                out[key] = int(v)
        elif v == OWN and keep_own:
            out[key] = OWN
    for key, name in (("short", "adhan_short"), ("stopkey", "adhan_stopkey")):
        if f.get(name) in ("on", "off"):
            out[key] = f[name] == "on"
    if f.get("adhan_volume", "").isdigit():
        out["volume"] = int(f["adhan_volume"])
    return out


# --- the settings as flat paths ------------------------------------------------------------------------------------

def flat(s):
    """{'city': (name, lat, lon, tz) ..., 'prayer.adjust.fajr': 2 ...} for the keys the dict has."""
    out = {}
    if "city" in s:
        c = s["city"]
        out["city"] = (c["name"], float(c["lat"]), float(c["lon"]), c["tz"])
        if "country" in c:
            out["city"] += (c["country"],)
    for k in ("digits", "clock24", "clock_size", "card_style", "greeting", "listen_pill", "update_bell", "hijri_offset", "sunrise", "prayerbar", "wxmore", "months"):
        if k in s:
            out[k] = s[k]
    for k, v in s.get("prayer", {}).items():
        if k == "adjust":
            for p, n in v.items():
                out["prayer.adjust." + p] = n
        else:
            out["prayer." + k] = v
    c = s.get("cards", {})
    if "panel" in c:
        out["cards.panel"] = c["panel"]
    if "panel2" in c:
        out["cards.panel2"] = c["panel2"]
    if "home" in c:
        out["cards.home"] = tuple(c["home"])
    for k, v in s.get("markets", {}).items():
        out["markets." + k] = tuple(v) if k == "list" else v
    if "teams" in s:
        out["teams"] = tuple(t["id"] for t in s["teams"])
    for k, v in s.get("adhan", {}).items():
        out["adhan." + k] = v
    return out


def same(a, b):
    if isinstance(a, tuple) and len(a) >= 4 and isinstance(a[1], float):   # a city: coordinates to 6 places
        return a[0] == b[0] and a[3] == b[3] and abs(a[1] - b[1]) < 1e-6 and abs(a[2] - b[2]) < 1e-6 and (len(b) == 4 or len(a) > 4 and a[4] == b[4])
    return a == b


def show(path, v, team_names=None):
    if path == "city":
        return f"{v[0]} ({coord(v[1])}, {coord(v[2])}) {v[3]}" + (f" · {v[4]}" if len(v) > 4 and v[4] else "")
    if isinstance(v, bool):
        return flag(v)
    if isinstance(v, tuple):
        if path == "teams":
            return ", ".join(f"{i} {team_names[i]}" if team_names and team_names.get(i) else i for i in v) or "none"
        return ",".join(v) or "none"
    if isinstance(v, int) and path.startswith("prayer.adjust."):
        return signed(v)
    if path == "hijri_offset":
        return signed(v)
    return str(v)


def team_names(*settings):
    out = {}
    for s in settings:
        for t in s.get("teams", []):
            if t.get("name"):
                out[t["id"]] = t["name"]
    return out


def diff(cur, new):
    """[(path, old, new)] for the settings in `new` that differ from the TV's; Refused for what the TV cannot show."""
    a, b = flat(cur), flat(new)
    if "city" in b and len(b["city"]) > 4 and len(a.get("city", ())) < 5:
        raise Refused("this TV does not report city.country (update the apps): nothing was sent")
    missing = [p for p in b if p not in a]
    if missing:
        raise Refused("this TV does not report " + ", ".join(sorted(missing)) + " (the apps may need an update: "
                      "optimize/install.sh; the adhan needs the home app): nothing was sent")
    return [(p, a[p], v) for p, v in b.items() if not same(a[p], v)]


# --- validation: all of it, before the TV is touched ----------------------------------------------------------------

def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def zone_known(z):
    if not ZONE_SHAPE.fullmatch(z) or len(z) > 40:
        return False
    try:
        import zoneinfo
        zoneinfo.ZoneInfo(z)
        return True
    except Exception:  # noqa: BLE001
        try:
            zoneinfo.ZoneInfo("UTC")
        except Exception:  # noqa: BLE001
            return True   # no time zone database on this computer (Windows without tzdata): the TV checks it, and answers error:
        return False


def text_problem(v, what, bad=""):
    if not isinstance(v, str):
        return f"{what} must be text"
    if not v.strip() or len(v) > MAX_TEXT:
        return f"{what} must be 1 to {MAX_TEXT} characters"
    if "'" in v:
        return f"{what}: no apostrophes please"
    if any(unicodedata.category(c).startswith("C") and c not in "\u200c\u200d" for c in v):   # the joiners Arabic and Persian names need are fine; control, bidi and other format characters are not
        return f"{what} has a control character"
    if any(c in v for c in bad):
        return f"{what} must not have any of {bad}"
    if v != v.strip():
        return f"{what} must not start or end with a space"
    return None


def obj(v, path, known, errs, notes):
    """v as a dict with its unknown keys noted; None (and an error) when it is not one."""
    if not isinstance(v, dict):
        errs.append(f"{path} must be an object")
        return None
    for k in v:
        if k not in known:
            notes.append(f"{path + '.' if path else ''}{k}")
    return v


def choice(v, allowed, path, errs):
    if not isinstance(v, str) or v not in allowed:
        errs.append(f"{path} must be one of {' '.join(allowed)}")
        return False
    return True


def boolean(v, path, errs):
    if not isinstance(v, bool):
        errs.append(f"{path} must be true or false")


def ranged(v, lo, hi, path, errs):
    if not is_int(v) or not lo <= v <= hi:
        errs.append(f"{path} must be a whole number from {lo} to {hi}")
        return False
    return True


def validate(d):
    """(clean dict, errors, ignored keys): the clean dict has only the allow-listed keys, each checked."""
    errs, notes, out = [], [], {}
    d = obj(d, "", ALLOWED_TOP, errs, notes)
    if d is None:
        return {}, errs, notes
    if "city" in d:
        c = obj(d["city"], "city", ("name", "lat", "lon", "tz", "country"), errs, notes)
        if c is not None:
            ok = True
            for k in ("name", "lat", "lon", "tz"):
                if k not in c:
                    errs.append(f"city.{k} is missing"); ok = False
            if "name" in c and (p := text_problem(c["name"], "city.name")):
                errs.append(p); ok = False
            for k, (lo, hi, rx) in COORD.items():
                if k not in c:
                    continue
                v = c[k]
                if isinstance(v, bool) or not isinstance(v, (int, float)):
                    errs.append(f"city.{k} must be a number"); ok = False
                elif not math.isfinite(v):
                    errs.append(f"city.{k} must be a finite number (not NaN or infinity)"); ok = False
                elif not lo <= v <= hi:
                    errs.append(f"city.{k} must be from {lo:g} to {hi:g}"); ok = False
                elif not re.fullmatch(rx, coord(float(v))):
                    errs.append(f"city.{k} is not a number the TV accepts"); ok = False
            if "tz" in c:
                if not isinstance(c["tz"], str):
                    errs.append("city.tz must be text"); ok = False
                elif not zone_known(c["tz"]):
                    errs.append("city.tz must be a time zone like Asia/Riyadh or Asia/Kuwait"); ok = False
            if "country" in c and (not isinstance(c["country"], str) or not re.fullmatch(r"(?:[A-Za-z]{2})?", c["country"])):
                errs.append("city.country must be a two-letter country code or empty"); ok = False
            if ok:
                out["city"] = {k: c[k] for k in ("name", "lat", "lon", "tz")}
                if "country" in c:
                    out["city"]["country"] = c["country"].upper()
    for key, allowed in (("digits", DIGITS), ("clock_size", CLOCK_SIZES), ("card_style", CARD_STYLES)):
        if key in d and choice(d[key], allowed, key, errs):
            out[key] = d[key]
    if "months" in d and choice(d["months"], MONTHS, "months", errs):
        out["months"] = d["months"]
    for key in ("clock24", "greeting", "listen_pill", "update_bell", "sunrise", "prayerbar", "wxmore"):
        if key in d:
            n = len(errs)
            boolean(d[key], key, errs)
            if len(errs) == n:
                out[key] = d[key]
    if "hijri_offset" in d and ranged(d["hijri_offset"], -HIJRI_MAX, HIJRI_MAX, "hijri_offset", errs):
        out["hijri_offset"] = d["hijri_offset"]
    if "prayer" in d:
        p = obj(d["prayer"], "prayer", ("method", "adjust_on", "adjust"), errs, notes)
        if p is not None:
            o = {}
            if "method" in p and choice(p["method"], PRAYER_METHODS, "prayer.method", errs):
                o["method"] = p["method"]
            if "adjust_on" in p:
                n = len(errs)
                boolean(p["adjust_on"], "prayer.adjust_on", errs)
                if len(errs) == n:
                    o["adjust_on"] = p["adjust_on"]
            if "adjust" in p:
                a = obj(p["adjust"], "prayer.adjust", PRAYERS, errs, notes)
                if a is not None:
                    o["adjust"] = {k: v for k, v in a.items() if k in PRAYERS
                                   and ranged(v, -PRAYER_ADJ_MAX, PRAYER_ADJ_MAX, f"prayer.adjust.{k}", errs)}
            out["prayer"] = o
    if "cards" in d:
        c = obj(d["cards"], "cards", ("panel", "panel2", "home"), errs, notes)
        if c is not None:
            o = {}
            if "panel" in c and choice(c["panel"], tuple(x for x in PANELS if x not in HOME_ONLY), "cards.panel", errs):
                o["panel"] = c["panel"]
            if "panel2" in c and choice(c["panel2"], tuple(x for x in PANELS if x not in HOME_ONLY), "cards.panel2", errs):
                o["panel2"] = c["panel2"]
                # the same rules the app applies (SecondCard.refuse): never the first card again, and none without a first card
                if "panel" in o and o["panel2"] != "none" and (o["panel2"] == o["panel"] or o["panel"] == "none"):
                    errs.append("cards.panel2 must be another card than cards.panel (and cards.panel must not be none), or none")
                    del o["panel2"]
            if "home" in c:
                h = c["home"]
                if not isinstance(h, list) or len(h) > HOME_CARDS or len(set(map(str, h))) != len(h) \
                        or not all(isinstance(x, str) and x in PANELS and x != "none" for x in h):
                    errs.append(f"cards.home must be a list of up to {HOME_CARDS} different cards of {' '.join(x for x in PANELS if x != 'none')} ([] = none)")
                else:
                    o["home"] = h
            out["cards"] = o
    if "markets" in d:
        m = obj(d["markets"], "markets", ("on", "list", "style"), errs, notes)
        if m is not None:
            o = {}
            if "on" in m:
                n = len(errs)
                boolean(m["on"], "markets.on", errs)
                if len(errs) == n:
                    o["on"] = m["on"]
            if "style" in m and choice(m["style"], MK_STYLES, "markets.style", errs):
                o["style"] = m["style"]
            if "list" in m:
                v = m["list"]
                if not isinstance(v, list) or not 1 <= len(v) <= MAX_MARKETS or len(set(map(str, v))) != len(v) \
                        or not all(isinstance(x, str) and x in MARKETS for x in v):
                    errs.append(f"markets.list must be 1 to {MAX_MARKETS} different ones of {' '.join(MARKETS)}")
                else:
                    o["list"] = v
            out["markets"] = o
    if "teams" in d:
        t = d["teams"]
        if not isinstance(t, list) or len(t) > MAX_TEAMS:
            errs.append(f"teams must be a list of up to {MAX_TEAMS} teams")
        else:
            clean, n = [], len(errs)
            for i, x in enumerate(t):
                x = obj(x, f"teams[{i}]", ("id", "name"), errs, notes)
                if x is None:
                    continue
                if not isinstance(x.get("id"), str) or not re.fullmatch(r"[0-9]{1,12}", x["id"]):
                    errs.append(f"teams[{i}].id must be the club's number, digits only"); continue
                name = x.get("name", "")
                if name != "" and (p := text_problem(name, f"teams[{i}].name", ",;|")):
                    errs.append(p); continue
                clean.append({"id": x["id"], "name": name})
            if len({x["id"] for x in clean}) != len(clean) and len(errs) == n:
                errs.append("teams has a club twice")
            if len(errs) == n:
                out["teams"] = clean
    if "adhan" in d:
        a = obj(d["adhan"], "adhan", ("on", "voice", "fajr_voice", "short", "stopkey", "volume", "bar"), errs, notes)
        if a is not None:
            o = {}
            for k in ("on", "short", "stopkey", "bar"):
                if k in a:
                    n = len(errs)
                    boolean(a[k], f"adhan.{k}", errs)
                    if len(errs) == n:
                        o[k] = a[k]
            if "voice" in a:
                v = a["voice"]
                if v == OWN or (is_int(v) and 1 <= v <= ADHAN_VOICES and v not in ADHAN_FAJR_VOICES):
                    o["voice"] = v
                else:
                    errs.append(f"adhan.voice must be 1 to {ADHAN_VOICES} (not the Fajr ones {', '.join(map(str, ADHAN_FAJR_VOICES))}), or {OWN}")
            if "fajr_voice" in a:
                v = a["fajr_voice"]
                if v == OWN or (is_int(v) and 1 <= v <= ADHAN_VOICES):
                    o["fajr_voice"] = v
                else:
                    errs.append(f"adhan.fajr_voice must be 1 to {ADHAN_VOICES}, or {OWN}")
            if "volume" in a and ranged(a["volume"], ADHAN_VOLUME[0], ADHAN_VOLUME[1], "adhan.volume", errs):
                o["volume"] = a["volume"]
            out["adhan"] = o
    return out, errs, notes


def migrate(d):
    """The dict at the current schema; Refused for a newer one, or an older one that has no migration."""
    v = d.get("schema")
    if not is_int(v) or v < 1:
        raise Refused('this is not a settings file: it needs "schema": 1')
    if v > CURRENT_SCHEMA:
        raise Refused(f"this file is schema {v}, made by a newer version of these tools (this one knows schema "
                      f"{CURRENT_SCHEMA}): update this folder (git pull), then run the same command again")
    while v < CURRENT_SCHEMA:
        if v not in MIGRATIONS:
            raise Refused(f"this file is schema {v} and there is no way to bring it to schema {CURRENT_SCHEMA}")
        d = MIGRATIONS[v](d)
        v += 1
        d["schema"] = v
    return d


def no_constants(name):
    raise ValueError(f"{name} is not allowed")


def no_duplicates(pairs):
    out = {}
    for k, v in pairs:
        if k in out:
            raise ValueError(f"{k} is given twice")
        out[k] = v
    return out


def find_file(name):
    """The file the person named: a relative name is the caller's (saver.sh hands over the directory it was run from; it moves to the
    repository root first), else the repository root's, so `import optimize/run/settings-before-import.json` works from anywhere."""
    p = Path(name)
    if p.is_absolute():
        return p
    caller = os.environ.get("SAVER_CALLER_DIR")
    for base in ([Path(caller)] if caller else []) + [ROOT]:
        if (base / p).is_file():
            return base / p
    return (Path(caller) if caller else ROOT) / p


def load(path):
    """The file's validated settings (a dict), plus the keys that are ignored; Refused with the reason otherwise."""
    p = Path(path)
    if not p.is_file():
        raise Refused(f"{path} is not a file")
    if p.stat().st_size > MAX_FILE:
        raise Refused(f"{path} is too big for a settings file")
    try:
        text = p.read_bytes().decode("utf-8-sig")
    except UnicodeDecodeError:
        raise Refused(f"{path} is not UTF-8 text (was it written by a shell that saves UTF-16?)")
    try:
        d = json.loads(text, parse_constant=no_constants, object_pairs_hook=no_duplicates)
    except json.JSONDecodeError as e:
        raise Refused(f"{path} is not complete or not valid JSON (line {e.lineno}, column {e.colno}: {e.msg}): was the export "
                      "cut short? export it again")
    except RecursionError:
        raise Refused(f"{path} is nested too deeply to be a settings file")
    except ValueError as e:
        raise Refused(f"{path} is not valid: {e}")
    if not isinstance(d, dict):
        raise Refused("this is not a settings file: it must be a JSON object")
    d = migrate(d)
    clean, errs, notes = validate(d)
    if errs:
        raise Refused("the file is refused, nothing was sent:\n" + "\n".join("  - " + e for e in errs))
    clean["schema"] = CURRENT_SCHEMA
    return clean, notes


# --- what goes to the TV ----------------------------------------------------------------------------------------------

def ops(values, names):
    """values: {path: value}; names: team id -> name. Returns (the saver's SET extras as one string or '', [the home app's commands])."""
    ex, home = [], []

    def es(name, v):
        ex.append(f"--es {name} {quote(v)}")

    for p, v in values.items():
        if p == "city":
            es("city", v[0]); es("lat", coord(v[1])); es("lon", coord(v[2])); es("tz", v[3])
            if len(v) > 4:
                es("country", v[4])
        elif p == "digits":
            es("digits", v)
        elif p == "clock24":
            es("clock24", flag(v))
        elif p == "clock_size":
            es("clocksize", v)
        elif p == "card_style":
            es("cardstyle", v)
        elif p in ("greeting", "sunrise", "prayerbar", "wxmore"):
            es(p, flag(v))
        elif p == "months":
            es("months", v)
        elif p == "cards.panel2":
            es("panel2", v)
        elif p == "listen_pill":
            es("listenpill", flag(v))
        elif p == "update_bell":
            es("updatebell", flag(v))
        elif p == "hijri_offset":
            es("hijrioffset", signed(v))
        elif p == "prayer.method":
            es("prayermethod", v)
        elif p == "prayer.adjust_on":
            es("prayeradj", flag(v))
        elif p.startswith("prayer.adjust."):
            es("adj_" + p.rsplit(".", 1)[1], signed(v))
        elif p == "cards.panel":
            es("panel", v)
        elif p == "cards.home":
            es("home_panel", ",".join(v) or "none")
        elif p == "markets.list":
            es("markets", ",".join(v))
        elif p == "markets.on":
            es("mkbar", flag(v))
        elif p == "markets.style":
            es("mkstyle", v)
        elif p == "teams":
            es("teams", ",".join(f"{i}:{names.get(i, '')}" for i in v))
        elif p == "adhan.bar":
            es("adhanbar", flag(v))
        elif p == "adhan.on":
            home.append(HOME + ("ADHAN_ON" if v else "ADHAN_OFF"))
        elif p == "adhan.voice":
            home.append(HOME + f"VOICE --ei voice {0 if v == OWN else v}")
        elif p == "adhan.fajr_voice":
            home.append(HOME + f"FAJR_VOICE --ei voice {0 if v == OWN else v}")
        elif p == "adhan.short":
            home.append(HOME + f"ADHAN_SHORT --ez on {flag(v).replace('on', 'true').replace('off', 'false')}")
        elif p == "adhan.stopkey":
            home.append(HOME + f"ADHAN_STOPKEY --ez on {flag(v).replace('on', 'true').replace('off', 'false')}")
        elif p == "adhan.volume":
            home.append(HOME + f"ADHAN_VOLUME --ei percent {v}")
    return (SAVER + "SET " + " ".join(ex)) if ex else "", home


def values_for(changes, which):
    """{path: value} of the changes' old (which=1) or new (which=2) side."""
    return {c[0]: c[which] for c in changes}


def rescheduled(changes):
    return any(c[0] == "prayer.method" or c[0].startswith("prayer.adjust") for c in changes)


def reschedule():
    try:   # as saver.sh does after prayermethod and prayeradj: the adhan alarm was set for the old times
        tv(HOME + "ADHAN_RESCHEDULE")
    except TvError:
        pass


def failed_answer(out):
    return out.startswith("error:")


def mismatches(changes, which):
    """The paths of the changes that the TV does not show as the old (1) or new (2) side now."""
    now = flat(read_tv(keep_own=True))
    return [c[0] for c in changes if c[0] not in now or not same(now[c[0]], c[which])]


def apply(changes, names, before_path):
    """All or nothing. Prints the result; True when everything is in."""
    new_cmd, new_home = ops(values_for(changes, 2), names)
    old_cmd, old_home = ops(values_for(changes, 1), names)   # built before anything is sent: an apostrophe stops here
    saver_in, home_tried = False, 0
    why = None
    try:
        if new_cmd:
            saver_in = True   # before the send: if the answer is lost after the TV applied it, it still gets put back
            out = tv(new_cmd)
            if failed_answer(out):
                saver_in = False   # the app checks the whole SET first and refused it: nothing of it was applied
                why = out
        for h in new_home if why is None else []:
            home_tried += 1
            out = tv(h)
            if failed_answer(out):
                why = out
                break
        if why is None:
            bad = mismatches(changes, 2)
            if bad:
                why = "the TV does not show what was sent for " + ", ".join(bad)
    except TvError as e:
        why = str(e)
    if why is None:
        if rescheduled(changes):
            reschedule()
        return True
    # put the old values back: the saver's in one SET, every adhan setting that was sent (the failed one too), last first
    undone = True
    steps = []
    if saver_in:
        steps.append(old_cmd)
    steps += list(reversed(old_home[:home_tried]))
    for cmd in steps:   # one step failing never stops the others
        try:
            undone &= not failed_answer(tv(cmd))
        except TvError:
            undone = False
    if saver_in and rescheduled(changes):
        try:
            reschedule()
        except TvError:
            pass
    try:
        undone &= not mismatches(changes, 1)
    except TvError:
        undone = False
    first = why.strip().splitlines()[0] if why.strip() else "the TV refused it"
    if undone:
        print(f"!! The import failed ({first}). The TV was put back as it was: nothing changed.")
    else:
        print(f"!! The import failed ({first}) and the TV could NOT be put back completely. Its settings from before are "
              f"saved in {before_path}: run `optimize/saver.sh import {before_path} --yes` to put them back.")
    return False


# --- the commands -------------------------------------------------------------------------------------------------

def without_own(s):
    """A copy of the settings without an adhan voice that is the person's own recording (it stays on that TV)."""
    out = dict(s)
    if "adhan" in out:
        out["adhan"] = {k: v for k, v in out["adhan"].items() if v != OWN}
    return out


def export_main():
    full = read_tv(keep_own=True)
    s = without_own(full)
    notes = []
    if not any(k in s.get("adhan", {}) for k in ("on", "voice", "short", "stopkey", "volume")):
        notes.append("note: the adhan's voice, volume and switches are not in this file (the home app did not answer, or the voice is "
                     "your own recording, which stays on this TV)")
    if full.get("adhan", {}) != s.get("adhan", {}):
        notes.append("note: an adhan voice that is your own recording is left out (the recording stays on this TV)")
    _, errs, _ = validate(s)
    notes += [f"note: this file would not pass an import: {e}" for e in errs]
    print(json.dumps(s, ensure_ascii=False, indent=2))
    for n in notes:
        print(n, file=sys.stderr)
    return 0


def import_main(args):
    yes = "--yes" in args
    files = [a for a in args if a != "--yes"]
    if len(files) != 1 or files[0].startswith("-"):
        raise Refused("usage: optimize/saver.sh import FILE.json [--yes]   (without --yes it only shows what would change)")
    source = find_file(files[0])
    new, ignored = load(source)
    for k in ignored:
        print(f"ignored (not a setting this version moves): {k}")
    cur = read_tv(keep_own=True)
    changes = diff(cur, new)
    names = team_names(new, cur)
    # every value must be sendable before anything is, the new ones and the old ones that would be put back: an apostrophe stops here
    ops(values_for(changes, 2), names)
    ops(values_for(changes, 1), names)
    if not changes:
        print("Nothing to change: the TV already has these settings.")
        return 0
    print(f"Preview: {len(changes)} setting{'s' if len(changes) != 1 else ''} would change on the TV")
    for p, old, nv in changes:
        print(f"  {p}: {show(p, old, names)} \u2192 {show(p, nv, names)}")
    if not yes:
        print("Nothing was sent. Run the same command with --yes to apply it.")
        return 0
    before = Path(BEFORE) if os.path.isabs(BEFORE) else ROOT / BEFORE
    if before.exists() and source.exists() and before.samefile(source):
        # this IS the undo file: keep it as it is, it is still the way back to the settings before the first import
        print(f"This is the file the last import saved: it is kept as it is ({BEFORE}), so it stays the way back.")
    else:
        try:
            before.parent.mkdir(parents=True, exist_ok=True)
            before.write_text(json.dumps(cur, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        except OSError as e:
            raise Refused(f"could not save the TV's settings before the import ({e}): nothing was sent")
        print(f"The TV's settings from before are saved in {BEFORE}")
    if not apply(changes, names, BEFORE):
        return 1
    print(f"Done: {len(changes)} setting{'s' if len(changes) != 1 else ''} changed. To undo: "
          f"optimize/saver.sh import {BEFORE} --yes")
    return 0


def main(argv):
    if not argv or argv[0] not in ("export", "import"):
        print("usage: optimize/saver.sh export > settings.json | optimize/saver.sh import settings.json [--yes]", file=sys.stderr)
        return 2
    try:
        return export_main() if argv[0] == "export" else import_main(argv[1:])
    except Refused as e:
        print(f"error: {e}", file=sys.stderr)
        return 1
    except TvError as e:
        print(f"error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
