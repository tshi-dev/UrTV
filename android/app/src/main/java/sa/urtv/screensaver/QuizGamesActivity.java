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

import java.util.ArrayList;
import java.util.List;

/**
 * «الألعاب»: the hub's fourth window opens this list (the same «ورقة» look as the quiz). One game for now, «الأسرع» (with phones); the others say «قريبًا».
 * One press opens a game; Back returns to the hub.
 */
public class QuizGamesActivity extends Activity {
    /** the games, first to last: key, name, caption (for the one that works) */
    private static final String[][] GAMES = {
        {"quiz", S.n("الأسرع"), S.n("بالجوالات")},
        {"words", S.n("اسم حيوان نبات"), S.n("قريبًا")},
        {"odd", S.n("واحد مو معنا"), S.n("قريبًا")},
        {"act", S.n("بدون كلام"), S.n("قريبًا")},
    };
    private FrameLayout root; private float scale; private int sel; private final List<TextView> tiles = new ArrayList<>(); private Typeface display, medium;

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
        try { display = Typeface.createFromAsset(getAssets(), "fonts/ThmanyahDisplay-Bold.otf"); medium = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf"); }
        catch (RuntimeException e) { display = Typeface.DEFAULT_BOLD; medium = Typeface.DEFAULT; }
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        root.setBackgroundColor(QuizActivity.PAPER);
        setContentView(root);
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
        root.removeAllViews(); tiles.clear();
        View band = new View(this); band.setBackgroundColor(QuizActivity.DEEP);
        root.addView(band, at(-1, 200, 0, 0));
        root.addView(text(S.t("الألعاب"), 96, 0xFFFFFFFF, true), at(1100, 120, 96, 54));
        for (int i = 0; i < GAMES.length; i++) {
            boolean focus = i == sel, live = i == 0;
            TextView tile = new TextView(this);
            tile.setText(S.tv(GAMES[i][1]) + "\n" + S.tv(GAMES[i][2]));
            tile.setTextColor(focus ? QuizActivity.PAPER : live ? QuizActivity.INK : 0x80161410);
            tile.setTextSize(TypedValue.COMPLEX_UNIT_PX, 52 * scale); tile.setTypeface(display); tile.setLineSpacing(0, 1.15f);
            tile.setGravity(Gravity.BOTTOM | Gravity.START); tile.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START); tile.setTextDirection(Dir.text());
            tile.setPadding(px(32), px(32), px(32), px(32));
            GradientDrawable g = new GradientDrawable(); g.setColor(focus ? QuizActivity.INK : live ? QuizActivity.TILE : QuizActivity.PAPER);
            if (live || focus) g.setStroke(px(6), QuizActivity.INK); else g.setStroke(px(4), 0x59161410, px(14), px(10));
            tile.setBackground(g);
            root.addView(tile, at(408, 336, 96 + i * (408 + 32), 260));
            tiles.add(tile);
        }
        LinearLayout hint = new LinearLayout(this); hint.setOrientation(LinearLayout.HORIZONTAL); hint.setLayoutDirection(Dir.layout());
        hint.addView(text(S.t("الأسهم بين الألعاب  •  اختيار لفتح اللعبة  •  رجوع للأقسام"), 32, 0xD9161410, false));
        root.addView(hint, at(-1, 48, 96, 978));
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode();
        int dx = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (Dir.rtl()) dx = -dx;
        if (dx != 0) { sel = Math.max(0, Math.min(GAMES.length - 1, sel + dx)); draw(); return true; }
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A) {
            if (sel == 0) startActivity(new Intent(this, QuizActivity.class));
            else Toast.makeText(this, S.t("قريبًا"), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }
}
