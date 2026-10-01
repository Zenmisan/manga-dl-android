package com.mangadl.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress

@Database(
    entities = [LibraryManga::class, ReadingProgress::class, DownloadEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun progressDao(): ProgressDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manga_dl.db"
                ).build().also { instance = it }
            }
    }
}
