/*
 * Copyright 2026 techgenius (tvOS Launcher, github.com/techgenius1/tvOS-Launcher)
 * Copyright 2026 UrTV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * The idea is tvOS Launcher's: glass made from the picture behind it shrunk and blurred once, never a live blur, so a weak TV draws it for
 * nothing. Changed by UrTV: the picture is UrTV's own backdrop (GlassSource, the home's photo at 1/8, blurred once off the main thread when
 * the photo changes); this drawable only draws the part of it behind the bar, with a tint and a 1 px edge. THIRD_PARTY_NOTICES.md has the notice.
 */
package sa.urtv.home;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * 4.5 «زجاجي»: the bar's liquid glass. The backdrop's piece behind the bar (by the bar's place on the screen), a tint, and a 1 px edge of one
 * colour (the owner: no gradient); with no backdrop yet (or on a weak TV, flat), the tint alone. No blur here: tests/glass_test.py looks for a live one.
 */
final class GlassBar extends Drawable {
    private Bitmap backdrop;
    private int screenX, screenY, screenW = 1, screenH = 1;
    private float radius;
    private boolean light;
    private final Paint photo = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF box = new RectF();
    private final Rect src = new Rect();

    /** the backdrop (null: none yet), set on the main thread once it was read off it */
    void backdrop(Bitmap b) { backdrop = b; invalidateSelf(); }

    /** where the bounds' (0, 0) is on the screen, and the screen's size: the backdrop is the whole screen */
    void place(int x, int y, int w, int h) {
        if (x == screenX && y == screenY && w == screenW && h == screenH) return;
        screenX = x; screenY = y; screenW = Math.max(1, w); screenH = Math.max(1, h);
        invalidateSelf();
    }

    void radius(float r) { radius = r; invalidateSelf(); }

    private boolean flat;

    /** 4.4 beta 2: a weak TV («الوضع الخفيف»): a flat fill of 16 % and the edge, no picture behind */
    void flat(boolean f) { flat = f; invalidateSelf(); }

    /** a light look (the tint white and stronger, as on a pale photo), or a dark one */
    void light(boolean l) { light = l; invalidateSelf(); }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        box.set(b);
        clip.reset();
        clip.addRoundRect(box, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        if (backdrop != null && !flat) {
            float sx = backdrop.getWidth() / (float) screenW, sy = backdrop.getHeight() / (float) screenH;
            src.set(Math.round((screenX + b.left) * sx), Math.round((screenY + b.top) * sy), Math.round((screenX + b.right) * sx), Math.round((screenY + b.bottom) * sy));
            src.intersect(0, 0, backdrop.getWidth(), backdrop.getHeight());
            if (!src.isEmpty()) c.drawBitmap(backdrop, src, b, photo);
        }
        tint.setColor(flat ? (light ? 0xB8FFFFFF : 0x29FFFFFF) : light ? (backdrop != null ? 0x8CFFFFFF : 0xB8FFFFFF) : (backdrop != null ? 0x33FFFFFF : 0x40FFFFFF));
        c.drawRect(b, tint);
        c.restore();
        float w = Math.max(1f, b.height() / 90f);   // the edge: 1.5 px at a bar 135 high
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(w);
        edge.setColor(0x57FFFFFF);   // the owner: one 1 px edge, no gradient
        box.inset(w / 2f, w / 2f);
        c.drawRoundRect(box, Math.max(0, radius - w / 2f), Math.max(0, radius - w / 2f), edge);
    }

    @Override public void setAlpha(int a) { photo.setAlpha(a); tint.setAlpha(a); edge.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
