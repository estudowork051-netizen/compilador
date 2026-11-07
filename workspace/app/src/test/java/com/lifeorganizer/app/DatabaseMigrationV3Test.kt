package com.lifeorganizer.app

import com.lifeorganizer.app.data.local.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationV3Test {

    @Test
    fun testMigrationVersions() {
        val m12 = AppDatabase.MIGRATION_1_2
        assertEquals(1, m12.startVersion)
        assertEquals(2, m12.endVersion)

        val m23 = AppDatabase.MIGRATION_2_3
        assertEquals(2, m23.startVersion)
        assertEquals(3, m23.endVersion)
    }

    @Test
    fun testDatabaseSingletonInitializable() {
        assertNotNull(AppDatabase.MIGRATION_2_3)
    }
}
