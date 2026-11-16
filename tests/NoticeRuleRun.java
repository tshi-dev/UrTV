package sa.urtv.home;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** tests/notice_test.py: NoticeRule on a plain JVM */
public class NoticeRuleRun {
    static String n(String id, String ar, String from, String to, String extra) {
        return "{\"id\":\"" + id + "\",\"text\":{\"ar\":\"" + ar + "\",\"en\":\"Hello\"},\"from\":\"" + from + "\",\"to\":\"" + to + "\"" + extra + "}";
    }

    static String id(NoticeRule.Notice x) { return x == null ? "none" : x.id + (x.dismissible ? "" : "!"); }

    public static void main(String[] a) {
        Object doc = MiniJson.parse("{\"v\":1,\"notices\":["
            + n("link", "زوروا https://x.example", "2027-01-01", "2027-12-31", "") + ","
            + n("domain", "زوروا urtv.sa الآن", "2027-01-01", "2027-12-31", "") + ","
            + n("eid", "عيدكم مبارك", "2027-03-09", "2027-03-12", ",\"min_app\":\"4.3.3\"") + ","
            + n("old-app", "حدّث", "2027-01-01", "2027-12-31", ",\"max_app\":\"4.3.2\"") + ","
            + n("fixed", "صيانة الليلة", "2027-01-01", "2027-12-31", ",\"dismissible\":false") + "]}");
        Set<String> none = Collections.emptySet();
        System.out.println("EID " + id(NoticeRule.pick(doc, "2027-03-10", "4.3.3", none, null)));
        System.out.println("ONE " + id(NoticeRule.pick(doc, "2027-03-10", "4.3.3", none, null)) + " ONLY");
        Set<String> seen = new HashSet<>(Collections.singleton("eid"));
        System.out.println("SEEN " + id(NoticeRule.pick(doc, "2027-03-10", "4.3.3", seen, null)));
        System.out.println("SAMEVISIT " + id(NoticeRule.pick(doc, "2027-03-10", "4.3.3", seen, "eid")));
        System.out.println("TOOOLD " + id(NoticeRule.pick(doc, "2027-03-10", "4.3.2", none, null)));
        System.out.println("TOONEW " + id(NoticeRule.pick(doc, "2027-05-01", "4.4", none, null)));
        System.out.println("BEFORE " + id(NoticeRule.pick(doc, "2026-12-31", "4.3.3", none, null)));
        System.out.println("NODOC " + id(NoticeRule.pick(null, "2027-03-10", "4.3.3", none, null)));
        String longAr = "ا".repeat(141);
        System.out.println("LONG " + id(NoticeRule.pick(MiniJson.parse("{\"v\":1,\"notices\":[" + n("long", longAr, "2027-01-01", "2027-12-31", "") + "]}"), "2027-03-10", "4.3.3", none, null)));
        System.out.println("CMP " + NoticeRule.compare("4.3.3", "4.3") + " " + NoticeRule.compare("4.3", "4.3.0") + " " + NoticeRule.compare("4.10", "4.9") + " " + NoticeRule.compare("4.3.2", "4.3.3"));
        NoticeRule.Notice e = NoticeRule.pick(doc, "2027-03-10", "4.3.3", none, null);
        System.out.println("TEXT " + e.text(false) + "|" + e.text(true));
    }
}
