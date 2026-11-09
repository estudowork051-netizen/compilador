package com.lifeorganizer.app.feature.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.core.components.ActivityCard
import com.lifeorganizer.app.core.components.AddActivityBottomSheet
import com.lifeorganizer.app.core.components.EmptyStateView
import com.lifeorganizer.app.core.components.LifeFlowTextField
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val activitiesList by viewModel.activities.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddActivity() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Nova atividade",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header com suporte a Voltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Minhas Atividades",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            LifeFlowTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = "",
                placeholder = "Pesquisar por nome, categoria...",
                leadingIcon = Icons.Rounded.Search
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Type Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedTypeFilter == null,
                    onClick = { viewModel.setTypeFilter(null) },
                    label = { Text("Todos os tipos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                ActivityType.values().forEach { type ->
                    val isSelected = uiState.selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setTypeFilter(if (isSelected) null else type)
                        },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("Todas as categorias") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                ActivityCategory.ALL_CATEGORIES.forEach { cat ->
                    val isSelected = uiState.selectedCategoryFilter == cat.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setCategoryFilter(if (isSelected) null else cat.name)
                        },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activities List or Empty State
            if (activitiesList.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategoryFilter != null || uiState.selectedTypeFilter != null) {
                        "Nenhuma atividade encontrada"
                    } else {
                        "Nenhuma atividade cadastrada"
                    },
                    description = if (uiState.searchQuery.isNotBlank()) {
                        "Nenhum resultado corresponde à sua pesquisa. Tente outros termos ou crie uma nova atividade."
                    } else {
                        "Cadastre suas tarefas, compromissos fixos e metas para organizar seu dia com facilidade."
                    },
                    icon = Icons.AutoMirrored.Rounded.EventNote,
                    actionText = "Criar atividade",
                    onActionClick = { viewModel.openAddActivity() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = activitiesList,
                        key = { it.id }
                    ) { activity ->
                        ActivityCard(
                            activity = activity,
                            onClick = { viewModel.openEditActivity(activity) },
                            onEdit = { viewModel.openEditActivity(activity) },
                            onDelete = { viewModel.promptDeleteActivity(activity) },
                            onDuplicate = { viewModel.duplicateActivity(activity) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Add/Edit Activity
    if (uiState.showAddSheet) {
        AddActivityBottomSheet(
            initialActivity = uiState.editingActivity,
            onDismissRequest = { viewModel.closeAddActivity() },
            onSaveActivity = { viewModel.saveActivity(it) },
            onSaveActivityWithContext = { activity, context ->
                viewModel.saveActivityWithContext(activity, context)
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.activityToDelete != null) {
        val toDelete = uiState.activityToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Excluir atividade?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja excluir \"${toDelete.title}\"? Esta ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text(
                        text = "Excluir",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
