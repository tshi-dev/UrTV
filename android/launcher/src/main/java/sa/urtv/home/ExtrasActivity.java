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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 4.3.3 «إضافات تلفزيونك» (UrTV Extras): the offer (docs/mockups/433-urtv-plus-offer.png, approved with this name). It opens only when the person asked
 * for a feature that needs the add-on and the add-on cannot act: the end of «مؤقت النوم» or «أطفئ الآن» (the screen off), the phone's keys or typing in
 * another app. It says what the add-on does, why Play Protect may warn («يطلب بيانات حساسة»: an accessibility service) and that it reads nothing it keeps;
 * then one button: «ثبّت الإضافة» (our release file, checked: our package, our key, then Android's installer), or once installed «شغّلها» (the TV's
 * accessibility settings, where the person turns «إضافات تلفزيونك» on). «ليس الآن» closes it; UrTV works without it.
 */
public class ExtrasActivity extends Activity {
    private static final String TAG = "UrTVExtras", RESULT = "sa.urtv.home.EXTRAS_INSTALL_RESULT";
    /** what asked: "sleep", "type", "keys", "overlay" («فوق تطبيق آخر»: the notice over another app) (else the plain line) */
    static final String EXTRA_FOR = "sa.urtv.home.EXTRAS_FOR";

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private TextView state, button;
    private boolean healTried;
    /** 4.4: the one fix when the add-on is turned on in the TV's list but Android does not run it */
    static final String STUCK_FIX = S.n("«إضافات تلفزيونك» مفعّلة لكن التلفزيون ما شغّلها: أطفئها وشغّلها من جديد في «تسهيل الاستخدام»");
    private boolean registered, working, askedSource, sentToSettings;
    /** «فوق تطبيق آخر» asked for the add-on: what matters is its «الظهور فوق التطبيقات» permission, not its accessibility service; the answer of the last ask (a thread) */
    private boolean forOverlay;
    private volatile boolean canDraw;

    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent screen = i.getParcelableExtra(Intent.EXTRA_INTENT);
                try {
                    if (screen != null) { startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); say(S.t("وافق على التثبيت في الشاشة التي ظهرت، ولو قال Play Protect شيئاً اضغط «تثبيت على أي حال»")); return; }
                } catch (RuntimeException e) { Log.w(TAG, "confirm: " + e.getClass().getSimpleName()); }
                failed();
                return;
            }
            if (status == PackageInstaller.STATUS_SUCCESS) { working = false; runOnUiThread(ExtrasActivity.this::refresh); return; }
            failed();
        }
    };

    private TextView text(String s, int sp, int color, String font, int gravity) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(gravity);
        t.setLineSpacing(0, 1.25f);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/" + font + ".otf")); } catch (RuntimeException ignored) { }
        return t;
    }

    private static GradientDrawable box(int fill, int line, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (line != 0) g.setStroke(2, line);
        return g;
    }

    /** the line that names what asked for the add-on */
    static String lead(String what) {
        String f = "sleep".equals(what) ? S.t("«إطفاء الشاشة»") : "type".equals(what) ? S.t("«الكتابة من الجوال»") : "keys".equals(what) ? S.t("«أزرار الجوال داخل التطبيقات»") : "overlay".equals(what) ? S.t("«فوق تطبيق آخر»") : S.t("هذه الميزة");
        return S.t("%s تحتاج إضافة صغيرة منّا اسمها «إضافات تلفزيونك». فيها أربع ميزات:", f);
    }

    private Typeface font(String f) { try { return Typeface.createFromAsset(getAssets(), "fonts/" + f + ".otf"); } catch (RuntimeException e) { return null; } }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        int start = Dir.start();
        // 4.4 (the owner: «ONE BOX for everything»): the same box as the setup's (Box), the offer's words scrolling inside it, read before the buttons
        Box page = Box.make(this, box(0xFF1A1C20, 0xFF3A3D44, dp(22)), 0xFFECEFF3, 0xFF9AA1AB, font("Thmanyah-Medium"), Dir.layout());
        page.page.setBackgroundColor(0xF0151619);
        page.title.setText(S.t("إضافات تلفزيونك"));
        LinearLayout col = page.content;
        forOverlay = getIntent() != null && "overlay".equals(getIntent().getStringExtra(EXTRA_FOR));
        col.addView(text(lead(getIntent() == null ? null : getIntent().getStringExtra(EXTRA_FOR)), 17, 0xFFECEFF3, "Thmanyah-Medium", start), lp(10));
        col.addView(text(S.t("١. زر الرئيسية يرجّعك لتلفزيونك لو فتح التلفزيون شاشته الأصلية (أجهزة Google TV).") + "\n"
            + S.t("٢. الكتابة من الجوال داخل التطبيقات الثانية، ومؤقت النوم يطفي الشاشة.") + "\n"
            + S.t("٣. زر الإعدادات في الريموت يشتغل وأنت تشاهد فيديو HDR.") + "\n"
            + S.t("٤. تنبيه المباراة والإقامة يظهر فوق التطبيق الذي تشاهده."), 17, 0xFFD6DAE0, "Thmanyah-Regular", start), lp(10));
        TextView warn = text(S.t("عند التثبيت ممكن يقول Play Protect إن التطبيق «يطلب بيانات حساسة». هذا لأن الإضافة تستخدم «تسهيل الاستخدام» لتعرف أي تطبيق قدامك فقط: ما تقرأ اللي تكتبه ولا ترسل شيئاً. اضغط «تثبيت على أي حال»."),
            17, 0xFFD6DAE0, "Thmanyah-Regular", start);
        warn.setBackground(box(0xFF1C1E22, 0xFF3A3D44, dp(16)));
        warn.setPadding(dp(16), dp(10), dp(16), dp(10));
        col.addView(warn, lp(16));
        col.addView(text(S.t("بعد التثبيت نفتح لك «تسهيل الاستخدام» لتشغّل «إضافات تلفزيونك». تقدر تحذفها متى ما تبي، وتلفزيونك يشتغل بدونها."), 17, 0xFF9AA1AB, "Thmanyah-Regular", start), lp(10));
        state = text("", 17, 0xFFD6AF6B, "Thmanyah-Regular", start);
        col.addView(state, lp(10));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        button = text(S.t("ثبّت الإضافة"), 17, 0xFF151619, "Thmanyah-Medium", Gravity.CENTER);
        TextView later = text(S.t("ليس الآن"), 17, 0xFFECEFF3, "Thmanyah-Medium", Gravity.CENTER);
        for (TextView t : new TextView[] {button, later}) {
            t.setFocusable(true);
            t.setClickable(true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(Compact.BUTTON_DP), 1f);   // 4.4 compact: 44 dp
            p.setMargins(dp(8), 0, dp(8), 0);
            row.addView(t, p);
        }
        button.setOnFocusChangeListener((v, has) -> button.setBackground(box(0xFFF2F2F2, has ? 0xFF7FB2FF : 0, dp(16))));
        later.setOnFocusChangeListener((v, has) -> later.setBackground(box(has ? 0xFF2A2D33 : 0x00000000, has ? 0xFF7FB2FF : 0xFF3A3D44, dp(16))));
        button.setBackground(box(0xFFF2F2F2, 0, dp(16)));
        later.setBackground(box(0x00000000, 0xFF3A3D44, dp(16)));
        button.setOnClickListener(v -> act());
        later.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(8);
        col.addView(row, rp);
        setContentView(page.page);
        page.focusFirst(button);
        InternalReceivers.register(this, result, new IntentFilter(RESULT));
        registered = true;
    }

    private LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        return p;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void say(String s) { runOnUiThread(() -> state.setText(s)); }

    private void failed() { working = false; runOnUiThread(() -> { state.setText(S.t("ما قدرنا نثبّتها. جرّب مرة ثانية")); button.setText(S.t("جرّب مرة ثانية")); }); }

    @Override protected void onResume() {
        super.onResume();
        refresh();
        // 4.4 review F9: the kept answer can be old (the person just turned it on in Android's settings): ask on a thread, and look again on the UI thread when it answers
        new Thread(() -> { Extras.askNow(); runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) refresh(); }); }, "extras-offer").start();
        if (forOverlay) new Thread(() -> { canDraw = Extras.canDraw(); runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) refresh(); }); }, "extras-overlay").start();   // the add-on's «الظهور فوق التطبيقات», on a thread
        if (askedSource && !working && canInstall()) { askedSource = false; install(); }   // back from Android's «allow this source» screen
    }

    /** the button says what is next: install, or turn the service on; once it runs, the screen closes */
    private void refresh() {
        if (working) return;
        if (!Extras.installed(this)) { button.setText(S.t("ثبّت الإضافة")); if (forOverlay) state.setText(S.t("«فوق تطبيق آخر» يحتاج «إضافات تلفزيونك» وهي غير مثبّتة")); return; }
        if (forOverlay) {   // the notice needs the add-on's «الظهور فوق التطبيقات», not its accessibility service
            if (canDraw) {
                android.widget.Toast.makeText(this, S.t("«فوق تطبيق آخر» جاهز"), android.widget.Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            button.setText(S.t("اسمح بالظهور فوق التطبيقات"));
            state.setText(S.t("الإضافة مثبّتة: بقي السماح لها بالظهور فوق التطبيقات"));
            return;
        }
        if (Extras.enabled()) {
            android.widget.Toast.makeText(this, S.t("«إضافات تلفزيونك» شغّالة"), android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        button.setText(S.t("شغّلها في «تسهيل الاستخدام»"));
        if (A11yHeal.listed(this)) {   // 4.4: turned on, but Android does not run it (a TCL refused the first bind; a force-stop): the one fix, after our own try where we may
            if (!healTried && A11yHeal.canHeal(this)) { healTried = true; A11yHeal.check(getApplicationContext(), "fix-offer"); state.postDelayed(this::refresh, 4000); }
            state.setText(S.tv(STUCK_FIX));
            return;
        }
        state.setText(sentToSettings ? S.t("اختر «إضافات تلفزيونك» وشغّلها، ثم ارجع") : S.t("الإضافة مثبّتة: بقي تشغيلها"));
    }

    private void act() {
        if (working) return;
        if (!Extras.installed(this)) { install(); return; }
        if (Parental.needsPin(this, ParentalPolicy.targets(ParentalPolicy.SLEEP_ACCESSIBILITY, null))) { say(S.t("إعدادات التلفزيون مقفولة بالرقم السري: افتحها من «مركز التحكم» واختر «إضافات تلفزيونك»")); return; }
        if (forOverlay) { allowOverlay(); return; }
        try {
            sentToSettings = true;
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            android.widget.Toast.makeText(this, S.t("فعّل «إضافات تلفزيونك»"), android.widget.Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            say(S.t("افتح إعدادات التلفزيون ← تسهيل الاستخدام، وشغّل «إضافات تلفزيونك»"));
        }
    }

    /** the TV's own «الظهور فوق التطبيقات» screen for the add-on; a TV that hides it: the computer's command, said once */
    private void allowOverlay() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ExtrasWords.PACKAGE));
        try {
            if (getPackageManager().resolveActivity(i, 0) != null) { startActivity(i); return; }
        } catch (RuntimeException ignored) { }
        say(S.t("هذا التلفزيون يخفي هذا الإذن، فيُعطى من الكمبيوتر"));
    }

    private boolean canInstall() { return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls(); }

    private void install() {
        if (!canInstall()) {
            askedSource = true;
            say(S.t("اسمح لـ UrTV بالتثبيت في الشاشة التي ظهرت، ثم ارجع"));
            try { startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); return; }
            catch (RuntimeException e) { askedSource = false; }
        }
        working = true;
        say(S.t("ينزّل…"));
        new Thread(() -> {
            File f = new File(getCacheDir(), "extras-install.apk");
            try {
                String sha = MirrorInstall.download(ExtrasWords.URL, f);
                if (sha == null) { working = false; say(S.t("ما قدرنا ننزّلها. جرّب مرة ثانية")); return; }
                int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
                PackageInfo info = getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), flags);
                Set<String> signers = new HashSet<>();
                if (info != null) {
                    android.content.pm.Signature[] sigs = null;
                    if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) sigs = info.signingInfo.getApkContentsSigners();
                    if ((sigs == null || sigs.length == 0) && info.signatures != null) sigs = info.signatures;
                    if (sigs != null) for (android.content.pm.Signature s : sigs) signers.add(SuggestedApps.hex(java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
                }
                String no = SaverInstallRule.verify(ExtrasWords.PACKAGE, info == null ? null : info.packageName, MirrorInstall.signers(getPackageManager(), getPackageName()), signers);
                if (no != null) { f.delete(); working = false; say("other_key".equals(no) ? S.t("الملف ليس موقّعًا بمفتاح UrTV، فما ثبّتناه") : S.t("الملف ليس «إضافات تلفزيونك»، فما ثبّتناه")); return; }
                say(S.t("يثبّت…"));
                MirrorInstall.commit(this, f, ExtrasWords.PACKAGE, RESULT);
            } catch (Exception e) {
                Log.w(TAG, "install: " + e.getClass().getSimpleName());
                f.delete();
                failed();
            }
        }, "extras-install").start();
    }

    @Override protected void onDestroy() {
        if (registered) try { unregisterReceiver(result); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }
}
