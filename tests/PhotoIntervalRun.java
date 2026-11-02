package sa.urtv.screensaver;

/** For tests/photo_interval_test.py: «تبديل الصورة كل» with 1 minute and 30 seconds (PhotoInterval.java) on a plain JVM. */
public final class PhotoIntervalRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        long big = 2048, g08 = 1765, cast = 1397, small = 907;
        check("a 2 GB TV (2048 or 1765 MB, as the reports say) may have 30 seconds; 1.5 GB, 1 GB and unknown may not",
                PhotoInterval.fast(big) && PhotoInterval.fast(g08) && !PhotoInterval.fast(cast) && !PhotoInterval.fast(small)
                && !PhotoInterval.fast(0) && !PhotoInterval.fast(-1), "");
        check("2 GB: the choices are 30 seconds, 1, 5, 10, 15, 30, 60 minutes",
                String.join(",", PhotoInterval.choices(big)).equals("30s,1,5,10,15,30,60"), String.join(",", PhotoInterval.choices(big)));
        check("under 2 GB: the same without 30 seconds (1 minute is the shortest)",
                String.join(",", PhotoInterval.choices(small)).equals("1,5,10,15,30,60"), String.join(",", PhotoInterval.choices(small)));
        check("receiver: every choice of a 2 GB TV is accepted there", PhotoInterval.refuse("30s", big) == null && PhotoInterval.refuse("1", big) == null
                && PhotoInterval.refuse("60", big) == null, String.valueOf(PhotoInterval.refuse("30s", big)));
        String why = PhotoInterval.refuse("30s", small);
        check("receiver: 30s on a smaller TV is refused, saying the memory and the shortest", why != null && why.startsWith("error: ") && why.contains("907 MB")
                && why.contains("1 (minute)"), String.valueOf(why));
        check("receiver: 1 minute is fine on a small TV", PhotoInterval.refuse("1", small) == null, "");
        check("receiver: anything else is refused (0.5, 30, s, 2, 120, empty)", PhotoInterval.refuse("0.5", big) != null && PhotoInterval.refuse("30S", big) != null
                && PhotoInterval.refuse("2", big) != null && PhotoInterval.refuse("120", big) != null && PhotoInterval.refuse("", big) != null, "");
        check("stored: nothing or something unknown is 30 minutes, as always", PhotoInterval.read(null, big).equals("30") && PhotoInterval.read("7", big).equals("30"), "");
        check("stored: 30s is 30s on a 2 GB TV and 1 minute on a smaller one", PhotoInterval.read("30s", big).equals("30s") && PhotoInterval.read("30s", small).equals("1"), "");
        check("stored: minutes stay what they are", PhotoInterval.read("5", small).equals("5") && PhotoInterval.read("60", big).equals("60"), "");
        check("fragment: the minutes as before, 0.5 for 30 seconds", PhotoInterval.fragmentValue("30").equals("30") && PhotoInterval.fragmentValue("1").equals("1")
                && PhotoInterval.fragmentValue("30s").equals("0.5"), "");
        check("the row's words", PhotoInterval.label("30s").equals("٣٠ ثانية") && PhotoInterval.label("1").equals("دقيقة واحدة")
                && PhotoInterval.label("5").equals("٥ دقائق") && PhotoInterval.label("10").equals("١٠ دقائق") && PhotoInterval.label("15").equals("١٥ دقيقة")
                && PhotoInterval.label("60").equals("٦٠ دقيقة"), PhotoInterval.label("15"));
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
