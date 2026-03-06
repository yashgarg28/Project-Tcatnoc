package com.example.tempcontacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset

@Composable
fun ContactContextMenu(
    contact: Contact,
    position: Offset = Offset.Zero,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onDelete: () -> Unit,
    onExtendTimer: () -> Unit,
    onSaveForever: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) { },
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            // Edit
            ContextMenuItem(
                icon = Icons.Default.Edit,
                label = "Edit",
                onClick = onEdit
            )

            // Call
            ContextMenuItem(
                icon = Icons.Default.Call,
                label = "Call",
                onClick = onCall
            )

            // Message
            ContextMenuItem(
                icon = Icons.Default.Message,
                label = "Message",
                onClick = onMessage
            )

            // Extend Timer
            ContextMenuItem(
                icon = Icons.Default.AccessTime,
                label = "Extend Timer",
                onClick = onExtendTimer
            )

            // Save Forever
            ContextMenuItem(
                icon = Icons.Default.Save,
                label = "Save Forever",
                onClick = onSaveForever
            )

            // Delete (Destructive)
            ContextMenuItem(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }

        // Divider between items (except after Delete)
        if (label != "Delete") {
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}