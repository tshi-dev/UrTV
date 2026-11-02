package sa.urtv.home;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * One task's page, opened from «ذكّرني» beside it in «المهام» (notes/proposals/reminder-set.png): the task, then the rows «ذكّرني» (on or off), اليوم, الساعة,
 * «قبلها بـ», «يتكرر», «صوت خفيف» and «أين يظهر». ◀ ▶ step a row's value and OK steps it on (a day later, the next choice); every change is saved at once
 * to the screensaver app (its ConfigProvider, same signing key), so there is nothing to confirm and Back just leaves. No dialogs, like «المهام».
 * `am start -n sa.urtv.home/.TaskReminderActivity --el id N` (the task's id from the screensaver's list).
 */
public class TaskReminderActivity extends Activity {
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    static final String EXTRA_ID = "id";
    private static final Uri TASKS = Uri.parse("content://sa.urtv.screensaver.config/tasks");

    private GlassStyle.Look look;
    private int TEXT, DIM;
    private long id;
    private ReminderEdit edit;
    private TimeZone zone;
    private TextView[] values;
    private TextView[] labels;
    private LinearLayout[] rows;
    private String taskText = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        id = getIntent().getLongExtra(EXTRA_ID, 0);
        load();
        if (edit == null) { finish(); return; }
        try {
            String[] z = ConfigSnapshot.read(this).cityZone();
            zone = z == null ? TimeZone.getDefault() : TimeZone.getTimeZone(ClockZone.valid(z[1]));
        } catch (RuntimeException e) {
            zone = TimeZone.getDefault();
        }
        ScrollView scroll = new ScrollView(this);
        look = GlassSource.page(this, scroll);
        TEXT = look.text; DIM = look.sub;
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(40), 0, dp(48));
        frame.addView(col, new FrameLayout.LayoutParams(dp(600), -2, Gravity.CENTER_HORIZONTAL));
        col.addView(text(S.t("مهمة"), 26, "Medium", TEXT), gap(dp(16)));
        TextView name = text(taskText, 20, "Medium", TEXT);
        name.setPadding(dp(16), dp(16), dp(16), dp(16));
        GlassPaint.card(name, look, 14);
        col.addView(name, gap(dp(8)));

        String[] names = {S.t("ذكّرني"), S.t("التاريخ"), S.t("الساعة"), S.t("قبلها بـ"), S.t("يتكرر"), S.t("صوت خفيف"), S.t("أين يظهر")};
        rows = new LinearLayout[names.length];
        labels = new TextView[names.length];
        values = new TextView[names.length];
        for (int k = 0; k < names.length; k++) {
            final int row = k;
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(dp(16), dp(16), dp(16), dp(16));
            r.setFocusable(true);
            r.setClickable(true);
            r.setId(View.generateViewId());
            labels[k] = text(names[k], 17, "Medium", TEXT);
            values[k] = text("", 17, "Regular", DIM);
            values[k].setGravity(Dir.end());
            r.addView(labels[k], new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(values[k], new LinearLayout.LayoutParams(-2, -2));
            GlassPaint.card(r, look, 14);
            r.setOnClickListener(v -> step(row, 1));
            r.setOnKeyListener((v, code, e) -> {
                if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    step(row, code == Dir.forwardKey() ? 1 : -1);
                    return true;
                }
                return false;
            });
            rows[k] = r;
            col.addView(r, gap(dp(8)));
        }
        TextView hint = text(S.t("◀ ▶ تغيّر القيمة، واختيار يتقدم بها. التغييرات تُحفظ فوراً."), 14, "Regular", DIM);
        col.addView(hint, gap(0));
        scroll.setLayoutDirection(Dir.layout(Dir.rtl()));
        setContentView(scroll);
        draw();
        rows[0].post(rows[0]::requestFocus);
    }

    /** the task's text and reminder from the screensaver app's list; edit stays null when it is gone or the app is too old for reminders */
    private void load() {
        try (android.database.Cursor c = getContentResolver().query(TASKS, null, null, null, null)) {
            if (c == null || c.getColumnIndex("due") < 0) return;
            while (c.moveToNext()) {
                if (c.getLong(c.getColumnIndexOrThrow("id")) != id) continue;
                taskText = c.getString(c.getColumnIndexOrThrow("t"));
                edit = new ReminderEdit(c.getLong(c.getColumnIndexOrThrow("due")), c.getInt(c.getColumnIndexOrThrow("bf")), c.getInt(c.getColumnIndexOrThrow("rp")),
                        c.getInt(c.getColumnIndexOrThrow("snd")) == 1, c.getInt(c.getColumnIndexOrThrow("wh")));
                return;
            }
        } catch (RuntimeException ignored) { }
    }

    private void step(int row, int dir) {
        long now = System.currentTimeMillis();
        switch (row) {
            case 0: edit.toggle(now); break;
            case 1: if (edit.on) edit.day(dir, now); break;
            case 2: if (edit.on) edit.time(dir, now); break;
            case 3: edit.cycleBefore(dir); break;
            case 4: edit.cycleRepeat(dir); break;
            case 5: edit.toggleSound(); break;
            default: edit.cycleWhere(dir);
        }
        save();
        draw();
    }

    private void save() {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("due", edit.savedDue());
        v.put("bf", edit.before);
        v.put("rp", edit.repeat);
        v.put("snd", edit.sound ? 1 : 0);
        v.put("wh", edit.where);
        try { getContentResolver().update(TASKS, v, null, null); } catch (RuntimeException ignored) { }
        ReminderScheduler.schedule(this);   // the alarm follows
    }

    private void draw() {
        boolean latin = Digits.latin();
        values[0].setText(edit.on ? S.t("مفعّل") : S.t("متوقف"));
        values[0].setTextColor(edit.on ? OnOff.green(look.light ? 0xFFFFFFFF : 0xFF000000) : DIM);
        values[1].setText(day(latin));
        values[2].setText(ClockText.time(field(java.util.Calendar.HOUR_OF_DAY), field(java.util.Calendar.MINUTE), Digits.clock24(), latin));
        values[3].setText(edit.before == 0 ? S.t("في وقت الموعد") : AdhanBarText.minutes(edit.before, latin));
        values[4].setText(edit.repeat == ReminderEdit.DAILY ? S.t("يومياً") : edit.repeat == ReminderEdit.WEEKLY ? S.t("أسبوعياً") : S.t("لا"));
        values[5].setText(edit.sound ? S.t("مفعّل") : S.t("متوقف"));
        values[6].setText(edit.where == 1 ? S.t("شاشة التوقف فقط") : edit.where == 2 ? S.t("الرئيسية فقط") : S.t("شاشة التوقف والرئيسية"));
        for (int k = 1; k < 5; k++) rows[k].setAlpha(edit.on ? 1f : .5f);   // the rest wait for «ذكّرني»
        rows[5].setAlpha(edit.on ? 1f : .5f);
        rows[6].setAlpha(edit.on ? 1f : .5f);
    }

    private int field(int f) {
        java.util.Calendar c = java.util.Calendar.getInstance(zone);
        c.setTimeInMillis(edit.due);
        return c.get(f);
    }

    private String day(boolean latin) {
        SimpleDateFormat f = new SimpleDateFormat("EEEE d MMMM", S.lang().equals(Tr.EN) ? Locale.ENGLISH : new Locale("ar"));
        f.setTimeZone(zone);
        return DigitsText.convert(f.format(new Date(edit.due)), latin);
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(S.tv(s));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        t.setTextDirection(Dir.text());
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams gap(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
