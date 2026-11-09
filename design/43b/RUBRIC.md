# Design rubric — 10-foot UI (4.3 round 2b)

Benchmarks: Apple tvOS (material, depth, parallax focus), Google TV and Netflix/Apple TV+ (hero + shelves), Samsung Ambient / Frame mode (the TV as art),
Nothing OS (type as the interface, dot matrix), Teenage Engineering (playful precision), high-end Arabic editorial and calligraphic type.

Every screen is scored 0–10 on each line; the screen's score is the **lowest** of the seven, not the average. Nothing goes out below 8. Scores are in `SCORES.md`.

| # | Line | 10 looks like | 5 looks like |
|---|------|---------------|--------------|
| 1 | **Hierarchy** | one thing is the hero (clock, title or photo), a second tier is clearly secondary, the rest recedes; the eye lands in under a second | everything the same size and weight; boxes of equal rank |
| 2 | **Depth and material** | the look has a *material* (frosted glass, paper, enamel, metal, light, ink) with a consistent light direction, shadow and edge; layers read front to back | flat translucent rectangles with a hairline |
| 3 | **Type scale and Arabic typography** | a real scale (hero 160+, title 64–96, body 28–32, caption ≥ 22 at 1080p), Arabic set with its own rhythm: generous line height, no letter-spacing, numerals chosen on purpose, headings with a distinct voice from the body | one size jumps, Latin habits (tracking, tiny caps labels) in Arabic |
| 4 | **Colour harmony** | one colour logic per look (a palette with a rule: duotone, one accent on a neutral, a day-phase ramp), accent used for meaning only, contrast ≥ 4.5:1 for every word | a rainbow of unrelated accents, grey text on a busy photo |
| 5 | **Focus and motion** | focus is unmistakable at 3 m, and *behaves* (lift + glow, parallax, ink fill, scale) with an easing and a duration we state; the path between two focuses is shown where it matters | a ring or a colour change only |
| 6 | **Restraint** | everything on the screen earns its place; empty space is designed (safe area 96/54, 8-scale spacing, ≥ 16 px inside boxes); no pills, no eyebrow labels, no decoration without a job | filler panels, badges, tiny helper text |
| 7 | **Legibility at 3 m** | nothing under 22 px at 1080p on the home, 28 px for what is read while moving; photo never fights a word (a plate, a gradient or a duotone sits between) | small text over detail |

A **look** is a complete system, so each screen of one states five things: **typography** (clock and headings), **card material**, **focus behaviour**,
**colour logic**, **how the wallpaper is treated** (full-bleed, blur plate, duotone, framed, dithered).

Method: three divergent directions per look at thumbnail size (`studies/`) → the strongest chosen → full-res → scored → self-critique → iterated (at least twice,
the log is in `SCORES.md`). Photos are the app's own packs (`photos/`, credits in `photos/CREDITS.md`).
