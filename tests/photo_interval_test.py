#!/usr/bin/env python3
"""«تبديل الصورة كل» with 1 minute and 30 seconds (PhotoInterval.java): 30 s only on a TV with 2 GB of memory, 1 minute the shortest below.
Read from the source: the memory read (once, ActivityManager), the pref read through PhotoInterval, the receiver's check before anything is
saved, the picker offering this TV's choices, the fragment's 0.5 and the page; then `optimize/saver.sh photos 30s` against a stand-in for
tv.py, and the JVM part (needs javac; without it, skipped with a note). The page itself: tests/page.test.mjs.
Run: python3 tests/photo_interval_test.py"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
receiver = (APP / "SaverReceiver.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
check("the memory is read once with ActivityManager.getMemoryInfo (totalMem, in MB)",
      "am.getMemoryInfo(mi);" in prefs and "mb = mi.totalMem / (1024 * 1024);" in prefs and "if (ramMb < 0) {" in prefs)
check("the pref is read through PhotoInterval (30 minutes by default; 30s is 1 minute under 2 GB)",
      'return PhotoInterval.read(sp.getString("photoMinutes", PhotoInterval.DEFAULT), ramMb(ctx));' in prefs)
check("the fragment sends 0.5 for 30 seconds", '+ "&photos=" + PhotoInterval.fragmentValue(photoMinutes())' in prefs)
check("the receiver checks it against this TV's memory before anything is saved",
      "PhotoInterval.refuse(photos, Prefs.ramMb(c))" in receiver
      and receiver.index("PhotoInterval.refuse(photos, Prefs.ramMb(c))") < receiver.index("if (photos != null) p.setPhotoMinutes(photos);"))
pick = st[st.index("private void pickPhotoMinutes()"):].split("\n    }\n")[0]
check("the picker offers this TV's choices only, the one in use checked, and saves the value",
      "final String[] values = PhotoInterval.choices(Prefs.ramMb(this));" in pick and "prefs.setPhotoMinutes(values[i]);" in pick
      and "conv(PhotoInterval.label(values[i]))" in pick)
check("the row says the value in words (no number parse of «30s»)", 'row("تبديل الصورة كل", PhotoInterval.label(prefs.photoMinutes())' in st
      and "Integer.parseInt(prefs.photoMinutes())" not in st)
check("the page takes 0.5 and 1 besides the minutes it knew, and looks every 5 s only below a minute",
      "[0.5,1,5,10,15,30,60].includes(m) ? m : 30" in tpl and "(CONFIG.photoMinutes < 1 ? 5 : 60)*1000" in tpl)
check("docs: SETTINGS.md and FEATURES.md say 30s and the 2 GB", all("photos 30s" in (ROOT / "docs" / d).read_text(encoding="utf-8") for d in ("SETTINGS.md", "FEATURES.md")))

tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)
for v in ("30s", "1"):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), "photos", v], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    sent = open(log, encoding="utf-8").read().strip()
    check(f"saver.sh photos {v}: one SET with the value quoted, for the TV to judge", r.returncode == 0 and sent.endswith(f"screensaver.SET --es photos '{v}'"), (r.returncode, sent))

if not shutil.which("javac"):
    print("SKIP photo interval: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(APP / "PhotoInterval.java"), str(APP / "DigitsText.java"),
                    str(ROOT / "tests/PhotoIntervalRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, "sa.urtv.screensaver.PhotoIntervalRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
