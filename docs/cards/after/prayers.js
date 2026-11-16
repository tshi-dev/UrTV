{ const old = prayerRows;
  prayerRows = function(){
    const rows = old(), p = riyadhParts(), mins = p.h * 60 + p.mi + p.s / 60;
    const n = PRAYERS.find(x => x.h * 60 + x.mi > mins) || PRAYERS[0];
    const left = n ? (n.h * 60 + n.mi - mins + 1440) % 1440 : 0;
    return rows.map(r => r.startsWith('<li class="next">') && left > 0 ? r.replace('</li>', `<span class="lf">بعد ${shortHM(Math.round(left * 60))}</span></li>`) : r);
  };
  renderPanel(false); }
