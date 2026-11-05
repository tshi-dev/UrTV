#!/usr/bin/env python3
"""«محفظتي» and the phone (the 4.2 review, R02 and R03): the pairing screen's «portfolio» extra is never trusted (with «اقفل محفظتي»
the PIN is asked there, cancelled = the plain phone view, an already paired phone gets nothing more), only our own apps may open that
screen (a signature permission), and a paired phone cannot renew its own window: only the TV does, on a monotonic clock, and locking,
wiping or ending a session closes it. Needs javac for the window's logic.   Run: python3 tests/portfolio_window_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

pa, pf, pp = text(HOME / "PhoneActivity.java"), text(HOME / "Portfolio.java"), text(HOME / "PortfolioPhone.java")
ctl, acts, mk = text(HOME / "phone/PhoneControl.java"), text(HOME / "phone/PhoneActions.java"), text(HOME / "MarketsActivity.java")
man, saver = text(ROOT / "android/launcher/src/main/AndroidManifest.xml"), text(ROOT / "android/app/src/main/AndroidManifest.xml")

# R02: the extra is a wish
resume = pa.split("protected void onResume() {")[1].split("\n    }\n")[0]
check("R02: onResume never opens the window on the extra; it asks authorizePortfolio", "openEditing" not in resume and "if (portfolio) authorizePortfolio();" in resume)
auth = pa.split("private void authorizePortfolio() {")[1].split("\n    }\n")[0]
check("R02: with «اقفل محفظتي» and a PIN, the PIN first (Parental.confirm), the window opened only on its OK",
      "Portfolio.locked(this)" in auth and "Parental.state(this).pinSet" in auth and "Parental.confirm(this" in auth
      and auth.index("Parental.confirm(") < auth.index("Portfolio.openEditing(", auth.index("Parental.confirm(")))
cancel = auth.split("}, () -> {")[1]
check("R02: a cancelled PIN: the plain phone view, nothing opened", "portfolio = false;" in cancel and "openEditing" not in cancel)
check("R02: the portfolio's address and QR (/m) only while the window is open, never on the extra alone",
      "private boolean portfolioShown() { return portfolio && Portfolio.editing(Portfolio.now()); }" in pa
      and 'portfolio ? "/m"' not in pa and "portfolio ? control.pairUrl" not in pa and pa.count("portfolioShown() ?") == 2)
check("R02: an existing paired session gets nothing from the extra: the phone bridge still asks the window on each read and write",
      pp.count("if (!Portfolio.editing(now))") == 2)
check("R02: the keypad leaves with the screen (asked again on the way back)", "PinPrompt.dismiss(this);" in pa.split("protected void onPause() {")[1].split("\n    }\n")[0])
check("R02: only our two apps (same key) may open the pairing screen from outside: a signature permission, the screensaver asks for it",
      re.search(r'<permission\s+android:name="sa\.urtv\.home\.OPEN_PHONE"\s+android:protectionLevel="signature"', man) is not None
      and re.search(r'android:name="\.PhoneActivity"\s+android:exported="true"\s+android:permission="sa\.urtv\.home\.OPEN_PHONE"', man) is not None
      and '<uses-permission android:name="sa.urtv.home.OPEN_PHONE" />' in saver)
# R03: only the TV renews it
check("R03: a phone's save renews nothing", "Portfolio.openEditing(" not in pp)
check("R03: the only callers of openEditing are the TV's screens", sorted({p.name for p in HOME.rglob("*.java") if "Portfolio.openEditing(" in text(p)}) == ["MarketsActivity.java", "PhoneActivity.java"])
check("R03: the window runs on SystemClock.elapsedRealtime, everywhere", "SystemClock.elapsedRealtime()" in pf
      and "PortfolioPhone.read(app, sa.urtv.home.PortfolioPhone.now())" in acts and "PortfolioPhone.save(app, sa.urtv.home.PortfolioPhone.now()," in acts
      and not re.search(r"(openEditing|editing)\(System\.currentTimeMillis", mk + pa + acts + pf))
check("R03: «اقفل محفظتي» closes it", "if (on) closeEditing();" in pf.split("static void setLocked(")[1].split("\n    }\n")[0])
check("R03: a wipe closes it", "closeEditing();" in pf.split("static synchronized void wipe(")[1].split("\n    }\n")[0])
check("R03: a phone revoked, logged out or locked out, or control stopped, closes it", "control.setOnSessionEnd(Portfolio::closeEditing);" in pa
      and ctl.count("sessionEnded();") == 4)
check("R03: with «اقفل محفظتي» the open tab renews it only after the PIN on that screen", "(unlocked || !Portfolio.locked(MarketsActivity.this)" in mk)

if not shutil.which("javac"):
    print("SKIP portfolio window: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "PortfolioWindow.java"), str(ROOT / "tests/PortfolioWindowRun.java")], check=True)
    got = dict(l.split("=", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.PortfolioWindowRun"], check=True, capture_output=True, text=True).stdout.splitlines())
want = {"closed_at_start": "false", "open_now": "true", "before_end": "true", "read_after_end": "false", "read_long_after": "false",
        "tv_renews": "true", "clock_back": "false", "clock_forward": "false", "closed": "false",
        "entry_open_window": "0", "entry_locked": "1", "entry_locked_no_pin": "2", "entry_unlocked": "2"}
for k, v in want.items():
    check(f"window: {k} = {v}", got.get(k) == v, got.get(k))
print("\n" + (f"{failed} failed" if failed else "portfolio window: all passed"))
sys.exit(1 if failed else 0)
