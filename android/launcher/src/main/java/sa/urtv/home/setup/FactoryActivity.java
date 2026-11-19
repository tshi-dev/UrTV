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
        if (going) return;   // 4.4 beta 3: this screen opened the phone page, the developer options or the add-on's install itself (the pairing): it stays behind them
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
        // 4.4 (the owner: «ONE BOX for everything»): the same box as the setup's (sa.urtv.home.Box): one size, the page's title fixed, the rest scrolls inside
        FrameLayout probe = new FrameLayout(this);
        look = GlassSource.page(this, probe);
        box = sa.urtv.home.Box.make(this, GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line), look.text, look.sub, null, Dir.layout());
        box.page.setBackground(probe.getBackground());
        col = box.content;
        setContentView(box.page);
        if (new FileStore(this).get(SetupEngine.FACTORY) != null && new FileStore(this).get(SetupEngine.FACTORY).startsWith("on")) showOn();
        else intro();
    }

    /** Back always leaves (a check or a step in progress goes on by itself on its thread; the ledger and the safety switch do not depend on this screen) */
    @Override public void onBackPressed() { leavePairing(); finish(); }

    // ---------------------------------------------------------------- the first page

    /** one OK on the settings' row lands here, not on a check: a plain description and two buttons, the focus on «رجوع»; nothing starts until «ابدأ» */
    private void intro() {
        col.removeAllViews();
        page(S.t("تخلّص من واجهة المصنع"), look.text);
        col.addView(text(S.t("هذي الخطوة تعطّل (ما تحذف) تطبيقات واجهة المصنع في تلفزيونك، ويصير UrTV هو الأساس."), 17, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("أول شيء نفحص التلفزيون ونعرض لك بالضبط وش بيتعطّل، وما نغيّر شيء قبل ما تتأكد مرتين."), 17, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("قد يظهر على الشاشة سؤال «السماح بتصحيح USB؟»: اضغط «سماح»."), 17, "Regular", look.sub), lp(dp(8)));
        col.addView(text(S.t("وتقدر ترجّع كل شيء بضغطة وحدة."), 17, "Regular", look.sub), lp(dp(16)));
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
            if (o == null) {
                // 4.4 beta 3 (the owner: every Google TV and Chromecast with only «تصحيح الأخطاء اللاسلكي»): 5555 did not answer; the paired route through the add-on
                String route = PairRoute.why(this);
                android.util.Log.i("UrTVFactory", "5555 did not answer; paired route: " + route);
                if ("old_android".equals(route)) {   // fail closed: today's reason, and that this TV has no wireless debugging
                    o = new SetupEngine.FactoryOffer(whyNot(why) + "\n\n" + S.t("هذا التلفزيون ما فيه التصحيح اللاسلكي (أندرويد ١٠ أو أقدم). شغّل «تصحيح USB» من خيارات المطوّر، وأعد تشغيل التلفزيون، وجرّب من جديد."), "", new ArrayList<String>());
                } else if ("paired".equals(route)) {
                    o = pairedOffer();
                } else {
                    final boolean needAddon = "no_addon".equals(route) || "old_addon".equals(route);
                    ui.post(() -> { if (!isFinishing() && !isDestroyed()) { busy = false; pairPage(needAddon); } });
                    return;
                }
            }
            final SetupEngine.FactoryOffer res = o;
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; offer = res; if (res.refusal != null) refused(res.refusal); else plan(); });
        }, "factory-check").start();
    }

    /** on the factory-check thread: the offer over the paired route; a failure is the refusal with what to do (wireless debugging off after a restart, or the connection refused) */
    private SetupEngine.FactoryOffer pairedOffer() {
        try (SetupSession s = PairRoute.open(this, line -> { })) {
            SetupEngine.FactoryOffer o = s.engine.factoryOffer();
            android.util.Log.i("UrTVFactory", "offer (paired route): " + (o.refusal == null ? "ok, " + o.packages.size() + " packages" : "refused: " + o.refusal));
            return o;
        } catch (Exception e) {
            String m = String.valueOf(e.getMessage());
            android.util.Log.w("UrTVFactory", "paired route failed: " + m);
            return new SetupEngine.FactoryOffer("no_port".equals(m)
                ? S.t("التصحيح اللاسلكي انطفأ بعد إعادة التشغيل (هذا التلفزيون يطفيه كل مرة). شغّله من خيارات المطوّر، وارجع هنا: ما تحتاج تقرن من جديد.")
                : S.t("تم الإقران لكن ما قدرنا نتصل بالتلفزيون بعده. تأكد أن «تصحيح الأخطاء اللاسلكي» شغّال وجرّب من جديد."), "", new ArrayList<String>());
        }
    }

    // ---------------------------------------------------------------- pairing from the phone (4.4 beta 3; the pairing itself runs in the add-on)

    private volatile boolean pairShown;
    private TextView pairLine;
    /** the state is asked of the add-on on this thread, never the UI thread; the words are put on the screen after */
    private final java.util.concurrent.ExecutorService pairAsk = java.util.concurrent.Executors.newSingleThreadExecutor(r -> new Thread(r, "factory-pair"));
    private final Runnable pairTick = new Runnable() {
        @Override public void run() {
            if (!pairShown || isFinishing() || isDestroyed()) return;
            try {
                pairAsk.execute(() -> {
                    final String st = PairBridge.status(FactoryActivity.this), why = PairBridge.why();
                    ui.post(() -> {
                        if (!pairShown || isFinishing() || isDestroyed()) return;
                        pairLine.setText(pairWords(st, why));
                        if ("paired".equals(st)) { pairShown = false; pairAsk.execute(() -> PairBridge.disarm(FactoryActivity.this)); checkPaired(); return; }
                        ui.postDelayed(pairTick, 1000);
                    });
                });
            } catch (RuntimeException ignored) { }
        }
    };

    private String pairWords(String st, String why) {
        if ("armed".equals(st)) return S.t("جاهز: اكتب الرمز من صفحة الجوال.");
        if ("pairing".equals(st)) return S.t("نقرن…");
        if ("failed".equals(st)) {
            return "no_dialog".equals(why) ? S.t("ما ظهرت نافذة «إقران الجهاز برمز الإقران». افتحها وجرّب الرمز من جديد.")
                : "wrong".equals(why) ? S.t("ما انقبل الرمز. اكتب الرمز الجديد الظاهر على التلفزيون.") : S.t("ما قدرنا نقرن. جرّب من جديد.");
        }
        return S.t("اضغط «ابدأ الإقران».");
    }

    /** 5555 did not answer: pair this TV by wireless debugging, the six digits typed on the phone page while Settings keeps its dialog open ({@code needAddon}: install the add-on first) */
    private void pairPage(boolean needAddon) {
        col.removeAllViews();
        pairShown = true;
        col.addView(text(S.t("إقران التلفزيون من الجوال"), 22, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("هذا التلفزيون ما عرض سؤال «السماح بتصحيح USB؟» لتطبيقنا. نقدر نقرنه بالتصحيح اللاسلكي، وتحتاج جوالك لأن رمز الإقران يظهر في نافذة لازم تبقى مفتوحة."), 17, "Regular", look.sub), lp(dp(12)));
        View first = null;
        if (needAddon) {
            col.addView(text(S.t("الإقران اللاسلكي يحتاج «إضافات تلفزيونك». ثبّتها أول (ما تحتاج كمبيوتر)، وبعدها ارجع هنا."), 17, "Medium", look.accent), lp(dp(8)));
            LinearLayout r0 = row();
            first = button(S.t("ثبّت إضافات تلفزيونك"), v -> {
                going = true;
                try { startActivity(new android.content.Intent().setClassName(getPackageName(), "sa.urtv.home.ExtrasActivity")); } catch (RuntimeException e) { android.util.Log.w("UrTVFactory", "the add-on's install screen did not open"); }
            });
            r0.addView(first, bw(false));
        }
        for (String step : new String[] {S.t("١. اضغط «ابدأ الإقران»، وافتح صفحة الجوال وامسح رمزها (لو جوالك متصل ما تحتاج)."),
                S.t("٢. من خيارات المطوّر شغّل «تصحيح الأخطاء اللاسلكي» ثم اضغط «إقران الجهاز برمز الإقران». يظهر رمز من ٦ أرقام: اترك النافذة مفتوحة."),
                S.t("٣. في صفحة الجوال افتح «إقران التلفزيون» واكتب الرمز."),
                S.t("٤. ارجع هنا بزر الرجوع: يكمل الفحص وحده.")})
            col.addView(text(step, 17, "Regular", look.text), lp(dp(6)));
        LinearLayout row = row();
        View start = button(S.t("ابدأ الإقران"), v -> pin(() -> {
            pairLine.setText(S.t("نفحص هذا التلفزيون…"));
            try {
                pairAsk.execute(() -> {   // the add-on is asked off the UI thread; a refusal is said on the line, never silent
                    final boolean armed = PairBridge.arm(FactoryActivity.this);
                    ui.post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (!armed) { pairLine.setText(S.t("الإقران اللاسلكي يحتاج «إضافات تلفزيونك». ثبّتها أول (ما تحتاج كمبيوتر)، وبعدها ارجع هنا.")); return; }
                        pairLine.setText(pairWords("armed", ""));
                        going = true;
                        try { startActivity(new android.content.Intent().setClassName(getPackageName(), "sa.urtv.home.PhoneActivity")); } catch (RuntimeException e) { android.util.Log.w("UrTVFactory", "the phone page's screen did not open"); }
                    });
                });
            } catch (RuntimeException ignored) { }
        }));
        row.addView(start, bw(false));
        row.addView(button(S.t("افتح خيارات المطوّر"), v -> {
            going = true;
            try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
            catch (RuntimeException e) { try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (RuntimeException ignored) { } }
        }), bw(true));
        pairLine = text(pairWords("off", ""), 17, "Medium", look.accent);   // each state and failure is said here, under the buttons (never a toast)
        col.addView(pairLine, lp(dp(12)));
        LinearLayout row2 = row();
        row2.addView(button(S.t("رجوع"), v -> { leavePairing(); finish(); }), bw(false));
        focus(first != null ? first : start);
        ui.removeCallbacks(pairTick);
        ui.postDelayed(pairTick, 1000);
    }

    /** paired: the check goes on over the paired connection */
    private void checkPaired() {
        message(S.t("نفحص هذا التلفزيون…"), "", null);
        busy = true;
        new Thread(() -> {
            final SetupEngine.FactoryOffer res = pairedOffer();
            ui.post(() -> { if (isFinishing() || isDestroyed()) return; busy = false; offer = res; if (res.refusal != null) refused(res.refusal); else plan(); });
        }, "factory-check-paired").start();
    }

    /** leaving the pairing: the add-on is told off the UI thread */
    private void leavePairing() {
        if (!pairShown) return;
        pairShown = false;
        final android.content.Context app = getApplicationContext();
        try { pairAsk.execute(() -> PairBridge.disarm(app)); } catch (RuntimeException ignored) { }
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

    @Override protected void onDestroy() {
        super.onDestroy();
        pairAsk.shutdown();   // a question already asked finishes; nothing new
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

    private void refused(String reason) {
        message(S.t("ما نقدر نسوّيها هنا"), S.tv(reason), new String[] {S.t("رجوع")});
    }

    // ---------------------------------------------------------------- what it will do

    private void plan() {
        col.removeAllViews();
        page(S.t("تخلّص من واجهة المصنع"), look.text);
        col.addView(text(S.t("تعطيل لا حذف: ما يُحذف شيء، وكل ما نعطّله يرجع بضغطة وحدة."), 17, "Medium", look.accent), lp(dp(12)));
        col.addView(text(S.t("هذا اللي بنعطّله في هذا التلفزيون بالضبط:"), 17, "Regular", look.sub), lp(dp(8)));
        col.addView(text(names(offer.packages), 17, "Regular", look.text), lp(dp(12)));
        col.addView(text(S.t("يبقى محميًّا ولا نلمسه: الواي فاي والبلوتوث والصوت والبث (Chromecast وAirPlay) ومداخل HDMI ولوحات المفاتيح وكل ما في قائمة الخصائص المحمية."), 17, "Regular", look.sub), lp(dp(12)));
        col.addView(text(S.t("للتراجع: «رجّع واجهة المصنع» في هذي الشاشة نفسها، ويرجع تلقائيًا لو توقف تلفزيونك مرتين عند التشغيل. اترك تصحيح USB شغّالًا ما دام هذا مفعّلًا."), 17, "Regular", look.sub), lp(dp(16)));
        LinearLayout row = row();
        View cancel = button(S.t("إلغاء"), v -> finish());
        row.addView(cancel, bw(false));
        row.addView(button(S.t("متابعة"), v -> sure()), bw(true));
        focus(cancel);   // the focus starts on «إلغاء»
    }

    /** the second step: «متأكد؟», again with the focus on «إلغاء»; only «نعم، عطّل» goes on (with the PIN when one is set) */
    private void sure() {
        col.removeAllViews();
        page(S.t("متأكد؟"), look.warn);
        col.addView(text(S.t("بنعطّل %s من تطبيقات واجهة المصنع في هذا التلفزيون ونفتح على تلفزيونك مباشرة. تقدر ترجّعها بضغطة وحدة.", Integer.toString(offer.packages.size())), 17, "Regular", look.text), lp(dp(16)));
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
        page(S.t("واجهة المصنع معطّلة"), look.text);
        col.addView(text(S.t("اللي عطّلناه (تعطيل لا حذف):"), 17, "Regular", look.sub), lp(dp(8)));
        List<String> ledger = SetupEngine.ledgerNames(new FileStore(this).get("disabled-by-us.txt"));
        col.addView(text(names(ledger), 17, "Regular", look.text), lp(dp(16)));
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
                if (r.status == SetupEngine.Status.DONE) SetupSession.dropKey(this);   // 4.4 F20: the interface is back: the key goes
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
            b.append(b.length() > 0 ? "\n" : "").append(label.equals(p) ? p : label + "  (" + p + ")");
        }
        return b.length() == 0 ? S.t("لا شيء يحتاج تعطيل") : b.toString();
    }

    private void message(String title, String body, String[] buttons) {
        col.removeAllViews();
        page(title, look.text);
        if (!body.isEmpty()) col.addView(text(body, 17, "Regular", look.sub), lp(dp(16)));
        if (buttons != null) {
            LinearLayout row = row();
            View first = button(buttons[0], v -> finish());
            row.addView(first, bw(false));
            focus(first);
        }
    }

    private void focus(final View v) { box.focusFirst(v); }   // 4.4: long words read before the focus goes to a button (Box)

    /** a new page in the same box: its title (in the box's fixed title), the words from the top */
    private void page(String title, int color) {
        box.title.setText(title);
        box.title.setTextColor(color);
        box.scroll.scrollTo(0, 0);
    }

    private sa.urtv.home.Box box;

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
        TextView t = text(label, 17, "Medium", look.text);
        t.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        t.setPadding(dp(20), dp(8), dp(20), dp(8));
        t.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));   // 4.4 compact: 44 dp
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
