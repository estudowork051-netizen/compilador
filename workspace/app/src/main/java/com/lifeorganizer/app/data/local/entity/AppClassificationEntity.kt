package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppClassificationModel

@Entity(tableName = "app_classifications")
data class AppClassificationEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val classification: String = AppCategoryClassification.UNCLASSIFIED.name,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): AppClassificationModel {
        return AppClassificationModel(
            packageName = packageName,
            appName = appName,
            classification = AppCategoryClassification.fromString(classification),
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(model: AppClassificationModel): AppClassificationEntity {
            return AppClassificationEntity(
                packageName = model.packageName,
                appName = model.appName,
                classification = model.classification.name,
                updatedAt = model.updatedAt
            )
        }
    }
}
