# Settings and switching looks: a study and an audit (4.5, phase 1)

> **الخلاصة بالعربي:** الشركات الكبيرة تتّفق على ثلاثة أشياء: التركيز يتصفّح والزر «موافق» يعتمد. والتغيير يظهر فورًا خلف لوحة الإعدادات أو بعد
> إغلاقها مباشرة، بلا إعادة تحميل ولا شاشة فارغة. والرجوع يعيدك إلى الصف نفسه الذي خرجت منه. أما تطبيقنا فالرئيسية لا تعرف أن الشكل تغيّر إلا
> عند رجوعها أو عند دقيقتها التالية. وتغيير الشكل يهدم صفحة البطاقات ويبنيها من جديد، ويمرّ بين الصور والآفاق بلون فارغ. والإعدادات تغيّر شكلها هي
> نفسها مع «منظر وساعة» وتُفتح من جديد. واختيار الشكل له أربع طرق مختلفة، وثلاث منها بلا معاينة. ولا يوجد «رجّع الشكل السابق».
> الترتيب الكامل للمشاكل في القسم ٣.

Owner's ask: «the settings and choosing from style to style need to be seamlessly good. Smooth. And efficient.»
The scope is the home (`android/launcher`), the settings (`SettingsActivity`, `SettingsHub`, `CouchStyle` in `android/app`), and switching the look:
the presets «أشكال جاهزة», «شكلك المعتاد» ↔ «منظر وساعة», the background, «شكل التطبيقات», the clock, and the layout.

Part 1 is what others do. Part 2 is what our app does, measured on the CI emulator (`.github/workflows/ui-measure.yml`) and read in the code.
Part 3 ranks the problems.

---

## 1. What the platforms and the good launchers do

The sources were read in July–November 2026 versions. "(snippet)" means the vendor's page was blocked from this session, so the claim rests on the
search engine's quote of it. No code was copied. Couch TV (MIT) and tvOS Launcher (Apache-2.0) were read for ideas. FLauncher is GPL-3.0, so we
take ideas only, never code.

### 1.1 Apple tvOS
- **Settings** is a deep list (General, Video and Audio…). The screen saver is chosen in General → Screen Saver, by showing or hiding Aerial themes,
  and it is previewed with an explicit **Preview** item, not while a theme is focused (support.apple.com, snippet).
- **Focus browses, a press commits.** The HIG: "selecting a focused item opens or activates it", and "avoid changing focus without people's
  interaction". If the focused item goes away, focus moves one step in the direction of travel
  ([focus-and-selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection)).
- **Back opens the parent** of the current screen, and the focus returns to the item that opened it: `remembersLastFocusedIndexPath`, and a presented
  controller restores the focus it had ([remotes](https://developer.apple.com/design/human-interface-guidelines/remotes)).
- **Motion:** "brief", "let people cancel it", none on interactions people do often. The HIG gives no millisecond numbers
  ([motion](https://developer.apple.com/design/human-interface-guidelines/motion)).
- **Top Shelf:** a still picture that is not focusable, or banners that rotate on a timer until focused
  ([top-shelf](https://developer.apple.com/design/human-interface-guidelines/top-shelf)).
- **Undo:** for a resolution change, the TV tries the new mode and goes back by itself unless the person confirms (users' reports; Apple does not
  document the countdown).

### 1.2 Google TV / Android TV
- **Settings is a panel over the live screen.** The TV design guide's "right overlay" template is a panel on the right "independent to the content
  in the background". There is also a two-pane template for deep content
  ([layouts](https://developer.android.com/design/ui/tv/guides/styles/layouts)). Google TV's own Settings slide in over the home.
- **Focus:** the focused item scales by 1.025, 1.05 or 1.1
  ([focus-system](https://developer.android.com/design/ui/tv/guides/styles/focus-system)).
- **Focus memory:** `Modifier.focusRestorer(fallback)` stores the last focused child and restores it when focus re-enters the group
  ([focusRestorer](https://developer.android.com/reference/kotlin/androidx/compose/ui/focus/focusRestorer.modifier)).
- **Ambient mode** is a source picked in Settings → System → Ambient mode (Google Photos, Art gallery…), with no live preview on focus (support.google.com, snippet).
- **Profiles** give each person their own home (up to 12).
- **Settings search:** none. The global search is the way in.
- **Motion numbers (Material):**

  | Pattern | Default duration |
  |---|---|
  | shared axis | 300 ms |
  | fade-through | 300 ms |
  | container transform | 300 / 250 ms |
  | fade | 150 ms in, 75 ms out |

  The rule is "duration grows with the area that moves"
  ([Motion.md](https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md)).
- **No recreate for a change of look.** Opting out of recreation and updating in place gives "fluid animations"
  ([runtime-changes](https://developer.android.com/guide/topics/resources/runtime-changes)).
- **Snapshots:** a window can be copied to a bitmap with `PixelCopy` (API 26+) and faded out over the new UI
  ([PixelCopy](https://developer.android.com/reference/android/view/PixelCopy)).
- **WebView first frame:** set its background to the theme colour before `loadUrl`, or it paints white (Chrome WebView tips, snippet).

### 1.3 Samsung Tizen and LG webOS
- **Quick settings** is a strip over the live picture: picture mode, sound mode, sleep (Samsung), and the gear's Quick Menu, which can be
  customised (LG webOS 23) (vendor pages, snippet). The picture is visible behind it, so a change shows at once.
- I found no source on whether picture modes preview while focus moves. Seen on the TVs themselves, the mode applies when chosen in the strip,
  over the live picture. That is not documented, so it is not used as evidence here.
- **Samsung Art Mode** and **LG Always Ready** are the "ambient" looks. They have their own brightness and are chosen in the full settings.

### 1.4 Amazon Fire TV and Roku
- **Fire TV:** Settings → Display & Sounds → Ambient Experience, a deep path. Apps' HDMI mode changes revert by themselves when the app ends
  ([developer.amazon.com](https://developer.amazon.com/docs/fire-tv/4k-apis-for-hdmi-mode-switch.html)).
- **Roku:** a *theme pack* changes the wallpaper, screensaver and sounds together. That is a preset. Screensavers are previewed with an explicit
  "Preview" before saving (support.roku.com, snippet).

### 1.5 Open launchers (code read)

| Launcher (licence) | Where the settings live | Preview | How a change is applied | Focus and Back |
|---|---|---|---|---|
| **FLauncher** (GPL-3.0) | a 350 px panel on the right, over the live home | none; OK applies, and the home behind the panel changes at once | instant swap, no crossfade; the wallpaper is decoded at full size | Back pops the panel's own pages first. The gradient grid always focuses its first item, not the current one (a fault) |
| **tvOS Launcher** (Apache-2.0) | its own activity: sections on one side, a pane that follows the focus, rows scale 1.02 over 160 ms | the first-run "Choose a look" shows the background **on focus** (decode, then a 300 ms fade), focus starts on the current choice | a look or language change calls `recreate()` in `onResume` (the RTL flip only); the wallpaper swaps with no fade | a stack of parent rows: Back lands on the row you came from. `prewarmCache()` builds the blurred backdrop and thumbnails in the background, "so Settings opens in <16 ms" |
| **Couch TV** (MIT; our «منظر وساعة» settings follow it) | a list with sections, from the dock's gear | thumbnails in rows, no live preview | "what you change shows when the list closes"; scenes are paused while settings cover them; scenes crossfade over 1600 ms | focus is restored by the app's key (it survives a reorder), kept 20 min, and cleared by Home. "Lighter home" turns itself on for ≤1 GB, or when the median frame is over 22 ms |
| **Projectivy** (closed) | Appearance → Wallpaper / Cards | 4.72 added a preview of the wallpaper provider's results (snippet) | not known | not known |
| **Hume Launcher** | no repository, store page or docs found | | | |

### 1.6 What makes it feel smooth: the principles

1. **Focus browses, OK commits.** Never apply something heavy (layout, the apps row, language) because focus landed on it. (Apple; FLauncher; Couch TV.)
2. **Preview on focus only for what is cheap to show and free to undo:** a background, a clock style, a colour. Show it from a decoded,
   screen-sized (or smaller) bitmap, off the UI thread, with a fade of about 200–300 ms. (tvOS Launcher's first run; Android's render guidance.)
3. **Show the change where it happens.** A panel over the live home (Android TV's right overlay, FLauncher, Samsung and LG quick settings) beats a full
   screen that hides it. Otherwise, apply at the very moment the settings close (Couch TV).
4. **Never recreate or reload to change a look.** Change the views in place. When a rebuild cannot be avoided (a WebView page, a new row), cover it with
   a snapshot of the old screen that fades out once the new one has drawn. No white, no black, no empty frame. (Android runtime-changes; PixelCopy.)
5. **One motion vocabulary:** 200–300 ms for panels and swaps, and longer only for ambient backgrounds. Animate only alpha and translation, never the
   layout. (Material; Android render guidance.)
6. **When a picker opens, focus starts on the current value.** When it closes, focus goes back to the row that opened it. Remember the home's focus by
   the item's key, not its index. (Apple; Compose focusRestorer; tvOS Launcher; Couch TV.)
7. **Presets plus individual rows, with a visible state.** A preset sets several rows at once (Roku theme packs). Changing one row afterwards says so
   ("custom") without losing the preset.
8. **A way back without a dialog.** The classic TV undo is "Keep these settings? Reverting in 15 s" for risky display changes (Apple, Chromecast,
   Windows). For looks, a quiet "previous look" entry is enough. Nobody does toasts for this.
9. **Weak TVs:** pre-decode and cache what a switch needs before it is chosen, hold idle animations still while covered, and use one batched write.
   Budget 16 ms per frame. (Couch TV; tvOS Launcher; Android device-capacity guidance.)
10. **Settings search** is rare on TV platforms (none on Android TV, tvOS or Roku). We have one, which is a plus. It must stay instant and land on the
    row itself.

---

## 2. Our app: the audit

### 2.1 How a look change travels (read in the code)

| Step | What happens | Where |
|---|---|---|
| choose | `pickOne` is an AlertDialog: OK saves, dismisses, re-renders. No preview. Presets: a dialog with a still JPEG (decoded on the UI thread). «وضع الألوان»: another activity (`AppearanceActivity`) with drawn mock tiles. Cards and card style: a live **WebView** preview (`pickWithPreview`) | `SettingsActivity.java` :509–525, :814–908, :2690, :3472 |
| write | a preset is ~15–20 separate `SharedPreferences.apply()` calls (each field's setter), not one batch | `Prefs.java` :1373–1458 |
| tell the home | **nothing.** The settings send no broadcast. `Prefs.CHANGED` is sent only by the ADB `SET` path, and only to the screensaver's own package | `SaverReceiver.java` :580, :600 |
| the home reads | on `onResume`, and on its **minute tick**, through `ConfigSnapshot` (one answer shared for 3 s) | `MainActivity.java` :307, :3433; `ConfigSnapshot.java` :95 |
| the home applies | in place, no `recreate()` (good). But: the cards' **WebView is destroyed and made again** whenever its URL changes (layout, cards, look, light); the apps row is **rebuilt from scratch** (`buildApps`) for a new way; photos ↔ «الآفاق» passes through the look's plain colour, and the first scene comes in with no fade; the date lines jump (no animation) for the glass clock | `HomeCard.java` :206–214; `MainActivity.java` :3593–3604, :3458–3540, :3549; `SceneHome.java` :205 |
| the settings | choosing «منظر وساعة» (or leaving it) **restarts SettingsActivity** with no animation, because with «منظر وساعة» the settings take couch-tv's form (`CouchStyle`) and otherwise the classic hub | `SettingsActivity.java` :1061–1068 |
| cold start | `HomeSnapshot` (the stand-in picture) is keyed by the colour look, light, size and language, **not** by layout, background, clock or apps way: after a look change, the first frame of the next cold start can be the old look | `HomeSnapshot.java`; `tests/home_snapshot_test.py` |

### 2.2 Measured on the emulator

MEASURED

### 2.3 The steps for the person

| Change | Today | Remote presses |
|---|---|---|
| «شكلك المعتاد» → «منظر وساعة» | home → control centre → settings → «المظهر» → «أشكال جاهزة» → the dialog → the look → OK, then Back twice to see it | about 9–11 |
| the background | … «المظهر» → «الخلفية» → dialog → choice (no preview) → Back to see it | about 8–9 |
| the apps' way | … «المظهر» → «شكل التطبيقات» → dialog → choice (no preview) → Back to see it | about 8–9 |
| undo a look | choose «شكلك المعتاد» again (only puts back what was before the *first* preset; there is no "previous look") | 9–11 |

The person never sees the home while choosing: the settings are an opaque full screen (Couch TV does the same, and Google TV, FLauncher and the
TVs' quick settings don't).

### 2.4 What we already do well
- No `recreate()` for a look: the home changes its views in place.
- Focus in the settings is kept by key (`FocusKey`), per section (`lastRow`), and across a restart (`forceWant`/`returnWant`).
- Settings search exists (`SettingsHub.search`, 120 ms debounce), and the platforms don't have one.
- One motion curve and 200 ms everywhere (`Motion.java`), and «الوضع الخفيف» makes moves instant on weak TVs.
- The cold start has a stand-in picture (`HomeSnapshot`) and a first-frame plan for photos (`WallFirstFrame`).
- Presets with «مخصّص» when a row is changed afterwards, and the person's own values kept.

---

## 3. The problems, ranked

RANKED
