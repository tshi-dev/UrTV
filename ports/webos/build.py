#!/usr/bin/env python3
"""Builds UrTV for LG webOS: UrTV-webos.ipk, the app package the TV's Developer Mode installs. BETA: not tested on a real LG TV.

    python3 ports/webos/build.py            # -> UrTV-webos.ipk
    python3 ports/webos/build.py --preview  # also leaves the files in out/webos/ to open index.html in a browser

The page is dist/tv.html with its fonts and photos (ports/tvports/common.py), ports/webos/appinfo.json and the icons, packed the way LG's
ares-package does (an ar archive with control.tar.gz and data.tar.gz), so no LG tools are needed. Python 3 only.

Adapted from Free My TV's webos/build.py (https://github.com/simbasjungles/free-my-tv, MIT; see THIRD_PARTY_NOTICES.md).
"""
import io
import json
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ports"))
from tvports import common  # noqa: E402

OUT = ROOT / "out" / "webos"
IPK = ROOT / "UrTV-webos.ipk"


def tar_gz(entries):
    """A .tar.gz from [(name, bytes or None for a directory)], owned by root, as ares-package makes them."""
    buf = io.BytesIO()
    now = int(time.time())
    with tarfile.open(fileobj=buf, mode="w:gz", format=tarfile.PAX_FORMAT) as tar:
        for name, data in entries:
            info = tarfile.TarInfo(name)
            info.mtime = now
            info.uid = info.gid = 0
            if data is None:
                info.type = tarfile.DIRTYPE
                info.mode = 0o755
                tar.addfile(info)
            else:
                info.size = len(data)
                info.mode = 0o644
                tar.addfile(info, io.BytesIO(data))
    return buf.getvalue()


def ar(members):
    out = io.BytesIO()
    out.write(b"!<arch>\n")
    now = int(time.time())
    for name, data in members:
        out.write(("%-16s%-12d%-6d%-6d%-8s%-10d`\n" % (name, now, 0, 0, "100644", len(data))).encode("ascii"))
        out.write(data)
        if len(data) % 2:
            out.write(b"\n")
    return out.getvalue()


def app_files():
    """{path in the app: bytes}: the page and its files, appinfo.json and the icons."""
    app = common.files()
    for name in ("appinfo.json", "icon.png", "largeIcon.png"):
        app[name] = (ROOT / "ports" / "webos" / name).read_bytes()
    return app


def build(preview=False):
    app = app_files()
    info = json.loads(app["appinfo.json"])
    app_id, version = info["id"], info["version"]
    base = "usr/palm/applications/" + app_id
    entries = [("usr", None), ("usr/palm", None), ("usr/palm/applications", None), (base, None)]
    dirs = set()
    for path in sorted(app):
        parent = path.rpartition("/")[0]
        if parent and parent not in dirs:
            dirs.add(parent)
            entries.append((base + "/" + parent, None))
        entries.append((base + "/" + path, app[path]))
    package = json.dumps({"id": app_id, "version": version, "app": app_id}, indent=2).encode("utf-8")
    entries += [("usr/palm/packages", None), ("usr/palm/packages/" + app_id, None),
                ("usr/palm/packages/" + app_id + "/packageinfo.json", package)]
    size = sum(len(d) for d in app.values()) + len(package)
    control = ("Package: %s\nVersion: %s\nSection: misc\nPriority: optional\nArchitecture: all\nInstalled-Size: %d\n"
               "Maintainer: UrTV <https://github.com/tshi-dev/UrTV>\nDescription: Arabic prayer-times and clock screen.\n"
               "webOS-Package-Format-Version: 2\nwebOS-Packager-Version: 1.0.0\n" % (app_id, version, size))
    data = ar([("debian-binary", b"2.0\n"), ("control.tar.gz", tar_gz([("control", control.encode("utf-8"))])),
               ("data.tar.gz", tar_gz(entries))])
    IPK.write_bytes(data)
    if preview:
        common.preview(app, OUT)
        print("Preview: open out/webos/index.html (or serve out/webos) in a browser.")
    print("Built %s (%s %s, %d KB)" % (IPK.name, app_id, version, len(data) // 1024))
    if not any(p.startswith("fonts/Thmanyah") for p in app):
        print("The Thmanyah fonts are not in assets/fonts here (they are private): the TV's own Arabic font is used.")
    return IPK


if __name__ == "__main__":
    build("--preview" in sys.argv)
