package sa.urtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.widget.LinearLayout;

/**
 * The favourites' row. A plain horizontal row (every way, as before) until «زجاجي» gives it a bar: then it draws the glass (GlassBar) round
 * its icons only, centred, and lays them out in 1 to 3 rows (FavBar.glassCellAt: row by row, the first at the start side). Its children
 * stay its children in the same order, so the home's lists of tiles, ◀ ▶ and the edit modes are the same in every way.
 */
final class DockGrid extends LinearLayout {
    private GlassBar glass;
    private int rows = 1, cell, gap, pad;
    private final int[] at = new int[2];

    DockGrid(Context c) { super(c); }

    /** «زجاجي»: the bar and its grid (null: the plain row of the other ways) */
    void glass(GlassBar g, int rows, int cell, int gap, int pad) {
        glass = g;
        this.rows = g == null ? 1 : Math.max(1, rows);
        this.cell = cell;
        this.gap = gap;
        this.pad = pad;
        setWillNotDraw(g == null);
        requestLayout();
        invalidate();
    }

    GlassBar glass() { return glass; }

    /** the rows the bar asks for (1 with no bar) */
    int rows() { return rows; }

    private boolean grid() { return glass != null && rows > 1 && getChildCount() > 1; }

    @Override protected void onMeasure(int w, int h) {
        if (!grid()) { super.onMeasure(w, h); return; }
        int n = getChildCount(), cols = FavBar.glassCols(n, rows), used = FavBar.glassRowsUsed(n, rows);
        int childH = 0;
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            c.measure(MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            childH = Math.max(childH, c.getMeasuredHeight());
        }
        int contentW = cols * cell + (cols - 1) * gap + getPaddingLeft() + getPaddingRight();
        int contentH = used * childH + (used - 1) * gap + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(contentW, w), resolveSize(contentH, h));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!grid()) { super.onLayout(changed, l, t, r, b); return; }
        int n = getChildCount(), cols = FavBar.glassCols(n, rows), used = FavBar.glassRowsUsed(n, rows);
        int childH = n > 0 ? getChildAt(0).getMeasuredHeight() : 0;
        int gridW = cols * cell + (cols - 1) * gap, gridH = used * childH + (used - 1) * gap;
        int x0 = (r - l - gridW) / 2, y0 = (b - t - gridH) / 2;   // centred both ways
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (int i = 0; i < n; i++) {
            int[] rc = FavBar.glassCellAt(i, n, rows);
            int col = rtl ? cols - 1 - rc[1] : rc[1];
            int x = x0 + col * (cell + gap), y = y0 + rc[0] * (childH + gap);
            getChildAt(i).layout(x, y, x + cell, y + childH);
        }
    }

    @Override protected void dispatchDraw(Canvas c) {
        if (glass != null && getChildCount() > 0) {
            int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (v.getVisibility() == GONE) continue;
                left = Math.min(left, v.getLeft()); top = Math.min(top, v.getTop()); right = Math.max(right, v.getRight()); bottom = Math.max(bottom, v.getBottom());
            }
            if (left < right) {
                glass.setBounds(left - pad, top - pad, right + pad, bottom + pad);
                getLocationOnScreen(at);
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                glass.place(at[0], at[1], dm.widthPixels, dm.heightPixels);
                glass.draw(c);
            }
        }
        super.dispatchDraw(c);
    }
}
