package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import sa.urtv.screensaver.games.hive.HiveBank;
import sa.urtv.screensaver.games.hive.HiveBoard;
import sa.urtv.screensaver.games.hive.HiveGame;

/**
 * «خلية الحروف» (games/letter-hive/SPEC.md, docs/GAMES-KIT.md): two teams, one remote. The board is 5 rows of 5 hexagons and fills the screen; the team in
 * turn moves over it and presses OK on a letter, answers its question aloud, OK shows the answer and the host says «صح» or «خطأ». The first team to join its
 * two sides wins. The rules are HiveGame (plain Java, tests/HiveTest.java); this class only draws and passes the keys on.
 */
public class LetterHiveActivity extends GameActivity {
    private enum Screen { SETUP, BOARD, ASK, WON }
    private static final int BOARD_W = 1232, BOARD_H = 864, PANEL_W = 448;

    private Screen screen = Screen.SETUP;
    private GameChips chips;
    private HiveBank bank;
    private HiveGame game;
    private String[] names;
    private int focus = HiveBoard.cell(2, 2), sel;
    private TickTimer timer;
    private HiveGame.Phase drawn;
    private boolean drawnUp;
    // the game's clock stands still while the pause card is up
    private long pausedAt, pausedFor;

    @Override protected String id() { return "letter-hive"; }
    @Override protected boolean inPlay() { return screen == Screen.BOARD || screen == Screen.ASK; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Map<?, ?> index = GameData.load(this, "letter-hive", GamesBoardData::hiveIndex, GamesBoardData::hiveIndex);
        Map<?, ?> samples = GameData.load(this, "khanat", GamesBoardData::samples, GamesBoardData::samples);
        bank = GamesBoardData.hive(QuizBank.load(this), index, samples);
        GameData.refresh(this, "letter-hive", GamesBoardData::hiveIndex, GamesBoardData.HIVE_BYTES);
        QuizBank.refresh(this);
        names = teamNames(2);
        chips = new GameChips();
        chips.add("level", S.t("المستوى"), new String[] {S.t("متنوع"), S.t("سهل"), S.t("متوسط")}, 0);
        chips.add("kids", S.t("الفئة"), new String[] {S.t("للجميع"), S.t("أطفال")}, 0);
        chips.add("time", S.t("وقت السؤال"), new String[] {S.t("%s ثانية", num(15)), S.t("%s ثانية", num(20)), S.t("%s ثانية", num(30))}, 0);
        chips.add("options", S.t("زر الخيارات"), new String[] {S.tv(ON_OFF[1]), S.tv(ON_OFF[0])}, 0);
        chips.add("steal", S.t("فرصة للفريق الآخر"), new String[] {S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
    }

    private long clock() { return (pausedAt != 0 ? pausedAt : System.currentTimeMillis()) - pausedFor; }
    @Override protected void onPauseChanged(boolean paused, long now) {
        if (paused) pausedAt = now; else if (pausedAt != 0) { pausedFor += now - pausedAt; pausedAt = 0; }
    }

    private void start() {
        if (bank == null) { toast(S.t("قريبًا")); return; }
        HiveGame.Settings s = new HiveGame.Settings();
        s.level = chips.picked("level"); s.kids = chips.picked("kids") == 1; s.seconds = HiveGame.SECONDS[chips.picked("time")];
        s.challenge = chips.picked("options") == 1; s.steal = chips.picked("steal") == 1; s.english = en;
        Random rnd = new Random();
        game = new HiveGame(bank, s, rnd.nextBoolean() ? HiveBoard.TEAM_SIDES : HiveBoard.TEAM_TOP, this::clock, rnd);
        game.avoid(GamePrefs.recent(this, id()));
        focus = HiveBoard.cell(2, 2); pausedAt = 0; pausedFor = 0;
        screen = Screen.BOARD;
        redraw();
    }

    private void leaveGame() {
        if (game != null) GamePrefs.remember(this, id(), new ArrayList<>(game.asked()));
    }
    @Override protected void onEndRound() { leaveGame(); screen = Screen.SETUP; }

    // ---------------------------------------------------------------- the keys

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP:
                if (keySetup(chips, back, ok, dx, dy)) start();
                break;
            case BOARD:
                if (back) { openPause(); return; }
                if (ok) {
                    if (game.pick(focus)) { screen = Screen.ASK; sel = 0; play(QuizSound.CHIME); } else play(QuizSound.LOW);   // a taken hexagon
                } else focus = HiveBoard.move(focus, dx > 0 ? HiveBoard.NEXT : dx < 0 ? HiveBoard.PREV : dy < 0 ? HiveBoard.UP : HiveBoard.DOWN);
                redraw();
                break;
            case ASK:
                if (back) { openPause(); return; }
                int n = game.phase() == HiveGame.Phase.JUDGE ? 2 : game.canShowOptions() ? 2 : 1;
                int v = keyButtons(Math.min(sel, n - 1), n, ok, dx);
                if (v >= 0) { sel = v; redraw(); return; }
                int chosen = -1 - v;
                if (game.phase() == HiveGame.Phase.JUDGE) {
                    game.judge(chosen == 0);
                    play(chosen == 0 ? QuizSound.CHIME : QuizSound.LOW);
                    if (game.phase() == HiveGame.Phase.WON) { leaveGame(); screen = Screen.WON; sel = 0; play(QuizSound.FANFARE); }
                    else { screen = Screen.BOARD; if (chosen == 0) focus = nearestFree(focus); }
                } else if (chosen == 1) game.showOptions();
                else game.reveal();
                sel = 0;
                redraw();
                break;
            default:
                if (back) { finish(); return; }
                int w = dy != 0 ? keyButtons(sel, 3, false, dy) : keyButtons(sel, 3, ok, 0);
                if (w >= 0) { sel = w; redraw(); return; }
                if (w == -1) start(); else if (w == -2) { screen = Screen.SETUP; redraw(); } else finish();
        }
    }

    /** after a hexagon is taken the focus steps to a free one beside it (or stays), so the next team starts on a letter it can pick */
    private int nearestFree(int from) {
        if (game.board.owner[from] == HiveBoard.FREE) return from;
        for (int nb : HiveBoard.neighbours(from)) if (game.board.owner[nb] == HiveBoard.FREE) return nb;
        for (int i = 0; i < HiveBoard.CELLS; i++) if (game.board.owner[i] == HiveBoard.FREE) return i;
        return from;
    }

    @Override protected void onTick(long now) {
        if (screen != Screen.ASK || game == null) return;
        game.tick();
        if (game.phase() != drawn || game.timeUp() != drawnUp) { if (game.timeUp()) play(QuizSound.LOW); redraw(); return; }
        if (timer != null && !game.timeUp()) timer.set((int) ((game.leftMs() + 999) / 1000));
    }

    // ---------------------------------------------------------------- the screens

    @Override protected void draw() {
        timer = null;
        if (screen == Screen.SETUP) { setup(chips, S.t("ابدأ")); return; }
        if (screen == Screen.ASK) { drawAsk(); return; }
        boolean won = screen == Screen.WON;
        root.addView(new HexBoard(this), at(BOARD_W, BOARD_H, 96, 96));
        float x = 96 + BOARD_W + 48;
        root.addView(text(gameName(), 64, L.ink, true), at(PANEL_W, 88, x, 96));
        for (int t = 0; t < 2; t++) {
            boolean on = !won && game.turn() == t + 1 || won && game.winner() == t + 1;
            FrameLayout tile = stamp(on ? L.ink : L.tile, L.ink, 6, false);
            tile.addView(box(GameLook.SIDES[t], on ? L.onInk : L.ink, 3, false), tileAt(48, 48, 24, 24));
            tile.addView(text(clip(names[t], 44, 250), 44, on ? L.onInk : L.ink, true), tileAt(260, 60, 92, 16));
            tile.addView(text(t == 0 ? S.t("من اليمين إلى اليسار") : S.t("من الأعلى إلى الأسفل"), 30, on ? L.saff : L.ink(0xCC), false), tileAt(400, 44, 24, 84));
            int cells = 0;
            for (int o : game.board.owner) if (o == t + 1) cells++;
            TextView c = text(num(cells), 56, on ? L.saff : L.ink, true);
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, android.view.Gravity.END | android.view.Gravity.TOP); cp.setMarginEnd(px(24)); cp.topMargin = px(8);
            tile.addView(c, cp);
            root.addView(tile, at(PANEL_W, 144, x, 208 + t * 160));
        }
        if (won) {
            TextView w = text(S.t("الفائز: %s", names[game.winner() - 1]), 56, L.ink, true); w.setMaxLines(2);
            root.addView(w, at(PANEL_W, 124, x, 540));
            String[] labels = endLabels();
            for (int i = 0; i < 3; i++) root.addView(button(labels[i], sel == i), at(PANEL_W, 88, x, 672 + i * 96));
            if (confetti) root.addView(new QuizActivity.ConfettiView(this), new FrameLayout.LayoutParams(-1, -1));
            hints("▲ ▼", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
        } else {
            TextView turn = text(S.t("دور %s", names[game.turn() - 1]), 64, L.ink, true); turn.setMaxLines(2);
            root.addView(turn, at(PANEL_W, 160, x, 548));
            TextView how = text(S.t("اختاروا حرفًا ثم اضغطوا «اختيار»"), 40, L.ink(0xCC), false); how.setMaxLines(3);
            root.addView(how, at(PANEL_W, 160, x, 724));
            hints(S.t("الأسهم"), S.t("الحرف"), S.t("اختيار"), S.t("السؤال"), S.t("رجوع"), S.t("توقف"));
        }
    }

    private FrameLayout.LayoutParams tileAt(float w, float h, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(px(w), px(h), android.view.Gravity.START | android.view.Gravity.TOP);
        p.setMarginStart(px(x)); p.topMargin = px(y);
        return p;
    }

    private void drawAsk() {
        HiveGame.Question q = game.question();
        HiveGame.Phase ph = game.phase();
        drawn = ph; drawnUp = game.timeUp();
        boolean judge = ph == HiveGame.Phase.JUDGE;
        String who = names[game.answering() - 1], letter = String.valueOf(q.letter);
        String meta = game.timeUp() && !judge ? S.t("انتهى الوقت") : ph == HiveGame.Phase.STEAL ? S.t("حرف %s، فرصة %s", letter, who) : S.t("حرف %s، دور %s", letter, who);
        String words = q.kind == HiveGame.Kind.BANK ? digits(q.item.q[en ? 1 : 0])
            : q.kind == HiveGame.Kind.CATEGORY ? S.t("اذكر دولة أو مدينة أو حيوانًا يبدأ بحرف %s", letter) : S.t("اذكر اسمًا يبدأ بحرف %s", letter);
        question(meta, words, 480);
        if (!judge && !game.timeUp()) { timer = new TickTimer(game.settings.seconds, 428); timer.set((int) ((game.leftMs() + 999) / 1000)); }
        if (q.kind == HiveGame.Kind.BANK && game.optionsShown()) {
            for (int i = 0; i < 4; i++) {
                int state = judge ? (i == q.rightPlace() ? STAMP_RIGHT : STAMP_WRONG) : STAMP;
                root.addView(answer(letter(i), digits(q.option(i, en)), state), at(856, 144, 96 + (i % 2) * 872, 512 + (i / 2) * 160));
            }
        } else if (judge && q.kind == HiveGame.Kind.BANK) {
            root.addView(answer(letter, digits(q.item.a[en ? 1 : 0]), STAMP_RIGHT), at(1728, 200, 96, 544));
        } else {
            String line = !judge ? S.t("جاوبوا بصوت مسموع") : S.t("الحكم لكم: أي جواب صحيح يُحسب");
            root.addView(text(line, 64, L.ink, true), at(1728, 96, 96, 536));
            if (judge && q.kind == HiveGame.Kind.CATEGORY) {
                TextView ex = text(S.t("أمثلة: %s", android.text.TextUtils.join(en ? ", " : "، ", q.hints)), 48, L.ink(0xCC), false); ex.setMaxLines(2);
                root.addView(ex, at(1728, 140, 96, 656));
            }
        }
        String[] labels = judge ? new String[] {S.t("صح"), S.t("خطأ")} : game.canShowOptions() ? new String[] {S.t("اكشف الجواب"), S.t("خيارات")} : new String[] {S.t("اكشف الجواب")};
        buttons(labels, Math.min(sel, labels.length - 1), 520, 848);
        hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"), S.t("رجوع"), S.t("توقف"));
    }

    /** the honeycomb: 5 rows of 5, the odd rows half a cell toward the end of the line; a row starts at the right in Arabic. Each team's two sides carry its colour. */
    private final class HexBoard extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path(); private final Rect bounds = new Rect();
        HexBoard(Context c) {
            super(c);
            fill.setStyle(Paint.Style.FILL); line.setStyle(Paint.Style.STROKE); line.setStrokeJoin(Paint.Join.MITER);
            ink.setTypeface(display()); ink.setTextAlign(Paint.Align.LEFT);
        }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), m = px(28), cw = (w - 2 * m) / (HiveBoard.COLS + .5f), hw = cw / 2, r = (h - 2 * m) / (1.5f * (HiveBoard.ROWS - 1) + 2);
            // the sides: team 1 right and left, team 2 top and bottom
            fill.setColor(GameLook.SIDES[0]); cv.drawRect(0, m, px(16), h - m, fill); cv.drawRect(w - px(16), m, w, h - m, fill);
            fill.setColor(GameLook.SIDES[1]); cv.drawRect(m, 0, w - m, px(16), fill); cv.drawRect(m, h - px(16), w - m, h, fill);
            boolean won = game.phase() == HiveGame.Phase.WON;
            int[] chain = won ? game.chain() : null;
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < HiveBoard.CELLS; i++) {
                boolean focused = !won && i == focus, lit = false;
                if (chain != null) for (int c : chain) lit |= c == i;
                if ((pass == 1) != (focused || lit)) continue;   // the focus and the winning chain are drawn last, over their neighbours' lines
                int row = HiveBoard.row(i), owner = game.board.owner[i];
                float x = m + hw + HiveBoard.col(i) * cw + ((row & 1) == 1 ? hw : 0), y = m + r + row * 1.5f * r, g = px(5), a = hw - g, b = r - g;
                if (rtl) x = w - x;
                path.rewind();
                path.moveTo(x, y - b); path.lineTo(x + a, y - b / 2); path.lineTo(x + a, y + b / 2); path.lineTo(x, y + b); path.lineTo(x - a, y + b / 2); path.lineTo(x - a, y - b / 2); path.close();
                fill.setColor(owner != HiveBoard.FREE ? GameLook.SIDES[owner - 1] : focused ? L.saff : L.tile);
                fill.setAlpha(won && !lit ? 0x66 : 0xFF);
                cv.drawPath(path, fill);
                line.setColor(L.ink); line.setStrokeWidth(px(focused || lit ? 14 : 5)); line.setAlpha(won && !lit ? 0x66 : 0xFF);
                cv.drawPath(path, line);
                String s = String.valueOf(game.board.letter[i]);
                ink.setTextSize(r * 1.05f); ink.setColor(L.onSaff); ink.setAlpha(won && !lit ? 0x66 : owner != HiveBoard.FREE && !focused ? 0xB3 : 0xFF);
                ink.getTextBounds(s, 0, 1, bounds);   // centred on the glyph's ink, not the font's line box
                cv.drawText(s, x - bounds.exactCenterX(), y - bounds.exactCenterY(), ink);
            }
        }
    }
}
