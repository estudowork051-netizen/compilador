package com.lifeorganizer.app.core.analytics

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.lifeorganizer.app.data.repository.DeviceUsageRepository
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppClassificationModel
import com.lifeorganizer.app.domain.model.AppUsageStatModel

/**
 * Architectural service for privacy-preserving, consent-based device usage monitoring.
 * Conforms to Etapa 6:
 * - Checks user consent and system OPSTR_GET_USAGE_STATS permission.
 * - Categorizes apps gracefully without assuming phone use is automatically procrastination.
 * - Stores aggregated durations in local Room database.
 */
interface UsageMonitoringContract {
    fun isUsageAccessGranted(): Boolean
    fun createUsageAccessIntent(): Intent
    suspend fun collectUsageInWindow(
        startMillis: Long,
        endMillis: Long,
        activityId: Long? = null
    ): List<AppUsageStatModel>
}

class UsageMonitoringService(
    private val context: Context,
    private val deviceUsageRepository: DeviceUsageRepository? = null
) : UsageMonitoringContract {

    private val appOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    }

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    override fun isUsageAccessGranted(): Boolean {
        val appOps = appOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override suspend fun collectUsageInWindow(
        startMillis: Long,
        endMillis: Long,
        activityId: Long?
    ): List<AppUsageStatModel> {
        if (!isUsageAccessGranted() || usageStatsManager == null) {
            return emptyList()
        }

        val stats: List<UsageStats> = try {
            usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startMillis,
                endMillis
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val packageManager = context.packageManager
        val result = mutableListOf<AppUsageStatModel>()

        for (item in stats) {
            val totalSeconds = item.totalTimeInForeground / 1000L
            if (totalSeconds <= 0L || item.packageName == context.packageName) continue

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(item.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                item.packageName
            }

            // Check custom classification from repository or infer default
            val classification = deviceUsageRepository?.getClassificationForPackage(item.packageName)?.classification
                ?: inferClassification(item.packageName, appName)

            result.add(
                AppUsageStatModel(
                    packageName = item.packageName,
                    appName = appName,
                    totalTimeInForegroundSeconds = totalSeconds,
                    classification = classification,
                    windowStartMillis = startMillis,
                    windowEndMillis = endMillis
                )
            )
        }

        // Sort descending by foreground time
        val sorted = result.sortedByDescending { it.totalTimeInForegroundSeconds }

        // Optionally persist into repository if available
        if (sorted.isNotEmpty() && deviceUsageRepository != null) {
            deviceUsageRepository.recordUsageLogs(sorted, activityId)
        }

        return sorted
    }

    companion object {
        fun inferClassification(packageName: String, appName: String): AppCategoryClassification {
            val lowerPkg = packageName.lowercase()
            val lowerName = appName.lowercase()

            return when {
                lowerPkg.contains("instagram") || lowerPkg.contains("tiktok") ||
                lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
                lowerPkg.contains("x.android") || lowerPkg.contains("reddit") ||
                lowerPkg.contains("game") || lowerName.contains("jogo") ||
                lowerPkg.contains("netflix") || lowerPkg.contains("disney") ||
                lowerPkg.contains("twitch") -> AppCategoryClassification.DISTRACTION

                lowerPkg.contains("classroom") || lowerPkg.contains("duolingo") ||
                lowerPkg.contains("anki") || lowerPkg.contains("kindle") ||
                lowerPkg.contains("coursera") || lowerPkg.contains("udemy") -> AppCategoryClassification.STUDY_TOOL

                lowerPkg.contains("notion") || lowerPkg.contains("slack") ||
                lowerPkg.contains("trello") || lowerPkg.contains("docs") ||
                lowerPkg.contains("sheets") || lowerPkg.contains("github") ||
                lowerPkg.contains("drive") || lowerPkg.contains("todoist") ||
                lowerPkg.contains("office") || lowerPkg.contains("teams") -> AppCategoryClassification.WORK

                lowerPkg.contains("spotify") || lowerPkg.contains("youtube") ||
                lowerPkg.contains("music") || lowerPkg.contains("podcast") ||
                lowerPkg.contains("calm") || lowerPkg.contains("headspace") ||
                lowerPkg.contains("fit") || lowerPkg.contains("health") ||
                lowerPkg.contains("strava") || lowerPkg.contains("meditation") ||
                lowerPkg.contains("sleep") -> AppCategoryClassification.LEISURE

                else -> AppCategoryClassification.UNCLASSIFIED
            }
        }
    }
}
