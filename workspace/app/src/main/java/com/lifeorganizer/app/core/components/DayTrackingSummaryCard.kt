package com.lifeorganizer.app.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus

@Composable
fun DayTrackingSummaryCard(
    scheduledEntries: List<ScheduleEntryModel>,
    remainingFreeMinutes: Int,
    currentActiveEntry: ScheduleEntryModel?,
    nextUpcomingEntry: ScheduleEntryModel?,
    onCompleteClick: (ScheduleEntryModel) -> Unit,
    onStillDoingClick: (ScheduleEntryModel) -> Unit,
    onPostponeClick: (ScheduleEntryModel) -> Unit,
    onStartFocusClick: (ScheduleEntryModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = scheduledEntries.size
    val completedCount = scheduledEntries.count { it.status == ScheduleStatus.COMPLETED }
    val postponedCount = scheduledEntries.count { it.status == ScheduleStatus.POSTPONED }
    val pendingCount = scheduledEntries.count { it.status == ScheduleStatus.PENDING || it.status == ScheduleStatus.IN_PROGRESS }

    val narrativeHeader = when {
        totalCount == 0 -> "Seu dia ainda está livre"
        completedCount == totalCount && totalCount > 0 -> "Todas as atividades concluídas!"
        completedCount > 0 -> "Seu dia está fluindo bem!"
        else -> "Começando a rotina de hoje"
    }

    val narrativeSubtitle = buildString {
        if (totalCount > 0) {
            append("Você concluiu $completedCount de $totalCount ${if (totalCount == 1) "atividade" else "atividades"}.")
        } else {
            append("Adicione atividades ou aproveite seu tempo livre com tranquilidade.")
        }
        if (remainingFreeMinutes > 0) {
            val freeHours = remainingFreeMinutes / 60
            val freeMins = remainingFreeMinutes % 60
            val freeText = if (freeHours > 0) "${freeHours}h ${freeMins}min" else "${freeMins}min"
            append(" Você ainda tem $freeText livres.")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header e Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acompanhamento do Dia",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = narrativeHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = narrativeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chips rápidos de status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCounterChip(
                    label = "Concluídas",
                    count = completedCount,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatusCounterChip(
                    label = "Pendentes",
                    count = pendingCount,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                StatusCounterChip(
                    label = "Adiada(s)",
                    count = postponedCount,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            // O que está fazendo agora
            if (currentActiveEntry != null) {
                Spacer(modifier = Modifier.height(18.dp))
                ActiveNowSection(
                    entry = currentActiveEntry,
                    onComplete = { onCompleteClick(currentActiveEntry) },
                    onStillDoing = { onStillDoingClick(currentActiveEntry) },
                    onPostpone = { onPostponeClick(currentActiveEntry) },
                    onStartFocus = { onStartFocusClick(currentActiveEntry) }
                )
            } else if (nextUpcomingEntry != null) {
                // Próxima atividade
                Spacer(modifier = Modifier.height(16.dp))
                NextUpcomingSection(
                    entry = nextUpcomingEntry,
                    onStartFocus = { onStartFocusClick(nextUpcomingEntry) },
                    onComplete = { onCompleteClick(nextUpcomingEntry) },
                    onPostpone = { onPostponeClick(nextUpcomingEntry) }
                )
            }
        }
    }
}

@Composable
private fun StatusCounterChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveNowSection(
    entry: ScheduleEntryModel,
    onComplete: () -> Unit,
    onStillDoing: () -> Unit,
    onPostpone: () -> Unit,
    onStartFocus: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Acontecendo agora",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF047857)
                    )
                }

                Text(
                    text = "${entry.timeFormatted} (${entry.targetDurationMinutes} min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.activityTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ações rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Concluir", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onStillDoing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.MoreTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Tempo", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onStartFocus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Foco", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun NextUpcomingSection(
    entry: ScheduleEntryModel,
    onStartFocus: () -> Unit,
    onComplete: () -> Unit,
    onPostpone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Próxima atividade",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${entry.timeFormatted} (${entry.targetDurationMinutes} min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.activityTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Concluir", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onStartFocus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focar", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onPostpone,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adiar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
