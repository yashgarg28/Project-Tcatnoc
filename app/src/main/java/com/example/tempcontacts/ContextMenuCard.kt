package com.example.tempcontacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onExtendTimer: (Int) -> Unit,
    onSaveForever: () -> Unit,
) {
    var showExtendOptions by remember { mutableStateOf(false) }

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

            // Extend Timer with submenu
            ContextMenuItem(
                icon = if (showExtendOptions) Icons.Default.ExpandLess else Icons.Default.AccessTime,
                label = "Extend Timer",
                onClick = { showExtendOptions = !showExtendOptions }
            )

            // Submenu options for extending timer
            if (showExtendOptions) {
                // 24 Hours option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExtendTimer(1)
                        }
                        .padding(vertical = 10.dp, horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "24 Hours",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "24 Hours",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 7 Days option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExtendTimer(7)
                        }
                        .padding(vertical = 10.dp, horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "7 Days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "7 Days",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Text on the left
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )

            // Icon on the right
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        // Divider between items (except after Delete which is last)
        if (label != "Delete") {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}