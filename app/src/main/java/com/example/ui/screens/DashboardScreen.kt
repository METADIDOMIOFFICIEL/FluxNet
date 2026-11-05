package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNetworkInfo
import com.example.data.model.ConnectionType
import com.example.data.model.DataPlanSummary
import com.example.data.model.NetworkStatus
import com.example.data.model.SpeedSample
import com.example.ui.components.AppIconImage
import com.example.ui.components.LiveThroughputChart
import com.example.ui.components.MiniSparkline
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    networkStatus: NetworkStatus,
    speedHistory: List<SpeedSample>,
    planSummary: DataPlanSummary,
    topApps: List<AppNetworkInfo>,
    hasUsageStatsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onToggleFirewall: (Boolean) -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onSelectApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header & Connection Status Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FluxNet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Télémétrie réseau en temps réel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ConnectionStatusPill(networkStatus = networkStatus)
            }
        }

        // 2. Data Consumption Alert Banner (if threshold reached or over budget pace)
        item {
            AnimatedVisibility(visible = planSummary.isOverAlertThreshold || planSummary.isOverBudgetPace) {
                val isCritical = planSummary.usagePercent >= 95f
                val bannerColor = if (isCritical) BlockedCoral else MobileAmber
                Surface(
                    color = bannerColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlan() }
                        .testTag("data_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Alerte consommation",
                            tint = bannerColor,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (planSummary.isOverAlertThreshold) {
                                    "Alerte : ${planSummary.usagePercent.toInt()}% du forfait consommés"
                                } else {
                                    "Rythme élevé : dépassement prévu"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = bannerColor
                            )
                            Text(
                                text = "${Formatters.formatBytes(planSummary.usedBytes)} sur ${Formatters.formatBytes(planSummary.quotaBytes)} • Projection : ${Formatters.formatCurrency(planSummary.projectedTotalCost, planSummary.config.currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Permission Prompt Card (Usage Stats / Notifications) if needed
        if (!hasUsageStatsPermission || !hasNotificationPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (!hasUsageStatsPermission) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Historique Wi-Fi & Mobile par app",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Autorisez l'accès aux statistiques pour mesurer chaque app depuis le début du forfait.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = onRequestUsageStatsPermission,
                                    modifier = Modifier.testTag("grant_usage_stats_button")
                                ) {
                                    Text("Autoriser")
                                }
                            }
                        }
                        if (!hasNotificationPermission) {
                            if (!hasUsageStatsPermission) Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Activer les alertes de dépassement",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = onRequestNotificationPermission,
                                    modifier = Modifier.testTag("grant_notif_button")
                                ) {
                                    Text("Activer")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Hero Real-Time Speed & Live 60s Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("realtime_speed_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Download Speed Box
                        SpeedMetricTile(
                            label = "RÉCEPTION (↓)",
                            speedText = Formatters.formatSpeed(networkStatus.downloadBps),
                            subText = "Session : ${Formatters.formatBytes(networkStatus.sessionRxBytes)}",
                            accentColor = DownloadCyan,
                            icon = Icons.Rounded.ArrowDownward,
                            modifier = Modifier.weight(1f)
                        )
                        // Upload Speed Box
                        SpeedMetricTile(
                            label = "ENVOI (↑)",
                            speedText = Formatters.formatSpeed(networkStatus.uploadBps),
                            subText = "Session : ${Formatters.formatBytes(networkStatus.sessionTxBytes)}",
                            accentColor = UploadEmerald,
                            icon = Icons.Rounded.ArrowUpward,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    LiveThroughputChart(
                        samples = speedHistory,
                        maxPoints = 60,
                        showTimeAxis = true
                    )
                }
            }
        }

        // 5. Bouclier Réseau (Firewall VPN) Quick Control Card
        item {
            val firewallOn = planSummary.config.firewallEnabled
            val blockedCount = topApps.count { it.isCurrentlyBlocked }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (firewallOn) {
                        UploadEmerald.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firewall_control_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (firewallOn) UploadEmerald.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (firewallOn) Icons.Rounded.Shield else Icons.Rounded.Security,
                                contentDescription = "Bouclier Réseau",
                                tint = if (firewallOn) UploadEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bouclier Réseau",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (firewallOn) {
                                    "Actif • $blockedCount app(s) bloquée(s) sur ${networkStatus.connectionType.labelFr}"
                                } else {
                                    "Inactif • Activez pour bloquer Wi-Fi / Mobile / Arrière-plan"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = firewallOn,
                        onCheckedChange = onToggleFirewall,
                        modifier = Modifier.testTag("firewall_switch")
                    )
                }
            }
        }

        // 6. Data Plan & Expense Estimation Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPlan() }
                    .testTag("plan_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Forfait • ${planSummary.config.operatorName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Jour ${planSummary.daysElapsed}/${planSummary.config.validityDays} • ${planSummary.daysRemaining}j restants",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onNavigateToPlan) {
                            Text("Configurer")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val progressColor = when {
                        planSummary.usagePercent >= 90f -> BlockedCoral
                        planSummary.usagePercent >= planSummary.config.alertThresholdPercent -> MobileAmber
                        else -> DownloadCyan
                    }

                    LinearProgressIndicator(
                        progress = { (planSummary.usagePercent / 100f).coerceIn(0f, 1f) },
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${Formatters.formatBytes(planSummary.usedBytes)} utilisés",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${Formatters.formatBytes(planSummary.remainingBytes)} restants",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Cost Estimation Row
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Dépense estimée",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(
                                        planSummary.estimatedCostUsed,
                                        planSummary.config.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Moy. / jour",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(
                                        planSummary.dailyAverageCost,
                                        planSummary.config.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Coût forfait",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(
                                        planSummary.config.cost,
                                        planSummary.config.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = DownloadCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Top Active Apps Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applications actives",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToApps,
                    modifier = Modifier.testTag("see_all_apps_button")
                ) {
                    Text("Tout voir (${topApps.size})")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        val previewApps = topApps
            .sortedWith(compareByDescending<AppNetworkInfo> { it.totalSpeedBps }.thenByDescending { it.totalBytes })
            .take(4)

        items(previewApps, key = { it.packageName }) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectApp(app.packageName) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(drawable = app.icon, contentDescription = app.appName, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total : ${Formatters.formatBytes(app.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MiniSparkline(
                        samples = app.speedHistory,
                        modifier = Modifier
                            .width(52.dp)
                            .height(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "↓ ${Formatters.formatSpeed(app.downloadBps)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = DownloadCyan
                        )
                        Text(
                            text = "↑ ${Formatters.formatSpeed(app.uploadBps)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = UploadEmerald
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConnectionStatusPill(networkStatus: NetworkStatus) {
    val (bgColor, contentColor, icon) = when (networkStatus.connectionType) {
        ConnectionType.WIFI -> Triple(
            DownloadCyan.copy(alpha = 0.16f),
            DownloadCyan,
            Icons.Rounded.Wifi
        )
        ConnectionType.MOBILE -> Triple(
            MobileAmber.copy(alpha = 0.16f),
            MobileAmber,
            Icons.Rounded.CellTower
        )
        ConnectionType.ETHERNET -> Triple(
            UploadEmerald.copy(alpha = 0.16f),
            UploadEmerald,
            Icons.Rounded.Wifi
        )
        ConnectionType.OFFLINE -> Triple(
            BlockedCoral.copy(alpha = 0.16f),
            BlockedCoral,
            Icons.Rounded.WifiOff
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("connection_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = networkStatus.networkName,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = networkStatus.networkName,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SpeedMetricTile(
    label: String,
    speedText: String,
    subText: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = speedText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
