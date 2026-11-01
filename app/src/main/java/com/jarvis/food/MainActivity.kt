package com.jarvis.food

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.food.data.Store
import com.jarvis.food.health.HealthSync
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.jarvis.food.notify.Reminders
import com.jarvis.food.ui.App
import com.jarvis.food.ui.JarvisTheme
import com.jarvis.food.ui.Nav
import java.time.ZonedDateTime
import java.time.Duration

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    Nav.refreshToday()
                    val now = ZonedDateTime.now()
                    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                    delay(Duration.between(now, midnight).toMillis().coerceIn(1_000L, 60_000L))
                }
            }
        }
        Reminders.createChannel(this)
        Reminders.rescheduleAll(this)
        if (Store.current.energy.watchConnected) HealthSync.schedule(this)
        if (Build.VERSION.SDK_INT >= 33 && Store.current.onboarded) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { JarvisTheme { App() } }
    }

    override fun onResume() {
        super.onResume()
        // Advance only selections that were following today; preserve intentional history.
        Nav.refreshToday()
        // Smartwatch: aggiorna i dati di Health Connect a ogni apertura (al massimo ogni 5 minuti).
        val e = Store.current.energy
        if (e.watchConnected && System.currentTimeMillis() - e.lastSync > 5 * 60_000) {
            lifecycleScope.launch { runCatching { HealthSync.sync(this@MainActivity, 3) } }
        }
    }

    override fun onPause() {
        super.onPause()
        Store.saveNow()
    }
}
