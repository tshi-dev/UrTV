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
