package com.jarvis.food.data

/**
 * Database alimenti generici integrato (funziona offline).
 * Valori medi indicativi per 100 g / 100 ml, ricavati da tabelle di composizione
 * di riferimento (CREA, USDA). I prodotti confezionati si cercano su Open Food Facts.
 */
object FoodBase {

    private fun f(
        id: String, name: String, cat: String,
        kcal: Number, p: Number, c: Number, fat: Number,
        fib: Number? = null, sug: Number? = null, salt: Number? = null,
        serving: String? = null, sg: Number? = null, liquid: Boolean = false
    ) = Food(
        id = "base:$id", name = name, kcal = kcal.toDouble(), protein = p.toDouble(),
        carbs = c.toDouble(), fat = fat.toDouble(), fiber = fib?.toDouble(),
        sugar = sug?.toDouble(), salt = salt?.toDouble(), servingName = serving,
        servingGrams = sg?.toDouble(), source = "base", liquid = liquid, category = cat
    )

    val foods: List<Food> = listOf(
        // Cereali, pane, pasta
        f("pasta", "Pasta di semola (cruda)", "Cereali", 353, 13, 71, 1.5, 3, 3.5, 0, "porzione", 80),
        f("pasta_cotta", "Pasta di semola (cotta)", "Cereali", 150, 5.5, 30, 0.8, 1.7, 0.6, 0, "piatto", 200),
        f("pasta_int", "Pasta integrale (cruda)", "Cereali", 340, 13.4, 64, 2.5, 8, 3, 0, "porzione", 80),
        f("riso", "Riso bianco (crudo)", "Cereali", 358, 7, 79, 0.6, 1, 0.2, 0, "porzione", 80),
        f("riso_cotto", "Riso bianco (cotto)", "Cereali", 130, 2.7, 28, 0.3, 0.4, 0.1, 0, "piatto", 200),
        f("basmati", "Riso basmati (crudo)", "Cereali", 352, 8, 78, 0.8, 1.3, 0.2, 0, "porzione", 80),
        f("riso_int", "Riso integrale (crudo)", "Cereali", 350, 7.5, 74, 2.7, 3.5, 0.8, 0, "porzione", 80),
        f("pane", "Pane comune", "Cereali", 280, 8.5, 58, 1, 3, 2, 1.2, "fetta", 40),
        f("pane_int", "Pane integrale", "Cereali", 245, 9, 44, 3.4, 7, 3, 1.1, "fetta", 40),
        f("fette_bisc", "Fette biscottate", "Cereali", 410, 11.3, 76, 6, 3.5, 6, 1.2, "fetta", 10),
        f("crackers", "Crackers", "Cereali", 430, 9.5, 72, 11, 3, 3, 2, "pacchetto", 25),
        f("grissini", "Grissini", "Cereali", 430, 12, 69, 11, 3.5, 3, 2, "grissino", 6),
        f("avena", "Fiocchi d'avena", "Cereali", 372, 13.5, 59, 7, 10, 1, 0, "porzione", 40),
        f("cornflakes", "Corn flakes", "Cereali", 375, 7, 84, 0.9, 3, 8, 1.6, "porzione", 30),
        f("muesli", "Muesli", "Cereali", 365, 9.7, 66, 6, 7, 20, 0.1, "porzione", 40),
        f("farro", "Farro (crudo)", "Cereali", 338, 15, 67, 2.5, 7, 1, 0, "porzione", 80),
        f("quinoa", "Quinoa (cruda)", "Cereali", 368, 14, 64, 6, 7, 0, 0, "porzione", 70),
        f("couscous", "Cous cous (crudo)", "Cereali", 360, 12.8, 72, 0.6, 5, 0, 0, "porzione", 70),
        f("farina", "Farina di grano tipo 00", "Cereali", 340, 11, 72, 1, 2.2, 1.7, 0),
        f("gnocchi", "Gnocchi di patate", "Cereali", 150, 3.5, 32, 0.5, 2, 1, 0.8, "piatto", 200),
        f("pizza", "Pizza margherita", "Piatti pronti", 250, 10.5, 33, 8.5, 2.3, 3, 1.3, "pizza intera", 330),
        f("piadina", "Piadina", "Cereali", 330, 8, 52, 10, 2.5, 2, 1.5, "piadina", 120),
        f("polenta", "Polenta (cotta)", "Cereali", 85, 2, 18, 0.4, 1, 0, 0.3, "porzione", 250),
        f("patate", "Patate (crude)", "Verdura", 77, 2, 17, 0.1, 2.2, 0.8, 0, "patata media", 170),
        f("patate_forno", "Patate al forno (con olio)", "Verdura", 150, 2.5, 22, 6, 2.5, 1, 0.4, "porzione", 200),
        f("patatine_fritte", "Patatine fritte", "Piatti pronti", 312, 3.4, 41, 15, 3.8, 0.3, 0.5, "porzione", 120),
        f("patate_dolci", "Patate dolci (crude)", "Verdura", 86, 1.6, 20, 0.1, 3, 4.2, 0, "patata media", 150),

        // Carne, pesce, uova, salumi
        f("pollo_petto", "Petto di pollo (crudo)", "Carne", 105, 23, 0, 1.2, 0, 0, 0.2, "porzione", 150),
        f("pollo_petto_cotto", "Petto di pollo alla piastra", "Carne", 160, 31, 0, 3.5, 0, 0, 0.2, "porzione", 120),
        f("tacchino", "Petto di tacchino (crudo)", "Carne", 105, 24, 0, 1, 0, 0, 0.2, "porzione", 150),
        f("pollo_coscia", "Coscia di pollo con pelle", "Carne", 180, 18, 0, 12, 0, 0, 0.2, "coscia", 150),
        f("manzo_magro", "Manzo magro (crudo)", "Carne", 125, 21.5, 0, 4.5, 0, 0, 0.1, "porzione", 150),
        f("macinato", "Macinato di manzo 15% grassi", "Carne", 215, 18.5, 0, 15.5, 0, 0, 0.2, "porzione", 120),
        f("bistecca", "Bistecca di manzo", "Carne", 150, 21, 0, 7, 0, 0, 0.1, "bistecca", 200),
        f("maiale_lonza", "Lonza di maiale (cruda)", "Carne", 145, 21, 0, 6.5, 0, 0, 0.1, "porzione", 150),
        f("salmone", "Salmone (crudo)", "Pesce", 200, 20, 0, 13, 0, 0, 0.1, "trancio", 150),
        f("tonno_nat", "Tonno al naturale (sgocciolato)", "Pesce", 103, 23.5, 0, 1, 0, 0, 1, "scatoletta", 56),
        f("tonno_olio", "Tonno sott'olio (sgocciolato)", "Pesce", 190, 25, 0, 10, 0, 0, 1, "scatoletta", 52),
        f("merluzzo", "Merluzzo (crudo)", "Pesce", 75, 17, 0, 0.7, 0, 0, 0.2, "filetto", 150),
        f("orata", "Orata (cruda)", "Pesce", 121, 20, 0, 4.5, 0, 0, 0.2, "porzione", 150),
        f("gamberi", "Gamberi (crudi)", "Pesce", 85, 18, 1, 1, 0, 0, 0.6, "porzione", 120),
        f("uova", "Uova intere", "Uova", 143, 12.6, 0.7, 9.5, 0, 0.4, 0.4, "uovo medio", 55),
        f("albume", "Albume d'uovo", "Uova", 48, 11, 0.7, 0.2, 0, 0.7, 0.4, "albume", 33),
        f("crudo", "Prosciutto crudo", "Salumi", 268, 26, 0, 18.4, 0, 0, 5, "fetta", 15),
        f("cotto", "Prosciutto cotto", "Salumi", 135, 19, 1, 6, 0, 1, 2, "fetta", 20),
        f("bresaola", "Bresaola", "Salumi", 155, 33, 0, 2.5, 0, 0, 4.3, "porzione", 50),
        f("salame", "Salame", "Salumi", 400, 25, 1, 33, 0, 1, 4.5, "fetta", 10),
        f("mortadella", "Mortadella", "Salumi", 310, 15, 0.5, 28, 0, 0.5, 2.2, "fetta", 20),
        f("speck", "Speck", "Salumi", 300, 28, 0.5, 20, 0, 0, 4.5, "fetta", 15),
        f("wurstel", "Würstel", "Salumi", 270, 13, 2, 23, 0, 1, 2.2, "würstel", 50),

        // Legumi e proteine vegetali
        f("tofu", "Tofu", "Legumi", 120, 13, 1.5, 7, 1, 0.5, 0, "porzione", 100),
        f("seitan", "Seitan", "Legumi", 120, 24, 4, 1.5, 0.5, 0.5, 0.8, "porzione", 100),
        f("ceci_secchi", "Ceci secchi (crudi)", "Legumi", 340, 20, 47, 6, 14, 10, 0, "porzione", 50),
        f("ceci", "Ceci in scatola (sgocciolati)", "Legumi", 120, 7, 16, 2.5, 5, 1, 0.5, "porzione", 120),
        f("lenticchie_secche", "Lenticchie secche (crude)", "Legumi", 325, 24, 50, 1.5, 14, 2, 0, "porzione", 50),
        f("lenticchie", "Lenticchie (cotte)", "Legumi", 116, 9, 20, 0.4, 8, 1.8, 0, "porzione", 150),
        f("borlotti", "Fagioli borlotti (cotti)", "Legumi", 120, 8, 19, 0.6, 7, 0.5, 0, "porzione", 150),
        f("fagioli_scatola", "Fagioli in scatola (sgocciolati)", "Legumi", 100, 7, 15, 0.5, 6, 0.5, 0.5, "porzione", 120),
        f("piselli", "Piselli surgelati", "Legumi", 80, 5.5, 12, 0.5, 5, 4, 0, "porzione", 100),
        f("edamame", "Edamame", "Legumi", 120, 12, 9, 5, 5, 2, 0, "porzione", 100),

        // Latte e derivati
        f("latte_intero", "Latte intero", "Latticini", 64, 3.3, 4.8, 3.6, 0, 4.8, 0.1, "tazza", 250, true),
        f("latte_ps", "Latte parzialmente scremato", "Latticini", 46, 3.4, 5, 1.6, 0, 5, 0.1, "tazza", 250, true),
        f("latte_scremato", "Latte scremato", "Latticini", 35, 3.5, 5, 0.1, 0, 5, 0.1, "tazza", 250, true),
        f("soia_drink", "Bevanda di soia", "Latticini", 40, 3.3, 2.5, 1.8, 0.5, 2.5, 0.1, "tazza", 250, true),
        f("avena_drink", "Bevanda d'avena", "Latticini", 45, 0.8, 7, 1.5, 0.8, 4, 0.1, "tazza", 250, true),
        f("yogurt", "Yogurt bianco intero", "Latticini", 66, 3.8, 4.3, 3.9, 0, 4.3, 0.1, "vasetto", 125),
        f("greco0", "Yogurt greco 0%", "Latticini", 57, 10, 4, 0.2, 0, 4, 0.1, "vasetto", 170),
        f("greco", "Yogurt greco intero", "Latticini", 95, 9, 3.5, 5, 0, 3.5, 0.1, "vasetto", 150),
        f("skyr", "Skyr", "Latticini", 63, 11, 4, 0.2, 0, 4, 0.1, "vasetto", 150),
        f("parmigiano", "Parmigiano Reggiano", "Latticini", 392, 33, 0, 28.4, 0, 0, 1.6, "cucchiaio grattugiato", 10),
        f("grana", "Grana Padano", "Latticini", 398, 33, 0, 29, 0, 0, 1.5, "cucchiaio grattugiato", 10),
        f("mozzarella", "Mozzarella vaccina", "Latticini", 250, 18.5, 1, 19.5, 0, 1, 0.5, "mozzarella", 125),
        f("mozzarella_light", "Mozzarella light", "Latticini", 165, 19, 1.5, 9, 0, 1.5, 0.5, "mozzarella", 125),
        f("ricotta", "Ricotta vaccina", "Latticini", 146, 8.8, 3.5, 10.9, 0, 3.5, 0.3, "porzione", 100),
        f("fiocchi_latte", "Fiocchi di latte", "Latticini", 100, 12, 2.5, 4.5, 0, 2.5, 0.8, "vasetto", 150),
        f("stracchino", "Stracchino", "Latticini", 300, 18.5, 0, 25, 0, 0, 1.2, "porzione", 100),
        f("gorgonzola", "Gorgonzola", "Latticini", 330, 19, 0, 28, 0, 0, 1.7, "porzione", 50),
        f("pecorino", "Pecorino romano", "Latticini", 390, 28, 0, 31, 0, 0, 4.5, "cucchiaio grattugiato", 10),
        f("burro", "Burro", "Grassi", 750, 0.8, 0.6, 83, 0, 0.6, 0, "noce", 10),
        f("panna", "Panna da cucina", "Latticini", 200, 2.5, 3.5, 20, 0, 3.5, 0.1, "cucchiaio", 15),
        f("spalmabile", "Formaggio spalmabile", "Latticini", 235, 5.5, 4, 22, 0, 4, 0.8, "porzione", 30),
        f("whey", "Proteine whey in polvere", "Integratori", 380, 78, 7, 5, 0, 5, 0.5, "misurino", 30),

        // Frutta
        f("mela", "Mela", "Frutta", 52, 0.3, 14, 0.2, 2.4, 10, 0, "mela media", 180),
        f("banana", "Banana", "Frutta", 89, 1.1, 23, 0.3, 2.6, 12, 0, "banana media", 120),
        f("arancia", "Arancia", "Frutta", 47, 0.9, 12, 0.1, 2.4, 9, 0, "arancia media", 200),
        f("pera", "Pera", "Frutta", 57, 0.4, 15, 0.1, 3.1, 10, 0, "pera media", 170),
        f("kiwi", "Kiwi", "Frutta", 61, 1.1, 15, 0.5, 3, 9, 0, "kiwi", 75),
        f("fragole", "Fragole", "Frutta", 32, 0.7, 7.7, 0.3, 2, 4.9, 0, "porzione", 150),
        f("uva", "Uva", "Frutta", 69, 0.7, 18, 0.2, 0.9, 15.5, 0, "grappolo", 150),
        f("ananas", "Ananas", "Frutta", 50, 0.5, 13, 0.1, 1.4, 10, 0, "fetta", 100),
        f("pesca", "Pesca", "Frutta", 39, 0.9, 10, 0.3, 1.5, 8.4, 0, "pesca media", 150),
        f("albicocche", "Albicocche", "Frutta", 48, 1.4, 11, 0.4, 2, 9, 0, "albicocca", 40),
        f("anguria", "Anguria", "Frutta", 30, 0.6, 7.6, 0.2, 0.4, 6.2, 0, "fetta", 300),
        f("melone", "Melone", "Frutta", 34, 0.8, 8, 0.2, 0.9, 7.9, 0, "fetta", 200),
        f("mirtilli", "Mirtilli", "Frutta", 57, 0.7, 14.5, 0.3, 2.4, 10, 0, "porzione", 100),
        f("lamponi", "Lamponi", "Frutta", 52, 1.2, 12, 0.7, 6.5, 4.4, 0, "porzione", 100),
        f("mandarino", "Mandarino", "Frutta", 53, 0.8, 13, 0.3, 1.8, 10.6, 0, "mandarino", 80),
        f("avocado", "Avocado", "Frutta", 160, 2, 8.5, 14.7, 6.7, 0.7, 0, "mezzo avocado", 75),
        f("cachi", "Cachi", "Frutta", 70, 0.6, 18.6, 0.2, 3.6, 12.5, 0, "caco", 200),
        f("fichi", "Fichi", "Frutta", 74, 0.8, 19, 0.3, 2.9, 16, 0, "fico", 50),
        f("datteri", "Datteri secchi", "Frutta", 282, 2.5, 75, 0.4, 8, 63, 0, "dattero", 8),

        // Verdura
        f("zucchine", "Zucchine", "Verdura", 17, 1.2, 3.1, 0.3, 1, 2.5, 0, "zucchina", 200),
        f("pomodori", "Pomodori", "Verdura", 18, 0.9, 3.9, 0.2, 1.2, 2.6, 0, "pomodoro", 120),
        f("lattuga", "Insalata / lattuga", "Verdura", 15, 1.4, 2.9, 0.2, 1.3, 0.8, 0, "porzione", 80),
        f("rucola", "Rucola", "Verdura", 25, 2.6, 3.7, 0.7, 1.6, 2, 0, "porzione", 50),
        f("spinaci", "Spinaci", "Verdura", 23, 2.9, 3.6, 0.4, 2.2, 0.4, 0.2, "porzione", 200),
        f("broccoli", "Broccoli", "Verdura", 34, 2.8, 6.6, 0.4, 2.6, 1.7, 0, "porzione", 200),
        f("cavolfiore", "Cavolfiore", "Verdura", 25, 1.9, 5, 0.3, 2, 1.9, 0, "porzione", 200),
        f("carote", "Carote", "Verdura", 41, 0.9, 9.6, 0.2, 2.8, 4.7, 0.2, "carota", 80),
        f("peperoni", "Peperoni", "Verdura", 31, 1, 6, 0.3, 2.1, 4.2, 0, "peperone", 160),
        f("melanzane", "Melanzane", "Verdura", 25, 1, 5.9, 0.2, 3, 3.5, 0, "porzione", 200),
        f("cipolla", "Cipolla", "Verdura", 40, 1.1, 9.3, 0.1, 1.7, 4.2, 0, "cipolla", 100),
        f("funghi", "Funghi champignon", "Verdura", 22, 3.1, 3.3, 0.3, 1, 2, 0, "porzione", 150),
        f("fagiolini", "Fagiolini", "Verdura", 31, 1.8, 7, 0.2, 2.7, 3.3, 0, "porzione", 200),
        f("cetrioli", "Cetrioli", "Verdura", 15, 0.7, 3.6, 0.1, 0.5, 1.7, 0, "cetriolo", 200),
        f("asparagi", "Asparagi", "Verdura", 20, 2.2, 3.9, 0.1, 2.1, 1.9, 0, "porzione", 150),
        f("carciofi", "Carciofi", "Verdura", 47, 3.3, 10.5, 0.2, 5.4, 1, 0.2, "carciofo", 120),
        f("finocchi", "Finocchi", "Verdura", 31, 1.2, 7.3, 0.2, 3.1, 3.9, 0.1, "finocchio", 250),
        f("zucca", "Zucca", "Verdura", 26, 1, 6.5, 0.1, 0.5, 2.8, 0, "porzione", 200),
        f("mais", "Mais in scatola", "Verdura", 80, 2.9, 15, 1.2, 2.4, 4.5, 0.6, "porzione", 70),
        f("passata", "Passata di pomodoro", "Verdura", 36, 1.5, 6.5, 0.2, 1.5, 4.5, 0.3, "porzione", 100),

        // Grassi, frutta secca, semi
        f("evo", "Olio extravergine d'oliva", "Grassi", 884, 0, 0, 100, 0, 0, 0, "cucchiaio", 10),
        f("mandorle", "Mandorle", "Frutta secca", 580, 21, 22, 50, 12.5, 4.4, 0, "manciata", 30),
        f("noci", "Noci", "Frutta secca", 654, 15, 14, 65, 6.7, 2.6, 0, "manciata", 30),
        f("nocciole", "Nocciole", "Frutta secca", 628, 15, 17, 61, 9.7, 4.3, 0, "manciata", 30),
        f("arachidi", "Arachidi tostate", "Frutta secca", 590, 26, 16, 50, 8.5, 4, 0, "manciata", 30),
        f("burro_arachidi", "Burro di arachidi", "Frutta secca", 590, 25, 20, 50, 6, 9, 0.5, "cucchiaio", 15),
        f("anacardi", "Anacardi", "Frutta secca", 553, 18, 30, 44, 3.3, 6, 0, "manciata", 30),
        f("pistacchi", "Pistacchi", "Frutta secca", 560, 20, 28, 45, 10, 7.7, 0, "manciata", 30),
        f("chia", "Semi di chia", "Frutta secca", 486, 17, 42, 31, 34, 0, 0, "cucchiaio", 10),
        f("fondente", "Cioccolato fondente 70%", "Dolci", 600, 7.8, 46, 43, 11, 24, 0, "quadretto", 10),
        f("cioc_latte", "Cioccolato al latte", "Dolci", 535, 7.6, 59, 30, 3.4, 52, 0.2, "quadretto", 10),
        f("crema_nocciole", "Crema spalmabile nocciole e cacao", "Dolci", 540, 6.3, 57.5, 31, 3.4, 56, 0.1, "cucchiaino", 15),

        // Dolci e snack
        f("zucchero", "Zucchero", "Dolci", 400, 0, 100, 0, 0, 100, 0, "cucchiaino", 5),
        f("miele", "Miele", "Dolci", 304, 0.3, 82, 0, 0.2, 82, 0, "cucchiaino", 10),
        f("marmellata", "Marmellata", "Dolci", 250, 0.4, 62, 0.1, 1, 55, 0, "cucchiaino", 15),
        f("frollini", "Biscotti frollini", "Dolci", 470, 7, 70, 18, 2.5, 25, 0.6, "biscotto", 10),
        f("cornetto", "Cornetto semplice", "Dolci", 410, 8, 46, 21, 2, 12, 0.8, "cornetto", 50),
        f("gelato", "Gelato fiordilatte", "Dolci", 200, 4, 25, 10, 0, 22, 0.2, "coppetta", 100),
        f("torta", "Torta margherita", "Dolci", 370, 7, 55, 14, 1, 30, 0.4, "fetta", 80),
        f("chips", "Patatine in busta", "Snack", 536, 7, 53, 34, 4.4, 0.5, 1.5, "busta piccola", 25),

        // Bevande
        f("espresso", "Caffè espresso", "Bevande", 2, 0.1, 0.3, 0, 0, 0, 0, "tazzina", 30, true),
        f("cappuccino", "Cappuccino", "Bevande", 40, 2.2, 3.3, 2, 0, 3.3, 0.1, "tazza", 150, true),
        f("succo_arancia", "Succo d'arancia", "Bevande", 45, 0.7, 10.4, 0.2, 0.2, 8.4, 0, "bicchiere", 200, true),
        f("cola", "Bibita gassata zuccherata (cola)", "Bevande", 42, 0, 10.6, 0, 0, 10.6, 0, "lattina", 330, true),
        f("birra", "Birra chiara", "Bevande", 43, 0.5, 3.6, 0, 0, 0, 0, "bottiglia", 330, true),
        f("vino_rosso", "Vino rosso", "Bevande", 85, 0.1, 2.6, 0, 0, 0.6, 0, "bicchiere", 125, true),

        // Salse e condimenti
        f("pesto", "Pesto alla genovese", "Salse", 480, 5, 6, 48, 2, 1, 2.5, "cucchiaio", 15),
        f("maionese", "Maionese", "Salse", 680, 1, 1, 75, 0, 1, 1.2, "cucchiaio", 15),
        f("ketchup", "Ketchup", "Salse", 110, 1.2, 25, 0.2, 0.3, 22, 2.3, "cucchiaio", 15),
        f("ragu", "Ragù alla bolognese", "Salse", 130, 8, 5, 9, 1, 3, 1, "porzione", 80),
        f("hummus", "Hummus", "Salse", 230, 7, 14, 17, 6, 0.5, 1, "cucchiaio", 30),
        f("balsamico", "Aceto balsamico", "Salse", 88, 0.5, 17, 0, 0, 15, 0, "cucchiaio", 15),

        // Piatti pronti
        f("lasagne", "Lasagne alla bolognese", "Piatti pronti", 165, 8.5, 14, 8.5, 1, 2, 0.8, "porzione", 300),
        f("carbonara", "Spaghetti alla carbonara", "Piatti pronti", 210, 9, 23, 9, 1, 1, 0.8, "piatto", 300),
        f("risotto", "Risotto allo zafferano", "Piatti pronti", 140, 3, 22, 4.5, 0.5, 0.3, 0.6, "piatto", 300),
        f("sushi", "Sushi nigiri al salmone", "Piatti pronti", 150, 6, 24, 3, 0.4, 4, 0.8, "pezzo", 30),
        f("hamburger", "Hamburger (panino)", "Piatti pronti", 250, 13, 25, 11, 1.5, 5, 1.2, "panino", 220),
        f("kebab", "Kebab", "Piatti pronti", 215, 13, 18, 10, 1.5, 2, 1.3, "kebab", 350),
        f("insalata_riso", "Insalata di riso", "Piatti pronti", 170, 5, 25, 6, 1, 1.5, 0.8, "piatto", 250)
    )

    private val byId = foods.associateBy { it.id }
    fun get(id: String): Food = byId["base:$id"] ?: error("Alimento base mancante: $id")

    fun search(q: String, extra: List<Food>): List<Food> {
        val terms = q.trim().lowercase().split(" ").filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return (extra + foods).distinctBy { it.id }.filter { food ->
            val hay = (food.name + " " + food.brand + " " + food.category).lowercase()
            terms.all { hay.contains(it) }
        }.sortedBy { if (it.name.lowercase().startsWith(terms.first())) 0 else 1 }
    }

    /** Attività con valore MET (Compendium of Physical Activities, valori medi). */
    data class Activity(val name: String, val met: Double, val emoji: String)

    val activities = listOf(
        Activity("Camminata moderata", 3.5, "🚶"),
        Activity("Camminata veloce", 4.3, "🚶"),
        Activity("Passeggiata col cane", 3.0, "🐕"),
        Activity("Corsa 8 km/h", 8.3, "🏃"),
        Activity("Corsa 10 km/h", 9.8, "🏃"),
        Activity("Ciclismo moderato", 6.8, "🚴"),
        Activity("Cyclette / spinning", 8.5, "🚴"),
        Activity("Nuoto", 6.0, "🏊"),
        Activity("Pesi / palestra", 5.0, "🏋️"),
        Activity("Pesi intensi", 6.0, "🏋️"),
        Activity("Calisthenics", 5.0, "🤸"),
        Activity("HIIT / circuito", 8.0, "🔥"),
        Activity("Yoga", 2.5, "🧘"),
        Activity("Pilates", 3.0, "🧘"),
        Activity("Ellittica", 5.0, "⚙️"),
        Activity("Vogatore", 7.0, "🚣"),
        Activity("Calcio", 7.0, "⚽"),
        Activity("Calcetto", 8.0, "⚽"),
        Activity("Padel", 6.0, "🎾"),
        Activity("Tennis", 7.3, "🎾"),
        Activity("Basket", 6.5, "🏀"),
        Activity("Pallavolo", 4.0, "🏐"),
        Activity("Boxe (sacco)", 7.8, "🥊"),
        Activity("Escursionismo", 6.0, "🥾"),
        Activity("Ballo", 5.0, "💃"),
        Activity("Salire le scale", 8.0, "🪜"),
        Activity("Lavori domestici", 3.3, "🧹"),
        Activity("Giardinaggio", 3.8, "🌱")
    )

    /** Ricette originali di partenza; i valori nutrizionali sono calcolati dagli ingredienti. */
    val recipes: List<Recipe> by lazy {
        fun r(id: String, name: String, emoji: String, cat: String, min: Int, serv: Int,
              ing: List<Pair<String, Number>>, steps: List<String>) = Recipe(
            id = "base:$id", name = name, servings = serv, minutes = min, category = cat,
            emoji = emoji, builtin = true, steps = steps,
            ingredients = ing.map { Ingredient(get(it.first), it.second.toDouble()) }
        )
        listOf(
            r("overnight", "Overnight oats proteici", "🥣", "Colazione", 5, 1,
                listOf("avena" to 50, "greco0" to 150, "latte_ps" to 100, "mirtilli" to 80, "chia" to 10),
                listOf("Mescola avena, yogurt, latte e semi di chia in un barattolo.",
                    "Chiudi e lascia in frigo tutta la notte.",
                    "Al mattino aggiungi i mirtilli e mescola.")),
            r("pancake", "Pancake avena e banana", "🥞", "Colazione", 15, 1,
                listOf("avena" to 40, "banana" to 100, "uova" to 110, "miele" to 10),
                listOf("Frulla avena, banana e uova fino a ottenere una pastella liscia.",
                    "Cuoci a cucchiaiate in padella antiaderente calda, 2 minuti per lato.",
                    "Servi con un filo di miele.")),
            r("skyr_bowl", "Bowl di skyr, frutta e noci", "🍓", "Colazione", 3, 1,
                listOf("skyr" to 200, "fragole" to 100, "noci" to 15, "miele" to 5),
                listOf("Versa lo skyr in una ciotola.", "Aggiungi fragole a pezzi, noci tritate e miele.")),
            r("pollo_riso", "Pollo, riso basmati e zucchine", "🍗", "Pranzo", 25, 1,
                listOf("pollo_petto" to 150, "basmati" to 80, "zucchine" to 200, "evo" to 10),
                listOf("Cuoci il riso in acqua salata.",
                    "Taglia le zucchine a rondelle e saltale in padella con metà dell'olio.",
                    "Cuoci il pollo a fette alla piastra, 4 minuti per lato.",
                    "Unisci tutto e completa con l'olio rimasto a crudo.")),
            r("pasta_tonno", "Pasta tonno, pomodorini e rucola", "🍝", "Pranzo", 15, 1,
                listOf("pasta" to 90, "tonno_nat" to 80, "pomodori" to 120, "rucola" to 30, "evo" to 10),
                listOf("Cuoci la pasta al dente.",
                    "In padella scalda l'olio con i pomodorini tagliati per 3 minuti.",
                    "Aggiungi il tonno sgocciolato e la pasta, salta 1 minuto.",
                    "Completa con la rucola fresca fuori dal fuoco.")),
            r("buddha", "Buddha bowl di ceci e quinoa", "🥗", "Pranzo", 20, 1,
                listOf("quinoa" to 60, "ceci" to 120, "avocado" to 60, "carote" to 80, "lattuga" to 50, "hummus" to 30),
                listOf("Sciacqua e cuoci la quinoa per 12 minuti.",
                    "Componi la ciotola con lattuga, carote julienne, ceci e avocado.",
                    "Aggiungi la quinoa e condisci con l'hummus.")),
            r("salmone_patate", "Salmone al forno con patate", "🐟", "Cena", 35, 1,
                listOf("salmone" to 150, "patate" to 200, "evo" to 10, "lattuga" to 60),
                listOf("Taglia le patate a spicchi, condiscile con metà dell'olio e cuoci a 200 °C per 20 minuti.",
                    "Aggiungi il salmone e cuoci altri 12 minuti.",
                    "Servi con l'insalata condita con l'olio rimasto.")),
            r("frittata", "Frittata di albumi e spinaci", "🍳", "Cena", 15, 1,
                listOf("uova" to 55, "albume" to 150, "spinaci" to 150, "parmigiano" to 10, "pane_int" to 50),
                listOf("Sbatti uovo e albumi con il parmigiano.",
                    "Appassisci gli spinaci in padella, poi versa le uova.",
                    "Cuoci coperto a fuoco basso 6-8 minuti. Servi con il pane.")),
            r("manzo_verdure", "Straccetti di manzo e verdure", "🥩", "Cena", 20, 1,
                listOf("manzo_magro" to 150, "peperoni" to 150, "cipolla" to 50, "evo" to 10, "riso" to 60),
                listOf("Cuoci il riso.",
                    "Salta cipolla e peperoni a listarelle con l'olio per 8 minuti.",
                    "Aggiungi la carne a straccetti e cuoci 3 minuti a fuoco vivo.",
                    "Servi sul riso.")),
            r("snack_greco", "Snack yogurt greco e frutta secca", "🥜", "Spuntino", 2, 1,
                listOf("greco0" to 170, "mandorle" to 20, "miele" to 5),
                listOf("Unisci yogurt, mandorle e miele.")),
            r("toast", "Toast prosciutto cotto e formaggio", "🥪", "Spuntino", 8, 1,
                listOf("pane_int" to 60, "cotto" to 40, "mozzarella_light" to 40),
                listOf("Farcisci il pane con prosciutto e mozzarella.",
                    "Tosta in piastra 3-4 minuti finché il formaggio fila."))
        )
    }
}
