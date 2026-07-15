package com.realestate.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.theme.*

data class PlanInfo(
    val tier: String,
    val name: String,
    val price: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val features: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val upgradeState by viewModel.upgradeState.collectAsState()

    val currentDetails = (uiState as? SubscriptionUiState.Success)?.details
    val currentTier = currentDetails?.subscriptionTier
    val isCurrentActive = currentDetails != null && isActivePaidPlan(currentDetails)

    var selectedPlan by remember { mutableStateOf<String?>(null) }
    // Default the selection to the user's current plan once it's known, but only once —
    // after that the user's own taps drive it.
    LaunchedEffect(currentTier) {
        if (selectedPlan == null && currentTier != null) {
            selectedPlan = currentTier
        }
    }
    val effectiveSelected = selectedPlan ?: "silver"

    val plans = listOf(
        PlanInfo(
            tier = "free",
            name = "Free Plan",
            price = "₹0",
            gradientStart = Color(0xFF78909C),
            gradientEnd = Color(0xFF546E7A),
            features = listOf("3 Properties Listing", "Standard Search", "10 Images upload limit")
        ),
        PlanInfo(
            tier = "silver",
            name = "Silver Plan",
            price = "₹299/mo",
            gradientStart = Color(0xFF90A4AE),
            gradientEnd = Color(0xFF37474F),
            features = listOf("10 Properties Listing", "20 Images upload limit", "Video Upload Tour enabled", "Standard Search support")
        ),
        PlanInfo(
            tier = "gold",
            name = "Gold Plan",
            price = "₹599/mo",
            gradientStart = Color(0xFFFFB300),
            gradientEnd = Color(0xFFFF8F00),
            features = listOf("Unlimited Listings", "Featured Listing Badge", "Priority Search listing placement", "Detailed View Analytics")
        ),
        PlanInfo(
            tier = "platinum",
            name = "Platinum Plan",
            price = "₹999/mo",
            gradientStart = Color(0xFF8E24AA),
            gradientEnd = Color(0xFF4A148C),
            features = listOf("Unlimited Listings", "Featured Agent highlights", "Lead Management integration", "Custom Business Profile", "24/7 priority support")
        ),
        PlanInfo(
            tier = "contractor",
            name = "Contractors Plan",
            price = "₹999/mo",
            gradientStart = Color(0xFFE65100),
            gradientEnd = Color(0xFFBF360C),
            features = listOf("Display Business in Feed", "Unlimited Portfolio projects", "Priority Construction Search", "Receive Direct Leads", "Customer Reviews & Ratings")
        )
    )

    LaunchedEffect(upgradeState) {
        if (upgradeState is UpgradeState.Success) {
            viewModel.upgradeState.value = UpgradeState.Idle
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Subscription Plans", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Subscription status header
            when (val state = uiState) {
                is SubscriptionUiState.Loading -> {
                    CircularProgressIndicator(color = NestXBlue)
                }
                is SubscriptionUiState.Error -> {
                    Text("Error loading details: ${state.message}", color = PrimaryRed)
                }
                is SubscriptionUiState.Success -> {
                    val currentTier = state.details.subscriptionTier
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Subscription", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentTier.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NestXBlue
                                )
                                Text(
                                    text = "${state.details.currentListingsCount} / ${if (state.details.maxListings > 1000) "Unlimited" else state.details.maxListings} Listings",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                            state.details.subscriptionExpiresAt?.let { exp ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Expires: ${exp.take(10)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Text("Select your plan below:", fontWeight = FontWeight.Bold, color = TextPrimary)

            // Render Premium plan cards
            plans.forEach { plan ->
                val isSelected = effectiveSelected == plan.tier
                val isThisCurrentActivePlan = isCurrentActive && plan.tier == currentTier
                // Rule: can't downgrade to the minimum (Free) plan while a paid plan is active.
                val isFreeLockedWhileActive = plan.tier == MIN_TIER && isCurrentActive
                val cardEnabled = !isThisCurrentActivePlan && !isFreeLockedWhileActive
                val borderStroke = if (isSelected) {
                    Modifier.border(2.dp, NestXBlue, RoundedCornerShape(16.dp))
                } else Modifier

                Card(
                    onClick = { selectedPlan = plan.tier },
                    enabled = cardEnabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderStroke)
                        .alpha(if (!cardEnabled) 0.55f else 1f)
                ) {
                    Column {
                        // Plan header with Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(plan.gradientStart, plan.gradientEnd)))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = plan.price,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Current-plan / locked-plan status badge
                        if (isThisCurrentActivePlan) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusApproved.copy(alpha = 0.12f),
                                modifier = Modifier.padding(start = 16.dp, top = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = StatusApproved, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Your Current Active Plan", fontSize = 11.sp, color = StatusApproved, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (isFreeLockedWhileActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TextSecondary.copy(alpha = 0.10f),
                                modifier = Modifier.padding(start = 16.dp, top = 10.dp)
                            ) {
                                Text(
                                    "Available once your current plan expires",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Feature list
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            plan.features.forEach { feature ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = plan.gradientStart,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(feature, fontSize = 13.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (upgradeState is UpgradeState.Error) {
                Text(
                    text = (upgradeState as UpgradeState.Error).message,
                    color = PrimaryRed,
                    fontSize = 13.sp
                )
            }

            // Rules: block re-buying the same active plan, and block downgrading to Free
            // while a paid plan is still active. The button label/enabled state reflects
            // whichever of those applies to the currently-selected plan.
            val isSameActivePlanSelected = isCurrentActive && effectiveSelected == currentTier
            val isFreeDowngradeSelected = isCurrentActive && effectiveSelected == MIN_TIER
            val submitBlocked = isSameActivePlanSelected || isFreeDowngradeSelected

            Button(
                onClick = {
                    viewModel.upgradePlan(effectiveSelected)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                enabled = upgradeState !is UpgradeState.Loading && !submitBlocked
            ) {
                if (upgradeState is UpgradeState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else if (isSameActivePlanSelected) {
                    Text(
                        text = "Current Active Plan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else if (isFreeDowngradeSelected) {
                    Text(
                        text = "Can't Downgrade While Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Text(
                        text = "Subscribe / Upgrade Plan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
