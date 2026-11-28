# Open-source Android TV launchers and screensavers on GitHub (research, 2026-11)

What the best-known open-source TV homes and screensavers do about the problems UrTV has: the home not sticking, slowness on
1–1.5 GB TVs, the remote, and the screensaver lost after a reboot. Read from public code only (shallow clones outside this repo);
nothing was copied. Star counts are from the GitHub search API on 2026-11-28.

**Licence rule for us:** GPL-3.0 projects give **ideas only**, never code. MIT and Apache-2.0 code could be used with attribution
(MIT: keep the copyright line; Apache-2.0: keep the NOTICE), but every item below is written as an idea to build our own way.

**Not on GitHub as source:** Projectivy Launcher, ATV Launcher, Wolf Launcher and Google's own TV launchers are closed source.
LeanbackOnFire's repo is archived and points to `tsynik/LeanbackLauncher` (an AOSP Leanback launcher port, no LICENSE file: treat
as unknown, ideas only). FLauncher's upstream lives on GitLab (GPL-3.0); its GitHub forks are listed. `docs/ui/SIMPLE.md`, named
in the brief, does not exist in this branch; README.md and docs/HOMEDOCTOR.md were the baseline.

## 1. Ranked table

| # | Project | Stars | Last push | Licence | What it is |
|---|---|---|---|---|---|
| 1 | Gathin23/cleanyourtv | 61.6k | 2026-11-28 | MIT | ADB debloat + Apple-TV-style home (`launcher/`) + ~40 add-on apps, 15+ of them DreamService screensavers |
| 2 | pengchujin/aerial-tv | 13.5k | 2026-11-27 | MIT | tvOS-style home for Chinese TVs (Xiaomi, TCL, Hisense, Coocaa…), "takes over at boot"; own dream; Lite build for Android 4 |
| 3 | simbasjungles/free-my-tv | 6.7k | 2026-11-13 | MIT | Home + screensaver for Google TV, plus webOS/Tizen; one "Original ⇄ Free My TV" switch over the TV's own ADB |
| 4 | JackyAndroid/AndroidTVLauncher | 3.1k | 2022-11-14 | GPL-3.0 | Classic Leanback-style launcher (minSdk 17), Glide; dormant |
| 5 | humblemuzzu/dumbtv | 2.3k | 2026-11-25 | MIT (API says NOASSERTION; LICENSE is MIT) | Fire TV debloat + launcher, direct-boot aware |
| 6 | theothernt/AerialViews | 1.2k | 2026-11-25 | GPL-3.0 | The reference DreamService screensaver (Apple aerials, own media); Projectivy wallpaper provider |
| 7 | lesnolie/Tigris | 693 | 2026-11-13 | MIT | tvOS-27-style home; "guard" ported from aerial-tv; idle-timer take-over |
| 8 | techgenius1/tvOS-Launcher | 628 | 2026-11-22 | Apache-2.0 | Lightweight tvOS-style home, button mapper, local-ADB self-setup, in-app updater |
| 9 | nielsvanvelzen/tv-launcher | 502 | 2026-11-27 | GPL-3.0 | Minimal Compose for TV launcher (Jellyfin dev); Coil |
| 10 | leanbitlab-org/LtvLauncher | 495 | 2026-11-28 | GPL-3.0 | FLauncher (Flutter) fork with a screensaver, a11y service, IzzyOnDroid + Downloader code |
| – | aradhyas8/Plume_Launcher | 432 | 2026-11-27 | GPL-3.0 | Google TV home with Home-key override and a screensaver |
| – | osrosal/flauncher (FLauncher) | 317 | 2025-07-27 | GPL-3.0 | The well-known Flutter launcher; GitLab upstream |
| – | schorschii/FsClock-Android | 165 | 2026-11-17 | GPL-3.0 | Fullscreen clock DreamService; F-Droid |

## 2. Staying the default HOME

Everyone declares `MAIN` + `HOME` + `DEFAULT` on a `singleTask` activity, usually with `stateNotNeeded="true"`,
`clearTaskOnLaunch`, and (aerial-tv, free-my-tv, dumbtv) `resumeWhilePausing` so Home paints before the leaving app finishes
pausing. That is the easy part; the projects with many stars differ in what they do when the firmware ignores it.

- **Role / home activity by ADB at setup** (all of them, like our angles A/B): `cmd role add-role-holder` or
  `cmd package set-home-activity`, then verify with `resolve-activity`.
- **Never disable the stock launcher on Google TV** (free-my-tv, Plume, Tigris all say so): it boot-loops or leaves a black screen
  (Google TV's RecoveryActivity, Kalley/Huawei reports). Same conclusion as our couch-tv lesson.
- **An accessibility "guard" that watches window changes** (aerial-tv, Tigris, cleanyourtv, Plume): when the stock launcher's
  *home* activity (a per-launcher list of class names, not the whole package, so its dashboard/sign-in windows are left alone)
  comes to the front, bring our home back. Where a launcher raises no window event, aerial-tv falls back to UsageStats once a
  second. Plume catches `KEYCODE_HOME` in `onKeyEvent` first, so Google TV never flashes, and keeps the window cover as fallback.
- **Process revival through a notification listener that reads nothing** (aerial-tv `RevivalListener`, Tigris, cleanyourtv
  `GuardRevival`): Android re-binds an approved listener ~0.5 s after its process dies, which restarts the app after a
  firmware/standby kill; the app then re-enables its a11y service (needs WRITE_SECURE_SETTINGS). Approved once with
  `cmd notification allow_listener`.
- **A11y service survives force-stop and boot** (cleanyourtv `ServiceRepair`, our `A11yHeal`): re-write the enabled list as
  remove-then-add so Android clears the "crashed" mark. **cleanyourtv also marks its key service `directBootAware`**: Android 11
  skips a non-direct-boot a11y service while the user is locked, writes `accessibility_enabled=0`, and TCL G03 then never binds
  it after unlock. Prefs are read only after `ACTION_USER_UNLOCKED`.
- **Boot handling** (aerial-tv `BootReceiver`, worth reading as notes): handle both `LOCKED_BOOT_COMPLETED` and `BOOT_COMPLETED`
  in `goAsync()` on a thread (a slow receiver got the app reported as not responding); re-check what is in front a few times
  after boot (the stock launcher covers the home ~20 s into boot on some firmwares); but **leave the TV alone if it started on an
  HDMI/"startup source" input or an app the person opened**.
- **Own receivers re-enabled** (aerial-tv `restoreReceivers`): vendor "startup managers" (Skyworth, Vidda) set the app's own
  boot/config receivers to disabled; the owning app can clear that override itself, re-checked 5 s and 15 s after a wake.
- **Wake from standby**: Tencent/Xiaomi put their launcher in front while going to sleep with no window event; aerial-tv asks
  UsageStats on wake. It also closes a "choose home" chooser that some firmwares show after a wake.
- **Fire OS**: the Home key never reaches an a11y service, Amazon's launcher has HOME priority 950; free-my-tv refuses to switch
  there, Plume/dumbtv cover Amazon's home window instead.
- **Remotes whose Home sends `KEYCODE_ALL_APPS`** (Xiaomi sticks, Philips long-press): cleanyourtv's key service maps it to Home.
- **Per-model knowledge in the APK** (aerial-tv `DeviceProfile`, Tigris `StockHomeCatalog` with unit tests): which home classes
  each stock launcher has, how long to leave it at boot, whether to filter keys.

## 3. Remote and focus

- **Views/Leanback** (JackyAndroid, LeanbackLauncher): `BrowseFragment`/`HorizontalGridView` keep focus natively.
- **Compose for TV** (nielsvanvelzen): `focusRestorer()` on each row so returning to a row lands on the last card; an
  `autoFocus` modifier for the first frame. **Flutter** (FLauncher, LtvLauncher): `FocusTraversalGroup` per row, an explicit
  autofocus on the first card, and long-press as "hold Enter" to enter a move mode.
- **Hand-made grids** (cleanyourtv `GridNavigation`, aerial-tv `Pressable`): their own D-pad maths with tests, so focus never
  leaves the grid; Back from a sub-screen returns to the tile that opened it.
- **The cost of filtering keys** (Tigris `HomeKeyFilterPolicy`, Plume, cleanyourtv `key_filter`): once any a11y service sets
  `FLAG_REQUEST_FILTER_KEY_EVENTS`, TCL Google TV (Android 12) drops key auto-repeat, volume can run away (lost KEY_UP), gamepads
  double-fire, and some apps see a held key. They filter only while needed; on Android 12+ Xiaomi, Home is answered on pause
  instead. UrTV already toggles the flag at run time (ExtrasA11yService).
- **Long-press decided on release by hold time** (Plume `HomeOverride`, cleanyourtv `hold:` entries): because many remotes send
  no repeats, `getRepeatCount()`/`isLongPress()` never fire.
- **App-key mapping**: cleanyourtv's one table for both services: `keycode|KEYCODE_name|scan:N` → package, `package/.Activity`,
  `input:<id>` (an HDMI port), an action word (home, back, screensaver, settings…), `hold:` prefix for the long-press action.

## 4. Speed on 1–1.5 GB TVs

- **Low-end detection**: aerial-tv `DeviceClass`: `isLowRamDevice || totalMem < 1800 MB || SDK < 24 || cores <= 2`, logged in
  diagnostics. Tigris and cleanyourtv add `onTrimMemory` policies (drop wallpaper/blur caches at `RUNNING_LOW`).
- **Images**: the native-view homes decode with `inSampleSize` to the tile size and `RGB_565` for opaque art; AerialViews uses
  Coil 3; nielsvanvelzen Coil-Compose; the old ones Glide. UrTV already does the first two (Ram, LiteAuto, PhotoCache).
- **Baseline profiles**: AerialViews (`baselineprofile` module + `profileinstaller`) and aerial-tv (`profileinstaller`) ship
  ART profiles so the first launches after install/update are not interpreted. UrTV ships none.
- **A separate Lite build** (aerial-tv `lite/`, Android 4.0–4.4): same look, no Compose. Comparable to our Lite mode, but as an APK.
- **Measured black time**: aerial-tv records the ms of black between Home asking for the dream and its first frame (Coocaa
  cold-starting SystemUI: 1.2–2.7 s), in diagnostics.

## 5. The screensaver

- **Declaration** (all): a `DreamService` with `BIND_DREAM_SERVICE`, `android.service.dreams.DreamService` filter, and
  `dream` metadata pointing at a settings activity.
- **Making it the system's**: by ADB, `settings put secure screensaver_components <pkg>/<class>` (AerialViews' README is the
  canonical guide: Google TV 12+ has no UI to pick a third-party dream). AerialViews notes: **on Google TV the
  `screen_off_timeout` must be at least 360000 (6 min) or the dream never starts**; on TCL Google TV the app needs "Auto Launch"
  in Safety Guard or the dream cannot start from the background; Fire OS needs extra keys.
- **Keeping it after a reboot or update**: aerial-tv is the only one with a real answer: the home holds WRITE_SECURE_SETTINGS,
  remembers "we own the screensaver" (set by install, cleared by restore), and **re-writes `screensaver_components` at every start
  and from a `ContentObserver` on that key** whenever the firmware puts its own back (TCL MT5879 does it on every boot); not when
  the person chose "System". It also turns `screensaver_enabled` off itself when the person picks "Never" (a Haier dream came up
  over phone casting), and back on only if it was the one that turned it off.
- **Idle take-over** (Tigris `IdleTimerTakeover`, free-my-tv `Screensaver` + "ScreensaverAlone"): where dreams are broken, the
  home runs its own idle timer and starts its dream activity; it never invents vendor timeout keys.
- **Starting it now**: `com.android.systemui/.Somnambulator` (aerial-tv), the same as "Start now" in Settings.

## 6. Settings UX

- **One switch first** (free-my-tv): the first screen is "Original ⇄ Free My TV", everything else below. Undo is one press.
- **Control Center** (aerial-tv): a quick panel (inputs, brightness, sound, screensaver) separate from the full "Personalize"
  settings; the update notice lives there.
- **Long-press on a tile** (FLauncher, LtvLauncher, Plume): app actions (move, hide, info) in place; settings as a side panel
  with sections (apps, sections, wallpaper, screensaver, misc).
- **Agent-ready guides**: cleanyourtv, aerial-tv, free-my-tv, Tigris ship AGENTS.md/CLAUDE.md and an "adapt to my TV" prompt
  where an agent tests the TV and writes a report for the owner to read before it is posted (aerial-tv `adapt.md`), as our
  homedoctor does.

## 7. Updates and distribution

- **In-app updater** (aerial-tv, free-my-tv, Tigris, Plume, tvOS-Launcher): check a release feed, download on request,
  `PackageInstaller`; aerial-tv reads the tag from its own website (GitHub is slow in China), falls over to the next mirror when
  a download stalls (< N bytes in M s), checks the archive's signer matches the installed one before installing, and checks at
  most every 3 h (10 min when the panel is open). UrTV already has the signer check (MirrorInstall).
- **Downloader codes**: LtvLauncher (7259827), Plume and tvOS-Launcher mention Downloader; UrTV uses 9378345.
- **F-Droid / IzzyOnDroid**: FsClock (F-Droid, fastlane metadata), LtvLauncher (IzzyOnDroid), dumbtv (F-Droid mention).
  Play: FLauncher, AerialViews, FsClock, cleanyourtv add-ons.
- **Projectivy plugin API** (AerialViews `projectivyapi`): AerialViews is also a wallpaper provider for Projectivy.

## 8. Clever ideas we lack

1. The screensaver keeper (ContentObserver + start-up re-write, "we own it" flag, respects "System") — aerial-tv (MIT).
2. Notification-listener process revival — aerial-tv/Tigris/cleanyourtv (MIT).
3. `directBootAware` key service, prefs read after unlock — cleanyourtv (MIT).
4. Own receivers re-enabled after a vendor startup manager disabled them — aerial-tv (MIT).
5. Boot that respects an HDMI/startup source and an app the person opened; boot re-checks at fixed delays — aerial-tv (MIT).
6. Long-press decided on key-up by hold time — Plume (GPL: idea only), cleanyourtv (MIT).
7. `KEYCODE_ALL_APPS` → Home; `hold:` / `scan:` / `input:` app-key table — cleanyourtv (MIT).
8. Baseline profile for cold start — AerialViews (GPL: idea only; the technique is standard AndroidX).
9. Black-time measurement between Home and the dream's first frame — aerial-tv (MIT).
10. Per-model profile table in the APK with tests — aerial-tv/Tigris (MIT).

## What UrTV should adopt

At most ten, highest value first. Effort: S < 1 day, M a few days, L a week or more. All are ideas to write ourselves.

| # | What | Problem it solves | Effort | Fits |
|---|---|---|---|---|
| 1 | **Screensaver keeper** in the home (it holds WRITE_SECURE_SETTINGS): a pref "UrTV owns the screensaver" set by setup and cleared by reset/«تراجع»; at boot, `MY_PACKAGE_REPLACED` and home start, and from a `ContentObserver` on `screensaver_components`, put ours back if the firmware wrote its own; never when the person chose the TV's own. Log it under a UrTV tag for the report. | The screensaver lost after a reboot (TCL writes its own at boot) or a system update | S | 4.4 fix |
| 2 | **Screensaver timeout floor**: wherever setup or the screensaver delay writes `screen_off_timeout` on a Google TV, never below 360000 ms, and say so in the delay row. (Check first whether any path writes lower; measure.sh restores what it found.) | Screensaver "never starts" on Google TV 12+ | S | 4.4 fix |
| 3 | **`directBootAware` on ExtrasA11yService** (and the boot receivers that must run before unlock), with every prefs/credential read moved after `ACTION_USER_UNLOCKED`. Test on a TCL Android 11 first. | Home key catch (R3) not bound after a reboot → home not sticking | S | 4.5 small |
| 4 | **Re-enable our own receivers** at each home start (and 5 s/15 s after a wake): if `getComponentEnabledSetting` says DISABLED for a component the manifest enables, set it back to DEFAULT. Report it. | Home and screensaver logic not running after boot on TVs with a vendor startup manager | S | 4.5 small |
| 5 | **Long-press on key-up by hold time** (e.g. ≥ 500 ms) in the home and the Extras key service, not on repeat counts; keep the key filter off whenever it is not needed (we already toggle it). | Remote issues: long-press not firing, volume/auto-repeat lost while filtering on TCL | M | 4.5 small |
| 6 | **Boot respects the person's front**: after boot, re-check what is in front at fixed delays and bring the home back only if a stock *home activity* (class list per launcher) is in front, never over an HDMI input the TV started on or an app the person opened. Feed it into homedoctor's angle F. | Home not sticking after reboot without fighting HDMI/CEC starts | M | 4.5 small |
| 7 | **Notification-listener reviver** (reads no notification): approved by setup with `cmd notification allow_listener`, removed by reset; when Android re-binds it after a kill, run A11yHeal. Needs an Arabic line in the plan and in «تراجع». | Home not sticking after standby kills (TCL/Xiaomi-style firmwares) | M | 4.5 small |
| 8 | **`KEYCODE_ALL_APPS` as Home** and a small app-key table (`hold:`, scancode, HDMI input targets) in SETTINGS.md commands. | Remote issues on Xiaomi-type remotes and unmapped app keys | M | 5.0 |
| 9 | **Baseline profile** (`profileinstaller` + a generated profile in CI) for the home and the screensaver. | Slowness: first launches after install/update on 1–1.5 GB TVs | M | 5.0 |
| 10 | **Per-model profile table in the APK**, fed by homedoctor winners (which angle, boot delay, key filter yes/no), with unit tests like Tigris' `StockHomeCatalogTest`. | Home not sticking on specific models, without new code per report | L | 5.0 |

Sources (all public): github.com/Gathin23/cleanyourtv, pengchujin/aerial-tv, simbasjungles/free-my-tv,
JackyAndroid/AndroidTVLauncher, humblemuzzu/dumbtv, theothernt/AerialViews, lesnolie/Tigris, techgenius1/tvOS-Launcher,
nielsvanvelzen/tv-launcher, leanbitlab-org/LtvLauncher, aradhyas8/Plume_Launcher, osrosal/flauncher, schorschii/FsClock-Android,
tsynik/LeanbackLauncher.
