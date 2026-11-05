package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CommunityPostEntity
import com.example.data.local.PostCommentEntity
import com.example.data.model.MobileOperator
import com.example.ui.components.CommentsSheet
import com.example.ui.components.PublishPostDialog
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobileAmber
import com.example.ui.theme.UploadEmerald
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CommunityScreen(
    posts: List<CommunityPostEntity>,
    snackbarHostState: SnackbarHostState,
    onGetComments: (Long) -> Flow<List<PostCommentEntity>>,
    onPublishPost: (
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
    ) -> Unit,
    onAddComment: (postId: Long, parentCommentId: Long?, replyToAuthor: String?, text: String, authorName: String) -> Unit,
    onToggleLikePost: (Long) -> Unit,
    onToggleLikeComment: (commentId: Long, currentCount: Int, isLiked: Boolean) -> Unit,
    onCopyPlanToComparator: (CommunityPostEntity) -> Unit,
    onNavigateToComparator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0 = Tous, 1 = Pannes, 2 = Forfaits
    var selectedOperatorFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showPublishDialog by remember { mutableStateOf(false) }
    var activeCommentsPost by remember { mutableStateOf<CommunityPostEntity?>(null) }

    val filteredPosts = remember(posts, selectedFilterIndex, selectedOperatorFilter, searchQuery) {
        posts.filter { post ->
            val matchType = when (selectedFilterIndex) {
                1 -> post.postType == "INCIDENT"
                2 -> post.postType == "PLAN_OFFER"
                else -> true
            }
            val matchOperator = selectedOperatorFilter == null || post.operatorKey.equals(selectedOperatorFilter, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchSearch = q.isEmpty() ||
                post.title.lowercase().contains(q) ||
                post.content.lowercase().contains(q) ||
                post.location.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.operatorKey.lowercase().contains(q)

            matchType && matchOperator && matchSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("community_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Banner & Action
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
                                        text = "Communauté FluxNet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${posts.size} signalements & forfaits partagés",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showPublishDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DownloadCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("open_publish_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Publier",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher ville, quartier, opérateur, mot-clé...") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        // Subtabs: Tous, Pannes, Forfaits
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedFilterIndex == 0,
                                onClick = { selectedFilterIndex = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Text("Tous (${posts.size})", style = MaterialTheme.typography.labelSmall)
                            }
                            SegmentedButton(
                                selected = selectedFilterIndex == 1,
                                onClick = { selectedFilterIndex = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Text("Pannes (${posts.count { it.postType == "INCIDENT" }})", style = MaterialTheme.typography.labelSmall)
                            }
                            SegmentedButton(
                                selected = selectedFilterIndex == 2,
                                onClick = { selectedFilterIndex = 2 },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Text("Forfaits (${posts.count { it.postType == "PLAN_OFFER" }})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 2. Operator Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedOperatorFilter == null,
                        onClick = { selectedOperatorFilter = null },
                        label = { Text("Tous les opérateurs") }
                    )
                    MobileOperator.entries.forEach { op ->
                        val isSelected = selectedOperatorFilter == op.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedOperatorFilter = if (isSelected) null else op.name },
                            label = { Text(op.shortName) }
                        )
                    }
                }
            }

            // 3. Post Feed Cards
            if (filteredPosts.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucune publication trouvée",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Soyez le premier à publier un signalement ou un bon plan de forfait pour votre zone !",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        onLikeClick = { onToggleLikePost(post.id) },
                        onCommentClick = { activeCommentsPost = post },
                        onShareClick = { sharePostExternally(context, post) },
                        onCopyPlan = {
                            onCopyPlanToComparator(post)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Forfait ${post.operatorKey} copié dans votre comparateur !",
                                    actionLabel = "Comparer"
                                )
                            }
                        },
                        onNavigateToComparator = onNavigateToComparator
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showPublishDialog) {
        PublishPostDialog(
            onDismiss = { showPublishDialog = false },
            onPublish = { authorName, postType, operatorKey, planType, location, title, content, problemLevel, durationMinutes, isResolved, priceFcfa, dataMb, callMinutes, smsCount, validityDays ->
                onPublishPost(
                    authorName,
                    postType,
                    operatorKey,
                    planType,
                    location,
                    title,
                    content,
                    problemLevel,
                    durationMinutes,
                    isResolved,
                    priceFcfa,
                    dataMb,
                    callMinutes,
                    smsCount,
                    validityDays
                )
                scope.launch {
                    snackbarHostState.showSnackbar("Publication partagée avec succès à la communauté !")
                }
            }
        )
    }

    activeCommentsPost?.let { post ->
        val commentsFlow = remember(post.id) { onGetComments(post.id) }
        val comments by commentsFlow.collectAsStateWithLifecycle(emptyList())

        CommentsSheet(
            post = post,
            comments = comments,
            onDismiss = { activeCommentsPost = null },
            onAddComment = { parentId, replyTo, text, author ->
                onAddComment(post.id, parentId, replyTo, text, author)
            },
            onToggleLikeComment = onToggleLikeComment
        )
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPostEntity,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyPlan: () -> Unit,
    onNavigateToComparator: () -> Unit
) {
    val op = MobileOperator.entries.find { it.name.equals(post.operatorKey, ignoreCase = true) } ?: MobileOperator.MTN
    val isIncident = post.postType == "INCIDENT"
    var planCopiedState by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_post_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(post.authorAvatarColor),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = post.authorName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "${post.timeFormatted} • ${post.dateFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // Post type tags row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Operator tag
                Surface(
                    color = op.brandColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                            text = op.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = op.brandColor
                        )
                    }
                }

                // Type tag
                if (isIncident) {
                    Surface(
                        color = BlockedCoral.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = BlockedCoral, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Signalement Panne", style = MaterialTheme.typography.labelSmall, color = BlockedCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (post.problemLevel.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = post.problemLevel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        color = UploadEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Savings, contentDescription = null, tint = UploadEmerald, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Bon Plan Forfait", style = MaterialTheme.typography.labelSmall, color = UploadEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = post.planType,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title & Content
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // SPECIAL PLAN OFFER BOX WITH COPY BUTTON FOR COMPARISON & ESTIMATION
            if (!isIncident && post.priceFcfa > 0) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, op.brandColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val volumeText = (post.dataMb / 1024.0).let {
                                    if (it >= 1.0) "${it.roundToInt()} Go" else "${post.dataMb.roundToInt()} Mo"
                                }
                                Text(
                                    text = "$volumeText • ${post.priceFcfa.roundToInt()} FCFA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = op.brandColor
                                )
                                Text(
                                    text = "Validité : ${post.validityDays} jours" + if (post.callMinutes > 0) " • ${post.callMinutes} min d'appels" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val rendMoPer100 = ((post.dataMb / post.priceFcfa) * 100.0).roundToInt()
                            Surface(
                                color = op.brandColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$rendMoPer100 Mo/100F",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                    color = op.brandColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // COPIER POUR COMPARER BUTTON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCopyPlan()
                                    planCopiedState = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (planCopiedState) UploadEmerald else op.brandColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_plan_button_${post.id}")
                            ) {
                                Icon(
                                    imageVector = if (planCopiedState) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = if (planCopiedState) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (planCopiedState) "Copié dans comparateur !" else "Copier pour comparer",
                                    fontWeight = FontWeight.Bold,
                                    color = if (planCopiedState) Color.White else Color.Black,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (planCopiedState) {
                                OutlinedButton(
                                    onClick = onNavigateToComparator,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Voir", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // SOCIAL ACTION BAR: Like, Comment, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("like_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (post.isLikedByMe) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.likeCount}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (post.isLikedByMe) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comment Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCommentClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("comment_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Commentaires",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.commentCount}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // External Share Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShareClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("share_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Partager en externe",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Partager",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun sharePostExternally(context: Context, post: CommunityPostEntity) {
    val shareContent = if (post.postType == "INCIDENT") {
        buildString {
            append("⚠️ [FluxNet - Signalement Réseau]\n")
            append("Opérateur : ${post.operatorKey} (${post.planType})\n")
            append("Lieu : ${post.location}\n")
            append("Gravité : ${post.problemLevel}\n")
            append("Titre : ${post.title}\n\n")
            append("${post.content}\n\n")
            append("Publié via FluxNet")
        }
    } else {
        val vol = (post.dataMb / 1024.0).let {
            if (it >= 1.0) "${it.roundToInt()} Go" else "${post.dataMb.roundToInt()} Mo"
        }
        buildString {
            append("💡 [FluxNet - Bon Plan Forfait Réseau]\n")
            append("Opérateur : ${post.operatorKey} (${post.planType})\n")
            append("Offre : ${post.title}\n")
            append("Tarif : ${post.priceFcfa.roundToInt()} FCFA pour $vol (Validité ${post.validityDays} jours)\n")
            append("Lieu : ${post.location}\n\n")
            append("${post.content}\n\n")
            append("Copiez ce forfait dans FluxNet pour estimer vos dépenses et comparer !")
        }
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareContent)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(sendIntent, "Partager ce signalement / forfait via")
    context.startActivity(chooser)
}
