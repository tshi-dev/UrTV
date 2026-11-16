{ // the counted noun as Arabic says it: ٥٧ يومًا، ٥ أيام، يومان، ١٠٠ يوم (qty() gave «٥٧ يوم» and «١ يوم» for every count)
  qty = function(n, u){
    if(n === 1) return u.one; if(n === 2) return u.two;
    const r = n % 100;
    if(r >= 3 && r <= 10) return `${ar(n)} ${u.few}`;
    return `${ar(n)} ${r >= 11 && u === UNIT.day ? 'يومًا' : u.many}`;
  };
  const n = countdownDays();
  if(n !== null && CONFIG.slot === 'countdown'){
    const days = n === 0 ? 'اليوم' : n === 1 ? 'غدًا' : qty(n, UNIT.day).replace(/^([٠-٩0-9]+) (.+)$/, '<span class="num">$1</span> <span class="unit">$2</span>');
    const when = gregFmt({timeZone:'UTC', weekday:'long', day:'numeric', month:'long'}).format(new Date(CONFIG.cd + 'T12:00:00Z')).replace('،', '');
    setText(document.getElementById('slot-label'), CONFIG.cdName || 'عدّ تنازلي');
    const big = document.querySelector('#match-row .big > span');
    if(big) big.innerHTML = days;
    const sub = document.querySelector('#match-row .sub');
    if(sub) sub.textContent = when;
  }
}
