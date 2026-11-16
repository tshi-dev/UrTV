# Where the data files live (4.3.3)

The data CI makes (league files, the match archive, the league catalogue, the games' and the quiz's files, market prices, the stations'
check) is moving from this repository's `data-*` branches to the data repository **tshi-dev/urtv-data**: code and docs on its `main`, the files on its branch `data` (one commit, replaced by each writer). Its own docs
(`docs/INVENTORY.md`, `docs/SCHEMAS.md`, `docs/SERVING.md` there) list every dataset, its writer, its schedule and its form.

The apps and the page read the new place first and the old branch second, with one map, the same in three places:
`android/app/.../screensaver/DataHost.java`, `android/launcher/.../home/DataHost.java` and `DATA_MAP` in `src/template.html`
(`tests/data_host_test.py` keeps them equal).

| old branch and folder (UrTV)  | urtv-data (branch data) |
|-------------------------------|-------------------------|
| `data-leagues/leagues/`       | `football/leagues/`     |
| `data-catalogue/catalogue/`   | `football/catalogue/`   |
| `data-archive/`               | `football/archive/`     |
| `data-games/quiz/`            | `games/quiz/`           |
| `data-games/games/`           | `games/`                |
| `data-markets/markets/`       | `markets/`              |
| `data-stations/stations/`     | `stations/`             |

- A reader still names the old address; `DataHost.get` (the page: `fetchData`) asks the new one first and the old one when the new
  answers anything but 200 or cannot be reached. Until a file is published in urtv-data that costs one 404 and nothing else.
- Both are `raw.githubusercontent.com`, never the jsDelivr CDN: the market and live files change every few minutes and the CDN keeps a
  branch's file for hours.
- The old writers in this repository keep running unchanged: the apps already on TVs read only the old branches. They are retired only
  when the maintainer says so, at least three months after the apps that read urtv-data are out.
- A file moved to urtv-data keeps its form; a breaking change gets a new file name (old apps keep reading the old one).

## Hand-edited content (4.3.3)

Four small files of urtv-data's branch `data`, edited by hand in its `content/` and checked there before they are published. The apps keep
the last good copy in their files (`ContentFile`, both apps), read it at once and fetch it again in the background; no file or a bad one
means the app is as before.

| file | app | what it does | rules |
|---|---|---|---|
| `notices/announcements.json` | home (`NoticeRule`) | one short line in the home's top bar | only when the bar has nothing else to say; never while our tilawah or adhan plays, in kids mode or «UrTV النظيف»; each id once; no links; 140 characters |
| `devices/devices.json` | home (`DeviceRule`, `DeviceDefaults`) | a model's defaults at the home's start | only where nothing is set; today `sound_opens_general_settings` (the TCL G07) |
| `seasonal/seasonal.json` | screensaver (`Seasonal`, `PackStore`) | Ramadan, Eid, National Day photo packs | offered only between their days, first in the picker; the same zip checks as every pack; licensed only |
| `prayer/adjustments.json` | screensaver (`PrayerFix`, `Prefs`) | minute corrections by country or city and date | through the same `adj=` as «تعديل وقت الصلاة», so the page, the home and the adhan move together; a prayer the person moved keeps the person's minutes; the settings screen says when one applies; −20..20 |
