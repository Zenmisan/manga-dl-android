package com.mangadl.android.community

import com.mangadl.android.data.model.CommentItem
import com.mangadl.android.data.model.CommentsResponse
import com.mangadl.android.data.model.HunterRankPayload
import com.mangadl.android.data.model.LeaderboardEntry
import com.mangadl.android.data.model.PostCommentPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityAndLeaderboardTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testCommentSerialization_matchesBackendSchema() {
        val jsonPayload = """
            {
                "comments": [
                    {
                        "id": "c1",
                        "user_id": "u1",
                        "username": "SoloHunter",
                        "display_name": "Solo Hunter",
                        "provider": "asura",
                        "manga_id": "solo-leveling",
                        "chapter_id": "ch-100",
                        "parent_id": null,
                        "body": "Peak chapter!",
                        "likes": 42,
                        "liked": true,
                        "created_at": "2026-10-08T18:00:00Z",
                        "replies": [
                            {
                                "id": "r1",
                                "user_id": "u2",
                                "username": "MangaFan",
                                "display_name": "Manga Fan",
                                "provider": "asura",
                                "manga_id": "solo-leveling",
                                "chapter_id": "ch-100",
                                "parent_id": "c1",
                                "body": "Totally agree!",
                                "likes": 5,
                                "liked": false,
                                "created_at": "2026-10-08T18:05:00Z"
                            }
                        ]
                    }
                ],
                "total": 1,
                "offset": 0,
                "limit": 20
            }
        """.trimIndent()

        val resp = json.decodeFromString<CommentsResponse>(jsonPayload)
        assertEquals(1, resp.total)
        assertEquals(1, resp.comments.size)

        val top = resp.comments[0]
        assertEquals("c1", top.id)
        assertEquals("SoloHunter", top.username)
        assertEquals("Solo Hunter", top.displayName)
        assertEquals("Peak chapter!", top.body)
        assertEquals(42, top.likes)
        assertTrue(top.liked)
        assertEquals(1, top.replies.size)

        val reply = top.replies[0]
        assertEquals("r1", reply.id)
        assertEquals("c1", reply.parentId)
        assertEquals("Totally agree!", reply.body)
        assertFalse(reply.liked)
    }

    @Test
    fun testPostCommentPayload_serialization() {
        val payload = PostCommentPayload(
            provider = "reaper",
            mangaId = "omniscient-reader",
            chapterId = "ch-1",
            parentId = null,
            body = "Starting this today!"
        )
        val serialized = json.encodeToString(PostCommentPayload.serializer(), payload)
        assertTrue(serialized.contains("\"provider\":\"reaper\""))
        assertTrue(serialized.contains("\"manga_id\":\"omniscient-reader\""))
        assertTrue(serialized.contains("\"chapter_id\":\"ch-1\""))
        assertTrue(serialized.contains("\"body\":\"Starting this today!\""))
    }

    @Test
    fun testLeaderboardSerialization_matchesBackendSchema() {
        val leaderboardJson = """
            [
                {
                    "user_id": "u100",
                    "username": "ShadowKing",
                    "display_name": "Shadow King",
                    "bio": "Arise.",
                    "avatar_url": "",
                    "chapters_read": 3500,
                    "manga_count": 180,
                    "streak_days": 210,
                    "score": 50000,
                    "rank": 1,
                    "hunter_rank": {
                        "rank_code": "MONARCH",
                        "rank_name": "Shadow Monarch",
                        "tier": "mythic"
                    }
                },
                {
                    "user_id": "u101",
                    "username": "SwordMaster",
                    "display_name": "Sword Master",
                    "bio": "Top guild fighter",
                    "avatar_url": "",
                    "chapters_read": 2100,
                    "manga_count": 95,
                    "streak_days": 130,
                    "score": 29875,
                    "rank": 2,
                    "hunter_rank": {
                        "rank_code": "S",
                        "rank_name": "S-Rank Hunter",
                        "tier": "diamond"
                    }
                }
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<LeaderboardEntry>>(leaderboardJson)
        assertEquals(2, list.size)

        val first = list[0]
        assertEquals(1, first.rank)
        assertEquals("ShadowKing", first.username)
        assertEquals(50000, first.score)
        assertNotNull(first.hunterRank)
        assertEquals("MONARCH", first.hunterRank?.rankCode)
        assertEquals("mythic", first.hunterRank?.tier)

        val second = list[1]
        assertEquals(2, second.rank)
        assertEquals("S", second.hunterRank?.rankCode)
        assertEquals("diamond", second.hunterRank?.tier)
    }
}
