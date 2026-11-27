# خلية الحروف (letter hive, team quiz on a honeycomb)

Our own name and look for the genre of the TV letter-grid quiz: two teams claim hexagons by answering questions whose answer starts with the hexagon's letter. No name, art, sound or questions of any show are used.

## In one line
A board of 28 hexagons (4 rows by 7), one for each letter of the alphabet. Team A must connect the right and left sides, team B top and bottom. A team claims a hexagon by answering its question; the first to make a connected chain wins.

## Board
4 rows by 7 hexagons, one per letter of the 28-letter alphabet (the approved mockup), shuffled for each game; the hexagons are the only focusable things. Team A color goes right-to-left (RTL reading) across the 7 columns, team B top-to-bottom across the 4 rows. Each team's two edges are painted in its color.

## Remote-only flow (default)
1. Ready screen: chips for الفرق (two, with avatars; or 2 against 2 players), مستوى الأسئلة (سهل، متوسط، متنوع; default متنوع), الفئة (للجميع / أطفال), الوقت لكل سؤال (15 s default), button «ابدأ».
2. **Turn.** The team in turn moves focus with arrows to a free hexagon and presses OK. The question appears (from the bank, `answer` starts with that letter), with its four options **hidden by default**; a button «خيارات» shows the 4 options at a cost (see scoring), as a help.
3. The team says the answer within 15 seconds. OK reveals; «صح» claims the hexagon for the team in turn; «خطأ» passes the turn and the hexagon stays free (it can be tried again by the other team, who then get the same question without the first team's wrong answer).
4. After a claim the engine checks for a winning chain (flood fill from team A's left edge to its right edge, etc.). The winning chain is highlighted, then the winner screen.
5. A game takes 8 to 12 minutes. Draw is impossible on a hex board with full fill; if all cells are claimed the chain rule ends the game.

## Scoring
There are no points; the board decides. «خيارات» is allowed once per turn and has no penalty in the default mode (a casual-play default); «تحدّي» mode removes options.

## Phone flow (optional)
Players on phones answer the same questions from four buttons; the first correct answer wins the hexagon. Without phones the host presses صح/خطأ.

## Short letters
Today ظ has 0 questions, ض 2, ذ 2, خ 3, ح 7, ش 8, ص 9, و 10 and ط 11 whose answer starts with the letter. The engine fills a short letter in this order: its own questions; then a **category question** («اذكر دولة أو مدينة أو حيوانًا يبدأ بحرف X», the host judges, with sample answers from `khanat/letters.json` as a hint); a cell may also be marked «بدون سؤال» and be claimed by the team in turn with a single OK. The bank grows by adding more items; nothing in the engine changes.

## Edge cases
- If no question exists for a letter at the chosen level, the engine takes any level for that letter; if still none, the cell uses a free question from the fallback list ("اذكر اسمًا يبدأ بحرف X" with no check: the host decides).
- Normalization of the first letter: أ إ آ → ا; ة ending is irrelevant; «ال» is skipped (القاهرة counts under ق). The index file already applies this; the engine only reads it.
- A question is never shown twice in a game.

## Data
`games/letter-hive/index.json` is derived from the verified quiz bank: for each letter, the ids of quiz items whose normalized answer starts with it; `games/letter-hive/letters.json` lists all 28 letters; `index.json` has `short_letters`, the letters with fewer than 12 questions (today ح خ ذ ش ص ض ظ و ط). For a short letter the engine uses the questions that exist, then falls back to the category rule below. Built by `games/tools/build_words.py`, check by `games/check.py` plus `games/check_words.py`.
