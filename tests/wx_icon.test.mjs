// 4.3.3: the screensaver's weather line has the condition's icon beside the temperature (the same drawings as the hourly strip,
// wxIcon), one line high, gone with the reading when it is too old.
//   node tests/wx_icon.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
async function line(code, iso){
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => r.request().url().includes('api.open-meteo.com')
    ? r.fulfill({ json: { current: { temperature_2m: 27.6, weather_code: code, visibility: 20000 }, daily: { time: [iso.slice(0, 10)], temperature_2m_max: [31], temperature_2m_min: [19], weather_code: [code] } }, headers: { 'Access-Control-Allow-Origin': '*' } })
    : r.abort());
  const p = await ctx.newPage(), errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(Date.parse(iso));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh');
  await p.waitForTimeout(1500);
  const r = await p.evaluate(() => {
    const i = document.getElementById('wx-ico'), t = document.getElementById('wx-temp'), s = i && i.querySelector('svg');
    const b = s ? s.getBoundingClientRect() : null, tb = t.getBoundingClientRect();
    const as = k => { const d = document.createElement('div'); d.innerHTML = WX_SVG[k]; return !!s && s.outerHTML === d.innerHTML; };
    return { w: b ? b.width : 0, th: tb.height, temp: t.textContent, sun: as('sun'), moon: as('moon'), rain: as('rain'),
             mid: b ? Math.abs((b.top + b.bottom) / 2 - (tb.top + tb.bottom) / 2) : 99 };
  });
  await ctx.close();
  return { r, errors };
}
let t = await line(0, '2026-11-16T09:00:00Z');   // noon in Riyadh, clear
check('a clear noon: the sun beside «٢٨°», about one line high and centred on it', t.r.sun && t.r.temp === '٢٨°' && t.r.w > 12 && t.r.w < t.r.th * 1.2 && t.r.mid < 8 && !t.errors.length, JSON.stringify(t));
t = await line(0, '2026-11-16T19:00:00Z');   // 22:00 in Riyadh
check('a clear night: the moon', t.r.moon, JSON.stringify(t));
t = await line(63, '2026-11-16T09:00:00Z');
check('rain: the rain drawing', t.r.rain, JSON.stringify(t));
await browser.close();
console.log(failed ? `${failed} failed` : 'all passed');
process.exit(failed ? 1 : 0);
