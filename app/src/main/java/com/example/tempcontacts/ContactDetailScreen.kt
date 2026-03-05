package com.example.tempcontacts

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    viewModel: ContactViewModel,
    contactId: Int,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val contacts by viewModel.allContacts.collectAsState()
    val contact = contacts.find { it.id == contactId }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }

    // ✅ NEW - Permission request launcher for multiple contacts permissions
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] ?: false

        if (readGranted && writeGranted) {
            // ✅ Both permissions granted - proceed with export
            pendingContact?.let {
                ContactExporter.performExport(context, it)
                pendingContact = null
            }
        } else {
            // ❌ User denied permissions
            Toast.makeText(
                context,
                "Contact permissions required to save contact to phone book",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        contact?.let {
                            val vcardString = "BEGIN:VCARD\n" +
                                    "VERSION:3.0\n" +
                                    "FN:${it.name}\n" +
                                    "TEL:${it.phone}\n" +
                                    "END:VCARD"

                            val vcfFile = File(context.cacheDir, "contact.vcf")
                            vcfFile.writeText(vcardString)

                            val vcfUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                vcfFile
                            )

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/x-vcard"
                                putExtra(Intent.EXTRA_STREAM, vcfUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Contact"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            contact?.let { contactDetails ->
                Spacer(modifier = Modifier.height(16.dp))
                // Icon
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactDetails.name.first().toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = contactDetails.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                // Time Remaining
                contactDetails.deletionTimestamp?.let { timestamp ->
                    Spacer(modifier = Modifier.height(8.dp))
                    RemainingTime(deletionTimestamp = timestamp, isDarkTheme = isDarkTheme)
                }

                // ✅ Display Tag Badge in Detail Screen
                if (contact.tag != "None") {
                    val (bgColor, contentColor, icon) = getTagAttributes(contact.tag)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contact.tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = contentColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                //Notes
                if (contactDetails.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = contactDetails.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Call and Text Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionButton(icon = Icons.Default.Call, label = "Call") {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contactDetails.phone}"))
                        context.startActivity(intent)
                    }
                    ActionButton(icon = Icons.Default.Message, label = "Text") {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contactDetails.phone}"))
                        context.startActivity(intent)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                DetailsCard(contact = contactDetails)

                Spacer(modifier = Modifier.height(16.dp))
                AdditionalInfoCard(contact = contactDetails)

                Spacer(Modifier.weight(1f))

                // ✅ SAVE FOREVER BUTTON - With permission handling
                Button(
                    onClick = {
                        contact?.let {
                            // Check if permissions are already granted
                            if (ContactExporter.hasContactsPermissions(context)) {
                                // Permissions granted, proceed directly
                                ContactExporter.performExport(context, it)
                            } else {
                                // Permissions not granted, store contact and request permissions
                                pendingContact = it
                                contactsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.WRITE_CONTACTS
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Forever",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Forever")
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Contact")
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Contact?") },
                        text = { Text("Are you sure you want to delete this contact? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.delete(contactDetails)
                                    Toast.makeText(context, "${contactDetails.name} deleted", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    onBackClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemainingTime(deletionTimestamp: Long, isDarkTheme: Boolean = false) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val diff = deletionTimestamp - currentTime

    val oneDay = 24 * 60 * 60 * 1000L
    val fiveDays = 5 * oneDay

    val textColor = when {
        diff <= 0 -> Color.Gray
        diff < oneDay -> Color(0xFFFF5252)
        diff < fiveDays -> Color(0xFFFFB74D)
        else -> if (isDarkTheme) Color.White else Color(0xFF2196F3)
    }

    val remainingText = if (diff <= 0) "Deleting..." else {
        val days = diff / oneDay
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60

        when {
            days > 0 -> "Deletes in $days d, $hours h"
            hours > 0 -> "Deletes in $hours h, $minutes m"
            else -> "Deletes in $minutes m"
        }
    }

    Text(
        text = remainingText,
        color = textColor,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailsCard(contact: Contact) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Contact Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(icon = Icons.Default.Call, label = "Phone", value = contact.phone)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(painter = painterResource(id = R.drawable.whatsapp_logo), label = "WhatsApp", value = contact.phone, iconSize = 28.dp, onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=${contact.phone}")
                    setPackage("com.whatsapp")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
                }
            })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(painter = painterResource(id = R.drawable.telegram_logo), label = "Telegram", value = contact.phone, onClick = {
                val cleanNumber = contact.phone.filter { char -> char.isDigit() }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$cleanNumber"))
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Could not open Telegram.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

@Composable
private fun AdditionalInfoCard(contact: Contact) {
    val context = LocalContext.current

    if (contact.email.isBlank() && contact.address.isBlank() && contact.website.isBlank()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Additional Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (contact.email.isNotBlank()) {
                DetailRow(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    value = contact.email,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${contact.email}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Email app not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            if (contact.address.isNotBlank()) {
                DetailRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "Address",
                    value = contact.address,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("geo:0,0?q=${Uri.encode(contact.address)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Maps app not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            if (contact.website.isNotBlank()) {
                DetailRow(
                    icon = Icons.Outlined.Language,
                    label = "Website",
                    value = contact.website,
                    onClick = {
                        val url = if (contact.website.startsWith("http")) {
                            contact.website
                        } else {
                            "https://${contact.website}"
                        }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Browser not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: Painter? = null,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    iconSize: Dp = 24.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).run { if (onClick != null) clickable(onClick = onClick) else this }
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize), tint = MaterialTheme.colorScheme.primary)
        } else if (painter != null) {
            Icon(painter, contentDescription = label, modifier = Modifier.size(iconSize), tint = Color.Unspecified)
        }
        Spacer(modifier = Modifier.padding(horizontal = 16.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
