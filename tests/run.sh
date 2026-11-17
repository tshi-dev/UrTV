#!/usr/bin/env bash
# Every check in one go: static checks, then the screen in a real browser. See tests/check.py and page.test.mjs.
#   pip install fonttools brotli pillow -r .github/tools/cities-world/requirements.txt && npm install --no-save playwright@1.56.1 acorn@8.14.0 && npx playwright install chromium
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tests/check.py
python3 tests/arabic_spacing_test.py   # Arabic letters stay joined: no letter-spacing on anything that can hold Arabic
python3 tests/tilawah_test.py
python3 tests/prayer_math_test.py
python3 tests/prayer_crosscheck_test.py
python3 tests/prayer_crosscheck_world_test.py
python3 tests/hijri_crosscheck_test.py
python3 tests/prayer_method_test.py
python3 tests/place_choice_test.py
python3 tests/prayer_adj_test.py
python3 tests/hijri_offset_test.py
python3 tests/saver_hijri_test.py
python3 tests/upload_test.py
python3 tests/install_extras_test.py
python3 tests/share_test.py
python3 tests/install_versions_test.py
python3 tests/install_retry_test.py
python3 tests/install_stream_test.py   # 4.3.3: the APK streamed to `cmd package install -S`, else the file as before
python3 tests/rescue_test.py   # 4.4 «إنقاذ تلفزيونك» and the other ways back with no computer
python3 tests/homedoctor_test.py   # 4.4 the Home doctor (optimize/homedoctor.py): parsing, verdicts, a fake Google TV run
python3 tests/home_fix_test.py   # 4.4 step 86 B: the switch survives, the Home watch, one ADB key, «إلغاء» on the prompt, the small setup window
python3 tests/extras_test.py   # 4.3.3 «إضافات تلفزيونك»: the add-on with the accessibility service, the home without one
python3 tests/stock_kept_test.py   # 4.3.3: the computer's home switch keeps the stock home and the wizard on; the factory provider
python3 tests/cast_test.py   # «البث من الجوال»: a Chromecast keeps its setup wizard (the cast credentials); dock.sh cast on|off|status
python3 tests/step52_test.py   # the tester's step 52 findings in the source: the saver's first frame, the debug switches, the top row's layout, the dark patch, the clean row
python3 tests/party_games_test.py   # «واحد مو معنا» and «بدون كلام / وصّف»: the rules on a plain JVM, the bundled word files
python3 tests/ram_tier_test.py   # the three memory tiers (LOW under 2000 MB, MID under 2600, NORMAL), one rule per app, the test switch
python3 tests/scene_home_test.py   # 4.3.3 «المشاهد»: the scene rules per tier on a JVM, the Saudi pack's files and sizes, the Couch TV download checks, the wiring
python3 tests/settings_smooth_test.py   # 4.4 «smoother» settings: one section built per render, the side list opens on a rest, focus recolours only, 120 ms scale, live search
python3 tests/webview66_test.py   # #19 WebView 66: a fallback before each inset/clamp/min/max, flex gap as margins, the Hijri date without Umm al-Qura
python3 tests/motion_test.py   # 4.4 the same easing everywhere (Motion: 200 ms, couch-tv's curve; a fade with «الوضع الخفيف»)
python3 tests/sweep_test.py   # 4.4 the consistency sweep: what it fixed stays fixed
python3 tests/separators_test.py   # 4.4 no «·» or «•» on a screen, in either language (S.sep)
python3 tests/settings_couch_test.py   # 4.4 «منظر وساعة»: the settings in couch-tv's style (its numbers, row kinds, pages, search, thumbnails), every registry row, Arabic mirrored
python3 tests/settings_classic_test.py   # 4.4 «شكل كنبة» switches the settings screen too: every row in both layouts (4.3.2 and the side list), search in both, the switch in both
python3 tests/settings_tree_test.py   # docs/SETTINGS-TREE.md is what the code says now (.github/tools/settings_tree.py)
python3 tests/scene_zoom_test.py   # 4.4 the owner: the clock where it is, the picture lower: more of the digits above the scenery (the visible share on a JVM over the scene's real cut-out)
python3 tests/photo_masks_gate_test.py   # 4.4 the gate every cut-out passes before it is published: fringe, stairs, floating pieces, the digits' fit (FIT / FRONT-ONLY / REJECT)
python3 tests/photo_glass_test.py   # 4.4 «الساعة: زجاجية» over our photos: which cut-out, where the digits go (on a JVM), made only while chosen, never «الحرمين»
python3 tests/cache_sweep_test.py   # 4.3.3 «مسح الملفات المؤقتة»: the caches only, on a JVM over made-up folders
python3 tests/lowram_test.py   # a weak TV (Ram.low): no key filter, the clock off the UI thread, pictures let go behind apps, markets «عادي» and 60 s, #lowram=1
python3 tests/netlog_test.py   # G3: the request log (NetLog: requests by host, one UrTVNet line per 10 minutes with the bytes)
python3 tests/page_cache_test.py   # G3: the page revalidates its data (ETag, 304) through the WebView cache, and skips periodic loads while offline
python3 tests/net_cache_test.py   # G3: Net.cached: ETag and date revalidation, calls merged, the offline back-off, the size cap; the markets, next match and leagues list use it
python3 tests/photo_size_test.py   # G3: photos kept at the TV's own screen size (1280 on a 720p screen or a weak TV)
python3 tests/lite_mode_test.py   # A14: «الوضع الخفيف» auto (low memory or 2 cores) / on / off in both apps, its row and command
python3 tests/home_return_test.py   # G4: less before the home's first frame: no live TV lookup at a cold start; the provider, update record and changed packages off the return's path
python3 tests/deadman_test.py
python3 tests/run_lock_test.py
python3 tests/run_identity_test.py
python3 tests/run_report_test.py
python3 tests/setup_phone_test.py
python3 tests/requirements_pin_test.py   # optimize/requirements.txt: exact versions with hashes; UAD-ng at a fixed commit; check-packages exit codes
python3 tests/saver_city_input_test.py   # 4.3 R1-2/R1-3: lat and lon go together and stay on the Earth
python3 tests/phone_userland_test.py
python3 tests/adb_client_test.py
python3 tests/setup_engine_test.py
python3 tests/saver_hold_test.py
python3 tests/mute_keep_test.py   # a muted TV stays muted after its own sleep or a restart (step 27)
python3 tests/fix181_test.py   # step 27: the update flow end, tasks, reset, setup, markets sources
python3 tests/step28b_test.py   # step 28b: home card ▲, matches day, fx fallback, station retry, Tadawul fulldayPrice, adhan on a muted TV
python3 tests/reciter_order_test.py   # step 27: favourite reciters and the letter index
python3 tests/setup_runner_test.py
python3 tests/setup_prep_test.py
python3 tests/setup_community_test.py
python3 tests/setup_install_test.py
python3 tests/setup_report_test.py
python3 tests/type_test.py
python3 tests/tester_fixes_test.py   # the tester's steps 16 to 20 (TCL, build 174): the fixes without a JVM case of their own
python3 tests/setup_lang_test.py   # Arabic is the default everywhere; the installer's «العربية | English» switch
python3 tests/tray_check_test.py   # the favourites tray's rendered check: its rules on made-up dumps and the CI wiring
python3 tests/fix179_test.py   # build 179 re-check: the round buttons, Tilawah rows, PIN pad, settings gap, markets, football names
python3 tests/portfolio_window_test.py   # «محفظتي» and the phone: the extra never trusted, only the TV renews the window (4.2 review R02, R03)
python3 tests/reminder_fixes_test.py   # reminders: the next repeat stays on the alarm, overlapping ones each delivered once, calendar days over summer time (4.2 review R04, R07, R08)
python3 tests/spacing_178_test.py   # build 178 alignment: footers inside y 1026, gaps on the 8 px scale, matches and markets by keys
python3 tests/setup_i18n_test.py   # «إعداد تلفزيوني» in English: the start, plan and progress screens (build 178 GO)
python3 tests/stream_url_test.py   # the live stations: radiojar's https -> http redirect, qurango's random 500 tried again
python3 tests/radio_policy_test.py   # 4.3: every station through ExoPlayer, joined again quietly, a second source, «صيغة» only when it never played (scripted server)
python3 tests/update_ui_test.py   # the update screen: card, steps, bar, one button
python3 tests/album_card_test.py   # «ألبوم صوري»: the card of the own photos (page, saver, home)
python3 tests/home_nudge_test.py   # «خلّ تلفزيونك يفتح على UrTV»: the card while we are not the Home
python3 tests/look_tweaks_test.py   # «اللون والتحكم»: accents, tray strength, wallpaper dimming, focus style
python3 tests/photo_dim_test.py   # 4.4: the photo's darkness is --pdim, never the colour --dim (an opacity read from a colour is 1: a solid black photo)
python3 tests/security_43_test.py   # 4.3 security and speed round
python3 tests/appearance_test.py   # the look and backgrounds page
python3 tests/sync_look_test.py   # 4.3 «توحيد المظهر»: one material for the home's upper buttons, cards and apps bar in every look; off, each on its own
python3 tests/home_packs_test.py   # photo packs on the home, shuffled
python3 tests/update_notice_test.py   # the home offers the update screen when the screensaver is older or a newer build is out
python3 tests/shuffle_test.py   # the photo rotation is shuffled across «صورنا» and the packs
python3 tests/reboot_test.py
python3 tests/tv_identity_test.py
python3 tests/tv_read_only_test.py
python3 tests/tv_safety_test.py
python3 tests/tweaks_trim_test.py
python3 tests/tweaks_journal_test.py
python3 tests/settings_audit_test.py
python3 tests/power_test.py
python3 tests/familydns_test.py
python3 tests/family_dns_test.py
python3 tests/receiver_flags_test.py
python3 tests/target_sdk_test.py   # both apps target 34 (Play Protect): FGS types, exact alarms, background starts, the notifications ask
python3 tests/restore_scripts_test.py
python3 tests/power_fixes_test.py
python3 tests/settle_test.py
python3 tests/measure_transport_test.py
python3 tests/measure_space_test.py   # measure.sh with a run folder whose path has spaces (macOS Application Support)
python3 tests/restore_complete_test.py
python3 tests/reset_restore_rc_test.py
python3 tests/failclosed_test.py
python3 tests/key_home_test.py
python3 tests/dock_names_test.py
python3 tests/dock_input_names_test.py
python3 tests/input_names_test.py
python3 tests/input_display_test.py
python3 tests/tv_vendor_test.py   # «المصادر» on any TV: the live TV app and source picker found on the TV (TCL, CultraView, MediaTek…)
python3 tests/dock_sleep_test.py
python3 tests/find_tv_test.py
python3 tests/wireless_test.py
python3 tests/precheck_test.py
python3 tests/install_saver_webview_test.py
python3 tests/autostart_test.py
python3 tests/find_team_test.py
python3 tests/team_search_test.py
python3 tests/team_ids_test.py
python3 tests/city_match_test.py
python3 tests/cities_world_test.py
python3 tests/cities_more_test.py   # 4.3.3 the offline city search: the list (GeoNames + Wikidata), its generator, the ranking
python3 tests/cities_md_test.py
python3 tests/home_card_test.py
python3 tests/home_page_test.py   # the home's cards on the screensaver's newer page (its stocks stayed stale on the APK's own)
python3 tests/perf_idle_test.py
python3 tests/league_fixtures_test.py
python3 tests/match_archive_test.py   # 4.3.1 the match archive: append-only, a corrected score, the live window, the backfill names
python3 tests/sports_files_test.py   # 4.4 the sports cards' files: sports/<league>.json from the store, f1/next.json once a day
python3 tests/kickoff_test.py   # 4.4 «تنبيه بداية المباراة»: the rules on a JVM (KickoffRun.java), the whistle, the defaults, the settings, saver.sh and dock.sh overlay
python3 tests/league_catalogue_test.py   # 4.3 «كل الدوريات»: the CI catalogue (the walk, the content rules, the Arabic names)
python3 tests/catalogue_leagues_test.py   # 4.3 «كل الدوريات» on the TV: read only when the picker opens, picked ones only, 8 at most
python3 tests/data_host_test.py   # 4.3.3 the data files: tshi-dev/urtv-data first, the old data branch second, one map in both apps and the page
python3 tests/prayer_fix_test.py   # 4.3.3 published prayer corrections: by country and city, by date, never over the person's own minutes, through adj=
python3 tests/seasonal_test.py   # 4.3.3 seasonal photo packs: only between their days, first in the picker, licensed, the same checks as every pack
python3 tests/device_rule_test.py   # 4.3.3 per-model defaults: matched by brand, model, device and SDK, applied only where nothing is set
python3 tests/notice_test.py   # 4.3.3 remote notices: one at a time, once per id, never over playback, no links
python3 tests/market_prices_test.py
python3 tests/markets_page_test.py   # «الأسواق»: the data and the sums on a JVM, only the sources we have, «الأخبار» off, the ways in
python3 tests/market_names_test.py   # 4.4 (#60): the home names every code the page names (MarketNames.java is generated from the page's maps)
python3 tests/portfolio_privacy_test.py   # «محفظتي» stays on the TV: share.py, Report, settings_move, ConfigProvider, the commands
python3 tests/stocks_search_test.py   # «قائمة الأسهم»: a Tadawul code, ٢٢٢٢ or a company name, and the match shown
python3 tests/adhkar_tool_test.py
python3 tests/saver_tasks_test.py
python3 tests/saver_quote_test.py
python3 tests/quote_every_test.py
python3 tests/user_options_test.py   # «الآية والحديث», «إظهار الثواني», «الخروج من تلفزيونك»: off by default
python3 tests/photo_interval_test.py
python3 tests/photo_packs_test.py   # photo packs: sha256 before use, safe names, the index, the picker
python3 tests/make_pack_test.py   # the pack builder: the content rule and the zip
python3 tests/hide_parts_test.py
python3 tests/update_row_test.py
python3 tests/haram_live_test.py
python3 tests/settings_move_test.py
python3 tests/saver_off_test.py
python3 tests/saver_reload_test.py
python3 tests/listen_pill_test.py
python3 tests/updates_bell_test.py
python3 tests/versions_test.py
python3 tests/version_name_test.py
python3 tests/home_tile_test.py
python3 tests/back_saver_test.py
python3 tests/back_saver_row_test.py
python3 tests/channel_test.py
python3 tests/app_fixes_test.py
python3 tests/keep_test.py
python3 tests/keep_restore_test.py
python3 tests/community_test.py
python3 tests/input_guard_test.py
python3 tests/reset_screensaver_test.py
python3 tests/reset_tweaks_test.py
python3 tests/task_voice_test.py
python3 tests/task_ids_test.py
python3 tests/task_typed_test.py
python3 tests/render_gone_test.py
python3 tests/sleep_boot_test.py
python3 tests/photo_prep_test.py
python3 tests/photos_sh_test.py
python3 tests/music_adhan_own_test.py
python3 tests/own_adhan_test.py
python3 tests/own_adhan_keep_test.py
python3 tests/player_logic_test.py
python3 tests/stall_watch_test.py   # 4.3 A2: the live station's stall watchdog and ExoPlayer for HLS
python3 tests/focus_rule_test.py   # 4.3.3 G1: the player keeps on behind the screensaver and the TV's settings; another sound pauses it and it comes back
python3 tests/surah_set_test.py   # 4.3.3: a reciter with «missing» surahs; the player steps over them
python3 tests/audio_library_test.py   # 4.4 «صوتياتي»: sound files from a USB stick or the phone, kept in the app; the caps; the own adhan from them
python3 tests/dua_test.py   # 4.4 «الأدعية والتذكيرات»: the rule (days, both calendars, prayer times), the alarm, playing like the adhan, the card
python3 tests/prayer_sound_test.py   # 4.4 «أذان كل صلاة»: silent prayers, a muezzin or own recording per prayer
python3 tests/quiet_hours_test.py   # 4.4 touch «ساعات الهدوء»
python3 tests/kahf_template_test.py   # 4.4 touch: the Friday Kahf, ready
python3 tests/fade_in_test.py   # 4.4 touch «يبدأ بهدوء»
python3 tests/friday_dhuhr_test.py   # 4.4 touch «أذان الجمعة»
python3 tests/black_screen_test.py   # 4.4 #62 «شاشة سوداء»: listening with the picture off
python3 tests/loudness_test.py   # the recitation and the adhan lifted inside the player (LoudnessEnhancer), never the TV volume or mute
python3 tests/seek_burst_test.py
python3 tests/pill_text_test.py
python3 tests/control_center_test.py
python3 tests/tasks_card_test.py
python3 tests/reminders_test.py   # «المهام» with a time: the alarm, the cards, the keys, saver.sh, the phone's date and time (the page's card: reminder_page.test.mjs)
python3 tests/now_playing_test.py
python3 tests/digits_text_test.py
python3 tests/page_stamp_test.py
python3 tests/greeting_test.py
python3 tests/netgate_test.py
python3 tests/clock_text_test.py
python3 tests/clock_setting_test.py
python3 tests/saver_clock24_test.py
python3 tests/card_box_test.py
python3 tests/glow_look_test.py
python3 tests/unsplash_header_test.py
python3 tests/bounded_test.py
python3 tests/adhan_bar_test.py
python3 tests/adhan_keys_test.py
python3 tests/adhan_options_test.py
python3 tests/fav_edit_test.py
python3 tests/apps_edit_test.py
python3 tests/app_actions_test.py
python3 tests/menu_dots_test.py
python3 tests/remote_keys_test.py
python3 tests/key_apps_test.py
python3 tests/settings_key_test.py
python3 tests/remote_nav_test.py
python3 tests/dpad_rtl_test.py
python3 tests/home_grid_test.py
python3 tests/spring_test.py
python3 tests/look_test.py
python3 tests/tv_rows_test.py
python3 tests/adhan_reply_test.py
python3 tests/clock_zone_test.py
python3 tests/zone_fix_test.py   # 4.3 issue #55: every native time from ZoneFix (a recent tz table, the TV's data, the page's word): Morocco +00, fake old data
python3 tests/phone_test.py
python3 tests/pairing_screen_test.py
python3 tests/glass_test.py
python3 tests/type_into_test.py
python3 tests/fix7_test.py
python3 tests/fix159_test.py
python3 tests/a11y_heal_test.py
python3 tests/saver_cards_test.py
python3 tests/phone_qr_test.py
python3 tests/update_test.py
python3 tests/update_cover_test.py   # step 41: the screensaver's cover over the home app's own replace (no last app in front)
python3 tests/suggested_apps_test.py   # 4.3 «تطبيقات مقترحة»: the catalog, the pinned IPTV player, the settings screen, the setup boxes
python3 tests/player_update_test.py   # 4.3 UrTV Player updates from inside it: the provider (the Player with its key only), INSTALL_PLAYER, urtv-player.version
python3 tests/phone_draw_test.py   # 4.3 #53 «ارسم»: live strokes from the phone over the TV's screen
python3 tests/lang_provider_test.py   # 4.3: «اللغة» read only for UrTV Player (content://sa.urtv.home.lang/lang)
python3 tests/match_app_test.py   # 4.3 «شاهد المباراة»: the match app and the FIND_MATCH contract
python3 tests/update_channel_test.py
python3 tests/android_compile_test.py
bash tests/android_compile.sh   # both apps against android-all API 34; SKIP (not a failure) when offline or the jar is not cached
python3 tests/countdown_text_test.py
python3 tests/month_names_test.py
python3 tests/focus_key_test.py
python3 tests/settings_hub_test.py
python3 tests/on_off_test.py   # on/off rows: the owner's choice I (green triangles and word), both apps
python3 tests/parental_text_test.py   # «الرقابة الأبوية»: the card and its two buttons, the count of locked apps (docs/UX-REVIEW.md)
python3 tests/fav_bar_test.py   # the favourites bar: the tray, five tiles, the focus inside it, the one 40 px move (docs/PROPOSALS-NOV2.md §1)
python3 tests/called_off_test.py   # 4.3 A1-A3: a called-off game is never live or upcoming; a team's game read from the copy further on
python3 tests/usb_test.py   # 4.3 B8: the USB viewers are never off and always in the apps row (issue #50)
python3 tests/sideload_apps_test.py   # #57: a sideloaded app shows on the home next to a Google home (only the TV's own phone-only helpers stay out)
python3 tests/kids_exit_test.py   # 4.3.3: kids mode always has a way out to «الرقابة الأبوية» (the long OK box, Menu, star, a long Settings key), behind the PIN
python3 tests/date_num_test.py   # the home's date with its year; «التاريخ بالأرقام» on the home and the screensaver
python3 tests/global_search_test.py   # «بحث» on the home: the TV's own search inside the apps, off by default
python3 tests/report_words_test.py   # 4.3 B10: the report page asks what happened, required
python3 tests/phone_message_test.py   # 4.3 #53: a short message from a paired phone on the TV's screen
python3 tests/photos_phone_test.py   # 4.3 «صوري»: 50, and from a paired phone (no camera data)
python3 tests/own_adhan_fajr_test.py   # 4.3 C16: a second own adhan, for Fajr
python3 tests/football_test.py   # the football home and «المباريات»: the setting, the pages, the widgets' focus, the tray's move, the broadcast table (docs/PROPOSALS-NOV2.md §1)
python3 tests/three_teams_test.py   # 4.3.3 A3: three followed teams through every step (saver Prefs, the provider, the launcher's link, the page)
python3 tests/follow_store_test.py   # 4.3.3 following from «المباريات» with the screensaver app missing or older: the home keeps it, joins it, hands it over
python3 tests/settings_report_test.py   # «أرسل تقرير»: share.py's allowlist, nothing personal
python3 tests/dialog_fit_test.py
python3 tests/second_card_test.py
python3 tests/pin_store_test.py
python3 tests/pin_recovery_test.py
python3 tests/parental_policy_test.py
python3 tests/parental_wiring_test.py
python3 tests/dock_pin_test.py
python3 tests/i18n_test.py   # «اللغة»: the translation table, its completeness, Arabic byte for byte (docs/ENGLISH.md)
# English phase 1 (the screensaver app): its new tests go right under this line
python3 tests/i18n_saver_test.py   # the settings in English (Words, WordMatch, Places), Arabic as it was, «اللغة» in «المزيد»
#
# English phase 2 (the page): its new tests go right under this line
python3 tests/i18n_page_test.py   # dist/tv-en.html: built beside tv.html (byte for byte), current, under budget; the app and the home card pick it
node tests/es2017_parse.test.mjs   # #19: the built pages parse as ES2017 (Android 9 TVs stuck on WebView 66)
node tests/date_num.test.mjs   # «التاريخ بالأرقام» on the screensaver page: day/month/year in the digits chosen
node tests/wx_icon.test.mjs   # the screensaver's weather line: the condition's icon beside the temperature
node tests/js_strip_test.mjs   # the pages ship without the script's comments: the same program (acorn trees), 50 KB smaller
node tests/i18n_page_test.mjs   # dist/tv-en.html in a browser: left to right, no Arabic in its own words on any card
#
# English phase 3 (the home app): its new tests go right under this line
python3 tests/i18n_home_test.py   # the home's words: none left behind, Arabic byte for byte (tests/i18n_home_cases.tsv), English with no Arabic, Dir, the Quran's names
#
# English phase 4 (the phone page): its new tests go right under this line
python3 tests/i18n_phone_test.py   # phone/index.en.html: generated by src/phone_en.py and current, under 64 KB, no Arabic of its own; the link; PhoneHost serves «اللغة»
node tests/i18n_phone_test.mjs   # phone/index.en.html in a browser: left to right, English on every tab, the link both ways; docs/shots/phone-en.png
#
# «الألعاب» (docs/QUIZ.md, docs/GAMES-KIT.md, docs/GAMES-DATA.md): each game's session adds its own test right under this block's last line
python3 tests/quiz_test.py   # «الأسرع»: the room, the picking, the server and the bank on a plain JVM, and the TV side's pins
node tests/quiz_phone.test.mjs   # «الأسرع»: the phone page at 360 px in a browser
node tests/quiz_e2e.test.mjs   # «الأسرع»: two phones against the real room and server on the loopback
python3 tests/games_data_test.py   # the games' data: the forms, the limits, the bundled starters
python3 tests/games_kit_test.py   # the games' kit, the registry and the list, «خمس خانات» and «الكلمة المفقودة» (their rules on a plain JVM)
python3 tests/domino_test.py   # «دومينو»: the rules and the computer players on a plain JVM, whole games from seeds (docs/DOMINO.md)
#
python3 tests/tvports_test.py   # BETA LG/Samsung packages: manifests, files, sizes (docs/LG-SAMSUNG.md)
python3 tests/games_board_test.py   # «خلية الحروف» and «خمّن الكلمة»: both engines on a plain JVM against the bundled data
node tests/prayer_crosscheck_page.mjs
node tests/prayer_crosscheck_world_page.mjs
node tests/hijri_crosscheck_page.mjs
node tests/page.test.mjs
node tests/tafsir.test.mjs   # «التفسير الميسر» (#tafsir=1): every verse of the deck has its tafsir, whole, in pages of three lines at most, never «…»
node tests/football_page.test.mjs   # the football home's widgets and «المباريات» in a browser, on fixed games
node tests/sports_cards.test.mjs   # 4.4 «الرياضة»: the sports cards (dist/sports.js) on the home, the couch row and the screensaver, the takeover, only the page's own build
node tests/matches_archive.test.mjs   # «المباريات» with the match archive (data-archive): every followed team, yesterday's results, a club, a league, the chooser
node tests/phone_page.test.mjs
node tests/sync_cards.test.mjs   # «توحيد المظهر» on the page: the cards in the material's fill, line and ink, readable on a light look
node tests/couch_row.test.mjs   # 4.4 the page: «كنبة» (#row=1, the cards in one strip above the tray) and «مربعات» (today's prayers as boxes in a row, the next one lit with its countdown)
node tests/card_title.test.mjs   # 4.4 the owner: a home card's title clear of its rounded corner and the rule, at the start, in every style and layout
node tests/photo_mask_page.test.mjs   # 4.4 the phone page «صوري»: a photo with its mask, checked on the phone, previewed, sent after the photo; the page's own CSP
node tests/reminder_page.test.mjs   # the screensaver's reminder card: its time, «الآن», «تم», the snooze, the keys, the adhan, English
node tests/tvports_page.test.mjs   # the packaged page with simulated webOS and Tizen keys
