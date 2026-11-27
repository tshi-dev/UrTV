# Plan: a night mode for the games, and playing from phones

No app code yet. Mockups: `games/design/night-1-games-list.png`, `night-2-hive-board.png`, `night-3-question.png`, `night-4-palettes.png` (made by `games/design/night.py`; real fonts not used, the spacing and colours are what counts).

## 1. Night mode («وضع ليلي»)
**Today:** `GameLook` already has a `NIGHT` set (ink ground, bone words) and every game draws from it through `L`, but `GameLook.of` returns `PAPER` always; «الأسرع» keeps its own constants (pinned line by line in `quiz_test.py` and `step52_test.py`); the quiz's phone page follows the phone.
**What the owner gets:** a switch at the top of the games list beside the header («وضع ليلي»: إيقاف / تشغيل / تلقائي بعد المغرب), remembered per TV (`GamePrefs`), and a few colour choices («ألوان الليل»: three palettes). It turns the list, every setup, every game, the podium and the TV side of «الأسرع» to the night look. The phone page is not touched (it follows the phone).
**Palettes (the owner picks; contrast computed, WCAG ratios):**
| | ground / ink | ink on ground | words on the green block | words on saffron | weakest team colour on ground |
|---|---|---|---|---|---|
| A فحم دافئ (warm charcoal, the one in the mockups) | #1C1A17 / #ECE4D4 | 13.7 | 8.2 | 8.2 | 5.3 |
| B ليل أزرق (blue-black) | #12161C / #E6E9EE | 14.9 | 8.7 | 8.8 | 5.5 |
| C بيج خافت (dim sepia) | #24201A / #E8DCC4 | 11.9 | 6.8 | 7.5 | 5.0 |
Not pure black on white; the green block, saffron, the right (green) and the wrong (coral) are muted a step; the ten team colours are dimmed tints of the day ones (still ten different hues, all at least 5:1 on the ground).
**Work:** palettes in `GameLook` (0.5 day); the switch and the three states in `QuizGamesActivity` and `GamePrefs` (1 day); «الأسرع»'s TV constants read from `L` instead (1 to 1.5 days, the pinned tests change with it); the test: every `GameLook` colour pair meets its floor in both sets (ink and words 7:1 or 4.5:1 as above, team colours 3:1 on the ground and pairwise distinguishable), 0.5 day. About 3.5 days. «تلقائي بعد المغرب»: the app already computes prayer times (`PrayerMethod`, `PrayerAdj`), so it needs one call for today's Maghrib at the TV's place; probably cheap (0.5 day), to confirm in the code before promising.

## 2. Most games playable from phones (a switch)
**Default stays the remote only.** Each game's setup gets one row «اللعب بالجوالات» (إيقاف / تشغيل); off is today's flow, on adds phones on top, and a game started on the remote never waits for a phone.
**One server, one page.** Reuse «الأسرع»'s game-only LAN server and phone page (room code, QR, token, rate limits, no remote control, nothing leaves the network, docs/QUIZ.md "security self-review") as a generic `GamePhoneRoom`: the TV sends each phone a small per-game state, the phone sends a few message types. `play.html` becomes a shell with one view per game (answer pad, hand, keyboard, buzzer).
**What the phone is in each game:**
| Game | Phone does | Removes |
|---|---|---|
| «واحد مو معنا» | shows your secret word or «أنت برا السالفة»; votes | the hand-over screens |
| «بدون كلام / وصّف» | shows the word to the actor; صح / تخطّي / مخالفة | passing the remote |
| «الكلمة المفقودة» | picks one of four words | saying aloud and marking |
| «خلية الحروف» | team buzzers: the first tap answers, then the host says who answered | who-answered step |
| «خمس خانات» | each player types the answers; the TV hides them until the reveal, then lists them under each player | paper |
| «خمّن الكلمة» | the Arabic keyboard on the phone | the 33-key TV keyboard |
| دومينو | your private hand; tap a tile | hot seat and hide screens |
**Order:** (1) generic room and page, with «واحد مو معنا» and «بدون كلام» (secret words are the strongest reason for phones), 4 to 5 days; (2) «الكلمة المفقودة» and «خلية الحروف» buzzers, 3 days; (3) «خمّن الكلمة» keyboard and «خمس خانات» answer pads, 4 days; (4) دومينو hands, 3 days. About 15 days in all, each phase shippable alone.
**Rules kept:** 2 to 12 players per room (10 in «خمس خانات»), names only for the game, answers validated on the TV, a phone that drops out never stops a round (its turn is skipped, the remote flow continues), the phone page follows the phone's dark setting.
**Decision for the owner:** which phase first; whether «الأسرع» and the phone games share one tile «بالجوالات» (suggested) or stay separate.
