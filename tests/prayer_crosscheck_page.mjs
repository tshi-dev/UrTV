// The page's computePrayers (src/template.html, built into dist/tv.html) against tests/prayer_crosscheck.tsv: the times of every city of the app
// from the independent reference (a port of adhan-js, see tests/prayer_crosscheck_gen.mjs). Each of the six times may differ by one minute.
// Also: the page's Ramadan (Umm al-Qura's Isha +30) agrees with the file's flag, which comes from the official month table.
//   node tests/prayer_crosscheck_page.mjs        (CHROMIUM=/path/to/chromium if Playwright has no browser of its own)
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const rows = readFileSync(path.join(ROOT, 'tests/prayer_crosscheck.tsv'), 'utf8').split(/\r?\n/).filter(l => l && !l.startsWith('#')).map(l => l.split('\t'));
const TOLERANCE = 1;
const mins = t => t.split(' ').map(x => +x.slice(0, 2) * 60 + +x.slice(3));
const NAMES = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };

// one page per zone and method kind (the Ja'fari and Omani Gulf-method rows are the same zone with #pm=jafari / #pm=gulf)
const groups = new Map();
const pmOf = r => r[6] === '0' ? 'jafari' : (r[6] === '8' && r[0] === 'Asia/Muscat') ? 'gulf' : '';   // the rows chosen by hand (#pm=): the others are the zone's own
for (const r of rows) { const k = r[0] + '|' + pmOf(r); (groups.get(k) || groups.set(k, []).get(k)).push(r); }
let done = 0, skipped = 0, badRows = 0, wrongMethod = 0, wrongRamadan = 0, rebased = 0;
const driftZones = new Set();
const worst = [0, 0, 0, 0, 0, 0], detail = [];
for (const [key, list] of groups) {
  const [zone, kind] = key.split('|');
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(new Date('2026-10-23T09:00:00Z'));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#tz=' + encodeURIComponent(zone) + (kind ? '&pm=' + kind : '') + '&gap=0');
  await p.waitForFunction(() => typeof computePrayers === 'function' && typeof CONFIG === 'object', null, { timeout: 8000 });
  for (const r of list) {
    const [, name, lat, lon, date, ram, method, noon, want] = r;
    await p.clock.setFixedTime(new Date(date + 'T09:00:00Z'));
    const got = await p.evaluate(([la, lo]) => {
      CONFIG.latitude = +la; CONFIG.longitude = +lo;
      const d = riyadhParts(), n = Date.UTC(d.y, d.mo - 1, d.d, 12), t = computePrayers();
      return { m: CONFIG.prayerMethod, zone: CONFIG.timezone, off: Math.round(tzOffsetMs(n - tzOffsetMs(n)) / 60000), ram: hijriMonthNow(true) === 9,
               t: [t.Fajr, t.Sunrise, t.Dhuhr, t.Asr, t.Maghrib, t.Isha].join(' ') };
    }, [lat, lon]);
    if (got.zone !== zone) { skipped++; continue; }                 // a zone this browser does not know at all (the page falls back to Riyadh)
    // this browser's time-zone data gives the zone another offset than the data that wrote the file (Morocco left UTC+1 in tzdata 2026c):
    // the sun is the same, only the clock moved, so the reference is re-based by the difference, and counted apart from formula errors
    const shift = got.off - +noon;
    if (shift) { rebased++; driftZones.add(zone); }
    done++;
    if (got.m !== +method) { wrongMethod++; detail.push(`${name} ${zone}: page picks method ${got.m}, reference ${method}`); }
    if (+method === 4 && got.ram !== (ram === '1')) { wrongRamadan++; detail.push(`${name} ${date}: page says Ramadan=${got.ram}, file ${ram}`); }
    const a = mins(got.t), b = mins(want).map(m => m + shift);
    let bad = false;
    a.forEach((m, i) => { const dm = Math.abs(m - b[i]); worst[i] = Math.max(worst[i], dm); if (dm > TOLERANCE) { bad = true; if (detail.length < 40) detail.push(`${name} ${zone} ${date} m${method} ${NAMES[i]}: page ${got.t.split(' ')[i]} vs reference ${want.split(' ')[i]}`); } });
    if (bad) badRows++;
  }
  if (errors.length) { badRows++; detail.push(zone + ': ' + errors.join('; ')); }
  await ctx.close();
}
console.log(`rows ${done}, skipped for a zone this browser does not know ${skipped}; worst difference in minutes: ` + NAMES.map((n, i) => `${n} ${worst[i]}`).join(', '));
console.log(`time-zone data: this browser ${rebased ? `re-based ${rebased} rows for its own offsets (${[...driftZones].join(', ')}), not formula errors` : 'gives every row the offset the file was written with'}`);
detail.slice(0, 40).forEach(l => console.log('  ' + l));
check(`page computePrayers vs the reference: every time of ${done} city-days within ${TOLERANCE} minute`, badRows === 0 && wrongMethod === 0, `${badRows} rows differ more, ${wrongMethod} wrong methods`);
check('page: Umm al-Qura Ramadan (Isha +30) is on exactly the days of the official Ramadan', wrongRamadan === 0, String(wrongRamadan));
check('page crosscheck: at most 2% of the rows skipped for time-zone data', skipped * 50 <= done + skipped, `${skipped} of ${done + skipped}`);

// the same page against the timetables four authorities printed (tests/prayer_published.tsv), each within its own margin
{
  const pub = readFileSync(path.join(ROOT, 'tests/prayer_published.tsv'), 'utf8').split(/\r?\n/).filter(l => l && !l.startsWith('#')).map(l => l.split('\t'));
  const bad = [];
  let n = 0, w = 0;
  for (const zone of new Set(pub.map(r => r[1]))) {
    const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 } });
    await ctx.route(/^https?:/, r => r.abort());
    const p = await ctx.newPage();
    await p.clock.setFixedTime(new Date('2026-10-23T09:00:00Z'));
    await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#tz=' + encodeURIComponent(zone) + '&gap=0');
    await p.waitForFunction(() => typeof computePrayers === 'function' && typeof CONFIG === 'object', null, { timeout: 8000 });
    for (const [name, , lat, lon, method, tol, , date, want] of pub.filter(r => r[1] === zone)) {
      await p.clock.setFixedTime(new Date(date + 'T09:00:00Z'));
      const got = await p.evaluate(([la, lo]) => { CONFIG.latitude = +la; CONFIG.longitude = +lo; const t = computePrayers(); return { m: CONFIG.prayerMethod, t: [t.Fajr, t.Sunrise, t.Dhuhr, t.Asr, t.Maghrib, t.Isha].join(' ') }; }, [lat, lon]);
      n++;
      if (got.m !== +method) bad.push(`${name}: method ${got.m}`);
      mins(got.t).forEach((m, i) => { const d = Math.abs(m - mins(want)[i]); w = Math.max(w, d); if (d > +tol) bad.push(`${name} ${date} ${NAMES[i]}: page ${got.t.split(' ')[i]} vs printed ${want.split(' ')[i]}`); });
    }
    await ctx.close();
  }
  check(`page computePrayers vs the printed timetables of Makkah, Doha, Dubai and Kuwait: ${n} days within each source's own margin (worst ${w} min)`, n === 48 && !bad.length, bad.slice(0, 4).join('; '));
}
await browser.close();
process.exit(failed ? 1 : 0);
