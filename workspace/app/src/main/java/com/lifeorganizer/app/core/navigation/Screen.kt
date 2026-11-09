package com.lifeorganizer.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
}

sealed class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavDestination(
        route = "home",
        title = "Hoje",
        icon = Icons.Rounded.Today
    )

    data object Activities : BottomNavDestination(
        route = "activities",
        title = "Atividades",
        icon = Icons.AutoMirrored.Rounded.FormatListBulleted
    )

    data object Planning : BottomNavDestination(
        route = "planning",
        title = "Agenda",
        icon = Icons.Rounded.CalendarMonth
    )

    data object Habits : BottomNavDestination(
        route = "habits",
        title = "Hábitos",
        icon = Icons.Rounded.TrackChanges
    )

    data object Settings : BottomNavDestination(
        route = "settings",
        title = "Perfil",
        icon = Icons.Rounded.Person
    )

    companion object {
        val items: List<BottomNavDestination>
            get() = listOfNotNull(Home, Planning, Habits, Settings)

        fun fromRoute(route: String?): BottomNavDestination? {
            if (route.isNullOrBlank()) return null
            return items.firstOrNull { it.route == route }
        }
    }
}
