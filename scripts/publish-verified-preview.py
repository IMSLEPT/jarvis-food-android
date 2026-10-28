"""Publish only the signed output of this successful product build, after all QA gates."""
from pathlib import Path
import hashlib, json, os, re, subprocess, xml.etree.ElementTree as ET, zipfile

def run(*args):
    return subprocess.check_output(args, text=True)

number = os.environ['GITHUB_RUN_NUMBER']
sha = os.environ['GITHUB_SHA']
apk = Path(f'dist/JarvisFood-v1.0.{number}.apk')
assert apk.exists()
counts = dict(tests=0, failures=0, errors=0, skipped=0)
for path in Path('app/build/test-results/testReleaseUnitTest').glob('TEST-*.xml'):
    root = ET.parse(path).getroot()
    for key in counts:
        counts[key] += int(root.get(key, 0))
assert counts['tests'] >= 99 and not any(counts[k] for k in ('failures', 'errors', 'skipped')), counts
logs = {'layout-profile-standard.log':2, 'layout-profile-large-text.log':2,
        'layout-profile-android10.log':1, 'layout-consistency-standard.log':3,
        'layout-consistency-large-text.log':3, 'layout-consistency-small-200.log':3, 'layout-standard.log':2, 'layout-large-text.log':2,
        'layout-buttons.log':1, 'layout-buttons-large-text.log':1, 'layout-gestures.log':1,
        'layout-android10-buttons.log':1, 'photo-ai.log':3, 'layout-features-standard.log':2,
        'layout-features-large-text.log':2, 'layout-options-standard.log':3,
        'layout-options-large-text.log':3, 'regression.log':3, 'emu.log':1}
for name, count in logs.items():
    content = Path(name).read_text()
    assert f'OK ({count} test' in content and 'FAILURES!!!' not in content, name
for name in ('crash.txt', 'layout-crash.txt'):
    assert 'FATAL EXCEPTION' not in Path(name).read_text(), name
sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
signer = sorted(sdk.glob('build-tools/*/apksigner'))[-1]
certificate = run(str(signer), 'verify', '--min-sdk-version', '26', '--print-certs', str(apk))
assert 'a039be1f4e694272f35ac964d46d28185535ce28b2dcfb5bbbedda264b4d80ca' in certificate.lower()
with zipfile.ZipFile(apk) as bundle:
    assert len(bundle.read('assets/food.tflite')) > 1_000_000
    assert 'assets/body-guide/index.html' not in bundle.namelist(), 'Obsolete 3D page must not be packaged'
record = dict(version=f'1.0.{number}', source_sha=sha, sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
              bytes=apk.stat().st_size, unit_tests=counts, android_executions=sum(logs.values()),
              original_signature_verified=True, visual_revision='Compact blue design, unified outline icons, preserved historical calorie rings, approved meal journey, water decrement and smartwatch insets; 100/130/200 percent text',
              checks=list(logs), offline_food_model_included=True)
verification = Path('dist/verification.json'); verification.write_text(json.dumps(record, indent=2)+'\n')
notes = Path('dist/release-notes.md')
notes.write_text(f'''Stile blu compatto come nel riferimento scelto: sfondo azzurro chiaro, schede bianche, icone a tratto uniformate anche nelle categorie, barra inferiore con selezione azzurra. Conservati gli anelli arancio delle calorie registrate nei giorni precedenti. Piano a percorso con foto a destra, tasto meno dell'acqua grigio e discreto, margine inferiore Smartwatch corretto. Conservate le funzionalità precedenti, le fotografie e i dati.

APK firmato con la chiave originale e installabile come aggiornamento. Verificati {counts['tests']} test unitari e {sum(logs.values())} esecuzioni Android, inclusi Android 10/15, schermi piccoli e testo al 100%, 130% e 200%. Modello AI offline e fotografie delle ricette inclusi.
''')
subprocess.run(['gh','release','create',f'preview-v1.0.{number}',str(apk),str(verification),
                '--target',sha,'--title',f'Jarvis Food 1.0.{number} · Revisione grafica','--prerelease','--notes-file',str(notes)], check=True)
print('VERIFICATION_RESULT='+json.dumps(record))
print(f'DOWNLOAD_URL=https://github.com/IMSLEPT/jarvis-food-android/releases/download/preview-v1.0.{number}/{apk.name}')
