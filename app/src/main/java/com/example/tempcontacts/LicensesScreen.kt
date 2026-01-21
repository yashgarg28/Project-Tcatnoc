package com.example.tempcontacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBackClick: () -> Unit) {
    val libraries = listOf(
        "Jetpack Compose",
        "Kotlin Coroutines",
        "Room Persistence Library",
        "Material Components 3",
        "AndroidX DataStore",
        "Firebase App Distribution"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(libraries) { lib ->
                LicenseItem(lib)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun LicenseItem(name: String) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Copyright © The Android Open Source Project\nLicensed under the Apache License, Version 2.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}