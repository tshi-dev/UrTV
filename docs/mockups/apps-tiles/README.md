# «شكل التطبيقات» and «كل التطبيقات»: the owner's test of build 525 (mockups, for his OK before building)

| Picture | What it shows |
|---|---|
| [01-tiles-1080.png](01-tiles-1080.png) | Today: the logo small in the middle of a dark, muddy tint of the app's colour. Proposed: the app's own TV banner filling the tile; with no banner, its icon large (56 % of the tile's height) on a light, neutral backing (#ECE9E3), never a tint. The focused tile grows 1.12 with a 4 px white ring and a shadow, its name under the row; 24 px between tiles, so the growth never covers a neighbour. |
| [02-tiles-720-large.png](02-tiles-720-large.png) | 720p with the large font: the same, scaled to the screen; the name in the large font. |
| [03-grid-1080.png](03-grid-1080.png) | «كل التطبيقات»: an even grid, 5 across; every row the same height (tile and name); the space between rows larger than the focus growth plus the ring, so the focused tile never covers the row below or its own name. The page moves a whole row at a time and the focused row is always whole on the screen, under the title with a safe margin, never cut at the top. |
| [04-grid-720-large.png](04-grid-720-large.png) | The grid at 720p with the large font: three whole rows on the screen. |

Logos are stand-ins (coloured shapes); on the TV each app's own banner or icon. The «tvOS» bar (glass session) keeps its own icons; its focus rule is the same: a clear ring, no overlap. HTML in `src/`, `node src/render.js`.
