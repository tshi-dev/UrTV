package sa.urtv.home.setup;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * «إنقاذ تلفزيونك» (UrTV Rescue, 4.3.3): one screen, one big button «رجّع التلفزيون كما كان». Over the TV's own adb (127.0.0.1:5555, «تصحيح USB» on; the TV asks once
 * «السماح بتصحيح USB؟»), it turns back on what our setup turned off (the home's ledger where the home still answers, and in any case the TV's own homes, the setup wizard
 * and the stock home's helpers), gives Home to the stock home, and says what it did (RescuePlan decides).
 * Nothing is removed or turned off. Works with no home at all: it is opened from the TV's apps list or from Downloader.
 */
public class RescueActivity extends Activity {
    private TextView state, button;
    /** where the build number is, per brand (the same lines as SetupActivity.DEV_HELP; tests/dev_help_test.py holds them equal) */
    static final String[] DEV_HELP = {
        "خيارات المطوّر ما تفتح؟ اضغط سبع مرات على رقم الإصدار، ومكانه حسب التلفزيون:",
        "TCL: الإعدادات ← النظام ← حول ← «إصدار نظام Android TV» (الأقدم: تفضيلات الجهاز ← حول ← Build)",
        "شاومي: الإعدادات ← تفضيلات الجهاز ← حول ← Build (بنظام Google TV: النظام ← حول ← «إصدار نظام Android TV»)",
        "سوني: الإعدادات ← النظام ← حول ← «إصدار نظام Android TV» (الأقدم: تفضيلات الجهاز ← حول ← Build)",
        "سكاي ورث: الإعدادات ← تفضيلات الجهاز ← حول ← Build (بنظام Google TV: النظام ← حول ← «إصدار نظام Android TV»)",
        "بعدها تلقى «خيارات المطوّر» تحت النظام أو تفضيلات الجهاز، وفيها «تصحيح USB».",
    };
    private volatile boolean working;

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

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView page = new ScrollView(this);
        page.setBackgroundColor(0xFF151619);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.setPadding(dp(48), dp(36), dp(48), dp(36));
        page.addView(col, new ScrollView.LayoutParams(-1, -2));
        col.addView(text("إنقاذ تلفزيونك", 28, 0xFFECEFF3, true));
        col.addView(text("يرجّع التلفزيون كما كان قبل UrTV: يشغّل الشاشة الأصلية ووسيط الإعداد وكل ما أوقفه إعدادنا، ويرجّع زر الرئيسية للشاشة الأصلية. ما يحذف شيئاً.", 17, 0xFFD6DAE0, false));
        col.addView(text("يحتاج «تصحيح USB» مفعّلاً (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB). لما يسألك التلفزيون «السماح بتصحيح USB؟» اختر «السماح دائماً».", 14, 0xFFD6AF6B, false));
        col.addView(text(String.join("\n", DEV_HELP), 14, 0xFFD6DAE0, false));   // where the build number is, per brand (the setup screen has the same lines)
        button = text("رجّع التلفزيون كما كان", 17, 0xFF151619, true);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setClickable(true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFF2F2F2);
        g.setCornerRadius(dp(12));
        button.setBackground(g);
        button.setOnFocusChangeListener((v, has) -> { g.setStroke(has ? dp(4) : 0, 0xFF7FB2FF); });
        button.setOnClickListener(v -> start());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(44));
        bp.topMargin = dp(16);
        col.addView(button, bp);
        state = text("", 17, 0xFFECEFF3, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(12);
        col.addView(state, sp);
        setContentView(page);
        button.post(button::requestFocus);
    }

    private void say(final String s) { runOnUiThread(() -> state.setText(s)); }

    private void start() {
        if (working) return;
        working = true;
        say("نتصل بالتلفزيون…");
        new Thread(this::rescue, "rescue").start();
    }

    private void rescue() {
        AdbClient adb = null;
        try {
            adb = AdbClient.connect("127.0.0.1", 5555, AdbKeys.load(this), Build.VERSION.SDK_INT, 180_000,
                () -> say("اختر «السماح دائماً» في رسالة التلفزيون «السماح بتصحيح USB؟»"), bytes -> Base64.encodeToString(bytes, Base64.NO_WRAP));
            Set<String> installed = RescuePlan.packages(adb.run("pm list packages; echo END_OF_LIST").output);
            Set<String> off = RescuePlan.packages(adb.run("pm list packages -d; echo END_OF_LIST").output);
            if (installed == null || off == null) { say("التلفزيون ما قال قائمة تطبيقاته، فما غيّرنا شيئاً. اضغط الزر مرة ثانية."); return; }
            List<String> ledger = RescuePlan.ledger(homeLedger());
            RescuePlan.Plan p = RescuePlan.plan(installed, off, ledger);
            for (String c : p.commands) adb.run(c);
            Set<String> after = RescuePlan.packages(adb.run("pm list packages -d; echo END_OF_LIST").output);
            StringBuilder said = new StringBuilder("تم.\n");
            int back = 0;
            for (String e : p.enabled) if (after == null || !after.contains(e)) back++;
            said.append(p.enabled.isEmpty() ? "ما كان شيء من تطبيقات التلفزيون موقوفاً.\n" : "شغّلنا " + back + " من " + p.enabled.size() + " تطبيقاً كان موقوفاً.\n");
            said.append(p.home.isEmpty() ? "ما عرفنا الشاشة الأصلية: اختر الشاشة الرئيسية من إعدادات التلفزيون ← التطبيقات.\n" : "زر الرئيسية يفتح الشاشة الأصلية الآن.\n");
            if (ledger == null) said.append("(ما ردّت الشاشة الرئيسية «تلفزيونك»، فشغّلنا ما نعرفه من تطبيقات التلفزيون الأساسية. لو بقي تطبيق موقوفاً شغّله من إعدادات التلفزيون ← التطبيقات.)\n");
            said.append("اضغط زر الرئيسية في الريموت.");
            say(said.toString());
        } catch (IOException e) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.startsWith("debugging is off")) say("«تصحيح USB» مطفأ. فعّله (الإعدادات ← تفضيلات الجهاز ← خيارات المطوّر ← تصحيح USB) ثم اضغط الزر مرة ثانية.");
            else if (m.contains("permission was not accepted")) say("ما قُبلت رسالة «السماح بتصحيح USB؟». اضغط الزر مرة ثانية واختر «السماح دائماً».");
            else if (m.contains("TLS")) say("هذا التلفزيون يطلب التصحيح اللاسلكي؛ فعّل «تصحيح USB» العادي ثم اضغط الزر مرة ثانية.");
            else say("انقطع الاتصال بالتلفزيون. اضغط الزر مرة ثانية.");
        } catch (RuntimeException e) {
            say("حدث خطأ؛ ما غيّرنا شيئاً بعده. اضغط الزر مرة ثانية.");
        } finally {
            if (adb != null) adb.close();
            working = false;
        }
    }

    /** what our setup turned off, asked of the home (our key only); null when the home is gone or did not answer */
    private String homeLedger() {
        try {
            Bundle b = getContentResolver().call(Uri.parse("content://" + RescuePlan.LEDGER_AUTHORITY), RescuePlan.LEDGER_CALL, null, null);
            return b == null ? null : b.getString(RescuePlan.LEDGER_RESULT);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
