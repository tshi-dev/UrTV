package sa.urtv.home.tilawah;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The project's type and small formatting helpers. */
final class Ui {
    private static final Map<String, Typeface> FONTS = new HashMap<>();

    private Ui() {}

    static Typeface font(Context c, String file) {
        Typeface cached = FONTS.get(file);
        if (cached != null) return cached;
        Typeface made = Typeface.createFromAsset(c.getAssets(), "fonts/" + file);
        FONTS.put(file, made);
        return made;
    }

    static void text(TextView view, String file, int sizeSp, int color) {
        view.setTypeface(font(view.getContext(), file));
        view.setText(digits(view.getText().toString()));   // a text written in the layout or the strings has the setting's digits too
        view.setTextSize(sizeSp);
        view.setTextColor(view.getContext().getColorStateList(color));   // res/color, in the screen's theme: the focus ink on a focused row
    }

    /**
     * The theme of «المظهر» for these screens (styles.xml: one per look and light, made from GlassStyle), from «الشكل» and «الإضاءة»
     * (GlassSource: automatic follows the home). For onCreate, before the screen is drawn.
     */
    static int theme(Context c) {
        String look;
        boolean light;
        try { look = sa.urtv.home.GlassSource.lookName(c); light = sa.urtv.home.GlassSource.light(c); }
        catch (RuntimeException e) { look = "glass"; light = false; }
        switch (look) {
            case "graphite": return light ? sa.urtv.home.R.style.Tilawah_Graphite_Light : sa.urtv.home.R.style.Tilawah_Graphite_Dark;
            case "navy": return light ? sa.urtv.home.R.style.Tilawah_Navy_Light : sa.urtv.home.R.style.Tilawah_Navy_Dark;
            case "clean": return light ? sa.urtv.home.R.style.Tilawah_Clean_Light : sa.urtv.home.R.style.Tilawah_Clean_Dark;
            default: return light ? sa.urtv.home.R.style.Tilawah_Glass_Light : sa.urtv.home.R.style.Tilawah_Glass_Dark;
        }
    }

    /** After setContentView: the page is the home's photo blurred once, under its veil (GlassSource.page); the old photo layer goes. */
    static void glassPage(Activity a, int photoId) {
        View photo = a.findViewById(photoId);
        if (photo == null) return;
        photo.setVisibility(View.GONE);
        if (photo instanceof android.widget.ImageView) ((android.widget.ImageView) photo).setImageDrawable(null);   // not kept in memory
        sa.urtv.home.GlassSource.page(a, (View) photo.getParent());
    }

    /**
     * «اللغة» on a screen built from a layout (docs/ENGLISH.md), right after setContentView: with English the layout's right-to-left root
     * reads left to right, and every text and spoken name the layout wrote (its @string words) is said in English. Arabic: nothing at all.
     */
    static void localize(Activity a) {
        if (!sa.urtv.home.S.en()) return;
        android.view.ViewGroup content = a.findViewById(android.R.id.content);
        View root = content == null || content.getChildCount() == 0 ? null : content.getChildAt(0);
        if (root == null) return;
        root.setLayoutDirection(sa.urtv.home.Dir.layout());
        say(root);
    }

    /** every text and spoken name under v said in «اللغة» (a dialog's layout too); with Arabic nothing changes */
    static void say(View v) {
        if (!sa.urtv.home.S.en() || v == null) return;
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getText() != null) t.setText(sa.urtv.home.S.tv(t.getText().toString()));
        }
        if (v.getContentDescription() != null) v.setContentDescription(sa.urtv.home.S.tv(v.getContentDescription().toString()));
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) say(g.getChildAt(i));
        }
    }

    /** An XML string in «اللغة» (rule 5: no values-en; the table has the XML words): with Arabic exactly getString, arguments and all. */
    static String str(Context c, int id, Object... args) {
        if (!sa.urtv.home.S.en()) return args.length == 0 ? c.getString(id) : c.getString(id, args);
        return sa.urtv.home.S.tv(c.getString(id), args);
    }

    /**
     * A row's line in «اللغة»: the whole line when the table has it («مفعّل  •  يتوقف بعد دقيقة»), else each part between «  •  » on its own
     * (a part is our Arabic word or one already said). The rows keep their Arabic lines, so OnOff still reads their state. Arabic: s itself.
     */
    static String line(String s) {
        if (s == null || !sa.urtv.home.S.en()) return s;
        String whole = sa.urtv.home.S.tv(s);
        if (!whole.equals(s)) return whole;
        String[] parts = s.split("  •  ", -1);
        for (int i = 0; i < parts.length; i++) parts[i] = sa.urtv.home.S.tv(parts[i]);
        return String.join("  •  ", parts);
    }

    /** 1:05, 12:40, 1:02:03 */
    static String time(int ms) {
        if (ms < 0) ms = 0;
        int total = ms / 1000;
        int h = total / 3600, m = (total % 3600) / 60, s = total % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s)
                     : String.format(Locale.US, "%d:%02d", m, s);
    }

    /** 114 -> ١١٤ (or 114 when the screensaver's digits are latin) */
    static String arabic(int n) { return digits(String.valueOf(n)); }

    /** "05:12" -> "٠٥:١٢" (or as it is when the digits are latin) */
    static String digits(String s) {
        return sa.urtv.home.DigitsText.convert(s, sa.urtv.home.Digits.latin());   // the screensaver's «الأرقام» setting
    }
}
