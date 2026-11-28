// one family: the adaptive icon (108 units, the 66 safe circle in the middle) and the 320x180 banner, as SVG strings
const BG = `<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#14402f"/><stop offset="1" stop-color="#061a12"/></linearGradient>
<linearGradient id="au" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F6DC9E"/><stop offset="1" stop-color="#C79A43"/></linearGradient></defs>`;
const W = 5;   // one stroke weight for every glyph (108 units)
const TV = `<rect x="29" y="33" width="50" height="36" rx="6" fill="none" stroke="url(#au)" stroke-width="${W}"/><path d="M47,78h14M54,69v9" stroke="url(#au)" stroke-width="${W}" stroke-linecap="round"/>`;
const INNER = {
  home:   `<circle cx="60" cy="47" r="5" fill="url(#au)"/><path d="M35,63 Q46,52 54,57 T74,60" fill="none" stroke="url(#au)" stroke-width="${W*0.8}" stroke-linecap="round"/>`,
  saver:  `<path d="M57,41 a10,10 0 1,0 9,15 a8,8 0 1,1 -9,-15z" fill="url(#au)"/>`,
  rescue: `<path d="M47,48 a8.5,8.5 0 1,1 1.5,9" fill="none" stroke="url(#au)" stroke-width="${W*0.8}" stroke-linecap="round"/><path d="M42,44 l5,4.5 l-5.5,3.5" fill="none" stroke="url(#au)" stroke-width="${W*0.8}" stroke-linecap="round" stroke-linejoin="round"/>`,
  extras: `<path d="M54,43v16M46,51h16" stroke="url(#au)" stroke-width="${W*0.9}" stroke-linecap="round"/>`,
  doctor: `<path d="M45,51.5l6,6l12,-12" fill="none" stroke="url(#au)" stroke-width="${W}" stroke-linecap="round" stroke-linejoin="round"/>`,
};
const PLAY = `<path d="M43,34 Q38,31 38,37 V71 Q38,77 43,74 L72,57 Q76,54 72,51 Z" fill="url(#au)"/>`;
function glyph(app) { return app === 'player' ? PLAY : TV + INNER[app]; }
function icon(app) { return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">${BG}<rect width="108" height="108" fill="url(#bg)"/>${glyph(app)}</svg>`; }
function fg(app) { return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">${BG}${glyph(app)}</svg>`; }
const NAMES = { home: 'تلفزيونك', saver: 'شاشة التوقف', rescue: 'إنقاذ تلفزيونك', extras: 'إضافات تلفزيونك', player: 'مشغّل تلفزيونك', doctor: 'فحص تلفزيونك' };
function banner(app, font) {
  const name = NAMES[app], size = name.length > 13 ? 26 : name.length > 10 ? 29 : name.length > 9 ? 32 : 42;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 180">${BG}<rect width="320" height="180" fill="url(#bg)"/>
  <g transform="translate(206 36) scale(1)"><rect width="108" height="108" rx="24" fill="#0b2a1f" opacity=".9"/>${glyph(app)}</g>
  <text x="196" y="${app === 'home' ? 100 : 98}" text-anchor="end" font-family="${font}" font-weight="700" font-size="${size}" fill="#F7F3EA">${name}</text>
  <text x="196" y="128" text-anchor="end" font-family="${font}" font-weight="600" font-size="15" letter-spacing="2" fill="#E3C07A">UrTV</text></svg>`;
}
module.exports = { icon, fg, banner, NAMES };
