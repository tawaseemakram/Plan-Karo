package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon

@Composable
fun AppBottomBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .testTag("app_bottom_bar")
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        // Tab 1: Tasks
        NavigationBarItem(
            selected = currentScreen == AppScreen.TASKS,
            onClick = { onScreenSelected(AppScreen.TASKS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.TASKS) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt,
                    contentDescription = "Daily Tasks"
                )
            },
            label = {
                Text(
                    text = "Daily Tasks",
                    fontSize = 12.sp,
                    fontWeight = if (currentScreen == AppScreen.TASKS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealDeep,
                selectedTextColor = TealNeon,
                indicatorColor = TealNeon,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_tasks")
        )

        // Tab 2: Notes (Placed right next to Daily Tasks)
        NavigationBarItem(
            selected = currentScreen == AppScreen.NOTES,
            onClick = { onScreenSelected(AppScreen.NOTES) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.NOTES) Icons.Filled.EditNote else Icons.Outlined.EditNote,
                    contentDescription = "Notes"
                )
            },
            label = {
                Text(
                    text = "Notes",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.NOTES) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealDeep,
                selectedTextColor = TealNeon,
                indicatorColor = TealNeon,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_notes")
        )

        // Tab 3: Smart Calendar
        NavigationBarItem(
            selected = currentScreen == AppScreen.CALENDAR,
            onClick = { onScreenSelected(AppScreen.CALENDAR) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.CALENDAR) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = "Smart Calendar"
                )
            },
            label = {
                Text(
                    text = "Calendar",
                    fontSize = 12.sp,
                    fontWeight = if (currentScreen == AppScreen.CALENDAR) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealDeep,
                selectedTextColor = TealNeon,
                indicatorColor = TealNeon,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_calendar")
        )

        // Tab 3: Habits & Routines
        NavigationBarItem(
            selected = currentScreen == AppScreen.HABITS,
            onClick = { onScreenSelected(AppScreen.HABITS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.HABITS) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                    contentDescription = "Habits & Routines"
                )
            },
            label = {
                Text(
                    text = "Routines",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.HABITS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealDeep,
                selectedTextColor = TealNeon,
                indicatorColor = TealNeon,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_habits")
        )

        // Tab 4: Profile
        NavigationBarItem(
            selected = currentScreen == AppScreen.PROFILE,
            onClick = { onScreenSelected(AppScreen.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.PROFILE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealDeep,
                selectedTextColor = TealNeon,
                indicatorColor = TealNeon,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_profile")
        )
    }
}
