H.classList.add('sd-clean');
lift();
if (!low && SD.glass) { H.classList.add('sd-glass'); H.style.setProperty('--sd-glass', `url('${SD.glass}')`); }
H.style.setProperty('--sd-top', SD.top + 'px'); H.style.setProperty('--sd-size', SD.size + 'px');
if (!low && SD.fg) put('sd-fg', '', `top:${SD.fgTop}px;height:${SD.fgH}px;background-image:url('${SD.fg}')`);
const nx = SD.opt === 'nonext' ? '' : nextLine();
put('sd-line', `<div class="d">${dayLine()}</div>` + (nx ? `<div class="n"><b>${nx.split(SEP)[0]}</b>${SEP}${nx.split(SEP).slice(1).join(SEP)}</div>` : ''), `top:${Math.round(SD.top + SD.size * .98)}px`);
