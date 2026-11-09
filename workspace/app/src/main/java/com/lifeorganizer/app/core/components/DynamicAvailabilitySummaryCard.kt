package com.lifeorganizer.app.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.domain.model.DayAvailability

/**
 * Dynamic summary card showing time distribution, available time, planned activities and remaining free time.
 * Includes visual multi-segment time distribution bar and polite conflict alerts.
 */
@Composable
fun DynamicAvailabilitySummaryCard(
    availability: DayAvailability,
    modifier: Modifier = Modifier,
    onAdjustClick: (() -> Unit)? = null
) {
    val headlineText = when {
        availability.isOverbooked -> "Atenção ao tempo planejado"
        availability.remainingFreeMinutes == 0 && availability.plannedActivitiesMinutes > 0 -> "Dia 100% preenchido!"
        availability.plannedActivitiesMinutes > 0 -> "Seu dia está quase organizado!"
        else -> "Dia livre para planejar"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (availability.isOverbooked) 1.5.dp else 1.dp,
            color = if (availability.isOverbooked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Título do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (availability.isOverbooked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${availability.wakeUpTime} → ${availability.sleepTime}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra visual de distribuição do tempo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    // Segmento de Compromissos Fixos (Lilás)
                    if (availability.fixedRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(availability.fixedRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFF8B5CF6))
                        )
                    }

                    // Segmento de Tarefas Planejadas (Primária / Verde Esmeralda)
                    if (availability.plannedRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(availability.plannedRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFF10B981))
                        )
                    }

                    // Segmento de Tempo Livre Restante (Cinza / Fundo)
                    if (availability.freeRatio > 0f && !availability.isOverbooked) {
                        Box(
                            modifier = Modifier
                                .weight(availability.freeRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFFE2E8F0).copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Indicadores Numéricos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeStatItem(
                    title = "Disponível",
                    value = availability.availableTimeFormatted,
                    dotColor = Color(0xFF6366F1)
                )

                TimeStatItem(
                    title = "Planejado",
                    value = availability.plannedTimeFormatted,
                    dotColor = Color(0xFF10B981)
                )

                TimeStatItem(
                    title = "Livre restante",
                    value = availability.remainingFreeFormatted,
                    dotColor = Color(0xFF94A3B8)
                )
            }

            // Alerta amigável de sobrecarga ou conflito
            AnimatedVisibility(visible = availability.isOverbooked || availability.conflicts.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Aviso",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (availability.isOverbooked) {
                                    "Seu planejamento ultrapassou o tempo disponível em ${availability.overbookedMinutes} min. Quer ajustar algumas atividades?"
                                } else {
                                    "Há horários sobrepostos na sua agenda. Toque para verificar os conflitos."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeStatItem(
    title: String,
    value: String,
    dotColor: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
