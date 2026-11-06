package sa.urtv.home.setup;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import sa.urtv.home.Digits;
import sa.urtv.home.Dir;
import sa.urtv.home.S;
import sa.urtv.home.DigitsText;
import sa.urtv.home.SetupGate;
import sa.urtv.home.phone.QrCode;
import sa.urtv.home.phone.QrView;

import java.util.ArrayList;
import java.util.List;

/**
 * «إعداد تلفزيوني»: the whole setup, done by the TV itself with no computer, from ONE button, «جهّز كل شي». The run is SetupRunner's full run (one at a
 * time, on a thread that outlives this screen); this screen only starts it and shows it, in five states:
 *   start: «جهّز كل شي» and «تراجع». The TV asks «السماح بتصحيح USB؟» once; with no answer in 20 s the screen says how to clear a stuck prompt.
 *   plan: after the precheck (read only), what the run will do on THIS TV with its real counts, the box for TV Bro and SmartTube (on), «ابدأ».
 *         Nothing is changed before «ابدأ».
 *   progress: one line per step: ✓ done, – تُرك, ✕ not done (with why).
 *   wifi: the TV's setup wizard owns the saved Wi-Fi: forget the network and join it again, then «كمّل» (the run goes on at the home switch).
 *   done: the report's QR code (docs/share, the person sends it if they want), how to turn debugging off, «لو صار شي», «تراجع».
 * «تراجع» undoes it with the same ledger and journal rules as optimize/restore.sh. Opened from the apps row (a second launcher entry of this app),
 * so it is there right after the APK is installed with the TV's Downloader app. Nothing here logs a network name, a serial or an address.
 */
public class SetupActivity extends Activity {
    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private sa.urtv.home.GlassStyle.Look look;

    private LinearLayout body;
    private TextView status, detail, start, undo;
    private boolean running;     // a run is shown as going (this screen's thread only)
    private boolean extras = true;
    private String mode = "";
    private final List<TextView> stepViews = new ArrayList<>();       // each step's label
    private final List<TextView> stepSubs = new ArrayList<>();        // the line under the step in progress
    private final List<StepMark> stepMarks = new ArrayList<>();       // its dot on the rail
    private TextView count;                                            // «٧ من ١٥»
    private sa.urtv.home.CalmBar countBar;
    private TextView notice;                                           // «سيعيد التلفزيون التشغيل الآن، وسيكمل بنفسه»
    private static SetupRunner.End dismissed;
    /** the done screen's QR code: a multiple of 8 dp (the packed report needs a small code, tester step 18) */
    static final int QR_DP = 224;   // a done screen the person closed with «تم»: the start screen again

    /** «اللغة» (docs/ENGLISH.md): the done screen's own words in English when it is set (i18n/en/setup.tsv) */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        // one card on a solid surface, in the 5 % safe area and 16 dp more, as the update screen
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        int sw = getResources().getDisplayMetrics().widthPixels, sh = getResources().getDisplayMetrics().heightPixels;
        frame.setPadding(Math.round(sw * 0.05f), Math.round(sh * 0.05f) + dp(16), Math.round(sw * 0.05f), Math.round(sh * 0.05f) + dp(16));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(40), dp(32), dp(40), dp(32));
        col.setBackground(sa.urtv.home.GlassPaint.fill(this, sa.urtv.home.GlassStyle.over(look.dialog, look.base), 22, look.line));
        frame.addView(col, new FrameLayout.LayoutParams(Math.min(dp(880), sw - 2 * Math.round(sw * 0.05f)), -2, Gravity.CENTER));
        col.addView(text(S.t("إعداد تلفزيوني"), 28, "Medium", TEXT), right(dp(16)));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        col.addView(body, right(0));
        status = text("", 16, "Medium", GOLD);
        col.addView(status, right(dp(8)));
        detail = text("", 13, "Regular", DIM);
        col.addView(detail, right(0));
        setContentView(scroll);
        show();
        if (SetupRecovery.pending(this) && SetupRecovery.gaveUp(this)) {
            say(SetupRecovery.GAVE_UP_NOTICE + ": اضغط «تراجع».", RED);
        } else if (SetupRecovery.pending(this) && !SetupRunner.get().running()) {
            // a home switch was cut off (the app was closed, the TV restarted): the safety switch is set again from its saved deadline, and one attempt is made now
            say("بدأ تبديل سابق للشاشة الرئيسية ولم يكتمل. نحاول إرجاع الشاشة الأصلية الآن؛ إن لم تظهر وحدها فاضغط «تراجع».", GOLD);
            SetupRecovery.rearmFromDisk(this);
            new Thread(new Runnable() {
                @Override public void run() {
                    SetupEngine.Result r = SetupRecovery.attempt(SetupActivity.this, new SetupEngine.Out() { @Override public void line(String t) { SetupActivity.this.line(t); } });
                    if (r != null) {
                        final String m = r.message;
                        runOnUiThread(new Runnable() { @Override public void run() { say(m, GOLD); } });
                    }
                }
            }, "setup-recover").start();
        }
    }

    // ---- what the screen shows ----------------------------------------------------------------------------------------------------

    /** The state the runner is in decides the screen; it is built again only when that state changes, otherwise its lines are updated. */
    private void show() {
        SetupRunner r = SetupRunner.get();
        if (resumedNow) r.screenShown();   // it is in front: the home need not open it again
        String want = decide(r);
        if (!want.equals(mode)) {
            mode = want;
            body.removeAllViews();
            stepViews.clear();
            stepSubs.clear();
            stepMarks.clear();
            count = null;
            countBar = null;
            notice = null;
            detail.setText("");
            if (want.equals("plan")) buildPlan(r);
            else if (want.equals("progress")) buildProgress(r);
            else if (want.equals("wifi")) buildWifi();
            else if (want.equals("done")) buildDone(r);
            else buildStart(r);
        }
        if (mode.equals("progress")) updateProgress(r);
        busy(r.running());
    }

    private String decide(SetupRunner r) {
        if (r.running()) return r.awaitingConfirm() ? "plan" : "progress";
        SetupRunner.End e = r.end();
        if ((e != null && e.wifi) || (e == null && SetupRunner.awaitingWifi(new FileStore(this)))) return "wifi";
        if (e == null || e == dismissed || SetupRunner.NOT_STARTED.equals(e.text)) return "start";
        if (r.kind() == SetupRunner.Kind.FULL && !ran(r)) return "start";   // it ended before any step (the prompt, the connection): said on the start screen
        return "done";
    }

    /** True when a step of the full run ran (the precheck at least). */
    private static boolean ran(SetupRunner r) {
        for (SetupRunner.Step s : r.steps()) if (s.state != SetupRunner.State.PENDING) return true;
        return false;
    }

    private void buildStart(SetupRunner r) {
        add(langToggle(), dp(12));
        add(text(S.t("يجهّز التلفزيون نفسه، بدون كمبيوتر ولا جوال. اضغط «جهّز كل شي»: يسألك التلفزيون مرة واحدة «السماح بتصحيح USB؟»، ثم نعرض عليك ما سنفعله بالضبط قبل أي تغيير."), 15, "Regular", TEXT), dp(12));
        add(text(S.t("قبل البدء: فعّل «تصحيح USB» (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB). وخلك عند التلفزيون: لما تطلع الرسالة اختر «السماح دائماً»."), 13, "Regular", GOLD), dp(16));
        if (SetupRunner.resumable(new FileStore(this))) add(text(S.t("إعداد سابق ما اكتمل: «جهّز كل شي» يكمله من حيث توقف."), 14, "Regular", TEXT), dp(12));
        start = button(S.t("جهّز كل شي"));
        start.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(false); } });
        add(start, dp(12));
        undo = quiet(S.t("تراجع (يرجّع التلفزيون كما كان)"));
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(true); } });
        add(undo, dp(16));
        // «جهّز كل شي» has the focus once the screen is laid out (asked before that, the TV's first OK only put the focus on it: nothing happened)
        final TextView again = refocusLang == null ? null : langButtons.get(refocusLang);
        refocusLang = null;
        final TextView first = again != null ? again : start;
        first.post(new Runnable() { @Override public void run() { first.requestFocus(); } });
        SetupRunner.End e = r.end();
        if (e != null && e != dismissed) say(S.tv(e.text), e.bad ? RED : GOLD);
        else say("", GOLD);
    }

    /**
     * «العربية | English»: the first row of the start screen, two buttons side by side at the start of the line, the one in use marked the way every
     * on/off is (three green triangles, its words in green), the other plain; no pills (radius 14 like every button), 24 px apart. Choosing one
     * saves the language for the whole system and builds the screen again in it (SetupLang). Arabic is the one in use until the person says else.
     */
    private final java.util.Map<String, TextView> langButtons = new java.util.HashMap<>();
    /** the language just chosen: the screen is built again, and the focus goes back to that button (not to «جهّز كل شي», where a second OK would start the run) */
    static String refocusLang;

    private View langToggle() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(Dir.layout());
        row.setGravity(Dir.start());
        String[][] opts = {{SetupLang.AR, "العربية"}, {SetupLang.EN, "English"}};
        for (int i = 0; i < opts.length; i++) {
            final String code = opts[i][0];
            final TextView b = button(opts[i][1]);
            b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            b.setContentDescription(opts[i][1]);
            final boolean on = code.equals(S.lang());
            paintLang(b, on, false);
            b.setOnFocusChangeListener(new View.OnFocusChangeListener() { public void onFocusChange(View v, boolean has) { paintLang(b, on, has); } });
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { if (!code.equals(S.lang())) refocusLang = code; SetupLang.choose(SetupActivity.this, code); } });
            b.setPadding(dp(16), dp(12), dp(16), dp(12));   // the whole word inside: 24 px above and below (the 88 px box cut «English» in half, build 180)
            b.setMinHeight(dp(52));
            langButtons.put(code, b);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(160), -2);
            if (i > 0) p.setMarginStart(dp(12));
            row.addView(b, p);
        }
        return row;
    }

    /** the on/off look (OnOff): the focus fill while focused, else the panel; the one in use has the green triangles and its words in green */
    private void paintLang(TextView t, boolean on, boolean focused) {
        t.setBackground(focused ? sa.urtv.home.GlassPaint.focused(this, look, 14) : sa.urtv.home.GlassPaint.panel(this, look, 14));
        int green = sa.urtv.home.OnOff.green(focused ? look.focus : look.light ? 0xFFFFFFFF : 0xFF000000);
        android.graphics.drawable.Drawable mark = null;
        if (on) {
            mark = getDrawable(sa.urtv.home.R.drawable.ic_on).mutate();
            mark.setTint(green);
            mark.setBounds(0, 0, dp(7), dp(22));
        }
        if (Dir.rtl()) t.setCompoundDrawables(null, null, mark, null); else t.setCompoundDrawables(mark, null, null, null);
        t.setCompoundDrawablePadding(dp(8));
        t.setTextColor(on ? green : focused ? INK : TEXT);
    }

    private void buildPlan(SetupRunner r) {
        SetupRunner.Preview p = r.preview();
        if (p == null) p = new SetupRunner.Preview();
        add(text(S.t("هذا ما سنفعله في تلفزيونك، بالترتيب. ما تغيّر شيء بعد:"), 15, "Medium", TEXT), dp(8));
        List<String> plan = SetupRunner.planLines(p, TR);
        for (int i = 0; i < plan.size(); i++) add(text(digits((i + 1) + ". " + plan.get(i)), 14, "Regular", TEXT), dp(8));
        final boolean already = p.tvbro && p.smarttube;
        final TextView box = button("");
        final String words = SetupRunner.extrasLine(p, TR);
        box.setText(already ? words : (extras ? "☑ " : "☐ ") + words);
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (already) return;
                extras = !extras;
                box.setText((extras ? "☑ " : "☐ ") + words);
            }
        });
        add(box, dp(12));
        final TextView begin = button(S.t("ابدأ"));
        begin.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { SetupRunner.get().confirm(extras); } });
        add(begin, dp(12));
        TextView no = quiet(S.t("إلغاء"));
        no.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { SetupRunner.get().cancel(); } });
        add(no, dp(12));
        begin.post(new Runnable() { @Override public void run() { begin.requestFocus(); } });
        say("", GOLD);
    }

    private void buildProgress(SetupRunner r) {
        add(text(r.kind() == SetupRunner.Kind.UNDO ? S.t("نرجّع التلفزيون كما كان:") : S.t("الإعداد يعمل. لا تطفئ التلفزيون، وقد يُعاد تشغيله وحده ويكمل."), 16, "Medium", TEXT), dp(16));
        stepsList(r);
    }

    /** «٧ من ١٥» with a calm bar, then the steps as a vertical rail: done (a tick), now (a ring, its line under it), next (hollow); a notice when the TV is about to restart */
    private void stepsList(SetupRunner r) {
        List<SetupRunner.Step> steps = r.steps();
        count = text("", 15, "Medium", GOLD);
        add(count, dp(8));
        countBar = new sa.urtv.home.CalmBar(this, look.line, GOLD);
        body.addView(countBar, right(dp(24)));
        notice = text("", 15, "Medium", TEXT);
        notice.setPadding(dp(16), dp(12), dp(16), dp(12));
        notice.setBackground(sa.urtv.home.GlassPaint.panel(this, look, 14));
        notice.setVisibility(View.GONE);
        add(notice, dp(16));
        for (int i = 0; i < steps.size(); i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(Dir.layout());
            StepMark mark = new StepMark(i == 0, i == steps.size() - 1);
            stepMarks.add(mark);
            row.addView(mark, new LinearLayout.LayoutParams(dp(32), -1));
            LinearLayout words = new LinearLayout(this);
            words.setOrientation(LinearLayout.VERTICAL);
            TextView t = text("", 16, "Regular", DIM);
            stepViews.add(t);
            words.addView(t, right(0));
            TextView sub = text("", 13, "Regular", DIM);
            sub.setVisibility(View.GONE);
            stepSubs.add(sub);
            words.addView(sub, right(0));
            words.setPadding(dp(8), dp(4), 0, dp(12));
            row.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
            body.addView(row, right(0));
        }
    }

    /** one step's dot on the rail, drawn: a tick (done), a ring with a dot (now), a hollow circle (next), a dash (left out), a cross (failed); the rail is the accent colour as far as the work has come */
    private final class StepMark extends View {
        private final boolean first, last;
        private SetupRunner.State state = SetupRunner.State.PENDING;
        private boolean before;   // the step above is done
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        StepMark(boolean first, boolean last) {
            super(SetupActivity.this);
            this.first = first;
            this.last = last;
        }

        void set(SetupRunner.State s, boolean doneAbove) {
            if (s == state && doneAbove == before) return;
            state = s;
            before = doneAbove;
            invalidate();
        }

        @Override protected void onDraw(android.graphics.Canvas c) {
            float x = getWidth() / 2f, y = dp(14), r = dp(7);
            boolean done = state == SetupRunner.State.DONE || state == SetupRunner.State.SKIPPED;
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            if (!first) { paint.setColor(before ? GOLD : look.line); c.drawLine(x, 0, x, y - r - dp(2), paint); }
            if (!last) { paint.setColor(done ? GOLD : look.line); c.drawLine(x, y + r + dp(2), x, getHeight(), paint); }
            switch (state) {
                case DONE:
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    paint.setColor(GOLD);
                    c.drawCircle(x, y, r, paint);
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(sa.urtv.home.GlassStyle.over(look.dialog, look.base));
                    c.drawLines(new float[] {x - dp(3), y, x - dp(1), y + dp(2), x - dp(1), y + dp(2), x + dp(3), y - dp(2)}, paint);
                    break;
                case RUNNING:
                    paint.setColor(GOLD);
                    c.drawCircle(x, y, r + dp(1), paint);
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    c.drawCircle(x, y, dp(3), paint);
                    break;
                case FAILED:
                    paint.setColor(RED);
                    c.drawCircle(x, y, r, paint);
                    c.drawLines(new float[] {x - dp(3), y - dp(3), x + dp(3), y + dp(3), x - dp(3), y + dp(3), x + dp(3), y - dp(3)}, paint);
                    break;
                case SKIPPED:
                    paint.setColor(DIM);
                    c.drawCircle(x, y, r, paint);
                    c.drawLine(x - dp(3), y, x + dp(3), y, paint);
                    break;
                default:
                    paint.setColor(look.line);
                    c.drawCircle(x, y, r, paint);
            }
        }
    }

    private void updateProgress(SetupRunner r) {
        List<SetupRunner.Step> steps = r.steps();
        int finished = 0;
        SetupRunner.Step now = null;
        for (int i = 0; i < steps.size() && i < stepViews.size(); i++) {
            SetupRunner.Step s = steps.get(i);
            if (s.state != SetupRunner.State.PENDING && s.state != SetupRunner.State.RUNNING) finished++;
            if (s.state == SetupRunner.State.RUNNING) now = s;
            String tail = "";
            int color;
            switch (s.state) {
                case DONE: color = TEXT; break;
                case SKIPPED: color = DIM; tail = " " + S.t("(تُرك)") + why(s.reason); break;
                case FAILED: color = RED; tail = why(s.reason); break;
                case RUNNING: color = TEXT; break;
                default: color = DIM; break;
            }
            TextView label = stepViews.get(i);
            label.setText(digits(S.tv(SetupRunner.label(s.id)) + tail));
            label.setTextColor(color);
            label.setTypeface(typeface(s.state == SetupRunner.State.RUNNING ? "Medium" : "Regular"));
            boolean above = i > 0 && (steps.get(i - 1).state == SetupRunner.State.DONE || steps.get(i - 1).state == SetupRunner.State.SKIPPED);
            stepMarks.get(i).set(s.state, above);
            stepSubs.get(i).setVisibility(View.GONE);
        }
        if (count != null) {
            count.setText(digits(S.t("%s من %s", String.valueOf(finished), String.valueOf(steps.size()))));
            countBar.setShare(steps.isEmpty() ? 0 : finished / (double) steps.size());
        }
        if (r.promptLate()) say(S.tv(SetupRunner.PROMPT_LATE), RED);
        else if (r.promptWaiting()) say(S.tv(SetupRunner.PROMPT_LINE), GOLD);
        else say(r.kind() == SetupRunner.Kind.UNDO ? S.t("جارٍ الرجوع…") : S.t("جارٍ الإعداد… لا تطفئ التلفزيون."), GOLD);
        List<SetupRunner.Line> lines = r.linesSince(Math.max(0, r.lastSeq() - 1));
        String said = lines.isEmpty() ? "" : digits(lines.get(lines.size() - 1).text);
        if (now != null && !said.isEmpty()) {   // the line the work is saying goes under the step in progress, not at the bottom of the card
            TextView sub = stepSubs.get(steps.indexOf(now));
            sub.setText(said);
            sub.setVisibility(View.VISIBLE);
            detail.setText("");
        } else detail.setText(said);
        // the TV restarts inside the two measuring steps, on a TV that survives it (the plan said so): said plainly, before it happens
        SetupRunner.Preview pv = r.preview();
        boolean restarting = now != null && ("before".equals(now.id) || "after".equals(now.id)) && pv != null && pv.restarts;
        if (notice != null) {
            notice.setVisibility(restarting ? View.VISIBLE : View.GONE);
            if (restarting) notice.setText(S.t("سيعيد التلفزيون التشغيل الآن، وسيكمل بنفسه."));
        }
    }

    private static String why(String reason) { return reason == null || reason.isEmpty() ? "" : " — " + reason; }

    private void buildWifi() {
        add(text(S.t("خطوة منك قبل الشاشة الرئيسية"), 18, "Medium", TEXT), dp(8));
        add(text(S.tv(SetupRunner.WIFI_ASK_TEXT), 15, "Regular", TEXT), dp(16));
        final TextView more = button(S.t("كمّل"));
        more.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(false); } });
        add(more, dp(12));
        undo = quiet(S.t("تراجع (يرجّع التلفزيون كما كان)"));
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(true); } });
        add(undo, dp(16));
        more.post(new Runnable() { @Override public void run() { more.requestFocus(); } });
        say("", GOLD);
    }

    /** Every fixed sentence SetupRunner shows (labels, the plan, the prompt lines, the Wi-Fi question), marked for the table: tests/setup_i18n_test.py checks the list against the runner. */
    static final String[] WORDS = {
        S.n("فحص التلفزيون"),
        S.n("نسخة احتياطية من حالة التلفزيون"),
        S.n("الميزات المحمية"),
        S.n("قياس الذاكرة قبل"),
        S.n("أذونات تلفزيونك"),
        S.n("القائمة الآمنة"),
        S.n("قائمة المجتمع"),
        S.n("تسريع التلفزيون"),
        S.n("تثبيت شاشة التوقف"),
        S.n("تفعيل شاشة التوقف"),
        S.n("TV Bro و SmartTube"),
        S.n("التشغيل التلقائي"),
        S.n("الشاشة الرئيسية"),
        S.n("قياس الذاكرة بعد"),
        S.n("التقرير"),
        S.n("التراجع"),
        S.n("أجوبة قائمة المجتمع"),
        S.n("نحفظ حالة التلفزيون الآن (نسخة احتياطية) قبل أي تغيير، ونرجّع أي ميزة محمية موقوفة (البث، الصوت، الريموت، المتجر)."),
        S.n("نقيس الذاكرة قبل الإعداد وبعده: يُعاد تشغيل التلفزيون مرتين، ويكمل الإعداد وحده بعد كل مرة."),
        S.n("نقيس الذاكرة قبل الإعداد وبعده (بدون إعادة تشغيل في هذا التلفزيون)."),
        S.n("نعطي تلفزيونك أذوناته (الموقع، منبّهات الأذان، التحديثات)."),
        S.n("نوقف تطبيقات القائمة الآمنة (إعلانات وتتبّع وعروض)، ونسجّلها لنرجعها بزر واحد."),
        S.n("القائمة الآمنة: لا شيء منها شغّال في هذا التلفزيون."),
        S.n("نوقف %s تطبيقاً من القائمة الآمنة (إعلانات وتتبّع وعروض)، ونسجّلها لنرجعها بزر واحد."),
        S.n("نطابق قائمة المجتمع لطراز تلفزيونك إن وُجدت، ونسألك عن المشكوك فيه. بدون إنترنت نتركها."),
        S.n("نسرّع التلفزيون: حركات أقصر، ومتجر Play لا يعمل في الخلفية."),
        S.n("نحدّث شاشة التوقف إن وُجدت نسخة أحدث، ونفعّلها."),
        S.n("نثبّت شاشة التوقف من إصداراتنا (بعد التأكد أنها موقّعة بمفتاحنا)، ونفعّلها."),
        S.n("نسمح لتطبيقاتنا بالعمل عند تشغيل التلفزيون (الأذان ومؤقت النوم بعد إعادة التشغيل)."),
        S.n("تلفزيونك هو الشاشة الرئيسية أصلاً."),
        S.n("نجعل تلفزيونك الشاشة الرئيسية. إن ما اشتغلت خلال 5 دقائق ترجع الأصلية وحدها."),
        S.n("وقبلها نطلب منك نسيان شبكة الواي فاي والاتصال بها من جديد (هذا التلفزيون يحفظها باسم وسيط الإعداد)."),
        S.n("نكتب تقريراً بلا أي معلومة شخصية، وتقدر ترسله برمز QR إن أردت."),
        S.n("TV Bro و SmartTube مثبّتان من قبل: نتركهما."),
        S.n("ثبّت أيضاً TV Bro (متصفح) و SmartTube (يوتيوب بلا إعلانات)"),
        S.n("التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت."),
        S.n("ما ظهرت الرسالة؟ أعد تشغيل التلفزيون وافتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون"),
        S.n("ما بدأنا الإعداد، وما تغيّر شيء في التلفزيون."),
        S.n("قبل تبديل الشاشة الرئيسية: شبكة الواي فاي محفوظة باسم «وسيط الإعداد» في هذا التلفزيون، وإيقافه يمسحها. افتح إعدادات التلفزيون ← الشبكة، واختر «انسَ الشبكة»، ثم اتصل بها من جديد بنفس كلمة المرور (خلّها قدامك قبل). بعدها ارجع هنا واضغط «كمّل». ما تبي؟ اضغط «كمّل» بدونها، ونكمل والوسيط شغّال."),
    };

    /** The ends SetupRunner.endOf says in fixed words, marked for the table (the done screen shows them with S.tv; tests/tester_fixes_test.py). */
    static final String[] ENDS = {
        S.n("تم الرجوع. أطفئ «تصحيح USB» من خيارات المطوّر، واختر «إلغاء تراخيص تصحيح USB» لتنظيف التلفزيون."),
        S.n("انتهى الرجوع مع ملاحظات (فوق)."),
        S.n("توقّفنا قبل أي تغيير."),
        S.n("انتهى الإعداد. اضغط زر الرئيسية في الريموت. ثم أطفئ «تصحيح USB» من خيارات المطوّر واختر «إلغاء تراخيص تصحيح USB» إن لم تحتجهما (التراجع من هنا يحتاجهما)."),
    };

    private void buildDone(SetupRunner r) {
        final SetupRunner.End e = r.end();
        say(S.tv(e.text), e.bad ? RED : GOLD);
        if (r.kind() == SetupRunner.Kind.FULL) {
            stepsList(r);
            mode = "progress";   // the step lines are drawn the same way, once
            updateProgress(r);
            mode = "done";
            say(S.tv(e.text), e.bad ? RED : GOLD);
            String report = new FileStore(this).get(SetupReport.FILE);
            String url = report == null || report.isEmpty() ? null : SetupRunner.shareUrl(report);
            if (url != null) {
                try {
                    // the owner: «too big and dense». The packed report (SetupReport.pack) needs a far smaller code; it sits centred in a white
                    // card (its quiet zone, 16 dp around the four white modules of the code itself), the caption under it, on the 8 dp grid
                    QrView qr = new QrView(this);
                    qr.setCode(QrCode.encodeLarge(url));
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(Gravity.CENTER_HORIZONTAL);
                    card.setPadding(dp(16), dp(16), dp(16), dp(16));
                    android.graphics.drawable.GradientDrawable white = new android.graphics.drawable.GradientDrawable();
                    white.setColor(0xFFFFFFFF);
                    white.setCornerRadius(dp(16));
                    card.setBackground(white);
                    card.addView(qr, new LinearLayout.LayoutParams(dp(QR_DP), dp(QR_DP)));
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    cp.gravity = Gravity.CENTER_HORIZONTAL;
                    cp.topMargin = dp(16);
                    body.addView(card, cp);
                    TextView caption = text(S.t("امسح الرمز بجوالك لترى تقرير الإعداد وترسله للمطوّر إن أردت (بلا أي معلومة شخصية)."), 13, "Regular", DIM);
                    caption.setGravity(Gravity.CENTER_HORIZONTAL);
                    caption.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(QR_DP + 32 + 96), LinearLayout.LayoutParams.WRAP_CONTENT);
                    tp.gravity = Gravity.CENTER_HORIZONTAL;
                    tp.topMargin = dp(8);
                    tp.bottomMargin = dp(16);
                    body.addView(caption, tp);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        add(text(S.t("بعد الإعداد أطفئ التصحيح:"), 15, "Medium", TEXT), dp(4));
        add(text(digits(S.t("1. الإعدادات ← تفضيلات الجهاز ← خيارات المطوّر.")), 14, "Regular", TEXT), dp(4));
        add(text(digits(S.t("2. اختر «إلغاء تراخيص تصحيح USB».")), 14, "Regular", TEXT), dp(4));
        add(text(digits(S.t("3. أطفئ «تصحيح USB». («تراجع» من هنا يحتاجه مفعّلاً.)")), 14, "Regular", TEXT), dp(12));
        add(text(S.t("لو صار شي:"), 15, "Medium", TEXT), dp(4));
        add(text(digits(S.t("• الشاشة الرئيسية ما فتحت تلفزيونك: انتظر 5 دقائق ترجع الأصلية وحدها، أو اضغط «تراجع».")), 14, "Regular", TEXT), dp(4));
        add(text(S.t("• تطبيق تحتاجه صار موقوفاً: «تراجع» يرجّع كل ما أوقفناه."), 14, "Regular", TEXT), dp(4));
        add(text(S.t("• انقطع الواي فاي: اتصل به من إعدادات التلفزيون ← الشبكة."), 14, "Regular", TEXT), dp(4));
        add(text(S.t("• تبي التلفزيون كما كان: فعّل «تصحيح USB» واضغط «تراجع»."), 14, "Regular", TEXT), dp(12));
        final TextView ok = button(S.t("تم"));
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissed = e;
                // «تم» ends the setup: the home opens and this screen goes (build 208: nothing happened, the screen stayed in front)
                startActivity(new android.content.Intent(SetupActivity.this, sa.urtv.home.MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
        add(ok, dp(12));
        undo = quiet(S.t("تراجع (يرجّع التلفزيون كما كان)"));
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(true); } });
        add(undo, dp(12));
        ok.post(new Runnable() { @Override public void run() { ok.requestFocus(); } });
    }

    // ---- the buttons --------------------------------------------------------------------------------------------------------------

    private void go(final boolean undoIt) {
        if (running) return;
        // with a family PIN set, the PIN first
        SetupGate.guard(this, new Runnable() { @Override public void run() { goAfterPin(undoIt); } }, new Runnable() { @Override public void run() { } });
    }

    private void goAfterPin(final boolean undoIt) {
        if (running) return;
        // the run itself is the runner's (one at a time, on a thread that outlives this screen): this screen starts it and shows what it says
        SetupRunner.Options o = new SetupRunner.Options();
        o.extras = extras;
        SetupRunner.Start s = SetupRunner.get().start(undoIt ? SetupRunner.Kind.UNDO : SetupRunner.Kind.FULL, o, new SetupHost(this));
        if (s != SetupRunner.Start.STARTED) {
            say("يعمل إعداد آخر الآن؛ انتظر حتى ينتهي.", GOLD);
            return;
        }
        dismissed = null;
        show();
    }

    /** While a run is going: the screen stays on (the screensaver would cut it; SetupService also holds it, and our screensaver steps aside). */
    private void busy(boolean on) {
        if (on && !running) {
            running = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // only while a run is going (the screensaver would cut it)
        } else if (!on && running) {
            running = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void say(String t, int color) {
        status.setText(digits(t));
        status.setTextColor(color);
        // the answer to the press is in sight even when the plan above pushes it under the screen's edge
        if (!t.isEmpty()) status.post(new Runnable() { @Override public void run() { status.requestRectangleOnScreen(new android.graphics.Rect(0, 0, status.getWidth(), status.getHeight())); } });
    }

    private boolean resumedNow;

    @Override protected void onResume() {
        super.onResume();
        resumedNow = true;
        SetupRunner.get().setListener(new Runnable() {
            @Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { show(); } }); }
        });
        SetupRunner.get().screenShown();   // the home need not open this screen again
        show();   // a run started before this screen was opened (or while it was closed) is shown as it is now
    }

    @Override protected void onPause() {
        resumedNow = false;
        SetupRunner.get().setListener(null);
        super.onPause();
    }

    /** A line of the recovery attempt at the start of this screen (not a run: the runner's lines are shown by show()). */
    private void line(final String t) {
        runOnUiThread(new Runnable() { @Override public void run() { detail.setText(digits(t)); } });
    }

    // ---- views --------------------------------------------------------------------------------------------------------------------

    /** the runner's fixed Arabic sentences, in the person's language (WORDS lists them for the table) */
    private static final java.util.function.UnaryOperator<String> TR = new java.util.function.UnaryOperator<String>() { @Override public String apply(String ar) { return S.tv(ar); } };

    private String digits(String s) { return DigitsText.convert(s, Digits.latin()); }

    private void add(View v, int bottom) { body.addView(v, right(bottom)); }

    /** a button in the glass look: the panel, the solid focus fill and its ink while focused; another window over this screen cannot press it */
    private TextView button(String label) {
        TextView t = text(label, 18, "Medium", TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setMinHeight(dp(56));   // 56 dp: the main button of a screen
        t.setFocusable(true);
        t.setClickable(true);
        t.setFilterTouchesWhenObscured(true);
        sa.urtv.home.GlassPaint.card(t, look, 14);
        return t;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());   // right in Arabic, left in English («اللغة»)
        t.setTextDirection(Dir.text());
        Typeface f = typeface(weight);
        if (f != null) t.setTypeface(f);
        return t;
    }

    private Typeface typeface(String weight) {
        try { return Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf"); }
        catch (RuntimeException ignored) { return null; }
    }

    /** a second choice beside the primary button: no panel at rest, the dim words; the focus fill and its ink when focused (the one filled button of a screen is the main one) */
    private TextView quiet(String label) {
        final TextView t = button(label);
        final android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, sa.urtv.home.GlassPaint.focused(this, look, 14));
        bg.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
        t.setBackground(bg);
        t.setTextColor(DIM);
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() { public void onFocusChange(View v, boolean has) { t.setTextColor(has ? INK : DIM); } });
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
