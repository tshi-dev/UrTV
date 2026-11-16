{ const old = quoteOfDay;
  quoteOfDay = function(card){ const q = old(card); q.rows = q.rows.map(r => r.replace(/^(<li class="src">[^<·]*) · /, '$1، الآية ')); return q; };
  renderPanel(false); }
