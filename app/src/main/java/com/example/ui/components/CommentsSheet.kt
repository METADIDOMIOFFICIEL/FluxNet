package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.data.local.CommunityPostEntity
import com.example.data.local.PostCommentEntity
import com.example.ui.theme.BlockedCoral
import com.example.ui.theme.DownloadCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    post: CommunityPostEntity,
    comments: List<PostCommentEntity>,
    onDismiss: () -> Unit,
    onAddComment: (parentCommentId: Long?, replyToAuthor: String?, text: String, authorName: String) -> Unit,
    onToggleLikeComment: (commentId: Long, currentCount: Int, isLiked: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentInputText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<PostCommentEntity?>(null) }
    var userPseudo by remember { mutableStateOf("Vous") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Commentaires (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fermer")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun commentaire pour le moment.\nSoyez le premier à réagir !",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Separate root comments and their replies
                    val rootComments = comments.filter { it.parentCommentId == null }
                    val repliesByParent = comments.filter { it.parentCommentId != null }
                        .groupBy { it.parentCommentId }

                    items(rootComments, key = { it.id }) { rootComment ->
                        CommentItem(
                            comment = rootComment,
                            isReply = false,
                            onReplyClick = {
                                replyingToComment = rootComment
                                commentInputText = "@${rootComment.authorName} "
                            },
                            onLikeClick = {
                                onToggleLikeComment(
                                    rootComment.id,
                                    rootComment.likeCount,
                                    rootComment.isLikedByMe
                                )
                            }
                        )

                        // Render threaded replies
                        repliesByParent[rootComment.id]?.forEach { reply ->
                            CommentItem(
                                comment = reply,
                                isReply = true,
                                onReplyClick = {
                                    replyingToComment = rootComment
                                    commentInputText = "@${reply.authorName} "
                                },
                                onLikeClick = {
                                    onToggleLikeComment(
                                        reply.id,
                                        reply.likeCount,
                                        reply.isLikedByMe
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Replying banner
            if (replyingToComment != null) {
                Surface(
                    color = DownloadCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Reply,
                                contentDescription = null,
                                tint = DownloadCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "En réponse à ${replyingToComment?.authorName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = DownloadCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                replyingToComment = null
                                if (commentInputText.startsWith("@")) commentInputText = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Annuler réponse",
                                tint = DownloadCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInputText,
                    onValueChange = { commentInputText = it },
                    placeholder = {
                        Text(
                            text = if (replyingToComment != null) "Votre réponse..." else "Ajouter un commentaire..."
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = commentInputText.trim()
                        if (text.isNotBlank()) {
                            val replyTo = replyingToComment?.authorName
                            val parentId = replyingToComment?.id
                            onAddComment(parentId, replyTo, text, userPseudo)
                            commentInputText = ""
                            replyingToComment = null
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DownloadCyan)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Envoyer",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: PostCommentEntity,
    isReply: Boolean,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 36.dp else 0.dp)
            .testTag("comment_item_${comment.id}")
    ) {
        // Author initial circle
        Surface(
            shape = CircleShape,
            color = Color(comment.authorAvatarColor),
            modifier = Modifier.size(if (isReply) 26.dp else 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = comment.authorName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${comment.timeFormatted} • ${comment.dateFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (comment.replyToAuthor != null) {
                        Text(
                            text = "En réponse à @${comment.replyToAuthor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DownloadCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = comment.commentText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Actions row: Répondre + Like
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onReplyClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Répondre",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.clickable { onLikeClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (comment.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (comment.isLikedByMe) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (comment.likeCount > 0) "${comment.likeCount}" else "J'aime",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.isLikedByMe) BlockedCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
