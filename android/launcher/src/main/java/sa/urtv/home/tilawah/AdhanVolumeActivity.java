package sa.urtv.home.tilawah;

import java.util.List;

import sa.urtv.home.S;

/**
 * 4.4 «مستوى صوت الأذان» (a user report: the adhan too loud and «لا أستطيع تغييره»): the general level («مثل صوت التلفزيون», or 0 to 100% of the
 * TV's highest volume), and each prayer's own («مثل العام» by default). OK on a level keeps it and plays two seconds of that adhan at it; left and
 * right on a row step it by 5. The same rows and focus as «أذان كل صلاة» (RowScreen).
 */
public class AdhanVolumeActivity extends RowScreen {
    private Prefs prefs;

    @Override protected void onCreate(android.os.Bundle state) {
        prefs = new Prefs(this);
        super.onCreate(state);
    }

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("مستوى صوت الأذان"); }
            @Override public String tagline() { return S.t("من أعلى صوت التلفزيون، ويعود صوت التلفزيون كما كان بعد الأذان"); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("لكل الصلوات"), MainActivity.volumeText(prefs.adhanVolumeLevel()), () -> choose(-1)).onStep(d -> {
                    prefs.setAdhanVolumeLevel(AdhanLevel.step(prefs.adhanVolumeLevel(), d));
                    hear(-1);
                }));
                out.add(Row.head(S.t("لكل صلاة")));
                for (int p : PrayerTimes.PRAYERS) {
                    final int prayer = p;
                    out.add(Row.of(S.tv(PrayerTimes.NAMES[p]), word(p), () -> choose(prayer)).onStep(d -> {
                        prefs.setPrayerVolume(prayer, AdhanLevel.choice(AdhanLevel.step(prefs.volumeFor(prayer), d)));
                        hear(prayer);
                    }));
                }
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للاختيار، ويمين ويسار للتغيير مع سماعه، ورجوع للعودة"); }

    /** a prayer's line: «مثل العام (٣٠٪)» or its own */
    private String word(int prayer) {
        String c = prefs.prayerVolume(prayer);
        if (c.isEmpty()) return S.t("مثل العام (%s)", MainActivity.volumeText(prefs.adhanVolumeLevel()));
        return MainActivity.volumeText(prefs.volumeFor(prayer));
    }

    /** the levels to pick from: a prayer can also follow the general one */
    private void choose(int prayer) {
        open(new Page() {
            @Override public String title() { return prayer < 0 ? S.t("لكل الصلوات") : S.tv(PrayerTimes.NAMES[prayer]); }
            @Override public String tagline() { return prayer < 0 ? MainActivity.volumeText(prefs.adhanVolumeLevel()) : word(prayer); }
            @Override public void rows(List<Row> out) {
                if (prayer >= 0) out.add(Row.of(S.t("مثل العام"), MainActivity.volumeText(prefs.adhanVolumeLevel()), () -> set(prayer, "")));
                out.add(Row.of(S.t("مثل صوت التلفزيون"), S.t("لا يتغير صوت التلفزيون"), () -> set(prayer, "tv")));
                for (int v = AdhanLevel.MIN; v <= AdhanLevel.MAX; v += 10) {
                    final String c = String.valueOf(v);
                    out.add(Row.of(MainActivity.volumeText(v), "", () -> set(prayer, c)));
                }
            }
        });
    }

    private void set(int prayer, String choice) {
        if (prayer < 0) prefs.setAdhanVolumeLevel(AdhanLevel.forPrayer(choice, prefs.adhanVolumeLevel()));
        else prefs.setPrayerVolume(prayer, choice);
        back();
        hear(prayer);
    }

    /** two seconds of the prayer's adhan (or the general one) at its level */
    private void hear(int prayer) {
        int voice = prayer < 0 ? prefs.playVoiceFor(this, false) : prefs.playFor(this, prayer);
        if (voice == PrayerSound.SILENT) voice = prefs.playVoiceFor(this, prayer == PrayerTimes.FAJR);   // a silent prayer: its general voice, to hear the level
        MusicService.adhan(this, voice, Adhan.name(voice), 2, prayer);
    }
}
