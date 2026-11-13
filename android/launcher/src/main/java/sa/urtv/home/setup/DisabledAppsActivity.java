package sa.urtv.home.setup;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import sa.urtv.home.Dir;
import sa.urtv.home.GlassPaint;
import sa.urtv.home.GlassSource;
import sa.urtv.home.GlassStyle;
import sa.urtv.home.PinGuard;
import sa.urtv.home.S;

/**
 * 4.3.1 «التطبيقات المعطّلة»: the apps turned off on this TV (by the person, by UrTV or by the maker), and a way to turn the chosen ones back on. Behind the family PIN. It only ever
 * turns apps ON (SetupEngine.enableApps: `pm enable`), never disables, uninstalls or clears anything. The list needs no connection (PackageManager says which apps are off); turning
 * on needs the TV's own adb (loopback, else the paired route), so the screens are: the list, one confirmation, the result of every app (worked, or why not), and a short screen when
 * no route to the TV exists that points to the pairing steps. An app that comes back leaves UrTV's ledger, so «رجّع واجهة المصنع» stays exact.
 */
public class DisabledAppsActivity extends Activity {
    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish();   // Home pressed: the screen is done
    }

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    /** SetupEngine.enableApps' reasons (plain Java words), marked here for «اللغة»; S.tv says them */
    static final String[] WHY = {
        S.n("التطبيق ما عاد موجود على التلفزيون"), S.n("التلفزيون ما سمح بتشغيله"), S.n("التلفزيون ما شغّله"), S.n("التلفزيون ما أكّد لنا النتيجة"),
    };

    private GlassStyle.Look look;
    private LinearLayout col;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Set<String> picked = new LinkedHashSet<>();
    private List<DisabledApps.Row> rows = new ArrayList<>();
    private boolean busy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // a sleeping screen shows no «Allow USB debugging?»
        ScrollView scroll = new ScrollView(this);
        look = GlassSource.page(this, scroll);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        frame.setPadding(Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16), Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16));
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(40), dp(32), dp(40), dp(32));
        col.setBackground(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));
        frame.addView(col, new FrameLayout.LayoutParams(Math.min(dp(1100), w - 2 * Math.round(w * 0.05f)), -2, Gravity.CENTER));
        setContentView(scroll);
        PinGuard.confirm(this, S.t("التطبيقات المعطّلة"), () -> list(), () -> finish());   // the family PIN first when one is set; a cancelled PIN leaves
    }

    @Override public void onBackPressed() { if (!busy) finish(); }

    // ---------------------------------------------------------------- the list

    private void load() {
        PackageManager pm = getPackageManager();
        List<String> pkgs = new ArrayList<>(), names = new ArrayList<>();
        List<Integer> states = new ArrayList<>();
        try {
            for (ApplicationInfo a : pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS)) {
                int st = pm.getApplicationEnabledSetting(a.packageName);
                if (!DisabledApps.off(st)) continue;
                pkgs.add(a.packageName);
                states.add(st);
                String label = a.packageName;
                try { label = String.valueOf(pm.getApplicationLabel(a)); } catch (RuntimeException ignored) { }
                names.add(label);
            }
        } catch (RuntimeException ignored) { }
        List<String> ledger = SetupEngine.ledgerNames(new FileStore(this).get("disabled-by-us.txt"));
        for (String p : SetupEngine.ledgerNames(new FileStore(this).get("community-applied.txt"))) if (!ledger.contains(p)) ledger.add(p);
        rows = DisabledApps.rows(pkgs, names, states, ledger);
        picked.retainAll(pkgsOf(rows));
    }

    private static Set<String> pkgsOf(List<DisabledApps.Row> r) {
        Set<String> s = new LinkedHashSet<>();
        for (DisabledApps.Row x : r) s.add(x.pkg);
        return s;
    }

    private void list() {
        load();
        col.removeAllViews();
        col.addView(text(S.t("التطبيقات المعطّلة"), 30, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("اختر اللي تبي تشغّله من جديد. من هنا ما نعطّل شيء، بس نشغّل."), 20, "Regular", look.sub), lp(dp(16)));
        View first = null;
        if (rows.isEmpty()) {
            col.addView(text(S.t("ما في تطبيقات معطّلة في تلفزيونك."), 22, "Regular", look.text), lp(dp(16)));
        } else {
            int mine = DisabledApps.byUrtv(rows);
            col.addView(text(S.t("%s تطبيقًا معطّلًا", String.valueOf(rows.size())) + (mine > 0 ? S.t("، منها %s عطّلها UrTV", String.valueOf(mine)) : ""), 20, "Regular", look.sub), lp(dp(8)));
            for (DisabledApps.Row r : rows) {
                View v = appRow(r);
                col.addView(v, lp(dp(8)));
                if (first == null) first = v;
            }
            col.addView(text(S.t("«رجّع واجهة المصنع» يبقى صحيحًا: نحدّث سجلّه مع كل تطبيق تشغّله."), 18, "Regular", look.sub), lp(dp(12)));
        }
        LinearLayout row = row();
        final TextView go = button(goLabel(), v -> { if (!picked.isEmpty()) sure(); });
        go.setTag("go");
        View back = button(S.t("رجوع"), v -> finish());
        row.addView(go, bw(false));
        row.addView(back, bw(true));
        final View start = first != null ? first : back;
        focus(start);
    }

    private String goLabel() {
        int n = picked.size();
        if (n == 0) return S.t("اختر تطبيقًا");
        if (n == 1) return S.t("شغّل تطبيقًا واحدًا");
        if (n == 2) return S.t("شغّل تطبيقين");
        return n <= 10 ? S.t("شغّل %s تطبيقات", String.valueOf(n)) : S.t("شغّل %s تطبيقًا", String.valueOf(n));
    }

    /** one app: its tick, its name and package, and «عطّلها UrTV» as small plain text when UrTV turned it off; OK ticks it */
    private View appRow(final DisabledApps.Row r) {
        final LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setLayoutDirection(Dir.layout());
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(16), dp(12), dp(16), dp(12));
        final TextView tick = text("", 24, "Medium", look.text);
        tick.setGravity(Gravity.CENTER);
        v.addView(tick, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(r.name, 22, "Medium", look.text));
        if (!r.name.equals(r.pkg)) {
            TextView p = text(r.pkg, 16, "Regular", look.sub);
            p.setTextDirection(View.TEXT_DIRECTION_LTR);
            names.addView(p);
        }
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.setMarginStart(dp(16));
        v.addView(names, np);
        if (r.byUrtv) {
            TextView note = text(S.t("عطّلها UrTV"), 16, "Regular", look.sub);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-2, -2);
            nl.setMarginStart(dp(16));
            v.addView(note, nl);
        }
        v.setFocusable(true);
        v.setClickable(true);
        v.setId(View.generateViewId());
        GlassPaint.card(v, look, 14);
        paintTick(tick, picked.contains(r.pkg));
        v.setContentDescription(r.name);
        v.setOnClickListener(x -> {
            if (!picked.remove(r.pkg)) picked.add(r.pkg);
            paintTick(tick, picked.contains(r.pkg));
            TextView go = col.findViewWithTag("go");
            if (go != null) go.setText(goLabel());
        });
        return v;
    }

    private void paintTick(TextView tick, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setStroke(dp(3), look.sub);
        if (on) g.setColor(look.accent);
        tick.setBackground(g);
        tick.setText(on ? "✓" : "");
        tick.setTextColor(on ? look.base : look.text);
    }

    // ---------------------------------------------------------------- the confirmation

    private void sure() {
        col.removeAllViews();
        List<String> shown = new ArrayList<>();
        for (DisabledApps.Row r : rows) if (picked.contains(r.pkg)) shown.add(r.name);
        col.addView(text(goLabel() + "؟", 30, "Medium", look.text), lp(dp(12)));
        col.addView(text(String.join("، ", shown) + S.t(": تظهر في قائمة التطبيقات وتشتغل من جديد."), 22, "Regular", look.text), lp(dp(12)));
        col.addView(text(S.t("نشغّلها من التلفزيون نفسه، وبعدها نعرض لك نتيجة كل تطبيق. ما نحذف شيء، وتقدر تعطّلها من جديد بنفس الإعداد الأصلي."), 20, "Regular", look.sub), lp(dp(16)));
        LinearLayout row = row();
        View cancel = button(S.t("إلغاء"), v -> list());
        row.addView(cancel, bw(false));
        row.addView(button(S.t("نعم، شغّلها"), v -> run(new ArrayList<>(picked))), bw(true));
        focus(cancel);   // the safe press
    }

    // ---------------------------------------------------------------- turning on

    private void run(final List<String> pkgs) {
        message(S.t("نشغّل التطبيقات…"), S.t("قد يظهر على الشاشة سؤال «السماح بتصحيح USB؟»: اضغط «سماح»."), null);
        busy = true;
        new Thread(() -> {
            List<SetupEngine.AppResult> results = null;
            boolean route = true;
            sa.urtv.home.setup.SaverHold.begin(getApplicationContext());   // no screensaver over the question
            try (SetupSession s = SetupSession.openAny(this, 45_000, null, line -> { })) {
                sa.urtv.home.setup.SaverHold.end();
                results = s.engine.enableApps(pkgs);
                android.util.Log.i("UrTVFactory", "disabled apps: asked " + pkgs.size() + ", on again " + countOk(results));
            } catch (IOException e) {
                sa.urtv.home.setup.SaverHold.end();
                route = false;
                android.util.Log.w("UrTVFactory", "disabled apps: no route to the TV: " + e.getMessage());
            }
            final List<SetupEngine.AppResult> res = results;
            final boolean reached = route;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                if (!reached || res == null) noRoute(pkgs); else result(res);
            });
        }, "disabled-apps").start();
    }

    private static int countOk(List<SetupEngine.AppResult> r) {
        int n = 0;
        for (SetupEngine.AppResult a : r) if (a.ok) n++;
        return n;
    }

    /** every app: «اشتغل», or «ما اشتغل» and why; the ones that did not can be tried again */
    private void result(List<SetupEngine.AppResult> res) {
        col.removeAllViews();
        int ok = countOk(res);
        col.addView(text(ok == res.size() ? S.t("اشتغلت كلها") : S.t("اشتغل %s من %s", String.valueOf(ok), String.valueOf(res.size())), 30, "Medium", look.text), lp(dp(12)));
        final List<String> failed = new ArrayList<>();
        for (SetupEngine.AppResult a : res) {
            String name = a.pkg;
            for (DisabledApps.Row r : rows) if (r.pkg.equals(a.pkg)) name = r.name;
            col.addView(text((a.ok ? "✓  " : "✗  ") + name + "  " + (a.ok ? S.t("اشتغل") : S.t("ما اشتغل") + ": " + S.tv(a.why)), 22, "Regular", a.ok ? look.text : look.warn), lp(dp(8)));
            if (!a.ok) failed.add(a.pkg);
        }
        LinearLayout row = row();
        View done = button(S.t("تمام"), v -> finish());
        if (!failed.isEmpty()) {
            row.addView(done, bw(false));
            row.addView(button(S.t("جرّب اللي ما اشتغل من جديد"), v -> run(failed)), bw(true));
        } else {
            row.addView(done, bw(false));
        }
        focus(done);
    }

    /** no route to the TV: short, and it points to the pairing steps */
    private void noRoute(final List<String> pkgs) {
        col.removeAllViews();
        col.addView(text(S.t("ما قدرنا نوصل للتلفزيون"), 30, "Medium", look.text), lp(dp(12)));
        col.addView(text(S.t("عشان نشغّل التطبيقات لازم UrTV يوصل لتلفزيونك من الداخل. افتح الإعدادات ثم الشاشة الرئيسية ثم «تخلّص من واجهة المصنع»، وكمّل خطوات الإقران، وبعدها ارجع هنا. ما تغيّر شيء."), 22, "Regular", look.sub), lp(dp(16)));
        LinearLayout row = row();
        View back = button(S.t("رجوع"), v -> list());
        row.addView(back, bw(false));
        row.addView(button(S.t("جرّب من جديد"), v -> run(pkgs)), bw(true));
        focus(back);
    }

    // ---------------------------------------------------------------- pieces

    private void message(String title, String body, String[] buttons) {
        col.removeAllViews();
        col.addView(text(title, 30, "Medium", look.text), lp(dp(12)));
        if (!body.isEmpty()) col.addView(text(body, 20, "Regular", look.sub), lp(dp(16)));
    }

    private void focus(final View v) { v.post(v::requestFocus); }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(Dir.layout());
        LinearLayout.LayoutParams p = lp(0);
        p.topMargin = dp(8);
        col.addView(r, p);
        return r;
    }

    private TextView button(String label, View.OnClickListener click) {
        TextView t = text(label, 22, "Medium", look.text);
        t.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        t.setPadding(dp(24), dp(16), dp(24), dp(16));
        t.setFocusable(true);
        t.setClickable(true);
        t.setId(View.generateViewId());
        t.setOnClickListener(click);
        GlassPaint.card(t, look, 14);
        return t;
    }

    private LinearLayout.LayoutParams bw(boolean gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        if (gap) p.setMarginStart(dp(16));
        return p;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        t.setLineSpacing(dp(4), 1f);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); } catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams lp(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
