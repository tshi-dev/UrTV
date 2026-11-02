#!/usr/bin/env python3
"""«الرقابة الأبوية» in the launcher's source (the wiring needs Android, so it is read from the code):
 1. every way the home screen opens something for a person asks the family lock first (tiles, rows, pills, «المصادر», the TV's settings, the menu);
 2. with nothing set the launcher behaves as before: no PIN asks nothing, runs the action at once, and every app is shown;
 3. the adhan is never blocked or delayed: a key that stops it works over the PIN keypad, the PIN check runs off the main thread, and
    nothing here touches the adhan's service or alarms;
 4. no accessibility service, and the PIN is kept only as the salted hash.
Run: python3 tests/parental_wiring_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
MAN = ROOT / "android/launcher/src/main/AndroidManifest.xml"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def src(n): return (HOME / n).read_text(encoding="utf-8")
main = src("MainActivity.java").split("\n")
parental = src("Parental.java")
prompt = src("PinPrompt.java")
policy = src("ParentalPolicy.java")

def enclosing(lines, i):
    for j in range(i, -1, -1):
        m = re.match(r"    (?:private |public |static |final )*[\w<>\[\]., ]+ (\w+)\([^;]*\{\s*$", lines[j])
        if m: return m.group(1)
    return ""
def guarded_near(lines, i, back=3):
    return any("guarded(" in lines[k] or "launchGuarded(" in lines[k] for k in range(max(0, i - back), i + 1))

# ---- 1. every way in asks
sites = {"launch": {"launchGuarded", "openPackage", "startSaver", "openPassthroughInput", "openUpdates"},   # the helpers a guarded call reaches (openUpdates: the bell, which calls it only from guarded(...))
         "openPackage": {"startSaver", "openPassthroughInput"}, "startSaver": set(), "openPassthroughInput": {"showInputsNow"},
         "showSleep": set(), "openUpdates": set(), "showInputsNow": set(), "showAppMenuNow": set()}
for fn, inner in sites.items():
    bad = []
    for i, l in enumerate(main):
        if re.search(rf"\b{fn}\(", l) and not re.match(rf"\s+private \w+ {fn}\(", l) and not l.strip().startswith(("*", "//", "/**")):
            if guarded_near(main, i) or enclosing(main, i) in inner:
                continue
            bad.append(f"{i + 1}: {l.strip()[:90]}")
    check(f"MainActivity: every {fn}(…) is behind the family lock", not bad, bad)
starts = [i for i, l in enumerate(main) if "startActivity(" in l]
check("MainActivity: startActivity only in launch(): nothing opens without going through it",
      all(enclosing(main, i) == "launch" for i in starts), [main[i].strip() for i in starts])
check("the long OK on «يُتلى الآن» opens the player behind the «تلاوة وأذان» lock (not around it)",
      any("nowCard.setOnLongClickListener" in l and "launchGuarded(ParentalPolicy.PILL_LISTEN, NowPlaying.player(this)" in l for l in main))
check("the home screen's key handling asks the menu key's «المصادر» through the lock too", "showInputs(pills.get(0), ParentalPolicy.MENU_KEY)" in "\n".join(main))
paths = re.search(r"static final int (TILE = 0.*?);", policy, re.S).group(1)
names = re.findall(r"([A-Z_]+) = ", paths) 
used = "".join(src(f.name) for f in HOME.glob("*.java") if f.name not in ("ParentalPolicy.java",))
check("every launch path ParentalPolicy knows is used by a screen", all(f"ParentalPolicy.{n}" in used for n in names), [n for n in names if f"ParentalPolicy.{n}" not in used])
check("the lock list the settings screen shows is the pills ParentalPolicy knows", "for (String pill : ParentalPolicy.PILLS)" in src("ParentalActivity.java"))
check("«قفل الإعدادات»: the arrange screen asks for itself too (it is opened from the screensaver's settings as well)", "ARRANGE_SCREEN" in src("DockActivity.java"))
check("«تحديث تلفزيوني»: no longer in the app's options (it is in the settings); its own screen asks for the family lock, and so does its install button (the phone's install asks in PhoneApi)",
      "MENU_UPDATE" not in src("MainActivity.java")
      and "Parental.guard(UpdateActivity.this" in src("UpdateActivity.java") and "ParentalPolicy.UPDATE_SCREEN" in src("UpdateActivity.java"))
check("the settings screen asks for the PIN itself, before anything shows", "screen.setVisibility(View.INVISIBLE)" in src("ParentalActivity.java") and "PARENTAL_SCREEN" in src("ParentalActivity.java"))
check("what was opened with the PIN is forgotten when the home screen comes back (onResume and onNewIntent)",
      "".join(main).count("Parental.SESSION.clear()") == 2)
check("the manifest lists the settings screen", 'android:name=".ParentalActivity"' in MAN.read_text(encoding="utf-8"))

# ---- 2. nothing set = as before
guard = parental.split("static void guard(")[1].split("\n    }\n")[0]
check("guard: with no lock it runs the action at once, before any keypad", guard.index("allowed.run()") < guard.index("ask("), guard)
check("a PIN not set means needsPin is false before any list is read", re.search(r"if \(!s\.pinSet\) return false;", policy) is not None)
check("kids mode counts only with a PIN, and shows every app when off",
      "pinSet && kidsKept" in policy and "return !s.kids || s.allowed.contains(pkg);" in policy)
load = "\n".join(main).split("private List<App> loadApps(")[1].split("\n    }\n")[0]
check("the kids-mode filter is in the one place the home screen's apps are loaded, before their art is", "ParentalPolicy.visible(family, ai.packageName)" in load
      and load.index("ParentalPolicy.visible") < load.index("appOf(") and "loadBanner" in "\n".join(main).split("private App appOf(")[1].split("\n    }\n")[0])
check("nothing is kept in the launcher's own preferences (a separate file, absent until used)", 'getSharedPreferences("Parental"' in parental)
check("the default for every setting is off", 'getBoolean("kids", false)' in parental and 'getString("locked", "")' in parental and 'getString("allowed", "")' in parental)
check("no key of the remote is taken by the home screen for this lock", "KEYCODE" not in parental + policy)

# ---- 3. the adhan
keys = prompt.split("setOnKeyListener(")[1]
check("the PIN keypad (a dialog) asks the adhan's rule before it reads any digit",
      "before.take(event)" in keys and keys.index("before.take(event)") < keys.index("KEYCODE_0"))
check("a key that stops the adhan is consumed whole there (never also a digit)", "if (before != null && before.take(event)) return true;" in keys)
check("the home screen hands its own AdhanKeys to the keypad", "Parental.guard(this, this::adhanTakes," in "\n".join(main))
dispatch = "\n".join(main).split("public boolean dispatchKeyEvent(KeyEvent e) {")[1]
check("the home screen still asks the adhan first of everything", dispatch.lstrip().startswith("// while the adhan plays") and "if (adhanTakes(e)) return true;" in dispatch[:400])
check("adhanTakes is the adhan's old rule, unchanged: AdhanKeys.decide, then stopAdhan",
      "adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), adhan, adhanBar.getVisibility() == View.VISIBLE," in "\n".join(main)
      and "if (stop == AdhanKeys.STOP) NowPlaying.stopAdhan(this);" in "\n".join(main))
for n in ("ParentalActivity.java", "DockActivity.java"):
    check(f"{n}: its PIN keypad has the adhan's rule too", "Parental.adhanFirst(this)" in src(n) or "adhan = Parental.adhanFirst(this)" in src(n))
ask = parental.split("private static void ask(")[1]
check("the PIN is checked on a worker thread, never on the main one", ask.index("WORK.execute(") < ask.index(".verify("))
check("the PIN is created on the worker thread too", parental.split("static void choose(")[1].index("WORK.execute(") < parental.split("static void choose(")[1].index(".set(first)"))
check("a wrong PIN or a wait never closes or pauses anything but the keypad: no sleep, no wait, no blocking call",
      not re.search(r"Thread\.sleep|\.wait\(|\.join\(\)|Future|CountDownLatch|\.invokeAll", parental + prompt + policy + src("PinStore.java")))
touch = [n for n in ("Parental.java", "PinPrompt.java", "PinUi.java", "PinStore.java", "PinCrypto.java", "ParentalPolicy.java", "ParentalActivity.java")
         if re.search(r"MusicService|AdhanScheduler|AdhanReceiver|AlarmManager|AdhanActivity|AdhanSync|Prefs\(", src(n))]
check("nothing of the family lock touches the adhan's service, alarms or settings", not touch, touch)
check("the only adhan calls are the ones the home screen already makes (live? stop-key setting? stop it)",
      sorted(set(re.findall(r"NowPlaying\.(\w+)", parental))) == ["adhanLive", "adhanStopKey", "stopAdhan"], sorted(set(re.findall(r"NowPlaying\.(\w+)", parental))))
check("the lock never decides about tilawah's pages: the adhan's own screen (AdhanActivity) and its alarm are not targets",
      "AdhanActivity" not in policy + parental and "tilawah" not in policy)

# ---- 4. no accessibility service; the PIN is only a hash
manifest_text = MAN.read_text(encoding="utf-8")
check("only two accessibility services, both owner-enabled and named: the sleep one (reads nothing) and the phone remote's typing one",
      manifest_text.count("android.accessibilityservice.AccessibilityService") == 2 and '".SleepService"' in manifest_text and '".TypeService"' in manifest_text)
check("no file of the family lock reads another app's screen or windows", not re.search(r"AccessibilityService|AccessibilityEvent|getRootInActiveWindow|UsageStats|getRunningTasks|FLAG_SECURE",
      "".join(src(n) for n in ("Parental.java", "PinPrompt.java", "PinUi.java", "ParentalActivity.java", "ParentalPolicy.java"))))
check("the PIN is only ever passed to PinCrypto (create / verify)", not re.search(r'putString\([^)]*(code|first|second|pin)\b', parental))
check("the PIN never goes to the log, a toast or the result of a broadcast",
      not re.search(r"Log\.|Toast\.makeText\([^;]*(code|first|pin)\b|setResultData\([^;]*\b(code|first)\b", parental + prompt + src("DockReceiver.java")))

print("\n" + (f"{failed} failed" if failed else "parental wiring: all passed"))
raise SystemExit(1 if failed else 0)
