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
// the cup files as sports_files.cup writes them: the knockout rounds with their ties (two legs, the aggregate), the league phase as groups
const tie = (a, an, b, bn, legs, ag) => ({ a, b, an, bn, legs, ...(ag ? { ag } : {}) });
const CUP = { v: 1, id: '4719', en: 'AFC Champions League Elite', ar: 'دوري أبطال آسيا للنخبة', season: '2025-2026', updated: 'x', table: [], last: [], next: [],
  cup: { rounds: [
    { r: '16', ar: 'دور الـ١٦', games: [], ties: [tie(T.hilal[0], T.hilal[1], '134000', 'Sepahan', [g('c1', ...T.hilal, '134000', 'Sepahan', NOW - 200 * H, '3', '1'), g('c1b', '134000', 'Sepahan', ...T.hilal, NOW - 30 * H, '1', '1')], [4, 2]),
      tie('134002', 'Al-Duhail', '134003', 'Persepolis', [], null)] },
    { r: '125', ar: 'ربع النهائي', games: [], ties: [tie('134001', 'Al-Sadd', T.hilal[0], T.hilal[1], [g('c2', '134001', 'Al-Sadd', ...T.hilal, NOW + 300 * H)], null)] }] } };
const row2 = (id, n, pts, f) => ({ id, n, ar: '', p: 4, w: 0, d: 0, l: 0, gf: 0, ga: 0, pts, f });
const PHASE = { v: 1, id: '4480', en: 'UEFA Champions League', ar: 'دوري أبطال أوروبا', season: '2026-2027', updated: 'x', table: [], last: [], next: [],
  cup: { phase: { md: 4, of: 8, groups: [[row2('1', 'A', 12, 'wwww'), row2('2', 'B', 10, 'wwdw'), row2('3', 'C', 9, 'wwwl'), row2('4', 'D', 9, 'wlww'), row2('5', 'E', 8, 'wwdl'),
    row2(T.nassr[0], T.nassr[1], 7, 'wdwl'), row2('7', 'G', 6, 'lwlw')], [row2('8', 'H', 12, 'wwww'), row2('9', 'I', 3, 'llwl')]] } } };
const INDEX = { v: 1, updated: 'x', leagues: { '4719': { en: 'AFC Champions League', ar: 'دوري أبطال آسيا للنخبة' } }, teams: { [T.hilal[0]]: { en: 'Al-Hilal', ar: 'الهلال', leagues: ['4668', '4719'] },
  [T.nassr[0]]: { en: 'Al-Nassr', ar: 'النصر', leagues: ['4668', '4480'] } } };
const F1 = { v: 1, updated: 'x', season: '2026', round: '24', name: 'Abu Dhabi Grand Prix', ar: 'جائزة أبوظبي الكبرى', circuit: 'Yas Marina Circuit', country: 'UAE',
  sessions: [{ k: 'fp1', t: '2026-12-04T09:30:00Z' }, { k: 'q', t: '2026-12-05T14:00:00Z' }, { k: 'race', t: '2026-12-06T13:00:00Z' }] };
const H2H = { v: 1, pair: '136013-136022', meetings: [g('m1', ...T.hilal, ...T.nassr, NOW - 900 * H, '2', '1'), g('m2', ...T.nassr, ...T.hilal, NOW - 1900 * H, '1', '1')] };
const KSA = { v: 1, id: '136137', en: 'Saudi Arabia', ar: 'السعودية', next: [{ id: 'n1', lg: '4500', s: '2026', t: iso(NOW + 100 * H) + 'Z', home: true, vs: '140000', vsn: 'Jordan', gf: null, ga: null, st: 'NS', r: '' }], last: [] };
// the state of today's game: before (in 3 h), live (the 60th minute), after (ended an hour ago)
const TODAY = { before: ev('d1', ...T.hilal, ...T.nassr, NOW + 3 * H, { strVenue: 'Kingdom Arena' }),
  live: ev('d1', ...T.hilal, ...T.nassr, NOW - 60 * 60000, { strStatus: '2H', intHomeScore: '2', intAwayScore: '1', strProgress: '60', strHomeGoalDetails: "12':Salem;45+2':Fahad;", strAwayGoalDetails: "Majed 38'" }),
  after: ev('d1', ...T.hilal, ...T.nassr, NOW - 3 * H, { strStatus: 'FT', intHomeScore: '3', intAwayScore: '1' }),
  soon: ev('2100001', ...T.hilal, ...T.nassr, NOW + 10 * 60000) };

const browser = await chromium.launch();
const json = (r, j) => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
async function open(hash, { state = 'before', size = [461, 250], sportsJs = null, init = null } = {}) {
  const ctx = await browser.newContext({ viewport: { width: size[0], height: size[1] }, deviceScaleFactor: 2, timezoneId: 'Asia/Riyadh' });
  if (init) await ctx.addInitScript(init);
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    asked.push(u);
    const a = /\/data-archive\/(.+)$/.exec(u);
    if (a) return a[1] === 'sports/4668.json' ? json(r, SPORTS) : a[1] === 'sports/4719.json' ? json(r, CUP) : a[1] === 'sports/4480.json' ? json(r, PHASE) : a[1] === 'index.json' ? json(r, INDEX) : a[1] === 'f1/next.json' ? json(r, F1)
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
  ['matchday', 'live', c => /٢ - ١/.test(c.full) && /Salem ١٢، Fahad ٤٥\+٢/.test(c.full) && /Majed ٣٨/.test(c.full) && !/المصدر لا يذكر/.test(c.full)],
  ['matchday', 'after', c => /٣ - ١/.test(c.full) && /انتهت/.test(c.full) && /الهلال الأول بـ٣٠ نقطة/.test(c.full) && /المصدر لا يذكر أسماء الهدّافين لهذه المباراة/.test(c.full)],
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
{   // the cup path: the club's ties round by round, the aggregate and the verdict; AFC found through the archive's index (no AFC game next)
  const { p, ctx, errors } = await open(home('cuppath'));
  const c = await card(p);
  check('cuppath: the club\'s path, its tie in each round with the aggregate, «تأهل», the next leg\'s time', !errors.length && c.hd === 'دوري أبطال آسيا للنخبة'
    && /دور الـ١٦/.test(c.full) && /× Sepahan|× سباهان/.test(c.full) && /٤ - ٢/.test(c.full) && /مجموع المباراتين، تأهل/.test(c.full) && /ربع النهائي/.test(c.full)
    && /السد|Al-Sadd/.test(c.full) && !/Duhail|الدحيل/.test(c.full) && (c.html.match(/spbox on/g) || []).length === 2, c.hd + ' :: ' + c.full + errors.join('|'));
  check('cuppath: the one line says the round the club is in', /الهلال في ربع النهائي/.test(c.line), c.line);
  await shot(p, 'cuppath-home');
  await ctx.close();
}
{   // the league phase: the club's group (not the other region's), its place of how many, the matchday
  const { p, ctx, errors } = await open('widget=1&home_panel=cuppath&teams=' + T.nassr[0]);
  const c = await card(p);
  check('cuppath: the league phase with the club\'s place and the matchday', !errors.length && /مرحلة الدوري، الجولة ٤ من ٨/.test(c.full) && /النصر/.test(c.full)
    && !/\bH\b|\bI\b/.test(c.full) && /النصر المركز ٦ من ٧، ٧ نقطة/.test(c.line), c.full + ' / ' + c.line + errors.join('|'));
  await shot(p, 'cuppath-phase-home');
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

// 4.4 polish: every sports card at 1080p (a 1080p TV is 960x540 CSS px at 2x), on the screensaver, the home's card (461x250) and the couch strip
// (864x108), the home also on a light look («نظيف»: the launcher's #hpanel/#hink…): no glyph box cut by its box or the card (an Arabic
// descender is in it), no two texts on each other, one font family, every small label one size, the words readable on their background
const AUDIT = () => {
  const root = CARDS[0].el, cb = root.getBoundingClientRect(), res = { clip: [], overlap: [], fonts: new Set(), sps: new Set(), low: [] };
  const vis = e => { const cs = getComputedStyle(e); return cs.display !== 'none' && cs.visibility !== 'hidden' && e.getClientRects().length; };
  const leaves = [...root.querySelectorAll('*')].filter(e => vis(e) && [...e.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()));
  const lum = c => { const m = (c.match(/[\d.]+/g) || []).map(Number); const f = v => { v /= 255; return v <= .03928 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4; };
    return [.2126 * f(m[0]) + .7152 * f(m[1]) + .0722 * f(m[2]), m[3] == null ? 1 : m[3]]; };
  const bgOf = e => { for (let a = e; a; a = a.parentElement) { const c = getComputedStyle(a).backgroundColor; if (c && lum(c)[1] > .5) return c; } return 'rgb(20,20,20)'; };
  const boxes = [];
  for (const e of leaves) {
    const cs = getComputedStyle(e);
    res.fonts.add(cs.fontFamily.split(',')[0].replace(/["']/g, '').trim());
    if (e.classList.contains('sps') && !e.closest('li.spc')) res.sps.add(cs.fontSize);
    const fg = lum(cs.color), bg = lum(bgOf(e)), cr = (Math.max(fg[0], bg[0]) + .05) / (Math.min(fg[0], bg[0]) + .05);
    if (cr < 3.5) res.low.push(e.textContent.trim().slice(0, 20) + ' ' + cr.toFixed(1));
    for (const n of e.childNodes) if (n.nodeType === 3 && n.textContent.trim()) {
      const r = document.createRange(); r.selectNodeContents(n);
      for (const x of r.getClientRects()) {
        boxes.push({ e, x, t: n.textContent.trim().slice(0, 20) });
        for (let a = e; a && a !== document.body; a = a.parentElement) { const st = getComputedStyle(a);
          if (st.overflowX === 'visible' && st.overflowY === 'visible') continue;
          const b = a.getBoundingClientRect();
          if (x.bottom > b.bottom + .5 || x.top < b.top - .5 || x.left < b.left - .5 || x.right > b.right + .5) res.clip.push(n.textContent.trim().slice(0, 20) + ' in .' + a.className); }
        if (x.bottom > cb.bottom + .5 || x.right > cb.right + .5 || x.left < cb.left - .5) res.clip.push(n.textContent.trim().slice(0, 20) + ' out of the card');
      }
    }
  }
  for (let i = 0; i < boxes.length; i++) for (let j = i + 1; j < boxes.length; j++) {
    const A = boxes[i], B = boxes[j];
    if (A.e === B.e || A.e.contains(B.e) || B.e.contains(A.e)) continue;
    if (Math.min(A.x.right, B.x.right) - Math.max(A.x.left, B.x.left) > 1 && Math.min(A.x.bottom, B.x.bottom) - Math.max(A.x.top, B.x.top) > 2) res.overlap.push(A.t + ' / ' + B.t);
  }
  return { clip: res.clip, overlap: res.overlap, fonts: [...res.fonts], sps: [...res.sps], low: res.low };
};
const LIGHT = '&hpanel=rgba(255,255,255,0.980)&hline=rgba(20,24,30,0.078)&hink=rgba(27,29,33,1)&hsub=rgba(80,85,94,1)&hacc=rgba(11,94,81,1)';   // «نظيف» (GlassStyle.CLEAN_LIGHT)
const lg = '&leagues=Saudi-Arabian Pro League';
const PLACES = [['the home', k => home(k, lg), [461, 250]], ['the home, light', k => home(k, lg + LIGHT), [461, 250]], ['the couch strip', k => home(k, lg + '&row=1'), [864, 108]],
  ['the couch strip, light', k => home(k, lg + '&row=1' + LIGHT), [864, 108]], ['the screensaver', k => `panel=${k}&teams=136013${lg}`, [960, 540]]];
let polished = 0;
const polishBad = [];
for (const [kind, state] of [['matchday', 'before'], ['matchday', 'live'], ['matchday', 'after'], ['livenow', 'live'], ['minitable', 'before'], ['nextmatches', 'before'],
  ['yesterday', 'before'], ['weekend', 'before'], ['cuppath', 'before'], ['formula', 'before'], ['national', 'before']]) for (const [place, url, size] of PLACES) {
  const { p, ctx, errors } = await open(url(kind), { state, size });
  const a = await p.evaluate(AUDIT);
  const bad = [a.clip.length && 'cut: ' + a.clip.slice(0, 2).join(' | '), a.overlap.length && 'overlap: ' + a.overlap.slice(0, 2).join(' | '), a.fonts.length !== 1 && 'fonts: ' + a.fonts,
    a.sps.length > 1 && 'small labels in ' + a.sps.join(' and '), a.low.length && 'hard to read: ' + a.low.slice(0, 2).join(' | '), errors.length && errors[0]].filter(Boolean);
  if (bad.length) polishBad.push(`${kind} (${state}) on ${place}: ${bad.join('; ')}`);
  polished++;
  await ctx.close();
}
check(`polish: ${polished} cards at 1080p, nothing cut, nothing on top of another, one font, one size of small label, readable (light look too)`, !polishBad.length, polishBad.slice(0, 6).join('\n   '));
{   // the takeover's heading is the card's name and the time, as words (the page once painted its markup as text, longer at each redraw)
  const { p, ctx } = await open('panel=matchday&teams=136013' + lg, { state: 'before', size: [960, 540] });
  await p.evaluate(() => { renderPanel(false, 'matchday'); renderPanel(false, 'matchday'); });
  const hd = await p.evaluate(() => CARDS[0].hd.textContent);
  check('the takeover\'s heading: «مباراة اليوم» once and the time, no markup', !/[<>]|span/.test(hd) && (hd.match(/مباراة اليوم/g) || []).length === 1 && /٥:٠٠/.test(hd), hd);
  // the screensaver's notice: the top right in Arabic too, nearly solid, a readable size
  const k = await p.evaluate(async () => { spKick({ kind: 'goal', h: 'الهلال', a: 'النصر', hs: 2, as: 1, min: '67', lg: 'دوري روشن السعودي' }); await new Promise(r => setTimeout(r, 800)); const e = document.getElementById('spkick'), cs = getComputedStyle(e);
    return { right: innerWidth - e.getBoundingClientRect().right, left: e.getBoundingClientRect().left, w: innerWidth, a: (n => n.length > 3 ? +n[3] : n.length === 3 ? 1 : NaN)(cs.backgroundColor.match(/[\d.]+/g) || []), fs: parseFloat(getComputedStyle(e.querySelector('.km')).fontSize) }; });
  check('the screensaver\'s notice: the top right in Arabic, solid (at least .95 opaque), the clubs at 20 CSS px or more (40 on a 1080p TV)', k.right < k.w * .05 && k.left > k.w / 2 && k.a >= .95 && k.fs >= 20, JSON.stringify(k));
  await ctx.close();
}
{   // on the home's card (the goal, the final score) and in the couch strip: inside the card's own window, over all of it, nothing cut
  for (const [where, q, size] of [['the home\'s card', '', [461, 250]], ['the couch strip', '&row=1', [864, 108]]]) {
    const { p, ctx } = await open(home('livenow', q), { state: 'live', size });
    const k = await p.evaluate(async () => { spKick({ kind: 'goal', h: 'الهلال', a: 'النصر', hs: 2, as: 1, min: '67', lg: 'دوري روشن السعودي' }); await new Promise(r => setTimeout(r, 800));   // once it has slid in
      const e = document.getElementById('spkick'), b = e.getBoundingClientRect(); let cut = 0;
      for (const x of e.querySelectorAll('span,b')) { const r = x.getBoundingClientRect(); if (r.width && (r.bottom > b.bottom + .5 || r.top < b.top - .5 || r.right > b.right + .5 || r.left < b.left - .5)) cut++; }
      return { inside: b.left >= 0 && b.top >= 0 && b.right <= innerWidth + .5 && b.bottom <= innerHeight + .5, cut, text: e.textContent }; });
    check(`the notice on ${where}: inside the card's window, nothing cut, the score`, k.inside && !k.cut && /٢ - ١/.test(k.text), JSON.stringify(k));
    await ctx.close();
  }
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all sports card tests passed');
