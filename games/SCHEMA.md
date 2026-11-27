# Schema: how the games' data grows without an app update

Built by `games/tools/build_meta.py`; the reference reader is `games/tools/meta_reader.py`; pinned by `tests/games_meta_test.py`. The app's Java parsers follow these rules (a 4.3.2 app change; today's parsers are strict, see the table at the end).

## Files on data-games
| Path | Form | For |
|---|---|---|
| `quiz/quiz.json` | the old bare array of compact items, unchanged | apps up to 4.3.1 (they reject any file that is not exactly this) |
| `quiz/quiz-v2.json` | `{"v":2,"items":[compact item,...]}`; an item also has `t`, its topic tag (`capitals`, `elements`, `surah-order`...) | new apps; extra fields in an item are ignored. A game may cap one topic's share of a round (for example 25 percent) so a category is not one template; `meta.json` counts the topics per category |
| `games/meta.json` | below | the setup chips of every game |
| `games/<game>.json` | `{"v":1,...}` (docs/GAMES-DATA.md) | each game's data |

## meta.json (v1)
```
{"v":1, "min_items":40,
 "levels":[{"id":1,"ar":"سهل","en":"Easy"}, ...],
 "groups":[{"id":"islamic","ar":"دين","en":"Religion","cats":["islamic"]}, ...],      // the chips of «الفئة»; «مزيج» is added by the app
 "games":{
   "quiz":{"items":1270,"bank":"<12 hex: fingerprint of the sorted ids>","cats":[{"id","ar","en","n":[easy,medium,hard],"kids":count}]},
   "missing-word":{"decks":[{"id","ar","en","n":[..]}]},   "odd-one":{"topics":[{"id","ar","en","words"}]},
   "charades":{"cats":[{"id","ar","en","n":[..],"kids"}]},  "khanat":{"boxes":[{"id","ar","en","extra","samples"}],"letters","hard"},
   "letter-hive":{"letters","short"},  "word5":{"answers","guesses"}},
 "variants":[]}        // later: {"game","id","ar","en","set":{chip key: option index}} (a new way to play a game, as data)
```
Names always come with both languages. A category in the data without a name stops the build.

## The rules every reader follows (the tolerant parser)
1. **Unknown fields are skipped** at every level (a new key in a file, an item, a category, a game). They never reject a file.
2. **An entry that cannot be used is skipped, not the file**: a category without an id or without both names, an item with a missing field or repeated options.
3. **Versions:** every file has `"v"`. A file whose `v` the app does not know is ignored and the app keeps its built-in lists and the bundled starter. Adding a field, a category, a deck, a game or a variant keeps `v`. Changing the meaning of a field or removing one is a new file name (`meta-2.json`, `quiz-v3.json`) published beside the old one, so old apps keep reading the old one.
4. **Chips come from the data:** a group is offered only when its categories hold `min_items` (40) or more questions for the chosen level and the kids filter (else dashed «قريبًا»). A category in the data that is in no group appears by itself under its own name. «مزيج» always works. The built-in lists are the offline default.
5. **Ids are stable:** an item id is derived from its source (`geo-cap-Q1000`: the Wikidata item; `el-sym-79`: the atomic number; `qu-next-12`: the surah number; hand rows have their own names) and never from its position, so a rebuild or a growth of the bank keeps every id. (Detail and the one exception: games/REVIEW.md, "ids".)
6. **Size and memory:** a file is read once into compact arrays; no game holds the whole bank as objects (the pick takes ids by category and level; see the app plan).

## Today's app against these rules (4.3.1)
| Reader | Strict where |
|---|---|
| QuizBankCheck (quiz, «الأسرع», hive questions) | exactly 7 keys per item (`m.size() != 7`), the whole file is rejected otherwise; the file is a bare array with no version |
| KhanatData | refuses an unknown box |
| Missing word, odd-one, charades, word5, hive | check sizes and shapes; to be made tolerant like the rest |
Chips for categories, levels, decks and boxes are hard-coded in every activity.
