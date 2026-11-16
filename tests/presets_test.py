#!/usr/bin/env python3
"""4.4 «أشكال جاهزة» (the owner's names, docs/mockups/presets): six looks that set the home and the screensaver at once, the person's own values kept
once and put back by «شكلك المعتاد», «مخصّص» once a row is changed after; the two open questions behind flags (on).   python3 tests/presets_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


prefs, st, hub, recv = ((APP / f).read_text(encoding="utf-8") for f in ("Prefs.java", "SettingsActivity.java", "SettingsHub.java", "SaverReceiver.java"))
saver = (ROOT / "optimize/saver.sh").read_text(encoding="utf-8")
arr = lambda name: re.findall(r'"([^"]*)"', prefs[prefs.index(f"static final String[] {name} = {{"):].split("};")[0].split("{", 1)[1])
keys, names, fields = arr("PRESETS"), arr("PRESET_NAMES"), arr("PRESET_FIELDS")
check("the six looks with the owner's names, in his order", keys == ["tv", "couch", "clean", "family", "sport", "calm"]
      and names == ["شكلك المعتاد", "منظر وساعة", "صورة فقط", "مواقيت يومي", "مباريات اليوم", "ليل هادئ"], (keys, names))
vals = prefs[prefs.index("static final String[][] PRESET_VALUES = {"):].split("};")[0]
rows = re.findall(r"\n        \{(.*?)\}", vals)
check("each look gives every field a value or null (the person's own)", len(rows) == 6 and all(len(re.split(r",\s(?![^()]*\))", r)) == len(fields) for r in rows),
      [len(re.split(r",\s(?![^()]*\))", r)) for r in rows])
check("«منظر وساعة» is the Couch look with the apps as icons, «شكلك المعتاد» the coloured tiles (when nothing of the person's was kept)",
      rows[1].startswith('"couch", "scenes", "glass", "icons"') and rows[0].startswith('"classic", "photos", "plain", "tiles"'), rows[:2])
check("«مباريات اليوم» is the football layout with the new sports cards", '"football", null, "plain", null, "livenow,nextmatches,minitable"' in rows[4]
      and all(f'{{"{c}", ' in prefs for c in ("livenow", "nextmatches", "minitable")), rows[4])
check("the two open questions are flags, both on: it replaces «شكل كنبة»; «صورة فقط» hides the round buttons",
      "static final boolean PRESETS_REPLACE_COUCH = true;" in prefs and "static final boolean CLEAN_HIDES_BUTTONS = true;" in prefs
      and 'CLEAN_HIDES_BUTTONS ? "hidden" : null' in rows[2] and 'if (!Prefs.PRESETS_REPLACE_COUCH) row("شكل كنبة"' in st)
sp = prefs[prefs.index("    void setPreset(String key) {"):].split("\n    }\n")[0]
check("the person's own values kept once (the first look chosen), put back before each look, and exactly by «شكلك المعتاد»",
      'if (before == null) { before = presetNow(); sp.edit().putString("presetBefore", before).apply(); }' in sp and "else presetPut(before);" in sp
      and 'if (before != null) presetPut(before);' in sp and '.remove("preset").remove("presetBefore").remove("presetApplied")' in sp)
check("a row changed after reads «مخصّص» (what the look set is kept, compared row by row); the city, the adhan, the teams and the locks are not fields",
      '.putString("presetApplied", presetNow())' in sp and "String.format(\"مخصّص (%s، وتغيّر: %s)\"" in prefs
      and not any(f in fields for f in ("city", "adhan", "teams", "pin", "lock")))
check("a TV that had «شكل كنبة» on reads «منظر وساعة», its four values kept as the person's own", 'if (!sp.contains("preset") && sp.getBoolean("couchOn", false))' in prefs
      and 'putString("preset", "couch").putString("presetBefore", before)' in prefs and 'if (PRESETS_REPLACE_COUCH) return "couch".equals(preset());' in prefs)
check("the row is first in «المظهر» in both layouts, the picture picker opens from it, the settings open again in the other layout when it changes",
      'row("أشكال جاهزة", prefs.presetLabel()' in st and "private void pickPreset()" in st and '.putExtra("want", "أشكال جاهزة")' in st
      and '{"look", "أشكال جاهزة", "شكل كنبة",' in hub and '{"أشكال جاهزة", LOOK}, {"شكل كنبة", LOOK}' in hub and '{"أشكال جاهزة", "look"}' in hub)
pics = [ROOT / f"android/app/src/main/assets/presets/{k}.jpg" for k in keys]
check("each look's picture is in the app, small (under 40 KB)", all(p.is_file() and p.stat().st_size < 40 * 1024 for p in pics), [p.name for p in pics if not p.is_file()])
check("saver.sh preset tv|couch|clean|family|sport|calm; show says «preset: family» or «family (custom)»; given before any one row",
      'preset)     set1 preset "${2:?usage: optimize/saver.sh preset tv|couch|clean|family|sport|calm}" ;;' in saver and '"\\npreset: " + p.presetShow()' in recv
      and recv.index("if (preset != null) p.setPreset(preset);") < recv.index("if (homeLayout != null) p.setHomeLayout(homeLayout);"))
check("docs/SETTINGS.md has the row and its command", "`optimize/saver.sh preset family`" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
