package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppDrawerSheet
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.network.NotificationHelper
import com.example.ui.FluxNetViewModel
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.theme.FluxNetTheme
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.Public
import kotlinx.coroutines.launch

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_APPS = "apps"
private const val ROUTE_PLAN = "plan"
private const val ROUTE_COMMUNITY = "community"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxNetTheme {
                FluxNetApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluxNetApp(
    viewModel: FluxNetViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val speedHistory by viewModel.globalSpeedHistory.collectAsStateWithLifecycle()
    val planSummary by viewModel.planSummary.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val selectedAppDetail by viewModel.selectedAppDetail.collectAsStateWithLifecycle()
    val hasUsageStatsPermission by viewModel.hasUsageStatsPermission.collectAsStateWithLifecycle()
    val userIncidents by viewModel.userIncidents.collectAsStateWithLifecycle()
    val networkQualityRecords by viewModel.networkQualityRecords.collectAsStateWithLifecycle()
    val systemRamInfo by viewModel.systemRamInfo.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setFirewallEnabled(true)
        } else {
            viewModel.setFirewallEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar("Autorisation VPN requise pour activer le Bouclier Réseau")
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
        scope.launch {
            snackbarHostState.showSnackbar("Statut protection administrateur mis à jour")
        }
    }

    val onRequestDeviceAdmin: () -> Unit = {
        val adminReceiver = android.content.ComponentName(context, com.example.service.FluxNetDeviceAdminReceiver::class.java)
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver)
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "L'activation de l'administration de l'appareil verrouille le bouton de désinstallation et empêche l'arrêt forcé de l'application."
            )
        }
        runCatching {
            deviceAdminLauncher.launch(intent)
        }.onFailure {
            scope.launch {
                snackbarHostState.showSnackbar("Impossible d'ouvrir l'activation Administrateur sur cet appareil.")
            }
        }
    }

    val onToggleFirewall: (Boolean) -> Unit = { enable ->
        if (!enable) {
            viewModel.setFirewallEnabled(false)
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnConsentLauncher.launch(prepareIntent)
            } else {
                viewModel.setFirewallEnabled(true)
            }
        }
    }

    val onRequestUsageStats: () -> Unit = {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    val onRequestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val destinations = remember {
        listOf(
            NavDestination(
                route = ROUTE_DASHBOARD,
                labelRes = R.string.nav_dashboard,
                selectedIcon = Icons.Rounded.Speed,
                unselectedIcon = Icons.Outlined.Speed,
                testTag = "nav_dashboard"
            ),
            NavDestination(
                route = ROUTE_APPS,
                labelRes = R.string.nav_apps,
                selectedIcon = Icons.Rounded.Apps,
                unselectedIcon = Icons.Outlined.Apps,
                testTag = "nav_apps"
            ),
            NavDestination(
                route = ROUTE_PLAN,
                labelRes = R.string.nav_plan,
                selectedIcon = Icons.Rounded.DataUsage,
                unselectedIcon = Icons.Outlined.DataUsage,
                testTag = "nav_plan"
            ),
            NavDestination(
                route = ROUTE_COMMUNITY,
                labelRes = R.string.nav_community,
                selectedIcon = Icons.Rounded.Public,
                unselectedIcon = Icons.Outlined.Public,
                testTag = "nav_community"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerSheet(
                currentRoute = currentRoute,
                versionName = "1.0",
                copyrightText = "Copyright METADIDOMI 2026",
                networkStatus = networkStatus,
                firewallEnabled = planSummary.config.firewallEnabled,
                onNavigate = { route ->
                    navigateToTab(route)
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (currentRoute) {
                                    ROUTE_DASHBOARD -> "Tableau de bord"
                                    ROUTE_APPS -> "Applications & Bouclier"
                                    ROUTE_PLAN -> "Forfait & Réseau"
                                    ROUTE_COMMUNITY -> "Communauté & Entraide"
                                    else -> "FluxNet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Ouvrir le volet latéral"
                            )
                        }
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "METADIDOMI 2026",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    destinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    val label = stringResource(dest.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(dest.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_DASHBOARD
            ) {
                composable(ROUTE_DASHBOARD) {
                    DashboardScreen(
                        networkStatus = networkStatus,
                        speedHistory = speedHistory,
                        planSummary = planSummary,
                        topApps = allApps,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onToggleFirewall = onToggleFirewall,
                        onRequestUsageStatsPermission = onRequestUsageStats,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onNavigateToApps = { navigateToTab(ROUTE_APPS) },
                        onNavigateToPlan = { navigateToTab(ROUTE_PLAN) },
                        onSelectApp = { pkg ->
                            viewModel.selectApp(pkg)
                            navigateToTab(ROUTE_APPS)
                        }
                    )
                }

                composable(ROUTE_APPS) {
                    AppsScreen(
                        apps = filteredApps,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        selectedSort = selectedSort,
                        autoBlockBackgroundHeavy = planSummary.config.autoBlockBackgroundHeavy,
                        firewallEnabled = planSummary.config.firewallEnabled,
                        systemRamInfo = systemRamInfo,
                        selectedAppDetail = selectedAppDetail,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onSelectFilter = viewModel::setFilter,
                        onSelectSort = viewModel::setSort,
                        onToggleAutoBlockBackground = { enabled ->
                            viewModel.updateAutoBlockBackground(enabled, null)
                        },
                        onCleanMemory = {
                            val reclaimed = viewModel.cleanMemory()
                            scope.launch {
                                snackbarHostState.showSnackbar("Bouclier Anti-Lenteur : +$reclaimed Mo de RAM libérés !")
                            }
                        },
                        onCleanAppMemory = { pkg ->
                            val reclaimed = viewModel.cleanAppMemory(pkg)
                            scope.launch {
                                snackbarHostState.showSnackbar("Processus purgé : +$reclaimed Mo libérés")
                            }
                        },
                        onSetRamThreshold = viewModel::setRamThreshold,
                        onToggleRamShield = viewModel::toggleRamShield,
                        onRequestDeviceAdmin = onRequestDeviceAdmin,
                        onSelectApp = viewModel::selectApp,
                        onUpdateAppRule = viewModel::updateAppRule
                    )
                }

                composable(ROUTE_PLAN) {
                    val operatorOffers by viewModel.operatorOffers.collectAsStateWithLifecycle()
                    PlanScreen(
                        planSummary = planSummary,
                        operatorOffers = operatorOffers,
                        userIncidents = userIncidents,
                        networkQualityRecords = networkQualityRecords,
                        snackbarHostState = snackbarHostState,
                        onSavePlan = viewModel::updatePlan,
                        onSaveOperatorOffer = viewModel::saveOperatorOffer,
                        onUpdateAlertThreshold = viewModel::updateAlertThreshold,
                        onUpdateAutoBlockBackground = viewModel::updateAutoBlockBackground,
                        onAddIncident = viewModel::addIncident,
                        onDeleteIncident = viewModel::deleteIncident,
                        onMeasureQuality = viewModel::recordCurrentNetworkQuality
                    )
                }

                composable(ROUTE_COMMUNITY) {
                    val communityPosts by viewModel.communityPosts.collectAsStateWithLifecycle()
                    CommunityScreen(
                        posts = communityPosts,
                        snackbarHostState = snackbarHostState,
                        onGetComments = viewModel::getCommentsForPost,
                        onPublishPost = viewModel::publishCommunityPost,
                        onAddComment = viewModel::addComment,
                        onToggleLikePost = viewModel::toggleLikePost,
                        onToggleLikeComment = viewModel::toggleLikeComment,
                        onCopyPlanToComparator = viewModel::copyPlanToComparator,
                        onNavigateToComparator = { navigateToTab(ROUTE_PLAN) }
                    )
                }
            }
        }
    }
}
}
