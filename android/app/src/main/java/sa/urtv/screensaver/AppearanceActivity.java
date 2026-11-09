package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
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
        int safe = Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f);   // the TV's safe area: 5 % in from the top and the bottom edge
        col.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        // the scroll's own viewport ends inside the safe area (a margin, not padding of what scrolls): a row below the first screen is cut at y 1026 and comes in with the focus,
        // it is never drawn out to y 1080 (the tester's step 44: «رجوع» and the last row reached the edge until one more DOWN)
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - dp(96), dp(1100)), -1);
        lp.topMargin = safe;
        lp.bottomMargin = safe;
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

    /** null: the page of six categories; else the one open (look, accent, light, cards, wall, density) */
    private String sub;
    private static final String[][] CATS = {{"look", "الشكل"}, {"accent", "اللون"}, {"light", "الإضاءة"}, {"cards", "البطاقات"}, {"wall", "الخلفية والصور"}, {"density", "الحجم والكثافة"}};

    @Override public void onBackPressed() {
        if (sub != null) { String was = sub; sub = null; render(); View t = col.findViewWithTag("cat:" + was); if (t != null) t.requestFocus(); }
        else super.onBackPressed();
    }

    private void render() {
        View focused = getCurrentFocus();
        Object want = focused == null ? null : focused.getTag();
        col.removeAllViews();
        if (sub == null) renderMain(); else renderSub();
        View back = button(w("رجوع"), v -> onBackPressed());
        back.setTag("back");
        col.addView(back, line(dp(8)));
        restoreFocus(want);
    }

    /** «المظهر»: one place for the look of both apps; each category is a tile with a picture of what is chosen now */
    private void renderMain() {
        col.addView(text(w("المظهر"), 28, "Medium", look.text), line(dp(8)));
        col.addView(text(w("كل ما يخص شكل التلفزيون في مكان واحد، وكل خيار يظهر قبل أن تختاره."), 16, "Regular", look.sub), line(dp(24)));
        boolean light = "light".equals(prefs.appearance());
        List<View> cells = new ArrayList<>();
        for (final String[] c : CATS) {
            Mock m; String value;
            switch (c[0]) {
                case "look": m = new Mock(Mock.HOME, prefs.look(), GlassStyle.of(prefs.look(), light)); value = Prefs.lookLabel(prefs.look()); break;
                case "accent": m = new Mock(Mock.DOTS, "all", look); value = prefs.accent().isEmpty() ? w("حسب الشكل") : accentName(prefs.accent()); break;
                case "light": m = new Mock(Mock.HOME, prefs.look(), GlassStyle.of(prefs.look(), false), GlassStyle.of(prefs.look(), true)); value = Prefs.appearanceLabel(prefs.appearance()); break;
                case "cards": m = new Mock(Mock.CARD, prefs.cardStyle(), GlassStyle.of(prefs.look(), false)); value = w(cardStyleName()); break;
                case "wall": m = new Mock(Mock.PHOTOS, "own", look); value = w("تعتيم الخلفية") + " " + prefs.wallDim() + "٪"; break;
                default: m = new Mock(Mock.BARS, prefs.favBar(), look); value = Prefs.favBarLabel(prefs.favBar()); break;
            }
            cells.add(category("cat:" + c[0], m, w(c[1]), conv(value), v -> { sub = c[0]; render(); }));
        }
        grid(cells, 3);
    }

    private String cardStyleName() { for (String[] c : Prefs.CARD_STYLES) if (c[0].equals(prefs.cardStyle())) return c[1]; return Prefs.CARD_STYLES[0][1]; }

    private String accentName(String key) { for (String[] a : GlassStyle.ACCENTS) if (a[0].equals(key)) return w(a[1]); return ""; }

    private void renderSub() {
        String title = "";
        for (String[] c : CATS) if (c[0].equals(sub)) title = c[1];
        col.addView(text(w("المظهر") + " ‹ " + w(title), 28, "Medium", look.text), line(dp(24)));
        boolean light = "light".equals(prefs.appearance());
        List<View> cells = new ArrayList<>();
        switch (sub) {
            case "look": {
                for (int i = 0; i < GlassStyle.LOOKS.length; i++) {
                    final String key = GlassStyle.LOOKS[i];
                    cells.add(tile("look:" + key, new Mock(Mock.HOME, key, GlassStyle.of(key, light)), w(GlassStyle.LOOK_NAMES[i]) + (i == 0 ? " · " + w("الافتراضي") : ""), key.equals(prefs.look()), v -> { prefs.setLook(key); render(); }));
                }
                grid(cells, 5);
                col.addView(text(w("الواجهة"), 16, "Regular", look.sub), line(dp(8)));
                List<View> layouts = new ArrayList<>();
                for (int i = 0; i < Prefs.HOME_LAYOUTS.length; i++) {
                    final String key = Prefs.HOME_LAYOUTS[i];
                    layouts.add(tile("layout:" + key, new Mock(Mock.HOME, prefs.look(), GlassStyle.of(prefs.look(), light)), w(Prefs.HOME_LAYOUT_NAMES[i]), key.equals(prefs.homeLayout()), v -> { prefs.setHomeLayout(key); render(); }));
                }
                grid(layouts, 5);
                break;
            }
            case "accent": {
                cells.add(tile("accent:", new Mock(Mock.DOTS, "all", look), w("حسب الشكل"), prefs.accent().isEmpty(), v -> { prefs.setAccent(""); render(); }));
                for (final String[] a : GlassStyle.ACCENTS) cells.add(tile("accent:" + a[0], new Mock(Mock.DOTS, a[2], look), w(a[1]), a[0].equals(prefs.accent()), v -> { prefs.setAccent(a[0]); render(); }));
                grid(cells, 5);
                break;
            }
            case "light": {
                for (final String a : Prefs.APPEARANCES) {
                    cells.add(tile("light:" + a, new Mock(Mock.HOME, prefs.look(), GlassStyle.of(prefs.look(), "light".equals(a)), "auto".equals(a) ? GlassStyle.of(prefs.look(), true) : null), w(shortLight(a)), a.equals(prefs.appearance()), v -> { prefs.setAppearance(a); render(); }));
                }
                grid(cells, 3);
                break;
            }
            case "cards": {
                col.addView(text(w("شكل البطاقات"), 16, "Regular", look.sub), line(dp(8)));
                for (String[] c : Prefs.CARD_STYLES) {
                    final String key = c[0];
                    cells.add(tile("style:" + key, new Mock(Mock.CARD, key, GlassStyle.of(prefs.look(), false)), w(c[1]), key.equals(prefs.cardStyle()), v -> { prefs.setCardStyle(key); render(); }));
                }
                grid(cells, 4);
                col.addView(text(w("بطاقات الشاشة الرئيسية وشريطها من لون الشكل نفسه، فتتناسق في كل شكل."), 16, "Regular", look.sub), line(dp(8)));
                break;
            }
            case "wall": {
                col.addView(text(w("شاشة التوقف"), 16, "Regular", look.sub), line(dp(8)));
                col.addView(photosRow(), line(dp(16)));
                col.addView(text(w("الصور في الشاشة الرئيسية"), 16, "Regular", look.sub), line(dp(8)));
                col.addView(homePhotosRow(), line(dp(16)));
                col.addView(stepper("dim", w("تعتيم الخلفية"), prefs.wallDim(), 0, 90, 10, v -> { prefs.setWallDim(v); render(); }), line(dp(16)));
                break;
            }
            default: {
                col.addView(text(w("تطبيقات في الشريط"), 16, "Regular", look.sub), line(dp(8)));
                for (final String n : Prefs.FAV_BARS) cells.add(tile("fav:" + n, new Mock(Mock.BARS, n, look), conv(Prefs.favBarLabel(n)), n.equals(prefs.favBar()), v -> { prefs.setFavBar(n); render(); }));
                grid(cells, 5);
                col.addView(stepper("bar", w("شفافية الشريط"), prefs.barPercent(), 30, 100, 10, v -> { prefs.setBarPercent(v); render(); }), line(dp(16)));
                col.addView(text(w("شكل التركيز"), 16, "Regular", look.sub), line(dp(8)));
                List<View> fs = new ArrayList<>();
                String[] names = {"ممتلئ", "إطار", "توهّج"};
                for (int i = 0; i < GlassStyle.FOCUS_STYLES.length; i++) {
                    final String key = GlassStyle.FOCUS_STYLES[i];
                    fs.add(tile("focus:" + key, new Mock(Mock.FOCUS, key, look), w(names[i]), key.equals(prefs.focusStyle()), v -> { prefs.setFocusStyle(key); render(); }));
                }
                grid(fs, 3);
                break;
            }
        }
    }

    /** a left-right control: ◀ less, ▶ more, in steps; the value is shown with its bar */
    private View stepper(String tag, String name, final int value, final int min, final int max, final int step, final IntChange change) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setLayoutDirection(Dir.layout());
        t.setPadding(dp(16), dp(12), dp(16), dp(12));
        t.setFocusable(true);
        t.setClickable(true);
        t.setTag("step:" + tag);
        LinearLayout head = new LinearLayout(this);
        head.setLayoutDirection(Dir.layout());
        TextView n = text(name, 18, "Medium", look.text);
        head.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(text(conv(String.valueOf(value)) + "٪", 18, "Medium", look.accent), new LinearLayout.LayoutParams(-2, -2));
        t.addView(head, new LinearLayout.LayoutParams(-1, -2));
        final float frac = (value - min) / (float) (max - min);
        View bar = new View(this) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                float w = getWidth(), h = getHeight(), r = h / 2;
                p.setColor(look.line);
                c.drawRoundRect(new RectF(0, 0, w, h), r, r, p);
                p.setColor(look.accent);
                boolean rtl = Dir.rtl();
                float fill = w * frac;
                c.drawRoundRect(rtl ? new RectF(w - fill, 0, w, h) : new RectF(0, 0, fill, h), r, r, p);
            }
        };
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(8));
        bp.topMargin = dp(8);
        t.addView(bar, bp);
        t.setOnKeyListener((v, code, ev) -> {
            if (ev.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            boolean left = code == android.view.KeyEvent.KEYCODE_DPAD_LEFT, right = code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
            if (!left && !right) return false;
            boolean more = Dir.rtl() ? left : right;   // in Arabic the bar fills toward the left
            change.to(Math.max(min, Math.min(max, value + (more ? step : -step))));
            return true;
        });
        GlassPaint.card(t, look, 14);
        return t;
    }

    private interface IntChange { void to(int v); }

    /** the tiles of one choice in rows of {@code per}; the first tile carries the tag "first" for the focus when the screen opens */
    private void grid(List<View> cells, int per) {
        for (int i = 0; i < cells.size(); i += per) {
            LinearLayout row = tiles();
            for (int j = 0; j < per; j++) {
                if (i + j < cells.size()) row.addView(cells.get(i + j), tileParams());
                else row.addView(new View(this), tileParams());
            }
            col.addView(row, line(dp(8)));
        }
    }

    /** a big tile of the main page: the picture of what is chosen, the category and its value */
    private View category(String tag, Mock mock, String name, String value, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setLayoutDirection(Dir.layout());
        t.setPadding(dp(16), dp(16), dp(16), dp(16));
        t.setFocusable(true);
        t.setClickable(true);
        t.setTag(tag);
        t.setOnClickListener(click);
        t.addView(mock, new LinearLayout.LayoutParams(-1, dp(104)));
        TextView n = text(name, 20, "Medium", look.text);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        t.addView(n, np);
        t.addView(text(value, 16, "Regular", look.sub), new LinearLayout.LayoutParams(-1, -2));
        GlassPaint.card(t, look, 14);
        return t;
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
        // «صوري» of the home's own: with ours, only, or none (never chosen: as the screensaver's), one tile that goes round
        LinearLayout mineRow = tiles();
        final String mode = prefs.homeMineEffective();
        String mineName = Own.count(this) == 0 ? w("صوري") : "only".equals(mode) ? w("صوري فقط") : "off".equals(mode) ? w("بدون صوري") : w("صوري مع صورنا");
        mineRow.addView(tile("home:mine", new Mock(Mock.PHOTOS, "own", look), mineName, !"off".equals(mode) && Own.count(this) > 0, v -> {
            if (Own.count(this) == 0) { toast(w("لا صور لك بعد. تضاف من الجوال.")); return; }
            prefs.setHomeMine("mix".equals(mode) ? "only" : "only".equals(mode) ? "off" : "mix");
            render();
        }), tileParams());
        all.addView(mineRow, line(dp(8)));
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

    private static View firstFocusable(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View v = g.getChildAt(i);
            if (v.isFocusable() && v.getTag() != null && !"back".equals(v.getTag())) return v;
            if (v instanceof ViewGroup) { View f = firstFocusable((ViewGroup) v); if (f != null) return f; }
        }
        return null;
    }

    private void restoreFocus(Object tag) {
        View target = tag == null ? null : col.findViewWithTag(tag);
        if (target == null) target = firstFocusable(col);
        if (target != null) target.requestFocus();
    }

    /**
     * The small picture on a tile, drawn in the colours it stands for: HOME is the home in that look (its ground, three round buttons, a card, a line of the clock); CARD is the
     * screensaver's card in that style over a dark photo; PHOTOS is a stack of photo frames (a different tint for each source).
     */
    private final class Mock extends View {
        static final int HOME = 0, CARD = 1, PHOTOS = 2, DOTS = 3, BARS = 4, FOCUS = 5;
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
            } else if (kind == DOTS) {   // the accents: all eight in a row, or the one colour
                int n = "all".equals(key) ? GlassStyle.ACCENTS.length : 1;
                float d = Math.min(dp(22), (w - dp(16)) / (n * 1.4f));
                for (int i = 0; i < n; i++) {
                    paint.setColor("all".equals(key) ? Color.parseColor(GlassStyle.ACCENTS[i][2]) : key.startsWith("#") ? Color.parseColor(key) : look.accent);
                    float cx = w / 2 + (i - (n - 1) / 2f) * d * 1.4f;
                    c.drawRoundRect(new RectF(cx - d / 2, h / 2 - d / 2, cx + d / 2, h / 2 + d / 2), dp(6), dp(6), paint);
                }
            } else if (kind == BARS) {   // the tray: as many tiles as the choice shows
                int n = "all".equals(key) ? 9 : Math.max(1, Integer.parseInt(key));
                paint.setColor(GlassStyle.over(l.panel, l.base));
                c.drawRoundRect(new RectF(dp(8), h * 0.3f, w - dp(8), h * 0.8f), dp(8), dp(8), paint);
                float tw = (w - dp(32)) / n;
                for (int i = 0; i < n; i++) {
                    paint.setColor(i == n / 2 ? l.focus : GlassStyle.over(l.dialog, l.base));
                    c.drawRoundRect(new RectF(dp(16) + i * tw + dp(1), h * 0.36f, dp(16) + (i + 1) * tw - dp(1), h * 0.74f), dp(3), dp(3), paint);
                }
            } else if (kind == FOCUS) {   // one tile with the focus line in its style
                paint.setColor(l.focus);
                RectF tile = new RectF(w * 0.25f, h * 0.2f, w * 0.75f, h * 0.8f);
                c.drawRoundRect(tile, dp(10), dp(10), paint);
                paint.setStyle(Paint.Style.STROKE);
                if ("frame".equals(key)) { paint.setStrokeWidth(dp(4)); paint.setColor(l.accent); c.drawRoundRect(tile, dp(10), dp(10), paint); }
                else if ("glow".equals(key)) { paint.setStrokeWidth(dp(8)); paint.setColor((l.accent & 0x00FFFFFF) | 0x66000000); c.drawRoundRect(tile, dp(10), dp(10), paint); }
                paint.setStyle(Paint.Style.FILL);
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
