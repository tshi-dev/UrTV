// 4.4 beta 2 «رياضي»: SPORTS.tops(n), the top n of every followed league from the files the TV already has (the archive's sports/<id>.json,
// else TheSportsDB's table of the season the page's games say); a league with neither, or a cup, is left out; ready is false while a file is
// still asked. Offline in a real browser.
//   node tests/sport_tops.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');
const row = (id, n, ar, p, pts) => ({ id, n, ar, p, w: 0, d: 0, l: 0, gf: 0, ga: 0, pts, f: '' });
const SAUDI = { v: 1, id: '4668', season: '2026-2027', updated: '2026-11-03T00:00:00Z', table: [row('136013', 'Al-Hilal', 'الهلال', 10, 26), row('136014', 'Al-Nassr', 'النصر', 10, 22),
  row('136015', 'Al-Ittihad', 'الاتحاد', 9, 19), row('136016', 'Al-Ahli', 'الأهلي', 10, 18)], last: [{ id: 'x', t: '2026-11-02T18:00:00Z' }], next: [] };
const TSDB = { table: [{ idTeam: '133610', strTeam: 'Chelsea', intRank: '2', intPoints: '27', intPlayed: '11', dateUpdated: '2026-11-03' },
  { idTeam: '133604', strTeam: 'Arsenal', intRank: '1', intPoints: '30', intPlayed: '11', dateUpdated: '2026-11-03' },
  { idTeam: '133602', strTeam: 'Liverpool', intRank: '3', intPoints: '25', intPlayed: '11', dateUpdated: '2026-11-03' },
  { idTeam: '133613', strTeam: 'Man City', intRank: '4', intPoints: '24', intPlayed: '11', dateUpdated: '2026-11-03' }] };
const ev = (lg, season) => ({ idEvent: 'e' + lg, idLeague: lg, strSeason: season, idHomeTeam: lg + '1', idAwayTeam: lg + '2', strHomeTeam: 'A' + lg, strAwayTeam: 'B' + lg, strStatus: 'NS',   // each its own clubs: the page keeps one game per two clubs and kick-off
  strTimestamp: '2026-11-08T15:00:00', dateEvent: '2026-11-08', strTime: '15:00:00', intRound: '12', strSport: 'Soccer' });

const browser = await chromium.launch();
async function open(files, slowTsdb = false) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, async r => {
    const u = r.request().url();
    const ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const js = /\/master\/dist\/(sports(?:-en)?\.js)$/.exec(u);
    if (js) return r.fulfill({ path: path.join(ROOT, 'dist', js[1]), headers: { 'Access-Control-Allow-Origin': '*' } });
    const a = /\/(?:data-archive|football\/archive)\/(.+)$/.exec(u);
    if (a) return files[a[1]] ? ok(files[a[1]]) : r.fulfill({ status: 404, body: '' });
    if (/lookuptable\.php\?l=4328/.test(u)) { if (slowTsdb) await new Promise(res => setTimeout(res, 20000)); return ok(TSDB); }
    if (/lookuptable\.php/.test(u)) return ok({ table: null });
    if (/thesportsdb/.test(u)) return ok({ events: null, results: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&matchpage=1');
  await p.waitForTimeout(1200);
  // four followed leagues: one with the archive's table, one with TheSportsDB's only, a cup, one with nothing
  await p.evaluate(([games]) => {
    CONFIG.leagues = ['Saudi', 'EPL', 'UCL', 'Nothing'];
    LEAGUE_DATA.Saudi = { updated: new Date().toISOString(), events: [games[0]] };
    LEAGUE_DATA.EPL = { updated: new Date().toISOString(), events: [games[1]] };
    LEAGUE_DATA.UCL = { updated: new Date().toISOString(), events: [games[2]] };
    LEAGUE_DATA.Nothing = { updated: new Date().toISOString(), events: [games[3]] };
    MATCH_REV++;
  }, [[ev('4668', '2026-2027'), ev('4328', '2026-2027'), ev('4480', '2026-2027'), ev('4999', '2026-2027')]]);
  return { p, ctx, errors };
}
const FILES = { 'sports/4668.json': SAUDI, 'sports/4480.json': { v: 1, id: '4480', cup: { rounds: [] }, table: [] } };
{
  const { p, ctx, errors } = await open(FILES);
  await p.evaluate(() => SPORTS.tops(3));
  await p.waitForFunction(() => SPORTS.tops(3).ready, null, { timeout: 15000 }).catch(() => {});   // TheSportsDB's asks are paced by the page
  const r = await p.evaluate(() => SPORTS.tops(3));
  check('ready once every file has answered', r.ready && !errors.length, JSON.stringify(r).slice(0, 200) + errors.join('|'));
  check('the followed leagues in their order, a cup and a league with no table left out', JSON.stringify(r.leagues.map(l => l.id)) === '["4668","4328"]', JSON.stringify(r.leagues.map(l => l.id)));
  const s = r.leagues[0];
  check('the archive\'s table: its top 3, the Arabic names, points and games played, the round (most played) and the date of its last result',
    s.src === 'archive' && JSON.stringify(s.rows) === JSON.stringify([{ rank: 1, id: '136013', name: 'الهلال', pts: 26, p: 10 }, { rank: 2, id: '136014', name: 'النصر', pts: 22, p: 10 },
      { rank: 3, id: '136015', name: 'الاتحاد', pts: 19, p: 9 }]) && s.round === 10 && s.asOf === '2026-11-02T18:00:00Z' && s.season === '2026-2027' && s.name, JSON.stringify(s));
  const e = r.leagues[1];
  check('no archive table: TheSportsDB\'s, by its rank (not its order), top 3, its date', e.src === 'thesportsdb' && e.rows.map(x => x.id).join() === '133604,133610,133602'
    && e.rows[0].pts === 30 && e.rows[0].p === 11 && e.asOf === '2026-11-03' && e.round === 11, JSON.stringify(e));
  const cr = await p.evaluate(() => { CRESTS['136013'] = { u: 'https://r2.thesportsdb.com/images/media/team/badge/x.png', d: 'data:image/png;base64,AAAA' }; const t = SPORTS.tops(3).leagues[0].rows; return [t[0].crest, 'crest' in t[1]]; });
  check('a club whose crest the page has: its real crest (the small copy), the others no crest field (the name alone, no swatch)', cr[0] === 'data:image/png;base64,AAAA' && cr[1] === false, JSON.stringify(cr));
  const sw = await p.evaluate(() => [spSw('136013'), spSw('999999')]);
  check('the sports cards: a club is its crest or nothing (no colour swatch)', /^<img class="spcr" src="data:image\/png;base64,AAAA"/.test(sw[0]) && sw[1] === '', JSON.stringify(sw));
  const n5 = await p.evaluate(() => SPORTS.tops(5).leagues[0].rows.length);
  check('n is the number of clubs (4 when the table has 4)', n5 === 4, n5);
  await ctx.close();
}
{   // while TheSportsDB has not answered: not ready, and only what is known
  const { p, ctx } = await open(FILES, true);
  await p.evaluate(() => SPORTS.tops(3));
  await p.waitForTimeout(600);
  const r = await p.evaluate(() => SPORTS.tops(3));
  check('a table still asked: ready is false, the leagues known so far are there', !r.ready && r.leagues.length === 1 && r.leagues[0].id === '4668', JSON.stringify(r).slice(0, 200));
  await ctx.close();
}
{   // nothing at all: no league, ready (the card is hidden, not empty)
  const { p, ctx } = await open({});
  await p.evaluate(() => { CONFIG.leagues = ['Nothing']; });
  await p.evaluate(() => SPORTS.tops(3));
  await p.waitForFunction(() => SPORTS.tops(3).ready, null, { timeout: 15000 }).catch(() => {});
  const r = await p.evaluate(() => SPORTS.tops(3));
  check('no data at all: ready, no league (the card hides)', r.ready && r.leagues.length === 0, JSON.stringify(r));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all sport tops tests passed');
