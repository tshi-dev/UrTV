{ // the date line: «الخميس ٢٢ أكتوبر · ٩:٠٠ م» -> «اليوم ٩:٠٠ م»; a finished game marked for its quieter look
  const wd = ms => new Intl.DateTimeFormat('ar', {timeZone: CONFIG.timezone, weekday: 'long'}).format(new Date(ms));
  const tidy = ul => ul.querySelectorAll('li.mg').forEach(li => {
    const w = li.querySelector('.mwhen'), d = li.querySelector('.dt');
    if(d && d.textContent === 'انتهت') li.classList.add('done');
    if(!w) return;
    const [day, time] = w.textContent.split(' · '), name = day.split(' ')[0];
    const word = name === wd(Date.now()) ? 'اليوم' : name === wd(Date.now() + 864e5) ? 'غدًا' : name;
    w.textContent = time ? word + ' ' + time : word;
  });
  const old = renderCard;
  renderCard = function(c, a, k){ old(c, a, k); if(c.eff === 'matches') tidy(c.ul); };
  renderPanel(false); }
