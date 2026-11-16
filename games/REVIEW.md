# Review pass of the existing banks (November 2026)

A first reader's pass over every list, by program (`games/tools/review_bank.py`, the builders) and by eye. What was wrong and is fixed, what is doubtful and left for the owner, how ids work. The skim of the doubtful part takes about 10 minutes.

## Numbers now
Quiz bank 1,138 items (geography 707, Islamic 181, science 108, maths and logic 96, Saudi and Gulf 17, animals 19, history 5, food 5; levels 1/2/3 = 156/316/666). Was 1,270: 132 items went out in this pass (below). Word data: «الكلمة المفقودة» 246 facts and 117 sayings; «واحد مو معنا» 32 topics, 692 words; «بدون كلام / وصّف» 714 words; «خانات» 575 samples; «خمّن الكلمة» 447 answers (was 511) and 31,367 accepted guesses.

## Fixed (clear errors)
| What | How many | Fix |
|---|---|---|
| The answer gave itself away: the answer is in the question as whole words («ما عاصمة موناكو؟» answered «موناكو»; Singapore, Djibouti, Tunis, San Marino, Luxembourg, Panama, Guatemala, Najran, Al-Baha, Tabuk, «القمر الطبيعي ... القمر», «الحمص ... الحمّص», the Trench battle, and others) | 27 items | removed (`common.leaks`, applied by `build_all.py`); the capital-name-equals-country cases were not questions at all |
| «في أي قارة تقع جمهورية إفريقيا الوسطى؟» answered «أفريقيا» | 1 | removed with the rest |
| Long official names in options («جمهورية الكونغو الديمقراطية», «ولايات ميكرونيسيا المتحدة», «الإمارات العربية المتحدة», «جمهورية التشيك» ...); odd city labels («غواتيمالا العاصمة», «مدينة بنما») | 18 labels | shortened by a table in `build_geo.py`; currency names too (`build_langcur.py`) |
| Official-language questions for countries with no official language in law (USA, UK, Australia, Mexico, Japan) | 5 items | removed (arguable) |
| Science was 207 of 216 element questions; names of elements by atomic number up to 86 (nobody knows Z = 70) | 148 items | symbols kept up to element 54 and five famous heavier ones, atomic numbers only up to 20 (the ones taught) |
| Currency and language questions were all level 2, also for countries nobody knows | 152 items | level 2 for the 38 well-known countries, level 3 for the rest |
| Words nobody in a family knows (asafoetida, a fig shoot...) or does not want (arsenic, insult, tyrant, gossip, slimming...) among the five-letter answers | 64 words | out of the answers (`OBSCURE` in `build_words2.py`); they are still accepted as guesses. The answers fell from 511 to 447, below the 500 target on purpose |
| The «ال» start of some five-letter answers | earlier | removed |
| Item ids that were a position (charades `chc-<n>`, the repo's sayings `mw-wis-<n>`, curated sayings `mw-cur-<n>`) | 714 + 117 ids | now a hash of the word or the saying, so they survive a reorder (see ids) |

## What the pass looked at and found fine
No question twice; no option twice or an option inside another (checked in normalised spelling); no answer longer than the card; every wrong option the same kind as the answer; no Israel or Western Sahara; Quran counts agree with ar.wikipedia (86 surahs checked, 26 used).

## Ids: how they are assigned and whether they are stable
Quiz items: `geo-cap-Q<Wikidata id>`, `geo-cont-`, `geo-rev-`, `geo-lang-`, `geo-cur-` (the country's Wikidata item), `el-sym-<atomic number>`, `el-bysym-`, `el-name-`, `qu-next-<surah number>`, `qu-num-`, `qu-cnt-`, `an-class-Q<id>`, hand rows by their own name (`is-badr-year`, `hv-frog`), arithmetic by its numbers (`mt-mul-6x7`) and units by a hash of the English question. All of them come from the source or the parameters, never from the position, so a rebuild or a bigger bank keeps every id. Word data: odd-one by topic id and the word, charades `chc-<hash of the word>`, sayings `mw-wis-<hash>` and `mw-cur-<hash>`, facts `mw-geo-cap-...` (they follow the quiz item). The ids that WERE positions are the ones listed above; they changed once, today, and are stable from now on. A removed or reworded item keeps no id: the memory of what was shown simply never meets it again. Rule for new items: an id comes from the source fact or a hash of the normalised text, never from a counter.

## Doubtful, left for the owner (grouped, about 10 minutes)
**A. Politics or law, capitals and currencies** (you decide whether to keep): capital and continent of Libya (Tripoli), Cyprus (Nicosia), Syria (Damascus), Somalia (Mogadishu); Yemen is already left out (two capitals). Currencies of countries that use a foreign money by law: Ecuador, Palau, Micronesia, Marshall Islands (US dollar), Liechtenstein (Swiss franc), Bhutan (the Indian rupee is also used).
**B. Quran verse counts (Hafs), for a scholar:** البقرة 286، النساء 176، المائدة 120، الأنعام 165، الأعراف 206، التوبة 129، يونس 109، هود 123، النحل 128، الإسراء 111، الكهف 110، مريم 98، طه 135، الأنبياء 112، المؤمنون 118، الشعراء 227، الصافات 182، الرحمن 78، العاديات 11، القارعة 11، الهمزة 9، الفيل 5، الماعون 7، الإخلاص 4، الفلق 5، الناس 6. The question says «برواية حفص»; other readings count a few of them differently.
**C. Sayings written from memory (60), the less certain ones first:** القلم أحد اللسانين · الصاحب ساحب · خذ الحكمة من أفواه المجانين · وراء كل عظيم امرأة · من جاور السعيد يسعد · الطمع ضر ما نفع · من جد في الطلب وجد · لا يصلح العطار ما أفسد الدهر · حاميها حراميها · أطعم الفم تستحي العين · التجربة خير برهان · من لا يعمل لا يأكل · مكره أخاك لا بطل · وافق شن طبقة · الإنسان ابن بيئته. The rest are the very common ones.
**D. Regional words in the word games** (known in the Gulf, maybe not elsewhere): هريس، مطبق، جريش، سليق، مضغوطة، حنيني، مطازيز، كليجا، مرقوق، قرصان، عصيدة، ثريد، لقيمات، دلة، ركوة، قرقيعان، عزيمة، شماغ، عقال. Fine for a Saudi family; say if the English side or other regions matter.
**E. Quiz structure, not errors:** geography is 62 percent and five templates (capital, continent, currency, reverse capital, language) are 80 percent of it; Islamic is mostly «أي سورة تأتي بعد ...» (113 of 181); level 3 is 58 percent. The data now carries what is needed to cap a topic's share in a game (see the plan, topic tags); the real answer is the growth plan, not more templates.
**F. Words that appear in two topics** of «واحد مو معنا» (21: حمامة، تونة، فلافل، شاورما، هريس، كنافة، بقلاوة، معلم، رائد فضاء، معطف، دراجة، صاروخ، ثلاجة، مكيف، سكين، مكتبة، حاسوب، شمس، قمر، ضباب، جدة): harmless (a game uses one topic) but the same word in two topics weakens «الواحد المختلف»; a topic list could drop one of each pair.
**G. Charades:** abstract words that are hard to act (time, history, geometry...) are `describe` only only where the category says so (places, countries, landmarks, space); the others allow both modes. If an acting word feels impossible, tell me the word.
