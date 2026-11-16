package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/** tests/seasonal_test.py: Seasonal on a plain JVM */
public class SeasonalRun {
    static String pack(String id, String from, String to, String lic) {
        return "{\"id\":\"" + id + "\",\"name\":{\"ar\":\"رمضان\",\"en\":\"Ramadan\"},\"source\":{\"ar\":\"أنسبلاش\",\"en\":\"Unsplash\"},\"from\":\"" + from + "\",\"to\":\"" + to + "\","
            + "\"url\":\"https://github.com/tshi-dev/UrTV/releases/download/photos-" + id + "/" + id + ".zip\",\"sha256\":\"" + "a".repeat(64) + "\",\"count\":30,\"bytes\":9000000"
            + (lic == null ? "" : ",\"license\":{\"name\":\"" + lic + "\"}") + "}";
    }

    static String ids(List<PhotoPacks.Pack> v) {
        StringBuilder b = new StringBuilder();
        for (PhotoPacks.Pack p : v) b.append(b.length() == 0 ? "" : ",").append(p.id);
        return b.toString();
    }

    public static void main(String[] a) {
        Object doc = MiniJson.parse("{\"v\":1,\"packs\":[" + pack("ramadan", "2027-02-08", "2027-03-09", "Unsplash License") + ","
            + pack("national-day", "2027-09-20", "2027-09-26", "Unsplash License") + "," + pack("nolicense", "2027-02-08", "2027-03-09", null) + ","
            + pack("Bad Id", "2027-02-08", "2027-03-09", "CC0-1.0") + "]}");
        System.out.println("RAMADAN " + ids(Seasonal.packs(doc, "2027-02-20")));
        System.out.println("FIRSTDAY " + ids(Seasonal.packs(doc, "2027-02-08")) + " LASTDAY " + ids(Seasonal.packs(doc, "2027-03-09")));
        System.out.println("AFTER [" + ids(Seasonal.packs(doc, "2027-03-10")) + "]");
        System.out.println("ALL " + ids(Seasonal.packs(doc, null)));
        System.out.println("NODOC [" + ids(Seasonal.packs(null, "2027-02-20")) + "]");
        List<PhotoPacks.Pack> index = new ArrayList<>();
        for (String id : new String[] {"space", "sea", "ramadan"})
            index.add(new PhotoPacks.Pack(id, "x", "x", "s", "s", "", PhotoPacks.RELEASES + "photos-" + id + "/" + id + ".zip", "b".repeat(64), 10, 1000));
        System.out.println("MERGE " + ids(Seasonal.merge(Seasonal.packs(doc, "2027-02-20"), index)));
        List<PhotoPacks.Pack> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) many.add(new PhotoPacks.Pack("p" + i, "x", "x", "s", "s", "", PhotoPacks.RELEASES + "photos-p" + i + "/p" + i + ".zip", "b".repeat(64), 10, 1000));
        System.out.println("CAP " + Seasonal.merge(Seasonal.packs(doc, "2027-02-20"), many).size() + " " + Seasonal.merge(Seasonal.packs(doc, "2027-02-20"), many).get(0).id);
    }
}
