#!/usr/bin/env python3
"""No network or device identity in the repository's notes (AGENTS.md: never an IP address, serial, Wi-Fi name or user name in reports
or docs; the tester's report once carried a Wi-Fi name, its BSSID and MAC addresses, removed in f49cf8eb). Every tracked file under
tester/ and docs/ is read: no IPv4 address (except 0.0.0.0, 127.0.0.1 and the documentation ranges 192.0.2.x, 198.51.100.x,
203.0.113.x), no MAC or BSSID (six hex pairs), no «SSID:», «BSSID=», «Wi-Fi name:» or «serial:» with a value after it, no e-mail address
(LESSONS R21; GitHub's noreply addresses and example.com/.org are fine).
Run: python3 tests/regressions/privacy_test.py"""
import re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IP = re.compile(r"(?<![\d.])(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?![\d.])")
MAC = re.compile(r"(?<![0-9A-Fa-f:-])(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}(?![0-9A-Fa-f:-])")
NAMED = re.compile(r"\b(b?ssid|wi-?fi name|network name|serial(?: number)?|ro\.serialno)\b\s*[:=]\s*[\"'«]?[A-Za-z0-9_-]{3,}", re.I)
MAIL = re.compile(r"\b[A-Za-z0-9._%+-]+@(?!users\.noreply\.github\.com\b|example\.(?:com|org)\b)[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}\b")
OK_IP = re.compile(r"^(0\.0\.0\.0|127\.0\.0\.1|192\.0\.2\.\d+|198\.51\.100\.\d+|203\.0\.113\.\d+)$")
TEXT = (".md", ".txt", ".tsv", ".csv", ".json", ".html", ".log", ".yml", ".yaml", "")


def scan(path, text):
    out = []
    for n, ln in enumerate(text.splitlines(), 1):
        for m in IP.finditer(ln):
            if all(int(x) <= 255 for x in m.groups()) and not OK_IP.match(m.group(0)):
                out.append(f"{path}:{n}: an IP address")
        if MAC.search(ln):
            out.append(f"{path}:{n}: a MAC/BSSID")
        if MAIL.search(ln):
            out.append(f"{path}:{n}: an e-mail address")
        if NAMED.search(ln):
            out.append(f"{path}:{n}: {NAMED.search(ln).group(1)} with a value")
    return out


def main():
    failed = 0
    files = subprocess.run(["git", "ls-files", "tester", "docs"], cwd=ROOT, capture_output=True, text=True, check=True).stdout.split("\n")
    files = [f for f in files if f and Path(f).suffix.lower() in TEXT and (ROOT / f).is_file()]
    hits = []
    for f in files:
        try:
            hits += scan(f, (ROOT / f).read_text(encoding="utf-8"))
        except UnicodeDecodeError:
            continue
    print(("PASS" if not hits else "FAIL") + f" no IP, MAC/BSSID, SSID or serial in the {len(files)} tracked text files of tester/ and docs/"
          + ("" if not hits else "  -> " + "; ".join(hits[:20])))
    failed += bool(hits)
    probe = scan("probe", "TV at 192.168.1.23\nBSSID: a4:2b:b0:11:22:33\nSSID: HomeNet5G\nserial = ABC12345\nversion 4.3.4, build 4.3.460\nlocalhost 127.0.0.1\nmail someone@gmail.com\nby 1+tshi@users.noreply.github.com\n")
    ok = len(probe) == 5
    print(("PASS" if ok else "FAIL") + " the rule finds an IP, a BSSID, an SSID, a serial and an e-mail, and not a version number or a noreply address" + ("" if ok else f"  -> {probe}"))
    failed += not ok
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
