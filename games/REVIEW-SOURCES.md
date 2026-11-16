# The doubtful list, checked against open sources (4.3.3)

The doubtful items of `games/REVIEW.md` (groups A to G) were checked against open data instead of memory. `games/tools/verify_sources.py quran|geo` repeats groups A and B (it downloads the sources into a cache folder; nothing downloaded is kept in the repository). Nothing is copied from these sources into the games: they are only compared with.

## Sources
| Source | Licence | Used for | Result |
|---|---|---|---|
| Tanzil.info `quran-data.xml` (metadata: surah order, names, verse counts), read from a GitHub mirror (tanzil.net itself is not reachable from the build machine) | CC BY 3.0 (the file's own header) | group B: all 159 surah items (next surah, surah by number, verse count, Hafs) | 0 differ |
| mledoze/countries (`countries.json`) | ODbL 1.0 | group A: capital, continent, official language and currency of every country item (703 matched) | 12 fixed or dropped, see below, 0 differ now |
| ar.wikipedia.org (article titles and first paragraph, API) | CC BY-SA 4.0 | group D: the regional words | all 19 exist and mean what the games use them for |
| GitHub code search over public repositories (the OpenITI classical corpus, `OpenArabicPE`, `GT-hikam` and others) | each repository's own (mostly open corpora; the licence of what was found is not reused) | group C: does the saying exist in this wording | 111 of 117 sayings found in 3 or more places; the other 6 handled below |
| The ISO 4217 list (six-group.com) and Arabic Wiktionary | not reachable from the build machine | the currency names were checked through mledoze (which carries ISO 4217 names); Wiktionary was not needed after the Wikipedia check | none |

## A. Capitals, continents, languages, currencies (703 items)
Kept as verified: capital and continent of Libya, Syria, Somalia (all agree with the dataset); the currencies of Ecuador, Palau, Micronesia, the Marshall Islands (US dollar), Liechtenstein (Swiss franc), Bhutan (ngultrum, the rupee is also used), Cambodia (riel), the Bahamas, Brunei (the dollar of each; the Singapore dollar is also accepted in Brunei).
Fixed: Argentina «Argentine peso» (the convertible peso ended in 2002), Peru «Peruvian sol» (the «nuevo sol» was renamed in 2015), Vanuatu «Vanuatu vatu», Turkmenistan «Turkmenistan manat», Mexico «Mexican peso», Ethiopia «birr», Nauru's capital «Yaren» (the dataset's name; Nauru has no capital in law), Niger's official language is French (not Hausa, which is a national language).
Dropped (disputed or two continents, so the question has no single answer): Moldova's official language (its name is a political question), and the continent of Cyprus, Armenia and Azerbaijan. Cyprus's capital Nicosia stays (every source agrees).
Name variants that agree with the dataset and stay: Persian and Persian (Farsi), Bangla and Bengali, pound sterling and British pound, renminbi and Chinese yuan, sovereign bolivar, Reykjavík. A list of these is `SAME` in the tool.

## B. Quran verse counts (Hafs)
All 159 surah items agree with the Tanzil metadata (`ayas` is the Hafs count, the Basmala not counted). The scholar's check can be skipped; the question says «برواية حفص».

## C. Sayings (117 in the missing-word game: 57 from the app's wisdom list, 60 written from memory)
A saying counts as found when its exact wording appears in three or more files of public repositories other than this one. 111 do. The six that did not, and what was done:
- «الباب الذي يأتيك منه الريح سدّه واستَرِح»: the attested wording is «الباب الذي تأتي منه الريح»; changed to that.
- «مد رجليك على قدر لحافك»: attested as «على قدر لحافك مدّ رجليك»; the item is now «على قدر لحافك مدّ ___» (رجليك).
- «اسأل مجرب ولا تسأل طبيبًا»: attested as «اسأل مجرِّبًا ولا تسأل طبيبًا»; the endings are written.
- «كثرة الطباخين تحرق الطبيخ»: the common wording is «تحرق الطبخة» (8 sources); the answer is «الطبخة» and «الطبيخ» is still accepted.
- «جاء يكحّلها فأعماها» (found in the classical corpus) and «من جدّ في الطلب وجد» (found in a collection of wisdom sayings): kept, one source each, the wording is the classical one.
The less certain ones of the first list that are in this game are attested 115 times («القلم أحد اللسانين»), 19 («خذ الحكمة من أفواه المجانين»), 15 («من جاور السعيد يسعد»), 13 («وراء كل عظيم امرأة»), 5 («الطمع ضر ما نفع») and 3 («لا يصلح العطار ما أفسد الدهر»).
The 57 sayings of `assets/shared/wisdom.json` are shared with the home screen's wisdom card; if one wording there should change, it changes there (it is not touched here).

## D. Regional words
All nineteen are articles on ar.wikipedia (هريس، مطبق، جريش، سليق، مضغوط، حنيني، مطازيز، كليجا، مرقوق، قرصان، عصيدة، ثريد، لقيمات، دلة، ركوة، قرقيعان، عزيمة، شماغ، عقال), the dish and cloth ones described as Saudi or Gulf. Kept. «قرصان» is also the word for a pirate (an ordinary word, fine in «خمّن الكلمة»). The English side of the regional words is a transliteration plus a plain description; no source for other regions was needed.

## E, F, G (structure, not facts)
E (geography 62 percent, five templates): no source can settle it; the topic share cap and the growth plan are the answer. F (21 words in two topics of «واحد مو معنا»): harmless, a game uses one topic. G (words hard to act): a matter of the families' taste, not a source. They stay as they are.
