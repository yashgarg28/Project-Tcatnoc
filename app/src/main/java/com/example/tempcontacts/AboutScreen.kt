package com.example.tempcontacts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale

// Helper function to get the version name from build.gradle dynamically
fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0" // Fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavHostController, // <--- Put it here with a comma
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val currentVersion = getAppVersionName(context)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Header
            Image(
                painter = painterResource(id = R.mipmap.logo_png),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp) // Adjusted size for a cleaner look
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Burner Book™", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // This now shows the ACTUAL version from your Gradle file
            Text(
                text = "Version $currentVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- HOW IT WORKS SECTION ---
            Text(
                text = "HOW IT WORKS",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutInfoRow(icon = Icons.Outlined.Timer, title = "Auto-Deletion", desc = "Contacts disappear automatically when the timer hits zero.")
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutInfoRow(icon = Icons.Outlined.Call, title = "Smart Caller ID", desc = "Identifies temporary contacts on incoming calls without cluttering your main phonebook.")
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutInfoRow(icon = Icons.Outlined.CloudOff, title = "100% Offline", desc = "Your data never leaves your device. No servers, no tracking.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CONNECT & LEGAL SECTION ---
            Text(
                text = "CONNECT & LEGAL",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    AboutClickableRow(icon = Icons.Outlined.Policy, text = "Privacy Policy", onClick = onPrivacyPolicyClick)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    AboutClickableRow(
                        icon = Icons.Outlined.Lightbulb,
                        text = "Suggest a Feature",
                        onClick = {
                            uriHandler.openUri("mailto:burnerbook07@gmail.com?subject=BurnerBook Feature Idea")
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    AboutClickableRow(
                        icon = Icons.Outlined.Share,
                        text = "Share with Friends",
                        onClick = {
                            // Replace with your actual Firebase/Play Store link
                            val betaLink = "https://appdistribution.firebase.dev/i/b671dff24882de79"

                            val shareText = """
            Join the Burner Book™ Beta! 🛡️
            
            I'm using this app to manage temporary contacts and screen burner calls. It's totally private and offline.
            
            Download the beta here:
            $betaLink
        """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Beta Invite"))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    AboutClickableRow(
                        icon = Icons.Outlined.Gavel, // Legal/License icon
                        text = "Open Source Licenses",
                        onClick = { navController.navigate("licenses") } // Or pass this as a parameter to the Composable
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "© 2026 Burner Book\nMade with ❤️ for Privacy",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AboutInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AboutClickableRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}