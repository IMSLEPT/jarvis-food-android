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
# 120 previous tests - 14 removed family tests + 7 new calendar/history/upgrade tests.
assert counts['tests'] >= 122 and not any(counts[k] for k in ('failures', 'errors', 'skipped')), counts
logs = {'layout-profile-standard.log':2, 'layout-profile-large-text.log':2,
        'layout-profile-android10.log':1, 'layout-consistency-standard.log':3,
        'layout-consistency-large-text.log':3, 'layout-consistency-small-200.log':3, 'layout-standard.log':2, 'layout-large-text.log':2,
        'layout-buttons.log':1, 'layout-buttons-large-text.log':1, 'layout-gestures.log':1,
        'layout-android10-buttons.log':1, 'photo-ai.log':3, 'layout-features-standard.log':2,
        'layout-features-large-text.log':2, 'layout-options-standard.log':3,
        'layout-options-large-text.log':3, 'layout-nutrition.log':3, 'regression.log':3, 'emu.log':1,
        'layout-video-standard.log':4, 'layout-video-large-text.log':4}
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
    for name in bundle.namelist():
        if re.fullmatch(r'classes\d*\.dex',name):
            dex=bundle.read(name)
            assert b'Lcom/jarvis/food/family/' not in dex, 'Removed family feature must not be packaged'
            assert b'textdb.dev/api/data/' not in dex, 'Removed sync endpoint must not be packaged'
record = dict(version=f'1.0.{number}', source_sha=sha, sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
              bytes=apk.stat().st_size, unit_tests=counts, android_executions=sum(logs.values()),
              original_signature_verified=True, visual_revision='Video-requested fixes: barcode meal shortcuts, visible vitamins tracker with conditional advice, live seconds fasting timer, read-only trend details, corrected solid/liquid units and product-specific piece weights; based on 1.0.171',
              checks=list(logs), offline_food_model_included=True)
verification = Path('dist/verification.json'); verification.write_text(json.dumps(record, indent=2)+'\n')
notes = Path('dist/release-notes.md')
notes.write_text(f'''Aggiornamento richiesto nei due video del 2 novembre e nell’anteprima approvata. Eliminato il pulsante orologio da Oggi/Piano; scorciatoie Codice a barre nei pasti; riquadro vitamine sempre visibile con andamento e copertura dati, consigli condizionati dallo storico; timer del digiuno con secondi, recupero sessione e insets corretti; dettagli interattivi per macro, acqua e attività; correzione solidi/liquidi anche nei dati esistenti, porzioni per pezzo con fonte o peso personalizzato. Per le Fette Biscottate Integrali Mulino Bianco: 8,8 g/fetta dalla scheda ufficiale (02/11/2026), senza trasferire il peso ad altre marche o varianti.

Piano: riquadro a quattro elementi come nel mockup approvato e pulsante Alimentazione. Sedici stili, selezione del periodo 1/7/14/30 giorni, conferma e rigenerazione verificata; vincoli sugli ingredienti ed esclusioni senza fallback incoerenti. Target personali invariati, giornate ON/OFF mantenute. Se lo stile è incompatibile o il calcolo non rientra nelle tolleranze dichiarate (kcal ±10%, macro ±15%), nessuna modifica applicata. Diari, giornate esterne al periodo e pasti già registrati conservati.

APK firmato con la chiave originale. {counts['tests']} test unitari e {sum(logs.values())} esecuzioni Android superati, Android 10/15 e testo 100/130/200%. Le prove non equivalgono a un test manuale sul dispositivo dell’utente.
''')
subprocess.run(['gh','release','create',f'preview-v1.0.{number}',str(apk),str(verification),
                '--target',sha,'--title',f'Jarvis Food 1.0.{number} · Revisione grafica','--prerelease','--notes-file',str(notes)], check=True)
print('VERIFICATION_RESULT='+json.dumps(record))
print(f'DOWNLOAD_URL=https://github.com/IMSLEPT/jarvis-food-android/releases/download/preview-v1.0.{number}/{apk.name}')
