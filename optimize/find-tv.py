#!/usr/bin/env python3
"""Find the TV on this computer's Wi-Fi. Standard library only, so it runs on Windows, macOS and Linux before
optimize/setup.sh, with whatever `python3` or `python` the computer has.

  optimize/find-tv.sh --check   (or: python optimize/find-tv.py --check)   can this terminal reach the home network?
  optimize/find-tv.sh                                                     scan for TVs with ADB (5555) and Cast (8008); saves the TV to optimize/.tv_host
                                                                          only when exactly ONE device answers (exit 3 and a numbered list when several do)
  optimize/find-tv.sh use <IP>                                            the person said which device is the TV (Settings → Network): check it answers, save it
  optimize/find-tv.sh pair [IP:port code]                                 Android 11+ «Wireless debugging»: pair with the code shown on the TV, connect, remember it
  optimize/find-tv.sh --closed                                            after the person turned debugging off on the TV: is the port really closed?
  optimize/find-tv.sh connect IP:port                                     connect to the TV's «IP address & Port» of Wireless debugging (after a restart changed it)

ADB needs a direct TCP connection to the TV. An agent whose commands run in a cloud container, a sandbox or a
virtual machine (even one running on this computer) usually has no route to the home network: the check says so
plainly, and then no amount of retrying will find the TV.
"""
import concurrent.futures
import errno
import ipaddress
import json
import os
import re
import socket
import subprocess
import sys
import urllib.request

for _stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page: «…» came out as «�»
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)   # python -I does not look next to the script
import wireless   # noqa: E402  (mDNS, the adb program, the remembered Wireless-debugging TV)
HOST_FILE = os.path.join(HERE, ".tv_host")


HOME = [ipaddress.ip_network(n) for n in ("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12")]


def local_ip():
    """The address this computer uses to go out (no packet is sent: UDP connect only picks the route)."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("1.1.1.1", 9))
        return s.getsockname()[0]
    except OSError:
        return None
    finally:
        s.close()


def home(ip):
    return bool(ip) and any(ipaddress.ip_address(ip) in n for n in HOME)


def fake_network(net):
    """A sandbox's proxy accepts a connection to any address. Knock where nothing can be: another home range than
    ours, on an unused port. A real network never accepts that; a proxy does."""
    other = "10.254.254.254" if not net.startswith("10.") else "192.168.254.254"
    return connects(other, 47813, 1.0) or connects(f"{net}.254", 47813, 1.0) and connects(f"{net}.253", 47813, 1.0)


def reachable(host, port, timeout=1.5):
    """True: something answered (even «refused» proves the route works). False: no route, or nothing there."""
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except ConnectionRefusedError:
        return True
    except OSError:
        return False


def no_route(host, port=80):
    try:
        socket.create_connection((host, port), timeout=1.5).close()
        return False
    except OSError as e:
        return e.errno in (errno.ENETUNREACH, errno.EHOSTUNREACH) or getattr(e, "winerror", None) in (10051, 10065)


def check():
    ip = local_ip()
    print(f"this terminal's address: {ip or 'none'}")
    if not home(ip):
        return verdict(False, f"{ip or 'no address'} is not a home network address (192.168.x, 10.x, 172.16-31.x)")
    net = ip.rsplit(".", 1)[0]
    if fake_network(net):
        return verdict(False, "addresses that cannot exist answer too, so a proxy answers, not your network")
    router = f"{net}.1"
    for port in (80, 443, 53):
        if reachable(router, port):
            return verdict(True, f"the router {router} answers")
    if no_route(router):
        return verdict(False, f"Network is unreachable ({router})")
    # some routers answer nothing: look for any device on the /24
    with concurrent.futures.ThreadPoolExecutor(64) as pool:
        hits = [h for h, ok in zip(range(1, 255), pool.map(lambda i: reachable(f"{net}.{i}", 80, 0.7), range(1, 255))) if ok]
    if hits:
        return verdict(True, f"devices answer on {net}.0/24")
    return verdict(False, f"nothing answers on {net}.0/24 (a sandbox or virtual machine's own network?)")


def verdict(ok, why):
    if ok:
        print(f"OK: this terminal can reach the home network ({why}). Next: optimize/find-tv.sh")
        return 0
    print(f"!! This terminal cannot reach the home network: {why}.")
    print("   It runs somewhere else (a cloud session, a sandbox, a virtual machine): it can never reach the TV.")
    print("   Use an agent that runs commands directly on a computer on the TV's Wi-Fi, for example Claude Code")
    print("   installed on that computer (Windows: in Git Bash or PowerShell; Mac: in Terminal).")
    return 1


def cast_name(host):
    try:
        url = f"http://{host}:8008/setup/eureka_info?params=name"
        with urllib.request.urlopen(url, timeout=3) as r:
            return json.load(r).get("name", "")
    except Exception:  # noqa: BLE001
        return ""


def cast_info(host):
    """{"name", "manufacturer", "model"} from the Cast info endpoint (no ADB authorisation needed); {} if it does not answer."""
    try:
        url = f"http://{host}:8008/setup/eureka_info?options=detail&params=name,device_info"
        with urllib.request.urlopen(url, timeout=2) as r:
            data = json.load(r)
    except Exception:  # noqa: BLE001
        return {}
    if not isinstance(data, dict):
        return {}
    info = {}
    for k in ("name", "manufacturer", "model_name"):
        for d in (data, data.get("device_info"), data.get("detail")):
            if isinstance(d, dict) and isinstance(d.get(k), str) and d[k].strip():
                info[k] = d[k].strip()
                break
    return {"name": info.get("name", ""), "manufacturer": info.get("manufacturer", ""), "model": info.get("model_name", "")}


def describe(host, info):
    """«192.0.2.10 — Brand Model (Cast name)»; just the address when the device gave no Cast info."""
    info = info or {}
    model = info.get("model", "")
    brand = info.get("manufacturer", "")
    what = model if brand and model.lower().startswith(brand.lower()) else " ".join(x for x in (brand, model) if x)
    if info.get("name"):
        what = (what + " " if what else "") + f"({info['name']})"
    return f"{host} — {what}" if what else host


# ---- where to look. The first version scanned the /24 of the address that goes out to the internet, with a one-second
# knock. That missed a TV with ADB on in five reports: (1) a TV on Wi-Fi power-save wakes slowly and answers a knock after
# more than a second; (2) the computer was on another subnet than the TV (an Ethernet adapter, a VPN, a router with a
# 255.255.254.0 mask, or a second 192.168.x network); (3) the TV is known to the router (the ARP table) or announces itself
# by mDNS even when a knock is lost. Now every home-range address of this computer is scanned with its real mask (at most a
# /22 each), the ARP table and mDNS hits are knocked first, and if nothing answered, a second, slower pass runs.
# (IPv6 is not scanned: ADB over the network listens on the IPv4 address of a TV in every report.)

def mask_prefix(mask):
    """255.255.254.0 or 0xfffffe00 -> 23; None if it is not a netmask."""
    try:
        n = int(mask, 16) if mask.lower().startswith("0x") else int(ipaddress.IPv4Address(mask))
    except ValueError:
        return None
    bits = bin(n)[2:].zfill(32)
    return bits.count("1") if re.fullmatch(r"1*0*", bits) and 0 < bits.count("1") < 32 else None


def parse_networks(text):
    """[(address, prefix)] from the output of `ip -o -4 addr`, `ifconfig` or `ipconfig` (in any language: only the numbers
    are read, an address and the netmask that follows it)."""
    found = []
    for m in re.finditer(r"inet (\d+\.\d+\.\d+\.\d+)/(\d+)", text):   # ip addr
        found.append((m.group(1), int(m.group(2))))
    text = re.sub(r"inet (\d+\.\d+\.\d+\.\d+)/(\d+)", " ", text)
    last = None
    for m in re.finditer(r"0x[0-9a-fA-F]{8}|\d{1,3}(?:\.\d{1,3}){3}", text):
        tok = m.group(0)
        pref = mask_prefix(tok)
        if pref and last:
            found.append((last, pref))
            last = None
        elif not tok.startswith("0x"):
            try:
                ipaddress.IPv4Address(tok)
            except ValueError:
                continue
            last = tok
    return found


def run_text(*cmd):
    try:
        return subprocess.run(cmd, capture_output=True, text=True, timeout=8, errors="replace").stdout or ""
    except (OSError, subprocess.SubprocessError):
        return ""


VIRTUAL_NETS = [ipaddress.ip_network(n) for n in (
    "192.168.56.0/24", "192.168.99.0/24", "192.168.122.0/24", "192.168.65.0/24", "172.17.0.0/16", "10.0.2.0/24", "10.0.3.0/24")]
VIRTUAL_IFACE = re.compile(r"^(vboxnet|docker|br-|veth|virbr|vmnet|utun|vethernet)|virtualbox|vmware|hyper-v", re.I)


def virtual_ip(ip, iface=None, nets=()):
    """An address in a virtual-machine, container or emulator network: never the TV. Those ranges are also used by some home routers, so a
    range decides only for an address that is on no network being scanned and on no known interface; on an interface its name decides
    (VIRTUAL_IFACE), and an address inside a scanned network (the one that goes out to the internet, or a real interface) is never dropped."""
    if iface:
        return bool(VIRTUAL_IFACE.search(iface))
    try:
        a = ipaddress.ip_address(ip)
        return not any(a in n for n in nets) and any(a in n for n in VIRTUAL_NETS)
    except ValueError:
        return False


def parse_ifaces(text):
    """[(interface name or "", address, prefix)] from `ip -o -4 addr`, `ifconfig` or `ipconfig`."""
    out, name, buf = [], "", []

    def flush():
        out.extend((name, ip, pref) for ip, pref in parse_networks("\n".join(buf)))

    for line in text.splitlines():
        m = re.match(r"\d+:\s+(\S+?)(?:@\S+)?\s+inet ", line)   # ip -o: one line per address
        h = re.match(r"^(\S+?):?\s+(?:flags=|Link encap)", line) or re.match(r"^\S.*adapter (.+?):\s*$", line)
        if m or h:
            flush()
            name, buf = (m or h).group(1), [line] if m else []
        else:
            buf.append(line)
    flush()
    return out


def interfaces():
    out = []
    for text in (run_text("ip", "-o", "-4", "addr", "show"), run_text("ifconfig"), run_text("ipconfig")):
        out += parse_ifaces(text)
    return out


def own_addresses(route_ip=None):
    """Every IPv4 address of this computer (not only the one that goes out), as strings."""
    return set(ip for _, ip, _ in interfaces()) | ({route_ip} if route_ip else set())


def parse_gateways(text):
    """Default gateways in the output of `ip route`, `route -n get default`, `netstat -rn` or `route print`."""
    found = []
    for line in text.splitlines():
        m = (re.search(r"default via (\d+\.\d+\.\d+\.\d+)", line) or re.search(r"gateway:\s*(\d+\.\d+\.\d+\.\d+)", line)
             or re.match(r"\s*(?:default|0\.0\.0\.0)\s+(?:0\.0\.0\.0\s+)?(\d+\.\d+\.\d+\.\d+)", line))
        if m and m.group(1) != "0.0.0.0":
            found.append(m.group(1))
    return list(dict.fromkeys(found))


def gateways():
    out = []
    for cmd in (("ip", "route"), ("route", "-n", "get", "default"), ("netstat", "-rn"), ("route", "print")):
        out += parse_gateways(run_text(*cmd))
    return list(dict.fromkeys(out))


def my_networks(route_ip):
    """The networks to scan: each home-range address of this computer with its real mask, capped to a /22 (1022 hosts); the
    address that goes out to the internet counts as a /24 if no mask is known. First one: the one that goes out. Networks of
    virtual machines, containers and emulators are left out."""
    nets = []

    def add(ip, prefix, name=None):   # name None = the address that goes out to the internet: that network is the home network
        if not home(ip) or (name is not None and virtual_ip(ip, name)):
            return
        net = ipaddress.ip_network(f"{ip}/{max(prefix, 22)}", strict=False)
        if net not in nets:
            nets.append(net)

    if route_ip:
        add(route_ip, 24)
    for name, ip, prefix in interfaces():
        add(ip, prefix, name)
    return nets


def parse_arp(text):
    """Addresses of the neighbours in the output of `arp -a` / `ip neigh` (home ranges only)."""
    return [ip for ip in dict.fromkeys(re.findall(r"\b(\d{1,3}(?:\.\d{1,3}){3})\b", text)) if home(ip) and not ip.endswith((".255", ".0"))]


def neighbours():
    return parse_arp(run_text("arp", "-a") + run_text("ip", "neigh"))


def knock_all(pairs, timeout, workers=128):
    """Which (host, port) accept a connection."""
    with concurrent.futures.ThreadPoolExecutor(workers) as pool:
        return [p for p, ok in zip(pairs, pool.map(lambda hp: connects(hp[0], hp[1], timeout), pairs)) if ok]


SERVICE_ADB = "_adb._tcp.local"   # adbd's own announcement while network ADB is on (Android 11 and newer, some older builds)
ANNOUNCED = []   # 4.4: the (host, port) that announced ADB by mDNS in the last scan: said by not_found() when no knock reached them


def mdns_hosts(iface_ip):
    """Addresses that announce themselves as Cast devices (every Android TV does), as Wireless-debugging TVs, or (4.4: a TV with ADB on
    that the knock missed) as adbd itself: these are knocked first, and again slowly when nothing answered."""
    hosts, wireless_hits = [], []
    del ANNOUNCED[:]
    for service in (wireless.SERVICE_CAST, wireless.SERVICE_CONNECT, SERVICE_ADB):
        try:
            hits = wireless.browse(service, 2.0, wireless.UdpTransport(iface_ip))
        except OSError:
            hits = []
        hosts += [h["host"] for h in hits]
        if service == wireless.SERVICE_CONNECT:
            wireless_hits = hits
        if service == SERVICE_ADB:
            ANNOUNCED.extend((h["host"], h["port"]) for h in hits)
    return list(dict.fromkeys(hosts)), wireless_hits


def candidates(adb, own, gws, nets, knock=None):
    """Drop what cannot be the TV: this computer's own addresses, virtual networks, the router. Without a known default gateway,
    the .1 and .254 of a scanned /24 count as the router only if they also answer 80, 443 or 53."""
    knock = knock or (lambda h, p: connects(h, p, 1.0))
    keep = []
    for h in adb:
        if h in own or h in gws or virtual_ip(h, nets=nets):
            continue
        if not gws and h.endswith((".1", ".254")) and any(knock(h, p) for p in (80, 443, 53)):
            continue
        keep.append(h)
    return keep


def scan():
    ip = local_ip()
    nets = my_networks(ip)
    if not nets or fake_network(nets[0].network_address.exploded.rsplit(".", 1)[0]):
        print("!! This terminal cannot reach the home network. Run: optimize/find-tv.sh --check")
        return 1
    own = own_addresses(ip)
    if not ip or virtual_ip(ip, nets=nets):
        ip = next((a for _, a, _ in interfaces() if a in nets[0]), str(nets[0].network_address))
    gws = set(gateways())
    hosts = [str(h) for n in nets for h in n.hosts() if str(h) not in own]
    print("Scanning " + ", ".join(str(n) for n in nets) + f" from {ip} …")
    near, tls = mdns_hosts(ip)
    near = [h for h in dict.fromkeys(near + neighbours()) if not virtual_ip(h, nets=nets) and h not in own]
    hosts = list(dict.fromkeys(near + hosts))   # the ones the network already knows go first
    pairs = [(h, p) for h in hosts for p in (5555, 8008)]
    open_ = knock_all(pairs, 1.5)
    adb = candidates([h for h, p in open_ if p == 5555], own, gws, nets)
    if not adb and near:   # a TV on Wi-Fi power-save answers slowly: the known ones get a long knock
        adb = candidates([h for h, p in knock_all([(h, 5555) for h in near], 5.0, 32)], own, gws, nets)
    if not adb:   # still nothing: the whole range once more, slower
        slow = knock_all([(h, 5555) for h in hosts], 3.5, 96)
        adb = candidates([h for h, p in slow], own, gws, nets)
    cast = [h for h, p in open_ if p == 8008 and h not in own and h not in gws and not virtual_ip(h, nets=nets)]
    infos = {}
    with concurrent.futures.ThreadPoolExecutor(16) as pool:
        for h, info in zip(list(dict.fromkeys(cast + adb)), pool.map(cast_info, list(dict.fromkeys(cast + adb)))):
            infos[h] = info
    for h in cast:
        print(f"Cast device: {describe(h, infos.get(h))}")
    if len(adb) > 1:
        print("ADB is open on several devices:")
        for i, h in enumerate(adb, 1):
            print(f"{i}) {describe(h, infos.get(h))}")
        print("!! More than one device answers with ADB open: ask the person which one is the TV (Settings → Network on the TV "
              "shows its address), then run: optimize/find-tv.sh use <IP>")
        return 3
    if adb:
        print(f"ADB open on: {describe(adb[0], infos.get(adb[0]))}")
        return save_tv(adb[0])
    return not_found(cast, tls, ip)


def save_tv(host, plain=True):
    with open(HOST_FILE, "w") as f:
        f.write(host + "\n")
    st = wireless.load_state()
    if plain and st.get("host") == host and st.get("mode") == "tls":   # 5555 answers again: the plain way is simpler
        wireless.save_state(dict(st, mode="tcp"))
    print(f"Saved {host} to optimize/.tv_host. Next: optimize/venv/bin/python -I optimize/tv.py --probe")
    print("The ADB key the TV trusts is in " + ("%USERPROFILE%\\.urtv" if os.name == "nt" else "~/.urtv")
          + ", outside this folder: if --probe waits with no «Allow USB debugging?» on the TV, see docs/SETUP-PROMPT.md.")
    return 0


def use_cmd(args, knock=None):
    """The person said which device is the TV: it must be a home-range IPv4 literal that accepts ADB's port."""
    knock = knock or (lambda h, p: connects(h, p, 5.0))
    if len(args) != 1:
        print("usage: optimize/find-tv.sh use <IP>   (the TV's address: Settings → Network on the TV)")
        return 2
    host = args[0]
    try:
        ok = isinstance(ipaddress.ip_address(host), ipaddress.IPv4Address) and home(host) and str(ipaddress.ip_address(host)) == host
    except ValueError:
        ok = False
    if not ok:
        print(f"!! «{host}» is not a home network IPv4 address (192.168.x.x, 10.x.x.x or 172.16-31.x.x). Ask the person to read the "
              "TV's address again (Settings → Network on the TV), then run: optimize/find-tv.sh use <IP>")
        return 2
    st = wireless.load_state()
    ports = [5555] + ([int(st["port"])] if st.get("host") == host and st.get("port") else [])
    answered = [p for p in ports if knock(host, p)]
    if not answered:
        print(f"!! Nothing accepts a connection at {host} on port " + " or ".join(str(p) for p in ports) + ". On the TV: Developer "
              "options → USB debugging (and Network debugging) on, TV awake, same Wi-Fi as this computer; check the address, then run "
              "the same command again.")
        return 1
    print(f"TV: {describe(host, cast_info(host))}")
    return save_tv(host, 5555 in answered)


def not_found(cast, tls, ip):
    """Nothing on 5555. A TV that remembers a pairing is reconnected on its new port; else say what to try."""
    st = wireless.load_state()
    if st.get("paired") and wireless.find_adb():
        hit = wireless.pick_connect(tls or wireless.browse(wireless.SERVICE_CONNECT, 3.0, wireless.UdpTransport(ip)),
                                    st.get("instance", ""), st.get("host", ""))
        if hit:
            return finish_connect(wireless.find_adb(), f"{hit['host']}:{hit['port']}", st.get("instance", ""))
    print("No TV with ADB open on port 5555. On the TV: Developer options → USB debugging (and Network debugging) on, TV awake.")
    if tls:
        print("The TV answers as «Wireless debugging» (Android 11+, port 5555 is closed on it): pair it once: optimize/find-tv.sh pair")
    else:
        print("Android 11 and newer TVs may keep 5555 closed: Developer options → «Wireless debugging» on, then: optimize/find-tv.sh pair")
    if cast:
        print("(The TV is probably one of the Cast devices above: set TV_HOST=<its address> after turning debugging on.)")
    for host, port in dict.fromkeys(ANNOUNCED):   # it says ADB is on, but no knock got through (Wi-Fi power-save, a firewall on this computer)
        print(f"A device at {host} announces ADB (port {port}) but did not answer the scan: if it is the TV, run: optimize/find-tv.sh use {host}")
    # 4.4 (reports: the scan missed a TV with ADB on): the person can read the address on the TV; a clear prompt instead of a dead end
    print("!! The scan did not find the TV. Ask the person, in Arabic: «افتح الإعدادات في التلفزيون ← الشبكة والإنترنت ← الشبكة المتصل فيها، "
          "واقرأ لي عنوان IP (مثل 192.168.x.x)». Then run: optimize/find-tv.sh use <that address>")
    return 1


# ---- Wireless debugging: pair, connect

def verify(adb, hostport, run=subprocess.run):
    try:
        r = run([adb, "-s", hostport, "shell", "echo urtv-ok"], capture_output=True, text=True, timeout=30)
    except (subprocess.SubprocessError, OSError):
        return False
    return "urtv-ok" in (r.stdout or "")


def finish_connect(adb, hostport, instance="", run=subprocess.run):
    """adb connect + a check that the TV answers, then remember it (address for the tools, the rest in .urtv)."""
    if not (wireless.connect(adb, hostport, run) and verify(adb, hostport, run)):
        print(f"!! The TV did not answer at that address and port. On the TV: Developer options → Wireless debugging: is it on? "
              "Read «IP address & Port» on the main screen (the pairing dialog has another one) and run: "
              "optimize/find-tv.sh connect <IP:port>")
        return 1
    host, port = hostport.rsplit(":", 1)
    st = wireless.load_state()
    keep = {"instance": instance or (st.get("instance", "") if st.get("host") == host else ""), "paired": True}
    wireless.save_state({"host": host, "port": int(port), "mode": "tls", **keep})
    with open(HOST_FILE, "w") as f:
        f.write(host + "\n")
    print("Connected over Wireless debugging and remembered (the port changes after a restart; it is found again by itself).")
    print("Next: optimize/venv/bin/python -I optimize/tv.py --probe")
    return 0


PAIR_HELP = """Wireless debugging (Android 11 and newer; for a TV that keeps port 5555 closed).
On the TV: Settings → System → Developer options → Wireless debugging → on, then «Pair device with pairing code».
Keep that dialog open. Ask the person for the 6-digit code and the IP:port written in it (say that the code changes whenever
the dialog is closed), then:
    optimize/find-tv.sh pair <IP:port of the pairing dialog> <6-digit code>"""


def pair_cmd(args, run=subprocess.run, browse_fn=None):
    adb = wireless.find_adb()
    if not args:   # the steps first (what to open on the TV, which numbers to read); a missing adb comes after them
        print(PAIR_HELP)
        if not adb:
            print()
            print(wireless.MISSING_ADB)
            return 1
    elif not adb:
        print(wireless.MISSING_ADB)
        return 1
    ip = local_ip()
    browse_fn = browse_fn or (lambda svc, t: wireless.browse(svc, t, wireless.UdpTransport(ip)))
    if not args:
        hits = browse_fn(wireless.SERVICE_PAIRING, 3.0)
        if hits:
            print(f"The pairing dialog answers at {hits[0]['host']}:{hits[0]['port']}: you only need the code from the person, then:")
            print(f"    optimize/find-tv.sh pair {hits[0]['host']}:{hits[0]['port']} <6-digit code>")
        return 1
    if len(args) < 2:
        print("usage: optimize/find-tv.sh pair <IP:port of the pairing dialog> <6-digit code> [<IP:port of «IP address & Port»>]")
        return 2
    hostport, code = args[0], args[1]
    res = wireless.pair(adb, hostport, code, run)
    if not res["ok"]:
        print(f"!! Pairing failed: {res['reason']}. Ask the person to read the code and the IP:port on the TV again, then the same command.")
        return 1
    host = hostport.rsplit(":", 1)[0]
    print("Paired. Looking for the TV's connection port …")
    st = dict(wireless.load_state(), host=host, paired=True, instance=res["guid"], mode="pending")
    wireless.save_state(st)
    target = args[2] if len(args) > 2 else ""
    if target and not wireless.valid_hostport(target):
        print(f"!! «{target}» is not an IP:port.")
        return 1
    if not target:
        hit = wireless.pick_connect(browse_fn(wireless.SERVICE_CONNECT, 3.0), res["guid"], host)
        if hit:
            target = f"{hit['host']}:{hit['port']}"
    if not target:
        print("Paired, but the TV's connection port was not found on the Wi-Fi (the router may block multicast). Ask the person to read "
              "«IP address & Port» on the main Wireless debugging screen (not the pairing dialog), then: optimize/find-tv.sh connect <IP:port>")
        return 1
    return finish_connect(adb, target, res["guid"], run)


def connect_cmd(args, run=subprocess.run):
    adb = wireless.find_adb()
    if not adb:
        print(wireless.MISSING_ADB)
        return 1
    if len(args) != 1 or not wireless.valid_hostport(args[0]):
        print("usage: optimize/find-tv.sh connect <IP:port>   (the «IP address & Port» on the Wireless debugging screen)")
        return 2
    return finish_connect(adb, args[0], "", run)


def connects(host, port, timeout=1.0):
    """Only a real open port (an accepted connection), not «refused»."""
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def closed_cmd(host=None, knock=None, browse_fn=None):
    """After the person turned debugging off on the TV (for privacy): nothing may answer on ADB's ports any more. Looks at 5555, at the
    port of Wireless debugging we remembered, and at the mDNS announcement of a Wireless-debugging TV at that address. When the TV
    answers nothing at all (asleep, off) it cannot tell. The idea (confirm the port is closed afterwards) is from CleanYourTV's README
    (github.com/Gathin23/cleanyourtv, MIT)."""
    knock = knock or (lambda h, p: connects(h, p, 3.0))
    st = wireless.load_state()
    if host is None:
        host = os.environ.get("TV_HOST") or (open(HOST_FILE).read().strip() if os.path.exists(HOST_FILE) else st.get("host", ""))
    if not host:
        print("Don't know your TV's IP: run optimize/find-tv.sh first (it is saved there), or set TV_HOST.")
        return 2
    ports = [5555] + ([int(st["port"])] if st.get("host") == host and st.get("port") else [])
    open_ports = [p for p in ports if knock(host, p)]
    browse_fn = browse_fn or (lambda svc, t: wireless.browse(svc, t, wireless.UdpTransport(local_ip())))
    announced = [h for h in browse_fn(wireless.SERVICE_CONNECT, 3.0) if h["host"] == host]
    if open_ports or announced:
        what = [f"port {p}" for p in open_ports] + (["Wireless debugging (announced on the Wi-Fi)"] if announced else [])
        print("!! ADB still answers on this TV: " + ", ".join(what) + ". On the TV: Settings → System → Developer options → turn off "
              "USB debugging, Network debugging and Wireless debugging; then run this again.")
        return 1
    if not any(knock(host, p) for p in (8008, 8009, 8443, 9000)):   # Cast ports: is the TV awake and on the network at all?
        print("?? Can't tell: the TV does not answer on any port (asleep or off?). Wake it, then run this again.")
        return 2
    print("OK: ADB is closed on this TV (nothing answers on 5555 or on Wireless debugging). To use the tools again later, the person turns "
          "USB debugging (and Network debugging) back on in Developer options.")
    return 0


def main(argv):
    if argv[:1] == ["--closed"]:
        return closed_cmd()
    if argv[:1] == ["pair"]:
        return pair_cmd(argv[1:])
    if argv[:1] == ["connect"]:
        return connect_cmd(argv[1:])
    if argv[:1] == ["use"]:
        return use_cmd(argv[1:])
    return check() if "--check" in argv else scan()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
