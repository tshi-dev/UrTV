#!/usr/bin/env python3
"""The games' content packs (GamePacks.java on a plain JVM; the pins on PackStore and the packs screen; docs/GAMES-PACKS.md)."""
import shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
if not shutil.which("javac"): print("SKIP: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as t:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", t, str(APP / "GamePacks.java"), str(APP / "MiniJson.java"), str(ROOT / "tests/GamePacksTest.java")], capture_output=True, text=True)
    check("GamePacks compiles on a plain JVM", c.returncode == 0, c.stderr[-400:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", t, "sa.urtv.screensaver.GamePacksTest"], capture_output=True, text=True, timeout=60)
        for l in r.stdout.splitlines():
            if l.startswith(("PASS", "FAIL")): print(l); failed += l.startswith("FAIL")
ps = (APP / "PackStore.java").read_text(encoding="utf-8"); act = (APP / "GamePacksActivity.java").read_text(encoding="utf-8"); gl = (APP / "QuizGamesActivity.java").read_text(encoding="utf-8")
check("a pack is downloaded only when its game opens: not during a round, not when the TV is low on space, only after the sha256 matches, written whole then renamed",
      "QuizBank.playing" in ps and "GamePacks.canDownload(" in ps and "GamePacks.shaOk(d.body, p.sha)" in ps and "tmp.renameTo(file(app, id))" in ps and "d.body.length != p.bytes" in ps)
check("the packs come from urtv-data over Net (a size cap, no redirect) and nothing about the TV is sent", "tshi-dev/urtv-data/main/games/packs/" in ps and "Net.get(" in ps and "PACK_MAX" in ps and "getAndroidId" not in ps and "Build.SERIAL" not in ps)
check("the packs screen lists size, «احتفظ دائمًا» / «تلقائي» and «حذف الآن»; the games list sweeps expired packs when it opens",
      'S.t("حذف الآن")' in act and 'S.t("احتفظ دائمًا")' in act and 'S.t("تلقائي")' in act and "PackStore.sweep(this)" in gl)
doc = (ROOT / "docs/GAMES-PACKS.md").read_text(encoding="utf-8") if (ROOT / "docs/GAMES-PACKS.md").exists() else ""
check("docs/GAMES-PACKS.md says where packs live, the two modes, the 14 days, the low-storage rule and how a pack is built", all(w in doc for w in ("games/packs/", "احتفظ دائمًا", "14 days", "400 MB", "pack_bundle.py", "index.json")))
sys.exit(1 if failed else 0)
