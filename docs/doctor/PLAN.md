# «فحص تلفزيونك» (UrTV Doctor): the plan (phase 1, revision 3, nothing built)

**Why it exists, in the owner's words: «I need it to improve our app».** «فحص تلفزيونك» is a free, small app of its own (`sa.urtv.doctor`)
that anyone can install on an Android TV, with or without UrTV. It checks the TV on the TV itself, with no computer and no phone. By default
(after one consent screen) it sends a valid, anonymous report. Every report feeds the loop below. The person sees a simple status and,
only when it is safe for them to do it themselves, a tip. The technical cause and our real fix stay with us.

It **only reads and measures**: it changes no setting, turns nothing on or off, installs nothing, and asks for no permission at run time.

Nothing is built before the owner approves. The decisions are in section 12.

**Revisions**
- Revision 1: a screen inside the home.
- Revision 2: a standalone app, reports as JSON, sending from the TV, triage.
- Revision 3 (this one):
  - the improvement loop first, and the report sent by default after consent;
  - its own release tag;
  - a dedicated HOME section;
  - checks grouped by the problem classes people actually hit;
  - a server-side «تحليل» that only picks from our vetted answers;
  - the person sees only a status and safe tips;
  - a «لاحقًا» section.

---

## 1. The improvement loop

```
the TV runs the doctor ──► report (urtv-doctor/1 JSON, no personal data) ──► our endpoint: validate, fold duplicates
        ▲                                                                             │
        │                                                                             ▼
«انحلّت في الإصدار X، حدّث»                         nightly sync into tshi-dev/urtv-reports (private)
(known-fixes.json, public)                                                            │
        ▲                                                                             ▼
        └── a fix ships, known.json says fixed_in ◄── fix sessions ◄── triage: ranked clusters per TV model and Android,
                                                                    auto-linked to R-rows; weekly owner summary;
                                                                    a list of new issues for the fix sessions
```

- **Every run sends a report by default.** The first run shows one consent screen (section 3); the choice is kept and can be changed.
- **Triage** (section 9) turns the reports into **clusters ranked by installs**, per TV model, Android version and UrTV build:
  - each cluster is linked to an R-row, a TVS.md note or an issue when it matches one;
  - the owner gets a weekly summary;
  - the fix sessions get a list of new clusters (`clean/doctor/new-issues.md`).
- **When a fix ships**, `known.json` (private) gets its `fixed_in` build. The public `known-fixes.json` gets one line («انحلّت في الإصدار X»).
  The next run on an affected TV says so, even without sending.
- **The reports must show which fix works per model.** Each report carries the UrTV build and every check's raw values. Triage can then say,
  per model, that the cause is gone from the build that should have fixed it, or that it is not (a fix that did not hold).

---

## 2. The app and its release

| | |
|---|---|
| Package | `sa.urtv.doctor`, module `android/doctor`. **Its own version** (`android/doctor/VERSION`, starting 1.0.0) and build number, released **independently** of the home. 4.3.7 stays a pure hotfix with no doctor change. |
| Release | Its own workflow (`release-doctor.yml`, run by hand or on a `doctor-v*` tag) publishes `urtv-doctor.apk` and `version.txt` under the release tags **`doctor-latest`** and **`doctor-beta`**. The home's `apk-latest`, `apk-beta` and `apk-next` are untouched. |
| Size | **Under 2 MB**: Java only, no library. The release workflow fails above 2 MB. It carries copies of `stations.json`, the KEEP/CORE lists and the vetted tips (generated at build; tests keep them equal to the sources). |
| Android | `minSdk 21`: it opens on an old TV and says Android is too old for UrTV (the home needs 26) instead of failing to install. Checks are written and tested for Android 8–15 (API 26–35); below 26, a check that needs a newer API says «ما قدرنا نفحص هذا». `targetSdk 34`. |
| Signing | **Our release key**, so our apps' `signature` permissions are granted to it (section 4), and the home can trust it. |
| Permissions | Normal only, granted at install: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE (only for the Wi-Fi signal level below Android 10, section 6.4). Our signature permissions: `sa.urtv.home.DOCTOR` (new) and `sa.urtv.screensaver.READ_CONFIG` (exists). **No dangerous permission**, no usage access, no accessibility service (another key filter would be the freeze it looks for), no overlay. |
| `<queries>` | Intents: HOME, LAUNCHER, LEANBACK_LAUNCHER, RECOGNIZE_SPEECH, RecognitionService, DreamService, `Settings.ACTION_HOME_SETTINGS`, `MANAGE_DEFAULT_APPS_SETTINGS`. Packages: `<package>` for our five apps, every KEEP package, and the known stock and operator homes (generated). |
| Name | «فحص تلفزيونك», free, with a leanback banner in the apps row. |
| Its own updates | At start it reads `doctor-latest/version.txt`. When behind, it says «فيه نسخة أحدث من الفحص» and installs only through Android's confirm screen. |

**How people get it**
1. **Downloader**: its own short code, registered by the owner for `…/releases/download/doctor-latest/urtv-doctor.apk`, like 9378345 (the setup
   app) and 8042301 (the rescue app).
2. **From UrTV**, in a later home release (not 4.3.7): the row «الإعدادات ← حول والتحديث ← فحص التلفزيون».
   - If `sa.urtv.doctor` is installed **and signed with our key** (`getPackageInfo(GET_SIGNING_CERTIFICATES)` against the pinned digest
     `SignDigest` uses), the row opens it (explicit intent to `sa.urtv.doctor/.MainActivity`).
   - If not, the row shows the Downloader code and offers to install it from `doctor-latest` through Android's confirm screen, the add-on's way
     (`ExtrasActivity`, `PackageInstaller`, signer checked first).
   - The home's `<queries>` gets `<package android:name="sa.urtv.doctor"/>`.
3. **An AI agent or the tester over adb**:
   - Install with `optimize/install.sh doctor` (the release APK from `doctor-latest`, never a build of its own; AGENTS.md).
   - Run with `optimize/dock.sh doctor`: `am start -n sa.urtv.doctor/.MainActivity --ez auto true --ez send false`. That runs the automatic checks
     only (adb's key presses skip the remote's path, so no key test) and sends nothing.
   - The results go to logcat under the tag `UrTVDoctor`; `dock.sh` prints them. A problem found this way is reported the AGENTS.md way,
     with `optimize/share.sh`.

---

## 3. Consent and what is sent

**The consent screen** (mockup 04), once, before the first report:
- Title «نرسل تقرير الفحص؟».
- What goes: the TV's type, Android, our apps' versions, and each check's result.
- What never goes: the network name, addresses, the serial number, accounts, location, anything typed.
- One honest line: «يُحلَّل التقرير آليًا لدى مزوّد خدمة نستخدمه» (the provider, Anthropic, is named on the privacy page).
- «إرسال» (focused) and «لا ترسل».
- An optional row «أرسل المدينة مع التقرير», off by default.

The choice is kept in the doctor's own storage. The start screen has «إرسال التقرير: مشغّل/مطفي» to change it.

**Never sent, and mostly never read**: the Wi-Fi name or BSSID, any IP or MAC, DNS servers, the serial, Android ID or advertising id, accounts or
email, coordinates or the time zone, other apps' accessibility service names, installed apps outside our lists, the USB drive's label or files.
The city is sent only with the person's yes, as the city **setting's** name.

**Free text**: none by default (decision 9). The report is only machine values, so it can carry nothing personal.

**Not presented as AI anywhere in the app.** The words are «تحليل» (the analysis) and «الحل المقترح» (the suggested fix). The consent line and
the privacy page are the honest disclosure.

**The privacy page**: `docs/doctor/PRIVACY.md`, Arabic first, phase 2. The app links to it from its start screen and its consent screen.

---

## 4. What the doctor can read: alone, and with UrTV

- **Alone** (no UrTV): its own normal permissions.
- **With UrTV, older home**: the same, plus our apps' versions and signers.
- **With UrTV, a home that has the doctor provider** (phase 2): the home answers `content://sa.urtv.home.doctor`, readable only with
  `sa.urtv.home.DOCTOR` (protectionLevel `signature`). The screensaver and the add-on answer one `exits` query each, through their existing
  signature-protected providers.

The provider gives only numbers and short words, and only what the home alone can know:

| Row | Why only the home has it |
|---|---|
| `role` | `RoleManager.isRoleHeld(ROLE_HOME)` answers only for the app asking. |
| `exits` | Other apps' exit records need `DUMP`; the home reads its own. |
| `crash_loop` | `crash-loop.txt` (CrashLoopStore). |
| `beta_crash` | BetaNotes' crash class. |
| `boots` | **New record** `boot-home.txt`, the last 5 boots, each with: seconds to the home's first frame; whether that came through HOME; whether each stock home (launcherx, tvlauncher, leanbacklauncher, the TV's own) was **enabled at boot**. A stock home enabled at boot after the setup turned it off is how «the firmware turns its home back on at every start» is seen. |
| `home_fails`, `boot_guard` | Counts and dates. |
| `ledger` | `disabled-by-us.txt`'s package names. |
| `lite` | `Ram.tier`, the `lite` setting, LiteAuto's word. |
| `radio_week`, `radio_fault` | The fields share.py reads. |
| `idle_set` | **New record** `idle-set.txt`: the last delay IdleSetRule wrote. |
| `a11y_saved` | A11yHeal's saved list of our services. |
| `home_key` | The time the home last received a HOME intent, for the key test. |
| `switch` | The uptime of the home's first frame after a HOME intent carrying `doctor_probe`. |
| `screensaver` | The component, through the home's own adb, only if the setup left it paired (read-only `settings get`). |

---

## 5. HOME: why UrTV is not the Home screen on this TV (the first priority)

This is the biggest cluster in the reports. In `tshi-dev/urtv-reports` `clean/analysis.md` (738 reports, 28 Nov):
- «the setup wizard of this TV answers Home and owns its saved Wi-Fi»: 45 reports;
- «Home did not open the new launcher»: 29 + 9;
- «could not tell which app is this TV's own home»: 3;
- the step that stops most is `launcher`, on Xiaomi MiTV-AFKR0/AFMU0, TCL Smart TV (Pro), Chromecast, Haier MatrixTV and Sony BRAVIA.

The HOME section runs eight reads and ends in **one cause per TV**.

| Id | Reads | API and permission | Alone / with UrTV |
|---|---|---|---|
| **H1** Who answers HOME | the default: `resolveActivity(MAIN+HOME, MATCH_DEFAULT_ONLY)`; every candidate: `queryIntentActivities(HOME, MATCH_DISABLED_COMPONENTS)`, with its package, whether it is a system app (`FLAG_SYSTEM`), enabled, and its filter priority | none (`<queries>` HOME) | both |
| **H2** The role | SDK ≥ 29: `RoleManager.isRoleAvailable(ROLE_HOME)`; whether the role request screen exists (`resolveActivity` of `createRequestRoleIntent(ROLE_HOME)`); the holder **only as "ours or not"**, through the home's `role` | none; `DOCTOR` for `role` | alone: availability and the request screen only |
| **H3** Wizard and provisioning | whether `com.google.android.tungsten.setupwraith` (and the TV's own OOBE packages, a generated list from TVS.md) is installed, enabled, and declares HOME; `Settings.Global.DEVICE_PROVISIONED` (public); `Settings.Secure` `user_setup_complete` only if readable (a hidden key may throw on 12+, then UNKNOWN); whether the wizard is a Chromecast/Streamer cast provider (TVS.md: maker Google, or sabrina, boreal, kirkwood) | none | both |
| **H4** The stock home | `getApplicationEnabledSetting` of launcherx, tvlauncher, leanbacklauncher and the TV's own (from `home-replaced.txt` and SetupLists); with UrTV, the `boots` record (enabled at boot after we turned it off) and the `ledger` | none; `DOCTOR` | alone: enabled now |
| **H5** Other launchers | every HOME candidate not a stock home and not ours, as known (operator images, e.g. the JAWWY TV row of TVS.md) or other (third-party) | none | both |
| **H6** The TV's own default-home screen | `Settings.ACTION_HOME_SETTINGS` and `MANAGE_DEFAULT_APPS_SETTINGS` resolvable | none | both |
| **H7** The Home key | in the key test (K1): the person presses Home. With UrTV: the home's `home_key` stamp says whether **our** home received it, and the add-on's log line (`home key caught ours opened`) through its provider says whether its catch saw it. Alone: only «left the screen». | none; `DOCTOR` | partly alone |
| **H8** After a restart | the `boots` record: whether each of the last 5 boots reached our home | `DOCTOR` | UrTV only |

**The cause** is a decision table, plain Java, tested on fixtures. One cause wins, in this order:

| Cause code (internal) | When | Source |
|---|---|---|
| `not_installed` | alone | – |
| `signed_other_key` | our home signed by another key | AGENTS.md, A1 |
| `not_provisioned` | provisioned 0 or setup not complete | H3 |
| `wizard_owns_home` | setupwraith (or an OOBE) answers HOME, enabled | 45 reports; TVS.md «When the setup wizard owns the Wi-Fi» |
| `cast_device_wizard` | the same on a Chromecast or Streamer (the wizard is kept for casting) | TVS.md, Chromecast note |
| `gtv_starts_launcherx` | role ours, yet Home opens launcherx (H7) | TVS.md «Google TV: Home works through the add-on» |
| `stock_reenabled_at_boot` | a stock home we turned off was enabled at a boot (H4, H8) | the new `boots` record |
| `operator_launcher` | a known operator home answers HOME | TVS.md, JAWWY TV |
| `other_launcher` | a third-party home is the default | H5 |
| `no_default` | the chooser answers | H1 |
| `unknown_own_home` | no stock home known and no single candidate | 3 reports |
| `ok` | ours answers, the role is ours, the Home key reached us, the last boots reached us | – |

**What the person sees** (mockup 08): a status, «الرئيسية تمام» or «زر الرئيسية ما يفتح تلفزيونك، ووصلنا تقريرك». Under it, «الحل المقترح» only
when a vetted safe tip applies (section 7.1):
- `wizard_owns_home`: the tip `wifi_rejoin`, only if the owner approves it (decision 7). It is the step `install.sh launcher` already asks the
  person to do, said with its guard: «إذا تعرف كلمة سر الواي فاي».
- the others: «انحلّت في الإصدار X، حدّث» when `fixed_in` is known; otherwise the status only.

The cause code stays in the report, and triage can tell per model which cause it is and whether a fix holds. **The doctor never touches the
Wi-Fi, the wizard or any home.**

---

## 6. The checks by problem class

Each check: what it reads, the API and permission, alone/with UrTV, and its **internal** rule:
- PASS, WARN, FAIL;
- UNKNOWN when unreadable on this TV;
- SKIP when not asked or not relevant.

Each check also gives the **cluster** it exists for. Raw values go in the report beside the verdict; the fields are listed in
[`doctor-report-1.schema.json`](doctor-report-1.schema.json).

### 6.1 The TV

- **D1 TV and Android.**
  - Reads: `Build.MANUFACTURER`, `MODEL`, `DEVICE`, `VERSION.RELEASE`, `SDK_INT`, `Build.ID`, `SECURITY_PATCH`, `FEATURE_LEANBACK`; Google TV =
    launcherx installed. Never `getSerial()`.
  - Rule: SDK under 26 FAIL.
  - Cluster: the `precheck` stops.
- **D2 WebView version.**
  - Reads: `WebView.getCurrentWebViewPackage()` (26+; below, `getPackageInfo` of the two WebView packages).
  - Rule: ≥ 80 PASS; 73–79 WARN; under 73 or none FAIL.
  - Cluster: R-381 (blank page on WebView 66–72), the `screensaver` stops.
- **D3 WebView JavaScript.**
  - Reads: a hidden WebView runs a 1 KB local page. The features are `tests/check.py`'s list (fromEntries, flat, matchAll, replaceAll,
    allSettled, `?.`, `??`, flex gap, clamp); a test keeps the two lists equal.
  - Rule: a non-polyfilled feature missing FAIL.
  - Cluster: R-381.

### 6.2 Slowness

Clusters: «the screensaver may slow this TV» (17 + 6 reports), «the safety switch put the stock home back (adb was too slow)» (16), R-300 (lite
mode not on for a struggling TV), slow app switching.

- **S1 Memory.**
  - Reads: `ActivityManager.getMemoryInfo` (total, available, `lowMemory`, `threshold`), `isLowRamDevice()`, `getMemoryClass()`; with UrTV, the
    home's tier, `lite` setting and LiteAuto state.
  - The tier rule is `Ram.tier()`'s, shared and tested.
  - Rule: LOW with light mode off by hand WARN; under 10% free or `lowMemory` WARN.
- **S2 CPU class.**
  - Reads: `availableProcessors()`, `SUPPORTED_64_BIT_ABIS`, top speed from `cpufreq/cpuinfo_max_freq` only if readable.
  - **CPU load is not readable** by apps from Android 8 (`/proc/stat` is closed to them), so it is not claimed; S7's frame timing is the load
    signal.
  - Rule: two cores or fewer, or 32-bit only, WARN (information for lite mode).
- **S3 Our processes.**
  - Reads: `getRunningAppProcesses()`. From Android 5 it lists **only the caller's own processes**, so this is the doctor's own view; with UrTV,
    the home's provider adds whether the home, the screensaver and the add-on processes are alive.
  - Other apps' processes are not readable and not claimed.
- **S4 Storage.**
  - Reads: `StatFs` of the data partition.
  - Rule: under 300 MB FAIL, under 1 GB WARN.
  - Cluster: `install_failed_insufficient_storage` (3).
- **S5 Heat.**
  - Reads: `PowerManager.getCurrentThermalStatus()` (29+).
  - Rule: MODERATE WARN; SEVERE or worse FAIL; below 29 UNKNOWN.
- **S6 Home start and switch** (UrTV only).
  - Reads: the doctor starts the home with HOME + `doctor_probe` three times; the home stamps its first frame (`switch`). The first is counted
    cold or warm.
  - Rule: warm ≤ 1 s PASS, 1–3 s WARN, over 3 s FAIL; cold over 8 s WARN.
- **S7 Smoothness.**
  - Reads: the doctor plays its own 3-second test animation (a moving card, the size of a home tile) and reads `FrameMetrics` (`Window`
    `addOnFrameMetricsAvailableListener`, 24+). Jank is the share of frames over one refresh interval (`Display.getRefreshRate`).
  - Rule: jank ≤ 5% PASS, 5–20% WARN, over 20% FAIL.
- **S8 Animation scales.**
  - Reads: `Settings.Global` `ANIMATOR_DURATION_SCALE`, `TRANSITION_ANIMATION_SCALE`, `WINDOW_ANIMATION_SCALE` (public).
  - Rule: any above 1 WARN (feels slow); 0 is information (the setup may set it on purpose).

### 6.3 The remote

Clusters: R-362 (a TCL OTT box, Android 8: «the remote moves briefly, then nothing»), R-306 (the YouTube key opened nothing), TVS.md «The
remote's shortcut keys» (6 reports), the 4.3.6 review (every key waiting for a key filter on Android 9/10).

- **K1 Every key.**
  - The screen asks for one key at a time, with a drawing:
    1. the arrows, then OK and Back;
    2. Home (H7);
    3. the colour keys;
    4. the app keys (190/191, TCL 4062/4065/4099, `RemoteButtons`' codes);
    5. Settings (176/83);
    6. voice (84/231/219).
  - For each key it records:
    - whether it arrived (`dispatchKeyEvent`, key and scan code only);
    - or left the screen (`onUserLeaveHint`/`onStop`, for keys the system takes);
    - or never came (10 s; Back skips);
    - its latency (`uptimeMillis − getEventTime`);
    - a **stuck** key (a down with no up within 2 s, or repeats the person did not hold);
    - a **repeated** key (two downs within 50 ms).
  - Rule: an arrow, OK or Back missing while other keys arrived FAIL; a stuck key FAIL; an app or voice key that did nothing WARN.
- **K2 The D-pad over 30 keys.**
  - «اضغط يمين ثم يسار، ١٥ مرة» on a row of tiles drawn like the home's.
  - For each press: latency, and the time to the next drawn frame after the focus moves (`Choreographer`).
  - The R-362 round's own rule: a key unanswered in 8 s, or the focus moving fewer than 4 times, or never after the 10th key, is FAIL.
  - Otherwise: median ≤ 100 ms PASS, 100–300 ms WARN, over 300 ms or any over 1 s FAIL.
- **K3 Which remote.**
  - Reads: for the device that sent the keys (`KeyEvent.getDeviceId` → `InputDevice`): `getName`, `getVendorId`, `getProductId`, and
    `isExternal()` (29+). An external device with a vendor id is very likely Bluetooth; the built-in IR receiver is not external.
  - Bluetooth pairing lists are **not read**: they need `BLUETOOTH_CONNECT`, a dangerous permission.
  - Rule: information. With K1 failures on a Bluetooth remote, the tip `remote_pair`.
- **K4 Apps that can swallow keys.**
  - Reads: `getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` and `ENABLED_ACCESSIBILITY_SERVICES` (public). For each enabled service,
    whether it filters keys (`CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS`). This is how key remappers work on Android TV, so it finds them
    without a list of names.
  - Ours are reported by name; other apps' only as counts.
  - Rule: another app filtering keys WARN; with K2 slow, FAIL.
- **K5 HDMI-CEC.**
  - Reads: `Settings.Global` `hdmi_control_enabled`, **when readable** (hidden; it may throw on 12+). `HdmiControlManager` needs a system
    permission, so it is not used.
  - Information; unreadable = UNKNOWN.

### 6.4 Network

Clusters: «TV not answering» and «did not come back» (about 140 reports: the computer's link to the TV, often a weak or busy Wi-Fi), «could not
download (no internet)» (14), the data each feature needs.

- **N1 Link and signal.**
  - Reads: `getNetworkCapabilities`: transport (Wi-Fi or Ethernet), `VALIDATED`. Signal: `NetworkCapabilities.getSignalStrength()` (29+), or
    below 29 `WifiManager.getConnectionInfo().getRssi()` (ACCESS_WIFI_STATE, normal). **Only the RSSI is kept**, never the SSID or BSSID.
  - Rule: Ethernet, or RSSI ≥ −67 dBm, PASS; −67 to −75 WARN (tip `wifi_weak`); under −75 FAIL (tip `wifi_weak`).
- **N2 Sign-in page.**
  - Reads: `CAPTIVE_PORTAL`, and `http://connectivitycheck.gstatic.com/generate_204`, redirects not followed.
  - Rule: not 204, or a portal, FAIL (tip `captive`).
- **N3 Our hosts: DNS, reach, speed.**
  - For each host of docs/NETWORK.md and DataHost.java, plus `script.google.com`:
    - DNS time (`InetAddress.getAllByName`; addresses not kept);
    - HTTPS `HEAD`, 6 s, 4 at a time.
  - Then one **speed** read: a fixed 512 KB file on the data host (`speed-512k.bin` in `tshi-dev/urtv-data`, new), in KB/s.
  - Rule: 1–3 hosts fail WARN; most fail FAIL; TLS failing on most points to T1; speed under 100 KB/s WARN.

### 6.5 The rest

- **T1 Clock.**
  - Reads: the `Date` header of N2/N3 against the TV's clock; `AUTO_TIME`, `AUTO_TIME_ZONE` (public); with UrTV, the city's zone, kept only
    as «same / not».
  - Rule: over 10 min off FAIL (tip `clock_auto` when AUTO_TIME is 0); 2–10 min WARN; the zone not the city's WARN.
- **Q1 Quran radio, each station.**
  - Reads: the doctor's copy of `stations.json` (13 stations): time to 16 KB, the HTTP code, whether it is audio, the `alt` when the first
    fails; 3 at a time, 8 s each, nothing played. With UrTV: `radio_week`, `radio_fault`.
  - Rule per station: ≤ 3 s PASS, 3–8 s or only `alt` WARN, none FAIL.
  - Cluster: R-357, radio stalls.
- **A1 Our apps.**
  - Reads: the home, screensaver, add-on, rescue and doctor: version, build, enabled, **signer ours or not**; the newest build (`version.txt`);
    `VersionMismatchRule`.
  - Rule: a signer not ours FAIL (it can never update; AGENTS.md); home and screensaver mismatched FAIL; one behind WARN (tip `update`).
  - Clusters: «signed with another key» (2), install verification failures (13).
- **P1 Protected packages, voice and accounts.**
  - Reads: every KEEP package and every visible CORE match: `enabled`, `getApplicationEnabledSetting`; with UrTV, whether **we** turned it off
    (`ledger`). Voice: `RecognitionService` and `RECOGNIZE_SPEECH` resolvable. **Accounts are not read** (no AccountManager; an app sees no
    accounts but its own anyway); the account-related packages in KEEP (Play services, the Google account login, the TV's account screens)
    enabled or not stand for «accounts state».
  - Rule: off and in our ledger FAIL; off by the TV itself PASS with a note; alone, off WARN.
  - Clusters: «protected but off» (42 + 13 + 2), «still off» (2), R-362 (`com.tcl.partnercustomizer`), R-380, accounts or assistant broken
    after setup.
- **V1 Screensaver.**
  - Reads: `SCREEN_OFF_TIMEOUT` (public); `screensaver_enabled` and `screensaver_components` when readable (may throw on 12+; with UrTV, the
    home's adb read); our dream installed (`queryIntentServices(DreamService)`); with UrTV, `idle_set`.
  - Rule: delay of a day or more FAIL («never»); different from `idle_set` WARN (put back at boot); another dream WARN.
  - Clusters: the `screensaver` stops, issue #42. A firmware that refuses third-party dreams is not detectable and not claimed.
- **U1 USB.**
  - Reads, after «فيه فلاشة موصّلة الحين؟»: `StorageManager.getStorageVolumes()`: removable, state, readable on 30+. The label, UUID and files
    are not read.
  - Rule: present but unmountable FAIL (tip `usb_fat32`); none seen FAIL (tip `usb_port`).
- **C1 Crashes** (UrTV only).
  - Reads: each app's own exit records (API 30+) through its provider, `crash_loop`, `beta_crash`.
  - Rule: none in 7 days PASS; a crash or ANR WARN; three or more, or a loop, FAIL.

**34 checks** in all: D1–D3, H1–H8, S1–S8, K1–K5, N1–N3, T1, Q1, A1, P1, V1, U1, C1.

---

## 7. What the person sees, and the «تحليل»

### 7.1 Status only, safe tips only

Every check maps to **one of three statuses** on the screen:
- «تمام»;
- «فيه مشكلة ووصلنا تقريرك» («فيه مشكلة» when sending is off);
- «انحلّت في الإصدار X، حدّث».

Under a status, «الحل المقترح» shows **only a vetted safe tip**, chosen by its id. The texts are ours, written once, in the public
`known-fixes.json` and in the APK.

| Tip id | The words (draft) | When |
|---|---|---|
| `wifi_weak` | الواي فاي ضعيف عند التلفزيون: قرّب الراوتر، أو وصّل سلك إنترنت | N1 |
| `captive` | الشبكة تطلب تسجيل دخول: سجّل فيها من المتصفح ثم افحص مرة ثانية | N2 |
| `clock_auto` | شغّل «الوقت التلقائي» من إعدادات التلفزيون ← التاريخ والوقت | T1 |
| `storage` | المساحة شبه خلصت: احذف تطبيقات ما تستخدمها من إعدادات التلفزيون ← التطبيقات | S4 |
| `update` | حدّث تلفزيونك: الإعدادات ← حول والتحديث ← تحديث تلفزيوني | A1, a `fixed_in` |
| `lite_on` | شغّل «الوضع الخفيف»: الإعدادات ← حول والتحديث ← الوضع الخفيف | S1 |
| `restart` | طفّ التلفزيون من الفيش دقيقة، ثم شغّله وافحص مرة ثانية | S5, C1 |
| `remote_pair` | غيّر بطاريات الريموت، أو اربطه من جديد من إعدادات التلفزيون ← الريموت | K1 with K3 Bluetooth |
| `usb_fat32` | جرّب فلاشة نوعها FAT32 | U1 |
| `usb_port` | جرّب منفذ USB ثاني | U1 |
| `wifi_rejoin` | إذا تعرف كلمة سر الواي فاي: من إعدادات التلفزيون انسَ الشبكة واتصل فيها مرة ثانية | HOME `wizard_owns_home`, **only if approved** (decision 7) |

**Never a tip**: a factory reset, turning a package off or on, uninstalling, anything about accounts, PINs or payments, developer options, adb.
A test fails if a tip's words contain any of them. The cause code, the R-row and our real fix stay in the report, `known.json` and the
triage, never on the screen.

### 7.2 «تحليل» (mockup 07)

The results screen has a panel «تحليل»: one line on the TV's overall state, the problems by status, and «الحل المقترح» tips. It is filled in
three layers, the last one only when the first two leave a FAIL unexplained:
1. **Local rules, on the TV, always.** The decision table above, offline. This is the basic answer with no network, and the answer when the
   server is slow.
2. **Server rules.** `known.json` (private) matched by the endpoint, giving `fixed_in` lines and tips.
3. **The analysis model, on the server, grounded.** For a report with a FAIL that neither rules layer explains:
   - The endpoint asks the model to **choose** a cause id from `known.json` and at most two tip ids from the vetted list, or `unknown`.
   - The server checks the ids against the lists and drops anything else.
   - The TV shows only our own words for those ids. **The model's words are never shown**, so it cannot give a harmful step.
   - `unknown` shows «ما نعرف السبب للحين، وصلنا تقريرك».

**The flow and its limits**
- The TV posts the report (`op=report`). The reply is quick: the code and the server-rules result.
- It then asks `op=analyse` with the code and waits **up to 20 s**:
  - in time: the «تحليل» panel updates;
  - not in time or offline: the panel keeps the local answer and says «التحليل يوصل بعدين». The next open of the results asks again
    (`op=analysis`, cached on the server for 7 days).
- **Rate**: at most 3 analyses per install id in 6 h.
- **Cost**: a **global daily cap**, `AI_DAILY_USD` (Script Property, default 1). The script adds up each answer's `usage` at the model's prices
  in a per-day counter. Once the cap is hit, rules only until the next day (Riyadh). A fixed-result report (same install and same results) is
  answered from the cache, at no cost.
- **Where the model is called**: the same Apps Script project (`apps-script/doctor.gs`), with `UrlFetchApp` (Apps Script has no Anthropic SDK,
  so raw HTTP is the way):
  - `POST https://api.anthropic.com/v1/messages`;
  - headers `x-api-key` and `anthropic-version: 2026-10-21`.
- **The key**: the Script Property **`ANTHROPIC_API_KEY`** in that Apps Script project (Project Settings → Script Properties), never in the APK
  or a repository. Make it in the Anthropic Console in its **own workspace** (e.g. `urtv-analysis`), with that workspace's monthly spend limit
  as the hard stop.
- **The model**: **`claude-haiku-5-5`**, proposed for cost. Request: `output_config: {effort: "low", format: <JSON schema>}`,
  `max_tokens: 1024`, the system prompt marked `cache_control`. No `temperature` (it is refused on this model).
- **Cost**: about 4,000 input tokens (mostly the cached prompt) and up to a few hundred output tokens, at $0.10 / $0.50 per million. That is about
  a tenth of a cent per analysis, so the default daily cap of $1 covers several hundred analyses.
- **The output schema** (built per request from the lists): `{cause: enum [<known.json cause ids>, "unknown"], tips: array ≤ 2 of enum [<tip
  ids>], confidence: enum [low, medium, high]}`.
- **The system prompt** (`apps-script/doctor_prompt.txt`, fixed):

  > You match one diagnostic report from an Android TV running UrTV to known causes. The report is machine values only: checks with an id, a
  > verdict and raw values. Choose the single cause from the list below whose conditions the report meets, or "unknown" when none clearly
  > does. Choose at most two tips from the tip list, only tips whose "when" condition the report meets; tips are things a TV owner can safely
  > do alone. Never choose a cause or tip that is not in the lists. Prefer "unknown" to a guess.
  >
  > Causes (id, conditions, notes from our test reports, TVS.md and the R-rows):
  > <known.json, rendered as lines>
  >
  > Tips (id, when):
  > <the vetted tip list>

- **The user message**: the report's `tv` block and `checks` (id, verdict, why, raw). Nothing else: no install id, time or city.

---

## 8. The report and the endpoint

**The report: one JSON**, schema `urtv-doctor/1`, written out in [`doctor-report-1.schema.json`](doctor-report-1.schema.json) (JSON Schema
2020-12, `additionalProperties: false` everywhere, short-token strings). It has:

| Field | What it holds |
|---|---|
| `schema` | The schema's name and version. |
| `app` | `version`, `build`, `cert` (the doctor's own signing certificate SHA-256, read at run time), `channel` (`doctor-latest` or `doctor-beta`). |
| `install` | A random id made at the first start: 128 bits from `SecureRandom`, Crockford base32. It is gone with an uninstall or a data clear; never a hardware id. |
| `time` | The network time from T1 (the `Date` header), or null, and the TV's offset from it. |
| `tv` | The TV's type and Android. |
| `urtv` | Whether UrTV is installed, and our apps. |
| `checks` | id, verdict, `why` (the internal cause or reason code), raw. |
| `home_cause` | The cause the HOME section found. |
| `consent` | What the person agreed to. |
| `city` | Only with consent. |
| `hash` | SHA-256 of the canonical JSON. |

The short code «D-XXXX-XXXX» comes from `hash`, so it is the same however the report travels. A report is about 6–10 KB.

**The endpoint: an Apps Script web app** bound to the same spreadsheet as the setup reports' form (the form cannot validate or answer):
- It is `apps-script/doctor.gs` in urtv-reports, deployed once by the owner: Deploy, New deployment, Web app, Execute as: Me, Who has access:
  Anyone.
- It writes a new tab `doctor`. `sync.gs` commits that tab nightly to `reports-doctor/YYYY-MM-DD.jsonl`, beside the setup reports, which stay
  as they are.

`op=report` does:
- **size**: 32 KB at most.
- **schema**: a hand-written validator, tested under node on the app's fixtures.
- **versions**: `ACCEPT_SCHEMAS`, `MIN_DOCTOR_BUILD`, and `DOCTOR_CERT` equal to our certificate (all Script Properties).
- **hash**: recomputed.
- **time**: within 48 h.
- **rate**: 4 reports per install in 6 h; 1000 in all in 6 h (CacheService). Apps Script does not give the script the sender's IP; none is
  stored.
- **fold**: the same install and the same sorted `id:verdict:why` within 7 days updates `seen` instead of adding a row.
- **refused**: each refusal is counted by reason in `doctor_refused`; no payload is kept.
- Scopes: none new (`spreadsheets.currentonly`, `script.external_request`).
- Honest limit: the hash and the certificate digest stop junk and accidents, not a determined forger. A sideloaded app cannot prove itself
  to a server.

**Without the network**, or when the endpoint fails:
- the report is kept (the newest one);
- «حاول مرة ثانية» and «أرسل بالجوال»: a QR of the deflated JSON after `#d.` on docs/share/ (mockup 06), whose «أرسل» posts the same JSON;
- the code still shows, since it comes from `hash`.

---

## 9. Triage in `tshi-dev/urtv-reports`

Beside the existing setup-report tools (`clean.py`, `analyse.py`, `nightly.yml`): standard-library Python, tests on made-up rows only. The rule
there stands: never a real row in tests, docs or commits.

- **`tools/doctor_clean.py`**: validates each row again (the validator ported to Python, the same fixtures), drops emulator runs, and folds
  what the server missed.
- **`tools/doctor_triage.py`**:
  - **signature** per report: the HOME cause plus the sorted `id:verdict:why` of FAIL and WARN.
  - **clusters**, ranked by distinct installs, with counts per model, Android, UrTV build and doctor build; first and last seen; this week
    against last week.
  - **links**: `doctor/known.json` maps rules to cause ids, R-rows (UrTV's `docs/dev/REGRESSIONS.md`, read from the public repository), TVS.md
    notes, issues and `fixed_in`. A cluster seen **after** its `fixed_in` build is flagged «fix did not hold».
  - **per model, which fix works**: for each HOME cause and model, the share of TVs on each UrTV build that still show it.
- **Outputs**:
  - `clean/doctor/clusters.json`;
  - `clean/doctor/weekly/YYYY-Www.md`, the owner's weekly summary, with one issue «Doctor reports: weekly» kept and commented in the private
    repository;
  - `clean/doctor/new-issues.md`, the list for the fix sessions: new clusters with 3 or more installs, each with its signature, models, counts
    and the raw values that set it apart.
- **The triage's own summarising model** (optional, decision 11): a nightly Actions job that writes one English line per new cluster for the
  weekly summary. It uses the GitHub Actions secret **`ANTHROPIC_API_KEY`** in `tshi-dev/urtv-reports` (Settings → Secrets and variables →
  Actions → New repository secret), and its own Console workspace with a monthly spend limit. Model `claude-haiku-5-5` through Message
  Batches, at most 100 clusters a night, `AI_MONTHLY_USD` 5. Suggestions only; `known.json` and `known-fixes.json` are changed by a person.
- **`known-fixes.json`** (public, `tshi-dev/urtv-data`) carries only rule ids, `fixed_in`, and the vetted tips' words. **Never a technical
  cause.**

---

## 10. The screens

| # | Screen | Mockup |
|---|---|---|
| 1 | Start: what it checks, what it found («لقينا تلفزيونك مثبّت»), «ابدأ الفحص», the send setting | 01 |
| 2 | The automatic checks (about 45 s; the screen says before S6 that it will jump to the home and back) | – |
| 3 | The key test, after the automatic checks, skippable | 02 |
| 4 | Results: one status per problem class, problems first; «الحل المقترح» where a safe tip applies | 03 |
| 5 | «تحليل»: the overall answer and the tips | 07 |
| 6 | The HOME result | 08 |
| 7 | Consent (first run) | 04 |
| 8 | Sent: «وصل التقرير، شكرًا» and the code | 05 |
| 9 | By phone: the QR | 06 |

Focus = scale + shadow, no pills, no «·», plain Gulf Arabic, digits through `Digits.of`, English through `i18n/en`.

---

## 11. لاحقًا (later, not designed now): a paid assistant on the TV

The owner's idea for later: chosen users, after paying, get a limited assistant on the TV that they can talk to and that can do things on
the TV. Not designed or built now. Whatever is designed later must meet these:
- **A paid quota per user**, metered on the server, with the model key only on the server.
- **A strict allow-list of actions**: open an app or a settings screen, change our own UrTV settings, run our diagnostics. **Never** a factory
  reset, turning packages off, accounts, PINs or payments.
- **Each action confirmed on screen** before it runs.
- **A log of every action** the user can see.
- **No personal data sent** beyond what the user types.

What the doctor keeps open for it:
- The same signed app, install id and endpoint can carry a paid session later; `urtv-doctor/1` leaves room for a new `op` without changing
  the report.
- The doctor stays read-only; an assistant that acts would be its own reviewed component behind the allow-list, never a change to the doctor.

---

## 12. Decisions for the owner

Proposed defaults are in **bold**.
1. **A standalone free app** `sa.urtv.doctor`, minSdk 21, our key, its own release tags `doctor-latest`/`doctor-beta`, **≤ 2 MB**; 4.3.7 untouched.
2. **A Downloader code** for `doctor-latest/urtv-doctor.apk`, registered by you.
3. **Send from the TV by default**, after the one-screen consent; the QR as fallback.
4. **The key test after the automatic checks**, skippable.
5. **The home's provider and the two small records** (`boot-home.txt` with the stock homes' state at boot, `idle-set.txt`). Without them, H2,
   H4's «turned back on at boot», H7, H8, S6 and C1 cannot be checked.
6. **The person sees only a status and vetted safe tips**; causes and fixes stay internal; `known-fixes.json` carries only «fixed in X» and tips.
7. Whether the tip `wifi_rejoin` (forget and rejoin the Wi-Fi, only if the password is known) may be shown for `wizard_owns_home`. Proposed: **yes,
   with that guard**, since `install.sh launcher` already asks the person for it.
8. **The «تحليل» model on the server**: `claude-haiku-5-5`, key as the Script Property `ANTHROPIC_API_KEY` in its own Console workspace with a
   spend limit, **`AI_DAILY_USD` = 1**, 3 analyses per install in 6 h.
9. Free text in the report: **none**.
10. The city: **only with the person's yes**, off by default.
11. The triage's summarising model in Actions (secret `ANTHROPIC_API_KEY` in `tshi-dev/urtv-reports`, `AI_MONTHLY_USD` = 5): **yes**, or rules only.
12. A 512 KB speed-test file in `tshi-dev/urtv-data`: **yes**.

## Mockups (1920×1080)

| File | What it shows |
|---|---|
| [mockups/01-start.png](mockups/01-start.png) | start: what it checks, what it found, «ابدأ الفحص» |
| [mockups/02-keys.png](mockups/02-keys.png) | the key test |
| [mockups/03-results.png](mockups/03-results.png) | results: a status per class, a safe tip, «انحلّت في الإصدار X» |
| [mockups/04-consent.png](mockups/04-consent.png) | consent: what is sent, the honest line, «إرسال» / «لا ترسل» |
| [mockups/05-sent.png](mockups/05-sent.png) | sent, thank you, with the code |
| [mockups/06-phone.png](mockups/06-phone.png) | the QR fallback |
| [mockups/07-analysis.png](mockups/07-analysis.png) | the «تحليل» panel and «الحل المقترح» |
| [mockups/08-home.png](mockups/08-home.png) | the HOME result |

Sources in `mockups/src/` (`node docs/doctor/mockups/src/render.js`). The rows, the code and the numbers are an example, not a real TV.
