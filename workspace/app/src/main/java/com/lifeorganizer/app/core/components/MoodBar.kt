package com.lifeorganizer.app.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageAccent
import com.lifeorganizer.app.core.theme.SageLight

enum class MoodType(val icon: String, val label: String, val feedback: String) {
    CALM("🍃", "Calmo", "Que serenidade boa. Mantenha essa paz no seu ritmo."),
    FOCUSED("⚡", "Focado", "Excelente momento de energia. Aproveite para avançar no que importa."),
    TIRED("☕", "Cansado", "Seu corpo pede calma. Respeite seus limites e faça pausas curtas."),
    INSPIRED("✨", "Inspirado", "Sinta essa faísca criativa! Um ótimo instante para criar.")
}

/**
 * Native Compose Mood Bar component (FASE B5).
 * Allows users to register how they feel right now with floating icon microinteraction.
 */
@Composable
fun MoodBar(
    selectedMood: MoodType?,
    onMoodSelected: (MoodType) -> Unit,
    modifier: Modifier = Modifier
) {
    var floatingIcon by remember { mutableStateOf<String?>(null) }
    val floatOffset = remember { Animatable(0f) }
    val floatAlpha = remember { Animatable(1f) }

    LaunchedEffect(floatingIcon) {
        if (floatingIcon != null) {
            floatOffset.snapTo(0f)
            floatAlpha.snapTo(1f)
            floatOffset.animateTo(
                targetValue = -35f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
            floatAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350)
            )
            floatingIcon = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Como você está agora?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Micro floating icon display
                if (floatingIcon != null) {
                    Box(
                        modifier = Modifier
                            .offset(y = floatOffset.value.dp)
                            .alpha(floatAlpha.value)
                    ) {
                        Text(text = floatingIcon ?: "", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mood selection row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoodType.values().forEach { mood ->
                    val isSelected = selectedMood == mood
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                floatingIcon = mood.icon
                                onMoodSelected(mood)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) PrimarySage.copy(alpha = 0.22f) else SageLight.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) PrimarySage else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = mood.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) PrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Gentle feedback banner
            selectedMood?.let { mood ->
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SageLight.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mood.feedback,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = PrimaryDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
