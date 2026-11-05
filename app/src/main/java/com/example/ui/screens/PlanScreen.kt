package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.NetworkIncidentEntity
import com.example.data.local.NetworkQualityRecordEntity
import com.example.data.local.OperatorOfferEntity
import com.example.data.model.DataPlanSummary
import com.example.data.model.MobileOperator
import com.example.data.model.OperatorComparator
import com.example.data.model.OperatorComparisonResult
import com.example.data.model.OperatorNeedType
import com.example.ui.components.IncidentHistoryView
import com.example.ui.components.IncidentReportDialog
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import com.example.util.Formatters
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    planSummary: DataPlanSummary,
    operatorOffers: Map<MobileOperator, OperatorOfferEntity>,
    userIncidents: List<NetworkIncidentEntity>,
    networkQualityRecords: List<NetworkQualityRecordEntity>,
    snackbarHostState: SnackbarHostState,
    onSavePlan: (
        operatorName: String,
        quotaMb: Long,
        validityDays: Int,
        cost: Double,
        currency: String,
        startDateMillis: Long
    ) -> Unit,
    onSaveOperatorOffer: (OperatorOfferEntity) -> Unit,
    onUpdateAlertThreshold: (Int) -> Unit,
    onUpdateAutoBlockBackground: (Boolean, Int?) -> Unit,
    onAddIncident: (NetworkIncidentEntity) -> Unit,
    onDeleteIncident: (Long) -> Unit,
    onMeasureQuality: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Mon Forfait, 1 = Comparateur, 2 = Signalements
    var showReportDialog by remember { mutableStateOf(false) }
    var dialogPresetOperator by remember { mutableStateOf<MobileOperator?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Forfait & Qualité Réseau",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dépenses, comparateur multi-critères et signalements d'incidents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_subtab_selector")
            ) {
                SegmentedButton(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Forfait", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("comparator_tab_button")
                ) {
                    Text("Comparateur", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("incidents_tab_button")
                ) {
                    Text("Pannes (${userIncidents.size})", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                MyPlanContent(
                    planSummary = planSummary,
                    snackbarHostState = snackbarHostState,
                    onSavePlan = onSavePlan,
                    onUpdateAlertThreshold = onUpdateAlertThreshold,
                    onUpdateAutoBlockBackground = onUpdateAutoBlockBackground
                )
            }
            1 -> {
                OperatorComparatorContent(
                    planSummary = planSummary,
                    operatorOffers = operatorOffers,
                    userIncidents = userIncidents,
                    networkQualityRecords = networkQualityRecords,
                    onSaveOperatorOffer = onSaveOperatorOffer,
                    onOpenReportDialog = { op ->
                        dialogPresetOperator = op
                        showReportDialog = true
                    },
                    onMeasureQuality = { op ->
                        onMeasureQuality(op)
                        scope.launch {
                            snackbarHostState.showSnackbar("Qualité réseau enregistrée pour $op dans votre historique !")
                        }
                    }
                )
            }
            2 -> {
                IncidentHistoryView(
                    incidents = userIncidents,
                    onOpenReportDialog = {
                        dialogPresetOperator = null
                        showReportDialog = true
                    },
                    onDeleteIncident = { id ->
                        onDeleteIncident(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Signalement retiré de l'historique")
                        }
                    }
                )
            }
        }
    }

    if (showReportDialog) {
        IncidentReportDialog(
            initialOperator = dialogPresetOperator,
            onDismiss = { showReportDialog = false },
            onSaveIncident = { incident ->
                onAddIncident(incident)
                scope.launch {
                    snackbarHostState.showSnackbar("Signalement pris en compte ! Le comparateur a été mis à jour.")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyPlanContent(
    planSummary: DataPlanSummary,
    snackbarHostState: SnackbarHostState,
    onSavePlan: (
        operatorName: String,
        quotaMb: Long,
        validityDays: Int,
        cost: Double,
        currency: String,
        startDateMillis: Long
    ) -> Unit,
    onUpdateAlertThreshold: (Int) -> Unit,
    onUpdateAutoBlockBackground: (Boolean, Int?) -> Unit
) {
    val cfg = planSummary.config
    val scope = rememberCoroutineScope()

    var selectedOperator by remember(cfg.operatorName) {
        mutableStateOf(cfg.operatorName)
    }
    var quotaGbText by remember(cfg.quotaMb) {
        mutableStateOf((cfg.quotaMb / 1024.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it) })
    }
    var validityDaysText by remember(cfg.validityDays) {
        mutableStateOf(cfg.validityDays.toString())
    }
    var costText by remember(cfg.cost) {
        mutableStateOf(if (cfg.cost % 1.0 == 0.0) cfg.cost.toInt().toString() else cfg.cost.toString())
    }
    var currency by remember(cfg.currency) {
        mutableStateOf(cfg.currency)
    }
    var startDateMillis by remember(cfg.startDateMillis) {
        mutableLongStateOf(cfg.startDateMillis)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val currencies = listOf("FCFA", "€", "$", "DH", "CHF")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Expense Estimation & Projection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_estimation_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = DownloadCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Estimation des dépenses (${cfg.operatorName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = DownloadCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Consommé à ce jour",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(planSummary.estimatedCostUsed, cfg.currency),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = DownloadCyan
                                )
                                Text(
                                    text = "${planSummary.usagePercent.toInt()}% du forfait",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val projColor = if (planSummary.isOverBudgetPace) BlockedCoral else UploadEmerald
                        Surface(
                            color = projColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Projection fin de cycle",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(planSummary.projectedTotalCost, cfg.currency),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = projColor
                                )
                                Text(
                                    text = if (planSummary.isOverBudgetPace) "Dépassement prévu" else "Rythme dans les clous",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = projColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Budget quotidien recommandé :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${Formatters.formatCurrency(planSummary.dailyRecommendedBudget, cfg.currency)} / j",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dépense quotidienne réelle :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${Formatters.formatCurrency(planSummary.dailyAverageCost, cfg.currency)} / j",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // 2. Active Plan Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_form_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Paramètres du Forfait Actif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Opérateur principal",
                        style = MaterialTheme.typography.labelMedium,
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
                            FilterChip(
                                selected = selectedOperator.equals(op.displayName, ignoreCase = true) ||
                                    selectedOperator.equals(op.shortName, ignoreCase = true),
                                onClick = { selectedOperator = op.displayName },
                                label = { Text(op.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quotaGbText,
                            onValueChange = { quotaGbText = it },
                            label = { Text("Volume (Go)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_quota_input")
                        )
                        OutlinedTextField(
                            value = validityDaysText,
                            onValueChange = { validityDaysText = it },
                            label = { Text("Durée (jours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_validity_input")
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Coût du forfait ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_cost_input")
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { currency = curr },
                                label = { Text(curr) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_start_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Date de début",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Début d'utilisation : ${Formatters.formatDate(startDateMillis)}")
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val gb = quotaGbText.replace(',', '.').toDoubleOrNull() ?: 20.0
                            val days = validityDaysText.toIntOrNull() ?: 30
                            val cost = costText.replace(',', '.').toDoubleOrNull() ?: 2000.0
                            onSavePlan(
                                selectedOperator,
                                (gb * 1024.0).toLong().coerceAtLeast(100L),
                                days.coerceIn(1, 365),
                                cost.coerceAtLeast(0.0),
                                currency,
                                startDateMillis
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("Forfait $selectedOperator enregistré")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_plan_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer le forfait", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Alerts & Protection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MobileAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Alertes & Protection Arrière-plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seuil d'alerte de consommation",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${cfg.alertThresholdPercent}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MobileAmber
                        )
                    }

                    Slider(
                        value = cfg.alertThresholdPercent.toFloat(),
                        onValueChange = { onUpdateAlertThreshold(it.roundToInt()) },
                        valueRange = 50f..95f,
                        steps = 8,
                        modifier = Modifier.testTag("alert_threshold_slider")
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bloquer les apps gourmandes en arrière-plan",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Surveille et restreint automatiquement toute app dépassant ${cfg.backgroundThresholdMbPerMin} Mo/min en arrière-plan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = cfg.autoBlockBackgroundHeavy,
                            onCheckedChange = { onUpdateAutoBlockBackground(it, null) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { startDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun OperatorComparatorContent(
    planSummary: DataPlanSummary,
    operatorOffers: Map<MobileOperator, OperatorOfferEntity>,
    userIncidents: List<NetworkIncidentEntity>,
    networkQualityRecords: List<NetworkQualityRecordEntity>,
    onSaveOperatorOffer: (OperatorOfferEntity) -> Unit,
    onOpenReportDialog: (MobileOperator?) -> Unit,
    onMeasureQuality: (String) -> Unit
) {
    val measuredDailyMb = remember(planSummary.usedBytes, planSummary.daysElapsed) {
        val rawMb = (planSummary.usedBytes / 1_048_576.0) / planSummary.daysElapsed.coerceAtLeast(1)
        if (rawMb >= 10.0) rawMb else 500.0
    }
    val hasRealHistory = planSummary.usedBytes > 1_048_576L

    var selectedNeed by remember { mutableStateOf(OperatorNeedType.INTERNET) }
    var selectedPlanTypeFilter by remember { mutableStateOf<String?>(null) }
    var useMeasuredHistory by remember { mutableStateOf(true) }
    var customDailyDataMb by remember { mutableStateOf("500") }
    var dailyCallMinText by remember { mutableStateOf("15") }
    var dailySmsText by remember { mutableStateOf("20") }

    val planFilterOptions = listOf(
        "Tous" to null,
        "Mobile 4G/5G" to "Mobile 4G/5G",
        "Fibre optique" to "Fibre optique",
        "Box 4G/Routeur" to "Box 4G/Routeur"
    )

    val effectiveDailyDataMb = if (useMeasuredHistory) {
        measuredDailyMb
    } else {
        customDailyDataMb.toDoubleOrNull()?.coerceAtLeast(10.0) ?: 500.0
    }
    val effectiveDailyCalls = dailyCallMinText.toIntOrNull()?.coerceAtLeast(1) ?: 15
    val effectiveDailySms = dailySmsText.toIntOrNull()?.coerceAtLeast(1) ?: 20

    val results = remember(
        operatorOffers,
        selectedNeed,
        effectiveDailyDataMb,
        effectiveDailyCalls,
        effectiveDailySms,
        networkQualityRecords,
        userIncidents,
        selectedPlanTypeFilter
    ) {
        OperatorComparator.evaluateUserOffers(
            offersByOperator = operatorOffers,
            needType = selectedNeed,
            userDailyDataMb = effectiveDailyDataMb,
            userDailyCallMin = effectiveDailyCalls,
            userDailySms = effectiveDailySms,
            qualityRecords = networkQualityRecords,
            incidents = userIncidents,
            filterPlanType = selectedPlanTypeFilter
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("operator_comparator_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Intelligence Algorithm Explanatory Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = DownloadCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = DownloadCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Comparateur Intelligent Multi-Critères",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prend en compte votre historique réel de pannes & qualité",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onOpenReportDialog(null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlockedCoral),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("quick_report_button")
                        ) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Signaler panne", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Algorithm formula pill breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Prix / Volume", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("40% du score", style = MaterialTheme.typography.bodySmall, color = DownloadCyan)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Qualité Réseau", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("30% (Débit/Ping)", style = MaterialTheme.typography.bodySmall, color = UploadEmerald)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Signalements", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("30% (Sévérité/Délai)", style = MaterialTheme.typography.bodySmall, color = BlockedCoral)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "ℹ️ Facteurs pris en compte : nombre d'incidents signalés par vos soins, gravité (coupure totale vs ralentissement), délai avant résolution (MTTR), et pannes récurrentes aux heures de pointe ou jours de week-end.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Need Type & Type of Plan Filter
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Configuration du profil de comparaison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Usage principal :",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OperatorNeedType.entries.forEach { need ->
                            FilterChip(
                                selected = selectedNeed == need,
                                onClick = { selectedNeed = need },
                                label = { Text(need.labelFr) },
                                modifier = Modifier.testTag("need_chip_${need.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Type de forfait :",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        planFilterOptions.forEach { (label, value) ->
                            FilterChip(
                                selected = selectedPlanTypeFilter == value,
                                onClick = { selectedPlanTypeFilter = value },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoGraph,
                                contentDescription = null,
                                tint = DownloadCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Historique de consommation",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (hasRealHistory) {
                                        "Rythme mesuré : ${measuredDailyMb.roundToInt()} Mo / jour"
                                    } else {
                                        "Rythme par défaut : 500 Mo / jour"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = useMeasuredHistory,
                            onCheckedChange = { useMeasuredHistory = it }
                        )
                    }

                    if (!useMeasuredHistory && (selectedNeed == OperatorNeedType.INTERNET || selectedNeed == OperatorNeedType.MIXED)) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDailyDataMb,
                            onValueChange = { customDailyDataMb = it },
                            label = { Text("Votre consommation Internet (Mo / jour)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (selectedNeed == OperatorNeedType.CALLS || selectedNeed == OperatorNeedType.MIXED) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dailyCallMinText,
                            onValueChange = { dailyCallMinText = it },
                            label = { Text("Vos appels moyens (minutes / jour)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (selectedNeed == OperatorNeedType.SMS || selectedNeed == OperatorNeedType.MIXED) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dailySmsText,
                            onValueChange = { dailySmsText = it },
                            label = { Text("Vos SMS moyens (SMS / jour)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Classement selon qualité & pannes signalées",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. User-Editable Operator Cards
        items(results, key = { it.operator.name }) { item ->
            UserOperatorOfferCard(
                item = item,
                needType = selectedNeed,
                onOfferChanged = onSaveOperatorOffer,
                onReportIncident = { onOpenReportDialog(item.operator) },
                onMeasureQuality = { onMeasureQuality(item.operator.name) }
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun UserOperatorOfferCard(
    item: OperatorComparisonResult,
    needType: OperatorNeedType,
    onOfferChanged: (OperatorOfferEntity) -> Unit,
    onReportIncident: () -> Unit,
    onMeasureQuality: () -> Unit
) {
    val op = item.operator
    val offer = item.offer
    var isExpanded by remember { mutableStateOf(false) }

    var priceText by remember(op.name, offer.priceFcfa) {
        mutableStateOf(if (offer.priceFcfa > 0) offer.priceFcfa.roundToInt().toString() else "")
    }
    var dataMbText by remember(op.name, offer.dataMb) {
        mutableStateOf(if (offer.dataMb > 0) offer.dataMb.roundToInt().toString() else "")
    }
    var callsText by remember(op.name, offer.callMinutes) {
        mutableStateOf(if (offer.callMinutes > 0) offer.callMinutes.toString() else "")
    }
    var smsText by remember(op.name, offer.smsCount) {
        mutableStateOf(if (offer.smsCount > 0) offer.smsCount.toString() else "")
    }
    var validityText by remember(op.name, offer.validityDays) {
        mutableStateOf(offer.validityDays.toString())
    }

    val emitUpdate: () -> Unit = {
        onOfferChanged(
            offer.copy(
                operatorKey = op.name,
                priceFcfa = priceText.toDoubleOrNull() ?: 0.0,
                dataMb = dataMbText.toDoubleOrNull() ?: 0.0,
                callMinutes = callsText.toIntOrNull() ?: 0,
                smsCount = smsText.toIntOrNull() ?: 0,
                validityDays = validityText.toIntOrNull()?.coerceAtLeast(1) ?: 7
            )
        )
    }

    val scoreColor = when {
        item.globalScore >= 80.0 -> UploadEmerald
        item.globalScore >= 65.0 -> DownloadCyan
        item.globalScore >= 50.0 -> MobileAmber
        else -> BlockedCoral
    }

    val cardContainer = if (item.isBestChoice) {
        op.brandColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isBestChoice) 4.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("operator_card_${op.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Operator + Global Score Badge + Best choice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(op.brandColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = op.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = offer.planType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isBestChoice) {
                        Surface(
                            color = op.brandColor,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Meilleur choix",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Recommandé",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Score Badge
                    Surface(
                        color = scoreColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Score ",
                                style = MaterialTheme.typography.labelSmall,
                                color = scoreColor
                            )
                            Text(
                                text = "${item.globalScore.roundToInt()}/100",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = scoreColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Badges list (Zéro panne, Débit élevé, etc.)
            if (item.badges.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.badges.forEach { badge ->
                        val bColor = when {
                            badge.contains("Zéro", ignoreCase = true) || badge.contains("Élevé", ignoreCase = true) || badge.contains("Rapide", ignoreCase = true) -> UploadEmerald
                            badge.contains("Fréquentes", ignoreCase = true) || badge.contains("Long", ignoreCase = true) || badge.contains("cours", ignoreCase = true) -> BlockedCoral
                            else -> DownloadCyan
                        }
                        Surface(
                            color = bColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = bColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 3-pillar breakdown: Rapport prix | Qualité réelle constatée | Fiabilité & Pannes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Col 1: Rendement
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Rapport Volume",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        val rendText = when (needType) {
                            OperatorNeedType.INTERNET -> "${item.valuePer100Fcfa.roundToInt()} Mo"
                            OperatorNeedType.CALLS -> "${String.format(Locale.FRANCE, "%.1f", item.valuePer100Fcfa)} min"
                            OperatorNeedType.SMS -> "${item.valuePer100Fcfa.roundToInt()} SMS"
                            OperatorNeedType.MIXED -> "${item.valuePer100Fcfa.roundToInt()} pts"
                        }
                        Text(
                            text = "$rendText / 100 F",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = op.brandColor
                        )
                    }
                }

                // Col 2: Qualité réelle historique
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Débit & Ping Réel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${item.qualityStats.avgDownloadMbps.roundToInt()} Mb/s • ${item.qualityStats.avgLatencyMs}ms",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = DownloadCyan
                        )
                    }
                }

                // Col 3: Fiabilité & Incidents
                val relColor = if (item.incidentStats.reliabilityScore >= 80) UploadEmerald else if (item.incidentStats.reliabilityScore >= 60) MobileAmber else BlockedCoral
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Fiabilité signalée",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${item.incidentStats.reliabilityScore.roundToInt()}% (${item.incidentStats.totalCount} panne)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = relColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Summary text
            Text(
                text = item.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Incident details accordion toggle
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Masquer les détails & tarifs" else "Voir détails des pannes & modifier tarifs",
                    style = MaterialTheme.typography.labelMedium,
                    color = DownloadCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = DownloadCyan
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Incident detailed diagnostic
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ReportProblem,
                                    contentDescription = null,
                                    tint = BlockedCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Impact des signalements enregistrés",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            if (item.incidentStats.totalCount == 0) {
                                Text(
                                    text = "Aucune panne signalée pour cet opérateur. Il bénéficie du bonus maximal de fiabilité (100%).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UploadEmerald
                                )
                            } else {
                                Text(
                                    text = "• Gravité : ${item.incidentStats.criticalCount} critique(s), ${item.incidentStats.majorCount} majeur(s), ${item.incidentStats.moderateCount} modéré(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• Résolution constatée : ${item.incidentStats.temporalDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onReportIncident,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Signaler incident", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = onMeasureQuality,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Enregistrer débit", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Edit Price & Validity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = {
                                priceText = it
                                emitUpdate()
                            },
                            label = { Text("Prix (FCFA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("input_price_${op.name.lowercase()}")
                        )
                        OutlinedTextField(
                            value = validityText,
                            onValueChange = {
                                validityText = it
                                emitUpdate()
                            },
                            label = { Text("Validité (j)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Edit Volumes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (needType == OperatorNeedType.INTERNET || needType == OperatorNeedType.MIXED) {
                            OutlinedTextField(
                                value = dataMbText,
                                onValueChange = {
                                    dataMbText = it
                                    emitUpdate()
                                },
                                label = { Text("Internet (Mo)") },
                                placeholder = { Text("ex: 3000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_data_${op.name.lowercase()}")
                            )
                        }
                        if (needType == OperatorNeedType.CALLS || needType == OperatorNeedType.MIXED) {
                            OutlinedTextField(
                                value = callsText,
                                onValueChange = {
                                    callsText = it
                                    emitUpdate()
                                },
                                label = { Text("Appels (min)") },
                                placeholder = { Text("ex: 60") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_calls_${op.name.lowercase()}")
                            )
                        }
                        if (needType == OperatorNeedType.SMS || needType == OperatorNeedType.MIXED) {
                            OutlinedTextField(
                                value = smsText,
                                onValueChange = {
                                    smsText = it
                                    emitUpdate()
                                },
                                label = { Text("SMS") },
                                placeholder = { Text("ex: 100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_sms_${op.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (item.hasEnteredVolume) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (item.globalScore / 100.0).toFloat().coerceIn(0f, 1f) },
                    color = op.brandColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
