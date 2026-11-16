#!/usr/bin/env python3
"""Wireless debugging (Android 11+): the mDNS lookup, adb pair/connect, the remembered TV, the reconnect on a new port after
a restart, and find-tv's wider scan. Everything is faked (a fake adb program, fake mDNS packets, fake sockets); what only a
real TV can confirm is the pairing handshake itself and what a given router lets through (multicast).

    python3 tests/wireless_test.py
"""
import importlib.util, io, json, os, socket, struct, subprocess, sys, tempfile, types
from contextlib import redirect_stdout, redirect_stderr
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "optimize"))
tmp = tempfile.mkdtemp()
os.environ["URTV_HOME"] = tmp
import wireless as w   # noqa: E402
spec = importlib.util.spec_from_file_location("find_tv", ROOT / "optimize/find-tv.py")
ft = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ft)
ft.HOST_FILE = os.path.join(tmp, "tv_host")
real_load_state = w.load_state

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

# ---- building DNS packets like a TV's responder (with name compression)
def enc(name, table=None, at=0):
    out = b""
    labels = name.split(".")
    for i in range(len(labels)):
        rest = ".".join(labels[i:])
        if table is not None and rest in table:
            return out + struct.pack(">H", 0xC000 | table[rest])
        if table is not None:
            table[rest] = at + len(out)
        out += bytes([len(labels[i])]) + labels[i].encode()
    return out + b"\0"

def response(records):
    """records: (name, type, rdata-spec). Names are compressed against earlier ones, as real responders do."""
    table, body, n = {}, b"", 0
    head = 12
    for name, rtype, val in records:
        off = head + len(body)
        nm = enc(name, table, off)
        if rtype == w.T_PTR:
            rd_off = off + len(nm) + 10
            rd = enc(val, table, rd_off)
        elif rtype == w.T_SRV:
            rd_off = off + len(nm) + 10
            rd = struct.pack(">HHH", 0, 0, val[0]) + enc(val[1], table, rd_off + 6)
        elif rtype == w.T_A:
            rd = socket.inet_aton(val)
        elif rtype == w.T_TXT:
            rd = b"".join(bytes([len(x)]) + x.encode() for x in val)
        else:
            rd = b""
        body += nm + struct.pack(">HHIH", rtype, 1, 120, len(rd)) + rd
        n += 1
    return struct.pack(">HHHHHH", 0, 0x8400, 0, n, 0, 0) + body

SVC = w.SERVICE_CONNECT
TV1 = "adb-ABC123-xYz"
def tv_answer(ip="192.168.1.20", port=41234, inst=TV1, host="Android-TV.local"):
    return response([(SVC, w.T_PTR, f"{inst}.{SVC}"), (f"{inst}.{SVC}", w.T_SRV, (port, host)),
                     (f"{inst}.{SVC}", w.T_TXT, ["v=1"]), (host, w.T_A, ip)])

class FakeNet:
    """send() records the question; recv() hands out the packets the 'TV' would answer, one per call, then nothing."""
    def __init__(self, answers): self.answers, self.sent, self.t = list(answers), [], 0.0
    def send(self, pkt): self.sent.append(pkt)
    def recv(self, timeout):
        self.t += min(timeout, 0.5)
        return self.answers.pop(0) if self.answers else None

clock_t = [0.0]
def fake_clock():
    clock_t[0] += 0.4
    return clock_t[0]

# ---- the packets
q = w.build_query([(SVC, w.T_PTR)])
check("query: header says one question, no answers", q[:12] == struct.pack(">HHHHHH", 0, 0, 1, 0, 0, 0))
check("query: the name is spelled in labels and asks PTR with the unicast bit",
      q[12:].startswith(b"\x10_adb-tls-connect\x04_tcp\x05local\x00") and q[-4:] == struct.pack(">HH", 12, 0x8001))
recs = w.parse_message(tv_answer())
check("parse: PTR, SRV, TXT and A come out through name compression",
      (SVC, w.T_PTR, f"{TV1}.{SVC}") in recs and (f"{TV1}.{SVC}", w.T_SRV, (41234, "Android-TV.local")) in recs
      and ("Android-TV.local", w.T_A, "192.168.1.20") in recs and (f"{TV1}.{SVC}", w.T_TXT, ["v=1"]) in recs, str(recs))
check("parse: a cut-off packet gives what was read, no exception", len(w.parse_message(tv_answer()[:70])) >= 0)
loop = struct.pack(">HHHHHH", 0, 0x8400, 0, 1, 0, 0) + b"\xc0\x0c" + struct.pack(">HHIH", 1, 1, 1, 4) + b"\1\2\3\4"
check("parse: a pointer that points at itself does not hang", w.parse_message(loop) == [])
check("parse: garbage gives nothing", w.parse_message(b"\x00\x01") == [] and w.parse_message(b"") == [])

# ---- browse
clock_t[0] = 0.0
hits = w.browse(SVC, 3.0, FakeNet([tv_answer()]), fake_clock)
check("browse: the TV's name, address and port", hits == [{"instance": TV1, "host": "192.168.1.20", "port": 41234}], str(hits))
clock_t[0] = 0.0
two = w.browse(SVC, 3.0, FakeNet([tv_answer(), tv_answer("192.168.1.31", 37000, "adb-BOX-qq", "Box.local")]), fake_clock)
check("browse: two devices both listed", len(two) == 2, str(two))
clock_t[0] = 0.0
class AsksFirst(FakeNet):   # the details come only when they are asked for by name
    def send(self, pkt):
        super().send(pkt)
        if w.T_SRV in [struct.unpack(">H", pkt[i:i + 2])[0] for i in range(12, len(pkt) - 3) if pkt[i - 1:i] == b"\0"]:
            self.answers.append(response([(f"{TV1}.{SVC}", w.T_SRV, (40000, "Tv.local")), ("Tv.local", w.T_A, "192.168.1.20")]))
net = AsksFirst([response([(SVC, w.T_PTR, f"{TV1}.{SVC}")])])   # an answer with no details: it asks for them
hits = w.browse(SVC, 3.0, net, fake_clock)
check("browse: an answer without SRV/A is followed up by name", hits and hits[0]["port"] == 40000 and len(net.sent) >= 3, str(hits))
clock_t[0] = 0.0
check("browse: silence (a router that blocks multicast) is an empty list", w.browse(SVC, 3.0, FakeNet([]), fake_clock) == [])
pick = w.pick_connect
two = [{"instance": "adb-BOX-qq", "host": "192.168.1.31", "port": 1}, {"instance": TV1, "host": "192.168.1.20", "port": 2}]
check("pick: by remembered name", pick(two, TV1, "")["port"] == 2)
check("pick: by remembered address when the name is not given", pick(two, "", "192.168.1.31")["port"] == 1)
check("pick: never a different device", pick(two, "adb-OTHER-zz", "192.168.1.99") is None)

# ---- adb pair / connect output
check("pair output: success with the guid", w.parse_pair_output("Successfully paired to 192.168.1.20:37105 [guid=adb-ABC123-xYz]\n") == {"ok": True, "guid": TV1, "reason": ""})
bad = w.parse_pair_output("Enter pairing code: Failed: Wrong password or connection was dropped.\n")
check("pair output: wrong code is said as such", not bad["ok"] and "code" in bad["reason"], str(bad))
check("pair output: refused means the dialog is closed", "dialog" in w.parse_pair_output("error: failed to connect to 192.168.1.20:37105: Connection refused")["reason"])
check("pair output: an adb that is too old", "too old" in w.parse_pair_output("error: unknown host service")["reason"])
check("connect output", w.parse_connect_output("connected to 192.168.1.20:41234") and w.parse_connect_output("already connected to 1.2.3.4:5")
      and not w.parse_connect_output("failed to connect to '1.2.3.4:5': Connection refused")
      and not w.parse_connect_output("cannot connect to 1.2.3.4:5: No connection could be made"))
check("hostport: valid and invalid", w.valid_hostport("192.168.1.20:37105") and not w.valid_hostport("192.168.1.20") and not w.valid_hostport("999.1.1.1:5")
      and not w.valid_hostport("a:1; rm") and not w.valid_hostport("1.2.3.4:99999"))

# ---- a fake adb program: pairing code 123456 at 192.168.1.20:37105, connection at a port that changes at every 'restart'
class Adb:
    def __init__(self): self.port, self.paired, self.log, self.hand_out = 41234, False, [], True
    def run(self, argv, **kw):
        self.log.append(argv[1:])
        a = argv[1:]
        def res(out, rc=0): return subprocess.CompletedProcess(argv, rc, out, "")
        if a[0] == "pair":
            if a[1] == "192.168.1.20:37105" and a[2] == "123456":
                self.paired = True
                return res("Successfully paired to 192.168.1.20:37105 [guid=adb-ABC123-xYz]\n")
            return res("Failed: Wrong password or connection was dropped.\n", 1)
        if a[0] == "connect":
            ok = self.paired and a[1] == f"192.168.1.20:{self.port}"
            return res(f"connected to {a[1]}\n" if ok else f"failed to connect to '{a[1]}': Connection refused\n")
        if a[0] == "-s" and a[2] == "shell":
            if not self.paired or a[1] != f"192.168.1.20:{self.port}":
                return res("error: device '%s' not found\n" % a[1], 1)
            return res("urtv-ok\n" if "urtv-ok" in a[3] else "1\n")
        if a[0] == "-s" and a[2] == "pull":
            open(a[4], "w").write("x"); return res("1 file pulled\n")
        raise SystemExit("fake adb: " + str(a))
adb = Adb()
w.find_adb = lambda: "/fake/adb"
def mdns(svc, t):
    if svc == w.SERVICE_PAIRING: return [{"instance": "adbpair", "host": "192.168.1.20", "port": 37105}]
    if svc == w.SERVICE_CONNECT and adb.hand_out: return [{"instance": TV1, "host": "192.168.1.20", "port": adb.port}]
    return []
def cmd(fn, *args, **kw):
    out = io.StringIO()
    with redirect_stdout(out):
        rc = fn(*args, **kw)
    return rc, out.getvalue()

rc, out = cmd(ft.pair_cmd, [], adb.run, mdns)
check("pair with no arguments: says what to do and offers the pairing port it found", rc == 1 and "6-digit" in out and "192.168.1.20:37105" in out, out)
w.find_adb = lambda: ""
rc, out = cmd(ft.pair_cmd, [], adb.run, mdns)
check("pair with no adb: says exactly what is missing", rc == 1 and "winget" in out and "brew" in out and "apt" in out, out)
check("pair with no adb and no arguments: the pairing steps come first (what to open, which numbers), then what is missing",
      rc == 1 and "Wireless debugging" in out and "6-digit" in out and "Pair device with pairing code" in out
      and out.index("6-digit") < out.index("winget"), out)
rc, out = cmd(ft.pair_cmd, ["192.168.1.20:37105", "123456"], adb.run, mdns)
check("pair with arguments and no adb: only what is missing", rc == 1 and "winget" in out and "Pair device" not in out, out)
rc, out = cmd(ft.connect_cmd, ["192.168.1.20:41234"], adb.run)
check("connect with no adb: same", rc == 1 and "brew" in out, out)
w.find_adb = lambda: "/fake/adb"
rc, out = cmd(ft.pair_cmd, ["192.168.1.20:37105", "000000"], adb.run, mdns)
check("pair with a wrong code: stops, nothing remembered", rc == 1 and "code" in out and not w.load_state(), out)
rc, out = cmd(ft.pair_cmd, ["192.168.1.20:37105", "12345"], adb.run, mdns)
check("pair with a 5-digit code: refused before adb is asked", rc == 1 and "6 digits" in out and not any(a[0] == "pair" and a[2] == "12345" for a in adb.log), out)
ft.local_ip = lambda: "192.168.1.5"
rc, out = cmd(ft.pair_cmd, ["192.168.1.20:37105", "123456"], adb.run, mdns)
st = w.load_state()
check("pair: paired, found the connect port by mDNS, connected, remembered",
      rc == 0 and st.get("mode") == "tls" and st.get("port") == 41234 and st.get("instance") == TV1 and st.get("paired")
      and open(ft.HOST_FILE).read().strip() == "192.168.1.20" and "123456" not in out, f"{out} {st}")
check("the remembered TV lives in the .urtv folder, next to the key", w.state_path() == os.path.join(tmp, "wireless.json"))

# pairing worked but mDNS is silent: the person is asked for the port, and `connect` finishes it
os.remove(w.state_path()); adb.paired = False; adb.hand_out = False
rc, out = cmd(ft.pair_cmd, ["192.168.1.20:37105", "123456"], adb.run, mdns)
check("pair with silent mDNS: asks for «IP address & Port» and says the next command",
      rc == 1 and "IP address & Port" in out and "find-tv.sh connect" in out and w.load_state().get("mode") == "pending", out)
rc, out = cmd(ft.connect_cmd, ["192.168.1.20:41234"], adb.run)
check("connect <IP:port> after that", rc == 0 and w.load_state().get("mode") == "tls" and w.load_state().get("instance") == TV1, out + str(w.load_state()))
adb.hand_out = True
rc, out = cmd(ft.connect_cmd, ["nonsense"], adb.run)
check("connect with something that is not IP:port", rc == 2, out)

# ---- tv.py uses it transparently, and finds the port again after a restart
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
class TcpDev:
    def __init__(self, *a, **k): TcpDev.made = (a, k)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = TcpDev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.168.1.20"
os.environ.pop("TV_PORT", None)
import tv   # noqa: E402
tv.signer = lambda: None
tv.time = types.SimpleNamespace(time=lambda: 0.0, sleep=lambda s: None)
tv.ID_FILE = os.path.join(tmp, "tv-id.json")
orig_cli = w.AdbCli.__init__
def cli_init(self, host, state, timeout=60.0, run=None, browse_fn=None):
    orig_cli(self, host, state, timeout, adb.run, mdns)
w.AdbCli.__init__ = cli_init
check("tv.py: a paired TV is reached through the adb program", isinstance(tv.new_dev(60.0), w.AdbCli))
check("tv.py: uploads use pieces a Windows command line can carry", tv.upload_chunk() <= 30_000)
t = tv.TV()
check("tv.py: a command goes through adb -s <TV> shell", t.shell("getprop ro.build.version.release").strip() == "1")
adb.port = 38888   # the TV restarted: a new port
tv.TRIES = 4
t2 = tv.TV()
check("tv.py: after a restart the port is looked up again by mDNS, with no word from the person",
      t2.shell("getprop x").strip() == "1" and w.load_state()["port"] == 38888, str(adb.log[-4:]))
adb.port = 39999; adb.hand_out = False
errs = io.StringIO()
try:
    with redirect_stderr(errs), redirect_stdout(io.StringIO()):
        tv.TV().shell("true")
    check("tv.py: port lost and mDNS silent -> stops", False, "no exit")
except SystemExit as e:
    check("tv.py: port lost and mDNS silent -> stops and says to ask for «IP address & Port»",
          "find-tv.sh connect" in str(e), str(e))
try:   # 4.3.3: a restart where the new port is never found: the wait says how to give it by hand, then go on from the step
    _t = tv.time; _c = [0.0]
    tv.time = types.SimpleNamespace(time=lambda: _c.__setitem__(0, _c[0] + 5) or _c[0], sleep=lambda s: None)
    tv.address_hint = lambda *a: ""
    try:
        with redirect_stderr(io.StringIO()), redirect_stdout(io.StringIO()):
            tv.wait_for_tv(limit_s=30)
    finally:
        tv.time = _t
    check("tv.py --wait: a Wireless-debugging TV that is not found again says to read «IP address & Port» and connect", False, "no exit")
except SystemExit as e:
    check("tv.py --wait: a Wireless-debugging TV that is not found again says to read «IP address & Port» and connect", "find-tv.sh connect" in str(e) and "Wireless debugging" in str(e), str(e))
adb.hand_out = True
adb.port = 38888
pulled = os.path.join(tmp, "pulled")
tv.TV().call("pull", "/sdcard/x", pulled)
check("tv.py: pull works through adb", os.path.exists(pulled))
os.environ["TV_PORT"] = "5555"
check("tv.py: TV_PORT set by hand means plain TCP", tv.new_dev(5.0).__class__ is TcpDev and tv.upload_chunk() == 40_000)
del os.environ["TV_PORT"]
tv.HOST = "192.168.1.77"   # another address than the remembered TV: plain TCP
check("tv.py: another address is not the paired TV", tv.new_dev(5.0).__class__ is TcpDev)
tv.HOST = "192.168.1.20"
w.AdbCli.__init__ = orig_cli
w.find_adb = lambda: ""
try:
    tv.new_dev(5.0)
    check("tv.py: paired TV but no adb program -> says what is missing", False, "no exit")
except SystemExit as e:
    check("tv.py: paired TV but no adb program -> says what is missing", "brew" in str(e), str(e))
w.find_adb = lambda: "/fake/adb"

# ---- shell errors from adb are connection errors (retried), not output
cli = w.AdbCli("192.168.1.20", {"port": 1}, 5, lambda argv, **k: subprocess.CompletedProcess(argv, 1, "error: device offline\n", ""), lambda *a: [])
try:
    cli.shell("true"); check("adb 'device offline' is raised so tv.py retries", False)
except ConnectionError:
    check("adb 'device offline' is raised so tv.py retries", True)
cli = w.AdbCli("192.168.1.20", {"port": 1}, 5, lambda argv, **k: subprocess.CompletedProcess(argv, 1, "", ""), lambda *a: [])
check("a command that fails on the TV (exit 1, no output) is just its output", cli.shell("grep x /nope") == "")

# ---- find-tv: where it looks
check("netmask: 255.255.254.0 is /23, 0xffffff00 is /24, garbage is None",
      ft.mask_prefix("255.255.254.0") == 23 and ft.mask_prefix("0xffffff00") == 24 and ft.mask_prefix("255.0.255.0") is None
      and ft.mask_prefix("255.255.255.255") is None)
linux = "2: wlan0    inet 192.168.1.5/23 brd 192.168.1.255 scope global dynamic wlan0\n3: eth0    inet 10.0.0.9/24 brd 10.0.0.255 scope global eth0"
mac = "en0: flags=8863\n\tinet 192.168.0.12 netmask 0xffffff00 broadcast 192.168.0.255\n"
win_ar = "   عنوان IPv4 . . . . . . . . . . . : 192.168.1.50\n   قناع الشبكة الفرعية . . . . . . . : 255.255.254.0\n   البوابة الافتراضية . . . . . . . . : 192.168.1.1\n"
check("networks: ip addr, ifconfig (hex mask) and an Arabic Windows ipconfig",
      ft.parse_networks(linux) == [("192.168.1.5", 23), ("10.0.0.9", 24)] and ft.parse_networks(mac) == [("192.168.0.12", 24)]
      and ft.parse_networks(win_ar) == [("192.168.1.50", 23)], str([ft.parse_networks(x) for x in (linux, mac, win_ar)]))
ft.run_text = lambda *c: linux if c[:2] == ("ip", "-o") else ""
nets = [str(n) for n in ft.my_networks("192.168.1.5")]
check("my_networks: the route address first, the mask kept, both adapters", nets == ["192.168.0.0/23", "10.0.0.0/24"] or nets == ["192.168.1.0/24", "192.168.0.0/23", "10.0.0.0/24"] or "192.168.0.0/23" in nets and "10.0.0.0/24" in nets, str(nets))
ft.run_text = lambda *c: "inet 10.1.2.3/8 scope global" if c[:2] == ("ip", "-o") else ""
check("my_networks: a /8 is capped to a /22 around the address", [str(n) for n in ft.my_networks(None)] == ["10.1.0.0/22"], str(ft.my_networks(None)))
ft.run_text = lambda *c: "inet 169.254.3.4/16 scope link\ninet 8.8.4.4/24" if c[:2] == ("ip", "-o") else ""
check("my_networks: link-local and public addresses are not scanned", ft.my_networks(None) == [])
check("arp table: neighbours of home ranges only, no broadcast", ft.parse_arp("? (192.168.1.1) at aa:bb on en0\n? (192.168.1.77) at cc:dd\n? (224.0.0.251) at 01:00\n? (192.168.1.255) at ff:ff") == ["192.168.1.1", "192.168.1.77"])

# ---- a scan on a fake network: a sleeping TV answers only to a long knock, on the second subnet of a /23
def scan_with(answer, nets=("192.168.0.0/23",), route="192.168.0.5", near=(), mdns_hits=([], [])):
    ft.local_ip = lambda: route
    ft.my_networks = lambda ip: [__import__("ipaddress").ip_network(n) for n in nets]
    ft.fake_network = lambda net: False
    ft.neighbours = lambda: list(near)
    ft.mdns_hosts = lambda ip: mdns_hits
    ft.cast_name = lambda h: ""
    ft.connects = lambda h, p, timeout=1.0: answer(h, p, timeout)
    ft.wireless.load_state = lambda: {}
    out = io.StringIO()
    if os.path.exists(ft.HOST_FILE): os.remove(ft.HOST_FILE)
    with redirect_stdout(out):
        rc = ft.scan()
    return rc, out.getvalue()
rc, out = scan_with(lambda h, p, t: h == "192.168.1.130" and p == 5555)
check("scan: a TV on the other half of a /23 is found", rc == 0 and open(ft.HOST_FILE).read().strip() == "192.168.1.130", out)
rc, out = scan_with(lambda h, p, t: h == "192.168.0.40" and p == 5555 and t >= 3.0)
check("scan: a TV that wakes slowly (3 s) is found by the slower second pass", rc == 0 and open(ft.HOST_FILE).read().strip() == "192.168.0.40", out)
rc, out = scan_with(lambda h, p, t: h == "192.168.0.40" and p == 5555 and t >= 5.0, near=["192.168.0.40"])
check("scan: a TV the router already knows gets a 5 s knock first", rc == 0 and "192.168.0.40" in out, out)
rc, out = scan_with(lambda h, p, t: h == "192.168.0.8" and p == 8008)
check("scan: only a Cast device and no ADB -> says wireless debugging may be the way", rc == 1 and "Cast device: 192.168.0.8" in out and "find-tv.sh pair" in out, out)
rc, out = scan_with(lambda h, p, t: False, mdns_hits=(["192.168.0.60"], [{"instance": TV1, "host": "192.168.0.60", "port": 4242}]))
check("scan: nothing on 5555 but a wireless-debugging TV announces itself -> pair", rc == 1 and "pair" in out and "Wireless debugging" in out, out)
rc, out = scan_with(lambda h, p, t: h == "192.168.0.40" and p == 5555)
check("scan: 5555 open is saved as before", rc == 0 and "Saved 192.168.0.40" in out, out)

# ---- find-tv.sh --closed: after the person turned debugging off
w.load_state = real_load_state
w.save_state({"host": "192.168.1.20", "port": 41234, "mode": "tls", "paired": True})
def closed(open_ports, announced=False, host="192.168.1.20"):
    out = io.StringIO()
    with redirect_stdout(out):
        rc = ft.closed_cmd(host, lambda h, p: (h, p) in open_ports, lambda svc, t: [{"instance": TV1, "host": host, "port": 1}] if announced else [])
    return rc, out.getvalue()
rc, out = closed({("192.168.1.20", 8009)})
check("--closed: nothing on 5555 or the remembered port, the TV awake: OK", rc == 0 and out.startswith("OK: ADB is closed"), out)
rc, out = closed({("192.168.1.20", 8009), ("192.168.1.20", 5555)})
check("--closed: 5555 still open: says so and what to turn off", rc == 1 and "port 5555" in out and "Network debugging" in out, out)
rc, out = closed({("192.168.1.20", 41234)})
check("--closed: the remembered Wireless-debugging port still open: not closed", rc == 1 and "port 41234" in out, out)
rc, out = closed({("192.168.1.20", 8009)}, announced=True)
check("--closed: Wireless debugging still announced by mDNS: not closed", rc == 1 and "announced" in out, out)
rc, out = closed(set())
check("--closed: the TV answers nothing at all: cannot tell (not «closed»)", rc == 2 and "Can't tell" in out and "OK" not in out, out)
rc, out = closed({("192.168.1.99", 8009)}, host="192.168.1.99")
check("--closed: another address than the remembered TV: its own 5555 only", rc == 0, out)

print("\nall wireless checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
