package sa.urtv.screensaver;

/** tests/own_place_test.py: OwnPlace.near on given places, one line each: "lat,lon -> zone country" or "-> none" */
public final class OwnPlaceRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] p = a.split(",", 2);
            String[] n = OwnPlace.near(p[0], p.length > 1 ? p[1] : "");
            System.out.println(a + " -> " + (n == null ? "none" : n[0] + " " + n[1]));
        }
    }
}
