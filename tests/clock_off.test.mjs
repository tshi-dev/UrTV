// 4.4 beta 4: a TV clock far from the network's (UrtvNative.clockOff() === '1', ClockCheck in the app) puts «ساعة التلفزيون غير مضبوطة» where
// the Gregorian date was (the English page: its own words); '0' or no app leaves the date as it was. A real browser, offline.
//   node tests/clock_off.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });
const greg = async (file, answer) => {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => r.abort());
  if (answer !== null) await ctx.addInitScript(a => { window.UrtvNative = { clockOff: () => a }; }, answer);
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#tz=Asia/Riyadh&gap=0');
  await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 6000 }).catch(() => errors.push('never revealed'));
  await p.waitForTimeout(300);
  const t = await p.evaluate(() => document.getElementById('greg').textContent);
  const cls = await p.evaluate(() => document.documentElement.classList.contains('clockoff'));
  await ctx.close();
  return { t, cls, errors };
};
const year = String(new Date().getUTCFullYear());
for (const [file, words] of [['tv.html', 'ساعة التلفزيون غير مضبوطة'], ['tv-en.html', 'The TV’s clock is not set']]) {
  const off = await greg(file, '1');
  check(`${file}: the clock is off: the date line says «${words}»`, off.t === words && off.cls, off.t);
  const ok = await greg(file, '0');
  check(`${file}: the clock is right: the date, not the words`, ok.t !== words && ok.t.length > 4 && !ok.cls, ok.t);
  const none = await greg(file, null);
  check(`${file}: no app (a browser): the date as before`, none.t === ok.t, none.t + ' | ' + ok.t);
  const unknown = await greg(file, '');
  check(`${file}: not known yet: the date as before`, unknown.t === ok.t, unknown.t);
  check(`${file}: no page error`, [off, ok, none, unknown].every(x => x.errors.length === 0), [off, ok, none, unknown].map(x => x.errors.join(',')).join(' | '));
}
await browser.close();
process.exit(failed ? 1 : 0);
