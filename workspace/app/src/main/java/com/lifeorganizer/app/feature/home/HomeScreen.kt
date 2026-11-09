package com.lifeorganizer.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.lifeorganizer.app.core.components.AddActivityBottomSheet
import com.lifeorganizer.app.core.components.CompletionPromptDialog
import com.lifeorganizer.app.core.components.ConfettiEffect
import com.lifeorganizer.app.core.components.FixedCommitmentAgendaCard
import com.lifeorganizer.app.core.components.HydrationCard
import com.lifeorganizer.app.core.components.PostponeEntryDialog
import com.lifeorganizer.app.core.components.ScheduleActivityBottomSheet
import com.lifeorganizer.app.core.components.ScreenUsageCard
import com.lifeorganizer.app.core.components.TimeExtensionDialog
import com.lifeorganizer.app.core.components.TodayScheduleItemCard
import com.lifeorganizer.app.core.components.VioraAssistantBottomSheet
import com.lifeorganizer.app.core.navigation.BottomNavDestination
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageLight
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDestination: (BottomNavDestination) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshTime()
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preparando seu espaço com calma...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("HOME_SCREEN_RENDERED")
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.markStartupStabilized()
        }
    }

    val greetingHeader = if (uiState.userName.isNotBlank()) {
        "${uiState.greeting}, ${uiState.userName} 🌿"
    } else {
        "${uiState.greeting} 🌿"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Cabeçalho Viora: Marca, Saudação e Assistente Conversacional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Viora",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PrimarySage
                            )
                            Text(
                                text = " • Seu ritmo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greetingHeader,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.dayOfWeekFormatted}, ${uiState.currentDateFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SageLight.copy(alpha = 0.55f),
                        modifier = Modifier.clickable { viewModel.openAssistant() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Assistente",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Card Principal: Atividade em andamento / Próxima atividade (1-tap complete)
                HomeMainFocusCard(
                    activeEntry = uiState.activeNowEntry,
                    nextEntry = uiState.nextUpcomingEntry,
                    remainingFreeFormatted = TimeCalculationUtils.formatMinutes(uiState.todayAvailability.remainingFreeMinutes),
                    onComplete = { entry ->
                        showConfetti = true
                        viewModel.completeTaskDirectly(entry)
                    },
                    onOrganizeWithAI = {
                        onNavigateToDestination(BottomNavDestination.Planning)
                    },
                    onAddActivity = {
                        viewModel.openScheduleSheet()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Resumo do Dia: Destaque de tempo livre, Concluídas, Pendentes
                HomeDaySummaryRow(
                    freeTimeText = TimeCalculationUtils.formatMinutes(uiState.todayAvailability.remainingFreeMinutes),
                    completedCount = uiState.completedCountToday,
                    pendingCount = uiState.pendingCountToday
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Card de Hidratação com contador e desfazer
                HydrationCard(
                    currentAmountMl = uiState.hydrationMl,
                    recordsCount = uiState.waterRecordsCount,
                    canUndo = uiState.canUndoWater,
                    onAddWater = { amount -> viewModel.recordWaterIntake(amount) },
                    onUndoWater = { viewModel.undoWater() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Card de Tempo de Uso de Celular (UsageStatsManager)
                ScreenUsageCard(
                    screenTimeMinutes = uiState.screenTimeMinutesToday,
                    isPermissionGranted = uiState.isUsagePermissionGranted,
                    onOpenSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Linha do Tempo Visual: Atividades de hoje (Fixas e Agendadas unificadas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Atividades de hoje",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.timelineItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${uiState.timelineItems.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Ver agenda completa",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToDestination(BottomNavDestination.Planning) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Listagem cronológica unificada de atividades de hoje
                if (uiState.timelineItems.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🌱", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma atividade planejada para hoje",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seu dia está livre. Toque no botão abaixo para agendar ou organizar sua rotina.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.timelineItems.forEachIndexed { index, item ->
                        when (item) {
                            is HomeTimelineItem.Scheduled -> {
                                val entry = item.entry
                                TodayScheduleItemCard(
                                    entry = entry,
                                    onStatusToggle = {
                                        if (entry.status == ScheduleStatus.COMPLETED) {
                                            viewModel.onTaskCheckboxClicked(entry)
                                        } else {
                                            showConfetti = true
                                            viewModel.completeTaskDirectly(entry)
                                        }
                                    },
                                    onEdit = {
                                        entry.activity?.let { act ->
                                            viewModel.openEditActivity(act)
                                        }
                                    },
                                    canMoveUp = index > 0,
                                    canMoveDown = index < uiState.timelineItems.size - 1,
                                    onMoveUp = { viewModel.moveEntryUp(entry) },
                                    onMoveDown = { viewModel.moveEntryDown(entry) },
                                    onFixedMoveAttempt = { viewModel.notifyFixedMoveAttempt() }
                                )
                            }
                            is HomeTimelineItem.Fixed -> {
                                FixedCommitmentAgendaCard(
                                    activity = item.activity,
                                    onClick = { viewModel.openEditActivity(item.activity) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão de Ação Rápida
                Button(
                    onClick = { onNavigateToDestination(BottomNavDestination.Planning) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Organizar dia com IA na Agenda",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Celebração de Confetes nativa ao concluir tarefa
        if (showConfetti) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize(),
                onAnimationEnd = { showConfetti = false }
            )
        }
    }

    // Modal de Planejamento de Atividade
    if (uiState.showScheduleSheet) {
        ScheduleActivityBottomSheet(
            activities = uiState.flexibleActivities,
            availability = uiState.todayAvailability,
            onDismissRequest = { viewModel.closeScheduleSheet() },
            onCreateNewActivity = {
                viewModel.closeScheduleSheet()
                viewModel.openAddActivity(ActivityType.FLEXIBLE)
            },
            onSaveNewActivity = { newActivity ->
                viewModel.saveActivity(newActivity)
            },
            onConfirmSchedule = { activity, startHour, startMinute, durationMinutes ->
                viewModel.scheduleActivityForToday(activity, startHour, startMinute, durationMinutes)
            }
        )
    }

    // Modal de Adicionar / Editar Atividade
    if (uiState.showAddSheet) {
        AddActivityBottomSheet(
            initialActivity = uiState.editingActivity,
            initialType = uiState.selectedTypeForAdd,
            onDismissRequest = { viewModel.closeAddActivity() },
            onSaveActivity = { activity -> viewModel.saveActivity(activity) },
            onSaveActivityWithContext = { activity, ctx -> viewModel.saveActivityWithContext(activity, ctx) }
        )
    }

    // Diálogo de Conclusão de Tarefa
    uiState.activeCompletionEntry?.let { entry ->
        CompletionPromptDialog(
            entry = entry,
            contextModel = uiState.activeCompletionContext,
            onDismissRequest = { viewModel.confirmCompletion(entry, null, false) },
            onConfirmCompleted = { actualDuration, advanceContext ->
                showConfetti = true
                viewModel.confirmCompletion(entry, actualDuration, advanceContext)
            },
            onStillDoing = { viewModel.markStillDoing(entry) },
            onMarkNotDone = { viewModel.markNotDone(entry) },
            onPostpone = { viewModel.openPostponeDialog(entry) }
        )
    }

    // Diálogo de Extensão de Tempo
    uiState.timeExtensionTargetEntry?.let { entry ->
        TimeExtensionDialog(
            entry = entry,
            nextEntry = uiState.nextUpcomingEntry,
            onDismiss = { viewModel.closeTimeExtension() },
            onConfirmExtension = { mins -> viewModel.confirmTimeExtension(entry, mins) }
        )
    }

    // Diálogo de Reagendamento
    uiState.activePostponeEntry?.let { entry ->
        PostponeEntryDialog(
            entry = entry,
            onDismissRequest = { viewModel.closePostponeDialog() },
            onPostponeTomorrow = { viewModel.postponeToTomorrow(entry) },
            onKeepPending = { viewModel.closePostponeDialog() },
            onCancelEntry = { viewModel.removeEntryFromDay(entry) }
        )
    }

    // Assistente Conversacional Viora
    if (uiState.showAssistantDialog) {
        VioraAssistantBottomSheet(
            messages = uiState.assistantMessages,
            onSendMessage = { prompt -> viewModel.sendAssistantMessage(prompt) },
            onDismissRequest = { viewModel.closeAssistant() }
        )
    }
}

/**
 * Card Principal da Home (Foco do Momento / Próxima Atividade)
 */
@Composable
private fun HomeMainFocusCard(
    activeEntry: ScheduleEntryModel?,
    nextEntry: ScheduleEntryModel?,
    remainingFreeFormatted: String,
    onComplete: (ScheduleEntryModel) -> Unit,
    onOrganizeWithAI: () -> Unit,
    onAddActivity: () -> Unit
) {
    val targetEntry = activeEntry ?: nextEntry
    if (targetEntry != null) {
        val isNow = activeEntry != null

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNow) SageLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isNow) PrimarySage else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNow) PrimarySage else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (isNow) "🌿 Em andamento agora" else "⏱️ Próxima atividade",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isNow) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = targetEntry.timeFormatted,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = targetEntry.activityTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val desc = targetEntry.activity?.description?.takeIf { it.isNotBlank() } ?: targetEntry.notes.takeIf { it.isNotBlank() }
                if (!desc.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${targetEntry.category.displayName} • Duração: ${targetEntry.durationFormatted}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onComplete(targetEntry) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySage)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Concluir atividade",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    } else {
        // Estado acolhedor quando não há nenhuma atividade agendada hoje
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🌿", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tudo livre por hoje",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Você tem $remainingFreeFormatted livres para descansar ou produzir no seu ritmo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOrganizeWithAI,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySage)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ir para Agenda", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    OutlinedButton(
                        onClick = onAddActivity,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrimarySage)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = PrimarySage, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agendar tarefa", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PrimarySage)
                    }
                }
            }
        }
    }
}

/**
 * Resumo do Dia em 3 cards elegantes
 */
@Composable
private fun HomeDaySummaryRow(
    freeTimeText: String,
    completedCount: Int,
    pendingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card Tempo Livre
        Surface(
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Tempo livre",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🌿 $freeTimeText",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDark
                )
            }
        }

        // Card Concluídas
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Concluídas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✅ $completedCount",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDark
                )
            }
        }

        // Card Pendentes
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Pendentes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "⏳ $pendingCount",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDark
                )
            }
        }
    }
}
