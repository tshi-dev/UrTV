# Proposals, 2 November

Research and mockups only. No app code was changed, and nothing here is built yet. The owner picks first. The mockups use
the lighter look the owner approved (`notes/ux-review` on `work/ux-review`): sand paper, white cards, hairline borders,
a green `#1B8354` ring for focus, and the Sadu triangles for «on». Each is 1920×1080, made from `notes/proposals/proto.html`
with Playwright. IBM Plex Sans Arabic stands in for Thmanyah. The crests in the mockups are plain coloured shields; on the
TV they would be TheSportsDB's badges, which the matches card already shows.

## 1. A football home theme

**What we already have.** All of the data for this already exists:
- `league_fixtures.py` runs in CI and writes each league's fixtures for the coming week, plus results from the last 36
  hours, to the `data-leagues` branch (`leagues/<slug>.json`). Each event has its round (`intRound`), kick-off, status,
  score and both crests (`strHomeTeamBadge` / `strAwayTeamBadge`).
- `teams-cache.json` and `assets/shared/teams.json` hold the Arabic club names. All 21 clubs of the Saudi Pro League are
  curated.
- The page's matches card already shows crests (`crestImg`, cached and shrunk), live scores (`liveRow`) and «يومك».
  The followed teams are a setting.

**Limit:** the free TheSportsDB key caps the league calls. Today's file for the Saudi Pro League holds only 3 games of round 13. The
script already works around the cap by asking for each day on its own. A full round needs either that workaround to
cover every day of the round, or a paid key (see 2).

**A: the owner's second reference, glass over a sky photo** (`notes/proposals/football-home.png`). From the top:
- **Clock and dates:** a big thin clock, a hairline divider, then the day in bold with the Hijri and Gregorian dates
  under it.
- **Top buttons:** our four, as square glass icon buttons.
- **Two glass cards under their section titles:**
  - «الطقس»: the city, a sun glyph, the temperature, the sky, and feels-like, humidity and wind.
  - «أوقات الصلاة»: five rows with icons. The next prayer is a lighter band, with «الظهر بعد ٤ س و٣٣ د» under it.
- **«مباريات اليوم»:** the followed teams first, then the Saudi Pro League. The source and its time are on the left
  (TheSportsDB, not 365Scores; see 2). Each card has the league and round, two crests with ×, and the kick-off time.
- **«Even better»:**
  - A game that clashes with a prayer says so in amber: «٥:١٠ م · المغرب ٥:١٧».
  - The followed club is green.
  - More games are shown by a sixth card fading at the edge, not by a chevron arrow.
- **«المفضلة»:** the focused app's name beside the title, and a glass tray of six big tiles. On the TV these are the
  apps' own banners.

**A-light** (`notes/proposals/football-home-light.png`) is the same grid on the approved light look: the photo on top
fades into sand, the cards are white, and the next prayer has the green ring. The cards stay readable on any photo.

**The owner's question: can it leave the favourites bar alone, or move it only while the widgets are in use?** Both
are drawn.
- **A** (`notes/proposals/football-home-A.png`): our normal favourites row, exactly as on the regular home. It has the same
  262×170 tiles, the same place, the «خيارات» tile, and the app's name above the row when an app has focus. The widgets fit
  above it. On the left are the weather and the prayers. On the right are the clock, then today's games as three lines in
  one glass card («٦ مباريات»; OK opens the league view with all of them). The image shows focus on the first game.
- **B** (`notes/proposals/football-home-B-widgets.png` and `notes/proposals/football-home-B-apps.png`):
  - When focus goes up into the widgets, the row slides down in 200 ms with ease-out. Only its top 56 px stay on screen,
    and the games grow into full cards.
  - When focus comes down to the apps, the row slides back up and the screen is A again.
  - Moving between apps never moves the row.
- **Recommendation for a D-pad: A.** The apps row is where most presses go, and in A it never moves. Every ▼ from the
  widgets lands in the same place, and nothing slides under the person's eye. That matters most for the older relatives,
  who find the TV by where things sit. B is a fair second choice if the owner wants the big match cards, because it moves
  only when focus crosses between the widgets and the apps.

**The owner's decision:** the favourites bar looks the same on every theme and every home layout. It always sits in its
tray and never slides, so B is out, and the «خيارات» tile is gone. The final home is
`notes/proposals/football-home-final.png`: A with this bar. The bar alone, on the sunset photo and on sand, is
`notes/proposals/fav-bar.png`.

### Favourites bar: exact spec (1920×1080; scale by height for 720p)

| Part | Value |
|---|---|
| Tray | x 120 to 1800 (1680 wide), 222 high, radius 32. Its bottom is 54 px above the screen's (the 5 % safe area), so its top is at y 804. The same place on every theme and layout. |
| Tray, over a photo (dark look) | fill `rgba(255,255,255,.14)`, background blur 30 px with saturate 1.3 (on a TV: the home photo blurred once, as the glass look already does, never a live blur), 1 px border `rgba(255,255,255,.28)`, a 1 px inner top highlight `rgba(255,255,255,.35)`, shadow `0 20px 50px rgba(0,0,0,.18)` |
| Tray, light look | fill `rgba(255,255,255,.72)`, the same blur, 1 px border `rgba(255,255,255,.85)`, a white 1 px inner top highlight, shadow `0 20px 50px rgba(40,36,24,.12)` |
| Tiles | 296×166 (the app's 16:9 banner), radius 18, 36 apart, 28 from the tray's edges, vertically centred. Five show; the first app is on the right (RTL). More apps scroll inside the tray, and the row never leaves it. |
| Resting tile | no movement and no dimming, shadow `0 2px 6px rgba(0,0,0,.12)` |
| Focused tile | `translateY(-6px) scale(1.08)` from the centre (about 320×179), above its neighbours. Ring 3 px: DGA green `#1B8354` on the light look, `#8FE0B4` over a photo. Shadow `0 18px 36px rgba(0,0,0,.40)` (`.22` on light). Sheen: a 125° white gradient over the tile, 26 % at the corner, 8 % at 38 %, gone by 52 %. It stays 14 px inside the tray's edges, so the focus never leaves the box. |
| Motion | 150 ms ease-out for the lift, scale, shadow and sheen when focus arrives or leaves. Nothing else moves. |
| App name | above the focused tile, centred on it, 30 px medium, white with a soft shadow over a photo, ink `#1B1F1D` on light. Its baseline is about 18 px above the tray's top edge (the text box is at y = tray top − 52). It follows the focus, and is hidden when focus is not in the bar. |
| No «خيارات» tile | the app options open from a long OK, as today, until a visible way is chosen that keeps the bar unchanged |
| Widgets above | everything else on the home must end by y 740, so the name has room above the tray |

The «⋮ خيارات» tile proposed in `UX-REVIEW.md` (problem 2) is overruled by this decision. The app box from that review
still opens from a long OK on the app.

**The first reference, on the light look** (an earlier pass, kept in `proto.html` as `#football`): a prayer strip and
four match cards with a live score.

On every version, OK on a card opens the league view the page already has (`#leagues=`). With no game today, the row
shows the next round's games with their day.

**B: «or even better»** (`notes/proposals/football-home-v2.png`):
- The followed team's next game is the large card. It has a countdown, and one line that places the game against the
  prayers: «تبدأ بعد العشاء بساعتين، والتذكير قبلها بربع ساعة». We have both data sets, and no football app has this line.
- The league table for the top 5 sits beside it, with the followed club in green. The table needs `lookuptable.php`
  from TheSportsDB. Its free-key cap must be checked in CI before we promise it.
- The reminder before kick-off reuses item 4.

**How it would ship:** a third choice under Settings › المظهر › الشكل («كرة القدم»). It would not be the default and it
would not touch the other looks. The data and the cache are the ones the page already uses, so it adds no new requests.

## 2. 365Scores

**No, on both counts.**
- **Data:** 365Scores has no public or partner API. Its data is licensed from Stats Perform, under a five-year supply deal
  signed in 2021. Its terms say: «you have no right to modify, edit, copy, reproduce, create derivative works of, reverse
  engineer… or in any way exploit any of the Content», and that the service is for personal use only
  (https://www.365scores.com/eula.html). Its publisher terms forbid robots and scraping. The `webws.365scores.com`
  endpoints that scrapers use are undocumented, and calling them from our app would break those terms.
- **Account linking:** 365Scores has no OAuth and no account API, so a user cannot connect their 365Scores account to
  another app.
- **ESPN's hidden API is no better:** the Disney terms forbid automated access.

**Today** we use TheSportsDB's free v1 key: the shared key, about 30 requests a minute, with the league caps described
above. CI fetches the data once and the TV reads small JSON files from `data-leagues`, so the TVs never call the API
themselves.

**The legal alternatives:**
1. **Keep TheSportsDB and support it on Patreon.** This lifts the caps and gives full rounds and the table, for a few
   dollars a month, paid by the project rather than by each TV. This is the recommendation.
2. **API-Football's free plan** (100 calls a day). It lists the Saudi Pro League, but whether the free plan includes the
   current season must be checked with a real key.
3. **football-data.org** (its free tier has no Saudi league) and **OpenLigaDB** (German leagues only) do not fit.

**"My favourite teams" without 365Scores:** the TV already has «الفرق». A one-time import on the phone page would let a
person type or paste their team names, and `find_team` would match them to our IDs. It needs no account.

Crests are the clubs' trademarks. We show them only to identify a club, from the provider's own artwork. They are never a
logo of ours and never in a store listing.

## 3. Screensaver photo packs

**Mockup:** `notes/proposals/photo-packs.png`. It is reached from Settings › البطاقات › شاشة التوقف › الصور. Each pack is a card
with a cover, its photo count and size, and its source:
- A pack that is on has the Sadu triangles and «مفعّل».
- A pack that is not downloaded says «نزّل».
- Several packs can be on at once, and the photos rotate across all of them.
- Our own set, «صورنا», stays and is on by default.

**Sources and licences:**

| Pack | Source | Licence |
|---|---|---|
| مدن سعودية | Wikimedia Commons | Only CC0, PD, CC BY or CC BY-SA, read per file from `extmetadata.LicenseShortName`. Saudi freedom of panorama for new buildings must be checked on Commons' Saudi Arabia copyright page first. |
| عواصم العالم | Wikimedia Commons | Same as above |
| طبيعة | Commons and NASA Earth images | Commons as above; NASA is public domain |
| الفضاء | images-api.nasa.gov (no key) | Public domain. No NASA logo or insignia, no third-party © images, nothing implying endorsement. |
| لوحات فنية | The Met (CC0), Art Institute of Chicago (CC0, `is_public_domain`), Cleveland (CC0, per record `share_license_status`), Rijksmuseum (CC0) | CC0 |

**Not usable for packs:**
- **Unsplash:** its licence forbids compiling its photos into a similar service, and its API guidelines require
  hotlinking with download tracking. Our current 86 Unsplash photos stay as they are.
- **Pexels and Pixabay:** both forbid standalone redistribution.

**Delivery:**
- CI builds each pack as a zip: 1920×1080 JPEGs plus `credits.json` (author, licence, link). Each zip goes on a
  `photos-<pack>` release.
- One `packs.json` index lists name, cover, count, size and sha256. The settings screen reads it.
- A pack downloads only when the person chooses it, is checked against its sha256, and stays on the TV. «حذف» frees the space.
- The credits are added to «حقوق الصور» and `docs/PHOTO-CREDITS.md` (CC BY and BY-SA need them).

**Content rules:** I found **no written rule** in the code or docs against people or religiously sensitive content. Our
own set includes Unsplash photos with small figures in them (`docs/PHOTO-CREDITS.md`). For packs, I propose writing the
rule down and having CI enforce it before review:
- no people (no faces or figures as the subject);
- no religious buildings other than mosques shown as architecture, no statues or idols, and no nudity. For artworks this
  means landscapes, still lifes, calligraphy and patterns only;
- a person reviews every pack once, the way `docs/CONTENT-REVIEW.md` does for the verses.

## 4. Appointment reminders on the screensaver

**Today:** a task (`Task.java`) has text, done, `at` (when it was **made**) and an id. It has no due time. A reminder
needs one new optional field, `due`, and an exact alarm. Old lists and `saver.sh tasks` keep working unchanged.

**Setting a reminder** (`notes/proposals/reminder-set.png`):
- **On the TV:** OK on a task in «المهام» opens its page. The rows are:
  - the text, with the mic;
  - «ذكّرني» (on or off);
  - اليوم, الساعة, «قبلها بـ» (0, 10, 30 or 60 minutes), «يتكرر» (no, daily, weekly);
  - «صوت خفيف» (off by default);
  - «أين يظهر».
- **By voice:** «ذكّرني الخميس الساعة ٥ بموعد الأسنان» fills the same rows. The page still asks for confirmation before
  it saves, as tasks do today.
- **On the phone page:** the new-task form gets a date field and a time field. A task with a time shows its bell and
  time in the list. The task goes to the TV only.
- **From the computer:** `saver.sh tasks add '<text>' --at '2026-11-05 17:00' --before 30`.

**How it appears** (`notes/proposals/reminder.png`):
- **Over the screensaver or the home screen:** a white card on top of the photo, with the text, the time and «بعد ٣٠
  دقيقة», and two buttons, «تم» and «ذكّرني بعد ١٠ دقائق».
- **Sound:** none by default.
- **Remote keys:** the first key press goes to the card and does not close the screensaver. «تم» marks the task done.
  With no press, the card stays until the due time and then becomes «الآن».
- **Inside another app:** the 5-second system notice («تنبيه عند الأذان» already works this way). It takes no focus and
  needs no overlay permission.
- **During the adhan:** the reminder waits until the adhan ends.

## 5. tv-prayer-companion (abuazam1)

**What it is:**
- A small Java add-on for Android TV, one commit (30 Oct 2026). Its README says it runs next to UrTV.
- It has **no licence file**, so it is all rights reserved: we may take ideas, not code. Its adhan recordings come from
  archive.org and belong to their owners.

**Worth taking:**
1. **Quran on the screensaver.** A recitation starts when the screensaver starts and stops when it ends. It has a volume
   cap and does not start if other media is playing (`IdleService.java`). Our «تلاوة وأذان» could offer this as a
   switch, off by default.
2. **Prayer reminder after the adhan.** An optional «وقت الصلاة الآن» card appears a few minutes after the adhan
   (`PrayerNotice.java`). Its forced remote lock (an accessibility service that blocks every key) should not be copied,
   or only behind the parental PIN.
3. **A guard that ends on time.** It stores its end time so it expires even if the process dies (`PrayerGuard.java`).
4. **Adhan when muted.** The adhan is heard even when the TV is muted, and the mute and volume are restored afterwards,
   with a hard stop (`AdhanPlayer.java`). We should check that ours restores the mute state the same way.

**Where we are already ahead:**
- It has a separate Fajr adhan; we already have «مؤذن الفجر».
- It recomputes on `TIME_CHANGED`/`TIMEZONE_CHANGED`. Check that our alarm does too.
- Its prayer times come only from Aladhan, for a hard-coded city, with no offline calculation. Ours are computed on the
  TV.

**Where it is worse:**
- Its settings activity is exported, so any app can start the adhan.
- It is signed with the debug key, which is the same update trap as `AGENTS.md`'s first rule.
- It needs appops grants over adb.

**Does it do anything better than us?** Only the Quran-on-screensaver idea.
