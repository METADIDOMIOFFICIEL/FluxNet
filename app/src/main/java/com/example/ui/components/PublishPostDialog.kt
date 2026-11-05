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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.MobileOperator
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald

@Composable
fun PublishPostDialog(
    onDismiss: () -> Unit,
    onPublish: (
        authorName: String,
        postType: String,
        operatorKey: String,
        planType: String,
        location: String,
        title: String,
        content: String,
        problemLevel: String,
        durationMinutes: Int,
        isResolved: Boolean,
        priceFcfa: Double,
        dataMb: Double,
        callMinutes: Int,
        smsCount: Int,
        validityDays: Int
    ) -> Unit
) {
    var selectedPostTypeIndex by remember { mutableIntStateOf(0) } // 0 = Panne / Incident, 1 = Bon plan Forfait
    var authorPseudo by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("Yaoundé") }
    var selectedOperator by remember { mutableStateOf(MobileOperator.MTN) }

    val planTypes = listOf("Mobile 4G/5G", "Fibre optique", "Box 4G/Routeur", "ADSL", "Illimité")
    var selectedPlanType by remember { mutableStateOf(planTypes[0]) }

    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    // Incident fields
    val severityLevels = listOf(
        "Critique (Coupure totale)",
        "Majeur (Débit quasi-nul)",
        "Modéré (Ralentissement)",
        "Mineur (Instabilité/Ping)"
    )
    var selectedSeverity by remember { mutableStateOf(severityLevels[1]) }
    var durationMinutesText by remember { mutableStateOf("45") }
    var isResolved by remember { mutableStateOf(false) }

    // Plan offer fields
    var priceText by remember { mutableStateOf("2000") }
    var dataGbText by remember { mutableStateOf("5") }
    var validityDaysText by remember { mutableStateOf("30") }
    var callsText by remember { mutableStateOf("30") }
    var smsText by remember { mutableStateOf("50") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("publish_post_dialog"),
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
                            color = DownloadCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = DownloadCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Publier pour la Communauté",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Partagez vos signalements ou vos bons plans",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Post Type Selector: Panne vs Bon plan Forfait
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedPostTypeIndex == 0,
                        onClick = { selectedPostTypeIndex = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
                        Text("Panne Réseau", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = selectedPostTypeIndex == 1,
                        onClick = { selectedPostTypeIndex = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
                        Text("Bon Plan Forfait", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Author & Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authorPseudo,
                        onValueChange = { authorPseudo = it },
                        label = { Text("Votre pseudo / nom") },
                        placeholder = { Text("ex: Alex K.") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Ville / Quartier") },
                        placeholder = { Text("ex: Douala - Akwa") },
                        leadingIcon = {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Operator Selection
                Text(
                    text = "Opérateur concerné",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MobileOperator.entries.forEach { op ->
                        val isSelected = selectedOperator == op
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) op.brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, op.brandColor) else null,
                            modifier = Modifier.clickable { selectedOperator = op }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(op.brandColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = op.shortName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Plan type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    planTypes.forEach { type ->
                        FilterChip(
                            selected = selectedPlanType == type,
                            onClick = { selectedPlanType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // INCIDENT FIELDS
                if (selectedPostTypeIndex == 0) {
                    Text(
                        text = "Gravité du problème",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        severityLevels.forEach { level ->
                            val isSelected = selectedSeverity == level
                            val chipColor = when {
                                level.startsWith("Critique") -> BlockedCoral
                                level.startsWith("Majeur") -> Color(0xFFF97316)
                                level.startsWith("Modéré") -> MobileAmber
                                else -> UploadEmerald
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) chipColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, chipColor) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSeverity = level }
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = durationMinutesText,
                        onValueChange = { durationMinutesText = it },
                        label = { Text("Durée constatée (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // PLAN OFFER FIELDS
                if (selectedPostTypeIndex == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Prix (FCFA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dataGbText,
                            onValueChange = { dataGbText = it },
                            label = { Text("Données (Go)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = validityDaysText,
                            onValueChange = { validityDaysText = it },
                            label = { Text("Validité (j)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = callsText,
                            onValueChange = { callsText = it },
                            label = { Text("Appels (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = smsText,
                            onValueChange = { smsText = it },
                            label = { Text("SMS") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Title & Content
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Titre de la publication") },
                    placeholder = {
                        Text(
                            if (selectedPostTypeIndex == 0) "ex: Coupure 4G secteur Dragages" else "ex: Pass Data 30 Go à 10 000 F"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Description & détails") },
                    placeholder = {
                        Text(
                            if (selectedPostTypeIndex == 0) "Expliquez ce que vous constatez (ralentissement, coupure, retour à la normale...)" else "Détaillez l'offre et vos astuces pour que d'autres utilisateurs puissent la copier et comparer."
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        val isIncident = selectedPostTypeIndex == 0
                        val gb = dataGbText.replace(',', '.').toDoubleOrNull() ?: 5.0
                        onPublish(
                            authorPseudo.ifBlank { "Utilisateur" },
                            if (isIncident) "INCIDENT" else "PLAN_OFFER",
                            selectedOperator.name,
                            selectedPlanType,
                            locationText.ifBlank { "National" },
                            titleText.ifBlank { if (isIncident) "Signalement ${selectedOperator.shortName}" else "Bon plan ${selectedOperator.shortName}" },
                            contentText.trim(),
                            if (isIncident) selectedSeverity else "",
                            durationMinutesText.toIntOrNull() ?: 30,
                            isResolved,
                            priceText.toDoubleOrNull() ?: 2000.0,
                            gb * 1024.0,
                            callsText.toIntOrNull() ?: 0,
                            smsText.toIntOrNull() ?: 0,
                            validityDaysText.toIntOrNull() ?: 30
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPostTypeIndex == 0) BlockedCoral else DownloadCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_community_post_button")
                ) {
                    Icon(
                        imageVector = if (selectedPostTypeIndex == 0) Icons.Rounded.ReportProblem else Icons.Rounded.DataUsage,
                        contentDescription = null,
                        tint = if (selectedPostTypeIndex == 0) Color.White else Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Publier pour la Communauté",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPostTypeIndex == 0) Color.White else Color.Black
                    )
                }
            }
        }
    }
}
