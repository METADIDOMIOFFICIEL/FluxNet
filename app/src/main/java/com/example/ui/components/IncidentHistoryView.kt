package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.NetworkIncidentEntity
import com.example.data.model.MobileOperator
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald

@Composable
fun IncidentHistoryView(
    incidents: List<NetworkIncidentEntity>,
    onOpenReportDialog: () -> Unit,
    onDeleteIncident: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOperatorFilter by remember { mutableStateOf<String?>(null) }
    var selectedPlanTypeFilter by remember { mutableStateOf<String?>(null) }

    val filteredIncidents = remember(incidents, selectedOperatorFilter, selectedPlanTypeFilter) {
        incidents.filter { inc ->
            val matchOp = selectedOperatorFilter == null || inc.operatorKey.equals(selectedOperatorFilter, ignoreCase = true)
            val matchPlan = selectedPlanTypeFilter == null || inc.planType.equals(selectedPlanTypeFilter, ignoreCase = true)
            matchOp && matchPlan
        }
    }

    // Statistical analysis
    val totalCount = incidents.size
    val criticalCount = incidents.count { it.problemLevel.contains("Critique", ignoreCase = true) }
    val majorCount = incidents.count { it.problemLevel.contains("Majeur", ignoreCase = true) }
    val avgDurationMins = if (incidents.isNotEmpty()) {
        (incidents.sumOf { it.durationMinutes.toLong() } / incidents.size).toInt()
    } else 0

    val mostFrequentDay = remember(incidents) {
        incidents.groupBy { it.dayOfWeek }
            .maxByOrNull { it.value.size }?.key ?: "Aucun"
    }

    val peakHourCount = remember(incidents) {
        incidents.count {
            val h = it.incidentTime.split(":", "h", "H").firstOrNull()?.trim()?.toIntOrNull() ?: 0
            h in 18..23 || h in 12..14
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("incident_history_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Action header & Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BlockedCoral.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReportProblem,
                                        contentDescription = null,
                                        tint = BlockedCoral,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Historique des Pannes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$totalCount signalement(s) pris en compte",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onOpenReportDialog,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlockedCoral),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_incident_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Signaler", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 4-box stat summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = BlockedCoral.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Critiques / Majeurs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${criticalCount + majorCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                    color = BlockedCoral
                                )
                            }
                        }

                        Surface(
                            color = DownloadCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Délai résolution moy.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                val durText = if (avgDurationMins < 60) "${avgDurationMins}m" else "${avgDurationMins / 60}h ${avgDurationMins % 60}m"
                                Text(
                                    text = durText,
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                    color = DownloadCyan
                                )
                            }
                        }

                        Surface(
                            color = MobileAmber.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Heures d'affluence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "$peakHourCount pannes",
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                    color = MobileAmber
                                )
                            }
                        }
                    }

                    if (totalCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "ℹ️ Jour le plus souvent impacté : $mostFrequentDay. Ces signalements pénalisent directement la note de fiabilité de chaque opérateur dans l'onglet Comparateur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Filter Row
        item {
            Column {
                Text(
                    text = "Filtrer par opérateur :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedOperatorFilter == null,
                        onClick = { selectedOperatorFilter = null },
                        label = { Text("Tous ($totalCount)") }
                    )
                    MobileOperator.entries.forEach { op ->
                        val count = incidents.count { it.operatorKey.equals(op.name, ignoreCase = true) }
                        FilterChip(
                            selected = selectedOperatorFilter == op.name,
                            onClick = { selectedOperatorFilter = if (selectedOperatorFilter == op.name) null else op.name },
                            label = { Text("${op.shortName} ($count)") }
                        )
                    }
                }
            }
        }

        // 3. Incidents List
        if (filteredIncidents.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = UploadEmerald,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Aucun signalement correspondant",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Utilisez le bouton 'Signaler' pour ajouter un ralentissement ou une coupure réseau constatée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredIncidents, key = { it.id }) { incident ->
                IncidentCard(
                    incident = incident,
                    onDelete = { onDeleteIncident(incident.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun IncidentCard(
    incident: NetworkIncidentEntity,
    onDelete: () -> Unit
) {
    val op = MobileOperator.entries.find { it.name.equals(incident.operatorKey, ignoreCase = true) } ?: MobileOperator.MTN

    val severityColor = when {
        incident.problemLevel.contains("Critique", ignoreCase = true) -> BlockedCoral
        incident.problemLevel.contains("Majeur", ignoreCase = true) -> Color(0xFFF97316)
        incident.problemLevel.contains("Modéré", ignoreCase = true) -> MobileAmber
        else -> UploadEmerald
    }

    val durFormatted = if (incident.durationMinutes < 60) {
        "${incident.durationMinutes} min"
    } else {
        val h = incident.durationMinutes / 60
        val m = incident.durationMinutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("incident_item_${incident.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Opérateur + Gravité + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(op.brandColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = op.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = incident.planType,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Severity badge & status
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = severityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = severityColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = incident.problemLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = severityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = if (incident.isResolved) UploadEmerald.copy(alpha = 0.12f) else BlockedCoral.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (incident.isResolved) Icons.Rounded.CheckCircle else Icons.Rounded.HourglassBottom,
                            contentDescription = null,
                            tint = if (incident.isResolved) UploadEmerald else BlockedCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (incident.isResolved) "Résolu en $durFormatted" else "En cours ($durFormatted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (incident.isResolved) UploadEmerald else BlockedCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Date, Hour, Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${incident.dayOfWeek} ${incident.incidentDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = incident.incidentTime,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (incident.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = incident.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
