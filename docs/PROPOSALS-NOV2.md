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
| Tray | x 120 to 1800 (1680 wide), 218 high, radius 32. Its bottom is 54 px above the screen's (the 5 % safe area), so its top is at y 808. The same place on every theme and layout. |
| Tray, over a photo (dark look) | fill `rgba(255,255,255,.14)`, background blur 30 px with saturate 1.3 (on a TV: the home photo blurred once, as the glass look already does, never a live blur), 1 px border `rgba(255,255,255,.28)`, a 1 px inner top highlight `rgba(255,255,255,.35)`, shadow `0 20px 50px rgba(0,0,0,.18)` |
| Tray, light look | fill `rgba(255,255,255,.72)`, the same blur, 1 px border `rgba(255,255,255,.85)`, a white 1 px inner top highlight, shadow `0 20px 50px rgba(40,36,24,.12)` |
| Tiles | 304×170 (the app's 16:9 banner), radius 16, 24 apart, 32 from the tray's sides and 24 from its top and bottom (one 8 px scale). Five show; the first app is on the right (RTL). More apps scroll inside the tray, and the row never leaves it. |
| Resting tile | no movement and no dimming, shadow `0 2px 6px rgba(0,0,0,.12)` |
| Focused tile | `translateY(-6px) scale(1.08)` from the centre (about 328×184), above its neighbours. Ring 3 px: DGA green `#1B8354` on the light look, `#8FE0B4` over a photo. Shadow `0 18px 36px rgba(0,0,0,.40)` (`.22` on light). Sheen: a 125° white gradient over the tile, 26 % at the corner, 8 % at 38 %, gone by 52 %. With the ring it stays at least 8 px inside the tray's edges, so the focus never leaves the box. |
| Motion | 150 ms ease-out for the lift, scale, shadow and sheen when focus arrives or leaves. Nothing else moves. |
| App name | above the focused tile, centred on it, 30 px medium, white with a soft shadow over a photo, ink `#1B1F1D` on light. Its text box is at y = tray top − 56, so its baseline sits about 16 px above the tray. It follows the focus, and is hidden when focus is not in the bar. |
| No «خيارات» tile | the app options open from a long OK, as today, until a visible way is chosen that keeps the bar unchanged |
| Widgets above | everything else on the home must end by y 728, so the name has room above the tray |
| Spacing everywhere (the owner's rule) | text at least 16 px inside any box (24 on big cards), gaps between cards on one 8 px scale (24 between widgets and between match cards), a section title 16–24 px above its box |

**The tray may move down a little, and only for a widget** (`notes/proposals/football-home-final-widgetfocus.png`).
- When focus enters the matches widget, the widget opens into a row of game cards that scrolls right to left with ◀ ▶.
  To make room, the tray moves down 40 px, from y 808 to 848, in 250 ms with ease-in-out. It looks exactly the same, and its tiles move with it.
- The app name above the tray is hidden while focus is outside the bar.
- When focus comes back down to the apps, the tray returns to y 808 in the same 250 ms before the tile lifts, and the
  widget closes back to its three lines.
- Nothing moves while focus stays on the apps, or while it moves between the other widgets (weather and prayers need no
  extra room).
- At 848 the tray's bottom is at 1066, still on the screen. 40 px is the most it ever moves.

### The «المباريات» page

It opens with OK on the matches widget, and from Settings › المزيد › المباريات. It is a page of its own on the light look:
- `notes/proposals/matches-page.png`:
  - **Days** across the top: أمس، اليوم، غدًا, then the next four days. The chosen day is green with a 4 px green bar
    under it.
  - **Leagues** down the right side, each with its game count. «فرقك» comes first.
  - **The followed team first**, under «فرقك». A live game shows «مباشر ٦٧′» in green and the score.
  - **The rest by league and round.** Each row: time, home team with crest, × or score, away team with crest, a note
    when the game clashes with a prayer (amber), and the broadcaster's mark.
  - The source and its age sit beside the title.
- `notes/proposals/matches-page-detail.png`: OK on a game opens its card over the page. It shows:
  - the league and round;
  - big crests and the kick-off time;
  - the venue and the broadcaster;
  - «مع الصلاة»: a line from Maghrib to 11 pm with the prayers marked and the game drawn as a green bar, and one
    sentence («تبدأ بعد العشاء بساعة و١٣ دقيقة»);
  - «ذكّرني قبلها بربع ساعة», which makes a reminder from item 4, and «تابع الأهلي».
- **Data:**
  - Venue (`strVenue`) is in TheSportsDB's events; `league_fixtures.py` must add it to `KEEP`.
  - The page reads the same `data-leagues` files as the home, so it adds no new requests.
  - Days beyond the coming week are out of scope (the files hold 7 days ahead and 36 hours back).

### Broadcast marks

**Decision: keep a small table of our own for each league and season, and show the rights holder, not a channel per game.**
- **The Saudi Pro League:**
  - Thmanyah (SRMG) holds exclusive MENA rights from 2025-26 to 2030-31. That covers the Pro League, the King's Cup, the
    Super Cup and the First Division (sportbusiness.com, 14 June 2025; argaam.com).
  - SSC lost the league and closed on 5 October 2025.
  - Thmanyah shows games free on thmanyah 1, 2 and 3. When more than three overlap, the rest are in the Thmanyah app.
  - So every Saudi Pro League game says «ثمانية», and the detail card says «ثمانية، أو تطبيق ثمانية».
- **The table:** `assets/shared/broadcast.json`, as `{league: {season: {"ar": "ثمانية", "en": "Thmanyah"}}}`. Each
  league's entry is checked once a season, with the source link in the file. A league missing from it shows no mark,
  never a guess.
- **TheSportsDB has TV endpoints**, `lookuptv.php?id=` and `eventstv.php?d=`.
  - On the free key they are reported to return about one row a day.
  - No Saudi channels were found for the Saudi Pro League.
  - Too thin to rely on; worth a second look with a Patreon key.
- **API-Football has no TV data.**
- **Commercial sources:** Sportmonks and SoccersAPI sell TV data, but they cost money and are not needed while one holder
  has the whole league.
### Rights holders for every competition we show (MENA, 2026-27)

**What we show today:**
- the 16 leagues of `Prefs.LEAGUES`;
- every game of a followed club or national team, in any competition (UEFA, AFC, the King's Cup, cups, qualifiers);
- F1, UFC and boxing (`Prefs.EVENTS`).

A game's competition is its TheSportsDB `idLeague`, so the table is keyed by league id and covers cup games too.

**How sure this is:** every row comes from web search snippets of 2 November 2026, not from articles opened and read. CI
could not reach the sites, and neither could this session. Each row must be opened and checked before it goes into
`broadcast.json`.

**The marks' names:**
- beIN shows as «beIN».
- Thmanyah shows as «ثمانية».
- MBC shows as «شاهد».
- STARZPLAY shows as «STARZPLAY».
- Dubai Sports shows as «دبي الرياضية».
- AD Sports shows as «أبوظبي الرياضية».
- Al Kass shows as «الكأس».
- ON Sport shows as «أون سبورت».
- DAZN shows as «DAZN».
- Apple TV shows as «Apple TV».

| Competition | Holder, MENA 2026-27 | Split rights, notes | Source | Confidence | Mark |
|---|---|---|---|---|---|
| Saudi Pro League (Roshn) | Thmanyah (SRMG), exclusive 2025-26 to 2030-31 | thmanyah 1/2/3 free, the overflow in the Thmanyah app | [sportbusiness](https://www.sportbusiness.com/news/srmgs-thmanyah-nets-spl-exclusivity-to-2031/) | high | ثمانية |
| Saudi First Division (Yelo) | Thmanyah | Same deal | [sportcal](https://www.sportcal.com/media/srmgs-thmanyah-secures-exclusive-saudi-league-rights-until-2031/) | high | ثمانية |
| King's Cup, Saudi Super Cup | Thmanyah | Same deal, to 2031 | [bridgemena](https://bridgemena.com/?p=13448) | high | ثمانية |
| Premier League | beIN, to 2027-28 | All 380 games, streamed on TOD | [beIN](https://www.beinsports.com/en-mena/football/premier-league/articles-video/bein-media-group-extends-exclusive-broadcast-rights-of-the-premier-league-across-mena-until-2028-2025-06-17) | high | beIN |
| La Liga | beIN, to 2027-28 | All games. In Iraq it is shared with 1001. | [laliga.com](https://www.laliga.com/en-US/news/bein-extends-exclusive-laliga-broadcast-rights-across-34-markets-in-mena-and-apac) | high | beIN |
| Serie A | STARZPLAY (with Abu Dhabi Media), 2025-26 to 2027-28 | Kuwait's Shasha also bought rights in 16 MENA countries; the overlap is unclear | [sportcal](https://www.sportcal.com/news/starzplay-retains-serie-a-rights-exclusively-in-mena-until-2028/) | medium | none until checked |
| Bundesliga | MBC Group, 2025-26 to 2027-28 | Every game on Shahid; 3 a week free on MBC Action. Was beIN to 2025. | [insideworldfootball](https://www.insideworldfootball.com/2025/08/22/bundesliga-agrees-groundbreaking-broadcast-deal-mbc-group/) | high | شاهد |
| Ligue 1 | beIN, 2024-25 to 2028-29 | All games | [qatar-tribune](https://www.qatar-tribune.com/article/248406/sports/european-football-returns-to-bein-sports-for-2026-27-season) | high | beIN |
| Primeira Liga (Portugal) | No MENA buyer found | Sold country by country until a central sale in 2028-29 | [advanced-television](https://www.advanced-television.com/2026/08/20/dazn-livemode-expand-portugal-football-rights-deals/) | low | none |
| Eredivisie | Dubai Sports, 2025-26 to 2027-28 | Reported free-to-air | [eredivisie.com](https://eredivisie.com/news/dubaisports-broadcast-across-mena/) | high | دبي الرياضية |
| Turkish Süper Lig | beIN / TOD | **Top games only**: a game beIN does not list has no holder | [beIN](https://www.beinsports.com/en-mena/football/articles-video/european-football-returns-to-bein-sports-for-the-2026-27-season-2026-08-11) | medium | none (only some games) |
| Egyptian Premier League | ON Sport (Egypt), 2024-27 | Some games free-to-air; no MENA-wide holder found | [africasoccer](https://africasoccer.com/pyramids-vs-smouha-tv-channel-live-stream-and-kick-off-time-for-egyptian-league-decider/) | medium | none (Egypt only) |
| UAE Pro League (ADNOC) | Abu Dhabi Sports and Dubai Sports, shared | Extended for three seasons; exact dates not found | [broadcastprome](https://broadcastprome.com/news/adm-and-dmi-extend-uae-pro-league-tv-partnership) | medium | none until checked |
| Qatar Stars League | Al Kass, host broadcaster and only holder | — | [qsl.qa](https://qsl.qa/en/news/al-kass-sports-channels-become-host-broadcaster-jetour-league) | high | الكأس |
| MLS (2026) | Apple TV, worldwide | Inside the Apple TV subscription since 2026 | [apple.com](https://www.apple.com/newsroom/2025/11/major-league-soccer-is-coming-to-apple-tv-starting-in-2026/) | high | Apple TV |
| NBA | beIN, renewed Nov 2025 | Up to 9 games a week plus the playoffs and Finals; the rest only on League Pass | [nba.com](https://pr.nba.com/nba-and-bein-media-group-announce-multiyear-partnership-extension-in-the-middle-east-and-north-africa) | high | none (only some games) |
| NFL | DAZN (NFL Game Pass), worldwide from 2023 | No MENA TV deal found | [nfl.com](https://www.nfl.com/news/nfl-dazn-enter-long-term-agreement-delivering-game-pass-international-beginning-) | medium | DAZN |
| UEFA Champions, Europa and Conference League | beIN, 2024-27 cycle | All games, 24 markets | [beIN](https://www.beinsports.com/en-mena/football/articles-video/bein-media-group-extends-rights-deal-in-mena-and-asia-to-broadcast-uefa-club-competitions-until-2027-2025-04-28) | high | beIN |
| AFC Champions League Elite and other AFC club competitions | beIN, 2025-28 | Exclusive in 17 countries **including Saudi Arabia**; not Thmanyah | [qatar-tribune](https://www.qatar-tribune.com/article/193564/sports/bein-reveals-mena-wide-broadcast-plans-for-asian-football) | high | beIN |
| AFC Asian Cup 2027 (Saudi Arabia, 7 Jan – 5 Feb 2027) | beIN | Same AFC deal | same as above | high | beIN |
| World Cup 2026 and its qualifiers | Over (beIN had it) | Nothing left in 2026-27 | [wego](https://blog.wego.com/where-to-watch-the-fifa-world-cup-in-saudi-arabia/) | high | — |
| Arab Cup, friendlies, other national-team games | Not known | Each game is sold on its own | — | low | none |
| Formula 1 | beIN, 2024–2033 | Not MBC, whose free-to-air deal ended in 2023 | [beIN](https://www.beinsports.com/en-mena/motorsports/motorsports-formula-1/articles/bein-media-group-secures-exclusive-10-year-broadcast-partnership-with-formula-1-across-25-countries-in-mena-and-t%C3%BCrkiye-2024-02-28) | high | beIN |
| UFC | STARZPLAY, from April 2024 | End date unknown | [broadcastprome](https://www.broadcastprome.com/news/ufc-and-starzplay-forge-exclusive-broadcast-partnership-for-mena-region/) | medium | none until checked |
| Boxing (Riyadh Season, The Ring) | DAZN | Co-exclusive in MENA; the other holder was not found | [sportcal](https://www.sportcal.com/media/dazn-in-major-multi-year-partnership-with-riyadh-season/) | medium | none (co-exclusive) |

**Rules for the mark:**
- **Unknown holder:** no holder, low confidence or an unchecked row shows nothing. The screen never guesses, and never
  says «غير متاح».
- **Holder with only some games:** a league whose holder shows only some games (Süper Lig top games, NBA 9 a week) shows
  nothing, because the free data cannot tell which games are covered. A wrong channel is worse than none.
- **One holder for every game:** shows the mark on every game.
- **A free channel on the side:** Bundesliga's 3 games a week on MBC Action, and thmanyah 1/2/3 for the Saudi league. The
  per-game mark stays the holder's. The free channel is named only in the detail card, when it is known for that game.
- **Each row carries its contract's end:** `"until": "2027-28"`. A row past its end is dropped automatically, so a stale
  mark never outlives the deal.
- **CI warns before deals lapse:** it opens an issue in the month before a season starts, listing the rows that need
  checking.
- **Checks:** `tests/check.py` fails when a row has no source link or no end season.

- **The marks in the PNGs** are plain text in a small rectangle, not the broadcasters' logos. On the TV it would be the
  same: text, never a trademark.
- **The «SSC» app tile** in the bar mockups is replaced by «ثمانية», since SSC closed.

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

## 6. The «الأسواق» page

It opens with OK on the markets card or widget, and from Settings › المزيد › المباريات والأسواق. A screen of its own,
with three tabs so each stays readable from the couch: **السوق**, **قائمتي** and **الأخبار**.

**Mockups** (all in `notes/proposals/`):
- `markets-page.png`: the «السوق» tab, dark glass over the home photo blurred once.
- `markets-page-light.png`: the same tab on the light look.
- `markets-page-watchlist.png`: the «قائمتي» tab.
- `markets-page-news.png`: the «الأخبار» tab.
- `markets-page-detail.png`: OK on a stock.

**How to read the mockups:**
- A card or cell with a **dashed border** needs a source we do not have: one that is paid, needs permission, or is not
  fetched yet. Its numbers or headlines are illustrative.
- On the TV, such a card is **not shown** until its source exists. Everything with a solid border uses data we already
  have, or public facts.
- The prices, changes and lines are the real `data-markets/markets.json` of 2 November 2026, after the close.

**Header, on every tab:**
- the market's state with a countdown («تداول مغلق · يفتح بعد ١١ س و١٥ د، الثلاثاء ١٠:٠٠ ص»);
- the last update, with «أسعار متأخرة».

Tadawul's hours are public facts: continuous trading 10:00–15:00, Sunday to Thursday, then the closing auction to 15:10.
Ramadan hours differ and come from Tadawul's yearly notice.

**«السوق»:**
- **TASI:** value, change in points and %, and a large intraday line against yesterday's close. Range tabs
  اليوم / أسبوع / شهر / سنة; «اليوم» is what we have today.
- **«اتجاه السوق»:** rising, unchanged and falling as a bar with counts, plus the session bar. The counts are computed
  from the 30 companies we fetch, and say so: 19 up and 11 down on 2 November.
- **«القطاعات»** (dashed):
  - Tadawul's sector indices have no free source: no Yahoo symbol was found, and Investing and TradingView do not allow
    reuse.
  - It needs Saudi Exchange's paid market data.
- **«الأكثر ارتفاعًا وانخفاضًا»:** the top 3 and bottom 3 of the 30 (real).
- **«الأكثر نشاطًا»** (dashed): the volume field (`regularMarketVolume`) is in the Yahoo answer we already get, so it
  costs no request. It covers only the 30 companies, under Yahoo's terms (below).
- **Turnover in riyals:** no free source, so it is not shown at all.

**«قائمتي»:**
- Each watched stock: name, code, its own day line (a sparkline), price and change.
- Gold (per gram and per ounce), silver and Brent, each with a sparkline.
- **Currencies:** only fixed pegs against the riyal (dollar, dirham, Qatari riyal, Bahraini dinar), which need no source.
  The euro and others would come once a day from the ECB's free reference rates, worked out through the dollar.
- **Dividend dates are left out:**
  - Argaam's dividend pages need written permission.
  - The exchange's financial calendar has no feed and no reuse permission.

**«الأخبار»** (dashed):
- **The design:** headlines only, newest first, with the source and the time. A side list filters: الكل، قائمتي، إعلانات
  تداول، هيئة السوق المالية، واس. OK shows a QR to read the item on the phone, on its own site; the TV never shows
  article text.
- **The sources, none usable today:**
  - **Argaam:** its terms require prior written authorisation, and no RSS was found.
  - **Saudi Exchange announcements:** no feed was found, and the site says Tadawul owns the content.
  - **Mubasher:** no RSS terms were found.
  - **CMA:** announcements by email only.
  - **SPA:** no RSS URL or reuse terms were found for news. Its photo platform is CC BY-SA, which does not prove the news
    is.
- **So the tab stays off until a source agrees in writing.** The step is an email to SPA and to Saudi Exchange
  (csc@saudiexchange.sa) asking whether a headline plus a link may be shown.

**The stock card** (OK on any stock):
- name, code, price, the change, and a large chart with the same range tabs;
- the day's open and range (real, from the 15-minute closes);
- the 52-week range, volume and last dividend (dashed: the first two are in Yahoo's answer but not kept yet; dividends
  have no free source);
- the company's own announcements (dashed: needs Tadawul's permission);
- «أزل من قائمتي» and «أظهره في شريط الأسواق».

There is no prayer line, because Tadawul trades straight through Dhuhr.

### Data: what we have, what is cheap, what is paid

**Yahoo already gives us** (`.github/tools/market_prices.py` in CI, `data-markets`):
- price, previous close and the 15-minute closes;
- for TASI, the 30 largest companies, the S&P 500, about 50 US tickers, gold, silver, Brent, bitcoin and XRP.

**Cheap: the same answer, kept fields.** Day high and low, 52-week high and low, and volume, from `meta`. These were not
verified live, because Yahoo is blocked here.

**Cheap: one more request per symbol.** Week, month and year charts (`range=1y&interval=1d`), only for TASI and the
watched stocks.

**Free, other source:**
- ECB daily reference rates for the euro, the pound and others.
- Tadawul's hours (public facts).

**Paid or needs permission, not shown:**
- sector indices;
- breadth and turnover of the whole market;
- most active across the whole market;
- dividend calendars;
- Argaam headlines;
- Tadawul announcements;
- live ticks and order books.

**Yahoo's terms are a real risk, already in the code today:** the licence is personal and revocable, and it forbids using
the data to build a data feed. Our CI file served to every TV looks like that. The page adds no new Yahoo use beyond a few
more fields. For a public release, the clean route is a licensed delayed feed from Saudi Exchange. That is the owner's
call.
