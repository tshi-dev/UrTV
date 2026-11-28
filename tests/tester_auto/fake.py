#!/usr/bin/env python3
"""A stand-in TV for tests/tester_auto_test.py: `fake.py tv CMD…`, `fake.py saver ARGS…`, `fake.py music ARGS…`. State lives in the folder FAKE_DIR (the front
app, the focused row, the adhan states); every command it was given is appended to FAKE_DIR/log.txt. FAKE_JANK=bad makes the walk janky, FAKE_FRONT=none makes
no app ever come to the front, FAKE_MUTE_ON_RADIO=1 mutes the music when a station starts, FAKE_NO_SETTINGS=1 leaves «الإعدادات» out of the
control centre (the reach step must fail), FAKE_NUDGE=1 puts «اختر المدينة» where UP lands in the header."""
import os
import re
import sys
import time

D = os.environ["FAKE_DIR"]
os.makedirs(D, exist_ok=True)
def _png():   # a 1200 x 800 grey picture
    import struct
    import zlib
    w, h = 1200, 800
    raw = b"".join(b"\x00" + b"\x40" * (w * 3) for _ in range(h))
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


PNG = _png()


def st(name, default=""):
    p = os.path.join(D, name)
    return open(p, encoding="utf-8").read() if os.path.exists(p) else default


def put(name, value):
    with open(os.path.join(D, name), "w", encoding="utf-8") as f:
        f.write(value)


def log(line):
    with open(os.path.join(D, "log.txt"), "a", encoding="utf-8") as f:
        f.write(line + "\n")


STATIONS = ["إذاعة القرآن الكريم", "محطة ثانية"]
MARKERS = ["بحث في الإعدادات", "الأذان لكل صلاة", "احذف صورة", "التخطيط", "بدء شاشة التوقف بعد", "الخروج من تلفزيونك", "مباريات اليوم", "الرقابة الأبوية", "المدينة", "تحديث تلفزيوني", "الأسرع"]


# the home as a person meets it after Home (the reach step): the apps row, a card in the couch strip, the header's «مركز التحكم»; its panel opens on a
# round toggle, «الإعدادات» is the first row under them; OK on it opens our settings
HOME_LABEL = {"apps": "يوتيوب", "card": "الأسهم", "nudge": "اختر المدينة", "center": "مركز التحكم", "pill": "تلاوة وأذان", "toggle": "البلوتوث", "settings": "الإعدادات"}


def xml():
    focused = int(st("focus", "0") or 0)
    nodes = []
    where = st("where")
    if st("front") == "sa.urtv.home" and where in HOME_LABEL:
        for k, n in HOME_LABEL.items():
            nodes.append(f'<node text="" content-desc="{n}" focused="{"true" if k == where else "false"}" bounds="[100,{100 + len(nodes) * 60}][600,{150 + len(nodes) * 60}]" />')
        nodes.append('<node text="الرياض" focused="false" bounds="[800,40][1000,100]" />')
        return '<?xml version="1.0"?><hierarchy>' + "".join(nodes) + "</hierarchy>"
    for m in MARKERS:
        nodes.append(f'<node text="{m}" focused="false" bounds="[100,100][400,160]" />')
    if st("opened") == "1":   # the tabs: «الإذاعات» is focused until the walk moved to it (LEFT, the Arabic side), then the list below
        nodes.append(f'<node text="القرّاء" focused="{"true" if st("tab", "0") == "0" else "false"}" bounds="[500,40][800,100]" />')
        nodes.append(f'<node text="الإذاعات" focused="{"true" if st("tab") == "1" else "false"}" bounds="[100,40][400,100]" />')
    for i, n in enumerate(STATIONS if st("opened") == "1" else []):
        nodes.append(f'<node text="{n}" focused="{"true" if i == focused else "false"}" bounds="[100,{200 + i * 60}][600,{250 + i * 60}]" />')
    nodes.append('<node text="الرياض" focused="false" bounds="[800,40][1000,100]" />')
    return '<?xml version="1.0"?><hierarchy>' + "".join(nodes) + "</hierarchy>"


def tv(cmd):
    log("tv " + cmd)
    if cmd.startswith("--shot"):
        return
    if cmd.startswith("wm size;") and os.environ.get("FAKE_WM_START"):
        print("Physical size: 1920x1080\nOverride size: 1600x900\nPhysical density: 320\nOverride density: 280")
        return
    for part in re.split(r";\s*", cmd):
        m = re.match(r"am start .*-n (\S+)", part)
        if m:
            put("front", "" if os.environ.get("FAKE_FRONT") == "none" else m.group(1).split("/")[0])
            put("focus", "0")
            put("where", "")
        if "KEYCODE_HOME" in part:
            put("front", "" if os.environ.get("FAKE_FRONT") == "none" else "sa.urtv.home")
            put("opened", "0")
            put("tab", "0")
            put("where", "apps")
        where = st("where") if st("front") == "sa.urtv.home" else ""
        if where in HOME_LABEL:   # the home's own focus (the reach step)
            card = st("card", "none") != "none" and st("layout", "classic") == "couch"
            nudge = os.environ.get("FAKE_NUDGE") == "1"   # a TV with no city: UP lands on «اختر المدينة», «مركز التحكم» is one LEFT from it
            handled = True
            if "KEYCODE_DPAD_UP" in part and where in ("apps", "card"):
                put("where", "card" if where == "apps" and card else "nudge" if nudge else "center")
            elif "KEYCODE_DPAD_LEFT" in part and where == "nudge":
                put("where", "center")
            elif "KEYCODE_DPAD_DOWN" in part and where == "toggle" and os.environ.get("FAKE_NO_SETTINGS") != "1":   # =1: a panel without the row (build 654)
                put("where", "settings")
            elif "KEYCODE_DPAD_RIGHT" in part and where == "center":
                put("where", "pill")   # the «تلاوة وأذان» pill of the header (the radio's way in; the screen itself is not exported)
            elif "KEYCODE_DPAD_CENTER" in part and where == "pill":
                put("opened", "1")
                put("focus", "0")
                put("where", "")
            elif "KEYCODE_DPAD_CENTER" in part and where == "center":
                put("where", "toggle")
            elif "KEYCODE_DPAD_CENTER" in part and where == "settings":
                put("front", "sa.urtv.screensaver")
                put("where", "")
            else:
                handled = False   # a key the reach step does not use (OK on the «تلاوة وأذان» pill, say) goes on to the rest
            if handled:
                continue
        if "KEYCODE_DPAD_LEFT" in part and st("opened") == "1":
            put("tab", "1")
        if "KEYCODE_DPAD_DOWN" in part and st("opened") == "1" and st("tab") == "1":
            put("tab", "2")   # DOWN from the tab lands on the first row
        elif "KEYCODE_DPAD_DOWN" in part:
            put("focus", str(min(len(STATIONS) - 1, int(st("focus", "0") or 0) + 1)))
        if "KEYCODE_DPAD_CENTER" in part and st("front") == "sa.urtv.home" and st("opened") != "1":
            put("opened", "1")   # OK on the «تلاوة وأذان» pill opens the screen (it is not exported: the round never starts it)
            put("focus", "0")
        elif "KEYCODE_DPAD_CENTER" in part and st("front") == "sa.urtv.home":
            put("playing", "1")
            if os.environ.get("FAKE_MUTE_ON_RADIO") == "1":
                put("muted", "1")
    if "dumpsys activity activities" in cmd and not cmd.startswith("dumpsys dreams"):
        f = st("front")
        print(f"  mResumedActivity: ActivityRecord{{1 u0 {f}/.MainActivity t5}}" if f else "  mResumedActivity: ActivityRecord{1 u0 com.other.app/.Main t5}")
    elif "uiautomator dump" in cmd:
        print(xml())
    elif cmd.startswith("am start -W"):
        print("Status: ok\nTotalTime: 700\nWaitTime: 720")
    elif cmd.startswith("dumpsys meminfo"):
        print("** MEMINFO in pid 123 **\n           TOTAL PSS:   150000            TOTAL RSS:   200000")
    elif "gfxinfo" in cmd and "framestats" in cmd:
        if os.environ.get("FAKE_JANK") == "bad":
            print("Total frames rendered: 400\nJanky frames: 200 (50.00%)\n90th percentile: 40ms\n99th percentile: 300ms")
        else:
            print("Total frames rendered: 400\nJanky frames: 8 (2.00%)\n90th percentile: 10ms\n99th percentile: 30ms")
    elif cmd.startswith("logcat -b crash"):
        print("")
    elif cmd.startswith("dumpsys dropbox"):
        print("Drop box contents: 0 entries")
    elif cmd.startswith("logcat -v threadtime"):
        print("11-27 16:00:00.000 123 123 I Choreographer: Skipped 3 frames!")
    elif cmd.startswith("logcat -d -s"):
        print("11-27 16:00:01.000 123 140 I MusicService: station started")
    elif cmd.startswith("dumpsys dreams"):
        print("  mCurrentDream=ComponentInfo{x/y}\n  mResumedActivity: DreamActivity")
    elif cmd.startswith("dumpsys audio"):
        print("- STREAM_MUSIC:\n   Muted: " + ("true" if st("muted") == "1" else "false") + "\n   Max: 15\n- STREAM_ALARM:\n   Muted: false")
    elif cmd.startswith("cmd media_session volume --stream 3 --get"):
        print("volume is 7 in range [0..15]")
    elif cmd.startswith("date"):
        print("2026-11-27 16:00:00")
    elif cmd.startswith("settings get system font_scale"):
        print("1.0")
    elif cmd.startswith("settings get system screen_off_timeout"):
        print(st("idle", "45000"))
    elif cmd.startswith("settings put system screen_off_timeout"):
        put("idle", cmd.split()[-1])
    elif cmd.startswith("pidof"):
        print("123")


def saver(a):
    log("saver " + " ".join(a))
    if a[:1] == ["export"]:
        print('{"city":"الرياض"}')
    elif a[:1] == ["show"]:
        print("version=4.4\ncity=الرياض\nhomelayout=" + st("layout", os.environ.get("FAKE_LAYOUT_START", "classic")) + "\nsettingsstyle=couch")
    elif a[:1] == ["homelayout"] and a[1:2] and a[1] not in ("classic", "couch", "football"):
        sys.exit(1)
    elif a[:1] == ["homelayout"] and a[1:2]:
        put("layout", a[1])
    elif a[:1] == ["home-card"] and a[1:2]:
        put("card", a[1])


def music(a):
    log("music " + " ".join(a))
    states = {k.split(":")[0]: k.split(":")[1] for k in st("adhan", os.environ.get("FAKE_ADHAN_START", "fajr:both,dhuhr:both,asr:both,maghrib:both,isha:both")).split(",")}
    if a[:1] == ["stations"]:
        for i, n in enumerate(STATIONS):
            print(f"{i}  {n}")
    elif a[:1] == ["status"]:
        print("source=radio" if st("playing") == "1" else "source=none")
    elif a[:2] == ["adhan", "state"]:
        states[a[2]] = a[3]
        put("adhan", ",".join(f"{k}:{v}" for k, v in states.items()))
    elif a[:1] == ["adhan"]:
        snd = ",".join(f"{k}:{'silent' if v in ('screen', 'none') else ''}" for k, v in states.items())
        scr = ",".join(f"{k}:{'off' if v in ('sound', 'none') else 'on'}" for k, v in states.items())
        print(f"saved|adhan_on=1|prayer_sound={snd}|prayer_screen={scr}")
    elif a[:1] == ["stop"]:
        put("playing", "0")


if __name__ == "__main__":
    what, args = sys.argv[1], sys.argv[2:]
    if what == "tv":
        if args and args[0] == "--shot":
            log("tv --shot")
            open(args[1], "wb").write(PNG)
        else:
            tv(" ".join(args))
    elif what == "saver":
        saver(args)
    elif what == "music":
        music(args)
    elif what == "install":
        log("install " + " ".join(args) + " tag=" + os.environ.get("APK_TAG", ""))
    elif what == "startsaver":
        log("startsaver")
