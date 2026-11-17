#!/usr/bin/env python3
"""4.4 «أشكال جاهزة» (the owner's names, docs/mockups/presets): six looks that set the home and the screensaver at once, the person's own values kept
once and put back by «شكلك المعتاد», «مخصّص» once a row is changed after; the two open questions behind flags (on).   python3 tests/presets_test.py"""
import re, shutil, subprocess, sys, tempfile
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
hid = re.search(r'BUTTONS_HIDDEN = "([^"]*)";', prefs).group(1)
check("«صورة فقط» (the owner): the sources, the tasks, the recitation, the markets, the games, the phone and the search hidden; «مركز التحكم» and «تحديث جديد» stay",
      sorted(hid.split("|")[0].split(",")) == ["inputs", "markets", "matches", "phone"] and hid.split("|")[1:] == ["false", "false", "false"]
      and '"inputs"' in prefs[prefs.index("QUICK_BUTTONS = {"):].split("}")[0], hid)
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
      'preset)     set1 preset "${2:?usage: optimize/saver.sh preset tv|couch|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass}" ;;' in saver and '"\\npreset: " + p.presetShow()' in recv
      and recv.index("if (preset != null) p.setPreset(preset);") < recv.index("if (homeLayout != null) p.setHomeLayout(homeLayout);"))
check("docs/SETTINGS.md has the row and its command", "`optimize/saver.sh preset family`" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))

# ---- «تركيبات مقترحة» (the owner approved docs/mockups/presets/combos-*.jpg): one tile, four mixes, the circle from the tile, a fade on a weak TV
combos = re.findall(r'\{"([a-z-]+)", "([^"]+)", "([^"]+)", "([a-z]+)"((?:, "[a-zA-Z]+=[^"]+")+)\}', prefs[prefs.index("static final String[][] COMBOS = {"):].split("};")[0])
check("four mixes, each one of the six and a row or two, with the owner's names", [c[0] for c in combos] == ["couch-prayers", "clean-icons", "sport-night", "tv-glass"]
      and [c[1] for c in combos] == ["منظر وساعة + مواقيت اليوم", "صورة فقط + أيقونات التطبيقات", "مباريات اليوم + ليل هادئ", "شكلك المعتاد + الساعة الزجاجية"]
      and all(c[3] in keys for c in combos) and all(re.findall(r'"([a-zA-Z]+)=', c[4]) and set(re.findall(r'"([a-zA-Z]+)=', c[4])) <= set(fields) for c in combos), combos)
sc = prefs[prefs.index("    void setCombo(String key) {"):].split("\n    }\n")[0]
check("a mix: its look first (the person's own kept the same way), then its rows, and it reads as itself until a row is changed after",
      "setPreset(c[3]);" in sc and '.putString("presetCombo", key).putString("presetApplied", presetNow())' in sc
      and 'String name = mix.isEmpty() ? presetName(key) : combo(mix)[1];' in prefs and 'sp.edit().remove("presetCombo").apply();' in prefs
      and 'if (combo(key) != null) { setCombo(key); return; }' in prefs)
pick = st[st.index("    private void pickPreset() {"):st.index("    /** a mix chosen: applied as a look is")]
check("the picker: the six, then the one tile «تركيبات مقترحة»; the focus on it opens the mixes, Back or up closes them into it",
      'names[mixAt] = "تركيبات مقترحة";' in pick and "if (pos == mixAt) mixes.open(v);" in pick and "mixes.close()" in pick
      and "KEYCODE_DPAD_UP" in pick and "close(); return true;" in pick)
check("the motion: a circular reveal from the tile's centre, the mixes fanning out of it, 250 ms slowing at the end; closing the same way back",
      "ViewAnimationUtils.createCircularReveal(panel, (int) c[0], (int) c[1], in ? 0f : r, in ? r : 0f)" in pick and "setDuration(PresetMotion.MS)" in pick
      and "new android.view.animation.DecelerateInterpolator(PresetMotion.EASE)" in pick and "translationX(in ? 0 : dx)" in pick)
check("a weak TV («الوضع الخفيف» on, or auto on a weak TV) only fades", "if (prefs.liteNow(SettingsActivity.this)) {" in pick
      and 'return "on".equals(v) || ("auto".equals(v) && lowRam(c));' in prefs and ".alpha(in ? 1f : 0f).setDuration(PresetMotion.MS)" in pick)
pics2 = [ROOT / f"android/app/src/main/assets/presets/{k}.jpg" for k in ["combos"] + ["combo-" + c[0] for c in combos]]
check("the tile's and each mix's picture are in the app, small", all(p.is_file() and p.stat().st_size < 40 * 1024 for p in pics2), [p.name for p in pics2 if not p.is_file()])
check("saver.sh preset takes a mix's key too", "preset tv|couch|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass}" in saver)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as d:
        run = (ROOT / "tests/PresetMotionRun.java")
        c = subprocess.run(["javac", "-d", d, str(APP / "PresetMotion.java"), str(run)], capture_output=True)
        check("PresetMotion compiles as plain Java", c.returncode == 0, c.stderr.decode()[-600:])
        r = subprocess.run(["java", "-cp", d, "sa.urtv.screensaver.PresetMotionRun"], capture_output=True, text=True)
        got = dict(l.split("=", 1) for l in r.stdout.split())
        check("…250 ms; the mockup's curve (half the reveal at about 52 ms, 87 % at half the time); the circle reaches the panel's farthest corner",
              r.returncode == 0 and got.get("ms") == "250" and abs(float(got["e50"]) - 0.875) < 1e-3 and abs(float(got["e21"]) - 0.5) < 0.01
              and abs(float(got["r"]) - 1000.0) < 0.01 and got.get("e0") == "0.0" and got.get("e1") == "1.0", got)
else:
    print("NOTE javac not found: PresetMotion not run here (CI runs it)")
sys.exit(1 if failed else 0)
