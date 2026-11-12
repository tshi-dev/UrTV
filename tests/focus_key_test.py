#!/usr/bin/env python3
"""The focus stays on the row that was just changed, on every settings screen (the owner's report: «the page goes to the first button»).
The screensaver's settings rebuild their whole list on each change (SettingsActivity.render): FocusKey (run for real, tests/FocusKeyRun.java)
picks the row back, and render wires it (read from the source). The launcher's screens rebuild with the row they were on: checked
from the source too. Needs javac for the JVM part.   Run: python3 tests/focus_key_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
render = st[st.index("    private void render() {"):]
render = render[:render.index("\n    }\n")]
check("settings: render reads the focused row, its place and the scroll before it empties the list (unless a screen was just opened)",
      render.index("View had = entering ? null : focusedRow();") < render.index("list.removeAllViews();") and "scroll.getScrollY()" in render.split("list.removeAllViews();")[0])
check("settings: only a screen just opened starts at its top (its first row; on a screen of buttons the one last opened), no other focus in render",
      "final String want = forceWant != null ? forceWant : returnWant != null && !entering ? returnWant : entering ? (tiles ? (hubAt != null ? \"hub:\" + hubAt : null) : lastRow.get(screen))" in render and ").requestFocus();" not in render
      and "int at = want == null ? (now.isEmpty() ? -1 : 0) : FocusKey.pick(keys, want, wasIndex);" in st)
check("settings: render ends by putting the focus back (restoreFocus), the scroll first, after the layout (post)",
      render.rstrip().endswith("restoreFocus(want, wasIndex, scrollY);   // a screen just opened: its first row") and "scroll.post(() -> {" in st
      and st.index("scroll.scrollTo(0, scrollY);") < st.index("now.get(at).requestFocus();"))
check("settings: every row is known by its FocusKey", "card.setTag(FocusKey.key(label));" in st)
check("settings: a rebuild under an open dialog is finished when the window has the focus back",
      "public void onWindowFocusChanged(boolean has)" in st and "if (hasWindowFocus()) pendingFocus = null;" in st)
check("settings: no other way to rebuild the list (recreate, a second setContentView, removeAllViews elsewhere)",
      "recreate()" not in st and st.count("setContentView(") == 1 and st.count("list.removeAllViews()") == 1)
calls = len(re.findall(r"\brender\(\)", st))
check(f"settings: every change goes through render ({calls} calls), so each keeps its row", calls > 30, calls)

# the launcher's screens: each rebuild names the row it lands on
tl = (HOME / "tilawah/MainActivity.java").read_text(encoding="utf-8")
for kind in ("KIND_ADHAN_TOGGLE", "KIND_SHORT", "KIND_STOPKEY"):
    seg = tl[tl.index(f"row.kind == {kind})"):]
    seg = seg[:seg.index("} else")]
    check(f"«تلاوة وأذان»: {kind} rebuilds and stays on its row", "adapter.rebuild();" in seg and "focusList(list.getSelectedItemPosition());" in seg, seg)
check("«تلاوة وأذان»: the volume and back from the muezzin screens keep the row",
      "prefs.setAdhanVolume(now);\n        adapter.rebuild();\n        focusList(list.getSelectedItemPosition());" in tl and "target = list.getSelectedItemPosition();" in tl)
for name in ("DockActivity", "TasksActivity", "ParentalActivity"):
    src = (HOME / f"{name}.java").read_text(encoding="utf-8")
    check(f"{name}: its list is rebuilt with the row to focus, and focused after the layout", re.search(r"private void render\(int focus", src) is not None
          and "f.post(f::requestFocus)" in src)

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "FocusKey.java"), str(ROOT / "tests/FocusKeyRun.java")], check=True, capture_output=True)
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.FocusKeyRun"], capture_output=True)
    out = run.stdout.decode("utf-8")
    print(out, end="")
    if run.returncode != 0 or "FAIL" in out:
        failed += 1
else:
    print("SKIP focus key JVM part: no javac here")
sys.exit(1 if failed else 0)
