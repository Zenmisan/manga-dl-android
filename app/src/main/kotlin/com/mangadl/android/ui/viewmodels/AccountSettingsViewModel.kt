package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.data.model.LibraryManga
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Request

class AccountSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = MangaDlApp.instance.database

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus

    fun saveProfile(name: String, bio: String) = viewModelScope.launch {
        _saveStatus.value = "Saving…"
        runCatching {
            SupabaseManager.client.auth.updateUser {
                data = buildJsonObject {
                    put("username", name.trim())
                    put("bio", bio.trim())
                }
            }
        }.onSuccess {
            _saveStatus.value = "Saved"
        }.onFailure { e ->
            _saveStatus.value = "Save failed: ${e.message}"
        }
    }

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus

    fun exportLibrary(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _exportStatus.value = null
        try {
            val items = db.libraryDao().getAll().first()
            val json = Json.encodeToString(items)
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.writer().use { it.write(json) }
            }
            _exportStatus.value = "Exported ${items.size} titles"
        } catch (e: Exception) {
            _exportStatus.value = "Export failed: ${e.message}"
        }
    }

    fun importLibrary(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _importStatus.value = null
        try {
            val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                ?: return@launch
            val items = Json.decodeFromString<List<LibraryManga>>(json)
            items.forEach { db.libraryDao().upsert(it) }
            _importStatus.value = "Imported ${items.size} titles"
        } catch (e: Exception) {
            _importStatus.value = "Import failed: ${e.message}"
        }
    }

    private val _deleteStatus = MutableStateFlow<String?>(null)
    val deleteStatus: StateFlow<String?> = _deleteStatus

    fun deleteAccount(onDone: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        _deleteStatus.value = "Deleting…"
        runCatching {
            // Call Supabase self-delete endpoint with the user's own JWT
            val token = SupabaseManager.client.auth.currentAccessTokenOrNull()
            if (token != null) {
                val req = Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                    .header("Authorization", "Bearer $token")
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .delete()
                    .build()
                MangaDlApp.instance.httpClient.newCall(req).execute().close()
            }
        }
        // Wipe all local data regardless of server response
        runCatching { db.libraryDao().deleteAll() }
        runCatching { db.progressDao().deleteAll() }
        runCatching { db.downloadDao().deleteAll() }
        runCatching { SupabaseManager.client.auth.signOut() }
        runCatching { MangaDlApp.instance.googleAuthHelper.signOut() }
        withContext(Dispatchers.Main) { onDone() }
    }
}
