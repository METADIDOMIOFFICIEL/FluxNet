package com.example.data.model

import android.graphics.drawable.Drawable

enum class ConnectionType(val labelFr: String) {
    WIFI("Wi-Fi"),
    MOBILE("Données Mobiles"),
    ETHERNET("Ethernet"),
    OFFLINE("Hors ligne")
}

data class SpeedSample(
    val timestampMillis: Long,
    val downloadBps: Long,
    val uploadBps: Long
)

data class NetworkStatus(
    val connectionType: ConnectionType = ConnectionType.OFFLINE,
    val networkName: String = "Déconnecté",
    val isMetered: Boolean = false,
    val isValidated: Boolean = false,
    val linkDownKbps: Int = 0,
    val linkUpKbps: Int = 0,
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L
)

data class AppNetworkInfo(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val speedHistory: List<SpeedSample> = emptyList(),
    val peakDownloadBps: Long = 0L,
    val peakUploadBps: Long = 0L,
    val wifiAllowed: Boolean = true,
    val mobileAllowed: Boolean = true,
    val blockBackground: Boolean = false,
    val autoWatch: Boolean = false,
    val isBackgroundHeavy: Boolean = false,
    val isCurrentlyBlocked: Boolean = false,
    val ramUsageMb: Double = 0.0,
    val isRamHeavy: Boolean = false
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
    val totalSpeedBps: Long get() = downloadBps + uploadBps
}

data class SystemRamInfo(
    val totalRamMb: Long = 4096L,
    val availableRamMb: Long = 1800L,
    val usedRamMb: Long = 2296L,
    val usedPercent: Int = 56,
    val ramThresholdMb: Int = 350,
    val ramShieldEnabled: Boolean = true,
    val isAntiUninstallActive: Boolean = false,
    val isWatchdogRunning: Boolean = true,
    val lastCleanReclaimedMb: Long = 0L
)

data class DataPlanConfig(
    val operatorName: String = "MTN",
    val quotaMb: Long = 20_480L,
    val validityDays: Int = 30,
    val cost: Double = 2000.0,
    val currency: String = "FCFA",
    val startDateMillis: Long = System.currentTimeMillis() - 3L * 86_400_000L,
    val alertThresholdPercent: Int = 80,
    val autoBlockBackgroundHeavy: Boolean = true,
    val backgroundThresholdMbPerMin: Int = 5,
    val firewallEnabled: Boolean = false
)

data class DataPlanSummary(
    val config: DataPlanConfig,
    val usedBytes: Long,
    val hasUsageStatsPermission: Boolean
) {
    val quotaBytes: Long get() = config.quotaMb * 1_048_576L
    val remainingBytes: Long get() = (quotaBytes - usedBytes).coerceAtLeast(0L)
    val usagePercent: Float
        get() = if (quotaBytes > 0L) ((usedBytes.toDouble() / quotaBytes) * 100.0).toFloat().coerceIn(0f, 100f) else 0f

    val endDateMillis: Long
        get() = config.startDateMillis + config.validityDays.toLong() * 86_400_000L

    val daysElapsed: Int
        get() {
            val diff = (System.currentTimeMillis() - config.startDateMillis).coerceAtLeast(0L)
            return ((diff / 86_400_000L).toInt() + 1).coerceAtLeast(1)
        }

    val daysRemaining: Int
        get() = (config.validityDays - daysElapsed + 1).coerceAtLeast(0)

    val estimatedCostUsed: Double
        get() = if (quotaBytes > 0L) (usedBytes.toDouble() / quotaBytes) * config.cost else 0.0

    val dailyAverageCost: Double
        get() = estimatedCostUsed / daysElapsed.coerceAtLeast(1)

    val dailyRecommendedBudget: Double
        get() = config.cost / config.validityDays.coerceAtLeast(1)

    val projectedTotalBytes: Long
        get() = ((usedBytes.toDouble() / daysElapsed.coerceAtLeast(1)) * config.validityDays).toLong()

    val projectedTotalCost: Double
        get() = if (quotaBytes > 0L) (projectedTotalBytes.toDouble() / quotaBytes) * config.cost else 0.0

    val isOverAlertThreshold: Boolean
        get() = usagePercent >= config.alertThresholdPercent

    val isOverBudgetPace: Boolean
        get() = projectedTotalBytes > quotaBytes
}
