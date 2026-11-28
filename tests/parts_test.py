#!/usr/bin/env python3
"""4.4 «ابدأ بـ» and «أجزاء UrTV» (the owner, docs/mockups/start-presets/): a part switched off disappears from the home, the screensaver, the
settings and their search, and its own settings come back as they were when it is on. Parts and the home's card filter run for real
(tests/PartsRun.java, one part at a time); the gates in the apps and the page are read in the code. Run: python3 tests/parts_test.py"""
import re, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

parts, prefs, sa, hub, start = read(APP / "Parts.java"), read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "SettingsHub.java"), read(APP / "StartActivity.java")
main, card, choice, snap, center, greet = (read(HOME / f) for f in ("MainActivity.java", "HomeCard.java", "HomeCardChoice.java", "ConfigSnapshot.java", "ControlCenter.java", "Greeting.java"))
tpl, recv = read(ROOT / "src/template.html"), read(APP / "SaverReceiver.java")
ids = re.findall(r'\{(PRAYER|TILAWAH|MATCHES|MARKETS|WEATHER|CARDS|SAVER|GAMES|TASKS|DUAS), "', parts)
check("ten parts, each with its name and a line of at most 8 words", len(ids) == 10
      and all(len(l.split()) <= 8 for l in re.findall(r'\{(?:PRAYER|TILAWAH|MATCHES|MARKETS|WEATHER|CARDS|SAVER|GAMES|TASKS|DUAS), "[^"]+", "([^"]+)"\}', parts)))
check("Parts has no Android in it (it runs in the test as it is)", "import android" not in parts)

# ---- each part, for real
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp, "src/android/content"); stub.mkdir(parents=True)
    (stub / "Context.java").write_text("package android.content; public abstract class Context { public static final int MODE_PRIVATE = 0; "
                                       "public abstract SharedPreferences getSharedPreferences(String n, int m); }")
    (stub / "SharedPreferences.java").write_text("package android.content; public interface SharedPreferences { String getString(String k, String d); }")
    out = Path(tmp, "out")
    srcs = [str(APP / "Parts.java"), str(HOME / "HomeCardChoice.java"), str(HOME / "PartsHome.java"), str(stub / "Context.java"),
            str(stub / "SharedPreferences.java"), str(ROOT / "tests/PartsRun.java")]
    r = subprocess.run(["javac", "-d", str(out)] + srcs, capture_output=True, text=True)
    check("Parts, the home's card filter and the run compile", r.returncode == 0, r.stderr[-800:])
    run = subprocess.run(["java", "-cp", str(out), "PartsRun"], capture_output=True, text=True) if r.returncode == 0 else None
    lines = run.stdout.splitlines() if run else []
facts = {l.split()[1]: dict(kv.split("=") for kv in l.split()[2:]) for l in lines if l.startswith("part ")}
for pid in ("prayer", "tilawah", "matches", "markets", "weather", "cards", "saver", "games", "tasks", "duas"):
    f = facts.get(pid, {})
    check(f"«{pid}» off: all its settings rows go ({f.get('rows')}), no other part's, and the search finds none of them",
          f and f["hidden"] == f["rows"] and f["othersHidden"] == "0" and f["search"] == "true", f)
    check(f"«{pid}» off: its cards leave the home ({'all of them' if pid == 'cards' else 'the others stay'}), a card of no part stays unless «البطاقات» is off",
          f and f["cardsGone"] == "true" and f["othersKept"] == "true" and f["verse"] == ("false" if pid == "cards" else "true"), f)
    check(f"«{pid}» on again: the list is empty again and every card is back (its own settings were never touched)", f and f["back"] == "true" and f["cardsOn"] == "true", f)
check("two parts off at once are kept in the table's order; an unknown key changes nothing", "two markets,games" in lines and "unknown markets" in lines, lines[-3:])
check("the rows of the parts: every row named is a real settings row (SettingsHub.ROWS)",
      all(f'{{"{r}", ' in hub for r in re.findall(r'\{"([^"]+)", (?:PRAYER|TILAWAH|MATCHES|MARKETS|WEATHER|CARDS|SAVER|GAMES|TASKS|DUAS)\}', parts)))

# ---- their data: off and on never touch anything but «partsOff»
setp = prefs[prefs.index("void setPartOff("):prefs.index("void setPartsOff(")]
check("a part off or on writes «partsOff» only (its own settings stay, so on brings it back as it was)",
      setp.count("sp.edit()") == 1 and 'putString("partsOff", Parts.with(partsOff(), id, off))' in setp and ".remove(" not in setp)
check("the default is every part on, so an update changes nothing", 'String partsOff() { return Parts.clean(sp.getString("partsOff", "")); }' in prefs)

# ---- the gates
check("the settings: a part's rows are not built (nor run by the search), its search results are left out, «شاشة التوقف» takes its section",
      "if (Parts.hidesRow(prefs.partsOff(), FocusKey.key(label))) return offscreen();" in sa and sa.count("Parts.visible(SettingsHub.search(") == 3
      and "private boolean sectionGone(String cat) { return SettingsHub.SAVER.equals(cat) && prefs.partOff(Parts.SAVER); }" in sa)
check("the pages (the home's cards and the screensaver) are told «&off=» only when a part is off", '(partsOff().isEmpty() ? "" : "&off=" + partsOff())' in prefs)
check("the home's cards and the football layout follow the parts", "if (panel != null) panel = HomeCardChoice.withoutParts(panel, partsFrag);" in card
      and 'if ("football".equals(layout) && PartsHome.offIn(partsFrag, "matches")) layout = "classic";' in card)
check("the home's buttons: تلاوة, the tasks, the markets and the matches go with their parts; the adhan bar with the prayers",
      'if (PartsHome.offIn(fragment, "tilawah")) listenPill = false;' in snap and '(tasksPill() && partOff(Parts.TASKS) ? "&taskspill=0" : "")' in prefs and 'if (PartsHome.offIn(fragment, "prayer")) bar = false;' in snap
      and 'if (cfg.partOff("markets")) hiddenQuick.add("markets");' in main and 'if (cfg.partOff("matches")) hiddenQuick.add("matches");' in main)
check("the home: «يُتلى الآن» with تلاوة, the weather line with the weather, the control centre's games, screensaver and adhan tiles",
      'if (ConfigSnapshot.ui(this).partOff("tilawah")) { nowCard.setVisibility(View.GONE); return; }' in main and 'if (snap.partOff("weather")) {' in greet
      and 'String[][] partTiles = {{"games", "games"}, {"saver", "saver"}, {"adhan", "prayer"}};' in center)
check("the owner: «المواقيت والأذان» off silences the adhan and the iqama (the next alarm is still set), and its rows say so",
      'sa.urtv.home.PartsHome.off(context, "prayer") ? "the prayer part is off"' in read(HOME / "tilawah/AdhanReceiver.java")
      and 'PartsHome.off(app, "prayer")' in read(HOME / "tilawah/IqamaReceiver.java") and "(إيقافها يوقف الأذان)" in sa and "والأذان ما يشتغل" in sa)
check("the duas and the tasks: no card while their part is off, the next alarm still set",
      '|| sa.urtv.home.PartsHome.off(c, "duas")) { DuaScheduler.schedule(c); return; }' in read(HOME / "tilawah/DuaReceiver.java")
      and 'if (PartsHome.off(c, "tasks")) { ReminderScheduler.schedule(c); return; }' in read(HOME / "ReminderReceiver.java"))
hand = read(HOME / "setup/SaverHandoff.java")
check("the owner: «شاشة التوقف» off gives the TV its own screensaver (the one the setup kept); on gives ours back only after our own hand-off",
      'Settings.Secure.putString(r, "screensaver_components", theirs);' in hand and 'if (!sp.getBoolean("handedOff", false)) return "kept";' in hand
      and "catch (SecurityException e)" in hand and 'saverPart(cfg.partOff("saver"));' in main)
check("the screensaver page leaves out what is off (its cards, the bars, the weather, the slot, the next prayer's panel)",
      "const PARTS_OFF = new Set((_Q.get('off') || '').split(',').filter(Boolean));" in tpl and "CONFIG.cards = CONFIG.cards.filter(k => !partGone(k));" in tpl
      and "html.off-prayer .p-prayer{display:none}" in tpl and "if (PARTS_OFF.has('weather')) { CONFIG.wxAt = 'off';" in tpl)

# ---- «ابدأ بـ»
check("the starts: «نظيف», «كامل», «كنبة» first, then «المزيد»; «خلّه مثل ما هو» changes nothing",
      'static final String[] FIRST = {"clean", "full", "couch"};' in parts and '{"same", "خلّه مثل ما هو", "ما يتغيّر شي", "", null},' in parts
      and 'new String[] {"clean", "full", "couch", "more"}' in start and "if (st[4] == null) return;   // «خلّه مثل ما هو»" in prefs)
check("the owner: «نظيف» is the clock and the apps; «كنبة» no cards and no clubs; «بسيط» a big clock with five apps at least (the bar's smallest)",
      '{"clean", "نظيف", "الساعة والتطبيقات، وبس", "clean", "cards,matches,markets,tilawah,prayer,saver,games,tasks,duas"}' in parts
      and '{"couch", "كنبة", "ساعة كبيرة في الوسط", "couch", "cards,matches"}' in parts and 'static final String[] FAV_BARS = {"5",' in prefs)
check("once by itself: a new TV with the focus on «نظيف», an updated one as a welcome on «خلّه مثل ما هو»; Back is «خلّه مثل ما هو»; never in kids mode",
      'putExtra("welcome", !fresh)' in main and "if (startAsked || cfg.startSeen() || !resumed || cleanShown || Parental.state(this).kids) return;" in main
      and 'choose("same");   // Back' in start and '(startSeen() ? "" : "&ask=1")' in prefs)
check("focus: tiles grow with a shadow; rows and buttons take the look's fill (docs/ui/SIMPLE.md), no ring",
      "scaleX(has ? 1.06f : 1f)" in start and "bg.setColor(has ? look.focus : 0x14FFFFFF)" in start and "setStroke" not in start)
check("the agent's words: saver.sh start KEY and part ID off|on, said by «show»", 'set1 start "${2:?' in read(ROOT / "optimize/saver.sh")
      and 'String start = i.getStringExtra("start"), part = i.getStringExtra("part");' in recv and '"\\nparts off: "' in recv)
check("no setup check ever meets the chooser: install.sh answers «خلّه مثل ما هو» before its home check when nothing was said; the CI and the round do too",
      "saver.sh start same" in read(ROOT / "optimize/install.sh") and "saver --es start same" in read(ROOT / ".github/screens/shoot.sh")
      and "saver.sh start same" in read(ROOT / "tester/auto/ci/run.sh") and "start: not asked yet" in read(ROOT / "tester/auto/round.sh"))
check("the start pictures are small JPGs in the app (under 40 KB each)", all((ROOT / f"android/app/src/main/assets/starts/{k}.jpg").stat().st_size < 40000
      for k in ("clean", "full", "couch", "more", "prayer", "sport", "kids", "simple")))
raise SystemExit(1 if failed else 0)
