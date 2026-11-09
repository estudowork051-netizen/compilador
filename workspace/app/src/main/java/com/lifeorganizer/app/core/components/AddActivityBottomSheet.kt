package com.lifeorganizer.app.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import com.lifeorganizer.app.core.utils.ContextExtractor
import com.lifeorganizer.app.domain.model.ActivityContextModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.core.utils.DateTimeUtils
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.PredefinedActivity
import com.lifeorganizer.app.domain.model.RecurrenceType

import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddActivityBottomSheet(
    onDismissRequest: () -> Unit,
    onSaveActivity: (ActivityModel) -> Unit,
    onSaveActivityWithContext: ((ActivityModel, ActivityContextModel?) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialActivity: ActivityModel? = null,
    initialType: ActivityType = initialActivity?.type ?: ActivityType.FLEXIBLE
) {
    var title by remember { mutableStateOf(initialActivity?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialActivity?.category ?: "Casa") }
    var selectedType by remember { mutableStateOf(initialType) }
    var durationMinutes by remember { mutableIntStateOf(initialActivity?.estimatedDurationMinutes ?: 30) }
    var executionCount by remember { mutableIntStateOf(initialActivity?.defaultCount ?: 1) }
    var priority by remember { mutableStateOf(initialActivity?.priority ?: ActivityPriority.MEDIUM) }
    var description by remember { mutableStateOf(initialActivity?.description ?: "") }

    // Fixed commitment state
    var startHour by remember { mutableIntStateOf(initialActivity?.startHour ?: 8) }
    var startMinute by remember { mutableIntStateOf(initialActivity?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialActivity?.endHour ?: 10) }
    var endMinute by remember { mutableIntStateOf(initialActivity?.endMinute ?: 0) }
    var recurrenceType by remember { mutableStateOf(initialActivity?.recurrenceType ?: RecurrenceType.DAILY) }

    // Weekly goal state
    var weeklyTargetCount by remember { mutableIntStateOf(initialActivity?.weeklyTargetCount ?: 3) }

    // Custom duration dialog state
    var showCustomDurationDialog by remember { mutableStateOf(false) }

    // Search query for suggestions
    var searchQuery by remember { mutableStateOf("") }
    val suggestions = remember(title, searchQuery) {
        val q = if (searchQuery.isNotBlank()) searchQuery else title
        PredefinedActivity.search(q)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialActivity != null) "Editar Atividade" else "Nova Atividade",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Selector (Flexible / Fixed / Habit)
            Text(
                text = "Tipo de atividade",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityType.values().forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            LifeFlowTextField(
                value = title,
                onValueChange = { title = it },
                label = "Nome da atividade",
                placeholder = "Ex: Lavar louça, Estudar Kotlin...",
                leadingIcon = Icons.Rounded.Search
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description Field (Optional)
            LifeFlowTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descrição detalhada (opcional)",
                placeholder = "Ex: Organizar o guarda-roupa, trocar a roupa de cama e limpar a mesa",
                singleLine = false,
                supportingText = "Ajuda a especificar o que você pretende realizar nesta atividade"
            )

            // Suggestions Carousel / Chips
            if (suggestions.isNotEmpty() && title.length < 15) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Sugestões rápidas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.take(5).forEach { suggestion ->
                        Surface(
                            modifier = Modifier.clickable {
                                title = suggestion.title
                                selectedCategory = suggestion.category
                                durationMinutes = suggestion.suggestedDurationMinutes
                                selectedType = suggestion.type
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${suggestion.title} (${suggestion.suggestedDurationMinutes}m)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            val detectedContext = remember(title, description, selectedCategory) {
                ContextExtractor.extract(title, description, selectedCategory)
            }
            if (detectedContext != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Viora Context: ${detectedContext.contentName ?: ""} (${detectedContext.progressLabel ?: ""})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Acompanhamento inteligente de progresso ativo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selector
            Text(
                text = "Categoria",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityCategory.ALL_CATEGORIES.forEach { cat ->
                    val isSelected = selectedCategory == cat.name
                    CategoryChip(
                        label = cat.name,
                        selected = isSelected,
                        onSelectedChange = { selectedCategory = cat.name }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic fields based on ActivityType
            when (selectedType) {
                ActivityType.FLEXIBLE -> {
                    // DURATION SELECTOR
                    DurationSelectionSection(
                        durationMinutes = durationMinutes,
                        onDurationSelect = { durationMinutes = it },
                        onCustomClick = { showCustomDurationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // EXECUTION COUNT MULTIPLIER (Section 8)
                    ExecutionCountSection(
                        count = executionCount,
                        durationPerExecution = durationMinutes,
                        onCountChange = { executionCount = it }
                    )
                }

                ActivityType.FIXED -> {
                    // FIXED COMMITMENT TIME PICKERS & RECURRENCE (Section 9)
                    FixedCommitmentSection(
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        recurrenceType = recurrenceType,
                        onStartTimeChange = { h, m ->
                            startHour = h
                            startMinute = m
                        },
                        onEndTimeChange = { h, m ->
                            endHour = h
                            endMinute = m
                        },
                        onRecurrenceChange = { recurrenceType = it }
                    )
                }

                ActivityType.HABIT -> {
                    // WEEKLY GOAL FREQUENCY (Section 10)
                    WeeklyGoalSection(
                        targetTimesPerWeek = weeklyTargetCount,
                        durationPerSession = durationMinutes,
                        onTargetChange = { weeklyTargetCount = it },
                        onDurationChange = { durationMinutes = it },
                        onCustomDurationClick = { showCustomDurationDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val isFormValid = title.isNotBlank() && durationMinutes > 0
            LifeFlowPrimaryButton(
                text = if (initialActivity != null) "Salvar alterações" else "Cadastrar atividade",
                enabled = isFormValid,
                onClick = {
                    val activityToSave = ActivityModel(
                        id = initialActivity?.id ?: 0,
                        title = title.trim(),
                        category = selectedCategory,
                        estimatedDurationMinutes = durationMinutes,
                        type = selectedType,
                        defaultCount = if (selectedType == ActivityType.FLEXIBLE) executionCount else 1,
                        priority = priority,
                        colorHex = ActivityCategory.getColorHexForCategory(selectedCategory),
                        description = description.trim(),
                        startHour = if (selectedType == ActivityType.FIXED) startHour else null,
                        startMinute = if (selectedType == ActivityType.FIXED) startMinute else null,
                        endHour = if (selectedType == ActivityType.FIXED) endHour else null,
                        endMinute = if (selectedType == ActivityType.FIXED) endMinute else null,
                        recurrenceType = if (selectedType == ActivityType.FIXED) recurrenceType else null,
                        weeklyTargetCount = if (selectedType == ActivityType.HABIT) weeklyTargetCount else null
                    )
                    if (onSaveActivityWithContext != null) {
                        onSaveActivityWithContext(activityToSave, detectedContext)
                    } else {
                        onSaveActivity(activityToSave)
                    }
                }
            )
        }
    }

    // Custom Duration Dialog
    if (showCustomDurationDialog) {
        CustomDurationDialog(
            currentMinutes = durationMinutes,
            onDismiss = { showCustomDurationDialog = false },
            onConfirm = { mins ->
                durationMinutes = mins
                showCustomDurationDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationSelectionSection(
    durationMinutes: Int,
    onDurationSelect: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val quickDurations = listOf(
        Pair("15 min", 15),
        Pair("30 min", 30),
        Pair("45 min", 45),
        Pair("1h", 60),
        Pair("1h30", 90),
        Pair("2h", 120)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Duração estimada",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationMinutes.toLong()),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickDurations.forEach { (label, mins) ->
                val isSelected = durationMinutes == mins
                Surface(
                    modifier = Modifier.clickable { onDurationSelect(mins) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Custom Option
            Surface(
                modifier = Modifier.clickable(onClick = onCustomClick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Personalizar...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ExecutionCountSection(
    count: Int,
    durationPerExecution: Int,
    onCountChange: (Int) -> Unit
) {
    val totalMinutes = durationPerExecution * count
    val totalFormatted = TimeCalculationUtils.formatMinutesToHoursAndMinutes(totalMinutes.toLong())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Execuções da atividade",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count x ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationPerExecution.toLong())} = $totalFormatted total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (count > 1) onCountChange(count - 1) },
                    enabled = count > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Diminuir execuções")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = { if (count < 10) onCountChange(count + 1) },
                    enabled = count < 10,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Aumentar execuções")
                }
            }
        }
    }
}

@Composable
private fun FixedCommitmentSection(
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    recurrenceType: RecurrenceType,
    onStartTimeChange: (Int, Int) -> Unit,
    onEndTimeChange: (Int, Int) -> Unit,
    onRecurrenceChange: (RecurrenceType) -> Unit
) {
    val startStr = DateTimeUtils.formatTime(startHour, startMinute)
    val endStr = DateTimeUtils.formatTime(endHour, endMinute)

    // Calculate duration considering midnight crossing
    val startMin = startHour * 60 + startMinute
    val endMin = endHour * 60 + endMinute
    val crossesMidnight = endMin <= startMin
    val durationMin = if (crossesMidnight) (24 * 60 - startMin) + endMin else endMin - startMin
    val durationStr = TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationMin.toLong())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimePickerField(
                label = "Início",
                timeValue = startStr,
                onTimeChange = {
                    val p = DateTimeUtils.parseTime(it)
                    onStartTimeChange(p.hour, p.minute)
                },
                modifier = Modifier.weight(1f)
            )

            TimePickerField(
                label = "Término",
                timeValue = endStr,
                onTimeChange = {
                    val p = DateTimeUtils.parseTime(it)
                    onEndTimeChange(p.hour, p.minute)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Badge indicating duration and midnight crossing
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duração calculada: $durationStr",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (crossesMidnight) {
                    Text(
                        text = "+1 dia (atravessa meia-noite)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recurrence Selector
        Text(
            text = "Recorrência",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RecurrenceType.values().forEach { rec ->
                val isSelected = recurrenceType == rec
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecurrenceChange(rec) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = rec.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalSection(
    targetTimesPerWeek: Int,
    durationPerSession: Int,
    onTargetChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onCustomDurationClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meta semanal",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$targetTimesPerWeek vezes por semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (targetTimesPerWeek > 1) onTargetChange(targetTimesPerWeek - 1) },
                        enabled = targetTimesPerWeek > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Diminuir meta")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "$targetTimesPerWeek",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (targetTimesPerWeek < 7) onTargetChange(targetTimesPerWeek + 1) },
                        enabled = targetTimesPerWeek < 7,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Aumentar meta")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DurationSelectionSection(
            durationMinutes = durationPerSession,
            onDurationSelect = onDurationChange,
            onCustomClick = onCustomDurationClick
        )
    }
}

@Composable
private fun CustomDurationDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(currentMinutes / 60) }
    var minutes by remember { mutableIntStateOf(currentMinutes % 60) }

    TimeSelectionDialog(
        initialHour = hours,
        initialMinute = minutes,
        title = "Definir duração personalizada",
        onDismiss = onDismiss,
        onConfirm = { h, m ->
            val total = h * 60 + m
            if (total > 0) {
                onConfirm(total)
            } else {
                onConfirm(15) // Fallback minimum 15 min
            }
        }
    )
}
