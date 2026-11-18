| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |
|---|---|---|---|---|---|---|
| 01-preset-tv-to-couch | 8.189 | 8.189 | 1 | 0 | 0 | android.widget.FrameLayout [1136,792][1440,962] → android.widget.LinearLayout [172,72][362,132] |
| 02-preset-couch-to-tv | 49.957 | 53.403 | 13 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 03-bg-photos-to-scenes | 35.292 | 35.425 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 04-bg-scenes-to-photos | 20.953 | 24.688 | 15 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 05-apps-tiles-to-banners | 6.702 | 6.768 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 06-apps-banners-to-icons | 52.441 | 52.484 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 07-apps-icons-to-tiles | 37.971 | 38.071 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 08-clock-plain-to-glass | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 09-clock-glass-to-plain | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 10-layout-classic-to-football | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 11-layout-football-to-classic | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 20-back-after-couch | 0.084 | 0.084 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 21-back-after-tv | 0.107 | 0.107 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 22-back-after-apps-banners | 0.091 | 0.091 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 23-back-after-apps-tiles | 0.097 | 0.097 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 30-settings-preset-couch | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 31-settings-preset-tv | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 40-cold-after-couch | 0.028 | 1.924 | 6 | 4 | 0 |  →  |
| 41-cold-after-tv | 0.166 | 1.663 | 5 | 3 | 0 |  →  |
| 50-panel-open | 0.388 | 0.807 | 2 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 51-panel-preview-couch | None | None | None | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 52-panel-back-saved | 0.232 | 0.305 | 2 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 53-panel-ok-couch | 0.161 | 0.675 | 3 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 54-panel-close | 0.076 | 0.076 | 1 | 0 | 0 |  → android.widget.LinearLayout [1264,221][1840,301] |
| 55-panel-undo | 0.153 | 0.389 | 2 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |

| budget | cases over it |
|---|---|
| no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted) | none |
| a change from outside shows within 3 s (it waited for the minute: 4 to 52 s) | 01-preset-tv-to-couch, 02-preset-couch-to-tv, 03-bg-photos-to-scenes, 04-bg-scenes-to-photos, 05-apps-tiles-to-banners, 06-apps-banners-to-icons, 07-apps-icons-to-tiles, 08-clock-plain-to-glass, 09-clock-glass-to-plain |
| after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV) | none |
