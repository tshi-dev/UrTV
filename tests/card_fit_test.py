#!/usr/bin/env python3
"""4.4 the owner: a home card that does not suit the look says so on its choice («ما يناسب هذا الشكل», the plain line under its name), and when the
look changes to one a card does not suit, it is swapped for the nearest of its own kind (never across kinds), with one short toast; the person's own
cards come back when the look suits them again; a card the person picks themselves is theirs (its note, never a swap). CardFit runs on a JVM; the
wiring in Prefs and the settings is read from the source.

    python3 tests/card_fit_test.py      (needs javac)"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


if not shutil.which("javac"):
    print("SKIP: no javac here"); sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "CardFit.java"), str(ROOT / "tests/CardFitRun.java")], check=True, capture_output=True)
    rows = [l.split("\t") for l in subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.CardFitRun"], check=True,
                                                    capture_output=True).stdout.decode("utf-8").splitlines()]
suits = {r[1]: r[2:] for r in rows if r[0] == "suits"}
check("the strip («couch» layout) takes the compact cards only (the next prayer, the prayers in a row, the hours, the verse); the classic layout takes them all",
      all(suits[c][0] == "true" for c in ("nextprayer", "prayerrow", "hourly", "verse", "none")) and all(suits[c][0] == "false" for c in ("stocks", "prayers", "forecast", "adhkar", "matches", "tasks", "shows"))
      and all(v[1] == "true" for v in suits.values()), suits)
check("an unsuited card's note is «ما يناسب هذا الشكل» (never «كنبة»), a suited one has none",
      suits["stocks"][2] == "ما يناسب هذا الشكل" and len(suits["verse"]) < 3 or suits["verse"][2] == "", suits)
fit = {r[1]: r[2:] for r in rows if r[0] == "fit"}
check("the swap is within a kind: prayers → the prayers in a row, the coming days' weather → the hours, the dhikr → the verse; stocks and matches have no "
      "compact card of their kind: they stay (with the note), never a card of another kind",
      fit["stocks,prayers,forecast"][:2] == ["stocks,prayerrow,hourly", "2"] and fit["adhkar,verse,hourly"][0] == "adhkar,verse,hourly"
      and fit["matches"][:2] == ["matches", "0"], fit)
check("a nearest card already shown is not doubled (prayers beside the prayers in a row: the prayers stay)", fit["prayers,prayerrow"][:2] == ["prayers,prayerrow", "0"], fit)
check("cards that suit: nothing swapped, no toast; the classic layout swaps nothing", fit["prayerrow,hourly"][:2] == ["prayerrow,hourly", "0"]
      and fit["prayerrow,hourly"][3] == "null" and all(v[2] == k for k, v in fit.items()), fit)
check("one short toast names what was swapped for what and why", fit["stocks,prayers,forecast"][3] == "بدّلنا «مواقيت الصلاة اليوم» بـ«الصلوات في صف» و«طقس الأيام القادمة» بـ«طقس الساعات القادمة» لأنها ما تناسب هذا الشكل",
      fit["stocks,prayers,forecast"][3])
# ---- 4.4 beta 2 (the owner approved docs/mockups/full-look §٧): one table, every card x look x background decided
import re  # noqa: E402
table = {r[1]: r[2:] for r in rows if r[0] == "row"}
looks, bgs = ["classic", "football", "couch", "tvos"], ["photos", "scenes"]
panels = re.findall(r'\{"(\w+)", "[^"]+"\}', (APP / "Prefs.java").read_text(encoding="utf-8").split("static final String[][] PANELS = {")[1].split("};")[0])
check("every home card of Prefs.PANELS (but «none») is a row of CardFit.TABLE, once", set(panels) - {"none"} <= set(table) and len(table) == len([r for r in rows if r[0] == "row"]),
      sorted(set(panels) - {"none"} - set(table)))
cells = {k: dict(c.split("=", 1) for c in v[1:]) for k, v in table.items()}
check("every row has one decision for each of the 4 looks x 2 backgrounds (8), each a known one",
      all(v[0] == "8" for v in table.values()) and all(set(c) == {f"{l}/{b}" for l in looks for b in bgs} for c in cells.values())
      and all(d.split("~")[0] in ("", "wide", "row4", "picks", "no") for c in cells.values() for d in c.values()), table)
check("a card that does not fit always has its one-line reason (never a silent no), and a «fits, but» says how",
      all(d.split("~")[1] for c in cells.values() for d in c.values() if d.split("~")[0] != ""), [k for k, c in cells.items() for d in c.values() if d.split("~")[0] and not d.split("~")[1]])
compact = set(next(r for r in rows if r[0] == "compact")[1].split(","))
check("the «كنبة» column is today's strip rule (CardFit.COMPACT): nothing a TV showed before changes", {k for k, c in cells.items() if c["couch/photos"].split("~")[0] != "no"} == compact & set(cells), sorted(compact ^ {k for k, c in cells.items() if c["couch/photos"].split("~")[0] != "no"}))
check("the classic and football layouts take every card, on both backgrounds (as before)", all(c[f"{l}/{b}"] == "~" for c in cells.values() for l in ("classic", "football") for b in bgs))
check("the tvOS column as agreed: verse and hadith two cards wide, the coming days a row of four, the smart card picks fitting cards, the long lists do not fit",
      cells["verse"]["tvos/photos"] == cells["hadith"]["tvos/scenes"] == "wide~بعرض بطاقتين" and cells["forecast"]["tvos/photos"] == "row4~صف من ٤ أيام"
      and cells["smart"]["tvos/photos"].startswith("picks~") and all(cells[k]["tvos/photos"] == "no~ما تناسب tvOS: أطول من بطاقة tvOS" for k in ("prayers", "matches", "minitable", "stocks", "tasks", "shows", "adhkar"))
      and all(cells[k]["tvos/photos"] == "~" for k in ("nextprayer", "nextmatch", "livenow", "formula", "national", "today", "album", "year", "wisdom")))
check("the background decides nothing yet: each card's two background columns are the same, on every look", all(c[f"{l}/photos"] == c[f"{l}/scenes"] for c in cells.values() for l in looks))
check("an unknown look or card keeps today's behaviour (fits), «none» always fits", next(r for r in rows if r[0] == "unknown")[1:] == ["", "", ""])
import importlib.util  # noqa: E402
spec = importlib.util.spec_from_file_location("matrix", ROOT / "docs/mockups/full-look/src/matrix.py")
mx = importlib.util.module_from_spec(spec); spec.loader.exec_module(mx)
diff = [(k, l, b) for k, _ in mx.CARDS for l in looks for b in bgs if mx.fit(k, l, b)[0] != (cells[k][f"{l}/{b}"].split("~")[0] != "no")]
check("the owner's picture of the table (docs/mockups/full-look/src/matrix.py, 08-matrix.png) says what the app decides, every pair", not diff, diff)

prefs, st = (APP / "Prefs.java").read_text(encoding="utf-8"), (APP / "SettingsActivity.java").read_text(encoding="utf-8")
check("Prefs: the swap when the layout or the preset changes; the person's own cards kept (homePanelMine) and put back when nothing needs swapping",
      prefs.count("fitHomeCards();") >= 2 and 'putString("homePanelMine"' in prefs and 'setHomePanels(mine); sp.edit().remove("homePanelMine")' in prefs)
check("a card the person picks is theirs: their pick clears the kept list (no swap of it later; the note says it does not suit)",
      'sp.edit().remove("homePanelMine").apply();   // the person\'s own pick' in prefs)
check("the settings (4.4 beta 2, the owner): a home card that does not fit the look and background is shown off with its reason (CardFit.why), never applied; the swap said once after a look change",
      "String why = CardFit.why(values[i], cardLook, bg);" in st and "off[i] = !CardFit.fits(values[i], cardLook, bg) && !values[i].equals(current);" in st
      and "if (off != null && off[pos]) {" in st and "n.setAlpha(0.5f); a.setTextColor(OFF_REASON);" in st and "String fitNote = prefs.takeFitNote();" in st
      and 'String cardLook() { return "tvos".equals(sp.getString("preset", "")) ? "tvos" : homeLayout(); }' in prefs)
note = st[st.index("    private void notice(String s) {"):].split("\n    }\n")[0]
check("the swap's note is the app's own notice, not the system Toast (a capsule on some TVs: the owner's no-pills rule): bottom-centre, a small-radius rectangle, gone after 4 s, no key",
      "if (fitNote != null) notice(fitNote);" in st and "toast(fitNote)" not in st and "Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL" in note
      and "bg.setCornerRadius(dp(8));" in note and "postDelayed(hideNotice, 4000)" in note and "setFocusable(false)" in note)
sys.exit(1 if failed else 0)
