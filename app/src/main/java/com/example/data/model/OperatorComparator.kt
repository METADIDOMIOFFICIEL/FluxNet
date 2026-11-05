package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.local.NetworkIncidentEntity
import com.example.data.local.NetworkQualityRecordEntity
import com.example.data.local.OperatorOfferEntity
import java.util.Locale
import kotlin.math.roundToInt

enum class OperatorNeedType(val labelFr: String) {
    INTERNET("Internet"),
    CALLS("Appels"),
    SMS("SMS"),
    MIXED("Mixte")
}

enum class MobileOperator(
    val displayName: String,
    val shortName: String,
    val brandColor: Color
) {
    MTN(
        displayName = "MTN",
        shortName = "MTN",
        brandColor = Color(0xFFEAB308)
    ),
    ORANGE(
        displayName = "Orange",
        shortName = "Orange",
        brandColor = Color(0xFFF97316)
    ),
    CAMTEL(
        displayName = "Camtel (Blue)",
        shortName = "Camtel",
        brandColor = Color(0xFF0284C7)
    ),
    NEXTTEL(
        displayName = "Nexttel",
        shortName = "Nexttel",
        brandColor = Color(0xFFE11D48)
    ),
    YOOMEE(
        displayName = "Yoomee",
        shortName = "Yoomee",
        brandColor = Color(0xFF9333EA)
    )
}

data class OperatorQualityStats(
    val sampleCount: Int,
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgLatencyMs: Long,
    val qualityScore: Double // 0 to 100
)

data class OperatorIncidentStats(
    val totalCount: Int,
    val criticalCount: Int,
    val majorCount: Int,
    val moderateCount: Int,
    val minorCount: Int,
    val unresolvedCount: Int,
    val avgDurationMinutes: Int,
    val peakHourIncidentsCount: Int,
    val recurringDays: List<String>,
    val reliabilityScore: Double, // 0 to 100
    val temporalDescription: String
)

data class OperatorComparisonResult(
    val operator: MobileOperator,
    val offer: OperatorOfferEntity,
    val hasEnteredVolume: Boolean,
    val valuePer100Fcfa: Double,
    val estimatedUserDurationDays: Double,
    val qualityStats: OperatorQualityStats,
    val incidentStats: OperatorIncidentStats,
    val globalScore: Double, // Comprehensive 0 to 100 score taking all into account
    val isBestChoice: Boolean,
    val badges: List<String>,
    val summaryText: String
)

object OperatorComparator {

    fun computeQualityStats(
        operator: MobileOperator,
        qualityRecords: List<NetworkQualityRecordEntity>
    ): OperatorQualityStats {
        val opRecords = qualityRecords.filter { it.operatorKey.equals(operator.name, ignoreCase = true) }
        if (opRecords.isEmpty()) {
            // Default baseline estimates when no measurement exists yet for this operator
            val baseline = when (operator) {
                MobileOperator.CAMTEL -> Triple(22.0, 40L, 82.0)
                MobileOperator.MTN -> Triple(18.0, 52L, 78.0)
                MobileOperator.ORANGE -> Triple(19.0, 48L, 80.0)
                MobileOperator.NEXTTEL -> Triple(12.0, 75L, 65.0)
                MobileOperator.YOOMEE -> Triple(14.0, 68L, 68.0)
            }
            return OperatorQualityStats(
                sampleCount = 0,
                avgDownloadMbps = baseline.first,
                avgUploadMbps = baseline.first * 0.4,
                avgLatencyMs = baseline.second,
                qualityScore = baseline.third
            )
        }

        val avgDown = opRecords.map { it.downloadSpeedMbps }.average().coerceAtLeast(0.1)
        val avgUp = opRecords.map { it.uploadSpeedMbps }.average().coerceAtLeast(0.1)
        val avgLat = opRecords.map { it.latencyMs.toDouble() }.average().toLong().coerceAtLeast(5L)

        // Quality score:
        // Speed score (0-60 pts): 25 Mbps is considered 60 pts
        val speedScore = (avgDown / 25.0 * 60.0).coerceIn(5.0, 60.0)
        // Latency score (0-40 pts): <= 30ms is 40 pts, >= 200ms is 5 pts
        val latScore = when {
            avgLat <= 30L -> 40.0
            avgLat <= 60L -> 32.0
            avgLat <= 100L -> 24.0
            avgLat <= 150L -> 15.0
            else -> 6.0
        }
        val computedScore = (speedScore + latScore).coerceIn(10.0, 100.0)

        return OperatorQualityStats(
            sampleCount = opRecords.size,
            avgDownloadMbps = avgDown,
            avgUploadMbps = avgUp,
            avgLatencyMs = avgLat,
            qualityScore = computedScore
        )
    }

    fun computeIncidentStats(
        operator: MobileOperator,
        incidents: List<NetworkIncidentEntity>,
        filterPlanType: String? = null
    ): OperatorIncidentStats {
        val opIncidents = incidents.filter {
            it.operatorKey.equals(operator.name, ignoreCase = true) &&
                (filterPlanType.isNullOrBlank() || it.planType.equals(filterPlanType, ignoreCase = true))
        }

        if (opIncidents.isEmpty()) {
            return OperatorIncidentStats(
                totalCount = 0,
                criticalCount = 0,
                majorCount = 0,
                moderateCount = 0,
                minorCount = 0,
                unresolvedCount = 0,
                avgDurationMinutes = 0,
                peakHourIncidentsCount = 0,
                recurringDays = emptyList(),
                reliabilityScore = 100.0,
                temporalDescription = "Aucun incident signalé (Fiabilité 100%)"
            )
        }

        var critical = 0
        var major = 0
        var moderate = 0
        var minor = 0
        var unresolved = 0
        var totalDuration = 0L

        var peakHourCount = 0
        val dayCounts = mutableMapOf<String, Int>()

        val now = System.currentTimeMillis()
        var totalSeverityPenalty = 0.0

        for (inc in opIncidents) {
            val isCritical = inc.problemLevel.contains("Critique", ignoreCase = true)
            val isMajor = inc.problemLevel.contains("Majeur", ignoreCase = true)
            val isModerate = inc.problemLevel.contains("Modéré", ignoreCase = true)
            val isMinor = inc.problemLevel.contains("Mineur", ignoreCase = true)

            // Age recency multiplier: incidents within last 14 days carry 1.4x weight
            val ageDays = ((now - inc.timestampMillis).coerceAtLeast(0L) / 86_400_000L).toInt()
            val recencyMultiplier = if (ageDays <= 14) 1.4 else 1.0

            val baseSeverity = when {
                isCritical -> {
                    critical++
                    20.0
                }
                isMajor -> {
                    major++
                    12.0
                }
                isModerate -> {
                    moderate++
                    6.0
                }
                isMinor -> {
                    minor++
                    2.5
                }
                else -> {
                    moderate++
                    6.0
                }
            }
            totalSeverityPenalty += (baseSeverity * recencyMultiplier)

            if (!inc.isResolved) {
                unresolved++
                totalSeverityPenalty += 12.0 // Active ongoing issue penalty
            }

            totalDuration += inc.durationMinutes

            // Parse time to check peak hour (18h-23h or 12h-14h)
            val hour = extractHour(inc.incidentTime)
            if (hour in 18..23 || hour in 12..14) {
                peakHourCount++
            }

            if (inc.dayOfWeek.isNotBlank()) {
                val cleanDay = inc.dayOfWeek.trim()
                dayCounts[cleanDay] = (dayCounts[cleanDay] ?: 0) + 1
            }
        }

        val avgDuration = if (opIncidents.isNotEmpty()) (totalDuration / opIncidents.size).toInt() else 0

        // MTTR (Mean time to resolve) penalty:
        val mttrPenalty = when {
            avgDuration <= 20 -> 0.0
            avgDuration <= 60 -> 4.0
            avgDuration <= 180 -> 10.0
            avgDuration <= 360 -> 18.0
            else -> 28.0
        }

        // Temporal penalty: recurrent peak hour issues
        val peakPenalty = if (peakHourCount >= 2) 8.0 else if (peakHourCount == 1) 3.0 else 0.0

        // Recurrent day penalty (e.g. 2+ incidents on Saturdays or Sundays)
        val recurringDaysList = dayCounts.filter { it.value >= 2 }.map { "${it.key} (${it.value}x)" }
        val dayPenalty = if (recurringDaysList.isNotEmpty()) 7.0 else 0.0

        val totalPenalty = totalSeverityPenalty + mttrPenalty + peakPenalty + dayPenalty
        val reliability = (100.0 - totalPenalty).coerceIn(10.0, 100.0)

        val temporalDesc = buildString {
            if (peakHourCount > 0) {
                append("$peakHourCount panne(s) aux heures de pointe (18h-23h/12h-14h). ")
            }
            if (recurringDaysList.isNotEmpty()) {
                append("Récurrence constatée : ${recurringDaysList.joinToString(", ")}. ")
            }
            if (avgDuration > 0) {
                append("Délai de rétablissement moyen : ${formatDurationMinutes(avgDuration)}.")
            }
        }.trim().ifEmpty { "Incidents ponctuels dispersés" }

        return OperatorIncidentStats(
            totalCount = opIncidents.size,
            criticalCount = critical,
            majorCount = major,
            moderateCount = moderate,
            minorCount = minor,
            unresolvedCount = unresolved,
            avgDurationMinutes = avgDuration,
            peakHourIncidentsCount = peakHourCount,
            recurringDays = recurringDaysList,
            reliabilityScore = reliability,
            temporalDescription = temporalDesc
        )
    }

    private fun extractHour(timeStr: String): Int {
        val parts = timeStr.split(":", "h", "H")
        return parts.firstOrNull()?.trim()?.toIntOrNull() ?: 12
    }

    private fun formatDurationMinutes(minutes: Int): String {
        return if (minutes < 60) {
            "${minutes}m"
        } else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }
    }

    fun evaluateUserOffers(
        offersByOperator: Map<MobileOperator, OperatorOfferEntity>,
        needType: OperatorNeedType,
        userDailyDataMb: Double,
        userDailyCallMin: Int,
        userDailySms: Int,
        qualityRecords: List<NetworkQualityRecordEntity> = emptyList(),
        incidents: List<NetworkIncidentEntity> = emptyList(),
        filterPlanType: String? = null
    ): List<OperatorComparisonResult> {
        val safeDailyData = userDailyDataMb.coerceAtLeast(10.0)
        val safeDailyCalls = userDailyCallMin.coerceAtLeast(1)
        val safeDailySms = userDailySms.coerceAtLeast(1)

        val rawEvaluated = MobileOperator.entries.map { op ->
            val offer = offersByOperator[op] ?: OperatorOfferEntity(operatorKey = op.name)
            val price = offer.priceFcfa.coerceAtLeast(1.0)

            val hasVolume = when (needType) {
                OperatorNeedType.INTERNET -> offer.dataMb > 0.0
                OperatorNeedType.CALLS -> offer.callMinutes > 0
                OperatorNeedType.SMS -> offer.smsCount > 0
                OperatorNeedType.MIXED -> offer.dataMb > 0.0 || offer.callMinutes > 0 || offer.smsCount > 0
            }

            val valuePer100 = if (!hasVolume || offer.priceFcfa <= 0.0) {
                0.0
            } else {
                when (needType) {
                    OperatorNeedType.INTERNET -> (offer.dataMb / price) * 100.0
                    OperatorNeedType.CALLS -> (offer.callMinutes.toDouble() / price) * 100.0
                    OperatorNeedType.SMS -> (offer.smsCount.toDouble() / price) * 100.0
                    OperatorNeedType.MIXED -> {
                        val normData = offer.dataMb / 100.0
                        val normCalls = offer.callMinutes.toDouble()
                        val normSms = offer.smsCount / 5.0
                        ((normData + normCalls + normSms) / price) * 100.0
                    }
                }
            }

            val durationDays = if (!hasVolume) {
                0.0
            } else {
                when (needType) {
                    OperatorNeedType.INTERNET -> offer.dataMb / safeDailyData
                    OperatorNeedType.CALLS -> offer.callMinutes.toDouble() / safeDailyCalls
                    OperatorNeedType.SMS -> offer.smsCount.toDouble() / safeDailySms
                    OperatorNeedType.MIXED -> {
                        val candidates = buildList {
                            if (offer.dataMb > 0.0) add(offer.dataMb / safeDailyData)
                            if (offer.callMinutes > 0) add(offer.callMinutes.toDouble() / safeDailyCalls)
                            if (offer.smsCount > 0) add(offer.smsCount.toDouble() / safeDailySms)
                        }
                        candidates.minOrNull() ?: 0.0
                    }
                }
            }

            val qualityStats = computeQualityStats(op, qualityRecords)
            val incidentStats = computeIncidentStats(op, incidents, filterPlanType)

            Triple(op, offer to Triple(hasVolume, valuePer100, durationDays), qualityStats to incidentStats)
        }

        val maxValue = rawEvaluated.maxOfOrNull { it.second.second.second }?.coerceAtLeast(1.0) ?: 1.0

        val evaluatedWithScore = rawEvaluated.map { (op, offerData, statsData) ->
            val offer = offerData.first
            val (hasVolume, valuePer100, durationDays) = offerData.second
            val (qualityStats, incidentStats) = statsData

            // Compute composite global score (0-100)
            // Weighting:
            // 40% Volume/Price Ratio (normalized against max)
            // 30% Real Measured Network Quality (Download, Latency, Jitter)
            // 30% User Incident & Reliability Record (Number of reports, severity, resolution delay, peak hours)
            val normalizedValue = if (hasVolume) (valuePer100 / maxValue * 100.0).coerceIn(0.0, 100.0) else 0.0
            val globalScore = if (!hasVolume) {
                (qualityStats.qualityScore * 0.5) + (incidentStats.reliabilityScore * 0.5)
            } else {
                (normalizedValue * 0.40) + (qualityStats.qualityScore * 0.30) + (incidentStats.reliabilityScore * 0.30)
            }.coerceIn(0.0, 100.0)

            val badges = buildList {
                if (incidentStats.totalCount == 0) {
                    add("Zéro Panne")
                } else if (incidentStats.reliabilityScore < 60.0) {
                    add("Pannes Fréquentes")
                }
                if (incidentStats.unresolvedCount > 0) {
                    add("Incident en cours")
                }
                if (qualityStats.avgDownloadMbps >= 20.0) {
                    add("Débit Élevé")
                }
                if (qualityStats.avgLatencyMs <= 45) {
                    add("Faible Latence")
                }
                if (incidentStats.avgDurationMinutes in 1..45 && incidentStats.totalCount > 0) {
                    add("Résolution Rapide")
                } else if (incidentStats.avgDurationMinutes >= 180) {
                    add("Rétablissement Long")
                }
            }

            OperatorComparisonResult(
                operator = op,
                offer = offer,
                hasEnteredVolume = hasVolume,
                valuePer100Fcfa = valuePer100,
                estimatedUserDurationDays = durationDays,
                qualityStats = qualityStats,
                incidentStats = incidentStats,
                globalScore = globalScore,
                isBestChoice = false,
                badges = badges,
                summaryText = ""
            )
        }

        // Sort primarily by:
        // 1. Has entered volume
        // 2. Global composite score (which combines value, quality history, and problem reports!)
        val sorted = evaluatedWithScore.sortedWith(
            compareByDescending<OperatorComparisonResult> { it.hasEnteredVolume }
                .thenByDescending { it.globalScore }
                .thenByDescending { it.valuePer100Fcfa }
        )

        return sorted.mapIndexed { index, item ->
            val isBest = index == 0 && item.hasEnteredVolume && item.globalScore > 0.0
            val summary = if (!item.hasEnteredVolume) {
                "Saisissez le volume connu pour ${item.operator.shortName} afin de comparer avec votre historique"
            } else if (isBest) {
                buildString {
                    append("Meilleur choix global : score ${item.globalScore.roundToInt()}/100. ")
                    if (item.incidentStats.totalCount == 0) {
                        append("Excellente stabilité (aucun incident signalé).")
                    } else {
                        append("Fiabilité ${item.incidentStats.reliabilityScore.roundToInt()}% (${item.incidentStats.totalCount} signalement(s)).")
                    }
                }
            } else {
                buildString {
                    append("Score global : ${item.globalScore.roundToInt()}/100 | ")
                    if (item.incidentStats.totalCount > 0) {
                        append("${item.incidentStats.totalCount} incident(s) signalé(s) (Fiabilité ${item.incidentStats.reliabilityScore.roundToInt()}%)")
                    } else {
                        append("Fiabilité 100% (0 signalement)")
                    }
                }
            }
            item.copy(isBestChoice = isBest, summaryText = summary)
        }
    }
}
