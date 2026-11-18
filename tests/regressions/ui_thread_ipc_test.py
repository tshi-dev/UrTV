#!/usr/bin/env python3
"""Regression gate R-UI (4.3.3/4.3.4: the TCL remote froze): no NEW cross-process call (a ContentResolver query/call/insert, ConfigSnapshot.read,
UpdateChannel.read) and no NEW disk work (streams, Files, prefs commit(), decodeFile) on a path the UI thread takes for a key, a click, onCreate, a
return to the screen (onResume/onStart), a receiver of a screen (the minute tick) — in every app under android/ (the home, the
screensaver app, the rescue app, and the extras when they come). The call graph is tests/regressions/javagraph.py (code handed to a thread or an executor is not counted; what it posts back is).

What is on such a path today was there at 4.3.4 and is listed, with a reason, in tests/regressions/ui_thread_baseline.txt; the test fails on
anything not in that list. A new line needs a reviewer's reason (docs/dev/RELEASE.md), never a bare copy of the printed line: move the work off
the UI thread instead whenever a key or the minute tick waits on it.

Run: python3 tests/regressions/ui_thread_ipc_test.py            (the check)
     python3 tests/regressions/ui_thread_ipc_test.py --list     (every path found, one line each, with its route)"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import javagraph  # noqa: E402

BASE = Path(__file__).resolve().parent / "ui_thread_baseline.txt"


def found(root=None):
    """{(kind, "app:Entry.method", sink): route} over both apps"""
    out = {}
    for app in javagraph.SRC:   # each app on its own: two have a MainActivity
        for e, kind, route in javagraph.reach(javagraph.load((app,), root)):
            sink = route.split(" -> ")[-1]
            key = (kind, f"{app}:{e.key}", sink)
            if key not in out or len(route) < len(out[key]):
                out[key] = route
    return out


def baseline():
    out = {}
    for ln in BASE.read_text(encoding="utf-8").splitlines():
        if not ln.strip() or ln.startswith("#"):
            continue
        parts = [p.strip() for p in ln.split("|")]
        out[tuple(parts[:3])] = parts[3] if len(parts) > 3 else ""
    return out


def main():
    now = found()
    if "--list" in sys.argv:
        for k in sorted(now):
            print(" | ".join(k) + " | " + now[k])
        return 0
    base = baseline()
    failed = 0
    new = sorted(k for k in now if k not in base)
    no_reason = sorted(k for k, r in base.items() if not r)
    gone = sorted(k for k in base if k not in now)
    ipc = [k for k in new if k[0].startswith("ipc:")]
    disk = [k for k in new if k[0].startswith("disk:")]
    for k in ipc:
        print("FAIL new cross-process call on the UI thread: " + " | ".join(k) + "\n     route: " + now[k])
    for k in disk:
        print("FAIL new disk work on the UI thread: " + " | ".join(k) + "\n     route: " + now[k])
    for k in no_reason:
        print("FAIL a baseline line with no reason: " + " | ".join(k))
    failed = len(new) + len(no_reason)
    for k in gone:
        print("NOTE no longer on the UI thread (remove its baseline line): " + " | ".join(k))
    print(f"{'PASS' if not new else 'FAIL'} UI-thread paths: {len(now)} found, {len(base)} in the baseline, {len(new)} new, {len(gone)} gone")
    # the rule must still see the two 4.3.4 freezes if they came back: a self-test on the graph itself
    probe = javagraph.load(("launcher",))
    names = {m.key for m in probe}
    ok = {"MainActivity.onResume", "MainActivity.dispatchKeyEvent", "ConfigSnapshot.read", "UpdateChannel.read"} <= names
    print(("PASS" if ok else "FAIL") + " the graph still finds the home's entries and the two provider reads")
    failed += not ok
    failed += selftest()
    return 1 if failed else 0


FIXTURE = """package x;
class A extends android.app.Activity {
    private B helper;
    // a string and a comment never count: "getContentResolver().query(" // ConfigSnapshot.read(this)
    @Override protected void onResume() { super.onResume(); helper.load(this); later(); }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent e) { return UpdateChannel.read(this) != null; }
    private void later() {
        new Thread(() -> { ConfigSnapshot.read(this); runOnUiThread(() -> shown()); }).start();
        pool.execute(new Runnable() { public void run() { getContentResolver().query(null, null, null, null, null); } });
    }
    private void shown() { getSharedPreferences("p", 0).edit().putInt("a", 1).commit(); }
}
"""
FIXTURE_B = """package x;
class B {
    void load(android.content.Context c) { try (android.database.Cursor q = c.getContentResolver().query(null, null, null, null, null)) { } }
}
"""


def selftest():
    """the rule on a made-up screen: what it must find, and what runs on another thread that it must not"""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        d = Path(tmp) / javagraph.SRC["launcher"].relative_to(javagraph.ROOT) / "x"
        d.mkdir(parents=True)
        (d / "A.java").write_text(FIXTURE, encoding="utf-8")
        (d / "B.java").write_text(FIXTURE_B, encoding="utf-8")
        got = {(k, e.key, r.split(" -> ")[-1]) for e, k, r in javagraph.reach(javagraph.load(("launcher",), tmp))}
    want = {("ipc:ContentResolver.query", "A.onResume", "B.load"),           # through a field of our own type
            ("ipc:UpdateChannel.read", "A.dispatchKeyEvent", "A.dispatchKeyEvent"),
            ("disk:prefs.commit", "A.onResume", "A.shown")}                    # posted back to the UI thread from a worker
    must_not = {("ipc:ConfigSnapshot.read", "A.onResume", "A.later"), ("ipc:ContentResolver.query", "A.onResume", "A.later")}
    bad = (want - got) | (got & must_not)
    print(("PASS" if not bad else "FAIL") + " self-test on a made-up screen" + ("" if not bad else f"  -> missing {want - got}, wrong {got & must_not}"))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
