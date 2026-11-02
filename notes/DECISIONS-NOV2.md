# UrTV decisions, 2 Nov 2026

Sources: `in-nov2/responses.csv` (546 rows; 524 setup/problem reports; **480 unique** once repeated submissions are removed, e.g. rows 488–492 are one report sent 5 times), `master` = `next` = d40bdc7 (4.1, build 168), CHANGELOG, `install.sh`/`tweaks.sh`/`RemoteKeys.java`/`template.html` on master, GitHub #48/#49, the four external repos (cloned read-only). Since 29 Oct 12:00 there are **114 unique reports (77 setup, 37 problem)**: TCL 55, Xiaomi 27, Google-branded boxes 10, Skyworth 13. By Android version: 11 = 59, 14 = 41, 9 = 11, 12 = 11. Row numbers below are CSV lines. No personal data.

## 1. The last 10 updates (`git log --first-parent origin/master`)

| # | commit, date | what shipped |
|---|---|---|
| 1 | a2d07a5, 21 Oct | Docs for the two screensaver timers (photos, idle) |
| 2 | a7d5682, 21 Oct | Settings redraw on return; `saver.sh` exits 1 on "error:" |
| 3 | 51b9680, 23 Oct | Release: install survives TVs that end background jobs, home cards, sleep timer, own photos/adhan, new design, Wi-Fi-safe home switch |
| 4 | 5bb43e5, 26 Oct | 3.141: any key stops the adhan, more countries and their methods, parental PIN, export/import, undoable home switch |
| 5 | 52ccea3, 29 Oct | **4.0** (b157): glass look and «المظهر», new settings, phone remote in every app, «تحديث تلفزيوني», Control Center, «تلفزيونك» tile, adhan keeps its city |
| 6 | 2b4b59f, 29 Oct | 4.0 update (b164): new phone page, a11y heals after update, Settings key outside our home, 12/24 h, 3,679 cities |
| 7 | 5e309c8, 30 Oct | 4.0 fix: TCL ASSOC_START allowed (adhan rescheduled after update) |
| 8 | 0f38c80, 30 Oct | **4.1**: the 4.0 fixes (TCL ASSOC_START, muted TV stays muted, Settings key on HDMI, pairing not cut) |
| 9 | d1d7e18, 30 Oct | 4.1, same, reworded |
| 10 | d40bdc7, 30 Oct | 4.1 build/publish (b168) |

**What they were meant to fix, and what is still reported** (unique reports, by the version that set the TV up):

| problem | meant to be fixed by | before | after (4.1 = d40bdc7, 71 unique) | verdict |
|---|---|---|---|---|
| Stock home kept on Google TV sets where the setup wizard owns the saved Wi-Fi | 4.0: «تلفزيونك» tile + one plain "keeps its own home" line | 16/67 on 5bb43e5 (24%) | **18/71 (25%)**; owners still complain: rows 470, 474, 498, 504 («للاسف الي تغير بس شاشة التوقف»). Models: Xiaomi AFKR0/AFMU0, TCL G07/G08/G13, Google TV Streamer, G7max, Skyworth GTV stick | **not fixed**: the tile works, but the people want Home |
| Remote Settings key | 4.0 (key opens our settings) + 4.1 (HDMI) | 6 reports 24–27 Oct | 3 on 4.1 (rows 462, 464, 469: Haier MatrixTV AEL/BE, TCL G05 keycode 4077) | **not fixed** on these remotes |
| Remote app keys (YouTube/Netflix/Prime) | none, only 190/191 mapped | 2 | 4 (rows 459, 463, 537; owner line row 533). TCL sends 4062 Netflix, 4065 YouTube, 4099 Prime | open |
| Android 9 "with problems" (tweaks) | 4.0: skip `device_config` < SDK 29, «rare» bucket | 13/14 A9 on 5bb43e5 | `device_config` gone (1); **`standby_bucket` still flagged on 6/7 A9 4.1 reports** ("asked for rare, it says active") | half fixed |
| "Safety switch put the stock home back" | 4.0 heartbeat check | 0 on 5bb43e5 | **5 on 4.1** (+2 on 2b4b59f, 1 on 0f38c80) | **new since 4.0** (see 2.3) |
| Adhan not rescheduled after update on TCL | 4.1 ASSOC_START | owner's TV | no report since; 23 TCL reports show `Auto-start (TCL): … allow` | fixed |
| TV does not answer / does not come back after the reboots | 4.0 retries | 8 on 5bb43e5 | 7 on 4.1 (row 463: G10 did not come back in 10 min) | still there, lower |

## 2. Problems since 29 Oct, ranked by how many people they hit

| rank | problem (reports) | cause (reports + code) | fix | how |
|---|---|---|---|---|
| 1 | **Stock home kept** (18 setups on 4.1 + 7 problem reports: 432, 445/448/459, 453, 470, 474, 498) | `install.sh` `wifi_owner()`: every saved network has `cname=…setupwraith`, so the wizard stays on and Home goes to `setupwraith/RecoveryActivity` then launcherx. Rows 448/459/496: **forgetting the Wi-Fi and joining it again in TV Settings moved cname to `com.android.tv.settings`/`android.uid.system`, and the switch held through 3 reboots** (Clean Your TV did the same on one of these TVs) | **Yes, low risk**: an optional guided step "forget and rejoin your Wi-Fi", then read `cname` again and switch only if it moved (wizard off first, our home confirmed in front, then launcherx). Nothing changes if it did not move | add a step |
| 2 | **Remote keys** (7: Settings key 462/464/469; app keys 459/463/537/533; Tornado 449) | `RemoteKeys.java` maps only 176, 83, 190, 191. TCL vendor codes 4062/4065/4099/4077 reach our window and are dropped (UrTVKeys logged `key=4065`, `key=4077`). Haier: SystemUI `TvNotificationPanel` eats Settings ("Could not resolve … TOGGLE_NOTIFICATION_HANDLER_PANEL"); YouTube 190 on G08 is a global key for launcherx's receiver, which is off | **Yes, no risk** for the codes (add 4062, 4065, 4099, 4077 to the map, test only). Haier: M, let our a11y service also catch Settings on our home when that panel has no handler | add codes; later a11y |
| 3 | **"Safety switch put the stock home back"** (5 on 4.1; row 445 shows the line printed while launcherx **stayed disabled** ~10 min and the TV showed the recovery screen) | `install.sh` line 537: the `*)` catch-all prints this line for **any** unknown return code (3 and others), not only when the deadman fired. On wizard-Wi-Fi TVs (AFKR0 rows 488/532) the 1st run ends here, the 2nd run ends "keeps its own home": 3–5 wasted minutes and a false message | **Yes, low risk**: check `wifi_owner` **before** arming (skip early, rc 6); give rc 3 its own message; on unknown rc read Home and the stock launcher's enabled state and say what is true | fix code |
| 4 | **Android 9 `standby_bucket` !!** (6/7 A9 on 4.1) | `tweaks.sh` 187–197: the Play Store goes back to bucket 10 (active) on Android 9, and any mismatch is a `!!` that makes the report "with problems" | **Turn it off**: do not set the bucket below SDK 30 (log it as skipped, as `device_config`) | turn off |
| 5 | **Requests in problem reports** (rows 460, 462, 463, 469: English UI; 462/463/469: Back on home opens the screensaver; 426/427: Settings key short/long and a Bluetooth button; 497 + 531: photos every 30 s; 501 hide «المهام»; 526 hide the Ramadan countdown) | features, see section 3 | see section 3 | |
| 6 | **Arabic letters cut apart under the clock** (row 528, AFMU0) | `template.html:433` `.subline{letter-spacing:.3em}`; also `.panel .label` .2em (448) and line 115 .18em. Chromium breaks Arabic joining when letter-spacing ≠ 0 | **Yes, no risk**: `letter-spacing:0` on Arabic text | CSS |
| 7 | **USB drive not seen after setup** (TCL C06, 5 rows 516–521, one TV) | none of the 14 disabled packages caused it (each tested); it came back after a reboot with Developer options off. USB host state after the setup's reboots, not our lists | **No code**. A `docs/TVS.md` line: "reboot once with Developer options off" | doc |
| 8 | **«يُتلى الآن» card: left/right changes the surah** (row 418) | documented behaviour (`docs/SETTINGS.md`): arrows on the card = previous/next surah, so focus cannot leave it sideways | **Yes, low risk**: arrows move focus; OK opens the controls; prev/next stay in the player | UX |
| 9 | **Apps hang after standby** (row 538, G08, 4.0) | tweaks: `cached_apps_freezer enabled` + Play Store `RUN_IN_BACKGROUND deny`. Not proven (reset before re-testing) | **Turn it off**: leave the freezer at `device_default` (Android 14 has it on anyway; on 11–13 it is experimental). Keep the Play Store tweak | turn off |
| 10 | HDMI inputs do nothing on CultraView (row 522) | `config.xml` `live_tv_package` is a single string (`com.tcl.tv`); here `com.cultraview.livetv` | **Yes, no risk**: a list, the first one installed wins | config |
| 11 | Install failures (457 Amlogic p231 no leanback, 486 Play Protect blocks the APK, 502 empty props, 543 KOBRA "No space" in `/data/local/tmp` with 7.4 GB free, 449 download failed) | each one TV | later; 543 needs the b64 temp file in another folder | later |
| 12 | Single reports: boots into Live TV after a power cut (416/417 Changhong), time back to manual after reboot (453), two «الواجهة» entries (453, likely app + «تلفزيونك» tile), Assistant error (537), Quran stutter on A9 (443), webOS beta text cut (456), Oman Isha ±5 min (477) | per TV | later (Changhong: see Clean Your TV `modelhome`, section 4) | later |

## 3. Every idea or request: verdicts

Effort: S under a day, M 1–3 days, L more. "Off" = off by default.

| item | source | verdict | effort | main risk |
|---|---|---|---|---|
| G7max / wizard Wi-Fi: forget and rejoin, then switch | owner 1, rows 448/459/496 | **DO now** | M | the person types the Wi-Fi password again; we never touch Wi-Fi ourselves |
| Custom background | owner 2 | Already there (own photos). Separate home/saver photos (row 485): **DO later** | M | memory on 1 GB TVs |
| Madinah adhan | owner 2 | **NO for now**: no licence-clean recording found (section 5). «أذان من ملفي» covers it: the person brings the file | – | copyright |
| Screensaver starts by itself at the adhan | owner 3 | **DO later**, opt-in, only when our home is in front | M | starting an activity over other apps needs SYSTEM_ALERT_WINDOW; it must never cover Live TV or an app |
| Quran radio stations (Cairo, Kuwait) | owner 4, rows 447/450 | **DO now** if each comes from its broadcaster or mp3quran's catalogue (`fetch_quran.py --check`) | S | a dead stream |
| Haram live, live prayers | owner 4/9 | **DO now as a link**: open SBA's official channel in its own app or page (section 5); no stream in our app | S | the link moves |
| Look at tellyscope / TvToolsPro | owner 5 | section 4 | – | GPL / no licence: ideas only |
| Fajr/other adhan from YouTube links | owner 6 | **NO**: YouTube rips, no licence | – | copyright |
| Channel that airs the team's match | owner 7 | **DO later**, channel **names as text**, no logos | M | TheSportsDB's TV data needs a paid key; beIN/Thmanyah logos are trademarks |
| Clean Your TV since fcde1ee | owner 8 | section 4 | – | – |
| 5-second notice at the adhan | owner 9 | **DO now**: a text toast «أذان العصر» when the adhan starts, only while the adhan is on | S | none (text toasts need no permission) |
| «التكبير فقط» adhan | owner 9 | **DO now** for the Makkah voices (per-voice cut time, fade as «أذان مختصر») | S–M | someone has to listen to each recording for its cut point |
| #48 voice tasks on Google TV | GitHub, row 495 | **DO now**: a text field + «إضافة» in «المهام»; the mic stays | S | none. `RecognizerIntent` is answered by Google's search on Google TV |
| #49 adhan only while the screensaver shows | GitHub | **DO now**, off by default: "while UrTV is in front (home or screensaver)", via `ACTION_DREAMING_STARTED/STOPPED` + our home's resumed state | S–M | the adhan must stay off by default (unchanged) |
| New settings (prototype) | owner 12 | **DO later** | M | moves rows people know |
| Reciters Badr Al-Turki, Muhammad Al-Luhaidan | owner 12, row 428 | **DO now** if on mp3quran (same source as the 40) | S | none |
| Verses/hadith every 3 minutes | owner 12, row 429 | **DO now** (add a 3 min choice) | S | none |
| Bluetooth button | owner 12, row 427 | **DO now** as a Control Center tile (the TV's "Remotes & accessories"), not a new top-row button | S | the intent differs per brand |
| Settings key short = TV settings, long = ours | owner 12, row 426 | **DO now**, on our home | S | some TVs take 176 first |
| Changhong boots into Live TV | owner 12, rows 416/417 | **DO later** (per-model study) | M | boot-time activity starts are restricted |
| «إعداد تلفزيوني» stuck under the screensaver | owner 12 | **DO now**: hold the screensaver off while it runs (TCL ignores KEEP_SCREEN_ON) | S | none |
| work/phone-setup (6 commits) | owner 12 | **DO later**, after this week's fixes; must add ASSOC_START (PLAN 30 Oct) | L | the setup runs on the TV without a PC |
| work/safe-mode (4 commits) | owner 12 | **DO later**, after its TV tests SM1–SM12 | M | a second Home process |
| English interface | rows 460/462/463/469; 18 rows in the whole file | **NO**: owner rule, Arabic UI | – | – |
| Back on home opens the screensaver | rows 462/463/469 (+247) | **DO now**, off by default (Clean Your TV c63d36a1 did the same) | S | Back must change on the home only |
| Photos every 30 s | rows 497, 531 (4 in the file) | **DO now**, floor 30 s on ≥2 GB, 1 min below | S | stutter on weak TVs |
| Hide Ramadan countdown, hide «المهام», night dimming off | rows 526/350, 501, 537 | **DO now** (three settings, on by default as today) | S | none |
| Gregorian only / Hijri only | 6 rows in the file | **DO later** | S–M | layout |
| Map every remote key | row 449 | **DO later** | M | Back/Home must never be taken |
| One-button app update; «تحديث الشاشة الآن» confuses | rows 441, 421 | **DO now**: rename it to «تحديث صفحة شاشة التوقف» and point to «تحديث تلفزيوني» | S | none |
| Oman ministry timetable | row 477 | **DO later** (import a yearly table) | M | data upkeep |
| Shia hadith instead of Sunni | row 499 | **NO**: Shia-specific content | – | – |
| Gold in KWD/BHD, Champions League, Asian Cup | rows 450, 454 | **DO later** | S each | data source |
| News card, SIMKL card, anime times, dental tips, adhkar over other apps every 5 s | rows 514, 424, 450, 444, 524 | **NO**: out of scope; overlays over other apps are unsafe | – | – |
| "Free RAM" button | row 536 | **NO**: removed in 4.0, no effect on Android 14 | – | – |
| Animated weather themes, Apple TV icons | rows 535, 465 | **DO later** (themes backlog) | L | memory |
| VPN for matches | row 440 | **NO**: out of scope | – | – |

## 4. Ideas worth taking

**Clean Your TV** (MIT, credit in THIRD_PARTY_NOTICES). 826 commits fcde1ee..6e8615eb, mostly device lists. The useful ones:
- `back_opens_clock`, off by default; Back on Home only, not mapped through the key helper: [c63d36a1](https://github.com/Gathin23/cleanyourtv/commit/c63d36a1).
- Vendor keys ≥ 1000 (TCL Netflix 4062) reach only the focused window, so Home must handle them: [e525f084](https://github.com/Gathin23/cleanyourtv/commit/e525f084), [8b1caf89](https://github.com/Gathin23/cleanyourtv/commit/8b1caf89), and remote-button action words [8e8aed0c](https://github.com/Gathin23/cleanyourtv/commit/8e8aed0c).
- Core guards: `com.android.sdksandbox` [bf505124](https://github.com/Gathin23/cleanyourtv/commit/bf505124) (not in our `core.txt`); `com.android.location.fused` boot-looped a TCL S5400A [f5c37c36](https://github.com/Gathin23/cleanyourtv/commit/f5c37c36) (ours keeps it in `keep.txt`; move it to `core.txt`).
- Inputs: `input_action` may name an activity (Redmi `ExternalSourceActivity`) [22e5884f](https://github.com/Gathin23/cleanyourtv/commit/22e5884f); the Input key starts `VIEW_INPUTS` [cded063f](https://github.com/Gathin23/cleanyourtv/commit/cded063f).
- Cold boot on Coocaa/Skyworth through `persist.service.model.*` (`modelhome`), a model for the Changhong Live-TV boot study: [5f85e645](https://github.com/Gathin23/cleanyourtv/commit/5f85e645).
- With launcherx redirected, app buttons need its SYSTEM_ALERT_WINDOW appop: [a88188ba](https://github.com/Gathin23/cleanyourtv/commit/a88188ba). HyperOS 3: a screen-off force-stops user apps after 2 s: [672195d0](https://github.com/Gathin23/cleanyourtv/commit/672195d0). `open_on_start_on_wake`: [7f402323](https://github.com/Gathin23/cleanyourtv/commit/7f402323).

**tellyscope v0.14.0** (GPL-3.0: ideas only, no code copied into UrTV):
- "What's new" per version, and a direct "Download APK" in the update dialog: [WhatsNewScreen.kt](https://github.com/man15h/tellyscope/blob/main/app/src/main/java/app/tellyscope/ui/WhatsNewScreen.kt), [UpdateDialog.kt](https://github.com/man15h/tellyscope/blob/main/app/src/main/java/app/tellyscope/ui/UpdateDialog.kt). That would answer row 441's confusion.
- "Who has access": list the TV's allowed ADB keys and revoke all but one: [Access.kt](https://github.com/man15h/tellyscope/blob/main/adb/src/main/kotlin/app/tellyscope/adb/Access.kt). Good for the end of our setup (the PC's key stays allowed today).
- A protected list by name pattern (keyboard, tvinput, hdmi, bluetooth, rcu, setupwraith…), where a TV's list can only add warnings: [Protected.kt](https://github.com/man15h/tellyscope/blob/main/adb/src/main/kotlin/app/tellyscope/adb/Protected.kt).

**TvToolsPro v1.2.1** (the repository has **no licence file**: ideas only). The 1.2.1 change is only "scan no longer lists the phone". Ideas: an arbitrary-keycode sender and recordable macros (a quick way to learn a remote's codes such as 4062/4077) ([MacroEntity.kt](https://github.com/deekshith0509/TvToolsPro/blob/master/app/src/main/java/dev/tvtools/pro/data/MacroEntity.kt)); a small on-TV helper with an HTTP API for absolute volume ([tvhelper](https://github.com/deekshith0509/TvToolsPro/tree/master/tvhelper)). Our phone page already does most of this.

**tv-prayer-companion** (an add-on built for UrTV, **no licence file**, 3 commits; the code is minified one-liners): it reacts to `ACTION_DREAMING_STARTED/STOPPED` ([IdleService.java](https://github.com/abuazam1/tv-prayer-companion/blob/main/app/src/main/java/local/tv/quranidle/IdleService.java)), which is the hook #49 needs. It also plays Surah Al-Baqarah when the screensaver starts. **Do not take:** the 20-minute full-screen notice that mutes media and blocks the remote through accessibility ([PrayerKeyService.java](https://github.com/abuazam1/tv-prayer-companion/blob/main/app/src/main/java/local/tv/quranidle/PrayerKeyService.java)): it is unsafe and against "adhan off by default". Its adhan files come from the same Archive.org PDM collection we use ([audio-sources.json](https://github.com/abuazam1/tv-prayer-companion/blob/main/scripts/audio-sources.json)), so nothing new there.

## 5. Licence-clean sources

Limit of this check: sba.sa, aloula.sa, archive.org and commons.wikimedia.org are **blocked from this session**, so nothing below was opened directly. They must be allowed in the network settings, or checked by hand, before anything ships.

- **Quran radio.** What we may do: play a broadcaster's public stream, with its name, without re-hosting it. Our station 0 «إذاعة القرآن الكريم - السعودية» = `stream.radiojar.com/0tpy1h0kxtzuv`, and **who runs it is not confirmed**: the only public listing of that ID describes a Cairo Quran station ([extensionauditor](https://extensionauditor.com/scan/kjepemkcnajjhiooinkcchookkjhocde)). **Action:** listen to it, and replace it with SBA's own stream URL from sba.sa or aloula.sa (SBA = the Saudi broadcaster, [Wikipedia](https://en.wikipedia.org/wiki/Saudi_Broadcasting_Authority)). The other 10 are mp3quran/qurango streams, the same source as our reciters. Cairo/Kuwait Quran radio: only the broadcasters' own URLs.
- **Haram live / live prayers.** SBA's Saudi Quran TV is the official Makkah feed; SBA streams it on aloula.sa and on its official YouTube channel ([guide](https://mhtwyat.com/?p=451049), secondary). **May:** a tile that opens the official page or the official channel in the YouTube app (a link, not a copy). **May not:** pull the HLS into our player, re-host it, or use aggregators such as makkahlive.net.
- **Madinah adhan: none found.** Our 21 adhans are Archive.org "Azaan of World Mosques" (Public Domain Mark, the uploader's label); none of the 21 is from Madinah, and the full file list could not be read from here. No Wikimedia Commons audio of the Prophet's Mosque adhan with a licence turned up ([search](https://thewahyproject.com/category/recitation/) only shows sites with no licence). **May:** «أذان من ملفي» (the person's own file). **May not:** YouTube rips, the links in owner item 6, unlicensed "free MP3" sites. Note: the Makkah files carry the uploader's PDM label, but they are probably broadcast recordings. That is acceptable for now; keep the source credit.

## 6. This week, in order

1. `RemoteKeys`: map 4062/4065/4099/4077 (S) + Settings short/long press on our home (S).
2. `install.sh`: check `wifi_owner` before arming the switch; a true message for every return code (fixes the false "safety switch" line) (S).
3. Guided "forget and rejoin Wi-Fi" step for wizard-owned networks, then switch only if `cname` moved; TV test on a G08 and an AFKR0 (M).
4. `tweaks.sh`: no standby bucket below SDK 30; freezer left at `device_default` (S).
5. `template.html`: `letter-spacing:0` on Arabic text (S).
6. #49 «الأذان أثناء شاشة التوقف فقط» (off) + 5-second adhan toast + «التكبير فقط» for the Makkah voices (S–M).
7. #48 text field + «إضافة» in «المهام»; «يُتلى الآن» arrows move focus (S).
8. Small settings: Back opens the screensaver (off), photos 30 s floor, hide countdown/«المهام»/night dim, verses every 3 min, Bluetooth tile, rename «تحديث الشاشة الآن», `live_tv_package` list (+cultraview), `sdksandbox` in `core.txt` (S each).
9. Streams: verify the radiojar station, get SBA's own Quran-radio URL, add a Haram-live link tile and Cairo/Kuwait stations from their broadcasters (S, after the hosts are allowed).
10. Share page: drop repeated submissions (5 copies of one report seen); then one 4.2 TV test on TCL G08 + Xiaomi AFKR0 + Haier, release.
