package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.SecondaryText

data class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

val NAV_ITEMS = listOf(
    NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavItem("browser", "Browser", Icons.Filled.Public, Icons.Outlined.Public, "nav_browser"),
    NavItem("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download, "nav_downloads"),
    NavItem("gallery", "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_gallery"),
    NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    activeDownloadsCount: Int,
    onNavigate: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = colorScheme.surface,
        contentColor = SecondaryText,
        windowInsets = WindowInsets.navigationBars
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                modifier = Modifier.testTag(item.tag),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.onPrimary,
                    selectedTextColor = colorScheme.primary,
                    indicatorColor = colorScheme.primary,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText
                ),
                icon = {
                    if (item.route == "downloads" && activeDownloadsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = PrimaryAccent) {
                                    Text(activeDownloadsCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = { Text(item.title, fontSize = 11.sp) }
            )
        }
    }
}
