# UrTV settings findability audit

Branch `work/44b4-settings-ia`, read-only. Ground truth: `SettingsHub.java` (where each row sits, in both settings styles) and `SettingsActivity.render` (the rows, their labels, what OK does). Also checked: the «تلاوة» app's adhan tab (`home/tilawah/MainActivity`, `AudioActivity`, `DuaActivity`), `AppearanceActivity` (opened by «وضع الألوان»), the «تلفزيونك» hub, the control centre, the launcher's entry points, the phone pages (`index.html`, `photos.html`, `audio.html`), `docs/SETTINGS-TREE.md` and `docs/SETTINGS.md`.

**205 features** in total:
- 139 settings rows. The 19 «زر … في الريموت» rows count as one feature.
- 23 inside the «تلاوة» adhan tab.
- 9 inside «وضع الألوان».
- 9 inside other rows.
- 15 in the control centre and home menus.
- 10 phone-only or agent-only.

## Top 10 findability problems (ranked by user impact)

1. **Deleting one of my photos.** الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] صوري ← اختر صورة لحذفها ← photo ← نعم. **~41 presses**, depth 6. It is filed under «البطاقات» and the header «التوقيت», on row 30 of 31, as the 4th item of a mode list. The TV says only the assistant can add photos, and never mentions the phone page, which has a «حذف» button under each photo.
2. **Using my own adhan recording.** الإعدادات ← الأذان ← صوتياتي وأذاني الخاص ← file ← استخدمه لأذاني الخاص. **~19 presses**, plus getting the file onto the TV by USB or phone. It is in another app and the search cannot find it. «المؤذن» has no "add your own", and «صوت الأذان» is the volume, so both rows are decoys.
3. **Duas and reminders with my own sound.** الإعدادات ← الأذان ← الأدعية والتذكيرات ← إضافة… ← sound ← time. **~23+ presses**, depth 5–6. It is not in settings or search, and no section name mentions it. A dua added from the phone arrives switched off and must be timed on the TV. There is no agent command.
4. **Getting to «الإعدادات» at all.** The control-centre tile called **«الإعدادات» opens Android's settings**. Ours is «مركز التحكم» ← «تلفزيونك» ← «الإعدادات», about 12 presses before any count in this file starts.
5. **«وضع الألوان» hides a whole «المظهر» screen.** It holds accent, light/dark, transparency, focus shape, wallpaper dim, and the only TV control for **home photos mode**. 20 presses to open, 23–31 to change anything. The label promises colours only, and none of it is searchable.
6. **The screensaver list is one flat list of 31 rows.** «البطاقات ← شاشة التوقف» costs 30–38 presses for «بدء شاشة التوقف بعد» (38), «تبديل الصورة كل» (35) and «شكل البطاقة» (32). Home-only rows («توهّج لون التطبيق», «نص أوضح») and «الوضع الخفيف» are parked there too.
7. **Unhiding a hidden app.** The hide toast points to «التطبيقات المخفية», a row that does not exist. The real path is الإعدادات ← المزيد ← التطبيقات والمفضلة ← ترتيب كل التطبيقات ← تعديل التطبيقات ← المخفية ← app, **~19+ presses**, depth 6.
8. **Prayer and adhan settings are split across four places.** «الأذان» (another app, 2), «الموقع والصلاة ← طريقة الحساب» (8) and «تعديل وقت الصلاة», «البطاقات ← شاشة التوقف ← تنبيه قبل الأذان» (10) and «الشروق بعد الفجر», and «تلاوة ← تنبيه عند الأذان» (20) next to its near-twin «تنبيه قبل الأذان».
9. **Home behaviour is scattered.** «البطاقات ← الشاشة الرئيسية» is a 30+ row mix: «الرجوع يفتح شاشة التوقف» (25 presses) sits in a different sub-screen from its twin «رجوع مرتين يفتح شاشة التوقف» (24), and «الخروج من تلفزيونك» takes 31 with a label that sounds like turning the TV off. The language, a basic TV-wide setting, is under «المزيد ← اللغة» (13).
10. **The phone link has five names.** The home pill is «الجوال», the button «التحكم من الجوال», the row «الجوالات المسموح لها» (6). The TV's own help and the phone pages tell people to scan the code from «أسهل من الجوال», which exists only on the Markets page. «رسائل الجوال» can be switched off only by an agent.

## How the counts were made

- **Style:** presses are for the **default settings style «أزرار»** (`settingsStyle` = buttons, the 4.3.2 hub). The «القائمة الجانبية» path is given too, without a count.
- **Starting point:** «الإعدادات» has just opened with the focus on the wide first button «المدينة». A button grid has two a line, the first on the right. Moving to button *i* costs ⌈i/2⌉ DOWN, plus one LEFT for a left-hand button, plus OK. The sub-screens «البطاقات» and «المزيد» are the same grid without the wide button.
- **Rows:** in a section, the focus starts on the first row. Reaching row *n* costs n−1 DOWN, plus OK. Headers are not focus stops.
- **Changing:** a switch row changes on that OK (+0). A picker row opens a dialog, counted as +2 (one move and OK). A text row is +1 and then typing. A direct button (المدينة, الأذان, مركز التحكم, التحكم من الجوال, أرسل تقرير, الرقابة الأبوية, تصفية المواقع, اللغة, بحث) runs its row at once.
- **Default TV state:** rows that only show conditionally are hidden («مكان الشريط», «العدّ التنازلي», «بطاقة ثانية», «إخفاء المباريات المنتهية», «زر الرئيسية يفتح تلفزيونك مباشرة», «المظهر على الرئيسية», «رجّع الشكل السابق», «رجّع بطاقاتك», «أرسل ملاحظة…», «رجّع واجهة المصنع»). The verse rows are shown, because the default card is verse/hadith. Three remote-key rows are assumed, since the real number depends on the remote. Counts on a given TV can differ by a few.
- **Depth:** each button, row, dialog or inner screen chosen below «الإعدادات».
- **Reachable by:** remote = on the TV with the remote; phone = the phone page; agent = an `optimize/*.sh` command; control centre = the home's control centre.

## A. Settings rows (grouped by the default screen's sections)

### Adhan (direct) — «الأذان»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 1 | Adhan settings (opens تلاوة app, adhan tab) | الأذان | الإعدادات ← الأذان (زر مباشر) | الإعدادات ← الصلاة والأذان ← الأذان | 2 | 1 | yes: clear, but hides 20 rows | remote + phone + agent |

### Location & prayer — «الموقع والصلاة»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 2 | Country | الدولة | الإعدادات ← الموقع والصلاة ← [الموقع] الدولة | الإعدادات ← الموقع والطقس ← الدولة | 6 | 3 | yes: obvious | remote + agent |
| 3 | City (weather, prayer times) | المدينة | الإعدادات ← الموقع والصلاة ← [الموقع] المدينة | الإعدادات ← الموقع والطقس ← المدينة | 1+typing (wide first button «المدينة»); 7+typing via «الموقع والصلاة» | 3 | yes: obvious, also first tile | remote + phone + agent + control centre |
| 4 | Prayer calc method | طريقة الحساب | الإعدادات ← الموقع والصلاة ← [الموقع] طريقة الحساب | الإعدادات ← الصلاة والأذان ← طريقة الحساب | 8 | 3 | yes: clear | remote + agent |
| 5 | Per-prayer minute offsets | تعديل وقت الصلاة | الإعدادات ← الموقع والصلاة ← [الموقع] تعديل وقت الصلاة | الإعدادات ← الصلاة والأذان ← تعديل وقت الصلاة | 7 | 2 | yes: clear | remote + agent |

### Clock & digits — «الساعة والأرقام»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 6 | Digits ١٢٣ vs 123 | الأرقام | الإعدادات ← الساعة والأرقام ← الأرقام | الإعدادات ← اللغة ← الأرقام | 6 | 3 | yes: clear; values show both forms | remote + agent |
| 7 | Date as numbers | التاريخ بالأرقام | الإعدادات ← الساعة والأرقام ← التاريخ بالأرقام | الإعدادات ← اللغة ← التاريخ بالأرقام | 5 | 2 | yes: says exactly what changes | remote + agent |
| 8 | Gregorian month names (Levant/Gulf) | أسماء الأشهر الميلادية | الإعدادات ← الساعة والأرقام ← أسماء الأشهر الميلادية | الإعدادات ← اللغة ← أسماء الأشهر الميلادية | 8 | 3 | yes: clear, slightly long | remote + agent |
| 9 | 12/24-hour clock | نظام الساعة | الإعدادات ← الساعة والأرقام ← نظام الساعة | الإعدادات ← اللغة ← نظام الساعة | 9 | 3 | yes: common phrase; docs say «ساعة ٢٤» | remote + agent |
| 10 | Show seconds | إظهار الثواني | الإعدادات ← الساعة والأرقام ← إظهار الثواني | الإعدادات ← المظهر ← [الساعة والنص] إظهار الثواني | 8 | 2 | yes: clear | remote + agent |
| 11 | Hijri date shift ±2 days | فرق التاريخ الهجري | الإعدادات ← الساعة والأرقام ← فرق التاريخ الهجري | الإعدادات ← اللغة ← فرق التاريخ الهجري | 11 | 3 | yes: clear for anyone who needs it | remote + agent |

### Quran & adhan — «تلاوة وأذان»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 12 | Open Quran/adhan app | افتح تلاوة وأذان | الإعدادات ← تلاوة وأذان ← افتح تلاوة وأذان | الإعدادات ← الصلاة والأذان ← افتح تلاوة وأذان | 5 | 2 | yes: clear | remote |
| 13 | Show Quran/adhan button | زر تلاوة وأذان | الإعدادات ← تلاوة وأذان ← زر تلاوة وأذان | الإعدادات ← الأزرار السريعة ← زر تلاوة وأذان | 6 | 2 | yes: clear | remote + agent |

### Cards → Screensaver — «شاشة التوقف»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 14 | Preview screensaver | معاينة الشاشة | الإعدادات ← البطاقات ← شاشة التوقف ← معاينة الشاشة | الإعدادات ← شاشة التوقف ← معاينة الشاشة | 6 | 3 | yes: clear | remote + phone (start now) + agent |
| 15 | Lite mode for weak TVs | الوضع الخفيف | الإعدادات ← البطاقات ← شاشة التوقف ← الوضع الخفيف | الإعدادات ← شاشة التوقف ← الوضع الخفيف | 9 | 4 | vague: light = theme or performance? | remote + agent |
| 16 | Pre-adhan bar (10/15 min) | تنبيه قبل الأذان | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] تنبيه قبل الأذان | الإعدادات ← الصلاة والأذان ← تنبيه قبل الأذان | 10 | 4 | yes: clear, filed under saver | remote + agent |
| 17 | Sunrise after Fajr | الشروق بعد الفجر | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] الشروق بعد الفجر | الإعدادات ← الصلاة والأذان ← الشروق بعد الفجر | 9 | 3 | yes: clear | remote + agent |
| 18 | Progress bar between prayers | شريط الوقت بين الصلاتين | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] شريط الوقت بين الصلاتين | الإعدادات ← الصلاة والأذان ← شريط الوقت بين الصلاتين | 10 | 3 | yes: clear | remote + agent |
| 19 | Humidity & wind on screensaver | الرطوبة والرياح | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] الرطوبة والرياح | الإعدادات ← الموقع والطقس ← [الطقس] الرطوبة والرياح | 11 | 3 | yes: clear, but filed oddly | remote + agent |
| 20 | Year dots strip | شريط السنة تحت الساعة | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] شريط السنة تحت الساعة | الإعدادات ← شاشة التوقف ← [بجانب الساعة] شريط السنة تحت الساعة | 12 | 3 | yes: clear | remote + agent |
| 21 | Album speed | سرعة ألبوم صوري | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] سرعة ألبوم صوري | الإعدادات ← المظهر ← [ألبوم صوري] سرعة ألبوم صوري | 15 | 4 | yes: clear once you know album | remote + agent |
| 22 | Album photo date on/off | تاريخ الصورة في ألبوم صوري | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] تاريخ الصورة في ألبوم صوري | الإعدادات ← المظهر ← [ألبوم صوري] تاريخ الصورة في ألبوم صوري | 14 | 3 | yes: clear | remote + agent |
| 23 | Album title on/off | عنوان ألبوم صوري | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] عنوان ألبوم صوري | الإعدادات ← المظهر ← [ألبوم صوري] عنوان ألبوم صوري | 15 | 3 | yes: clear | remote + agent |
| 24 | Album fit/fill | شكل صورة ألبوم صوري | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] شكل صورة ألبوم صوري | الإعدادات ← المظهر ← [ألبوم صوري] شكل صورة ألبوم صوري | 18 | 4 | vague: «شكل» of photo unclear | remote + agent |
| 25 | Album size | حجم ألبوم صوري | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] حجم ألبوم صوري | الإعدادات ← المظهر ← [ألبوم صوري] حجم ألبوم صوري | 19 | 4 | yes: clear | remote + agent |
| 26 | App colour glow | توهّج لون التطبيق | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] توهّج لون التطبيق | الإعدادات ← المظهر ← [التطبيقات] توهّج لون التطبيق | 20 | 4 | vague: cosmetic, filed under screensaver | remote + agent |
| 27 | Clearer (brighter) text | نص أوضح | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] نص أوضح | الإعدادات ← المظهر ← [الساعة والنص] نص أوضح | 19 | 3 | yes: clear, filed under screensaver | remote + agent |
| 28 | Next occasion (Ramadan/Eid) line | المناسبة القادمة | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] المناسبة القادمة | الإعدادات ← شاشة التوقف ← [بجانب الساعة] المناسبة القادمة | 20 | 3 | vague: users say «رمضان»/«العيد» | remote + agent |
| 29 | Night dimming | تعتيم الليل | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] تعتيم الليل | الإعدادات ← المظهر ← [الساعة والنص] تعتيم الليل | 21 | 3 | yes: clear | remote + agent |
| 30 | National days | المناسبات الوطنية | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] المناسبات الوطنية | الإعدادات ← شاشة التوقف ← [بجانب الساعة] المناسبات الوطنية | 24 | 4 | yes: clear | remote + agent |
| 31 | Screensaver design | تصميم شاشة التوقف | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] تصميم شاشة التوقف | الإعدادات ← المظهر ← [الساعة والنص] تصميم شاشة التوقف | 25 | 4 | yes: clear | remote + agent |
| 32 | Back twice starts screensaver | رجوع مرتين يفتح شاشة التوقف | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] رجوع مرتين يفتح شاشة التوقف | الإعدادات ← المظهر ← [الساعة والنص] رجوع مرتين يفتح شاشة التوقف | 24 | 3 | yes: clear; twin lives elsewhere | remote + agent |
| 33 | Middle bar content | الشريط في الوسط | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] الشريط في الوسط | الإعدادات ← المظهر ← [الساعة والنص] الشريط في الوسط | 27 | 4 | vague: which bar? where? | remote + agent |
| 34 | Bar position | مكان الشريط | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] مكان الشريط | الإعدادات ← المظهر ← [الساعة والنص] مكان الشريط | 28 (row hidden by default: only when «الشريط في الوسط» is not «بدون») | 4 | vague: which bar? | remote + agent |
| 35 | Weather position | مكان الطقس | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] مكان الطقس | الإعدادات ← المظهر ← [الساعة والنص] مكان الطقس | 28 | 4 | yes: clear | remote + agent |
| 36 | Screensaver clock size | حجم الساعة | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] حجم الساعة | الإعدادات ← المظهر ← [الساعة والنص] حجم الساعة | 29 | 4 | yes: clear | remote + agent |
| 37 | Left slot content | الخانة اليسرى | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] الخانة اليسرى | الإعدادات ← شاشة التوقف ← [بجانب الساعة] الخانة اليسرى | 30 | 4 | vague: which slot, where | remote + agent |
| 38 | Countdown date | العدّ التنازلي | الإعدادات ← البطاقات ← شاشة التوقف ← [العرض] العدّ التنازلي | الإعدادات ← شاشة التوقف ← [بجانب الساعة] العدّ التنازلي | 30+typing (row hidden by default: only with the countdown slot or a date) | 4 | yes: clear, conditional | remote + agent |
| 39 | Card beside clock | المحتوى | الإعدادات ← البطاقات ← شاشة التوقف ← [بجانب الساعة] المحتوى | الإعدادات ← شاشة التوقف ← [بجانب الساعة] المحتوى | 31 | 4 | vague: content of what? | remote + agent |
| 40 | Second card (small clock) | بطاقة ثانية | الإعدادات ← البطاقات ← شاشة التوقف ← [بجانب الساعة] بطاقة ثانية | الإعدادات ← شاشة التوقف ← [بجانب الساعة] بطاقة ثانية | 32 (row hidden by default: only with a small clock) | 4 | yes: clear, conditional | remote + agent |
| 41 | Card style | شكل البطاقة | الإعدادات ← البطاقات ← شاشة التوقف ← [بجانب الساعة] شكل البطاقة | الإعدادات ← المظهر ← [البطاقات] شكل البطاقة | 32 | 4 | yes: clear; duplicated in «وضع الألوان» | remote + agent |
| 42 | Verse/hadith rotation | تبديل الآية والحديث | الإعدادات ← البطاقات ← شاشة التوقف ← [بجانب الساعة] تبديل الآية والحديث | الإعدادات ← شاشة التوقف ← [بجانب الساعة] تبديل الآية والحديث | 33 | 4 | yes: clear, conditional | remote + agent |
| 43 | Verse vs hadith kind | الآية والحديث | الإعدادات ← البطاقات ← شاشة التوقف ← [بجانب الساعة] الآية والحديث | الإعدادات ← شاشة التوقف ← [بجانب الساعة] الآية والحديث | 34 | 4 | yes: clear, conditional | remote + agent |
| 44 | Photo change interval | تبديل الصورة كل | الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] تبديل الصورة كل | الإعدادات ← المظهر ← [الصور] تبديل الصورة كل | 35 | 4 | yes: clear | remote + agent |
| 45 | Photo packs (ours + downloadable) | صور شاشة التوقف | الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] صور شاشة التوقف | الإعدادات ← المظهر ← [الصور] صور شاشة التوقف | 34 | 3 | vague: collides with «صوري» next row | remote |
| 46 | My photos: mode, delete one, delete all | صوري (n/max) | الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] صوري | الإعدادات ← المظهر ← [الصور] صوري | 37 | 4 | vague: no hint of delete/add | remote + phone (add + delete) + agent |
| 47 | Screensaver idle delay | بدء شاشة التوقف بعد | الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] بدء شاشة التوقف بعد | الإعدادات ← شاشة التوقف ← بدء شاشة التوقف بعد | 38 | 4 | yes: clear | remote + phone + agent |

### Cards → Home screen — «الشاشة الرئيسية»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 48 | Make UrTV the home screen | اجعل UrTV الشاشة الرئيسية | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] اجعل UrTV الشاشة الرئيسية | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] اجعل UrTV الشاشة الرئيسية | 7 | 3 | yes: clear; brand in Latin | remote |
| 49 | Re-run / undo TV setup | إعداد تلفزيوني | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] إعداد تلفزيوني | الإعدادات ← حول ← إعداد تلفزيوني | 8 | 3 | yes: clear | remote |
| 50 | Restore factory launcher | رجّع واجهة المصنع | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] رجّع واجهة المصنع | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] رجّع واجهة المصنع | 9 (row hidden by default: replaces «تخلّص من واجهة المصنع» once the factory home is off) | 3 | yes: plain Gulf, clear | remote |
| 51 | Disable factory launcher | تخلّص من واجهة المصنع | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] تخلّص من واجهة المصنع | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] تخلّص من واجهة المصنع | 9 | 3 | yes: plain Gulf, clear | remote |
| 52 | Read before uninstalling | قبل ما تحذف التطبيق… | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] قبل ما تحذف التطبيق… | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] قبل ما تحذف التطبيق… | 10 | 3 | yes: plain Gulf, clear | remote |
| 53 | Clean (minimal) home mode | UrTV النظيف | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] UrTV النظيف | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] UrTV النظيف | 11 | 3 | jargon: brand + adjective, unclear | remote |
| 54 | Markets/matches pages always dark | صفحات الأسهم والمباريات داكنة | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] صفحات الأسهم والمباريات داكنة | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] صفحات الأسهم والمباريات داكنة | 12 | 3 | yes: clear | remote + agent |
| 55 | Home card 1 | البطاقة الأولى | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] البطاقة الأولى | الإعدادات ← المظهر ← [البطاقات] البطاقة الأولى | 15 | 4 | yes: clear | remote + phone + agent |
| 56 | Home card 2 | البطاقة الثانية | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] البطاقة الثانية | الإعدادات ← المظهر ← [البطاقات] البطاقة الثانية | 16 | 4 | yes: clear | remote + agent |
| 57 | Home card 3 | البطاقة الثالثة | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] البطاقة الثالثة | الإعدادات ← المظهر ← [البطاقات] البطاقة الثالثة | 17 | 4 | yes: clear | remote + agent |
| 58 | Home greeting + weather line | التحية والطقس | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] التحية والطقس | الإعدادات ← الموقع والطقس ← [الطقس] التحية والطقس | 16 | 3 | yes: clear | remote + agent |
| 59 | Your name in the greeting | اسمك في التحية | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] اسمك في التحية | الإعدادات ← الموقع والطقس ← [الطقس] اسمك في التحية | 18+typing | 4 | yes: clear, friendly | remote + agent |
| 60 | Show inputs button | زر المصادر | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر المصادر | الإعدادات ← الأزرار السريعة ← زر المصادر | 18 | 3 | jargon: «مصادر» = HDMI inputs | remote + agent |
| 61 | Show markets button | زر الأسواق | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر الأسواق | الإعدادات ← الأزرار السريعة ← زر الأسواق | 19 | 3 | yes: clear | remote + agent |
| 62 | Show matches button | زر المباريات | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر المباريات | الإعدادات ← الأزرار السريعة ← زر المباريات | 20 | 3 | yes: clear | remote + agent |
| 63 | Show phone button | زر الجوال | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر الجوال | الإعدادات ← الأزرار السريعة ← زر الجوال | 21 | 3 | yes: clear | remote + agent |
| 64 | Show search button | زر البحث | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر البحث | الإعدادات ← الأزرار السريعة ← زر البحث | 22 | 3 | yes: clear | remote + agent |
| 65 | Show control-centre button | زر مركز التحكم | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر مركز التحكم | الإعدادات ← الأزرار السريعة ← زر مركز التحكم | 23 | 3 | yes: clear | remote + agent |
| 66 | Show tasks button | زر المهام | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر المهام | الإعدادات ← الأزرار السريعة ← زر المهام | 24 | 3 | yes: clear | remote + agent |
| 67 | Back on home starts screensaver | الرجوع يفتح شاشة التوقف | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] الرجوع يفتح شاشة التوقف | الإعدادات ← الأزرار السريعة ← الرجوع يفتح شاشة التوقف | 25 | 3 | yes: clear; twin under saver | remote + agent |
| 68 | Home key opens UrTV (Google TV) | زر الرئيسية يفتح تلفزيونك مباشرة | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر الرئيسية يفتح تلفزيونك مباشرة | الإعدادات ← الأزرار السريعة ← زر الرئيسية يفتح تلفزيونك مباشرة | 26 (row hidden by default: only with «إضافات تلفزيونك» on a Google TV) | 3 | yes: clear, conditional row | remote + agent |
| 69 | Remote shortcut keys (one row per key on this remote) | زر يوتيوب في الريموت … (one row per key) | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] زر يوتيوب في الريموت | الإعدادات ← الأزرار السريعة ← زر يوتيوب في الريموت | 28 | 4 | yes: clear, many rows | remote + agent |
| 70 | How Back leaves the home | الخروج من تلفزيونك | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] الخروج من تلفزيونك | الإعدادات ← التطبيقات ← [الشاشة الرئيسية للتلفزيون] الخروج من تلفزيونك | 31 | 4 | vague: sounds like quitting/turning off | remote + agent |
| 71 | Updates bell | جرس التحديثات | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] جرس التحديثات | الإعدادات ← الأزرار السريعة ← جرس التحديثات | 30 | 3 | yes: clear | remote + agent |
| 72 | Continue-watching row | أكمل المشاهدة | الإعدادات ← البطاقات ← الشاشة الرئيسية ← [الشاشة الرئيسية (تلفزيونك)] أكمل المشاهدة | الإعدادات ← المظهر ← [البطاقات] أكمل المشاهدة | 31 | 3 | yes: clear | remote + agent |

### Look & backgrounds — «المظهر والخلفيات»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 73 | Live look panel on the home | المظهر على الرئيسية | الإعدادات ← المظهر والخلفيات ← المظهر على الرئيسية | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] المظهر على الرئيسية | 6 (row hidden by default: only with «لوحة المظهر على الرئيسية» on) | 2 | vague: twin of «لوحة المظهر…» | remote + agent |
| 74 | Undo last look change | رجّع الشكل السابق | الإعدادات ← المظهر والخلفيات ← رجّع الشكل السابق | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] رجّع الشكل السابق | 6 (row hidden by default: only after a look change with the undo row on) | 2 | yes: plain Gulf, clear verb | remote + agent |
| 75 | Ready-made looks (presets) | أشكال جاهزة | الإعدادات ← المظهر والخلفيات ← أشكال جاهزة | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] أشكال جاهزة | 8 | 3 | yes: clear | remote + agent |
| 76 | Settings-screen style (buttons/side list/tvOS) | شكل شاشة الإعدادات | الإعدادات ← المظهر والخلفيات ← شكل شاشة الإعدادات | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] شكل شاشة الإعدادات | 9 | 3 | yes: clear but rarely needed | remote + agent |
| 77 | Customise (mix presets) | تخصيص | الإعدادات ← المظهر والخلفيات ← تخصيص | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] تخصيص | 10 | 3 | vague: customise what? no object | remote + agent |
| 78 | Restore your cards after a preset | رجّع بطاقاتك | الإعدادات ← المظهر والخلفيات ← رجّع بطاقاتك | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] رجّع بطاقاتك | 9 (row hidden by default: only after a preset hid cards) | 2 | yes: clear; only appears conditionally | remote + agent |
| 79 | «Couch» look switch | شكل كنبة | الإعدادات ← المظهر والخلفيات ← شكل كنبة | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] شكل كنبة | 9 (row hidden by default: never drawn (PRESETS_REPLACE_COUCH = true) though listed in SETTINGS-TREE) | 2 | jargon: internal design nickname | remote + agent |
| 80 | Home layout (TV/couch/football/tvOS) | التخطيط | الإعدادات ← المظهر والخلفيات ← التخطيط | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] التخطيط | 11 | 3 | vague: layout of what? | remote + agent |
| 81 | Home background (photos/scenes) | الخلفية | الإعدادات ← المظهر والخلفيات ← الخلفية | الإعدادات ← المظهر ← [الخلفية والآفاق] الخلفية | 12 | 3 | yes: clear | remote + agent |
| 82 | Home clock style (plain/glass) | الساعة | الإعدادات ← المظهر والخلفيات ← الساعة | الإعدادات ← المظهر ← [الساعة والنص] الساعة | 13 | 3 | vague: which clock, what about it | remote + agent |
| 83 | App tiles style | شكل التطبيقات | الإعدادات ← المظهر والخلفيات ← شكل التطبيقات | الإعدادات ← المظهر ← [التطبيقات] شكل التطبيقات | 14 | 3 | yes: clear | remote + agent |
| 84 | App bar alignment | محاذاة التطبيقات | الإعدادات ← المظهر والخلفيات ← محاذاة التطبيقات | الإعدادات ← المظهر ← [التطبيقات] محاذاة التطبيقات | 15 | 3 | yes: clear | remote + agent |
| 85 | tvOS bar size | حجم شريط tvOS | الإعدادات ← المظهر والخلفيات ← حجم شريط tvOS | الإعدادات ← المظهر ← [التطبيقات] حجم شريط tvOS | 16 | 3 | jargon: Apple product name | remote + agent |
| 86 | tvOS bar rows | صفوف شريط tvOS | الإعدادات ← المظهر والخلفيات ← صفوف شريط tvOS | الإعدادات ← المظهر ← [التطبيقات] صفوف شريط tvOS | 17 | 3 | jargon: Apple product name | remote + agent |
| 87 | Scene packs | حزم الآفاق | الإعدادات ← المظهر والخلفيات ← حزم الآفاق | الإعدادات ← المظهر ← [الخلفية والآفاق] حزم الآفاق | 18 | 3 | jargon: «آفاق» is a product name | remote + agent |
| 88 | Scene duration | بقاء كل أفق | الإعدادات ← المظهر والخلفيات ← بقاء كل أفق | الإعدادات ← المظهر ← [الخلفية والآفاق] بقاء كل أفق | 19 | 3 | jargon: depends on «آفاق» term | remote + agent |
| 89 | Scene follows time of day | الأفق يتبع الوقت | الإعدادات ← المظهر والخلفيات ← الأفق يتبع الوقت | الإعدادات ← المظهر ← [الخلفية والآفاق] الأفق يتبع الوقت | 18 | 2 | jargon: depends on «آفاق» term | remote + agent |
| 90 | Scene follows weather | الأفق يتبع الطقس | الإعدادات ← المظهر والخلفيات ← الأفق يتبع الطقس | الإعدادات ← المظهر ← [الخلفية والآفاق] الأفق يتبع الطقس | 19 | 2 | jargon: depends on «آفاق» term | remote + agent |
| 91 | Opens whole «المظهر» hub (look, accent, light, sync, cards, photos, density) | وضع الألوان | الإعدادات ← المظهر والخلفيات ← وضع الألوان | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] وضع الألوان | 20 | 2 | vague: hides seven sub-screens inside | remote + agent |
| 92 | Enable look panel row | لوحة المظهر على الرئيسية | الإعدادات ← المظهر والخلفيات ← لوحة المظهر على الرئيسية | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] لوحة المظهر على الرئيسية | 21 | 2 | vague: meta-setting, confusing twin | remote + agent |
| 93 | Show undo-look row | زر رجّع الشكل السابق | الإعدادات ← المظهر والخلفيات ← زر رجّع الشكل السابق | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] زر رجّع الشكل السابق | 22 | 2 | vague: meta-setting about a row | remote + agent |
| 94 | Smooth look switching | تبديل الشكل بلا وميض | الإعدادات ← المظهر والخلفيات ← تبديل الشكل بلا وميض | الإعدادات ← المظهر ← [أشكال جاهزة والتخطيط] تبديل الشكل بلا وميض | 23 | 2 | jargon: technical rendering detail | remote + agent |

### Control centre (direct) — «مركز التحكم»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 95 | Open control centre | مركز التحكم | الإعدادات ← مركز التحكم (زر مباشر) | الإعدادات ← الأزرار السريعة ← مركز التحكم | 5 | 1 | yes: named as on home | remote |

### Phone control (direct) — «التحكم من الجوال»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 96 | Phone remote on/off + pairing | الجوالات المسموح لها | الإعدادات ← التحكم من الجوال (زر مباشر) | الإعدادات ← العائلة والأمان ← الجوالات المسموح لها | 6 | 1 | vague: reads as a list, not switch | remote + phone (disconnect only) + agent |

### Update — «تحديث تلفزيوني»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 97 | Update both apps | تحديث تلفزيوني | الإعدادات ← تحديث تلفزيوني ← تحديث تلفزيوني | الإعدادات ← حول ← تحديث تلفزيوني | 7 | 2 | yes: clear | remote + phone |
| 98 | Auto-update screensaver page | التحديث التلقائي للصفحة | الإعدادات ← تحديث تلفزيوني ← [التطبيق] التحديث التلقائي للصفحة | الإعدادات ← حول ← التحديث التلقائي للصفحة | 8 | 2 | jargon: «الصفحة» is an internal concept | remote |
| 99 | Update screensaver page now | تحديث صفحة شاشة التوقف | الإعدادات ← تحديث تلفزيوني ← [التطبيق] تحديث صفحة شاشة التوقف | الإعدادات ← حول ← تحديث صفحة شاشة التوقف | 9 | 2 | jargon: «صفحة» internal concept | remote + agent |
| 100 | Beta channel | النسخ التجريبية | الإعدادات ← تحديث تلفزيوني ← [التطبيق] النسخ التجريبية | الإعدادات ← حول ← النسخ التجريبية | 10 | 2 | yes: clear | remote + agent |
| 101 | Beta feedback QR | أرسل ملاحظة عن النسخة التجريبية | الإعدادات ← تحديث تلفزيوني ← [التطبيق] أرسل ملاحظة عن النسخة التجريبية | الإعدادات ← حول ← أرسل ملاحظة عن النسخة التجريبية | 11 (row hidden by default: only on the beta channel) | 2 | yes: clear, beta only | remote |
| 102 | Clear caches | مسح الملفات المؤقتة | الإعدادات ← تحديث تلفزيوني ← [التطبيق] مسح الملفات المؤقتة | الإعدادات ← حول ← مسح الملفات المؤقتة | 11 | 2 | yes: standard term | remote |
| 103 | Data sources & licences | مصادر البيانات | الإعدادات ← تحديث تلفزيوني ← [التطبيق] مصادر البيانات | الإعدادات ← حول ← مصادر البيانات | 12 | 2 | yes: clear | remote |

### Send report (direct) — «أرسل تقرير»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 104 | Send report (QR) | أرسل تقرير | الإعدادات ← أرسل تقرير (زر مباشر) | الإعدادات ← حول ← أرسل تقرير | 7 | 1 | yes: clear | remote |

### More → Matches & markets — «المباريات والأسواق»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 105 | Open matches page | مباريات اليوم | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] مباريات اليوم | الإعدادات ← المباريات والأسواق ← [المباريات] مباريات اليوم | 9 | 3 | yes: clear | remote |
| 106 | Followed teams | الفرق المتابعة (n/40) | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] الفرق المتابعة | الإعدادات ← المباريات والأسواق ← [المباريات] الفرق المتابعة | 11+typing | 4 | yes: clear | remote + agent |
| 107 | App that plays matches | تطبيق المباريات | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] تطبيق المباريات | الإعدادات ← المباريات والأسواق ← [المباريات] تطبيق المباريات | 13 | 4 | vague: which app, for what | remote + agent |
| 108 | Follow whole leagues | الدوريات كاملة | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] الدوريات كاملة | الإعدادات ← المباريات والأسواق ← [المباريات] الدوريات كاملة | 14 | 4 | vague: «كاملة» unclear meaning | remote + agent |
| 109 | Matches card today only | بطاقة المباريات لليوم فقط | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] بطاقة المباريات لليوم فقط | الإعدادات ← المباريات والأسواق ← [المباريات] بطاقة المباريات لليوم فقط | 13 | 3 | yes: clear | remote + agent |
| 110 | Other sports events | رياضات أخرى | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] رياضات أخرى | الإعدادات ← المباريات والأسواق ← [المباريات] رياضات أخرى | 16 | 4 | yes: clear | remote + agent |
| 111 | Hide finished games (football layout) | إخفاء المباريات المنتهية | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] إخفاء المباريات المنتهية | الإعدادات ← المباريات والأسواق ← [المباريات] إخفاء المباريات المنتهية | 15 (row hidden by default: only with the football layout) | 3 | yes: clear, conditional | remote + agent |
| 112 | Kick-off alert | تنبيه بداية المباراة | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] تنبيه بداية المباراة | الإعدادات ← المباريات والأسواق ← [المباريات] تنبيه بداية المباراة | 15 | 3 | yes: clear | remote + agent |
| 113 | Alert whistle sound | صوت التنبيه | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] صوت التنبيه | الإعدادات ← المباريات والأسواق ← [المباريات] صوت التنبيه | 16 | 3 | yes: clear in context | remote + agent |
| 114 | Goal alert | عند الهدف | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] عند الهدف | الإعدادات ← المباريات والأسواق ← [المباريات] عند الهدف | 17 | 3 | vague: needs header context | remote + agent |
| 115 | Full-time alert | صافرة النهاية | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] صافرة النهاية | الإعدادات ← المباريات والأسواق ← [المباريات] صافرة النهاية | 18 | 3 | yes: clear in context | remote + agent |
| 116 | Alert over other apps | فوق تطبيق آخر | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] فوق تطبيق آخر | الإعدادات ← المباريات والأسواق ← [المباريات] فوق تطبيق آخر | 19 | 3 | vague: needs header context | remote + agent |
| 117 | Test alert | جرّب التنبيه | الإعدادات ← المزيد ← المباريات والأسواق ← [المباريات] جرّب التنبيه | الإعدادات ← المباريات والأسواق ← [المباريات] جرّب التنبيه | 20 | 3 | yes: clear | remote |
| 118 | Open markets page | افتح صفحة الأسواق | الإعدادات ← المزيد ← المباريات والأسواق ← [صفحة الأسواق] افتح صفحة الأسواق | الإعدادات ← المباريات والأسواق ← [صفحة الأسواق] افتح صفحة الأسواق | 21 | 3 | yes: clear | remote |
| 119 | Markets strip on screensaver | شريط الأسواق | الإعدادات ← المزيد ← المباريات والأسواق ← [الأسواق (أعلى اليسار)] شريط الأسواق | الإعدادات ← المباريات والأسواق ← [صفحة الأسواق] شريط الأسواق | 22 | 3 | yes: clear | remote + agent |
| 120 | Markets list (screensaver) | الأسواق (n/max) | الإعدادات ← المزيد ← المباريات والأسواق ← [الأسواق (أعلى اليسار)] الأسواق | الإعدادات ← المباريات والأسواق ← [صفحة الأسواق] الأسواق | 24+typing | 4 | vague: same word as the page | remote + agent |
| 121 | Markets display style | عرض الأسواق | الإعدادات ← المزيد ← المباريات والأسواق ← [الأسواق (أعلى اليسار)] عرض الأسواق | الإعدادات ← المباريات والأسواق ← [صفحة الأسواق] عرض الأسواق | 26 | 4 | vague: «عرض» = show or style? | remote + agent |
| 122 | Stocks list | قائمة الأسهم (n/max) | الإعدادات ← المزيد ← المباريات والأسواق ← [الأسهم] قائمة الأسهم | الإعدادات ← المباريات والأسواق ← [الأسهم] قائمة الأسهم | 26+typing | 4 | yes: clear | remote + agent |
| 123 | Stock change format | تغيّر الأسهم | الإعدادات ← المزيد ← المباريات والأسواق ← [الأسهم] تغيّر الأسهم | الإعدادات ← المباريات والأسواق ← [الأسهم] تغيّر الأسهم | 28 | 4 | vague: change of what, how | remote + agent |

### More → Apps & favourites — «التطبيقات والمفضلة»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 124 | Apps count in bar | تطبيقات في الشريط | الإعدادات ← المزيد ← التطبيقات والمفضلة ← تطبيقات في الشريط | الإعدادات ← التطبيقات ← [شريط التطبيقات] تطبيقات في الشريط | 12 | 4 | yes: clear | remote + phone + agent |
| 125 | Order favourites (list) | ترتيب التطبيقات المفضلة | الإعدادات ← المزيد ← التطبيقات والمفضلة ← ترتيب التطبيقات المفضلة | الإعدادات ← التطبيقات ← [شريط التطبيقات] ترتيب التطبيقات المفضلة | 11 | 3 | yes: clear; twin of «تعديل المفضلة» | remote + agent |
| 126 | Suggested apps to install | تطبيقات مقترحة | الإعدادات ← المزيد ← التطبيقات والمفضلة ← تطبيقات مقترحة | الإعدادات ← التطبيقات ← [شريط التطبيقات] تطبيقات مقترحة | 12 | 3 | yes: clear | remote |
| 127 | Rename HDMI inputs | أسماء المصادر | الإعدادات ← المزيد ← التطبيقات والمفضلة ← أسماء المصادر | الإعدادات ← التطبيقات ← [شريط التطبيقات] أسماء المصادر | 13 | 3 | jargon: «مصادر» = inputs, not obvious | remote |
| 128 | Edit favourites on home | تعديل المفضلة | الإعدادات ← المزيد ← التطبيقات والمفضلة ← تعديل المفضلة | الإعدادات ← التطبيقات ← [شريط التطبيقات] تعديل المفضلة | 14 | 3 | vague: overlaps «ترتيب…المفضلة» | remote + agent |
| 129 | Order/hide/unhide all apps | ترتيب كل التطبيقات | الإعدادات ← المزيد ← التطبيقات والمفضلة ← ترتيب كل التطبيقات | الإعدادات ← التطبيقات ← [شريط التطبيقات] ترتيب كل التطبيقات | 15 | 3 | vague: hides «unhide» inside | remote |
| 130 | Lock apps with PIN | قفل التطبيقات | الإعدادات ← المزيد ← التطبيقات والمفضلة ← قفل التطبيقات | الإعدادات ← التطبيقات ← [شريط التطبيقات] قفل التطبيقات | 16 | 3 | yes: clear; duplicated in parental | remote + agent |
| 131 | Android app info list | تفاصيل التطبيقات | الإعدادات ← المزيد ← التطبيقات والمفضلة ← تفاصيل التطبيقات | الإعدادات ← التطبيقات ← [شريط التطبيقات] تفاصيل التطبيقات | 17 | 3 | vague: details of what? | remote |

### More → New series — «جديد المسلسلات»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 132 | Series genres | التصنيفات | الإعدادات ← المزيد ← جديد المسلسلات ← [جديد المسلسلات] التصنيفات | الإعدادات ← المباريات والأسواق ← [جديد المسلسلات] التصنيفات | 12 | 4 | vague: needs header context | remote + agent |
| 133 | Series languages | اللغات | الإعدادات ← المزيد ← جديد المسلسلات ← [جديد المسلسلات] اللغات | الإعدادات ← المباريات والأسواق ← [جديد المسلسلات] اللغات | 13 | 4 | vague: clashes with «اللغة» | remote + agent |
| 134 | Picked series | مسلسلات مختارة | الإعدادات ← المزيد ← جديد المسلسلات ← [جديد المسلسلات] مسلسلات مختارة | الإعدادات ← المباريات والأسواق ← [جديد المسلسلات] مسلسلات مختارة | 12 | 3 | yes: clear | remote |
| 135 | Minimum rating | أقل تقييم | الإعدادات ← المزيد ← جديد المسلسلات ← [جديد المسلسلات] أقل تقييم | الإعدادات ← المباريات والأسواق ← [جديد المسلسلات] أقل تقييم | 15 | 4 | yes: clear in context | remote + agent |

### More → Parental (direct) — «الرقابة الأبوية»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 136 | Parental PIN, locks, kids mode | الرقابة الأبوية | الإعدادات ← المزيد ← الرقابة الأبوية (زر مباشر) | الإعدادات ← العائلة والأمان ← الرقابة الأبوية | 10 | 2 | yes: standard term | remote + phone (view + kids exit only) + agent |

### More → Site filter (direct) — «تصفية المواقع»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 137 | Family DNS filtering | تصفية المواقع | الإعدادات ← المزيد ← تصفية المواقع (زر مباشر) | الإعدادات ← العائلة والأمان ← تصفية المواقع | 10 | 2 | yes: clear | remote + agent |

### More → Language (direct) — «اللغة»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 138 | TV language (Arabic/English) | اللغة | الإعدادات ← المزيد ← اللغة (زر مباشر) | الإعدادات ← اللغة ← اللغة | 13 | 3 | yes: plain word, obvious meaning | remote + phone + agent |

### Search (direct) — «بحث في الإعدادات»

| # | Feature | TV label | Path (default style «أزرار») | Path in «القائمة الجانبية» style | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|---|
| 139 | Settings search | بحث في الإعدادات | الإعدادات ← بحث في الإعدادات (زر مباشر) | الإعدادات ← بحث في الإعدادات (first item) | 9+typing | 2 | yes: clear | remote |
## B. Features one level past a row (not rows of the settings list)

These are not in `SettingsHub`, so **«بحث في الإعدادات» cannot find any of them** (it only searches the rows above, their section names and the aliases).

### B1. Inside «الأذان» (opens the separate «تلاوة» app on its adhan tab; focus lands on «الأذان التلقائي»)

Presses = 2 (the «الأذان» button) + rows down + OK (+ the choice). Row order as drawn: الأذان التلقائي, [«قد يتأخر الأذان دقائق» only when exact alarms are off], الفجر, الظهر, العصر, المغرب, العشاء, المؤذن, أذان كل صلاة, الأدعية والتذكيرات, ساعات الهدوء, الإقامة, صوتياتي وأذاني الخاص, مؤذن الفجر, أذان مختصر, تكبير فقط, إيقاف الأذان بأي زر, الأذان على شاشة التوقف فقط, تنبيه عند الأذان, شاشة التوقف وقت الأذان, صوت الأذان.

| # | Feature | TV label | Path | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|
| 140 | Adhan on/off | الأذان التلقائي | الإعدادات ← الأذان ← الأذان التلقائي | 3 | 2 | yes: clear | remote + phone («الأذان في أوقات الصلاة», «الأذان يعمل») + agent |
| 141 | Hear one prayer's adhan (preview) | الفجر … العشاء | الإعدادات ← الأذان ← الظهر | 4–8 | 2 | vague: looks like a time, plays sound | remote |
| 142 | Muezzin (built-in 21 voices) | المؤذن | الإعدادات ← الأذان ← المؤذن ← (list) | 12 | 3 | yes: but no «add your own» | remote + phone («المؤذن (لغير الفجر)») + agent |
| 143 | Per-prayer sound / silent | أذان كل صلاة | الإعدادات ← الأذان ← أذان كل صلاة ← (prayer) ← (sound) | ~15 | 4 | vague: sound or time? unclear | remote + agent |
| 144 | Friday Dhuhr heard | أذان الجمعة | الإعدادات ← الأذان ← أذان كل صلاة ← أذان الجمعة | ~17 | 4 | yes: clear once found | remote + agent |
| 145 | Duas & reminders at set times | الأدعية والتذكيرات | الإعدادات ← الأذان ← الأدعية والتذكيرات ← إضافة دعاء أو تذكير ← (sound from «صوتياتي») ← (day/time/…) | 11 to open; ~23+ to add one | 5–6 | yes: but buried, unsearchable | remote (+ phone uploads the sound only) — **no agent command** |
| 146 | Ready Friday «سورة الكهف» | سورة الكهف يوم الجمعة | الإعدادات ← الأذان ← الأدعية والتذكيرات ← سورة الكهف يوم الجمعة | 13 | 3 | yes | remote |
| 147 | Quiet hours | ساعات الهدوء | الإعدادات ← الأذان ← ساعات الهدوء | 12 (+~6) | 3–4 | yes: clear | remote + agent |
| 148 | Iqama sound & notice | الإقامة | الإعدادات ← الأذان ← الإقامة | 13 (+~3) | 3 | yes | remote + agent |
| 149 | My sound files (USB / phone), storage, delete | صوتياتي وأذاني الخاص | الإعدادات ← الأذان ← صوتياتي وأذاني الخاص | 14 | 2 | yes: Gulf possessive, clear | remote (USB) + phone («صوتياتي») + agent (adhan file only) |
| 150 | **Own adhan from a file** | استخدمه لأذاني الخاص | الإعدادات ← الأذان ← صوتياتي وأذاني الخاص ← (file) ← استخدمه لأذاني الخاص | ~19 once the file is on the TV | 4 (+ USB browse) | vague: verb hidden in file page | remote + phone («أذاني الخاص») + agent (`music.sh adhan add`) |
| 151 | Own Fajr adhan | استخدمه لأذان الفجر الخاص | … ← (file) ← استخدمه لأذان الفجر الخاص | ~20 | 4 | yes | remote + phone («أذان الفجر») + agent |
| 152 | Delete a sound file | حذف | … ← صوتياتي وأذاني الخاص ← (file) ← حذف ← حذف | ~23 | 5 | yes | remote + phone |
| 153 | Larger audio storage | سعة أكبر | … ← صوتياتي وأذاني الخاص ← سعة أكبر | ~17+n files | 3 | vague: capacity of what? | remote |
| 154 | Fajr muezzin | مؤذن الفجر | الإعدادات ← الأذان ← مؤذن الفجر ← (list) | 18 | 3 | yes | remote + phone + agent |
| 155 | Short adhan | أذان مختصر | الإعدادات ← الأذان ← أذان مختصر | 16 | 2 | yes | remote + phone + agent |
| 156 | Takbir only | تكبير فقط | الإعدادات ← الأذان ← تكبير فقط | 17 | 2 | vague: no context of when | remote + agent |
| 157 | Any key stops adhan | إيقاف الأذان بأي زر | الإعدادات ← الأذان ← إيقاف الأذان بأي زر | 18 | 2 | yes | remote + phone + agent |
| 158 | Adhan only while screensaver | الأذان على شاشة التوقف فقط | الإعدادات ← الأذان ← الأذان على شاشة التوقف فقط | 19 | 2 | vague: reads as display | remote + agent |
| 159 | Notice at adhan | تنبيه عند الأذان | الإعدادات ← الأذان ← تنبيه عند الأذان | 20 | 2 | vague: twin of «تنبيه قبل الأذان» | remote + agent |
| 160 | Screensaver at adhan time | شاشة التوقف وقت الأذان | الإعدادات ← الأذان ← شاشة التوقف وقت الأذان | 21 | 2 | vague: no verb | remote + agent |
| 161 | Adhan volume (+ per prayer) | صوت الأذان | الإعدادات ← الأذان ← صوت الأذان (←/→ steps; OK opens per-prayer levels) | 22 | 2–3 | **misleading: «صوت» = voice or volume** | remote + phone («مستوى صوت الأذان») + agent |
| 162 | Favourite reciters | (long OK on a reciter) | الإعدادات ← تلاوة وأذان ← افتح تلاوة وأذان ← (reciter, long OK) | 5 + n | 3 | jargon: gesture with no label | remote + agent |

### B2. Inside «وضع الألوان» (opens a whole screen titled «المظهر» with seven picture tiles, three a line)

Opening it costs 20 presses (row 15 of «المظهر والخلفيات»); tiles: الشكل, اللون, الإضاءة / توحيد المظهر, البطاقات, الخلفية والصور / الحجم والكثافة.

| # | Feature | TV label | Path | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|
| 163 | Look (glass, graphite, navy…) | الشكل | الإعدادات ← المظهر والخلفيات ← وضع الألوان ← الشكل ← (tile) | ~23 | 4 | yes | remote + agent (`look`) |
| 164 | Home layout (again) | الواجهة | … ← وضع الألوان ← الشكل ← [الواجهة] (tile) | ~25 | 4 | vague: duplicate of «التخطيط» | remote + agent |
| 165 | Accent colour | اللون | … ← وضع الألوان ← اللون ← (tile) | ~24 | 4 | vague: docs call it «لون التمييز» | remote + agent (`accent`) |
| 166 | Dark / light / auto | الإضاءة | … ← وضع الألوان ← الإضاءة ← (tile) | ~25 | 4 | vague: «light» reads as brightness | remote + agent (`appearance`) |
| 167 | One material / per-surface transparency & fill | توحيد المظهر | … ← وضع الألوان ← توحيد المظهر ← (on/off, steppers) | ~24–30 | 4–5 | jargon: designer term | remote + agent (`looksync`) |
| 168 | Card style (again) | البطاقات | … ← وضع الألوان ← البطاقات ← (tile) | ~25 | 4 | yes: duplicate of «شكل البطاقة» | remote + agent |
| 169 | **Home photos mode** (mine / with ours / none) and screensaver photo choice | الخلفية والصور | … ← وضع الألوان ← الخلفية والصور ← [الصور في الشاشة الرئيسية] (tile) | ~27 | 4 | yes: but behind «وضع الألوان» | remote + agent (`homemine`) — only TV place for it |
| 170 | Wallpaper dimming | تعتيم الخلفية | … ← وضع الألوان ← الخلفية والصور ← تعتيم الخلفية (←/→) | ~29 | 4 | yes | remote + agent (`walldim`) |
| 171 | Bar transparency, focus shape (+ fav-bar count and glow again) | الحجم والكثافة | … ← وضع الألوان ← الحجم والكثافة ← شفافية الشريط / شكل التركيز | ~27–31 | 4 | jargon: «الكثافة» unclear | remote + agent (`baropacity`, `focusstyle`) |

### B3. Inside other rows

| # | Feature | TV label | Path | Presses | Depth | Label | Reachable by |
|---|---|---|---|---|---|---|---|
| 172 | **Delete one of my photos** | اختر صورة لحذفها | الإعدادات ← البطاقات ← شاشة التوقف ← [التوقيت] صوري ← اختر صورة لحذفها ← (photo) ← نعم | **~41** | **6** | vague: hidden in a mode list | remote + phone (per-photo «حذف») + agent (`photos.sh del N`) |
| 173 | Delete all my photos | حذف كل صوري | … ← صوري ← حذف كل صوري ← حذف | ~40 | 5 | yes | remote + agent (`photos.sh clear`) |
| 174 | Add my photos | — (no TV way) | phone page ← صوري ← اختر الصور | phone only | — | TV says «يضيفها مساعدك الذكي» | **phone + agent only** |
| 175 | Glass clock behind own photo (mask) | صورة مع قناعها | phone page ← صوري ← صورة مع قناعها | phone only | — | jargon: «قناع» | **phone only** (+ needs «الساعة: زجاجية» on TV) |
| 176 | Unhide a hidden app | المخفية | الإعدادات ← المزيد ← التطبيقات والمفضلة ← ترتيب كل التطبيقات ← تعديل التطبيقات ← المخفية ← (app) | ~19+ | 6 | vague: toast names «التطبيقات المخفية», no such row | remote |
| 177 | Set / change parental PIN | غيّر الرمز | الإعدادات ← المزيد ← الرقابة الأبوية ← غيّر الرمز ← (4 digits) | ~12 + 4 digits | 4 | yes | remote + agent (`dock.sh pin set`) |
| 178 | What the PIN locks (apps, settings, top buttons) | ما الذي يُقفل؟ ← التطبيقات المقفلة / قفل الإعدادات / الأزرار في الأعلى | الإعدادات ← المزيد ← الرقابة الأبوية ← … | ~13–18 | 4 | yes | remote + agent (`pin lock`) |
| 179 | Kids mode + apps shown to kids | وضع الأطفال / يظهر للأطفال | الإعدادات ← المزيد ← الرقابة الأبوية ← وضع الأطفال | ~14+ | 4 | yes | remote (+ phone: exit only) + agent |
| 180 | Phone remote on/off, pair a phone | الجوالات المسموح لها | الإعدادات ← التحكم من الجوال (direct) ← … | 6 + | 2–3 | vague: list vs switch | remote (+ phone: disconnect) |

### B4. Not in «الإعدادات» at all: control centre, home menus

The control centre opens from the home's «مركز التحكم» pill (or a short press of the remote's Settings key on TVs without their own quick panel). Its tiles: الشبكة, البلوتوث, مخرج الصوت, الأذان والإقامة, اختبار السرعة, مؤقت النوم, أطفئ الآن, شاشة سوداء, انعكاس الشاشة, شاشة التوقف, المدينة, الألعاب, تلفزيونك, (تحديث), **الإعدادات (= Android's settings)**, إدارة التطبيقات.

| # | Feature | Label | Where | Reachable by |
|---|---|---|---|---|
| 181 | Wi-Fi / network | الشبكة | control centre | remote |
| 182 | Bluetooth | البلوتوث | control centre | remote |
| 183 | Audio output | مخرج الصوت | control centre | remote |
| 184 | Next adhan / iqama + preview | الأذان والإقامة | control centre | remote |
| 185 | Speed test | اختبار السرعة | control centre | remote |
| 186 | Sleep timer | مؤقت النوم | control centre | remote + phone |
| 187 | Screen off now | أطفئ الآن | control centre | remote |
| 188 | Black screen (sound on) | شاشة سوداء | control centre | remote |
| 189 | Screen mirroring | انعكاس الشاشة | control centre | remote |
| 190 | Start screensaver now | شاشة التوقف | control centre (also «معاينة الشاشة») | remote + phone |
| 191 | Games | الألعاب | control centre, «تلفزيونك» hub | remote |
| 192 | Android settings | الإعدادات | control centre | remote |
| 193 | Android app management | إدارة التطبيقات | control centre (also «تفاصيل التطبيقات» row) | remote |
| 194 | Move / hide / lock / uninstall / store page of an app | (long OK on an app) | home | remote |
| 195 | Tasks & «ذكّرني» reminders | المهام | home pill / card | remote + phone + agent |

### B5. Phone page only, agent only

| # | Feature | Label | Where | Reachable by |
|---|---|---|---|---|
| 196 | Draw on the TV | ارسم | phone page ← على الشاشة | **phone only** |
| 197 | Message on the TV | اعرضها على التلفزيون | phone page ← على الشاشة | **phone only** |
| 198 | Type text into TV apps / search | اكتب على التلفزيون… | phone page (remote tab) | **phone only** |
| 199 | Launch an app from the phone | التطبيقات tab | phone page | **phone only** |
| 200 | Recitation player + its own sleep timer | القارئ والسورة / مؤقت النوم للتلاوة | phone page (now tab) | phone (+ remote inside «تلاوة») |
| 201 | Turn phone messages off (and over other apps) | رسائل الجوال | none on the TV; phone shows «رسائل الجوال موقوفة» | **agent only** (`dock.sh message`) |
| 202 | Arabic names for apps | أسماء التطبيقات (docs) | none on the TV (only inputs: «أسماء المصادر») | **agent only** (`dock.sh name`) |
| 203 | Copy settings to a second TV | نقل الإعدادات لتلفزيون ثاني (docs) | none on the TV | **agent only** (`saver.sh export/import`) |
| 204 | Turn our screensaver off / on | (ADB only) | none on the TV | **agent only** (`saver.sh off/on`) |
| 205 | Full PIN reset when forgotten | — | none on the TV | **agent only** (`dock.sh pin reset`) |

## C. The three owner reports: why each was hard to find

### (a) Using one's own adhan sound

- **Path (TV):** الإعدادات ← الأذان ← *(the «تلاوة» app opens, adhan tab)* ← صوتياتي وأذاني الخاص *(row 12)* ← من فلاشة USB *(browse to the file)* or the file already sent from the phone ← *(the file)* ← استخدمه لأذاني الخاص.
- **Presses:** ~19 once the file is on the TV (2 + 12 + 3 + 2), plus the USB browse or the whole phone round trip. Depth 4 below «الإعدادات», and it crosses into another app.
- **Why it is hard:**
  1. The places a person would look do not offer it. **«المؤذن»** lists the 21 built-in voices; the person's own recording shows there only after it exists. There is no «أضف أذانك» entry. **«صوت الأذان»** sounds like "the adhan's voice", but it is the **volume**. Both rows are decoys.
  2. The right row is 12th in a list that starts with the switch and the five prayer times, so it sits below the first screen. Its name, «صوتياتي وأذاني الخاص», names a file library. It does not read as "change the adhan".
  3. The step that does the job, «استخدمه لأذاني الخاص», is two screens further in, on the file's own page.
  4. None of this is in the settings app, so **«بحث في الإعدادات» finds nothing** for أذاني، أذان خاص، ملف، تسجيل. The alias «صوت» only reaches the «الأذان» button.
  5. The «الأذان» button's subtitle, «تشغيله وصوته وصوت الفجر والمختصر والإيقاف بأي زر والمستوى», does not mention your own recording.
  6. The «من الجوال» row only shows a toast: «افتح صفحة الجوال من «أسهل من الجوال»». A button called «أسهل من الجوال» exists only on the Markets page. The home's button is «الجوال».
  7. On the phone page, the «الأذان» card in the settings tab has the muezzin selects but no link to «صوتياتي». The link is on the remote tab, under «على الشاشة», as «صوتياتي — أدعية وأذان».
  8. One thing goes by four names: «صوتياتي وأذاني الخاص» and «استخدمه لأذاني الخاص» on the TV, «أذاني الخاص» on the phone, and «أذان من ملفي» / «أذاني للفجر» in docs/SETTINGS.md.

### (b) «الأدعية والتذكيرات» with their own audio

- **Path (TV):** الإعدادات ← الأذان ← *(«تلاوة», adhan tab)* ← الأدعية والتذكيرات *(row 9)* ← إضافة دعاء أو تذكير ← *(«صوتياتي» in pick mode: the file)* ← *(the dua's page: name, days, time in three steps, repeat, volume, on)*.
- **Presses:** 11 to open the list; ~23+ to add one with a file already on the TV. Depth 5–6.
- **Why it is hard:**
  1. It is **not in «الإعدادات»**. It is the 9th row of the adhan tab of the «تلاوة» app, under the five prayer times. No settings section name mentions it: classic «الأذان» or «تلاوة وأذان», couch «الصلاة والأذان», and their subtitles.
  2. **The settings search cannot find it.** No row or alias matches دعاء، أدعية، تذكير، تذكيرات.
  3. A different, unrelated feature has a similar name: the card «أذكار الصباح والمساء». Someone searching for "أذكار" lands on a card value, not this.
  4. It takes two places. The sound must first be in «صوتياتي», from USB on the TV or from the phone. Only then can the dua be made here. Choosing «دعاء أو تذكير» on the phone creates the dua **switched off**, and the phone says «اضبط وقته من التلفزيون: الأذان، ثم الأدعية والتذكيرات». So a phone user cannot finish there.
  5. There is no agent command for duas, so an assistant cannot set it up for them either.

### (c) Deleting a photo in «صوري»

- **Path (TV, default style):** الإعدادات ← البطاقات ← شاشة التوقف ← *[التوقيت]* صوري (n/max) *(row 30 of 31)* ← اختر صورة لحذفها *(4th of 5 choices)* ← *(the photo in a 5-wide grid)* ← نعم.
- **In «القائمة الجانبية» style:** الإعدادات ← المظهر ← [الصور] صوري ← …, about the 36th visible row of a 46-row section.
- **Presses:** ~41 (4 + 1 + 30 + 4 + ~1–3 + 1–2). Depth 6. This is the deepest and costliest common task on the TV.
- **Why it is hard:**
  1. Photos are under **«البطاقات»** (cards), then «شاشة التوقف», under the header **«التوقيت»** (timing). Yet the photos also show on the home.
  2. The row is 30th in one flat list of 31, which takes ~29 DOWN presses.
  3. The label «صوري (٣/١٠)» with the value «تُضاف إلى صور التطبيق» reads as a mode setting. OK opens a **single-choice radio list** of display modes, and delete is the 4th item in it. Nothing on the row says "add" or "delete".
  4. The row just above, «صور شاشة التوقف» (the photo packs), is the natural wrong turn. «وضع الألوان ← الخلفية والصور» also shows «صوري» tiles, but has no delete.
  5. With no photos, the row says «لا توجد صور، يضيفها مساعدك الذكي», and OK toasts «اطلب من مساعدك الذكي إضافة صورك». The TV never mentions the phone page. The phone's «صوري» page can add photos **and** has a «حذف» button under each one, which is the easy way.
  6. Search works only if the person thinks of it. The aliases «حذف الصور» and «حذف صوره» lead to «صوري». But search is the last button (8 presses to open) and needs the on-screen keyboard, and it opens the row, not the delete grid.

## D. Entry points to «الإعدادات» (from the home, before the counts above start)

| Way in | What happens | Approx. presses to «الإعدادات» |
|---|---|---|
| Pill «مركز التحكم» → tile **«تلفزيونك»** → hub window «الإعدادات» (4th of 4, bottom-left) | the route the TV teaches | ~2 to the pill + OK + ~5 + OK + 3 = **~12** |
| Pill «مركز التحكم» → tile **«الإعدادات»** | **opens Android's settings, not ours** (trap: same word) | — |
| Long press of the remote's Settings key | opens the «تلفزيونك» hub (the key again inside it opens our settings) | 1 + 3 |
| Short press of the Settings key | the TV's own quick panel where it has one, else our control centre | — |
| «الإعدادات» square at the end of the apps bar | only in apps style «أيقونات»; opens the hub | ~n + 3 |
| «UrTV النظيف» on | the control-centre pill is relabelled «الإعدادات» and opens the hub | 1 + 3 |
| The screensaver app in the app list | opens the «تلفزيونك» hub | n + 3 |
| Control centre «المدينة», the home's city prompt | opens our settings straight on the city picker | direct |
| Pill «تلاوة وأذان» | opens «تلاوة» on the reciters tab. The adhan tab is two ←/→ away (no «الإعدادات» on the way) | 1 + 2 |
| Prayer card / widget on the home | opens the adhan tab directly («إعدادات الأذان») | 1 |
| Long OK on an app | «نقل / إخفاء / قفل / إلغاء التثبيت / صفحته في المتجر» | 1 |

## E. Flagged lists

### E1. Features deeper than 3 levels below «الإعدادات» (depth counted as each button, row, dialog or inner screen chosen)

- **Every picker row inside «البطاقات» and «المزيد»** (two button screens, then the row, then its dialog = 4): all of «البطاقات ← شاشة التوقف» (e.g. «تصميم شاشة التوقف», «حجم الساعة», «الخانة اليسرى», «المحتوى», «شكل البطاقة», «تبديل الصورة كل», «بدء شاشة التوقف بعد», «الوضع الخفيف», «تنبيه قبل الأذان», «المناسبات الوطنية», the album rows); «البطاقات ← الشاشة الرئيسية» pickers («البطاقة الأولى/الثانية/الثالثة», «الخروج من تلفزيونك», the remote-key rows); «المزيد ← المباريات والأسواق» pickers («تطبيق المباريات», «الدوريات كاملة», «رياضات أخرى», «عرض الأسواق», «تغيّر الأسهم», «الأسواق», «قائمة الأسهم», «الفرق المتابعة»); «المزيد ← التطبيقات والمفضلة ← تطبيقات في الشريط»; «المزيد ← جديد المسلسلات» pickers.
- **Everything in «وضع الألوان»** (4–5): look, accent, light/dark, one material and transparency, card style, home photos mode, wallpaper dim, bar transparency, focus shape.
- **Inside «تلاوة»:** «أذان كل صلاة» → prayer → sound (4), «أذان الجمعة» (4), **own adhan** (4 + USB), own Fajr adhan (4), deleting a sound (5), **«الأدعية والتذكيرات»** add (5–6).
- **«صوري» → delete one (6)**, delete all (5).
- **Unhide an app** (6): المزيد ← التطبيقات والمفضلة ← ترتيب كل التطبيقات ← تعديل التطبيقات ← المخفية ← app.
- **Parental:** PIN change (4), locked apps / settings lock (4–5), kids mode and its apps (4–5).

### E2. Duplicate or overlapping places for the same thing

| Thing | Places |
|---|---|
| City | first button «المدينة» (direct); «الموقع والصلاة ← المدينة»; control centre «المدينة»; the home's city prompt; phone page «المدينة» |
| Adhan settings | «الأذان» button (opens «تلاوة»); «تلاوة وأذان ← افتح تلاوة وأذان» then ←/→ to the adhan tab; pill «تلاوة وأذان»; prayer card; control centre «الأذان والإقامة» (preview only); phone «الأذان» card |
| Prayer-related display | «الموقع والصلاة» (method, offsets) vs «البطاقات ← شاشة التوقف ← العرض» («تنبيه قبل الأذان», «الشروق بعد الفجر», «شريط الوقت بين الصلاتين») vs «تلاوة ← الأذان» («تنبيه عند الأذان», «شاشة التوقف وقت الأذان», «الإقامة») |
| Card style | «شاشة التوقف ← شكل البطاقة» and «وضع الألوان ← البطاقات» |
| Home layout | «التخطيط»; «وضع الألوان ← الشكل ← الواجهة»; «أشكال جاهزة»; «تخصيص»; «المظهر على الرئيسية» panel |
| Apps-bar count | «المزيد ← التطبيقات والمفضلة ← تطبيقات في الشريط» and «وضع الألوان ← الحجم والكثافة»; phone page |
| App glow | «البطاقات ← شاشة التوقف ← توهّج لون التطبيق» (a home-only setting filed under the screensaver) and «وضع الألوان ← الحجم والكثافة» |
| Photos | «صور شاشة التوقف» (packs) and «صوري» (own), side by side; «وضع الألوان ← الخلفية والصور» (screensaver and home photo modes); «المظهر والخلفيات ← الخلفية» (photos vs scenes); phone «صوري» |
| Favourites order | «ترتيب التطبيقات المفضلة» (list), «تعديل المفضلة» (on the home), long OK «نقل» |
| Hide / unhide apps | long OK «إخفاء»; «ترتيب كل التطبيقات ← تعديل التطبيقات ← المخفية»; the toast's non-existent «التطبيقات المخفية» |
| App locks | «المزيد ← التطبيقات والمفضلة ← قفل التطبيقات»; «الرقابة الأبوية ← التطبيقات المقفلة»; long OK «قفل» |
| Back opens screensaver | «الشاشة الرئيسية ← الرجوع يفتح شاشة التوقف» vs «شاشة التوقف ← رجوع مرتين يفتح شاشة التوقف» (different sub-screens) |
| Clock | «الساعة والأرقام» (12/24, seconds) vs «المظهر والخلفيات ← الساعة» (home style) vs «شاشة التوقف ← حجم الساعة» |
| Look panel | «المظهر على الرئيسية» (open it) and «لوحة المظهر على الرئيسية» (allow it) |
| Undo look | «رجّع الشكل السابق» and «زر رجّع الشكل السابق»; «رجّع بطاقاتك» |
| Updates | «تحديث تلفزيوني» (section + row); «تحديث صفحة شاشة التوقف»; «التحديث التلقائي للصفحة»; home bell; control centre update tile; phone «تحديث تلفزيونك» |
| Android app info | «تفاصيل التطبيقات» row; control centre «إدارة التطبيقات» |
| Settings search vs «تلاوة» | adhan, duas, own audio, iqama and quiet hours are outside the search's reach |

### E3. Labels that differ between the TV, the phone page and the docs

| TV | Phone page | Docs / toasts |
|---|---|---|
| «الأذان التلقائي» | «الأذان في أوقات الصلاة», «الأذان يعمل» | control centre «الأذان والإقامة» |
| «المؤذن» | «المؤذن (لغير الفجر)» | — |
| «صوت الأذان» (volume) | «مستوى صوت الأذان» | — |
| «صوتياتي وأذاني الخاص» / «استخدمه لأذاني الخاص» / «استخدمه لأذان الفجر الخاص» | link «صوتياتي — أدعية وأذان»; buttons «أذاني الخاص», «أذان الفجر», «دعاء أو تذكير», «بلا استخدام» | SETTINGS.md «أذان من ملفي», «أذاني للفجر» |
| «الأدعية والتذكيرات» | «دعاء أو تذكير» | — |
| «صوري» + «صور شاشة التوقف» | «صوري — خلفيات وصور التلفزيون» | TV empty state «يضيفها مساعدك الذكي» (ignores the phone) |
| «البطاقة الأولى/الثانية/الثالثة» | «بطاقات الشاشة الرئيسية» | — |
| «بدء شاشة التوقف بعد» | «تبدأ تلقائيًا بعد» | — |
| «تحديث تلفزيوني» | «تحديث تلفزيونك» | — |
| pill «الجوال», button «التحكم من الجوال», row «الجوالات المسموح لها» | «الجوالات المتصلة», «قطع الاتصال»; «امسح الرمز من «أسهل من الجوال»» | «أسهل من الجوال» exists only on the Markets page |
| «اللغة» | a button «English» | — |
| «وضع الألوان ← الإضاءة» | «الوضع الفاتح» (the phone page's own theme, not the TV's) | SETTINGS.md «المظهر ← الإضاءة» |
| «وضع الألوان ← اللون» | — | SETTINGS.md «لون التمييز» |
| «نظام الساعة» | — | SETTINGS.md «ساعة ٢٤» |
| «زر يوتيوب في الريموت» … | — | SETTINGS.md «أزرار الريموت» |
| «ترتيب كل التطبيقات ← تعديل التطبيقات ← المخفية» | — | toast «ويرجع من الإعدادات: «التطبيقات المخفية»» |
| «وضع الألوان ← الخلفية والصور» | — | SETTINGS.md «المظهر ← الخلفية والصور ← الشاشة الرئيسية ← صوري» (the inner screen is titled «المظهر», which clashes with the section «المظهر والخلفيات») |
| control centre «الإعدادات» = Android | — | hub «الإعدادات» = ours; «UrTV النظيف» pill «الإعدادات» = the hub |
| — | message error «رسائل الجوال موقوفة» | no TV row named «رسائل الجوال» |
| «شكل كنبة» | — | listed in SETTINGS-TREE.md but never drawn (PRESETS_REPLACE_COUCH = true) |

### E4. Only reachable from the phone or by an agent

- **Phone only:** adding own photos (the TV can only show or delete them), the clock mask behind a photo, drawing on the TV, sending a message to the screen, typing into TV apps, launching apps from the phone.
- **Phone or agent, never the TV alone:** adding photos (`photos.sh add`). Own audio can also come from USB on the TV.
- **Agent only:** «رسائل الجوال» on/off (and over other apps), Arabic app names, copying settings to another TV (export/import), switching our screensaver off/on, the full PIN reset, and tester-only memory tiers, debug and update channel.
- **TV only, with no agent command** (so an assistant cannot help): «الأدعية والتذكيرات», «صور شاشة التوقف» packs, «تطبيقات مقترحة», «UrTV النظيف», «مسلسلات مختارة», «مصادر البيانات», «التحديث التلقائي للصفحة», hiding or unhiding apps, the «إعداد تلفزيوني» actions.

### E5. Other structural findings

- **«البطاقات ← شاشة التوقف» is one flat list of 31 rows.** Anything at the bottom costs 30–38 presses: «بدء شاشة التوقف بعد» 38, «صوري» 37, «تبديل الصورة كل» 35. It also holds home-only rows («توهّج لون التطبيق», «نص أوضح») and a TV-wide one («الوضع الخفيف»).
- **«البطاقات ← الشاشة الرئيسية» mixes 30+ rows:** factory-home actions, the three cards, greeting, every pill switch, Back and Home-key behaviour, one row per remote key, the exit gesture, the bell, and «أكمل المشاهدة».
- **The section names do not match what is inside.** «البطاقات» holds the whole screensaver and home configuration, not cards. «المزيد» holds matches, markets, apps, parental control, site filtering and the language. The language is a TV-wide basic setting, but changing it costs 13 presses under «المزيد».
- **The «القائمة الجانبية» style («شكل شاشة الإعدادات» → القائمة الجانبية) is better organised** (language, place, look, apps, buttons, prayer…). It is off by default, its own toggle is buried in «المظهر والخلفيات» (row 2), and it puts «صوري» about 36th in a 46-row «المظهر» list.
- **Conditional rows appear and disappear.** Examples: «مكان الشريط», «العدّ التنازلي», «بطاقة ثانية», «الآية والحديث», «إخفاء المباريات المنتهية», «زر الرئيسية يفتح تلفزيونك مباشرة», «المظهر على الرئيسية», «رجّع الشكل السابق». Positions, and so press counts, shift between TVs, which makes over-the-phone guidance unreliable.
