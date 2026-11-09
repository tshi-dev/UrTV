package sa.urtv.screensaver;

/** 4.3 «صوري» from the phone: what a JPEG carries besides the picture. Plain Java (tests/photos_phone_test.py). */
final class JpegMeta {
    private JpegMeta() { }

    /**
     * 4.3: does the JPEG still carry its camera's data (an APP1 «Exif» segment: where and when it was taken)? The phone page re-encodes
     * every photo, which drops it; a file that still has it is refused, so no location ever reaches the TV from the phone.
     */
    static boolean cameraData(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 4 || (jpeg[0] & 0xff) != 0xFF || (jpeg[1] & 0xff) != 0xD8) return false;
        int i = 2;
        while (i + 4 <= jpeg.length && (jpeg[i] & 0xff) == 0xFF) {
            int marker = jpeg[i + 1] & 0xff;
            if (marker == 0xDA || marker == 0xD9) return false;   // the image itself: no more headers
            int len = ((jpeg[i + 2] & 0xff) << 8) | (jpeg[i + 3] & 0xff);
            if (len < 2) return false;
            if (marker == 0xE1 && i + 10 <= jpeg.length && jpeg[i + 4] == 'E' && jpeg[i + 5] == 'x' && jpeg[i + 6] == 'i' && jpeg[i + 7] == 'f') return true;
            i += 2 + len;
        }
        return false;
    }

}
