package sa.urtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** The calm progress bar of the update and setup screens: a track, and the share done in the accent colour from the reading side (right in Arabic, left in English). */
public final class CalmBar extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int track, fill;
    private double share;

    public CalmBar(Context c, int track, int fill) {
        super(c);
        this.track = track;
        this.fill = fill;
    }

    public void setShare(double s) {
        share = Math.max(0, Math.min(1, s));
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Math.round(8 * getResources().getDisplayMetrics().density)); }

    @Override protected void onDraw(Canvas c) {
        float rad = getHeight() / 2f;
        paint.setColor(track);
        c.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), rad, rad, paint);
        if (share <= 0) return;
        float w = Math.max((float) (getWidth() * share), getHeight());
        float left = Dir.rtl() ? getWidth() - w : 0;
        paint.setColor(fill);
        c.drawRoundRect(new RectF(left, 0, left + w, getHeight()), rad, rad, paint);
    }
}
