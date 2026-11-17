#!/usr/bin/env python3
"""The LG (ports/webos/) and Samsung (ports/tizen/) packages build, and what is in them is what the TVs need. No TV, no network.

    python3 tests/tvports_test.py                 # build both, check the manifests, the files, the sizes
    python3 tests/tvports_test.py --extract DIR   # also leave each package's files in DIR/webos and DIR/tizen (tests/tvports_page.test.mjs)

BETA: the packages are not tested on a real LG or Samsung TV; this checks what can be checked without one.
"""
import io
import json
import re
import struct
import subprocess
import sys
import tarfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "ports"))
from tvports import common  # noqa: E402
import importlib.util  # noqa: E402

failures = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        failures.append(name)


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def png_size(data):
    return struct.unpack(">II", data[16:24]) if data[:8] == b"\x89PNG\r\n\x1a\n" else None


def ar_members(data):
    """[(name, bytes)] of an ar archive: what an .ipk is."""
    assert data[:8] == b"!<arch>\n", "not an ar archive"
    out, i = [], 8
    while i < len(data):
        name = data[i:i + 16].decode().strip()
        size = int(data[i + 48:i + 58].decode().strip())
        out.append((name, data[i + 60:i + 60 + size]))
        i += 60 + size + (size % 2)
    return out


def tar_files(data):
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as t:
        return {m.name: t.extractfile(m).read() for m in t.getmembers() if m.isfile()}


def page_checks(label, files):
    html = files["index.html"].decode("utf-8")
    check(f"{label}: the page is the app's index.html, under {common.MAX_PAGE_BYTES // 1024} KB", 0 < len(html) < common.MAX_PAGE_BYTES, len(html))
    check(f"{label}: the page is Arabic and right to left", '<html lang="ar" dir="rtl">' in html)
    check(f"{label}: the settings data and the shell come before the page's own script",
          0 < html.find('<script src="data.js">') < html.find('<script src="shell.js">') < html.find("<script>"))
    left = re.findall(r"https://raw\.githubusercontent\.com/tshi-dev/UrTV/master/assets/\S+?(?=[\"')])", html)
    check(f"{label}: no font or bundled photo is still asked of GitHub", not left, left)
    refs = set(re.findall(r'(?:src=|url\(|u:)"?((?:fonts|bg)/[A-Za-z0-9_.-]+)', html))
    check(f"{label}: every font and photo the page names is in the package", bool(refs) and all(r in files for r in refs), sorted(r for r in refs if r not in files))
    thmanyah = [p for p in files if "Thmanyah" in p]
    check(f"{label}: the private fonts are packaged when assets/fonts has them, else the system's Arabic font is used",
          (len(thmanyah) == 4) if (ROOT / "assets/fonts/Thmanyah-Regular.woff2").is_file() else not thmanyah and "data:," in html, thmanyah)


def data_checks(label, files):
    text = files["data.js"].decode("utf-8")
    m = re.fullmatch(r"window\.URTV_TV_DATA=(.*);\n", text, re.S)
    data = json.loads(m.group(1)) if m else {}
    regions = data.get("regions", [])
    cities = [c for r in regions for c in r["cities"]]
    check(f"{label}: the settings screen has the screensaver's regions and cities", len(regions) >= 20 and len(cities) >= 300, (len(regions), len(cities)))
    check(f"{label}: Riyadh is there with its coordinates and zone",
          any(c["name"] == "الرياض" and abs(c["lat"] - 24.69) < .1 and c["tz"] == "Asia/Riyadh" for c in cities))
    check(f"{label}: other countries carry their own time zone", any(c["name"] == "القاهرة" and c["tz"] == "Africa/Cairo" for c in cities))
    check(f"{label}: every city has a name, coordinates in range and a zone",
          all(c["name"] and -90 <= c["lat"] <= 90 and -180 <= c["lon"] <= 180 and "/" in c["tz"] for c in cities))
    v, f = data.get("voices", []), data.get("fajrVoices", [])
    check(f"{label}: the adhan voices are the Android app's: 17 and 4 for Fajr", len(v) == 17 and len(f) == 4, (len(v), len(f)))
    check(f"{label}: the adhan is streamed from the public-domain collection, never bundled",
          all(x["url"].startswith("https://archive.org/download/azaan-of-world-mosques_202608/") for x in v + f))
    check(f"{label}: the cards offered are ones the page knows", {c["id"] for c in data.get("cards", [])} <= {
        "verse", "wisdom", "adhkar", "prayers", "prayerrow", "forecast", "hourly", "year", "none"} and len(data.get("cards", [])) >= 5)


def size_checks(label, files, package_bytes):
    check(f"{label}: the package is under {common.MAX_PACKAGE_BYTES // 1024 // 1024} MB", package_bytes < common.MAX_PACKAGE_BYTES, package_bytes)
    big = {p: len(d) for p, d in files.items() if len(d) > common.MAX_FILE_BYTES}
    check(f"{label}: no file is over {common.MAX_FILE_BYTES // 1024 // 1024} MB", not big, big)
    check(f"{label}: at most {common.MAX_FILES} files", len(files) <= common.MAX_FILES, len(files))


extract = sys.argv[sys.argv.index("--extract") + 1] if "--extract" in sys.argv else None

# ---- LG webOS
webos = load("ports/webos/build.py", "webos_build")
ipk = webos.build()
members = ar_members(ipk.read_bytes())
check("webOS: the .ipk is an ar archive of debian-binary, control.tar.gz, data.tar.gz",
      [n for n, _ in members] == ["debian-binary", "control.tar.gz", "data.tar.gz"], [n for n, _ in members])
control = tar_files(dict(members)["control.tar.gz"]).get("control", b"").decode()
data_tar = tar_files(dict(members)["data.tar.gz"])
info = json.loads((ROOT / "ports/webos/appinfo.json").read_text(encoding="utf-8"))
base = f"usr/palm/applications/{info['id']}/"
wfiles = {p[len(base):]: d for p, d in data_tar.items() if p.startswith(base)}
check("webOS appinfo.json: id, version, type, title, main, icons, resolution",
      re.fullmatch(r"[a-z0-9]+(\.[a-z0-9_-]+){2,}", info["id"]) and re.fullmatch(r"\d+\.\d+\.\d+", info["version"]) and info["type"] == "web"
      and info["title"] and info["main"] == "index.html" and info["icon"] == "icon.png" and info["largeIcon"] == "largeIcon.png"
      and info["resolution"] == "1920x1080", info)
check("webOS: the manifest's files are in the package", all(info[k] in wfiles for k in ("main", "icon", "largeIcon")))
check("webOS: icon 80x80, largeIcon 130x130 (what LG asks for)", png_size(wfiles["icon.png"]) == (80, 80) and png_size(wfiles["largeIcon.png"]) == (130, 130))
check("webOS: the control file names the app and its version", f"Package: {info['id']}\n" in control and f"Version: {info['version']}\n" in control, control)
pkg = json.loads(data_tar.get(f"usr/palm/packages/{info['id']}/packageinfo.json", b"{}"))
check("webOS: packageinfo.json names the app", pkg.get("id") == info["id"] and pkg.get("app") == info["id"] and pkg.get("version") == info["version"], pkg)
check("webOS: the manifest asks for no permission (it only shows a page)", "requiredPermissions" not in info)
page_checks("webOS", wfiles)
data_checks("webOS", wfiles)
size_checks("webOS", wfiles, ipk.stat().st_size)

# ---- Samsung Tizen
tizen = load("ports/tizen/build.py", "tizen_build")
wgt = tizen.build()
with zipfile.ZipFile(wgt) as z:
    check("Tizen: the .wgt is a valid zip", z.testzip() is None)
    tfiles = {n: z.read(n) for n in z.namelist() if not n.endswith("/")}
ns = {"w": "http://www.w3.org/ns/widgets", "t": "http://tizen.org/ns/widgets"}
root = ET.fromstring(tfiles["config.xml"])
app = root.find("t:application", ns)
privs = {p.get("name") for p in root.findall("t:privilege", ns)}
check("Tizen config.xml: widget id and a semantic version", root.tag == "{http://www.w3.org/ns/widgets}widget" and root.get("id", "").startswith("https://")
      and re.fullmatch(r"\d+\.\d+\.\d+", root.get("version", "")), root.attrib)
check("Tizen config.xml: application id is <10 letters or digits>.<name> and its package is those 10",
      app is not None and re.fullmatch(r"([A-Za-z0-9]{10})\.[A-Za-z0-9]+", app.get("id", "")) and app.get("package") == app.get("id").split(".")[0], app.attrib if app is not None else None)
check("Tizen config.xml: the TV profile", [p.get("name") for p in root.findall("t:profile", ns)] == ["tv-samsung"])
check("Tizen config.xml: the content and the icon are in the package",
      root.find("w:content", ns).get("src") in tfiles and root.find("w:icon", ns).get("src") in tfiles)
check("Tizen config.xml: it asks the internet and the remote's keys, and nothing else",
      privs == {"http://tizen.org/privilege/internet", "http://tizen.org/privilege/tv.inputdevice"}, privs)
check("Tizen config.xml: the Back key reaches the page (hwkey-event)", root.find("t:setting", ns).get("hwkey-event") == "enable")
check("Tizen: a name in Arabic and one in the manifest", bool(root.findtext("w:name", namespaces=ns)))
check("Tizen: icon is a PNG within 117x117", (png_size(tfiles["icon.png"]) or (999, 999))[0] <= 117)
page_checks("Tizen", tfiles)
data_checks("Tizen", tfiles)
size_checks("Tizen", tfiles, wgt.stat().st_size)
check("Tizen: not signed here (the TV only takes a signed one: ports/tizen/build.py --sign, or TizenBrew)", "author-signature.xml" not in tfiles)
check("both packages carry the same page", wfiles["index.html"] == tfiles["index.html"] and wfiles["shell.js"] == tfiles["shell.js"])

# ---- the shell runs on a browser too old for the page, to say so
shell = (ROOT / "ports/tvports/shell.js").read_text(encoding="utf-8")
code = re.sub(r"/\*.*?\*/|//[^\n]*", "", shell, flags=re.S)
code = re.sub(r"'(?:[^'\\\n]|\\.)*'|\"(?:[^\"\\\n]|\\.)*\"", "''", code)   # the strings: new Function('... => ...') is not syntax of this file
es6 = re.findall(r"=>|\blet\b|\bconst\b|`|\bclass\b|\.\.\.", code)
check("shell.js is plain ES5 (it must run where the page cannot, to say so)", not es6, es6[:5])
node = subprocess.run(["node", "--check", str(ROOT / "ports/tvports/shell.js")], capture_output=True, text=True)
check("shell.js: node --check", node.returncode == 0, node.stderr)

# ---- the key codes the shell maps
for needle, why in (("461", "LG Back"), ("10009", "Samsung Back"), ("registerKey", "Tizen registerKey"), ("tvinputdevice", "tizen.tvinputdevice")):
    check(f"shell.js maps {why}", needle in shell)

# ---- the installers: usage on a bad call, and they only take what they are given
for script in ("ports/webos/install.py", "ports/tizen/install.py"):
    r = subprocess.run([sys.executable, "-I", str(ROOT / script)], capture_output=True, text=True)
    check(f"{script}: no address -> usage, exit 1, nothing tried", r.returncode == 1 and "BETA" in (r.stderr + r.stdout), r.stdout + r.stderr)
r = subprocess.run([sys.executable, "-I", str(ROOT / "ports/webos/install.py"), "not-an-ip"], capture_output=True, text=True)
check("ports/webos/install.py: something that is not an address -> usage", r.returncode == 1)

# ---- credit and no private data
notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8") if (ROOT / "THIRD_PARTY_NOTICES.md").is_file() else ""
check("THIRD_PARTY_NOTICES.md credits Free My TV (MIT) with its licence", "Free My TV" in notices and "simbasjungles/free-my-tv" in notices and "MIT" in notices
      and "Permission is hereby granted" in notices)
for f in ("ports/webos/build.py", "ports/webos/install.py", "ports/tizen/build.py", "ports/tizen/install.py", "ports/tvports/common.py"):
    check(f"{f}: its header credits Free My TV", "free-my-tv" in (ROOT / f).read_text(encoding="utf-8")[:2500])
ip = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
mine = [ROOT / p for p in ("ports/tvports/shell.js", "ports/tvports/common.py", "ports/webos/build.py", "ports/webos/install.py", "ports/webos/appinfo.json", "ports/tizen/build.py",
                           "ports/tizen/install.py", "ports/tizen/config.xml", "docs/LG-SAMSUNG.md", "tests/tvports_test.py", "tests/tvports_page.test.mjs")]
leaks = [f"{p.name}: {m}" for p in mine if p.is_file() for m in ip.findall(p.read_text(encoding="utf-8"))]
check("no IP address in the code, the docs or the tests", not leaks, leaks)

if extract:
    for label, files in (("webos", wfiles), ("tizen", tfiles)):
        common.preview(files, Path(extract) / label)

print()
print(f"{len(failures)} failed" if failures else "all checks passed")
sys.exit(1 if failures else 0)
