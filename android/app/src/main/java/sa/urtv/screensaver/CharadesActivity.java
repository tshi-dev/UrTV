package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.charades.CharadesGame;
import sa.urtv.screensaver.games.charades.CharadesWords;

/**
 * «بدون كلام» and «وصّف» (docs/GAMES-PARTY.md, games/charades/SPEC.md): a team's actor sees a word and gets the team to say it before the clock ends, by acting
 * or by describing. One remote: the actor holds it; the first word comes through the kit's hand-over («غطّوا عيونكم»), then OK is «صح», the left arrow
 * «تخطّي», up shows the word again, down is «مخالفة» (in «وصّف», when switched on). The rules are CharadesGame (no Android); this class only draws them.
 */
public class CharadesActivity extends GameActivity {
    static final int BYTES_MAX = 250_000;
    static final GameDataCheck.Parser<List<CharadesWords.Word>> PUBLISHED = raw -> CharadesWords.parse(GameDataCheck.root(raw, '{'), CharadesWords.WORDS_MIN);
    static final GameDataCheck.Parser<List<CharadesWords.Word>> STARTER = raw -> CharadesWords.parse(GameDataCheck.root(raw, '{'), CharadesWords.STARTER_MIN);
    private static final int[] TEAMS = {2, 3, 4};

    private enum Screen { SETUP, PLAY, TURN_END, END }
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<CharadesWords.Word> words; private CharadesGame game; private GameScore score; private String[] names;
    private int sel; private TickTimer timer; private final Random rnd = new Random();

    @Override protected String id() { return "charades"; }
    @Override protected boolean inPlay() { return screen != Screen.SETUP && screen != Screen.END; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        words = GameData.load(this, "charades", PUBLISHED, STARTER);
        GameData.refresh(this, "charades", PUBLISHED, BYTES_MAX);
        String[] teams = new String[TEAMS.length], secs = new String[CharadesGame.SECONDS.length];
        for (int i = 0; i < teams.length; i++) teams[i] = num(TEAMS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(CharadesGame.SECONDS[i]));
        chips = new GameChips().add("mode", S.t("الطريقة"), new String[]{S.t("بدون كلام"), S.t("وصّف")}, 0).add("teams", S.t("الفرق"), teams, 0).add("time", S.t("الوقت"), secs, 1)
            .add("age", S.t("الكلمات"), new String[]{S.t("للجميع"), S.t("للأطفال")}, 0).add("foul", S.t("المخالفات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
        onChips(chips);
    }
    /** «مخالفة» belongs to «وصّف» only */
    @Override protected void onChips(GameChips c) { c.setOff("foul", 1, c.picked("mode") == 0); }
    @Override protected void onPause() { super.onPause(); keep(); }
    /** the words this TV played last wait in the next games (the words only, the newest first) */
    private void keep() {
        if (game == null) return;
        List<String> r = game.recent(); Collections.reverse(r);
        GamePrefs.remember(this, id(), r);
    }

    private void start() {
        if (words == null) { toast(S.t("قريبًا")); return; }
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id())); Collections.reverse(recent);
        boolean describe = chips.picked("mode") == 1;
        game = new CharadesGame(new CharadesWords.Deck(words, describe, chips.picked("age") == 1, recent, rnd), describe, TEAMS[chips.picked("teams")],
            CharadesGame.SECONDS[chips.picked("time")], CharadesGame.DEFAULT_TURNS, chips.picked("foul") == 1, System::currentTimeMillis);
        score = new GameScore(game.teams); names = teamNames(game.teams);
        pass();
    }
    /** the team's actor takes the remote and sees the first word alone; hiding it starts the clock */
    private void pass() {
        screen = Screen.PLAY;
        final CharadesGame g = game; final int turn = g.turn, sudden = g.sudden, team = g.team;
        g.ready();
        if (g.phase == CharadesGame.Phase.END) { end(); return; }
        handOver(names[team], g.word.text(en), S.t("الوقت يبدأ لما تخفي الكلمة"), () -> {
            if (g != game || g.turn != turn || g.sudden != sudden || g.team != team || (g.phase != CharadesGame.Phase.PASS && g.phase != CharadesGame.Phase.PLAY)) return;   // ended from the pause card meanwhile
            g.ready(); g.hide();
        });
    }
    private void sync() { for (int i = 0; i < game.teams; i++) score.points[i] = game.score[i]; }
    private void end() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); redraw(); }
    private void turnEnded() { sync(); screen = Screen.TURN_END; }

    @Override protected void onPauseChanged(boolean paused, long now) { if (game != null) { if (paused) game.pause(); else game.resume(); } }
    @Override protected void onEndRound() { if (game != null && screen == Screen.PLAY) { game.dropTurn(); turnEnded(); } }

    private int seconds() { return (int) ((game.leftMs() + 999) / 1000); }
    @Override protected void onTick(long now) {
        if (screen != Screen.PLAY || game.phase != CharadesGame.Phase.PLAY) return;
        boolean changed = game.tick();
        if (game.phase == CharadesGame.Phase.TURN_END) { turnEnded(); play(QuizSound.CHIME); redraw(); return; }   // the bell: time is up
        if (changed) redraw(); else if (timer != null) timer.set(seconds());
    }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case PLAY: {
                sync();
                String turn = S.t("دور %s", names[game.team]);
                if (game.wordVisible()) {
                    String w = game.word.text(en);
                    question(turn, S.t("الكلمة لك وحدك"), 340);   // 340 high so the line stands whole above the ticks
                    timer = new TickTimer((int) (game.turnMs() / 1000), 288);
                    root.addView(centered(w, sizeFor(w, 200, 136, 96, 12, 24), L.ink, true), at(1728, 360, 96, 330));
                    root.addView(centered(S.t("الباقين: غطّوا عيونكم"), 56, L.ink(0xCC), true), at(1728, 84, 96, 730));
                    if (game.canSkip()) hints(S.t("اختيار"), S.t("أخفِ الكلمة"), "◀", S.t("تخطّي"));
                    else hints(S.t("اختيار"), S.t("أخفِ الكلمة"));
                } else {
                    question(turn, game.describe ? S.t("وصّفها بدون ما تقولها") : S.t("مثّلها بدون كلام"), 420);
                    timer = new TickTimer((int) (game.turnMs() / 1000), 360);
                    root.addView(centered(S.t("جبتوا %s", num(game.got)), 120, L.ink, true), at(1728, 180, 96, 500));
                    sidesBar(score, names, game.team, 760);
                    List<String> h = new ArrayList<>();
                    Collections.addAll(h, S.t("اختيار"), S.t("صح"));
                    if (game.canSkip()) Collections.addAll(h, "◀", S.t("تخطّي (باقي %s)", num(CharadesGame.SKIPS_MAX - game.skips)));
                    Collections.addAll(h, "▲", S.t("شوف الكلمة"));
                    if (game.canFoul()) Collections.addAll(h, "▼", S.t("مخالفة"));
                    Collections.addAll(h, S.t("رجوع"), S.t("إيقاف"));
                    hints(h.toArray(new String[0]));
                }
                timer.set(seconds());
                break;
            }
            case TURN_END:
                title(game.timeUp ? S.t("انتهى الوقت") : S.t("انتهى الدور"), names[game.team]);
                if (game.word != null) root.addView(centered(S.t("الكلمة كانت: %s", game.word.text(en)), 96, L.ink, true), at(1728, 150, 96, 290));
                root.addView(centered(S.t("جبتوا %s", num(game.got)), 120, L.ink, true), at(1728, 180, 96, 480));
                sidesBar(score, names, game.team, 760);
                hints(S.t("اختيار"), S.t("كمّل"));
                break;
            default: podium(score, names, sel);
        }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (screen == Screen.SETUP) { if (keySetup(chips, back, ok, dx, dy)) start(); return; }
        if (screen == Screen.END) {
            if (back) { finish(); return; }
            int v = keyButtons(sel, 3, ok, dx);
            if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
            if (v == -1) start(); else if (v == -2) { screen = Screen.SETUP; game = null; redraw(); } else finish();
            return;
        }
        if (back) { openPause(); return; }
        if (screen == Screen.TURN_END) {
            if (!ok) return;
            int was = game.sudden;
            game.nextTurn();
            if (game.phase == CharadesGame.Phase.END) { end(); return; }
            if (game.sudden > was) toast(S.t("تعادل، كلمة وحدة لكل فريق"));
            pass();
            return;
        }
        boolean left = dx == (rtl ? 1 : -1);   // the arrow that points left on the remote, in both languages
        if (game.wordVisible()) {
            if (ok) game.hide(); else if (left) game.skip(); else return;
        } else if (ok) {
            if (!game.correct()) return;
            play(QuizSound.CHIME);
            if (game.phase == CharadesGame.Phase.TURN_END) turnEnded();
        } else if (left) { if (!game.skip()) return; }
        else if (dy < 0) game.peek();
        else if (dy > 0) { if (!game.foul()) return; play(QuizSound.LOW); }
        else return;
        if (game.deckReshuffled()) toast(S.t("خلصت الكلمات، نعيدها من جديد"));
        redraw();
    }
}
