"""Conservative photographic sharing: visually equivalent foods and identical actions.

Weights and dairy fat percentages do not change a picture. Different fruits,
vegetables, sauces, serving forms and cooking methods keep separate pictures.
"""
import hashlib
import json
import re

EQUIVALENT = {
    'greco_intero': 'greco0', 'yogurt_magro': 'yogurt',
    'latte_sl': 'latte_ps', 'latte_intero': 'latte_ps',
    'ricotta_light': 'ricotta', 'mozzarella_light': 'mozzarella',
    'pollo': 'petto_pollame', 'tacchino': 'petto_pollame',
    'merluzzo_nordico': 'merluzzo', 'eglefino': 'merluzzo', 'tilapia': 'merluzzo',
}
INVISIBLE = {'sale', 'acqua'}
COOKING = {'forno', 'padella', 'griglia', 'friggitrice', 'vapore', 'senza_cottura', 'freddo'}
FORMS = ('porridge', 'overnight', 'pancake', 'smoothie', 'frullato', 'yogurt', 'kefir',
         'toast', 'fette biscottate', 'panino', 'piadina', 'tacos', 'tortilla', 'wrap', 'pita',
         'pasta al forno', 'lasagne', 'risotto', 'spaghetti', 'linguine', 'tagliatelle',
         'pasta', 'fusilli', 'penne', 'rigatoni', 'garganelli', 'caserecce', 'mezze maniche',
         'curry', 'zuppa', 'minestra', 'vellutata', 'insalata', 'bowl', 'poke', 'wok',
         'noodles', 'ramen', 'burger', 'hamburger', 'polpette', 'frittata', 'omelette',
         'straccetti', 'scaloppine', 'cartoccio', 'arrosto', 'grigliat', 'al forno',
         'budino', 'pudding', 'mousse', 'torta', 'crumble', 'gelato', 'cheesecake', 'tiramisu')


def canonical(food):
    return EQUIVALENT.get(food, food)


def dish_signature(recipe):
    name = recipe['name'].lower()
    form = next((f for f in FORMS if f in name), recipe['id'])
    foods = sorted({canonical(i['food']) for i in recipe['ings'] if i['food'] not in INVISIBLE})
    method = sorted(set(recipe.get('methods', [])) & COOKING)
    return ['dish', form, method, foods]


def finishing(recipe, index):
    step = recipe['steps'][index].lower()
    return index == len(recipe['steps']) - 1 and bool(re.search(r'\b(servi|impiatta|completa|accompagna)\b', step))


def signature(recipe, index=None):
    if index is None or finishing(recipe, index):
        return dish_signature(recipe)
    step = recipe['steps'][index]
    refs = re.findall(r'\{([^}]+)\}', step)
    normal = re.sub(r'\{([^}]+)\}', lambda m: '{' + canonical(m[1]) + '}', step.lower())
    normal = re.sub(r'\d+(?:[.,]\d+)?', '#', normal)
    # Isolated preparation of explicit ingredients can be shared across recipes.
    # Mixing/finishing an existing pan or bowl requires the whole dish context.
    isolated = refs and re.match(r'^(taglia|pela|sbuccia|sciacqua|tosta|lessa|cuoci|griglia|sguscia|versa)\b', normal)
    context_words = ('stessa', 'restante', 'rimasto', 'aggiungi', 'unisci', 'salsa', 'condimento', 'brodo')
    if isolated and not any(word in normal for word in context_words):
        return ['step', normal, sorted({canonical(food) for food in refs})]
    return ['step', normal, dish_signature(recipe)]


def group_id(recipe, index=None):
    return hashlib.sha256(json.dumps(signature(recipe, index), ensure_ascii=False, sort_keys=True).encode()).hexdigest()[:20]
