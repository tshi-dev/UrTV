package sa.urtv.home.safe;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import sa.urtv.home.SetupGate;
import sa.urtv.home.phone.QrCode;
import sa.urtv.home.phone.QrView;
import sa.urtv.home.setup.RestoreHome;

/**
 * «الوضع الآمن»: a second, small Home of this app, in its own process (android:process=":safe" in the manifest), so a crash or a hang of the home
 * screen (MainActivity) cannot take it down. Its HOME filter has a LOWER priority than MainActivity's, so Android never picks it over the home
 * screen by itself, and never shows a chooser between the two (different priorities). It is opened by the crash-loop guard (CrashGuard: the home
 * screen stopped or hung CrashLoop.LIMIT times in two minutes) or, should a firmware hand Home to it with nothing wrong, it passes Home on to the
 * home screen at once. No WebView, no network, no photo, no font file: system text and plain views, big buttons for the remote.
 */
public class SafeActivity extends Activity {
    public static final String EXTRA_LOOP = "sa.urtv.home.SAFE_LOOP";
    static final int BG = 0xFF101418, TEXT = 0xFFF2F2F2, DIM = 0xFFB8BEC6, GOLD = 0xFFE8C46A, RED = 0xFFFF8A80, PANEL = 0xFF222A33, INK = 0xFF101418;

    private LinearLayout col;
    private TextView status, log;
    private boolean sub;   // the apps list or the report is shown: Back returns to the buttons
    private volatile boolean restoring;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        CrashLoop loop = CrashGuard.read(this);
        Intent in = getIntent();
        boolean asked = in != null && in.getBooleanExtra(EXTRA_LOOP, false);
        boolean byHome = in != null && in.hasCategory(Intent.CATEGORY_HOME);
        if (byHome && !asked && !loop.tripped()) {   // Home came here with nothing wrong (a firmware's pick): the home screen is the Home
            openHome(false);
            return;
        }
        sa.urtv.home.setup.SetupHook.atHomeStart(this);   // a Home too: a home switch that was cut off is not forgotten here either (it never throws)
        showButtons(loop);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        CrashLoop loop = CrashGuard.read(this);
        if (intent.hasCategory(Intent.CATEGORY_HOME) && !intent.getBooleanExtra(EXTRA_LOOP, false) && !loop.tripped()) { openHome(false); return; }   // as in onCreate
        if (sub) showButtons(loop);
    }

    private void showButtons(CrashLoop loop) {
        sub = false;
        ScrollView scroll = page();
        title("الوضع الآمن");
        String what = loop.what();
        say(what.isEmpty() ? "شاشة احتياطية بسيطة لتلفزيونك: افتح منها ما تحتاج، أو رجّع واجهة التلفزيون الأصلية." : what, GOLD, 18);
        TextView first = button("افتح الشاشة الرئيسية من جديد", new View.OnClickListener() { public void onClick(View v) { openHome(true); } });
        button("كل التطبيقات", new View.OnClickListener() { public void onClick(View v) { showApps(); } });
        button("إعدادات التلفزيون", new View.OnClickListener() { public void onClick(View v) { openSettings(); } });
        TextView restore = button("رجّع واجهة التلفزيون الأصلية", new View.OnClickListener() { public void onClick(View v) { restore(); } });
        restore.setFilterTouchesWhenObscured(true);
        button("أرسل تقرير المشكلة", new View.OnClickListener() { public void onClick(View v) { showReport(); } });
        status = say("", GOLD, 17);
        log = say("", DIM, 14);
        setContentView(scroll);
        first.post(new Runnable() { @Override public void run() { first.requestFocus(); } });
    }

    // ---- the buttons

    /** «افتح الشاشة الرئيسية من جديد» (fresh): the troubles are forgotten and the home screen opens once without the redirect. */
    void openHome(boolean fresh) {
        if (fresh) CrashGuard.clear(this);
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName(getPackageName(), getPackageName() + ".MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (fresh) i.putExtra(CrashGuard.FROM_SAFE, true);
            startActivity(i);
            finish();
        } catch (RuntimeException e) {
            if (status != null) status.setText("ما فتحت الشاشة الرئيسية. جرّب «رجّع واجهة التلفزيون الأصلية».");
        }
    }

    void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            status.setText("ما فتحت إعدادات التلفزيون من هنا.");
        }
    }

    /** «رجّع واجهة التلفزيون الأصلية»: the same undo as «تراجع» in «إعداد تلفزيوني» (RestoreHome), behind the family PIN when one is set. */
    void restore() {
        if (restoring) return;
        if (!RestoreHome.debuggingOn(this)) {   // say how, before anything else
            status.setTextColor(RED);
            status.setText(RestoreHome.HOW_TO_DEBUG);
            return;
        }
        SetupGate.guard(this, "الوضع الآمن", new Runnable() { @Override public void run() { restoreAfterPin(); } }, new Runnable() { @Override public void run() { } });
    }

    private void restoreAfterPin() {
        if (restoring) return;
        restoring = true;
        status.setTextColor(GOLD);
        status.setText("جارٍ الرجوع إلى واجهة التلفزيون الأصلية… لا تطفئ التلفزيون.");
        log.setText("");
        new Thread(new Runnable() {
            @Override public void run() {
                final RestoreHome.Outcome o = RestoreHome.run(SafeActivity.this, new RestoreHome.Lines() {
                    @Override public void line(String t) { add(t); }
                    @Override public void waiting() { add("التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت."); }
                });
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        restoring = false;
                        status.setTextColor(o.ok ? GOLD : RED);
                        status.setText(o.ok ? o.message + " اضغط زر الرئيسية في الريموت." : o.message);
                    }
                });
            }
        }, "safe-restore").start();
    }

    private void add(final String t) {
        runOnUiThread(new Runnable() { @Override public void run() { if (log != null) log.append("• " + t + "\n"); } });
    }

    /** «كل التطبيقات»: every app with a TV or a phone launcher entry, by name, one button each. */
    void showApps() {
        sub = true;
        ScrollView scroll = page();
        title("كل التطبيقات");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> all = new ArrayList<>();
        for (String cat : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            try { all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)); } catch (RuntimeException ignored) { }
        }
        final List<String[]> apps = new ArrayList<>();   // label, package
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : all) {
            String p = r.activityInfo.packageName;
            if (p.equals(getPackageName()) || !seen.add(p)) continue;
            CharSequence l = r.loadLabel(pm);
            apps.add(new String[] {l == null ? p : l.toString(), p});
        }
        final Collator coll = Collator.getInstance(new Locale("ar"));
        Collections.sort(apps, new Comparator<String[]>() { @Override public int compare(String[] a, String[] b) { return coll.compare(a[0], b[0]); } });
        TextView first = null;
        for (final String[] a : apps) {
            TextView t = button(a[0], new View.OnClickListener() { public void onClick(View v) { launch(a[1]); } });
            if (first == null) first = t;
        }
        if (apps.isEmpty()) say("ما وجدنا تطبيقات.", DIM, 16);
        status = say("", GOLD, 16);
        log = null;
        setContentView(scroll);
        final TextView f = first;
        if (f != null) f.post(new Runnable() { @Override public void run() { f.requestFocus(); } });
    }

    private void launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        try {
            if (i == null) throw new IllegalStateException();
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            status.setText("ما فتح هذا التطبيق.");
        }
    }

    /** «أرسل تقرير المشكلة»: a QR code of a few lines for the docs/share page (SafeReport), and the GitHub address. */
    void showReport() {
        sub = true;
        ScrollView scroll = page();
        title("أرسل تقرير المشكلة");
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = sa.urtv.home.BuildConfig.MARKETING + "-" + (Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode);   // the name is MARKETING, the build the code
        } catch (Exception ignored) { }
        String url = SafeReport.url(SafeReport.lines(CrashGuard.read(this), Build.VERSION.SDK_INT, Build.VERSION.RELEASE, version, RestoreHome.switchState(this)));
        if (url != null) {
            say("امسح الرمز بكاميرا الجوال: تفتح صفحة فيها التقرير (لا اسم فيه ولا عنوان). لا يُرسل شيء حتى تضغط «أرسل» في الصفحة.", TEXT, 17);
            QrView qr = new QrView(this);
            qr.setCode(QrCode.encode(url));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(260), dp(260));
            p.gravity = Gravity.RIGHT;
            p.bottomMargin = dp(14);
            col.addView(qr, p);
        }
        say("أو اكتب لنا من أي جهاز: " + SafeReport.ISSUES, DIM, 16);
        TextView back = button("رجوع", new View.OnClickListener() { public void onClick(View v) { showButtons(CrashGuard.read(SafeActivity.this)); } });
        status = say("", GOLD, 16);
        log = null;
        setContentView(scroll);
        back.post(new Runnable() { @Override public void run() { back.requestFocus(); } });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (sub) showButtons(CrashGuard.read(this));   // a Home has nowhere to go back to: Back only leaves the list
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---- plain views

    private ScrollView page() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(96), dp(36), dp(96), dp(40));
        scroll.addView(col);
        return scroll;
    }

    private void title(String s) { say(s, TEXT, 28); }

    private TextView say(String s, int color, float sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        col.addView(t, p);
        return t;
    }

    /** a big button for the remote: a panel, and a solid gold fill with dark ink while it has the focus */
    private TextView button(String label, View.OnClickListener click) {
        final TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        t.setTextColor(TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(20), dp(16), dp(20), dp(16));
        t.setFocusable(true);
        t.setClickable(true);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, round(GOLD));
        bg.addState(new int[] {}, round(PANEL));
        t.setBackground(bg);
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) { t.setTextColor(has ? INK : TEXT); }
        });
        t.setOnClickListener(click);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        col.addView(t, p);
        return t;
    }

    private GradientDrawable round(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        return d;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
