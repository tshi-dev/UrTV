#!/usr/bin/env python3
"""The release gate's «API surface diff» (docs/dev/RELEASE.md): what this build adds or drops against the previous release tag, for a
reviewer to read before anything is published:

- permissions each app asks for (uses-permission), and permissions it defines;
- providers (authority, exported, the permissions guarding them);
- exported components (an activity, service or receiver with exported="true", or an intent filter and no exported="false");
- key handlers (onKeyDown/onKeyUp/onKey/dispatchKeyEvent and every `boolean x(KeyEvent e)`);
- the work done in onResume/onStart of each screen (the calls in their bodies);
- the UI-thread paths to a cross-process call or the disk (tests/regressions/javagraph.py).

It fails (exit 1) on a NEW UI-thread path to a cross-process call (ipc:*): the 4.3.3/4.3.4 freeze. Everything else is printed for review,
and to $GITHUB_STEP_SUMMARY when it is set.

Run: python3 .github/tools/api_surface_diff.py [--base <tag>]   (default: the newest v* tag that is an ancestor of HEAD)"""
import io, os, re, subprocess, sys, tarfile, tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tests/regressions"))
import javagraph  # noqa: E402

A = "{http://schemas.android.com/apk/res/android}"


def git(*a):
    return subprocess.run(["git", *a], cwd=ROOT, check=True, capture_output=True, text=True).stdout.strip()


def base_tag():
    for t in git("tag", "--merged", "HEAD", "--sort=-v:refname", "--list", "v*").split():
        if git("rev-list", "-n1", t) != git("rev-parse", "HEAD"):
            return t
    return None


def manifest(root):
    out = set()
    for mf in sorted(Path(root).glob("android/*/src/main/AndroidManifest.xml")):
        app = mf.parts[-4]
        try:
            x = ET.parse(mf).getroot()
        except ET.ParseError as e:
            out.add(f"{app}: manifest does not parse ({e})")
            continue
        for p in x.iter("uses-permission"):
            out.add(f"{app}: uses-permission {p.get(A + 'name')}")
        for p in x.iter("permission"):
            out.add(f"{app}: defines permission {p.get(A + 'name')} ({p.get(A + 'protectionLevel') or 'normal'})")
        for c in x.iter("provider"):
            out.add(f"{app}: provider {c.get(A + 'authorities')} exported={c.get(A + 'exported')} "
                    f"read={c.get(A + 'readPermission') or c.get(A + 'permission')} write={c.get(A + 'writePermission') or c.get(A + 'permission')}")
        for kind in ("activity", "activity-alias", "service", "receiver"):
            for c in x.iter(kind):
                exp = c.get(A + "exported")
                filters = c.findall("intent-filter")
                if exp == "true" or (exp is None and filters):
                    acts = sorted({a.get(A + "name") for f in filters for a in f.iter("action")})
                    out.add(f"{app}: exported {kind} {c.get(A + 'name')} permission={c.get(A + 'permission')} actions={','.join(acts) or '-'}")
    return out


def code(root):
    keys, resume, ui = set(), set(), {}
    old = javagraph.ROOT
    for app, src in javagraph.SRC.items():
        rel = src.relative_to(old)
        base = Path(root) / rel
        if not base.exists():
            continue
        for p in sorted(base.rglob("*.java")):
            s = javagraph.blank(p.read_text(encoding="utf-8"))
            cls = (javagraph._CLASS.search(s) or [None, p.stem])[1]
            for m in re.finditer(r"\bboolean\s+(\w+)\s*\(\s*(?:final\s+)?KeyEvent\s+\w+\s*\)\s*\{|\bboolean\s+(onKeyDown|onKeyUp|onKeyLongPress|onKey|dispatchKeyEvent)\s*\(", s):
                keys.add(f"{app}: key handler {cls}.{m.group(1) or m.group(2)}")
            for m in re.finditer(r"\bvoid\s+(onResume|onStart)\s*\(\s*\)\s*\{", s):
                body = s[m.end() - 1:javagraph.match_paren(s, m.end() - 1)]
                calls = sorted({c.group(0).rstrip("( ").replace(" ", "") for c in re.finditer(r"\b(?:[A-Za-z_]\w*\s*\.\s*)?[a-z_]\w*\s*\(", body)}
                               - {"if", "for", "while", "switch", "catch", "return", "super.onResume", "super.onStart"})
                for c in calls:
                    resume.add(f"{app}: {cls}.{m.group(1)} calls {c}")
        for e, kind, route in javagraph.reach(javagraph.load((app,), root)):
            k = f"{kind} | {app}:{e.key} | {route.split(' -> ')[-1]}"
            if k not in ui or len(route) < len(ui[k]):
                ui[k] = route
    return keys, resume, ui


def tree_at(tag, into):
    data = subprocess.run(["git", "archive", "--format=tar", tag, "android"], cwd=ROOT, check=True, capture_output=True).stdout
    with tarfile.open(fileobj=io.BytesIO(data)) as t:
        t.extractall(into, filter="data") if hasattr(tarfile, "data_filter") else t.extractall(into)


def main():
    args = sys.argv[1:]
    tag = args[args.index("--base") + 1] if "--base" in args else base_tag()
    lines = []
    def say(s=""):
        print(s)
        lines.append(s)
    if not tag:
        say("No earlier v* tag: nothing to compare.")
        return 0
    with tempfile.TemporaryDirectory() as tmp:
        tree_at(tag, tmp)
        m0, (k0, r0, u0) = manifest(tmp), code(tmp)
    m1, (k1, r1, u1) = manifest(ROOT), code(ROOT)
    say(f"## API surface: {git('rev-parse', '--short', 'HEAD')} against {tag}")
    new_ipc = []
    for title, a, b in (("Manifest: permissions, providers, exported components", m0, m1), ("Key handlers", k0, k1),
                        ("Work in onResume / onStart", r0, r1), ("UI-thread paths to a cross-process call or the disk", set(u0), set(u1))):
        add, gone = sorted(b - a), sorted(a - b)
        say(f"\n### {title}: {len(add)} new, {len(gone)} gone")
        for x in add:
            say(f"+ {x}" + (f"\n    route: {u1[x]}" if x in u1 else ""))
            if x in u1 and x.startswith("ipc:"):
                kind, entry, _ = [y.strip() for y in x.split("|")]
                # the same call one method deeper (a read split into read -> ask): the old sink is on the new route
                moved = [o for o in u0 if o.startswith(f"{kind} | {entry} |") and f"{o.split('|')[2].strip()} ->" in u1[x] + " ->"]
                if moved:
                    say(f"    (the same path as before, one call deeper: {moved[0]})")
                else:
                    new_ipc.append(x)
        for x in gone:
            say(f"- {x}")
    say("")
    if new_ipc:
        say(f"FAIL {len(new_ipc)} new UI-thread path(s) to a cross-process call since {tag}: move the work off the UI thread "
            "(tests/regressions/ui_thread_ipc_test.py, docs/dev/RELEASE.md)")
    else:
        say(f"PASS no new UI-thread path to a cross-process call since {tag}")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    return 1 if new_ipc else 0


if __name__ == "__main__":
    sys.exit(main())
