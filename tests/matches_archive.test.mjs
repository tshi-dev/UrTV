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
const team = (id, en, ar, next, last) => ({ v: 1, id, en, ar, updated: '2026-11-04T00:00:00Z', next, last });
// the server's free answer: Al-Hilal's next home game only; nothing for the two others
const SERVER_NEXT = { '136013': [ev('11', 'Al-Hilal', '136013', 'Al-Fateh', '136011', '2026-11-04T17:00:00')] };
const ARCHIVE = {
  'index.json': { v: 1, updated: '2026-11-04T00:00:00Z', teams: { '136013': { en: 'Al-Hilal', ar: 'الهلال', leagues: ['4668'] }, '136014': { en: 'Al-Nassr', ar: 'النصر', leagues: ['4668'] },
    '136015': { en: 'Al-Ittihad', ar: 'الاتحاد', leagues: ['4668'] }, '136017': { en: 'Al-Shabab', ar: 'الشباب', leagues: ['4668'] }, '136016': { en: 'Al-Qadsiah', ar: 'القادسية', leagues: ['4668'] } },
    leagues: { '4668': { en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي' }, '4328': { en: 'English Premier League', ar: 'الدوري الإنجليزي' } } },
  'teams/136013.json': team('136013', 'Al-Hilal', 'الهلال', [g('11', '2026-11-04T17:00:00Z', true, '136011', 'Al-Fateh', null, null)], []),
  'teams/136014.json': team('136014', 'Al-Nassr', 'النصر', [g('12', '2026-11-04T18:00:00Z', false, '136016', 'Al-Qadsiah', null, null)], []),
  'teams/136015.json': team('136015', 'Al-Ittihad', 'الاتحاد', [g('13', '2026-11-05T18:00:00Z', false, '136017', 'Al-Shabab', null, null)], []),
};

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const json = (r, j) => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
async function open(hash, { archive = ARCHIVE } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    const a = /\/data-archive\/(.+)$/.exec(u);
    if (a) { asked.push(a[1]); return archive[a[1]] ? json(r, archive[a[1]]) : r.fulfill({ status: 404, body: '' }); }
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

await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all match archive tests passed');
