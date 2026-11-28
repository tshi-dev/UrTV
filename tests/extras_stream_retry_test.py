"""optimize/install.sh extras: a big extra (SmartTube) is streamed with a longer timeout and once more, and one that could not be
installed says «not installed» (R-367).

The streamed install of SmartTube timed out on a weak TV (12 minutes without a word from pm while it verified the APK), and
the report said only «SmartTube no». A stub tv.py and curl; no TV needed.

    python3 tests/extras_stream_retry_test.py
"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

TVBRO, STUBE = "tvbro-1.2.3-generic-geckoExcluded.apk", "SmartTube_stable_1.0_arm64-v8a.apk"
STUB_PY = r'''#!/usr/bin/env bash
[ "$2" = -c ] && exec python3 "$@"
shift 2
[ "$1" = --bg ] && { shift 2; set -- "$*"; }
case "$1" in
  --install-stream) n=$(( $(cat "$T/streams" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/streams"
    echo "stream $(basename "$2") read=${TV_STREAM_READ_S:-default}" >> "$T/log"
    case "$2" in *tvbro*) echo com.phlox.tvwebbrowser >> "$T/installed"; echo Success; exit 0 ;; esac
    [ "$STREAM" = second ] && [ "$n" -ge 3 ] && { echo org.smarttube.stable >> "$T/installed"; echo Success; exit 0; }
    exit 3 ;;   # no answer from pm: a timeout
  --push-b64) echo "push $(basename "$2")" >> "$T/log"; exit 0 ;;
  "getprop ro.product.cpu.abilist") echo arm64-v8a ;;
  "pm list packages "*) pkg=${1##* }; grep -qx "$pkg" "$T/installed" 2>/dev/null && echo "package:$pkg" ;;
  "pm install -r /data/local/tmp/SmartTube"*) exit 1 ;;   # the file way: silent too
esac
exit 0
'''
STUB_CURL = r'''#!/bin/sh
case "$*" in
  *api.github.com/repos/truefedex/tv-bro*) printf '{"assets":[{"name":"%s","browser_download_url":"https://x/%s","size":10}]}\n200' "$TVBRO" "$TVBRO" ;;
  *api.github.com/repos/yuliskov/SmartTube*) printf '{"assets":[{"name":"%s","browser_download_url":"https://x/%s","size":20}]}\n200' "$STUBE" "$STUBE" ;;
  *) out=; while [ $# -gt 0 ]; do [ "$1" = -o ] && out=$2; shift; done; head -c 20 /dev/zero > "$out" ;;
esac
'''
def run(stream):
    t = Path(tempfile.mkdtemp()); tree = t / "tree"; bin_ = t / "bin"; bin_.mkdir()
    (tree / "optimize" / "lists").mkdir(parents=True)
    for f in ("install.sh", "home-journal.sh"):
        shutil.copy(ROOT / "optimize" / f, tree / "optimize")
    for f, body in (("py", STUB_PY), ("curl", STUB_CURL), ("sleep", "#!/bin/sh\nexit 0\n")):
        (bin_ / f).write_text(body); (bin_ / f).chmod(0o755)
    (t / "installed").write_text(""); (t / "log").write_text("")
    env = dict(os.environ, T=str(t), PATH=f"{bin_}:{os.environ['PATH']}", PY=str(bin_ / "py"), TVBRO=TVBRO, STUBE=STUBE, STREAM=stream)
    env.pop("TV_STREAM_READ_S", None)
    r = subprocess.run(["bash", str(tree / "optimize" / "install.sh"), "extras"], env=env, capture_output=True, text=True, timeout=120)
    return r.returncode, r.stdout + r.stderr, (t / "log").read_text().splitlines(), (t / "installed").read_text().split()

rc, out, log, inst = run("second")
st = [l for l in log if l.startswith(f"stream {STUBE}")]
check("SmartTube: a stream with no answer is streamed once more, and installs then (no upload)",
      rc == 0 and len(st) == 2 and "org.smarttube.stable" in inst and f"push {STUBE}" not in log, f"{rc} {log} {out}")
check("the extras' stream may stay silent 25 minutes (TV_STREAM_READ_S=1500)", st and all(l.endswith("read=1500") for l in st), log)
rc, out, log, inst = run("never")
check("never installed: «not installed» said plainly, then the !! line, and the step still ends well",
      rc == 0 and "   SmartTube (org.smarttube.stable): not installed" in out.splitlines()
      and "!! skipped after 2 timeouts: SmartTube; to add it later: optimize/install.sh extras" in out.splitlines(), out)
tv = (ROOT / "optimize" / "tv.py").read_text(encoding="utf-8")
check("tv.py: the streamed install reads TV_STREAM_READ_S", "TV_STREAM_READ_S" in tv and "stream_read_s()" in tv.split("def install_stream")[1])
run_sh = (ROOT / "optimize" / "run.sh").read_text(encoding="utf-8")
check("the report says «not installed» for a missing extra", re.search(r'Extras: .*echo "not installed"', run_sh) is not None)
sys.exit(1 if bad else 0)
