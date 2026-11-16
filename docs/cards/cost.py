"""Bytes an «after» would add to dist/tv.html: its CSS and JS with comments and indentation gone (as the page is written: one rule a line).
   python3 docs/cards/cost.py [card ...]"""
import re, sys, pathlib
A = pathlib.Path(__file__).parent / 'after'
def mini(t, js):
    t = re.sub(r'/\*.*?\*/', '', t, flags=re.S)
    lines = [l.strip() for l in t.splitlines()]
    return '\n'.join(l for l in lines if l and not (js and l.startswith('//')))
for c in sys.argv[1:] or sorted({p.stem for p in A.iterdir()}):
    n = sum(len(mini(p.read_text(), p.suffix == '.js').encode()) for p in A.glob(c + '.*'))
    print(f'{c:12} {n:6} bytes')
