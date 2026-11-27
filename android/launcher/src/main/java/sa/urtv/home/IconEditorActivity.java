/*
 * Copyright 2026 techgenius (tvOS Launcher, github.com/techgenius1/tvOS-Launcher)
 * Copyright 2026 UrTV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * The editor's idea is tvOS Launcher's custom app icon (CustomMedia: an image, an edge-sampled «auto» colour among a set of colours, zoom and
 * move). Changed by UrTV: its own screen, Arabic first and right to left; the image from the TV's storage or a USB drive; UrTV's three shapes;
 * every render and file read off the main thread (GlassArt, IconStore). THIRD_PARTY_NOTICES.md has the notice.
 */
package sa.urtv.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 4.5 «تغيير الأيقونة» (a long OK on an app in «زجاجي», then «تغيير الأيقونة»; docs/mockups/glass/glass-icon-editor.png): the app's icon from an
 * image on the TV's storage or a USB drive, or the app's own icon back; a background colour (13, or «تلقائي»: the image's edge colour); the
 * shape (square, rounded square, circle); a zoom and a move (OK on the preview, then the arrows; OK or Back ends it); a live preview; «حفظ»
 * and «إلغاء», rectangles. The icon is kept in this app's files (IconStore), never the app's own.
 */
public final class IconEditorActivity extends Activity {
    static final String PKG = "pkg", LOOK = "look", LIGHT = "light";
    private boolean light;
    static final int ASK_READ = 41, MAX_IMAGES = 80, DEPTH = 4;

    /** the colours: «تلقائي» (0, the edge colour), then 13 */
    static final int[] COLOURS = {0, 0xFF1C1C1E, 0xFFFFFFFF, 0xFF8E8E93, 0xFFE53935, 0xFFFF7043, 0xFFFFB300, 0xFFFDD835,
            0xFF43A047, 0xFF00897B, 0xFF039BE5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60};
    static final String[] SHAPES = {GlassArt.SQUARE, GlassArt.ROUNDED, GlassArt.CIRCLE};
    static final String[] SHAPE_NAMES = {S.n("مربع"), S.n("مربع بحواف دائرية"), S.n("دائرة")};

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private String pkg;
    private Bitmap image;   // the chosen image (null: the app's own icon)
    private Bitmap appIcon;
    private int colour;
    private String shape = GlassArt.ROUNDED;
    private float zoom = 1f, dx, dy;
    private boolean moving, own;
    private ImageView preview;
    private FrameLayout previewBox;
    private LinearLayout strip;
    private TextView stripNote;
    private Typeface medium;
    private GlassStyle.Look look;
    private int renderGen;
    private volatile boolean alive = true;
    private String pendingSource;

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        pkg = getIntent().getStringExtra(PKG);
        if (pkg == null) { finish(); return; }
        medium = font("Thmanyah-Medium");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setLayoutDirection(Dir.layout());
        root.setPadding(dp(48), dp(32), dp(48), dp(24));
        // the look and the light come from the home that opened this (no read of the screensaver's settings on the main thread); the page is the
        // home's photo, already blurred once (GlassSource), under its veil: the frost
        light = getIntent().getBooleanExtra(LIGHT, false);
        look = GlassStyle.of(GlassStyle.look(getIntent().getStringExtra(LOOK)), light);
        root.setBackgroundColor(look.base);   // the look's own colour at once; the photo when it was read on the worker
        final GlassStyle.Look pageLook = look;
        worker.execute(() -> {
            Bitmap photo = GlassSource.backdrop(this);
            float veil = GlassSource.scrim(this, pageLook);
            runOnUiThread(() -> { if (alive && photo != null) root.setBackground(GlassPaint.page(this, photo, pageLook, veil)); });
        });

        // the preview at the start side, the controls beside it
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = label(22, look.text);
        title.setText(S.t("تغيير الأيقونة"));
        left.addView(title, new LinearLayout.LayoutParams(-2, -2));
        previewBox = new FrameLayout(this);
        previewBox.setFocusable(true);
        previewBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        previewBox.setBackground(frame(dp(36)));
        previewBox.setContentDescription(S.t("المعاينة: اضغط موافق ثم الأسهم لتحريك الصورة"));
        preview = new ImageView(this);
        previewBox.addView(preview, new FrameLayout.LayoutParams(dp(176), dp(176), Gravity.CENTER));
        previewBox.setOnClickListener(v -> setMoving(!moving));
        previewBox.setOnKeyListener((v, k, e) -> moveKey(k, e));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.topMargin = dp(20);
        left.addView(previewBox, pp);
        TextView hint = label(12, look.sub);
        hint.setText(S.t("موافق على المعاينة ثم الأسهم لتحريك الصورة"));
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(220), -2);
        hp.topMargin = dp(10);
        left.addView(hint, hp);
        root.addView(left, new LinearLayout.LayoutParams(-2, -1));

        ScrollView scroll = new ScrollView(this);   // the large font and 720p: every row reachable
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), dp(4), dp(8), dp(8));
        scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -1, 1f);
        sp.setMarginStart(dp(40));
        root.addView(scroll, sp);

        col.addView(heading(S.t("الصورة")));
        LinearLayout src = row();
        src.addView(button(S.t("من التخزين"), () -> pick("storage")), cell());
        src.addView(button(S.t("من USB"), () -> pick("usb")), cell());
        src.addView(button(S.t("الأيقونة الأصلية"), this::original), cell());
        col.addView(src);
        stripNote = label(12, look.sub);
        stripNote.setVisibility(View.GONE);
        col.addView(stripNote);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setLayoutDirection(Dir.layout());
        strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setLayoutDirection(Dir.layout());
        strip.setPadding(dp(4), dp(6), dp(4), dp(6));
        hs.addView(strip);
        col.addView(hs, new LinearLayout.LayoutParams(-1, -2));

        col.addView(heading(S.t("لون الخلفية")));
        LinearLayout colours = row();
        for (int c : COLOURS) colours.addView(swatch(c), swatchCell());
        HorizontalScrollView ch = new HorizontalScrollView(this);
        ch.setHorizontalScrollBarEnabled(false);
        ch.setLayoutDirection(Dir.layout());
        ch.addView(colours);
        col.addView(ch, new LinearLayout.LayoutParams(-1, -2));

        col.addView(heading(S.t("الشكل")));
        LinearLayout shapes = row();
        for (int i = 0; i < SHAPES.length; i++) { final String s = SHAPES[i]; shapes.addView(button(S.tv(SHAPE_NAMES[i]), () -> { shape = s; render(); }), cell()); }
        col.addView(shapes);

        col.addView(heading(S.t("الحجم")));
        LinearLayout zooms = row();
        zooms.addView(button(S.t("تكبير"), () -> { zoom = Math.min(3f, zoom + 0.1f); render(); }), cell());
        zooms.addView(button(S.t("تصغير"), () -> { zoom = Math.max(0.5f, zoom - 0.1f); render(); }), cell());
        zooms.addView(button(S.t("في الوسط"), () -> { zoom = 1f; dx = 0; dy = 0; render(); }), cell());
        col.addView(zooms);

        LinearLayout end = row();
        LinearLayout.LayoutParams ep = cell();
        View save = button(S.t("حفظ"), this::save);
        end.addView(save, ep);
        end.addView(button(S.t("إلغاء"), () -> { setResult(RESULT_CANCELED); finish(); }), cell());
        LinearLayout.LayoutParams endP = new LinearLayout.LayoutParams(-1, -2);
        endP.topMargin = dp(22);
        col.addView(end, endP);

        setContentView(root);
        worker.execute(() -> {   // the app's icon (and the person's, if kept) off the main thread
            try {
                Bitmap ic = GlassArt.trim(GlassArt.bitmapOf(getPackageManager().getApplicationIcon(pkg), 256));
                runOnUiThread(() -> { if (!alive) return; appIcon = ic; render(); });
            } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                runOnUiThread(() -> finish());   // the app is gone: nothing to change
            }
        });
        save.post(() -> ((View) src.getChildAt(0)).requestFocus());
    }

    /** OK on the preview: the arrows move the image (a 20th a step) until OK or Back; every other key goes on */
    private boolean moveKey(int k, KeyEvent e) {
        if (!moving) return false;
        boolean arrow = k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN;
        if (k == KeyEvent.KEYCODE_BACK) { if (e.getAction() == KeyEvent.ACTION_UP) setMoving(false); return true; }
        if (!arrow) return false;
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        float step = 0.05f;
        if (k == KeyEvent.KEYCODE_DPAD_LEFT) dx -= step;
        else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) dx += step;
        else if (k == KeyEvent.KEYCODE_DPAD_UP) dy -= step;
        else dy += step;
        dx = Math.max(-0.5f, Math.min(0.5f, dx));
        dy = Math.max(-0.5f, Math.min(0.5f, dy));
        render();
        return true;
    }

    private void setMoving(boolean m) {
        moving = m;
        previewBox.setActivated(m);
        if (m) Toast.makeText(this, S.t("حرّك بالأسهم، وموافق للإنهاء"), Toast.LENGTH_SHORT).show();
    }

    /** the preview drawn again off the main thread; only the newest render shows */
    private void render() {
        final int gen = ++renderGen;
        final Bitmap src = image != null ? image : appIcon;
        final boolean fill = image != null;
        final int bg = colour, size = dp(176);
        final String sh = shape;
        final float z = zoom, x = dx, y = dy;
        if (src == null) return;
        worker.execute(() -> {
            Bitmap out = GlassArt.render(src, size, bg, sh, fill, z, x, y);
            runOnUiThread(() -> { if (alive && gen == renderGen) preview.setImageBitmap(out); });
        });
    }

    private void original() {
        image = null;
        own = true;
        zoom = 1f; dx = 0; dy = 0; colour = 0; shape = GlassArt.ROUNDED;
        render();
        Toast.makeText(this, S.t("الأيقونة الأصلية: اضغط «حفظ»"), Toast.LENGTH_SHORT).show();
    }

    private void save() {
        final Bitmap src = image != null ? image : appIcon;
        final boolean back = image == null && own;
        final boolean fill = image != null;
        final int bg = colour;
        final String sh = shape;
        final float z = zoom, x = dx, y = dy;
        worker.execute(() -> {
            boolean ok = true;
            if (back) IconStore.clear(this, pkg);
            else if (src != null) ok = IconStore.write(this, pkg, GlassArt.render(src, IconStore.SIZE, bg, sh, fill, z, x, y));
            final boolean done = ok;
            runOnUiThread(() -> {
                if (!alive) return;
                if (!done) { Toast.makeText(this, S.t("ما انحفظت الأيقونة"), Toast.LENGTH_SHORT).show(); return; }
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    // ---- the images: the TV's storage or a USB drive (jpg, png, webp), a strip of thumbnails

    private void pick(String where) {
        if (checkSelfPermission(readPermission()) != PackageManager.PERMISSION_GRANTED) {
            pendingSource = where;
            requestPermissions(new String[] {readPermission()}, ASK_READ);
            return;
        }
        List<File> roots = new ArrayList<>();
        if ("usb".equals(where)) roots.addAll(usbRoots());
        else {
            File ext = android.os.Environment.getExternalStorageDirectory();
            for (String d : new String[] {"Pictures", "DCIM", "Download", "Downloads"}) roots.add(new File(ext, d));
        }
        strip.removeAllViews();
        note("usb".equals(where) && roots.isEmpty() ? S.t("ما وجدنا فلاشة: ركّبها في التلفزيون وانتظر ثواني") : S.t("يبحث عن الصور…"));
        if (roots.isEmpty()) return;
        worker.execute(() -> {
            List<File> found = new ArrayList<>();
            for (File r : roots) walk(r, 0, found);
            runOnUiThread(() -> {
                if (!alive) return;
                if (found.isEmpty()) { note(S.t("لا صور هنا (jpg أو png أو webp)")); return; }
                note(null);
                for (File f : found) strip.addView(thumb(f), swatchCell());
                if (strip.getChildCount() > 0) strip.getChildAt(0).requestFocus();
            });
            for (int i = 0; i < found.size() && alive; i++) {   // the thumbnails, small, one after another
                final int at = i;
                final Bitmap t = decode(found.get(i), dp(72));
                runOnUiThread(() -> { if (alive && at < strip.getChildCount()) ((ImageView) ((FrameLayout) strip.getChildAt(at)).getChildAt(0)).setImageBitmap(t); });
            }
        });
    }

    static void walk(File dir, int depth, List<File> out) {
        if (out.size() >= MAX_IMAGES || depth > DEPTH || dir == null) return;
        File[] all = dir.listFiles();
        if (all == null) return;
        java.util.Arrays.sort(all, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));   // the newest first
        for (File f : all) {
            if (out.size() >= MAX_IMAGES) return;
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.startsWith(".")) continue;
            if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"))) out.add(f);
        }
        for (File f : all) if (f.isDirectory() && !f.getName().startsWith(".")) walk(f, depth + 1, out);
    }

    /** an image at about {@code max} on its short side (inSampleSize: never the whole photo in memory) */
    static Bitmap decode(File f, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int s = 1, side = Math.min(o.outWidth, o.outHeight);
            while (side / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            return BitmapFactory.decodeFile(f.getPath(), o);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    private View thumb(File f) {
        FrameLayout box = new FrameLayout(this);
        box.setFocusable(true);
        box.setClickable(true);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        box.setBackground(frame(dp(10)));
        box.setContentDescription(f.getName());
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(img, new FrameLayout.LayoutParams(dp(72), dp(72)));
        box.setOnClickListener(v -> worker.execute(() -> {
            Bitmap b = decode(f, 512);
            runOnUiThread(() -> {
                if (!alive) return;
                if (b == null) { Toast.makeText(this, S.t("ما قدرنا نقرأ الصورة"), Toast.LENGTH_SHORT).show(); return; }
                image = b; own = false; zoom = 1f; dx = 0; dy = 0;
                render();
            });
        }));
        return box;
    }

    private void note(String s) {
        stripNote.setVisibility(s == null ? View.GONE : View.VISIBLE);
        if (s != null) stripNote.setText(s);
    }

    private List<File> usbRoots() { return usbRoots(this); }

    static List<File> usbRoots(android.content.Context c) {
        List<File> out = new ArrayList<>();
        try {
            StorageManager sm = (StorageManager) c.getSystemService(STORAGE_SERVICE);
            for (StorageVolume v : sm.getStorageVolumes()) {
                if (!v.isRemovable() || !android.os.Environment.MEDIA_MOUNTED.equals(v.getState())) continue;
                File d = Build.VERSION.SDK_INT >= 30 ? v.getDirectory() : (v.getUuid() == null ? null : new File("/storage/" + v.getUuid()));
                if (d != null && d.isDirectory()) out.add(d);
            }
        } catch (RuntimeException ignored) { }
        return out;
    }

    private static String readPermission() {
        return Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_IMAGES" : android.Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != ASK_READ) return;
        if (checkSelfPermission(readPermission()) == PackageManager.PERMISSION_GRANTED && pendingSource != null) pick(pendingSource);
        else note(S.t("بدون إذن قراءة الصور ما نقدر نشوفها"));
    }

    // ---- the parts: a rectangle button, a colour, a heading

    private View swatch(int c) {
        FrameLayout box = new FrameLayout(this);
        box.setFocusable(true);
        box.setClickable(true);
        box.setPadding(dp(5), dp(5), dp(5), dp(5));
        box.setBackground(frame(dp(10)));
        box.setContentDescription(c == 0 ? S.t("تلقائي") : "#" + Integer.toHexString(c).substring(2).toUpperCase(Locale.ROOT));
        View fill;
        if (c == 0) {
            TextView t = label(11, look.text);
            t.setText(S.t("تلقائي"));
            t.setGravity(Gravity.CENTER);
            fill = t;
        } else fill = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(6));
        g.setColor(c == 0 ? 0x26FFFFFF : c);
        g.setStroke(Math.max(1, dp(1)), 0x4DFFFFFF);
        fill.setBackground(g);
        box.addView(fill, new FrameLayout.LayoutParams(dp(c == 0 ? 64 : 40), dp(40)));
        box.setOnClickListener(v -> { colour = c; render(); });
        return box;
    }

    private View button(String text, Runnable r) {
        TextView t = label(15, look.text);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        t.setPadding(dp(16), dp(10), dp(16), dp(10));
        t.setMinHeight(dp(44));
        GradientDrawable rest = new GradientDrawable(), on = new GradientDrawable();
        rest.setCornerRadius(dp(8));
        rest.setColor(0x24FFFFFF);
        rest.setStroke(Math.max(1, dp(1)), 0x40FFFFFF);
        on.setCornerRadius(dp(8));
        on.setColor(look.focus);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {}, rest);
        t.setBackground(s);
        t.setOnFocusChangeListener((v, has) -> t.setTextColor(has ? look.onFocus : look.text));
        t.setOnClickListener(v -> r.run());
        return t;
    }

    /** a light frame while focused (and while the preview moves the image: activated) */
    private StateListDrawable frame(int radius) {
        GradientDrawable on = new GradientDrawable(), act = new GradientDrawable();
        on.setCornerRadius(radius);
        on.setStroke(Math.max(2, dp(3)), 0xE6FFFFFF);
        act.setCornerRadius(radius);
        act.setStroke(Math.max(2, dp(3)), look.accent);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_activated}, act);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
        return s;
    }

    private TextView heading(String s) {
        TextView t = label(14, look.sub);
        t.setText(s);
        t.setPadding(0, dp(16), 0, dp(6));
        return t;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(Dir.layout());
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMarginEnd(dp(10));
        return p;
    }

    private LinearLayout.LayoutParams swatchCell() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMarginEnd(dp(6));
        return p;
    }

    private TextView label(float sp, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(medium);
        t.setTextColor(color);
        return t;
    }

    private Typeface font(String f) { try { return Typeface.createFromAsset(getAssets(), "fonts/" + f + ".otf"); } catch (RuntimeException e) { return null; } }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        alive = false;
        worker.shutdownNow();
        super.onDestroy();
    }
}
