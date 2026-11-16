// «المباريات» (#matchpage=1) with the match archive (docs/MATCH-ARCHIVE.md, branch data-archive), in a real browser, offline, on fixed data:
// A3 three followed teams all reach the page (the server's free answer holds one, the archive's team files the others)
//   node tests/matches_archive.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');   // 7:13 in Riyadh, a Wednesday
const ev = (id, home, hid, away, aid, iso, extra = {}) => ({ idEvent: id, idLeague: '4668', idHomeTeam: hid, idAwayTeam: aid, strHomeTeam: home,
  strAwayTeam: away, strStatus: 'NS', strTimestamp: iso, dateEvent: iso.slice(0, 10), strTime: iso.slice(11), strSport: 'Soccer', strSeason: '2026-2027', intRound: '13', ...extra });
const g = (id, t, home, vs, vsn, gf, ga, r = '13', lg = '4668') => ({ id, lg, s: '2026-2027', t, home, vs, vsn, gf, ga, st: gf == null ? 'NS' : 'FT', r });
const m = (id, t, h, hn, a, an, hs, as, lg = '4668') => ({ id, lg, s: '2026-2027', t, r: '12', h, a, hn, an, hs, as, st: 'FT' });
const team = (id, en, ar, next, last) => ({ v: 1, id, en, ar, updated: '2026-11-04T00:00:00Z', next, last });
// the server's free answer: Al-Hilal's next home game only; nothing for the two others
const SERVER_NEXT = { '136013': [ev('11', 'Al-Hilal', '136013', 'Al-Fateh', '136011', '2026-11-04T17:00:00')] };
const ARCHIVE = {
  'index.json': { v: 1, updated: '2026-11-04T00:00:00Z', teams: { '136013': { en: 'Al-Hilal', ar: 'الهلال', leagues: ['4668'] }, '136014': { en: 'Al-Nassr', ar: 'النصر', leagues: ['4668'] },
    '136015': { en: 'Al-Ittihad', ar: 'الاتحاد', leagues: ['4668'] }, '136017': { en: 'Al-Shabab', ar: 'الشباب', leagues: ['4668'] }, '136016': { en: 'Al-Qadsiah', ar: 'القادسية', leagues: ['4668'] } },
    leagues: { '4668': { en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي' }, '4328': { en: 'English Premier League', ar: 'الدوري الإنجليزي' } } },
  'teams/136013.json': team('136013', 'Al-Hilal', 'الهلال', [g('11', '2026-11-04T17:00:00Z', true, '136011', 'Al-Fateh', null, null), g('14', '2026-11-08T17:00:00Z', false, '136017', 'Al-Shabab', null, null, '14'),
    g('15', '2026-11-26T17:00:00Z', false, '4999', 'Cup Side', null, null, '', '5649')], [g('23', '2026-11-02T18:00:00Z', true, '136011', 'Al-Fateh', '3', '0', '12'), g('d:2026-10-20:136013:136016', '2026-10-20T15:00:00Z', true, '136016', 'Al-Qadsiah', '1', '1', '')]),
  'h2h/136011-136013.json': { v: 1, pair: '136011-136013', updated: '2026-11-04T00:00:00Z', meetings: [m('23', '2026-11-02T18:00:00Z', '136013', 'Al-Hilal', '136011', 'Al-Fateh', '3', '0'),
    m('d:2025-03-01:136011:136013', '2025-03-01T15:00:00Z', '136011', 'Al-Fateh', '136013', 'Al-Hilal', '0', '2')] },
  'teams/136014.json': team('136014', 'Al-Nassr', 'النصر', [g('12', '2026-11-04T18:00:00Z', false, '136016', 'Al-Qadsiah', null, null)], []),
  'teams/136015.json': team('136015', 'Al-Ittihad', 'الاتحاد', [g('13', '2026-11-05T18:00:00Z', false, '136017', 'Al-Shabab', null, null)], []),
  'results/4668.json': { v: 1, id: '4668', en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي', updated: '2026-11-04T00:00:00Z', results: [
    m('21', '2026-11-03T17:00:00Z', '136015', 'Al-Ittihad', '136017', 'Al-Shabab', '2', '0'), m('22', '2026-11-03T15:00:00Z', '136016', 'Al-Qadsiah', '136020', 'Al-Ettifaq', '1', '1'),
    m('23', '2026-11-02T18:00:00Z', '136013', 'Al-Hilal', '136011', 'Al-Fateh', '3', '0')] },
  'results/4328.json': { v: 1, id: '4328', en: 'English Premier League', ar: 'الدوري الإنجليزي', updated: '2026-11-04T00:00:00Z', results: [
    m('31', '2026-11-03T20:00:00Z', '133604', 'Arsenal', '133610', 'Chelsea', '1', '2', '4328')] },
  'results/4335.json': { v: 2, results: [m('41', '2026-11-03T20:00:00Z', '133739', 'Barcelona', '133738', 'Real Madrid', '9', '9', '4335')] },   // a format this page does not know: nothing from it
};

const LEAGUES = { 'english-premier-league': { league: 'English Premier League', id: '4328', updated: '2026-11-04T04:00:00Z', events: [
  ev('51', 'Arsenal', '133604', 'Chelsea', '133610', '2026-11-07T15:00:00', { idLeague: '4328', strLeague: 'English Premier League', intRound: '11' }),
  ev('52', 'Everton', '133615', 'Fulham', '133600', '2026-11-08T15:00:00', { idLeague: '4328', strLeague: 'English Premier League', intRound: '11' })] } };
const TABLE = { table: [{ idTeam: '133604', strTeam: 'Arsenal', intRank: '1', intPoints: '25' }, { idTeam: '133610', strTeam: 'Chelsea', intRank: '2', intPoints: '22' }] };

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const json = (r, j) => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
async function open(hash, { archive = ARCHIVE } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    const a = /\/data-archive\/(.+)$/.exec(u);
    if (a) { asked.push(a[1]); return archive[a[1]] ? json(r, archive[a[1]]) : r.fulfill({ status: 404, body: '' }); }
    const l = /\/data-leagues\/leagues\/(.+)\.json$/.exec(u);
    if (l) return LEAGUES[l[1]] ? json(r, LEAGUES[l[1]]) : r.fulfill({ status: 404, body: '' });
    if (/lookuptable\.php\?l=4328&s=2026-2027$/.test(u)) { asked.push('table'); return json(r, TABLE); }
    const n = /eventsnext\.php\?id=(\d+)/.exec(u);
    if (n) return json(r, { events: SERVER_NEXT[n[1]] || null });
    if (u.includes('eventslast.php')) return json(r, { results: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&' + hash + '&gap=0');
  await p.waitForTimeout(1200);
  return { p, ctx, errors, asked };
}
const section = (p, key) => p.$$eval(`#mp-list section[data-g="${key}"] li`, ls => ls.map(l => l.textContent));
const ids = (p, key) => p.evaluate(k => ((MP.leagues.find(x => x.key === k) || {}).games || []).map(e => e.idEvent).join(), key);   // the games of a section of the list

// ---- A3: three followed teams, every one on the page
{
  const { p, ctx, errors, asked } = await open('teams=136013,136014,136015&matchpage=1');
  check('A3: no script error', !errors.length, errors.join(' | '));
  check('A3: the page has the three teams', await p.evaluate(() => CONFIG.teamIds.join()) === '136013,136014,136015');
  check('A3: each followed team\'s archive file is asked for', ['teams/136013.json', 'teams/136014.json', 'teams/136015.json'].every(f => asked.includes(f)), asked.join());
  const mine = await ids(p, 'mine');
  check('A3: today «فرقك» has the server\'s game of the first team and the archive\'s away game of the second', mine === '11,12', mine);
  check('A3: …drawn as two rows of the section', (await section(p, 'mine')).length === 2);
  await p.evaluate(() => { MP.day = fbDayAdd(cityToday(), 1); MP.picked = true; mpRender(); });
  const tomorrow = await ids(p, 'mine');
  check('A3: tomorrow «فرقك» has the third team\'s game (the archive alone has it)', tomorrow === '13', tomorrow);
  check('A3: the archive\'s game is not twice (Al-Hilal\'s is the server\'s and the archive\'s)', await p.evaluate(() => fbGames().filter(e => e.idEvent === '11').length) === 1);
  check('A3: the teams\' archive games are kept for a quick reopen', await p.evaluate(() => Object.keys(JSON.parse(localStorage.getItem('archTeams'))).sort().join()) === '136013,136014,136015');
  await ctx.close();
}
{   // the archive is down (every file 404): the page is as before, the server's game alone, no error
  const { p, ctx, errors } = await open('teams=136013,136014,136015&matchpage=1', { archive: {} });
  const mine = await ids(p, 'mine');
  check('A3: with no archive the server\'s game is there and nothing breaks', !errors.length && mine === '11', errors.join(' | ') + mine);
  await ctx.close();
}

// ---- G2: yesterday's results by themselves: the followed teams' first, then the followed leagues', then the top leagues'
{
  const { p, ctx, errors, asked } = await open('teams=136015&leagues=English Premier League&matchpage=1');
  await p.evaluate(() => { MP.day = fbDayAdd(cityToday(), -1); MP.picked = true; mpRender(); });
  await p.waitForTimeout(500);
  check('G2: no script error', !errors.length, errors.join(' | '));
  check('G2: the followed league\'s and the top leagues\' results files are asked', ['results/4328.json', 'results/4668.json'].every(f => asked.includes(f)), asked.join());
  const order = await p.evaluate(() => MP.leagues.filter(g => !g.pin).map(g => g.key + ':' + g.games.map(e => e.idEvent).join('+')).join(' '));
  check('G2: «فرقك» first (the followed team\'s result), then the followed league, then the top league; only that day, only results', order === 'mine:21 4328:31 4668:22', order);
  check('G2: a result shows its score', /٢ - ٠/.test(await p.textContent('#mp-list section[data-g="mine"]')), await p.textContent('#mp-list section[data-g="mine"]'));
  check('G2: a file of an unknown format gives nothing', !(await p.evaluate(() => MP.rows.some(e => e.idEvent === '41'))));
  await p.evaluate(() => { MP.day = cityToday(); mpRender(); });
  check('G2: today shows no result of yesterday', !(await p.evaluate(() => MP.rows.some(e => ['21', '22', '31'].includes(e.idEvent)))));
  await ctx.close();
}

// ---- A2: a club's page from a game's card: its coming games by round, its last results, its meetings with the other club
{
  const { p, ctx, errors } = await open('teams=136013&matchpage=1');
  await p.evaluate(() => { const ev = fbGames().find(e => e.idEvent === '11'); mpOpen(ev); });
  const btns = await p.$$eval('#mp-detail .mdq button', bs => bs.map(b => b.dataset.k));
  check('A2: a game\'s card has «صفحة الفريقين» beside «رجوع» (the actions\' row as it was)', btns.join() === 'back,club', btns.join());
  await p.keyboard.press('ArrowDown'); await p.keyboard.press('ArrowLeft');
  check('A2: ▼ then ◀ rings «صفحة الفريقين» alone', await p.$$eval('#mp-detail button.f', b => b.length === 1 && b[0].dataset.k === 'club'));
  await p.keyboard.press('Enter');
  await p.waitForTimeout(500);
  check('A2: no script error', !errors.length, errors.join(' | '));
  check('A2: the card closed, the club\'s page is open with its name', await p.evaluate(() => !MP.detail && MP.view && MP.view.k === 'club') && /الهلال/.test(await p.textContent('#mp-h')), await p.textContent('#mp-h'));
  const secs = await p.evaluate(() => MP.leagues.map(g => g.name + ':' + g.games.map(e => e.idEvent).join('+')));
  check('A2: the coming games one section per round (the competition and its round), then the last results, then the meetings', secs.length === 5 && /الجولة ١٣:11$/.test(secs[0]) && /الجولة ١٤:14$/.test(secs[1]) && /:15$/.test(secs[2])
    && secs[3] === 'آخر النتائج:23+d:2026-10-20:136013:136016' && /^المواجهات مع .+:23\+d:2025-03-01:136011:136013$/.test(secs[4]), secs.join(' | '));
  check('A2: a result in the club\'s page says its day, not «انتهت»', /أمس|الإثنين|٢ نوفمبر/.test(await p.textContent('#mp-list section[data-g="last"] li')), await p.textContent('#mp-list section[data-g="last"] li'));
  const tabs = await p.$$eval('#mp-days li', ls => ls.map(l => l.textContent));
  check('A2: the strip: follow or unfollow the club, and the other club\'s page', tabs.length === 2 && /^ألغِ متابعة الهلال$/.test(tabs[0]) && /^صفحة /.test(tabs[1]), tabs.join(' / '));
  await p.evaluate(() => { MP.zone = 'days'; MP.d = 1; mpRender(); });
  await p.keyboard.press('Enter');
  check('A2: «صفحة …» swaps to the other club, the meetings still there', await p.evaluate(() => MP.view.id === '136011' && MP.view.vs === '136013' && MP.leagues.some(g => g.key === 'h2h')));
  await p.evaluate(() => { MP.zone = 'rows'; MP.r = 0; mpRender(); });
  await p.keyboard.press('Enter');
  check('A2: OK on a game of the club\'s page opens its card', await p.evaluate(() => !!MP.detail));
  check('A2: Back closes the card, then the club\'s page, back to the day as it was', await p.evaluate(() => [mpBack(), !!MP.view, mpBack(), MP.view, mpBack()].join()) === 'closed,true,closed,,out');
  await ctx.close();
}
{   // a club with no archive file: its page still opens, with what the page has and «لا شيء هنا بعد»
  const { p, ctx, errors } = await open('teams=136014&matchpage=1', { archive: {} });
  await p.evaluate(() => mpGo({k: 'club', id: '136014'}));
  await p.waitForTimeout(400);
  const none = await p.$$eval('#mp-list .fnone', ns => ns.map(n => n.textContent));
  check('A2: no file, no error: the empty lists say so', !errors.length && none.length === 2 && none.every(t => t === 'لا شيء هنا بعد'), errors.join(' | ') + none.join(' / '));
  await ctx.close();
}

// ---- the followed leagues pinned on top of the side list: OK opens the league (its table, its results, its games)
{
  const { p, ctx, errors, asked } = await open('teams=136013&leagues=English Premier League&matchpage=1');
  const side = await p.$$eval('#mp-side li', ls => ls.map(l => (l.classList.contains('pin') ? 'pin:' : '') + l.querySelector('span').textContent));
  check('pins: the followed league is first in the side list, marked, before the day\'s sections', side[0] === 'pin:الدوري الإنجليزي' && side.slice(1).every(x => !x.startsWith('pin:')) && side.length > 1, side.join(' / '));
  check('pins: the lit side row is the focused game\'s section, not the pin', await p.$$eval('#mp-side li.on', ls => ls.length === 1 && !ls[0].classList.contains('pin')));
  await p.evaluate(() => { MP.zone = 'leagues'; MP.l = 0; mpRender(); });
  await p.keyboard.press('Enter');
  await p.waitForTimeout(400);
  check('pins: no script error', !errors.length, errors.join(' | '));
  check('pins: OK on the pin opens the league, its name on top, three tabs (table, results, games)', await p.evaluate(() => MP.view && MP.view.k === 'league' && MP.view.id === '4328')
    && await p.textContent('#mp-h') === 'الدوري الإنجليزي' && (await p.$$eval('#mp-days li', ls => ls.map(l => l.textContent).join())) === 'جدول الترتيب,النتائج,المباريات');
  check('pins: the table asks TheSportsDB once for the season the league\'s games say', asked.filter(a => a === 'table').length === 1, asked.join());
  const table = await p.$$eval('#mp-list li', ls => ls.map(l => l.textContent));
  check('pins: the table\'s rows: place, club, points', table.length === 2 && /^١\. .+٢٥ نقطة$/.test(table[0]), table.join(' / '));
  await p.evaluate(() => { MP.zone = 'rows'; MP.r = 1; mpRender(); });
  await p.keyboard.press('Enter');
  check('pins: OK on a club of the table opens its page', await p.evaluate(() => MP.view.k === 'club' && MP.view.id === '133610'));
  await p.evaluate(() => mpBack());
  await p.evaluate(() => { MP.zone = 'days'; MP.d = 1; mpRender(); });
  await p.keyboard.press('Enter');
  const res = await p.evaluate(() => MP.leagues.map(g => g.games.map(e => e.idEvent).join('+')).join(' '));
  check('pins: «النتائج» the league\'s last results (the archive), by day', res === '31', res);
  await p.evaluate(() => { MP.zone = 'days'; MP.d = 2; mpRender(); });
  await p.keyboard.press('Enter');
  const fx = await p.evaluate(() => MP.leagues.map(g => g.name + ':' + g.games.map(e => e.idEvent).join('+')).join(' | '));
  check('pins: «المباريات» its coming games by round', /الجولة ١١:51\+52$/.test(fx), fx);
  check('pins: Back to the day', await p.evaluate(() => [mpBack(), MP.view].join()) === 'closed,');
  await ctx.close();
}
{   // the table not answered (no season known, nothing asked): «الترتيب غير متاح الآن», no error
  const { p, ctx, errors } = await open('matchpage=1', { archive: {} });
  await p.evaluate(() => mpGo({k: 'league', id: '4339'}));
  await p.waitForTimeout(300);
  check('pins: a league with no table says so', !errors.length && await p.textContent('#mp-list .fnone') === 'الترتيب غير متاح الآن', errors.join(' | ') + await p.textContent('#mp-list'));
  await ctx.close();
}

await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all match archive tests passed');
