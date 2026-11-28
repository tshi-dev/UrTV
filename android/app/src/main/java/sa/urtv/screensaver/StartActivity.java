package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 «ابدأ بـ» (the owner, docs/mockups/start-presets/ 01, 02, 08, 09): how the TV starts. Four tiles, the owner's three («نظيف», «كامل», «كنبة»)
 * and «المزيد» (the other four); under them what the focused one turns on and off. OK applies it (Prefs.applyStart); «كامل» first asks which parts
 * to take out (02); Back is «خلّه مثل ما هو» (nothing changes). A TV that got 4.4 as an update sees it once as a welcome, the focus on «خلّه مثل
 * ما هو» (EXTRA_WELCOME); a new TV on «نظيف». After that it lives in «الإعدادات ← الشكل ← ابدأ من جديد». The pictures are small JPGs in the
 * app (assets/starts), decoded once, off nothing heavy: a screen opened by a person, not the home.
 */
public class StartActivity extends Activity {
    static final String EXTRA_WELCOME = "welcome";
    private Prefs prefs;
    private FrameLayout root;
    private LinearLayout page;
    private TextView on, offLine;
    private GlassStyle.Look look = GlassStyle.DARK;
    private boolean welcome, more, full;
    private final java.util.Map<String, Typeface> fonts = new java.util.HashMap<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        prefs = new Prefs(this);
        welcome = getIntent().getBooleanExtra(EXTRA_WELCOME, false);
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        look = GlassSource.page(this, root);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(Dir.layout());
        int safeY = Math.round(getResources().getDisplayMetrics().heightPixels * 0.06f), safeX = Math.round(getResources().getDisplayMetrics().widthPixels * 0.045f);
        page.setPadding(safeX, safeY, safeX, safeY);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        draw();
    }

    /** the four tiles of the first page, or of «المزيد»; or «كامل»'s list of parts */
    private void draw() {
        page.removeAllViews();
        if (full) { drawFull(); return; }
        TextView title = text(more ? "المزيد" : "ابدأ بـ", 34, look.text, "Bold");
        page.addView(title);
        TextView sub = text("اختر كيف يبدأ تلفزيونك. تقدر تغيّره بعدين.", 17, look.sub, "Regular");
        page.addView(sub, lp(-2, -2, 4, 0));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setLayoutDirection(Dir.layout());
        String[] keys = more ? new String[] {"prayer", "sport", "kids", "simple"} : new String[] {"clean", "full", "couch", "more"};
        View first = null, focus = null;
        for (String k : keys) {
            View t = tile(k);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMarginEnd(dp(16));
            grid.addView(t, p);
            if (first == null) first = t;
        }
        page.addView(grid, lp(-1, -2, 28, 0));
        // under the grid: what the focused one turns on, and what it leaves off (to add any time)
        LinearLayout what = new LinearLayout(this);
        what.setOrientation(LinearLayout.HORIZONTAL);
        what.setLayoutDirection(Dir.layout());
        on = text("", 15, look.text, "Regular");
        offLine = text("", 15, look.text, "Regular");
        what.addView(on, new LinearLayout.LayoutParams(0, -2, 1f));
        what.addView(offLine, new LinearLayout.LayoutParams(0, -2, 1f));
        page.addView(what, lp(-1, -2, 24, 0));
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        if (welcome && !more) {   // a TV that was set up before 4.4: «خلّه مثل ما هو» is the one with the focus
            TextView same = button("خلّه مثل ما هو");
            same.setOnClickListener(v -> choose("same"));
            page.addView(same, lp(dp(360), -2, 0, 12));
            focus = same;
        }
        TextView hint = text(more ? "رجوع يرجّعك للبدايات الثلاث" : "رجوع يخلّي تلفزيونك مثل ما هو. وتلقى هذي الشاشة في الإعدادات ← الشكل ← ابدأ من جديد", 14, look.sub, "Regular");
        page.addView(hint);
        View f = focus != null ? focus : first;
        if (f != null) { f.requestFocus(); if (f.getTag() instanceof String) say((String) f.getTag()); }
    }

    /** a start's tile: its picture, its name, its line; focus is a scale and a shadow (the owner), no ring */
    private View tile(final String key) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setLayoutDirection(Dir.layout());
        t.setFocusable(true);
        t.setClickable(true);
        t.setTag(key);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(14));
        t.setBackground(bg);
        t.setClipToOutline(true);
        ImageView pic = new ImageView(this);
        pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bm = picture(key);
        if (bm != null) pic.setImageBitmap(bm);
        t.addView(pic, new LinearLayout.LayoutParams(-1, dp(130)));
        String[] st = "more".equals(key) ? null : Parts.start(key);
        TextView name = text(st == null ? "المزيد" : st[1], 22, look.text, "Bold");
        name.setPadding(dp(14), dp(10), dp(14), 0);
        t.addView(name);
        TextView line = text(st == null ? "مصلّي، رياضة، للصغار، بسيط" : st[2], 14, look.sub, "Regular");
        line.setPadding(dp(14), dp(2), dp(14), dp(12));
        t.addView(line);
        t.setOnFocusChangeListener((v, has) -> {
            v.animate().cancel();
            v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).translationZ(has ? dp(12) : 0).setDuration(150).start();
            ((GradientDrawable) v.getBackground()).setColor(has ? 0x26FFFFFF : 0x14FFFFFF);
            if (has) say(key);
        });
        t.setOnClickListener(v -> choose(key));
        return t;
    }

    /** under the grid: «يشتغل» and «ما يشتغل، وتضيفه متى ما تبي» for the focused start */
    private void say(String key) {
        if (on == null) return;
        if ("more".equals(key)) { on.setText(conv("أربع بدايات ثانية")); offLine.setText(""); return; }
        String[] st = Parts.start(key);
        if (st == null || st[4] == null) { on.setText(""); offLine.setText(""); return; }
        List<String> yes = new ArrayList<>(), no = new ArrayList<>();
        for (String[] p : Parts.ALL) (Parts.off(st[4], p[0]) ? no : yes).add(p[1]);
        on.setText(conv(String.format(java.util.Locale.ROOT, "يشتغل: %s", Prefs.join(yes, "، "))));
        offLine.setText(no.isEmpty() ? "" : conv(String.format(java.util.Locale.ROOT, "ما يشتغل، وتضيفه متى ما تبي: %s", Prefs.join(no, "، "))));   // «المواقيت والأذان» in it says the adhan
    }

    private void choose(String key) {
        if ("more".equals(key)) { more = true; draw(); return; }
        if ("full".equals(key)) { full = true; draw(); return; }
        prefs.applyStart(key);
        if ("kids".equals(key)) {   // «للصغار»: the lock needs a PIN: the family settings, on «الرقابة الأبوية»
            startActivity(new Intent(this, SettingsActivity.class).putExtra("screen", SettingsHub.FAMILY).putExtra("hubAt", SettingsHub.FAMILY).putExtra("want", "الرقابة الأبوية"));
        }
        finish();
    }

    /** «كامل»: every part ticked; OK on one takes it out or puts it back; «كمّل» applies «كامل» with the parts taken out (docs mockup 02) */
    private void drawFull() {
        page.addView(text("كامل: وش تبي فيه؟", 34, look.text, "Bold"));
        page.addView(text("شيل الصح عن اللي ما تبيه. يختفي من كل مكان، وإعداداته تبقى لو رجّعته.", 17, look.sub, "Regular"), lp(-2, -2, 4, 0));
        final List<String> off = new ArrayList<>();
        LinearLayout cols = new LinearLayout(this);
        cols.setOrientation(LinearLayout.HORIZONTAL);
        cols.setLayoutDirection(Dir.layout());
        LinearLayout a = column(), c = column();
        cols.addView(a, new LinearLayout.LayoutParams(0, -2, 1f));
        cols.addView(c, new LinearLayout.LayoutParams(0, -2, 1f));
        View first = null;
        for (int i = 0; i < Parts.ALL.length; i++) {
            final String[] p = Parts.ALL[i];
            final TextView row = button("");
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            row.setText(conv(p[1]));
            row.setOnClickListener(v -> {
                if (off.contains(p[0])) off.remove(p[0]); else off.add(p[0]);
                boolean gone = off.contains(p[0]);
                row.setText(conv(!gone ? p[1] : String.format(Parts.PRAYER.equals(p[0]) ? "%s (متوقف، والأذان ما يشتغل)" : "%s (متوقف)", p[1])));
                row.setAlpha(gone ? .6f : 1f);
            });
            (i % 2 == 0 ? a : c).addView(row, lp(-1, -2, 0, 10));
            if (first == null) first = row;
        }
        page.addView(cols, lp(-1, -2, 24, 0));
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        TextView go = button("كمّل");
        go.setOnClickListener(v -> {
            prefs.applyStart("full");
            prefs.setPartsOff(Prefs.join(off, ","));
            prefs.lookChanged();
            finish();
        });
        page.addView(go, lp(dp(300), -2, 0, 0));
        if (first != null) first.requestFocus();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) {
            if (more || full) { more = false; full = false; draw(); return true; }
            choose("same");   // Back: «خلّه مثل ما هو», and it never shows by itself again
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    // ---- the parts

    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPaddingRelative(0, 0, dp(16), 0); return l; }

    /** a button and a row of the list: the look's fill when focused, with dark ink (rows: the fill, docs/ui/SIMPLE.md) */
    private TextView button(String s) {
        final TextView b = text(s, 18, look.text, "Medium");
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        b.setFocusable(true);
        b.setClickable(true);
        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        b.setOnFocusChangeListener((v, has) -> { bg.setColor(has ? look.focus : 0x14FFFFFF); b.setTextColor(has ? look.onFocus : look.text); });
        return b;
    }

    private TextView text(String s, int sp, int color, String weight) {
        TextView t = new TextView(this);
        t.setText(conv(s));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(font(weight));
        t.setTextDirection(Dir.text());
        return t;
    }

    private Bitmap picture(String key) {
        try (java.io.InputStream in = getAssets().open("starts/" + key + ".jpg")) { return BitmapFactory.decodeStream(in); }
        catch (java.io.IOException | RuntimeException e) { return null; }
    }

    private String conv(String s) { return DigitsText.convert(Words.of(s), "latin".equals(prefs.digits())); }

    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            String file = "Bold".equals(name) ? "ThmanyahDisplay-Bold" : "Thmanyah-" + name;   // Regular, Medium; the titles in the display face
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + file + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        return p;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
