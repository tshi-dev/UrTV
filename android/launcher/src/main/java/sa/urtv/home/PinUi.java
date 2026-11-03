package sa.urtv.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** The PIN keypad's TV colours, dp sizing and a visible remote-focus state (from AbdullahCg's PR #40). */
final class PinUi {
    // the glass look of now (look(), before the keypad is built): the panel opaque (the dialog's glass over the look's solid colour), so
    // it reads the same over any screen it comes up on; the keys glass tiles, the focused one the solid focus fill
    static int BG, LINE, SURFACE, GOLD, TEXT, MUTED, FOCUS, INK, RING;
    static { look(GlassStyle.DARK); }

    static void look(Context c) { look(GlassSource.look(c)); }

    private static void look(GlassStyle.Look l) {
        BG = GlassStyle.over(l.dialog, l.base); LINE = l.line; SURFACE = l.panel; GOLD = l.accent; TEXT = l.text; MUTED = l.sub;
        FOCUS = l.focus; INK = l.onFocus; RING = l.ring != 0 ? l.ring : l.focus;   // «رملي»: the green line round the soft focus fill
    }
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable panel(Context c,int color,int border){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,14));g.setStroke(dp(c,1),border);return g;
    }
    static TextView text(Context c,String value,int size,int color){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Dir.start());t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        try{t.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/"+(size>=25?"ThmanyahDisplay-Bold":"Thmanyah-Regular")+".otf"));}
        catch(RuntimeException ignored){t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));}return t;
    }
    static Button button(Context c,String label){
        Button b=new Button(c);b.setId(View.generateViewId());b.setText(label);b.setAllCaps(false);
        b.setTextSize(21);try{b.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/Thmanyah-Medium.otf"));}catch(RuntimeException ignored){}b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setBackgroundTintList(null);b.setStateListAnimator(null);
        int[][] states={{android.R.attr.state_focused},{android.R.attr.state_pressed},{}};
        b.setTextColor(new ColorStateList(states,new int[]{INK,INK,TEXT}));
        StateListDrawable bg=new StateListDrawable();
        bg.addState(states[0],panel(c,FOCUS,RING));bg.addState(states[1],panel(c,FOCUS,RING));
        bg.addState(states[2],panel(c,SURFACE,LINE));b.setBackground(bg);
        return b;   // no focus scale: a grown key ate 4 px of its neighbour's gap (the tester measured 20 and 24 in one row); the fill and the ring show the focus
    }
}
