# Release smoke test (15 minutes, on a real TV)

Run before every push to master (docs/dev/RELEASE.md, step 4), on the build that will be published (`apk-next` of the release commit),
on a **TCL** (the TV that froze in 4.3.3) and, when there is time, a second make. Use the remote, not ADB, except where a line says so.

Write one line per check in the run's report: `S-nn PASS` or `S-nn FAIL <what you saw> (R-xxx)`. A FAIL names the register row
(docs/dev/REGRESSIONS.md) it brought back; any FAIL stops the release. Every row a person can see on the TV (On TV = Y) is under one check
here (`tests/regressions/regressions_index_test.py` keeps it so): when a check lists many rows, look at each thing it names, quickly.

Before you start: the TV on the release build, a city chosen, two favourites cards on, the adhan on, «النسخ التجريبية» off, a phone on
the same network. Note the build in the report's first line.

## Keys and speed (4 min)

- **S-01 Remote latency, minute tick and after Home.** Wait for the clock's minute to turn while pressing ◀▶ on the favourites: no stall
  over about half a second. Open an app, press Home, press ▶ at once and keep moving: the focus follows every press. Press Back on the
  home 5 times quickly: each answers at once. Repeat once after `optimize/tv.py` force-stops the screensaver app (the TCL case).
  Also: switch «اللغة» and back, then move at once (the home made again answers at once); open a reminder card and press OK.
  On a TV with the add-on: the same at the minute tick; open «مركز التحكم», press the adhan preview; open «المباريات».
  Rows: R-269, R-270, R-271, R-272, R-001, R-002, R-042, R-254, R-255.
- **S-02 Return to the home.** From YouTube, Home: the photo is there at once (no black, no blur), all favourites (not five), the tray's
  tiles inside it, the name under the focused tile, no other app comes to the front afterwards; «كل التطبيقات» lists the store and the
  maker's apps, no «تلفزيونك UrTV» tile, app names in one language.
  Rows: R-028, R-030, R-039, R-152, R-089, R-157, R-158, R-159, R-127, R-117, R-164.
- **S-03 Long-press menu, hidden apps, edit modes.** Long-press an app with a short name: every option's words whole. Hide an app,
  then settings ← «ترتيب كل التطبيقات» ← «التطبيقات المخفية» brings it back. In «تعديل المفضلة» press a colour key and a channel key:
  they act (the mode does not swallow them); Back ends the mode.
  In «ترتيب المفضلة» move an app and press a colour key (it acts); Menu inside an edit mode opens nothing over it; cancel the PIN on
  «التطبيقات المخفية», then long-press «انقل» on an app: it moves that app.
  Rows: R-005, R-008, R-010, R-256, R-257, R-258.
- **S-04 The remote's own buttons.** On the home and in an app: Netflix, YouTube, Prime (when the remote has them), the mic, a colour
  key, Settings (the TV's settings or ours, as the row says), the TV/Live key (the tuner, not the last HDMI), the input key.
  On a Google TV: Home from an app, quickly, five times: UrTV each time; after a restart the add-on works (Home, sleep). Apps
  installed outside the store are on the home.
  Rows: R-263, R-264, R-266, R-268, R-009, R-054, R-058, R-069, R-121, R-122, R-139, R-046, R-055.

## Settings and update (4 min)

- **S-05 «مركز التحكم».** Open it; all tiles fit (also at 720p when a TV has it); the sound tile names the real output; «الألعاب»
  opens the games (not settings, not an old preview) and Back returns to the home; «أطفئ الآن» says what it needs. After playing a
  game, Home, then «تلفزيونك» in it: the hub, not the game left on top.
  Rows: R-007, R-095, R-096, R-093, R-246, R-290.
- **S-06 Settings, single press.** Open the screensaver settings and press OK on 5 rows in a row, fast: each opens on the first press.
  The focused row is readable, the list does not slide sideways, dialogs are not see-through, no second line ends with «..», hints and
  footers inside the screen, the month names whole, the version with its build, focus stays on «UrTV النظيف» after it, «صوري» once,
  «إعداد تلفزيوني» findable, «قبل ما تحذف التطبيق…» there, «بدء شاشة التوقف بعد» says a real time or «أبداً», «تطبيقات مقترحة» and
  the hub inside the screen.
  Rows: R-006, R-090, R-091, R-092, R-137, R-138, R-146, R-147, R-149, R-150, R-235, R-223, R-224, R-248, R-019, R-014, R-215, R-221.
- **S-07 «تحديث تلفزيوني», focus order and beta.** Down from the top visits every row in order, «تحديث صفحة شاشة التوقف» included
  (none skipped); the version line reads well; a stray OK under the auto-update row does not flip it. Turn «النسخ التجريبية» on:
  «تحقق من التحديث» says «لا يوجد أحدث» or offers the beta (never «ما يشتغل على هذا الجهاز»); turn it off: back on the normal channel.
  Rows: R-004, R-003, R-015, R-251, R-066, R-125, R-234, R-016.

## The home's cards and the screensaver (3 min)

- **S-08 Card toggles.** From the phone page turn one home card off and one on, «حفظ البطاقات» once: the home follows within seconds,
  an off card is not drawn; the prayer card and adhan times there; the greeting fits the hour; the city's clock right; the stocks card
  without a dark box or bright rim; clean mode on then off: the quick-buttons box as wide as before, the prayer card centred; the
  «خلّ تلفزيونك يفتح على UrTV» line never over a card; the quick buttons all in the look's fill; adhkar digits as set.
  On the football home, «بدون» turns its weather, prayers and games widgets off too. Start the screensaver after a few minutes idle: no
  old picture with an old time first (the dark ground until the page is up, then the real time).
  Rows: R-289, R-252, R-022, R-029, R-050, R-085, R-123, R-199, R-233, R-242, R-243, R-225, R-188, R-196, R-035, R-082, R-087, R-205.
- **S-09 Screensaver.** Start it (`optimize/saver.sh` preview or wait): no black/blank second, the photo visible (never solid black),
  photos of several packs mixed, the bottom strip steady across two slot turns, weather and prices current, Arabic joined, «حقوق الصور»
  names readable and «إغلاق» visible; the English page with no dark patch.
  Rows: R-011, R-013, R-204, R-206, R-207, R-209, R-210, R-211, R-097, R-098, R-099, R-119, R-049, R-112.
- **S-10 Looks.** «المظهر»: mocks drawn, pack tiles named, «رجوع» inside the screen. Rows: R-212, R-213.

## Pages (3 min)

- **S-11 Markets.** «الأسواق»: names (not codes) for 4164, 4002, 4349; Arabic digits accepted in «قائمة الأسهم»; tabs stay put on
  «أخفِ المبالغ»; labels whole; signs on the right side; gold and oil names on one line; the number pad's last row visible; a stock card
  fits with «رجوع» reachable; header follows the live market; currency picker keeps its place and marks the picks; rates there; saving
  a holding with 0 says why; language change followed; no screensaver over live mode.
  Rows: R-012, R-018, R-051, R-154, R-156, R-160, R-161, R-163, R-166, R-167, R-171, R-174, R-178, R-180, R-181, R-182, R-186, R-187,
  R-194, R-195, R-197, R-198, R-208.
- **S-12 Matches and leagues.** «المباريات»: source line clear of the title; each game once; postponed games not live; names whole;
  «المباريات القادمة» over tomorrow's games; the card opens with focus; one OK on the team button changes nothing by itself; no player
  installed: the sheet still offers something; «الدوريات كاملة» opens in seconds, Arabic country names, «حفظ» reachable.
  Rows: R-021, R-031, R-033, R-155, R-165, R-170, R-189, R-193, R-218, R-236, R-247, R-249, R-216, R-217, R-219, R-088.
- **S-13 Tilawah, radio and adhan.** Play a surah and a station (the Saudi and Cairo ones): they start; level close to the TV's; no
  notifications question each time; reciter index readable; rows on the 8 px scale. ADB: `optimize/music.sh adhan test` on a muted TV:
  heard, then the TV muted again at its old volume; the sleep pill right after a restart.
  Rows: R-036, R-047, R-113, R-115, R-068, R-140, R-191, R-203, R-190, R-045, R-151, R-129, R-175, R-176, R-104.

## Phone, tasks, setup, games (1 min each, quick)

- **S-14 Phone.** Pair the phone page (the screensaver coming during pairing keeps it on); one swipe moves one tile; type into YouTube;
  an empty send says so; the screensaver times in the right units; the title inside the safe area.
  Rows: R-070, R-077, R-083, R-084, R-128, R-130, R-131, R-132, R-214, R-067, R-020, R-148.
- **S-15 Tasks.** «المهام»: focus on the text field; add one by typing; «🎤 تكلّم» says what it can.
  Rows: R-059, R-060, R-135, R-177.
- **S-16 «إعداد تلفزيوني».** Open it (do not run «جهّز كل شي» on the owner's TV): the language follows the home, «ابدأ» takes the first
  OK, the language switch labels whole, the screen stays for the run, the factory page keeps the screen on; on a test TV a full run
  passes the USB question and ends with UrTV as the home, casting, the remote keys, USB viewer and inputs still working.
  Rows: R-259, R-260, R-262, R-265, R-267, R-017, R-040, R-052, R-061, R-073, R-133, R-134, R-145, R-162, R-173, R-244, R-169, R-043, R-172, R-041, R-038, R-283, R-285, R-287 (on a Google TV: update the home over the last build with `pm install -r`, then the FIRST Home press opens UrTV), R-304 (on a TV with only wireless debugging, «جهّز كل شي» pairs once, and when pairing is refused it says why).
- **S-17 English.** «اللغة» English for a minute: app captions, the grid heading, Tilawah footers, the favourites left to right, the
  long-press hint. Back to Arabic. Rows: R-142, R-143, R-144, R-185.
- **S-18 Games.** «الأسرع» with one phone: room code shown, digits as set, the podium right; «خمّن الكلمة» takes a common word and has
  ة ى ء; a round of «واحد مو معنا» ends on «إنهاء الجولة».
  Rows: R-226, R-227, R-228, R-229, R-230, R-231, R-232, R-237, R-238, R-239, R-241.
- **S-19 UrTV Player tile.** «تطبيقات مقترحة»: its real icon. Rows: R-245.

## When a Sony TV is at hand

- **S-20 Sony remote after the setup.** After «جهّز كل شي» with the clean-up: APPS, GUIDE, Netflix, YouTube, Input, the mic and the colour
  keys each do what they did before the setup. A dead one is a FAIL naming the button (docs/dev/REGRESSIONS.md «Watch»: the candidates).
  Rows: R-009.

## When a test TV is at hand (never the owner's TV)

- **S-21 Removing UrTV after «عطّل واجهة المصنع».** One Android 9 TV and one Android 11 or newer: without «إنقاذ تلفزيونك» the step refuses and
  changes nothing; with it, after the step Home still opens UrTV; uninstall UrTV from the TV's Settings: the TV opens «تلفزيونك بدون شاشة
  رئيسية» by itself (never a black screen, never a chooser); «افتح الإعدادات» and an app of the list open; turn the stock home on by hand:
  the next Home opens it, and the rescue screen does not come back. The full steps are in tester/STEP.md. Rows: R-282.
- **S-22 Beta 3 field reports.** On a TCL Smart TV (Android 14): the remote's YouTube button opens YouTube with UrTV as the home. After a
  restart no «السماح بتصحيح USB؟» shows over the home by itself. A Bahrain place of one's own keeps Bahrain's prayer method after a restart. On a
  Xiaomi box the app buttons and the volume work after the setup and the Wi-Fi keeps its saved networks; «المصادر» lists HDMI 1, HDMI 2 and
  the aerial where the TV has them; an app installed from outside the store that declares HOME (cinemana) is in «كل التطبيقات»; after a
  week the home is not heavier (the card's page is let go). «رجوع مرتين يفتح شاشة التوقف» is off on a fresh install and sits in the screensaver's settings; turned on, two Backs within a second on the home start the screensaver.
  Rows: R-306, R-307, R-308, R-309, R-310, R-311, R-312, R-313, R-315.

## The report

```
build <N> on <TV model>, <date>
S-01 PASS
S-02 FAIL the favourites showed five apps after Home (R-030)
...
```
- **S-23 Match page channels.** On a match of a league with a verified broadcaster, «شاهد المباراة» opens UrTV Player with the broadcaster's name (Arabic and English) in the channel search, not the club's. Rows: R-314
- **S-24 The bar 15 minutes before the adhan.** In the screensaver's settings press OK on «تنبيه قبل الأذان» until it says 15 minutes: 15 minutes before a prayer the bar shows on the home and on the screensaver and stays until the adhan; on 10 (the default) it starts at 10. Rows: R-316
- **S-26 A stalled radio station takes its alt.** Play the Saudi Quran radio; when it runs dry for about 12 seconds the line «يعيد الاتصال…» shows and it comes back (logcat: `stall: source 2 of`), not silent. Rows: R-318
- **S-25 An input in the favourites.** In «أسماء المصادر» choose an input, press «أضفه للمفضلة»: its tile (the input's name) is in the favourites row on the home; OK switches to that input (a kids lock on «المصادر» stops it); a long OK opens no app menu; «شيله من المفضلة» takes it off. Rows: R-317
- **S-27 «الآفاق» never black.** With the background on «الآفاق» the home shows a scene; if the TV cannot draw one, the home shows its photos instead of black, and `logcat | grep -i scene` says which scene failed. Rows: R-319
- **S-28 Matches card, today only.** In Settings, المباريات, turn on «بطاقة المباريات لليوم فقط»: the «المباريات» card beside the clock is titled «مباريات اليوم» and shows only today's games (a game earlier today keeps its result), «لا مباريات اليوم» when none; off, it is the week's games as before. Rows: R-320
- **S-29 The big clock with no card.** With `optimize/saver.sh home-card none` and «الساعة: زجاجية»: the big glass clock is in the middle and the date above it (`logcat | grep -i scene` says «scene 1 of N» for each scene). With a card on, the small clock stays, as the owner chose. Rows: R-321
- **S-30 The home at 720p.** With the home up, `wm size 1280x720`: the home lays itself out again in a second (apps row and card present); `wm size reset` brings it back. A round of keys in «كل التطبيقات» logs no «frozen-screen recovered» while the focus moves. Rows: R-322
- **S-31 A silent ready station restarts.** Play a station while another app holds the sound (or when the TV refuses it): after about 20 seconds the station starts again by itself and plays (logcat: «station ready but not playing for 20 s»), not «state 0» for minutes. Rows: R-323
- **S-32 The date under the greeting.** With `optimize/saver.sh home-card none` and «الساعة: زجاجية» the date sits under the greeting row, never over «مساء الخير» or the Wi-Fi icon. Rows: R-324
- **S-33 «كنبة»: the big clock stays.** With the couch preset and a card (`saver.sh home-card stocks`): the big glass clock is in the middle with the date over it and the strip under it, no small clock top right; `home-card none`: the clock is in the same place. At `wm size 1280x720` (density 213) the clock, the strip and the apps row all fit. Rows: R-326
- **S-34 Club logos stay.** With several whole leagues followed, the matches card and the league pages keep every club's logo while the page refreshes for a few minutes (none disappears and comes back). Rows: R-327
- **S-35 The Ja'fari calculation is back.** Settings, «طريقة الحساب»: «تلقائي»، «الخليج»، «جعفري (شيعة)». Choosing «جعفري» changes the prayer times on the home, the screensaver and the adhan (Fajr about 16°, Isha 14°), and the line under the clock says «الحساب الجعفري»; `saver.sh prayermethod auto` brings the country's own back. Rows: R-328
- **S-36 The adhan per prayer: sound and screen apart.** Settings, «الأذان لكل صلاة»: five prayers, each «صوت وشاشة»، «صوت فقط»، «شاشة فقط»، «لا شيء». Set Maghrib to «صوت فقط» (`optimize/music.sh adhan state maghrib sound`): before Maghrib no bar and no notice, the adhan still plays. Set it to «شاشة فقط»: the bar shows and no sound. The other prayers stay as they were. Rows: R-329
- **S-37 «أذاني من ملف».** تلاوة ← الأذان ← «المؤذن»: the last row is «أذاني من ملف». OK opens «صوتياتي» on «أضف من فلاشة أو التنزيلات» (the TV's «التنزيلات» and a stick if one is in). Pick an mp3: it says «صار أذانك ✓», the screen closes, and «المؤذن» now has «أذاني الخاص» chosen. The same from «مؤذن الفجر» makes the Fajr adhan. Rows: R-330
- **S-38 Add photos from a stick or Downloads.** Settings › شاشة التوقف › «أضف من فلاشة أو التنزيلات» (below «صوري»): the first time it asks to read photos; allow. A grid of the photos on the stick, in Downloads, Pictures and DCIM appears. OK on one: «أضيفت إلى صوري ✓»; back, and «صوري» counts one more. A photo straight from a camera is added too (no location data). Rows: R-331
- **S-39 The muezzin, the duas and my sounds have rows.** الإعدادات ← الصلاة والأذان: «المؤذن» (the muezzin's name under it) opens the voices, the last row «أذاني من ملف»؛ «الأدعية والتذكيرات» opens the duas; «صوتياتي» opens the list of files. Send a sound from the phone page («صوتياتي» ← «دعاء»): the duas row says «جديد من الجوال: ١» until its time is set. Searching «دعاء» finds the duas row, «اذاني» the muezzin. Rows: R-332
- **S-40 Cancel on the USB debugging question, and a big photo from a stick.** «جهّز كل شي» (the factory page): when «Allow USB debugging?» comes press «إلغاء»: the screen says «ضغطت «إلغاء»…» and offers to try again (also with «إضافات تلفزيونك» installed). Then الإعدادات ← صوري والخلفيات ← «أضف من فلاشة أو التنزيلات»: a photo straight from a phone camera (12 MP or more) is added («أضيفت إلى صوري ✓»), and «صوري» counts one more. Rows: R-334, R-336
- **S-41 The home's tray jank (a re-check of «Known bugs», tester step 71, 73, 76).** `tester/auto/round.sh` on the owner's PC, the `health` part: `raw/gfxinfo.txt` of the home is kept; the report's jank number must be under 15 % (the limit in `tester/auto/thresholds.tsv`). If it is over, send `raw/gfxinfo.txt` and `raw/start-home.txt` (the cause is found from the framestats; «الوضع الخفيف» already lowers it). The round counts only the walk after a warm-up.
- **S-42 The control centre (a re-check, step 46).** «مركز التحكم»: the last row's title is not cut, «تخصيص» opens at the top of its list (not scrolled), Back closes only the panel and leaves the home. «الإعدادات» is its first row (R-346 and the settings entry).
- **S-43 Setup «Done» and the mute after a restart (a re-check, step 39).** «جهّز كل شي» to its end: «تم» closes the page; after a TV restart the music stream is not unmuted about 20 seconds later (`logcat -s UrTVMute` says who; empty is fine when nothing changed). The Home step says it worked when Home works (R-358: the add-on is waited for 30 s).
- **S-44 The old visual notes (re-checks, steps 27, 28, 34, 35, 37, 38).** One look each, on 1080p and with English: the matches page's source line does not touch its big title (R-021); the home's stocks card has the same inner padding as the matches card; the converter shows «0» for zero and the amount line has separators; the markets header does not say «أسعار متأخرة» for five-minute-old data; English: «Started 4:25…» is not cut and a three-button stock card is even; «رملي»: the reciter letters are readable and the reminder card is not over the clock; the screensaver's matches card does not show last evening's game after midnight. A FAIL names the line.
- **S-45 The league chooser's tick boxes (R-350).** «الدوريات كاملة» with «كل البطولات الكبرى» on: the ticked ones show a ✓ in a filled box, the others an empty box; pressing a tournament while the switch is off turns it on with that one ticked.
- **S-46 A sound cut off comes back on request (R-357).** Play a station, then `adb shell am kill sa.urtv.home` (or update System WebView): the sound stops; the home's top bar says «توقفت الإذاعة فجأة» with «كمّل الإذاعة» and «لاحقاً». OK starts the station again; «لاحقاً» drops the offer; nothing starts by itself.
- **S-47 The Gregorian date alone (R-355).** Settings › اللغة والموقع › «التاريخ الهجري»: «ما يظهر» removes the Hijri line on the home, its cards and the screensaver; «يظهر» brings it back; with «التاريخ بالأرقام» on it is the Gregorian numbers alone.
- **S-48 «كمّل المشاهدة» on a TV that shares nothing (R-354).** Where the row stays empty (a Xiaomi TV): the row says «ما يشارك» (and does nothing on OK), and `adb logcat -s UrTVRows` prints `watchnext epg=… provider_rows=N shown=M`. Send that line with a report.
- **S-49 The rescue keeps a copy (R-359).** After «جهّز كل شي» and one start of the home, uninstall UrTV (not the rescue app) and open «إنقاذ تلفزيونك»: it turns back on what the setup turned off and says it used the saved copy.
- **S-50 The remote on an Android 8 box (R-362, report row 765).** On a TV of Android 8 (a TCL or another OTT box) after the install, press the D-pad 30 times in the home: the focus moves on every press and never stops. Then turn on «إضافات تلفزيونك» in the TV's accessibility settings and press 30 times again. PASS = the focus moves both times. FAIL = the remote moves briefly and then nothing: write the TV's name and the firmware, and whether the add-on was on.
- **S-58 The accounts page after the setup (R-450).** After «جهّز كل شي» on a Google TV: Settings › Accounts & sign-in opens the Google account with no sync or sign-in error, and the remote's Assistant button answers. `adb shell pm list packages -d` names none of com.google.android.partnersetup, com.google.android.syncadapters.calendar, com.google.android.syncadapters.contacts, com.tcl.usercenter.
- **S-59 The focus stays on its app when the row is built again (R-451).** Move to the third favourite (or an app in the grid) and leave the home in front; from a computer, update any app (`adb install -r` of a small APK). The focus stays on the same app and OK opens it.
- **S-60 The screensaver starts with the home stopped (R-452).** `adb shell am force-stop sa.urtv.home`, then `optimize/start-saver.sh`, and press keys during its first 3 s: no «لا يستجيب» dialog, the page shows within about 2 s, and `adb logcat -d -s ActivityManager | grep -c ANR` stays 0.
- **S-61 The screensaver's settings survive its reinstall (R-453).** Change the city and the clock size, go back to the home and wait a minute; `adb uninstall sa.urtv.screensaver`, install its APK again, open the home: within a minute `optimize/saver.sh show` gives the same city and clock size (`adb logcat -d -s UrTVSaver` says «settings restored from the home's copy»).
- **S-62 The doctor offer after an update (R-382).** On a TV that has UrTV 4.3.6 or older, update to this build, with urtv-data's content/doctor.json on (`{"v":1,"on":true,"code":"…"}`) and the doctor not installed: the top bar says «ساعدنا نحسّن UrTV، ثبّت فحص تلفزيونك، مجاني» with «ثبّته» and «لاحقاً»; «ثبّته» shows Downloader's code on the bar; «لاحقاً» hides it for this build. With the flag off, the doctor installed, or on a fresh install: no bar. «تطبيقات مقترحة» lists «فحص تلفزيونك» only while the flag is on. PASS = all of that. FAIL = a bar on a fresh install or with the flag off.
- **S-51 The Home role at boot (R-401).** On a TV whose Home is UrTV, `optimize/venv/bin/python -I optimize/tv.py --reboot`, then `adb logcat -d -s UrTVSetup | grep 'home role at boot'`: one line names the holder (ours, setup wizard, settings, a known stock home's package or «another app») and never another app's name. Where the TV took Home back after the boot: the role is given back once; a second time, the home shows «التلفزيون رجّع زر الرئيسية لشاشته الأصلية…» once, and not at the next start.
- **S-52 The voice key after a favourites change (R-404).** On the home, open «تعديل المفضلة», move one app, leave it, then press the remote's voice (mic) or search key: the TV's voice search opens. Do the same over YouTube with «إضافات تلفزيونك» on. PASS = it opens both times.
- **S-53 Home on a Xiaomi Google TV (R-405).** On a MiTV-MZTU0 (or AFMU0/1, AXTU0) after «جهّز كل شي», press Home 5 times over YouTube and 5 times on the home: UrTV every time, Google TV's home never shows; `adb logcat -s UrTVWatch` says «home key caught ours opened». Then a remote app button (YouTube) and the volume: they work as before.
- **S-54 The app buttons over another app (R-406).** On a TCL (Android 12 or newer) with UrTV as the home and «إضافات تلفزيونك» on: open YouTube, press the remote's Netflix button: Netflix opens; press Prime Video: Prime Video opens (where installed). An app that is not installed: nothing changes, and `adb logcat -s UrTVType` says «key app=noyoutube».
- **S-55 The Settings key on a weak TV (R-407).** On a TV of 2 GB or less whose stock home the setup turned off, with «إضافات تلفزيونك» on: open YouTube, press the remote's Settings key: Android's settings open (the PIN first when «قفل إعدادات التلفزيون» is on). Then move around YouTube for a minute: no key is slower than before.
- **S-56 A TV whose setup wizard keeps Home (R-409).** On a TCL or Xiaomi where the setup said «وسيط إعداد التلفزيون ماسك زر الرئيسية»: press the remote's Home over YouTube and on the TV's settings, 5 times each: UrTV every time. The report has a «Setup wizard:» line. The «تلفزيونك» tile opens UrTV too.
- **S-57 Boot to UrTV (R-410).** On a Changhong or Skyworth with UrTV as the home: `optimize/dock.sh boot-home status` says on (this TV's default); pull the power, plug it back: within about 10 s of the TV's own screen, UrTV is in front (`adb logcat -s UrTVBoot`). On another brand it says off and nothing changes at boot.

