package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.emoji.EmojiGame;
import sa.urtv.screensaver.games.quran.QuranItems;

/**
 * «من هو القارئ؟» (docs/GAMES-QURAN.md): a short clip of a recitation plays on the TV, everyone picks the reciter among four. HIDDEN: it is not in GameRegistry until
 * mp3quran's terms are read and allow streaming a short clip (see the doc). Nothing is stored or bundled: the clip is streamed at play time from the same files the app's
 * «تلاوة» already plays (the first seconds of a short surah), and stops after QuranItems.CLIP_SECONDS. 2 to 10 phones; nothing repeats on this TV.
 */
public class QuranReciterActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, END }
    static final long REVEAL_MS = 7_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<EmojiGame.Puzzle> bank; private EmojiGame game; private GameScore score; private String[] names = new String[0];
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "reciter"; }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try (java.io.InputStream in = getAssets().open("quran/reciters.json")) { bank = QuranItems.clips(MiniJson.parse(new String(Bounded.readAll(in, 1_000_000), java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { bank = null; }
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
    private android.media.MediaPlayer mp; private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable stopper = this::stopClip;
    private void stopClip() { handler.removeCallbacks(stopper); if (mp != null) { try { mp.release(); } catch (RuntimeException e) { } mp = null; } }
    /** streams the clip's first seconds; any failure is silence (the question can still be answered by guessing), never a crash */
    private void playClip(String url) {
        stopClip();
        try {
            mp = new android.media.MediaPlayer(); mp.setAudioAttributes(new android.media.AudioAttributes.Builder().setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(android.media.AudioAttributes.USAGE_MEDIA).build());
            mp.setDataSource(url); mp.setOnPreparedListener(m -> { m.start(); handler.postDelayed(stopper, QuranItems.CLIP_SECONDS * 1000L); }); mp.setOnErrorListener((m, w, e) -> { stopClip(); return true; }); mp.prepareAsync();
        } catch (Exception e) { stopClip(); }
    }
    @Override protected void onDestroy() { stopClip(); super.onDestroy(); }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toAsk() {
        if (game.phase == EmojiGame.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis();
        String[] opts = game.options().toArray(new String[0]); int[] acts = new int[opts.length]; for (int i = 0; i < acts.length; i++) acts[i] = i;
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.PICK, "", S.t("استمعوا"), S.t("اختر القارئ"), opts, acts, -1);
        playClip(game.puzzle().emoji); redraw();
    }
    private void toReveal() {
        stopClip(); game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { stopClip(); keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) { stopClip(); pausedAt = now; } else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
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
                question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total)), S.t("من هو القارئ؟"), 520);
                timer = new TickTimer((int) (askMs / 1000), 470); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اختاروا القارئ من جوالاتكم"), 60, L.ink, true), at(1728, 90, 96, 570));
                root.addView(centered(S.t("أجاب %s من %s", num(game.answered()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 690));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL: {
                question(S.t("القارئ"), game.puzzle().answer(en), 420);
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
