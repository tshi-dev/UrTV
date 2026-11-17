# 4.4 sweep 2: no «·», no pills (for the owner's approval)

These are real screens from the CI emulator at 1080p (`.github/workflows/screens.yml`). Each `pair-*.jpg` is half size: the
after picture (`work/44-sweep2`) is on the left, the before picture (the 4.3.2 candidate) on the right. The full shots are on
the `data-screens` branch, in `run-6-1117-0707` (before) and `run-11-1117-1020` (after).

## What changed

1. **One separator per language.** Two parts of one line are now joined by «، » in Arabic and ", " in English, never «·» or
   «•».
   - This covers the hub's second lines, the adhan bar, the clock's zone line, the now-playing line, the hints, the line under
     a TV card, the markets footer, the inputs' standby mark and the phone page.
   - Both apps have `S.sep()`. The screensaver's English reads a «، » line part by part.
   - The setup's bullet list is now plain lines.
   - A hidden number stays «••••», because it is a mask and not a separator.
   - Test: `tests/separators_test.py` bans «·» and «•» in both languages.
2. **The city prompt.** It is one glass strip (12 dp corners) with plain words in it: «وين أنت؟ …», «اختر المدينة»,
   «لاحقاً». Only the focused choice has a fill (its focus ring), and nothing in it is a capsule.
3. **The control centre.** «تخصيص», «شكل اللوحة», «إعادة الترتيب» and «تم» are plain words with no outline at rest, and
   get an 8 dp fill on focus. The top row's bar is never rounder than 8 dp, and a labelled button such as «الجوال» has 6 dp
   corners.
4. **The settings hint.** It now uses the rows' secondary size (14 sp, and couch-tv's info-line size). It was the smallest
   text on the screen.
5. **The weather line and the top row.** The city strip keeps at least 16 px (at 1080p) from the weather line under
   «صباح الخير» and from the top row as it opens. When there is not enough room for both gaps, the question ends in «…»
   (`pair-home-pill-left-1.jpg`), and it shows in full again once there is room.

## For the owner to choose: the Riyadh scene's outlined clock behind the cards

These three are mockups (an HTML render of the real layout), not screenshots:

- `riyadh-clock-now.jpg`: now. The cards sit over the lower part of the outlined clock.
- `riyadh-clock-down.jpg`: **option 1.** With this scene, the cards move down under the clock, 16 px below it, still
  above the apps.
- `riyadh-clock-hide.jpg`: **option 2.** While cards show, the regular clock replaces the outlined one, and the outlined
  clock comes back when the cards are off.
