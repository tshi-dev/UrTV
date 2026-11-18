package sa.urtv.home.setup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 4.4 (R7): the fallback home of «إنقاذ تلفزيونك» (RescueHome, the alias that opens this screen). The TV shows it when UrTV is gone and the stock home is off: three ways
 * out, and nothing else. «رجّع واجهة المصنع» opens the rescue's own screen (it turns the stock home back on over the TV's adb); «افتح الإعدادات» opens the TV's settings,
 * where the stock home is turned on by hand (the way is written here); and the TV's apps, so the TV can be used meanwhile. When a stock home answers Home again, the
 * alias turns itself off and Home goes to it.
 */
public class FallbackHomeActivity extends Activity {
    private LinearLayout col;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setLineSpacing(0, 1.25f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView button(String s, View.OnClickListener click) {
        TextView b = text(s, 17, 0xFF151619, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFF2F2F2);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        b.setOnFocusChangeListener((v, has) -> g.setStroke(has ? dp(4) : 0, 0xFF7FB2FF));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
        p.topMargin = dp(12);
        b.setLayoutParams(p);
        return b;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        GradientDrawable face = new GradientDrawable();
        face.setColor(0xFF1A1C20);
        face.setCornerRadius(dp(22));
        sa.urtv.home.Box page = sa.urtv.home.Box.make(this, face, 0xFFECEFF3, 0xFF9AA1AB, Typeface.DEFAULT_BOLD, View.LAYOUT_DIRECTION_RTL);
        page.page.setBackgroundColor(0xFF151619);
        page.title.setText("تلفزيونك بدون شاشة رئيسية");
        col = page.content;
        col.addView(text("الشاشة الرئيسية الأصلية لتلفزيونك موقوفة، وUrTV ما عاد موجوداً. هذي الشاشة الاحتياطية من «إنقاذ تلفزيونك» حتى ترجّعها.", 17, 0xFFD6DAE0, false));
        TextView first = button("رجّع واجهة المصنع", v -> open(new Intent(this, RescueActivity.class)));
        col.addView(first);
        col.addView(button("افتح الإعدادات", v -> open(new Intent(Settings.ACTION_SETTINGS))));
        col.addView(text("بدون كمبيوتر: الإعدادات ← التطبيقات ← كل التطبيقات (أو «إظهار تطبيقات النظام») ← الشاشة الرئيسية الأصلية (Google TV أو Android TV Home أو شاشة الشركة) ← تفعيل، ثم اضغط زر الرئيسية.", 14, 0xFFD6AF6B, false));
        if (ours() != null) col.addView(button("افتح UrTV", v -> open(ours())));
        List<Intent> apps = apps();
        if (!apps.isEmpty()) {
            TextView h = text("تطبيقات التلفزيون", 17, 0xFFECEFF3, true);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
            hp.topMargin = dp(16);
            col.addView(h, hp);
            for (Intent i : apps) col.addView(button(i.getStringExtra(LABEL), v -> open(i)));
        }
        setContentView(page.page);
        page.focusFirst(first);
    }

    @Override protected void onResume() {
        super.onResume();
        // a stock home answers Home again (turned on by hand, or by «رجّع واجهة المصنع»): this screen steps aside for good
        if (RescueHome.stockHomeOn(homePackages())) {
            try {
                getPackageManager().setComponentEnabledSetting(new ComponentName(this, RescueHome.ALIAS_CLASS),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            } catch (RuntimeException ignored) {
                // stays as the fallback until the next time
            }
        }
    }

    @Override public void onBackPressed() { }   // a home: Back goes nowhere

    private void open(Intent i) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (RuntimeException ignored) { }
    }

    private List<String> homePackages() {
        List<String> out = new ArrayList<>();
        for (ResolveInfo r : getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0))
            if (r.activityInfo != null) out.add(r.activityInfo.packageName);
        return out;
    }

    private Intent ours() {
        Intent i = getPackageManager().getLaunchIntentForPackage("sa.urtv.home");
        return i;
    }

    private static final String LABEL = "sa.urtv.rescue.LABEL";

    /** the TV's apps (Android TV's row, then the plain launcher's), by name; never this app */
    private List<Intent> apps() {
        PackageManager pm = getPackageManager();
        Set<String> seen = new TreeSet<>();
        List<Intent> out = new ArrayList<>();
        for (String cat : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat).setClassName(pkg, r.activityInfo.name);
                i.putExtra(LABEL, String.valueOf(r.loadLabel(pm)));
                out.add(i);
            }
        }
        java.util.Collections.sort(out, (a, c) -> a.getStringExtra(LABEL).compareToIgnoreCase(c.getStringExtra(LABEL)));   // (List.sort is Android 7)
        return out;
    }
}
