package com.example.tempcontacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    viewModel: ContactViewModel,
    contactId: Int,
    onContactUpdated: () -> Unit,
    onBackClick: () -> Unit
) {
    val contacts by viewModel.allContacts.collectAsState()
    val contact = contacts.find { it.id == contactId }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(countryList.find { it.name == "India" } ?: countryList[0]) }
    var isPhoneNumberValid by remember { mutableStateOf(false) }

    var selectedDurationMillis by remember { mutableStateOf<Long?>(null) }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var customDurationLabel by remember { mutableStateOf("Custom") }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var countryCodeExpanded by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
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
                ExposedDropdownMenuBox(expanded = countryCodeExpanded, onExpandedChange = { countryCodeExpanded = !countryCodeExpanded }, modifier = Modifier.weight(0.4f)) {
                    OutlinedTextField(
                        value = "${country.flagEmoji} ${country.code}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryCodeExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = countryCodeExpanded, onDismissRequest = { countryCodeExpanded = false }) {
                        countryList.forEach { c ->
                            DropdownMenuItem(text = { Text("${c.flagEmoji} ${c.name} (${c.code})") }, onClick = { 
                                country = c 
                                countryCodeExpanded = false
                            })
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
            
            Text("Auto-Delete Duration")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedChip == "no_time", onClick = { 
                    selectedChip = "no_time"
                    selectedDurationMillis = null
                 }, label = { Text("No Time") })
                FilterChip(selected = selectedChip == "24h", onClick = { 
                    selectedChip = "24h"
                    selectedDurationMillis = TimeUnit.HOURS.toMillis(24)
                 }, label = { Text("24 Hours") })
                FilterChip(selected = selectedChip == "7d", onClick = { 
                    selectedChip = "7d"
                    selectedDurationMillis = TimeUnit.DAYS.toMillis(7)
                 }, label = { Text("7 Days") })
                FilterChip(selected = selectedChip == "custom", onClick = { 
                    selectedChip = "custom"
                    showBottomSheet = true
                 }, label = { Text(customDurationLabel) })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank() && isPhoneNumberValid) {
                        val fullPhoneNumber = "${country.code} $phone"
                        val deletionTimestamp = selectedDurationMillis?.let { System.currentTimeMillis() + it }
                        val updatedContact = contact?.copy(name = name, phone = fullPhoneNumber, deletionTimestamp = deletionTimestamp) ?: Contact(name = name, phone = fullPhoneNumber, deletionTimestamp = deletionTimestamp)
                        if (contactId == 0) {
                            viewModel.insert(updatedContact)
                        } else {
                            viewModel.update(updatedContact)
                        }
                        onContactUpdated()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && isPhoneNumberValid
            ) {
                Text(if (contactId == 0) "Add Contact" else "Save Changes")
            }
        }
    }

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

@Composable
fun CustomDurationPicker(onSet: (days: Int, hours: Int, minutes: Int) -> Unit, onCancel: () -> Unit) {
    var days by remember { mutableStateOf(0f) }
    var hours by remember { mutableStateOf(0f) }
    var minutes by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(16.dp)) {
        // Days Slider
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
        // Hours Slider
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
        // Minutes Slider
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
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onSet(days.toInt(), hours.toInt(), minutes.toInt()) }) {
                Text("Set")
            }
        }
    }
}
