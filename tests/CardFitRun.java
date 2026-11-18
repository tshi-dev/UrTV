package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/card_fit_test.py: CardFit's answers, one line each. */
public final class CardFitRun {
    public static void main(String[] a) {
        StringBuilder out = new StringBuilder();
        for (String c : new String[] {"stocks", "prayers", "forecast", "adhkar", "matches", "tasks", "shows", "nextprayer", "prayerrow", "hourly", "verse", "none"})
            out.append("suits\t").append(c).append('\t').append(CardFit.suits(c, "couch")).append('\t').append(CardFit.suits(c, "classic")).append('\t').append(CardFit.note(c, "couch")).append('\n');
        String[][] cases = {{"stocks,prayers,forecast"}, {"prayers,prayerrow"}, {"adhkar,verse,hourly"}, {"matches"}, {"prayerrow,hourly"}};
        for (String[] k : cases) {
            List<String[]> swaps = new ArrayList<>();
            List<String> f = CardFit.fitted(Arrays.asList(k[0].split(",")), "couch", swaps);
            List<String> back = CardFit.fitted(Arrays.asList(k[0].split(",")), "classic", null);
            out.append("fit\t").append(k[0]).append('\t').append(String.join(",", f)).append('\t').append(swaps.size()).append('\t').append(String.join(",", back))
               .append('\t').append(CardFit.swapNote(swaps, new String[][] {{"prayers", "مواقيت الصلاة اليوم"}, {"prayerrow", "الصلوات في صف"}, {"forecast", "طقس الأيام القادمة"}, {"hourly", "طقس الساعات القادمة"}})).append('\n');
        }
        System.out.print(out);
    }
}
