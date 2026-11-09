package com.lifeorganizer.app

import com.lifeorganizer.app.data.local.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationV6Test {

    @Test
    fun testMigrationVersionsUpToV6() {
        val m12 = AppDatabase.MIGRATION_1_2
        assertEquals(1, m12.startVersion)
        assertEquals(2, m12.endVersion)

        val m23 = AppDatabase.MIGRATION_2_3
        assertEquals(2, m23.startVersion)
        assertEquals(3, m23.endVersion)

        val m34 = AppDatabase.MIGRATION_3_4
        assertEquals(3, m34.startVersion)
        assertEquals(4, m34.endVersion)

        val m45 = AppDatabase.MIGRATION_4_5
        assertEquals(4, m45.startVersion)
        assertEquals(5, m45.endVersion)

        val m56 = AppDatabase.MIGRATION_5_6
        assertEquals(5, m56.startVersion)
        assertEquals(6, m56.endVersion)
    }

    @Test
    fun testMigration5to6NotNull() {
        assertNotNull(AppDatabase.MIGRATION_5_6)
    }
}
