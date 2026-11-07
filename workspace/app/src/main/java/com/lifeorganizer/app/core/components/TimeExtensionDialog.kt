package com.lifeorganizer.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.domain.model.ScheduleEntryModel

@Composable
fun TimeExtensionDialog(
    entry: ScheduleEntryModel,
    nextEntry: ScheduleEntryModel?,
    onDismiss: () -> Unit,
    onConfirmExtension: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(15) }
    var customText by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    val activeDuration = if (isCustomSelected) {
        customText.toIntOrNull() ?: 15
    } else {
        selectedMinutes
    }

    // Check potential conflict with next upcoming task
    val currentEndMin = entry.endHour * 60 + entry.endMinute
    val projectedEndMin = currentEndMin + activeDuration
    val conflictNextEntry = if (nextEntry != null && projectedEndMin > (nextEntry.startHour * 60 + nextEntry.startMinute)) nextEntry else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Precisa de mais tempo?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Você está realizando: \"${entry.activityTitle}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Escolha quanto tempo gostaria de estender:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick buttons: +10m, +15m, +30m
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30).forEach { mins ->
                        val isSelected = !isCustomSelected && selectedMinutes == mins
                        OutlinedButton(
                            onClick = {
                                selectedMinutes = mins
                                isCustomSelected = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = if (isSelected) {
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            } else {
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("+$mins min", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom minutes input
                OutlinedTextField(
                    value = customText,
                    onValueChange = {
                        customText = it.filter { char -> char.isDigit() }.take(3)
                        isCustomSelected = true
                    },
                    label = { Text("Outro valor (minutos)") },
                    placeholder = { Text("Ex: 20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (conflictNextEntry != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Essa extensão sobrepõe o início de \"${conflictNextEntry.activityTitle}\" (${conflictNextEntry.timeFormatted}). O horário será ajustado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmExtension(activeDuration) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar +$activeDuration min")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
