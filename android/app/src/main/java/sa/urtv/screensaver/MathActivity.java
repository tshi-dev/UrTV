package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.maths.MathGame;

/**
 * «سباق الحساب» (docs/GAMES-NEW.md): quick mental arithmetic, made by code, in both languages at once on the TV. Everyone types the answer on their phone (a number pad);
 * the first right answer scores most. 2 to 8 phones. The rules and the questions are MathGame (no Android); there is no bank, so nothing can repeat.
 */
public class MathActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, END }
    static final long REVEAL_MS = 11_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private MathGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "maths"; }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String[] qs = new String[MathGame.QUESTIONS.length], secs = new String[MathGame.SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(MathGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(MathGame.SECONDS[i]));
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 0).add("level", S.t("المستوى"), new String[] {S.t("سهل"), S.t("متوسط"), S.t("صعب")}, 1).add("time", S.t("الوقت"), secs, 1);
        remembered(soundChip(chips));
    }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }

    private void start() {
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < MathGame.MIN_PLAYERS) return;
        if (n > MathGame.MAX_PLAYERS) { toast(S.t("اللعبة تتسع لثمانية لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        game = new MathGame(n, MathGame.generate(chips.picked("level") + 1, MathGame.QUESTIONS[chips.picked("count")], rnd)); score = new GameScore(n);
        askMs = MathGame.SECONDS[chips.picked("time")] * 1000L;
        toAsk();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toAsk() {
        if (game.phase == MathGame.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis();
        MathGame.Question q = game.question(); String shown = DigitsText.convert(q.expr, latin);
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.TEXT, "", shown, q.ar + "  " + q.en, new String[] {S.t("الجواب")}, null, 1);   // self = 1: the phone shows a number pad
        redraw();
    }
    private void toReveal() {
        game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.ASK) {
            for (int p = 0; p < game.players; p++) { String[] t = phones.room.typed(p); if (t != null) game.submit(p, t[0], now); }
            if (game.allIn() || age >= askMs) { toReveal(); return; }
            if (timer != null) timer.set((int) ((askMs - age + 999) / 1000));
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) { game.next(); toAsk(); }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case ASK: if (back) openPause(); else if (ok) toReveal(); return;
            case REVEAL: if (back) openPause(); else if (ok) { game.next(); toAsk(); } return;
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
            case LOBBY: drawPhoneLobby(MathGame.MIN_PLAYERS, MathGame.MAX_PLAYERS); break;
            case ASK: {
                MathGame.Question q = game.question();
                question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total)), DigitsText.convert(q.expr, latin), 480);
                timer = new TickTimer((int) (askMs / 1000), 428); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(q.ar + "     " + q.en, 64, L.ink, true), at(1728, 90, 96, 540));
                root.addView(centered(S.t("أجاب %s من %s", num(game.answered()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 650));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case REVEAL: {
                question(S.t("الجواب"), numberText(game.question().answer), 330);
                List<Integer> r = new ArrayList<>(); for (int p = 0; p < game.players; p++) r.add(p); r.sort((x, y) -> game.gained[y] - game.gained[x]);
                for (int k = 0; k < r.size() && k < 6; k++) {
                    int p = r.get(k);
                    String line = names[p] + ": " + (game.gave(p) ? numberText(game.givenBy(p)) : "-") + (game.gained[p] > 0 ? "  +" + num(game.gained[p]) : "");
                    root.addView(text(clip(line, 56, 1100), 56, game.gained[p] > 0 ? L.ink : L.ink(0x99), true), at(1728, 76, 96, 380 + k * 84));
                }
                if (r.isEmpty()) root.addView(centered(S.t("ما أحد أجاب"), 64, L.ink(0xCC), true), at(1728, 90, 96, 480));
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
    private String numberText(long v) { return groups(v); }
    /** 9372610 -> 9 372 610 in the person's digits (a thin space between the groups of three) */
    private String groups(long v) {
        String s = String.valueOf(v), out = "";
        for (int i = 0; i < s.length(); i++) { if (i > 0 && (s.length() - i) % 3 == 0) out += " "; out += s.charAt(i); }
        return DigitsText.convert(out, latin);
    }
}
