package com.example.tempcontacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = """
                    1. Data Storage: All contact information created in Burner Book is stored locally on your device's internal storage. 
                    
                    2. Caller ID & Call Screening: To provide Caller ID notifications, the app identifies incoming phone numbers by comparing them against your local Burner Book database. This process happens entirely on your device. We never log your call history or share your call data.
                    
                    3. Data Transmission: This app does not have internet permissions and cannot upload your data or call information to any servers.
                    
                    4. Third Parties: No data is shared with third-party services or advertisers.
                    
                    5. Permissions: We request Notification permissions for deletion alerts, and Phone/Call Log permissions specifically to enable the Caller ID identification feature.
                    
                    6. Deletion: Once a contact's timer expires, the data is permanently erased.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        }
    }
}