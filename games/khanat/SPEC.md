# خانات (name-the-boxes letter game)

Our own name and wording for the traditional game «اسم حيوان نبات جماد بلاد». Free to implement as a folk game.

## In one line
A random letter appears; every team writes or says one answer per box that starts with that letter; the fastest finisher shouts «خلّصت»; points for each answer that is right and not repeated.

## Boxes (default 5)
حيوان، نبات، جماد، بلاد، اسم. Optional extras in the settings chips: أكلة، مهنة، مدينة سعودية، مدينة (any city: world capitals from Wikidata) (replace «اسم» or add a sixth). Each box has an icon and a color; 4 to 6 boxes fit the TV.

## Remote-only flow (default)
1. Ready screen: chips for الجولات (3, 5, 8; default 5), الوقت (60 or 90 s or «بدون وقت»; default 90), الصناديق (5 or 6); button «ابدأ».
2. **Letter reveal:** the letter spins like a wheel for about 1.5 s and stops on a big letter. The letter is drawn at random from the **letters list**, never repeating inside a game.
3. **Writing time:** teams write on paper or just remember; the TV shows the letter, the boxes and the countdown. Anyone may press OK to stop early: «خلّصنا» goes to the team (the next screen asks which one with the four team avatars).
4. **Reveal round:** boxes are revealed one at a time with OK. For each box, the screen shows: the box, the letter and each team's chip. The group agrees aloud; the host presses the chip of each team that answered **validly**. Hint line, only when asked (a button «أمثلة»): up to 5 sample answers for this letter and box from the data file, so an argument can be settled.
5. **Scoring** per box: 10 points for a valid answer nobody else gave, 5 points for a valid answer another team gave too, 0 for none or invalid. The team that pressed «خلّصنا» first gets +5 once if all its boxes were valid. Totals show on the team bar; after the last round the winner screen shows.
6. Ties: one tie-break round with a single box chosen at random.

## Phone flow (optional)
Each phone is an answer pad: it shows the letter and the boxes, players type answers. The TV hides them until the reveal, then lists them under each team. Auto-check against the sample list is **advisory only** (a green tick when the answer is in the list; never a red cross): the list is not the whole language, so the group decides.

## Letters list
25 letters are used: ا ب ت ج ح خ د ذ ر ز س ش ص ط ع غ ف ق ك ل م ن ه و ي (ث ض ظ are rare and removed by default; «حروف صعبة» chip adds them). Normalization: أ إ آ count as ا; ة counts with ه and ت for matching; ى counts as ي. Words starting with «ال» are judged by the word after it.

## Edge cases
- Same answer spelled two ways: the group decides; the host presses the same chip state for both.
- A team that stops early with a wrong box still gets the points of its valid boxes.
- Back during writing pauses the timer.

## Data
`games/khanat/letters.json` (letters, sample answers per box per letter). Samples come from Wikidata labels of the most-linked animals, plants, countries, cities and Saudi cities, and are advisory.
