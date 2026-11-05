# Photo packs

Settings › شاشة التوقف › «صور شاشة التوقف». A pack is a set of 1920×1080 photos on one theme. It is downloaded when the person chooses it,
stays on the TV, and its photos join the screensaver's rotation. «صورنا» (our own set) stays and is on by default; it can be turned off
only while a pack that is on is on the TV.

## What the app does with a pack

1. Reads the index, `photos/packs.json` on `master` (the last good copy is kept for offline).
2. On «نزّل»: downloads the pack's zip, **checks its sha256 against the index before opening it**, then unpacks it into a work folder and
   moves it into place whole. A wrong digest, a bad name inside the zip, a file that is not a JPEG that decodes, a gap in 1..count, or a
   zip not from our releases: nothing is kept (`PhotoPacks`, `PackStore`; `tests/photo_packs_test.py`).
3. Several packs can be on; the photos rotate across all of them and «صورنا».
4. «حذف» frees the space. The credits of every photo are in «حقوق الصور» on the same screen.

## The index: `photos/packs.json`

```json
{
 "version": 1,
 "packs": [
  {
   "id": "space",
   "name": "الفضاء",
   "name_en": "Space",
   "source": "ناسا، ملكية عامة",
   "source_en": "NASA, public domain",
   "cover": "https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/covers/space.jpg",
   "url": "https://github.com/tshi-dev/UrTV/releases/download/photos-space/space.zip",
   "sha256": "<64 lower-case hex digits of the zip>",
   "count": 8,
   "bytes": 5242880
  }
 ]
}
```

| Field | Rule |
|---|---|
| `id` | `[a-z][a-z0-9-]{1,30}`: a folder name and part of the address |
| `name`, `name_en` | the card's title, at most 40 characters |
| `source`, `source_en` | the card's last line, at most 80 |
| `cover` | an `https` address of a 480×270 JPEG, or empty |
| `url` | exactly `https://github.com/tshi-dev/UrTV/releases/download/photos-<id>/<id>.zip`; any other address is not followed |
| `sha256` | of the zip |
| `count` | photos in it, 1 to 200 |
| `bytes` | the zip's size, at most 80 MB |

An entry that breaks a rule is left out of the list, never half shown. At most 12 packs, each id once.

## The zip

`1.jpg` … `N.jpg` (1920×1080, each under 3 MB) and `credits.json`; nothing else (a path or any other name makes the app refuse the pack).

```json
{"pack": "space", "photos": [{"file": "1.jpg", "title": "...", "author": "NASA/JPL", "licence": "Public domain", "link": "https://..."}]}
```

## Content rule

Written down here because it was not before (docs/PROPOSALS-NOV2.md):

- only CC0 and public domain are taken as they are; CC BY and CC BY-SA (2.0 to 4.0, never NC or ND) only for a pack the owner allowed
  (`"allow_attribution": true`; today «مدن السعودية» only), each photo with its `licence_url` and `changes` (`make_pack.py` refuses it
  otherwise), which the credit line in «حقوق الصور» shows after the licence;
- no people (no faces or figures as the subject);
- no religious buildings other than mosques shown as architecture, no statues or idols, no nudity; for artworks, landscapes, still lifes,
  calligraphy and patterns only;
- no logos, flags or insignia, and nothing by a third party inside a public-domain image;
- a person looks at every photo of a pack once before it is published, the way `docs/CONTENT-REVIEW.md` does for the verses.

`.github/tools/make_pack.py` enforces what a script can (licence allow-list, words in a title or note, size) and refuses a pack until the
manifest says `"reviewed": true`.

## Making a pack (the maintainer)

```
python3 .github/tools/make_pack.py nasa --query nebula --query "mars surface" --limit 12 --dir work/space     # candidates, optional
# look at every photo in work/space, delete any with people, a logo or a third party's ©, edit manifest.json, then "reviewed": true
python3 .github/tools/make_pack.py build --id space --name الفضاء --name-en Space \
    --source "ناسا، ملكية عامة" --source-en "NASA, public domain" --src work/space \
    --index photos/packs.json --covers photos/covers
```

It writes `build/packs/space.zip`, the cover, and the pack's line in `photos/packs.json` with the sha256. Then create the release
`photos-space` on tshi-dev/UrTV with that zip as its asset, add the photos to `docs/PHOTO-CREDITS.md`, and commit the index and the cover.
The same photos always give the same zip.

## Where things are

| | |
|---|---|
| Rules (no Android) | `android/app/src/main/java/sa/urtv/screensaver/PhotoPacks.java` |
| Index, download, unpack, delete | `PackStore.java` |
| The screen | `PhotoPacksActivity.java` |
| What the page asks the app for | `https://packs.urtv.invalid/<id>/<N>.jpg` and `.../credits.json` (`PhotoCache`), `#packs=id:count,…` and `#ours=0` |
