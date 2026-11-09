# Verification

## Status

**1057 verified items** in `games/data/` (quiz 841, true/false 188, riddle 18, estimate 10): geography 663, science 143, maths and logic 124, Islamic 75, Saudi and Gulf 25, animals 18, food 5, history 4. The target of 1500 was **not reached**. Categories with nothing yet: Arabic language and proverbs, maths and logic, sports, kids-only, riddles, estimation, word game, charades and describe-the-word lists, topics for the odd-one-out game. They are not faked: nothing is shipped that was not checked.

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
