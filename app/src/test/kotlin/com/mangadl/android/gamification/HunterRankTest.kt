package com.mangadl.android.gamification

import com.mangadl.android.data.gamification.*
import org.junit.Assert.*
import org.junit.Test

class HunterRankTest {

    @Test
    fun testReaderScoreFormula() {
        // formula: (ch * 10) + (streak * 50) + (lib * 25)
        assertEquals(0, calculateReaderScore(0, 0, 0))
        assertEquals(10, calculateReaderScore(1, 0, 0))
        assertEquals(50, calculateReaderScore(0, 1, 0))
        assertEquals(25, calculateReaderScore(0, 0, 1))

        // Complex combination: 120 ch, 7 streak, 35 library
        // 120 * 10 = 1200
        // 7 * 50 = 350
        // 35 * 25 = 875
        // Total = 2425
        assertEquals(2425, calculateReaderScore(120, 7, 35))

        // Negative values are clamped to 0
        assertEquals(0, calculateReaderScore(-5, -1, -10))
    }

    @Test
    fun testHunterRankTiers() {
        // E-Rank: 0 .. 499
        val rankE = getHunterRank(0)
        assertEquals("E", rankE.code)
        assertEquals("Awakened Novice", rankE.tag)

        val rankEUpper = getHunterRank(499)
        assertEquals("E", rankEUpper.code)

        // D-Rank: 500 .. 1499
        val rankD = getHunterRank(500)
        assertEquals("D", rankD.code)
        assertEquals("Dungeon Scavenger", rankD.tag)

        // C-Rank: 1500 .. 3999
        val rankC = getHunterRank(1500)
        assertEquals("C", rankC.code)
        assertEquals("Raid Ready", rankC.tag)

        // B-Rank: 4000 .. 7999
        val rankB = getHunterRank(4000)
        assertEquals("B", rankB.code)
        assertEquals("Veteran", rankB.tag)

        // A-Rank: 8000 .. 14999
        val rankA = getHunterRank(8000)
        assertEquals("A", rankA.code)
        assertEquals("High Guild", rankA.tag)

        // S-Rank: >= 15000
        val rankS = getHunterRank(15000)
        assertEquals("S", rankS.code)
        assertEquals("Apex Elite", rankS.tag)

        val rankSAbove = getHunterRank(99999)
        assertEquals("S", rankSAbove.code)

        // MONARCH: isWorldFirst and score > 0
        val monarch = getHunterRank(5000, isWorldFirst = true)
        assertEquals("MONARCH", monarch.code)
        assertEquals("#1 Sovereign", monarch.tag)
    }

    @Test
    fun testRankProgress() {
        val rankD = getHunterRank(500) // min 500, next 1500 (span 1000)
        assertEquals(0f, calculateRankProgress(500, rankD), 0.001f)
        assertEquals(0.5f, calculateRankProgress(1000, rankD), 0.001f)
        assertEquals(0.999f, calculateRankProgress(1499, rankD), 0.01f)

        val rankS = getHunterRank(15000)
        assertEquals(1f, calculateRankProgress(15000, rankS), 0.001f)
    }

    @Test
    fun testMilestoneRegistryCountAndCategories() {
        assertEquals(31, MILESTONES.size)
        val chapters = MILESTONES.filter { it.category == MilestoneCategory.CHAPTERS }
        val library = MILESTONES.filter { it.category == MilestoneCategory.LIBRARY }
        val streak = MILESTONES.filter { it.category == MilestoneCategory.STREAK }

        assertEquals(15, chapters.size)
        assertEquals(8, library.size)
        assertEquals(8, streak.size)

        // Ensure thresholds are strictly ascending in each category
        assertTrue(chapters.map { it.threshold }.let { it == it.sorted() })
        assertTrue(library.map { it.threshold }.let { it == it.sorted() })
        assertTrue(streak.map { it.threshold }.let { it == it.sorted() })
    }

    @Test
    fun testUserMilestonesEvaluation() {
        // Zero progress
        val zero = getUserMilestones(0, 0, 0)
        assertEquals(0, zero.unlockedCount)
        assertEquals(31, zero.locked.size)
        assertEquals("Aspiring Reader", zero.currentTitle)
        assertNotNull(zero.nextMilestone)
        assertEquals(1, zero.nextMilestone!!.total) // first milestone needs 1

        // 1 chapter read
        val one = getUserMilestones(1, 0, 0)
        assertEquals(1, one.unlockedCount)
        assertEquals("First Page", one.unlocked.first().title)
        assertEquals("First Page", one.currentTitle)

        // 100 chapters read, 3 streak, 15 library
        val mid = getUserMilestones(100, 3, 15)
        // Chapters: 1, 10, 25, 50, 100 -> 5 unlocked
        // Library: 1, 5, 10 -> 3 unlocked
        // Streak: 1, 3 -> 2 unlocked
        // Total unlocked = 10
        assertEquals(10, mid.unlockedCount)
        assertEquals("Volume Devourer", mid.currentTitle)
        assertEquals(Pair(5, 15), mid.categoryCounts[MilestoneCategory.CHAPTERS])
        assertEquals(Pair(3, 8), mid.categoryCounts[MilestoneCategory.LIBRARY])
        assertEquals(Pair(2, 8), mid.categoryCounts[MilestoneCategory.STREAK])

        // Closest next milestone should be chosen
        assertNotNull(mid.nextMilestone)
    }
}
