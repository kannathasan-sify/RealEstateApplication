package com.realestate.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.app.R
import com.realestate.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        delay(2000)
        val destination = viewModel.getStartDestination()
        onNavigate(destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NestXBlue),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // App logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.ic_nestx_logo),
                    contentDescription = "NestX",
                    tint              = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier          = Modifier.size(72.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text       = "NestX",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Find Your Dream Property in Tamil Nadu",
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
