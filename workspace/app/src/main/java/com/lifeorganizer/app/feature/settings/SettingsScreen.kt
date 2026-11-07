package com.lifeorganizer.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.core.components.LifeFlowCard
import com.lifeorganizer.app.core.components.LifeFlowTextField
import com.lifeorganizer.app.core.components.TimePickerField
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.OrganizationMode
import com.lifeorganizer.app.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    var editingName by remember(uiState.name) { mutableStateOf(uiState.name) }
    var editingWakeUp by remember(uiState.wakeUpTime) { mutableStateOf(uiState.wakeUpTime) }
    var editingSleep by remember(uiState.sleepTime) { mutableStateOf(uiState.sleepTime) }

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

            Text(
                text = "Perfil e Configurações",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. DADOS PESSOAIS
            SectionTitle(title = "Dados pessoais", icon = Icons.Rounded.Person)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                LifeFlowTextField(
                    value = editingName,
                    onValueChange = {
                        editingName = it
                        viewModel.updateName(it)
                    },
                    label = "Nome ou apelido",
                    placeholder = "Como deseja ser chamado?"
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePickerField(
                    label = "Horário de acordar",
                    timeValue = editingWakeUp,
                    onTimeChange = {
                        editingWakeUp = it
                        viewModel.updateWakeUpTime(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePickerField(
                    label = "Horário de dormir",
                    timeValue = editingSleep,
                    onTimeChange = {
                        editingSleep = it
                        viewModel.updateSleepTime(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. INTELIGÊNCIA E PERSONALIZAÇÃO (Etapa 5 / Seção 14)
            SectionTitle(title = "Inteligência e personalização", icon = Icons.Rounded.Psychology)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                SettingSwitchRow(
                    title = "Recomendações personalizadas",
                    subtitle = "Sugestões proativas adaptadas aos seus horários e necessidades",
                    checked = uiState.personalizedRecommendationsEnabled,
                    onCheckedChange = { viewModel.updatePersonalizedRecommendations(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    title = "Aprender com o histórico",
                    subtitle = "Identifica durações habituais e momentos de maior taxa de conclusão",
                    checked = uiState.learningEnabled,
                    onCheckedChange = { viewModel.updateLearningEnabled(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    title = "Exibir justificativas das sugestões",
                    subtitle = "Apresenta os motivos e evidências reais que geraram cada recomendação",
                    checked = uiState.showExplanationsEnabled,
                    onCheckedChange = { viewModel.updateShowExplanations(it) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = { viewModel.openRestoreDurationsDialog() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar durações sugeridas padrão")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.openClearLearningDialog() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpar dados de aprendizado")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. EQUILÍBRIO E LAZER
            SectionTitle(title = "Equilíbrio e lazer", icon = Icons.Rounded.Balance)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                ThemeRadioOption(
                    title = "Equilibrado (Recomendado)",
                    subtitle = "Alterna harmoniosamente entre foco, responsabilidades e lazer",
                    icon = Icons.Rounded.Balance,
                    selected = uiState.balancePreference == BalancePreference.BALANCED,
                    onClick = { viewModel.updateBalancePreference(BalancePreference.BALANCED) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Foco em Estudos e Trabalho",
                    subtitle = "Prioriza atividades produtivas e módulos de aprendizado",
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    selected = uiState.balancePreference == BalancePreference.STUDY_FOCUS,
                    onClick = { viewModel.updateBalancePreference(BalancePreference.STUDY_FOCUS) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Foco em Descanso e Lazer",
                    subtitle = "Prioriza relaxamento, séries, leituras e bem-estar",
                    icon = Icons.Rounded.SelfImprovement,
                    selected = uiState.balancePreference == BalancePreference.REST_FOCUS,
                    onClick = { viewModel.updateBalancePreference(BalancePreference.REST_FOCUS) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Sem preferência",
                    subtitle = "Distribui atividades seguindo apenas a disponibilidade livre",
                    icon = Icons.Rounded.Tune,
                    selected = uiState.balancePreference == BalancePreference.NO_PREFERENCE,
                    onClick = { viewModel.updateBalancePreference(BalancePreference.NO_PREFERENCE) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. APARÊNCIA
            SectionTitle(title = "Aparência", icon = Icons.Rounded.Palette)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                ThemeRadioOption(
                    title = "Seguir configuração do dispositivo",
                    subtitle = "Adapta-se automaticamente ao sistema",
                    icon = Icons.Rounded.PhoneAndroid,
                    selected = uiState.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Tema Claro",
                    subtitle = "Fundo claro e alto contraste",
                    icon = Icons.Rounded.LightMode,
                    selected = uiState.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Tema Escuro",
                    subtitle = "Fundo escuro, ideal para a noite",
                    icon = Icons.Rounded.DarkMode,
                    selected = uiState.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.updateThemeMode(ThemeMode.DARK) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. ORGANIZAÇÃO
            SectionTitle(title = "Preferência de organização", icon = Icons.Rounded.Tune)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                ThemeRadioOption(
                    title = "Modo Manual",
                    subtitle = "Você decide horários e atividades com sugestões",
                    icon = Icons.Rounded.Handyman,
                    selected = uiState.organizationMode == OrganizationMode.MANUAL,
                    onClick = { viewModel.updateOrganizationMode(OrganizationMode.MANUAL) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeRadioOption(
                    title = "Modo Automático",
                    subtitle = "Propostas automáticas para sua rotina com aprendizado contínuo",
                    icon = Icons.Rounded.AutoMode,
                    selected = uiState.organizationMode == OrganizationMode.AUTOMATIC,
                    onClick = { viewModel.updateOrganizationMode(OrganizationMode.AUTOMATIC) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. LEMBRETES E TRANQUILIDADE (Etapa 6)
            SectionTitle(title = "Lembretes e tranquilidade", icon = Icons.Rounded.Tune)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                SettingSwitchRow(
                    title = "Lembretes do LifeFlow",
                    subtitle = "Avisos suaves e acompanhamento da sua rotina",
                    checked = uiState.remindersEnabled,
                    onCheckedChange = { viewModel.updateRemindersEnabled(it) }
                )

                if (uiState.remindersEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchRow(
                        title = "Avisar antes do início",
                        subtitle = "Notificar ${uiState.notifyBeforeMinutes} minutos antes da atividade começar",
                        checked = uiState.notifyBeforeStart,
                        onCheckedChange = { viewModel.updateNotifyBeforeStart(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchRow(
                        title = "Perguntar sobre conclusão",
                        subtitle = "Ao final do horário previsto, confirmar se terminou ou precisa de mais tempo",
                        checked = uiState.promptCompletionOnEnd,
                        onCheckedChange = { viewModel.updatePromptCompletionOnEnd(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchRow(
                        title = "Sugestões gentis de reorganização",
                        subtitle = "Dicas suaves quando uma tarefa for adiada repetidamente",
                        checked = uiState.reorganizeSuggestionsEnabled,
                        onCheckedChange = { viewModel.updateReorganizeSuggestionsEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchRow(
                        title = "Horário de silêncio",
                        subtitle = "Não emitir alertas entre ${uiState.quietHoursStart} e ${uiState.quietHoursEnd}",
                        checked = uiState.quietHoursEnabled,
                        onCheckedChange = { viewModel.updateQuietHoursEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. MONITORAMENTO DO USO DO CELULAR (Etapa 6 - Opcional e Consentido)
            SectionTitle(title = "Uso do celular e foco (Opcional)", icon = Icons.Rounded.PhoneAndroid)
            Spacer(modifier = Modifier.height(10.dp))

            LifeFlowCard {
                SettingSwitchRow(
                    title = "Acompanhamento consciente de tela",
                    subtitle = "Identifica tempo em aplicativos durante blocos para ajudar a manter o equilíbrio",
                    checked = uiState.usageMonitoringConsentGranted,
                    onCheckedChange = { viewModel.updateUsageMonitoringConsent(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Privacidade garantida",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "O LifeFlow processa apenas a duração total por aplicativo, sem acessar dados pessoais, mensagens ou conteúdo. Nenhuma informação sai do seu telefone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. INFORMAÇÕES DO APLICATIVO
            SectionTitle(title = "Sobre o aplicativo", icon = Icons.Rounded.Storage)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = uiState.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Versão: ${uiState.appVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Processamento Local e Offline: Todas as análises de hábitos, durações e preferências são realizadas no próprio dispositivo, sem envio para servidores externos ou uso de IA de terceiros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Diálogo de confirmação para limpar dados de aprendizado
    if (uiState.showClearLearningDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearLearningDialog() },
            title = {
                Text(
                    text = "Limpar dados de aprendizado?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Isso apagará o histórico de execuções analisado e feedback das recomendações.\n\nSuas atividades cadastradas e seus compromissos agendados NÃO serão apagados.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearLearningData() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Limpar histórico")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeClearLearningDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmação para restaurar durações
    if (uiState.showRestoreDurationsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeRestoreDurationsDialog() },
            title = {
                Text(
                    text = "Restaurar durações sugeridas?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "As atividades com durações modificadas pelo aprendizado retornarão aos valores originais do catálogo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.restoreDefaultSuggestedDurations() }) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeRestoreDurationsDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun ThemeRadioOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
