package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.emoji.EmojiGame;
import sa.urtv.screensaver.games.logic.LogicItems;

/**
 * «ألغاز المنطق» (docs/GAMES-NEW.md): logic puzzles in both languages at once (who is the tallest, the next number, the odd one out, ages, and hand-written riddles);
 * everyone picks the right one of four on their phone, the first right answer scores most. 2 to 8 phones. Most puzzles are made by code (LogicItems, no Android) and
 * are always right; the hand-written ones are the bundle assets/games/logic.gb, and nothing from it repeats on this TV.
 */
public class LogicActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, END }
    static final long REVEAL_MS = 7_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<EmojiGame.Puzzle> bank; private EmojiGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "logic"; }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = LogicItems.bank(GameBundles.load(this, "logic"));
        String[] qs = new String[EmojiGame.QUESTIONS.length], secs = new String[EmojiGame.SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(EmojiGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(EmojiGame.SECONDS[i]));
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 0).add("time", S.t("الوقت"), secs, 1);
        remoteChip(chips);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() { if (game != null) { List<String> r = new ArrayList<>(); for (String x : game.asked()) if (x.startsWith("lg-") && !x.startsWith("lg-order-") && !x.startsWith("lg-seq-") && !x.startsWith("lg-odd-") && !x.startsWith("lg-age-")) r.add(x); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }

    private void start() {
        if (remoteOn(chips)) { startActivity(remoteIntent(CallAloudActivity.class, chips).putExtra("src", "logic")); finish(); return; }   // «اللعب بالريموت بس»: the answers are called aloud (CallAloudActivity)
        if (bank == null) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < EmojiGame.MIN_PLAYERS) return;
        if (n > 8) { toast(S.t("اللعبة تتسع لثمانية لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        int total = EmojiGame.QUESTIONS[chips.picked("count")], fromBank = Math.min(total * 2 / 5, bank.size());
        List<EmojiGame.Puzzle> picks = new ArrayList<>(EmojiGame.pick(bank, fromBank, false, new HashSet<>(GamePrefs.recent(this, id())), rnd));
        picks.addAll(LogicItems.generate(total - picks.size(), rnd)); Collections.shuffle(picks, rnd);
        game = new EmojiGame(n, picks, picks, en, rnd); score = new GameScore(n);
        askMs = EmojiGame.SECONDS[chips.picked("time")] * 1000L;
        toAsk();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toAsk() {
        if (game.phase == EmojiGame.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis();
        String[] opts = new String[game.options().size()]; for (int i = 0; i < opts.length; i++) opts[i] = DigitsText.convert(game.options().get(i), latin); int[] acts = new int[opts.length]; for (int i = 0; i < acts.length; i++) acts[i] = i;
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.PICK, "", DigitsText.convert(game.puzzle().emoji, latin).replace(LogicItems.SEP, '\n'), S.t("اختر الجواب"), opts, acts, -1);
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
            case LOBBY: drawPhoneLobby(EmojiGame.MIN_PLAYERS, 8); break;
            case ASK:
                question(S.t("اللغز %s من %s", num(game.index + 1), num(game.total)), "", 520);
                String[] both = DigitsText.convert(game.puzzle().emoji, latin).split(String.valueOf(LogicItems.SEP), 2);
                android.widget.TextView ta = centered(both[0], 50, L.onDeep, true), te = centered(both.length > 1 ? both[1] : "", 40, L.onDeep, false); ta.setMaxLines(3); te.setMaxLines(3);
                root.addView(ta, at(1400, 170, 96, 112)); root.addView(te, at(1400, 140, 96, 290));
                timer = new TickTimer((int) (askMs / 1000), 470); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اختاروا الجواب من جوالاتكم"), 60, L.ink, true), at(1728, 90, 96, 570));
                root.addView(centered(S.t("أجاب %s من %s", num(game.answered()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 690));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL: {
                question(S.t("الجواب"), DigitsText.convert(game.puzzle().answer(en), latin), 420);
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
