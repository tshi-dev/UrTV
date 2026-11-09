# خمّن الكلمة (guess the five-letter word)

The word game of the design/43b mockup (28 keys in 7 by 4; six tries; letters turn green, yellow or gray).

## In one line
Guess a hidden Arabic word of five letters in six tries. After each guess each letter shows: right place (green), in the word elsewhere (yellow) or not in it (gray).

## Remote-only flow (default)
1. Start: «ابدأ» with chips for عدد المحاولات (6; 5 or 7) and the age filter (للجميع / أطفال). The word is chosen at random from `answers.json` by the TV; **nobody has to see it** because the game is a team of everyone against the word. Pass-and-play is not needed.
2. The player (or the group, aloud) moves a focus cursor over the 28-key keyboard with arrows and presses OK on a letter; a key «حذف» and a key «إدخال». A guess is accepted only if it is in `accepted.json`, else the row shakes and «ما نعرف هذي الكلمة» appears; it does not cost a try.
3. After «إدخال» the row flips with the feedback; the keyboard keys take the best-known color. Win: the word and its hint show with a confetti burst; loss: the word is shown.
4. **Two-player duel** (setting «تحدّي»): player 1 plays the word first; then player 2 plays the same word; fewer tries wins, a tie goes to the next word. Hand-over is not needed because the same word is revealed after both.

## Phone flow (optional)
Phones can be the keyboard (typing Arabic is natural there), the TV shows the board. Several phones can type for the same board; the first to press «إدخال» wins the guess.

## Normalization (the same for answers, accepted guesses and what is typed)
1. Remove diacritics (harakat, sukun, shadda) and tatweel.
2. أ إ آ ٱ → ا.
3. ة → ه.
4. ى → ي.
5. The 28 letters of the keyboard are: ا ب ت ث ج ح خ د ذ ر ز س ش ص ض ط ظ ع غ ف ق ك ل م ن ه و ي. A word with ء ؤ ئ is not in the lists (it cannot be typed), so the keyboard has no hamza keys; all lists are filtered to exactly these 28 letters.
6. A word has to be five letters after normalization. The word shown to the player (the answer reveal) is the original spelling kept in `answers.json` (`display`).
Feedback with repeated letters follows the standard rule: green first, then yellow for the remaining count of that letter in the answer, the rest gray.

## Edge cases
- The answers list is only common family-safe words (animals, fruits, foods, jobs, places, everyday things). The accepted-guesses list is larger (dictionary lemmas) so valid guesses are rarely refused.
- A word is never repeated in a game; the TV stores the last 200 answers played.
- RTL: letters fill each row right to left; the cursor wraps.

## Data
- `games/word5/answers.json`: common words, with `display`, English meaning, `category`, `difficulty`, `age`, `source` (Wikidata item).
- `games/word5/accepted.json`: normalized five-letter strings only (from Wikidata Arabic lexemes and the answers list), a plain sorted array.
Both are built by `games/tools/build_words.py` and checked by `games/check_words.py`.
