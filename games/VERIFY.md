# Verification

## Status: no items shipped yet

The bank is empty on purpose. The session that built this folder could not open any reference page (en/ar Wikipedia, Wikidata, api.alquran.cloud and Britannica were refused by the network policy), and the rule is that an item is kept only when its fact was checked in a source that was actually opened. Writing questions from memory and marking them `verified: true` would be false, so none were written. `games/data/` is ready and `games/check.py` rejects any file with `verified` other than `true`.

| Category | Kept | Dropped |
|---|---|---|
| all | 0 | 0 (nothing attempted) |

## Method (for the session that fills the bank)

1. Draft items per category. Drop at once: anything time-sensitive (current champions or leaders, prices, records that change), ambiguous wording, more than one defensible answer, politics, anything disputed between Islamic schools, fatwa-type questions, Israel or Western Sahara as a country.
2. Open one reliable page per fact (two different sources for every number, date or record; the item says `numeric: true`, and `estimate` items always need two). Quran counts come from api.alquran.cloud (Tanzil text), hadith and verses from the repository's own checked files, never typed by hand.
3. Record the opened URLs in `source`. Set `verified: true` only after that.
4. Wording is our own; only facts are taken.
5. `python3 games/check.py` must print 0 problems: schema, unique ids and questions, exactly 3 distinct wrong options (and distinct from the answer) in both languages, Arabic and English both present, question at most 90 Arabic characters, answers at most 28, no banned text.

## Residual risk

When filled, the risk that remains is a source that is itself wrong, a fact that changes after verification, and Arabic phrasing that reads oddly to a native speaker. A native reader should skim every file once before release.
