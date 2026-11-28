// 4.4 beta 4: «شريط الأسواق» turned off reaches a screensaver already on the screen. The app calls window.urtvMkBar(on) on the running page
// (ScreenDream, PagePush; tests/page_push_test.py); here, in a real browser and offline, the page takes it: the line hides, no markets request
// is made while it is off, it shows again and asks for its prices when turned on, and the answer is true (else the app builds the page again).
//   node tests/mkbar_push.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });
for (const file of ['tv.html', 'tv-en.html']) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const asked = [];
  await ctx.route(/^https?:/, r => { const u = r.request().url(); if (/yahoo|coingecko|markets|stooq/i.test(u)) asked.push(u); return r.abort(); });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#city=' + encodeURIComponent('الرياض') + '&lat=24.6877&lon=46.7219&tz=Asia/Riyadh&mkbar=on&gap=0');
  await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 6000 }).catch(() => errors.push('never revealed'));
  await p.waitForTimeout(400);
  const shown0 = await p.evaluate(() => !document.querySelector('.mk').hidden);
  check(`${file}: the markets line shows at the start (#mkbar=on)`, shown0);
  const r1 = await p.evaluate(() => window.urtvMkBar ? urtvMkBar(false) : 'missing');
  const off = await p.evaluate(() => document.querySelector('.mk').hidden);
  check(`${file}: urtvMkBar(false) answers true and hides the line at once`, r1 === true && off, String(r1) + ' ' + off);
  asked.length = 0;
  await p.evaluate(() => { BUSY.markets = false; return loadMarkets(); }).catch(() => {});
  await p.waitForTimeout(300);
  const offAsks = asked.filter(u => !/\.SR|stocks/i.test(u)).length;
  check(`${file}: while it is off, a markets run asks nothing for the line`, offAsks === 0, asked.join(' '));
  const r2 = await p.evaluate(() => urtvMkBar(true));
  await p.waitForTimeout(400);
  const on = await p.evaluate(() => !document.querySelector('.mk').hidden);
  check(`${file}: urtvMkBar(true) shows the line again and asks for its prices`, r2 === true && on && asked.length > 0, String(r2) + ' ' + on + ' ' + asked.length);
  check(`${file}: no page error`, errors.length === 0, errors.join(' | '));
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
