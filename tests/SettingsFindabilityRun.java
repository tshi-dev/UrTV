package sa.urtv.screensaver;

/**
 * For tests/settings_findability_test.py: the settings tree as each style sees it (SettingsHub.classic: «أزرار» and tvOS true, «كنبة» false), on a plain
 * JVM. Args: the style names, then "--" and the rows to look up, then "--" and the words to search. Lines: "row<TAB>style<TAB>name<TAB>category<TAB>parent
 * <TAB>rank", "cat<TAB>style<TAB>key<TAB>parent", "search<TAB>style<TAB>word<TAB>row=category…".
 */
public final class SettingsFindabilityRun {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        int a = java.util.Arrays.asList(args).indexOf("--"), b = java.util.Arrays.asList(args).subList(a + 1, args.length).indexOf("--") + a + 1;
        for (int s = 0; s < a; s++) {
            String style = args[s];
            SettingsHub.classic(!"couch".equals(style));
            for (String c : SettingsHub.tiles(null)) out.append("cat\t").append(style).append('\t').append(c).append('\t').append(SettingsHub.parent(c)).append('\n');
            for (String[] c : SettingsHub.CATEGORIES) if (SettingsHub.parent(c[0]) != null) out.append("cat\t").append(style).append('\t').append(c[0]).append('\t').append(SettingsHub.parent(c[0])).append('\n');
            for (int i = a + 1; i < b; i++) {
                String key = FocusKey.key(args[i]), cat = SettingsHub.of(key, "MISSING");
                out.append("row\t").append(style).append('\t').append(args[i]).append('\t').append(cat).append('\t').append(SettingsHub.parent(cat)).append('\t')
                    .append(SettingsHub.rank(cat, key)).append('\n');
            }
            for (int i = b + 1; i < args.length; i++) {
                out.append("search\t").append(style).append('\t').append(args[i]);
                for (String[] r : SettingsHub.search(args[i])) out.append('\t').append(r[0]).append('=').append(r[1]);
                out.append('\n');
            }
        }
        System.out.print(out);
    }
}
