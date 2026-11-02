package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «اللغة», the plain-Java part (Tr.java) on a JVM; tests/i18n_test.py compiles and runs it.
 *   args[0] = a file of every S.t literal in both apps, one per line, escaped as the table is (\n, \t, \\)
 *   args[1] = a file of texts whose placeholders it prints as "P<TAB>text<TAB>1,2" for the Python side to compare
 */
public class TrRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> table = Tr.parse("# a comment\n\nحفظ\tSave\nبقي %s دقيقة\t%s minutes left\nمن %s إلى %s\tto %2$s from %1$s\r\n"
            + "سطر\\nثان\tline\\ntwo\nالهلال\t=\nبلا تبويب\nكثير\tمن\tالتبويبات\n\tبلا عربي\nبلا إنجليزي\t\n");
        check("parse: the entries, \\n as a line break, a CR at the end dropped, «=» as the Arabic itself", table.size() == 5 && "Save".equals(table.get("حفظ"))
            && "الهلال".equals(table.get("الهلال"))
            && "line\ntwo".equals(table.get("سطر\nثان")) && "to %2$s from %1$s".equals(table.get("من %s إلى %s")), table.toString());
        check("parse: a line without exactly one tab, or with an empty side, is skipped", !table.containsKey("بلا تبويب") && !table.containsKey("كثير")
            && !table.containsKey("") && !table.containsKey("بلا إنجليزي"), table.toString());

        check("ar: the text itself, even when the table has it", "حفظ".equals(Tr.t("ar", table, "حفظ")), Tr.t("ar", table, "حفظ"));
        check("ar: with no table", "حفظ".equals(Tr.t("ar", null, "حفظ")), "");
        check("ar with an argument: exactly the concatenation it replaces", ("بقي " + 5 + " دقيقة").equals(Tr.t("ar", table, "بقي %s دقيقة", 5)), Tr.t("ar", table, "بقي %s دقيقة", 5));
        check("ar with two: in order", ("من " + 3 + " إلى " + 7.5).equals(Tr.t("ar", table, "من %s إلى %s", 3, 7.5)), Tr.t("ar", table, "من %s إلى %s", 3, 7.5));
        check("ar: a % with no arguments is left alone", "٥٠%".equals(Tr.t("ar", table, "٥٠%")) && "100%s".equals(Tr.t("ar", table, "100%s")), "");
        check("ar: null as an argument is the word null, as + gives", ("قيمة " + null).equals(Tr.t("ar", table, "قيمة %s", (Object) null)), Tr.t("ar", table, "قيمة %s", (Object) null));
        check("en: the entry", "Save".equals(Tr.t("en", table, "حفظ")), "");
        check("en: its arguments, by number", "to 7 from 3".equals(Tr.t("en", table, "من %s إلى %s", 3, 7)), Tr.t("en", table, "من %s إلى %s", 3, 7));
        check("en: no entry, the Arabic (the build test fails on it first)", "غير مترجم".equals(Tr.t("en", table, "غير مترجم")), "");
        check("en: no table, the Arabic", "حفظ".equals(Tr.t("en", null, "حفظ")), "");
        check("format: %% is one %, a missing argument stays as written", "50% x %s".equals(Tr.format("%s%% x %s", 50)) && "a %3$s".equals(Tr.format("a %3$s", 1)), Tr.format("%s%% x %s", 50));

        check("the setting: ar and en only", Tr.valid("ar") && Tr.valid("en") && !Tr.valid("EN") && !Tr.valid("") && !Tr.valid(null) && !Tr.valid("fr"), "");
        check("norm: anything but en is ar", "ar".equals(Tr.norm(null)) && "ar".equals(Tr.norm("fr")) && "en".equals(Tr.norm("en")) && "ar".equals(Tr.norm("EN")), "");
        check("the fragment: nothing for ar (the page and the home as they were), &lang=en for en", "".equals(Tr.fragment("ar")) && "".equals(Tr.fragment(null))
            && "&lang=en".equals(Tr.fragment("en")), Tr.fragment("ar"));
        check("fromFragment: en only with lang=en as a whole pair", "en".equals(Tr.fromFragment("city=x&lang=en&digits=hindi")) && "en".equals(Tr.fromFragment("lang=en"))
            && "ar".equals(Tr.fromFragment("city=x&langs=English")) && "ar".equals(Tr.fromFragment("city=x&xlang=en")) && "ar".equals(Tr.fromFragment(null))
            && "ar".equals(Tr.fromFragment("city=x&lang=fr")), "");
        check("the page's file per language", "tv.html".equals(Tr.pageName("ar")) && "tv-en.html".equals(Tr.pageName("en")) && "tv.html".equals(Tr.pageName(null)), "");
        check("the phone page's asset per language", "phone/index.html".equals(Tr.phonePageName("ar")) && "phone/index.en.html".equals(Tr.phonePageName("en")), "");
        check("unescape: \\t \\n \\\\ \\uXXXX, anything else as written", "a\tb\nc\\d e\\qf\\u12".equals(Tr.unescape("a\\tb\\nc\\\\d\\u00a0e\\qf\\u12"))
            && "\\u-001".equals(Tr.unescape("\\u-001")) && "\\u٠٠٠١".equals(Tr.unescape("\\u٠٠٠١")), Tr.unescape("a\\tb\\nc\\\\d\\u00a0e\\qf\\u12"));

        // every literal of both apps: with Arabic each comes back byte for byte, with or without an entry, with or without its arguments filled
        List<String> lits = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        Map<String, String> all = new HashMap<>();
        for (String l : lits) all.put(Tr.unescape(l), "x");
        int bad = 0;
        String first = "";
        for (String l : lits) {
            String s = Tr.unescape(l);
            boolean same = s.equals(Tr.t("ar", all, s)) && s.equals(Tr.t("ar", null, s));
            if (Tr.placeholders(s).isEmpty()) same &= s.equals(Tr.t("ar", all, s, new Object[0]));
            if (!same) { bad++; first = s; }
        }
        check("ar: every S.t literal of both apps (" + lits.size() + ") is itself, byte for byte", bad == 0, first);

        for (String l : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8)) {
            StringBuilder p = new StringBuilder();
            for (int k : Tr.placeholders(Tr.unescape(l))) p.append(p.length() > 0 ? "," : "").append(k);
            System.out.println("P\t" + l + "\t" + p);
        }
        System.exit(failed == 0 ? 0 : 1);
    }
}
