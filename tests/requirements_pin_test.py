"""4.3 S3: optimize/requirements.txt pins every package at an exact version with sha256 hashes (pip's --require-hashes mode), as
.github/tools/pin_requirements.py writes it; setup.sh installs it with --require-hashes on a computer; the UAD-ng list is read at a fixed
commit (check-packages.py and the TV's setup); check-packages.py says its exit codes and its callers tell 3 (Unsafe) from 2 (offline).
No network.

    python3 tests/requirements_pin_test.py
"""
import ast, os, re, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

req = (ROOT / "optimize/requirements.txt").read_text(encoding="utf-8")
logical = [l for l in re.sub(r"\\\n\s*", " ", req).splitlines() if l.strip() and not l.lstrip().startswith("#")]
bad = [l[:60] for l in logical if not re.match(r"^[A-Za-z0-9_.-]+==[0-9][^ ;]*( ;[^-]+)? +(--hash=sha256:[0-9a-f]{64} *)+$", l)]
check("every requirement is pinned with == and carries at least one sha256 hash", logical and not bad, bad)
names = [l.split("==")[0].lower() for l in logical]
check("the five setup.sh needs are pinned, and what they need (pyasn1, cffi, pycparser)",
      all(n in names for n in ("adb-shell", "rsa", "cryptography", "certifi", "pillow", "pyasn1", "cffi", "pycparser")), names)
check("no package twice", len(names) == len(set(names)), names)

tool = (ROOT / ".github/tools/pin_requirements.py").read_text(encoding="utf-8")
pins = next(ast.literal_eval(n.value) for n in ast.parse(tool).body if isinstance(n, ast.Assign) and getattr(n.targets[0], "id", "") == "PINS")
check("the file matches the tool's PINS (names, versions, markers)",
      [(n, v) for n, v, _ in pins] == [(l.split("==")[0], l.split("==")[1].split()[0]) for l in logical], pins)

setup = (ROOT / "optimize/setup.sh").read_text(encoding="utf-8")
check("setup.sh: --require-hashes on a computer, the names alone where the system's packages are used",
      "hashes=--require-hashes" in setup and '${hashes:+"$hashes"} -r "$reqs"' in setup and "sed -n 's/^" in setup)
# the names a phone gets: the same sed as setup.sh, on the real file
with tempfile.TemporaryDirectory() as d:
    out = subprocess.run(["sed", "-n", r"s/^\([A-Za-z][A-Za-z0-9_.-]*\)==.*/\1/p", str(ROOT / "optimize/requirements.txt")], capture_output=True, text=True).stdout.split()
check("a phone's list: the names in the file's order, no version and no hash", out == [n for n, _, _ in pins], out)

for f in ("optimize/check-packages.py", "android/launcher/src/main/java/sa/urtv/home/setup/SetupCommunity.java"):
    s = (ROOT / f).read_text(encoding="utf-8")
    check(f"{f}: UAD-ng's list at a fixed commit, not a branch", re.search(r"universal-android-debloater-next-generation/[0-9a-f]{40}/resources", s)
          and "next-generation/main/" not in s and '"next-generation/main' not in s)

cp = (ROOT / "optimize/check-packages.py").read_text(encoding="utf-8")
check("check-packages.py: exit 2 when the list cannot be read, 3 when something is Unsafe", "except (OSError, ValueError)" in cp and "sys.exit(2)" in cp and "sys.exit(3)" in cp)
# offline: no proxy that answers, a list of one package
with tempfile.TemporaryDirectory() as d:
    lst = Path(d) / "l.txt"; lst.write_text("com.example.a\n")
    env = dict(os.environ, HTTPS_PROXY="http://127.0.0.1:9", https_proxy="http://127.0.0.1:9", ALL_PROXY="", NO_PROXY="")
    r = subprocess.run([sys.executable, "-I", str(ROOT / "optimize/check-packages.py"), str(lst)], capture_output=True, text=True, env=env, timeout=120)
check("check-packages.py offline: exit 2 and a !! line, no traceback", r.returncode == 2 and "!! UAD-ng's list could not be read" in r.stdout and "Traceback" not in r.stderr, (r.returncode, r.stdout, r.stderr[-300:]))
run = (ROOT / "optimize/run.sh").read_text(encoding="utf-8"); com = (ROOT / "optimize/community.sh").read_text(encoding="utf-8")
check("run.sh and community.sh take 3 as «rated, something Unsafe», anything else as «could not rate»",
      '[ "$rc" != 0 ] && [ "$rc" != 3 ]' in run and '[ "$rc" = 0 ] || [ "$rc" = 3 ]' in com)
sys.exit(1 if failed else 0)
