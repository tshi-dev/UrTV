#!/usr/bin/env python3
"""Make the PIN recovery record once, on your own computer (maintainer only; AGENTS.md, «PIN recovery»).

It asks for your recovery code (5 to 8 digits, typed twice, never shown) and makes its salted PBKDF2 record
«1:rounds:salt:hash», the only form the build ever sees. The code itself is never written anywhere.

  python3 .github/tools/make_pin_recovery.py            prints the record, for the repository secret PIN_RECOVERY_RECORD
  python3 .github/tools/make_pin_recovery.py --local    also writes it to android/pin-recovery.local (untracked) for a local build

With GitHub CLI signed in (`gh auth login`), --gh stores it as the secret itself. Every record of the same code differs
(a new salt each time); any of them works.
"""
import getpass
import hashlib
import hmac
import re
import secrets
import shutil
import subprocess
import sys
from pathlib import Path

ROUNDS = 400000   # within PinCrypto's 10000..1000000; checked rarely, so slower than the PIN's own
SHORTEST, LONGEST = 5, 8   # PinRecovery.SHORTEST / LONGEST


def make(code, salt=None, rounds=ROUNDS):
    if not re.fullmatch(r"[0-9]{%d,%d}" % (SHORTEST, LONGEST), code or ""):
        raise ValueError(f"a recovery code is {SHORTEST} to {LONGEST} digits")
    salt = salt if salt is not None else secrets.token_bytes(16)
    dk = hashlib.pbkdf2_hmac("sha256", code.encode("ascii"), salt, rounds, 32)
    return f"1:{rounds}:{salt.hex()}:{dk.hex()}"


def matches(code, record):
    p = record.split(":")
    dk = hashlib.pbkdf2_hmac("sha256", code.encode("ascii"), bytes.fromhex(p[2]), int(p[1]), 32)
    return hmac.compare_digest(dk.hex(), p[3])


def main(argv):
    code = getpass.getpass(f"recovery code ({SHORTEST}-{LONGEST} digits, not shown): ")
    if getpass.getpass("again: ") != code:
        sys.exit("the two differ; nothing made")
    try:
        record = make(code)
    except ValueError as e:
        sys.exit(str(e))
    del code
    if "--local" in argv:
        out = Path(__file__).resolve().parents[2] / "android/pin-recovery.local"
        out.write_text(record + "\n", encoding="ascii")
        print(f"written to {out.relative_to(Path.cwd()) if out.is_relative_to(Path.cwd()) else out.name} (untracked)")
    if "--gh" in argv and shutil.which("gh"):
        subprocess.run(["gh", "secret", "set", "PIN_RECOVERY_RECORD", "--body", record], check=True)
        print("stored as the repository secret PIN_RECOVERY_RECORD")
    else:
        print(record)


if __name__ == "__main__":
    main(sys.argv[1:])
