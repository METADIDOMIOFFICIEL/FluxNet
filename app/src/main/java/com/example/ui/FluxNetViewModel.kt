package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppRuleEntity
import com.example.data.local.CommunityPostEntity
import com.example.data.local.FluxNetDatabase
import com.example.data.local.NetworkIncidentEntity
import com.example.data.local.NetworkQualityRecordEntity
import com.example.data.local.OperatorOfferEntity
import com.example.data.local.PlanPreferencesRepository
import com.example.data.local.PostCommentEntity
import com.example.data.model.AppNetworkInfo
import com.example.data.model.DataPlanConfig
import com.example.data.model.DataPlanSummary
import com.example.data.model.MobileOperator
import com.example.data.model.NetworkStatus
import com.example.data.model.SpeedSample
import com.example.data.network.NetworkMonitorRepository
import com.example.service.FluxNetVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppFilter(val labelFr: String) {
    ALL("Toutes"),
    ACTIVE("Actives"),
    RESTRICTED("Restreintes"),
    HEAVY("Gourmandes Réseau"),
    RAM_HEAVY("Gourmandes RAM")
}

enum class AppSort(val labelFr: String) {
    SPEED("Débit actuel"),
    USAGE("Données"),
    RAM("Mémoire RAM"),
    NAME("Nom")
}

class FluxNetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FluxNetDatabase.getInstance(application)
    private val appRuleDao = database.appRuleDao()
    private val operatorOfferDao = database.operatorOfferDao()
    private val networkIncidentDao = database.networkIncidentDao()
    private val networkQualityRecordDao = database.networkQualityRecordDao()
    private val communityPostDao = database.communityPostDao()
    private val postCommentDao = database.postCommentDao()
    private val planPrefs = PlanPreferencesRepository(application)
    private val networkRepo = NetworkMonitorRepository(application, appRuleDao)

    val networkStatus: StateFlow<NetworkStatus> = networkRepo.networkStatus
    val globalSpeedHistory: StateFlow<List<SpeedSample>> = networkRepo.globalSpeedHistory
    val hasUsageStatsPermission: StateFlow<Boolean> = networkRepo.hasUsageStatsPermission

    val operatorOffers: StateFlow<Map<MobileOperator, OperatorOfferEntity>> =
        operatorOfferDao.getAllOffers()
            .combine(kotlinx.coroutines.flow.flowOf(Unit)) { list, _ ->
                val byKey = list.associateBy { it.operatorKey }
                MobileOperator.entries.associateWith { op ->
                    byKey[op.name] ?: OperatorOfferEntity(operatorKey = op.name)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                MobileOperator.entries.associateWith {
                    OperatorOfferEntity(operatorKey = it.name)
                }
            )

    val userIncidents: StateFlow<List<NetworkIncidentEntity>> =
        networkIncidentDao.getAllIncidents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkQualityRecords: StateFlow<List<NetworkQualityRecordEntity>> =
        networkQualityRecordDao.getAllRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityPosts: StateFlow<List<CommunityPostEntity>> =
        communityPostDao.getAllPosts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planConfig: StateFlow<DataPlanConfig> = planPrefs.planConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataPlanConfig())

    val planSummary: StateFlow<DataPlanSummary> = combine(
        planPrefs.planConfigFlow,
        networkRepo.totalPlanUsedBytes,
        networkRepo.hasUsageStatsPermission
    ) { config, usedBytes, hasPerm ->
        DataPlanSummary(
            config = config,
            usedBytes = usedBytes,
            hasUsageStatsPermission = hasPerm
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DataPlanSummary(DataPlanConfig(), 0L, false)
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(AppSort.SPEED)
    val selectedSort: StateFlow<AppSort> = _selectedSort.asStateFlow()

    private val _selectedAppPackage = MutableStateFlow<String?>(null)
    val selectedAppPackage: StateFlow<String?> = _selectedAppPackage.asStateFlow()

    val allApps: StateFlow<List<AppNetworkInfo>> = networkRepo.appsList

    val filteredApps: StateFlow<List<AppNetworkInfo>> = combine(
        networkRepo.appsList,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { apps, query, filter, sort ->
        val q = query.trim().lowercase()
        val filtered = apps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.ACTIVE -> app.totalSpeedBps > 0L
                AppFilter.RESTRICTED -> !app.wifiAllowed || !app.mobileAllowed || app.blockBackground
                AppFilter.HEAVY -> app.isBackgroundHeavy || app.totalBytes > 50L * 1_048_576L
                AppFilter.RAM_HEAVY -> app.isRamHeavy || app.ramUsageMb >= 300.0
            }
            matchesQuery && matchesFilter
        }

        when (sort) {
            AppSort.SPEED -> filtered.sortedWith(
                compareByDescending<AppNetworkInfo> { it.totalSpeedBps }
                    .thenByDescending { it.totalBytes }
            )
            AppSort.USAGE -> filtered.sortedByDescending { it.totalBytes }
            AppSort.RAM -> filtered.sortedByDescending { it.ramUsageMb }
            AppSort.NAME -> filtered.sortedBy { it.appName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemRamInfo: StateFlow<com.example.data.model.SystemRamInfo> = networkRepo.systemRamInfo

    val selectedAppDetail: StateFlow<AppNetworkInfo?> = combine(
        networkRepo.appsList,
        _selectedAppPackage
    ) { apps, pkg ->
        if (pkg == null) null else apps.find { it.packageName == pkg }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seedDefaultDataIfEmpty()
        }

        viewModelScope.launch {
            appRuleDao.getAllRules().collect { rules ->
                networkRepo.updateRulesSnapshot(rules)
            }
        }
        viewModelScope.launch {
            planPrefs.planConfigFlow.collect { config ->
                networkRepo.updatePlanConfigSnapshot(config)
            }
        }
        // Keep VPN firewall synchronized with active network & per-app rules
        viewModelScope.launch {
            combine(
                planPrefs.planConfigFlow,
                networkRepo.appsList
            ) { config, apps ->
                val blocked = apps.filter { it.isCurrentlyBlocked }.map { it.packageName }
                config.firewallEnabled to blocked
            }.collect { (enabled, blocked) ->
                FluxNetVpnService.syncFirewall(getApplication(), enabled, blocked)
            }
        }

        // Start Anti-Kill & Anti-Slowness Watchdog persistent background service
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                com.example.service.FluxNetWatchdogService.startWatchdog(application)
            }
        }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val existingIncidents = networkIncidentDao.getAllIncidents().first()
        if (existingIncidents.isEmpty()) {
            networkIncidentDao.insertIncident(
                NetworkIncidentEntity(
                    operatorKey = "MTN",
                    planType = "Mobile 4G/5G",
                    problemLevel = "Critique (Coupure totale)",
                    durationMinutes = 120,
                    incidentDate = "01/11/2026",
                    incidentTime = "19:45",
                    dayOfWeek = "Dimanche",
                    timestampMillis = now - 3 * dayMs,
                    isResolved = true,
                    description = "Coupure réseau complète durant la soirée"
                )
            )
            networkIncidentDao.insertIncident(
                NetworkIncidentEntity(
                    operatorKey = "MTN",
                    planType = "Mobile 4G/5G",
                    problemLevel = "Modéré (Ralentissement)",
                    durationMinutes = 50,
                    incidentDate = "03/11/2026",
                    incidentTime = "20:15",
                    dayOfWeek = "Mardi",
                    timestampMillis = now - 1 * dayMs,
                    isResolved = true,
                    description = "Ralentissement important et latence élevée"
                )
            )
            networkIncidentDao.insertIncident(
                NetworkIncidentEntity(
                    operatorKey = "ORANGE",
                    planType = "Mobile 4G/5G",
                    problemLevel = "Mineur (Instabilité/Ping)",
                    durationMinutes = 30,
                    incidentDate = "02/11/2026",
                    incidentTime = "14:20",
                    dayOfWeek = "Lundi",
                    timestampMillis = now - 2 * dayMs,
                    isResolved = true,
                    description = "Instabilité ping durant les visioconférences"
                )
            )
            networkIncidentDao.insertIncident(
                NetworkIncidentEntity(
                    operatorKey = "CAMTEL",
                    planType = "Fibre optique",
                    problemLevel = "Modéré (Ralentissement)",
                    durationMinutes = 40,
                    incidentDate = "31/10/2026",
                    incidentTime = "10:30",
                    dayOfWeek = "Samedi",
                    timestampMillis = now - 4 * dayMs,
                    isResolved = true,
                    description = "Baisse passagère de bande passante le week-end"
                )
            )
        }

        val existingRecords = networkQualityRecordDao.getAllRecords().first()
        if (existingRecords.isEmpty()) {
            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = "CAMTEL",
                    connectionType = "Fibre",
                    downloadSpeedMbps = 24.8,
                    uploadSpeedMbps = 11.2,
                    latencyMs = 32L,
                    jitterMs = 6L,
                    timestampMillis = now - 100_000L,
                    networkName = "Blue Fibre"
                )
            )
            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = "ORANGE",
                    connectionType = "Mobile 4G",
                    downloadSpeedMbps = 19.5,
                    uploadSpeedMbps = 7.8,
                    latencyMs = 46L,
                    jitterMs = 12L,
                    timestampMillis = now - 200_000L,
                    networkName = "Orange 4G"
                )
            )
            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = "MTN",
                    connectionType = "Mobile 4G",
                    downloadSpeedMbps = 16.2,
                    uploadSpeedMbps = 5.4,
                    latencyMs = 64L,
                    jitterMs = 18L,
                    timestampMillis = now - 300_000L,
                    networkName = "MTN 4G"
                )
            )
            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = "NEXTTEL",
                    connectionType = "Mobile 3G/4G",
                    downloadSpeedMbps = 11.0,
                    uploadSpeedMbps = 3.2,
                    latencyMs = 82L,
                    jitterMs = 24L,
                    timestampMillis = now - 400_000L,
                    networkName = "Nexttel"
                )
            )
            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = "YOOMEE",
                    connectionType = "Mobile 4G",
                    downloadSpeedMbps = 13.8,
                    uploadSpeedMbps = 4.0,
                    latencyMs = 70L,
                    jitterMs = 20L,
                    timestampMillis = now - 500_000L,
                    networkName = "Yoomee 4G"
                )
            )
        }

        // Seed community posts if empty
        val existingPosts = communityPostDao.getAllPosts().first()
        if (existingPosts.isEmpty()) {
            val p1Id = communityPostDao.insertPost(
                CommunityPostEntity(
                    authorName = "Patrick T.",
                    authorAvatarColor = 0xFF0284C7,
                    postType = "PLAN_OFFER",
                    operatorKey = "CAMTEL",
                    planType = "Fibre optique / Box",
                    location = "Douala - Akwa",
                    title = "Forfait Blue Mo 10 000 FCFA (Camtel)",
                    content = "30 Go pour 10 000 FCFA valable 30 jours. Très bon débit 4G/Fibre sur Douala, parfait pour le télétravail et les visioconférences. Vous pouvez le copier dans votre comparateur !",
                    timestampMillis = now - 3_600_000L * 4,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "14:30",
                    priceFcfa = 10000.0,
                    dataMb = 30720.0,
                    callMinutes = 0,
                    smsCount = 0,
                    validityDays = 30,
                    likeCount = 22,
                    isLikedByMe = false,
                    commentCount = 3
                )
            )

            val p2Id = communityPostDao.insertPost(
                CommunityPostEntity(
                    authorName = "Sandrine N.",
                    authorAvatarColor = 0xFFEAB308,
                    postType = "INCIDENT",
                    operatorKey = "MTN",
                    planType = "Mobile 4G/5G",
                    location = "Yaoundé - Bastos",
                    title = "Coupure totale 4G vers Bastos / Dragages",
                    content = "Plus aucun signal internet depuis 18h30. Les données mobiles tournent dans le vide. Est-ce que d'autres personnes ont le même souci dans le secteur ?",
                    timestampMillis = now - 3_600_000L * 2,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "18:45",
                    problemLevel = "Critique (Coupure totale)",
                    durationMinutes = 90,
                    isResolved = false,
                    likeCount = 14,
                    isLikedByMe = false,
                    commentCount = 4
                )
            )

            val p3Id = communityPostDao.insertPost(
                CommunityPostEntity(
                    authorName = "Brice K.",
                    authorAvatarColor = 0xFFF97316,
                    postType = "PLAN_OFFER",
                    operatorKey = "ORANGE",
                    planType = "Mobile 4G/5G",
                    location = "Bafoussam - Centre",
                    title = "Pass Orange Maxi 2 000 FCFA",
                    content = "Forfait 4.5 Go + 30 min d'appels à 2000 FCFA valable 7 jours. Excellent pour le streaming en fin de journée et les réseaux sociaux.",
                    timestampMillis = now - 3_600_000L * 8,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "09:15",
                    priceFcfa = 2000.0,
                    dataMb = 4608.0,
                    callMinutes = 30,
                    smsCount = 50,
                    validityDays = 7,
                    likeCount = 31,
                    isLikedByMe = true,
                    commentCount = 2
                )
            )

            communityPostDao.insertPost(
                CommunityPostEntity(
                    authorName = "Hervé M.",
                    authorAvatarColor = 0xFF0284C7,
                    postType = "INCIDENT",
                    operatorKey = "CAMTEL",
                    planType = "Fibre optique",
                    location = "Douala - Bonanjo",
                    title = "Ralentissement fibre optique zone portuaire",
                    content = "Latence très élevée (> 190ms) constatée durant les heures de bureau. Rétablissement en cours.",
                    timestampMillis = now - 3_600_000L * 14,
                    dateFormatted = "03/11/2026",
                    timeFormatted = "11:20",
                    problemLevel = "Modéré (Ralentissement)",
                    durationMinutes = 60,
                    isResolved = true,
                    likeCount = 7,
                    isLikedByMe = false,
                    commentCount = 1
                )
            )

            // Seed comments for post 1
            val c1Id = postCommentDao.insertComment(
                PostCommentEntity(
                    postId = p1Id,
                    authorName = "Mireille T.",
                    authorAvatarColor = 0xFF9333EA,
                    commentText = "Je confirme, le débit est très bon à Akwa Nord. Merci pour le partage !",
                    timestampMillis = now - 3_600_000L * 3,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "15:10",
                    likeCount = 3
                )
            )
            postCommentDao.insertComment(
                PostCommentEntity(
                    postId = p1Id,
                    parentCommentId = c1Id,
                    replyToAuthor = "Mireille T.",
                    authorName = "Patrick T.",
                    authorAvatarColor = 0xFF0284C7,
                    commentText = "Avec plaisir Mireille ! Je l'ai copié dans mon comparateur FluxNet pour vérifier l'autonomie.",
                    timestampMillis = now - 3_600_000L * 2,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "15:45",
                    likeCount = 2
                )
            )

            // Seed comments for post 2
            val c2Id = postCommentDao.insertComment(
                PostCommentEntity(
                    postId = p2Id,
                    authorName = "Christian B.",
                    authorAvatarColor = 0xFFE11D48,
                    commentText = "Même problème à Omnisport ! Coupure totale 4G chez moi aussi.",
                    timestampMillis = now - 3_600_000L * 1,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "19:10",
                    likeCount = 5
                )
            )
            postCommentDao.insertComment(
                PostCommentEntity(
                    postId = p2Id,
                    parentCommentId = c2Id,
                    replyToAuthor = "Christian B.",
                    authorName = "Sandrine N.",
                    authorAvatarColor = 0xFFEAB308,
                    commentText = "Merci Christian pour le retour, j'ai enregistré le signalement dans l'app.",
                    timestampMillis = now - 1_800_000L,
                    dateFormatted = "04/11/2026",
                    timeFormatted = "19:35",
                    likeCount = 1
                )
            )
        }
    }

    fun getCommentsForPost(postId: Long): Flow<List<PostCommentEntity>> {
        return postCommentDao.getCommentsForPost(postId)
    }

    fun publishCommunityPost(
        authorName: String,
        postType: String, // "INCIDENT" or "PLAN_OFFER"
        operatorKey: String,
        planType: String,
        location: String,
        title: String,
        content: String,
        problemLevel: String = "",
        durationMinutes: Int = 0,
        isResolved: Boolean = true,
        priceFcfa: Double = 0.0,
        dataMb: Double = 0.0,
        callMinutes: Int = 0,
        smsCount: Int = 0,
        validityDays: Int = 7
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)
            val nowDate = Date(now)

            val post = CommunityPostEntity(
                authorName = authorName.ifBlank { "Utilisateur FluxNet" },
                authorAvatarColor = 0xFF0284C7,
                postType = postType,
                operatorKey = operatorKey,
                planType = planType,
                location = location.ifBlank { "National" },
                title = title.trim(),
                content = content.trim(),
                timestampMillis = now,
                dateFormatted = dateFormat.format(nowDate),
                timeFormatted = timeFormat.format(nowDate),
                problemLevel = problemLevel,
                durationMinutes = durationMinutes,
                isResolved = isResolved,
                priceFcfa = priceFcfa,
                dataMb = dataMb,
                callMinutes = callMinutes,
                smsCount = smsCount,
                validityDays = validityDays,
                likeCount = 0,
                isLikedByMe = false,
                commentCount = 0
            )

            communityPostDao.insertPost(post)

            // If it's an incident, also register it in user's personal incident history!
            if (postType == "INCIDENT") {
                val dayFormat = SimpleDateFormat("EEEE", Locale.FRANCE)
                networkIncidentDao.insertIncident(
                    NetworkIncidentEntity(
                        operatorKey = operatorKey,
                        planType = planType,
                        problemLevel = problemLevel.ifBlank { "Modéré (Ralentissement)" },
                        durationMinutes = durationMinutes.coerceAtLeast(15),
                        incidentDate = dateFormat.format(nowDate),
                        incidentTime = timeFormat.format(nowDate),
                        dayOfWeek = dayFormat.format(nowDate).replaceFirstChar { it.uppercase() },
                        timestampMillis = now,
                        isResolved = isResolved,
                        description = title + " - " + content
                    )
                )
            }
        }
    }

    fun addComment(
        postId: Long,
        parentCommentId: Long?,
        replyToAuthor: String?,
        commentText: String,
        authorName: String = "Vous"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)
            val nowDate = Date(now)

            postCommentDao.insertComment(
                PostCommentEntity(
                    postId = postId,
                    parentCommentId = parentCommentId,
                    replyToAuthor = replyToAuthor,
                    authorName = authorName.ifBlank { "Vous" },
                    authorAvatarColor = 0xFF10B981,
                    commentText = commentText.trim(),
                    timestampMillis = now,
                    dateFormatted = dateFormat.format(nowDate),
                    timeFormatted = timeFormat.format(nowDate),
                    likeCount = 0,
                    isLikedByMe = false
                )
            )
            communityPostDao.incrementCommentCount(postId)
        }
    }

    fun toggleLikePost(postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val post = communityPostDao.getPostById(postId) ?: return@launch
            val newIsLiked = !post.isLikedByMe
            val newCount = if (newIsLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
            communityPostDao.updateLike(postId, newIsLiked, newCount)
        }
    }

    fun toggleLikeComment(commentId: Long, currentCount: Int, currentlyLiked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val newIsLiked = !currentlyLiked
            val newCount = if (newIsLiked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
            postCommentDao.updateCommentLike(commentId, newIsLiked, newCount)
        }
    }

    fun copyPlanToComparator(post: CommunityPostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            operatorOfferDao.upsertOffer(
                OperatorOfferEntity(
                    operatorKey = post.operatorKey,
                    planType = post.planType,
                    priceFcfa = post.priceFcfa.coerceAtLeast(1.0),
                    dataMb = post.dataMb,
                    callMinutes = post.callMinutes,
                    smsCount = post.smsCount,
                    validityDays = post.validityDays.coerceAtLeast(1)
                )
            )
        }
    }

    fun addIncident(incident: NetworkIncidentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            networkIncidentDao.insertIncident(incident)
        }
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            networkIncidentDao.deleteIncidentById(id)
        }
    }

    fun recordCurrentNetworkQuality(operatorKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = networkStatus.value
            val downMbps = (status.linkDownKbps / 1000.0).coerceAtLeast(if (status.downloadBps > 0) status.downloadBps * 8.0 / 1_000_000.0 else 18.0)
            val upMbps = (status.linkUpKbps / 1000.0).coerceAtLeast(if (status.uploadBps > 0) status.uploadBps * 8.0 / 1_000_000.0 else 6.0)
            val latency = if (status.isValidated) 42L else 85L

            networkQualityRecordDao.insertRecord(
                NetworkQualityRecordEntity(
                    operatorKey = operatorKey,
                    connectionType = status.connectionType.labelFr,
                    downloadSpeedMbps = String.format(Locale.US, "%.1f", downMbps).toDoubleOrNull() ?: 18.0,
                    uploadSpeedMbps = String.format(Locale.US, "%.1f", upMbps).toDoubleOrNull() ?: 6.0,
                    latencyMs = latency,
                    jitterMs = 10L,
                    timestampMillis = System.currentTimeMillis(),
                    networkName = status.networkName
                )
            )
        }
    }

    fun refreshPermissions() {
        networkRepo.refreshPermissions()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: AppSort) {
        _selectedSort.value = sort
    }

    fun selectApp(packageName: String?) {
        _selectedAppPackage.value = packageName
    }

    fun updateAppRule(
        packageName: String,
        wifiAllowed: Boolean,
        mobileAllowed: Boolean,
        blockBackground: Boolean,
        autoWatch: Boolean
    ) {
        viewModelScope.launch {
            appRuleDao.upsertRule(
                AppRuleEntity(
                    packageName = packageName,
                    wifiAllowed = wifiAllowed,
                    mobileAllowed = mobileAllowed,
                    blockBackground = blockBackground,
                    autoWatch = autoWatch
                )
            )
        }
    }

    fun setFirewallEnabled(enabled: Boolean) {
        viewModelScope.launch {
            planPrefs.setFirewallEnabled(enabled)
        }
    }

    fun updatePlan(
        operatorName: String,
        quotaMb: Long,
        validityDays: Int,
        cost: Double,
        currency: String,
        startDateMillis: Long
    ) {
        viewModelScope.launch {
            planPrefs.updatePlan(operatorName, quotaMb, validityDays, cost, currency, startDateMillis)
        }
    }

    fun saveOperatorOffer(offer: OperatorOfferEntity) {
        viewModelScope.launch {
            operatorOfferDao.upsertOffer(offer)
        }
    }

    fun updateAlertThreshold(percent: Int) {
        viewModelScope.launch {
            planPrefs.updateAlertThreshold(percent)
        }
    }

    fun updateAutoBlockBackground(enabled: Boolean, thresholdMbPerMin: Int? = null) {
        viewModelScope.launch {
            planPrefs.updateAutoBlockBackground(enabled, thresholdMbPerMin)
        }
    }

    fun cleanMemory(): Long {
        return networkRepo.cleanMemory()
    }

    fun cleanAppMemory(packageName: String): Long {
        return networkRepo.cleanAppMemory(packageName)
    }

    fun setRamThreshold(thresholdMb: Int) {
        networkRepo.setRamThreshold(thresholdMb)
    }

    fun toggleRamShield(enabled: Boolean) {
        networkRepo.setRamShieldEnabled(enabled)
    }
}
