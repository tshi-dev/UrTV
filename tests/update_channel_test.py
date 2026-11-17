#!/usr/bin/env python3
"""The hidden update channel (`saver.sh updatechannel next|latest`, testing only): off by default, in `show`, never in the settings export,
sent quoted, validated by the receiver, and the updaters read the channel's release with the checks unchanged."""
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


APP = "android/app/src/main/java/sa/urtv/screensaver/"
HOME = "android/launcher/src/main/java/sa/urtv/home/phone/"
prefs, recv, prov = text(APP + "Prefs.java"), text(APP + "SaverReceiver.java"), text(APP + "ConfigProvider.java")
check("off by default: anything but next or beta is latest", '"next".equals(v) || "beta".equals(v) ? v : "latest"' in prefs and 'setUpdateChannel(String v)' in prefs)
check("the receiver takes latest or next only and refuses another word", 'error: updatechannel is one of latest beta next' in recv and 'p.setUpdateChannel(updateChannel)' in recv)
check("`saver.sh show` says it", '"\\nupdatechannel: " + p.updateChannel()' in recv)
_cols = re.search(r'new MatrixCursor\(new String\[\] \{("fragment"[^}]*)\}\);', prov)
check("the provider tells the home app in its own column, after the older ones (a column added later goes after it: none moves)",
      _cols is not None and re.findall(r'"(\w+)"', _cols.group(1))[:7] == ["fragment", "home_panel", "greeting", "home_panels", "adhan_bar", "listen_pill", "update_channel"]
      and 'p.listenPill() ? "1" : "0", p.updateChannel()' in prov, _cols and _cols.group(1))
check("the home app asks the provider and treats anything but next or beta as latest", '"next".equals(v) || "beta".equals(v)' in text(HOME + "UpdateChannel.java") and 'return "latest"' in text(HOME + "UpdateChannel.java"))
pu = text(HOME + "PhoneUpdater.java")
check("version.txt, the sizes and the files all come from the channel's release (read once)", 'readVersion(base + "version.txt")' in pu and 'sizeOf(base + ' in pu and 'fetch(base + ' in pu and "UpdateRules.BASE +" not in pu and "UpdateRules.VERSION_URL" not in pu)
check("the channel is read once per run and kept (a flip mid-run cannot mix releases)", pu.count("UpdateChannel.read(c)") == 1 and "final String base = releaseBase;" in pu and "base(c)" not in pu, "")
check("no unused VERSION_URL in the rules", "VERSION_URL" not in text(HOME + "UpdateRules.java"))
check("the signature, newer-version and permission checks are untouched (verify still runs on every file, the install permission is still asked)", "verify(c, pkg, f)" in pu and "canInstall(c)" in pu)
up = text(APP + "Updater.java")
check("the newest-version line follows the channel, and the next channel never falls back to the latest's redirect", 'latestFromAsset(next ? NEXT_VERSION_URL : beta ? BETA_VERSION_URL : VERSION_URL)' in up and 'if (v == null && !next && !beta)' in up)
sm = text("optimize/settings_move.py")
sm_code = "\n".join(l.split("#", 1)[0] for l in sm.splitlines())   # comments may say it is never there (settings_move.py does)
check("never in export/import (the allow-list does not name it)", "updatechannel" not in sm_code and "update_channel" not in sm_code and "updateChannel" not in sm_code)
check("no screen row for it by word (the beta row is its own: tests/beta_channel_test.py)", '"updatechannel"' not in text(APP + "SettingsActivity.java").lower() and "update_channel" not in text(APP + "SettingsActivity.java"))
docs = text("docs/SETTINGS.md")
check("SETTINGS.md documents it as testing only", "updatechannel" in docs and "testing only" in docs)
check("the apk-next release carries version.txt (CI), or the channel could not read a version", re.search(r"apk-next.*?version\.txt", text(".github/workflows/android.yml"), re.S) is not None)

# every `files: |` block of the workflows is a literal block: a trailing «# comment» would be part of the file pattern (it then matches nothing and
# the file is silently not uploaded), so every line of it is a bare file name
for wf in sorted((ROOT / ".github/workflows").glob("*.yml")):
    lines = wf.read_text(encoding="utf-8").split("\n")
    for n, line in enumerate(lines):
        if re.match(r"\s*files:\s*\|\s*$", line):
            indent = len(line) - len(line.lstrip())
            block = []
            for nxt in lines[n + 1:]:
                if nxt.strip() and len(nxt) - len(nxt.lstrip()) <= indent:
                    break
                if nxt.strip():
                    block.append(nxt.strip())
            check(f"{wf.name}:{n + 1} files block: every line a bare file name, no comment", bool(block) and all(re.fullmatch(r"[A-Za-z0-9_.\-*/]+", b) for b in block), block)
            check(f"{wf.name}:{n + 1} the publish step fails when a listed file matches nothing", "fail_on_unmatched_files: true" in "\n".join(lines[max(0, n - 2):n]), lines[max(0, n - 2):n])
            if "apk-next" in "\n".join(lines[max(0, n - 6):n]):
                check("the apk-next release uploads exactly the two APKs and version.txt", block == ["urtv-screensaver.apk", "urtv-home.apk", "version.txt"], block)

check("the newest version shown is the one of the current channel: it is stored with its channel, a value from before a change is not shown, and a change reads it again",
      'putString("latestChannel", ch)' in up and 'updateChannel().equals(sp.getString("latestChannel", "latest"))' in prefs and "p.latestVersion()" in recv
      and "Updater.refreshLatestAsync(c);" in recv and "new Prefs(c).latestVersion()" in up)
check("`saver.sh show` says when the newest number is the next channel's", '", on the next channel"' in recv)

tmp = tempfile.mkdtemp()
log = os.path.join(tmp, "sent")
fake = os.path.join(tmp, "fakepy")
Path(fake).write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)


def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()


for v in ("next", "latest"):
    rc, out, sent = run("updatechannel", v)
    check(f"saver.sh updatechannel {v}: one SET with the value quoted", rc == 0 and sent.endswith(f"screensaver.SET --es updatechannel '{v}'"), (rc, sent))
rc, out, sent = run("updatechannel", "it's")
check("an apostrophe: refused in the shell, nothing sent", rc != 0 and not sent, (rc, out, sent))
rc, out, sent = run("updatechannel")
check("no value: usage, nothing sent", rc != 0 and not sent, (rc, out, sent))
sys.exit(1 if failed else 0)
