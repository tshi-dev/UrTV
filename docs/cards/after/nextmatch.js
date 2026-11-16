CARDS.forEach(c => c.kind === 'nextmatch' && c.el.classList.add('nm1'));
{ const wd = ms => new Intl.DateTimeFormat('ar', {timeZone: CONFIG.timezone, weekday: 'long'}).format(new Date(ms));
  document.querySelectorAll('.week.nm1 .mwhen').forEach(w => { const [day, time] = w.textContent.split(' · '), n = day.split(' ')[0];
    w.textContent = (n === wd(Date.now()) ? 'اليوم' : n === wd(Date.now() + 864e5) ? 'غدًا' : n) + (time ? ' ' + time : ''); });
  document.querySelectorAll('.week.nm1 .dt').forEach(d => { d.textContent = d.textContent.replace(/^بعد ١ يوم$/, 'بعد يوم'); }); }
