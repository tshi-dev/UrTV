#!/usr/bin/env python3
"""«أكمل المشاهدة» on the home screen (the idea from talkies, MIT): one row right under the favourites from the TV's Watch Next list.
After the TV test of 3.145 the owner wanted only that row (the apps' own rows looked like ads, and the rows were «a mess»: gaps, a card
cut at the left, English titles, the clock and the photo gone). TvRowsPick runs for real here (tests/TvRowsRun.java); the reading,
the permission, the setting and the home's wiring are checked in the source. Needs javac.
Run: python3 tests/tv_rows_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
main, rows, pick, screen = read(HOME / "MainActivity.java"), read(HOME / "TvRows.java"), read(HOME / "TvRowsPick.java"), read(HOME / "WatchNextActivity.java")
manifest = read(ROOT / "android/launcher/src/main/AndroidManifest.xml")
settings = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
fn = lambda src, name: src[src.index(name):].split("\n    }\n")[0]

load = fn(main, "private void loadTvRows()")
# ---- only «أكمل المشاهدة» (the owner, after the TV test of 3.145)
check("only the TV's Watch Next list is read: no apps' rows, no chooser of rows (ChannelsActivity is gone)",
      "PreviewPrograms" not in rows and "TvContract.Channels" not in rows and "arrange" not in rows and "arrange" not in pick
      and not (HOME / "ChannelsActivity.java").exists() and "ChannelsActivity" not in manifest and "ChannelsActivity" not in settings)
check("the row's name is Arabic whatever the apps call it", "name.setText(TvRowsPick.WATCH_NEXT_TITLE);" in main and 'WATCH_NEXT_TITLE = "أكمل المشاهدة"' in pick)
show = fn(main, "private void showTvRows(List<TvRows.Row> rows)")
check("the row sits in the first screen, right under the favourites, inside the same band padding (not under it, below the clock's screen)",
      "bottom.addView(tvRowsBox, new LinearLayout.LayoutParams(-1, -2));" in main and "content.addView(tvRowsBox" not in main
      and main.index("bottom.addView(dockRow") < main.index("bottom.addView(tvRowsBox") and "tvRowsBox.setPadding(dp(20), 0, dp(20), 0);" in main
      and "dockRow.setPadding(dp(20), dp(22), dp(20), dp(22));" in main)
check("no card is cut: no horizontal scroll, at most as many cards as favourites, each a favourite's width (rowCell, the dock's own rule)",
      "HorizontalScrollView" not in main and "int n = TvRowsPick.cells(dockCards.size(), MIN_DOCK);" in show and "line.addView(box, rowCell(i, n, -2));" in show
      and "int shown = Math.min(n, r.items.size());" in show and "line.addView(new View(this), rowCell(i, n, 1));" in show)
check("tight: the name 14 dp under the favourites and 10 dp over its cards, one row only", "tvRowsBox.addView(name, llp(-2, -2, 0, dp(14), 0, dp(10)));" in show
      and "TvRows.Row row = new Row();" not in show)
card = fn(main, "private View tvCard(TvRows.Item it)")
check("on a card the clock and the photo stay: the page stays at the top, nothing dims, the episode's name is the line above the favourites",
      "glideTo(0);" in card and "fadeGlow(1f);" in card and "title.setText(it.meta.isEmpty() ? it.title : it.title" in card
      and "fadeGlow(0.35f)" not in card and "title.animate().alpha(1f)" in card and "scrollToTvRow" not in main)
check("a card grows as a favourite does (1.12, the same spring)", "scaleTo(v, has ? 1.12f : 1f);" in card)
wire = fn(main, "private void wireFocus()")
check("▼ from the favourites goes to the row; ▲ from the first apps row comes from it; ▲ from it to the favourites",
      "c.setNextFocusDownId(!tvCards.isEmpty() ? tvCards.get(0).get(" in wire and "lastTv != null ? lastTv.get(Math.min(col, lastTv.size() - 1)).getId()" in wire
      and ": d > 0 ? dockCards.get(Math.min(i, d - 1)).getId() : topId);" in wire)
check("the card's picture: its title on a plain card until it comes; how far in as a gold line from the right",
      "tvPics.put(it, img);" in card and "plain.setText(it.title);" in card and "Gravity.BOTTOM | Gravity.RIGHT" in card and "it.progress" in card)

# ---- the setting: on/off
prefs_s, provider, recv, snap = (read(ROOT / "android/app/src/main/java/sa/urtv/screensaver" / n) for n in ("Prefs.java", "ConfigProvider.java", "SaverReceiver.java")), None, None, None
prefs_s = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java")
provider = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")
recv = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java")
snap = read(HOME / "ConfigSnapshot.java")
saver = read(ROOT / "optimize/saver.sh")
check("the setting: on by default, in the provider after the other columns, --es watchnext on|off, saver.sh watchnext, show",
      'boolean watchNext() { return sp.getBoolean("watchNext", true); }' in prefs_s and '"update_bell", "watch_next"});' in provider
      and 'p.watchNext() ? "1" : "0"});' in provider and 'i.getStringExtra("watchnext")' in recv and '"\\nwatchnext: "' in recv
      and 'watchnext) set1 watchnext' in saver)
check("the home reads it (an older screensaver says nothing: on) and shows no row when it is off", 'cur.getColumnIndex("watch_next")' in snap
      and "if (!TvRows.allowed(this) || !ConfigSnapshot.read(this).watchNextOn) {" in load)
row = settings[settings.index('row("أكمل المشاهدة"'):settings.index('render(); } });', settings.index('row("أكمل المشاهدة"'))]
check("the settings row: OK turns it on or off; on and not allowed yet, OK asks the one question (the home app's WatchNextActivity)",
      "prefs.setWatchNext(!prefs.watchNext());" in row and "if (prefs.watchNext() && !homeMayReadWatchNext()) { openWatchNextPermission(); return; }" in row
      and '"sa.urtv.home.WatchNextActivity"' in settings and 'checkPermission("android.permission.READ_TV_LISTINGS", "sa.urtv.home")' in settings)
check("in the settings menu, under «الشاشة الرئيسية»", '{"أكمل المشاهدة", HOME}' in read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java"))

# ---- permission and privacy
check("the launcher declares the TV listings permissions", '<uses-permission android:name="android.permission.READ_TV_LISTINGS" />' in manifest
      and '<uses-permission android:name="com.android.providers.tv.permission.READ_EPG_DATA" />' in manifest)
check("without the permission nothing is read and the home has no row, as before", "if (!allowed(c)) return out;" in fn(rows, "static List<Row> load(Context c)"))
check("the permission is asked only in WatchNextActivity, by the owner pressing «السماح» (never by the home on its own); allowed already, it closes",
      'card("السماح", v -> requestPermissions(new String[]{TvRows.PERMISSION, TvRows.EPG}, ASK))' in screen and "requestPermissions" not in main
      and "if (TvRows.allowed(this)) { finish(); return; }" in screen and 'android:name=".WatchNextActivity"' in manifest)
check("…behind «قفل الإعدادات» like «ترتيب المفضلة»", "ParentalPolicy.targets(ParentalPolicy.ARRANGE_SCREEN, null)" in screen and "Parental.guard(this" in screen)
check("a card opens only into the app that published it (TvRowsPick.linkAllowed), a plain link pinned to it (setPackage); else that app itself",
      "if (TvRowsPick.linkAllowed(it.pkg, i.getPackage(), comp)) {" in rows and "i.setSelector(null);" in rows and "if (comp == null) i.setPackage(it.pkg);" in rows
      and "getLeanbackLaunchIntentForPackage(it.pkg)" in fn(rows, "static boolean open(Context c, Item it)"))
check("a card goes through the app's own family lock first (ParentalPolicy.GRID, as the apps' grid)",
      "card.setOnClickListener(v -> guarded(ParentalPolicy.GRID, it.pkg, it.title, () -> TvRows.open(this, it)));" in main)
check("a picture is read bounded (Bounded.IMAGE_MAX), from http(s) with timeouts or the publishing app's own provider only (no android.resource, no file)",
      "Bounded.readAll(in, Bounded.IMAGE_MAX)" in rows and "ART_W = 400, ART_H = 400" in rows and "android.resource" not in rows.split("private static InputStream stream(")[1]
      and "resolveContentProvider(u.getAuthority(), 0)" in rows and "TvRowsPick.pictureProviderAllowed(it.pkg, p.packageName)" in rows
      and "h.setConnectTimeout((int) PICTURE_MS);" in rows and "h.setReadTimeout((int) PICTURE_MS);" in rows)
small = fn(rows, "private static Bitmap small(byte[] bytes, Bitmap.Config config)")
check("a picture is never decoded at full size: its size first (inJustDecodeBounds), then at a fraction (inSampleSize), then fitted to 400×400, the bigger one recycled",
      "size.inJustDecodeBounds = true;" in small and "o.inSampleSize = sample;" in small and "TvRowsPick.fitted(b.getWidth(), b.getHeight(), ART_W, ART_H)" in small
      and "if (s != b) b.recycle();" in small and small.index("inJustDecodeBounds") < small.index("o.inSampleSize"))
check("running out of memory on a picture costs that picture only (OutOfMemoryError caught where pictures are made and read)",
      "catch (Exception | OutOfMemoryError e)" in fn(rows, "private static File store(") and "catch (Exception | OutOfMemoryError e)" in fn(rows, "private static Bitmap decode(File f)")
      and "if (b != null) b.recycle();" in fn(rows, "private static File store("))
check("each picture has its own few seconds (a hung provider or server is left behind, a fresh thread for the next), all of them a budget",
      "f.get(ms, TimeUnit.MILLISECONDS)" in rows and "fetcher = Executors.newSingleThreadExecutor();" in rows and "PICTURE_MS = 4000" in rows
      and "long end = System.currentTimeMillis() + budgetMs;" in rows and "TV_PICTURES_MS = 8000" in main)
check("the row has its own thread (never the apps' loader); their words are shown first, the pictures follow; a newer load wins",
      "tvLoader.execute(() -> {" in load and "loader.execute" not in load and load.index("showTvRows(rows)") < load.index("TvRows.pictures(")
      and "if (gen == tvRowsGen && !isDestroyed()) {" in load and "tvLoader.shutdownNow();" in main and "artUri" in rows and "cached(" not in rows)
check("coming back to the home with the same row rebuilds nothing; a rebuild puts the focus back on the same card",
      "if (!sig.equals(tvShownSig)) { tvShownSig = sig; showTvRows(rows); }" in load
      and "tvCards.get(0).get(Math.min(hadAt, tvCards.get(0).size() - 1)).requestFocus();" in main)
check("the row is read again each time the home comes back to the front («أكمل المشاهدة» moves on)", "loadTvRows();   // what one watched since" in fn(main, "protected void onResume()"))
check("talkies is credited (THIRD_PARTY_NOTICES.md, and in the classes)", "Copyright (c) 2026 TechPP" in read(ROOT / "THIRD_PARTY_NOTICES.md")
      and all("talkies" in x and "THIRD_PARTY_NOTICES.md" in x for x in (rows, pick, screen)))
check("documented: SETTINGS.md, FEATURES.md and TEST-PLAN.md, and «صفوف القنوات» is gone from them",
      "saver.sh watchnext off" in read(ROOT / "docs/SETTINGS.md") and "**أكمل المشاهدة:**" in read(ROOT / "docs/FEATURES.md")
      and "No other rows" in read(ROOT / "docs/TEST-PLAN.md") and not any("صفوف القنوات" in read(ROOT / d) for d in ("docs/SETTINGS.md", "docs/FEATURES.md", "docs/TEST-PLAN.md")))

# ---- TvRowsPick, for real
if not shutil.which("javac"):
    print("SKIP tv rows: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TvRowsPick.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/TvRowsRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.TvRowsRun"], check=True, capture_output=True).stdout.decode("utf-8")
r = dict(l.split(" = ", 1) for l in out.split("\n") if " = " in l)
check("«أكمل المشاهدة» is called so", r["watchnext_title"] == "أكمل المشاهدة", r["watchnext_title"])
check("«أكمل المشاهدة»: what one is in the middle of, and the next episode; not a new season or a watchlist; not hidden, not of an app that is gone",
      (r["wn_continue"], r["wn_next"], r["wn_new"], r["wn_watchlist"], r["wn_hidden"], r["wn_no_app"]) == ("true", "true", "false", "false", "false", "false"))
check("the line under a card: «الموسم ٢ · الحلقة ١ · Netflix», digits as the setting says", r["meta_episode"] == "الموسم ٢ · الحلقة ١ · Netflix" and r["meta_latin"] == "الموسم 2 · الحلقة 10 · Netflix",
      (r["meta_episode"], r["meta_latin"]))
check("…else the episode's name, else the description; «null» from an app is nothing",
      r["meta_eptitle"] == "The Long Night" and r["meta_same_title"] == "desc here" and r["meta_nulls"] == "", (r["meta_eptitle"], r["meta_same_title"], r["meta_nulls"]))
check("a link opens only into its own app: by package or component, or naming none (pinned to the app by the caller); never another app",
      (r["link_package"], r["link_component"], r["link_other_component"], r["link_other_package"], r["link_mixed"], r["link_none"], r["link_no_row"])
      == ("true", "true", "false", "false", "false", "true", "false"))
check("a huge picture (8000×6000, 190 MB at full size) is decoded at 1/16 (500×375, under 1 MB) and fitted to 400×300",
      r["sample_huge"] == "16 500x375 732KB" and r["fit_huge"] == "400x300", (r["sample_huge"], r["fit_huge"]))
check("a very tall picture is bounded by its height too (1000×30000: 1/64, then 9×400)", r["sample_tall"] == "64 15x468" and r["fit_tall"] == "13x400", (r["sample_tall"], r["fit_tall"]))
check("a small picture is taken as it is; a picture with no size is none", r["sample_small"] == "1 320" and r["sample_none"] == "0 0", (r["sample_small"], r["sample_none"]))
check("a content:// picture only from the publishing app's own provider", (r["provider_own"], r["provider_other"], r["provider_none"]) == ("true", "false", "false"))
check("the rows' signature tells the same rows from changed ones", r["sig_same"] == "true" and r["sig_diff"] == "false", (r["sig_same"], r["sig_diff"]))
# ---- the TV test of 3.147: one huge card filled two thirds of the screen and pushed the favourites up over the clock
check("«أكمل المشاهدة» is never one cell: before the favourites are on screen it is cut as the least of them (5), then as they are",
      r["cells_before"] == "5" and r["cells_after"] == "7", (r["cells_before"], r["cells_after"]))
check("at 960x540 dp a card is a favourite tile's size: 148.8x83.7 dp with 5 favourites, 64.4x36.2 with 10 (one cell would be 824x463.5)",
      r["card_5"] == "148.8x83.7" and r["card_10"] == "64.4x36.2" and r["card_1"] == "824.0x463.5", (r["card_5"], r["card_10"], r["card_1"]))
check("…cut like the favourites (the dock band's 20 dp padding, rowCell) and built again when their number changes",
      "tvRowsBox.setPadding(dp(20), 0, dp(20), 0);" in main and "dockRow.setPadding(dp(20), dp(22), dp(20), dp(22));" in main
      and "if (!tvShown.isEmpty() && tvCells != dockCards.size()) showTvRows(tvShown);" in main)
# the first screen at 960x540 dp with the row (the tallest case, 5 favourites): estimated as the home lays it out (sp ~ dp x 1.2 for a line)
card5 = float(r["card_5"].split("x")[1])
header = 36 + (15 * 1.2 + 4) + (12 * 1.2 + 5) + (58 * 1.25 - 2) + (14 * 1.2 - 2)        # top, greeting, weather, clock, date
bottom = 30 + (11 * 1.2 + 24 + card5 * 1.12) + (22 + card5 * 1.12 + 22) + (34 * 1.2 + 14)  # margin, row (label, focused card), dock band, title
check(f"at 960x540 dp the favourites and the row stay under the clock: the clock block ends at ~{header:.0f} dp, the favourites' block starts at ~{540 - bottom:.0f} dp",
      540 - bottom > header + 8, (header, 540 - bottom))
check("how far in: a fraction up to 1, unknown without a position or a length",
      r["progress_half"] == "0.5" and r["progress_over"] == "1.0" and r["progress_unknown"] == "-1.0 -1.0", (r["progress_half"], r["progress_over"], r["progress_unknown"]))
sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TvRowsPick.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/TvRowsRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.TvRowsRun"], check=True, capture_output=True).stdout.decode("utf-8")
r = dict(l.split(" = ", 1) for l in out.split("\n") if " = " in l)
check("«أكمل المشاهدة» is called so", r["watchnext_title"] == "أكمل المشاهدة", r["watchnext_title"])
check("«أكمل المشاهدة»: what one is in the middle of, and the next episode; not a new season or a watchlist; not hidden, not of an app that is gone",
      (r["wn_continue"], r["wn_next"], r["wn_new"], r["wn_watchlist"], r["wn_hidden"], r["wn_no_app"]) == ("true", "true", "false", "false", "false", "false"))
check("the line under a card: «الموسم ٢ · الحلقة ١ · Netflix», digits as the setting says", r["meta_episode"] == "الموسم ٢ · الحلقة ١ · Netflix" and r["meta_latin"] == "الموسم 2 · الحلقة 10 · Netflix",
      (r["meta_episode"], r["meta_latin"]))
check("…else the episode's name, else the description; «null» from an app is nothing",
      r["meta_eptitle"] == "The Long Night" and r["meta_same_title"] == "desc here" and r["meta_nulls"] == "", (r["meta_eptitle"], r["meta_same_title"], r["meta_nulls"]))
check("a link opens only into its own app: by package or component, or naming none (pinned to the app by the caller); never another app",
      (r["link_package"], r["link_component"], r["link_other_component"], r["link_other_package"], r["link_mixed"], r["link_none"], r["link_no_row"])
      == ("true", "true", "false", "false", "false", "true", "false"))
check("a huge picture (8000×6000, 190 MB at full size) is decoded at 1/16 (500×375, under 1 MB) and fitted to 400×300",
      r["sample_huge"] == "16 500x375 732KB" and r["fit_huge"] == "400x300", (r["sample_huge"], r["fit_huge"]))
check("a very tall picture is bounded by its height too (1000×30000: 1/64, then 9×400)", r["sample_tall"] == "64 15x468" and r["fit_tall"] == "13x400", (r["sample_tall"], r["fit_tall"]))
check("a small picture is taken as it is; a picture with no size is none", r["sample_small"] == "1 320" and r["sample_none"] == "0 0", (r["sample_small"], r["sample_none"]))
check("a content:// picture only from the publishing app's own provider", (r["provider_own"], r["provider_other"], r["provider_none"]) == ("true", "false", "false"))
check("the rows' signature tells the same rows from changed ones", r["sig_same"] == "true" and r["sig_diff"] == "false", (r["sig_same"], r["sig_diff"]))
check("how far in: a fraction up to 1, unknown without a position or a length",
      r["progress_half"] == "0.5" and r["progress_over"] == "1.0" and r["progress_unknown"] == "-1.0 -1.0", (r["progress_half"], r["progress_over"], r["progress_unknown"]))
check("the apps' channels are asked only while a row of one is shown (every return to the home screen loads the rows): not with only «أكمل المشاهدة» or nothing shown",
      (r["needs_unchosen"], r["needs_only_wn"], r["needs_nothing"], r["needs_other"]) == ("true", "false", "false", "true"), (r["needs_unchosen"], r["needs_only_wn"], r["needs_nothing"], r["needs_other"]))
check("TvRows.load uses it (the channel queries are the cost), and the settings' list (arrange) still asks every channel",
      "TvRowsPick.needsChannels(p.getString(\"shown\", null)) ? found(c)" in (HOME / "TvRows.java").read_text() and "TvRowsPick.arrange(found(c)" in (HOME / "TvRows.java").read_text())
sys.exit(1 if failed else 0)
