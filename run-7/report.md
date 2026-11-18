| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |
|---|---|---|---|---|---|---|
| 01-preset-tv-to-couch | 1.764 | 1.764 | 1 | 0 | 0 | android.widget.FrameLayout [1136,792][1440,962] → android.widget.LinearLayout [172,72][362,132] |
| 02-preset-couch-to-tv | 33.318 | 36.022 | 15 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 03-bg-photos-to-scenes | 18.894 | 18.963 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 04-bg-scenes-to-photos | 4.757 | 7.265 | 15 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 05-apps-tiles-to-banners | 50.696 | 50.771 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 06-apps-banners-to-icons | 36.567 | 36.645 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 07-apps-icons-to-tiles | 22.269 | 22.314 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |

| budget | cases over it |
|---|---|
| no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted) | none |
| a change from outside shows within 3 s (it waited for the minute: 4 to 52 s) | 02-preset-couch-to-tv, 03-bg-photos-to-scenes, 04-bg-scenes-to-photos, 05-apps-tiles-to-banners, 06-apps-banners-to-icons, 07-apps-icons-to-tiles |
| after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV) | none |
