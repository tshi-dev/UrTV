// 4.3.3 «التاريخ بالأرقام» (#datenum=1): the screensaver's two date lines as day/month/year, in the digits chosen; without it, as they were.
//   node tests/date_num.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
async function dates(file, hash){
  const ctx = await browser.newContext({ timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage(), errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(Date.parse('2026-11-16T09:00:00Z'));   // Monday 16 November 2026, 6 Jumada al-Akhirah 1448
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#city=x&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&' + hash);
  await p.waitForTimeout(800);
  const r = await p.evaluate(() => [document.getElementById('hijri').textContent, document.getElementById('greg').textContent]);
  await ctx.close();
  return { r, errors };
}
let t = await dates('tv.html', 'datenum=1');
check('#datenum=1, Arabic digits: «الاثنين، ٠٦/٠٦/١٤٤٨ هـ» and «الاثنين، ١٦/١١/٢٠٢٦»', t.r[0] === 'الاثنين، ٠٦/٠٦/١٤٤٨ هـ' && t.r[1] === 'الاثنين، ١٦/١١/٢٠٢٦' && !t.errors.length, JSON.stringify(t));
t = await dates('tv.html', 'datenum=1&digits=latin');
check('#datenum=1 with Latin digits: the same numbers, Latin', t.r[0] === 'الاثنين، 06/06/1448 هـ' && t.r[1] === 'الاثنين، 16/11/2026', JSON.stringify(t));
t = await dates('tv.html', '');
check('without it the lines are in words, as they were', t.r[0] === 'الاثنين، ٦ جمادى الآخرة ١٤٤٨ هـ' && t.r[1] === 'الاثنين، ١٦ نوفمبر ٢٠٢٦', JSON.stringify(t));
t = await dates('tv-en.html', 'datenum=1&lang=en');
check('English page: «Monday, 06/06/1448 AH» and «Monday, 16/11/2026»', t.r[0] === 'Monday, 06/06/1448 AH' && t.r[1] === 'Monday, 16/11/2026' && !t.errors.length, JSON.stringify(t));
await browser.close();
console.log(failed ? `${failed} failed` : 'all passed');
process.exit(failed ? 1 : 0);
