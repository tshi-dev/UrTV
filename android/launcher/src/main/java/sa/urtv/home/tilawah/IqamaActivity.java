package sa.urtv.home.tilawah;

import java.util.List;

import sa.urtv.home.S;

/**
 * 4.4 «الإقامة»: a sound at the iqama (none by default, our short bell, or one of «صوتياتي») at its own volume, and «تنبيه الإقامة» (a notice
 * where the person is, over another app as the kick-off notice, never there in kids mode). The same rows and focus as «أذان كل صلاة» (RowScreen).
 */
public class IqamaActivity extends RowScreen {
    private Prefs prefs;

    @Override protected void onCreate(android.os.Bundle state) {
        prefs = new Prefs(this);
        super.onCreate(state);
    }

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("الإقامة"); }
            @Override public String tagline() { return S.t("الفجر بعد ٢٥ دقيقة من الأذان، والمغرب بعد ١٠، والبقية بعد ٢٠، ولا إقامة لظهر الجمعة"); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("صوت الإقامة"), soundName(IqamaActivity.this, prefs.iqamaSound()), () -> chooseSound()));
                out.add(Row.of(S.t("مستوى صوت الإقامة"), MainActivity.volumeText(prefs.iqamaVolume()), () -> hear()).onStep(d -> {
                    prefs.setIqamaVolume(prefs.iqamaVolume() + d * 10);
                    hear();
                }));
                out.add(Row.of(S.t("تنبيه الإقامة"), prefs.iqamaNotice() ? S.t("مفعّل: فوق التطبيقات الأخرى أيضًا، إلا في وضع الأطفال") : S.t("متوقف"), () -> {
                    prefs.setIqamaNotice(!prefs.iqamaNotice());
                    AdhanScheduler.schedule(IqamaActivity.this);   // the adhan's alarm sets the iqama's: it is wanted now
                    refresh();
                }));
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للاختيار، ويمين ويسار للتغيير مع سماعه، ورجوع للعودة"); }

    /** the sound's name: «بدون صوت», «نغمة قصيرة», or the sound's own */
    static String soundName(android.content.Context c, String sound) {
        if (sound.isEmpty()) return S.t("بدون صوت");
        if (sound.equals("chime")) return S.t("نغمة قصيرة");
        AudioStore.Item it = AudioStore.get(c, Integer.parseInt(sound.substring(1)));
        return it == null ? S.t("بدون صوت") : it.name;
    }

    /** the adhan tab's line */
    static String summary(android.content.Context c, Prefs prefs) {
        String s = soundName(c, prefs.iqamaSound());
        return prefs.iqamaNotice() ? S.t("%s، مع تنبيه", s) : s;
    }

    private void chooseSound() {
        open(new Page() {
            @Override public String title() { return S.t("صوت الإقامة"); }
            @Override public String tagline() { return soundName(IqamaActivity.this, prefs.iqamaSound()); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("بدون صوت"), S.t("الافتراضي"), () -> set("")));
                out.add(Row.of(S.t("نغمة قصيرة"), S.t("جرس هادئ من نغمتين"), () -> set("chime")));
                List<AudioStore.Item> mine = AudioStore.list(IqamaActivity.this);
                if (!mine.isEmpty()) out.add(Row.head(S.t("صوتياتي")));
                for (AudioStore.Item it : mine) {
                    final String c = "a" + it.id;
                    out.add(Row.of(it.name, "", () -> set(c)));
                }
            }
        });
    }

    private void set(String sound) {
        prefs.setIqamaSound(sound);
        AdhanScheduler.schedule(this);
        back();
        hear();
    }

    /** the chosen sound at its volume, now (nothing when none is chosen) */
    private void hear() {
        String path = prefs.iqamaSound().isEmpty() ? null : IqamaReceiver.source(this, prefs.iqamaSound());
        if (path != null) MusicService.dua(this, path, S.n("الإقامة"), prefs.iqamaVolume());
    }
}
