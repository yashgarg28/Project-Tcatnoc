package com.example.tempcontacts

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    viewModel: ContactViewModel,
    settingsDataStore: SettingsDataStore,
    contactId: Int,
    onContactUpdated: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val contact = contacts.find { it.id == contactId }

    // --- State Management ---
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(countryList.find { it.name == "India" } ?: countryList[0]) }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }
    val lastCountryCode by settingsDataStore
        .lastCountryCodeFlow
        .collectAsState(initial = null)
    var isPhoneNumberValid by remember { mutableStateOf(false) }

    var selectedDurationMillis by remember { mutableStateOf<Long?>(null) }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var customDurationLabel by remember { mutableStateOf("Custom") }

    var showBottomSheet by remember { mutableStateOf(false) }

    // New Permission Popup States
    var showPermissionPopup by remember { mutableStateOf(false) }
    var missingRole by remember { mutableStateOf(false) }
    var missingOverlay by remember { mutableStateOf(false) }


    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var countryCodeExpanded by remember { mutableStateOf(false) }

    // --- Services Setup (Vibrator & RoleManager) ---
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    } else null

    val requestRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // When they return from Role Dialog, check if we can dismiss the popup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            missingRole = !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            // If they fixed everything, close the popup and finish
            if (!missingRole && !missingOverlay) {
                showPermissionPopup = false
                scope.launch { settingsDataStore.saveFirstSetupCompleted() }
                onContactUpdated()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missingOverlay = !Settings.canDrawOverlays(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(contact) {
        contact?.let {
            name = it.name
            val parts = it.phone.split(" ")
            if (parts.size == 2) {
                country = countryList.find { c -> c.code == parts[0] } ?: countryList[0]
                phone = parts[1]
            } else {
                phone = it.phone
            }
        }
    }

    LaunchedEffect(lastCountryCode, contact) {
        // Only apply saved country when adding a NEW contact
        if (contact == null && lastCountryCode != null) {
            countryList.firstOrNull { it.code == lastCountryCode }?.let {
                country = it
            }
        }
    }


    LaunchedEffect(phone, country) {
        isPhoneNumberValid = phone.length == country.phoneLength
    }

    // Function to handle the success feedback (Vibration + Toast)
    val triggerSuccessFeedback = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
        Toast.makeText(context, "Contact Saved Successfully", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (contactId == 0) "Add Contact" else "Edit Contact") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = countryCodeExpanded,
                    onExpandedChange = { countryCodeExpanded = !countryCodeExpanded },
                    modifier = Modifier.weight(0.4f)
                ) {
                    OutlinedTextField(
                        value = "${country.flagEmoji} ${country.code}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryCodeExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = countryCodeExpanded,
                        onDismissRequest = { countryCodeExpanded = false }
                    ) {
                        countryList.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.flagEmoji} ${c.name} (${c.code})") },
                                onClick = {
                                    country = c
                                    countryCodeExpanded = false

                                    // ✅ Save last selected country
                                    scope.launch {
                                        settingsDataStore.saveLastCountryCode(c.code)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        phone = newValue
                            .filter { it.isDigit() }
                            .take(country.phoneLength)
                    },
                    label = { Text("Phone") },
                    isError = !isPhoneNumberValid,
                    supportingText = { if (!isPhoneNumberValid) Text("Must be ${country.phoneLength} digits") else null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(0.6f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g. Delivery for the couch") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp), // Make it taller for notes
                maxLines = 3,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Auto-Delete Duration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedChip == "no_time",
                        onClick = {
                            selectedChip = "no_time"
                            selectedDurationMillis = null
                        },
                        label = { Text("Don't delete", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedChip == "24h",
                        onClick = {
                            selectedChip = "24h"
                            selectedDurationMillis = TimeUnit.HOURS.toMillis(24)
                        },
                        label = { Text("24 Hours", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedChip == "7d",
                        onClick = {
                            selectedChip = "7d"
                            selectedDurationMillis = TimeUnit.DAYS.toMillis(7)
                        },
                        label = { Text("7 Days", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedChip == "custom",
                        onClick = {
                            selectedChip = "custom"
                            showBottomSheet = true
                        },
                        label = { Text(customDurationLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && isPhoneNumberValid) {
                        val fullPhoneNumber = "${country.code} $phone"
                        val deletionTimestamp = selectedDurationMillis?.let { System.currentTimeMillis() + it }

                        // ✅ Include 'notes' in both the copy (update) and the constructor (new)
                        val updatedContact = contact?.copy(
                            name = name,
                            phone = fullPhoneNumber,
                            notes = notes, // 👈 Added here
                            deletionTimestamp = deletionTimestamp
                        ) ?: Contact(
                            name = name,
                            phone = fullPhoneNumber,
                            notes = notes, // 👈 Added here
                            deletionTimestamp = deletionTimestamp
                        )

                        if (contactId == 0) {
                            // Saving New Contact
                            viewModel.insert(updatedContact)
                            triggerSuccessFeedback()

                            scope.launch {
                                val hasDoneSetup = settingsDataStore.hasCompletedFirstSetupFlow.first()

                                val isRoleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    (context.getSystemService(Context.ROLE_SERVICE) as RoleManager)
                                        .isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                                } else true

                                val isOverlayAllowed = Settings.canDrawOverlays(context)

                                if (!hasDoneSetup && (!isRoleHeld || !isOverlayAllowed)) {
                                    missingRole = !isRoleHeld
                                    missingOverlay = !isOverlayAllowed
                                    showPermissionPopup = true
                                } else {
                                    onContactUpdated()
                                }
                            }
                        } else {
                            // Updating Existing Contact
                            viewModel.update(updatedContact)
                            triggerSuccessFeedback()
                            onContactUpdated()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = name.isNotBlank() && isPhoneNumberValid
            ) {
                Text(if (contactId == 0) "Add Contact" else "Save Changes")
            }
        }
    }

    // --- Smart Permission Popup ---

    val isEverythingEnabled = !missingRole && !missingOverlay
    if (showPermissionPopup) {
        AlertDialog(
            onDismissRequest = {
                showPermissionPopup = false
                scope.launch { settingsDataStore.saveFirstSetupCompleted() }
                onContactUpdated()
            },
            icon = {
                Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Final Step: Enable Caller ID") },
            text = {
                Text(
                    "To identify this burner contact when they call, please enable the following features:",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {

                    if (missingRole) {
                        Button(
                            onClick = {
                                roleManager?.let {
                                    val intent =
                                        it.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    requestRoleLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Identification Service")
                        }
                    }

                    if (missingOverlay) {
                        if (missingRole) Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                context.startActivity(intent)

                                // Re-check permission when returning to app
                                scope.launch {
                                    // small delay so system applies the change
                                    kotlinx.coroutines.delay(300)
                                    missingOverlay = !Settings.canDrawOverlays(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Visual Overlay")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showPermissionPopup = false
                            scope.launch { settingsDataStore.saveFirstSetupCompleted() }
                            onContactUpdated()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (isEverythingEnabled) "Done" else "I'll do it later"
                        )
                    }

                }
            }
        )
    }




    // --- Bottom Sheet ---
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
            CustomDurationPicker(
                onSet = { days, hours, minutes ->
                    val totalMillis = TimeUnit.DAYS.toMillis(days.toLong()) +
                            TimeUnit.HOURS.toMillis(hours.toLong()) +
                            TimeUnit.MINUTES.toMillis(minutes.toLong())
                    selectedDurationMillis = totalMillis
                    customDurationLabel = "Custom: ${days}d ${hours}h ${minutes}m"
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showBottomSheet = false
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showBottomSheet = false
                    }
                }
            )
        }
    }
}

// CustomDurationPicker component
@Composable
fun CustomDurationPicker(onSet: (days: Int, hours: Int, minutes: Int) -> Unit, onCancel: () -> Unit) {
    var days by remember { mutableStateOf(0f) }
    var hours by remember { mutableStateOf(0f) }
    var minutes by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Custom Duration", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(text = "Days: ${days.toInt()}")
            Slider(
                value = days,
                onValueChange = { days = it },
                onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..30f,
                steps = 29
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text(text = "Hours: ${hours.toInt()}")
            Slider(
                value = hours,
                onValueChange = { hours = it },
                onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..23f,
                steps = 22
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text(text = "Minutes: ${minutes.toInt()}")
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..59f,
                steps = 58
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onSet(days.toInt(), hours.toInt(), minutes.toInt()) }) {
                Text("Set Timer")
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}