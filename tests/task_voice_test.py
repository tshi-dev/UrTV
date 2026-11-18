#!/usr/bin/env python3
"""«المهام» by voice: what the home app (TaskVoice.java) understands, on tests/task_voice_cases.tsv.
Needs javac; without it, skipped with a note.   Run: python3 tests/task_voice_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP task voice: no javac here")
    sys.exit(0)
cases = [l.split("\t") for l in (ROOT / "tests/task_voice_cases.tsv").read_text(encoding="utf-8").split("\n")
         if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                    str(ROOT / "android/launcher/src/main/java/sa/urtv/home/TaskVoice.java"),
                    str(ROOT / "tests/TaskVoiceRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.TaskVoiceRun",
                          str(ROOT / "tests/task_voice_cases.tsv")], check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
failed = 0
for c, g in zip(cases, got):
    want = "\t".join(c[2:])
    ok = g == want
    print(("PASS " if ok else "FAIL ") + f"{c[0]!r} ({c[1]!r})" + ("" if ok else f"  -> {g!r}, wanted {want!r}"))
    failed += not ok
if len(got) < len(cases):
    print("FAIL fewer answers than cases")
    failed += 1
act = (ROOT / "android/launcher/src/main/java/sa/urtv/home/TasksActivity.java").read_text(encoding="utf-8")
wired = "TaskVoice.spoken(heard.get(0), texts, dones, MAX_CHARS)" in act and "SearchManager.QUERY" in act and 'ask(S.t("أضيف «%s»؟", t), () -> add(t));' in act
print(("PASS " if wired else "FAIL ") + "#48: «تكلّم» asks to add plain words, and reads a recognizer's answer given as a search query")
failed += not wired
sys.exit(1 if failed else 0)
