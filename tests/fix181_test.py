#!/usr/bin/env python3
"""Step 27 (build 181), the source-level checks of the fixes that have no pure part of their own: the update flow's end (UpdateActivity), the tasks page,
the English hint, reset.sh's appops, the setup's community list line. (The rules with a pure part have their own tests: saver_hold, mute_keep, markets_page,
football_page, setup_runner.) Run: python3 tests/fix181_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


upd = text(HOME / "UpdateActivity.java")
check("update: «تم» on the done screen and Back both leave for MainActivity (clear top), and the screen finishes", "private View doneButton()" in upd and 'S.t("تم")' in upd and "new android.content.Intent(this, MainActivity.class)" in upd
      and "FLAG_ACTIVITY_CLEAR_TOP" in upd and "public void onBackPressed()" in upd and "toHome();" in upd.split("public void onBackPressed()")[1][:300])
check("update: both success paths (the record of the replace, the live 'done' state) end with the «تم» button", upd.count("return doneButton();") == 2)
tasks = text(HOME / "TasksActivity.java")
check("tasks: the 13th is refused with «١٢ مهمة كحد أقصى» (the limit, said), shown on the page as well as in a toast", 'S.t("%s مهمة كحد أقصى", ar(MAX_TASKS))' in tasks and "if (status != null) status.setText(s);" in tasks and "MAX_TASKS = 12" in tasks)
check("tasks: every row the same minimum height with its card and buttons filling it, so the gap between rows is one number", "r.setMinimumHeight(dp(56));" in tasks and "new LinearLayout.LayoutParams(0, -1, 1f)" in tasks)
tsv = text(ROOT / "i18n/en/home.tsv")
for ar in ("%s مهمة كحد أقصى", "اكتب الكمية", "اكتب متوسط التكلفة"):
    check(f"i18n: «{ar}» has its English", re.search("^" + re.escape(ar) + "\t.+$", tsv, re.M) is not None)
tw = text(ROOT / "optimize/tweaks.sh")
check("reset: an appop put back to «allow» that reads «default» is not a problem (no !! line), nothing else changes", 'appop_*) [ "${kv#*=}" = allow ] && [ "$got" = default ]' in tw)
st = text(HOME / "setup/SetupTweaks.java")
check("setup: the same rule in the app (allow reads back as default)", 'k.startsWith("appop_") && v.equals("allow") && got.equals("default")' in st)
com = text(HOME / "setup/SetupCommunity.java")
check("setup: the community list used is named in the run's lines (and so in the report)", 'out.line("قائمة المجتمع لهذا الطراز: " + choice.meta.name);' in com)
mp = text(HOME / "MarketData.java")
check("markets: both spark shapes are read (spark.result[].response[].meta and the map keyed by symbol)", "// shape 1:" in mp and "// shape 2:" in mp)
mf = text(HOME / "MarketFile.java")
check("markets: an answer that parses to nothing logs its first 300 characters (UrTVMarkets body_start)", 'body_start=' in mf and "Math.min(300, body.length())" in mf)
py = text(ROOT / ".github/tools/market_prices.py")
check("markets CI: fx.json is carried by every run (the one-commit branch is rebuilt from the folder)", "carried_fx = True" in py and "allowlist_fx(prev_fx)" in py)
print("ALL PASS" if not failed else f"{failed} failed")
raise SystemExit(1 if failed else 0)
