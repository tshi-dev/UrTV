// The football home's widgets (#widget=1&football=1) and «المباريات» (#matchpage=1) in a real browser, offline, on fixed data
// (docs/PROPOSALS-NOV2.md §1): the three cards end by y 572 and the open row by 824 at 1080p, every text at least 16 px inside its box,
// the followed team first, the live minute and score, the prayer that falls in a game, the marks of assets/shared/broadcast.json only
// when a row is high, whole, verified and in season, the day strip, the keys, the game's card and its two buttons.
//   node tests/football_page.test.mjs            (SHOTS=1 also writes docs/shots/football-*.png)
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');   // 7:13 in Riyadh, a Wednesday
const badge = n => `https://r2.thesportsdb.com/images/media/team/badge/${n}.png`;
const ev = (id, home, hid, away, aid, iso, extra = {}) => ({ idEvent: id, idLeague: '4668', idHomeTeam: hid, idAwayTeam: aid, strHomeTeam: home,
  strAwayTeam: away, strStatus: 'NS', strTimestamp: iso, dateEvent: iso.slice(0, 10), strTime: iso.slice(11), strSport: 'Soccer',
  strSeason: '2026-2027', intRound: '13', strHomeTeamBadge: badge('h' + id), strAwayTeamBadge: badge('a' + id), ...extra });
const LEAGUE = { league: 'Saudi-Arabian Pro League', id: '4668', updated: '2026-11-04T04:10:00Z', events: [
  ev('1', 'Al-Hilal', '136013', 'Al-Ettifaq', '136020', '2026-11-04T03:30:00', { strStatus: '2H', intHomeScore: '2', intAwayScore: '1', strProgress: '67' }),
  ev('2', 'Al-Ahli', '137721', 'Al-Taawoun', '136012', '2026-11-04T17:00:00', { strVenue: 'King Abdullah Sports City' }),
  ev('3', 'Al-Fateh', '136011', 'Al-Riyadh', '147445', '2026-11-04T14:10:00'),
  ev('4', 'Al-Nassr', '136014', 'Al-Qadsiah', '136016', '2026-11-04T18:00:00'),
  ev('5', 'Al-Ittihad', '136015', 'Al-Shabab', '136017', '2026-11-05T18:00:00'),
] };
const ROWS = [{ idLeague: '4668', competition: 'Saudi Pro League', ar: 'ثمانية', en: 'Thmanyah', detail_ar: 'ثمانية، أو تطبيق ثمانية', detail_en: 'Thmanyah, or the Thmanyah app',
  from: '2025-26', until: '2030-31', source: 'https://example.org/source', confidence: 'high', whole: true, verified: '2026-11-04', app: ['com.thmanyah.shasha.tv', 'com.thmanyah.shasha'] }];

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
async function open(hash, { file = 'tv.html', rows = null, width = 1920, height = 832 } = {}) {
  const ctx = await browser.newContext({ viewport: { width, height }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    if (u.includes('/data-leagues/leagues/saudi-arabian-pro-league.json')) return r.fulfill({ json: LEAGUE, headers: { 'Access-Control-Allow-Origin': '*' } });
    if (u.includes('eventsnext.php') || u.includes('eventslast.php')) return r.fulfill({ json: { events: null, results: null }, headers: { 'Access-Control-Allow-Origin': '*' } });
    if (u.includes('api.open-meteo.com')) return r.fulfill({ json: { current: { temperature_2m: 28, apparent_temperature: 30, relative_humidity_2m: 18, wind_speed_10m: 12, weather_code: 0, is_day: 1 } }, headers: { 'Access-Control-Allow-Origin': '*' } });
    if (u.includes('/badge/')) return r.fulfill({ path: path.join(ROOT, 'android/app/src/main/res/mipmap-xhdpi/ic_launcher.png') }).catch(() => r.abort());
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  if (rows) await p.addInitScript(r => { window.__rows = r; }, rows);
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#' + hash + '&gap=0');
  if (rows) await p.evaluate(r => { BROADCAST.length = 0; r.forEach(x => BROADCAST.push(x)); }, rows);
  await p.waitForTimeout(900);
  return { p, ctx, errors };
}
const box = (p, sel) => p.evaluate(s => { const b = document.querySelector(s).getBoundingClientRect(); return { l: b.left, t: b.top, r: b.right, b: b.bottom, w: b.width, h: b.height }; }, sel);
// every text node's box inside its card, at least `pad` px from the card's edges
const crowded = (p, card, pad) => p.evaluate(([sel, pad]) => {
  const out = [];
  document.querySelectorAll(sel).forEach(c => {
    const b = c.getBoundingClientRect(), w = document.createTreeWalker(c, NodeFilter.SHOW_TEXT);
    for (let n; (n = w.nextNode());) {
      if (!n.textContent.trim()) continue;
      const r = document.createRange(); r.selectNodeContents(n);
      for (const t of r.getClientRects()) if (t.width && (t.left < b.left + pad - .5 || t.right > b.right - pad + .5 || t.top < b.top + pad * .5 - .5 || t.bottom > b.bottom - pad * .5 + .5))
        out.push(n.textContent.trim().slice(0, 20));
    }
  });
  return out;
}, [card, pad]);

// SHOTS=1: the pictures in docs/shots; the home's widgets over one of our photos (the launcher draws the photo, the clock and the tray)
const shot = async (p, name) => {
  if (!process.env.SHOTS) return;
  if (name.startsWith('football-home')) await p.addStyleTag({ content: `html.fb body{background:url("file://${path.join(ROOT, 'assets/backgrounds/LAj90eAXOZA.jpg')}") center/cover}` });
  await p.waitForTimeout(400);
  await p.screenshot({ path: path.join(ROOT, 'docs/shots', name) });
};

// The geometry of one match line template (the home widget's .fln and the list's .mpg): the × (or the score) is the shared axis; the time is a column at the row's start;
// 24 px between a crest and its name, 32 px between the names and the ×; crests 40 px, centred with the text. Measured in the page, in px at the page's own unit.
const lineGeo = async (p, sel) => p.evaluate(sel => {
  const u = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--u')) || 1;
  const near = (a, b) => Math.min(Math.abs(a.left - b.right), Math.abs(b.left - a.right));
  const rtl = getComputedStyle(document.documentElement).direction === 'rtl';
  return { u, rtl, rows: [...document.querySelectorAll(sel)].filter(r => r.offsetParent).map(r => {
    const q = c => r.querySelector(c), R = e => e.getBoundingClientRect(), mid = R(q('.fx, .fsc')), w = R(q('.mlw')), rr = R(r);
    const hn = R(q('.mlh .nm')), an = R(q('.mla .nm')), hc = R(q('.mlh .cr, .mlh .crgap')), ac = R(q('.mla .cr, .mla .crgap'));
    return { axis: (mid.left + mid.right) / 2, midGapHome: near(hn, mid), midGapAway: near(an, mid), crHome: near(hc, hn), crAway: near(ac, an), crW: [hc.width, hc.height, ac.width, ac.height],
      timeEdge: rtl ? rr.right - w.right : w.left - rr.left, timeY: (w.top + w.bottom) / 2, midY: (mid.top + mid.bottom) / 2, hcY: (hc.top + hc.bottom) / 2, hnY: (hn.top + hn.bottom) / 2,
      homeToward: rtl ? hn.left - hc.left : hc.right - hn.right, rowH: rr.height, textInset: Math.min(...[...r.querySelectorAll('.nm,.mlw')].map(e => Math.min(R(e).left - rr.left, rr.right - R(e).right))) };
  }) };
}, sel);
const checkLineGeo = (label, g, sel) => {
  const u = g.u, same = (xs, tol = 1.5) => Math.max(...xs) - Math.min(...xs) <= tol;
  check(`${label}: rows found`, g.rows.length >= 2, g.rows.length);
  check(`${label}: the × (or the score) is one axis for every row`, same(g.rows.map(r => r.axis)), JSON.stringify(g.rows.map(r => r.axis)));
  check(`${label}: 32 px between the names and the ×, both sides`, g.rows.every(r => Math.abs(r.midGapHome - 32 * u) <= 1.5 && Math.abs(r.midGapAway - 32 * u) <= 1.5), JSON.stringify(g.rows.map(r => [r.midGapHome, r.midGapAway])));
  check(`${label}: 24 px between a crest and its name`, g.rows.every(r => Math.abs(r.crHome - 24 * u) <= 1.5 && Math.abs(r.crAway - 24 * u) <= 1.5), JSON.stringify(g.rows.map(r => [r.crHome, r.crAway])));
  check(`${label}: crests 40 px, the same on both sides`, g.rows.every(r => r.crW.every(x => Math.abs(x - 40 * u) <= 1)), JSON.stringify(g.rows.map(r => r.crW)));
  check(`${label}: the time is a column at the row's start (same edge in every row), centred with the line`, same(g.rows.map(r => r.timeEdge)) && g.rows.every(r => Math.abs(r.timeY - r.midY) <= 3 && Math.abs(r.hcY - r.hnY) <= 3), JSON.stringify(g.rows.map(r => [r.timeEdge, r.timeY, r.midY])));
};

// ---- the football home
{
  const sp = 'city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&teams=136013&widget=1&football=1';
  const { p, ctx, errors } = await open(sp, { rows: ROWS });
  await p.evaluate(() => fbTop(216));
  await p.waitForTimeout(200);
  check('football home: no script error', !errors.length, errors.join(' | '));
  const k = 1920 / 1920;
  for (const id of ['fb-wx', 'fb-pr', 'fb-mt']) {
    const b = await box(p, '#' + id);
    check(`football home: ${id} ends by y 572 (the favourites' name needs up to 728)`, b.b <= 572 * k + .5, JSON.stringify(b));
    check(`football home: ${id} inside the safe sides (120 to 1800)`, b.l >= 120 - .5 && b.r <= 1800 + .5, JSON.stringify(b));
  }
  const wx = await box(p, '#fb-wx'), pr = await box(p, '#fb-pr'), mt = await box(p, '#fb-mt');
  check('football home: the cards 24 px apart, the games on the right (RTL)', Math.abs(pr.l - wx.r - 24) < 1 && Math.abs(mt.l - pr.r - 24) < 1 && mt.r > pr.r, JSON.stringify([wx, pr, mt]));
  check('football home: text at least 16 px inside each card', !(await crowded(p, '.fbw .fbc', 16)).length, (await crowded(p, '.fbw .fbc', 16)).join(' | '));
  {   // step 28 (the owner's screenshots): the prayer card's last line keeps 16 px from the card's bottom edge, the rows are 40 apart by 8, the games sit centred in their card
    const g = await p.evaluate(() => {
      const r = e => e.getBoundingClientRect(), card = r(document.getElementById('fb-pr-c')), n = r(document.querySelector('.fpn')), li = [...document.querySelectorAll('#fb-pr li')].map(r);
      const mt = r(document.getElementById('fb-mt-c')), ln = [...document.querySelectorAll('#fb-mt-c .fln')].map(r);
      return { below: card.bottom - n.bottom, rowH: li[0].height, step: li[1].top - li[0].top, above: ln[0].top - mt.top, under: mt.bottom - ln[ln.length - 1].bottom };
    });
    const u = 1;
    check('football home: the prayer card\'s last line is 16+ px from its bottom edge, rows 40 high on an 8 gap', g.below >= 16 * u - .5 && Math.abs(g.rowH - 40 * u) < 1 && Math.abs(g.step - 48 * u) < 1, JSON.stringify(g));
    check('football home: the games are centred in their card (the gap above the first = the gap under the last)', Math.abs(g.above - g.under) <= 2, JSON.stringify(g));
  }
  const lines = await p.$$eval('#fb-mt-c .fln', ls => ls.map(l => l.textContent));
  check('football home: three games, the followed team first, live with its score and minute', lines.length === 3 && /الهلال/.test(lines[0]) && /٢ - ١/.test(lines[0]) && /الدقيقة ٦٧/.test(lines[0]), lines.join(' / '));
  checkLineGeo('football home: the match line', await lineGeo(p, '#fb-mt-c .fln'));
  check('football home: a game during a prayer says which, in amber', await p.$$eval('#fb-mt-c .fnote', ns => ns.some(n => /المغرب/.test(n.textContent) && getComputedStyle(n).color !== 'rgb(255, 255, 255)')), lines.join(' / '));
  check('football home: the count and the source over the games', /TheSportsDB · ٤ مباريات/.test(await p.textContent('#fb-mt h3')), await p.textContent('#fb-mt h3'));
  check('football home: the weather (city, temperature, feels-like, humidity, wind)', await p.evaluate(() => { const t = document.getElementById('fb-wx').textContent; return /الرياض/.test(t) && /٢٨°/.test(t) && /٣٠°/.test(t) && /١٨/.test(t) && /١٢ كم\/س/.test(t); }), await p.textContent('#fb-wx'));
  check('football home: five prayers, the next one lit, its time left under them', await p.$$eval('#fb-pr li', l => l.length === 5 && l.filter(x => x.classList.contains('nx')).length === 1) && /بعد/.test(await p.textContent('.fpn')));
  await shot(p, 'football-home.png');
  // the focus on the games: the row opens under the prayers and ends by 824, its first card ringed; ◀ ▶ move along it
  const rects = await p.evaluate(() => fbFocus('mt'));
  const row = await box(p, '#fb-row');
  check('football home: focused, the games open into a row from y 640 to 824', Math.abs(row.t - 640) < 1 && row.b <= 824.5 && row.l >= 119.5 && row.r <= 1800.5, JSON.stringify(row));
  check('football home: fbRects names the open row for the launcher\'s focus target', /mt:120\.0,640\.0,1680\.0,184\.0/.test(rects), rects);
  check('football home: the row is under the prayers and the weather (nothing overlaps)', row.t >= Math.max(wx.b, pr.b) + 24, JSON.stringify([row, wx, pr]));
  check('football home: every card of the row carries the broadcast mark of a verified, whole, in-season row', await p.$$eval('#fb-row .fmk', m => m.length === 4 && m.every(x => x.textContent === 'ثمانية')));
  check('football home: the row\'s title says they are all on that channel', /كلها على ثمانية/.test(await p.textContent('#fb-row h3')));
  check('football home: text at least 16 px inside each game card', !(await crowded(p, '#fb-row .fcd', 16)).length, (await crowded(p, '#fb-row .fcd', 16)).join(' | '));
  check('football home: the first card is chosen, OK gives its game', await p.evaluate(() => document.querySelector('.fcd.sel').dataset.i === '0' && fbPick() === '1'));
  check('football home: ▶▶ moves on, false at the end', await p.evaluate(() => fbMove(1) && fbMove(1) && fbMove(1) && !fbMove(1) && fbPick() === '4' && fbMove(-1)));
  await shot(p, 'football-home-widgetfocus.png');
  await p.evaluate(() => fbFocus('pr'));
  check('football home: the focus back on the prayers closes the row', await p.evaluate(() => !document.documentElement.classList.contains('fb-open') && document.getElementById('fb-pr').classList.contains('f')));
  await p.evaluate(() => fbLight(true));
  check('football home: the light look turns the ink dark', await p.evaluate(() => getComputedStyle(document.getElementById('fb')).color === 'rgb(27, 31, 29)'));
  await ctx.close();
}
// no mark unless the row is high, whole, verified and in season (and none at all with the shipped table, which has no rows yet)
for (const [why, rows] of [['the shipped table', null], ['medium confidence', [{ ...ROWS[0], confidence: 'medium' }]], ['only some games', [{ ...ROWS[0], whole: false }]],
  ['not verified', [{ ...ROWS[0], verified: '' }]], ['its season over', [{ ...ROWS[0], until: '2025-26' }]], ['no source', [{ ...ROWS[0], source: '' }]]]) {
  const { p, ctx } = await open('teams=136013&widget=1&football=1', { rows: rows || [] });
  await p.evaluate(() => { fbFocus('mt'); fbRender(); });
  check(`marks: none for ${why}`, (await p.$$('.fmk')).length === 0);
  await ctx.close();
}

// ---- «المباريات»
{
  const { p, ctx, errors } = await open('city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&teams=136013&matchpage=1', { rows: ROWS, height: 1080 });
  await p.evaluate(() => mpRender());
  check('matches page: no script error', !errors.length, errors.join(' | '));
  const days = await p.$$eval('#mp-days li', l => l.map(x => x.textContent));
  check('matches page: yesterday, today, tomorrow and four more days, today chosen', days.length === 7 && days[0] === 'أمس' && days[1] === 'اليوم' && days[2] === 'غدًا'
    && await p.$eval('#mp-days li.on', x => x.textContent === 'اليوم'), days.join(','));
  const side = await p.$$eval('#mp-side li', l => l.map(x => x.textContent));
  check('matches page: «فرقك» first in the leagues, each with its count', side[0].startsWith('فرقك') && side[0].endsWith('١') && /دوري روشن/.test(side[1]) && side[1].endsWith('٤'), side.join(','));
  const heads = await p.$$eval('#mp-list h2', h => h.map(x => x.textContent));
  check('matches page: the followed team first, then the league with its round', heads[0] === 'فرقك' && heads[1] === 'دوري روشن، الجولة ١٣', heads.join(','));
  check('matches page: a live game shows its minute and score', /الدقيقة ٦٧/.test(await p.textContent('#mp-list section:first-child')) && /٢ - ١/.test(await p.textContent('#mp-list section:first-child')));
  check('matches page: text at least 16 px inside the rows and the leagues', !(await crowded(p, '#mp-list ul, #mp-side', 16)).length, (await crowded(p, '#mp-list ul, #mp-side', 16)).join(' | '));
  check('matches page: the mark on every row of the league', await p.$$eval('#mp-list .mpg .fmk', m => m.length === 4));
  const lg = await lineGeo(p, '#mp-list .mpg');
  checkLineGeo('matches page: the match line', lg);
  check('matches page: a row is at least 88 (the 8 px scale) and a one-line row exactly 88; its text 16 px or more from the row edges', lg.rows.every(r => r.rowH >= 88 * lg.u - .5 && r.textInset >= 16 * lg.u - .5) && lg.rows.some(r => Math.abs(r.rowH - 88 * lg.u) < 1), JSON.stringify(lg.rows.map(r => [r.rowH, r.textInset])));
  await shot(p, 'football-matches-page.png');
  await p.keyboard.press('ArrowDown');
  await p.keyboard.press('ArrowDown');
  check('matches page: ▼ walks the rows', await p.$eval('.mpg.f', x => /الأهلي/.test(x.textContent)));
  await p.keyboard.press('Enter');
  const card = await p.textContent('#mp-detail');
  check('matches page: OK opens the game\'s card: the round, the venue, the channel, the prayers', /دوري روشن، الجولة ١٣/.test(card) && /King Abdullah Sports City/.test(card)
    && /ثمانية، أو تطبيق ثمانية/.test(card) && /مع الصلاة/.test(card) && /تبدأ بعد العشاء/.test(card), card);
  check('matches page: the card\'s two buttons: remind and follow', await p.$$eval('.mdb button', b => b.length === 2 && /ذكّرني قبلها بنصف ساعة/.test(b[0].textContent) && /أضف الأهلي لفرقي/.test(b[1].textContent)));
  check('matches page: the game a green bar on the prayers\' line, the prayers marked', await p.$$eval('.mpline .mpk', k => k.length >= 1) && (await p.$$('.mpbar')).length === 1);
  await p.evaluate(() => { window.UrtvMatches = { remind: (t, at) => (window.__rem = [t, at], true), follow: (id, n) => (window.__fol = [id, n], true) }; });
  check('matches page: the card opens with a visible focus: the card itself is ringed and no button is', await p.$$eval('#mp-detail .mdx.f', c => c.length === 1) && await p.$$eval('.mdb button.f', b => b.length === 0));
  check('matches page: the card\'s buttons cannot take the browser\'s own focus (tabindex -1), so there is one focus: the drawn ring', await p.$$eval('.mdb button', b => b.every(x => x.getAttribute('tabindex') === '-1')));
  await p.keyboard.press('Enter');   // the card just opened: no button has the focus, OK adds nothing
  check('matches page: OK on the freshly opened card adds no reminder and follows no team', await p.evaluate(() => window.__rem === undefined && window.__fol === undefined));
  await p.keyboard.press('ArrowLeft');   // ◀ is forward in Arabic: the focus goes to «ذكّرني»
  check('matches page: after ◀ exactly one thing is ringed, «ذكّرني», and the card is not', await p.$$eval('.mdb button.f', b => b.length === 1 && b[0].dataset.k === 'remind') && await p.$$eval('#mp-detail .mdx.f', c => c.length === 0));
  await p.evaluate(() => document.querySelector('.mdb button[data-k="follow"]').focus());   // the browser\'s own focus elsewhere must not change what OK does
  await p.keyboard.press('Enter');
  const rem = await p.evaluate(() => window.__rem);
  check('matches page: «ذكّرني» hands the launcher the task and the kick-off itself (the app reminds 30 minutes ahead)', rem && /الأهلي × التعاون/.test(rem[0]) && rem[1] === Date.parse('2026-11-04T17:00:00Z'), JSON.stringify(rem));
  await p.keyboard.press('ArrowLeft');
  await p.keyboard.press('Enter');
  check('matches page: OK acts on exactly the ringed button: after ◀ again the ring is on «أضف … لفرقي» alone', await p.$$eval('.mdb button.f', b => b.length === 1 && b[0].dataset.k === 'follow'));
  check('matches page: «أضف … لفرقي» hands it the club\'s id and name', JSON.stringify(await p.evaluate(() => window.__fol)) === '["137721","Al-Ahli"]');
  await shot(p, 'football-matches-detail.png');
  // a live game's "with prayer" line says the minute, not «تبدأ بعد العشاء…»; the hyphen of a Latin club name does not break
  const liveLine = await p.evaluate(() => { const ev = { ...MP.detail, strStatus: '2H', strProgress: '67', intHomeScore: '1', intAwayScore: '0', dateEvent: MP.detail.dateEvent, strTimestamp: new Date(Date.now() - 67 * 60000).toISOString().slice(0, 19) };
    const keep = MP.detail; MP.detail = ev; mpDetailRender(); const t = document.querySelector('.mds').textContent; MP.detail = keep; mpDetailRender(); return t; });
  check('matches page: a live game reads its minute on the prayer line, not «تبدأ بعد…»', /الدقيقة/.test(liveLine) && !/تبدأ/.test(liveLine), liveLine);
  check('matches page: a Latin club name is kept whole at its hyphen (non-breaking hyphen)', await p.evaluate(() => nbName('Shabab Al-Ahli Dubai') === 'Shabab Al\u2011Ahli Dubai' && nbName('Al-Hilal') === 'Al\u2011Hilal' && nbName('الأهلي') === 'الأهلي'));
  // «شاهد على ثمانية»: only with a verified row for the competition AND the broadcaster's app installed; first, ringed by default; OK opens the app and adds nothing else
  const watchCase = async (installed, ev) => p.evaluate(([installed, ev]) => {
    window.__open = []; window.__rem = undefined; window.__fol = undefined; window.__unf = undefined;
    window.UrtvMatches = { appOf: csv => installed ? csv.split(',')[0] : '', open: csv => (window.__open.push(csv), true), remind: (t, at) => (window.__rem = [t, at], true), follow: (id, n) => (window.__fol = [id, n], true), unfollow: id => (window.__unf = id, true) };
    mpOpen(ev || MP.detail);
    return { ks: [...document.querySelectorAll('.mdb button')].map(b => b.dataset.k), texts: [...document.querySelectorAll('.mdb button')].map(b => b.textContent.trim()), ringed: [...document.querySelectorAll('.mdb button.f')].map(b => b.dataset.k), card: document.querySelectorAll('#mp-detail .mdx.f').length };
  }, [installed, ev || null]);
  const w1 = await watchCase(true);
  check('matches page: with the verified row and the app installed: «شاهد على ثمانية» first, then remind, then «أضف … لفرقي»; it is the ringed one', JSON.stringify(w1.ks) === '["watch","remind","follow"]' && w1.texts[0] === 'شاهد على ثمانية' && JSON.stringify(w1.ringed) === '["watch"]' && w1.card === 0, JSON.stringify(w1));
  await p.keyboard.press('Enter');
  check('matches page: OK on «شاهد على …» opens the app (the TV build first) and adds no reminder and follows no team', await p.evaluate(() => JSON.stringify(window.__open) === '["com.thmanyah.shasha.tv,com.thmanyah.shasha"]' && window.__rem === undefined && window.__fol === undefined));
  const w2 = await watchCase(false);
  check('matches page: the app not installed: no «شاهد على …» button (hidden), the card ringed, remind and follow as before', JSON.stringify(w2.ks) === '["remind","follow"]' && w2.card === 1 && w2.ringed.length === 0, JSON.stringify(w2));
  const w3 = await p.evaluate(() => { const ev = { ...MP.detail, idLeague: '4328' }; window.UrtvMatches = { appOf: () => 'x', open: () => true, follow: () => true, remind: () => true, unfollow: () => true }; mpOpen(ev); return [...document.querySelectorAll('.mdb button')].map(b => b.dataset.k); });
  check('matches page: a competition with no verified row (the Premier League here) never shows «شاهد على …», installed or not', JSON.stringify(w3) === '["remind","follow"]', JSON.stringify(w3));
  const w4 = await p.evaluate(() => { CONFIG.teamIds.push(String(MP.detail.idHomeTeam), String(MP.detail.idAwayTeam)); window.UrtvMatches = { appOf: () => '', unfollow: id => (window.__unf = id, true), remind: () => true, follow: () => true, open: () => true }; mpOpen(MP.detail); return [...document.querySelectorAll('.mdb button')].map(b => [b.dataset.k, b.textContent.trim()]); });
  check('matches page: both teams already followed: «أزل … من فرقي» instead of the add button', w4.length === 2 && w4[1][0] === 'unfollow' && /^أزل .+ من فرقي$/.test(w4[1][1]), JSON.stringify(w4));
  await p.keyboard.press('ArrowLeft'); await p.keyboard.press('ArrowLeft'); await p.keyboard.press('Enter');
  check('matches page: OK on the ringed «أزل … من فرقي» removes exactly that team (nothing else)', await p.evaluate(() => window.__unf === String(MP.detail.idHomeTeam) && window.__fol === undefined), '');
  await p.evaluate(() => { CONFIG.teamIds = CONFIG.teamIds.filter(x => x !== String(MP.detail.idAwayTeam) && x !== String(MP.detail.idHomeTeam)); delete window.UrtvMatches; });
  check('matches page: Back closes the card first, then leaves', await p.evaluate(() => mpBack() === 'closed' && mpBack() === 'out'));
  await p.keyboard.press('ArrowUp'); await p.keyboard.press('ArrowUp'); await p.keyboard.press('ArrowUp');
  await p.keyboard.press('ArrowLeft');
  check('matches page: ◀ on the days shows tomorrow\'s games', await p.$eval('#mp-days li.on', x => x.textContent === 'غدًا') && await p.$$eval('#mp-list .mpg', r => r.length === 1 && r[0].querySelector('.mpw').textContent === '٩:٠٠ م'));
  await ctx.close();
}
// English: the same page laid out left to right, none of its own words in Arabic
{
  const { p, ctx, errors } = await open('teams=136013&matchpage=1&lang=en', { file: 'tv-en.html', rows: ROWS, height: 1080 });
  await p.evaluate(() => mpRender());
  const own = await p.evaluate(() => [...document.querySelectorAll('#mp-days li, #mp-list h2, #mp h1, #mp-side li span')].map(x => x.textContent).filter(t => /[؀-ۿ]/.test(t) && !/الجولة/.test(t)));
  checkLineGeo('English matches page: the match line (left to right)', await lineGeo(p, '#mp-list .mpg'));
  check('English matches page: no script error, its own words in English', !errors.length && !own.length, errors.join(' | ') + own.join(','));
  await ctx.close();
}
await browser.close();
console.log(failed ? `${failed} failed` : 'all football page tests passed');
process.exit(failed ? 1 : 0);
