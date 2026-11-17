package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityHighContainer
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityLowContainer
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityMediumContainer

enum class Priority(val label: String, val level: Int) {
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1);

    val color: Color
        get() = when (this) {
            HIGH -> PriorityHigh
            MEDIUM -> PriorityMedium
            LOW -> PriorityLow
        }

    val containerColor: Color
        get() = when (this) {
            HIGH -> PriorityHighContainer
            MEDIUM -> PriorityMediumContainer
            LOW -> PriorityLowContainer
        }
}
