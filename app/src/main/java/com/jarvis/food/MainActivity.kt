package com.jarvis.food

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.food.data.Store
import com.jarvis.food.notify.Reminders
import com.jarvis.food.ui.App
import com.jarvis.food.ui.JarvisTheme
import com.jarvis.food.ui.Nav
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        Reminders.createChannel(this)
        Reminders.rescheduleAll(this)
        if (Build.VERSION.SDK_INT >= 33 && Store.current.onboarded) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { JarvisTheme { App() } }
    }

    override fun onResume() {
        super.onResume()
        // Se l'app resta aperta oltre mezzanotte, il diario torna su "oggi".
        if (Nav.stack.isEmpty() && Nav.diaryDate == LocalDate.now().minusDays(1)) Nav.diaryDate = LocalDate.now()
    }

    override fun onPause() {
        super.onPause()
        Store.saveNow()
    }
}
