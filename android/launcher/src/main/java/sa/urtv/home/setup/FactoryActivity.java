package sa.urtv.home.setup;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sa.urtv.home.Dir;
import sa.urtv.home.GlassPaint;
import sa.urtv.home.GlassSource;
import sa.urtv.home.GlassStyle;
import sa.urtv.home.PinGuard;
import sa.urtv.home.S;

/**
 * «تخلّص من واجهة المصنع»: the second step of «UrTV النظيف», offered only from inside it. The screen first checks this TV (SetupEngine.factoryOffer: it refuses with the reason where the
 * setup wizard owns the Wi-Fi, where the safety switch cannot be set, where debugging is off), then lists EXACTLY what it will turn off, by readable name, says «تعطيل لا حذف», what stays
 * protected and how it is undone, and asks twice: a first screen and a «متأكد؟» one, each with the focus on «إلغاء», and the family PIN when one is set. Nothing is changed before the second
 * «نعم». When it is on, the same screen shows the ledger and «رجّع واجهة المصنع» (one press, the PIN when set). It never uninstalls and never touches the protected features.
 */
public class FactoryActivity extends Activity {
    /** Home pressed: the screen is done, not left behind the home (step 47: a clean-mode screen came back to the front when Settings opened later) */
    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish();
    }

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private GlassStyle.Look look;
    private LinearLayout col;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private SetupEngine.FactoryOffer offer;
    private boolean busy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        frame.addView(col, new FrameLayout.LayoutParams(Math.min(dp(980), w - 2 * Math.round(w * 0.05f)), -2, Gravity.CENTER));
        setContentView(scroll);
        if (new FileStore(this).get(SetupEngine.FACTORY) != null && new FileStore(this).get(SetupEngine.FACTORY).startsWith("on")) showOn();
        else intro();
    }

    /** Back always leaves (a check or a step in progress goes on by itself on its thread; the ledger and the safety switch do not depend on this screen) */
    @Override public void onBackPressed() { finish(); }

    // ---------------------------------------------------------------- the first page

    /** one OK on the settings' row lands here, not on a check: a plain description and two buttons, the focus on «رجوع»; nothing starts until «ابدأ» */
    private void intro() {
        col.removeAllViews();
        col.addView(text(S.t("تخلّص من واجهة المصنع"), 30, "Medium", look.text), lp(dp(12)));
        col.addView(text(S.t("هذي الخطوة تعطّل (ما تحذف) تطبيقات واجهة المصنع في تلفزيونك، ويصير UrTV هو الأساس."), 20, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("أول شيء نفحص التلفزيون ونعرض لك بالضبط وش بيتعطّل، وما نغيّر شيء قبل ما تتأكد مرتين."), 20, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("قد يظهر على الشاشة سؤال «السماح بتصحيح USB؟»: اضغط «سماح»."), 20, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("وتقدر ترجّع كل شيء بضغطة وحدة."), 20, "Regular", look.sub), lp(dp(16)));
        LinearLayout row = row();
        View back = button(S.t("رجوع"), v -> finish());
        row.addView(back, bw(false));
        row.addView(button(S.t("ابدأ"), v -> check()), bw(true));
        focus(back);   // the safe press
    }

    // ---------------------------------------------------------------- the check

    /** this TV is read over its own ADB (nothing is changed): the plan, or the reason it is refused */
    private void check() {
        message(S.t("نفحص هذا التلفزيون…"), S.t("قد يظهر على الشاشة سؤال «السماح بتصحيح USB؟»: اضغط «سماح»."), null);
        busy = true;
        new Thread(() -> {
            SetupEngine.FactoryOffer o;
            try (SetupSession s = SetupSession.open(this, 60_000, () -> ui.post(() -> { if (!isFinishing() && !isDestroyed()) message(S.t("نفحص هذا التلفزيون…"), S.t("اضغط «سماح» في سؤال تصحيح USB على الشاشة."), null); }), line -> { })) {
                o = s.engine.factoryOffer();
            } catch (Exception e) {
                String why = String.valueOf(e.getMessage());
                o = new SetupEngine.FactoryOffer(why.contains("debugging is off") ? S.t("تصحيح USB مطفأ في هذا التلفزيون، وهذه الخطوة تحتاجه (وتحتاجه الاستعادة). فعّله من إعدادات المطوّرين ثم جرّب.")
                        : S.t("ما قدرنا نكلّم التلفزيون: تأكد من تفعيل تصحيح USB ومن «السماح» ثم جرّب."), "", new ArrayList<String>());
            }
            final SetupEngine.FactoryOffer res = o;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; offer = res; if (res.refusal != null) refused(res.refusal); else plan(); });
        }, "factory-check").start();
    }

    private void refused(String reason) {
        message(S.t("ما نقدر نسوّيها هنا"), S.tv(reason), new String[] {S.t("رجوع")});
    }

    // ---------------------------------------------------------------- what it will do

    private void plan() {
        col.removeAllViews();
        col.addView(text(S.t("تخلّص من واجهة المصنع"), 30, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("تعطيل لا حذف: ما يُحذف شيء، وكل ما نعطّله يرجع بضغطة وحدة."), 20, "Medium", look.accent), lp(dp(12)));
        col.addView(text(S.t("هذا اللي بنعطّله في هذا التلفزيون بالضبط:"), 20, "Regular", look.sub), lp(dp(8)));
        col.addView(text(names(offer.packages), 18, "Regular", look.text), lp(dp(12)));
        col.addView(text(S.t("يبقى محميًّا ولا نلمسه: الواي فاي والبلوتوث والصوت والبث (Chromecast وAirPlay) ومداخل HDMI ولوحات المفاتيح وكل ما في قائمة الخصائص المحمية."), 18, "Regular", look.sub), lp(dp(12)));
        col.addView(text(S.t("للتراجع: «رجّع واجهة المصنع» في هذي الشاشة نفسها، ويرجع تلقائيًا لو توقف تلفزيونك مرتين عند التشغيل. اترك تصحيح USB شغّالًا ما دام هذا مفعّلًا."), 18, "Regular", look.sub), lp(dp(16)));
        LinearLayout row = row();
        View cancel = button(S.t("إلغاء"), v -> finish());
        row.addView(cancel, bw(false));
        row.addView(button(S.t("متابعة"), v -> sure()), bw(true));
        focus(cancel);   // the focus starts on «إلغاء»
    }

    /** the second step: «متأكد؟», again with the focus on «إلغاء»; only «نعم، عطّل» goes on (with the PIN when one is set) */
    private void sure() {
        col.removeAllViews();
        col.addView(text(S.t("متأكد؟"), 34, "Medium", look.warn), lp(dp(12)));
        col.addView(text(S.t("بنعطّل %s من تطبيقات واجهة المصنع في هذا التلفزيون ونفتح على تلفزيونك مباشرة. تقدر ترجّعها بضغطة وحدة.", Integer.toString(offer.packages.size())), 22, "Regular", look.text), lp(dp(16)));
        LinearLayout row = row();
        View cancel = button(S.t("إلغاء"), v -> finish());
        row.addView(cancel, bw(false));
        row.addView(button(S.t("نعم، عطّل"), v -> pin(this::run)), bw(true));
        focus(cancel);
    }

    private void run() {
        message(S.t("نعطّل واجهة المصنع…"), S.t("لا تطفئ التلفزيون. الشاشة الأصلية سترجع وحدها لو ما نجح شيء."), null);
        busy = true;
        new Thread(() -> {
            SetupEngine.Result r;
            try (SetupSession s = SetupSession.open(this, 60_000, null, line -> { })) {
                r = s.engine.factoryOff();
                if (r.status == SetupEngine.Status.DONE) new FileStore(this).put(BootGuard.FILE, BootGuard.fresh(System.currentTimeMillis()).text());
            } catch (Exception e) {
                r = new SetupEngine.Result(SetupEngine.Status.FAILED, S.t("انقطع الاتصال بالتلفزيون أثناء التعطيل. «رجّع واجهة المصنع» يرجّع كل ما تغيّر."));
            }
            final SetupEngine.Result res = r;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; if (res.status == SetupEngine.Status.DONE) showOn(); else message(S.t("ما اكتمل"), S.tv(res.message), new String[] {S.t("رجوع")}); });
        }, "factory-off").start();
    }

    // ---------------------------------------------------------------- when it is on

    private void showOn() {
        col.removeAllViews();
        col.addView(text(S.t("واجهة المصنع معطّلة"), 30, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("اللي عطّلناه (تعطيل لا حذف):"), 20, "Regular", look.sub), lp(dp(8)));
        List<String> ledger = SetupEngine.ledgerNames(new FileStore(this).get("disabled-by-us.txt"));
        col.addView(text(names(ledger), 18, "Regular", look.text), lp(dp(16)));
        LinearLayout row = row();
        View back = button(S.t("رجّع واجهة المصنع"), v -> pin(this::restore));
        row.addView(back, bw(false));
        row.addView(button(S.t("رجوع"), v -> finish()), bw(true));
        focus(back);   // the safe press
    }

    private void restore() {
        message(S.t("نرجّع واجهة المصنع…"), "", null);
        busy = true;
        new Thread(() -> {
            SetupEngine.Result r;
            try (SetupSession s = SetupSession.open(this, 60_000, null, line -> { })) {
                r = s.engine.restoreFactory();
                if (r.status == SetupEngine.Status.DONE) SetupSession.deleteKeyIfFinished(this);
            } catch (Exception e) {
                r = new SetupEngine.Result(SetupEngine.Status.FAILED, S.t("ما قدرنا نكلّم التلفزيون. تأكد من تصحيح USB وجرّب مرة ثانية."));
            }
            final SetupEngine.Result res = r;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; message(res.status == SetupEngine.Status.DONE ? S.t("رجعت واجهة المصنع") : S.t("ما اكتمل"), S.tv(res.message), new String[] {S.t("رجوع")}); });
        }, "factory-restore").start();
    }

    // ---------------------------------------------------------------- pieces

    /** the family PIN first when one is set (a wrong or cancelled PIN changes nothing) */
    private void pin(final Runnable go) {
        PinGuard.confirm(this, "\u2066UrTV\u2069 النظيف", go, () -> { });
    }

    /** the readable name of each package (its label when the TV knows it, else the package's own name), one a line */
    private String names(List<String> pkgs) {
        PackageManager pm = getPackageManager();
        StringBuilder b = new StringBuilder();
        for (String p : pkgs) {
            String label = p;
            try { label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(p, PackageManager.MATCH_DISABLED_COMPONENTS))); } catch (Exception ignored) { }
            b.append(b.length() > 0 ? "\n" : "").append("•  ").append(label.equals(p) ? p : label + "  (" + p + ")");
        }
        return b.length() == 0 ? S.t("لا شيء يحتاج تعطيل") : b.toString();
    }

    private void message(String title, String body, String[] buttons) {
        col.removeAllViews();
        col.addView(text(title, 30, "Medium", look.text), lp(dp(12)));
        if (!body.isEmpty()) col.addView(text(body, 20, "Regular", look.sub), lp(dp(16)));
        if (buttons != null) {
            LinearLayout row = row();
            View first = button(buttons[0], v -> finish());
            row.addView(first, bw(false));
            focus(first);
        }
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
