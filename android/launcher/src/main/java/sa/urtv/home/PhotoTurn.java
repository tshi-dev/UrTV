package sa.urtv.home;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;

/** A decoded photo turned the way its camera said (the EXIF orientation: PhotoSize.orient has the rules). Never throws: a photo with no tag, or a bad one, is left as it is. */
final class PhotoTurn {
    private PhotoTurn() {}

    static Bitmap upright(File f, Bitmap b) {
        if (b == null) return null;
        try {
            int[] o = PhotoSize.orient(new ExifInterface(f.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1));
            if (o[0] == 0 && o[1] == 0) return b;
            Matrix m = new Matrix();
            if (o[1] == 1) m.postScale(-1, 1);
            if (o[0] != 0) m.postRotate(o[0]);
            Bitmap t = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (t != b) b.recycle();
            return t;
        } catch (Exception | OutOfMemoryError e) {
            return b;
        }
    }
}
