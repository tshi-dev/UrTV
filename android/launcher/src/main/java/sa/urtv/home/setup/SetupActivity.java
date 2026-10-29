package sa.urtv.home.setup;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import sa.urtv.home.SetupGate;

import java.io.IOException;
import java.util.List;

/**
 * «إعداد تلفزيوني»: the setup, done by the TV itself with no computer. It shows exactly what it will do (SetupEngine.plan), and «ابدأ» runs it over
 * the TV's own local ADB: the person accepts the TV's "Allow USB debugging?" once. «تراجع» undoes it with the same ledger and journal rules as
 * optimize/restore.sh. Opened from the apps row (a second launcher entry of this app), so it is there right after the APK is installed with
 * the TV's Downloader app. Nothing here logs a network name, a serial or an address.
 */
public class SetupActivity extends Activity {
    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private sa.urtv.home.GlassStyle.Look look;

    private TextView log, status, start, undo;

    /** «لو صار شي»: the steps of docs/IF-BROKEN.md, very short, on the done screen */
    static final String IF_BROKEN = "لو صار شي:\n"
        + "• الشاشة سوداء أو الرئيسية ما تفتح: انتظر 5 دقائق (مفتاح الأمان يرجّع الشاشة الأصلية وحده). وإذا صارت الرئيسية تتوقف كل مرة، فزر الرئيسية يفتح «الوضع الآمن».\n"
        + "• الريموت ما يستجيب: افصل التلفزيون من الكهرباء 30 ثانية ثم شغّله.\n"
        + "• من «الوضع الآمن» أو من هنا: «رجّع واجهة التلفزيون الأصلية» / «تراجع» (يحتاج «تصحيح USB»).\n"
        + "• من كمبيوتر: optimize/restore-home.sh\n"
        + "• آخر حل: إعادة ضبط المصنع (تمسح كل التطبيقات والحسابات والإعدادات).\n"
        + "• للإبلاغ: «أرسل تقرير المشكلة» في «الوضع الآمن»، أو github.com/tshi-dev/UrTV/issues";
    private volatile boolean running;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(120), dp(36), dp(120), dp(48));
        scroll.addView(col);

        col.addView(text("إعداد تلفزيوني", 26, "Medium", TEXT), right(0));
        col.addView(text("يجهّز التلفزيون نفسه، بدون كمبيوتر. هذا ما سيفعله بالترتيب:", 14, "Regular", DIM), right(dp(10)));
        List<String> plan = SetupEngine.plan();
        for (int i = 0; i < plan.size(); i++) col.addView(text((i + 1) + ". " + plan.get(i), 14, "Regular", TEXT), right(dp(6)));
        col.addView(text("قبل البدء: فعّل «تصحيح USB» (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB). "
            + "عند الضغط على «ابدأ» سيسألك التلفزيون «السماح بتصحيح USB؟»: اختر «السماح دائماً».", 13, "Regular", GOLD), right(dp(14)));

        start = button("ابدأ", TEXT);
        start.setFilterTouchesWhenObscured(true);   // another window over this screen cannot press the buttons
        start.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(false); } });
        col.addView(start, right(dp(10)));
        undo = button("تراجع (يرجّع التلفزيون كما كان)", TEXT);
        undo.setFilterTouchesWhenObscured(true);
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(true); } });
        col.addView(undo, right(dp(14)));

        status = text("", 16, "Medium", GOLD);
        col.addView(status, right(dp(8)));
        log = text("", 13, "Regular", DIM);
        col.addView(log, right(0));
        setContentView(scroll);
        // «ابدأ» has the focus once the screen is laid out (asked before that, the TV's first OK only put the focus on it: nothing happened)
        start.post(new Runnable() { @Override public void run() { start.requestFocus(); } });
        if (SetupRecovery.pending(this) && SetupRecovery.gaveUp(this)) {
            status.setText(SetupRecovery.GAVE_UP_NOTICE + ": اضغط «تراجع».");
        } else if (SetupRecovery.pending(this)) {
            // a home switch was cut off (the app was closed, the TV restarted): the safety switch is set again from its saved deadline, and one attempt is made now
            status.setText("بدأ تبديل سابق للشاشة الرئيسية ولم يكتمل. نحاول إرجاع الشاشة الأصلية الآن؛ إن لم تظهر وحدها فاضغط «تراجع».");
            SetupRecovery.rearmFromDisk(this);
            new Thread(new Runnable() {
                @Override public void run() {
                    SetupEngine.Result r = SetupRecovery.attempt(SetupActivity.this, new SetupEngine.Out() { @Override public void line(String t) { SetupActivity.this.line(t); } });
                    if (r != null) {
                        final String m = r.message;
                        runOnUiThread(new Runnable() { @Override public void run() { status.setText(m); } });
                    }
                }
            }, "setup-recover").start();
        }
    }

    private void go(final boolean undoIt) {
        if (running) return;
        // with a family PIN set, the PIN first
        SetupGate.guard(this, new Runnable() { @Override public void run() { goAfterPin(undoIt); } }, new Runnable() { @Override public void run() { } });
    }

    private void goAfterPin(final boolean undoIt) {
        if (running) return;
        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // only while a run is going (the screensaver would cut it)
        start.setAlpha(.5f);
        undo.setAlpha(.5f);
        log.setText("");
        status.setText(undoIt ? "جارٍ الرجوع…" : "جارٍ الإعداد… لا تطفئ التلفزيون.");
        // the answer to the press is in sight even when the plan above pushes it under the screen's edge
        status.post(new Runnable() { @Override public void run() { status.requestRectangleOnScreen(new android.graphics.Rect(0, 0, status.getWidth(), status.getHeight())); } });
        new Thread(new Runnable() {
            @Override public void run() {
                String end;
                int color = GOLD;
                if (undoIt) {
                    // the one undo, shared with «الوضع الآمن» and the phone page (RestoreHome)
                    RestoreHome.Outcome o = RestoreHome.run(SetupActivity.this, new RestoreHome.Lines() {
                        @Override public void line(String t) { SetupActivity.this.line(t); }
                        @Override public void waiting() { SetupActivity.this.line("التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت."); }
                    });
                    end = o.message;
                    if (!o.ok && !o.message.startsWith("انتهى الرجوع")) color = RED;   // it did not run (a finished undo with notes stays gold, as before)
                } else try (SetupSession s = SetupSession.open(SetupActivity.this, 180_000, new AdbClient.Approval() {
                    @Override public void waiting() { line("التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت."); }
                }, new SetupEngine.Out() { @Override public void line(String t) { SetupActivity.this.line(t); } })) {
                    SetupEngine e = s.engine;
                    SetupEngine.Result pre = e.precheck();
                    if (pre.status != SetupEngine.Status.DONE) {
                        end = "توقّفنا قبل أي تغيير.";
                        color = RED;
                    } else {
                        int failed = 0, skipped = 0;
                        SetupEngine.Result[] rs = {e.grants(), e.debloat(), e.screensaver(), e.home()};
                        for (SetupEngine.Result r : rs) { if (r.status == SetupEngine.Status.FAILED) failed++; else if (r.status == SetupEngine.Status.SKIPPED) skipped++; }
                        end = failed == 0 && skipped == 0 ? "انتهى الإعداد. اضغط زر الرئيسية في الريموت. ثم أطفئ «تصحيح USB» من خيارات المطوّر واختر «إلغاء تراخيص تصحيح USB» إن لم تحتجهما (التراجع من هنا يحتاجهما)."
                            : "انتهى الإعداد مع " + failed + " خطوة لم تتم و" + skipped + " تُركت (التفاصيل فوق). «تراجع» يرجّع كل شيء.";
                        if (failed > 0) color = RED;
                        end += "\n\n" + IF_BROKEN;   // the done screen: what to do if something goes wrong later (docs/IF-BROKEN.md, short)
                    }
                } catch (IOException ex) {
                    end = RestoreHome.friendly(ex.getMessage());
                    color = RED;
                } catch (RuntimeException ex) {
                    end = "حدث خطأ غير متوقع، ما غيّرنا شيئاً بعده. جرّب «تراجع» ثم أعد.";
                    color = RED;
                }
                if (!undoIt) SetupSession.deleteKeyIfFinished(SetupActivity.this);   // after EVERY end (done, failed, skipped, error): the key is a standing credential; it stays only while a switch is pending (RestoreHome does it for the undo)
                final String msg = end;
                final int c = color;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        running = false;
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        start.setAlpha(1f);
                        undo.setAlpha(1f);
                        status.setText(msg);
                        status.setTextColor(c);
                    }
                });
            }
        }, "setup-run").start();
    }

    private void line(final String t) {
        runOnUiThread(new Runnable() { @Override public void run() { log.append("• " + t + "\n"); } });
    }

    /** a button in the glass look: the panel, the solid focus fill and its ink while focused */
    private TextView button(String label, int fg) {
        TextView t = text(label, 18, "Medium", fg);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setFocusable(true);
        t.setClickable(true);
        sa.urtv.home.GlassPaint.card(t, look, 14);
        return t;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams right(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = sa.urtv.home.GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub; RED = look.warn;
    }
}
