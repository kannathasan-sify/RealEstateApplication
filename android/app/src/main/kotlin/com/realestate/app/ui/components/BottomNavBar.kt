package com.realestate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.navigation.Screen
import com.realestate.app.ui.theme.NestXBlue
import com.realestate.app.ui.theme.PrimaryRed
import com.realestate.app.ui.theme.TextSecondary

/**
 * Bottom nav bar — 3 items: Home | (+) Place an Ad | Menu
 * Favourites and Chat removed (Favourites accessible from top bar heart icon).
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    onPostAdClick: () -> Unit,
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home
            BottomNavItem(
                icon     = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                label    = "Home",
                selected = currentRoute == Screen.Home.route,
                onClick  = { onNavigate(Screen.Home) },
            )

            // Centre elevated FAB — Place an Ad
            FloatingActionButton(
                onClick        = onPostAdClick,
                shape          = CircleShape,
                containerColor = PrimaryRed,
                contentColor   = Color.White,
                modifier       = Modifier
                    .size(52.dp)
                    .shadow(6.dp, CircleShape),
                elevation      = FloatingActionButtonDefaults.elevation(0.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Place an Ad", modifier = Modifier.size(28.dp))
            }

            // Menu
            BottomNavItem(
                icon     = if (currentRoute == Screen.Menu.route) Icons.Filled.Menu else Icons.Outlined.Menu,
                label    = "Menu",
                selected = currentRoute == Screen.Menu.route,
                onClick  = { onNavigate(Screen.Menu) },
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) PrimaryRed else TextSecondary
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(
            text       = label,
            fontSize   = 10.sp,
            color      = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
