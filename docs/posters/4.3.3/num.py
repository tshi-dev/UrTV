"""4.3.3 hero options: the number built as SVG, laid out from measured digit widths (tight and even), dots as small diamonds."""
import re, sys
base = open('poster-433-a.html', encoding='utf-8').read()
FONTS = """
@font-face{font-family:'BSD';font-weight:900;src:url(a/big-shoulders-display-latin-900-normal.woff2)}
@font-face{font-family:'BSD';font-weight:800;src:url(a/big-shoulders-display-latin-800-normal.woff2)}
@font-face{font-family:'Bebas';font-weight:400;src:url(a/bebas-neue-latin-400-normal.woff2)}
#num{display:block;margin:0 auto;direction:ltr;unicode-bidi:isolate}
"""
# common layout script: digits as <text>, measured, then placed with gap G around each diamond (size D)
JS = r"""
<script>
document.fonts.ready.then(() => {
  const svg = document.getElementById('num'), NS = 'http://www.w3.org/2000/svg';
  const cfg = window.NUM;
  const mk = (t, a) => { const e = document.createElementNS(NS, t); for (const k in a) e.setAttribute(k, a[k]); return e; };
  // measure one digit in the face
  const probe = mk('text', {x: 0, y: 0, 'text-anchor': 'start', direction: 'ltr', 'font-family': cfg.family, 'font-weight': cfg.weight, 'font-size': cfg.size}); probe.textContent = '4';
  svg.appendChild(probe);
  const w4 = probe.getBBox().width; probe.textContent = '3'; const w3 = probe.getBBox().width; probe.remove();
  const widths = [w4, w3, w3], D = cfg.diamond, G = cfg.gap;
  const total = widths.reduce((a, b) => a + b, 0) + 2 * (D + 2 * G);
  let x = (cfg.W - total) / 2; const xs = [], ds = [];
  widths.forEach((w, i) => { xs.push(x); x += w; if (i < 2) { ds.push(x + G + D / 2); x += D + 2 * G; } });
  cfg.draw(svg, mk, xs, ds, widths);
});
</script>"""

def page(opt_css, svg_defs, draw_js, family, weight, size, diamond, gap, W=1700, H=490, wrapper_style=""):
    s = base.replace("</style>", FONTS + opt_css + "</style>", 1)
    num = f'<div style="{wrapper_style}"><svg id="num" direction="ltr" width="{W}" height="{H}" viewBox="0 0 {W} {H}" overflow="visible"><defs>{svg_defs}</defs></svg></div>'
    s = re.sub(r'<div class="ver disp">4\.3\.3</div>', num, s)
    cfg = f"<script>window.NUM={{family:'{family}',weight:{weight},size:{size},diamond:{diamond},gap:{gap},W:{W},draw:{draw_js}}};</script>"
    return s.replace("</body>", cfg + JS + "</body>")

# (1) engraved gold plaque
p1 = page(".hero h1{margin-top:20px}", """
<linearGradient id="gold" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#FBE9B4"/><stop offset=".10" stop-color="#EDCB7E"/><stop offset=".48" stop-color="#D3A452"/>
  <stop offset=".80" stop-color="#A9772E"/><stop offset="1" stop-color="#7E551C"/></linearGradient>
<linearGradient id="edge" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF4D2" stop-opacity=".9"/><stop offset=".5" stop-color="#FFF4D2" stop-opacity=".15"/><stop offset="1" stop-color="#FFF4D2" stop-opacity="0"/></linearGradient>
<filter id="plaque" x="-10%" y="-10%" width="120%" height="140%"><feDropShadow dx="0" dy="16" stdDeviation="16" flood-color="#000" flood-opacity=".55"/></filter>
""", """(svg, mk, xs, ds, w) => {
  const g = mk('g', {filter: 'url(#plaque)'}); svg.appendChild(g); const y = 440;
  ['4','3','3'].forEach((d, i) => {
    const t = mk('text', {x: xs[i], y, 'text-anchor': 'start', direction: 'ltr', 'font-family': 'BSD', 'font-weight': 900, 'font-size': 520, fill: 'url(#gold)', stroke: '#6E4A16', 'stroke-width': 3, 'paint-order': 'stroke'}); t.textContent = d; g.appendChild(t);
    const h = mk('text', {x: xs[i], y, 'text-anchor': 'start', direction: 'ltr', 'font-family': 'BSD', 'font-weight': 900, 'font-size': 520, fill: 'none', stroke: 'url(#edge)', 'stroke-width': 3}); h.textContent = d; g.appendChild(h);
  });
  ds.forEach(cx => { const s = 30, cy = y - s; g.appendChild(mk('path', {d: `M${cx} ${cy-s} L${cx+s} ${cy} L${cx} ${cy+s} L${cx-s} ${cy} Z`, fill: 'url(#gold)', stroke: '#FFF0C8', 'stroke-opacity': .6, 'stroke-width': 2})); });
}""", 'BSD', 900, 520, 60, 26)

# (2) light-built: gold light lines
p2 = page(".hero h1{margin-top:20px}", """
<filter id="neon" x="-20%" y="-30%" width="140%" height="160%">
  <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="b1"/><feGaussianBlur in="SourceGraphic" stdDeviation="14" result="b2"/><feGaussianBlur in="SourceGraphic" stdDeviation="34" result="b3"/>
  <feMerge><feMergeNode in="b3"/><feMergeNode in="b2"/><feMergeNode in="b1"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
""", """(svg, mk, xs, ds, w) => {
  const g = mk('g', {filter: 'url(#neon)'}); svg.appendChild(g); const y = 450;
  ['4','3','3'].forEach((d, i) => {
    const t = mk('text', {x: xs[i], y, 'text-anchor': 'start', direction: 'ltr', 'font-family': 'Bebas', 'font-size': 560, fill: 'none', stroke: '#F2CF84', 'stroke-width': 7, 'stroke-linejoin': 'round'}); t.textContent = d; g.appendChild(t);
    const c = mk('text', {x: xs[i], y, 'text-anchor': 'start', direction: 'ltr', 'font-family': 'Bebas', 'font-size': 560, fill: 'none', stroke: '#FFF3D6', 'stroke-width': 2.2, 'stroke-linejoin': 'round'}); c.textContent = d; g.appendChild(c);
  });
  ds.forEach(cx => { const s = 26, cy = y - s - 4; g.appendChild(mk('path', {d: `M${cx} ${cy-s} L${cx+s} ${cy} L${cx} ${cy+s} L${cx-s} ${cy} Z`, fill: 'none', stroke: '#F2CF84', 'stroke-width': 7, 'stroke-linejoin': 'round'})); });
}""", 'Bebas', 400, 560, 52, 34)

# (3) layered depth: extrusion, slight perspective, the TV glow behind
p3 = page(""".hero h1{margin-top:20px}
.tvglow{position:absolute;left:50%;top:470px;width:1540px;height:640px;transform:translateX(-50%);border-radius:60px;
  background:radial-gradient(closest-side,rgba(233,199,127,.26),rgba(233,199,127,.08) 70%,rgba(233,199,127,0));filter:blur(30px)}
.glow{display:none}""", """
<linearGradient id="face" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F6DC9C"/><stop offset=".55" stop-color="#E2BC6E"/><stop offset="1" stop-color="#C9994A"/></linearGradient>
<linearGradient id="side" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7A5A22"/><stop offset="1" stop-color="#1F3A28"/></linearGradient>
<filter id="soft" x="-10%" y="-10%" width="120%" height="150%"><feDropShadow dx="0" dy="22" stdDeviation="22" flood-color="#000" flood-opacity=".5"/></filter>
""", """(svg, mk, xs, ds, w) => {
  const g = mk('g', {filter: 'url(#soft)'}); svg.appendChild(g); const y = 420, N = 30;
  const glyph = (d, x, dy, fill, extra) => { const t = mk('text', Object.assign({x, y: y + dy, 'text-anchor': 'start', direction: 'ltr', 'font-family': 'BSD', 'font-weight': 900, 'font-size': 500, fill}, extra || {})); t.textContent = d; g.appendChild(t); };
  const dia = (cx, dy, fill, extra) => { const s = 30, cy = y - s + dy; g.appendChild(mk('path', Object.assign({d: `M${cx} ${cy-s} L${cx+s} ${cy} L${cx} ${cy+s} L${cx-s} ${cy} Z`, fill}, extra || {}))); };
  for (let k = N; k >= 1; k--) { const f = k === N ? '#16301F' : 'url(#side)'; ['4','3','3'].forEach((d, i) => glyph(d, xs[i], k, f)); ds.forEach(cx => dia(cx, k, f)); }
  ['4','3','3'].forEach((d, i) => glyph(d, xs[i], 0, 'url(#face)', {stroke: '#FFF1C9', 'stroke-opacity': .5, 'stroke-width': 2}));
  ds.forEach(cx => dia(cx, 0, 'url(#face)'));
}""", 'BSD', 900, 500, 60, 24, wrapper_style="transform:perspective(1600px) rotateX(16deg);transform-origin:50% 70%")
p3 = p3.replace('<div class="glow"></div>', '<div class="glow"></div><div class="tvglow"></div>')
for i, p in enumerate((p1, p2, p3), 1):
    open(f'poster-433-{i}.html', 'w', encoding='utf-8').write(p)
