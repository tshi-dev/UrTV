#!/usr/bin/env python3
"""«كل التطبيقات» on the home screen (the owner's photo from the TCL: the title at the far right edge, the first app a step to its left,
the row looking shifted). The dock and the grid share one rule for a row's cells (MainActivity.rowCell: equal shares, 20 dp between
two, none at the row's ends, as start and end margins), and the title is inset like the grid. Checked in the source, then the rows are
laid out here as Android's LinearLayout lays them out, right to left as the home is and left to right too, at 1920 and 1280 wide:
the title's start, the first app and the first favourite line up, and the last tile ends inside the screen.   Run: python3 tests/home_grid_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

cell = main[main.index("private LinearLayout.LayoutParams rowCell(int i, int n, int height)"):].split("\n    }\n")[0]
check("one rule for a row's cells: start and end margins, 10 dp each side between two cells, none at the row's ends",
      "p.setMarginStart(i == 0 ? 0 : dp(10));" in cell and "p.setMarginEnd(i == n - 1 ? 0 : dp(10));" in cell and "setMargins(" not in cell)
check("the dock, the dock rebuilt in «تعديل المفضلة», the grid and the last row's padding all use it",
      "dockRow.addView(tile, rowCell(i, dock.size(), -2));" in main and "dockRow.addView(box, rowCell(i, order.size(), -2));" in main
      and "row.addView(tile, rowCell(i % cols, cols, -2));" in main and "row.addView(new View(this), rowCell(i, cols, 1));" in main)
check("no row cell is placed with left and right margins any more (they put the outer gap inside in RTL)",
      not re.search(r"setMargins\(i (%|==)", main), re.findall(r".*setMargins\(i.*", main))
check("«كل التطبيقات» is inset like the grid on both sides (68 dp), the grid 68 dp too",
      "content.addView(appsLabel, llp(-2, -2, dp(68), dp(8), dp(68), dp(18)));" in main and "content.addView(grid, llp(-1, -2, dp(68), 0, dp(68), dp(160)));" in main)
check("the dock band: 48 dp in from the screen and 20 dp of padding, so its first tile is 68 dp in too",
      "hero.addView(bottom, lp(-1, -2, Gravity.BOTTOM, dp(48), 0, dp(48), dp(30)));" in main and "dockRow.setPadding(dp(20), dp(22), dp(20), dp(22));" in main)
check("the home is right to left whatever the TV's language (the English TV too); «اللغة» English is left to right (Dir)", "root.setLayoutDirection(Dir.layout());" in main
      and main.count("setLayoutDirection(View.LAYOUT_DIRECTION_LTR)") == 0)
cols = int(re.search(r'<integer name="columns">(\d+)</integer>', (ROOT / "android/launcher/src/main/res/values/config.xml").read_text(encoding="utf-8")).group(1))

def lay(width, inset, n, margins, rtl):
    """LinearLayout, horizontal, every child width 0 and weight 1: the space less the margins in equal shares; returns [(left, right)]."""
    ms = [margins(i, n, rtl) for i in range(n)]   # (left, right) of each child, resolved
    share = (width - 2 * inset - sum(l + r for l, r in ms)) / n
    out, x = [], (width - inset if rtl else inset)
    for l, r in ms:
        if rtl:
            x -= r; out.append((x - share, x)); x -= share + l
        else:
            x += l; out.append((x, x + share)); x += share + r
    return out

def start_end(dp):   # rowCell: start/end, resolved by the direction
    def m(i, n, rtl):
        s, e = (0 if i == 0 else 10 * dp), (0 if i == n - 1 else 10 * dp)
        return (e, s) if rtl else (s, e)
    return m

def left_right(dp):  # the old way: left = (first ? 0 : 10), right = (last ? 0 : 10), whatever the direction
    return lambda i, n, rtl: (0 if i == 0 else 10 * dp, 0 if i == n - 1 else 10 * dp)

for width, dp in ((1920, 2.0), (1920, 1.5), (1280, 1.0), (1280, 4 / 3)):
    for rtl, lang in ((True, "Arabic"), (False, "English layout")):
        grid = lay(width, 68 * dp, cols, start_end(dp), rtl)
        dock = lay(width, (48 + 20) * dp, 6, start_end(dp), rtl)
        title_start = width - 68 * dp if rtl else 68 * dp
        first_g, first_d = (grid[0][1], dock[0][1]) if rtl else (grid[0][0], dock[0][0])
        last_g = grid[-1][0] if rtl else grid[-1][1]
        check(f"{width} px (×{dp:g}), {lang}: the first app starts under the start of «كل التطبيقات», and under the first favourite",
              abs(first_g - title_start) < 0.5 and abs(first_d - first_g) < 0.5, (first_g, title_start, first_d))
        check(f"{width} px (×{dp:g}), {lang}: the row is whole, its last tile ends 68 dp from the other edge (nothing cut)",
              abs((last_g if rtl else width - last_g) - 68 * dp) < 0.5 and all(r - l > 100 for l, r in grid), grid)
        gaps = [abs(grid[k][0] - grid[k + 1][1]) if rtl else abs(grid[k + 1][0] - grid[k][1]) for k in range(cols - 1)]
        check(f"{width} px (×{dp:g}), {lang}: the gaps between the apps are all 20 dp", all(abs(g - 20 * dp) < 0.5 for g in gaps), gaps)
old = lay(1920, 68 * 2, cols, left_right(2), True)
check("the test sees the photo's fault: with left and right margins the first app in RTL is a step inside the title's start",
      abs(old[0][1] - (1920 - 68 * 2)) >= 10 * 2 - 0.5, old[0])
sys.exit(1 if failed else 0)
