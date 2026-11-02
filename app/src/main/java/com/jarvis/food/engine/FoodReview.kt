package com.jarvis.food.engine

import com.jarvis.food.data.Food

/** Transparent label summary. No invented /100 score, GI, additive toxicity or pesticide claims. */
object FoodReview {
    const val CLAIMS_SOURCE = "https://food.ec.europa.eu/food-safety/labelling-and-nutrition/nutrition-and-health-claims/nutrition-claims_en"
    const val NUTRISCORE_SOURCE = "https://www.santepubliquefrance.fr/determinants-de-sante/nutrition-et-activite-physique/articles/nutri-score"
    enum class Tone { GOOD, CAUTION, LIMIT, UNKNOWN }
    data class Point(val title: String, val value: String, val tone: Tone)
    data class Review(val title: String, val tone: Tone, val grade: String?, val positive: List<Point>, val attention: List<Point>, val facts: List<Point>, val missing: List<String>)

    fun assess(food: Food): Review {
        val signals = FoodGuidance.signals(food)
        val positive = mutableListOf<Point>()
        val attention = mutableListOf<Point>()
        val facts = mutableListOf<Point>()
        val missing = mutableListOf<String>()
        fun value(v: Double) = String.format(java.util.Locale.ITALY,"%.1f",v)
        val unit = "100 ${if(food.liquid) "ml" else "g"}"
        signals.forEach { signal ->
            val v=signal.per100
            when(signal.level) {
                FoodGuidance.Level.LOW -> positive += Point(signal.label, "${value(v!!)} g/$unit · basso", Tone.GOOD)
                FoodGuidance.Level.HIGH -> attention += Point(signal.label, "${value(v!!)} g/$unit · elevato", Tone.LIMIT)
                FoodGuidance.Level.MEDIUM -> facts += Point(signal.label,"${value(v!!)} g/$unit · medio",Tone.CAUTION)
                FoodGuidance.Level.UNKNOWN -> if(v==null) missing += signal.label else facts += Point(signal.label,"${value(v)} g/$unit · senza colore",Tone.UNKNOWN)
            }
        }
        val protein = NutritionScience.per100(food,"protein")
        val kcal = NutritionScience.per100(food,"kcal")
        if(protein!=null && kcal!=null && kcal>0) {
            val fraction=protein*4/kcal
            if(fraction in .12..1.0) positive.add(0,Point(if(fraction>=.20) "Ricco di proteine" else "Fonte di proteine","${value(protein)} g/$unit",Tone.GOOD))
        }
        val fiber=NutritionScience.per100(food,"fiber")
        if(fiber!=null) {
            val per100Kcal = if(kcal!=null && kcal>0) fiber*100/kcal else 0.0
            val highFiber = (!food.liquid && fiber>=6) || per100Kcal>=3
            if(highFiber || (!food.liquid && fiber>=3) || per100Kcal>=1.5) positive.add(0,Point(if(highFiber) "Ricco di fibre" else "Fonte di fibre","${value(fiber)} g/$unit",Tone.GOOD))
        }
        if(fiber==null) missing += "Fibre"
        val n=FoodDb.normalize(food.name)
        val pureNuts=food.name.contains("100%") && listOf("mandorl","arachid","nocciol","pistacch","anacard","noci").any { it in n } &&
            (NutritionScience.per100(food,"sugar") ?: 0.0)<=15 && (NutritionScience.per100(food,"salt") ?: 0.0)<=.3 && (NutritionScience.per100(food,"addedSugar") ?: 0.0)==0.0
        if(pureNuts || food.group in setOf("frutta_secca","semi","olio")) facts += Point("Energia concentrata","Conta la porzione, non solo i valori per 100 g",Tone.CAUTION)
        if(food.group=="salumi") attention += Point("Carne lavorata","Da limitare e alternare con altre fonti",Tone.LIMIT)
        food.productFacts?.let { p ->
            p.additivesCount?.let { count -> facts += Point("Additivi",if(count==0) "Nessuno dichiarato negli ingredienti disponibili" else "$count dichiarati · il numero non misura il rischio",Tone.UNKNOWN) }
            if(p.organic==true) facts += Point("Biologico","Etichetta bio dichiarata · non è un voto nutrizionale",Tone.UNKNOWN)
            p.nova?.let { nova -> facts += Point("Lavorazione NOVA","Gruppo $nova/4 · dato Open Food Facts",Tone.UNKNOWN) }
        }
        val grade=food.productFacts?.nutriScore?.uppercase()?.takeIf { it in listOf("A","B","C","D","E") }
        val high=signals.count { it.level==FoodGuidance.Level.HIGH }
        val tone=when {
            grade in listOf("A","B") -> Tone.GOOD
            grade=="C" -> Tone.CAUTION
            grade in listOf("D","E") -> Tone.LIMIT
            food.group=="salumi" -> Tone.LIMIT
            pureNuts -> Tone.CAUTION
            high>=2 -> Tone.LIMIT
            high==1 -> Tone.CAUTION
            signals.any { it.level==FoodGuidance.Level.UNKNOWN } -> Tone.UNKNOWN
            else -> Tone.GOOD
        }
        val title=when {
            grade!=null -> "Nutri-Score $grade"
            pureNuts -> "Conta la porzione"
            tone==Tone.LIMIT -> "Da limitare nei parametri evidenziati"
            tone==Tone.CAUTION -> "Da dosare"
            tone==Tone.UNKNOWN -> "Dati incompleti"
            else -> "Parametri favorevoli"
        }
        return Review(title,tone,grade,positive,attention,facts,missing.distinct())
    }
}
