package com.mangadl.android.ui.screens.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.data.model.CommentItem
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.CommentsUiState
import com.mangadl.android.ui.viewmodels.CommentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSheet(
    provider: String,
    mangaId: String,
    chapterId: String? = null,
    title: String = "Comments",
    subtitle: String? = null,
    onDismiss: () -> Unit,
    vm: CommentsViewModel = viewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vm.state.collectAsState()
    val isPosting by vm.isPosting.collectAsState()
    val c = MdTheme.colors

    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentItem?>(null) }

    LaunchedEffect(provider, mangaId, chapterId) {
        vm.loadComments(provider, mangaId, chapterId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.sheet,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val countText = when (val s = state) {
                        is CommentsUiState.Success -> " (${s.total})"
                        else -> ""
                    }
                    BodyText("$title$countText", size = 18.sp, weight = FontWeight.ExtraBold)
                    if (!subtitle.isNullOrBlank()) {
                        BodyText(subtitle, size = 12.sp, color = c.fgMuted, maxLines = 1)
                    }
                }
                MdIconButton(MdIcons.Close, "Close comments", onDismiss)
            }
            Divider(color = c.surfaceHigh)

            // Content
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    is CommentsUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(36.dp))
                        }
                    }
                    is CommentsUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BodyText(s.message, color = c.fgMuted)
                        }
                    }
                    is CommentsUiState.Success -> {
                        if (s.comments.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        MdIcons.Chat,
                                        contentDescription = null,
                                        tint = c.fgSubtle,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    BodyText("No comments yet", size = 16.sp, weight = FontWeight.Bold)
                                    BodyText(
                                        "Be the first to share your thoughts on this chapter!",
                                        size = 13.sp,
                                        color = c.fgMuted,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(s.comments, key = { it.id }) { item ->
                                    CommentCard(
                                        comment = item,
                                        onLike = { vm.toggleLike(item.id) },
                                        onReply = { replyingTo = item },
                                        onLikeReply = { replyId -> vm.toggleLike(replyId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = c.surfaceHigh)

            // Reply banner
            if (replyingTo != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.surfaceHigh)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BodyText(
                        "Replying to @${replyingTo?.displayName?.ifBlank { replyingTo?.username }}",
                        size = 12.sp,
                        color = c.accentSoft,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        MdIcons.Close,
                        contentDescription = "Cancel reply",
                        tint = c.fgMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { replyingTo = null }
                    )
                }
            }

            // Input Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        BodyText(
                            if (replyingTo != null) "Write a reply..." else "Add a comment...",
                            size = 13.sp,
                            color = c.fgMuted
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.surfaceHigh,
                        focusedContainerColor = c.bg,
                        unfocusedContainerColor = c.bg,
                    ),
                    maxLines = 3,
                )

                MdIconButton(
                    icon = MdIcons.Send,
                    contentDescription = "Post comment",
                    onClick = {
                        val text = commentText.trim()
                        if (text.isNotEmpty() && !isPosting) {
                            vm.postComment(
                                text = text,
                                parentId = replyingTo?.id,
                                onSuccess = {
                                    commentText = ""
                                    replyingTo = null
                                }
                            )
                        }
                    },
                    background = if (commentText.isNotBlank()) c.accent else c.surfaceHigh,
                    tint = if (commentText.isNotBlank()) Color.White else c.fgMuted,
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: CommentItem,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onLikeReply: (String) -> Unit,
) {
    val c = MdTheme.colors
    val displayName = comment.displayName.ifBlank { comment.username.ifBlank { "Reader" } }
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.accentFaint),
                contentAlignment = Alignment.Center,
            ) {
                BodyText(initial, size = 13.sp, weight = FontWeight.Bold, color = c.accentSoft)
            }
            Column(Modifier.weight(1f)) {
                BodyText(displayName, size = 13.sp, weight = FontWeight.Bold)
                if (comment.createdAt.isNotBlank()) {
                    val dateFormatted = comment.createdAt.take(10)
                    BodyText(dateFormatted, size = 11.sp, color = c.fgSubtle)
                }
            }
        }

        BodyText(comment.body, size = 13.sp, color = c.fg)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onLike)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (comment.liked) MdIcons.HeartFilled else MdIcons.Heart,
                    contentDescription = "Like",
                    tint = if (comment.liked) Color(0xFFEF4444) else c.fgMuted,
                    modifier = Modifier.size(15.dp)
                )
                if (comment.likes > 0) {
                    BodyText(
                        "${comment.likes}",
                        size = 12.sp,
                        color = if (comment.liked) Color(0xFFEF4444) else c.fgMuted,
                        weight = FontWeight.SemiBold
                    )
                }
            }

            BodyText(
                "Reply",
                Modifier
                    .clickable(onClick = onReply)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                size = 12.sp,
                color = c.accentSoft,
                weight = FontWeight.SemiBold
            )
        }

        // Replies thread
        if (comment.replies.isNotEmpty()) {
            Column(
                Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                comment.replies.forEach { reply ->
                    ReplyItem(
                        reply = reply,
                        onLike = { onLikeReply(reply.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: CommentItem,
    onLike: () -> Unit,
) {
    val c = MdTheme.colors
    val displayName = reply.displayName.ifBlank { reply.username.ifBlank { "Reader" } }
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceHigh.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(c.accentFaint),
                contentAlignment = Alignment.Center,
            ) {
                BodyText(initial, size = 11.sp, weight = FontWeight.Bold, color = c.accentSoft)
            }
            Column(Modifier.weight(1f)) {
                BodyText(displayName, size = 12.sp, weight = FontWeight.Bold)
                if (reply.createdAt.isNotBlank()) {
                    BodyText(reply.createdAt.take(10), size = 10.sp, color = c.fgSubtle)
                }
            }
        }

        BodyText(reply.body, size = 12.sp, color = c.fg)

        Row(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onLike)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (reply.liked) MdIcons.HeartFilled else MdIcons.Heart,
                contentDescription = "Like",
                tint = if (reply.liked) Color(0xFFEF4444) else c.fgMuted,
                modifier = Modifier.size(13.dp)
            )
            if (reply.likes > 0) {
                BodyText(
                    "${reply.likes}",
                    size = 11.sp,
                    color = if (reply.liked) Color(0xFFEF4444) else c.fgMuted,
                    weight = FontWeight.SemiBold
                )
            }
        }
    }
}
