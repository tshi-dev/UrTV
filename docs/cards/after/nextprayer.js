nextPrayerRows = function(){
  if(!PRAYERS.length) return [];
  const p = riyadhParts(), mins = p.h * 60 + p.mi + p.s / 60;
  let i = PRAYERS.findIndex(x => x.h * 60 + x.mi > mins), tomorrow = false;
  if(i < 0){ i = 0; tomorrow = true; }
  const n = PRAYERS[i], at = n.h * 60 + n.mi + (tomorrow ? 1440 : 0), pr = PRAYERS[(i + PRAYERS.length - 1) % PRAYERS.length];
  const from = pr.h * 60 + pr.mi - (i === 0 && !tomorrow ? 1440 : 0), done = Math.max(0, Math.min(100, (mins - from) / (at - from) * 100));
  const iq = n.key === 'Dhuhr' && isFriday() ? 0 : CONFIG.iqama[n.key] || 0, q = n.h * 60 + n.mi + iq;
  return [`<li class="np"><b>${prayerName(n)}</b><span class="t">${fmtTime(n.h, n.mi)}</span><span class="r">بعد ${untilAdhan(at - mins)}</span>`
    + (iq ? `<span class="iq">الإقامة ${fmtTime(Math.floor(q / 60) % 24, q % 60)}</span>` : '') + `<span class="pb"><i style="width:${done.toFixed(1)}%"></i></span></li>`];
};
renderPanel(false);
