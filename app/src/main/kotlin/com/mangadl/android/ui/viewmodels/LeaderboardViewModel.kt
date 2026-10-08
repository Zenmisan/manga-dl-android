package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.gamification.HunterRank
import com.mangadl.android.data.gamification.calculateReaderScore
import com.mangadl.android.data.gamification.getHunterRank
import com.mangadl.android.data.model.HunterRankPayload
import com.mangadl.android.data.model.LeaderboardEntry
import com.mangadl.android.data.repository.LeaderboardRepository
import com.mangadl.android.data.ui.currentStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LeaderboardPeriod(val queryParam: String, val label: String) {
    ALL_TIME("all_time", "All-Time"),
    YEARLY("yearly", "Yearly"),
    MONTHLY("monthly", "Monthly"),
    WEEKLY("weekly", "Weekly"),
}

class LeaderboardViewModel(
    private val repo: LeaderboardRepository = LeaderboardRepository(),
) : ViewModel() {
    private val app = MangaDlApp.instance
    private val db = app.database

    private val _selectedPeriod = MutableStateFlow(LeaderboardPeriod.ALL_TIME)
    val selectedPeriod: StateFlow<LeaderboardPeriod> = _selectedPeriod.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()

    val totalChaptersRead = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { items -> items.count { it.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { it.currentStreak() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val library = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val localUserScore: StateFlow<Int> = combine(
        totalChaptersRead,
        streak,
        library
    ) { ch, strk, lib ->
        calculateReaderScore(ch, strk, lib.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val localHunterRank: StateFlow<HunterRank> = localUserScore.map { score ->
        getHunterRank(score)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), getHunterRank(0))

    init {
        loadLeaderboard(LeaderboardPeriod.ALL_TIME)
    }

    fun selectPeriod(period: LeaderboardPeriod) {
        _selectedPeriod.value = period
        loadLeaderboard(period)
    }

    fun loadLeaderboard(period: LeaderboardPeriod = _selectedPeriod.value) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repo.getLeaderboard(period.queryParam)
            result.onSuccess { remoteEntries ->
                if (remoteEntries.isNotEmpty()) {
                    _entries.value = remoteEntries
                } else {
                    _entries.value = generateFallbackHunters()
                }
            }.onFailure {
                _entries.value = generateFallbackHunters()
            }
            _isLoading.value = false
        }
    }

    private fun generateFallbackHunters(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(
                userId = "1",
                username = "SungJinWoo",
                displayName = "Sung Jin-Woo",
                bio = "Arise.",
                chaptersRead = 2840,
                streakDays = 180,
                mangaCount = 142,
                score = 40950,
                rank = 1,
                hunterRank = HunterRankPayload("MONARCH", "Shadow Monarch", "mythic")
            ),
            LeaderboardEntry(
                userId = "2",
                username = "ChaHaeIn",
                displayName = "Cha Hae-In",
                bio = "Sword Dance Guild Master",
                chaptersRead = 1750,
                streakDays = 120,
                mangaCount = 88,
                score = 25700,
                rank = 2,
                hunterRank = HunterRankPayload("S", "S-Rank Hunter", "diamond")
            ),
            LeaderboardEntry(
                userId = "3",
                username = "ThomasAndre",
                displayName = "Thomas Andre",
                bio = "Scavenger Guild Leader",
                chaptersRead = 1420,
                streakDays = 94,
                mangaCount = 76,
                score = 20800,
                rank = 3,
                hunterRank = HunterRankPayload("S", "S-Rank Hunter", "diamond")
            ),
            LeaderboardEntry(
                userId = "4",
                username = "LiuZhigang",
                displayName = "Liu Zhigang",
                bio = "Hero of the East",
                chaptersRead = 1100,
                streakDays = 75,
                mangaCount = 60,
                score = 16250,
                rank = 4,
                hunterRank = HunterRankPayload("S", "S-Rank Hunter", "diamond")
            ),
            LeaderboardEntry(
                userId = "5",
                username = "GoGunhee",
                displayName = "Go Gun-Hee",
                bio = "Hunter Association Chairman",
                chaptersRead = 890,
                streakDays = 60,
                mangaCount = 52,
                score = 13200,
                rank = 5,
                hunterRank = HunterRankPayload("A", "A-Rank Hunter", "platinum")
            ),
            LeaderboardEntry(
                userId = "6",
                username = "WooJinchul",
                displayName = "Woo Jin-Chul",
                bio = "Chief Surveillance Inspector",
                chaptersRead = 640,
                streakDays = 42,
                mangaCount = 40,
                score = 9500,
                rank = 6,
                hunterRank = HunterRankPayload("A", "A-Rank Hunter", "platinum")
            ),
            LeaderboardEntry(
                userId = "7",
                username = "BaekYoonho",
                displayName = "Baek Yoon-Ho",
                bio = "White Tiger Guild",
                chaptersRead = 450,
                streakDays = 30,
                mangaCount = 35,
                score = 6875,
                rank = 7,
                hunterRank = HunterRankPayload("B", "B-Rank Hunter", "gold")
            ),
            LeaderboardEntry(
                userId = "8",
                username = "ChoiJongin",
                displayName = "Choi Jong-In",
                bio = "Ultimate Soldier Guild",
                chaptersRead = 310,
                streakDays = 25,
                mangaCount = 28,
                score = 5050,
                rank = 8,
                hunterRank = HunterRankPayload("B", "B-Rank Hunter", "gold")
            )
        )
    }
}
