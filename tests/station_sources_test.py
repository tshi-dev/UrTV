#!/usr/bin/env python3
"""4.4 radio reliability, part 2: a station's addresses in turn: its own, «alt», then «mirrors» (stations.json; an older app ignores the key), https
only, no repeats, at most six; when a source failed every try the next one is taken, round and round (StationSources, run for real).
    python3 tests/station_sources_test.py"""
import json, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, cat = read(T / "MusicService.java"), read(T / "Catalog.java")
check("StationSources is plain Java", "import android" not in read(T / "StationSources.java"))
check("Catalog reads «mirrors» (https only), keeping «alt»", 's.optJSONArray("mirrors")' in cat and 'if (m.startsWith("https://")) st.mirrors.add(m);' in cat and "st.alt = alt;" in cat)
check("a pick takes every source of the station, the first one first", "radioSources = StationSources.of(st.url, st.alt, st.mirrors," in svc and "radioSource = 0;" in svc)
check("after a source failed every try, the next one, round and round", "radioSource = StationSources.next(radioSource, radioSources.size());" in svc
      and "radioAddress = radioSources.get(radioSource);" in svc)
stations = json.loads(read(ROOT / "android/launcher/src/main/assets/quran/stations.json"))["stations"]
bad = [s["name"] for s in stations if "mirrors" in s and not (isinstance(s["mirrors"], list) and all(isinstance(u, str) and u.startswith("https://") for u in s["mirrors"]))]
check("stations.json: «mirrors», where given, is a list of https addresses", not bad, bad)
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
import java.util.*;
public class SrcRun { static void out(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  List<String> s = StationSources.of("https://a/1", "https://b/2", Arrays.asList("https://c/3", "http://d/4", "https://a/1"), Arrays.asList("https://e/5"));
  out("own, alt, mirrors, published, in that order; no http, no repeat", s.equals(Arrays.asList("https://a/1", "https://b/2", "https://c/3", "https://e/5")));
  out("no alt and no mirrors: the one address (as before)", StationSources.of("https://a/1", null, null, null).equals(Arrays.asList("https://a/1")));
  List<String> many = new ArrayList<>(); for (int i = 0; i < 10; i++) many.add("https://m/" + i);
  out("at most six", StationSources.of("https://a/1", null, many, null).size() == StationSources.MAX && StationSources.MAX == 6);
  out("an address with a space or far too long is left out", StationSources.of("https://a/1", "https://b /2", null, null).size() == 1);
  out("the next one, round and round", StationSources.next(0, 3) == 1 && StationSources.next(2, 3) == 0 && StationSources.next(0, 1) == 0 && StationSources.next(0, 0) == 0);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "SrcRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "StationSources.java"), str(Path(tmp, "SrcRun.java"))], capture_output=True, text=True)
        check("StationSources compiles on its own", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.SrcRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 5, len(out.splitlines()))
sys.exit(1 if failed else 0)
