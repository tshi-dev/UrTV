# صفحة الدوري الجديدة: دراسة (4.5، لا كود بعد)

## الملخص بالعربي

**السؤال:** كيف نجعل صفحة الدوري في «المباريات» أوضح وأمتع على التلفزيون، خاصة لدوري يُلعب الآن؟ صفحة الدوري الحالية (`mpLeagueView`)
فيها ثلاثة أقسام: الترتيب، النتائج، المباريات. الترتيب فيها قائمة أسطر «١. الهلال  ٢٨ نقطة، لعب ١١». لا تُظهر الزمن الحاضر: ما يُلعب الآن،
ومتى المباراة القادمة، ومن صعد ومن نزل، ومن في منطقة الخطر.

**ماذا درسنا:** تطبيقات ومواقع عالمية: FotMob وSofaScore وOneFootball وESPN وBBC Sport والدوري الإنجليزي والليغا والبوندسليغا وتطبيق Apple TV
(دوري MLS) وYouTube TV وبطاقات Google. ومن العالم العربي: كووورة ويلا كورة وموقع دوري روشن وتطبيق «ثمانية» (الناقل الحالي لدوري روشن)
وSSC وbeIN وشاهد. ودرسنا رسوم البث التلفزيوني أيضًا. التفاصيل والمصادر في القسم الإنجليزي.

**حدود البحث:** الشبكة هنا لا تفتح مواقع هذه المنتجات. لذلك جاء البحث من نتائج محركات البحث وصفحات المتاجر والأخبار ودراسات الحالة.
كل ما لم نتحقق منه مكتوب بجانبه «لم يُتحقق». الصور الحقيقية تحتاج من يفتح المواقع بنفسه، ولهذا كتبنا
[GEMINI-BRIEF.md](GEMINI-BRIEF.md) ليجمعها Gemini.

**المبادئ الخمسة:**

1. **الصفحة تجيب عن «ماذا يحدث الآن وماذا بعده؟».** أعلاها: الجولة الحالية والعدّ للمباراة القادمة. تحته شريط المباشر.
   ثم ثلاثة أعمدة: الترتيب، والجولة الحالية، والأبطال. كل شيء آخر يبعد ضغطة «موافق» واحدة.
2. **الترتيب في الرئيسية نافذة على الجدول، لا الجدول كله.** تُظهر الأوائل الأربعة، وفريقك والفريقين حوله، وفرق الهبوط.
   وضغطة «موافق» تفتح الجدول كاملًا على شاشة واحدة: ١٨ صفًّا بلا تمرير، وكل الأعمدة.
3. **لون وكلمة معًا، لا لون وحده.**
   - المناطق: خط فاصل عادي بعرض الجدول تحت آخر مركز في كل منطقة، وعنده اسم المنطقة بنص باهت («دوري أبطال آسيا للنخبة»)، بلا ألوان ولا مفتاح، كما يفعل موقع دوري روشن وBBC. (قرار المالك ١٩ نوفمبر: لا أشرطة ملونة على حافة الصف.)
   - الشكل حروف «ف ت خ» في مربعات ملونة، والأحدث يمينًا.
   - الصعود والنزول سهم ورقم.
   - المباشر بالأحمر مع الدقيقة.
4. **الأسهم تتبع اتجاه القراءة.** في العربي اليمين أقدم واليسار أحدث: يمين جولة قبل، يسار جولة بعد.
   - شريط الجولات يعرض الموسم كله (١ إلى ٣٤) في صف واحد.
   - أعلى وأسفل لا يغيّران الصفحة أبدًا.
   - رجوع يخرج طبقة واحدة.
   - التركيز حلقة خضراء واحدة كما هو الآن.
5. **لا شيء يتحرك من مكانه، والصفحة خفيفة.**
   - الأرقام المتغيرة (العدّ، النتيجة، الدقيقة) في صناديق ثابتة العرض، فلا يزحزح تغيرها ما حولها.
   - الشعارات تُحمّل وقت العرض، ولا شيء مكانها حتى تصل (لا دائرة بحرف).
   - لا خطوط ولا صور جديدة.
   - المسافات بهوامش، لا `gap`، ولا `inset` ولا `clamp` ولا `min()`.
   - التقدير: ١٢ إلى ١٨ ك.ب. من الكود في `tv.html`.

**النماذج:** في [docs/mockups/league-v2/](../mockups/league-v2/README.md):
1. رئيسية الدوري
2. شريط الجولات
3. الجدول كاملًا
4. نظرة سريعة على نادٍ من الجدول
5. الرئيسية بالمظهر الداكن

6. صفحة النادي

كلها بحجم ١٩٢٠×١٠٨٠ و١٢٨٠×٧٢٠، وكلها بألوان صفحة «المباريات» نفسها.

**ما رآه المختبِر بعينه (الخطوة ١٠٣، ١٨ نوفمبر):** فتح المختبِر FotMob وSofaScore وESPN وBBC وموقع الدوري الإنجليزي وموقع دوري روشن
بدقة ١٢٨٠×٧٢٠، وصوّر الصفحات. هذا أكّد ما كان «لم يُتحقق». التفاصيل في القسم 2b. أهم ما وجده:
- **المناطق:** شريط ملون على حافة الصف، في FotMob وSofaScore والدوري الإنجليزي وموقع دوري روشن.
- **الشكل:** مربعات «ف ت خ»، في FotMob وSofaScore وBBC والدوري الإنجليزي.
- **أسهم الصعود والنزول:** في موقع دوري روشن الرسمي وحده، وهي حركة منذ الجولة الماضية. وهو ما اقترحناه بالضبط.
- **عمود «الخصم القادم»:** في FotMob وBBC والدوري الإنجليزي.
- **صفحة النادي في FotMob:** صف «آخر ٥» نتائج ملونة، وبطاقة المباراة القادمة، والجدول وصف النادي مُبرز. ولا توجد فيها صفحة بطولات.

**تصحيح:** بيانات الهدّافين من عمل 4.5 فيها **هدّاف كل موسم منتهٍ فقط**، لا قائمة هدّافي الموسم الجاري. لا يوجد مصدر مجاني مرخّص لأهداف كل
مباراة. لذلك استبدلنا عمود الهدّافين في الرئيسية بـ«الأبطال» و«هدّاف الموسم الماضي».

**صفحة النادي الجديدة:** النموذج ٦ في المجلد نفسه، وتفاصيلها في القسم 2c.

**ما يحتاج قرار المالك:** في آخر الملف (قسم 8).

---

## English details

### 1. Scope and method

**Goal.** A new interactive league page for «المباريات», mainly for a league in progress. It covers:
- the table, with zones, form, movement and a live table;
- next and live games, results and the matchweeks;
- top scorers, champions and clubs.

All of it must work with a D-pad at 3 m, at 720p, Arabic first, RTL.

**This task covers** research, this study, a brief for visual research, and mockups. No app code.

**Method.**
- Two research passes: global products, and Arab products with TV graphics.
- They used web search over official help pages, store listings, press releases, design case studies and news.
- **Limit:** this session's network does not allow the products' own sites. Blocked hosts include fotmob.com, sofascore.com, premierleague.com, bbc.com, apps.apple.com pages, yallakora.com and spl.com.sa. So no product page was read in full and no screenshot was taken.
- Claims below cite the page whose search summary carried them. Anything from memory or inference is marked **unverified**.
- The visual gap is what [GEMINI-BRIEF.md](GEMINI-BRIEF.md) is for.

### 2. What we found, product by product

**FotMob.**
- **League home tabs:** Overview, Table, Fixtures, Player stats, Team stats, Transfers, Seasons. Cups add a Knockout tab. ([PL](https://www.fotmob.com/leagues/47/overview/premier-league), [MLS](https://www.fotmob.com/leagues/130/overview/mls))
- **Overview:** puts a table, the latest results, the upcoming fixtures and the top scorers on one page.
- **Form column:** the last results plus the next fixture. ([team table](https://www.fotmob.com/teams/8650/table/liverpool))
- **Other tables:** an xG "justice table" ([FAQ](https://fotmob.com/faq)); the store copy mentions fixture difficulty in tables ([App Store](https://apps.apple.com/us/app/fotmob-soccer-live-scores/id488575683)).
- **Live table:** reviews say the table reorders live as goals go in ([review](https://www.betdico.com/4-apps-for-tracking-premier-league-live-standings-fixtures-and-match-results/)).
- **Unverified:** short/full/form views, home/away filters, coloured zone markers with a legend, fixtures by round with arrows.

**SofaScore.**
- League page: table, schedule and stats on one page ([PL](https://www.sofascore.com/tournament/football/england/premier-league/17)).
- "Smarter Standings", a redesign giving more context at a glance ([App Store](https://apps.apple.com/us/app/sofascore-live-sports-scores/id1176147574)).
- Live standings: "see how every result changes the table" ([Play](https://play.google.com/store/apps/details?id=com.sofascore.results)).
- Total, home and away tables exist in its data ([scraper model](https://apify.com/omkar-cloud/sofascore-standings-scraper)).
- **Unverified:** a zone bar at the row's start with a legend.

**OneFootball.**
- Competition pages: tables, group tables, brackets and news ([analysis](https://screensdesign.com/showcase/onefootball-all-soccer-scores)). Matches can be ordered by game week ([review](https://apps.apple.com/gb/app/id382002079)).
- **Table rows can show whether a club moved up or down.** This was the only source we found for movement indicators ([PL table](https://onefootball.com/en/competition/premier-league-9/table)).
- Conference split where the league has one ([MLS](https://onefootball.com/en/competition/major-league-soccer-15/table)).

**ESPN.**
- League hub and a separate standings page ([hub](https://www.espn.com/soccer/league/_/name/usa.1), [standings](https://www.espn.com/soccer/standings/_/league/eng.1)).
- Columns: GP, W, D, L, GD, P ([NWSL](https://www.espn.com/soccer/league/_/name/usa.nwsl)).
- Scoreboard by date, with live time ([scoreboard](https://www.espn.com/soccer/scoreboard), [guide](https://trnto.com/weekly-note/espn-soccer-scoreboard-your-ultimate-guide-1764812451)).
- **Unverified:** a coloured row edge with a legend; date navigation rather than matchweeks.

**BBC Sport.**
- The app has scores, stats, fixtures, results, tables and player stats ([App Store](https://apps.apple.com/gb/app/bbc-sport-news-live-scores/id377388936)).
- TV: smart-TV apps, iPlayer and Red Button ([guide](https://journalireland.net/tech/bbc-sport-football-news-guide/), [Final Score](https://en.wikipedia.org/wiki/Final_Score)).
- **Unverified:** form as the last 6 W/D/L letters, a qualification/relegation key, a live table on matchdays.

**Premier League.**
- The 2025 relaunch brought "Matchday Live": verified live scores, stats and **live table updates** ([news](https://www.premierleague.com/en/news/4337361/premier-league-launches-new-fan-facing-platforms-as-part-of-digital-transformation), [App Store](https://apps.apple.com/us/app/premier-league-scores-news/id1138895159)).
- **Unverified:** matchweek filter, home/away, form with the next opponent, zone lines.
- Broadcast: DixonBaxi's "Field of Play" is one motion system for live data, tables and profiles across broadcasters ([Design Week](https://www.designweek.co.uk/issues/15-21-august-2016/dixonbaxi-looked-give-premier-league-brand-new-life-screen/), [It's Nice That](https://www.itsnicethat.com/news/dixonbaxi-premier-league-field-of-play-120816)).
- Match graphics are by AE Live ([AE Live](https://www.ae.live/work/premier-league)).

**LaLiga.**
- App sections: Calendar, Results, Standings; a favourite-team area; minute-by-minute live ([app page](https://www.laliga.com/en-ES/apps/app-oficial), [Play](https://play.google.com/store/apps/details?id=es.lfp.gi.main&hl=en)).
- Results are organised by matchday ([results](https://www.laliga.com/en-GB/laliga-easports/results)).

**Bundesliga.**
- **Compact and detailed table views**, home and away form compared, table updates pushed with goals ([App Store](https://apps.apple.com/us/app/bundesliga-live-soccer-scores/id1005632836?platform=tv)).
- The site's table shows points, goals, home, away and form ([table](https://www.bundesliga.com/en/bundesliga/table)).
- Relaunched in 2025 ([DFL](https://www.dfl.de/en/innovation/bundesliga-app-relaunched/)).
- No TV app found.

**Apple TV (MLS).**
- The MLS screen browses matches in progress with live scores, replays, shows and **league standings** ([Apple support](https://support.apple.com/guide/tv/watch-mls-atvbce971f19/tvos)).
- Scores can be hidden ("Show Sports Scores") ([discussion](https://discussions.apple.com/thread/254670816)).
- Multiview of up to four games ([newsroom](https://www.apple.com/newsroom/2023/02/mls-season-pass-is-now-available-worldwide-on-the-apple-tv-app/)).
- At launch, the recent-score tiles **could not be selected**, and reviewers criticised it ([The Streamable](https://thestreamable.com/news/the-good-and-the-bad-first-impressions-of-apple-tvs-mls-season-pass-debut)).
- Deeper stats are sent to the phone app Apple Sports ([newsroom 2026](https://www.apple.com/newsroom/2026/02/major-league-soccer-kicks-off-2026-season-on-apple-tv/)).

**YouTube TV.**
- "Scores & stats" opens a right-hand column; the video shrinks beside it ([9to5Google](https://9to5google.com/2024/03/13/youtube-tv-video-player-redesign-sports/), [help](https://support.google.com/youtubetv/answer/9119412?hl=en)).
- Tabs: Teams, Scores, Stats, Key Plays and Fantasy ([Android Central](https://www.androidcentral.com/streaming-tv/youtube-tv-fantasy-custom-multiview-nfl-sunday-ticket)).
- **Stats sync to the point being watched**, so a recording is not spoiled ([TechCrunch](https://techcrunch.com/2023/04/17/youtube-tv-nabs-its-first-ever-technical-emmy-win-for-views-feature/)).

**Google sports cards.**
- Interactive league schedules by day and week, plus standings and stats ([TechCrunch 2013](https://techcrunch.com/2013/03/20/google-improves-sports-related-search-results-with-interactive-league-schedules-more-stats/), [SerpApi](https://serpapi.com/sports-results)).
- A matchup carousel across a league's games ([article](https://techieus.com/technology-news-gadgets/ai-and-football-how-new-search-features-are-changing-the-game-prep-scene/)).
- **Unverified:** the current tab names and the "last 5" icons.

**Thmanyah (ثمانية), the Roshn Saudi League's rights holder from 2025-26 to 2030-31** ([company](https://company.thmanyah.com/2700), [Bridge MENA](https://bridgemena.com/news/thmanyah-launches-digital-platform-and-satellite-channels-to-broadcast-saudi-football-tournaments-until-2031)).
- **Tournament page:** one page per competition, with four sections: Overview, Match schedule, Standings, Statistics ([help](https://thmanyah.open.cx/en/tournament-pages), [help 2](https://ask.thmanyah.com/hc/en-001/articles/47637929383313-Champions-pages)).
- **Standings follow the format:** points and goal difference for a league, stage by stage for a knockout.
- **Match page:** a live score, events and stats ([help](https://ask.thmanyah.com/hc/en-001/articles/47676798422161-Match-page-in-thmanyah-App)); called «داخل المباراة» in the store ([App Store](https://apps.apple.com/app/6746764325)).
- **Unverified:** round navigation, zones, form, digits.
- Note: SSC and Shahid held the rights until 2024-25 ([Wikipedia](https://en.wikipedia.org/wiki/Saudi_Sports_Company), [Arab News](https://www.arabnews.pk/node/2352026/media)). For the Saudi league today, Thmanyah is the product to look at.

**Roshn Saudi League (spl.com.sa and app).**
- The app has live scores, fixtures, standings, transfers, lineups, player season stats, alerts, Arabic and English, and a season calendar ([App Store](https://apps.apple.com/sa/app/id6477780627)).
- The data is Opta's, including OptaAI ([SVG](https://www.sportsvideo.org/2024/12/02/saudi-pro-league-and-clubs-to-embrace-optaai-as-part-of-comprehensive-expanded-partnership-with-stats-perform/)).
- The table on the site could not be read (**unverified**).

**Kooora (كووورة).**
- Owned by Footballco (GOAL) since 2022 ([Wamda](https://www.wamda.com/2022/04/footballco-acquires-kooora-25-million)); its pages now sit on GOAL's platform.
- **A pattern from Arab sites:** one season page bundles the fixtures, **each match's channel**, and the table ([Kooora](https://www.kooora.com/كرة-قدم/القوائم/جدول-مباريات-دوري-روشن-السعودي-2025-2026--القنوات-الناقلة-والترتيب/blt17c971b2cacda8b9)).
- GOAL Arabic **explains the zones in words** under the table (direct to the ACL, playoff, relegation) ([GOAL Arabic](https://www.goal.com/ar/%D8%A7%D9%84%D9%82%D9%88%D8%A7%D8%A6%D9%85/%D8%AA%D8%B1%D8%AA%D9%8A%D8%A8-%D8%AF%D9%88%D8%B1%D9%8A-%D9%8A%D9%84%D9%88-%D8%A7%D9%84%D8%AF%D8%B1%D8%AC%D8%A9-%D8%A7%D9%84%D8%A7%D9%94%D9%88%D9%84%D9%89-%D8%A7%D9%84%D8%B3%D8%B9%D9%88%D8%AF%D9%8A-2025-2026-%D8%AC%D8%AF%D9%88%D9%84-%D8%A7%D9%84%D9%85%D8%A8%D8%A7%D8%B1%D9%8A%D8%A7%D8%AA-%D9%88%D8%A7%D9%84%D9%82%D9%86%D9%88%D8%A7%D8%AA-%D8%A7%D9%84%D9%86%D8%A7%D9%82%D9%84%D8%A9/bltd798cd80fc0d71c0)).
- **A lesson from its app reviews:** after the redesign, users say the table's words are "jumbled" ([reviews](https://mwm.ai/apps/kooora/859950269)). That points to broken bidi, where number runs are not isolated in RTL rows.

**Yallakora, FilGoal, beIN, Shahid.**
- Nothing verified for Yallakora or FilGoal (the site was blocked).
- **Unverified:** Yallakora's «مركز المباريات» with day tabs, and tournament pages with table, scorers and matches tabs.
- beIN's app: scores, standings, favourites ([App Store](https://apps.apple.com/app/id1115211914)). Its table UI is unverified.
- Shahid's sports UI: nothing found.

**TV broadcast graphics.**
- **SSC (2021-25).** Identity, titles and broadcast graphics by WORKS ([WORKS](https://works.design/project/saudi-sports-company/)). The type is F37 Attila with a bespoke Arabic **Naskh/Kufic hybrid**, chosen to stay legible small and crisp large ([F37](https://f37.com/case-studies/ssc), [Fonts In Use](https://fontsinuse.com/uses/43873/saudi-sports-company-brand-identity-1)).
- **SPL world feed.** Produced by IMG with "unified graphics" ([Sportcal](https://www.sportcal.com/media/img-adds-to-saudi-soccer-tie-up-with-five-year-production-deal/)). Alamiya runs eight live graphics systems ([BroadcastPro](https://www.broadcastprome.com/case-studies/contentcreation-production/ushering-in-a-new-era-of-sports-broadcasting)).
- **beIN's 2014 rebrand.** About 400 graphics in four languages, Arabic among them ([RCS](https://realitychecksystems.com/bein-sport-rebrand), [Behance](https://www.behance.net/gallery/16694657/beIN-SPORTS-Rebranding-2014-On-Air-Graphics-Package)).
- **MBC Pro Sports.** Studio data scenes in Arabic on RCS LaunchPad ([SVG Europe](https://www.svgeurope.org/blog/headlines/mbc-pro-sports-kicks-off-saudi-pro-soccer-league-broadcasts-with-launchpad/)).
- **Scorebug trend for 2025-26: restraint.** The work is "not in adding more information, but in smarter ways to reveal it" ([SVG](https://www.sportsvideo.org/2026/06/09/designing-the-modern-scorebug-how-broadcast-graphics-teams-are-rethinking-the-most-important-element-on-screen/)). Logos have beaten letter codes ([Newscast Studio](https://www.newscaststudio.com/2025/09/25/fox-sports-nfl-score-bug-2025-season/)). Tight, square type loses its inner shapes at a distance ([critique](https://pimpmytype.com/fifa-2026-font/)).
- **RTL charts.** Arabic outlets that mirror their UI mirror their charts too ([paper](https://rakotondravony.com/assets/pdf/alebri2024design.pdf)).

### 2b. What the big sites do: the tester's own visit (step 103, 18 Nov)

**Method.** The tester opened these sites at 1280×720, with no login and no cookies accepted. The images stay on the owner's PC. The notes
are in `tester/league-shots.md` on `work/tester-report`. Where a note here differs from §2, this one is first-hand and wins.

**Leagues.**

| Site | Table columns | Zones | Form | Movement | Next / rounds |
|---|---|---|---|---|---|
| FotMob ([table](https://www.fotmob.com/leagues/536/table/saudi-pro-league)) | # team PL +- GD PTS Form Next | thin bar at the row start + legend (Champions League Elite, Qualification, League Two, Relegation) | last 5 squares, W green / D grey / L red | **none** | Next = the opponent's crest. Fixtures by date / round / team; Overview has «Round N» with arrows or a dropdown ([fixtures](https://www.fotmob.com/leagues/536/fixtures/saudi-pro-league)) |
| SofaScore ([league](https://www.sofascore.com/football/tournament/saudi-arabia/saudi-pro-league/955)) | with All / Home / Away tabs | a small coloured chip with the zone code on the badge + «Rules and legend» below | last 5 squares | none | «By date / By round» with a «Round N» dropdown and arrows; a featured next-match card; heavy (popups, ads) |
| ESPN ([table](https://www.espn.com/soccer/standings/_/league/KSA.1)) | club GP W D L F A GD P | none | none | none | a day strip with arrows and a calendar ([schedule](https://www.espn.com/soccer/schedule/_/league/KSA.1)) |
| BBC Sport ([PL](https://www.bbc.com/sport/football/premier-league/table)) | P W D L F A GD Pts Form | thin separator lines between rows (after 4th, 5th), no legend in view | last 5 squares + a trailing «next» square | none | n/a |
| premierleague.com ([tables](https://www.premierleague.com/en/tables)) | pos crest P W D L GF GA GD Pts Form Next | coloured bar at the row start | last 5 | none | Next = crest |
| **spl.com.sa** ([standings](https://www.spl.com.sa/en/standings)) | **arrow** club P W D L GF GA GD PTS TREND | coloured bar at the row start | a TREND column that expands per club | **up green / down red / dash, since the last round** | «Overall» and season dropdowns |
| Kooora / Yallakora | home pages only (ad and consent gates) | — | — | — | a yesterday / today / tomorrow strip with a match carousel |

**Clubs (FotMob: Al-Hilal, Al-Nassr, Al-Ittihad, Liverpool; [Al-Hilal](https://www.fotmob.com/teams/2529/overview/al-hilal)).**
- **Header:** the crest and country. Tabs: Overview, Table, Fixtures, Squad, Player stats, Team stats, Transfers, History, News.
- **Overview, top row:** «Team form», the last 5 as coloured score chips with the opponent's crest under each; and «Next match», with the competition, time, both crests and date.
- **Overview, below:**
  - the league table with the club's row highlighted;
  - a season-stats pitch with the best XI;
  - «Fixture difficulty» for the next three;
  - a fixtures carousel with arrows;
  - Liverpool also has an AI-written daily news summary.
- **Honours:** no honours page. History shows coach win % per season, past table positions, and a table per season ([history](https://www.fotmob.com/teams/2529/history/al-hilal)).
- **Squad:** photo, name, position, flag, number, age, height and value.

**What this changes in our proposal.**
- **Confirmed:**
  - the zone bar at the row's start, with a legend (FotMob, SofaScore, Premier League, spl.com.sa);
  - form as W/D/L squares (four sites);
  - movement arrows "since the last round". These are exactly the **official Roshn site's**, and that site is our users' reference, so we keep them.
- **New idea, not taken for now:**
  - a «next opponent» column (FotMob, BBC, Premier League). At 720p the full table has no room for it.
  - Instead, the club sheet and the club page show the next game in full.
- **Club page:** the FotMob overview's top row (form + next match) and its highlighted table are the backbone of our club page (§2c). Squad, player ratings and fixture difficulty are not taken: we have no data for them.
- **Honours:** no big site gives honours a place on the club page. This is where our 4.5 honours data stands out: «البطولات» with the latest title per competition.

### 2c. The club page proposal (mockup 6)

[6-club-page-1080p.png](../mockups/league-v2/6-club-page-1080p.png) / [720p](../mockups/league-v2/6-club-page-720p.png)

- **Header:**
  - the crest (runtime; nothing stands in for it while it loads), the name, and «الثاني في دوري روشن السعودي، ٢٦ نقطة من ١١ مباراة»;
  - on the left, «آخر بطولة» from the honours champions files.
- **Tabs:**
  - «النادي» (this overview), then «المباريات», «النتائج», «البطولات» and «المواجهات»: today's club view's groups;
  - follow / unfollow stays the last tab, as now.
- **Top row:**
  - «آخر ٥» as five score tiles, each the club's goals first, its colour, and the opponent under it, newest on the right;
  - beside it, the next game card with the round and a fixed-box countdown.
- **Below, «في الترتيب»:** the table window around the club, its row ringed, and OK opens the full table.
- **Below, «المباريات القادمة»:** by round, including other competitions (the AFC game in the mockup).
- **Below, «البطولات»:**
  - each competition with its latest title, «آخرها ٢٠٢٥-٢٠٢٦», and no count until the list is complete (the honours worker's rule; with a complete list: «و٩ قبلها في أرشيفنا»);
  - «هدّاف الدوري» when one of the club's players was a season's top scorer (`scorers/<lg>/<season>.json`, `team` = the club's id).
  - The source line is the honours one.
- **Data:** all of it is there or coming in 4.5:
  - `teams/<id>.json` for the next and last games;
  - `sports/<lg>.json` for the table, form and `mv`;
  - the honours `champions/` and `scorers/` files.
  - No squad, no player stats.

### 3. Comparison table

| Product | League home | Table: zones / form / movement / live | Next and live | Round navigation | TV (10-foot) |
|---|---|---|---|---|---|
| FotMob | Overview + 6 tabs | legend unverified / form + next game / unverified / live (reviews) | on Overview | by round, unverified | none |
| SofaScore | one page: table, schedule, stats | bar unverified / unverified / unverified / live standings | yes | round picker, unverified | none |
| OneFootball | table, groups, brackets | unverified / no / **yes, up/down** / no | Matches tab | by game week | none |
| ESPN | hub + standings page | unverified / no / no / unverified | scoreboard by date | by date | none |
| BBC Sport | scores, table, top scorers | key, unverified / letters, unverified / no / unverified | yes | by date, unverified | Red Button, iPlayer |
| Premier League | relaunch 2025 | lines, unverified / unverified / unverified / **live (Matchday Live)** | yes | matchweek, unverified | broadcast package |
| LaLiga | calendar, results, standings | not found | minute by minute | by matchday | none |
| Bundesliga | fixtures, table, match center | not found / home and away form / no / pushed updates | yes | unverified | none found |
| Apple TV (MLS) | live games, shows, standings | not found | live tiles (not selectable at launch) | not found | **yes, the reference** |
| YouTube TV | side column next to the video | not found | scores across the league | n/a | **yes; synced stats** |
| Google card | matches, standings | unverified | carousel | by day and week | n/a |
| Thmanyah | Overview, schedule, standings, stats | format-aware / unverified / unverified / live match page | yes | unverified | yes (app) |
| RSL app | scores, fixtures, standings, stats | unverified | yes | season calendar | none found |
| Kooora / GOAL Ar | season page: fixtures + channels + table | **zones in words** / unverified / no / no | with channel | unverified | none |
| **UrTV now** | 3 tabs: table, results, games | none / on the club only / none / none | games tab | none | **yes** |
| **UrTV v2 (proposed)** | header + live strip + 3 columns, 6 tabs | bar + words / ف ت خ squares / ▲▼n / live points | live strip + round + countdown | season strip 1–34 | **yes** |

### 4. Principles for a TV remote (D-pad, 3 m, 720p, RTL, Arabic first)

1. **One question per screen.**
   - The league home answers "what is happening now and next". The header holds the round and the countdown to the next game.
   - Under it: the live strip, then the table window, the current round, and the scorers.
   - Everything else is one OK away. Every card opens something (the Apple TV lesson).
2. **A window, then the whole.**
   - The home shows the table's top four, the followed club with one club either side, and the relegation places, with a line «٧ أندية أخرى، موافق: الجدول كاملًا» (hints say «موافق», never «OK», as in the rest of the app).
   - The full table is its own focused screen: the page header steps aside and all 18 rows show without scrolling, with every column. This follows Bundesliga's compact and detailed tables.
3. **Colour plus a word.**
   - **Zones (the owner, 19 Nov):** a plain full-width divider line under the last place of each zone, with the places and the zone's name in plain muted text at the line's start («دوري أبطال آسيا للنخبة»), as spl.com.sa and BBC do. No coloured edge bars, no colour chips, no legend row. (Superseded: a 6 px bar on the row's start edge (the right in RTL) with a legend in words under the table, like GOAL Arabic.
   - Form is ف ت خ in filled squares, newest nearest the points. The squares are not capsules.
   - Movement is ▲/▼ with the number of places.
   - Live is the red minute after the name and the points in red, with a «قبل المباريات الجارية» toggle.
   - The three zone colours are green, blue and red, readable on a washed-out panel.
4. **Direction follows reading.**
   - In RTL, Right is earlier and Left is later, on the rounds and on the days.
   - Up and Down move inside a list and never change the page. Back leaves one layer: the sheet, then the focused table, then the league.
   - There is one focus ring, the existing green one. The hint line in the tab row says what the keys do.
5. **Nothing jumps, nothing heavy.**
   - Changing numbers sit in fixed-width boxes, so the layout never moves (the clock rule). These are the countdown, the live score and the minute.
   - Number runs are isolated with `unicode-bidi:isolate`; this is the Kooora "jumbled" lesson.
   - Crests load at runtime; nothing stands in for them while they load (no letter-in-circle placeholder).
   - No new fonts or images. Margins instead of `gap`, nothing from R10's list.
   - The digits stay as the person set them. The mockups use Arabic-Indic, the app's default.

### 5. What we take

- **FotMob's single Overview** that holds the table, results, fixtures and scorers. Ours is the three-column home.
- **Form beside the table** (FotMob, BBC). Movement arrows (OneFootball).
- **Live table** (Premier League Matchday Live, SofaScore). Ours shows provisional points and a toggle back to the table before the games on now.
- **Compact and detailed tables** (Bundesliga): the window on the home, the full table one OK away.
- **Zones explained in words** (GOAL Arabic).
- **One page per competition with format-aware standings** (Thmanyah): a cup keeps 4.4's rounds and groups.
- **Matchweek-first navigation** (LaLiga, OneFootball, and how Arab sites talk: «الجولة ١١»).
- **Every tile selectable** (the Apple TV lesson).
- **Restraint** (the 2026 scorebug trend).
- **Bidi isolation of numbers** (the Kooora reviews).

### 6. What we don't take, and why

| Not taken | Why |
|---|---|
| xG / "justice table", fixture difficulty, player ratings | We have no such data (TheSportsDB free + our archive), and they are noise at 3 m. |
| News, transfers, video highlights tabs | No data or rights; the page is for games. |
| A tab per stat (team stats, player stats), a current scorers list | Too many tabs for a D-pad, and no data: 4.5's honours work has each past season's top scorer only. |
| A «next opponent» column in the table (FotMob, BBC, PL) | No room at 720p; the club sheet and the club page show the next game in full. |
| Squad, ratings, fixture difficulty on the club page (FotMob) | No data. |
| Kit-colour backgrounds (PL broadcast), club-coloured cards | No bundled club data or logos; colours would fight the zones. |
| Date strip navigation (ESPN) as the main navigator | A league in progress is talked about in rounds; days remain inside a round. |
| Swipe gestures, hover, horizontal carousels that hide items | D-pad only; the season strip shows all 34 rounds at once. |
| Pills and capsules for tabs, form or filters, and «·» separators | The house rules. Tabs are the underlined #mp-days, form squares have a 6 px corner. |
| Three-letter Latin codes for clubs | Arabic names fit at 26 px; codes are a Latin broadcast habit. |
| Score hiding / spoiler sync (YouTube TV, Apple TV) | Good, but it is a setting of its own. Proposed for later, not in v2. |
| Animated table reorders | WebView 66 on weak TVs (`lowram`): rows change in place. |
| Multiview | A player feature, not a page one. |

### 7. Our data, screen by screen (what is real today)

| Element | Source | Status |
|---|---|---|
| Table (P W D L GF GA GD Pts) | `sports/<id>.json` table computed from the archive | there (4.4) |
| Form, last 5 | the same file (each club's last 5) / `formOf()` | there |
| Movement ▲▼ | the table before the current round, from the season's results | **new**, computed in CI, about 1 byte per club |
| Home / away tables | the season's results | **new**, CI, optional |
| Live points | the live API (TheSportsDB free) + the table | **new**, client side, provisional |
| Current round, countdown | `fbGames()` / `fbRound()` | there |
| Rounds 1–34 and their games | `results/<id>.json` + coming games | there (round number per game: check coverage) |
| Champions, a past season's top scorer | 4.5 `work/45-honours` (`champions/`, `scorers/<lg>/<season>.json`: the season's top scorer only) | coming |
| **This season's scorers list** | none: no free licensed per-match scorers (honours study 2.1) | **not available**; the home shows «الأبطال» and «هدّاف الموسم الماضي» instead |
| Badges | runtime (TheSportsDB) | there |
| Zones per league | **new**: a small table per league (places → zone), kept in the archive index | to write; the owner checks it |

Weight: about 6–8 KB of CSS and 6–10 KB of JS in the template, no assets. `tv.html` stays well under 500 KB.

### 7b. The UI session's review (18 Nov) and what changed

The UI session approved the direction and all ten decisions, with these changes, which the mockups now carry:
- **Size floor:** at 720p, no text is smaller than the rows' muted secondary text (`.mpi i`, 24 at 1080p lengths, 16 px at 720p).
  This covers hints, the legend and sub-lines.
  - `render.mjs` checks every text at 1920×1080, 1280×720 and 960×540, and fails under the floor. The real page gets the same test the
    honours worker added.
  - The form squares are 36 (24 px at 720p) with 24 letters (16 px). If a table cannot fit five at that size, it shows the last 3.
  - The movement arrows are the size of the row's numbers.
- **Wording:**
  - «موافق», not «OK», in every hint.
  - «يمين ويسار: الجولات», not arrows used as decoration.
  - The honours wording as built: «٢٠٢٥-٢٠٢٦: النصر», «آخرها …», and no count until a list is complete.
- **Club page:** the follow item is a plain tab like the others; the ring shows only when it has the focus.
- **Full-table mode:** the title stays where the page header was. The change is a cut on weak TVs (`lowram`), never a slide.
- **Scope:** the honours work (work/45-honours, 4.4 beta 2) already builds «الأبطال», «الهدّافون», the past seasons in the side column, and
  the club's «البطولات». v2 builds on that code and wording, and makes no second version. The split is agreed with the honours worker
  before phase (a).

### 7c. The owner's verdict on the zone bars (19 Nov)

The owner circled the coloured bars at the row edges on 3-table-full and called them "AI slop". These changes are now in every mockup
(the full table, the home's window, the club page's window, the quick view behind its veil):
- **No coloured edge accents anywhere.** A zone is a plain full-width divider line under its last place: after 3rd, after 4th, and after
  15th for the relegation places.
  - At the start of each line is one plain, muted label naming the places and the zone: «دوري أبطال آسيا للنخبة», «دوري أبطال آسيا ٢»,
    «الهبوط» (the UI session: the zone's name only, on a 1 px line).
  - In a window that hides a zone's last place, the line sits before the next shown row.
- **No legend row:** no coloured squares and no zone colours. The arrows and the red «مباشر» minute need no key. The season strip's legend
  is a plain line too.
- **No letter-in-circle crest placeholders:** the real crest loads at runtime, and nothing stands in for it while it loads.
- **Still in the mockups, for the UI session's audit of AI-look patterns:**
  - the form squares and the club's score tiles (coloured fills with ف ت خ);
  - the dashed lines of the «٧ أندية أخرى» rows;
  - the round strip's squares;
  - the quick view's five stat boxes;
  - the rounded white cards.

  These are the #mp page's own cards and the data colours the big sites use. The audit decides which stay.

### 7d. The UI session's audit of AI-look patterns (19 Nov), applied

- **Form:** no boxes. «ف ت خ» are plain bold letters coloured as text only (green, grey, red), newest next to the points. The column head
  is «آخر ٥».
- **The club's «آخر ٥»:** plain score text («٤ : ٠», the club's goals first), the opponent under it, and the result letter in coloured text.
  No fill.
- **Movement:** plain signed numbers, «+٢» in green and «−١» in red, and «–» for no change. No ▲▼.
- **Live:** the red «مباشر ٦٧'» stays.
- **«٧ أندية أخرى، موافق: الجدول كاملًا»:** a plain muted text row, with no dashed lines.
- **The season strip:** plain numbers. Played rounds are muted, the current one is bold, and the selected one carries the page's focus ring.
  No shaded or filled squares.
- **The quick view:** plain label and value lines («فاز ٥، تعادل ٣، خسر ٣» and «له ١٦، عليه ١٤، الفارق +٢») between 1 px lines. No boxes.
- **The cards:** the page's own white panels for now (as `#mp` has them). Flat sections with 1 px lines are the alternative, and that is
  the UI session's call.

### 7e. Zone labels above their rows; the last audit points (19 Nov)

- **Each zone's label sits directly above the rows it names** (the owner's rule: a label under a zone is read as naming the rows below
  it).
  - A plain 1 px line goes before the zone's first place, with the name in muted text at the line: «دوري أبطال آسيا للنخبة» above row 1
    (the first line inside the table), «دوري أبطال آسيا ٢» above row 4, and «الهبوط» above row 16.
  - A thin unnamed line closes a zone that the next rows are not in (before row 5).
  - In a shortened table, a zone that starts in a hidden stretch gets its line above that stretch's «N أندية أخرى» row.
- **«N أندية أخرى، موافق: الجدول كاملًا»:** the same plain 1 px line, with the text muted.
- **The quick view:** one plain line, «فاز ٥، تعادل ٣، خسر ٣، له ١٦، عليه ١٤», with the numbers bold.
- **Cards:** the UI session keeps the page's own white panels (`#mp-list ul`) for consistency with «المباريات», the club page and the honours
  rows. A flat look for the whole matches page (sections on the sand with 1 px lines) would be one page-wide change, made later in one
  place. That is a question for the owner (§8, question 7).

### 7f. The owner's changes (19 Nov): form as coloured dots, rows easier to follow

- **Form is colour only.** Five equal solid dots per row: 14 px at 1080p, 12 px at 720p (`calc(--u*6 + 8px)`), with no border, glow or
  gradient. The newest is next to the points.
  - The colours differ in lightness as well as hue, for colour-blind readers.
    - Light look (CIE L\*): win `#145A37` (33), loss `#E0604E` (57), draw `#B0B3AD` (72).
    - Dark look: win `#5FD39A` (77), loss `#E2604E` (57), draw `#5E6168` (41).
  - The club page's «آخر ٥» and the quick view's results use the same dot beside the plain score.
  - The dot is the one element `render.mjs` exempts, by class (`fdot`), from "no rounded fill smaller than a row". The check stays for
    everything else.
- **Rows:**
  - a firmer 1 px line between every row (`#D9D5CA` light, white at 24 % dark);
  - a very light flat zebra (`#F8F7F3`, white at 3.5 % dark);
  - the followed club's row: its name in bold and a light flat fill a step darker than the zebra (`#EFEDE6`; white at 8 % dark), **no outline** (the UI session, after the owner's «no outline» for icons and focus);
  - the focused row keeps the page's ring and its light accent fill;
  - no edge bars, and row corners down to 4.
- **Large font, checked at 720p** (`7-table-full-large-720p.png`, `8-league-home-large-720p.png`; the page ×1.15, an over-estimate,
  since the real setting grows the text only):
  - the full table drops «له / عليه» first and scrolls with the ring for the last rows;
  - the home's champions card leaves the first screen (it is a tab);
  - the home's table scrolls with the ring for the relegation rows.

  Every text stays at or above the 16 px floor.

### 7g. The owner's changes (19 Nov, on fbdf5698)

- **The loss colour is the page's own red** (`--flo`, the red of «مباشر» and of a negative movement). Win and draw moved so the three
  still differ in lightness:
  - light look (L\*): win `#2E9E63` 58, draw `#C2C5BF` 79, loss `#B3261E` 40;
  - dark look: win `#1B8354` 48, draw `#4A4D53` 33, loss `#FF9A8F` 74.
- **A new setting, «شكل آخر المباريات»** (values `dots | boxes | boxes-letters`, after the owner's second look):
  - «نقاط ملونة»: the default, the colour-only dots.
  - «مربعات»: small square boxes, colour only. They are 16 px at 1080p and 14 at 720p, a little larger than the dots, with square corners
    and a gap between them.
  - «مربعات بحروف»: the same square boxes with ف ت خ inside. To keep a letter at the 16 px floor at 720p, the box is 24 px there (36 at
    1080p lengths), larger than the plain boxes, with a gap of 9.
  - The plain-letters style is dropped unless the owner asks again.
  - Mockups at 720p: `9-form-boxes`, `10-form-boxes-letters`, their large-font versions (`11`, `12`), and the club page with each (`13`, `14`).
  - All three fit at 720p and with the large font.
  - `render.mjs` exempts only `.fdot` and `.fbox`, by class.
  - The setting is specified in the plan (§3a3). It is built with phase (a), not now.
- **The column head is «آخر خمس مباريات»,** centred over its column, on the home table and the club page too. It fits on one line at 720p,
  and wraps to two rather than shrink.
- **Spacing:** the table panel has inner padding at the bottom (18) and the sides (20), and the rows 14 more. The last row and the row
  numbers no longer touch the panel's edges.
- **The 5 % safe area, found while checking this:** some earlier renders ran up to 20 px into the bottom and top margins at 720p.
  - Fixed: the page has #mp's own outer padding back (56 at 1080p lengths, top and bottom);
  - the full table's title, filters and hint share one row;
  - the live strip is one line a game;
  - rows are 38 with square corners;
  - the round page's cards are three a row.
  - `render.mjs` now fails on anything outside the 5 % safe area too, except the large-font renders, which scroll by design.

### 8. Questions for the owner

1. **The home's tab name:** «الرئيسية», or «الآن» for a league in progress?
2. **Zones:** a hand-kept list per league (places → ACL Elite, ACL2, relegation, playoff), or zones shown only where we are sure? A wrong zone is worse than none.
3. **Live points:** should the table move while games are on (provisional, in red), with a toggle back? Or should it stay as it was until full time?
4. **Form order:** newest next to the points (right, as in the mockups), or newest at the far left?
5. **Digits** follow the person's setting; is that agreed for the table too?
6. **Phase order:** (a) the table with zones, form and movement, plus the full-table screen; (b) the home with the live strip and the round; (c) the season strip; (d) the club sheet.
7. **Cards (from the UI session):** keep the page's white panels, or make the whole matches page flat (sections on the sand, 1 px lines)? This would be one page-wide change, not for the league page alone.
