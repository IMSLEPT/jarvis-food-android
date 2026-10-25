"""Resumable, network-free batches around the existing photography tools.

This manages work and reviews; it cannot call the chat's included image generator.
It never starts a paid service, downloads a model, commits, pushes or builds APKs.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

from PIL import Image, ImageDraw, ImageOps
from plan import briefs

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'tools/photography'
MANIFEST = SOURCE / 'approved.json'
INDEX = ROOT / 'app/src/main/java/com/jarvis/food/ui/EditorialPhotos.kt'
ASSETS = ROOT / 'app/src/main/assets'
CHECKS = ('ingredients', 'stage', 'realism', 'style', 'squareCrop')


def read(path):
    return json.loads(Path(path).read_text())


def write(path, value):
    path = Path(path)
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    temporary.replace(path)


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def catalog():
    return read(ASSETS / 'recipes.json')['recipes']


def complete(recipe, records):
    return recipe['id'] in records and all(
        f"{recipe['id']}_s{i}" in records for i in range(len(recipe['steps'])))


def statistics(records):
    recipes = catalog()
    done = [r for r in recipes if complete(r, records)]
    pending = [r for r in recipes if not complete(r, records)]
    items = list(briefs())
    ready = {i['group'] for i in items if i['id'] in records}
    return dict(recipes=len(recipes), complete=len(done), remaining=len(pending),
                images=len({v['asset'] for v in records.values()}), mappings=len(records),
                coveredSteps=sum(len(r['steps']) for r in done),
                missingSlots=len(items) - len(records),
                imagesToProduce=len({i['group'] for i in items} - ready),
                nextRecipe=pending[0]['id'] if pending else None)


def create(directory, count):
    directory = Path(directory).resolve()
    if (directory / 'batch.json').exists():
        raise ValueError('Batch already exists. Use status; never replace in-flight work.')
    if not 1 <= count <= 5:
        raise ValueError('Select one to five complete recipes per batch.')
    records = read(MANIFEST)
    recipes = [r for r in catalog() if not complete(r, records)][:count]
    if not recipes:
        raise ValueError('All recipes are complete.')
    selected = {r['id'] for r in recipes}
    items = list(briefs())
    ready = {i['group']: i['id'] for i in items if i['id'] in records}
    tasks, by_group, reuses = [], {}, []
    for item in items:
        if item['recipeId'] not in selected or item['id'] in records:
            continue
        if item['group'] in ready:
            reuses.append({'id': item['id'], 'reuse': ready[item['group']]})
        elif item['group'] in by_group:
            by_group[item['group']]['aliases'].append(item['id'])
        else:
            task = {**item, 'aliases': [], 'candidate': None, 'review': None}
            tasks.append(task)
            by_group[item['group']] = task
    state = dict(schema=1, createdAt=datetime.now(timezone.utc).isoformat(),
                 recipes=recipes, catalogSha256={name: digest(ASSETS / name)
                                                for name in ('recipes.json', 'foods.json')},
                 baseline=records, tasks=tasks, reuses=reuses, integrated=False)
    directory.mkdir(parents=True, exist_ok=True)
    write(directory / 'batch.json', state)
    (directory / 'requests.jsonl').write_text(''.join(
        json.dumps({k: t[k] for k in ('id', 'recipeId', 'stepIndex', 'prompt', 'aliases')},
                   ensure_ascii=False) + '\n' for t in tasks))
    (directory / 'BRIEF.md').write_text(
        '# Gruppo fotografico da completare\n\n'
        'Usare requests.jsonl e il contesto esatto in batch.json. Un soggetto per foto.\n'
        'Il generatore incluso si chiama dalla sessione assistita, non da Python.\n'
        'Nessuna API a pagamento. Nessuna approvazione automatica.\n\n'
        + ''.join(f"- {r['name']} (`{r['id']}`)\n" for r in recipes)
        + '\nPer ogni immagine: ingredienti, fase, realismo, stile e ritaglio quadrato.\n'
        'Registrare il candidato, vedere originale e anteprima, poi registrare la revisione.\n'
        'Integrare solo il gruppo completo. Commit [skip ci] e push prima del successivo.\n')
    return dict(directory=str(directory), recipes=list(selected), images=len(tasks),
                reusedSlots=len(reuses), statistics=statistics(records))


def task_for(state, key):
    return next(t for t in state['tasks'] if t['id'] == key)


def candidate(directory, key, path, prompt, kind='ai_illustration', source=None):
    state = read(directory / 'batch.json')
    if state['integrated']:
        raise ValueError('Batch already integrated.')
    task = task_for(state, key)
    path = Path(path).resolve()
    with Image.open(path) as im:
        im.load()
        if min(im.size) < 800:
            raise ValueError('Candidate must have at least 800 pixels on each side.')
        preview = ImageOps.fit(ImageOps.exif_transpose(im).convert('RGB'), (800, 800))
        preview.save(directory / f'{key}.square.jpg', quality=93)
    if kind not in ('ai_illustration', 'licensed_photo'):
        raise ValueError('Unsupported origin.')
    if not prompt.strip():
        raise ValueError('Record the actual generation prompt or photographic description.')
    if kind == 'licensed_photo':
        required = ('provider', 'author', 'pageUrl', 'originalUrl', 'license',
                    'licenseUrl', 'verifiedAt', 'changes')
        if not source or not all(source.get(k) for k in required):
            raise ValueError('Verified license and complete author/source metadata required.')
        if not all(source[k].startswith('https://') for k in ('pageUrl', 'originalUrl', 'licenseUrl')):
            raise ValueError('Source and license URLs must use HTTPS.')
    task['candidate'] = dict(path=str(path), sha256=digest(path), prompt=prompt, kind=kind,
                             **({'source': source} if source else {}))
    task['review'] = None
    write(directory / 'batch.json', state)
    return dict(id=key, preview=str(directory / f'{key}.square.jpg'), review='required')


def review(directory, key, decision, reviewer, notes, checks):
    state = read(directory / 'batch.json')
    if state['integrated']:
        raise ValueError('Batch already integrated.')
    task = task_for(state, key)
    image = task['candidate']
    if not image or digest(image['path']) != image['sha256']:
        raise ValueError('Candidate missing or changed: register it and inspect it again.')
    if not reviewer.strip() or not notes.strip():
        raise ValueError('A reviewer and concrete visual observations are required.')
    if decision == 'approve' and set(checks) != set(CHECKS):
        raise ValueError('Explicit checks required: ' + ','.join(CHECKS))
    task['review'] = dict(decision=decision, reviewer=reviewer, notes=notes,
                          checks=checks, sha256=image['sha256'],
                          at=datetime.now(timezone.utc).isoformat())
    write(directory / 'batch.json', state)
    return dict(id=key, decision=decision)


def reviewed_inputs(state):
    inputs = []
    for task in state['tasks']:
        image, decision = task['candidate'], task['review']
        if not image or not decision or decision['decision'] != 'approve':
            raise ValueError('Unreviewed or rejected image: ' + task['id'])
        if set(decision['checks']) != set(CHECKS) or digest(image['path']) != decision['sha256'] or image['sha256'] != decision['sha256']:
            raise ValueError('Review no longer matches candidate: ' + task['id'])
        inputs.append({'id': task['id'], **{k: v for k, v in image.items() if k != 'sha256'}})
    return inputs


def update_progress(before, records, state):
    stats = statistics(records)
    progress = SOURCE / 'PROGRESS.md'
    text = progress.read_text()
    substitutions = [
        (r'- \d+ ricette complete di foto principale e di tutti i passaggi\.',
         f"- {stats['complete']} ricette complete di foto principale e di tutti i passaggi."),
        (r'- \d+ WebP distinti, \d+ abbinamenti verificati\.',
         f"- {stats['images']} WebP distinti, {stats['mappings']} abbinamenti verificati."),
        (r'- \d+ passaggi delle ricette complete coperti\.',
         f"- {stats['coveredSteps']} passaggi delle ricette complete coperti."),
        (r'- \d+ ricette da completare\.', f"- {stats['remaining']} ricette da completare.")]
    for pattern, replacement in substitutions:
        text, matches = re.subn(pattern, lambda _: replacement, text, count=1)
        if matches != 1:
            raise ValueError('Unrecognized PROGRESS summary. Update template before applying.')
    text = text.replace('Punto preciso da cui riprendere:', 'Punto di ripresa precedente (vedere ultimo gruppo):')
    names = ', '.join(r['id'] for r in state['recipes'])
    text += (f"\n## Gruppo con pipeline riprendibile — {datetime.now(timezone.utc).date()}\n\n"
             f"Ricette completate: {names}. Nuove immagini: {stats['images'] - before['images']}; "
             f"nuovi abbinamenti: {stats['mappings'] - before['mappings']}.\n\n"
             'Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. '
             'Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. '
             'File fotografici e record precedenti preservati; database e funzionalità invariati. '
             'Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.\n\n'
             f"Punto preciso da cui riprendere: **{stats['nextRecipe'] or 'catalogo completo'}**. "
             f"Restano {stats['remaining']} ricette e {stats['missingSlots']} abbinamenti; "
             f"{stats['imagesToProduce']} gruppi fotografici ancora da produrre. "
             'Commit [skip ci] e push del gruppo prima del successivo. Nessun processo in background avviato.\n')
    progress.write_text(text)
    path = SOURCE / 'README.md'
    text = path.read_text()
    text = re.sub(r'\d+ ricette complete, \d+ abbinamenti immagine e \d+ fotografie distinte',
                  f"{stats['complete']} ricette complete, {stats['mappings']} abbinamenti immagine e {stats['images']} fotografie distinte", text)
    text = re.sub(r'\d+ passaggi fotografici di \d+ ricette',
                  f"{stats['coveredSteps']} passaggi fotografici di {stats['complete']} ricette", text)
    text = re.sub(r'Restano \d+ ricette', f"Restano {stats['remaining']} ricette", text)
    path.write_text(text)


def integrate(directory):
    state = read(directory / 'batch.json')
    if state['integrated']:
        return dict(alreadyIntegrated=True, statistics=statistics(read(MANIFEST)))
    for name, sha in state['catalogSha256'].items():
        if digest(ASSETS / name) != sha:
            raise ValueError('Catalog changed; create a fresh batch.')
    records = read(MANIFEST)
    if records != state['baseline']:
        raise ValueError('Library changed since planning; replan without overwriting new work.')
    inputs = reviewed_inputs(state)
    for item in inputs:
        if item['id'] in records or (ASSETS / 'photography' / (item['id'] + '.webp')).exists():
            raise ValueError('Refusing to replace existing image: ' + item['id'])
    if any(i['kind'] == 'licensed_photo' for i in inputs):
        # Existing prepare.py supports these; credits must also be synchronized manually.
        raise ValueError('Licensed photo: use documented prepare.py workflow and synchronize both credit files before publishing.')
    old_assets = {p: digest(p) for p in (ASSETS / 'photography').iterdir() if p.is_file()}
    mutable = [MANIFEST, INDEX, SOURCE / 'PROGRESS.md', SOURCE / 'README.md']
    backup = {p: p.read_bytes() for p in mutable}
    before = statistics(records)
    write(directory / 'reviewed-inputs.json', inputs)
    try:
        for script, args in [('prepare.py', [str(directory / 'reviewed-inputs.json')]), ('share.py', []), ('verify.py', [])]:
            subprocess.run([sys.executable, str(SOURCE / script), *args], cwd=ROOT, check=True)
        subprocess.run([sys.executable, str(ROOT / 'scripts/verify-aurora-boundary.py')], cwd=ROOT, check=True)
        now = read(MANIFEST)
        if any(now.get(k) != v for k, v in records.items()) or any(digest(p) != sha for p, sha in old_assets.items()):
            raise ValueError('A pre-existing record or asset changed.')
        if not all(complete(r, now) for r in state['recipes']):
            raise ValueError('Incomplete recipe: the entire batch must be completed.')
        paths = {ASSETS / r['asset'] for r in now.values()}
        if len(paths) != len({digest(p) for p in paths}):
            raise ValueError('Duplicate image content: use only the existing compatible sharing rules.')
        for path in paths - set(old_assets):
            with Image.open(path) as image:
                image.load()
                if image.format != 'WEBP' or min(image.size) < 800 or max(image.size) > 1280:
                    raise ValueError('Invalid converted image: ' + str(path))
        kotlin = INDEX.read_text()
        mappings = dict(re.findall(r'^        "([^"/]+)" to "([^"]+)",$', kotlin, re.M))
        if mappings != {k: v['asset'] for k, v in now.items()}:
            raise ValueError('Kotlin index differs from the approved library.')
        update_progress(before, now, state)
        subprocess.run(['git', 'diff', '--check'], cwd=ROOT, check=True)
    except BaseException:
        for p, content in backup.items():
            p.write_bytes(content)
        for p in (ASSETS / 'photography').iterdir():
            if p.is_file() and p not in old_assets:
                p.unlink()
        raise
    audit = SOURCE / 'reviews' / (state['createdAt'].replace(':', '-') + '.json')
    audit.parent.mkdir(exist_ok=True)
    write(audit, dict(createdAt=state['createdAt'], recipes=[r['id'] for r in state['recipes']],
                     images=[dict(id=t['id'], prompt=t['candidate']['prompt'],
                                  originalSha256=t['candidate']['sha256'],
                                  webpSha256=now[t['id']]['sha256'], review=t['review'])
                             for t in state['tasks']]))
    state['integrated'] = True
    state['result'] = statistics(now)
    write(directory / 'batch.json', state)
    return dict(integrated=True, statistics=state['result'], next='Commit [skip ci] and push before planning next batch.')


def status(directory):
    state = read(directory / 'batch.json')
    return dict(integrated=state['integrated'], recipes=[r['id'] for r in state['recipes']],
                tasks=[dict(id=t['id'], candidate=bool(t['candidate']),
                            decision=t['review']['decision'] if t['review'] else 'pending') for t in state['tasks']],
                statistics=statistics(read(MANIFEST)))


def main():
    parser = argparse.ArgumentParser(__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('plan'); p.add_argument('--out', type=Path, required=True); p.add_argument('--recipes', type=int, default=1)
    for command in ('status', 'integrate', 'candidate', 'review'):
        p = sub.add_parser(command); p.add_argument('directory', type=Path)
        if command in ('candidate', 'review'):
            p.add_argument('id')
        if command == 'candidate':
            p.add_argument('path', type=Path); p.add_argument('--prompt-file', type=Path, required=True)
            p.add_argument('--kind', default='ai_illustration'); p.add_argument('--source-file', type=Path)
        if command == 'review':
            p.add_argument('--decision', choices=('approve', 'reject'), required=True)
            p.add_argument('--reviewer', required=True); p.add_argument('--notes', required=True)
            p.add_argument('--checks', default='')
    sub.add_parser('stats')
    args = parser.parse_args()
    if args.command == 'plan': result = create(args.out, args.recipes)
    elif args.command == 'stats': result = statistics(read(MANIFEST))
    elif args.command == 'candidate':
        result = candidate(args.directory, args.id, args.path, args.prompt_file.read_text(), args.kind,
                           read(args.source_file) if args.source_file else None)
    elif args.command == 'review':
        result = review(args.directory, args.id, args.decision, args.reviewer, args.notes,
                        [c for c in args.checks.split(',') if c])
    elif args.command == 'integrate': result = integrate(args.directory)
    else: result = status(args.directory)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, StopIteration, OSError, subprocess.CalledProcessError) as error:
        sys.exit(str(error) or 'Unknown task ID')
