#!/usr/bin/env python3
"""4.4 radio reliability, part 3: the stations check (CI, once a day) counts each station's failed checks in a row into stations/status.json; three in
a row is «dead» and the home app hides it from the list and steps over it, never the station in use, never all of them, never from a status
more than four days old. The extra mirrors that played are tried after the station's own (StationStatus run for real, check_stations.status() too).
    python3 tests/station_status_test.py"""
import importlib.util, json, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

# --- the CI side: status() from what played and the last run's file ---
spec = importlib.util.spec_from_file_location("check_stations", ROOT / ".github/tools/check_stations.py")
cs = importlib.util.module_from_spec(spec); spec.loader.exec_module(cs)
sts = [{"name": "أ", "url": "https://a/1"}, {"name": "ب", "url": "https://b/1", "alt": "https://b/2", "mirrors": ["https://b/3", "http://b/4"]}, {"name": "ج", "url": "https://c/1"}]
check("a station's sources: own, alt, mirrors, extra mirrors; https only past the first, no repeats",
      cs.sources(sts[1], ["https://b/5", "https://b/2"]) == ["https://b/1", "https://b/2", "https://b/3", "https://b/5"])
prev = {"v": 1, "stations": {"أ": {"fails": 2}, "ب": {"fails": 5}, "ج": {"fails": 1}}}
played = {"أ": [("https://a/1", False)], "ب": [("https://b/1", False), ("https://b/2", True)], "ج": [("https://c/1", True), ("https://c/9", True)]}
s = cs.status(sts, played, prev, {"ج": ["https://c/9"]}, 1_700_000_000_000)
e = s["stations"]
check("down a third time in a row: dead", e["أ"] == {"fails": 3, "dead": True, "mirrors": []}, e["أ"])
check("its second source plays: up, the count back to 0", e["ب"]["fails"] == 0 and not e["ب"]["dead"], e["ب"])
check("an extra mirror that played is published (stations.json's own are not repeated)", e["ج"]["mirrors"] == ["https://c/9"] and e["ب"]["mirrors"] == [], e)
check("the file: v 1, its time in ms, the threshold", s["v"] == 1 and s["checked_ms"] == 1_700_000_000_000 and s["dead_after"] == cs.DEAD_AFTER == 3)
none = cs.status(sts, {k: [(u, False) for u, _ in v] for k, v in played.items()}, prev, {}, 1)
check("nothing played at all (the runner's network): the counts stay as they were", [none["stations"][k]["fails"] for k in "أبج"] == [2, 5, 1], none)
fresh = cs.status(sts, played, None, {}, 1)["stations"]
check("no last file: a station down now has 1 failed check", fresh["أ"]["fails"] == 1 and not fresh["أ"]["dead"], fresh)
check("a garbled last file is ignored", cs.status(sts, played, {"v": 1, "stations": {"أ": {"fails": "x"}}}, {}, 1)["stations"]["أ"]["fails"] == 1)
mir = json.loads(read(ROOT / ".github/tools/station_mirrors.json"))
check("station_mirrors.json: v 1, a map of https lists by an Arabic name of stations.json", mir["v"] == 1 and isinstance(mir["mirrors"], dict)
      and all(isinstance(v, list) and all(u.startswith("https://") for u in v) for v in mir["mirrors"].values()))
names = {x["name"] for x in json.loads(read(ROOT / "android/launcher/src/main/assets/quran/stations.json"))["stations"]}
check("every station_mirrors.json key is a station", set(mir["mirrors"]) <= names, set(mir["mirrors"]) - names)
wf = read(ROOT / ".github/workflows/stations-check.yml")
check("the check runs once a day and reads the last run's status", "schedule:" in wf and "cron:" in wf and "git show FETCH_HEAD:stations/status.json" in wf and "STATIONS_PREV:" in wf)
check("status.json goes out with the reports (data-stations, copied to urtv-data stations/)", "git add -f stations" in wf and '(OUT / "status.json")' in read(ROOT / ".github/tools/check_stations.py"))

patch = read(ROOT / "docs/patches/urtv-data-stations-status.patch")
check("the patch for urtv-data's Stations: daily, the last status read from its data branch, the mirrors from UrTV, status.json published with the rest",
      "+    - cron: '17 5 * * *'" in patch and "git show FETCH_HEAD:stations/status.json" in patch and "STATIONS_PREV:" in patch
      and "tools/inputs.sh .github/tools/station_mirrors.json || true" in patch)
added = "\n".join(l[1:] for l in patch.split("+++ b/.github/tools/check_stations.py", 1)[1].split("\ndiff --git", 1)[0].splitlines() if l.startswith("+") and not l.startswith("+++"))
mine = read(ROOT / ".github/tools/check_stations.py")
check("the patch's check_stations.py lines are this one's (urtv-data copies the tool unchanged)", added and all(l in mine for l in added.splitlines()))

# --- the app side ---
feed, svc, act = read(H / "StationFeed.java"), read(T / "MusicService.java"), read(T / "MainActivity.java")
check("StationStatus is plain Java", "import android" not in read(T / "StationStatus.java"))
cf, dh = read(H / "ContentFile.java"), read(H / "DataHost.java")
check("the status: urtv-data's stations/status.json first, UrTV's data-stations branch after a 404 or no answer (DataHost.get), kept and fetched in the background",
      'URL = DataHost.OLD + "data-stations/stations/status.json"' in feed and "ContentFile.refreshFrom(app, URL, NAME," in feed and "ContentFile.read(" in feed
      and "fetch(c, name, maxAgeMs, () -> DataHost.get(oldUrl, 15000, 20000, MAX_BYTES));" in cf and '{"data-stations/stations/", "stations/"},' in dh
      and "if (r.status == 200) return r;" in dh)
check("refresh() for the other content files is as before (urtv-data only)", "fetch(c, name, maxAgeMs, () -> Net.get(DataHost.NEW + path, null, 15000, 20000, MAX_BYTES));" in cf)
check("the list leaves a dead station out, never the one in use", "StationStatus.hidden(sa.urtv.home.StationFeed.status(MainActivity.this), keys," in act
      and "prefs.source() == Prefs.SOURCE_RADIO ? prefs.station() : -1" in act and "if (gone[i]) continue;" in act)
check("next and previous step over a dead station", "playRadio(StationStatus.step(station, 1, hiddenStations()));" in svc and "playRadio(StationStatus.step(station, -1, hiddenStations()));" in svc)
check("the published mirrors come after the station's own", "StationSources.of(st.url, st.alt, st.mirrors, StationStatus.mirrors(sa.urtv.home.StationFeed.status(this), st.key," in svc)
check("the same threshold in the app and the check", "static final int DEAD_AFTER = 3;" in read(T / "StationStatus.java"))

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
import java.util.*;
public class StRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
static Map<String, Object> e(double fails, boolean dead, String... m) { Map<String, Object> x = new HashMap<>(); x.put("fails", fails); x.put("dead", dead); x.put("mirrors", new ArrayList<Object>(Arrays.asList(m))); return x; }
public static void main(String[] a) {
  long now = 1_700_000_000_000L;
  Map<String, Object> st = new HashMap<>(); st.put("A", e(3, true)); st.put("B", e(0, false, "https://b/9")); st.put("C", e(4, true)); st.put("D", e(1, true));
  Map<String, Object> s = new HashMap<>(); s.put("v", 1.0); s.put("checked_ms", (double) (now - 3600_000L)); s.put("stations", st);
  List<String> keys = Arrays.asList("A", "B", "C", "D");
  out("dead after three failed checks in a row (a «dead» with fewer is not)", StationStatus.dead(s, "A", now) && !StationStatus.dead(s, "B", now) && !StationStatus.dead(s, "D", now));
  boolean[] h = StationStatus.hidden(s, keys, -1, now);
  out("the dead ones hidden", h[0] && !h[1] && h[2] && !h[3]);
  out("never the one in use", !StationStatus.hidden(s, keys, 0, now)[0]);
  Map<String, Object> all = new HashMap<>(s); Map<String, Object> st2 = new HashMap<>(); for (String k : keys) st2.put(k, e(9, true)); all.put("stations", st2);
  out("never all of them", !StationStatus.hidden(all, keys, -1, now)[0] && !StationStatus.hidden(all, keys, -1, now)[3]);
  out("a status more than four days old is not used", !StationStatus.dead(s, "A", now + StationStatus.MAX_AGE_MS + 1) && StationStatus.mirrors(s, "B", now + StationStatus.MAX_AGE_MS + 1).isEmpty());
  out("nor one from far ahead of the TV's clock", !StationStatus.dead(s, "A", now - 3 * 3600_000L));
  Map<String, Object> v2 = new HashMap<>(s); v2.put("v", 2.0);
  out("no file, another version, an unknown station: nothing hidden", !StationStatus.dead(null, "A", now) && !StationStatus.dead(v2, "A", now) && !StationStatus.dead(s, "Z", now)
      && StationStatus.hidden(null, keys, -1, now).length == 4 && !StationStatus.hidden(null, keys, -1, now)[0]);
  out("the published mirrors", StationStatus.mirrors(s, "B", now).equals(Arrays.asList("https://b/9")) && StationStatus.mirrors(s, "A", now).isEmpty() && StationStatus.mirrors(null, "B", now).isEmpty());
  out("next and previous step over the hidden, round and round", StationStatus.step(1, 1, h) == 3 && StationStatus.step(3, 1, h) == 1 && StationStatus.step(1, -1, h) == 3 && StationStatus.step(3, -1, h) == 1);
  out("nothing hidden: the next one, as before", StationStatus.step(3, 1, new boolean[4]) == 0 && StationStatus.step(0, -1, new boolean[4]) == 3);
  out("from a hidden station (the one that was in use) to the next shown", StationStatus.step(0, 1, h) == 1 && StationStatus.step(2, -1, h) == 1);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "StRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "StationStatus.java"), str(Path(tmp, "StRun.java"))], capture_output=True, text=True)
        check("StationStatus compiles on its own", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.StRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 11, len(out.splitlines()))

    # the launcher's DataHost, run for real against a local server: urtv-data has no stations/status.json yet (404), UrTV's data-stations has it
    run2 = r"""package sa.urtv.home;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
public class FeedRun { public static void main(String[] a) throws Exception {
  HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  StringBuilder asked = new StringBuilder();
  s.createContext("/", x -> { String p = x.getRequestURI().getPath(); synchronized (asked) { asked.append(p).append(' '); }
    byte[] b = ("got " + p).getBytes(StandardCharsets.UTF_8); int code = p.startsWith("/old/data-stations/") || p.equals("/new/stations/report.json") ? 200 : 404;
    x.sendResponseHeaders(code, b.length); x.getResponseBody().write(b); x.close(); });
  s.start();
  String base = "http://127.0.0.1:" + s.getAddress().getPort();
  System.out.println("FALLBACK " + DataHost.getWith(base + "/old/", base + "/new/", base + "/old/data-stations/stations/status.json", 3000, 5000, 1 << 16).text());
  System.out.println("ASKED " + asked);
  s.stop(0);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "FeedRun.java").write_text(run2)
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "DataHost.java"), str(H / "Net.java"), str(H / "NetLog.java"), str(Path(tmp, "FeedRun.java"))], capture_output=True, text=True)
        check("the launcher's DataHost compiles on a plain JVM", r.returncode == 0, r.stderr[-600:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FeedRun"], capture_output=True, text=True, timeout=60).stdout
            check("status.json: a 404 at urtv-data, then UrTV's data-stations answers", "FALLBACK got /old/data-stations/stations/status.json" in out, out)
            check("asked in that order: urtv-data's stations/ first", "ASKED /new/stations/status.json /old/data-stations/stations/status.json" in out, out)
sys.exit(1 if failed else 0)
