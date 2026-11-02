package sa.urtv.home.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Draws a QrCode black on white with the four-module quiet zone around it, as large as the view allows (a square). */
public final class QrView extends View {
    private QrCode code;
    private final Paint paint = new Paint();

    public QrView(Context c) {
        super(c);
        paint.setAntiAlias(false);
    }

    public void setCode(QrCode q) {
        code = q;
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) {
        int side = Math.min(MeasureSpec.getSize(w), MeasureSpec.getSize(h) > 0 && MeasureSpec.getMode(h) != MeasureSpec.UNSPECIFIED ? MeasureSpec.getSize(h) : MeasureSpec.getSize(w));
        setMeasuredDimension(side, side);
    }

    @Override protected void onDraw(Canvas canvas) {
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        if (code == null) return;
        int n = code.size + 8;   // four modules of white on each side
        int side = Math.min(getWidth(), getHeight());
        // whole pixels per module when they fit (sharp edges, every module the same size: easier for a phone's camera), centred in the view
        float cell = side >= n ? (float) Math.floor(side / (float) n) : side / (float) n;
        float x0 = (getWidth() - cell * n) / 2f + 4 * cell, y0 = (getHeight() - cell * n) / 2f + 4 * cell;
        paint.setColor(0xFF000000);
        for (int y = 0; y < code.size; y++) {
            for (int x = 0; x < code.size; x++) {
                if (code.dark(x, y)) canvas.drawRect(x0 + x * cell, y0 + y * cell, x0 + (x + 1) * cell, y0 + (y + 1) * cell, paint);
            }
        }
    }
}
