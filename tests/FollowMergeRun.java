package sa.urtv.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** tests/follow_store_test.py: the home's own follows joined to the page's settings (FollowMerge), on a plain JVM. */
public class FollowMergeRun {
    static List<String> l(String... a) { return new ArrayList<>(Arrays.asList(a)); }

    public static void main(String[] a) {
        List<String> none = l();
        System.out.println("nothing " + FollowMerge.merge(null, none, none));
        System.out.println("asIs " + FollowMerge.merge("city=X&teams=1,2&leagues=English%20Premier%20League&panel=verse", none, none));
        System.out.println("noSaver " + FollowMerge.merge(null, l("136013|Al-Hilal", "136014|Al-Nassr", "136015|Al-Ittihad"), l("4337|الدوري الهولندي|Dutch Eredivisie|Dutch Eredivisie", "4396||English League 1|id4396")));
        System.out.println("joined " + FollowMerge.merge("city=X&teams=136013,9&leagues=English%20Premier%20League,id4719&lgn=4719~%D8%A2%D8%B3%D9%8A%D8%A7&panel=verse",
            l("136013|Al-Hilal", "136015|Al-Ittihad"), l("4328|الدوري الإنجليزي|English Premier League|English Premier League", "4719|آسيا|AFC|id4719", "5000|كأس|Cup|id5000")));
        List<String> twenty = l(); for (int i = 1; i <= 20; i++) twenty.add(String.valueOf(i));
        System.out.println("fullTeams " + FollowMerge.merge("teams=" + String.join(",", twenty), l("999|X"), none));
        System.out.println("add " + FollowMerge.add(l("1|a"), FollowMerge.team("1", "b"), 20) + " " + FollowMerge.add(l("1|a"), FollowMerge.team("2", "b;c|d"), 20) + " " + FollowMerge.add(l("1|a"), FollowMerge.team("x", "b"), 20)
            + " " + FollowMerge.add(l("1|a"), FollowMerge.team("3", "c"), 1));
        System.out.println("league " + FollowMerge.league("4337", "هولندا", "Dutch Eredivisie", "Dutch Eredivisie") + " " + FollowMerge.league("4337", "a", "b", "x&y=z") + " " + FollowMerge.league("43a", "a", "b", null));
        System.out.println("remove " + FollowMerge.remove(l("1|a", "2|b"), "1"));
    }
}
