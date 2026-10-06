package com.mangadl.android.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Runs [BackupManager.exportBackup] on a periodic schedule when "Automatic backups" is enabled. */
class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            BackupManager().exportBackup(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Automatic backup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutoBackupWorker"
        const val WORK_NAME = "auto_backup_weekly"
    }
}
