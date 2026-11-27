package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.liar.LiarRemote;

import sa.urtv.screensaver.games.author.AuthorGame;
import sa.urtv.screensaver.games.liar.LiarGame;

/**
 * «واحد يكذب» (docs/GAMES-NEW.md): three players tell a short story on one topic, one of the three is made up; the others question them and vote on the liar. The
 * phones carry the secret roles (a true story, or «اكذب»). 4 to 10 phones. The rules are LiarGame (no Android); the topics are the bundled bank assets/games/liar.gb.
 */
public class LiarActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, DEAL, TELL, VOTE, REVEAL, END }
    static final long VOTE_MS = 30_000, REVEAL_MS = 9_000, DEAL_MS = 25_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<AuthorGame.Prompt> bank; private LiarGame game; private GameScore score; private String[] names = new String[0];
    private boolean remote; private int dealK, voteSel;   // «اللعب بالريموت بس»: the remote goes to each teller (hand-over), the vote is a show of hands
    private long phaseAt, tellMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "liar"; }
    @Override protected String namesKind() { return remoteOn(chips) ? "players" : null; }
    @Override protected int namesCount() { return LiarRemote.MIN_PLAYERS + chips.picked("players"); }
    @Override protected boolean inPlay() { return screen == Screen.DEAL || screen == Screen.TELL || screen == Screen.VOTE || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = AuthorGame.parse(GameBundles.load(this, "liar"));
        String[] rs = new String[LiarGame.ROUNDS.length], secs = new String[LiarGame.TELL_SECONDS.length];
        for (int i = 0; i < rs.length; i++) rs[i] = num(LiarGame.ROUNDS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(LiarGame.TELL_SECONDS[i]));
        String[] ps = new String[LiarGame.MAX_PLAYERS - LiarRemote.MIN_PLAYERS + 1]; for (int i = 0; i < ps.length; i++) ps[i] = num(LiarRemote.MIN_PLAYERS + i);
        chips = new GameChips().add("rounds", S.t("الجولات"), rs, 1).add("time", S.t("وقت الحكاية"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0).add("players", S.t("اللاعبون"), ps, 0);
        remoteChip(chips);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() { if (game != null) { List<String> r = new ArrayList<>(game.asked()); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }

    private void start() {
        if (bank == null) { toast(S.t("قريبًا")); return; }
        if (remoteOn(chips)) { startRemote(); return; }
        remote = false;
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < LiarGame.MIN_PLAYERS) return;
        if (n > LiarGame.MAX_PLAYERS) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        List<AuthorGame.Prompt> picks = AuthorGame.pick(bank, LiarGame.ROUNDS[chips.picked("rounds")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new LiarGame(n, picks, rnd); score = new GameScore(n);
        tellMs = LiarGame.TELL_SECONDS[chips.picked("time")] * 1000L;
        toDeal();
    }
    /** one remote: the players are numbered (or named by the people), the remote goes round for the secret roles, the room votes by hands */
    private void startRemote() {
        int n = LiarRemote.MIN_PLAYERS + chips.picked("players"); remote = true; closePhones();
        names = playerNames(n);
        List<AuthorGame.Prompt> picks = AuthorGame.pick(bank, LiarGame.ROUNDS[chips.picked("rounds")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new LiarGame(n, picks, rnd); score = new GameScore(n);
        tellMs = LiarGame.TELL_SECONDS[chips.picked("time")] * 1000L;
        toDeal();
    }
    /** the remote goes to each of the three tellers in turn: the role is shown to that player alone, then the telling starts */
    private void dealNext() {
        if (game == null || game.phase != LiarGame.Phase.DEAL) return;
        int[] t = game.tellers();
        if (dealK >= t.length) { toTell(); return; }
        final int p = t[dealK];
        handOver(names[p], game.liarIs(p) ? S.t("اكذب: اخترع قصة") : S.t("احكِ قصة حقيقية"), S.t("الموضوع: %s", game.topic().text(en)), () -> { dealK++; dealNext(); });
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    /** the three tellers get their secret on their own phone (a true story, or «اكذب»); the others wait */
    private void toDeal() {
        if (game.phase == LiarGame.Phase.END) { toEnd(); return; }
        screen = Screen.DEAL; phaseAt = System.currentTimeMillis();
        if (remote) { dealK = 0; dealNext(); redraw(); return; }
        String topic = game.topic().text(en);
        for (int i = 0; i < game.players; i++) {
            if (!game.isTeller(i)) phones.room.hold(i, S.t("%s يحكون، استعدوا للأسئلة", tellersText()));
            else if (game.liarIs(i)) phones.room.card(i, GamePhoneRoom.SECRET, names[i], S.t("اكذب: اخترع قصة"), S.t("الموضوع: %s", topic), null, null, -1);
            else phones.room.card(i, GamePhoneRoom.SECRET, names[i], S.t("احكِ قصة حقيقية"), S.t("الموضوع: %s", topic), null, null, -1);
        }
        redraw();
    }
    private String tellersText() { StringBuilder b = new StringBuilder(); for (int t : game.tellers()) b.append(b.length() > 0 ? (en ? ", " : "، ") : "").append(names[t]); return b.toString(); }
    private void toTell() {
        game.startTelling(); screen = Screen.TELL; phaseAt = System.currentTimeMillis();
        if (!remote) for (int i = 0; i < game.players; i++) phones.room.hold(i, game.isTeller(i) ? S.t("احكِ قصتك، وجاوب على الأسئلة") : S.t("اسألوهم، ومن يكذب؟"));
        redraw();
    }
    private void toVote() {
        game.endTelling(); screen = Screen.VOTE; phaseAt = System.currentTimeMillis();
        if (remote) { voteSel = 0; redraw(); return; }
        int[] t = game.tellers(); String[] opts = new String[3]; for (int i = 0; i < 3; i++) opts[i] = names[t[i]];
        for (int i = 0; i < game.players; i++) {
            if (game.isTeller(i)) { phones.room.hold(i, S.t("الجميع يصوّت عليكم")); continue; }
            // the pick card offers the three tellers: their numbers in the names list are the options 0 to 2, mapped back to players on the TV
            phones.room.card(i, GamePhoneRoom.PICK, "", S.t("مين يكذب؟"), "", opts, new int[] {0, 1, 2}, -1);
        }
        redraw();
    }
    private void toReveal() {
        game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        if (phones != null) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null) return;
        if (remote) { if (screen == Screen.TELL) { long left = tellMs - (System.currentTimeMillis() - phaseAt); if (left <= 0) { toVote(); return; } if (timer != null) timer.set((int) ((left + 999) / 1000)); } return; }
        if (!onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.DEAL) {
            boolean all = true;
            for (int t : game.tellers()) all &= phones.room.replied(t);
            if (all || age >= DEAL_MS) toTell();
        } else if (screen == Screen.TELL) {
            if (age >= tellMs) { toVote(); return; }
            if (timer != null) timer.set((int) ((tellMs - age + 999) / 1000));
        } else if (screen == Screen.VOTE) {
            int[] t = game.tellers();
            for (int p = 0; p < game.players; p++) if (!game.isTeller(p)) { int a = phones.room.reply(p); if (a >= 0 && a < 3) game.vote(p, t[a]); }
            if (game.allVoted() || age >= VOTE_MS) toReveal();
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) { game.next(); toDeal(); }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case DEAL: if (back) openPause(); else if (ok) toTell(); return;
            case TELL: if (back) openPause(); else if (ok) toVote(); return;
            case VOTE:
                if (back) openPause();
                else if (remote) {   // the room points on the count; the host picks the teller they chose, or «ما اتفقنا»
                    if (ok) { int[] t = game.tellers(); LiarRemote.applyRoom(game, voteSel < 3 ? t[voteSel] : -1); toReveal(); }
                    else if (dx != 0) { voteSel = Math.max(0, Math.min(3, voteSel + dx)); redraw(); }
                } else if (ok) toReveal();
                return;
            case REVEAL: if (back) openPause(); else if (ok) { game.next(); toDeal(); } return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); game = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); game = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(LiarGame.MIN_PLAYERS, LiarGame.MAX_PLAYERS); break;
            case DEAL:
                question(S.t("الجولة %s من %s", num(game.index + 1), num(game.total)), game.topic().text(en), 480);
                root.addView(centered(tellersText(), 72, L.ink, true), at(1728, 110, 96, 540));
                root.addView(centered(S.t("شوفوا أدواركم في جوالاتكم، واحد منكم يكذب"), 52, L.ink(0xCC), true), at(1728, 80, 96, 680));
                hints(S.t("اختيار"), S.t("ابدأوا الحكي"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case TELL:
                question(S.t("الجولة %s من %s", num(game.index + 1), num(game.total)), game.topic().text(en), 480);
                timer = new TickTimer((int) (tellMs / 1000), 428); timer.set((int) ((tellMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(tellersText(), 72, L.ink, true), at(1728, 110, 96, 540));
                root.addView(centered(S.t("كل واحد يحكي قصته، والباقي يسألون"), 56, L.ink(0xCC), true), at(1728, 80, 96, 680));
                if (remote) scoreBar(score, names, -1, 820); else board(score, names, false);
                hints(S.t("اختيار"), S.t("نصوّت"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case VOTE:
                if (remote) {
                    root.addView(centered(S.t("مين اللي يكذب؟"), 88, L.ink, true), at(1728, 150, 96, 96));
                    root.addView(centered(S.t("على العدّ، الكل يشير للي يظنه. المضيف يختار الأكثر إشارة."), 48, L.ink(0xCC), false), at(1728, 90, 96, 250));
                    int[] tl = game.tellers(); float cw = 480;
                    for (int i = 0; i < 3; i++) root.addView(focusCard(names[tl[i]], 64, voteSel == i), at(cw, 240, 192 + i * (cw + 48), 420));
                    root.addView(focusCard(S.t("ما اتفقنا"), 52, voteSel == 3), at(672, 112, 624, 740));
                    hints("◀ ▶", S.t("اختاروا"), S.t("اختيار"), S.t("هذا اللي اخترناه"), S.t("رجوع"), S.t("إيقاف"));
                    break;
                }
                question(S.t("مين يكذب؟"), tellersText(), 330);
                root.addView(centered(S.t("صوّت %s من %s", num(game.votes()), num(game.players - 3)), 64, L.ink, true), at(1728, 90, 96, 460));
                hints(S.t("اختيار"), S.t("اكشف الكذّاب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL: {
                question(S.t("الكذّاب"), names[game.liar()], 330);
                StringBuilder got = new StringBuilder();
                for (int p = 0; p < game.players; p++) if (game.voteOf(p) == game.liar()) got.append(got.length() > 0 ? (en ? ", " : "، ") : "").append(names[p]);
                root.addView(centered(got.length() == 0 ? S.t("ما أحد عرفه") : S.t("عرفوه: %s", got.toString()), 60, L.ink, true), at(1728, 90, 96, 460));
                root.addView(centered(S.t("+%s للكذّاب", num(game.gained[game.liar()])), 52, L.ink(0xCC), true), at(1728, 80, 96, 580));
                if (remote) scoreBar(score, names, -1, 820); else board(score, names, false);
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
}
