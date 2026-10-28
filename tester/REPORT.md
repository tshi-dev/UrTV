step 7, part A (no owner)

Version: before 3.145, after "version: 3.147" (installed with `APK_TAG=apk-next optimize/install.sh all`, screensaver first; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | — | idle (no screensaver yet): sa.urtv.screensaver 0% CPU, WebViews 0, no ServiceRecord. MemAvailable 781 MB, SwapFree 182 MB. After 2 min on Home the real screensaver started by itself; measured then: 0% CPU, WebViews 1, services ScreenDream + its sandboxed WebView (expected while it shows) |
| A2 | OK | — | 5x YouTube→Home: screensaver pid the same before/after (no restart); its CPU 0%; total 14%; MemAvailable 742 MB, SwapFree 257 MB. cpuinfo lines looked unrefreshed (same fault counts as A1) |
| A3 | OK / problem | key83, key176 | keyevent 83 → sa.urtv.screensaver/.SettingsActivity (ours). keyevent 176 → nothing, home stayed in front |
| A3b | problem | pair-pc4, pair-pc6, pair-allow, pair-allow2 | the page opened in the PC's Chrome (address answers 200); the owner typed the code (twice). Both times the screensaver started ~45 s after the last remote key, closed the pairing screen and switched phone control OFF. Try 1: the page said «ما نجح الأمر». Try 2: the page waited «بانتظار موافقة التلفزيون: اضغط «سماح» بالريموت», but the TV was in the screensaver; after waking it the screen showed «تشغيل» (off). Owner: «when waiting on some main pages the screensaver should not go»: the pairing screen and a pending «سماح» must keep the TV awake. Try 3: the «سماح / رفض» prompt did show («جوال جديد يطلب التحكم», the PC's address), focus on «رفض»; the next key a moment later found phone control OFF again («تشغيل»): the screensaver had ended it before «سماح» could be pressed. Good: the pairing screen now fits one page, address and code large |
| A3b (try 4) | OK | pair-allow5, pair-allow6 | with the TV kept awake (a harmless key every 20 s from the PC), the owner typed the code in the PC's Chrome (window 400x860, phone size), the TV asked «جوال جديد يطلب التحكم» (focus on «رفض» first), «سماح» pressed: the page connected («متصل · بقي 30 دقيقة») |
| A3c | OK / problem | phone-home, phone-left | «الريموت»: «الرئيسية» and the arrows move the TV; volume −/+ pressed. «التطبيقات»: real icons + «أدوات تلفزيونك»; Spotify opened on the TV. «يعمل الآن»: the tilawah player and the next adhan. «الإعدادات»: first home card set to «الصلاة القادمة» → TV home_panel nextprayer (works); setting it back to «بدون» from the page and saving did NOT change the TV (still nextprayer; put back with saver.sh). Light mode switches the page to light and back |
| A3d | problem | type-yt-field, type-yt2 | type-setup on; YouTube search open with its keyboard; typed «قرآن» + «بحث»: nothing appeared. UrTVType: «text request: mode=replace search=true len=4», «foreground=other», «service=on», «root=present», «field=none», «result=no_field» |
| A3e | OK | — | «افحص التحديث» → «تلفزيونك محدّث» (nothing installed) |
| A4 | OK | phone-row, phone-ctl | «الجوال والتحديث» → «تحكم من الجوال» is the first row; it shows «تشغيل», so phone control is already OFF after the update. Not switched on |
| A6 | OK | — | familydns on: «is on (family.adguard-dns.com); the internet works through it»; private_dns_mode hostname; off: «put back by the home app», status off, private_dns_mode null (start value). Log: started → RUNNING; working → on (us); ENABLED → RESTORED |
| A7 | OK | adhkar4 | «أذكار المساء» with the full dhikr text, «مرة واحدة», page dots. Card set back to verse. (Somnambulator did not start the dream; used --preview) |

New problems seen in part A:
- HOME LAYOUT BROKEN on 3.147 (shots key176, home-now, home-after-home): the favourites row sits at the top over the greeting/clock/date (they show through it), and ONE huge «أكمل المشاهدة» card fills the lower 2/3 of the screen. Stays after pressing Home again.
- GLASS SETTINGS, focused tile unreadable (a4-hub, a4-pair, phone-row): the focused tile/row is solid white and its title, icon and summary are white too, so nothing on it can be read. Seen on every tile and on the rows inside sections.

Numbers vs step 6: screensaver CPU 45% → 0%; MemAvailable 570 → 742-781 MB; SwapFree 84 → 182-257 MB.
!! lines: none.

Restore after part A: home card none, accessibility services back to the start value (typing service off), the PC's pairing removed («إلغاء») and phone control OFF («توقف التحكم من الجوال لأن آخر جوال انتهت جلسته»), card verse, familydns off, updatechannel unchanged (latest). Part B (owner) still to do.

step 7, part B (owner asked the agent to do it and send him photos; done over adb)

| # | OK/problem | shots | note |
|---|---|---|---|
| B1 | OK / problem | glass-hub, glass-sub, glass-backup, glass-dialog | keyevent 83 opens our settings. Main menu: the focused tile is now readable (dark text on white). Inside «الموقع والصلاة» the focused ROW is still white text on white (unreadable). After 5 down and 5 up the title is visible, but the whole page jumped sideways (list moved from the left half to the right half and back). «طريقة الحساب» dialog is see-through: the rows behind show through its options |
| B2 | OK | glass-light | light: white frosted tiles, dark text, focused tile dark: readable. Back to dark |
| B3 | not done | — | «تعديل التطبيقات» not run (would change the owner's apps without him) |
| B4 | problem | watchnext | still broken: favourites at the top over the clock/greeting, one huge «أكمل المشاهدة» card fills 2/3 of the screen |
| B5 | OK / check | center-glass, center-settings | nothing cut now; the focused tile is readable; «إعدادات تلفزيونك» opens sa.urtv.screensaver/.SettingsActivity. Its icon is a plain white square (missing icon) |
| B6 | OK | tilawah-glass, tilawah-surahs | glass look, readable, focused row readable |
| B7 | not done | — | owner not at the remote |
| B8 | OK | inputs-cec2 | our «المصادر» pill: AV, HDMI 1-4 each «وضع الاستعداد», «البث التلفزيوني». No CEC device on, so the device-name case was not seen |
| B9 | OK | pair-pc4 | one page, no scrolling, address and code large |
| B10 | OK | — | after 5x YouTube→Home: screensaver pid unchanged, CPU 0%, home 1.4%, total 19%; MemAvailable 783 MB, SwapFree 363 MB (step 6: 45% CPU, 570 MB, swap almost full) |

Restored: appearance dark, card verse, home card none, accessibility services start value, phone control off and the PC pairing removed, familydns off, updatechannel latest.
