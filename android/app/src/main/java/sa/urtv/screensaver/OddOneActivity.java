package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.oddone.OddOneGame;
import sa.urtv.screensaver.games.oddone.OddOneWords;

/**
 * «واحد مو معنا» (docs/GAMES-PARTY.md, games/odd-one/SPEC.md): everyone is shown the same word but one player, who sees the topic only; one sentence each,
 * then the room points at who it thinks did not know. One remote: the words go round through the kit's hand-over («غطّوا عيونكم»), the vote is by show of
 * hands and the host enters who got the most. The rules are OddOneGame (no Android); this class only draws them with the kit's blocks.
 */
public class OddOneActivity extends GameActivity {
    static final int BYTES_MAX = 150_000;
    static final GameDataCheck.Parser<List<OddOneWords.Topic>> DATA = raw -> OddOneWords.parse(GameDataCheck.root(raw, '{'));
    private static final int[] PLAYERS = {3, 4, 5, 6, 7, 8, 9, 10};
    private static final int COLS = 5;

    private enum Screen { SETUP, LOBBY, SEEN, TALK, HANDS, ACCUSED, ODD, GUESS_ASK, GUESS_JUDGE, SCORE, END }
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<OddOneWords.Topic> topics; private OddOneGame game; private GameScore score; private String[] names;
    private int sel; private TickTimer timer; private boolean beeped; private final Random rnd = new Random();

    @Override protected String id() { return "odd-one"; }
    @Override protected boolean inPlay() { return screen != Screen.SETUP && screen != Screen.LOBBY && screen != Screen.END; }
    @Override protected String namesKind() { return "players"; }
    @Override protected int namesCount() { return PLAYERS[chips.picked("players")]; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        topics = GameData.load(this, "odd-one", DATA, DATA);
        GameData.refresh(this, "odd-one", DATA, BYTES_MAX);
        String[] players = new String[PLAYERS.length], rounds = new String[OddOneGame.ROUNDS.length];
        for (int i = 0; i < players.length; i++) players[i] = num(PLAYERS[i]);
        for (int i = 0; i < rounds.length; i++) rounds[i] = num(OddOneGame.ROUNDS[i]);
        chips = new GameChips().add("players", S.t("اللاعبين"), players, 1).add("rounds", S.t("الجولات"), rounds, 0)
            .add("laps", S.t("الكلام"), new String[]{S.t("لفّة وحدة"), S.t("لفّتين")}, 0)
            .add("phones", S.t("اللعب بالجوالات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
        chips.get("phones").picked = getIntent().getBooleanExtra("phones", false) ? 1 : 0;   // off unless the «بالجوالات» page opened this game; never kept from last time
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    /** the words this TV played last wait in the next games (ids only, the newest first) */
    private void keep() {
        if (game == null) return;
        List<String> r = game.recent(); Collections.reverse(r);
        GamePrefs.remember(this, id(), r);
    }

    private void start() {
        if (topics == null) { toast(S.t("قريبًا")); return; }
        if (chips.picked("phones") == 1) { if (!openPhones()) return; screen = Screen.LOBBY; phoneLobby = true; redraw(); return; }
        closePhones();   // the switch is off: the remote plays alone, as it always did
        begin(PLAYERS[chips.picked("players")], null);
    }
    /** the lobby's start: the phones that came in are the players, in the order they came */
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < OddOneGame.MIN_PLAYERS) return;
        if (n > OddOneGame.MAX_PLAYERS) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        begin(n, phones.room.names().toArray(new String[0]));
    }
    private void begin(int players, String[] phoneNames) {
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id())); Collections.reverse(recent);
        game = new OddOneGame(topics, players, OddOneGame.ROUNDS[chips.picked("rounds")], chips.picked("laps") + 1, false, null, recent, System::currentTimeMillis, rnd);
        score = new GameScore(game.players);
        names = new String[game.players];
        String[] own = phoneNames != null ? phoneNames : playerNames(names.length);   // the phones' own names, else what the people wrote, else «لاعب 1»
        System.arraycopy(own, 0, names, 0, names.length);
        deal();
    }
    /** the remote goes round: each player's own hand-over; under it waits the talk */
    private void deal() {
        if (phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY) { dealToPhones(); return; }
        screen = Screen.TALK; beeped = false; sel = 0;
        final OddOneGame g = game; final int round = g.round, p = g.dealPlayer();
        OddOneWords.Word w = g.secretFor(p);
        handOver(names[p], w == null ? S.t("أنت برا السالفة") : w.text(en), S.t("الموضوع: %s", g.topic.name(en)), () -> {
            if (g != game || g.round != round || g.phase != OddOneGame.Phase.DEAL) return;   // the round was ended from the pause card meanwhile
            g.show(); g.hide();
            if (g.phase == OddOneGame.Phase.DEAL) deal();
        });
    }
    /** with phones: every player's word goes to their own phone at once (the one outside gets «أنت برا السالفة»); the TV waits until they have seen it */
    private void dealToPhones() {
        screen = Screen.SEEN; beeped = false; sel = 0;
        final OddOneGame g = game;
        for (int p = 0; p < g.players; p++) {
            OddOneWords.Word w = g.secretFor(p);
            phones.room.card(p, GamePhoneRoom.SECRET, names[p], w == null ? S.t("أنت برا السالفة") : w.text(en), S.t("الموضوع: %s", g.topic.name(en)), null, null, -1);
        }
        redraw();
    }
    /** everyone has seen their word (or the host went on): the talk starts, the phones look at the TV */
    private void seenAll() {
        while (game.phase == OddOneGame.Phase.DEAL) { game.show(); game.hide(); }
        phones.room.waitAll(S.t("شوفوا التلفزيون"));
        screen = Screen.TALK; beeped = false; sel = 0; redraw();
    }
    /** with phones: the vote cards go out, one name each (not your own), «ما أدري» last */
    private void voteOnPhones() {
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.VOTE, "", S.t("مين اللي مو معنا؟"), "", names, null, i);
    }
    private boolean phoneVote() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    /** the votes of the phones: the name with the most, «ما اتفقنا» when two share the most or nobody voted a name */
    private int tally() {
        int[] c = new int[game.players]; int best = 0, who = OddOneGame.NOBODY;
        for (int i = 0; i < game.players; i++) { int a = phones.room.reply(i); if (a >= 0 && a < c.length) c[a]++; }
        for (int i = 0; i < c.length; i++) { if (c[i] > best) { best = c[i]; who = i; } else if (c[i] == best && best > 0) who = OddOneGame.NOBODY; }
        return who;
    }
    private void accuseNow(int who) {
        game.accuse(who);
        if (phones != null) phones.room.waitAll(S.t("شوفوا التلفزيون"));
        screen = Screen.ACCUSED; redraw();
    }
    private void settle() {
        score.newRound();
        for (int i = 0; i < game.players; i++) score.add(i, game.gained[i]);
        screen = Screen.SCORE;
    }
    private void next() {
        if (game.phase == OddOneGame.Phase.END) { keep(); if (phones != null) phones.room.waitAll(S.t("شوفوا التلفزيون")); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); redraw(); }
        else deal();
    }

    @Override protected void onPauseChanged(boolean paused, long now) { if (game != null) { if (paused) game.pause(); else game.resume(); } }
    @Override protected void onEndRound() { if (game != null && inPlay()) { game.finish(); next(); } }

    @Override protected void onTick(long now) {
        if (screen == Screen.SEEN && phones != null && phones.room.repliedCount() >= phones.room.active()) { seenAll(); return; }
        if (screen == Screen.HANDS && phoneVote() && phones.room.repliedCount() >= phones.room.active()) { accuseNow(tally()); return; }
        if (screen != Screen.TALK || timer == null || game.phase != OddOneGame.Phase.TALK) return;
        int left = (int) ((game.talkLeftMs() + 999) / 1000);
        timer.set(left);
        if (left == 0 && !beeped) { beeped = true; play(QuizSound.LOW); }   // the time is a guide: one soft tone, nothing moves
    }

    private String round() { return S.t("الجولة %s من %s", num(game.round + 1), num(game.rounds)); }
    private void line(String s, float size, float y) { root.addView(centered(s, size, L.ink, true), at(1728, size * 1.5f, 96, y)); }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(OddOneGame.MIN_PLAYERS, OddOneGame.MAX_PLAYERS); break;
            case SEEN:
                title(round(), S.t("كل واحد يشوف كلمته"));
                root.addView(centered(S.t("شوفوا كلماتكم في جوالاتكم"), 96, L.ink, true), at(1728, 150, 96, 360));
                root.addView(centered(S.t("شافوا %s من %s", num(phones.room.repliedCount()), num(game.players)), 72, L.ink(0xCC), true), at(1728, 110, 96, 560));
                hints(S.t("اختيار"), S.t("كمّل"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case TALK: {
                int sp = game.speaker();
                question(round(), S.t("دور %s", names[sp]), 420);
                timer = new TickTimer((int) (OddOneGame.TALK_MS / 1000), 360);
                timer.set((int) ((game.talkLeftMs() + 999) / 1000));
                line(S.t("الموضوع: %s", game.topic.name(en)), 88, 470);
                line(S.t("قل جملة وحدة عن الكلمة بدون ما تقولها"), 48, 630);
                sidesBar(score, names, sp, 760);
                String okDoes = game.talkDone() + 1 >= game.talkTotal() ? S.t("التصويت") : S.t("اللي بعده");
                if (game.canRedeal()) hints(S.t("اختيار"), okDoes, "▼", S.t("نعيد توزيع الكلمات"), S.t("رجوع"), S.t("إيقاف"));
                else hints(S.t("اختيار"), okDoes, S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case HANDS: {
                title(S.t("مين اللي مو معنا؟"), phoneVote() ? S.t("صوّتوا من جوالاتكم") : S.t("أشّروا كلكم على ثلاثة"));
                int n = game.players, rows = (n + COLS - 1) / COLS;
                if (phoneVote()) root.addView(button(S.t("عدّوا الأصوات"), sel == n + 1), at(640, 104, 800, 284 + rows * 160));
                for (int i = 0; i < n; i++) root.addView(button(names[i], sel == i), at(326, 136, 96 + (i % COLS) * 350, 260 + (i / COLS) * 160));
                root.addView(button(S.t("ما اتفقنا"), sel == n), at(640, 104, 96, 284 + rows * 160));
                if (phoneVote()) root.addView(centered(S.t("صوّت %s من %s", num(phones.room.repliedCount()), num(n)), 56, L.ink(0xCC), true), at(1728, 90, 96, 480 + rows * 160));
                hints("◀ ▶ ▲ ▼", S.t("الاختيار"), S.t("اختيار"), phoneVote() ? S.t("اختيار الأصوات أو الأصابع") : S.t("اللي عليه أكثر الأصابع"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case ACCUSED:
                question(round(), game.accused == OddOneGame.NOBODY ? S.t("ما اتفقتوا على أحد") : S.t("اخترتوا %s", names[game.accused]), 520);
                line(S.t("مين كان اللي مو معنا؟"), 72, 640);
                hints(S.t("اختيار"), S.t("اكشف"));
                break;
            case ODD:
                question(game.caught() ? S.t("كشفتوه") : S.t("ما انكشف"), S.t("اللي مو معنا: %s", names[game.odd]), 520);
                line(game.caught() ? S.t("باقي له فرصة: يخمّن الكلمة") : S.t("له نقطتين"), 72, 640);
                hints(S.t("اختيار"), S.t("كمّل"));
                break;
            case GUESS_ASK:
                question(names[game.odd], S.t("خمّن الكلمة بصوت عالي"), 520);
                line(S.t("الموضوع: %s", game.topic.name(en)), 72, 640);
                hints(S.t("اختيار"), S.t("اكشف الكلمة"));
                break;
            case GUESS_JUDGE:
                question(S.t("الكلمة"), game.word.text(en), 520);
                root.addView(text(S.t("هل خمّنها %s؟", names[game.odd]), 64, L.ink, true), at(1728, 96, 96, 580));
                buttons(new String[]{S.t("صح"), S.t("خطأ")}, sel, 520, 720);
                hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
                break;
            case SCORE:
                title(game.outcome == OddOneGame.Outcome.CAUGHT ? S.t("كشفتوه") : game.outcome == OddOneGame.Outcome.CAUGHT_GUESSED ? S.t("انكشف وعرف الكلمة") : S.t("ما انكشف"),
                    S.t("الكلمة: %s", game.word.text(en)));
                board(score, names, true);
                hints(S.t("اختيار"), game.lastRound() ? S.t("النتيجة النهائية") : S.t("الجولة الجاية"));
                break;
            default: podium(score, names, sel);
        }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (screen == Screen.SETUP) { if (keySetup(chips, back, ok, dx, dy)) start(); return; }
        if (screen == Screen.LOBBY) {
            if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones();
            return;
        }
        if (screen == Screen.END) {
            if (back) { finish(); return; }
            int v = keyButtons(sel, 3, ok, dx);
            if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
            if (v == -1) start(); else if (v == -2) { screen = Screen.SETUP; game = null; redraw(); } else finish();
            return;
        }
        if (back) { openPause(); return; }
        switch (screen) {
            case SEEN: if (ok) seenAll(); break;
            case TALK:
                if (ok) { game.nextSpeaker(); beeped = false; if (game.phase == OddOneGame.Phase.VOTE) { screen = Screen.HANDS; sel = 0; if (phoneVote()) voteOnPhones(); } redraw(); }
                else if (dy > 0 && game.canRedeal()) { game.redeal(); deal(); }
                break;
            case HANDS: {
                int n = game.players, to = sel, last = phoneVote() ? n + 1 : n;
                if (ok) { accuseNow(sel == n + 1 ? tally() : sel == n ? OddOneGame.NOBODY : sel); return; }
                if (dy > 0) to = sel >= n ? last : sel / COLS < (n - 1) / COLS ? Math.min(n - 1, sel + COLS) : n;
                else if (dy < 0) to = sel == n + 1 ? n : sel == n ? (n - 1) / COLS * COLS : sel >= COLS ? sel - COLS : sel;
                else if (dx != 0 && sel < n) to = Math.max(0, Math.min(n - 1, sel + dx));
                else if (dx != 0 && last == n + 1 && sel >= n) to = sel == n ? (dx > 0 ? n + 1 : n) : (dx < 0 ? n : n + 1);
                if (to != sel) { sel = to; redraw(); }
                break;
            }
            case ACCUSED: if (ok) { screen = Screen.ODD; play(game.caught() ? QuizSound.CHIME : QuizSound.LOW); redraw(); } break;
            case ODD:
                if (ok) { game.afterReveal(); if (game.phase == OddOneGame.Phase.GUESS) screen = Screen.GUESS_ASK; else settle(); redraw(); }
                break;
            case GUESS_ASK: if (ok) { game.showWord(); screen = Screen.GUESS_JUDGE; sel = 0; redraw(); } break;
            case GUESS_JUDGE: {
                int v = keyButtons(sel, 2, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                game.guess(v == -1); settle(); redraw();
                break;
            }
            case SCORE: if (ok) { game.nextRound(); next(); } break;
            default: break;
        }
    }
}
