# «واحد مو معنا» and «بدون كلام / وصّف»: how they are built

Two family games played with **one remote and no phones** (pass-and-play). Specs: `games/odd-one/SPEC.md`, `games/charades/SPEC.md` and `games/COMMON.md` on the branch work/games-data. Words: `docs/GAMES-DATA.md` (`odd-one.json`, `charades.json`). Look and shared screens: the games kit (`docs/GAMES-KIT.md`).

## The parts
| Part | Where | What |
|---|---|---|
| Rules | `games/oddone/OddOneGame.java`, `games/charades/CharadesGame.java` | One game's state: phases, timers, votes, points. No Android, no screen, `java.util` only, so a phone mode can drive the same object later (`secretFor(player)` already answers per player). |
| Words | `OddOneWords.java`, `CharadesWords.java` | The whole-file check of the published forms, and the picking: never a word twice in a game, the last ones played on this TV wait (120 and 200). |
| Screens | `OddOneActivity.java`, `CharadesActivity.java` | Built on the kit (`GameActivity`): its hand-over shows every secret, its pause card answers Back, its chips, timer, score bar, board and podium draw the rest. |
| Tests | `tests/party_games_test.py`, `tests/PartyGamesTest.java` | All of the above on a plain JVM with a fake clock and the bundled starters. |

## «واحد مو معنا»: the rule choices
- **Players 3 to 10**, default 4, named by number. Rounds 3 or 5. One lap of talk, or two.
- **Deal:** the remote goes round in seat order. The kit's hand-over: pass screen → OK → a 2-second count → the secret until OK (it hides by itself after 20 seconds) → the next pass screen. Normal players see the topic and the word; the odd one sees the topic and «أنت برا السالفة».
- **A secret never stays up:** Back on it only hides it (the same player again); the app leaving hides it. Before the first speaker, DOWN «نعيد توزيع الكلمات» sends the remote round again with the same word, for someone who missed his.
- **Talk:** the seats in a circle from a random first speaker (the odd one may be first: starting anywhere else would clear a player). 30 seconds each as a guide only: one soft beep at 0, OK moves on.
- **The vote is by show of hands (the default, chosen as the faster one for a living room):** everyone points on the count, the host picks the most pointed-at player on the grid, or «ما اتفقنا» when the room cannot settle it (counts as not caught). It is one screen instead of one hand-over per player.
- **The secret vote** of the SPEC is kept in the rules only (no screen yet; it suits a phone mode): the remote goes round, each picks a suspect (never himself); the count comes after the last vote; a tie is voted again once between the tied players only; a second tie accuses nobody.
- **Reveal:** who the room chose, then who the odd one was. Caught: one spoken guess; OK opens the word for the room, then «صح» (right, focused) or «خطأ» (left).
- **Points:** caught and wrong: 1 to every other player. Caught but right: 1 to the odd one only. Not caught: 2 to the odd one. Most points wins; a tie is shared.
- **Words:** «عشوائي» goes through every topic before one comes again; a chosen topic that runs out hands over to another.
- **«إنهاء الجولة»** on the pause card drops the round with no points.

## «بدون كلام / وصّف»: the rule choices
- **Teams 2 to 4**, 45 / 60 / 90 seconds (default 60), 3 turns each. «أطفال» takes kids words of level 1 only. «وصّف» has the words that cannot be acted as well.
- **Turn:** pass screen → OK shows the actor the word, the clock waits → OK hides it and **the clock starts**.
- **Keys on the clock:** OK «صح» (+1 and the next word at once), LEFT «تخطّي» (free, 3 a turn), UP shows the word again for 3 seconds (free), DOWN «مخالفة» (only in «وصّف» with the setting on: -1, never below 0).
- **The first word** comes through the kit's hand-over. **A new word** stays up 4 seconds or until OK, with «الباقين: غطّوا عيونكم» under it. «صح» is ignored for 0.6 seconds after a new word came, so a double press does not throw it away.
- **The clock at 0:** the bell, the open word is shown to everyone, the turn passes. Back hides the word first and stops the clock; before the clock started it returns to the pass screen with the same word.
- **A tie for first:** sudden death between the tied teams only, one word each, 30 seconds, no skip; up to 3 goes, then they share the win.
- **Deck:** used up → shuffled again and the screen says so; never the same word twice in a row.

## Left for a real TV
Reading the word from 3 to 4 m in 4 to 5 seconds, the feel of the 0.6-second guard, whether 2 seconds of count is enough to lift the remote, and the bell and beep levels.

## The words on screen
In `i18n/en/saver.tsv`, under the line of the two games.
