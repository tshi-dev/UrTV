#!/usr/bin/env python3
"""4.3.4, the release gate's findings fixed before the release (docs/dev/REGRESSIONS.md on work/qa-gate, «QA-n» and the review's F1/F2):
- QA-1: a home made again (recreate, a language change) draws its first clock from the answer the process already has
  (ConfigSnapshot.known, never a query) and reads the screensaver's settings off the UI thread; only a process's very first draw asks;
- QA-3: the reminder card reads the tasks, the digits and the city's zone (cross-process) on a thread of its own, then shows on the UI thread;
  the return to the home's alarm reschedule and a press on the card (the task written, the alarm) are off the UI thread too;
- QA-5: «ترتيب المفضلة» while an app is moved keeps only the keys the move uses (FavEdit.owns); a remote's other keys go on;
- F1: a settings ask for «التطبيقات المخفية» that a PIN or kids mode stopped is forgotten by the next long-press «انقل»;
- F2: the Menu key over an edit mode no longer opens «المصادر».
Run: python3 tests/qa_434_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return (H / p).read_text(encoding="utf-8")
def body(src, head):
    if head not in src: return ""
    i = src.index(head) + len(head) - 1
    d = 0
    for j in range(i, len(src)):
        d += {"{": 1, "}": -1}.get(src[j], 0)
        if d == 0: return src[i:j + 1]
    return ""

main, card, dock, cs, lg = text("MainActivity.java"), text("ReminderCard.java"), text("DockActivity.java"), text("ConfigSnapshot.java"), text("LastGood.java")

uc = body(main, "private void updateClock() {")
first = body(uc, "if (!clockFirstDone) {")
check("QA-1: the first draw takes ConfigSnapshot.known() and reads only when the process has no answer yet",
      "ConfigSnapshot known = ConfigSnapshot.known();" in first and "updateClock(known != null ? known : ConfigSnapshot.ui(this));" in first
      and "ConfigSnapshot.read(this)" not in first, first)
check("QA-1: …then goes on to the background read (no return after drawing the known answer)", "updateClock(known != null ? known : ConfigSnapshot.ui(this));\n        }" in first and "return;" not in first
      and 'new Thread(' in uc and '"home-config").start();' in uc)
kn = body(cs, "static ConfigSnapshot known() {")
check("QA-1: ConfigSnapshot.known() is memory only (LastGood.peek), no content resolver", "LAST.peek()" in kn and "query" not in kn and "getContentResolver" not in kn, kn)
check("QA-1: LastGood.peek() hands out the last good value and asks nothing", "public synchronized T peek() { return last; }" in lg)

ref = body(card, "void refresh(boolean reschedule) {")
thread = body(ref, "new Thread(() -> {")
outside = ref.replace(thread, "")
check("QA-3: refresh reads the tasks, the digits and the zone inside its own thread", all(x in thread for x in ("ReminderStore.read(app)", "Digits.init(app)", "zone(app)")), thread[:300])
check("QA-3: …and nothing cross-process outside it", not re.search(r"ReminderStore\.read|ConfigSnapshot\.read|Digits\.init|getContentResolver", outside), outside)
check("QA-3: the card is shown on the UI thread (card.post), and a refresh asked meanwhile reads once more", "card.post(() -> {" in thread and "readAgain" in ref
      and "if (readAgain) { readAgain = false; refresh(); }" in thread)
show = body(card, "private void show(List<ReminderPlan.Row> rows, TimeZone zone) {")
check("QA-3: the return to the home asks the alarm's reschedule of the card's thread, not of the UI thread",
      "if (reminder != null) reminder.refresh(true);" in main and "ReminderScheduler.schedule(this); }   // a card" not in main
      and "if (reschedule) ReminderScheduler.scheduleSoon();" in ref and "if (ReminderScheduler.takeSoon()) ReminderScheduler.schedule(app);" in thread)
pr = body(card, "private void press(final int which) {")
prt = body(pr, "new Thread(() -> {")
check("QA-3: OK on the card writes the task, sets the alarm and reads the next card off the UI thread, in that order",
      prt and prt.index("ReminderStore.done(app, id)") < prt.index("ReminderScheduler.schedule(app);") < prt.index("card.post(this::refresh);")
      and not re.search(r"ReminderStore\.|ReminderScheduler\.schedule", pr.replace(prt, "")), pr)
check("QA-3: show() asks nothing (the read's result only)", show and not re.search(r"ReminderStore\.read|ConfigSnapshot\.read|Digits\.init|zone\(\)", show), show[:200])
check("QA-3: the card's chime still once per card, and the topmost view (tests/reminders_test.py)", "row.sound && chimed.add(" in show)

dk = body(dock, "public boolean dispatchKeyEvent(KeyEvent e) {")
check("QA-5: while moving, a key the move does not use goes on (FavEdit.owns), before the mode takes the rest",
      "if (!FavEdit.owns(k)) return super.dispatchKeyEvent(e);" in dk
      and dk.index("if (moving < 0)") < dk.index("FavEdit.owns(k)") < dk.index("ACTION_DOWN) return true;"), dk[:400])

be = body(main, "private void beginAppsEdit(String pkg) {")
check("F1: a long-press «انقل» (pkg) forgets a hidden-list ask that a PIN or kids mode stopped, before anything can return",
      be.strip("{} \n").startswith("if (pkg != null) wantHiddenList = false;"), be[:200])
menu = [ln for ln in main.splitlines() if "KEYCODE_MENU" in ln and "ACTION_UP" in ln and "showInputs" not in ln]
inputs = next((ln for ln in main.splitlines() if "e.getKeyCode() == KeyEvent.KEYCODE_MENU" in ln and "inputsAnchor() != null" in ln), "")
check("F2: the Menu key opens «المصادر» only with no edit mode on", "edit == null && appsEdit == null" in inputs, inputs)

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        run = Path(tmp) / "PeekRun.java"
        run.write_text("package sa.urtv.home;\npublic class PeekRun { public static void main(String[] a) {\n"
                       "  LastGood<String> g = new LastGood<>(); final int[] asked = {0};\n"
                       "  System.out.println(g.peek() == null);\n"
                       "  g.read(1000, 3000, () -> { asked[0]++; return \"x\"; });\n"
                       "  System.out.println(\"x\".equals(g.peek()) && asked[0] == 1);\n"
                       "  g.read(9000, 3000, () -> { asked[0]++; return null; });\n"
                       "  System.out.println(\"x\".equals(g.peek()) && asked[0] == 2);\n"
                       "  for (int i = 0; i < 5; i++) g.peek();\n  System.out.println(asked[0] == 2);\n} }\n", encoding="utf-8")
        subprocess.run(["javac", "-d", tmp, str(H / "LastGood.java"), str(run)], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.PeekRun"], check=True, capture_output=True, text=True).stdout.split()
    check("QA-1 on a JVM: peek is null before any answer, the last good after one, kept through a failed read, and never asks", out == ["true"] * 4, out)
else:
    print("SKIP the JVM run: no javac")
sys.exit(1 if failed else 0)
