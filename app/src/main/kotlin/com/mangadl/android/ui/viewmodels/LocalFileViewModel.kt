package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipInputStream

class LocalFileViewModel(app: Application) : AndroidViewModel(app) {

    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "avif")

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _pages = MutableStateFlow<List<String>>(emptyList())
    val pages: StateFlow<List<String>> = _pages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun processUri(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _loading.value = true
        _error.value = null
        _pages.value = emptyList()
        try {
            val cacheDir = File(getApplication<Application>().cacheDir, "cbz_extract")
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            val entries = mutableListOf<Pair<String, ByteArray>>()
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val ext = entry.name.substringAfterLast('.', "").lowercase()
                            if (ext in IMAGE_EXTS) {
                                entries.add(entry.name to zip.readBytes())
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            val sorted = entries.sortedWith(
                compareBy({ it.first.substringBeforeLast('/') }, { it.first.substringAfterLast('/') })
            )

            val paths = sorted.mapIndexed { i, (name, data) ->
                val filename = "${i.toString().padStart(5, '0')}_${name.substringAfterLast('/')}"
                val file = File(cacheDir, filename)
                file.writeBytes(data)
                file.absolutePath
            }

            _pages.value = paths
            _title.value = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Local File"
        } catch (e: Exception) {
            Log.e("LocalFileVM", "Failed to parse archive: $uri", e)
            _error.value = "Could not open file: ${e.message}"
        } finally {
            _loading.value = false
        }
    }
}
