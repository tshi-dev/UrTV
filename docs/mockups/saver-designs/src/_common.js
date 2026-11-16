// shared by the designs: the class on <html>, a node in <body>, the day's words from the page itself
const H = document.documentElement, low = SD.opt === 'low' || CONFIG.lowram;
const put = (cls, html, css) => { const e = document.createElement('div'); e.className = cls; e.innerHTML = html; if (css) e.style.cssText = css; document.body.appendChild(e); return e; };
const txt = id => (document.getElementById(id) || {}).textContent || '';
const dayLine = () => [txt('hijri'), txt('greg').replace(/^[^،]+،\s*/, '')].filter(Boolean).join(SEP);   // «الخميس، ١١ جمادى الأولى ١٤٤٨ هـ، ٢٢ أكتوبر ٢٠٢٦»
const nextLine = () => txt('prayer-name') && [txt('prayer-name') + ' ' + txt('prayer-time'), txt('prayer-count')].filter(Boolean).join(SEP);
// the clock out of the stage's grid (a photo's settle movement makes the stage a containing block): placed on the screen itself
const lift = () => document.body.appendChild(document.querySelector('.clockwrap'));
// the clock's colon as two round dots (the app's Thmanyah has round ones; the stand-in font here draws squares)
document.querySelectorAll('.clock .sep').forEach(s => { s.classList.add('sd-dots'); s.textContent = ''; });
