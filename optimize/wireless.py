#!/usr/bin/env python3
"""Wireless debugging (Android 11+) for the setup tools. Standard library only, so find-tv.py and tv.py both load it.

Some Android 11+ TVs close port 5555 after every restart. What is left is «Wireless debugging»: the TV shows a pairing
dialog (a 6-digit code and an IP:port), then a main screen with «IP address & Port» that changes on every restart.

  * adb_shell (the Python ADB client tv.py uses) has no pairing and no TLS, which Wireless debugging needs, so this
    path uses the platform's own `adb` program when it is installed (adb pair, adb connect, adb shell). If it is not
    installed, find_adb() is empty and the tools say exactly what is missing.
  * The port is found again after a restart with mDNS (_adb-tls-connect._tcp), written here with the standard library
    only (a DNS-SD query to 224.0.0.251:5353 and a small parser). A router that blocks multicast gives no answer:
    then the person is asked for the new port (find-tv.sh connect <ip:port>).
  * The TV is remembered in wireless.json in the person's own .urtv folder, next to the ADB key (never in a report or a
    share). It holds the address, the last port and the TV's mDNS name.
"""
import json
import os
import re
import shutil
import socket
import struct
import subprocess
import sys
import time

MDNS_GROUP, MDNS_PORT = "224.0.0.251", 5353
SERVICE_CONNECT = "_adb-tls-connect._tcp.local"
SERVICE_PAIRING = "_adb-tls-pairing._tcp.local"
SERVICE_CAST = "_googlecast._tcp.local"
T_A, T_PTR, T_TXT, T_AAAA, T_SRV = 1, 12, 16, 28, 33
HOSTPORT = re.compile(r"^(\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})$")


def home_dir():
    return os.environ.get("URTV_HOME") or os.path.join(os.path.expanduser("~"), ".urtv")


def state_path():
    return os.path.join(home_dir(), "wireless.json")


def load_state():
    try:
        with open(state_path(), encoding="utf-8") as f:
            st = json.load(f)
        return st if isinstance(st, dict) else {}
    except (OSError, ValueError):
        return {}


def save_state(st):
    os.makedirs(home_dir(), exist_ok=True)
    tmp = state_path() + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(st, f)
    os.replace(tmp, state_path())


def state_for(host):
    """The wireless-debugging record when it is for this address and in use (mode «tls»), else None."""
    st = load_state()
    return st if st.get("mode") == "tls" and st.get("host") == host and st.get("port") else None


def valid_hostport(s):
    m = HOSTPORT.match(s or "")
    return bool(m) and all(int(x) < 256 for x in m.group(1).split(".")) and 0 < int(m.group(2)) < 65536


# ---------------------------------------------------------------------------------------------------- the adb program

MISSING_ADB = (
    "!! Wireless debugging needs the platform's own `adb` program, and it is not installed on this computer.\n"
    "   (The Python ADB client of these tools has no pairing and no TLS.) Install it, then the same command again:\n"
    "     Windows:  winget install -e --id Google.PlatformTools --source winget   (then a new Git Bash window)\n"
    "     macOS:    brew install android-platform-tools\n"
    "     Linux:    sudo apt install adb      (or your package manager's «android-tools»)\n"
    "     Termux (an Android phone): pkg install android-tools       Alpine / iSH (an iPhone or iPad): apk add android-tools\n"
    "   or unpack Google's «SDK Platform-Tools» zip into the .urtv folder in your home folder (as platform-tools).")


def find_adb():
    """The adb program: PATH first, then the .urtv folder, then the Android SDK folders. Empty if none."""
    exe = "adb.exe" if os.name == "nt" else "adb"
    found = shutil.which("adb")
    if found:
        return found
    cands = [os.path.join(home_dir(), "platform-tools", exe)]
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            cands.append(os.path.join(os.environ[var], "platform-tools", exe))
    for c in cands:
        if os.path.isfile(c):
            return c
    return ""


def parse_pair_output(text):
    """`adb pair` output -> {"ok", "guid", "reason"}. The guid (adb-<serial>-<suffix>) is also the TV's mDNS name."""
    text = (text or "").replace("\r", "")
    m = re.search(r"Successfully paired to \S+ \[guid=([^\]\s]+)\]", text)
    if m:
        return {"ok": True, "guid": m.group(1), "reason": ""}
    if "Successfully paired" in text:
        return {"ok": True, "guid": "", "reason": ""}
    low = text.lower()
    if "wrong password" in low or "password" in low:
        why = "the code is wrong or it has expired (it changes whenever the pairing dialog is closed and opened again)"
    elif "refused" in low or "failed to connect" in low or "timed out" in low or "cannot connect" in low:
        why = "nothing answered at that IP:port: the pairing dialog must be open on the TV, and the IP:port is the one shown there"
    elif "unknown host service" in low or "unknown command" in low or "usage" in low:
        why = "this adb is too old for `adb pair` (adb 30 or newer is needed)"
    else:
        why = text.strip().splitlines()[-1] if text.strip() else "no answer from adb"
    return {"ok": False, "guid": "", "reason": why}


def parse_connect_output(text):
    """`adb connect` output -> True when connected (also «already connected»)."""
    low = (text or "").lower()
    return ("connected to" in low or "already connected" in low) and "failed" not in low and "cannot" not in low \
        and "unable" not in low


def pair(adb, hostport, code, run=subprocess.run, timeout=60):
    if not valid_hostport(hostport):
        return {"ok": False, "guid": "", "reason": f"«{hostport}» is not an IP:port (the pairing dialog shows it, like 192.168.x.x:xxxxx)"}
    if not re.fullmatch(r"\d{6}", code or ""):
        return {"ok": False, "guid": "", "reason": "the pairing code is 6 digits"}
    try:
        r = run([adb, "pair", hostport, code], capture_output=True, text=True, timeout=timeout, input="\n")
    except subprocess.TimeoutExpired:
        return {"ok": False, "guid": "", "reason": "adb pair got no answer in a minute (is the pairing dialog still open on the TV?)"}
    except OSError as e:
        return {"ok": False, "guid": "", "reason": f"adb could not start ({e})"}
    return parse_pair_output((r.stdout or "") + (r.stderr or ""))


def connect(adb, hostport, run=subprocess.run, timeout=30):
    try:
        r = run([adb, "connect", hostport], capture_output=True, text=True, timeout=timeout)
    except (subprocess.TimeoutExpired, OSError):
        return False
    return parse_connect_output((r.stdout or "") + (r.stderr or ""))


# ---------------------------------------------------------------------------------------------------- mDNS

def _name(n):
    return b"".join(bytes([len(p)]) + p for p in n.encode("utf-8").split(b".") if p) + b"\0"


def build_query(questions):
    """A DNS-SD query for [(name, type)]. The «unicast response» bit is set and the sender's port is not 5353, so the
    answers come straight back to us (RFC 6762 §5.4, §6.7): no multicast group to join, on any operating system."""
    out = struct.pack(">HHHHHH", 0, 0, len(questions), 0, 0, 0)
    for name, qtype in questions:
        out += _name(name) + struct.pack(">HH", qtype, 0x8001)
    return out


def _read_name(data, off):
    labels, end, hops = [], None, 0
    while True:
        if off >= len(data):
            raise ValueError("name past the end")
        n = data[off]
        if n == 0:
            off += 1
            break
        if n & 0xC0 == 0xC0:
            if off + 1 >= len(data):
                raise ValueError("cut pointer")
            if end is None:
                end = off + 2
            off = ((n & 0x3F) << 8) | data[off + 1]
            hops += 1
            if hops > 20:
                raise ValueError("pointer loop")
            continue
        labels.append(data[off + 1:off + 1 + n].decode("utf-8", "replace"))
        off += 1 + n
    return ".".join(labels), (end if end is not None else off)


def parse_message(data):
    """The records of a DNS message: [(name, type, value)]. PTR -> target name, SRV -> (port, target), A/AAAA -> address,
    TXT -> list of strings. A broken packet gives what could be read before the break (never an exception)."""
    recs = []
    try:
        _, _, qd, an, ns, ar = struct.unpack(">HHHHHH", data[:12])
        off = 12
        for _ in range(qd):
            _, off = _read_name(data, off)
            off += 4
        for _ in range(an + ns + ar):
            name, off = _read_name(data, off)
            rtype, _cls, _ttl, rdlen = struct.unpack(">HHIH", data[off:off + 10])
            off += 10
            rd = data[off:off + rdlen]
            if len(rd) < rdlen:
                break
            if rtype == T_PTR:
                value = _read_name(data, off)[0]
            elif rtype == T_SRV and rdlen >= 7:
                value = (struct.unpack(">H", rd[4:6])[0], _read_name(data, off + 6)[0])
            elif rtype == T_A and rdlen == 4:
                value = socket.inet_ntoa(rd)
            elif rtype == T_AAAA and rdlen == 16:
                value = socket.inet_ntop(socket.AF_INET6, rd)
            elif rtype == T_TXT:
                value, i = [], 0
                while i < len(rd):
                    n = rd[i]
                    value.append(rd[i + 1:i + 1 + n].decode("utf-8", "replace"))
                    i += 1 + n
            else:
                value = None
            recs.append((name, rtype, value))
            off += rdlen
    except (ValueError, struct.error, IndexError):
        pass
    return recs


class UdpTransport:
    """The real thing: one UDP socket on this computer's Wi-Fi address (so a computer with several networks asks on the right
    one), send to the mDNS group, read the answers that come back."""

    def __init__(self, iface_ip=None):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 255)
        try:
            self.sock.bind((iface_ip or "", 0))
            if iface_ip:
                self.sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_IF, socket.inet_aton(iface_ip))
        except OSError:
            pass

    def send(self, packet):
        try:
            self.sock.sendto(packet, (MDNS_GROUP, MDNS_PORT))
        except OSError:
            pass

    def recv(self, timeout):
        self.sock.settimeout(max(timeout, 0.01))
        try:
            return self.sock.recvfrom(9000)[0]
        except (socket.timeout, OSError):
            return None

    def close(self):
        self.sock.close()


def browse(service, timeout=3.0, transport=None, clock=time.monotonic):
    """Instances of an mDNS service on the Wi-Fi: [{"instance", "host", "port"}] (host is an IPv4 address; an instance whose
    address could not be learnt is left out). `transport` has send(packet) and recv(timeout) -> bytes|None."""
    own = transport is None
    transport = transport or UdpTransport()
    ptrs, srv, addr = set(), {}, {}
    suffix = "." + service

    def absorb(packet):
        for name, rtype, value in parse_message(packet):
            if rtype == T_PTR and name.lower() == service and isinstance(value, str):
                ptrs.add(value)
            elif rtype == T_SRV and value:
                srv[name] = value
            elif rtype == T_A and isinstance(value, str):
                addr[name.lower()] = value

    def listen(seconds):
        end = clock() + seconds
        while True:
            left = end - clock()
            if left <= 0:
                return
            pkt = transport.recv(left)
            if pkt:
                absorb(pkt)

    try:
        transport.send(build_query([(service, T_PTR)]))
        listen(timeout / 2)
        transport.send(build_query([(service, T_PTR)]))   # a second ask: a sleeping TV, a lost packet
        listen(timeout / 2)
        missing = [i for i in ptrs if i not in srv or srv[i][1].lower() not in addr]
        if missing:   # an answer without its details: ask for them by name
            qs = []
            for i in missing:
                qs.append((i, T_SRV))
                if i in srv:
                    qs.append((srv[i][1], T_A))
            transport.send(build_query(qs))
            listen(min(1.5, timeout))
    finally:
        if own:
            transport.close()
    found = []
    for inst in sorted(ptrs):
        if inst in srv and srv[inst][1].lower() in addr:
            port, target = srv[inst]
            short = inst[:-len(suffix)] if inst.lower().endswith(suffix) else inst
            found.append({"instance": short, "host": addr[target.lower()], "port": port})
    return found


def pick_connect(found, instance="", host=""):
    """Which of the `_adb-tls-connect` hits is our TV: the one with the remembered name, else the one at the remembered
    address; None when neither is there (never a different device)."""
    for f in found:
        if instance and f["instance"] == instance:
            return f
    for f in found:
        if host and f["host"] == host:
            return f
    return None


def rediscover(state, browse_fn=browse):
    """After a restart the port is new: look the TV up by name or address. Returns the hit or None."""
    return pick_connect(browse_fn(SERVICE_CONNECT, 3.0), state.get("instance", ""), state.get("host", ""))


# ---------------------------------------------------------------------------------------------------- the device

class AdbCli:
    """The same few calls as adb_shell's AdbDeviceTcp (connect, shell, pull, close), through the adb program, for a TV that
    is reached by Wireless debugging. The port is looked up again by mDNS when it no longer answers."""

    chunk = 24_000   # a Windows command line stops at 32 KB, far below the 96 KB pieces of a plain upload

    def __init__(self, host, state, timeout=60.0, run=subprocess.run, browse_fn=browse):
        self.host, self.state, self.timeout = host, dict(state), timeout
        self.run, self.browse = run, browse_fn
        self.adb = find_adb()
        if not self.adb:
            sys.exit(MISSING_ADB.replace("Wireless debugging needs", "This TV is reached by Wireless debugging, which needs"))
        self.connected = False

    @property
    def serial(self):
        return f"{self.host}:{self.state['port']}"

    def connect(self, **_ignored):   # rsa_keys / auth_timeout_s: adb has its own key, made when the TV was paired
        if connect(self.adb, self.serial, self.run, min(self.timeout, 30)):
            self.connected = True
            return
        hit = rediscover(self.state, self.browse)
        if hit and hit["port"] != self.state["port"]:
            self.state["port"], self.state["host"] = hit["port"], hit["host"]
            self.host = hit["host"]
            try:
                save_state(dict(load_state(), **{"host": self.host, "port": hit["port"], "mode": "tls"}))
            except OSError:
                pass
            if connect(self.adb, self.serial, self.run, min(self.timeout, 30)):
                self.connected = True
                return
        raise ConnectionError(
            "the TV's Wireless-debugging port does not answer and was not found on the Wi-Fi. Ask the person to open "
            "Settings → Developer options → Wireless debugging and read «IP address & Port» (not the pairing one), then: "
            "optimize/find-tv.sh connect <that IP:port>")

    def shell(self, cmd, read_timeout_s=None):
        try:
            r = self.run([self.adb, "-s", self.serial, "shell", cmd], capture_output=True, text=True,
                         timeout=read_timeout_s or self.timeout, errors="replace")
        except subprocess.TimeoutExpired as e:
            raise TimeoutError("adb shell timed out") from e
        out = (r.stdout or "") + (r.stderr or "")
        if re.match(r"\s*error: (device|closed|no devices|protocol|more than one)", out) or "device offline" in out[:80]:
            raise ConnectionError(out.strip().splitlines()[0])
        return out

    def install_stream(self, command, local, timeout):
        """`adb exec-in <command>` with the APK as its standard input (tv.py --install-stream): no file on the TV."""
        with open(local, "rb") as f:
            try:
                r = self.run([self.adb, "-s", self.serial, "exec-in", command], stdin=f, capture_output=True, timeout=timeout)
            except subprocess.TimeoutExpired as e:
                raise TimeoutError("adb exec-in timed out") from e
        out = ((r.stdout or b"") + (r.stderr or b"")).decode("utf-8", "replace")
        if re.match(r"\s*error: (device|closed|no devices|protocol|more than one)", out) or "device offline" in out[:80]:
            raise ConnectionError(out.strip().splitlines()[0])
        return out

    def pull(self, remote, local):
        r = self.run([self.adb, "-s", self.serial, "pull", remote, local], capture_output=True, text=True,
                     timeout=self.timeout * 4, errors="replace")
        if r.returncode != 0:
            tail = ((r.stdout or "") + (r.stderr or "")).strip().splitlines()
            raise ConnectionError(tail[-1] if tail else "adb pull failed")

    def close(self):
        self.connected = False
