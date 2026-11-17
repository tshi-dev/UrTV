# 4.4 consistency sweep

The app on the CI emulator at 1080p (`.github/workflows/screens.yml`), before (`work/44-sweep-before`, the 4.3.2 candidate's
screens with the build-413 settings fixes) and after (`work/44-settings-couch`). The full 1080p pictures are on the
`data-screens` branch, in `run-6-1117-0707` (before) and `run-7-1117-0718` (after). Each `pair-*.jpg` (half size) shows the
before picture on the right and the after picture on the left.

## Fixed in this sweep

- **Motion:** every move of our screens takes 200 ms on cubic-bezier(.2,.8,.2,1) (`Motion` in both apps). This covers the
  control centre, the top row's pills, the favourites tray, and the focus in both settings styles. With «الوضع الخفيف» a
  move is instant and only a fade runs. Test: `tests/motion_test.py`.
- **WebView 66 (#19):** the page drops `inset` and puts a plain value before each clamp()/min()/max(). Flex gap becomes
  margins where the engine has no gap, and the Hijri date comes from Umm al-Qura's own months where the engine has none.
  Test: `tests/webview66_test.py`.
- **«المنطقة»:** each region's count of cities follows the digits setting. Before, it read «(32)» next to «١٢ ساعة».
  Test: `tests/sweep_test.py`.

## Kept (content, not a move of the screen)

- The cross-fades between photos and scenes.
- The home's first arrival.
- The games.
- The kick-off notice.
- The setup progress.
- The home cards' spring scale on focus.

## For the owner (a new design, not a fix)

1. **«·» and «•» in the apps' own sentences.** These are the hub's second lines («الرياض · تلقائي · أم القرى»), the
   adhan bar, the clock's zone line, the now-playing line, the hints, and the line under a TV card. Replacing them with «، »
   touches the English tables, the on/off parser and about 20 tests, so it needs one i18n-aware pass and the owner's word.
   The «،» version is ready to show.
2. **The city prompt on a new home** («وين أنت؟ اختر مدينتك…», «اختر المدينة», «لاحقًا») is three capsule buttons.
   The rule says no pills. Proposal: one line of text with «اختر المدينة» as plain focusable text.
3. **The big outlined clock** of the Riyadh style sits behind the prayer and match cards, by design (the digits are a
   backdrop). Under the «no overlaps» rule it would move above the cards, or the cards would move down.
4. **The control centre's mini-pills at the top left** («الجوال», the mic) are capsules, like the top row. They would
   become plain icons with a text label on focus.
5. **The hint line** at the top of both settings styles is small grey text. It reads well at 1080p but is the smallest
   text on screen. Proposal: one size up, or show it only for a few seconds.
6. **The weather line under «صباح الخير»** («سماء صافية في الرياض، ٢٢°») runs up to the city prompt's first capsule on a
   new home, with only a few pixels between them. Proposal: when the prompt shows, the weather line moves under the clock.

The "after" home shows the next hours' weather because the forecast had loaded by then. In the "before" run it had not.
That is a timing difference, not a change in the code.
