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
assert counts['tests'] >= 124 and not any(counts[k] for k in ('failures', 'errors', 'skipped')), counts
logs = {'layout-diet-followup-standard.log':3, 'layout-diet-followup-large-text.log':3, 'layout-food-review-standard.log':2, 'layout-food-review-large-text.log':2, 'layout-food-followup-standard.log':4, 'layout-food-followup-large-text.log':4, 'layout-november-standard.log':3, 'layout-november-large-text.log':3, 'layout-profile-standard.log':2, 'layout-profile-large-text.log':2,
        'layout-profile-android10.log':1, 'layout-consistency-standard.log':3,
        'layout-consistency-large-text.log':3, 'layout-consistency-small-200.log':3, 'layout-standard.log':2, 'layout-large-text.log':2,
        'layout-buttons.log':1, 'layout-buttons-large-text.log':1, 'layout-gestures.log':1,
        'layout-android10-buttons.log':1, 'photo-ai.log':3, 'layout-features-standard.log':2,
        'layout-features-large-text.log':2, 'layout-options-standard.log':3,
        'layout-options-large-text.log':3, 'layout-nutrition.log':3, 'regression.log':3, 'emu.log':1,
        'layout-video-standard-allThreeProgressCardsOpenReadOnlyDayDetails.log':1,
        'layout-video-standard-brandedSlicesUseGramsAndSaveTheSelectedPieceCount.log':1,
        'layout-video-standard-timerMovesImmediatelyAndResumesWithoutReset.log':1,
        'layout-video-standard-dietSelectionNeedsConfirmationKeepsTargetsAndRejectsConflict.log':1,
        'layout-video-large-text-allThreeProgressCardsOpenReadOnlyDayDetails.log':1,
        'layout-video-large-text-brandedSlicesUseGramsAndSaveTheSelectedPieceCount.log':1,
        'layout-video-large-text-timerMovesImmediatelyAndResumesWithoutReset.log':1,
        'layout-video-large-text-dietSelectionNeedsConfirmationKeepsTargetsAndRejectsConflict.log':1}
for name, count in logs.items():
    content = Path(name).read_text()
    assert f'OK ({count} test' in content and 'FAILURES!!!' not in content, name
for name in ('crash.txt', 'layout-crash.txt', 'layout-video-crash.log'):
    content = Path(name).read_text()
    assert 'FATAL EXCEPTION' not in content, name
    assert not any('Fatal signal' in line and '(com.jarvis.food)' in line for line in content.splitlines()), name
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
              original_signature_verified=True, visual_revision='Videos 1000196321, 1000196336, 1000196340: equal plan controls, distinct healthy style, actionable diet time limits, joint day balancing, compact daily tips and product nutrition profiles; based on 1.0.201',
              checks=list(logs), offline_food_model_included=True)
verification = Path('dist/verification.json'); verification.write_text(json.dumps(record, indent=2)+'\n')
notes = Path('dist/release-notes.md')
notes.write_text(f'''Modifiche richieste nei video 1000196321, 1000196336 e 1000196340 del 2 novembre 2026, basate sulla versione 1.0.201.

- Piano: pulsanti ON/OFF e Alimentazione della stessa grandezza, con spazio prima dei dettagli nutrizionali.
- Alimentazione: nuova voce Salutare distinta da Bilanciata e varia; controlli su tutti gli stili. I limiti dei tempi si distinguono dalle esclusioni, con correzione esplicita prima di confermare. Target e diario conservati.
- Calcolo: bilanciamento congiunto delle quantità soltanto per le giornate fuori tolleranza, rispettando porzioni massime e pasti fissi o già registrati.
- Home: tre consigli brevi con illustrazioni e colori, personalizzati quando il diario lo consente oppure generali; rotazione giornaliera e fonti apribili toccando il consiglio. Rimossa la lunga guida espandibile.
- Alimenti: profilo con pallino colorato, punti positivi e aspetti da considerare. Nutri-Score A–E quando fornito da Open Food Facts; altrimenti sintesi trasparente dei nutrienti noti. Ingredienti, additivi e bio solo dai dati disponibili; nessun voto /100 o indice glicemico inventato. Alternative apribili con un pulsante.
- Conservate tutte le modifiche della versione 1.0.201, inclusi ricerca ricette e pasti personali.

Firma originale verificata. {counts['tests']} test unitari e {sum(logs.values())} esecuzioni Android superati. Le prove non verificano sul telefono personale i servizi online e l'AI con la chiave dell'utente.
''')
subprocess.run(['gh','release','create',f'preview-v1.0.{number}',str(apk),str(verification),
                '--target',sha,'--title',f'Jarvis Food 1.0.{number} · Revisione grafica','--prerelease','--notes-file',str(notes)], check=True)
print('VERIFICATION_RESULT='+json.dumps(record))
print(f'DOWNLOAD_URL=https://github.com/IMSLEPT/jarvis-food-android/releases/download/preview-v1.0.{number}/{apk.name}')
