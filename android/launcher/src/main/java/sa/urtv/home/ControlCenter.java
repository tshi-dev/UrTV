package sa.urtv.home;

// Adapted from talkies (https://github.com/rajupp/talkies, MIT, (c) 2026 TechPP) HomeActivity.java (its quick-settings panel: free memory,
// network, speed test, sound output), see THIRD_PARTY_NOTICES.md. The panel itself is written for this home screen.

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * «مركز التحكم»: the panel the home screen's round pill opens, a glass side panel at the start edge of the screen (inside the 5 % safe area,
 * 560 px wide on a 1080p TV, sliding in for 220 ms): four round toggles on top (Bluetooth, the screen mirroring app, the sleep timer,
 * «أطفئ الآن»), then rows with a short value each (the network, the sound output, the speed test, the TV's settings, ours), walked with
 * the D-pad: OK acts, Back closes, the focus goes back to the pill. «تخصيص» (the first line's small button) is a second view of the
 * same panel: a tick for what shows, the arrows (Left and Right) to move a row up or down, at most six rows, «إعادة الضبط» for the
 * defaults; the choice is kept on this TV. Opening it is behind the family lock's «@center»; whatever opens something keeps its own lock (the sleep timer's,
 * the Wi-Fi's, the screensaver's, our settings' «@urtv», the TV's settings') through the host.
 *
 * Nothing runs until a row is pressed: no network traffic (the speed test starts on its OK only and is cancelled when the panel closes)
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

    /** the panel: 560 px of 1920 wide, inside the 5 % safe area (x from 96, y from 54 to 1026 at 1080p) */
    static final float PANEL_W = 560f / 1920f, SAFE = 0.05f;
    /** the slide-in, and the focus fill coming in (ms) */
    static final int SLIDE_MS = 220, FOCUS_MS = 170;
    /** the most rows the panel shows; the four round toggles are not counted */
    static final int MAX_ROWS = 6;
    /** the round toggles, in this order; every other item is a row */
    static final String[] TOGGLES = {"bluetooth", "mirror", "sleep", "off"};
    /** the rows' order before the person moves any; «شاشة التوقف» is unticked until they ask for it (its own pill is on the home) */
    static final String[] ROWS = {"net", "sound", "speed", "tv", "ours", "saver"};
    static final String[] DEFAULT_OFF = {"saver"};

    // the glass look the home shows (GlassSource.look, when the panel opens): the panel opaque over the look's solid colour (it sits over the
    // home's photo as it is), the rows a glass panel on it, the focused one the solid focus fill; GlassStyle checks each of them
    private int PANEL, LINE, FOCUSED, TILE, TEXT, SUB, ON, ON_SUB, ACCENT, RING;

    private final Activity act;
    private final Host host;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final float density;
    private PopupWindow pw;
    private SpeedTest speed;      // the one running now, or null
    private String lastKey;       // the item that had the focus last: the panel opens on it again while the home stays in front
    private TextView title;
    private View anchorView;
    private final List<Item> items = new ArrayList<>();   // everything this TV can show, in the default order
    private FrameLayout body;     // what the panel shows now: the main view or «تخصيص»
    private boolean customising;

    ControlCenter(Activity act, Host host) {
        this.act = act;
        this.host = host;
        density = act.getResources().getDisplayMetrics().density;
    }

    boolean showing() { return pw != null && pw.isShowing(); }

    private boolean closeNow, backFromCustom;
    private String customKey;   // the row just changed in «تخصيص»: where its list starts; null = the top of the list
    void dismiss() { if (pw != null) { closeNow = true; try { pw.dismiss(); } finally { closeNow = false; } } }

    /** The home screen is in front again (from an app, or Home pressed): the next opening starts on the first item. */
    void forgetLast() {
        lastKey = null;
        soundReturned();
    }

    /**
     * TCL Smart TV G07 (Android 11): the TV's settings crash opening the sound screen (its SoundActivity class is missing in that build) and the home
     * comes straight back. A return within SOUND_FAIL_MS of «مخرج الصوت» opening it is read as that: from then on the row opens the TV's general
     * settings, which every TV has, and never the sound screen again (no crash loop). The mark is kept on this TV.
     */
    static final long SOUND_FAIL_MS = 3500;

    private void soundReturned() {
        long t = prefs().getLong("sound_t", 0);
        if (t == 0) return;
        SharedPreferences.Editor e = prefs().edit().remove("sound_t");
        if (System.currentTimeMillis() - t < SOUND_FAIL_MS) e.putBoolean("sound_bad", true);
        e.apply();
    }

    /** one thing the panel can show: a round toggle or a row */
    private static final class Item {
        final String key, name;
        String value;
        final int icon;
        Runnable action, hold;
        TextView valueView;   // while it is on the screen
        Item(String key, String name, String value, int icon) { this.key = key; this.name = name; this.value = value; this.icon = icon; }
        boolean toggle() { return Arrays.asList(TOGGLES).contains(key); }
        void setValue(String v) { value = v; if (valueView != null) valueView.setText(v); }
    }

    /** Opens the panel over the home screen; the focus returns to the anchor (the pill) when it closes. */
    void open(View anchor) { open(anchor, null); }

    /** …starting on an item (its key) or on «تخصيص» ("custom"): for the debug pictures; null is the panel as the person finds it */
    void open(View anchor, String start) {
        if (showing()) return;
        GlassStyle.Look l = GlassSource.look(act);   // the look of now, before any row is made
        PANEL = GlassStyle.over(l.dialog, l.base); LINE = l.line; FOCUSED = l.focus; TILE = l.panel; TEXT = l.text; SUB = l.sub;
        ON = l.onFocus; ON_SUB = l.onFocusSub; ACCENT = l.accent; RING = l.ring;
        anchorView = anchor;
        customising = false;
        items.clear();

        // the catalogue (docs/UX-REVIEW.md): Wi-Fi and Bluetooth first, then the sound, the speed test, the sleep timer, «أطفئ الآن», mirroring
        Item net = tile("net", S.t("الشبكة"), networkLine(), R.drawable.ic_wifi, () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_WIFI, new Intent(Settings.ACTION_WIFI_SETTINGS), "واي فاي"); });
        // «البلوتوث»: the TV's remotes-and-accessories screen, only when an action for it resolves; locked as the TV's settings are
        final Intent bluetooth = bluetoothIntent();
        if (bluetooth != null)
            tile("bluetooth", ControlText.BLUETOOTH, ControlText.BLUETOOTH_HINT, R.drawable.ic_bluetooth,
                    () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, bluetooth, ControlText.BLUETOOTH); });
        tile("sound", S.t("مخرج الصوت"), outputLine(), R.drawable.ic_sound, () -> {
            dismiss();
            try {
                Intent sound = soundIntent();
                if (Settings.ACTION_SOUND_SETTINGS.equals(sound.getAction())) prefs().edit().putLong("sound_t", System.currentTimeMillis()).apply();   // soundReturned reads it
                host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, sound, S.t("إعدادات الصوت"));
            } catch (RuntimeException e) {   // A6: the TCL G07 crashed here: the tile must never take the home down; the log says what it was
                android.util.Log.w("UrTVControl", "sound tile failed " + e.getClass().getSimpleName());
            }
        });
        Item test = tile("speed", S.t("اختبار السرعة"), S.t("اضغط اختيار للبدء"), R.drawable.ic_speed, null);
        test.action = () -> speedTest(test);
        tile("sleep", S.t("مؤقت النوم"), sleepLine(), R.drawable.ic_sleep, () -> { dismiss(); host.openSleep(anchor); });
        Item off = tile("off", S.t("أطفئ الآن"), SleepService.enabled() ? S.t("تُطفأ الشاشة فورًا") : S.t("يحتاج تفعيل، اضغط للتفعيل"),
                R.drawable.ic_power, null);
        off.action = () -> turnOffNow();
        off.hold = () -> {   // a long OK: where the sleep service is turned on, behind the same lock as the sleep timer's own line
            dismiss();
            host.launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "إعدادات التلفزيون");
        };
        // «انعكاس الشاشة»: the TV's own mirroring app (ControlText.MIRROR_APPS), only when one is installed; locked as that app's own tile is
        final Intent mirror = mirrorIntent();
        if (mirror != null)
            tile("mirror", ControlText.MIRROR, ControlText.MIRROR_HINT, R.drawable.ic_cast,
                    () -> { dismiss(); host.launchGuarded(ParentalPolicy.TILE, mirror, ControlText.MIRROR); });
        if (host.saverInstalled()) {
            Item saver = tile("saver", S.t("شاشة التوقف"), S.t("ابدأها الآن"), R.drawable.ic_screensaver, () -> { dismiss(); host.openSaver(); });
            saver.hold = () -> { dismiss(); host.openSaverSettings(); };   // a shortcut: its settings are in «إعدادات تلفزيونك» ← البطاقات
            tile("ours", S.t("إعدادات تلفزيونك"), S.t("الأذان والمفضلة والمظهر"), R.drawable.ic_home, () -> { dismiss(); host.openOurSettings(); });
        }
        tile("tv", S.t("الإعدادات"), S.t("إعدادات التلفزيون"), R.drawable.ic_settings,
                () -> { dismiss(); host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون"); });

        android.util.DisplayMetrics dm = act.getResources().getDisplayMetrics();
        final int W = dm.widthPixels, H = dm.heightPixels;
        final int margin = Math.round(W * SAFE), top = Math.round(H * SAFE);
        final int panelW = Math.round(W * PANEL_W), panelH = H - 2 * top;
        final boolean rtl = Dir.rtl();

        // the popup is the whole screen, clear: the panel slides across it from its own edge, a press outside closes it
        FrameLayout root = new FrameLayout(act);
        root.setLayoutDirection(Dir.layout());   // the home is built in code: the popup's own window has no direction either
        root.setOnClickListener(v -> dismiss());
        LinearLayout panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);   // a press on the panel itself is not a press outside it
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(PANEL);
        bg.setStroke(dp(1), LINE);
        panel.setBackground(bg);
        body = new FrameLayout(act);
        panel.addView(body, new LinearLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(panelW, panelH, Dir.start() | Gravity.TOP);
        plp.setMarginStart(margin);
        plp.topMargin = top;
        root.addView(panel, plp);

        // The popup's own decor view takes Back before any view of ours does (it starts tracking on the press and dismisses on the release), so a key listener on a cell
        // never sees it: «تخصيص» is left by overriding dismiss(): Back there goes back to the panel's list (focus on «تخصيص»), every other close is a real one (closeNow)
        pw = new PopupWindow(root, W, H, true) {
            @Override public void dismiss() {
                if (customising && !closeNow && body != null) { backFromCustom = true; showMain(); return; }
                super.dismiss();
            }
        };
        pw.setElevation(dp(24));
        pw.setOnDismissListener(() -> {
            if (speed != null) speed.cancel();   // nothing keeps running behind a closed panel
            speed = null;
            ui.removeCallbacksAndMessages(null);
            title = null;
            body = null;
            for (Item it : items) it.valueView = null;
            pw = null;
            anchor.requestFocus();
        });
        if ("custom".equals(start)) { customKey = null; showCustom(); }
        else {
            if (start != null) lastKey = start;
            showMain();
        }
        // the slide: from beyond its own edge to its place in 220 ms
        panel.setTranslationX((rtl ? 1 : -1) * (panelW + margin));
        pw.showAtLocation(anchor.getRootView(), Gravity.NO_GRAVITY, 0, 0);   // the whole screen, from its corner
        panel.post(() -> panel.animate().translationX(0).setDuration(SLIDE_MS).setInterpolator(new DecelerateInterpolator()).start());
    }

    // ---------------------------------------------------------------- what is kept on this TV

    private SharedPreferences prefs() { return act.getSharedPreferences("control", Context.MODE_PRIVATE); }

    private Set<String> unticked() {
        String v = prefs().getString("off", null);
        return new HashSet<>(Arrays.asList(v == null ? DEFAULT_OFF : v.isEmpty() ? new String[0] : v.split(",")));
    }

    /** the rows this TV can show, in the person's order (the default order for any they have not placed) */
    private List<Item> rowsInOrder() {
        List<String> order = new ArrayList<>();
        String v = prefs().getString("rows", "");
        if (!v.isEmpty()) order.addAll(Arrays.asList(v.split(",")));
        for (String k : ROWS) if (!order.contains(k)) order.add(k);
        List<Item> out = new ArrayList<>();
        for (String k : order) { Item it = find(k); if (it != null) out.add(it); }
        return out;
    }

    private List<Item> togglesPresent() {
        List<Item> out = new ArrayList<>();
        for (String k : TOGGLES) { Item it = find(k); if (it != null) out.add(it); }
        return out;
    }

    private Item find(String key) { for (Item it : items) if (it.key.equals(key)) return it; return null; }

    private int tickedRows(Set<String> off) {
        int n = 0;
        for (Item it : rowsInOrder()) if (!off.contains(it.key)) n++;
        return n;
    }

    private void save(List<Item> rows, Set<String> off) {
        StringBuilder r = new StringBuilder(), o = new StringBuilder();
        for (Item it : rows) r.append(r.length() > 0 ? "," : "").append(it.key);
        for (String k : off) o.append(o.length() > 0 ? "," : "").append(k);
        prefs().edit().putString("rows", r.toString()).putString("off", o.toString()).apply();
    }

    private void reset() { prefs().edit().remove("rows").remove("off").apply(); }

    // ---------------------------------------------------------------- the main view

    private Item tile(String key, String title, String value, int icon, Runnable action) {
        Item it = new Item(key, S.tv(title), S.tv(value), icon);   // a name or line given as our Arabic words (ControlText's) is said in «اللغة»; one already said stays
        it.action = action;
        items.add(it);
        return it;
    }

    private void showMain() {
        customising = false;
        body.removeAllViews();
        for (Item it : items) it.valueView = null;
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        body.addView(col, new FrameLayout.LayoutParams(-1, -1));
        List<View> focusables = new ArrayList<>();
        Set<String> off = unticked();

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        title = text(S.tv(ControlText.NAME), 22, TEXT, "sans-serif-medium");
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Cell custom = new Cell(Cell.PILL, null);
        custom.label(S.t("تخصيص"), ACCENT);
        custom.setOnClickListener(v -> { customKey = null; showCustom(); });
        head.addView(custom, new LinearLayout.LayoutParams(-2, dp(32)));
        head.setMinimumHeight(dp(48));
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));
        focusables.add(custom);

        // the round toggles: four across, each its icon in a circle and its name under it
        List<Item> toggles = new ArrayList<>();
        for (Item it : togglesPresent()) if (!off.contains(it.key)) toggles.add(it);
        List<Cell> toggleCells = new ArrayList<>();
        if (!toggles.isEmpty()) {
            LinearLayout strip = new LinearLayout(act);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            for (Item it : toggles) {
                Cell c = new Cell(Cell.TOGGLE, it);
                strip.addView(c, new LinearLayout.LayoutParams(0, dp(76), 1f));
                focusables.add(c);
                toggleCells.add(c);
            }
            col.addView(strip, new LinearLayout.LayoutParams(-1, -2));
            View line = new View(act);
            line.setBackgroundColor(LINE);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
            llp.topMargin = dp(8);
            llp.bottomMargin = dp(8);
            col.addView(line, llp);
        }
        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Cell> rowCells = new ArrayList<>();
        int shown = 0;
        for (Item it : rowsInOrder()) {
            if (off.contains(it.key) || shown >= MAX_ROWS) continue;
            shown++;
            Cell c = new Cell(Cell.ROW, it);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
            if (!rowCells.isEmpty()) lp.topMargin = dp(4);
            list.addView(c, lp);
            focusables.add(c);
            rowCells.add(c);
        }
        ScrollView scroller = new ScrollView(act);   // a short screen scrolls what does not fit, with the focus
        scroller.setVerticalScrollBarEnabled(false);
        scroller.addView(list, new ScrollView.LayoutParams(-1, -2));
        col.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1f));

        // D-pad: the toggles walk along their strip (the first is the start one: the right one in Arabic), up and down by the lines, no wrapping
        for (int i = 0; i < toggleCells.size(); i++) {
            Cell c = toggleCells.get(i);
            int before = i == 0 ? c.getId() : toggleCells.get(i - 1).getId(), after = i == toggleCells.size() - 1 ? c.getId() : toggleCells.get(i + 1).getId();
            c.setNextFocusRightId(Dir.rtl() ? before : after);
            c.setNextFocusLeftId(Dir.rtl() ? after : before);
            c.setNextFocusUpId(custom.getId());
            c.setNextFocusDownId(rowCells.isEmpty() ? c.getId() : rowCells.get(0).getId());
        }
        custom.setNextFocusUpId(custom.getId());
        custom.setNextFocusDownId(!toggleCells.isEmpty() ? toggleCells.get(0).getId() : rowCells.isEmpty() ? custom.getId() : rowCells.get(0).getId());
        for (int i = 0; i < rowCells.size(); i++) {
            Cell c = rowCells.get(i);
            c.setNextFocusLeftId(c.getId());
            c.setNextFocusRightId(c.getId());
            c.setNextFocusUpId(i == 0 ? (toggleCells.isEmpty() ? custom.getId() : toggleCells.get(0).getId()) : rowCells.get(i - 1).getId());
            c.setNextFocusDownId(i == rowCells.size() - 1 ? c.getId() : rowCells.get(i + 1).getId());
        }
        if (backFromCustom) {   // Back from «تخصيص»: the focus is on the button that opened it
            backFromCustom = false;
            final View fc = custom;
            fc.requestFocus();
            body.post(() -> { if (showing() && !fc.hasFocus()) fc.requestFocus(); });
        } else focusStart(focusables);
    }

    // ---------------------------------------------------------------- «تخصيص»

    private void showCustom() {
        customising = true;
        body.removeAllViews();
        for (Item it : items) it.valueView = null;
        title = null;
        final Set<String> off = unticked();
        final List<Item> rows = rowsInOrder();
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        body.addView(col, new FrameLayout.LayoutParams(-1, -1));
        List<View> focusables = new ArrayList<>();

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Cell back = new Cell(Cell.BACK, null);
        back.setOnClickListener(v -> showMain());
        head.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        focusables.add(back);
        TextView t = text(S.t("تخصيص"), 22, TEXT, "sans-serif-medium");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tlp.setMarginStart(dp(8));
        head.addView(t, tlp);
        TextView count = text(Digits.of(S.t("%s من %s", tickedRows(off), MAX_ROWS)), 14, SUB, "sans-serif");
        head.addView(count, new LinearLayout.LayoutParams(-2, -2));
        col.addView(head, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView hint = text(S.t("اختر ما يظهر ورتّبه بالأسهم"), 13, SUB, "sans-serif");
        hint.setMaxLines(2);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.bottomMargin = dp(8);
        col.addView(hint, hlp);

        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Cell> cells = new ArrayList<>();
        List<Item> all = new ArrayList<>(rows);
        all.addAll(togglesPresent());
        for (int i = 0; i < all.size(); i++) {
            final Item it = all.get(i);
            final int at = i;
            Cell c = new Cell(Cell.CHECK, it);
            c.ticked = !off.contains(it.key);
            c.paint(false);
            c.setOnClickListener(v -> {
                Set<String> now = unticked();
                if (now.contains(it.key)) {
                    if (!it.toggle() && tickedRows(now) >= MAX_ROWS) return;   // the sixth row is the last
                    now.remove(it.key);
                } else now.add(it.key);
                save(rowsInOrder(), now);
                customKey = it.key;
                showCustom();
            });
            c.setOnKeyListener((v, code, e) -> {
                if (code == KeyEvent.KEYCODE_BACK) { if (e.getAction() == KeyEvent.ACTION_UP) showMain(); return true; }
                boolean up = code == (Dir.rtl() ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT), down = code == (Dir.rtl() ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                if (!up && !down) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN && !it.toggle()) {   // the arrows move a row; a toggle keeps its place
                    List<Item> now = rowsInOrder();
                    int p = now.indexOf(it), q = p + (up ? -1 : 1);
                    if (q >= 0 && q < now.size()) {
                        now.remove(p);
                        now.add(q, it);
                        save(now, unticked());
                        customKey = it.key;
                        showCustom();
                    }
                }
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
            if (i > 0) lp.topMargin = dp(4);
            list.addView(c, lp);
            cells.add(c);
            focusables.add(c);
        }
        ScrollView scroller = new ScrollView(act);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.addView(list, new ScrollView.LayoutParams(-1, -2));
        col.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout foot = new LinearLayout(act);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        Cell done = new Cell(Cell.PRIMARY, null);
        done.label(S.t("تم"), 0);
        done.setOnClickListener(v -> showMain());
        Cell reset = new Cell(Cell.PILL, null);
        reset.label(S.t("إعادة الضبط"), TEXT);
        reset.setOnClickListener(v -> { reset(); lastKey = null; customKey = null; showCustom(); });
        // «تم» first (the start side), «إعادة الضبط» after it, 16 px apart
        foot.addView(done, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, dp(48));
        rlp.setMarginStart(dp(12));
        foot.addView(reset, rlp);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = dp(12);
        col.addView(foot, flp);
        focusables.add(done);
        focusables.add(reset);

        // up and down by the lines: back, the list, the footer; no wrapping; left and right stay where they are (the arrows move a row)
        List<View> walk = new ArrayList<>();
        walk.add(back);
        walk.addAll(cells);
        walk.add(done);
        for (int i = 0; i < walk.size(); i++) {
            View c = walk.get(i);
            c.setNextFocusUpId(i == 0 ? c.getId() : walk.get(i - 1).getId());
            c.setNextFocusDownId(i == walk.size() - 1 ? c.getId() : walk.get(i + 1).getId());
            if (c != done) { c.setNextFocusLeftId(c.getId()); c.setNextFocusRightId(c.getId()); }
        }
        done.setNextFocusLeftId(Dir.rtl() ? reset.getId() : done.getId());
        done.setNextFocusRightId(Dir.rtl() ? done.getId() : reset.getId());
        reset.setNextFocusLeftId(Dir.rtl() ? reset.getId() : done.getId());
        reset.setNextFocusRightId(Dir.rtl() ? done.getId() : reset.getId());
        reset.setNextFocusUpId(cells.isEmpty() ? back.getId() : cells.get(cells.size() - 1).getId());
        reset.setNextFocusDownId(reset.getId());
        // Back from any button here goes back to the main view, not out of the panel
        for (View v : new View[] {done, reset}) v.setOnKeyListener((vv, code, e) -> {
            if (code != KeyEvent.KEYCODE_BACK) return false;
            if (e.getAction() == KeyEvent.ACTION_UP) showMain();
            return true;
        });
        back.setOnKeyListener((vv, code, e) -> {
            if (code != KeyEvent.KEYCODE_BACK) return false;
            if (e.getAction() == KeyEvent.ACTION_UP) showMain();
            return true;
        });
        // the focus: the row just changed, else the first line of the list
        View start = null;
        for (Cell c : cells) if (c.item != null && c.item.key.equals(customKey)) start = c;
        if (start == null) start = cells.isEmpty() ? back : cells.get(0);
        start.requestFocus();
        if (start == (cells.isEmpty() ? back : cells.get(0))) scroller.post(() -> scroller.scrollTo(0, 0));   // opened at the top: the first row whole, not half hidden
        final View fs = start;
        body.post(() -> { if (showing() && !fs.hasFocus()) fs.requestFocus(); });
    }

    /** the focus on the item used last while the home stays in front, else the first line of the list (the first row, else a toggle) */
    private void focusStart(List<View> focusables) {
        View start = null;
        for (View v : focusables) if (v instanceof Cell && ((Cell) v).item != null && ((Cell) v).item.key.equals(lastKey)) start = v;
        if (start == null) for (View v : focusables) if (v instanceof Cell && ((Cell) v).kind == Cell.TOGGLE) { start = v; break; }
        if (start == null) start = focusables.get(0);
        final View focusFirst = start;
        focusFirst.requestFocus();
        body.post(() -> { if (showing() && !focusFirst.hasFocus()) focusFirst.requestFocus(); });   // the window may take the focus after this call
    }

    private TextView text(String s, int sp, int color, String family) {
        TextView t = new TextView(act);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(host.font(family));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setGravity(Dir.start() | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return t;
    }

    // ---------------------------------------------------------------- the cells

    /** One focusable thing in the panel; its fill comes in over FOCUS_MS when it has the focus, solid, with the look's ink over it. */
    private final class Cell extends LinearLayout {
        static final int TOGGLE = 0, ROW = 1, CHECK = 2, PILL = 3, PRIMARY = 4, BACK = 5;
        final int kind;
        final Item item;
        boolean ticked;
        private final GradientDrawable fill = new GradientDrawable();
        private final GradientDrawable circle = new GradientDrawable();
        private TextView name, value, own;
        private ImageView glyph, mark, chevron, chevron2;
        private ValueAnimator anim;
        private int shown;   // the fill showing now

        Cell(int kind, Item item) {
            super(act);
            this.kind = kind;
            this.item = item;
            setFocusable(true);
            setClickable(true);
            setId(View.generateViewId());
            fill.setCornerRadius(dp(kind == PILL || kind == PRIMARY ? 16 : 12));
            if (kind == TOGGLE) buildToggle();
            else if (kind == ROW) buildRow();
            else if (kind == CHECK) buildCheck();
            else if (kind == BACK) buildBack();
            if (kind != TOGGLE) setBackground(fill);
            if (item != null) {
                setContentDescription(item.name);
                setOnClickListener(v -> { if (item.action != null) item.action.run(); });
                if (item.hold != null) setOnLongClickListener(v -> { item.hold.run(); return true; });
            }
            paint(false);
        }

        void label(String s, int ink) {
            setGravity(Gravity.CENTER);
            setPadding(dp(16), 0, dp(16), 0);
            own = text(s, 15, ink == 0 ? ON : ink, "sans-serif-medium");
            own.setGravity(Gravity.CENTER);
            own.setSingleLine(true);
            addView(own, new LayoutParams(-2, -2));
            labelInk = ink;
            setContentDescription(s);
            paint(false);
        }
        private int labelInk;

        private void buildToggle() {
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            FrameLayout disc = new FrameLayout(act);
            disc.setBackground(circle);
            glyph = icon(item.icon, 24);
            disc.addView(glyph, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            addView(disc, new LayoutParams(dp(44), dp(44)));
            name = text(item.name, 12, TEXT, "sans-serif");
            name.setGravity(Gravity.CENTER);
            name.setPadding(dp(2), 0, dp(2), 0);
            LayoutParams nlp = new LayoutParams(-1, -2);
            nlp.topMargin = dp(4);
            addView(name, nlp);
            circle.setShape(GradientDrawable.OVAL);
        }

        private void buildRow() {
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(12), 0, dp(12), 0);
            glyph = icon(item.icon, 24);
            addView(glyph, new LayoutParams(dp(24), dp(24)));
            name = text(item.name, 16, TEXT, "sans-serif-medium");
            name.setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP);   // the title shrinks to fit before it is cut («إعدادات تلفزيونك» in a 280 dp panel)
            LayoutParams nlp = new LayoutParams(0, -2, 1f);
            nlp.setMarginStart(dp(12));
            addView(name, nlp);
            value = text(item.value, 13, SUB, "sans-serif");
            value.setMaxWidth(Math.round(act.getResources().getDisplayMetrics().widthPixels * PANEL_W * 0.30f));   // the line under a title never takes the title's room
            value.setGravity(Dir.end() | Gravity.CENTER_VERTICAL);
            item.valueView = value;
            LayoutParams vlp = new LayoutParams(-2, -2);
            vlp.setMarginStart(dp(8));
            if (!item.value.isEmpty()) addView(value, vlp);
            if (item.key.equals("tv") || item.key.equals("ours")) {   // these open another screen: a chevron on the end side
                chevron = icon(R.drawable.ic_chevron, 16);
                chevron.setScaleX(Dir.rtl() ? 1f : -1f);
                LayoutParams clp = new LayoutParams(dp(16), dp(16));
                clp.setMarginStart(dp(8));
                addView(chevron, clp);
            }
        }

        private void buildCheck() {
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(12), 0, dp(12), 0);
            mark = icon(R.drawable.ic_check, 16);
            addView(mark, new LayoutParams(dp(24), dp(24)));
            glyph = icon(item.icon, 24);
            LayoutParams glp = new LayoutParams(dp(24), dp(24));
            glp.setMarginStart(dp(12));
            addView(glyph, glp);
            name = text(item.name, 16, TEXT, "sans-serif-medium");
            name.setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP);
            LayoutParams nlp = new LayoutParams(0, -2, 1f);
            nlp.setMarginStart(dp(12));
            addView(name, nlp);
            chevron2 = icon(R.drawable.ic_chevron, 16);   // the two arrows shown on the focused row, in the order of the keys: «˄» (Right in Arabic) then «˅»
            chevron = icon(R.drawable.ic_chevron, 16);
            addView(chevron2, new LayoutParams(dp(16), dp(16)));
            LayoutParams clp = new LayoutParams(dp(16), dp(16));
            clp.setMarginStart(dp(8));
            addView(chevron, clp);
        }

        private void buildBack() {
            setGravity(Gravity.CENTER);
            glyph = icon(R.drawable.ic_chevron, 16);
            glyph.setRotation(Dir.rtl() ? 180f : 0f);   // «<» is back in English, «>» in Arabic
            addView(glyph, new LayoutParams(dp(16), dp(16)));
        }

        private ImageView icon(int res, int size) {
            ImageView v = new ImageView(act);
            v.setImageDrawable(act.getDrawable(res).mutate());
            return v;
        }

        @Override protected void onFocusChanged(boolean gained, int direction, android.graphics.Rect previouslyFocusedRect) {
            super.onFocusChanged(gained, direction, previouslyFocusedRect);
            if (gained && item != null) lastKey = item.key;
            paint(true);
        }

        /** the colours of the state, the fill coming in over FOCUS_MS (ease-out) when the focus changed */
        void paint(boolean animate) {
            boolean f = hasFocus();
            int target = f ? FOCUSED : kind == TOGGLE ? TILE : kind == PRIMARY ? ACCENT : 0;
            int ink = f ? ON : TEXT;
            GradientDrawable shape = kind == TOGGLE ? circle : fill;
            if (anim != null) anim.cancel();
            if (animate) {
                anim = ValueAnimator.ofArgb(shown, target);
                anim.setDuration(FOCUS_MS);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.addUpdateListener(a -> shape.setColor((Integer) a.getAnimatedValue()));
                anim.start();
            } else shape.setColor(target);
            shown = target;
            if (RING != 0) shape.setStroke(f ? dp(3) : kind == TOGGLE || kind == PILL ? Math.max(1, dp(1)) : 0, f ? RING : LINE);   // «رملي»: a calm focus, a green line round a soft fill
            else if (kind == TOGGLE && !f) shape.setStroke(Math.max(1, dp(1)), SleepTimer.minutesLeft(act) > 0 && item.key.equals("sleep") ? ACCENT : LINE);
            else if (kind == PILL && !f) shape.setStroke(Math.max(1, dp(1)), LINE);
            else shape.setStroke(0, 0);
            if (name != null) name.setTextColor(kind == TOGGLE ? TEXT : ink);
            if (value != null) value.setTextColor(f ? ON_SUB : SUB);
            if (glyph != null) glyph.getDrawable().setTint(ink);
            if (chevron != null) {
                chevron.getDrawable().setTint(f ? ON_SUB : SUB);
                if (kind == CHECK) {   // the focused row shows its two arrows (a toggle does not move)
                    int vis = f && !item.toggle() ? View.VISIBLE : View.INVISIBLE;
                    chevron2.setVisibility(vis);
                    chevron.setVisibility(vis);
                    chevron2.getDrawable().setTint(f ? ON_SUB : SUB);
                    chevron2.setRotation(90f);    // «<» turned a quarter clockwise points up: the first arrow, the key that moves the row up
                    chevron.setRotation(-90f);    // …counter-clockwise, down
                }
            }
            if (own != null) own.setTextColor(kind == PRIMARY || f ? ON : labelInk);
            if (mark != null) {   // the tick: a box in the row's colours, the check inside it when ticked
                mark.setBackground(box(f));
                mark.getDrawable().setTint(ticked ? (f ? FOCUSED : PANEL) : 0);
                mark.setImageAlpha(ticked ? 255 : 0);
                mark.setPadding(dp(3), dp(3), dp(3), dp(3));
            }
        }

        private GradientDrawable box(boolean f) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(6));
            g.setColor(ticked ? (f ? ON : ACCENT) : 0);
            g.setStroke(Math.max(1, dp(2)), ticked ? (f ? ON : ACCENT) : (f ? ON_SUB : SUB));
            return g;
        }
    }

    // ---------------------------------------------------------------- what the tiles say

    private String sleepLine() {
        int left = SleepTimer.minutesLeft(act);
        return Digits.of(left > 0 ? S.t("يعمل: باقي %s د", left) : S.t("١٥ إلى ٩٠ دقيقة"));
    }

    /** the network by name, as the TV reports it (the cable, the Wi-Fi's name, or not connected); read now, once (no polling) */
    private String networkLine() {
        boolean wired = false, wifi = false;
        String ssid = null;
        try {
            android.net.ConnectivityManager cm = act.getSystemService(android.net.ConnectivityManager.class);
            android.net.NetworkCapabilities nc = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
            wired = nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET);
            wifi = nc != null && !wired && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
            if (wifi) {
                WifiManager wm = (WifiManager) act.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                android.net.wifi.WifiInfo info = wm == null ? null : wm.getConnectionInfo();   // one snapshot: two calls could answer two different networks (S8)
                if (info != null) ssid = info.getSSID();
            }
        } catch (RuntimeException e) {   // S8 (A6, the TCL G07): any failure of the network services leaves the row with what it has; the panel must open
            android.util.Log.w("UrTVControl", "network tile failed " + e.getClass().getSimpleName());
        }
        return ControlText.network(wired, wifi, ssid);
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
        } catch (RuntimeException | LinkageError e) {   // A6 (TCL G07): the audio service may answer with anything, or an old class: the line says «غير معروف» and the panel stays
            android.util.Log.w("UrTVControl", "output line failed " + e.getClass().getSimpleName());
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

    /** the first Bluetooth / accessories screen this TV can open (ControlText.bluetoothAction), or null when none resolves */
    private Intent bluetoothIntent() {
        android.content.pm.PackageManager pm = act.getPackageManager();
        String action = ControlText.bluetoothAction(a -> {
            try { return pm.resolveActivity(new Intent(a), 0) != null; } catch (RuntimeException e) { return false; }
        });
        return action == null ? null : new Intent(action);
    }

    /** the sound settings if this TV has them (the public action), else the TV's settings: never an intent that does not resolve */
    private Intent soundIntent() {
        Intent sound = new Intent(Settings.ACTION_SOUND_SETTINGS);
        if (prefs().getBoolean("sound_bad", false)) return new Intent(Settings.ACTION_SETTINGS);   // this TV's sound screen crashed: never again
        try {
            return act.getPackageManager().resolveActivity(sound, 0) != null ? sound : new Intent(Settings.ACTION_SETTINGS);
        } catch (RuntimeException e) {
            return new Intent(Settings.ACTION_SETTINGS);
        }
    }

    // ---------------------------------------------------------------- «أطفئ الآن»

    /**
     * With the sleep service on the screen turns off at once. Without it the tile says «يحتاج تفعيل، اضغط للتفعيل» and OK opens the TV's accessibility settings (behind the same
     * lock as the sleep timer's own line) with a line saying which service to turn on; there is no silent fallback here any more («شاشة التوقف» next to it starts the screensaver).
     */
    private void turnOffNow() {
        if (SleepService.enabled()) { dismiss(); SleepTimer.turnOff(act); return; }
        dismiss();
        android.widget.Toast.makeText(act, S.t("فعّل «%s»", act.getString(R.string.sleep_service_label)), android.widget.Toast.LENGTH_LONG).show();
        host.launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "إعدادات التلفزيون");
    }

    // ---------------------------------------------------------------- «اختبار السرعة»

    /** OK starts a download of a few MB off the main thread; OK while it runs stops it. Closing the panel stops it too. */
    private void speedTest(Item t) {
        if (speed != null) {
            speed.cancel();
            speed = null;
            t.setValue(S.t("أُلغي · اضغط اختيار للبدء"));
            return;
        }
        final SpeedTest test = new SpeedTest();
        speed = test;
        t.setValue(S.t("جارٍ القياس…"));
        new Thread(() -> {
            String said;
            try {
                said = Digits.of(ControlText.mbps(test.downloadMbps()));
            } catch (java.io.IOException | RuntimeException e) {
                said = test.cancelled() ? null : S.t("تعذّر القياس: تأكد من الشبكة");
            }
            final String result = said;
            ui.post(() -> {
                if (speed != test || !showing()) return;   // cancelled, or the panel closed meanwhile
                speed = null;
                t.setValue(result);
            });
        }, "speed-test").start();
    }

    private int dp(float v) { return Math.round(v * density); }
}
