"""Reuse reviewed compatible photos without publishing incomplete recipe heroes."""
from pathlib import Path
import json
import subprocess
import sys
import tempfile
from plan import briefs


def main():
    directory = Path(__file__).resolve().parent
    records = json.loads((directory / 'approved.json').read_text())
    items = list(briefs())
    sources = {item['group']: item['id'] for item in items if item['id'] in records}
    candidates = [item for item in items if item['id'] not in records and item['group'] in sources]
    available = set(records) | {item['id'] for item in candidates}
    steps = {}
    for item in items:
        if item['stepIndex'] is not None:
            steps.setdefault(item['recipeId'], []).append(item['id'])
    approved = [
        {'id': item['id'], 'reuse': sources[item['group']]}
        for item in candidates
        if item['stepIndex'] is not None or all(key in available for key in steps[item['recipeId']])
    ]
    if not approved:
        print('No additional compatible photographs to share.')
        return
    with tempfile.TemporaryDirectory(prefix='jarvis-photography-') as temporary:
        inputs = Path(temporary) / 'aliases.json'
        inputs.write_text(json.dumps(approved, ensure_ascii=False))
        subprocess.run([sys.executable, str(directory / 'prepare.py'), str(inputs)], check=True)
    print(f'Shared {len(approved)} compatible photographs; incomplete heroes stay unpublished.')


if __name__ == '__main__':
    main()
