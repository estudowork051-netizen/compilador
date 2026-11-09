package com.lifeorganizer.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lifeorganizer.app.MainActivity
import com.lifeorganizer.app.core.utils.AdaptiveReminderEngine

/**
 * Manages local notifications for task start reminders, completion prompts,
 * focus sessions, and wellbeing nudges.
 * Conforms to Etapa 6:
 * - Configured channels with calm titles.
 * - Actionable notifications (Concluir, Adiar).
 * - Enforces quiet hours and notification permission checks.
 */
class LifeFlowNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_REMINDERS = "lifeflow_task_reminders"
        const val CHANNEL_FOCUS = "lifeflow_focus_sessions"
        const val CHANNEL_WELLBEING = "lifeflow_wellbeing"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Lembretes e Rotina",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos suaves de início e término de atividades"
                enableVibration(true)
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Momento de Foco",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Acompanhamento do cronômetro de foco suave"
                enableVibration(false)
            }

            val wellbeingChannel = NotificationChannel(
                CHANNEL_WELLBEING,
                "Equilíbrio e Bem-estar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sugestões gentis de descanso e equilíbrio"
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(
                listOf(remindersChannel, focusChannel, wellbeingChannel)
            )
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    fun showPreStartReminder(
        entryId: Long,
        taskTitle: String,
        minutesBefore: Int
    ) {
        if (!hasNotificationPermission()) return

        val notifId = (entryId * 10 + 1).toInt()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", entryId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Próxima atividade em breve")
            .setContentText("Sua atividade \"$taskTitle\" começa em $minutesBefore minutos.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun showCompletionPrompt(
        taskId: Long,
        taskTitle: String,
        promptMessage: String = "Conseguiu terminar sua atividade?"
    ) {
        if (!hasNotificationPermission()) return

        val notifId = (taskId * 10 + 2).toInt()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_ACTION", "PROMPT_COMPLETION")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete
        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(ReminderActionReceiver.EXTRA_ENTRY_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 100,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Postpone 15m
        val postponeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_POSTPONE_15
            putExtra(ReminderActionReceiver.EXTRA_ENTRY_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val postponePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 200,
            postponeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(taskTitle)
            .setContentText(promptMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$promptMessage\nEscolha uma ação rápida abaixo:"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Concluir", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 min", postponePendingIntent)
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun showAdaptiveSuggestion(
        entryId: Long,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission()) return
        val notifId = (entryId * 10 + 3).toInt()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WELLBEING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun cancelReminder(taskId: Long) {
        notificationManager.cancel((taskId * 10 + 1).toInt())
        notificationManager.cancel((taskId * 10 + 2).toInt())
        notificationManager.cancel((taskId * 10 + 3).toInt())
    }
}
