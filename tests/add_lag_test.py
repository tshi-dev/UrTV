#!/usr/bin/env python3
"""4.4 beta 2 (the owner, build 570: «adding teams or leagues freezes, the search between clubs lags»), the screensaver's side:
«الدوريات كاملة» (LeaguesActivity) waits for the typing to rest, matches on names folded once, reads the chosen leagues once a draw, changes a
ticked row in place (no new list, the focus stays) and shows at most MAX_FOUND of a search; «الفرق المتابعة» reads and searches the club list
off the UI thread, and a club's names are normalised once. The page's side: tests/add_lag.test.mjs.

    python3 tests/add_lag_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
act, st, ts, lg = read(APP / "LeaguesActivity.java"), read(APP / "SettingsActivity.java"), read(APP / "TeamSearch.java"), read(APP / "LeagueGroups.java")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "LeagueGroups.java"), str(ROOT / "tests/AddLagRun.java")], capture_output=True, text=True)
        check("LeagueGroups and the run compile on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            o = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.AddLagRun"], capture_output=True, text=True)
            print("\n".join(l for l in o.stdout.splitlines() if l.startswith("INFO")))
            bad = [l for l in o.stdout.splitlines() if l.startswith("FAIL")]
            check("the folded search: the old answers, the same hits on 842 rows, and cheaper", o.returncode == 0 and not bad and o.stdout.count("PASS") == 3, bad[:3] or o.stderr[-300:])
else:
    print("SKIP the JVM run (no javac here)")

watcher = act.split("afterTextChanged(Editable s)", 1)[1].split("\n", 1)[0]
check("«الدوريات كاملة»: a key does not draw the list; the draw waits for the typing to rest (SEARCH_MS)",
      "fill()" not in watcher and "h.postDelayed(searchNow, SEARCH_MS)" in watcher and "static final long SEARCH_MS = 250;" in act)
fill = act.split("    private void fill() {", 1)[1].split("\n    }\n", 1)[0]
check("…a draw reads the chosen leagues once (not once a row) and matches on names folded once",
      "chosenNow = prefs.leagues();" in fill and "for (String s : chosenNow)" in act and "prefs.leagues()" not in act.split("private boolean chosen(", 1)[1].split("}", 1)[0]
      and "LeagueGroups.matchesFolded(drawn," in fill and "foldedCat = foldAll(rows)" in act)
check("…the catalogue's names folded off the UI thread when it is refreshed", "final List<String> folded = foldAll(got);   // off the UI thread too" in act)
check("…a search shows at most MAX_FOUND (60) of the catalogue's leagues, then says to type more (Arabic, with its English entry)",
      "static final int MAX_FOUND = 60;" in act and "if (searching && found >= MAX_FOUND) { more = true; break; }" in fill and 'S.t("اكتب أكثر لترى بقية النتائج")' in fill
      and "اكتب أكثر لترى بقية النتائج\tType more to see the other results" in read(ROOT / "i18n/en/saver.tsv"))
tog = act.split("private void toggleLeague(String stored, String tag) {", 1)[1].split("\n    }\n", 1)[0]
check("…a tick changes its own row in place (the focus stays, no list built again); the whole list only when the row is not there",
      "paintTick((LinearLayout) row, !had); showCount();" in tog and tog.count("fill()") == 1 and "else fill();" in tog
      and 'toggleLeague(l[0], "lg:" + l[0])' in act and "toggleLeague(stored, tag)" in act and "GlassPaint.follow(row, look);" in act)
check("…a tournament's tick too, with the switch's count", "paintTick((LinearLayout) v, !now.contains(t[0]));" in act and 'list.findViewWithTag("tour:all")' in act)
check("…the waiting draw is dropped when the screen closes", "protected void onDestroy() { h.removeCallbacks(searchNow);" in act)
srch = st.split("private void searchTeam(final int slot) {", 1)[1].split("\n    }\n", 1)[0]
check("«الفرق المتابعة»: the club search runs off the UI thread (the list read and searched in a thread), its results shown on the UI thread",
      "new Thread(() -> {" in srch and "Teams.search(app, q)" in srch and "runOnUiThread(() -> {" in srch and srch.index("Teams.search(app, q)") < srch.index("runOnUiThread"))
check("…a league's clubs too, and the list is read ahead when «الفرق المتابعة» opens", "Teams.inLeague(app, league);   // 4.4 beta 2: read off the UI thread" in st
      and "warmTeams();" in st.split("private void manageTeams() {", 1)[1][:200] and 'new Thread(() -> Teams.all(c), "teams-load").start();' in st)
check("TeamSearch: a club's names normalised once (words()), not at every search", "synchronized List<List<String>> words()" in ts and "for (List<String> w : t.words())" in ts
      and "tier(q, normalize(x))" not in ts)
sys.exit(1 if failed else 0)
