package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 4.5 «زجاجي»: «كل التطبيقات» as a frosted-glass window over the home (docs/mockups/glass/glass-all-apps.png): its count, a search field (a
 * rectangle with small corners) and every app in five columns, the focused one in a light frame. The list is the home's (Dock.catalogue,
 * kids mode's allowed apps only, the names the owner gave); every icon is read and drawn off the main thread. OK on an app gives it back to
 * the home, which opens it through its own tile (the family lock asks there, as for any app). Back closes the window.
 */
public final class AllAppsActivity extends Activity {
    static final String PKG = "pkg", LOOK = "look", LIGHT = "light";
    private boolean light;
    static final int COLS = 5;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Item> all = new ArrayList<>(), shown = new ArrayList<>();
    private Adapter adapter;
    private TextView count;
    private GridView grid;
    private GlassStyle.Look look;
    private Typeface medium;
    private volatile boolean alive = true;

    static final class Item {
        String pkg, label, folded;
        ResolveInfo ri;
        volatile android.graphics.drawable.Drawable icon;
    }

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        medium = font("Thmanyah-Medium");
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
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

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(Dir.layout());
        int pad = dp(28);
        panel.setPadding(pad, dp(24), pad, dp(12));
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(dp(28));
        glass.setColor(light ? 0x8CFFFFFF : 0x2EFFFFFF);
        glass.setStroke(Math.max(1, dp(1)), 0x4DFFFFFF);
        panel.setBackground(glass);
        root.addView(panel, new FrameLayout.LayoutParams(Math.round(dm.widthPixels * 0.78f), Math.round(dm.heightPixels * 0.86f), Gravity.CENTER));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(22, look.text);
        title.setText(S.t("كل التطبيقات"));
        count = label(15, look.sub);
        head.addView(title, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.setMarginStart(dp(12));
        head.addView(count, cp);
        panel.addView(head, new LinearLayout.LayoutParams(-1, -2));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint(S.t("ابحث عن تطبيق"));
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        search.setTypeface(medium);
        search.setTextColor(look.text);
        search.setHintTextColor(look.sub);
        search.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        search.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        search.setBackground(field());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(14);
        sp.bottomMargin = dp(14);
        panel.addView(search, sp);

        grid = new GridView(this);
        grid.setNumColumns(COLS);
        grid.setLayoutDirection(Dir.layout());
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(10));
        grid.setHorizontalSpacing(dp(10));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));   // the item draws its own frame
        grid.setDrawSelectorOnTop(false);
        grid.setClipToPadding(false);
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        grid.setVerticalScrollBarEnabled(false);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            setResult(RESULT_OK, new Intent().putExtra(PKG, shown.get(pos).pkg));
            finish();
        });
        panel.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        // the keyboard only when the person goes to the search (the emulator: it opened with the window, and Back closed only it)
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        search.setFocusable(true);
        search.setFocusableInTouchMode(false);
        load();
    }

    /** the list off the main thread, the names first (it shows at once), then the icons a few at a time */
    private void load() {
        worker.execute(() -> {
            PackageManager pm = getPackageManager();
            Map<String, String> given = AppNames.given(this);
            ParentalPolicy.State family = Parental.state(this);
            List<Item> got = new ArrayList<>();
            for (ResolveInfo ri : Dock.catalogue(this)) {
                if (!ParentalPolicy.visible(family, ri.activityInfo.packageName)) continue;
                Item it = new Item();
                it.pkg = ri.activityInfo.packageName;
                it.ri = ri;
                it.label = AppNames.label(this, ri, given);
                it.folded = it.label.toLowerCase(Locale.ROOT);
                got.add(it);
            }
            Collections.sort(got, (x, y) -> x.label.compareToIgnoreCase(y.label));
            runOnUiThread(() -> {
                if (!alive) return;
                all.clear();
                all.addAll(got);
                filter(null);
                grid.post(() -> { if (alive && !shown.isEmpty()) { grid.setSelection(0); grid.requestFocus(); } });   // the first app has the focus once there is one (an empty grid could not take it)
            });
            int size = FavBar.glassCell(getResources().getDisplayMetrics().heightPixels, 1.25f);
            for (int i = 0; i < got.size() && alive; i++) {
                Item it = got.get(i);
                Bitmap own = IconStore.read(this, it.pkg);
                try { it.icon = new android.graphics.drawable.BitmapDrawable(getResources(), own != null ? own : GlassArt.appIcon(it.ri.loadIcon(pm), size)); }
                catch (RuntimeException | OutOfMemoryError e) { it.icon = null; }
                if (i % 10 == 9 || i == got.size() - 1) runOnUiThread(() -> { if (alive) adapter.notifyDataSetChanged(); });
            }
        });
    }

    private String query = "";

    /** the apps whose name has the words typed (any case), the count of them */
    private void filter(String q) {
        if (q != null) query = q.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Item it : all) if (query.isEmpty() || it.folded.contains(query) || it.pkg.contains(query)) shown.add(it);
        count.setText(Digits.of(String.valueOf(shown.size())));
        adapter.notifyDataSetChanged();
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int i, View v, ViewGroup parent) {
            LinearLayout cell = (LinearLayout) v;
            if (cell == null) {
                cell = new LinearLayout(AllAppsActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(8), dp(12), dp(8), dp(10));
                cell.setBackground(frame());
                ImageView img = new ImageView(AllAppsActivity.this);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                cell.addView(img, new LinearLayout.LayoutParams(dp(64), dp(64)));
                TextView name = label(13, look.text);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                name.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
                np.topMargin = dp(8);
                cell.addView(name, np);
            }
            Item it = shown.get(i);
            ((ImageView) cell.getChildAt(0)).setImageDrawable(it.icon);
            ((TextView) cell.getChildAt(1)).setText(it.label);
            cell.setContentDescription(it.label);
            return cell;
        }
    }

    /** the focused app: a light frame (no fill change of the icon), nothing at rest */
    private StateListDrawable frame() {
        GradientDrawable on = new GradientDrawable();
        on.setCornerRadius(dp(16));
        on.setColor(0x26FFFFFF);
        on.setStroke(Math.max(2, dp(2)), 0xE6FFFFFF);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_selected}, on);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {android.R.attr.state_pressed}, on);
        s.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
        return s;
    }

    /** the search field: a rectangle with small corners, a light line, brighter while focused */
    private StateListDrawable field() {
        GradientDrawable rest = new GradientDrawable(), on = new GradientDrawable();
        for (GradientDrawable g : new GradientDrawable[] {rest, on}) { g.setCornerRadius(dp(8)); g.setColor(0x1FFFFFFF); }
        rest.setStroke(Math.max(1, dp(1)), 0x40FFFFFF);
        on.setStroke(Math.max(2, dp(2)), 0xE6FFFFFF);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {}, rest);
        return s;
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
