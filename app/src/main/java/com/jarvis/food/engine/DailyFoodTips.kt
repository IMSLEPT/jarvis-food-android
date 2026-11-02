package com.jarvis.food.engine

import com.jarvis.food.data.*
import java.time.LocalDate

object DailyFoodTips {
    data class Tip(val id:String,val title:String,val text:String,val food:String,val group:String,
        val source:String="variety",val personal:Boolean=false,val attention:Boolean=false,val foodIds:List<String> = emptyList())
    private val general=listOf(
        Tip("vegetables","Un colore in più","Aggiungi una verdura diversa a pranzo o cena.","broccoli","verdura",foodIds=listOf("broccoli")),
        Tip("wholegrain","Cambia cereale","Prova il riso integrale al posto di quello bianco.","riso integrale","cereali_integrali",foodIds=listOf("riso_int")),
        Tip("water","Acqua a portata","Tieni una bottiglia vicina e bevi durante la giornata.","acqua","bevande"),
        Tip("legumes","Varia le proteine","Inserisci ceci nel prossimo pasto, adattando la porzione.","ceci","legumi",foodIds=listOf("ceci")),
        Tip("fruit","Uno spuntino semplice","Scegli un frutto intero tra i pasti, se ti va.","mela","frutta",foodIds=listOf("mela")),
        Tip("salt","Prima di salare","Assaggia prima: usa limone ed erbe per dare sapore.","limone","frutta",source="sodium"),
        Tip("nuts","Una piccola porzione","Frutta a guscio al naturale: riferimento 30 g, da adattare.","noci","frutta_secca",source="portions",foodIds=listOf("noci")),
        Tip("plan","Prepara il prossimo pasto","Scegli il pasto nel Piano prima di avere fretta.","piatto",""),
        Tip("variety","Una scelta diversa","Cambia una fonte proteica rispetto a ieri, nei tuoi vincoli.","piatto",""),
        Tip("label","Confronta l’etichetta","Tra prodotti simili confronta sale, zuccheri e saturi.","yogurt",""),
        Tip("quantity","Dai spazio alle quantità","Controlla la porzione scelta oltre ai valori per 100 g.","piatto",""),
        Tip("simple","Cucina semplice","Scegli una preparazione semplice senza bruciare gli alimenti.","piatto",""),
        Tip("breakfast","Cambia colazione","Prova avena e yogurt bianco nelle quantità del tuo piano.","avena","cereali_integrali",foodIds=listOf("avena","greco0")),
        Tip("olive","Scegli il condimento","Prova olio d’oliva al posto del burro, contando la quantità.","olio oliva","olio",foodIds=listOf("olio")),
        Tip("slow","Prenditi una pausa","Mangia con calma e presta attenzione a fame e sazietà.","piatto",""),
        Tip("fresh","Varia anche i contorni","Cambia verdura rispetto a ieri: fresca o surgelata al naturale.","carote","verdura",foodIds=listOf("carote"))
    )
    fun build(s:AppState,date:LocalDate,db:FoodDb,review:NutritionReview?=null):List<Tip> {
        val style=s.plan.firstOrNull { it.date==date.toString() }?.styleId ?: s.dietPrefs.styleId
        val pool=general.filter { tip -> tip.foodIds.all { id ->
            val f=db.food(id) ?: return@all false
            f.tags.none { it in s.dietPrefs.exclude } && DietStyles.allowed(db,RecipeTemplate("tip:$style:$id",f.name,"spuntino",0,listOf(TemplateIngredient(id,100.0,"v")),emptyList(),cuisine=if(style=="japanese") "giapponese" else "italiana"),style)
        } }
        val notices=review?.let { NutritionScience.actionable(it) }.orEmpty().take(2).map { n ->
            val item=when(n.id) {
                "free-sugar" -> Triple("Meno zuccheri liberi","Prova una colazione senza aggiunte dolci, nelle tue quantità.","piatto")
                "sodium" -> Triple("Riduci il sale","Confronta le etichette e scegli una variante meno salata.","piatto")
                "saturated" -> Triple("Varia i grassi","Sostituisci parte dei saturi con fonti di grassi insaturi.","olio oliva")
                "fiber" -> Triple("Più fibre","Aumenta gradualmente le fonti di fibre compatibili con la tua dieta.","verdura")
                "intake-vitD" -> Triple("Varia le fonti di vitamina D","Il diario indica un apporto basso: non dimostra una carenza.","piatto")
                "intake-vitB12" -> Triple("Controlla le fonti di B12","Scegli fonti adatte alla tua dieta; per la vegana serve una fonte affidabile.","piatto")
                else -> Triple(n.title,"Varia le fonti compatibili con il tuo piano e le tue esclusioni.","piatto")
            }
            Tip("personal:${n.id}",item.first,item.second,item.third,"",n.sourceId,true,n.attention)
        }
        val start=Math.floorMod(date.toEpochDay()*3,pool.size.toLong()).toInt()
        return notices + (pool.drop(start)+pool.take(start)).take(3-notices.size)
    }
}
