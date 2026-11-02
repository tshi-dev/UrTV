#!/usr/bin/env python3
"""«اللغة» for the files that are not Java (docs/ENGLISH.md): the translation table (i18n/en/*.tsv), the S.t literals of the Java code, and
the hook that writes the English copy of an HTML file. No dependencies. The Java side is Tr.java (the same rules: tests/i18n_test.py runs
both on the same cases).

  table = load()                    every i18n/en/*.tsv as {arabic: english}; Bad on a broken line or two entries that disagree
  english, left = translate(html, table)
                                    the English copy of a page (the Arabic file itself is never changed): every whole Arabic literal
                                    ('…', "…", `…`) and every whole HTML text node (>…<) that has an entry is replaced, <html lang="ar"
                                    dir="rtl"> becomes lang="en" dir="ltr"; left = the Arabic literals and text nodes it found no entry for
  arabic_literals(html)             what translate looks at, in order: [(start, end, arabic)]
  java_literals(source)             the Arabic of every S.t("…") / S.n("…") in a Java file (comments skipped)

The English that replaces a literal must be safe inside JS quotes and HTML, so an entry used by a page has none of ' " ` < > & backslash and no
line break (write ’ for an apostrophe). Placeholders are as in Java: %s, %1$s…%9$s, %% (the page's own code fills them).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TABLE_DIR = ROOT / "i18n/en"
AR = "\u0600-\u06ff"
# a quoted literal with Arabic in it, on one line, whole: opened after code punctuation or a space (so the ’s of an English comment is no
# quote), closed before the same; no backslash, no other quote, no ${…} inside (a template with values is no one literal)
QUOTED = re.compile(r"""(?:(?<=^)|(?<=[\s(\[{,:=+?!|&>;]))(['"`])((?:(?!\$\{)[^'"`\n\\<>])*?[%s](?:(?!\$\{)[^'"`\n\\<>])*?)\1(?=[\s)\]},:;+.?|&<]|$)""" % AR, re.M)
# an HTML text node with Arabic in it, whole, on one line; nothing that looks like code (braces, =, ;, $, backticks, parentheses)
TEXT_NODE = re.compile(r">(\s*)([^<>\n{}`;=$()]*?[%s][^<>\n{}`;=$()]*?)(\s*)<" % AR)
UNSAFE = set("'\"`<>&\\\n\r")


class Bad(Exception):
    pass


def unescape(s):
    """\\t, \\n, \\\\ and \\uXXXX as the character (Tr.unescape)."""
    out, i = [], 0
    while i < len(s):
        ch = s[i]
        if ch != "\\" or i + 1 >= len(s):
            out.append(ch); i += 1; continue
        n = s[i + 1]
        if n == "t": out.append("\t"); i += 2
        elif n == "n": out.append("\n"); i += 2
        elif n == "\\": out.append("\\"); i += 2
        elif n == "u" and re.fullmatch(r"[0-9A-Fa-f]{4}", s[i + 2:i + 6]):
            out.append(chr(int(s[i + 2:i + 6], 16))); i += 6
        else:
            out.append("\\" + n); i += 2
    return "".join(out)


def parse(text, name="table"):
    """[(line number, arabic, english)] of one file; Bad on a line that is not «arabic<TAB>english». English «=» is the Arabic itself."""
    rows = []
    for no, line in enumerate(text.split("\n"), 1):
        line = line.rstrip("\r")
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) != 2 or not parts[0] or not parts[1]:
            raise Bad(f"{name}:{no}: not «arabic<TAB>english» (exactly one tab, both sides filled)")
        ar = unescape(parts[0])
        rows.append((no, ar, ar if parts[1] == "=" else unescape(parts[1])))   # «=»: this one stays Arabic (a verse, a club's name)
    return rows


def load(directory=TABLE_DIR, where=None):
    """{arabic: english} of every *.tsv; the same Arabic in two places must have the same English. where, when given, gets
    {arabic: "file:line"} of the first place each entry is."""
    table, seen = {}, {} if where is None else where
    for f in sorted(Path(directory).glob("*.tsv")):
        for no, ar, en in parse(f.read_text(encoding="utf-8"), f.name):
            if ar in table and table[ar] != en:
                raise Bad(f"{f.name}:{no}: «{ar}» is already {seen[ar]} with another English")
            if ar not in table:
                table[ar], seen[ar] = en, f"{f.name}:{no}"
    return table


def placeholders(s):
    """The argument numbers a text uses, sorted, each once (Tr.placeholders): '%s و%s' and '%2$s and %1$s' are both [1, 2]."""
    out, nxt, i = [], 0, 0
    while i + 1 < len(s):
        if s[i] != "%":
            i += 1; continue
        n, k = s[i + 1], 0
        if n == "s":
            nxt += 1; k = nxt; i += 2
        elif n == "%":
            i += 2
        elif "1" <= n <= "9" and s[i + 2:i + 4] == "$s":
            k = int(n); i += 4
        else:
            i += 1
        if k and k not in out:
            out.append(k)
    return sorted(out)


def arabic_literals(html):
    """[(start, end, arabic)] of every whole quoted literal and text node with Arabic, by position; start/end are the Arabic's own."""
    quoted = [(m.start(2), m.end(2), m.group(2)) for m in QUOTED.finditer(html)]
    nodes = [(m.start(2), m.end(2), m.group(2)) for m in TEXT_NODE.finditer(html)]
    found, end = [], -1
    for a, b, ar in sorted(quoted + nodes, key=lambda x: (x[0], -x[1])):   # a text node holding a quoted word wins: it is the whole text
        if a >= end:
            found.append((a, b, ar))
            end = b
    return found


def translate(html, table):
    """(the English copy, [the Arabic it found no entry for, each once, in order]). The Arabic file is not touched."""
    out, left, at = [], [], 0
    for a, b, ar in arabic_literals(html):
        out.append(html[at:a])
        if ar in table:
            out.append(table[ar])
        else:
            out.append(ar)
            if ar not in left:
                left.append(ar)
        at = b
    out.append(html[at:])
    return "".join(out).replace('<html lang="ar" dir="rtl">', '<html lang="en" dir="ltr">', 1), left


def strip_comments(src):
    """Java source with // and /* */ comments blanked (string and char literals kept), line breaks kept."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"' or c == "'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            out.append(src[i:j + 1]); i = j + 1
        elif src.startswith("//", i):
            j = src.find("\n", i)
            j = n if j < 0 else j
            i = j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("\n" * src.count("\n", i, j)); i = j
        else:
            out.append(c); i += 1
    return "".join(out)


def mask_literals(code):
    """The same text, every string and char literal's inside as spaces (same length, so offsets agree)."""
    out, i, n = [], 0, len(code)
    while i < n:
        c = code[i]
        if c == '"' or c == "'":
            j = i + 1
            while j < n and code[j] != c:
                j += 2 if code[j] == "\\" else 1
            out.append(c + " " * (min(j, n) - i - 1) + (c if j < n else "")); i = j + 1
        else:
            out.append(c); i += 1
    return "".join(out)


def java_unescape(s):
    """A Java string literal's body as its text."""
    simple = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", "0": "\0", '"': '"', "'": "'", "\\": "\\", "s": " "}
    out, i = [], 0
    while i < len(s):
        if s[i] == "\\" and i + 1 < len(s):
            n = s[i + 1]
            if n == "u":
                j = i + 1
                while j < len(s) and s[j] == "u":
                    j += 1
                out.append(chr(int(s[j:j + 4], 16))); i = j + 4; continue
            out.append(simple.get(n, n)); i += 2; continue
        out.append(s[i]); i += 1
    return "".join(out)


S_CALL = re.compile(r"\bS\.[tn]\(")
LITERAL = re.compile(r'\s*"((?:[^"\\\n]|\\.)*)"\s*([,)])')


def java_literals(src, name="source"):
    """[(line, arabic)] of every S.t("…") and S.n("…") call; Bad on a call whose first argument is not one plain literal (a variable, "a" + b)."""
    code = strip_comments(src)
    found = []
    for m in S_CALL.finditer(mask_literals(code)):   # a call written inside a string is no call
        line = code.count("\n", 0, m.start()) + 1
        lit = LITERAL.match(code, m.end())
        if not lit:
            raise Bad(f"{name}:{line}: S.t( and S.n( take one plain string literal first (the completeness test must see it); a variable goes to S.tv(")
        found.append((line, java_unescape(lit.group(1))))
    return found


if __name__ == "__main__":
    try:
        t = load()
    except Bad as e:
        print(e); sys.exit(1)
    print(f"{len(t)} entries in {TABLE_DIR.relative_to(ROOT)}")
