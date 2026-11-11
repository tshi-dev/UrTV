package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * «الألعاب»: the hub's games window opens this list, in the «ورقة» look of the games (docs/GAMES-KIT.md). The tiles come from GameRegistry, four to a line:
 * a ready game opens with one press (its ready screen has «ابدأ» focused, so a round starts in two presses); the others say «قريبًا». The games are played
 * with one remote and no phones; only «الأسرع» needs phones and says so («بالجوالات»). The focus starts on the game played last, else on the first game
 * with no phones. Back returns to the hub.
 */
public class QuizGamesActivity extends Activity {
    /** the registry's words, marked for the English table (GameRegistry has no S in it; tests/games_kit_test.py keeps the two the same) */
    static final String[] WORDS = {
        S.n("الأسرع"), S.n("الكل يجاوب من جواله، والأسرع يكسب"),
        S.n("خمس خانات"), S.n("حرف واحد، وخانة لكل من حيوان ونبات وجماد وبلاد واسم"),
        S.n("الكلمة المفقودة"), S.n("جملة ناقصة كلمة، من يكملها؟"),
        S.n("واحد مو معنا"), S.n("الكل يعرف الكلمة إلا واحد، من هو؟"),
        S.n("بدون كلام"), S.n("مثّل الكلمة وفريقك يخمّنها"),
        S.n("خلية الحروف"), S.n("فريقان يتسابقان على وصل الخلية"),
        S.n("خمّن الكلمة"), S.n("كلمة من خمسة حروف في ست محاولات"),
        S.n("دومينو"), S.n("الدومينو كما تعرفونه، بالريموت"),
    };
    static final int COLS = 4;
    private FrameLayout root; private float scale; private int sel; private Typeface display, medium; private GameLook L;

    @Override protected void attachBaseContext(Context base) {
        Context e = S.wrap(base);
        if (S.en()) { super.attachBaseContext(e); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar")); c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        L = GameLook.of(this);
        try { display = Typeface.createFromAsset(getAssets(), "fonts/ThmanyahDisplay-Bold.otf"); medium = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf"); }
        catch (RuntimeException e) { display = Typeface.DEFAULT_BOLD; medium = Typeface.DEFAULT; }
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        root.setBackgroundColor(L.paper);
        setContentView(root);
        sel = GameRegistry.firstFocus(GamePrefs.last(this));
        draw();
    }

    private int px(float v) { return Math.round(v * scale); }
    private FrameLayout.LayoutParams at(float w, float h, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w < 0 ? -1 : px(w), px(h), Gravity.START | Gravity.TOP);
        p.setMarginStart(px(x)); p.topMargin = px(y);
        return p;
    }
    private TextView text(String s, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale); v.setTypeface(bold ? display : medium);
        v.setIncludeFontPadding(false); v.setTextDirection(Dir.text()); v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return v;
    }

    private void draw() {
        root.removeAllViews();
        View band = new View(this); band.setBackgroundColor(L.deep);
        root.addView(band, at(-1, 200, 0, 0));
        root.addView(text(S.t("الألعاب"), 96, L.onDeep, true), at(900, 120, 96, 54));
        TextView mode = text(S.t("بدون جوالات: ريموت واحد يدور بينكم"), 40, L.saff, false); mode.setGravity(Gravity.END); mode.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(px(820), px(56), Gravity.END | Gravity.TOP); mp.setMarginEnd(px(96)); mp.topMargin = px(84);
        root.addView(mode, mp);
        float w = 408, hgt = 336, gap = 32;
        for (int i = 0; i < GameRegistry.ALL.length; i++) {
            GameRegistry.Game g = GameRegistry.ALL[i];
            boolean focus = i == sel, live = g.ready;
            int words = focus ? L.onInk : live ? L.ink : L.ink(0x80);
            LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.BOTTOM); tile.setLayoutDirection(Dir.layout());
            tile.setPadding(px(32), px(24), px(32), px(28));
            GradientDrawable bg = new GradientDrawable(); bg.setColor(focus ? L.ink : live ? L.tile : L.paper);
            if (live || focus) bg.setStroke(px(6), L.ink); else bg.setStroke(px(4), L.ink(0x59), px(14), px(10));
            tile.setBackground(bg);
            String markWord = !live ? S.t("قريبًا") : g.phones ? S.t("بالجوالات") : "";
            if (!markWord.isEmpty()) {   // the mark: a small square-cornered label at the top of the tile
                TextView mark = text(markWord, 30, live ? L.onSaff : words, true); mark.setSingleLine(true);
                if (live) mark.setBackgroundColor(L.saff);
                mark.setPadding(px(live ? 14 : 0), px(4), px(live ? 14 : 0), px(4));
                LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-2, -2); kp.gravity = Gravity.START;
                tile.addView(mark, kp);
            }
            tile.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
            TextView name = text(S.tv(g.name), 56, words, true); name.setSingleLine(true);
            tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
            TextView line = text(S.tv(g.line), 30, words, false); line.setMaxLines(2); line.setLineSpacing(0, 1.1f); line.setAlpha(focus ? .9f : .8f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(84)); lp.topMargin = px(8);
            tile.addView(line, lp);
            root.addView(tile, at(w, hgt, 96 + (i % COLS) * (w + gap), 236 + (i / COLS) * (hgt + gap)));
        }
        LinearLayout hint = new LinearLayout(this); hint.setOrientation(LinearLayout.HORIZONTAL); hint.setLayoutDirection(Dir.layout());
        String[] kv = {"◀ ▶ ▲ ▼", S.t("الألعاب"), S.t("اختيار"), S.t("افتح اللعبة"), S.t("رجوع"), S.t("الأقسام")};
        for (int i = 0; i + 1 < kv.length; i += 2) {
            TextView key = text(kv[i], 30, L.ink, false); key.setGravity(Gravity.CENTER); key.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); key.setBackgroundColor(L.ink(0x24)); key.setPadding(px(16), 0, px(16), 0); key.setMinWidth(px(64));
            LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-2, px(48)); kp.setMarginEnd(px(12));
            hint.addView(key, kp);
            TextView wv = text(kv[i + 1], 32, L.ink, false); wv.setAlpha(.85f); wv.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, px(48)); wp.setMarginEnd(px(48));
            hint.addView(wv, wp);
        }
        root.addView(hint, at(-1, 48, 96, 978));
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode(), n = GameRegistry.ALL.length;
        int dx = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (Dir.rtl()) dx = -dx;
        int dy = k == KeyEvent.KEYCODE_DPAD_UP ? -1 : k == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : 0;
        if (dx != 0) { if (sel % COLS + dx >= 0 && sel % COLS + dx < COLS && sel + dx < n) { sel += dx; draw(); } return true; }
        if (dy != 0) { int to = sel + dy * COLS; if (to >= 0 && to < n) { sel = to; draw(); } return true; }
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A) {
            GameRegistry.Game g = GameRegistry.ALL[sel];
            boolean opened = false;
            if (g.ready) {
                try { startActivity(new Intent().setClassName(this, getClass().getPackage().getName() + "." + g.activity)); GamePrefs.setLast(this, g.id); opened = true; }
                catch (RuntimeException notThere) { /* a game marked ready whose screen is not in this build: «قريبًا» */ }
            }
            if (!opened) Toast.makeText(this, S.t("قريبًا"), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }
}
