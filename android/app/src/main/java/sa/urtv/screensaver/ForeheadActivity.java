package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.charades.CharadesWords;
import sa.urtv.screensaver.games.forehead.ForeheadGame;

/**
 * «على جبيني» (docs/GAMES-NEW.md): the phone is the card. One player holds it to the forehead, the team describes the word on it; tilt down is «صح», up is «تخطّي».
 * Needs phones (they are the game), so «ابدأ» opens the lobby. The TV never shows the word. The rules are ForeheadGame (no Android); the words are the charades
 * bank (the «وصّف» words, nothing repeats on this TV).
 */
public class ForeheadActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, READY, COUNT, PLAY, TURN_END, END }
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<CharadesWords.Word> words; private ForeheadGame game; private GameScore score; private String[] names = new String[0];
    private CharadesWords.Deck deck; private int sel, seat; private TickTimer timer; private final Random rnd = new Random();

    @Override protected String id() { return "forehead"; }
    @Override protected boolean inPlay() { return screen == Screen.COUNT || screen == Screen.PLAY; }
    @Override protected String namesKind() { return "teams"; }
    @Override protected int namesCount() { return ForeheadGame.MIN_TEAMS + chips.picked("teams"); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        words = GameData.load(this, "charades", CharadesActivity.PUBLISHED, CharadesActivity.STARTER);
        GameData.refresh(this, "charades", CharadesActivity.PUBLISHED, CharadesActivity.BYTES_MAX);
        String[] secs = new String[ForeheadGame.SECONDS.length], turns = new String[ForeheadGame.TURNS.length];
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(ForeheadGame.SECONDS[i]));
        for (int i = 0; i < turns.length; i++) turns[i] = num(ForeheadGame.TURNS[i]);
        chips = new GameChips().add("teams", S.t("الفرق"), new String[]{num(2), num(3), num(4)}, 0).add("time", S.t("الوقت"), secs, 1)
            .add("turns", S.t("أدوار كل فريق"), turns, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() {
        if (deck == null) return;
        List<String> r = deck.recent(); Collections.reverse(r);
        GamePrefs.remember(this, id(), r);
    }

    private int teamCount() { return ForeheadGame.MIN_TEAMS + chips.picked("teams"); }
    private void start() {
        if (words == null) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private void startWithPhones() {
        if (phones.room.count() < teamCount()) return;   // at least a phone for every team
        phones.room.start(); phoneLobby = false;
        begin();
    }
    private void begin() {
        int teams = teamCount();
        names = teamNames(teams);
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id())); Collections.reverse(recent);
        deck = new CharadesWords.Deck(words, true, chips.picked("kids") == 1, recent, rnd);
        game = new ForeheadGame(teams, ForeheadGame.TURNS[chips.picked("turns")], ForeheadGame.SECONDS[chips.picked("time")],
            () -> { CharadesWords.Word w = deck.draw(); return w == null ? "" : w.text(en); }, System::currentTimeMillis);
        score = new GameScore(teams);
        toReady();
    }
    /** the phones of a team take the turns in turn: the first phone of the team plays the team's first turn, the next phone its second ... */
    private int playerSeat() {
        int team = game.team(), n = phones.room.count(), mine = 0;
        for (int s = team; s < n; s += game.teams) mine++;
        int k = game.round() % Math.max(1, mine);
        return team + k * game.teams;
    }
    private void sync() { for (int i = 0; i < game.teams; i++) score.points[i] = game.score[i]; }

    private void toReady() {
        screen = Screen.READY; sel = 0; seat = playerSeat();
        String who = phones.room.name(seat);
        for (int i = 0; i < phones.room.count(); i++) phones.room.hold(i, S.t("دور %s: %s", names[game.team()], who));
        phones.room.card(seat, GamePhoneRoom.ACT, names[game.team()], S.t("ضع الجوال على جبينك، ثم اضغط جاهز"), "", null, new int[] {GamePhoneRoom.ACT_READY}, -1);
        redraw();
    }
    private void sendWord() {
        if (game.word.isEmpty()) return;
        phones.room.card(seat, GamePhoneRoom.TILT, names[game.team()], game.word, "", null, new int[] {GamePhoneRoom.ACT_OK, GamePhoneRoom.ACT_SKIP}, -1);
    }

    @Override protected void onPauseChanged(boolean paused, long now) { if (game != null) { if (paused) game.pause(); else game.resume(); } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); keep(); screen = Screen.END; sel = 0; if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        if (screen == Screen.READY && phones.room.replied(seat)) {
            game.ready(); screen = Screen.COUNT;
            for (int i = 0; i < phones.room.count(); i++) phones.room.hold(i, i == seat ? S.t("ثبّت الجوال على جبينك") : S.t("استعدوا، الوقت يبدأ"));
            redraw(); return;
        }
        if (screen == Screen.COUNT || screen == Screen.PLAY) {
            ForeheadGame.Phase was = game.phase;
            game.tick();
            if (was == ForeheadGame.Phase.COUNT && game.phase == ForeheadGame.Phase.PLAY) {
                screen = Screen.PLAY; sendWord();
                for (int i = 0; i < phones.room.count(); i++) if (i != seat) phones.room.hold(i, S.t("وصّفوا الكلمة بدون ما تقولوها"));
                redraw(); return;
            }
            if (game.phase == ForeheadGame.Phase.TURN_END) { sync(); screen = Screen.TURN_END; sel = 0; play(QuizSound.CHIME); phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); return; }
            if (screen == Screen.PLAY) {
                boolean changed = false;
                for (int a : phones.room.take(seat)) {
                    if (a == GamePhoneRoom.ACT_OK && game.right()) { changed = true; play(QuizSound.CHIME); }
                    else if (a == GamePhoneRoom.ACT_SKIP && game.skip()) changed = true;
                }
                if (changed) { sync(); sendWord(); redraw(); }
                else if (timer != null) timer.set((int) ((game.leftMs() + 999) / 1000));
            }
        }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case READY: if (back) { openPause(); } else if (ok) { game.ready(); screen = Screen.COUNT; redraw(); } return;   // the host can start the count for a player
            case COUNT: case PLAY: if (back) openPause(); return;
            case TURN_END:
                if (back) { openPause(); return; }
                if (ok) { game.nextTurn(); if (game.phase == ForeheadGame.Phase.END) { keep(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); } else toReady(); }
                return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); screen = Screen.SETUP; game = null; start(); } else if (v == -2) { closePhones(); screen = Screen.SETUP; game = null; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(teamCount(), 12); break;
            case READY:
                question(S.t("الجولة %s من %s", num(game.round() + 1), num(game.turnsEach)), S.t("دور %s", names[game.team()]), 420);
                root.addView(centered(S.t("الجوال على جبين %s", phones.room.name(seat)), 96, L.ink, true), at(1728, 150, 96, 500));
                root.addView(centered(S.t("الفريق يوصف الكلمة بدون ما يقولها"), 52, L.ink(0xCC), true), at(1728, 90, 96, 690));
                sidesBar(score, names, game.team(), 800);
                hints(S.t("اختيار"), S.t("ابدأ العدّ"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case COUNT: {
                root.setBackgroundColor(L.deep);
                root.addView(centered(S.t("ثبّتوا الجوال على الجبين"), 96, L.onDeep, true), at(1728, 140, 96, 300));
                root.addView(centered(S.t("الوقت يبدأ بعد لحظات"), 64, L.saff, true), at(1728, 100, 96, 520));
                break;
            }
            case PLAY:
                question(S.t("دور %s", names[game.team()]), S.t("جبتوا %s", num(game.got)), 420);
                timer = new TickTimer((int) (game.turnMs / 1000), 360); timer.set((int) ((game.leftMs() + 999) / 1000));
                root.addView(centered(S.t("تخطّينا %s", num(game.skipped)), 56, L.ink(0xCC), true), at(1728, 80, 96, 500));
                root.addView(centered(S.t("الكلمة على الجوال فقط"), 60, L.ink, true), at(1728, 90, 96, 640));
                sidesBar(score, names, game.team(), 800);
                hints(S.t("رجوع"), S.t("إيقاف"));
                break;
            case TURN_END:
                title(S.t("انتهى الوقت"), names[game.team()]);
                root.addView(centered(S.t("جبتوا %s", num(game.got)), 144, L.ink, true), at(1728, 200, 96, 330));
                sidesBar(score, names, game.team(), 640);
                hints(S.t("اختيار"), game.turn + 1 >= game.turns() ? S.t("النتيجة النهائية") : S.t("الدور الجاي"), S.t("رجوع"), S.t("إيقاف"));
                break;
            default: sync(); podium(score, names, sel);
        }
    }
}
