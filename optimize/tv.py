#!/usr/bin/env python3
"""Tiny ADB-over-Wi-Fi client (no adb binary needed; avoids macOS Local Network blocking).

    tv.py --probe                            (TV_HOST env or optimize/.tv_host)
    tv.py 'pm list packages'                 one shell command
    tv.py --batch < cmds.txt                 one command per line, same connection
    tv.py --pull /sdcard/x.png x.png
    tv.py --push-b64 local.apk /data/local/tmp/x.apk   chunked base64 upload with md5 check
    tv.py --shot out.png                     screenshot from the TV
    tv.py --bg NAME 'cmd'                    a long command, run in the background on the TV and polled (no 60 s silence)
    tv.py --reboot                           restart the TV (sent once, never retried) and wait until it is back from a new boot
    tv.py --wait                             wait until the TV answers again (after a reboot), up to 10 min
    tv.py --check                            is the device answering the TV the setup started on? (saves it if none yet)
    tv.py --new-tv                           the device answering now is the TV to work on from here (replaces the saved one)

The TV the setup started on is remembered (serial, make and model) in tv-id.json, next to the ADB key in the person's own
home folder (~/.urtv, %USERPROFILE%\\.urtv on Windows), local only and outside the clone: a new clone of UrTV is the same
computer, and the TV still trusts its key. Its path is never printed (it holds the user name). Every command
that changes the TV checks, once per connection, that the device answering is that TV, and stops with a !! line if not.
Read-only ones (getprop, dumpsys, pm list, settings get, --probe, --pull, --shot) don't need it.

Built for slow or flaky Wi-Fi: connecting and every command are retried, a dropped connection is
reopened, and an interrupted upload resumes where it stopped.
"""
import base64
import hashlib
import json
import os
import re
import shlex
import shutil
import sys
import time
from pathlib import Path

for _stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page (Arabic would crash) and with \r\n (Git Bash keeps the \r)
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

try:
    from adb_shell.adb_device import AdbDeviceTcp
    from adb_shell.auth.keygen import keygen
    from adb_shell.auth.sign_pythonrsa import PythonRSASigner
except ModuleNotFoundError:
    sys.exit("!! The Python packages aren't in optimize/venv. Run optimize/setup.sh: it must end with "
             "«✓ Python ADB client», else it says why. Then the same command again.")

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)   # python -I does not look next to the script
import wireless   # noqa: E402  (Wireless debugging, Android 11+: pairing, the adb program, mDNS)
HOST_FILE = os.path.join(HERE, ".tv_host")  # written by find-tv.sh, git-ignored
# What identifies this computer to the TV (its ADB key) and which TV the setup is for live in the person's home, not in the
# clone: a new clone gets no new key (the TV would ask «Allow USB debugging?» for a computer it already trusts, or nothing
# would connect), and it knows the TV already. URTV_HOME moves it (the tests do).
HOME_DIR = os.environ.get("URTV_HOME") or os.path.join(os.path.expanduser("~"), ".urtv")
HOST = os.environ.get("TV_HOST") or (open(HOST_FILE).read().strip() if os.path.exists(HOST_FILE) else "")
if not HOST:
    sys.exit("Don't know your TV's IP. Run optimize/find-tv.sh, or set TV_HOST=192.168.x.y")
PORT = int(os.environ.get("TV_PORT", "5555"))
KEY = os.path.join(HOME_DIR, "adbkey")
OLD_KEY = os.path.join(HERE, "adbkey", "adbkey")   # where the key lived before: inside the clone
# A TV on weak Wi-Fi (TCL G10 on Android 12 was the worst) stops answering for a minute at a time, or wakes slowly from standby:
# nine reports said «TV not answering after 4 tries», which gave up after 30 seconds. Now 6 tries with a longer pause each (5, 10,
# 15, 20, 30 s: 80 s), and 10 for the long jobs (uploads, background installs: 5 to 30 s apart, over 3 minutes). TV_TRIES sets both.
TRIES = int(os.environ.get("TV_TRIES", "6"))
LONG_TRIES = int(os.environ.get("TV_TRIES", "10"))
MAX_PAUSE_S = 30
ID_FILE = os.path.join(os.environ.get("URTV_RUN_DIR") or HOME_DIR, "tv-id.json")
OLD_ID_FILE = os.path.join(HERE, "run", "tv-id.json")

# One round trip: who is answering. The id is the first usable one of ro.serialno, ro.boot.serialno and the Android id (random, per TV): the
# same policy as optimize/run.sh's, which asks `tv.py --id`. A placeholder (cheap boxes all carry one) is never an id.
IDENT_CMD = ('echo "serial=$(getprop ro.serialno)"; echo "bootserial=$(getprop ro.boot.serialno)"; '
             'echo "androidid=$(settings get secure android_id)"; '
             'echo "manufacturer=$(getprop ro.product.manufacturer)"; echo "model=$(getprop ro.product.model)"')
PLACEHOLDERS = ("", "unknown", "null", "none", "n/a", "0123456789abcdef", "1234567890abcdef", "1234567890", "0000000000", "00000000")


def is_placeholder(value):
    v = (value or "").strip().lower()
    return v in PLACEHOLDERS or len(v) < 4 or len(set(v)) == 1   # empty, a known placeholder, too short, or one repeated character


def parse_identity(out):
    """{serial, android_id, manufacturer, model}. Each candidate is checked before the next is looked at: a placeholder
    ro.serialno does not hide a real ro.boot.serialno."""
    got = dict(l.split("=", 1) for l in out.replace("\r", "").splitlines() if "=" in l)
    serial = next((got.get(k, "").strip() for k in ("serial", "bootserial") if not is_placeholder(got.get(k, ""))), "")
    aid = got.get("androidid", "").strip()
    return {"serial": serial, "android_id": "" if is_placeholder(aid) else aid,
            "manufacturer": got.get("manufacturer", "").strip(), "model": got.get("model", "").strip()}


def unique_id(ident):
    """The one id run.sh and this file share: the serial, else «aid:<android id>», else nothing (the address then)."""
    if ident.get("serial"):
        return ident["serial"]
    return "aid:" + ident["android_id"] if ident.get("android_id") else ""


def tv_name(ident):
    return " ".join(x for x in (ident.get("manufacturer"), ident.get("model")) if x) or "a device that does not say what it is"


def unreadable(ident):
    """An identity with no serial, no make and no model says nothing about which device answered (a TV that has not finished
    waking up, a shell that returned nothing): comparing it with the saved one would pass."""
    return not (ident.get("serial") or ident.get("android_id") or ident.get("manufacturer") or ident.get("model"))


def same_tv(saved, now):
    """Same device? The first unique id both sides have (serial, then Android id) decides, and a model never overrides it. A saved unique id
    that the TV cannot show now is NOT a reason to fall back to the model (two TVs of one model are the case this guards). Only when the
    saved identity has no unique id at all (an older record, a placeholder serial) do the make and model decide."""
    for k in ("serial", "android_id"):
        if saved.get(k) and now.get(k):
            if saved[k].lower() != now[k].lower():
                return False   # another id: another TV, whatever the model says
            break              # the same id: still not the same TV if the make or model say otherwise (one made-up serial on many boxes)
    else:
        if saved.get("serial") or saved.get("android_id"):
            return False
    for k in ("manufacturer", "model"):
        if saved.get(k) and now.get(k) and saved[k].lower() != now[k].lower():
            return False
    return True


def load_identity():
    if not os.environ.get("URTV_RUN_DIR"):
        move_old(OLD_ID_FILE, ID_FILE)
    try:
        with open(ID_FILE, encoding="utf-8") as f:
            ident = json.load(f)
        if not isinstance(ident, dict):
            return None
        # a record from an older version can hold a placeholder as the serial (it only rejected «unknown» and «null»): that was never an id
        for k in ("serial", "android_id"):
            if k in ident and is_placeholder(str(ident[k])):
                ident[k] = ""
        return ident
    except (OSError, ValueError):
        return None


def save_identity(ident):
    try:
        os.makedirs(os.path.dirname(ID_FILE), exist_ok=True)
        tmp = ID_FILE + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(ident, f)
        os.replace(tmp, ID_FILE)
    except OSError as e:
        print(f"   (could not remember which TV this is: {e})", file=sys.stderr)


def check_identity(out):
    """The device that answered `out` must be the TV the setup started on; the first one seen is saved. Stops with a !!
    line that names both models (never a serial: this line can end up in run.log) when it is not."""
    now = parse_identity(out)
    if unreadable(now):
        sys.exit("!! Could not read which TV this is (it gave no serial, make or model), so nothing was changed. Wait a "
                 "minute, check the TV is awake and on Wi-Fi, then run the same command again.")
    saved = load_identity()
    if saved is None:
        save_identity(now)
        return
    if same_tv(saved, now):   # an older record learns the ids it did not have (the Android id) once the TV is confirmed
        merged = {**saved, **{k: v for k, v in now.items() if v and not saved.get(k)}}
        if merged != saved:
            save_identity(merged)
    if not same_tv(saved, now):
        was, is_now = tv_name(saved), tv_name(now)
        no_id = (saved.get("serial") or saved.get("android_id")) and not any(saved.get(k) and now.get(k) for k in ("serial", "android_id"))
        if no_id:   # the saved TV had an id this answer does not show: not downgraded to a model match
            is_now += " (that did not show its serial this time)"
        elif was.lower() == is_now.lower():
            is_now += " (another one of the same model)"
        sys.exit(f"!! This is not the TV the setup started on: that was a {was}, and a {is_now} answers now. Nothing was "
                 "changed. If two devices are on this Wi-Fi, run optimize/find-tv.sh and pick the TV (or set TV_HOST), then "
                 "the same command again. If this really is a new TV: optimize/venv/bin/python -I optimize/tv.py --new-tv, "
                 "then optimize/run.sh.")


# Commands that only read. Anything else (installs, pm enable/disable, settings put, am, content, input, rm, ...) is
# treated as changing the TV and checked first. When in doubt a command counts as changing: that costs one getprop.
READ_ONLY = ("getprop", "dumpsys", "pm list", "pm path", "pm query-services", "pm resolve-activity", "settings get", "cat",
             "ls", "ps", "top", "df", "du", "grep", "egrep", "wc", "md5sum", "head", "tail", "echo", "printf", "id",
             "uname", "test", "[", "[[", "true", "sleep", "cmd package resolve-activity", "cmd package query-services",
             "cmd package list", "device_config get", "appops get", "am get-standby-bucket", "cmd package query-activities")
# Commands that read in one form and write in another («wm size 1280x720», «wm density 240», «date -s …» change the TV): only
# the reading form counts, whatever follows.
READ_FORMS = (re.compile(r"wm (size|density)\s*$"), re.compile(r"date(\s+-u)?(\s+\+\S*)?\s*$"),
              re.compile(r"sed -n\s+\d+p\s*$"))   # «sed -n 1p»: the first line of what came before
SHELL_WORDS = ("for", "do", "done", "if", "then", "else", "elif", "fi", "while", "{", "}", "!")


def is_read_only(cmd):
    # single quotes hold text only; a double-quoted string that runs a command ($( or `) is looked into
    cmd = re.sub(r"'[^']*'", "''", cmd)
    cmd = re.sub(r'"[^"$`]*"', '""', cmd)
    cmd = re.sub(r"\d*>+\s*(/dev/null|&\d)", " ", cmd)
    if ">" in cmd:                       # writes a file
        return False
    for seg in re.split(r"[;&|\n()`]+|\$\(", cmd):
        seg = re.sub(r"^(\s|\"|'|\w+=)*", "", seg)
        while seg.split(None, 1)[:1] and seg.split(None, 1)[0] in SHELL_WORDS + ("for",):
            first, _, rest = seg.partition(" ")
            seg = "" if first == "for" else rest   # «for x in a b» only names words
        if not seg.strip():
            continue
        if not (any(seg == r or seg.startswith(r + " ") for r in READ_ONLY) or any(p.match(seg.strip()) for p in READ_FORMS)):
            return False
    return True


def shell_line(args):
    """The command line to send for these arguments. One argument is a whole command, as written ('pm list packages'); several are
    words of one, each quoted so that one with a space or a ; stays one word on the TV (a path as Git Bash may have rewritten it
    is put right first)."""
    if len(args) == 1:
        return tv_path(args[0])
    return " ".join(shlex.quote(tv_path(x)) for x in args)


def move_old(old, new):
    """The first time: a file this clone kept inside optimize/ moves to the home folder, unless one is there already
    (then the clone's copy is a stale one and stays as it is)."""
    try:
        if os.path.isfile(old) and not os.path.exists(new):
            os.makedirs(os.path.dirname(new), exist_ok=True)
            shutil.move(old, new)
            return True
    except OSError:
        pass
    return False


def new_dev(timeout):
    """A connection object for the TV. Port 5555 by default (adb_shell). A TV that was paired for Wireless debugging (find-tv.sh
    pair) goes through the adb program instead, on the port it has now (looked up again by mDNS after a restart); TV_PORT set
    by hand always means plain TCP."""
    st = None if "TV_PORT" in os.environ else wireless.state_for(HOST)
    if st:
        return wireless.AdbCli(HOST, st, timeout)
    return AdbDeviceTcp(HOST, PORT, default_transport_timeout_s=timeout)


def upload_chunk():
    # 40 000 characters: the size reports from Android 12 TVs say works (96 000 stalled with TcpTimeoutException on some)
    return wireless.AdbCli.chunk if "TV_PORT" not in os.environ and wireless.state_for(HOST) else 40_000


def signer():
    os.makedirs(os.path.dirname(KEY), exist_ok=True)
    try:
        os.chmod(os.path.dirname(KEY), 0o700)
    except OSError:
        pass
    if not os.path.exists(KEY) and move_old(OLD_KEY, KEY):
        move_old(OLD_KEY + ".pub", KEY + ".pub")
        print("   (the ADB key moved out of this folder into your home folder, in .urtv: a new clone of UrTV will use it too)",
              file=sys.stderr)
    if not os.path.exists(KEY):
        keygen(KEY)
    return PythonRSASigner(open(KEY + ".pub").read(), open(KEY).read())


# Commands whose effect must not happen twice: they add or delete by position, or press a key. If the connection drops after
# the TV ran one, the reply is lost and a blind retry would run it again (a second task, the wrong photo deleted, a key
# pressed twice). These are sent once; reads and settings (the same value twice is harmless) keep retrying.
# HOME and WAKEUP twice land where once does: the setup presses them often, on Wi-Fi that drops, so they keep retrying.
SEND_ONCE = re.compile(r"^\s*input\s(?!keyevent\s+(KEYCODE_)?(HOME|WAKEUP)\s*$)|--es op (add|del|done)\b|-a \S*\.SEEK_BY\b")


def send_once(cmd):
    return bool(SEND_ONCE.search(cmd))


class TV:
    """One connection, reopened on failure; every call retried with a growing pause."""

    def __init__(self, guard=False, tries=None, transport_s=60.0):
        self.dev = None
        self.transport_s = transport_s   # how long one read from the TV may stay silent: 180 s for uploads, decodes, installs
        self.guard = guard   # this run changes the TV: every new connection first checks it is the right one
        self.tries = tries or TRIES
        self.last_ok = 0.0

    def _open(self):
        self.close()
        dev = new_dev(self.transport_s)
        dev.connect(rsa_keys=[signer()], auth_timeout_s=120)
        if self.guard:
            try:
                for look in range(3):   # an empty answer is read again before it counts as unreadable
                    ident_out = dev.shell(IDENT_CMD, read_timeout_s=30) or ""
                    if not unreadable(parse_identity(ident_out)):
                        break
                    time.sleep(3)
                check_identity(ident_out)
            except BaseException:   # a refusal (SystemExit) or a dropped connection: this one is not used
                try:
                    dev.close()
                except Exception:  # noqa: BLE001
                    pass
                raise
        self.dev = dev

    def close(self):
        if self.dev is not None:
            try:
                self.dev.close()
            except Exception:
                pass
            self.dev = None

    def call(self, fn, *args, **kw):
        for attempt in range(self.tries):
            try:
                if self.dev is None:
                    self._open()
                r = getattr(self.dev, fn)(*args, **kw)
                self.last_ok = time.time()
                return r
            except Exception as e:  # noqa: BLE001  (network errors come in many types)
                self.close()
                if attempt == self.tries - 1:
                    sys.exit(f"!! TV not answering after {self.tries} tries ({type(e).__name__}: {e}). "
                             "Check the TV is on and on Wi-Fi, then run the same command again.")
                wait = backoff(attempt)
                print(f"   (connection problem: {type(e).__name__}; retrying in {wait}s)", file=sys.stderr)
                time.sleep(wait)

    def shell(self, cmd, timeout=120):
        if send_once(cmd):
            return self.once(cmd, timeout)
        return self.call("shell", cmd, read_timeout_s=timeout) or ""

    def once(self, cmd, timeout=120):
        """Send a command that must not run twice. A connection that may have gone stale is first tested with a harmless
        command (that one is retried as usual), so the single send meets a live one; if the reply is lost anyway, it is
        not sent again: the person is told to look first."""
        if self.dev is None or time.time() - self.last_ok > 15:
            self.call("shell", "true", read_timeout_s=30)
        try:
            r = self.dev.shell(cmd, read_timeout_s=timeout) or ""
            self.last_ok = time.time()
            return r
        except Exception as e:  # noqa: BLE001
            self.close()
            sys.exit(f"!! The TV stopped answering while it ran this command ({type(e).__name__}), so it may or may not have "
                     "been done. It was NOT sent again. Look first (tasks list, photos list, or the TV's screen), then run "
                     "it again only if it did not happen.")

    def soft(self, cmd, timeout=30, tries=2):
        """Like shell, but a TV that does not answer gives None instead of ending the run. For looking at a job that is
        already running on the TV: while it installs a big APK some TVs stop answering ADB for minutes, and that is not a
        failure. (A refusal by the identity guard still stops.)"""
        for attempt in range(tries):
            try:
                if self.dev is None:
                    self._open()
                return self.dev.shell(cmd, read_timeout_s=timeout) or ""
            except Exception as e:  # noqa: BLE001
                self.close()
                if attempt < tries - 1:
                    time.sleep(backoff(attempt))
        return None


def backoff(attempt):
    """Seconds to wait after the attempt-th failure (0 is the first): 5, 10, 15, 20, 25, 30, 30, …"""
    return min(5 * (attempt + 1), MAX_PAUSE_S)


ANDROID_ROOT = re.compile(r"/(?:data|sdcard|storage|system|vendor|product|apex|mnt|proc|dev|cache|odm|oem)(?=/|$)")


def tv_path(arg):
    """A path on the TV, as Git Bash on Windows may have rewritten it: it turns an argument like /data/local/tmp/x
    into C:/Program Files/Git/data/local/tmp/x before Python sees it. Cut its own folder back off."""
    if os.name != "nt" or not re.match(r"[A-Za-z]:[\\/]", arg):
        return arg
    s = arg.replace("\\", "/")
    for m in ANDROID_ROOT.finditer(s):
        if is_git_folder(s[:m.start()]):
            return s[m.start():]
    return arg


def is_git_folder(folder):   # Git Bash's own folder is the one with its bash in it
    return os.path.exists(folder + "/usr/bin/bash.exe") or os.path.exists(folder + "/bin/bash.exe")


JOB = "/data/local/tmp/urtv-job-"
SLOW_READ_S = 180.0   # read timeout for the upload, md5, decode and pm install steps (the library's own is 60 s)
POLL_S = 3                                              # between two looks at a background job
LIMIT_S = int(os.environ.get("TV_JOB_LIMIT_S", "600"))  # a job silent for longer than this is given up on


def run_bg(tv, name, cmd, progress=None, limit_s=None):
    """Run a command that can take minutes on a weak TV (decoding or installing a big APK) in the background on the TV,
    then look at it every few seconds with short commands: a shell command that prints nothing for 60 s fails, even
    though the TV is still working. Its output and exit code land in /data/local/tmp/urtv-job-<name>.out / .rc.
    Safe to repeat: a job still running is joined, not started twice. Returns (exit code, output).
    `progress` is a TV command that prints how far it is (a number); it is shown as a percentage of 100 if given."""
    assert "'" not in cmd and re.fullmatch(r"[\w.-]+", name), "background command: no quote marks"
    j = JOB + name
    alive = f"grep -q {JOB}{name} /proc/$(cat {j}.pid)/cmdline 2>/dev/null"   # our own sh -c, not a recycled pid
    started = tv.shell(
        f"if {alive}; then echo joined; else rm -f {j}.rc {j}.out; "
        f"nohup sh -c '({cmd}) > {j}.out 2>&1; echo $? > {j}.rc.x; mv {j}.rc.x {j}.rc; rm -f {j}.pid' "
        f"> /dev/null 2>&1 & echo $! > {j}.pid; echo started; fi").strip()
    if started == "joined":
        print(f"   ({name} is already running on the TV: waiting for it)", file=sys.stderr)
    began = time.time()
    limit = LIMIT_S if limit_s is None else limit_s
    silent = 0   # looks in a row the TV did not answer (it is busy with the job: not a failure)
    while True:
        waited = int(time.time() - began)
        # alive is checked first: the job writes .rc before it lets go of .pid, so "not alive, no .rc" means it died
        got = tv.soft(f"if {alive}; then echo running $({progress or 'echo'}); elif [ -f {j}.rc ]; then echo done; "
                      f"else echo dead; fi", timeout=SLOW_READ_S)
        state = (got or "").split()
        if got is None:
            silent += 1
        else:
            silent = 0
            if state[:1] == ["done"]:
                break
            if state[:1] != ["running"]:
                # it died with no answer: some TVs end what a shell left running, at once or a few seconds after that shell
                # closes (a TCL on Android 14 did it after the first look). Do it the old, direct way. Both jobs (decode,
                # pm install) are safe to run again from the start.
                print(f"   ({name}: this TV doesn't keep background jobs; running it directly)", file=sys.stderr)
                tv.shell(f"rm -f {j}.rc {j}.out {j}.pid")
                # a command that prints nothing for 60 s fails whatever its read timeout: dots are printed while it runs
                out = tv.shell(f"({cmd}) 2>&1 & p=$!; while kill -0 $p 2>/dev/null; do echo -n .; sleep 15; done; wait $p; "
                               "echo __rc=$?", timeout=limit).rstrip()
                body, _, rc = out.rpartition("__rc=")
                body = re.sub(r"^\.+", "", body)
                return (int(rc) if rc.strip().lstrip("-").isdigit() else 1), body.rstrip()
        if waited >= limit:
            sys.exit(f"!! The {name} job on the TV is still running after {limit // 60} minutes"
                     + (" and the TV has stopped answering" if silent else "")
                     + ". It is left running there: run the same command again to wait for it.")
        shown = f"{waited}s"
        if progress and len(state) > 1 and state[1].isdigit():
            shown = f"{state[1]}%"
        if silent:
            shown += " (the TV is busy and not answering yet)"
        print(f"\r   {name}: {shown}   ", end="", file=sys.stderr, flush=True)
        time.sleep(POLL_S)
    if time.time() - began > POLL_S:
        print(file=sys.stderr)
    rc = tv.shell(f"cat {j}.rc").strip()
    out = tv.shell(f"cat {j}.out").rstrip()
    tv.shell(f"rm -f {j}.rc {j}.out {j}.pid")
    return (int(rc) if rc.lstrip("-").isdigit() else 1), out


def cut_back(tmp, i):
    """Make `tmp` exactly its first `i` characters again, whatever state a lost reply left it in. The first version, `head -c i tmp >
    tmp.x && mv tmp.x tmp`, failed with "mv: bad '….b64.x': No such file or directory" at 0% (two reports): at the start there is no
    file to cut, and some TVs' head makes no .x for an empty cut. So: nothing to keep (i is 0) -> remove it; else cut only if it is
    there; never mv a file that is not."""
    if i == 0:
        return f"rm -f {tmp} {tmp}.x"
    return f"if [ -f {tmp} ]; then head -c {i} {tmp} > {tmp}.x && [ -f {tmp}.x ] && mv {tmp}.x {tmp}; fi"


def push_b64(tv, local, remote, chunk=None):   # one argument may not exceed 128 KB on Android (32 KB on a Windows command line)
    """Upload in base64 pieces; on a retry, pieces already on the TV are kept (resumes, not restarts)."""
    chunk = chunk or upload_chunk()
    data = open(local, "rb").read()
    enc = base64.b64encode(data).decode()
    folder = remote.rsplit("/", 1)[0] or "/"
    if tv.shell(f"[ -d '{folder}' ] && echo y").strip() != "y":
        sys.exit(f"!! {folder} is not a folder on the TV, so nothing can be uploaded to {remote}.")
    tmp = remote + ".b64"
    have = tv.shell(f"[ -f {tmp} ] && wc -c < {tmp} || echo 0").strip()
    done = int(have) if have.isdigit() and int(have) % chunk == 0 and int(have) <= len(enc) else 0
    if done:   # a killed run may have left a piece in twice: resume only if what is there is exactly right
        theirs = tv.shell(f"md5sum {tmp}").split()[0] if tv.shell(f"[ -f {tmp} ] && echo y").strip() else ""
        if theirs != hashlib.md5(enc[:done].encode()).hexdigest():
            done = 0
    if done == 0:
        tv.shell(f"rm -f {tmp} {remote}")
    elif done:
        print(f"resuming upload at {done * 100 // len(enc)}%")
    for i in range(done, len(enc), chunk):
        piece, want = enc[i:i + chunk], str(min(i + chunk, len(enc)))
        size = tv.shell(f"echo -n '{piece}' >> {tmp}; wc -c < {tmp}", SLOW_READ_S).strip()   # write and check in one call
        for _ in range(5):    # a retried call wrote the piece twice, or not at all: cut back and write it again
            if size == want:
                break
            size = tv.shell(cut_back(tmp, i) + f"; echo -n '{piece}' >> {tmp}; wc -c < {tmp}", SLOW_READ_S).strip()
        if size != want:
            free = tv.shell(f"df -h {folder} | tail -1").strip()
            sys.exit(f"!! upload keeps failing at {i * 100 // len(enc)}% (the TV wrote {size or 'nothing'} of {want}; "
                     f"space there: {free}). Run the same command again.")
        print(f"\r   uploading {Path(local).name}: {int(want) * 100 // len(enc)}%", end="", flush=True)
    print()
    # the decode of a big file is the slowest step on a weak TV and prints nothing: run it in the background and watch it
    if tv.shell(f"[ -f {tmp} ] && echo y").strip():   # not there: an earlier run decoded it already (md5 below decides)
        rc, out = run_bg(tv, "decode", f"base64 -d {tmp} > {remote}.part",
                         progress=f"echo $(( $(wc -c < {remote}.part 2>/dev/null || echo 0) / {len(data) // 100 + 1} ))")   # no 32-bit overflow
        if rc != 0:
            tv.shell(f"rm -f {remote}.part")
            sys.exit(f"!! The decode on the TV failed (exit {rc}: {out or 'no message'}). Run the same command again.")
        tv.shell(f"[ -f {remote}.part ] && mv {remote}.part {remote}; rm -f {tmp}")   # safe to repeat if the reply is lost
    remote_md5 = ""   # md5sum of a big APK prints nothing for over a minute on a weak TV: a background job, like the decode
    if tv.shell(f"[ -f {remote} ] && echo y").strip():
        rc, out = run_bg(tv, "md5", f"md5sum {remote}")
        remote_md5 = out.split()[0] if rc == 0 and out.split() else ""
    local_md5 = hashlib.md5(data).hexdigest()
    if remote_md5 != local_md5:
        tv.shell(f"rm -f {remote} {tmp}")
        sys.exit(f"md5 mismatch: {remote_md5} != {local_md5} (run the same command again)")
    print(f"pushed {local} -> {remote} ({len(data)} bytes, md5 ok)")


def boot_marker(dev):
    """Which boot the TV is in: the kernel's boot id (new on every boot), else the uptime (it only grows inside one boot)."""
    bid = dev.shell("cat /proc/sys/kernel/random/boot_id", read_timeout_s=15).strip()
    if re.fullmatch(r"[0-9a-fA-F-]{32,40}", bid):
        return ("id", bid)
    try:
        return ("up", float(dev.shell("cat /proc/uptime", read_timeout_s=15).split()[0]))
    except (IndexError, ValueError):
        return None


def is_new_boot(before, now):
    """Is `now` a later boot than `before`? True, False, or None when that can't be told: no ids to compare, or an uptime
    that is not lower (a TV that was up only briefly passes its old uptime again soon after it restarts)."""
    if before is None or now is None or before[0] != now[0]:
        return None
    if before[0] == "id":
        return now[1] != before[1]
    return True if now[1] < before[1] else None


WAIT_S = 600          # how long a restarted TV is waited for
HINT_EVERY_S = 60     # while waiting: look again for the TV this often


def address_hint(host=None):
    """When the saved address does not answer: the one other device on this Wi-Fi with ADB open (a TV whose address changed
    after the restart), as a printable hint, else "". Looks only; it never saves or switches to anything: the identity guard
    decides which TV this is, and a person says so. Uses find-tv.py's own helpers (standard library only)."""
    host = host or HOST
    try:
        import importlib.util
        spec = importlib.util.spec_from_file_location("find_tv", os.path.join(HERE, "find-tv.py"))
        ft = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(ft)
        st = wireless.load_state()
        ports = [PORT] + ([int(st["port"])] if st.get("host") == host and str(st.get("port", "")).isdigit() else [])
        if any(ft.connects(host, p, 1.5) for p in ports):
            return ""   # the saved TV is there (it may still be booting): nothing to suggest
        ip = ft.local_ip()
        nets = ft.my_networks(ip)
        if not nets:
            return ""
        own = ft.own_addresses(ip)
        pool = [str(h) for n in nets for h in n.hosts() if str(h) not in own]
        if len(pool) > 1024:
            pool = [h for h in ft.neighbours() if h not in own]
        found = ft.candidates([h for h, _ in ft.knock_all([(h, 5555) for h in pool if h != host], 1.5)], own, set(ft.gateways()), nets)
    except Exception:  # noqa: BLE001  (a hint is never worth a failure)
        return ""
    if len(found) != 1:
        return ""
    return (f"   (the saved address {host} does not answer, but one device on this Wi-Fi has ADB open at {found[0]}: "
            f"if that is the TV, its address changed. Ask the person, then: optimize/find-tv.sh use {found[0]})")


def wait_for_tv(limit_s=WAIT_S, before=None, restart=False):
    """Try every 10 s until the TV answers and has finished booting, up to 10 minutes (a few TVs take that long to restart).
    The same address and the same port are used throughout (5555, or the remembered Wireless-debugging port, looked up again by
    name when it changed). Every 60 s, if the saved address does not answer, a hint is printed when another device on the
    Wi-Fi now has ADB open; the saved TV is never replaced by itself. After a reboot (restart=True) the TV may still
    answer for a while before it goes down, so it only counts as back from a different boot than `before`; where the
    boots can't be told apart, only once it was seen not answering."""
    start = time.time()
    down = False   # seen not answering, or still booting, since we started
    next_hint, hinted = start + HINT_EVERY_S, set()
    while time.time() - start < limit_s:
        try:
            dev = new_dev(15.0)
            dev.connect(rsa_keys=[signer()], auth_timeout_s=30)
            booted = dev.shell("getprop sys.boot_completed", read_timeout_s=15).strip()
            now = boot_marker(dev) if restart else None
            dev.close()
            if booted != "1":
                down = True
            elif not restart or is_new_boot(before, now) or (is_new_boot(before, now) is None and down):
                print(f"TV is back ({int(time.time() - start)} s)")
                return
        except Exception:  # noqa: BLE001
            down = True
        if time.time() >= next_hint:
            next_hint += HINT_EVERY_S
            print(f"   (still waiting for the TV: {int(time.time() - start) // 60} min)", file=sys.stderr)
            hint = address_hint()
            if hint and hint not in hinted:
                hinted.add(hint)
                print(hint, file=sys.stderr)
        time.sleep(10)
    if restart and not down:
        sys.exit("!! The TV kept answering but never restarted: it may have ignored the reboot. Restart it from its "
                 "own menu (not with this command again), then go on.")
    sys.exit("!! The TV did not come back within 10 minutes. Check it is on and on Wi-Fi (ADB stays on after a reboot)."
             + ("\n" + "\n".join(hinted) if hinted else ""))


def main():
    a = sys.argv[1:]
    if a == ["--address-hint"]:   # only looks: prints a line if another device now has ADB open, else nothing
        hint = address_hint()
        if hint:
            print(hint)
        return
    if a[:1] == ["--is-placeholder"]:   # exit 0 when the value is not an id (run.sh uses the same list)
        sys.exit(0 if is_placeholder(a[1] if len(a) > 1 else "") else 1)
    if a == ["--transport"]:   # how this TV is reached: «wireless» (Wireless debugging: TLS and pairing, the port changes on a restart) or «adb» (plain network adb, port 5555)
        print("wireless" if "TV_PORT" not in os.environ and wireless.state_for(HOST) else "adb")
        return
    if a == ["--id"]:   # the TV's unique id for run.sh: the serial, else aid:<android id>, else nothing (read-only)
        tv = TV()
        ident = parse_identity(tv.shell(IDENT_CMD))
        tv.close()
        print(unique_id(ident))
        return
    if a == ["--wait"]:
        wait_for_tv()
        return
    if a == ["--reboot"]:   # once: a retried reboot could restart the TV twice
        try:
            dev = new_dev(30.0)
            dev.connect(rsa_keys=[signer()], auth_timeout_s=60)
        except Exception as e:  # noqa: BLE001  (nothing was sent: safe to run again)
            sys.exit(f"!! Can't reach the TV to restart it ({type(e).__name__}: {e}). Nothing was sent; run it again.")
        try:
            check_identity(dev.shell(IDENT_CMD, read_timeout_s=30) or "")   # the right TV, or nothing is sent
        except Exception as e:  # noqa: BLE001  (nothing was sent: safe to run again)
            sys.exit(f"!! Can't tell which TV this is, so it was not restarted ({type(e).__name__}: {e}). Nothing was sent; run it again.")
        try:
            before = boot_marker(dev)   # which boot this is, to know the TV really came back from a new one
        except Exception:  # noqa: BLE001
            before = None
        try:
            dev.shell("reboot", read_timeout_s=10)
        except Exception:  # noqa: BLE001  (the connection drops as the TV goes down)
            pass
        print("restarting the TV…")
        time.sleep(45)
        wait_for_tv(before=before, restart=True)
        return
    if a == ["--check"]:
        TV(guard=True).shell("true")
        print("OK: this is the saved TV")   # a refusal exits above with its !! line; the make, model and serial stay out of this line
        return
    if a == ["--new-tv"]:
        tv = TV()
        ident = parse_identity(tv.shell(IDENT_CMD))
        if unreadable(ident):
            sys.exit("!! Could not read which TV this is (it gave no serial, make or model): nothing was saved. Run it again in a minute.")
        save_identity(ident)
        print(f"Working on this TV from now on: {tv_name(ident)}")
        tv.close()
        return
    batch = [l.strip() for l in sys.stdin] if a[:1] == ["--batch"] else []
    batch = [l for l in batch if l and not l.startswith("#")]
    if a[:1] == ["--batch"]:
        changes = not all(is_read_only(l) for l in batch)
    elif a[:1] in (["--pull"], ["--shot"]):
        changes = False
    elif a[:1] in (["--bg"], ["--push-b64"]):
        changes = True
    else:
        changes = bool(a) and a != ["--probe"] and not is_read_only(shell_line(a))
    slow = a[:1] in (["--bg"], ["--push-b64"])
    tv = TV(guard=changes, tries=LONG_TRIES if slow else None, transport_s=SLOW_READ_S if slow else 60.0)   # uploads and installs get the longer patience
    if not a or a == ["--probe"]:
        for p in ("ro.product.manufacturer", "ro.product.brand", "ro.product.model", "ro.product.device",
                  "ro.build.version.release", "ro.build.version.sdk", "ro.build.display.id"):
            print(f"{p} = {tv.shell(f'getprop {p}').strip()}")
        if load_identity() is None:   # the first TV seen is the one the setup is for (never an unreadable answer)
            first = parse_identity(tv.shell(IDENT_CMD))
            if not unreadable(first):
                save_identity(first)
    elif a[0] == "--batch":
        for line in batch:
            print(f"$ {line}\n{tv.shell(line).rstrip()}")
    elif a[0] == "--pull":
        tv.call("pull", tv_path(a[1]), a[2])
    elif a[0] == "--bg":   # prints the command's own output and exits with the command's own exit code
        code, out = run_bg(tv, a[1], shell_line(a[2:]))
        print(out)
        tv.close()
        sys.exit(code)
    elif a[0] == "--push-b64":
        push_b64(tv, a[1], tv_path(a[2]))
    elif a[0] == "--shot":
        tv.shell("screencap -p /sdcard/_shot.png")
        tv.call("pull", "/sdcard/_shot.png", a[1])
        tv.shell("rm /sdcard/_shot.png")
        print(f"saved {a[1]}")
    else:
        print(tv.shell(shell_line(a)).rstrip())
    tv.close()


if __name__ == "__main__":
    main()
