package com.lifeorganizer.app

import com.lifeorganizer.app.domain.model.OrganizationMode
import com.lifeorganizer.app.domain.model.ThemeMode
import com.lifeorganizer.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserProfileModelTest {

    @Test
    fun testDefaultUserProfile() {
        val profile = UserProfile()
        assertEquals("", profile.name)
        assertEquals("08:00", profile.wakeUpTime)
        assertEquals("00:00", profile.sleepTime)
        assertEquals(ThemeMode.SYSTEM, profile.themeMode)
        assertEquals(OrganizationMode.MANUAL, profile.organizationMode)
        assertFalse(profile.isOnboardingCompleted)
        assertEquals(1, profile.onboardingStep)
    }

    @Test
    fun testThemeModeFromString() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown_val"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(null))
    }

    @Test
    fun testOrganizationModeFromString() {
        assertEquals(OrganizationMode.MANUAL, OrganizationMode.fromString("MANUAL"))
        assertEquals(OrganizationMode.MANUAL, OrganizationMode.fromString("manual"))
        assertEquals(OrganizationMode.AUTOMATIC, OrganizationMode.fromString("AUTOMATIC"))
        assertEquals(OrganizationMode.AUTOMATIC, OrganizationMode.fromString("automatic"))
        assertEquals(OrganizationMode.MANUAL, OrganizationMode.fromString("unknown_val"))
        assertEquals(OrganizationMode.MANUAL, OrganizationMode.fromString(null))
    }
}
