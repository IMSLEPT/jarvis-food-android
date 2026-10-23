package com.jarvis.food.appearance

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.food.ui.*

/** Preference is separate from the nutrition archive: changing style never migrates or resets data. */
object Appearance {
    private var context: Context? = null
    var classic by mutableStateOf(false)
        private set
    var settingsOpen by mutableStateOf(false)
    fun init(ctx: Context) {
        context = ctx.applicationContext
        classic = ctx.getSharedPreferences("jarvis_appearance", Context.MODE_PRIVATE).getBoolean("classic", false)
    }
    fun selectClassic(value: Boolean) {
        if (classic == value) return
        context?.getSharedPreferences("jarvis_appearance", Context.MODE_PRIVATE)?.edit()?.putBoolean("classic", value)?.apply()
        classic = value
    }
}

@Composable
fun AppearanceRoot() {
    if (Appearance.classic) {
        com.jarvis.food.classicui.JarvisTheme {
            if (Appearance.settingsOpen) AppearanceScreen() else com.jarvis.food.classicui.App()
        }
    } else JarvisTheme { if (Appearance.settingsOpen) AppearanceScreen() else App() }
}

@Composable
fun AppearanceEntry() {
    JCard(Modifier.padding(top = 14.dp), onClick = { Appearance.settingsOpen = true }) {
        Text("Aspetto", style = T.Title2)
        Text(if (Appearance.classic) "Aurora precedente · cambia quando vuoi" else "Aurora · grafica aggiornata", style = T.Small)
    }
}

@Composable
fun AppearanceScreen() {
    BackHandler { Appearance.settingsOpen = false }
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("Aspetto", { Appearance.settingsOpen = false })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Scegli la tua Aurora", style = T.H2)
                Text("Puoi cambiare grafica in qualsiasi momento. Diario, piani, ricette e collegamenti restano gli stessi.", style = T.Body)
                JCard(onClick = { Appearance.selectClassic(false) }) {
                    Text("Aurora", style = T.Title2)
                    Text("La nuova grafica: più ordine, colori e spazio ai tuoi contenuti.", style = T.Muted)
                    if (!Appearance.classic) Text("Selezionata", style = T.Link)
                }
                JCard(onClick = { Appearance.selectClassic(true) }) {
                    Text("Aurora precedente", style = T.Title2)
                    Text("Ripristina l’interfaccia che usavi prima del redesign.", style = T.Muted)
                    if (Appearance.classic) Text("Selezionata", style = T.Link)
                }
                PrimaryButton("Fatto", Modifier.fillMaxWidth()) { Appearance.settingsOpen = false }
            }
        }
    }
}
