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
| private packs q01–q08 + qbank | 4077 + 3 (bank-extra) | read item by item (8 reviewers, one per pack), cited ar.wikipedia opened where in doubt, computed items recomputed | 19 | 136 | 295 | 625 | private repo only (`review/packs-q01..q08.md`) |
| urtv-data `content/` | — | **not reviewed**: adding the repository, and then a read-only clone, were both refused by this session's permission check | — | — | — | — | — |

## Ranked: fix first

1. **hadith W1** (wrong; two on screen): six Sahih Muslim narrations carried a running index instead of the standard number
   (e.g. «صحيح مسلم، ٦٩٨٩» for 2759). **Fixed on this branch**, with the cause: `verify_hadith.py` / `apply_hadith_report.py`
   never print the index again (`tests/hadith_number_test.py`).
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
12. **Private packs**: 19 wrong items (two impossible triangles, two right answers, a country given as a company name, blood
    group O «لا تحتوي على أضداد», …) and 136 doubtful; by id in the private repo (`review/DECISIONS.md`), never here.
13. Wording: grammar (hi-giza «يقع», sg-oryx «ذي»), element names without the article, currency and capital labels, English glosses (بلبل، نسر، صرصور، نقانق).

## What was not verified, and why
- The exact QPC Hafs marks of `uthmani` need api.quran.com (blocked here); CI's `quran-verify` covers them.
- Hadith grades were not cross-checked outside the dataset (sunnah.com, dorar.net blocked here).
- `tshi-dev/urtv-data` (`content/`: seasonal, sa-school, announcements, prayer-adjustments, devices): this session was refused access (add_repo, then a read-only clone); a session with access must do it.
- The pack reviewers checked most facts from knowledge and opened the cited article only where in doubt (about 25 per pack); every «not verified» row in the private reports says so.
- `qbank/reviewed*/` working files and packs q09–q17 of urtv-games-content were outside the asked scope (q01–q08 + qbank's compiled `bank-extra.json`).

## قائمة قرارات المالك (البنوك العامة)

كل تغيير لإجابة، وكل حذف، وكل بند مشكوك فيه، بمعرّفه، مع المقترح. ما نُفّذ فعلًا في هذا الفرع: أرقام صحيح مسلم الستة فقط
(تصحيح مصدر، لا معنى). بنود الحزم الخاصة والبنوك المشفرة في المستودع الخاص (`review/DECISIONS.md` في urtv-games-content).

### الحديث (assets/shared/hadith.json)
| المعرّف | القرار المطلوب | المقترح |
|---|---|---|
| #182، #238، #307، #322، #393، #560 | رقم صحيح مسلم | **نُفّذ**: الترقيم المعتمد (٢٧٣٢، ٣٠٠٦، ٢٧٥٩، ٢٧٠٠، ٢٥٦٥، ٩٦٣) |
| #449 «إن لكل دين خلقًا» | حسّنه الألباني، وضعّفه الأرنؤوط وزبير علي زئي | حذف من الشاشة، أو إبقاء مع ملاحظة الخلاف |
| #589 «ماء زمزم لما شُرب له» | صحّحه الألباني، وضعّفه الأرنؤوط وزبير علي زئي | إبقاء مع ملاحظة الخلاف، أو حذف |
| #117 «النساء شقائق الرجال» | الموضوع لا يناسب شاشة العائلة | حذف من الشاشة |
| #478 «فزوروا القبور» | يذكر منع الاستغفار لأم النبي ﷺ بلا شرح | حذف، أو عرض «فزوروا القبور فإنها تذكّر الموت» وحده |
| #436، #126 | ألفاظ لا تناسب شاشة العائلة | قرارك: إبقاء أو حذف |
| #28/#658، #4/#107 | مكرّر | إبقاء واحد من كل زوج |
| الـ42 التي ضعّفها زبير علي زئي وحده | في التقرير فقط | لا شيء ما لم تُرِد قاعدة أشد |

### المسابقة (assets/quiz/bank.json؛ التصحيح في games/data/quiz.json على work/games-data)
| المعرّف | المشكلة | المقترح |
|---|---|---|
| geo-cap-Q233 | عاصمة مالطا «البلد» | تغيير الإجابة إلى «فاليتا» |
| hv-dhuhr | «الجمعة» صحيحة أيضًا | تغيير الخيار إلى «الوتر» |
| geo-cont-Q29، Q142، Q38 | «أفريقيا» صحيحة جزئيًا لإسبانيا وفرنسا وإيطاليا | تغيير الخيار إلى قارة أخرى |
| geo-cont-Q804 | «أمريكا الجنوبية» لبنما | تغيير الخيار |
| geo-cap-Q252 | جاكرتا/نوسانتارا | إبقاء ومراقبة، أو حذف |
| geo-cap-Q983، geo-rev-Q983 | عاصمة غينيا الاستوائية الجديدة | حذف حتى تستقر المصادر |
| geo-cap-Q697 | ناورو بلا عاصمة رسمية | حذف |
| geo-cur-Q717 | «بوليفار السيادي» قديم | تغيير الإجابة إلى «بوليفار فنزويلي» |
| geo-lang-Q115 | إثيوبيا: خمس لغات عمل | حذف أو إعادة صياغة |
| geo-lang-Q1032 | النيجر: «الفرنسية» هنا و«الهوسية» في خلية الحروف | حذف الاثنين أو السؤال عن «اللغة الوطنية» |
| hv-khwarizmi | ابن سينا يحتمل | إعادة صياغة («اشتُقّت من اسمه كلمة خوارزمية») |
| an-class-Q152 | «حيوان سمكة» ← «أسماك» | حذف |
| assets/quiz/starter.json | ٢٣ بندًا لم تعد في البنك | إعادة البناء من البنك الحالي |
| hi-giza، sg-oryx، el-*، an-class-*، العملات والعواصم | صياغة ونحو | التصحيحات في quiz.md |

### ألعاب الأسرة (assets/games؛ التصحيح في games/ على work/games-data)
| المعرّف | المشكلة | المقترح |
|---|---|---|
| khanat م/country «ألمانيا» | لا تبدأ بالميم | تغيير إلى «مالي» |
| khanat و/object «ألوان» | لا تبدأ بالواو | تغيير إلى «وعاء» |
| odd-one family | «صديق، جار، ضيف، معلم، عريس، عروس» ليسوا من العائلة | حذفها من الموضوع |
| odd-one clothes / farm / dishes | «نظارة، ساعة، كم»؛ «قطة، كلب، حمامة»؛ «برياني» | حذف |
| arab_countries (charades، odd-one) و gs-arab | فلسطين غائبة هنا وموجودة في «خمّن» | قرار واحد لكل الألعاب |
| khanat ن/saudi_city «نيوم» | ليست مدينة | تغيير إلى «النماص» |
| word5 | كلمات نادرة أو ثقيلة («مشرحة، طاغية، أمركة…») | رفع المستوى أو حذف |
| missing-word mw-cur-26 | «طبيبًا» | «حكيمًا» |
| sc-moon (خلية الحروف) | الإجابة في السؤال | حذف |
| الترجمات الإنجليزية (بلبل، نسر، صرصور، نقانق، برج المملكة) | ترجمة | التصحيحات في games.md |
