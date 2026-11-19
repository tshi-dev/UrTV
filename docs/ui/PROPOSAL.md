# Seamless settings and look switching: the proposal (4.5, phase 2, for the owner's OK)

Nothing is built yet. The audit behind it is [STUDY.md](STUDY.md). All pictures are 1920×1080, Arabic and RTL, with no pills and no «·».
Their HTML is in `mockups/src/`, rendered with `node mockups/src/render.js`.

| Picture | What it shows |
|---|---|
| [01-settings-model.png](mockups/01-settings-model.png) | **One settings model.** Look settings live on «المظهر», a panel over the live home: focus previews, OK keeps, Back reverts. Every other setting lives in the settings, in couch-tv's compact rows for every look: the form no longer switches with «منظر وساعة». Sensitive ones ask first. In every page, focus starts on the current value, and Back returns to the row you came from. |
| [02-before-look-settings.png](mockups/02-before-look-settings.png) → [02-after-look-panel.png](mockups/02-after-look-panel.png) | Today a choice is a dialog with no preview, over a full-screen page that hides the home. In the proposal, the home moves aside, whole and live (one scale of its root, no relayout). A row opens in place with picture choices, and the focused one («الآفاق») is already on the home. The clock stays where it was. |
| [03-after-presets-preview.png](mockups/03-after-presets-preview.png) | **The presets.** The six looks are a 3×2 grid. When focus rests on one for ⅓ s, the real home shows it, with one fade. Under the grid, the four rows the look sets. OK keeps it, and Back puts back «شكلك المعتاد». |
| [04-switch-before-after.png](mockups/04-switch-before-after.png) | **The switch.** The top strip is the real frames recorded today: the old look, a dark dip, then a build in steps, 1.3 s. The proposal: a snapshot of the current home covers it, everything changes and draws once behind it, then one 250 ms crossfade. Focus stays on the same app. |
| [05-after-undo.png](mockups/05-after-undo.png) | **«رجّع الشكل السابق»:** the first row of «المظهر», only after a change, naming the previous look and when it was chosen. Focus previews it like any look. No toast, no countdown. |
| [06-after-scenes-settings.png](mockups/06-after-scenes-settings.png) | **«منظر وساعة»'s own settings** («الآفاق») in the same panel. The horizon is chosen from pictures with a live preview, and the packs, stay time, time and weather rows are compact. The clock does not move between horizons. |
| [07-budgets.png](mockups/07-budgets.png) | **Performance budgets**, today's measurement next to each. |

## How it works (the build, phase 3)

1. **One write, one signal.**
   - A preset or a part is saved in one `SharedPreferences.Editor` (one `apply()`).
   - Then a broadcast, `sa.urtv.home.LOOK_CHANGED`, goes out under our signature permission. The home reads fresh on it (it skips the 3 s `ConfigSnapshot` copy), and an open settings screen re-renders on it.
   - The phone page and `saver.sh` use the same path, so a change shows in under a second, not at the next minute.
2. **Apply behind a cover.** `HomeLook.apply(cfg)` in the home is the one place a look is applied.
   - It draws the current window into a half-size bitmap (`PixelCopy`, or `View.draw` on API < 26) and lays it on top.
   - It changes everything in place: layout, background, clock, the apps cells' style, and the cards.
   - It waits for the first frame of the new state, and for the cards' page commit, at 600 ms at most.
   - Then it fades the cover out in 250 ms on `Motion.ease()`. With «الوضع الخفيف» it is a cut, still with no blank frame.
   - The bitmap is released right after.
3. **The cards' page is never torn down for a look.**
   - The colour look, the light and the glass material are sent to the open page as a message (`evaluateJavascript`), with no new URL.
   - When the URL must change (cards, layout), the new WebView is made off-screen behind the old one, and the swap happens on `onPageCommitVisible`.
   - Its background is the theme colour before `loadUrl`.
4. **The apps row is restyled in place.** A new way changes the cells' size, art and gap on the existing views when the app list is the same; the loader is not called. Focus is kept by the app's package, not by index.
5. **«المظهر» as a panel.**
   - It is a translucent activity in the home's own app, over the home, so the home stays drawn and receives the previews.
   - On open, the home's root scales to 0.61 and moves aside in 200 ms. Only the root's render transform changes, no layout.
   - **Preview:** after ⅓ s of focus rest, the home applies the focused value as a preview (step 2's cover and fade, 200 ms). The preview is not saved: OK saves it, Back restores the saved look.
   - Thumbnails are decoded off the UI thread once, at 480 px, and cached for the panel's life.
   - Heavy previews (layout, football) wait for the ⅓ s rest. Background, clock and colours are cheap.
6. **One settings form.** couch-tv's compact rows become the settings for every look. Choosing «منظر وساعة» no longer restarts the screen, and the classic hub's tiles go.
   - This is the one decision that removes something the owner may like, so it has its own line under "Asked of the owner".
7. **Undo.** `presetPrevious` keeps the look before the last change, all 12 fields, plus the time. «رجّع الشكل السابق» applies it like a preset, and the current look becomes the new previous one, so pressing it twice toggles.
8. **Cold start.** `HomeSnapshot`'s key gets the whole look (layout, background, clock, apps way). The stand-in is shown at full brightness, and the black frame before it goes: the window background becomes the snapshot.

## Tests (CI)

- `tests/look_switch_test.py`, source rules (every PR):
  - `HomeLook.apply` is the only way a look is applied;
  - no `HomeCard.destroy()` on a look or light change;
  - the preset is one `Editor`;
  - the broadcast is protected by the signature permission;
  - the panel's preview is never saved before OK;
  - «الوضع الخفيف» cuts.
- `.github/workflows/ui-measure.yml` becomes a **check** on `work/**` and `next`, with today's script and these limits on the emulator (2× the TV budget):

  | Measure | Limit on the emulator |
  |---|---|
  | settled after OK or Back | ≤ 600 ms |
  | blank frames | 0 |
  | distinct pictures | ≤ 1 crossfade |
  | focus | the same node (by resource id and text) before and after |
  | a phone change | shows in ≤ 2 s |
  | janky UI-thread frames for an apps way | ≤ 4 |

- `checks.yml` is dispatched on `work/45-ui`, because the APK build alone doesn't run the tests on work branches.

## Files (phase 3)

- **New:** `HomeLook.java`, `LookPanelActivity.java`, `LookPreview.java` (launcher); `tests/look_switch_test.py`.
- **Changed:**
  - `Prefs.setPreset` (one editor, previous look);
  - `SettingsActivity` / `SettingsHub` («المظهر» opens the panel; one form);
  - `CouchStyle`;
  - `MainActivity` (the apply path, the receiver);
  - `HomeCard` (no teardown for a look);
  - `FavBar` (restyle in place);
  - `HomeSnapshot` (the key);
  - `ConfigSnapshot` (fresh read on the signal).
- **Coordination:**
  - It starts from `rel-433` after `work/44-cards-build` and `work/44-sweep2` are merged.
  - The cards session builds the glass dock in `FavBar` and `MainActivity`'s dock code, so the two will agree on which methods each side changes before either edits them.

## Asked of the owner

1. **«المظهر» as a panel over the live home**, with the home moving aside (02, 03): yes or no?
2. **Preview on focus after ⅓ s**, for presets and the parts: yes, or preview only on OK?
3. **One settings form** (couch-tv's compact rows for every look; the classic hub's tiles go): yes or no?
4. **«رجّع الشكل السابق»** as the first row of «المظهر», only after a change: yes or no?
5. **The budgets** in 07 as CI checks: yes or no?

## Agreed with the cards session (for phase 3)

The cards session owns `FavBar`'s geometry (look, zoom, cellW, aspect, gap, radius, rowH, padSide, centredStart) and the coming glass way.
This work adds `FavBar.restyle(cells, way)` on top of it, plus `HomeLook.apply`. An in-place restyle must:

1. **Load each app's icon off the main thread before the swap** (`artCache "i:"+pkg`, `fitted(ri.loadIcon(pm), false)`). Today that happens only
   inside `loadApps`, and only for «أيقونات».
2. **Reset every per-tile value that `tile()` captures:**
   - the outline's and the ring's corner;
   - `AspectFrame.ratio` (1 for icons, 9/16 otherwise);
   - the image's padding and scale type;
   - the card's background colour.
3. **Swap the tray:**
   - its background (`dockBand` for tiles, none for banners or icons);
   - `dockRow`'s side padding (`FavBar.padSide`);
   - its height (`applyTrayHeight` → `FavBar.rowH`).
4. **Keep the rest as it is:**
   - «أكمل المشاهدة» stays on `dockCell(i, n, favs)`, the tiles' geometry, whatever the way;
   - focus is kept by package (`cardByPkg`).

## Agreed with the sweep session (for phase 3)

1. **Glass clock rule.** Since sweep 3 (6df8ffc2) the rule is `glassClock(cfg.homeClock, homeCard != null && homeCard.shownCards() > 0)`: the regular clock shows while any card shows (the owner's choice). `tests/sweep_test.py` and `tests/scene_home_test.py` pin it. The rule moves behind the cover unchanged.
2. **The hint.** CouchStyle's `HINT` (1.4375 rem) and the hint on `CouchStyle.SUB` stay as they are. If CouchStyle becomes the only form, the classic `HINT_SP` 14 goes, and the hint is never smaller than that.

## The owner's standing rule: no AI-look decoration (19 Nov)

Nowhere in UrTV, mockups included: no coloured edge or accent bars, no rounded colour stripes, no decorative badges or chips, no gradient used as
decoration, no letter-in-circle placeholder crests, no sparkle or emoji-like marks. Instead: plain 1 px dividers with a plain text label, colour on
text only where it carries data (a result, a live minute, a reason), the real logo or nothing, flat fills. Kept because they work, not decorate:
the readability scrims behind text on photos, a bar whose gradient is the data (cold to hot), the glass clock's own rendering.
