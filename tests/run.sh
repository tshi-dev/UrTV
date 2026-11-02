#!/usr/bin/env bash
# Every check in one go: static checks, then the screen in a real browser. See tests/check.py and page.test.mjs.
#   pip install fonttools brotli pillow -r .github/tools/cities-world/requirements.txt && npm install --no-save playwright@1.56.1 && npx playwright install chromium
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
python3 tests/deadman_test.py
python3 tests/run_lock_test.py
python3 tests/run_identity_test.py
python3 tests/run_report_test.py
python3 tests/setup_phone_test.py
python3 tests/phone_userland_test.py
python3 tests/adb_client_test.py
python3 tests/setup_engine_test.py
python3 tests/saver_hold_test.py
python3 tests/setup_runner_test.py
python3 tests/setup_prep_test.py
python3 tests/setup_community_test.py
python3 tests/setup_install_test.py
python3 tests/setup_report_test.py
python3 tests/type_test.py
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
python3 tests/cities_md_test.py
python3 tests/home_card_test.py
python3 tests/home_page_test.py   # the home's cards on the screensaver's newer page (its stocks stayed stale on the APK's own)
python3 tests/perf_idle_test.py
python3 tests/league_fixtures_test.py
python3 tests/market_prices_test.py
python3 tests/stocks_search_test.py   # «قائمة الأسهم»: a Tadawul code, ٢٢٢٢ or a company name, and the match shown
python3 tests/adhkar_tool_test.py
python3 tests/saver_tasks_test.py
python3 tests/saver_quote_test.py
python3 tests/quote_every_test.py
python3 tests/user_options_test.py   # «الآية والحديث», «إظهار الثواني», «الخروج من تلفزيونك»: off by default
python3 tests/photo_interval_test.py
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
python3 tests/loudness_test.py   # the recitation and the adhan lifted inside the player (LoudnessEnhancer), never the TV volume or mute
python3 tests/seek_burst_test.py
python3 tests/pill_text_test.py
python3 tests/control_center_test.py
python3 tests/tasks_card_test.py
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
python3 tests/settings_key_test.py
python3 tests/remote_nav_test.py
python3 tests/dpad_rtl_test.py
python3 tests/home_grid_test.py
python3 tests/spring_test.py
python3 tests/look_test.py
python3 tests/tv_rows_test.py
python3 tests/adhan_reply_test.py
python3 tests/clock_zone_test.py
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
python3 tests/football_test.py   # the football home and «المباريات»: the setting, the pages, the widgets' focus, the tray's move, the broadcast table (docs/PROPOSALS-NOV2.md §1)
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
node tests/i18n_page_test.mjs   # dist/tv-en.html in a browser: left to right, no Arabic in its own words on any card
#
# English phase 3 (the home app): its new tests go right under this line
python3 tests/i18n_home_test.py   # the home's words: none left behind, Arabic byte for byte (tests/i18n_home_cases.tsv), English with no Arabic, Dir, the Quran's names
#
# English phase 4 (the phone page): its new tests go right under this line
python3 tests/i18n_phone_test.py   # phone/index.en.html: generated by src/phone_en.py and current, under 64 KB, no Arabic of its own; the link; PhoneHost serves «اللغة»
node tests/i18n_phone_test.mjs   # phone/index.en.html in a browser: left to right, English on every tab, the link both ways; docs/shots/phone-en.png
#
python3 tests/tvports_test.py   # BETA LG/Samsung packages: manifests, files, sizes (docs/LG-SAMSUNG.md)
node tests/prayer_crosscheck_page.mjs
node tests/prayer_crosscheck_world_page.mjs
node tests/hijri_crosscheck_page.mjs
node tests/page.test.mjs
node tests/football_page.test.mjs   # the football home's widgets and «المباريات» in a browser, on fixed games
node tests/phone_page.test.mjs
node tests/tvports_page.test.mjs   # the packaged page with simulated webOS and Tizen keys
