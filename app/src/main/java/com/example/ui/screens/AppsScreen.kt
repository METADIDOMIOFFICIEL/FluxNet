package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNetworkInfo
import com.example.data.model.SystemRamInfo
import com.example.ui.AppFilter
import com.example.ui.AppSort
import com.example.ui.components.AppIconImage
import com.example.ui.components.LiveThroughputChart
import com.example.ui.components.MiniSparkline
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    apps: List<AppNetworkInfo>,
    searchQuery: String,
    selectedFilter: AppFilter,
    selectedSort: AppSort,
    autoBlockBackgroundHeavy: Boolean,
    firewallEnabled: Boolean,
    systemRamInfo: SystemRamInfo,
    selectedAppDetail: AppNetworkInfo?,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (AppFilter) -> Unit,
    onSelectSort: (AppSort) -> Unit,
    onToggleAutoBlockBackground: (Boolean) -> Unit,
    onCleanMemory: () -> Unit,
    onCleanAppMemory: (String) -> Unit,
    onSetRamThreshold: (Int) -> Unit,
    onToggleRamShield: (Boolean) -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onSelectApp: (String?) -> Unit,
    onUpdateAppRule: (
        packageName: String,
        wifiAllowed: Boolean,
        mobileAllowed: Boolean,
        blockBackground: Boolean,
        autoWatch: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("apps_screen")
    ) {
        // Header & Search
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Applications (${apps.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Débit détaillé & contrôle Wi-Fi / Mobile / Arrière-plan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Rechercher une application...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Rechercher")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apps_search_input")
            )

            Spacer(Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.labelFr) },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            // Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trier :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppSort.entries.forEach { sort ->
                    FilterChip(
                        selected = selectedSort == sort,
                        onClick = { onSelectSort(sort) },
                        label = { Text(sort.labelFr) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Anti-Slowness RAM Shield Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ram_shield_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = DownloadCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Memory,
                                            contentDescription = null,
                                            tint = DownloadCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bouclier Anti-Lenteur (RAM)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Surveillance et libération mémoire automatique",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = systemRamInfo.ramShieldEnabled,
                                onCheckedChange = onToggleRamShield,
                                modifier = Modifier.testTag("toggle_ram_shield_switch")
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // RAM usage indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAM utilisée : ${systemRamInfo.usedRamMb} Mo / ${systemRamInfo.totalRamMb} Mo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${systemRamInfo.usedPercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (systemRamInfo.usedPercent > 80) BlockedCoral else DownloadCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Progress Bar
                        val ramProgress = (systemRamInfo.usedPercent / 100f).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ramProgress)
                                    .height(8.dp)
                                    .background(if (systemRamInfo.usedPercent > 80) BlockedCoral else DownloadCyan)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Threshold slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seuil d'alerte lenteur :",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${systemRamInfo.ramThresholdMb} Mo",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = systemRamInfo.ramThresholdMb.toFloat(),
                            onValueChange = { onSetRamThreshold(it.toInt()) },
                            valueRange = 200f..800f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = DownloadCyan,
                                activeTrackColor = DownloadCyan
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ram_threshold_slider")
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (systemRamInfo.lastCleanReclaimedMb > 0L) {
                                Text(
                                    text = "Dernière purge : +${systemRamInfo.lastCleanReclaimedMb} Mo libérés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UploadEmerald
                                )
                            } else {
                                Text(
                                    text = "${apps.count { it.isRamHeavy }} app(s) dépassent le seuil",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (apps.any { it.isRamHeavy }) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = onCleanMemory,
                                colors = ButtonDefaults.buttonColors(containerColor = DownloadCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("clean_ram_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Libérer la RAM", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Anti-Kill & Anti-Uninstall Security Shield Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (systemRamInfo.isAntiUninstallActive) {
                            UploadEmerald.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anti_uninstall_shield_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (systemRamInfo.isAntiUninstallActive) UploadEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = if (systemRamInfo.isAntiUninstallActive) UploadEmerald else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Protection Anti-Désinstallation & Anti-Arrêt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (systemRamInfo.isAntiUninstallActive) {
                                        "Verrouillage administrateur actif • Désinstallation et arrêt forcé bloqués"
                                    } else {
                                        "Activer le statut Admin pour empêcher la désinstallation de l'app"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = UploadEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "START_STICKY ACTIF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = UploadEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Auto-relance garantie",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!systemRamInfo.isAntiUninstallActive) {
                                OutlinedButton(
                                    onClick = onRequestDeviceAdmin,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("activate_admin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Activer Admin")
                                }
                            } else {
                                Surface(
                                    color = UploadEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "✓ Verrouillé",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = UploadEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Auto-block background-heavy apps banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoMode,
                                contentDescription = null,
                                tint = DownloadCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Blocage auto réseau en arrière-plan",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (firewallEnabled) {
                                        "Restreint automatiquement les apps gourmandes"
                                    } else {
                                        "Activez le Bouclier Réseau pour appliquer le blocage"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = autoBlockBackgroundHeavy,
                            onCheckedChange = onToggleAutoBlockBackground,
                            modifier = Modifier.testTag("auto_block_bg_switch")
                        )
                    }
                }
            }

            if (apps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune application ne correspond à ce filtre.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(apps, key = { it.packageName }) { app ->
                    AppNetworkRowCard(
                        app = app,
                        onClick = { onSelectApp(app.packageName) },
                        onCleanApp = { onCleanAppMemory(app.packageName) },
                        onToggleWifi = {
                            onUpdateAppRule(
                                app.packageName,
                                !app.wifiAllowed,
                                app.mobileAllowed,
                                app.blockBackground,
                                app.autoWatch
                            )
                        },
                        onToggleMobile = {
                            onUpdateAppRule(
                                app.packageName,
                                app.wifiAllowed,
                                !app.mobileAllowed,
                                app.blockBackground,
                                app.autoWatch
                            )
                        },
                        onToggleBackground = {
                            onUpdateAppRule(
                                app.packageName,
                                app.wifiAllowed,
                                app.mobileAllowed,
                                !app.blockBackground,
                                app.autoWatch
                            )
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Detailed App Throughput & Control Bottom Sheet
    if (selectedAppDetail != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onSelectApp(null) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppDetailBottomSheetContent(
                app = selectedAppDetail,
                firewallEnabled = firewallEnabled,
                onCleanAppMemory = onCleanAppMemory,
                onUpdateAppRule = onUpdateAppRule
            )
        }
    }
}

@Composable
private fun AppNetworkRowCard(
    app: AppNetworkInfo,
    onClick: () -> Unit,
    onCleanApp: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
    onToggleBackground: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_card_${app.packageName}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(drawable = app.icon, contentDescription = app.appName, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isCurrentlyBlocked) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = BlockedCoral.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "BLOQUÉE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlockedCoral,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Wi-Fi ${Formatters.formatBytes(app.wifiBytes)} • Mobile ${Formatters.formatBytes(app.mobileBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                MiniSparkline(
                    samples = app.speedHistory,
                    modifier = Modifier
                        .width(50.dp)
                        .height(22.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "↓ ${Formatters.formatSpeed(app.downloadBps)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                        color = DownloadCyan
                    )
                    Text(
                        text = "↑ ${Formatters.formatSpeed(app.uploadBps)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = UploadEmerald
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // RAM status and Quick Access Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (app.isRamHeavy) BlockedCoral.copy(alpha = 0.16f) else DownloadCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = if (app.isRamHeavy) BlockedCoral else DownloadCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${app.ramUsageMb.toInt()} Mo RAM",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (app.isRamHeavy) BlockedCoral else DownloadCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (app.isRamHeavy) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = BlockedCoral.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LENTEUR DÉTECTÉE",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlockedCoral,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCleanApp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = "Purger RAM",
                            modifier = Modifier.size(14.dp),
                            tint = DownloadCyan
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Purger RAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Quick Per-App Access Pills (Wi-Fi, Mobile, Background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAccessPill(
                    label = if (app.wifiAllowed) "Wi-Fi OK" else "Wi-Fi Bloqué",
                    isAllowed = app.wifiAllowed,
                    icon = Icons.Rounded.Wifi,
                    onClick = onToggleWifi,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessPill(
                    label = if (app.mobileAllowed) "Mobile OK" else "Mobile Bloqué",
                    isAllowed = app.mobileAllowed,
                    icon = Icons.Rounded.CellTower,
                    onClick = onToggleMobile,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessPill(
                    label = if (app.blockBackground) "Arrière-plan Bloqué" else "Arrière-plan OK",
                    isAllowed = !app.blockBackground,
                    icon = Icons.Rounded.Block,
                    onClick = onToggleBackground,
                    modifier = Modifier.weight(1.25f)
                )
            }
        }
    }
}

@Composable
private fun QuickAccessPill(
    label: String,
    isAllowed: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isAllowed) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        BlockedCoral.copy(alpha = 0.16f)
    }
    val fg = if (isAllowed) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        BlockedCoral
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppDetailBottomSheetContent(
    app: AppNetworkInfo,
    firewallEnabled: Boolean,
    onCleanAppMemory: (String) -> Unit,
    onUpdateAppRule: (
        packageName: String,
        wifiAllowed: Boolean,
        mobileAllowed: Boolean,
        blockBackground: Boolean,
        autoWatch: Boolean
    ) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .testTag("app_detail_sheet")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(drawable = app.icon, contentDescription = app.appName, size = 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // RAM & Anti-slowness status
        Surface(
            color = if (app.isRamHeavy) BlockedCoral.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = if (app.isRamHeavy) BlockedCoral else DownloadCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mémoire RAM consommée : ${app.ramUsageMb.toInt()} Mo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (app.isRamHeavy) "Attention : dépassement du seuil de lenteur" else "Empreinte mémoire normale",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (app.isRamHeavy) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { onCleanAppMemory(app.packageName) },
                    colors = ButtonDefaults.buttonColors(containerColor = DownloadCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Purger", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Live & Peak Speeds Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatBox(
                title = "Débit actuel",
                value = "↓ ${Formatters.formatSpeed(app.downloadBps)}",
                subValue = "↑ ${Formatters.formatSpeed(app.uploadBps)}",
                accent = DownloadCyan,
                modifier = Modifier.weight(1f)
            )
            DetailStatBox(
                title = "Pic enregistré",
                value = "↓ ${Formatters.formatSpeed(app.peakDownloadBps)}",
                subValue = "↑ ${Formatters.formatSpeed(app.peakUploadBps)}",
                accent = UploadEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Detailed Per-App Throughput Chart
        Text(
            text = "Graphique détaillé du débit (45 dernières secondes)",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        LiveThroughputChart(
            samples = app.speedHistory,
            maxPoints = 30,
            showTimeAxis = true
        )

        Spacer(Modifier.height(16.dp))

        // Wi-Fi vs Mobile Data Breakdown
        val total = app.totalBytes.coerceAtLeast(1L)
        val wifiRatio = (app.wifiBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        Text(
            text = "Répartition Wi-Fi / Données Mobiles",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MobileAmber)
        ) {
            if (wifiRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(wifiRatio)
                        .height(10.dp)
                        .background(DownloadCyan)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Wi-Fi : ${Formatters.formatBytes(app.wifiBytes)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = DownloadCyan
            )
            Text(
                text = "Mobile : ${Formatters.formatBytes(app.mobileBytes)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MobileAmber
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            text = if (firewallEnabled) {
                "Contrôles d'accès réseau (Bouclier actif)"
            } else {
                "Contrôles d'accès réseau (Activez le Bouclier sur le Tableau)"
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        RuleSwitchRow(
            title = "Autoriser le Wi-Fi",
            subtitle = "Permettre à l'application d'utiliser la connexion Wi-Fi",
            checked = app.wifiAllowed,
            onCheckedChange = {
                onUpdateAppRule(
                    app.packageName,
                    it,
                    app.mobileAllowed,
                    app.blockBackground,
                    app.autoWatch
                )
            }
        )
        RuleSwitchRow(
            title = "Autoriser les données mobiles",
            subtitle = "Permettre l'utilisation du forfait 4G/5G",
            checked = app.mobileAllowed,
            onCheckedChange = {
                onUpdateAppRule(
                    app.packageName,
                    app.wifiAllowed,
                    it,
                    app.blockBackground,
                    app.autoWatch
                )
            }
        )
        RuleSwitchRow(
            title = "Bloquer en arrière-plan",
            subtitle = "Couper l'accès internet lorsque l'app n'est pas ouverte",
            checked = app.blockBackground,
            onCheckedChange = {
                onUpdateAppRule(
                    app.packageName,
                    app.wifiAllowed,
                    app.mobileAllowed,
                    it,
                    app.autoWatch
                )
            }
        )
        RuleSwitchRow(
            title = "Surveillance automatique",
            subtitle = "Bloquer automatiquement si pic de consommation en arrière-plan",
            checked = app.autoWatch,
            onCheckedChange = {
                onUpdateAppRule(
                    app.packageName,
                    app.wifiAllowed,
                    app.mobileAllowed,
                    app.blockBackground,
                    it
                )
            }
        )
    }
}

@Composable
private fun DetailStatBox(
    title: String,
    value: String,
    subValue: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RuleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
