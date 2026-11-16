// Screensaver designs, mockups on the real page (dist/tv.html): 960x540 dp at 2x = 1920x1080; each design is d/<id>.css and .js added to it.
//   python3 src/build.py && BG_DIR=… FONT_DIR=… node docs/mockups/saver-designs/src/designs.mjs [name…]
import { chromium } from 'playwright';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import path from 'node:path';
// BG_DIR: the horizons and the photo (riyadh*, night*, nature7*: see ../README.md); FONT_DIR: an open Arabic font standing in for Thmanyah; OUT: where the pictures go
const HERE = path.dirname(new URL(import.meta.url).pathname), ROOT = path.resolve(HERE, '../../../..'), BG = process.env.BG_DIR, FD = process.env.FONT_DIR, OUTD = process.env.OUT || HERE + '/..';
const NOW = new Date('2026-10-22T13:58:00Z');   // Thursday 16:58 Riyadh: Maghrib at 17:21 is next
const hdr = { 'Access-Control-Allow-Origin': '*' };
const weather = () => { const t0 = Math.floor((NOW.getTime() + 3 * 36e5) / 36e5) * 36e5, hr = { time: [], temperature_2m: [], weather_code: [] };
  for (let k = 0; k < 48; k++) { hr.time.push(new Date(t0 + k * 36e5).toISOString().slice(0, 16)); hr.temperature_2m.push(31 - Math.min(k, 10) * .9); hr.weather_code.push(k % 5 ? 0 : 1); }
  const days = [0, 1, 2, 3, 4, 5, 6].map(i => new Date(t0 + i * 864e5).toISOString().slice(0, 10));
  return JSON.stringify({ current: { temperature_2m: 31, weather_code: 0 }, hourly: hr, daily: { time: days, temperature_2m_max: days.map(() => 36), temperature_2m_min: days.map(() => 22), weather_code: days.map(() => 0), sunrise: days.map(d => d + 'T05:55'), sunset: days.map(d => d + 'T17:21') } }); };
function net(route) {
  const u = new URL(route.request().url()), s = u.href; let m;
  if ((m = s.match(/assets\/fonts\/(Thmanyah[^/]*)\.woff2$/))) { const w = /Light/.test(m[1]) ? 300 : /Medium/.test(m[1]) ? 500 : /Display|Bold/.test(m[1]) ? 700 : 400;
    return route.fulfill({ path: `${FD}/ibm-plex-sans-arabic-arabic-${w}-normal.woff2`, headers: hdr }); }
  if ((m = s.match(/\/master\/((assets\/(backgrounds|fonts)|photos)\/.+)$/))) { const f = path.join(ROOT, m[1]); return existsSync(f) ? route.fulfill({ path: f, headers: hdr }) : route.abort(); }
  if (u.host === 'bg.local') return route.fulfill({ path: path.join(BG, u.pathname.slice(1)), headers: hdr });
  if (/open-meteo/.test(u.host)) return route.fulfill({ contentType: 'application/json', headers: hdr, body: weather() });
  if ((m = s.match(/chart\/([^?]+)\?/))) return route.fulfill({ contentType: 'application/json', headers: hdr, body: JSON.stringify({ chart: { result: [{ meta: { regularMarketPrice: 100, chartPreviousClose: 99 }, indicators: { quote: [{ close: [99, 100] }] } }] } }) });
  if (u.pathname.endsWith('/leagues/saudi-arabian-pro-league.json')) return route.fulfill({ contentType: 'application/json', headers: hdr, body: readFileSync(ROOT + '/tests/fixtures/saudi-pro-league.json', 'utf8') });
  if (/thesportsdb/.test(u.host) && !/images/.test(s)) { const F = JSON.parse(readFileSync(ROOT + '/tests/fixtures/saudi-pro-league.json', 'utf8'));
    return route.fulfill({ contentType: 'application/json', headers: hdr, body: JSON.stringify(u.pathname.endsWith('eventsnext.php') && u.searchParams.get('id') === '136013' ? { events: F.events.filter(e => e.idHomeTeam === '136013' || e.idAwayTeam === '136013').slice(-2) } : { events: null, results: null }) }); }
  return route.abort();
}
const SCENES = {   // the photo, its frosted glass (SceneClock.frost), its cut-out cropped to its box (scenes.tsv / masks index.json) and the digits' place, in dp
  riyadh: { photo: 'riyadh.jpg', glass: 'riyadh-frost.jpg', fg: 'riyadh-fg.webp', fgTop: 67.5, fgH: 321.5, top: 110, size: 190 },
  night: { photo: 'night.jpg', glass: 'night-frost.jpg', fg: 'night-fg.webp', fgTop: 77.5, fgH: 311.5, top: 113, size: 196 },
  nature7: { photo: 'nature16.jpg', glass: 'nature16-dark.jpg', fg: 'nature16-fg.webp', fgTop: 171, fgH: 218, top: 71, size: 200 },
};
const SHOTS = [
  ['01-clean-riyadh', 'clean', 'riyadh'], ['02-clean-night', 'clean', 'night'], ['03-clean-photo', 'clean', 'nature7'], ['04-clean-nonext-night', 'clean', 'night', 'nonext'],
  ['05-bigclock', 'bigclock', 'nature7'], ['06-frame', 'frame', 'night'], ['07-info', 'info', 'nature7'], ['08-dayline', 'dayline', 'riyadh'],
  ['09-clean-low', 'clean', 'night', 'low'],
  ['10-clean-bar-prayers-night', 'clean', 'night', '', { bar: 'prayers', barAt: 'bottom', nonext: 1 }],
  ['11-clean-bar-prayers-under-clock', 'clean', 'nature7', '', { bar: 'prayers', barAt: 'clock', nonext: 1 }],
  ['12-clean-bar-match', 'clean', 'riyadh', '', { bar: 'match', barAt: 'bottom', slot: 'matches' }],
  ['13-clean-bar-countdown', 'clean', 'night', '', { bar: 'countdown', barAt: 'bottom' }],
  ['14-clean-bar-weather', 'clean', 'nature7', '', { bar: 'weather', barAt: 'bottom' }],
  ['15-clean-weather-under-clock', 'clean', 'night', '', { wx: 'clock' }],
  ['16-clean-weather-corner', 'clean', 'riyadh', '', { wx: 'corner' }],
  ['17-clean-weather-usual', 'clean', 'nature7', '', { wx: 'usual' }],
  ['18-clean-bar-low', 'clean', 'night', 'low', { bar: 'prayers', barAt: 'bottom', nonext: 1 }],
];
const want = process.argv.slice(2), BARJS = readFileSync(HERE + '/bar.js', 'utf8');
const browser = await chromium.launch();
for (const [name, design, scene, opt, ex = {}] of SHOTS) {
  if (want.length && !want.some(w => name.includes(w))) continue;
  const sc = SCENES[scene];
  const ctx = await browser.newContext({ viewport: { width: 960, height: 540 }, deviceScaleFactor: 2, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, net);
  await ctx.addInitScript(i => { try { localStorage.setItem('photoIndex', i); } catch (e) {} }, readFileSync(ROOT + '/photos/index.json', 'utf8'));
  const p = await ctx.newPage(); await p.clock.setFixedTime(NOW);
  const errs = []; p.on('pageerror', e => errs.push(e.message));
  await p.goto('file://' + ROOT + '/dist/tv.html#city=' + encodeURIComponent('الرياض') + '&lat=24.7136&lon=46.6753&gap=0&panel=verse&teams=136013&slot=' + (ex.slot || 'countdown') + '&cd=2026-12-18&cdname=' + encodeURIComponent('إجازة نهاية الفصل') + (opt === 'low' ? '&lowram=1' : ''));
  await p.waitForFunction(() => typeof showBackground === 'function' && document.fonts.status === 'loaded', null, { timeout: 8000 }).catch(() => errs.push('not loaded'));
  await p.evaluate(u => showBackground(u, { d: 1, c: '' }), 'https://bg.local/' + sc.photo);
  await p.waitForTimeout(ex.slot ? 3000 : 1200);
  if (design !== 'info') {
    const css = readFileSync(HERE + '/' + design + '.css', 'utf8') + readFileSync(HERE + '/bar.css', 'utf8');
    const js = readFileSync(HERE + '/_common.js', 'utf8') + '\n' + (existsSync(HERE + '/' + design + '.js') ? readFileSync(HERE + '/' + design + '.js', 'utf8') : '');
    await p.addStyleTag({ content: css });
    await p.evaluate(([js, sc, opt, ex]) => { window.SD = { ...ex,  glass: sc.glass && 'https://bg.local/' + sc.glass, fg: sc.fg && 'https://bg.local/' + sc.fg, fgTop: sc.fgTop, fgH: sc.fgH, top: sc.top, size: sc.size, opt }; (0, eval)('{' + js + '}'); }, [js + '\n' + BARJS, sc, opt || '', ex]);
  }
  await p.waitForTimeout(900);
  writeFileSync(OUTD + '/' + name + '.png', await p.screenshot());
  if (errs.length) console.log(name, errs.join('; '));
  console.log('wrote', name);
  await ctx.close();
}
await browser.close();
