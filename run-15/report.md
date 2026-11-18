| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |
|---|---|---|---|---|---|---|
| 01-preset-tv-to-couch | 2.81 | 3.466 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 02-preset-couch-to-tv | 2.141 | 3.401 | 4 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 03-bg-photos-to-scenes | 1.257 | 1.257 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 04-bg-scenes-to-photos | 0.567 | 3.328 | 13 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 05-apps-tiles-to-banners | 0.134 | 1.242 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 06-apps-banners-to-icons | 1.206 | 1.206 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 07-apps-icons-to-tiles | 1.27 | 1.27 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 08-clock-plain-to-glass | 0.088 | 1.226 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 09-clock-glass-to-plain | 0.101 | 1.25 | 2 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.widget.LinearLayout [172,72][362,132] |
| 10-layout-classic-to-football | 0.27 | 0.27 | 1 | 0 | 0 | android.widget.LinearLayout [172,72][362,132] → android.webkit.WebView [0,0][1920,832] |
| 11-layout-football-to-classic | 0.203 | 1.014 | 2 | 0 | 0 | android.webkit.WebView [0,0][1920,832] → android.widget.LinearLayout [172,72][362,132] |
| 20-back-after-couch | 0.11 | 0.11 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 21-back-after-tv | 0.099 | 0.099 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 22-back-after-apps-banners | 0.106 | 0.106 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 23-back-after-apps-tiles | 0.085 | 0.085 | 1 | 0 | 0 |  → android.widget.LinearLayout [172,72][362,132] |
| 30-settings-preset-couch | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 31-settings-preset-tv | None | None | None | 0 | 0 | android.widget.LinearLayout [80,219][1211,297] → android.widget.LinearLayout [80,219][1211,297] |
| 40-cold-after-couch | 0.289 | 2.015 | 5 | 4 | 0 |  →  |
| 41-cold-after-tv | 0.165 | 1.844 | 4 | 3 | 0 |  →  |
| 50-panel-open | 0.356 | 0.669 | 2 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 51-panel-preview-couch | None | None | None | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |
| 52-panel-back-saved | 0.206 | 0.296 | 2 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 53-panel-ok-couch | 0.155 | 0.478 | 3 | 0 | 0 |  → android.widget.LinearLayout [80,219][1211,297] |
| 54-panel-close | 0.09 | 0.09 | 1 | 0 | 0 |  → android.widget.LinearLayout [1264,223][1840,377] |
| 55-panel-undo | 0.123 | 0.611 | 4 | 0 | 0 |  → android:id/select_dialog_listview android.widget.ListView [432,498][1488,706] |

| budget | cases over it |
|---|---|
| no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted) | none |
| a change from outside shows within 3 s (it waited for the minute: 4 to 52 s) | none |
| after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV) | none |
