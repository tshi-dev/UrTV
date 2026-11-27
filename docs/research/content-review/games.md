# Content review: open game banks in assets/games/

Checked 2026-11-27: charades (190 words), khanat (25 letters × up to 9 boxes, 13 KB), letter-hive (78 embedded quiz
items plus ids in the quiz starter), missing-word (30 facts, 50 sayings), odd-one (10 topics, 213 words), word5-answers
(511), word5-guesses (spot check only: a dictionary of forms), meta. These are the bundled starters; the source of truth
is `games/` on branch `work/games-data` (`make_games_data.py starter`), so **fixes go there**. Nothing was changed.

## Automatic pass
- Forms, ids, limits: the files pass the project's own check (`tests/games_data_test.py` pins them).
- `meta.json` charades counts (`n`, `kids`) describe the full published set, not the 190-word starter: expected, not a finding.
- Khanat: every sample starts with its letter after the documented rules, **except two** (K1).
- Letter-hive items: same findings as the quiz bank (quiz.md) where shared.

## Findings

| id | problem | proposed fix | source | severity |
|---|---|---|---|---|
| K1 khanat `م/country` | «ألمانيا» does not start with م | Replace with «موريشيوس» or «مالي» | — | wrong |
| K1 khanat `و/object` | «ألوان» does not start with و | Replace with «وعاء» | — | wrong |
| odd-one `family` | «صديق، جار، ضيف، معلم» (d3), «عريس، عروس» are listed as members of «العائلة»; in an odd-one-out round a friend or a teacher is then «not the odd one», which players will dispute | Move them out (or rename the topic «ناس حولنا») | — | wrong |
| odd-one `clothes` | «نظارة، ساعة» (accessories), «كم» (a part of a garment) as clothes | Drop or move to an «إكسسوارات» topic | — | doubtful |
| odd-one `farm` | «قطة، كلب، حمامة» as farm animals | Drop, or keep only «كلب» | — | doubtful |
| odd-one `dishes` «أكلات عربية» | «برياني» is South Asian (common in the Gulf, but not Arab in origin) | Drop or rename the topic «أكلات» | en.wikipedia.org/wiki/Biryani | doubtful |
| charades + odd-one `arab_countries` | 21 of the 22 Arab League members; Palestine is missing (the quiz leaves it out on purpose, VERIFY.md) | Owner's decision; players will notice its absence | arableagueonline.org members list | doubtful (sensitive) |
| khanat `ن/saudi_city` | «نيوم» is a project and region, not a city | Replace with «النماص» | en.wikipedia.org/wiki/Neom | doubtful |
| khanat `غ/object`, `ق/object` | «الغميضة» (a game), «قفز الحبل» (an activity) are not objects | «غطاء», «قفل» | — | wording |
| khanat `ا/country`, `ج/animal` | «الإمارات» and «الإمارات العربية المتحدة» both; «جراد» and «جرادة» both | Keep one of each | — | style (duplicate) |
| khanat `م/job` | «محامي» | «محامٍ» (the word5 list keeps «محامي» because it needs five letters) | — | wording |
| charades `body`, odd-one `body` | «رجل» (leg) reads first as «رَجُل» (man) | «رِجْل» | — | wording |
| charades `kitchen` | «قدر» (pot) reads as «قَدْر» | «قِدْر» | — | wording |
| charades + odd-one `birds` | English gloss «بلبل = nightingale»: the bulbul is another bird (nightingale = «عندليب») | gloss «bulbul» | en.wikipedia.org/wiki/Bulbul | wording (gloss) |
| charades + odd-one `birds` | «نسر = eagle»: in Arabic نسر is the vulture (eagle = «عقاب»); the English gloss follows popular usage | gloss «vulture» or word «عقاب» | ar.wikipedia.org/wiki/نسر | wording (gloss) |
| charades `landmarks` | «برج المملكة = Kingdom Tower»: «Kingdom Tower» is the former name of Jeddah Tower; Riyadh's tower is «Kingdom Centre» | gloss «Kingdom Centre» | en.wikipedia.org/wiki/Kingdom_Centre | wording (gloss) |
| odd-one `food`, word5 | «نقانق = hot dog» | gloss «sausages» | — | wording (gloss) |
| word5 | «صرصور = cricket»: صرصور is usually the cockroach («صرصار الليل» = cricket) | gloss «cockroach» | — | wording (gloss) |
| charades `dishes` | «سليق، حنيني، مطازيز، مضغوطة، كليجا» are level 1 for kids to act: regional Saudi dishes many children cannot mime | Level 2 or describe-only | — | style (level) |
| missing-word facts (30) | All 30 bundled facts are element symbols, with «لعنصر هيدروجين» (no article) | Mix the facts; «لعنصر الهيدروجين» | — | wording / style |
| missing-word `mw-cur-26` | «اسأل مجرب ولا تسأل طبيبا»: the common form ends «حكيمًا» | «حكيمًا» | — | wording |
| missing-word sayings | 117 sayings in the full set, 60 «written from memory» (games/CONTENT-REVIEW.md); the 50 bundled are all well-known and correctly worded except mw-cur-26 | Add a source (a proverbs collection) per saying in the source data | — | doubtful (unsourced) |
| word5 answers | Rare or dictionary-only words as level 2 answers: «زغاية، ساجور، تناضح، كناشة، مشاية، ملحون، مرسين، مقروض»; many rare level 3 words («دغموس، كرسوع، قطمير، علجوم، حرقدة، خرشنة، دركاه، سليخة، شرشور، هليلج») | Level 3 at least, or drop the rarest | — | style (level) |
| word5 answers | Grim or charged words in a family game: «مشرحة، مشيمة، طاغية، إهانة، أمركة، مسكون، تخدير» | Owner's call | — | doubtful (fit, low) |
| letter-hive `geo-lang-Q1032` vs quiz `geo-lang-Q1032` | Niger's language: the letter-hive item says «الهوسية», the quiz bank says «الفرنسية»; since the 2025 charter Hausa is the national language and French a working language | Ask «اللغة الوطنية» with Hausa, or drop both | en.wikipedia.org/wiki/Languages_of_Niger | doubtful (inconsistent) |
| letter-hive `geo-cap-Q697` «ضاحية يارين», `geo-cap-Q252` | Same as quiz.md (Nauru has no official capital; Jakarta/Nusantara) | as quiz.md | — | doubtful |
| letter-hive `qu-next-55` | Wrong option «الرحمن» is the surah named in the question | another surah | — | style |
| letter-hive `sc-moon` | «ما القمر الطبيعي الوحيد للأرض؟» → «القمر»: the answer is in the question | Drop | — | style (give-away) |
| letter-hive `hv-autumn`, `is-hira` | as quiz.md | as quiz.md | — | style |

Counted: wrong 3, doubtful 10, wording 11, style 7 (by group).
