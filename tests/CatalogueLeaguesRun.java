package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/catalogue_leagues_test.py: CatalogueLeagues on made-up entries. One line per check: name TAB ok|FAIL. */
public final class CatalogueLeaguesRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        String e = CatalogueLeagues.entry("4866", "كأس آسيا", "AFC Asian Cup");
        check("an entry: id, Arabic, English", "id4866|كأس آسيا|AFC Asian Cup".equals(e));
        check("a name loses the list's separators and is cut at 60", "id1|a b c d|x".equals(CatalogueLeagues.entry("1", "a;b,c|d", "x~"))
            && CatalogueLeagues.entry("2", new String(new char[90]).replace('\0', 'z'), "").length() == "id2|".length() + 60 + 1);
        check("an id that is not one is refused", CatalogueLeagues.entry("12a", "x", "y") == null && CatalogueLeagues.entry("1234567", "x", "y") == null
            && CatalogueLeagues.entry(null, "x", "y") == null);
        String[] p = CatalogueLeagues.parse(e);
        check("parse gives it back", p != null && Arrays.equals(p, new String[] {"4866", "كأس آسيا", "AFC Asian Cup"}));
        check("a bare id (saver.sh) parses, with no names", Arrays.equals(CatalogueLeagues.parse("id4866"), new String[] {"4866", "", ""}));
        check("a built-in league's name is not one", CatalogueLeagues.parse("Saudi-Arabian Pro League") == null && CatalogueLeagues.parse("idea|x|y") == null);
        check("the name in the setting's language, the other next, then #id", "كأس آسيا".equals(CatalogueLeagues.name(p, false)) && "AFC Asian Cup".equals(CatalogueLeagues.name(p, true))
            && "AFC Asian Cup".equals(CatalogueLeagues.name(new String[] {"4866", "", "AFC Asian Cup"}, false)) && "#4866".equals(CatalogueLeagues.name(new String[] {"4866", "", ""}, true)));
        check("the page's token: id4866 for a catalogue league, the name for a built-in one", "id4866".equals(CatalogueLeagues.pageToken(e)) && "NBA".equals(CatalogueLeagues.pageToken("NBA")));
        List<String> l = Arrays.asList("Saudi-Arabian Pro League", e, "id77", CatalogueLeagues.entry("5", "", "Andorran Cup"));
        check("#lgn: the catalogue ones with a name, in the language", "4866~كأس آسيا|5~Andorran Cup".equals(CatalogueLeagues.names(l, false))
            && "4866~AFC Asian Cup|5~Andorran Cup".equals(CatalogueLeagues.names(l, true)) && "".equals(CatalogueLeagues.names(Arrays.asList("NBA"), false)));
        check("the same league, stored with names or bare", CatalogueLeagues.same(e, "id4866") && !CatalogueLeagues.same(e, "id4867") && CatalogueLeagues.same("NBA", "nba"));
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"1", "A", "", "England", "إنجلترا", "Soccer", "1"});
        rows.add(new String[] {"2", "B", "", "Andorra", "", "Soccer", ""});
        rows.add(new String[] {"3", "C", "", "england", "إنجلترا", "Soccer", ""});
        List<String[]> c = CatalogueLeagues.countries(rows, false);
        check("the countries once each, in the catalogue's order, Arabic where named", c.size() == 2 && "إنجلترا".equals(c.get(0)[1]) && "Andorra".equals(c.get(1)[1])
            && "England".equals(CatalogueLeagues.countries(rows, true).get(0)[1]));
    }
}
