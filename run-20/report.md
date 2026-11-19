| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |
|---|---|---|---|---|---|---|
| 01-preset-tv-to-couch | 2.337 | 2.803 | 2 | 0 | 0 | android.widget.FrameLayout [1136,788][1440,958] → android.widget.LinearLayout [172,72][362,132] |
| 02-preset-couch-to-tv | 2.096 | 2.096 | 1 | 1 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 03-bg-photos-to-scenes | 1.184 | 1.184 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 04-bg-scenes-to-photos | 0.402 | 3.038 | 13 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 05-apps-tiles-to-banners | 1.284 | 1.284 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 06-apps-banners-to-icons | 0.112 | 1.245 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 07-apps-icons-to-tiles | 1.352 | 1.352 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 08-clock-plain-to-glass | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 09-clock-glass-to-plain | None | None | None | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 10-layout-classic-to-football | 2.144 | 2.448 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.webkit.WebView [0,0][1920,832] |
| 11-layout-football-to-classic | 1.808 | 1.808 | 1 | 0 | 0 | android.webkit.WebView [0,0][1920,832] → android.widget.LinearLayout [172,72][362,132] |
| 20-back-after-couch | 0.108 | 0.108 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 21-back-after-tv | 0.091 | 0.091 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 22-back-after-apps-banners | 0.1 | 0.1 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 23-back-after-apps-tiles | 0.097 | 0.097 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 30-settings-preset-couch | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 31-settings-preset-tv | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 40-cold-after-couch | 0.265 | 2.081 | 5 | 5 | 0 |  →  |
| 41-cold-after-tv | 0.261 | 1.833 | 4 | 5 | 0 |  →  |
| 50-panel-open | 0.358 | 0.673 | 3 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 51-panel-preview-couch | None | None | None | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 52-panel-back-saved | 0.222 | 0.323 | 2 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 53-panel-ok-couch | 0.15 | 0.55 | 4 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 54-panel-close | 0.092 | 0.092 | 1 | 0 | 0 |  → android.widget.LinearLayout [1264,223][1840,377] |
| 55-panel-undo | 0.105 | 0.582 | 2 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |

| budget | cases over it |
|---|---|
| no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted) | 02-preset-couch-to-tv |
| a change from outside shows within 3 s (it waited for the minute: 4 to 52 s) | 08-clock-plain-to-glass, 09-clock-glass-to-plain |
| after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV) | none |
