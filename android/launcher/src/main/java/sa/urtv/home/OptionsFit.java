package sa.urtv.home;

/**
 * 4.3.4: the app's options box (AppOptions) was exactly the app's width, and its rows are one line each: on a small tile, or with the TV's
 * font made bigger, «صفحته في المتجر», «إلغاء التثبيت» and «افتح القفل» were cut off. The box is now as wide as its widest row needs (never
 * narrower than the app), at most the screen less its margins, and kept on the screen. Pixels in, pixels out; no Android in here
 * (tests/options_fit_test.py runs it).
 */
final class OptionsFit {
    private OptionsFit() { }

    /** the box's width: the app's, or what its widest row needs when that is more; never past the screen less a margin each side */
    static int width(int cardW, int widestRow, int screenW, int margin) {
        int max = Math.max(1, screenW - 2 * margin);
        return Math.max(1, Math.min(Math.max(cardW, widestRow), max));
    }

    /** the box's left edge: centred on the app, moved in when it would leave the screen */
    static int left(int centreX, int w, int screenW, int margin) {
        int x = centreX - w / 2;
        return Math.max(margin, Math.min(x, screenW - margin - w));
    }

    /** a row's height: the grid's button, or the line of text with its room when the TV's font is bigger */
    static int rowHeight(int buttonH, int lineH, int room) {
        return Math.max(buttonH, lineH + room);
    }
}
