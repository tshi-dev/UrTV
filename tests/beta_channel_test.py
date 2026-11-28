#!/usr/bin/env python3
"""4.3.3 «النسخ التجريبية»: the beta channel of «تحديث تلفزيوني». Off by default; on, the version and the APKs come from the apk-beta release (the
screensaver's newest-version line and the home's update screen); off again never downgrades; `saver.sh updatechannel beta|latest|next`; and the owner's
workflow promote-beta.yml copies an approved apk-next build to apk-beta as it is.   Run: python3 tests/beta_channel_test.py"""
import os
import re
import shutil
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
prefs, recv, act, up = text(APP + "Prefs.java"), text(APP + "SaverReceiver.java"), text(APP + "SettingsActivity.java"), text(APP + "Updater.java")
rules, chan, pu = text(HOME + "UpdateRules.java"), text(HOME + "UpdateChannel.java"), text(HOME + "PhoneUpdater.java")
hub, words, en = text(APP + "SettingsHub.java"), text(APP + "Words.java"), text("i18n/en/saver.tsv")

# the row
LINE = "تجيك التحديثات قبل الكل، وفيها أخطاء أحيانًا"   # the audit: at most 8 words, the warning kept
check("the row «النسخ التجريبية» is in the update page with its line", 'row("النسخ التجريبية"' in act and LINE in act and "toggleBeta()" in act)
check("off by default", 'sp.getString("updateChannel", "latest")' in prefs and "boolean betaOn()" in prefs and '"beta".equals(updateChannel())' in prefs)
tg = act.split("private void toggleBeta() {")[1].split("\n    }\n")[0]
check("the row flips beta and latest, and reads the release it now follows", 'wasOn ? "latest" : "beta"' in tg and "Updater.checkLatest(app)" in tg)
check("turning it off while the TV is ahead says it waits, and never downgrades", "wasOn" in tg and "newest < BuildConfig.VERSION_CODE" in tg and "Updater.BACK_LINE" in tg
      and 'BACK_LINE = "بترجع للنسخة العادية لما توصل لنفس الرقم أو أعلى"' in up)
check("the same line sits under the versions while the TV is ahead of the normal build", "if (back) out.add(BACK_LINE);" in up)
check("the row is searchable and in English", '{"النسخ التجريبية", UPDATE}' in hub and 'S.n("النسخ التجريبية")' in words and "\nالنسخ التجريبية\t" in en)
for w in ("بترجع للنسخة العادية لما توصل لنفس الرقم أو أعلى",):
    check("English and the marker for «" + w + "»", ("\n" + w + "\t") in en and 'S.n("' + w + '")' in words)

# the screensaver app's path
check("the newest-version line reads apk-beta's version.txt on the beta channel, never the latest's redirect",
      "releases/download/apk-beta/version.txt" in up and "next ? NEXT_VERSION_URL : beta ? BETA_VERSION_URL : VERSION_URL" in up and "!next && !beta" in up)
check("the receiver takes beta, and `show` says it", 'updatechannel is one of latest beta next' in recv and "on the beta channel" in recv)
check("saver.sh takes beta and keeps next", "updatechannel beta|latest|next" in text("optimize/saver.sh") and "updatechannel next" in text("optimize/saver.sh"))
check("never in export/import", not any(w in text("optimize/settings_move.py").split("#")[0] for w in ("updatechannel", "update_channel", "updateChannel")))

# the home's update screen
check("the rules know the apk-beta release and accept only our files from it", 'BETA_BASE = RELEASES + "apk-beta/"' in rules and "{BASE, NEXT_BASE, BETA_BASE}" in rules)
check("baseFor: beta is apk-beta, next is apk-next, anything else apk-latest", '"next".equals(channel) ? NEXT_BASE : "beta".equals(channel) ? BETA_BASE : BASE' in rules)
check("the home asks the provider for beta too, an unknown word is the latest", '"next".equals(v) || "beta".equals(v)' in chan and 'return "latest"' in chan)
check("the checks are untouched: no downgrade, same signature", "isNewer(newest, installed)" in text(HOME + "UpdateFlow.java") and "verify(c, pkg, f)" in pu)

check("the engine passes the beta flag, so no apk-beta release says «uptodate» (not «cannot update»)", 'versionRead(ver.reached, ver.code, newest, UpdateRules.BETA_BASE.equals(base))' in pu and 'if (v.result == UpdateFlow.Result.UP_TO_DATE) { set("uptodate", "", ""); return; }' in pu)

# the workflow
wf = text(".github/workflows/promote-beta.yml")
check("promote-beta is by hand only, with the build as input", re.search(r"^on:\n  workflow_dispatch:\n    inputs:\n      build:", wf, re.M) is not None and "push:" not in wf and "schedule:" not in wf and "workflow_run" not in wf)
check("it copies the source release's files as they are (no build, no signing step)", 'gh release download "$SRC"' in wf and "gradle" not in wf.lower() and "apksigner" not in wf and "keystore" not in wf.lower())
check("it refuses when the source holds another build than the one asked for", "$SRC holds build" in wf and "exit 1" in wf)
check("the source is apk-next (default) or apk-latest, nothing else (4.3.4: so beta TVs also get what everyone gets)", "default: apk-next" in wf and "- apk-latest" in wf and 'case "$SRC" in apk-next|apk-latest)' in wf)
check("it publishes the APKs and version.txt to apk-beta, version.txt last", "urtv-screensaver.apk" in wf and "urtv-home.apk" in wf and wf.rindex("next/version.txt") > wf.rindex('"${apks[@]}"') and "apk-beta" in wf)
check("R-288 (4.4 review D-F1): promote-beta copies EVERY .apk the source holds (not a fixed list), requires home, screensaver and extras there, and uploads version.txt last",
      "--pattern '*.apk'" in wf and "--pattern 'version.txt'" in wf and "for f in urtv-screensaver.apk urtv-home.apk urtv-extras.apk version.txt" in wf
      and 'apks=(next/*.apk)' in wf and wf.rindex("next/version.txt") > wf.rindex('"${apks[@]}"'))
check("R-288: after uploading it checks apk-beta holds every asset name of the source and fails the job otherwise; it deletes nothing",
      "comm -23 source-assets.txt beta-assets.txt" in wf and "apk-beta lacks" in wf and wf.index("apk-beta lacks") > wf.rindex("next/version.txt") and "exit 1" in wf.split("apk-beta lacks")[1][:40]
      and not re.search(r"release\s+delete|delete-asset|gh api[^\n]*DELETE", wf))
check("the apk-beta release is a pre-release and never the repository's latest", "--prerelease" in wf and "--latest=false" in wf)
check("nothing else promotes: no other workflow writes apk-beta", not any(re.search(r"tag_name:\s*apk-beta|release\s+(edit|create|upload|delete)\s+apk-beta|--tag\s+apk-beta", p.read_text(encoding="utf-8")) for p in (ROOT / ".github/workflows").glob("*.yml") if p.name != "promote-beta.yml"))
if shutil.which("python3"):
    try:
        import yaml
        doc = yaml.safe_load(wf)
        check("it is valid YAML: the release gate on the source build's commit, then the one job that promotes, only after it (docs/dev/RELEASE.md)",
              set(doc["jobs"]) == {"source", "gate", "promote"} and doc["jobs"]["gate"].get("uses") == "./.github/workflows/regressions.yml"
              and doc["jobs"]["promote"].get("needs") == "gate")
    except ImportError:
        if os.environ.get("CI"): check("PyYAML is installed in CI (the workflow guard needs it)", False, "pip install pyyaml")
        else: print("SKIP yaml: not installed")

# the rules run
if not shutil.which("javac"):
    print("SKIP rules: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    (Path(tmp) / "R.java").write_text("""package sa.urtv.home.phone;
public class R { public static void main(String[] a) {
  String b = "https://github.com/tshi-dev/UrTV/releases/download/";
  boolean ok = UpdateRules.baseFor("beta").equals(b + "apk-beta/") && UpdateRules.baseFor("next").equals(b + "apk-next/") && UpdateRules.baseFor("latest").equals(b + "apk-latest/")
    && UpdateRules.baseFor("x").equals(b + "apk-latest/") && UpdateRules.baseFor(null).equals(b + "apk-latest/")
    && UpdateRules.isReleaseUrl(b + "apk-beta/version.txt") && UpdateRules.isReleaseUrl(b + "apk-beta/urtv-home.apk") && !UpdateRules.isReleaseUrl(b + "apk-beta/other.apk")
    && !UpdateRules.isReleaseUrl(b + "apk-beta2/version.txt") && UpdateRules.isReleaseUrl(b + "apk-latest/version.txt")
    && !UpdateFlow.isNewer(500, 512) && !UpdateFlow.isNewer(512, 512) && UpdateFlow.first(500, 512, true).result == UpdateFlow.Result.UP_TO_DATE;
  System.out.println(ok ? "ALL PASS" : "FAILED"); System.exit(ok ? 0 : 1); } }
""", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / HOME / "UpdateRules.java"), str(ROOT / HOME / "UpdateFlow.java"), str(ROOT / HOME / "UpdateOrder.java"), str(Path(tmp) / "R.java")], capture_output=True, text=True)
    check("the rules compile", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.R"], capture_output=True, text=True).stdout
    check("beta/next/latest bases, our files only, and an older stable build is never installed over a newer beta", "ALL PASS" in out, out)
sys.exit(1 if failed else 0)
