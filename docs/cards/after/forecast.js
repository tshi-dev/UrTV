forecastRows = function(){
  const today = cityToday(), tomorrow = new Date(Date.parse(today + 'T12:00:00Z') + DAY_MS).toISOString().slice(0, 10);
  const days = FORECAST.filter(d => d.date >= today).slice(0, 5);
  if(!days.some(d => d.date === today)) return [];
  const lo = Math.min(...days.map(d => d.lo)), span = Math.max(1, Math.max(...days.map(d => d.hi)) - lo);
  return days.map(d => {
    const day = d.date === today ? 'اليوم' : d.date === tomorrow ? 'غداً'
      : new Intl.DateTimeFormat(GREG, {timeZone:'UTC', weekday:'long'}).format(new Date(d.date + 'T12:00:00Z'));
    const a = (d.lo - lo) / span * 100, b = (d.hi - d.lo) / span * 100;
    return `<li class="fc"><span>${day}</span><span class="wxi">${wxIcon(d.code, false)}</span><span class="ep">${WX_CODES[d.code] || ''}</span>`
         + `<span class="lo">${ar(Math.round(d.lo))}°</span><span class="rg"><i style="right:${a.toFixed(0)}%;width:${b.toFixed(0)}%"></i></span><span class="hi">${ar(Math.round(d.hi))}°</span></li>`;
  });
};
renderPanel(false);
