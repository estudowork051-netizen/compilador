package com.lifeorganizer.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeorganizer.app.core.components.AddActivityBottomSheet
import com.lifeorganizer.app.core.components.BreatheModal
import com.lifeorganizer.app.core.components.CompletionPromptDialog
import com.lifeorganizer.app.core.components.ConfettiEffect
import com.lifeorganizer.app.core.components.EnvironmentPeriod
import com.lifeorganizer.app.core.components.FocusSessionCard
import com.lifeorganizer.app.core.components.HydrationCard
import com.lifeorganizer.app.core.components.MoodBar
import com.lifeorganizer.app.core.components.MoodType
import com.lifeorganizer.app.core.components.NatureHeroCard
import com.lifeorganizer.app.core.components.OrganizeDayShimmerButton
import com.lifeorganizer.app.core.components.PostponeEntryDialog
import com.lifeorganizer.app.core.components.ScheduleActivityBottomSheet
import com.lifeorganizer.app.core.components.SmartRecommendationCard
import com.lifeorganizer.app.core.components.TimeExtensionDialog
import com.lifeorganizer.app.core.components.TodayScheduleItemCard
import com.lifeorganizer.app.core.navigation.BottomNavDestination
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ScheduleStatus
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDestination: (BottomNavDestination) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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

    val currentHour = LocalTime.now().hour
    val greetingIcon = when (currentHour) {
        in 5..11 -> "☀️"
        in 12..17 -> "🌿"
        else -> "🌙"
    }

    val greetingHeader = if (uiState.userName.isNotBlank()) {
        "${uiState.greeting}, ${uiState.userName} $greetingIcon"
    } else {
        "${uiState.greeting} $greetingIcon"
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

                // 1. Saudação acolhedora
                Column {
                    Text(
                        text = greetingHeader,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Seu dia está tomando forma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Seletor de ambiente (3 chips com bordas arredondadas e ícone)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnvironmentPeriod.values().forEach { period ->
                        val isSelected = uiState.selectedEnvironment == period
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setEnvironmentPeriod(period) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${period.icon} ${period.title}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Hero Card principal
                NatureHeroCard(
                    currentPeriod = uiState.selectedEnvironment,
                    activeEntry = uiState.activeNowEntry,
                    nextUpcomingEntry = uiState.nextUpcomingEntry,
                    remainingFreeFormatted = TimeCalculationUtils.formatMinutes(uiState.todayAvailability.remainingFreeMinutes),
                    onBreatheClick = { viewModel.openBreatheModal() }
                )

                // Active Focus Session Card (se houver sessão em andamento)
                uiState.activeFocusSession?.let { focusSession ->
                    if (focusSession.isActive) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FocusSessionCard(
                            session = focusSession,
                            onPause = { viewModel.pauseFocus() },
                            onResume = { viewModel.resumeFocus() },
                            onExtend = { minutes -> viewModel.extendFocus(minutes) },
                            onConclude = { viewModel.concludeFocus() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Mood Bar ("Como você está agora?")
                MoodBar(
                    selectedMood = uiState.currentMood,
                    onMoodSelected = { mood -> viewModel.setMood(mood) }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 5. Seção "Planejados de hoje"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Planejados de hoje",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.todayScheduledEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${uiState.todayScheduledEntries.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (uiState.todayScheduledEntries.isNotEmpty()) {
                        Text(
                            text = "Ver agenda",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToDestination(BottomNavDestination.Planning) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.todayScheduledEntries.isEmpty()) {
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
                                text = "Seu dia está livre. Toque em 'Organizar meu dia com IA' para planejar seus blocos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.todayScheduledEntries.forEachIndexed { index, entry ->
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
                            canMoveDown = index < uiState.todayScheduledEntries.size - 1,
                            onMoveUp = { viewModel.moveEntryUp(entry) },
                            onMoveDown = { viewModel.moveEntryDown(entry) },
                            onFixedMoveAttempt = { viewModel.notifyFixedMoveAttempt() }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Sugestão inteligente (LifeFlow Context)
                uiState.currentRecommendation?.let { recommendation ->
                    Spacer(modifier = Modifier.height(18.dp))
                    SmartRecommendationCard(
                        recommendation = recommendation,
                        showExplanation = uiState.showExplanations,
                        onAccept = { viewModel.acceptRecommendation(recommendation) },
                        onDismiss = { viewModel.dismissRecommendation(recommendation) },
                        onNotInterested = { viewModel.notInterestedRecommendation(recommendation) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 6. Tracker de hidratação rápido
                HydrationCard(
                    currentAmountMl = uiState.hydrationMl,
                    onAddWater = { amount -> viewModel.addWater(amount) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 7. Botão principal com shimmer "Organizar meu dia com IA"
                OrganizeDayShimmerButton(
                    onClick = { viewModel.openScheduleSheet() },
                    text = "✨ Organizar meu dia com IA"
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Celebração de Confetes nativa
        if (showConfetti) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize(),
                onAnimationEnd = { showConfetti = false }
            )
        }
    }

    // Modal de Respiração Guiada (B4.3)
    if (uiState.showBreatheModal) {
        BreatheModal(
            onDismissRequest = { viewModel.closeBreatheModal() }
        )
    }

    // Modal de Planejamento com 3 abas e criação integrada (B10)
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

    // Diálogo de Extensão de Tempo ("Ainda estou fazendo")
    uiState.timeExtensionTargetEntry?.let { entry ->
        TimeExtensionDialog(
            entry = entry,
            nextEntry = uiState.nextUpcomingEntry,
            onDismiss = { viewModel.closeTimeExtension() },
            onConfirmExtension = { mins -> viewModel.confirmTimeExtension(entry, mins) }
        )
    }

    // Diálogo de Reagendamento Amigável
    uiState.activePostponeEntry?.let { entry ->
        PostponeEntryDialog(
            entry = entry,
            onDismissRequest = { viewModel.closePostponeDialog() },
            onPostponeTomorrow = { viewModel.postponeToTomorrow(entry) },
            onKeepPending = { viewModel.closePostponeDialog() },
            onCancelEntry = { viewModel.removeEntryFromDay(entry) }
        )
    }
}
