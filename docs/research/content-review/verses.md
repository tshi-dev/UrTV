# Content review: assets/shared/verses.json (500 verses)

Checked 2026-11-27 against api.alquran.cloud (editions `quran-simple`, `quran-uthmani`, `ar.muyassar`), whole Quran fetched once.

| Check | Method | Result |
|---|---|---|
| `text` | code point for code point against `quran-simple` (Tanzil), same surah:ayah | 500 / 500 identical |
| `uthmani` | letter skeleton (harakat, waqf and orthographic marks removed, alif/hamza forms folded) against `quran-uthmani` | 500 / 500 identical (12 first ayahs: the source carries the basmala inside ayah 1, the file does not; correct) |
| `uthmani` exact marks | needs the QPC Hafs text of api.quran.com (`.github/tools/verify_uthmani.py`, CI) | not reachable from this container (proxy refused): **not verified here** |
| `surah` name | against the source's surah name | 500 / 500 identical |
| `ayah` / `ref` | ayah field equals the ref; every ref exists | 500 / 500 |
| `page` | Madani page of the source | 500 / 500 identical |
| `tafsir` | against `ar.muyassar` (التفسير الميسر), whole | 500 / 500 identical |
| duplicates | same ref twice | none |

## Findings

None. No change proposed.

Not verified: the exact QPC Hafs marks of `uthmani` (CI job `quran-verify` covers them).
