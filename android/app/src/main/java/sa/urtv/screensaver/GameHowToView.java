package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The «طريقة اللعب» panel (docs/GAMES-KIT.md): the title band, the goal and the numbered steps at the start edge, boxes for the phones, the points and the end at the other, all in a
 * ScrollView that the arrows scroll (▲ ▼ show when there is more). Drawn on the 1920 x 1080 design like every game screen, in the games' look (GameLook); the body text is 36 design
 * pixels, raised to 42 on a 720p screen (about 28 px real) and by the system font scale (to 1.3 at most). Both bases use it, the games' GameActivity and «الأسرع»'s QuizActivity.
 * It owns no key: its host calls `scroll(dy)` and `close()`ing is the host's.
 */
final class GameHowToView extends FrameLayout {
    static final float COL_START = 1040, COL_END = 640, GAP = 48, BODY_Y = 200, BODY_H = 760;

    private final float scale, k; private final GameLook L; private final boolean rtl, en, latin; private final Typeface display, medium;
    private final ScrollView body; private final TextView more, less;

    GameHowToView(Context c, GameHowTo.Content ct, String gameName, GameLook look, float scale, boolean rtl, boolean en, boolean latin, Typeface display, Typeface medium) {
        super(c);
        this.L = look; this.scale = scale; this.rtl = rtl; this.en = en; this.latin = latin; this.display = display; this.medium = medium;
        float fs = c.getResources().getConfiguration().fontScale;
        int width = c.getResources().getDisplayMetrics().widthPixels;
        this.k = (width <= 1400 ? 42f / 36f : 1f) * Math.max(1f, Math.min(1.3f, fs));   // 720p: 42 design px is 28 real; a big system font grows it more
        setBackgroundColor(L.paper); setLayoutDirection(Dir.layout());
        // the title band
        addView(rect(L.deep), at(-1, 200, 0, 0));
        addView(label(t("طريقة اللعب"), 96, L.onDeep, true), at(1100, 120, 96, 54));
        TextView small = label(gameName, 40, L.saff, false); small.setGravity(Gravity.END); small.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        addView(small, atEnd(700, 56, 96, 84));
        // the body: the start column (goal, steps) and the end column (phones, points, end)
        body = new ScrollView(c); body.setVerticalScrollBarEnabled(false); body.setOverScrollMode(View.OVER_SCROLL_NEVER); body.setFocusable(false); body.setLayoutDirection(Dir.layout());
        LinearLayout row = new LinearLayout(c); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(Dir.layout()); row.setPadding(px(96), px(24), px(96), px(48));
        LinearLayout startCol = new LinearLayout(c); startCol.setOrientation(LinearLayout.VERTICAL);
        startCol.addView(tag(t("هدف اللعبة"), L.ink, L.onInk), wrap());
        TextView goal = label(t(ct.goal), 38 * k, L.ink, true); goal.setLineSpacing(0, 1.25f); startCol.addView(goal, topMargin(8));
        startCol.addView(tag(t("كيف تلعب"), L.deep, L.onDeep), topMargin(24));
        for (int i = 0; i < ct.steps.size(); i++) {
            LinearLayout step = new LinearLayout(c); step.setOrientation(LinearLayout.HORIZONTAL); step.setLayoutDirection(Dir.layout());
            TextView n = label(num(i + 1), 38 * k, L.onInk, true); n.setGravity(Gravity.CENTER); n.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); n.setBackgroundColor(L.ink);
            step.addView(n, new LinearLayout.LayoutParams(px(60 * k), px(60 * k)));
            TextView s = label(t(ct.steps.get(i)), 36 * k, L.ink, false); s.setLineSpacing(0, 1.25f);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -2, 1f); sp.setMarginStart(px(18)); step.addView(s, sp);
            startCol.addView(step, topMargin(14));
        }
        row.addView(startCol, new LinearLayout.LayoutParams(px(COL_START), -2));
        LinearLayout endCol = new LinearLayout(c); endCol.setOrientation(LinearLayout.VERTICAL);
        if (ct.hasPhones()) endCol.addView(box(t("بالجوالات"), t(ct.phones)), boxParams());
        if (!ct.scoring.isEmpty()) endCol.addView(box(t("النقاط"), t(ct.scoring)), boxParams());
        if (!ct.end.isEmpty()) endCol.addView(box(t("النهاية"), t(ct.end)), boxParams());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(px(COL_END), -2); ep.setMarginStart(px(GAP));
        row.addView(endCol, ep);
        body.addView(row, new FrameLayout.LayoutParams(-1, -2));
        addView(body, at(-1, BODY_H, 0, BODY_Y));
        more = label("▼", 44, L.ink, true); more.setGravity(Gravity.CENTER); more.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); more.setBackgroundColor(L.paper); more.setAlpha(.92f);
        less = label("▲", 44, L.ink, true); less.setGravity(Gravity.CENTER); less.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); less.setBackgroundColor(L.paper); less.setAlpha(.92f);
        addView(more, at(-1, 56, 0, 904)); addView(less, at(-1, 56, 0, BODY_Y));
        body.setOnScrollChangeListener((v, x, y, ox, oy) -> cues());
        body.getViewTreeObserver().addOnGlobalLayoutListener(this::cues);
        cues();
        addHints();
    }

    // ---------------------------------------------------------------- what the host calls

    /** scroll by `dy` steps (+1 down); a step is 120 design pixels */
    void scroll(int dy) { body.smoothScrollBy(0, Math.round(dy * 120 * scale)); }
    /** is there more above (-1) or below (+1)? (for the tests of the source, and the cues) */
    boolean canScroll(int dir) { return body.canScrollVertically(dir); }

    private void cues() { more.setVisibility(body.canScrollVertically(1) ? VISIBLE : GONE); less.setVisibility(body.canScrollVertically(-1) ? VISIBLE : GONE); }

    // ---------------------------------------------------------------- small helpers (the base's blocks, for a view that stands alone)

    private int px(float v) { return Math.round(v * scale); }
    private String t(String ar) { String v = S.tv(ar); return en ? v : DigitsText.convert(v, latin); }
    private String num(int n) { return DigitsText.convert(String.valueOf(n), latin); }
    private View rect(int fill) { View v = new View(getContext()); v.setBackgroundColor(fill); return v; }
    private FrameLayout.LayoutParams at(float w, float h, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w < 0 ? -1 : px(w), h < 0 ? -1 : px(h), Gravity.START | Gravity.TOP); p.setMarginStart(px(x)); p.topMargin = px(y); return p;
    }
    private FrameLayout.LayoutParams atEnd(float w, float h, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(px(w), px(h), Gravity.END | Gravity.TOP); p.setMarginEnd(px(x)); p.topMargin = px(y); return p;
    }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-2, -2); }
    private LinearLayout.LayoutParams topMargin(float v) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = px(v); return p; }
    private LinearLayout.LayoutParams boxParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = px(16); return p; }
    private TextView label(CharSequence s, float size, int color, boolean heavy) {
        TextView v = new TextView(getContext());
        v.setText(s); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale); v.setTypeface(heavy ? display : medium);
        v.setIncludeFontPadding(false); v.setTextDirection(Dir.text()); v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return v;
    }
    /** a small heading block: filled, square */
    private TextView tag(String s, int fill, int words) {
        TextView v = label(s, 36 * Math.min(k, 1.15f), words, true); v.setBackgroundColor(fill); v.setPadding(px(20), px(6), px(20), px(8)); return v;
    }
    /** a box: an ink heading and the words under it, in a square ink line */
    private LinearLayout box(String head, String words) {
        LinearLayout b = new LinearLayout(getContext()); b.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(); g.setColor(L.tile); g.setStroke(px(6), L.ink); b.setBackground(g); b.setPadding(px(6), px(6), px(6), px(6));
        TextView h = label(head, 36 * Math.min(k, 1.15f), L.onInk, true); h.setBackgroundColor(L.ink); h.setPadding(px(20), px(6), px(20), px(8)); b.addView(h, new LinearLayout.LayoutParams(-1, -2));
        TextView w = label(words, 34 * k, L.ink, false); w.setLineSpacing(0, 1.25f); w.setPadding(px(20), px(10), px(20), px(12)); b.addView(w, new LinearLayout.LayoutParams(-1, -2));
        return b;
    }
    /** the bottom line: ▲ ▼ scroll, Back closes */
    private void addHints() {
        LinearLayout row = new LinearLayout(getContext()); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(Dir.layout());
        String[] kv = {"▲ ▼", t("تصفّح"), t("رجوع"), t("إغلاق")};
        for (int i = 0; i + 1 < kv.length; i += 2) {
            TextView key = label(kv[i], 30, L.ink, false); key.setGravity(Gravity.CENTER); key.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); key.setBackgroundColor(L.ink(0x24)); key.setPadding(px(16), 0, px(16), 0); key.setMinWidth(px(64));
            TextView w = label(kv[i + 1], 32, L.ink, false); w.setAlpha(.85f); w.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-2, px(48)); kp.setMarginEnd(px(12)); row.addView(key, kp);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, px(48)); wp.setMarginEnd(px(48)); row.addView(w, wp);
        }
        addView(row, at(-1, 48, 96, 978));
    }
}
