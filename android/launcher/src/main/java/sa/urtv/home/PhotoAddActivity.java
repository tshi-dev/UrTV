package sa.urtv.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 4.4 beta 4 «أضف من فلاشة أو التنزيلات» for «صوري»: the photos on a USB stick, in the TV's Downloads, Pictures and DCIM, as a grid; OK on one adds it to «صوري»
 * through the screensaver app's own provider call (the one the phone's upload uses: a JPEG at screen size, with no camera data), so the app has no storage
 * permission of its own. The picture is read here, in the home, which already holds the read permission (the icon editor's). The screen stays open: add as many as
 * wanted, back leaves.
 */
public final class PhotoAddActivity extends Activity {
    private static final int ASK_READ = 43, SIDE = 160;
    private static final Uri SAVER = Uri.parse("content://sa.urtv.screensaver.config");

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<File> files = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView note;
    private boolean alive;

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }   // «اللغة»

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        S.init(this);
        alive = true;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#17181b"));
        root.setPadding(dp(48), dp(32), dp(48), dp(24));
        TextView title = new TextView(this);
        title.setText(S.t("أضف من فلاشة أو التنزيلات"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        root.addView(title);
        note = new TextView(this);
        note.setTextColor(Color.parseColor("#dadce0"));
        note.setTextSize(16);
        note.setPadding(0, dp(8), 0, dp(16));
        root.addView(note);
        GridView grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setHorizontalSpacing(dp(10));
        grid.setVerticalSpacing(dp(10));
        GradientDrawable sel = new GradientDrawable();   // the focus: a frame round the photo
        sel.setCornerRadius(dp(10));
        sel.setStroke(dp(4), Color.parseColor("#54c08a"));
        grid.setSelector(sel);
        adapter = new Thumbs();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, position, id) -> add(files.get(position)));
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        load();
    }

    @Override protected void onDestroy() {
        alive = false;
        worker.shutdownNow();
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static String readPermission() {
        return Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_IMAGES" : android.Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void load() {
        if (checkSelfPermission(readPermission()) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[] {readPermission()}, ASK_READ); return; }
        note.setText(S.t("يبحث عن الصور…"));
        worker.execute(() -> {
            List<File> found = new ArrayList<>();
            List<File> roots = new ArrayList<>(IconEditorActivity.usbRoots(this));
            File ext = android.os.Environment.getExternalStorageDirectory();
            for (String d : new String[] {"Download", "Downloads", "Pictures", "DCIM"}) roots.add(new File(ext, d));
            for (File r : roots) IconEditorActivity.walk(r, 0, found);
            runOnUiThread(() -> {
                if (!alive) return;
                files.clear();
                files.addAll(found);
                note.setText(found.isEmpty() ? S.t("لا صور هنا (jpg أو png أو webp). ركّب الفلاشة وانتظر ثواني ثم ارجع وافتحها") : S.t("اضغط «اختيار» على صورة لإضافتها إلى صوري"));
                adapter.notifyDataSetChanged();
            });
        });
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != ASK_READ) return;
        if (checkSelfPermission(readPermission()) == PackageManager.PERMISSION_GRANTED) load();
        else note.setText(S.t("بدون إذن قراءة الصور ما نقدر نشوف الفلاشة. تقدر تضيف من الجوال"));
    }

    /** the photo at screen size as a JPEG (a re-encoded picture has no camera data), handed to the screensaver app's own «صوري» store */
    private void add(File f) {
        Toast.makeText(this, S.t("يضيف «%s»…", f.getName()), Toast.LENGTH_SHORT).show();
        worker.execute(() -> {
            String msg;
            try {
                byte[] jpeg = screenJpeg(f, PhotoShrink.screenMax(this));
                if (jpeg == null) throw new IllegalStateException("unreadable");
                Bundle in = new Bundle();
                in.putByteArray("jpeg", jpeg);
                Bundle res = getContentResolver().call(SAVER, "phone_photo_add", null, in);
                if (res == null) msg = S.t("ثبّت أو حدّث تطبيق «شاشة تلفزيونك» أولاً");
                else if ("photo_added".equals(res.getString("result"))) msg = S.t("أضيفت إلى صوري ✓");
                else msg = "full".equals(res.getString("why")) ? S.t("صوري ممتلئة: احذف بعضها أولاً") : S.t("ما قدرنا نضيف هذه الصورة");
            } catch (RuntimeException | OutOfMemoryError e) {   // F89: a photo too big for this TV's memory is said, never a crash
                msg = S.t("ما قدرنا نقرأ الصورة");
            }
            final String shown = msg;
            runOnUiThread(() -> { if (alive) Toast.makeText(this, shown, Toast.LENGTH_SHORT).show(); });
        });
    }

    /**
     * The photo as a JPEG for «صوري»: sub-sampled while its long side stays at least {@code maxLong} (a 12 MP photo is never decoded whole), scaled to exactly that long side,
     * and saved at the best quality that fits one provider call (PhotoSize.MAX_JPEG_BYTES; Binder carries about 1 MB). Null when it cannot be read.
     */
    static byte[] screenJpeg(File f, int maxLong) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = PhotoSize.sample(bounds.outWidth, bounds.outHeight, maxLong);
        Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
        if (b == null) return null;
        b = PhotoTurn.upright(f, b);   // review F98: a portrait photo is stored sideways with a tag
        int[] to = PhotoSize.fit(b.getWidth(), b.getHeight(), maxLong);
        if (to[0] != b.getWidth() || to[1] != b.getHeight()) {
            Bitmap scaled = Bitmap.createScaledBitmap(b, to[0], to[1], true);
            if (scaled != b) b.recycle();
            b = scaled;
        }
        byte[] best = null;
        for (int q : PhotoSize.QUALITIES) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, q, out);
            best = out.toByteArray();
            if (best.length <= PhotoSize.MAX_JPEG_BYTES) break;
        }
        b.recycle();
        return best;
    }

    private final class Thumbs extends BaseAdapter {
        @Override public int getCount() { return files.size(); }
        @Override public Object getItem(int position) { return files.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convert, ViewGroup parent) {
            FrameLayout box = convert instanceof FrameLayout ? (FrameLayout) convert : new FrameLayout(PhotoAddActivity.this);
            ImageView img;
            if (box.getChildCount() == 0) {
                box.setPadding(dp(4), dp(4), dp(4), dp(4));
                GradientDrawable g = new GradientDrawable();
                g.setCornerRadius(dp(10));
                g.setStroke(dp(2), Color.parseColor("#33ffffff"));
                box.setBackground(g);
                img = new ImageView(PhotoAddActivity.this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                box.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(SIDE), Gravity.CENTER));
            } else img = (ImageView) box.getChildAt(0);
            final File f = files.get(position);
            box.setContentDescription(f.getName());
            img.setImageBitmap(null);
            img.setTag(f);
            worker.execute(() -> {
                final Bitmap t = IconEditorActivity.decode(f, dp(SIDE));
                runOnUiThread(() -> { if (alive && img.getTag() == f) img.setImageBitmap(t); });
            });
            return box;
        }
    }
}
