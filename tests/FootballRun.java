package sa.urtv.home;

/** tests/football_test.py: the football home's and «المباريات»'s pages as HomeCardChoice makes them. */
public class FootballRun {
    public static void main(String[] a) {
        System.out.println("home " + HomeCardChoice.footballUrl(new String[] {"city=Riyadh&panel=verse&teams=136013", "verse,hourly"}));
        System.out.println("homeEn " + HomeCardChoice.footballUrl(new String[] {"city=Riyadh&lang=en", "prayers"}));
        System.out.println("homeNone " + HomeCardChoice.footballUrl(null));
        System.out.println("page " + HomeCardChoice.matchesUrl("city=Riyadh&panel=verse&teams=136013", "2573409"));
        System.out.println("pageBad " + HomeCardChoice.matchesUrl("city=Riyadh", "1&x=2"));
        System.out.println("pageNull " + HomeCardChoice.matchesUrl(null, null));
    }
}
