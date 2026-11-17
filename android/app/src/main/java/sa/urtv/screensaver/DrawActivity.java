package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.charades.CharadesWords;
import sa.urtv.screensaver.games.draw.DrawGame;

/**
 * «ارسم وخمّن» (docs/GAMES-NEW.md): one phone draws a word, the TV shows the strokes as they come (the phone server's /api/draw, the only route that takes more than a
 * kilobyte), the other phones type guesses. 3 to 10 phones. The rules are DrawGame (no Android); the words are the charades bank, nothing repeats on this TV.
 */
public class DrawActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, DRAW, REVEAL, END }
    static final long REVEAL_MS = 9_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<CharadesWords.Word> words; private CharadesWords.Deck deck; private DrawGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, roundMs, pausedAt; private TickTimer timer; private int sel, shownRev = -1; private Board board; private final Random rnd = new Random();

    @Override protected String id() { return "draw"; }
    @Override protected boolean inPlay() { return screen == Screen.DRAW || screen == Screen.REVEAL; }

    /** the drawing: the strokes of the drawer's card, scaled from the phone's 0 to 1000 to the board */
    private final class Board extends View {
        private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG); private List<int[]> strokes = new ArrayList<>();
        Board(Context c) { super(c); pen.setStyle(Paint.Style.STROKE); pen.setStrokeCap(Paint.Cap.ROUND); pen.setStrokeJoin(Paint.Join.ROUND); }
        void set(List<int[]> s) { strokes = s; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            c.drawColor(L.paper); pen.setColor(L.ink); pen.setStrokeWidth(Math.max(4f, getWidth() / 90f));
            float sx = getWidth() / 1000f, sy = getHeight() / 1000f;
            for (int[] st : strokes) {
                if (st.length < 2) continue;
                Path p = new Path(); p.moveTo(st[0] * sx, st[1] * sy);
                if (st.length == 2) p.lineTo(st[0] * sx + 0.1f, st[1] * sy);
                for (int i = 2; i + 1 < st.length; i += 2) p.lineTo(st[i] * sx, st[i + 1] * sy);
                c.drawPath(p, pen);
            }
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        words = GameData.load(this, "charades", CharadesActivity.PUBLISHED, CharadesActivity.STARTER);
        String[] secs = new String[DrawGame.SECONDS.length], tn = new String[DrawGame.TURNS.length];
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(DrawGame.SECONDS[i]));
        for (int i = 0; i < tn.length; i++) tn[i] = num(DrawGame.TURNS[i]);
        chips = new GameChips().add("turns", S.t("أدوار كل لاعب"), tn, 0).add("time", S.t("الوقت"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() { if (deck != null) { List<String> r = deck.recent(); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }

    private void start() {
        if (words == null) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < DrawGame.MIN_PLAYERS) return;
        if (n > DrawGame.MAX_PLAYERS) { toast(S.t("اللعبة تتسع لعشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id())); Collections.reverse(recent);
        deck = new CharadesWords.Deck(words, true, chips.picked("kids") == 1, recent, rnd);
        game = new DrawGame(n, DrawGame.TURNS[chips.picked("turns")]); score = new GameScore(n);
        roundMs = DrawGame.SECONDS[chips.picked("time")] * 1000L;
        toDraw();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }
    private void guessCard(int p, String note) { phones.room.card(p, GamePhoneRoom.TEXT, names[game.drawer()], S.t("خمّن الرسمة"), note, new String[] {S.t("تخمينك")}, null, -1); }

    private void toDraw() {
        if (game.phase == DrawGame.Phase.END) { toEnd(); return; }
        CharadesWords.Word w = deck.draw();
        if (w == null) { toEnd(); return; }
        game.setWord(w.text[0], w.text[1]); screen = Screen.DRAW; phaseAt = System.currentTimeMillis(); shownRev = -1;
        int d = game.drawer();
        for (int i = 0; i < game.players; i++) {
            if (i == d) phones.room.card(i, GamePhoneRoom.DRAW, S.t("ارسم"), w.text(en), S.t("لا تكتب حروفًا ولا أرقامًا"), null, null, -1);
            else guessCard(i, "");
        }
        redraw();
    }
    private void toReveal() {
        game.endRound(); sync(); keep(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(game.correct > 0 ? QuizSound.CHIME : QuizSound.LOW);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.DRAW) {
            int d = game.drawer();
            for (int p = 0; p < game.players; p++) {
                if (p == d || game.isRight(p)) continue;
                String[] t = phones.room.typed(p);
                if (t == null) continue;
                if (game.guess(p, t[0])) { phones.room.hold(p, S.t("صح! %s", game.word)); play(QuizSound.CHIME); redraw(); }
                else guessCard(p, S.t("ليست هذه، جرّب غيرها"));
            }
            int rev = phones.room.drawRev(d);
            if (board != null && rev != shownRev) { shownRev = rev; board.set(phones.room.strokes(d)); }
            if (game.allRight() || age >= roundMs) { toReveal(); return; }
            if (timer != null) timer.set((int) ((roundMs - age + 999) / 1000));
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) { game.next(); toDraw(); }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case DRAW: if (back) openPause(); else if (ok) toReveal(); return;
            case REVEAL: if (back) openPause(); else if (ok) { game.next(); toDraw(); } return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); game = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); game = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        timer = null; board = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(DrawGame.MIN_PLAYERS, DrawGame.MAX_PLAYERS); break;
            case DRAW: {
                question(S.t("الدور %s من %s", num(game.round + 1), num(game.rounds())), S.t("%s يرسم، والباقي يخمّن", names[game.drawer()]), 300);
                timer = new TickTimer((int) (roundMs / 1000), 258); timer.set((int) ((roundMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                board = new Board(this); root.addView(board, at(1000, 640, 96, 340)); shownRev = -1;
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < game.players; i++) if (game.isRight(i)) b.append(b.length() > 0 ? (en ? ", " : "، ") : "").append(names[i]);
                root.addView(centered(S.t("خمّنوا من جوالاتكم"), 52, L.ink, true), at(560, 80, 1264, 360));
                root.addView(centered(b.length() == 0 ? S.t("ما أحد خمّن بعد") : S.t("خمّنوا: %s", b.toString()), 44, L.ink(0xCC), true), at(560, 300, 1264, 460));
                hints(S.t("اختيار"), S.t("اكشف الكلمة"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case REVEAL: {
                question(S.t("الكلمة"), game.word, 330);
                for (int i = 0, k = 0; i < game.players; i++) if (game.gained[i] > 0) { root.addView(text(clip(names[i] + "  +" + num(game.gained[i]), 56, 1100), 56, L.ink, true), at(1728, 76, 96, 420 + k * 84)); k++; }
                if (game.correct == 0) root.addView(centered(S.t("ما أحد خمّن"), 64, L.ink(0xCC), true), at(1728, 90, 96, 480));
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
}
