package com.jarvis.food.data

import java.time.LocalDate

data class BodyMeasureGuide(val key: String, val name: String, val landmark: String, val steps: List<String>, val unit: String = "cm")

object BodyMeasurements {
    val guides = listOf(
        BodyMeasureGuide("vita", "Vita", "A metà tra l'ultima costola palpabile e la parte superiore dell'osso del bacino.", listOf(
            "In piedi, addome rilassato e piedi vicini. Individua i due riferimenti sui fianchi.",
            "Fai passare il metro orizzontale nel punto intermedio, a contatto con la pelle senza stringere.",
            "Leggi al termine di un'espirazione normale, senza trattenere o tirare dentro la pancia.")),
        BodyMeasureGuide("fianchi", "Fianchi", "Intorno al punto più sporgente dei glutei.", listOf(
            "Resta in piedi con i piedi uniti.", "Fai scorrere il metro fino alla circonferenza massima dei glutei.",
            "Controlla di lato che sia orizzontale e non comprima la pelle.")),
        BodyMeasureGuide("petto", "Petto", "Intorno al torace all'altezza della linea dei capezzoli: riferimento pratico dell'app.", listOf(
            "In piedi e con le braccia rilassate lungo i fianchi.", "Passa il metro attorno al torace, orizzontale anche sulla schiena.",
            "Leggi dopo un'espirazione normale. Mantieni lo stesso riferimento e le stesse condizioni a ogni controllo.")),
        BodyMeasureGuide("braccio", "Braccio", "Braccio destro: punto intermedio fra la sporgenza della spalla e la punta del gomito.", listOf(
            "Piega il gomito per individuare e segnare il punto intermedio.", "Lascia poi il braccio rilassato e disteso lungo il fianco.",
            "Circonda il punto segnato con il metro perpendicolare al braccio, senza stringere. Non contrarre il bicipite.")),
        BodyMeasureGuide("coscia", "Coscia", "Coscia destra: a metà tra la piega inguinale e il margine superiore della rotula, riferimento dell'app.", listOf(
            "In piedi, distribuisci il peso su entrambe le gambe e rilassa la coscia.", "Individua e segna il punto intermedio sulla parte anteriore.",
            "Passa il metro orizzontale attorno alla coscia. Usa sempre lo stesso lato e lo stesso punto.")),
        BodyMeasureGuide("collo", "Collo", "Sotto la prominenza della laringe, senza premere sulla gola: riferimento dell'app.", listOf(
            "Tieni lo sguardo diritto e le spalle rilassate.", "Avvolgi delicatamente il metro attorno al collo, senza stringere.",
            "Usa lo stesso livello a ogni controllo. Non comprimere i tessuti.")),
        BodyMeasureGuide("grasso", "Massa grassa", "Inserisci la percentuale ottenuta con lo stesso metodo di valutazione.", listOf(
            "Usa il dato della bilancia impedenziometrica o della valutazione del professionista.",
            "Ripeti il controllo in condizioni simili; metodi diversi possono dare risultati diversi.",
            "La sagoma non stima la massa grassa e qui non serve il metro."), "%")
    )
    fun guide(key: String) = guides.first { it.key == key }
    fun valid(key: String, value: Double): Boolean = value.isFinite() && when(key) {
        "grasso" -> value in 1.0..75.0
        "vita", "fianchi", "petto" -> value in 20.0..300.0
        "braccio", "coscia", "collo" -> value in 5.0..200.0
        else -> false
    }
    fun save(history: List<Measurement>, date: LocalDate, key: String, value: Double): List<Measurement> {
        require(valid(key, value)) { "Misura non valida" }
        return (history.filterNot { it.date == date.toString() && it.type == key } + Measurement(date.toString(), key, value))
            .sortedWith(compareBy<Measurement> { it.date }.thenBy { it.type })
    }
}
