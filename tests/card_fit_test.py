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
prefs, st = (APP / "Prefs.java").read_text(encoding="utf-8"), (APP / "SettingsActivity.java").read_text(encoding="utf-8")
check("Prefs: the swap when the layout or the preset changes; the person's own cards kept (homePanelMine) and put back when nothing needs swapping",
      prefs.count("fitHomeCards();") >= 2 and 'putString("homePanelMine"' in prefs and 'setHomePanels(mine); sp.edit().remove("homePanelMine")' in prefs)
check("a card the person picks is theirs: their pick clears the kept list (no swap of it later; the note says it does not suit)",
      'sp.edit().remove("homePanelMine").apply();   // the person\'s own pick' in prefs)
check("the settings: the note as the plain line under the card's name in the home cards' picker; the swap said once after it",
      "if (home && !CardFit.suits(values[i], prefs.homeLayout())) about[i] = CardFit.NOTE" in st and "String fitNote = prefs.takeFitNote();" in st)
note = st[st.index("    private void notice(String s) {"):].split("\n    }\n")[0]
check("the swap's note is the app's own notice, not the system Toast (a capsule on some TVs: the owner's no-pills rule): bottom-centre, a small-radius rectangle, gone after 4 s, no key",
      "if (fitNote != null) notice(fitNote);" in st and "toast(fitNote)" not in st and "Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL" in note
      and "bg.setCornerRadius(dp(8));" in note and "postDelayed(hideNotice, 4000)" in note and "setFocusable(false)" in note)
sys.exit(1 if failed else 0)
