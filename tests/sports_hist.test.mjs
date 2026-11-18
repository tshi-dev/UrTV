// 4.4 the club and league pages' history (#matchpage=1), offline in a real browser: the league's table from the archive's own file
// (sports/<id>.json, computed by CI) first, TheSportsDB's when the archive has none, and a plain line when neither has it; the club page's
// all-time count of the meetings the archive keeps (up to 100) and «كل المواجهات» past the first 10.
//   node tests/sports_hist.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');
const iso = ms => new Date(ms).toISOString().slice(0, 19) + 'Z';
const row = (id, n, ar, p, pts) => ({ id, n, ar, p, w: 0, d: 0, l: 0, gf: 0, ga: 0, pts, f: 'wdl' });
const SPORTS = { v: 1, id: '4668', en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي', season: '2026-2027', updated: 'x',
  table: [row('136013', 'Al-Hilal', 'الهلال', 10, 26), row('136014', 'Al-Nassr', 'النصر', 10, 22), row('136015', 'Al-Ittihad', '', 9, 19)], last: [], next: [] };
const TSDB = { table: [{ idTeam: '133604', strTeam: 'Arsenal', intRank: '1', intPoints: '30' }, { idTeam: '133610', strTeam: 'Chelsea', intRank: '2', intPoints: '27' }] };
// 34 meetings of 136013 (Al-Hilal) and 136014: Al-Hilal 15 wins, 9 draws, the other 10 (home and away both ways)
const MEET = [];
for (let i = 0; i < 34; i++) {
  const hilalHome = i % 2 === 0, res = i < 15 ? 'h' : i < 24 ? 'd' : 'n';
  const [hg, ng] = res === 'h' ? [2, 1] : res === 'd' ? [1, 1] : [0, 1];
  MEET.push({ id: 'm' + i, lg: '4668', s: '2020-2021', t: iso(NOW - (i + 1) * 30 * 86400000), r: '1', h: hilalHome ? '136013' : '136014', a: hilalHome ? '136014' : '136013',
    hn: hilalHome ? 'Al-Hilal' : 'Al-Nassr', an: hilalHome ? 'Al-Nassr' : 'Al-Hilal', hs: String(hilalHome ? hg : ng), as: String(hilalHome ? ng : hg), st: 'FT' });
}
const TEAM = { v: 1, id: '136013', en: 'Al-Hilal', ar: 'الهلال', updated: 'x', last: [],
  next: [{ id: 'n1', lg: '4668', s: '2026-2027', t: iso(NOW + 86400000), home: true, vs: '136014', vsn: 'Al-Nassr', gf: null, ga: null, st: 'NS', r: '11' }] };

const browser = await chromium.launch();
async function open(hash, files) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    asked.push(u);
    const ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const a = /\/(?:data-archive|football\/archive)\/(.+)$/.exec(u);
    if (a && files[a[1]] !== undefined) return files[a[1]] === null ? r.fulfill({ status: 404, body: '' }) : ok(files[a[1]]);
    if (a) return r.fulfill({ status: 404, body: '' });
    if (/lookuptable\.php\?l=4328/.test(u)) return ok(TSDB);
    if (/lookuptable\.php/.test(u)) return ok({ table: null });
    if (/eventsnext\.php|eventslast\.php|eventsnextleague|eventspastleague/.test(u)) return ok({ events: null, results: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&matchpage=1&' + hash);
  await p.waitForTimeout(1200);
  return { p, ctx, errors, asked };
}
const rows = p => p.evaluate(() => [...document.querySelectorAll('#mp li.mpg')].map(li => li.innerText.replace(/\s+/g, ' ').trim()));
const groupsText = p => p.evaluate(() => document.getElementById('mp').innerText.replace(/\s+/g, ' '));

{   // (a) the archive's table first: its order, the Arabic names, points and games played; TheSportsDB is not asked
  const { p, ctx, errors, asked } = await open('teams=136013', { 'sports/4668.json': SPORTS, 'results/4668.json': { v: 1, id: '4668', results: [] } });
  await p.evaluate(() => mpGo({ k: 'league', id: '4668' }));
  await p.waitForTimeout(800);
  const r = await rows(p);
  check('(a) the league table from the archive\'s own file: in order, Arabic names, points and games played', !errors.length && /^١\. الهلال/.test(r[0] || '') && /٢٦ نقطة، لعب ١٠/.test(r[0] || '')
    && /^٢\. النصر/.test(r[1] || '') && r.length === 3, JSON.stringify(r) + errors.join('|'));
  check('(a) …and TheSportsDB\'s table is not asked then', !asked.some(u => u.includes('lookuptable')), asked.filter(u => u.includes('lookup')).join());
  const opened = await p.evaluate(() => { MP.zone = 'rows'; MP.r = 0; const x = MP.rows && MP.rows[0]; if (x && x.act) x.act(); return MP.view && MP.view.k + ':' + MP.view.id; });
  check('(a) OK on a row opens that club\'s page', opened === 'club:136013', opened);
  await ctx.close();
}
{   // (a) no archive file for the league (before master publishes it, or a league it does not hold): TheSportsDB's, as before
  const { p, ctx, errors } = await open('teams=133604', { 'sports/4328.json': null, 'results/4328.json': { v: 1, id: '4328', results: [{ id: 'r1', lg: '4328', s: '2026-2027', t: iso(NOW - 86400000), r: '11', h: '133604', a: '133610', hn: 'Arsenal', an: 'Chelsea', hs: '1', as: '0', st: 'FT' }] } });
  await p.evaluate(() => mpGo({ k: 'league', id: '4328' }));
  await p.waitForTimeout(1200);
  const r = await rows(p);
  check('(a) no archive table: TheSportsDB\'s table of the season, as before', !errors.length && /^١\./.test(r[0] || '') && /٣٠ نقطة/.test(r[0] || '') && r.length === 2, JSON.stringify(r) + errors.join('|'));
  await ctx.close();
}
{   // (c) neither has a table: one plain line for this league, not an empty tab
  const { p, ctx, errors } = await open('teams=136013', { 'sports/5649.json': null, 'results/5649.json': { v: 1, id: '5649', results: [{ id: 'k1', lg: '5649', s: '2025-2026', t: iso(NOW - 86400000), r: '32', h: '1', a: '2', hn: 'A', an: 'B', hs: '1', as: '0', st: 'FT' }] } });
  await p.evaluate(() => mpGo({ k: 'league', id: '5649' }));
  await p.waitForTimeout(1200);
  const t = await groupsText(p);
  check('(c) neither source has a table: «الترتيب غير متاح لهذا الدوري»', !errors.length && t.includes('الترتيب غير متاح لهذا الدوري'), t.slice(0, 300) + errors.join('|'));
  await ctx.close();
}
{   // (b) the club page: the all-time count from every stored meeting, 10 meetings, then all of them after «كل المواجهات»
  const { p, ctx, errors } = await open('teams=136013', { 'teams/136013.json': TEAM, 'h2h/136013-136014.json': { v: 1, pair: '136013-136014', meetings: MEET } });
  await p.evaluate(() => mpGo({ k: 'club', id: '136013', vs: '136014' }));
  await p.waitForTimeout(1000);
  const t = await groupsText(p);
  const other = await p.evaluate(() => teamNameOf('136014'));   // the page's own name for the other club
  check('(b) the all-time count from the club\'s side: «الهلال ١٥ فوز، ٩ تعادل، <the other> ١٠», in ٣٤ meetings', !errors.length && t.includes('الهلال ١٥ فوز، ٩ تعادل، ' + other + ' ١٠') && t.includes('في ٣٤ مواجهة'), other + ' :: ' + t.slice(0, 400) + errors.join('|'));
  const n10 = await p.evaluate(() => MP.rows.filter(x => !x.it && String(x.idEvent).startsWith('m')).length);
  check('(b) ten meetings first, and «كل المواجهات (٣٤)» under them', n10 === 10 && t.includes('كل المواجهات (٣٤)'), n10);
  const n = await p.evaluate(() => { const i = MP.rows.findIndex(x => x.it && /كل المواجهات/.test(x.t)); MP.zone = 'rows'; MP.r = i; MP.rows[i].act(); return MP.rows.filter(x => !x.it && String(x.idEvent).startsWith('m')).length; });
  const t2 = await groupsText(p);
  check('(b) OK on it: all 34, and the line is gone', n === 34 && !t2.includes('كل المواجهات ('), n);
  check('(b) no «·» in the new lines', !/[·•]/.test(t2), '');
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all sports history tests passed');
