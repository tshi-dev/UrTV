# Scores against RUBRIC.md (self-assessment, stand-in fonts)

Line order: hierarchy · depth/material · type · colour · focus/motion · restraint · legibility. The screen's score is its **lowest** line. Nothing under 8 went into `preview/`.

| Screen | H | D | T | C | F | R | L | **Score** | Weakest line and why |
|---|---|---|---|---|---|---|---|---|---|
| 01 لوح (tvOS-like glass) | 8 | 9 | 8 | 8 | 9 | 8 | 8 | **8** | type: Arabic at weight 200 is only as good as the font's thin cut; check with Thmanyah |
| 02 مجلس (paper, brass, arch) | 9 | 9 | 9 | 8 | 8 | 8 | 8 | **8** | the calligraphic clock needs a real display face; DejaVu/Naskh stand in |
| 03 سينما (hero + shelves) | 9 | 8 | 8 | 8 | 8 | 9 | 8 | **8** | the dock icons at 55 % opacity are the least legible part |
| 04 إطار (art on the wall) | 9 | 9 | 8 | 9 | 8 | 9 | 8 | **8** | no focus on this screen by design; the only motion is the slow hand-over of the photo (shown in the notes) |
| 05 يوم (sky dial, Maghrib) | 8 | 8 | 8 | 9 | 8 | 8 | 8 | **8** | the dial's labels are the densest text; 26 px |
| 06 يوم، من الفجر إلى العشاء | 8 | 8 | 8 | 9 | 8 | 8 | 8 | **8** | the Dhuhr (light) frame's glass is the flattest |
| 07 نقاط (dot matrix) | 9 | 8 | 8 | 9 | 8 | 9 | 8 | **8** | the clock uses Latin digits (the dot font is 0–9); an Arabic-Indic dot set is a design task of its own |
| 08 ألوان (playful blocks) | 8 | 8 | 8 | 8 | 9 | 8 | 8 | **8** | the bento is busy; the hard shadow is what keeps it ordered |
| 09 focus strip (لوح) | 8 | 9 | 8 | 8 | 9 | 8 | 8 | **8** | |
| 10 games hub | 9 | 8 | 9 | 9 | 8 | 8 | 8 | **8** | the confetti must stay at this density at most |
| 11 quiz question | 9 | 8 | 9 | 9 | 9 | 8 | 9 | **8** | team bar sits close to the answers |
| 12 quiz reveal | 9 | 9 | 9 | 9 | 9 | 8 | 8 | **8** | |
| 13 score animation | 8 | 8 | 8 | 8 | 9 | 8 | 8 | **8** | frames are crops, the real thing is 600 ms of motion |
| 14 memory pairs | 8 | 8 | 8 | 9 | 8 | 9 | 8 | **8** | |
| 15 word game | 8 | 8 | 9 | 9 | 8 | 8 | 8 | **8** | the letter keyboard is dense (28 keys); a 3-row picker is the alternative |

## Self-critique log
**Pass 1 → 2** (found by looking at the first renders):
- 02: the wood shelf had not drawn (a template string written with quotes); the arch and the shelf are now one system. 01: the focused label wrapped onto the tile; it is one line now.
- 03: the shelf ran off the left edge (five cards too wide); cards are 300/372 px now and the progress bars read in the right direction.
- 04: the frame looked like a rectangle on black; added the picture light, a wall grain, a glass glare and an inner shadow on the photo.
- 05/06: all four phases said «العصر» next; each phase now has its own time and next prayer. The sun's glow hid its own label; labels near the sun move out.
- 08: the photo's caption was white on white; the bento had an empty cell, so it has eight tiles now.
- Games: the hub's first card collided with the subtitle; the team bar overlapped the answers; the word game's keyboard had one orphan key (now 7 × 4).

**Pass 2 → 3** (second look at full size): dial moved up 40 px so the next-prayer line clears the arc's feet; the sun label offset; the studies page shows the chosen direction large and the two alternatives beside it.

Not done on purpose: no real Arabic calligraphic clock (needs a licensed calligraphic face; `majlis` marks where it goes), no animation files (frames and timings are given instead).
