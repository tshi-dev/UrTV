package sa.urtv.home.tilawah;

import java.util.List;

import sa.urtv.home.S;

/**
 * 4.4 «أذان كل صلاة» (docs/mockups/audio-prayers.png): for each of the five prayers, its adhan's sound: like the general adhan (the default, as before),
 * silent (its time, card and bar stay), a muezzin of ours, or the person's own recording. Kept at once; the alarm is the same one.
 */
public class PrayerSoundActivity extends RowScreen {
    private Prefs prefs;

    @Override protected void onCreate(android.os.Bundle state) {
        prefs = new Prefs(this);
        super.onCreate(state);
    }

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("الأذان لكل صلاة"); }
            @Override public String tagline() { return S.t("لكل صلاة: صوتها وظهورها على الشاشة (الشريط والتنبيه)، كلٌّ على حدة"); }
            @Override public void rows(List<Row> out) {
                // 4.4: the common wish in one OK: every prayer heard, Fajr silent (its time, card and notice stay); OK again: all five heard
                out.add(Row.of(S.t("كل الصلوات بصوت عدا الفجر"), allButFajr(prefs) ? S.t("مفعّل") : S.t("متوقف"), () -> {
                    boolean on = !allButFajr(prefs);
                    for (int p : PrayerTimes.PRAYERS) {
                        if (p == PrayerTimes.FAJR) prefs.setPrayerSound(p, on ? "silent" : "");
                        else if ("silent".equals(prefs.prayerSound(p))) prefs.setPrayerSound(p, "");   // the others heard; their own muezzin kept
                    }
                    refresh();
                }));
                out.add(Row.head(S.t("كل صلاة")));
                for (int p : PrayerTimes.PRAYERS) out.add(Row.of(S.tv(PrayerTimes.NAMES[p]), PrayerScreen.word(prefs.prayerState(p)), () -> choose(p)));
                // touch «أذان الجمعة»: a silent Dhuhr is heard on Fridays
                out.add(Row.of(S.t("أذان الجمعة"), prefs.fridayDhuhr() ? S.t("مفعّل: الظهر يُسمع يوم الجمعة ولو كان صامتًا") : S.t("متوقف"),
                        () -> { prefs.setFridayDhuhr(!prefs.fridayDhuhr()); refresh(); }));
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للاختيار: صامت، أو مثل الأذان العام، أو مؤذن، أو تسجيلي"); }

    private String word(int prayer) {
        String c = prefs.prayerSound(prayer);
        int general = prefs.playVoiceFor(this, prayer == PrayerTimes.FAJR);
        if (c.isEmpty()) return S.t("مثل الأذان العام (%s)", Adhan.name(general));
        if (c.equals("silent")) return S.t("صامت");
        int v = prefs.playFor(this, prayer);
        return v == PrayerSound.SILENT ? S.t("صامت") : Adhan.name(v);
    }

    private void choose(int prayer) {
        open(new Page() {
            @Override public String title() { return S.tv(PrayerTimes.NAMES[prayer]); }
            @Override public String tagline() { return PrayerScreen.word(prefs.prayerState(prayer)); }
            @Override public void rows(List<Row> out) {
                // 4.4 beta 4: the four states, one focus target each (the UI session: no two toggles in one row); the muezzin below is for the sound
                int now = prefs.prayerState(prayer);
                for (int st : new int[] {PrayerScreen.BOTH, PrayerScreen.SOUND_ONLY, PrayerScreen.SCREEN_ONLY, PrayerScreen.NONE})
                    out.add(Row.of(PrayerScreen.word(st), st == now ? S.t("الحالي") : "", () -> { prefs.setPrayerState(prayer, st); back(); }));
                out.add(Row.head(S.t("المؤذن")));
                int general = prefs.playVoiceFor(PrayerSoundActivity.this, prayer == PrayerTimes.FAJR);
                out.add(Row.of(S.t("مثل الأذان العام"), Adhan.name(general), () -> set(prayer, "")));
                if (OwnAdhan.exists(PrayerSoundActivity.this)) out.add(Row.of(S.tv(OwnAdhanText.NAME), "", () -> set(prayer, "own")));
                if (OwnAdhan.fajrExists(PrayerSoundActivity.this)) out.add(Row.of(S.tv(OwnAdhanText.NAME_FAJR), "", () -> set(prayer, "own_fajr")));
                for (int v = 1; v <= Adhan.VOICES; v++) {
                    if (AdhanVoices.isFajr(v) && prayer != PrayerTimes.FAJR) continue;   // «الصلاة خير من النوم»: Fajr only
                    final String c = "v" + v;
                    final int voice = v;
                    out.add(Row.of(Adhan.name(v), "", () -> set(prayer, c)).onLong(() -> MusicService.adhan(PrayerSoundActivity.this, voice, Adhan.name(voice), 6)));
                }
            }
        });
    }

    private void set(int prayer, String choice) {
        prefs.setPrayerSound(prayer, choice);
        back();
    }

    /** 4.4: Fajr silent and the four others heard */
    static boolean allButFajr(Prefs prefs) {
        for (int p : PrayerTimes.PRAYERS) if ("silent".equals(prefs.prayerSound(p)) != (p == PrayerTimes.FAJR)) return false;
        return true;
    }

    /** the adhan tab's line: «الخمس بصوت», or which ones are silent */
    static String summary(Prefs prefs) {
        int[] st = new int[PrayerScreen.IDS.length];
        String[] names = new String[st.length];
        for (int i = 0; i < st.length; i++) { st[i] = prefs.prayerState(PrayerScreen.IDS[i]); names[i] = S.tv(PrayerTimes.NAMES[PrayerScreen.IDS[i]]); }
        return PrayerScreen.summary(st, names);
    }
}
