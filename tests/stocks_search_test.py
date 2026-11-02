#!/usr/bin/env python3
"""«قائمة الأسهم»: a person can type a Tadawul code (2222, ٢٢٢٢, 2222.SR), a company's name (أرامكو, مصرف الراجحي, بنك الرياض, Aramco) or a
US ticker, and the screen says what a name was taken as. Stocks.java (no Android) on the cases below; its names are the page's (TADAWUL in
src/template.html) and their English the page's (i18n/en/page.tsv); the settings screen uses it and shows «أرامكو 2222» in its row.
Needs javac for the Java cases; without it they are skipped with a note.
Run: python3 tests/stocks_search_test.py"""
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

stocks = (APP / "Stocks.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
page = (ROOT / "src/template.html").read_text(encoding="utf-8")

check("Stocks is pure: no android import", "import android" not in stocks)
rows = [re.findall(r'"([^"]*)"', r) for r in re.findall(r"^\s*\{(\"\d{4}\".*)\},$", stocks, re.M)]
blk = page[page.index("const TADAWUL = {"):]
blk = blk[:blk.index("};")]
web = re.findall(r"'(\d{4})':'([^']+)'", blk)
check("the names are the page's, code for code and in its order", [(r[0], r[1]) for r in rows] == web, [(r[0], r[1]) for r in rows])
tsv = {}
for line in (ROOT / "i18n/en/page.tsv").read_text(encoding="utf-8").split("\n"):
    if "\t" in line and not line.startswith("#"):
        a, b = line.split("\t", 1)
        tsv.setdefault(a, b)
wrong = [(r[1], r[2], tsv.get(r[1])) for r in rows if tsv.get(r[1], r[1]) != r[2]]
check("the English names are the page's", not wrong, wrong)
ask = st.split("private void askStocks(")[1].split("\n    }\n")[0]
check("the settings screen reads the text with Stocks.parse and says what a name was taken as",
      "Stocks.parse(in.getText().toString())" in ask and 'toast("الأسهم: "' in ask and "got.matched" in ask and 'toast("رمز غير صالح: " + got.bad)' in ask)
check("the saved list is still codes and tickers only (what the page asks Yahoo for)", "prefs.setStocks(v);" in ask and "List<String> v = got.codes;" in ask)
check("its row shows the company beside the code", "Stocks.label(k, S.en())" in st.split("private String stocksLabel(")[1].split("\n    }\n")[0])
check("tests/run.sh runs this test", "tests/stocks_search_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP stocks search: no javac here")
    sys.exit(1 if failed else 0)
CASES = [   # what is typed -> codes | matched | bad
    ("2222 1120 AAPL NVDA", "2222,1120,AAPL,NVDA||"),
    ("٢٢٢٢ ١١٢٠", "2222,1120|2222,1120|"),
    ("2222.SR", "2222|2222|"),
    ("أرامكو", "2222|2222|"),
    ("ارامكو، الراجحي", "2222,1120|2222,1120|"),
    ("مصرف الراجحي سابك", "1120,2010|1120,2010|"),
    ("بنك الرياض 7010", "1010,7010|1010|"),
    ("دار الأركان", "4300|4300|"),
    ("البنك العربي الوطني", "1080|1080|"),
    ("راجحي", "1120|1120|"),
    ("راجح", "1120|1120|"),
    ("المَراعي", "2280|2280|"),
    ("اس تي سي", "7010|7010|"),
    ("Aramco Almarai", "2222,2280|2222,2280|"),
    ("Al Rajhi, Riyad Bank", "1120,1010|1120,1010|"),
    ("stc SABIC elm", "STC,SABIC,ELM||"),          # a short Latin word stays a ticker, as before
    ("aapl 2222 أرامكو", "AAPL,2222||"),           # the same company twice is kept once
    ("بنك", "||بنك"),                              # several companies start so: nothing guessed
    ("شركة", "||شركة"),
    ("2222 XYZ!", "2222||XYZ!"),
    ("12345", "||12345"),
    ("", "||"),
]
consts = re.search(r'static final String STOCK_PATTERN = "[^"]*";', prefs).group(0)
with tempfile.TemporaryDirectory() as tmp:
    d = Path(tmp)
    (d / "Prefs.java").write_text("package sa.urtv.screensaver; final class Prefs { " + consts + " }", encoding="utf-8")
    (d / "StocksRun.java").write_text(
        "package sa.urtv.screensaver; import java.nio.file.*; import java.util.*; public final class StocksRun { public static void main(String[] a) throws Exception {"
        " StringBuilder o = new StringBuilder(); for (String line : Files.readAllLines(Paths.get(a[0]), java.nio.charset.StandardCharsets.UTF_8)) {"
        " Stocks.Parsed p = Stocks.parse(line); o.append(String.join(\",\", p.codes)).append('|').append(String.join(\",\", p.matched)).append('|')"
        ".append(p.bad == null ? \"\" : p.bad).append('\\n'); }"
        " o.append(Stocks.label(\"2222\", false)).append('|').append(Stocks.label(\"2222\", true)).append('|').append(Stocks.label(\"AAPL\", false)).append('\\n');"
        " System.out.write(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); System.out.flush(); } }", encoding="utf-8")
    (d / "in.txt").write_text("\n".join(c for c, _ in CASES) + "\n", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "Stocks.java"), str(d / "Prefs.java"), str(d / "StocksRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.StocksRun", str(d / "in.txt")], check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for (typed, want), got in zip(CASES, out):
    check(f"typed {typed!r}", got == want, f"{got!r} != {want!r}")
check("the match as shown: «أرامكو 2222», «Aramco 2222», a ticker alone", out[len(CASES)] == "أرامكو 2222|Aramco 2222|AAPL", out[len(CASES)])

print("\n" + (f"{failed} failed" if failed else "stocks search: all passed"))
sys.exit(1 if failed else 0)
