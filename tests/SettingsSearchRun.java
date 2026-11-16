package sa.urtv.screensaver;

/** For tests/settings_sections_test.py: SettingsHub's search, canon and order on a plain JVM. One line per question. */
public final class SettingsSearchRun {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (String q : args) {
            out.append("search\t").append(q);
            for (String[] r : SettingsHub.search(q)) out.append('\t').append(r[0]).append('=').append(r[1]);
            out.append('\n');
        }
        for (String[] o : SettingsHub.OLD) out.append("canon\t").append(o[0]).append('\t').append(SettingsHub.canon(o[0])).append('\n');
        out.append("canon\tlook\t").append(SettingsHub.canon("look")).append('\n').append("canon\tnope\t").append(SettingsHub.canon("nope")).append('\n');
        for (String[] r : SettingsHub.ROWS) out.append("row\t").append(r[0]).append('\t').append(r[1]).append('\t').append(SettingsHub.rank(r[1], r[0])).append('\n');
        System.out.print(out);
    }
}
