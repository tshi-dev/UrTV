# The tafsir card, short (4.4, pictures only: built after the owner approves)

The owner: «make it short; the book name and pages is okay». No tafsir is written or shortened by anyone: the text is a recognised
book's, word for word. The book is the short one the card already uses, «التفسير الميسر» (نخبة من العلماء، مجمع الملك فهد لطباعة
المصحف الشريف). What changes: its name moves under the text, small, with the mushaf page of the verse:
«التفسير الميسر، صفحة ٣٤١». The text is shown whole; only one still too long for the card pages (the existing ١/٣ under it).

- `tafsir-short.png`: الإخلاص ٣, the shortest tafsir in our 500 verses (30 letters), page 604.
- `tafsir-medium.png`: التكاثر ٨, the middle one (194 letters), page 600.
- `tafsir-long.png`: الحج ٧٧, the longest (968 letters: the book explains ٧٧ and ٧٨ together), page 341; three pages on the card.

The page numbers are the Madinah mushaf's, from the same source as the verses (api.alquran.cloud, its `page` for each ayah), not
typed by hand; the build would store them with each verse (`.github/tools/fetch_verses.py`). The renders use the page as it is now with
a small change on top; the photo is one of ours; the font is a stand-in for Thmanyah (a private font, not in the repository).

**Source and terms.** The text: `assets/shared/verses.json` (fetched by `.github/tools/fetch_verses.py` from api.alquran.cloud, edition
`ar.muyassar`), shipped as `dist/tafsir.json`. QuranEnc.com publishes the same tafsir on these terms: re-publication allowed with no
modification, addition or deletion, the publisher and source named (THIRD_PARTY_NOTICES.md, «التفسير الميسر»). The card meets them: the
text whole and unchanged, the book named under it, and «مصادر البيانات» in the settings names the publisher and the source.
The second choice, «المختصر في تفسير القرآن الكريم» (مركز تفسير), is not needed while the first is in use; QuranEnc's own list could not
be checked from the build machine (its network does not reach quranenc.com), so whether and on what terms it carries it is still open.
