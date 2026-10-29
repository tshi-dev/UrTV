#!/usr/bin/env python3
"""Write how the setup went, for its owner to send to UrTV's author if they want to (optimize/share.sh).

Only what helps fix the setup for others: the TV (maker, model, firmware, Android), the computer's system, the agent,
UrTV's version and the apps', how long each step took and where it stopped, the errors and dropped connections,
memory before and after, the checks, the agent's notes and the owner's own words if they want to add any.
Only these fields can be in a report (an allowlist, see restrict()); scrub() is a second net over all of it.
Nothing personal: no city, clubs, favourites or apps of theirs, no names, network addresses or file paths.

    optimize/share.sh --preview --agent "Codex (gpt-…)" --owner "the owner's words, if any" "the agent's notes"
    optimize/share.sh --send

A problem any time later (the owner says «بلّغ المطوّر عن مشكلة», or something broke): --problem marks it as one and
works without this computer's setup results too (the TV is then read live, if it answers). It goes to the same form:
the agent never opens a GitHub issue, comment or pull request for the owner.

    optimize/share.sh --preview --problem --agent "…" --owner "what the owner said happened" "what the agent checked"

--preview writes optimize/run/share.md and prints exactly what would be sent; nothing leaves the computer.
--send sends that file as it is. Without either, it writes and sends in one go (older instructions).
"""
import argparse
import datetime
import os
import platform
import re
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request
from pathlib import Path

for stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

RUN = Path(__file__).resolve().parent / "run"
OUT = RUN / "share.md"
QR = RUN / "share-qr.png"
# The author's Google Form (anonymous, no sign-in): each summary becomes a row in the author's sheet. The same two
# values are in docs/share/index.html.
FORM_ID = "1FAIpQLSfDJFfy1NgiB-ZMXoBI29n_KTdYnGadn1-iOqXJOxaQJEr-ZQ"   # from the form's link: docs.google.com/forms/d/e/<FORM_ID>/viewform
FORM_FIELD = "1457112794"   # the question's number: entry.<FORM_FIELD>=… in the form's pre-filled link
# Without the form, or offline: the QR code opens this page (docs/share/, GitHub Pages) with the summary after the #
PAGE = "https://tshi-dev.github.io/UrTV/share/"


# --- what a report may contain -------------------------------------------------------------------------------------
# 1. ALLOWLIST: every line the script builds must start like one of these (the fields above: TV, computer, steps, ADB,
#    memory, checks). A line that does not is dropped, whatever it says. Only the agent's notes and the owner's words are
#    free text, after their own heading.
# 2. scrub(): a second net over everything, free text included, for what still slips through.
FREE_HEADINGS = ("Notes from the agent:", "From the owner:")
ALLOWED_LINE = re.compile(
    r"UrTV (problem|setup) report, |TV: |Computer: |Steps, minutes:seconds:$|Memory:$|Checks:$|Setup results: |ADB: "
    r"|Protected features on: |(Device|WebView|Home|Home switch|Screensaver|Versions|Extras):|Auto-start|Optional picks|!! |- |  (off|on): "
    r"|home hides|[\w-]+: \d+ off, |\(no steps\.log: an older run\.sh\)$|\(not measured\)$|\(no report\.md yet: the setup didn't finish\)$|\d+ min in all$|\[\d+:\d\d until it was run again\]$"
    r"|[A-Za-z0-9_.-]+ \d+:\d\d( STOPPED.*)?(, [A-Za-z0-9_.-]+ \d+:\d\d( STOPPED.*)?)*(, \[.*\])?$")
MAX_LINE = 400
# The `!! ` lines (and the `- !! ` ones of the problems list) carry what the scripts printed: free text. They pass the
# allowlist by their start only, so they are scrubbed here, visibly, and the scrub is their one real net.
FREE_LINE = re.compile(r"(- )?!! ")


def restrict(text):
    """The allowlist: lines before the free-text headings must look like a field this script builds; others are dropped."""
    out, free = [], False
    for line in text.splitlines():
        if line.strip() in FREE_HEADINGS:
            free = True
        elif not free and line.strip() and not ALLOWED_LINE.match(line):
            continue
        if not free and FREE_LINE.match(line):
            line = scrub(line)
        out.append(line if free else line[:MAX_LINE])
    return "\n".join(out) + ("\n" if text.endswith("\n") else "")


def own_names():   # this computer's user and host: wherever they show up, they are not for the report
    names = set()
    for get in (lambda: __import__("getpass").getuser(), platform.node, lambda: Path.home().name):
        try:
            names.add(get())
        except Exception:  # noqa: BLE001
            pass
    return {n for n in names if len(n) >= 3 and n.lower() not in ("root", "user", "admin", "runner")}


WIFI_KEY = r"(?:ssid|wi-?fi(?:\s+name)?|network(?:\s+name)?|wlan|(?:ال)?شبكة|(?:ال)?واي\s?فاي)"
QUOTED = r"""(?:"[^"\r\n]*"|'[^'\r\n]*'|\u201c[^\u201d\r\n]*\u201d|\u00ab[^\u00bb\r\n]*\u00bb)"""


def scrub(text):   # in case a note or an output carries something personal
    """The net over a whole report. Lines the scripts build (a `!! ` line before the free-text headings) keep their ordinary words: only a Wi-Fi name that is
    marked as one (ssid=…, a quoted name after Wi-Fi / network, «connected to X», «couldn't find X») goes there. The free text (the agent's notes and the
    owner's words, and any line that is not a script's) gets the whole Wi-Fi rule, an unquoted `network: x` included."""
    out, free = [], False
    for line in text.split("\n"):
        if line.strip() in FREE_HEADINGS:
            free = True
        out.append(_scrub_line(line, scripted=not free and bool(FREE_LINE.match(line))))
    return "\n".join(out)


# «couldn't find X» / «could not connect to X»: X is a network name when it is quoted, or a bare word that is not one of the ordinary things a script says it could not reach
COULD_NOT = re.compile(rf"(?i)\b(?:couldn'?t|could not|can'?t|cannot|unable to|failed to)\s+(?:find|connect to|join|reach)\s+(?:{QUOTED}|(?!(?:the|a|an|any|your|this|that|it|its|to|adb|tv|github|internet|network|wi-?fi|server|device|host|port|home|<)\b)[^\s,;:.)<]+(?:\s+[A-Za-z0-9][\w-]*)?)")


def _scrub_line(text, scripted=False):
    # a link keeps its address but not what follows ?, # or a user:password@ (tokens, sessions, keys)
    text = re.sub(r"(?i)(https?://)[^/\s@]+@", r"\1", text)
    text = re.sub(r"(?i)(https?://[^\s?#]*)[?#]\S*", r"\1", text)
    text = re.sub(r"(?i)([?&](?:token|key|apikey|api_key|secret|auth|sig|session|password|pass|code)=)[^&\s]*", r"\1<x>", text)
    # an IPv4 address: four numbers of 0 to 255, on their own. Not inside a word or a version string (V816.0.6.0.X, TVOS-01.12.01.12, 6.0.1.2.3):
    # lookarounds, not \b, so that Arabic prefixes stick to the next word («على192.168.1.5», «فيC:\Users») and still match
    octet = r"(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"
    text = re.sub(rf"(?<![A-Za-z0-9_.-]){octet}(?:\.{octet}){{3}}(:\d+)?(?![0-9A-Za-z_-])(?!\.\d)", "<ip>", text)
    hexg = r"[0-9A-Fa-f]{1,4}"
    text = re.sub(rf"(?<![0-9A-Fa-f:])\[?(?:(?:{hexg}:){{7}}{hexg}|(?:{hexg}:){{0,6}}[0-9A-Fa-f]{{0,4}}::(?:{hexg}:){{0,6}}[0-9A-Fa-f]{{0,4}})"
                  r"(?:%[\w.-]+)?\]?(?![0-9A-Za-z_:])", "<ip>", text)
    text = re.sub(r"(?<![0-9A-Fa-f])([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}(?![0-9A-Fa-f])", "<mac>", text)
    # the user folder may hold spaces ("Ann Lee") or any script: it runs to the next separator or the end of the line
    text = re.sub(r"(?i)(?<![A-Za-z])[A-Z]:[\\/]+(Users|Documents and Settings)[\\/]+[^\\/\r\n]+", "<home>", text)
    text = re.sub(r"(?i)(/c)?/Users/[^/\r\n]+|/home/[^/\r\n]+", "<home>", text)
    # a Wi-Fi name. ssid / wlan with = or : always; the plainer words (Wi-Fi, network, الشبكة) with = or : only in free text, since the scripts' own lines say
    # «network: left on»; in quotes after any of them; and after «connected to» / «couldn't find» on a !! line
    strong = r"(?:ssid|wlan)"
    text = re.sub(rf"(?i)\b({strong})(\s*[=:]\s*)({QUOTED}|[^\s,;)]+)", r"\1\2<wifi>", text)
    if not scripted:
        text = re.sub(rf"(?i)\b({WIFI_KEY})(\s*[=:]\s*)({QUOTED}|[^\s,;)]+)", r"\1\2<wifi>", text)
    text = re.sub(rf"(?i)\b({WIFI_KEY})(\s*[=:]\s*|\s+)({QUOTED})", r"\1\2<wifi>", text)
    if "!!" in text:
        text = re.sub(r"(?i)\b(connected to|joined|associated with|connecting to)\s+(?!the\b|a\b|<).*$", r"\1 <wifi>", text)
    text = COULD_NOT.sub(lambda m: re.sub(r"(find|connect to|join|reach)\s+.*$", r"\1 <wifi>", m.group(0), flags=re.I), text)
    # an email, a serial (also inside the ADB mDNS name adb-<serial>-xxx) and the usual tokens
    text = re.sub(r"[^\s@<>()\[\]\"']+@[^\s@<>()\[\]\"']+\.\w+", "<email>", text)
    text = re.sub(r"(?i)\badb-[A-Za-z0-9]+(?=-)", "adb-<serial>", text)
    text = re.sub(r"(?i)\b(ro\.serial\w*|serial(?:\s?(?:no|number))?)(\s*[=:]\s*|\s+)(?!<)[A-Za-z0-9]{6,}", r"\1\2<serial>", text)
    text = re.sub(r"(?i)\b(adb\s+device|device)\s+[A-Z0-9]{8,}\b(?=\s+(offline|unauthorized|device|no permissions))", r"\1 <serial>", text)
    text = re.sub(r"(?i)\bBearer\s+\S+", "Bearer <token>", text)
    text = re.sub(r"\bgh[pousr]_\w{20,}", "<token>", text)
    text = re.sub(r"\beyJ[\w-]+\.[\w-]+\.[\w-]*", "<token>", text)
    for name in sorted(own_names(), key=len, reverse=True):
        # a whole word or a whole run of letters: user «dev» is not the start of «Device:» (Arabic prefixes still match)
        text = re.sub(rf"(?<![A-Za-z0-9]){re.escape(name)}(?![A-Za-z0-9])", "<user>", text, flags=re.I)
    return text


def tv():
    path = RUN / "tv.txt"
    if path.exists():
        props = dict(re.findall(r"^(\S+) = (.*)$", path.read_text(encoding="utf-8"), re.M))
    else:   # set up from another computer: ask the TV itself, if it answers
        props = live_props()
        if not props:
            return "? (not set up from this computer, and the TV did not answer)"
    return (f"{props.get('ro.product.manufacturer', '?')} {props.get('ro.product.model', '?')} "
            f"({props.get('ro.product.device', '?')}), Android {props.get('ro.build.version.release', '?')} "
            f"(SDK {props.get('ro.build.version.sdk', '?')}), firmware {props.get('ro.build.display.id', '?')}")


def live_props():
    """The TV's getprop as {name: value}; {} when it does not answer."""
    try:
        out = subprocess.run([sys.executable, "-I", str(Path(__file__).resolve().parent / "tv.py"), "getprop"],
                             capture_output=True, text=True, timeout=60, env={**os.environ, "TV_TRIES": "1"})
    except Exception:  # noqa: BLE001
        return {}
    return dict(re.findall(r"^\[(ro\.[^\]]+)\]: \[(.*)\]$", out.stdout, re.M)) if out.returncode == 0 else {}


def urtv_version():   # this copy of the repo: its last commit
    try:
        out = subprocess.run(["git", "-C", str(RUN.parent.parent), "log", "-1", "--format=%h %cs"],
                             capture_output=True, text=True, timeout=20).stdout.strip()
        return out or "?"
    except Exception:  # noqa: BLE001
        return "?"


def trouble():   # from run.log: what the scripts said went wrong, and how often ADB dropped
    log = RUN / "run.log"
    if not log.exists():
        return []
    lines = [l.strip() for l in re.split(r"[\r\n]+", log.read_text(encoding="utf-8", errors="replace"))]
    errors = list(dict.fromkeys(l for l in lines if l.startswith("!!") or " !! " in l))[:8]
    drops = sum("(connection problem" in l for l in lines)
    resumed = sum(l.startswith("resuming upload") for l in lines)
    out = [f"ADB: {drops} dropped connections (retried), {resumed} resumed uploads"]
    return out + [f"- {l}" for l in errors]


def windows_release(system, version, release):
    """«11» or «10»: Python's release() says 10 on Windows 11 too, so the build number decides (22000 and over is 11).
    The build is the last part of platform.version() («10.0.22631»), or of Git Bash's system («MINGW64_NT-10.0-22631»)."""
    for text in (version, system):
        m = re.search(r"\b10\.0[.-](\d{4,6})\b", text or "")
        if m:
            return "11" if int(m.group(1)) >= 22000 else "10"
    return release


def computer():
    name = platform.system()
    if name == "Windows" or "MINGW" in name.upper() or "MSYS" in name.upper():
        return f"Windows {windows_release(name, platform.version(), platform.release())}"
    return {"Darwin": f"macOS {platform.mac_ver()[0]}"}.get(name, f"{name} {platform.release()}")


def live_versions():
    """The two apps' versionName as the TV says it now, «Versions: screensaver 3.124, home 3.124»; None when the TV does not
    answer (then report.md's line, written at the last setup, is all there is)."""
    tv_py = Path(__file__).resolve().parent / "tv.py"
    found = []
    for package in ("sa.urtv.screensaver", "sa.urtv.home"):
        try:
            out = subprocess.run([sys.executable, "-I", str(tv_py), f"dumpsys package {package} 2> /dev/null | grep versionName | sed -n 1p"],
                                 capture_output=True, text=True, timeout=60, env={**os.environ, "TV_TRIES": "1"})
        except Exception:  # noqa: BLE001
            return None
        m = re.search(r"versionName=(\S+)", out.stdout) if out.returncode == 0 else None
        if not m:
            return None
        found.append(m.group(1))
    return f"Versions: screensaver {found[0]}, home {found[1]}"


def steps():
    log = RUN / "steps.log"
    if not log.exists():
        return ["(no steps.log: an older run.sh)"]
    lines, started, first, last, stopped_at = [], {}, None, None, None
    for row in log.read_text(encoding="utf-8").splitlines():
        m = re.match(r"(\S+ \S+) (start|done|stopped) (\S+)(.*)", row)
        if not m:
            continue
        t = datetime.datetime.strptime(m.group(1), "%Y-%m-%d %H:%M:%S")
        first, last = min(first or t, t), max(last or t, t)   # steps.log can span a re-run: never assume it is in order
        what, step = m.group(2), m.group(3)
        if what == "start":
            started[step] = t
            if stopped_at:   # the time between a stop and the next try went into finding out why
                gap = int((t - stopped_at).total_seconds())
                if gap >= 0:   # a clock or date out of order gives no gap rather than a negative one
                    lines.append(f"[{gap // 60}:{gap % 60:02d} until it was run again]")
                stopped_at = None
        elif step in started:
            took = max(0, int((t - started.pop(step)).total_seconds()))
            lines.append(f"{step} {took // 60}:{took % 60:02d}" + ("" if what == "done" else f" STOPPED{m.group(4)}"))
            stopped_at = t if what == "stopped" else None
    total = f"{int((last - first).total_seconds()) // 60} min in all" if first else ""
    return [", ".join(lines), total] if lines else [total]


def memory(summary):   # summary.md's table as short lines: they read better in a message and fit a QR code
    if not summary.exists():
        return ["(not measured)"]
    rows = [[c.strip() for c in l.strip("|").split("|")] for l in summary.read_text(encoding="utf-8").splitlines()
            if l.startswith("|") and not l.startswith("|---")]
    if not rows:
        return ["(not measured)"]
    head, out = rows[0], []
    total = "".join(ch for ch in (head[1] if len(head) > 1 else "") if ch.isdigit())
    for r in rows[1:]:
        if len(r) < 6 or not r[1].isdigit():
            out.append(f"- {r[0]}: {r[1] if len(r) > 1 else ''}")
            continue
        out.append(f"- {r[0]}: {r[1]} MB free{' of ' + total if total else ''}, {r[2]} MB swap, {r[3]} processes, "
                   f"{r[4] or '?'} CPU idle, {r[5]} disabled")
    return out


def report_checks():
    report = RUN / "report.md"
    if not report.exists():
        return ["(no report.md yet: the setup didn't finish)"]
    lines = report.read_text(encoding="utf-8").splitlines()
    ok = sum(l.startswith("- ok:") for l in lines)
    bad = [l[2:] for l in lines if l.startswith("- !!")]
    live = live_versions()   # the apps on the TV now; the report's line only dates from the last setup
    out = [f"Protected features on: {ok}/{ok + len(bad)}", *bad,
           *[live if l.startswith("Versions:") and live else l
             for l in lines if l.startswith(("Device:", "WebView:", "!! WebView:", "Auto-start", "!! Auto-start", "Home:", "Screensaver:", "Versions:", "Extras:", "Optional picks"))]]
    if live and not any(l.startswith("Versions:") for l in lines):
        out.append(live)
    return out + packages(lines)


# in keep.txt but off by design once our home is in: install.sh launcher turns them off; keep.sh leaves them too
OFF_BY_DESIGN = {"com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher",
                 "com.google.android.tungsten.setupwraith", "com.tcl.keycustomfunctionservice"}


def off_before():   # packages already disabled in the backup taken before setup: the TV keeps them off itself
    try:
        text = (RUN / "backup.txt").read_text(encoding="utf-8", errors="replace")
    except OSError:
        return set()
    sect = re.search(r"^== disabled packages.*?\n(.*?)(?=^== |\Z)", text, flags=re.M | re.S)
    return set(re.findall(r"^package:(\S+)", sect.group(1), flags=re.M)) if sect else set()


def packages(lines, before=None):   # what our lists turned off, what they left on, and what the home screen hides
    state = {}   # (state, list) -> packages
    for l in lines:
        m = re.match(r"- pkg (off|on|absent): (\S+) \((\S+)\)", l)
        if m:
            state.setdefault((m.group(1), m.group(3)), []).append(m.group(2))
    out = []
    for lst in ("tcl-safe", "tcl-optional", "community", "home-replaced"):
        off, on = state.get(("off", lst), []), state.get(("on", lst), [])
        absent = len(state.get(("absent", lst), []))
        if off or on or absent:
            out.append(f"{lst}: {len(off)} off, {len(on)} still on, {absent} not on this TV")
            out += [f"  off: {', '.join(off)}"] if off else []
            out += [f"  on: {', '.join(on)}"] if on else []
    before = off_before() if before is None else before   # off before UrTV touched the TV: not ours to flag
    protected = [p for p in state.get(("off", "keep"), []) if p not in OFF_BY_DESIGN and p not in before]
    if protected:
        out.append(f"!! protected but off: {', '.join(protected)}")
    out += hidden(lines)
    return out


def hidden(lines):   # the home screen's "package | reason" lines: the owner's own apps only as counts per reason
    ours = {l.split()[0] for f in (Path(__file__).resolve().parent / "lists").glob("*.txt")
            for l in f.read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")}
    reasons, named = {}, []
    for l in lines:
        if not l.startswith("- home hides: "):
            continue
        pkg, _, why = l[len("- home hides: "):].partition(" | ")
        if not why:
            continue
        if pkg.strip() in ours:
            named.append(f"{pkg.strip()} ({why.strip()})")
        else:
            reasons[why.strip()] = reasons.get(why.strip(), 0) + 1
    out = []
    if reasons:
        out.append("home hides: " + ", ".join(f"{n} {why}" for why, n in reasons.items()))
    if named:
        out.append("home hides, from our lists: " + ", ".join(named))
    return out


def main():
    a = argparse.ArgumentParser(description="Send the author how the setup went, or a problem (nothing personal).")
    a.add_argument("--problem", action="store_true", help="a problem report, any time; needs no setup results here")
    a.add_argument("--agent", default="", help="which agent ran the setup, and its model")
    a.add_argument("--owner", default="", help="the owner's own words for the author, if they gave any")
    a.add_argument("notes", nargs="*", help="the agent's notes: what failed or was slow, and what fixed it")
    mode = a.add_mutually_exclusive_group()
    mode.add_argument("--preview", action="store_true", help="write the report and print it; send nothing")
    mode.add_argument("--send", action="store_true", help="send the report --preview wrote")
    args = a.parse_args()
    if not args.send and not args.problem and not (RUN / "tv.txt").exists():
        sys.exit("!! No setup results here (optimize/run/tv.txt): run the setup first, "
                 "or for a problem: optimize/share.sh --preview --problem --owner \"<what happened>\" \"<your notes>\".")
    if args.send:
        if args.notes or args.agent or args.owner:
            sys.exit("!! --send sends the file as --preview wrote it: give the notes to --preview.")
        if not OUT.exists():
            sys.exit(f"!! Nothing to send: {OUT.relative_to(RUN.parent.parent)} is not there yet. "
                     "Run optimize/share.sh --preview \"<notes>\" first, show the owner the text, then --send.")
        deliver(scrub(restrict(OUT.read_text(encoding="utf-8"))))
        return
    notes = " ".join(args.notes).strip()
    summary = RUN / "summary.md"
    parts = [
        f"UrTV {'problem' if args.problem else 'setup'} report, {datetime.date.today()}, UrTV {urtv_version()}",
        "",
        f"TV: {tv()}",
        f"Computer: {computer()}" + (f", agent: {args.agent}" if args.agent else ""),
    ]
    if (RUN / "tv.txt").exists():
        parts += ["", "Steps, minutes:seconds:", *steps(), *trouble(), "", "Memory:", *memory(summary), "", "Checks:", *report_checks()]
    else:   # a problem from a computer that didn't set this TV up: the apps' versions, if the TV answers
        parts += ["", "Setup results: not on this computer", *([live] if (live := live_versions()) else [])]
    if notes:
        parts += ["", "Notes from the agent:", notes]
    if args.owner.strip():
        parts += ["", "From the owner:", args.owner.strip()]
    text = scrub(restrict("\n".join(parts))) + "\n"
    OUT.write_text(text, encoding="utf-8")
    print(text)
    print(f"Saved in {OUT.relative_to(RUN.parent.parent)}.")
    if args.preview:
        print("NOT sent. This is exactly what optimize/share.sh --send would send.")
        print("للمالك: لا تكتب في كلامك أسماء ولا أرقام هواتف أو عناوين ولا اسم شبكة الواي فاي؛ نحذف ما نعرفه تلقائيًا لكن الأسلم ألا يُكتب أصلًا.")
        return
    deliver(text)


def deliver(text):
    if send(text):
        print("Sent to the author. Thank you!")
        return
    # not sent from here (no internet?): a QR code carries it to the phone, whose page sends it
    if qr(PAGE + "#" + urllib.parse.quote(text)):
        print(f"QR code: {QR.relative_to(RUN.parent.parent)}" + (" (open on this screen)" if show(QR) else ""))
        print("Scan it with the phone's camera: a page opens with this text and a button that sends it.")


def send(text):
    """Into the author's Google Form, anonymously; False if it didn't go through."""
    url = f"https://docs.google.com/forms/d/e/{FORM_ID}/formResponse"
    data = urllib.parse.urlencode({f"entry.{FORM_FIELD}": text}).encode()
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data), timeout=30) as r:
            return r.status == 200
    except Exception as e:  # noqa: BLE001
        print(f"(couldn't send it from this computer: {e})")
        return False


def qr(link):
    try:
        import segno
    except ImportError:
        print("(no QR code: optimize/share.sh installs segno when it runs, and that did not work: no internet for pip?)")
        return False
    try:
        segno.make(link, error="l").save(str(QR), scale=6, border=4)
    except segno.DataOverflowError:
        print("(too long for a QR code: send it from a computer with internet: optimize/share.sh again)")
        return False
    return True


def show(path):
    """Open the picture on this computer's screen, if there is a way."""
    try:
        if sys.platform == "win32" or os.environ.get("MSYSTEM"):
            os.startfile(str(path))   # noqa: S606  (Windows' own viewer)
        elif sys.platform == "darwin":
            subprocess.run(["open", str(path)], check=True, timeout=20)
        elif shutil.which("xdg-open"):
            subprocess.run(["xdg-open", str(path)], check=True, timeout=20, capture_output=True)
        else:
            return False
        return True
    except Exception:  # noqa: BLE001
        return False


if __name__ == "__main__":
    main()
