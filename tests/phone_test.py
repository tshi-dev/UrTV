#!/usr/bin/env python3
"""«تحكم من الجوال» (phone control): the logic on a plain JVM (tests/PhoneRun.java: one-time token and its expiry, the lockout after five tries,
session idle and maximum time, Host/Origin, private ranges, the JSON allow-list, the signer check, the PIN hook, and that with the feature off
no ServerSocket is ever created), plus what can only be read from the source: where the one ServerSocket is, who may turn it on, that nothing
advertises it, that the page has no outside file and builds no HTML from data, and that its calls are the server's endpoints.
Needs javac for the first part (skipped with a note without).   Run: python3 tests/phone_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LAUNCHER = ROOT / "android/launcher/src/main"
JAVA = LAUNCHER / "java/sa/urtv/home"
PHONE = JAVA / "phone"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


all_java = sorted((ROOT / "android").rglob("*.java"))
phone_java = sorted(PHONE.glob("*.java"))

# ---- where it listens, and who may start it
makers = [f"{f.relative_to(ROOT)}" for f in all_java if re.search(r"new\s+ServerSocket\s*\(", text(f))]
check("the only `new ServerSocket` in the apps is PhoneServer.DefaultBinder", makers == ["android/launcher/src/main/java/sa/urtv/home/phone/PhoneServer.java"], makers)
check("it is always bound to a given address and port (never the one-argument form that listens on every address)",
      re.findall(r"new\s+ServerSocket\s*\(([^)]*)\)", text(PHONE / "PhoneServer.java")) == ["port, 8, address"], "")
check("no wildcard address in the phone code", not any(re.search(r"0\.0\.0\.0|getByName\(\s*\"0|INADDR_ANY|anyLocalAddress\(\)\s*[^.]", text(f).replace("isAnyLocalAddress", "")) for f in phone_java), "")
starts = [f.name for f in all_java if re.search(r"\bturnOn\(", text(f)) and f.name not in ("PhoneHost.java",)]
check("only the owner's button can turn it on: PhoneHost.turnOn is called from PhoneActivity and nowhere else", starts == ["PhoneActivity.java"], starts)
act = text(JAVA / "PhoneActivity.java")
on_create = act.split("protected void onCreate")[1].split("protected void onResume")[0]
check("opening the screen starts nothing (no turnOn or start in onCreate)", "turnOn" not in on_create and ".start(" not in on_create, "")
click = re.search(r'button\("تشغيل".*?\}\s*\}\);', act, re.S)
check("«تشغيل» is a button the owner presses with the remote", bool(click) and "turnOn()" in click.group(0), "")
others = [f.name for f in all_java if re.search(r"PhoneHost\.|\bPhoneControl\b", text(f)) and "/phone/" not in str(f) and f.name not in ("PhoneActivity.java",)]
check("no other screen, receiver or boot code touches the phone control", others == [], others)
manifest = text(LAUNCHER / "AndroidManifest.xml")
check("no receiver, service or boot hook for the phone control in the manifest (only the installer's answer, not exported)",
      "PhoneInstallReceiver" in manifest and re.search(r'PhoneInstallReceiver"\s+android:exported="false"', manifest) is not None
      and not re.search(r"Phone(Control|Service|Boot|Server)", manifest), "")
check("the screen is in the manifest, and nothing says it should start at boot", 'android:name=".PhoneActivity"' in manifest, "")
check("nothing is advertised on the network (no mDNS / NSD / jmdns / UPnP)", not any(re.search(r"NsdManager|registerService|jmdns|MulticastSocket|SSDP", text(f)) for f in all_java if "/launcher/" in str(f)), "")
check("nothing in the phone code runs a command, a shell, SQL or deletes files",
      not any(re.search(r"Runtime\.getRuntime|ProcessBuilder|\.exec\(|rawQuery|execSQL|pm uninstall|\.uninstall|deletePackage|factory.?reset|\.delete\(\)", text(f).replace("f.delete()", "").replace("file.delete()", "")) for f in phone_java), "")
check("only the update's own downloaded files are deleted (inside its own folder)", len(re.findall(r"\.delete\(\)", text(PHONE / "PhoneUpdater.java"))) == 1, "")
check("the update asks Android's installer and never installs silently (no INSTALL_PACKAGES permission, no pm install)",
      "android.permission.INSTALL_PACKAGES" not in manifest and "pm install" not in text(PHONE / "PhoneUpdater.java") and "STATUS_PENDING_USER_ACTION" in text(PHONE / "PhoneUpdater.java")
      and "REQUEST_INSTALL_PACKAGES" in manifest, "")
upd = text(PHONE / "PhoneUpdater.java")
check("an APK is verified (package, version, signer) before it is handed to the installer", upd.index("UpdateRules.verify") < re.search(r"installNext\((c|app)\);", upd).start(), "")
check("the update's URLs are the ones optimize/install.sh uses",
      all(f in text(ROOT / "optimize/install.sh") for f in ("urtv-screensaver.apk", "urtv-home.apk")) and "releases/download/${APK_TAG:-apk-latest}" in text(ROOT / "optimize/install.sh")
      and "releases/download/\"" in text(PHONE / "UpdateRules.java") and 'BASE = RELEASES + "apk-latest/"' in text(PHONE / "UpdateRules.java"), "")
check("the PIN hook is there for the parental branch (PinGate, PhoneControl.setPinGate)", "setPinGate" in text(PHONE / "PhoneControl.java") and "interface PinGate" in text(PHONE / "PinGate.java"), "")
check("the screen the owner answers on is out of the phone's reach (PrivateScreen)", "implements Activity" not in act and "PrivateScreen" in act and "instanceof PrivateScreen" in text(PHONE / "PhoneActions.java"), "")

# ---- the same code paths as the TV's own settings
check("the city and the home cards go through SaverReceiver.set, the code optimize/saver.sh uses", "SaverReceiver.set(" in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java"), "")
check("the provider's phone calls need the signature permission", "checkCallingPermission(PHONE_PERMISSION)" in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")
      and 'sa.urtv.screensaver.PHONE_SETTINGS"\n        android:protectionLevel="signature"' in text(ROOT / "android/app/src/main/AndroidManifest.xml"), "")
check("the provider lets the phone set only the city, the home cards and the idle time", re.search(r'for \(String k : new String\[\] \{"city", "home_panel", "idle"\}\)', text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")) is not None, "")
til = JAVA / "tilawah"
check("the adhan goes through AdhanSettings, for the ADB receiver and for the phone alike", "AdhanSettings." in text(til / "MusicReceiver.java") and "AdhanSettings." in text(til / "PhoneBridge.java"), "")
voices = re.search(r"COUNT = (\d+)", text(til / "AdhanVoices.java"))
check("Schema.ADHAN_VOICES is AdhanVoices.COUNT", voices and f"ADHAN_VOICES = {voices.group(1)};" in text(PHONE / "Schema.java"), voices and voices.group(1))
check("the settings screen of the screensaver opens it like «ترتيب التطبيقات المفضلة»", "sa.urtv.home.PhoneActivity" in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java"), "")

# ---- the phone page
page = text(LAUNCHER / "assets/phone/index.html")
check("the page is Arabic, right to left and made for a phone", 'lang="ar" dir="rtl"' in page and 'name="viewport"' in page, "")
check("the page loads nothing from outside (no http(s) address, no src/href to elsewhere, no import)", not re.search(r"https?://|//cdn|@import|<link|<img|src=|url\(", page), re.findall(r"https?://\S+", page))
check("the page builds no HTML from data (no innerHTML, outerHTML, insertAdjacentHTML, document.write, eval, new Function)",
      not re.search(r"innerHTML|outerHTML|insertAdjacentHTML|document\.write|\beval\(|new Function|setTimeout\(\s*['\"]", page), "")
check("the page's one script carries the nonce the server fills in", page.count("<script") == 1 and '<script nonce="__NONCE__">' in page, "")
check("the page sends the session in X-UrTV-Session and the cookie goes with it", "'X-UrTV-Session'" in page and "credentials: 'same-origin'" in page, "")
check("the page takes the token out of the address bar at once", "history.replaceState" in page and page.index("history.replaceState") < page.index("pair({ token: h })"), "")
api = text(PHONE / "PhoneApi.java")
served = set(re.findall(r'case "(/api/[a-z_/]+)"', api)) | {"/api/state"} | ({"/api/icon/"} if 'startsWith("/api/icon/")' in api else set())
used = set(re.findall(r"['\"](/api/[a-z_/]+)['\"]", page))
check("every call of the page is an endpoint the server has, and the reverse", used == served, (used ^ served))
check("the page says plainly what it cannot do: keys to other apps, and text outside our screens",
      "فعّل خدمة «تلفزيونك: التحكم والكتابة من الجوال»" in page and "تحتاج أندرويد ١٣" in page and "يُكتب في حقلها بعد تفعيل خدمة إمكانية الوصول" in page and "/api/text" in page, "")
check("the page has no reset, uninstall, ADB or PIN-changing control", not re.search(r"reset|uninstall|adb|إعادة ضبط|حذف التطبيق", page, re.I), "")

# ---- docs and registration
runsh = text(ROOT / "tests/run.sh")
check("the tests are registered in tests/run.sh", "tests/phone_test.py" in runsh and "tests/phone_qr_test.py" in runsh, "")
features = text(ROOT / "docs/FEATURES.md")
settings = text(ROOT / "docs/SETTINGS.md")
check("FEATURES.md has «تحكم من الجوال» and says honestly what the phone page can't do", "تحكم من الجوال" in features and "لا يقدر" in features.split("تحكم من الجوال", 1)[1], "")
check("SETTINGS.md has a row for it", "تحكم من الجوال" in settings, "")

# ---- the family PIN: the phone asks the same store as the TV's keypad
activity = text(JAVA / "PhoneActivity.java")
check("PhoneActivity puts the family lock's PinStore behind the phone's PIN gate before anything can be asked",
      "setPinGate(new sa.urtv.home.phone.PinStoreGate(Parental.pin(this)" in activity)
check("the gate asks PinStore.verify (the shared lockout), never a PIN of its own", "store.verify(pin, clock.now())" in text(PHONE / "PinStoreGate.java"))

# ---- the logic itself
if not shutil.which("javac"):
    print("SKIP phone control logic: no javac here")
    sys.exit(1 if failed else 0)
pure = ["Json", "Schema", "Pairing", "Sessions", "Guard", "QrCode", "UpdateRules", "PinGate", "PinStoreGate", "Actions", "PhoneServer", "PhoneControl", "PhoneApi"]
family = [str(JAVA / "PinStore.java"), str(JAVA / "PinCrypto.java")]   # the family lock's PIN store, which the gate stands on
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(PHONE / f"{n}.java") for n in pure] + family + [str(ROOT / "tests/PhoneRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.phone.PhoneRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")

# the remote's newer parts: apps, icons, launch, adhan stop, do, family
apps_src = text(JAVA / "PhoneApps.java")
main_src = text(JAVA / "MainActivity.java")
actions_src = text(PHONE / "PhoneActions.java")
check("the apps and icons are the favourites the home shows (Dock.shown, kids mode applied); an icon is only given for one of them",
      "Dock.shown(app, all)" in apps_src and "ParentalPolicy.visible(st," in apps_src and "if (!shown) return null;" in apps_src)
check("an app that asks for the PIN is never started by the phone: the home screen is brought up with the request and asks ON THE TV",
      "locked = Parental.needsPin(app, target);" in apps_src and "if (!locked) {" in apps_src and "putExtra(EXTRA_LAUNCH, target)" in apps_src and "pin_on_tv" in apps_src)
check("the home runs the request through the very guard of a tap, for the favourites and the pills only, once",
      "phoneRequested(" in main_src and "guarded(ParentalPolicy.REMOTE_APP, t, labelOf(t), () -> openPackage(t))" in main_src and "i.removeExtra(PhoneApps.EXTRA_LAUNCH)" in main_src
      and "for (String[] a : PhoneApps.apps(this)) shown |= a[0].equals(t);" in main_src)
check("kids mode shows the phone no pills and no app outside the allowed list", "if (st.kids) return out;" in apps_src and 'Parental.state(app).kids' in apps_src)
check("the family summary is read only and never carries the PIN", "public static Map<String, Object> family(" in apps_src and "pin_set" in apps_src and "Parental.pin(" not in apps_src and "PinStore" not in apps_src)
check("the phone has no way to change the family lock, reset, uninstall or reach ADB: no such call anywhere in the phone package",
      not re.search(r"setLocked|setKids|setAllowed|adbSet|adbReset|adbLock|turnOff\(|\.uninstall\(|deletePackage|uninstallPackage|Runtime\.getRuntime|ProcessBuilder|\"adb\"|pm clear|factory", "".join(text(p) for p in PHONE.glob("*.java"))), "")
check("Back, Home and the arrows reach every app through our accessibility services when one is on (RemoteNav says which call; our own screens keep dispatchKeyEvent): see tests/remote_nav_test.py",
      'PhoneApps.pressKey(key)' in actions_src and "performGlobalAction(st.code)" in text(JAVA / "SleepService.java") and "GLOBAL_BACK = 1" in text(JAVA / "RemoteNav.java") and "a.dispatchKeyEvent" in actions_src)
check("the adhan stop and the sleep timer go through the app's own code", "MusicService.stopAdhan(c)" in text(til / "PhoneBridge.java") and "SleepTimer.start(" in apps_src and "SleepTimer.MINUTES" in apps_src)
check("the favourites are looked up at most every few seconds and cleared on a package or favourites change; an icon comes from the cache after that check and expires",
      "APPS_TTL_MS = 5_000" in apps_src and "public static synchronized void invalidate()" in apps_src and "PhoneApps.invalidate();" in main_src and main_src.count("PhoneApps.invalidate();") == 2
      and "ICON_TTL_MS" in apps_src and apps_src.index("for (String[] a : apps(app)) shown |= a[0].equals(pkg);") < apps_src.index("ICONS.get(pkg)"))
check("kids mode and the locks reach the phone at once: the cache is cleared when they change, and launch looks at kids mode again",
      "PhoneApps.invalidate();" in text(JAVA / "Parental.java") and text(JAVA / "Parental.java").count("PhoneApps.invalidate();") == 3
      and "ParentalPolicy.visible(Parental.state(app), target)" in apps_src)
check("kids mode: a pill request from the phone is ignored by the home (it shows none)", 'if (t.startsWith("@") && Parental.state(this).kids) return;' in main_src)
check("cancelling the sleep timer from the phone is a PIN-protected change", '(endpoint.equals("do") && "sleep_cancel".equals(v.get("op")))' in api and 'only(body, "op", "pin")' in text(PHONE / "Schema.java"))
check("the new settings are the app's lists", re.findall(r"MINUTES = \{([0-9, ]+)\}", text(JAVA / "SleepTimer.java"))[0].replace(" ", "") == "15,30,60,90"
      and re.findall(r"IDLE_MINUTES = \{([^}]*)\}", text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java"))[0].replace('"', "").replace(" ", "") == "1,2,5,10,15,30", "")

sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
