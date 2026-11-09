package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;

/**
 * «المظهر والخلفيات» (4.3): the look and the backgrounds of the screensaver and of the home, each on its own, in one place. Every choice is a tile with a small live picture of
 * what it gives (drawn in the colours of the look itself, not a stand-in): the screensaver's card style, its photos (the packs, «صورنا», «صوري»), the home's look and its photos (the
 * same packs as the screensaver's, or its own). The settings are the ones the other screens set (Prefs); this screen only gathers them. Arabic and English (Words.of), the 16 / 8 dp rule.
 */
public class AppearanceActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.Context en = S.wrap(base);
        if (S.en()) { super.attachBaseContext(en); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    private Prefs prefs;
    private FrameLayout root;
    private LinearLayout col;
    private GlassStyle.Look look = GlassStyle.DARK;
    private final Map<String, Typeface> fonts = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        prefs = new Prefs(this);
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        look = GlassSource.page(this, root);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        int safe = Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f) + dp(8);   // the TV's safe area: the first line and «رجوع» stay 5 % in from the edges
        col.setPadding(dp(16), Math.max(dp(40), safe), dp(16), Math.max(dp(40), safe));
        scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - dp(96), dp(1100)), -1);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(scroll, lp);
        setContentView(root);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        look = GlassSource.page(this, root);
        render();   // back from the packs: their counts moved
    }

    // ---- drawing ----

    private void render() {
        View focused = getCurrentFocus();
        Object want = focused == null ? null : focused.getTag();
        col.removeAllViews();
        col.addView(text(w("المظهر والخلفيات"), 28, "Medium", look.text), line(dp(8)));
        col.addView(text(w("شكل شاشة التوقف وشكل الشاشة الرئيسية وصورهما، كلٌّ على حدة."), 16, "Regular", look.sub), line(dp(24)));

        // ---- the screensaver
        col.addView(text(w("شاشة التوقف"), 22, "Medium", look.text), line(dp(8)));
        col.addView(text(w("شكل البطاقات"), 16, "Regular", look.sub), line(dp(8)));
        LinearLayout styles = tiles();
        for (String[] c : Prefs.CARD_STYLES) {
            final String key = c[0];
            styles.addView(tile("style:" + key, new Mock(Mock.CARD, key, GlassStyle.of(prefs.look(), false)), w(c[1]), key.equals(prefs.cardStyle()), v -> { prefs.setCardStyle(key); render(); }), tileParams());
        }
        col.addView(styles, line(dp(16)));
        col.addView(text(w("الصور"), 16, "Regular", look.sub), line(dp(8)));
        col.addView(photosRow(), line(dp(32)));

        // ---- the home
        col.addView(text(w("الشاشة الرئيسية"), 22, "Medium", look.text), line(dp(8)));
        col.addView(text(w("الشكل"), 16, "Regular", look.sub), line(dp(8)));
        LinearLayout looks = tiles();
        boolean light = "light".equals(prefs.appearance());
        for (int i = 0; i < GlassStyle.LOOKS.length; i++) {
            final String key = GlassStyle.LOOKS[i];
            looks.addView(tile("look:" + key, new Mock(Mock.HOME, key, GlassStyle.of(key, light)), w(GlassStyle.LOOK_NAMES[i]), key.equals(prefs.look()), v -> { prefs.setLook(key); render(); }), tileParams());
        }
        col.addView(looks, line(dp(16)));
        col.addView(text(w("الإضاءة"), 16, "Regular", look.sub), line(dp(8)));
        LinearLayout lights = tiles();
        for (final String a : Prefs.APPEARANCES) {
            lights.addView(tile("light:" + a, new Mock(Mock.HOME, prefs.look(), GlassStyle.of(prefs.look(), "light".equals(a)), "auto".equals(a) ? GlassStyle.of(prefs.look(), true) : null), w(shortLight(a)), a.equals(prefs.appearance()), v -> { prefs.setAppearance(a); render(); }), tileParams());
        }
        col.addView(lights, line(dp(16)));
        col.addView(text(w("الصور في الشاشة الرئيسية"), 16, "Regular", look.sub), line(dp(8)));
        col.addView(homePhotosRow(), line(dp(16)));

        View back = button(w("رجوع"), v -> finish());
        back.setTag("back");
        col.addView(back, line(dp(8)));
        restoreFocus(want);
    }

    private static String shortLight(String a) { return "light".equals(a) ? "فاتح" : "auto".equals(a) ? "تلقائي" : "داكن"; }

    /** the screensaver's photos: the packs (their own screen), «صورنا» on or off, «صوري» mixed with ours or alone */
    private View photosRow() {
        LinearLayout row = tiles();
        Map<String, Integer> have = PackStore.installed(this);
        Set<String> on = prefs.packsOn();
        int active = 0;
        for (String id : on) if (have.containsKey(id)) active++;
        row.addView(tile("packs", new Mock(Mock.PHOTOS, "", look), w("مجموعات الصور") + " · " + conv(String.valueOf(active)), active > 0, v -> startActivity(new Intent(this, PhotoPacksActivity.class))), tileParams());
        final boolean ours = prefs.oursOn() || !PhotoPacks.mayTurnOursOff(on, have);
        row.addView(tile("ours", new Mock(Mock.PHOTOS, "ours", look), w("صورنا"), ours, v -> {
            if (ours && !PhotoPacks.mayTurnOursOff(on, have)) { toast(w("فعّل مجموعة منزّلة أولاً، وبعدها تقدر توقف صورنا")); return; }
            prefs.setOursOn(!ours);
            render();
        }), tileParams());
        int own = Own.count(this);
        final boolean only = Own.ONLY.equals(Own.mode(this));
        String ownName = own == 0 ? w("صوري") : only ? w("صوري فقط") : w("صوري مع صورنا");
        row.addView(tile("own", new Mock(Mock.PHOTOS, "own", look), ownName, own > 0, v -> {
            if (Own.count(this) == 0) { toast(w("لا صور لك بعد. تضاف من الجوال.")); return; }
            prefs.setOwnMode(only ? Own.MIX : Own.ONLY);
            render();
        }), tileParams());
        return row;
    }

    /** the home's photos: «مثل شاشة التوقف» or «مجموعات خاصة بها» (then each pack on the TV, and «صورنا», on or off for the home alone) */
    private View homePhotosRow() {
        LinearLayout all = new LinearLayout(this);
        all.setOrientation(LinearLayout.VERTICAL);
        all.setLayoutDirection(Dir.layout());
        LinearLayout row = tiles();
        final boolean same = prefs.homePhotosSame();
        row.addView(tile("home:same", new Mock(Mock.PHOTOS, "same", look), w("مثل شاشة التوقف"), same, v -> { prefs.setHomePhotosSame(true); render(); }), tileParams());
        row.addView(tile("home:own", new Mock(Mock.PHOTOS, "set", look), w("مجموعات خاصة بها"), !same, v -> { prefs.setHomePhotosSame(false); render(); }), tileParams());
        all.addView(row, line(dp(8)));
        if (!same) {
            LinearLayout set = tiles();
            Map<String, Integer> have = PackStore.installed(this);
            final Set<String> mine = prefs.homePacksOn();
            final Map<String, String> names = PackStore.names(this);   // the pack's name, not its id («space»)
            for (final String id : have.keySet()) {
                final boolean on = mine.contains(id);
                set.addView(tile("home:pack:" + id, new Mock(Mock.PHOTOS, id, look), names.containsKey(id) ? names.get(id) : id, on, v -> { prefs.setHomePacksOn(PhotoPacks.toggled(prefs.homePacksOn(), id, !on)); render(); }), tileParams());
            }
            final boolean ours = prefs.homeOursOn() || prefs.homePacks().isEmpty();
            set.addView(tile("home:ours", new Mock(Mock.PHOTOS, "ours", look), w("صورنا"), ours, v -> { prefs.setHomeOursOn(!ours); render(); }), tileParams());
            all.addView(set, line(0));
        }
        return all;
    }

    private LinearLayout tiles() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(Dir.layout());
        row.setBaselineAligned(false);
        return row;
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(dp(8), dp(8), dp(8), dp(8));
        return p;
    }

    /** one choice: its picture, its name, the on mark when it is the one in use */
    private View tile(String tag, Mock mock, String name, boolean on, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setLayoutDirection(Dir.layout());
        t.setPadding(dp(16), dp(16), dp(16), dp(16));
        t.setFocusable(true);
        t.setClickable(true);
        t.setTag(tag);
        t.setOnClickListener(click);
        mock.on = on;
        t.addView(mock, new LinearLayout.LayoutParams(-1, dp(88)));
        LinearLayout head = new LinearLayout(this);
        head.setLayoutDirection(Dir.layout());
        head.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView mark = new android.widget.ImageView(this);
        mark.setImageDrawable(getDrawable(R.drawable.ic_on).mutate());
        mark.getDrawable().setTint(OnOff.green(look.light ? 0xFFFFFFFF : 0xFF000000));
        mark.setVisibility(on ? View.VISIBLE : View.GONE);
        head.addView(mark, new LinearLayout.LayoutParams(dp(7), dp(22)));
        TextView n = text(name, 16, "Medium", look.text);
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        if (on) np.setMarginStart(dp(8));
        head.addView(n, np);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        t.addView(head, hp);
        GlassPaint.card(t, look, 14);
        return t;
    }

    private void restoreFocus(Object tag) {
        View target = tag == null ? null : col.findViewWithTag(tag);
        if (target == null) target = col.findViewWithTag("style:" + prefs.cardStyle());
        if (target != null) target.requestFocus();
    }

    /**
     * The small picture on a tile, drawn in the colours it stands for: HOME is the home in that look (its ground, three round buttons, a card, a line of the clock); CARD is the
     * screensaver's card in that style over a dark photo; PHOTOS is a stack of photo frames (a different tint for each source).
     */
    private final class Mock extends View {
        static final int HOME = 0, CARD = 1, PHOTOS = 2;
        private final int kind;
        private final String key;
        private final GlassStyle.Look l;
        private final GlassStyle.Look l2;   // «تلقائي»: the home in the light look on the other half, so it shows it follows the time of day
        boolean on;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Mock(int kind, String key, GlassStyle.Look l) { this(kind, key, l, null); }

        Mock(int kind, String key, GlassStyle.Look l, GlassStyle.Look l2) {
            super(AppearanceActivity.this);
            this.kind = kind;
            this.key = key;
            this.l = l;
            this.l2 = l2;
        }

        /** the home in one look; a glass look's cards are nearly the ground's colour, so each one also gets a hairline, or it would be missing on a dark ground */
        private void drawHome(Canvas c, GlassStyle.Look l, float w, float h, float r) {
            RectF all = new RectF(0, 0, w, h);
            paint.setColor(GlassStyle.over(l.base, 0xFF000000));
            c.drawRoundRect(all, r, r, paint);
            paint.setColor(l.text);
            c.drawRoundRect(new RectF(dp(12), dp(12), dp(52), dp(22)), dp(3), dp(3), paint);   // the clock
            RectF[] cards = new RectF[7];
            for (int i = 0; i < 3; i++) cards[i] = new RectF(w - dp(16) - (i + 1) * dp(18) - i * dp(6), dp(12), w - dp(16) - i * dp(24), dp(24));   // the round buttons
            cards[3] = new RectF(dp(12), dp(36), w * 0.5f, h - dp(12));   // a card
            for (int i = 0; i < 3; i++) cards[4 + i] = new RectF(w * 0.55f + i * (w * 0.15f), h - dp(34), w * 0.55f + i * (w * 0.15f) + w * 0.12f, h - dp(12));   // the favourites
            for (int i = 0; i < cards.length; i++) {
                float cr = i < 3 ? dp(6) : i == 3 ? dp(8) : dp(4);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(GlassStyle.over(i < 4 ? l.panel : l.dialog, l.base));
                c.drawRoundRect(cards[i], cr, cr, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1, dp(1)));
                paint.setColor(l.line);
                c.drawRoundRect(cards[i], cr, cr, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(l.accent);
            c.drawCircle(dp(24), h - dp(24), dp(4), paint);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = dp(8);
            RectF all = new RectF(0, 0, w, h);
            if (kind == HOME) {
                if (l2 == null) drawHome(c, l, w, h, r);
                else {
                    c.save(); c.clipRect(0, 0, w / 2, h); drawHome(c, l, w, h, r); c.restore();
                    c.save(); c.clipRect(w / 2, 0, w, h); drawHome(c, l2, w, h, r); c.restore();
                }
            } else if (kind == CARD) {
                paint.setColor(0xFF1C2A24);
                c.drawRoundRect(all, r, r, paint);   // the photo
                boolean glass = key.startsWith("glass"), dark = key.startsWith("dark");
                RectF card = new RectF(w * 0.12f, h * 0.2f, w * 0.88f, h * 0.8f);
                paint.setColor(glass ? 0x40FFFFFF : dark ? 0xCC0A120E : 0x990A120E);
                c.drawRoundRect(card, dark || glass ? dp(10) : dp(4), dark || glass ? dp(10) : dp(4), paint);
                if (key.endsWith("3")) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(0xFFD4AF6A);
                    c.drawRoundRect(new RectF(card.left + dp(4), card.top + dp(4), card.right - dp(4), card.bottom - dp(4)), dp(8), dp(8), paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(0xFFF6EFE0);
                c.drawRoundRect(new RectF(card.left + dp(12), card.top + dp(12), card.left + dp(60), card.top + dp(20)), dp(3), dp(3), paint);
                paint.setColor(0x99F6EFE0);
                c.drawRoundRect(new RectF(card.left + dp(12), card.top + dp(28), card.right - dp(12), card.top + dp(34)), dp(3), dp(3), paint);
            } else {
                int[] tints = {0xFF2B4A3B, 0xFF3B3A4F, 0xFF4F3F2B};
                for (int i = 2; i >= 0; i--) {   // frames, the nearest in front
                    float o = i * dp(10);
                    paint.setColor(tints[Math.abs(key.hashCode() + i) % tints.length]);
                    c.drawRoundRect(new RectF(dp(12) + o, dp(10) + o * 0.4f, w - dp(36) + o, h - dp(26) + o * 0.4f), dp(6), dp(6), paint);
                }
            }
            if (on) {   // the one in use: a ring
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(look.accent);
                c.drawRoundRect(new RectF(dp(1), dp(1), w - dp(1), h - dp(1)), r, r, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    // ---- small things ----

    private static String w(String ar) { return Words.of(ar); }

    private String conv(String s) { return DigitsText.convert(s, "latin".equals(prefs.digits())); }

    private TextView text(String s, int sp, String font, int color) {
        TextView t = new TextView(this);
        t.setText(conv(s));
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(font(font));
        t.setGravity(Gravity.START);
        t.setTextDirection(Dir.text());
        return t;
    }

    private View button(String label, View.OnClickListener click) {
        TextView t = text(label, 18, "Medium", look.text);
        t.setPadding(dp(24), dp(14), dp(24), dp(14));
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnClickListener(click);
        GlassPaint.card(t, look, 14);
        return t;
    }

    private LinearLayout.LayoutParams line(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private void toast(String s) { Toast.makeText(this, conv(s), Toast.LENGTH_LONG).show(); }

    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + name + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
