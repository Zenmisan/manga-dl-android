package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel

class NavStateHolder : ViewModel() {
    var mangaId: String = ""
    var sourceId: String = ""
    var mangaTitle: String = ""
    var chapterId: String = ""
    var chapterLabel: String = ""
    var localPages: List<String> = emptyList()
    var isLocalRead: Boolean = false
}
