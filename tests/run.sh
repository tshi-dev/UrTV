#!/usr/bin/env bash
# Every check in one go: static checks, then the screen in a real browser. See tests/check.py and page.test.mjs.
#   pip install fonttools==4.68.0 brotli==1.2.1 pillow==13.0.0 cryptography==50.0.2 numpy==2.5.5 scipy==1.18.1 opencv-python-headless==4.14.0.94 -r .github/tools/cities-world/requirements.txt && npm install --no-save playwright@1.56.1 acorn@8.14.0 && npx playwright install chromium
set -euo pipefail
cd "$(dirname "$0")/.."
# «regressions»: the release gate's group alone (docs/dev/REGRESSIONS.md, docs/dev/RELEASE.md): bash tests/run.sh regressions
if [ "${1:-}" = "regressions" ]; then shift; exec python3 tests/regressions/run.py "$@"; fi
python3 tests/check.py
python3 tests/arabic_spacing_test.py   # Arabic letters stay joined: no letter-spacing on anything that can hold Arabic
python3 tests/tilawah_test.py
python3 tests/no_banned_names_test.py   # the owner: no Palestine and no Israel in any game or quiz
python3 tests/hadith_number_test.py   # Sahih Muslim: the standard number, never the dataset index
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
python3 tests/home_fix2_test.py   # 4.4 steps 87-88: the watch counts one bring per press, wanted, the dialog, the language, R3 off
python3 tests/home_fix3_test.py   # 4.4 step 88 (build 386): R2g, never skip the home step, TCL appops, the result page, «تراجع»
python3 tests/compact_test.py   # 4.4 compact: every size of the setup, update, install, rescue and add-on screens
python3 tests/home_fix_test.py   # 4.4 step 86 B: the switch survives, the Home watch, one ADB key, «إلغاء» on the prompt, the small setup window
python3 tests/home_update_key_test.py   # 4.4 beta 2: the first Home after an update of the home opens ours (the add-on's catch judged again)
python3 tests/pair_commands_test.py   # 4.4 beta 3: the paired route only for our home with our certificate, and only the setup's own commands (by shape)
python3 tests/pair_test.py   # 4.4 beta 3: wireless debugging in the add-on only (pinned libraries), the home through its provider, 5555 first, fails closed
python3 tests/extras_test.py   # 4.3.3 «إضافات تلفزيونك»: the add-on with the accessibility service, the home without one
python3 tests/stock_kept_test.py   # 4.3.3: the computer's home switch keeps the stock home and the wizard on; the factory provider
python3 tests/cast_test.py   # «البث من الجوال»: a Chromecast keeps its setup wizard (the cast credentials); dock.sh cast on|off|status
python3 tests/step52_test.py   # the tester's step 52 findings in the source: the saver's first frame, the debug switches, the top row's layout, the dark patch, the clean row
python3 tests/party_games_test.py   # «واحد مو معنا» and «بدون كلام / وصّف»: the rules on a plain JVM, the bundled word files
python3 tests/ram_tier_test.py   # the three memory tiers (LOW under 2000 MB, MID under 2600, NORMAL), one rule per app, the test switch
python3 tests/lite_auto_test.py   # 4.4 beta 3: «الوضع الخفيف» تلقائي also from what this TV shows (memory low in front, a slow first frame, a low-memory kill): said once, never on the UI thread
python3 tests/matches_today_test.py   # «بطاقة المباريات لليوم فقط» (PR #65): the setting from the command to the page
python3 tests/scene_blank_test.py   # 4.4 beta 4: no scene draws: logged, and the wall photos instead of a black home
python3 tests/scene_home_test.py   # 4.3.3 «المشاهد»: the scene rules per tier on a JVM, the Saudi pack's files and sizes, the Couch TV download checks, the wiring
python3 tests/presets_test.py   # 4.4 «أشكال جاهزة»: six looks, the person's own values kept and put back, «مخصّص», the flags, saver.sh preset
python3 tests/saver_flush_test.py   # R-305: «saved» only after the settings file is written to the TV's storage (a lost write came back after a power cycle)
python3 tests/settings_smooth_test.py   # 4.4 «smoother» settings: one section built per render, the side list opens on a rest, focus recolours only, 120 ms scale, live search

python3 tests/webview66_test.py   # #19 WebView 66: a fallback before each inset/clamp/min/max, flex gap as margins, the Hijri date without Umm al-Qura
python3 tests/tafsir_timer_test.py   # 4.4 review: a redraw of another card does not stop the paged tafsir (the timer is per list)
python3 tests/motion_test.py   # 4.4 the same easing everywhere (Motion: 200 ms, couch-tv's curve; a fade with «الوضع الخفيف»)
python3 tests/sweep_test.py   # 4.4 the consistency sweep: what it fixed stays fixed
python3 tests/separators_test.py   # 4.4 no «·» or «•» on a screen, in either language (S.sep)
python3 tests/settings_couch_test.py   # 4.4 «كنبة»: the settings in couch-tv's style (its numbers, row kinds, pages, search, thumbnails), every registry row, Arabic mirrored
python3 tests/settings_one_form_test.py   # 4.5 one settings form for every look (Prefs.ONE_FORM): every row and tile of the 4.3.2 hub listed with its new place
python3 tests/look_switch_test.py   # 4.5 a look applied whole: one write and signal, HomeLook's cover and one fade (a cut when light), undo on failure, no black card, focus kept, «رجّع الشكل السابق»

python3 tests/settings_classic_test.py   # 4.4 «شكل كنبة» switches the settings screen too: every row in both layouts (4.3.2 and the side list), search in both, the switch in both
python3 tests/settings_tree_test.py   # docs/SETTINGS-TREE.md is what the code says now (.github/tools/settings_tree.py)
python3 tests/version_tap_styles_test.py   # the owner's pass: the version note is wired to versionTap the same in every settings style and layout
python3 tests/scene_zoom_test.py   # 4.4 the owner: the clock where it is, the picture lower: more of the digits above the scenery (the visible share on a JVM over the scene's real cut-out)
python3 tests/photo_masks_gate_test.py   # 4.4 the gate every cut-out passes before it is published: fringe, stairs, floating pieces, the digits' fit (FIT / FRONT-ONLY / REJECT)
python3 tests/photo_glass_test.py   # 4.4 «الساعة: زجاجية» over our photos: which cut-out, where the digits go (on a JVM), made only while chosen, never «الحرمين»
python3 tests/cache_sweep_test.py   # 4.3.3 «مسح الملفات المؤقتة»: the caches only, on a JVM over made-up folders
python3 tests/sources_rule_test.py   # 4.4 «مصادر البيانات» under «حول»: each source, its terms, its row; the page's 60%-and-half-the-light rule
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
python3 tests/update_optional_test.py   # 4.4 R-288: an optional package whose file is absent at the release (the add-on on apk-beta) is skipped, never counted as behind and never a failed download
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
python3 tests/input_fav_test.py
python3 tests/look_panel_affinity_test.py
python3 tests/audio_adhan_thread_test.py
python3 tests/pair_pool_test.py
python3 tests/pair_retry_test.py
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
python3 tests/own_place_test.py
python3 tests/cities_world_test.py
python3 tests/cities_more_test.py   # 4.3.3 the offline city search: the list (GeoNames + Wikidata), its generator, the ranking
python3 tests/cities_md_test.py
python3 tests/home_card_count_test.py   # 4.4 beta 4: «بدون» counts no card (the glass clock comes back)
python3 tests/home_card_test.py
python3 tests/night_rebuild_test.py
python3 tests/football_off_test.py   # 4.3.4: «بدون» turns the football home's widgets off
python3 tests/home_page_test.py   # the home's cards on the screensaver's newer page (its stocks stayed stale on the APK's own)
python3 tests/perf_idle_test.py
python3 tests/league_fixtures_test.py
python3 tests/match_archive_test.py   # 4.3.1 the match archive: append-only, a corrected score, the live window, the backfill names
python3 tests/sports_files_test.py   # 4.4 the sports cards' files: sports/<league>.json from the store, f1/next.json once a day
python3 tests/honours_study_test.py   # 4.5 honours study: a season's champion from our archive only when sure (both legs of a final, never MLS's table leader)
python3 tests/honours_test.py   # 4.5 honours: champions and top scorers from the archive, Wikidata and Wikipedia, weekly (docs/HONOURS-STUDY.md)
python3 tests/kickoff_test.py   # 4.4 «تنبيه بداية المباراة»: the rules on a JVM (KickoffRun.java), the whistle, the defaults, the settings, saver.sh and dock.sh overlay
python3 tests/overlay_offer_test.py   # 4.4 the add-on's «فوق تطبيق آخر» offered once on the first run (rule on a JVM, the bar, the once-flag, the changelog line)
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
python3 tests/connectivity_keep_test.py
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
python3 tests/adhan_keep_test.py   # 4.4 beta 3: the chosen adhan recordings kept in the app files, a saved voice stands in, a missed adhan said
python3 tests/player_logic_test.py
python3 tests/stall_watch_test.py   # 4.3 A2: the live station's stall watchdog and ExoPlayer for HLS
python3 tests/net_reconnect_test.py   # 4.4 radio: bigger buffer, pre-buffer, join again when the network is back, «يعيد الاتصال…»
python3 tests/station_sources_test.py   # 4.4 radio: every source of a station in turn (alt, mirrors)
python3 tests/music_stations_test.py   # 4.4 beta 4: music.sh stations lists the installed app's stations; start-saver.sh waits for a slow dream
python3 tests/station_status_test.py    # 4.4 radio: a station dead three checks in a row is hidden (stations/status.json)
python3 tests/radio_week_test.py        # 4.4 radio: the stations' troubles of the last 7 days, kept and reported
python3 tests/surah_recover_test.py      # 4.4: a surah through ExoPlayer (file buffer, Range resume), on from its place, no loop of prepares
python3 tests/adhan_level_test.py        # 4.4 «مستوى صوت الأذان»: 0-100 or like the TV, per prayer; «كل الصلوات بصوت عدا الفجر»
python3 tests/iqama_test.py             # 4.4 «الإقامة»: a sound and a notice at the iqama (off by default; never in kids mode over other apps)
python3 tests/audio_upload_test.py      # 4.4 «صوتياتي»: one upload at a time, no lock held over a copy, the iqama on delete, the page's words
python3 tests/surah_places_test.py      # 4.4 «تلاوة وأذان»: each surah its place, saved while it plays, the sleep timer and the adhan, the letters, the card
python3 tests/dua_time_test.py         # 4.4 the duas after midnight, on a DST day, the Hijri shift; the adhan's previews
python3 tests/focus_rule_test.py   # 4.3.3 G1: the player keeps on behind the screensaver and the TV's settings; another sound pauses it and it comes back
python3 tests/surah_set_test.py   # 4.3.3: a reciter with «missing» surahs; the player steps over them
python3 tests/audio_library_test.py   # 4.4 «صوتياتي»: sound files from a USB stick or the phone, kept in the app; the caps; the own adhan from them
python3 tests/dua_test.py   # 4.4 «الأدعية والتذكيرات»: the rule (days, both calendars, prayer times), the alarm, playing like the adhan, the card
python3 tests/prayer_sound_test.py   # 4.4 «أذان كل صلاة»: silent prayers, a muezzin or own recording per prayer
python3 tests/prayer_screen_test.py   # 4.4 beta 4 «الأذان لكل صلاة»: each prayer's screen apart from its sound (the bar, the notice, the phone, music.sh)
python3 tests/prayer_summary_test.py   # 4.4 beta 4: one summary rule for the settings row and the player's row, driven by one case table
python3 tests/adhan_from_file_test.py   # 4.4 beta 4 «أذاني من ملف»: the last row of «المؤذن», «التنزيلات» beside the USB stick
python3 tests/photo_from_file_test.py   # 4.4 beta 4: photos from a stick or Downloads into «صوري», the screensaver app with no storage permission
python3 tests/tester_auto_test.py   # 4.4 beta 4: the automatic tester round (tester/auto): its parsing on saved answers, the whole round on a stand-in TV, the putting back, never a blind key
python3 tests/pray_rows_test.py   # 4.4 beta 4: «المؤذن», «الأدعية والتذكيرات», «صوتياتي» as rows of «الصلاة والأذان», the dua count from the phone
python3 tests/music_adhan_state_test.py   # 4.4 beta 4: `music.sh adhan state <prayer> <state>` is reached (R-333); `adhan state` alone is still the status
python3 tests/setup_cancel_test.py   # 4.4 beta 4 review F87 (R-262): «إلغاء» is its own message with its retry, also when the paired route exists
python3 tests/extras_trust_test.py   # 4.4 beta 4 review F88: the add-on's provider is called only when its owner is the add-on signed as the home
python3 tests/photo_size_test.py   # 4.4 beta 4 review F89 (R-336): a 12 MP photo is sub-sampled by its long side and sent under 700 KB
python3 tests/quiet_hours_test.py   # 4.4 touch «ساعات الهدوء»
python3 tests/kahf_template_test.py   # 4.4 touch: the Friday Kahf, ready
python3 tests/fade_in_test.py   # 4.4 touch «يبدأ بهدوء»
python3 tests/friday_dhuhr_test.py   # 4.4 touch «أذان الجمعة»
python3 tests/black_screen_test.py   # 4.4 #62 «شاشة سوداء»: listening with the picture off
python3 tests/photo_delete_test.py   # 4.4 «صوري»: delete one photo from the phone or the TV; search aliases
python3 tests/loudness_test.py   # the recitation and the adhan lifted inside the player (LoudnessEnhancer), never the TV volume or mute
python3 tests/seek_burst_test.py
python3 tests/pill_text_test.py
python3 tests/control_center_test.py
python3 tests/control_center_token_test.py   # 4.4 beta 3, GitHub #67: «مركز التحكم» opens only over a live window (one retry once attached; never BadTokenException)
python3 tests/back_twice_test.py   # 4.4 beta 3: «رجوع مرتين يفتح شاشة التوقف» (on by default, never a single Back, never in kids mode); the cards keep their last frame on the way back
python3 tests/frozen_screen_test.py   # 4.4 beta 3: every cover over the home ends by a deadline; the key watchdog «frozen-screen recovered»
python3 tests/config_change_test.py   # 4.4 beta 4: a change of the screen's size or density makes the home again; a focus move is a frame
python3 tests/glass_clock_rule_test.py   # 4.4 beta 4: in «كنبة» the big glass clock stays centred with a card; the strip fits at 720p
python3 tests/tasks_card_test.py
python3 tests/reminders_test.py   # «المهام» with a time: the alarm, the cards, the keys, saver.sh, the phone's date and time (the page's card: reminder_page.test.mjs)
python3 tests/now_playing_test.py
python3 tests/digits_text_test.py
python3 tests/page_stamp_test.py
python3 tests/greeting_test.py
python3 tests/greeting_name_test.py
python3 tests/netgate_test.py
python3 tests/clock_text_test.py
python3 tests/clock_setting_test.py
python3 tests/saver_clock24_test.py
python3 tests/card_box_test.py
python3 tests/no_decoration_test.py   # the owner (19 Nov): no AI-look decoration (accent bars, chips, decorative gradients, glows)
python3 tests/apps_align_test.py   # 4.4 beta 2 «محاذاة التطبيقات»: the apps row centred by default, right or left
python3 tests/tile_art_test.py   # 4.4 beta 2 (the owner's test of 525): an app's tile (banner, else its icon on a neutral tile; tiny, wide, white, transparent) and the grid's whole-row scroll
python3 tests/couch_faces_test.py   # 4.4 beta 4: couch-tv's app faces for «أيقونات» (one table, MIT notice, the marks listed)
python3 tests/couch_copy_test.py   # 4.4 beta 4: «كنبة» as couch-tv: the bar's «الإعدادات» and «كل التطبيقات», the date over the big clock
python3 tests/icons_no_home_grid_test.py   # 4.4 beta 4: «أيقونات» has no apps grid on the home; DOWN from the row never lands in the apps
python3 tests/card_fit_test.py   # 4.4 the owner: a home card that does not suit the look says so, and is swapped within its kind when the look changes
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
python3 tests/kids_exit_beta3_test.py   # 4.4 beta 3: kids mode always shows its way out (the home row, the phone page); under every QR what to do when it does not open
python3 tests/update_test.py
python3 tests/update_cover_test.py   # step 41: the screensaver's cover over the home app's own replace (no last app in front)
python3 tests/suggested_apps_test.py   # 4.3 «تطبيقات مقترحة»: the catalog, the pinned IPTV player, the settings screen, the setup boxes
python3 tests/player_update_test.py   # 4.3 UrTV Player updates from inside it: the provider (the Player with its key only), INSTALL_PLAYER, urtv-player.version
python3 tests/phone_draw_test.py   # 4.3 #53 «ارسم»: live strokes from the phone over the TV's screen
python3 tests/lang_provider_test.py   # 4.3: «اللغة» read only for UrTV Player (content://sa.urtv.home.lang/lang)
python3 tests/match_app_test.py   # 4.3 «شاهد المباراة»: the match app and the FIND_MATCH contract
python3 tests/update_channel_test.py
python3 tests/beta_channel_test.py   # 4.3.3: «النسخ التجريبية», apk-beta, promote-beta.yml
python3 tests/beta_report_test.py   # 4.3.3: «channel: beta|latest» in the report
python3 tests/beta_notes_test.py   # 4.3.3: the one-time beta line and the crash question
python3 tests/beta_pill_test.py   # 4.3.3: the home update pill follows the channel
python3 tests/android_compile_test.py
bash tests/android_compile.sh   # both apps against android-all API 34; SKIP (not a failure) when offline or the jar is not cached
python3 tests/countdown_text_test.py
python3 tests/month_names_test.py
python3 tests/focus_key_test.py
python3 tests/settings_hub_test.py
python3 tests/on_off_test.py   # on/off rows: the owner's choice I (green triangles and word), both apps
python3 tests/parental_text_test.py   # «الرقابة الأبوية»: the card and its two buttons, the count of locked apps (docs/UX-REVIEW.md)
python3 tests/fav_bar_test.py   # the favourites bar: the tray, five tiles, the focus inside it, the one 40 px move (docs/PROPOSALS-NOV2.md §1)
python3 tests/apps_look_test.py   # 4.4 «شكل التطبيقات»: بنرات / أيقونات / مربعات ملونة, centred, A and B with no tray and «تكبير وظل», nothing bundled
python3 tests/tvos_home_test.py   # 4.4 beta 2 the full look «tvOS»: the preset, the layout only through it, the row above the bar, CardFit per slot, the weak TV
python3 tests/settings_tvos_test.py   # 4.4 beta 2 «شكل شاشة الإعدادات: tvOS»: the 4.3.2 logic in the tvOS manner, light on a weak TV
python3 tests/glass_dock_test.py   # 4.5 «زجاجي»: the glass bar's size and rows, «كل التطبيقات» as glass, «تغيير الأيقونة»; R1, R3, the tvOS Launcher credit
python3 tests/called_off_test.py   # 4.3 A1-A3: a called-off game is never live or upcoming; a team's game read from the copy further on
python3 tests/usb_test.py   # 4.3 B8: the USB viewers are never off and always in the apps row (issue #50)
python3 tests/sideload_apps_test.py   # #57: a sideloaded app shows on the home next to a Google home (only the TV's own phone-only helpers stay out)
python3 tests/home_apps_test.py
python3 tests/kids_exit_test.py   # 4.3.3: kids mode always has a way out to «الرقابة الأبوية» (the long OK box, Menu, star, a long Settings key), behind the PIN
python3 tests/date_num_test.py   # the home's date with its year; «التاريخ بالأرقام» on the home and the screensaver
python3 tests/global_search_test.py   # «بحث» on the home: the TV's own search inside the apps, off by default
python3 tests/report_words_test.py   # 4.3 B10: the report page asks what happened, required
python3 tests/phone_message_test.py   # 4.3 #53: a short message from a paired phone on the TV's screen
python3 tests/photos_phone_test.py   # 4.3 «صوري»: 50, and from a paired phone (no camera data)
python3 tests/own_adhan_fajr_test.py   # 4.3 C16: a second own adhan, for Fajr
python3 tests/football_test.py   # the football home and «المباريات»: the setting, the pages, the widgets' focus, the tray's move, the broadcast table (docs/PROPOSALS-NOV2.md §1)
python3 tests/three_teams_test.py   # 4.3.3 A3: three followed teams through every step (saver Prefs, the provider, the launcher's link, the page)
python3 tests/follow_limit_test.py   # 4.4 more than 8 leagues and 20 clubs: one warning once, at most 24 and 40, every place a person adds one
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
python3 tests/ar_style_test.py   # docs/STYLE-AR.md: the Arabic wording check warns (never fails), the approved Saudi wording is in the app
# English phase 1 (the screensaver app): its new tests go right under this line
python3 tests/i18n_saver_test.py   # the settings in English (Words, WordMatch, Places), Arabic as it was, «اللغة» in «المزيد»
#
# English phase 2 (the page): its new tests go right under this line
python3 tests/i18n_page_test.py   # dist/tv-en.html: built beside tv.html (byte for byte), current, under budget; the app and the home card pick it
python3 tests/idle_rule_test.py
python3 tests/google_tv_home_test.py
python3 tests/wifi_hotspot_test.py
python3 tests/dev_help_test.py
python3 tests/build413_test.py
python3 tests/report_lists_test.py
python3 tests/quick_measure_test.py
python3 tests/dock_exact_test.py
python3 tests/install_fallback_test.py
python3 tests/run_resume_test.py
python3 tests/prefetch_test.py
# 4.4: tests that existed but CI never ran (all pass on next; about 50 s together)
python3 tests/adhan_late_test.py   # Hotfix 4.3 «الأذان الساعة 12 الليل»: an adhan alarm that arrives late, early, with no prayer time or not near
python3 tests/build413_test.py   # 4.4, the tester's build 413 (Home 100 % in normal use; T9 with the owner's real remote: the add-on's Home key
python3 tests/clean_mode_test.py   # «UrTV النظيف» part A (the clean home): it is a MASK over the home (nothing deleted, none of the person's choic
python3 tests/clean_prayer_card_test.py   # «UrTV النظيف»'s prayer card («بطاقة الصلاة»: بدون / سطر تحت التاريخ / بطاقة; «مكان البطاقة»: وسط / تحت الساعة
python3 tests/clear_text_test.py   # «نص أوضح» (4.3.1): one setting that makes the grey words and numbers of the home and the screensaver as clear
python3 tests/control_shape_test.py   # 4.3.1 «شكل لوحة التحكم»: the control center is the tall panel (default, as 4.3) or a wide page; both fit 1080p
python3 tests/dev_help_test.py   # User report «ماهو راضي يفعل تصحيح USB»: Developer options open from the build number, whose place differs per
python3 tests/dock_retry_test.py   # Hotfix 4.3: after a cold start the favourites row showed only five apps (two users, 4.2 and 4.3). showApps() f
python3 tests/factory_test.py   # «تخلّص من واجهة المصنع» (its own row in the settings): the engine on SetupEngineRun's fake TV (the offer and i
python3 tests/rescue_home_test.py   # 4.4 (R7): the rescue's fallback home: armed before the stock home goes, off when it is back; the factory step only where needed; a PC-switched TV gets its stock home back
python3 tests/fragment_keep_test.py   # 4.3.2 (a user's adhan times and prayer card disappeared after 4.3.1): the home keeps the screensaver's last RE
python3 tests/glow_mode_test.py   # «توهّج لون التطبيق» (4.3.1): the glow behind the focused app is OFF by default (new installs and old ones), ha
python3 tests/google_tv_home_test.py   # Google TV (user reports: TCL G10/G08, Changhong Android 14, Sony BRAVIA VH21, Xiaomi MiTV-AFMU0/1): since 4.3
python3 tests/home_snapshot_test.py   # The home's stand-in picture (HomeSnapshot): the rules pinned from the source: what the key is made of, the age
python3 tests/idle_rule_test.py   # «تبدأ شاشة التوقف بعد 2147483 ثانية» (user report): the TV's idle delay was 2147483647 (Integer.MAX: the TV tu
python3 tests/install_saver_test.py   # 4.3.2 «ثبّت الجزء الثاني»: a TV with only the home (a Fire TV, or installed with Downloader) must never meet a
python3 tests/league_groups_test.py   # 4.3.1 «البطولات الكبرى» and «اختر دورياتك»: the big tournaments' list is one data file that resolves against t
python3 tests/add_lag_test.py   # 4.4 beta 2: adding leagues and clubs never freezes (the search waits, folds once, ticks in place, off the UI thread)
python3 tests/match_teams_button_test.py   # 4.3.1: «فرقي» in the matches page's header opens the settings' teams chooser (the page is made again on return
python3 tests/net_test.py   # Net: the small GET the apps use for their own data files (IPv4 first, 4 s per address, a deadline, a size cap,
python3 tests/phone_room_test.py   # «اللعب بالجوالات»: the generic phone room of the party games and the game-only server on a plain JVM (tests/Ph
python3 tests/radio_fault_test.py   # 4.3.1 «الإذاعة»: why a station stopped, in one plain line (no internet / the station did not answer / format n
python3 tests/settings_sections_test.py   # 4.3.2 «الإعدادات تكون مضبوطة»: every option lives in the one section it belongs to, in a set order, found by a
python3 tests/settings_findability_test.py   # 4.4 beta 4 (the owner, the findability review): one tree in every settings style, every audited row reachable within section ← row ← picker, the hidden features found by their words
python3 tests/setup_row_test.py   # 4.3.1 «إعداد تلفزيوني» is a row of the settings (Home screen) that always opens the setup, even when UrTV is a
python3 tests/step46_test.py   # The tester's step 46 findings (build 214, a real TCL), the ones that are pinned in the source: «تطبيقات مقترحة
python3 tests/step47_test.py   # The tester's step 47 findings (build 222, a real TCL) that are pinned in the source: the update screen opened
python3 tests/tournaments_fixtures_test.py   # 4.3.1 the CI tool of «البطولات الكبرى» (.github/tools/tournaments_fixtures.py) with TheSportsDB mocked: three
python3 tests/vector_drawables_test.py   # Every vector drawable's pathData must be one the Android Gradle plugin's own vector parser reads (the release
python3 tests/wall_first_frame_test.py   # Hotfix 4.3: the home never shows black on the way back (a user's Mi Box on 4.3: the cards on black for a few s
python3 tests/wifi_hotspot_test.py   # A user's tested route (Xiaomi): when the setup wizard owns the saved Wi-Fi and the TV shows no «انسَ الشبكة» o
python3 tests/your_tv_hub_test.py   # «تلفزيونك» (YourTvHubActivity): what opening the app shows: four windows (the markets, the matches, the games,
python3 tests/rescue_code_test.py   # the rescue app's Downloader code: the same on the setup page and in docs/RESCUE.md
python3 tests/run_list_test.py   # every test file is run by this script (CI runs nothing else)

node tests/crest_keep.test.mjs   # 4.4 beta 4: the crest cache never throws out a club that is on the page
node tests/es2017_parse.test.mjs   # #19: the built pages parse as ES2017 (Android 9 TVs stuck on WebView 66)
node tests/date_num.test.mjs   # «التاريخ بالأرقام» on the screensaver page: day/month/year in the digits chosen
node tests/wx_icon.test.mjs   # the screensaver's weather line: the condition's icon beside the temperature
node tests/js_strip_test.mjs   # the pages ship without the script's comments: the same program (acorn trees), 50 KB smaller
python3 tests/ui_thread_channel_test.py   # 4.3.3 hotfix: no query to the screensaver on the home's UI thread (the TCL remote froze)
python3 tests/ui_thread_44_test.py   # 4.4 (QA-6..QA-10, R1): the add-on, the home key, the adhan preview, the follows and the notices file off the UI thread
python3 tests/home_review2_test.py   # 4.4 review round 2: the kept settings copy, the tasks focus, the language guard, the page copy lock, the markets queue, the overlay notice token
python3 tests/black_video_test.py   # 4.4: no page of ours keeps working behind another app (the screensaver activity, every WebView owner, the page's hidden gate)
python3 tests/games_open_test.py   # 4.3.4: «الألعاب» opens a fresh «تلفزيونك» on its games (an open settings task came back instead)
python3 tests/hidden_apps_way_test.py   # 4.3.4: a way back to hidden apps (settings «التطبيقات المخفية»), the toast names it, the mode survives onResume
python3 tests/options_fit_test.py   # 4.3.4: the app options box is as wide as its words (they were cut off on a small tile)
python3 tests/update_rows_focus_test.py   # 4.3.4: a step down in «تحديث تلفزيوني» no longer skips «تحديث صفحة شاشة التوقف» (fullScroll moved the focus)
python3 tests/ok_hold_test.py   # 4.3.4: a settings rebuild while OK is held waits for the release (the first press was lost)
python3 tests/edit_keys_test.py   # the edit modes keep only the keys they use: a remote's app, colour, channel and maker keys go on
python3 tests/qa_434_test.py   # 4.3.4: the release gate's findings: the first clock draw from memory (QA-1), the reminder card read off the UI thread (QA-3), the favourites move keeps only its keys (QA-5), the review's F1/F2
python3 tests/beta_newest_test.py   # 4.3.4: «النسخ التجريبية» takes the newer of apk-beta and apk-latest (a beta user got no update again)
python3 tests/beta_confirm_test.py   # 4.3.4: a beta build asks «نسخة تجريبية» on the TV before anything is downloaded
python3 tests/webview_guard_test.py   # 4.3.5: no WebView on the TV: the home draws without its card, never a crash at start (a CVTE Android 9 black screen)
python3 tests/crash_loop_test.py   # 4.3.5 (R7): two crashes right after start open SafeActivity (the TV's own home one press away), never a black screen
python3 tests/safe_stock_test.py   # 4.4 (step 100): «افتح الشاشة الأصلية» turns a stock home an earlier setup turned off back on (our key), else says the way by hand; never silent
python3 tests/api28_compile_test.py   # 4.3.5: the home compiles against Android 9 with every newer API behind a Build.VERSION check; the reports name the WebView
python3 tests/api26_compile_test.py   # 4.3.6: the home against Android 8.0 (its minSdk): every newer API behind a version check for 27 or newer (a Percee OTT box)
python3 tests/black_video_test.py   # 4.3.5: no page of ours keeps working behind another app (the screensaver activity, every WebView owner, the page's hidden gate)
node tests/es2017_parse.test.mjs   # #19: the built pages parse as ES2017 (Android 9 TVs stuck on WebView 66)
python3 tests/api26_compile_test.py   # 4.3.6: the home against Android 8.0 (its minSdk): every newer API behind a version check for 27 or newer (a Percee OTT box)
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
python3 tests/howto_test.py   # «طريقة اللعب»: the small «!» on a start screen and the panel behind it, the texts of the 19 games (rules on a plain JVM, the three places a text lives, the Android side by its source)
python3 tests/games_kit_test.py   # the games' kit, the registry and the list, «خمس خانات» and «الكلمة المفقودة» (their rules on a plain JVM)
python3 tests/games_fixes_test.py   # the tester's ten game bugs of build 629: the tile and setup words, «ابدأ», the Domino backs, the hex letters, «بدون كلام»'s cover, «ه» and the letter names, «خمّن الكلمة», the how-to, the room page, the setup rows
python3 tests/domino_test.py   # «دومينو»: the rules and the computer players on a plain JVM, whole games from seeds (docs/DOMINO.md)
#
python3 tests/tvports_test.py   # BETA LG/Samsung packages: manifests, files, sizes (docs/LG-SAMSUNG.md)
python3 tests/games_board_test.py   # «خلية الحروف» and «خمّن الكلمة»: both engines on a plain JVM against the bundled data
node tests/prayer_crosscheck_page.mjs
node tests/prayer_crosscheck_world_page.mjs
node tests/hijri_crosscheck_page.mjs
node tests/page.test.mjs
node tests/pack_fallback.test.mjs   # 4.4 beta 3: a pack or «صوري» photo that does not load never leaves the screensaver black (a bundled photo shows)
node tests/tafsir.test.mjs   # «التفسير الميسر» (#tafsir=1): every verse of the deck has its tafsir, whole, in pages of three lines at most, never «…»
node tests/saver_designs.test.mjs   # 4.4 the screensaver's designs (the owner approved them): each shows what it should; a prayer's name and time never touch
node tests/lone_card_fit.test.mjs   # 4.4 the owner: a lone card on the launcher is fitted to its box, never a row cut at the bottom
node tests/saver_depth.test.mjs   # 4.4 «نظيفة» with depth on the screensaver: the photo's own cut-out, never off its pixels, never over unreadable digits
node tests/verse_hadith.test.mjs   # 4.4 «آية» and «حديث» at once: the hadith card beside the verse card, each its own
node tests/strip_steady.test.mjs   # 4.4 the screensaver's bottom strip stays put as the slot turns (games, scores, weather, markets)
node tests/sources_rule.test.mjs   # 4.4 the owner's rule: a source on a card is a tiny dim line at most (60%, half the light), never a provider named elsewhere
node tests/cards_batch3.test.mjs   # 4.4 card review batch 3 (12 adhkar, 13 tasks, 14 now playing, 16 year, 17 today, 18 shows): new look by default, the old one with «الشكل السابق»
node tests/tafsir_short.test.mjs   # 4.4 the tafsir card short: the Muyassar whole, «التفسير الميسر، صفحة N» under it, the pages stored per verse
node tests/football_page.test.mjs   # the football home's widgets and «المباريات» in a browser, on fixed games
node tests/sports_cards.test.mjs   # 4.4 «الرياضة»: the sports cards (dist/sports.js) on the home, the couch row and the screensaver, the takeover, only the page's own build
node tests/many_follows.test.mjs   # 4.4 40 clubs and 24 leagues in the page: the extra ones asked every 2nd load, files 4 at a time, the bar built once a minute, the warning's two OKs, «فرقي» at 720p
node tests/sports_hist.test.mjs   # 4.4 the league page's table from the archive first (TheSportsDB's after it, a plain line when neither), the club page's all-time meetings
node tests/honours.test.mjs   # 4.5 the league page's seasons, «الأبطال», «الهدّافون» with their source, the club page's titles (docs/HONOURS-STUDY.md)
node tests/sport_tops.test.mjs   # «رياضي»: SPORTS.tops(n), the top of every followed league from the files the TV has, never invented
node tests/matches_archive.test.mjs   # «المباريات» with the match archive (data-archive): every followed team, yesterday's results, a club, a league, the chooser
node tests/add_lag.test.mjs   # 4.4 beta 2: the page's chooser of 800 leagues draws round the focus; the club search draws once the typing rests
node tests/phone_page.test.mjs
node tests/sync_cards.test.mjs   # «توحيد المظهر» on the page: the cards in the material's fill, line and ink, readable on a light look
node tests/couch_row.test.mjs   # 4.4 the page: «كنبة» (#row=1, the cards in one strip above the tray) and «مربعات» (today's prayers as boxes in a row, the next one lit with its countdown)
node tests/tvos_row.test.mjs   # 4.4 beta 2 «tvOS»: the page's glass row, two cards wide, four days, rowOverflow
node tests/card_title.test.mjs   # 4.4 the owner: a home card's title clear of its rounded corner and the rule, at the start, in every style and layout
node tests/photo_mask_page.test.mjs   # 4.4 the phone page «صوري»: a photo with its mask, checked on the phone, previewed, sent after the photo; the page's own CSP
node tests/reminder_page.test.mjs   # the screensaver's reminder card: its time, «الآن», «تم», the snooze, the keys, the adhan, English
node tests/tvports_page.test.mjs   # the packaged page with simulated webOS and Tizen keys
node tests/game_phone_page.test.mjs   # The party games' phone page (assets/quiz/game.html) in a real browser against the REAL room and server (tests/
python3 tests/game_bundle_test.py   # the games' bundled data and its checks
python3 tests/game_packs_test.py   # the games' packs
python3 tests/new_games_test.py   # the new games' rules and screens
python3 tests/game_engine_test.py   # the games engine and its remote release switch (docs/GAME-ENGINE.md, docs/GAMES-RELEASE.md)
python3 tests/saver_review44_test.py   # 4.4 review fixes in the screensaver app (F12-F15)
python3 tests/saver_gate_test.py   # 4.4 review: a settings change during the page start is kept and looked at again (F1)
python3 tests/saver_prayerfix_test.py   # 4.4 review: the late prayer correction reaches a running dream (F2)
python3 tests/saver_kids_keep_test.py   # 4.4 review: the kids copy keeps its last real answer (F3)
#
# «regressions» (docs/dev/REGRESSIONS.md): the gate's own tests, and any guard of the register this file does not run by name above
python3 tests/regressions/run.py --rest
