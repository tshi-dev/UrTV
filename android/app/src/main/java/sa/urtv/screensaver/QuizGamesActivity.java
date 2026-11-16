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
        S.n("على جبيني"), S.n("الجوال على جبينك، والفريق يوصف لك الكلمة"),
        S.n("من كاتبها؟"), S.n("الكل يكتب إجابة، وتخمّنون من كتبها"),
        S.n("أقرب رقم"), S.n("خمّنوا الرقم، والأقرب يكسب"),
        S.n("واحد يكذب"), S.n("ثلاثة يحكون وواحد منهم يكذب، فمن هو؟"),
        S.n("حزم المحتوى"), S.n("حجم ما حمّلتموه من الألعاب، واحتفظوا به أو احذفوه"),
    };
    /** the night switch's words, marked for the English table: «وضع ليلي» with its three states, and the three night palettes */
    static final String[] NIGHT_WORDS = {S.n("وضع ليلي"), S.n("تلقائي بعد المغرب"), S.n("ألوان الليل"), S.n("فحم دافئ"), S.n("ليل أزرق"), S.n("بيج خافت")};
    static final String[] NIGHT_STATES = {S.n("إيقاف"), S.n("تشغيل"), NIGHT_WORDS[1]}, PALETTE_NAMES = {NIGHT_WORDS[3], NIGHT_WORDS[4], NIGHT_WORDS[5]};
    /** the filter at the top: all the games, or only the ones that can be played with phones (the choice is kept) */
    static final String[] FILTER_WORDS = {S.n("الكل"), S.n("بالجوالات"), S.n("عرض")};
    static final int COLS = 4;
    private FrameLayout root; private float scale; private int sel, head, filter, top; private Typeface display, medium; private GameLook L;   // head: 0 the tiles have the focus, 1 the night switch, 2 the night colours, 3 the filter; top: the first row of tiles on the screen (two rows show, the list scrolls by row)

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
        filter = GamePrefs.filter(this);
        GamePackStore.sweep(this);   // a «تلقائي» pack nobody opened for 14 days goes
        sel = filter == 0 ? GameRegistry.firstFocus(GamePrefs.last(this)) : 0;
        draw();
    }

    private int px(float v) { return Math.round(v * scale); }
    private FrameLayout.LayoutParams at(float w, float h, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w < 0 ? -1 : px(w), px(h), Gravity.START | Gravity.TOP);
        p.setMarginStart(px(x)); p.topMargin = px(y);
        return p;
    }
    /** the biggest size from `size` down (never under 22, design pixels) at which `s` stands in `maxLines` lines of a `w` x `h` box with no line cut */
    private float fit(Typeface face, String s, float size, float w, float h, int maxLines, float spacing) {
        android.text.TextPaint paint = new android.text.TextPaint(); paint.setTypeface(face);
        for (float f = size; f > 22; f -= 2) {
            paint.setTextSize(f * scale);
            android.text.StaticLayout lay = new android.text.StaticLayout(s, paint, Math.max(1, px(w)), android.text.Layout.Alignment.ALIGN_NORMAL, spacing, 0, false);
            if (lay.getLineCount() <= maxLines && lay.getHeight() <= px(h)) return f;
        }
        return 22;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale); v.setTypeface(bold ? display : medium);
        v.setIncludeFontPadding(false); v.setTextDirection(Dir.text()); v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return v;
    }

    /** the games on the page: all of them, or only the ones that can be played with phones */
    private java.util.List<GameRegistry.Game> shown() {
        java.util.List<GameRegistry.Game> v = new java.util.ArrayList<>();
        for (GameRegistry.Game g : GameRegistry.ALL) if (filter == 0 || GameRegistry.inPhones(g.id)) v.add(g);
        return v;
    }
    private void draw() {
        root.removeAllViews();
        java.util.List<GameRegistry.Game> gs = shown();
        View band = new View(this); band.setBackgroundColor(L.deep);
        root.addView(band, at(-1, 200, 0, 0));
        root.addView(text(S.t("الألعاب"), 96, L.onDeep, true), at(900, 120, 96, 54));
        TextView mode = text(filter == 1 ? S.t("كل واحد بجواله، والريموت للمضيف") : S.t("بدون جوالات: ريموت واحد يدور بينكم"), 40, L.saff, false); mode.setGravity(Gravity.END); mode.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(px(820), px(56), Gravity.END | Gravity.TOP); mp.setMarginEnd(px(96)); mp.topMargin = px(84);
        root.addView(mode, mp);
        headerButtons();
        float w = 408, hgt = 336, gap = 32;
        int selRow = sel / COLS; if (selRow < top) top = selRow; if (selRow > top + 1) top = selRow - 1;
        for (int i = 0; i < gs.size(); i++) {
            GameRegistry.Game g = gs.get(i);
            int vis = i / COLS - top;
            if (vis < 0 || vis > 1) continue;   // the tiles of the other rows are off the screen
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
            // the title and the line fit the tile whatever the language: the size comes down (the title may take two lines, the line three) before anything is cut
            float inner = w - 64;
            String title = S.tv(g.name), about = S.tv(g.line);
            TextView name = text(title, fit(display, title, 56, inner, 120, 2, 1f), words, true); name.setMaxLines(2);
            tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
            TextView line = text(about, fit(medium, about, 30, inner, 96, 3, 1.1f), words, false); line.setMaxLines(3); line.setLineSpacing(0, 1.1f); line.setAlpha(focus ? .9f : .8f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(96)); lp.topMargin = px(8);
            tile.addView(line, lp);
            root.addView(tile, at(w, hgt, 96 + (i % COLS) * (w + gap), 236 + vis * (hgt + gap)));
        }
        LinearLayout hint = new LinearLayout(this); hint.setOrientation(LinearLayout.HORIZONTAL); hint.setLayoutDirection(Dir.layout());
        String[] kv = {"◀ ▶ ▲ ▼", gs.size() > 2 * COLS ? S.t("الألعاب، والباقي تحت") : S.t("الألعاب"), S.t("اختيار"), S.t("افتح اللعبة"), S.t("رجوع"), S.t("الأقسام")};
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

    /** the buttons at the top of the list, on the green band at the end of the line: «وضع ليلي» (off, on, automatic after Maghrib), «ألوان الليل» when it is not off, and the filter «عرض: الكل / بالجوالات» */
    private void headerButtons() {
        int st = GamePrefs.night(this);
        String[] label = {S.t("وضع ليلي") + ": " + S.tv(NIGHT_STATES[st]), S.t("ألوان الليل") + ": " + S.tv(PALETTE_NAMES[GamePrefs.palette(this)]), S.t("عرض") + ": " + S.tv(FILTER_WORDS[filter])};
        float[] w = {380, 380, 340};
        float x1 = 96, gap = 16;                          // from the end edge: the switch first, then the colours, then the filter toward the start
        for (int h : heads()) {
            int i = h - 1; boolean focus = head == h;
            TextView b = text(label[i], 34, focus ? L.paper : L.ink, true); b.setGravity(Gravity.CENTER); b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); b.setSingleLine(true);
            GradientDrawable bg = new GradientDrawable(); bg.setColor(focus ? L.ink : L.tile); bg.setStroke(px(focus ? 6 : 4), focus ? L.saff : L.ink);
            b.setBackground(bg);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(px(w[i]), px(56), Gravity.END | Gravity.TOP); p.setMarginEnd(px(x1)); p.topMargin = px(128);
            root.addView(b, p);
            x1 += w[i] + gap;
        }
    }
    /** the buttons that are shown, from the end edge toward the start */
    private int[] heads() { return GamePrefs.night(this) == GamePrefs.NIGHT_OFF ? new int[] {1, 3} : new int[] {1, 2, 3}; }

    private void applyNight() { L = GameLook.of(this); root.setBackgroundColor(L.paper); draw(); }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode(), n = shown().size();
        int dx = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (Dir.rtl()) dx = -dx;
        int dy = k == KeyEvent.KEYCODE_DPAD_UP ? -1 : k == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : 0;
        boolean okKey = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A;
        if (head != 0) {   // the focus is on the night switch (1) or the night colours (2); the colours sit toward the start, so a step toward the end leads to the switch
            int[] hs = heads(), at = {0};
            for (int i = 0; i < hs.length; i++) if (hs[i] == head) at[0] = i;
            if (dy > 0) { head = 0; draw(); return true; }
            if (dx != 0) { int to = Math.max(0, Math.min(hs.length - 1, at[0] + (dx < 0 ? 1 : -1))); if (hs[to] != head) { head = hs[to]; draw(); } return true; }
            if (okKey) {
                if (head == 3) { filter = 1 - filter; GamePrefs.setFilter(this, filter); sel = 0; draw(); return true; }
                if (head == 1) { int st = (GamePrefs.night(this) + 1) % 3; GamePrefs.setNight(this, st); }
                else GamePrefs.setPalette(this, (GamePrefs.palette(this) + 1) % GamePrefs.PALETTES);
                applyNight();
                return true;
            }
            return super.dispatchKeyEvent(e);
        }
        if (dy < 0 && sel < COLS) { head = 1; draw(); return true; }   // up from the first line of tiles reaches the night switch
        if (dx != 0) { if (sel % COLS + dx >= 0 && sel % COLS + dx < COLS && sel + dx < n) { sel += dx; draw(); } return true; }
        if (dy != 0) { int to = sel + dy * COLS; if (to >= 0 && to < n) { sel = to; draw(); } return true; }
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A) {
            GameRegistry.Game g = shown().get(sel);
            boolean opened = false;
            if (g.ready) {
                try { Intent go = new Intent().setClassName(this, getClass().getPackage().getName() + "." + g.activity); if (filter == 1 && !g.id.equals("quiz")) go.putExtra("phones", true); startActivity(go);   // from the «بالجوالات» view the phones switch starts on
                     GamePrefs.setLast(this, g.id); opened = true; }
                catch (RuntimeException notThere) { /* a game marked ready whose screen is not in this build: «قريبًا» */ }
            }
            if (!opened) Toast.makeText(this, S.t("قريبًا"), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }
}
