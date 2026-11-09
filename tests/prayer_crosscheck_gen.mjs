// Writes tests/prayer_crosscheck.tsv: the prayer times of every city in the app's list from tests/adhan_port.mjs, a port of the formulas of
// adhan-js 4.4.6 (Batoul Apps, MIT: Meeus' Astronomical Algorithms, three-day interpolation, its own rounding), as an independent reference
// for the short solar series that PrayerMath.java and template.html use.
//
//   node tests/prayer_crosscheck_gen.mjs > tests/prayer_crosscheck.tsv
//
// The test (tests/prayer_crosscheck_test.py) needs neither adhan nor the network: it reads the file this wrote. Run this again only when the
// city list, the dates or a method change; the diff of the .tsv is then the review.
//
// What it checks and what it does not: the astronomy, the rounding, the time zone / summer-time handling and the wiring of a zone to its
// method. The angles and margins of each method are the ones docs/SETTINGS.md and PrayerMath.java state (Aladhan's ids); adhan has Umm
// al-Qura, Dubai, Qatar, Kuwait and Egyptian of its own, and for the other methods (Gulf, Oman's ministry, Jordan, Morocco, Algeria, Tunisia) the
// same numbers are fed to the same formulas, so those numbers themselves are not independently confirmed here (Aladhan's list is not
// reachable from the cloud container that wrote this). adhan's +1 minute on Dhuhr for the Egyptian method is left out: Aladhan's method 5
// has no such margin and neither does the app (see notes/VERIFY-2026-10.md).
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { prayerTimes, METHODS } from './adhan_port.mjs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

// ---- the app's own city list (Cities.java: name, lat, lon[, zone]; no zone = Saudi Arabia)
const src = readFileSync(path.join(ROOT, 'android/app/src/main/java/sa/urtv/screensaver/Cities.java'), 'utf8');
const cities = [...src.matchAll(/\{"([^"]+)", "(-?[\d.]+)", "(-?[\d.]+)"(?:, "([^"]+)")?\}/g)]
  .map(m => ({ name: m[1], lat: +m[2], lon: +m[3], zone: m[4] || 'Asia/Riyadh' }));

// ---- methods, by Aladhan's id (the numbers of adhan's own methods where it has one)
const METHOD = {
  4: METHODS.ummAlQura,
  9: METHODS.kuwait,
  10: METHODS.qatar,
  16: METHODS.dubai,
  5: { ...METHODS.egyptian, adj: {} },
  8: { fajr: 19.5, isha: 0, ishaInterval: 90 },
  23: { fajr: 18, isha: 18, adj: { maghrib: 5 } },
  21: { fajr: 19, isha: 17, adj: { dhuhr: 5, maghrib: 5 } },
  19: { fajr: 18, isha: 17 },
  18: { fajr: 18, isha: 18 },
  0: { fajr: 16, isha: 14, maghribAngle: 4 },
  // 100 is ours, not Aladhan's: Oman's Ministry of Awqaf and Religious Affairs, Fajr 18, Isha 18, Dhuhr/Asr/Maghrib +5, as fitted by
  // egyjs/afnon-android-tv-launcher (MIT, Method.OMAN_AWQAF) to the ministry's 2026 timetable (adhan's own PrayerAdjustments(0, 0, 5, 5, 5, 0))
  100: { fajr: 18, isha: 18, adj: { dhuhr: 5, asr: 5, maghrib: 5 } },
};
const ZONE_METHOD = {
  'Asia/Riyadh': 4, 'Asia/Kuwait': 9, 'Asia/Qatar': 10, 'Asia/Dubai': 16, 'Asia/Bahrain': 8, 'Asia/Muscat': 100,
  'Africa/Cairo': 5, 'Asia/Damascus': 5, 'Asia/Beirut': 5, 'Asia/Gaza': 5, 'Asia/Hebron': 5,
  'Asia/Amman': 23, 'Africa/Casablanca': 21, 'Africa/Algiers': 19, 'Africa/Tunis': 18,
};

// ---- dates. Ramadan is the official Umm al-Qura month 9 (tests/hijri_crosscheck.tsv): 1447 = 2026-02-18..03-19, 1448 = 2027-02-08..03-08
const RAMADAN = [['2026-02-18', '2026-03-19'], ['2027-02-08', '2027-03-08']];
const isRamadan = d => RAMADAN.some(([a, b]) => d >= a && d <= b);
const SPREAD = ['2026-01-15', '2026-02-20', '2026-03-20', '2026-05-10', '2026-06-21', '2026-08-15', '2026-10-23', '2026-12-21',
                '2027-01-12', '2027-02-09', '2027-03-21', '2027-09-23'];
// the days the clocks change, per zone (2026 and 2027, from the zone database; Egypt and Lebanon at midnight, Palestine in the night,
// Morocco at 02:00/03:00 at the start and end of Ramadan), the day before and after, and the Ramadan edges of Umm al-Qura
const CHANGE = {
  'Africa/Cairo': ['2026-04-23', '2026-04-24', '2026-10-29', '2026-10-30', '2027-04-29', '2027-04-30', '2027-10-28', '2027-10-29'],
  'Asia/Beirut': ['2026-03-28', '2026-03-29', '2026-10-24', '2026-10-25', '2027-03-27', '2027-03-28', '2027-10-30', '2027-10-31'],
  'Asia/Hebron': ['2026-03-27', '2026-03-28', '2026-10-23', '2026-10-24', '2027-03-26', '2027-03-27', '2027-10-29', '2027-10-30'],
  'Asia/Gaza': ['2026-03-27', '2026-03-28', '2026-10-23', '2026-10-24', '2027-03-26', '2027-03-27', '2027-10-29', '2027-10-30'],
  // Morocco: the 2026 Ramadan edges, and 20 September 2026, from which tzdata 2026c keeps it at UTC+0 with no Ramadan switches (older
  // data keeps UTC+1 and the switches: the tests re-base by whichever this machine has)
  'Africa/Casablanca': ['2026-02-14', '2026-02-15', '2026-03-21', '2026-03-22', '2026-09-19', '2026-09-20'],
};
const RAMADAN_EDGES = ['2026-02-17', '2026-02-18', '2026-03-19', '2026-03-20', '2027-02-07', '2027-02-08', '2027-03-08', '2027-03-09'];
// a handful of Saudi cities also get the Ramadan edges (the 194 would only repeat each other; 4.3 A4: no Ja'fari rows)
const RAMADAN_CITIES = ['الرياض', 'مكة المكرمة', 'الدمام', 'أبها', 'تبوك', 'عرعر'];

// ---- the reference's instants as the clock of the city
const fmt = new Map();
const parts = (zone, t) => {
  if (!fmt.has(zone)) fmt.set(zone, new Intl.DateTimeFormat('en-GB', { timeZone: zone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }));
  return Object.fromEntries(fmt.get(zone).formatToParts(t).map(p => [p.type, p.value]));
};
const offsetMin = (zone, t) => { const p = parts(zone, t), u = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute); return Math.round((u - Math.floor(t / 60000) * 60000) / 60000); };
const clock = (zone, t, date) => {   // HH:MM of the instant; past midnight of `date` it goes on (24:30), as Isha does in the app
  const p = parts(zone, t), days = Math.round((Date.UTC(+p.year, +p.month - 1, +p.day) - Date.parse(date + 'T00:00:00Z')) / 86400000);
  const m = +p.hour * 60 + +p.minute + days * 1440;
  return String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0');
};

const rows = [];
const add = (c, date, method) => {
  const [y, mo, d] = date.split('-').map(Number), ram = isRamadan(date) && method === 4;
  const p = { ...METHOD[method], adj: { ...(METHOD[method].adj || {}) } };
  if (ram) p.adj.isha = (p.adj.isha || 0) + 30;   // Umm al-Qura, +30 minutes for Isha in Ramadan (adhan's own note)
  const t = prayerTimes(c.lat, c.lon, y, mo, d, p);
  const noonUtc = Date.UTC(y, mo - 1, d, 12), noon = offsetMin(c.zone, noonUtc - offsetMin(c.zone, noonUtc) * 60000);
  rows.push([c.zone, c.name, c.lat, c.lon, date, ram ? 1 : 0, method, noon,
    [t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha].map(x => clock(c.zone, x, date)).join(' ')].join('\t'));
};
for (const c of cities) {
  const m = ZONE_METHOD[c.zone];
  if (!m) throw new Error('no method for ' + c.zone);
  for (const date of [...SPREAD, ...(CHANGE[c.zone] || []), ...(m === 4 && RAMADAN_CITIES.includes(c.name) ? RAMADAN_EDGES : [])]) add(c, date, m);
  if (c.zone === 'Asia/Muscat') for (const date of SPREAD) add(c, date, 8);   // Oman's older default, still there as #pm=gulf
}

console.log('# Prayer times of every city in android/app/.../Cities.java from tests/adhan_port.mjs (a port of adhan-js 4.4.6, Batoul Apps, MIT), the independent reference for tests/prayer_crosscheck_test.py. Generated by tests/prayer_crosscheck_gen.mjs; do not edit by hand.');
console.log(`# Written with the time-zone data of this node (tzdata ${process.versions.tz}); where another machine's data gives a zone another offset the tests re-base those rows and say so.`);
console.log('# zone, city, lat, lon, date, ramadan (Umm al-Qura +30 for Isha), method (Aladhan id), the zone\'s UTC offset at local noon in minutes, then fajr sunrise dhuhr asr maghrib isha as the city\'s clock (24:30 = 00:30 the next day)');
for (const r of rows) console.log(r);
