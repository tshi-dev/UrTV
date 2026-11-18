# The games engine (4.4)

A game of the family hub can be **data**: a small JSON file from urtv-data that the app plays with building blocks it already has. A new game, or a change to one, then reaches the TVs without an app update and **never as code** (nothing is downloaded and run). What the file can say is closed and checked; anything else is not a game, and the game stays hidden.

Release is separate (`docs/GAMES-RELEASE.md`): a game is on a TV only when the release file names it for that TV's group. Nothing about an engine game is in the hub, the changelog or the settings until then.

## A game
    {"v": 1, "id": "test-1", "name": {"ar": "اختبار ١", "en": "Test 1"}, "line": {"ar": "…", "en": "…"},
     "icon": "star", "look": {"accent": "saff"}, "kids": false,
     "players": {"min": 1, "max": 8, "teams": true},
     "rounds": 8, "seconds": 20, "score": "first", "points": 3, "hints": true,
     "source": {"ar": "من أين المحتوى", "en": "where the content comes from"},
     "items": [ {block}, … ]          or          "pack": "<pack name>"

- **`id`** lower-case letters, digits and dashes, 2 to 40. **`name`** at most 24 characters, **`line`** at most 70: a string (Arabic) or `{ar, en}`. **The Arabic is required and must have Arabic letters**; the English is optional and English only.
- **`icon`**, a picture from the built-in set (nothing is downloaded): `star`, `heart`, `circle`, `square`, `triangle`, `diamond`, `question`, `check`, `clock`, `bulb`, `flag`, `drop`.
- **`look.accent`**, a colour of the app's look (so it follows the night palettes): `saff`, `ok`, `pink`, `deep`, `ink`, and the ten player colours `side0`, `side1`, `side2`, `side3`, `side4`, `side5`, `side6`, `side7`, `side8`, `side9`. A game never writes a colour.
- **`kids`**, **`hints`**, **`players.teams`**: true or false. **`players`**: `min` and `max`, 1 to 10 (the phones are the players). With `teams` on, the setup offers 2, 3 or 4 teams (the phones take them in turn); a team's score is its phones' points added.
- **`rounds`** 3 to 30 (fewer when the game has fewer items), **`seconds`** 5 to 120 per round, **`points`** 1 to 10.
- **`items`** at most 200, or **`pack`**: the name of a game pack (`{"v": 1, "items": [...]}`, downloaded like the other packs, `docs/GAMES-PACKS.md`) whose items are checked as strictly. Never both, never neither. Items the TV played lately wait while there are enough others.
- **`source`** where the content comes from (urtv-data's check wants it for a game that is published; the app ignores it).

## The blocks (`t` of an item)
| `t` | item | the phone | answered right when |
|---|---|---|---|
| `choose` | `q`, `o` 2 to 6 options, `a` the number of the right one | buttons | the right option |
| `odd` | the same, «which one is the odd one out» | buttons | the right option |
| `tf` | `q`, `a` true or false | صح / خطأ buttons | the right one |
| `order` | `q`, `o` 3 to 6 things **in the right order** (the TV shuffles them) | tap them in order, undo, send | the whole order |
| `pairs` | `q`, `p` 3 to 5 pairs `[left, right]` (the TV shuffles the rights) | tap the right-hand match for each left in turn | every pair |
| `word` | `q`, `a` up to 6 accepted answers | a text field | the answer, in a forgiving spelling |
| `blank` | `q` with `___`, `a` answers | a text field | the answer; the TV shows the sentence with the answer in the blank |
| `closest` | `q`, `a` a number, `u` a unit | a number pad | the exact number; otherwise the nearest wins (`score` «closest») |
| `draw` | `w` the word `{ar, en}` | the drawer draws with a finger; the others type guesses | the word (the English also counts); the drawer scores for each right guess |
| `buzz` | `q`, optional `a` the answer to show | a button «أنا!» | the host calls the first to press right (OK) or wrong (down); a wrong one is locked out |
| `vote` | `q` | pick another player | you voted for the player with the most votes (a tie shares) |

A hint (`h`, a text) may be given to any block; with `hints` on it appears on the TV at half the time. **A typed word** is compared with marks and tatweel off, أ إ آ ٱ = ا, ى ئ = ي, ؤ = و, ة = ه, a leading «ال» off, Arabic digits as Latin, Latin lower case.

## Scoring (`score`)
`first` the first right answer scores `points`, the next one less, down to 1 (the default) · `all` every right answer scores `points` · `speed` a right answer scores 1 to `points`, more the earlier · `closest` for a `closest` block: the nearest number scores `points`, the next one less, an exact number 1 more · `bank` one bank for everyone: a round where more than half the players are right adds `points`, any other round loses one of three lives, and the game ends with the last life. A `vote` round scores 1 for each voter of the most voted player; a `buzz` round scores `points` for the speaker the host calls right.

## What is refused (and then the game is not a game, never a broken screen)
An unknown block, icon, accent or score; a missing or wrongly typed field; more than 200 items, more than 4000 values in a file, a text over its limit (a question 160, an option or an answer 40); an Arabic text with no Arabic letter (an option or an answer may be a number); a text with a control or direction-changing character, a zero-width space, `·`, `•`, a link or a space at an end; a blank without `___`; a pack that is not there or has anything wrong. `GameDef.parse` says why in one line, and urtv-data's `tools/check_content.py` runs the same rules before anything is published.

## Where it runs
`GameDef` (the reading and the limits), `GameEngine` (the rules; no Android, tested on a JVM), `EngineGameActivity` (one screen for every game, built on the games kit: the green question block, the timer, answer stamps, the podium, the pause card, the night palettes), `EngineLayout` (where the stamps stand: inside the 96 px safe area, no overlap, 56 px high and 300 px wide at the least at 1280 x 720), `EngineIcons`, the phone cards of `GamePhoneRoom` (`pick`, `order`, `text`, `draw`, `act`). The hub builds a tile from the file's name, line, icon and accent.

## Limits of the engine, on purpose
Rhythm, a map to tap, a board game and anything else that needs a screen of its own are **native games**, not blocks: they ship as code only when they are released. Games here are rounds of questions, tasks and votes.

## Examples
`docs/examples/engine/test-1.json` (choose, true or false, odd one out, word, blank, closest number, with teams and hints), `test-2.json` (order, pairs, vote), `test-3.json` (draw and guess, buzz, speed scoring). They are made-up test games; `tests/game_engine_test.py` plays each one through on a JVM. A real game cites its content's source in `source`.

## Tests
`tests/game_engine_test.py` and `tests/GameEngineTest.java`: hidden by default, the a/b timing, revoking, the offline copy, a broken file, group B's decryption and a wrong code that is silent, every block's rules and scoring, the limits and the refusals, the layout at 1280 x 720, and the pins for R1 and for the privacy of the request. The phone's order card is in `tests/PhoneRoomTest.java` and `tests/game_phone_page.test.mjs`.
