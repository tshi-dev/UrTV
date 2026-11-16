package sa.urtv.home.tilawah;

import java.util.List;

import sa.urtv.home.S;

/** 4.4 «ساعات الهدوء»: on or off, from, to (left and right by half an hour), and the adhan inside them: heard, Fajr only, or silent. */
public class QuietActivity extends RowScreen {
    private Prefs prefs;

    @Override protected void onCreate(android.os.Bundle state) {
        prefs = new Prefs(this);
        super.onCreate(state);
    }

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("ساعات الهدوء"); }
            @Override public String tagline() { return S.t("لا أدعية ولا تذكيرات وقت النوم، والأذان كما تختار"); }
            @Override public void rows(List<Row> out) {
                boolean on = prefs.quietOn();
                out.add(Row.of(S.t("ساعات الهدوء"), on ? S.t("مفعّل") : S.t("متوقف"), () -> set(!prefs.quietOn(), prefs.quietFrom(), prefs.quietTo(), prefs.quietAdhan())));
                out.add(Row.of(S.t("من"), DuaWords.clock(prefs.quietFrom()), null).onStep(s -> set(prefs.quietOn(), prefs.quietFrom() + 30 * s, prefs.quietTo(), prefs.quietAdhan())));
                out.add(Row.of(S.t("إلى"), DuaWords.clock(prefs.quietTo()), null).onStep(s -> set(prefs.quietOn(), prefs.quietFrom(), prefs.quietTo() + 30 * s, prefs.quietAdhan())));
                out.add(Row.of(S.t("الأذان فيها"), adhanWord(prefs.quietAdhan()),
                        () -> set(prefs.quietOn(), prefs.quietFrom(), prefs.quietTo(), (prefs.quietAdhan() + 1) % 3)));
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للتغيير، ويمين ويسار للوقت، ورجوع للعودة"); }

    static String adhanWord(int p) {
        return p == QuietHours.ADHAN_SILENT ? S.t("صامت") : p == QuietHours.ADHAN_FAJR_ONLY ? S.t("الفجر فقط") : S.t("يُسمع");
    }

    private void set(boolean on, int from, int to, int adhan) {
        prefs.setQuiet(on, from, to, adhan);
        refresh();
    }

    /** the adhan tab's line */
    static String summary(Prefs prefs) {
        return prefs.quietOn() ? S.t("من %1$s إلى %2$s", DuaWords.clock(prefs.quietFrom()), DuaWords.clock(prefs.quietTo())) : S.t("متوقف");
    }
}
