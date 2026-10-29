# Worldwide cities

The reviewed list is integrated into the app. The following files describe the original inputs; the reproducible review and integration workflow is below.

- `build.py`: GeoNames (the geonamescache package) → `cities-world.json` (1,379 cities with an Arabic name from GeoNames). The order: Arab countries first, then by number of Muslims (approximate, for ordering only).
- `check-result.json`: per-region model check: name fixes, added names, and the calculation method for each country.
- `names-result.json`: Arabic names for the 1,958 cities that had none (a small model, with a light check).
- `merge.py` → `cities-merged.json`: originally 3,305 cities; after review, 3,307 cities, each with `name_ar` + `name_en`, `lat`/`lon`/`tz`/`population`/`capital`/`order` and `method`.
- Many original small-city names were model transliterations. Their review and remaining uncertainties are recorded in `REVIEW.md`.

## Reviewed app data

- `REVIEW.md` records the full Arabic-name review, country counts, 119 corrections and two capital additions.
- `overrides.json` is the only place for reviewed spelling changes; never edit generated JSON or Java.
- `requirements.txt` pins GeoNames cache and Babel so regeneration is stable.
- `sources/capitals-geonames.tsv` preserves the credited GeoNames record for Palau's capital (below the cache cutoff).
- `merge.py` works from any current directory, restores country names from CLDR, recognizes capital aliases, and recomputes country order.
- The original 374 cities in the 14 established countries are unchanged. `gen_cities.py` appends packed world regions using `Cities-support.java.txt`; chunks stay below Java's string/initializer limits.

Regenerate from the repository root:

```sh
pip install -r .github/tools/cities-world/requirements.txt
python3 .github/tools/cities-world/merge.py
python3 .github/tools/gen_cities.py "$(python3 -c 'import geonamescache,pathlib; print(pathlib.Path(geonamescache.__file__).parent / "data/cities500.json")')"
python3 .github/tools/cities_md.py
python3 tests/cities_world_test.py
```

The apps send the country-resolved calculation id in the existing `pm` settings fragment; the page carries no per-country table. `tests/prayer_method_registry.json` snapshots Aladhan's new-method parameters; `tests/prayer_crosscheck_world.json` preserves 60 published responses, tested independently on the launcher and the offline browser page. No network is needed for these tests.
