package com.jarvis.food.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Store
import com.jarvis.food.health.HealthSync
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun WatchScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(HealthSync.status(ctx)) }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun sync() {
        busy = true
        scope.launch {
            msg = runCatching { HealthSync.sync(ctx) }.getOrElse { "Errore: ${it.message}" }
            granted = runCatching { HealthSync.granted(ctx) }.getOrDefault(emptySet())
            busy = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(HealthSync.permissionContract()) { res ->
        granted = res
        if (res.any { it in HealthSync.PERMISSIONS }) {
            Store.setEnergy(Store.current.energy.copy(mode = 2, watchConnected = true))
            HealthSync.schedule(ctx)
            sync()
        } else msg = "Permessi non concessi: puoi riprovare quando vuoi."
    }
    LaunchedEffect(Unit) {
        status = HealthSync.status(ctx)
        granted = runCatching { HealthSync.granted(ctx) }.getOrDefault(emptySet())
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Smartwatch e attività", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JCard {
                Text("⌚ Amazfit Balance", style = T.Title2)
                Text("Il watch sincronizza i dati con l'app Zepp, che li scrive in Health Connect; Jarvis Food li legge da lì: " +
                        "passi, calorie attive, allenamenti, frequenza cardiaca, sonno e peso.", style = T.Read.copy(fontSize = T.Body.fontSize))
                Spacer(Modifier.height(10.dp))
                Text("Una volta sola, nell'app Zepp:", style = T.Label)
                Text("Profilo → Collegamento account di terze parti (3rd Party Account Linking) → Health Connect → consenti tutte le categorie.", style = T.Body)
                Spacer(Modifier.height(8.dp))
                GhostButton("Apri Zepp") {
                    val i = ctx.packageManager.getLaunchIntentForPackage("com.huami.watch.hmwatchmanager")
                    runCatching { ctx.startActivity(i ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.huami.watch.hmwatchmanager"))) }
                }
            }
            JCard {
                Text("Health Connect", style = T.Title2)
                when (status) {
                    0 -> {
                        val ok = granted.any { it in HealthSync.PERMISSIONS }
                        Text(if (ok) "Collegato · ${granted.count { it in HealthSync.PERMISSIONS }} tipi di dati autorizzati" else "Da autorizzare", style = T.Muted)
                        if (ok && HealthSync.BACKGROUND !in granted) Text("Lettura in background non autorizzata: i dati si aggiornano quando apri l'app.", style = T.Small)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(if (ok) "Permessi" else "Collega", color = C.Accent) { permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND) }
                            if (ok) GhostButton("Sincronizza ora") { sync() }
                        }
                    }
                    1 -> {
                        Text("Health Connect va installato o aggiornato dal Play Store.", style = T.Muted)
                        GhostButton("Apri Play Store") {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }
                        }
                    }
                    else -> Text("Health Connect non è disponibile su questo dispositivo.", style = T.Muted)
                }
                if (busy) CircularProgressIndicator(Modifier.padding(top = 8.dp), color = C.Accent, strokeWidth = 2.dp)
                msg?.let { Text(it, style = T.Small, modifier = Modifier.padding(top = 8.dp)) }
                if (s.energy.lastSync > 0) Text("Ultima sincronizzazione: ${fmtDayTime(s.energy.lastSync)}", style = T.Small)
            }

            val today = s.health[LocalDate.now().toString()]
            if (today != null) JCard {
                Text("Oggi dal watch", style = T.Title2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Passi", today.steps.thousands(), Modifier.weight(1f))
                    StatTile("Kcal attive", today.activeKcal?.r0() ?: "—", Modifier.weight(1f))
                    StatTile("Allenamenti", today.sessions.size.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("FC a riposo", today.restingHr?.r0() ?: "—", Modifier.weight(1f))
                    StatTile("Sonno", today.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", Modifier.weight(1f))
                    StatTile("Kcal totali", today.totalKcal?.r0() ?: "—", Modifier.weight(1f))
                }
                if (today.activeKcal == null) Text("Il watch non ha condiviso le calorie attive: le stimo da passi e allenamenti.", style = T.Small, modifier = Modifier.padding(top = 8.dp))
            }

            SectionTitle("Come conteggio l'attività")
            Choice("Livello di attività fisso (dal profilo)", s.energy.mode == 0) { Store.setEnergy(s.energy.copy(mode = 0)) }
            Choice("Sedentario + attività inserite a mano", s.energy.mode == 1) { Store.setEnergy(s.energy.copy(mode = 1)) }
            Choice("Smartwatch: si adatta a ciò che fai ogni giorno", s.energy.mode == 2) {
                if (granted.any { it in HealthSync.PERMISSIONS }) Store.setEnergy(s.energy.copy(mode = 2, watchConnected = true))
                else permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
            }
            if (s.energy.mode != 0) JCard {
                Text("Calorie attive conteggiate: ${s.energy.eatBackPct}%", style = T.Label)
                Slider(s.energy.eatBackPct.toFloat(), { Store.setEnergy(Store.current.energy.copy(eatBackPct = (it / 5).toInt() * 5)) }, valueRange = 50f..100f,
                    colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceAlt))
                Text("I dispositivi da polso tendono a sovrastimare le calorie dell'attività: conteggiarne una parte è una scelta prudente. " +
                        "Se il peso scende più del previsto aumenta la percentuale, se sale diminuiscila.", style = T.Small)
            }
            SwitchRow("Importa il peso da Health Connect", "Ultima pesata degli ultimi 30 giorni", s.energy.importWeight) {
                Store.setEnergy(s.energy.copy(importWeight = it))
            }
            GhostButton("Come calcolo l'obiettivo di oggi", Modifier.fillMaxWidth()) { Nav.go(Screen.EnergyInfo) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Suppress("unused") private val align = Alignment.Center
