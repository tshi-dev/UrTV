"""optimize/tv.py: a TV that comes back from a restart in standby is woken (WAKEUP), and the person is asked to press Power (R-371).

The setup gave up after a restart on TVs that answered ADB with the screen still in standby (the steps after it found no
screen). wait_for_tv now reads mWakefulness when the TV is back, sends KEYCODE_WAKEUP while it sleeps, asks once (in Arabic)
for the Power button, and says so plainly if the screen stays asleep. A fake TV on a fake clock; no TV needed.

    python3 tests/standby_wake_test.py
"""
import contextlib, io, os, sys, tempfile, types
clock = {"t": 0.0}
st = {}

class Dev:
    def __init__(self, *a, **k): pass
    def connect(self, **k): pass
    def close(self): pass
    def shell(self, cmd, read_timeout_s=0):
        st["cmds"].append(cmd)
        if cmd == "getprop sys.boot_completed":
            return "1\n"
        if cmd.startswith("dumpsys power"):
            awake = clock["t"] >= st["awake_at"] or (st["wakeup_works"] and st["wakeups"] > 0)
            return f"  mWakefulness={'Awake' if awake else st['sleep_word']}\n"
        if cmd == "input keyevent KEYCODE_WAKEUP":
            st["wakeups"] += 1
            return ""
        raise SystemExit("fake: unknown command " + cmd)

for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = Dev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"   # a documentation address: nothing is reached
os.environ["URTV_RUN_DIR"] = tempfile.mkdtemp()
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "optimize"))
import tv
tv.signer = lambda: None
tv.address_hint = lambda host=None: ""
tv.new_dev = lambda timeout: Dev()
tv.time = types.SimpleNamespace(time=lambda: clock["t"], sleep=lambda s: clock.__setitem__("t", clock["t"] + s))

bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

def wait(awake_at, wakeup_works, sleep_word="Asleep"):
    clock["t"] = 0.0
    st.update(cmds=[], wakeups=0, awake_at=awake_at, wakeup_works=wakeup_works, sleep_word=sleep_word)
    out, code = io.StringIO(), 0
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        try:
            tv.wait_for_tv()
        except SystemExit as e:
            code = 1
            print(e.code)
    return code, out.getvalue()

code, out = wait(awake_at=0, wakeup_works=False)
check("an awake TV: back at once, no WAKEUP, no question", code == 0 and "TV is back" in out and st["wakeups"] == 0 and "اضغط" not in out, out)
code, out = wait(awake_at=10 ** 9, wakeup_works=True)
check("in standby: WAKEUP wakes it, back, no question", code == 0 and "TV is back" in out and st["wakeups"] == 1 and "اضغط" not in out, out)
code, out = wait(awake_at=10 ** 9, wakeup_works=True, sleep_word="Dozing")
check("Dozing counts as standby too", code == 0 and st["wakeups"] == 1, out)
code, out = wait(awake_at=60, wakeup_works=False)
check("WAKEUP does nothing: the person is asked once to press Power (in Arabic), and the setup goes on when they do",
      code == 0 and "TV is back" in out and out.count("اضغط زر التشغيل (Power)") == 1 and st["wakeups"] >= 2, out)
code, out = wait(awake_at=10 ** 9, wakeup_works=False)
check("the screen never wakes: a !! line that says standby and Power, not «did not come back»",
      code == 1 and "screen stayed in standby" in out and "did not come back" not in out, out)
sys.exit(1 if bad else 0)
