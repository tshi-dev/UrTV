// 4.4 beta 3 (two users on 4.3.x, a Google TV on Android 14: the screensaver stayed black with «الطبيعة» or «صوري فقط» chosen): a photo that does
// not load never leaves the screen empty. A pack whose photos all fail (not downloaded, unreadable) and «صوري فقط» with unreadable photos both
// fall back to a bundled photo; a pack that loads shows its own. dist/tv.html in a real browser.   node tests/pack_fallback.test.mjs
import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const JPEG = Buffer.from('/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=', 'base64');

const browser = await chromium.launch();
async function shown(hash, setup, { failPacks = false, failOwn = false } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  await ctx.route('**/*', r => {
    const u = r.request().url();
    if (u.startsWith('https://packs.urtv.invalid/')) return failPacks || !u.endsWith('.jpg') ? r.abort() : r.fulfill({ contentType: 'image/jpeg', body: JPEG });
    if (u.startsWith('https://own.urtv.invalid/')) return failOwn ? r.abort() : r.fulfill({ contentType: 'image/jpeg', body: JPEG });
    if (u.startsWith('file:') || u.startsWith('data:')) return r.continue();
    return r.abort();   // offline: no hosted photos, no data
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(String(e)));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#' + hash);
  await p.waitForTimeout(800);
  await p.evaluate(setup);
  await p.evaluate(() => applyBackground(true));
  let bg = '';
  for (let i = 0; i < 40 && !bg; i++) { await p.waitForTimeout(250); bg = await p.evaluate(() => document.querySelector('.photo').style.backgroundImage); }
  await ctx.close();
  return { bg, errors };
}
const bundled = bg => bg.includes('/assets/backgrounds/') || bg.includes('data:image');

let r = await shown('ours=0', () => { CONFIG.packs = [['nature', 3]]; CONFIG.ours = false; }, { failPacks: true });
check('«الطبيعة» chosen, «صورنا» off, every pack photo fails (not downloaded): a bundled photo shows, never a black screen', bundled(r.bg) && !r.errors.length, r.bg + ' ' + r.errors.join(' '));
r = await shown('ours=0', () => { CONFIG.packs = [['nature', 3]]; CONFIG.ours = false; });
check('…and when the pack loads, its own photo shows', r.bg.includes('packs.urtv.invalid/nature/'), r.bg);
r = await shown('own=3&ownmode=only', () => {}, { failOwn: true });
check('«صوري فقط» with photos that cannot be read: a bundled photo shows, never a black screen', bundled(r.bg) && !r.errors.length, r.bg + ' ' + r.errors.join(' '));
r = await shown('own=0&ownmode=only', () => {});
check('«صوري فقط» with no photos at all: our photos show (only is only for photos that exist)', r.bg !== '' && !r.errors.length, r.bg);
await browser.close();
process.exit(failed ? 1 : 0);
