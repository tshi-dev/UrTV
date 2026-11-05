#!/usr/bin/env python3
"""Both apps target Android 14 (API 34): Play Protect blocks an app built for an older Android when the family installs it from Downloader. Source
and manifest checks of what targetSdk 34 asks for: explicit exported flags, a type for every foreground service, exact alarms with a fallback,
foreground starts that never crash from the background, PendingIntent mutability, and the notifications permission asked once, never at launch.
(Run-time receivers and their export flags: tests/receiver_flags_test.py.)"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A = "{http://schemas.android.com/apk/res/android}"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


MODS = {"app": ("screensaver", 21), "launcher": ("home", 26)}
for mod, (pkg, min_sdk) in MODS.items():
    g = text(f"android/{mod}/build.gradle")
    check(f"{mod}: targetSdk 34", re.search(r"^\s*targetSdk 34\b", g, re.M) is not None)
    check(f"{mod}: compileSdk 34 (the target is never above what it is compiled against)", re.search(r"^\s*compileSdk 34\b", g, re.M) is not None)
    check(f"{mod}: minSdk unchanged ({min_sdk})", re.search(rf"^\s*minSdk {min_sdk}\b", g, re.M) is not None)

    root = ET.fromstring(text(f"android/{mod}/src/main/AndroidManifest.xml"))
    app = root.find("application")
    perms = {p.get(A + "name") for p in root.findall("uses-permission")}
    unflagged = [c.get(A + "name") for c in app if c.tag in ("activity", "activity-alias", "service", "receiver", "provider")
                 and c.find("intent-filter") is not None and c.get(A + "exported") is None]
    check(f"{mod}: every component with an intent filter says android:exported (required from 31)", not unflagged, unflagged)

    src = "\n".join(f.read_text(encoding="utf-8") for f in (ROOT / f"android/{mod}/src/main/java").rglob("*.java"))
    no_flag = [m.group(0)[:90] for m in re.finditer(r"PendingIntent\.get(Activity|Broadcast|Service)\([^;]*;", src, re.S)
               if not re.search(r"FLAG_IMMUTABLE|FLAG_MUTABLE|pendingFlags\(\)|\bflags\b|\bpi\)", m.group(0))]
    check(f"{mod}: every PendingIntent says its mutability (required from 31)", not no_flag, no_flag)

    services = {s.get(A + "name"): s for s in app.findall("service")}
    for java in (ROOT / f"android/{mod}/src/main/java").rglob("*.java"):
        if "startForeground(" not in java.read_text(encoding="utf-8"):
            continue
        name = "." + str(java.relative_to(ROOT / f"android/{mod}/src/main/java/sa/urtv/{pkg}"))[:-5].replace("/", ".")
        s = services.get(name)
        kind = s.get(A + "foregroundServiceType") if s is not None else None
        check(f"{mod}: {name} is a foreground service with a type in the manifest (required from 34)", bool(kind), name)
        if not kind:
            continue
        for t in kind.split("|"):
            want = "android.permission.FOREGROUND_SERVICE_" + re.sub(r"([A-Z])", r"_\1", t).upper()
            check(f"{mod}: {name}'s type {t} has its permission {want.split('.')[-1]}", want in perms and "android.permission.FOREGROUND_SERVICE" in perms)
            if t == "specialUse":
                prop = [p for p in s.findall("property") if p.get(A + "name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"]
                check(f"{mod}: {name} says what its special use is", bool(prop) and len(prop[0].get(A + "value", "")) > 20)

L = "android/launcher/src/main/java/sa/urtv/home/"
lm = text("android/launcher/src/main/AndroidManifest.xml")
check("launcher: SCHEDULE_EXACT_ALARM (Android 12) and USE_EXACT_ALARM (13+): the adhan stays exact on a new install of a 33+ target",
      "android.permission.SCHEDULE_EXACT_ALARM" in lm and "android.permission.USE_EXACT_ALARM" in lm)
check("launcher: no full-screen-intent permission was added (the update's notification stays the last, quiet try)", "USE_FULL_SCREEN_INTENT" not in lm)

# exact alarms: each place that sets one has the inexact fallback (a TV that takes the permission back)
for f in ["tilawah/AdhanScheduler.java", "ReminderScheduler.java", "UpdateCarry.java", "setup/SetupDeadman.java", "SleepTimer.java"]:
    s = text(L + f)
    check(f"{f}: an exact alarm falls back to an inexact one", "setExact" in s and ("canScheduleExactAlarms" in s or "exactAllowed" in s or "catch (SecurityException" in s)
          and re.search(r"am\.set(AndAllowWhileIdle)?\(AlarmManager\.RTC_WAKEUP", s) is not None)

setup = text(L + "setup/SetupService.java")
check("SetupService: startForeground names specialUse on Android 14+", "FOREGROUND_SERVICE_TYPE_SPECIAL_USE" in setup and "SDK_INT >= 34" in setup)
check("SetupService: a refused start still runs the job on a plain thread", "catch (RuntimeException e)" in setup and 'new Thread(r, "setup-run")' in setup)

music = text(L + "tilawah/MusicService.java")
start = music[music.index("static void start(Context c, Intent intent)"):]
start = start[:start.index("\n    }\n")]
check("MusicService.start: a foreground start refused from the background (Android 12+) is caught, tried plainly, and logged",
      "catch (IllegalStateException e)" in start and "c.startService(intent)" in start and "Log.w(" in start)
fg = music[music.index("private void foreground()"):]
fg = fg[:fg.index("\n    }\n")]
check("MusicService.foreground: a refused startForeground does not crash the player", "catch (IllegalStateException e)" in fg and "FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK" in fg)

ask = text(L + "tilawah/NotifyAsk.java")
check("NotifyAsk: POST_NOTIFICATIONS on 33+ only, asked once (kept), with one line that says why",
      "SDK_INT < 33" in ask and 'getBoolean("asked", false)' in ask and 'putBoolean("asked", true)' in ask and "S.t(" in ask and "requestPermissions(" in ask)
check("the player asks it when it opens (something was just started), and the Tilawah screen no longer asks at each open",
      "NotifyAsk.once(this)" in text(L + "tilawah/PlayerActivity.java") and "POST_NOTIFICATIONS" not in text(L + "tilawah/MainActivity.java"))
asked = [f.name for f in (ROOT / "android").rglob("*.java") if "POST_NOTIFICATIONS" in f.read_text(encoding="utf-8") and f.name != "NotifyAsk.java"]
check("nothing else asks for notifications (never at launch)", not asked, asked)
check("the line has its English", "اسمح بالإشعارات ليظهر ما يُتلى" in text("i18n/en/home.tsv"))
sys.exit(1 if failed else 0)
