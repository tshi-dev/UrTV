#!/usr/bin/env python3
"""«زر تلاوة وأذان» can be hidden (`optimize/saver.sh listenpill on|off`, a row in the screensaver's settings): default shown, hiding removes only
the button, the player stays reachable (app menu, «يُتلى الآن» card), the family lock still guards the way in, and the adhan, its bar, the any-key
stop and the card do not depend on it. The apps need Android, so the code is read from the source; the command is run against a stand-in for tv.py.
Run: python3 tests/listen_pill_test.py"""
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


text = lambda p: Path(p).read_text(encoding="utf-8")
prefs, receiver, provider, settings = text(APP / "Prefs.java"), text(APP / "SaverReceiver.java"), text(APP / "ConfigProvider.java"), text(APP / "SettingsActivity.java")
snap, main = text(HOME / "ConfigSnapshot.java"), text(HOME / "MainActivity.java")

# ---- the setting: shown unless it says off
check("the default is shown (nothing changes for anyone)", 'boolean listenPill() { return sp.getBoolean("listenPill", true); }' in prefs)
check("the ADB receiver takes on or off and refuses the rest before saving", 'listenpill is one of on off' in receiver
      and receiver.index('listenpill is one of on off') < receiver.index('p.setListenPill("on".equals(listenPill))'))
check("`saver.sh show` says it", '"\\nlistenpill: " + (p.listenPill() ? "on" : "off")' in receiver)
check("the provider hands it to the launcher as listen_pill", '"listen_pill"' in provider and 'p.listenPill() ? "1" : "0"' in provider)
check("the launcher reads it, and an older screensaver without the column means shown",
      'getColumnIndex("listen_pill")' in snap and 'listenPill = true' in snap and '"0".equals(cur.getString(l))) listenPill = false' in snap)
row = settings.split('row("زر تلاوة وأذان"')[1].split("});")[0]
check("the screensaver's settings have a row for it, and its help line says where the screen opens from when it is hidden",
      "prefs.listenPill()" in row and "مخفي" in row and "«افتح تلاوة وأذان»" in row and "يُتلى الآن" in row, row)

# ---- the home screen: out of the row, not just invisible
apply = main.split("private void applyListenPill(boolean setting) {")[1].split("\n    }\n")[0]
check("hiding takes the button out of the row and out of the focus list (a hidden sibling would stop the walk along the row)",
      "pillRow.removeView(tilawahPill)" in apply and "pills.remove(tilawahPill)" in apply and "setVisibility" not in apply, apply)
check("the focus goes to the pill before it when the hidden button had it", "hadFocus" in apply and "pills.get(pills.size() - 1).requestFocus()" in apply, apply)
check("it comes back at the end of the row, where it always was", "pillRow.addView(tilawahPill" in apply and "pills.add(tilawahPill)" in apply, apply)
check("the row is built from the setting too (no flash of the button on a TV that hid it)", "listenPillWanted(ConfigSnapshot.read(this).listenPillOn)) row.add(tilawah)" in main)
check("a build without the listening part (R.bool.show_listening false) never shows it, whatever the setting says",
      "return setting && getResources().getBoolean(R.bool.show_listening);" in main)
tick = main.split("applyListenPill(cfg.listenPillOn);")
check("the minute tick applies the setting", len(tick) == 2 and "updateAdhanBar(cfg);" in tick[0][-80:], "")
menu = main.split("private void showAppMenuNow(")[1].split("showMenu(anchor, labels, actions);")[0]
check("hidden: the settings' «افتح تلاوة وأذان» opens the screen through the home, behind the same family lock as the button (the app's options are about the app only)",
      'row("افتح تلاوة وأذان"' in settings and 'openHome("listen")' in settings and "تلاوة وأذان" not in menu)
check("shown: the button itself is still behind that lock",
      'tilawah.setOnClickListener(v -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class)' in main)
check("the «يُتلى الآن» card still opens the player (long OK) behind the same lock",
      'nowCard.setOnLongClickListener(v -> { launchGuarded(ParentalPolicy.PILL_LISTEN, NowPlaying.player(this)' in main)

# ---- the adhan's settings are reachable without the button, from the same screen (no second copy of the logic)
til = text(HOME / "tilawah/MainActivity.java")
check("«تلاوة وأذان» can open on its adhan tab (EXTRA_OPEN = adhan)", 'public static final String EXTRA_OPEN = "open";' in til
      and 'selectTab("adhan".equals(getIntent().getStringExtra(EXTRA_OPEN)) ? TAB_ADHAN : TAB_RECITERS)' in til)
adhan_rows = til.split("KIND_ADHAN_TOGGLE, getString(R.string.adhan_auto)")[1].split("notifyDataSetChanged")[0] if "KIND_ADHAN_TOGGLE, getString(R.string.adhan_auto)" in til else ""
check("that tab has everything: on or off, the voice, the Fajr voice, short, any key stops it, the volume",
      all(k in til for k in ("KIND_ADHAN_TOGGLE", "KIND_VOICE", "KIND_FAJR_VOICE", "KIND_SHORT", "KIND_STOPKEY", "KIND_VOLUME")), "")
check("hidden: the settings' «الأذان» opens the same screen on its adhan tab, through the home, behind the same family lock",
      "openAdhanSettings(); } });" in settings and 'launchGuarded(ParentalPolicy.PILL_LISTEN, adhanSettingsIntent(), S.t("إعدادات الأذان"))' in main)
check("adhanSettingsIntent opens it on the adhan tab", 'putExtra(sa.urtv.home.tilawah.MainActivity.EXTRA_OPEN, "adhan")' in main)
req = main.split("private void openRequested(Intent i) {")[1].split("\n    }\n")[0]
check("the home opens the adhan settings when asked with open=adhan, behind the family lock (shown or hidden button), and forgets the request",
      'String what = i == null ? null : i.getStringExtra("open");' in req and 'case "adhan":' in req and 'i.removeExtra("open")' in req
      and req.index('i.removeExtra("open")') < req.index("switch (what)") and "launchGuarded(ParentalPolicy.PILL_LISTEN, adhanSettingsIntent()" in req.split('case "adhan":')[1].split("break;")[0], req)
check("open=listen opens «تلاوة وأذان» behind the same lock as its button, open=center the panel behind its pill's lock (none in kids mode)",
      'launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class)' in req.split('case "listen":')[1].split("break;")[0]
      and "guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(centerPill, ccview))" in req.split('case "center":')[1].split("break;")[0]
      and "!Parental.state(this).kids" in req.split('case "center":')[1].split("break;")[0], req)
check("it asks on create and on a new intent (the home is singleTask)", "openRequested(getIntent());" in main and "openRequested(intent);" in main.split("protected void onNewIntent(Intent intent) {")[1].split("\n    }\n")[0])
row2 = settings.split('row("الأذان"')[1].split("});")[0]
check("the screensaver's settings have an «الأذان» row that brings the home up with open=adhan (the lock applies there, not here)",
      "openAdhanSettings()" in row2 and 'setClassName("sa.urtv.home", "sa.urtv.home.MainActivity").putExtra("open", "adhan")' in settings, row2)

# ---- nothing else depends on the button
others = [f.name for f in sorted(list(HOME.rglob("*.java")) + list(APP.glob("*.java"))) if "listenPill" in text(f) or "listen_pill" in text(f) or "tilawahPill" in text(f)]
check("only the setting's own files and the home screen know about the button: not the adhan, its alarm or bar, the stop key or the card",
      set(others) == {"Prefs.java", "SaverReceiver.java", "ConfigProvider.java", "SettingsActivity.java", "ConfigSnapshot.java", "MainActivity.java"}, others)
adhan_bar = text(HOME / "AdhanBar.java") + text(HOME / "AdhanKeys.java") + text(HOME / "NowCard.java")
check("the adhan bar, the any-key stop and the card never look at it", "listenPill" not in adhan_bar and "tilawahPill" not in adhan_bar, "")

# ---- the command
tmp = tempfile.mkdtemp()
log = os.path.join(tmp, "sent.txt")
fake = os.path.join(tmp, "fakepy")
Path(fake).write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)


def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()


for v in ("on", "off"):
    rc, out, sent = run("listenpill", v)
    check(f"saver.sh listenpill {v}: one SET with the value quoted", rc == 0 and sent.endswith(f"screensaver.SET --es listenpill '{v}'"), (rc, sent))
for bad in ("yes", "on off", "off; reboot"):   # the receiver refuses these (read above); the shell only ever sends them quoted
    rc, out, sent = run("listenpill", bad)
    check(f"saver.sh listenpill {bad!r}: sent as one quoted word, for the receiver to refuse", sent.endswith(f"--es listenpill '{bad}'"), (rc, sent))
rc, out, sent = run("listenpill", "it's")
check("saver.sh listenpill with an apostrophe: refused in the shell, nothing sent", rc != 0 and not sent, (rc, out, sent))
rc, out, sent = run("listenpill")
check("saver.sh listenpill with no value: usage, nothing sent", rc != 0 and not sent, (rc, out, sent))
# the TV refuses anything else (the receiver says so; the stand-in only logs): the receiver's own check is read above
check("the settings export and import carry it as listen_pill", '"listen_pill"' in text(ROOT / "optimize/settings_move.py") and 'es("listenpill", flag(v))' in text(ROOT / "optimize/settings_move.py"))
check("SETTINGS.md and FEATURES.md and SETUP-PROMPT.md say it", all("listenpill" in text(ROOT / "docs" / d) for d in ("SETTINGS.md", "FEATURES.md", "SETUP-PROMPT.md")))
# restore / re-show fixes found in review
home = text(ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java")
check("open=adhan is handled at creation only on a fresh start (not after a restore or from history)",
      "b == null && (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {\n            openRequested(getIntent());\n            phoneRequested(getIntent());" in home)
check("onNewIntent keeps the new intent", "super.onNewIntent(intent);\n        setIntent(intent);" in home)
check("a pill shown or hidden again re-wires up/down", re.search(r"requestFocus\(\);\n        \}\n        wireFocus\(\);", home) is not None)
tw = text(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/MainActivity.java")
check("the player opened again with open=adhan shows the adhan tab", "void onNewIntent(Intent intent)" in tw and "selectTab(TAB_ADHAN);\n    }" in tw)
sys.exit(1 if failed else 0)
