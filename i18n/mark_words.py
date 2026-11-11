#!/usr/bin/env python3
"""After adding rows to i18n/en/saver.tsv: mark the new ones in Words.java (SHOWN, or PATTERNS for a row with %s), which tests/i18n_saver_test.py asks for.

    python3 i18n/mark_words.py        adds one line of S.n("…") per list for the rows not marked yet; changes nothing when all are

It only ever adds; a row removed from the table is removed from Words.java by hand."""
import re, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import tr

ROOT = Path(__file__).resolve().parents[1]
WORDS = ROOT / "android/app/src/main/java/sa/urtv/screensaver/Words.java"


def main():
    src = WORDS.read_text(encoding="utf-8")
    marked = {ar for _, ar in tr.java_literals(src, "Words.java")}
    xml = re.findall(r"<string name=\"[^\"]+\"[^>]*>(.*?)</string>", (ROOT / "android/app/src/main/res/values/strings.xml").read_text(encoding="utf-8"))
    rows = [ar for _, ar, _ in tr.parse((ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8"), "saver.tsv")]
    new = [r for r in dict.fromkeys(rows) if r not in marked and r not in xml]
    lit = lambda s: 'S.n("' + s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '")'
    for block, pick in (("static final String[] SHOWN", [r for r in new if "%" not in r]), ("static final String[] PATTERNS", [r for r in new if "%" in r])):
        if not pick:
            continue
        end = src.index("\n    };", src.index(block))
        src = src[:end] + "\n        " + ", ".join(lit(r) for r in pick) + "," + src[end:]
    WORDS.write_text(src, encoding="utf-8")
    print(f"{len(new)} rows marked")


if __name__ == "__main__":
    main()
