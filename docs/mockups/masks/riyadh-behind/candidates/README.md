# Riyadh night: three Unsplash candidates

The owner picks one. Until then the bundled night photo stays.

All three are under the [Unsplash License](https://unsplash.com/license), which allows bundling in an app.

The owner's rule: the clock has the same place in every picture, so the photo is framed instead. The clock is the fixed
clean box (digits 266 to 542, 718 wide, centred). Each photo is framed so that the Kingdom Centre stands behind the clock:
- `vAkHAP27QMk`: frame `14,112,1372` (zoom 1.4). The tower is small, so the frame centres it and puts its top above the
  digits.
- `_UxsPARhtOg` and `ZiN8NGVd2pE`: the whole photo. The tower already stands at the centre.

Each went through the full mask pipeline at that frame: the cut-out, alignment, halo, and `owner_ok`. The gate gave all three
FIT as `owner_ok`. Under rule v1 (at most a quarter of a digit covered, never the colon), no frame passes for any of them,
because a tower behind the clock covers whole digits. So they keep their depth only as the owner's own picture, like the
current one.

| Photo | Photographer | Light | Source |
|---|---|---|---|
| `vAkHAP27QMk` | سيف الظاهر | night; the Kingdom Centre is lit, small in the frame | https://unsplash.com/photos/a-city-at-night-vAkHAP27QMk (4032×3024) |
| `_UxsPARhtOg` | Ibrahim Abdullah | dusk, orange sky | https://unsplash.com/photos/kingdom-centre-skyscraper-in-riyadh-_UxsPARhtOg (4971×3314) |
| `ZiN8NGVd2pE` | Oualid Lakhrouti | late golden light, blue sky | https://unsplash.com/photos/modern-skyscraper-against-a-clear-blue-sky-ZiN8NGVd2pE (5200×4100) |

Files:
- `<id>-1230.jpg` and `<id>-458.jpg`: the 01 night layout at 12:30 and 4:58, through the frame.
- `<id>-arch-100.jpg`: a 100% crop (480×360) of the arch from the 1920 frame.
- `contact-sheet.jpg`: all three, with the credits.

Clipped pixels in the arch crop (any channel 253 or more): 0.0%, 0.4% and 1.2%. None of them has the flat blue of the
current photo.

Only `vAkHAP27QMk` is a true night shot; at its frame the buildings beside the tower hide most of the digits. The `_UxsPARhtOg` id starts with an underscore, so the scene id would need a new
name.
