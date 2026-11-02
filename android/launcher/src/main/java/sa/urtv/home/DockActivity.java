package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * «ترتيب المفضلة»: the favourites row as a list, arranged with the remote. OK on an app picks it up, ▲▼ move
 * it, OK drops it. «إزالة» beside each one, every other app below to add. No dialogs: the launcher is laid
 * out left to right, so rows are built right-aligned by hand. Opened from the screensaver's settings only (the home
 * screen's own way is «تعديل المفضلة», see FavEdit), or `am start -n sa.urtv.home/.DockActivity`.
 */
public class DockActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private int GOLD, INK, TEXT, DIM;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;

    private Map<String, String> apps;
    private final List<String> row = new ArrayList<>();
    private LinearLayout list;
    private TextView count;
    private final List<View> cards = new ArrayList<>(), removes = new ArrayList<>();
    private int moving = -1;
    private View screen;
    private ScrollView scroll;
    private boolean verified;
    private int swallowUp = -1;   // the OK that drops an app must not also click it

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        apps = Dock.launchable(this);
        row.addAll(Dock.shown(this, apps));

        scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(36), 0, dp(48));
        frame.addView(col, new FrameLayout.LayoutParams(dp(600), -2, Gravity.CENTER_HORIZONTAL));

        col.addView(text(S.t("ترتيب المفضلة"), 26, "Medium", TEXT), right(0));
        col.addView(text(S.t("اختيار على تطبيق لتحريكه بالأسهم (أعلى/أسفل) ثم اختيار لتثبيته. من %s إلى %s تطبيقات.", ar(Dock.MIN),
            ar(Dock.MAX)), 14, "Regular", DIM), right(dp(6)));
        count = text("", 13, "Medium", GOLD);
        col.addView(count, right(dp(24)));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list, new LinearLayout.LayoutParams(-1, -2));
        // the D-pad ids of these rows are left-to-right: pinned, so a TV set to Arabic does not mirror the rows under them. English is the
        // mirror of the Arabic screen: the same rows laid out right to left, their margins at the start and their ◀▶ ids swapped
        scroll.setLayoutDirection(Dir.layout(!Dir.rtl()));
        setContentView(scroll);
        screen = scroll;
        render(0);
    }

    /** «قفل الإعدادات»: with it on, this screen opens only after the PIN, whoever opens it (the tiles' menu has asked already). */
    @Override
    protected void onResume() {
        super.onResume();
        if (verified) return;
        String[] targets = ParentalPolicy.targets(ParentalPolicy.ARRANGE_SCREEN, null);
        if (!Parental.needsPin(this, targets)) {
            verified = true;
            return;
        }
        screen.setVisibility(View.INVISIBLE);
        Parental.guard(this, Parental.adhanFirst(this), "ترتيب المفضلة", targets, () -> {
            verified = true;
            screen.setVisibility(View.VISIBLE);
        }, () -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinPrompt.dismiss(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        verified = false;
    }

    // ---------------------------------------------------------------- list

    private void render(int focus) {
        list.removeAllViews();
        cards.clear();
        removes.clear();
        count.setText(S.t("المفضلة %s من %s", ar(row.size()), ar(Dock.MAX)));
        for (int i = 0; i < row.size(); i++) list.addView(favouriteRow(i), rowParams());

        List<String> others = new ArrayList<>(apps.keySet());
        others.removeAll(row);
        Collections.sort(others, (x, y) -> apps.get(x).compareToIgnoreCase(apps.get(y)));
        if (!others.isEmpty()) {
            TextView h = text(S.t("إضافة تطبيق"), 13, "Medium", GOLD);
            LinearLayout.LayoutParams hp = right(0);
            hp.topMargin = dp(22);
            hp.bottomMargin = dp(8);
            list.addView(h, hp);
            for (String p : others) list.addView(card(p, "+", false, v -> add(p)), rowParams());
        }
        View reset = card(null, S.t("الترتيب الافتراضي"), false, v -> {
            Dock.reset(this);
            row.clear();
            row.addAll(Dock.shown(this, apps));
            render(0);
            toast(S.t("رجعت المفضلة للترتيب الافتراضي"));
        });
        LinearLayout.LayoutParams rp = rowParams();
        rp.topMargin = dp(22);
        list.addView(reset, rp);

        for (int i = 0; i < cards.size(); i++) {   // ▲▼ stay in the column: card to card, «إزالة» to «إزالة»
            cards.get(i).setNextFocusUpId(cards.get(Math.max(0, i - 1)).getId());
            removes.get(i).setNextFocusUpId(removes.get(Math.max(0, i - 1)).getId());
            if (i < cards.size() - 1) {
                cards.get(i).setNextFocusDownId(cards.get(i + 1).getId());
                removes.get(i).setNextFocusDownId(removes.get(i + 1).getId());
            }
        }
        // the first app brings the list back to its very top, the title over it (the TV test: back up, the header stayed hidden)
        View first = cards.isEmpty() ? reset : cards.get(0);
        final View.OnFocusChangeListener was = first.getOnFocusChangeListener();
        first.setOnFocusChangeListener((v, has) -> {
            if (was != null) was.onFocusChange(v, has);
            if (has) scroll.post(() -> scroll.smoothScrollTo(0, 0));
        });
        View f = cards.isEmpty() ? reset : cards.get(Math.max(0, Math.min(focus, cards.size() - 1)));
        f.post(f::requestFocus);
    }

    /** [إزالة] [  label  icon  n ] — right-aligned by hand (English: the screen is mirrored, [ n  icon  label  ] [إزالة]). */
    private View favouriteRow(final int i) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        TextView remove = (TextView) pill(S.t("إزالة"), v -> remove(i));
        r.addView(remove, new LinearLayout.LayoutParams(dp(84), -1));
        View c = card(row.get(i), ar(i + 1), i == moving, v -> {
            if (moving == i) drop(); else { moving = i; render(i); }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMarginStart(dp(10));
        r.addView(c, cp);
        if (Dir.rtl()) c.setNextFocusLeftId(remove.getId()); else c.setNextFocusRightId(remove.getId());
        if (Dir.rtl()) remove.setNextFocusRightId(c.getId()); else remove.setNextFocusLeftId(c.getId());
        cards.add(c);
        removes.add(remove);
        return r;
    }

    /** A row card: badge (number or +) on the right (the left in English), then the icon and name; pkg null = a text button. */
    private View card(String pkg, String badge, boolean lifted, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        c.setPadding(dp(16), dp(10), dp(16), dp(10));
        c.setFocusable(true);
        c.setClickable(true);
        c.setId(View.generateViewId());

        final TextView arrows = text(lifted ? "▲▼" : "", 14, "Medium", INK);
        c.addView(arrows, new LinearLayout.LayoutParams(-2, -2));
        c.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        final TextView name = text(pkg == null ? badge : apps.get(pkg), 17, "Medium", TEXT);
        name.setSingleLine(true);
        c.addView(name, new LinearLayout.LayoutParams(-2, -2));
        if (pkg != null) {
            ImageView ic = new ImageView(this);
            try {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                ic.setImageDrawable(d);
            } catch (Exception ignored) { }
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(30), dp(30));
            ip.setMarginStart(dp(12));   // the same on both sides
            ip.setMarginEnd(dp(12));
            c.addView(ic, ip);
            TextView n = text(badge, 15, "Medium", GOLD);
            n.setMinWidth(dp(22));
            n.setGravity(Gravity.CENTER);
            c.addView(n, new LinearLayout.LayoutParams(-2, -2));
        }
        if (lifted) {   // picked up: the solid focus fill all the time, its words in the focus ink
            c.setBackground(GlassPaint.fill(this, look.focus, 14, 0));
            name.setTextColor(INK);
            if (pkg != null) ((TextView) c.getChildAt(c.getChildCount() - 1)).setTextColor(INK);
        } else {
            GlassPaint.card(c, look, 14);
        }
        c.setOnClickListener(click);
        return c;
    }

    private View pill(String label, View.OnClickListener click) {
        final TextView p = text(label, 13, "Medium", DIM);
        p.setGravity(Gravity.CENTER);
        p.setFocusable(true);
        p.setClickable(true);
        p.setId(View.generateViewId());
        p.setTextColor(look.warn);   // «إزالة»: the warning colour, the solid focus fill while focused
        GlassPaint.card(p, look, 14);
        p.setOnClickListener(click);
        return p;
    }

    // ---------------------------------------------------------------- actions

    private void remove(int i) {
        if (row.size() <= Dock.MIN) { toast(S.t("المفضلة لا تقل عن %s تطبيقات", ar(Dock.MIN))); return; }
        row.remove(i);
        Dock.save(this, row);
        render(Math.min(i, row.size() - 1));
    }

    private void add(String pkg) {
        if (row.size() >= Dock.MAX) { toast(S.t("المفضلة ممتلئة (%s)", ar(Dock.MAX))); return; }
        row.add(pkg);
        Dock.save(this, row);
        render(row.size() - 1);
        toast(S.t("أُضيف %s", apps.get(pkg)));
    }

    private void drop() {
        int at = moving;
        moving = -1;
        Dock.save(this, row);
        render(at);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (swallowUp == k && e.getAction() == KeyEvent.ACTION_UP) { swallowUp = -1; return true; }
        if (moving < 0) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;   // while moving, every key is ours
        if (k == KeyEvent.KEYCODE_DPAD_UP && moving > 0) {
            Collections.swap(row, moving, moving - 1);
            moving--;
            render(moving);
        } else if (k == KeyEvent.KEYCODE_DPAD_DOWN && moving < row.size() - 1) {
            Collections.swap(row, moving, moving + 1);
            moving++;
            render(moving);
        } else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_BACK) {
            swallowUp = k;
            drop();
        }
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(S.tv(s));   // the screen's words given as Arabic are said in «اللغة»; one already said stays
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams right(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        return p;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static String ar(int n) {
        return Digits.of(String.valueOf(n));
    }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub;
    }
}
