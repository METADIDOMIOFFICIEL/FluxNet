package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConnectionType
import com.example.data.model.NetworkStatus
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import com.example.util.Formatters

@Composable
fun AppDrawerSheet(
    currentRoute: String,
    versionName: String = "1.0",
    copyrightText: String = "Copyright METADIDOMI 2026",
    networkStatus: NetworkStatus,
    firewallEnabled: Boolean,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(330.dp)
            .testTag("app_navigation_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Drawer Header with Gradient, App Identity & Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DownloadCyan.copy(alpha = 0.25f),
                                MobileAmber.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = DownloadCyan,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FluxNet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = DownloadCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Version $versionName",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = DownloadCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.testTag("close_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Fermer le volet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Contrôle & télémétrie de connexion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. Network & Firewall Quick Status Pill
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val netIcon = when (networkStatus.connectionType) {
                                ConnectionType.WIFI -> Icons.Rounded.Wifi
                                ConnectionType.MOBILE -> Icons.Rounded.CellTower
                                else -> Icons.Rounded.CellTower
                            }
                            val netColor = if (networkStatus.connectionType != ConnectionType.OFFLINE) UploadEmerald else BlockedCoral
                            Icon(
                                imageVector = netIcon,
                                contentDescription = null,
                                tint = netColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = networkStatus.networkName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = Formatters.formatSpeed(networkStatus.downloadBps),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = DownloadCyan
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (firewallEnabled) Icons.Rounded.Shield else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (firewallEnabled) UploadEmerald else MobileAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bouclier Réseau :",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (firewallEnabled) "Actif" else "Désactivé",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (firewallEnabled) UploadEmerald else MobileAmber
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. Navigation items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Tableau de bord (Débit direct)") },
                    icon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        onNavigate("dashboard")
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = DownloadCyan.copy(alpha = 0.15f),
                        selectedTextColor = DownloadCyan,
                        selectedIconColor = DownloadCyan
                    ),
                    modifier = Modifier.testTag("drawer_nav_dashboard")
                )

                NavigationDrawerItem(
                    label = { Text("Applications & Bouclier") },
                    icon = { Icon(Icons.Rounded.Apps, contentDescription = null) },
                    selected = currentRoute == "apps",
                    onClick = {
                        onNavigate("apps")
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = DownloadCyan.copy(alpha = 0.15f),
                        selectedTextColor = DownloadCyan,
                        selectedIconColor = DownloadCyan
                    ),
                    modifier = Modifier.testTag("drawer_nav_apps")
                )

                NavigationDrawerItem(
                    label = { Text("Forfait & Comparateur") },
                    icon = { Icon(Icons.Rounded.DataUsage, contentDescription = null) },
                    selected = currentRoute == "plan",
                    onClick = {
                        onNavigate("plan")
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = DownloadCyan.copy(alpha = 0.15f),
                        selectedTextColor = DownloadCyan,
                        selectedIconColor = DownloadCyan
                    ),
                    modifier = Modifier.testTag("drawer_nav_plan")
                )

                NavigationDrawerItem(
                    label = { Text("Communauté & Signalements") },
                    icon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    selected = currentRoute == "community",
                    onClick = {
                        onNavigate("community")
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = DownloadCyan.copy(alpha = 0.15f),
                        selectedTextColor = DownloadCyan,
                        selectedIconColor = DownloadCyan
                    ),
                    modifier = Modifier.testTag("drawer_nav_community")
                )

                Spacer(Modifier.height(10.dp))

                // Informative project card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = DownloadCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "À propos de FluxNet",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Analyse en temps réel de votre connexion, suivi du budget forfait et comparateur basé sur votre historique réel de pannes et qualité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Footer with Version & Copyright METADIDOMI 2026
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_footer"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = DownloadCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "FluxNet v$versionName",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Copyright,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = copyrightText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Tous droits réservés",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
