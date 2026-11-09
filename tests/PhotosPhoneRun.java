package sa.urtv.screensaver;

/** For tests/photos_phone_test.py: JpegMeta on made-up JPEG headers. One line per check: name TAB ok|FAIL. */
public final class PhotosPhoneRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static byte[] b(int... v) { byte[] o = new byte[v.length]; for (int i = 0; i < v.length; i++) o[i] = (byte) v[i]; return o; }

    public static void main(String[] a) {
        // SOI, APP0 JFIF (16 bytes), then the scan
        byte[] plain = b(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0xFF, 0xDA, 0x00, 0x02, 0xFF, 0xD9);
        byte[] exif = b(0xFF, 0xD8, 0xFF, 0xE1, 0x00, 0x0A, 'E', 'x', 'i', 'f', 0, 0, 'M', 'M', 0xFF, 0xDA, 0x00, 0x02, 0xFF, 0xD9);
        byte[] exifLater = b(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x04, 0, 0, 0xFF, 0xE1, 0x00, 0x0A, 'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 0xFF, 0xDA, 0x00, 0x02);
        byte[] xmp = b(0xFF, 0xD8, 0xFF, 0xE1, 0x00, 0x08, 'h', 't', 't', 'p', 0, 0, 0xFF, 0xDA, 0x00, 0x02);
        check("a JPEG as the phone page makes it (JFIF, no Exif): no camera data", !JpegMeta.cameraData(plain));
        check("a camera's JPEG (an APP1 «Exif» segment first): camera data", JpegMeta.cameraData(exif));
        check("…also after another segment", JpegMeta.cameraData(exifLater));
        check("an APP1 that is not Exif (XMP) is not taken for it", !JpegMeta.cameraData(xmp));
        check("not a JPEG, empty, null, a broken length: no answer of camera data (other checks refuse them)",
            !JpegMeta.cameraData(b(1, 2, 3, 4)) && !JpegMeta.cameraData(new byte[0]) && !JpegMeta.cameraData(null) && !JpegMeta.cameraData(b(0xFF, 0xD8, 0xFF, 0xE1, 0, 0)));
    }
}
