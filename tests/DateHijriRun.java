package sa.urtv.home;

/** For tests/hijri_line_test.py: DateNum's Gregorian-only line, one "name<TAB>got" line per check. */
public final class DateHijriRun {
    public static void main(String[] a) {
        System.out.println("off\t" + DateNum.hijriOff("city=x&nohijri=1&datenum=1") + " " + DateNum.hijriOff("nohijri=1") + " " + DateNum.hijriOff("city=x&datenum=1") + " " + DateNum.hijriOff(null) + " " + DateNum.hijriOff("nohijri=10"));
        System.out.println("line ar\t" + DateNum.lineGreg("الاثنين", "06/06/1448|16/11/2026", true));
        System.out.println("line en\t" + DateNum.lineGreg("Monday", "06/06/1448|16/11/2026", false));
        System.out.println("with hijri\t" + DateNum.line("Monday", "06/06/1448|16/11/2026", false));
        System.out.println("no parts\t" + DateNum.lineGreg("Monday", "x", false));
    }
}
