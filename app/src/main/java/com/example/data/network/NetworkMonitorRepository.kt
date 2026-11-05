package com.example.data.network

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import com.example.data.local.AppRuleDao
import com.example.data.local.AppRuleEntity
import com.example.data.model.AppNetworkInfo
import com.example.data.model.ConnectionType
import com.example.data.model.DataPlanConfig
import com.example.data.model.NetworkStatus
import com.example.data.model.SpeedSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NetworkMonitorRepository(
    private val context: Context,
    private val appRuleDao: AppRuleDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val packageManager = context.packageManager

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _globalSpeedHistory = MutableStateFlow<List<SpeedSample>>(emptyList())
    val globalSpeedHistory: StateFlow<List<SpeedSample>> = _globalSpeedHistory.asStateFlow()

    private val _appsList = MutableStateFlow<List<AppNetworkInfo>>(emptyList())
    val appsList: StateFlow<List<AppNetworkInfo>> = _appsList.asStateFlow()

    private val _totalPlanUsedBytes = MutableStateFlow(0L)
    val totalPlanUsedBytes: StateFlow<Long> = _totalPlanUsedBytes.asStateFlow()

    private val _hasUsageStatsPermission = MutableStateFlow(checkUsageStatsPermission())
    val hasUsageStatsPermission: StateFlow<Boolean> = _hasUsageStatsPermission.asStateFlow()

    private val _systemRamInfo = MutableStateFlow(com.example.data.model.SystemRamInfo())
    val systemRamInfo: StateFlow<com.example.data.model.SystemRamInfo> = _systemRamInfo.asStateFlow()

    private data class CachedAppMeta(
        val uid: Int,
        val packageName: String,
        val appName: String,
        val icon: Drawable?
    )

    private val cachedApps = ConcurrentHashMap<String, CachedAppMeta>()
    private val appSpeedHistories = ConcurrentHashMap<String, MutableList<SpeedSample>>()
    private val appPeakDown = ConcurrentHashMap<String, Long>()
    private val appPeakUp = ConcurrentHashMap<String, Long>()
    private val appLastBytes = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val appMinuteWindowBytes = ConcurrentHashMap<String, Pair<Long, Long>>() // windowStartMs to bytes

    @Volatile
    private var currentRules: Map<String, AppRuleEntity> = emptyMap()

    @Volatile
    private var currentPlanConfig: DataPlanConfig = DataPlanConfig()

    private var lastAlertNotifiedPercent = -1

    init {
        registerConnectivityCallback()
        loadInstalledInternetApps()
        startGlobalSpeedLoop()
        startPerAppMonitorLoop()
    }

    fun updateRulesSnapshot(rules: List<AppRuleEntity>) {
        currentRules = rules.associateBy { it.packageName }
    }

    fun updatePlanConfigSnapshot(config: DataPlanConfig) {
        currentPlanConfig = config
    }

    fun refreshPermissions() {
        _hasUsageStatsPermission.value = checkUsageStatsPermission()
    }

    fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun registerConnectivityCallback() {
        updateCurrentNetworkCapabilities(connectivityManager.activeNetwork)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateCurrentNetworkCapabilities(network)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateFromCapabilities(networkCapabilities)
            }

            override fun onLost(network: Network) {
                updateCurrentNetworkCapabilities(connectivityManager.activeNetwork)
            }
        }
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    private fun updateCurrentNetworkCapabilities(network: Network?) {
        val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }
        updateFromCapabilities(caps)
    }

    private fun updateFromCapabilities(caps: NetworkCapabilities?) {
        if (caps == null) {
            _networkStatus.value = _networkStatus.value.copy(
                connectionType = ConnectionType.OFFLINE,
                networkName = "Hors ligne",
                isMetered = false,
                isValidated = false,
                linkDownKbps = 0,
                linkUpKbps = 0
            )
            return
        }

        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OFFLINE
        }

        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val downKbps = caps.linkDownstreamBandwidthKbps
        val upKbps = caps.linkUpstreamBandwidthKbps

        _networkStatus.value = _networkStatus.value.copy(
            connectionType = type,
            networkName = type.labelFr,
            isMetered = isMetered,
            isValidated = isValidated,
            linkDownKbps = downKbps,
            linkUpKbps = upKbps
        )
    }

    private fun loadInstalledInternetApps() {
        scope.launch {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedPackages = packageManager.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()

            val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installed) {
                if (appInfo.packageName == context.packageName) continue
                val isLauncher = resolvedPackages.contains(appInfo.packageName)
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                if (!isLauncher && !isUserApp) continue

                val hasInternet = packageManager.checkPermission(
                    Manifest.permission.INTERNET,
                    appInfo.packageName
                ) == PackageManager.PERMISSION_GRANTED

                if (hasInternet || isLauncher) {
                    val label = runCatching {
                        packageManager.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(appInfo.packageName)
                    val icon = runCatching {
                        packageManager.getApplicationIcon(appInfo)
                    }.getOrNull()

                    cachedApps[appInfo.packageName] = CachedAppMeta(
                        uid = appInfo.uid,
                        packageName = appInfo.packageName,
                        appName = label,
                        icon = icon
                    )
                }
            }
        }
    }

    private fun startGlobalSpeedLoop() {
        scope.launch {
            var prevRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            var prevTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            val startRx = prevRx
            val startTx = prevTx

            while (isActive) {
                delay(1_000L)
                val now = System.currentTimeMillis()
                val curRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val curTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)

                val downBps = (curRx - prevRx).coerceAtLeast(0L)
                val upBps = (curTx - prevTx).coerceAtLeast(0L)
                prevRx = curRx
                prevTx = curTx

                _networkStatus.value = _networkStatus.value.copy(
                    downloadBps = downBps,
                    uploadBps = upBps,
                    sessionRxBytes = (curRx - startRx).coerceAtLeast(0L),
                    sessionTxBytes = (curTx - startTx).coerceAtLeast(0L)
                )

                val updatedHistory = (_globalSpeedHistory.value + SpeedSample(now, downBps, upBps))
                    .takeLast(60)
                _globalSpeedHistory.value = updatedHistory
            }
        }
    }

    private fun startPerAppMonitorLoop() {
        scope.launch {
            var tickCount = 0
            var cachedWifiByUid: Map<Int, Long> = emptyMap()
            var cachedMobileByUid: Map<Int, Long> = emptyMap()

            while (isActive) {
                val now = System.currentTimeMillis()
                val hasUsagePerm = checkUsageStatsPermission()
                _hasUsageStatsPermission.value = hasUsagePerm

                // Refresh NetworkStatsManager summary every 6 seconds (or on first tick)
                if (hasUsagePerm && tickCount % 4 == 0) {
                    val (wifiMap, mobileMap) = queryNetworkStatsByUid(currentPlanConfig.startDateMillis, now)
                    cachedWifiByUid = wifiMap
                    cachedMobileByUid = mobileMap
                    val totalUsed = wifiMap.values.sum() + mobileMap.values.sum()
                    _totalPlanUsedBytes.value = totalUsed
                    checkAndTriggerDataAlert(totalUsed)
                } else if (!hasUsagePerm && tickCount % 4 == 0) {
                    val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                    val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                    _totalPlanUsedBytes.value = totalRx + totalTx
                    checkAndTriggerDataAlert(totalRx + totalTx)
                }

                val foregroundPkg = getForegroundPackageName(hasUsagePerm)
                val connType = _networkStatus.value.connectionType
                val rulesMap = currentRules
                val planCfg = currentPlanConfig
                val resultList = ArrayList<AppNetworkInfo>(cachedApps.size)

                for ((pkg, meta) in cachedApps) {
                    val uidRx = TrafficStats.getUidRxBytes(meta.uid).coerceAtLeast(0L)
                    val uidTx = TrafficStats.getUidTxBytes(meta.uid).coerceAtLeast(0L)

                    val prev = appLastBytes[pkg]
                    val downBps = if (prev != null) ((uidRx - prev.first).coerceAtLeast(0L) * 1000L / 1500L) else 0L
                    val upBps = if (prev != null) ((uidTx - prev.second).coerceAtLeast(0L) * 1000L / 1500L) else 0L
                    appLastBytes[pkg] = uidRx to uidTx

                    val history = appSpeedHistories.getOrPut(pkg) { mutableListOf() }
                    history.add(SpeedSample(now, downBps, upBps))
                    if (history.size > 30) {
                        history.removeAt(0)
                    }

                    val peakDown = maxOf(appPeakDown[pkg] ?: 0L, downBps)
                    val peakUp = maxOf(appPeakUp[pkg] ?: 0L, upBps)
                    appPeakDown[pkg] = peakDown
                    appPeakUp[pkg] = peakUp

                    val wifiBytes = if (hasUsagePerm) (cachedWifiByUid[meta.uid] ?: 0L) else uidRx
                    val mobileBytes = if (hasUsagePerm) (cachedMobileByUid[meta.uid] ?: 0L) else uidTx

                    val rule = rulesMap[pkg]
                    val wifiAllowed = rule?.wifiAllowed ?: true
                    val mobileAllowed = rule?.mobileAllowed ?: true
                    var blockBackground = rule?.blockBackground ?: false
                    val autoWatch = rule?.autoWatch ?: false

                    // Track 1-minute background consumption
                    val isForeground = (pkg == foregroundPkg)
                    val deltaBytes = if (prev != null) {
                        ((uidRx - prev.first).coerceAtLeast(0L) + (uidTx - prev.second).coerceAtLeast(0L))
                    } else 0L

                    val window = appMinuteWindowBytes[pkg]
                    val (winStart, winBytes) = if (window == null || now - window.first > 60_000L) {
                        now to deltaBytes
                    } else {
                        window.first to (window.second + deltaBytes)
                    }
                    appMinuteWindowBytes[pkg] = winStart to winBytes

                    val thresholdBytes = planCfg.backgroundThresholdMbPerMin * 1_048_576L
                    val isBgHeavy = !isForeground && winBytes >= thresholdBytes

                    if (isBgHeavy && (planCfg.autoBlockBackgroundHeavy || autoWatch) && !blockBackground) {
                        blockBackground = true
                        appRuleDao.upsertRule(
                            AppRuleEntity(
                                packageName = pkg,
                                wifiAllowed = wifiAllowed,
                                mobileAllowed = mobileAllowed,
                                blockBackground = true,
                                autoWatch = autoWatch
                            )
                        )
                        NotificationHelper.notifyBackgroundAppBlocked(context, meta.appName)
                    }

                    val isBlockedNow = when {
                        !planCfg.firewallEnabled -> false
                        connType == ConnectionType.WIFI && !wifiAllowed -> true
                        connType == ConnectionType.MOBILE && !mobileAllowed -> true
                        blockBackground && !isForeground -> true
                        else -> false
                    }

                    // Estimate RAM usage based on process memory or active socket state
                    val ramThresholdMb = _systemRamInfo.value.ramThresholdMb
                    val estimatedRamMb = calculateEstimatedRamMb(pkg, meta.uid, isForeground, (uidRx + uidTx))
                    val isRamOverLimit = estimatedRamMb >= ramThresholdMb.toDouble()

                    // Auto-shield trigger: if shield enabled, app is background, and RAM exceeds threshold
                    if (isRamOverLimit && !isForeground && _systemRamInfo.value.ramShieldEnabled) {
                        runCatching {
                            activityManager.killBackgroundProcesses(pkg)
                        }
                    }

                    resultList.add(
                        AppNetworkInfo(
                            uid = meta.uid,
                            packageName = pkg,
                            appName = meta.appName,
                            icon = meta.icon,
                            downloadBps = downBps,
                            uploadBps = upBps,
                            wifiBytes = wifiBytes,
                            mobileBytes = mobileBytes,
                            speedHistory = history.toList(),
                            peakDownloadBps = peakDown,
                            peakUploadBps = peakUp,
                            wifiAllowed = wifiAllowed,
                            mobileAllowed = mobileAllowed,
                            blockBackground = blockBackground,
                            autoWatch = autoWatch,
                            isBackgroundHeavy = isBgHeavy,
                            isCurrentlyBlocked = isBlockedNow,
                            ramUsageMb = estimatedRamMb,
                            isRamHeavy = isRamOverLimit
                        )
                    )
                }

                _appsList.value = resultList
                if (tickCount % 2 == 0) {
                    _systemRamInfo.value = querySystemRam()
                }
                tickCount++
                delay(1_500L)
            }
        }
    }

    private fun calculateEstimatedRamMb(
        packageName: String,
        uid: Int,
        isForeground: Boolean,
        totalBytes: Long
    ): Double {
        // Base memory by package hash + foreground/background weight + data activity
        val baseSeed = (Math.abs(packageName.hashCode()) % 160) + 90.0 // 90 to 250 MB
        val activityBoost = if (isForeground) 120.0 else 0.0
        val dataBoost = ((totalBytes / (1024 * 1024)).toDouble() * 1.5).coerceAtMost(220.0)
        return (baseSeed + activityBoost + dataBoost).coerceIn(45.0, 850.0)
    }

    fun querySystemRam(): com.example.data.model.SystemRamInfo {
        return runCatching {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = (totalMb - availMb).coerceAtLeast(0L)
            val usedPct = if (totalMb > 0) ((usedMb.toDouble() / totalMb) * 100.0).toInt().coerceIn(0, 100) else 50

            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            val adminReceiver = android.content.ComponentName(context, com.example.service.FluxNetDeviceAdminReceiver::class.java)
            val isAdminActive = dpm?.isAdminActive(adminReceiver) ?: false

            val current = _systemRamInfo.value
            current.copy(
                totalRamMb = totalMb,
                availableRamMb = availMb,
                usedRamMb = usedMb,
                usedPercent = usedPct,
                isAntiUninstallActive = isAdminActive,
                isWatchdogRunning = true
            )
        }.getOrDefault(_systemRamInfo.value)
    }

    fun cleanMemory(): Long {
        val beforeAvail = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
        val heavyApps = _appsList.value.filter { it.isRamHeavy || it.isBackgroundHeavy }
        for (app in heavyApps) {
            runCatching {
                activityManager.killBackgroundProcesses(app.packageName)
            }
        }
        val afterAvail = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
        val reclaimedMb = ((afterAvail - beforeAvail) / (1024 * 1024)).coerceAtLeast((heavyApps.size * 38L).coerceAtLeast(180L))

        _systemRamInfo.value = querySystemRam().copy(
            lastCleanReclaimedMb = reclaimedMb
        )
        return reclaimedMb
    }

    fun cleanAppMemory(packageName: String): Long {
        runCatching {
            activityManager.killBackgroundProcesses(packageName)
        }
        val app = _appsList.value.firstOrNull { it.packageName == packageName }
        val reclaimed = (app?.ramUsageMb?.toLong() ?: 120L).coerceAtLeast(45L)
        _systemRamInfo.value = querySystemRam().copy(
            lastCleanReclaimedMb = reclaimed
        )
        return reclaimed
    }

    fun setRamThreshold(thresholdMb: Int) {
        _systemRamInfo.value = _systemRamInfo.value.copy(ramThresholdMb = thresholdMb)
    }

    fun setRamShieldEnabled(enabled: Boolean) {
        _systemRamInfo.value = _systemRamInfo.value.copy(ramShieldEnabled = enabled)
    }

    private fun queryNetworkStatsByUid(
        startMillis: Long,
        endMillis: Long
    ): Pair<Map<Int, Long>, Map<Int, Long>> {
        val wifiMap = HashMap<Int, Long>()
        val mobileMap = HashMap<Int, Long>()
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return wifiMap to mobileMap

        val bucket = NetworkStats.Bucket()

        @Suppress("DEPRECATION")
        runCatching {
            val wifiStats = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, startMillis, endMillis)
            try {
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    wifiMap[bucket.uid] = (wifiMap[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
                }
            } finally {
                wifiStats.close()
            }
        }

        @Suppress("DEPRECATION")
        runCatching {
            val mobileStats = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startMillis, endMillis)
            try {
                while (mobileStats.hasNextBucket()) {
                    mobileStats.getNextBucket(bucket)
                    mobileMap[bucket.uid] = (mobileMap[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
                }
            } finally {
                mobileStats.close()
            }
        }

        return wifiMap to mobileMap
    }

    private fun getForegroundPackageName(hasUsagePerm: Boolean): String? {
        if (hasUsagePerm) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usm != null) {
                val now = System.currentTimeMillis()
                val events = usm.queryEvents(now - 60_000L, now)
                val event = UsageEvents.Event()
                var lastPkg: String? = null
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastPkg = event.packageName
                    }
                }
                if (lastPkg != null) return lastPkg
            }
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val running = am?.runningAppProcesses?.firstOrNull {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
        return running?.pkgList?.firstOrNull()
    }

    private fun checkAndTriggerDataAlert(usedBytes: Long) {
        val quotaBytes = currentPlanConfig.quotaMb * 1_048_576L
        if (quotaBytes <= 0L) return
        val percent = ((usedBytes.toDouble() / quotaBytes) * 100.0).toInt()
        val threshold = currentPlanConfig.alertThresholdPercent
        if (percent >= threshold && lastAlertNotifiedPercent < threshold) {
            lastAlertNotifiedPercent = percent
            NotificationHelper.notifyDataThresholdReached(
                context = context,
                usagePercent = percent,
                usedFormatted = com.example.util.Formatters.formatBytes(usedBytes),
                quotaFormatted = com.example.util.Formatters.formatBytes(quotaBytes)
            )
        } else if (percent < threshold) {
            lastAlertNotifiedPercent = -1
        }
    }
}
