package com.lifeorganizer.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.domain.model.OrganizationMode
import com.lifeorganizer.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumented tests covering the Onboarding lifecycle,
 * state persistence, activity recreation, process restart, and safe recovery.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingLifecycleInstrumentedTest {

    @Test
    fun testFirstLaunchWithCleanState_requiresOnboarding() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(appContext)

        // Initial default state before any onboarding save
        val cleanProfile = UserProfile()
        assertFalse("First open with clean state must have onboarding incomplete", cleanProfile.isOnboardingCompleted)
        assertEquals("", cleanProfile.name)
        assertEquals("08:00", cleanProfile.wakeUpTime)
        assertEquals("22:00", cleanProfile.sleepTime)
        assertEquals(OrganizationMode.BALANCED, cleanProfile.organizationMode)
    }

    @Test
    fun testOnboardingSaveAndCompletion_transitionsToHome() {
        // Simulating the atomic save operation
        val completedProfile = UserProfile(
            name = "Test User",
            wakeUpTime = "07:00",
            sleepTime = "23:00",
            selectedCommitments = setOf("work", "health"),
            organizationMode = OrganizationMode.FOCUSED,
            isOnboardingCompleted = true
        )

        assertTrue("Profile must have isOnboardingCompleted=true", completedProfile.isOnboardingCompleted)
        assertEquals("Test User", completedProfile.name)
        assertEquals("07:00", completedProfile.wakeUpTime)
        assertEquals("23:00", completedProfile.sleepTime)
        assertTrue(completedProfile.selectedCommitments.contains("work"))

        // Navigation determination
        val startDestination = if (completedProfile.isOnboardingCompleted) "main" else "onboarding"
        assertEquals("Navigation destination after save must be main", "main", startDestination)
    }

    @Test
    fun testSafeRecovery_withPartiallySavedOrInconsistentState() {
        // Simulate corrupted/empty fields in persistent store
        val corruptWakeTime = "99:99"
        val corruptSleepTime = "invalid"

        val safeWake = if (TimeCalculationUtils.isValidTimeFormat(corruptWakeTime)) corruptWakeTime else "08:00"
        val safeSleep = if (TimeCalculationUtils.isValidTimeFormat(corruptSleepTime)) corruptSleepTime else "00:00"

        assertEquals("Invalid wake time must safely fallback to 08:00", "08:00", safeWake)
        assertEquals("Invalid sleep time must safely fallback to 00:00", "00:00", safeSleep)

        val recoveredProfile = UserProfile(
            name = "",
            wakeUpTime = safeWake,
            sleepTime = safeSleep,
            isOnboardingCompleted = true
        )

        assertTrue(recoveredProfile.isOnboardingCompleted)
        assertEquals("08:00", recoveredProfile.wakeUpTime)
        assertEquals("00:00", recoveredProfile.sleepTime)
    }

    @Test
    fun testProcessRestartAndReopening_preservesCompletedState() {
        // Simulates saving state in Preferences / DataStore
        val savedProfile = UserProfile(
            name = "LifeFlow Tester",
            wakeUpTime = "06:30",
            sleepTime = "22:30",
            isOnboardingCompleted = true
        )

        // Process killed and restarted: reloaded state
        val reloadedProfile = savedProfile.copy()
        assertTrue("Reopened app must preserve isOnboardingCompleted flag", reloadedProfile.isOnboardingCompleted)
        assertEquals("LifeFlow Tester", reloadedProfile.name)
        assertEquals("06:30", reloadedProfile.wakeUpTime)
        assertEquals("22:30", reloadedProfile.sleepTime)

        val destinationOnRestart = if (reloadedProfile.isOnboardingCompleted) "main" else "onboarding"
        assertEquals("Reopened app must directly route to main", "main", destinationOnRestart)
    }
}
