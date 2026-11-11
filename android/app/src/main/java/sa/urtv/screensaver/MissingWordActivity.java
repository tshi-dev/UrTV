package sa.urtv.screensaver;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «الكلمة المفقودة» on the TV (games/missing-word/SPEC.md, the remote-only flow; the rules, the picking and the data check are MissingWord, the screens
 * the kit's blocks: docs/GAMES-KIT.md). The statement stands in the green block with a dashed box for the hidden word; the team whose turn it is says the
 * word aloud, OK shows it, and the host says how it went: «صح», «خطأ», or, with «الخطف» on, the next team that said it right before the reveal.
 */
public class MissingWordActivity extends GameActivity {
    enum Screen { SETUP, ASK, SHOWN, END }

    static final int[] TEAMS = {1, 2, 3, 4};
    /** the kinds' names, marked for the English table (MissingWord has no S in it) */
    static final String[] DECK_WORDS = {S.n("مزيج"), S.n("حكمة ومثل"), S.n("معلومة")};

    private List<MissingWord.Item> all = new ArrayList<>(); private GameChips chips; private MissingWord.Engine game; private String[] names = new String[0];
    private Screen screen = Screen.SETUP; private int sel; private boolean timeUp;
    private final GameClock clock = new GameClock(); private TickTimer timer; private final Random rnd = new Random();

    @Override protected String id() { return "missing-word"; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        List<MissingWord.Item> v = GameData.load(this, "missing-word", MissingWord.PUBLISHED, MissingWord.STARTER);
        if (v != null) all = v;
        GameData.refresh(this, "missing-word", MissingWord.PUBLISHED, MissingWord.BYTES_MAX);
        chips = new GameChips()
            .add("deck", S.t("النوع"), new String[]{S.tv(DECK_WORDS[0]), S.tv(DECK_WORDS[1]), S.tv(DECK_WORDS[2])}, 0)
            .add("teams", S.t("الفرق"), new String[]{num(1), num(2), num(3), num(4)}, 1)
            .add("count", S.t("الأسئلة"), new String[]{num(5), num(10), num(15)}, 1)
            .add("time", S.t("الوقت"), new String[]{S.t("%s ثانية", num(10)), S.t("%s ثانية", num(15)), S.t("%s ثانية", num(30))}, 1)
            .add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0)
            .add("steal", S.t("الخطف"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
        onChips(chips);
    }

    /** what the data can fill now: a kind with too few statements, and «للأطفال» with none, are dashed */
    @Override protected void onChips(GameChips c) {
        c.setOff("kids", 1, MissingWord.available(all, MissingWord.MIX, true, en) == 0);
        boolean kids = c.picked("kids") == 1;
        for (int i = 0; i < MissingWord.DECKS.length; i++) c.setOff("deck", i, !MissingWord.offered(all, MissingWord.DECKS[i], kids, en));
    }

    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.SHOWN; }
    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) clock.pause(now); else clock.resume(now); }
    @Override protected void onEndRound() { if (game != null) { game.finish(); clock.stop(); go(Screen.END); play(QuizSound.FANFARE); } }

    private void go(Screen s) { screen = s; sel = 0; redraw(); }

    private void start() {
        boolean kids = chips.picked("kids") == 1;
        List<MissingWord.Item> picks = MissingWord.pick(all, MissingWord.DECKS[chips.picked("deck")], kids, en, MissingWord.COUNTS[chips.picked("count")], GamePrefs.recent(this, id()), rnd);
        if (picks.isEmpty()) { toast(S.t("قريبًا")); return; }
        List<String> ids = new ArrayList<>();
        for (MissingWord.Item it : picks) ids.add(it.id);
        GamePrefs.remember(this, id(), ids);
        int teams = TEAMS[chips.picked("teams")];
        names = teamNames(teams);
        game = new MissingWord.Engine(teams, picks, chips.picked("steal") == 1);
        ask();
    }
    private void ask() { timeUp = false; clock.start(MissingWord.SECONDS[chips.picked("time")] * 1000L, System.currentTimeMillis()); go(Screen.ASK); }

    @Override protected void onTick(long now) {
        if (screen != Screen.ASK || !clock.running()) return;
        if (timer != null) timer.set(clock.leftSeconds(now));
        if (clock.over(now)) { clock.stop(); timeUp = true; play(QuizSound.LOW); redraw(); }   // the word is not shown by the clock: the host presses OK
    }

    // ---------------------------------------------------------------- keys

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case ASK: {
                if (back) { openPause(); return; }
                int v = keyButtons(sel, 2, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -2) { if (game.hint()) { sel = 0; redraw(); } return; }
                clock.stop(); game.reveal(); go(Screen.SHOWN);
                return;
            }
            case SHOWN: {
                if (back) { openPause(); return; }
                int n = game.canSteal() ? 3 : 2;
                int v = keyButtons(sel, n, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                int pick = -1 - v;   // the buttons from the start: «صح», («خطف»), «خطأ»
                int verdict = pick == 0 ? MissingWord.Engine.CORRECT : pick == n - 1 ? MissingWord.Engine.WRONG : MissingWord.Engine.STOLEN;
                game.judge(verdict);
                if (game.phase == MissingWord.Engine.Phase.END) { go(Screen.END); play(QuizSound.FANFARE); }
                else { play(game.lastPoints > 0 ? QuizSound.CHIME : QuizSound.LOW); ask(); }
                return;
            }
            case END: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) start(); else if (v == -2) go(Screen.SETUP); else finish();
                return;
            }
            default:
        }
    }

    // ---------------------------------------------------------------- screens

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case ASK: drawTurn(false); break;
            case SHOWN: drawTurn(true); break;
            case END: podium(game.score, names, sel); break;
            default:
        }
    }

    private void drawTurn(boolean shown) {
        MissingWord.Item it = game.current();
        if (it == null) return;
        String words = digits(it.text(en)), word = digits(it.word(en));
        question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total())), blanked(words, MissingWord.BLANK, word, game.hinted ? it.firstLetter(en) : null, shown), 528);
        String team = names[game.turn];
        if (!shown) {
            timer = new TickTimer(MissingWord.SECONDS[chips.picked("time")], 452);
            timer.set(timeUp ? 0 : clock.leftSeconds(System.currentTimeMillis()));
            root.addView(button(S.t("اكشف الكلمة"), sel == 0), at(560, 104, 96, 572));
            TextView hint = button(game.hinted ? S.t("أول حرف ظاهر") : S.t("تلميح بنصف النقاط"), sel == 1);
            if (game.hinted) hint.setAlpha(.5f);
            root.addView(hint, at(560, 104, 96 + 560 + 24, 572));
            String line = timeUp ? S.t("انتهى الوقت") : game.score.n == 1 ? S.t("قولوا الكلمة بصوت عالٍ") : S.t("دور %s: قولوا الكلمة بصوت عالٍ", team);
            root.addView(text(line, 56, timeUp ? L.pink : L.ink, true), at(1728, 80, 96, 712));
            hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), sel == 0 ? S.t("اكشف الكلمة") : S.t("تلميح"), S.t("رجوع"), S.t("إيقاف مؤقت"));
        } else {
            String[] labels = game.canSteal()
                ? new String[]{S.t("صح"), S.t("خطف %s", names[game.stealer()]), S.t("خطأ")}
                : new String[]{S.t("صح"), S.t("خطأ")};
            buttons(labels, sel, game.canSteal() ? 480 : 560, 572);
            String line = game.score.n == 1 ? S.t("هل قلتم الكلمة؟ صح بـ %s نقاط", num(game.worth())) : S.t("هل قالها %s؟ صح بـ %s نقاط", team, num(game.worth()));
            root.addView(text(line, 56, L.ink, true), at(1728, 80, 96, 712));
            hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"), S.t("رجوع"), S.t("إيقاف مؤقت"));
        }
        sidesBar(game.score, names, game.turn, 836);
    }
}
