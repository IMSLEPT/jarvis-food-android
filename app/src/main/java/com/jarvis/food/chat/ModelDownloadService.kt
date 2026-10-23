package com.jarvis.food.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.food.MainActivity
import com.jarvis.food.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servizio in primo piano che tiene vivo il download dei modelli AI anche con l'app chiusa, in un'altra schermata
 * o con lo schermo bloccato. Il download vero e proprio lo fa [ModelManager] (che riprende da dove era arrivato);
 * il servizio impedisce che il sistema fermi l'app, mostra il progresso nella notifica e si spegne a lavoro finito.
 */
class ModelDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcher: Job? = null
    private var wake: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            ModelManager.cancel(); clearPending(this)
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            return START_NOT_STICKY
        }
        val idx = intent?.getIntExtra(EXTRA_MODEL, -1)?.takeIf { it >= 0 } ?: pendingModel(this)
        if (idx == null) { stopSelf(); return START_NOT_STICKY }
        val model = LocalModel.of(idx)
        startFg(notification(model, 0f, "Preparo il download…"))
        acquireWake()
        // idempotente: se il download è già in corso (avviato dall'app) non ne parte un secondo
        ModelManager.start(applicationContext, model)
        watcher?.cancel()
        watcher = scope.launch {
            var last = -1
            while (true) {
                val cur = ModelManager.downloading
                if (cur == null) break
                val got = ModelManager.downloaded
                val pct = (got * 100 / cur.bytes).toInt()
                if (pct != last) {
                    last = pct
                    notify(notification(cur, got.toFloat() / cur.bytes, "${gb(got)} di ${gb(cur.bytes)} · $pct%"))
                }
                delay(1500)
            }
            val ready = ModelManager.isReady(applicationContext, model)
            clearPending(applicationContext)
            if (ready) finishNote("${model.title} è pronto", "Puoi usare Jarvis sul telefono, anche senza internet.")
            else if (ModelManager.error != null) finishNote("Download di ${model.title} interrotto", (ModelManager.error ?: "") + " Riapri l'app per riprendere.")
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        watcher?.cancel(); releaseWake()
        super.onDestroy()
    }

    private fun gb(b: Long) = String.format(java.util.Locale.ITALY, "%.1f GB", b / 1_000_000_000.0)

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Download modelli AI", NotificationManager.IMPORTANCE_LOW))
    }

    private fun open(): PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun notification(m: LocalModel, progress: Float, text: String): Notification {
        ensureChannel()
        val cancel = PendingIntent.getService(this, 1, Intent(this, ModelDownloadService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Scarico ${m.title}").setContentText(text).setContentIntent(open())
            .setProgress(1000, (progress * 1000).toInt(), false).setOngoing(true).setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Annulla", cancel).build()
    }

    private fun startFg(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NOTE_ID, n)
    }

    private fun notify(n: Notification) = getSystemService(NotificationManager::class.java).notify(NOTE_ID, n)

    private fun finishNote(title: String, text: String) {
        ensureChannel()
        val n = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open()).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(NOTE_ID + 1, n)
    }

    private fun acquireWake() {
        if (wake?.isHeld == true) return
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JarvisFood:modelDownload")
            .apply { setReferenceCounted(false); acquire(4 * 60 * 60 * 1000L) }
    }

    private fun releaseWake() { runCatching { if (wake?.isHeld == true) wake?.release() }; wake = null }

    companion object {
        private const val CHANNEL = "model_download"
        private const val NOTE_ID = 4711
        private const val ACTION_CANCEL = "com.jarvis.food.CANCEL_MODEL_DOWNLOAD"
        private const val EXTRA_MODEL = "model"
        private const val PREFS = "model_download"

        private fun pendingModel(ctx: Context): Int? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("model", -1).takeIf { it >= 0 }
        private fun clearPending(ctx: Context) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("model").apply() }

        /** Avvia (o riprende) il download in primo piano. */
        fun start(ctx: Context, m: LocalModel) {
            val app = ctx.applicationContext
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("model", m.ordinal).apply()
            ContextCompat.startForegroundService(app, Intent(app, ModelDownloadService::class.java).putExtra(EXTRA_MODEL, m.ordinal))
        }

        fun cancel(ctx: Context) {
            val app = ctx.applicationContext
            clearPending(app)
            ModelManager.cancel()
            app.stopService(Intent(app, ModelDownloadService::class.java))
        }
    }
}
