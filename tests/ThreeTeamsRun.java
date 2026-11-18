package sa.urtv.home;

/** tests/three_teams_test.py: three followed teams through the launcher's link to «المباريات». */
public class ThreeTeamsRun {
    public static void main(String[] a) {
        System.out.println("page " + HomeCardChoice.matchesUrl("city=Riyadh&lat=24.7&lon=46.6&tz=Asia/Riyadh&teams=136013,136014,136015&events=&leagues=&panel=verse", null));
        System.out.println("event " + HomeCardChoice.matchesUrl("city=Riyadh&teams=136013,136014,136015&panel=verse", "11"));
    }
}
