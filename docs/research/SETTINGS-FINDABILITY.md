# Settings findability review (4.4 beta 4, owner task)

The owner: «When something is not findable, make it a better findable way.» People on X could not find three features that exist: their own adhan sound,
«الأدعية والتذكيرات» with their own audio, and deleting a photo in «صوري». This review measures where every feature is, explains those three, and
proposes a structure. **Nothing here is code.** The mockups go to the owner first.

- Full audit (205 features, path, presses, label verdict, how reachable): [SETTINGS-AUDIT.md](SETTINGS-AUDIT.md)
- Sources with quotes: [SETTINGS-SOURCES.md](SETTINGS-SOURCES.md). Pages marked **(S)** could not be opened from the review's network (nngroup.com,
  m3.material.io, source.android.com, w3.org); their links were confirmed by search results only, and nothing here quotes them.
- Mockups (1080p): [../mockups/settings-findability/](../mockups/settings-findability/)

Press counts are for the default settings style «أزرار», starting on the focused first button, one press per D-pad move or OK (method in the audit).

## 1. What we found

| # | Problem | Today | Presses |
|---|---|---|---|
| 1 | Delete one of my photos | الإعدادات ← البطاقات ← شاشة التوقف ← صوري (row 30 of 31) ← اختر صورة لحذفها (4th choice of a mode list) ← الصورة ← نعم | ~41, depth 6 |
| 2 | Use my own adhan | الإعدادات ← الأذان (opens the «تلاوة» app) ← صوتياتي وأذاني الخاص (row 12) ← الملف ← استخدمه لأذاني الخاص | ~19 + getting the file on |
| 3 | Duas and reminders with my own audio | الإعدادات ← الأذان ← الأدعية والتذكيرات (row 9) ← إضافة… ← الصوت ← الوقت | ~23+, depth 5–6 |
| 4 | Reaching our settings | the control-centre tile named «الإعدادات» opens **Android's** settings; ours is مركز التحكم ← تلفزيونك ← الإعدادات | ~12 before any count above |
| 5 | «وضع الألوان» | opens a whole screen titled «المظهر» (accent, light/dark, transparency, focus, wallpaper dim, home photos mode); none of it is searchable | 20 to open |
| 6 | The screensaver list | «البطاقات ← شاشة التوقف» is one flat list of 31 rows, with home-only rows in it | up to 38 |
| 7 | Unhide an app | the hide message names «التطبيقات المخفية», a row that does not exist | ~19, depth 6 |
| 8 | Prayer settings | in four places: «الأذان» (another app), «الموقع والصلاة», «شاشة التوقف ← تنبيه قبل الأذان», «تلاوة ← تنبيه عند الأذان» | 2 to 20 |
| 9 | Names that hide their content | «البطاقات» holds the whole screensaver and home setup; «المزيد» holds matches, markets, apps, parental control and the language | — |
| 10 | One thing, five names | the phone link: «الجوال», «التحكم من الجوال», «الجوالات المسموح لها»; the help says «أسهل من الجوال», which exists only on the Markets page | — |

**Why the three owner cases fail** (details in the audit, section C):

- **Own adhan.** The two rows a person tries are decoys: «المؤذن» lists only the built-in voices, and «صوت الأذان» is the volume. The working row
  is named after a file library, it lives in another app, and the settings search cannot reach it.
- **Duas.** They are not in settings at all, and no section name or search word leads to them. A dua sent from the phone arrives switched off and can
  only be timed on the TV.
- **Delete a photo.** «صوري» is filed under «البطاقات» and the header «التوقيت», and delete is hidden inside a list of display modes. With no
  photos, the TV says only the assistant can add them; the phone page, which adds and deletes in one place, is never mentioned.

## 2. Rules the fix follows (each with its source)

1. **Shallow and short.** Every change in at most two levels (section → row → its picker); a third level only for a list of like items (the prayers,
   the photos). "Organize your information in a way that requires the fewest screens" and "Don't over-complicate by adding unnecessary layers of
   navigation." ([Android TV, Navigation on TV](https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv); see also
   [NN/g, Flat vs. Deep Hierarchies](https://www.nngroup.com/articles/flat-vs-deep-hierarchy/) (S))
2. **Few sections, most used first.** "Destinations are ordered according to user importance, with frequent destinations first and related destinations
   grouped together", and "limit the number of main navigation destinations … to five to six."
   ([Android TV, Navigation drawer](https://developer.android.com/design/ui/tv/guides/components/navigation-drawer)) We go from 13 buttons to 9 sections.
3. **Two panes.** "A 2-pane layout performs better when the page shows hierarchical content." ([Android TV, Layouts](https://developer.android.com/design/ui/tv/guides/styles/layouts))
   Our «القائمة الجانبية» style already is this.
4. **Names that say what is inside**, from one glossary: no «المزيد», no «البطاقات» for a whole screensaver. "Create a list of common terms, and
   reference that list to keep your language consistent" and "avoid the temptation to be too cute or clever with your labels."
   ([Apple HIG, Writing](https://developer.apple.com/design/human-interface-guidelines/writing);
   [NN/g, Confusing Category Names](https://www.nngroup.com/articles/category-names-suck/) (S))
5. **One name per thing, everywhere** (TV, phone page, help, docs, search).
   ([WCAG 2.2, Consistent Identification](https://www.w3.org/WAI/WCAG22/Understanding/consistent-identification) (S))
6. **One focus target per row, its value shown.** "Avoid placing buttons inside a list item as it can cause confusion about which element is currently
   in focus." ([Android TV, Lists](https://developer.android.com/design/ui/tv/guides/components/lists))
7. **Options where the task happens, and in two places when people look in two.** "Make these options available in the screens they affect, where
   they're discoverable and convenient." ([Apple HIG, Settings](https://developer.apple.com/design/human-interface-guidelines/settings);
   [NN/g, Polyhierarchies](https://www.nngroup.com/articles/polyhierarchy/) (S)) Hence own adhan in «المؤذن», and duas in settings and in «تلاوة».
8. **Fewer settings, good defaults.** "Too many settings can make the experience feel less approachable, while also making it hard to find a particular
   setting." ([Apple HIG, Settings](https://developer.apple.com/design/human-interface-guidelines/settings))
9. **One search for everything, with suggestions, and results that show where they live.** "Aim to make your app's content searchable through a single
   location" and "Provide suggestions to make searching easier." ([Apple HIG, Searching](https://developer.apple.com/design/human-interface-guidelines/searching))
10. **«جديد» rarely.** "Use badges sparingly and only when necessary" ([Android TV, Navigation drawer](https://developer.android.com/design/ui/tv/guides/components/navigation-drawer));
    "Reserve badges for critical information" ([Apple HIG, Tab bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars)).
11. **Focus is predictable; Back returns to the parent with the focus on where you came from.** "Avoid changing focus without people's interaction."
    ([Apple HIG, Focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection);
    [Android TV, TV navigation](https://developer.android.com/training/tv/get-started/navigation))
12. **Test the names before building.** A tree test of the new tree with five tasks (the three owner cases, unhide an app, the language) shows whether
    people find them. ([NN/g, Tree Testing](https://www.nngroup.com/articles/tree-testing/) (S);
    [NN/g, Card Sorting](https://www.nngroup.com/articles/card-sorting-definition/) (S))

## 3. The proposed structure

Search stays first. Then nine sections, the most used first:

| Section | What is in it (main rows, in order) | Replaces |
|---|---|---|
| **الصلاة والأذان** | الأذان · المؤذن (with «أذاني») · الأذان لكل صلاة · صوت الأذان · تنبيه قبل الأذان · الإقامة · الأدعية والتذكيرات · *المواقيت:* طريقة الحساب (with «جعفري») · تعديل وقت الصلاة · الشروق بعد الفجر | «الأذان», the prayer half of «الموقع والصلاة», the adhan rows of «شاشة التوقف» and of «تلاوة» |
| **صوري والخلفيات** | *صوري:* أضف من الجوال · أضف من فلاشة أو التنزيلات · احذف صورة · أين تظهر صوري · *الخلفيات:* خلفية الشاشة الرئيسية · صور شاشة التوقف · تبديل الصورة كل | «صوري» (row 30 of the screensaver list), «صور شاشة التوقف», «وضع الألوان ← الخلفية والصور» |
| **الشكل** | أشكال جاهزة · التخطيط · الألوان (today's «وضع الألوان», as rows) · شكل البطاقات (size, corners) · الساعة · البطاقات الثلاث · التحية والطقس | «المظهر والخلفيات», «وضع الألوان», «الساعة والأرقام» (clock style) |
| **شاشة التوقف** | بدء شاشة التوقف بعد · المحتوى · حجم الساعة · رجوع مرتين يفتح شاشة التوقف · الرجوع يفتح شاشة التوقف | the 31-row list, less what moved out |
| **التطبيقات والأزرار** | ترتيب المفضلة · التطبيقات المخفية (a real row) · قفل التطبيقات · الأزرار السريعة · أزرار الريموت | «التطبيقات والمفضلة», «الأزرار السريعة», «مركز التحكم» |
| **المباريات والأسواق** | as today | the «المزيد» part |
| **العائلة والجوال** | الجوال (one name for the phone link) · الرقابة الأبوية · تصفية المواقع | «التحكم من الجوال», «العائلة والأمان» |
| **اللغة والموقع** | اللغة · المدينة · الأرقام والتقويم · نظام الساعة | «المزيد ← اللغة», «الموقع والطقس», «الساعة والأرقام» |
| **حول والتحديث** | تحديث تلفزيوني · أرسل تقرير · الإصدار | «تحديث تلفزيوني», «أرسل تقرير», «حول» |

**Depth limit:** section → row → picker. Two lists may go one deeper: the five prayers (in «الأذان لكل صلاة») and the photos (in «احذف صورة»).
No section over about 12 rows; a longer one gets group headers, never a flat list of 31.

**Search:** index the «تلاوة» rows and the «وضع الألوان» screen too, add people's words (أذاني، أذان خاص، دعاء، تذكير، فلاشة، USB، يو اس بي،
التنزيلات، حذف صورة، أضف صور), and show each result's path under it. Search opens the row in its place, with Back returning to search.

**«جديد»:** only on a row that is new in this version, gone once that row is opened, at most three on one screen.

**Entry:** the control-centre tile that opens Android's settings is renamed «إعدادات الجهاز», so «الإعدادات» always means ours.

**Default style:** the side menu («القائمة الجانبية») is the base of this structure, but it is off by default today. Changing a default on installed TVs
breaks our "stable first" rule, so the proposal is: new setups start in it, and installed TVs are offered it once («جرّب الإعدادات الجديدة؟»), never
switched without asking. Owner's decision.

## 4. Where the new rows go (answers given to the release session)

- **الأذان لكل صلاة:** in «الصلاة والأذان», right after «الأذان», before «تنبيه قبل الأذان». The value is a summary («كلها صوت وشاشة» or
  «الفجر: شاشة فقط»). Inside are the five prayers, one row each, one focus target, value «صوت وشاشة» / «صوت فقط» / «شاشة فقط» / «لا شيء»;
  OK opens a four-choice picker. Not two toggles in one row (rule 6). Dimmed with «شغّل الأذان أولًا» while the adhan is off.
- **Files from a USB stick or Downloads:** no new top-level entry. One wording everywhere: «أضف من فلاشة أو التنزيلات», inside each target:
  «صوري» (its own action row), «المؤذن ← أذاني», and «صوتياتي». The phone audio page's tile is «أذاني», the same name as the TV.
- **Own adhan:** a last choice «أذاني» in the existing «المؤذن» picker (sources: «أضف من فلاشة أو التنزيلات» / «أضف من الجوال»), the same store
  as the «تلاوة» app. «صوت الأذان» stays the volume.
- **الأدعية والتذكيرات:** a row in «الصلاة والأذان» opening the same page as «تلاوة»; its value says «١ جديد من الجوال» until a dua sent from the
  phone is timed.
- **Jafari:** «جعفري (شيعة)» in the prayer method list, in the existing order, no badge.

## 5. Changes ranked by user impact

1. **«صوري» with add and delete first**, under «صوري والخلفيات», and its empty state pointing to the phone page. Delete one photo: ~41 → 6 presses.
2. **Own adhan inside «المؤذن»** in settings and in «تلاوة», with the two sources. ~19 → 4 presses once the file is on the TV.
3. **«الأدعية والتذكيرات» in settings and search**, and phone-sent duas shown as waiting. Today: not findable from settings at all.
4. **Search reaches everything** («تلاوة», «وضع الألوان») with people's words and the path shown. Fixes the long tail without moving anything.
5. **«إعدادات الجهاز»** for the tile that opens Android's settings. A one-word change that ends the most common wrong turn.
6. **All prayer settings in «الصلاة والأذان»**, including «تنبيه قبل/عند الأذان» side by side.
7. **«التطبيقات المخفية» as a real row**, matching the hide message.
8. **One name per thing**: «الجوال» for the phone link, «أذاني» for the own adhan, and the docs and help brought in line (audit E3).
9. **The 31-row screensaver list split**, home-only rows moved to «الشكل».
10. **«وضع الألوان» as rows in «الشكل»**, named «الألوان».
11. **«جديد» discipline**: new rows only, gone once opened, three at most.
12. **The side menu for new setups**, offered once on installed TVs (owner's call).

Items 1–5 need no restructure and can ship first. 6–12 are the new tree; run the tree test (rule 12) on it before building.

## 6. Mockups (1080p)

| File | Shows |
|---|---|
| [settings-before-1-hub.png](../mockups/settings-findability/settings-before-1-hub.png) | **Before:** today's first page (13 buttons) |
| [settings-after-1-prayer.png](../mockups/settings-findability/settings-after-1-prayer.png) | **After:** the nine sections; «الصلاة والأذان» with every prayer row |
| [settings-after-2-muezzin-own.png](../mockups/settings-findability/settings-after-2-muezzin-own.png) | **After:** «المؤذن» with «أذاني» and its two sources |
| [settings-after-3-photos.png](../mockups/settings-findability/settings-after-3-photos.png) | **After:** «صوري والخلفيات», add and delete first |
| [settings-after-4-search.png](../mockups/settings-findability/settings-after-4-search.png) | **After:** search results with their path |
| [card-shape-settings-1080.png](../mockups/settings-findability/card-shape-settings-1080.png) | Card shape: size step and corners, with a live preview |
| [card-shape-sizes-corners-1080.png](../mockups/settings-findability/card-shape-sizes-corners-1080.png) | Card shape: the 3 sizes × 3 corners side by side |

The pictures use a stand-in font (IBM Plex Sans Arabic); ours is not in the repo. Muezzin names and values are examples. The card corners go up to 26
px at 1080p, so a card is never a pill; in «كنبة» the strip's height is fixed, so «أكبر» equals «عادي» there and the row says so.
