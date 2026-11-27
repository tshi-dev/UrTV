# دومينو (dominoes)

A traditional game, free to implement under its own name. Rules summary in `games/RULES.md` (draw dominoes, double-six, 28 tiles).

## Players and setup
2 to 4 players, each a "side". Default 2 players. Tiles: 28 (double-six). 2 players take 7 tiles each and the rest is the boneyard; 3 players take 9 each and the double-blank is set aside, no boneyard (ar.wikipedia, opened); 4 players take 7 each, no boneyard (ar.wikipedia). The holder of the highest double starts.

## Remote-only flow (default)
The TV shows the chain and a **hand strip** along the bottom. Hands are secret, so the remote is passed with the hand-over screen (COMMON.md): «سلّموا الريموت لـ <name>», then the hand shows, the player moves focus left/right over the tiles, presses OK on a tile; if it fits on only one end it is placed there, if on both ends the player chooses with left/right and OK. The turn ends with the hide screen. Playable tiles are highlighted; non-playable tiles are dimmed and cannot be selected, so a wrong move is impossible. With no playable tile the button «سحب» draws from the boneyard (2-player game), then «تمرير» when it is empty. Hot-seat for 2 players; for 3 or 4 players it is the same.

## Phone flow (optional)
Each phone is a private hand; the TV shows only the chain, the counts of tiles each player holds and whose turn it is. No hand-over is needed.

## Scoring and end
First to play all tiles wins the round and scores the pip total of the others' remaining tiles. A blocked game is won by the lowest pip total (equal totals: the round is a draw, no score). Target 100 points by default (chips: 50, 100, 150), the first side to reach it wins.

## Edge cases
- The chain scrolls and bends at the edges of the TV (a snake) so it never gets small; the two open ends are always marked.
- «تراجع» does not exist (hot-seat honesty), but a confirm step is shown on a tile that can go on both ends.
- Rule variants not in the default: block (no boneyard) for 2 players, and "Muggins" scoring: not implemented.

## Data
None (no question data). The tile set is generated.
