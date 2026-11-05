package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.NetworkIncidentEntity
import com.example.data.model.MobileOperator
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun IncidentReportDialog(
    initialOperator: MobileOperator? = null,
    onDismiss: () -> Unit,
    onSaveIncident: (NetworkIncidentEntity) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val now = remember { Date() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale.FRANCE) }

    var selectedOperator by remember {
        mutableStateOf(initialOperator ?: MobileOperator.MTN)
    }

    val planTypes = listOf(
        "Mobile 4G/5G",
        "Fibre optique",
        "Box 4G/Routeur",
        "ADSL",
        "Illimité Nuit/Jour"
    )
    var selectedPlanType by remember { mutableStateOf(planTypes[0]) }

    val severityLevels = listOf(
        "Critique (Coupure totale)",
        "Majeur (Débit quasi-nul)",
        "Modéré (Ralentissement)",
        "Mineur (Instabilité/Ping)"
    )
    var selectedSeverity by remember { mutableStateOf(severityLevels[1]) }

    var dateText by remember { mutableStateOf(dateFormat.format(now)) }
    var timeText by remember { mutableStateOf(timeFormat.format(now)) }

    val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    val initialDay = dayFormat.format(now).replaceFirstChar { it.uppercase() }
    var selectedDay by remember {
        mutableStateOf(daysOfWeek.find { it.equals(initialDay, ignoreCase = true) } ?: "Mercredi")
    }

    var durationMinutes by remember { mutableIntStateOf(45) }
    var isResolved by remember { mutableStateOf(true) }
    var descriptionText by remember { mutableStateOf("") }

    val durationPresets = listOf(
        15 to "15 min",
        30 to "30 min",
        45 to "45 min",
        60 to "1h",
        120 to "2h",
        240 to "4h",
        720 to "12h",
        1440 to "24h"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("incident_report_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BlockedCoral.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ReportProblem,
                                    contentDescription = null,
                                    tint = BlockedCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Signaler un problème réseau",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Affecte le score réel de votre comparateur",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 1. Choix Opérateur
                Text(
                    text = "Opérateur concerné",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MobileOperator.entries.forEach { op ->
                        val isSelected = selectedOperator == op
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) op.brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, op.brandColor) else null,
                            modifier = Modifier
                                .clickable { selectedOperator = op }
                                .testTag("op_chip_${op.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(op.brandColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = op.shortName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. Type de forfait
                Text(
                    text = "Type de forfait",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    planTypes.forEach { type ->
                        FilterChip(
                            selected = selectedPlanType == type,
                            onClick = { selectedPlanType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Niveau de gravité du problème
                Text(
                    text = "Niveau du problème",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    severityLevels.forEach { level ->
                        val isSelected = selectedSeverity == level
                        val chipColor = when {
                            level.startsWith("Critique") -> BlockedCoral
                            level.startsWith("Majeur") -> Color(0xFFF97316)
                            level.startsWith("Modéré") -> MobileAmber
                            else -> UploadEmerald
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, chipColor) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSeverity = level }
                                .testTag("severity_${level.take(8).lowercase().trim()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 4. Date, Heure et Jour de la semaine
                Text(
                    text = "Date, Heure & Jour de l'incident",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date") },
                        leadingIcon = {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.1f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Heure") },
                        leadingIcon = {
                            Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Jour de la semaine :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DownloadCyan.copy(alpha = 0.2f),
                                selectedLabelColor = DownloadCyan
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 5. Durée avant résolution
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Durée avant résolution",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = !isResolved,
                            onCheckedChange = { isResolved = !it }
                        )
                        Text(
                            text = "Toujours en cours",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (isResolved) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durationPresets.forEach { (mins, label) ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                        label = { Text("Durée personnalisée (minutes)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 6. Commentaires
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Détails / Symptômes (optionnel)") },
                    placeholder = { Text("ex: Coupure 4G après coupure de courant, vidéo impossible...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        val incident = NetworkIncidentEntity(
                            operatorKey = selectedOperator.name,
                            planType = selectedPlanType,
                            problemLevel = selectedSeverity,
                            durationMinutes = if (isResolved) durationMinutes else 180,
                            incidentDate = dateText.ifBlank { dateFormat.format(now) },
                            incidentTime = timeText.ifBlank { timeFormat.format(now) },
                            dayOfWeek = selectedDay,
                            timestampMillis = System.currentTimeMillis(),
                            isResolved = isResolved,
                            description = descriptionText.trim()
                        )
                        onSaveIncident(incident)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlockedCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_incident_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrer le signalement", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
