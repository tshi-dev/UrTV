#!/usr/bin/env python3
"""Builds UrTV for Samsung TVs (Tizen): UrTV-tizen.wgt, the app package the TV installs. BETA: not tested on a real Samsung TV.

    python3 ports/tizen/build.py                 # -> UrTV-tizen.wgt, not signed yet
    python3 ports/tizen/build.py --sign PROFILE  # signed with a certificate profile of Samsung's Tizen tools
    python3 ports/tizen/build.py --preview       # also leaves the files in out/tizen/ to open index.html in a browser

The same page as on LG (ports/tvports/common.py), with ports/tizen/config.xml and ports/tizen/icon.png, in a zip as Tizen Studio makes it.
TVs only install signed packages. TizenBrew Installer signs it with the person's Samsung certificate on 2023 and newer TVs;
--sign uses `tizen package` (Tizen Studio) with a profile made there. Nothing signs it for you and no key is kept here.

Adapted from Free My TV's tizen/build.py (https://github.com/simbasjungles/free-my-tv, MIT; see THIRD_PARTY_NOTICES.md).
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ports"))
from tvports import common  # noqa: E402

OUT = ROOT / "out" / "tizen"
WGT = ROOT / "UrTV-tizen.wgt"


def tizen_cli():
    """Samsung's `tizen` command (Tizen Studio or the Tizen SDK), or None."""
    name = "tizen.bat" if os.name == "nt" else "tizen"
    home = os.path.expanduser("~")
    for base in (os.environ.get("TIZEN_STUDIO", ""), os.path.join(home, "tizen-studio"), os.path.join(home, "tizen-sdk"), "C:\\tizen-studio"):
        path = os.path.join(base, "tools", "ide", "bin", name)
        if base and os.path.exists(path):
            return path
    return shutil.which("tizen")


def sign(profile):
    cli = tizen_cli()
    if not cli:
        sys.exit("Signing needs Samsung's Tizen tools (Tizen Studio). Not found: set TIZEN_STUDIO to where it is installed.")
    out = tempfile.mkdtemp()
    try:
        run = subprocess.run([cli, "package", "-t", "wgt", "-s", profile, "-o", out, "--", str(WGT)], capture_output=True, text=True)
        made = [n for n in os.listdir(out) if n.endswith(".wgt")]
        if run.returncode != 0 or not made:
            sys.exit("`tizen package` didn't sign it:\n" + (run.stdout + run.stderr).strip())
        shutil.move(os.path.join(out, made[0]), WGT)
    finally:
        shutil.rmtree(out, ignore_errors=True)
    with zipfile.ZipFile(WGT) as z:
        names = z.namelist()
    if "author-signature.xml" not in names or "signature1.xml" not in names:
        sys.exit("The package came back without its signatures.")


def app_files():
    """{path in the app: bytes}: the page and its files, config.xml and the icon."""
    app = common.files()
    for name in ("config.xml", "icon.png"):
        app[name] = (ROOT / "ports" / "tizen" / name).read_bytes()
    return app


def build(preview=False, profile=None):
    app = app_files()
    manifest = app["config.xml"].decode("utf-8")
    app_id = re.search(r'<tizen:application id="([^"]+)"', manifest).group(1)
    version = re.search(r'<widget [^>]*version="([^"]+)"', manifest).group(1)
    with zipfile.ZipFile(WGT, "w", zipfile.ZIP_DEFLATED) as z:
        for path in sorted(app):
            z.writestr(path, app[path])
    if profile:
        sign(profile)
    if preview:
        common.preview(app, OUT)
        print("Preview: open out/tizen/index.html (or serve out/tizen) in a browser.")
    print("Built %s (%s %s, %d KB, %s)" % (WGT.name, app_id, version, WGT.stat().st_size // 1024,
                                           'signed with "%s"' % profile if profile else "not signed"))
    if not any(p.startswith("fonts/Thmanyah") for p in app):
        print("The Thmanyah fonts are not in assets/fonts here (they are private): the TV's own Arabic font is used.")
    return WGT


if __name__ == "__main__":
    args = sys.argv[1:]
    build("--preview" in args, args[args.index("--sign") + 1] if "--sign" in args else None)
