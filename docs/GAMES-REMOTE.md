# «ريموت واحد»: the one-remote way for the phone games

The first row of the setup page of seven phone games, «كيف تلعبون؟» (**ريموت واحد** / جوالات), kept per game like any chip; the party games keep «جوالات» as the default, so nothing changes until someone picks «ريموت واحد» (or presses «نلعب بالريموت» in the phone room). The old switch «اللعب بالريموت بس» is gone: this row replaces it (docs/research/GAMES-FLOW.md). On «ريموت واحد», the game does not open a room: it plays on the TV with the remote. The setup page and the tile say «ريموت» then. Mockups: `docs/mockups/games-remote/`. Review and designs: `docs/research/GAMES-REVIEW-CODE.md`. Nothing here adds content: every question comes from the game's own bank, verses file or generator, read in memory through `GameBundles` as before.

| Game | One-remote way | Code |
|---|---|---|
| اقرأ الإيموجي, ألغاز المنطق, كمّل الآية, سباق الحساب | «نادوا الجواب»: the question and four options (a sum has none) on the TV, a clock, everybody calls the answer aloud, OK shows the right one, the host picks WHO called it first with the D-pad (or «ما أحد»): +3. The sides are 2 to 8 teams (names by the people, as everywhere) | `CallAloudActivity`, `games/remote/CallAloudRound` |
| على جبيني | a mode of «بدون كلام»: «ولّ ظهرك للتلفزيون» (the guesser), then the word stays on the TV for everyone else and the clock starts; OK = «صح» and the next word, ◀ = «تخطّي». The «وصّف» words, the charades scores and tie rule | `CharadesActivity` (`behind`), `CharadesGame.behind` |
| واحد يكذب | the remote goes to each of the three tellers (the kit's hand-over: «احكِ قصة حقيقية» or «اكذب»), the clock, then a show of hands: the host picks the teller the room chose or «ما اتفقنا». The room is scored as one voice: every player who did not tell votes the same | `LiarActivity`, `games/liar/LiarRemote` |
| أقرب رقم | paper: everyone writes a number, the TV is the clock and shows the number, the host picks the closest three (3, 2, 1; 5 for an exact first place); a side holds one place | `ClosestActivity`, `games/closest/PaperPlaces` |

**Constants the owner may change** (one line each): `CallAloudRound.POINTS` (3), `CallAloudRound.MAX_SIDES` (8), `LiarRemote.MIN_PLAYERS` (5; the phone game keeps 4), `LiarRemote.NOBODY_FOOLS` («ما اتفقنا» counts as not found), `PaperPlaces.PLACES` / `POINTS` / `EXACT`.

Look: the score is in its own cell with a «نقطة» label (`GameActivity.scoreBar`), the focus is scale and shadow only (`focusCard`), no pills, digits follow the person's setting (Arabic-Indic by default). Tests: `tests/remote_modes_test.py` (rules on a JVM and the wiring by source).

## Draft: «طريقة اللعب» for the remote modes (for the owner to approve; NOT wired into the app yet)

Same blocks as the other games (هدف اللعبة، كيف تلعب، النقاط، النهاية); «بالجوالات» is left out because these modes need none. Plain Gulf Arabic, the keys named as the bottom line names them. The numbers are the code's (3 points; 5 players at least in «واحد يكذب»; three places in «أقرب رقم»): if the owner changes a constant, the text changes with it. Wiring = rows in `GameRegistry.HOWTO` (marked in `Words.java`, English in `saver.tsv`, and `docs/mockups/howto/TEXTS.md`) once approved.

### نادوا الجواب (اقرأ الإيموجي، ألغاز المنطق، كمّل الآية، سباق الحساب)
**هدف اللعبة**
- اقرأ الإيموجي: اعرفوا الكلمة اللي يدل عليها سطر الإيموجي قبل الباقين.
- ألغاز المنطق: فكّروا وحلّوا اللغز قبل الباقين.
- كمّل الآية: كمّلوا الآية، والأسرع يكسب.
- سباق الحساب: احسبوا المسألة قبل الباقين.

**كيف تلعب**
1. اختاروا عدد الفرق (من ٢ إلى ٨) والأسئلة والوقت، ثم «ابدأ».
2. يطلع السؤال على الشاشة ومعه أربعة خيارات (المسائل الحسابية بدون خيارات). كل فريق يقول جوابه بصوت عالٍ.
3. اضغطوا «اختيار» وتظهر الإجابة الصحيحة.
4. اضغطوا «اختيار» مرة ثانية، واختاروا بالأسهم الفريق اللي قالها أول ثم «اختيار». إذا ما أحد عرفها اختاروا «ما أحد».

**النقاط** الفريق اللي قالها أول وكانت صح ياخذ ٣ نقاط. «ما أحد»: بدون نقاط.
**النهاية** بعد آخر سؤال (١٠ أو ١٥ أو ٢٠) يفوز الأعلى نقاطًا.

### على جبيني (بالريموت)
**هدف اللعبة** فريقك يوصّف لك الكلمة اللي على الشاشة، وأنت تخمّنها وظهرك للتلفزيون.
**كيف تلعب**
1. اختاروا الفرق (من ٢ إلى ٤) والوقت، ثم «ابدأ».
2. اللي يخمّن يولّي ظهره للتلفزيون، والباقين يشوفون الكلمة. اضغطوا «اختيار» ويبدأ الوقت.
3. وصّفوا الكلمة بدون ما تقولونها. إذا خمّنها اضغطوا «اختيار» وتطلع الكلمة اللي بعدها.
4. إذا صعبت اضغطوا ◀ لتخطّيها (ثلاث مرات في الدور).

**النقاط** نقطة لكل كلمة صح.
**النهاية** كل فريق يلعب ثلاث أدوار، والأعلى نقاطًا يفوز. إذا تعادلتوا يلعب المتعادلون كلمة وحدة لكل فريق.

### واحد يكذب (بالريموت)
**هدف اللعبة** ثلاثة يحكون قصصهم وواحد منهم يكذب، والباقين يكشفونه.
**كيف تلعب**
1. اختاروا عدد اللاعبين (من ٥ إلى ١٠) والجولات والوقت، ثم «ابدأ».
2. الشاشة تسمّي ثلاثة لاعبين وموضوعًا. الريموت يروح لكل واحد منهم بالدور ويشوف دوره وحده: «احكِ قصة حقيقية» أو «اكذب». الباقين يغطّون عيونهم.
3. كل واحد من الثلاثة يحكي قصته، والباقين يسألون.
4. اضغطوا «اختيار» وصوّتوا: على العدّ، الكل يشير للي يظنه. اختاروا بالأسهم اللي أشار عليه الأكثر، أو «ما اتفقنا».

**النقاط** إذا كشفتوه، كل واحد ما حكى ياخذ نقطتين. إذا ما كشفتوه، الكذّاب ياخذ نقطة عن كل واحد انخدع.
**النهاية** بعد ٣ أو ٥ أو ٦ جولات يفوز الأعلى نقاطًا.

### أقرب رقم (على الورقة)
**هدف اللعبة** اكتبوا رقمًا قريبًا من الجواب الصحيح.
**كيف تلعب**
1. اختاروا عدد الفرق والأسئلة والوقت، ثم «ابدأ».
2. يطلع سؤال جوابه رقم صحيح. كل فريق يكتب رقمه على ورقة، ولا أحد يشوف ورقة غيره.
3. اضغطوا «اختيار» وتظهر الإجابة.
4. اختاروا بالأسهم الفريق صاحب أقرب ورقة، ثم الثاني، ثم الثالث. إذا كان الرقم بالضبط فعّلوا «بالضبط» أول.

**النقاط** الأقرب ٣ نقاط (٥ إذا كان الرقم بالضبط)، ثم ٢، ثم ١.
**النهاية** بعد ٨ أو ١٠ أو ١٢ سؤالًا يفوز الأعلى نقاطًا.
