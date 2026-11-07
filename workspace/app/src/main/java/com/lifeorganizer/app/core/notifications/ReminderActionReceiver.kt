package com.lifeorganizer.app.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.lifeorganizer.app.LifeFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling interactive notification actions:
 * - Mark as completed
 * - Postpone by 15 minutes
 */
class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.lifeorganizer.app.ACTION_COMPLETE"
        const val ACTION_POSTPONE_15 = "com.lifeorganizer.app.ACTION_POSTPONE_15"
        const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notifId != -1) {
            notificationManager?.cancel(notifId)
        }

        if (entryId == -1L) return

        val app = context.applicationContext as? LifeFlowApplication ?: return
        val scheduleRepository = app.scheduleRepository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        scheduleRepository.markCompleted(
                            entryId = entryId,
                            notes = "Concluído via notificação rápida"
                        )
                    }
                    ACTION_POSTPONE_15 -> {
                        scheduleRepository.postponeEntry(
                            id = entryId,
                            notes = "Adiado em 15 minutos via notificação"
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            } finally {
                pendingResult.finish()
            }
        }
    }
}
