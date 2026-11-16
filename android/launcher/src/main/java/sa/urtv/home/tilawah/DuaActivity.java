package sa.urtv.home.tilawah;

import android.content.Intent;

import java.util.List;

import sa.urtv.home.S;

/**
 * 4.4 «الأدعية والتذكيرات» (docs/mockups/audio-list.png, audio-edit.png, audio-time.png): the list, «إضافة» first; one dua's page with its name,
 * sound, days, time, repeat, volume, «فوق تطبيق آخر» (off by default), on/off, listen now and delete. Every change is kept at once and the alarm
 * set again (DuaScheduler). The time is chosen in three steps (a clock time, or before or after a prayer; which prayer; by how much), the
 * sentence it makes said above the list, with today's moment.
 */
public class DuaActivity extends RowScreen {
    private static final int PICK_SOUND = 11;
    /** the dua whose page is open (a copy; kept with DuaStore.put on every change) */
    private DuaStore.Dua edit;

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("الأدعية والتذكيرات"); }
            @Override public String tagline() { return S.t("تُشغَّل فوق الرئيسية وشاشة التوقف، وتوقف التلاوة ثم تعيدها"); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("إضافة دعاء أو تذكير"), S.t("صوت من «صوتياتي»"), DuaActivity.this::add));
                for (DuaStore.Dua d : DuaStore.list(DuaActivity.this)) {
                    String state = d.on ? S.t("مفعّل") : S.t("متوقف");
                    out.add(Row.of(d.name, state + ": " + DuaWords.line(d.rule), () -> openDua(d.id)).onLong(() -> listen(d)));
                }
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للفتح والتعديل، ومطوّلًا للاستماع الآن"); }

    // ---- a new one: its sound first, then its page

    private void add() {
        if (DuaStore.list(this).size() >= DuaStore.MAX) { toast(S.t("وصلت الحد: %s", Ui.arabic(DuaStore.MAX))); return; }
        DuaStore.Dua d = new DuaStore.Dua();
        d.rule.day = DuaRule.EVERY_DAY;
        d.rule.at = DuaRule.AFTER;
        d.rule.prayer = PrayerTimes.FAJR;
        d.rule.offset = 15;
        edit = d;
        pickSound();
    }

    private void pickSound() {
        startActivityForResult(new Intent(this, AudioActivity.class).putExtra(AudioActivity.EXTRA_PICK, true), PICK_SOUND);
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != PICK_SOUND || edit == null || result != RESULT_OK || data == null) return;
        AudioStore.Item it = AudioStore.get(this, data.getIntExtra("id", -1));
        if (it == null) return;
        boolean fresh = edit.id <= 0;
        edit.sound = "a:" + it.id;
        if (edit.name.isEmpty()) edit.name = it.name;
        keep();
        if (fresh) openDua(edit.id); else refresh();
    }

    private void keep() {
        if (!DuaStore.put(this, edit)) { toast(S.t("وصلت الحد: %s", Ui.arabic(DuaStore.MAX))); return; }
        DuaScheduler.schedule(this);
    }

    private void listen(DuaStore.Dua d) {
        String src = DuaStore.source(this, d);
        if (src == null) { toast(S.t("الملف محذوف: اختر صوتًا آخر")); return; }
        MusicService.dua(this, src, d.name, d.volume);
    }

    // ---- one dua's page

    private void openDua(int id) {
        DuaStore.Dua d = DuaStore.get(this, id);
        if (d == null) return;
        edit = d;
        open(new Page() {
            @Override public String title() { return edit.name; }
            @Override public String tagline() {
                long next = edit.on ? edit.rule.next(System.currentTimeMillis(), new DuaDays(DuaActivity.this)) : 0;
                return next > 0 ? S.t("المرة القادمة: %s", when(next)) : S.t("متوقف");
            }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("الاسم"), edit.name, DuaActivity.this::rename));
                out.add(Row.of(S.t("الصوت"), DuaStore.soundName(DuaActivity.this, edit), DuaActivity.this::pickSound));
                out.add(Row.of(S.t("الأيام"), DuaWords.day(edit.rule), DuaActivity.this::chooseDay));
                out.add(Row.of(S.t("الوقت"), DuaWords.time(edit.rule), DuaActivity.this::chooseTime));
                out.add(Row.of(S.t("يتكرر"), repeatWord(), () -> { edit.rule.repeat = !edit.rule.repeat; fixYear(); keep(); refresh(); }));
                out.add(Row.of(S.t("مستوى الصوت"), Ui.arabic(edit.volume) + "٪", null)
                        .onStep(s -> { edit.volume = Math.max(10, Math.min(100, edit.volume + 10 * s)); keep(); }));
                out.add(Row.of(S.t("فوق تطبيق آخر يشغّل صوتًا"), edit.overOthers ? S.t("نعم: يوقفه ويشتغل") : S.t("لا: إذا كان تطبيق آخر يشغّل صوتًا يُترك"),
                        () -> { edit.overOthers = !edit.overOthers; keep(); refresh(); }));
                out.add(Row.of(S.t("التشغيل"), edit.on ? S.t("مفعّل") : S.t("متوقف"), () -> { edit.on = !edit.on; keep(); refresh(); }));
                out.add(Row.of(S.t("استمع الآن"), "", () -> listen(edit)));
                out.add(Row.red(S.t("حذف"), "", () -> ask(S.t("تحذف «%s»؟", edit.name), S.t("الملف يبقى في «صوتياتي»"), false,
                        new String[]{S.t("حذف"), S.t("إلغاء")},
                        new Runnable[]{() -> { DuaStore.remove(DuaActivity.this, edit.id); DuaScheduler.schedule(DuaActivity.this); back(); }, null})));
            }
        });
    }

    private String repeatWord() {
        if (!edit.rule.repeat) return S.t("مرة واحدة");
        return edit.rule.day == DuaRule.GREGORIAN || edit.rule.day == DuaRule.HIJRI ? S.t("كل سنة")
                : edit.rule.day == DuaRule.WEEKDAYS ? S.t("كل أسبوع") : S.t("كل يوم");
    }

    /** a date that does not repeat needs its year: this year's (the next one when the date has passed is the person's to step) */
    private void fixYear() {
        if (edit.rule.repeat || (edit.rule.day != DuaRule.GREGORIAN && edit.rule.day != DuaRule.HIJRI) || edit.rule.year > 0) return;
        DuaRule.Day today = new DuaDays(this).day(0);
        edit.rule.year = edit.rule.day == DuaRule.HIJRI ? today.hy : today.gy;
    }

    private String when(long at) {
        java.util.Calendar c = java.util.Calendar.getInstance(sa.urtv.home.ZoneFix.zone(Place.get(this).tz), java.util.Locale.US);
        c.setTimeInMillis(at);
        DuaRule.Day today = new DuaDays(this).day(0);
        long days = Math.max(0, (at - today.midnight) / 86_400_000L);
        String clock = DuaWords.clock(c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE));
        if (days == 0) return S.t("اليوم %s", clock);
        if (days == 1) return S.t("غدًا %s", clock);
        return S.tv(DuaWords.DAYS[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]) + " " + Ui.arabic(c.get(java.util.Calendar.DAY_OF_MONTH)) + " " + clock;
    }

    private void rename() {
        final android.widget.EditText field = new android.widget.EditText(this);
        field.setText(edit.name);
        field.setSingleLine(true);
        field.setSelection(field.getText().length());
        new android.app.AlertDialog.Builder(this).setTitle(S.t("الاسم")).setView(field)
                .setPositiveButton(S.t("حفظ"), (dlg, w) -> {
                    String n = AudioRules.name(field.getText().toString() + ".x");
                    if (!n.isEmpty()) { edit.name = n; keep(); refresh(); }
                })
                .setNegativeButton(S.t("إلغاء"), null).show();
    }

    // ---- the days

    private void chooseDay() {
        ask(S.t("الأيام"), null, false,
                new String[]{S.t("كل يوم"), S.t("أيام من الأسبوع"), S.t("تاريخ ميلادي"), S.t("تاريخ هجري")},
                new Runnable[]{
                    () -> { edit.rule.day = DuaRule.EVERY_DAY; keep(); refresh(); },
                    this::chooseWeekdays,
                    () -> chooseDate(DuaRule.GREGORIAN),
                    () -> chooseDate(DuaRule.HIJRI)});
    }

    private void chooseWeekdays() {
        if (edit.rule.day != DuaRule.WEEKDAYS || edit.rule.weekdays == 0) {
            edit.rule.weekdays = 1 << new DuaDays(this).day(0).weekday;   // today, to start from
        }
        edit.rule.day = DuaRule.WEEKDAYS;
        keep();
        open(new Page() {
            @Override public String title() { return S.t("أيام من الأسبوع"); }
            @Override public String tagline() { return DuaWords.day(edit.rule); }
            @Override public void rows(List<Row> out) {
                for (int i = 0; i < 7; i++) {
                    final int bit = 1 << i;
                    boolean on = (edit.rule.weekdays & bit) != 0;
                    out.add(Row.of(S.tv(DuaWords.DAYS[i]), on ? S.t("مفعّل") : S.t("متوقف"), () -> {
                        int w = edit.rule.weekdays ^ bit;
                        if (w == 0) { toast(S.t("يبقى يوم واحد على الأقل")); return; }
                        edit.rule.weekdays = w;
                        keep();
                        refresh();
                    }));
                }
                out.add(Row.of(S.t("تم"), "", DuaActivity.this::back));
            }
        });
    }

    private void chooseDate(int kind) {
        DuaRule.Day today = new DuaDays(this).day(0);
        if (edit.rule.day != kind) {   // a new date starts from today's
            edit.rule.day = kind;
            edit.rule.month = kind == DuaRule.HIJRI ? today.hm : today.gm;
            edit.rule.date = kind == DuaRule.HIJRI ? today.hd : today.gd;
            edit.rule.year = kind == DuaRule.HIJRI ? today.hy : today.gy;
        }
        fixYear();
        keep();
        open(new Page() {
            @Override public String title() { return kind == DuaRule.HIJRI ? S.t("تاريخ هجري") : S.t("تاريخ ميلادي"); }
            @Override public String tagline() { return DuaWords.day(edit.rule); }
            @Override public void rows(List<Row> out) {
                int max = kind == DuaRule.HIJRI ? 30 : 31;
                out.add(Row.of(S.t("اليوم"), Ui.arabic(edit.rule.date), null).onStep(s -> { edit.rule.date = (edit.rule.date - 1 + s + max) % max + 1; keep(); }));
                out.add(Row.of(S.t("الشهر"), S.tv((kind == DuaRule.HIJRI ? DuaWords.HIJRI : DuaWords.GREG)[edit.rule.month - 1]), null)
                        .onStep(s -> { edit.rule.month = (edit.rule.month - 1 + s + 12) % 12 + 1; keep(); }));
                if (!edit.rule.repeat) {
                    int now = kind == DuaRule.HIJRI ? today.hy : today.gy;
                    out.add(Row.of(S.t("السنة"), Ui.arabic(edit.rule.year), null).onStep(s -> { edit.rule.year = Math.max(now, Math.min(now + 5, edit.rule.year + s)); keep(); }));
                }
                out.add(Row.of(S.t("يتكرر كل سنة"), edit.rule.repeat ? S.t("مفعّل") : S.t("متوقف"), () -> { edit.rule.repeat = !edit.rule.repeat; fixYear(); keep(); refresh(); }));
                out.add(Row.of(S.t("تم"), S.t("يمين ويسار يغيّران اليوم والشهر"), DuaActivity.this::back));
            }
        });
    }

    // ---- the time: three steps, the sentence above

    private void chooseTime() {
        ask(S.t("متى يُشغَّل؟"), DuaWords.time(edit.rule), false,
                new String[]{S.t("في ساعة محددة"), S.t("قبل صلاة"), S.t("بعد صلاة")},
                new Runnable[]{this::chooseClock, () -> choosePrayer(DuaRule.BEFORE), () -> choosePrayer(DuaRule.AFTER)});
    }

    private void chooseClock() {
        if (edit.rule.at != DuaRule.AT_CLOCK) { edit.rule.at = DuaRule.AT_CLOCK; edit.rule.clock = 10 * 60; }
        keep();
        open(new Page() {
            @Override public String title() { return S.t("في ساعة محددة"); }
            @Override public String tagline() { return DuaWords.time(edit.rule); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("الساعة"), DuaWords.clock(edit.rule.clock), null).onStep(s -> { edit.rule.clock = ((edit.rule.clock + 60 * s) % 1440 + 1440) % 1440; keep(); }));
                out.add(Row.of(S.t("الدقائق"), DuaWords.clock(edit.rule.clock), null).onStep(s -> { edit.rule.clock = ((edit.rule.clock + 5 * s) % 1440 + 1440) % 1440; keep(); }));
                out.add(Row.of(S.t("تم"), S.t("يمين ويسار يغيّران الوقت"), DuaActivity.this::back));
            }
        });
    }

    private static final int[] OFFSETS = {0, 5, 10, 15, 20, 30, 45, 60, 90, 120};

    private void choosePrayer(int at) {
        open(new Page() {
            @Override public String title() { return at == DuaRule.BEFORE ? S.t("قبل صلاة") : S.t("بعد صلاة"); }
            @Override public String tagline() { return S.t("الصلاة"); }
            @Override public void rows(List<Row> out) {
                int[] list = {PrayerTimes.FAJR, PrayerTimes.SUNRISE, PrayerTimes.DHUHR, PrayerTimes.ASR, PrayerTimes.MAGHRIB, PrayerTimes.ISHA};
                for (int p : list) out.add(Row.of(S.tv(PrayerTimes.NAMES[p]), "", () -> chooseOffset(at, p)));
            }
        });
    }

    private void chooseOffset(int at, int prayer) {
        open(new Page() {
            @Override public String title() { return at == DuaRule.BEFORE ? S.t("قبلها بـ") : S.t("بعدها بـ"); }
            @Override public String tagline() { return S.tv(PrayerTimes.NAMES[prayer]); }
            @Override public void rows(List<Row> out) {
                for (int o : OFFSETS) {
                    DuaRule r = edit.rule.copy() != null ? edit.rule.copy() : new DuaRule();
                    r.at = at; r.prayer = prayer; r.offset = o;
                    long next = r.next(System.currentTimeMillis(), new DuaDays(DuaActivity.this));
                    out.add(Row.of(o == 0 ? S.t("عند الأذان") : DuaWords.minutes(o), DuaWords.time(r) + (next > 0 ? S.t("، %s", when(next)) : ""), () -> {
                        edit.rule.at = at; edit.rule.prayer = prayer; edit.rule.offset = o;
                        keep();
                        back(); back();   // the offsets and the prayers: back on the dua's page
                    }));
                }
            }
        });
    }
}
