# What تلفزيونك already does: look here before you build anything

The same features explained for people, in Arabic: [FEATURES.md](FEATURES.md).

For an agent working on a TV that has تلفزيونك, during the setup or any time after. When the person asks for
something about the screensaver, the home screen or the TV, find it in the tables below and run its command.

## Rules

- **Use the setting.** Everything below is already in the apps; one command changes it, the screen redraws, and
  it stays through every update.
- **Never build or patch the apps for a request.** No editing `android/`, `src/` or `dist/`, no building an APK,
  no installing your own build, no turning the automatic update off. The next update would undo it, a build of
  your own can't update itself afterwards, and the TV is left on a version nobody else has.
- **Not in the tables?** Tell the person, in Arabic, that it isn't in تلفزيونك yet, and that their words can go
  to the developer as a suggestion: add them to the share message at the end (`optimize/share.sh --preview
  --owner "<their words>" …`, see `docs/SETUP-PROMPT.md` step 8). Don't make it yourself.
- **Close to one?** Offer the nearest setting (a trip date: the countdown; a darker card: `cardstyle dark1`).
- **After a change:** read it back (`optimize/saver.sh show`, `optimize/dock.sh show`) and look at a screenshot
  (`optimize/shot.sh screensaver` or `optimize/shot.sh home`, the `-small.png`) before you say it's done.
- `optimize/saver.sh`, `optimize/dock.sh` and `optimize/music.sh` with no arguments list every option.
  Names and texts go in single quotes; no apostrophes inside them.

## The screensaver «شاشة تلفزيونك»

| They ask for | The setting | Command |
|---|---|---|
| their city, prayer times for it | المدينة | `optimize/saver.sh cities`, then `optimize/saver.sh city <name>` (a short form is fine if only one city matches); not listed: `optimize/saver.sh city '<name>' <lat> <lon> <time zone>` (`… 25.86 43.49 Asia/Riyadh` in Saudi Arabia, `… 29.37 47.98 Asia/Kuwait` elsewhere); a place that is not in the list is refused without its time zone («add the time zone») |
| their clubs, the matches | الفرق المتابعة (up to 20) | `optimize/saver.sh find-team <club>` (Arabic or English), then `optimize/saver.sh teams <id:Name> …` in their order; **no team at all** (the defaults, الهلال والمنتخب السعودي, are on until you change them): `optimize/saver.sh teams none` (`teams ""` does the same); with no team the bottom-left slot shows the weather |
| a whole league, every match of its clubs | الدوريات كاملة (up to 5) | `optimize/saver.sh leagues 'Saudi-Arabian Pro League'` (the Arabic name, «دوري روشن السعودي», works too; `""` = none) |
| Formula 1, UFC, boxing | رياضات أخرى | `optimize/saver.sh events f1,ufc,boxing` (`""` = none) |
| what shows beside the clock | المحتوى | `optimize/saver.sh card <verse, wisdom, adhkar, stocks, tasks, year, shows, prayers, forecast, hourly, prayerrow, matches, none>` |
| verse / hadith of the day | آية اليوم وحديث اليوم | `optimize/saver.sh card verse` (a verse one day, a hadith the next) |
| the year, a dot for every day | السنة | `optimize/saver.sh card year` (365 dots, 366 in a leap year; days gone light up toward gold, today glows; «اليوم ٢٩٦ من ٣٦٥ · ٨١٪» under it; also `home-card year`) |
| a slim year under the clock, every day | شريط السنة تحت الساعة | `optimize/saver.sh yearstrip on` (`off` = the default): 365 dots (366 in a leap year) in 7 rows of 53 under «بتوقيت الرياض», from the right, passed days toward gold, today glowing white, and no words under them (the big `year` card keeps its «اليوم ٢٩٦ من ٣٦٥» line); screensaver only (not the home screen); it hides by itself when the clock leaves no room; the big `year` card stays a choice |
| a proverb or saying | حكمة اليوم | `optimize/saver.sh card wisdom` |
| morning and evening adhkar | أذكار الصباح والمساء | `optimize/saver.sh card adhkar` (morning Fajr–Dhuhr, evening Asr–Maghrib, end of al-Baqara at night; else the verse) |
| today's prayer times as a list | مواقيت الصلاة اليوم | `optimize/saver.sh card prayers` (the next prayer is always at the bottom right) |
| the weather for the coming days | طقس الأيام القادمة | `optimize/saver.sh card forecast`, or the bottom-left `optimize/saver.sh slot weather` |
| the five prayers in one row | الصلوات في صف | `optimize/saver.sh card prayerrow` or `home-card prayerrow`: الفجر، الظهر (الجمعة on Friday)، العصر، المغرب، العشاء side by side, the next one lit, and above them «المغرب بعد ١ س و ٥ د» (after Isha the next is tomorrow's Fajr) |
| the day in one card | يومك | `optimize/saver.sh card today` or `home-card today`: the next prayer and its time; the next game of a followed team; the first open task; the next occasion with its days. A line with nothing to say is left out |
| a card that picks itself by the time | البطاقة الذكية | `optimize/saver.sh card smart` or `home-card smart`, any place: a followed team live or within 60 minutes → the matches card (that game); within 15 minutes of a prayer → the prayer row (it wins over the adhkar window); the adhkar window (morning Fajr–Dhuhr, evening Asr–Maghrib) → adhkar; else the next hours' weather. Checked every minute, no request of its own |
| the weather for the next hours | طقس الساعات القادمة | `optimize/saver.sh card hourly` (the next 6 hours, each with its time, icon and temperature; also `home-card hourly`; it uses the same weather request as the forecast, every 30 minutes) |
| new episodes of series | جديد المسلسلات | `optimize/saver.sh card shows`; filters `genres`, `langs`, `minrating` (`optimize/saver.sh` lists them); «مسلسلات مختارة» is in the app's settings |
| their stocks | الأسهم | `optimize/saver.sh stocks 2222 1120 AAPL` (up to 8: 4 digits = Tadawul, letters = US) and `optimize/saver.sh card stocks`. A price that could not be refreshed stays, dimmed, with «آخر تحديث ٣:١٥ م» (its fetch time; «غير محدّث» after 24 hours): that is the TV's network or the price source, not the setting |
| a to-do list | المهام | `optimize/saver.sh tasks add '<text>'` (also `list`, `done N`, `del N`, `clear`) and `optimize/saver.sh card tasks`; on the home screen the «المهام» pill, with voice; and when `home-card` has `tasks` (any of the three places) that card is itself a button: the D-pad reaches it from the pills, it shows a small mic mark and a gold ring, OK opens the list with the focus on «🎤 تكلّم» so one more OK starts adding by voice; the other cards are not focusable (nothing to set) |
| a different look for that card | شكل البطاقة | `optimize/saver.sh cardstyle <current, dark1, glass1, dark3, glass3>` |
| numbers as 123, not ١٢٣ | الأرقام | `optimize/saver.sh digits latin` (`hindi` = ١٢٣, the default); it reaches every screen of both apps, the page and the Java ones (the home clock, date, greeting, pills, «يُتلى الآن», «تلاوة وأذان», «المهام», the settings), through one helper each (`DigitsText`) |
| no green on National / Founding Day | المناسبات الوطنية | `optimize/saver.sh national off` (`auto` is the default) |
| what the bottom-left shows | الخانة اليسرى | `optimize/saver.sh slot <matches, weather, fasting, countdown, markets, none>` |
| the next voluntary fast | الصيام | `optimize/saver.sh slot fasting` |
| days left to a date (a trip, a wedding) | العدّ التنازلي | `optimize/saver.sh countdown <YYYY-MM-DD> '<name>'` and `optimize/saver.sh slot countdown` (`countdown clear` removes it) |
| markets at the top left | الأسواق | `optimize/saver.sh markets <up to 4 of tasi, aramco, spx, btc, gold, silver, oil, xrp>` |
| prices, not just arrows | عرض الأسواق | `optimize/saver.sh mkstyle prices` (`arrows` is the default) |
| the next occasion, Ramadan, Eid | المناسبة القادمة | always shown in the middle at the bottom; nothing to set |
| the photo to change more or less often | تبديل الصورة كل | `optimize/saver.sh photos <5, 10, 15, 30, 60>` (minutes) |
| the screensaver to start sooner or later | بدء شاشة التوقف بعد | `optimize/saver.sh idle <1, 2, 5, 10, 15, 30>` (minutes; the TV's own timer) |
| their own photos as the background (a player, their country, family), 1 to 10, with ours or alone | صوري | `optimize/photos.sh add <files they named>` (made ready here: upright, screen size, no location or camera data), then `optimize/photos.sh mode mix` (with ours, the default) or `optimize/photos.sh mode only`; `optimize/photos.sh list`, `optimize/photos.sh del N`, `optimize/photos.sh clear` (back to ours only), `optimize/photos.sh show` (a screenshot with them; it waits 70 s for the page to settle, so allow 80 s). Ask where the files are; never search their computer |
| the newest screen now | تحديث الشاشة الآن | `optimize/saver.sh update` (it also updates by itself; keep «التحديث التلقائي للصفحة» on) |
| to turn our screensaver off (it slows the TV, or they don't want it), and the TV's own back | (ADB only) | `optimize/saver.sh off` (puts back the one from before the setup, else one the TV lists, else none; ours stays installed with its settings; ask first) |
| our screensaver again | (ADB only) | `optimize/saver.sh on`; `optimize/saver.sh show` says, on its last line, which one is active |
| which version they have, how to update | the last lines of the settings | `optimize/saver.sh show` (first line: `version: …`, and the newest one if known); to update the apps, `docs/SETUP-PROMPT.md` (the settings stay) |
| to see the screensaver now | معاينة الشاشة | `optimize/start-saver.sh`, or `optimize/shot.sh screensaver` |
| everything as it is now | | `optimize/saver.sh show` |

## The home screen «تلفزيونك»

| They ask for | The setting | Command |
|---|---|---|
| their favourite apps in the row, in order | ترتيب التطبيقات المفضلة | `optimize/dock.sh apps`, then `optimize/dock.sh set <pkg> …` (5–10, in their order); `optimize/dock.sh show`, `optimize/dock.sh reset` |
| an app hidden, or a missing one shown | | `optimize/dock.sh hidden` (what is left out and why), `optimize/dock.sh hide <pkg> …`, `optimize/dock.sh show <pkg> …` |
| app names in Arabic | أسماء التطبيقات | `optimize/dock.sh names`, then `optimize/dock.sh name <pkg> '<الاسم>' …` for each still in English (`''` = its usual name; `names-clear` = all back) |
| cards on the home screen, up to three | البطاقة الأولى، الثانية، الثالثة | `optimize/saver.sh home-card prayers,hourly,verse` (a comma list, up to 3, in this order; one value works; `none` or `""` = no card). Every choice of `card` works in any of the three places. One card is the size it always was; two sit side by side (the first on the right); a third goes full width under them; all left of the clock, above the app title and the dock. A card that does not fit its place steps down in text size and rows (a verse or a dhikr takes a shorter one). Each also follows `cardstyle`. The same card twice is shown once |
| club crests on the home screen's matches | nothing to set | `optimize/saver.sh home-card matches` shows each club's badge beside its name (a club with no badge shows its name alone); the screensaver's own matches have none. A game is one line on the home screen's card and on the screensaver's: «الهلال [badge] ٢ - ١ [badge] النصر», then the date and «بعد ٧ ساعات» (or «انتهت» with the day it was played; with no score «الأهلي [badge] × [badge] الاتحاد»). When the line is too long: the clubs' short names, then a shorter date («الجمعة ٩:٠٠ م» this week, «٢٤ أكتوبر» later), then a slightly smaller size, never «…»; nothing to set. While a followed team plays, that game shows alone and big (score, minute, «مباشر», both badges) in the matches card and in the bottom-left slot, until it ends |
| the greeting above the home clock, with the weather | التحية والطقس | on by default: «صباح الخير» from Fajr to Dhuhr, «طاب يومك» to Asr, «مساء الخير» to Isha, «تصبح على خير» after, and under it «سماء صافية في الرياض، ٣١°» with the sunset or sunrise; `optimize/saver.sh greeting off` brings back the plain good-morning with no weather (one weather request every 30 minutes while on) |
| the TV to sleep after a while | مؤقت النوم | the «مؤقت النوم» pill (15 / 30 / 60 / 90 / cancel, the minutes left show in it); from the computer `optimize/dock.sh sleep 30`, `sleep` (how it stands), `sleep cancel`, `sleep-setup` (turns on the sleep service once, over ADB). At the end it stops the sound, then turns the screen off through the sleep service (an accessibility service: the lock-screen action, Android 9+); without the service it brings the screensaver up with no wake lock and the TV's own idle timer carries on. It cannot press power or send HDMI-CEC standby, and it is lost if the TV restarts |
| the Quran, Quran radio, the adhan screen | تلاوة وأذان | the «تلاوة وأذان» pill (while a surah or a station is on, a small «يُتلى الآن» card shows at the right end of the pills row: the surah or station, who recites it and the sleep timer if set; «اختيار» (the OK key) pauses/plays, left/right the previous/next surah, a long press opens the player; it goes when playback stops, nothing to set; `status` below is what it shows). While a surah or a station is playing, the screensaver also shows a small «يُتلى الآن · سورة الكهف · ماهر المعيقلي» card at the top centre, in the style picked for «شكل البطاقة» (it asks the home app when it starts and once a minute; no setting); from the computer `optimize/music.sh status`, `reciters`, `stations`, `surah <reciter> <1-114>`, `radio <n>`, `pause`, `resume`, `toggle`, `next`, `prev`, `seek <seconds>`, `stop`, `sleep <minutes>` |
| a slim bar before the adhan | تنبيه قبل الأذان | on by default: on the screensaver and the home screen, «الأذان بعد ٨ دقائق» in the last 10 minutes before each prayer and «الإقامة بعد ١٢ دقيقة» from the adhan to the iqama (Fajr 25, Maghrib 10, the rest 20, none for Friday's Dhuhr); `optimize/saver.sh adhanbar off` (or `on`) |
| the adhan (or the iqama) is a few minutes early or late for their mosque | تعديل وقت الصلاة (off by default) | `optimize/saver.sh prayeradj fajr +2 maghrib -1 …` (fajr dhuhr asr maghrib isha, whole minutes from -20 to +20, the others stay) then `optimize/saver.sh prayeradj on`; `prayeradj off` goes back to the calculated times without forgetting the minutes; `optimize/saver.sh show` lists them (`prayeradj: on fajr=+2 dhuhr=0 …`). The moved times are the ones used everywhere: the screensaver (next prayer, the card of the prayers, the row, the adhan bar, the iqama countdown, the smart card, «يومك»), the home screen (greeting, adhan bar, cards), the adhan alarm and the prayer rows of «تلاوة وأذان». The sunrise is not a prayer and never moves. On the TV: screensaver settings ← «تعديل وقت الصلاة», then each prayer with left (+) and right (−), OK for zero |
| the adhan at prayer times | الأذان | `optimize/music.sh adhan` (status, with the size of their own recording), `adhan on` / `off`, `adhan voice <1-21>`, `adhan volume <10-100>`, `adhan now`; `optimize/music.sh adhan` also says `adhan_exact=0` when the TV does not allow exact alarms (the adhan may then be minutes late: «المنبّهات الدقيقة» in the TV's app settings) |
| the adhan in a recording of their own (a reciter whose recordings we can't give; the person brings the file) | أذان من ملفي | `optimize/music.sh adhan add <file>` (any audio file: made small on this computer with ffmpeg, mono AAC about 48 kbps, at most 5 minutes (longer is cut at 5:00 with a short fade, and it says so), uploaded to the TV's home app and made the voice; if ffmpeg is missing it says what to install; one file only, a new one replaces it), `adhan voice own` (`adhan voice 3` goes back to ours), `adhan own-clear` removes it; `adhan` shows its size. In «تلاوة وأذان» ← الأذان ← المؤذن a row «أذان من ملفي» is first when there is a file. **Never bundle or upload one: it is theirs, it stays on their TV only** |
| HDMI inputs, Wi-Fi, the TV's settings | | the «المصادر», «واي فاي» and «الإعدادات» pills on the home screen: every pill in the top row shows its icon only, the one with the focus widens to show its name (the Wi-Fi pill: the network's name; the sleep pill: the minutes left, and a gold icon while it runs); nothing to set |

## The TV itself

| They ask for / it happened | Command |
|---|---|
| something protected went off (AirPlay, casting, voice, updates) | `optimize/keep.sh` |
| a TV our own lists do not know | `optimize/community.sh` (a run.sh step) uses the CleanYourTV list for exactly this model if there is one (`optimize/lists/community/`, made by `.github/tools/community_lists.py`, which the owner can run again for another batch); the packages are in `report.md` under «Community list» and in `keep.sh off`; `reset.sh` turns them back on |
| what the setup turned off, by name («تنبيهات TCL»…) | `optimize/keep.sh off` (also in `report.md`, under «Turned off by the setup») |
| «I want this one back» (an app the cleanup turned off) | `optimize/keep.sh restore <package> …` (the package ids are in `keep.sh off`): turns it on and remembers it in `~/.urtv/kept.txt`, so a later `optimize/run.sh again` leaves it on; `optimize/keep.sh unkeep <package> …` turns it off again. The home screen's own helpers are refused (`optimize/restore.sh` is for those) |
| TCL: the adhan alarms or the sleep timer stop after a restart | `optimize/autostart.sh` (allows auto-start for our two apps on a TCL and writes it to disk; `autostart.sh check` reads it; the setup does it already) |
| the person turned debugging off on the TV, is it really closed? | `optimize/find-tv.sh --closed` (`OK: ADB is closed`, `!! ADB still answers …` or `?? Can't tell` when the TV sleeps). The apps' data and content update without ADB; a new version of the apps and any change of settings from the computer need it, so it is turned on again then |
| a TV system update brought the old home back, or apps back on | `optimize/run.sh again` (settings stay) |
| undo everything, back as it came | `optimize/reset.sh` (the TV's own screensaver comes back with its settings) |
| restart the TV | `optimize/venv/bin/python -I optimize/tv.py --reboot` (never a plain `reboot`) |
| how much memory is free | `optimize/measure.sh after --now` |
| a screenshot | `optimize/shot.sh home`, `screensaver`, `settings`, `favourites`, `now <name>` |
| send the developer how it went | `optimize/share.sh --preview …`, then `--send` (`docs/SETUP-PROMPT.md` step 8) |

## Not in تلفزيونك yet (say so, and pass it on with `--owner`)

- Asked for, not planned yet: other countries' photos and prayer methods, moving cards around (layout), a news
  ticker, a flight countdown with the flight number (the countdown above can count to the trip's date).
- Not possible: Samsung (Tizen) and LG (webOS) TVs can't run these apps.
