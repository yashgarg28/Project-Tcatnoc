package com.example.tempcontacts

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    viewModel: ContactViewModel,
    settingsDataStore: SettingsDataStore, // Added this parameter
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
    var isPhoneNumberValid by remember { mutableStateOf(false) }

    var selectedDurationMillis by remember { mutableStateOf<Long?>(null) }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var customDurationLabel by remember { mutableStateOf("Custom") }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showCallerIdGuide by remember { mutableStateOf(false) }

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
        onContactUpdated()
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
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        phone = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Phone") },
                    isError = !isPhoneNumberValid,
                    supportingText = { if (!isPhoneNumberValid) Text("Must be ${country.phoneLength} digits") else null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(0.6f)
                )
            }
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
                        val updatedContact = contact?.copy(name = name, phone = fullPhoneNumber, deletionTimestamp = deletionTimestamp)
                            ?: Contact(name = name, phone = fullPhoneNumber, deletionTimestamp = deletionTimestamp)

                        if (contactId == 0) {
                            viewModel.insert(updatedContact)
                            triggerSuccessFeedback()

                            // Check if this is the very first contact
                            if (contacts.isEmpty()) {
                                // Mark setup as completed to prevent double-popup from MainActivity
                                scope.launch {
                                    settingsDataStore.saveFirstSetupCompleted()
                                }
                                showCallerIdGuide = true
                            } else {
                                onContactUpdated()
                            }
                        } else {
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

    // --- Caller ID Guide Dialog ---
    if (showCallerIdGuide) {
        AlertDialog(
            onDismissRequest = {
                showCallerIdGuide = false
                onContactUpdated()
            },
            icon = { Icon(Icons.Outlined.ContactPhone, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Enable Caller ID?") },
            text = {
                Text(
                    "Would you like to identify these contacts when they call? This helps you know who it is without cluttering your permanent phonebook.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCallerIdGuide = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        requestRoleLauncher.launch(intent)
                    } else {
                        onContactUpdated()
                    }
                }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCallerIdGuide = false
                    onContactUpdated()
                }) {
                    Text("Maybe Later")
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

// CustomDurationPicker code remains the same...

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