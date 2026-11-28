"""The small copy of a screenshot (960 px wide) without Pillow: a PNG read and written with the standard library only.

    python3 -I optimize/png_small.py big.png small.png [width]      exit 0: written; 1: not a PNG this reads (it says why)

Reads 8-bit RGB or RGBA PNGs without interlace (what Android's `screencap -p` writes), every row filter. Each pixel of the
copy is the mean of the 2 x 2 source pixels it starts at, so text stays readable. Slower than Pillow (a few seconds for a
1080p screenshot); optimize/shot.sh uses Pillow when it is there.
"""
import struct, sys, zlib


def read_png(path):
    data = open(path, "rb").read()
    if len(data) < 8 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG" + (" (the file is empty)" if not data else ""))
    pos, idat, ihdr = 8, [], None
    while pos + 8 <= len(data):
        n, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat.append(body)
        elif kind == b"IEND":
            break
        pos += 12 + n
    if ihdr is None:
        raise ValueError("no PNG header")
    w, h, depth, ctype, _, _, interlace = ihdr
    if depth != 8 or ctype not in (2, 6) or interlace:
        raise ValueError(f"a PNG kind this does not read (depth {depth}, colour type {ctype}, interlace {interlace})")
    bpp = 3 if ctype == 2 else 4
    raw = zlib.decompress(b"".join(idat))
    stride = w * bpp
    rows, prior = [], bytearray(stride)
    for y in range(h):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        if f == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif f == 2:
            line = bytearray((a + b) & 255 for a, b in zip(line, prior))
        elif f == 3:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + prior[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                b = prior[i]
                c = prior[i - bpp] if i >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        elif f != 0:
            raise ValueError(f"unknown row filter {f}")
        rows.append(line)
        prior = line
    return w, h, bpp, rows


def write_png(path, w, h, rows):   # RGB, filter 0 on every row
    raw = b"".join(b"\x00" + bytes(r) for r in rows)
    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xffffffff)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b""))


def small(src, dst, width=960):
    w, h, bpp, rows = read_png(src)
    nw = min(width, w)
    nh = max(1, round(h * nw / w))
    out = []
    for y in range(nh):
        sy = y * h // nh
        r0, r1 = rows[sy], rows[min(sy + 1, h - 1)] if h > nh else rows[sy]
        line = bytearray(nw * 3)
        for x in range(nw):
            sx = x * w // nw
            sx1 = min(sx + 1, w - 1) if w > nw else sx
            for c in range(3):
                i, j = sx * bpp + c, sx1 * bpp + c
                line[x * 3 + c] = (r0[i] + r0[j] + r1[i] + r1[j] + 2) >> 2
        out.append(line)
    write_png(dst, nw, nh, out)
    return nw, nh


if __name__ == "__main__":
    try:
        small(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 960)
    except (OSError, ValueError, zlib.error) as e:
        sys.exit(f"(no small copy: {e})")
