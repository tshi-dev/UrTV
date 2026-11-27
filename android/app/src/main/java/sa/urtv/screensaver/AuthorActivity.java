package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.author.AuthorGame;

/**
 * «من كاتبها؟» (docs/GAMES-NEW.md): a question, everyone types an answer on their phone, the answers come up without names and everyone votes who wrote each.
 * The phones are the game (3 to 10), so «ابدأ» opens the lobby. The rules and the prompts are AuthorGame (no Android); the prompts are the bundled bank
 * assets/games/author.gb (GameBundle), nothing repeats on this TV.
 */
public class AuthorActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, WRITE, VOTE, REVEAL, END }
    static final long VOTE_MS = 25_000, REVEAL_MS = 9_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<AuthorGame.Prompt> bank; private AuthorGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, writeMs; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "author"; }
    @Override protected boolean inPlay() { return screen == Screen.WRITE || screen == Screen.VOTE || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = AuthorGame.parse(GameBundles.load(this, "author"));
        String[] qs = new String[AuthorGame.QUESTIONS.length], secs = new String[AuthorGame.WRITE_SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(AuthorGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(AuthorGame.WRITE_SECONDS[i]));
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 1).add("time", S.t("وقت الكتابة"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() { if (game != null) { List<String> r = new ArrayList<>(game.asked()); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }

    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private void start() {
        if (bank == null) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < AuthorGame.MIN_PLAYERS) return;
        if (n > AuthorGame.MAX_PLAYERS) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        List<AuthorGame.Prompt> picks = AuthorGame.pick(bank, AuthorGame.QUESTIONS[chips.picked("count")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new AuthorGame(n, picks, rnd); score = new GameScore(n);
        writeMs = AuthorGame.WRITE_SECONDS[chips.picked("time")] * 1000L;
        toWrite();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toWrite() {
        if (game.phase == AuthorGame.Phase.END) { toEnd(); return; }
        screen = Screen.WRITE; phaseAt = System.currentTimeMillis();
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.TEXT, "", game.prompt().text(en), S.t("اكتب إجابة قصيرة، ولا أحد يعرف أنها لك"), new String[] {S.t("إجابتك")}, null, -1);
        redraw();
    }
    private void toVote() {
        if (game.phase == AuthorGame.Phase.WRITE) { toWrite(); return; }   // fewer than two answers: the question was skipped
        if (game.phase == AuthorGame.Phase.END) { toEnd(); return; }
        screen = Screen.VOTE; phaseAt = System.currentTimeMillis();
        String[] opts = names;
        for (int i = 0; i < game.players; i++) {
            if (i == game.shownWriter()) phones.room.hold(i, S.t("هذي إجابتك، لا تصوّت"));
            else {
                int[] acts = new int[game.players - 1]; int k = 0;
                for (int j = 0; j < game.players; j++) if (j != i) acts[k++] = j;
                phones.room.card(i, GamePhoneRoom.PICK, "", S.t("من كتب: %s؟", game.answer()), "", opts, acts, -1);
            }
        }
        redraw();
    }
    private void toReveal() {
        game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void afterReveal() { game.next(); if (game.phase == AuthorGame.Phase.VOTE) toVote(); else if (game.phase == AuthorGame.Phase.WRITE) toWrite(); else toEnd(); }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    private long pausedAt;
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.WRITE) {
            for (int p = 0; p < game.players; p++) { String[] t = phones.room.typed(p); if (t != null) game.submit(p, t[0]); }
            if (game.allSubmitted() || age >= writeMs) { game.endWrite(); toVote(); return; }
            if (timer != null) timer.set((int) ((writeMs - age + 999) / 1000));
        } else if (screen == Screen.VOTE) {
            for (int p = 0; p < game.players; p++) { int a = phones.room.reply(p); if (a >= 0) game.vote(p, a); }
            if (game.allVoted() || age >= VOTE_MS) toReveal();
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) afterReveal();
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case WRITE: if (back) openPause(); else if (ok) { game.endWrite(); toVote(); } return;
            case VOTE: if (back) openPause(); else if (ok) toReveal(); return;
            case REVEAL: if (back) openPause(); else if (ok) afterReveal(); return;
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
            case LOBBY: drawPhoneLobby(AuthorGame.MIN_PLAYERS, AuthorGame.MAX_PLAYERS); break;
            case WRITE:
                question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total)), game.prompt().text(en), 480);
                timer = new TickTimer((int) (writeMs / 1000), 428); timer.set((int) ((writeMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اكتبوا إجاباتكم من جوالاتكم"), 64, L.ink, true), at(1728, 90, 96, 540));
                root.addView(centered(S.t("كتب %s من %s", num(game.submitted()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 650));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("كفاية، نشوف الإجابات"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case VOTE: {
                question(S.t("إجابة %s من %s", num(game.shown + 1), num(game.answersCount())), S.t("من كتبها؟"), 330);
                String a = game.answer();
                bigTile(a);
                root.addView(centered(S.t("صوّت %s من %s", num(game.votes()), num(game.players - 1)), 56, L.ink(0xCC), true), at(1728, 80, 96, 800));
                hints(S.t("اختيار"), S.t("اكشف الكاتب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case REVEAL: {
                question(S.t("الكاتب"), names[game.writer()], 330);
                bigTile(game.answer());
                StringBuilder got = new StringBuilder();
                for (int p = 0; p < game.players; p++) if (game.voteOf(p) == game.writer()) got.append(got.length() > 0 ? (en ? ", " : "، ") : "").append(names[p]);
                root.addView(centered(got.length() == 0 ? S.t("ما أحد عرفه") : S.t("عرفوه: %s", got.toString()), 52, L.ink, true), at(1728, 80, 96, 760));
                root.addView(centered(S.t("+%s للكاتب", num(game.gained[game.writer()])), 52, L.ink(0xCC), true), at(1728, 80, 96, 850));
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }

    /** the answer in a big tile under the green block */
    private void bigTile(String text) {
        android.widget.FrameLayout t = stamp(L.tile, L.ink, 6, false);
        android.widget.TextView v = centered(text, text.length() <= 14 ? 120 : text.length() <= 24 ? 90 : 66, L.ink, true); v.setMaxLines(2);
        t.addView(v, new android.widget.FrameLayout.LayoutParams(-1, -1));
        root.addView(t, at(1728, 330, 96, 400));
    }
}
