# Content review: assets/quiz/bank.json (1134) and assets/quiz/starter.json (210)

Checked 2026-11-27. The bank is built from `games/data/quiz.json` (branch `work/games-data`), where every item carries its
`source` (Wikidata, api.alquran.cloud, Wikipedia, or `computed:`) and, for hand-written items, an `evidence` regex.
**Any fix below has to be made there** (`games/tools/`, then `make_quiz_bank.py`); editing `assets/quiz/*.json` alone is
overwritten by the next build. Nothing was changed in this pass.

## Automatic pass

| Check | Result |
|---|---|
| Format (keys, 3 wrong options, both languages), unique ids, unique questions | 1134 / 1134 |
| Same option twice | 0 |
| Every item has a source in the source file | 1134 / 1134 |
| Geography (703): answer re-checked live against Wikidata (query.wikidata.org): capital P36, continent P30, currency P38, official language P37, current statements only | 703 / 703 current; 0 answer not current |
| Geography: a wrong option that Wikidata also gives for the country | 4 items (G1) |
| Quran (160): next surah, surah by number, verse count, 114, against api.alquran.cloud | 160 / 160 |
| Elements (100): symbol and atomic number against Wikidata (P246, P1086) | 100 / 100 |
| Arithmetic and sequences (96): recomputed | 96 / 96 |
| Facts and animal classes (76): read one by one, below | see table |
| `starter.json` against the bank | 23 starter items are no longer in the bank, 32 differ (old labels or level): S1 |

## Findings

| id | problem | proposed fix | source | severity |
|---|---|---|---|---|
| G1 geo-cont-Q29 | Spain: «آسيا» is not an issue, but «أفريقيا» is a wrong option while Spain has Ceuta, Melilla and the Canaries in Africa (Wikidata P30 lists Africa) | Replace the option with «أمريكا الجنوبية» or «أوقيانوسيا» | wikidata Q29 P30 | doubtful |
| G1 geo-cont-Q142 | France: «أفريقيا» as wrong option (Réunion, Mayotte; Wikidata lists Africa) | Replace the option | wikidata Q142 P30 | doubtful |
| G1 geo-cont-Q38 | Italy: «أفريقيا» as wrong option (Lampedusa; Wikidata lists Africa) | Replace the option | wikidata Q38 P30 | doubtful (low) |
| G1 geo-cont-Q804 | Panama: «أمريكا الجنوبية» as wrong option; Panama is often counted as transcontinental (Wikidata lists both) | Replace the option | wikidata Q804 P30 | doubtful |
| geo-cap-Q252 | Indonesia: Wikidata now lists two current capitals, Jakarta and Nusantara (Q110601629); the move is under way | Keep Jakarta but watch, or drop until the move is settled | wikidata Q252 P36 | doubtful (outdated risk) |
| geo-cap-Q983, geo-rev-Q983 (Equatorial Guinea) | Answer «سيوداد دي لا باز»: a capital moved from Malabo very recently; most players and many sources still say Malabo (Malabo is not among the options) | Consider dropping until the move is settled in the sources | wikidata Q983 P36 | doubtful (outdated risk) |
| geo-cap-Q697 (Nauru) | «يارين»: Nauru has no official capital; Yaren is the seat of government | Drop, or ask «مقر الحكومة» | en.wikipedia.org/wiki/Yaren_District | doubtful |
| geo-cur-Q717 (Venezuela) | «بوليفار السيادي»: redenominated in Oct 2021 as the «بوليفار الرقمي» (VED) | Answer «بوليفار فنزويلي» (stays true) | en.wikipedia.org/wiki/Venezuelan_bolívar | doubtful (outdated) |
| geo-lang-Q115 (Ethiopia) | «الأمهرية» only; since 2020 Ethiopia has five federal working languages (Afaan Oromo, Tigrinya, Somali, Afar added) | Ask «اللغة الرسمية الأكثر استعمالًا في الحكومة» or drop | en.wikipedia.org/wiki/Languages_of_Ethiopia | doubtful |
| geo-lang-Q819 | Laos: answer «لاو» is the country's name in the question | «اللاوية» | — | wording |
| geo-cap-Q233 (Malta) | Answer «البلد»: an unusual Arabic label for Valletta; players know «فاليتا» | «فاليتا» | ar.wikipedia.org/wiki/فاليتا | wrong (label) |
| geo-cap-Q215 (Slovenia), geo-cap-Q221 (N. Macedonia) | «لبلانة», «إسكوبية»: rare forms; ar.wikipedia uses «ليوبليانا», «سكوبيه» | Use the ar.wikipedia title | ar.wikipedia | wording |
| geo-cur (labels) | Odd currency labels: «لو ملداوي» (ليو مولدوفي), «تنك قزاقستاني» (تينغ كازاخستاني), «كوردبا نيكاراغوا» (كوردوبا نيكاراغوي), «جوردة هايتية» (غورد هايتي), «بوتسوانا بولا» (بولا بوتسواني), «بانجا تونجي» (بانغا تونغي), «مثقال موزنبيقي» (موزمبيقي, the bank writes the country «موزمبيق») | as in brackets | ar.wikipedia | wording |
| geo-cap / geo-cur / geo-lang (level) | Level 3 for very easy items: China=Beijing, Norway=Oslo, Portugal=Lisbon, Ireland=Dublin, Sweden, Austria, Belgium, Thailand, Philippines, Kenya; every currency and every language item is level 3 | Level 2 for capitals of well-known countries; review the rule that sets the level | — | style (level) |
| hi-giza | «في أي دولة **تقع** الهرم الأكبر»: الهرم is masculine | «يقع» | — | wording (grammar) |
| sg-oryx | «ما اسم الظبي الأبيض **ذو** القرنين»: genitive after «اسم» | «ذي القرنين» | — | wording (grammar) |
| el-sym-*, el-bysym-* (84) | «ما الرمز الكيميائي لعنصر هيدروجين؟», answers «هيدروجين»: no article | «لعنصر الهيدروجين», answer «الهيدروجين» | — | wording |
| an-class-* (10) | «إلى أي مجموعة ينتمي حيوان أسد؟» awkward; an-class-Q152 «حيوان سمكة» → answer «أسماك» gives itself away | «إلى أي مجموعة ينتمي الأسد؟»; drop Q152 | — | wording |
| hv-dhuhr | Wrong option «الجمعة» is also a right answer on Fridays (Jumu'ah is prayed between Fajr and Asr, in place of Dhuhr) | Replace with «الوتر» or «التراويح» | en.wikipedia.org/wiki/Friday_prayer | doubtful (two right answers) |
| sg-camel-hump | «الجمل العربي (الهجين)»: «الهجين» is a racing camel (or a cross), not the name of the species | «الجمل العربي» | en.wikipedia.org/wiki/Dromedary | wording |
| hv-khwarizmi | Ibn Sina also wrote on mathematics and astronomy; the question leans on «الجغرافيا» alone | «من العالم الذي اشتُقّت من اسمه كلمة "خوارزمية"؟» | en.wikipedia.org/wiki/Algorithm | doubtful (low) |
| gk-everest | Option «مونت بلانك»: Arabic is «مون بلان» | «مون بلان» | ar.wikipedia | wording |
| hv-autumn | Options «الصيف» and «الشتاء» are named in the question, so they are not real choices | «الربيع / الموسم المطير / ...» or other seasons' words | — | style (give-away) |
| is-hira | Options «جبل أحد / جبل عرفات» are not caves | «غار ثور» plus two other caves, or ask «في أي جبل» | — | style (give-away) |
| mt-seq-pow10 | Options 10001, 10004, 10006 for 10000: never plausible | 1100, 9000, 100000 | computed | style (give-away) |
| mt-sides-octagon | English «a octagon» | «an octagon» | — | wording (typo) |
| English answers | 130 English answers start lower case («hydrogen», «euro», «mammals») | Capitalise | — | style |
| S1 starter.json | Stale: 23 items no longer in the bank, among them answers inside the question (geo-cap-Q334 Singapore, geo-cap-Q817 Kuwait, sg-bahah, sg-najran, sg-tabuk) and two continent items with «أوروبا» as a wrong option for Armenia and Azerbaijan (geo-cont-Q399, geo-cont-Q227: transcontinental); 32 items carry old labels («واشنطن العاصمة», «دولار للولايات المتحدة») or levels | Rebuild with `make_quiz_bank.py … --starter` from the current bank | docs/QUIZ.md | doubtful |

Counted: checked 1134 + 210; wrong 1; doubtful 13 (G1 ×4, Indonesia, Equatorial Guinea, Nauru, Venezuela, Ethiopia, hv-dhuhr,
hv-khwarizmi, S1, Italy low); wording 10 groups; style 5 groups.
