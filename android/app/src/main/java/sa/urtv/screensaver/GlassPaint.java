package sa.urtv.screensaver;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The glass look drawn (GlassStyle's colours): the page, panels, tiles, dialogs. Flat fills only: the page's photo is the one small
 * backdrop GlassSource keeps, drawn scaled, never blurred here (no live blur: tests/glass_test.py looks for one).
 *
 * <p>This file is the SAME in both apps (only the package line differs): tests/glass_test.py fails when the two copies differ.
 */
public final class GlassPaint {
    private GlassPaint() { }

    /** The page: the backdrop under the veil it needs, or the look's solid colour while there is none. */
    public static Drawable page(Context c, Bitmap backdrop, GlassStyle.Look l, float scrim) {
        if (backdrop == null) return new ColorDrawable(l.base);
        BitmapDrawable photo = new BitmapDrawable(c.getResources(), backdrop);   // the same bitmap, not a copy
        photo.setFilterBitmap(true);
        photo.setGravity(Gravity.FILL);
        return new LayerDrawable(new Drawable[] {photo, new ColorDrawable(GlassStyle.scrimColor(l, scrim))});
    }

    /** A rounded fill, with a thin line when {@code line} is not 0. */
    public static GradientDrawable fill(Context c, int color, float radiusDp, int line) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, radiusDp));
        d.setColor(color);
        if (line != 0) d.setStroke(Math.max(1, dp(c, 1)), line);
        return d;
    }

    /** A panel: a list of rows, a tile at rest. */
    public static GradientDrawable panel(Context c, GlassStyle.Look l, float radiusDp) {
        return fill(c, l.panel, radiusDp, l.line);
    }

    /** A tile or a row: the panel at rest, the solid focus fill while it has the focus. */
    public static Drawable tile(Context c, GlassStyle.Look l, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, fill(c, l.focus, radiusDp, 0));
        s.addState(new int[] {}, panel(c, l, radiusDp));
        return s;
    }

    /** A row inside a panel: nothing at rest (the panel is under it), the focus fill while focused. */
    public static Drawable row(Context c, GlassStyle.Look l, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, fill(c, l.focus, radiusDp, 0));
        s.addState(new int[] {}, new ColorDrawable(0));
        return s;
    }

    /** A dialog's own panel. */
    public static GradientDrawable dialog(Context c, GlassStyle.Look l) {
        return fill(c, l.dialog, 22, l.line);
    }

    /** The hint line's pill at the top of a screen. */
    public static GradientDrawable hint(Context c, GlassStyle.Look l) {
        return fill(c, l.hint, 18, l.line);
    }

    /** the tag of an ImageView that is one of our line icons: it takes the card's focus colours (an app's icon keeps its own) */
    public static final String ICON = "glass:icon";

    /** a colour at rest, another while focused or selected (a list's row) */
    public static ColorStateList focusColors(int rest, int focused) {
        return new ColorStateList(new int[][] {{android.R.attr.state_focused}, {android.R.attr.state_selected}, {}}, new int[] {focused, focused, rest});
    }

    /**
     * A focusable card in the glass look: the panel at rest, the solid focus fill while focused ({@link #tile}), and every word and icon
     * in it in the focus ink then ({@link #follow}). A word in the sub colour becomes onFocusSub; an icon tagged {@link #ICON} the accent,
     * then the focus ink.
     */
    public static void card(View card, GlassStyle.Look l, float radiusDp) {
        card.setBackground(tile(card.getContext(), l, radiusDp));
        follow(card, l);
    }

    /**
     * The words and icons of {@code card} (and the card itself, when it is a word) in the focus ink while it has the focus, their own
     * colours otherwise. Set from the card's focus listener (it runs first, then the one the screen had), never through
     * duplicateParentState: on the TV test of 3.147 the rows inside the settings kept their white words on the white fill that way.
     */
    public static void follow(View card, GlassStyle.Look l) {
        final java.util.List<TextView> words = new java.util.ArrayList<>();
        final java.util.List<int[]> colours = new java.util.ArrayList<>();   // {rest, focused}
        final java.util.List<ImageView> icons = new java.util.ArrayList<>();
        collect(card, l, words, colours, icons);
        View.OnFocusChangeListener had = card.getOnFocusChangeListener();
        final View.OnFocusChangeListener was = had instanceof Follow ? ((Follow) had).was : had;   // dressed again: the screen's own, once
        card.setOnFocusChangeListener(new Follow(was) {
            @Override public void onFocusChange(View v, boolean has) {
                for (int i = 0; i < words.size(); i++) words.get(i).setTextColor(colours.get(i)[has ? 1 : 0]);
                for (ImageView iv : icons) if (iv.getDrawable() != null) iv.getDrawable().mutate().setTint(has ? l.onFocus : l.accent);
                if (was != null) was.onFocusChange(v, has);
            }
        });
        for (int i = 0; i < words.size(); i++) words.get(i).setTextColor(colours.get(i)[card.hasFocus() ? 1 : 0]);
        for (ImageView iv : icons) if (iv.getDrawable() != null) iv.getDrawable().mutate().setTint(card.hasFocus() ? l.onFocus : l.accent);
    }

    /** our focus listener, keeping the one the screen had */
    private abstract static class Follow implements View.OnFocusChangeListener {
        final View.OnFocusChangeListener was;
        Follow(View.OnFocusChangeListener was) { this.was = was; }
    }

    private static void collect(View v, GlassStyle.Look l, java.util.List<TextView> words, java.util.List<int[]> colours, java.util.List<ImageView> icons) {
        if (v instanceof TextView) {
            int rest = ((TextView) v).getCurrentTextColor();
            words.add((TextView) v);
            colours.add(new int[] {rest, rest == l.sub ? l.onFocusSub : l.onFocus});
        } else if (v instanceof ImageView && ICON.equals(v.getTag())) {   // our own line icons only, never an app's icon
            icons.add((ImageView) v);
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), l, words, colours, icons);
        }
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
