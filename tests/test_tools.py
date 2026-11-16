#!/usr/bin/env python3
"""tools/clean.py and tools/analyse.py on tests/sample.csv: hand-written, made-up reports (never real rows)."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import analyse  # noqa: E402
import clean  # noqa: E402

SAMPLE_SRC = ROOT / "tests/sample.csv"
# The personal-looking values are made here, at run time, so no file of this repository holds a phone number, an address or a path:
# tests/sample.csv has {PHONE}, {EMAIL}, {IP}, {PATH}, {WIFI} and {CITY} where a careless report would have them.
FAKE = {"PHONE": "05" + "1" * 8, "EMAIL": "someone" + "@" + "example.com", "IP": ".".join(["10", "0", "0", "7"]),
        "PATH": "/".join(["", "Users", "someone", "UrTV"]), "WIFI": "Home" + "Net", "CITY": "Ri" + "yadh"}
_tmp = tempfile.mkdtemp()
SAMPLE = Path(_tmp) / "sample.csv"
_text = SAMPLE_SRC.read_text(encoding="utf-8")
for _k, _v in FAKE.items():
    _text = _text.replace("{" + _k + "}", _v)
SAMPLE.write_text(_text, encoding="utf-8")


class Clean(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.recs, cls.dropped = clean.clean(clean.read_rows(SAMPLE))
        cls.by_row = {r["row"]: r for r in cls.recs}

    def test_drops(self):
        self.assertEqual(self.dropped, {"not a report": 1, "test": 2, "duplicate": 1})
        self.assertEqual(sorted(self.by_row), [2, 5, 6, 7, 10])   # 3 is 2 again, 4 says "test", 8 is an emulator, 9's owner wrote «تجربة»

    def test_only_allowlisted_lines(self):
        r = self.by_row[5]
        self.assertEqual(r["lines_dropped"], 2)   # «Wi-Fi name: …» and «Owner's city: …» are not in the allowlist
        text = json.dumps(self.recs, ensure_ascii=False)
        for secret in FAKE.values():
            self.assertNotIn(secret, text)
        self.assertNotIn("{", "".join(FAKE.values()))

    def test_scrub_marks(self):
        self.assertIn("<phone>", self.by_row[2]["owner"])
        self.assertIn("<email>", self.by_row[2]["owner"])
        self.assertIn("<ip>", self.by_row[6]["notes"])
        self.assertIn("<path>", self.by_row[6]["notes"])

    def test_tv_normalised(self):
        a, b = self.by_row[2]["tv"], self.by_row[5]["tv"]
        self.assertEqual((a["maker"], a["model"], a["device"]), ("TCL", "Smart TV Pro", "g07"))
        self.assertEqual((b["maker"], b["model"], b["device"]), ("TCL", "Smart TV Pro", "g07"))   # «tcl  Smart TV Pro (g07)»
        self.assertEqual((a["android"], a["sdk"]), ("11", 30))
        self.assertEqual(self.by_row[6]["tv"]["maker"], "Xiaomi")

    def test_versions(self):
        self.assertEqual((self.by_row[2]["version"], self.by_row[2]["build"]), ("4.3.1", None))
        self.assertEqual((self.by_row[5]["version"], self.by_row[5]["build"]), ("4.3.2", 309))
        self.assertEqual((self.by_row[6]["version"], self.by_row[6]["commit"], self.by_row[6]["commit_date"]), ("git-1a2b3c4", "1a2b3c4", "2026-10-30"))
        self.assertEqual(self.by_row[6]["kind"], "problem")

    def test_steps(self):
        s = self.by_row[2]["steps"]
        self.assertEqual([x["step"] for x in s], ["connect", "backup", "apps", "launcher", "launcher", "debloat"])
        self.assertEqual(s[3], {"step": "launcher", "seconds": 50, "stopped": True, "cause": "home switch: wizard owns wifi"})
        self.assertEqual(self.by_row[2]["gaps_seconds"], 130)
        self.assertEqual(self.by_row[2]["total_min"], 11)
        self.assertEqual(self.by_row[2]["adb"], {"drops": 2, "resumed": 1})
        self.assertEqual(self.by_row[7]["errors"], ["Could not disable com.example.vendor.thing (protected)"])

    def test_cli(self):
        with tempfile.TemporaryDirectory() as t:
            r = subprocess.run([sys.executable, str(ROOT / "tools/clean.py"), str(SAMPLE), "--out", t], capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertEqual(len((Path(t) / "reports.jsonl").read_text(encoding="utf-8").splitlines()), 5)
            self.assertEqual(json.loads((Path(t) / "dropped.json").read_text())["kept"], 5)


class Analyse(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        recs, _ = clean.clean(clean.read_rows(SAMPLE))
        cls.a = analyse.analyse(recs, since="2026-11-04", min_n=1)

    def test_stops(self):
        self.assertIn({"step": "launcher", "model": "TCL Smart TV Pro", "android": "11", "version": "4.3.1", "count": 1}, self.a["stops"])
        self.assertIn({"step": "debloat", "model": "Sony BRAVIA 4K VH2", "android": "10", "version": "4.3.2", "count": 1}, self.a["stops"])
        self.assertEqual(len(self.a["stops"]), 3)

    def test_causes_new_and_repeated(self):
        c = {e["cause"]: e for e in self.a["causes"]}
        self.assertEqual(c["home switch: wizard owns wifi"]["reports"], 2)
        self.assertEqual(c["home switch: wizard owns wifi"]["status"], "repeated")
        self.assertEqual(c["could not disable <pkg> (protected)"]["status"], "new")   # a package name is not part of the cause
        self.assertEqual(c["disable: user # protected"]["status"], "new")

    def test_regression(self):
        self.assertEqual(self.a["regressions"], [{"step": "debloat", "from": "4.3.1", "to": "4.3.2", "rate_from": 0.0, "rate_to": 0.5, "tried": 2}])
        self.assertEqual(analyse.analyse(clean.clean(clean.read_rows(SAMPLE))[0], min_n=3)["regressions"], [])   # too few reports by default

    def test_version_order(self):
        recs = [{"version": v, "commit_date": d} for v, d in (("4.10.0", None), ("4.3.2", None), ("git-bbbbbbb", "2026-11-02"), ("git-aaaaaaa", "2026-11-01"))]
        self.assertEqual(sorted({r["version"] for r in recs}, key=lambda v: analyse.version_key(v, recs)), ["4.3.2", "4.10.0", "git-aaaaaaa", "git-bbbbbbb"])

    def test_timings(self):
        self.assertEqual(self.a["timings"]["connect"], {"n": 4, "median_s": 37.5, "p90_s": 47.0})
        self.assertEqual(self.a["timings"]["launcher"]["n"], 3)   # the stopped ones are not timings
        self.assertEqual(self.a["total_by_version"]["4.3.2"], {"n": 3, "median_min": 9})

    def test_markdown(self):
        md = analyse.markdown(self.a)
        self.assertIn("## Regressions by version", md)
        self.assertIn("**debloat**", md)


if __name__ == "__main__":
    unittest.main(verbosity=1)
