package sa.urtv.screensaver;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.link.LinkGame;

/**
 * «اربط الكلمات» (docs/GAMES-NEW.md): 25 words, two teams; each team's captain sees on their phone which words are their team's and gives a one-word clue and a number
 * aloud; the team touches words on their phones. The first phones (two) are the captains, the others take the teams in turn. 4 to 10 phones. The rules and the words
 * are LinkGame (no Android) and the bundled list assets/games/link.gb; nothing repeats on this TV.
 */
public class LinkActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, PLAY, END }
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<String[]> bank; private LinkGame game; private String[] names = new String[0]; private int sel;
    private final Random rnd = new Random();

    @Override protected String id() { return "link"; }
    @Override protected boolean inPlay() { return screen == Screen.PLAY; }
    @Override protected String namesKind() { return "teams"; }
    @Override protected int namesCount() { return 2; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = LinkGame.parse(GameBundles.load(this, "link"));
        chips = new GameChips();
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() { if (game != null) { List<String> r = new ArrayList<>(); for (String[] w : game.words) r.add(w[0]); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private String word(int i) { return en ? game.words.get(i)[1] : game.words.get(i)[0]; }
    private int teamOf(int seat) { return seat % 2 == 0 ? LinkGame.TEAM1 : LinkGame.TEAM2; }
    private boolean captain(int seat) { return seat < 2; }

    private void start() {
        if (bank == null) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < 4) return;
        if (n > 10) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = teamNames(2);
        game = new LinkGame(LinkGame.pick(bank, new HashSet<>(GamePrefs.recent(this, id())), rnd), rnd);
        keep(); screen = Screen.PLAY; sendCards(); redraw();
    }

    /** every phone gets what its role needs: the captains the key (the one whose turn it is also the numbers), the team's other phones the words to touch */
    private void sendCards() {
        if (!onPhones()) return;
        String[] cells = new String[LinkGame.CELLS]; for (int i = 0; i < cells.length; i++) cells[i] = word(i);
        int n = phones.room.count();
        for (int s = 0; s < n; s++) {
            int team = teamOf(s); boolean mine = game.turn == team;
            if (game.phase == LinkGame.Phase.END) { phones.room.waitAll(S.t("شوفوا التلفزيون")); return; }
            if (captain(s)) {
                int[] nums = mine && game.phase == LinkGame.Phase.CLUE ? new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9} : new int[0];
                String note = mine ? (game.phase == LinkGame.Phase.CLUE ? S.t("قل التلميح بصوت عالٍ، ثم اختر عدد الكلمات") : S.t("فريقك يخمّن، باقي %s", num(game.guessesLeft))) : S.t("دور الفريق الثاني");
                phones.room.card(s, GamePhoneRoom.KEY, names[team - 1], game.keyString(), note, cells, nums, -1);
            } else if (mine && game.phase == LinkGame.Phase.GUESS) {
                String[] opts = new String[LinkGame.CELLS + 1]; int[] acts = new int[LinkGame.CELLS + 1]; int k = 0;
                for (int i = 0; i < LinkGame.CELLS; i++) { opts[i] = cells[i]; if (!game.shown[i]) acts[k++] = i; }
                opts[LinkGame.CELLS] = S.t("أنهينا"); acts[k++] = LinkGame.CELLS;
                phones.room.card(s, GamePhoneRoom.PICK, names[team - 1], S.t("اختاروا كلمة"), S.t("باقي %s", num(game.guessesLeft)), opts, java.util.Arrays.copyOf(acts, k), -1);
            } else phones.room.hold(s, mine ? S.t("الكابتن يعطي التلميح") : S.t("دور %s", names[game.turn - 1]));
        }
    }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones() || screen != Screen.PLAY) return;
        int n = phones.room.count();
        if (game.phase == LinkGame.Phase.CLUE) {
            int cap = game.turn == LinkGame.TEAM1 ? 0 : 1;
            int a = phones.room.reply(cap);
            if (a >= 1 && game.clue(a)) { sendCards(); redraw(); }
        } else if (game.phase == LinkGame.Phase.GUESS) {
            int best = -1; long at = Long.MAX_VALUE;
            for (int s = 2; s < n; s++) if (teamOf(s) == game.turn && phones.room.replied(s) && phones.room.repliedAt(s) < at) { at = phones.room.repliedAt(s); best = s; }
            if (best >= 0) {
                int a = phones.room.reply(best);
                if (a == LinkGame.CELLS) game.pass(); else if (!game.guess(a)) { sendCards(); return; }
                play(game.phase == LinkGame.Phase.END ? QuizSound.FANFARE : QuizSound.TICK);
                afterMove();
            }
        }
    }
    private void afterMove() { if (game.phase == LinkGame.Phase.END) { screen = Screen.END; sel = 0; keep(); } sendCards(); redraw(); }

    @Override protected void onEndRound() { if (game != null) { game.finish(); screen = Screen.END; sel = 0; keep(); sendCards(); redraw(); } }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case PLAY: if (back) openPause(); else if (ok && game.phase == LinkGame.Phase.GUESS) { game.pass(); afterMove(); } return;   // the host ends a turn from the remote
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); game = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); game = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(4, 10); break;
            default: drawBoard();
        }
    }

    private void drawBoard() {
        boolean end = screen == Screen.END;
        band(200);
        String head;
        if (end) head = game.winner == 0 ? S.t("خلصت اللعبة") : game.lostByEnd ? S.t("فاز %s: الكلمة الأخيرة وقعت على الثاني", names[game.winner - 1]) : S.t("فاز %s", names[game.winner - 1]);
        else head = game.phase == LinkGame.Phase.CLUE ? S.t("دور %s: الكابتن يعطي التلميح", names[game.turn - 1]) : S.t("دور %s: باقي %s", names[game.turn - 1], num(game.guessesLeft));
        root.addView(text(head, 64, L.onDeep, true), at(1500, 90, 96, 30));
        for (int t = 1; t <= 2; t++) {
            TextView c = text(clip(names[t - 1], 40, 360) + ": " + num(game.remaining(t)), 40, t == game.turn && !end ? L.saff : L.onDeep, true);
            root.addView(c, at(560, 56, 96 + (t - 1) * 580, 124));
        }
        float w = 330, h = 112, gap = 12;
        for (int i = 0; i < LinkGame.CELLS; i++) {
            boolean shown = game.shown[i] || end; int k = game.key[i];
            int fill = !shown ? L.tile : k == LinkGame.TEAM1 ? L.sides[0] : k == LinkGame.TEAM2 ? L.sides[1] : k == LinkGame.END ? L.ink : L.paper;
            FrameLayout t = stamp(fill, L.ink, game.shown[i] ? 3 : 6, !shown ? false : k == LinkGame.NONE);
            int words = !shown ? L.ink : k == LinkGame.END ? L.paper : k == LinkGame.NONE ? L.ink(0x99) : L.onSaff;
            TextView v = centered(word(i), word(i).length() <= 8 ? 46 : 36, words, true); v.setSingleLine(true);
            t.addView(v, new FrameLayout.LayoutParams(-1, -1));
            root.addView(t, at(w, h, 96 + (i % 5) * (w + gap), 236 + (i / 5) * (h + gap)));
        }
        hints(S.t("رجوع"), S.t("إيقاف"), S.t("اختيار"), end ? S.t("اللي بعده") : S.t("أنهِ الدور"));
    }
}
