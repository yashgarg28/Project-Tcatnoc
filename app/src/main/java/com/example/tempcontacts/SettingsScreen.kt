package com.example.tempcontacts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ContactViewModel,
    settingsDataStore: SettingsDataStore, 
    onBackClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val theme by settingsDataStore.themeFlow.collectAsState(initial = "System")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val json = Gson().toJson(contacts)
                    outputStream.write(json.toByteArray())
                    Toast.makeText(context, "Contacts exported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export contacts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val type = object : TypeToken<List<Contact>>() {}.type
                    val importedContacts: List<Contact> = Gson().fromJson(reader, type)
                    importedContacts.forEach { contact -> viewModel.insert(contact) }
                    Toast.makeText(context, "Contacts imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import contacts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
    } else {
        null
    }

    var isCallerIdEnabled by remember { mutableStateOf(false) }
    var isRoleAvailable by remember { mutableStateOf(false) }

    val requestRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isCallerIdEnabled = roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
        }
    }

    LaunchedEffect(roleManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isRoleAvailable = roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)
            if (isRoleAvailable) {
                isCallerIdEnabled = roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ThemeCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.LightMode,
                            label = "Light",
                            isSelected = theme == "Light",
                            onClick = { scope.launch { settingsDataStore.saveTheme("Light") } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.DarkMode,
                            label = "Dark",
                            isSelected = theme == "Dark",
                            onClick = { scope.launch { settingsDataStore.saveTheme("Dark") } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.SettingsBrightness,
                            label = "System",
                            isSelected = theme == "System",
                            onClick = { scope.launch { settingsDataStore.saveTheme("System") } }
                        )
                    }
                }
            }

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Export or import your contacts as a .json file.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { exportLauncher.launch("contacts.json") }) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = "Export", modifier = Modifier.padding(end = 8.dp))
                            Text("Export")
                        }
                        Button(onClick = { importLauncher.launch("application/json") }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = "Import", modifier = Modifier.padding(end = 8.dp))
                            Text("Import")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    TextButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         Icon(Icons.Outlined.Delete, contentDescription = "Delete All", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 8.dp))
                         Text("Delete All Contacts", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (isRoleAvailable) {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Caller ID", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isCallerIdEnabled) "Enabled" else "Disabled")
                            Button(onClick = {
                                if (roleManager != null) {
                                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                                    requestRoleLauncher.launch(intent)
                                }
                            }) {
                                Text(if (isCallerIdEnabled) "Change Default" else "Set as Default")
                            }
                        }
                    }
                }
            }

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact & Support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            val appVersion = try {
                                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                "${pInfo.versionName} (${pInfo.versionCode})"
                            } catch (e: Exception) {
                                "N/A"
                            }

                            val deviceInfo = "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                             "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                             "App Version: $appVersion"

                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("yashgarg2801@outlook.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Bug Report for BrnBook")
                                putExtra(Intent.EXTRA_TEXT, "Please describe the bug:\n\n\n---\n$deviceInfo")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "Report a bug", modifier = Modifier.padding(end = 8.dp))
                        Text("Report a bug")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = onAboutClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "About", modifier = Modifier.padding(end = 8.dp))
                        Text("About")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Contacts?") },
            text = { Text("Are you sure you want to delete all contacts? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAll()
                        Toast.makeText(context, "All contacts deleted", Toast.LENGTH_SHORT).show()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}