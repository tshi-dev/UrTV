#!/usr/bin/env python3
"""Arabic wording check (docs/STYLE-AR.md): WARNINGS only, never a failure.

Looks at the Arabic strings a change adds (git diff against a base) and warns on:
  - «·» (the middle dot) or an emoji in an Arabic string;
  - Western digits (0-9) in an Arabic string of a file whose Arabic strings use Arabic-Indic digits (٠-٩);
  - a string that got longer than its old wording allows (old length + 10%, at least 2 characters): TV space;
  - formal patterns the style guide replaces in sentences (notes, the person decides).

    python3 .github/tools/ar_style.py [base]          the diff from base (default: origin/work/44-beta4) to the working tree
    python3 .github/tools/ar_style.py --text "…" …    check the given strings (no length rule)
Exit code is always 0: the wording is a person's call (docs/STYLE-AR.md «review step»).
"""
import re
import subprocess
import sys

AR = re.compile(r"[؀-ۿ]")
LIT = re.compile(r'"((?:[^"\\\n]|\\.)*)"|\'((?:[^\'\\\n]|\\.)*)\'')
EMOJI = re.compile("[\U0001F000-\U0001FAFF☀-⛿✀-➿️]")
KEEP = set("✓✔✕✖★☆")   # marks the app already draws as text, not emoji
WEST = re.compile(r"(?<![%$\w])[0-9]")
EAST = re.compile(r"[٠-٩]")
PLACE = re.compile(r"%(\d+\$)?[sd]|\{\d+\}")   # placeholders, not digits on screen
FORMAL = [("لا يوجد", "ما فيه"), ("تعذّر", "ما قدرنا…"), ("أعد المحاولة", "جرّب مرة ثانية"),
          ("مسبقاً", "من قبل"), ("التي ", "اللي"), ("الذي ", "اللي"), ("قبل أن", "قبل لا")]
SOURCES = (".java", ".kt", ".html", ".js", ".mjs", ".tsv", ".xml")


def limit(n):
    return n + max(2, -(-n // 10))


def strings(line, path):
    if path.endswith(".tsv"):
        first = line.split("\t", 1)[0]
        return [first] if AR.search(first) else []
    out = [a or b for a, b in LIT.findall(line)]
    if path.endswith((".html", ".xml")):
        out += [t.strip() for t in re.findall(r">([^<>]+)<", line)]
    return [s for s in out if AR.search(s)]


def check_one(s, east_file=True):
    w = []
    if "·" in s:
        w.append("«·» in Arabic text")
    if any(c not in KEEP for c in EMOJI.findall(s)):
        w.append("emoji in Arabic text")
    bare = PLACE.sub("", re.sub(r"<[^>]*>", "", s))   # markup and placeholders are not on screen
    if WEST.search(bare) and (east_file or EAST.search(bare)):
        w.append("Western digits in Arabic text (the app shows ٠-٩)")
    for old, new in FORMAL:
        if old in s:
            w.append(f"note: «{old.strip()}» reads formal; the guide says «{new}» in sentences")
    return w


def east_file(path):
    try:
        text = open(path, encoding="utf-8").read()
    except OSError:
        return False
    return len(EAST.findall(text)) >= len(WEST.findall(" ".join(s for s in re.findall(r'"([^"\n]*[؀-ۿ][^"\n]*)"', text))))


def diff(base):
    out = subprocess.run(["git", "diff", "-U0", base, "--"], capture_output=True, text=True).stdout
    path, minus, report = None, [], []
    for line in out.splitlines():
        if line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else None
            minus = []
        elif line.startswith("@@"):
            minus = []
        elif path and path.endswith(SOURCES) and not path.startswith(("docs/", "tests/")):
            if line.startswith("-") and not line.startswith("---"):
                minus.append(line[1:])
            elif line.startswith("+"):
                if path.endswith(".tsv"):   # the same English: the same entry
                    tail = line[1:].split("\t", 1)[1:]
                    old = next((m for m in minus if m.split("\t", 1)[1:] == tail), None)
                else:
                    old = minus.pop(0) if minus else None
                olds = strings(old, path) if old is not None else []
                east = east_file(path)
                news = [x for x in strings(line[1:], path) if x not in olds]   # only what this change wrote
                gone = [x for x in olds if x not in news and x not in strings(line[1:], path)]
                for i, s in enumerate(news):
                    w = check_one(s, east)
                    if len(gone) == len(news) and len(s) > limit(len(gone[i])):   # one old wording for each new one
                        w.append(f"longer than the old wording allows ({len(s)} > {limit(len(gone[i]))})")
                    report += [(path, s, x) for x in w]
    return report


def main(argv):
    if argv[:1] == ["--text"]:
        report = [("text", s, w) for s in argv[1:] for w in check_one(s)]
    else:
        report = diff(argv[0] if argv else "origin/work/44-beta4")
    for path, s, w in report:
        print(f"WARN {path}: {w}: «{s[:80]}»")
    print(f"ar_style: {len(report)} warning(s); wording is a person's call (docs/STYLE-AR.md)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
