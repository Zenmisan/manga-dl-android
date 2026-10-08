package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.gamification.HunterRank
import com.mangadl.android.data.gamification.MILESTONES
import com.mangadl.android.data.gamification.MilestoneBadge
import com.mangadl.android.data.gamification.UserMilestoneSummary
import com.mangadl.android.data.gamification.calculateRankProgress
import com.mangadl.android.data.gamification.calculateReaderScore
import com.mangadl.android.data.gamification.getHunterRank
import com.mangadl.android.data.gamification.getUserMilestones
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.ui.currentStreak
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HunterProfileState(
    val readerScore: Int = 0,
    val hunterRank: HunterRank = getHunterRank(0),
    val rankProgress: Float = 0f,
    val milestoneSummary: UserMilestoneSummary = getUserMilestones(0, 0, 0),
    val pinnedBadges: Set<String> = emptySet(),
    val pinnedBadgeObjects: List<MilestoneBadge> = emptyList()
)

class ProfileViewModel : ViewModel() {
    private val app = MangaDlApp.instance
    private val db = app.database
    private val prefs = AppPreferences.getInstance(app)

    val library: StateFlow<List<LibraryManga>> = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentActivity: StateFlow<List<ReadingProgress>> = db.progressDao().getRecent(5)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalChaptersRead: StateFlow<Int> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { items -> items.count { it.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak: StateFlow<Int> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { it.currentStreak() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hunterState: StateFlow<HunterProfileState> = combine(
        totalChaptersRead,
        streak,
        library,
        prefs.pinnedBadges
    ) { chapters, streakDays, libList, pinnedIds ->
        val score = calculateReaderScore(chapters, streakDays, libList.size)
        val rank = getHunterRank(score)
        val progress = calculateRankProgress(score, rank)
        val summary = getUserMilestones(chapters, streakDays, libList.size)

        // Resolve pinned badges: if user has explicit pins, use those that exist in MILESTONES;
        // otherwise default to up to 4 most recently unlocked badges.
        val resolvedBadges = if (pinnedIds.isNotEmpty()) {
            pinnedIds.mapNotNull { id -> MILESTONES.find { it.id == id } }
        } else {
            summary.unlocked.takeLast(4).reversed()
        }

        HunterProfileState(
            readerScore = score,
            hunterRank = rank,
            rankProgress = progress,
            milestoneSummary = summary,
            pinnedBadges = pinnedIds,
            pinnedBadgeObjects = resolvedBadges
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HunterProfileState())

    fun togglePinnedBadge(badgeId: String) {
        viewModelScope.launch {
            prefs.togglePinnedBadge(badgeId, maxCount = 4)
        }
    }
}
