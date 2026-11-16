package sa.urtv.screensaver;

/** tests/settings_classic_test.py runs this: SettingsHub's answers in each layout, one per line */
public final class SettingsClassicRun {
    public static void main(String[] a) {
        for (boolean classic : new boolean[] {true, false}) {
            SettingsHub.classic(classic);
            String l = classic ? "432" : "44";
            System.out.println(l + "\ttiles\t" + String.join(",", SettingsHub.tiles(null)));
            System.out.println(l + "\tcards\t" + String.join(",", SettingsHub.tiles("cards")));
            System.out.println(l + "\tmore\t" + String.join(",", SettingsHub.tiles("more")));
            for (String row : new String[] {"المدينة", "شكل كنبة", "الأذان", "المحتوى", "الأرقام", "التصنيفات", "مسح الملفات المؤقتة", "فرق التاريخ الهجري", "بحث في الإعدادات"})
                System.out.println(l + "\tof\t" + row + "\t" + SettingsHub.of(row, null));
            for (String q : new String[] {"مدينه", "صوت", "كنبه", "واجهه المصنع"}) {
                StringBuilder b = new StringBuilder();
                for (String[] r : SettingsHub.search(q)) b.append(r[0]).append("@").append(r[1]).append(";");
                System.out.println(l + "\tsearch\t" + q + "\t" + b);
            }
            System.out.println(l + "\tdirect\t" + SettingsHub.direct("city") + "\t" + SettingsHub.direct("search"));
            System.out.println(l + "\twide\t" + SettingsHub.wide(null));
            System.out.println(l + "\tcanon\t" + SettingsHub.canon("pray") + "\t" + SettingsHub.canon("look") + "\t" + SettingsHub.canon("cards"));
        }
    }
}
