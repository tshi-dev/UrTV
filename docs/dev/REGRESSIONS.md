# Regressions: every bug we fixed, and the test that keeps it fixed

The owner's rule (17 Nov 2026, after 4.3.4): **every new update is validated so nothing comes back.** This register is how: one row per bug
ever fixed, each with the test that fails if the bug returns. `bash tests/run.sh regressions` runs every guard named here plus the gate's own
rules (`tests/regressions/`); CI runs the same group before anything is published (`.github/workflows/regressions.yml`, docs/dev/RELEASE.md).

**Adding a row (every bug fix, in the same commit as the fix):** the next free id; the symptom as the person saw it (one line); the cause
(one line); the version or build that fixed it; the guard (one or more test files, in backticks, that FAIL if the fix is reverted: check by
reverting it once); the area (`launcher`, `saver-app`, `page`, `phone`, `optimize`, `rescue`, `extras`, `CI`, and the TV when it matters);
**On TV** Y when a person can see it on the TV in a quick check, which also means a line in `tester/RELEASE-SMOKE.md`.
`tests/regressions/regressions_index_test.py` fails on a row without a test file that exists, on a duplicate id, on a Y row missing from
the smoke test, and on an IP address in this file. Never an IP, a serial, a Wi-Fi name or a person's name here (AGENTS.md).

Sources of the first rows: the 4.3.3/4.3.4 hotfix list, CHANGELOG.md (4.0 to 4.3.2), the GitHub issues, and the tester's report
(`tester/REPORT.md` on `work/tester-report`, steps 1 to 93). A row seeded from the tester names the step; the CHANGELOG and the report
often describe the same bug in two ways, and both rows are kept when their guards differ.

| ID | Symptom | Cause | Fixed | Guard | Area | On TV |
|---|---|---|---|---|---|---|
| R-001 | TCL: the remote froze for seconds every minute and at each return to the home | UpdateChannel.read: a provider query on the home's UI thread (tick, onResume) restarted the killed screensaver process | 4.3.4 | `tests/ui_thread_channel_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher (TCL) | Y |
| R-002 | Back on the home was slow on a TCL | backExitTakes read ConfigSnapshot (a provider query) on every Back press | 4.3.4 | `tests/ui_thread_channel_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher (TCL) | Y |
| R-003 | TVs on the beta channel got no update when a release went to everyone | promote-beta copied only from apk-next, and the TV read apk-beta alone; now the newer of apk-beta and apk-latest | 4.3.4 | `tests/beta_channel_test.py`, `tests/beta_newest_test.py`, `tests/regressions/release_gate_ci_test.py` | CI / updater | Y |
| R-004 | «تحديث تلفزيوني»: Down skipped «تحديث صفحة شاشة التوقف» | the release note's fullScroll moved the focus to the last row | 4.3.4 | `tests/update_rows_focus_test.py` | saver-app | Y |
| R-005 | The long-press app menu cut its words («صفحته في المتجر», «إلغاء التثبيت») | the options box was exactly the tile's width | 4.3.4 | `tests/options_fit_test.py` | launcher | Y |
| R-006 | A settings row needed two presses | a late reply rebuilt the list while OK was held; the first press was lost | 4.3.4 | `tests/ok_hold_test.py` | saver-app (TCL) | Y |
| R-007 | «الألعاب» in «مركز التحكم» opened settings or an old preview instead of the games | it brought back the hub's existing task | 4.3.4 | `tests/games_open_test.py` | launcher + saver-app | Y |
| R-008 | A hidden app could not be brought back; the edit mode closed itself when an app changed | no route to the hidden list; onResume ended the mode | 4.3.4 | `tests/hidden_apps_way_test.py` | launcher | Y |
| R-009 | Sony remote: Netflix, YouTube, Prime, mic and colour buttons did nothing after the setup | community lists turned off the maker packages that answer them | 4.3.4 | `tests/regressions/keep_list_test.py` | optimize + setup (Sony) | Y |
| R-010 | In «تعديل المفضلة»/«تعديل التطبيقات» app, colour, channel and maker keys did nothing | the edit modes took every key but volume and power | 4.3.4 | `tests/edit_keys_test.py`, `tests/regressions/key_handlers_test.py` | launcher (Sony) | Y |
| R-011 | The screensaver's bottom strip jumped as the slot turned | the strip's height followed the slot's content | 4.3.3 | `tests/strip_steady.test.mjs` | page | Y |
| R-012 | Markets on the home showed codes (4164, 4002, 4349) instead of names (GitHub #60) | the home's name table knew only some codes; now generated from the page's map | 4.3.3 | `tests/market_names_test.py` | launcher | Y |
| R-013 | The photo went fully black on some TVs | the photo's dimming read a colour (--dim) as an opacity | 4.3.3 | `tests/photo_dim_test.py` | page | Y |
| R-014 | «بدء شاشة التوقف بعد» said «٢١٤٧٤٨٣ ثانية» and opened the maker's settings | 2147483647 (never) shown as seconds; the row opened settings with no timer | 4.3.3 | `tests/idle_rule_test.py` | saver-app | Y |
| R-015 | «تحقق من التحديث» on the beta channel with no beta out said it does not work on this TV | no apk-beta release was treated as an error, not «uptodate» | 4.3.3 | `tests/beta_channel_test.py` | saver-app | Y |
| R-016 | Apps showed version «4.3» on the 4.3.1 release | VERSION allowed only major.minor; the patch number was dropped from the name | 4.3.2 | `tests/version_name_test.py` | CI | Y |
| R-017 | «تلفزيونك», «المدينة», «فرقي» did nothing on a TV with only the home app | Second app (settings/screensaver) missing; buttons silently opened nothing | 4.3.2 | `tests/install_saver_test.py` | launcher, Fire TV | Y |
| R-018 | Markets «قائمتي» showed stock code and «-» instead of name and price (#60) | Names/prices only for the 30 largest; others asked Yahoo via slow proxies | 4.3.2 | `tests/market_names_test.py` | page + launcher | Y |
| R-019 | Deleting UrTV after removing the factory UI left a black screen, no home | Stock home disabled by «تخلّص من واجهة المصنع»; no warning before uninstall | 4.3.2 | `tests/regressions/legacy_guards_test.py` | saver-app | Y |
| R-020 | «تحكم من الجوال» title sat outside the 5% safe area (y 40 of 1080) | Fixed top padding ignored the 5% safe margin | 4.3.2 | `tests/spacing_178_test.py` | launcher | Y |
| R-021 | Matches page source line touched the page title | Header had no gap or wrap between title and source line | 4.3.2 | `tests/regressions/legacy_guards_test.py` | page | Y |
| R-022 | Adhan times and prayer card vanished from home after updating to 4.3.1 | Home started with defaults when the screensaver app did not answer | 4.3.2 | `tests/fragment_keep_test.py` | launcher | Y |
| R-023 | Command install failed with «Unable to open file» | APK did not reach the TV whole; install.sh never resent it | 4.3.2 | `tests/install_retry_test.py` | optimize, Nokia Box 8000 / TCL BeyondTV4 | N |
| R-024 | Command install of a truncated upload failed instead of resending | File size on the TV not compared with the local file before pm install | 4.3.2 | `tests/install_retry_test.py` | optimize | N |
| R-025 | Setup report listed apps already off before setup as «turned off by setup» | Report did not separate pre-disabled apps (backup) from setup's own | 4.3.2 | `tests/share_test.py` | optimize, Nokia / TCL BeyondTV | N |
| R-026 | SmartTube install said «no build» on armeabi-v7a TVs | Newest release lacked that ABI's file; no fallback to an older full release | 4.3.2 | `tests/install_extras_test.py` | optimize | N |
| R-027 | Adhan sounded at midnight for no reason | Late alarm (standby or clock jump at boot) still played the adhan | 4.3.1 | `tests/adhan_late_test.py` | launcher | N |
| R-028 | Home showed black (or blurry) for seconds when coming back from an app (GitHub #58) | Photo let go on trim and reloaded; no stand-in first frame | 4.3.1 | `tests/wall_first_frame_test.py`, `tests/home_snapshot_test.py` | launcher, Mi Box | Y |
| R-029 | «Make your TV open on UrTV» card covered a home card, unreachable by remote | Two-line card with a button; top buttons' UP led to themselves | 4.3.1 | `tests/home_nudge_test.py` | launcher | Y |
| R-030 | Favourites row showed only five apps after the TV was switched on | App list incomplete at cold start; row topped up to five, never rebuilt | 4.3.1 | `tests/dock_retry_test.py` | launcher | Y |
| R-031 | Same match shown twice, sometimes with a wrong (unplayed) score | Same game from team, league and tournament sources not merged to finished copy | 4.3.1 | `tests/football_page.test.mjs` | page | Y |
| R-032 | Radio station stopped for good after a drop, with a vague error | No automatic retry/backoff or source swap; one generic fault line | 4.3.1 | `tests/radio_fault_test.py`, `tests/radio_policy_test.py` | launcher | N |
| R-033 | Postponed or cancelled games shown as live or upcoming | Page did not treat TheSportsDB PST/ABD/AWD statuses as called off | 4.3 | `tests/called_off_test.py` | page | Y |
| R-034 | Followed team's result appeared late on the matches page | Game read from the league file, not the fresher copy | 4.3 | `tests/called_off_test.py` | page | N |
| R-035 | Home clock, prayer times and reminders an hour ahead in Morocco (#55) | Launcher used the TV's outdated time-zone data, unlike the page | 4.3 | `tests/zone_fix_test.py` | launcher | Y |
| R-036 | Quran Radio said «Stream format not supported» after a brief drop | Any player error read as a format fault; no second-source try | 4.3 | `tests/radio_policy_test.py` | launcher | Y |
| R-037 | Live radio stations stalled and stayed silent | No stall watchdog; live HLS played on MediaPlayer | 4.3 | `tests/stall_watch_test.py` | launcher | N |
| R-038 | USB sticks could not be opened after setup (#50) | Setup could turn off the TV's own USB file viewer | 4.3 | `tests/usb_test.py` | optimize + launcher | Y |
| R-039 | Previously used app came to the front while the home updated itself | Home app's self-replace left the last app in front, no cover | 4.3 | `tests/update_cover_test.py` | launcher | Y |
| R-040 | In-app setup «جهّز كل شي» stopped at «قياس الذاكرة قبل» after its restart | Resume mark written after reboot was sent; memory read had no time limit | 4.2 | `tests/setup_runner_test.py` | launcher (setup) | Y |
| R-041 | Casting failed: «untrusted device» on Chromecast with Google TV | Home switch disabled the setup wizard holding the cast credentials | 4.2 | `tests/cast_test.py` | optimize + launcher, Chromecast with Google TV | Y |
| R-042 | Heavy lag on weak TVs after the 4.0 to 4.1 update | Settings reads on UI thread, key filter, city scan of 3,679 cities per read | 4.2 | `tests/lowram_test.py` | launcher + saver-app + page | Y |
| R-043 | Play Protect blocked the home app as built for an older Android | Launcher targeted an old API level, not 34 | 4.2 | `tests/target_sdk_test.py` | launcher | Y |
| R-044 | Player could crash when started from the background | Android 12+ refused the foreground start and it was not caught | 4.2 | `tests/target_sdk_test.py` | launcher | N |
| R-045 | Notifications permission asked at every open of «تلاوة» | The ask was not remembered once made | 4.2 | `tests/target_sdk_test.py` | launcher | Y |
| R-046 | «المصادر» (live TV, source picker) did nothing on non-TCL TVs | Live TV app and source picker hard-coded to TCL packages | 4.2 | `tests/tv_vendor_test.py` | launcher, CultraView | Y |
| R-047 | Recitation and adhan too quiet next to TV sound | Player played the quiet recordings at unity gain | 4.2 | `tests/loudness_test.py` | launcher | Y |
| R-048 | measure.sh failed when the run folder path had spaces | $RUN paths were not quoted in the script | 4.2 | `tests/measure_space_test.py` | optimize (macOS) | N |
| R-049 | Arabic letters shown cut apart (adhan bar, score, clock line, card labels) | Non-zero letter-spacing breaks Chromium's Arabic joining | 4.2 | `tests/arabic_spacing_test.py` | page + launcher | Y |
| R-050 | Home stocks card stayed stale; page fixes never reached the home | Launcher always opened its bundled page, not the saver's newer download | 4.2 | `tests/home_page_test.py` | launcher | Y |
| R-051 | Stock list refused codes typed in Arabic digits (٢٢٢٢) | Input accepted only Western digits | 4.2 | `tests/stocks_search_test.py` | saver-app | Y |
| R-052 | TV setup stuck at «Allow USB debugging?» under the screensaver | Screensaver started over the prompt; TCL ignores keep-screen-on | 4.2 | `tests/saver_hold_test.py` | launcher + saver-app, TCL | Y |
| R-053 | TCL firmware updates stopped working after setup | com.tcl.xian.StartandroidService was on the safe-to-disable list | 4.2 | `tests/input_guard_test.py` | optimize, TCL | N |
| R-054 | Xiaomi remote's app keys and pairing broke after setup | tvsetup.partnercustomizer could be disabled by a community list | 4.2 | `tests/input_guard_test.py` | optimize, Xiaomi | Y |
| R-055 | Setup could turn off TV inputs, tuner or keyboards | Lists not checked against the TV's own input/keyboard packages | 4.2 | `tests/input_guard_test.py` | optimize + launcher (setup) | Y |
| R-056 | First-run or pairing wizard could be disabled from a community list | No filter for oobe/wizard packages in community lists | 4.2 | `tests/input_guard_test.py` | optimize | N |
| R-057 | TCL Guard key on the home killed the adhan | Key reached TCL's cleaner, which kills our processes | 4.2 | `tests/remote_keys_test.py` | launcher, TCL | N |
| R-058 | TCL Netflix/YouTube/Prime keys and G05 Settings key did nothing on home | Vendor keycodes 4062/4065/4099/4077 not mapped; logged and dropped | 4.2 | `tests/remote_keys_test.py` | launcher, TCL G05 | Y |
| R-059 | Could not add a task on Google TV (#48) | Tasks took only speech; the system speech window returned nothing | 4.2 | `tests/task_typed_test.py` | launcher, Google TV | Y |
| R-060 | «🎤 تكلّم» button did nothing on TVs without speech recognition | Mic shown without checking a recognizer exists | 4.2 | `tests/task_typed_test.py` | launcher | Y |
| R-061 | Home switch skipped; UrTV not the home (wizard owns the Wi-Fi) | Setup wizard owned saved Wi-Fi, so it stayed on and kept Home | 4.2 | `tests/install_retry_test.py` | optimize, TCL G08/G10/G14, Google TV | Y |
| R-062 | Setup said «safety switch put the stock home back» when it had not | Fixed line printed without checking Home, enabled state and journal | 4.2 | `tests/install_retry_test.py` | optimize | N |
| R-063 | Setup on Android 9/10 ended «with problems» | Play Store standby bucket could not be set; TV kept active | 4.2 | `tests/tweaks_journal_test.py` | optimize | N |
| R-064 | Apps hung after standby | Setup turned the cached-apps freezer on | 4.2 | `tests/tweaks_journal_test.py`, `tests/reset_tweaks_test.py` | optimize | N |
| R-065 | Boot loop risk: system sandbox package could be disabled | com.android.sdksandbox missing from the never-disable core list | 4.2 | `tests/community_test.py` | optimize, TCL | N |
| R-066 | People took the page update row for the apps update | Row named «تحديث الشاشة الآن» was ambiguous | 4.2 | `tests/update_row_test.py` | saver-app | Y |
| R-067 | On TCL, phone remote failed in other apps; adhan not rescheduled after update | TCL's ASSOC_START app-op blocked our receivers and services | 4.1 | `tests/autostart_test.py` | optimize, TCL | Y |
| R-068 | Muted TV left at volume 0 after the adhan | Adhan unmuted and did not restore the level and mute | 4.1 | `tests/fix159_test.py`, `tests/step28b_test.py` | launcher | Y |
| R-069 | Remote's Settings key did nothing on HDMI or in other apps | Key handled by the stock launcher, which setup turns off | 4.1 | `tests/settings_key_test.py` | launcher, TCL | Y |
| R-070 | Screensaver over the pairing screen switched phone control off | Any pause was treated as the person leaving the screen | 4.1 | `tests/fix159_test.py` | launcher + phone | Y |
| R-071 | Phone control in other apps stopped after the app updated itself | Android left our accessibility services unbound, switch at 0 | 4.0 | `tests/a11y_heal_test.py` | launcher, TCL | N |
| R-072 | Typing from the phone into other apps found no text field | Service did not track window state or the input-focused field | 4.0 | `tests/type_into_test.py` | launcher + phone | N |
| R-073 | «ابدأ» in «إعداد تلفزيوني» had no focus | Focus was requested before the screen's layout | 4.0 | `tests/fix159_test.py` | launcher | Y |
| R-074 | Phone page counted wrongly in Arabic («8 ساعات و 56 دقيقة», «دقيقتين») | Naive number and plural forms | 4.0 | `tests/phone_page.test.mjs` | phone | N |
| R-075 | Phone page showed the wrong current screensaver delay (45 s) | Delay sent and offered only in whole minutes | 4.0 | `tests/phone_page.test.mjs`, `tests/fix159_test.py` | phone | N |
| R-076 | Phone page «بدون» card needed a second press to save | First save of an empty card list was not sent | 4.0 | `tests/phone_page.test.mjs` | phone | N |
| R-077 | One swipe on the phone pad moved the TV focus several tiles | Swipe sent a key per distance step, not one per swipe | 4.0 | `tests/phone_page.test.mjs` | phone | Y |
| R-078 | Phone page text field hidden under the tab bar | Focused field not scrolled above the fixed bar | 4.0 | `tests/phone_page.test.mjs` | phone | N |
| R-079 | Phone page put back the old card while choosing | A TV answer still on its way redrew the selects | 4.0 | `tests/phone_page.test.mjs` | phone | N |
| R-080 | Message over the phone page swallowed taps | Overlay took pointer events | 4.0 | `tests/phone_page.test.mjs` | phone | N |
| R-081 | Adhan alarm fell back to Riyadh times | Failed read of screensaver settings used the bundled place | 4.0 | `tests/place_choice_test.py` | launcher, TCL | N |
| R-082 | Adhkar card with Western digits showed Arabic-Indic ayah numbers | Ayah signs carry Arabic-Indic numbers in the text | 4.0 | `tests/page.test.mjs` | page | Y |
| R-083 | Screensaver closed the pairing, update and setup screens | Those screens did not keep the screen on | 4.0 | `tests/fix7_test.py` | launcher | Y |
| R-084 | Typing from the phone into YouTube TV did nothing | No Android text field; resolveActivity is null on Android 11+ | 4.0 | `tests/fix7_test.py` | launcher + phone | Y |
| R-085 | Cards saved from the phone did not reach the home in front | No change message to the home; save never read back | 4.0 | `tests/fix7_test.py`, `tests/saver_cards_test.py` | launcher + saver-app + phone | Y |
| R-086 | «حماية العائلة (DNS)» refused a working TV | First check did not wait for Private DNS to validate | 4.0 | `tests/family_dns_test.py` | launcher | N |
| R-087 | Adhkar card at night showed only a start time | Night verse dropped as too long; no evening fallback | 4.0 | `tests/page.test.mjs` | page | Y |
| R-088 | Could not save «الدوريات كاملة»: «حفظ» off screen | Long choice list pushed «حفظ» below the screen | 4.0 | `tests/dialog_fit_test.py` | saver-app | Y |
| R-089 | «أكمل المشاهدة» card filled the screen, pushed favourites over the clock | Row cell not sized like a favourite tile | 4.0 | `tests/tv_rows_test.py` | launcher | Y |
| R-090 | Focused settings row's text unreadable | Focus ink via duplicateParentState not applied on the white fill | 4.0 | `tests/glass_test.py` | launcher + saver-app | Y |
| R-091 | Settings list moved sideways while scrolling | Weighted pane widths and horizontal scroll allowed | 4.0 | `tests/settings_hub_test.py` | saver-app | Y |
| R-092 | Pickers and dialogs see-through; rows behind showed | Dialog background was translucent glass | 4.0 | `tests/glass_test.py` | launcher + saver-app | Y |
| R-093 | «إعدادات تلفزيونك» tile icon was a plain white square | Tinted icon drawn with a filled square behind it | 4.0 | `tests/glass_test.py` | launcher | Y |
| R-094 | Page went blank when the WebView renderer died | onRenderProcessGone not handled | 4.0 | `tests/render_gone_test.py` | launcher + saver-app | N |
| R-095 | Control center tiles did not fit on 720p | Tile layout worked out for 1080p only | 4.0 | `tests/control_center_test.py` | launcher | Y |
| R-096 | Sound output tile did not name the real output | Generic label instead of the devices' product names | 4.0 | `tests/control_center_test.py` | launcher | Y |
| R-097 | Dust and weather mixed up; dust lost on a failed refresh | One description for weather and air quality | 4.0 | `tests/page.test.mjs` | page | Y |
| R-098 | Old forecast or weather shown as today's | Forecast rows not dated; reading age not shown | 4.0 | `tests/page.test.mjs` | page | Y |
| R-099 | Prices vanished after a failed refresh | Failed refresh dropped the last price instead of keeping it marked | 4.0 | `tests/page.test.mjs` | page | Y |
| R-100 | Home froze or lagged on slow network and large downloads | Fetches without deadlines, app list on UI thread, uncapped downloads | 4.0 | `tests/bounded_test.py` | launcher + saver-app | N |
| R-101 | Unsplash API key sent to image hosts | Client-ID header added to every request, not only the API | 4.0 | `tests/unsplash_header_test.py` | CI | N |
| R-102 | Setup trimmed all app caches every run | trim-caches ran unconditionally | 4.0 | `tests/tweaks_trim_test.py` | optimize | N |
| R-103 | Install died when ADB went silent during dex optimisation | A silent TV was treated as a failure | 4.0 | `tests/upload_test.py` | optimize | N |
| R-104 | Sleep timer shown as running after a restart | Alarm gone after reboot, saved state kept | 4.0 | `tests/sleep_boot_test.py` | launcher | Y |
| R-105 | Stock home not brought back when ours failed to open | Safety switch could be disarmed by an old marker or none | 4.0 | `tests/deadman_test.py` | optimize | N |
| R-106 | Android core packages could be disabled by setup | No never-disable core list checked before disabling | 4.0 | `tests/community_test.py`, `tests/failclosed_test.py` | optimize | N |
| R-107 | Reset put Android defaults back instead of the TV's own values | reset.sh ignored the values backup.sh recorded | 4.0 | `tests/reset_tweaks_test.py`, `tests/reset_screensaver_test.py` | optimize | N |
| R-108 | Share report printed a negative step time | Clock or date moved back between steps | 4.0 | `tests/share_test.py` | optimize | N |
| R-109 | Windows 11 reported as Windows 10 | platform.release() says 10; build number not used | 4.0 | `tests/share_test.py` | optimize | N |
| R-110 | ADB key and saved TV kept inside the clone | Default paths pointed into the repository | 4.0 | `tests/key_home_test.py` | optimize | N |
| R-111 | Reports could leak IPv6 addresses, emails, serials or Wi-Fi names | Scrub covered too few patterns | 4.0 | `tests/share_test.py` | optimize | N |
| R-112 | Page JavaScript stopped on older WebView 66 (#19, #21); 4.4: the photo and clock wrong there too | Page used syntax and CSS Chrome 66 lacks | 4.0 (JS), 4.4 (CSS) | `tests/check.py`, `tests/webview66_test.py` | page | Y |
| R-113 | Quran and adhan stuttered or fought other sound (#35) | Audio focus not requested or handled | 4.0 | `tests/regressions/legacy_guards_test.py` | launcher | Y |
| R-114 | Adhan at the wrong time after a preview or city change | A preview set the last-due guard; city change not rescheduled | 4.0 | `tests/tilawah_test.py` | launcher | N |
| R-115 | TV volume not restored after the adhan | Several volume paths; a refused focus left it changed | 4.0 | `tests/player_logic_test.py` | launcher | Y |
| R-116 | Own adhan lost when its replacement failed | Old recording deleted before the new one was in | 4.0 | `tests/own_adhan_keep_test.py` | launcher | N |
| R-117 | OEM store, TV Assistant, Aptoide, OEM settings missing from All Apps grid (GitHub #1) | Home listed only LEANBACK_LAUNCHER activities, not ordinary LAUNCHER ones | 4.0 | `tests/regressions/legacy_guards_test.py` | launcher (CVTE/MediaTek board, Android 9) | Y |
| R-118 | Setup stops at com.tcl.smartalexa with '!! still off' after keep.sh (GitHub #4) | keep.sh treated a package the TV itself ships disabled as a failure | 4.0 | `tests/regressions/legacy_guards_test.py`, `tests/share_test.py` | optimize (TCL BeyondTV, Android 9) | N |
| R-119 | 'Heavy dust' shown all day in Riyadh under a clear sky (GitHub #29) | CAMS dust model overestimate; dust label ignored visibility | 4.0 | `tests/page.test.mjs`, `tests/greeting_test.py` | page + launcher greeting (TCL Smart TV Pro, Android 11) | Y |
| R-120 | install.sh launcher stops: Wi-Fi owner still the setup wizard (GitHub #32) | Wi-Fi owner guard (setupwraith) blocked the home switch outright | 4.0 | `tests/install_retry_test.py` | optimize (TCL G07, Android 11) | N |
| R-121 | Remote's Settings/quick-settings key does nothing on the UrTV home (GitHub #51) | System kept the Settings key from the home on some Haier TVs | 4.3 | `tests/settings_key_test.py`, `tests/remote_keys_test.py` | launcher (Haier) | Y |
| R-122 | Live TV and remote TV button open last HDMI input, not tuner (GitHub #54) | Tuner opened as passthrough; TCL Live TV resumes last input; key 4001 unmapped | 4.3 | `tests/tv_vendor_test.py`, `tests/remote_keys_test.py` | launcher (TCL G10, Android 12) | Y |
| R-123 | Phone page «بدون» on home cards needed two presses of «حفظ البطاقات» | a TV state refresh re-rendered the selects and dropped the first choice | 3.152; came back on 159, fixed build 160 | `tests/phone_page.test.mjs`, `tests/fix7_test.py` | phone | Y |
| R-124 | Adhan used no city after YouTube opened: TCL refused the screensaver's ConfigProvider | TclAppBoot blocks the provider start; a failed read had no fallback | 3.153 | `tests/place_choice_test.py` | launcher, TCL | N |
| R-125 | Update screen read «(بناء 155) 4.0» with double brackets, wrong order | version not isolated inside the RTL line; build shown to people | build 156 (4.0) | `tests/update_test.py` | launcher | Y |
| R-126 | saver.sh named the old 3.142 build «4.0 (build 142)» | newest-build line applied the new marketing name to pre-4.0 builds | build 156 | `tests/versions_test.py` | optimize | N |
| R-127 | Our own «كل التطبيقات» listed the «تلفزيونك UrTV» tile | apps grid did not skip our own package's launcher alias | build 156 (about; not re-checked) | `tests/home_tile_test.py` | launcher | Y |
| R-128 | After a self-update accessibility was off: phone remote and typing dead until reboot | TCL blocked the MY_PACKAGE_REPLACED heal; our entry was dropped from the list | build 178 (4.2) | `tests/a11y_heal_test.py`, `tests/tester_fixes_test.py` | launcher, TCL | Y |
| R-129 | The TV's own update unmuted a muted TV | nothing carried the mute across our own package replace | build 180 (4.2) | `tests/tester_fixes_test.py` | launcher | Y |
| R-130 | Phone control switched off when the screensaver came during pairing | onPause stopped control whenever the pairing screen was covered | build 160 (not re-checked on TV) | `tests/fix159_test.py` | launcher | Y |
| R-131 | Phone page said «8 ساعة» and «1 دقيقة» for a 45 s screensaver | no Arabic counting; delay sent in whole minutes only | build 160 | `tests/phone_page.test.mjs`, `tests/fix159_test.py` | phone | Y |
| R-132 | One long swipe on the phone pad moved the TV focus five tiles | a swipe sent repeated keys for its whole length | build 160 | `tests/phone_page.test.mjs` | phone | Y |
| R-133 | Setup app: first OK on «ابدأ» did nothing | «ابدأ» focused before the screen was laid out | build 160 | `tests/fix159_test.py` | launcher | Y |
| R-134 | Setup's USB-debugging prompt never appeared (adbd «Invalid base64 key») | the app's public key text differed from AOSP's encoder | build 183 (prompt on first press); see open row for the recurrence | `tests/adb_client_test.py` | launcher, TCL | Y |
| R-135 | «المهام» opened with focus on «🎤 تكلّم», not the text field | first render focused the mic button | build 178 | `tests/tasks_card_test.py` | launcher | Y |
| R-136 | install.sh launcher tried a downgrade and printed a !! line | installed build not compared with the release tag's build | build 178 | `tests/install_retry_test.py` | optimize | N |
| R-137 | Settings showed «الإصدار الحالي: ٤٫٢» with no build number | the version line carried the name only | build 178 | `tests/versions_test.py` | saver-app | Y |
| R-138 | A settings row's second line was cut with «..» | value text was single-line | build 178 (not re-checked) | `tests/tester_fixes_test.py` | saver-app | Y |
| R-139 | Remote Settings key (TCL 83) on our home opened only the TV's settings | 83 not treated as the Settings key; no long-press path to ours | build 209 (4.3) | `tests/fix7_test.py`, `tests/settings_key_test.py` | launcher, TCL | Y |
| R-140 | Saudi Quran radio (radiojar) stuck buffering, never played | https to plain-http redirect not followed; cleartext blocked | build 179 | `tests/stream_url_test.py` | launcher | Y |
| R-141 | qurango live stations answered HTTP 500 at random | no retry on a transient 500 | build 179 (best guess) | `tests/stream_url_test.py` | launcher | N |
| R-142 | English: focused app caption and apps grid heading and names stayed Arabic | labels read in the TV's Arabic; heading not translated | build 178 | `tests/tester_fixes_test.py` | launcher | Y |
| R-143 | English: Tilawah footer and adhan page lines stayed Arabic | aapt collapsed double spaces so the table lookup missed | build 178 | `tests/tester_fixes_test.py` | launcher | Y |
| R-144 | English: favourites row still ran right to left | row direction not set from the language | build 178 | `tests/tester_fixes_test.py`, `tests/fav_bar_test.py` | launcher | Y |
| R-145 | English: the whole «إعداد تلفزيوني» setup screen stayed Arabic | setup strings shown without the S.t table | build 179 | `tests/setup_i18n_test.py`, `tests/tester_fixes_test.py` | launcher | Y |
| R-146 | Settings and Tilawah footer hints drawn below the 5% safe area | footer bottom padding too small (glyphs to y 1037/1032) | build 179 | `tests/spacing_178_test.py` | saver-app, launcher | Y |
| R-147 | Gaps off the 8 px scale: control centre tiles, settings rows, setup screen | 10/20 dp margins left from older layouts | build 179 | `tests/spacing_178_test.py` | saver-app, launcher | Y |
| R-148 | «تحكم من الجوال» title started at y 40, above the safe area | top padding below 5% of height | build 179 | `tests/spacing_178_test.py` | launcher | Y |
| R-149 | Month-names line ended with «…» | sample list cut by design | build 179 | `tests/spacing_178_test.py` | saver-app | Y |
| R-150 | Home round buttons 20 px apart, PIN pad 20/24, 124 px first settings gap | focus scale ate gaps; uneven margins | build 180 | `tests/fix179_test.py`, `tests/spacing_178_test.py` | launcher, saver-app | Y |
| R-151 | Tilawah rows and tabs 20 px apart (off scale) | 10 dp margins | build 180 (rows), build 181 (tabs) | `tests/fix179_test.py`, `tests/spacing_178_test.py` | launcher | Y |
| R-152 | After a self-update another app (YouTube, Jellyfin, Apple TV, Play Store) was in front | home task died on replace; Android resumed the last task | build 182 (home brought back); build 214 (UpdateCover, no flash) | `tests/tester_fixes_test.py`, `tests/update_cover_test.py` | launcher | Y |
| R-153 | No UpdateCarry log lines after an update | carry stages did not log | build 181 | `tests/tester_fixes_test.py` | launcher | N |
| R-154 | «أخفِ المبالغ» jumped the markets tabs to «السوق» | tab redraw let the falling focus switch the tab | build 180 | `tests/fix179_test.py` | launcher | Y |
| R-155 | Football games card cut club and league names with «…» | single-line clamp | build 180 | `tests/fix179_test.py` | page | Y |
| R-156 | Markets lists started under the safe area; rows had 16 px padding | page not bounded at 5%; small row padding | build 180 | `tests/fix179_test.py` | launcher | Y |
| R-157 | Favourites tray: 6th tile cut or drawn outside it; tiles not inset | tray did not clip its contents to its outline | build 181 | `tests/fav_bar_test.py`, `tests/tray_check_test.py` | launcher | Y |
| R-158 | Favourite app's name label moved with focus or sat over the wrong tile | label anchored to the focused tile | build 181 | `tests/fav_bar_test.py`, `tests/tray_check_test.py` | launcher | Y |
| R-159 | 2 px sliver of the next tile at 7 favourites | tile-width rounding leftover pixels | build 186 | `tests/fav_bar_test.py`, `tests/tray_check_test.py` | launcher | Y |
| R-160 | Arabic markets change drawn «−(٪٠٫٠٧−)» with signs on the wrong side | one bidi isolate around both value and percent | build 182 | `tests/fix179_test.py`, `tests/markets_page_test.py` | launcher | Y |
| R-161 | English markets labels wrapped («10:00 / AM», «Market direction») | breakable space in times; title not given its width | build 181 | `tests/fix179_test.py` | launcher | Y |
| R-162 | Setup language switch labels cut in half; OK jumped focus to «جهّز كل شي» | 88 px buttons; rebuilt screen focused the start button | build 181 | `tests/setup_lang_test.py` | launcher | Y |
| R-163 | «الذهب والنفط» names drawn one letter per line | fixed 260 dp columns squeezed the name | build 181 | `tests/fix179_test.py` | launcher | Y |
| R-164 | Dock showed Downloader in Arabic but Apple TV in English | dock top-up ordered by label, not package | build 181 | `tests/fix179_test.py` | launcher | Y |
| R-165 | Matches page said «لا مباريات اليوم» while the widget showed a match | page opened on another day than the widget | build 181 | `tests/fix179_test.py` | page | Y |
| R-166 | After saving a holding focus went to «مباشر», then off-screen | page redraw lost focus; no scroll to the saved row | build 183 | `tests/fix179_test.py`, `tests/markets_page_test.py` | launcher | Y |
| R-167 | Live market prices never moved (parsed=0) | both spark answer shapes not read; bad User-Agent | build 182 | `tests/fix181_test.py`, `tests/fix179_test.py` | launcher | Y |
| R-168 | Live mode parsed fewer symbols than asked (Tadawul rows) | rows had only fulldayPrice; closed exchanges still asked | build 196 | `tests/step28b_test.py`, `tests/markets_page_test.py` | launcher | N |
| R-169 | Screensaver covered the update's second install question | the saver hold ended with our own package replace | build 183 | `tests/saver_hold_test.py` | launcher | Y |
| R-170 | Match card opened with no visible focus; OK acted on another button | browser focus differed from the drawn ring | build 182 | `tests/football_page.test.mjs`, `tests/fix179_test.py` | page | Y |
| R-171 | «اختر العملات»: «تم» blank, chosen currencies unmarked, focus lost | list squeezed the button; no state mark; no focus return | build 182 | `tests/markets_page_test.py` | launcher | Y |
| R-172 | Play Protect blocked installing the home app from Downloader | targetSdk 30 | build 182 | `tests/target_sdk_test.py` | launcher | Y |
| R-173 | Setup screen disappeared for most of the run after «ابدأ» | memory measurement pressed HOME 2 s into the run | build 183 | `tests/setup_runner_test.py` | launcher | Y |
| R-174 | «صرف العملات» showed no rates after a fresh install, for hours | CI fx file missing; Yahoo refused (429) without a browser UA | build 186 | `tests/step28b_test.py`, `tests/markets_page_test.py` | launcher | Y |
| R-175 | TV came back unmuted after its own sleep and after a restart | TV audio service unmutes on SCREEN_ON/boot | build 182 | `tests/mute_keep_test.py` | launcher, TCL | Y |
| R-176 | After a fresh install a restart came back unmuted | no mute record on the first start | build 183 | `tests/regressions/tester_guards_test.py` | launcher | Y |
| R-177 | 13th task typed on the TV was ignored silently | the 12-task limit was not said | build 182 (not re-checked) | `tests/fix181_test.py` | launcher | Y |
| R-178 | Holding «احفظ» with quantity 0 or no cost did nothing, said nothing | no hint for the missing field | build 182 (not re-checked) | `tests/markets_page_test.py` | launcher | Y |
| R-179 | WindowLeaked: converter dialog left open when HOME pressed | dialogs not closed on pause | build 182 | `tests/markets_page_test.py` | launcher | N |
| R-180 | Open markets page did not follow a language change | page not recreated on lang change | build 182 | `tests/markets_page_test.py` | launcher | Y |
| R-181 | Screensaver started over the markets page in live mode | live mode did not keep the screen on | build 182 | `tests/markets_page_test.py` | launcher | Y |
| R-182 | Gold card sparkline touched the price | no margin between line and price | build 182 | `tests/markets_page_test.py` | launcher | Y |
| R-183 | reset.sh printed two !! for RUN_IN_BACKGROUND appops | an allow that reads back as default counted as failure | build 182 | `tests/fix181_test.py` | optimize | N |
| R-184 | Setup plan and log never named the community list used | list name not written to the run lines | build 183+ (not re-checked) | `tests/fix181_test.py`, `tests/step28b_test.py` | launcher | N |
| R-185 | Long-press hint missing in English, not shown in Arabic | hint counter already spent on a used TV | build 188 | `tests/step28b_test.py` | launcher | Y |
| R-186 | «صرف العملات» and «أخفِ المبالغ» labels cut in half | fixed 80 px button height | build 183 | `tests/regressions/tester_guards_test.py` | launcher | Y |
| R-187 | Currency picker jumped back to the top after each pick | list rebuilt on every pick | build 183 | `tests/regressions/tester_guards_test.py` | launcher | Y |
| R-188 | Two new quick buttons grey-filled in «رملي», others white | new pills did not take the look | build 183 | `tests/regressions/tester_guards_test.py` | launcher | Y |
| R-189 | Widget title «مباريات اليوم» above tomorrow's games | fixed title regardless of day | build 183 | `tests/regressions/tester_guards_test.py` | page | Y |
| R-190 | Reciter letter index too small to read | letters at 11 sp | build 183 | `tests/reciter_order_test.py` | launcher | Y |
| R-191 | Station 0 stuck on «جارٍ التحميل…» forever, then failed silently | no start timeout; radiojar cleartext on targetSdk 34 | build 183 (message), build 186 (plays) | `tests/step28b_test.py`, `tests/radio_policy_test.py` | launcher | Y |
| R-192 | Adhan logged tv_muted_before=false on a muted TV; no UrTVAdhan lines | mute read after the unmute; adhan path did not log | build 186 | `tests/step28b_test.py`, `tests/fix159_test.py` | launcher | N |
| R-193 | «فرقك» and the team's game vanished after adding a team or quick reopen | team copy beat league copy; refused requests left nothing | build 191 | `tests/step28b_test.py`, `tests/football_page.test.mjs` | page | Y |
| R-194 | Number pad bottom row (⌫ ٠ ٫) cut at the bottom | glyph padding; baseline alignment pushed keys down | build 191 | `tests/step28b_test.py` | launcher | Y |
| R-195 | Stock card taller than screen: «رجوع» unreachable or blank, button squeezed | no scroll body or fixed footer; chart too tall | build 193 (رجوع), build 200 (fits) | `tests/step28b_test.py` | launcher | Y |
| R-196 | Home greeting said «تصبح على خير» at 19:18 | night started at Isha | build 191 (evening not re-checked) | `tests/step28b_test.py` | launcher | Y |
| R-197 | Markets header stayed on Tadawul and «أسعار متأخرة» while US prices were live | header ignored the US session and live state | build 188+ (not re-checked) | `tests/step28b_test.py` | launcher | Y |
| R-198 | US stock card: Tadawul hours on the axis, wrong open and range | Saudi session assumed for every symbol | build 193 (axis) | `tests/step28b_test.py` | launcher | Y |
| R-199 | Home stocks card mixed amounts (US) and percent (Saudi) | amount option applied to US rows only | build 188+ (not re-checked) | `tests/step28b_test.py`, `tests/page.test.mjs` | page | Y |
| R-200 | dock.sh cast status called the TCL a Chromecast; stray cast.txt error | cast provider alone matched; redirect order in cast.sh | build 193 | `tests/cast_test.py` | optimize, TCL | N |
| R-201 | Live tick redrew frames though prices were unchanged | header text redrawn every tick | build 193 | `tests/step28b_test.py` | launcher | N |
| R-202 | Home tray jank rose to about 31% | glow cross-fade stacked layers | build 196 | `tests/step28b_test.py` | launcher | N |
| R-203 | Cairo Quran radio never played | MediaTek sniffer starved by the slow stream before 20 s | build 200 (removed); 4.3.1 build 286 (back, ExoPlayer) | `tests/step28b_test.py`, `tests/radio_policy_test.py` | launcher | Y |
| R-204 | Screensaver page black, then blank, for 1-3 s before the page | window had no ground or last picture before the WebView | build 198 (no black); 233/235 (last picture) | `tests/step28b_test.py`, `tests/step52_test.py` | saver-app | Y |
| R-205 | Prayer card showed «—» on a cold start | times waited for the network answer | build 198 | `tests/step28b_test.py` | page | Y |
| R-206 | English screensaver: hard-edged dark patch at top middle | top veil moved side but gradient centre not flipped | build 198 | `tests/step28b_test.py`, `tests/page.test.mjs` | page | Y |
| R-207 | Screensaver stock prices hours old during Tadawul session | GitHub schedule not firing; an old file still trusted | build 198 (data loop, 20-min trust) | `tests/step28b_test.py` | page | Y |
| R-208 | S&P 500 row: percent and price touching, price pale when focused | no spacing; dim colour on the white pill | build 200 | `tests/step28b_test.py` | launcher | Y |
| R-209 | Photo packs list empty on next-channel builds | packs.json existed on master only | 4.3 (data, the day of step 38) | `tests/regressions/tester_guards_test.py` | page | Y |
| R-210 | «حقوق الصور»: file names garbled; «إغلاق» nearly invisible | no left-to-right isolate; pale button on pale | build 209 (4.3) | `tests/regressions/tester_guards_test.py` | saver-app | Y |
| R-211 | Packs shown pack after pack, not mixed | packs concatenated in order | build 209 | `tests/shuffle_test.py` | page | Y |
| R-212 | Looks page: empty glass mock, auto mock like dark, pack tile shows its id | mock cards hidden on dark; tile labelled with id | build 212 (4.3) | `tests/appearance_test.py` | saver-app | Y |
| R-213 | Looks page bottom row and «رجوع» at the screen's bottom edge | scroll viewport not inset to the safe area | build 222 | `tests/step46_test.py`, `tests/appearance_test.py` | saver-app | Y |
| R-214 | Phone page: send with an empty message did nothing, said nothing | no empty check | build 212 | `tests/regressions/tester_guards_test.py` | phone | Y |
| R-215 | «تطبيقات مقترحة» wider than the screen, title cut | 1000 dp column on a 960 dp TV | build 228 | `tests/step46_test.py` | saver-app | Y |
| R-216 | «كل الدوريات» catalogue took 54 s to open | 4 addresses x 12 s timeout; no cached copy | build 228 | `tests/step46_test.py` | saver-app | Y |
| R-217 | Leagues picker: «حفظ» unreachable in long list; second dialog dropped picks | no focus way to save; dialogs could open twice | build 228 (not re-checked) | `tests/step46_test.py` | saver-app | Y |
| R-218 | Every empty day said «لا مباريات اليوم» | one fixed empty line | build 228 | `tests/step46_test.py` | page | Y |
| R-219 | Catalogue country names in English (Czechia, United Kingdom, 20+) | no Arabic name for TheSportsDB country spellings | build 233 | `tests/league_catalogue_test.py` | saver-app | Y |
| R-220 | Home tray jank about 8% on every key | photo backdrop redrawn by the GPU every frame | build 260 (0.5%) | `tests/step52_test.py` | launcher | N |
| R-221 | Hub bottom row cut by its hint line; hub kept old language | fixed-height lines; no recreate on language change | build 228 | `tests/your_tv_hub_test.py` | saver-app | Y |
| R-222 | Update screen created twice when the card came back | UpdateActivity not singleTop, no debounce | build 222+ (not re-checked) | `tests/step47_test.py` | launcher | N |
| R-223 | «UrTV النظيف» row did not say on; clean screen came back over settings | state not read; clean screens kept in recents | build 228/233 | `tests/step47_test.py`, `tests/step52_test.py` | saver-app, launcher | Y |
| R-224 | Two «صوري» tiles with the same name, one above the other | both tiles labelled «صوري» | build 228 | `tests/step47_test.py` | saver-app | Y |
| R-225 | Quick-buttons box narrower after turning clean mode off | box width not restored | build 228 | `tests/regressions/tester_guards_test.py` | launcher | Y |
| R-226 | Quiz lobby address shown without the room code | RTL layout dropped the last part | build 228 | `tests/quiz_test.py` | saver-app | Y |
| R-227 | Quiz: locked join showed raw JSON; half the requests reset | no page for locked; flooding connections cut | build 228 | `tests/quiz_test.py` | phone | Y |
| R-228 | Quiz: hint line over answers; podium order and heights wrong | fixed layout overlapped; heights swapped | build 228 | `tests/quiz_test.py` | saver-app | Y |
| R-229 | Quiz used Arabic-Indic digits on a TV set to 0-9 | digits ignored the person's setting | build 228 (TV); build 235 (phone counters) | `tests/quiz_test.py`, `tests/step52_test.py` | saver-app, phone | Y |
| R-230 | Quiz question ended early with a silent player; phantom points | everyone-answered ignored quiet players; answer had no question tag | build 233 | `tests/quiz_test.py` | saver-app | Y |
| R-231 | Quiz answer letters off-centre; place 1 box missing its edge | letters centred on font line box, not ink | build 233 | `tests/quiz_test.py` | saver-app | Y |
| R-232 | Quiz: «انتهى الوقت» on early end, «الفائز» with all 0, gain «591+» | end texts and isolation missing | build 233/235 | `tests/quiz_test.py`, `tests/step52_test.py` | saver-app | Y |
| R-233 | Darker straight-edged box under the home stocks card | card style shadow drawn for the whole focus box | build 233 | `tests/step52_test.py` | page | Y |
| R-234 | Update page: auto-update switch flipped by a stray OK right under the row | no gap between rows | build 233 | `tests/step52_test.py` | saver-app | Y |
| R-235 | Settings focus came back on the row above after «UrTV النظيف» | focus restored before the list re-render | build 260 | `tests/step52_test.py` | saver-app | Y |
| R-236 | One OK on the match sheet's team button changed followed teams | no confirmation step | build 260 | `tests/football_test.py` | page | Y |
| R-237 | Games: big letter and headers cut by the time bar; English titles cut | fixed text sizes in fixed boxes | build 260 | `tests/games_kit_test.py` | saver-app | Y |
| R-238 | Games: «إنهاء الجولة» did not end; word not shown; double OK skipped | end-round started next round; no press guard | build 260 | `tests/games_kit_test.py`, `tests/party_games_test.py` | saver-app | Y |
| R-239 | «خمّن الكلمة» refused common words; no ة ى ء keys | guess list used as a gate; keyboard missing a row | build 260 | `tests/games_board_test.py` | saver-app | Y |
| R-240 | «خمّن الكلمة» 46% and «خلية الحروف» 19% janky frames | whole screen rebuilt on every key | build 260 | `tests/games_kit_test.py` | saver-app | N |
| R-241 | «خمّن الكلمة»: «ء» turned grey though never typed | ة ى ء coloured from the guess | build 262 | `tests/games_kit_test.py` | saver-app | Y |
| R-242 | «توحيد المظهر»: quick box darker, stocks card with bright rim and sheen | card style sheen and frame kept under sync | build 262 | `tests/football_test.py` | page, launcher | Y |
| R-243 | Clean-mode prayer card not centred; covered the settings button | card placement ignored the top boxes | build 262 | `tests/clean_prayer_card_test.py` | launcher | Y |
| R-244 | Factory page let the screen sleep while waiting for the USB question | no keep-screen-on while waiting | build 267 | `tests/factory_test.py`, `tests/fix7_test.py` | launcher | Y |
| R-245 | UrTV Player tile showed a plain «U»; TiviMate still suggested | no icon for the mirror entry; catalog kept TiviMate | build 274 | `tests/suggested_apps_test.py` | saver-app | Y |
| R-246 | «أطفئ الآن» did not say it needs the add-on/service enabled | tile text ignored the service state | build 274 | `tests/control_center_test.py` | launcher | Y |
| R-247 | Match sheet offered nothing when UrTV Player was missing | watch button hidden with no install path | build 274 | `tests/match_app_test.py` | page | Y |
| R-248 | «إعداد تلفزيوني» could not be found in apps or settings | no visible entry on this TV | build 286 (settings row) | `tests/setup_row_test.py` | saver-app | Y |
| R-249 | Live match card said «تبدأ بعد العشاء…»; «Shabab Al-/Ahli» split | upcoming wording for live games; breakable hyphen | build 181+ (not re-checked) | `tests/football_page.test.mjs` | page | Y |
| R-251 | Beta on with no beta build: update said it cannot update from the TV | a 404 on apk-beta read as CANNOT | 4.3.3 (build 458) | `tests/update_test.py` | launcher | Y |
| R-252 | «بدون» left the football home's weather, prayers and games widgets on | HomeCardChoice.football did not ask whether the cards were on | 4.3.4 | `tests/football_off_test.py` | launcher | Y |
| R-253 | The tester's report carried a Wi-Fi name, its BSSID and MAC addresses | device details pasted from a diagnosis into a tracked report | 4.3.3 (report fixed in f49cf8eb) | `tests/regressions/privacy_test.py` | docs / tester | N |
| R-254 | The home answered the remote late right after it was made again (a language change, recreate) | the first clock draw read the screensaver's settings on the UI thread (QA-1) | 4.3.4 | `tests/qa_434_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher (TCL) | Y |
| R-255 | The remote lagged at the return to the home and on a reminder card's OK | the reminder card read and wrote the screensaver's tasks provider on the UI thread (QA-3) | 4.3.4 | `tests/qa_434_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher (TCL) | Y |
| R-256 | While moving an app in «ترتيب المفضلة» the remote's app, colour and media keys did nothing | the move mode took every key (QA-5) | 4.3.4 | `tests/qa_434_test.py`, `tests/regressions/key_handlers_test.py` | launcher | Y |
| R-257 | After a cancelled PIN on «التطبيقات المخفية», the next «انقل» opened the hidden list or said there are none | the settings' hidden-list ask stayed set (review F1) | 4.3.4 | `tests/qa_434_test.py`, `tests/apps_edit_test.py` | launcher | Y |
| R-258 | Menu inside an edit mode opened «المصادر» over it | the Inputs branch did not check for an edit mode (review F2) | 4.3.4 | `tests/qa_434_test.py` | launcher | Y |
| R-259 | Setup page English while the home was Arabic; «2 apps chosen» for none (tester step 86/88) | the page language and the count were not read from the right state | 4.4 (build 386) | `tests/home_fix2_test.py` | launcher (setup) | Y |
| R-260 | Setup page larger than the screen, cut at the top; QR at the far right (tester step 86) | the setup window was not fitted to the screen | 4.4 (build 376) | `tests/home_fix_test.py` | launcher (setup) | Y |
| R-261 | Setup made a new ADB key at each start; the TV's key list filled up (tester step 86) | no single kept ADB key between the steps (and, owner's decision 4.4 review F20: deleted at «تراجع» and the factory restore, never after a normal done) | 4.4 (build 376) | `tests/setup_engine_test.py` | launcher (setup) | N |
| R-262 | «إلغاء» on the USB-debugging question: the setup hung or closed with no message (tester step 86) | the cancel was not handled as an answer | 4.4 (build 386) | `tests/setup_runner_test.py`, `tests/home_fix_test.py` | launcher (setup) | Y |
| R-263 | Google TV kept its home: UrTV never the home after the setup (tester steps 86-88) | Google TV took the home role back; no Home-key catch or window watch | 4.4 (build 413) | `tests/home_fix_test.py`, `tests/home_fix2_test.py`, `tests/home_fix3_test.py`, `tests/build413_test.py` | launcher + extras (Google TV) | Y |
| R-264 | The add-on was not bound after a reboot or a force-stop on the TCL (tester steps 85, 88) | nothing healed a listed but unbound accessibility service | 4.4 (build 413) | `tests/dock_sleep_test.py`, `tests/home_fix3_test.py`, `tests/extras_test.py` | extras (TCL) | Y |
| R-265 | The setup skipped the home step after two old failures (tester step 88) | old failures counted against a new run | 4.4 (build 413) | `tests/setup_engine_test.py` | launcher (setup) | Y |
| R-266 | Pressing Home quickly on our home paused the watch, and Google TV's home stayed (tester step 88) | a bring counted more than once per press | 4.4 (build 427) | `tests/home_fix3_test.py` | launcher (Google TV) | Y |
| R-267 | The setup intro's first focus was on «English»: one OK switched the language (tester step 88) | the first focus went to the language switch | 4.4 (build 427) | `tests/fix159_test.py` | launcher (setup) | Y |
| R-268 | Sideloaded apps missing from the home next to a Google home (GitHub #57) | apps without a TV activity were hidden | 4.4 | `tests/sideload_apps_test.py` | launcher (Google TV) | Y |
| R-269 | The home could stall at its return and every minute while it asked the add-on whether it runs (QA-6) | Extras.enabled: a provider call into the add-on's process on the UI thread | 4.4 | `tests/ui_thread_44_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher + extras (TCL) | Y |
| R-270 | The screensaver settings asked the home at every redraw (QA-7) | NowPlayingBridge.homeKey on the UI thread in render | 4.4 | `tests/ui_thread_44_test.py`, `tests/regressions/ui_thread_ipc_test.py` | saver-app | Y |
| R-271 | Pressing the adhan preview in «مركز التحكم» asked the screensaver on the UI thread (QA-8) | Prefs.playFor -> Place.get on the press | 4.4 | `tests/ui_thread_44_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher | Y |
| R-272 | «المباريات» asked the screensaver as it opened, to hand over kept follows (QA-9) | HomeFollows.sync in onCreate | 4.4 | `tests/ui_thread_44_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher | Y |
| R-273 | The notice bar read and parsed its file every minute on the UI thread (QA-10) | dueNotice -> ContentFile.read on the tick | 4.4 | `tests/ui_thread_44_test.py`, `tests/regressions/ui_thread_ipc_test.py` | launcher | N |
| R-274 | Play Protect blocked 4.3.4 («can request access to sensitive data») | the home still held the overlay permission (SYSTEM_ALERT_WINDOW, «فوق تطبيق آخر») after its accessibility services moved to the add-on | 4.4 | `tests/regressions/lessons_test.py`, `tests/kickoff_test.py`, `tests/iqama_test.py` | launcher + extras | N |
| R-275 | An LG-branded CVTE Android 9 set: the LG logo, then a black screen; the home died at every start | `new WebView` threw on a TV without a working system WebView, and the home card made one at each start | 4.3.5 | `tests/webview_guard_test.py` | launcher (CVTE, Android 9) | N |
| R-276 | A home that kept crashing or hanging right after start left the TV on a black screen | no crash-loop guard: two quick crashes or two starts without a first frame now open SafeActivity with the TV's own home | 4.3.5 | `tests/crash_loop_test.py` | launcher | N |
| R-277 | A call newer than Android 9 could crash the home on Android 8 and 9 TVs | minSdk 26 and lint does not stop the build: nothing compiled the home against API 28 | 4.3.5 | `tests/api28_compile_test.py` | launcher | N |
| R-278 | A community list could turn off a TV's own Settings app or its Live TV / channel app, leaving no way back to the inputs or the network (the comparison with Clean Your TV, docs/research/COMPARE-2026-11.md) | the guards asked the TV only for Home, inputs and keyboards, not for the apps that open Settings, a channel or the inputs' setup | 4.3.6 | `tests/input_guard_test.py`, `tests/setup_community_test.py` | optimize + launcher setup | N |
| R-279 | `com.google.android.sdksandbox` (the Google spelling of Android's SDK sandbox) was not core, so a list could name it | core.txt had only the `com.android.` spelling | 4.3.6 | `tests/regressions/lessons_test.py`, `tests/community_test.py` | optimize + launcher setup | N |
| R-280 | Community lists named a TV's own Settings, Live TV / channel and tuning apps (Whale OS Settings, Vestel's com.apps.channels, Beko/Grundig channels, Sony broadcast and tuning settings…); with one off a TV can lose its way back to Settings, its channels or its inputs | keep.txt did not name them, and the runtime guard (R-278) was not there yet | 4.3.6 | `tests/regressions/keep_list_test.py` | optimize + launcher setup | N |
| R-281 | The remote froze on our home on an Android 8.0 OTT box (Percee); uninstalling UrTV fixed it | the accessibility key filter asked for every key while our home was in front: «the system takes the Settings key» was always true on a box without Android TV's SystemUI, and Android 8.0 holds the input queue for the answer | 4.3.6 | `tests/settings_key_test.py`, `tests/api26_compile_test.py` | launcher + extras | N |
| R-282 | «I deleted the app and the screen became black»: UrTV removed from the TV's Settings on a TV whose stock home our setup had turned off left no home at all | the stock home off («عطّل واجهة المصنع», or a PC switch of before 4.3.3 never undone on the TV) with nothing to take Home after us | 4.4 | `tests/rescue_home_test.py`, `tests/factory_test.py`, `tests/rescue_test.py` | launcher + rescue + optimize | Y |
| R-283 | «إعداد تلفزيوني» stopped at 26 % every time on a Skyworth IRIS (Android 14) and the app closed with no word | the «before» reading restarted the TV and the run waited for a boot resume that did not come back; its end was kept only in memory and the setup screen could not come forward at a boot | 4.4 | `tests/setup_runner_test.py` | launcher setup | Y |
| R-285 | The safe page's «افتح الشاشة الأصلية» did nothing on a TV whose earlier setup had turned Google TV's home off (the owner's TCL G08, Android 14) | the button opened whatever else answered Home, and the stock home itself was off | 4.4 | `tests/safe_stock_test.py` | launcher | Y |
| R-289 | Screensaver start showed an OLD frame with the OLD time for about 0.8 s, then the real screen (owner, beta 1, build 525) | the start picture (SaverSnapshot, taken every 3 minutes, kept up to 24 h) carries the clock and was drawn over the page whatever its age | 4.4 | `tests/home_snapshot_test.py` | screensaver | Y |
| R-288 | Beta 1: «تحديث» in «تحديث تلفزيوني» failed with the generic «اطلب من مساعد الذكاء الاصطناعي» and no reason, and «تحديث جديد 4.4» stayed offered although the installed build was the newest (the owner's TV, build 525) | apk-beta held no `urtv-extras.apk`; the add-on (build 516) kept the lowest installed build below the newest for ever and its download answered 404 (`cannot update: download`) | 4.4 | `tests/update_optional_test.py`, `tests/update_notice_test.py`, `tests/beta_channel_test.py` | launcher | N |
| R-290 | Home after the games, then «تلفزيونك» in «مركز التحكم» opened the games again instead of the hub (beta-1, build 525) | the screensaver app keeps one task (the hub at its root, the games on top) and the plain «تلفزيونك» entry had no CLEAR_TASK, so it resumed that task | 4.4 | `tests/games_open_test.py` | launcher | Y |

## Open findings of the gate (QA-n)

Paths the UI-thread rule (`tests/regressions/ui_thread_ipc_test.py`) found already there at 4.3.4. They are in its baseline so the gate
passes today, each with a written reason (the test refuses the bare «baseline 4.3.4»); the findings carry their QA number (all fixed in 4.4); none was fixed by the gate itself. A fix removes the line from `tests/regressions/ui_thread_baseline.txt`
and adds a row above.

- **QA-1** `MainActivity.onResume -> updateClock`: the first draw read `ConfigSnapshot.read` on the UI thread. **Fixed in 4.3.4**
  (R-254, `tests/qa_434_test.py`): a home made again draws from the process's last answer; only a cold process's very first draw
  still reads (nothing on screen yet), and that one line stays in the baseline.
- **QA-2** the screensaver (`ScreenDream` / its `MainActivity`) `dispatchKeyEvent -> NowPlayingBridge.adhanState`: a cross-process query
  to the home on a key. **Fixed in 4.4** (2981706d, `tests/ui_thread_44_test.py`): the key decides from the adhan state the page's own ask
  (every 3 s, `adhan()`) or a thread last kept (`adhanKnown`, fresh for 8 s), never a query; the stop call goes on a thread. The first key of an
  adhan that began a second earlier may not know it yet, as the bar did not.
- **QA-3** the reminder card's refresh, the return's reschedule and OK on the card read and wrote the screensaver's tasks provider on the
  UI thread. **Fixed in 4.3.4** (R-255, `tests/qa_434_test.py`).
- **QA-4** `updateClock -> AdhanSync.onConfig -> Place.get`: **not a bug.** onConfig calls the 3-argument `Place.get`, which never
  queries; the graph mixed up the overloads and took `AdhanScheduler.schedule(c, p)` for an executor. Both fixed in `javagraph.py`.
- **QA-5** `DockActivity.dispatchKeyEvent` («ترتيب المفضلة»): while an app was moved every key was taken. **Fixed in 4.3.4**
  (R-256: FavEdit.owns, `tests/qa_434_test.py`).

- **QA-6** (4.4, R1, the TCL freeze's shape) `Extras.enabled` is a provider call into the add-on's process on the UI thread (cached 2 s):
  at the home's onResume and every minute tick (`updateHomeNudge -> A11yHeal.stuck`), and when «مركز التحكم» or «الإضافات» opens. A TCL
  that killed the add-on starts its process there. **Fixed in 4.4** (0f81bf1b, `tests/ui_thread_44_test.py`).
- **QA-7** (4.4, R1) the screensaver settings' render asks the home (`NowPlayingBridge.homeKey`) on the UI thread at every render
  (open, return, each toggle). **Fixed in 4.4** (ca54c843, `tests/ui_thread_44_test.py`).
- **QA-8** (4.4, R1, low) the control centre's adhan preview tile asks the screensaver for the place on the UI thread when pressed
  (`Prefs.playFor -> Place.get`). **Fixed in 4.4** (b8e3a8aa, `tests/ui_thread_44_test.py`).
- **QA-9** (4.4, R1, low) «المباريات»'s onCreate queries and calls the screensaver to hand over kept follows (`HomeFollows.sync`), only
  when the home kept some. **Fixed in 4.4** (15c4a306, `tests/ui_thread_44_test.py`).
- **QA-10** (4.4, R1, low) the home's notice bar reads and parses the notices file (`ContentFile.read`, own disk, bounded) at every
  minute tick and return (`updateHomeNudge -> dueNotice`). **Fixed in 4.4** (d0c0b9c0, `tests/ui_thread_44_test.py`).
- **QA-11** (R1, the TCL freeze's shape) the home's and its screens' reads of the screensaver's settings on the UI thread (`GlassSource.light` /
  `lookName` / `pageFor`, `Digits.init`, the pill row, the markets, matches, tasks and reminder screens). **Fixed in 4.4** (3f3fde22,
  `tests/ui_thread_44_test.py`): `ConfigSnapshot.ui` serves the known answer, else the last real one kept on disk (with its look, appearance and
  buttons), and a thread reads the fresh one; a change shows at the next draw, return or tick.
- **QA-12** (R1, low) one-off calls into the screensaver's or the home's provider from a screen's open or a press, on the UI thread.
  **Fixed in 4.4**: the tasks and reminder screens and their alarm (e26f9f9c); the matches page copy, the language choice in the first-run setup,
  the market toggles, the «تلاوة وأذان» prayer row and its alarm (e8e26069); the settings screens' backdrop, the hub's `CleanMask.state`, the
  «جرّب» whistle (2981706d). All on threads of their own, the result drawn when it comes (`tests/ui_thread_44_test.py`).
- **QA-13** (R1, low) the app's own disk work at a start or a render. **Fixed in 4.4**: the last-picture overlays, the glass backdrop, the
  first-frame wallpaper, the setup ledger, the model defaults and the report's `logcat` read (e8e26069); the screensaver's start picture, the
  settings' bundled pictures, the packs' names and credits, the published corrections (2981706d). Decoded or read on a thread; a picture that
  comes after its frame is not drawn (the home's overlay after 400 ms) or is drawn when it is here.

At 4.4 the baseline has no «open» line. What stays in it has a reason that says why: the WebView's own IO thread (`PhotoCache.serve`, the
card's `shouldInterceptRequest`), the update's and the phone page's own threads, and small `commit()` writes of the app's own prefs and the
safe-mode counter (own disk, no other process).

The 4.3.4 review's F1 and F2: R-257, R-258.

## Watch

- **Sony: the remote's APPS and GUIDE buttons after the clean-up.** Four Sony packages are turned off by community lists and are not in
  `keep.txt` (the supervisor's call for 4.3.4: they customise Sony's own home, which the clean-up is meant to remove, and none is a known
  key handler): `com.sony.dtv.tvlaunchercustomizer` (and `.overlay.apporder`), `com.sony.dtv.launcherxcustomizer`, `com.sony.dtv.moreapps`,
  `com.sony.dtv.sonyshelf`. If a Sony owner reports a dead APPS or GUIDE button, these are the candidates: add the one that answers it to
  `keep.txt` and a row here. Smoke S-20 checks it whenever a Sony TV is at hand.

## Known bugs, not fixed yet

Not regressions (they never worked); listed so a release review knows them. A fix adds a row above.

- Phone page keys did nothing on YouTube's profile picker: open; not re-checked since 3.152 (tester step 10)
- USB question never shows on first «جهّز كل شي»/factory page until a restart: open: adbd «Invalid base64 key» after install; last seen 4.4 and 4.3.3 (tester step 39, 60, 61, 62, 63, 86, 91)
- Home stocks card inner padding measured about 36 px, not 24: open: page pins 24 px but TV measured 36 on 183; not re-measured (tester step 28, 28b)
- ▲/▼ from «صرف العملات» left the currency view: open; never re-checked (tester step 28)
- Converter shows «٠٫٠٠٠٠٠٠» for 0; amount line has no separators: open; never re-checked (tester step 27)
- Next-prayer minute flips by one on a cold start: open; computed then fetched time differ (tester step 34)
- Markets header says «أسعار متأخرة» with 5-min-old data: open; old not-live wording (tester step 34, 35)
- English: matches «Started 4:25…» cut; 3-button stock card uneven: open; never re-checked (tester step 35)
- «رملي»: reciter letter index nearly invisible; reminder card over the clock: open; never re-checked (tester step 37)
- Screensaver «المباريات» card shows last evening's game after midnight: open; not seen again (no data) (tester step 38, 41)
- Setup «Done» did nothing; TV unmuted about 20 s after a restart: open; not re-checked (tester step 39)
- Control centre: last row title cut; «تخصيص» starts scrolled; Back closes all: open; panel redesigned later, not re-checked (tester step 46)
- Home tray jank back to 9-19% (was 0.5% on 260): open; 286 9.3%, 292 19%, 300 about 9% (tester step 71, 73, 76)
- League chooser tick boxes drawn empty in every state: open (tester step 71)
- UrTV Player tile gone from suggested apps; Open Shelf card past safe area: open (tester step 71)
- Radio stopped silently when the TV updated System WebView: open: home process killed, no error, no restart (tester step 71)
- Iqama bar drawn over the apps page's first row: open (tester step 71)
- Matches page source line runs into the big title: open (tester step 73)
- Dock of 3 apps leaves a wide empty bar; music.sh stations hides station 12: open (tester step 76)
- Setup result says the Home step failed while Home works: open: judged before the add-on bound; last seen 427 (tester step 88, 92)
- Reinstall loses the setup record: «تراجع» and rescue leave 18 apps off: open: record lives in app data (tester step 92)
- Rescue leaves our screensaver when nothing recorded the TV's own: open (design question) (tester step 93)
- ClassCastException in MusicReceiver on «music.sh radio N»: open: station read as String, sent as Integer (tester step 33)
- Settings key does nothing inside other apps; must leave app (HDR lost): Settings key handler lives in stock launcher, turned off by setup (GitHub #37)
- Screensaver flashes then closes immediately on TCL 65C7K Google TV: Model allows only Google's dream (system Colors dream closes too) (GitHub #42)
- Continue watching row never appears on Xiaomi: Unknown; Google TV may keep Watch Next private to Google home (GitHub #56)
