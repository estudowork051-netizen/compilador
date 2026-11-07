package com.lifeorganizer.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.domain.model.ScheduleEntryModel

/**
 * Completion Prompt Dialog conforming to Step 3 Section 8:
 * "Conseguiu terminar sua atividade?"
 * Options:
 * - Sim, concluí
 * - Ainda estou fazendo
 * - Não consegui fazer
 * - Adiar
 */
@Composable
fun CompletionPromptDialog(
    entry: ScheduleEntryModel,
    contextModel: com.lifeorganizer.app.domain.model.ActivityContextModel? = null,
    onDismissRequest: () -> Unit,
    onConfirmCompleted: (actualDurationMinutes: Int?, advanceContext: Boolean) -> Unit,
    onStillDoing: () -> Unit,
    onMarkNotDone: () -> Unit,
    onPostpone: () -> Unit
) {
    val nextLabel = contextModel?.nextUnitProgress()?.progressLabel

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conseguiu terminar sua atividade?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "\"${entry.activityTitle}\"",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Planejada para ${entry.formattedTimeRange} (${entry.formattedDuration})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (contextModel != null && nextLabel != null && nextLabel != contextModel.progressLabel) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Progresso atual: ${contextModel.progressLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Deseja avançar para o $nextLabel?",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Como foi a execução da atividade?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val now = java.time.LocalTime.now()
                val nowMin = now.hour * 60 + now.minute
                val isToday = entry.date == java.time.LocalDate.now().toString()
                val suggestedActual = when {
                    !isToday -> entry.computedDurationMinutes
                    nowMin <= entry.startTotalMinutes -> null
                    nowMin in (entry.startTotalMinutes + 1) until entry.endTotalMinutes -> nowMin - entry.startTotalMinutes
                    else -> entry.computedDurationMinutes
                }

                if (contextModel != null && nextLabel != null && nextLabel != contextModel.progressLabel) {
                    Button(
                        onClick = { onConfirmCompleted(suggestedActual, true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Concluir e avançar para $nextLabel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Button(
                    onClick = { onConfirmCompleted(suggestedActual, false) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Sim, concluí!", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStillDoing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ainda fazendo", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onPostpone,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Adiar", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onMarkNotDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Não consegui fazer",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        dismissButton = {}
    )
}

/**
 * Friendly Postpone Dialog conforming to Step 3 Section 9 & 13:
 * "Essa atividade ainda está pendente. Quer reagendar?"
 */
@Composable
fun PostponeEntryDialog(
    entry: ScheduleEntryModel,
    onDismissRequest: () -> Unit,
    onPostponeTomorrow: () -> Unit,
    onKeepPending: () -> Unit,
    onCancelEntry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reagendar atividade?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "A atividade \"${entry.activityTitle}\" não foi concluída no horário previsto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seu planejamento pode mudar a qualquer momento de forma natural.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onPostponeTomorrow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mover para amanhã", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onKeepPending,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manter pendente hoje")
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onCancelEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancelar esta ocorrência",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        dismissButton = {}
    )
}
