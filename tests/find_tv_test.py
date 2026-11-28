#!/usr/bin/env python3
"""optimize/find-tv.py --check on three simulated networks: a home Wi-Fi, a sandbox whose proxy accepts every
connection, and a virtual machine with no route out. Run: python3 tests/find_tv_test.py"""
import errno
import importlib.util
import io
import socket
import sys
from contextlib import redirect_stdout
from pathlib import Path

spec = importlib.util.spec_from_file_location("find_tv", Path(__file__).resolve().parents[1] / "optimize/find-tv.py")
ft = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ft)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def run(ip, answer):
    """answer(host, port) -> "open" | "refused" | "timeout" | "unreachable" (errno or winerror)."""
    def connect(addr, timeout=None):
        a = answer(*addr)
        if a == "open":
            return socket.socket()
        if a == "refused":
            raise ConnectionRefusedError()
        if a == "unreachable":
            raise OSError(errno.ENETUNREACH, "Network is unreachable")
        if a == "winunreachable":
            e = OSError(None, "A socket operation was attempted to an unreachable network")
            e.winerror = 10051
            raise e
        raise TimeoutError()
    ft.local_ip, socket.create_connection = (lambda: ip), connect
    out = io.StringIO()
    with redirect_stdout(out):
        code = ft.check()
    return code, out.getvalue()


real = socket.create_connection
try:
    # home Wi-Fi: the router answers on 80, unused ports are refused or silent, other ranges time out
    home = lambda h, p: "open" if (h, p) == ("192.168.1.1", 80) else "refused" if h.startswith("192.168.1.") else "timeout"
    code, out = run("192.168.1.23", home)
    check("home Wi-Fi: OK", code == 0, out)
    # a sandbox: the proxy accepts any address and port
    code, out = run("192.168.1.23", lambda h, p: "open")
    check("sandbox proxy answering everything: not OK", code == 1 and "proxy" in out, out)
    # a virtual machine on this computer: its own address, no route to the home network
    code, out = run("172.17.0.2", lambda h, p: "unreachable")
    check("virtual machine, no route: not OK", code == 1 and "unreachable" in out, out)
    code, out = run("10.0.2.15", lambda h, p: "winunreachable")
    check("no route on Windows (WinError 10051): not OK", code == 1 and "unreachable" in out, out)
    # a cloud container: not a home address at all
    code, out = run("192.0.2.2", lambda h, p: "open")
    check("cloud container address: not OK", code == 1 and "not a home network address" in out, out)
finally:
    socket.create_connection = real

# ---- scan(): which device is the TV
import tempfile

FIND = {"192.168.1.23": "me", "192.168.1.10": "tv1", "192.168.1.11": "tv2", "192.168.1.1": "gw", "192.168.56.101": "vm"}
INFO = {"192.168.1.10": {"name": "Living room", "manufacturer": "Acme", "model": "X1"},
        "192.168.1.11": {"name": "Bedroom", "manufacturer": "Other", "model": "Y2"}}


def scan_with(adb_hosts, info=INFO, route="192.168.1.1", ifaces="2: wlan0    inet 192.168.1.23/24 brd 192.168.1.255 scope global wlan0\n"
              "3: vboxnet0    inet 192.168.56.1/24 scope global vboxnet0\n"):
    saved = {k: getattr(ft, k) for k in ("local_ip", "run_text", "knock_all", "mdns_hosts", "neighbours", "cast_info", "fake_network",
                                         "connects", "HOST_FILE")}
    saved_state = (ft.wireless.load_state, ft.wireless.save_state)
    d = tempfile.mkdtemp()
    ft.HOST_FILE = d + "/.tv_host"
    ft.local_ip = lambda: "192.168.1.23"
    ft.run_text = lambda *c: ifaces if c[:2] == ("ip", "-o") else f"default via {route} dev wlan0\n" if c[:2] == ("ip", "route") else ""
    ft.knock_all = lambda pairs, t, w=128: [p for p in pairs if p[0] in adb_hosts or p[1] == 8008 and p[0] in info]
    ft.mdns_hosts, ft.neighbours, ft.fake_network = (lambda ip: ([], [])), (lambda: []), (lambda net: False)
    ft.cast_info = lambda h: info.get(h, {})
    ft.connects = lambda h, p, t=1.0: False
    ft.wireless.load_state, ft.wireless.save_state = (lambda: {}), (lambda st: None)
    out = io.StringIO()
    try:
        with redirect_stdout(out):
            code = ft.scan()
    finally:
        for k, v in saved.items():
            setattr(ft, k, v)
        ft.wireless.load_state, ft.wireless.save_state = saved_state
    f = Path(d) / ".tv_host"
    return code, out.getvalue(), (f.read_text().strip() if f.exists() else None)


code, out, saved_host = scan_with({"192.168.1.10", "192.168.1.11"})
check("two candidates: exit 3, not saved", code == 3 and saved_host is None, (code, out, saved_host))
check("two candidates listed with brand/model/name",
      "1) 192.168.1.10 — Acme X1 (Living room)" in out and "2) 192.168.1.11 — Other Y2 (Bedroom)" in out, out)
check("two candidates: the «!!» line", "!! More than one device answers with ADB open: ask the person which one is the TV (Settings → Network on the "
      "TV shows its address), then run: optimize/find-tv.sh use <IP>" in out, out)
code, out, saved_host = scan_with({"192.168.1.10"})
check("one candidate: saved, brand/model printed", code == 0 and saved_host == "192.168.1.10" and "ADB open on: 192.168.1.10 — Acme X1 (Living room)" in out,
      (code, out, saved_host))
code, out, saved_host = scan_with({"192.168.1.10", "192.168.56.101", "192.168.1.1", "192.168.1.23"})
check("VM subnet, gateway and own address skipped", code == 0 and saved_host == "192.168.1.10" and "56.101" not in out.split("Saved")[0].split("ADB open on")[-1], (code, out, saved_host))
code, out, saved_host = scan_with({"192.168.1.1", "192.168.56.101", "192.168.1.23"})
check("only non-TV addresses answer: nothing saved", code == 1 and saved_host is None and "No TV with ADB open" in out, (code, out, saved_host))
code, out, saved_host = scan_with({"192.168.1.10", "192.168.1.12"}, info={})
check("no Cast info: listed by address only", code == 3 and "1) 192.168.1.10\n" in out and "2) 192.168.1.12\n" in out, out)
check("virtual interface network not scanned", "192.168.56" not in scan_with({"192.168.1.10"})[1].split("\n")[0], "")
check("parse_gateways: ip route / route -n get / netstat / route print",
      ft.parse_gateways("default via 192.168.1.1 dev eth0") == ["192.168.1.1"] and ft.parse_gateways("   gateway: 10.0.0.1") == ["10.0.0.1"]
      and ft.parse_gateways("default            192.168.0.1        UGScg  en0") == ["192.168.0.1"]
      and ft.parse_gateways("          0.0.0.0          0.0.0.0      192.168.2.1   192.168.2.5     25") == ["192.168.2.1"], "")
# no known gateway: .1 is skipped only if it answers 80/443/53
check("fallback: .1 answering 80 is the router", ft.candidates(["192.168.1.1", "192.168.1.10"], set(), set(), [], lambda h, p: p == 80) == ["192.168.1.10"], "")
check("fallback: .1 answering nothing is kept", ft.candidates(["192.168.1.1"], set(), set(), [], lambda h, p: False) == ["192.168.1.1"], "")
check("parse_ifaces names", ("vboxnet0", "192.168.56.1", 24) in ft.parse_ifaces("3: vboxnet0    inet 192.168.56.1/24 scope global vboxnet0")
      and ("en0", "192.168.1.5", 24) in ft.parse_ifaces("en0: flags=8863<UP>\n\tinet 192.168.1.5 netmask 0xffffff00 broadcast 192.168.1.255"), "")
check("virtual_ip: a bare address in a VM range is virtual; the same address inside a scanned network, or on a real interface, is not",
      ft.virtual_ip("192.168.99.7") and not ft.virtual_ip("192.168.99.7", nets=[ft.ipaddress.ip_network("192.168.99.0/24")])
      and not ft.virtual_ip("192.168.99.7", "wlan0") and ft.virtual_ip("192.168.99.7", "vboxnet0"), "")
_saved_if = ft.interfaces
ft.interfaces = lambda: [("wlan0", "192.168.99.23", 24), ("docker0", "172.17.0.1", 16)]
nets = [str(n) for n in ft.my_networks("192.168.99.23")]
ft.interfaces = _saved_if
check("a home router that uses 192.168.99.x (a real interface, the route out) is scanned; docker0 is not", nets == ["192.168.99.0/24"], nets)


# ---- use IP
def use_with(arg, open_ports=(5555,), info=None, state=None):
    saved = (ft.HOST_FILE, ft.cast_info, ft.wireless.load_state, ft.wireless.save_state)
    d = tempfile.mkdtemp()
    ft.HOST_FILE = d + "/.tv_host"
    ft.cast_info = lambda h: info or {}
    ft.wireless.load_state, ft.wireless.save_state = (lambda: state or {}), (lambda st: None)
    out = io.StringIO()
    try:
        with redirect_stdout(out):
            code = ft.use_cmd([arg], knock=lambda h, p: p in open_ports)
    finally:
        ft.HOST_FILE, ft.cast_info = saved[0], saved[1]
        ft.wireless.load_state, ft.wireless.save_state = saved[2], saved[3]
    f = Path(d) / ".tv_host"
    return code, out.getvalue(), (f.read_text().strip() if f.exists() else None)


code, out, h = use_with("192.168.1.10", info=INFO["192.168.1.10"])
check("use: reachable home IP saved with brand/model", code == 0 and h == "192.168.1.10" and "TV: 192.168.1.10 — Acme X1 (Living room)" in out, (code, out, h))
code, out, h = use_with("8.8.8.8")
check("use: non-home IP refused", code == 2 and h is None and "!!" in out, (code, out, h))
code, out, h = use_with("not-an-ip")
check("use: not an IP refused", code == 2 and h is None, (code, out, h))
code, out, h = use_with("192.168.1.10", open_ports=())
check("use: unreachable refused", code == 1 and h is None and "!!" in out, (code, out, h))
code, out, h = use_with("192.168.1.10", open_ports=(37123,), state={"host": "192.168.1.10", "port": 37123})
check("use: remembered Wireless-debugging port accepted", code == 0 and h == "192.168.1.10", (code, out, h))

# 4.4 (R-375): a TV with ADB on that the knock missed: adbd's own mDNS announcement (_adb._tcp) puts it first, and when nothing is
# found the person is asked for the TV's address in Arabic (no dead end)
seen = []
def fake_browse(service, timeout=3.0, transport=None):
    seen.append(service)
    return [{"instance": "adb-x", "host": "192.168.1.40", "port": 5555}] if service == "_adb._tcp.local" else []
saved = (ft.wireless.browse, ft.wireless.UdpTransport)
ft.wireless.browse, ft.wireless.UdpTransport = fake_browse, (lambda ip=None: None)
try:
    near, tls = ft.mdns_hosts("192.168.1.23")
finally:
    ft.wireless.browse, ft.wireless.UdpTransport = saved
check("mDNS: adbd's own announcement (_adb._tcp) is asked for, and its host is knocked first", "_adb._tcp.local" in seen and near == ["192.168.1.40"]
      and ft.ANNOUNCED == [("192.168.1.40", 5555)], (seen, near))
saved = ft.wireless.load_state
ft.wireless.load_state = lambda: {}
out = io.StringIO()
try:
    with redirect_stdout(out):
        code = ft.not_found([], [], "192.168.1.23")
finally:
    ft.wireless.load_state = saved
o = out.getvalue()
check("nothing found: the device that announced ADB is named with its use command, and the person is asked for the address in Arabic",
      code == 1 and "A device at 192.168.1.40 announces ADB (port 5555)" in o and "optimize/find-tv.sh use 192.168.1.40" in o
      and "!! The scan did not find the TV" in o and "عنوان IP" in o and "optimize/find-tv.sh use <that address>" in o, o)
del ft.ANNOUNCED[:]

print("\nall find-tv checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
