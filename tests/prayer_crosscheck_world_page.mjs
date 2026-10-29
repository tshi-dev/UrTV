// Actual Aladhan samples through the built page, with all network requests blocked.
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const { rows } = JSON.parse(readFileSync(path.join(ROOT, 'tests/prayer_crosscheck_world.json'), 'utf8'));
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const keys = ['Fajr','Sunrise','Dhuhr','Asr','Maghrib','Isha'];
const minutes = value => { const [h,m] = value.split(':').map(Number); return h*60+m; };
let failed = 0, worst = 0;
try {
  for (const city of new Set(rows.map(r => r.country+'|'+r.name_en))) {
    const samples = rows.filter(r => r.country+'|'+r.name_en === city), first = samples[0];
    const context = await browser.newContext();
    await context.route(/^https?:/, route => route.abort());
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.clock.setFixedTime(new Date('2026-06-21T09:00:00Z'));
    await page.goto('file://'+path.join(ROOT,'dist/tv.html')+'#tz='+encodeURIComponent(first.tz)+'&pm='+first.method+'&lat='+first.lat+'&lon='+first.lon+'&gap=0');
    await page.waitForFunction(() => typeof computePrayers === 'function');
    for (const row of samples) {
      const [d,m,y] = row.date.split('-');
      await page.clock.setFixedTime(new Date(`${y}-${m}-${d}T09:00:00Z`));
      const got = await page.evaluate(([lat,lon]) => {
        CONFIG.latitude=lat; CONFIG.longitude=lon;
        return {method:CONFIG.prayerMethod, times:computePrayers()};
      }, [row.lat,row.lon]);
      if (got.method !== row.method) { failed++; console.log('FAIL country method '+city); }
      const actual = keys.map(key => minutes(got.times[key]));
      const accepted = await page.evaluate(([timings,date]) => checkedTimings({data:{timings,date:{gregorian:{date}}}},date), [row.timings,row.date]);
      if (!accepted || minutes(accepted.Maghrib)<minutes(accepted.Asr) || minutes(accepted.Isha)<minutes(accepted.Maghrib)) {
        failed++; console.log('FAIL published night rollover '+city+' '+row.date);
      }
      for (let i=0;i<6;i++) {
        const delta0 = Math.abs(actual[i]%1440-minutes(row.timings[keys[i]]));
        const delta = Math.min(delta0,1440-delta0);
        worst = Math.max(worst,delta);
        if (!Number.isFinite(actual[i]) || delta>2 || i>0 && actual[i]<=actual[i-1]) {
          failed++; console.log(`FAIL ${city} ${row.date} ${keys[i]}: ${got.times[keys[i]]} vs ${row.timings[keys[i]]}`);
        }
      }
    }
    if (first.name_en === 'Oslo') {
      await page.clock.setFixedTime(new Date('2026-06-21T22:01:00Z')); // 22 June, 00:01 in Oslo
      const pending = await page.evaluate(() => {
        buildPrayers(computePrayers()); tickPrayer();
        const p=riyadhParts(), mins=p.h*60+p.mi;
        return PRAYERS.find(x => x.h*60+x.mi>mins).key;
      });
      await page.clock.setFixedTime(new Date('2026-06-21T22:40:00Z'));
      const later = await page.evaluate(() => {
        tickPrayer(); const p=riyadhParts(), mins=p.h*60+p.mi;
        return PRAYERS.find(x => x.h*60+x.mi>mins).key;
      });
      if (pending !== 'Isha' || later !== 'Fajr') { failed++; console.log('FAIL midnight Isha handoff '+pending+' / '+later); }
      else console.log('PASS page: last night’s Isha remains next after midnight, then Fajr');
    }
    if (errors.length) { failed++; console.log('FAIL browser errors '+errors.join('; ')); }
    await context.close();
  }
} finally { await browser.close(); }
console.log(`${failed ? 'FAIL' : 'PASS'} page: ${rows.length} published Aladhan city-days within 2 minutes (worst ${worst})`);
process.exit(failed ? 1 : 0);
