package com.example.tempcontacts

import android.app.role.RoleManager
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Dialog States
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showBetaInfoDialog by remember { mutableStateOf(false) }

    // --- JSON Export Logic ---
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

    // --- JSON Import Logic ---
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

    // --- Caller ID Role Logic ---
    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    } else {
        null
    }

    var isCallerIdEnabled by remember { mutableStateOf(false) }
    var isRoleAvailable by remember { mutableStateOf(false) }

    val requestRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isCallerIdEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
    }

    LaunchedEffect(roleManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
            if (isRoleAvailable) {
                isCallerIdEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
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

            // --- Theme Selection ---
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

            // --- Data Management ---
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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

            // --- Caller ID Card ---
            // --- Caller ID Card (Theme Color Fix) ---
            if (isRoleAvailable) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Caller ID",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface // Black in Light / White in Dark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Identify temporary contacts on incoming calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Muted theme color
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (roleManager != null) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                        requestRoleLauncher.launch(intent)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isCallerIdEnabled) "Service is Active" else "Service is Disabled",
                                style = MaterialTheme.typography.bodyLarge,
                                // FORCE THEME COLORS:
                                // No more default blue. We use onSurface for both,
                                // or primary only if you want the "Active" state to pop.
                                color = if (isCallerIdEnabled)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            // Material 3 Switch with custom Tick/X icons
                            Switch(
                                checked = isCallerIdEnabled,
                                onCheckedChange = {
                                    if (roleManager != null) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                        requestRoleLauncher.launch(intent)
                                    }
                                },
                                thumbContent = {
                                    val icon = if (isCallerIdEnabled) Icons.Outlined.Check else Icons.Outlined.Close
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        // Tinting the icon inside the switch thumb
                                        tint = if (isCallerIdEnabled)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // --- Contact & Support Card ---
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact & Support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // BETA TESTER GUIDE BUTTON (Color matched to other sub-options)
                    TextButton(
                        onClick = { showBetaInfoDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.TipsAndUpdates, contentDescription = "Beta", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Beta Tester Guide", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // BUG REPORT BUTTON
                    TextButton(
                        onClick = {
                            val appVersion = try {
                                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                "${pInfo.versionName} (${pInfo.versionCode})"
                            } catch (e: Exception) { "N/A" }

                            val deviceInfo = "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                    "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                    "App Version: $appVersion"

                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("yashgarg2801@outlook.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Bug Report for Burner Book")
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
                        Icon(Icons.Outlined.BugReport, contentDescription = "Report", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Report a bug", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ABOUT BUTTON
                    TextButton(
                        onClick = onAboutClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "About", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("About", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- DIALOGS ---

    // Beta Info Dialog
    if (showBetaInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBetaInfoDialog = false },
            icon = { Icon(Icons.Outlined.TipsAndUpdates, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Beta Tester Missions", textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Help us polish the app! Please verify these functions:", style = MaterialTheme.typography.bodyMedium)
                    MissionItem("Timer & Auto-Delete", "Set a 1-min timer. Does the contact delete when app is closed?")
                    MissionItem("Data Import/Export", "Export your list, delete a contact, then import. Does it return?")
                    MissionItem("Delete All", "Does the 'Delete All Contacts' button clear the list successfully?")
                    MissionItem("Caller ID", "Enable Caller ID. Receive a call from a temp contact—do you see the custom notification?")
                    MissionItem("About Page", "Check if the Privacy Policy and License links in 'About' open correctly.")
                    MissionItem("Visuals", "Does the app look good in both Light and Dark modes?")
                    MissionItem("New Idea?", "Suggest one feature you think would be a great addition!")
                }
            },
            confirmButton = {
                Button(onClick = { showBetaInfoDialog = false }) { Text("Got it!") }
            }
        )
    }

    // Delete All Confirmation Dialog
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
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MissionItem(title: String, description: String) {
    Column {
        Text("• $title", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
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