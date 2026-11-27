# بدون كلام / وصّف (act it out, or describe it)

Two modes on the same word deck; the genre is a folk game. Names «بدون كلام» (act without speaking) and «وصّف» (describe without saying the word) are plain Arabic; no product's name, taboo cards or word list is used.

## In one line
A team member sees a word and gets the team to say it before the timer ends, by acting (بدون كلام) or by describing (وصّف).

## Remote-only flow (default)
1. Ready screen: chips for الوضع (بدون كلام / وصّف; default بدون كلام), الفرق (2 to 4; default 2), الوقت (45, 60, 90 s; default 60), the age filter (للجميع / أطفال), button «ابدأ».
2. **Team turn.** Header: «دور الصقور». The remote holder is the **actor**. The first screen is the hand-over screen («غطّوا عيونكم» for the guessers who must not see the TV); the actor presses OK to see the word on the secret screen, memorises it, and presses OK again to hide it: the clock starts. If the actor forgets, OK peeks again (free; the word hides again after OK or 3 seconds).
3. **Play.** The team guesses aloud for the timer. When the word is said the actor presses OK for «صح»: +1 point, next word at once (secret screen again). Left arrow is «تخطّي» (skip, costs nothing but at most 3 skips per turn). At the end of the timer a bell rings, the current word is shown to everyone, and the turn passes.
4. **Scoring.** 1 point per word guessed. Optional setting «وصّف» only: -1 for a spoken forbidden part (a word of the card, or a word from the same root) when the other team's host presses «مخالفة».
5. **Rounds.** Every team gets the same number of turns (default 3); highest total wins; a tie gets one sudden-death word each.

## Phone flow (optional)
The actor's phone shows the word (so the remote is never passed); the TV shows the timer, the team bar and, for guessers, only the letter count. The phone also has the three buttons صح، تخطّي and مخالفة.

## Edge cases
- Back pauses the clock; the secret screen hides first.
- Deck exhaustion: when all words of the chosen age and level are used, the deck reshuffles and says so.
- Kids filter picks only `age: kids` words and difficulty 1.

## Data
`games/charades/words.json`: words (Arabic and English) in categories (animals, food, jobs, sports, vehicles, clothes, places, actions), `difficulty` 1 to 3 (rank by how well known the thing is), `age`, `modes` (`act`, `describe`, or both). Source: Wikidata items (an item must carry exactly this Arabic and English label). Actions and verbs are not in the first data file.
