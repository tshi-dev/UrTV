#!/usr/bin/env python3
"""4.3 B8 (issue #50, «the flash drive is not read» after the setup): the TVs' own USB viewers are never turned off (optimize/lists/keep.txt,
so disable.sh, community.sh and the app's setup leave them, and keep.sh / «الميزات المحمية» turn them back on), and the home's apps row always
shows them (UsbViewers, Dock.scan). The two lists are the same, and docs/USB.md says what to do.   python3 tests/usb_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

keep = ROOT / "optimize/lists/keep.txt"
k = keep.read_text(encoding="utf-8")
block = k[k.index("# USB sticks and drives"):k.index("# Accessibility and sound")]
usb_keep = {l.split()[0] for l in block.splitlines() if l.strip() and not l.startswith("#")}
uv = (ROOT / "android/launcher/src/main/java/sa/urtv/home/UsbViewers.java").read_text(encoding="utf-8")
usb_app = set(re.findall(r'"([a-zA-Z0-9_.]+\.[a-zA-Z0-9_]+)"', uv[uv.index("PACKAGES"):uv.index("private UsbViewers")]))
check("keep.txt's USB block and UsbViewers name the same viewers", usb_keep == usb_app and len(usb_app) >= 15, (usb_keep ^ usb_app))
lists = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
keep_arr = lists[lists.index("static final String[] KEEP"):]
keep_arr = keep_arr[:keep_arr.index("};")]
check("the app's setup has them in its KEEP list (generated from keep.txt): never off, turned back on by «الميزات المحمية»", all(f'"{p}"' in keep_arr for p in usb_app), [p for p in usb_app if f'"{p}"' not in keep_arr])
check("TCL's media center, Xiaomi's media explorer and MediaTek's USB player are among them", {"com.tcl.ui_mediaCenter", "com.xiaomi.mitv.mediaexplorer", "com.mediatek.wwtv.mediaplayer"} <= usb_app)
check("no USB updater or screen-share service is kept by this rule (they are not viewers)", not any(re.search(r"usbupgrade|usbota|usbcast|screenshare", p) for p in usb_app))
dock = (ROOT / "android/launcher/src/main/java/sa/urtv/home/Dock.java").read_text(encoding="utf-8")
check("the apps row shows a USB viewer even next to a Google home (unless the person hid it)",
      ': UsbViewers.is(p) ? null' in dock and dock.index('hidden.contains(p) ? "hidden with dock.sh hide') < dock.index("UsbViewers.is(p)") < dock.index('googleHome && category.equals(Intent.CATEGORY_LAUNCHER)'))
doc = (ROOT / "docs/USB.md").read_text(encoding="utf-8") if (ROOT / "docs/USB.md").exists() else ""
check("docs/USB.md: what to do when a USB stick is not read (the format, the port, the viewer app, keep.sh)", all(w in doc for w in ("FAT32", "exFAT", "optimize/keep.sh", "NTFS")), doc[:100])
sys.exit(1 if failed else 0)
