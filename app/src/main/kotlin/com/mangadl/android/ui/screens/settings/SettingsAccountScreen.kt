package com.mangadl.android.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSwitch
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.AccountSettingsViewModel
import com.mangadl.android.ui.viewmodels.AuthViewModel

@Composable
fun AccountSettingsScreen(onBack: () -> Unit, onSignOut: () -> Unit, onDeleteAccount: () -> Unit = {}) {
    val c = MdTheme.colors
    val vm: AccountSettingsViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()
    val exportStatus by vm.exportStatus.collectAsState()
    val importStatus by vm.importStatus.collectAsState()
    val deleteStatus by vm.deleteStatus.collectAsState()
    val user = authVm.currentUser
    val meta = user?.userMetadata
    val initialName = try {
        (meta?.get("username") as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: (meta?.get("full_name") as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: user?.email?.substringBefore('@')
            ?: ""
    } catch (_: Exception) { user?.email?.substringBefore('@') ?: "" }
    val initialBio = try {
        (meta?.get("bio") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
    } catch (_: Exception) { "" }
    val realEmail = user?.email ?: ""
    var name by rememberState(initialName)
    var bio by rememberState(initialBio)
    var public by rememberState(true)
    var confirmSignOut by rememberState(true)
    var confirmDelete by rememberState(false)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportLibrary(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importLibrary(it) }
    }

    val saveStatus by vm.saveStatus.collectAsState()
    Screen {
        BackHeader("Account", onBack, Modifier.padding(bottom = 0.dp))
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF3A1518)), contentAlignment = Alignment.Center) {
                    BodyText(initial, size = 24.sp, weight = FontWeight.ExtraBold)
                }
                Column {
                    BodyText(name.ifEmpty { "Guest" }, size = 18.sp, weight = FontWeight.ExtraBold)
                    BodyText(realEmail, size = 13.sp, color = c.fgSubtle)
                }
            }
            if (realEmail.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.success.copy(alpha = 0.10f))
                        .border(1.dp, c.success.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(MdIcons.CloudCheck, null, tint = c.successText, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        BodyText("Cloud sync on", weight = FontWeight.Bold)
                        BodyText("Signed in as $realEmail", size = 12.sp, color = c.fg.copy(alpha = 0.65f))
                    }
                }
            }
            MdTextField(name, { name = it }, label = "Display name", height = 48.dp)
            MdTextField(bio, { bio = it }, label = "Bio", height = 48.dp)
            Column {
                Divider()
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BodyText("Public profile", size = 15.sp, weight = FontWeight.SemiBold)
                        BodyText("Let other readers see your stats", size = 12.sp, color = c.fgSubtle)
                    }
                    MdSwitch(public, { public = it })
                }
            }
            if (saveStatus != null) BodyText(saveStatus!!, size = 12.sp, color = c.fgSubtle)
            MdButton("Save Changes", { vm.saveProfile(name, bio) }, Modifier.fillMaxWidth(), height = 50.dp, fontSize = 14.sp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.divider, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BodyText("Library data", weight = FontWeight.Bold)
                BodyText("Export or import your library as JSON.", size = 13.sp, color = c.fg.copy(alpha = 0.7f))
                if (exportStatus != null) BodyText(exportStatus!!, size = 12.sp, color = c.fgSubtle)
                if (importStatus != null) BodyText(importStatus!!, size = 12.sp, color = c.fgSubtle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MdButton(
                        "Export",
                        { exportLauncher.launch("manga-dl-library.json") },
                        Modifier.weight(1f),
                        tone = ButtonTone.Ghost,
                        height = 44.dp,
                        shape = RoundedCornerShape(12.dp),
                        fontSize = 13.sp,
                    )
                    MdButton(
                        "Import",
                        { importLauncher.launch(arrayOf("application/json")) },
                        Modifier.weight(1f),
                        height = 44.dp,
                        shape = RoundedCornerShape(12.dp),
                        fontSize = 13.sp,
                    )
                }
            }
            if (confirmSignOut) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.accentFaint)
                        .border(1.dp, c.errorBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BodyText("Sign out of this device?", weight = FontWeight.Bold)
                    BodyText("Downloads stay on the device. Sync stops until you sign in again.", size = 13.sp, color = c.fg.copy(alpha = 0.7f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdButton("Cancel", { confirmSignOut = false }, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 13.sp)
                        MdButton("Sign Out", { vm.signOut(onSignOut) }, Modifier.weight(1f), tone = ButtonTone.DangerFill, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 13.sp)
                    }
                }
            } else {
                MdButton("Sign Out", { confirmSignOut = true }, Modifier.fillMaxWidth(), tone = ButtonTone.Danger, height = 50.dp, fontSize = 14.sp)
            }
            // Delete account danger zone
            if (confirmDelete) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.errorBorder.copy(alpha = 0.08f))
                        .border(1.dp, c.errorBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BodyText("Permanently delete account?", weight = FontWeight.Bold, color = c.errorText)
                    BodyText(
                        "This removes your account and wipes all local data (library, history, downloads). Cannot be undone.",
                        size = 13.sp, color = c.fg.copy(alpha = 0.7f), lineHeight = 19.sp,
                    )
                    if (deleteStatus != null) BodyText(deleteStatus!!, size = 12.sp, color = c.fgSubtle)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdButton("Cancel", { confirmDelete = false }, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 13.sp)
                        MdButton(
                            "Delete Forever",
                            { vm.deleteAccount(onDeleteAccount) },
                            Modifier.weight(1f),
                            tone = ButtonTone.DangerFill,
                            height = 44.dp,
                            shape = RoundedCornerShape(12.dp),
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                MdButton("Delete Account", { confirmDelete = true }, Modifier.fillMaxWidth(), tone = ButtonTone.Ghost, height = 44.dp, fontSize = 13.sp)
            }
        }
    }
}
