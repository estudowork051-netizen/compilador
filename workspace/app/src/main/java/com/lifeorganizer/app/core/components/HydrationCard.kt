package com.lifeorganizer.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeorganizer.app.core.theme.CreamCard
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageAccent
import com.lifeorganizer.app.core.theme.SageLight
import kotlin.math.sin

/**
 * Native Compose Hydration Tracker Card with animated water wave (FASE B8).
 * Features wave simulation using Canvas + Path and +250ml increments.
 */
@Composable
fun HydrationCard(
    currentAmountMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier,
    recordsCount: Int = 0,
    canUndo: Boolean = false,
    onUndoWater: () -> Unit = {},
    dailyGoalMl: Int = 2000
) {
    val animatedFill = remember { Animatable(0f) }

    LaunchedEffect(currentAmountMl) {
        val targetFraction = (currentAmountMl.toFloat() / dailyGoalMl.toFloat()).coerceIn(0f, 1f)
        animatedFill.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 600)
        )
    }

    // Infinite wave phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Animated Water Flask Container
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SageLight.copy(alpha = 0.35f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Wave Canvas
                Canvas(modifier = Modifier.fillMaxWidth().height(88.dp)) {
                    val width = size.width
                    val height = size.height
                    val fillHeight = height * animatedFill.value
                    val waterTop = height - fillHeight

                    val wavePath = Path()
                    wavePath.moveTo(0f, height)
                    wavePath.lineTo(0f, waterTop)

                    val waveAmplitude = 4f
                    val waveFrequency = 0.08f

                    var x = 0f
                    while (x <= width) {
                        val y = waterTop + sin(x * waveFrequency + wavePhase) * waveAmplitude
                        wavePath.lineTo(x, y)
                        x += 4f
                    }

                    wavePath.lineTo(width, height)
                    wavePath.close()

                    drawPath(
                        path = wavePath,
                        color = Color(0xFF679589).copy(alpha = 0.85f)
                    )
                }

                // Drop Icon overlay
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center: Hydration metrics & info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Água hoje: $recordsCount registros 💧",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$currentAmountMl ml bebidos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                )
                Text(
                    text = "Meta: $dailyGoalMl ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Action buttons
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = { onAddWater(250) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySage)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bebi água",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (canUndo) {
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.TextButton(
                        onClick = onUndoWater,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Desfazer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
