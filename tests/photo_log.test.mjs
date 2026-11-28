// 4.4 beta 4: photos stayed black on some Google TVs and nothing said why. The page now tells the app (UrtvNative.photoError: what failed and the
// file's name) when a photo does not load, and 8 s after the start, with no photo on the screen or one that does not load, it shows a bundled
// one that loads. Here, in a real browser: all photos served (nothing said), the hosted ones refused (said, a bundled one shows), every photo
// refused (said, and the guard says it found none that loads). Offline; the photos come from the repo as PhotoCache serves them on the TV.
//   node tests/photo_log.test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const INDEX = readFileSync(path.join(ROOT, 'photos/index.json'), 'utf8');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });
async function run(serve) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(), i = u.indexOf('/master/'), rel = i > 0 ? u.slice(i + 8).split('?')[0] : '';
    if (rel && serve(rel)) return r.fulfill({ path: path.join(ROOT, rel), headers: { 'Access-Control-Allow-Origin': '*' } });
    return r.abort();
  });
  await ctx.addInitScript(i => {
    try { localStorage.setItem('photoIndex', i); } catch (e) {}
    window.__said = [];
    window.UrtvNative = { photoError: (w, n) => window.__said.push(w + ':' + n) };
  }, INDEX);
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#tz=Asia/Riyadh&gap=0');
  await p.waitForTimeout(9500);
  const said = await p.evaluate(() => window.__said);
  const bg = await p.evaluate(() => document.querySelector('.photo').style.backgroundImage);
  await ctx.close();
  return { said, bg, errors };
}
const fonts = rel => /^assets\/fonts\//.test(rel);
const all = await run(rel => fonts(rel) || /^(assets\/backgrounds|photos)\//.test(rel));
check('every photo served: a photo shows and nothing is said', /url\(/.test(all.bg) && all.said.length === 0, all.bg + ' | ' + all.said.join(','));
const noHosted = await run(rel => fonts(rel) || /^assets\/backgrounds\//.test(rel));
check('the hosted photos refused: «hosted» is said with the file name (no address)', noHosted.said.some(x => /^hosted:[^/:]+\.(jpe?g|webp|png)$/i.test(x)), noHosted.said.join(','));
check('the hosted photos refused: a bundled photo shows', /assets\/backgrounds\//.test(noHosted.bg), noHosted.bg);
const none = await run(rel => fonts(rel));
check('every photo refused: the guard says the one shown does not load, then that no bundled one does', none.said.some(x => /^(shown|none):/.test(x)) && none.said.includes('bundled:'), none.said.join(','));
check('no page error', [all, noHosted, none].every(x => x.errors.length === 0), [all, noHosted, none].map(x => x.errors.join(',')).join(' | '));
await browser.close();
process.exit(failed ? 1 : 0);
