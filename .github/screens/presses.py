#!/usr/bin/env python3
"""Counts the remote's presses to a setting on a real (emulated) TV, in each settings style: the findability review's targets
(docs/research/SETTINGS-FINDABILITY.md §5). It moves the focus the way a person would: it reads the screen (uiautomator), presses the arrow
toward the row it wants (or down/up when the row is not on the screen yet), and OK on it; every key is counted. It never types.

    python3 .github/screens/presses.py <out dir>        (adb on the path, both apps installed; writes presses.txt and one PNG per target)

A target not reached within its key budget is written as MISSED with the last focused text, never left out.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = sys.argv[1] if len(sys.argv) > 1 else "."
SAVER = "sa.urtv.screensaver"
HOME = "sa.urtv.home"


def adb(*a, out=False):
    r = subprocess.run(["adb", *a], capture_output=True, timeout=60)
    return r.stdout.decode("utf-8", "replace") if out else None


class Walk:
    def __init__(self):
        self.n = 0

    def key(self, k):
        adb("shell", "input", "keyevent", k)
        self.n += 1
        time.sleep(0.7)

    def nodes(self):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        x = adb("exec-out", "cat", "/sdcard/ui.xml", out=True)
        try:
            root = ET.fromstring(x[x.index("<?xml"):] if "<?xml" in x else x)
        except Exception:
            return []
        out = []
        for e in root.iter("node"):
            b = re.findall(r"\d+", e.get("bounds", "0,0,0,0"))
            out.append({"text": (e.get("text") or "") + " " + (e.get("content-desc") or ""), "focused": e.get("focused") == "true",
                        "focusable": e.get("focusable") == "true", "b": [int(v) for v in b[:4]], "el": e})
        return out

    @staticmethod
    def inside(n, all_):
        """the texts of a node and of everything under it"""
        return " ".join((c.get("text") or "") + " " + (c.get("content-desc") or "") for c in n["el"].iter("node"))

    def focused(self, ns):
        f = [n for n in ns if n["focused"]]
        return f[-1] if f else None

    def goto(self, text, budget=40):
        """moves the focus onto the focusable item that holds `text`; True when it is there"""
        last = None
        same = 0
        for _ in range(budget):
            ns = self.nodes()
            f = self.focused(ns)
            if f and text in self.inside(f, ns):
                return True
            hits = [n for n in ns if text in n["text"]]
            if not f:
                self.key("KEYCODE_DPAD_DOWN")
                continue
            fx, fy = (f["b"][0] + f["b"][2]) / 2, (f["b"][1] + f["b"][3]) / 2
            if not hits:
                k = "KEYCODE_DPAD_DOWN"
            else:
                t = hits[0]["b"]
                tx, ty = (t[0] + t[2]) / 2, (t[1] + t[3]) / 2
                if t[1] <= fy <= t[3] or abs(ty - fy) < 8:   # on its line: sideways
                    k = "KEYCODE_DPAD_LEFT" if tx < fx else "KEYCODE_DPAD_RIGHT"
                else:
                    k = "KEYCODE_DPAD_UP" if ty < fy else "KEYCODE_DPAD_DOWN"
            sig = (f["b"], k)
            same = same + 1 if sig == last else 0
            last = sig
            if same >= 2:   # the focus does not move that way: try across
                k = {"KEYCODE_DPAD_UP": "KEYCODE_DPAD_LEFT", "KEYCODE_DPAD_DOWN": "KEYCODE_DPAD_RIGHT",
                     "KEYCODE_DPAD_LEFT": "KEYCODE_DPAD_DOWN", "KEYCODE_DPAD_RIGHT": "KEYCODE_DPAD_UP"}[k]
                same = 0
            self.key(k)
        return False

    def ok(self):
        self.key("KEYCODE_DPAD_CENTER")
        time.sleep(1.2)

    def where(self):
        f = self.focused(self.nodes())
        return " ".join((f and self.inside(f, []) or "").split())[:80]


def settings(style):
    adb("shell", f"am broadcast -n {SAVER}/.SaverReceiver -a {SAVER}.SET --es settingsstyle {style}")
    adb("shell", "am", "start", "-W", "--activity-clear-task", "-n", f"{SAVER}/.SettingsActivity")
    time.sleep(3)


# (name, the doc's target from the settings' first screen, the steps: a text to reach then OK)
TARGETS = [
    ("احذف صورة", 6, [("صوري والخلفيات", True), ("احذف صورة", True)]),
    ("أذاني من ملف", 6, [("الصلاة والأذان", True), ("المؤذن", True), ("أذاني من ملف", True)]),
    ("الأدعية والتذكيرات", 6, [("الصلاة والأذان", True), ("الأدعية والتذكيرات", True)]),
    ("التطبيقات المخفية", 12, [("التطبيقات والأزرار", True), ("التطبيقات المخفية", True)]),
    ("حجم البطاقات", 12, [("الشكل", True), ("حجم البطاقات", True)]),
    ("بحث في الإعدادات", 2, [("بحث في الإعدادات", True)]),   # the search's field; the word «جعفري» is typed there (no presses of the arrows)
]


def main():
    lines = ["# presses from the settings' first screen (each arrow and OK counted; typing in the search not counted)",
             "style\ttarget\tpresses\ttarget_max\tresult\tlast_focus"]
    for style in ("buttons", "couch", "tvos"):
        for name, want, steps in TARGETS:
            settings(style)
            w = Walk()
            ok = True
            for text, press in steps:
                # in «كنبة» a section is an item of the side list: the rows show at once, OK goes into them
                if not w.goto(text):
                    ok = False
                    break
                if press:
                    w.ok()
            shot = f"{OUT}/presses-{style}-{re.sub(r'[^a-z0-9]+', '-', str(TARGETS.index((name, want, steps))))}.png"
            subprocess.run(f"adb exec-out screencap -p > '{shot}'", shell=True)
            res = "OK" if ok and w.n <= want else "OVER" if ok else "MISSED"
            lines.append(f"{style}\t{name}\t{w.n}\t{want}\t{res}\t{w.where()}")
            print(lines[-1], flush=True)
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
    # from HOME: the remote's Settings key held opens «تلفزيونك», then its «الإعدادات»
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(3)
    w = Walk()
    adb("shell", "input", "keyevent", "--longpress", "KEYCODE_SETTINGS")
    w.n += 1
    time.sleep(2)
    got = w.goto("الإعدادات", budget=12)
    if got:
        w.ok()
    lines.append(f"any\tHOME → الإعدادات (Settings key held)\t{w.n}\t4\t{'OK' if got and w.n <= 4 else 'OVER' if got else 'MISSED'}\t{w.where()}")
    print(lines[-1], flush=True)
    open(f"{OUT}/presses.txt", "w", encoding="utf-8").write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
