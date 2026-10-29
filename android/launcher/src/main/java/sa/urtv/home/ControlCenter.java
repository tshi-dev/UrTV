package sa.urtv.home;

// Adapted from talkies (https://github.com/rajupp/talkies, MIT, (c) 2026 TechPP) HomeActivity.java (its quick-settings panel: free memory,
// network, speed test, sound output), see THIRD_PARTY_NOTICES.md. The panel itself is written for this home screen.

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * «مركز التحكم»: the panel the home screen's round pill opens: the sleep timer, turn the TV off now, the network, the speed
 * test, the sound output, screen mirroring (when the TV has a mirroring app), the screensaver, our own settings and the TV's settings, as big tiles you walk with the D-pad (OK acts, Back closes, the focus goes
 * back to the pill). Opening it is behind the family lock's «@center»; the tiles that open something keep their own locks (the sleep
 * timer's, the Wi-Fi's, the screensaver's, our settings' «@urtv», the TV's settings') through the host.
 *
 * Nothing runs until a tile is pressed: no network traffic (the speed test starts on its OK only and is cancelled when the panel closes)
 * and no background service. The logic that can be tested without Android is ControlText and SpeedTest.
 */
final class ControlCenter {
    /** What the panel asks of the home screen: its locks, its fonts, and the two popups it already has. */
    interface Host {
        void launchGuarded(int path, Intent i, String name);
        void openSaver();           // the screensaver pill's OK, locked by «@saver»
        void openSaverSettings();   // its long OK
        void openOurSettings();     // «إعدادات تلفزيونك»: the screensaver app's settings screen, locked by «@urtv»
        void openSleep(View anchor);   // «مؤقت النوم» popup, locked by «@sleep»
        boolean saverInstalled();
        Typeface font(String family);
    }

    // the glass look the home shows (GlassSource.look, when the panel opens): the panel opaque over the look's solid colour (it sits over the
    // home's photo as it is), the tiles a glass panel on it, the focused one the solid focus fill; GlassStyle checks each of them
    private int PANEL, LINE, FOCUSED, TILE, TEXT, SUB, ON, ON_SUB, ACCENT;

    private final Activity act;
    private final Host host;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final float density;
    private PopupWindow pw;
    private SpeedTest speed;      // the one running now, or null
    private String lastKey;       // the tile that had the focus last: the panel opens on it again while the home stays in front
    private TextView title;
    private boolean offPending;   // «أطفئ الآن» showed its notice and is about to act

    ControlCenter(Activity act, Host host) {
        this.act = act;
        this.host = host;
        density = act.getResources().getDisplayMetrics().density;
    }

    boolean showing() { return pw != null && pw.isShowing(); }

    void dismiss() { if (pw != null) pw.dismiss(); }

    /** The home screen is in front again (from an app, or Home pressed): the next opening starts on the first tile. */
    void forgetLast() { lastKey = null; }

    /** Opens the panel over the home screen; the focus returns to the anchor (the pill) when it closes. */
    void open(View anchor) {
        if (showing()) return;
        GlassStyle.Look l = GlassSource.look(act);   // the look of now, before any tile is made
        PANEL = GlassStyle.over(l.dialog, l.base); LINE = l.line; FOCUSED = l.focus; TILE = l.panel; TEXT = l.text; SUB = l.sub;
        ON = l.onFocus; ON_SUB = l.onFocusSub; ACCENT = l.accent;
        List<Tile> tiles = new ArrayList<>();

        tiles.add(tile("sleep", "مؤقت النوم", sleepLine(), R.drawable.ic_sleep, () -> { dismiss(); host.openSleep(anchor); }));
        Tile off = tile("off", "أطفئ الآن", SleepService.enabled() ? "تُطفأ الشاشة فورًا" : "الخدمة مطفأة: تظهر شاشة التوقف بدل الإطفاء. مطوّلًا: تفعيلها",
                R.drawable.ic_power, null);
        off.action = () -> turnOffNow();
        off.setOnLongClickListener(v -> {   // a long OK: where the sleep service is turned on, behind the same lock as the sleep timer's own line
            dismiss();
            host.launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "إعدادات التلفزيون");
            return true;
        });
        tiles.add(off);
        Tile net = tile("net", "الشبكة", "", R.drawable.ic_wifi, () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_WIFI, new Intent(Settings.ACTION_WIFI_SETTINGS), "واي فاي"); });
        networkLines(net);
        tiles.add(net);
        Tile test = tile("speed", "اختبار السرعة", "اضغط اختيار للبدء", R.drawable.ic_speed, null);
        test.action = () -> speedTest(test);
        tiles.add(test);
        tiles.add(tile("sound", "مخرج الصوت", outputLine(), R.drawable.ic_sound, () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, soundIntent(), "إعدادات الصوت"); }));
        // «انعكاس الشاشة»: the TV's own mirroring app (ControlText.MIRROR_APPS), only when one is installed; locked as that app's own tile is
        final Intent mirror = mirrorIntent();
        if (mirror != null)
            tiles.add(tile("mirror", ControlText.MIRROR, ControlText.MIRROR_HINT, R.drawable.ic_cast,
                    () -> { dismiss(); host.launchGuarded(ParentalPolicy.TILE, mirror, ControlText.MIRROR); }));
        if (host.saverInstalled()) {
            Tile saver = tile("saver", "شاشة التوقف", "مطوّلًا: الإعدادات", R.drawable.ic_screensaver, () -> { dismiss(); host.openSaver(); });
            saver.setOnLongClickListener(v -> { dismiss(); host.openSaverSettings(); return true; });
            tiles.add(saver);
            tiles.add(tile("ours", "إعدادات تلفزيونك", "الأذان والمفضلة والمظهر", R.drawable.ic_home, () -> { dismiss(); host.openOurSettings(); }));
        }
        tiles.add(tile("tv", "الإعدادات", "إعدادات التلفزيون", R.drawable.ic_settings,
                () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون"); }));

        LinearLayout panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);   // the home is built in code: the popup's own window has no direction either
        panel.setPadding(dp(18), dp(14), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(22));
        bg.setColor(PANEL);
        bg.setStroke(dp(1), LINE);
        panel.setBackground(bg);

        title = new TextView(act);
        title.setText(ControlText.NAME);
        title.setTextColor(TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(host.font("sans-serif-medium"));
        title.setGravity(Gravity.RIGHT);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));
        ((LinearLayout.LayoutParams) title.getLayoutParams()).bottomMargin = dp(12);

        // the tiles' size comes from the width AND the height of the screen (ControlLayout): every row shows on a 1080p or a 720p TV
        android.util.DisplayMetrics dm = act.getResources().getDisplayMetrics();
        ControlLayout.Plan plan = ControlLayout.plan(dm.widthPixels, dm.heightPixels, density, tiles.size());
        final int cols = plan.cols;
        for (Tile t : tiles) t.compact(plan.compact, plan.tiny);
        LinearLayout grid = new LinearLayout(act);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < plan.rows; r++) {
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols && r * cols + c < tiles.size(); c++) {
                Tile t = tiles.get(r * cols + c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(plan.tileW, plan.tileH);
                if (c > 0) lp.setMarginStart(dp(ControlLayout.GAP));
                row.addView(t, lp);
            }
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
            if (r > 0) rlp.topMargin = dp(ControlLayout.GAP);
            grid.addView(row, rlp);
        }
        // whatever still does not fit the screen scrolls with the focus (the focus walk below brings a tile into view)
        ScrollView scroller = new ScrollView(act);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setFillViewport(false);
        scroller.addView(grid, new ScrollView.LayoutParams(-2, -2));
        panel.addView(scroller, new LinearLayout.LayoutParams(-2, -2));
        // D-pad: right and left along a row (the row reads from the right: the first tile is the rightmost), up and down by row, no wrapping
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            int col = i % cols;
            t.setNextFocusRightId(col == 0 ? t.getId() : tiles.get(i - 1).getId());
            t.setNextFocusLeftId(col == cols - 1 || i == tiles.size() - 1 ? t.getId() : tiles.get(i + 1).getId());
            t.setNextFocusUpId(i < cols ? t.getId() : tiles.get(i - cols).getId());
            t.setNextFocusDownId(i + cols < tiles.size() ? tiles.get(i + cols).getId()
                    : i / cols < (tiles.size() - 1) / cols ? tiles.get(tiles.size() - 1).getId() : t.getId());
        }

        pw = new PopupWindow(panel, plan.panelW, Math.min(plan.panelH, ControlLayout.maxHeight(dm.heightPixels)), true);
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(24));
        pw.setOnDismissListener(() -> {
            if (speed != null) speed.cancel();   // nothing keeps running behind a closed panel
            speed = null;
            ui.removeCallbacksAndMessages(null);   // a notice that was about to turn the screen off does not run behind a closed panel
            title = null;
            offPending = false;
            pw = null;
            anchor.requestFocus();
        });
        pw.showAtLocation(anchor.getRootView(), Gravity.CENTER, 0, 0);
        // the first tile of the reading order (the rightmost of the top row: the panel is RTL), or the one used last while the home stays in front
        Tile start = tiles.get(0);
        for (Tile t : tiles) if (t.key.equals(lastKey)) start = t;
        final Tile focusFirst = start;
        focusFirst.requestFocus();
        panel.post(() -> { if (showing() && !focusFirst.hasFocus()) focusFirst.requestFocus(); });   // the window may take the focus after this call
    }

    // ---------------------------------------------------------------- tiles

    private Tile tile(String key, String title, String value, int icon, Runnable action) {
        Tile t = new Tile(act, key, title, value, icon);
        t.action = action;
        t.setOnClickListener(v -> { if (t.action != null) t.action.run(); });
        return t;
    }

    /** One tile: an icon, its name and a line under it (the state, or the result); white while it has the focus. */
    private final class Tile extends LinearLayout {
        Runnable action;
        final String key;
        private final TextView name, value;
        private final ImageView glyph;
        private final GradientDrawable fill = new GradientDrawable();

        Tile(Context c, String key, String title, String line, int icon) {
            super(c);
            this.key = key;
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setFocusable(true);
            setClickable(true);
            setId(View.generateViewId());
            fill.setCornerRadius(dp(14));
            fill.setColor(TILE);
            setBackground(fill);
            setPadding(dp(8), dp(8), dp(8), dp(8));
            glyph = new ImageView(c);
            glyph.setImageDrawable(c.getDrawable(icon).mutate());
            addView(glyph, new LayoutParams(dp(30), dp(30)));
            name = new TextView(c);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            name.setTypeface(host.font("sans-serif-medium"));
            name.setSingleLine(true);
            name.setGravity(Gravity.CENTER);
            name.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            addView(name, new LayoutParams(-1, -2));
            ((LayoutParams) name.getLayoutParams()).topMargin = dp(6);
            value = new TextView(c);
            value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            value.setTypeface(host.font("sans-serif"));
            value.setMaxLines(2);
            value.setEllipsize(android.text.TextUtils.TruncateAt.END);
            value.setGravity(Gravity.CENTER);
            value.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            addView(value, new LayoutParams(-1, -2));
            name.setText(title);
            value.setText(line);
            setContentDescription(title);
            paint();
        }

        /** the line as given: a network name may hold digits, so the numbers of the other lines go through Digits.of where they are made */
        void setValue(String line) { value.setText(line); }

        /** a small panel (ControlLayout.Plan.compact): a smaller icon and words, one line under the name (none when tiny) */
        void compact(boolean on, boolean tiny) {
            if (!on) return;
            if (tiny) value.setVisibility(View.GONE);   // below ControlLayout.MIN_H there is no room for it
            glyph.getLayoutParams().width = glyph.getLayoutParams().height = dp(24);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            value.setMaxLines(1);
            setPadding(dp(4), dp(4), dp(4), dp(4));
        }

        @Override protected void onFocusChanged(boolean gained, int direction, android.graphics.Rect previouslyFocusedRect) {
            super.onFocusChanged(gained, direction, previouslyFocusedRect);
            if (gained) lastKey = key;
            paint();
        }

        private void paint() {
            boolean f = hasFocus();
            fill.setColor(f ? FOCUSED : TILE);
            name.setTextColor(f ? ON : TEXT);
            value.setTextColor(f ? ON_SUB : SUB);
            glyph.getDrawable().setTint(f ? ON : TEXT);
        }
    }

    // ---------------------------------------------------------------- what the tiles say

    private String sleepLine() {
        int left = SleepTimer.minutesLeft(act);
        return Digits.of(left > 0 ? "يعمل: باقي " + left + " د" : "١٥ إلى ٩٠ دقيقة");
    }

    /** the network by name, its signal when it is Wi-Fi, as the TV reports them; read now, once (no polling) */
    private void networkLines(Tile t) {
        boolean wired = false, wifi = false;
        String ssid = null;
        int rssi = 0;
        try {
            android.net.ConnectivityManager cm = act.getSystemService(android.net.ConnectivityManager.class);
            android.net.NetworkCapabilities nc = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
            wired = nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET);
            wifi = nc != null && !wired && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
            if (wifi) {
                WifiManager wm = (WifiManager) act.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null && wm.getConnectionInfo() != null) {
                    ssid = wm.getConnectionInfo().getSSID();
                    rssi = wm.getConnectionInfo().getRssi();
                }
            }
        } catch (SecurityException ignored) { }
        t.setValue(ControlText.network(wired, wifi, ssid) + "\n" + Digits.of(ControlText.networkDetail(wired, wifi, rssi)));
    }

    /** the real output: what AudioManager lists as outputs, with the product name each reports (ControlText.outputs says it) */
    private String outputLine() {
        try {
            AudioManager am = (AudioManager) act.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] d = am == null ? new AudioDeviceInfo[0] : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            int[] types = new int[d.length];
            String[] products = new String[d.length];
            for (int i = 0; i < d.length; i++) {
                types[i] = d[i].getType();
                CharSequence p = d[i].getProductName();
                products[i] = p == null ? null : p.toString();
            }
            return ControlText.outputs(types, products);
        } catch (RuntimeException e) {
            return ControlText.outputs(new int[0], null);
        }
    }

    /** the first mirroring app this TV can open (ControlText.mirrorApp), as the intent that opens it; null when there is none */
    private Intent mirrorIntent() {
        android.content.pm.PackageManager pm = act.getPackageManager();
        String pkg = ControlText.mirrorApp(p -> launchIntent(pm, p) != null);
        return pkg == null ? null : launchIntent(pm, pkg);
    }

    /** the app's TV screen, else its phone one (a mirroring receiver often has only that); null when it is missing, disabled or has none */
    private static Intent launchIntent(android.content.pm.PackageManager pm, String pkg) {
        try {
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            return i != null ? i : pm.getLaunchIntentForPackage(pkg);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** the sound settings if this TV has them (the public action), else the TV's settings: never an intent that does not resolve */
    private Intent soundIntent() {
        Intent sound = new Intent(Settings.ACTION_SOUND_SETTINGS);
        return act.getPackageManager().resolveActivity(sound, 0) != null ? sound : new Intent(Settings.ACTION_SETTINGS);
    }

    // ---------------------------------------------------------------- «أطفئ الآن»

    /**
     * With the sleep service on the screen turns off at once. Without it the TV only stops the sound and shows the screensaver: the panel
     * says so first (and how to turn the service on), and does it after a moment, unless the panel was closed meanwhile.
     */
    private void turnOffNow() {
        if (SleepService.enabled()) { dismiss(); SleepTimer.turnOff(act); return; }
        if (title == null || offPending) return;
        offPending = true;
        title.setText(ControlText.SLEEP_OFF_NOTICE);
        title.setTextColor(ACCENT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setLineSpacing(0, 1.1f);
        ui.postDelayed(() -> {
            offPending = false;
            if (!showing()) return;
            dismiss();
            SleepTimer.turnOff(act);
        }, ControlText.NOTICE_MS);
    }

    // ---------------------------------------------------------------- «اختبار السرعة»

    /** OK starts a download of a few MB off the main thread; OK while it runs stops it. Closing the panel stops it too. */
    private void speedTest(Tile t) {
        if (speed != null) {
            speed.cancel();
            speed = null;
            t.setValue("أُلغي · اضغط اختيار للبدء");
            return;
        }
        final SpeedTest test = new SpeedTest();
        speed = test;
        t.setValue("جارٍ القياس…");
        new Thread(() -> {
            String said;
            try {
                said = Digits.of(ControlText.mbps(test.downloadMbps()));
            } catch (java.io.IOException | RuntimeException e) {
                said = test.cancelled() ? null : "تعذّر القياس: تأكد من الشبكة";
            }
            final String result = said;
            ui.post(() -> {
                if (speed != test || !showing()) return;   // cancelled, or the panel closed meanwhile
                speed = null;
                t.setValue(result + "\nاضغط اختيار للإعادة");
            });
        }, "speed-test").start();
    }

    private int dp(float v) { return Math.round(v * density); }
}
