"""Verify the reviewed library and compatible sharing without imaging dependencies."""
from pathlib import Path
import hashlib
import json
import re
from shared import group_id

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets'


def subject(recipes, key):
    if key in recipes:
        return recipes[key], None
    match = re.fullmatch(r'(.+)_s(\d+)', key)
    assert match and match[1] in recipes, key
    recipe, step = recipes[match[1]], int(match[2])
    assert 0 <= step < len(recipe['steps']), key
    return recipe, step


def main():
    recipes = {r['id']: r for r in json.loads((ASSETS / 'recipes.json').read_text())['recipes']}
    records = json.loads(Path(__file__).with_name('approved.json').read_text())
    shared = 0
    for key, record in records.items():
        recipe, index = subject(recipes, key)
        assert record['subject'] == (recipe['name'] if index is None else recipe['steps'][index]), key
        path = record['asset']
        assert path.startswith('photography/') and '..' not in path, key
        data = (ASSETS / path).read_bytes()
        assert len(data) > 10000 and data[:4] == b'RIFF' and data[8:12] == b'WEBP', key
        assert hashlib.sha256(data).hexdigest() == record['sha256'], key
        if 'sharedFrom' in record:
            source, source_index = subject(recipes, record['sharedFrom'])
            assert group_id(recipe, index) == group_id(source, source_index), key
            assert records[record['sharedFrom']]['asset'] == path, key
            shared += 1
    heroes = [key for key in records if key in recipes]
    for key in heroes:
        assert all(f'{key}_s{i}' in records for i in range(len(recipes[key]['steps']))), key
    print(f'Photography OK: {len(heroes)} complete recipes, {len(records)} mappings, {shared} compatible shared uses.')


if __name__ == '__main__':
    main()
