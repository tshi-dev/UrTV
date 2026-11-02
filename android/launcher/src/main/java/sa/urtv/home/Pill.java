package sa.urtv.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A pill of the home screen's top row: its icon only, a round button; the one that has the focus widens (about 150 ms) to show
 * its name beside the icon, and shrinks back when the focus leaves. The height is fixed, so nothing in the row moves up or
 * down: the row only grows sideways while a name is out. The icon is on the right (the row reads from the right), the name
 * comes out to its left; PillText says what the name is. The name is always the pill's content description.
 */
final class Pill extends LinearLayout {
    private static final int ON = 0xE6FFFFFF, FOCUSED = 0xFF111111, GOLD = 0xFFD4AF6A;
    private static final long MS = 150;
    /** the row's height in dp: every pill, the circles and the one with the focus, is this tall, so they share one centre line */
    static final int HEIGHT = 34;

    private final float density;
    private final ImageView icon;
    private final TextView name;
    private final FrameLayout clip;   // the name's window: its width is what the animation changes
    private final GradientDrawable bg = new GradientDrawable();
    private final Drawable glyph;
    private String label;
    private float open;   // 0 = icon only, 1 = the name out
    private boolean accent;   // gold icon: something is running (the sleep timer)
    private ValueAnimator anim;

    Pill(Context c, String label, int iconRes, Typeface face) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        bg.setCornerRadius(dp(HEIGHT / 2f));
        bg.setColor(0x66000000);
        bg.setStroke(dp(1), 0x33FFFFFF);
        setBackground(bg);

        glyph = c.getDrawable(iconRes).mutate();
        icon = new ImageView(c);
        icon.setImageDrawable(glyph);
        addView(icon, new LayoutParams(dp(18), dp(18)));

        name = new TextView(c);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setIncludeFontPadding(false);
        name.setTypeface(face);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        clip = new FrameLayout(c);
        clip.setClipChildren(true);
        clip.addView(name, new FrameLayout.LayoutParams(-2, -1, Dir.start()));   // wider than its window: the start of the name shows first
        addView(clip, new LayoutParams(0, -1));

        setLabel(label);
        apply();
    }

    /** the full name (the focused pill shows it); the sleep and network pills change theirs */
    void setLabel(String text) {
        text = S.tv(text);   // a name given as our Arabic words (ControlText.NAME) is said in «اللغة»; one already said stays
        label = text;
        setContentDescription(text);
        apply();
    }

    /** the icon in gold while something runs (the sleep timer), so an unfocused pill still says so */
    void setAccent(boolean on) { accent = on; paint(); }

    @Override protected void onFocusChanged(boolean gained, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gained, direction, previouslyFocusedRect);
        paint();
        if (anim != null) anim.cancel();
        float to = gained ? 1f : 0f;
        anim = ValueAnimator.ofFloat(open, to);
        anim.setDuration(Math.max(30, Math.round(MS * Math.abs(to - open))));   // what is left of the way, at the same speed
        anim.addUpdateListener(a -> { open = (float) a.getAnimatedValue(); apply(); });
        anim.start();
    }

    private void paint() {
        boolean f = hasFocus();
        bg.setColor(f ? 0xF2FFFFFF : 0x66000000);
        name.setTextColor(f ? FOCUSED : ON);
        glyph.setTint(f ? FOCUSED : accent ? GOLD : ON);
    }

    /** the width of the name's window now: nothing when closed, the name and the gap to the icon when open */
    private void apply() {
        float textWidth = name.getPaint().measureText(label == null ? "" : label);
        int full = Math.round(dp(9) + textWidth) + dp(2);
        int pad = Math.round(dp(8) + dp(8) * open);   // a 34 dp circle closed (18 dp icon, 8 dp around), 16 dp of room on each side open
        setPadding(pad, 0, pad, 0);
        LayoutParams lp = (LayoutParams) clip.getLayoutParams();
        int w = Math.round(full * open);
        if (lp.width != w) { lp.width = w; clip.setLayoutParams(lp); }
        // the name is always laid out whole (an exact width), right edge on the icon's side; the window above only shows part of it
        FrameLayout.LayoutParams nlp = (FrameLayout.LayoutParams) name.getLayoutParams();
        if (nlp.width != full) { nlp.width = full; name.setLayoutParams(nlp); }
        name.setPadding(0, 0, dp(9), 0);   // the gap to the icon, on the name's right
        String shown = PillText.shown(label, open > 0);   // icon only while closed: no words at all, not hidden ones
        if (!shown.contentEquals(name.getText())) name.setText(shown);
    }

    private int dp(float v) { return Math.round(v * density); }
}
