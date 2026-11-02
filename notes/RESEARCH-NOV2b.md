# UrTV research, 2 Nov 2026 (b): English UI, Clean Your TV since fcde1ee, GitHub survey, "close all apps"

Read-only. Code: `origin/next` at 84da7f6 (2 Nov, "Refresh the page build stamp after the 4.2 merges"), unpacked with
`git archive` into `scratchpad/work/rs-next/`. Clean Your TV: `scratchpad/ext-nov2/cleanyourtv`, pulled to 3cd274f8 (2 Nov).
Owner reports: `scratchpad/in-nov2/responses.csv` (546 rows), `scratchpad/in-nov2/owner-items.md`.

**Two earlier "NO" decisions are now in question.** `work/DECISIONS-NOV2.md` §3 marked "English interface" as **NO** (the
owner's rule: Arabic UI) and "Free RAM button" as **NO** ("removed in 4.0, no effect on Android 14"). The owner now asks
for both. Sections 1 and 4 give him what he needs to decide again.

---

## 1. English UI: a plan

### 1.1 How many Arabic UI strings there are (rough grep counts)

These counts are lines with a quoted string that contains Arabic (`"…[؀-ۿ]…"`), unless the row says otherwise.

| Area | Count | Notes |
|---|---|---|
| Launcher Java (`android/launcher/src/main/java`, 167 files) | **~550 lines, ~528 unique strings** | Top files: MainActivity 64, setup/SetupEngine 52, tilawah/Adhan 43 (some are muezzin and station names, which are data), ClockZone 35 (city names, data), TasksActivity 30, ParentalActivity 28, PhoneActivity 26, setup/SetupActivity 22, GreetingText 21, ControlText 21, phone/UpdateText 18, FamilyDns(+Activity) 30, ControlCenter 15, DockActivity 13 |
| Launcher XML | **58 `<string>` entries** in `res/values/strings.xml` and **18** in `app_names.xml` | The only strings in `res/`. There is no `values-en` yet. `getString(R.string…)` is used 41 times. Everything else is a Java literal. |
| Screensaver Java (`android/app/src/main/java`, 44 files) | **~1,295 lines, but ~400 are UI** | Cities.java has 896 lines of city and country names (data; world entries already carry English names). The UI is in SettingsActivity 205, Prefs 67 (labels), PrayerMethod 37, SettingsHub 27, Arabic 18 (month and day names), Updater 15, TeamSearch 11, plus about 15 in other files. `res/values/strings.xml` has one string. |
| Page (`src/template.html` → `dist/tv.html`) | **~461 unique quoted Arabic strings + ~41 HTML text nodes; ~10.8 KB of Arabic letters (UTF-8)** | 348 template lines contain Arabic. The verses, hadith and adhkar (the bulk of the 675 KB) are content and stay Arabic. |
| Phone page (`android/launcher/src/main/assets/phone/index.html`) | **~198 quoted + ~68 HTML text strings; ~9.5 KB of Arabic letters** | `<html lang="ar" dir="rtl">`. 65,283 bytes against a 65,536-byte test limit (`tests/phone_page.test.mjs:745`), so only **253 bytes** are left. |
| `optimize/` scripts (47 .sh/.py) | **322 lines with Arabic; ~53 are echo/print lines** | Mostly «للشخص:» lines the agent passes on to the person, plus Arabic names in examples and matching (cities, teams). The rest of the output is already English, because it is meant for the agent. |
| Tests | **169 of 271 test files contain Arabic (≈7,200 lines)** | Many tests pin exact Arabic literals in the Java sources. This matters for the approach (§1.3). |

The page budget is `tests/check.py:44`, `("tv.html", 680_000)`. The page is 675,318 bytes, so about **4.7 KB** is left.

### 1.2 What we already have that helps

- **The «الأرقام» pattern.** It is the right model for a language setting. The screensaver holds the setting
  (`Prefs.digits()`, values `hindi` or `latin`). The page gets it as `#digits=` (`Prefs.java:498`). The launcher reads it
  through `ConfigProvider` → `ConfigSnapshot` (`home/Digits.java`) and keeps a copy for screens opened on their own.
  `DigitsText.convert()` converts both ways. A «اللغة» setting can travel the same three roads with no new plumbing.
- **Both manifests set `android:supportsRtl="true"`.** Layouts are built in code (only 9 XML layouts). The code
  forces RTL in places: 58 `Gravity.RIGHT`, 21 `setLayoutDirection`, 16 `LAYOUT_DIRECTION_RTL` and 12 `TEXT_DIRECTION`,
  against 7 `Gravity.START` and 2 `Gravity.END`. The D-pad left/right swap has its own test (`tests/dpad_rtl_test.py`).
- **Clean Your TV's launcher ships 30 `values-xx` folders**, `values-ar` among them. tellyscope ships 6. Both use plain
  Android resources. That is the standard route, but both projects started that way, and we did not.

### 1.3 Recommended approach

**A per-app «اللغة» setting (العربية / English), with Arabic as the default.** Do not follow the TV's system locale.
Our users' TVs are often set to English while the household wants Arabic, so following the locale would flip the UI
for most current users the day the update lands. Per-app locales (`LocaleManager`) exist only from Android 13, and
most of our fleet is on 9 to 12 (see §4.3). We have no AppCompat dependency. The setting is therefore applied by hand:
each Activity's `attachBaseContext` wraps the context with `createConfigurationContext(config.setLocale(...))`. This
works from API 17, and both apps' minSdk values (21 and 26) are above that.

**Strings: one translation table keyed by the Arabic text (gettext style).** Do not move 900 literals into
`strings.xml` ids.
- In Java, `S.t("المفضلة لا تقل عن %s تطبيقات", n)` returns the Arabic as it is today, or the English entry when
  «اللغة» is `en`. The Arabic literal stays in the source, so the ~7,200 test lines that pin Arabic keep passing and
  each diff is one wrapper per literal.
- One table, `i18n/en.tsv` (Arabic → English, with `%s` placeholders), feeds all four consumers:
  - the two APKs, copied into assets at build time;
  - `src/build.py` for the page;
  - a generator for the phone page.
- A new test pulls every wrapped literal out of the code, then fails on any literal without an English entry and on
  any entry whose placeholders differ.
- The 58 + 18 + 1 XML strings get a real `values-en/` in addition. The system shows app labels and banners from
  these, so they have to be standard resources.

**Layout mirroring (RTL → LTR).**
- Replace hard `Gravity.RIGHT` and `LAYOUT_DIRECTION_RTL` with `START` and `END`, and let one `Ui.dir(context)` decide
  the direction.
- Keep the Arabic path identical. Each converted screen gets a screenshot or test at both directions.
- The D-pad swap (`dpad_rtl_test`) must follow the direction, not a constant.
- The clock, digits and Hijri lines are already direction-neutral.

**The page (`dist/tv.html`): build two files from one template.**
- `src/build.py` writes `dist/tv.html` (Arabic, unchanged) and `dist/tv-en.html` (`lang="en" dir="ltr"`), substituting
  the table at build time.
- Each file keeps its own 680 KB budget. The English one will be about the same size or smaller, since ASCII uses
  1 byte per letter and Arabic uses 2.
- There is no runtime string table and no extra download on Arabic TVs.
- `Updater` and `PageStamp` pick the file that matches «اللغة» (`Updater.java:17,28`), and `Prefs.java:477` uses the
  matching asset.
- Putting both languages in one file does not fit: about 9 KB of English plus keys against 4.7 KB of headroom.
- Content stays Arabic: Quran verses (always), hadith and adhkar (no checked English source; adding one is a separate
  content project). Hijri months are transliterated (Muharram…). Team and city names use the English names we already
  have (Cities world entries, `teams-cache.json`); for Gulf entries that only have Arabic names, check before Phase 3
  (Saudi first).

**The phone page: same idea.**
- A generator (or `build.py`) writes `assets/phone/index.en.html` from `index.html` and the table.
- `PhoneHost.page()` (`PhoneHost.java:57`) serves the one that matches «اللغة».
- The 64 KB test applies to each file. Arabic keeps its 253 bytes of room and the English file has more.
- A small «English / العربية» link on the phone page can set the TV's setting through the existing API.
- The alternative, fetching a JSON table at runtime, adds a request and a flash of Arabic, so it is not recommended.

**Digits and dates.**
- «الأرقام» stays a separate setting.
- When «اللغة» is English, default digits to `latin` and Gregorian dates to English month names. The Hijri date is
  shown as "12 Jumada al-Awwal 1448".
- The «الأرقام» row should then read "Digits: 123 / ١٢٣". Do not hide it: some Arabic users want latin digits, and an
  English user may want Arabic-Indic digits.
- `DigitsText` needs no change.

**`optimize/`.** Low priority. The agent already talks to the person in the person's language
(`docs/SETUP-PROMPT.md:27`). Add one rule: "«للشخص» lines are Arabic; translate them if the person speaks English".
A `saver.sh lang en|ar` command sets the setting, the same way `digits` is set today.

**Fonts.** Thmanyah (private, CI-only) and Noto Naskh are Arabic-first. Check whether Thmanyah's Latin glyphs are good
enough at TV sizes. If not, add an OFL Latin face for `en` only (it adds about 100 KB per APK; the page uses the
system font).

### 1.4 Tests

1. Table completeness: every `S.t(...)` literal and every page and phone literal has an entry with the same
   placeholders.
2. `values-en/strings.xml` has the same keys as `values/strings.xml`.
3. Page: `tv.html` and `tv-en.html` each stay under budget; the English file has `dir="ltr"` and no stray Arabic
   outside the content blocks (verses, hadith, names).
4. Phone: both files stay under 64 KB with the same CSP and nonce checks.
5. A direction test per converted screen (pure Java layout math, as `ControlLayout` is tested today).
6. One emulator or TV screenshot pass in each language before each phase ships.

### 1.5 Effort, risks, order

| Phase | What ships | Size | Risk |
|---|---|---|---|
| 0 | Plumbing only: «اللغة» in Prefs (default `ar`), through ConfigProvider, `#lang=`, phone API; `S.t()` and the table with zero entries; the tests above. Nothing visible changes. | S (1 day) | none |
| 1 | Screensaver settings in English (~400 strings, mirrored). This is where the setting lives, so it comes first. | M (2–3 days) | the settings screen is dense; English labels are wider in Latin fonts → DialogFit checks |
| 2 | Page `tv-en.html` (build two files; Updater picks). | M (2–3 days) | the Updater and PageStamp change touches the update path; test the fallback to the asset |
| 3 | Launcher home, Control Center, tasks, tilawah, parental (~550 strings, 58 Gravity.RIGHT). | L (4–6 days) | **RTL regressions for the Arabic majority**; D-pad direction; vendor-key paths untouched |
| 4 | Phone page `index.en.html`. | S–M (1–2 days) | 64 KB limit on the Arabic file (253 bytes left: any added switcher must come from the generator, not hand edits) |
| 5 | Setup wizard on the TV (SetupEngine/SetupActivity), `optimize/` rule, docs. | M | the setup is the riskiest flow; do it last |

The total is about **2.5–3.5 weeks** of agent work, plus the owner's (or a native speaker's) review of the English
wording.

Main risks:
1. Mirroring mistakes in the Arabic UI. Mitigate: Arabic stays the default path, and screens are converted one at a
   time.
2. The page and phone budgets. Mitigate: generate one file per language.
3. Translation drift. Mitigate: the completeness test fails the build.
4. The owner's own rule "Arabic UI" in `DECISIONS-NOV2.md`. He has to lift it explicitly.

---

## 2. Clean Your TV since fcde1ee

Repo: https://github.com/Gathin23/cleanyourtv (MIT). From **fcde1ee (29 Oct) to 3cd274f8 (2 Nov): 991 commits**, with
735 files changed. Most of the change is device lists: 44% `examples/` and 4% `docs/devices`. The launcher is 9%,
tools 4% and tests 12%. `DECISIONS-NOV2.md` §4 covered fcde1ee..6e8615eb (826 commits). **165 commits are new since
then.**

**The biggest single item is not a commit: our community lists are stale.** Every one of our 254
`optimize/lists/community/*.txt` files says "commit 6cad2be3, taken 2026-10-23".
- Upstream now has **416 example folders against 253 at 6cad2be3**: 164 new TVs, 57 of them since fcde1ee.
- 294 debloat files changed, +15.7k lines.
- Re-running `.github/tools/community_lists.py` at 3cd274f8 brings all of it in. It goes through our existing
  `keep.txt` and `core.txt` filters and model matching.
- **Safe**, with one caveat: review the diff for packages that newer upstream notes now *keep*. Examples: partnercustomizer
  on Xiaomi, `com.arris.oemdata`, Vewd/AirPlay on Sony, TCL `t_solo`/`gallery` on AR09 (below).

Status key: **have** = in origin/next; **partly**; **no** = missing. Safe = low risk to take, as idea or code with an MIT
credit.

### 2.1 Package lists and guards (stability)

| Commit | What | Us | Safe? |
|---|---|---|---|
| bf505124 | `com.android.sdksandbox` in the core guard | **have** (`core.txt`, 4.2 merge 7990b69) | – |
| f5c37c36 | `com.android.location.fused` in the core guard (a TCL S5400A boot-looped) | **have** in `core.txt` (also still in `keep.txt`, harmless) | – |
| b8fcbabe, older 66e83196 | **Input-driver guard**: before disabling, read `dumpsys package`; a package holding `TV_INPUT_HARDWARE`, or exporting a `TvInputService`, is skipped (fail closed when the dump is unreadable). From SDK 23, a package that requests no permissions passes. | **no**: our `optimize/disable.sh` checks `keep.txt` and `core.txt` only | **Yes, take it.** This is the guard that would have caught the AR09 case below |
| 5326ab19 (+ the HUNG case at HEAD, `tools/disable.sh`) | When `--batch` dies mid-list, still read back what is disabled and fail the run. "Service dumps disabled due to hung system process" means skip and reboot, not pass. | **partly**: we write the ledger before the batch and read `pm list packages -d` after, but do not fail on `BATCH_RC` | Yes (small) |
| 9dc4a95e (1 Nov) | **TCL 65P735, Kuwait, G03 AR11, 2 GB, 32-bit** (a Gulf unit like ours): eight days stable. Keycodes Netflix 4062, YouTube 4065, Prime 4099, Guard 4140, gear 4077. **`com.tcl.guard` (TCL's memory cleaner) and the `APP_AUTO_START` appop killed a VPN in the background.** `dreamx` off leaves `screensaver_components` pointing at it. | Keys: **have** (RemoteKeys 4062/4065/4099, SettingsKey 4077); 4140 no. `com.tcl.guard` is in `tcl-optional.txt`. `APP_AUTO_START`: **have** (`optimize/autostart.sh`). dreamx: we set our screensaver first. | Add 4140 → nothing or our settings (opt.). Consider moving `com.tcl.guard` to the safe list, since it kills background apps such as our adhan service (needs one TV test) |
| (G03 notes, 50S546 AR09) | `com.tcl.gallery`, `com.tcl.ui_mediaCenter` and `com.tcl.t_solo` **hold TV_INPUT_HARDWARE on AR09**; they are safe only on AR11 | `t_solo` in `tcl-optional.txt`; gallery and mediaCenter not in our lists | Keep them out of any list until the input-driver guard exists |
| 3cd274f8 (2 Nov) | Xiaomi `partnercustomizer` answers the remote's Netflix, Prime and Input buttons and BT pairing | Xiaomi only via community lists | Re-import; consider adding it to `keep.txt` for Xiaomi |
| (G03 note) | `com.tcl.partnercustomizer` "stays enabled (remote shortcut keys)" on G03; 16+ other TCL lists disable it | **we disable it** in `tcl-safe.txt:10` | Our home answers the vendor keys itself, so this is probably fine. Check on a G03 with the star or Live TV key |
| (G03 note) | `com.tcl.xian.StartandroidService`: "TCL's updater needs it", so it is commented out in the shared list | **we disable it** in `tcl-safe.txt:23` | Check the firmware update on one TV; move it to optional if updates break |
| 9c006662 | Remove `com.arris.oemdata` (crash-loops `remoteservice`) | only via the re-import | Yes |
| cfc85c55 | Sony UR2: keep Vewd or AirPlay crash-loops | via the re-import | Yes |
| 9e95d68a | OnePlus "HDMI No Signal" ← `com.heytap.tv.support` | via the re-import | Yes |
| 75ade88e (1 Nov) | Chromecast 4K keep-cast list; the Ambient screensaver pair can go once the launcher's clock is the screensaver | n/a (we replace dreamx on TCL) | Idea |
| 7ae565ce (2 Nov) | Skyworth: the key service crashes when launcherx is disabled **and a second user exists** | no check | Add to `install.sh`: if `pm list users` shows >1 user, keep launcherx (S) |
| f7752aa7 (2 Nov) | Google Home app loses control of a Streamer whose `setupwraith` is disabled | `setupwraith` is in our `keep.txt` | – |
| ef88f909 | Ambient album picker needs launcherx and setupwraith | – | – |
| 040b5ac3 (1 Nov) | A TV that answers ping but has no ADB port: how to tell it from a network fault | our wizard has Wi-Fi rejoin | Doc idea for SETUP-PROMPT |

### 2.2 Launcher features and key handling

| Commit | What | Us | Safe? |
|---|---|---|---|
| c63d36a1 | `back_opens_clock`: Back on Home starts the screensaver, off by default | **no** (DECISIONS: "DO now"; not in origin/next) | Yes |
| e525f084, 8b1caf89, 93a36965 | Vendor keys ≥1000 reach only the focused window, so Home runs "hold:" entries and releases holds on pause | **have** (`RemoteKeys.java:26–85`, held code/scan, released on pause) | – |
| 8e8aed0c | `app_keys` action word "library" | n/a (we have fixed key actions) | Idea for "map a remote key" (DECISIONS: later) |
| 22e5884f, cded063f | `input_action` may name an activity (Redmi `ExternalSourceActivity`); the Input key starts `VIEW_INPUTS` | **no** (our «المصادر» uses our own list) | Yes, as a fallback when our list is empty |
| fefba107 (1 Nov) | `inputs_cec_state_only`: some TVs (TCL UnionTV) report HDMI port state on the wrong port; trust the CEC state only | **no** | Idea; only if a user reports wrong "connected" marks |
| 8725bd02 (1 Nov) | Memory figure on kernels without `MemAvailable`: fall back to MemFree+Buffers+Cached | **no**: `optimize/measure.sh:93–100` treats 0 as "misread" | Yes, small; also useful for the §4 tile |
| 7f402323, 9065bd91 | `open_on_start_on_wake`, and the "Open on wake" add-on | no | Not needed (our home is the start app) |
| 6a115f41 | `clock_format` 12/24 h, whatever the TV says | **have** (24-hour clock setting) | – |
| e62aa10a | Launcher 3.3: hold OK on a tile → Move / Remove / App info, on by default | partly (our FavEdit) | – |
| 126ba69e, 3fa7546a | HOME filter also lists `COMMON_HOME` (CVTE firmware) and declares `MODERN_HOME` (ColorOS TV) | **no** (HOME only, manifest lines 72/122) | Yes, if a CVTE or OnePlus user reports Home not taking |
| a88188ba, 15a04e51 (1 Nov) | With launcherx redirected, app buttons need its SYSTEM_ALERT_WINDOW appop; TCL G10 app buttons go through launcherx, and a log reader brings them back | n/a: we disable launcherx and answer the keys ourselves | Note for the redirect mode below |
| 672195d0 | HyperOS 3: after a screen-off, user apps are force-stopped after 2 s | – | Relevant to the adhan service on Xiaomi: note in docs/TVS.md |
| 5f85e645 | Skyworth/Coocaa cold boot dispatches via `persist.service.model.*` (`modelhome`) | – | Model for the Changhong "boots into Live TV" item (backlog) |
| (README, G09 55P7L, examples/tcl-g09-google-tv) | **Redirect home mode**: keep launcherx and setupwraith enabled; an accessibility `HomeKeyService` sends the stock home back to ours (`stock_home_redirect`) plus `home_long_press_home` and `power_on_home_always`. Used where `set-home-activity` is a no-op or the vendor keys need launcherx. Trade-off: volume repeat becomes step-wise with the key filter. | **no** (we disable the stock home and set ours as Home) | **Not now.** It is a fallback for TVs where our install fails; it needs an always-on accessibility service (we already run two, A11yHeal), and 2 of 5 cold boots stayed on Google home on one Sony |

### 2.3 Tools (speed and evidence)

- `tools/bench.py`: key-to-frame latency, jank from `dumpsys gfxinfo framestats`, cold start (`am start -W`), return to
  Home, idle CPU, the launcher's PSS. **Idea worth taking** for our before/after report, instead of "free RAM" alone
  (§4).
- `tools/flamechart.py` and `trace.py` (Perfetto): deep profiling; not needed.
- Exit-code hygiene (many "answers 2 when it never reached the TV" commits): we already distinguish these.

---

## 3. GitHub survey: stabilise and optimise Android TV

| Project | Licence / status | What we could adopt | Risk |
|---|---|---|---|
| **Clean Your TV** (Gathin23/cleanyourtv) | MIT, very active (≈1,000 commits in 4 days) | Lists (re-import, §2), the input-driver guard, `bench.py`'s measurements, the MemAvailable fallback, the core guard regex | AI-written notes; "tested the same day" claims; always our keep/core filters |
| **UAD-ng** (Universal-Debloater-Alliance/universal-android-debloater-next-generation) | GPL-3.0, active (last push 27 Oct) | `uad_lists.json` (5,382 entries) ratings as a **second opinion only** (README already credits it). TV coverage is thin: TCL 10 Recommended / 25 Expert, MediaTek 32/16/8 Unsafe, Sony 9, Google TV 5. It agrees with us on `sdksandbox` = Unsafe (bootloop), `location.fused` = Expert, launcherx/setupwraith = Expert (it notes setupwraith breaks volume on Sony), dreamx = Recommended, `com.tcl.guard` = Expert | GPL: use ratings as data and checks, don't copy descriptions verbatim into our repo |
| **tellyscope** (man15h/tellyscope) | GPL-3.0, v0.14.0 (30 Oct) | Ideas only: "What's new" per version, Download-APK in the update dialog, "Who has access" (revoke ADB keys). Its tweaks are just animations 0.5× + Private DNS, the same as ours. 6 `values-xx` locales: a model for §1 | none (ideas) |
| **TvToolsPro** (deekshith0509/TvToolsPro) | **no licence file** (all rights reserved), last commit Aug | Ideas only: keycode sender and macros (learn a remote's codes), bulk force-stop **over ADB from a phone** | Do not copy code |
| **Key Mapper** (keymapperorg/KeyMapper) | GPL-3.0, active (31 Oct) | Ideas for "map every remote key" (DECISIONS: later): accessibility key capture, per-key triggers, "while app X is in front" constraints. Proves Back/Home can be left untouched by design | GPL: ideas only; an accessibility key filter breaks held-volume repeat on TCL G09 (CYT notes) |
| **Projectivy Launcher** | closed source (free + paid) | Idea: "override stock launcher" by accessibility = the same as CYT's redirect mode | n/a |
| **tv-home** (mihirgupta0900/tv-home) | our launcher's origin (`android/launcher/LICENSE-tv-home`) | Its measured claim: 2 GB TCL Google TV, 25 apps disabled, home PSS 192 → 83 MB. A useful figure: the measurable gain is the *home's* memory, not "free RAM" | – |
| **Free My TV** (simbasjungles/free-my-tv) | MIT, already used (`setup/AdbClient.java`) | – | – |
| tv-vibehack, cleanmytv, android-tv-debloat-toolkit | forks/companions of CYT lists; the toolkit says it is unmaintained | nothing beyond CYT | – |
| **ATV Tools** | no maintained public repo found | – | – |

**Evidence-based, reversible tweaks** (with what we already do):

1. **Animation scales 0.5** (window, transition, animator): **have** (`optimize/tweaks.sh`, journaled restore). This is
   the one tweak every tool agrees on, and it is perceived speed, not memory.
2. **Standby bucket and background appops for heavy autostarters**: **have** for the Play Store (restricted bucket on
   SDK ≥30; measured idle CPU 31% → 6%). Possible extension: the same for 2–3 measured TCL autostarters
   (`com.tcl.tv.tclhome_passive` is disabled already). Only with before/after CPU numbers.
3. **`max_cached_processes`**: we have it opt-in (`CACHED_LIMIT=16`), off by default, and that is right. Android 14
   raised the limit on purpose (up to 30% fewer cold starts, AOSP cached-apps-freezer doc), and on a TCL A14 the
   device_config value did not survive a reboot.
4. **Cached apps freezer**: we leave it at the TV's value (one report linked it to hangs after standby). CYT's 25 Oct
   finding: on A14 the freezer freezes Cast (`mediashell`) once launcherx is off. The fix was for Home to bind
   `CastReceiverService`. Worth a check on our TCL A14 units if Cast drops.
5. **Developer option "background process limit"** (`ActivityManager.setProcessLimit`): not persistent (reset at
   reboot) and placebo-ish. **Skip.**
6. **NEW, worth testing: AOT-compile our own apps after install.** Run `cmd package compile -m speed-profile -f
   sa.urtv.home` (and the screensaver). A sideloaded APK runs JIT until the idle dexopt job runs. This is measurable
   with `am start -W` (cold start) and reversible (`cmd package compile --reset`). Size: S, in `install.sh`, with
   before/after numbers in the report.
7. **NEW, safety: the input-driver guard** (§2.1) before any disable.
8. **`pm trim-caches`** only when storage is low: **have**.
9. **LMK tuning (`ro.lmk.*`), zram size, swappiness**: need root or are read-only on retail TVs. **Skip.**

---

## 4. A "close all apps / speed up" button

### 4.1 Our history

- **2c0bee6 / 3f92d53 (27 Oct)**, "Control Center («مركز التحكم»)": added the tile **«حرّر الذاكرة»** with
  `KILL_BACKGROUND_PROCESSES` (a normal permission, no prompt).
  - It called `ActivityManager.killBackgroundProcesses(pkg)` for each launchable app the person installed. It never
    touched ours (home, screensaver, adhan), a system app or a keyboard (`ControlText.killable`).
  - It slept 1.2 s, compared `MemoryInfo.availMem` before and after, and showed «تحرر N ميجابايت», «لم يتحرر شيء:
    الذاكرة مرتاحة», or «لا تطبيقات في الخلفية لإغلاقها».
- **The tester, step 6 (f564488, 28 Oct), on a TCL Android 14**: «حرّر الذاكرة» "says nothing was freed («الذاكرة
  مرتاحة») while MemAvailable was about 570–620 MB of 2.4 GB and swap nearly full".
- **018964e / aca6a1c (28 Oct)**, "drop «حرّر الذاكرة»": the tile, the permission, its text rules and tests were
  removed, "nothing useful on Android 14". The settings doc now says: "there is no «حرّر الذاكرة» tile: Android 14 gives a
  normal app no useful way to free other apps' memory". That is why it was left out of 4.0's notes.
- `DECISIONS-NOV2.md`: "Free RAM button (row 536): NO".

### 4.2 What a normal app can really do

| Means | Who can | What it does | On Android 14 |
|---|---|---|---|
| `ActivityManager.killBackgroundProcesses(pkg)` + `KILL_BACKGROUND_PROCESSES` (normal permission) | any app | Kills that package's processes that are already in the background (cached or empty, not visible, not foreground services). The system may restart sticky services at once. | **Only the caller's own processes**: a behaviour change for *all* apps in Android 14 (other packages are ignored with "Invalid packageName" in logcat). Our launcher's targetSdk 30 does not help. This is exactly what the tester saw. |
| `am kill-all` / `am kill <pkg>` | **shell (adb) only** | Kills all background processes that are safe to kill | Works from adb. An app could run it only through our in-app ADB client (`setup/AdbClient.java`, used by the on-TV setup), which needs network/wireless debugging left on and authorised. That is a security cost we should not take for a daily button. |
| `am force-stop` | shell, or system apps with `FORCE_STOP_PACKAGES` (signature) | Stops everything, cancels alarms and jobs | **Not possible for a normal app.** Even from adb it is harmful: it cancels the app's alarms (an adhan app would go silent). |
| Close the foreground app | – | An app cannot close another app's activity. Pressing Home does what is possible: it sends the app to the background. | – |

### 4.3 Does it measurably help? Our own numbers

From `responses.csv`: **449 setup reports with before/after memory** (`optimize/measure.sh`: MemAvailable, swap,
process count). The "after" state includes the debloat, tweaks and our home replacing the stock one, which is a far
bigger change than one button.

- Median TV: **1,990 MB RAM, 796 MB free before** (lowest 171 MB). 345 of 449 TVs have under 2.2 GB.
- **Free RAM change after the whole setup: median +15 MB.** 268 TVs went up and 181 went down. Process count: median
  −2. Swap: median −32 MB.
- By Android version (median change):

  | Android | SDK | TVs | Median change |
  |---|---|---|---|
  | 9 | 28 | 66 | +31 MB |
  | 10 | 29 | 26 | +22 MB |
  | 11 | 30 | 184 | +1.5 MB |
  | 12 | 31 | 55 | +33 MB |
  | 14 | 34 | 112 | +8 MB |

- **Noise from merely starting the screensaver** (the "before-screensaver" minus "before" measurement, nothing else
  changed): **median −72 MB, 10th percentile −128 MB, 90th percentile +3 MB.** The run-to-run swing is several times
  larger than anything a kill can show.
- Owner item 1 (G7max, Android 11): free memory 2527 → 2385 MB after setup, 2266 MB with the screensaver. Still
  plenty free.

Reading:
1. Disabling ~16 packages, which stops their processes for good, moves MemAvailable by only tens of MB, because the
   kernel and LMKD refill memory with cache and cached apps at once. A kill that the system undoes within seconds will
   move it less.
2. Cached processes cost no CPU when frozen (freezer on A14; mostly idle elsewhere), and LMKD already kills them under
   pressure before anything visible slows.
3. **What does make a TV slow is foreground load:** a heavy home (tv-home's 192 → 83 MB), autostarting vendor services
   (the Play Store's 31% → 6% idle CPU), swap thrash on 1.5–2 GB sets. The debloat and tweaks already target exactly
   those.
4. A "boost" button would mostly show a random number, and on Android 14 (25% of our fleet) it can do nothing at all.

### 4.4 Recommendation

**Do not bring back a "speed up" or "close all apps" button that claims speed.** That would be the placebo the owner's
"no slop" rule forbids. If the owner still wants something in «مركز التحكم», this is the honest version:

**Tile «تفريغ الذاكرة» (Android 9–13 only; hidden on 14+)**
- **Kills:** only cached background apps that the person installed and that have a launcher entry (the old
  `ControlText.killable` rule).
- **Never kills:**
  - ours (`sa.urtv.*`: home, screensaver, adhan/MusicService);
  - the app holding audio focus or a media session (a radio playing behind the home);
  - any app with an enabled accessibility service or an input method;
  - any system app or `FLAG_UPDATED_SYSTEM_APP`;
  - anything in `keep.txt` or `core.txt`.
- **Shows real numbers:** MemAvailable before and 2 s after (with the MemFree+Buffers+Cached fallback, CYT 8725bd02),
  e.g. «أُغلق ٤ تطبيقات في الخلفية · الذاكرة المتاحة: ٧٢٠ ← ٧٨٥ ميجابايت». It says «قد يعيد النظام تشغيل بعضها» when the
  number falls back.
- **Never** says «تسريع».
- **On Android 14+:** the tile is absent, or shows only the memory figure and a line «أندرويد ١٤ يدير الذاكرة بنفسه».
- **Locks:** under `@center`, as before.

**Size:** S (half the code is in 018964e's diff, reversible with `git revert` of the removal minus the A14 path).
**Risk:** low with the exclusions above. The only real risk is the tile overpromising.

**Better use of the same effort, which actually speeds TVs up:**
1. A one-line "health" figure in the tile: free memory, swap used and uptime. If swap is nearly full and uptime is
   days, the honest advice is «أعد تشغيل التلفزيون» (a reboot is the one thing that measurably clears swap thrash on
   1.5–2 GB sets).
2. `cmd package compile -m speed-profile` for our two apps at install (§3, item 6).
3. The `bench.py`-style cold-start and key-latency numbers in the setup report, so "faster" is measured, not felt.

---

### Sources
- Clean Your TV: https://github.com/Gathin23/cleanyourtv (local clone at 3cd274f8); commits as linked above
  (`https://github.com/Gathin23/cleanyourtv/commit/<hash>`).
- UAD-ng: https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation
  (`resources/assets/uad_lists.json`).
- tellyscope: https://github.com/man15h/tellyscope · TvToolsPro: https://github.com/deekshith0509/TvToolsPro ·
  Key Mapper: https://github.com/keymapperorg/KeyMapper
- Search results: https://github.com/mihirgupta0900/tv-home/wiki, https://github.com/Gathin23/tv-vibehack/wiki,
  https://github.com/wingedpanther/cleanmytv/wiki, https://github.com/seun-novodev/android-tv-debloat-toolkit,
  https://www.digitec.ch/en/page/a-tv-home-screen-without-ads-projectivy-launcher-spices-up-android-and-google-tv-38294
- Android 14 behaviour change (killBackgroundProcesses limited to the caller's own package):
  https://developer.android.com/about/versions/14/behavior-changes-all · cached-apps freezer:
  https://source.android.com/docs/core/perf/cached-apps-freezer
- Our history: commits 2c0bee6, f564488, 018964e; `scratchpad/work/DECISIONS-NOV2.md`; the memory parser is
  `scratchpad/work/rs-tools/mem.py`.
