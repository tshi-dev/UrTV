# Serving

Two addresses serve every file of the `data` branch:

| | address | fresh |
|---|---|---|
| GitHub raw | `https://raw.githubusercontent.com/tshi-dev/urtv-data/data/<path>` | about 5 minutes (GitHub's own cache) |
| jsDelivr | `https://cdn.jsdelivr.net/gh/tshi-dev/urtv-data@data/<path>` | up to 12 hours for a branch (and up to 7 days at some edges) unless purged |

jsDelivr caches a branch's files for hours, so data that changes during the day must **bypass the CDN** and be read from GitHub raw:

| dataset | changes | CDN |
|---|---|---|
| `markets/` | every 5 minutes while an exchange is open | **no** |
| `football/archive/` | every 15 minutes while a game is live | **no** |
| `football/leagues/` | every 3 to 6 hours, scores of the day | **no** |
| `football/catalogue/` | weekly | yes |
| `games/`, `games/quiz/` | when the games data changes | yes |
| `stations/` | weekly | yes |
| `photos/` | by hand | yes |

`index.json` says the same per dataset (`"cdn": true|false`). A pinned commit on jsDelivr (`@<sha>`) never changes and may be cached for
ever, but the data branch's commits are replaced and later collected, so the apps do not pin.

The apps (UrTV 4.3.3 on) read GitHub raw only, `https://raw.githubusercontent.com/tshi-dev/urtv-data/data/` first and the old UrTV branch
second (UrTV's `docs/DATA-HOST.md`, `DataHost.java` in both apps, `DATA_MAP` in the page). The old UrTV writers keep running for the apps
already on TVs until the maintainer retires them (at least three months).
