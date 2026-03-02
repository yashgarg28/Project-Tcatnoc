package com.example.tempcontacts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun getTagAttributes(tag: String): Triple<Color, Color, ImageVector> {
    return when (tag) {
        "Work" -> Triple(Color(0xFFE3F2FD), Color(0xFF2196F3), Icons.Default.BusinessCenter)
        "Delivery" -> Triple(Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.LocalShipping)
        "Social" -> Triple(Color(0xFFFCE4EC), Color(0xFFE91E63), Icons.Default.Groups)
        "Personal" -> Triple(Color(0xFFE8F5E9), Color(0xFF4CAF50), Icons.Default.Person)
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), Icons.Default.Label)
    }
}