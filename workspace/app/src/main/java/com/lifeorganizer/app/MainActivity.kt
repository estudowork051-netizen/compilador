package com.lifeorganizer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lifeorganizer.app.core.navigation.LifeFlowRootNavHost
import com.lifeorganizer.app.core.theme.LifeFlowTheme
import com.lifeorganizer.app.domain.model.ThemeMode
import com.lifeorganizer.app.domain.model.UserProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("ACTIVITY_ON_CREATE_STARTED")
        enableEdgeToEdge()

        val preferencesRepository = (application as LifeFlowApplication).preferencesRepository

        setContent {
            val userProfileState by preferencesRepository.userProfileFlow
                .collectAsState(initial = null)

            val currentTheme = userProfileState?.themeMode ?: ThemeMode.SYSTEM

            var isSplashVisible by remember { mutableStateOf(true) }

            DisposableEffect(isSplashVisible) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (isSplashVisible) {
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            LifeFlowTheme(themeMode = currentTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val isRecovery = com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.isRecoveryModeActive

                    if (isRecovery) {
                        com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("RECOVERY_SCREEN_DISPLAYED")
                        com.lifeorganizer.app.feature.recovery.RecoveryScreen(
                            onRetryNormalFlow = {
                                recreate()
                            },
                            onReturnToOnboarding = {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    preferencesRepository.setOnboardingCompleted(false)
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        recreate()
                                    }
                                }
                            }
                        )
                    } else if (isSplashVisible) {
                        com.lifeorganizer.app.feature.splash.SplashScreen(
                            onSplashFinished = {
                                isSplashVisible = false
                            }
                        )
                    } else {
                        val profile = userProfileState
                        if (profile == null) {
                            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("DATASTORE_READ_STARTED")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent(
                                "DATASTORE_READ_SUCCESS",
                                "isOnboardingCompleted=${profile.isOnboardingCompleted}"
                            )
                            LifeFlowRootNavHost(
                                isOnboardingCompleted = profile.isOnboardingCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}
