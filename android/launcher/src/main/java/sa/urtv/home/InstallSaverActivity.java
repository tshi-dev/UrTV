package sa.urtv.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 4.3.2 «ثبّت الجزء الثاني»: what «تلفزيونك», «المدينة», «فرقي» and the settings open when the screensaver app (the second app, which holds the
 * settings) is not on the TV (SaverInstallRule): one screen that says so, a button that downloads our release file and hands it to Android's
 * installer (REQUEST_INSTALL_PACKAGES: the first time, Android's «allow this source» screen comes first), and, for a TV that cannot download
 * it, Downloader's code. Only our own file is installed: the package and the signing key are checked first (SaverInstallRule.verify).
 */
public class InstallSaverActivity extends Activity {
    private static final String TAG = "UrTVSaverInstall", RESULT = "sa.urtv.home.SAVER_INSTALL_RESULT";

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private TextView state;
    private TextView button;
    private boolean registered, working, askedSource;

    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {   // Android's own install screen: handed over
                Intent screen = i.getParcelableExtra(Intent.EXTRA_INTENT);
                try {
                    if (screen != null) { startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); say(S.t("وافق على التثبيت في الشاشة التي ظهرت")); return; }
                } catch (RuntimeException e) { Log.w(TAG, "confirm: " + e.getClass().getSimpleName()); }
                failed(S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
                return;
            }
            if (status == PackageInstaller.STATUS_SUCCESS) { done(); return; }
            failed(S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
        }
    };

    private TextView text(String s, int sp, int color, String font) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/" + font + ".otf")); } catch (RuntimeException ignored) { }
        return t;
    }

    private Typeface font(String f) { try { return Typeface.createFromAsset(getAssets(), "fonts/" + f + ".otf"); } catch (RuntimeException e) { return null; } }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        // 4.4 (the owner: «ONE BOX for everything»): the same box as the setup's (Box), in the 5 % safe area
        android.graphics.drawable.GradientDrawable face = new android.graphics.drawable.GradientDrawable();
        face.setColor(0xFF1A1C20);
        face.setCornerRadius(dp(22));
        Box box = Box.make(this, face, 0xFFFFFFFF, 0x99FFFFFF, font("Thmanyah-Medium"), Dir.layout());
        box.page.setBackgroundColor(0xF0101216);
        box.title.setText(S.t("ثبّت الجزء الثاني"));
        LinearLayout col = box.content;
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView why = text(S.t("الإعدادات والمدينة والفرق وشاشة التوقف في الجزء الثاني من UrTV، وهو غير مثبّت على هذا التلفزيون."), 17, 0xCCFFFFFF, "Thmanyah-Regular");
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, -2);
        wp.topMargin = dp(16);
        col.addView(why, wp);
        button = text(S.t("نزّل وثبّت"), 17, 0xFF101216, "Thmanyah-Medium");
        button.setBackgroundColor(0xFFD6AF6B);
        button.setPadding(dp(32), dp(8), dp(32), dp(8));
        button.setMinHeight(dp(Compact.BUTTON_DP));   // 4.4 compact: 44 dp
        button.setFocusable(true);
        button.setClickable(true);
        button.setOnClickListener(v -> start());
        button.setOnFocusChangeListener((v, has) -> button.setBackgroundColor(has ? 0xFFFFFFFF : 0xFFD6AF6B));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.topMargin = dp(20);
        col.addView(button, bp);
        state = text("", 17, 0xFFD6AF6B, "Thmanyah-Regular");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.topMargin = dp(16);
        col.addView(state, sp);
        LinearLayout.LayoutParams dp0 = new LinearLayout.LayoutParams(-2, -2);
        dp0.topMargin = dp(20);
        col.addView(text(S.t("أو من تطبيق Downloader: اكتب الكود"), 17, 0x99FFFFFF, "Thmanyah-Regular"), dp0);
        TextView code = text(SaverInstallRule.DOWNLOADER_CODE, 22, 0xFFFFFFFF, "Thmanyah-Medium");   // Latin digits, as Downloader asks them
        col.addView(code);
        setContentView(box.page);
        box.focusFirst(button);
        InternalReceivers.register(this, result, new IntentFilter(RESULT));
        registered = true;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void say(String s) { runOnUiThread(() -> state.setText(s)); }

    private void failed(String s) { working = false; runOnUiThread(() -> { state.setText(s); button.setText(S.t("جرّب مرة ثانية")); }); }

    /** installed (this app sees it): back to what asked for it */
    private void done() {
        working = false;
        say(S.t("تم التثبيت. افتح الإعدادات من جديد"));
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> finish(), 1800);
    }

    @Override protected void onResume() {
        super.onResume();
        if (installed()) { finish(); return; }
        if (askedSource && !working && canInstall()) { askedSource = false; start(); }   // back from Android's «allow this source» screen
    }

    private boolean installed() {
        try { getPackageManager().getPackageInfo(SaverInstallRule.PACKAGE, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private boolean canInstall() { return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls(); }

    private void start() {
        if (working) return;
        if (!canInstall()) {   // Android asks the person once to allow this app to install: the screen of that setting, then back here
            askedSource = true;
            say(S.t("اسمح لـ UrTV بالتثبيت في الشاشة التي ظهرت، ثم ارجع"));
            try { startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); return; }
            catch (RuntimeException e) { askedSource = false; /* the installer's own screen asks instead */ }
        }
        working = true;
        say(S.t("ينزّل…"));
        new Thread(() -> {
            File f = new File(getCacheDir(), "saver-install.apk");
            try {
                String sha = MirrorInstall.download(SaverInstallRule.URL, f);
                if (sha == null) { failed(S.t("ما قدرنا ننزّله. جرّب مرة ثانية")); return; }
                int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
                PackageInfo info = getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), flags);
                Set<String> signers = new HashSet<>();
                if (info != null) {
                    android.content.pm.Signature[] sigs = null;
                    if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) sigs = info.signingInfo.getApkContentsSigners();
                    if ((sigs == null || sigs.length == 0) && info.signatures != null) sigs = info.signatures;
                    if (sigs != null) for (android.content.pm.Signature s : sigs) signers.add(SuggestedApps.hex(java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
                }
                String no = SaverInstallRule.verify(info == null ? null : info.packageName, MirrorInstall.signers(getPackageManager(), getPackageName()), signers);
                if (no != null) { f.delete(); failed(SaverInstallRule.refusal(no)); return; }
                say(S.t("يثبّت…"));
                MirrorInstall.commit(this, f, SaverInstallRule.PACKAGE, RESULT);
            } catch (Exception e) {
                Log.w(TAG, "install: " + e.getClass().getSimpleName());
                f.delete();
                failed(S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
            }
        }, "saver-install").start();
    }

    @Override protected void onDestroy() {
        if (registered) try { unregisterReceiver(result); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }
}
