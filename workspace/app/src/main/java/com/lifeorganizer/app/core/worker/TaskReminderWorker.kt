package com.lifeorganizer.app.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeorganizer.app.LifeFlowApplication
import com.lifeorganizer.app.core.notifications.LifeFlowNotificationManager

/**
 * WorkManager worker scheduled to trigger completion prompts when an activity's end time arrives.
 */
class TaskReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("TASK_ID", -1L)
        val taskTitle = inputData.getString("TASK_TITLE") ?: "Sua atividade"

        if (taskId != -1L) {
            val notificationManager = LifeFlowNotificationManager(applicationContext)
            notificationManager.showCompletionPrompt(
                taskId = taskId,
                taskTitle = taskTitle,
                promptMessage = "Conseguiu terminar sua atividade?"
            )
        }

        return Result.success()
    }
}
