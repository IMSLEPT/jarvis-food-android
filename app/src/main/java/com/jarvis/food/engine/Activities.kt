package com.jarvis.food.engine

/** Attività con valori MET medi (Compendium of Physical Activities), per l'inserimento manuale. */
object Activities {
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

}
