#!/usr/bin/env python3
"""4.4 (GitHub #60): the home's «الأسواق ← قائمتي» showed 4164, 4002 and 4349 as numbers: MarketData.name() knew only the 30 largest. The names now come from the
page's own maps (TADAWUL and TADAWUL_MORE in src/template.html) through the generated MarketNames.java: one list, no second hand-kept one.
Run: python3 tests/market_names_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
sys.path.insert(0, str(ROOT / ".github/tools"))
import market_names  # noqa: E402

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


names = market_names.page_names()
page = (ROOT / "src/template.html").read_text(encoding="utf-8")
every = {}
for obj in ("TADAWUL", "TADAWUL_MORE"):
    every.update(re.findall(r"'(\d{4})':'([^']+)'", re.search(r"const " + obj + r" = \{(.*?)\n\};", page, re.S).group(1)))
check("the page's two maps give the names (the 30 and the more)", len(names) == len(every) >= 45 and {"4164", "4002", "4349", "2222"} <= set(names), len(names))
check("MarketNames.java is current (python3 .github/tools/market_names.py makes it again)", (HOME / "MarketNames.java").read_text(encoding="utf-8") == market_names.source(names))
check("MarketData.name() asks MarketNames", "String more = MarketNames.get(code);" in (HOME / "MarketData.java").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP the JVM run: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "MarketData.java"), str(HOME / "MarketNames.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/MarketNamesRun.java")], check=True, capture_output=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.MarketNamesRun", *every, "9999", "AAPL"], capture_output=True)
got = dict(l.split("\t") for l in run.stdout.decode("utf-8").splitlines() if "\t" in l)
bad = {c: (n, got.get(c)) for c, n in every.items() if got.get(c) != n}
check("every code of the page's maps resolves to the same name in MarketData.name()", not bad and len(got) == len(every) + 2, dict(list(bad.items())[:5]))
check("a code the page has no name for, and a US ticker, stay as they are", got.get("9999") == "9999" and got.get("AAPL") == "AAPL", got)
sys.exit(1 if failed else 0)
