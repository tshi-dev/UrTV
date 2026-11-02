package sa.urtv.screensaver;

/** For tests/user_options_test.py: «الآية والحديث» (VerseKind.java) and «الخروج من تلفزيونك» (HomeExit.java) on a plain JVM. */
public final class VerseKindRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("versekind: both, verse and hadith are accepted", VerseKind.valid("both") && VerseKind.valid("verse") && VerseKind.valid("hadith"), "");
        check("versekind: anything else is refused (Verse, quran, day, 3, empty, null)", !VerseKind.valid("Verse") && !VerseKind.valid("quran")
                && !VerseKind.valid("day") && !VerseKind.valid("3") && !VerseKind.valid("") && !VerseKind.valid(null), "");
        check("versekind stored: unknown or missing is both, as always", VerseKind.read(null).equals("both") && VerseKind.read("x").equals("both")
                && VerseKind.read("verse").equals("verse") && VerseKind.read("hadith").equals("hadith"), VerseKind.read(null));
        check("versekind fragment: nothing for both, so the page and the URL are as they were", VerseKind.fragment("both").isEmpty()
                && VerseKind.fragment(null).isEmpty() && VerseKind.fragment("x").isEmpty(), VerseKind.fragment("both"));
        check("versekind fragment: &versekind=verse / &versekind=hadith", VerseKind.fragment("verse").equals("&versekind=verse")
                && VerseKind.fragment("hadith").equals("&versekind=hadith"), VerseKind.fragment("verse"));
        check("versekind row: «آية وحديث» first, «آية فقط», «حديث فقط»", VerseKind.CHOICES.length == 3 && VerseKind.CHOICES[0].equals("آية وحديث")
                && VerseKind.CHOICES[1].equals("آية فقط") && VerseKind.CHOICES[2].equals("حديث فقط"), String.join(",", VerseKind.CHOICES));
        boolean round = true;
        for (int i = 0; i < 3; i++) round &= VerseKind.choice(VerseKind.fromChoice(i)) == i && VerseKind.valid(VerseKind.fromChoice(i));
        check("versekind row: a choice gives the agent's value and back; the label is the one set", round && VerseKind.label("hadith").equals("حديث فقط")
                && VerseKind.label(null).equals("آية وحديث"), "");

        check("backexit: press, hold and three are accepted", HomeExit.valid("press") && HomeExit.valid("hold") && HomeExit.valid("three"), "");
        check("backexit: anything else is refused (3, long, HOLD, empty, null)", !HomeExit.valid("3") && !HomeExit.valid("long")
                && !HomeExit.valid("HOLD") && !HomeExit.valid("") && !HomeExit.valid(null), "");
        check("backexit stored: unknown or missing is one press, as always", HomeExit.read(null).equals("press") && HomeExit.read("x").equals("press")
                && HomeExit.read("hold").equals("hold") && HomeExit.read("three").equals("three"), HomeExit.read(null));
        check("backexit fragment: nothing for one press; &backexit=hold / &backexit=three", HomeExit.fragment("press").isEmpty()
                && HomeExit.fragment(null).isEmpty() && HomeExit.fragment("hold").equals("&backexit=hold")
                && HomeExit.fragment("three").equals("&backexit=three"), HomeExit.fragment("hold"));
        check("backexit row: «ضغطة رجوع» first, «ضغطة رجوع مطوّلة», «ثلاث ضغطات رجوع»", HomeExit.CHOICES.length == 3
                && HomeExit.CHOICES[0].equals("ضغطة رجوع") && HomeExit.CHOICES[1].equals("ضغطة رجوع مطوّلة")
                && HomeExit.CHOICES[2].equals("ثلاث ضغطات رجوع"), String.join(",", HomeExit.CHOICES));
        round = true;
        for (int i = 0; i < 3; i++) round &= HomeExit.choice(HomeExit.fromChoice(i)) == i && HomeExit.valid(HomeExit.fromChoice(i));
        check("backexit row: a choice gives the agent's value and back", round && HomeExit.label("three").equals("ثلاث ضغطات رجوع"), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
