package com.mangadl.android.ui.screens.migrate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.extensions.ExtensionMeta
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

private enum class MigrateStep { SELECTING, SEARCHING, CONFIRMING, MIGRATING }

@Composable
fun MigrateScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val db = MangaDlApp.instance.database
    val extensionManager = MangaDlApp.instance.extensionManager
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var step by remember { mutableStateOf(MigrateStep.SELECTING) }
    var library by remember { mutableStateOf<List<LibraryManga>>(emptyList()) }
    var selectedManga by remember { mutableStateOf<LibraryManga?>(null) }
    var extensions by remember { mutableStateOf<List<ExtensionMeta>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MangaSearchResult>>(emptyList()) }
    var selectedSource by remember { mutableStateOf<ExtensionMeta?>(null) }
    var targetResult by remember { mutableStateOf<MangaSearchResult?>(null) }
    var searching by remember { mutableStateOf(false) }
    var librarySearch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.libraryDao().getAll().collect { library = it }
    }
    LaunchedEffect(Unit) {
        extensions = extensionManager.listExtensions()
    }

    Scaffold(
        containerColor = MangaDlColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    when (step) {
                        MigrateStep.SELECTING -> onBack()
                        MigrateStep.SEARCHING -> { step = MigrateStep.SELECTING; selectedManga = null }
                        MigrateStep.CONFIRMING -> step = MigrateStep.SEARCHING
                        MigrateStep.MIGRATING -> {}
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
                }
                Text(
                    when (step) {
                        MigrateStep.SELECTING -> "MIGRATE"
                        MigrateStep.SEARCHING -> "FIND ON SOURCE"
                        MigrateStep.CONFIRMING -> "CONFIRM"
                        MigrateStep.MIGRATING -> "MIGRATING"
                    },
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MigrateStep.values().forEachIndexed { i, s ->
                    val active = step == s || step.ordinal > s.ordinal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) MangaDlColors.Primary else Color(0x33FFFFFF)),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (step) {
                MigrateStep.SELECTING -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Select manga to migrate",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                        OutlinedTextField(
                            value = librarySearch,
                            onValueChange = { librarySearch = it },
                            placeholder = { Text("Search library", color = MangaDlColors.TextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MangaDlColors.Primary,
                                unfocusedBorderColor = MangaDlColors.CardBorder,
                                focusedTextColor = MangaDlColors.TextPrimary,
                                unfocusedTextColor = MangaDlColors.TextPrimary,
                                cursorColor = MangaDlColors.Primary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MangaDlColors.TextSecondary) },
                        )
                        val filtered = library.filter { it.title.contains(librarySearch, ignoreCase = true) }
                        LazyColumn {
                            items(filtered.size) { i ->
                                val manga = filtered[i]
                                LibraryMangaRow(manga = manga, onClick = {
                                    selectedManga = manga
                                    searchQuery = manga.title
                                    step = MigrateStep.SEARCHING
                                })
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }

                MigrateStep.SEARCHING -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Source selector
                        selectedManga?.let { manga ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MangaDlColors.CardBg)
                                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("From: ${manga.provider}", color = MangaDlColors.TextSecondary, fontSize = 11.sp)
                                    Text(manga.title, color = MangaDlColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        if (selectedSource == null) {
                            Text(
                                "SELECT A SOURCE",
                                color = MangaDlColors.SectionRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                            LazyColumn {
                                items(extensions.size) { i ->
                                    val ext = extensions[i]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedSource = ext }
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MangaDlColors.CardBg),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(ext.name.take(1), color = MangaDlColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(ext.name, color = MangaDlColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(MangaDlColors.CardBorder))
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Source: ${selectedSource!!.name}", color = MangaDlColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                TextButton(onClick = { selectedSource = null; searchResults = emptyList() }) {
                                    Text("Change", color = MangaDlColors.Primary, fontSize = 12.sp)
                                }
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search manga", color = MangaDlColors.TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MangaDlColors.Primary,
                                        unfocusedBorderColor = MangaDlColors.CardBorder,
                                        focusedTextColor = MangaDlColors.TextPrimary,
                                        unfocusedTextColor = MangaDlColors.TextPrimary,
                                        cursorColor = MangaDlColors.Primary,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            scope.launch {
                                                searching = true
                                                try {
                                                    searchResults = extensionManager.search(selectedSource!!.id, searchQuery)
                                                } catch (e: Exception) {
                                                    searchResults = emptyList()
                                                } finally {
                                                    searching = false
                                                }
                                            }
                                        }
                                    }),
                                )
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            scope.launch {
                                                searching = true
                                                try {
                                                    searchResults = extensionManager.search(selectedSource!!.id, searchQuery)
                                                } catch (e: Exception) {
                                                    searchResults = emptyList()
                                                } finally {
                                                    searching = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MangaDlColors.Primary),
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                                }
                            }

                            if (searching) {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MangaDlColors.Primary)
                                }
                            } else {
                                LazyColumn {
                                    items(searchResults.size) { i ->
                                        val result = searchResults[i]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    targetResult = result
                                                    step = MigrateStep.CONFIRMING
                                                }
                                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 40.dp, height = 56.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MangaDlColors.CardBg),
                                            )
                                            Text(result.title, color = MangaDlColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 2)
                                        }
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(MangaDlColors.CardBorder))
                                    }
                                    item { Spacer(Modifier.height(24.dp)) }
                                }
                            }
                        }
                    }
                }

                MigrateStep.CONFIRMING -> {
                    val from = selectedManga
                    val to = targetResult
                    val toSource = selectedSource
                    if (from != null && to != null && toSource != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("Review migration", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MangaDlColors.CardBg)
                                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                LabelValue("Title", from.title)
                                LabelValue("From source", from.provider)
                                LabelValue("To source", toSource.name)
                                LabelValue("New title match", to.title)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MangaDlColors.Primary)
                                    .clickable {
                                        step = MigrateStep.MIGRATING
                                        scope.launch {
                                            try {
                                                val newId = "${toSource.id}:${to.id}"
                                                val updated = from.copy(
                                                    id = newId,
                                                    provider = toSource.id,
                                                    url = to.url,
                                                )
                                                db.libraryDao().delete(from.id)
                                                db.libraryDao().upsert(updated)
                                                snackbarHostState.showSnackbar("Migration complete")
                                                onComplete()
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Migration failed: ${e.message}")
                                                step = MigrateStep.CONFIRMING
                                            }
                                        }
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Confirm Migration", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(12.dp))
                                    .clickable { step = MigrateStep.SEARCHING }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Cancel", color = MangaDlColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                MigrateStep.MIGRATING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(color = MangaDlColors.Primary)
                            Text("Migrating...", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMangaRow(manga: LibraryManga, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MangaDlColors.CardBg),
        )
        Column(Modifier.weight(1f)) {
            Text(manga.title, color = MangaDlColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(manga.provider, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MangaDlColors.CardBorder))
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = MangaDlColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.width(96.dp))
        Text(value, color = MangaDlColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
