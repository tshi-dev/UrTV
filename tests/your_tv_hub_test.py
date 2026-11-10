#!/usr/bin/env python3
"""«تلفزيونك» (YourTvHubActivity): what opening the app shows: four windows (the markets, the matches, the games, the settings) in two lines, the same page, grid and focus as the
settings' first screen. The pins: the launcher entry, the four windows and their marks, the games' «قريبًا» flag, the settings' door unchanged (the Settings key, the control center and the
home's button open SettingsActivity, not the hub), the grid's explicit focus ways in both directions, no «·» in what it says, its words in the table, and the market's hours on a JVM.
Run: python3 tests/your_tv_hub_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

hub = (APP / "YourTvHubActivity.java").read_text(encoding="utf-8")
man = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
words = (APP / "Words.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8")
home = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
res = ROOT / "android/app/src/main/res/drawable"

hub_block = man.split('android:name=".YourTvHubActivity"')[1].split("</activity>")[0]
check("opening the app shows the hub: it is the launcher (and the leanback launcher) entry; SettingsActivity no longer is, and stays exported for the home, the remote's key and the control center",
      "android.intent.category.LAUNCHER" in hub_block and "LEANBACK_LAUNCHER" in hub_block
      and "LAUNCHER" not in man.split('android:name=".SettingsActivity"')[1].split("/>")[0].split("</activity>")[0]
      and re.search(r'android:name="\.SettingsActivity"\s+android:exported="true"', man) is not None)
check("four windows in this order (from the start side): الأسواق, المباريات, الألعاب, الإعدادات, each with a mark and a colour",
      [w for w in re.findall(r'\{"(\w+)", "([^"]+)", "(\w+)", "(#[0-9A-F]{6})"\}', hub)] == [("markets", "الأسواق", "ic_hub_markets", "#14705A"), ("matches", "المباريات", "ic_hub_matches", "#1F5FBF"), ("games", "الألعاب", "ic_hub_games", "#7B3FC9"), ("settings", "الإعدادات", "ic_settings", "#5B6677")])
check("the marks are different silhouettes (bars, a ball, a die, a gear), solid, white, each a file of its own",
      all((res / f"{n}.xml").is_file() for n in ("ic_hub_markets", "ic_hub_matches", "ic_hub_games", "ic_settings"))
      and len({(res / f"{n}.xml").read_text() for n in ("ic_hub_markets", "ic_hub_matches", "ic_hub_games")}) == 3)
check("«الألعاب» opens the games list (GAMES_READY on: «الأسرع» for now, the others «قريبًا»), and the notice stays for a build with it off",
      "static final boolean GAMES_READY = true;" in hub and '"قريبًا"' in hub and "Toast.makeText(this, Words.of(\"قريبًا\")" in hub and "startActivity(new Intent(this, QuizGamesActivity.class))" in hub)
check("the markets and the matches open the home app's own pages (class names as the settings' rows use), the fourth window the settings",
      'setClassName("sa.urtv.home", "sa.urtv.home.MarketsActivity")' in hub and 'setClassName("sa.urtv.home", "sa.urtv.home.MatchesActivity")' in hub
      and "new Intent(this, SettingsActivity.class)" in hub)
check("no second hub: the home's settings paths still open SettingsActivity itself (not the hub)",
      'SAVER_PKG + ".SettingsActivity"' in home and "YourTvHub" not in home)
check("the same page as the settings: GlassSource.page, the 44 sp title «تلفزيونك», 8 dp inside, 5 % under the hint, 12 dp between the tiles, the glass tile's focus (GlassPaint.card 14)",
      "GlassSource.page(this, root)" in hub and "TypedValue.COMPLEX_UNIT_SP, 44" in hub and "page.setPadding(dp(8), dp(32), dp(8), dp(16))" in hub
      and "heightPixels * 0.05f" in hub and "GlassPaint.card(card, look, 14)" in hub and "setMarginEnd(dp(12))" in hub and "bottomMargin = dp(12)" in hub)
check("the grid walks by explicit focus ways in both directions (index 0 is the start side: right in Arabic, left in English), no wrapping",
      all(f in hub for f in ("c.setNextFocusUpId(", "c.setNextFocusDownId(", "c.setNextFocusLeftId(cards.get(rtl ? toEnd : toStart).getId());", "c.setNextFocusRightId(cards.get(rtl ? toStart : toEnd).getId());")))
check("every word it says is in the English table and in Words (the settings' own mechanism)",
      all(f'S.n("{w}")' in words for w in ("الألعاب", "قريبًا", "السوق السعودي مفتوح", "السوق السعودي مغلق", "اختر فرقك", "فرقك أولًا", "تعذّر الفتح"))
      and all(re.search(rf"(?m)^{w}\t", tsv) for w in ("الألعاب", "قريبًا", "السوق السعودي مفتوح", "السوق السعودي مغلق", "اختر فرقك", "فرقك أولًا", "تعذّر الفتح", "الأسواق", "المباريات", "الإعدادات", "تلفزيونك")))
check("no «·» between texts in what it shows", "·" not in re.sub(r"/\*.*?\*/|//[^\n]*", "", hub, flags=re.S))
nm = (APP / "NextMatch.java").read_text(encoding="utf-8")
check("«المباريات» shows the real next match of the followed teams (NextMatch, cached 30 min), the generic line stays when nothing is known",
      "nextMatchLine(v)" in hub and "NextMatch.cached(" in hub and "KEEP_MS = 30L * 60 * 1000" in nm and "TEAMS_MAX = 3" in nm
      and "if (!same || when <= now) return null;" in nm and 'status.matches(".*(postp|cancel|abandon|suspend).*")' in nm)
check("the next-match line has no «·»", "·" not in re.sub(r"/\*.*?\*/|//[^\n]*", "", (APP / "NextMatchTime.java").read_text(encoding="utf-8"), flags=re.S))
# ---- step 47 (a real TCL): the bottom row was cut by the hint; the language did not follow; the Settings key did nothing; the markets line named no market
check("the two lines share the height above the hint (weights, not 188 dp each); the hint is in words, with no separator sign",
      "page.addView(line, new LinearLayout.LayoutParams(-1, 0, 1f));" in hub and "line.setMinimumHeight(dp(120));" in hub and "LayoutParams(-1, dp(188))" not in hub
      and 'Words.of("الأسهم للتنقل، واختيار للفتح، ورجوع للخروج")' in hub and "•" not in hub.replace("// the 5 % safe area", ""))
def fits(h_px, density):   # the arithmetic of the page's boxes at one screen: where the hint starts, the title block, what each of the two lines gets, the smallest a card needs
    sp = density
    hint = 14 * sp * 1.3 + (10 + 8) * density + h_px * 0.05
    title = 32 * density + 44 * sp * 1.3 + 16 * density + 16 * density
    gaps = 2 * 12 * density
    each = (h_px - hint - title - gaps) / 2
    need = (20 + 20 + 96) * density   # a card: 20 dp above and below its 96 dp mark
    return each, need, h_px - hint
ok = True
for h, d in ((1080, 2.0), (720, 1.0), (720, 1.333), (1080, 1.0)):
    each, need, bottom = fits(h, d)
    ok = ok and each >= need and bottom <= h * 0.95 + 1
check("at 1920x1080 (density 2), 1280x720 (density 1 and 1.33) the two lines each get at least the 136 dp a card needs, and the last ends above the hint, inside the 5 % safe area: " + str([tuple(round(v) for v in fits(h, d)) for h, d in ((1080, 2.0), (720, 1.0), (720, 1.333))]), ok)
check("the hub is made again when «اللغة» or the look changes (the broadcast, and on return), and the remote's Settings key (176, 83, 4077) opens our settings there",
      "madeLang" in hub and "InternalReceivers.register(this, changed, new android.content.IntentFilter(Prefs.CHANGED))" in hub and "if (!madeLang.equals(prefs.lang())" in hub and "recreate();" in hub
      and "code == 176 || code == 83 || code == 4077" in hub and 'open("settings")' in hub)
check("«الأسواق» names the market it means (the Saudi one: Sunday to Thursday 10:00 to 15:00), as its line says", '"السوق السعودي مفتوح" : "السوق السعودي مغلق"' in hub)

if not shutil.which("javac"):
    print("SKIP market hours: no javac")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-d", tmp, str(APP / "MarketHours.java"), str(ROOT / "tests/MarketHoursRun.java")], check=True, capture_output=True)
    cases = [("2026-11-08T07:30:00Z", "open"),    # Sunday 10:30 in Riyadh
             ("2026-11-08T06:59:00Z", "closed"),  # 09:59
             ("2026-11-08T11:59:00Z", "open"),    # 14:59
             ("2026-11-08T12:00:00Z", "closed"),  # 15:00
             ("2026-11-12T08:00:00Z", "open"),    # Thursday 11:00
             ("2026-11-13T08:00:00Z", "closed"),  # Friday
             ("2026-11-14T08:00:00Z", "closed")]  # Saturday
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.MarketHoursRun"] + [c[0] for c in cases], check=True, capture_output=True, text=True).stdout.split()
    subprocess.run(["javac", "-nowarn", "-d", tmp, str(APP / "NextMatchTime.java"), str(ROOT / "tests/NextMatchTimeRun.java")], check=True, capture_output=True)
    def nmt(*a):
        return subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.NextMatchTimeRun", *a], check=True, capture_output=True, text=True, encoding="utf-8").stdout.strip()
    check("the next match's time is read as UTC and shown in the city's zone: today, tomorrow, a weekday; 12 or 24 h",
          nmt("2026-11-12T16:00:00", "2026-11-12T08:00:00Z", "Asia/Riyadh", "0", "1") == "Next: A vs B, today 7:00 PM"
          and nmt("2026-11-13T16:00:00", "2026-11-12T08:00:00Z", "Asia/Riyadh", "1", "1") == "Next: A vs B, tomorrow 19:00"
          and nmt("2026-11-16T16:00:00", "2026-11-12T08:00:00Z", "Asia/Riyadh", "0", "1") == "Next: A vs B, Monday 7:00 PM"
          and nmt("2026-11-12T23:30:00", "2026-11-12T08:00:00Z", "Asia/Riyadh", "1", "0").startswith("التالية: A ضد B، غدًا"))
    check("the market's state: Sunday to Thursday 10:00 to 15:00 in Riyadh, closed Friday and Saturday and outside the hours", out == [c[1] for c in cases], out)
sys.exit(1 if failed else 0)
