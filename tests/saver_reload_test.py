#!/usr/bin/env python3
"""A screensaver that is already on the screen starts over when a SET saves something (the first `optimize/start-saver.sh` after
`optimize/saver.sh city <new>` showed the previous city: the page reads its settings once, when it starts, and a dream that was
already running kept the old ones). Read from the source, as the apps need Android: SaverReceiver.set sends Prefs.CHANGED to the app
itself after saving, and the dream and the preview listen while they are on the screen and build a fresh page.   Run: python3 tests/saver_reload_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


text = lambda n: (APP / n).read_text(encoding="utf-8")
receiver, dream, preview, prefs = text("SaverReceiver.java"), text("ScreenDream.java"), text("MainActivity.java"), text("Prefs.java")

check("Prefs.CHANGED is a broadcast name of this app", re.search(r'static final String CHANGED = "sa\.urtv\.screensaver\.SETTINGS_CHANGED";', prefs) is not None)
setter = receiver.split("static String set(Context c, Prefs p, Intent i) {")[1].split("\n    }\n")[0]
sent = "c.sendBroadcast(new Intent(Prefs.CHANGED).setPackage(c.getPackageName()));"
check("SaverReceiver.set sends it, to this app only, after everything is saved and before the answer", sent in setter
      and setter.index('p.setList("leagues", leagues);') < setter.index(sent) < setter.index('return "saved\\n"'), setter[-400:])
check("and never on a refusal: every `return \"error:` comes before the saves, so none reaches the broadcast",
      setter.replace('if (!p.flush()) return "error: the settings could not be written', '').rindex('return "error:') < setter.index("if (city != null) p.setCity("), "")   # 4.4 R-305: the flush's own error comes after the saves on purpose (nothing was written, nothing is broadcast)
for name, src, flag in (("the dream", dream, "attached"), ("the preview", preview, "alive")):
    check(f"{name} listens for it while it is on the screen (registered when it starts, unregistered when it ends)",
          "InternalReceivers.register(this, changed, new IntentFilter(Prefs.CHANGED))" in src and "unregisterReceiver(changed)" in src, "")
    handler = src.split("onReceive(Context c, Intent i) {")[1].split("\n        }\n")[0]
    # 4.4 review F1: the receiver hands over to rebuildIfChanged (a change during the async start is kept and looked at once the page exists)
    rebuild = (src.split("private void rebuildIfChanged(Context c) {")[1] if "private void rebuildIfChanged(Context c) {" in src else src.split("private void rebuild() {")[1]).split("\n    }\n")[0]
    if "private void rebuild() {" in src and "rebuild();" in rebuild:   # 4.4 beta 4 (PagePush): rebuildIfChanged hands a change the page cannot take to rebuild()
        rebuild += src.split("private void rebuild() {")[1].split("\n    }\n")[0]
    check(f"{name} throws the old page away and builds a new one, only while it is up",
          f"if (!{flag}" in (handler + rebuild) and "web == null) return;" in rebuild and "ScreenView.release(old)" in rebuild and "show(" in rebuild
          and rebuild.index("ScreenView.release(old)") < rebuild.index("show("), rebuild)
sys.exit(1 if failed else 0)
