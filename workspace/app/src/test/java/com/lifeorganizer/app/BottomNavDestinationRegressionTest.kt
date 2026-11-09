package com.lifeorganizer.app

import com.lifeorganizer.app.core.navigation.BottomNavDestination
import com.lifeorganizer.app.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavDestinationRegressionTest {

    @Test
    fun testBottomNavItems_AreNotNullAndComplete() {
        val items = BottomNavDestination.items
        assertNotNull("BottomNavDestination.items must not be null", items)
        assertEquals("There should be exactly 4 navigation destinations", 4, items.size)
        assertTrue("All destinations in items must have non-blank routes", items.all { it.route.isNotBlank() })
        assertTrue("All destinations in items must have non-blank titles", items.all { it.title.isNotBlank() })

        val expected = listOf(
            BottomNavDestination.Home,
            BottomNavDestination.Planning,
            BottomNavDestination.Habits,
            BottomNavDestination.Settings
        )
        assertEquals(expected, items)
    }

    @Test
    fun testBottomNavDestinationProperties() {
        val items = BottomNavDestination.items
        items.forEach { destination ->
            assertNotNull(destination)
            assertTrue("Route must not be blank", destination.route.isNotBlank())
            assertTrue("Title must not be blank", destination.title.isNotBlank())
            assertNotNull("Icon must not be null", destination.icon)
        }

        assertEquals("home", BottomNavDestination.Home.route)
        assertEquals("Hoje", BottomNavDestination.Home.title)

        assertEquals("planning", BottomNavDestination.Planning.route)
        assertEquals("Agenda", BottomNavDestination.Planning.title)

        assertEquals("habits", BottomNavDestination.Habits.route)
        assertEquals("Hábitos", BottomNavDestination.Habits.title)

        assertEquals("settings", BottomNavDestination.Settings.route)
        assertEquals("Perfil", BottomNavDestination.Settings.title)

        val distinctRoutes = items.map { it.route }.distinct()
        assertEquals(4, distinctRoutes.size)
    }

    @Test
    fun testFromRoute_ValidRoutes() {
        assertEquals(BottomNavDestination.Home, BottomNavDestination.fromRoute("home"))
        assertEquals(BottomNavDestination.Planning, BottomNavDestination.fromRoute("planning"))
        assertEquals(BottomNavDestination.Habits, BottomNavDestination.fromRoute("habits"))
        assertEquals(BottomNavDestination.Settings, BottomNavDestination.fromRoute("settings"))
    }

    @Test
    fun testFromRoute_InvalidAndNullRoutes_NeverThrows() {
        assertNull(BottomNavDestination.fromRoute(null))
        assertNull(BottomNavDestination.fromRoute(""))
        assertNull(BottomNavDestination.fromRoute("   "))
        assertNull(BottomNavDestination.fromRoute("unknown_route"))
        assertNull(BottomNavDestination.fromRoute("onboarding"))
        assertNull(BottomNavDestination.fromRoute("main"))
    }

    @Test
    fun testNavigationSelectionSimulation_WithVariousRoutes() {
        val items = BottomNavDestination.items.filterNotNull()

        // 1. Initial / null route simulation
        val nullRoute: String? = null
        val nullSelections = items.map { dest -> nullRoute != null && nullRoute == dest.route }
        assertTrue(nullSelections.all { !it })

        // 2. Home route simulation
        val homeRoute = "home"
        val homeSelections = items.map { dest -> homeRoute == dest.route }
        assertEquals(listOf(true, false, false, false), homeSelections)

        // 3. Planning route simulation
        val planningRoute = "planning"
        val planningSelections = items.map { dest -> planningRoute == dest.route }
        assertEquals(listOf(false, true, false, false), planningSelections)

        // 4. Habits route simulation
        val habitsRoute = "habits"
        val habitsSelections = items.map { dest -> habitsRoute == dest.route }
        assertEquals(listOf(false, false, true, false), habitsSelections)

        // 5. Settings route simulation
        val settingsRoute = "settings"
        val settingsSelections = items.map { dest -> settingsRoute == dest.route }
        assertEquals(listOf(false, false, false, true), settingsSelections)

        // 6. Subscreen / Internal non-tab route simulation
        val subscreenRoute = "activities/detail/123"
        val subscreenSelections = items.map { dest -> subscreenRoute == dest.route }
        assertTrue(subscreenSelections.all { !it })
    }

    @Test
    fun testOnboardingTransitionToMainContainer_DoesNotThrowNPE() {
        // Simulates the exact state transition after saving onboarding
        val isOnboardingCompleted = true
        val targetScreen = if (isOnboardingCompleted) Screen.Main.route else Screen.Onboarding.route
        assertEquals("main", targetScreen)

        // Inside MainContainerScreen: NavHost backstack entry destination is initially null or starts at Home
        val rawRoute: String? = null
        val currentRoute = rawRoute ?: BottomNavDestination.Home.route
        assertEquals("home", currentRoute)

        // BottomBar renders availableDestinations
        val availableDestinations = BottomNavDestination.items.filterNotNull()
        assertEquals(4, availableDestinations.size)

        // Ensure every destination can safely read route and compute selection
        var selectedCount = 0
        availableDestinations.forEach { destination ->
            val destinationRoute = destination.route
            assertNotNull(destinationRoute)
            if (currentRoute == destinationRoute) {
                selectedCount++
            }
        }
        assertEquals(1, selectedCount)
    }

    @Test
    fun testAppReopeningWithCompletedOnboarding_DestinationResolution() {
        // App is relaunched with persisted onboarding flag = true
        val startDestination = Screen.Main.route
        assertEquals("main", startDestination)

        // NavHost initial destination is Home
        val initialTab = BottomNavDestination.Home.route
        val matchedTab = BottomNavDestination.fromRoute(initialTab)
        assertNotNull(matchedTab)
        assertEquals(BottomNavDestination.Home, matchedTab)

        // Simulating safe navigation lambda in NavHost
        val navigationHistory = mutableListOf<String>()
        var activeRoute = initialTab

        val navigateToTab = { destination: BottomNavDestination? ->
            val targetRoute = destination?.route
            if (targetRoute != null && activeRoute != targetRoute) {
                activeRoute = targetRoute
                navigationHistory.add(targetRoute)
            }
        }

        navigateToTab(BottomNavDestination.Planning)
        assertEquals("planning", activeRoute)

        navigateToTab(BottomNavDestination.Habits)
        assertEquals("habits", activeRoute)

        navigateToTab(BottomNavDestination.Settings)
        assertEquals("settings", activeRoute)

        navigateToTab(BottomNavDestination.Home)
        assertEquals("home", activeRoute)

        // Calling with null does not change active route or crash
        navigateToTab(null)
        assertEquals("home", activeRoute)

        assertEquals(listOf("planning", "habits", "settings", "home"), navigationHistory)
    }
}
