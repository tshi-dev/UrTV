package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.guess.GuessGame;

/**
 * «كم تقدر؟» (docs/GAMES-NEW.md): a topic («فواكه») is shown; each phone bids how many things of it the player can name; the highest bid types them on their phone, and each
 * name is checked against the topic's list. The phones are the game (2 to 10). The rules and the topics are GuessGame (no Android); the topics are the bundled bank
 * assets/games/guess.gb, nothing repeats on this TV. Points only.
 */
public class GuessActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, BID, ANSWER, REVEAL, END }
    static final long REVEAL_MS = 12_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<GuessGame.Topic> bank; private GuessGame game; private GameScore score; private String[] names = new String[0]; private String[] typed = new String[0];
    private long phaseAt, bidMs, answerMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "guess"; }
    @Override protected boolean inPlay() { return screen == Screen.BID || screen == Screen.ANSWER || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = GuessGame.parse(GameBundles.load(this, "guess"));
        String[] qs = new String[GuessGame.ROUNDS.length], secs = new String[GuessGame.ANSWER_SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(GuessGame.ROUNDS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(GuessGame.ANSWER_SECONDS[i]));
        chips = new GameChips().add("count", S.t("الجولات"), qs, 0).add("time", S.t("وقت الكتابة"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
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
        if (n < GuessGame.MIN_PLAYERS) return;
        if (n > GuessGame.MAX_PLAYERS) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        List<GuessGame.Topic> picks = GuessGame.pick(bank, GuessGame.ROUNDS[chips.picked("count")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new GuessGame(n, picks, rnd); score = new GameScore(n);
        bidMs = GuessGame.BID_SECONDS[1] * 1000L; answerMs = GuessGame.ANSWER_SECONDS[chips.picked("time")] * 1000L;
        toBid();
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toBid() {
        if (game.phase == GuessGame.Phase.END) { toEnd(); return; }
        screen = Screen.BID; phaseAt = System.currentTimeMillis();
        GuessGame.Topic t = game.topic();
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.TEXT, "", t.text(en), S.t("كم تقدر تسمّي؟ من ١ إلى %s", num(game.cap())), new String[] {S.t("العدد")}, null, 1);   // self = 1: a number pad
        redraw();
    }
    private void toAnswer() {
        game.endBids();
        if (game.phase == GuessGame.Phase.REVEAL) { toReveal(); return; }
        screen = Screen.ANSWER; typed = new String[0]; phaseAt = System.currentTimeMillis();
        String[] f = new String[game.topBid]; for (int i = 0; i < f.length; i++) f[i] = num(i + 1);
        for (int i = 0; i < game.players; i++) {
            if (i == game.winner) phones.room.card(i, GamePhoneRoom.TEXT, "", S.t("اكتب %s من: %s", num(game.topBid), game.topic().text(en)), "", f, null, -1);
            else phones.room.hold(i, S.t("%s يكتب الآن", names[game.winner]));
        }
        redraw();
    }
    private void toReveal() {
        sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(game.succeeded() ? QuizSound.CHIME : QuizSound.LOW);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.BID) {
            for (int p = 0; p < game.players; p++) { String[] t = phones.room.typed(p); if (t != null) { Long n = sa.urtv.screensaver.games.closest.ClosestGame.number(t[0]); if (n != null && n <= GuessGame.BID_CAP) game.bid(p, (int) (long) n); } }
            if (game.allBid() || age >= bidMs) { toAnswer(); return; }
            if (timer != null) timer.set((int) ((bidMs - age + 999) / 1000));
        } else if (screen == Screen.ANSWER) {
            String[] t = phones.room.typed(game.winner);
            if (t != null) { typed = t; game.answer(Arrays.asList(t)); toReveal(); return; }
            if (age >= answerMs) { game.answer(new ArrayList<String>()); toReveal(); return; }
            if (timer != null) timer.set((int) ((answerMs - age + 999) / 1000));
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) { game.next(); toBid(); }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case BID: if (back) openPause(); else if (ok) toAnswer(); return;
            case ANSWER: if (back) openPause(); return;
            case REVEAL: if (back) openPause(); else if (ok) { game.next(); toBid(); } return;
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
            case LOBBY: drawPhoneLobby(GuessGame.MIN_PLAYERS, GuessGame.MAX_PLAYERS); break;
            case BID:
                question(S.t("الجولة %s من %s", num(game.index + 1), num(game.total)), game.topic().text(en), 480);
                timer = new TickTimer((int) (bidMs / 1000), 428); timer.set((int) ((bidMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("كم تقدر تسمّي؟ اكتبوا رقمكم في الجوال"), 60, L.ink, true), at(1728, 90, 96, 540));
                root.addView(centered(S.t("أجاب %s من %s", num(game.bidsIn()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 650));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("أغلق المزايدة"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case ANSWER:
                question(game.topic().text(en), S.t("%s يسمّي %s", names[game.winner], num(game.topBid)), 480);
                timer = new TickTimer((int) (answerMs / 1000), 428); timer.set((int) ((answerMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("أعلى مزايدة، يكتب أسماءه في جواله"), 60, L.ink, true), at(1728, 90, 96, 540));
                hints(S.t("اختيار"), "", S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL: {
                if (game.winner < 0) { question(game.topic().text(en), S.t("ما أحد قدّم رقمًا"), 330); }
                else {
                    question(game.topic().text(en), S.t("%s سمّى %s من %s", names[game.winner], num(game.counted), num(game.topBid)), 330);
                    root.addView(centered(game.succeeded() ? S.t("نجح: +%s", num(game.topBid)) : S.t("ما نجح: كل البقية +%s", num(GuessGame.FAIL_POINTS)), 64, L.ink, true), at(1728, 90, 96, 420));
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < typed.length && i < game.topBid; i++) b.append(b.length() > 0 ? (en ? ",  " : "،  ") : "").append(typed[i].isEmpty() ? "-" : typed[i]).append(game.valid(i) ? " ✓" : " ✗");
                    root.addView(centered(clip(b.toString(), 44, 1700), 44, L.ink(0xCC), true), at(1728, 70, 96, 540));
                }
                List<String> ex = game.examples(8); StringBuilder e = new StringBuilder();
                for (String s : ex) e.append(e.length() > 0 ? (en ? ",  " : "،  ") : "").append(s);
                if (e.length() > 0) root.addView(centered(S.t("من الجواب: %s", e.toString()), 40, L.ink(0x99), true), at(1728, 70, 96, 680));
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
}
