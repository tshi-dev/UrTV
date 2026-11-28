package sa.urtv.screensaver;

import java.io.File;

/** For tests/photo_twin_test.py: PhotoTwin.find(args[0], args[1..]) printed (the kept photo's number, 0 for none). */
public final class PhotoTwinRun {
    public static void main(String[] args) {
        File[] kept = new File[args.length - 1];
        for (int i = 1; i < args.length; i++) kept[i - 1] = new File(args[i]);
        System.out.println(PhotoTwin.find(new File(args[0]), kept));
    }
}
