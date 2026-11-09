package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The app's options (docs/UX-REVIEW.md, the owner's v5): a small box right above the app, exactly its width, with no title. Three rows
 * one under another, «انقل», «أخفِ», «اقفل», and under them two equal icon buttons: ★ (filled: a favourite, OK takes it out; an outline:
 * OK adds it) and ⓘ (the app's details), the ★ at the start. One grid: padding 8, gap 4, every button 36 tall, radius 6 (the box 14).
 * Focus is the same on a row and an icon button: the look's focus fill, with its line round it when the look has one (GlassStyle.ring).
 * Opened by «⋮ خيارات» at the end of the favourites and by a long OK on the app (a shortcut to the same box); the colours are the look's.
 */
final class AppOptions {
    private AppOptions() {}

    /** dp: the grid (the owner's v5 at 1080p, halved) */
    static final int PAD = 8, GAP = 4, BUTTON_H = 36, RADIUS = 6, BOX_RADIUS = 14, ICON = 18, ABOVE = 12;

    /** the words: the rows, and the two icon buttons' names for a screen reader */
    static final String MOVE = S.n("انقل"), HIDE = S.n("أخفِ"), LOCK = S.n("اقفل"), UNLOCK = S.n("افتح القفل"),
            ADD_FAV = S.n("أضف للمفضلة"), REMOVE_FAV = S.n("أزل من المفضلة"), INFO = S.n("تفاصيل التطبيق"),
            UNINSTALL = S.n("إلغاء التثبيت"), STORE = S.n("صفحته في المتجر");

    /** What the box offers for this app, and what each does (null: the row is not there; kids mode moves and hides nothing). */
    static final class Spec {
        Runnable move, hide, lock, favourite, info;
        Runnable uninstall;   // null: not offered (a system app, or ours)
        Runnable store;       // null: not offered (no Play Store on this TV)
        boolean locked, favouriteNow;
    }

    /** the height of the box, dp, for this many rows (the icon row always) */
    static int heightDp(int rows) { return PAD * 2 + (rows + 1) * BUTTON_H + rows * GAP; }

    /**
     * Shows the box over {@code card} (its width as it shows while focused: {@code scale}), above it, or under it when there is no
     * room above. Returns the window (the caller closes it when the home goes).
     */
    static PopupWindow show(Activity a, View card, float scale, GlassStyle.Look l, Typeface medium, Spec s) {
        float d = a.getResources().getDisplayMetrics().density;
        int dialog = GlassStyle.over(l.dialog, l.base);   // opaque: the box sits over the photo and the apps
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        int pad = px(PAD, d);
        box.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(BOX_RADIUS, d));
        bg.setColor(dialog);
        bg.setStroke(Math.max(1, px(0.5f, d)), l.line);
        box.setBackground(bg);

        final PopupWindow[] pw = new PopupWindow[1];
        List<View> rows = new ArrayList<>();
        if (s.move != null) rows.add(row(a, d, l, dialog, medium, R.drawable.ic_move, MOVE, s.move, pw));
        if (s.hide != null) rows.add(row(a, d, l, dialog, medium, R.drawable.ic_hide, HIDE, s.hide, pw));
        if (s.lock != null) rows.add(row(a, d, l, dialog, medium, R.drawable.ic_lock, s.locked ? UNLOCK : LOCK, s.lock, pw));
        if (s.store != null) rows.add(row(a, d, l, dialog, medium, R.drawable.ic_store, STORE, s.store, pw));
        if (s.uninstall != null) rows.add(row(a, d, l, dialog, medium, R.drawable.ic_delete, UNINSTALL, s.uninstall, pw));
        for (int i = 0; i < rows.size(); i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(BUTTON_H, d));
            if (i > 0) lp.topMargin = px(GAP, d);
            box.addView(rows.get(i), lp);
        }
        LinearLayout icons = new LinearLayout(a);
        icons.setOrientation(LinearLayout.HORIZONTAL);
        icons.setLayoutDirection(Dir.layout());
        View star = icon(a, d, l, dialog, s.favouriteNow ? R.drawable.ic_star_filled : R.drawable.ic_star, s.favouriteNow ? REMOVE_FAV : ADD_FAV,
                s.favouriteNow, s.favourite, pw);
        View info = icon(a, d, l, dialog, R.drawable.ic_info, INFO, false, s.info, pw);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, px(BUTTON_H, d), 1f);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, px(BUTTON_H, d), 1f);
        ip.setMarginStart(px(GAP, d));
        icons.addView(star, sp);
        icons.addView(info, ip);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, px(BUTTON_H, d));
        if (!rows.isEmpty()) rp.topMargin = px(GAP, d);
        box.addView(icons, rp);

        // the walk: up and down through the rows to the icon row; ◀ ▶ only between the two icons (the right one first in Arabic)
        List<View> column = new ArrayList<>(rows);
        column.add(star);
        for (int i = 0; i < column.size(); i++) {
            View v = column.get(i);
            v.setNextFocusUpId(column.get(Math.max(0, i - 1)).getId());
            v.setNextFocusDownId(column.get(Math.min(column.size() - 1, i + 1)).getId());
            if (v != star) { v.setNextFocusLeftId(v.getId()); v.setNextFocusRightId(v.getId()); }
        }
        info.setNextFocusUpId(rows.isEmpty() ? info.getId() : rows.get(rows.size() - 1).getId());
        info.setNextFocusDownId(info.getId());
        star.setNextFocusLeftId(Dir.rtl() ? info.getId() : star.getId());
        star.setNextFocusRightId(Dir.rtl() ? star.getId() : info.getId());
        info.setNextFocusRightId(Dir.rtl() ? star.getId() : info.getId());
        info.setNextFocusLeftId(Dir.rtl() ? info.getId() : star.getId());

        int w = Math.round(card.getWidth() * scale), h = px(heightDp(rows.size()), d);
        PopupWindow p = new PopupWindow(box, w, h, true);
        pw[0] = p;
        p.setOutsideTouchable(true);
        p.setElevation(px(12, d));
        int[] at = new int[2];
        ((View) card.getParent()).getLocationInWindow(at);
        int cx = at[0] + card.getLeft() + card.getWidth() / 2, cy = at[1] + card.getTop() + card.getHeight() / 2;
        int top = cy - Math.round(card.getHeight() * scale / 2f), bottom = cy + Math.round(card.getHeight() * scale / 2f);
        int gap = px(ABOVE, d);
        int y = top - gap - h >= px(8, d) ? top - gap - h : bottom + gap;   // above the app; under it when the app is at the top
        p.showAtLocation(card, Gravity.NO_GRAVITY, cx - w / 2, y);   // x and y from the window's top left corner
        (column.isEmpty() ? star : column.get(0)).requestFocus();
        return p;
    }

    private static View row(Activity a, float d, GlassStyle.Look l, int dialog, Typeface medium, int icon, String label, Runnable action, PopupWindow[] pw) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(Dir.layout());
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(px(12, d), 0, px(12, d), 0);
        ImageView g = new ImageView(a);
        Drawable dr = a.getDrawable(icon).mutate();
        g.setImageDrawable(dr);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(px(ICON, d), px(ICON, d));
        gp.setMarginEnd(px(8, d));
        r.addView(g, gp);
        TextView t = new TextView(a);
        t.setText(S.tv(label));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTypeface(medium);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        r.addView(t, new LinearLayout.LayoutParams(0, -1, 1f));
        dress(a, d, l, dialog, r, false, () -> { t.setTextColor(r.hasFocus() || r.isFocused() ? l.onFocus : l.text); dr.setTint(tint(l, r.isFocused(), false)); });
        click(r, label, action, pw);
        return r;
    }

    private static View icon(Activity a, float d, GlassStyle.Look l, int dialog, int icon, String label, boolean on, Runnable action, PopupWindow[] pw) {
        LinearLayout b = new LinearLayout(a);
        b.setGravity(Gravity.CENTER);
        ImageView g = new ImageView(a);
        Drawable dr = a.getDrawable(icon).mutate();
        g.setImageDrawable(dr);
        b.addView(g, new LinearLayout.LayoutParams(px(ICON + 2, d), px(ICON + 2, d)));
        dress(a, d, l, dialog, b, true, () -> dr.setTint(tint(l, b.isFocused(), on)));
        click(b, label, action, pw);
        return b;
    }

    /** an icon's colour: the focus ink while focused (the ring's green when the look has one), a filled ★ the accent, else the second ink */
    static int tint(GlassStyle.Look l, boolean focused, boolean on) {
        if (focused) return l.ring != 0 ? l.ring : l.onFocus;
        return on ? (l.ring != 0 ? l.ring : l.accent) : l.sub;
    }

    /** quiet at rest (a row: nothing; an icon button: a faint fill so the pair reads as buttons), the look's focus while focused */
    private static void dress(Activity a, float d, GlassStyle.Look l, int dialog, LinearLayout v, boolean faint, Runnable paint) {
        v.setId(View.generateViewId());
        v.setFocusable(true);
        v.setClickable(true);
        GradientDrawable rest = new GradientDrawable();
        rest.setCornerRadius(px(RADIUS, d));
        rest.setColor(faint ? GlassStyle.over(l.line, dialog) : 0);
        GradientDrawable focus = GlassPaint.focused(a, l, GlassStyle.over(l.focus, dialog), RADIUS);
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, focus);
        s.addState(new int[] {}, rest);
        v.setBackground(s);
        v.setOnFocusChangeListener((x, has) -> paint.run());
        paint.run();
    }

    private static void click(View v, String label, Runnable action, PopupWindow[] pw) {
        v.setContentDescription(S.tv(label));
        v.setOnClickListener(x -> { if (pw[0] != null) pw[0].dismiss(); if (action != null) action.run(); });
        v.setOnKeyListener((x, code, e) -> code == KeyEvent.KEYCODE_DPAD_CENTER && e.getRepeatCount() > 0);   // a held OK acts once
    }

    private static int px(float dp, float density) { return Math.round(dp * density); }
}
