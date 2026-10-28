#!/usr/bin/env python3
"""Retry emulator transport failures only; preserve app/test failures and binary evidence."""
import io
import os
from pathlib import Path
import subprocess
import sys
import tarfile


def transport_failed(code, stdout, stderr):
    text = (stderr + (stdout if code else b'')).lower()
    return any(x in text for x in (b'device offline', b'device not found', b'no devices/emulators found',
                                   b'error: closed', b'error: protocol fault'))


def valid_capture(args, stdout):
    if not ('exec-out' in args and 'tar' in args):
        return True
    if len(stdout) < 1024 or stdout[-1024:] != bytes(1024):
        return False
    try:
        with tarfile.open(fileobj=io.BytesIO(stdout), mode='r:') as archive:
            members = archive.getmembers()
            return bool(members) and all(not m.isfile() or len(archive.extractfile(m).read()) == m.size for m in members)
    except (tarfile.TarError, EOFError, OSError):
        return False


def main(args):
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise SystemExit('Android SDK not configured')
    adb = str(Path(sdk) / 'platform-tools/adb')
    for attempt in range(3):
        result = subprocess.run([adb, *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        transport = transport_failed(result.returncode, result.stdout, result.stderr)
        incomplete = result.returncode == 0 and not valid_capture(args, result.stdout)
        if (transport or incomplete) and attempt < 2:
            print('Emulator transport interrupted; reconnecting before retry.', file=sys.stderr)
            subprocess.run([adb, 'reconnect', 'offline'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20)
            try:
                subprocess.run([adb, 'wait-for-device'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=60, check=True)
            except (subprocess.TimeoutExpired, subprocess.CalledProcessError):
                continue
            continue
        if incomplete:
            print('Incomplete screenshot archive; evidence rejected.', file=sys.stderr)
            return 1
        sys.stdout.buffer.write(result.stdout)
        sys.stderr.buffer.write(result.stderr)
        return result.returncode
    return 1


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
