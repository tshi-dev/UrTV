# The family games' data: how it is published and what the app enforces

Source of truth: `games/<id>/` on branch work/games-data (questions in `games/data/`). One workflow, **`.github/workflows/quiz-bank.yml`**, publishes everything on the branch **`data-games`** (one orphan commit, force-pushed by this workflow alone): `quiz/quiz.json` (the live quiz and the letter hive's questions, see QUIZ.md) and one compact file per game under `games/`. It **fails closed**: `games/check.py` and `games/check_words.py` first (any problem stops the run, and the branch keeps its last good files), then `make_quiz_bank.py` and `make_games_data.py build`, then `--verify` of every built file (with the app's rules below, and every id of the letter hive checked against the built quiz file), and only then the publish. Tool: `.github/tools/make_games_data.py` (`build`, `starter`, `--verify`); test: `tests/games_data_test.py`.

The app fetches `https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/games/<name>.json` when a game's screen opens and the kept copy is older than 3 days, with no redirects and a read cap equal to the file's limit below, checks the whole file, and only then replaces the kept copy (written beside it and renamed), exactly like `QuizBank`. Any failure: the last good copy stays; with none, the bundled starter in `assets/games/<name>.json` (the same form, so the same code reads it).

## The files, their forms and the limits the app enforces
All text is 1 to 200 characters without control characters (`en` may be empty only where said), every file is a JSON object with `"v": 1` (a file with another version is ignored), every id is `[A-Za-z0-9._:-]{1,64}`, and a build is deterministic (sorted, compact) so the same data is the same bytes.

| File | Form | Size now (gzip) | App limit: bytes, items |
|---|---|---|---|
| `khanat.json` | `{v,L,H,R,B,X,s:{letter:{box:[[ar,en],...]}}}`; each sample at most 24 characters and starts with its letter (after «ال» and the أ إ آ ة ى rules); `en` may be "" for names | 24 KB (9 KB) | 200 KB, 200 to 4,000 samples |
| `missing-word.json` | `{v,w:[{i,k:"f"\|"s",d,a,t:[ar,en],w:[ar,en]}]}`; `t[0]` holds exactly one `___` and is at most 90 characters; `en` may be "" for a saying | 52 KB (10 KB) | 300 KB, 100 to 5,000 items |
| `odd-one.json` | `{v,t:[{i,n:[ar,en],w:[[ar,en,d],...]}]}`; 12 to 100 words a topic, no repeated word | 20 KB (8 KB) | 150 KB, 8 to 200 topics |
| `charades.json` | `{v,c:[category],w:[[ar,en,categoryIndex,d,kids,modes],...]}`; modes bit 1 act, bit 2 describe; no repeated word | 24 KB (8 KB) | 250 KB, 200 to 10,000 words |
| `letter-hive.json` | `{v,L,S,a:{letter:[quiz id]},e:{"A":[quiz id]}}`; `L` is the 28 letters, `S` the letters with fewer than 12 questions (must match the index); every id is in `quiz/quiz.json` | 32 KB (5 KB) | 250 KB, 200 to 12,000 ids |
| `word5-answers.json` | `{v,w:[[word,display,en,d,kids],...]}`; `word` is five letters of the 28-letter alphabet after normalization and `norm(display) == word`; `en` may be "" for dictionary words | 19 KB (5 KB) | 120 KB, 100 to 5,000 words |
| `word5-guesses.json` | `{v,A,n,w}`: `w` is the sorted, unique five-letter words packed as one string, 5 characters each (character k of `a..z01` is the k-th letter of `A`); every answer must be in it | 157 KB (74 KB) | 700 KB, 2,000 to 120,000 words |

**Memory on a 2 GB TV:** the guesses are looked up by **binary search on the packed string** (`w`), so there is no set of 31,000 strings in memory (about 160 KB of characters in all). The other files are small (about 185 KB of JSON for all games without the quiz); parse them with the same `MiniJson` the quiz bank uses.

## The bundled starters (`assets/games/`, so the first round works offline)
Same forms and same verification (`--verify assets/games assets/quiz/starter.json --starter`; the letter hive's ids are checked against the quiz starter).

| Starter | Bytes | What it holds |
|---|---|---|
| `khanat.json` | 13,063 | 5 samples per letter and box |
| `missing-word.json` | 10,218 | 30 facts, 50 sayings |
| `odd-one.json` | 6,058 | 10 topics of 20 or more words |
| `charades.json` | 6,188 | 190 words: 120 for kids to act (kids, easy, act), then 70 others across the levels |
| `letter-hive.json` | 27,606 | 6 questions for each of the 25 best-covered Arabic letters (ظ ذ ض left out) and 4 for each of 25 English letters (X left out); the ids are in the quiz starter or embedded in this file (`T` is the threshold 6 for `S`, `q` holds the 78 embedded compact quiz items), chosen short, easy and kids-friendly first, at most 2 of one kind per letter |
| `word5-answers.json` | 18,863 | all answers |
| `word5-guesses.json` | 12,286 | the dictionary lemmas and the answers (about 2,600 words); offline, a valid inflected form may be refused |
| **Total** | **94,282** | with the quiz starter (57 KB) **151 KB**, under the 300 KB limit (`tests/games_data_test.py` pins it) |

Rebuild after a data change: `python3 .github/tools/make_games_data.py starter <games dir> assets/games assets/quiz/starter.json`.
