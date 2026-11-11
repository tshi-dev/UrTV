# «واحد مو معنا» and «بدون كلام / وصّف»: how they are built

Two family games played with **one remote and no phones** (pass-and-play). Specs: `games/odd-one/SPEC.md`, `games/charades/SPEC.md` and `games/COMMON.md` on the branch work/games-data. Words: `docs/GAMES-DATA.md` (`odd-one.json`, `charades.json`). Look and shared screens: the games kit (`docs/GAMES-KIT.md`).

## The parts
| Part | Where | What |
|---|---|---|
| Rules | `games/oddone/OddOneGame.java`, `games/charades/CharadesGame.java` | One game's state: phases, timers, votes, points. No Android, no screen, `java.util` only, so a phone mode can drive the same object later (`secretFor(player)` already answers per player). |
| Words | `OddOneWords.java`, `CharadesWords.java` | The whole-file check of the published forms, and the picking: never a word twice in a game, the last ones played on this TV wait (120 and 200). |
| Screens | `OddOneFlow.java`, `CharadesFlow.java` | Which screen is up and what each key does, as plain classes. The activity only draws `screen()` and feeds `key()`, `select()`, `tick()`, `background()`. |
| Tests | `tests/party_games_test.py`, `tests/PartyGamesTest.java` | All of the above on a plain JVM with a fake clock and the bundled starters. |

## «واحد مو معنا»: the rule choices
- **Players 3 to 10** (the SPEC says 8, the owner's brief 10), default 4, named by number. Rounds 3 or 5. One lap of talk, or two.
- **Deal:** the remote goes round in seat order. Pass screen → OK → a 2-second count → the secret for **5 seconds** or until OK → the next pass screen. Normal players see the topic and the word; the odd one sees the topic and «أنت برا السالفة».
- **A secret never stays up:** Back on it only hides it (the same player again); the app leaving hides it; nothing opens under the pause card. «رجّعها» (DOWN on the next pass screen, or before the first speaker) gives the last player his word again.
- **Talk:** the seats in a circle from a random first speaker (the odd one may be first: starting anywhere else would clear a player). 30 seconds each as a guide only: one soft beep at 0, OK moves on.
- **The vote is by show of hands (the default, chosen as the faster one for a living room):** everyone points on the count, the host picks the most pointed-at player on the grid, or «ما اتفقنا» when the room cannot settle it (counts as not caught). It is one screen instead of one hand-over per player.
- **The secret vote** of the SPEC is kept as a setting: the remote goes round, each picks a suspect (never himself); the count comes after the last vote; a tie is voted again once between the tied players only; a second tie accuses nobody.
- **Reveal:** who the room chose, then who the odd one was. Caught: one spoken guess; OK opens the word for the room, then «صح» (right, focused) or «خطأ» (left).
- **Points:** caught and wrong: 1 to every other player. Caught but right: 1 to the odd one only. Not caught: 2 to the odd one. Most points wins; a tie is shared.
- **Words:** «عشوائي» goes through every topic before one comes again; a chosen topic that runs out hands over to another.
- **«إنهاء الجولة»** on the pause card drops the round with no points.

## «بدون كلام / وصّف»: the rule choices
- **Teams 2 to 4**, 45 / 60 / 90 seconds (default 60), 3 turns each. «أطفال» takes kids words of level 1 only. «وصّف» has the words that cannot be acted as well.
- **Turn:** pass screen → OK shows the actor the word, the clock waits → OK hides it and **the clock starts**.
- **Keys on the clock:** OK «صح» (+1 and the next word at once), LEFT «تخطّي» (free, 3 a turn), UP shows the word again for 3 seconds (free), DOWN «مخالفة» (only in «وصّف» with the setting on: -1, never below 0).
- **A new word** stays up 4 seconds or until OK. «صح» is ignored for 0.6 seconds after a new word came, so a double press does not throw it away.
- **The clock at 0:** the bell, the open word is shown to everyone, the turn passes. Back hides the word first and stops the clock; before the clock started it returns to the pass screen with the same word.
- **A tie for first:** sudden death between the tied teams only, one word each, 30 seconds, no skip; up to 3 goes, then they share the win.
- **Deck:** used up → shuffled again and the screen says so; never the same word twice in a row.

## Left for a real TV
Reading the word from 3 to 4 m in 4 to 5 seconds, the feel of the 0.6-second guard, whether 2 seconds of count is enough to lift the remote, and the bell and beep levels.

## The words on screen (go to `i18n/en/saver.tsv` with the screens)
| Arabic | English |
|---|---|
| واحد مو معنا | Odd One Out |
| الكل يعرف الكلمة إلا واحد، اكتشفوه | Everyone knows the word but one. Find them |
| بدون كلام | No Talking |
| وصّف | Describe It |
| مثّل الكلمة أو وصّفها لفريقك قبل ما يخلص الوقت | Act the word or describe it to your team before time runs out |
| اللاعبين | Players |
| الموضوع | Topic |
| عشوائي | Random |
| الجولات | Rounds |
| التصويت | Vote |
| برفع الأيدي | Show of hands |
| سرّي | Secret |
| الوضع | Mode |
| الفرق | Teams |
| الوقت | Time |
| %s ثانية | %s s |
| للجميع | Everyone |
| أطفال | Kids |
| ابدأ | Start |
| لاعب %s | Player %s |
| سلّموا الريموت لـ %s | Pass the remote to %s |
| الباقين: غطّوا عيونكم | Everyone else: cover your eyes |
| أنا جاهز | I'm ready |
| رجّعها للاعب اللي قبل | Show the last player again |
| أنت برا السالفة | You are the odd one |
| اضغط OK للإخفاء | Press OK to hide |
| دور %s | %s's turn |
| وصّف الكلمة بجملة وحدة بدون ما تقولها | Describe the word in one sentence without saying it |
| اضغطوا OK للّي بعده | Press OK for the next player |
| مين اللي مو معنا؟ | Who is the odd one? |
| أشّروا كلكم على ثلاثة، واختاروا اللي عليه أكثر | Everyone point on three, then pick the most pointed-at |
| ما اتفقنا | We can't agree |
| اختر اللي تشك فيه | Pick your suspect |
| تعادل، صوّتوا مرة ثانية بين المتعادلين | A tie. Vote again between the tied players |
| اخترتوا %s | You chose %s |
| ما اتفقتوا على أحد | You did not agree on anyone |
| اللي مو معنا هو %s | The odd one was %s |
| خمّن الكلمة بصوت عالي | Guess the word out loud |
| اضغطوا OK لكشف الكلمة | Press OK to show the word |
| صح | Right |
| خطأ | Wrong |
| كشفتوه | You caught them |
| كشفتوه لكنه عرف الكلمة | Caught, but they guessed the word |
| ما انكشف | They got away |
| الجولة %s من %s | Round %s of %s |
| الجولة الجاية | Next round |
| النتيجة النهائية | Final score |
| الفائز %s | %s wins |
| تعادل في المركز الأول | A tie for first place |
| لعبة جديدة | New game |
| الرئيسية | Home |
| متابعة | Continue |
| إنهاء الجولة | End the round |
| الممثل ياخذ الريموت | The actor takes the remote |
| اضغط OK لتشوف الكلمة | Press OK to see the word |
| اضغط OK وابدأ | Press OK and start |
| OK صح | OK: right |
| تخطّي | Skip |
| شوف الكلمة | See the word |
| مخالفة | Foul |
| باقي %s تخطّي | %s skips left |
| خلص الوقت | Time's up |
| الكلمة كانت %s | The word was %s |
| جبتوا %s | You got %s |
| الدور لـ %s | Next: %s |
| تعادل، كلمة وحدة لكل فريق | A tie: one word each |
| خلصت الكلمات، نعيدها من جديد | Out of words, shuffling again |
