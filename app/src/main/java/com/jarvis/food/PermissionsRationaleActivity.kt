package com.jarvis.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.food.ui.AppBackground
import com.jarvis.food.ui.JarvisTheme
import com.jarvis.food.ui.PrimaryButton
import com.jarvis.food.ui.T

/** Informativa mostrata da Health Connect: quali dati legge Jarvis Food e perché. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                AppBackground {
                    Column(Modifier.statusBarsPadding().padding(20.dp)) {
                        Text("Dati di salute usati da Jarvis Food", style = T.Title)
                        Spacer(Modifier.height(12.dp))
                        Text("Jarvis Food legge da Health Connect passi, calorie bruciate, allenamenti, frequenza cardiaca, sonno e peso " +
                                "per adattare il tuo fabbisogno calorico all'attività del giorno. I dati restano sul telefono, " +
                                "non vengono inviati a server e non vengono scritti in Health Connect.", style = T.Read)
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton("Ho capito") { finish() }
                    }
                }
            }
        }
    }
}
