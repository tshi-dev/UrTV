package sa.urtv.screensaver;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.StatFs;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * «صور شاشة التوقف»: the photo packs, reached from Settings › شاشة التوقف. Each pack is a card with its cover, its photo count and size, and its
 * source. OK on one that is not on the TV downloads it (checked against its sha256 before use, PackStore); OK on one that is on the TV turns
 * it on or off, or deletes it («حذف») or shows its credits. Several can be on at once and the screensaver rotates across all of them, with
 * our own set («صورنا», on by default). A pack that is on shows the three green triangles (the on mark of every setting) and «مفعّل».
 * Arabic and English as every screen of this app (Words.of); the glass look of all our screens.
 */
public class PhotoPacksActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.Context en = S.wrap(base);
        if (S.en()) { super.attachBaseContext(en); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    private static final int COLUMNS = 3;
    private Prefs prefs;
    private FrameLayout root;
    private LinearLayout grid;
    private TextView free;
    private GlassStyle.Look look = GlassStyle.DARK;
    private List<PhotoPacks.Pack> packs = new ArrayList<>();
    private boolean loaded;
    private final java.util.Set<String> busy = new java.util.HashSet<>();   // being downloaded now
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
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutDirection(Dir.layout());
        grid.setPadding(dp(16), dp(40), dp(16), dp(32));
        scroll.addView(grid, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - dp(96), dp(1100)), -1);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(scroll, lp);
        setContentView(root);
        render();
        new Thread(() -> {   // the index off the UI thread: the kept copy when offline
            final List<PhotoPacks.Pack> v = PackStore.index(getApplicationContext());
            runOnUiThread(() -> { if (isFinishing()) return; packs = v; loaded = true; render(); });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        look = GlassSource.page(this, root);
    }

    // ---- drawing ----

    private void render() {
        View focused = getCurrentFocus();
        Object want = focused == null ? null : focused.getTag();
        grid.removeAllViews();
        grid.addView(text(w("صور شاشة التوقف"), 28, "Medium", look.text), line(dp(8)));
        grid.addView(text(w("اختر مجموعة أو أكثر. نزّل مرة واحدة وتبقى في التلفزيون، والصور تتبدّل بينها."), 16, "Regular", look.sub), line(dp(24)));

        Map<String, Integer> have = PackStore.installed(this);
        Set<String> on = prefs.packsOn();
        List<View> cards = new ArrayList<>();
        cards.add(oursCard(have, on));
        for (PhotoPacks.Pack p : packs) cards.add(packCard(p, have, on));
        for (int i = 0; i < cards.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(Dir.layout());
            for (int j = 0; j < COLUMNS; j++) {
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
                cp.setMargins(dp(8), dp(8), dp(8), dp(8));
                row.addView(j + i < cards.size() ? cards.get(i + j) : new View(this), cp);
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        if (loaded && packs.isEmpty()) grid.addView(text(w("لا توجد مجموعات للتنزيل الآن."), 16, "Regular", look.sub), line(dp(16)));
        if (!loaded) grid.addView(text(w("جارٍ التحميل…"), 16, "Regular", look.sub), line(dp(16)));

        LinearLayout foot = new LinearLayout(this);
        foot.setLayoutDirection(Dir.layout());
        foot.setGravity(Gravity.CENTER_VERTICAL);
        TextView note = text(w("بلا أشخاص، وكل صورة بمصدرها ورخصتها في «حقوق الصور»."), 14, "Regular", look.sub);
        foot.addView(note, new LinearLayout.LayoutParams(0, -2, 1f));
        free = text(conv(w("المساحة الفاضية: %s", freeText())), 14, "Regular", look.sub);
        foot.addView(free, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(16);
        grid.addView(foot, fp);
        View credits = button(w("حقوق الصور"), v -> showCredits());
        credits.setTag("credits");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.topMargin = dp(16);
        bp.setMarginStart(dp(8));
        grid.addView(credits, bp);
        restoreFocus(want);
    }

    private void restoreFocus(Object tag) {
        View target = tag == null ? null : grid.findViewWithTag(tag);
        if (target == null) target = grid.findViewWithTag("ours");
        if (target != null) target.requestFocus();
    }

    private View oursCard(Map<String, Integer> have, Set<String> on) {
        boolean ours = prefs.oursOn() || !PhotoPacks.mayTurnOursOff(on, have);
        return card("ours", null, w("صورنا"), ours, w("مع التطبيق"), w("Unsplash"), null, v -> {
            if (ours && !PhotoPacks.mayTurnOursOff(on, have)) { toast(w("فعّل مجموعة منزّلة أولاً، وبعدها تقدر توقف صورنا")); return; }
            prefs.setOursOn(!ours);
            render();
        });
    }

    private View packCard(PhotoPacks.Pack p, Map<String, Integer> have, Set<String> on) {
        boolean there = have.containsKey(p.id), active = there && on.contains(p.id);
        String state = busy.contains(p.id) ? "downloading" : there ? (active ? "on" : "off") : "get";
        String name = S.en() ? p.nameEn : p.name, source = S.en() ? p.sourceEn : p.source;
        String meta = conv(w("%s صورة · %s م.ب", String.valueOf(p.count), String.valueOf(PhotoPacks.megabytes(p.bytes))));
        return card("pack:" + p.id, p, name, active, meta, source, state, v -> {
            if (busy.contains(p.id)) return;
            if (!there) { download(p); return; }
            choose(p, active);
        });
    }

    /** one card: the cover, the name with the on mark, the state word, the count and size, and the source */
    private View card(String tag, PhotoPacks.Pack p, String name, boolean active, String meta, String source, String state, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(Dir.layout());
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(tag);
        card.setOnClickListener(click);
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackgroundColor(look.line);
        card.addView(cover, new LinearLayout.LayoutParams(-1, dp(150)));
        loadCover(cover, p);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout head = new LinearLayout(this);
        head.setLayoutDirection(Dir.layout());
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageDrawable(getDrawable(R.drawable.ic_on).mutate());
        mark.getDrawable().setTint(OnOff.green(look.light ? 0xFFFFFFFF : 0xFF000000));
        mark.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        head.addView(mark, new LinearLayout.LayoutParams(dp(9), dp(28)));
        TextView n = text(name, 20, "Medium", look.text);
        n.setSingleLine(true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.setMarginStart(dp(8));
        head.addView(n, np);
        body.addView(head, new LinearLayout.LayoutParams(-1, -2));
        String word = state == null ? (active ? "مفعّل" : "متوقف") : "downloading".equals(state) ? "جارٍ التنزيل…" : "on".equals(state) ? "مفعّل" : "off".equals(state) ? "متوقف" : "نزّل";
        TextView st = text(w(word), 16, "Medium", active ? OnOff.green(look.light ? 0xFFFFFFFF : 0xFF000000) : look.accent);
        body.addView(st, line(dp(8)));
        body.addView(text(meta, 14, "Regular", look.sub), line(dp(4)));
        body.addView(text(source, 14, "Regular", look.sub), line(0));
        card.addView(body, new LinearLayout.LayoutParams(-1, -2));
        GlassPaint.card(card, look, 14);
        return card;
    }

    private void loadCover(ImageView cover, PhotoPacks.Pack p) {
        final java.io.File own = p == null ? null : PackStore.photo(this, p.id, 1);
        if (p == null) {   // «صورنا»: one of the app's own photos
            try (java.io.InputStream in = getAssets().open("backgrounds/" + firstBackground())) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 4;
                cover.setImageBitmap(BitmapFactory.decodeStream(in, null, o));
            } catch (java.io.IOException | RuntimeException ignored) { }
            return;
        }
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            java.io.File f = own.isFile() ? own : PackStore.coverOf(app, p);
            if (f == null) return;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            final Bitmap bm = BitmapFactory.decodeFile(f.getPath(), o);
            if (bm != null) runOnUiThread(() -> { if (!isFinishing()) cover.setImageBitmap(bm); });
        }).start();
    }

    private String firstBackground() {
        try {
            String[] all = getAssets().list("backgrounds");
            if (all != null && all.length > 0) { java.util.Arrays.sort(all); return all[0]; }
        } catch (java.io.IOException ignored) { }
        return "";
    }

    // ---- what OK does ----

    private void download(PhotoPacks.Pack p) {
        busy.add(p.id);
        render();
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final String why = PackStore.download(app, p);
            runOnUiThread(() -> {
                busy.remove(p.id);
                if (why == null) { prefs.setPacksOn(PhotoPacks.toggled(prefs.packsOn(), p.id, true)); }
                if (!isFinishing()) {
                    toast(why == null ? w("تم التنزيل، والمجموعة مفعّلة")
                        : "sha".equals(why) ? w("الملف لم يطابق بصمته فلم يُستخدم")
                        : "space".equals(why) ? w("لا توجد مساحة كافية") : "bad".equals(why) ? w("المجموعة غير صالحة") : w("تعذّر التنزيل، تحقق من الإنترنت"));
                    render();
                }
            });
        }).start();
    }

    /** a pack that is on the TV: turn it on or off, delete it */
    private void choose(PhotoPacks.Pack p, boolean active) {
        String[] items = {active ? "إيقاف" : "تشغيل", "حذف"};
        WordsBuilder dlg = new WordsBuilder(this, look.light ? R.style.UrDialogLight : R.style.UrDialog).own();   // the pack's name is data, not a word to translate
        dlg.setTitle(S.en() ? p.nameEn : p.name)
            .setItems(new String[] {w(items[0]), w(items[1])}, (d, i) -> {
                if (i == 0) prefs.setPacksOn(PhotoPacks.toggled(prefs.packsOn(), p.id, !active));
                else {
                    PackStore.delete(this, p.id);
                    prefs.setPacksOn(PhotoPacks.toggled(prefs.packsOn(), p.id, false));
                }
                render();
            }).show();
    }

    /** «حقوق الصور»: every photo of the packs on the TV with its author, licence and link; and the app's own set */
    private void showCredits() {
        StringBuilder sb = new StringBuilder();
        for (PhotoPacks.Pack p : packs) {
            List<String[]> rows = PackStore.creditRows(this, p.id);
            if (rows.isEmpty()) continue;
            sb.append(S.en() ? p.nameEn : p.name).append('\n');
            for (String[] r : rows) sb.append("  ").append(r[0]).append(" · ").append(PhotoPacks.creditLine(r[1], r[2])).append(" · ").append(r[3]).append('\n');
            sb.append('\n');
        }
        sb.append(w("صورنا: Unsplash، وكل صورة في docs/PHOTO-CREDITS.md"));
        TextView body = text(sb.toString(), 14, "Regular", look.text);
        body.setPadding(dp(24), dp(16), dp(24), dp(8));
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        new WordsBuilder(this, look.light ? R.style.UrDialogLight : R.style.UrDialog).setTitle(w("حقوق الصور")).setView(sv)
            .setPositiveButton(w("إغلاق"), null).show();
    }

    // ---- small things ----

    private String freeText() {
        try {
            StatFs fs = new StatFs(getFilesDir().getPath());
            double gb = fs.getAvailableBytes() / (1024.0 * 1024 * 1024);
            return gb >= 1 ? String.format(java.util.Locale.ROOT, "%.1f", gb) + " ج.ب" : (fs.getAvailableBytes() >> 20) + " م.ب";
        } catch (RuntimeException e) {
            return "—";
        }
    }

    /** the text in «اللغة» (Words.of), with %s filled */
    private static String w(String ar, String... args) {
        String t = ar;
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            int k = 0;
            for (String part : ar.split("%s", -1)) { sb.append(part); if (k < args.length) sb.append(args[k++]); }
            t = sb.toString();
        }
        return Words.of(t);
    }

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
