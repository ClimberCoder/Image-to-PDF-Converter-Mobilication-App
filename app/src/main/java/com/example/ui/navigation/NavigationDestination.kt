package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    FILES("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder),
    USAGE("storage", "Storage", Icons.Filled.Storage, Icons.Outlined.Storage),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person);

    companion object {
        val bottomNavDestinations = listOf(HOME, FILES, USAGE, PROFILE)
    }
}
