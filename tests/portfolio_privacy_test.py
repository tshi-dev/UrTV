#!/usr/bin/env python3
"""«محفظتي» stays on this TV (docs/PROPOSALS-NOV2.md §7): each road out of the TV is checked to carry no holding.

1. optimize/share.py (share.sh reports): a fake TV state with a holding in it goes through the report's own filters (restrict: the
   allow-list, scrub: the net); no number of the holding is left.
2. «أرسل تقرير» on the TV (Report.java): its fixed fields only, and no name of the portfolio's prefs in it or its screen.
3. Settings export and import (optimize/settings_move.py): «portfolio» is not in ALLOWED_TOP; an import file with one is refused (reported,
   never sent), and an export never has one.
4. ConfigProvider, the URL, the commands: the screensaver app never names the holdings; the page's fragment has no key for them; no
   saver.sh or dock.sh command reads them.
5. Where they live: Portfolio's private prefs, backup off; `pin reset` and «احذف المحفظة» wipe them.
The phone API (only to a paired phone, only while the TV edits) is tests/PhoneRun.java's portfolio(), run by tests/phone_test.py.
Run: python3 tests/portfolio_privacy_test.py"""
import importlib.util
import json
import os
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


# a holding nobody would type by chance: its numbers must appear nowhere
QTY, COST, CODE = "7319", "48.3517", "4013"
HOLDING = f"{CODE}|{QTY}:{COST}"
KEYS = ("holdings", "hide_amounts", "lock_with_pin")

# ---- 1. share.py: the report from a TV whose state shows a holding
sys.path.insert(0, str(ROOT / "optimize"))
import share  # noqa: E402

share.live_versions = lambda: None
share.RUN = Path(tempfile.mkdtemp())
# the TV's state as a run folder could hold it: the home's prefs dumped (shared_prefs/portfolio.xml), a line about them, the report's own
# lines with a holding beside them. (A `!! ` line is a script's free text and passes by its start: no script ever prints them, part 4.)
(share.RUN / "run.log").write_text("\n".join([
    f"pref portfolio holdings={HOLDING}",
    f'<string name="holdings">{HOLDING}</string>',
    f"- portfolio {QTY} x {COST}",
]), encoding="utf-8")
(share.RUN / "report.md").write_text("\n".join([
    "Home: ok",
    f"Portfolio: {HOLDING}",
    f"- holdings: {QTY} {COST}",
    f'  <string name="holdings">{HOLDING}</string>',
    "Versions: screensaver 3.1, home 3.1",
]), encoding="utf-8")
built = "\n".join(share.trouble() + share.report_checks())
report = share.restrict(share.scrub(built))
check("share.py: a holding in the TV's state reaches no line of the report (the allow-list drops it, the net is after it)",
      QTY not in report and COST not in report and HOLDING not in report, report)
check("share.py: «portfolio» and the prefs' keys are no field of the report's allow-list",
      not any(share.ALLOWED_LINE.match(f"{k}: {QTY}") for k in ("Portfolio", "portfolio", *KEYS)))
check("share.py never reads the home's prefs (no shared_prefs, no run-as)", "shared_prefs" not in text(ROOT / "optimize/share.py") and "run-as" not in text(ROOT / "optimize/share.py"))

# ---- 2. «أرسل تقرير» on the TV
rep = text(HOME / "Report.java") + text(HOME / "ReportActivity.java")
check("Report.java and its screen name nothing of the portfolio (fixed fields: versions, Android, WebView)",
      not re.search(r"(?i)portfolio|holding|Holdings|محفظ", rep) and not any(k in rep for k in KEYS), re.findall(r"(?i)portfolio|holding", rep))
lines = re.search(r"public static List<String> lines\((.*?)\) \{", text(HOME / "Report.java")).group(1)
check("Report.lines takes only the versions, Android and the WebView", lines == "String home, String saver, String release, int sdk, String webView", lines)

# ---- 3. settings export and import
spec = importlib.util.spec_from_file_location("settings_move", ROOT / "optimize/settings_move.py")
sm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sm)
check("settings_move: «portfolio» (and the holdings) are not in ALLOWED_TOP", not any(k in sm.ALLOWED_TOP for k in ("portfolio", "holdings", *KEYS)), sm.ALLOWED_TOP)
clean, errs, notes = sm.validate({"schema": 1, "digits": "latin", "portfolio": {"holdings": HOLDING}, "holdings": [HOLDING]})
check("settings_move: an import file with «portfolio» is refused for it: reported as ignored, never taken",
      "portfolio" not in clean and "holdings" not in clean and any("portfolio" in n for n in notes) and clean.get("digits") == "latin", (clean, notes))
ops = sm.ops(clean, {})
check("settings_move: nothing of it would be sent to the TV", not any(QTY in o or COST in o or "portfolio" in o for o in ops), ops)
src = text(ROOT / "optimize/settings_move.py")
check("settings_move never reads the home's prefs", "shared_prefs" not in src and "portfolio" not in src.lower().replace("«محفظتي»", ""), "")

# ---- 4. ConfigProvider, the URL, the commands
app = "\n".join(text(p) for p in APP.glob("*.java"))
check("the screensaver app (ConfigProvider, Prefs, the fragment) never names the holdings", not re.search(r"(?i)portfolio|holding", app), re.findall(r"(?i)\w*portfolio\w*|\w*holding\w*", app)[:5])
page = text(ROOT / "src/template.html")
check("the page has no key for them (#… of the fragment)", not re.search(r"(?i)_Q\.get\('(portfolio|holdings?)'\)|_list\('(portfolio|holdings?)'\)", page))
act = text(HOME / "MarketsActivity.java")
check("the markets page puts no holding in an Intent or a URL (the only extra is the phone screen's flag)",
      all("Holdings" not in l and "Portfolio.load" not in l for l in act.splitlines() if "putExtra" in l or "Uri." in l) and "EXTRA_PORTFOLIO, true" in act)
shs = {p.name: text(p) for p in (ROOT / "optimize").glob("*.sh")}
check("no command reads them (saver.sh, dock.sh and the rest have no portfolio)", not any(re.search(r"(?i)portfolio|holdings", t) for t in shs.values()),
      [n for n, t in shs.items() if re.search(r"(?i)portfolio|holdings", t)])
rx = text(HOME / "DockReceiver.java")
check("DockReceiver has no action that reads them", not re.search(r"(?i)portfolio|holding", rx.replace("Parental.adbReset", "")))

# ---- 5. where they live, and the wipe
pf = text(HOME / "Portfolio.java")
check("Portfolio: this app's private prefs (MODE_PRIVATE), the one place that keeps them", 'getSharedPreferences(PREFS, Context.MODE_PRIVATE)' in pf
      and sum("getSharedPreferences(\"portfolio\"" in text(p) for p in HOME.rglob("*.java")) == 0)
check("Android backup is off in both apps", 'android:allowBackup="false"' in text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
      and 'android:allowBackup="false"' in text(ROOT / "android/app/src/main/AndroidManifest.xml"))
check("only the markets page and the phone bridge touch them", sorted(p.name for p in HOME.rglob("*.java") if "Portfolio.load(" in text(p) or "Portfolio.save(" in text(p))
      == ["MarketsActivity.java", "PortfolioPhone.java"], [p.name for p in HOME.rglob("*.java") if "Portfolio.load(" in text(p)])
check("`dock.sh pin reset` wipes them, and «احذف المحفظة» does", "Portfolio.wipe(c);" in text(HOME / "Parental.java") and "Portfolio.wipe(this);" in act)
check("the phone's window: in memory only, opened by the TV (the tab, «أسهل من الجوال»), a few minutes", "private static final PortfolioWindow WINDOW = new PortfolioWindow();" in pf
      and "EDIT_MS = 10 * 60_000L" in pf and "Portfolio.openEditing(" in act and "Portfolio.openEditing(" in text(HOME / "PhoneActivity.java"))
pp = text(HOME / "PortfolioPhone.java")
check("the phone bridge reads and writes only inside the window", pp.count("if (!Portfolio.editing(now))") == 2)
check("«أخفِ المبالغ»: «••••» for the amounts, the percentages and prices stay", '"••••"' in act and "hide ? dots : price(r.h.avg())" in act and "price(r.price)" in act)
check("«اقفل محفظتي»: the parental PIN before the tab, the same PIN and waits (Parental.confirm), only while a PIN exists",
      "Portfolio.locked(this) && Parental.state(this).pinSet" in act and "Parental.confirm(this, Parental.adhanFirst(this)" in act
      and 'S.t("ضع رقمًا سريًا من الرقابة الأبوية أولًا")' in act)
sys.exit(1 if failed else 0)
