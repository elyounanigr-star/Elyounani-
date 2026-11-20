package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.EsimGenerator
import com.example.model.EsimStatus
import com.example.model.VirtualEsim
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.MonoTelemetryStyle

@Composable
fun VirtualEsimCard(
    esim: VirtualEsim,
    isHero: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = esim.accentEnum
    val status = esim.statusEnum
    val statusColor = when (status) {
        EsimStatus.ACTIVE -> EmeraldSignal
        EsimStatus.STANDBY -> accent.primaryColor
        EsimStatus.EXPIRED -> CoralAlert
    }

    val cardBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0C1A2B),
            accent.secondaryColor.copy(alpha = if (isHero) 0.72f else 0.45f),
            Color(0xFF09131F)
        )
    )

    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (isHero) 4.dp else 2.dp,
        shadowElevation = if (isHero) 8.dp else 2.dp,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBrush)
            .border(
                width = if (status == EsimStatus.ACTIVE) 1.5.dp else 1.dp,
                color = if (status == EsimStatus.ACTIVE) {
                    accent.primaryColor.copy(alpha = 0.75f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .then(clickableMod)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Chip + Country Badge + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsimMicrochipGraphic(accentColor = accent.primaryColor)
                    Column {
                        Text(
                            text = esim.label.ifBlank { "Virtual eSIM" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = accent.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${esim.carrierName} • ${esim.countryName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBED0E4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                StatusPill(
                    label = status.displayName.uppercase(),
                    color = statusColor
                )
            }

            // Data Allowance Progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val remainingLabel = if (esim.unlimitedData) {
                        "Unlimited • ${EsimGenerator.formatDataAmount(esim.dataUsedMb)} used"
                    } else {
                        "${EsimGenerator.formatDataAmount(esim.remainingMb)} left of ${EsimGenerator.formatDataAmount(esim.dataLimitMb)}"
                    }
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${esim.validityDays}d validity",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9EB4CC)
                    )
                }

                LinearProgressIndicator(
                    progress = { 1f - esim.usageFraction },
                    color = accent.primaryColor,
                    trackColor = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
            }

            // Bottom Telemetry Strip: ICCID + Capabilities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ICCID ${esim.formattedIccid}",
                    style = MonoTelemetryStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                    color = Color(0xFFD0E0F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (esim.fiveGEnabled) {
                        TelemetryBadge(text = "5G", color = accent.primaryColor)
                    }
                    if (esim.roamingEnabled) {
                        TelemetryBadge(text = "ROAM", color = EmeraldSignal)
                    }
                    Icon(
                        imageVector = if (status == EsimStatus.ACTIVE) {
                            Icons.Rounded.SignalCellularAlt
                        } else {
                            Icons.Rounded.QrCode2
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = CircleShape,
        modifier = modifier.border(1.dp, color.copy(alpha = 0.55f), CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TelemetryBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EsimMicrochipGraphic(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 40.dp, height = 30.dp)) {
        val w = size.width
        val h = size.height
        // Metallic gold/amber contact plate
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(AmberGold, Color(0xFFFDE68A), Color(0xFFD97706))
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Subtle inner circuit traces
        val stroke = Stroke(width = 1.1.dp.toPx())
        val traceColor = Color(0xFF451A03).copy(alpha = 0.55f)
        drawLine(traceColor, Offset(0f, h * 0.35f), Offset(w * 0.35f, h * 0.35f), strokeWidth = stroke.width)
        drawLine(traceColor, Offset(0f, h * 0.65f), Offset(w * 0.35f, h * 0.65f), strokeWidth = stroke.width)
        drawLine(traceColor, Offset(w * 0.65f, h * 0.35f), Offset(w, h * 0.35f), strokeWidth = stroke.width)
        drawLine(traceColor, Offset(w * 0.65f, h * 0.65f), Offset(w, h * 0.65f), strokeWidth = stroke.width)
        drawRoundRect(
            color = traceColor,
            topLeft = Offset(w * 0.34f, h * 0.18f),
            size = Size(w * 0.32f, h * 0.64f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = stroke
        )
        // Tiny active LED dot
        drawCircle(
            color = accentColor,
            radius = 2.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}
