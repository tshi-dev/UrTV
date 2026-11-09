# Family games: what to ship first (study)

Setting: one TV, one remote, optional phones, ages 6 to 60 on the same sofa, a round of 5 to 10 minutes.

## What works in a living room

- **Pick, don't type.** A D-pad cannot type Arabic. Every answer is one of 2 to 4 big choices, or "think it, then press OK to reveal".
- **Everyone can play at once.** Teams (2 to 4) take turns; phones, when present, are only buzzers or answer pads. The game must be fully playable with the remote alone.
- **Short turns.** About 30 seconds a question, 10 questions a round = 5 to 8 minutes. A visible round counter and a score bar (as in design/43b).
- **Fair across ages.** Each item carries a difficulty (1 child, 2 family, 3 adult) and an `age` flag (`kids` or `all`). A round mixes levels so a 7-year-old and a grandfather both score.
- **Reveal is the reward.** The answer, a one-line fact, then points. The fact line is where the fun is, so `fact` is optional but wanted.
- **Safe by construction.** No politics, no sectarian content, nothing disputed between schools, nothing that goes stale (current champions, prices, "largest/tallest" records).
- **Text fits a card.** Question at most 90 Arabic characters, answers at most 28.

## Recommended first six

| # | Game | Format | Why it works | Levels |
|---|------|--------|--------------|--------|
| 1 | سؤال وجواب (quiz) | question + 1 right + 3 wrong, teams take turns | the anchor game, matches the mockup, easiest to extend | 1: kids' general, animals, food. 2: geography, science, history, Saudi and Gulf. 3: Islamic general knowledge, language, harder science |
| 2 | صح أم خطأ | statement, 2 buttons | fastest round, ideal for small children and for mixed ages, minimal reading | 1 to 3 |
| 3 | أكمل المثل | first half of a proverb, 1 right ending + 3 wrong | Arabic-first, grandparents know it, kids learn it; no typing | 1 common sayings, 2 less common, 3 rare |
| 4 | الأقرب يفوز (closest wins) | a number question; each team types or picks a guess on the phone or with +/- on the remote; closest wins | the best comedy and argument generator; only stable facts (counts, fixed measurements, dates) | 1 to 3 by how guessable |
| 5 | لغز (riddle) | picture-less riddle (logic, maths, word), reveal on OK | needs no input device; the family argues out loud | 1 to 3 |
| 6 | خمّن الكلمة (حروف style) | a 5-letter Arabic word with 6 tries, coloured letter feedback | in the mockup; it is a word list, not Q&A | easy common words first |

Not first: memory pairs (needs only emoji or word pairs, no content verification, can be added in a day) and a drawing game (needs phones).

## Question formats

- `quiz`, `proverb`: `question`, `answer`, exactly 3 `wrong` of the same kind as the answer (a city next to a city, a number next to a number), shuffled by the app.
- `truefalse`: `question` is a statement, `answer` is `true` or `false`; no wrong options.
- `estimate`: `answer` is a number with a unit; accepted as the exact fact, scored by distance. Only stable facts, two sources.
- `riddle`: `question` and `answer`, no options.
- `word`: Arabic word (5 letters, without diacritics), English gloss, source.

Every item has Arabic and English, a `category`, `difficulty` 1 to 3, `age` kids or all, `source` (what was opened) and `verified`.

## Categories

general knowledge, geography, science and nature, animals, history, Islamic general knowledge (Quran and seerah facts agreed across schools), Arabic language and proverbs, Saudi and Gulf culture, sports (records of the past only), food, maths and logic, kids.

## Delivery

One JSON file per game type under `games/data/`, fetched online on demand (each file well under 500 KB), never packed in the APK.

## What families in Saudi Arabia actually play

Sources opened: Arab News (2025, board games returning to Saudi homes), Saudipedia (Saudi Committee for Traditional Games; the Ramadan games list), App Store listings of Saudi card and board apps, ar.wikipedia (بلوت). No official popularity ranking exists; the order below is our judgement from these. "Traditional" = a folk game with no owner, we may implement it under its own name. "Product" = someone's game: we build our own game of that genre with our own name, rules wording, look and content, and never use their name, questions, art or sounds.

| Game | How it is played | Players | Why it is loved | TV, one remote, optional phones | Status |
|---|---|---|---|---|---|
| بلوت | trick-taking cards, 32 cards, 2 teams of 2, bid صن or حكم, first to 152 | 4 | the Gulf majlis game, skill and banter | hands are secret: phones are the hands, TV shows the table and score; without phones, a pass-the-remote hot seat with a "hide" screen | traditional |
| جاكارو | cards decide the moves of 4 marbles per player on a board, partners help each other | 4 (2 teams) | loud, tactical, cutting the partner's rival | board on TV, hands on phones | traditional |
| دومينو (ضومنة) | match the ends of 28 tiles | 2 to 4 | easy, old, all ages | TV table, hands on phones or hot seat | traditional |
| كيرم | flick discs into pockets | 2 to 4 | a physical board game | a screen version loses what makes it fun | traditional, skip |
| اسم حيوان نبات جماد بلاد | one letter, name one item per category, 60 seconds | 2 to 8 | no equipment, everyone can answer | phones as answer pads, TV shows letter, timer and scoring; remote-only: say answers out loud and press OK to score | traditional, the best first addition |
| بدون كلام (charades) | act a word without speaking, team guesses | 4 to 12 | physical comedy, ages 6 to 60 | TV shows the word to the actor only (a phone or a "look away" step), timer, teams | traditional genre |
| وصف الكلمة (describe, no forbidden words) | say clues for a word without the listed words | 4 to 12 | fast team shouting | same as above, forbidden words shown | genre, own name |
| الكلمة المفقودة | guess the missing word of a sentence or saying | 2+ | grandparents and kids both know it | quiz-like, remote picks | genre |
| برا السالفة | everyone knows the topic but one person; talk, vote the odd one out | 3 to 8 | the biggest party game of recent years | phones show the secret word, TV runs the timer and the vote | product (La Tagool and Barra Alsalfa): make our own, name e.g. «واحد مو معنا», our own topic lists |
| حروف (TV quiz show, hex grid) | two teams claim hexagons by answering a question starting with a letter | 2 teams | TV-show feel, teams argue | perfect on a TV, the remote picks the hexagon | product genre: our own name, look and questions |
| من سيربح (ladder quiz) | ladder of questions with rising points and lifelines | 1 to 4 | suspense, family argues | remote picks, lifelines | product genre: our own name and questions |
| أونو-style card shedding | match colour or number, special cards | 2 to 6 | simple, kids love it | hands on phones | product: our own deck design and name |
| المافيا (social deduction) | hidden roles, night and day votes | 6 to 12 | pure talking | phones deal roles | traditional genre, free |
| Sequence, Saudi Deal, ألعاب بالخمس and similar | board and card products from the Arab News list | 2 to 4 | the revival of the family board game | | products: do not copy |

## Ranking of additions (fit with the TV and one remote, then effort)

1. اسم حيوان نبات جماد بلاد: fits perfectly, effort low (letter and category data).
2. بدون كلام and وصف الكلمة: fits perfectly, effort low (word lists by level and age).
3. «واحد مو معنا» (برا السالفة genre): fits with phones, effort low to medium (topic lists, 12 words each).
4. Letter-grid team quiz (حروف genre): fits the TV best, effort medium (questions keyed by letter).
5. دومينو: effort medium, a clear rules sheet is in `games/RULES.md`.
6. بلوت: the most wanted traditional game, effort high (bidding, projects, bots); rules sheet in `games/RULES.md`.
7. جاكارو: effort high.
8. أونو-style: effort medium, only with phones.

The data files for items 1 to 3 are started under `games/data/` with the same schema where facts need verifying; word lists that carry no fact (describe-the-word, charades) are a different file kind, see `games/VERIFY.md`.
