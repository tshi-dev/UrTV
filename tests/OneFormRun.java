package sa.urtv.screensaver;

/**
 * For tests/settings_one_form_test.py: every place of the 4.3.2 settings screen (the classic hub, frozen in tests/settings_432_places.tsv when its tables
 * left SettingsHub in 4.4 beta 4) and where it is in the one tree of every style now. Arg: that file.
 */
public final class OneFormRun {
    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(args[0]), java.nio.charset.StandardCharsets.UTF_8);
        java.util.Map<String, String> renamed = new java.util.HashMap<>();   // a row the findability review renamed: looked up by its new name
        for (String l : lines) if (l.startsWith("renamed\t")) renamed.put(l.split("\t")[1], l.split("\t")[2]);
        for (String l : lines) {
            if (l.startsWith("#") || l.isEmpty() || l.startsWith("renamed\t")) continue;
            String[] f = l.split("\t", -1);
            for (boolean classic : new boolean[] {true, false}) {   // every style answers the same (the layout is all classic changes)
                SettingsHub.classic(classic);
                if ("row".equals(f[0])) {
                    String now = null;
                    String name = renamed.getOrDefault(f[1], f[1]);
                    for (String[] n : SettingsHub.ROWS) if (n[0].equals(name)) { now = n[1]; break; }
                    out.append("row\t").append(f[1]).append('\t').append(f[2]).append('\t').append(now).append('\t').append(now == null ? "" : String.valueOf(SettingsHub.name(now))).append('\n');
                } else {
                    String now = SettingsHub.canon(f[1]);
                    out.append("tile\t").append(f[1]).append('\t').append(f[2]).append('\t').append(now).append('\t').append(now == null ? "" : String.valueOf(SettingsHub.name(now))).append('\t').append(f[3]).append('\n');
                }
            }
        }
        System.out.print(out);
    }
}
