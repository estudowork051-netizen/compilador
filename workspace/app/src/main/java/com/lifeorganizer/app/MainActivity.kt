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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lifeorganizer.app.core.navigation.LifeFlowRootNavHost
import com.lifeorganizer.app.core.theme.LifeFlowTheme
import com.lifeorganizer.app.domain.model.ThemeMode
import com.lifeorganizer.app.domain.model.UserProfile

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = (application as LifeFlowApplication).preferencesRepository

        setContent {
            val userProfileState by preferencesRepository.userProfileFlow
                .collectAsState(initial = null)

            val currentTheme = userProfileState?.themeMode ?: ThemeMode.SYSTEM

            LifeFlowTheme(themeMode = currentTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val profile = userProfileState
                    if (profile == null) {
                        // Brief initial load state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        LifeFlowRootNavHost(
                            isOnboardingCompleted = profile.isOnboardingCompleted
                        )
                    }
                }
            }
        }
    }
}
