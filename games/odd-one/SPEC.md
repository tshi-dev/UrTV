# واحد مو معنا (the odd one out)

Our own game in the genre of the "everyone knows the topic but one" party games. Own name, wording, look and topic lists; no name, word list, art or sound of any product is used.

## In one line
Everyone is shown the same secret word except one person, who only sees the topic. Players describe the word one by one without saying it; then everyone votes for who they think did not know it.

## Players
3 to 8, each person is a "player", not a team. Names are numbers and avatars by default (لاعب ١ ... ٨), editable under the button.

## Remote-only flow (default)
1. Ready screen: chips for اللاعبين (3 to 8; default 4), الموضوع (عشوائي or a topic), الجولات (3; or 5) and button «ابدأ».
2. **Deal.** The game picks a topic and a word of that topic, and picks the odd player at random. It runs the **hand-over screen** (COMMON.md) once per player: «سلّموا الريموت لـ لاعب ٢». Each normal player sees «<topic>: <word>»; the odd player sees «<topic>: أنت برا السالفة» (topic only). Each hand-over takes about 4 seconds.
3. **Talk round.** The TV shows the order of play (avatars in a circle) and a timer of 30 seconds per player, one lap (setting: two laps). The current player says one sentence about the word without saying it. OK moves to the next player; the timer is only a guide and beeps softly.
4. **Vote.** Each player in turn, with the hand-over screen, points to a suspect (a grid of the avatars; OK selects). Votes are secret until all are in; ties go to a revote between the tied players only.
5. **Reveal.** The most-voted player is shown; then the odd player is revealed with "he/she did not know it" and may try to guess the word aloud: the host presses OK to reveal the word then «صح»/«خطأ».
6. **Scoring** per round: if the group voted out the odd player and the odd player did not guess the word: 1 point for each normal player. If the group voted out the wrong person: 2 points to the odd player. If the odd player was caught but guessed the word: 1 point to the odd player only. After the last round the player with most points wins.

## Phone flow (optional)
The phones receive the word or the topic (no hand-over needed), and the vote is on the phones. The TV still shows the order, timer and the final reveal. Players without a phone mix in with hand-over: both kinds can play together.

## Edge cases
- A player presses OK by mistake on the secret screen: the secret hides at once; the hand-over restarts for that player only.
- 3 players: the vote always has a majority of two or a tie; ties revote once, then the odd player wins.
- If a topic runs out of unused words in one game, the next word of another topic is used; a word is never repeated inside one game.

## Data
`games/odd-one/topics.json`: topics, each with 12 to 40 words (Arabic and English), difficulty 1 to 3 by how well the thing is known, `age`. Words come from the most-linked Wikidata items per class, reviewed to be family-safe; the owner-agreed rule applies (no politics or sectarian items).
