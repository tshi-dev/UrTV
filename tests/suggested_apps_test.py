#!/usr/bin/env python3
"""4.3 «تطبيقات مقترحة»: the catalog (SuggestedApps, run for real by tests/SuggestedAppsRun.java), the settings screen that installs from it
(SuggestedAppsActivity: each file from its own GitHub releases, the package checked, Android's install screen
asks), the row in the screensaver's settings, and the IPTV player: one, its Play Store page only, a player only, no channels or links of ours.   python3 tests/suggested_apps_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

cat = (HOME / "SuggestedApps.java").read_text(encoding="utf-8")
act = (HOME / "SuggestedAppsActivity.java").read_text(encoding="utf-8")
mf = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
st = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
inst = (HOME / "setup/SetupInstall.java").read_text(encoding="utf-8")
apks = (HOME / "setup/SetupApks.java").read_text(encoding="utf-8")

check("the catalog says the IPTV player is a player only, its store page: no channels, and we give none", "it comes with no channels, films or playlists, and we give no links" in cat
      and "وما فيه ولا نعطي أي قنوات" in cat and 'new App("tivimate", TIVIMATE, "TiviMate"' in cat and "Kind.STORE, null, null, 21, false)," in cat.split('new App("tivimate"')[1][:200])
check("Lumen is gone everywhere (the owner's decision): no entry, no pinned file, no hash, no words", not any("lumen" in t.lower() for t in (cat, act, inst, apks,
      (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8"), (ROOT / "CHANGELOG.md").read_text(encoding="utf-8"))) and "PINNED" not in cat + act + inst + apks)
check("no playlist, stream or IPTV address anywhere in the catalog or the screen", not re.search(r"\.m3u8?\b|get\.php|xtream|/playlist", cat + act, re.I))
check("the screen: the file from the app's own GitHub releases and the package checked before Android's install screen; https only; the size limit",
      act.index("!a.pkg.equals(info.packageName)") < act.index("commit(f, a.pkg);")
      and '"https".equals(con.getURL().getProtocol())' in act and "UpdateRules.MAX_APK_BYTES" in act and "SuggestedApps.githubOk(a, url)" in act)
check("the screen asks Android's own install screen (PackageInstaller, the owner confirms), the file goes after", "STATUS_PENDING_USER_ACTION" in act
      and "session.commit(" in act and "f.delete();" in act.split("private void commit(")[1])
check("a Play Store app opens its page (the store, else the web page); an installed app opens", 'market://details?id=' in act and "getLeanbackLaunchIntentForPackage" in act)
check("the screen is declared, and the screensaver's settings open it under «التطبيقات والمفضلة»", 'android:name=".SuggestedAppsActivity"' in mf
      and 'row("تطبيقات مقترحة"' in st and '"sa.urtv.home.SuggestedAppsActivity"' in st
      and st.index('row("تطبيقات في الشريط"') < st.index('row("تطبيقات مقترحة"'))
check("UrTV Player in the screen: the newest «player-» release, its published SHA-256, the package and the pinned certificate before the install; refused unless pinned",
      "if (a.kind == SuggestedApps.Kind.MIRROR) return mirror(a, f);" in act and "if (!SuggestedApps.mirrorReady(a))" in act
      and act.index("SuggestedApps.mirrorFileOk(a, sha, published") < act.split("private String mirror(")[1].index("commit(f, a.pkg);") + act.index("private String mirror(")
      and "GET_SIGNING_CERTIFICATES" in act and 'PLAYER_CERT = "13ade7d009a07f5ba5bab0b7734bd449df10dd47b53868dfa28e44cd8069dfea";' in cat)
check("the setup installs from the same catalog: GitHub releases only, a Play Store app never", "SuggestedApps.githubOk(a, url)" in inst and "if (a == null || !a.inSetup()) continue;" in inst)
act_setup = (HOME / "setup/SetupActivity.java").read_text(encoding="utf-8")
check("the setup's boxes: one per suggested app, the on/off look of the screen's other rows (paintLang), OK ticks or unticks, one already on the TV left",
      "SuggestedApps.forSetup(android.os.Build.VERSION.SDK_INT)" in act_setup and "paintLang(box, picks.contains(a.id), box.hasFocus());" in act_setup
      and 'S.t("مثبّت من قبل: نتركه")' in act_setup and "☑" not in act_setup)
rows = re.findall(r'\{"([^"]+)", (TVBRO|SMARTTUBE), "([^"]+)"\}', inst)
check("SmartTube and TV Bro: the same repositories and file patterns as the setup's (install.sh's)",
      all(f'"{repo}", "{pat}"' in cat.replace("\n            ", " ").replace(",\n", ",") or (repo in cat and pat.replace("\\\\", "\\") in cat.replace("\\\\", "\\")) for repo, _, pat in rows) and len(rows) == 2, rows)

if not shutil.which("javac"):
    print("SKIP the catalog's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SuggestedApps.java"), str(ROOT / "tests/SuggestedAppsRun.java")], capture_output=True, text=True)
        check("SuggestedApps compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SuggestedAppsRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"SuggestedApps: every case ({len(lines)})", len(lines) == 17 and not bad, bad or out)
sys.exit(1 if failed else 0)
