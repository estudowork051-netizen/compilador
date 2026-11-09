package com.lifeorganizer.app.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageAccent
import com.lifeorganizer.app.core.theme.SageLight
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import kotlinx.coroutines.delay
import java.time.LocalTime

enum class EnvironmentPeriod(val title: String, val icon: String) {
    MORNING("Manhã", "☀️"),
    AFTERNOON("Tarde", "🌿"),
    NIGHT("Noite", "🌙")
}

/**
 * Nature Hero Card (FASE B4.2 & B4.3).
 * Integrates dynamic clock, next real activity, countdown, free time, and Breathe action.
 */
@Composable
fun NatureHeroCard(
    currentPeriod: EnvironmentPeriod,
    activeEntry: ScheduleEntryModel?,
    nextUpcomingEntry: ScheduleEntryModel?,
    remainingFreeFormatted: String,
    onBreatheClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic live clock
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            currentTime = LocalTime.now()
        }
    }
    val clockString = String.format("%02d:%02d", currentTime.hour, currentTime.minute)

    // Dynamic gradient by selected environment period
    val backgroundBrush = remember(currentPeriod) {
        when (currentPeriod) {
            EnvironmentPeriod.MORNING -> Brush.linearGradient(
                listOf(Color(0xFFE8EFE9), Color(0xFFFDFBF7))
            )
            EnvironmentPeriod.AFTERNOON -> Brush.linearGradient(
                listOf(Color(0xFFDDE9D9), Color(0xFFF7F4EC))
            )
            EnvironmentPeriod.NIGHT -> Brush.linearGradient(
                listOf(Color(0xFF465B4B), Color(0xFF75937B))
            )
        }
    }

    val isNight = currentPeriod == EnvironmentPeriod.NIGHT
    val contentColor = if (isNight) Color.White else PrimaryDark
    val secondaryColor = if (isNight) Color(0xFFE0ECE2) else Color(0xFF5E6D60)

    val targetEntry = activeEntry ?: nextUpcomingEntry
    val nowMinutes = currentTime.hour * 60 + currentTime.minute

    val countdownText = remember(targetEntry, nowMinutes) {
        if (targetEntry == null) null
        else if (activeEntry != null) "Acontecendo agora"
        else {
            val startMin = targetEntry.startTotalMinutes
            val diff = startMin - nowMinutes
            when {
                diff in 1..59 -> "Começa em $diff minutos"
                diff >= 60 -> "Começa em ${diff / 60}h ${diff % 60}min"
                else -> "Horário programado chegado"
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, if (isNight) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row: Clock & Free Time Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = clockString,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentPeriod.icon,
                            fontSize = 20.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNight) Color.White.copy(alpha = 0.18f) else SageLight.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "$remainingFreeFormatted livres hoje",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = contentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Middle: Next Activity Details or Peaceful Free State
                if (targetEntry != null) {
                    Text(
                        text = if (activeEntry != null) "Atividade em andamento" else "Próxima atividade",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = secondaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = targetEntry.activityTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = targetEntry.timeFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = contentColor
                        )
                        countdownText?.let { countText ->
                            Text(
                                text = " • $countText",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                                color = secondaryColor
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Próximo período livre 🌿",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aproveite para descansar, focar no que importa ou planejar seu dia com calma.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action: Respirar button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBreatheClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNight) Color.White.copy(alpha = 0.25f) else PrimarySage,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Respirar",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Ritmo natural ativo",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor
                    )
                }
            }
        }
    }
}
