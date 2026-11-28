# The hard open cases of 4.4 beta 4

## ملخّص للمالك

سبع حالات صعبة من بلاغات المستخدمين. صلّحنا خمس منها بكود واختبار، وفي اثنتين ما قدرنا نثبت السبب، فكتبنا الأوامر والأسئلة اللي تحسم الموضوع.

- **صفحة الحسابات والمساعد فيها أخطاء بعد الإعداد (TCL أندرويد 12): انحلّت (R-450).** قائمتنا الآمنة كانت توقف «إعداد شركاء قوقل» وحساب TCL، وقوائم المجتمع كانت توقف مزامنة التقويم وجهات الاتصال وتطبيقات الحسابات. صارت كلها محمية، والإعداد الجاي يرجّع اللي انوقف قبل.
- **التركيز يقفز ويفتح تطبيق ثاني: انحلّت (R-451).** كل ما تنبني المفضلة من جديد (وهذا يصير لين أربع مرات في أول دقيقتين بعد التشغيل، ومع كل تحديث تطبيق) كان التركيز يرجع لأول تطبيق، فضغطة OK تفتح غيره.
- **شاشة التوقف «لا تستجيب»: انحلّ أكبر سبب (R-452).** كانت تسأل الواجهة الرئيسية وتنتظرها وهي واقفة. **وإعادة تثبيتها تمسح إعداداتها: انحلّت (R-453).** الواجهة تحفظ نسخة منها وترجّعها.
- **الواجهة تعلّق بعد يوم تلاوة: ما لقينا تسريب ذاكرة في الكود.** خففنا أكبر حمل ثابت (R-454: كانت ترسل للنظام «وش يشتغل» كل ثانية)، وسوّينا اختبار تحمّل ساعات على المحاكي. **يحتاج قرارك:** اختبار التحمّل يشتغل يدويًا بس، وGitHub ما يعرض زر التشغيل إلا لما يكون الملف على الفرع الأساسي.
- **بطء التنقل بين التطبيقات: صلّحنا جزء (R-455)**، وصار عندنا قياس للتنقل. الباقي مكتوب تحت بالترتيب.
- **الفلاشة ما تنقرأ إلا بعد إعادة تشغيل (TCL أندرويد 11، Sony أندرويد 12): مو من قوائمنا.** ما فيه أي قائمة توقف التخزين أو USB. سدّينا ثغرة واحدة احتياطًا (R-456)، والسؤال للمبلّغ تحت.
- **الريموت يتجمّد على TCL Percee أندرويد 8 (صف 765): ما ثبت السبب.** المحاكي ما يتجمّد. كتبنا الأوامر بالضبط للمختبر أو المبلّغ.

## Scope

Base `origin/work/44-beta4` at `68a0c4a`, branch `work/44-fixes-c`. Each fix is one commit with its row in `docs/dev/REGRESSIONS.md` (R-450 to R-456),
a CHANGELOG line and a test that fails with the fix reverted (each was reverted once and seen to fail). Nothing here names a person, a TV's
address or a serial. A placeholder in angle brackets (`<PID>`) is filled on the TV.

| Case | Verdict | Commit | Test |
|---|---|---|---|
| 1 The UI froze after a day of Quran | NOT proven; the steady load is fixed; the soak is ready, not run | `ae0bcc1` R-454 | `tests/soak_test.py` |
| 2 Lag and hangs switching apps | PARTLY fixed; measured | `95da54c` R-455 | `tests/app_switch_test.py` |
| 3 A USB drive is not seen until a reboot | NOT a list; one gap closed; workaround below | `abc22e9` R-456 | `tests/usb_lists_test.py` |
| 4 The Assistant and accounts page shows errors | FIXED | `220bc43` R-450, `e91228a` | `tests/accounts_keep_test.py` |
| 5 The focus jumps and another app opens | FIXED (the main path); two smaller paths open | `39e332c` R-451 | `tests/home_focus_keep_test.py` |
| 6 The screensaver stopped answering; a reinstall reset it | FIXED (the main ANR path, and the backup) | `72df32d` R-452, `ab94126` R-453 | `tests/dream_hold_test.py`, `tests/settings_copy_test.py` |
| 7 Row 765: the remote freezes on a TCL Percee, Android 8 | NOT proven; the diagnostic commands below | (R-362 earlier) | — |

## 1. The UI froze after a day of Quran playback

**What the code shows.** The whole playback path was read: `MusicService`, `Playback`, `ExoPlayback`, `StreamUrl`, `StallWatch`, `StationSources`,
`RadioPolicy`, `PlayerActivity` and `BlackScreenActivity`. Every receiver is removed and every list is capped. No cover art is decoded per track. The
tick cannot run twice. No heap leak was found by reading.

**What was fixed (R-454).** During play the session got a new `MediaMetadata` and `PlaybackState` every second, about 86,000 calls a day into the
system, and each one wakes every controller: the TV's media notification, its home, the Assistant. The black screen also set its keep-on flag every
second, which relayouts the window. Now `SessionPush` sends the metadata when it changes, and the state when the state, the actions or the speed
change, or when the position drifts (a seek, a rebuffer). The flag is set only when it changes.

**Still suspected, not proven.**
- On every stall restart, retry and source switch a new ExoPlayer is built, with its playback thread and a `LoudnessEnhancer`. A station that keeps
  flapping (the Saudi Quran radio switches source after a stall of about 12 s) makes 2 to 5 of them every 2 minutes. `release()` waits up to 500 ms on
  the main thread, and a playback thread stuck in the audio driver is never freed.
- The R-323 «idle too long» restart is not counted in `StallWatch.restarts`, so `GIVE_UP` never limits it.

**The soak.** `.github/workflows/soak.yml` runs on manual dispatch only. It takes 2 to 5 hours of radio and a recitation in turns on the API 30
Android TV emulator. `tester/soak/soak.sh` samples the home's PSS, its threads, system_server's PSS and the ANRs every 5 minutes.
`tester/soak/judge.py` fails the job when the PSS grows by more than 6 MB/h, when the threads grow by more than 10, when the home's process changes,
or when an ANR is logged. It fails on thread growth, which is what a leak of the first suspect would show; the end of a run keeps `ps -T` of the
home for that.

**Needs the owner:** a `workflow_dispatch` workflow can be started only once its file is on the default branch. Until then the soak can be run by hand
on any Android TV emulator: `bash tester/soak/soak.sh <saver.apk> <home.apk> soak-out 4`.

**Question for the reporter (one line):** «هل كانت الإذاعة أو التلاوة؟ وأي إذاعة؟ وهل كانت «الشاشة السوداء» مفتوحة؟ ولما علّقت، هل الصوت كان شغّال؟»

## 2. Lag and hangs switching apps

**Fixed (R-455).** The next app waits for the home's `onPause`. There, since 4.4 beta 3, `HomeCard.keepStill` draws the card's WebView in software
into a new full-size picture (the fix for the black card on the way back). It now draws at half size into the picture of the last pause, which is a
quarter of the pixels and no new allocation. A debug build turns StrictMode on: the main thread's disk, network and slow calls are logged (penaltyLog
only, never in a release).

**Measured.** `tester/perf/switch.sh OUT 5` does five rounds of going out to Settings and back. Each round keeps `am start -W` of the leave (the
home's `onPause` is in its WaitTime), of the return (TotalTime) and `dumpsys gfxinfo` of the 3 s after it. `tester/perf/switch.py` prints the
medians, and judges them only when it is given limits. The soak runs it first on the emulator.

**Needs the release engineer:** the hook in the auto round (`tester/auto/round.sh` and its workflow are theirs). It goes after the jank walk:
`bash tester/perf/switch.sh "$OUT/switch" 5`, with limits only in the real TV's thresholds.

**Not fixed, in order of likely cost (each needs a measurement on a TV first):**
1. Pending `SharedPreferences.apply()` writes are flushed in `onStop`. The biggest is `nextWallpaper()`, which writes the wallpaper index to the large
   `MainActivity` prefs file (the photo list is in it) on most returns.
2. About 20 to 30 binder calls on the main thread on each return. `updateClock` runs twice, because the sticky Wi-Fi broadcast fires the tick, and
   each run calls `UpdateNotice.wanted` (3 `getPackageInfo`), `GlobalSearch.intent` (2 `resolveActivity`) and the network icon. `updateHomeNudge`
   makes another 6 `getPackageInfo` calls and reads a file (`SetupHook.stockBackOffered`).
3. Any package change (a background Play Store update) rebuilds the whole bar and grid on the return and empties the icon cache.
4. About 10 threads start at once on each return, 6 of them querying the screensaver's provider.

The UI-thread gate does not see 1 or 2: `onPause` and `onStop` are not entry points, and PackageManager is not a sink. Adding them finds 21 routes
that are already in the baseline under `onResume`. That is for the release review, not this branch.

## 3. A USB drive is not seen after the setup until a reboot (TCL Android 11, Sony Android 12)

**Checked.** No list of ours (`tcl-safe`, `tcl-optional`, `home-replaced`, `SetupLists.SAFE`), and no community list after our filters, names a
storage, MTP or USB-host package. `core.txt` already protected `com.android.mtp`, `externalstorage`, `storagemanager` and `providers.*`, and
`keep.txt` has the files apps and the media centers (4.3 B8). One gap was found and closed (R-456): Google's mainline media provider,
`com.google.android.providers.media.module`, which is how the drive's files reach the apps on these TVs, was not protected by its own name. No list
ever named it, so this is not the cause.

**Not reproduced.** The emulator has no USB host.

**Likely cause (a hypothesis):** these TVs have one USB port that also serves debugging. With USB debugging on (developer options, which the setup
needs), some boards keep that port in device mode, so a drive plugged there is not mounted until the port's role is reset. A restart with developer
options off resets it. That matches «until a reboot with developer options off».

**Workaround to tell the person (Arabic, one line):** «جرّب الفلاشة في منفذ USB الثاني؛ لو ما فيه إلا واحد، طفّ التلفزيون من الكهرباء دقيقة وشغّله.»
Do not turn developer options off for it: the agent's ADB then stops.

**Commands for the reporter or a tester** (the drive plugged in, in the port that fails):

```
adb shell sm list-disks
adb shell sm list-volumes all
adb shell dumpsys usb | head -n 80                # the ports: current mode, data role (host or device)
adb shell getprop sys.usb.config ; adb shell getprop sys.usb.state ; adb shell getprop persist.sys.usb.config
adb shell settings get global adb_enabled ; adb shell settings get global development_settings_enabled
adb logcat -c ; (unplug, plug the drive in again, wait 10 s) ; adb logcat -d -s vold StorageManagerService UsbPortManager UsbHostManager MediaProvider
adb shell pm list packages -d                     # what is off
```

**Question for the reporter:** «كم منفذ USB في التلفزيون؟ وهل الفلاشة تنقرأ في المنفذ الثاني؟ وهل تنقرأ لو شلتها ورجعتها بدون إعادة تشغيل؟»
If the drive is seen in the other port, or `dumpsys usb` shows the port with data role `device`, the hypothesis holds, and the fix belongs in the
setup's guidance, not in a list.

## 4. The Assistant and accounts page shows errors after the setup (TCL Android 12)

**Fixed (R-450, `220bc43`; `e91228a` moves the block out of keep.txt's USB section).** `tcl-safe.txt` turned off `com.google.android.partnersetup`
(Google's partner setup, which gives the Assistant and Google apps their partner ids) and `com.tcl.usercenter` (the TCL account). Community lists also
turned these off: the calendar and contacts sync adapters (152 lists name partnersetup, 122 name the calendar sync), `sss.authbridge`,
`backuptransport`, and makers' account and assistant apps.

All are on `keep.txt` now. `SetupEngine.HEAL` and `disable.sh` turn back on the ones the ledger says an earlier run turned off, so a TV already set up
is mended by the next setup or `disable.sh`. `katniss` and `tv.assistant` were already kept. `tests/accounts_keep_test.py` fails when any list names
such an app that `keep.txt` does not hold.

**Question for the reporter, after the next update and «جهّز كل شي»:** «صفحة الحسابات صارت بدون أخطاء؟ وزر المساعد في الريموت يشتغل؟» If not,
send `adb shell pm list packages -d` and `adb shell dumpsys account | head -n 60` through `share.sh`.

## 5. The focus jumps and another app opens

**Fixed (R-451).** Every build of the favourites and the grid gives each tile a new view id, and `showApps` put the focus back by the old id, which
it never found. So the focus fell to the first favourite, and the next OK opened it. The rebuild happens up to 4 times in the first 100 s after a
start of the home (`DockRetry`: the default favourites name two Netflix packages, and only one can be installed). It also happens on any package
broadcast, on a return after a package change, and on a look switch. Now the focused app is kept by its package (`FocusKeep`) and given the focus
back once its tile exists. No click carries over a rebuild: the tiles click through `View.onKeyUp`, which needs the DOWN on the same view.

**Still open (smaller):**
- For up to 2.5 s after a cold start, keys pass under the home's picture (`HomeSnapshot`), which shows the old focus while the live focus is on the
  first app. The same is true under `HomeLook`'s cover.
- «كمّل المشاهدة» puts the focus back by index, so a reordered row focuses another programme (`tv_rows_test.py` pins the index).
- `DockRetry` retries for a favourite that can never appear (the second Netflix package, or one hidden by kids mode).

## 6. The screensaver stopped answering, and a reinstall reset its preferences

**Fixed (R-452).** `onAttachedToWindow` and `onDreamingStarted` queried the home's provider on the dream's main thread (`setupWaiting`,
`setupRunning`). A home that was not running or was busy kept the dream's window up with a stuck thread for seconds (Android allows up to 10 s for
the provider to start), and a key then was an ANR. Both are asked on a thread now. The dream waits at most 1.5 s on its dark ground, and a late
«held» still ends it. The UI-thread gate now walks these two entry points, and the old code fails it.

**Fixed (R-453).** The settings lived only in the app's own data (backup is off in the manifest). The home now keeps a copy in its own files (at its
start and when it comes in front, at most every 30 minutes) and gives it back to a fresh install, once, on a thread. Both sides check the other's
signature. The copy never holds the update channel or the memory debug switch. An empty or broken answer never replaces the kept copy, and the export
says nothing until a fresh install's restore has run. If the home itself is reinstalled too, both copies are gone; the rescue app does not keep one
(a second copy there needs a new permission in the rescue: the owner's call).

**Still open (each is a cost, not a wait on another process):**
- The WebView is built on the main thread after the first frame: 0.8 to 2.7 s on a cold start (`SaverStart`). Android requires that.
- `Prefs.screenUrl` reads the page's stamp files and calls `getPackageInfo` on the main thread, three times per start: 50 to 200 ms on an old TV.
- `SaverReceiver`'s PHOTOS add copies and decodes a photo on the main thread. It is reached only from ADB.

## 7. Row 765: the remote freezes after installing 4.3.6 (TCL Percee, Android 8)

**Known.** The emulator (API 26, alone and with the add-on's service on) shows no freeze (R-362). Build 517 asks for no key filter below Android 9
(R-281). The setup had turned off `com.tcl.partnercustomizer` (now kept and turned back on), `com.tcl.initsetup` and `com.tcl.factory.view`.

**Commands for a tester or the reporter.** Run them in this order, once before the install, once after the freeze, and keep each output in its own
file. Everything here only reads, except the `pm enable` lines, and those turn on only what the ledger says our setup turned off.

```
# 1. which packages are off, and which are ours to turn back on
adb shell pm list packages -d > off.txt
adb shell pm list packages -e > on.txt
cat optimize/run/disabled-by-us.txt     # the ledger of a setup run from the computer (a setup on the TV keeps it in the home's private files)

# 2. who gets the keys: the focused window and app, and what the input dispatcher is waiting for
adb shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp|mInputMethodTarget|mObscuringWindow"
adb shell dumpsys input > input.txt          # look at: FocusedApplication, FocusedWindow, PendingEvent, «Waiting because», InboundQueue length, ANR
adb shell dumpsys activity activities | grep -E "mResumedActivity|mFocusedActivity"
adb shell dumpsys accessibility | grep -E "Bound services|Enabled services|touchExplorationEnabled|KeyEventDispatcher"

# 3. does the kernel still see the remote? press OK and the arrows while this runs (Ctrl+C after 10 s)
adb shell getevent -lt

# 4. what the system logged around the freeze
adb logcat -d -b events | grep -E "am_anr|input_focus|am_crash"
adb logcat -d | grep -iE "InputDispatcher|Input event|ANR in|KeyEvent|tclremote|keycustom" | tail -n 200
```

**How to read them.**
- `getevent` shows the key codes, but `dumpsys input` names no focused window, or a `PendingEvent` waits: the keys reach Android and no window takes
  them. Check what `mCurrentFocus` names. If it is not `sa.urtv.home` and not an app, something invisible holds the focus; send that window's name.
- `getevent` shows nothing when keys are pressed: the remote's link is gone, below Android (a pairing or an IR receiver). Re-pair the remote.
- `dumpsys input` shows the focus on `sa.urtv.home` with an inbound queue that grows: the home's main thread is stuck. Send `adb shell kill -3 <PID>`
  first (`<PID>` from `adb shell pidof sa.urtv.home`), then `adb pull /data/anr/` if the TV allows it.

**TCL packages to turn back on, one at a time,** only those that `off.txt` lists and that our setup turned off (the ledger, or `tcl-safe.txt` for a setup on the TV). After each one, press the arrows 10 times:

```
adb shell pm enable --user 0 com.tcl.partnercustomizer     # TCL's part of the remote (the setup turns it back on since R-362)
adb shell pm enable --user 0 com.tcl.initsetup             # TCL's first-run setup
adb shell pm enable --user 0 com.tcl.factory.view          # the factory menu
adb shell pm enable --user 0 com.tcl.usercenter            # the TCL account (kept since R-450)
adb shell pm enable --user 0 com.google.android.partnersetup
```

Never turn anything off to test, and never turn off the add-on's service by hand: «إضافات تلفزيونك» has its own row. The one that brings the remote
back goes to `keep.txt` with a row of its own.

**Question for the reporter (one line):** «لما يتجمد الريموت، هل زر الرئيسية يشتغل؟ وهل الريموت يشتغل في الإعدادات أو في تطبيق ثاني؟ وهل ريموت جوال «Google TV» يحرّك الشاشة؟»
If the phone remote moves the focus and the TCL remote does not, the remote's own path is the cause (step 3, then the package list). If neither
moves it, it is the focus or the home (step 2).
