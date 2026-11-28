// 4.4 beta 4: the football home's widget cards with the focus are bigger and lifted (a 3 % scale and a deeper shadow), never ringed (an inset ring
// made the focused card look smaller); the card without the focus is as before. A real browser, offline, the launcher's page (#widget=1&football=1).
//   node tests/fb_focus.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });
for (const extra of ['', '&lowram=1']) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#tz=Asia/Riyadh&gap=0&widget=1&football=1' + extra);
  await p.waitForTimeout(1500);
  const look = () => p.evaluate(() => ['pr', 'wx'].map(k => { const c = document.querySelector('#fb-' + k + ' .fbc'); if (!c) return null; const s = getComputedStyle(c); return { t: s.transform, sh: s.boxShadow }; }));
  const rest = await look();
  await p.evaluate(() => window.fbFocus && fbFocus('pr'));
  await p.waitForTimeout(400);
  const on = await look();
  const tag = extra ? ' (lite)' : '';
  check(`the widgets are there${tag}`, rest.every(Boolean), JSON.stringify(rest));
  if (rest.every(Boolean)) {
    check(`at rest: no scale${tag}`, rest.every(x => x.t === 'none'), JSON.stringify(rest));
    check(`the focused card is 3 % larger${tag}`, /^matrix\(1\.03, 0, 0, 1\.03/.test(on[0].t), on[0].t);
    check(`the focused card has no ring: only the top light and its drop shadow${tag}`, (on[0].sh.match(/inset/g) || []).length === 1 && !/inset 0px 0px 0px/.test(on[0].sh), on[0].sh);
    check(`the other card stays as it was${tag}`, on[1].t === 'none' && on[1].sh === rest[1].sh, JSON.stringify(on[1]));
  }
  check(`no page error${tag}`, errors.length === 0, errors.join(' | '));
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
