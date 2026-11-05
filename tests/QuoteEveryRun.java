package sa.urtv.screensaver;

/** For tests/quote_every_test.py: «تبديل الآية والحديث» (QuoteEvery.java) on a plain JVM. */
public final class QuoteEveryRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("receiver: day and 3 are accepted", QuoteEvery.valid("day") && QuoteEvery.valid("3"), "");
        check("receiver: anything else is refused (2, 03, 3m, DAY, on, empty, null)",
                !QuoteEvery.valid("2") && !QuoteEvery.valid("03") && !QuoteEvery.valid("3m") && !QuoteEvery.valid("DAY")
                && !QuoteEvery.valid("on") && !QuoteEvery.valid("") && !QuoteEvery.valid(null), "");
        check("stored: an unknown or missing value is the day, as always", QuoteEvery.read(null).equals("day") && QuoteEvery.read("x").equals("day")
                && QuoteEvery.read("3").equals("3"), QuoteEvery.read(null));
        check("fragment: nothing for the day, so the page and the URL are as they were", QuoteEvery.fragment("day").isEmpty()
                && QuoteEvery.fragment(null).isEmpty() && QuoteEvery.fragment("x").isEmpty(), QuoteEvery.fragment("day"));
        check("fragment: &versemin=3 for every 3 minutes", QuoteEvery.fragment("3").equals("&versemin=3"), QuoteEvery.fragment("3"));
        check("settings row: two choices, «كل يوم» first and «كل ٣ دقائق»",
                QuoteEvery.CHOICES.length == 2 && QuoteEvery.CHOICES[0].equals("كل يوم") && QuoteEvery.CHOICES[1].equals("كل ٣ دقائق"),
                String.join(",", QuoteEvery.CHOICES));
        check("settings row: its value is the one set, the picker checks it", QuoteEvery.label("day").equals("كل يوم") && QuoteEvery.label("3").equals("كل ٣ دقائق")
                && QuoteEvery.choice("day") == 0 && QuoteEvery.choice("3") == 1, QuoteEvery.label("3"));
        check("settings row: a choice gives the same value as the agent's command, and back",
                QuoteEvery.fromChoice(0).equals("day") && QuoteEvery.fromChoice(1).equals("3")
                && QuoteEvery.choice(QuoteEvery.fromChoice(0)) == 0 && QuoteEvery.choice(QuoteEvery.fromChoice(1)) == 1, "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
