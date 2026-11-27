# Content review: summary

Review of every content item the TV shows, 2026-11-27, branch `work/44-content-review` (from `work/44-beta4`). Golden
rule: no fact without a source; nothing changed silently. **No content was changed in this pass**: every fix is a
proposal for the owner (the public banks are built from `work/games-data`, so a fix there is the only one that lasts).

## Per bank

| Bank | Items | Checked against | Wrong | Doubtful | Wording | Style | Report |
|---|---|---|---|---|---|---|---|
| verses.json | 500 | api.alquran.cloud (simple, uthmani, muyassar), letter by letter | 0 | 0 | 0 | 0 | [verses.md](verses.md) |
| hadith.json | 721 (501 shown) | the six books + graders (fawazahmed0/hadith-api) | 6 | 4 (+42 low) | 1 | 6 | [hadith.md](hadith.md) |
| quiz bank.json | 1134 | Wikidata live (703 geography), alquran.cloud (160), Wikidata (100 elements), recomputed (96), read (76) | 1 | 13 | 10 groups | 5 groups | [quiz.md](quiz.md) |
| quiz starter.json | 210 | the bank | — | 1 (stale) | — | — | [quiz.md](quiz.md) |
| open game banks | charades 190, khanat ~850 samples, letter-hive 78, missing-word 80, odd-one 213, word5 511 | read; Wikipedia where cited | 3 | 10 | 11 | 7 | [games.md](games.md) |
| encrypted banks (.gb) | author 129, closest 133, emoji 151, guess 22 topics, liar 82, link 348, logic 25 | opened in memory, equal to the private sources; read, recomputed | 0 | 4 | 17 | 7 | private repo only (`review/encrypted-banks.md` on `work/content-review` in urtv-games-content) |
| private packs q01–q08 + qbank | 4077 + 3 | review in progress | … | … | … | … | private repo only (`review/packs-q01..q08.md`) |
| urtv-data `content/` | — | **not reviewed**: adding the repository to this session was refused | — | — | — | — | — |

## Ranked: fix first

1. **hadith W1** (wrong, on screen): six hadith from Sahih Muslim show a running index instead of the standard number
   (e.g. «صحيح مسلم، ٦٩٨٩» for 2759). Fix the numbers and the fallback in `apply_hadith_report.py`.
2. **khanat K1** (wrong): «ألمانيا» under م and «ألوان» under و: the letter rule of the game is broken.
3. **odd-one `family`** (wrong): «صديق، جار، ضيف، معلم، عريس، عروس» counted as family; rounds become disputable.
4. **quiz geo-cap-Q233** (wrong label): Malta's capital shown as «البلد».
5. **quiz hv-dhuhr** (two right answers): «الجمعة» is also a prayer between Fajr and Asr.
6. **quiz continents G1** (two right answers, arguably): Africa as a wrong option for Spain, France, Italy; South America for Panama.
7. **hadith #449, #589** (doubtful): Ibn Majah narrations Al-Albani accepts but Shuaib al-Arnaut and Zubair Ali Zai grade weak.
8. **hadith #117, #478** (fit/sensitive on a family screen): drop from the screen list or show only the short text.
9. **Outdated or moving facts**: Indonesia's capital, Equatorial Guinea's capital, Nauru, Venezuela's bolívar, Ethiopia's and Niger's languages (the quiz and the letter hive disagree on Niger).
10. **quiz starter.json is stale**: 23 items no longer in the bank (some with the answer in the question); rebuild.
11. **Palestine** is in the guess game's Arab countries but not in charades and odd-one: an owner decision, made once for all games.
12. Wording: grammar (hi-giza «يقع», sg-oryx «ذي»), element names without the article, currency and capital labels, English glosses (بلبل، نسر، صرصور، نقانق).

## What was not verified, and why
- The exact QPC Hafs marks of `uthmani` need api.quran.com (blocked here); CI's `quran-verify` covers them.
- Hadith grades were not cross-checked outside the dataset (sunnah.com, dorar.net blocked here).
- `tshi-dev/urtv-data` (`content/`: seasonal, sa-school, announcements, prayer-adjustments, devices): not reachable from this session.
- `qbank/reviewed*/` working files and packs q09–q17 of urtv-games-content were outside the asked scope (q01–q08 + qbank's compiled `bank-extra.json`).
