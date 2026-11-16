// SD.bar: prayers | match | weather | countdown | markets (none: no bar); SD.barAt: bottom | clock; SD.wx: off | usual | clock | corner
const BAR_TOP = () => Math.round(SD.top + SD.size * .98 + 62);
if (SD.bar) {
  let h = '';
  if (SD.bar === 'prayers') {
    const names = {Fajr:'الفجر', Sunrise:'الشروق', Dhuhr:isFriday() ? 'الجمعة' : 'الظهر', Asr:'العصر', Maghrib:'المغرب', Isha:'العشاء'};
    const at = k => { const [x, y] = String(DAY_TIMES[k]).split(':').map(Number); return x * 60 + y; };
    const p = riyadhParts(), now = p.h * 60 + p.mi, nk = Object.keys(names).find(k => k !== 'Sunrise' && at(k) > now);
    for (const k of Object.keys(names)) { const [x, y] = String(DAY_TIMES[k]).split(':').map(Number);
      h += `<div class="c${k === nk ? ' next' : at(k) <= now ? ' past' : ''}"><b>${names[k]}</b><i>${fmtTime(x, y)}</i>${k === nk ? '<em>' + txt('prayer-count') + '</em>' : ''}</div>`; }
  } else {
    const blk = document.querySelector('#match-row .mblock'), big = blk ? blk.querySelector('.big').innerHTML : '', sub = blk ? (blk.querySelector('.sub') || {}).innerHTML || '' : '';
    const k = SD.bar === 'weather' ? 'الطقس' : txt('slot-label');
    const v = SD.bar === 'weather' ? `${txt('wx-temp')} ${txt('wx-desc')}` : big;
    const r = SD.bar === 'weather' ? txt('wx-range').replace(/^[،\s]+/, '') : sub;
    h = `<div class="one"><span class="k">${k}</span><span class="v">${v}</span><span class="r">${r}</span></div>`;
  }
  const b = put('sd-bar ' + (SD.barAt === 'clock' ? 'at-clock' : 'at-bottom'), h);
  if (SD.barAt === 'clock') b.style.top = BAR_TOP() + 'px';
}
if (SD.wx && SD.wx !== 'off') {
  const w = `<b>${txt('wx-temp')}</b> ${txt('wx-desc')} <span>${txt('wx-range').replace(/^[،\s]+/, '')}</span>`;
  if (SD.wx === 'usual') H.classList.add('sd-wx-usual');
  else if (SD.wx === 'clock') put('sd-wx at-clock', w, `top:${Math.round(SD.top + SD.size * .98 + (document.querySelector('.sd-line .n') ? 56 : 32))}px`);
  else put('sd-wx at-corner', w);
}
