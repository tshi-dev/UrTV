# Verification

## Status

**1450 verified items** in `games/data/` (quiz 1234, true/false 188, riddle 18, estimate 10): geography 896, science 212, Islamic 166, maths and logic 124, Saudi and Gulf 25, animals 18, food 5, history 4. Geography is 62 percent. The target of 1500 was **not reached**. Categories with nothing yet: Arabic language and proverbs, maths and logic, sports, kids-only, riddles, estimation, word game, charades and describe-the-word lists, topics for the odd-one-out game. They are not faked: nothing is shipped that was not checked.

| Category | Kept | Dropped | How checked |
|---|---|---|---|
| geography (capitals, continents, true/false) | 663 | 16 of 193 countries left out (177 kept) | Wikidata (current UN member states with exactly one capital, Arabic and English labels from Wikidata) |
| science (chemical elements, planets, nature) | 143 | 17 elements | each element: Wikidata symbol and atomic number must equal the "symbol X and atomic number N" sentence of its English Wikipedia intro |
| Islamic general knowledge | 75 | 0 | Quran order and name from api.alquran.cloud (Tanzil text); verse counts also require the surah's ar.wikipedia intro to state the same number (86 of 114 surahs agreed, 28 left out); seerah and worship facts from English Wikipedia, with a regular expression that must match the page |
| Saudi and Gulf | 25 | 0 | Wikipedia (city intros), Wikidata |
| animals / food / history | 8 / 5 / 4 | 1 (planet count, second source did not say it) | Wikipedia intro regex |
| maths and logic | 124 | 0 | computed: the script calculates every answer (tables, sums, fractions, percentages, sequences, handshakes, ages, clock angles) and riddles are solved by a search that must find exactly one solution; sources read `computed:<rule>` and are allowed only in this category |
| language, proverbs, sports | 0 | 0 | not started: no reachable page lists Arabic proverbs or sports facts that do not go stale |

Left out on purpose, never asked: Israel and Taiwan (not listed as a country), Palestine and Western Sahara (no entry), countries with more than one capital or a disputed seat (Bolivia, South Africa, Sri Lanka, Eswatini, Ivory Coast, Benin, Tanzania, Malaysia, Switzerland, Montenegro, Netherlands), and Georgia (the name also means a US state). Left out for staleness: current leaders, champions, prices, population, records that change (tallest, longest, largest by area).

## Method

1. Facts come from a page the build actually opened: Wikidata (SPARQL), api.alquran.cloud, and the intro of an English or Arabic Wikipedia article. Builders are in `games/tools/` (`build_all.py` rebuilds everything; it needs network and caches pages).
2. Hand-written items (`rows_islamic.py`, `rows_general.py`) carry `(page, regex)` evidence. The build opens the page and drops the item if the regex is missing; the regex is stored in the item as `evidence`. A numeric, dated or record item needs two different pages that both match, or the build drops it.
3. Generated items (capitals, elements, Quran) are rendered from data, so a fact cannot be mistyped. Wrong options are picked from the same kind of data (a capital next to capitals, a symbol next to symbols) and the checker makes sure none equals the answer.
4. `python3 games/check.py` (and `tests/games_check_test.py`) enforce: schema, unique ids and questions, exactly 3 distinct wrong options in Arabic and English, both languages, question at most 90 Arabic characters, answer at most 28, no `·`, no Israel or Western Sahara, `verified` true, two sources for numeric items.
5. Wording is our own; only facts are taken.

## Residual risk, honestly

- **Wikidata and Wikipedia are not infallible.** Capitals and elements are stable, and elements are cross-checked against a second text, but capitals have only one source (Wikidata, both country and city entries), allowed by the rule for non-numeric facts.
- **Wikipedia intros are English-language evidence for Arabic questions.** The Arabic wording was written by the build author, not reviewed by a native speaker. A native reader should skim every file once before release.
- **Quran verse counts** follow the Hafs count (the question says so); other readings count some surahs differently. Surah names use the common spelling; a scholar should look at the Islamic file once (the owner's note in `notes/CONTENT-REVIEW.md` applies).
- **Wrong options** for countries are other real capitals, but a few Arabic transliterations of small capitals are not the only spelling in use.
- **Rules sheets** (`RULES.md`) rely on rule summaries from search results because the rule sites were not reachable from the build environment; numbers marked [open] or [variant] there are not settled.
- The owner's three links (two online game shops and a Drive file) could not be opened from this environment, so they are not reflected in the Saudi games study.

## Word and game data (second batch)

Files per game, each with a SPEC.md next to it; all pass `python3 games/check_words.py` (`tests/games_words_test.py`).

| Game | Data | Count | How checked |
|---|---|---|---|
| خانات | `khanat/letters.json` | 25 letters, 286 sample answers | Wikidata labels; samples are advisory only (the group decides) |
| واحد مو معنا | `odd-one/topics.json` | 9 topics, 135 words | hand-written lists; a pair is kept only if one Wikidata item carries exactly this Arabic label (or alias) and this English label |
| بدون كلام / وصّف | `charades/words.json` | 192 words | same pair check; blocklist for anything not family-safe |
| الكلمة المفقودة | `missing-word/items.json` | 246 facts, 57 sayings | facts are built from the verified bank; sayings come from `assets/shared/wisdom.json`, which the repo's own tests check (marked `verified: "repo"`, not re-verified against the book by us) |
| خلية الحروف | `letter-hive/index.json` | 16 letters with 8 or more questions | derived from the verified quiz bank; a 5 by 5 board needs 25 letters, so the board is 4 by 4 until the bank grows |
| خمّن الكلمة | `word5/answers.json`, `accepted.json` | 118 answers, 2377 accepted words | answers: topic words or curated common words that are also Wikidata Arabic lexemes; accepted: Wikidata Arabic lexemes (noun, verb, adjective) of five letters in the 28-letter alphabet |
| دومينو | none | | rules only |

Honest limits of this batch:
- Roughly half of the hand-written words did not match a Wikidata item by exact label and were dropped (for example سيارة, طائرة, جزر, خيار: Wikidata spells or labels them differently). The kept words are real; the lists are shorter than planned (odd-one has 9 topics, not 20).
- Wikidata lexemes are a partial dictionary (we asked for 20000 entries): the accepted-guesses list is 2377 words, so some valid Arabic words will be refused. A bigger source (a word list with a licence we can use) would help; none was reachable.
- The word5 answers are family-safe by our choice of topics and a blocklist, but a native reader should skim the lists.
- Wikipedia (and Wikidata) wording for the sayings deck is the repo's; no new proverbs were added because no reachable page lists Arabic proverbs.
- بلوت and جاكارو: notes only (`RULES.md`), no spec, by the owner's decision.

## Rebalance (third batch)

Maths and logic (124) and animal classes (16 new: Wikidata taxonomy puts the species under exactly one of mammals, birds, reptiles, amphibians, fish; unclear ones are dropped) were added to move the bank away from geography. Geography is still 63 percent of the bank. More science, animals, history and Arabic language need more reachable sources (a second encyclopaedia, a proverb list); the builders are ready, only sources are missing. Screens against the specs: `games/SCREENS.md`.

## Second pass (lists widened; words are curated, facts stay verified)

Rule from the supervisor: verification by source is for **facts**; a playable word is not a fact claim. Words are marked `source: "curated"` (hand-written everyday vocabulary, English glosses ours) or carry a Wikidata lexeme link. Facts keep the strict rule.

| Game | Now | Target | Note |
|---|---|---|---|
| خمّن الكلمة | 560 answers (160 curated, 400 dictionary words), 25,718 accepted guesses | 500 / as wide as possible | dictionary answers are Wikidata Arabic noun and adjective lexemes whose word is also an ar.wikipedia article of 3000 bytes or more (a frequency proxy), minus a blocklist (politics, religion, war, vulgar, illness). Accepted guesses: all lexeme lemmas plus all inflected forms of lexemes starting with ا ت ج ح; the query service times out for the other letters, so inflected forms starting with them are not accepted yet. |
| بدون كلام / وصّف | 714 words (level 1: 247, level 2: 244, level 3: 223; 238 for kids) | 600 | curated; sacred places and acts (Kaaba, prayer, Hajj) are left out on purpose |
| واحد مو معنا | 32 topics, 692 words (20 to 24 each) | 30 topics x 20 | curated |
| خانات | 843 sample answers over 8 boxes (animal, plant, object, country, name, food, job, city) | 15 per letter per box | 15 or more only for some letter and box pairs; the language and our lists do not give 15 for every letter. Hard letters are marked in the file. |
| الكلمة المفقودة | 246 facts, 57 repo sayings, 48 curated sayings (351) | 200 sayings | the sayings are 105, not 200: I only added sayings I am sure of; the rest would be guessing |
| خلية الحروف | 28 letters; 19 have 12 or more questions | all 28 at 12 | short letters: ظ 0, ض 2, ذ 2, خ 3, ح 7, ش 8, ص 9, و 10, ط 11 (see `letter-hive/SPEC.md`, "Short letters"). The index now uses the whole bank, including all surah-order questions, element-by-symbol, official-language and currency questions. |

Quiz bank at this point: see the counts in the report; the new generators are `build_langcur.py` (official language and currency, one per country, from Wikidata), element-by-symbol (`build_science.py`) and next-surah for every surah (`build_quran.py`).
Residual risk for this pass: curated words are not source-checked; a native reader must skim `odd-one`, `charades`, `khanat`, `missing-word` and `word5` once.

## Third pass (gaps, publishing)

- **Quiz bank:** 1486 verified items (1270 are 4-option quiz items). New: 17 short-letter facts with page evidence (`rows_hive.py`), surah-by-number for surahs whose name starts with a letter that was short.
- **Letter hive, Arabic, final table (answers starting with the letter):** all 28 letters have 12 or more except خ 8, ذ 5, ض 7, ظ 4, و 11. Where the language has few (ظ, ذ) there is no more to add that has one clear answer. English side (`en_index` in `letter-hive/index.json`): all 26 letters have items; under 12: J 9, Q 6, W 11, X 2, Y 11, Z 10. The SPEC does not promise English play, so this is additive.
- **word5:** 511 answers (152 curated, 359 dictionary words; blocklist widened, words starting with «ال» removed); accepted guesses 31,367 (lemmas of all letters, all forms of lexemes starting with ا ت ج ح, and the forms of the first 2,800 lexeme ids). The rest of the 73,000 lexemes' forms need about 12 hours of Wikidata queries at the service's pace, so they are not in; `lex_forms2.py` resumes from its cache.
- **Sayings:** 117 (57 repo, 60 curated). 150 is not reached: the rest would be guesses.
- **khanat:** the box `city` (world capitals) is new and additive; `saudi_city` now holds only Saudi cities.
- **Reading by a native speaker:** `games/CONTENT-REVIEW.md`.
