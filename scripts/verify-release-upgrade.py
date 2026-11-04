#!/usr/bin/env python3
"""Exercise signed APK updates and retain real user-state data on an emulator."""
from datetime import date
import glob
import json
from pathlib import Path
import subprocess
import time

ADB = './.ci-bin/adb'
PACKAGE = 'com.jarvis.food'
STATE = f'/data/user/0/{PACKAGE}/files/jarvis_food.json'
log = Path('redesign-upgrade.log').open('w')

def adb(*args):
    result = subprocess.run([ADB, *args], capture_output=True, text=True, check=True)
    log.write(result.stdout + result.stderr)
    log.flush()
    return result.stdout

try:
    adb('uninstall', PACKAGE)
    original, previous, new = (glob.glob(pattern)[0] for pattern in ('original/*.apk', 'previous/*.apk', 'app/build/outputs/apk/release/*.apk'))
    adb('install', original)
    adb('shell', 'am', 'start', '-W', '-n', f'{PACKAGE}/.MainActivity')
    time.sleep(2)
    adb('shell', 'am', 'force-stop', PACKAGE)
    owner = adb('shell', 'stat', '-c', '%u:%g', f'/data/user/0/{PACKAGE}/files').strip()
    fixture = {'onboarded': True, 'profile': {'name': 'Upgrade fixture'},
               'water': {date.today().isoformat(): 750}, 'steps': {date.today().isoformat(): 3285},
               'chat': [{'id': 'upgrade-chat', 'role': 'user', 'text': 'Cronologia da conservare', 'time': 1000}]}
    local = Path('redesign-upgrade-fixture.json')
    local.write_text(json.dumps(fixture))
    adb('push', str(local), '/data/local/tmp/jarvis-upgrade.json')
    adb('shell', 'cp', '/data/local/tmp/jarvis-upgrade.json', STATE)
    adb('shell', 'chown', owner, STATE)
    adb('shell', 'chmod', '600', STATE)
    for label, apk in [('original', None), ('latest-preview', previous), ('new-release', new)]:
        if apk:
            assert 'Success' in adb('install', '-r', apk), f'Upgrade rejected: {label}'
        adb('logcat', '-c')
        launch = adb('shell', 'am', 'start', '-W', '-n', f'{PACKAGE}/.MainActivity')
        assert 'Status: ok' in launch, f'Launch failed: {label}'
        time.sleep(3)
        assert adb('shell', 'pidof', PACKAGE).strip(), f'Process stopped: {label}'
        adb('shell', 'am', 'force-stop', PACKAGE)
        state = json.loads(adb('shell', 'cat', STATE))
        assert state['profile']['name'] == fixture['profile']['name'], f'Profile lost: {label}'
        for key in ('water', 'steps', 'chat'):
            assert state[key] == fixture[key], f'{key} lost: {label}'
        assert 'FATAL EXCEPTION' not in adb('logcat', '-d', '-s', 'AndroidRuntime:E'), f'Crash: {label}'
        log.write(f'PASS {label}: signed install, launch, profile/water/steps/chat retained\n')
        log.flush()
finally:
    log.close()
