// 4.4 «نظيفة» on the screensaver with depth (the owner: the scenery in front of the glass clock, as approved): the photo's own cut-out from
// urtv-data masks/, only with «نظيفة», not on a weak TV, only in the layouts its gate lists, never off its own pixels (a cut-out moved 6 px is
// refused), never where the digits would not read (unless owner_ok); the photo held still under it.
//   python3 src/build.py && node tests/saver_depth.test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const PHOTO = 'https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/anozjF6uqI0.jpg';
const H = { 'Access-Control-Allow-Origin': '*' };

async function run(hash, rec, fg = 'anozjF6uqI0-fg.webp') {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(); asked.push(u);
    if (u === PHOTO) return r.fulfill({ path: path.join(ROOT, 'photos/anozjF6uqI0.jpg'), headers: H, contentType: 'image/jpeg' });
    if (u.endsWith('/masks/photos/index.json')) return r.fulfill({ body: JSON.stringify({ files: { anozjF6uqI0: rec } }), headers: H, contentType: 'application/json' });
    if (u.endsWith('/masks/photos/anozjF6uqI0-fg.webp')) return r.fulfill({ path: path.join(ROOT, 'tests/fixtures/masks', fg), headers: H, contentType: 'image/webp' });
    const m = u.match(/\/master\/(assets\/fonts\/.+)$/); if (m) return r.fulfill({ path: path.join(ROOT, m[1]), headers: H }).catch(() => r.abort());
    return r.abort();
  });
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&panel=verse' + hash);
  await p.waitForFunction(() => typeof showBackground === 'function', null, { timeout: 8000 });
  await p.evaluate(u => { showBackground(u, { c: '' }); if (typeof designTick === 'function') designTick(); }, PHOTO);
  await p.waitForTimeout(2500);
  const r = await p.evaluate(() => { const e = document.querySelector('body > .sd-fg'); return { fg: !!e, box: e ? [e.style.left, e.style.top, e.style.width, e.style.height] : null,
    still: document.documentElement.classList.contains('sd-still'), glass: document.documentElement.classList.contains('sd-glass') }; });
  await ctx.close();
  return { r, errors, masks: asked.filter(u => u.includes('/masks/')).length };
}
const REC = JSON.parse(readFileSync(path.join(ROOT, 'tests/fixtures/masks/anozjF6uqI0.json'), 'utf8'));
{
  const { r, errors } = await run('&design=clean', REC);
  check('«نظيفة»: the photo\'s own cut-out over the clock at its box (0,466,1920,312 of 1080p), the photo held still, the glass digits; no script errors',
    r.fg && r.still && r.glass && r.box[0] === '0vw' && r.box[1].startsWith('43.14') && r.box[2] === '100vw' && r.box[3].startsWith('28.88') && !errors.length, JSON.stringify(r) + errors.join('; '));
}
{
  const { r } = await run('&design=clean', REC, 'shifted-fg.webp');
  check('a cut-out off its own pixels (moved 6 px) is never drawn: the clock in front', !r.fg && !r.still, JSON.stringify(r));
}
{
  const { r } = await run('&design=clean', Object.assign({}, REC, { depth: ['couch'] }));
  check('its gate lists only «couch»: none on the screensaver\'s «نظيفة»', !r.fg, JSON.stringify(r));
}
{
  const { r } = await run('&design=clean', Object.assign({}, REC, { box: [0, 200, 1920, 578] }));
  check('a cut-out placed so it would cover the digits past a quarter: not drawn (the clock in front)', !r.fg, JSON.stringify(r));
}
{
  const { r } = await run('&design=clean', Object.assign({}, REC, { box: [0, 200, 1920, 578], owner_ok: true }), 'anozjF6uqI0-fg.webp');
  check('owner_ok waives the legibility rule (never the alignment one: here the cut-out is not on its pixels, so none)', !r.fg, JSON.stringify(r));
}
for (const [hash, what] of [['', 'the default design (info)'], ['&design=big', '«ساعة كبيرة»'], ['&design=clean&lowram=1', 'a weak TV'], ['&design=clean&widget=1', 'the launcher\'s card']]) {
  const { r, masks } = await run(hash, REC);
  check(`${what}: no cut-out, nothing asked for from masks/`, !r.fg && masks === 0, JSON.stringify(r) + masks);
}
await browser.close();
process.exit(failed ? 1 : 0);
