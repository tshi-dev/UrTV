# The owner's standard, one pass over every screen (4.4 beta, November 2026)

> «ما تخلي شيء مكرر ولا متكرر ولا صعب. كلش بسيط. كلش مضبوط. كلش حلو.»

**What was checked:**
- The emulator's shots of 4.4 beta 4: auto round 25 and screens run 34. These cover:
  - the settings in the three styles, every section, at 720p and with large text;
  - the home in its three layouts × side card, and its top row, cards and couch panel;
  - the screensaver and the games.
- The phone page, drawn at phone size.
- The code behind each finding.
- The doctor app and the start presets, reviewed before this pass (see [SIMPLE.md](SIMPLE.md)).

**The marks:**
- **Fixed:** on work/44-simple, with its test.
- **4.4:** safe for 4.4 (words, a duplicate removed, spacing), not done yet.
- **Later:** a behaviour or layout change, or a decision for the owner.

Worst first. Each area keeps its own numbering.

## Fixed on work/44-simple

| # | Screen | What it was | Now |
|---|---|---|---|
| 1 | Settings, «أزرار» and tvOS | Row names cut («الفرق المتابع…»، «تحديث تلفزي…»، «خلّ UrTV ه…»): the name had a weight of 1, the value up to half the pane | The name is whole; the value wraps beside it in what is left |
| 2 | Settings, every screen and style | «اختيار يفتح، ورجوع يرجّعك خطوة» fixed at the top, on every screen, over the first row | Gone; the remote's keys say it |
| 3 | Settings, «أزرار» hub | Two titles: «تلفزيونك» over «الإعدادات» | One: «الإعدادات», or the section's name |
| 4 | Settings, «صوري والخلفيات» | No photos said twice, in two wordings («احذف صورة: ما فيه صور بعد» and «صوري: لا توجد صور، أضفها من الجوال») | Said once, by «صوري» |
| 5 | Games, the phones' room | The room's code twice side by side (Arabic digits in the saffron box, Latin in the middle) | Once, in Latin digits (the Arabic zero reads as a dot) |
| 6 | تلاوة وأذان, «الأذان» | The time to the next prayer twice: on the switch and on the prayer's own row | Once, on the prayer's row; the switch says مفعّل or متوقف |
| 7 | تلاوة وأذان, the surah grid | The main screen's hint, «يمين/يسار بين الأقسام…», on a screen with no sections | Its own: «اختيار يشغّل السورة، رجوع للقرّاء» |
| 8 | Phone page, login | «افتح هذه الصفحة بمسح الرمز…» shown to someone already on the page | «اكتب الرمز الظاهر على التلفزيون.» |
| 9 | Phone page, the header | A gear «الإعدادات» that opened the same tab as «الإعدادات» in the bottom bar | Gone |
| 10 | Phone page, «يعمل الآن» | The adhan's switch on two tabs, under two names («الأذان يعمل», «الأذان في أوقات الصلاة») | In «الإعدادات» only; «يعمل الآن» says the next adhan |

## Settings

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| S1 | repeat | Every value ends with where it applies («، كل التلفزيون»، «، الرئيسية وشاشة التوقف»), and «، شاشة التوقف» on every row of the شاشة التوقف page | Say it once in the section's head, and on a row only when it differs | Later: the owner asked for these words in 4.4 |
| S2 | ugly | The couch style's focused section item shows its dim line with the big name blank (family, saver and update, in auto round 25; run 34 was fine) | Find the cause on a TV: CouchStyle.ink or the shot's timing | 4.4, once found |
| S3 | imprecise | On and off said six ways: «يعمل»، «مفعّل»، «تعمل/متوقفة»، «مفعّلة/متوقفة»، «ظاهر/مخفي»، «مطفأ» | مفعّل / متوقف everywhere; OnOff reads both | 4.4 (many rows; their pins) |
| S4 | ugly | An on row has green marks, a green word and its icon at once; couch draws a ring and drops the word | The marks and the word, the same in every style | 4.4 |
| S5 | hard | Second lines over 8 words: «تعتيم الليل» (15), «المناسبة القادمة» (17), «الأذان» (11), «تنبيه قبل الأذان» (11), «النسخ التجريبية» (12, the same in both states), «الجوالات المسموح لها» (11); «تصفية المواقع» gives no state | Short lines with the state first | 4.4 |
| S6 | duplicate | «تخصيص» lists the rows right under it (التخطيط، الألوان والإضاءة، the cards) | Show it only after a change, or remove it | Later |
| S7 | duplicate | A row and its own on/off row: «رجّع الشكل السابق» / «زر رجّع الشكل السابق», «المظهر على الرئيسية» / «لوحة المظهر على الرئيسية» | Drop the two switches, keep the defaults | Later |
| S8 | duplicate | Three update rows; one only explains the overlap and says «صفحة»، «التطبيقين» | Fold «تحديث صفحة شاشة التوقف» into «تحديث تلفزيوني» | Later (the words: 4.4) |
| S9 | duplicate | Four rows on the home screen's role, «قبل ما تحذف…» a paragraph as a row | One row «الشاشة الرئيسية», the warning inside it | Later |
| S10 | duplicate | «أذاني من ملف» and «صوتياتي» | One row | Later |
| S11 | repeat | 89 rows with the eye icon, every prayer row a gear | The section's icon only | Later (the owner's look) |
| S12 | duplicate | Search: the page and its only row have the same name; the hub's line says «الصف»; couch's box has an outline | «اكتب كلمة»; no outline | 4.4 |
| S13 | repeat | Each section has a different line in each style | One short line per section for every style | 4.4 |
| S14 | hard | «المدينة: الرياض. لتغييرها: الإعدادات ثم اللغة والموقع» (a menu path) on the hub, on the lang page above the city row, and on the screensaver | A row that opens the city picker, nothing on the lang page or the screensaver | 4.4 for the lang page, later for the rest |
| S15 | repeat | Row names repeat their header: «… ألبوم صوري» ×5, «زر … في الريموت» ×19, «زر …» ×10 | «السرعة»، «الحجم»… under the header | Later (names are search keys) |
| S16 | duplicate | «تبديل الآية والحديث» beside «الآية والحديث»; four market rows | One row each, the choice inside | Later |
| S17 | imprecise | The app called «UrTV», «تلفزيونك» and «تلفزيوني» | تلفزيونك in the lines | 4.4 for the lines, later for the row names |
| S18 | imprecise | «تطبيقات في الشريط» says a bare «٥» | «٥ تطبيقات» (a new translated pattern) | 4.4 |
| S19 | imprecise | Couch's chevron depends on the value's length, not on whether the row opens a page | A chevron on every row that opens a page | 4.4 |
| S20 | hard | With large text the hub shows four tiles, and the saver tile's line is cut | Shorter tile lines («بعد ١٠ دقائق») | 4.4 |

## The home

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| H1 | imprecise | Football layout: «بتوقيت الرياض» drawn over «مباريات اليوم» | The card box starts under the zone line | 4.4 |
| H2 | imprecise | Football weather card: its last row (الرياح، الرطوبة، المحسوسة) cut at the edge | Drop that row when it does not fit | 4.4 |
| H3 | imprecise | Side cards: the first starts inside the top row; three squeezed to unreadable text (B); stocks touch the tray | Start under the bar, at most what fits | Later |
| H4 | duplicate | «اختر مدينتك للطقس والصلاة» and its button «اختر المدينة» say the same, while the screen already says «في الرياض» | One button «اختر مدينتك» and «لاحقاً»; ask only with no city | 4.4 for the words |
| H5 | duplicate | «الغروب ٥:٠٤ م» under the greeting, «المغرب ٥:٠٤ م» in the prayer card | No sun line while a prayer card shows | 4.4 |
| H6 | duplicate | Three ways to the settings in couch: the gear tile, «مركز التحكم ← الإعدادات», «مركز التحكم ← تلفزيونك» | The owner's call | Later |
| H7 | duplicate | Match rows: «اليوم ٥:٢٠ م» then «بعد ٥ ساعات» | The day and time; «بعد …» only within the hour | 4.4 |
| H8 | hard | «المهام» drawn with a microphone | A list icon | 4.4 if one exists |
| H9 | imprecise | The city strip moves 100–300 px with each step along the top row; the panel covers it | Pinned to the bar's widest state | Later |
| H10 | imprecise | The app's name above its banner repeats the logo, at the screen's edge, in line with nothing | At the tray's own edge | 4.4 for the place |
| H11 | ugly | Rings: the focused tasks card (white), football cards (inset), live cards (gold), «ما يشارك» (white outline) | Scale and shadow only | 4.4 (CSS) |
| H12 | ugly | The now-playing card is a capsule, 10 dp taller than the row | The row's radius and height | 4.4 for the radius |
| H13 | repeat | Time left written three ways: «٢ س و ٥٤ د»، «بعد ساعتين و ٤٦ دقيقة»، «بعد ١٢ دقيقة» | The bar's words everywhere they fit | 4.4, cell by cell at 720p |
| H14 | jargon | Football heading: «TheSportsDB» and «٩ مباريات» with three shown | The source in «مصادر البيانات»; «٣ من ٩» | 4.4 if its licence allows |
| H15 | imprecise | «ص»/«م» float under the digits and move with their width (the row's gravity set twice) | On the digits' baseline | 4.4 |
| H16 | duplicate | Top row buttons for المباريات، الأسواق، المهام، تلاوة وأذان while their card is on the same screen | Hide the button while its card shows | Later |
| H17 | imprecise | Card titles in four styles; three edges that don't line up (cards, tray, clock column) | One title style, one pair of edges | Later |

## The screensaver

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| V1 | duplicate | One match twice: in «مباريات فرقك» and in the bottom slot | The slot takes another item | Later |
| V2 | duplicate | The iqama countdown twice: the top bar and the bottom panel's «now» | Hide the bar while the panel says it | Later |
| V3 | hard | «المدينة: الرياض. لتغييرها: …» (S14), with the city already said three times | Drop it on the screensaver | 4.4 |
| V4 | jargon | «Open-Meteo، الآن»: the name is the licence's | Keep the name, drop «الآن» | 4.4 |

## تلاوة وأذان

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| T1 | hard | The adhan tab has about 20 rows; the voice is set in four places | The switch, the times, المؤذن, صوت الأذان, then «المزيد» | Later |
| T2 | jargon | «رجوع للواجهة»، «تعذر الاتصال»، «جارٍ إعادة المحاولة»، the slashes, a credit inside a key hint | «رجوع للرئيسية»، «ما قدرنا نتصل»… | 4.4 |
| T3 | hard | Row names over 3 words («الأذان على شاشة التوقف فقط»، «إيقاف الأذان بأي زر»); «فعّل المنبّهات الدقيقة» names a system setting | Shorter names, a button that opens it | 4.4 |

## The games

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| G1 | imprecise | The «الكل» view's header «بدون جوالات: ريموت واحد يدور بينكم» while its first game needs phones | «بالريموت، و«جوال» يحتاج جوالات», or no line | 4.4, with the games session (its test pins the line) |
| G2 | repeat | «ريموت» on 7 of 8 tiles | Mark only «جوال» | 4.4, with the games session |
| G3 | repeat | A key legend on every screen, Back as a label | Only a key that is not obvious | 4.4 |
| G4 | duplicate | Two different lobbies (الأسرع and the others) | One lobby | Later |
| G5 | repeat | The lobby's hint repeats the button («اختيار ابدأ» under «ابدأ») | No hint in the lobby | 4.4 |
| G6 | duplicate | The setup's corner tag «جوال/ريموت» repeats the chip row | Remove the tag | 4.4 |
| G7 | jargon | «لا توجد شبكة محلية» | «ما فيه شبكة…» | 4.4 |

## The phone page

| # | Kind | What | Fix | Mark |
|---|---|---|---|---|
| P1 | duplicate | Two sleep timers on «يعمل الآن» (the recitation's and the TV's) | Say which is which, or one card | Later (they are two features) |
| P2 | jargon | «تحديث تلفزيونك»: five lines on signatures and the Android installer, and «إصدار تلفزيونك: 4.4.0» | One line | 4.4 |
| P3 | hard | The remote tab: a paragraph on Android and accessibility, an arrow path, «أندرويد ١٣» | One sentence and numbered steps | 4.4 |
| P4 | repeat | «الجوالات المتصلة» twice; «الرقابة الأبوية: مفعّلة» under «الرقابة الأبوية»; «يعمل» for on | Once; مفعّل / متوقف | 4.4 |
| P5 | jargon | «إقران التلفزيون / رمز التصحيح اللاسلكي» in the settings | Hidden, or what it does for them | 4.4 |
| P6 | repeat | Play/pause twice on the remote tab | One | 4.4 |
| P7 | imprecise | Latin digits on the phone, Arabic-Indic on the TV | The owner's call | Later |

## The doctor app and the start presets

The doctor app has 15 changes, sent to its session; its visual changes wait for this review's OK. The start presets went from eight
choices to four («نظيف»، «كامل»، «كنبة»، «المزيد»). Both are in [SIMPLE.md](SIMPLE.md).
