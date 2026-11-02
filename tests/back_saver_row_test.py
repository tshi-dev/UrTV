#!/usr/bin/env python3
"""«الرجوع يفتح شاشة التوقف» on the TV: a row in the screensaver's settings, «الشاشة الرئيسية», right after «زر المهام», «مفعّل»/«متوقف»,
off by default. It reaches the home the way the other home rows do (the screensaver's ConfigProvider fragment, read by ConfigSnapshot), and
there it sets the home's own switch, the one `dock.sh backsaver` sets (Dock.back_saver), only when the row changed (HomeBack.rowChanged, run in
tests/HomeBackRun.java by tests/home_tile_test.py). The Back rule itself: tests/back_saver_test.py.   Run: python3 tests/back_saver_row_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
text = lambda p: p.read_text(encoding="utf-8")

prefs, receiver, st, hub = (text(APP / f) for f in ("Prefs.java", "SaverReceiver.java", "SettingsActivity.java", "SettingsHub.java"))
snap, dock, main, hb, recv = (text(HOME / f) for f in ("ConfigSnapshot.java", "Dock.java", "MainActivity.java", "HomeBack.java", "DockReceiver.java"))
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]
row = render[render.index('row("الرجوع يفتح شاشة التوقف"'):].split("});")[0]

check("the screensaver keeps the row, off by default", 'boolean backSaver() { return sp.getBoolean("backSaver", false); }' in prefs
      and 'void setBackSaver(boolean v) { sp.edit().putBoolean("backSaver", v).apply(); }' in prefs)
check("the fragment says it only while on (#backsaver=1), so nothing changes for anyone else", '(backSaver() ? "&backsaver=1" : "")' in prefs)
check("the row is in «الشاشة الرئيسية», right after «زر المهام», before «جرس التحديثات»",
      render.index('header("الشاشة الرئيسية (تلفزيونك)")') < render.index('row("زر المهام"') < render.index('row("الرجوع يفتح شاشة التوقف"')
      < render.index('row("جرس التحديثات"') < render.index('header("جديد المسلسلات")'))
check("one plain label, «مفعّل» or «متوقف», and OK flips it",
      'row("الرجوع يفتح شاشة التوقف", prefs.backSaver() ? "مفعّل" : "متوقف", R.drawable.ic_eye,' in row
      and "prefs.setBackSaver(!prefs.backSaver()); render();" in row)
check("the settings hub files it under «الشاشة الرئيسية»", '{"الرجوع يفتح شاشة التوقف", HOME}' in hub)
check("the receiver takes backsaver on or off and refuses the rest before saving; `saver.sh show` says it",
      '{"backsaver", backSaver}' in receiver and receiver.index('{"backsaver", backSaver}') < receiver.index('p.setBackSaver("on".equals(backSaver));')
      and '"\\nbacksaver: " + (p.backSaver() ? "on" : "off")' in receiver)

check("home: a real answer only (an old copy or no screensaver says null: nothing moves on a guess)",
      'Boolean backSaver() { return stale || fragment == null ? null : (fragment.contains("&backsaver=1") || fragment.startsWith("backsaver=1")); }' in snap)
follow = dock[dock.index("static void followBackSaver(Context c, Boolean tv) {"):].split("\n    }\n")[0]
check("home: it sets the very switch `dock.sh backsaver` sets (back_saver), and remembers the row's value it took",
      'if (!HomeBack.rowChanged(p.getBoolean("back_saver_tv", false), tv)) return;' in follow
      and 'p.edit().putBoolean("back_saver", tv).putBoolean("back_saver_tv", tv).apply();' in follow
      and 'putBoolean("back_saver", on)' in dock and "Dock.setBackSaver(c, m.equals(\"on\"))" in recv)
check("home: only on a change of the row", "return row != null && row != taken;" in hb)
check("home: the minute tick (and every return) takes it, beside the other home rows",
      "applyTasksPill(cfg.tasksPillOn());\n        Dock.followBackSaver(this, cfg.backSaver());" in main and main.count("Dock.followBackSaver(") == 1)

docs = text(ROOT / "docs/FEATURES.md") + text(ROOT / "docs/SETTINGS.md")
check("FEATURES.md and SETTINGS.md say where the row is", "الإعدادات ← الشاشة الرئيسية ← **الرجوع يفتح شاشة التوقف**" in docs
      and "الشاشة الرئيسية; مفعّل/متوقف, off by default" in docs)
sys.exit(1 if failed else 0)
