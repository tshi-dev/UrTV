# Phone or no phone: the flow today and ONE simpler flow (proposal, nothing built)

The owner finds the way to play with a phone or without one «not good». This page is the review of the whole flow, both ways, with the emulator's shots of today (`docs/research/games-flow/before-*.png`, from `.github/screens/games-flow.sh` on this branch, run 46169286302; the same files are in data-screens/games-flow-latest/) and mockups of the proposal (`after-*.png`, 1080p mockups, not built, same look: no pills, no «·», focus by scale and shadow, Arabic-Indic digits). The words are the ones the games already use or plain Gulf Arabic.

## How it works today

1. **The list** («الألعاب»): a header button (a phone picture) switches the list between «كل الألعاب» and the games «بالجوالات» and says so in a line («بدون جوالات: ريموت واحد يدور بينكم» / «كل واحد بجواله، والريموت للمضيف»). It also decides how a tile opens (the filtered list opens the older games with the phones switch on). Each tile now says «جوال» or «ريموت» (before-01).
2. **The setup page**: the older games (خمس خانات، الكلمة المفقودة، واحد مو معنا، بدون كلام، خلية الحروف، خمّن الكلمة، دومينو) have a row «اللعب بالجوالات: لا / نعم», off every time (before-02). The party games have no such row (they need phones): since beta 5 seven of them have the opposite one, «اللعب بالريموت بس» (before-08 shows the phone-only setup). So the family has two switches that mean opposite things in two places.
3. **No phone**: «ابدأ» opens the remote's pass screen at once (before-03): «سلّموا الريموت لـ لاعب ١، الباقين: غطّوا عيونكم», OK, a 2 s count, the secret alone, OK to hide, the next player. A player count row sits in the setup (3 to 10), the names are the optional «الأسماء» button.
4. **With phones**: «ابدأ» opens the room (before-07, -09, -11): a QR, the address, the 4-digit code in Arabic-Indic digits AND in Latin digits, and a start button that stays grey with «لازم ٤ لاعبين على الأقل». The code is also inside the address. Each phone scans, types a name, taps join. The only way out of a room that nobody joins is Back.

## What is confusing, too long or unclear

1. **Two switches, opposite, in two places.** «اللعب بالجوالات» (old games) and «اللعب بالريموت بس» (party games) say the same thing the other way round; the header phone button is a third one.
2. **The header button is a filter that also changes behaviour**, shown as a phone picture and a line of small words. People do not know it exists, and what it does when pressed is not obvious.
3. **«Off every time»** for the phone switch: a family that always plays with phones turns it on every game.
4. **A phone-only game is a dead end without phones**: «ابدأ» opens a room and a grey button; nothing says «or play with the remote» (the tester hit this on 9 games).
5. **The room says the same thing three times** (the code in two digit styles and inside the address) and has no list of who has come, only a count in a button label.
6. **The player count lives in the setup for the remote and nowhere for phones** (the joined phones are the players), but the row is shown either way.
7. **Number of presses to start a round, no phones** (odd-one, 4 players): tile OK, «ابدأ», then 3 presses a player (ready, secret, hide). With phones: tile OK, up twice and left and OK to turn the switch on, «ابدأ», each phone scans and types and taps, the host presses again.

## The proposal: ONE choice, in ONE place

1. **A single first row on every setup page: «كيف تلعبون؟» with «ريموت واحد» and «جوالات»** (after-01, after-02). It replaces «اللعب بالجوالات», «اللعب بالريموت بس» and the header button. A game that can only be played one way does not show the row. It is **remembered per game** (not «off every time»); the default is the way the game was designed (the older games «ريموت واحد», the party games «جوالات» unless they have a remote way).
2. **The player count row only for «ريموت واحد»**; with «جوالات» a line says «الجوالات اللي تدخل هي اللاعبين (٣ إلى ١٠)», and the start button is «افتح الغرفة».
3. **The list loses the header phone button** (after-04): the tiles already say «جوال» / «ريموت» / «ريموت أو جوال», and the moon (night) stays. Every tile opens the same way.
4. **One block for the room, with a way out** (after-03): the QR, the address (the code only inside it), the phones that came as coloured name tiles with the count («٣ من ٣ على الأقل»), «ابدأ» (grey until enough) and, beside it, **«نلعب بالريموت بدلاً»** for a game that has a remote way: it closes the room and starts the same game on the remote (the choice is remembered). A phone-only game shows only «أغلق الغرفة».
5. **Nothing else changes**: the hand-over (pass screen, count, secret, hide) stays as it is, the names stay optional, the how-to «!» stays; the only text changes are the new row, the room's line and the two buttons.

## What it changes in the code (small)

- `GameActivity.setup`: one `mode` row (`GameChips`) replaces the `phones` and `remote` rows of every game; each game's `start()` reads the row; the ten party games' remote paths (beta 5) are already behind `remoteOn`.
- `QuizGamesActivity`: the header phone button and `filter` go; tiles open every game the same way (the `phones` extra goes).
- `GameActivity.drawPhoneLobby`: one block, the list of names, the counter, the fallback button; the code is shown once.
- Tests: the setup rows per game, the remembered mode, the room's fallback (it closes the room and starts the remote game), and the source pins of the removed button.
- No new content, no new setting storage beyond the chip that is already remembered per game.

## Before and after

| Before (emulator, today) | After (mockup) |
|---|---|
| `before-01-list-header.png`: the header line and the tags | `after-04-list-no-phone-button.png` |
| `before-02-oddone-setup.png`: «اللعب بالجوالات: لا» row | `after-01-setup-one-switch.png`, `after-02-setup-phones.png` |
| `before-03-oddone-remote-start.png`: the remote's first screen | unchanged |
| `before-07-oddone-phones-lobby.png`, `before-09-liar-lobby.png`, `before-11-quiz-room.png`: the grey room | `after-03-room-one-block.png` |
| `before-08-liar-setup.png`: a party game with no remote way | the same page with the row «كيف تلعبون؟» |

To be decided by the owner: whether the default for the party games is «جوالات» (as now) or «ريموت واحد»; whether the header button goes (the proposal) or stays as a shortcut.
