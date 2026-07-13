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

    var selectedPlan by remember { mutableStateOf("silver") }

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
                val isSelected = selectedPlan == plan.tier
                val borderStroke = if (isSelected) {
                    Modifier.border(2.dp, NestXBlue, RoundedCornerShape(16.dp))
                } else Modifier

                Card(
                    onClick = { selectedPlan = plan.tier },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderStroke)
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

            Button(
                onClick = {
                    viewModel.upgradePlan(selectedPlan)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                enabled = upgradeState !is UpgradeState.Loading
            ) {
                if (upgradeState is UpgradeState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
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
