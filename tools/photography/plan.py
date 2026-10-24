"""Inventory and recipe-specific photo briefs. Does not generate or fetch images.

Run with --out pending.jsonl to export unfinished briefs for the image tool.
Only reviewed assets in approved.json are packaged in the Android app.
"""
from pathlib import Path
import argparse
import json
import re
from shared import group_id

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets'
STYLE = (
    'One photorealistic editorial cookbook photograph. Soft natural window light, '
    'a bright warm home kitchen, light wood, neutral ivory ceramics, realistic '
    'appetizing food textures and achievable homemade plating. Tight food framing '
    'that remains meaningful in a square crop. No text, labels, watermark, collage '
    'or UI. Show the actual ingredients/action described; no invented garnishes. '
    'Focus on food; hands are allowed when needed, no portraits or visible faces. '
)


def briefs():
    foods = {f['id']: f for f in json.loads((ASSETS / 'foods.json').read_text())['foods']}
    recipes = json.loads((ASSETS / 'recipes.json').read_text())['recipes']
    for recipe in recipes:
        names = {i['food']: i.get('label') or foods[i['food']]['name'] for i in recipe['ings']}
        ingredients = ', '.join(dict.fromkeys(names.values()))
        base = f'Recipe: {recipe["name"]}. Ingredients: {ingredients}. '
        resolved = [re.sub(r'\{([^}]+)\}', lambda match: names.get(match[1], match[1]), step) for step in recipe['steps']]
        preparation = ' Preparation context (do not print it): ' + ' '.join(resolved) + ' '
        yield {'id': recipe['id'], 'recipeId': recipe['id'], 'stepIndex': None, 'group': group_id(recipe),
               'prompt': STYLE + base + preparation + 'Show the completed dish, all components actually present, beautifully plated. Match the actual cooking method and serving form. No ingredients absent from this list.'}
        for index, step in enumerate(recipe['steps']):
            action = re.sub(r'\{([^}]+)\}', lambda match: names.get(match[1], match[1]), step)
            yield {'id': f'{recipe["id"]}_s{index}', 'recipeId': recipe['id'], 'stepIndex': index, 'group': group_id(recipe, index),
                   'prompt': STYLE + base + 'Earlier actions (context only): ' + ' '.join(resolved[:index]) + f' Focal action to photograph: {action}. Only show ingredients already used at this stage; no finished-dish photo in place of an intermediate action.'}


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('--out', type=Path)
    parser.add_argument('--legacy-only', action='store_true', help='Prioritize replacing every currently shipped photograph.')
    args = parser.parse_args()
    approved_path = Path(__file__).with_name('approved.json')
    approved = json.loads(approved_path.read_text()) if approved_path.exists() else {}
    items = list(briefs())
    if args.legacy_only:
        old = {r['id'] for r in json.loads((ASSETS / 'recipes.json').read_text())['recipes'] if r.get('image')}
        items = [item for item in items if item['recipeId'] in old]
    pending = [item for item in items if item['id'] not in approved]
    groups = {}
    for item in items:
        groups.setdefault(item['group'], []).append(item)
    requests = []
    for group, members in groups.items():
        ready = next((item for item in members if item['id'] in approved), None)
        if ready:
            requests.append({'id': ready['id'], 'reuse': ready['id'], 'aliases': [m['id'] for m in members if m['id'] not in approved], 'group': group})
        else:
            requests.append({**members[0], 'aliases': [m['id'] for m in members[1:]]})
    heroes = [item for item in items if item['stepIndex'] is None]
    complete = sum(all(item['id'] in approved for item in items if item['recipeId'] == hero['id']) for hero in heroes)
    print(json.dumps({'recipes': len(heroes), 'steps': len(items) - len(heroes),
                      'reviewed_slots': sum(item['id'] in approved for item in items), 'pending_slots': len(pending),
                      'recipes_with_complete_new_photography': complete,
                      'shared_groups': len(groups), 'images_to_produce': sum('reuse' not in r for r in requests)}, indent=2))
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(''.join(json.dumps(item, ensure_ascii=False) + '\n' for item in requests if 'reuse' not in item or item['aliases']))


if __name__ == '__main__':
    main()
