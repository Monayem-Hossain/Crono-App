package com.monayem.crono.ui.theme

import androidx.compose.ui.graphics.Color

object ColorUtils {
    val subjectColors = listOf(
        Color(0xFFF4B183), // Light Orange
        Color(0xFFC5E0B4), // Light Green
        Color(0xFF87CEEB), // Sky Blue
        Color(0xFFF7CAC9), // Light Pink
        Color(0xFFFCEE82), // Light Yellow
        Color(0xFFB2B2B2), // Grey
        Color(0xFFD4A6C8), // Light Purple
        Color(0xFFB3E5FC)  // Lighter Blue
    )

    fun getColorForSubject(subjectCount: Int): Color {
        return subjectColors[subjectCount % subjectColors.size]
    }
}
