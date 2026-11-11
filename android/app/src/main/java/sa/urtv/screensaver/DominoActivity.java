package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.domino.DominoGame;
import sa.urtv.screensaver.games.domino.DominoTable;
import sa.urtv.screensaver.games.domino.Snake;
import sa.urtv.screensaver.games.domino.Tile;

/**
 * «دومينو» on the TV with one remote (docs/DOMINO.md), on the games kit (docs/GAMES-KIT.md). The rules, the computer players, the chain's snake and
 * the turns are plain Java in games/domino; this class only draws them. The table is built once; after that a key changes only what DominoTable says
 * changed: an arrow in the hand redraws the two tiles the focus left and reached and nothing else (no redraw(), no new view), a move redraws the chain
 * once and slides the one tile that was played. Tiles and pips are drawn with lines and circles (TileView, ChainView), no picture.
 */
public class DominoActivity extends GameActivity {
    enum Screen { SETUP, PLAY, ROUND, END }
    static final String[] LEVELS = {S.n("سهل"), S.n("وسط"), S.n("صعب")};
    /** a computer player waits this long before it moves, and its tile slides this long: 600 ms together */
    static final int BOT_MS = 350, MOVE_MS = 250, SETTLE_MS = 900;
    /** the 1920 x 1080 design: the sides' bar, the tile counts, the table, the hand (a half tile is 64), the button row */
    static final int BAR_Y = 96, COUNTS_Y = 192, TABLE_Y = 240, TABLE_H = 480, HAND_Y = 736, HAND_U = 64, ROW_Y = 880, UNIT = 72;
    static final int PLAIN = 0, FOCUSED = 1, DIM = 2;

    private Screen screen = Screen.SETUP;
    private GameChips chips; private DominoTable tb; private GameScore score; private String[] names; private int endSel;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable pending;
    private ChainView chain; private CountsView counts; private TileView mover;
    private final List<View> barViews = new ArrayList<>(), handViews = new ArrayList<>(), rowViews = new ArrayList<>(), hintViews = new ArrayList<>();
    private final List<TileView> handTiles = new ArrayList<>();
    private final Runnable botMove = new Runnable() { @Override public void run() { botMove(); } };
    private final Runnable settle = new Runnable() { @Override public void run() { settle(); } };

    @Override protected String id() { return "domino"; }
    @Override protected boolean inPlay() { return screen == Screen.PLAY || screen == Screen.ROUND; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        chips = new GameChips();
        chips.add("people", S.t("اللاعبون"), new String[]{num(1), num(2), num(3), num(4)}, 1);
        chips.add("bots", S.t("الكمبيوتر"), new String[]{S.t("بدون"), num(1), num(2), num(3)}, 0);
        chips.add("level", S.t("مستوى الكمبيوتر"), new String[]{S.tv(LEVELS[0]), S.tv(LEVELS[1]), S.tv(LEVELS[2])}, 1);
        chips.add("target", S.t("الهدف"), new String[]{num(DominoGame.TARGETS[0]), num(DominoGame.TARGETS[1]), num(DominoGame.TARGETS[2])}, 1);
        remembered(soundChip(chips));
        onChips(chips);
    }
    /** two to four at the table: the computer chips that would make fewer or more are switched off */
    @Override protected void onChips(GameChips c) {
        int people = c.picked("people") + 1;
        for (int bots = 0; bots <= 3; bots++) c.setOff("bots", bots, people + bots < 2 || people + bots > 4);
    }
    /** the app leaves the front: with two people or more a hand on the screen goes back behind «دور ...» */
    @Override protected void onPause() { if (tb != null && screen == Screen.PLAY) tb.hide(); super.onPause(); }
    @Override protected void onDestroy() { ui.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override protected void onPauseChanged(boolean paused, long now) {
        if (paused) { ui.removeCallbacksAndMessages(null); if (tb != null && screen == Screen.PLAY) tb.hide(); }
        else if (pending != null) ui.postDelayed(pending, BOT_MS);
    }
    /** «إنهاء الجولة» on the pause card: the game ends here, with the scores as they stand */
    @Override protected void onEndRound() { later(null, 0); if (tb != null) { screen = Screen.END; endSel = 0; } }

    private void later(Runnable r, int ms) { ui.removeCallbacksAndMessages(null); pending = r; if (r != null && !paused()) ui.postDelayed(r, ms); }

    private void start() {
        int people = chips.picked("people") + 1, bots = chips.picked("bots"), n = people + bots;
        int[] levels = new int[n];
        names = new String[n];
        String[] team = teamNames(people);
        for (int i = 0; i < n; i++) {
            if (i < people) { names[i] = team[i]; continue; }
            levels[i] = chips.picked("level") + 1;
            names[i] = bots == 1 ? S.t("الكمبيوتر") : S.t("الكمبيوتر %s", num(i - people + 1));
        }
        tb = new DominoTable(levels, DominoGame.TARGETS[chips.picked("target")], new Random(), new Random());
        score = new GameScore(n);
        screen = Screen.PLAY;
        redraw();
    }

    // ---------------------------------------------------------------- the screens

    @Override protected void draw() {
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case PLAY: drawTable(); break;
            case ROUND: {
                DominoGame.RoundResult r = tb.game.lastRound();
                title(S.t("انتهت الجولة"), r.winner < 0 ? S.t("مقفلة بالتعادل: لا نقاط") : r.blocked ? S.t("مقفلة: فاز %s بأقل مجموع", names[r.winner]) : S.t("فاز %s بالجولة", names[r.winner]));
                board(score, names, true);
                hints(S.t("اختيار"), S.t("الجولة التالية"), S.t("رجوع"), S.t("الإيقاف"));
                break;
            }
            default: podium(score, names, endSel);
        }
    }

    /** the table, built once for a turn of play; after this only the groups below are replaced */
    private void drawTable() {
        barViews.clear(); handViews.clear(); rowViews.clear(); hintViews.clear(); handTiles.clear();
        chain = new ChainView(this); root.addView(chain, at(1728, TABLE_H, 96, TABLE_Y));
        counts = new CountsView(this); root.addView(counts, at(1728, 40, 96, COUNTS_Y));
        mover = new TileView(this); mover.setVisibility(View.INVISIBLE);
        root.addView(mover, new FrameLayout.LayoutParams(px(2 * UNIT), px(2 * UNIT), Gravity.LEFT | Gravity.TOP));
        chain.sync();
        bar(); hand(); row(); keys();
        if (tb.state() == DominoTable.State.BOT) later(botMove, BOT_MS);
        else if (tb.state() == DominoTable.State.ROUND_END || tb.state() == DominoTable.State.GAME_END) later(settle, SETTLE_MS);
    }

    /** replaces one group of views: the old ones leave, what `build` adds is the new group */
    private void group(List<View> g, Runnable build) {
        for (View v : g) root.removeView(v);
        g.clear();
        int n = root.getChildCount();
        build.run();
        for (int i = n; i < root.getChildCount(); i++) g.add(root.getChildAt(i));
        if (mover != null) mover.bringToFront();
    }

    private void bar() {
        group(barViews, new Runnable() { @Override public void run() {
            DominoTable.State st = tb.state();
            sidesBar(score, names, st == DominoTable.State.ROUND_END || st == DominoTable.State.GAME_END ? -1 : tb.game.turn(), BAR_Y);
        } });
        counts.invalidate();
    }

    private void hand() {
        group(handViews, new Runnable() { @Override public void run() {
            handTiles.clear();
            DominoTable.State st = tb.state();
            int turn = tb.game.turn(), shown = tb.shown();
            if (st == DominoTable.State.HANDOVER) {   // no hand on the screen until its owner says so
                root.addView(rect(L.deep), at(1728, 232, 96, HAND_Y));
                root.addView(centered(S.t("دور %s", names[turn]), 88, L.onDeep, true), at(1728, 120, 96, HAND_Y + 12));
                root.addView(centered(S.t("الباقين: غطّوا عيونكم"), 48, L.saff, true), at(1728, 72, 96, HAND_Y + 140));
                return;
            }
            if (shown < 0) {
                if (st == DominoTable.State.BOT) root.addView(centered(S.t("يلعب %s", names[turn]), 56, L.ink(0xCC), true), at(1728, 128, 96, HAND_Y));
                return;
            }
            List<Tile> h = tb.game.hand(shown);
            boolean mine = st == DominoTable.State.TURN || st == DominoTable.State.CHOOSE_END;
            float step = Math.min(HAND_U + 16, 1728f / Math.max(1, h.size())), x0 = 96 + (1728 - (h.size() * step - (step - HAND_U))) / 2;
            for (int i = 0; i < h.size(); i++) {
                TileView v = new TileView(DominoActivity.this);
                v.set(h.get(i).low, h.get(i).high, true, px(HAND_U), mine ? (i == tb.focus() ? FOCUSED : tb.fits(i) ? PLAIN : DIM) : PLAIN);
                root.addView(v, at(HAND_U, 2 * HAND_U, x0 + i * step, HAND_Y));
                handTiles.add(v);
            }
        } });
    }

    /** under the hand: the one button when no tile fits (draw, then pass), else what just happened; the target and the boneyard at the end side */
    private void row() {
        group(rowViews, new Runnable() { @Override public void run() {
            DominoTable.State st = tb.state();
            if (st == DominoTable.State.HANDOVER) return;
            DominoGame g = tb.game;
            int b = tb.button();
            if (b != -1) {
                root.addView(button(b == DominoGame.DRAW ? S.t("سحب") : S.t("تمرير"), true), at(400, 88, 96, ROW_Y));
                root.addView(text(b == DominoGame.DRAW ? S.t("لا حجر يناسب، اسحب حجرًا") : S.t("لا حجر يناسب ولا سحب، مرّر الدور"), 40, L.ink, false), at(760, 64, 520, ROW_Y + 16));
            } else if (!g.events().isEmpty()) {
                DominoGame.Event e = g.events().get(g.events().size() - 1);
                String said = e.kind == DominoGame.PASS ? S.t("مرّر %s الدور", names[e.player]) : e.kind == DominoGame.DRAW ? S.t("سحب %s من السحب", names[e.player]) : null;
                if (said != null) root.addView(text(said, 44, L.ink, true), at(1100, 64, 96, ROW_Y + 12));
            }
            root.addView(text(S.t("الهدف: %s", num(g.target)), 40, L.ink(0xCC), false), atEnd(260, 64, 96, ROW_Y + 16));
            if (g.players == 2) root.addView(text(S.t("السحب: %s", num(g.boneyard())), 40, L.ink(0xCC), false), atEnd(260, 64, 96 + 260 + 24, ROW_Y + 16));
        } });
    }

    /** the bottom line: the next key in words, for the state the table is in */
    private void keys() {
        group(hintViews, new Runnable() { @Override public void run() {
            int b = tb.button();
            switch (tb.state()) {
                case HANDOVER: hints(S.t("اختيار"), S.t("أظهر أحجاري"), S.t("رجوع"), S.t("الإيقاف")); break;
                case TURN:
                    if (b != -1) hints(S.t("اختيار"), b == DominoGame.DRAW ? S.t("سحب") : S.t("تمرير"), S.t("رجوع"), S.t("الإيقاف"));
                    else hints("◀ ▶", S.t("الحجر"), S.t("اختيار"), S.t("ضع الحجر"), S.t("رجوع"), S.t("الإيقاف"));
                    break;
                case CHOOSE_END: hints("◀ ▶", S.t("الطرف"), S.t("اختيار"), S.t("ضع الحجر"), S.t("رجوع"), S.t("غيّر الحجر")); break;
                case BOT: hints(S.t("اختيار"), S.t("تخطَّ"), S.t("رجوع"), S.t("الإيقاف")); break;
                default: break;
            }
        } });
    }

    // ---------------------------------------------------------------- the keys

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case ROUND:
                if (back) openPause();
                else if (ok) { tb.ok(); screen = Screen.PLAY; redraw(); }
                return;
            case END: {
                if (back) { screen = Screen.SETUP; redraw(); return; }
                if (!ok && dx == 0) return;
                int v = keyButtons(endSel, 3, ok, dx);
                if (v >= 0) { if (v != endSel) { endSel = v; redraw(); } return; }
                if (v == -1) start(); else if (v == -2) { screen = Screen.SETUP; redraw(); } else finish();
                return;
            }
            default: break;
        }
        // the table
        DominoTable.State was = tb.state();
        if (was == DominoTable.State.ROUND_END || was == DominoTable.State.GAME_END) { if (ok) settle(); else if (back) openPause(); return; }
        if (back) { if (tb.back()) { chain.sync(); keys(); } else openPause(); return; }
        int focus = tb.focus(), events = tb.game.events().size();
        int c = ok ? tb.ok() : dx != 0 ? tb.move(was == DominoTable.State.CHOOSE_END ? (rtl ? -dx : dx) : dx) : DominoTable.NOTHING;   // the ends are left and right as the eye sees them
        if (c == DominoTable.NOTHING) return;
        if (c == DominoTable.FOCUS) {   // an arrow: two tiles of the hand, or the chain's two ends; nothing else is touched
            if (was == DominoTable.State.CHOOSE_END) chain.sync();
            else { handTiles.get(focus).style(PLAIN); handTiles.get(tb.focus()).style(FOCUSED); }
            return;
        }
        View from = was != DominoTable.State.BOT && focus >= 0 && focus < handTiles.size() ? handTiles.get(focus) : null;
        changed(c, events, from);
    }

    private void botMove() {
        if (screen != Screen.PLAY || paused() || tb.state() != DominoTable.State.BOT) return;
        int events = tb.game.events().size();
        changed(tb.botStep(), events, null);
    }

    /** the game moved on: the chain once, the tile that was played slides to its place, and the groups the change names */
    private void changed(int c, int events, View from) {
        DominoGame g = tb.game;
        float fx = from == null ? 0 : from.getX() + from.getWidth() / 2f, fy = from == null ? 0 : from.getY() + from.getHeight() / 2f;
        chain.sync();
        List<DominoGame.Event> ev = g.events();
        DominoGame.Event last = ev.size() > events ? ev.get(ev.size() - 1) : null;
        if (last != null && last.kind == DominoGame.PLAY) {
            play(QuizSound.TICK);
            if (animate && chain.snake != null && chain.snake.slots.length > 0) {
                if (from == null) { float[] p = counts.middle(last.player); fx = counts.getX() + p[0]; fy = px(BAR_Y + 44); }
                slide(last.end == DominoGame.LEFT ? 0 : chain.snake.slots.length - 1, fx, fy);
            }
        } else if (last != null && last.kind == DominoGame.PASS) play(QuizSound.LOW);
        if ((c & (DominoTable.STATE | DominoTable.SCORE)) != 0) bar(); else counts.invalidate();
        if ((c & DominoTable.HAND) != 0) hand();
        row(); keys();
        DominoTable.State st = tb.state();
        if (st == DominoTable.State.BOT) later(botMove, BOT_MS + (animate ? MOVE_MS : 0));
        else if (st == DominoTable.State.ROUND_END || st == DominoTable.State.GAME_END) later(settle, SETTLE_MS);
        else later(null, 0);
    }

    /** the one moving thing: the played tile from the hand (or from its player's place in the bar) to its place in the chain */
    private void slide(final int index, float fromX, float fromY) {
        mover.animate().cancel();
        Snake.Slot s = chain.snake.slots[index];
        float u = chain.snake.unit;
        boolean upright = s.upright();
        int w = Math.round(upright ? u : 2 * u), hgt = Math.round(upright ? 2 * u : u);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mover.getLayoutParams();
        if (lp.width != w || lp.height != hgt) { lp.width = w; lp.height = hgt; mover.setLayoutParams(lp); }
        mover.set(s.v1, s.v2, upright, u, PLAIN);
        mover.setX(fromX - w / 2f); mover.setY(fromY - hgt / 2f);
        mover.setVisibility(View.VISIBLE); mover.bringToFront();
        chain.hidden = index; chain.invalidate();
        mover.animate().x(chain.getX() + s.cx() - w / 2f).y(chain.getY() + s.cy() - hgt / 2f).setDuration(MOVE_MS).withEndAction(new Runnable() {
            @Override public void run() { mover.setVisibility(View.INVISIBLE); chain.hidden = -1; chain.invalidate(); }
        });
    }

    /** the round is over: its points to the board, then the round's result or the podium */
    private void settle() {
        if (screen != Screen.PLAY || paused()) return;
        later(null, 0);
        DominoGame.RoundResult r = tb.game.lastRound();
        score.newRound();
        if (r.winner >= 0) score.add(r.winner, r.points);
        boolean over = tb.state() == DominoTable.State.GAME_END;
        play(over ? QuizSound.FANFARE : QuizSound.CHIME);
        screen = over ? Screen.END : Screen.ROUND; endSel = 0;
        redraw();
    }

    // ---------------------------------------------------------------- the drawing: tiles of lines and circles

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float[][][] PIPS = {{}, {{0, 0}}, {{-1, -1}, {1, 1}}, {{-1, -1}, {0, 0}, {1, 1}}, {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}},
        {{-1, -1}, {1, -1}, {0, 0}, {-1, 1}, {1, 1}}, {{-1, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {1, 1}}};

    /** one tile from the middles of its two halves: a square-cornered face with an ink line, the line between the halves, the pips */
    private void tile(Canvas c, float x1, float y1, int v1, float x2, float y2, int v2, float u, int style) {
        boolean upright = x1 == x2;
        float gap = u * .04f, line = u * (style == FOCUSED ? .10f : .06f);
        float l = Math.min(x1, x2) - u / 2 + gap, r = Math.max(x1, x2) + u / 2 - gap, t = Math.min(y1, y2) - u / 2 + gap, b = Math.max(y1, y2) + u / 2 - gap;
        int ink = style == DIM ? L.ink(0x59) : style == FOCUSED ? L.onSaff : L.ink;
        paint.setStyle(Paint.Style.FILL); paint.setColor(style == FOCUSED ? L.saff : style == DIM ? L.paper : L.tile);
        c.drawRect(l, t, r, b, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(line); paint.setColor(ink);
        c.drawRect(l + line / 2, t + line / 2, r - line / 2, b - line / 2, paint);
        paint.setStrokeWidth(u * .04f);
        float mx = (x1 + x2) / 2, my = (y1 + y2) / 2, in = u * .2f;
        if (upright) c.drawLine(l + in, my, r - in, my, paint); else c.drawLine(mx, t + in, mx, b - in, paint);
        paint.setStyle(Paint.Style.FILL);
        pips(c, x1, y1, v1, u, upright); pips(c, x2, y2, v2, u, upright);
    }
    private void pips(Canvas c, float x, float y, int v, float u, boolean upright) {
        float d = u * .26f, rad = u * .085f;
        for (float[] p : PIPS[v]) c.drawCircle(x + (upright ? p[0] : -p[1]) * d, y + (upright ? p[1] : p[0]) * d, rad, paint);   // a lying tile is the standing one turned
    }

    /** one tile as a view: a tile of the hand, and the tile that slides */
    final class TileView extends View {
        private int a, b, style; private boolean upright = true; private float u;
        TileView(Context c) { super(c); }
        void set(int a, int b, boolean upright, float u, int style) { this.a = a; this.b = b; this.upright = upright; this.u = u; this.style = style; invalidate(); }
        void style(int s) { if (s != style) { style = s; invalidate(); } }
        @Override protected void onDraw(Canvas c) {
            if (upright) tile(c, u / 2, u / 2, a, u / 2, u * 1.5f, b, u, style); else tile(c, u / 2, u / 2, a, u * 1.5f, u / 2, b, u, style);
        }
    }

    /** the table: the chain as its snake, the two open ends marked; while an end is being chosen, the tile shown on the end it would take */
    final class ChainView extends View {
        Snake snake; int hidden = -1; private int offered = -1, openLeft = -1;
        ChainView(Context c) { super(c); }
        /** lay the chain again from the game (the snake keeps every laid tile where it was unless a row is added or the unit shrinks) */
        void sync() {
            DominoGame g = tb.game;
            List<DominoGame.Placed> list = g.chain(); int anchor = g.anchor(); offered = -1;
            if (tb.state() == DominoTable.State.CHOOSE_END) {
                List<DominoGame.Placed> with = g.chainWith(g.turn(), g.hand(g.turn()).get(tb.focus()), tb.end());
                if (with != null) { list = with; if (tb.end() == DominoGame.LEFT) { anchor++; offered = 0; } else offered = list.size() - 1; }
            }
            openLeft = anchor;
            snake = Snake.lay(list, anchor, px(1728), px(TABLE_H), px(UNIT));
            hidden = -1;
            invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            if (snake == null) return;
            Snake.Slot[] s = snake.slots; float u = snake.unit;
            for (int i = 0; i < s.length; i++) if (i != hidden) tile(c, s[i].x1, s[i].y1, s[i].v1, s[i].x2, s[i].y2, s[i].v2, u, i == offered ? FOCUSED : PLAIN);
            DominoTable.State st = tb.state();
            if (s.length == 0 || hidden >= 0 || st == DominoTable.State.ROUND_END || st == DominoTable.State.GAME_END) return;
            if (offered != 0) end(c, s[0], openLeft == 0, u);                 // the open ends: a saffron frame on the half that takes the next tile
            if (offered != s.length - 1) end(c, s[s.length - 1], false, u);
        }
        private void end(Canvas c, Snake.Slot s, boolean firstHalf, float u) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(u * .10f); paint.setColor(L.saff);
            float x = firstHalf ? s.x1 : s.x2, y = firstHalf ? s.y1 : s.y2, k = u / 2 - u * .09f;
            if (s.tile.isDouble()) c.drawRect(Math.min(s.x1, s.x2) - k, Math.min(s.y1, s.y2) - k, Math.max(s.x1, s.x2) + k, Math.max(s.y1, s.y2) + k, paint);
            else c.drawRect(x - k, y - k, x + k, y + k, paint);
        }
    }

    /** under each side of the bar: how many tiles it holds, as a number and as that many small backs */
    final class CountsView extends View {
        CountsView(Context c) { super(c); }
        private float width() { int n = tb.game.players; return Math.min(560, (1728 - (n - 1) * 16) / (float) n); }
        /** the middle of side i's place, in this view's pixels */
        float[] middle(int i) { float w = width(), x = i * (w + 16) + w / 2; return new float[]{px(rtl ? 1728 - x : x), px(20)}; }
        @Override protected void onDraw(Canvas c) {
            DominoGame g = tb.game; float w = width();
            paint.setTypeface(display()); paint.setTextSize(px(32)); paint.setTextAlign(rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);
            for (int i = 0; i < g.players; i++) {
                float start = i * (w + 16) + 20; int dir = rtl ? -1 : 1;
                float x = px(rtl ? 1728 - start : start);
                paint.setStyle(Paint.Style.FILL); paint.setColor(L.ink);
                c.drawText(num(g.count(i)), x, px(31), paint);
                float bx = x + dir * px(56);
                for (int k = 0; k < g.count(i); k++) {
                    float l = bx + dir * k * px(18), r = l + dir * px(12);
                    paint.setStyle(Paint.Style.FILL); paint.setColor(L.tile); c.drawRect(Math.min(l, r), px(6), Math.max(l, r), px(34), paint);
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(px(3)); paint.setColor(L.ink); c.drawRect(Math.min(l, r), px(6), Math.max(l, r), px(34), paint);
                }
            }
        }
    }
}
