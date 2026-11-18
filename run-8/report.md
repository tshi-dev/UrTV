| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |
|---|---|---|---|---|---|---|
| 01-preset-tv-to-couch | 3.522 | 3.88 | 2 | 0 | 0 | android.widget.FrameLayout [1136,792][1440,962] → android.widget.LinearLayout [172,72][362,132] |
| 02-preset-couch-to-tv | 4.799 | 6.438 | 8 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 03-bg-photos-to-scenes | 1.21 | 1.21 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 04-bg-scenes-to-photos | 0.398 | 2.622 | 16 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 05-apps-tiles-to-banners | 0.095 | 1.207 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |

| budget | cases over it |
|---|---|
| no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted) | none |
| a change from outside shows within 3 s (it waited for the minute: 4 to 52 s) | 01-preset-tv-to-couch, 02-preset-couch-to-tv |
| after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV) | none |
