package sa.urtv.screensaver;

/** For tests/quote_every_test.py: «تبديل الآية والحديث» (QuoteEvery.java) on a plain JVM. */
public final class QuoteEveryRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("receiver: day, 3 and 10 are accepted", QuoteEvery.valid("day") && QuoteEvery.valid("3") && QuoteEvery.valid("10"), "");
        check("receiver: anything else is refused (2, 03, 3m, DAY, on, empty, null)",
                !QuoteEvery.valid("2") && !QuoteEvery.valid("03") && !QuoteEvery.valid("3m") && !QuoteEvery.valid("DAY")
                && !QuoteEvery.valid("on") && !QuoteEvery.valid("") && !QuoteEvery.valid(null), "");
        check("stored: an unknown or missing value is the day, as always", QuoteEvery.read(null).equals("day") && QuoteEvery.read("x").equals("day")
                && QuoteEvery.read("3").equals("3") && QuoteEvery.read("10").equals("10"), QuoteEvery.read(null));
        check("fragment: nothing for the day, so the page and the URL are as they were", QuoteEvery.fragment("day").isEmpty()
                && QuoteEvery.fragment(null).isEmpty() && QuoteEvery.fragment("x").isEmpty(), QuoteEvery.fragment("day"));
        check("fragment: &versemin=3 for every 3 minutes, &versemin=10 for every 10", QuoteEvery.fragment("3").equals("&versemin=3") && QuoteEvery.fragment("10").equals("&versemin=10"), QuoteEvery.fragment("10"));
        check("settings row: three choices, «كل يوم» first, «كل ٣ دقائق» and «كل ١٠ دقائق»",
                QuoteEvery.CHOICES.length == 3 && QuoteEvery.CHOICES[0].equals("كل يوم") && QuoteEvery.CHOICES[1].equals("كل ٣ دقائق") && QuoteEvery.CHOICES[2].equals("كل ١٠ دقائق"),
                String.join(",", QuoteEvery.CHOICES));
        check("settings row: its value is the one set, the picker checks it", QuoteEvery.label("day").equals("كل يوم") && QuoteEvery.label("3").equals("كل ٣ دقائق")
                && QuoteEvery.choice("day") == 0 && QuoteEvery.choice("3") == 1 && QuoteEvery.label("10").equals("كل ١٠ دقائق") && QuoteEvery.choice("10") == 2, QuoteEvery.label("3"));
        check("settings row: a choice gives the same value as the agent's command, and back",
                QuoteEvery.fromChoice(0).equals("day") && QuoteEvery.fromChoice(1).equals("3")
                && QuoteEvery.fromChoice(2).equals("10") && QuoteEvery.choice(QuoteEvery.fromChoice(0)) == 0 && QuoteEvery.choice(QuoteEvery.fromChoice(1)) == 1 && QuoteEvery.choice(QuoteEvery.fromChoice(2)) == 2, "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
