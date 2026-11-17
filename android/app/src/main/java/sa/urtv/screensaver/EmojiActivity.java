package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.emoji.EmojiGame;

/**
 * «اقرأ الإيموجي» (docs/GAMES-NEW.md): a line of emoji stands for a word; everyone picks the right one of four on their phone, the first right answer scores most.
 * 2 to 10 phones. The rules and the puzzles are EmojiGame (no Android); the puzzles are the bundled bank assets/games/emoji.gb; nothing repeats on this TV.
 */
public class EmojiActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, END }
    static final long REVEAL_MS = 7_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<EmojiGame.Puzzle> bank; private EmojiGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "emoji"; }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = EmojiGame.parse(GameBundles.load(this, "emoji"));
        String[] qs = new String[EmojiGame.QUESTIONS.length], secs = new String[EmojiGame.SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(EmojiGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(EmojiGame.SECONDS[i]));
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 0).add("time", S.t("الوقت"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
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
        if (n < EmojiGame.MIN_PLAYERS) return;
        if (n > EmojiGame.MAX_PLAYERS) { toast(S.t("اللعبة تتسع لعشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        boolean kids = chips.picked("kids") == 1;
        List<EmojiGame.Puzzle> picks = EmojiGame.pick(bank, EmojiGame.QUESTIONS[chips.picked("count")], kids, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new EmojiGame(n, picks, bank, en, rnd); score = new GameScore(n);
        askMs = EmojiGame.SECONDS[chips.picked("time")] * 1000L;
        toAsk();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toAsk() {
        if (game.phase == EmojiGame.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis();
        String[] opts = game.options().toArray(new String[0]); int[] acts = new int[opts.length]; for (int i = 0; i < acts.length; i++) acts[i] = i;
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.PICK, "", game.puzzle().emoji, S.t("اختر الكلمة"), opts, acts, -1);
        redraw();
    }
    private void toReveal() {
        game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.ASK) {
            for (int p = 0; p < game.players; p++) { int a = phones.room.reply(p); if (a >= 0) game.answer(p, a, phones.room.repliedAt(p)); }
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
            case LOBBY: drawPhoneLobby(EmojiGame.MIN_PLAYERS, EmojiGame.MAX_PLAYERS); break;
            case ASK:
                question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total)), game.puzzle().emoji, 520);
                timer = new TickTimer((int) (askMs / 1000), 470); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اختاروا الكلمة من جوالاتكم"), 60, L.ink, true), at(1728, 90, 96, 570));
                root.addView(centered(S.t("أجاب %s من %s", num(game.answered()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 690));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL: {
                question(game.puzzle().emoji, game.puzzle().answer(en), 420);
                StringBuilder got = new StringBuilder();
                for (int p = 0; p < game.players; p++) if (game.wasRight(p)) got.append(got.length() > 0 ? (en ? ", " : "، ") : "").append(names[p]).append(game.gained[p] == 3 ? " +3" : game.gained[p] == 2 ? " +2" : " +1");
                root.addView(centered(got.length() == 0 ? S.t("ما أحد عرفها") : got.toString(), 52, L.ink, true), at(1728, 140, 96, 470));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
}
