package sa.urtv.home.setup;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import sa.urtv.home.CalmBar;
import sa.urtv.home.Dir;
import sa.urtv.home.GlassPaint;
import sa.urtv.home.GlassStyle;

/**
 * «إعداد تلفزيوني», the progress and the done screens (design/43, mockup 11 and better): five phases as a compact stepper at the start (done = a tick, now = the accent, next = dim),
 * and the main area for where the run is: the step in hand (its name big, the line the work is saying, a calm breathing indicator, the share and the time), or, while the TV waits for
 * «السماح بتصحيح USB؟», one instruction card with a picture of that dialog and its «السماح دائماً» lit, which takes the whole main area until the TV is answered; at the end the result.
 * Everything is in the glass look's colours (dark and light), laid out from the start side (right in Arabic), inside the screen's safe area. The words come in already said (Model).
 */
final class SetupProgressView extends LinearLayout {
    static final int WAIT = 0, NOW = 1, DONE = 2, FAILED = 3;

    /** one phase of the run: its name and where it is */
    static final class Phase {
        final String name;
        int state;
        Phase(String name, int state) { this.name = name; this.state = state; }
    }

    /** everything the view shows, as words and numbers (made by SetupActivity) */
    static final class Model {
        List<Phase> phases;
        String stepName = "", doing = "", counts = "", elapsed = "", restart = "";
        int percent;
        boolean prompt, promptLate, finished, bad;
        String artOk = "", artAlways = "", promptTitle = "", promptHint = "", promptLateText = "", artTitle = "", artCancel = "", artAllow = "", result = "";
    }

    private final GlassStyle.Look look;
    private final Typeface medium, regular;
    private final LinearLayout stepper;
    private final LinearLayout running, promptCard, resultCard;
    private final TextView stepName, doing, percent, counts, elapsed, restart, promptTitle, promptHint, promptLate, resultText, resultSub;
    private final CalmBar bar;
    private final Breath breath;
    private final DialogArt art;
    private final CheckBadge badge;

    SetupProgressView(Context c, GlassStyle.Look look, Typeface medium, Typeface regular) {
        super(c);
        this.look = look;
        this.medium = medium;
        this.regular = regular;
        // 4.4 compact (the owner: «toooo much»): in the 560 dp window the five steps are ONE row of plain words above the work (no side rail)
        setOrientation(VERTICAL);
        setLayoutDirection(Dir.layout());
        stepper = new LinearLayout(c);
        stepper.setOrientation(HORIZONTAL);
        stepper.setLayoutDirection(Dir.layout());
        addView(stepper, new LayoutParams(-1, -2));

        FrameLayout main = new FrameLayout(c);
        LayoutParams mp = new LayoutParams(-1, -2);
        mp.topMargin = dp(16);
        addView(main, mp);

        running = panel();
        breath = new Breath(c);
        running.addView(breath, new LayoutParams(dp(96), dp(24)));
        stepName = text(22, true, look.text);
        running.addView(stepName, margin(dp(20), 0));
        doing = text(17, false, look.sub);
        doing.setMinLines(2);
        running.addView(doing, margin(dp(8), 0));
        LinearLayout row = new LinearLayout(c);
        row.setLayoutDirection(Dir.layout());
        row.setGravity(Gravity.BOTTOM);
        percent = text(22, true, look.accent);
        row.addView(percent, new LayoutParams(-2, -2));
        counts = text(14, false, look.sub);
        LayoutParams cp = new LayoutParams(0, -2, 1f);
        cp.setMarginStart(dp(24));
        cp.bottomMargin = dp(10);
        row.addView(counts, cp);
        running.addView(row, margin(dp(32), 0));
        bar = new CalmBar(c, look.line, look.accent);
        running.addView(bar, margin(dp(8), 0));
        elapsed = text(14, false, look.sub);
        running.addView(elapsed, margin(dp(16), 0));
        restart = text(17, true, look.text);
        restart.setPadding(dp(12), dp(8), dp(12), dp(8));
        restart.setBackground(GlassPaint.fill(c, GlassStyle.over(look.dialog, look.base), 14, look.accent));
        restart.setVisibility(GONE);
        running.addView(restart, margin(dp(16), 0));
        main.addView(running);

        promptCard = panel();
        promptTitle = text(17, true, look.text);
        promptCard.addView(promptTitle, margin(0, 0));
        art = new DialogArt(c);
        promptCard.addView(art, margin(dp(12), 0, dp(220)));   // 4.4: the dialog and the four keys under it
        promptHint = text(17, true, look.accent);
        promptCard.addView(promptHint, margin(dp(16), 0));
        promptLate = text(14, false, look.warn);
        promptLate.setVisibility(GONE);
        promptCard.addView(promptLate, margin(dp(12), 0));
        promptCard.setVisibility(GONE);
        main.addView(promptCard);

        resultCard = panel();
        resultCard.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        badge = new CheckBadge(c);
        resultCard.addView(badge, new LayoutParams(dp(56), dp(56)));
        resultText = text(22, true, look.text);
        resultCard.addView(resultText, margin(dp(24), 0));
        resultSub = text(14, false, look.sub);
        resultCard.addView(resultSub, margin(dp(8), 0));
        resultCard.setVisibility(GONE);
        main.addView(resultCard);
    }

    void show(Model m) {
        stepper.removeAllViews();
        List<Phase> ph = m.phases;
        for (int i = 0; i < ph.size(); i++) {
            Phase p = ph.get(i);
            // 4.4 (the owner: no pills; compact): plain words in one row, the step now in the accent (gold), bold, with a thin line under it; the others dim,
            // a failed one in red; nothing rounded behind them
            LinearLayout w = new LinearLayout(getContext());
            w.setOrientation(VERTICAL);
            TextView t = text(17, p.state == NOW, p.state == NOW ? look.accent : p.state == FAILED ? look.warn : look.sub);
            t.setAlpha(p.state == WAIT ? 0.7f : 1f);
            t.setText(p.name);
            w.addView(t, new LayoutParams(-2, -2));
            View line = new View(getContext());
            line.setBackgroundColor(p.state == NOW ? look.accent : 0x00000000);
            LayoutParams lp = new LayoutParams(-1, dp(2));
            lp.topMargin = dp(4);
            w.addView(line, lp);
            LayoutParams wp = new LayoutParams(-2, -2);
            if (i > 0) wp.setMarginStart(dp(20));
            stepper.addView(w, wp);
        }
        boolean showPrompt = m.prompt && !m.finished;
        promptCard.setVisibility(showPrompt ? VISIBLE : GONE);
        resultCard.setVisibility(m.finished ? VISIBLE : GONE);
        running.setVisibility(!showPrompt && !m.finished ? VISIBLE : GONE);
        if (showPrompt) {
            promptTitle.setText(m.promptTitle);
            promptHint.setText(m.promptHint);
            art.set(m.artTitle, m.artCancel, m.artAllow, m.artOk, m.artAlways);
            promptLate.setVisibility(m.promptLate ? VISIBLE : GONE);
            promptLate.setText(m.promptLateText);
            breath.stop();
            art.pulse(true);
        } else if (m.finished) {
            badge.set(!m.bad);
            resultText.setText(m.result);
            resultSub.setText(m.counts + (m.elapsed.isEmpty() ? "" : "، " + m.elapsed));   // no «·» between them (the owner's rule)
            breath.stop();
            art.pulse(false);
        } else {
            stepName.setText(m.stepName);
            doing.setText(m.doing);
            percent.setText(m.percent + (Dir.rtl() ? "٪" : "%"));
            counts.setText(m.counts);
            bar.setShare(m.percent / 100.0);
            elapsed.setText(m.elapsed);
            restart.setVisibility(m.restart.isEmpty() ? GONE : VISIBLE);
            restart.setText(m.restart);
            breath.start();
            art.pulse(false);
        }
    }

    /** only the time: the screen ticks it every second without rebuilding anything */
    void tick(String elapsedText) { elapsed.setText(elapsedText); }

    @Override protected void onDetachedFromWindow() { breath.stop(); art.pulse(false); super.onDetachedFromWindow(); }

    // ---- pieces

    private LinearLayout panel() {
        LinearLayout p = new LinearLayout(getContext());
        p.setOrientation(VERTICAL);
        p.setLayoutDirection(Dir.layout());
        p.setPadding(dp(20), dp(16), dp(20), dp(16));
        p.setBackground(GlassPaint.panel(getContext(), look, 20));
        return p;
    }

    private TextView text(float sp, boolean bold, int color) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(bold ? medium : regular);
        t.setGravity(Dir.start());
        t.setTextDirection(Dir.text());
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(4), 1f);
        return t;
    }

    private LayoutParams margin(int top, int bottom) { LayoutParams p = new LayoutParams(-1, -2); p.topMargin = top; p.bottomMargin = bottom; return p; }
    private LayoutParams margin(int top, int bottom, int h) { LayoutParams p = new LayoutParams(-1, h); p.topMargin = top; p.bottomMargin = bottom; return p; }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** a phase's mark on its rail: a tick on the accent (done), a ring with a dot (now), a hollow ring (next), a cross on red (failed); the rail runs through, the accent as far as the work has come */
    private final class Mark extends View {
        private final int state;
        private final boolean first, last, aboveDone;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Mark(Context c, int state, boolean first, boolean last, boolean aboveDone) {
            super(c);
            this.state = state; this.first = first; this.last = last; this.aboveDone = aboveDone;
        }

        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = dp(13), rail = dp(2);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(rail);
            p.setColor(aboveDone ? look.accent : look.line);
            if (!first) c.drawRect(cx - rail / 2, 0, cx + rail / 2, cy - r, p);
            p.setColor(state == DONE ? look.accent : look.line);
            if (!last) c.drawRect(cx - rail / 2, cy + r, cx + rail / 2, getHeight(), p);
            int ink = look.light ? 0xFFFFFFFF : 0xFF14161A;
            if (state == DONE || state == FAILED) {
                p.setColor(state == DONE ? look.accent : look.warn);
                c.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(3));
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(ink);
                Path path = new Path();
                if (state == DONE) { path.moveTo(cx - r * .42f, cy + r * .02f); path.lineTo(cx - r * .1f, cy + r * .36f); path.lineTo(cx + r * .46f, cy - r * .32f); }
                else { path.moveTo(cx - r * .32f, cy - r * .32f); path.lineTo(cx + r * .32f, cy + r * .32f); path.moveTo(cx + r * .32f, cy - r * .32f); path.lineTo(cx - r * .32f, cy + r * .32f); }
                c.drawPath(path, p);
            } else if (state == NOW) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(3));
                p.setColor(look.accent);
                c.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, r * .42f, p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                p.setColor(look.line);
                c.drawCircle(cx, cy, r, p);
            }
        }
    }

    /** the calm indicator: three dots that rise and fall one after the other, about two seconds a round; nothing runs while the screen is not showing the work */
    private final class Breath extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator anim;
        private float t;

        Breath(Context c) { super(c); }

        void start() {
            if (anim != null && anim.isRunning()) return;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(2000);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(a -> { t = (float) a.getAnimatedValue(); invalidate(); });
            anim.start();
        }

        void stop() { if (anim != null) { anim.cancel(); anim = null; } }

        @Override protected void onDraw(Canvas c) {
            float r = dp(6), gap = dp(26), cy = getHeight() / 2f;
            float x0 = Dir.rtl() ? getWidth() - r : r, dir = Dir.rtl() ? -1 : 1;
            for (int i = 0; i < 3; i++) {
                float ph = (t - i * 0.16f + 1f) % 1f;
                float k = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * ph));   // 0..1..0
                p.setColor(look.accent);
                p.setAlpha(Math.round(70 + 185 * k));
                c.drawCircle(x0 + dir * i * gap, cy - dp(5) * k, r * (0.8f + 0.35f * k), p);
            }
        }
    }

    /** a tick in a circle, or a cross: the result of the run */
    private final class CheckBadge extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean good = true;
        CheckBadge(Context c) { super(c); }
        void set(boolean good) { this.good = good; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - dp(2);
            p.setStyle(Paint.Style.FILL);
            p.setColor(good ? look.accent : look.warn);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(6));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(look.light ? 0xFFFFFFFF : 0xFF14161A);
            Path path = new Path();
            if (good) { path.moveTo(cx - r * .38f, cy + r * .02f); path.lineTo(cx - r * .1f, cy + r * .32f); path.lineTo(cx + r * .42f, cy - r * .3f); }
            else { path.moveTo(cx - r * .3f, cy - r * .3f); path.lineTo(cx + r * .3f, cy + r * .3f); path.moveTo(cx + r * .3f, cy - r * .3f); path.lineTo(cx - r * .3f, cy + r * .3f); }
            c.drawPath(path, p);
        }
    }

    /**
     * The picture of what the TV is showing: its dialog («السماح بتصحيح USB؟») with the box «السماح دائماً من هذا الكمبيوتر» and its two buttons, and under
     * it the four keys of the remote in order. 4.4 (step 88): on the TV the focus starts on the box, and DOWN from it lands on «إلغاء», so the way is
     * اختيار (the box) ← تحت ← يسار ← اختيار («السماح», lit, with a ring that breathes). It is a drawing made for this screen, not the TV's own dialog.
     */
    private final class DialogArt extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String title = "", cancel = "", allow = "", ok = "", always = "";
        private ValueAnimator anim;
        private float t;

        DialogArt(Context c) { super(c); }

        void set(String title, String cancel, String allow, String ok, String always) { this.title = title; this.cancel = cancel; this.allow = allow; this.ok = ok; this.always = always; invalidate(); }

        void pulse(boolean on) {
            if (!on) { if (anim != null) { anim.cancel(); anim = null; } return; }
            if (anim != null && anim.isRunning()) return;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1400);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.addUpdateListener(a -> { t = (float) a.getAnimatedValue(); invalidate(); });
            anim.start();
        }

        private void label(Canvas c, String s, float cx, float cy, float sp, int color, boolean bold) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
            p.setTypeface(bold ? medium : regular);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, cx, cy - (p.ascent() + p.descent()) / 2f, p);
        }

        /** one key of the remote: its word in a thin outline (a keycap, not a button of this screen) and its number above */
        private void key(Canvas c, String word, int n, float cx, float cy, float w) {
            float h = dp(32);
            RectF k = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(look.accent);
            c.drawRoundRect(k, dp(6), dp(6), p);
            label(c, word, cx, cy, 14, look.text, true);
            label(c, String.valueOf(n), cx, k.top - dp(12), 14, look.sub, false);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), dw = Math.min(w, dp(440)), left = (w - dw) / 2f, top = dp(4), dh = h - dp(80);
            RectF box = new RectF(left, top, left + dw, top + dh);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF2A2C31);
            c.drawRoundRect(box, dp(18), dp(18), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1));
            p.setColor(0x33FFFFFF);
            c.drawRoundRect(box, dp(18), dp(18), p);
            label(c, title, box.centerX(), box.top + dh * 0.2f, 17, 0xFFF1F1F3, true);
            // the box «السماح دائماً من هذا الكمبيوتر», ticked (step 1)
            float cy = box.top + dh * 0.45f, bs = dp(22), bx = box.left + dp(32);
            RectF tick = new RectF(bx, cy - bs / 2f, bx + bs, cy + bs / 2f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(look.accent);
            c.drawRoundRect(tick, dp(4), dp(4), p);
            Path v = new Path();
            v.moveTo(tick.left + bs * .22f, tick.centerY());
            v.lineTo(tick.left + bs * .42f, tick.bottom - bs * .25f);
            v.lineTo(tick.right - bs * .2f, tick.top + bs * .25f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setColor(look.light ? 0xFFFFFFFF : 0xFF14161A);
            c.drawPath(v, p);
            p.setTextAlign(Paint.Align.LEFT);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE1E2E6);
            p.setTypeface(regular);
            p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
            c.drawText(always, tick.right + dp(12), cy - (p.ascent() + p.descent()) / 2f, p);
            // the two buttons as the TV places them: «السماح» on the LEFT (step 4), «إلغاء» on the right (where DOWN lands, step 2)
            float bw = (dw - dp(72)) / 2f, bh = dp(40), by = box.bottom - dp(16) - bh;
            RectF a = new RectF(box.left + dp(24), by, box.left + dp(24) + bw, by + bh);
            RectF b = new RectF(box.right - dp(24) - bw, by, box.right - dp(24), by + bh);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x14FFFFFF);
            c.drawRoundRect(b, dp(12), dp(12), p);
            label(c, cancel, b.centerX(), b.centerY(), 14, 0xFFB9BBC2, false);
            float grow = dp(4) + dp(4) * t;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setColor((look.accent & 0x00FFFFFF) | Math.round(60 + 120 * (1 - t)) << 24);
            RectF ring = new RectF(a.left - grow, a.top - grow, a.right + grow, a.bottom + grow);
            c.drawRoundRect(ring, dp(16), dp(16), p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(look.accent);
            c.drawRoundRect(a, dp(12), dp(12), p);
            label(c, allow, a.centerX(), a.centerY(), 17, look.light ? 0xFFFFFFFF : 0xFF14161A, true);
            // the remote's four keys in order, in the reading direction of the screen: اختيار، تحت (▼)، يسار (◀)، اختيار
            String[] keys = {ok, "▼", "◀", ok};
            float kw = dp(72), gap = dp(20), total = 4 * kw + 3 * gap, ky = box.bottom + dp(44);
            boolean rtl = Dir.rtl();
            for (int i = 0; i < 4; i++) {
                int slot = rtl ? 3 - i : i;
                float kx = (w - total) / 2f + slot * (kw + gap) + kw / 2f;
                key(c, keys[i], i + 1, kx, ky, kw);
                if (i < 3) label(c, rtl ? "←" : "→", kx + (rtl ? -1 : 1) * (kw / 2f + gap / 2f), ky, 14, look.sub, false);
            }
        }
    }
}
