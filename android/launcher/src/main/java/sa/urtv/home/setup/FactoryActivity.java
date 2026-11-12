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
import sa.urtv.home.setup.pair.PairState;

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
        if (going) return;   // this screen opened the phone page or the developer options itself (the pairing page): it stays behind them
        finish();
    }

    private boolean going;

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private GlassStyle.Look look;
    private LinearLayout col;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private SetupEngine.FactoryOffer offer;
    private boolean busy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // a screen that sleeps mid-wait shows no «Allow USB debugging?» (the tester's build 266)
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
    @Override public void onBackPressed() { PairBridge.disarm(); finish(); }

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
            SetupEngine.FactoryOffer o = null;
            String why = "";
            // The paths, one after the other, each with a short wait (no dead three minutes): 1) the TV's own loopback, as the first-run setup does it (SetupHost: the screensaver held back
            // while the TV asks); 2) the TV's own address on the network, where adbd sees a network client (some TVs raise the question only for one). The answer is logged under UrTVFactory.
            for (int path = 0; path < 2 && o == null; path++) {
                final String host = path == 0 ? "127.0.0.1" : LanAddress.find();
                if (host == null) { android.util.Log.i("UrTVFactory", "path " + path + ": the TV has no address on the network"); continue; }
                android.util.Log.i("UrTVFactory", "path " + (path == 0 ? "loopback" : "network address") + ": trying for " + PROMPT_MS / 1000 + " s");
                SaverHold.begin(getApplicationContext());   // no screensaver over the question
                try (SetupSession s = SetupSession.open(this, host, PROMPT_MS, () -> ui.post(() -> { if (!isFinishing() && !isDestroyed()) message(S.t("نفحص هذا التلفزيون…"), S.t("اضغط «سماح» في سؤال تصحيح USB على الشاشة."), null); }), line -> { })) {
                    SaverHold.end();
                    o = s.engine.factoryOffer();
                    android.util.Log.i("UrTVFactory", "offer (" + (path == 0 ? "loopback" : "network address") + "): " + (o.refusal == null ? "ok, " + o.packages.size() + " packages" : "refused: " + o.refusal));
                } catch (Exception e) {
                    SaverHold.end();
                    why = String.valueOf(e.getMessage());
                    android.util.Log.w("UrTVFactory", "path " + (path == 0 ? "loopback" : "network address") + " failed: " + why);
                    if (why.contains("debugging is off")) break;   // the port is closed: no other address opens it
                }
            }
            if (o == null && PairRoute.available() && !why.contains("debugging is off")) {
                // the TV never raised the question: wireless-debugging pairing (4.3.1), typed from the phone page while Settings shows its dialog. Paired before: the connection over it first.
                if (PairRoute.paired(this)) {
                    try (SetupSession s = PairRoute.open(this, line -> { })) {
                        o = s.engine.factoryOffer();
                        android.util.Log.i("UrTVFactory", "offer (paired route): " + (o.refusal == null ? "ok, " + o.packages.size() + " packages" : "refused: " + o.refusal));
                    } catch (Exception e) {
                        android.util.Log.w("UrTVFactory", "paired route failed: " + e.getMessage());
                    }
                }
                if (o == null) {
                    ui.post(() -> { if (!isFinishing() && !isDestroyed()) { busy = false; pairPage(); } });
                    return;
                }
            }
            if (o == null) o = new SetupEngine.FactoryOffer(whyNot(why), "", new ArrayList<String>());
            final SetupEngine.FactoryOffer res = o;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; offer = res; if (res.refusal != null) refused(res.refusal); else plan(); });
        }, "factory-check").start();
    }

    /** how long each path waits for «السماح بتصحيح USB؟» (the first-run setup tells the person after 20 s that the question may be stuck) */
    private static final int PROMPT_MS = 45_000;

    /** what went wrong with the connection, in words that say what to do next (the log tag UrTVFactory has each step) */
    private String whyNot(String why) {
        if (why.contains("debugging is off")) return S.t("تصحيح USB مطفأ في هذا التلفزيون، وهذه الخطوة تحتاجه (وتحتاجه الاستعادة). فعّله من إعدادات المطوّرين ثم جرّب.");
        if (why.contains("not accepted") || why.isEmpty())
            return S.t("هذا التلفزيون ما عرض سؤال «السماح بتصحيح USB؟» لتطبيقنا، فما نقدر نسوّي هذه الخطوة منه. ما غيّرنا شيئًا، وUrTV يشتغل ممتاز بدونها. وإن كان جهاز آخر موصول بالتلفزيون عبر adb فافصله وجرّب.");
        if (why.contains("did not respond")) return S.t("التلفزيون ما ردّ على اتصال التصحيح. أطفئ «تصحيح USB» وشغّله من إعدادات المطوّرين ثم جرّب.");
        if (why.contains("rejected")) return S.t("التلفزيون رفض مفتاح التطبيق. من إعدادات المطوّرين اختر «إلغاء تصريحات تصحيح USB» ثم جرّب من جديد.");
        return S.t("ما قدرنا نكلّم التلفزيون: تأكد من تفعيل تصحيح USB، وافصل أي كمبيوتر موصول بالتلفزيون عبر adb، ثم جرّب.");
    }

    // ---------------------------------------------------------------- pairing from the phone (4.3.1)

    private boolean pairShown;
    private TextView pairLine;
    private final Runnable pairTick = new Runnable() {
        @Override public void run() {
            if (!pairShown || isFinishing() || isDestroyed()) return;
            String st = PairBridge.status();
            pairLine.setText(pairWords(st));
            if (PairState.PAIRED.equals(st)) { pairShown = false; PairBridge.disarm(); checkPaired(); return; }
            ui.postDelayed(this, 1000);
        }
    };

    private String pairWords(String st) {
        if (PairState.ARMED.equals(st)) return S.t("جاهز: اكتب الرمز من صفحة الجوال.");
        if (PairState.PAIRING.equals(st)) return S.t("نقرن…");
        if (PairState.FAILED.equals(st)) {
            String why = PairBridge.why();
            return "no_dialog".equals(why) ? S.t("ما ظهرت نافذة «إقران الجهاز برمز الإقران». افتحها وجرّب الرمز من جديد.") : "wrong".equals(why) ? S.t("ما انقبل الرمز. اكتب الرمز الجديد الظاهر على التلفزيون.") : S.t("ما قدرنا نقرن. جرّب من جديد.");
        }
        return S.t("اضغط «ابدأ الإقران».");
    }

    /** the TV never raised «السماح بتصحيح USB؟»: pair it by wireless debugging, with the six digits typed on the phone page while Settings keeps its dialog open */
    private void pairPage() {
        col.removeAllViews();
        pairShown = true;
        col.addView(text(S.t("إقران التلفزيون من الجوال"), 30, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("هذا التلفزيون ما عرض سؤال «السماح بتصحيح USB؟» لتطبيقنا. نقدر نقرنه بالتصحيح اللاسلكي، وتحتاج جوالك لأن رمز الإقران يظهر في نافذة لازم تبقى مفتوحة."), 20, "Regular", look.sub), lp(dp(12)));
        for (String step : new String[] {S.t("١. اضغط «ابدأ الإقران»، وافتح صفحة الجوال وامسح رمزها (لو جوالك متصل ما تحتاج)."),
                S.t("٢. من خيارات المطوّر شغّل «تصحيح الأخطاء اللاسلكي» ثم اضغط «إقران الجهاز برمز الإقران». يظهر رمز من ٦ أرقام: اترك النافذة مفتوحة."),
                S.t("٣. في صفحة الجوال افتح «إقران التلفزيون» واكتب الرمز."),
                S.t("٤. ارجع هنا بزر الرجوع: يكمل الفحص وحده.")})
            col.addView(text(step, 18, "Regular", look.text), lp(dp(6)));
        pairLine = text(pairWords(PairBridge.status()), 20, "Medium", look.accent);
        col.addView(pairLine, lp(dp(12)));
        LinearLayout row = row();
        View start = button(S.t("ابدأ الإقران"), v -> pin(() -> {
            PairBridge.arm();
            going = true;
            try { startActivity(new android.content.Intent().setClassName(getPackageName(), "sa.urtv.home.PhoneActivity")); } catch (RuntimeException e) { android.util.Log.w("UrTVFactory", "the phone page's screen did not open"); }
        }));
        row.addView(start, bw(false));
        row.addView(button(S.t("افتح خيارات المطوّر"), v -> {
            going = true;
            try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
            catch (RuntimeException e) { try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (RuntimeException ignored) { } }
        }), bw(true));
        LinearLayout row2 = row();
        View back = button(S.t("رجوع"), v -> { PairBridge.disarm(); finish(); });
        row2.addView(back, bw(false));
        focus(start);
        ui.removeCallbacks(pairTick);
        ui.postDelayed(pairTick, 1000);
    }

    /** paired: the check goes on over the paired connection */
    private void checkPaired() {
        message(S.t("نفحص هذا التلفزيون…"), "", null);
        busy = true;
        new Thread(() -> {
            SetupEngine.FactoryOffer o;
            try (SetupSession s = PairRoute.open(this, line -> { })) {
                o = s.engine.factoryOffer();
                android.util.Log.i("UrTVFactory", "offer (paired route): " + (o.refusal == null ? "ok, " + o.packages.size() + " packages" : "refused: " + o.refusal));
            } catch (Exception e) {
                android.util.Log.w("UrTVFactory", "paired route failed: " + e.getMessage());
                o = new SetupEngine.FactoryOffer(S.t("تم الإقران لكن ما قدرنا نتصل بالتلفزيون بعده. تأكد أن «تصحيح الأخطاء اللاسلكي» شغّال وجرّب من جديد."), "", new ArrayList<String>());
            }
            final SetupEngine.FactoryOffer res = o;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; offer = res; if (res.refusal != null) refused(res.refusal); else plan(); });
        }, "factory-check-paired").start();
    }

    @Override protected void onResume() {
        super.onResume();
        going = false;
        if (pairShown) { ui.removeCallbacks(pairTick); ui.postDelayed(pairTick, 300); }
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(pairTick);
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
            try (SetupSession s = SetupSession.openAny(this, 60_000, null, line -> { })) {
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
            try (SetupSession s = SetupSession.openAny(this, 60_000, null, line -> { })) {
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
