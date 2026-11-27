package sa.urtv.home;

import java.io.File;

/**
 * 4.3.3 «مسح الملفات المؤقتة» (الإعدادات ← حول والتحديث): what a sweep may delete, and nothing else. The app's cache folder and the image and data caches
 * named by the caller, never the settings, the person's own photos, the downloaded packs, the adhan's sound files, or an app file waiting to
 * be installed (*.apk). Plain Java (tests/CacheSweepRun.java runs it on real folders). The same class is in both apps.
 */
public final class CacheSweep {
    private CacheSweep() { }

    /** never deleted, wherever they are: the adhan's sounds (the launcher's cache/adhan) and an installer's APK on its way */
    static boolean kept(File f) {
        String n = f.getName();
        return n.endsWith(".apk") || (f.isDirectory() && n.equals("adhan"));
    }

    /** the bytes a sweep of these folders would free */
    public static long size(File... roots) {
        long n = 0;
        for (File r : roots) n += sizeIn(r);
        return n;
    }

    private static long sizeIn(File d) {
        File[] in = d == null ? null : d.listFiles();
        if (in == null) return 0;
        long n = 0;
        for (File f : in) {
            if (kept(f)) continue;
            n += f.isDirectory() ? sizeIn(f) : f.length();
        }
        return n;
    }

    /** deletes what is inside each folder (the folders themselves stay); returns the bytes freed */
    public static long clear(File... roots) {
        long n = 0;
        for (File r : roots) n += clearIn(r);
        return n;
    }

    private static long clearIn(File d) {
        File[] in = d == null ? null : d.listFiles();
        if (in == null) return 0;
        long n = 0;
        for (File f : in) {
            if (kept(f)) continue;
            if (f.isDirectory()) { n += clearIn(f); f.delete(); }
            else { long len = f.length(); if (f.delete()) n += len; }
        }
        return n;
    }
}
