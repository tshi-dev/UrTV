package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.5 «المظهر» over the live home (docs/ui/PROPOSAL.md, approved): a see-through panel at the end side (left in Arabic) while the home moves aside,
 * whole and live. The looks (أشكال جاهزة) and the parts (التخطيط، الخلفية، الساعة، شكل التطبيقات) are here; the focus resting ⅓ s on one shows it
 * on the home (LOOK_PREVIEW), OK keeps it (Prefs, then LOOK_CHANGED), Back puts the saved look back. The owner's protections: nothing is saved
 * before OK; Back always ends the preview (and so does leaving this screen any way at all, onStop); the home shows each change behind its own
 * still picture (HomeLook), a cut in «الوضع الخفيف». couch-tv's compact rows and colours (CouchStyle), no pills, Arabic first.
 */
public final class LookPanelActivity extends Activity {
    static final String PREVIEW = "sa.urtv.home.LOOK_PREVIEW", PANEL = "sa.urtv.home.LOOK_PANEL";
    /** the focus rests this long on a choice before the home shows it (a passing focus shows nothing) */
    static final long REST_MS = 330;
    static final String[] PARTS = {"layout", "bg", "clock", "apps"};
    static final String[] PART_NAMES = {"التخطيط", "الخلفية", "الساعة", "شكل التطبيقات"};

    private Prefs prefs;
    private CouchStyle cs;
    private LinearLayout list;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable pending;
    /** the part whose choices are open in place (-1: none) */
    private int openPart = -1;
    private View openRow;
    private LinearLayout choices;
    private boolean previewing;

    /** opens «المظهر» over the home: the home first (its own task in front), then this see-through screen over it */
    static void open(Activity from) {
        try {
            from.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage("sa.urtv.home").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) { /* our home is not here: the panel over whatever is in front, still a choice with its OK and Back */ }
        from.startActivity(new Intent(from, LookPanelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        cs = new CouchStyle(this);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        FrameLayout root = new FrameLayout(this);
        int width = getResources().getDisplayMetrics().widthPixels;
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Motion.lite(this) ? CouchStyle.VEIL_LITE : CouchStyle.VEIL);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(cs.px(3.25f), cs.px(CouchStyle.SAFE_Y), cs.px(3.25f), cs.px(CouchStyle.SAFE_Y));
        scroll.addView(list, new ViewGroup.LayoutParams(-1, -2));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(Math.round(width * 700f / 1920f), -1);
        at.gravity = Dir.rtl() ? Gravity.LEFT : Gravity.RIGHT;   // the end side: the home's clock (at its start) stays in sight
        root.addView(scroll, at);
        setContentView(root);
        build();
    }

    @Override protected void onStart() { super.onStart(); panel(true); }

    /** the panel goes, whatever the way (Back, Home, another app): the home comes back to its size and to the saved look */
    @Override protected void onStop() { ui.removeCallbacksAndMessages(null); previewOff(); panel(false); super.onStop(); if (!isFinishing()) finish(); }

    // ---- the rows

    private void build() {
        list.removeAllViews();
        list.addView(text("المظهر", CouchStyle.TITLE, CouchStyle.WHITE, true));
        list.addView(text("التركيز يعرض الشكل على الرئيسية، وموافق يثبّته، ورجوع يرجّع شكلك", CouchStyle.HINT, CouchStyle.SUB, false));
        View first = null;
        if (prefs.hasPreviousLook()) first = row("رجّع الشكل السابق", prefs.previousLookName(), () -> { prefs.restorePreviousLook(); build(); });
        list.addView(head("أشكال جاهزة"));
        List<View> cards = new ArrayList<>();
        LinearLayout line = null;
        String now = prefs.presetCombo().isEmpty() ? prefs.preset() : "";
        for (int i = 0; i < Prefs.PRESETS.length; i++) {
            if (i % 3 == 0) { line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); list.addView(line, new LinearLayout.LayoutParams(-1, -2)); }
            View c = presetCard(Prefs.PRESETS[i], Prefs.PRESET_NAMES[i], Prefs.PRESETS[i].equals(now));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(cs.px(0.375f), cs.px(0.375f), cs.px(0.375f), cs.px(0.375f));
            line.addView(c, lp);
            cards.add(c);
        }
        list.addView(head("كل جزء وحده"));
        for (int k = 0; k < PARTS.length; k++) {
            final int part = k;
            row(PART_NAMES[k], valueName(k, prefs.presetField(PARTS[k])), () -> openChoices(part));
        }
        list.addView(text("موافق يثبّت، ورجوع يرجّع الشكل المحفوظ", CouchStyle.HINT, CouchStyle.DIM, false));
        View focus = first != null ? first : cards.isEmpty() ? null : cards.get(Math.max(0, java.util.Arrays.asList(Prefs.PRESETS).indexOf(now)));
        if (focus != null) focus.requestFocus();
    }

    private View presetCard(final String key, String name, boolean current) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setFocusable(true);
        c.setBackground(cs.card(CouchStyle.ROW, CouchStyle.SEC_ON, CouchStyle.THUMB_RADIUS));
        c.setPadding(cs.px(0.375f), cs.px(0.375f), cs.px(0.375f), cs.px(0.5f));
        ImageView pic = new ImageView(this);
        pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        c.addView(pic, new LinearLayout.LayoutParams(-1, cs.px(6f)));
        thumb(pic, "presets/" + key + ".jpg");
        TextView t = text(name, CouchStyle.ROW_SUB, 0, false);
        c.setSelected(current);   // the look shown now: its card's own fill (SEC_ON), as the settings' section shown
        t.setTextColor(CouchStyle.ink(CouchStyle.TEXT, CouchStyle.TEXT, CouchStyle.INK));
        t.setDuplicateParentStateEnabled(true);
        c.addView(t);
        c.setOnFocusChangeListener((v, has) -> {
            CouchStyle.focus(v, has, CouchStyle.SEC_SCALE);
            if (has) rest(() -> preview(prefs.lookOf(key)));
        });
        c.setOnClickListener(v -> { ui.removeCallbacksAndMessages(null); prefs.setPreset(key); previewing = false; build(); });
        return c;
    }

    private View row(String name, String value, final Runnable ok) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setMinimumHeight(cs.px(CouchStyle.ROW_H));
        r.setPadding(cs.px(CouchStyle.PAD), 0, cs.px(CouchStyle.PAD), 0);
        r.setBackground(cs.card(CouchStyle.ROW, CouchStyle.ROW, CouchStyle.ROW_RADIUS));
        TextView n = text(name, CouchStyle.ROW_TEXT, 0, false);
        n.setTextColor(CouchStyle.ink(CouchStyle.TEXT, CouchStyle.TEXT, CouchStyle.INK));
        n.setDuplicateParentStateEnabled(true);
        r.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = text(value, CouchStyle.ROW_SUB, 0, false);
        v.setTextColor(CouchStyle.ink(CouchStyle.VALUE, CouchStyle.VALUE, CouchStyle.INK_SUB));
        v.setDuplicateParentStateEnabled(true);
        r.addView(v);
        r.setOnFocusChangeListener((x, has) -> { CouchStyle.focus(x, has, CouchStyle.ROW_SCALE); if (has && openPart < 0) rest(this::previewOff); });
        r.setOnClickListener(x -> ok.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = cs.px(CouchStyle.ROW_GAP);
        list.addView(r, lp);
        return r;
    }

    /** a part's choices open in place under its row; the focus on one shows it after REST_MS; OK keeps it; Back closes them (the saved look) */
    private void openChoices(final int part) {
        closeChoices();
        String[] values = values(part), names = names(part);
        openPart = part;
        openRow = list.getFocusedChild();
        choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        String saved = prefs.presetField(PARTS[part]);
        View focus = null;
        for (int i = 0; i < values.length; i++) {
            final String value = values[i];
            TextView c = text(names[i], CouchStyle.ROW_SUB, 0, false);
            c.setGravity(Gravity.CENTER);
            c.setFocusable(true);
            c.setMinimumHeight(cs.px(3.5f));
            c.setPadding(cs.px(1f), 0, cs.px(1f), 0);
            c.setBackground(cs.card(CouchStyle.ROW, CouchStyle.SEC_ON, CouchStyle.THUMB_RADIUS));
            c.setTextColor(CouchStyle.ink(CouchStyle.TEXT, CouchStyle.TEXT, CouchStyle.INK));
            c.setSelected(value.equals(saved));
            c.setOnFocusChangeListener((v, has) -> {
                CouchStyle.focus(v, has, CouchStyle.SEC_SCALE);
                if (has) rest(() -> { String[] p = new String[6]; p[part] = value; preview(p); });
            });
            c.setOnClickListener(v -> { ui.removeCallbacksAndMessages(null); prefs.setLookPart(PARTS[part], value); previewing = false; openPart = -1; build(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(cs.px(0.25f), cs.px(0.375f), cs.px(0.25f), 0);
            choices.addView(c, lp);
            if (value.equals(saved)) focus = c;
        }
        list.addView(choices, list.indexOfChild(openRow) + 1, new LinearLayout.LayoutParams(-1, -2));
        (focus != null ? focus : choices.getChildAt(0)).requestFocus();   // the focus starts on the current value
    }

    private void closeChoices() {
        if (choices != null) list.removeView(choices);
        choices = null;
        View back = openRow;
        openPart = -1;
        openRow = null;
        if (back != null) back.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) {
            ui.removeCallbacksAndMessages(null);
            previewOff();   // the owner: Back always puts the saved look back
            if (choices != null) { closeChoices(); return true; }
            finish();
            return true;
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) return true;
        return super.dispatchKeyEvent(e);
    }

    // ---- the home: the preview (never saved), the panel's place

    private void rest(Runnable r) {
        if (pending != null) ui.removeCallbacks(pending);
        pending = r;
        ui.postDelayed(r, REST_MS);
    }

    /** layout, bg, clock, apps, light, cards (null: the saved one) shown on the home, not saved */
    private void preview(String[] p) {
        previewing = true;
        Intent i = new Intent(PREVIEW).setPackage("sa.urtv.home").putExtra("on", true)
            .putExtra("layout", p[0]).putExtra("bg", p[1]).putExtra("clock", p[2]).putExtra("apps", p[3]).putExtra("light", p[4]).putExtra("cards", p[5]);
        try { sendBroadcast(i, Prefs.LOOK_PERMISSION); } catch (RuntimeException ignored) { }
    }

    private void previewOff() {
        if (!previewing) return;
        previewing = false;
        try { sendBroadcast(new Intent(PREVIEW).setPackage("sa.urtv.home").putExtra("on", false), Prefs.LOOK_PERMISSION); } catch (RuntimeException ignored) { }
    }

    private void panel(boolean open) {
        try { sendBroadcast(new Intent(PANEL).setPackage("sa.urtv.home").putExtra("open", open), Prefs.LOOK_PERMISSION); } catch (RuntimeException ignored) { }
    }

    // ---- the parts' values and names (the settings' own)

    static String[] values(int part) {
        switch (part) {
            case 0: return Prefs.HOME_LAYOUTS;
            case 1: return Prefs.HOME_BGS;
            case 2: return Prefs.HOME_CLOCKS;
            default: return Prefs.HOME_APPS;
        }
    }

    static String[] names(int part) {
        switch (part) {
            case 0: return Prefs.HOME_LAYOUT_NAMES;
            case 1: return Prefs.HOME_BG_NAMES;
            case 2: return Prefs.HOME_CLOCK_NAMES;
            default: return Prefs.HOME_APPS_NAMES;
        }
    }

    private static String valueName(int part, String value) {
        if (part == 3) return Prefs.appsStyleLabel(value);
        int i = java.util.Arrays.asList(values(part)).indexOf(value);
        return names(part)[Math.max(0, i)];
    }

    // ---- small views

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(Words.of(s));
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(size));
        if (color != 0) t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        return t;
    }

    private TextView head(String s) {
        TextView t = text(s, CouchStyle.SHEAD, CouchStyle.DIM, false);
        t.setPadding(0, cs.px(CouchStyle.SHEAD_TOP), 0, cs.px(0.25f));
        return t;
    }

    /** a look's picture, decoded once off the UI thread at a quarter of its size (the panel's cards are small) */
    private void thumb(final ImageView v, final String path) {
        new Thread(() -> {
            Bitmap b = null;
            try (InputStream in = getAssets().open(path)) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                b = BitmapFactory.decodeStream(in, null, o);
            } catch (Exception | OutOfMemoryError e) { /* no picture: the name alone */ }
            final Bitmap got = b;
            if (got != null) ui.post(() -> { if (!isDestroyed()) v.setImageBitmap(got); });
            else ui.post(() -> v.setBackgroundColor(Color.argb(40, 255, 255, 255)));
        }, "look-thumb").start();
    }
}
