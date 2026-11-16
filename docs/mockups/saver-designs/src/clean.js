H.classList.add('sd-clean');
if (SD.opt === 'low') H.classList.add('sd-low');
lift();
if (!low && SD.glass) { H.classList.add('sd-glass'); if (SD.dark) H.classList.add('sd-dark'); H.style.setProperty('--sd-glass', `url('${SD.glass}')`); }
H.style.setProperty('--sd-top', SD.top + 'px'); H.style.setProperty('--sd-size', SD.size + 'px'); H.style.setProperty('--sd-x', ((SD.x || 960) - 960) / 2 + 'px');
if (!low && SD.fg) put('sd-fg', '', `top:${SD.fgTop}px;height:${SD.fgH}px;background-image:url('${SD.fg}')`);
const nx = SD.opt === 'nonext' || SD.nonext ? '' : nextLine();
put('sd-line', `<div class="d">${dayLine()}</div>` + (nx ? `<div class="n">${nx}</div>` : ''), `top:${Math.round(SD.top + SD.size * .98)}px`);
