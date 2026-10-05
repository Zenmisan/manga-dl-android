package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AccountSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = MangaDlApp.instance.database

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
}
