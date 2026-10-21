package com.jarvis.food.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jarvis.food.MainActivity
import com.jarvis.food.R
import com.jarvis.food.data.Store
import java.time.LocalDateTime
import java.time.ZoneId

/** Promemoria locali: pasti, acqua, pesata e fine del digiuno. Nessun server. */
object Reminders {
    private const val CHANNEL = "promemoria"
    const val EXTRA_KIND = "kind"

    // Codici richiesta
    private const val BREAKFAST = 1
    private const val LUNCH = 2
    private const val DINNER = 3
    private const val WATER = 4
    private const val WEIGH = 5
    private const val FAST_END = 6

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Promemoria", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Pasti, acqua, pesata e digiuno"
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun pending(ctx: Context, code: Int): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_KIND, code)
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun schedule(ctx: Context, code: Int, atMillis: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, code)
        val exactOk = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exactOk) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }

    private fun cancel(ctx: Context, code: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, code))
    }

    /** Prossima occorrenza di un orario (minuti dalla mezzanotte). */
    private fun nextAt(minutes: Int): Long {
        val now = LocalDateTime.now()
        var t = now.withHour(minutes / 60).withMinute(minutes % 60).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Prossimo promemoria acqua: ogni 2 ore tra le 9 e le 21. */
    private fun nextWater(): Long {
        val now = LocalDateTime.now()
        for (h in 9..21 step 2) {
            val t = now.withHour(h).withMinute(0).withSecond(0).withNano(0)
            if (t.isAfter(now)) return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun rescheduleAll(ctx: Context) {
        val s = Store.current
        val r = s.reminders
        if (r.meals) {
            schedule(ctx, BREAKFAST, nextAt(r.breakfast))
            schedule(ctx, LUNCH, nextAt(r.lunch))
            schedule(ctx, DINNER, nextAt(r.dinner))
        } else listOf(BREAKFAST, LUNCH, DINNER).forEach { cancel(ctx, it) }
        if (r.water) schedule(ctx, WATER, nextWater()) else cancel(ctx, WATER)
        if (r.weighIn) schedule(ctx, WEIGH, nextAt(r.weighInTime)) else cancel(ctx, WEIGH)
        val start = s.fasting.activeStart
        if (r.fasting && start != null) {
            val end = start + s.fasting.targetHours * 3_600_000L
            if (end > System.currentTimeMillis()) schedule(ctx, FAST_END, end) else cancel(ctx, FAST_END)
        } else cancel(ctx, FAST_END)
    }

    fun handle(ctx: Context, kind: Int) {
        val s = Store.current
        val (title, text) = when (kind) {
            BREAKFAST -> "Colazione" to "Ricordati di registrare la colazione ☀️"
            LUNCH -> "Pranzo" to "Cosa hai mangiato a pranzo? Registralo in un tocco 🍝"
            DINNER -> "Cena" to "Registra la cena per chiudere la giornata 🌙"
            WATER -> {
                val today = java.time.LocalDate.now().toString()
                val ml = s.water[today] ?: 0
                "Acqua" to "Hai bevuto ${ml} ml su ${s.goals.waterMl} ml. Un bicchiere adesso? 💧"
            }
            WEIGH -> "Pesata" to "Buongiorno! Registra il peso di oggi ⚖️"
            FAST_END -> "Digiuno completato 🎉" to "Hai raggiunto l'obiettivo di ${s.fasting.targetHours} ore."
            else -> return
        }
        notify(ctx, kind, title, text)
        // I promemoria giornalieri si riprogrammano da soli.
        rescheduleAll(ctx)
    }

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 100 + id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context.applicationContext)
        Reminders.createChannel(context)
        Reminders.handle(context, intent.getIntExtra(Reminders.EXTRA_KIND, 0))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context.applicationContext)
        Reminders.rescheduleAll(context)
    }
}
