# Content review: assets/shared/hadith.json (721 entries)

Checked 2026-11-27. Source: the six books of `fawazahmed0/hadith-api` (the same data `.github/tools/verify_hadith.py`
uses), fetched from raw.githubusercontent.com (Arabic text of the six books, Al-Albani and other graders from the English
editions of the four Sunan). sunnah.com, dorar.net and api.sunnah.com were not reachable from this container.

## Automatic pass

| Check | Result |
|---|---|
| Every entry has `source`, `found` (book + number) and `full` (book, number, whole narration, grades) | 721 / 721 |
| `text` found (letters only) in every book and number listed in `found` | 721 / 721 |
| Books named in `source` are the first ones of `found` | 721 / 721 |
| `full.text` identical to the book's narration | 721 / 721 (Muslim matched by its standard number, see W1) |
| `full.grades` identical to the source | 721 / 721 |
| An entry that rests only on narrations Al-Albani grades weak | 0 |
| Exact duplicate `text` | 0 |
| Shown on the screen after `src/build.py`'s filters | 501 (not shown: 175 isnad too long, 18 too long, 16 al-Tirmidhi grades it weak, 11 no quoted words) |
| Shown narrations where the Prophet ﷺ is not named before the quoted words (companion's words shown as hadith) | 0 |
| Shown narration graded weak by a scholar the build counts, or not accepted by Al-Albani (four Sunan) | 0 |

`found` also lists, for 41 entries, other numbers in the Sunan that Al-Albani grades weak, mauquf or does not grade
(e.g. #30 al-Tirmidhi 2612 Daif, #37 Ibn Majah 2225 Very Daif, #171 al-Tirmidhi 3594 Munkar). They are never shown (the
screen shows only `full`) and every one of these entries also has an accepted narration. No action needed; listed so it is
known.

## Findings (shown on the screen)

| id | problem | proposed fix | source | severity |
|---|---|---|---|---|
| W1: #182, #238, #307, #322, #393, #560 (on screen: #307, #393; the others are filtered out by length) | **Fixed in this branch** (commit «Hadith: the standard (Abd al-Baqi) number…»). `full.book` = صحيح مسلم with a number that is the dataset's running index, not the standard (Abd al-Baqi) number the screen prints: 6929, 7512, 6989, 6855, 6544, 2232. The dataset has no `arabicnumber` for these rows, so `apply_hadith_report.py` fell back to the index. | Print the standard number: #182 → 2732, #307 → 2759, #322 → 2700, #393 → 2565, #560 → 963 (2732 and 963: the next row of the same narration carries 2732.03 / 963.02; 2759, 2700, 2565: the rows around them are numbered 2758.03–2760.01, 2699.02–2701, 2564.03–2565.03); #238 (حديث أبي اليسر) lies between 3005 and 3014, standard 3006, to confirm on sunnah.com. Pipeline fix: when `arabicnumber` is empty, take it from the neighbouring row of the same narration, or skip. | fawazahmed0/hadith-api `ara-muslim` rows 6930 (2732.03), 2233 (963.02); sunnah.com/muslim | wrong |
| #449 | Ibn Majah 4181 «إن لكل دين خلقا…»: Al-Albani Hasan, but Shuaib al-Arnaut **and** Zubair Ali Zai grade it Daif | Keep only if the owner accepts Al-Albani alone; otherwise drop | grades in `full.grades` | doubtful |
| #589 | Ibn Majah 3062 «ماء زمزم لما شرب له»: Al-Albani Sahih, Shuaib al-Arnaut and Zubair Ali Zai Daif | as #449 | `full.grades` | doubtful |
| #60, #92, #109, #112, #117, #142, #143, #176, #183, #185, #189, #249, #258, #289, #296, #321, #331, #332, #343, #369, #370, #382, #398, #405, #424, #428, #431, #437, #466, #474, #497, #501, #536, #545, #546, #549, #568, #570, #608, #653, #660, #663 | Shown with Al-Albani's grade (Sahih/Hasan), but Zubair Ali Zai grades this narration Daif (the build does not count him). Al-Albani's grade is the project rule, so this is information, not an error. | None unless the owner wants a stricter rule | `full.grades` | doubtful (low) |
| #117 | Abu Dawud 236 (wet dreams and ghusl, «النساء شقائق الرجال»): the whole narration is shown on a family TV screen; also graded Daif by Zubair Ali Zai | Drop from the screen list, or keep only the short `text` | Abu Dawud 236 | doubtful (fit) |
| #436 | Bukhari 5630: «…وإذا بال أحدكم فلا يمسح ذكره بيمينه…» on a family screen | Owner's call: keep or drop | Bukhari 5630 | style (fit) |
| #126 | Bukhari 6474 «ما بين لحييه وما بين رجليه» | Owner's call | Bukhari 6474 | style (fit) |
| #478 | Muslim 976: the Prophet ﷺ not permitted to ask forgiveness for his mother; a sensitive point shown without explanation | Drop from the screen list, or keep the short `text` «فزوروا القبور فإنها تذكر الموت» only | Muslim 976 | doubtful (sensitive) |
| #619 | Ibn Majah 3523: the Prophet's quoted words are only «نعم»; the ruqya in `text` is Jibril's words, shown small as narration | Fine as a hadith; owner may prefer Muslim 2186 (same ruqya) or drop | Ibn Majah 3523 | wording |
| #28 / #658 | Near-duplicates: «إن من أحبكم إلي أحسنكم أخلاقا» and «إن من خياركم أحسنكم أخلاقا» (different Bukhari numbers, both shown) | Keep one | Bukhari 3759, 3559 | style |
| #4 / #107 | #107 «فلا يؤذي جاره» is the first sentence of #4 (Bukhari 6018), both shown | Keep #4 | Bukhari 6018, 5185 | style |
| #28, #29, #48 | The shown narration carries an unrelated second part (the four Quran teachers; «وإذا مات صاحبكم فدعوه»; the names يسار/رباح/نجيح/أفلح) | Owner's call; the text is the book's, so only dropping or choosing another narration is possible | Bukhari 3759, Tirmidhi 3895, Muslim 2137 | style |

Not verified: the grades of al-Tirmidhi's own wording come from the book text; sunnah.com and dorar.net were not reachable,
so no grade was cross-checked outside the dataset.

## للمالك: أربعة أحاديث تحتاج قرارك

الدرجات من بيانات المشروع نفسها (`full.grades`). وسبب التضعيف مأخوذ من الراوي الظاهر في الإسناد كما يذكره أهل الجرح
(تقريب التهذيب لابن حجر)، ولم أتحقق منه من موقع حديثي في هذه الجلسة.

| # | أول الحديث | المصدر | الدرجة | من ضعّفه، ولماذا | المقترح |
|---|---|---|---|---|---|
| 449 | «إن لكل دين خلقًا، وخلق الإسلام الحياء» | سنن ابن ماجه ٤١٨١ | حسّنه الألباني | ضعّفه شعيب الأرنؤوط وزبير علي زئي؛ في إسناده معاوية بن يحيى (الصدفي)، وهو ضعيف عند أهل الجرح | يُحذف من الشاشة، أو يبقى مع «حسّنه الألباني، وضعّفه غيره» |
| 589 | «ماء زمزم لما شُرب له» | سنن ابن ماجه ٣٠٦٢ | صحّحه الألباني | ضعّفه شعيب الأرنؤوط وزبير علي زئي؛ في إسناده عبد الله بن المؤمّل، وهو ضعيف، وأبو الزبير مدلّس وقد عنعن (الإسناد هنا «سمعت»، فيبقى الضعف من ابن المؤمّل) | يبقى مع الملاحظة نفسها، فالخلاف فيه مشهور؛ أو يُحذف إن أردتَ ما لا خلاف فيه |
| 117 | «سُئل رسول الله ﷺ عن الرجل يجد البلل… إنما النساء شقائق الرجال» | سنن أبي داود ٢٣٦ | حسّنه الألباني، وحسّنه الأرنؤوط لغيره | ضعّفه زبير علي زئي؛ في إسناده عبد الله بن عمر العُمَري، وهو ضعيف من قِبل حفظه | يُحذف من الشاشة: موضوعه (الاحتلام والغسل) لا يناسب شاشة العائلة، وإن كان الحديث مقبولًا |
| 478 | «استأذنتُ ربي أن أستغفر لها فلم يؤذن لي… فزوروا القبور فإنها تذكّر الموت» | صحيح مسلم ٩٧٦ | صحيح (مسلم) | لا تضعيف؛ الملاحظة في المناسبة فقط: يذكر أمّ النبي ﷺ ومنع الاستغفار لها دون شرح | يُحذف من الشاشة، أو يُعرض المقطع «فزوروا القبور فإنها تذكّر الموت» وحده |
