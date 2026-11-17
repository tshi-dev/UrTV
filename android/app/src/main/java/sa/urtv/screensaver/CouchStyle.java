package sa.urtv.screensaver;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

/**
 * 4.4 the owner: with the look «منظر وساعة» the settings are couch-tv's, exactly (Seagey/couch-tv, MIT: tv-home/web/app.css «Home settings»;
 * docs/mockups/settings-couchtv/, approved). Its sizes are in its rem, a 120th of the screen's width (16 px on 1920), so they are the same share
 * of any screen; its colours and its 200 ms cubic-bezier(.2,.8,.2,1). Plain numbers and three small drawings (the toggle's ring, the choice's
 * tick, the chevron); SettingsActivity builds the views with them. The other looks keep the settings as they are.
 */
final class CouchStyle {
    final float rem;   // px of one couch-tv rem on this screen

    CouchStyle(Context c) { rem = c.getResources().getDisplayMetrics().widthPixels / 120f; }

    int px(float r) { return Math.round(r * rem); }

    // the screen: the home under a dark veil (blurred: the backdrop is the home's photo made small and blurred); a weak TV a nearly solid one
    static final int VEIL = 0xCC0A080C, VEIL_LITE = 0xF20A080C;
    // sizes, in rem
    static final float SAFE_X = 5f, SAFE_Y = 3.75f, SIDE = 36f, GAP = 4f;
    static final float TITLE = 2.875f, TITLE_LINE = 3.75f, TITLE_TOP = 2.25f, TITLE_BOTTOM = 2.5f, TITLE_SIDE = 1.25f, FIND = 3.75f;
    static final float SEC_H = 4.75f, SEC_TEXT = 1.75f, SEC_ICON_BOX = 3.25f, SEC_ICON = 1.875f, SEC_GAP = 0.375f, SEC_RADIUS = 1.5f, SEC_ICON_RADIUS = 1f;
    static final float CRUMB = 1.3125f, CRUMB_H = 2f, HEAD = 2.375f, HEAD_LINE = 3f, PAD = 1.75f;
    static final float ROW_H = 4.75f, ROW_TEXT = 1.75f, ROW_SUB = 1.25f, ROW_GAP = 0.5f, ROW_RADIUS = 1.5f, ROW_PAD_Y = 0.75f;
    static final float SHEAD = 1.25f, SHEAD_TOP = 1.125f, HINT = 1.25f, BOX = 2.25f, MARK = 1.875f, THUMB_W = 5.5f, THUMB_H = 3.125f, THUMB_RADIUS = 0.625f;
    static final float TYPE_TEXT = 2.25f, TYPE_LABEL = 1.3125f;
    // colours
    static final int WHITE = 0xFFFFFFFF, INK = 0xFF111111, TEXT = 0xEBFFFFFF, SEC = 0xC2FFFFFF, VALUE = 0x9EFFFFFF, SUB = 0x99FFFFFF, DIM = 0x80FFFFFF,
        ROW = 0x14FFFFFF, SEC_ON = 0x21FFFFFF, ICON_BOX = 0x1AFFFFFF, ICON_BOX_FOCUS = 0x12000000, INK_SUB = 0x99000000, TYPE_BG = 0x1FFFFFFF,
        GREEN = 0xFF30D158, RING = 0x6BFFFFFF, RING_FOCUS = 0x52000000;
    static final float SEC_SCALE = 1.04f, ROW_SCALE = 1.02f;
    static final long MS = Motion.MS;
    static android.view.animation.Interpolator ease() { return Motion.ease(); }   // couch-tv's cubic-bezier(.2,.8,.2,1), the same everywhere

    /** a card: white when focused, else `rest` (a section: SEC_ON when it is the one shown) */
    Drawable card(int rest, int selected, float radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, round(WHITE, radius));
        s.addState(new int[] {android.R.attr.state_selected}, round(selected, radius));
        s.addState(new int[] {}, rest == 0 ? new ColorDrawable(0) : round(rest, radius));
        return s;
    }
    GradientDrawable round(int color, float radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(px(radius)); return g; }

    /** a text's colour: ink on the white focus, else its own (a section's brighter while it is the one shown) */
    static ColorStateList ink(int rest, int selected, int focused) {
        return new ColorStateList(new int[][] {{android.R.attr.state_focused}, {android.R.attr.state_selected}, {}}, new int[] {focused, selected, rest});
    }

    /** the focus: a short scale over the neighbours, couch-tv's curve */
    static void focus(View v, boolean has, float scale) {
        v.setTranslationZ(has ? 8f : 0f);
        v.animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f).setDuration(Motion.lite(v.getContext()) ? 0 : MS).setInterpolator(ease()).start();
    }

    /** a toggle's ring (green with a tick when on), a choice's tick, a link's chevron: drawn in its row's colours, mirrored for Arabic */
    static final class Mark extends View {
        static final int RING_ON = 1, RING_OFF = 2, TICK = 3, CHEVRON = 4;
        final int kind; final boolean rtl;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Mark(Context c, int kind, boolean rtl) { super(c); this.kind = kind; this.rtl = rtl; setDuplicateParentStateEnabled(true); }
        @Override protected void drawableStateChanged() { super.drawableStateChanged(); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h);
            boolean focused = isFocused() || (getParent() instanceof View && ((View) getParent()).isFocused());
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            if (kind == RING_ON || kind == RING_OFF) {
                if (kind == RING_ON) { p.setStyle(Paint.Style.FILL); p.setColor(GREEN); c.drawCircle(w / 2, h / 2, s / 2, p); }
                else { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(s / 12f); p.setColor(focused ? RING_FOCUS : RING); c.drawCircle(w / 2, h / 2, s / 2 - s / 24f, p); }
                if (kind == RING_ON) tick(c, w / 2, h / 2, s * 0.62f, WHITE, s * 0.125f);
                return;
            }
            int color = focused ? INK : TEXT;
            if (kind == TICK) { tick(c, w / 2, h / 2, s, color, s * 0.075f); return; }
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(s * 0.075f); p.setColor(color); p.setAlpha(Math.round(255 * 0.45f));
            Path a = new Path(); float d = s * 0.25f, x = w / 2, y = h / 2;
            if (rtl) { a.moveTo(x + d / 2, y - d); a.lineTo(x - d / 2, y); a.lineTo(x + d / 2, y + d); }   // toward the page: left in Arabic
            else { a.moveTo(x - d / 2, y - d); a.lineTo(x + d / 2, y); a.lineTo(x - d / 2, y + d); }
            c.drawPath(a, p);
        }
        private void tick(Canvas c, float x, float y, float s, int color, float stroke) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(stroke); p.setColor(color);
            Path t = new Path();
            t.moveTo(x - s * 0.29f, y + s * 0.02f); t.lineTo(x - s * 0.1f, y + s * 0.21f); t.lineTo(x + s * 0.29f, y - s * 0.19f);
            c.drawPath(t, p);
        }
    }
}
