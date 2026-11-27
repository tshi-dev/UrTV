#!/usr/bin/env python3
"""4.4 beta 4, review F88 (security): the home calls the add-on's provider only when the app that owns the authority is «sa.urtv.extras» signed with the home's own certificate.
The authority had no permission on the home's side: any installed app could declare it, answer «available» and get the pairing code and the setup's shell commands.
Run: python3 tests/extras_trust_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)[:500]))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

# every call into the add-on's authority is ExtrasTrust.call: nowhere else asks the ContentResolver for it
offenders = []
for f in sorted(H.rglob("*.java")):
    if f.name == "ExtrasTrust.java":
        continue
    t = rd(f)
    for m in re.finditer(r"getContentResolver\(\)\s*\.call\(([^;]*)", t):
        if "EXTRAS_AUTHORITY" in m.group(1) or "ADDON" in m.group(1):
            offenders.append(f.name)
    if re.search(r'content://"\s*\+\s*ExtrasWords\.EXTRAS_AUTHORITY', t) and f.name not in ("ExtrasTrust.java",):
        offenders.append(f.name + " (the authority's address)")
check("no code but ExtrasTrust asks the add-on's authority directly (Extras and PairRoute go through ExtrasTrust.call)", not offenders, offenders)
tr = rd(H / "ExtrasTrust.java")
check("ExtrasTrust.ok: resolveContentProvider of the authority, the owner's package and its certificate against the home's own (SignDigest)",
      "resolveContentProvider(ExtrasWords.EXTRAS_AUTHORITY" in tr and "SignDigest.of(c, pkg)" in tr and "SignDigest.of(c, c.getPackageName())" in tr and "if (!ok(c)) return null;" in tr)
check("the pairing and the shell of the setup go through it (PairRoute.call, Extras.call)", "ExtrasTrust.call(c, method, arg, extras)" in rd(H / "setup/PairRoute.java") and "ExtrasTrust.call(c, method, arg, extras)" in rd(H / "Extras.java"))
if not shutil.which("javac"):
    print("SKIP the rule on a JVM: no javac")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    run = Path(tmp, "Run.java")
    run.write_text('''package sa.urtv.home;
public final class Run {
    static void c(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "FAIL")); }
    public static void main(String[] a) {
        String own = "ab12";
        c("the add-on, signed as the home is: trusted", ExtrasTrust.trusted("sa.urtv.extras", "ab12", own));
        c("another app that declares the authority: not trusted", !ExtrasTrust.trusted("com.evil.app", "ab12", own));
        c("the add-on's name with another certificate: not trusted", !ExtrasTrust.trusted("sa.urtv.extras", "ff00", own));
        c("no certificate readable (null or empty): not trusted", !ExtrasTrust.trusted("sa.urtv.extras", null, own) && !ExtrasTrust.trusted("sa.urtv.extras", "", ""));
        c("nobody owns the authority: not trusted", !ExtrasTrust.trusted(null, null, own));
        c("the home's own certificate unreadable: not trusted", !ExtrasTrust.trusted("sa.urtv.extras", "ab12", null));
    }
}
''', encoding="utf-8")
    words = H / "ExtrasWords.java"
    src = tmp + "/src"; Path(src).mkdir()
    # ExtrasTrust needs Android (Context): compile only the rule, a copy without the two Android methods
    t = tr
    cut = t.index("    /** the app that answers the add-on's authority now is the add-on")
    Path(src, "ExtrasTrust.java").write_text(t[:cut].replace("import android.content.Context;\nimport android.content.pm.ProviderInfo;\nimport android.net.Uri;\nimport android.os.Bundle;\n", "") + "}\n", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(words), src + "/ExtrasTrust.java", str(run)], capture_output=True, text=True)
    check("the rule compiles on a JVM", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.Run"], capture_output=True, text=True).stdout.splitlines()
        for line in out:
            n, res = line.rsplit("\t", 1)
            check(n, res == "ok")
        check("every check ran", len(out) == 6, len(out))
sys.exit(1 if failed else 0)
