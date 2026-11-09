package sa.urtv.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * P8: a hosted photo is made screen sized when it arrives. The photos on the host are bigger than any TV screen (they were kept as they came, then decoded whole by the WebView and the
 * home, every time one was shown); a copy at most {@link #MAX} px on its long side, saved once, is what every later show reads. A photo already that small is kept as it is.
 */
final class PhotoShrink {
    private PhotoShrink() { }

    static final int MAX = 1920, QUALITY = 88;

    /** the photo's bytes, or smaller ones (a JPEG at most MAX px on the long side) when it was larger; the bytes as given when it cannot be read or is already small */
    static byte[] toScreen(byte[] data) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int w = o.outWidth, h = o.outHeight, longSide = Math.max(w, h);
            if (w <= 0 || h <= 0 || longSide <= MAX) return data;
            int sample = 1;
            while (longSide / (sample * 2) >= MAX) sample *= 2;   // decode no larger than twice the target
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            Bitmap big = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (big == null) return data;
            float k = MAX / (float) Math.max(big.getWidth(), big.getHeight());
            Bitmap small = k < 1f ? Bitmap.createScaledBitmap(big, Math.round(big.getWidth() * k), Math.round(big.getHeight() * k), true) : big;
            if (small != big) big.recycle();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, data.length / 3));
            boolean ok = small.compress(Bitmap.CompressFormat.JPEG, QUALITY, out);
            small.recycle();
            return ok && out.size() > 0 && out.size() < data.length ? out.toByteArray() : data;
        } catch (RuntimeException | OutOfMemoryError e) {
            return data;
        }
    }
}
