package com.jarvis.food.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.Store
import com.jarvis.food.data.WorkoutSession
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Smartwatch tramite Health Connect. L'app Zepp (Amazfit Balance) scrive in Health Connect passi, allenamenti,
 * frequenza cardiaca, sonno e, se abilitati, calorie e peso; Jarvis Food li legge da lì.
 */
object HealthSync {
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )
    const val BACKGROUND = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    /** 0 disponibile, 1 da installare/aggiornare, 2 non supportato. */
    fun status(context: Context): Int = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> 0
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> 1
        else -> 2
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun granted(context: Context): Set<String> =
        if (status(context) != 0) emptySet() else HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()

    /** Legge gli ultimi [days] giorni e aggiorna lo stato dell'app. Restituisce un messaggio di esito. */
    suspend fun sync(context: Context, days: Int = 7): String {
        if (status(context) != 0) return "Health Connect non disponibile su questo telefono."
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (granted.none { it in PERMISSIONS }) return "Permessi Health Connect non concessi."
        val zone = ZoneId.systemDefault()
        val result = HashMap<String, HealthDay>()
        for (i in 0 until days) {
            val date = LocalDate.now().minusDays(i.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), java.time.Instant.now())
            val range = TimeRangeFilter.between(start, end)
            val metrics = buildSet {
                if (HealthPermission.getReadPermission(StepsRecord::class) in granted) add(StepsRecord.COUNT_TOTAL)
                if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (HealthPermission.getReadPermission(RestingHeartRateRecord::class) in granted) add(RestingHeartRateRecord.BPM_AVG)
                if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) add(HeartRateRecord.BPM_AVG)
            }
            val agg = if (metrics.isEmpty()) null else runCatching { client.aggregate(AggregateRequest(metrics, range)) }.getOrNull()
            val sessions = if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) runCatching {
                client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records.map { r ->
                    val kcal = runCatching {
                        client.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            TimeRangeFilter.between(r.startTime, r.endTime)))[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                    }.getOrNull()
                    WorkoutSession(r.title ?: exerciseName(r.exerciseType), r.exerciseType, r.startTime.toEpochMilli(), r.endTime.toEpochMilli(), kcal)
                }
            }.getOrDefault(emptyList()) else emptyList()
            val sleep = if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) runCatching {
                // Il sonno che termina in questa giornata (notte precedente).
                client.readRecords(ReadRecordsRequest(SleepSessionRecord::class,
                    TimeRangeFilter.between(start.minusSeconds(12 * 3600), end))).records
                    .filter { it.endTime >= start && it.endTime <= end }
                    .sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }
            }.getOrNull() else null
            result[date.toString()] = HealthDay(
                date = date.toString(),
                steps = agg?.get(StepsRecord.COUNT_TOTAL) ?: 0,
                activeKcal = agg?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories,
                totalKcal = agg?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
                sessions = sessions,
                restingHr = agg?.get(RestingHeartRateRecord.BPM_AVG)?.toDouble(),
                avgHr = agg?.get(HeartRateRecord.BPM_AVG)?.toDouble(),
                sleepMinutes = sleep?.takeIf { it > 0 },
                updatedAt = System.currentTimeMillis()
            )
        }
        // Peso: l'ultima pesata registrata (es. bilancia smart sincronizzata con Zepp).
        var weightMsg = ""
        if (HealthPermission.getReadPermission(WeightRecord::class) in granted && Store.current.energy.importWeight) {
            runCatching {
                val w = client.readRecords(ReadRecordsRequest(WeightRecord::class,
                    TimeRangeFilter.between(LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant(), java.time.Instant.now())))
                    .records.maxByOrNull { it.time }
                if (w != null) {
                    val d = w.time.atZone(zone).toLocalDate()
                    val kg = Math.round(w.weight.inKilograms * 10) / 10.0
                    if (Store.current.weights.none { it.date == d.toString() }) { Store.addWeight(d, kg); weightMsg = " Peso importato: $kg kg." }
                }
            }
        }
        Store.update { s ->
            s.copy(health = (s.health + result).filterKeys { it >= LocalDate.now().minusDays(120).toString() },
                energy = s.energy.copy(lastSync = System.currentTimeMillis(), syncError = null, watchConnected = true))
        }
        val today = result[LocalDate.now().toString()]
        return "Sincronizzato: ${today?.steps ?: 0} passi oggi." + weightMsg
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("health-sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork("health-sync")

    fun exerciseName(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Corsa"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Camminata"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Ciclismo"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Cyclette"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Allenamento con i pesi"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Pesi"
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Nuoto"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Escursione"
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Calcio"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Ellittica"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Vogatore"
        else -> "Allenamento"
    }
}

/** Sincronizzazione periodica in background (se il permesso di lettura in background è concesso). */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        if (!Store.current.energy.watchConnected) return Result.success()
        val granted = runCatching { HealthSync.granted(applicationContext) }.getOrDefault(emptySet())
        if (HealthSync.BACKGROUND !in granted) return Result.success()
        return runCatching { HealthSync.sync(applicationContext, 2); Store.saveNow() }
            .fold({ Result.success() }, { Result.retry() })
    }
}
