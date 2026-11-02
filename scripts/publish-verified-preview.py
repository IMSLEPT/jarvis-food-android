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
logs = {'layout-food-followup-standard.log':4, 'layout-food-followup-large-text.log':4, 'layout-november-standard.log':3, 'layout-november-large-text.log':3, 'layout-profile-standard.log':2, 'layout-profile-large-text.log':2,
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
              original_signature_verified=True, visual_revision='Videos 1000196317 and 1000196318: food guidance, immediate general advice, recipe search and manual planned meals with quantity assistance; based on 1.0.185',
              checks=list(logs), offline_food_model_included=True)
verification = Path('dist/verification.json'); verification.write_text(json.dumps(record, indent=2)+'\n')
notes = Path('dist/release-notes.md')
notes.write_text(f'''Modifiche richieste nei video 1000196317 e 1000196318 del 2 novembre 2026, basate sulla versione 1.0.185.

- Cosa mangio adesso: area di scorrimento contenuta, testata opaca e spazio riservato ai comandi Android.
- Ogni alimento: indicatori testuali e colorati per zuccheri, saturi e sale, quantità e frequenza registrata, alternative e confronto online delle etichette di prodotti/marche. Dati mancanti espliciti; nessuna classificazione della lavorazione o frequenza inventata dalla marca.
- Vitamine e minerali: guida pratica consultabile subito, anche senza storico; distinta dall'analisi dei dati registrati.
- Alternative: ricerca per nome/ingredienti nel catalogo, ricette personali e compositore con alimenti del catalogo, personalizzati o di marca.
- Compositore: confronto calorie e macro di pasto/giornata, proposta limitata delle quantità da approvare, salvataggio persistente nel piano. Il diario cambia solo premendo Segna mangiato.

Firma originale verificata. {counts['tests']} test unitari e {sum(logs.values())} esecuzioni Android superati. Le prove non verificano sul telefono personale i servizi online e l'AI con la chiave dell'utente.
''')
subprocess.run(['gh','release','create',f'preview-v1.0.{number}',str(apk),str(verification),
                '--target',sha,'--title',f'Jarvis Food 1.0.{number} · Revisione grafica','--prerelease','--notes-file',str(notes)], check=True)
print('VERIFICATION_RESULT='+json.dumps(record))
print(f'DOWNLOAD_URL=https://github.com/IMSLEPT/jarvis-food-android/releases/download/preview-v1.0.{number}/{apk.name}')
