package sa.urtv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.TimeZone;

/**
 * «المهام» with a time, on the home screen: a white card at the top start, the task, when, and two buttons, «تم» and «ذكّرني بعد ١٠ دقائق»
 * (notes/proposals/reminder.png). It sits over the home in its own window (a view of the home's own root, so it needs no overlay permission) and
 * takes the keys while it is up (ReminderKeys: the first press goes to the card): OK presses the button that has the ring, ◀ ▶ move it, any other
 * key puts the card away until the appointment changes. Silent, but for the soft chime when the reminder asks for it.
 */
final class ReminderCard {
    static final int SNOOZE_MIN = 10;

    private final Context c;
    private final FrameLayout card;
    private final TextView title, sub, ok, later;
    private final ReminderKeys keys = new ReminderKeys();
    private ReminderPlan.Row row;
    private int focus;
    /** cards put away (a key) or answered: not shown again for the same time */
    private final java.util.Set<String> shut = new java.util.HashSet<>(), chimed = new java.util.HashSet<>();

    ReminderCard(Context c, FrameLayout root) {
        this.c = c;
        final boolean rtl = Dir.rtl();
        card = new FrameLayout(c);
        card.setVisibility(View.GONE);
        card.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(fill(0xF7FFFDFA, 14, 0, 0));
        card.addView(box, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView bell = new TextView(c);
        bell.setText("🔔");
        bell.setGravity(Gravity.CENTER);
        bell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        bell.setBackground(fill(0xFFE6F3EC, 8, 0, 0));
        head.addView(bell, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout words = new LinearLayout(c);
        words.setOrientation(LinearLayout.VERTICAL);
        title = text(20, "Medium", 0xFF1C2420);
        title.setMaxLines(2);
        sub = text(14, "Regular", 0xFF5A645F);
        words.addView(title, new LinearLayout.LayoutParams(-1, -2));
        words.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1f);
        wp.setMarginStart(dp(16));
        head.addView(words, wp);
        box.addView(head, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(c);
        ok = button(S.t("تم"), true);
        later = button("", false);
        buttons.addView(ok, new LinearLayout.LayoutParams(0, dp(40), 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.setMarginStart(dp(8));
        buttons.addView(later, lp);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(16);
        box.addView(buttons, bp);
        ok.setOnClickListener(v -> press(0));
        later.setOnClickListener(v -> press(1));

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(380), -2, Dir.start() | Gravity.TOP);
        p.setMarginStart(dp(60));
        p.topMargin = dp(60);
        root.addView(card, p);
    }

    boolean showing() { return card.getVisibility() == View.VISIBLE; }

    /** the card for what is due now, if there is one (called when the alarm says so and when the home comes back to the front) */
    void refresh() {
        long now = System.currentTimeMillis();
        List<ReminderPlan.Row> due = ReminderPlan.showing(ReminderStore.read(c), now);
        ReminderPlan.Row next = null;
        for (ReminderPlan.Row r : due) if (!shut.contains(ReminderPlan.key(r))) { next = r; break; }
        if (next == null) { hide(); return; }
        if (row == null || row.id != next.id || row.fireAt() != next.fireAt()) {
            row = next;
            focus = 0;
            Digits.init(c);
            title.setText(row.text);
            later.setText(ReminderText.snooze(Digits.latin()));
            paint();
            card.setVisibility(View.VISIBLE);
            if (row.sound && chimed.add(ReminderPlan.key(row))) chime();   // once for a card, not again when the home comes back
        }
        sub.setText(ReminderText.when(row, now, zone(), Digits.clock24(), Digits.latin()));
    }

    void hide() {
        row = null;
        card.setVisibility(View.GONE);
    }

    /** the keys while the card is up: true when it took the key */
    boolean key(KeyEvent e) {
        int d = keys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), showing());
        if (d == ReminderKeys.PASS) return false;
        if (d == ReminderKeys.OK) press(focus);
        else if (d == ReminderKeys.MOVE) { focus = 1 - focus; paint(); }
        else if (d == ReminderKeys.AWAY) away();
        return true;
    }

    private void press(int which) {
        if (row == null) return;
        ReminderPlan.Row r = row;
        shut.add(ReminderPlan.key(r));
        if (which == 0) ReminderStore.done(c, r.id); else ReminderStore.snooze(c, r.id, SNOOZE_MIN);
        hide();
        ReminderScheduler.schedule(c);
        refresh();   // the next one, if two are due
    }

    private void away() {
        if (row != null) shut.add(ReminderPlan.key(row));
        hide();
        refresh();
    }

    private TimeZone zone() {
        try {
            String[] z = ConfigSnapshot.read(c).cityZone();
            return z == null ? ZoneFix.tvZone() : ZoneFix.zone(ClockZone.valid(z[1]));
        } catch (RuntimeException e) {
            return ZoneFix.tvZone();
        }
    }

    private void chime() {
        try {
            final android.media.ToneGenerator g = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 35);
            g.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 400);
            card.postDelayed(g::release, 900);
        } catch (RuntimeException ignored) { }
    }

    /** the ring on the button that OK presses; «تم» is the solid green one */
    private void paint() {
        ok.setBackground(fill(0xFF23996B, 8, focus == 0 ? 0xFF1F8A5B : 0xFF23996B, focus == 0 ? 3 : 2));
        later.setBackground(fill(0xFFFFFFFF, 8, focus == 1 ? 0xFF1F8A5B : 0xFFD8DDD9, focus == 1 ? 3 : 2));
    }

    private TextView button(String label, boolean primary) {
        TextView t = text(16, "Medium", primary ? Color.WHITE : 0xFF1C2420);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setClickable(true);
        return t;
    }

    private TextView text(float sp, String weight, int color) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        t.setTextDirection(Dir.text());
        try { t.setTypeface(Typeface.createFromAsset(c.getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private GradientDrawable fill(int color, float radiusDp, int line, int lineDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (lineDp > 0) g.setStroke(dp(lineDp), line);
        return g;
    }

    private int dp(float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    /** the one line of the notice over another app */
    static String line(Context c, ReminderPlan.Row r, long now) {
        TimeZone tz;
        try {
            String[] z = ConfigSnapshot.read(c).cityZone();
            tz = z == null ? ZoneFix.tvZone() : ZoneFix.zone(ClockZone.valid(z[1]));
        } catch (RuntimeException e) {
            tz = ZoneFix.tvZone();
        }
        return ReminderText.line(r, now, tz, Digits.clock24(), Digits.latin());
    }
}
