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
        String artOk = "", promptTitle = "", promptHint = "", promptLateText = "", artTitle = "", artCancel = "", artAllow = "", result = "";
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
        setOrientation(HORIZONTAL);
        setLayoutDirection(Dir.layout());
        stepper = new LinearLayout(c);
        stepper.setOrientation(VERTICAL);
        stepper.setLayoutDirection(Dir.layout());
        addView(stepper, new LayoutParams(dp(232), -2));

        FrameLayout main = new FrameLayout(c);
        LayoutParams mp = new LayoutParams(0, -2, 1f);
        mp.setMarginStart(dp(32));
        addView(main, mp);

        running = panel();
        breath = new Breath(c);
        running.addView(breath, new LayoutParams(dp(96), dp(24)));
        stepName = text(34, true, look.text);
        running.addView(stepName, margin(dp(20), 0));
        doing = text(19, false, look.sub);
        doing.setMinLines(2);
        running.addView(doing, margin(dp(8), 0));
        LinearLayout row = new LinearLayout(c);
        row.setLayoutDirection(Dir.layout());
        row.setGravity(Gravity.BOTTOM);
        percent = text(64, true, look.accent);
        row.addView(percent, new LayoutParams(-2, -2));
        counts = text(19, false, look.sub);
        LayoutParams cp = new LayoutParams(0, -2, 1f);
        cp.setMarginStart(dp(24));
        cp.bottomMargin = dp(10);
        row.addView(counts, cp);
        running.addView(row, margin(dp(32), 0));
        bar = new CalmBar(c, look.line, look.accent);
        running.addView(bar, margin(dp(8), 0));
        elapsed = text(17, false, look.sub);
        running.addView(elapsed, margin(dp(16), 0));
        restart = text(18, true, look.text);
        restart.setPadding(dp(16), dp(12), dp(16), dp(12));
        restart.setBackground(GlassPaint.fill(c, GlassStyle.over(look.dialog, look.base), 14, look.accent));
        restart.setVisibility(GONE);
        running.addView(restart, margin(dp(16), 0));
        main.addView(running);

        promptCard = panel();
        promptTitle = text(28, true, look.text);
        promptCard.addView(promptTitle, margin(0, 0));
        art = new DialogArt(c);
        promptCard.addView(art, margin(dp(16), 0, dp(236)));
        promptHint = text(24, true, look.accent);
        promptCard.addView(promptHint, margin(dp(16), 0));
        promptLate = text(18, false, look.warn);
        promptLate.setVisibility(GONE);
        promptCard.addView(promptLate, margin(dp(12), 0));
        promptCard.setVisibility(GONE);
        main.addView(promptCard);

        resultCard = panel();
        resultCard.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        badge = new CheckBadge(c);
        resultCard.addView(badge, new LayoutParams(dp(88), dp(88)));
        resultText = text(32, true, look.text);
        resultCard.addView(resultText, margin(dp(24), 0));
        resultSub = text(19, false, look.sub);
        resultCard.addView(resultSub, margin(dp(8), 0));
        resultCard.setVisibility(GONE);
        main.addView(resultCard);
    }

    void show(Model m) {
        stepper.removeAllViews();
        List<Phase> ph = m.phases;
        for (int i = 0; i < ph.size(); i++) {
            Phase p = ph.get(i);
            LinearLayout r = new LinearLayout(getContext());
            r.setOrientation(HORIZONTAL);
            r.setLayoutDirection(Dir.layout());
            r.setGravity(Gravity.CENTER_VERTICAL);
            boolean aboveDone = i > 0 && ph.get(i - 1).state == DONE;
            r.addView(new Mark(getContext(), p.state, i == 0, i == ph.size() - 1, aboveDone), new LayoutParams(dp(36), dp(64)));
            TextView t = text(p.state == NOW ? 24 : 21, p.state == NOW, p.state == WAIT ? look.sub : p.state == FAILED ? look.warn : look.text);
            t.setAlpha(p.state == WAIT ? 0.7f : 1f);
            t.setText(p.name);
            LayoutParams tp = new LayoutParams(0, -2, 1f);
            tp.setMarginStart(dp(12));
            r.addView(t, tp);
            stepper.addView(r, new LayoutParams(-1, dp(64)));
        }
        boolean showPrompt = m.prompt && !m.finished;
        promptCard.setVisibility(showPrompt ? VISIBLE : GONE);
        resultCard.setVisibility(m.finished ? VISIBLE : GONE);
        running.setVisibility(!showPrompt && !m.finished ? VISIBLE : GONE);
        if (showPrompt) {
            promptTitle.setText(m.promptTitle);
            promptHint.setText(m.promptHint);
            art.set(m.artTitle, m.artCancel, m.artAllow, m.artOk);
            promptLate.setVisibility(m.promptLate ? VISIBLE : GONE);
            promptLate.setText(m.promptLateText);
            breath.stop();
            art.pulse(true);
        } else if (m.finished) {
            badge.set(!m.bad);
            resultText.setText(m.result);
            resultSub.setText(m.counts + (m.elapsed.isEmpty() ? "" : " · " + m.elapsed));
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
        p.setPadding(dp(32), dp(28), dp(32), dp(28));
        p.setBackground(GlassPaint.panel(getContext(), look, 20));
        p.setMinimumHeight(dp(400));
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
     * The picture of what the TV is showing: its dialog («السماح بتصحيح USB؟») with the two buttons, «السماح دائماً» lit with a ring that breathes, and an «OK» key under it. It is a drawing
     * made for this screen, not the TV's own dialog (that one is the system's and looks different on each TV).
     */
    private final class DialogArt extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String title = "", cancel = "", allow = "", ok = "";
        private ValueAnimator anim;
        private float t;

        DialogArt(Context c) { super(c); }

        void set(String title, String cancel, String allow, String ok) { this.title = title; this.cancel = cancel; this.allow = allow; this.ok = ok; invalidate(); }

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

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), dw = Math.min(w, dp(520)), left = (w - dw) / 2f, top = dp(4), dh = h - dp(60);
            RectF box = new RectF(left, top, left + dw, top + dh);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF2A2C31);
            c.drawRoundRect(box, dp(18), dp(18), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1));
            p.setColor(0x33FFFFFF);
            c.drawRoundRect(box, dp(18), dp(18), p);
            label(c, title, box.centerX(), box.top + dh * 0.28f, 22, 0xFFF1F1F3, true);
            float bw = (dw - dp(72)) / 2f, bh = dp(54), by = box.bottom - dp(24) - bh;
            boolean rtl = Dir.rtl();
            RectF a = new RectF(rtl ? box.right - dp(24) - bw : box.left + dp(24), by, rtl ? box.right - dp(24) : box.left + dp(24) + bw, by + bh);   // «السماح دائماً» at the start
            RectF b = new RectF(rtl ? box.left + dp(24) : box.right - dp(24) - bw, by, rtl ? box.left + dp(24) + bw : box.right - dp(24), by + bh);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x14FFFFFF);
            c.drawRoundRect(b, dp(12), dp(12), p);
            label(c, cancel, b.centerX(), b.centerY(), 18, 0xFFB9BBC2, false);
            float grow = dp(4) + dp(4) * t;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setColor((look.accent & 0x00FFFFFF) | Math.round(60 + 120 * (1 - t)) << 24);
            RectF ring = new RectF(a.left - grow, a.top - grow, a.right + grow, a.bottom + grow);
            c.drawRoundRect(ring, dp(16), dp(16), p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(look.accent);
            c.drawRoundRect(a, dp(12), dp(12), p);
            label(c, allow, a.centerX(), a.centerY(), 20, look.light ? 0xFFFFFFFF : 0xFF14161A, true);
            // the remote's OK key, under the lit button
            float kw = dp(84), kh = dp(36), kx = a.centerX() - kw / 2f, ky = box.bottom + dp(12);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x26FFFFFF);
            c.drawRoundRect(new RectF(kx, ky, kx + kw, ky + kh), dp(10), dp(10), p);
            label(c, ok, kx + kw / 2f, ky + kh / 2f, 17, look.text, true);
            Path arrow = new Path();   // a small arrow from the key up to the button
            arrow.moveTo(a.centerX(), ky - dp(2));
            arrow.lineTo(a.centerX() - dp(6), ky - dp(9));
            arrow.lineTo(a.centerX() + dp(6), ky - dp(9));
            arrow.close();
            p.setColor(look.accent);
            c.drawPath(arrow, p);
        }
    }
}
