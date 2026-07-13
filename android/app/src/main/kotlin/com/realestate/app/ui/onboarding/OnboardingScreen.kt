package com.realestate.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.*
import com.realestate.app.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Find Your Dream Home",
        subtitle = "Browse thousands of properties for rent and sale across UAE. Find the perfect home that fits your lifestyle.",
        icon = Icons.Filled.Home,
    ),
    OnboardingPage(
        title = "Search & Filter Easily",
        subtitle = "Use powerful filters to narrow down properties by price, location, amenities, and more. Save your searches.",
        icon = Icons.Filled.Search,
    ),
    OnboardingPage(
        title = "Connect with Agents",
        subtitle = "Contact verified landlords and agents directly via call, WhatsApp, or email. Book property visits instantly.",
        icon = Icons.Filled.People,
    ),
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == onboardingPages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // Skip button
        if (!isLast) {
            TextButton(
                onClick = {
                    viewModel.markOnboardingDone()
                    onDone()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Text("Skip", color = TextSecondary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            HorizontalPager(
                count = onboardingPages.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            Spacer(Modifier.height(32.dp))

            // Dots indicator
            HorizontalPagerIndicator(
                pagerState = pagerState,
                activeColor = PrimaryRed,
                inactiveColor = BorderColor,
                indicatorWidth = 8.dp,
                indicatorHeight = 8.dp,
                spacing = 6.dp,
            )

            Spacer(Modifier.height(32.dp))

            // CTA button
            Button(
                onClick = {
                    if (isLast) {
                        viewModel.markOnboardingDone()
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (isLast) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Blob background with icon
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(OnboardingBlob),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = PrimaryRed,
                modifier = Modifier.size(80.dp),
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}
