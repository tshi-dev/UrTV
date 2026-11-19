// 4.4 beta 2 (the owner, build 570: «adding teams or leagues freezes, the search between clubs lags»), the page's side, in a real browser:
// the chooser of 800 leagues («أضف دوريًا أو فريقًا») draws only the rows around the focus (MP_WIN each side), and ▼ walks all of them with
// the focus on the right row; the club search draws once the typing rests, not at every key, and ▼ right after typing acts on the results.
//   node tests/add_lag.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
// a catalogue as big as the real one (842 leagues, 141 «top»), in 60 countries
const LEAGUES = Array.from({ length: 842 }, (_, i) => ({ id: String(5000 + i), en: 'League ' + i, ar: 'دوري ' + i, country: 'Land ' + (i % 60), country_ar: 'بلد ' + (i % 60), top: i < 141 ? 1 : 0 }));
const TEAMS = Object.fromEntries(Array.from({ length: 760 }, (_, i) => [String(140000 + i), { en: 'Club ' + i, ar: i === 7 ? 'النصر' : 'نادي ' + i, leagues: ['5000'] }]));

const browser = await chromium.launch();
async function open() {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(), ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    if (/catalogue\/catalogue\.json/.test(u)) return ok({ leagues: LEAGUES });
    if (/(?:data-archive|football\/archive)\/index\.json/.test(u)) return ok({ v: 1, teams: TEAMS, leagues: {} });
    if (/thesportsdb/.test(u)) return ok({ events: null, results: null, teams: null });
    return r.fulfill({ status: 404, body: '' });
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&matchpage=1');
  await p.waitForTimeout(800);
  return { p, ctx, errors };
}
{   // the chooser: 842 leagues in «كل الدول»
  const { p, ctx, errors } = await open();
  await p.evaluate(() => mpGo({ k: 'pick' }));
  await p.waitForTimeout(800);
  const n0 = await p.evaluate(() => [MP.rows.length, document.querySelectorAll('#mp-list li').length]);
  check('the chooser holds every league (842) but draws only the rows round the focus (at most 2 × MP_WIN)', n0[0] === 842 && n0[1] > 0 && n0[1] <= 120, JSON.stringify(n0));
  let bad = [], worst = 0;
  for (let k = 0; k < 70; k++) {
    const t0 = Date.now();
    await p.keyboard.press('ArrowDown');
    worst = Math.max(worst, Date.now() - t0);
    if (k % 10 === 9) {
      const r = await p.evaluate(() => { const f = document.querySelector('#mp-list li.f'), b = f && f.getBoundingClientRect();
        return { r: MP.r, data: f ? +f.dataset.r : -1, text: f ? f.textContent : '', want: MP.rows[MP.r] && MP.rows[MP.r].t, on: !!b && b.top >= 0 && b.bottom <= innerHeight + 1, drawn: document.querySelectorAll('#mp-list li').length }; });
      if (r.data !== r.r || !r.text.startsWith(r.want) || !r.on || r.drawn > 120) bad.push(r);
    }
  }
  const last = await p.evaluate(() => MP.r);
  check('▼ 70 times: the focus on the right row (its number and its name), on screen, the drawn rows never more than 120', !bad.length && last >= 60, JSON.stringify(bad.slice(0, 2)) + ' r=' + last);
  check('…and a key is answered quickly (each under 250 ms here)', worst < 250, worst + ' ms');
  const end = await p.evaluate(() => { MP.r = MP.rows.length - 1; mpRender(); const f = document.querySelector('#mp-list li.f'); return [f ? +f.dataset.r : -1, MP.rows.length - 1]; });
  check('the last league is reachable: the window moves to the end', end[0] === end[1], JSON.stringify(end));
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
}
{   // the search: typing draws once it rests; ▼ right away acts on the results
  const { p, ctx, errors } = await open();
  await p.evaluate(() => mpGo({ k: 'find' }));
  await p.waitForTimeout(300);
  const draws = await p.evaluate(() => { let n = 0; const real = mpRender; window.mpRender = mpRender = (...a) => { n++; return real(...a); }; window.__draws = () => n; return 0; });
  await p.keyboard.type('النصر', { delay: 40 });
  const during = await p.evaluate(() => window.__draws());
  await p.waitForTimeout(400);
  const after = await p.evaluate(() => [window.__draws(), [...document.querySelectorAll('#mp-list li')].map(l => l.textContent).join()]);
  check('typing five letters: no draw while typing, one when it rests, the club found', during === 0 && after[0] === 1 && after[1] === 'النصر', JSON.stringify([during, after]));
  await p.keyboard.type('x', { delay: 0 });
  await p.evaluate(() => { const q = document.getElementById('mp-q'); q.value = 'النصر'; q.dispatchEvent(new Event('input', { bubbles: true })); });
  await p.keyboard.press('ArrowDown');
  const z = await p.evaluate(() => [MP.zone, MP.rows.length]);
  check('▼ right after typing (the draw still waiting): drawn first, the focus goes to the result', z[0] === 'rows' && z[1] === 1, JSON.stringify(z));
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all add-lag tests passed');
