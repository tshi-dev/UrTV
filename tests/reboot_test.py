"""optimize/tv.py --reboot: the TV only counts as back from a new boot, however slowly it goes down.
A fake TV on a fake clock; the reboot command must be sent exactly once.

    python3 tests/reboot_test.py          (no TV needed)
"""
import contextlib, io, os, sys, types
OTHER = ".".join(["192", "168", "1", "77"])   # another device on the network (written in parts: the repo keeps no addresses in tests)

clock = {"t": 0.0}
tvstate = {}

class Dev:
    """The TV: answers `before_down` seconds after the reboot command, is gone until `up_at`, then boots."""
    def __init__(self, *a, **k): pass
    def connect(self, **k):
        if tvstate["state"]() == "down":
            raise ConnectionRefusedError("no route")
    def close(self): pass
    def shell(self, cmd, read_timeout_s=0):
        st = tvstate["state"]()
        if st == "down":
            raise ConnectionResetError("gone")
        if cmd == "reboot":
            tvstate["sent"] += 1
            tvstate["sent_at"] = clock["t"]
            return ""
        if cmd.startswith('echo "serial='):   # who is answering: the same TV before and after the restart
            return "serial=ABC123\nbootserial=\nmanufacturer=TCL\nmodel=55P735\n"
        new = st in ("booting", "new")
        if cmd == "getprop sys.boot_completed":
            return "1\n" if st in ("old", "new") else "0\n"
        if cmd == "cat /proc/sys/kernel/random/boot_id":
            if not tvstate["has_id"]:
                return "cat: No such file\n"
            return ("new-" if new else "old-") + "0123456789abcdef0123456789abcdef\n"
        if cmd.startswith("dumpsys power"):   # awake after its restart (a TV left in standby: tests/standby_wake_test.py)
            return "  mWakefulness=Awake\n"
        if cmd == "cat /proc/uptime":
            return f"{(clock['t'] - tvstate['sent_at'] - tvstate['up_at'] + tvstate['boot_age']) if new else clock['t'] + tvstate['old_up']:.2f} 100.0\n"
        raise SystemExit("fake: unknown command " + cmd)

for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = Dev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "1.2.3.4"
import tempfile
os.environ["URTV_RUN_DIR"] = tempfile.mkdtemp()   # where the TV's identity is remembered
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "optimize"))
import tv
tv.signer = lambda: None
hints = {"n": 0}
def fake_hint(host=None):   # find-tv.py's look at the Wi-Fi: one other device has ADB open (it is only printed, never used)
    hints["n"] += 1
    return "   (the saved address does not answer, but one device on this Wi-Fi has ADB open at " + OTHER + ": optimize/find-tv.sh use " + OTHER + ")"
tv.address_hint = fake_hint
tv.time = types.SimpleNamespace(time=lambda: clock["t"], sleep=lambda s: clock.__setitem__("t", clock["t"] + s))

def scenario(label, down_at, up_at, ready_at, has_id=True, old_up=5000.0, ignored=False, want="back", min_wait=0):
    """After the reboot command at t0: old boot until down_at, no answer until up_at, new boot (still booting) until
    ready_at. ignored: the TV never restarts."""
    clock["t"] = 1000.0
    tvstate.update(sent=0, has_id=has_id, up_at=up_at, old_up=old_up - clock["t"], boot_age=0.0, sent_at=None)
    def state():
        if tvstate["sent"] == 0 or ignored:
            return "old"
        dt = clock["t"] - tvstate["sent_at"]
        return "old" if dt < down_at else "down" if dt < up_at else "booting" if dt < ready_at else "new"
    tvstate["state"] = state
    sys.argv = ["tv.py", "--reboot"]
    out = io.StringIO()
    code = 0
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        try:
            tv.main()
        except SystemExit as e:
            code = 1 if e.code else 0
            print(e.code)
    waited = clock["t"] - 1000.0
    text = out.getvalue()
    ok = tvstate["sent"] == 1
    if want == "back":
        ok = ok and code == 0 and "TV is back" in text and waited >= max(min_wait, ready_at)
    elif want == "never restarted":
        ok = ok and code == 1 and "never restarted" in text
    else:
        ok = ok and code == 1 and "did not come back" in text
    print(f"reboot: {label}: " + ("ok" if ok else f"WRONG sent={tvstate['sent']} code={code} waited={waited:.0f}s {text.strip()[-120:]}"))
    return ok

results = [
    scenario("goes down within seconds, new boot", down_at=5, up_at=50, ready_at=90),
    # the Sony report: it still answers when the 45 s wait is over, and is only back much later
    scenario("still answering after 45 s, then restarts", down_at=70, up_at=100, ready_at=130, min_wait=130),
    scenario("same, no boot id: uptime, old one longer", down_at=70, up_at=100, ready_at=130, has_id=False),
    scenario("same, no boot id, a TV up only 30 s before", down_at=70, up_at=100, ready_at=130, has_id=False, old_up=30.0),
    scenario("the reboot is ignored: never counts as back", down_at=0, up_at=0, ready_at=0, ignored=True, want="never restarted"),
    scenario("never answers again", down_at=5, up_at=10_000, ready_at=10_001, want="did not come back"),
    # F: a TV that takes 8 minutes to be back is found (it used to be given up on after 5)
    scenario("back after 8 minutes", down_at=5, up_at=470, ready_at=480, min_wait=480),
]
hints["n"] = 0
clock["t"] = 1000.0
ok10 = scenario("never answers: the line says 10 minutes", down_at=5, up_at=10_000, ready_at=10_001, want="did not come back")
tx = io.StringIO()
tvstate.update(sent=0, state=lambda: "down")
clock["t"] = 0.0
try:
    with contextlib.redirect_stderr(tx):
        tv.wait_for_tv(restart=False)
except SystemExit as e:
    line = str(e.code)
print("reboot: failure line and wait: " + ("ok" if line.startswith("!! The TV did not come back within 10 minutes.") and 600 <= clock["t"] <= 620
      and OTHER in line and OTHER in tx.getvalue() else f"WRONG {line!r} t={clock['t']}"))
results.append(ok10 and line.startswith("!! The TV did not come back within 10 minutes.") and 600 <= clock["t"] <= 620 and OTHER in line)
# the hint is looked for about once a minute while waiting, and the host is never changed by it
results.append(hints["n"] >= 9 and tv.HOST == "1.2.3.4")
print("reboot: hint looked for once a minute, saved host unchanged: " + ("ok" if results[-1] else f"WRONG {hints['n']} {tv.HOST}"))

# unreachable before anything is sent: stops, nothing sent, nothing waited for
class Dead(Dev):
    def connect(self, **k): raise ConnectionRefusedError("off")
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = tv.AdbDeviceTcp = Dead
tvstate.update(sent=0, state=lambda: "down")
sys.argv = ["tv.py", "--reboot"]
buf = io.StringIO()
try:
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        tv.main()
    dead = False
except SystemExit as e:
    dead = tvstate["sent"] == 0 and "Nothing was sent" in str(e.code)
print("reboot: unreachable TV: " + ("stops, nothing sent" if dead else "WRONG"))
results.append(dead)
sys.exit(0 if all(results) else 1)
