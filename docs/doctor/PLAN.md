# «فحص تلفزيونك» (UrTV Doctor): the plan (phase 1, nothing built)

**What it is.** A screen on the TV that checks a TV running UrTV **on the TV itself**, with no computer, and ends in one clear report:
a list of rows, each one «تمام», «انتبه» or «فيه مشكلة», in plain Gulf Arabic, and «أرسل التقرير». The tester can run the same checks over
adb on a TV at hand. It **only reads and measures**: it changes no setting, turns nothing on or off, installs nothing, and never asks for a
new permission at run time. Where a check cannot read something on a given TV, the row says «ما قدرنا نفحص هذا» (grey) and never guesses.

**Why.** Every check below answers a problem from users' reports: the remote freezing (TCL, Android 8–10, the key filter path, 4.3.6 review),
the home not sticking after a restart, the setup wizard owning Home (TVS.md), protected packages left off, the assistant or accounts broken
after setup, app keys doing nothing (TVS.md «The remote's shortcut keys»), the screensaver delay put back by the TV, an old WebView (R-381),
low RAM/CPU, USB not seen, slow app switching, radio stalls, a wrong TV clock.

Owner decisions are in the last section. Nothing is built before the owner approves.

---

## 1. Where it lives (recommendation: inside the home, in a process of its own)

| | A screen in the home (`sa.urtv.home`), **recommended** | A separate APK (`sa.urtv.doctor`) |
|---|---|---|
| Reach | on every UrTV TV already, after the next home update; nothing to install | must be installed: Downloader or `install.sh`, the add-on's offer; the people who most need it are the ones who can't install |
| Update path | rides the home's own update (PhoneUpdater, auto-update, the three channels) | a fifth release file, a new `install.sh` target, a new `UpdateRules` entry; one more app that can end up «signed with another key» |
| HOME check | `RoleManager.isRoleHeld(ROLE_HOME)` answers **only for the app asking** (Extras.java already calls it this way): only the home can ask it about itself | can read the default home with `resolveActivity`, never the role itself (`getRoleHolders` needs a system permission) |
| Our crashes | `ActivityManager.getHistoricalProcessExitReasons` gives another package's exits only with `DUMP` (not for apps): only the home can read its own crashes and ANRs | cannot read the home's |
| Our own records | the home's private files (`crash-loop.txt`, the setup's `steps.log`, `home-fails.txt`, `boot-guard.txt`, the ledgers, `LiteAuto`, the radio's `radio_week`) are readable in-process | would need a new provider for each |
| Permissions | none new: the home already holds INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, and its `<queries>` already cover HOME, LAUNCHER, LEANBACK_LAUNCHER, RECOGNIZE_SPEECH | the same set to declare again |
| Size | a few Java classes and one layout, no library (target: under 100 KB added to the APK) | a whole APK |
| When the home itself won't start | covered by running it in its own process (`android:process=":doctor"`), opened from the screensaver's settings or from «إنقاذ تلفزيونك» (`sa.urtv.rescue`); a home that crashes at start does not take it down | starts by itself |

**Recommendation: the home.** Reach and update path decide it; the role and crash checks need it. The one weakness (a home that crashes)
is handled by the separate process and the two outside entries.

**How it is opened**
- «الإعدادات ← حول والتحديث ← فحص التلفزيون»: a new row in the screensaver app's settings (`SettingsHub.UPDATE`, after «أرسل تقرير»), which
  starts `sa.urtv.home/.doctor.DoctorActivity` through a signature permission `sa.urtv.home.OPEN_DOCTOR` (the same pattern as `OPEN_UPDATE`).
  Adding the row follows the four steps of the settings tree (`SettingsHub.ROWS`/`ORDER`, `settings_tree.py`, SETTINGS.md, `i18n/en`).
- «إنقاذ تلفزيونك» gets a button with the same intent (it already holds the `EXTRAS` signature permission; it would get `OPEN_DOCTOR`).
- **The tester, over adb:** an `activity-alias` guarded by `android.permission.DUMP`, which the shell holds and no app does:
  `adb shell am start -n sa.urtv.home/.doctor.DoctorAdb --ez auto true` runs every automatic check (no key test) and writes the report's
  lines to logcat under the tag `UrTVDoctor`, the same lines as the QR. Phase 2 adds `optimize/dock.sh doctor` that starts it through
  `tv.py` and prints those lines (and `--keys` to run the key test with the person's real remote, since adb's own key presses skip the
  paths we are testing).

---

## 2. The checks

Each check has an id (used in the report line), what it reads, the API, the permission, and its rule. «Normal» means a permission granted
at install that the home already holds. **No dangerous permission is used.** No check writes anything except the doctor's own result in
the home's private files.

### 2.1 The TV

**D1 The TV and its Android**
- Reads: `Build.MANUFACTURER`, `Build.MODEL`, `Build.DEVICE`, `Build.VERSION.RELEASE`, `Build.VERSION.SDK_INT`, `Build.ID` (the firmware
  build), `Build.VERSION.SECURITY_PATCH`; `PackageManager.hasSystemFeature(FEATURE_LEANBACK)`; Google TV = `com.google.android.apps.tv.launcherx`
  installed (`getApplicationInfo`, it is in our HOME `<queries>`).
- Permission: none. `Build.getSerial()` is **never** called.
- Rule: always «تمام» (information). The name and Android go at the top of the report as share.py's `TV:` line.

**D2 WebView version**
- Reads: `WebView.getCurrentWebViewPackage()` (API 26+): package and `versionName`; the major number.
- Permission: none.
- Rule: major ≥ 80 (`SetupEngine.MIN_WEBVIEW`) «تمام»; 73–79 «انتبه» (pages work, the CSS of the look does not: clamp, gap); under 73 or no
  WebView «فيه مشكلة» (the page was blank on 66–72 before the polyfill, R-381; the screensaver is not set up below 80).
- Words on FAIL: «متصفح التلفزيون (WebView) قديم، وشاشة التوقف ما تشتغل عليه. حدّثه من المتجر إذا موجود.»

**D3 WebView JavaScript features**
- Reads: a hidden `WebView` (not attached to the screen) loads a 1 KB local page from the APK's assets and `evaluateJavascript` returns one
  word per feature: `Object.fromEntries`, `Array.prototype.flat`, `String.prototype.matchAll`, `replaceAll`, `Promise.allSettled`, optional
  chaining and `??` (each inside `new Function(...)` in a try, so a syntax error is caught), `CSS.supports('gap','1px')` in flex, `clamp()`.
  These are exactly the features `tests/check.py` forbids or polyfills, so the list stays in one place: phase 2 generates the probe from
  check.py's list and a test fails when they differ.
- Permission: none (no network, JavaScript on for that WebView only).
- Rule: every feature our pages use without a polyfill is there «تمام»; a polyfilled one missing (fromEntries) «تمام» with a note; any
  other missing «فيه مشكلة»; the WebView fails to start (some TVs throw at `new WebView`) «فيه مشكلة» with the exception class only.

**D4 Memory, processor and the light mode**
- Reads: `ActivityManager.getMemoryInfo` (`totalMem`, `availMem`, `lowMemory`), `isLowRamDevice()`, `getMemoryClass()`,
  `Runtime.availableProcessors()`, `Build.SUPPORTED_64_BIT_ABIS.length`; the home's own `Ram.tier()` and the `lite` setting (`auto|on|off`) with
  `LiteAuto`'s state (the same `lite=…` word as the report's `Home:` line). The CPU's top speed from
  `/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq` **only if readable** (SELinux blocks it on some TVs; then left out).
- Permission: none.
- Rule: tier HIGH «تمام». Tier LOW or MID with light mode on (or auto and on) «تمام» with «الوضع الخفيف شغّال». Tier LOW with light mode
  **off** by hand «انتبه» («التلفزيون ذاكرته قليلة، والأفضل تشغّل الوضع الخفيف»). Free memory under 10% of total at the time of the check,
  or `lowMemory` true, «انتبه». Never FAIL: a small TV is not broken.

**D5 Storage**
- Reads: `StatFs(Environment.getDataDirectory())` free and total bytes.
- Permission: none.
- Rule: free ≥ 1 GB «تمام»; 300 MB–1 GB «انتبه» (updates and the adhan files need room); under 300 MB «فيه مشكلة» («المساحة شبه خلصت،
  والتحديث ممكن ما ينزل»).

**D6 USB drives**
- Reads: `StorageManager.getStorageVolumes()` (API 24+): for each removable volume `isRemovable()`, `getState()` (mounted, unmounted,
  unmountable, checking…); on API 30+ also whether `getDirectory()` is readable by us (`File.canRead`). **Not** read: the volume's label
  (`getDescription` may carry a person's name), its UUID, any file name.
- Permission: none for the list. Reading files needs READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE (dangerous), which the home declares for
  «الصوت من USB» (docs/USB.md); the doctor **does not ask** for it, it only says whether it is already granted
  (`checkSelfPermission`).
- Rule: this check runs only after a question on its screen, «فيه فلاشة موصّلة الحين؟» (نعم / لا). No: «تمام» (skipped). Yes and a
  removable volume mounted «تمام»; yes and a volume present but unmounted or unmountable «فيه مشكلة» («التلفزيون شاف الفلاشة بس ما
  قدر يفتحها: غالبًا نوعها NTFS أو exFAT. جرّب فلاشة FAT32»; the format itself is not readable, so this is said as likely); yes and no
  removable volume at all «فيه مشكلة» («التلفزيون ما شاف الفلاشة. جرّب منفذ ثاني»). Mounted but the permission not granted «انتبه» with
  where to grant it.

### 2.2 The home and the setup

**H1 Our home is the TV's Home**
- Reads: `resolveActivity(ACTION_MAIN + CATEGORY_HOME, MATCH_DEFAULT_ONLY)` → the package that answers Home (or the chooser, package
  `android`); on Android 10+ `RoleManager.isRoleHeld(ROLE_HOME)` from the home itself; `queryIntentActivities(HOME)` → the other homes
  present and enabled (package names only: launcherx, setupwraith, a TV's own; they are system apps, not personal).
- Permission: none (the HOME `<queries>` entry is already in the manifest).
- Rule: both say ours «تمام». The chooser (no default) «انتبه». Another package answers Home «فيه مشكلة», and the words say which kind:
  the setup wizard (`com.google.android.tungsten.setupwraith`) → «مساعد إعداد
  التلفزيون ماسك زر الرئيسية» with the same Arabic line `install.sh launcher` sends (the person forgets and rejoins the Wi-Fi in the TV's
  own settings: **the doctor never touches the Wi-Fi or the wizard**); the stock home → «الرئيسية الأصلية رجعت»; with «إضافات تلفزيونك»
  installed, its Home key catch state too (H4).
- Also: `Settings.Global.DEVICE_PROVISIONED` (public) = 0 «فيه مشكلة» (the TV thinks its own setup is not finished: the wizard then takes
  Home at boot). `Settings.Secure` `user_setup_complete` is read only if readable (on Android 12+ a hidden key can throw
  `SecurityException` for apps targeting 31+; then left out, not FAIL).

**H2 Home after a restart**
- Reads: the home's own records: `crash-loop.txt` (`CrashLoopStore`: last start, quick crashes, opened/drawn), the setup's `home-fails.txt`
  and `boot-guard.txt` (counts and dates only), and **one new small record** written at each `BOOT_COMPLETED` by the boot receiver the
  home already has (`SetupBootReceiver`): seconds since boot when the home was first drawn and whether it came through the HOME intent
  (`boot-home.txt`, the last 5 boots, numbers only). `SystemClock.elapsedRealtime()`.
- Permission: none new (RECEIVE_BOOT_COMPLETED is already held).
- Rule: the last 5 boots all reached our home by HOME «تمام»; one of the last 5 did not «انتبه»; the last one did not, or no record after
  a restart since the doctor came «فيه مشكلة» («بعد ما طفّيت وشغّلت، الرئيسية ما كانت تلفزيونك»). Before any restart is recorded:
  «ما قدرنا نفحص هذا: طفّ التلفزيون وشغّله ثم افحص مرة ثانية».

**H3 Our three apps: versions and match**
- Reads: `getPackageInfo` for `sa.urtv.home`, `sa.urtv.screensaver`, `sa.urtv.extras` (and `sa.urtv.rescue` if present): `versionName`,
  `versionCode` (`getLongVersionCode` on 28+), whether each is enabled (`ApplicationInfo.enabled`); the channel (`UpdateChannel`). The match
  rule is the one already in the code: `VersionMismatchRule.which` (home vs screensaver) and `UpdateNoticeRule.behind` (any app below the
  newest build); the newest build is read from `version.txt` only if the last read of `UpdateNotice` is older than 6 hours (no extra fetch
  otherwise).
- Permission: none.
- Rule: same build everywhere and none behind «تمام»; one behind the release «انتبه» («فيه تحديث: الإعدادات ← حول والتحديث ← تحديث
  تلفزيوني»); home and screensaver of different builds by `VersionMismatchRule` «فيه مشكلة»; the screensaver missing or disabled «فيه
  مشكلة». The add-on missing is «تمام» (it is optional).

**H4 The accessibility services**
- Reads: `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` and `ACCESSIBILITY_ENABLED` (public keys; `A11yHeal` already reads them);
  `AccessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` → for each enabled service whether it filters keys
  (`getCapabilities() & CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS`). Ours by name: `TypeService` («تلفزيونك: التحكم والكتابة من الجوال»), the
  sleep timer's service, `ExtrasA11yService`; and whether the add-on's Home key catch is on (`ExtrasWords.HOME_KEY`, through the add-on's
  provider the home already calls).
- Permission: none.
- What goes in the report: our services on/off by name; **other** services only as a count, and how many of them filter keys (another
  app's service names are not sent).
- Rule: our phone-remote service on (when a phone is linked) «تمام»; it was on before and is off now (`A11yHeal`'s saved list) «انتبه»;
  another app's service that filters keys «انتبه» («فيه تطبيق ثاني يمسك أزرار الريموت، وهذا ممكن يبطّئ الريموت»): this is the path of the
  Android 9/10 freeze in the 4.3.6 review (every key waits for the filter).

**H5 Protected packages left off**
- Reads: for every package of `SetupLists.KEEP` (generated from `optimize/lists/keep.txt`) and every installed package matching
  `SetupLists.CORE` (from `core.txt`): `getApplicationInfo(name, MATCH_DISABLED_COMPONENTS)` → `enabled`, and
  `getApplicationEnabledSetting`. The ledger `disabled-by-us.txt` says whether **we** turned it off. Read only: nothing is turned on here
  (that stays `optimize/keep.sh`, and the setup's own repair).
- Permission: none. **Android 11+ package visibility:** a package is seen only if a `<queries>` entry covers it. Phase 2 generates a
  `<package>` entry for every KEEP name (from the same `gen_setup_lists.py`, about 120 lines in the manifest, checked by
  `tests/setup_lists_test.py`). CORE is a list of patterns, so on 11+ it is checked only against the packages we can see (our intents plus
  the KEEP names); the row says how many were checked.
- Rule: none off «تمام». Off, not in our ledger (the TV shipped it off, e.g. `com.tcl.smartalexa` outside Alexa's regions) «تمام» with
  «مطفي من التلفزيون نفسه، مو منّا» (keep.sh's words). Off **and** in our ledger «فيه مشكلة» («طفّينا شي لازم يبقى شغّال»), with the
  package names (system packages, not personal).
- The assistant and accounts: the same check covers the Google packages in KEEP (Play services, the account login, Katniss, the voice
  search); plus `queryIntentServices(RecognitionService)` and `resolveActivity(RECOGNIZE_SPEECH)` (both in `<queries>` already): none
  found «فيه مشكلة» («البحث بالصوت مو موجود»). **Accounts are never read** (`AccountManager` is not called).

**H6 The screensaver**
- Reads: `Settings.System.SCREEN_OFF_TIMEOUT` (public, readable by any app); `Settings.Secure` `screensaver_enabled`,
  `screensaver_components`, `screensaver_default_component` **when readable** (hidden keys: on Android 8–11 they are read by apps; on
  12+ an app targeting 31+ may get a `SecurityException`; then the home's own adb, **only if the setup already left it paired**, runs the
  read-only `settings get secure screensaver_components` that `SetupReport` runs; else «ما قدرنا نفحص هذا»). Whether our dream is
  installed and enabled (`queryIntentServices(DreamService.SERVICE_INTERFACE)` for `sa.urtv.screensaver`).
- The delay put back at boot: the home already writes the delay (`IdleSetRule`); phase 2 keeps the last value it wrote (`idle-set.txt`,
  one number). The doctor compares the TV's value now with it.
- Permission: none.
- Rule: our dream chosen, on, and the delay one of 1/2/5/10/15/30 min and equal to what we wrote «تمام». Delay 2147483647 or a day or
  more («never», `IdleRule`) «فيه مشكلة» («شاشة التوقف مطفية من إعدادات التلفزيون»). Delay different from the one we wrote «انتبه»
  («التلفزيون رجّع مدة شاشة التوقف بعد التشغيل»). Another dream chosen «انتبه». The TV refuses third-party dreams (the TCL 65C7K
  note in TVS.md) is not detectable from an app: not claimed.

### 2.3 The remote

**K1 The key test (interactive)**
- A screen asks for one key at a time, with its drawing, and shows «وصل» the moment it arrives. Keys, in this order: the four arrows,
  OK, Back, Home, the colour keys (red, green, yellow, blue: `KEYCODE_PROG_RED..BLUE` 183–186), the app keys (YouTube, Netflix, and any
  key the person presses: `BUTTON_3/4` 190/191, TCL 4062/4065/4099, `RemoteButtons`' list), the Settings key (176, 83 on a TCL), the voice
  key (`KEYCODE_SEARCH` 84, `VOICE_ASSIST` 231, `ASSIST` 219).
- How each key is seen:
  - Arrows, OK, Back, colour keys and app keys that reach the window: `Activity.dispatchKeyEvent` in the doctor (key code and scan code
    only, as `UrTVKeys` logs them). The test's own focus never moves on an arrow (the screen consumes it), so Back is not lost.
  - A key the system takes before us (Home, often voice, Settings and app keys on TVs that list them in `global_keys`): the doctor sees
    `onUserLeaveHint`/`onPause`. Home: the doctor marks «waiting for Home» in the home's process; if **our** `MainActivity` receives the HOME
    intent within 3 s it records the time and brings the doctor back → «وصل، وفتح تلفزيونك». If something else came in front, the person
    comes back by themselves (the screen said how before asking) and the row is «فتح شاشة ثانية». Voice: leaving the screen = «فتح
    المساعد».
  - A key that never arrives: after 10 s with nothing, «ما وصل» and the next key; Back skips a key at any time («ما عندي هذا الزر»). The
    colour and app keys are optional (many remotes have none).
- Permission: none. adb's key presses do not go through the remote's path, so this test is only for the real remote.
- Rule: every arrow, OK and Back arrived «تمام». An arrow, OK or Back that did not arrive in 10 s **while other keys did** «فيه مشكلة»
  («زر … ما يوصل»: swallowed). Home opened another screen «فيه مشكلة» (H1's words). An app key or voice key that did nothing (no event, no
  leaving) «انتبه» with the TVS.md note in plain words («بعض التلفزيونات تمسك هذا الزر قبل تطبيقنا»).

**K2 D-pad responsiveness**
- During K1's arrows and then a short run («اضغط يمين ١٠ مرات بسرعة»): for each press `SystemClock.uptimeMillis() − KeyEvent.getEventTime()`
  at `dispatchKeyEvent` (the delay before the key reached us), and the time to the next drawn frame after the screen moves its marker
  (`Choreographer.postFrameCallback`); repeats (`getRepeatCount() > 0`) counted apart.
- Permission: none.
- Rule: median ≤ 100 ms and none over 500 ms «تمام»; median 100–300 ms or one press over 500 ms «انتبه»; median over 300 ms or any over
  1 s «فيه مشكلة» («الريموت يتأخر»), and the row adds H4's key filters and D4's memory when they are the likely cause (said as «ممكن»).
  The numbers in the report: median, worst, count.

### 2.4 The network and the clock

**N1 The connection and a sign-in page**
- Reads: `ConnectivityManager.getActiveNetwork()` and `getNetworkCapabilities`: transport (Wi-Fi / Ethernet), `NET_CAPABILITY_VALIDATED`,
  `NET_CAPABILITY_CAPTIVE_PORTAL`; `LinkProperties.isPrivateDnsActive()` (API 28+, yes/no only). Then one plain HTTP request to
  `http://connectivitycheck.gstatic.com/generate_204` (the host the family DNS check already uses), redirects not followed.
- Permission: ACCESS_NETWORK_STATE, INTERNET (normal, held). The Wi-Fi name, addresses, DNS servers, gateway and MAC are **never read**
  (no `WifiManager.getConnectionInfo`, no `LinkProperties.getDnsServers`).
- Rule: validated and the request answers 204 «تمام». No network «فيه مشكلة» («التلفزيون مو متصل بالإنترنت»). Captive portal, or a 200/30x
  instead of 204 «فيه مشكلة» («الشبكة تطلب تسجيل دخول من المتصفح»). Connected but not validated «انتبه».

**N2 Our data hosts**
- Reads: for each host the apps use (docs/NETWORK.md and DataHost.java: `raw.githubusercontent.com`, `github.com`, `api.github.com`,
  `api.open-meteo.com`, `api.aladhan.com`, `www.thesportsdb.com`, `api.tvmaze.com`, `query1.finance.yahoo.com`, `api.coingecko.com`,
  `archive.org`, `cdn.mp3quran.net`): DNS time (`InetAddress.getAllByName`, the addresses are not kept), then an HTTPS `HEAD` (GET of 1
  byte where HEAD is refused) with 6 s timeouts. Result per host: ok and ms, or which step failed: dns, timeout, tls, http code. Run 4 at a
  time; the host list is one array that phase 2 checks against NETWORK.md in a test.
- Permission: INTERNET (held).
- Rule: all ok «تمام». One to three fail «انتبه» with which part of UrTV it touches (weather, prayer times, matches, updates…). Most fail
  «فيه مشكلة». TLS fails on most hosts «فيه مشكلة» and points to T1 («ساعة التلفزيون غلط» is the usual cause).

**T1 The clock**
- Reads: the `Date` header of N1's and N2's answers (the server's time), against `System.currentTimeMillis()`; `Settings.Global.AUTO_TIME`
  and `AUTO_TIME_ZONE` (public); `TimeZone.getDefault().getID()` against the city's time zone (`cityTz` from the screensaver's config
  provider, read as `ConfigSnapshot` already does; only «same / not the same» goes in the report, not the zone or the city).
- Permission: none beyond INTERNET.
- Rule: within 2 min «تمام». 2–10 min «انتبه». Over 10 min, or the year wrong, «فيه مشكلة» («ساعة التلفزيون غلط، وهذا يخرّب أوقات الصلاة
  والاتصال»), with «شغّل الوقت التلقائي من إعدادات التلفزيون» when AUTO_TIME is 0. Zone not the city's «انتبه». No `Date` header
  reachable: «ما قدرنا نفحص هذا».

**Q1 Quran radio, each station**
- Reads: `assets/quran/stations.json` (13 stations today; hosts backup.qurango.net, stream.radiojar.com, live.kwikmotion.com,
  serverkw.quran-uni.com): for each `url` (and its `alt` when the first fails), open the stream, time to the first byte and to 16 KB, HTTP
  code and whether the content type is audio; close at once (no sound is played). 3 at a time, 8 s per station. Plus the radio's own
  counters the home keeps (`radio_week`: stalls, errors, reconnects, rebuffers in the last 7 days; the last `radio_fault`), the same fields
  share.sh reads.
- Permission: INTERNET (held).
- Rule per station: 16 KB in ≤ 3 s «تمام»; 3–8 s, or only its `alt` answers, «انتبه»; nothing «فيه مشكلة». The row shows the count
  («١١ من ١٣ تشتغل») and opens to the list by station name. The 7-day counters: over 10 stalls «انتبه» («الإذاعة تقطع كثير»).

### 2.5 Speed and crashes

**W1 Switching to our home**
- Reads: the doctor starts `MainActivity` with the HOME intent and an extra `doctor_probe`; the home stamps
  `SystemClock.uptimeMillis()` at its first drawn frame after that intent (`ViewTreeObserver.OnDrawListener`, removed after one frame)
  and brings the doctor back. Three times, the first one counted apart (cold or warm, by whether the home's process was already running:
  `ActivityManager.getRunningAppProcesses` lists our own processes, enough here). On Android 15, `getHistoricalProcessStartReasons` for
  our own package adds the system's own start timings. Plus `LiteAuto`'s «first frame over 8 s» signal.
- Permission: none.
- Rule: warm ≤ 1 s «تمام»; 1–3 s «انتبه»; over 3 s «فيه مشكلة» («الرئيسية بطيئة تفتح»), with D4 when memory is low. Cold over 8 s
  «انتبه».

**C1 Our recent crashes and freezes (our own logs only)**
- Reads: Android 11+ (API 30): `getHistoricalProcessExitReasons(our package, 0, 20)` → reason (CRASH, CRASH_NATIVE, ANR, LOW_MEMORY,
  INITIALIZATION_FAILURE), time; `BetaNotes`' last crash class; `crash-loop.txt`. Android 8–10: only `crash-loop.txt` and `BetaNotes`
  (the system keeps no exit list there). The screensaver and the add-on: each reads **its own** exit list the same way and answers through
  its existing signature-protected provider (phase 2 adds one query to each, counts and class names only). The home's own logcat
  (`logcat -d --pid=<self>`, as `ReportActivity` already runs) gives exception class names only.
- Permission: none (own package only; `DUMP` is never needed).
- Rule: none in 7 days «تمام»; low-memory kills only «انتبه» (D4); a crash or ANR in 7 days «انتبه»; three or more, or a crash loop
  (`CrashLoop`), «فيه مشكلة». Report: counts per reason and at most 5 class names (`Report.name` cleaning).

---

## 3. The run

1. **Start screen**: what it checks, about a minute, «ابدأ الفحص»; and «فحص أزرار الريموت» apart (it needs the person).
2. **Automatic checks** (D1–D6, H1–H6, N1, N2, T1, Q1, W1, C1): about 40 s, the network ones in parallel; each row turns from «جاري الفحص»
   to its result as it finishes. W1 makes the screen jump to the home and back three times: the screen says so first.
3. **The key test** (K1, K2): about a minute, offered after the automatic checks, skippable.
4. **Results**: «فيه مشكلة» rows first, then «انتبه», then «تمام»; each row opens to its details and what to do. The result is kept
   (`doctor-last.txt`, the report's lines) so the next open shows it with its date and «افحص مرة ثانية».
5. **«أرسل التقرير»** (section 4).

The D-pad, Back and OK move as everywhere in UrTV (focus = scale + shadow, no pills, no «·»). Text in plain Gulf Arabic
(STYLE-AR.md), digits through `Digits.of`. English words through `i18n/en` as the rest.

---

## 4. The report and «أرسل التقرير»

**The format: the existing problem report, one more kind.** The on-TV «أرسل تقرير» already builds share.py's allowed lines
(`Report.java`), and the setup has its packed link (`SetupReport.pack`/`shareUrl`: raw deflate + base64url after `#z.`), both opened by
docs/share/ on the phone, which posts them to the same form `optimize/share.sh --send` uses. The doctor adds one kind and one line field:

```
UrTV problem report, doctor
TV: TCL Smart TV Pro (G07), Android 11 (SDK 30), firmware <Build.ID>
Versions: home 4.4.0 (build 431), screensaver 4.4.0 (build 431), extras 4.4.0 (build 431)
channel: beta
Device: Android 11, API 30
WebView: com.google.android.webview-120.0.6099.230
Home: look=classic, appearance=dark, layout=standard, lite=auto-on-t2-s0-o1
Doctor: H1=FAIL home=com.google.android.tungsten.setupwraith role=no provisioned=1
Doctor: K1=FAIL missing=dpad_up got=12 of 13
Doctor: K2=WARN median=180 worst=640 n=22
Doctor: Q1=WARN ok=11 slow=1 down=1 week_stalls=4
Doctor: H5=PASS checked=118 off_tv=1
...
```
(an example of the shape only, not a real TV)

- **One `Doctor:` line per check**, FAIL first, then WARN, then PASS, so when the link must drop lines (the 2953-byte QR limit,
  `Report.url`) the passes go first. Values are `key=value` words of `[A-Za-z0-9._,-]` only (`Report.safe`/`name`).
- `Doctor:` is added to share.py's `ALLOWED_LINE` (and `SetupReport`'s mirror); `tests/settings_report_test.py`'s way is reused: a test
  runs every line the doctor can build (all results, every check) through share.py's `ALLOWED_LINE` and `_scrub_line` and fails if a line is
  dropped or changed by the scrub (that would mean it carried something it should not).
- The nightly analysis reads `Doctor:` lines like the others (phase 2: one parser, one test).

**What is never in it**: the Wi-Fi name, any IP or MAC address, DNS servers, the serial, Android ID, advertising ID, accounts or email,
another app's accessibility service name, installed apps outside our KEEP/CORE lists, the USB drive's label or files, the city, the
coordinates or the time zone (only «same zone as the city: yes/no»). The code does not read most of these at all (section 2 says per
check); what it reads it does not write.

**How it is sent.** Today's rule is «nothing is sent from the TV» (`BetaNotes`, `Report`): the person scans the QR with the phone, sees
the report on docs/share/, writes the problem in their words, and presses «أرسل» there. **Recommended: keep it** (mockup 04). The TV
cannot know that the phone sent it, so the screen does not say «وصل»; it says what to do on the phone.

Option for the owner (mockup 05): «أرسل من التلفزيون» posts the same text from the TV to the same form, as `share.py send()` does from
the computer (one HTTPS POST to docs.google.com, nothing else), after a screen that shows every line that goes. It reaches people without
a phone camera at hand, and then the screen can say «وصل». It changes the rule above, so it is the owner's call.

---

## 5. Phase 2 (after approval): what gets built

- `android/launcher/.../home/doctor/`: the checks as plain Java rules with no Android in them where possible (the pattern of `IdleSetRule`,
  `VersionMismatchRule`: thresholds and words, tested by `tests/*_test.py`), and thin readers for the Android calls; `DoctorActivity`
  (process `:doctor`), `DoctorKeys` (K1/K2), `DoctorReport` (lines, link); the alias for adb.
- Small additions elsewhere: `boot-home.txt` (H2), `idle-set.txt` (H6), the `doctor_probe` stamp in `MainActivity` (W1), one exit-list
  query in the screensaver's and the add-on's providers (C1), the `<package>` queries for KEEP (H5), the row in `SettingsHub` and the
  rescue button, `Doctor:` in `ALLOWED_LINE`, `optimize/dock.sh doctor`.
- Tests: each rule's thresholds; the privacy test above; the WebView probe against check.py; the host list against NETWORK.md; the
  settings tree. CI as today. The emulator round: Android 8 (API 26), 11, 14 and 15 TV images, the automatic checks and the report, plus
  the tester's key test on the owner's TV with the real remote.
- Nothing in it changes a default or a setting of an installed TV; the doctor only reads.

---

## 6. For the owner to decide

1. **Where**: inside the home (recommended, section 1) or a separate `sa.urtv.doctor` APK.
2. **Sending**: QR to the phone only, as today (recommended), or also «أرسل من التلفزيون» (mockup 05).
3. **The key test**: offered after the automatic checks (recommended), or only from its own button.
4. **The two new small records** (`boot-home.txt` for «after a restart», `idle-set.txt` for the screensaver delay): without them H2 and
   the «delay put back» part of H6 can only say «ما قدرنا نفحص هذا».

## Mockups (1920×1080)

| File | What it shows |
|---|---|
| [mockups/01-start.png](mockups/01-start.png) | the start screen: what it checks, «ابدأ الفحص» |
| [mockups/02-keys.png](mockups/02-keys.png) | the key test: the remote drawn, the key asked for, the keys that arrived |
| [mockups/03-results.png](mockups/03-results.png) | the results: «فيه مشكلة» first, then «انتبه», then «تمام», one row opened |
| [mockups/04-report.png](mockups/04-report.png) | «أرسل التقرير» the recommended way: the QR for the phone and what is in the report |
| [mockups/05-sent.png](mockups/05-sent.png) | the option of section 4: sent from the TV |

Sources in `mockups/src/` (`node docs/doctor/mockups/src/render.js`). The rows and words in the mockups are an example, not a real TV.
