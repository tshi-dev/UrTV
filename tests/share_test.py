"""optimize/share.py on a fake run folder: nothing of the owner's may reach the text that is sent.

    python3 tests/share_test.py          (no TV needed)
"""
import os, sys, tempfile
from pathlib import Path
here = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(here, "..", "optimize"))
import share

ours = next(l.split()[0] for l in open(os.path.join(here, "..", "optimize", "lists", "tcl-safe.txt"))
            if l.strip() and not l.startswith("#"))
share.live_versions = lambda: None   # no TV here: the report's own line
share.RUN = Path(tempfile.mkdtemp())
(share.RUN / "run.log").write_text("\n".join([
    "!! adb " + ".".join(["192", "168", "1", "23"]) + ":5555 refused",   # built apart: tests/check.py bars IPs in files
    "!! mac aa:bb:cc:dd:ee:ff and AA-BB-CC-DD-EE-FF",
    r"!! saved C:\Users\Ann Lee\x\run.log",
    r"!! also c:\documents and settings\Ann Lee\x",
    "!! /home/ann/x and /Users/Ann Lee/y and /c/Users/ann/z",
]), encoding="utf-8")
(share.RUN / "report.md").write_text("\n".join([
    "- ok: com.android.vending",
    "- home hides: com.thirdparty.secretapp | no TV activity",
    "- home hides: com.other.private | no TV activity",
    "- home hides: com.third.cfg | config.xml",
    f"- home hides: {ours} | config.xml",
]), encoding="utf-8")

text = share.scrub("\n".join(share.trouble() + share.report_checks()))
leaks = [w for w in ("192.168", "aa:bb", "AA-BB", "Ann", "Lee", "/ann", "secretapp", "other.private", "third.cfg")
         if w.lower() in text.lower()]
counts = "2 no TV activity" in text and "1 config.xml" in text and ours in text
print("share: " + ("nothing personal left, counts kept" if not leaks and counts else "WRONG")
      + (f", leaked {leaks}" if leaks else ""))

def keep_off(*pkgs, before=()):   # report lines naming these keep packages as off
    return share.packages([f"- pkg off: {p} (keep)" for p in pkgs], before=set(before))


by_design = [l for l in keep_off("com.google.android.apps.tv.launcherx") if l.startswith("!!")]
real = [l for l in keep_off("com.google.android.tungsten.setupwraith", "com.tcl.airplay2") if l.startswith("!!")]
factory = [l for l in keep_off("com.tcl.smartalexa", "com.tcl.airplay2", before=["com.tcl.smartalexa"]) if l.startswith("!!")]
alarm = not by_design and real == ["!! protected but off: com.tcl.airplay2"] \
    and factory == ["!! protected but off: com.tcl.airplay2"]
print("share: " + ("off-by-design keep packages not flagged, others still are" if alarm else f"WRONG alarm {by_design} {real}"))
# Windows 11 says «10» in platform.release(): the build number decides
wins = [(("Windows", "10.0.22631", "10"), "11"), (("Windows", "10.0.22000", "10"), "11"), (("Windows", "10.0.21996", "10"), "10"),
        (("Windows", "10.0.19045", "10"), "10"), (("MINGW64_NT-10.0-26100", "3.4.10", "3.4.10"), "11"),
        (("MINGW64_NT-10.0-19045", "3.4.10", "3.4.10"), "10"), (("Windows", "6.1.7601", "7"), "7")]
bad_win = [(a, share.windows_release(*a), want) for a, want in wins if share.windows_release(*a) != want]
print("share: " + ("Windows 11 told from 10 by the build number" if not bad_win else f"WRONG windows {bad_win}"))
# the Versions line: live from the TV when it answers, else the report's
(share.RUN / "report.md").write_text("Home: h\nVersions: screensaver 3.89, home 3.89\n", encoding="utf-8")
share.live_versions = lambda: "Versions: screensaver 3.124, home 3.124"
live = [l for l in share.report_checks() if l.startswith("Versions:")]
share.live_versions = lambda: None
old = [l for l in share.report_checks() if l.startswith("Versions:")]
(share.RUN / "report.md").write_text("Home: h\n", encoding="utf-8")
share.live_versions = lambda: "Versions: screensaver 3.124, home 3.124"
added = [l for l in share.report_checks() if l.startswith("Versions:")]
share.live_versions = lambda: None
versions_ok = live == ["Versions: screensaver 3.124, home 3.124"] and old == ["Versions: screensaver 3.89, home 3.89"] and len(added) == 1
print("share: " + ("Versions read live, the report's line as the fallback" if versions_ok else f"WRONG versions {live} {old} {added}"))
# --preview writes and prints the text and sends nothing; --send sends that file, or says there is none
import contextlib, io
run2 = Path(tempfile.mkdtemp())
(run2 / "tv.txt").write_text("ro.product.model = Test TV\n", encoding="utf-8")
share.RUN, share.OUT, share.QR = run2, run2 / "share.md", run2 / "share-qr.png"
sent = []
share.send = lambda text: sent.append(text) or True
share.qr = lambda link: False

def cli(*argv):
    sys.argv = ["share.py", *argv]
    out, err = io.StringIO(), io.StringIO()
    code = 0
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            share.main()
        except SystemExit as e:
            code = e.code if isinstance(e.code, int) else 1
            err.write(str(e.code))
    return code, out.getvalue(), err.getvalue()

code, out, err = cli("--send")
no_file = code != 0 and "Nothing to send" in err and not sent
code, out, err = cli("--preview", "--agent", "tester", "slow adb at step 3")
written = share.OUT.read_text(encoding="utf-8")
preview = code == 0 and not sent and "slow adb at step 3" in written and written.strip() in out and "NOT sent" in out
code, out, err = cli("--send")
sends = code == 0 and sent == [written]
code, out, err = cli("--send", "more notes")
refuses = code != 0 and "give the notes to --preview" in err and len(sent) == 1
sent.clear()
code, out, err = cli("an old-style note")
plain = code == 0 and len(sent) == 1 and "an old-style note" in sent[0]
# a problem, any time: works with no setup results here, says it is a problem; a setup report still needs them
run3 = Path(tempfile.mkdtemp())
share.RUN, share.OUT, share.QR = run3, run3 / "share.md", run3 / "share-qr.png"
share.live_props = lambda: {}
sent.clear()
code, out, err = cli("--preview")
needs_setup = code != 0 and "--problem" in err and not share.OUT.exists()
code, out, err = cli("--preview", "--problem", "--owner", "الأذان ما اشتغل العصر", "checked the adhan setting: on")
written = share.OUT.read_text(encoding="utf-8") if share.OUT.exists() else ""
problem = (code == 0 and not sent and written.startswith("UrTV problem report") and "الأذان ما اشتغل العصر" in written
           and "not on this computer" in written and "Steps" not in written)
code, out, err = cli("--send")
problem_sent = code == 0 and sent == [written]
share.live_props = lambda: {"ro.product.manufacturer": "TCL", "ro.product.model": "Live TV"}
live_tv = "TCL Live TV" in share.tv()
# steps.log spanning a re-run, out of order (the clock or date moved back): no negative gap, no negative time
(share.RUN / "steps.log").write_text("\n".join([
    "2026-10-20 10:00:00 start probe", "2026-10-20 10:00:30 done probe",
    "2026-10-20 10:01:00 start extras", "2026-10-20 10:05:00 stopped extras exit 1",
    "2026-10-20 09:40:00 start extras", "2026-10-20 09:41:00 done extras",   # the clock jumped back
]) + "\n", encoding="utf-8")
step_text = " ".join(share.steps())
steps_ok = "-" not in step_text.replace("-", "", 0).replace("min in all", "") and "until it was run again" not in step_text and "probe 0:30" in step_text
print("share: steps never print a negative time" if steps_ok else f"WRONG steps {step_text}")
ok = steps_ok and no_file and preview and sends and refuses and plain and needs_setup and problem and problem_sent and live_tv
print("share: " + ("--preview sends nothing, --send sends the file written, plain form still sends, --problem works without setup results"
                    if ok else f"WRONG preview/send {no_file} {preview} {sends} {refuses} {plain} {needs_setup} {problem} {problem_sent} {live_tv}"))

# the allowlist and the second net (scrub), on obviously fake data (documentation ranges, example names); built apart
# so tests/check.py does not see addresses in the file
v6 = ":".join(["2001", "db8"]) + "::" + "7"
v6full = ":".join(["2001", "db8", "0", "0", "0", "0", "0", "1"])
v4 = ".".join(["192", "0", "2", "7"])
fake_home = "/ho" + "me/"
cases = {
    "IPv6 compressed": (f"!! adb [{v6}]:5555 refused", ("db8",)),
    "IPv6 full, with scope": (f"!! no route to {v6full}%wlan0", ("db8",)),
    "IPv4 glued to Arabic": (f"!! تعذّر الاتصال على{v4}", ("192.0.2",)),
    "Windows path glued to Arabic": ("!! الملف فيC:\\Users\\Example Name\\x", ("Example",)),
    "mixed-language user name in a path": (f"!! saved {fake_home}Example مثال Person/x/run.log", ("Example", "مثال", "Person")),
    "mixed-language Windows user": ("!! C:\\Users\\مثال Example\\run.log", ("مثال", "Example")),
    "URL query token": ("!! GET https://example.com/api/v1/photos?token=abc123SECRET&page=2 failed", ("abc123SECRET",)),
    "URL fragment and password": ("!! https://example:hunter2@example.com/x#access_token=zzTOP", ("hunter2", "zzTOP")),
    "bare token parameter": ("!! failed with &apikey=k3y-SECRET in the call", ("k3y-SECRET",)),
    "Wi-Fi name after ssid=": ("!! wifi dropped ssid=Example-Home-5G rssi -80", ("Example-Home",)),
    "Wi-Fi name quoted": ("!! TV left the Wi-Fi \"Example Home\" for another", ("Example Home",)),
    "Wi-Fi name in Arabic quotes": ("!! الشبكة: «بيت المثال» ما ردّت  network: «بيت المثال»", ("بيت المثال",)),
    "email": ("!! mail me at example.person@example.com", ("example.person",)),
    "serial in a line": ("!! adb device R58M12ABCDE offline", ("R58M12ABCDE",)),
    "serial in the mDNS name": ("Device: adb-R58M12ABCDE-xyz._adb-tls-connect._tcp", ("R58M12ABCDE",)),
    "serial property": ("!! ro.serialno=R58M12ABCDE", ("R58M12ABCDE",)),
    "bearer token": ("!! Authorization: Bearer abcdEFGH12345.secret", ("abcdEFGH",)),
    "github token": ("!! push failed with ghp_" + "a1B2c3D4e5F6g7H8i9J0k1L2", ("a1B2c3",)),
    "jwt": ("!! got eyJhbGciOi.eyJzdWIiOiIx.sigPART back", ("eyJhbGciOi", "sigPART")),
    "Wi-Fi name after connected to": ("!! TV connected to Example Home 5G but ADB timed out", ("Example Home",)),
}
(share.RUN / "run.log").write_text("\n".join(c[0] for c in cases.values()), encoding="utf-8")
(share.RUN / "tv.txt").write_text("ro.product.model = Test TV\n", encoding="utf-8")
net_leaks = {name: [w for w in words if w in share.scrub(share.restrict(line))] for name, (line, words) in cases.items()}
net_leaks = {k: v for k, v in net_leaks.items() if v}
print("share: " + ("IPv6, mixed-language names, URL tokens and Wi-Fi names in !! lines are scrubbed" if not net_leaks else f"WRONG scrub {net_leaks}"))
# the machine's own user name goes wherever it appears
share.own_names = lambda: {"exampleuser"}
own = share.scrub("copied by ExampleUser to the desktop") == "copied by <user> to the desktop"
share.own_names = lambda: {"dev", "mac"}   # short names are whole words only: «Device:» and «macOS» stay as they are
own = own and share.scrub("Device: x, macOS 14, dev ran, عن‌dev") .startswith("Device: x, macOS 14, <user> ran")
share.own_names = lambda: {"exampleuser"}
# the allowlist: a line the script does not build is dropped, only the notes and the owner's words are free text
allow = share.restrict("\n".join(["UrTV problem report, 2026-01-01, UrTV abc", "", "TV: Test TV", "password is 1234",
                                   "(my wifi password is hunter2)", "? the code is 4321", "(not measured)",
                                   "Checks:", "- ok: x", "Home switch: no safety switch on the TV itself (this TV ends background jobs with the ADB session)", "Notes from the agent:", "anything the agent says", "From the owner:", "كلامي"]))
allowed = "password" not in allow and "hunter2" not in allow and "4321" not in allow and "(not measured)" in allow and "Home switch: no safety switch" in allow and "anything the agent says" in allow and "كلامي" in allow and "- ok: x" in allow
# --send applies it too: a hand-edited file cannot add a field
share.RUN = Path(tempfile.mkdtemp()); share.OUT = share.RUN / "share.md"
share.OUT.write_text("UrTV problem report, 2026-01-01, UrTV abc\n\nTV: Test TV\nWi-Fi password hunter2\n", encoding="utf-8")
sent.clear()
cli("--send")
send_ok = len(sent) == 1 and "hunter2" not in sent[0] and "TV: Test TV" in sent[0]
print("share: " + ("allowlist keeps only the report's own fields, free text only after the notes headings" if own and allowed and send_ok
                   else f"WRONG allowlist {own} {allowed} {send_ok}"))
# an email typed in the free text (the agent's notes, the owner's own words) is scrubbed too, also glued to Arabic
free = share.scrub(share.restrict("UrTV problem report, 2026-01-01, UrTV abc\n\nTV: Test TV\nNotes from the agent:\nmailed example.person@example.com about it\n"
                                  "From the owner:\nكتبت لهم علىsara.owner@example.org وما ردوا\n"))
free_ok = "example.person" not in free and "sara.owner" not in free and "<email>" in free and "كتبت لهم" in free
print("share: " + ("an email in the free text is scrubbed, also glued to Arabic" if free_ok else f"WRONG free-text email {free!r}"))
# the IP rule keeps firmware and version strings and removes real addresses; the Wi-Fi rule leaves our scripts' own lines alone and takes names from free text
ip_ok_words = ("V816.0.6.0.X", "TVOS-01.12.01.12", "6.0.1.2.3", "SDK 34")
ver_line = "!! Home: firmware V816.0.6.0.X, build TVOS-01.12.01.12, WebView 6.0.1.2.3, SDK 34"
ver_out = share.scrub(ver_line)
ip_line = f"!! no answer from {v4} and {v4}:5555, then 10.0.0.8."
ip_out = share.scrub(ip_line)
ip_ok = all(w in ver_out for w in ip_ok_words[:3]) and v4 not in ip_out and "10.0.0.8" not in ip_out and ip_out.count("<ip>") == 3 and "<ip>" not in ver_out
print("share: " + ("the IP rule keeps version strings and firmware names, removes addresses" if ip_ok else f"WRONG ip {ver_out!r} {ip_out!r}"))
own_line = "!! The setup wizard answers Home, but the TV did not say who owns its saved Wi-Fi network: left on (network: unknown)"
report = "UrTV problem report, 2026-01-01, UrTV abc\n\n" + own_line + "\n!! TV couldn't find HomeNetExample 5G and then could not find the TV\n!! Couldn't connect to \"Example Home\"\n\nNotes from the agent:\nwifi network: HomeNetFree and Wi-Fi: OtherNet\n\nFrom the owner:\nشبكتي network = MyOwnNet\n"
out_report = share.scrub(share.restrict(report))
wifi_ok = (own_line in out_report and "HomeNetExample" not in out_report and "Example Home" not in out_report and "could not find the TV" in out_report
           and "HomeNetFree" not in out_report and "OtherNet" not in out_report and "MyOwnNet" not in out_report)
print("share: " + ("our scripts' own lines keep their words; a Wi-Fi name is taken from free text and after couldn't find / connect to" if wifi_ok else f"WRONG wifi {out_report!r}"))
ask_ok = "اسم شبكة الواي فاي" in (Path(share.__file__).read_text(encoding="utf-8")) and "اسم شبكة الواي فاي" in (Path(share.__file__).resolve().parent.parent / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8")
print("share: " + ("the share step asks, in Arabic, not to write names, numbers or Wi-Fi names" if ask_ok else "WRONG: the Arabic sentence is missing"))
_extra = not (not net_leaks and own and allowed and send_ok and free_ok and ip_ok and wifi_ok and ask_ok)
if _extra: sys.exit(1)
sys.exit(1 if leaks or not counts or not alarm or not ok or bad_win or not versions_ok else 0)
