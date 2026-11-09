#!/usr/bin/env python3
"""4.3 #53 «اكتب وارسم», the text: a paired phone sends a short message (POST /api/message, behind the session like every change) and the TV
shows it for 20 s over what plays (MessageActivity, not exported, see-through); Back hides it. «رسائل الجوال» turns it off and «فوق التطبيقات»
keeps it to our home and the screensaver (optimize/dock.sh message …). The rules run for real (tests/PhoneMessageRun.java).
    python3 tests/phone_message_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main"
P = L / "java/sa/urtv/home/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")

api, acts, man, act = read(P / "PhoneApi.java"), read(P / "PhoneActions.java"), read(L / "AndroidManifest.xml"), read(L / "java/sa/urtv/home/MessageActivity.java")
check("the route is behind the session (after «everything below needs a phone the owner allowed»)",
      api.index("everything below needs a phone the owner allowed") < api.index('case "/api/message": endpoint = "message"') and 'case "message": return result(a.message(' in api)
m = re.search(r'<activity\s+android:name="\.MessageActivity"(.*?)/>', man, re.S)
check("MessageActivity: not exported, see-through, no recents, one at a time", m is not None and 'android:exported="false"' in m.group(1)
      and "Theme.Translucent" in m.group(1) and 'excludeFromRecents="true"' in m.group(1) and 'launchMode="singleTop"' in m.group(1), m and m.group(1))
check("the message shows 20 s, a new one replaces it, Back hides it and is used up",
      "handler.postDelayed(close, PhoneMessage.SHOW_MS);" in act and "onNewIntent" in act and "if (code == KeyEvent.KEYCODE_BACK) { finish(); return true; }" in act
      and "SHOW_MS = 20_000" in read(P / "PhoneMessage.java"))
check("the TV decides with the two switches and whether our screen is in front; only a SHOW opens the screen",
      "MESSAGES.decide(text, sa.urtv.home.MessageActivity.on(app), sa.urtv.home.MessageActivity.overApps(app), ours" in acts
      and "if (v != PhoneMessage.Verdict.SHOW) return Result.fail(PhoneMessage.code(v));" in acts)
check("both switches on by default", 'getBoolean("phone_message", true)' in act and 'getBoolean("phone_message_over_apps", true)' in act)
dock, recv = read(ROOT / "optimize/dock.sh"), read(L / "java/sa/urtv/home/DockReceiver.java")
check("optimize/dock.sh message on|off and message overapps on|off; the receiver answers how both stand",
      'send PHONE_MESSAGE "--es mode $2"' in dock and 'send PHONE_MESSAGE "--es mode status --es overapps $3"' in dock and 'case "sa.urtv.home.PHONE_MESSAGE":' in recv
      and '<action android:name="sa.urtv.home.PHONE_MESSAGE" />' in man)
check("docs/SETTINGS.md names the commands", "`optimize/dock.sh message off`" in read(ROOT / "docs/SETTINGS.md") and "`optimize/dock.sh message overapps off`" in read(ROOT / "docs/SETTINGS.md"))
for f in ("index.html", "index.en.html"):
    page = read(L / "assets/phone" / f)
    check(f"phone/{f}: the card, 200 characters at most, sent as one line, and the TV's answers", 'id="msgT"' in page and 'maxlength="200"' in page
          and "api('/api/message', { text: t.slice(0, 200) })" in page and all(k in page for k in ("message_shown:", "message_off:", "message_busy_app:", "slow_down:")))
if not shutil.which("javac"):
    print("SKIP the rules' run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(P / "PhoneMessage.java"), str(P / "Schema.java"), str(ROOT / "tests/PhoneMessageRun.java")], capture_output=True, text=True)
        check("PhoneMessage and Schema compile on their own (plain Java)", r.returncode == 0, r.stderr[-600:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.PhoneMessageRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                n, res = line.rsplit("\t", 1)
                check(n, res == "ok")
            check("every case ran", len(out.splitlines()) == 11, len(out.splitlines()))
sys.exit(1 if failed else 0)
