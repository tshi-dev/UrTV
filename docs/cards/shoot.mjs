// The card review's pictures (docs/cards/REVIEW.md): each card as it is today and as proposed, on the real page (dist/tv.html).
//   python3 src/build.py && node docs/cards/shoot.mjs [card-id ...]
// Offline: the network is answered with demo data; the photos come from photos/ and the 433-design scene. Without the private Thmanyah
// font, FONT_DIR (an open Arabic font, IBM Plex Sans Arabic woff2 files) stands in for it in both «before» and «after».
// «after» = the same page with docs/cards/after/<id>.css (and .js) added: the CSS that would go into src/template.html.
import { chromium } from 'playwright';
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const HERE = path.join(ROOT, 'docs/cards');
const OUT = process.env.OUT || HERE;
const FONT_DIR = process.env.FONT_DIR || '';
const BG_DIR = process.env.BG_DIR || '';     // riyadh.jpg, riyadh-fg.webp, riyadh-glass.jpg (433-design's scenes/saudi)
const NOW = new Date('2026-10-22T14:00:00Z');   // Thursday 17:00 in Riyadh: Maghrib is next
const PHOTO = { typical: 'wTmGtmGQCjQ.jpg', bright: '0SUho_B0nus.jpg', dark: '0rvKw0fDiHk.jpg' };
const hdr = { 'Access-Control-Allow-Origin': '*' };
const SAUDI = JSON.parse(readFileSync(path.join(ROOT, 'tests/fixtures/saudi-pro-league.json'), 'utf8'));

const weather = () => {   // Open-Meteo for Riyadh (UTC+3), 48 hours from NOW
  const t0 = Math.floor((NOW.getTime() + 3 * 36e5) / 36e5) * 36e5, hr = { time: [], temperature_2m: [], weather_code: [], precipitation_probability: [] };
  const codes = [1, 1, 2, 2, 0, 0, 0, 0, 0, 1, 2, 3];
  for (let k = 0; k < 48; k++) { hr.time.push(new Date(t0 + k * 36e5).toISOString().slice(0, 16)); hr.temperature_2m.push(33 - Math.min(k, 10) * .9);
    hr.weather_code.push(codes[k % codes.length]); hr.precipitation_probability.push(0); }
  const days = [0, 1, 2, 3, 4, 5, 6].map(i => new Date(t0 + i * 864e5).toISOString().slice(0, 10));
  return JSON.stringify({ current: { temperature_2m: 33, weather_code: 1, apparent_temperature: 34, relative_humidity_2m: 18, wind_speed_10m: 14 }, hourly: hr,
    daily: { time: days, temperature_2m_max: [36, 35, 34, 36, 37, 35, 33], temperature_2m_min: [22, 21, 21, 23, 24, 22, 20], weather_code: [1, 2, 0, 0, 3, 61, 1],
             sunrise: days.map(d => d + 'T05:55'), sunset: days.map(d => d + 'T17:21'), precipitation_probability_max: [0, 0, 0, 0, 10, 40, 0] } });
};
const chart = (price, closes, prev) => JSON.stringify({ chart: { result: [{ meta: { regularMarketPrice: price, chartPreviousClose: prev ?? closes[0] }, indicators: { quote: [{ close: closes }] } }] } });
const MK = { '^TASI.SR': chart(11512.4, [11420, 11470, 11512.4]), '2222.SR': chart(26.3, [26.5, 26.2, 26.3]), '^GSPC': chart(6800.2, [6850, 6900, 6800.2]),
  'GC=F': chart(4010, [3950, 3980, 4010]), 'SI=F': chart(50, [49, 50, 50]), 'BZ=F': chart(82.34, [80, 82.34]), '1120.SR': chart(98.1, [97, 98.1]),
  'AAPL': chart(231.5, [229, 231.5]), 'NVDA': chart(181.2, [184, 181.2]) };

function net(route) {
  const u = new URL(route.request().url()), s = u.href;
  const ok = body => route.fulfill({ contentType: 'application/json', headers: hdr, body: typeof body === 'string' ? body : JSON.stringify(body) });
  let m;
  if ((m = s.match(/assets\/fonts\/(Thmanyah[^/]*)\.woff2$/)) && FONT_DIR) {
    const w = /Light/.test(m[1]) ? 300 : /Medium/.test(m[1]) ? 500 : /Display|Bold/.test(m[1]) ? 700 : 400;
    return route.fulfill({ path: path.join(FONT_DIR, `ibm-plex-sans-arabic-arabic-${w}-normal.woff2`), headers: hdr });
  }
  if ((m = s.match(/\/master\/((assets\/(backgrounds|fonts)|photos)\/.+)$/))) {
    const f = path.join(ROOT, m[1]);
    return existsSync(f) ? route.fulfill({ path: f, headers: hdr }) : route.abort();
  }
  if (u.host === 'bg.local') return route.fulfill({ path: path.join(BG_DIR, u.pathname.slice(1)), headers: hdr });
  if (/open-meteo/.test(u.host)) return ok(weather());
  if ((m = s.match(/chart\/([^?]+)\?/))) return ok(MK[decodeURIComponent(m[1])] || chart(100, [99, 100]));
  if ((m = s.match(/coingecko.*ids=([\w,-]+)/))) return ok(Object.fromEntries(m[1].split(',').map(k => [k, k === 'bitcoin' ? { usd: 67000.4, usd_24h_change: -1.23 } : { usd: 2.35, usd_24h_change: 3.1 }])));
  if (u.pathname.endsWith('/leagues/saudi-arabian-pro-league.json')) return ok(SAUDI);
  if (/thesportsdb/.test(u.host)) {
    if (/\/images\//.test(s)) return route.abort();   // no club logos in the review
    const id = u.searchParams.get('id');
    if (u.pathname.endsWith('eventsnext.php')) return ok({ events: id === '136013' ? SAUDI.events.filter(e => e.idHomeTeam === '136013' || e.idAwayTeam === '136013').slice(-2) : null });
    if (u.pathname.endsWith('eventslast.php')) return ok({ results: null });
    return ok({ events: null, results: null });
  }
  return route.abort();
}

// the launcher's default card material (GlassStyle: the cards' «glass» fill and line, HomeCard.glassQuery)
const HOME_GLASS = '&hpanel=' + encodeURIComponent('rgba(255,255,255,0.141)') + '&hline=' + encodeURIComponent('rgba(255,255,255,0.278)');
const extra = (id, ext) => { const f = path.join(HERE, 'after', id + ext); return existsSync(f) ? readFileSync(f, 'utf8') : ''; };

// The TV's WebViews lay out in dp: a 1080p TV at density 2 is a 960x540 page drawn at 2x (the CI emulator's wm density 320), so every
// picture is that page at deviceScaleFactor 2 = 1920x1080 pixels, what the TV shows.
// a home: the photo (or the glass-clock scene), a plain or glass clock at the right, the card where the launcher puts it (48, 84 dp; 461x250 dp)
const homeHtml = (src, bg, w, h) => `<!doctype html><meta charset=utf-8><style>
html,body{margin:0;width:960px;height:540px;overflow:hidden;background:#000;font-family:Thmanyah,sans-serif}
.bg{position:absolute;inset:0;background:url('${bg.photo}') center/cover}
.veil{position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,.70),rgba(0,0,0,.25) 25%,rgba(0,0,0,.15) 50%,rgba(0,0,0,.55) 75%,rgba(0,0,0,.90))}   /* MainActivity's scrim (the dark look) */
.ck{position:absolute;right:48px;top:70px;font:700 190px/1 ThmanyahDisplay,Thmanyah,sans-serif;color:#fff;direction:ltr;text-shadow:0 2px 16px rgba(0,0,0,.35)}
.ck.glass{color:transparent;background:url('${bg.glass || ''}') center/960px 540px fixed;-webkit-background-clip:text;background-clip:text;text-shadow:none;-webkit-text-stroke:2px rgba(255,255,255,.55)}
.fg{position:absolute;inset:0;background:url('${bg.fg || ''}') center/cover}
iframe{position:absolute;left:48px;top:84px;width:${w}px;height:${h}px;border:0;background:transparent}
.dock{position:absolute;left:48px;right:48px;bottom:28px;height:76px;border-radius:12px;background:rgba(20,16,12,.55)}
</style><div class=bg></div><div class=veil></div><div class="ck${bg.glass ? ' glass' : ''}">٥:٠٠</div>${bg.fg ? '<div class=fg></div>' : ''}<div class=dock></div>
<iframe allowtransparency="true" src="${src}"></iframe>`;

async function shoot(browser, card, state, look, scale) {
  const ctx = await browser.newContext({ viewport: { width: 960, height: 540 }, deviceScaleFactor: scale, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, net);
  await ctx.addInitScript(i => { try { localStorage.setItem('photoIndex', i); } catch (e) {} }, readFileSync(path.join(ROOT, 'photos/index.json'), 'utf8'));
  if (card.init) await ctx.addInitScript(card.init);
  const p = await ctx.newPage();
  await p.clock.setFixedTime(NOW);
  const errs = []; p.on('pageerror', e => errs.push(e.message));
  const base = 'file://' + path.join(ROOT, 'dist/tv.html') + '#city=' + encodeURIComponent('الرياض') + '&lat=24.7136&lon=46.6753&gap=0&' + card.hash
             + (look.cardstyle ? '&cardstyle=' + look.cardstyle : '');
  let frame;
  if (look.home) {
    const src = base + '&widget=1' + (card.home ? '&' + card.home : '') + HOME_GLASS;
    const bg = look.glass ? { photo: 'https://bg.local/riyadh.jpg', glass: 'https://bg.local/riyadh-glass.jpg', fg: 'https://bg.local/riyadh-fg.webp' }
                          : { photo: 'https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/' + PHOTO[look.photo] };
    const tmp = path.join(tmpdir(), `urtv-card-home-${process.pid}.html`);   // a file page: a file iframe may sit in it
    writeFileSync(tmp, homeHtml(src, bg, card.w || 461, card.h || 250));
    await p.goto('file://' + tmp);
    await p.waitForTimeout(400);
    frame = p.frames().find(f => f.url().includes('/dist/tv.html'));
  } else {
    await p.goto(base);
    frame = p.mainFrame();
  }
  await frame.waitForFunction(() => typeof reveal === 'function' && document.fonts.status === 'loaded', null, { timeout: 6000 }).catch(() => errs.push('not loaded'));
  // the photo of the day is the page's own choice: the review shows the same photo before and after
  if (!look.home) await frame.evaluate(f => showBackground(PHOTO_BASE + f, { d: 1, c: 'NEOM · Unsplash' }), PHOTO[look.photo]);
  else await frame.evaluate(() => reveal());
  await frame.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 4000 }).catch(() => errs.push('never revealed'));
  if (card.run) await frame.evaluate(card.run);
  await p.waitForTimeout(card.wait || 1200);
  if (state === 'after') {
    const css = extra('_shared', '.css') + extra(card.id, '.css'), js = extra(card.id, '.js');
    if (css) await frame.addStyleTag({ content: css });
    if (js) await frame.evaluate(js);
    await p.waitForTimeout(500);
  }
  if (card.focus) await frame.evaluate(card.focus);
  await p.waitForTimeout(200);
  const clip = card.clip && card.clip(look);
  const buf = await p.screenshot(clip ? { clip } : {});
  await ctx.close();
  if (errs.length) console.log('  page errors:', card.id, errs.join('; '));
  return buf;
}

export { shoot, PHOTO, NOW };

const CARDS = (await import('./cards.mjs')).CARDS;
const want = process.argv.slice(2);
const browser = await chromium.launch();
mkdirSync(OUT, { recursive: true });
for (const card of CARDS) {
  if (want.length && !want.includes(card.id)) continue;
  for (const state of ['before', 'after']) {
    if (state === 'after' && !existsSync(path.join(HERE, 'after', card.id + '.css')) && !existsSync(path.join(HERE, 'after', card.id + '.js'))) continue;
    // the main picture: the first look at 2x; the others at 1x for the sheet
    for (const [i, look] of card.looks.entries()) {
      const buf = await shoot(browser, card, state, look, 2);
      const name = i === 0 ? `${card.nn}-${card.id}-${state}.png` : `${card.nn}-${card.id}-${state}-${look.tag}.png`;
      writeFileSync(path.join(i === 0 ? OUT : (process.env.SHEET || OUT), name), buf);
      console.log('wrote', name);
    }
  }
}
await browser.close();
