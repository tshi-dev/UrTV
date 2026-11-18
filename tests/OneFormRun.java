package sa.urtv.screensaver;

/** For tests/settings_one_form_test.py: every place of the 4.3.2 settings screen (the classic hub) and where it is in the one form (4.5). */
public final class OneFormRun {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        SettingsHub.classic(true);
        java.util.List<String[]> rows = new java.util.ArrayList<>(), cats = new java.util.ArrayList<>();
        for (String[] r : SettingsHub.ROWS_432) rows.add(new String[] {r[0], r[1]});
        for (String[] c : SettingsHub.CATEGORIES_432) cats.add(new String[] {c[0], c[1], c[4]});
        SettingsHub.classic(false);
        for (String[] r : rows) {
            String now = null;
            for (String[] n : SettingsHub.ROWS) if (n[0].equals(r[0])) { now = n[1]; break; }
            out.append("row\t").append(r[0]).append('\t').append(r[1]).append('\t').append(now).append('\t').append(now == null ? "" : String.valueOf(SettingsHub.name(now))).append('\n');
        }
        for (String[] c : cats) {
            String now = SettingsHub.canon(c[0]);
            out.append("tile\t").append(c[0]).append('\t').append(c[1]).append('\t').append(now).append('\t').append(now == null ? "" : String.valueOf(SettingsHub.name(now))).append('\t').append(c[2]).append('\n');
        }
        System.out.print(out);
    }
}
