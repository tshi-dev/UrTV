# UrTV 4.0 first settings screen: prototype map

Files: `proto.html` (one page, `#main|#cards|#more|#adhan`), rendered by `render.js` to `main.png`, `cards.png`, `more.png`, `adhan.png`. Mockup only.

## Proposed order (first screen, two columns read right to left)
الأذان (one card: title, switch, status; OK flips it, and its options open from inside, adhan.png) · الموقع والصلاة · تلاوة وأذان · البطاقات · المظهر · مركز التحكم · التحكم من الجوال · تحديث تلفزيوني · أرسل تقرير · المزيد (حماية العائلة is inside «المزيد»).
Daily prayer things first, then looks, then the TV and phone, then maintenance, then the rest. Cards show only the current value; no pills, eyebrows, chevrons or hints.

## Every current row (SettingsHub.ROWS, all of them, + the per-prayer minutes) → new place
| Current row | New place |
|---|---|
| الأذان | main button «الأذان» (switch) + «خيارات الأذان» |
| تنبيه قبل الأذان | inside «الأذان» |
| الدولة، المدينة، طريقة الحساب، تعديل وقت الصلاة (+ the 5 per-prayer minutes) | main «الموقع والصلاة» |
| زر تلاوة وأذان | main «تلاوة وأذان» (its hide/show switch moves there) |
| المظهر | main «المظهر» |
| تحكم من الجوال | main «التحكم من الجوال» |
| تحديث تلفزيوني، تحديث الشاشة الآن، التحديث التلقائي للصفحة | main «تحديث تلفزيوني» (version, update, screen refresh together) |
| حماية العائلة (family DNS) | «المزيد» › حماية العائلة |
| (not a row today) | main «مركز التحكم»: opens the launcher's Control Center |
| (not a row today) | main «أرسل تقرير»: opens a QR to the docs/share page with the readings, as the safe-mode branch's `SafeReport` does; second line empty, or «آخر تقرير: أمس» once one was sent |
| معاينة الشاشة، المحتوى، بطاقة ثانية، شكل البطاقة، الخانة اليسرى، العدّ التنازلي، حجم الساعة، شريط السنة تحت الساعة، الشروق بعد الفجر، شريط الوقت بين الصلاتين، الرطوبة والرياح، المناسبات الوطنية، تبديل الصورة كل، صوري، بدء شاشة التوقف بعد | «البطاقات» › شاشة التوقف |
| البطاقة الأولى، البطاقة الثانية، البطاقة الثالثة، التحية والطقس، أكمل المشاهدة، جرس التحديثات | «البطاقات» › الشاشة الرئيسية |
| الفرق المتابعة، الدوريات كاملة، رياضات أخرى، شريط الأسواق، الأسواق، عرض الأسواق، قائمة الأسهم | «المزيد» › المباريات والأسواق |
| التصنيفات، اللغات، مسلسلات مختارة، أقل تقييم | «المزيد» › جديد المسلسلات |
| ترتيب التطبيقات المفضلة، تعديل المفضلة، ترتيب كل التطبيقات، أسماء المصادر | «المزيد» › التطبيقات والمفضلة |
| نظام الساعة، الأرقام، أسماء الأشهر الميلادية | «المزيد» › الساعة والأرقام |

My choices to confirm with the owner: the content rows (teams, markets, shows, stocks) sit in «المزيد», not in «البطاقات», so «البطاقات» keeps exactly his two cards . «تحديث الشاشة الآن» and the page auto-refresh join «تحديث تلفزيوني» instead of being lost under a phone category.

## Extra main button suggested: «الساعة والأرقام» (12/24-hour clock, digits, month names, Hijri offset)
Evidence (docs/PRS-ISSUES-4.0.md, 4.0 record; REPORTS-ANALYSIS.md has nothing on settings use, it is about setup failures):
- 6 of the 45 open PRs and issues are about how time and numbers are written: #41 (24-hour clocks, taken), #13 and #34 and #23 (Latin digits), #45 (English time), #15 (Gregorian-only date); the owner's recent 12/24-hour request makes it the most repeated settings ask.
- The setting exists but is buried today: «نظام الساعة» is one of one of 15 rows in «شاشة التوقف», the same «not found» problem the adhan had.
Second, smaller idea (not a button): picture mode and the Settings key outside our home (#37 plus the recent report). Our home cannot catch the key inside other apps; put a «وضع الصورة» tile in «مركز التحكم» that opens the TV's picture settings, and screen mirroring already has its tile there when the TV has the app. If the owner wants a tenth main button instead, «الصورة والصوت» is the one.
If «الساعة والأرقام» becomes a main button, «المزيد» keeps three groups.

## What changes in the code later (one paragraph)
`SettingsHub.java` gets new CATEGORIES (the 9 buttons + «المزيد» + «البطاقات» with two sub-groups; `ROWS` and `HEADERS` re-pointed, same «a missing row follows its header» rule, `tests/settings_hub_test.py` and `SettingsHubRun.java` updated to the new keys). `SettingsActivity.java`: `hub()` draws one wide switch button plus a 3-column grid (`SettingsHub.next` moves from 2 to 3 columns, hero row first), `render()` filters by the new keys, and the «الأذان» button reads and writes the adhan switch directly (today it opens `openAdhanSettings()` through the home app, so the screensaver app needs the home's adhan state, e.g. via the same bridge as `openAdhanSettings`, and the button must show it even while the home is locked by the family PIN). The launcher side: «مركز التحكم» and «تلاوة وأذان» become entries from the first screen (`ControlCenter.openOurSettings` already goes the other way), and the Settings-key route `ParentalPolicy.OUR_SETTINGS` still lands on the first screen. `docs/SETTINGS.md` and `docs/FEATURES.md` change the sentence about eight category tiles and the list of which row is where. No command-line options change.

## v2 (answers to the owner's two questions): `v2-main.png`, `v2-adhan.png`, `v2-clock.png`, `v2-more.png`
- **«الأذان» opens its options:** a wide card on top; OK opens the adhan page with its switch as the first, focused row (turn it on = OK, OK). Why: one button does one thing on a D-pad; a long press or a second focus stop inside the card is hidden from remote users. The card's switch only shows the state.
- **«الساعة والأرقام»:** yes, a main button next to «الموقع والصلاة» (both about time), holding نظام الساعة (١٢/٢٤)، الأرقام (عربية/لاتينية)، أسماء الأشهر الميلادية، تعديل التاريخ الهجري. Why: the most repeated ask (6 PRs/issues) and buried today. «المزيد» keeps four groups.
- Grid: 2 columns kept; cards 124 px tall at a 140 px step so the wide adhan card plus 5 rows fit 1080.
- Sub-lines use «،» or words, not «·» between two numbers: «·» next to Arabic digits reads as a zero («٣٠»).

## v3: three replacements for the iOS switch (`v3-toggle-A/B/C.png`, open `proto.html#adhan:A:1` etc.)
Each PNG is two adhan screens stacked: top with «الأذان» focused and on, bottom with it focused and off; the other rows show unfocused on and off.
- **A, check square:** a 50 px square, empty when off, filled green with a white check when on.
- **B, the word:** «مفعّل» in white with a thick green underline, «متوقف» in grey with none.
- **C, two-part bar:** «مفعّل | متوقف» side by side, the current half filled (green for on, white or dark for off).
