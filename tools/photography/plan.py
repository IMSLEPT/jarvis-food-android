"""Inventory and recipe-specific photo briefs. Does not generate or fetch images.

Run with --out pending.jsonl to export unfinished briefs for the image tool.
Only reviewed assets in approved.json are packaged in the Android app.
"""
from pathlib import Path
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets'
STYLE = (
    'One photorealistic editorial cookbook photograph. Soft natural window light, '
    'a bright warm home kitchen, light wood, neutral ivory ceramics, realistic '
    'appetizing food textures and achievable homemade plating. Tight food framing '
    'that remains meaningful in a square crop. No text, labels, watermark, collage '
    'or UI. Show the actual ingredients/action described; no invented garnishes. '
)


def briefs():
    foods = {f['id']: f for f in json.loads((ASSETS / 'foods.json').read_text())['foods']}
    recipes = json.loads((ASSETS / 'recipes.json').read_text())['recipes']
    for recipe in recipes:
        names = {i['food']: i.get('label') or foods[i['food']]['name'] for i in recipe['ings']}
        ingredients = ', '.join(dict.fromkeys(names.values()))
        base = f'Recipe: {recipe["name"]}. Ingredients: {ingredients}. '
        yield {'id': recipe['id'], 'recipeId': recipe['id'], 'stepIndex': None,
               'prompt': STYLE + base + 'Show the completed dish, all components actually present, beautifully plated. No ingredients absent from this list.'}
        for index, step in enumerate(recipe['steps']):
            action = re.sub(r'\{([^}]+)\}', lambda match: names.get(match[1], match[1]), step)
            yield {'id': f'{recipe["id"]}_s{index}', 'recipeId': recipe['id'], 'stepIndex': index,
                   'prompt': STYLE + base + f'Show this preparation action: {action}. Only show ingredients already used at this stage; no finished-dish photo in place of an intermediate action.'}


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('--out', type=Path)
    args = parser.parse_args()
    approved_path = Path(__file__).with_name('approved.json')
    approved = json.loads(approved_path.read_text()) if approved_path.exists() else {}
    items = list(briefs())
    pending = [item for item in items if item['id'] not in approved]
    heroes = [item for item in items if item['stepIndex'] is None]
    complete = sum(all(item['id'] in approved for item in items if item['recipeId'] == hero['id']) for hero in heroes)
    print(json.dumps({'recipes': len(heroes), 'steps': len(items) - len(heroes),
                      'reviewed_slots': len(approved), 'pending_slots': len(pending),
                      'recipes_with_complete_new_photography': complete}, indent=2))
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(''.join(json.dumps(item, ensure_ascii=False) + '\n' for item in pending))


if __name__ == '__main__':
    main()
