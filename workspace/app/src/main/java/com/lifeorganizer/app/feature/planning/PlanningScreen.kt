package com.lifeorganizer.app.feature.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.lifeorganizer.app.core.components.AddActivityBottomSheet
import com.lifeorganizer.app.core.components.CompletionPromptDialog
import com.lifeorganizer.app.core.components.DynamicAvailabilitySummaryCard
import com.lifeorganizer.app.core.components.EmptyStateView
import com.lifeorganizer.app.core.components.FixedCommitmentAgendaCard
import com.lifeorganizer.app.core.components.FreeTimeSlotAgendaCard
import com.lifeorganizer.app.core.components.PostponeEntryDialog
import com.lifeorganizer.app.core.components.ProposalReviewBottomSheet
import com.lifeorganizer.app.core.components.ScheduleActivityBottomSheet
import com.lifeorganizer.app.core.components.ScheduledTaskAgendaCard
import com.lifeorganizer.app.core.components.WeekNavigationBar
import com.lifeorganizer.app.core.navigation.BottomNavDestination
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel,
    onNavigateToDestination: (BottomNavDestination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openScheduleSheet(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Agendar atividade",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Cabeçalho com atalho para Minhas Atividades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Planejamento inteligente",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Organize sua rotina e tempo disponível.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable { onNavigateToDestination(BottomNavDestination.Activities) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Minhas atividades",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navegação Semanal e Seletor de Dias
            WeekNavigationBar(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                onPreviousWeek = { viewModel.previousWeek() },
                onNextWeek = { viewModel.nextWeek() },
                onTodayClick = { viewModel.resetToToday() },
                daysWithTasks = uiState.daysWithTasks
            )

            uiState.feedbackMessage?.let { feedback ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.dismissFeedback() }
                ) {
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card com Resumo Dinâmico da Disponibilidade e Barra de Distribuição
            DynamicAvailabilitySummaryCard(
                availability = uiState.availability,
                onAdjustClick = { viewModel.openScheduleSheet(null) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ações de Organização Automática (Etapa 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.generateAutoRoutine() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sugerir Rotina", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                if (uiState.selectedDate == LocalDate.now()) {
                    OutlinedButton(
                        onClick = { viewModel.reorganizeToday() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reorganizar dia", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título da Linha do Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agenda do dia",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = uiState.displayDateFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linha do Tempo Interativa (Timeline)
            val hasItems = uiState.fixedActivitiesForDay.isNotEmpty() || uiState.scheduledEntriesForDay.isNotEmpty()

            if (!hasItems) {
                EmptyStateView(
                    title = "Nenhuma atividade agendada",
                    description = "Seu dia ainda está completamente livre. Escolha uma tarefa cadastrada e defina um horário.",
                    icon = Icons.Rounded.CalendarToday,
                    actionText = "Agendar primeira atividade",
                    onActionClick = { viewModel.openScheduleSheet(null) }
                )
            } else {
                // 1. Compromissos Fixos
                if (uiState.fixedActivitiesForDay.isNotEmpty()) {
                    Text(
                        text = "Compromissos fixos",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    uiState.fixedActivitiesForDay.forEach { fixedAct ->
                        FixedCommitmentAgendaCard(
                            activity = fixedAct,
                            onClick = {}
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 2. Tarefas Agendadas no Dia
                if (uiState.scheduledEntriesForDay.isNotEmpty()) {
                    Text(
                        text = "Atividades planejadas (${uiState.scheduledEntriesForDay.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    uiState.scheduledEntriesForDay.forEachIndexed { index, entry ->
                        ScheduledTaskAgendaCard(
                            entry = entry,
                            onStatusToggle = { viewModel.onTaskCheckboxClicked(entry) },
                            onDetailsClick = { viewModel.onTaskCheckboxClicked(entry) },
                            onPostponeClick = { viewModel.openPostponeDialog(entry) },
                            onMarkNotDoneClick = { viewModel.markNotDone(entry) },
                            onRemoveFromDayClick = { viewModel.removeEntryFromDay(entry) },
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.scheduledEntriesForDay.size - 1,
                            onMoveUp = { viewModel.reorderScheduledEntries(index, index - 1) },
                            onMoveDown = { viewModel.reorderScheduledEntries(index, index + 1) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 3. Intervalos Livres Disponíveis (Gaps)
                if (uiState.availability.freeSlots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Horários livres disponíveis",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    uiState.availability.freeSlots.take(3).forEach { slot ->
                        FreeTimeSlotAgendaCard(
                            slot = slot,
                            onAllocateClick = { viewModel.openScheduleSheet(slot) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    // Modal de Agendamento de Atividade
    if (uiState.showScheduleSheet) {
        ScheduleActivityBottomSheet(
            activities = uiState.allActivities,
            availability = uiState.availability,
            initialSlot = uiState.selectedSlotForSchedule,
            initialActivity = uiState.selectedActivityForSchedule,
            executionRecords = uiState.executionRecords,
            balancePreference = uiState.balancePreference,
            onDismissRequest = { viewModel.closeScheduleSheet() },
            onCreateNewActivity = { viewModel.openCreateActivitySheet() },
            onConfirmSchedule = { activity, hour, minute, duration ->
                viewModel.confirmSchedule(activity, hour, minute, duration)
            }
        )
    }

    // Modal de Criação de Atividade dentro do Fluxo de Planejamento (Seção 6)
    if (uiState.showCreateActivitySheet) {
        AddActivityBottomSheet(
            onDismissRequest = { viewModel.closeCreateActivitySheet() },
            onSaveActivity = { viewModel.saveNewActivity(it) },
            onSaveActivityWithContext = { act, ctx -> viewModel.saveNewActivity(act, ctx) }
        )
    }

    // Diálogo de Confirmação de Conclusão ("Conseguiu terminar sua atividade?")
    uiState.activeCompletionPromptEntry?.let { entry ->
        CompletionPromptDialog(
            entry = entry,
            contextModel = uiState.activeCompletionPromptContext,
            onDismissRequest = { viewModel.confirmCompletion(entry, null, false) },
            onConfirmCompleted = { actualDuration, advanceContext ->
                viewModel.confirmCompletion(entry, actualDuration, advanceContext)
            },
            onStillDoing = { viewModel.markStillDoing(entry) },
            onMarkNotDone = { viewModel.markNotDone(entry) },
            onPostpone = { viewModel.openPostponeDialog(entry) }
        )
    }

    // Modal de Revisão da Proposta de Rotina Automática (Etapa 4)
    if (uiState.showProposalSheet && uiState.activeProposal != null) {
        ProposalReviewBottomSheet(
            proposal = uiState.activeProposal!!,
            onDismissRequest = { viewModel.dismissProposalSheet() },
            onApprove = { viewModel.approveProposal(it) }
        )
    }

    // Diálogo de Reagendamento Amigável
    uiState.activePostponeEntry?.let { entry ->
        PostponeEntryDialog(
            entry = entry,
            onDismissRequest = { viewModel.closePostponeDialog() },
            onPostponeTomorrow = { viewModel.postponeToTomorrow(entry) },
            onKeepPending = { viewModel.closePostponeDialog() },
            onCancelEntry = { viewModel.removeEntryFromDay(entry); viewModel.closePostponeDialog() }
        )
    }
}
