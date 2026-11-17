#!/usr/bin/env python3
"""Installs UrTV on a Samsung TV in Developer Mode, over your Wi-Fi, without Samsung's sdb. BETA: not tested on a real Samsung TV
(docs/LG-SAMSUNG.md).

    python3 ports/tizen/install.py <TV-IP>              # install or update, then open it
    python3 ports/tizen/install.py <TV-IP> --uninstall  # remove it

First build and sign it: python3 ports/tizen/build.py --sign <profile> (docs/LG-SAMSUNG.md). Before the first install, on the
TV: open Apps, type 1 2 3 4 5 on the remote, turn Developer mode on with this computer's IP as Host PC IP, and
restart the TV. (Or install the .wgt with TizenBrew Installer, which signs it for you on 2023 and newer TVs.)

It speaks SDB (Samsung's ADB, port 26101) itself, so it needs only Python 3: no Tizen Studio, and no Rosetta on Apple
Silicon, where Samsung's sdb (an Intel binary) doesn't run. Like Samsung's tools it pushes the package to the folder
the TV names in its capabilities and installs it with `0 vd_appinstall`, the command TizenBrew Installer uses on
every Tizen; `0 was_execute` opens it.

Adapted from Free My TV's tizen/install.py (https://github.com/simbasjungles/free-my-tv, MIT; see THIRD_PARTY_NOTICES.md),
which reports it working on a 2017, a 2019 and a 2024 Samsung. We have not tried it on any.
"""
import os
import re
import socket
import struct
import sys
import time
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WGT = os.path.join(ROOT, 'UrTV-tizen.wgt')
PORT = 26101
MAX_DATA = 4096   # what this side announces; sdbd keeps each packet within both sides' maximum
ON_TV = 'Apps, Settings, UrTV, Delete'   # removing it on the TV itself


class Sdb:
    """The pieces of the SDB wire protocol this needs: ADB's framing (a 24-byte header whose checksum is a byte sum),
    streams opened with OPEN, and every WRTE answered with OKAY before the next one."""

    def __init__(self, ip):
        self.sock = socket.create_connection((ip, PORT), timeout=15)
        self.buf = b''
        self.next_id = 1
        self.okay, self.inbox, self.closed = {}, {}, set()
        self.send(b'CNXN', 0x01000000, MAX_DATA, b'host::\0')
        cmd, _, max_data, data = self.read()
        if cmd != b'CNXN':
            raise ConnectionError('the TV answered %r instead of connecting' % cmd)
        self.max = min(max_data, MAX_DATA)
        self.banner = data.rstrip(b'\0').decode('utf-8', 'replace')

    def send(self, cmd, arg0, arg1, data=b''):
        c = struct.unpack('<I', cmd)[0]
        self.sock.sendall(struct.pack('<6I', c, arg0, arg1, len(data), sum(data) & 0xFFFFFFFF, c ^ 0xFFFFFFFF) + data)

    def _recv(self, n):
        while len(self.buf) < n:
            chunk = self.sock.recv(65536)
            if not chunk:
                raise ConnectionError('the TV closed the connection')
            self.buf += chunk
        out, self.buf = self.buf[:n], self.buf[n:]
        return out

    def read(self):
        cmd, arg0, arg1, n, _, _ = struct.unpack('<6I', self._recv(24))
        return struct.pack('<I', cmd), arg0, arg1, self._recv(n) if n else b''

    def pump(self):
        """Reads one packet and files it under its stream: OKAY, data (answered with OKAY) or close."""
        cmd, remote, local, data = self.read()
        if cmd == b'OKAY':
            self.okay[local] = remote
        elif cmd == b'WRTE':
            self.inbox[local] = self.inbox.get(local, b'') + data
            self.send(b'OKAY', local, remote)
        elif cmd == b'CLSE':
            self.closed.add(local)

    def open(self, service):
        local, self.next_id = self.next_id, self.next_id + 1
        self.send(b'OPEN', local, 0, service.encode() + b'\0')
        while local not in self.okay:
            if local in self.closed:
                raise ConnectionError('the TV refused ' + service)
            self.pump()
        return local, self.okay.pop(local)

    def write(self, stream, data):
        local, remote = stream
        self.send(b'WRTE', local, remote, data)
        while local not in self.okay:
            if local in self.closed:
                raise ConnectionError('the TV closed the stream')
            self.pump()
        del self.okay[local]

    def read_until(self, stream, done, seconds):
        """The stream's text until done(text) is true, the TV closes it, or time runs out."""
        local = stream[0]
        end = time.time() + seconds
        while time.time() < end and local not in self.closed and not done(self.inbox.get(local, b'')):
            self.sock.settimeout(max(0.1, end - time.time()))
            try:
                self.pump()
            except socket.timeout:
                break
        self.sock.settimeout(15)
        return self.inbox.pop(local, b'')

    def shell(self, command, until=None, seconds=20):
        stream = self.open('shell:' + command)
        out = self.read_until(stream, lambda t: bool(until and re.search(until, t.decode('utf-8', 'replace'))), seconds)
        return out.decode('utf-8', 'replace')

    def capabilities(self):
        text = self.read_until(self.open('capability:'), lambda t: False, 5).decode('utf-8', 'replace')
        return dict(line.split(':', 1) for line in text.splitlines() if ':' in line)

    def push(self, path, data):
        """Copies data to path on the TV (sync SEND), then checks the TV's answer."""
        stream = self.open('sync:')
        spec = ('%s,%d' % (path, 0o100644)).encode()
        self.write(stream, b'SEND' + struct.pack('<I', len(spec)) + spec)
        step = self.max - 8
        for i in range(0, len(data), step):
            part = data[i:i + step]
            self.write(stream, b'DATA' + struct.pack('<I', len(part)) + part)
        self.write(stream, b'DONE' + struct.pack('<I', int(time.time())))
        answer = self.read_until(stream, lambda t: len(t) >= 8, 60)
        if answer[:4] != b'OKAY':
            raise ConnectionError('the TV didn\'t take the file: ' + answer[8:].decode('utf-8', 'replace'))
        self.send(b'WRTE', stream[0], stream[1], b'QUIT' + struct.pack('<I', 0))


def this_computer(ip):
    """This computer's address on the TV's network (what Host PC IP must be)."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((ip, PORT))
        return s.getsockname()[0]
    except OSError:
        return 'this computer\'s IP'


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if len(args) != 1:
        sys.exit(__doc__)
    ip = args[0]
    uninstall = '--uninstall' in sys.argv
    if uninstall:
        # Removing needs only the app's ids: the manifest the package is built from has them, so no build is needed.
        with open(os.path.join(ROOT, 'ports', 'tizen', 'config.xml'), encoding='utf-8') as f:
            manifest = f.read()
    else:
        if not os.path.exists(WGT):
            sys.exit('Build and sign it first: python3 ports/tizen/build.py --sign <profile>')
        with zipfile.ZipFile(WGT) as z:
            if 'author-signature.xml' not in z.namelist():
                sys.exit('UrTV-tizen.wgt isn\'t signed, and the TV only installs signed apps: '
                         'python3 ports/tizen/build.py --sign <profile>')
            manifest = z.read('config.xml').decode('utf-8')
    tag = re.search(r'<tizen:application[^>]*>', manifest).group(0)
    app_id = re.search(r'\bid="([^"]+)"', tag).group(1)
    package = re.search(r'\bpackage="([^"]+)"', tag).group(1)

    try:
        sdb = Sdb(ip)
    except (OSError, ConnectionError) as e:
        # A 2024 QA75Q60D (Tizen 9) closed the connection like this for a while, across restarts, with the right Host
        # PC IP, then took the same install without a change: what it waited for is unknown.
        dropped = isinstance(e, ConnectionResetError) or 'closed the connection' in str(e)
        sys.exit('Couldn\'t reach the TV on port %d (%s). On the TV: Apps, 1 2 3 4 5, Developer mode on with Host PC '
                 'IP %s, then restart the TV.%s' % (PORT, e, this_computer(ip),
                 ' If it already is, wait a minute after the TV restarts and try again.' if dropped else ''))
    caps = sdb.capabilities()
    print('Connected: %s, Tizen %s.' % (sdb.banner.split(':')[2] if sdb.banner.count(':') >= 2 else sdb.banner,
                                         caps.get('platform_version', '?')))

    if uninstall:
        out = sdb.shell('0 vd_appuninstall ' + package, r'spend time', 120).strip()
        if not re.search(r'uninstall completed', out):
            # Some TVs' sdbd refuses this command and answers nothing.
            sys.exit(('The TV said:\n' + out if out else 'The TV didn\'t answer.') + '\nRemove it on the TV instead: ' + ON_TV + '.')
        print('Removed.')
        return

    # Tizen 2.4's sdbd names no sdk_toolpath and has no /home/owner: its tools' folder is /opt/usr/apps/tmp.
    default_path = '/opt/usr/apps/tmp' if caps.get('platform_version', '').startswith('2.') else '/home/owner/share/tmp/sdk_tools'
    remote = caps.get('sdk_toolpath', default_path).rstrip('/') + '/UrTV-tizen.wgt'
    with open(WGT, 'rb') as f:
        data = f.read()
    print('Copying the app to the TV ...')
    sdb.push(remote, data)
    print('Installing ...')
    out = sdb.shell('0 vd_appinstall %s %s' % (package, remote), r'spend time', 120)
    if not re.search(r'install completed', out):
        hint = ''
        if 'different certificate' in out:
            hint = ('\nThe UrTV on the TV was signed with another certificate, and the TV only updates it with the '
                    'same one. Remove it on the TV (' + ON_TV + '), then run this again.')
        sys.exit('The TV didn\'t finish installing. It said:\n' + out.strip() + hint)
    sdb.shell('0 was_execute ' + app_id, r'launched|spend time', 20)
    print('Installed and open on the TV.')
    print('Tip: turn on Autorun Last App in the TV\'s settings (General, Smart Features; where it is changes by year) '
          'and the TV turns on into UrTV. It plays the adhan only while it is in front of the TV.')


if __name__ == '__main__':
    main()
