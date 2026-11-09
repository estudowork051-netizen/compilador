package com.lifeorganizer.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageAccent
import com.lifeorganizer.app.core.theme.SageLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Native Compose expanding-ring breathing modal (FASE B4.3).
 * Guides the user through tranquil Inhale (4s) and Exhale (4s) cycles.
 */
@Composable
fun BreatheModal(
    onDismissRequest: () -> Unit
) {
    var isRunning by remember { mutableStateOf(true) }
    var phaseText by remember { mutableStateOf("Inspire suavemente...") }
    val ringScale = remember { Animatable(1.0f) }

    LaunchedEffect(isRunning) {
        while (isRunning && isActive) {
            // Fase Inspire (4 segundos) -> expande
            phaseText = "Inspire profundamente..."
            ringScale.animateTo(
                targetValue = 1.6f,
                animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
            )
            delay(300)

            // Fase Expire (4 segundos) -> contrai
            phaseText = "Expire devagar..."
            ringScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
            )
            delay(300)
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Momento de Respiro 🌿",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Expanding breathing ring container
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsating aura ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(ringScale.value)
                            .clip(CircleShape)
                            .background(SageLight.copy(alpha = 0.45f))
                            .border(2.dp, SageAccent.copy(alpha = 0.6f), CircleShape)
                    )

                    // Middle breathing core
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .clip(CircleShape)
                            .background(PrimarySage.copy(alpha = 0.85f))
                            .border(2.dp, PrimaryDark.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🍃",
                            fontSize = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Phase prompt
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PrimaryDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Acalme a mente e sintonize seu ritmo natural.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Control buttons: Iniciar / Pausar / Fechar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySage)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "Pausar" else "Iniciar")
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}
