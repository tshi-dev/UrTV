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
