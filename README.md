# urtv-data

Open data for UrTV (tshi-dev/UrTV): football (fixtures, results, live window, teams, head-to-head, seasons, the league catalogue), the live
Quran stations' check, the games' word lists and quiz bank, market prices, and the photo packs' metadata. Updated automatically by the
workflows in `.github/workflows`.

- **`main`** (this branch): the generators (`.github/tools`, copied from UrTV unchanged), the publisher (`tools/`), the workflows and the docs.
  Small history, never rewritten.
- **`data`**: the files the apps read, one parentless commit replaced by every writer (`docs/LAYOUT.md` says why), with `index.json` at its
  top listing every dataset with its `v`, `updated` and `sha256`.

| doc | what |
|---|---|
| [docs/INVENTORY.md](docs/INVENTORY.md) | every dataset: its writer, schedule, readers in the apps, size, and where it lived in UrTV |
| [docs/LAYOUT.md](docs/LAYOUT.md) | the folders, the data branch and why, how writers never race, versions |
| [docs/SCHEMAS.md](docs/SCHEMAS.md) | the form of every file |
| [docs/SERVING.md](docs/SERVING.md) | raw.githubusercontent.com and jsDelivr addresses, and which data must not go through the CDN |

Rules: data grows additively; every dataset has a `v` (in `index.json`, and in the file itself for forms made here); a breaking change is a new
file name, so the apps already on TVs keep working. No personal data, no keys.
