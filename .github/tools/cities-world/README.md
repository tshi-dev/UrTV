# Worldwide cities (draft data, not yet in the app)

- `build.py`: GeoNames (the geonamescache package) → `cities-world.json` (1,379 cities with an Arabic name from GeoNames). The order: Arab countries first, then by number of Muslims (approximate, for ordering only).
- `check-result.json`: per-region model check: name fixes, added names, and the calculation method for each country.
- `names-result.json`: Arabic names for the 1,958 cities that had none (a small model, with a light check).
- `merge.py` → `cities-merged.json`: 3,305 cities, each with `name_ar` + `name_en`, `lat`/`lon`/`tz`/`population`/`capital`/`order` and `method`.
- The names of small cities are by-pronunciation spellings from a model: they need review (see the task in CODEX-TASK.md).
