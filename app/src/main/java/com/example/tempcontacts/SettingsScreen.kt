package com.example.tempcontacts

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    // --- Permissions State ---
    var isOverlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isCallerIdEnabled by remember { mutableStateOf(false) }
    var isRoleAvailable by remember { mutableStateOf(false) }

    // Dialog States
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showBetaInfoDialog by remember { mutableStateOf(false) }

    // --- Lifecycle Observer to refresh permissions when returning to app ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayAllowed = Settings.canDrawOverlays(context)
                // Refresh Role status
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    isCallerIdEnabled = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- JSON Export Logic ---
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(Gson().toJson(contacts).toByteArray())
                    Toast.makeText(context, "Contacts exported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- JSON Import Logic ---
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val reader = BufferedReader(InputStreamReader(input))
                    val list: List<Contact> = Gson().fromJson(reader, object : TypeToken<List<Contact>>() {}.type)
                    list.forEach { c -> viewModel.insert(c) }
                    Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Role Manager Setup ---
    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    } else null

    val requestRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isCallerIdEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
    }

    LaunchedEffect(roleManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
            isCallerIdEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
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
            Spacer(modifier = Modifier.height(8.dp))

            // --- Theme Selection ---
            Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ThemeCard(Modifier.weight(1f), Icons.Outlined.LightMode, "Light", theme == "Light") { scope.launch { settingsDataStore.saveTheme("Light") } }
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(Modifier.weight(1f), Icons.Outlined.DarkMode, "Dark", theme == "Dark") { scope.launch { settingsDataStore.saveTheme("Dark") } }
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(Modifier.weight(1f), Icons.Outlined.SettingsBrightness, "System", theme == "System") { scope.launch { settingsDataStore.saveTheme("System") } }
                    }
                }
            }

            // --- Caller ID Card ---
            if (isRoleAvailable) {
                Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Caller ID Features", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        PermissionSwitchRow(
                            title = "Identification Service",
                            subtitle = if (isCallerIdEnabled) "Service is Active" else "Required to recognize numbers",
                            isChecked = isCallerIdEnabled,
                            onCheckedChange = {
                                if (roleManager != null) {
                                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    requestRoleLauncher.launch(intent)
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        PermissionSwitchRow(
                            title = "Display Over Apps",
                            subtitle = if (isOverlayAllowed) "Permission Granted" else "Required for the floating box",
                            isChecked = isOverlayAllowed,
                            onCheckedChange = {
                                if (!isOverlayAllowed) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                    }
                                } else {
                                    // Optionally lead them to settings to disable it
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }

            // --- Data Management ---
            Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { exportLauncher.launch("contacts.json") }) {
                            Icon(Icons.Outlined.FileUpload, null, Modifier.padding(end = 8.dp))
                            Text("Export")
                        }
                        Button(onClick = { importLauncher.launch("application/json") }) {
                            Icon(Icons.Outlined.FileDownload, null, Modifier.padding(end = 8.dp))
                            Text("Import")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    TextButton(onClick = { showDeleteAllDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 8.dp))
                        Text("Delete All Contacts", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // --- Support Card ---
            Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsRow(Icons.Outlined.TipsAndUpdates, "Beta Tester Guide") { showBetaInfoDialog = true }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRow(Icons.Outlined.BugReport, "Report a bug") { /* Bug logic */ }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRow(Icons.Outlined.Info, "About", onAboutClick)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- Dialogs ---
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Contacts?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { Button(onClick = { viewModel.deleteAll(); showDeleteAllDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBetaInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBetaInfoDialog = false },
            title = { Text("Beta Missions") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MissionItem("Caller ID", "Verify the floating overlay appears on calls.")
                    MissionItem("Auto-Delete", "Check if contacts vanish after the timer ends.")
                }
            },
            confirmButton = { Button(onClick = { showBetaInfoDialog = false }) { Text("Got it") } }
        )
    }
}

// --- Helper Components (Placed outside main function) ---

@Composable
fun PermissionSwitchRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    imageVector = if (isChecked) Icons.Outlined.Check else Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MissionItem(title: String, description: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("• $title", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ThemeCard(modifier: Modifier, icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(vertical = 20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}