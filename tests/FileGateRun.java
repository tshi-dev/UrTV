package sa.urtv.home;

public final class FileGateRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        String page = "/data/user/0/sa.urtv.home/files/web/tv_20261101.html";
        check("the page itself is allowed", FileGate.allowed(page, page));
        check("the app's assets are allowed (fonts, photos)", FileGate.allowed("/android_asset/fonts/Thmanyah-Regular.woff2", page) && FileGate.allowed("/android_asset/web/tv.html", "/android_asset/web/tv.html"));
        check("the app's folders are not: shared_prefs, files, caches, databases", !FileGate.allowed("/data/user/0/sa.urtv.home/shared_prefs/portfolio.xml", page) && !FileGate.allowed("/data/user/0/sa.urtv.home/files/photos/x.jpg", page)
            && !FileGate.allowed("/data/data/sa.urtv.home/databases/x", page) && !FileGate.allowed("/sdcard/Download/x", page));
        check("no way round: «..», an encoded «..», an asset path with nothing in it, nothing, a NUL", !FileGate.allowed("/android_asset/../shared_prefs/x.xml", page) && !FileGate.allowed("/android_asset/%2e%2e/x", page)
            && !FileGate.allowed("/android_asset/", page) && !FileGate.allowed("", page) && !FileGate.allowed(null, page) && !FileGate.allowed("/android_asset/a\0b", page));
        check("with no page given only the assets", FileGate.allowed("/android_asset/a.js", null) && !FileGate.allowed(page, null));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
