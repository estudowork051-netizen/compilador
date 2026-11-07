package com.lifeorganizer.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeorganizer.app.core.components.AvailableTimeIndicator
import com.lifeorganizer.app.core.components.CategoryChip
import com.lifeorganizer.app.core.components.InformativeBanner
import com.lifeorganizer.app.core.components.LifeFlowIconButton
import com.lifeorganizer.app.core.components.LifeFlowOutlinedButton
import com.lifeorganizer.app.core.components.LifeFlowPrimaryButton
import com.lifeorganizer.app.core.components.LifeFlowTextField
import com.lifeorganizer.app.core.components.StepProgressIndicator
import com.lifeorganizer.app.core.components.TimePickerField
import com.lifeorganizer.app.domain.model.OrganizationMode

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.currentStep > 1) {
        viewModel.previousStep()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 1) {
                    LifeFlowIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar",
                        onClick = { viewModel.previousStep() }
                    )
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                StepProgressIndicator(
                    currentStep = uiState.currentStep,
                    totalSteps = 6
                )

                Spacer(modifier = Modifier.size(44.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
                    1 -> StepWelcome(onStartClick = { viewModel.nextStep() })
                    2 -> StepName(
                        name = uiState.name,
                        onNameChange = { viewModel.setName(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    3 -> StepRoutineTimes(
                        wakeUpTime = uiState.wakeUpTime,
                        sleepTime = uiState.sleepTime,
                        awakeDurationText = uiState.awakeDurationFormatted,
                        onWakeUpChange = { viewModel.setWakeUpTime(it) },
                        onSleepChange = { viewModel.setSleepTime(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    4 -> StepCommitments(
                        selectedCommitments = uiState.selectedCommitments,
                        onToggleCommitment = { viewModel.toggleCommitment(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    5 -> StepOrganizationMode(
                        selectedMode = uiState.organizationMode,
                        onSelectMode = { viewModel.setOrganizationMode(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    6 -> StepCompletion(
                        state = uiState,
                        onEnterApp = { viewModel.completeOnboarding(onFinished) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: Boas-vindas
// -------------------------------------------------------------
@Composable
private fun StepWelcome(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Local vector illustration
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Sua rotina, do seu jeito.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Organize seu tempo, cuide das suas prioridades e encontre espaço para o que realmente importa.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = "Começar",
                onClick = onStartClick,
                icon = Icons.AutoMirrored.Rounded.ArrowForward
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2: Personalização (Nome)
// -------------------------------------------------------------
@Composable
private fun StepName(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = "Como podemos chamar você?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seu nome ou apelido será usado apenas para personalizar as saudações no aplicativo. O preenchimento é opcional.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            LifeFlowTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Seu nome ou apelido (opcional)",
                placeholder = "Ex: Carlos, Ana..."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = if (name.isBlank()) "Continuar sem informar" else "Avançar",
                onClick = onNext
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 3: Horários habituais
// -------------------------------------------------------------
@Composable
private fun StepRoutineTimes(
    wakeUpTime: String,
    sleepTime: String,
    awakeDurationText: String,
    onWakeUpChange: (String) -> Unit,
    onSleepChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = "Como costuma ser seu dia?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Defina os horários aproximados em que costuma acordar e dormir. O LifeFlow aceita horários que atravessam a meia-noite.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            TimePickerField(
                label = "Horário habitual de acordar",
                timeValue = wakeUpTime,
                onTimeChange = onWakeUpChange,
                supportingText = "Sugestão padrão: 08:00"
            )

            Spacer(modifier = Modifier.height(20.dp))

            TimePickerField(
                label = "Horário habitual de dormir",
                timeValue = sleepTime,
                onTimeChange = onSleepChange,
                supportingText = "Sugestão padrão: 00:00 (ou após meia-noite)"
            )

            Spacer(modifier = Modifier.height(24.dp))

            AvailableTimeIndicator(
                wakeUpTime = wakeUpTime,
                sleepTime = sleepTime,
                awakeDurationText = awakeDurationText
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = "Avançar",
                onClick = onNext
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Compromissos habituais
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCommitments(
    selectedCommitments: Set<String>,
    onToggleCommitment: (String) -> Unit,
    onNext: () -> Unit
) {
    val options = listOf(
        Pair("Trabalho", Icons.Rounded.Work),
        Pair("Faculdade", Icons.Rounded.School),
        Pair("Escola", Icons.Rounded.School),
        Pair("Treino", Icons.Rounded.FitnessCenter),
        Pair("Outro compromisso", Icons.Rounded.MoreHoriz),
        Pair("Configurar depois", Icons.Rounded.HourglassEmpty)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = "Você tem algum compromisso fixo?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Selecione as opções que fazem parte da sua semana. A configuração detalhada de horários e recorrências será realizada em etapa posterior.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { (title, icon) ->
                    CategoryChip(
                        label = title,
                        icon = icon,
                        selected = selectedCommitments.contains(title),
                        onSelectedChange = { onToggleCommitment(title) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = "Avançar",
                onClick = onNext
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 5: Preferência de organização
// -------------------------------------------------------------
@Composable
private fun StepOrganizationMode(
    selectedMode: OrganizationMode,
    onSelectMode: (OrganizationMode) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = "Como prefere organizar sua rotina?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Você poderá alterar essa preferência a qualquer momento nas configurações.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OrganizationOptionCard(
                title = "Manual",
                description = "Você decide seus horários e atividades. Nós ajudamos com os cálculos e sugestões.",
                icon = Icons.Rounded.Handyman,
                isSelected = selectedMode == OrganizationMode.MANUAL,
                onClick = { onSelectMode(OrganizationMode.MANUAL) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OrganizationOptionCard(
                title = "Automático",
                description = "Receba sugestões de organização para seu dia e ajuste tudo como preferir.",
                icon = Icons.Rounded.AutoMode,
                isSelected = selectedMode == OrganizationMode.AUTOMATIC,
                onClick = { onSelectMode(OrganizationMode.AUTOMATIC) },
                badge = "Próximas etapas"
            )

            Spacer(modifier = Modifier.height(16.dp))

            InformativeBanner(
                title = "Nota sobre a automação",
                message = "A distribuição automática completa e o aprendizado com hábitos serão desenvolvidos nas etapas 4 e 5. A fundação de preferências já está pronta."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = "Avançar",
                onClick = onNext
            )
        }
    }
}

@Composable
private fun OrganizationOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 6: Finalização
// -------------------------------------------------------------
@Composable
private fun StepCompletion(
    state: OnboardingUiState,
    onEnterApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tudo pronto para começar!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Seu espaço de organização está preparado. Vamos construir uma rotina que funciona para você.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Resumo da sua configuração",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.name.isNotBlank()) {
                        SummaryRow(label = "Nome:", value = state.name)
                    }
                    SummaryRow(label = "Horário acordado:", value = "${state.wakeUpTime} às ${state.sleepTime} (${state.awakeDurationFormatted})")
                    SummaryRow(label = "Modo escolhido:", value = if (state.organizationMode == OrganizationMode.MANUAL) "Manual" else "Automático")
                    if (state.selectedCommitments.isNotEmpty()) {
                        SummaryRow(label = "Compromissos:", value = state.selectedCommitments.joinToString(", "))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            LifeFlowPrimaryButton(
                text = "Entrar no aplicativo",
                onClick = onEnterApp
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
