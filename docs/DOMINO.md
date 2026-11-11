# «دومينو» (dominoes)

One TV, one remote, no phones. 2 to 4 players, people or the computer, so one person can play a full table.
This page is the rules as the app plays them, with every choice we made where the rules have variants, and how the code is laid out.
The spec is `games/domino/SPEC.md` and `games/RULES.md` on the branch `work/games-data`.

Status: built on the games kit (docs/GAMES-KIT.md) and listed in «الألعاب». The rules, the computer players, the chain's layout and the turns are
tested on a plain JVM (`tests/domino_test.py`); the screen itself has not been seen on a real TV yet (see the end of this page).

## The rules

The game is the **draw game** with the double-six set (28 tiles), the one played most in Saudi Arabia and the Gulf, in rounds to a target score.

| Rule | What the app does | Why |
|---|---|---|
| Tiles | double-six, 28 tiles | [checked] in RULES.md |
| Sides | each for himself, also with four players | SPEC: «each a side». Partners (2 against 2) is a [variant], not built |
| Deal, 2 players | 7 tiles each, 14 in the boneyard (السحب) | [checked] |
| Deal, 3 players | 9 tiles each, the double blank set aside, no boneyard | [checked] |
| Deal, 4 players | 7 tiles each, no boneyard | [checked] |
| Who opens the first round | the holder of the highest double, and he opens with that tile | [variant]: we took the highest double, the common rule |
| No double was dealt (2 players only) | the heaviest tile opens (most pips, then the higher half) | [open] in the rules: our choice, so a round always has an opener without a new deal |
| Who opens later rounds | the winner of the last round, with any tile | [variant]: the common rule. After a drawn round the highest double opens again |
| A move | one tile on one of the two open ends, a half matching that end. A double is laid across | [checked] |
| A tile fits both ends | the player chooses the end | SPEC |
| No tile fits | he draws one tile at a time until one fits or the boneyard is empty, then passes. With a tile that fits he may neither draw nor pass | [checked]. The boneyard is drawn to the last tile: «leave two» is a [variant], not built |
| Going out | the first empty hand wins the round and scores the pips left in all the other hands | [checked] |
| Blocked round | nothing to draw and no hand fits: the lowest pip count wins and scores the pips of the other hands (his own are not taken off) | [checked in outline]; not taking off his own is our choice, the simpler count |
| Blocked, lowest count shared | the round is a draw, nobody scores | SPEC |
| Target | 50, 100 or 150, 100 by default. The first to reach it wins the game | SPEC (RULES.md lists 100, 150, 200 as a [variant]; the SPEC's chips win) |
| Not built | the block game for 2 players, Muggins (fives) scoring, undo | SPEC |

## The computer players

Three levels. Each one decides from what a person at the table would know: its own hand, the tiles on the table, how many tiles each player holds,
the scores, and what everybody saw (who drew or passed, on which ends). It never sees another hand or the boneyard: the code gives it a `View` of
copies, not the game. The same seed gives the same game, move for move.

- **Level 1** («سهل»): any tile that fits, by chance.
- **Level 2** («وسط»): doubles first, then the heaviest tile.
- **Level 3** («صعب»): doubles early and heavy tiles out, but it keeps numbers in hand to answer both ends with (variety), and it closes the ends
  on the numbers a player was seen to lack, more so when it is ahead (fewer tiles than the others, or leading in the score). No search, one pass over the hand.

Measured in the test, 600 two-player games to 100 with the seats swapped: level 3 wins about 540 against level 1 and about 480 against level 2;
level 2 wins about 470 against level 1. A decision takes well under a millisecond.

## The code

`android/app/src/main/java/sa/urtv/screensaver/games/domino/`, plain Java 8 with `java.util` only (no Android, no clock, no I/O):

- `Tile`: one tile, immutable.
- `DominoGame`: the deal, whose turn, `legal(p)`, `play`, `draw`, `pass`, the end of a round, the scores, the target. Nothing illegal is ever
  accepted: the call answers false (or null) and nothing changes. The only chance is the `Random` given to the constructor.
- `DominoBot`: `choose(view, level, random)` gives the computer's play, draw or pass.
- `Snake`: where each tile of the chain lies on the table. It grows both ways from the round's first tile, turns at the table's edges (the right arm
  down, the left arm up, rows two units apart) and picks the largest unit that fits, from 72 px down. A double lies across the line, but in line at a
  turn (the corner tile and the one after it). A laid tile stays where it is; the table moves only when a row is added or the unit shrinks. On the
  1080p table (1728 x 480) the unit is 72 px and drops to 64 px only when a third row is needed; with all 28 tiles on one arm it never goes under 56 px.
- `DominoTable`: one remote at the table. The screen sends `move`, `ok`, `back`, and draws only what the answer says changed (the focus, the hand,
  the chain, the state, the scores). States: `HANDOVER` («دور <name>», no hand shown), `TURN` (the hand, focus only on tiles that fit; with none, on the
  one button: draw, then pass), `CHOOSE_END`, `BOT`, `ROUND_END`, `GAME_END`.

### The hand on the screen

- One person at the table: his hand stays on the screen through everything, no hand-over.
- Two people or more: a hand shows only in its owner's turn, after OK on the hand-over. It hides when he has played, and when the app leaves the
  screen or the pause card opens (`hide()`), so coming back shows the hand-over again.
- A tile that fits both ends asks which end only when the two ends differ. When both show the same number it is laid at once, on the shorter arm.
- A person with no tile that fits still takes his turn: the button draws one tile for each OK, then passes. Nothing is done for him.
- OK during a computer's move skips to the next person's turn.

Tests: `python3 tests/domino_test.py` (needs javac). It checks the set, the shuffle, the deal for 2 to 4 players, who opens, the moves at both ends,
drawing, passing, the blocked round, the scores and the target; then 1500 whole games by computer players with a wild illegal try before every
step (all refused, the 28 tiles conserved, the chain joined, every game ends with one winner); that the computer's move never changes when the
tiles it cannot see are dealt out again; and that the levels are levels.

## The screen

`DominoActivity` (package `sa.urtv.screensaver`, on `GameActivity`). Setup chips: «اللاعبون» 1 to 4 people, «الكمبيوتر» none to 3 (only what makes
2 to 4 at the table is offered), «مستوى الكمبيوتر», «الهدف», «الصوت». The default is two people, no computer, to 100. People take the kit's names
(الصقور، النجوم…), people first at the table, then the computer players.

On the 1920 x 1080 design: the sides' bar (name and points, the side to play in ink), under it each side's tile count as a number and small backs,
the table (1728 x 480), the hand along the bottom (a half tile is 64 px), then one row for the draw / pass button or what just happened («مرّر … الدور»),
with the target and the boneyard's count at the end side, and the kit's line of keys.

- The hand-over is the game's own, lighter than the kit's secret screen: a green block where the hand would be, «دور <name>» and «الباقين: غطّوا عيونكم»;
  OK shows the hand. The kit's `handOver` shows a word after a two-second count, which does not fit a hand of tiles played every turn.
- The two open ends have a saffron frame. A tile that can go on two different ends is shown on the table at the end it would take; left and right move
  it to the other end (as the eye sees the table, in both languages), OK lays it, Back returns to the hand.
- A computer player waits 350 ms, then its tile slides for 250 ms from its place in the bar. OK skips to the next person's turn. A low-RAM TV, or
  animations off, gets no slide.
- A round's end shows the table for a moment (OK skips), then the round's result and the board; the game's end is the kit's podium.
- «إنهاء الجولة» on the pause card ends the game with the scores as they stand.

### One key, how much work

The table is built once. `DominoTable` answers each key with what changed, and the screen touches only that:

| Key | What is redrawn |
|---|---|
| an arrow in the hand | the two tiles the focus left and reached (two small views), nothing else: no `redraw()`, no view added or removed |
| an arrow while choosing the end | the table view, once |
| OK that plays a tile | the table view once, the one sliding tile, the hand, the bar and the row under the hand |
| a computer's move | the same without the hand (one person) |

Measured: the rules' share of an arrow key is about 150 ns on a desktop JVM (`tests/domino_test.py` prints it), and the source pins above are checked by
the same test. Not measured: frame times on a TV. That needs a real set.

## What needs a real TV

- The frame time of a key on a weak TV (the arrows in a full hand of 21 tiles, a move with a three-row chain).
- The look at 3 m: tile and pip sizes at 72 / 64 px on the table and 64 px in the hand, the saffron frames on the open ends, the dimmed tiles.
- The slide's start and end points in Arabic and in English, and at 720p and 4K.
- The hand-over with real people: is «دور <name>» enough, and is hiding the hand after each move quick enough with two.
- A hand that grew to 15 or more tiles after drawing (the tiles close up; they should still be easy to tell apart).
- The tick on each tile and the fanfare through the TV's speakers.
