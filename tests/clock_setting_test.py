#!/usr/bin/env python3
"""«ساعة ٢٤» on the screensaver's side (ClockSetting.java): what the receiver accepts, the settings fragment, which stays as it
was while the setting is off, and the settings screen's row «نظام الساعة» (١٢ / ٢٤ ساعة), read from the source: it changes the same
setting, which reaches the screensaver's page, the home's cards and the home's clock. The JVM part needs javac; without it, skipped
with a note.   Run: python3 tests/clock_setting_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]
pick = st[st.index("private void pickClock24()"):].split("\n    }\n")[0] if "private void pickClock24()" in st else ""
check("«نظام الساعة» is a row of the settings, its value the choice that is set",
      'row("نظام الساعة", ClockSetting.label(prefs.clock24()), R.drawable.ic_eye,' in render)
check("…in «العرض» (شاشة التوقف), right after «حجم الساعة», with the rows' own style (an icon, a picker)",
      render.index('header("العرض")') < render.index('row("حجم الساعة"') < render.index('row("نظام الساعة"') < render.index('row("الخانة اليسرى"')
      and "pickClock24(); } });" in render)
check("its picker: the two choices, the one set checked, a choice saved with the same pref as `saver.sh clock24`, the dialog closed and the rows redrawn",
      "new Dlg(this).setTitle(\"نظام الساعة\")" in pick and "setSingleChoiceItems(ClockSetting.CHOICES, ClockSetting.choice(prefs.clock24())," in pick
      and "prefs.setClock24(ClockSetting.fromChoice(i)); d.dismiss(); render();" in pick)
check("the pref the row sets is the one the receiver sets and the fragment carries",
      'void setClock24(boolean v) { sp.edit().putBoolean("clock24", v).apply(); }' in prefs and "+ ClockSetting.fragment(clock24())" in prefs
      and 'p.setClock24("on".equals(clock24))' in (APP / "SaverReceiver.java").read_text(encoding="utf-8"))
check("the fragment goes to the screensaver's page (dashboardUrl), the settings' previews (previewUrl) and the home (ConfigProvider)",
      'String dashboardUrl(Context c) { return pageBase(c) + "#" + fragment(); }' in prefs and "String frag = fragment()" in prefs
      and "c.addRow(new Object[] {p.fragment()," in (APP / "ConfigProvider.java").read_text(encoding="utf-8"))
home_main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
check("the home follows it: its clock reads it at every tick, the cards load a fresh page when the fragment changed, the Java screens' times through Digits",
      "boolean h24 = cfg.clock24();" in home_main and "Digits.remember(this, cfg);" in home_main
      and "if (!want.equals(url)) {" in card and "frag = c.getString(0);" in card
      and 'fragment.contains("&clock=24")' in (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8")
      and "clock24 = now24;" in (HOME / "Digits.java").read_text(encoding="utf-8"))
check("the page reads #clock=24", "clock24: _Q.get('clock') === '24'," in (ROOT / "src/template.html").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP clock setting: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ClockSetting.java"),
                    str(ROOT / "tests/ClockSettingRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.ClockSettingRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
