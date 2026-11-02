#!/usr/bin/env python3
"""Community debloat lists for a TV our own lists do not know. Standard library only.

The lists in optimize/lists/community/ are packages only, taken from the CleanYourTV project's tested per-model lists
(github.com/Gathin23/cleanyourtv, MIT; .github/tools/community_lists.py makes them). Each file says, in its header, which TV it is for:

    # match-brand: tcl
    # match-model: 43S5400A
    # match-device: C06

The rule is conservative: a list is used only when the TV's maker or brand is one of the list's brands AND
  1. the TV's model equals one of the list's models (letters and digits only, any case; a model shorter than 5 characters or a
     generic one such as «Smart TV» never counts), and exactly one list says so; or else
  2. the TV's device code equals one of the list's devices (3 characters or more, not a generic word), and exactly one list says so.
Two lists that both fit (the same device code shared by several sizes) mean none is used: that is a guess, and a list for a
different model must never be applied.

    community.py match   < props           props: `ro.product.manufacturer = X` lines (optimize/run/tv.txt); prints the list's path,
                                           or nothing (and why, on stderr)
    community.py packages <list> [--keep keep.txt] [--homes file] [--installed file]
                                           the list's packages without the protected ones (keep.txt), without launchers and setup
                                           wizards (the home switch is ours; --homes: packages that answer HOME on this TV), and,
                                           with --installed (`pm list packages` output), only those on the TV
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "lists", "community")
GENERIC_MODELS = {"SMARTTV", "ANDROIDTV", "GOOGLETV", "TV", "ATV", "TVBOX", "ANDROIDTVBOX", "GENERIC", "ANDROID", "UNKNOWN",
                  "SMARTTV4K", "ANDROIDTV4K", "LEDTV", "LCDTV", "SMARTLEDTV", "TELEVISION"}
GENERIC_DEVICES = {"TV", "ATV", "GTV", "DEVICE", "GENERIC", "ANDROID", "UNKNOWN", "TVBOX", "BOX"}
# what a home switch of ours must never find in a list: launchers, setup wizards (and the first-run «oobe»), the provisioning app
NOT_FOR_US = re.compile(r"(launcher|tvhome|setupwraith|setupwizard|wizard|oobe|provision|\.home$|\.setup$)", re.I)


def norm(s):
    return re.sub(r"[^A-Z0-9]", "", (s or "").upper())


def header_values(lines, key):
    out = []
    for l in lines:
        m = re.match(rf"#\s*{key}:\s*(.*)$", l.strip())
        if m:
            out += [norm(x) for x in m.group(1).split() if norm(x)]
    return out


def load(path):
    """(meta, packages) of a community list file."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().splitlines()
    pkgs = [l.split()[0] for l in lines if l.strip() and not l.lstrip().startswith("#")]
    return {"brands": set(header_values(lines, "match-brand")), "models": set(header_values(lines, "match-model")),
            "devices": set(header_values(lines, "match-device")), "path": path}, pkgs


def choose(props, metas):
    """The one list for this TV, or (None, why)."""
    makers = {norm(props.get("ro.product.manufacturer")), norm(props.get("ro.product.brand"))} - {""}
    model, device = norm(props.get("ro.product.model")), norm(props.get("ro.product.device"))
    fits = [m for m in metas if m["brands"] & makers]
    if not fits:
        return None, "no community list is for this brand"
    by_model = [m for m in fits if model and len(model) >= 5 and model not in GENERIC_MODELS and model in m["models"]]
    if len(by_model) == 1:
        return by_model[0], "the model matches"
    if len(by_model) > 1:
        return None, "more than one list fits this model: none is used"
    by_device = [m for m in fits if device and len(device) >= 3 and device not in GENERIC_DEVICES and device in m["devices"]]
    if len(by_device) == 1:
        return by_device[0], "the device code matches"
    if len(by_device) > 1:
        return None, "the device code is shared by several lists (sizes or models): none is used"
    return None, "no community list is for this model or device code"


def all_metas(directory=DIR):
    out = []
    try:
        names = sorted(os.listdir(directory))
    except OSError:
        return out
    for n in names:
        if n.endswith(".txt") and not n.startswith(("LICENSE", "README")):
            out.append(load(os.path.join(directory, n))[0])
    return out


def read_props(text):
    return dict(re.findall(r"^(\S+) = (.*)$", text, re.M))


def read_pkgs(path):
    try:
        with open(path, encoding="utf-8") as f:
            return [re.sub(r"^package:", "", l.split()[0]) for l in f if l.strip() and not l.lstrip().startswith("#")]
    except OSError:
        return []


def packages(path, keep=(), homes=(), installed=None):
    keep, homes = set(keep), set(homes)
    out = []
    for p in load(path)[1]:
        if p in keep or p in homes or NOT_FOR_US.search(p) or p in out:
            continue
        if installed is not None and p not in installed:
            continue
        out.append(p)
    return out


def main(argv):
    if argv[:1] == ["match"]:
        meta, why = choose(read_props(sys.stdin.read()), all_metas())
        if meta:
            print(meta["path"])
        print(why, file=sys.stderr)
        return 0
    if argv[:2] and argv[0] == "packages":
        args = argv[2:]
        opt = {args[i]: args[i + 1] for i in range(0, len(args) - 1, 2) if args[i].startswith("--")}
        installed = set(read_pkgs(opt["--installed"])) if "--installed" in opt else None
        print("\n".join(packages(argv[1], read_pkgs(opt.get("--keep", "")), read_pkgs(opt.get("--homes", "")), installed)))
        return 0
    sys.exit(__doc__)


if __name__ == "__main__":
    for _s in (sys.stdout, sys.stderr):
        if hasattr(_s, "reconfigure"):
            _s.reconfigure(encoding="utf-8", errors="replace", newline="\n")
    sys.exit(main(sys.argv[1:]))
