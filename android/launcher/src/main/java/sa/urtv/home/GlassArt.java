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
 * Adapted from tvOS Launcher (SceneHome.createSmoothSquircleDrawable, CustomMedia.sampleDominantEdgeColor, renderEditedAppIcon and
 * trimTransparentPadding). Changed by UrTV: rewritten in Java for the UrTV home; one render for the glass bar and the icon editor
 * (shape, background, zoom and move in one place); the shapes are UrTV's three (square, rounded, circle); every call made off the main
 * thread (a weak TV); no image of tvOS Launcher is used. THIRD_PARTY_NOTICES.md has the notice.
 */
package sa.urtv.home;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * 4.5 «زجاجي»: an app's icon as the glass bar draws it, a smooth squircle with the icon filling it, and the icon editor's own render of a
 * changed icon (the person's image, a background colour, a shape, a zoom and a move). Bitmaps only, never a view: called off the main thread.
 */
final class GlassArt {
    private GlassArt() {}

    /** the three shapes of «تغيير الأيقونة»: «مربع», «مربع بحواف دائرية» (the glass bar's own), «دائرة» */
    static final String SQUARE = "square", ROUNDED = "rounded", CIRCLE = "circle";

    /** the corner over the side for a shape (the rounded one is FavBar.GLASS_CORNER) */
    static float corner(String shape) {
        if (CIRCLE.equals(shape)) return 0.5f;
        if (SQUARE.equals(shape)) return 0.06f;   // a hint of a corner, as a TV draws a square
        return FavBar.GLASS_CORNER;
    }

    /** the drawable as a bitmap of its own size (at most {@code max} on a side), for the sampling and the shader */
    static Bitmap bitmapOf(Drawable d, int max) {
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b.getWidth() <= max && b.getHeight() <= max) return b;
        }
        int w = d.getIntrinsicWidth() > 0 ? Math.min(max, d.getIntrinsicWidth()) : max, h = d.getIntrinsicHeight() > 0 ? Math.min(max, d.getIntrinsicHeight()) : max;
        Bitmap b = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        android.graphics.Rect was = d.copyBounds();
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        d.setBounds(was);
        return b;
    }

    /**
     * The colour of an icon's edge (tvOS Launcher's sampleDominantEdgeColor): the average of its opaque pixels on a ring 8 % in from the edge,
     * every 16th of a side; an icon with no opaque edge is a dark grey. The glass bar fills a transparent icon's corners with it, and «تلقائي»
     * of the editor's colours is this.
     */
    static int edgeColor(Bitmap b) {
        if (b == null || b.getWidth() < 4 || b.getHeight() < 4) return 0xFF2C2C2E;
        int w = b.getWidth(), h = b.getHeight(), ix = Math.max(1, Math.round(w * 0.08f)), iy = Math.max(1, Math.round(h * 0.08f));
        int stepX = Math.max(1, w / 16), stepY = Math.max(1, h / 16);
        long r = 0, g = 0, bl = 0, n = 0;
        for (int x = ix; x < w - ix; x += stepX) {
            for (int y : new int[] {iy, h - 1 - iy}) {
                int p = b.getPixel(x, y);
                if (Color.alpha(p) < 200) continue;
                r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++;
            }
        }
        for (int y = iy; y < h - iy; y += stepY) {
            for (int x : new int[] {ix, w - 1 - ix}) {
                int p = b.getPixel(x, y);
                if (Color.alpha(p) < 200) continue;
                r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++;
            }
        }
        return n == 0 ? 0xFF2C2C2E : Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
    }

    /** the icon without the transparent margin round it (tvOS Launcher's trimTransparentPadding), so it fills the shape; the same bitmap when there is none */
    static Bitmap trim(Bitmap b) {
        if (b == null) return null;
        int w = b.getWidth(), h = b.getHeight(), l = w, t = h, r = -1, btm = -1;
        int step = Math.max(1, Math.min(w, h) / 64);
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if (Color.alpha(b.getPixel(x, y)) < 24) continue;
                if (x < l) l = x;
                if (x > r) r = x;
                if (y < t) t = y;
                if (y > btm) btm = y;
            }
        }
        if (r < 0) return b;
        l = Math.max(0, l - step); t = Math.max(0, t - step); r = Math.min(w - 1, r + step); btm = Math.min(h - 1, btm + step);
        if (l == 0 && t == 0 && r == w - 1 && btm == h - 1) return b;
        return Bitmap.createBitmap(b, l, t, r - l + 1, btm - t + 1);
    }

    /**
     * The icon in its shape, {@code size} square (tvOS Launcher's createSmoothSquircleDrawable and renderEditedAppIcon): the background colour
     * (0: the icon's edge colour), then the icon drawn through a shader in the same rounded outline, filling it ({@code fill}: cropped to fill;
     * else whole inside with a margin), zoomed by {@code zoom} about its centre and moved by {@code dx}, {@code dy} (a part of the size, -0.5..0.5).
     */
    static Bitmap render(Bitmap icon, int size, int background, String shape, boolean fill, float zoom, float dx, float dy) {
        size = Math.max(8, size);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float rad = size * corner(shape);
        RectF box = new RectF(0, 0, size, size);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(background != 0 ? background : edgeColor(icon));
        c.drawRoundRect(box, rad, rad, bg);
        if (icon == null) return out;
        float iw = icon.getWidth(), ih = icon.getHeight();
        float base = fill ? Math.max(size / iw, size / ih) : Math.min(size / iw, size / ih) * 0.78f;
        float s = base * Math.max(0.5f, Math.min(3f, zoom));
        Matrix m = new Matrix();
        m.setScale(s, s);
        m.postTranslate((size - iw * s) / 2f + dx * size, (size - ih * s) / 2f + dy * size);
        BitmapShader sh = new BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        sh.setLocalMatrix(m);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setShader(sh);
        if (fill) c.drawRoundRect(box, rad, rad, p);   // the shader's clamp would smear the edge pixels: only a filling icon is drawn to the outline
        else {
            RectF at = new RectF(0, 0, iw, ih);
            m.mapRect(at);
            at.intersect(box);
            c.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(box, rad, rad, android.graphics.Path.Direction.CW);
            c.clipPath(clip);
            c.drawRect(at, p);
            c.restore();
        }
        return out;
    }

    /** the glass bar's icon of an installed app: its launcher icon, trimmed, whole on its edge colour in the rounded squircle */
    static Bitmap appIcon(Drawable icon, int size) {
        Bitmap b = trim(bitmapOf(icon, Math.max(size, 192)));
        boolean opaque = b != null && Color.alpha(b.getPixel(b.getWidth() / 2, 0)) > 200 && Color.alpha(b.getPixel(0, b.getHeight() / 2)) > 200;
        return render(b, size, 0, ROUNDED, opaque, 1f, 0, 0);   // an icon already square to its edges fills the shape; one with a margin of its own sits on its colour
    }
}
