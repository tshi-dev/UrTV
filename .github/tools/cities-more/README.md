# The offline city search list

`build.py` writes `android/app/src/main/assets/cities-more.txt`, the places the screensaver app's city search («بحث عن مدينة أو قرية») and
`saver.sh city` find without the internet, beside the built-in list (`Cities.java`, which it never changes). The app reads it only on the first
search (`CitiesMore.java`, `CitySearch.java`); a place picked from it is saved like a searched place (name, coordinates, zone, country), so a
later version of the file never changes a saved city.

- Sources: GeoNames cities500 (CC BY 4.0, the pinned `geonamescache` of `../cities-world/requirements.txt`) and Wikidata (CC0): the Arabic
  label and aliases of each GeoNames id, and the seats of the 136 Saudi governorates. `fetch_wikidata.py` refreshes the snapshot in
  `sources/` (about one query a minute is allowed); `build.py` itself runs offline.
- Which places: `TIERS` in `build.py` (all of GeoNames for Saudi Arabia, the Gulf, Jordan, Lebanon, Syria, Yemen, Libya and Iraq; 5,000 people
  and more for Egypt, Sudan, the Maghreb and Palestine; 15,000 for Turkey and Europe; 30,000 for the US, Canada, Australia and New Zealand; 50,000 for
  Malaysia, Indonesia, Pakistan, India, Singapore and Brunei). Only places with an Arabic name; never Israel or Western Sahara as a country.
- Each line: GeoNames id (or the Wikidata item of a governorate seat GeoNames lacks), country, zone (index into the `@` line), latitude,
  longitude, population, Arabic, English, other spellings (`|`). Lines starting with `=` add the English name, the other spellings and the
  population to a built-in city, by its coordinates.
- `overrides.json` is the only hand-made input: `names` (GeoNames id → reviewed Arabic), `drop` (ids left out), `add`, `builtin`.
- `ids.lock`: every id ever shipped. `tests/cities_more_test.py` fails when one goes away: the data only grows.
- The calculation is the country's (`PrayerMethod`, `Cities.REGIONS`): Umm al-Qura for Saudi Arabia, each country's own elsewhere.

```sh
pip install -r .github/tools/cities-world/requirements.txt
python3 .github/tools/cities-more/build.py          # then: python3 tests/cities_more_test.py
```
