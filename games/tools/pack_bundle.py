#!/usr/bin/env python3
"""Makes a games bundle (android/.../GameBundle.java reads it): deflate, AES-256-CBC, HMAC-SHA256, keys from the bundle's name.
   python3 games/tools/pack_bundle.py NAME SOURCE.json OUT.gb      pack          python3 games/tools/pack_bundle.py --open NAME FILE.gb     print the JSON
The SOURCE is the editable content: it is kept outside the public repositories (docs/LICENSE-CONTENT.md); only OUT is committed or published."""
import hashlib, hmac, os, sys, zlib
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
PIECES = ["urtv", "games", "ٱلْمُحْتَوَى", "2026"]
def keys(name):
    pw = "".join(p + "|" for p in PIECES) + name
    salt = hashlib.sha256(("urtv-salt|" + name).encode()).digest()
    k = hashlib.pbkdf2_hmac("sha256", pw.encode("utf-16-be").replace(b"\0", b"") if False else _java_chars(pw), salt, 20000, 64)
    return k[:32], k[32:]
def _java_chars(s):
    # PBEKeySpec(char[]) with PBKDF2WithHmacSHA256 encodes the characters as UTF-8
    return s.encode("utf-8")
def pack(name, plain, iv=None):
    ek, mk = keys(name); iv = iv or os.urandom(16)
    z = zlib.compress(plain, 9)
    p = padding.PKCS7(128).padder(); padded = p.update(z) + p.finalize()
    enc = Cipher(algorithms.AES(ek), modes.CBC(iv)).encryptor(); ct = enc.update(padded) + enc.finalize()
    body = b"GB1" + b"\x01" + iv + ct
    return body + hmac.new(mk, body, hashlib.sha256).digest()
def unpack(name, blob):
    ek, mk = keys(name)
    assert blob[:3] == b"GB1" and hmac.compare_digest(hmac.new(mk, blob[:-32], hashlib.sha256).digest(), blob[-32:]), "not a good bundle"
    dec = Cipher(algorithms.AES(ek), modes.CBC(blob[4:20])).decryptor(); z = dec.update(blob[20:-32]) + dec.finalize()
    u = padding.PKCS7(128).unpadder()
    return zlib.decompress(u.update(z) + u.finalize())
if __name__ == "__main__":
    a = sys.argv[1:]
    if a and a[0] == "--open": sys.stdout.buffer.write(unpack(a[1], open(a[2], "rb").read()))
    else: open(a[2], "wb").write(pack(a[0], open(a[1], "rb").read())); print(a[2], os.path.getsize(a[2]), "bytes")
