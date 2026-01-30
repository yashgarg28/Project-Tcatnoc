package com.example.tempcontacts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// --- 1. THE HELPER FUNCTION (Must be here) ---
fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavHostController,
    isDarkTheme: Boolean, // <--- Correctly received from MainActivity
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val currentVersion = getAppVersionName(context)

    // BRAND COLOR LOGIC: White in Dark Mode, Blue in Light Mode
    val brandColor = if (isDarkTheme) Color.White else Color(0xFF313591)

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

            // --- THEME ADAPTIVE LOGO ---
            Image(
                painter = painterResource(id = if (isDarkTheme) R.mipmap.burnerlogowhite else R.mipmap.burnerlogoblue),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Version $currentVersion",
                style = MaterialTheme.typography.bodySmall,
                color = brandColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- HOW IT WORKS SECTION ---
            Text(
                text = "HOW IT WORKS",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = brandColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutInfoRow(brandColor, Icons.Outlined.Timer, "Auto-Deletion", "Contacts disappear automatically when the timer hits zero.")
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutInfoRow(brandColor, Icons.Outlined.Call, "Smart Caller ID", "Identifies temporary contacts on incoming calls without cluttering your main phonebook.")
                    Spacer(modifier = Modifier.height(12.dp))
                    AboutInfoRow(brandColor, Icons.Outlined.CloudOff, "100% Offline", "Your data never leaves your device. No servers, no tracking.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CONNECT & LEGAL SECTION ---
            Text(
                text = "CONNECT & LEGAL",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = brandColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    AboutClickableRow(brandColor, Icons.Outlined.Policy, "Privacy Policy", onPrivacyPolicyClick)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    AboutClickableRow(
                        brandColor,
                        Icons.Outlined.Lightbulb,
                        "Suggest a Feature",
                        onClick = {
                            uriHandler.openUri("mailto:burnerbook07@gmail.com?subject=BurnerBook Feature Idea")
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    AboutClickableRow(
                        brandColor,
                        Icons.Outlined.Share,
                        "Share with Friends",
                        onClick = {
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
                        brandColor,
                        Icons.Outlined.Gavel,
                        "Open Source Licenses",
                        onClick = { navController.navigate("licenses") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "© 2026 Burner Book™\nMade with ❤️ for Privacy",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = brandColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun AboutInfoRow(brandColor: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = brandColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AboutClickableRow(brandColor: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = brandColor)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}