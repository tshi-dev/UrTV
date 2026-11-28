package sa.urtv.home;

/**
 * 4.4 beta 4 (a user: the «كل التطبيقات» grid was cut at the top): the grid's height within its box (the 90 % safe area's panel, under the title and
 * the search) is a whole number of rows. A grid of any height showed the row above the focused one cut in half once it scrolled (it scrolls a
 * row at a time with the keys); with whole rows each step moves exactly one row and every row on the screen is whole. The rest of the box stays
 * below the grid. No Android in here (tests/GridFitRun.java).
 */
final class GridFit {
    private GridFit() { }

    /** the grid's height for `room` px of box: whole rows of `cell` px with `gap` between them, and its own padding above and below; at least one row */
    static int height(int room, int cell, int gap, int padTop, int padBottom) {
        if (cell <= 0 || room <= 0) return room;
        int inner = room - padTop - padBottom;
        int rows = Math.max(1, (inner + gap) / (cell + gap));
        return Math.min(room, rows * cell + (rows - 1) * gap + padTop + padBottom);
    }
}
