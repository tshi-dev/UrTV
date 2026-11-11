# «دومينو» (dominoes)

One TV, one remote, no phones. 2 to 4 players, people or the computer, so one person can play a full table.
This page is the rules as the app plays them, with every choice we made where the rules have variants, and how the code is laid out.
The spec is `games/domino/SPEC.md` and `games/RULES.md` on the branch `work/games-data`.

Status: the rules and the computer players are done and tested (`tests/domino_test.py`). The screen comes next, on the shared games kit.

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

Tests: `python3 tests/domino_test.py` (needs javac). It checks the set, the shuffle, the deal for 2 to 4 players, who opens, the moves at both ends,
drawing, passing, the blocked round, the scores and the target; then 1500 whole games by computer players with a wild illegal try before every
step (all refused, the 28 tiles conserved, the chain joined, every game ends with one winner); that the computer's move never changes when the
tiles it cannot see are dealt out again; and that the levels are levels.
