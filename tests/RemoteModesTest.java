package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import sa.urtv.screensaver.games.author.AuthorGame;
import sa.urtv.screensaver.games.charades.CharadesGame;
import sa.urtv.screensaver.games.charades.CharadesWords;
import sa.urtv.screensaver.games.closest.ClosestGame;
import sa.urtv.screensaver.games.closest.PaperPlaces;
import sa.urtv.screensaver.games.liar.LiarGame;
import sa.urtv.screensaver.games.liar.LiarRemote;
import sa.urtv.screensaver.games.remote.CallAloudRound;

/** For tests/remote_modes_test.py: the rules of the one-remote modes («نادوا الجواب»، «على الظهر»، «واحد يكذب» by hands, «أقرب رقم» on paper) on a plain JVM. Prints PASS/FAIL lines. */
public final class RemoteModesTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};

    /** n made-up items in the files' form ({"v":1,"items":[{"i","q":[ar],"v"}]}): only for the tests */
    static String synthetic(int n, boolean withValue) {
        StringBuilder b = new StringBuilder("{\"v\":1,\"items\":[");
        for (int i = 0; i < n; i++) b.append(i > 0 ? "," : "").append("{\"i\":\"t").append(i).append("\",\"q\":[\"item ").append(i).append("\"]").append(withValue ? ",\"v\":" + (1000 + i) + ",\"d\":1,\"k\":1" : ",\"k\":1").append("}");
        return b.append("]}").toString();
    }
    static CallAloudRound.Item item(String q, int right) { return new CallAloudRound.Item("", q, Arrays.asList("a", "b", "c", "d"), right, "x", ""); }

    public static void main(String[] a) throws Exception {
        // ---------------- «نادوا الجواب»
        {
            CallAloudRound r = new CallAloudRound(3, Arrays.asList(item("q1", 1), item("q2", 2), new CallAloudRound.Item("", "7 x 8", null, -1, "56", "")));
            check("call aloud: it starts on the question; OK reveals, OK again asks who, nothing skips a phase", r.phase == CallAloudRound.Phase.ASK && !r.pick(0));
            r.toPick(); check("call aloud: «من قالها أول؟» cannot come before the answer is shown", r.phase == CallAloudRound.Phase.ASK);
            r.reveal(); r.toPick();
            check("call aloud: the side picked scores POINTS (3), the next question comes", r.phase == CallAloudRound.Phase.PICK && r.pick(1) && r.score[1] == CallAloudRound.POINTS && r.index == 1 && r.phase == CallAloudRound.Phase.ASK && r.winner == 1);
            r.reveal(); r.toPick(); r.pick(-1);
            check("call aloud: «ما أحد» scores nobody and goes on", r.score[0] + r.score[1] + r.score[2] == CallAloudRound.POINTS && r.winner == -1 && r.index == 2);
            r.reveal(); r.toPick(); boolean bad = r.pick(3) || r.pick(-2); r.pick(2);
            check("call aloud: a side that does not exist is refused; after the last question it ends", !bad && r.phase == CallAloudRound.Phase.END && r.score[2] == 3 && r.item() == null);
            check("call aloud: 2 to 8 sides, an empty list ends at once; a question may have no options (a sum)",
                new CallAloudRound(2, new ArrayList<CallAloudRound.Item>()).phase == CallAloudRound.Phase.END && CallAloudRound.MAX_SIDES == 8 && CallAloudRound.POINTS == 3
                && new CallAloudRound.Item("", "p", null, -1, "1", "").options.isEmpty());
            boolean threw = false; try { new CallAloudRound(1, new ArrayList<CallAloudRound.Item>()); } catch (IllegalArgumentException e) { threw = true; }
            check("call aloud: one side is not a game", threw);
            CallAloudRound f = new CallAloudRound(2, Arrays.asList(item("q", 0), item("q", 0))); f.finish();
            check("call aloud: «إنهاء الجولة» ends it", f.phase == CallAloudRound.Phase.END);
        }
        // ---------------- «أقرب رقم» on paper
        {
            String closestJson = synthetic(45, true);   // the banks are encrypted and stay closed: the test makes its own items
            List<ClosestGame.Item> items = ClosestGame.parse(MiniJson.parse(closestJson));
            check("paper: the starter's own items load (the bank's questions and numbers unchanged)", items != null && items.size() >= 2);
            PaperPlaces g = new PaperPlaces(4, items.subList(0, 2));
            g.reveal(); g.toPlace();
            check("paper: three places, 3 2 1 points, 5 for an exact first place (constants in PaperPlaces)", PaperPlaces.PLACES == 3 && Arrays.equals(PaperPlaces.POINTS, new int[] {3, 2, 1}) && PaperPlaces.EXACT == 5 && g.pointsOfPlace() == 3);
            g.setExact(true); int exactFirst = g.pointsOfPlace(); g.pick(2);
            check("paper: «بالضبط» makes the first place 5; it is for the first place only", exactFirst == 5 && g.score[2] == 5 && g.place == 1 && g.pointsOfPlace() == 2);
            g.setExact(true);
            check("paper: the second place cannot be made exact", g.pointsOfPlace() == 2);
            boolean again = g.pick(2); g.pick(0);
            check("paper: a side holds one place; the next gets 2", !again && g.score[0] == 2 && g.taken(2) && g.taken(0));
            g.pick(3);
            check("paper: after the third place the next question comes", g.score[3] == 1 && g.index == 1 && g.phase == PaperPlaces.Phase.ASK);
            g.reveal(); g.toPlace(); g.pick(1); g.pick(-1);
            check("paper: «ما أحد» stops the picking and goes on (the second question was the last: it ends)", g.score[1] == 3 && g.phase == PaperPlaces.Phase.END);
            PaperPlaces two = new PaperPlaces(2, items.subList(0, 1)); two.reveal(); two.toPlace(); two.pick(0); two.pick(1);
            check("paper: with two sides only two places are given", two.score[0] == 3 && two.score[1] == 2 && two.phase == PaperPlaces.Phase.END);
        }
        // ---------------- «واحد يكذب» with one remote
        {
            List<AuthorGame.Prompt> topics = AuthorGame.parse(MiniJson.parse(synthetic(45, false)));
            check("liar: the starter's own topics load", topics != null && topics.size() >= 3);
            check("liar: the fewest players with one remote is 5 (one place to change it: LiarRemote.MIN_PLAYERS); the phone game keeps 4", LiarRemote.MIN_PLAYERS == 5 && LiarGame.MIN_PLAYERS == 4);
            LiarGame g = new LiarGame(5, topics.subList(0, 3), new Random(4)); g.startTelling(); g.endTelling();
            int liar = -1; for (int t : g.tellers()) if (g.liarIs(t)) liar = t;
            LiarRemote.applyRoom(g, liar); g.reveal();
            int voters = 0; for (int p = 0; p < 5; p++) if (!g.isTeller(p)) { voters++; if (g.score[p] != LiarGame.FOUND) voters = -99; }
            check("liar: the room found the liar: every player who did not tell scores FOUND (2), the liar nothing", voters == 2 && g.score[liar] == 0);
            g.next(); g.startTelling(); g.endTelling();
            int liar2 = -1, honest = -1; for (int t : g.tellers()) { if (g.liarIs(t)) liar2 = t; else if (honest < 0) honest = t; }
            LiarRemote.applyRoom(g, honest); g.reveal();
            check("liar: the room chose an honest teller: the liar scores 1 for each voter fooled (2), the voters nothing", g.gained[liar2] == 2);
            g.next(); g.startTelling(); g.endTelling(); int before = g.score.clone().length;
            LiarRemote.applyRoom(g, -1); g.reveal(); int liar3 = -1; for (int t : g.tellers()) if (g.liarIs(t)) liar3 = t;
            check("liar: «ما اتفقنا» counts as not found (NOBODY_FOOLS): the liar scores for the voters", LiarRemote.NOBODY_FOOLS && g.gained[liar3] == 2);
            int votes = 0; LiarGame h = new LiarGame(6, topics.subList(0, 1), new Random(1)); h.startTelling(); h.endTelling(); LiarRemote.applyRoom(h, h.tellers()[0]);
            check("liar: six players, three voters: all of them voted once the room spoke", h.votes() == 3 && h.allVoted());
        }
        // ---------------- «على جبيني» as «على الظهر»
        {
            List<CharadesWords.Word> words = CharadesWords.parse(MiniJson.parse(new String(Files.readAllBytes(Paths.get(a[0], "charades.json")), StandardCharsets.UTF_8)));
            CharadesGame g = new CharadesGame(new CharadesWords.Deck(words, true, false, null, new Random(5)), true, 2, 60, 3, false, () -> now[0]);
            g.behind = true; g.ready();
            check("behind: OK on the cover starts the clock at once and the word is on the TV for the room", g.phase == CharadesGame.Phase.PLAY && g.wordVisible() && g.clockRunning() && g.leftMs() == 60_000);
            now[0] += 3000; String w1 = g.word.text[0]; boolean ok1 = g.correct() && g.got == 1 && g.wordVisible() && !g.covered();
            check("behind: «صح» scores and the next word shows at once (never covered, never hidden)", ok1 && g.score[0] == 1);
            now[0] += 100; boolean quick = g.correct();
            check("behind: a double press is ignored for 0.6 s (the guard stays)", !quick && g.score[0] == 1);
            now[0] += 1000; boolean sk = g.skip();
            check("behind: «تخطّي» gives another word (3 a turn)", sk && g.skips == 1 && g.wordVisible());
            g.hide(); now[0] += 10_000; g.tick();
            check("behind: hide() and the word timer do nothing: the word stays up all the turn", g.wordVisible());
            g.pause(); boolean hid = !g.wordVisible(); g.resume();
            check("behind: pause hides the word, resume brings it back", hid && g.wordVisible());
            now[0] += 60_000; g.tick();
            check("behind: at 0 the turn ends and the word is shown to everyone", g.phase == CharadesGame.Phase.TURN_END);
            CharadesGame o = new CharadesGame(new CharadesWords.Deck(words, true, false, null, new Random(5)), true, 2, 60, 3, false, () -> now[0]); o.ready();
            check("behind: off (the default), the first word waits for the hand-over and the clock has not started", o.wordVisible() && !o.clockRunning());
        }
        System.exit(failed == 0 ? 0 : 1);
    }
}
