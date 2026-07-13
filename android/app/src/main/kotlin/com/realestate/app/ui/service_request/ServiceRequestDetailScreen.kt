package com.realestate.app.ui.service_request

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.realestate.app.data.models.Quotation
import com.realestate.app.data.models.RequestUrgency
import com.realestate.app.data.models.ServiceRequest
import com.realestate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestDetailScreen(
    viewModel: ServiceRequestViewModel,
    requestId: String,
    currentUserId: String,
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()

    var quoteAmount by remember { mutableStateOf("") }
    var quoteTimeline by remember { mutableStateOf("") }
    var quoteNotes by remember { mutableStateOf("") }

    LaunchedEffect(requestId) {
        viewModel.loadRequestDetail(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = BackgroundWhite,
    ) { padding ->
        when (val state = detailState) {
            is ServiceRequestDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NestXBlue)
                }
            }
            is ServiceRequestDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = PrimaryRed)
                }
            }
            is ServiceRequestDetailUiState.Success -> {
                val req = state.request
                val isOwner = req.userId == currentUserId
                val context = LocalContext.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header / Category + Urgency
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (req.category == "construction") Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = req.serviceType,
                                color = if (req.category == "construction") Color(0xFFE65100) else Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        if (req.urgencyEnum != RequestUrgency.NORMAL) {
                            val emergency = req.urgencyEnum == RequestUrgency.EMERGENCY
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (emergency) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        if (emergency) Icons.Default.Warning else Icons.Default.Bolt,
                                        null,
                                        tint = if (emergency) StatusRejected else Color(0xFFB26A00),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        req.urgencyEnum.displayName,
                                        color = if (emergency) StatusRejected else Color(0xFFB26A00),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(req.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(req.district, fontSize = 14.sp, color = TextSecondary)
                    }

                    req.budgetDisplay?.let { budget ->
                        Surface(shape = RoundedCornerShape(8.dp), color = BannerBlue) {
                            Text(
                                "Budget: $budget",
                                color = NestXBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (!req.preferredDate.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            val formatted = try {
                                val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val outFmt = SimpleDateFormat("d MMMM yyyy", Locale.US)
                                outFmt.format(inFmt.parse(req.preferredDate)!!)
                            } catch (e: Exception) { req.preferredDate }
                            Text("Preferred start: $formatted", fontSize = 14.sp, color = TextSecondary)
                        }
                    }

                    if (!req.contactPhone.isNullOrBlank()) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${req.contactPhone}")))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NestXBlue),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call ${req.contactPhone}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(req.description ?: "No description provided.", fontSize = 14.sp, color = TextPrimary)

                    if (!req.images.isNullOrEmpty()) {
                        Text("Photos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(req.images) { imgUrl ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(120.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    if (isOwner) {
                        // OWNER VIEW: List of Submitted Quotations
                        Text("Submitted Quotations (${state.quotations.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (state.quotations.isEmpty()) {
                            Text("No quotations submitted yet. Providers in your radius will be notified shortly.", fontSize = 13.sp, color = TextSecondary)
                        } else {
                            state.quotations.forEach { quote ->
                                QuotationItem(
                                    quote = quote,
                                    onAccept = { viewModel.updateQuotationStatus(req.id, quote.id, "accepted") },
                                    onReject = { viewModel.updateQuotationStatus(req.id, quote.id, "rejected") }
                                )
                            }
                        }
                    } else {
                        // CONTRACTOR VIEW: Submit Quote form
                        val alreadyQuoted = state.quotations.any { it.contractorId == currentUserId }
                        if (alreadyQuoted) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "✓ You have already submitted a quotation for this request.",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            Text("Submit your Quotation", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            OutlinedTextField(
                                value = quoteAmount,
                                onValueChange = { quoteAmount = it },
                                label = { Text("Quotation Amount (₹)") },
                                placeholder = { Text("e.g. 75000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = quoteTimeline,
                                onValueChange = { quoteTimeline = it },
                                label = { Text("Estimated Timeline") },
                                placeholder = { Text("e.g. 5 days, 3 weeks") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = quoteNotes,
                                onValueChange = { quoteNotes = it },
                                label = { Text("Notes / Proposal Details") },
                                placeholder = { Text("Describe materials used, work breakdown...") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    val amount = quoteAmount.toDoubleOrNull()
                                    if (amount != null && quoteTimeline.isNotBlank()) {
                                        viewModel.submitQuotation(
                                            requestId = req.id,
                                            amount = amount,
                                            timeline = quoteTimeline,
                                            notes = quoteNotes
                                        )
                                        // clear inputs
                                        quoteAmount = ""
                                        quoteTimeline = ""
                                        quoteNotes = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                            ) {
                                Text("Submit Quote", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotationItem(
    quote: Quotation,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.amountDisplay ?: "Estimation pending",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NestXBlue
                )
                Text(
                    text = "Timeline: ${quote.timeline}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            if (!quote.notes.isNullOrBlank()) {
                Text(quote.notes, fontSize = 13.sp, color = TextPrimary)
            }

            if (quote.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Accept Quote", color = Color.White, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                        border = BorderStroke(1.dp, PrimaryRed),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject", fontSize = 13.sp)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (quote.status == "accepted") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = quote.status.replaceFirstChar { it.uppercase() },
                        color = if (quote.status == "accepted") Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
