"""Query di ricerca (in inglese) per le foto dei piatti e dei passaggi."""
import re, sys, os
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R

EN = {"avena": "oats", "latte_sl": "milk", "latte_ps": "milk", "banana": "banana", "noci": "walnuts", "greco0": "greek yogurt",
      "granola": "granola", "mirtilli": "blueberries", "mandorle": "almonds", "miele": "honey", "pane_int": "whole wheat bread",
      "uova": "eggs", "avocado": "avocado", "pomodori": "tomatoes", "albume": "egg whites", "chia": "chia seeds", "kiwi": "kiwi",
      "fette_bisc": "rusks", "arachidi": "peanut butter", "fiocchi_latte": "cottage cheese", "mela": "apple", "nocciole": "hazelnuts",
      "cornflakes": "cornflakes", "fragole": "strawberries", "pera": "pear", "frutti_bosco_surg": "berries", "pasta": "pasta",
      "pollo": "chicken breast", "zucchine": "zucchini", "olio": "olive oil", "parmigiano": "parmesan", "tonno_nat": "tuna",
      "rucola": "arugula", "ceci": "chickpeas", "passata": "tomato sauce", "lenticchie": "lentils", "carote": "carrots",
      "basmati": "rice", "salmone": "salmon", "cetrioli": "cucumber", "edamame": "edamame", "farro": "farro", "feta": "feta",
      "olive": "olives", "riso": "rice", "zucca": "pumpkin", "quinoa": "quinoa", "peperoni": "bell peppers", "pasta_int": "pasta",
      "patate": "potatoes", "fagiolini": "green beans", "pesto": "pesto", "couscous": "couscous", "riso_int": "brown rice",
      "tacchino": "turkey", "broccoli": "broccoli", "piadina": "flatbread", "fesa_tacchino": "turkey slices",
      "mozzarella_light": "mozzarella", "lattuga": "lettuce", "mais": "corn", "pane": "bread", "merluzzo": "cod fillet",
      "spinaci": "spinach", "manzo_magro": "beef", "melanzane": "eggplant", "funghi": "mushrooms", "macinato": "ground beef",
      "tofu": "tofu", "branzino": "sea bass", "gamberi": "shrimp", "prosciutto_cotto": "ham", "trota": "trout",
      "patate_dolci": "sweet potatoes", "ricotta_light": "ricotta", "marmellata": "jam", "mandarini": "tangerines",
      "gallette": "rice cakes", "hummus": "hummus", "lupini": "lupini beans", "crackers": "crackers", "provola": "cheese",
      "whey": "protein powder", "kefir": "kefir", "fondente": "dark chocolate", "lenticchie_secche": "lentils"}
VERBS = [(r"\b(frulla)", "blending {i} in blender"), (r"\b(taglia|trita|affetta|sbuccia)", "cutting {i} on cutting board"),
         (r"\b(versa)", "pouring {i}"), (r"\b(spalma)", "spreading {i} on bread"), (r"\bforno\b", "{i} in baking tray oven"),
         (r"\b(griglia|piastra)", "grilling {i}"), (r"\b(salta|soffriggi|rosola|scalda|trifola|cuoci .* in padella|scotta)", "sauteing {i} in pan"),
         (r"\b(cuoci|lessa|porta a bollore|bollente)", "cooking {i} in pot boiling water"), (r"\b(sbatti)", "whisking {i} in bowl"),
         (r"\b(mescola|unisci|aggiungi|componi)", "mixing {i} in bowl"), (r"\b(servi|completa|accompagna|bevi)", "{dish} served")]

def hero_queries(r):
    q = r["img"]; words = q.split()
    return [q, " ".join(words[:3]), " ".join(words[:2])]

def step_query(r, st):
    keys = re.findall(r"\{([a-z0-9_]+)\}", st)
    ing = " and ".join(EN.get(k, k.replace("_", " ")) for k in keys[:2]) or " ".join(r["img"].split()[:2])
    low = st.lower()
    for pat, tpl in VERBS:
        if re.search(pat, low):
            return tpl.format(i=ing, dish=" ".join(r["img"].split()[:3]))
    return f"{ing} cooking"

if __name__ == "__main__":
    for r in R[:6]:
        print(r["id"], hero_queries(r))
        for st in r["steps"]: print("   ", step_query(r, st))
