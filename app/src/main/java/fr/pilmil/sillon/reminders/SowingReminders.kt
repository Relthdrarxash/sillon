package fr.pilmil.sillon.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.pilmil.sillon.MainActivity
import fr.pilmil.sillon.R
import fr.pilmil.sillon.data.GardenRepository
import fr.pilmil.sillon.data.SillonPrefs
import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.model.todayLocalDate
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Rappel hebdomadaire facultatif, le samedi matin : les semis et plantations qui s'ouvrent
 * cette quinzaine. Rien n'est envoyé quand il n'y a rien à faire.
 */
object SowingReminders {

    private const val WORK_NAME = "rappel-semis"
    const val CHANNEL_ID = "semis"
    private val SEND_AT: LocalTime = LocalTime.of(9, 0)

    fun apply(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(SEND_AT)
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        val request = PeriodicWorkRequestBuilder<SowingReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next))
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Semis de la quinzaine", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Un message le samedi matin quand des semis ou des plantations s'ouvrent."
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class SowingReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission") // Vérifiée par canNotify.
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!SillonPrefs(context).sowingReminders || !SowingReminders.canNotify(context)) return Result.success()
        val plan = DashboardPlanner.plan(GardenRepository(context).loadGarden(), todayLocalDate())
        val names = (plan.sowIndoors + plan.sowOutdoors + plan.transplant).map { it.vegetable.name }.distinct()
        if (names.isEmpty()) return Result.success()

        SowingReminders.ensureChannel(context)
        val shown = names.take(4).joinToString(", ")
        val text = if (names.size > 4) "$shown et ${names.size - 4} autres." else "$shown."
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, SowingReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Au potager cette quinzaine")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("À semer ou planter : $text"))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, notification)
        return Result.success()
    }
}
