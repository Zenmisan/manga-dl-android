package com.mangadl.android.data.db

import android.content.Context
import androidx.room.Room
import com.mangadl.android.data.model.CategoryEntity
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.LibraryCategoryEntity
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.NewChapterEntry
import com.mangadl.android.data.model.ReadingProgress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RoomDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var libraryDao: LibraryDao
    private lateinit var progressDao: ProgressDao
    private lateinit var downloadDao: DownloadDao
    private lateinit var updatesDao: UpdatesDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        libraryDao = db.libraryDao()
        progressDao = db.progressDao()
        downloadDao = db.downloadDao()
        updatesDao = db.updatesDao()
        categoryDao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── LibraryDao Tests ──────────────────────────────────────────────────────

    @Test
    fun testLibraryDao_upsertAndGetById() = runTest {
        val manga = LibraryManga(
            id = "one-piece",
            title = "One Piece",
            coverUrl = "https://example.com/op.jpg",
            provider = "mangadex",
            url = "https://mangadex.org/title/op",
            addedAt = 1000L,
            totalChapters = 1100,
            readCount = 1000
        )

        libraryDao.upsert(manga)
        val loaded = libraryDao.getById("one-piece")

        assertNotNull(loaded)
        assertEquals("One Piece", loaded!!.title)
        assertEquals("https://example.com/op.jpg", loaded.coverUrl)
        assertEquals(1100, loaded.totalChapters)
        assertEquals(1000, loaded.readCount)
    }

    @Test
    fun testLibraryDao_isInLibraryFlow() = runTest {
        val inLibraryInitial = libraryDao.isInLibrary("solo-leveling").first()
        assertFalse(inLibraryInitial)

        val manga = LibraryManga(
            id = "solo-leveling",
            title = "Solo Leveling",
            coverUrl = "https://example.com/sl.jpg",
            provider = "asura",
            url = "https://asura.gg/solo-leveling"
        )
        libraryDao.upsert(manga)

        val inLibraryAfter = libraryDao.isInLibrary("solo-leveling").first()
        assertTrue(inLibraryAfter)
    }

    @Test
    fun testLibraryDao_updateFields() = runTest {
        val manga = LibraryManga(
            id = "m1",
            title = "Title 1",
            coverUrl = "",
            provider = "p1",
            url = ""
        )
        libraryDao.upsert(manga)

        libraryDao.updateLastRead("m1", "ch-42", 5000L)
        libraryDao.updateReadCount("m1", 42)
        libraryDao.updateTotalChapters("m1", 100)

        val updated = libraryDao.getById("m1")!!
        assertEquals("ch-42", updated.lastReadChapterId)
        assertEquals(5000L, updated.lastReadAt)
        assertEquals(42, updated.readCount)
        assertEquals(100, updated.totalChapters)
    }

    @Test
    fun testLibraryDao_delete() = runTest {
        val manga = LibraryManga(
            id = "to-delete",
            title = "To Delete",
            coverUrl = "",
            provider = "p",
            url = ""
        )
        libraryDao.upsert(manga)
        assertNotNull(libraryDao.getById("to-delete"))

        libraryDao.delete("to-delete")
        assertNull(libraryDao.getById("to-delete"))
    }

    // ── ProgressDao Tests ─────────────────────────────────────────────────────

    @Test
    fun testProgressDao_upsertAndGet() = runTest {
        val progress = ReadingProgress(
            mangaId = "manga-1",
            chapterId = "ch-1",
            provider = "asura",
            page = 15,
            totalPages = 30,
            readAt = 2000L,
            completed = true
        )

        progressDao.upsert(progress)
        val loaded = progressDao.get("manga-1", "ch-1")

        assertNotNull(loaded)
        assertEquals(15, loaded!!.page)
        assertEquals(30, loaded.totalPages)
        assertTrue(loaded.completed)
    }

    @Test
    fun testProgressDao_countCompleted() = runTest {
        progressDao.upsert(ReadingProgress("m1", "c1", "p", 10, 10, completed = true))
        progressDao.upsert(ReadingProgress("m1", "c2", "p", 10, 10, completed = true))
        progressDao.upsert(ReadingProgress("m1", "c3", "p", 5, 10, completed = false))
        progressDao.upsert(ReadingProgress("m2", "c1", "p", 10, 10, completed = true))

        val completedM1 = progressDao.countCompleted("m1")
        assertEquals(2, completedM1)

        val completedM2 = progressDao.countCompleted("m2")
        assertEquals(1, completedM2)
    }

    @Test
    fun testProgressDao_getForMangaOrder() = runTest {
        progressDao.upsert(ReadingProgress("m1", "c1", "p", readAt = 1000L))
        progressDao.upsert(ReadingProgress("m1", "c2", "p", readAt = 3000L))
        progressDao.upsert(ReadingProgress("m1", "c3", "p", readAt = 2000L))

        val list = progressDao.getForManga("m1").first()
        assertEquals(3, list.size)
        assertEquals("c2", list[0].chapterId)
        assertEquals("c3", list[1].chapterId)
        assertEquals("c1", list[2].chapterId)
    }

    // ── DownloadDao Tests ─────────────────────────────────────────────────────

    @Test
    fun testDownloadDao_queueAndStatusUpdates() = runTest {
        val dl1 = DownloadEntry(
            id = "dl-1",
            mangaId = "m1",
            mangaTitle = "Manga One",
            chapterId = "ch-1",
            chapterTitle = "Chapter 1",
            provider = "asura",
            status = "queued",
            addedAt = 1000L
        )
        val dl2 = DownloadEntry(
            id = "dl-2",
            mangaId = "m1",
            mangaTitle = "Manga One",
            chapterId = "ch-2",
            chapterTitle = "Chapter 2",
            provider = "asura",
            status = "queued",
            addedAt = 2000L
        )

        downloadDao.upsert(dl1)
        downloadDao.upsert(dl2)

        val pending = downloadDao.getPending()
        assertEquals(2, pending.size)
        assertEquals("dl-1", pending[0].id)
        assertEquals("dl-2", pending[1].id)

        // Progress update
        downloadDao.updateProgress("dl-1", "downloading", 50)
        val updated = downloadDao.getById("dl-1")!!
        assertEquals("downloading", updated.status)
        assertEquals(50, updated.progress)

        // Completion
        downloadDao.markCompleted("dl-1", 3000L, "/path/to/ch1.cbz")
        val completed = downloadDao.getById("dl-1")!!
        assertEquals("completed", completed.status)
        assertEquals(3000L, completed.completedAt)
        assertEquals("/path/to/ch1.cbz", completed.filePath)

        // Completed chapter query
        val downloadedChapterIds = downloadDao.getDownloadedChapterIds("m1").first()
        assertEquals(listOf("ch-1"), downloadedChapterIds)
    }

    // ── UpdatesDao Tests ──────────────────────────────────────────────────────

    @Test
    fun testUpdatesDao_batchUpsertAndClear() = runTest {
        val updates = listOf(
            NewChapterEntry("m1:c1", "m1", "Manga 1", "", "p", "c1", "Ch 1", 1f, detectedAt = 100L),
            NewChapterEntry("m1:c2", "m1", "Manga 1", "", "p", "c2", "Ch 2", 2f, detectedAt = 200L),
            NewChapterEntry("m2:c1", "m2", "Manga 2", "", "p", "c1", "Ch 1", 1f, detectedAt = 300L)
        )

        updatesDao.upsert(updates)
        val all = updatesDao.getAll().first()
        assertEquals(3, all.size)
        // Ordered by detectedAt DESC
        assertEquals("m2:c1", all[0].id)
        assertEquals("m1:c2", all[1].id)
        assertEquals("m1:c1", all[2].id)

        // Clear for m1
        updatesDao.clearForManga("m1")
        val remaining = updatesDao.getAll().first()
        assertEquals(1, remaining.size)
        assertEquals("m2:c1", remaining[0].id)
    }

    // ── CategoryDao Tests ─────────────────────────────────────────────────────

    @Test
    fun testCategoryDao_crudAndAssociations() = runTest {
        val cat1 = CategoryEntity(id = "favorites", name = "Favorites", sortOrder = 1)
        val cat2 = CategoryEntity(id = "reading", name = "Currently Reading", sortOrder = 2)
        categoryDao.upsert(cat1)
        categoryDao.upsert(cat2)

        val allCategories = categoryDao.getAll().first()
        assertEquals(2, allCategories.size)
        assertEquals("favorites", allCategories[0].id)
        assertEquals("reading", allCategories[1].id)

        // Assign manga m1 to both categories
        categoryDao.setMangaCategories("m1", listOf("favorites", "reading"))
        val m1Cats = categoryDao.getCategoriesForManga("m1").first()
        assertEquals(2, m1Cats.size)
        assertTrue(m1Cats.contains("favorites"))
        assertTrue(m1Cats.contains("reading"))

        // Update manga m1 to only favorites
        categoryDao.setMangaCategories("m1", listOf("favorites"))
        val m1CatsUpdated = categoryDao.getCategoriesForManga("m1").first()
        assertEquals(listOf("favorites"), m1CatsUpdated)

        // Delete category "favorites" and verify cascade association deletion
        categoryDao.deleteCategory("favorites")
        val remainingCats = categoryDao.getAll().first()
        assertEquals(1, remainingCats.size)
        assertEquals("reading", remainingCats[0].id)

        val m1CatsAfterDelete = categoryDao.getCategoriesForManga("m1").first()
        assertTrue(m1CatsAfterDelete.isEmpty())
    }
}
