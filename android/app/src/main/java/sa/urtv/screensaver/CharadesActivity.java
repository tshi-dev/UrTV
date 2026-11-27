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

    private enum Screen { SETUP, LOBBY, ACTOR, BACK, PLAY, TURN_END, END }
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<CharadesWords.Word> words; private CharadesGame game; private GameScore score; private String[] names;
    private int sel, actor, passes; private TickTimer timer; private final Random rnd = new Random();
    private boolean behind;   // «على جبيني» with one remote (opened by ForeheadActivity): the word on the TV for all but the guesser
    private CharadesWords.Word pushed; private boolean pushedSkip, pushedFoul;   // what the actor's phone shows now

    @Override protected String id() { return "charades"; }
    @Override protected String gameName() { return behind ? S.tv(GameRegistry.byId("forehead").name) : super.gameName(); }
    @Override protected boolean inPlay() { return screen != Screen.SETUP && screen != Screen.LOBBY && screen != Screen.END; }
    @Override protected String namesKind() { return "teams"; }
    @Override protected int namesCount() { return TEAMS[chips.picked("teams")]; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        behind = getIntent() != null && getIntent().getBooleanExtra("behind", false);
        words = GameData.load(this, "charades", PUBLISHED, STARTER);
        GameData.refresh(this, "charades", PUBLISHED, BYTES_MAX);
        String[] teams = new String[TEAMS.length], secs = new String[CharadesGame.SECONDS.length];
        for (int i = 0; i < teams.length; i++) teams[i] = num(TEAMS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(CharadesGame.SECONDS[i]));
        chips = new GameChips().add("mode", S.t("الطريقة"), new String[]{S.t("بدون كلام"), S.t("وصّف")}, 0).add("teams", S.t("الفرق"), teams, 0).add("time", S.t("الوقت"), secs, 1)
            .add("age", S.t("الكلمات"), new String[]{S.t("للجميع"), S.t("للأطفال")}, 0).add("foul", S.t("المخالفات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0)
            .add("phones", S.t("اللعب بالجوالات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
        chips.get("phones").picked = getIntent().getBooleanExtra("phones", false) ? 1 : 0;   // off unless the «بالجوالات» page opened this game; never kept from last time
        if (behind) {   // opened by «على جبيني» in «اللعب بالريموت بس»: its teams, time and kids filter (the two lists have the same values)
            chips.get("teams").picked = Math.max(0, Math.min(TEAMS.length - 1, getIntent().getIntExtra("teams", 0)));
            chips.get("time").picked = Math.max(0, Math.min(CharadesGame.SECONDS.length - 1, getIntent().getIntExtra("time", 1)));
            chips.get("age").picked = getIntent().getIntExtra("kids", 0) == 1 ? 1 : 0;
            chips.get("phones").picked = 0;
        }
        onChips(chips);
        if (behind) new android.os.Handler(android.os.Looper.getMainLooper()).post(this::start);   // the forehead's setup page was the setup: straight to the cover
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
        if (chips.picked("phones") == 1) { if (!openPhones()) return; screen = Screen.LOBBY; phoneLobby = true; redraw(); return; }
        closePhones();   // the switch is off: the remote plays alone, as it always did
        begin();
    }
    /** the lobby's start: the phones that came in are the actors' phones, in turn (two at least, so there is someone else to guess) */
    private void startWithPhones() {
        if (phones.room.count() < 2) return;
        phones.room.start(); phoneLobby = false; passes = 0;
        begin();
    }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private void begin() {
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id())); Collections.reverse(recent);
        boolean describe = behind || chips.picked("mode") == 1;   // «على جبيني» uses the «وصّف» words
        game = new CharadesGame(new CharadesWords.Deck(words, describe, chips.picked("age") == 1, recent, rnd), describe, TEAMS[chips.picked("teams")],
            CharadesGame.SECONDS[chips.picked("time")], CharadesGame.DEFAULT_TURNS, chips.picked("foul") == 1, System::currentTimeMillis);
        game.behind = behind && !onPhones();
        game.coverNext = !onPhones();   // with one remote a new word waits behind a cover (the TV is where the guessers look); with phones it is on the actor's phone
        score = new GameScore(game.teams); names = teamNames(game.teams);
        pass();
    }
    /** the team's actor takes the remote and sees the first word alone; hiding it starts the clock */
    private void pass() {
        if (game.behind) { screen = Screen.BACK; redraw(); return; }   // the guesser turns their back first; OK starts the clock
        screen = Screen.PLAY;
        final CharadesGame g = game; final int turn = g.turn, sudden = g.sudden, team = g.team;
        g.ready();
        if (g.phase == CharadesGame.Phase.END) { end(); return; }
        if (onPhones()) { toActor(); return; }
        handOver(names[team], g.word.text(en), S.t("الوقت يبدأ لما تخفي الكلمة"), () -> {
            if (g != game || g.turn != turn || g.sudden != sudden || g.team != team || (g.phase != CharadesGame.Phase.PASS && g.phase != CharadesGame.Phase.PLAY)) return;   // ended from the pause card meanwhile
            g.ready(); g.hide();
        });
    }
    /** with phones: the word goes to the actor's phone alone (the turns go round the phones); the clock starts when the actor says they have it */
    private void toActor() {
        actor = passes++ % phones.room.count();
        phones.room.waitAll(S.t("شوفوا التلفزيون"));
        phones.room.card(actor, GamePhoneRoom.SECRET, S.t("الكلمة لك وحدك"), game.word.text(en), S.t("الوقت يبدأ لما تضغط شفتها"), null, null, -1);
        pushed = null; screen = Screen.ACTOR; redraw();
    }
    private void actorReady() {
        game.ready(); game.hide();   // the clock starts; the word never stays on the TV
        screen = Screen.PLAY; pushWord(); redraw();
    }
    /** the actor's phone shows the word in play and the buttons that can be used now */
    private void pushWord() {
        if (game.word == null || game.phase != CharadesGame.Phase.PLAY) return;
        pushed = game.word; pushedSkip = game.canSkip(); pushedFoul = game.canFoul();
        List<Integer> a = new ArrayList<>(); a.add(GamePhoneRoom.ACT_OK); if (pushedSkip) a.add(GamePhoneRoom.ACT_SKIP); if (pushedFoul) a.add(GamePhoneRoom.ACT_FOUL);
        int[] acts = new int[a.size()]; for (int i = 0; i < acts.length; i++) acts[i] = a.get(i);
        phones.room.card(actor, GamePhoneRoom.ACT, game.describe ? S.t("وصّفها بدون ما تقولها") : S.t("مثّلها بدون كلام"), game.word.text(en), "", null, acts, -1);
    }
    /** what the actor pressed on the phone, in order; the word that comes next is hidden on the TV at once */
    private void phoneActions() {
        boolean changed = false;
        for (int a : phones.room.take(actor)) {
            if (game.phase != CharadesGame.Phase.PLAY) break;
            if (a == GamePhoneRoom.ACT_OK) { if (game.correct()) { changed = true; play(QuizSound.CHIME); if (game.phase == CharadesGame.Phase.PLAY) game.hide(); } }
            else if (a == GamePhoneRoom.ACT_SKIP) { if (game.skip()) { changed = true; game.hide(); } }
            else if (a == GamePhoneRoom.ACT_FOUL) { if (game.foul()) { changed = true; play(QuizSound.LOW); } }
        }
        if (game.deckReshuffled()) toast(S.t("خلصت الكلمات، نعيدها من جديد"));
        if (game.phase == CharadesGame.Phase.TURN_END) { turnEnded(); redraw(); return; }
        if (changed || pushed != game.word || pushedSkip != game.canSkip() || pushedFoul != game.canFoul()) { pushWord(); redraw(); }
    }
    private void sync() { for (int i = 0; i < game.teams; i++) score.points[i] = game.score[i]; }
    private void end() { keep(); sync(); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); redraw(); }
    private void turnEnded() { sync(); screen = Screen.TURN_END; if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (game != null) { if (paused) game.pause(); else game.resume(); } }
    @Override protected void onEndRound() { if (game != null && screen == Screen.PLAY) { game.dropTurn(); turnEnded(); } }

    private int seconds() { return (int) ((game.leftMs() + 999) / 1000); }
    @Override protected void onTick(long now) {
        if (screen == Screen.ACTOR && phones != null && phones.room.replied(actor)) { actorReady(); return; }
        if (screen == Screen.PLAY && onPhones() && game.phase == CharadesGame.Phase.PLAY) phoneActions();
        if (screen != Screen.PLAY || game.phase != CharadesGame.Phase.PLAY) return;
        boolean changed = game.tick();
        if (game.phase == CharadesGame.Phase.TURN_END) { turnEnded(); play(QuizSound.CHIME); redraw(); return; }   // the bell: time is up
        if (changed) redraw(); else if (timer != null) timer.set(seconds());
    }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(2, 12); break;
            case BACK:
                root.setBackgroundColor(L.deep);
                root.addView(centered(S.t("ولّ ظهرك للتلفزيون"), 128, L.onDeep, true), at(1728, 170, 96, 150));
                root.addView(centered(S.t("اللي يخمّن من %s", names[game.team]), 72, L.saff, true), at(1728, 110, 96, 340));
                root.addView(centered(S.t("الباقين: شوفوا الكلمة ووصّفوها بدون ما تقولونها"), 52, L.onDeep, true), at(1728, 100, 96, 520));
                root.addView(focusCard(S.t("جاهزين، ابدأوا الوقت"), 56, true), at(672, 112, 624, 720));
                hints(S.t("اختيار"), S.t("يبدأ الوقت"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case ACTOR:
                question(S.t("دور %s", names[game.team]), S.t("الممثل يشوف الكلمة في جواله"), 420);
                root.addView(centered(phones.room.name(actor), 120, L.ink, true), at(1728, 180, 96, 520));
                root.addView(centered(S.t("الباقين: غطّوا عيونكم"), 56, L.ink(0xCC), true), at(1728, 84, 96, 730));
                hints(S.t("اختيار"), S.t("ابدأ الوقت"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case PLAY: {
                sync();
                String turn = S.t("دور %s", names[game.team]);
                if (game.behind && game.wordVisible()) {   // the word for the room; the guesser's back is to the TV
                    String w = game.word.text(en);
                    question(turn, S.t("وصّفوها بدون ما تقولونها"), 340);
                    timer = new TickTimer((int) (game.turnMs() / 1000), 288);
                    root.addView(centered(w, sizeFor(w, 200, 136, 96, 12, 24), L.ink, true), at(1728, 360, 96, 330));
                    root.addView(centered(S.t("اللي يخمّن: ولّ ظهرك للتلفزيون"), 52, L.ink(0xCC), true), at(1728, 84, 96, 672));
                    sidesBar(score, names, game.team, 820);
                    if (game.canSkip()) hints(S.t("اختيار"), S.t("صح والكلمة اللي بعدها"), "◀", S.t("تخطّي (باقي %s)", num(CharadesGame.SKIPS_MAX - game.skips)));
                    else hints(S.t("اختيار"), S.t("صح والكلمة اللي بعدها"));
                } else if (game.wordVisible() && !onPhones()) {
                    String w = game.word.text(en);
                    question(turn, S.t("الكلمة لك وحدك"), 340);   // 340 high so the line stands whole above the ticks
                    timer = new TickTimer((int) (game.turnMs() / 1000), 288);
                    root.addView(centered(w, sizeFor(w, 200, 136, 96, 12, 24), L.ink, true), at(1728, 360, 96, 330));
                    root.addView(centered(S.t("الباقين: غطّوا عيونكم"), 56, L.ink(0xCC), true), at(1728, 84, 96, 730));
                    if (game.canSkip()) hints(S.t("اختيار"), S.t("أخفِ الكلمة"), "◀", S.t("تخطّي"));
                    else hints(S.t("اختيار"), S.t("أخفِ الكلمة"));
                } else if (game.covered() && !onPhones()) {   // the next word waits: nothing of it on the TV until the actor asks
                    question(turn, S.t("الكلمة الجاية جاهزة"), 340);
                    timer = new TickTimer((int) (game.turnMs() / 1000), 288);
                    root.addView(centered(S.t("الممثل: اضغط اختيار لتشوفها"), 88, L.ink, true), at(1728, 140, 96, 400));
                    root.addView(centered(S.t("الباقين: غطّوا عيونكم قبل ما يضغط"), 56, L.ink(0xCC), true), at(1728, 84, 96, 730));
                    hints(S.t("اختيار"), S.t("شوف الكلمة"), S.t("رجوع"), S.t("إيقاف"));
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
        if (screen == Screen.LOBBY) {
            if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones();
            return;
        }
        if (screen == Screen.BACK) { if (back) openPause(); else if (ok) { game.ready(); if (game.phase == CharadesGame.Phase.END) end(); else { screen = Screen.PLAY; redraw(); } } return; }
        if (screen == Screen.END) {
            if (back) { finish(); return; }
            int v = keyButtons(sel, 3, ok, dx);
            if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
            if (v == -1) start(); else if (v == -2) { screen = Screen.SETUP; game = null; redraw(); } else finish();
            return;
        }
        if (back) { openPause(); return; }
        if (screen == Screen.ACTOR) { if (ok) actorReady(); return; }
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
        if (game.behind && game.phase == CharadesGame.Phase.PLAY) {   // the word stays on the TV: OK is «صح» (the next word at once), left is «تخطّي»
            if (ok) { if (!game.correct()) return; play(QuizSound.CHIME); if (game.phase == CharadesGame.Phase.TURN_END) turnEnded(); }
            else if (left) { if (!game.skip()) return; }
            else return;
            redraw(); return;
        }
        if (game.wordVisible()) {
            if (ok) game.hide(); else if (left) game.skip(); else return;
        } else if (game.covered()) {
            if (ok || dy < 0) game.uncover(); else return;   // the new word shows only when the actor asks
        } else if (ok) {
            if (!game.correct()) return;
            play(QuizSound.CHIME);
            if (game.phase == CharadesGame.Phase.TURN_END) turnEnded();
        } else if (left) { if (!game.skip()) return; }
        else if (dy < 0) { if (onPhones()) return; game.peek(); }   // the word is on the actor's phone, never again on the TV
        else if (dy > 0) { if (!game.foul()) return; play(QuizSound.LOW); }
        else return;
        if (onPhones() && game.wordVisible()) game.hide();
        if (game.deckReshuffled()) toast(S.t("خلصت الكلمات، نعيدها من جديد"));
        redraw();
    }
}
