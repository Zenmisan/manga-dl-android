package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.data.model.CommentItem
import com.mangadl.android.data.repository.CommentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CommentsUiState {
    data object Loading : CommentsUiState
    data class Success(val comments: List<CommentItem>, val total: Int) : CommentsUiState
    data class Error(val message: String) : CommentsUiState
}

class CommentsViewModel(
    private val repo: CommentsRepository = CommentsRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private var currentProvider: String = ""
    private var currentMangaId: String = ""
    private var currentChapterId: String? = null

    fun loadComments(provider: String, mangaId: String, chapterId: String? = null) {
        currentProvider = provider
        currentMangaId = mangaId
        currentChapterId = chapterId
        _state.value = CommentsUiState.Loading

        viewModelScope.launch {
            val result = repo.getComments(provider, mangaId, chapterId)
            result.onSuccess { resp ->
                _state.value = CommentsUiState.Success(resp.comments, resp.total)
            }.onFailure {
                // Offline-first graceful fallback: empty list
                _state.value = CommentsUiState.Success(emptyList(), 0)
            }
        }
    }

    fun postComment(
        text: String,
        parentId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        if (text.isBlank()) return
        _isPosting.value = true

        viewModelScope.launch {
            val result = repo.postComment(
                provider = currentProvider,
                mangaId = currentMangaId,
                chapterId = currentChapterId,
                parentId = parentId,
                text = text,
            )
            _isPosting.value = false
            result.onSuccess { newComment ->
                if (newComment != null) {
                    val current = _state.value
                    if (current is CommentsUiState.Success) {
                        if (parentId == null) {
                            _state.value = current.copy(
                                comments = listOf(newComment) + current.comments,
                                total = current.total + 1
                            )
                        } else {
                            val updated = current.comments.map { c ->
                                if (c.id == parentId) {
                                    c.copy(replies = c.replies + newComment)
                                } else c
                            }
                            _state.value = current.copy(comments = updated, total = current.total + 1)
                        }
                    } else {
                        _state.value = CommentsUiState.Success(listOf(newComment), 1)
                    }
                }
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to post comment")
            }
        }
    }

    fun toggleLike(commentId: String) {
        val current = _state.value as? CommentsUiState.Success ?: return
        val updated = current.comments.map { c ->
            if (c.id == commentId) {
                val newLiked = !c.liked
                val newLikes = if (newLiked) c.likes + 1 else maxOf(0, c.likes - 1)
                c.copy(liked = newLiked, likes = newLikes)
            } else {
                val updatedReplies = c.replies.map { r ->
                    if (r.id == commentId) {
                        val newLiked = !r.liked
                        val newLikes = if (newLiked) r.likes + 1 else maxOf(0, r.likes - 1)
                        r.copy(liked = newLiked, likes = newLikes)
                    } else r
                }
                c.copy(replies = updatedReplies)
            }
        }
        _state.value = current.copy(comments = updated)

        viewModelScope.launch {
            repo.toggleLike(commentId)
        }
    }
}
