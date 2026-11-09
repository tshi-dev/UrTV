# Verification

## Status

**923 verified items** in `games/data/` (quiz 735, true/false 188). The target of 1500 was **not reached**. Categories with nothing yet: Arabic language and proverbs, maths and logic, sports, kids-only, riddles, estimation, word game, charades and describe-the-word lists, topics for the odd-one-out game. They are not faked: nothing is shipped that was not checked.

| Category | Kept | Dropped | How checked |
|---|---|---|---|
| geography (capitals, continents, true/false) | 663 | 16 of 193 countries left out (177 kept) | Wikidata (current UN member states with exactly one capital, Arabic and English labels from Wikidata) |
| science (chemical elements, planets, nature) | 143 | 17 elements | each element: Wikidata symbol and atomic number must equal the "symbol X and atomic number N" sentence of its English Wikipedia intro |
| Islamic general knowledge | 75 | 0 | Quran order and name from api.alquran.cloud (Tanzil text); verse counts also require the surah's ar.wikipedia intro to state the same number (86 of 114 surahs agreed, 28 left out); seerah and worship facts from English Wikipedia, with a regular expression that must match the page |
| Saudi and Gulf | 25 | 0 | Wikipedia (city intros), Wikidata |
| animals / food / history | 8 / 5 / 4 | 1 (planet count, second source did not say it) | Wikipedia intro regex |
| maths and logic, language, proverbs, sports, kids | 0 | 0 | not started |

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
