#!/usr/bin/env python3
"""4.3 UrTV Player's updates from inside it: the provider content://sa.urtv.home.player/update (READ_LANG, answered only to com.urtv.player with
its pinned certificate, one row when a newer verified Player is published, from the cache, refreshed in the background at most every 6 h), the
activity sa.urtv.home.action.INSTALL_PLAYER (started for a result by that caller only, the version asked must be the one found and newer; the
same install path as «تطبيقات مقترحة»: over the installed Player, never uninstalled), the tile's «تحديث متوفر», and the release asset
urtv-player.version (PlayerUpdate, run for real by tests/PlayerUpdateRun.java).
    python3 tests/player_update_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


mf, prov, act, mi, sa = (read(ROOT / "android/launcher/src/main/AndroidManifest.xml"), read(HOME / "PlayerUpdateProvider.java"),
                         read(HOME / "InstallPlayerActivity.java"), read(HOME / "MirrorInstall.java"), read(HOME / "SuggestedAppsActivity.java"))
block = mf[mf.index('android:name=".PlayerUpdateProvider"') - 30:]
block = block[:block.index("/>") + 2]
check("the provider: sa.urtv.home.player, exported behind READ_LANG (the normal permission the Player already holds), no new permission",
      'android:authorities="sa.urtv.home.player"' in block and 'android:permission="sa.urtv.permission.READ_LANG"' in block
      and not re.search(r'android:name="[^"]*PLAYER[^"]*"\s*android:protectionLevel', mf, re.I) and mf.count("<permission") == 7)   # no permission of its own: READ_LANG
check("the provider answers only /update, only to the Player with its key (null to anyone else), from the cache, the refresh in the background",
      '!("/" + PATH).equals(uri.getPath())) return null;' in prov and "String caller = getCallingPackage();" in prov
      and "if (!PlayerUpdate.callerOk(caller, MirrorInstall.signers(getContext().getPackageManager(), caller))) return null;" in prov
      and prov.index("callerOk") < prov.index("MirrorInstall.refreshAsync(getContext(), null);") < prov.index("MirrorInstall.known(getContext())")
      and 'COLUMNS = {"version_code", "version_name", "notes"}' in prov)
check("the provider is read only", prov.count('throw new UnsupportedOperationException("read only")') == 3)
check("the refresh never blocks the caller: a thread, at most every 6 h, a failed try not again for 10 minutes", 'new Thread(() -> {' in mi.split("static void refreshAsync(")[1].split("static boolean refresh(")[0]
      and "PlayerUpdate.refreshDue(cache(c), now)" in mi and "now - lastTry < 10 * 60_000L" in mi)
check("the refresh goes through Net (IPv4 first, a deadline), following one redirect to GitHub's file host only", "Net.get(url, h, 15_000, 25_000, max)" in mi
      and "PlayerUpdate.redirectOk(r.headers.get(\"location\"))" in mi)
ab = mf[mf.index('android:name=".InstallPlayerActivity"') - 30:]
ab = ab[:ab.index("</activity>")]
check("the activity: exported with the action sa.urtv.home.action.INSTALL_PLAYER", 'android:exported="true"' in ab and '<action android:name="sa.urtv.home.action.INSTALL_PLAYER" />' in ab)
check("the activity refuses before anything: no caller (not for a result), not the Player with its key, no version_code: RESULT_CANCELED with our words",
      "String caller = getCallingPackage();" in act and "PlayerUpdate.callerOk(caller, MirrorInstall.signers(getPackageManager(), caller)) || code <= 0" in act
      and act.index("callerOk") < act.index("new Thread(") and 'end(RESULT_CANCELED, S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));' in act)
check("the activity: the same install path as the tile, the version asked checked, RESULT_OK once Android's installer has it",
      "MirrorInstall.mirror(this, SuggestedApps.PLAYER_APP, new File(getCacheDir(), \"player-update.apk\"), RESULT, code)" in act
      and "PlayerUpdate.installOk(expectCode, v, installedCode(c))" in mi and "STATUS_PENDING_USER_ACTION" in act and "end(RESULT_OK, null)" in act)
check("an update keeps the Player's data: a plain install over it (MODE_FULL_INSTALL), never an uninstall", "MODE_FULL_INSTALL" in mi
      and not re.search(r"uninstall\(|ACTION_DELETE|ACTION_UNINSTALL", mi + act + prov))
check("the Player's install keeps every check: published SHA-256, the package, the pinned certificate", mi.index("SuggestedApps.parseSha(") < mi.index("download(urls[0], f)")
      < mi.index("SuggestedApps.mirrorFileOk(a, sha, published") < mi.index("commit(c, f, a.pkg, resultAction);"))
check("the tile: «تحديث متوفر» for an installed Player when a newer one is known, and OK installs it over", 'b.setText(S.t("تحديث متوفر"))' in sa
      and "MirrorInstall.known(this) != null" in sa and "if (installed(a.pkg) && !update) { open(a.pkg); return; }" in sa)
docs = read(ROOT / "docs/SETTINGS.md") + read(ROOT / "docs/FEATURES.md")
check("SETTINGS and FEATURES say it, with the release file's format", "sa.urtv.home.player/update" in docs and "urtv-player.version" in docs and "version_code=" in docs
      and "INSTALL_PLAYER" in docs)
if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SuggestedApps.java"), str(HOME / "PlayerUpdate.java"), str(ROOT / "tests/PlayerUpdateRun.java")],
                           capture_output=True, text=True)
        check("PlayerUpdate compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.PlayerUpdateRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"PlayerUpdate: every case ({len(lines)})", len(lines) == 17 and not bad, bad or out)
sys.exit(1 if failed else 0)
