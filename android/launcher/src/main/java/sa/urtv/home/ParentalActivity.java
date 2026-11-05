package sa.urtv.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

/**
 * «الرقابة الأبوية»: the master PIN, «قفل الإعدادات», «وضع الأطفال», a lock for each pill, and each installed app with its lock and its
 * «يظهر للأطفال». Everything is off until a PIN is set. With a PIN it opens only after the PIN (and again after the home screen has
 * come to the front). Opened from the tiles' long-press menu, or {@code am start -n sa.urtv.home/.ParentalActivity}. A family lock on
 * what the home screen opens, not a lock on the TV; the line under the title says so.
 */
public class ParentalActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private int GOLD, INK, TEXT, DIM;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;

    private Map<String, String> apps;
    private LinearLayout list;
    private View screen;
    private PinPrompt.Before adhan;
    private boolean verified;
    private int kidsRow;   // where «وضع الأطفال» is in rows, to put the focus back on it
    private final List<View[]> rows = new ArrayList<>();   // each row's focusable cells, left to right

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        adhan = Parental.adhanFirst(this);
        apps = Dock.launchable(this);

        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(36), 0, dp(48));
        frame.addView(col, new FrameLayout.LayoutParams(dp(600), -2, Gravity.CENTER_HORIZONTAL));
        col.addView(text(S.t("الرقابة الأبوية"), 26, "Medium", TEXT), right(0));
        col.addView(text(S.t("قفل عائلي على ما تفتحه شاشة تلفزيونك الرئيسية، وليس قفلاً كاملاً للتلفزيون."), 14, "Regular", DIM), right(dp(24)));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list, new LinearLayout.LayoutParams(-1, -2));
        // the D-pad ids of these rows are left-to-right: pinned, so a TV set to Arabic does not mirror the rows under them. English is the
        // mirror of the Arabic screen: the same rows laid out right to left, their margins at the start and their ◀▶ ids swapped (wire)
        scroll.setLayoutDirection(Dir.layout(!Dir.rtl()));
        setContentView(scroll);
        screen = scroll;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (verified) return;
        if (!Parental.needsPin(this, ParentalPolicy.PARENTAL)) {
            open();
            return;
        }
        screen.setVisibility(View.INVISIBLE);   // nothing of the lists shows before the PIN
        Parental.guard(this, adhan, "الرقابة الأبوية", ParentalPolicy.targets(ParentalPolicy.PARENTAL_SCREEN, null), this::open, () -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinPrompt.dismiss(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        verified = false;   // coming back asks again (unless the home screen has not been to the front since)
    }

    private void open() {
        verified = true;
        screen.setVisibility(View.VISIBLE);
        render(0, 0);
    }

    // ---------------------------------------------------------------- list

    private void render(int focusRow, int focusCell) {
        list.removeAllViews();
        rows.clear();
        ParentalPolicy.State s = Parental.state(this);

        // the card and its two buttons (docs/UX-REVIEW.md): off, «فعّل الرقابة الأبوية» and «ما الذي يُقفل؟»; on, «التطبيقات المقفلة» and «غيّر الرمز»
        if (!s.pinSet) {
            list.addView(card(S.t("الرقابة الأبوية متوقفة"), S.t("ضع رمزًا من أربعة أرقام، ثم اختر ما يُقفل. بدون رمز لا يُقفل شيء، والشاشة الرئيسية كما هي.")), cardParams());
            View what = button(S.t("ما الذي يُقفل؟"), v -> explain());
            View start = primary(button(S.t("فعّل الرقابة الأبوية"), v -> choosePin(false)));
            list.addView(buttons(what, start), rowParams());
            explainAt = list.getChildCount();
            wire();
            focusAfter(focusRow, focusRow == 0 && focusCell == 0 ? 1 : focusCell);
            return;
        }
        int lockedApps = 0;
        for (String pkg : apps.keySet()) if (s.locked.contains(pkg)) lockedApps++;
        list.addView(card(S.t("الرقابة الأبوية مفعّلة"), Digits.of(ParentalText.summary(lockedApps, s.locked.contains(ParentalPolicy.OWN_SETTINGS), s.kids))), cardParams());
        View pin = button(S.t("غيّر الرمز"), v -> choosePin(true));
        View locked = button(S.t("التطبيقات المقفلة"), v -> focusAfter(appsRow, 1));
        list.addView(buttons(pin, locked), rowParams());
        list.addView(row(S.t("قفل الإعدادات"), null, toggle(s.locked.contains(ParentalPolicy.OWN_SETTINGS), S.t("مقفل"), S.t("مفتوح"),
            on -> Parental.setLocked(this, ParentalPolicy.OWN_SETTINGS, on))), rowParams());
        list.addView(text(S.t("يقفل إعدادات تلفزيونك، وخيارات التطبيقات، وترتيب المفضلة."), 12, "Regular", DIM), right(dp(8)));
        kidsRow = rows.size();
        list.addView(row(S.t("وضع الأطفال"), null, state(s.kids, v -> kids())), rowParams());
        list.addView(text(S.t("يُظهر في الشاشة الرئيسية التطبيقات المعلَّمة «يظهر للأطفال» فقط، والخروج منه يحتاج الرمز."), 12, "Regular", DIM), right(dp(8)));
        list.addView(row(S.t("إيقاف الرقابة الأبوية"), null, button(S.t("إيقاف"), v -> Parental.confirm(this, adhan, "إيقاف الرقابة الأبوية", () -> {
            Parental.turnOff(this);
            toast(S.t("أُوقفت الرقابة الأبوية"));
            render(0, 0);
        }, () -> { }))), rowParams());

        list.addView(header(S.t("الأزرار في الأعلى")));
        for (String pill : ParentalPolicy.PILLS) {
            list.addView(row(pillName(pill), null, toggle(s.locked.contains(pill), S.t("مقفل"), S.t("مفتوح"), on -> Parental.setLocked(this, pill, on))), rowParams());
        }
        list.addView(header(S.t("التطبيقات")));
        appsRow = rows.size();
        for (Map.Entry<String, String> e : apps.entrySet()) {
            String pkg = e.getKey();
            list.addView(row(e.getValue(), pkg,
                toggle(s.allowed.contains(pkg), S.t("يظهر للأطفال"), S.t("مخفي عن الأطفال"), on -> Parental.setAllowed(this, pkg, on)),
                toggle(s.locked.contains(pkg), S.t("مقفل"), S.t("مفتوح"), on -> Parental.setLocked(this, pkg, on))), rowParams());
        }
        wire();
        focusAfter(focusRow, focusRow == 0 && focusCell == 0 ? 1 : focusCell);   // the card's first button: the one at the start (the right in Arabic)
    }

    private static String pillName(String pill) {
        switch (pill) {
            case ParentalPolicy.INPUTS: return S.t("المصادر");
            case ParentalPolicy.WIFI: return S.t("واي فاي");
            case ParentalPolicy.TV_SETTINGS: return S.t("إعدادات التلفزيون");
            case ParentalPolicy.TASKS: return S.t("المهام");
            case ParentalPolicy.SAVER: return S.t("شاشة التوقف");
            case ParentalPolicy.LISTEN: return S.t("تلاوة وأذان");
            case ParentalPolicy.CENTER: return S.t("مركز التحكم");
            case ParentalPolicy.SLEEP: return S.t("مؤقت النوم");
            default: return pill;
        }
    }

    // ---------------------------------------------------------------- actions

    private void choosePin(boolean change) {
        Parental.choose(this, adhan, () -> {
            toast(change ? S.t("تغيّر الرمز") : S.t("عُيّن الرمز"));
            render(0, 0);
        }, () -> { });
    }

    private void kids() {
        ParentalPolicy.State s = Parental.state(this);   // the toggles under it may have changed since the list was drawn
        if (s.kids) {
            Parental.confirm(this, adhan, S.t("الخروج من وضع الأطفال"), () -> {
                Parental.setKids(this, false);
                render(kidsRow, 0);
            }, () -> { });
            return;
        }
        if (!ParentalPolicy.canStartKids(s)) {
            toast(S.t("علّم تطبيقاً واحداً على الأقل «يظهر للأطفال» أولاً"));
            return;
        }
        Parental.setKids(this, true);
        render(kidsRow, 0);
    }

    // ---------------------------------------------------------------- views

    private interface Flip {
        void to(boolean on);
    }

    /** A cell that flips between two words and keeps the choice. Nothing is saved without a PIN (the screen is only here with one). */
    private View toggle(boolean on, String yes, String no, Flip flip) {
        final boolean[] state = {on};
        final TextView t = cell(on ? yes : no, on);
        t.setOnClickListener(v -> {
            state[0] = !state[0];
            flip.to(state[0]);
            t.setText(S.tv(state[0] ? yes : no));
            paint(t, state[0], t.hasFocus());
        });
        return t;
    }

    /** An on/off cell that opens its own question (kids mode asks for the PIN first): «مفعّل» or «متوقف», drawn as every on/off one. */
    private View state(boolean on, View.OnClickListener click) {
        TextView t = cell(on ? OnOff.ON_WORD : OnOff.OFF_WORD, on);
        t.setOnClickListener(click);
        return t;
    }

    private View button(String label, View.OnClickListener click) {
        TextView t = cell(label, false);
        t.setOnClickListener(click);
        return t;
    }

    private TextView cell(String label, boolean on) {
        final TextView t = text(label, 14, "Medium", TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        t.setFocusable(true);
        t.setClickable(true);
        t.setId(View.generateViewId());
        t.setTag(on);
        t.setOnFocusChangeListener((v, has) -> paint(t, Boolean.TRUE.equals(t.getTag()), has));
        paint(t, on, false);
        return t;
    }

    private void paint(TextView t, boolean on, boolean focused) {
        t.setTag(on);
        // focused: the solid focus fill, else the panel; on: as every on/off one (OnOff): the three green triangles at its start and its
        // words in the green that reads on what is under them; off: plain words
        GradientDrawable bg = focused ? GlassPaint.focused(this, look, 14) : GlassPaint.panel(this, look, 14);
        t.setBackground(bg);
        int green = OnOff.green(focused ? look.focus : look.light ? 0xFFFFFFFF : 0xFF000000);
        Drawable mark = null;
        if (on) {
            mark = getDrawable(R.drawable.ic_on).mutate();
            mark.setTint(green);
            mark.setBounds(0, 0, dp(7), dp(22));
        }
        if (Dir.rtl()) t.setCompoundDrawables(null, null, mark, null);   // on the right: where an Arabic word starts
        else t.setCompoundDrawables(mark, null, null, null);   // English: on the left
        t.setCompoundDrawablePadding(dp(8));
        t.setTextColor(on ? green : focused ? INK : TEXT);
    }

    /** [cells…] [ name  icon ], right-aligned by hand as in «ترتيب المفضلة» (English: the screen is mirrored, [icon  name ] [cells…]). */
    private View row(String label, String pkg, View... cells) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        for (View c : cells) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(150), dp(40));
            p.setMarginStart(dp(8));
            r.addView(c, p);
        }
        r.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        TextView name = text(label, 16, "Medium", TEXT);
        name.setSingleLine(true);
        r.addView(name, new LinearLayout.LayoutParams(-2, -2));
        if (pkg != null) {
            ImageView ic = new ImageView(this);
            try {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                ic.setImageDrawable(d);
            } catch (PackageManager.NameNotFoundException ignored) { }
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(30), dp(30));
            ip.setMarginStart(dp(12));
            r.addView(ic, ip);
        }
        rows.add(cells);
        return r;
    }

    private int appsRow, explainAt;

    /** The card at the top: what the lock is now, in a title and one line under it, on the look's panel. */
    private View card(String title, String line) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(24), dp(20), dp(24), dp(20));
        c.setBackground(GlassPaint.panel(this, look, 16));
        c.addView(text(title, 20, "Medium", TEXT), right(dp(6)));
        c.addView(text(line, 14, "Regular", DIM), right(0));
        return c;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        return p;
    }

    /** Two buttons under the card, side by side at the start (the right in Arabic): [second] [first], the first one focused. */
    private View buttons(View second, View first) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        r.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        for (View c : new View[] {second, first}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(200), dp(44));
            p.setMarginStart(dp(12));
            r.addView(c, p);
        }
        rows.add(new View[] {second, first});
        return r;
    }

    /** The button that starts the lock: the look's accent filled at rest (the focus look while focused, as every other). */
    private View primary(View b) {
        TextView t = (TextView) b;
        t.setOnFocusChangeListener((v, has) -> paintPrimary(t, has));
        paintPrimary(t, false);
        return t;
    }

    private void paintPrimary(TextView t, boolean focused) {
        if (focused) { paint(t, false, true); return; }
        t.setBackground(GlassPaint.fill(this, look.accent, 14, 0));
        t.setCompoundDrawables(null, null, null, null);
        t.setTextColor(GlassStyle.contrast(0xFFFFFFFF, look.accent) >= GlassStyle.contrast(0xFF000000, look.accent) ? 0xFFFFFFFF : 0xFF14161A);
    }

    /** «ما الذي يُقفل؟»: the long answer under the buttons, once (OK again hides it). */
    private void explain() {
        if (list.getChildCount() > explainAt) { list.removeViewAt(explainAt); return; }
        list.addView(text(S.t("التطبيقات التي تختارها، وإعدادات تلفزيونك وخيارات التطبيقات، والأزرار في الأعلى إن أردت، ووضع الأطفال الذي لا يُظهر إلا ما تسمح به. التلفزيون نفسه يبقى كما هو."),
                14, "Regular", DIM), explainAt, right(dp(8)));
    }

    private TextView header(String label) {
        TextView h = text(label, 13, "Medium", GOLD);
        LinearLayout.LayoutParams hp = right(dp(8));
        hp.topMargin = dp(22);
        h.setLayoutParams(hp);
        return h;
    }

    /** The D-pad: ▲▼ stay in the column, ◀▶ move along the row, nothing wraps. */
    private void wire() {
        for (int i = 0; i < rows.size(); i++) {
            View[] cs = rows.get(i);
            for (int j = 0; j < cs.length; j++) {
                cs[j].setNextFocusUpId(rows.get(Math.max(0, i - 1))[Math.min(j, rows.get(Math.max(0, i - 1)).length - 1)].getId());
                int down = Math.min(rows.size() - 1, i + 1);
                cs[j].setNextFocusDownId(rows.get(down)[Math.min(j, rows.get(down).length - 1)].getId());
                int before = cs[Math.max(0, j - 1)].getId(), after = cs[Math.min(cs.length - 1, j + 1)].getId();   // before: on the left in Arabic
                cs[j].setNextFocusLeftId(Dir.rtl() ? before : after);
                cs[j].setNextFocusRightId(Dir.rtl() ? after : before);
            }
        }
    }

    private void focusAfter(int row, int cell) {
        if (rows.isEmpty()) return;
        View[] cs = rows.get(Math.max(0, Math.min(row, rows.size() - 1)));
        View f = cs[Math.max(0, Math.min(cell, cs.length - 1))];
        f.post(f::requestFocus);
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(S.tv(s));   // the screen's words given as Arabic (OnOff's, a pill's name) are said in «اللغة»; one already said stays
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

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub;
    }
}
