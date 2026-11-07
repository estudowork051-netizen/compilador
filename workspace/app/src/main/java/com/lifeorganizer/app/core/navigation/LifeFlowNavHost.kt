package com.lifeorganizer.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeorganizer.app.LifeFlowApplication
import com.lifeorganizer.app.core.components.LifeFlowBottomBar
import com.lifeorganizer.app.feature.activities.ActivitiesScreen
import com.lifeorganizer.app.feature.activities.ActivitiesViewModel
import com.lifeorganizer.app.feature.habits.HabitsScreen
import com.lifeorganizer.app.feature.habits.HabitsViewModel
import com.lifeorganizer.app.feature.home.HomeScreen
import com.lifeorganizer.app.feature.home.HomeViewModel
import com.lifeorganizer.app.feature.onboarding.OnboardingScreen
import com.lifeorganizer.app.feature.onboarding.OnboardingViewModel
import com.lifeorganizer.app.feature.planning.PlanningScreen
import com.lifeorganizer.app.feature.planning.PlanningViewModel
import com.lifeorganizer.app.feature.settings.SettingsScreen
import com.lifeorganizer.app.feature.settings.SettingsViewModel

@Composable
fun LifeFlowRootNavHost(
    isOnboardingCompleted: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (isOnboardingCompleted) {
        Screen.Main.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Onboarding.route) {
            val repository = LifeFlowApplication.instance.preferencesRepository
            val viewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.provideFactory(repository)
            )

            OnboardingScreen(
                viewModel = viewModel,
                onFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen()
        }
    }
}

@Composable
fun MainContainerScreen(
    mainNavController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavDestination.Home.route

    Scaffold(
        bottomBar = {
            LifeFlowBottomBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    if (currentRoute != destination.route) {
                        mainNavController.navigate(destination.route) {
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = BottomNavDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(BottomNavDestination.Home.route) {
                val prefRepository = LifeFlowApplication.instance.preferencesRepository
                val activityRepository = LifeFlowApplication.instance.activityRepository
                val scheduleRepository = LifeFlowApplication.instance.scheduleRepository
                val contextRepository = LifeFlowApplication.instance.contextRepository
                val learningRepository = LifeFlowApplication.instance.learningRepository
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        prefRepository,
                        activityRepository,
                        scheduleRepository,
                        contextRepository,
                        learningRepository,
                        LifeFlowApplication.instance.focusSessionManager
                    )
                )
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDestination = { destination ->
                        mainNavController.navigate(destination.route) {
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(BottomNavDestination.Activities.route) {
                val activityRepository = LifeFlowApplication.instance.activityRepository
                val contextRepository = LifeFlowApplication.instance.contextRepository
                val viewModel: ActivitiesViewModel = viewModel(
                    factory = ActivitiesViewModel.provideFactory(activityRepository, contextRepository)
                )
                ActivitiesScreen(
                    viewModel = viewModel,
                    onBackClick = { mainNavController.popBackStack() }
                )
            }

            composable(BottomNavDestination.Planning.route) {
                val prefRepository = LifeFlowApplication.instance.preferencesRepository
                val activityRepository = LifeFlowApplication.instance.activityRepository
                val scheduleRepository = LifeFlowApplication.instance.scheduleRepository
                val contextRepository = LifeFlowApplication.instance.contextRepository
                val learningRepository = LifeFlowApplication.instance.learningRepository
                val viewModel: PlanningViewModel = viewModel(
                    factory = PlanningViewModel.provideFactory(prefRepository, activityRepository, scheduleRepository, contextRepository, learningRepository)
                )
                PlanningScreen(
                    viewModel = viewModel,
                    onNavigateToDestination = { destination ->
                        mainNavController.navigate(destination.route) {
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(BottomNavDestination.Habits.route) {
                val scheduleRepository = LifeFlowApplication.instance.scheduleRepository
                val activityRepository = LifeFlowApplication.instance.activityRepository
                val learningRepository = LifeFlowApplication.instance.learningRepository
                val focusRepository = LifeFlowApplication.instance.focusRepository
                val deviceUsageRepository = LifeFlowApplication.instance.deviceUsageRepository
                val viewModel: HabitsViewModel = viewModel(
                    factory = HabitsViewModel.provideFactory(
                        scheduleRepository,
                        activityRepository,
                        learningRepository,
                        focusRepository,
                        deviceUsageRepository
                    )
                )
                HabitsScreen(viewModel = viewModel)
            }

            composable(BottomNavDestination.Settings.route) {
                val repository = LifeFlowApplication.instance.preferencesRepository
                val learningRepository = LifeFlowApplication.instance.learningRepository
                val activityRepository = LifeFlowApplication.instance.activityRepository
                val deviceUsageRepository = LifeFlowApplication.instance.deviceUsageRepository
                val usageMonitoringService = LifeFlowApplication.instance.usageMonitoringService
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        repository,
                        learningRepository,
                        activityRepository,
                        deviceUsageRepository,
                        usageMonitoringService
                    )
                )
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
