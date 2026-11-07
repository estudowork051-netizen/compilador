package com.lifeorganizer.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeorganizer.app.core.utils.TimeAvailabilityEngine
import com.lifeorganizer.app.core.utils.TimePreferenceAnalyzer
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.PredefinedActivity
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime

data class ContextualActivitySuggestion(
    val activity: ActivityModel,
    val reason: String,
    val isFromHistory: Boolean = false
)

data class FreeSlotOption(
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val isRecommended: Boolean,
    val reason: String
) {
    val startTimeFormatted: String get() = "%02d:%02d".format(startHour, startMinute)
    val endHour: Int get() = ((startHour * 60 + startMinute + durationMinutes) / 60) % 24
    val endMinute: Int get() = (startHour * 60 + startMinute + durationMinutes) % 60
    val rangeFormatted: String get() = "%02d:%02d–%02d:%02d".format(startHour, startMinute, endHour, endMinute)
}

/**
 * Bottom Sheet for scheduling an activity into a specific time slot on a date.
 * Features clear separation between "Minhas atividades" and "Sugestões rápidas",
 * with in-flow "+ Criar nova atividade" button and real-time conflict checking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleActivityBottomSheet(
    activities: List<ActivityModel>,
    availability: DayAvailability,
    initialSlot: TimeSlot? = null,
    initialActivity: ActivityModel? = null,
    executionRecords: List<ExecutionRecordModel> = emptyList(),
    balancePreference: BalancePreference = BalancePreference.BALANCED,
    onDismissRequest: () -> Unit,
    onCreateNewActivity: (() -> Unit)? = null,
    onSaveNewActivity: ((ActivityModel) -> Unit)? = null,
    onConfirmSchedule: (activity: ActivityModel, startHour: Int, startMinute: Int, durationMinutes: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Predefined suggestions converted to transient ActivityModel
    val predefinedActivities = remember {
        PredefinedActivity.LIBRARY.map { p ->
            ActivityModel(
                id = 0L,
                title = p.title,
                category = p.category,
                type = p.type,
                estimatedDurationMinutes = p.suggestedDurationMinutes
            )
        }
    }

    // Contextual activity suggestions based on profile, history and pending activities (Section 8)
    val contextualSuggestions = remember(activities, executionRecords, balancePreference) {
        val list = mutableListOf<ContextualActivitySuggestion>()

        // 1. Frequent activities from execution records
        val countsByAct = executionRecords
            .filter { it.finalStatus == ScheduleStatus.COMPLETED }
            .groupingBy { it.activityId }
            .eachCount()

        val frequentActs = activities
            .filter { (countsByAct[it.id] ?: 0) >= 2 }
            .sortedByDescending { countsByAct[it.id] ?: 0 }

        for (act in frequentActs) {
            val count = countsByAct[act.id] ?: 0
            list.add(
                ContextualActivitySuggestion(
                    activity = act,
                    reason = "Concluída $count vezes recentemente",
                    isFromHistory = true
                )
            )
        }

        // 2. Profile-based matching
        when (balancePreference) {
            BalancePreference.STUDY_FOCUS -> {
                val studies = activities.filter {
                    it.category.equals("Estudos", ignoreCase = true) || it.category.equals("Trabalho", ignoreCase = true)
                }.filter { act -> list.none { it.activity.id == act.id } }
                for (act in studies) {
                    list.add(
                        ContextualActivitySuggestion(
                            activity = act,
                            reason = "Alinhada ao seu foco em estudos",
                            isFromHistory = false
                        )
                    )
                }
            }
            BalancePreference.REST_FOCUS -> {
                val restOrHealth = activities.filter {
                    it.category.equals("Lazer", ignoreCase = true) || it.category.equals("Saúde", ignoreCase = true)
                }.filter { act -> list.none { it.activity.id == act.id } }
                for (act in restOrHealth) {
                    list.add(
                        ContextualActivitySuggestion(
                            activity = act,
                            reason = "Momento leve para descanso",
                            isFromHistory = false
                        )
                    )
                }
            }
            BalancePreference.BALANCED -> {
                val balancedActs = activities.filter { act -> list.none { it.activity.id == act.id } }
                for (act in balancedActs.take(3)) {
                    list.add(
                        ContextualActivitySuggestion(
                            activity = act,
                            reason = "Equilíbrio na rotina",
                            isFromHistory = false
                        )
                    )
                }
            }
            else -> {}
        }

        // Fallback: If list is empty (e.g. no custom activities yet), use curated predefined library templates
        if (list.isEmpty()) {
            val fallbacks = when (balancePreference) {
                BalancePreference.STUDY_FOCUS -> predefinedActivities.filter { it.category == "Estudos" || it.title.contains("Estud") }
                BalancePreference.REST_FOCUS -> predefinedActivities.filter { it.category == "Lazer" || it.category == "Saúde" }
                else -> predefinedActivities.take(3)
            }
            for (fb in fallbacks.take(3)) {
                list.add(
                    ContextualActivitySuggestion(
                        activity = fb,
                        reason = "Sugestão para seu perfil (${balancePreference.displayName})",
                        isFromHistory = false
                    )
                )
            }
        }

        list.distinctBy { it.activity.title }.take(5)
    }

    var selectedTabIndex by remember { mutableIntStateOf(if (initialActivity != null || activities.isNotEmpty()) 0 else 1) }

    var selectedActivity by remember {
        mutableStateOf(initialActivity ?: (activities.firstOrNull() ?: predefinedActivities.firstOrNull()))
    }

    var showManualTimePicker by remember { mutableStateOf(false) }
    var showAllSlots by remember { mutableStateOf(false) }

    val defaultStartHour = initialSlot?.let { it.startMinutes / 60 } ?: 9
    val defaultStartMinute = initialSlot?.let { it.startMinutes % 60 } ?: 0
    val defaultDuration = initialSlot?.durationMinutes?.takeIf { it > 0 } ?: (selectedActivity?.estimatedDurationMinutes ?: 45)

    var startHour by remember { mutableIntStateOf(defaultStartHour) }
    var startMinute by remember { mutableIntStateOf(defaultStartMinute) }
    var durationMinutes by remember { mutableIntStateOf(defaultDuration) }

    val presetDurations = listOf(15, 30, 45, 60, 90, 120)

    // State for Tab 2 "+ Criar Nova" in-place creation form (FASE B10)
    var newTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Estudos") }
    var newDuration by remember { mutableIntStateOf(30) }
    var newPriority by remember { mutableStateOf(ActivityPriority.MEDIUM) }
    var newDescription by remember { mutableStateOf("") }

    var pastTimeError by remember { mutableStateOf<String?>(null) }

    // Calculate free time slot suggestions based on durationMinutes, availability and history
    val smartSlotOptions = remember(selectedActivity, durationMinutes, availability, executionRecords) {
        val now = LocalTime.now()
        val isToday = try {
            availability.date.isBlank() || LocalDate.parse(availability.date) == LocalDate.now()
        } catch (e: Exception) {
            true
        }
        val currentMinutesNow = if (isToday) now.hour * 60 + now.minute else 0
        val minAllowedStart = if (isToday) ((currentMinutesNow + 4) / 5) * 5 else 0

        val historicalPreferences = if (selectedActivity != null && executionRecords.isNotEmpty()) {
            TimePreferenceAnalyzer.analyzeActivityTimeSlots(
                activityId = selectedActivity!!.id,
                activityTitle = selectedActivity!!.title,
                records = executionRecords
            ).filter { pref ->
                // Rule: history must NEVER recommend past time or expired slot for today!
                if (isToday) {
                    val prefEndMin = if (pref.endHour == 0) 24 * 60 else pref.endHour * 60
                    prefEndMin > minAllowedStart
                } else true
            }
        } else emptyList()

        val options = mutableListOf<FreeSlotOption>()
        for (slot in availability.freeSlots) {
            val effStart = if (isToday) maxOf(slot.startMinutes, minAllowedStart) else slot.startMinutes
            val gap = slot.endMinutes - effStart
            if (gap >= durationMinutes) {
                val sH = (effStart / 60) % 24
                val sM = effStart % 60

                val pref = historicalPreferences.firstOrNull { p ->
                    val prefStart = p.startHour * 60
                    val prefEnd = if (p.endHour == 0) 24 * 60 else p.endHour * 60
                    effStart in prefStart until prefEnd &&
                            p.completionRate >= 0.6f && p.confidence != ConfidenceLevel.INSUFFICIENT_DATA
                }

                options.add(
                    FreeSlotOption(
                        startHour = sH,
                        startMinute = sM,
                        durationMinutes = durationMinutes,
                        isRecommended = pref != null,
                        reason = pref?.let { "Você costuma concluir suas tarefas com mais frequência nesse horário." }
                            ?: "Horário sugerido com base na sua disponibilidade."
                    )
                )

                if (gap >= durationMinutes + 60) {
                    val secondStart = effStart + 60
                    val sH2 = (secondStart / 60) % 24
                    val sM2 = secondStart % 60
                    val pref2 = historicalPreferences.firstOrNull { p ->
                        val prefStart2 = p.startHour * 60
                        val prefEnd2 = if (p.endHour == 0) 24 * 60 else p.endHour * 60
                        secondStart in prefStart2 until prefEnd2 &&
                                p.completionRate >= 0.6f && p.confidence != ConfidenceLevel.INSUFFICIENT_DATA
                    }
                    options.add(
                        FreeSlotOption(
                            startHour = sH2,
                            startMinute = sM2,
                            durationMinutes = durationMinutes,
                            isRecommended = pref2 != null,
                            reason = pref2?.let { "Você costuma concluir suas tarefas com mais frequência nesse horário." }
                                ?: "Horário sugerido com base na sua disponibilidade."
                        )
                    )
                }
            }
        }
        options.sortedByDescending { it.isRecommended }
    }

    // Calculate current proposed slot
    val proposedStartTotal = startHour * 60 + startMinute
    val proposedEndTotal = proposedStartTotal + durationMinutes
    val proposedSlot = remember(startHour, startMinute, durationMinutes) {
        TimeSlot(proposedStartTotal, proposedEndTotal, selectedActivity?.title ?: "Nova atividade")
    }

    val isToday = remember(availability.date) {
        try {
            availability.date.isBlank() || LocalDate.parse(availability.date) == LocalDate.now()
        } catch (e: Exception) {
            true
        }
    }

    // Check conflict against existing fixed, scheduled occupied slots, and past times for today
    val conflictWith = remember(proposedSlot, availability, startHour, startMinute, isToday) {
        val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
        if (isToday && (startHour * 60 + startMinute) < nowMin) {
            "Esse horário já passou. Por favor, escolha um horário a partir de agora."
        } else {
            val awakeWindow = TimeAvailabilityEngine.getAwakeWindow(availability.wakeUpTime, availability.sleepTime)
            val clamped = TimeAvailabilityEngine.clampToAwakeWindow(proposedSlot, awakeWindow)
            if (clamped == null) {
                "O horário escolhido fica fora da sua vigília (${availability.wakeUpTime} às ${availability.sleepTime})."
            } else {
                val directConflict = availability.conflicts.firstOrNull { it.overlapSlot.overlaps(proposedSlot) }
                directConflict?.message
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agendar no dia",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Seção Contextual: Sugestões para o seu dia (Seção 8)
            if (contextualSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sugestões para o seu dia (${balancePreference.displayName}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(contextualSuggestions) { sug ->
                        val isSelected = selectedActivity?.title == sug.activity.title
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clickable {
                                selectedActivity = sug.activity
                                durationMinutes = sug.activity.estimatedDurationMinutes
                                selectedTabIndex = if (sug.activity.id != 0L) 0 else 1
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = sug.activity.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = sug.reason,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Seção 6: Abas Claras "Minhas atividades" vs "Sugestões rápidas" vs "+ Criar Nova" (FASE B10)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        if (selectedActivity == null || selectedActivity?.id == 0L) {
                            activities.firstOrNull()?.let {
                                selectedActivity = it
                                durationMinutes = it.estimatedDurationMinutes
                            }
                        }
                    },
                    text = {
                        Text(
                            text = "Minhas atividades (${activities.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        if (selectedActivity == null || selectedActivity?.id != 0L) {
                            predefinedActivities.firstOrNull()?.let {
                                selectedActivity = it
                                durationMinutes = it.estimatedDurationMinutes
                            }
                        }
                    },
                    text = {
                        Text(
                            text = "Sugestões rápidas",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "+ Criar Nova",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Exibição da lista conforme a aba selecionada
            if (selectedTabIndex == 0) {
                // Aba: Minhas Atividades
                if (activities.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Você ainda não criou atividades personalizadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedTabIndex = 2
                                onCreateNewActivity?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Criar nova atividade")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Escolha sua atividade:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onCreateNewActivity != null) {
                            Text(
                                text = "+ Nova atividade",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onCreateNewActivity() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activities) { activity ->
                            val isSelected = activity.id == selectedActivity?.id && selectedActivity?.id != 0L
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.clickable {
                                    selectedActivity = activity
                                    durationMinutes = activity.estimatedDurationMinutes
                                }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = activity.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activity.category} • ${activity.durationFormatted}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 1) {
                // Aba: Sugestões Rápidas
                Text(
                    text = "Sugestões da biblioteca:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(predefinedActivities) { p ->
                        val isSelected = selectedActivity?.title == p.title && selectedActivity?.id == 0L
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.clickable {
                                selectedActivity = p
                                durationMinutes = p.estimatedDurationMinutes
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${p.category} • ${p.durationFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 2) {
                // Aba: + Criar Nova (FASE B10 - Formulário funcional)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Criar nova atividade:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Nome da atividade (ex: Estudar Java)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Categoria:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Estudos", "Trabalho", "Casa", "Lazer", "Saúde", "Outros")) { cat ->
                                FilterChip(
                                    selected = newCategory == cat,
                                    onClick = { newCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Duração estimada:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(15, 30, 45, 60, 90)) { dur ->
                                FilterChip(
                                    selected = newDuration == dur,
                                    onClick = { newDuration = dur },
                                    label = { Text("${dur}m", fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    val created = ActivityModel(
                                        id = System.currentTimeMillis(),
                                        title = newTitle.trim(),
                                        category = newCategory,
                                        estimatedDurationMinutes = newDuration,
                                        priority = newPriority,
                                        description = newDescription.trim(),
                                        type = ActivityType.FLEXIBLE
                                    )
                                    onSaveNewActivity?.invoke(created)
                                    onCreateNewActivity?.invoke()
                                    selectedActivity = created
                                    durationMinutes = newDuration
                                    selectedTabIndex = 0
                                }
                            },
                            enabled = newTitle.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar e Escolher Horário")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Seção Inteligente: Horários Livres Disponíveis e Histórico
            Text(
                text = "Quando você quer fazer essa atividade?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Encontramos horários livres calculados para você hoje:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (smartSlotOptions.isNotEmpty()) {
                val displayedSlots = if (showAllSlots) smartSlotOptions else smartSlotOptions.take(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedSlots.forEach { opt ->
                        val isSelected = startHour == opt.startHour && startMinute == opt.startMinute
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    startHour = opt.startHour
                                    startMinute = opt.startMinute
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = opt.rangeFormatted,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (opt.isRecommended) Color(0xFF10B981).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = if (opt.isRecommended) "⭐ Recomendado" else "Livre",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (opt.isRecommended) Color(0xFF047857) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = opt.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selecionado",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (smartSlotOptions.size > 3) {
                        Text(
                            text = if (showAllSlots) "Ver menos horários" else "Ver mais horários livres (${smartSlotOptions.size - 3})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable { showAllSlots = !showAllSlots }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Não encontramos intervalos livres contínuos de ${durationMinutes} min. Você pode ajustar a duração abaixo ou escolher um horário manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão para expandir ajuste manual se o usuário quiser escolher outro horário
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showManualTimePicker = !showManualTimePicker },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showManualTimePicker) "Ocultar ajuste manual de horário" else "Escolher outro horário manualmente",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (showManualTimePicker) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Seletor de Hora
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Hora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    startHour = if (startHour > 0) startHour - 1 else 23
                                }
                            ) {
                                Text("-", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "%02d".format(startHour),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    startHour = if (startHour < 23) startHour + 1 else 0
                                }
                            ) {
                                Text("+", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Seletor de Minuto
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Minuto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    startMinute = if (startMinute >= 5) startMinute - 5 else 55
                                }
                            ) {
                                Text("-", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "%02d".format(startMinute),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    startMinute = if (startMinute < 55) startMinute + 5 else 0
                                }
                            ) {
                                Text("+", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Duração da Atividade
            Text(
                text = "Duração (${durationMinutes} min)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetDurations) { preset ->
                    val isSelected = durationMinutes == preset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.clickable { durationMinutes = preset }
                    ) {
                        Text(
                            text = "${preset}m",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horário Calculado
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Horário previsto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "%02d:%02d até %02d:%02d".format(
                                startHour,
                                startMinute,
                                (proposedEndTotal / 60) % 24,
                                proposedEndTotal % 60
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${durationMinutes} min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Aviso de Conflito ou Horário Passado
            val effectiveErrorMessage = conflictWith ?: pastTimeError
            if (effectiveErrorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = effectiveErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão Confirmar com revalidação temporal antes de salvar
            Button(
                onClick = {
                    val currentNow = LocalTime.now()
                    val curMin = currentNow.hour * 60 + currentNow.minute
                    val isPast = isToday && ((startHour * 60 + startMinute) < curMin)
                    if (isPast) {
                        pastTimeError = "Esse horário já passou. Por favor, escolha um horário a partir de agora."
                    } else {
                        pastTimeError = null
                        selectedActivity?.let { activity ->
                            onConfirmSchedule(activity, startHour, startMinute, durationMinutes)
                        }
                    }
                },
                enabled = selectedActivity != null && conflictWith == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar agendamento",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
