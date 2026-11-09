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
        private static final long MS = 150;
    /** the row's height in dp: every pill, the circles and the one with the focus, is this tall, so they share one centre line */
    static final int HEIGHT = 30;
    /** dp between two buttons of the top row (and the now-playing card): 4 dp = 8 px at 1080p, on the 8 px scale (design/43: one bar of squares, 8 px apart) */
    static final int GAP = 4;

    private final float density;
    private final ImageView icon;
    private final TextView name;
    private final FrameLayout clip;   // the name's window: its width is what the animation changes
    private final GradientDrawable bg = new GradientDrawable();
    private Drawable glyph;
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
        bg.setCornerRadius(dp(8));   // a rounded square of 60 px at 1080p, no longer a circle; the bar behind the row is PillBar's
        setBackground(bg);

        glyph = c.getDrawable(iconRes).mutate();
        icon = new ImageView(c);
        icon.setImageDrawable(glyph);
        addView(icon, new LayoutParams(dp(16), dp(16)));

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

    /** another icon (the control center's pill is the settings button in «UrTV النظيف») */
    void setIcon(int res) { glyph = getContext().getDrawable(res).mutate(); icon.setImageDrawable(glyph); paint(); }

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

    /**
     * The look of the home now (design/43): at rest nothing of its own (the bar behind the row, PillBar, is the panel), focused the look's solid focus fill
     * with its line in the focus style, the words and the icon in the look's ink. The same for every look.
     */
    void look(GlassStyle.Look l) {
        if (l == null) return;
        look = l;
        paint();
    }

    private GlassStyle.Look look = GlassStyle.DARK;

    private void paint() {
        boolean f = hasFocus();
        GlassStyle.Look l = look;
        bg.setColor(f ? l.focus : 0);
        String style = GlassStyle.focusStyle();
        if (!f) bg.setStroke(0, 0);
        else if ("frame".equals(style)) bg.setStroke(dp(3), l.accent);
        else if ("glow".equals(style)) bg.setStroke(dp(4), (l.accent & 0x00FFFFFF) | 0x80000000);
        else if (l.ring != 0) bg.setStroke(dp(2), l.ring);
        else bg.setStroke(0, 0);
        name.setTextColor(f ? l.onFocus : l.text);
        glyph.setTint(f ? l.onFocus : accent ? l.accent : l.text);
    }

    /** the width of the name's window now: nothing when closed, the name and the gap to the icon when open */
    private void apply() {
        float textWidth = name.getPaint().measureText(label == null ? "" : label);
        int full = Math.round(dp(9) + textWidth) + dp(2);
        int pad = Math.round(dp(7) + dp(9) * open);   // a 30 dp square closed (16 dp icon, 7 dp around), 16 dp of room on each side open
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
