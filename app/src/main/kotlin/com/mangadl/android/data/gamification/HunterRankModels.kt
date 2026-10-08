package com.mangadl.android.data.gamification

import androidx.compose.ui.graphics.Color

enum class MilestoneCategory(val label: String) {
    CHAPTERS("Chapters"),
    LIBRARY("Library"),
    STREAK("Streak")
}

enum class MilestoneTier(
    val label: String,
    val color: Color,
    val bgColor: Color,
    val borderColor: Color,
    val glowColor: Color
) {
    BRONZE(
        label = "Bronze",
        color = Color(0xFFCD7F32),
        bgColor = Color(0x3378350F),
        borderColor = Color(0x66B45309),
        glowColor = Color(0x40CD7F32)
    ),
    SILVER(
        label = "Silver",
        color = Color(0xFF94A3B8),
        bgColor = Color(0x33334155),
        borderColor = Color(0x6664748B),
        glowColor = Color(0x4094A3B8)
    ),
    GOLD(
        label = "Gold",
        color = Color(0xFFF59E0B),
        bgColor = Color(0x3378350F),
        borderColor = Color(0x66F59E0B),
        glowColor = Color(0x4DF59E0B)
    ),
    PLATINUM(
        label = "Platinum",
        color = Color(0xFF38BDF8),
        bgColor = Color(0x330369A1),
        borderColor = Color(0x6638BDF8),
        glowColor = Color(0x4D38BDF8)
    ),
    DIAMOND(
        label = "Diamond",
        color = Color(0xFFA855F7),
        bgColor = Color(0x33581C87),
        borderColor = Color(0x66A855F7),
        glowColor = Color(0x59A855F7)
    ),
    MYTHIC(
        label = "Mythic",
        color = Color(0xFFEF4444),
        bgColor = Color(0x337F1D1D),
        borderColor = Color(0x66EF4444),
        glowColor = Color(0x73EF4444)
    )
}

data class MilestoneBadge(
    val id: String,
    val title: String,
    val category: MilestoneCategory,
    val threshold: Int,
    val description: String,
    val iconName: String,
    val tier: MilestoneTier
) {
    val color: Color get() = tier.color
}

data class HunterRank(
    val code: String,
    val name: String,
    val tier: MilestoneTier,
    val tag: String,
    val minScore: Int,
    val nextScore: Int,
    val color: Color,
    val bgColor: Color,
    val borderColor: Color,
    val glowColor: Color
)

data class MilestoneProgress(
    val badge: MilestoneBadge,
    val current: Int,
    val total: Int,
    val percent: Int,
    val remaining: Int
)

data class UserMilestoneSummary(
    val unlocked: List<MilestoneBadge>,
    val locked: List<MilestoneBadge>,
    val totalCount: Int,
    val unlockedCount: Int,
    val currentTitle: String,
    val currentTitleTier: MilestoneTier,
    val nextMilestone: MilestoneProgress?,
    val categoryCounts: Map<MilestoneCategory, Pair<Int, Int>>
)

fun calculateReaderScore(chaptersRead: Int, streakDays: Int, libraryCount: Int): Int {
    val ch = maxOf(0, chaptersRead)
    val strk = maxOf(0, streakDays)
    val lib = maxOf(0, libraryCount)
    return (ch * 10) + (strk * 50) + (lib * 25)
}

fun getHunterRank(score: Int, isWorldFirst: Boolean = false): HunterRank {
    if (isWorldFirst && score > 0) {
        return HunterRank(
            code = "MONARCH",
            name = "Shadow Monarch",
            tier = MilestoneTier.MYTHIC,
            tag = "#1 Sovereign",
            minScore = 15000,
            nextScore = 15000,
            color = Color(0xFFD8B4FE),
            bgColor = Color(0x4D3B0764),
            borderColor = Color(0x80A855F7),
            glowColor = Color(0x66A855F7)
        )
    }
    return when {
        score >= 15000 -> HunterRank(
            code = "S",
            name = "S-Rank Hunter",
            tier = MilestoneTier.DIAMOND,
            tag = "Apex Elite",
            minScore = 15000,
            nextScore = 15000,
            color = Color(0xFFD8B4FE),
            bgColor = Color(0x3B3B0764),
            borderColor = Color(0x66C084FC),
            glowColor = Color(0x4DA855F7)
        )
        score >= 8000 -> HunterRank(
            code = "A",
            name = "A-Rank Hunter",
            tier = MilestoneTier.PLATINUM,
            tag = "High Guild",
            minScore = 8000,
            nextScore = 15000,
            color = Color(0xFF7DD3FC),
            bgColor = Color(0x330369A1),
            borderColor = Color(0x6638BDF8),
            glowColor = Color(0x4D38BDF8)
        )
        score >= 4000 -> HunterRank(
            code = "B",
            name = "B-Rank Hunter",
            tier = MilestoneTier.GOLD,
            tag = "Veteran",
            minScore = 4000,
            nextScore = 8000,
            color = Color(0xFFFCD34D),
            bgColor = Color(0x3378350F),
            borderColor = Color(0x66F59E0B),
            glowColor = Color(0x4DF59E0B)
        )
        score >= 1500 -> HunterRank(
            code = "C",
            name = "C-Rank Hunter",
            tier = MilestoneTier.SILVER,
            tag = "Raid Ready",
            minScore = 1500,
            nextScore = 4000,
            color = Color(0xFFCBD5E1),
            bgColor = Color(0x331E293B),
            borderColor = Color(0x6694A3B8),
            glowColor = Color(0x4094A3B8)
        )
        score >= 500 -> HunterRank(
            code = "D",
            name = "D-Rank Hunter",
            tier = MilestoneTier.BRONZE,
            tag = "Dungeon Scavenger",
            minScore = 500,
            nextScore = 1500,
            color = Color(0xFFFB923C),
            bgColor = Color(0x33451A03),
            borderColor = Color(0x66C2410C),
            glowColor = Color(0x33CD7F32)
        )
        else -> HunterRank(
            code = "E",
            name = "E-Rank Novice",
            tier = MilestoneTier.BRONZE,
            tag = "Awakened Novice",
            minScore = 0,
            nextScore = 500,
            color = Color(0xFFA1A1AA),
            bgColor = Color(0x3327272A),
            borderColor = Color(0x4052525B),
            glowColor = Color(0x2671717A)
        )
    }
}

fun calculateRankProgress(score: Int, rank: HunterRank): Float {
    if (rank.nextScore <= rank.minScore) return 1f
    val current = (score - rank.minScore).toFloat()
    val span = (rank.nextScore - rank.minScore).toFloat()
    return (current / span).coerceIn(0f, 1f)
}

val MILESTONES: List<MilestoneBadge> = listOf(
    // Chapters Read (15 Tiers)
    MilestoneBadge("first_page", "First Page", MilestoneCategory.CHAPTERS, 1, "Took the very first step into the panel multiverse.", "BookOpen", MilestoneTier.BRONZE),
    MilestoneBadge("page_turner", "Page Turner", MilestoneCategory.CHAPTERS, 10, "Getting hooked on the panel flow and cliffhangers.", "BookOpen", MilestoneTier.BRONZE),
    MilestoneBadge("casual_reader", "Casual Reader", MilestoneCategory.CHAPTERS, 25, "Finding curiosity in every story arc.", "Feather", MilestoneTier.BRONZE),
    MilestoneBadge("manga_enthusiast", "Manga Enthusiast", MilestoneCategory.CHAPTERS, 50, "A dependable appetite for weekly releases.", "Scroll", MilestoneTier.SILVER),
    MilestoneBadge("volume_devourer", "Volume Devourer", MilestoneCategory.CHAPTERS, 100, "Consumed a physical shelf worth of tankōbon.", "BookOpen", MilestoneTier.SILVER),
    MilestoneBadge("arc_conqueror", "Arc Conqueror", MilestoneCategory.CHAPTERS, 250, "Cruised through epic sagas without blinking.", "Shield", MilestoneTier.SILVER),
    MilestoneBadge("binge_specialist", "Binge Specialist", MilestoneCategory.CHAPTERS, 500, "Lost entire weekends to non-stop chapter binges.", "Zap", MilestoneTier.GOLD),
    MilestoneBadge("panel_virtuoso", "Panel Virtuoso", MilestoneCategory.CHAPTERS, 750, "Appreciating every masterstroke and speed line.", "Award", MilestoneTier.GOLD),
    MilestoneBadge("four_digit_club", "Four-Digit Club", MilestoneCategory.CHAPTERS, 1000, "Crossed the 1,000 chapter milestone. Truly unstoppable.", "Trophy", MilestoneTier.GOLD),
    MilestoneBadge("lore_master", "Lore Master", MilestoneCategory.CHAPTERS, 1500, "Anticipating plot twists three arcs ahead of the canon.", "Compass", MilestoneTier.PLATINUM),
    MilestoneBadge("ink_veteran", "Ink Veteran", MilestoneCategory.CHAPTERS, 2500, "Years of serialized releases cannot quench your thirst.", "Star", MilestoneTier.PLATINUM),
    MilestoneBadge("grand_reader", "Grand Reader", MilestoneCategory.CHAPTERS, 4000, "A walking encyclopedia of character arcs and legends.", "Sparkles", MilestoneTier.PLATINUM),
    MilestoneBadge("domain_sovereign", "Domain Sovereign", MilestoneCategory.CHAPTERS, 6000, "Expanded your reading domain across countless universes.", "Crown", MilestoneTier.DIAMOND),
    MilestoneBadge("mythic_scholar", "Mythic Scholar", MilestoneCategory.CHAPTERS, 8500, "Few mortals possess such boundless panel wisdom.", "Crown", MilestoneTier.DIAMOND),
    MilestoneBadge("ascended_otaku", "Ascended Otaku", MilestoneCategory.CHAPTERS, 10000, "Transcended the mortal realm of manga reading.", "Infinity", MilestoneTier.MYTHIC),

    // Library Size (8 Tiers)
    MilestoneBadge("solo_diver", "Solo Diver", MilestoneCategory.LIBRARY, 1, "Laser focus on a single captivating story journey.", "Compass", MilestoneTier.BRONZE),
    MilestoneBadge("genre_curious", "Genre Curious", MilestoneCategory.LIBRARY, 5, "Sampling distinct tropes, genres, and art styles.", "Library", MilestoneTier.BRONZE),
    MilestoneBadge("shelf_builder", "Shelf Builder", MilestoneCategory.LIBRARY, 10, "Curating a personalized library lineup.", "Library", MilestoneTier.SILVER),
    MilestoneBadge("apprentice_curator", "Apprentice Curator", MilestoneCategory.LIBRARY, 25, "A rich palette spanning shonen, seinen, and beyond.", "Library", MilestoneTier.SILVER),
    MilestoneBadge("manga_collector", "Manga Collector", MilestoneCategory.LIBRARY, 50, "A flourishing digital collection spanning dozens of worlds.", "Library", MilestoneTier.GOLD),
    MilestoneBadge("grand_bibliophile", "Grand Bibliophile", MilestoneCategory.LIBRARY, 100, "Master of over a hundred distinct story universes.", "Award", MilestoneTier.PLATINUM),
    MilestoneBadge("multiverse_voyager", "Multiverse Voyager", MilestoneCategory.LIBRARY, 200, "Navigated hundreds of timelines, worlds, and characters.", "Sparkles", MilestoneTier.DIAMOND),
    MilestoneBadge("archive_sovereign", "Archive Sovereign", MilestoneCategory.LIBRARY, 350, "Custodian of a monumental personal manga repository.", "Crown", MilestoneTier.MYTHIC),

    // Streak / Dedication (8 Tiers)
    MilestoneBadge("ignition_spark", "Ignition Spark", MilestoneCategory.STREAK, 1, "Ignited the flame of daily manga reading.", "Flame", MilestoneTier.BRONZE),
    MilestoneBadge("momentum", "Momentum", MilestoneCategory.STREAK, 3, "Building a steady, continuous reading rhythm.", "Flame", MilestoneTier.BRONZE),
    MilestoneBadge("weekly_devotee", "Weekly Devotee", MilestoneCategory.STREAK, 7, "A full 7-day week of uninterrupted daily reading.", "Flame", MilestoneTier.SILVER),
    MilestoneBadge("fortnight_fanatic", "Fortnight Fanatic", MilestoneCategory.STREAK, 14, "Two solid weeks without missing a single day.", "Flame", MilestoneTier.SILVER),
    MilestoneBadge("monthly_habit", "Monthly Habit", MilestoneCategory.STREAK, 30, "One solid month of daily manga immersion.", "Flame", MilestoneTier.GOLD),
    MilestoneBadge("seasoned_soul", "Seasoned Soul", MilestoneCategory.STREAK, 60, "A continuous reading streak persevering across seasons.", "Flame", MilestoneTier.PLATINUM),
    MilestoneBadge("centurion_flame", "Centurion Flame", MilestoneCategory.STREAK, 100, "Triple-digit daily streak. Dedication forged in iron.", "Flame", MilestoneTier.DIAMOND),
    MilestoneBadge("eternal_flame", "Eternal Flame", MilestoneCategory.STREAK, 365, "A full 365 days of daily reading. A living legend.", "Crown", MilestoneTier.MYTHIC)
)

fun getUserMilestones(chaptersRead: Int, streakDays: Int, libraryCount: Int): UserMilestoneSummary {
    val ch = maxOf(0, chaptersRead)
    val strk = maxOf(0, streakDays)
    val lib = maxOf(0, libraryCount)

    fun valueFor(cat: MilestoneCategory): Int = when (cat) {
        MilestoneCategory.CHAPTERS -> ch
        MilestoneCategory.LIBRARY -> lib
        MilestoneCategory.STREAK -> strk
    }

    val unlocked = mutableListOf<MilestoneBadge>()
    val locked = mutableListOf<MilestoneBadge>()

    val categoryCounts = mutableMapOf(
        MilestoneCategory.CHAPTERS to Pair(0, 0),
        MilestoneCategory.LIBRARY to Pair(0, 0),
        MilestoneCategory.STREAK to Pair(0, 0)
    )

    for (badge in MILESTONES) {
        val (unl, tot) = categoryCounts[badge.category] ?: Pair(0, 0)
        val currentVal = valueFor(badge.category)
        if (currentVal >= badge.threshold) {
            unlocked.add(badge)
            categoryCounts[badge.category] = Pair(unl + 1, tot + 1)
        } else {
            locked.add(badge)
            categoryCounts[badge.category] = Pair(unl, tot + 1)
        }
    }

    // Determine currentTitle
    val chapterUnlocked = unlocked
        .filter { it.category == MilestoneCategory.CHAPTERS }
        .sortedByDescending { it.threshold }

    val (currentTitle, currentTitleTier) = when {
        chapterUnlocked.isNotEmpty() -> chapterUnlocked.first().title to chapterUnlocked.first().tier
        ch == 0 && lib > 0 -> "Library Explorer" to MilestoneTier.BRONZE
        else -> "Aspiring Reader" to MilestoneTier.BRONZE
    }

    // Determine nextMilestone (closest locked badge)
    val nextMilestone = if (locked.isNotEmpty()) {
        locked.map { badge ->
            val cur = valueFor(badge.category)
            val total = badge.threshold
            val pct = ((cur.toFloat() / total) * 100).toInt().coerceIn(0, 99)
            val rem = maxOf(0, total - cur)
            MilestoneProgress(badge, cur, total, pct, rem)
        }.minWithOrNull(
            compareByDescending<MilestoneProgress> { it.percent }
                .thenBy { it.remaining }
        )
    } else null

    return UserMilestoneSummary(
        unlocked = unlocked,
        locked = locked,
        totalCount = MILESTONES.size,
        unlockedCount = unlocked.size,
        currentTitle = currentTitle,
        currentTitleTier = currentTitleTier,
        nextMilestone = nextMilestone,
        categoryCounts = categoryCounts
    )
}
