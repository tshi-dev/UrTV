H.classList.add('sd-day');
const names = {Fajr:'الفجر', Sunrise:'الشروق', Dhuhr:'الظهر', Asr:'العصر', Maghrib:'المغرب', Isha:'العشاء'};
const at = k => { const [h, m] = String(DAY_TIMES[k]).split(':').map(Number); return h * 60 + m; };
const p = riyadhParts(), now = p.h * 60 + p.mi, a = at('Fajr') - 30, b = at('Isha') + 30, x = t => Math.max(0, Math.min(100, (t - a) / (b - a) * 100));
const nk = Object.keys(names).find(k => k !== 'Sunrise' && at(k) > now);
let h = `<div class="done" style="width:${x(now)}%"></div>`;
for (const k of Object.keys(names)) { const t = at(k), hm = String(DAY_TIMES[k]).split(':').map(Number);
  h += `<div class="p${t <= now ? ' past' : k === nk ? ' next' : ''}" style="right:${x(t)}%"><b>${names[k]}</b><i>${fmtTime(hm[0], hm[1])}</i></div>`; }
h += `<div class="now" style="right:${x(now)}%"></div>`;
if (nk) h += `<div class="left" style="right:${x(at(nk))}%">${txt('prayer-count')}</div>`;
put('sd-dl', h);
