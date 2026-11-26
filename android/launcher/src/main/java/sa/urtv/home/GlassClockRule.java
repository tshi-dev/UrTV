package sa.urtv.home;

/**
 * 4.4 beta 4 (the owner, typed): «الساعة: زجاجية» is drawn as the big glass clock, in the middle, whatever the cards do. Two ways the cards sit:
 * - «كنبة» (the strip under the clock, `inRow`): the clock stays in the middle ALWAYS and the strip is under it (couch-tv); no small clock at the top right,
 *   and the clock never moves between card states.
 * - the other layouts (the box beside the clock): a card that can cover the clock's lower half makes the header's regular clock take its place while it
 *   shows (the owner's sweep 2, option 2), the glass clock again without cards.
 * No Android in here: tests/glass_clock_rule_test.py runs it.
 */
final class GlassClockRule {
    private GlassClockRule() { }

    static boolean glass(String homeClock, boolean cardsShown, boolean inRow) {
        return "glass".equals(homeClock) && (!cardsShown || inRow);
    }
}
