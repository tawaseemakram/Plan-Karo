package com.example.data.model

import androidx.compose.ui.graphics.Color

data class TaskCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val isCustom: Boolean = false
) {
    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF3B82F6)
        }

    val containerColor: Color
        get() = color.copy(alpha = 0.16f)

    companion object {
        val WORK = TaskCategory(
            id = "work",
            name = "Work",
            colorHex = "#3B82F6", // Vibrant Blue
            isCustom = false
        )

        val PERSONAL = TaskCategory(
            id = "personal",
            name = "Personal",
            colorHex = "#A855F7", // Purple / Violet
            isCustom = false
        )

        val HEALTH = TaskCategory(
            id = "health",
            name = "Health",
            colorHex = "#10B981", // Emerald Green
            isCustom = false
        )

        val FINANCE = TaskCategory(
            id = "finance",
            name = "Finance",
            colorHex = "#F59E0B", // Amber / Gold
            isCustom = false
        )

        val STUDY = TaskCategory(
            id = "study",
            name = "Study",
            colorHex = "#06B6D4", // Cyan
            isCustom = false
        )

        val HOME = TaskCategory(
            id = "home",
            name = "Home",
            colorHex = "#EC4899", // Rose / Pink
            isCustom = false
        )

        val DEFAULT_CATEGORIES = listOf(
            WORK,
            PERSONAL,
            HEALTH,
            FINANCE,
            STUDY,
            HOME
        )

        val PRESET_COLORS = listOf(
            "#3B82F6", // Blue
            "#A855F7", // Purple
            "#10B981", // Emerald Green
            "#F59E0B", // Amber
            "#06B6D4", // Cyan
            "#EC4899", // Pink
            "#EF4444", // Red
            "#8B5CF6", // Indigo / Violet
            "#14B8A6", // Teal
            "#F97316"  // Orange
        )

        fun findCategory(categories: List<TaskCategory>, name: String?): TaskCategory {
            if (name.isNullOrBlank()) return PERSONAL
            return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: DEFAULT_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: TaskCategory(
                    id = name.lowercase().replace(" ", "_"),
                    name = name,
                    colorHex = "#00E5BE",
                    isCustom = true
                )
        }
    }
}
