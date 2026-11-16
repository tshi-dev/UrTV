// 4.4 «الرياضة»: the sports cards (dist/sports.js beside the page) in a real browser, offline, on fixed data: each card on the home and the
// screensaver, the couch row's one line, the takeover of «مباراة اليوم», sports.js only from the page's own build, nothing asked more often
//   node tests/sports_cards.test.mjs      (SHOTS=<dir> writes a picture of each)
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-05T14:00:00Z');   // Thursday 17:00 in Riyadh
const iso = ms => new Date(ms).toISOString().slice(0, 19);
const H = 3600000;
const ev = (id, h, hn, a, an, at, extra = {}) => ({ idEvent: id, idLeague: '4668', strLeague: 'Saudi-Arabian Pro League', idHomeTeam: h, idAwayTeam: a, strHomeTeam: hn, strAwayTeam: an,
  strStatus: 'NS', strTimestamp: iso(at), dateEvent: iso(at).slice(0, 10), strTime: iso(at).slice(11), strSport: 'Soccer', strSeason: '2026-2027', intRound: '11', ...extra });
const g = (id, h, hn, a, an, at, hs = null, as = null) => ({ id, lg: '4668', t: iso(at) + 'Z', r: '11', h, a, hn, an, hs, as, st: hs == null ? 'NS' : 'FT' });
const T = { hilal: ['136013', 'Al-Hilal'], nassr: ['136022', 'Al-Nassr'], ittihad: ['136018', 'Al-Ittihad'], shabab: ['136020', 'Al-Shabab'], ahli: ['137721', 'Al-Ahli'],
  ettifaq: ['136017', 'Al-Ettifaq'], fateh: ['136011', 'Al-Fateh'], qadsiah: ['136015', 'Al-Qadsiah'] };
const row = (k, pts, f) => ({ id: T[k][0], n: T[k][1], ar: '', p: 10, w: 0, d: 0, l: 0, gf: 0, ga: 0, pts, f });
const SPORTS = { v: 1, id: '4668', en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي', season: '2026-2027', updated: '2026-11-05T00:00:00Z',
  table: [row('hilal', 30, 'wwdww'), row('nassr', 26, 'wdwlw'), row('ittihad', 24, 'wwlwd'), row('ahli', 22, 'dwwlw'), row('qadsiah', 18, 'lwdwl'), row('fateh', 17, 'wlldw'), row('ettifaq', 16, 'dlwdw'), row('shabab', 12, 'llldw')],
  last: [g('y1', ...T.ittihad, ...T.shabab, NOW - 22 * H, '0', '0'), g('y2', ...T.ahli, ...T.ettifaq, NOW - 20 * H, '3', '1')],
  next: [g('w1', ...T.ittihad, ...T.shabab, NOW + 1 * H), g('w2', ...T.ahli, ...T.fateh, NOW + 27 * H), g('w3', ...T.ettifaq, ...T.qadsiah, NOW + 51 * H)] };
const CUP = { v: 1, id: '4719', en: 'AFC Champions League Elite', ar: 'دوري أبطال آسيا للنخبة', season: '2026-2027', updated: 'x', table: [], last: [], next: [],
  cup: { rounds: [{ r: '16', ar: 'دور الـ١٦', games: [{ ...g('c1', ...T.hilal, '134000', 'Sepahan', NOW - 200 * H, '3', '1'), lg: '4719' }] },
    { r: '125', ar: 'ربع النهائي', games: [{ ...g('c2', ...T.hilal, '134001', 'Al-Sadd', NOW + 300 * H), lg: '4719' }] }] } };
const F1 = { v: 1, updated: 'x', season: '2026', round: '24', name: 'Abu Dhabi Grand Prix', ar: 'جائزة أبوظبي الكبرى', circuit: 'Yas Marina Circuit', country: 'UAE',
  sessions: [{ k: 'fp1', t: '2026-12-04T09:30:00Z' }, { k: 'q', t: '2026-12-05T14:00:00Z' }, { k: 'race', t: '2026-12-06T13:00:00Z' }] };
const H2H = { v: 1, pair: '136013-136022', meetings: [g('m1', ...T.hilal, ...T.nassr, NOW - 900 * H, '2', '1'), g('m2', ...T.nassr, ...T.hilal, NOW - 1900 * H, '1', '1')] };
const KSA = { v: 1, id: '136137', en: 'Saudi Arabia', ar: 'السعودية', next: [{ id: 'n1', lg: '4500', s: '2026', t: iso(NOW + 100 * H) + 'Z', home: true, vs: '140000', vsn: 'Jordan', gf: null, ga: null, st: 'NS', r: '' }], last: [] };
// the state of today's game: before (in 3 h), live (the 60th minute), after (ended an hour ago)
const TODAY = { before: ev('d1', ...T.hilal, ...T.nassr, NOW + 3 * H, { strVenue: 'Kingdom Arena' }),
  live: ev('d1', ...T.hilal, ...T.nassr, NOW - 60 * 60000, { strStatus: '2H', intHomeScore: '2', intAwayScore: '1', strProgress: '60', strHomeGoalDetails: "12':Salem;55':Fahad;", strAwayGoalDetails: "38':Majed;" }),
  after: ev('d1', ...T.hilal, ...T.nassr, NOW - 3 * H, { strStatus: 'FT', intHomeScore: '3', intAwayScore: '1' }),
  soon: ev('2100001', ...T.hilal, ...T.nassr, NOW + 10 * 60000) };

const browser = await chromium.launch();
const json = (r, j) => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
async function open(hash, { state = 'before', size = [461, 250], sportsJs = null, init = null } = {}) {
  const ctx = await browser.newContext({ viewport: { width: size[0], height: size[1] }, timezoneId: 'Asia/Riyadh' });
  if (init) await ctx.addInitScript(init);
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    asked.push(u);
    const a = /\/data-archive\/(.+)$/.exec(u);
    if (a) return a[1] === 'sports/4668.json' ? json(r, SPORTS) : a[1] === 'sports/4719.json' ? json(r, CUP) : a[1] === 'f1/next.json' ? json(r, F1)
      : a[1] === 'h2h/136013-136022.json' ? json(r, H2H) : a[1] === 'teams/136137.json' ? json(r, KSA) : r.fulfill({ status: 404, body: '' });
    if (/eventsnext\.php\?id=136013/.test(u)) return json(r, { events: [TODAY[state]] });
    if (/eventsnext\.php/.test(u)) return json(r, { events: null });
    if (/eventslast\.php|lookupevent|lookuptable/.test(u)) return json(r, { results: null, events: null, table: null });
    if (u.endsWith('/master/dist/sports.js')) return sportsJs ? r.fulfill({ body: sportsJs, contentType: 'text/javascript', headers: { 'Access-Control-Allow-Origin': '*' } })
      : r.fulfill({ path: path.join(ROOT, 'dist/sports.js'), headers: { 'Access-Control-Allow-Origin': '*' } });   // the test's browser reads no file: XHR (the TV's WebView does)
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&' + hash + '&gap=0');
  await p.waitForTimeout(1500);
  return { p, ctx, errors, asked };
}
const card = p => p.evaluate(() => { const c = CARDS[0]; return { hd: c.hd.textContent, full: [...c.ul.querySelectorAll('li.spf')].map(l => l.textContent).join(' / '),
  line: [...c.ul.querySelectorAll('li.spc')].map(l => l.textContent).join(' / '), html: c.ul.innerHTML }; });
const shot = async (p, name) => { if (process.env.SHOTS) await p.screenshot({ path: path.join(process.env.SHOTS, name + '.png') }); };
const home = (k, extra = '') => `widget=1&home_panel=${k}&teams=136013${extra}`;

for (const [kind, state, expect] of [
  ['matchday', 'before', c => /الهلال/.test(c.full) && /النصر/.test(c.full) && /تبدأ بعد/.test(c.full) && /المركز ١، ٣٠ نقطة/.test(c.full) && /الانطلاقة/.test(c.full) && /آخر مواجهتين: الهلال ١ فوز، ١ تعادل/.test(c.full)],
  ['matchday', 'live', c => /٢ - ١/.test(c.full) && /Salem ١٢، Fahad ٥٥/.test(c.full) && /Majed ٣٨/.test(c.full)],
  ['matchday', 'after', c => /٣ - ١/.test(c.full) && /انتهت/.test(c.full) && /الهلال الأول بـ٣٠ نقطة/.test(c.full)],
  ['livenow', 'live', c => /٢ - ١/.test(c.full) && /الهلال/.test(c.full)],
  ['minitable', 'before', c => c.hd === 'دوري روشن السعودي' && /١الهلال/.test(c.full) && /٣٠/.test(c.full) && /٧الاتفاق/.test(c.full)],
  ['nextmatches', 'before', c => /الهلال/.test(c.full) && /× النصر/.test(c.full) && /على أرضه/.test(c.full)],
  ['yesterday', 'before', c => /الأهلي/.test(c.full) && /٣ - ١/.test(c.full) && /الاتحاد/.test(c.full) && !/النصر/.test(c.full)],
  ['weekend', 'before', c => /اليوم|الخميس/.test(c.full) && /الأهلي × الفتح/.test(c.full) && /الاتفاق × القادسية/.test(c.full)],
  ['formula', 'before', c => /جائزة أبوظبي الكبرى/.test(c.full) && /التأهيل/.test(c.full) && /السباق/.test(c.full)],
  ['national', 'before', c => /السعودية/.test(c.full) && /الأردن|Jordan/.test(c.full)],
]) {
  const { p, ctx, errors } = await open(home(kind, kind === 'formula' || kind === 'national' ? '' : '&leagues=Saudi-Arabian Pro League'), { state });
  const c = await card(p);
  check(`${kind} (${state}): no script error`, !errors.length, errors.join(' | '));
  check(`${kind} (${state}): the card says what it should`, expect(c), c.hd + ' :: ' + c.full);
  check(`${kind} (${state}): no «·» or «•» anywhere in it`, !/[·•]/.test(c.full + c.line + c.hd), c.full);
  check(`${kind} (${state}): text inside the card, nothing cut`, await p.evaluate(() => { const b = CARDS[0].el.getBoundingClientRect(); return [...CARDS[0].el.querySelectorAll('b')].every(e => { const r = e.getBoundingClientRect(); return !r.width || (r.left >= b.left - 1 && r.right <= b.right + 1); }); }));
  await shot(p, `${kind}-${state}-home`);
  await ctx.close();
}
{   // the cup path with the cup file
  const { p, ctx, errors } = await open(home('cuppath', '&leagues=id4719'));
  const c = await card(p);
  check('cuppath: the rounds, the followed club gold in each', !errors.length && /دور الـ١٦/.test(c.full) && /ربع النهائي/.test(c.full) && (c.html.match(/spbox on/g) || []).length === 2, c.full + errors.join('|'));
  await shot(p, 'cuppath-home');
  await ctx.close();
}
{   // the couch row: one line per card
  const { p, ctx } = await open(home('matchday', '&row=1'), { state: 'live', size: [1300, 200] });
  const vis = await p.evaluate(() => [...CARDS[0].ul.children].filter(li => getComputedStyle(li).display !== 'none').map(li => li.className + ':' + li.textContent));
  check('couch row: «مباراة اليوم» is its one line (the score and the minute)', vis.length === 1 && /^spc:/.test(vis[0]) && /٢ - ١/.test(vis[0]), JSON.stringify(vis));
  await ctx.close();
}
{   // the screensaver: «مباراة اليوم» takes the middle from 3 hours before the kick-off, the time in its title; the home's card never does
  const { p, ctx, errors } = await open('panel=matchday&teams=136013&leagues=Saudi-Arabian Pro League', { state: 'before', size: [1920, 1080] });
  const t = await p.evaluate(() => ({ take: document.documentElement.classList.contains('sptake'), clock: getComputedStyle(document.querySelector('.clockwrap')).display, hd: CARDS[0].hd.textContent }));
  check('screensaver: the takeover in the window (the big clock hidden, the time in the card\'s title)', !errors.length && t.take && t.clock === 'none' && /٥:٠٠/.test(t.hd), JSON.stringify(t) + errors.join('|'));
  await shot(p, 'matchday-takeover-saver');
  await ctx.close();
}
{   // the home's card never takes over
  const { p, ctx } = await open(home('matchday'), { state: 'before' });
  check('home: no takeover', !(await p.evaluate(() => document.documentElement.classList.contains('sptake'))));
  await ctx.close();
}
{   // sports.js of another build is never run: the repository's copy with another stamp is refused, the card says it is loading
  const { p, ctx } = await open(home('livenow'), { sportsJs: "SPORTS_BUILD:19990101000000:\nSPORTS = {card(){ return {rows:['<li>WRONG</li>']}; }, load(){}};" });
  check('another build\'s sports.js is never run', !(await p.evaluate(() => !!SPORTS)) && !/WRONG/.test((await card(p)).html));
  await ctx.close();
}
{   // no sports card and the alert off: sports.js is not even read; the sports files are read at the games' pace (once per load)
  const { p, ctx, asked } = await open('widget=1&home_panel=verse&teams=136013&ko=0');
  check('no sports card and the alert off: sports.js is not read', !(await p.evaluate(() => !!SPORTS)));
  await ctx.close();
  const o = await open(home('minitable', '&leagues=Saudi-Arabian Pro League'));
  await o.p.clock.runFor(10 * 60000);
  check('the league\'s sports file is asked once in 10 minutes (the games\' 30-minute pace)', o.asked.filter(u => u.includes('/data-archive/sports/4668.json')).length === 1, o.asked.filter(u => u.includes('sports/')).join());
  await o.ctx.close();
}

// 4.4 the kick-off alert, the page's side
const SPY = `window.__kicks = []; new MutationObserver(m => m.forEach(r => r.addedNodes.forEach(n => { if (n.id === 'spkick') window.__kicks.push(n.textContent); })))
  .observe(document, { childList: true, subtree: true });`;
{   // the home: the launcher reads the followed club's coming games with spKickList() (no JavaScript interface), the time as a string of digits
  const { p, ctx, errors } = await open(home('matchday'), { state: 'soon', init: SPY });
  const h = await p.evaluate(() => typeof spKickList === 'function' ? JSON.parse(spKickList() || '[]') : []);
  check('home: the coming games of the followed club are ready for the launcher', !errors.length && h.length === 1 && h[0].id === '2100001' && /^\d{13}$/.test(h[0].t) && +h[0].t === Date.parse('2026-11-05T14:10:00Z')
    && /الهلال/.test(h[0].h) && /النصر/.test(h[0].a) && Array.isArray(h[0].hc), JSON.stringify(h) + errors.join('|'));
  await p.clock.runFor(11 * 60000);
  check('home: the page draws no notice by itself (the launcher asks for it)', !(await p.evaluate(() => window.__kicks.length)));
  // the launcher's ask: the notice in the top-right corner, cream text on dark glass, gone after 8 s
  const k = await p.evaluate(() => { spKick(JSON.stringify({ kind: 'start', h: 'الهلال', a: 'النصر', lg: 'دوري روشن السعودي' })); const e = document.getElementById('spkick'), r = e.getBoundingClientRect(), cs = getComputedStyle(e);
    return { t: e.textContent, right: innerWidth - r.right, top: r.top, w: innerWidth, bg: cs.backgroundColor, fg: getComputedStyle(e.querySelector('.km')).color }; });
  check('the notice: the two clubs and «بدأت المباراة», top right', /بدأت المباراة/.test(k.t) && /الهلال/.test(k.t) && /النصر/.test(k.t) && k.right < k.w * 0.25 && k.top < 120 && !/[·•«»]/.test(k.t), JSON.stringify(k));
  check('the notice: light text on dark glass', /rgba?\((\d+), (\d+), (\d+)/.test(k.bg) && +RegExp.$1 < 40 && /rgb\((2[2-5]\d)/.test(k.fg), k.bg + ' / ' + k.fg);
  await p.clock.runFor(9000);
  check('the notice goes away after 8 s', !(await p.evaluate(() => document.getElementById('spkick'))));
  await ctx.close();
}
{   // the screensaver (no launcher hand-off there): its own notice at the kick-off, once
  const { p, ctx, errors } = await open('panel=verse&teams=136013&leagues=Saudi-Arabian Pro League', { state: 'soon', size: [1920, 1080], init: SPY });
  check('screensaver: nothing before the kick-off', !(await p.evaluate(() => window.__kicks.length)));
  await p.clock.runFor(10 * 60000 + 2000);
  const ks = await p.evaluate(() => window.__kicks);
  check('screensaver: its own notice at the kick-off, once', !errors.length && ks.length === 1 && /بدأت المباراة/.test(ks[0]), JSON.stringify(ks) + errors.join('|'));
  await ctx.close();
  const o = await open('panel=verse&teams=136013&ko=0', { state: 'soon', size: [1920, 1080], init: SPY });
  await o.p.clock.runFor(11 * 60000);
  check('screensaver with the alert off (&ko=0): no notice', !(await o.p.evaluate(() => window.__kicks.length)));
  await o.ctx.close();
  const k = await open('panel=verse&teams=136013&kids=1&kogoal=1', { state: 'soon', size: [1920, 1080], init: SPY });
  await k.p.clock.runFor(11 * 60000);
  const kk = await k.p.evaluate(async () => { SP.score['2100001'] = { hs: 0, as: 0, live: true, done: false }; spScoreWatch(Date.now() + 3600000); await new Promise(r => setTimeout(r, 0)); return window.__kicks.length; });
  check('screensaver in kids mode (&kids=1, from the launcher): no notice at all', kk === 0, kk);
  await k.ctx.close();
}
{   // a goal (&kogoal=1): only when the score of a followed club's live game goes up since the last look; never at the first look
  for (const [flag, want] of [['&kogoal=1', 1], ['', 0]]) {
    const { p, ctx } = await open(home('livenow', flag), { state: 'live', init: SPY });
    const n0 = await p.evaluate(() => window.__kicks.length);
    const ks = await p.evaluate(async () => { SP.score.d1 = { hs: 1, as: 1, live: true, done: false }; spScoreWatch(Date.now()); await new Promise(r => setTimeout(r, 0)); return window.__kicks; });
    check(`goal notice ${flag || 'off (the default)'}: ${want ? 'the score in the corner' : 'nothing'}`, n0 === 0 && ks.length === want && (!want || /٢ - ١/.test(ks[0])), JSON.stringify(ks));
    await ctx.close();
  }
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all sports card tests passed');
