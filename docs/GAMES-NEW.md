# The new party games (4.4)

All of them are played with phones as controllers (the TV is the board), sit in the games list marked «بالجوالات» and under the «بالجوالات» filter, use the night palette like every game, keep their banks in the app as encrypted bundles (`assets/games/*.gb`, docs/LICENSE-CONTENT.md) with the larger ones as packs (docs/GAMES-PACKS.md), and never repeat an item on the same TV (the last items are remembered per game). Names of teams and players are written by the people (docs/GAMES-KIT.md). The rules of each game are a plain-Java engine tested on a JVM (`tests/new_games_test.py`).

## على جبيني (guess the word on your forehead)
Two to four teams. A player holds the phone to the forehead (the phone says «جاهز» first, then a count of three); the team describes the word on the phone without saying it. Tilt the phone down (or the «صح» button) for a point and the next word; tilt up (or «تخطّي») for the next word with no point. 45, 60 or 90 seconds a turn; every team plays 2, 3 or 4 turns, the phones of a team take the turns in turn; most points wins. The TV never shows the word. The words are the «وصّف» words of «بدون كلام» (with the kids filter); nothing repeats on this TV. Phones: at least one a team, 12 at most. The tilt uses the phone's motion sensor (iPhones ask once, on a tap); the two buttons always work.

## من كاتبها؟ (who wrote it?)
Three to ten phones. A question appears on the TV («أغرب شيء أكلته في حياتك؟»); everyone types a short answer on their phone (30 characters, nobody knows whose it is). The answers come up one at a time, without names; everyone but the writer votes who wrote it (never yourself). A right guess scores 1 for the voter; every wrong guess scores 1 for the writer. 6, 8 or 10 questions; the writing takes 45, 60 or 90 seconds (it ends sooner when everyone has written). The bank is 129 family-safe prompts we wrote (120 are marked for kids), bundled as `assets/games/author.gb`; nothing repeats on this TV.

## أقرب رقم (closest number)
Two to ten phones. A question whose answer is a whole number («كم ثانية في اليوم؟»); everyone types a guess on a number pad; the closest scores 3 (5 for the exact number), the next distance 2, the next 1, guesses at the same distance share the points. 8, 10 or 12 questions, 20, 30 or 45 seconds each (sooner when everyone has answered). The bank is 133 items, each with a source (`assets/games/closest.gb`): 90 areas of countries from mledoze/countries (ODbL; independent UN members only, Israel and Western Sahara are never listed), 43 constants and sums (computed, or checked on ar.wikipedia: 13 of 15 checked pages state the number; the Moon's distance and Kilimanjaro's height are the standard values). Nothing repeats on this TV.

## واحد يكذب (who's lying)
Four to ten phones. Each round the TV names three players (the ones who have told the fewest times) and a topic («رحلة ما تنساها»). Each of the three sees a secret on their own phone: two are told «احكِ قصة حقيقية», one «اكذب: اخترع قصة». They tell their stories (60, 90 or 120 seconds) and the others question them. Then everyone who did not tell votes on their phone who lied. Finding the liar scores 2; the liar scores 1 for every voter fooled. 3, 5 or 6 rounds. The bank is 82 topics we wrote (`assets/games/liar.gb`); nothing repeats on this TV. The TV never shows who the liar is before the reveal.

## اقرأ الإيموجي (read the emoji)
Two to ten phones. A line of emoji stands for a word (🍚🍗 is «كبسة»); everyone picks the right one of four on their phone; the first right answer scores 3, the second 2, every other right answer 1. 10, 15 or 20 puzzles, 10, 15 or 20 seconds each. The bank is 151 puzzles (food, animals, places, things, nature, jobs, phrases) bundled as `assets/games/emoji.gb`; the wrong options are other answers of the same category; no film or cartoon titles. Nothing repeats on this TV. The emoji are drawn by the TV's own emoji font.

## اربط الكلمات (link the words)
Four to ten phones, two teams (the phones take the teams in turn). 25 words on the TV. A secret key (9 words for the team that starts, 8 for the other, 7 neutral, 1 ending word) is on the two captains' phones (the first two phones) and nowhere else. The captain of the team whose turn it is says a one-word clue aloud and picks on the phone how many words it links (1 to 9); the team's other phones then touch words (the first tap counts, «أنهينا» ends the turn). Its own word goes on (up to the number plus one), a neutral word ends the turn, the other team's word counts for that team and ends the turn, the ending word loses the game. The first team to uncover all its words wins. The TV shows the key only at the end. The list is 348 common nouns (`assets/games/link.gb`); nothing repeats on this TV.

## ليل القرية (Village Night)

Six to ten phones. Each phone gets a secret role: villager, wolf (1 for 6 players, 2 for 7 to 8, 3 for 9 to 10), one guard, one seer. The wolves see each other's names. At night the wolves choose someone to send away, the guard protects one player (not the same one two nights in a row), the seer asks about one player and hears «wolf» or «not a wolf» on their own phone. In the morning the village learns who left (and their role), talks for 90 seconds, then votes on the phones. A tie sends nobody. The village wins when the last wolf is out; the wolves win when they are as many as the rest.

- Rules: `games/night/NightGame.java` (no Android), tested in `tests/NewGamesTest.java`.
- Screen: `NightActivity`. The TV shows a role only when its player leaves or at the end. The wording is family-safe: someone «leaves the village», nobody is killed.
- No content bank, so nothing is bundled.

## كم تقدر؟ (How Many?)

Two to ten phones. A topic is shown («فواكه»); each phone bids a number from 1 to 10 (at most half the list): how many things of the topic they can name. The highest bid (a tie is settled by chance) types the names on their phone; every name is checked against the topic's list (spelling is forgiving: marks, alef and taa marbuta forms and «ال» do not matter; a name twice counts once). Naming the bid or more scores the bid; naming fewer scores nothing and every other player gets 1. Points only. 6, 8 or 10 topics; 45, 60 or 90 seconds to type. The bank is 22 closed topics (fruit, vegetables, wild animals, Arab countries, world capitals, Saudi cities, dishes, car makers, months...) in `assets/games/guess.gb`; answers are typed in Arabic. Nothing repeats on this TV.

## ارسم وخمّن (Draw & Guess)

Three to ten phones. One phone draws a word (the charades bank) with a finger; the TV shows the strokes as they arrive; the others type guesses on their phones (spelling is forgiving; the English word counts too). The first right guess scores 3, the second 2, the rest 1; the drawer gets 1 for each right guess. Each player draws once or twice. 60, 90 or 120 seconds a round, sooner when everyone is right. Nothing repeats on this TV.

The phone server grows its body limit for this one route only:
- `POST /api/draw` takes up to 8 KB (every other route still 1 KB, anything bigger is refused with 413 before it is read).
- The room takes it only from the phone whose current card is a draw card, with the card's id (a stale card is refused), at most 8 posts a second, 500 points in a post, 300 strokes and 6000 points a card, every coordinate a whole number from 0 to 1000; a post with anything else in it is refused whole. The server stays on the local network only, with its code and rate limits as before.
- Strokes are shown by the TV as lines on a canvas; nothing the phone sends is ever read as text, a link or a picture. The page builds no HTML from them.

**«كم تقدر؟» is hidden for now:** its topic lists were written from common knowledge and are not yet checked against sources. `GuessActivity` stays in the manifest; to show it, add `new Game("guess", "كم تقدر؟", "من يسمّي أكثر في الموضوع؟", "GuessActivity", true, true)` to `GameRegistry` before the «packs» entry, `"guess"` to `PHONE_GAMES`, and the name and line to `QuizGamesActivity.WORDS`.

## سباق الحساب (Math Race)

Two to eight phones. Quick mental arithmetic made by code, so it is always right and never runs out (no bank). Everyone types the answer on a number pad; the first right answer scores 3, the second 2, every other right answer 1; a wrong answer costs nothing. Three levels (easy: sums, differences and tables up to 10; medium: bigger products, division, brackets, a missing number; hard: two-digit products, squares, percentages, sums of products, sequences, a difference of squares). 10, 15 or 20 questions, 15, 25 or 40 seconds each. The TV shows the question in both languages together (an instruction line in Arabic and in English), and the phones show the same question; the digits follow the person's digit setting. The tests evaluate 600 generated questions with an independent evaluator.
