package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4.3.3 Seasonal photo packs (Ramadan, Eid, National Day; urtv-data seasonal/seasonal.json, docs/SCHEMAS.md there): packs like the others
 * (the same release zip, sha256 and checks: PhotoPacks.valid), offered in the picker only between their two days and before the others. A
 * pack the person already downloaded stays theirs after its days. Their pictures are licensed ones only: urtv-data's check refuses a pack
 * without a license that allows it. Plain Java on a JVM (tests/SeasonalRun.java).
 */
final class Seasonal {
    private Seasonal() { }

    static final String PATH = "seasonal/seasonal.json", NAME = "seasonal.json";

    /** the packs of {@code doc} whose days hold {@code day} (YYYY-MM-DD), or every pack when {@code day} is null (their names); invalid ones dropped */
    static List<PhotoPacks.Pack> packs(Object doc, String day) {
        List<PhotoPacks.Pack> out = new ArrayList<>();
        if (!(doc instanceof Map)) return out;
        Object list = ((Map<?, ?>) doc).get("packs");
        if (!(list instanceof List)) return out;
        for (Object o : (List<?>) list) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o;
            String from = str(p.get("from")), to = str(p.get("to"));
            if (day != null && (from.isEmpty() || to.isEmpty() || day.compareTo(from) < 0 || day.compareTo(to) > 0)) continue;
            Map<?, ?> name = map(p.get("name")), source = map(p.get("source")), lic = map(p.get("license"));
            if (str(lic.get("name")).isEmpty()) continue;
            PhotoPacks.Pack k = new PhotoPacks.Pack(str(p.get("id")), str(name.get("ar")), str(name.get("en")), str(source.get("ar")), str(source.get("en")),
                str(p.get("cover")), str(p.get("url")), str(p.get("sha256")), num(p.get("count")), num(p.get("bytes")));
            if (PhotoPacks.valid(k)) out.add(k);
        }
        return out;
    }

    /** the seasonal packs of today first, then the index's, each id once, at most PACKS_MAX (PhotoPacks.keepValid) */
    static List<PhotoPacks.Pack> merge(List<PhotoPacks.Pack> seasonal, List<PhotoPacks.Pack> index) {
        List<PhotoPacks.Pack> all = new ArrayList<>(seasonal);
        all.addAll(index);
        return PhotoPacks.keepValid(all);
    }

    private static String str(Object o) { return o instanceof String ? (String) o : ""; }
    private static Map<?, ?> map(Object o) { return o instanceof Map ? (Map<?, ?>) o : java.util.Collections.emptyMap(); }
    private static int num(Object o) { return o instanceof Double && (Double) o == Math.rint((Double) o) && (Double) o < Integer.MAX_VALUE ? ((Double) o).intValue() : 0; }
}
