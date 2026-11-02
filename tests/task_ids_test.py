#!/usr/bin/env python3
"""«المهام»: a task is named by its own id, not by its text or place, and the changes are one at a time (F24). The list logic runs for
real (TaskList.java on tests/TaskListRun.java); the wiring is read from the source. Needs javac; without it that part is skipped.
Run: python3 tests/task_ids_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

text = lambda p: p.read_text(encoding="utf-8")
prefs, prov = text(APP / "Prefs.java"), text(APP / "ConfigProvider.java")
tasks_ui = text(ROOT / "android/launcher/src/main/java/sa/urtv/home/TasksActivity.java")
check("Prefs: every change to the list holds the one lock, so two cannot interleave",
      prefs.count("synchronized (TASKS_LOCK)") >= 7 and "private static final Object TASKS_LOCK" in prefs)
check("Prefs: an old list gets ids when it is read, and the ids are saved", "TaskList.needsIds(v)" in prefs and '.put("id", t.id)' in prefs)
check("the provider lists the id and changes by it", '"at", "id"' in prov and 'getAsLong("id")' in prov and 'p.deleteTaskId(' in prov and 'p.doneTaskId(' in prov)
check("the provider still takes a number (an older launcher)", "p.doneTask(v.getAsInteger" in prov and "deleteTask(Integer.parseInt" in prov)
check("the screen acts on the id of the row, not on its text", "delete(t, id)" in tasks_ui and "markDone(t, id)" in tasks_ui and "ids.add(" in tasks_ui)

if not shutil.which("javac"):
    print("SKIP task list: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "Task.java"), str(APP / "TaskList.java"), str(APP / "Reminders.java"), str(ROOT / "tests/TaskListRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.TaskListRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t") for l in out.split("\n") if l]
for name, res in rows:
    check("task list: " + name, res == "ok")
check("every check ran", len(rows) >= 10, len(rows))
sys.exit(1 if failed else 0)
