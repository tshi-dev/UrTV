#!/usr/bin/env python3
"""A14 (4.3.3) «الوضع الخفيف»: the LOW tier is the light profile (fewer animations: no transitions, no seconds; slower refresh; smaller photos; one photo layer; no
blurred copy behind the card). It comes by itself on a weak TV (Android's low-RAM flag, under 2000 MB, or 2 cores or fewer), and the person can force it on or off
(the screensaver's settings row, `saver.sh lite auto|on|off`); off never gives LOW but keeps MID's memory cuts. The home follows through the provider's «lite» column.
The tester's `ramtier` switch still wins. The rule is the same in both apps.

    python3 tests/lite_mode_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return Path(p).read_text(encoding="utf-8")

pf, ram = text(SAVER / "Prefs.java"), text(HOME / "Ram.java")
check("the same core count in both apps (2)", "static final int LITE_CORES = 2;" in pf and "static final int LITE_CORES = 2;" in ram)
check("the cores rule in both: 2 cores or fewer is LOW, else the memory rule",
      "return cores > 0 && cores <= LITE_CORES ? RAM_LOW : ramTier(lowRamFlag, mb);" in pf and "return cores > 0 && cores <= LITE_CORES ? LOW : tier(lowRamFlag, totalMb);" in ram)
check("the person's choice in both: on = LOW, off = at least MID, auto = found",
      'return "on".equals(lite) ? RAM_LOW : "off".equals(lite) ? Math.max(found, RAM_MID) : found;' in pf and 'return "on".equals(lite) ? LOW : "off".equals(lite) ? Math.max(found, MID) : found;' in ram)
LOW, MID, NORMAL = 0, 1, 2
def tier(flag, mb, cores):
    if cores > 0 and cores <= 2: return LOW
    if flag or (mb > 0 and mb < 2000): return LOW
    return MID if mb > 0 and mb < 2600 else NORMAL
def lite(found, v): return LOW if v == "on" else max(found, MID) if v == "off" else found
check("auto: a 4-core 3 GB TV is NORMAL, a 2-core 3 GB TV LOW, a 1-core one LOW, unknown cores (0) as the memory says",
      [tier(False, 2800, 4), tier(False, 2800, 2), tier(False, 2800, 1), tier(False, 2800, 0), tier(False, 1800, 4)] == [NORMAL, LOW, LOW, NORMAL, LOW])
check("on: LOW everywhere; off: a LOW TV becomes MID, MID and NORMAL stay",
      [lite(t, "on") for t in (LOW, MID, NORMAL)] == [LOW] * 3 and [lite(t, "off") for t in (LOW, MID, NORMAL)] == [MID, MID, NORMAL] and [lite(t, "auto") for t in (LOW, MID, NORMAL)] == [LOW, MID, NORMAL])
check("the saver: cores and the choice in ramTier(c), the tester's ramtier after them (it wins)",
      re.search(r'ramTier\(flag, ramMb\(c\), Runtime\.getRuntime\(\)\.availableProcessors\(\)\);.*?t = liteTier\(t, s\.getString\("lite", "auto"\)\);.*?if \(f >= 0\) t = f;', pf, re.S) is not None)
check("the home: the same order in Ram.tier(c)",
      re.search(r'tier\(flag, mb, Runtime\.getRuntime\(\)\.availableProcessors\(\)\);.*?t = liteTier\(t, sp\.getString\("lite", "auto"\)\);.*?if \(f >= 0\) t = f;', ram, re.S) is not None)
check("a change of the choice is read again at once (the cached tier forgotten) in both", "void setLite(String v) {" in pf and "ramTier = -1; }" in pf.split("void setLite(String v) {")[1].split("\n")[0] and "tier = -1;" in ram.split("static void lite(Context c, String v)")[1].split("/** a forced")[0])
cp, cs = text(SAVER / "ConfigProvider.java"), text(HOME / "ConfigSnapshot.java")
check("the provider says «lite» (last column: an older home reads its columns by name), the home passes it to Ram",
      '"city_chosen", "lite"});' in cp and 'p.cityChosen() ? "1" : "0", p.lite()});' in cp and 'int li = cur.getColumnIndex("lite");' in cs and "if (li >= 0) Ram.lite(c, cur.getString(li));" in cs)
st = text(SAVER / "SettingsActivity.java")
check("the settings row «الوضع الخفيف» cycles auto, on, off, and says whether auto is on here (no «·»)",
      'row("الوضع الخفيف", Prefs.liteLabel(prefs.lite(), Prefs.lowRam(this))' in st and 'prefs.setLite("auto".equals(v) ? "on" : "on".equals(v) ? "off" : "auto")' in st
      and "·" not in pf.split("static String liteLabel")[1].split("\n")[0])
rc, sh = text(SAVER / "SaverReceiver.java"), text(ROOT / "optimize/saver.sh")
check("saver.sh lite auto|on|off, checked by the receiver, and `show` says lite:",
      'lite)     case "${2:-}" in auto|on|off) set1 lite "$2"' in sh and '"error: lite is one of auto on off"' in rc and "if (lite != null) p.setLite(lite);" in rc and '"\\nlite: " + p.lite()' in rc)
check("docs/SETTINGS.md has the row with its command", "optimize/saver.sh lite" in text(ROOT / "docs/SETTINGS.md") and "الوضع الخفيف" in text(ROOT / "docs/SETTINGS.md"))
tpl = text(ROOT / "src/template.html")
check("the page: no blurred copy behind the card in the light profile", "if(!CARD_FILTER[CONFIG.cardStyle] || CONFIG.lowram) return;" in tpl)
check("what LOW already does stays: no transitions, slower timers, one photo layer", "html.lowram *, html.lowram *::before, html.lowram *::after { transition: none !important; }" in tpl and "CONFIG.lowram ? 15000 : 3000" in tpl and "CONFIG.lowram || !imgs[0].classList.contains('on')" in tpl)
check("the home in the light profile: the focused tile without its elevation shadow and shine (the scale and the highlight stay)",
      "if (Ram.low(this)) dbgFocus |= DebugFlags.LIFT | DebugFlags.SHINE;" in text(HOME / "MainActivity.java"))
sys.exit(1 if failed else 0)
